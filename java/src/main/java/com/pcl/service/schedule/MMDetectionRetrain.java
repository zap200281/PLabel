package com.pcl.service.schedule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pcl.constant.Constants;
import com.pcl.constant.UserConstants;
import com.pcl.dao.AlgInstanceDao;
import com.pcl.dao.AlgModelDao;
import com.pcl.dao.LabelTaskDao;
import com.pcl.dao.LabelTaskItemDao;
import com.pcl.dao.PrePredictTaskDao;
import com.pcl.dao.RetrainTaskDao;
import com.pcl.dao.UserDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.model.CreateTrainVal;
import com.pcl.pojo.mybatis.AlgInstance;
import com.pcl.pojo.mybatis.AlgModel;
import com.pcl.pojo.mybatis.LabelTask;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.pojo.mybatis.PrePredictTask;
import com.pcl.pojo.mybatis.RetrainTask;
import com.pcl.pojo.mybatis.User;
import com.pcl.service.LabelDataSetMerge;
import com.pcl.service.MinioFileService;
import com.pcl.service.ObjectFileService;
import com.pcl.service.TokenManager;
import com.pcl.util.CocoAnnotationsUtil;
import com.pcl.util.FileUtil;
import com.pcl.util.JsonUtil;
import com.pcl.util.ProcessExeUtil;
import com.pcl.util.TimeUtil;
import com.pcl.util.VocAnnotationsUtil;
import com.pcl.util.mmdetetcion.DataSetClassReplaceUtil;
import com.pcl.util.mmdetetcion.EvaluationClassReplaceUtil;


@Service
public class MMDetectionRetrain extends ATask{

	private static Logger logger = LoggerFactory.getLogger(MMDetectionRetrain.class);

	@Autowired
	private AlgModelDao algModelDao;

	@Autowired
	private RetrainTaskDao retrainTaskDao;

	@Autowired
	private UserDao userDao;

	@Autowired
	private AlgInstanceDao algInstanceDao;

	@Autowired
	private LabelForPictureSchedule labelSchedule;

	@Autowired
	private PrePredictTaskDao prePredictTaskDao;

	@Autowired
	private LabelTaskItemDao labelTaskItemDao;
	
	@Autowired
	private LabelTaskDao labelTaskDao;

	@Autowired
	private ObjectFileService fileService;
	
	@Autowired
	private CocoAnnotationsUtil cocoAnnotationsUtil;
	
	@Autowired
	private VocAnnotationsUtil vocUtil;
	
	private final static String RETRAIN = "0";
	
	private final static String INIT = "1";
	
	private final static String DETECTION_TYPE_INPUT = "1";
	
	private final static String DETECTION_TYPE_LABEL = "0";//使用标注数据中的标注类型
	
	private final static String VOC_DATASET_CLASS_PATH = "/mmdet/datasets/voc.py";
	
	private final static String COCO_DATASET_CLASS_PATH = "/mmdet/datasets/coco.py";
	
	private final static String CLASS_NAME_DATASET_CLASS_PATH = "/mmdet/core/evaluation/class_names.py";

