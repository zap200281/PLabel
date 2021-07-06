package com.pcl.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.api.client.util.Strings;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.pcl.constant.Constants;
import com.pcl.constant.LogConstants;
import com.pcl.constant.UserConstants;
import com.pcl.dao.DataSetDao;
import com.pcl.dao.LabelDcmTaskItemDao;
import com.pcl.dao.LabelTaskDao;
import com.pcl.dao.LabelTaskItemDao;
import com.pcl.dao.PrePredictTaskDao;
import com.pcl.dao.PrePredictTaskResultDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.PageResult;
import com.pcl.pojo.body.LabelTaskBody;
import com.pcl.pojo.display.DisplayLabelTask;
import com.pcl.pojo.mybatis.DataSet;
import com.pcl.pojo.mybatis.LabelTask;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.pojo.mybatis.LogInfo;
import com.pcl.pojo.mybatis.PrePredictTask;
import com.pcl.pojo.mybatis.PrePredictTaskResult;
import com.pcl.service.schedule.miaod.MIAODSchedule;
import com.pcl.util.JsonUtil;
import com.pcl.util.TimeUtil;

import ij.plugin.DICOM;

@Service
public class LabelTaskService {

	@Autowired
	private LabelTaskDao labelTaskDao;

	@Autowired
	private PrePredictTaskResultDao prePredictTaskResultDao;

	@Autowired
	private PrePredictTaskDao prePredictTaskDao;

	@Autowired
	private LabelTaskItemDao labelTaskItemDao;

	@Autowired
	private LabelDcmTaskItemDao labelDcmTaskItemDao;

	@Autowired
	private UserService userService;

	@Autowired
	private DataSetDao dataSetDao;

	@Autowired
	private LogService logService;
	
	@Autowired
	private ObjectFileService fileService;
	
	@Autowired
	private MIAODSchedule miaod;

	private Gson gson = new Gson();
	
	private static final int VERIFY_UPDATE_FLAG = 100;
	
	private static final int LABEL_UPDATE_FLAG = 0;
	
	private static final String NOT_VALIDE_VALUE = "1";

	private static Logger logger = LoggerFactory.getLogger(LabelTaskService.class);

	public DisplayLabelTask queryLabelTaskById(String token, String id) {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		LabelTask labelTask = labelTaskDao.queryLabelTaskById(id);

		if(labelTask == null) {
			logger.info("the label task is not exist. id=" + id);
			return null;
		}

		Map<String, Integer> countMap = getLabelTaskStatus(userId,Arrays.asList(labelTask));

		DisplayLabelTask reTask = new DisplayLabelTask();
		reTask.setId(labelTask.getId());
		reTask.setTask_flow_type(labelTask.getTask_flow_type());
		reTask.setTask_name(labelTask.getTask_name());
		reTask.setTask_add_time(labelTask.getTask_add_time());
		reTask.setRelate_task_name(labelTask.getRelate_task_name());
		reTask.setTask_type(labelTask.getTask_type());
		reTask.setTask_label_type_info(labelTask.getTask_label_type_info());
		reTask.setRelate_task_id(labelTask.getRelate_task_id());
		
		String otherLabelTask = labelTask.getRelate_other_label_task();
		if(!Strings.isNullOrEmpty(otherLabelTask)) {
			List<String> idLists = gson.fromJson(otherLabelTask, new TypeToken<List<String>>() {
				private static final long serialVersionUID = 1L;}.getType());
			if(idLists != null && idLists.size() > 0) {
				Map<Integer,String> userIdMap = userService.getAllUser();
				List<LabelTask> taskList = labelTaskDao.queryLabelTaskByIds(idLists);
				Map<String,String> idTaskName = new HashMap<>();
				for(LabelTask task : taskList) {
					idTaskName.put(task.getId(), task.getTask_name() + "(" + userIdMap.get(task.getAssign_user_id()) +")");
				}
				reTask.setRelate_other_label_task(gson.toJson(idTaskName));
			}
		}

		setLabelStatus(countMap, labelTask, reTask);

		return reTask;
	}

