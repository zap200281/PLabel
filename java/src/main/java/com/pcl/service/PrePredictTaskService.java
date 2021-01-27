package com.pcl.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pcl.constant.Constants;
import com.pcl.constant.UserConstants;
import com.pcl.dao.AlgModelDao;
import com.pcl.dao.DataSetDao;
import com.pcl.dao.PrePredictTaskDao;
import com.pcl.dao.PrePredictTaskResultDao;
import com.pcl.dao.UserDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.PageResult;
import com.pcl.pojo.body.PrePredictTaskBody;
import com.pcl.pojo.display.DisplayPrePredictTask;
import com.pcl.pojo.display.DisplaySimplePrePredictTask;
import com.pcl.pojo.mybatis.AlgModel;
import com.pcl.pojo.mybatis.DataSet;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.pojo.mybatis.PrePredictTask;
import com.pcl.pojo.mybatis.PrePredictTaskResult;
import com.pcl.pojo.mybatis.User;
import com.pcl.service.schedule.LabelForPictureSchedule;
import com.pcl.util.JsonUtil;
import com.pcl.util.TimeUtil;


@Service
public class PrePredictTaskService {

	private static Logger logger = LoggerFactory.getLogger(PrePredictTaskService.class);
	
	@Autowired
	private PrePredictTaskDao prePredictTaskDao;
	
	
	@Autowired
	private AlgModelDao algModelDao;
	
	@Autowired
	private UserDao userDao;
	
	@Autowired
	private LabelForPictureSchedule schedule;
	
	@Autowired
	private DataSetDao dataSetDao;
	
	@Autowired
	private PrePredictTaskResultDao prePredictTaskResultDao;
	
	private ConcurrentHashMap<String, Long> requestForTime = new ConcurrentHashMap<>();
	
	public int addPrePredictTask(String token,PrePredictTaskBody body) throws LabelSystemException {
		long start = System.currentTimeMillis();
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		
		String str = JsonUtil.toJson(body);
		
		if(requestForTime.containsKey(str)) {
			logger.info("repeart str=" + str);
			long time = requestForTime.get(str);
			if(System.currentTimeMillis() - time < 60 * 1000) {
				throw new LabelSystemException("不要重复提交自动标注。");
			}
		}else {
			//logger.info("put str" + str);
			requestForTime.put(str, System.currentTimeMillis());
		}
		
		PrePredictTask prePredictTask = new PrePredictTask();
		prePredictTask.setId(UUID.randomUUID().toString().replaceAll("-",""));
		prePredictTask.setTask_start_time(TimeUtil.getCurrentTimeStr());
		prePredictTask.setUser_id(userId);
		prePredictTask.setTask_name(body.getTaskName());
		
		prePredictTask.setTask_status(Constants.PREDICT_TASK_STATUS_WAIT_GPU);
		prePredictTask.setAlg_model_id(body.getAlgModel());
		
		prePredictTask.setDataset_id(body.getDataSetId());
		
		prePredictTask.setDelete_no_label_picture(body.getDeleteNoLabelPicture());
		prePredictTask.setScore_threshhold(body.getScore_threshhold());
		prePredictTask.setNeedToDistiguishTypeOrColor(body.getNeedToDistiguishTypeOrColor());
		prePredictTask.setDelete_similar_picture(body.getDelete_similar_picture());
		
		//增加限制，如果没有图片，则不能创建。
		DataSet dataSet = dataSetDao.queryDataSetById(body.getDataSetId());
//		if(dataSet.getTotal() == 0 && dataSet.getDataset_type() == Constants.DATASET_TYPE_VIDEO) {
//			//如果是视频自动标注，则抽
//			
//			
//			throw new LabelSystemException("数据集中图片数量为0，不能创建自动标注。");
//		}
		
		int re = prePredictTaskDao.addPrePredictTask(prePredictTask);
		
		schedule.addTask(prePredictTask);
		logger.info("addPrePredictTask cost: " + (System.currentTimeMillis() - start) + " ms");
		return re;
	}
	
	
	public int addPrePredictTask(int userId,PrePredictTaskBody body) throws LabelSystemException {
		long start = System.currentTimeMillis();

		PrePredictTask prePredictTask = new PrePredictTask();
		prePredictTask.setId(UUID.randomUUID().toString().replaceAll("-",""));
		prePredictTask.setTask_start_time(TimeUtil.getCurrentTimeStr());
		prePredictTask.setUser_id(userId);
		prePredictTask.setTask_name(body.getTaskName());
		
		prePredictTask.setTask_status(Constants.PREDICT_TASK_STATUS_WAIT_GPU);
		prePredictTask.setAlg_model_id(body.getAlgModel());
		
		prePredictTask.setDataset_id(body.getDataSetId());
		
		prePredictTask.setDelete_no_label_picture(body.getDeleteNoLabelPicture());
		prePredictTask.setScore_threshhold(body.getScore_threshhold());
		prePredictTask.setNeedToDistiguishTypeOrColor(body.getNeedToDistiguishTypeOrColor());
		prePredictTask.setDelete_similar_picture(body.getDelete_similar_picture());
		
		//增加限制，如果没有图片，则不能创建。
		//DataSet dataSet = dataSetDao.queryDataSetById(body.getDataSetId());
		//if(dataSet.getTotal() == 0) {
		//	throw new LabelSystemException("数据集中图片数量为0，不能创建自动标注。");
		//}
		
		int re = prePredictTaskDao.addPrePredictTask(prePredictTask);
		
		schedule.addTask(prePredictTask);
		logger.info("add auto PrePredictTask cost: " + (System.currentTimeMillis() - start) + " ms");
		return re;
	}
	