	@Override
	public void doExecute(RetrainTask retrainTask, List<Integer> availableGpuIdList) {
		logger.info("start to exe MMDetectionRetrain retrain task.  use gpu:" + availableGpuIdList.toString());
		
		String algRootPath = null;
		boolean isCoco = false;
		String dataSetPath = null;
		try {
			//更新数据库状态为  进行中
			updateTaskProgressing(retrainTask.getId(),retrainTaskDao);


			AlgModel algModel = algModelDao.queryAlgModelById(retrainTask.getAlg_model_id());
			if(algModel == null) {
				logger.info("algModel is null. return. id=" + retrainTask.getAlg_model_id());
				return;
			}
			
			AlgInstance algInstance = algInstanceDao.queryAlgInstanceById(Integer.parseInt(algModel.getAlg_instance_id()));
			if(algInstance == null) {
				logger.info("algInstance is null. return. id=" + algModel.getAlg_instance_id());
				return;
			}

			algRootPath = LabelDataSetMerge.getAlgRootPath(algInstance.getAlg_root_dir());

			String trainScript = algModel.getTrain_script();
			dataSetPath =  algRootPath + "data" +File.separator + "coco" +File.separator + TimeUtil.getCurrentTimeStrByyyyyMMddHHmmss() + File.separator;
			if(!isCoco) {
				dataSetPath =  algRootPath + "data" +File.separator + "VOCdevkit" +File.separator + TimeUtil.getCurrentTimeStrByyyyyMMddHHmmss() + File.separator + "VOC2007" + File.separator;
			}

			new File(dataSetPath).mkdirs();
			
			List<String> typeList = new ArrayList<>();
			
			String newConf = collectPictureToTrainDataSet(retrainTask, dataSetPath, algModel, algRootPath,isCoco,typeList);
			
			if(newConf.startsWith(algRootPath)) {
				newConf = newConf.substring(algRootPath.length());
			}
			
			updateTaskConf(retrainTask.getId(),newConf);
			
			logger.info("trainScript=" + trainScript + " newConf=" + newConf);
			trainScript = trainScript.replace("{configPath}", newConf);
			int gpunum = 0;
			if(availableGpuIdList.size() > 1) {
				if(availableGpuIdList.size() % 2 == 0) {
					gpunum = availableGpuIdList.size() - 2;
				}else {
					gpunum = availableGpuIdList.size() - 1;
				}
			}
			if(gpunum == 0) {
				gpunum = 1;
			}
			String	script =  trainScript.replace("{gpunum}", String.valueOf(gpunum));
			logger.info("script=" + script);

			ProcessExeUtil.execScript(script, algRootPath, 3600 * 24 * 2);

			//正常结束，更新状态
			updateTaskFinish(retrainTask.getId(),Constants.RETRAINTASK_STATUS_FINISHED,"",retrainTaskDao);

			//拷贝重训后的模型到 model目录下
			int newAlgModelId = copyLastModelToRightPath(retrainTask, algRootPath, algModel,newConf,typeList);

			logger.info("Succeed to update retrain task status to finished.");
			if(newAlgModelId != -1 && retrainTask.getPre_predict_task_id() != null) {
				PrePredictTask preTask = prePredictTaskDao.queryPrePredictTaskById(retrainTask.getPre_predict_task_id());
				if(preTask != null) {
					preTask.setAlg_model_id(newAlgModelId);
					//需要将关联的自动标注任务再次执行一下。
					logger.info("re build predict task, id=" + retrainTask.getAlg_model_id());
					labelSchedule.addTask(preTask);
				}
			}else {
				logger.info("error to rebuild auto label.");
			}
		}catch (Exception e) {
			logger.info(e.getMessage());
			e.printStackTrace();
		}finally {
			//删除拷贝过来的数据
			if(algRootPath != null) {
				logger.info("recover env,  delete the user train dataset.");
				FileUtil.delDir(dataSetPath);
			}
		}
	}

	private void updateTaskConf(String retrainTaskId,String conf) {
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", retrainTaskId);
		//更新数据库状态为  进行中
		paramMap.put("task_status", Constants.RETRAINTASK_STATUS_PROGRESSING);
		paramMap.put("confPath", conf);
		retrainTaskDao.updateRetrainTask(paramMap);
	}