	public void queryLabelCount(String token) {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("user_id", userId);
		
		new Thread(()->{
			
			long startTime = System.currentTimeMillis();
			logger.info("update userId=" + userId + "  label count start.");
			
			List<LabelTask> taskList = labelTaskDao.queryLabelTaskByUser(paramMap);
			
			for(LabelTask labelTask : taskList) {
				Map<String,Object> tmpParam = new HashMap<>();
				tmpParam.put("label_task_id", labelTask.getId());
				tmpParam.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE));
				int count = 0;
				
				if(labelTask.getTask_type() == Constants.LABEL_TASK_TYPE_ORIGIN_DCM) {
					count = labelDcmTaskItemDao.queryLabelTaskItemPageCountByLabelTaskId(tmpParam);
				}else {
					count =labelTaskItemDao.queryLabelTaskItemPageCountByLabelTaskId(tmpParam);
				}
				
				int total = 0;
				
				int pageSize = 1000;
				for(int i = 0; i < (count/pageSize) +1; i++) {
					tmpParam.put("currPage", i * pageSize);
					tmpParam.put("pageSize", pageSize);
					if(labelTask.getTask_type() == Constants.LABEL_TASK_TYPE_ORIGIN_DCM) {
						List<LabelTaskItem> itemList = labelDcmTaskItemDao.queryLabelTaskItemPageByLabelTaskId(tmpParam);
						total += countItem(itemList);
					}else {
						List<LabelTaskItem> itemList =labelTaskItemDao.queryLabelTaskItemPageByLabelTaskId(tmpParam);
						total += countItem(itemList);
					}
				}
				
				//更新属性
				Map<String,Object> updateParam = new HashMap<>();
				updateParam.put("id", labelTask.getId());
				updateParam.put("total_label", total);
				labelTaskDao.updateLabelTaskLabelCount(updateParam);
				
			}
			
			logger.info("update userId=" + userId + "  label count finished. cost=" + (System.currentTimeMillis() - startTime));
			
		}).start();
		
	}


	private int countItem(List<LabelTaskItem> itemList) {
		int re = 0;
		for(LabelTaskItem item : itemList) {
			String labelInfo = item.getLabel_info();
			if(Strings.isNullOrEmpty(labelInfo)) {
				continue;
			}
			ArrayList<Map<String,Object>> labelList = gson.fromJson(labelInfo, new TypeToken<ArrayList<Map<String,Object>>>() {
				private static final long serialVersionUID = 1L;}.getType());
			if(labelList.isEmpty()) {
				continue;
			}
			re += labelList.size();
		}
		return re;
	}

	public PageResult queryLabelTask(String token,int currPage, int pageSize){

		PageResult pageResult = new PageResult();
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		List<DisplayLabelTask> result = new ArrayList<>();

		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		paramMap.put("user_id", userId);
		List<LabelTask> labelTaskList = labelTaskDao.queryLabelTask(paramMap);

		int totalCount = labelTaskDao.queryLabelTaskCount(paramMap);

		pageResult.setTotal(totalCount);
		pageResult.setData(result);
		pageResult.setCurrent(currPage);

		if(totalCount > 0) {

			Map<Integer,String> userIdForName = userService.getAllUser();
			Map<String, Integer> labelCountMap = getLabelTaskStatus(userId,labelTaskList);

			for(LabelTask labelTask : labelTaskList) {
				DisplayLabelTask reTask = new DisplayLabelTask();
				reTask.setId(labelTask.getId());
				reTask.setTask_name(labelTask.getTask_name());
				reTask.setTask_add_time(labelTask.getTask_add_time());
				reTask.setRelate_task_name(labelTask.getRelate_task_name());
				reTask.setTask_type(labelTask.getTask_type());
				reTask.setTask_flow_type(labelTask.getTask_flow_type());
				setLabelStatus(labelCountMap, labelTask, reTask);
				if(userIdForName.containsKey(labelTask.getUser_id())) {
					reTask.setUser(userIdForName.get(labelTask.getUser_id()));
				}
				if(userIdForName.containsKey(labelTask.getVerify_user_id())) {
					reTask.setVerify_user(userIdForName.get(labelTask.getVerify_user_id()));
				}
				if(labelTask.getAssign_user_id() == 0) {
					reTask.setAssign_user(userIdForName.get(labelTask.getUser_id()));
				}else {
					reTask.setAssign_user(userIdForName.get(labelTask.getAssign_user_id()));
				}
				reTask.setTotal_label(labelTask.getTotal_label());
				
				result.add(reTask);
			}
		}

		return pageResult;
	}

	private void setLabelStatus(Map<String, Integer> countMap, LabelTask labelTask, DisplayLabelTask reTask) {
		int progress = 0;
		int finished = 0;
		String key = labelTask.getId();
		if(labelTask.getTask_status() == Constants.LABEL_TASK_STATUS_LABEL) {
			key += "label";
		}else if(labelTask.getTask_status() == Constants.LABEL_TASK_STATUS_VERIFY){
			key += "verify";
		}
		if(countMap.containsKey(key)) {
			finished = countMap.get(key);
		}
		if(finished == labelTask.getTotal_picture()) {
			progress = 100;
		}else {
			double tmp = finished * 1.0d / labelTask.getTotal_picture();
			tmp *= 100;
			progress = (int)tmp;
		}
		reTask.setTask_status(labelTask.getTask_status());
		reTask.setTask_status_desc(progress + "%(" + finished +  "/" + labelTask.getTotal_picture() + ")");
	}

	private Map<String, Integer> getLabelTaskStatus(int userId,List<LabelTask> labelTaskList) {
		List<String> labelTaskIdList = new ArrayList<>();
		for(LabelTask labelTask : labelTaskList) {
			labelTaskIdList.add(labelTask.getId());
		}

		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE));
		paramMap.put("list", labelTaskIdList);
		
		List<Map<String,Object>> labelTaskStatusList = labelTaskItemDao.queryLabelTaskStatusByLabelTaskId(paramMap);
		Map<String,Integer> countMap = new HashMap<>();
		for(Map<String,Object> map : labelTaskStatusList) {
			String label_task_id = map.get("label_task_id").toString();
			int total = Integer.parseInt(map.get("total").toString());
			int label_not_finished = Integer.parseInt(map.get("label_not_finished").toString());
			int verify_finished = 0;
			if(map.get("verify_finished") != null) {
				verify_finished = Integer.parseInt(map.get("verify_finished").toString());
			}
			countMap.put(label_task_id + "label", total - label_not_finished);
			countMap.put(label_task_id + "verify", verify_finished);
		}

		List<Map<String,Object>> labelDcmTaskStatusList = labelDcmTaskItemDao.queryLabelTaskStatusByLabelTaskId(labelTaskIdList);
		for(Map<String,Object> map : labelDcmTaskStatusList) {
			String label_task_id = map.get("label_task_id").toString();
			int total = Integer.parseInt(map.get("total").toString());
			int label_not_finished = Integer.parseInt(map.get("label_not_finished").toString());
			int verify_finished = 0;
			if(map.get("verify_finished") != null) {
				verify_finished = Integer.parseInt(map.get("verify_finished").toString());
			}
			countMap.put(label_task_id + "label", total - label_not_finished);
			countMap.put(label_task_id + "verify", verify_finished);
		}

		return countMap;
	}


	private void dealDataSetLabelDcmTask(DataSet dataSet,LabelTask labelTask) throws LabelSystemException {

		labelTask.setRelate_task_name(dataSet.getTask_name());
		labelTask.setTask_type(Constants.LABEL_TASK_TYPE_ORIGIN_DCM);
		try {
			//拷贝一次记录，文件不再动
			List<LabelTaskItem> list = labelDcmTaskItemDao.queryLabelTaskItemByLabelTaskId(dataSet.getId());
			labelTask.setTotal_picture(list.size());
			List<LabelTaskItem> batchDcmList = new ArrayList<>();
			for(LabelTaskItem item : list) {
				LabelTaskItem taskItem = new LabelTaskItem();
				taskItem.setId(UUID.randomUUID().toString().replaceAll("-",""));
				taskItem.setItem_add_time(TimeUtil.getCurrentTimeStr());
				taskItem.setLabel_task_id(labelTask.getId());
				taskItem.setPic_image_field(item.getPic_image_field());
				taskItem.setPic_object_name(item.getPic_object_name());
				taskItem.setPic_url(item.getPic_url());
				taskItem.setDisplay_order1(item.getDisplay_order1());
				taskItem.setDisplay_order2(item.getDisplay_order2());
				taskItem.setLabel_status(Constants.LABEL_TASK_STATUS_NOT_FINISHED);
				taskItem.setLabel_info(item.getLabel_info());
				batchDcmList.add(taskItem);
			}
			labelDcmTaskItemDao.addBatchLabelTaskItem(batchDcmList);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	

	private void dealDataSetLabelTask(DataSet dataSet,LabelTask labelTask) throws LabelSystemException {
		labelTask.setRelate_task_name(dataSet.getTask_name());
		if(dataSet.getDataset_type() == Constants.DATASET_TYPE_VIDEO) {
			labelTask.setTask_type(Constants.LABEL_TASK_TYPE_VIDEO);
		}else {
			labelTask.setTask_type(Constants.LABEL_TASK_TYPE_ORIGIN);
		}
		try {
			//拷贝一次记录，文件不再动
			List<LabelTaskItem> list = labelTaskItemDao.queryLabelTaskItemByLabelTaskId(TokenManager.getUserTablePos(dataSet.getUser_id(), UserConstants.LABEL_TASK_SINGLE_TABLE),dataSet.getId());
			copyLabelTaskItem(dataSet.getUser_id(),labelTask, list);
			//addBatchItemToDb(dataSet.getUser_id(), batchList);
			//labelTaskItemDao.addBatchLabelTaskItem(batchDcmList);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void copyLabelTaskItem(int userId,LabelTask labelTask, List<LabelTaskItem> list) {
		labelTask.setTotal_picture(list.size());
		List<LabelTaskItem> batchList = new ArrayList<>();
		for(LabelTaskItem item : list) {
			LabelTaskItem taskItem = new LabelTaskItem();
			taskItem.setId(UUID.randomUUID().toString().replaceAll("-",""));
			taskItem.setItem_add_time(TimeUtil.getCurrentTimeStr());
			taskItem.setLabel_task_id(labelTask.getId());
			taskItem.setPic_image_field(item.getPic_image_field());
			taskItem.setPic_object_name(item.getPic_object_name());
			taskItem.setPic_url(item.getPic_url());
			taskItem.setDisplay_order1(item.getDisplay_order1());
			taskItem.setDisplay_order2(item.getDisplay_order2());
			taskItem.setLabel_status(Constants.LABEL_TASK_STATUS_NOT_FINISHED);
			taskItem.setLabel_info(item.getLabel_info());
			batchList.add(taskItem);
			
			if(batchList.size() == 2000) {
				Map<String,Object> paramMap = new HashMap<>();
				paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE));
				paramMap.put("list", batchList);
				labelTaskItemDao.addBatchLabelTaskItemMap(paramMap);
				batchList.clear();
			}
		}
		if(batchList.size() > 0) {
			Map<String,Object> paramMap = new HashMap<>();
			paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE));
			paramMap.put("list", batchList);
			labelTaskItemDao.addBatchLabelTaskItemMap(paramMap);
			batchList.clear();
		}
		
	}

	public int updateLabelTask(String token,String labelTaskId,String taskLabelTypeInfo) {

		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", labelTaskId);
		paramMap.put("task_label_type_info", taskLabelTypeInfo);

		return labelTaskDao.updateLabelTask(paramMap);

	}


	public int addLabelTask(String token,LabelTaskBody body) throws LabelSystemException {
		//		if(Strings.isNullOrEmpty(token)) {
		//			token = "JWT d7f52e6fbc154ac285cece3e2704468a";
		//		}
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));

		try {
			if(body.getTaskLabelTypeInfo() != null && !body.getTaskLabelTypeInfo().isEmpty()) {
				LabelPropertyService.checkLabelJson(body.getTaskLabelTypeInfo());
			}else {
				body.setTaskLabelTypeInfo(null);
			}
		}catch (Exception e) {
			logger.info(e.getMessage());
			body.setTaskLabelTypeInfo(null);
		}
		
		LabelTask labelTask = new LabelTask();
		labelTask.setId(UUID.randomUUID().toString().replaceAll("-",""));
		labelTask.setRelate_task_id(body.getRelateTaskId());
		labelTask.setTask_name(body.getTaskName());
		labelTask.setUser_id(userId);
		labelTask.setTask_add_time(TimeUtil.getCurrentTimeStr());
		labelTask.setTask_label_type_info(body.getTaskLabelTypeInfo());
		if(body.getAssign_user_id() == 0) {
			labelTask.setAssign_user_id(userId);
		}else {
			labelTask.setAssign_user_id(body.getAssign_user_id());
		}
		labelTask.setTask_flow_type(body.getTask_flow_type());
		labelTask.setRelate_other_label_task(body.getRelate_other_label_task());
		labelTask.setTask_status(0);

		if(body.getTaskType() == Constants.LABEL_TASK_TYPE_ORIGIN) {

			String taskId = body.getRelateTaskId();
			DataSet dataSet = dataSetDao.queryDataSetById(taskId);
			if(dataSet == null) {
				throw new LabelSystemException("关联的任务ID错误。");
			}
			if(dataSet.getTotal() == 0) {
				throw new LabelSystemException("数据集中图片数量为0，不能创建人工标注。");
			}
			labelTask.setRelate_task_name(dataSet.getTask_name());
			if(dataSet.getDataset_type() == Constants.DATASET_TYPE_DCM) {
				dealDataSetLabelDcmTask(dataSet, labelTask);
			}else {
				dealDataSetLabelTask(dataSet, labelTask);
			}
		}
		else {
			if(!Strings.isNullOrEmpty(body.getRelateTaskId())) {
				labelTask.setTask_type(Constants.LABEL_TASK_TYPE_AUTO);
				PrePredictTask predictTask = prePredictTaskDao.queryPrePredictTaskById(body.getRelateTaskId());
				if(predictTask == null) {
					throw new LabelSystemException("关联的任务ID错误。");
				}
				List<PrePredictTaskResult> taskResultList = prePredictTaskResultDao.selectByPrePredictTaskId(TokenManager.getUserTablePos(userId, UserConstants.PREDICT_SINGLE_TABLE),body.getRelateTaskId());
				labelTask.setTotal_picture(taskResultList.size());
				labelTask.setRelate_task_name(predictTask.getTask_name());
				List<LabelTaskItem> batchList = new ArrayList<>();
				//拷贝预检完成到labeltaskitem中
				for(PrePredictTaskResult preTaskResult : taskResultList) {
					LabelTaskItem taskItem = new LabelTaskItem();
					taskItem.setId(UUID.randomUUID().toString().replaceAll("-",""));
					taskItem.setItem_add_time(TimeUtil.getCurrentTimeStr());
					taskItem.setLabel_info(preTaskResult.getLabel_info());
					taskItem.setLabel_task_id(labelTask.getId());
					taskItem.setPic_image_field(preTaskResult.getPic_image_field());
					taskItem.setPic_object_name(preTaskResult.getPic_object_name());
					taskItem.setPic_url(preTaskResult.getPic_url());
					taskItem.setLabel_status(Constants.LABEL_TASK_STATUS_NOT_FINISHED);
					batchList.add(taskItem);
					if(batchList.size() == 300) {
						Map<String,Object> paramMap = new HashMap<>();
						paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE));
						paramMap.put("list", batchList);
						labelTaskItemDao.addBatchLabelTaskItemMap(paramMap);
						batchList.clear();
					}
				}
				if(batchList.size() > 0) {
					Map<String,Object> paramMap = new HashMap<>();
					paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE));
					paramMap.put("list", batchList);
					labelTaskItemDao.addBatchLabelTaskItemMap(paramMap);
					batchList.clear();
				}
				//addBatchItemToDb(userId, batchList);
				//labelTaskItemDao.addBatchLabelTaskItem(batchList);
			}
		}

		labelTaskDao.addLabelTask(labelTask);
		
		//todo delete
		if(labelTask.getTask_flow_type() == 3) {
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					miaod.doMIAOD();
				}
			}).start();;
		}
		return 1;
	}


