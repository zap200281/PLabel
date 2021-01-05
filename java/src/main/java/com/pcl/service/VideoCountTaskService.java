package com.pcl.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pcl.constant.Constants;
import com.pcl.constant.LogConstants;
import com.pcl.dao.DataSetDao;
import com.pcl.dao.VideoCountTaskDao;
import com.pcl.dao.VideoCountTaskItemDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.PageResult;
import com.pcl.pojo.mybatis.DataSet;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.pojo.mybatis.LogInfo;
import com.pcl.pojo.mybatis.VideoCountTask;
import com.pcl.util.TimeUtil;

@Service
public class VideoCountTaskService {

	private static Logger logger = LoggerFactory.getLogger(VideoCountTaskService.class);
	
	@Autowired
	private VideoCountTaskDao videoCountTaskDao;
	
	@Autowired
	private VideoCountTaskItemDao videoCountTaskItemDao;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private DataSetDao dataSetDao;
	
	@Autowired
	private LogService logService;
	
	public int addVideoCountTask(String token, VideoCountTask task) throws LabelSystemException {
		
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
		
		return videoCountTaskDao.addVideoCountTask(task);
		
	}

	public VideoCountTask queryVideoCountTask(String token, String id) {
		
		return videoCountTaskDao.queryVideoCountTask(id);
	}
	
	public int deleteVideoCountTask(String token, String id) {
		
		videoCountTaskItemDao.deleteLabelTaskByTaskId(id);
		
		return videoCountTaskDao.deleteVideoCountTask(id);
		
	}

	public PageResult queryVideoCountTaskPage(String token, Integer currPage, Integer pageSize) {
		
		PageResult pageResult = new PageResult();
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
	
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		paramMap.put("user_id", userId);
		List<VideoCountTask> videoTaskList = videoCountTaskDao.queryVideoCountTaskPage(paramMap);

		int totalCount = videoCountTaskDao.queryVideoCountTaskPageCount(paramMap);

		pageResult.setTotal(totalCount);
		pageResult.setData(videoTaskList);
		pageResult.setCurrent(currPage);

		if(totalCount > 0) {
			Map<Integer,String> userIdForName = userService.getAllUser();
			for(VideoCountTask videoTask : videoTaskList) {
				if(userIdForName.containsKey(videoTask.getUser_id())) {
					videoTask.setUser(userIdForName.get(videoTask.getUser_id()));
				}
				if(videoTask.getAssign_user_id() == 0) {
					videoTask.setAssign_user(userIdForName.get(videoTask.getUser_id()));
				}else {
					videoTask.setAssign_user(userIdForName.get(videoTask.getAssign_user_id()));
				}
			}
		}

		return pageResult;
		
	}

	public int addLabelTaskItem(LabelTaskItem body, String token) {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		body.setId(UUID.randomUUID().toString().replaceAll("-",""));
		body.setItem_add_time(TimeUtil.getCurrentTimeStr());
		
		int re = videoCountTaskItemDao.addLabelTaskItem(body);
		
		logAddLabelTaskItem(body, userId);
		return re;
	}
	
	private void logAddLabelTaskItem( LabelTaskItem newBody,int user_id) {
		LogInfo logInfo = new LogInfo();
		logInfo.setOper_type(LogConstants.LOG_UPATE);
		logInfo.setUser_id(user_id);
		logInfo.setOper_name("视频流统计标注");
		logInfo.setOper_id(LogConstants.LOG_VEDIO_COUNT_LABEL_ITEM);
		logInfo.setOper_time_start(newBody.getItem_add_time());
		logInfo.setOper_time_end(newBody.getItem_add_time());
		logInfo.setOper_json_content_new(newBody.getLabel_info());
		logInfo.setOper_json_content_old(null);
		logInfo.setRecord_id(newBody.getId());
		logInfo.setExtend2(newBody.getPic_image_field());
		logService.addLogInfo(logInfo);
	}

	public PageResult queryVideoCountTaskItemPage(String label_task, Integer currPage, Integer pageSize) {
		
		PageResult pageResult = new PageResult();

		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		paramMap.put("label_task_id", label_task);
		
		List<LabelTaskItem> result = videoCountTaskItemDao.queryLabelTaskItemPageByLabelTaskId(paramMap);
		int totalCount = videoCountTaskItemDao.queryLabelTaskItemPageCountByLabelTaskId(paramMap);
		pageResult.setTotal(totalCount);
		pageResult.setData(result);
		pageResult.setCurrent(currPage);

		return pageResult;
	}

	public int deleteLabelTaskItem(String id, String token) {
		
		return videoCountTaskItemDao.deleteLabelTaskById(id);
		
	}

	public int updateLabelTaskItem(LabelTaskItem body, String token) {
		
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", body.getId());
		paramMap.put("label_info", body.getLabel_info());
		paramMap.put("label_status", body.getLabel_status());
		paramMap.put("item_add_time", TimeUtil.getCurrentTimeStr());
		
		return videoCountTaskItemDao.updateLabelTaskItem(paramMap);
	}

	public int updateVideoCountLabelTaskStatus(String token, String id, int task_status) {
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", id);
		paramMap.put("task_status", task_status);
		
		return videoCountTaskDao.updateVideoCountLabelTaskStatus(paramMap);
		
	}

	public List<LabelTaskItem> queryVideoCountTaskItemByLocus(String label_task, String locus) {
		
		List<LabelTaskItem> dbList = videoCountTaskItemDao.queryLabelTaskItemByLabelTaskId(label_task);
		List<LabelTaskItem> result = new ArrayList<>();
		String dest = "\"traj_mode\":\"" + locus + "\"";
		for(LabelTaskItem item : dbList) {
			String labelInfo = item.getLabel_info();
			if(labelInfo != null && labelInfo.contains(dest)) {
				result.add(item);
			}
		}
		return result;
	}
	
	
	
}
