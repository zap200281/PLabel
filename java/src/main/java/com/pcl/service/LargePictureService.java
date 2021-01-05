package com.pcl.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pcl.constant.Constants;
import com.pcl.dao.DataSetDao;
import com.pcl.dao.LargePictureTaskDao;
import com.pcl.dao.LargePictureTaskItemDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.PageResult;
import com.pcl.pojo.mybatis.DataSet;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.pojo.mybatis.LargePictureTask;
import com.pcl.util.TimeUtil;

@Service
public class LargePictureService {

	private static Logger logger = LoggerFactory.getLogger(LargePictureService.class);
	
	@Autowired
	private LargePictureTaskDao largePictureTaskDao;
	
	@Autowired
	private LargePictureTaskItemDao largePictureTaskItemDao;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private DataSetDao dataSetDao;
	

	public void addLargePictureTask(String token, LargePictureTask task) throws LabelSystemException {
		
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		
		DataSet dataSet = dataSetDao.queryDataSetById(task.getDataset_id());
		if(dataSet == null) {
			logger.info("The dataset is not exists. id=" + task.getDataset_id());
			throw new LabelSystemException("The dataset is not exists. id=" + task.getDataset_id());
		}
		
		task.setId(UUID.randomUUID().toString().replaceAll("-",""));
		task.setUser_id(userId);
		task.setTask_add_time(TimeUtil.getCurrentTimeStr());
		if(task.getTask_status() <= 0) {
			task.setTask_status(Constants.LARGE_TASK_STATUS_START);
		}
		task.setZip_object_name("/minio/" + dataSet.getZip_bucket_name() + "/" + dataSet.getZip_object_name());
		task.setMainVideoInfo(dataSet.getMainVideoInfo());
		
		largePictureTaskDao.addLargePictureTask(task);
		
		
		LabelTaskItem taskItem = new LabelTaskItem();
		
		taskItem.setId(UUID.randomUUID().toString().replaceAll("-",""));
		taskItem.setItem_add_time(TimeUtil.getCurrentTimeStr());
		taskItem.setLabel_task_id(task.getId());
		taskItem.setLabel_status(Constants.LABEL_TASK_STATUS_NOT_FINISHED);
		
		largePictureTaskItemDao.addLabelTaskItem(taskItem);
	
	}

	public LargePictureTask queryLargePictureTask(String token, String id) {
		
		return largePictureTaskDao.queryLargePictureTask(id);
		
	}

	public int deleteLargePictureTask(String token, String id) {
		
		return largePictureTaskDao.deleteLargePictureTask(id);
	}

	public void updateLargePictureTask(String token, String id, String taskLabelTypeInfo) {
		
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", id);
		paramMap.put("task_label_type_info", taskLabelTypeInfo);
		
		largePictureTaskDao.updateLargePictureTask(paramMap);
		
		
	}

	public void updateLargePictureTaskStatus(String token, String id, int task_status) {
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", id);
		paramMap.put("task_status", task_status);
		
		largePictureTaskDao.updateLargePictureTaskStatus(paramMap);
	}

	public PageResult queryLargePictureTaskPage(String token, Integer currPage, Integer pageSize) {
		return queryLargePictureTaskPage(token, null, currPage, pageSize);
	}



	public int updatePictureTaskLabelItem(LabelTaskItem updateBody, String token) {
		String time = TimeUtil.getCurrentTimeStr();
		updateBody.setItem_add_time(time);
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", updateBody.getId());
		paramMap.put("label_info", updateBody.getLabel_info());
		paramMap.put("label_status", updateBody.getLabel_status());
		paramMap.put("item_add_time", updateBody.getItem_add_time());

		logger.info("update body=" + paramMap.toString());
		
		return largePictureTaskItemDao.updateLabelTaskItem(paramMap);
	}

	public List<LabelTaskItem>  queryPictureTaskLabelItem(String label_task_id) {


		return largePictureTaskItemDao.queryLabelTaskItemByLabelTaskId(label_task_id);
		
	}

	public PageResult queryLargePictureTaskPage(String token, String appid, Integer currPage, Integer pageSize) {
		PageResult pageResult = new PageResult();
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
	
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		paramMap.put("user_id", userId);
		if(appid != null) {
			paramMap.put("appid", appid);
		}
		logger.info("paramMap=" + paramMap.toString());
		List<LargePictureTask> largePictureTaskList = largePictureTaskDao.queryLargePictureTaskPage(paramMap);

		int totalCount = largePictureTaskDao.queryLargePictureTaskPageCount(paramMap);

		pageResult.setTotal(totalCount);
		pageResult.setData(largePictureTaskList);
		pageResult.setCurrent(currPage);

		if(totalCount > 0) {
			Map<Integer,String> userIdForName = userService.getAllUser();
			for(LargePictureTask largePictureTask : largePictureTaskList) {
				if(userIdForName.containsKey(largePictureTask.getUser_id())) {
					largePictureTask.setUser(userIdForName.get(largePictureTask.getUser_id()));
				}
				if(largePictureTask.getAssign_user_id() == 0) {
					largePictureTask.setAssign_user(userIdForName.get(largePictureTask.getUser_id()));
				}else {
					largePictureTask.setAssign_user(userIdForName.get(largePictureTask.getAssign_user_id()));
				}
				
			}
		}

		return pageResult;
	}
	
	
	
}