//	public List<LabelTaskItem> queryLabelTaskItemByLabelTaskId(String token,String labelTaskId){
//
//		LabelTask labelTask = labelTaskDao.queryLabelTaskById(labelTaskId);
//		int user_id = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
//		if(labelTask != null) {
//			if(labelTask.getTask_type() == Constants.LABEL_TASK_TYPE_ORIGIN_DCM) {
//				return labelDcmTaskItemDao.queryLabelTaskItemByLabelTaskId(labelTaskId);
//			}else {
//				return labelTaskItemDao.queryLabelTaskItemByLabelTaskId(TokenManager.getUserTablePos(user_id, UserConstants.LABEL_TASK_SINGLE_TABLE),labelTaskId);
//			}
//		}
//
//		return new ArrayList<>();
//	}


	public List<LabelTaskItem> queryLabelTaskItemByLabelTaskIdAndPicImage(String token, String labelTaskId,String picImageListStr){

		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		

		LabelTask labelTask = labelTaskDao.queryLabelTaskById(labelTaskId);

		List<String> picImageList = gson.fromJson(picImageListStr, new TypeToken<List<String>>() {
			private static final long serialVersionUID = 1L;}.getType());

		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("label_task_id", labelTaskId);
		paramMap.put("picList", picImageList);
		paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE));

		if(labelTask != null) {
			if(labelTask.getTask_type() == Constants.LABEL_TASK_TYPE_ORIGIN_DCM) {
				return labelDcmTaskItemDao.queryLabelTaskItemByLabelTaskIdAndPicImage(paramMap);
			}else {
				return labelTaskItemDao.queryLabelTaskItemByLabelTaskIdAndPicImage(paramMap);
			}
		}

		return new ArrayList<>();
	}


	public PageResult queryLabelTaskItemPageByLabelTaskId(String labelTaskId,int currPage, int pageSize, int orderType, int findLast,String token){
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		PageResult pageResult = new PageResult();

		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		paramMap.put("label_task_id", labelTaskId);
		paramMap.put("orderType", orderType);
		paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE));
		
		LabelTask labelTask = labelTaskDao.queryLabelTaskById(labelTaskId);

		if(labelTask != null) {

			if(findLast == Constants.QUERY_ITEM_PAGE_FIND_LAST) {
				logger.info("findLast order.");
				if(labelTask.getTask_type() == Constants.LABEL_TASK_TYPE_ORIGIN_DCM) {
					int totalCount = labelDcmTaskItemDao.queryLabelTaskItemPageCountByLabelTaskId(paramMap);
					List<LabelTaskItem> result = new ArrayList<>();
					
					int count = 0;
					while(count < totalCount) {
						
						result = labelDcmTaskItemDao.queryLabelTaskItemPageByLabelTaskId(paramMap);
						boolean isExistNotFinished = false;

						for(LabelTaskItem item :result) {
							if(item.getLabel_status() == Constants.LABEL_TASK_STATUS_NOT_FINISHED) {
								isExistNotFinished = true;
								break;
							}
						}
						if(isExistNotFinished) {
							break;
						}else {
							currPage ++;
							count += result.size();
							paramMap.put("currPage", currPage * pageSize);
						}
					}
					pageResult.setTotal(totalCount);
					pageResult.setData(result);
					pageResult.setCurrent(currPage);
				}else {
					int totalCount = labelTaskItemDao.queryLabelTaskItemPageCountByLabelTaskId(paramMap);
					List<LabelTaskItem> result = new ArrayList<>();
					
					int count = 0;
					while(count < totalCount) {
						
						result = labelTaskItemDao.queryLabelTaskItemPageByLabelTaskId(paramMap);
						boolean isExistNotFinished = false;

						for(LabelTaskItem item :result) {
							if(item.getLabel_status() == Constants.LABEL_TASK_STATUS_NOT_FINISHED) {
								isExistNotFinished = true;
								break;
							}
						}
						if(isExistNotFinished) {
							break;
						}else {
							currPage ++;
							count += result.size();
							paramMap.put("currPage", currPage * pageSize);
						}
					}
					pageResult.setTotal(totalCount);
					pageResult.setData(result);
					pageResult.setCurrent(currPage);
				}

			}
			
			else {
				if(findLast == Constants.QUERY_ITEM_PAGE_MIAOD) {
					paramMap.put("orderType", 2);
				}
				
				if(labelTask.getTask_type() == Constants.LABEL_TASK_TYPE_ORIGIN_DCM) {
					int totalCount = labelDcmTaskItemDao.queryLabelTaskItemPageCountByLabelTaskId(paramMap);
					List<LabelTaskItem> result = labelDcmTaskItemDao.queryLabelTaskItemPageByLabelTaskId(paramMap);
					pageResult.setTotal(totalCount);
					pageResult.setData(result);
				}else {
					List<LabelTaskItem> result = labelTaskItemDao.queryLabelTaskItemPageByLabelTaskId(paramMap);
					int totalCount = labelTaskItemDao.queryLabelTaskItemPageCountByLabelTaskId(paramMap);
					pageResult.setTotal(totalCount);
					pageResult.setData(result);
				}
				pageResult.setCurrent(currPage);
			}
		}

		return pageResult;
	}


	public int updateLabelTaskItem(LabelTaskItem updateBody,String token) {

		Map<String,Object> paramMap = new HashMap<>();
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		LabelTaskItem oldLabelTaskItem = labelTaskItemDao.queryLabelTaskItemById(TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE),updateBody.getId());

		if(oldLabelTaskItem != null) {
			if(labelEquals(oldLabelTaskItem.getLabel_info(),updateBody.getLabel_info())){
				if(updateBody.getLabel_status() == oldLabelTaskItem.getLabel_status()) {
					return 0;
				}
			}
		}
		//logger.info("udpate item:" + JsonUtil.toJson(updateBody));
		String time = TimeUtil.getCurrentTimeStr();
		updateBody.setItem_add_time(time);

		paramMap.put("id", updateBody.getId());
		paramMap.put("label_info", updateBody.getLabel_info());
		
		paramMap.put("label_status", getLabelStatus(updateBody));
		
		
		if(updateBody.getDisplay_order2() == LABEL_UPDATE_FLAG) {//如果是标注更新，则需要将审核状态修改为未审核
		    paramMap.put("verify_status", 0);//修改成未审核
		}else {
			paramMap.put("verify_status", updateBody.getVerify_status());
		}
		paramMap.put("pic_object_name", updateBody.getPic_object_name());
		paramMap.put("item_add_time", updateBody.getItem_add_time());
		
		paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE));

		int re = labelTaskItemDao.updateLabelTaskItem(paramMap);

		
		
		LabelTask labelTask = labelTaskDao.queryLabelTaskById(oldLabelTaskItem.getLabel_task_id());
		Map<String,String> extend = new HashMap<>();
		extend.put("label_user_id", String.valueOf(labelTask.getUser_id()));	
		logUpdateLabelTaskItem(oldLabelTaskItem,updateBody,userId,JsonUtil.toJson(extend));

		return re;
	}
	
	

	//只有当新旧比较时，仅且多了一个不合格的属性时，才由标注完成转换成标注未完成。
	private int getLabelStatus(LabelTaskItem updateBody) {
		if(updateBody.getDisplay_order2() != VERIFY_UPDATE_FLAG) {//审核状态更新，需要将display_order2状态设置100
			return updateBody.getLabel_status();
		}
		List<Map<String,Object>> newList = JsonUtil.getLabelList(updateBody.getLabel_info());
		
		for(Map<String,Object> label : newList) {
			if(label.get("other") != null ) {
				Map<String,Object> newOther = (Map<String,Object>)label.get("other");
				Map<String,Object> newregion_attributes = (Map<String,Object>)newOther.get(LogConstants.REGION_ATTRIBUTES);
				if(newregion_attributes != null && newregion_attributes.get(LogConstants.VERIFY_FIELD) != null) {
					if(newregion_attributes.get(LogConstants.VERIFY_FIELD).toString().equals(NOT_VALIDE_VALUE)) {
						logger.info("only exist verify == 1");
						return Constants.LABEL_TASK_STATUS_NOT_FINISHED;
					}
				}
			}
		}
		return updateBody.getLabel_status();
	}
	

	private void logUpdateLabelTaskItem(LabelTaskItem oldLabelTaskItem, LabelTaskItem updateBody,int user_id,String extend1) {
		LogInfo logInfo = new LogInfo();
		logInfo.setOper_type(LogConstants.LOG_UPATE);
		logInfo.setUser_id(user_id);
		logInfo.setOper_name("通用标注");
		logInfo.setOper_id(LogConstants.LOG_NORMAL_UPDATE_LABEL_ITEM);
		logInfo.setOper_time_start(updateBody.getItem_add_time());
		logInfo.setOper_time_end(updateBody.getItem_add_time());
		logInfo.setOper_json_content_new(updateBody.getLabel_info());
		logInfo.setOper_json_content_old(oldLabelTaskItem.getLabel_info());
		logInfo.setRecord_id(oldLabelTaskItem.getId());
		logInfo.setExtend1(extend1);
		logInfo.setExtend2(oldLabelTaskItem.getPic_image_field());
		logService.addLogInfo(logInfo);
	}


	public int deleteLabelTaskById(String token, String labelTaskId) {
		
		LabelTask labelTask = labelTaskDao.queryLabelTaskById(labelTaskId);
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		if(labelTask.getTask_type() == Constants.LABEL_TASK_TYPE_ORIGIN_DCM) {
			//数据库删除
			labelDcmTaskItemDao.deleteLabelTaskById(labelTaskId);
		}else {
			//数据库删除
			labelTaskItemDao.deleteLabelTaskById(TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE),labelTaskId);
		}
		return labelTaskDao.deleteLabelTaskById(labelTaskId);
	}

	public List<DisplayLabelTask> queryLabelTaskByRelatedDataSetId(String token, String dataSetId){
		List<DisplayLabelTask> re = new ArrayList<>();
		List<LabelTask> taskList = labelTaskDao.queryLabelTaskByDataSetId(dataSetId);

		Map<Integer,String> userIdForName = userService.getAllUser();

		for(LabelTask task : taskList) {
			DisplayLabelTask displayLabelTask = new DisplayLabelTask();
			displayLabelTask.setId(task.getId());
			displayLabelTask.setTask_name(task.getTask_name());
			displayLabelTask.setTask_flow_type(task.getTask_flow_type());
			if(task.getAssign_user_id() == 0) {
				displayLabelTask.setAssign_user(userIdForName.get(task.getUser_id()));
			}else {
				displayLabelTask.setAssign_user(userIdForName.get(task.getAssign_user_id()));
			}
			re.add(displayLabelTask);
		}
		return re;
	}


	/**
	 * 只保存到数据库中，不保存到xml中，导出的话，要修改，直接从数据库中取出
	 * @param id
	 * @param labelInfo
	 * @param labelStatus
	 * @param token
	 * @return
	 */
	public int updateLabelDcmTaskItem(LabelTaskItem updateBody,String token) {
		logger.info("updateBody.getLabel_info()=" +updateBody.getLabel_info());
		Map<String,Object> paramMap = new HashMap<>();

		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		LabelTaskItem oldLabelTaskItem = labelDcmTaskItemDao.queryLabelTaskItemById(updateBody.getId());
		if(oldLabelTaskItem != null) {
			if(labelEquals(oldLabelTaskItem.getLabel_info(),updateBody.getLabel_info())){
				return 0;
			}
		}
		//logger.info("oldLabelTaskItem.getLabel_info()=" + oldLabelTaskItem.getLabel_info());
		//logger.info("updateBody.getLabel_info()=" +updateBody.getLabel_info());
		String time = TimeUtil.getCurrentTimeStr();
		updateBody.setItem_add_time(time);
		
		paramMap.put("id", updateBody.getId());
		paramMap.put("label_info", updateBody.getLabel_info());
		paramMap.put("label_status", updateBody.getLabel_status());
		paramMap.put("verify_status", updateBody.getVerify_status());
		paramMap.put("pic_object_name", updateBody.getPic_object_name());
		paramMap.put("item_add_time", updateBody.getItem_add_time());

		int re = labelDcmTaskItemDao.updateLabelTaskItem(paramMap);

		logUpdateLabelTaskItem(oldLabelTaskItem,updateBody,userId,null);
		return re;
	}

	private boolean labelEquals(String old_label_info, String new_label_info) {
		if(Strings.isNullOrEmpty(old_label_info) && Strings.isNullOrEmpty(new_label_info)) {
			return true;
		}
		if(Strings.isNullOrEmpty(old_label_info) && "[]".equals(new_label_info)) {
			return true;
		}
		if(!Strings.isNullOrEmpty(old_label_info) && !Strings.isNullOrEmpty(new_label_info)) {
			if(old_label_info.equals(new_label_info)) {
				return true;
			}
		}
		return false;
	}

	public String upateAutoLabelInfo(Map<String, Object> msgMap) {
		
		String filename = (String)msgMap.get("filename");
		String itemId = filename.substring(0,filename.length() - 4);
		
		int userId = -1;
		if(msgMap.get("userId") != null) {
			userId = Integer.parseInt(msgMap.get("userId").toString());
		}
		//这个地方的标注需要修改
		LabelTaskItem item = labelTaskItemDao.queryLabelTaskItemById(TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE),itemId);
		if(item == null) {
			logger.info("the id is null.id=" + itemId);
			return itemId;
		}
		if(item.getLabel_status() == Constants.LABEL_TASK_STATUS_NOT_FINISHED || isEmpty(item.getLabel_info())) {
			Object labelInfo = msgMap.get("label_info");
			Map<String,Object> paramMap = new HashMap<>();
			paramMap.put("id", itemId);
			paramMap.put("label_info", gson.toJson(labelInfo));
			paramMap.put("label_status", Constants.LABEL_TASK_STATUS_FINISHED);
			paramMap.put("item_add_time", TimeUtil.getCurrentTimeStr());
			paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE));
			labelTaskItemDao.updateLabelTaskItem(paramMap);
		}else {
			logger.info("the id status is finished or label_info not empty. item=" + gson.toJson(item));
		}
		return item.getLabel_task_id();
	}

	private boolean isEmpty(String label_info) {
		if(Strings.isNullOrEmpty(label_info)) {
			return true;
		}
		if(label_info.equals("[]")) {
			return true;
		}
		return false;
	}

	public int updateLabelTaskItemStatus(LabelTaskItem updateBody, String token) {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		String time = TimeUtil.getCurrentTimeStr();
		updateBody.setItem_add_time(time);
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", updateBody.getId());
		paramMap.put("label_status", updateBody.getLabel_status());
		paramMap.put("item_add_time", updateBody.getItem_add_time());
		paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE));
		return labelTaskItemDao.updateLabelTaskItem(paramMap);
		
	}

	public int updateLabelTaskStatus(String token, String labelTaskId,int verifyUserId, int taskStatus) {
		
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", labelTaskId);
		paramMap.put("task_status", taskStatus);
		if(verifyUserId != -1) {
			paramMap.put("verify_user_id", verifyUserId);
			//如果转换为审核状态，给每个框增加一个是否合格的属性
			if(verifyUserId > 0) {
				LabelTask labelTask = labelTaskDao.queryLabelTaskById(labelTaskId);
				Map<String,Object> map = JsonUtil.getMap(labelTask.getTask_label_type_info());
				if(map.isEmpty()) {
					putIdAndDefaultType(map);
				}
				if(!map.containsKey("verify")) {
					Map<String,Object> verify = new HashMap<>();
					map.put("verify", verify);
					verify.put("type", "dropdown");
					verify.put("description", "用于审核状态是否此框是否合格。");
					Map<String,Object> optionsMap = new HashMap<>();
					verify.put("options", optionsMap);
					optionsMap.put(LogConstants.VERIFY_FIELD_RESULT_VALID_0, "合格");
					optionsMap.put(LogConstants.VERIFY_FIELD_RESULT_NOTVALID_1, "大小不合格");
					optionsMap.put(LogConstants.VERIFY_FIELD_RESULT_NOTVALID_2, "颜色不合格");
					optionsMap.put(LogConstants.VERIFY_FIELD_RESULT_NOTVALID_3, "其它不合格");
					paramMap.put("task_label_type_info", JsonUtil.toJson(map));
				}
			}
		}
		logger.info("update label task status:" + paramMap.toString());
		return labelTaskDao.updateLabelTaskStatus(paramMap);
	}

	
	private void putIdAndDefaultType(Map<String,Object> map) {
		
		Map<String,Object> id = new HashMap<>();
		map.put("id", id);
		id.put("type", "text");
		id.put("description", "标注框id，在一张图片中唯一，数字。");
		id.put("default_value", "");

		Map<String,Object> defaultType = new HashMap<>();
		map.put("type", defaultType);
		defaultType.put("type", "dropdown");
		defaultType.put("description", "缺省标注类型，car或者person。");
		Map<String,Object> optionsMap = new HashMap<>();
		defaultType.put("options", optionsMap);
		optionsMap.put("car", "car");
		optionsMap.put("person", "person");
		optionsMap.put("non-motor", "non-motor");
		
	}
	
	public int updateLabelItemVerifyStatus(LabelTaskItem updateBody, String token) {
		String time = TimeUtil.getCurrentTimeStr();
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		updateBody.setItem_add_time(time);
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", updateBody.getId());
		paramMap.put("verify_status", updateBody.getVerify_status());
		paramMap.put("item_add_time", updateBody.getItem_add_time());
		paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE));
		return labelTaskItemDao.updateLabelTaskItem(paramMap);
	}

	public void deleteLabel(String labelTaskId, Integer startId, Integer endId, String one_name, String token) {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		
		List<LabelTaskItem> taskItem = labelTaskItemDao.queryLabelTaskItemByLabelTaskId(TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE),labelTaskId);
		List<LabelTaskItem> deleteItemList = new ArrayList<>();
		for(int i = startId - 1; i< taskItem.size() && i < endId; i++) {
			deleteItemList.add(taskItem.get(i));
		}
		for(LabelTaskItem deleteItem : deleteItemList) {
			Map<String,Object> paramMap = new HashMap<>();
			paramMap.put("id", deleteItem.getId());
			
			paramMap.put("label_info", JsonUtil.toJson(new ArrayList<>()));
			paramMap.put("label_status", Constants.LABEL_TASK_STATUS_NOT_FINISHED);
			paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE));
			paramMap.put("item_add_time", TimeUtil.getCurrentTimeStr());
			labelTaskItemDao.updateLabelTaskItem(paramMap);
		}
		
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

	public void addLabelTaskFromObs(String token, LabelTaskBody body, String obsPath) {
		
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		logger.info("add meidcal task start." + body.getTaskName() + " obspath=" + obsPath +" userId=" + userId);
		LabelTask labelTask = new LabelTask();
		labelTask.setId(UUID.randomUUID().toString().replaceAll("-",""));
		labelTask.setRelate_task_id(body.getRelateTaskId());
		labelTask.setTask_name(body.getTaskName());
		labelTask.setUser_id(userId);
		labelTask.setTask_add_time(TimeUtil.getCurrentTimeStr());
		labelTask.setTask_label_type_info(body.getTaskLabelTypeInfo());
		if(body.getAssign_user_id() == 0) {
			labelTask.setAssign_user_id(userId);
		}else {
			labelTask.setAssign_user_id(body.getAssign_user_id());
		}
		labelTask.setTask_flow_type(body.getTask_flow_type());
		labelTask.setRelate_task_name(body.getRelateTaskName());
		labelTask.setRelate_other_label_task(body.getRelate_other_label_task());
		labelTask.setTask_status(0);
		labelTask.setTask_type(body.getTaskType());
		
		
		List<String> allFile = fileService.listAllFile(obsPath);
		
		
		Map<String,Integer> groupNumberMap = new HashMap<>();
		Map<String,String> groupKeyMap = new HashMap<>();
		Map<String,String> widthHeightMap = new HashMap<>();
		for(String path :allFile) {
			if(!(path.toLowerCase().endsWith("ima") || path.toLowerCase().endsWith("dcm"))) {
				continue;
			}
			String relativePath = "/dcm" + path;
			Map<String,String> info = getDcmInfo(relativePath);
			logger.info("info=" + info.toString());
			String groupKey = info.get("groupKey");
			if(groupNumberMap.containsKey(groupKey)) {
				groupNumberMap.put(groupKey, groupNumberMap.get(groupKey) +1);
			}else {
				groupNumberMap.put(groupKey, 1);
			}
			widthHeightMap.put(relativePath, info.get("widthHeight"));
			groupKeyMap.put(relativePath, groupKey);
		}
		logger.info("widthHeightMap=" + widthHeightMap.toString());
		if(groupKeyMap.size() > 0) {//DCM类型
			labelTask.setTask_type(Constants.LABEL_TASK_TYPE_ORIGIN_DCM);
		}else {
			labelTask.setTask_type(Constants.LABEL_TASK_TYPE_ORIGIN);
		}
		
		List<LabelTaskItem> batchList = new ArrayList<>();
		
		List<LabelTaskItem> dcmBatchList = new ArrayList<>();
		
		int count = 0;
		for(String path :allFile) {
			if(path.endsWith("/")) {
				logger.info("this is a directory:" + path);
				continue;
			}
			if(path.toLowerCase().endsWith(".svs") || path.toLowerCase().endsWith(".tif")) {
				continue;
			}
			count++;
			LabelTaskItem taskItem = new LabelTaskItem();
			taskItem.setId(UUID.randomUUID().toString().replaceAll("-",""));
			taskItem.setItem_add_time(TimeUtil.getCurrentTimeStr());
			taskItem.setLabel_task_id(labelTask.getId());
			if(path.toLowerCase().endsWith("ima") || path.toLowerCase().endsWith("dcm")) {
				taskItem.setPic_image_field("/dcm" + path);
			}else {
				taskItem.setPic_image_field("/minio" + path);
			}
			
			taskItem.setLabel_status(Constants.LABEL_TASK_STATUS_NOT_FINISHED);
			String pic_object_name = widthHeightMap.get(taskItem.getPic_image_field());
			taskItem.setPic_object_name(pic_object_name);
			taskItem.setPic_url(groupKeyMap.get(taskItem.getPic_image_field()));
			taskItem.setVerify_status(0);
			if(groupKeyMap.get(taskItem.getPic_image_field()) != null && groupNumberMap.get(groupKeyMap.get(taskItem.getPic_image_field())) != null) {
				taskItem.setDisplay_order1(groupNumberMap.get(groupKeyMap.get(taskItem.getPic_image_field())));
				dcmBatchList.add(taskItem);
			}else {
				batchList.add(taskItem);
			}
			
			if(batchList.size() == 300) {
				Map<String,Object> paramMap = new HashMap<>();
				paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE));
				paramMap.put("list", batchList);
				labelTaskItemDao.addBatchLabelTaskItemMap(paramMap);
				batchList.clear();
			}
			if(dcmBatchList.size() == 300) {
				labelDcmTaskItemDao.addBatchLabelTaskItem(dcmBatchList);
				dcmBatchList.clear();
			}
			
		}
		if(batchList.size() > 0) {
			Map<String,Object> paramMap = new HashMap<>();
			paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE));
			paramMap.put("list", batchList);
			labelTaskItemDao.addBatchLabelTaskItemMap(paramMap);
			batchList.clear();
		}
		if(dcmBatchList.size() > 0) {
			labelDcmTaskItemDao.addBatchLabelTaskItem(dcmBatchList);
			dcmBatchList.clear();
		}
		
		labelTask.setTotal_picture(count);
		labelTaskDao.addLabelTask(labelTask);
		logger.info("add meidcal task end." + body.getTaskName() + " obspath=" + obsPath);
	}
	
	

	public PageResult queryLabelTaskPageForMedical(String token, String appid, Integer currPage, Integer pageSize) {
		
		//int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		//logger.info("add meidcal task start." + body.getTaskName() + " obspath=" + obsPath);
		PageResult pageResult = new PageResult();
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		List<DisplayLabelTask> result = new ArrayList<>();

		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		paramMap.put("user_id", userId);
		paramMap.put("relate_task_name", appid);
		List<LabelTask> labelTaskList = labelTaskDao.queryLabelTask(paramMap);

		int totalCount = labelTaskDao.queryLabelTaskCount(paramMap);

		pageResult.setTotal(totalCount);
		pageResult.setData(result);
		pageResult.setCurrent(currPage);

		if(totalCount > 0) {

			Map<Integer,String> userIdForName = userService.getAllUser();
			Map<String, Integer> labelCountMap = getLabelTaskStatus(userId,labelTaskList);

			for(LabelTask labelTask : labelTaskList) {
				DisplayLabelTask reTask = new DisplayLabelTask();
				reTask.setId(labelTask.getId());
				reTask.setTask_name(labelTask.getTask_name());
				reTask.setTask_add_time(labelTask.getTask_add_time());
				reTask.setRelate_task_name(labelTask.getRelate_task_name());
				reTask.setTask_type(labelTask.getTask_type());
				reTask.setTask_flow_type(labelTask.getTask_flow_type());
				setLabelStatus(labelCountMap, labelTask, reTask);
				if(userIdForName.containsKey(labelTask.getUser_id())) {
					reTask.setUser(userIdForName.get(labelTask.getUser_id()));
				}
				if(userIdForName.containsKey(labelTask.getVerify_user_id())) {
					reTask.setVerify_user(userIdForName.get(labelTask.getVerify_user_id()));
				}
				if(labelTask.getAssign_user_id() == 0) {
					reTask.setAssign_user(userIdForName.get(labelTask.getUser_id()));
				}else {
					reTask.setAssign_user(userIdForName.get(labelTask.getAssign_user_id()));
				}
				reTask.setTotal_label(labelTask.getTotal_label());
				
				result.add(reTask);
			}
		}

		return pageResult;
	}


}
