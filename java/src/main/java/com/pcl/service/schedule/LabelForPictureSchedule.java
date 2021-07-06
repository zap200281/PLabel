package com.pcl.service.schedule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.pcl.constant.Constants;
import com.pcl.constant.UserConstants;
import com.pcl.dao.AlgInstanceDao;
import com.pcl.dao.AlgModelDao;
import com.pcl.dao.DataSetDao;
import com.pcl.dao.LabelTaskItemDao;
import com.pcl.dao.PrePredictTaskDao;
import com.pcl.dao.PrePredictTaskResultDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.DcmObj;
import com.pcl.pojo.mybatis.AlgInstance;
import com.pcl.pojo.mybatis.AlgModel;
import com.pcl.pojo.mybatis.DataSet;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.pojo.mybatis.PrePredictTask;
import com.pcl.pojo.mybatis.PrePredictTaskResult;
import com.pcl.service.LabelDataSetMerge;
import com.pcl.service.ObjectFileService;
import com.pcl.service.TokenManager;
import com.pcl.util.DcmUtil;
import com.pcl.util.FileUtil;
import com.pcl.util.FingerPrint;
import com.pcl.util.GpuInfoUtil;
import com.pcl.util.JsonUtil;
import com.pcl.util.ProcessExeUtil;
import com.pcl.util.TimeUtil;
import com.pcl.util.VocAnnotationsUtil;

@Service
public class LabelForPictureSchedule {

	private static Logger logger = LoggerFactory.getLogger(LabelForPictureSchedule.class);

	private ArrayBlockingQueue<PrePredictTask> queue = new ArrayBlockingQueue<>(100000);

	@Autowired
	private AlgModelDao algModelDao;

	@Autowired
	private ObjectFileService fileService;

	@Autowired
	private AlgInstanceDao algInstanceDao;

	@Autowired
	private PrePredictTaskResultDao prePredictTaskResultDao;

	@Autowired
	private PrePredictTaskDao prePredictTaskDao;

	@Autowired
	private DataSetDao dataSetDao;

	@Autowired
	private LabelTaskItemDao labelTaskItemDao;

	@Value("${server.port}")
	private String port;

	@Value("${msgresttype:https}")
	private String msgresttype;

	@Value("${msgrestip:127.0.0.1}")
	private String msgrestip;

	@Value("${server.enable.gpu:true}")
	private boolean enable = true;

	@Autowired
	private VocAnnotationsUtil vocAnnotation;

	private final static double SCORE_THRESHHOLD = 0.45;

	private Gson gson = new Gson();

	private  static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(4,8,100,TimeUnit.MINUTES,new ArrayBlockingQueue<Runnable>(2000));

	LabelDataSetMerge dataSetMerge = new LabelDataSetMerge();

	public boolean addTask(PrePredictTask prePredictTask) {
		return queue.offer(prePredictTask);
	}

	@PostConstruct
	public void init() {

		if(!enable) {
			logger.info("no gpu, so not start to run picture schedule.");
			return;
		}

		logger.info("start to init queue : LabelForPictureSchedule ");
		//从数据库加载未完成的任务继续运行。
		loadTaskFromDb();

		logger.info("start to execute runnable : LabelForPictureSchedule ");

		logger.info("user.name=" + System.getProperty("user.name"));
		logger.info("user.home=" + System.getProperty("user.home"));
		logger.info("user.dir=" + System.getProperty("user.dir"));
		logger.info("sun.jnu.encoding=" + System.getProperty("sun.jnu.encoding"));

		new Thread(new Runnable() {

			@Override
			public void run() {
				while(true) {
					try {
						//判断是否有可用的GPU 
						List<Integer> availableGpuIdList = GpuInfoUtil.getAvalibleGPUInfo(5);
						if(availableGpuIdList.size() == 0) {
							logger.info("Not gpu available. wait 10 second.");

							Iterator<PrePredictTask> iterator = queue.iterator();

							while(iterator.hasNext()) {
								PrePredictTask task = iterator.next();
								Map<String,Object> paramMap = new HashMap<>();
								paramMap.put("id", task.getId());
								paramMap.put("task_status", Constants.PREDICT_TASK_STATUS_WAIT_GPU);
								paramMap.put("task_status_desc", "No GPU Available.");
								prePredictTaskDao.updatePrePredictTaskStatus(paramMap);
								//retrainTaskDao.updateRetrainTask(paramMap);
							}
							waitSecond(10);
							continue;
						}else {
							for(Integer gpuInt : availableGpuIdList) {
								PrePredictTask prePredictTask = queue.take();

								PrePredictTask dbPrePredictTask  = prePredictTaskDao.queryPrePredictTaskById(prePredictTask.getId());
								if(dbPrePredictTask.getTask_status() != Constants.PREDICT_TASK_STATUS_WAIT_GPU) {
									logger.info("not deal the task, the task is progressing.");
									continue;//已经在运行中了，跳过。
								}
								if(prePredictTask.getAlg_model_id() > 100) {
									threadPool.execute(()->{exeLabelByPythonTmp(dbPrePredictTask, gpuInt);});
								}else {
									threadPool.execute(()->{exeLabelByPython(prePredictTask,gpuInt);});
								}
								
							}
						}
					}catch (Exception e) {
						logger.info("Failed to predict label picture.");
						e.printStackTrace();
					}
				}

			}
		},"LabelSchedule").start();
	}