	private int copyLastModelToRightPath(RetrainTask retrainTask, String algRootPath, AlgModel algModel,String newConf,List<String> typeList) {

		//String confPy = newConf;

		String dir = getWorkDir(algRootPath,newConf);
		if(dir != null) {
			String lastModel = FileUtil.getLastModifiedFile(dir, "epoch_", ".pth");
			File file = new File(lastModel);
			String relativeModelPath = "model" + File.separator + file.getParentFile().getName() + File.separator + TimeUtil.getCurrentDayStryyyyMMdd() + "_" + file.getName();
			String destModelPath = algRootPath + relativeModelPath;
			File destModelFile = new File(destModelPath);
			if(destModelFile.exists()) {
				destModelFile.delete();
			}else {
				destModelFile.getParentFile().mkdir();
			}
			FileUtil.copyFile(lastModel, destModelPath);

			AlgModel algModelSrc = algModelDao.queryAlgModelById(retrainTask.getAlg_model_id());

			User user = userDao.queryUserById(retrainTask.getUser_id());
			String userName = user.getUsername();

			//往模型库插入一条记录
			
			String modelName = algModelSrc.getModel_name() + "(" + userName + ")";
			if(retrainTask.getRetrain_model_name() != null) {
				modelName = retrainTask.getRetrain_model_name();
			}
			List<AlgModel> modelList = algModelDao.queryAlgModel(modelName);
			if(modelList != null && modelList.size() > 0) {
				for(AlgModel tmpAlgModel : modelList) {
					algModelDao.delete(tmpAlgModel.getId());
				}
			}

			AlgModel algModelDest = new AlgModel();
			algModelDest.setConf_path(newConf);
			algModelDest.setExec_script(algModelSrc.getExec_script());
			algModelDest.setModel_name(modelName);
			algModelDest.setModel_url(relativeModelPath);
			algModelDest.setTrain_script(algModelSrc.getTrain_script());
			algModelDest.setAlg_instance_id(algModelSrc.getAlg_instance_id());
			algModelDest.setType_list(JsonUtil.toJson(typeList));
			
			algModelDao.addAlgModel(algModelDest);

			modelList = algModelDao.queryAlgModel(modelName);
			
			Map<String,Object> paramMap = new HashMap<>();
			paramMap.put("modelPath", algModelDest.getModel_url());
			paramMap.put("id", retrainTask.getId());
			paramMap.put("task_status", Constants.RETRAINTASK_STATUS_FINISHED);
			retrainTaskDao.updateRetrainTask(paramMap);
			
			//删除所有的模型
			List<File> fileList = FileUtil.getAllFileList(dir);
			for(File tmpFile : fileList) {
				if(tmpFile.getName().endsWith(".pth")) {
					logger.info("delete not need model:" + tmpFile.getAbsolutePath());
					tmpFile.delete();
				}
			}
			
			return modelList.get(0).getId();//返回新的模型ID，用于重新标注
		}
		return -1;
	}

	private String getWorkDir(String algRootPath, String trainScript) {
		String workDir = algRootPath + "work_dirs" +File.separator;
		int index = trainScript.lastIndexOf("/");
		if(index != -1) {
			String confPy = trainScript.substring(index + 1);
			return workDir + confPy.substring(0,confPy.length() - 3);
		}
		return null;
	}

