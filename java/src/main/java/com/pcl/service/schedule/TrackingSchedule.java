package com.pcl.service.schedule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pcl.constant.Constants;
import com.pcl.constant.UserConstants;
import com.pcl.dao.AlgInstanceDao;
import com.pcl.dao.AlgModelDao;
import com.pcl.dao.LabelTaskItemDao;
import com.pcl.dao.ReIDLabelTaskItemDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.body.AutoLabelBody;
import com.pcl.pojo.mybatis.AlgInstance;
import com.pcl.pojo.mybatis.AlgModel;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.service.MinioFileService;
import com.pcl.service.ObjectFileService;
import com.pcl.service.TokenManager;
import com.pcl.util.FileUtil;
import com.pcl.util.JsonUtil;
import com.pcl.util.ProcessExeUtil;
import com.pcl.util.TimeUtil;

@Service
public class TrackingSchedule {

	private static Logger logger = LoggerFactory.getLogger(AutoLabelVideoSchedule.class);


	@Autowired
	private AlgModelDao algModelDao;

	@Autowired
	private AlgInstanceDao algInstanceDao;

	@Autowired
	private LabelTaskItemDao labelTaskItemDao;

	@Autowired
	private ReIDLabelTaskItemDao reIDlabelTaskItemDao;

	@Autowired
	private ObjectFileService fileService;
	
	private final static String REID_TASK_TRACKING = "4";

	private boolean isMultiTrack(AlgModel algModel) {
		if(algModel.getId() == 20 || algModel.getId() == 21) {
			return true;
		}
		return false;
	}


	public void tracking(AutoLabelBody body,int exceedTime) throws LabelSystemException {
		
		int algId = body.getModel();
		AlgModel algModel = algModelDao.queryAlgModelById(algId);//
		AlgInstance algInstance = null;
		if(algModel != null) {
			algInstance = algInstanceDao.queryAlgInstanceById(Integer.parseInt(algModel.getAlg_instance_id()));
		}
		if(algInstance == null) {
			logger.info("the algInstance is null. algInstanceId=" + algModel.getAlg_instance_id());
			return;
		}

		String datasetName = "auto";
		String imageDir =  algInstance.getAlg_root_dir()  + "img_tracking" + File.separator + System.nanoTime() + File.separator;
		File imageDirFile = new File(imageDir);
		if(!imageDirFile.exists()) {
			imageDirFile.mkdir();
		}
		File dataSetRootFile = new File(imageDir,datasetName);
		dataSetRootFile.mkdirs();
		List<LabelTaskItem> taskItem = null;
		if(REID_TASK_TRACKING.equals(body.getTask_type())) {
			taskItem = reIDlabelTaskItemDao.queryLabelTaskItemByLabelTaskIdOderbyImageNameAsc(TokenManager.getUserTablePos(body.getUserId(), UserConstants.REID_TASK_SINGLE_TABLE),body.getTaskId());
		}else {
			taskItem = labelTaskItemDao.queryLabelTaskItemByLabelTaskId(TokenManager.getUserTablePos(body.getUserId(), UserConstants.LABEL_TASK_SINGLE_TABLE),body.getTaskId());
		}

		List<LabelTaskItem> autoTrackingList = new ArrayList<>();
		if(body.getEndIndex() > 0 || body.getStartIndex() > 0) {
			if(body.getEndIndex() > body.getStartIndex()) {
				for(int i = body.getStartIndex() - 1; i< taskItem.size() && i < body.getEndIndex(); i++) {
					autoTrackingList.add(taskItem.get(i));
				}
			}else {
				for(int i = body.getStartIndex() - 1; i >= 0 && i >= body.getEndIndex(); i--) {
					autoTrackingList.add(taskItem.get(i));
				}
			}
		}
		if(autoTrackingList.size() == 0) {
			autoTrackingList.addAll(taskItem);
		}
		//生成groundtruth.txt
		Map<String,Object> firstLableMap = new HashMap<>();
		Map<String,LabelTaskItem> itemMap = new HashMap<>();
		if(!isMultiTrack(algModel)) {//单目标跟踪需要人工标注帧
			String labelInfo = autoTrackingList.get(0).getLabel_info();
			List<Map<String,Object>> labelList = JsonUtil.getLabelList(labelInfo);

			if(labelList.isEmpty()) {
				throw new LabelSystemException("第一帧数据没有标注。");
			}

			firstLableMap = getFirstLabelMap(labelList,body.getLabel_id());
			writeGroundTxt(dataSetRootFile,firstLableMap);
			copyImgToPath(autoTrackingList,dataSetRootFile);
		}else {
			itemMap.putAll(copyImgToPath(autoTrackingList,new File(imageDir)));
		}
		
		String tmpScript = algModel.getExec_script();
		tmpScript = tmpScript.replace("{data_dir}", imageDir);
		tmpScript = tmpScript.replace("{data_name}", datasetName);
		String resultFileName = datasetName +".txt";
		
		if(isMultiTrack(algModel)) {
			tmpScript = tmpScript.replace("{output}", imageDir + "result.json");
			
			if(body.getLabel_option() == 11) {
				tmpScript = tmpScript.replace("{class_name}", "car");
				tmpScript = tmpScript.replace("models/mot17_half.pth", "models/coco_tracking.pth");
				tmpScript = tmpScript.replace("--num_class 1", "");
			}
			else {
				tmpScript = tmpScript.replace("{class_name}", "person");
			}
			resultFileName = "result.json";
		}
		

		final String script = tmpScript;
		final String rootPath = algInstance.getAlg_root_dir();

		//int timeSeconds = 24 * 3600;
		logger.info("exec script:" + script);
		try {
			ProcessExeUtil.execScript(script, rootPath, exceedTime);

			File resultFile = new File(imageDir,resultFileName); 
			if(resultFile.exists()) {
				logger.info("start to write tracking result.");
				//read result;
				if(isMultiTrack(algModel)) {
				    multiTrackingResult(body, itemMap, resultFile);
				}else {
					singleTrackingResult(body, autoTrackingList, firstLableMap, resultFile);
				}
				FileUtil.delDir(imageDir);
			}else {
				logger.info("not found result:" + resultFile.getAbsolutePath());
			}
		} catch (LabelSystemException e) {
			e.printStackTrace();
		}
	}