	protected void waitSecond(int second) {
		try {
			Thread.sleep(second * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void loadTaskFromDb() {
		List<PrePredictTask> notStartTaskList = prePredictTaskDao.queryPrePredictTaskByStatus(String.valueOf(Constants.PREDICT_TASK_STATUS_WAIT_GPU));
		for(PrePredictTask prePredictTask  : notStartTaskList) {
			addTask(prePredictTask);
		}
	}
	
	
	private void exeLabelByPythonTmp(PrePredictTask prePredictTask,Integer gpuNum) {
		long start = System.currentTimeMillis();
		String script = getScript(prePredictTask.getAlg_model_id());
		logger.info("exe script:" + script);
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", prePredictTask.getId());
		String tmpImageDir = null;
		String existFramePath = null;
		try {
			//获取执行的cmd命令根目录。
			String algRootPath = getAlgRootPath(prePredictTask);
			logger.info("start download picture.");
			String dataSetId = prePredictTask.getDataset_id();
			DataSet dataSet = dataSetDao.queryDataSetById(dataSetId);
			if(dataSet == null) {
				throw new LabelSystemException("自动标注所选择的数据集不存在或者已经被删除。");
			}
			tmpImageDir = algRootPath  + System.nanoTime() ;
			String imageDir =  tmpImageDir  + File.separator + "VOC2007" + File.separator + Constants.JPEGIMAGES +File.separator;
			FileUtil.delDir(imageDir);

			File imageDirFile = new File(imageDir);
			imageDirFile.mkdirs();

			Map<String,String> pictureNameMap = new HashMap<>();
			Map<String,String> pictureNameForItemMap = new HashMap<>();
			
				//下载图片
			existFramePath = downLoadPicture(prePredictTask, paramMap, dataSetId, dataSet, imageDir,
					pictureNameMap, pictureNameForItemMap);
			

			paramMap.put("task_status", Constants.PREDICT_TASK_STATUS_PROGRESSING);
			paramMap.put("task_status_desc", "开始进行标注");
			prePredictTaskDao.updatePrePredictTaskStatus(paramMap);

			HashMap<String, PrePredictTaskResult> preResultMap = new HashMap<>();
		
			String tableNamePos = TokenManager.getUserTablePos(prePredictTask.getUser_id(), UserConstants.PREDICT_SINGLE_TABLE);
			
			Map<String,Object> itemParamMap = new HashMap<>();
			itemParamMap.put("label_task_id", dataSetId);
			itemParamMap.put("user_id", TokenManager.getUserTablePos(prePredictTask.getUser_id(), UserConstants.LABEL_TASK_SINGLE_TABLE));
			logger.info("itemParamMap ==" + itemParamMap.toString());
			int count = labelTaskItemDao.queryLabelTaskItemPageCountByLabelTaskId(itemParamMap);
			logger.info("query count ==" + count);
			int pageSize = 100;
			itemParamMap.put("pageSize", pageSize);

			for(int i = 0; i < (count / pageSize) + 1; i++) {
				itemParamMap.put("currPage", i * pageSize);
				List<LabelTaskItem> list = labelTaskItemDao.queryLabelTaskItemPageByLabelTaskId(itemParamMap);
				for(LabelTaskItem item : list) {
					PrePredictTaskResult result = new PrePredictTaskResult();
					result.setId(UUID.randomUUID().toString().replaceAll("-",""));
					
					
					List<Map<String, Object>> labelList = JsonUtil.getLabelList(item.getLabel_info());
					if(labelList.isEmpty()) {
						Map<String, Object> map = new HashMap<>();
						map.put("id", "0");
						map.put("class_name", "person");
						String widthHeight = fileService.getImageWidthHeight(item.getPic_image_field());
						if(widthHeight != null && widthHeight.length() > 0) {
							String tmps[] = widthHeight.split(",");
							map.put("box", Arrays.asList("0","0",tmps[0],tmps[1]));
						}else {
							map.put("box", Arrays.asList("0","0","5","5"));
						}
						
						labelList.add(map);
					}
					
					result.setLabel_info(JsonUtil.toJson(labelList));
					result.setItem_add_time(TimeUtil.getCurrentTimeStr());
					result.setPre_predict_task_id(prePredictTask.getId());
					result.setPic_image_field(item.getPic_image_field());
					result.setUser_id(tableNamePos);
					prePredictTaskResultDao.addPrePredictTaskResult(result);
					
					String diskImagePath = imageDir + pictureNameForItemMap.get(item.getPic_image_field());
					preResultMap.put(diskImagePath, result);
				}
			}
			
			PredictPersonProperty personProperty = new PredictPersonProperty(algModelDao,prePredictTaskResultDao,algInstanceDao);
			personProperty.dealDistinguishPersonPropertyResult(gpuNum, preResultMap, prePredictTask,prePredictTask.getAlg_model_id());
		


			//更新预检任务状态
			paramMap.put("task_status", Constants.PREDICT_TASK_STATUS_FINISHED);
			paramMap.put("task_status_desc", "共耗时" + ((System.currentTimeMillis() - start) / 1000) + "s");
			logger.info("Finished label picture task, cost:" + ((System.currentTimeMillis() - start) / 1000) + "s");
		} catch (Exception e) {
			e.printStackTrace();
			paramMap.put("task_status", Constants.TASK_STATUS_EXCEPTION);
			paramMap.put("task_status_desc", e.getMessage());
		}finally {
			if(tmpImageDir != null) {
				FileUtil.delDir(tmpImageDir);
			}
			if(existFramePath != null) {
				FileUtil.delDir(existFramePath);
			}
		}
		prePredictTaskDao.updatePrePredictTaskStatus(paramMap);
	}
	

	private void exeLabelByPython(PrePredictTask prePredictTask,Integer gpuNum) {
		long start = System.currentTimeMillis();
		String script = getScript(prePredictTask.getAlg_model_id());
		logger.info("exe script:" + script);
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", prePredictTask.getId());
		String tmpImageDir = null;
		String existFramePath = null;
		try {
			//获取执行的cmd命令根目录。
			String algRootPath = getAlgRootPath(prePredictTask);
			logger.info("start download picture.");
			String dataSetId = prePredictTask.getDataset_id();
			DataSet dataSet = dataSetDao.queryDataSetById(dataSetId);
			if(dataSet == null) {
				throw new LabelSystemException("自动标注所选择的数据集不存在或者已经被删除。");
			}
			tmpImageDir = algRootPath  + System.nanoTime() ;
			String imageDir =  tmpImageDir  + File.separator + "VOC2007" + File.separator + Constants.JPEGIMAGES +File.separator;
			FileUtil.delDir(imageDir);

			File imageDirFile = new File(imageDir);
			imageDirFile.mkdirs();

			Map<String,String> pictureNameMap = new HashMap<>();
			Map<String,String> pictureNameForItemMap = new HashMap<>();
			boolean predictVideo = false;
			if(dataSet.getTotal() == 0 && dataSet.getDataset_type() == Constants.DATASET_TYPE_VIDEO) {
				//预处理视频
				predictVideo = true;
				PredictVideo.preDeal(dataSet, imageDir, fileService);
				logger.info("finished video draw frame.");
			}else {
				//下载图片
				existFramePath = downLoadPicture(prePredictTask, paramMap, dataSetId, dataSet, imageDir,
						pictureNameMap, pictureNameForItemMap);
			}

			long tmp = System.currentTimeMillis();
			if(prePredictTask.getDelete_similar_picture() > 0) {
				deleteSimilarPicture(prePredictTask, paramMap, imageDir, pictureNameMap, pictureNameForItemMap, tmp);
			}
			File files[] = imageDirFile.listFiles();
			int length = 0;
			if(files != null) {
				length = files.length;
			}

			paramMap.put("task_status", Constants.PREDICT_TASK_STATUS_PROGRESSING);
			paramMap.put("task_status_desc", "开始进行标注");
			prePredictTaskDao.updatePrePredictTaskStatus(paramMap);

			String outputDir = tmpImageDir + File.separator + "output" + File.separator;
			FileUtil.delDir(outputDir);
			new File(outputDir).mkdirs();
			script += " --image_dir " + imageDir;
			script += " --output_dir " + outputDir;
			script += " --taskid " + prePredictTask.getId() + "##" + prePredictTask.getUser_id();
			script += " --msgrest "+ msgresttype + "://" + msgrestip + ":" + port;
			script += " --gpu " + gpuNum;

			//调用命令
			ProcessExeUtil.execScript(script, algRootPath, getTimeOutTime(prePredictTask, length));

			//处理结果文件
			dealResultJson(prePredictTask, gpuNum, tmpImageDir, algRootPath, imageDir, pictureNameMap, predictVideo,
					outputDir, length);


			//更新预检任务状态
			paramMap.put("task_status", Constants.PREDICT_TASK_STATUS_FINISHED);
			paramMap.put("task_status_desc", "共耗时" + ((System.currentTimeMillis() - start) / 1000) + "s");
			logger.info("Finished label picture task, cost:" + ((System.currentTimeMillis() - start) / 1000) + "s");
		} catch (Exception e) {
			e.printStackTrace();
			paramMap.put("task_status", Constants.TASK_STATUS_EXCEPTION);
			paramMap.put("task_status_desc", e.getMessage());
		}finally {
			if(tmpImageDir != null) {
				FileUtil.delDir(tmpImageDir);
			}
			if(existFramePath != null) {
				FileUtil.delDir(existFramePath);
			}
		}
		prePredictTaskDao.updatePrePredictTaskStatus(paramMap);
	}

	//返回秒
	private int getTimeOutTime(PrePredictTask prePredictTask,int pictureTotal) {
		AlgModel algModel = algModelDao.queryAlgModelById(prePredictTask.getAlg_model_id());
		int ratio = 2;
		if(algModel.getA_picture_cost_time() > 0){  //分割
			ratio = algModel.getA_picture_cost_time();
		}
		return pictureTotal * ratio + 30;
	}

	private String getAlgRootPath(PrePredictTask prePredictTask) throws LabelSystemException {
		AlgModel algModel = algModelDao.queryAlgModelById(prePredictTask.getAlg_model_id());

		if(algModel == null) {
			logger.info("the algInstance is null. modelId=" + prePredictTask.getAlg_model_id());
			throw new LabelSystemException("自动标注所选择的算法模型不存在。");
		}

		AlgInstance algInstance = algInstanceDao.queryAlgInstanceById(Integer.parseInt(algModel.getAlg_instance_id()));
		if(algInstance == null) {
			logger.info("the algInstance is null. algInstanceId=" + algModel.getAlg_instance_id());
			throw new LabelSystemException("自动标注所选择的算法模型不存在。");
		}
		String algRootPath = LabelDataSetMerge.getAlgRootPath(algInstance.getAlg_root_dir());
		return algRootPath;
	}

	private void dealResultJson(PrePredictTask prePredictTask, Integer gpuNum, String tmpImageDir, String algRootPath,
			String imageDir, Map<String, String> pictureNameMap, boolean predictVideo, String outputDir, int length)
					throws LabelSystemException {
		String jsonFile = outputDir + "result.json";
		File jsonResultFile = new File(jsonFile);
		if(predictVideo) {
			PredictVideo.dealResultJson(prePredictTask, outputDir, length, jsonResultFile, fileService, prePredictTaskResultDao);
		}else {
			//读取Json文件，并保存
			PredictCarDistinguish objectDistinguish = new PredictCarDistinguish(algModelDao,prePredictTaskResultDao);
			HashMap<String,PrePredictTaskResult> preResultMap = new HashMap<>();
			if(jsonResultFile.exists()) {
				saveResultToDb(prePredictTask, tmpImageDir, imageDir, pictureNameMap, objectDistinguish, jsonFile,
						preResultMap);
			}
			logger.info("getNeedToDistiguishTypeOrColor=" + prePredictTask.getNeedToDistiguishTypeOrColor());
			if(prePredictTask.getNeedToDistiguishTypeOrColor() > 0) {
				if(isCarDistinguish(prePredictTask.getNeedToDistiguishTypeOrColor())) {
					objectDistinguish.dealDistinguishCarPropertyResult(gpuNum, tmpImageDir, algRootPath, outputDir, length, preResultMap, prePredictTask);
				}else {
					PredictPersonProperty personProperty = new PredictPersonProperty(algModelDao,prePredictTaskResultDao,algInstanceDao);
					personProperty.dealDistinguishPersonPropertyResult(gpuNum, preResultMap, prePredictTask,prePredictTask.getNeedToDistiguishTypeOrColor());
				}
			}
		}
	}

	private boolean isCarDistinguish(int id) {
		if(id < 100 && id > 0) {
			return true;
		}
		return false;
	}
	
	private String downLoadPicture(PrePredictTask prePredictTask, Map<String, Object> paramMap,
			String dataSetId, DataSet dataSet, String imageDir, Map<String, String> pictureNameMap,
			Map<String, String> pictureNameForItemMap) throws LabelSystemException, IOException {
		Map<String,Object> itemParamMap = new HashMap<>();
		itemParamMap.put("label_task_id", dataSetId);
		itemParamMap.put("user_id", TokenManager.getUserTablePos(prePredictTask.getUser_id(), UserConstants.LABEL_TASK_SINGLE_TABLE));
		logger.info("itemParamMap ==" + itemParamMap.toString());
		int count = labelTaskItemDao.queryLabelTaskItemPageCountByLabelTaskId(itemParamMap);
		logger.info("query count ==" + count);
		int pageSize = 100;
		itemParamMap.put("pageSize", pageSize);

		paramMap.put("task_status", Constants.PREDICT_TASK_STATUS_PROGRESSING);
		paramMap.put("task_status_desc", "下载图片中，总数量：" + count + "张");
		prePredictTaskDao.updatePrePredictTaskStatus(paramMap);

		return downDowdPictureFromObjectDb(dataSet, imageDir, pictureNameMap,
				pictureNameForItemMap, itemParamMap, count, pageSize);

	}


	private String downDowdPictureFromObjectDb( DataSet dataSet, String imageDir,
			Map<String, String> pictureNameMap, Map<String, String> pictureNameForItemMap,
			Map<String, Object> itemParamMap, int count, int pageSize) throws LabelSystemException, IOException {
		String json = dataSet.getMainVideoInfo();
		Map<String,Object> mainVideoMap = JsonUtil.getMap(json);
		String existFramePath = null;
		boolean isExistTmpFrame = false;
		if(mainVideoMap.get("tmpFramePath") != null) {
			existFramePath =  mainVideoMap.get("tmpFramePath").toString();
			if(new File(existFramePath).exists()) {
				isExistTmpFrame = true;
			}
		}
		int totalPictureSize = 0;
		for(int i = 0; i < (count / pageSize) + 1; i++) {
			itemParamMap.put("currPage", i * pageSize);
			List<LabelTaskItem> list = labelTaskItemDao.queryLabelTaskItemPageByLabelTaskId(itemParamMap);

			for(LabelTaskItem item : list) {
				String itemExtendName = item.getPic_image_field();
				itemExtendName = itemExtendName.substring(itemExtendName.lastIndexOf("."));
				String pictureName = imageDir + item.getId() + itemExtendName;
				totalPictureSize ++;
				String tmp[] = item.getPic_image_field().split("/");
				int length = tmp.length;
				File tmpPicFile = new File(existFramePath,tmp[length-1]);
				if(isExistTmpFrame && tmpPicFile.exists()) {
					//临时文件存在，重新命名即可。
					tmpPicFile.renameTo(new File(pictureName));	
					logger.info("rename " + tmp[length-1] +" to " + pictureName);
				}else {
					fileService.downLoadFileFromMinioAndSetPictureName(tmp[length-2], tmp[length-1], pictureName);
				}
				if(dataSet.getDataset_type() == Constants.DATASET_TYPE_DCM) {
					//convertto dcm
					DcmObj dcmObj = DcmUtil.getImageByDcmFile(pictureName);
					if(dcmObj != null && dcmObj.getImage() != null) {
						ImageIO.write(dcmObj.getImage(), "jpg", new File(pictureName));
					}
				}

				//logger.info("bucketname=" + tmp[length-2] + " objectname=" + tmp[length-1]);
				pictureNameMap.put(item.getId() + itemExtendName, item.getPic_image_field());
				pictureNameForItemMap.put(item.getPic_image_field(), item.getId() + itemExtendName);
			}
		}

		logger.info("download picture size=" + totalPictureSize);
		return existFramePath;
	}

	private void saveResultToDb(PrePredictTask prePredictTask, String tmpImageDir, String imageDir,
			Map<String, String> pictureNameMap, PredictCarDistinguish objectDistinguish, String jsonFile,
			HashMap<String, PrePredictTaskResult> preResultMap) {
		String jsonStr = FileUtil.getAllContent(jsonFile, "utf-8");
		Map<String,Object> map = JsonUtil.getMap(jsonStr);
		logger.info("delete exist predict task result.");
		String tableNamePos = TokenManager.getUserTablePos(prePredictTask.getUser_id(), UserConstants.PREDICT_SINGLE_TABLE);
		prePredictTaskResultDao.deletePrePredictTaskResultById(tableNamePos,prePredictTask.getId());

		logger.info("AUTO_DELETE_NO_LABEL_PICTURE=" + prePredictTask.getDelete_no_label_picture());

		for(Entry<String,Object> entry : map.entrySet()) {

			filterByThreshhold(prePredictTask, entry);

			if(needDeleteBlankPicture(entry, prePredictTask)) {
				continue;
			}

			String imagePath = pictureNameMap.remove(entry.getKey());
			String labelInfo = gson.toJson(entry.getValue());

			PrePredictTaskResult result = savePredictTaskResult(tableNamePos,labelInfo,prePredictTask.getId(),imagePath);
			String diskImagePath = imageDir + entry.getKey();
			preResultMap.put(diskImagePath, result);

			if(isCarDistinguish(prePredictTask.getNeedToDistiguishTypeOrColor())) {
				String xmlPath = vocAnnotation.getXmlPathByImagePath(diskImagePath);
				new File(xmlPath).getParentFile().mkdir();
				objectDistinguish.saveToXml(labelInfo,diskImagePath,xmlPath,vocAnnotation);
			}
		}
		if(isCarDistinguish(prePredictTask.getNeedToDistiguishTypeOrColor())) {
			String testTxtPath = tmpImageDir + "/VOC2007/ImageSets/Main/test.txt";
			objectDistinguish.saveImageSetTestTxt(testTxtPath, map);
		}

		if(Constants.AUTO_DELETE_NO_LABEL_PICTURE_PRI == prePredictTask.getDelete_no_label_picture()) {
			for(Entry<String,String> entry : pictureNameMap.entrySet()) {
				String itemId = entry.getKey().substring(0,entry.getKey().lastIndexOf("."));
				logger.info("delete primitive record:" + itemId);
				labelTaskItemDao.deleteLabelTaskItemById(TokenManager.getUserTablePos(prePredictTask.getUser_id(), UserConstants.LABEL_TASK_SINGLE_TABLE),itemId);
			}
		}
	}

	private void deleteSimilarPicture(PrePredictTask prePredictTask, Map<String, Object> paramMap, String imageDir,
			Map<String, String> pictureNameMap, Map<String, String> pictureNameForItemMap, long tmp)
					throws IOException {
		double similar = 1;
		if(1 == prePredictTask.getDelete_similar_picture()) {
			similar = 0.98;
		}else if(2 == prePredictTask.getDelete_similar_picture()) {
			similar = 0.97;
		}

		paramMap.put("task_status", Constants.PREDICT_TASK_STATUS_PROGRESSING);
		paramMap.put("task_status_desc", "开始删除相似度大于" + similar + "图片");
		prePredictTaskDao.updatePrePredictTaskStatus(paramMap);


		ArrayList<String> list = new ArrayList<>();
		list.addAll(pictureNameMap.values());
		int countSimilar = 0;
		Collections.sort(list);
		FingerPrint first = new FingerPrint(ImageIO.read(new File(imageDir,pictureNameForItemMap.get(list.get(0))))); 
		first.setName(list.get(0));

		for(int i = 1; i <list.size(); i++) {
			logger.info("start to deal similar  " + i + " picture");
			File pictureFile = new File(imageDir,pictureNameForItemMap.get(list.get(i)));
			FingerPrint second = new FingerPrint(ImageIO.read(pictureFile));
			second.setName(list.get(i));
			double si = first.compare(second);
			if(si > similar) {
				countSimilar++;
				pictureFile.delete();
				logger.info(second.getName() + " and " +  first.getName() + " similar is:" + first.compare(second)*100 + " %");
			}else {
				first = second;
			}
		}
		logger.info("delete similar picture total is :" + countSimilar + " cost=" + (System.currentTimeMillis() - tmp));
	}

	private void filterByThreshhold(PrePredictTask prePredictTask, Entry<String, Object> entry) {
		if(prePredictTask.getScore_threshhold() >  SCORE_THRESHHOLD) {
			List<Map<String,Object>> labelMap = (List<Map<String,Object>>)entry.getValue();
			for(int i = 0; i < labelMap.size(); i++) {
				Map<String,Object> label = labelMap.get(i);
				Object score = label.get("score");
				if(score != null) {
					double scored = Double.parseDouble(score.toString());
					if(scored < prePredictTask.getScore_threshhold()) {
						//如果得分小于用户填写的分值门限，则直接丢弃。
						labelMap.remove(i);
						i--;
					}
				}
			}

		}

	}

	private boolean needDeleteBlankPicture(Entry<String,Object> entry,PrePredictTask prePredictTask) {

		if(Constants.AUTO_DELETE_NO_LABEL_PICTURE == prePredictTask.getDelete_no_label_picture()
				||
				Constants.AUTO_DELETE_NO_LABEL_PICTURE_PRI == prePredictTask.getDelete_no_label_picture()) {
			logger.info("entry.getKey()" + entry.getKey() + "  entry.getValue()=" + entry.getValue());
			if(entry.getValue() != null && entry.getValue() instanceof List) {

				if(((List)entry.getValue()).size() == 0) {
					if(Constants.AUTO_DELETE_NO_LABEL_PICTURE_PRI == prePredictTask.getDelete_no_label_picture()) {
						String itemId = entry.getKey().substring(0,entry.getKey().lastIndexOf("."));
						logger.info("delete primitive record:" + itemId);
						labelTaskItemDao.deleteLabelTaskItemById(TokenManager.getUserTablePos(prePredictTask.getUser_id(), UserConstants.LABEL_TASK_SINGLE_TABLE),itemId);
					}
					return true;
				}
			}else {
				if(Constants.AUTO_DELETE_NO_LABEL_PICTURE_PRI == prePredictTask.getDelete_no_label_picture()) {
					String itemId = entry.getKey().substring(0,entry.getKey().lastIndexOf("."));
					logger.info("delete primitive record:" + itemId);
					labelTaskItemDao.deleteLabelTaskItemById(TokenManager.getUserTablePos(prePredictTask.getUser_id(), UserConstants.LABEL_TASK_SINGLE_TABLE),itemId);
				}
				return true;
			}
		}
		return false;
	}


	private PrePredictTaskResult savePredictTaskResult(String tableNamePos,String labelInfo, String preditcTaskid, String imagePath) {
		PrePredictTaskResult result = new PrePredictTaskResult();
		result.setId(UUID.randomUUID().toString().replaceAll("-",""));
		result.setLabel_info(labelInfo);
		result.setItem_add_time(TimeUtil.getCurrentTimeStr());
		result.setPre_predict_task_id(preditcTaskid);

		result.setPic_image_field(imagePath);
		result.setUser_id(tableNamePos);

		prePredictTaskResultDao.addPrePredictTaskResult(result);

		return result;
	}


	private String getScript(int algModelId) {
		AlgModel algModel = algModelDao.queryAlgModelById(algModelId);
		if(algModel != null) {
			String execScript = algModel.getExec_script();
			if(algModel.getConf_path() != null) {
				execScript = execScript.replace("{configPath}", algModel.getConf_path());
			}
			if(algModel.getModel_url() != null) {
				execScript = execScript.replace("{modelPath}", algModel.getModel_url());
			}
			return execScript;
		}

		return null;
	}

}
