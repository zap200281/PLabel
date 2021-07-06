package com.pcl.service.schedule.miaod;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.pcl.constant.Constants;
import com.pcl.constant.UserConstants;
import com.pcl.dao.AlgInstanceDao;
import com.pcl.dao.AlgModelDao;
import com.pcl.dao.LabelTaskDao;
import com.pcl.dao.LabelTaskItemDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.model.CreateTrainVal;
import com.pcl.pojo.mybatis.AlgInstance;
import com.pcl.pojo.mybatis.AlgModel;
import com.pcl.pojo.mybatis.LabelTask;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.service.LabelDataSetMerge;
import com.pcl.service.ObjectFileService;
import com.pcl.service.TokenManager;
import com.pcl.util.FileUtil;
import com.pcl.util.GpuInfoUtil;
import com.pcl.util.JsonUtil;
import com.pcl.util.ProcessExeUtil;
import com.pcl.util.TimeUtil;
import com.pcl.util.VocAnnotationsUtil;
import com.pcl.util.mmdetetcion.DataSetClassReplaceUtil;


@Configuration      //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling   // 2.开启定时任务
public class MIAODSchedule {

	@Autowired
	private LabelTaskDao labelTaskDao;
	
	@Autowired
	private LabelTaskItemDao labelTaskItemDao;
	
	@Autowired
	private AlgModelDao algModelDao;

	@Autowired
	private AlgInstanceDao algInstanceDao;
	
	@Autowired
	private ObjectFileService fileService;
	
	@Autowired
	private VocAnnotationsUtil vocUtil;
	
	private static Logger logger = LoggerFactory.getLogger(MIAODSchedule.class);

	@Scheduled(cron = "0 0 20 ? * *")//每天晚上8点执行一次。
	public void doMIAOD() {
		logger.info("start to do miaod task.");
		List<Integer> availableGpuIdList;
		try {
			availableGpuIdList = GpuInfoUtil.getAvalibleGPUInfo(5);
			if(availableGpuIdList.size() == 0) {
				logger.info("Not gpu available. so return");
				return;
			}
			Map<String,Object> paramMap = new HashMap<>();
			paramMap.put("task_add_time", TimeUtil.getBeforeDayTimeStr(30));
			List<LabelTask> labelTaskList = labelTaskDao.queryLabelTaskAfterTime(paramMap);
			
			for(LabelTask labelTask : labelTaskList) {
				if(isEnableToMIAOD(paramMap, labelTask)) {
					//训练
					doMIAODTrain(labelTask);
				}
			}
			logger.info("end to do miaod task.");
		} catch (Exception e) {
			logger.info("miaod task exception.");
			e.printStackTrace();
		}
		
	}
	
	
	private void doMIAODTrain(LabelTask labelTask) {
		logger.info("start to do a LabelTask:" + JsonUtil.toJson(labelTask));
		int modelId = 60;
		AlgModel algModel = algModelDao.queryAlgModelById(modelId);
		if(algModel == null) {
			logger.info("algModel is null. return. id=" + modelId);
			return;
		}
		
		AlgInstance algInstance = algInstanceDao.queryAlgInstanceById(Integer.parseInt(algModel.getAlg_instance_id()));
		if(algInstance == null) {
			logger.info("algInstance is null. return. id=" + algModel.getAlg_instance_id());
			return;
		}

		String algRootPath = LabelDataSetMerge.getAlgRootPath(algInstance.getAlg_root_dir());
		String trainScript = algModel.getTrain_script();
		String workDir = algRootPath + "data" + File.separator  + TimeUtil.getCurrentTimeStrByyyyyMMddHHmmss() + File.separator;
		
		trainScript = trainScript.replace("{work_directory}", workDir);
		
		String dataSetPath = workDir +  "VOCdevkit" +File.separator +  "VOC2007" + File.separator;
		new File(dataSetPath).mkdirs();
		
		String dataSetTrainPath = dataSetPath  + "JPEGImages";
		String annoFilePath = dataSetPath + "Annotations" +File.separator;
		
		new File(dataSetTrainPath).mkdir();
		new File(annoFilePath).mkdir();
		
		List<LabelTaskItem> itemList = labelTaskItemDao.queryLabelTaskItemByLabelTaskId(TokenManager.getUserTablePos(labelTask.getUser_id(), UserConstants.LABEL_TASK_SINGLE_TABLE), labelTask.getId());
		int size = itemList.size();
		HashSet<String> typeSet = new HashSet<>();
		Map<String,LabelTaskItem> picturePathItem = new HashMap<>();
		List<String> trainList = new ArrayList<>();
		List<String> testList = new ArrayList<>();
		logger.info("item size=" + size);
		for(int i = 0; i < size ;i++) {
			LabelTaskItem item = itemList.get(i);
			List<Map<String,Object>> labelList = JsonUtil.getLabelList(item.getLabel_info());
			collectSupportType(labelList, typeSet);
			String relativeUrl = item.getPic_image_field();
			String tmp[] = relativeUrl.split("/");
			int length = tmp.length;
			String bucketName = tmp[length-2];
			String objectName = tmp[length-1];
			try {
				fileService.downLoadFileFromMinio(bucketName, objectName, dataSetTrainPath);
				picturePathItem.put(dataSetTrainPath + File.separator + objectName, item);
			} catch (LabelSystemException e) {
				e.printStackTrace();
			}
			//if(!labelList.isEmpty()) {
			writeXml(item, annoFilePath);
			//}
			if(!labelList.isEmpty()) {
				trainList.add(objectName);
			}else {
				testList.add(objectName);
			}
		}

		CreateTrainVal createTrainVal = new CreateTrainVal();
		createTrainVal.createMIAODTrainVal(dataSetPath,trainList,testList);
		
		int numClasses = typeSet.size();
		trainScript = trainScript.replace("{num_classes}", String.valueOf(numClasses));
		
		//修改 mmdet/datasets/voc.py中的classes
		DataSetClassReplaceUtil dataSetUtil = new DataSetClassReplaceUtil();
		List<String> typeList = new ArrayList<>();
		typeList.addAll(typeSet);
		logger.info("typeList=" + JsonUtil.toJson(typeList));
		dataSetUtil.replaceClass(typeList, algRootPath + "mmdet/datasets/voc.py");
		
		try {
			ProcessExeUtil.execScript(trainScript, algRootPath, 3600 * 12);//晚上8点到早上8点，12个小时
			
			String resultJson = workDir + "result.json";
			if(new File(resultJson).exists()) {
				logger.info("deal resultJson file." + resultJson);
				String content = FileUtil.getAllContent(resultJson, "utf-8");
				Map<String,Object> fileScoreMap = JsonUtil.getMap(content);
				
				for(Entry<String,Object> entry : fileScoreMap.entrySet()) {
					double scoreD = (Double)entry.getValue();
					String fileName = entry.getKey();
					LabelTaskItem tmpItem = picturePathItem.get(fileName);
					if(tmpItem != null) {
						int score =(int)(scoreD * 1000000);
						Map<String,Object> paramMap = new HashMap<>();
						paramMap.put("id", tmpItem.getId());
						paramMap.put("display_order2", score);
						paramMap.put("item_add_time", TimeUtil.getCurrentTimeStr());
						labelTaskItemDao.updateLabelTaskItem(paramMap);
					}
				}
			}else {
				logger.info("not found resultJson file." + resultJson);
			}
		} catch (LabelSystemException e) {
			e.printStackTrace();
		}
	}
	
