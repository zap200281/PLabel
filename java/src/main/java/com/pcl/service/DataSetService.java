package com.pcl.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.logging.log4j.util.Strings;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.pcl.constant.Constants;
import com.pcl.constant.UserConstants;
import com.pcl.dao.AlgModelDao;
import com.pcl.dao.DataSetDao;
import com.pcl.dao.DataSetVideoInfoDao;
import com.pcl.dao.LabelDcmTaskItemDao;
import com.pcl.dao.LabelTaskItemDao;
import com.pcl.dao.ProgressDao;
import com.pcl.dao.UserDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.FileResult;
import com.pcl.pojo.PageResult;
import com.pcl.pojo.Progress;
import com.pcl.pojo.body.DramFrameBody;
import com.pcl.pojo.body.PrePredictTaskBody;
import com.pcl.pojo.display.DisplayDataSet;
import com.pcl.pojo.mybatis.AlgModel;
import com.pcl.pojo.mybatis.DataSet;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.pojo.mybatis.LargePictureTask;
import com.pcl.pojo.mybatis.User;
import com.pcl.pojo.mybatis.VideoInfo;
import com.pcl.service.schedule.LargeImgToDziSchedule;
import com.pcl.service.schedule.ThreadSchedule;
import com.pcl.util.FileUtil;
import com.pcl.util.JsonUtil;
import com.pcl.util.PclJsonAnnotationsUtil;
import com.pcl.util.TimeUtil;
import com.pcl.util.VideoUtil;
import com.pcl.util.VocAnnotationsUtil;

import ij.plugin.DICOM;

@Service
public class DataSetService {

	private static Logger logger = LoggerFactory.getLogger(DataSetService.class);

	@Autowired
	private DataSetDao dataSetDao;

	@Autowired
	private UserDao userDao;

	@Autowired
	private ObjectFileService fileService;

	@Autowired
	private LabelTaskItemDao labelTaskItemDao;

	@Autowired
	private LabelDcmTaskItemDao labelDcmTaskItemDao;

	@Autowired
	private DataSetVideoInfoDao dataSetVideoInfoDao;
	
	@Autowired
	private VocAnnotationsUtil vocUtil;
	
	@Autowired
	private PclJsonAnnotationsUtil pclJsonUtil;
	
	@Value("${server.port}")
    private String port;
	
	@Value("${server.saveTmpVideo:1}")
    private int saveTmpVideo;//2表示存储临时文件，1表示不存储。
	
	@Autowired
	private ProgressDao progressDao;
	
	@Autowired
	private LargeImgToDziSchedule largeImgToDziSchedule;
	
	@Autowired
	private LargePictureService largePictureSevice;
	
	@Autowired
	private PrePredictTaskService prePredictTaskService;
	
	@Autowired
	private AlgModelDao algModelDao;

	private Gson gson = new Gson();
	
	//private Map<String,Progress> progressMap = new HashMap<>();

	public DisplayDataSet queryDataSetById(String token,String dataSetId) {
		DataSet dataSet = dataSetDao.queryDataSetById(dataSetId);
		if(dataSet != null) {
			DisplayDataSet displayDataSet = new DisplayDataSet();
			displayDataSet.setId(dataSet.getId());
			displayDataSet.setTask_name(dataSet.getTask_name());
			displayDataSet.setTask_add_time(dataSet.getTask_add_time());
			displayDataSet.setDatasetType(dataSet.getDataset_type());
			displayDataSet.setTotal(dataSet.getTotal());
			displayDataSet.setCamera_number(getDisplayValue(dataSet.getCamera_number()));
			displayDataSet.setCamera_gps(getDisplayValue(dataSet.getCamera_gps()));
			displayDataSet.setCamera_date(getDisplayValue(dataSet.getCamera_date()));
			
			if(dataSet.getVideoSet() != null && dataSet.getVideoSet().length() > 0) {
				displayDataSet.setVideoSet(JsonUtil.getList(dataSet.getVideoSet()));
			}
			displayDataSet.setTask_desc(getDisplayValue(dataSet.getTask_desc()));
			displayDataSet.setTask_status(dataSet.getTask_status());
			displayDataSet.setZip_object_name("/minio/" + dataSet.getZip_bucket_name() + "/" + dataSet.getZip_object_name());
			return displayDataSet;
		}
		return null;
	}
	
	private Progress getProgress(String id) {
		
		return progressDao.queryProgressById(id);
		
	}

