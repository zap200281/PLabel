package com.pcl.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.pcl.constant.Constants;
import com.pcl.constant.LogConstants;
import com.pcl.dao.DataSetDao;
import com.pcl.dao.VideoLabelTaskDao;
import com.pcl.dao.VideoLabelTaskItemDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.PageResult;
import com.pcl.pojo.mybatis.DataSet;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.pojo.mybatis.LogInfo;
import com.pcl.pojo.mybatis.VideoInfo;
import com.pcl.pojo.mybatis.VideoLabelTask;
import com.pcl.service.schedule.AutoLabelVideoSchedule;
import com.pcl.util.BucketNameUtil;
import com.pcl.util.FileUtil;
import com.pcl.util.JsonUtil;
import com.pcl.util.TimeUtil;
import com.pcl.util.VideoUtil;

@Service
public class VideoLabelTaskService {

	private static Logger logger = LoggerFactory.getLogger(VideoLabelTaskService.class);
	
	@Autowired
	private VideoLabelTaskDao videoLabelTaskDao;
	
	@Autowired
	private VideoLabelTaskItemDao videoLabelTaskItemDao;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private ObjectFileService fileService;
	
	@Autowired
	private DataSetDao dataSetDao;
	
	@Autowired
	private AutoLabelVideoSchedule autoLabelVideoSchedule;
	
	@Autowired
	private LogService logService;
	
	private Gson gson = new Gson();
	
	private ThreadPoolExecutor threadPool = new ThreadPoolExecutor(2,2,100,TimeUnit.MINUTES,new ArrayBlockingQueue<Runnable>(1000));

	
	public int addVideoLabelTask(String token, VideoLabelTask task) throws LabelSystemException {
		
		logger.info("addVideoCountTask to db. ");
		
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		
		DataSet dataSet = dataSetDao.queryDataSetById(task.getDataset_id());
		if(dataSet == null) {
			logger.info("The dataset is not exists. id=" + task.getDataset_id());
			throw new LabelSystemException("The dataset is not exists. id=" + task.getDataset_id());
		}
		
		task.setId(UUID.randomUUID().toString().replaceAll("-",""));
		task.setUser_id(userId);
		task.setTask_add_time(TimeUtil.getCurrentTimeStr());
		task.setTask_status(Constants.VIDEO_TASK_STATUS_START);
		task.setZip_object_name("/minio/" + dataSet.getZip_bucket_name() + "/" + dataSet.getZip_object_name());
		task.setMainVideoInfo(dataSet.getMainVideoInfo());
		
		int count = videoLabelTaskDao.addVideoLabelTask(task);
		
		return count;
	}

	public VideoLabelTask queryVideoLabelTask(String token, String id) {
		
		VideoLabelTask videoTask =  videoLabelTaskDao.queryVideoLabelTask(id);
		
		videoTask.setVideoInfo(gson.fromJson(videoTask.getMainVideoInfo(), VideoInfo.class));
		
		return videoTask;
		
	}
	
	public int deleteVideoLabelTask(String token, String id) {
		
		int re =  videoLabelTaskDao.deleteVideoLabelTask(id);
		new Thread(()-> {
			//异步删除标注信息中的minio文件
			List<LabelTaskItem> itemList = videoLabelTaskItemDao.queryLabelTaskItemByLabelTaskId(id);
			for(LabelTaskItem item :itemList) {
				fileService.deleteFileFromMinio(item.getPic_image_field());
			}
			videoLabelTaskItemDao.deleteLabelTaskByTaskId(id);
		}).start();
		return re;
	}

	public PageResult queryVideoLabelTaskPage(String token, Integer currPage, Integer pageSize) {
		
		PageResult pageResult = new PageResult();
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
	
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		paramMap.put("user_id", userId);
		List<VideoLabelTask> videoTaskList = videoLabelTaskDao.queryVideoLabelTaskPage(paramMap);

		int totalCount = videoLabelTaskDao.queryVideoLabelTaskPageCount(paramMap);

		pageResult.setTotal(totalCount);
		pageResult.setData(videoTaskList);
		pageResult.setCurrent(currPage);

		if(totalCount > 0) {
			Map<Integer,String> userIdForName = userService.getAllUser();
			for(VideoLabelTask videoTask : videoTaskList) {
				if(userIdForName.containsKey(videoTask.getUser_id())) {
					videoTask.setUser(userIdForName.get(videoTask.getUser_id()));
				}
				if(videoTask.getAssign_user_id() == 0) {
					videoTask.setAssign_user(userIdForName.get(videoTask.getUser_id()));
				}else {
					videoTask.setAssign_user(userIdForName.get(videoTask.getAssign_user_id()));
				}
				videoTask.setVideoInfo(gson.fromJson(videoTask.getMainVideoInfo(), VideoInfo.class));
			}
		}

		return pageResult;
		
	}