	private String collectPictureToTrainDataSet(RetrainTask retrainTask,String dataSetPath,AlgModel algModel,String algRootPath,boolean isCoco,List<String> typeList) {
		//查询数据库，找到最近一次训练的时间，从这个时间往后，
		//收集最近已经标注好的图片，并进行增量训练。
		//训练完成之后，再对未完成的进行重新标注
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("alg_model_id", algModel.getId());
		paramMap.put("task_status", Constants.RETRAINTASK_STATUS_FINISHED);
		//List<RetrainTask> lastTaskList = retrainTaskDao.queryLastRetrainTask(paramMap);
		//String minTime = retrainTask.getLabel_date();
		//if(minTime == null || minTime.isEmpty()) {
		//	minTime = TimeUtil.getBeforeDayTimeStr(7);
		//}
		//String currentDay = TimeUtil.getCurrentDayStr();
		
		String conf = algRootPath + algModel.getConf_path();
		if(!new File(conf).exists()) {
			logger.info("conf is not exists." + conf);
			return null;
		}
		
		String dataSetValPath = dataSetPath  + "val2017";
		String dataSetTrainPath = dataSetPath  + "train2017";
		if(!isCoco) {
			dataSetValPath = dataSetPath +"JPEGImages";
			dataSetTrainPath = dataSetPath +"JPEGImages";
		}
		if(!new File(dataSetValPath).exists()) {
			new File(dataSetValPath).mkdir();
		}
		if(!new File(dataSetTrainPath).exists()) {
			new File(dataSetTrainPath).mkdir();
		}
		String lastModel = algRootPath + algModel.getModel_url();
		if(RETRAIN.equals(retrainTask.getRetrain_type())){//重训处理
//			RetrainTask lastTask = null;
//			if(lastTaskList != null) {
//				for(RetrainTask task : lastTaskList) {
//					String time = task.getTask_start_time();
//					if(minTime.compareTo(time) < 0 && !time.startsWith(currentDay)) {
//						minTime = time;
//						lastTask = task;
//					}
//				}
//			}

//			if(lastTask != null) {
//				lastModel = algRootPath + lastTask.getModelPath();
//			}
			//重训，类型保持与原来一致。
			typeList.addAll(JsonUtil.getList(algModel.getType_list()));
		}else {
			//初始训练
			if(DETECTION_TYPE_INPUT.equals(retrainTask.getDetection_type())) {
				if(retrainTask.getDetection_type_input() != null && retrainTask.getDetection_type_input().length() > 0) {
					String types[] = retrainTask.getDetection_type_input().split(",");
					for(String type : types) {
						typeList.add(type);
					}
				}
			}
		}
		logger.info(" typeList=" + typeList.toString() + " lastModel=" + lastModel);
		//paramMap = new HashMap<>();
		//paramMap.put("item_add_time", minTime);

		
		
		//要从人工标注中的 id中获取。
		//List<LabelTaskItem> itemList = labelTaskItemDao.queryLabelTaskItemAfterTime(paramMap);
		//拷贝图片
		List<LabelTaskItem> trainList = new ArrayList<>();
		List<LabelTaskItem> valList = new ArrayList<>();
		
		String retrainData = retrainTask.getRetrain_data();
		List<String> labelTaskIdList = JsonUtil.getList(retrainData);
		
		for(String labelTaskId : labelTaskIdList) {
			List<LabelTaskItem> itemList = labelTaskItemDao.queryLabelTaskItemByLabelTaskId(TokenManager.getUserTablePos(retrainTask.getUser_id(), UserConstants.LABEL_TASK_SINGLE_TABLE),labelTaskId);
			int size = itemList.size();
			Map<String,Map<String,String>> replaceTypeMap = new HashMap<>();
			logger.info("item size=" + size);
			for(int i = 0; i < size ;i++) {
				LabelTaskItem item = itemList.get(i);
				List<Map<String,Object>> labelList = JsonUtil.getLabelList(item.getLabel_info());
				if(typeList.contains("car")) {
					if(replaceTypeMap.get(item.getLabel_task_id()) == null) {
						queryRelabelType(item.getLabel_task_id(),replaceTypeMap);
					}
					replaceLabelType(labelList,replaceTypeMap.get(item.getLabel_task_id()));
				}

				dealSupportType(labelList,typeList,retrainTask.getDetection_type());

				item.setLabel_info(JsonUtil.toJson(labelList));

				String relativeUrl = item.getPic_image_field();
				String tmp[] = relativeUrl.split("/");
				int length = tmp.length;
				//logger.info("bucketname=" + tmp[length-2] + " objectname=" + tmp[length-1]);
				String bucketName = tmp[length-2];
				String objectName = tmp[length-1];
				try {
					if(i % 10 == 0) {
						fileService.downLoadFileFromMinio(bucketName, objectName, dataSetValPath);
						valList.add(item);
					}else {			
						fileService.downLoadFileFromMinio(bucketName, objectName, dataSetTrainPath);
						trainList.add(item);
					}
				} catch (LabelSystemException e) {
					e.printStackTrace();
				}
			}
		}
		
		
		logger.info("final  typeList=" + typeList.toString());
		
		int num_class = typeList.size() + 1;
		
		//将不支持的type要追加到voc.py及coco.py，还有class_names.py中。
		DataSetClassReplaceUtil dataSetUtil = new DataSetClassReplaceUtil();
		EvaluationClassReplaceUtil evaluationClassUtil = new EvaluationClassReplaceUtil();
		evaluationClassUtil.replaceClass(typeList, algRootPath + CLASS_NAME_DATASET_CLASS_PATH);
		String newconf = null;
		if(isCoco) {
			dataSetUtil.replaceClass(typeList, algRootPath + COCO_DATASET_CLASS_PATH);
			newconf = makeCocoTrain(dataSetPath, lastModel, num_class, conf, trainList, valList,retrainTask);
		}else {
			dataSetUtil.replaceClass(typeList, algRootPath + VOC_DATASET_CLASS_PATH);
			newconf = makeVocTrain(dataSetPath, lastModel, num_class, conf, trainList, valList,retrainTask) ;
		}
		return newconf;
	}