	public PageResult queryDataSet(String token,int currPage, int pageSize){

		PageResult pageResult = new PageResult();
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		List<DisplayDataSet> result = new ArrayList<>();

		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		paramMap.put("user_id", userId);
		List<DataSet> dataSetList = dataSetDao.queryDataSet(paramMap);

		int totalCount = dataSetDao.queryDataSetCount(paramMap);

		pageResult.setTotal(totalCount);
		pageResult.setData(result);
		pageResult.setCurrent(currPage);

		if(totalCount > 0) {
			List<User> userList = userDao.queryAll();
			HashMap<Integer,String> userIdForName = new HashMap<>();
			for(User user : userList) {
				userIdForName.put(user.getId(), user.getUsername());
			}

			for(DataSet dataSet : dataSetList) {
				DisplayDataSet displayDataSet = new DisplayDataSet();
				displayDataSet.setId(dataSet.getId());
				displayDataSet.setTask_name(dataSet.getTask_name());
				displayDataSet.setTask_add_time(dataSet.getTask_add_time());
				displayDataSet.setDatasetType(dataSet.getDataset_type());
				displayDataSet.setTotal(dataSet.getTotal());
				displayDataSet.setCamera_number(getDisplayValue(dataSet.getCamera_number()));
				displayDataSet.setCamera_gps(getDisplayValue(dataSet.getCamera_gps()));
				displayDataSet.setCamera_date(getDisplayValue(dataSet.getCamera_date()));
				
				if(dataSet.getVideoSet() != null && dataSet.getVideoSet().length() > 0) {
					displayDataSet.setVideoSet(JsonUtil.getList(dataSet.getVideoSet()));
				}
				Progress pro = getProgress(dataSet.getId());
				if(pro != null) {
					//显示进度
					double progress = (System.currentTimeMillis()/1000 - pro.getStartTime())*1.0d/pro.getExceedTime();
					int progressInt = (int)(progress * 100);
					if(progressInt > 100) {
						progressInt = 100;
					}
					displayDataSet.setTask_status_desc(progressInt + "%");
				}
				displayDataSet.setTask_desc(getDisplayValue(dataSet.getTask_desc()));
				
				displayDataSet.setTask_status(dataSet.getTask_status());

				if(userIdForName.containsKey(dataSet.getUser_id())) {
					displayDataSet.setUser(userIdForName.get(dataSet.getUser_id()));
				}
				if(userIdForName.containsKey(dataSet.getAssign_user_id())) {
					displayDataSet.setAssign_user(userIdForName.get(dataSet.getAssign_user_id()));
				}else {
					displayDataSet.setAssign_user("");
				}
				result.add(displayDataSet);
			}
		}

		return pageResult;
	}

	private String getDisplayValue(String str) {
		if(str == null) {
			return "";
		}
		return str;
	}

	public List<DisplayDataSet> queryAllDataSet(String token,List<String> dataSetType){

		List<DisplayDataSet> result = new ArrayList<>();
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		Map<String,Object> paramMap = new HashMap<>();

		paramMap.put("user_id", userId);
		paramMap.put("datasettypeList", dataSetType);

		List<DataSet> dataSetList = dataSetDao.queryDataSetByType(paramMap);

		if(dataSetList.size() > 0) {
			for(DataSet dataSet : dataSetList) {
				DisplayDataSet displayDataSet = new DisplayDataSet();
				displayDataSet.setId(dataSet.getId());
				displayDataSet.setTask_name(dataSet.getTask_name());
				displayDataSet.setTask_add_time(dataSet.getTask_add_time());
				displayDataSet.setDatasetType(dataSet.getDataset_type());
				displayDataSet.setTotal(dataSet.getTotal());
				displayDataSet.setTask_desc(dataSet.getTask_desc());
				result.add(displayDataSet);
			}
		}
		return result;
	}