	public String addLabelTaskItem(LabelTaskItem body, String token) throws LabelSystemException {
		
		logger.info("addLabelTaskItem=" +gson.toJson(body));
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		body.setId(UUID.randomUUID().toString().replaceAll("-",""));
		body.setItem_add_time(TimeUtil.getCurrentTimeStr());
		
		String time = body.getPic_url(); //时间格式为  00:20:01.367
		String labelTaskId = body.getLabel_task_id();
		VideoLabelTask task = videoLabelTaskDao.queryVideoLabelTask(labelTaskId);
		
		if(task == null) {
			throw new LabelSystemException("the task not exist.id=" + labelTaskId);
		}
		
		videoLabelTaskItemDao.addLabelTaskItem(body);
		
		threadPool.execute(()->extractPicture(time, task,body,userId));
		
		return body.getId();
		
	}

	private void extractPicture(String time, VideoLabelTask task,LabelTaskItem body,int userId) {
		long start = System.currentTimeMillis();
		String tmpPath = LabelDataSetMerge.getUserDataSetPath() + File.separator + "video_" +  System.nanoTime();
		//获取图片，解压
		try {
			new File(tmpPath).mkdirs();
			String tmp[] = task.getZip_object_name().split("/");
			int length = tmp.length;

			logger.info("bucketname=" + tmp[length-2] + " objectname=" + tmp[length-1]);
			String downloadFilePath = fileService.downLoadFileFromMinio(tmp[length-2], tmp[length-1],tmpPath);
			File downloadFile = new File(downloadFilePath);
			String picture = time.replace(":", "_");
			picture = picture.replace(".", "_") + ".jpg";
			picture = tmpPath + File.separator + picture;
			String picturePath = VideoUtil.getPictureFromVideo(time, downloadFile, picture);
			logger.info("picturePath=" + picturePath);
			String bucketName = downloadFile.getName();
			bucketName = bucketName.substring(0,bucketName.lastIndexOf("."));
			bucketName = BucketNameUtil.getRightBucketName(bucketName);
			File pictureFile = new File(picturePath);
			fileService.uploadPictureFile(Arrays.asList(pictureFile), bucketName);
			String pic_image_field = "/minio/" +bucketName + "/" + pictureFile.getName();
			
			String oldWidthHeight = body.getPic_object_name();
			
			String newWidthHeight = getWidthHeight(pictureFile);
			body.setPic_object_name(newWidthHeight);
			body.setLabel_info(getNewLabelInfo(oldWidthHeight,newWidthHeight,body.getLabel_info()));
			body.setPic_image_field(pic_image_field);
			body.setItem_add_time( TimeUtil.getCurrentTimeStr());
			
			Map<String,Object> paramMap = new HashMap<>();
			paramMap.put("id", body.getId());
			paramMap.put("pic_object_name", body.getPic_object_name());
			paramMap.put("label_info", body.getLabel_info());
			paramMap.put("pic_image_field", body.getPic_image_field());
			paramMap.put("item_add_time", body.getItem_add_time());
			paramMap.put("label_status", body.getLabel_status());
			
			videoLabelTaskItemDao.updateLabelTaskItem(paramMap);
			
			logAddLabelTaskItem(body, userId);
			
			if(Constants.LABEL_TASK_STATUS_NOT_FINISHED == body.getLabel_status()) {
				//进行自动标注
				autoLabelVideoSchedule.labelPicture(pictureFile.getAbsolutePath(),Constants.AUTO_LABLE_VIDEO_PICTURE_TASK, body,userId);
			}
		
			logger.info("Extract picture cost:" + (System.currentTimeMillis() - start) + " ms");
		}catch (Exception e) {
			logger.info(e.getMessage(),e);
		}finally {
			FileUtil.delDir(tmpPath);
		}
	}
	