	private void collectSupportType(List<Map<String, Object>> labelList, HashSet<String> typeSet) {
		Iterator<Map<String, Object>> it = labelList.iterator();
		while(it.hasNext()){
			Map<String, Object> label = it.next();
				String type = (String)label.get("class_name");
				if(type != null) {
					typeSet.add(type);
				}
		}
	}

	private void writeXml(LabelTaskItem item,String annoFilePath) {
		Document doc = vocUtil.getXmlDocumentOrNotLabel(item,null,new HashMap<>());
		if(doc == null) {
			return;
		}
		String relativeFileName = item.getPic_image_field();
		String fileName = relativeFileName.substring(relativeFileName.lastIndexOf("/") +1);
		fileName = fileName.substring(0,fileName.lastIndexOf(".")) +  ".xml";
		
		try (FileWriter fileWriter = new FileWriter(new File(annoFilePath,fileName))){
			OutputFormat format = new OutputFormat("\t", true);
			format.setTrimText(true);//去掉原来的空白(\t和换行和空格)！
			XMLWriter writer = new XMLWriter(fileWriter, format);
			// 把document对象写到out流中。
			writer.write(doc);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private boolean isEnableToMIAOD(Map<String, Object> paramMap, LabelTask labelTask) {
		if(labelTask.getTask_flow_type() == Constants.LABEL_TASK_FLOW_TYPE_MIAOD) {
			if(labelTask.getTotal_picture() >= 700) {//大于1000张
				Map<String,Object> tmpParamMap = new HashMap<>();
				tmpParamMap.put("user_id", TokenManager.getUserTablePos(labelTask.getUser_id(), UserConstants.LABEL_TASK_SINGLE_TABLE));
				tmpParamMap.put("list", Arrays.asList(labelTask.getId()));
				logger.info("tmpParamMap=" + tmpParamMap);
				List<Map<String,Object>> labelTaskStatusList = labelTaskItemDao.queryLabelTaskStatusByLabelTaskId(tmpParamMap);
				for(Map<String,Object> map : labelTaskStatusList) {
					int total = Integer.parseInt(map.get("total").toString());
					int label_not_finished = Integer.parseInt(map.get("label_not_finished").toString());
					logger.info("the task finished item:" + (total - label_not_finished));
//					if(total - label_not_finished > 100) {
//						return true;
//					}
				}
				return true;
			}else {
				logger.info("the task total picture is too small. " + labelTask.getTotal_picture());
			}
			
		}
		return false;
	}
	
}