	public int addDataSet(String token, DataSet dataSet) throws LabelSystemException {

		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));

		dataSet.setId(UUID.randomUUID().toString().replaceAll("-",""));
		dataSet.setUser_id(userId);
		dataSet.setAssign_user_id(dataSet.getAssign_user_id());
		dataSet.setTask_add_time(TimeUtil.getCurrentTimeStr());
		dataSet.setTask_status(Constants.DATASET_PROCESS_NOT_START);
		
		dataSetDao.addDataSet(dataSet);

		if(dataSet.getDataset_type() == Constants.DATASET_TYPE_DCM) {
			dealDcmDataSet(dataSet);
		}else if(dataSet.getDataset_type() == Constants.DATASET_TYPE_PICTURE) {
			dealPictureDataSet(dataSet);
		}else if(dataSet.getDataset_type() == Constants.DATASET_TYPE_VIDEO) {
			ThreadSchedule.execThread(()->saveVideoInfoToDb(dataSet));
		}else if(dataSet.getDataset_type() == Constants.DATASET_TYPE_SVS) {
			
			if(dataSet.getVideoSet() != null && "autoAddLabelTask".equals(dataSet.getVideoSet())) {
				//自动增加标注任务
				LargePictureTask task = new LargePictureTask();
				task.setTask_name(dataSet.getTask_name());
				task.setAppid(dataSet.getAppid());
				task.setDataset_id(dataSet.getId());
				task.setTask_status(Constants.LARGE_TASK_STATUS_NOT_START);
				largePictureSevice.addLargePictureTask(token, task);
			}
			
			largeImgToDziSchedule.addTask(dataSet);
		}
		return 1;
	}

	
	public Map<String,Object> getDziInfo(String dataSetId) {
		DataSet dataSet = dataSetDao.queryDataSetById(dataSetId);
		
		String json = dataSet.getMainVideoInfo();
		
		return JsonUtil.getMap(json);
		
	}

	public void saveVideoInfoToDb(DataSet dataSet) {

		String tmpPath = LabelDataSetMerge.getUserDataSetPath() + File.separator + "video_" +  System.nanoTime();
		new File(tmpPath).mkdirs();
		if(dataSet.getVideoSet() != null) {
			logger.info("getVideoSet=" + dataSet.getVideoSet());
			List<String> videoList = JsonUtil.getList(dataSet.getVideoSet());
			if(videoList.size() > 0) {
				dealMultiVideo(dataSet, tmpPath, videoList);
				logger.info("success update videoSet, dataSet=" + dataSet.getId());
			}
		}
		saveMainVideoInfo(dataSet, tmpPath);
		if(saveTmpVideo == 1) {
			FileUtil.delDir(tmpPath);
		}
	}

	private void dealMultiVideo(DataSet dataSet, String tmpPath, List<String> videoList) {
		List<VideoInfo> batch = new ArrayList<>();
		for(String videoMinioUrl : videoList) {
			logger.info("videoMinioUrl=" + videoMinioUrl);
			File downloadFile = null;
			try {
				String tmp[] = videoMinioUrl.split("/");
				int length = tmp.length;
				String bucketName = tmp[length - 2];
				String objectName = tmp[length - 1];
				String downloadFilePath = fileService.downLoadFileFromMinio(bucketName, objectName, tmpPath);
				downloadFile = new File(downloadFilePath);

				VideoInfo videoInfo = VideoUtil.getVideoObj(downloadFile);

				if(videoInfo == null) {
					logger.info("the video is not support.");
					videoInfo = new VideoInfo();
					videoInfo.setVideoCode("Not support.");
				}
				videoInfo.setDataset_id(dataSet.getId());
				videoInfo.setId(UUID.randomUUID().toString().replaceAll("-",""));
				videoInfo.setMinio_url(videoMinioUrl);
				videoInfo.setCamera_gps(dataSet.getCamera_gps());
				videoInfo.setCamera_number(dataSet.getCamera_number());
				batch.add(videoInfo);
			}catch (Exception e) {
				logger.info(e.getMessage());
			}finally {
				if(downloadFile != null) {
					downloadFile.delete();
				}
			}
		}
		logger.info("add video to video info table.");
		dataSetVideoInfoDao.addBatchVideoInfo(batch);
		List<String> idList = new ArrayList<>();
		batch.forEach((e)->idList.add(e.getId()));
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", dataSet.getId());
		paramMap.put("videoSet", gson.toJson(idList));
		paramMap.put("task_status", Constants.DATASET_PROCESS_NOT_START);
		dataSetDao.updateDataSet(paramMap);
	}

	private void saveMainVideoInfo(DataSet dataSet, String tmpPath) {
		try {
			String bucketName =dataSet.getZip_bucket_name();
			String objectName = dataSet.getZip_object_name();
			String downloadFilePath = fileService.downLoadFileFromMinio(bucketName, objectName, tmpPath);
			VideoInfo mainVideoInfo = VideoUtil.getVideoObj(new File(downloadFilePath));
			if(mainVideoInfo == null) {
				logger.info("the video is not support.");
				mainVideoInfo = new VideoInfo();
				mainVideoInfo.setVideoCode("Not support.");
			}else {
				mainVideoInfo.setTmpVideoPath(downloadFilePath);
			}
			
			String mainVideoStr = gson.toJson(mainVideoInfo);
			Map<String,Object> paramMap = new HashMap<>();
			paramMap.put("id", dataSet.getId());
			paramMap.put("mainVideoInfo", mainVideoStr);
			paramMap.put("task_status", Constants.DATASET_PROCESS_PREPARE);
			dataSetDao.updateDataSet(paramMap);
		}catch(Exception e) {
			logger.info(e.getMessage(),e);
		}
	}


	public void chouZhen(String dataSetId,Map<String,String> para) {
		DataSet dataSet = dataSetDao.queryDataSetById(dataSetId);
		if(dataSet == null) {
			return;
		}
		if(dataSet.getDataset_type() != Constants.DATASET_TYPE_VIDEO) {
			return;
		}
		if(dataSet.getTask_status() == Constants.DATASET_PROCESS_CHOUZHEN) {//如果该任务已经处于抽帧状态，则不再处理。
			return;
		}
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", dataSet.getId());
		paramMap.put("task_status", Constants.DATASET_PROCESS_CHOUZHEN);
		dataSetDao.updateDataSet(paramMap);

		ThreadSchedule.execThread(()->runChouZhen(dataSet,para));
	}

	private void runChouZhen(DataSet dataSet,Map<String,String> para) {
		long start = System.currentTimeMillis();
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", dataSet.getId());
		String bucketName = UUID.randomUUID().toString().replaceAll("-","");
		
		String json = dataSet.getMainVideoInfo();
		Map<String,Object> mainVideoMap = JsonUtil.getMap(json);
		
		String tmpPath = LabelDataSetMerge.getUserDataSetPath() + File.separator + "video_" +  System.nanoTime();
		String existVideoPath = null;
		if(mainVideoMap.get("tmpVideoPath") != null) {
			existVideoPath = mainVideoMap.get("tmpVideoPath").toString();
			if(new File(existVideoPath).exists()) {
				tmpPath = new File(existVideoPath).getParentFile().getAbsolutePath();
			}else {
				existVideoPath = null;
			}
		}
		
		//获取图片，解压
		try {
			if(existVideoPath == null) {
				new File(tmpPath).mkdirs();
				// -s 1024x768
				existVideoPath = fileService.downLoadFileFromMinio(dataSet.getZip_bucket_name(), dataSet.getZip_object_name(), tmpPath);
			}else {
				logger.info("the video has existed in disk.");
			}
			File downloadFile = new File(existVideoPath);
			Map<String,String> param = new HashMap<>();
			String cameraNumber = dataSet.getCamera_number();
			if(cameraNumber != null) {
				param.put("cameraNumber", cameraNumber);
			}else {
				param.put("cameraNumber", "1");
			}
			String cameraDate = dataSet.getCamera_date();
			if(cameraDate != null) {
				param.put("cameraDate", cameraDate);
			}
			param.putAll(para);
			
			String fps = param.get("fps");
			if(fps == null) {
				fps = "0.5";//默认每2秒抽一帧
			}
			double fpsDouble = Double.parseDouble(fps);
			
			VideoInfo videoObj = VideoUtil.getVideoObj(downloadFile);
			
			int time = VideoUtil.getExceetTime(videoObj, fpsDouble);
			
			setProgress(dataSet.getId(),time);
			
			String chouzhengTmpPath = VideoUtil.chouZhen(videoObj,downloadFile, param);
	
			//进行抽关键帧
			List<File> fileList = FileUtil.getAllFileList(chouzhengTmpPath);
			logger.info("The picture numer of chouzhen is:" + fileList.size());
			fileService.uploadPictureFile(fileList, bucketName);
			//删除下载的文件
			downloadFile.delete();
			//临时文件先不删除，
	
			mainVideoMap.put("tmpFramePath", chouzhengTmpPath);
			String mainVideoStr = gson.toJson(mainVideoMap);
			paramMap.put("mainVideoInfo", mainVideoStr);
			paramMap.put("total", fileList.size());
			paramMap.put("task_status", Constants.DATASET_PROCESS_PREPARE);
			paramMap.put("file_bucket_name", bucketName);
			
			
			dataSetDao.updateDataSet(paramMap);

			//删除已经存在的抽帧图片
			labelTaskItemDao.deleteLabelTaskById(TokenManager.getUserTablePos(dataSet.getUser_id(), UserConstants.LABEL_TASK_SINGLE_TABLE),dataSet.getId());

			//删除minio中的图片
			if(dataSet.getFile_bucket_name() != null) {
				logger.info("delete minio exist picture.");
				fileService.removeBucketName(dataSet.getFile_bucket_name());
			}

			List<LabelTaskItem> batchList = new ArrayList<>();
			for(File file : fileList) {
				LabelTaskItem taskItem = new LabelTaskItem();
				taskItem.setId(UUID.randomUUID().toString().replaceAll("-",""));
				taskItem.setItem_add_time(TimeUtil.getCurrentTimeStr());
				taskItem.setLabel_task_id(dataSet.getId());
				String relativePath = "/minio/" + bucketName +  "/" + file.getName();
				taskItem.setPic_image_field(relativePath);
				taskItem.setLabel_status(Constants.LABEL_TASK_STATUS_NOT_FINISHED);
				batchList.add(taskItem);
				if(batchList.size() == 2000) {
					addBatchItemToDb(dataSet.getUser_id(), batchList);
					batchList.clear();
				}
			}
			if(!batchList.isEmpty()) {
				addBatchItemToDb(dataSet.getUser_id(), batchList);
				//labelTaskItemDao.addBatchLabelTaskItem(batchList);
			}
			
			if(para.containsKey("isDeleteVideo")) {
				String value = para.get("isDeleteVideo");
				if(value != null && value.equals("0")) {
					//抽帧完成后，删除原始视频，节省空间
					fileService.deleteFileFromMinio(dataSet.getZip_bucket_name(), dataSet.getZip_object_name());
					//修改数据集类型为图片
					Map<String,Object> tmpParamMap = new HashMap<>();
					tmpParamMap.put("id", dataSet.getId());
					tmpParamMap.put("dataset_type", Constants.DATASET_TYPE_PICTURE);
					dataSetDao.updateDataSet(tmpParamMap);
				}
			}
			if(saveTmpVideo == 1) {
				FileUtil.delDir(chouzhengTmpPath);
			}
			
			if(para.get("createAutoLabelTask") != null) {
				//开始创建自动标注任务
				logger.info("start create auto label task.");
				String createAutoLabelTask = para.get("createAutoLabelTask");
				if("1".equals(createAutoLabelTask)) {
					int modelId = getYolov3();
					if(modelId != -1) {
						logger.info("use yolov3 to create auto label.");
						PrePredictTaskBody prebody = new PrePredictTaskBody();
						prebody.setTaskName(dataSet.getTask_name());
						prebody.setDataSetId(dataSet.getId());
						prebody.setAlgModel(getYolov3());
						//prebody.setScore_threshhold(0);
						prebody.setDeleteNoLabelPicture(Constants.AUTO_DELETE_NO_LABEL_PICTURE_PRI);
						
						prePredictTaskService.addPrePredictTask(dataSet.getUser_id(), prebody);
						
					}else {
						logger.info("Not found yolov3 model. return.");
					}
				}
			}
		} catch (Exception e) {
			logger.info("video deal error.",e);
			paramMap.put("task_status", Constants.DATASET_STATUS_ERROR);
			paramMap.put("task_desc","视频处理错误。请上传支持的视频格式，如mp4格式，不要压缩。");
			dataSetDao.updateDataSet(paramMap);
		}finally {
			progressDao.deleteProgress(dataSet.getId());
			if(saveTmpVideo == 1) {
				FileUtil.delDir(tmpPath);
			}
		}
		logger.info("Draw frame cost:" + (System.currentTimeMillis() - start)/1000 + "s");
	}

	private int getYolov3() {
		String modelName = "YOLOV3";
		List<AlgModel> re = algModelDao.queryAlgModel(modelName);
		if(re != null && re.size() > 0) {
			return re.get(0).getId();
		}
		return -1;
	}

	private void setProgress(String id, int time) {
		Progress pro = new Progress();
		pro.setId(id);
		pro.setStartTime(System.currentTimeMillis()/1000);
		pro.setExceedTime(time);
		
		Progress tmp = progressDao.queryProgressById(id);
		if(tmp != null) {
			progressDao.deleteProgress(id);
		}
		
		progressDao.addProgress(pro);
	}

	private void dealDcmDataSet(DataSet dataSet) {
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", dataSet.getId());
		ThreadSchedule.execThread(new Runnable() {
			@Override
			public void run() {
				String tmpPath = LabelDataSetMerge.getUserDataSetPath() + File.separator + "dcm" + File.separator + System.nanoTime();
				//获取图片，解压
				try {
					new File(tmpPath).mkdirs();
					String downloadFilePath = fileService.downLoadFileFromMinio(dataSet.getZip_bucket_name(), dataSet.getZip_object_name(), tmpPath);
					File downloadFile = new File(downloadFilePath);

					String bucketName = UUID.randomUUID().toString().replaceAll("-","");
					
					List<String> fileList  = fileService.unZipFileToMinio(downloadFile,bucketName);
					//删除下载的文件
					//downloadFile.delete();
					Map<String,Integer> groupNumberMap = new HashMap<>();
					Map<String,String> groupKeyMap = new HashMap<>();
					Map<String,String> widthHeightMap = new HashMap<>();
					for(String fileName : fileList) {
						String relativePath = "/dcm/" + bucketName +  "/" + fileName;
						Map<String,String> info = getDcmInfo(relativePath);
						if(info.isEmpty()) {
							continue;
						}
						String groupKey = info.get("groupKey");
						if(groupNumberMap.containsKey(groupKey)) {
							groupNumberMap.put(groupKey, groupNumberMap.get(groupKey) +1);
						}else {
							groupNumberMap.put(groupKey, 1);
						}
						widthHeightMap.put(relativePath, info.get("widthHeight"));
						groupKeyMap.put(relativePath, groupKey);
					}
					
					List<LabelTaskItem> batchList = new ArrayList<>();
					for(String fileName : fileList) {
						LabelTaskItem taskItem = new LabelTaskItem();
						taskItem.setId(UUID.randomUUID().toString().replaceAll("-",""));
						taskItem.setItem_add_time(TimeUtil.getCurrentTimeStr());
						taskItem.setLabel_task_id(dataSet.getId());
						String relativePath = "/dcm/" + bucketName +  "/" + fileName;
						taskItem.setPic_image_field(relativePath);
						String pic_object_name = widthHeightMap.get(relativePath);
						if(pic_object_name == null) {
							logger.info("cannot recoginize dcm file. the file is:" + relativePath);
							continue;
						}
						taskItem.setPic_object_name(pic_object_name);
						taskItem.setPic_url(groupKeyMap.get(relativePath));
						taskItem.setLabel_status(Constants.LABEL_TASK_STATUS_NOT_FINISHED);
						taskItem.setDisplay_order1(groupNumberMap.get(groupKeyMap.get(relativePath)));
						taskItem.setVerify_status(0);
						batchList.add(taskItem);
					}
					labelDcmTaskItemDao.addBatchLabelTaskItem(batchList);
					
					//删除下载的文件
					FileUtil.delDir(tmpPath);
					
					paramMap.put("total", fileList.size());
					paramMap.put("task_status", Constants.DATASET_PROCESS_PREPARE);//数据集准备好了。
					paramMap.put("file_bucket_name", bucketName);
					dataSetDao.updateDataSet(paramMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		});
	}

	private Map<String,String> getDcmInfo(String filePath) {
		Map<String,String> re = new HashMap<>();
		DICOM dicom = new DICOM(fileService.getImageInputStream(filePath));
		dicom.run("Name");
		if(dicom.getStringProperty("0010,0010  Patient's Name") == null) {
			return re;
		}
		String groupKey= dicom.getStringProperty("0010,0010  Patient's Name") + " " + dicom.getStringProperty("0008,0021  Series Date") + " " +  dicom.getStringProperty("0008,103E  Series Description");
		String widthHeight = dicom.getStringProperty("0028,0010  Rows") + "," + dicom.getStringProperty("0028,0011  Columns");
		
		re.put("groupKey", groupKey);
		re.put("widthHeight", widthHeight);
		
		return re;
	}

	private void dealPictureDataSet(DataSet dataSet) {
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", dataSet.getId());
		ThreadSchedule.execThread(new Runnable() {
			@Override
			public void run() {
				String time = String.valueOf(System.nanoTime());
				String tmpPath = LabelDataSetMerge.getUserDataSetPath() + File.separator + time;
				//获取图片，解压
				try {
					new File(tmpPath).mkdirs();
					String downloadFilePath = fileService.downLoadFileFromMinio(dataSet.getZip_bucket_name(), dataSet.getZip_object_name(), tmpPath);
					File downloadFile = new File(downloadFilePath);
					String bucketName = UUID.randomUUID().toString().replaceAll("-","");
					
					Map<String,LabelTaskItem> annation = getAnnationMap(downloadFile);
					
					//解压到minio中
					List<String> fileList  = fileService.unZipFileToMinio(downloadFile,bucketName);
					//删除下载的文件
					//downloadFile.delete();
					
					List<LabelTaskItem> batchList = new ArrayList<>();
					for(String fileName : fileList) {
						LabelTaskItem taskItem = new LabelTaskItem();
						taskItem.setId(UUID.randomUUID().toString().replaceAll("-",""));
						taskItem.setItem_add_time(TimeUtil.getCurrentTimeStr());
						taskItem.setLabel_task_id(dataSet.getId());
						String relativePath = "/minio/" + bucketName +  "/" + fileName;
						String annaKey = fileName.substring(0,fileName.lastIndexOf("."));
						if(annation.get(annaKey) != null) {
							LabelTaskItem tmp = annation.get(annaKey);
							taskItem.setLabel_info(tmp.getLabel_info());
							taskItem.setPic_object_name(tmp.getPic_object_name());
						}
						taskItem.setPic_image_field(relativePath);
						taskItem.setLabel_status(Constants.LABEL_TASK_STATUS_NOT_FINISHED);
						batchList.add(taskItem);
					}
					FileUtil.delDir(tmpPath);

					addBatchItemToDb(dataSet.getUser_id(), batchList);
					
					paramMap.put("total", fileList.size());
					paramMap.put("task_status", Constants.DATASET_PROCESS_PREPARE);
					paramMap.put("file_bucket_name", bucketName);
					dataSetDao.updateDataSet(paramMap);
					logger.info("upload dataset picture status finised.");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			private Map<String, LabelTaskItem> getAnnationMap(File zipFile) throws IOException {
				logger.info("start deal annatotion....");
				Map<String, LabelTaskItem> result = new HashMap<>();
				try(ZipFile zip = new ZipFile(zipFile,Charset.forName("GBK"))){//解决中文文件夹乱码
					for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements();) {  
						ZipEntry entry = (ZipEntry) entries.nextElement();  
						String entryName = entry.getName();
						
						if(!(entryName.endsWith(".xml") || entryName.endsWith(".json"))) {
							continue;
						}
						int index = entryName.lastIndexOf("/");
						String keyName = entry.getName();
						if(index != -1) {
							keyName = keyName.substring(index + 1);
						}
						keyName =  keyName.substring(0, keyName.lastIndexOf("."));
						try(InputStream in = zip.getInputStream(entry)){
							if(entryName.endsWith(".xml")) {
								result.put(keyName, vocUtil.readLabelInfoFromXmlDocument(in));
							}else if(entryName.endsWith(".json")) {
								if(!result.containsKey(keyName)) {
									result.put(keyName, pclJsonUtil.readLabelInfoFromPclJson(in));
								}
							}
						}
					}  
				}
				logger.info("end deal annatotion....size=" + result.size());
				return result;
			}
		});
	}

	
	private void addBatchItemToDb(int user_id,List<LabelTaskItem> batchList) {
		Map<String,Object> paramMap = new HashMap<>();
		
		paramMap.put("user_id", TokenManager.getUserTablePos(user_id, UserConstants.LABEL_TASK_SINGLE_TABLE));
		
		paramMap.put("list", batchList);
		labelTaskItemDao.addBatchLabelTaskItemMap(paramMap);
	}
	
	
	

	public void deleteDataSetById(String token, String dataSetId) {
		DataSet dataSet = dataSetDao.queryDataSetById(dataSetId);
		dataSetDao.deleteDataSetById(dataSetId);

		new Thread(new Runnable() {
			@Override
			public void run() {
				asyncDelete(dataSetId, dataSet);
			}}).start();
	}

	private void asyncDelete(String dataSetId, DataSet dataSet) {
		logger.info("async delete start.");
		long start = System.currentTimeMillis();
		if(dataSet != null) {
			String rootPath = LabelDataSetMerge.getUserDataSetPath();
			if(dataSet.getDataset_type() == Constants.DATASET_TYPE_DCM) {
				List<LabelTaskItem> list = labelDcmTaskItemDao.queryLabelTaskItemByLabelTaskId(dataSetId);
				labelDcmTaskItemDao.deleteLabelTaskById(dataSetId);
				for(LabelTaskItem item : list) {
					//删除文件
					new File(rootPath + item.getPic_image_field()).delete();
				}				
			}else {
				List<LabelTaskItem> list = labelTaskItemDao.queryLabelTaskItemByLabelTaskId(TokenManager.getUserTablePos(dataSet.getUser_id(), UserConstants.LABEL_TASK_SINGLE_TABLE),dataSetId);
				labelTaskItemDao.deleteLabelTaskById(TokenManager.getUserTablePos(dataSet.getUser_id(), UserConstants.LABEL_TASK_SINGLE_TABLE),dataSetId);
				for(LabelTaskItem item : list) {
					//删除磁盘上的文件
					new File(rootPath + item.getPic_image_field()).delete();
				}	
			}
		}
		//删除minio中的文件
		if(dataSet != null) {
			fileService.deleteFileFromMinio(dataSet.getZip_bucket_name(), dataSet.getZip_object_name(),dataSet.getFile_bucket_name());
		}

		List<VideoInfo> videoInfoList = dataSetVideoInfoDao.queryVideoInfoByDataSetId(dataSet.getId());
		for(VideoInfo video : videoInfoList) {
			fileService.deleteFileFromMinio(video.getMinio_url());
		}

		dataSetVideoInfoDao.deleteVideoInfoByDataSetId(dataSetId);


		logger.info("async delete end,cost=" +(System.currentTimeMillis() - start) + " ms.");
	}


	public void updateDataSet(String token, DataSet body) {

		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", body.getId());
		if(Strings.isNotEmpty(body.getTask_name())) {
			paramMap.put("task_name",body.getTask_name());
		}
		//if(Strings.isNotEmpty(body.getTask_desc())) {
		paramMap.put("task_desc",body.getTask_desc());
		//}

		int task_status = body.getTask_status();
		if(task_status <= 0) {
			task_status = 0;
		}

		paramMap.put("task_status",task_status);

		if(body.getAssign_user_id() > 0) {
			paramMap.put("assign_user_id",body.getAssign_user_id());
		}

		dataSetDao.updateDataSet(paramMap);
	}

	
	
	public PageResult queryDataSetPictureItemPage(String datasetId,int currPage,int pageSize){

		PageResult pageResult = new PageResult();

		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		paramMap.put("label_task_id", datasetId);
		

		DataSet dataSet = dataSetDao.queryDataSetById(datasetId);
		
		paramMap.put("user_id", TokenManager.getUserTablePos(dataSet.getUser_id(), UserConstants.LABEL_TASK_SINGLE_TABLE));

		if(dataSet.getDataset_type() == Constants.DATASET_TYPE_DCM) {
			List<LabelTaskItem> result = labelDcmTaskItemDao.queryLabelTaskItemPageByLabelTaskId(paramMap);
			int totalCount = labelDcmTaskItemDao.queryLabelTaskItemPageCountByLabelTaskId(paramMap);
			pageResult.setTotal(totalCount);
			pageResult.setData(result);

		}else {
			List<LabelTaskItem> result = labelTaskItemDao.queryLabelTaskItemPageByLabelTaskId(paramMap);
			int totalCount = labelTaskItemDao.queryLabelTaskItemPageCountByLabelTaskId(paramMap);
			pageResult.setTotal(totalCount);
			pageResult.setData(result);
		}

		pageResult.setCurrent(currPage);
		return pageResult;
	}

	public String chouZhen(String token, DramFrameBody body) {
		String dataSetId = body.getDateset_id();
		Map<String,String> para = new HashMap<>();
		if(body.getBaseDate() != null && body.getBaseDate().length() > 0) {
			para.put("cameraDate", body.getBaseDate());
		}
		para.put("fps", body.getFps());
		para.put("fileNameFormate", body.getFileNameFormate());
		para.put("drawFrameType", String.valueOf(body.getDrawFrameType()));
		para.put("-s", body.getWidthHeight());
		para.put("isDeleteVideo", String.valueOf(body.getIsDeleteVideo()));
		para.put("createAutoLabelTask", String.valueOf(body.getCreateAutoLabelTask()));
		
		chouZhen(dataSetId, para);

		return "";
	}

	public List<VideoInfo> queryDataSetVideoList(String token, String dataset_id) {

		return dataSetVideoInfoDao.queryVideoInfoByDataSetId(dataset_id);
	}

	public String videoConcat(String token, String datasetId, String videoSetIdListStr,String destFileName) throws LabelSystemException {

		List<String> videoSetIdList = JsonUtil.getList(videoSetIdListStr);

		if(videoSetIdList.size() == 0) {
			throw new LabelSystemException("合并的视频信息为空。datasetId=" + datasetId);
		}

		List<VideoInfo> videoList = new ArrayList<>();
		for(String id : videoSetIdList) {
			VideoInfo videoInfo = dataSetVideoInfoDao.queryVideoInfoById(id);
			if(videoInfo == null) {
				throw new LabelSystemException("视频不存在了。id=" + id);
			}
			videoList.add(videoInfo);
		}
		
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", datasetId);
		paramMap.put("task_status", Constants.DATASET_PROCESS_CONCAT);
		dataSetDao.updateDataSet(paramMap);
		
		ThreadSchedule.execThread(()->runVideoConcat(datasetId, videoList,destFileName));

		return "success";
	}


	private void runVideoConcat(String datasetId, List<VideoInfo> videoList,String destFileName) {

		long start = System.currentTimeMillis();
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", datasetId);

		String tmpPath = LabelDataSetMerge.getUserDataSetPath() + File.separator + "video_" +  System.nanoTime();
		//获取图片，解压
		try {
			new File(tmpPath).mkdirs();
			List<File> videoFileList = new ArrayList<>();

			Map<String,String> concatPara = new HashMap<>();
			concatPara.put("destFileName", destFileName);
			int time = 0;
			for(VideoInfo videoInfo : videoList ) {
				String tmp[] = videoInfo.getMinio_url().split("/");
				int length = tmp.length;
				logger.info("bucketname=" + tmp[length-2] + " objectname=" + tmp[length-1]);
				String downloadFilePath = fileService.downLoadFileFromMinio(tmp[length-2], tmp[length-1], tmpPath);
				videoFileList.add(new File(downloadFilePath));
				time += VideoUtil.getIntSecond(videoInfo.getDuration());
			}

			setProgress(datasetId,time/100 + 10);
			
			String destFilePath = VideoUtil.concat(videoFileList, concatPara);

			//上传到minio中，并存储到dataset中
			FileResult result = fileService.uploadVideoFile(new File(destFilePath));

			//删除原始视频
			deleteMultiVideo(datasetId, videoList);

			paramMap.put("zip_bucket_name", result.getBucket_name());
			paramMap.put("zip_object_name", result.getObject_name());
			paramMap.put("task_status", Constants.DATASET_PROCESS_PREPARE);
			paramMap.put("videoSet", "");
			dataSetDao.updateDataSet(paramMap);

			VideoInfo mainVideoInfo = VideoUtil.getVideoObj(new File(destFilePath));
			if(mainVideoInfo == null) {
				logger.info("the video is not support.");
				mainVideoInfo = new VideoInfo();
				mainVideoInfo.setVideoCode("Not support.");
			}
			String mainVideoStr = gson.toJson(mainVideoInfo);
			paramMap.clear();
			paramMap.put("id", datasetId);
			paramMap.put("mainVideoInfo", mainVideoStr);
			dataSetDao.updateDataSet(paramMap);
			
			logger.info("concat video cost:" + (System.currentTimeMillis() - start)/1000 + " s");
		}catch (Exception e) {
			logger.info(e.getMessage(),e);
			paramMap.put("task_status", Constants.DATASET_STATUS_ERROR);
			paramMap.put("task_desc","视频合并错误。请上传支持的视频格式，如mp4格式，不要压缩。");
		}finally {
			progressDao.deleteProgress(datasetId);
			FileUtil.delDir(tmpPath);
		}
	}

	private void deleteMultiVideo(String datasetId, List<VideoInfo> videoList) {
		dataSetVideoInfoDao.deleteVideoInfoByDataSetId(datasetId);
		for(VideoInfo video : videoList) {
			fileService.deleteFileFromMinio(video.getMinio_url());
		}

	}

	public String zoomSvs(String token, String datasetId) {
		
	
		DataSet dataSet = dataSetDao.queryDataSetById(datasetId);
		
		largeImgToDziSchedule.addTask(dataSet);
		
		return "succeed.";
		
	}
	
	public static void main(String[] args) {
		String dziPath = "D:\\2020文档\\问题定位\\0728\\dzi.xml";
		Map<String,String> msg = new HashMap<>();
		if(new File(dziPath).exists()) {
			SAXReader reader = new SAXReader();
			Document document;
			try {
				document = reader.read(new FileInputStream(dziPath));
				Element root = document.getRootElement();
				msg.put("Format", root.attributeValue("Format"));
				msg.put("Overlap", root.attributeValue("Overlap"));
				msg.put("TileSize", root.attributeValue("TileSize"));
				
				Element size = root.element("Size");
				
				msg.put("Height", size.attributeValue("Height"));
				msg.put("Width", size.attributeValue("Width"));
			
			}catch (Exception e) {
				logger.info(e.getMessage(),e);
			}
			
		}
		
		logger.info("msg = " + msg);
		
	}

}