	private void logAddLabelTaskItem( LabelTaskItem newBody,int user_id) {
		LogInfo logInfo = new LogInfo();
		logInfo.setOper_type(LogConstants.LOG_UPATE);
		logInfo.setUser_id(user_id);
		logInfo.setOper_name("视频标注");
		logInfo.setOper_id(LogConstants.LOG_VEDIO_LABEL_TASK_ITEM);
		logInfo.setOper_time_start(newBody.getItem_add_time());
		logInfo.setOper_time_end(newBody.getItem_add_time());
		logInfo.setOper_json_content_new(newBody.getLabel_info());
		logInfo.setOper_json_content_old(null);
		logInfo.setRecord_id(newBody.getId());
		logInfo.setExtend2(newBody.getPic_image_field());
		logService.addLogInfo(logInfo);
	}
	
	private void logUpdateLabelTaskItem(LabelTaskItem oldLabelTaskItem, LabelTaskItem updateBody,int user_id) {
		LogInfo logInfo = new LogInfo();
		logInfo.setOper_type(LogConstants.LOG_UPATE);
		logInfo.setUser_id(user_id);
		logInfo.setOper_name("视频标注");
		logInfo.setOper_id(LogConstants.LOG_VEDIO_LABEL_TASK_ITEM);
		logInfo.setOper_time_start(updateBody.getItem_add_time());
		logInfo.setOper_time_end(updateBody.getItem_add_time());
		logInfo.setOper_json_content_new(updateBody.getLabel_info());
		logInfo.setOper_json_content_old(oldLabelTaskItem.getLabel_info());
		logInfo.setRecord_id(oldLabelTaskItem.getId());
		logInfo.setExtend2(updateBody.getPic_image_field());
		logService.addLogInfo(logInfo);
	}

	@SuppressWarnings("unchecked")
	private String getNewLabelInfo(String oldWidthHeight, String newWidthHeight, String labelInfo) {
		if(oldWidthHeight.equals(newWidthHeight)) {
			return labelInfo;
		}
		String olds[] = oldWidthHeight.split(",");
		String news[] = newWidthHeight.split(",");
		
		double widthRation = Double.parseDouble(news[0]) / Double.parseDouble(olds[0]);
		double heightRation = Double.parseDouble(news[1]) / Double.parseDouble(olds[1]);
		
		List<Map<String,Object>> labelList = JsonUtil.getLabelList(labelInfo);
		for(Map<String,Object> label : labelList) {
			List<Object> boxList = (List<Object>)label.remove("box");
			if(boxList != null) {
				convertNewList(widthRation, heightRation, boxList, boxList.size());
				label.put("box", boxList);
			}
			
			List<Object> maskList = (List<Object>)label.remove("mask");
			if(maskList != null) {
				convertNewList(widthRation, heightRation, maskList, maskList.size());
				label.put("mask", maskList);
			}
			
			List<Object> keypointsList = (List<Object>)label.remove("keypoints");
			if(keypointsList != null) {
				convertNewList(widthRation, heightRation, keypointsList, 2);
				label.put("keypoints", keypointsList);
			}
		}
		
		return gson.toJson(labelList);
	}

	private void convertNewList(double widthRation, double heightRation, List<Object> boxList, int length) {
		
		for(int i = 0; i < boxList.size() && i < length; i++) {
			if(i % 2 == 0) {
				boxList.set(i,getIntStr(String.valueOf(boxList.get(i)),widthRation));
			}else {
				boxList.set(i,getIntStr(String.valueOf(boxList.get(i)),heightRation));
			}
		}
	}
	
	public static int getIntStr(String doubleStr,double ration) {
		double tmp = Double.parseDouble(doubleStr);
		tmp = tmp * ration;
		return (int)Math.round(tmp);
	}

	private String getWidthHeight(File pictureFile) throws IOException {
		try(InputStream inputStream = new FileInputStream(pictureFile)){
			BufferedImage sourceImg =ImageIO.read(inputStream);
			int width = sourceImg.getWidth();
			int height = sourceImg.getHeight();
			return width + "," + height;
		}
	}