	private String makeVocTrain(String dataSetPath, String lastModel, int num_class, String conf,
			List<LabelTaskItem> trainList, List<LabelTaskItem> valList,RetrainTask retrainTask) {
		
		String annoFilePath = dataSetPath + "Annotations" +File.separator;
		new File(annoFilePath).mkdir();
		
		for(LabelTaskItem item : trainList) {
			writeXml(item, annoFilePath);
		}
		for(LabelTaskItem item : valList) {
			writeXml(item, annoFilePath);
		}
		CreateTrainVal createTrainVal = new CreateTrainVal();
		//createTrainVal.createTrainVal(dataSetPath, true);
		double ratio = retrainTask.getTestTrainRatio();
		if(ratio < 0.01) {
			ratio = 0.1;
		}
		createTrainVal.createTrainVal(dataSetPath, ratio);
		
		String newConf = makeNewVocConf(dataSetPath, lastModel, num_class, conf, retrainTask);
		
		return newConf;
	}
	
	private void writeXml(LabelTaskItem item,String annoFilePath) {
		Document doc = vocUtil.getXmlDocument(item,null,new HashMap<>());
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
	

	private String makeCocoTrain(String dataSetPath, String lastModel, int num_class, String conf,
			List<LabelTaskItem> trainList, List<LabelTaskItem> valList,RetrainTask retrainTask) {
		Map<String,Object> trainJsonMap = cocoAnnotationsUtil.getTmpCocoJson(trainList);
		Map<String,Object> valJsonMap = cocoAnnotationsUtil.getTmpCocoJson(valList);
		
		
		String trainJsonFilePath = dataSetPath + "annotations" +File.separator + "instances_train2017.json";
		String valJsonFilePath = dataSetPath + "annotations" +File.separator + "instances_val2017.json";
		new File(dataSetPath + "annotations").mkdir();
		File trainJsonFile = new File(trainJsonFilePath);
		File valJsonFile = new File(valJsonFilePath);
		
		try(BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(trainJsonFile),"utf-8"))){
			bufferedWriter.write(JsonUtil.toJson(trainJsonMap));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try(BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(valJsonFile),"utf-8"))){
			bufferedWriter.write(JsonUtil.toJson(valJsonMap));
		} catch (IOException e) {
			e.printStackTrace();
		}

		String newconf = makeNewCocoConf(dataSetPath, lastModel, num_class, conf, retrainTask);
		return newconf;
	}


	private static String makeNewCocoConf(String dataSetPath, String lastModel, int num_class, String conf,RetrainTask retrainTask) {
		//修改配置文件
		List<String> allLine = FileUtil.getAllLineList(conf, "utf-8");
		
		String newconf = conf.substring(0,conf.length() - 3) + "_" +  TimeUtil.getCurrentTimeStrByyyyyMMddHHmmss() + ".py";
		
		String workdir = newconf.substring(newconf.lastIndexOf("/") + 1);
		workdir = workdir.substring(0,workdir.length() - 3);
		
		List<String> newLineList = new ArrayList<>();
		for(int i = 0; i < allLine.size(); i++) {
			String line = allLine.get(i);
			if(RETRAIN.equals(retrainTask.getRetrain_type())) {
				if(line.indexOf("pretrained=") != -1) {
					int tmp = line.indexOf("pretrained=");
					line = line.substring(0,tmp + "pretrained=".length()) + "'" + lastModel + "',";
				}
			}
			if(line.trim().startsWith("dataset_type")) {
				line = "dataset_type = 'CocoDataset'";
			}
			if(line.trim().startsWith("train=dict(")) {
				int j = i + 1;
				for(; j< allLine.size(); j++) {
					if(allLine.get(j).trim().startsWith("pipeline=train_pipeline")) {
						break;
					}
				}
				i = j;
				newLineList.add(line);
				int tmp = line.indexOf("train=dict(");
				String blank = line.substring(0,tmp) + "    ";
				newLineList.add(blank + "type=dataset_type,");
				newLineList.add(blank + "ann_file=data_root + 'annotations/instances_train2017.json',");
				newLineList.add(blank + "img_prefix=data_root + 'train2017/',");
				newLineList.add(blank + "pipeline=train_pipeline),");
				continue;
			}
			if(line.trim().startsWith("val=dict(")) {
				int j = i + 1;
				for(; j< allLine.size(); j++) {
					if(allLine.get(j).trim().startsWith("pipeline=test_pipeline")) {
						break;
					}
				}
				i = j;
				newLineList.add(line);
				int tmp = line.indexOf("val=dict(");
				String blank = line.substring(0,tmp) + "    ";
				newLineList.add(blank + "type=dataset_type,");
				newLineList.add(blank + "ann_file=data_root + 'annotations/instances_val2017.json',");
				newLineList.add(blank + "img_prefix=data_root + 'val2017/',");
				newLineList.add(blank + "pipeline=test_pipeline),");
				continue;

			}
			if(line.trim().startsWith("test=dict(")) {
				int j = i + 1;
				for(; j< allLine.size(); j++) {
					if(allLine.get(j).trim().startsWith("pipeline=test_pipeline")) {
						break;
					}
				}
				i = j;
				newLineList.add(line);
				int tmp = line.indexOf("test=dict(");
				String blank = line.substring(0,tmp) + "    ";
				newLineList.add(blank + "type=dataset_type,");
				newLineList.add(blank + "ann_file=data_root + 'annotations/instances_val2017.json',");
				newLineList.add(blank + "img_prefix=data_root + 'val2017/',");
				newLineList.add(blank + "pipeline=test_pipeline))");
				continue;
			}
			if(line.trim().startsWith("data_root")) {
				line = "data_root = '" + dataSetPath +  "'";
			}
			if(line.trim().startsWith("num_classes=")) {
				int tmp = line.indexOf("num_classes=");
				line = line.substring(0,tmp) + "num_classes=" + num_class + ",";
			}
			if(line.trim().startsWith("work_dir")) {
				line = "work_dir='./work_dirs/" + workdir +  "'";
			}
			newLineList.add(line);
		}
		
		logger.info("newconf=" + newconf);
		try(BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newconf),"utf-8"))){
			for(String line : newLineList) {
				bufferedWriter.write(line);
				bufferedWriter.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return newconf;
	}
	
	private static String makeNewVocConf(String dataSetPath, String lastModel, int num_class, String conf,RetrainTask retrainTask) {
		//修改配置文件
		int index = dataSetPath.indexOf("VOC2007");
		dataSetPath = dataSetPath.substring(0,index);
		String newconf = conf.substring(0,conf.length() - 3) + "_" +  TimeUtil.getCurrentTimeStrByyyyyMMddHHmmss() + ".py";
		
		String workdir = newconf.substring(newconf.lastIndexOf("/") + 1);
		workdir = workdir.substring(0,workdir.length() - 3);
		List<String> allLine = FileUtil.getAllLineList(conf, "utf-8");
		List<String> newLineList = new ArrayList<>();
		for(int i = 0; i < allLine.size(); i++) {
			String line = allLine.get(i);
			if(RETRAIN.equals(retrainTask.getRetrain_type())) {
				if(line.indexOf("pretrained=") != -1) {
					int tmp = line.indexOf("pretrained=");
					line = line.substring(0,tmp + "pretrained=".length()) + "'" + lastModel + "',";
				}
			}
			if(line.trim().startsWith("dataset_type")) {
				line = "dataset_type = 'VOCDataset'";
			}
			if(line.trim().startsWith("train=dict(")) {
				int j = i + 1;
				for(; j< allLine.size(); j++) {
					if(allLine.get(j).trim().startsWith("pipeline=train_pipeline")) {
						break;
					}
				}
				i = j;
				newLineList.add(line);
				int tmp = line.indexOf("train=dict(");
				String blank = line.substring(0,tmp) + "    ";
				newLineList.add(blank + "type=dataset_type,");
				newLineList.add(blank + "ann_file=data_root + 'VOC2007/ImageSets/Main/trainval.txt',");
				newLineList.add(blank + "img_prefix=data_root + 'VOC2007/',");
				newLineList.add(blank + "pipeline=train_pipeline),");
				continue;
			}
			if(line.trim().startsWith("val=dict(")) {
				int j = i + 1;
				for(; j< allLine.size(); j++) {
					if(allLine.get(j).trim().startsWith("pipeline=test_pipeline")) {
						break;
					}
				}
				i = j;
				newLineList.add(line);
				int tmp = line.indexOf("val=dict(");
				String blank = line.substring(0,tmp) + "    ";
				newLineList.add(blank + "type=dataset_type,");
				newLineList.add(blank + "ann_file=data_root + 'VOC2007/ImageSets/Main/test.txt',");
				newLineList.add(blank + "img_prefix=data_root + 'VOC2007/',");
				newLineList.add(blank + "pipeline=test_pipeline),");
				continue;

			}
			if(line.trim().startsWith("test=dict(")) {
				int j = i + 1;
				for(; j< allLine.size(); j++) {
					if(allLine.get(j).trim().startsWith("pipeline=test_pipeline")) {
						break;
					}
				}
				i = j;
				newLineList.add(line);
				int tmp = line.indexOf("test=dict(");
				String blank = line.substring(0,tmp) + "    ";
				newLineList.add(blank + "type=dataset_type,");
				newLineList.add(blank + "ann_file=data_root + 'VOC2007/ImageSets/Main/test.txt',");
				newLineList.add(blank + "img_prefix=data_root + 'VOC2007/',");
				newLineList.add(blank + "pipeline=test_pipeline))");
				continue;
			}
			if(line.trim().startsWith("data_root")) {
				line = "data_root = '" + dataSetPath +  "'";
			}
			if(line.trim().startsWith("num_classes=")) {
				int tmp = line.indexOf("num_classes=");
				line = line.substring(0,tmp) + "num_classes=" + num_class + ",";
			}
			if(line.trim().startsWith("work_dir")) {
				line = "work_dir='./work_dirs/" + workdir +  "'";
			}
			newLineList.add(line);
		}
		logger.info("newvocconf=" + newconf);
		try(BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newconf),"utf-8"))){
			for(String line : newLineList) {
				bufferedWriter.write(line);
				bufferedWriter.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return newconf;
	}


	private void queryRelabelType(String label_task_id, Map<String, Map<String, String>> replaceTypeMap) {
		LabelTask labelTask = labelTaskDao.queryLabelTaskById(label_task_id);
		Map<String,String> replace = new HashMap<>();
		replaceTypeMap.put(label_task_id, replace);
		if(labelTask != null) {
			String taskLabelInfo = labelTask.getTask_label_type_info();
			if(taskLabelInfo != null && taskLabelInfo.indexOf("轿车") != -1) {//暂且写死
				for(int i = 1; i<= 20; i++) {
					replace.put("" + i, "car");
				}
			}
		}
	}


	private void replaceLabelType(List<Map<String, Object>> labelList, Map<String, String> replaceTypeMap) {
		Iterator<Map<String, Object>> it = labelList.iterator();
		while(it.hasNext()){
			Map<String, Object> label = it.next();
		    Object type = label.get("class_name");
		    if(type != null) {
		    	String newType = replaceTypeMap.get(type.toString());
		    	if(newType != null) {
		    		label.put("class_name", newType);
		    	}
		    }
		}
	}

	private void dealSupportType(List<Map<String, Object>> labelList, List<String> typeList,String detection_type) {
		Iterator<Map<String, Object>> it = labelList.iterator();
		while(it.hasNext()){
			Map<String, Object> label = it.next();
			if(DETECTION_TYPE_LABEL.equals(detection_type)) {
				String type = (String)label.get("class_name");
				if(type != null) {
					if(!typeList.contains(type)) {
						typeList.add(type);
					}
				}
			}else {
				if(!isContain(label,typeList)){
					it.remove();
				}
			}
		}
	}

	
	private boolean isContain(Map<String, Object> label, List<String> typeList) {
		String type = (String)label.get("class_name");
		if(type != null) {
			if(typeList.contains(type)) {
				return true;
			}
		}
		return false;
	}

	
	public static void main(String[] args) {
		
		makeNewCocoConf("D:\\2019文档\\问题定位\\0615", "aa.pth", 2, "D:\\2019文档\\问题定位\\0615\\retinanet_free_anchor_x101-32x4d_fpn_1x_voc_new.py",null);
		
	}

}