	private void multiTrackingResult(AutoLabelBody body, Map<String, LabelTaskItem> itemMap, File resultFile) {
		//多目标跟踪结果处理
		String content = FileUtil.getAllContent(resultFile.getAbsolutePath(), "utf-8");
		List<Map<String,Object>> resultList = JsonUtil.getLabelList(content);
		
		
		logger.info("start to save multi track result. size=" + resultList.size());
		for(Map<String,Object> resultMap : resultList) {
			for(Entry<String,Object> entry :resultMap.entrySet()) {
				String fileName = entry.getKey();
				Object value = entry.getValue();
				List<Map<String,Object>> currentLabelList = new ArrayList<>();
				if(value instanceof List) {
					List<?> valueList = (List)value;
					for(int i = 0; i < valueList.size() ;i++) {
						Object valueObj = valueList.get(i);
						if(valueObj instanceof Map) {
							Map<String,Object> label = (Map<String,Object>)valueObj;
							//specialDeal(label);
							if(REID_TASK_TRACKING.equals(body.getTask_type())) {
								label.put(Constants.REID_KEY, label.get("id"));
							}
							currentLabelList.add(label);
						}									
					}
					
				}
				logger.info("fileName=" +fileName + " currentLabelList=" + JsonUtil.toJson(currentLabelList));
				LabelTaskItem item = itemMap.get(fileName);
				if(item != null) {
					saveLabel(body, item, currentLabelList);
				}
			}
		}
	}


//	private void specialDeal(Map<String, Object> resultMap) {
//		if(resultMap.containsKey("bbox")) {
//			resultMap.put("box", resultMap.remove("bbox"));
//		}
//		if(resultMap.containsKey("tracking_id")) {
//			resultMap.put("id", resultMap.remove("tracking_id"));
//		}
//		if(resultMap.containsKey("class")) {
//			resultMap.put("class_name", resultMap.remove("class"));
//		}
//		resultMap.remove("ct");
//		resultMap.remove("tracking");
//	}


	private void singleTrackingResult(AutoLabelBody body, List<LabelTaskItem> autoTrackingList,
			Map<String, Object> firstLableMap, File resultFile) {
		List<String> allLine = FileUtil.getAllLineList(resultFile.getAbsolutePath(), "utf-8");
		for(int i = 1; i <allLine.size(); i++) {//第一帧已经标注过了，不需要再标注。
			logger.info("line=" + allLine.get(i));
			String line = allLine.get(i);
			Map<String,Object> newLabelMap = getLabelInfo(line,firstLableMap);
			LabelTaskItem item = autoTrackingList.get(i);
			List<Map<String,Object>> currentLabelList = new ArrayList<>();
			if(body.getLabel_option() == 1) {//清除已有的标注。
				currentLabelList.add(newLabelMap);
			}else {//合并已有的标注
				currentLabelList = JsonUtil.getLabelList(item.getLabel_info());
				mergeLabel(currentLabelList,newLabelMap);
			}

			saveLabel(body, item, currentLabelList);

		}
	}


	private void saveLabel(AutoLabelBody body, LabelTaskItem item, List<Map<String, Object>> currentLabelList) {
		Map<String,Object> paramMap = new HashMap<>();
		String time = TimeUtil.getCurrentTimeStr();

		paramMap.put("id", item.getId());
		paramMap.put("label_info", JsonUtil.toJson(currentLabelList));
		paramMap.put("label_status", Constants.LABEL_TASK_STATUS_FINISHED);
		paramMap.put("item_add_time", time);
		
		
		if(REID_TASK_TRACKING.equals(body.getTask_type())) {
			paramMap.put("user_id", TokenManager.getUserTablePos(body.getUserId(), UserConstants.REID_TASK_SINGLE_TABLE));
			reIDlabelTaskItemDao.updateLabelTaskItem(paramMap);
		}else {
			paramMap.put("user_id", TokenManager.getUserTablePos(body.getUserId(), UserConstants.LABEL_TASK_SINGLE_TABLE));
			labelTaskItemDao.updateLabelTaskItem(paramMap);
		}
	}