	public PageResult queryVideoLabelTaskItemPage(String label_task, Integer currPage, Integer pageSize,int orderBy) {
		
		PageResult pageResult = new PageResult();

		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		paramMap.put("label_task_id", label_task);
		List<LabelTaskItem> result = null;
		if(orderBy == 1) {
			result = videoLabelTaskItemDao.queryLabelTaskItemPageByLabelTaskIdOrderByStartTime(paramMap);
		}else {
			result = videoLabelTaskItemDao.queryLabelTaskItemPageByLabelTaskId(paramMap);
		}
		
		int totalCount = videoLabelTaskItemDao.queryLabelTaskItemPageCountByLabelTaskId(paramMap);
		pageResult.setTotal(totalCount);
		pageResult.setData(result);
		pageResult.setCurrent(currPage);

		return pageResult;
	}

	public int deleteLabelTaskItem(String id, String token) {
		
		LabelTaskItem item = videoLabelTaskItemDao.queryLabelTaskItemById(id);
		
		int re = videoLabelTaskItemDao.deleteLabelTaskById(id);
		
		fileService.deleteFileFromMinio(item.getPic_image_field());
		
		return re;
		
	}

	public int updateLabelTaskItem(LabelTaskItem newBody, String token) {
		logger.info("update body=" + gson.toJson(newBody));
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		LabelTaskItem oldItem = videoLabelTaskItemDao.queryLabelTaskItemById(newBody.getId());
		
		String labelInfo = newBody.getLabel_info();
		
		if(newBody.getPic_object_name() != null && oldItem.getPic_object_name() != null) {
			if(!newBody.getPic_object_name().equals(oldItem.getPic_object_name())) {
				labelInfo = getNewLabelInfo(newBody.getPic_object_name(), oldItem.getPic_object_name(), labelInfo);
			}
		}
		newBody.setLabel_info(labelInfo);
		newBody.setItem_add_time(TimeUtil.getCurrentTimeStr());
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", newBody.getId());
		paramMap.put("label_info", newBody.getLabel_info());
		paramMap.put("label_status", newBody.getLabel_status());
		paramMap.put("item_add_time", newBody.getItem_add_time());
		
		int re = videoLabelTaskItemDao.updateLabelTaskItem(paramMap);
		
		logUpdateLabelTaskItem(oldItem, newBody, userId);
		
		return re;
	}

	public LabelTaskItem queryVideoLabelTaskItemByTime(String label_task, String time) {
		
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("pic_url", time);
		paramMap.put("label_task_id", label_task);
		
		List<LabelTaskItem> result = videoLabelTaskItemDao.queryVideoLabelTaskItemByTime(paramMap);

		if(result != null && result.size() > 0) {
			return result.get(0);
		}
		
		return null;
	}

	public String upateAutoLabelInfo(Map<String, Object> msgMap) {
		
		String filename = (String)msgMap.get("filename");
		String itemId = filename.substring(0,filename.length() - 4);
		
		LabelTaskItem item = videoLabelTaskItemDao.queryLabelTaskItemById(itemId);
		if(item == null) {
			logger.info("the id is null.id=" + itemId);
			return itemId;
		}
		if(item.getLabel_status() == Constants.LABEL_TASK_STATUS_NOT_FINISHED) {
			Object labelInfo = msgMap.get("label_info");
			Map<String,Object> paramMap = new HashMap<>();
			paramMap.put("id", itemId);
			paramMap.put("label_info", gson.toJson(labelInfo));
			paramMap.put("label_status", Constants.LABEL_TASK_STATUS_FINISHED);
			paramMap.put("item_add_time", TimeUtil.getCurrentTimeStr());
			videoLabelTaskItemDao.updateLabelTaskItem(paramMap);
		}else {
			logger.info("the id status is finished. item=" + gson.toJson(item));
		}
		return item.getLabel_task_id();
	}

	public void updateVideoLabelTask(String token, String id, String taskLabelTypeInfo) {
		
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", id);
		paramMap.put("task_label_type_info", taskLabelTypeInfo);

		videoLabelTaskDao.updateVideoLabelTask(paramMap);
		
	}

	public void updateVideoLabelTaskStatus(String token, String id, int task_status) {
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", id);
		paramMap.put("task_status", task_status);
		
		videoLabelTaskDao.updateVideoLabelTaskStatus(paramMap);
		
	}
	
}