	public List<DisplaySimplePrePredictTask> queryAllPredictTask(String token){
		long start = System.currentTimeMillis();
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		
		List<DisplaySimplePrePredictTask> re = new ArrayList<>();
		
		List<PrePredictTask> dbList = prePredictTaskDao.queryAllPrePredictTaskByUser(userId);
		
		for(PrePredictTask task : dbList) {
			DisplaySimplePrePredictTask t = new DisplaySimplePrePredictTask();
			t.setId(task.getId());
			t.setTask_name(task.getTask_name());
			re.add(t);
		}
		logger.info("queryAllPredictTask cost: " + (System.currentTimeMillis() - start) + " ms");
		return re;
	}
	
	public PageResult queryPredictTaskItemByTaskId(String token, String predictTaskId,int currPage, int pageSize){
		long start = System.currentTimeMillis();
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		
		PageResult pageResult = new PageResult();
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		paramMap.put("pre_predict_task_id", predictTaskId);
		paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.PREDICT_SINGLE_TABLE));
		
		List<PrePredictTaskResult> taskResultList = prePredictTaskResultDao.queryPredictTaskItemPageByTaskId(paramMap);
		int totalCount = prePredictTaskResultDao.queryPredictTaskItemPageCountByTaskId(paramMap);
		List<LabelTaskItem> batchList = new ArrayList<>();
		//拷贝预检完成到labeltaskitem中
		for(PrePredictTaskResult preTaskResult : taskResultList) {
			LabelTaskItem taskItem = new LabelTaskItem();
			taskItem.setId(preTaskResult.getId());
			taskItem.setItem_add_time(TimeUtil.getCurrentTimeStr());
			taskItem.setLabel_info(preTaskResult.getLabel_info());
			taskItem.setLabel_task_id(predictTaskId);
			taskItem.setPic_image_field(preTaskResult.getPic_image_field());
			taskItem.setPic_object_name(preTaskResult.getPic_object_name());
			taskItem.setPic_url(preTaskResult.getPic_url());
			taskItem.setLabel_status(Constants.LABEL_TASK_STATUS_NOT_FINISHED);
			batchList.add(taskItem);
		}
		pageResult.setTotal(totalCount);
		pageResult.setData(batchList);
		pageResult.setCurrent(currPage);
		
		logger.info("queryPredictTaskItemByTaskId cost: " + (System.currentTimeMillis() - start) + " ms");
		
		return pageResult;
		
	}
	
	public PageResult selectPredictTask(String token,int currPage, int pageSize){
		long start = System.currentTimeMillis();
		
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		
		List<DisplayPrePredictTask> reList = new ArrayList<>();
		
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		paramMap.put("user_id", userId);
		List<PrePredictTask> dbList = prePredictTaskDao.queryPrePredictTask(paramMap);
		int totalCount = prePredictTaskDao.queryPrePredictTaskCount(paramMap);
		
		List<AlgModel> allModelList = algModelDao.queryAlgModelForAutoLabel();
		
		HashMap<Integer,String> modelIdForName = new HashMap<>();
		for(AlgModel algModel : allModelList) {
			modelIdForName.put(algModel.getId(), algModel.getModel_name());
		}
		
		List<User> userList = userDao.queryAll();
		HashMap<Integer,String> userIdForName = new HashMap<>();
		for(User user : userList) {
			userIdForName.put(user.getId(), user.getUsername());
		}
		
		List<DataSet> dataSetList = dataSetDao.queryAllDataSet();
		HashMap<String,String> dataSetIdForName = new HashMap<>();
		for(DataSet dataSet : dataSetList) {
			dataSetIdForName.put(dataSet.getId(), dataSet.getTask_name());
		}
		
		for(PrePredictTask task : dbList) {
			
			DisplayPrePredictTask displayTask = new DisplayPrePredictTask();
			reList.add(displayTask);
			
			displayTask.setId(task.getId());
			if(modelIdForName.containsKey(task.getAlg_model_id())) {
				displayTask.setAlg_model(modelIdForName.get(task.getAlg_model_id()));
			}
			displayTask.setTask_name(task.getTask_name());
			displayTask.setTask_start_time(task.getTask_start_time());
			displayTask.setTask_status(task.getTask_status());
			displayTask.setTask_status_desc(task.getTask_status_desc() == null ? "" : task.getTask_status_desc());
			if(userIdForName.containsKey(userId)) {
				displayTask.setUser(userIdForName.get(userId));
			}
			displayTask.setDataset_name(dataSetIdForName.get(task.getDataset_id()));
			displayTask.setScore_threshhold(task.getScore_threshhold());
			displayTask.setDelete_no_label_picture(task.getDelete_no_label_picture());
			displayTask.setDelete_similar_picture(task.getDelete_similar_picture());
			displayTask.setNeedToDistiguishTypeOrColor(task.getNeedToDistiguishTypeOrColor());
		}
		
		PageResult re = new PageResult();
		re.setTotal(totalCount);
		re.setData(reList);
		re.setCurrent(currPage);
		
		logger.info("selectPredictTask cost: " + (System.currentTimeMillis() - start) + " ms");
		
		return re;
		
	}


	public void deletePrePredictTaskById(String token, String predictTaskId) {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				asyncDelete(userId, predictTaskId);
			}}).start();
		
	}
	
	private void asyncDelete(int userId, String predictTaskId) {
		
		long start = System.currentTimeMillis();
		logger.info("start to async delete predict data.");
		prePredictTaskResultDao.deletePrePredictTaskResultById(TokenManager.getUserTablePos(userId, UserConstants.PREDICT_SINGLE_TABLE),predictTaskId);
		
		prePredictTaskDao.deletePrePredictTaskById(predictTaskId);
		logger.info("async deletePrePredictTaskById cost: " + (System.currentTimeMillis() - start) + " ms");
	}
	
	public void updateProgressMsg(String taskId,Map<String,Object> msg) {
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", taskId);
		
		String index = (String)msg.get("index");
		String total = (String)msg.get("total");
		
		int progress = getProgress(index,total);
		
		StringBuilder str = new StringBuilder();
		str.append(progress).append("%");
		//str.append("当前文件：").append(msg.get("filename"));
		logger.info(str.toString() + " current file:" + msg.get("filename"));
//		try {
//			logger.info(str.toString() + " current file：" + new String(msg.get("filename").toString().getBytes(),"utf-8"));
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
		paramMap.put("task_status_desc", str.toString());
		
		prePredictTaskDao.updatePrePredictTaskStatusDesc(paramMap);
		
	}


	private int getProgress(String index, String total) {
		
		double re = Double.parseDouble(index) / Double.parseDouble(total);
		
		return (int)(re * 100);
	}
	

}