	private void mergeLabel(List<Map<String, Object>> currentLabelList, Map<String, Object> newLabelMap) {
		int id = 0;
		for(Map<String,Object> labelMap : currentLabelList) {
			Object obj = labelMap.get("id");
			if(obj != null) {
				try {
					int objIntId = 2;
					if(obj instanceof Double) {
						objIntId = (int)Double.parseDouble(obj.toString());
					}else if(obj instanceof Integer) {
						objIntId = Integer.parseInt(obj.toString());
					}else {
						objIntId = (int)Double.parseDouble(obj.toString());
					}
					if(id < objIntId) {
						id = objIntId;
					}
				}catch (Exception e) {
					logger.info("id is not int, id=" + obj.toString());
				}
			}

		}
		newLabelMap.put("id", id + 1);
		currentLabelList.add(newLabelMap);
	}


	private Map<String, Object> getFirstLabelMap(List<Map<String, Object>> labelList, int label_id) {
		for(Map<String,Object> labelMap : labelList) {
			if(labelMap.get("id") != null) {
				if(String.valueOf(label_id).equals(getId(labelMap.get("id")))) {
					return labelMap;
				}
			}
		}
		return labelList.get(0);
	}

	private String getId(Object obj) {
		int objIntId = 0;
		if(obj instanceof Double) {
			objIntId = (int)Double.parseDouble(obj.toString());
		}else if(obj instanceof Integer) {
			objIntId = Integer.parseInt(obj.toString());
		}else {
			objIntId = (int)Double.parseDouble(obj.toString());
		}
		return String.valueOf(objIntId);
	}

	private Map<String,Object>  getLabelInfo(String line,Map<String,Object> firstLabel) {
		Map<String,Object> label = new HashMap<>();


		String tmp[] = line.split(",");
		List<String> boxList = new ArrayList<>();

		int xmin = (int)Double.parseDouble(tmp[0]);
		int ymin = (int)Double.parseDouble(tmp[1]);
		int xmax =  (int)(Double.parseDouble(tmp[2]) + Double.parseDouble(tmp[0]));
		int ymax = (int)(Double.parseDouble(tmp[3]) + Double.parseDouble(tmp[1]));

		boxList.add(String.valueOf(xmin));
		boxList.add(String.valueOf(ymin));
		boxList.add(String.valueOf(xmax));
		boxList.add(String.valueOf(ymax));

		label.put("box", boxList);
		label.put("class_name", firstLabel.get("class_name"));
		if(firstLabel.get("reId") != null) {
			label.put("reId", firstLabel.get("reId"));
		}
		label.put("id", "1");

		return label;
	}


	private Map<String,LabelTaskItem> copyImgToPath(List<LabelTaskItem> autoTrackingList, File dataSetRootFile) throws LabelSystemException {
		Map<String,LabelTaskItem> itemMap = new HashMap<>();
		
		String picturePath = dataSetRootFile.getAbsolutePath() + File.separator + "img";
		new File(picturePath).mkdir();
		DecimalFormat df = new DecimalFormat("00000000");
		for(int i = 0; i < autoTrackingList.size();i++) {
			LabelTaskItem item = autoTrackingList.get(i);
			String relate_url = item.getPic_image_field();
			String tmp[] = relate_url.split("/");
			int length = tmp.length;
			String name = df.format(i) + tmp[length-1].substring(tmp[length-1].lastIndexOf("."));
			String pictureName = picturePath + File.separator + name;
			fileService.downLoadFileFromMinioAndSetPictureName(tmp[length-2], tmp[length-1], pictureName);
			itemMap.put(name, item);
		}
		return itemMap;
	}


	private void writeGroundTxt(File dataSetRootFile, Map<String, Object> firstLableMap) throws LabelSystemException {
		String fileName = "groundtruth.txt";
		@SuppressWarnings("unchecked")
		List<Object> boxList = (List<Object>)firstLableMap.get("box");
		if(boxList != null) {
			int xmin = (getIntStr(String.valueOf(boxList.get(0))));
			int ymin = (getIntStr(String.valueOf(boxList.get(1))));
			int xmax = (getIntStr(String.valueOf(boxList.get(2))));
			int ymax = (getIntStr(String.valueOf(boxList.get(3))));
			String line = xmin + "," + ymin + "," + Math.abs(xmax-xmin) + "," + Math.abs(ymax-ymin);
			try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dataSetRootFile.getAbsolutePath(),fileName)))){
				writer.write(line);
				writer.newLine();
				writer.write(line);
			}catch (Exception e) {
				logger.info(e.getMessage());
				throw new LabelSystemException("writer ." + fileName + " error.");
			}
		}else {
			throw new LabelSystemException("第一帧数据没有标注。");
		}
	}

	private int getIntStr(String doubleStr) {
		return (int)Double.parseDouble(doubleStr);
	}


}
