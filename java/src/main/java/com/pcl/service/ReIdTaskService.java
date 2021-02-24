package com.pcl.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.util.Strings;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.pcl.constant.Constants;
import com.pcl.constant.LogConstants;
import com.pcl.constant.UserConstants;
import com.pcl.dao.DataSetDao;
import com.pcl.dao.LabelTaskItemDao;
import com.pcl.dao.PrePredictTaskDao;
import com.pcl.dao.PrePredictTaskResultDao;
import com.pcl.dao.ReIDLabelTaskItemDao;
import com.pcl.dao.ReIDTaskDao;
import com.pcl.dao.ReIDTaskResultDao;
import com.pcl.dao.ReIDTaskShowResultDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.PageResult;
import com.pcl.pojo.Progress;
import com.pcl.pojo.display.DisplayReIDTask;
import com.pcl.pojo.display.DisplayReIDTaskShowResult;
import com.pcl.pojo.display.DisplayReIdTaskResult;
import com.pcl.pojo.mybatis.DataSet;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.pojo.mybatis.LogInfo;
import com.pcl.pojo.mybatis.PrePredictTask;
import com.pcl.pojo.mybatis.PrePredictTaskResult;
import com.pcl.pojo.mybatis.ReIDTask;
import com.pcl.pojo.mybatis.ReIDTaskResult;
import com.pcl.pojo.mybatis.ReIDTaskShowResult;
import com.pcl.service.schedule.ReIDSchedule;
import com.pcl.service.schedule.ReIDShowResultSchedule;
import com.pcl.service.schedule.ThreadSchedule;
import com.pcl.util.JsonUtil;
import com.pcl.util.LabelInfoUtil;
import com.pcl.util.ReIDUtil;
import com.pcl.util.TimeUtil;

@Service
public class ReIdTaskService {

	@Autowired
	private ReIDTaskDao reIDTaskDao;

	@Autowired
	private UserService userService;

	@Autowired
	private PrePredictTaskResultDao prePredictTaskResultDao;

	@Autowired
	private PrePredictTaskDao prePredictTaskDao;

	@Autowired
	private ReIDLabelTaskItemDao reIdLabelTaskItemDao;

	@Autowired
	private ReIDTaskResultDao reIDTaskResultDao;

	@Autowired
	private ReIDTaskShowResultDao reIDTaskShowResultDao;

	@Autowired
	private LabelTaskItemDao labelTaskItemDao;

	@Autowired
	private ReIDSchedule reIdSchedule;

	@Autowired
	private ReIDShowResultSchedule showResultSchedule;

	@Autowired
	private ObjectFileService fileService;

	@Autowired
	private DataSetDao dataSetDao;

	@Autowired
	private LogService logService;

	private ThreadPoolExecutor pool = new ThreadPoolExecutor(5,5,1000,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(1000));

	private Gson gson = new Gson();

	private Map<String,Long> timeMap = new HashMap<>();

	private static Logger logger = LoggerFactory.getLogger(ReIdTaskService.class);

	public void addReIdTask(String token, ReIDTask body) throws LabelSystemException {
		long start = System.currentTimeMillis();
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		body.setId(UUID.randomUUID().toString().replaceAll("-",""));
		body.setUser_id(userId);
		body.setTask_start_time(TimeUtil.getCurrentTimeStr());


		logger.info("srcPredictTaskId=" +  body.getSrc_predict_taskid());

		//从自动标注结果中拷贝标注数据
		int order = 0;
		String srcPredictTaskId = body.getSrc_predict_taskid();
		int srcPictureNumber = copyTaskItem(userId,body, srcPredictTaskId,order);
		body.setTotal_picture(srcPictureNumber);

		String destPredictTaskIds = body.getDest_predict_taskid();

		logger.info("destPredictTaskIds=" +  body.getDest_predict_taskid());

		if(!Strings.isEmpty(destPredictTaskIds)) {
			List<String> destPredictTaskIdList = gson.fromJson(destPredictTaskIds, new TypeToken<List<String>>() {
				private static final long serialVersionUID = 1L;}.getType());

			for(String destPredictTaskId : destPredictTaskIdList) {
				order ++;
				if(destPredictTaskId.equals(srcPredictTaskId)) {
					continue;
				}
				copyTaskItem(userId,body, destPredictTaskId, order);
			}
		}


		if(Constants.REID_TASK_TYPE_AUTO == body.getTask_type()) {

			//调用行人再识别算法，将已经标注好的框自动进行行人再识别处理
			if(body.getReid_auto_type() == 1) {
				body.setTask_status(Constants.REID_TASK_STATUS_AUTO_PROGRESSING);
				body.setTask_status_desc("自动分类算法处理中...");
				reIDTaskDao.addReIDTask(body);

				reIdSchedule.addTask(body);
			}else {
				body.setTask_status(Constants.REID_TASK_STATUS_AUTO_FINISHED);
				body.setTask_status_desc("新建成功.");
				reIDTaskDao.addReIDTask(body);
			}
		}else {
			body.setTask_status(Constants.REID_TASK_STATUS_PROGRESSING);
			body.setTask_status_desc("人工标注ReID");
			reIDTaskDao.addReIDTask(body);
		}
		logger.info("add task cost:" + (System.currentTimeMillis() - start));
	}

	private int copyTaskItem(int userId,ReIDTask body, String predictTaskId,int order) throws LabelSystemException {
		if(Constants.REID_TASK_TYPE_AUTO == body.getTask_type()) {
			body.setTask_status(Constants.REID_TASK_STATUS_AUTO_PROGRESSING);
			if(body.getReid_obj_type() == Constants.REID_TASK_OBJ_TYPE_PERSON) {
				body.setAlg_model_id(17);
			}else if(body.getReid_obj_type() == Constants.REID_TASK_OBJ_TYPE_CAR) {
				body.setAlg_model_id(9);
			}
			return copyPredictTaskItem(userId,body, predictTaskId,order);
		}else {
			body.setTask_status(Constants.TASK_STATUS_PROGRESSING);
			return copyDateSetTaskItem(userId,body, predictTaskId,order);
		}

	}

	private int copyPredictTaskItem(int userId,ReIDTask body, String predictTaskId,int order) throws LabelSystemException {
		PrePredictTask predictTask = prePredictTaskDao.queryPrePredictTaskById(predictTaskId);
		if(predictTask == null) {
			throw new LabelSystemException("关联的任务ID错误。");
		}

		List<PrePredictTaskResult> taskResultList = prePredictTaskResultDao.selectByPrePredictTaskId(TokenManager.getUserTablePos(predictTask.getUser_id(), UserConstants.PREDICT_SINGLE_TABLE),predictTaskId);

		List<LabelTaskItem> batchList = new ArrayList<>();
		//拷贝自动标注完成的标注信息到labeltaskitem中
		for(PrePredictTaskResult preTaskResult : taskResultList) {
			LabelTaskItem taskItem = new LabelTaskItem();
			taskItem.setId(UUID.randomUUID().toString().replaceAll("-",""));
			taskItem.setItem_add_time(TimeUtil.getCurrentTimeStr());
			taskItem.setLabel_info(preTaskResult.getLabel_info());
			taskItem.setLabel_task_id(body.getId());
			taskItem.setPic_image_field(preTaskResult.getPic_image_field());
			taskItem.setPic_object_name(preTaskResult.getPic_object_name());
			taskItem.setPic_url(predictTaskId);//此属性用作是行人再识别的源数据集id或者目标数据集id
			taskItem.setLabel_status(Constants.LABEL_TASK_STATUS_NOT_FINISHED);
			taskItem.setDisplay_order1(order);
			batchList.add(taskItem);
			if(batchList.size() == 2000) {
				Map<String,Object> paramMap = new HashMap<>();
				paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE));
				paramMap.put("list", batchList);
				reIdLabelTaskItemDao.addBatchLabelTaskItem(paramMap);
				batchList.clear();
			}
		}
		if(!batchList.isEmpty()) {
			Map<String,Object> paramMap = new HashMap<>();
			paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE));
			paramMap.put("list", batchList);
			reIdLabelTaskItemDao.addBatchLabelTaskItem(paramMap);
		}else {
			logger.info("empty item for predictTaskId.");
		}
		return taskResultList.size();
	}


	private int copyDateSetTaskItem(int userId, ReIDTask body, String dataSetId,int order) throws LabelSystemException {

		List<LabelTaskItem> list = labelTaskItemDao.queryLabelTaskItemByLabelTaskId(TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE),dataSetId);

		List<LabelTaskItem> batchList = new ArrayList<>();
		for(LabelTaskItem item : list) {
			LabelTaskItem taskItem = new LabelTaskItem();
			taskItem.setId(UUID.randomUUID().toString().replaceAll("-",""));
			taskItem.setItem_add_time(TimeUtil.getCurrentTimeStr());
			taskItem.setLabel_task_id(body.getId());
			taskItem.setPic_image_field(item.getPic_image_field());
			taskItem.setPic_object_name(item.getPic_object_name());
			taskItem.setPic_url(dataSetId);
			taskItem.setDisplay_order1(order);
			taskItem.setDisplay_order2(item.getDisplay_order2());
			taskItem.setLabel_status(Constants.LABEL_TASK_STATUS_NOT_FINISHED);
			batchList.add(taskItem);
			if(batchList.size() == 2000) {//2000条执行一次保存
				Map<String,Object> paramMap = new HashMap<>();
				paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE));
				paramMap.put("list", batchList);
				reIdLabelTaskItemDao.addBatchLabelTaskItem(paramMap);
				batchList.clear();
			}
		}
		if(!batchList.isEmpty()) {
			Map<String,Object> paramMap = new HashMap<>();
			paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE));
			paramMap.put("list", batchList);
			reIdLabelTaskItemDao.addBatchLabelTaskItem(paramMap);
		}else {
			logger.info("empty item for dataSetId.");
		}
		return list.size();
	}

	public PageResult queryReIDTask(String token, Integer currPage, Integer pageSize) {

		PageResult pageResult = new PageResult();
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		List<DisplayReIDTask> result = new ArrayList<>();

		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		paramMap.put("user_id", userId);
		List<ReIDTask> labelTaskList = reIDTaskDao.queryReIDTask(paramMap);

		int totalCount = reIDTaskDao.queryReIDTaskCount(paramMap);

		pageResult.setTotal(totalCount);
		pageResult.setData(result);
		pageResult.setCurrent(currPage);

		if(totalCount > 0) {

			HashSet<String> taskIdSet = new HashSet<>();
			for(ReIDTask reIdTask : labelTaskList) {
				taskIdSet.add(reIdTask.getSrc_predict_taskid());
				if(!Strings.isEmpty(reIdTask.getDest_predict_taskid())) {
					List<String> destPredictTaskIdList = gson.fromJson(reIdTask.getDest_predict_taskid(), new TypeToken<List<String>>() {
						private static final long serialVersionUID = 1L;}.getType());
					taskIdSet.addAll(destPredictTaskIdList);
				}
			}

			List<String> taskIdList = new ArrayList<>();
			taskIdList.addAll(taskIdSet);

			Map<String,String> autoTaskNameMap = getTaskName(taskIdList, Constants.REID_TASK_TYPE_AUTO,false);

			Map<String,String> dataSetTaskNameMap = getTaskName(taskIdList, Constants.REID_TASK_TYPE_MANUAL,false);

			Map<Integer,String> userIdForName = userService.getAllUser();
			for(ReIDTask reIdTask : labelTaskList) {
				DisplayReIDTask reIdDisplayTask = new DisplayReIDTask();
				reIdDisplayTask.setId(reIdTask.getId());
				reIdDisplayTask.setTask_name(reIdTask.getTask_name());
				reIdDisplayTask.setTask_start_time(reIdTask.getTask_start_time());

				if(reIdTask.getTask_type() == Constants.REID_TASK_TYPE_AUTO) {
					reIdDisplayTask.setRelate_task_name(autoTaskNameMap.get(reIdTask.getSrc_predict_taskid()));
				}else {
					reIdDisplayTask.setRelate_task_name(dataSetTaskNameMap.get(reIdTask.getSrc_predict_taskid()));
				}
				reIdDisplayTask.setReid_obj_type(reIdTask.getReid_obj_type());
				reIdDisplayTask.setTask_type(reIdTask.getTask_type());
				reIdDisplayTask.setTask_flow_type(reIdTask.getTask_flow_type());
				reIdDisplayTask.setTask_status(String.valueOf(reIdTask.getTask_status()));
				reIdDisplayTask.setTask_status_desc(reIdTask.getTask_status_desc());
				Date startDate = TimeUtil.getDateTimebyStr(reIdTask.getTask_start_time());

				reIdDisplayTask.setCostTime((int)((System.currentTimeMillis() - startDate.getTime())/1000));
				if(userIdForName.containsKey(reIdTask.getUser_id())) {
					reIdDisplayTask.setUser(userIdForName.get(reIdTask.getUser_id()));
				}
				if(reIdTask.getAssign_user_id() == 0) {
					reIdDisplayTask.setAssign_user(userIdForName.get(reIdTask.getUser_id()));
				}else {
					reIdDisplayTask.setAssign_user(userIdForName.get(reIdTask.getAssign_user_id()));
				}
				result.add(reIdDisplayTask);
			}
		}

		return pageResult;

	}

	public void deleteReIDTask(String token, String reIDTaskId) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				asyncDelete(token, reIDTaskId);
			}}).start();

	}

	private void asyncDelete(String token, String reIDTaskId) {
		logger.info("async delete start.");
		long start = System.currentTimeMillis();

		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		fileService.removeBucketName(reIDTaskId);
		reIDTaskDao.deleteReIDTask(reIDTaskId);
		reIdLabelTaskItemDao.deleteLabelTaskById(TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE),reIDTaskId);
		reIDTaskResultDao.deleteByReIDTaskId(reIDTaskId);
		reIDTaskShowResultDao.deleteByReIDTaskId(reIDTaskId);

		logger.info("async delete end,cost=" +(System.currentTimeMillis() - start) + " ms.");
	}

	public List<DisplayReIdTaskResult> queryReIdDestImagePage(String id, String pic_image_field, String labelId) {
		String srcLabelInfo = pic_image_field;
		if(!labelId.equals("-1")) {
			srcLabelInfo = ReIDUtil.getLabel(pic_image_field, labelId);
		}else {
			srcLabelInfo = pic_image_field.substring(pic_image_field.lastIndexOf("/") + 1);
		}
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", id);
		paramMap.put("src_image_info", srcLabelInfo);

		List<ReIDTaskResult> dbResultList = reIDTaskResultDao.queryReIDTaskResult(paramMap);

		List<DisplayReIdTaskResult> result = new ArrayList<>();

		for(ReIDTaskResult dbResult : dbResultList) {
			DisplayReIdTaskResult re = new DisplayReIdTaskResult();
			re.setTaskName(dbResult.getLabel_task_name());
			List<Map<String,String>> imageInfoList = gson.fromJson(dbResult.getRelated_info(), new TypeToken<List<Map<String,String>>>() {
				private static final long serialVersionUID = 1L;}.getType());
			re.setImageInfoList(imageInfoList);
			result.add(re);
		}

		return result;
	}


	public void updateReIDInfo(String token, String reTaskId, String imageListInfo, String reId) {
		Map<String,String> imageMapInfo =  gson.fromJson(imageListInfo, new TypeToken<Map<String,String>>() {
			private static final long serialVersionUID = 1L;}.getType());
		Map<String,Map<String,String>> convertMap = new HashMap<>();
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		Map<String,String> reIdNameMap = new HashMap<>();

		for(Entry<String,String> entry : imageMapInfo.entrySet()) {
			String key = entry.getKey();
			key = ReIDUtil.getLabelId(key);
			Map<String,String> map = convertMap.get(entry.getValue());
			if(map == null) {
				map = new HashMap<>();
				convertMap.put(entry.getValue(), map);
			}
			map.put(key,key);
			reIdNameMap.put("/minio/" +reTaskId + "/" +  entry.getKey(), entry.getValue());
		}


		for(Entry<String,Map<String,String>> entry : convertMap.entrySet()) {
			String labelItemId = entry.getKey();
			Map<String,String> map = entry.getValue();
			LabelTaskItem item = reIdLabelTaskItemDao.queryLabelTaskItemById(TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE),labelItemId);

			List<Map<String,Object>> labelList = ReIDUtil.getLabelList(item.getLabel_info());
			if(labelList.isEmpty()) {
				logger.info("jsonLabelInfo is null. jsonLabelInfo=" + item.getLabel_info());
				continue;
			}
			for(Map<String,Object> label : labelList) {
				Object idObj = label.get("id");
				if(idObj == null) {
					continue;
				}
				if(map.get(idObj.toString()) != null) {
					label.put("reId", reId);
				}
			}

			String newJsonLabelInfo = gson.toJson(labelList);

			item.setLabel_info(newJsonLabelInfo);

			updateReIDLabelTaskItem(item, token);
		}
		//往reidtaskshowresult表中插入一条记录，或者更新一条记录
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("label_task_id", reTaskId);
		paramMap.put("reid_name", reId);
		ReIDTaskShowResult re = reIDTaskShowResultDao.queryReIDShowTaskResult(paramMap);
		paramMap.put("related_info", gson.toJson(reIdNameMap));
		if(re != null) {
			reIDTaskShowResultDao.updateShowResult(paramMap);
		}else {
			ReIDTaskShowResult addRe = new ReIDTaskShowResult();
			addRe.setLabel_task_id(reTaskId);
			addRe.setReid_name(reId);
			addRe.setRelated_info(gson.toJson(reIdNameMap));
			reIDTaskShowResultDao.addBatchShowResultItem(Arrays.asList(addRe));
		}
	}

	private Map<String,String> getTaskName(List<String> taskIdList, int taskType,boolean append){
		Map<String,String> result = new HashMap<>();
		String srcId = taskIdList.get(0);
		if(Constants.REID_TASK_TYPE_AUTO == taskType) {
			List<PrePredictTask> taskList = prePredictTaskDao.queryPrePredictTaskByIdList(taskIdList);
			for(PrePredictTask task: taskList) {
				if(task.getId().equals(srcId)) {
					result.put(task.getId(), task.getTask_name() + getAppend(1, append));
				}else {
					result.put(task.getId(), task.getTask_name() + getAppend(2, append));
				}

			}
		}else {
			List<DataSet> dataSetList = dataSetDao.queryAllDataSetByIdList(taskIdList);
			for(DataSet dataSet: dataSetList) {
				if(dataSet.getId().equals(srcId)) {
					result.put(dataSet.getId(), dataSet.getTask_name() + getAppend(1, append));
				}else {
					result.put(dataSet.getId(), dataSet.getTask_name() + getAppend(2, append));
				}
			}
		}
		return result;
	}

	private String getAppend(int type,boolean isAppend) {
		if(!isAppend) {
			return "";
		}else {
			if(type == 1) {
				return "(源)";
			}else {
				return "(对照)";
			}
		}
	}





	public int updateReIDLabelTaskItem(LabelTaskItem updateBody,String token) {


		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		LabelTaskItem oldLabelTaskItem = reIdLabelTaskItemDao.queryLabelTaskItemById(TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE),updateBody.getId());

		String time = TimeUtil.getCurrentTimeStr();
		updateBody.setItem_add_time(time);

		Map<String,Object> paramMap = new HashMap<>();

		paramMap.put("id", updateBody.getId());
		paramMap.put("label_info", updateBody.getLabel_info());
		paramMap.put("label_status", updateBody.getLabel_status());
		paramMap.put("pic_object_name", updateBody.getPic_object_name());
		paramMap.put("item_add_time", updateBody.getItem_add_time());
		paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE));
		int re = reIdLabelTaskItemDao.updateLabelTaskItem(paramMap);

		pool.execute(()->{dealNewBox(updateBody, oldLabelTaskItem);});

		logUpdateLabelTaskItem(oldLabelTaskItem,updateBody,userId);

		return re;
	}

	
	class ReIdInfo{
		long time;
		String reIdName;
		String imgPath;
	}
	
	Map<String,List<ReIdInfo>> reIdInfoCache = new HashMap<>();
	
	private long getTime(String primitiveImageName) {
		String time = getStrTimeFromPrimitiveImageName(primitiveImageName);
		if(time != null) {
			try {
				return Long.parseLong(time);
			}catch (Exception e) {
				logger.info("time format error=" + time);
			}
		}
		return -1;
	}
	
	private boolean isTime(String primitiveImageName) {
		String time = getStrTimeFromPrimitiveImageName(primitiveImageName);
		if(time != null && time.length() == 6) {
			return true;
		}
		return false;
	}
	
	private String getStrTimeFromPrimitiveImageName(String primitiveImageName) {
		int index = primitiveImageName.lastIndexOf(".");
		if(index != -1) {
			primitiveImageName = primitiveImageName.substring(0,index);
		}
		index =  primitiveImageName.lastIndexOf("_");
		String time = primitiveImageName.substring(index + 1);
		if(time.length() < 6) {
			primitiveImageName = primitiveImageName.substring(0,index);
			index = primitiveImageName.lastIndexOf("_");
		}
		if(index != -1 && primitiveImageName.length() > (index + 1)) {
			return primitiveImageName.substring(index + 1);
		}
		return null;
	}
	
	private long getLongTimeFromCutImageName(String cutImageName) {
		int index = cutImageName.lastIndexOf("_");
		if(index != -1) {
			cutImageName = cutImageName.substring(0,index);
		}
		index =  cutImageName.lastIndexOf("_");
		String time = cutImageName.substring(index + 1);
		if(time.length() < 6) {
			cutImageName = cutImageName.substring(0,index);
			index = cutImageName.lastIndexOf("_");
		}
		if(index != -1 && cutImageName.length() > (index + 1)) {
			try {
				return Long.parseLong(cutImageName.substring(index + 1));
			}catch (Exception e) {
				logger.info("cutImageName time format error=" + cutImageName.substring(index + 1));
			}
		}
		return -1;
	}
	
	//输入ReID任务Id及原始图片名称，开始在已有结果中查找相同时间点
	public Map<String,String> queryNearReID(String token, String reTaskId,String primitiveImageName,long intervalTime){
		logger.info("reTaskId=" + reTaskId + " primitiveImageName=" + primitiveImageName + " intervalTime=" + intervalTime);
		Map<String,String> resultMap = new HashMap<>();
		long currentTime = getTime(primitiveImageName);
		if(currentTime == -1) {
			return resultMap;
		}
		List<ReIdInfo> reIdInfoList = reIdInfoCache.get(reTaskId);
		if(reIdInfoList == null) {
			loadCacheFromDb(reTaskId);
			reIdInfoList = reIdInfoCache.get(reTaskId);
		}
		if(!isTime(primitiveImageName)) {//如果不是时间格式，则按照1秒4张图片放大序号
			intervalTime = intervalTime * 4;
		}
		if(reIdInfoList != null) {
			//2分查找
			int left = 0;
			int right = reIdInfoList.size() - 1;
			
			while(left <= right) {
				int middle = (right + left) / 2;
				ReIdInfo info = reIdInfoList.get(middle);
				if(Math.abs(info.time - currentTime) < intervalTime) {
					addResultToMap(intervalTime, resultMap, currentTime, reIdInfoList, middle);
					break;
				}
				if(info.time < currentTime) {
					left = middle + 1;
				}else {
					right = middle - 1;
				}
			}
		}
		return resultMap;
	}

	private void addResultToMap(long intervalTime, Map<String, String> resultMap, long currentTime,
			List<ReIdInfo> reIdInfoList, int middle) {
		for(int j = middle; j < reIdInfoList.size(); j++) {
			ReIdInfo tmp = reIdInfoList.get(j);
			if(Math.abs(tmp.time - currentTime) < intervalTime) {
				resultMap.put(tmp.reIdName, tmp.imgPath);
			}else {
				break;
			}
		}
		
		for(int j = middle - 1; j > 0; j--) {
			ReIdInfo tmp = reIdInfoList.get(j);
			if(Math.abs(tmp.time - currentTime) < intervalTime) {
				resultMap.put(tmp.reIdName, tmp.imgPath);
			}else {
				break;
			}
		}
	}
	
	private void addToCache(String reTaskId,List<ReIdInfo> reidInfoList) {
		if(reIdInfoCache.size() > 10) {
			//如果超过10个，随机清除一个
			int random = new Random(10).nextInt();
			random = random % 10;
			Set<String> keySet = reIdInfoCache.keySet();
			List<String> keyList = new ArrayList<>();
			keyList.addAll(keySet);
			String removeKey = keyList.get(random);
			reIdInfoCache.remove(removeKey);
		}
		reIdInfoCache.put(reTaskId, reidInfoList);
	}
	
	private void insertRightLocation(ReIdInfo reIdInfo,List<ReIdInfo> reidInfoList) {
		boolean insert = false;
		for(int i = 0; i < reidInfoList.size(); i++) {
			if(reidInfoList.get(i).time > reIdInfo.time) {
				reidInfoList.add(i, reIdInfo);
				insert = true;
				break;
			}
		}
		if(!insert) {
			reidInfoList.add(reIdInfo);
		}
	}
	
	private void loadCacheFromDb(String reTaskId) {
		List<ReIDTaskShowResult> reList = reIDTaskShowResultDao.queryReIDShowTaskResultById(reTaskId);
		List<ReIdInfo> reidInfoList = new ArrayList<>();
		if(reList != null) {
			for(ReIDTaskShowResult result :reList) {
				String relatedInfo = result.getRelated_info();
				Map<String,Object> map = JsonUtil.getMap(relatedInfo);
				for(Entry<String,Object> entry : map.entrySet()) {
					ReIdInfo reIdInfo = new ReIdInfo();
					reIdInfo.reIdName = result.getReid_name();
					reIdInfo.imgPath = entry.getKey();
					reIdInfo.time = getLongTimeFromCutImageName(entry.getKey());
					if(reIdInfo.time == -1) {
						continue;
					}
					insertRightLocation(reIdInfo, reidInfoList);
				}
			}
		}
		addToCache(reTaskId, reidInfoList);
		
	}

	private void dealNewBox(LabelTaskItem updateBody, LabelTaskItem oldLabelTaskItem) {
		String reTaskId = oldLabelTaskItem.getLabel_task_id();
		List<Map<String,Object>> newLabelList = JsonUtil.getLabelList(updateBody.getLabel_info());
		for(Map<String,Object> label : newLabelList) {
			Object idObj = label.get("id");
			if(idObj == null || Strings.isBlank(idObj.toString())) {
				logger.info("it exists error, id is null.  reTaskId=" + oldLabelTaskItem.getLabel_task_id() + " item.id=" + updateBody.getId());
				continue; 
			}
			Object reIdObj = label.get(Constants.REID_KEY);
			if(reIdObj == null || Strings.isBlank(reIdObj.toString())) {
				continue;
			}
			//将新的reId标注图片保存到minio中
			String tmpPicName =ReIDUtil.getLabel(oldLabelTaskItem.getPic_image_field(), idObj.toString());
			String imgName = "/minio/" + reTaskId + "/" + tmpPicName;
			if(!fileService.isExistMinioFile(reTaskId, tmpPicName)) {
				logger.info("create new image to minio.imgName=" + imgName);
				reIdSchedule.createImage(tmpPicName,label,oldLabelTaskItem.getPic_image_field(),reTaskId);
				
				Map<String,Object> newParamMap = new HashMap<>();
				newParamMap.put("label_task_id", reTaskId);
				newParamMap.put("reid_name", reIdObj.toString());
				ReIDTaskShowResult newResult = reIDTaskShowResultDao.queryReIDShowTaskResult(newParamMap);

				if(newResult != null) {
					String relatedInfo = newResult.getRelated_info();
					Map<String,Object> map = JsonUtil.getMap(relatedInfo);
					map.put(imgName, updateBody.getId());
					newParamMap.put("related_info", JsonUtil.toJson(map));
					reIDTaskShowResultDao.updateShowResult(newParamMap);
				}else {
					Map<String,Object> map = new HashMap<>();
					map.put(imgName, updateBody.getId());
					ReIDTaskShowResult re = new ReIDTaskShowResult();
					re.setLabel_task_id(reTaskId);
					re.setReid_name(reIdObj.toString());
					re.setRelated_info(JsonUtil.toJson(map));
					reIDTaskShowResultDao.addBatchShowResultItem(Arrays.asList(re));
				}
				
				ReIdInfo reIdInfo = new ReIdInfo();
				reIdInfo.reIdName = reIdObj.toString();
				reIdInfo.imgPath = imgName;
				reIdInfo.time = getLongTimeFromCutImageName(imgName);
				if(reIdInfo.time != -1) {
					List<ReIdInfo> reidInfoList = reIdInfoCache.get(reTaskId);
					if(reidInfoList == null) {
						loadCacheFromDb(reTaskId);
						reidInfoList = reIdInfoCache.get(reTaskId);
					}
					insertRightLocation(reIdInfo, reidInfoList);
				}
			}
		}
		
		List<Map<String,Object>> oldLabelList = JsonUtil.getLabelList(oldLabelTaskItem.getLabel_info());
		Map<String,Integer> resultMap = LabelInfoUtil.getCompareResult(oldLabelList, newLabelList);
		//如果标注的框大小有变更，还需要更新扣图
		if(resultMap.containsKey("box")) {
			for(Map<String,Object> label : newLabelList) {
				Object idObj = label.get("id");
				if(idObj == null || Strings.isBlank(idObj.toString())) {
					logger.info("it exists error, id is null.  reTaskId=" + oldLabelTaskItem.getLabel_task_id() + " item.id=" + updateBody.getId());
					continue; 
				}
				
				String tmpPicName =ReIDUtil.getLabel(oldLabelTaskItem.getPic_image_field(), idObj.toString());
				String imgName = "/minio/" + reTaskId + "/" + tmpPicName;
				if(fileService.isExistMinioFile(reTaskId, tmpPicName)) {
					fileService.deleteFileFromMinio(reTaskId, tmpPicName);
					logger.info("box is change, so need delete minio kou tu picture.");
					logger.info("create new image to minio.imgName=" + imgName);
					reIdSchedule.createImage(tmpPicName,label,oldLabelTaskItem.getPic_image_field(),reTaskId);
					
				}
			}
		}
	}



	private void logUpdateLabelTaskItem(LabelTaskItem oldLabelTaskItem, LabelTaskItem updateBody,int user_id) {
		LogInfo logInfo = new LogInfo();
		logInfo.setOper_type(LogConstants.LOG_UPATE);
		logInfo.setUser_id(user_id);
		logInfo.setOper_name("ReID标注");
		logInfo.setOper_id(LogConstants.LOG_REID_UPDATE_LABEL_ITEM);
		logInfo.setOper_time_start(updateBody.getItem_add_time());
		logInfo.setOper_time_end(updateBody.getItem_add_time());
		logInfo.setOper_json_content_new(updateBody.getLabel_info());
		logInfo.setOper_json_content_old(oldLabelTaskItem.getLabel_info());
		logInfo.setRecord_id(oldLabelTaskItem.getId());
		logInfo.setExtend2(oldLabelTaskItem.getPic_image_field());
		logService.addLogInfo(logInfo);
	}


	public DisplayReIDTask queryReIDTaskById(String token, String id) throws LabelSystemException {

		ReIDTask reIdTask =  reIDTaskDao.queryReIDTaskById(id);
		if(reIdTask == null) {
			throw new LabelSystemException("ReId task is not exists. id=" + id);
		}

		DisplayReIDTask result = new DisplayReIDTask();

		result.setId(reIdTask.getId());
		result.setTask_name(reIdTask.getTask_name());
		result.setTask_status("");
		result.setTask_type(reIdTask.getTask_type());
		result.setTask_label_type_info(reIdTask.getTask_label_type_info());

		return result;

	}

	public void updateReIdTask(String token, String reIdTaskId, String taskLabelTypeInfo) {

		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", reIdTaskId);
		paramMap.put("task_label_type_info", taskLabelTypeInfo);

		reIDTaskDao.updateReIDTaskSelfDefineInfo(paramMap);

	}


	public Map<String,String> queryReIdTaskRelatedNameInfo(String reIdTaskId){
		ReIDTask reIdTask = reIDTaskDao.queryReIDTaskById(reIdTaskId);
		List<String> taskIdList = new ArrayList<>();
		taskIdList.add(reIdTask.getSrc_predict_taskid());

		if(!Strings.isEmpty(reIdTask.getDest_predict_taskid())) {
			List<String> destPredictTaskIdList = gson.fromJson(reIdTask.getDest_predict_taskid(), new TypeToken<List<String>>() {
				private static final long serialVersionUID = 1L;}.getType());
			taskIdList.addAll(destPredictTaskIdList);
		}

		Map<String,String> taskNameList = getTaskName(taskIdList, reIdTask.getTask_type(),true);
		return taskNameList;
	}


	public PageResult queryReIdTaskItemPageByLabelTaskId(String reIdTaskId,int currPage, int pageSize,String related_task_id,int orderType,int findLast,String token){
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		PageResult pageResult = new PageResult();

		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		paramMap.put("orderType", orderType);
		paramMap.put("label_task_id", reIdTaskId);
		if(related_task_id != null) {
			paramMap.put("pic_url", related_task_id);
		}
		paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE));
		
		ReIDTask reIdTask = reIDTaskDao.queryReIDTaskById(reIdTaskId);

		List<String> taskIdList = new ArrayList<>();
		taskIdList.add(reIdTask.getSrc_predict_taskid());

		if(!Strings.isEmpty(reIdTask.getDest_predict_taskid())) {
			List<String> destPredictTaskIdList = gson.fromJson(reIdTask.getDest_predict_taskid(), new TypeToken<List<String>>() {
				private static final long serialVersionUID = 1L;}.getType());
			taskIdList.addAll(destPredictTaskIdList);
		}

		Map<String,String> taskNameList = getTaskName(taskIdList, reIdTask.getTask_type(),true);

		if(reIdTask != null) {
			int totalCount = reIdLabelTaskItemDao.queryLabelTaskItemPageCountByLabelTaskId(paramMap);

			List<LabelTaskItem> result = new ArrayList<>();

			if(findLast == Constants.QUERY_ITEM_PAGE_FIND_LAST) {
				int count = 0;
				while(count < totalCount) {
					logger.info("currPage=" + currPage);
					result = reIdLabelTaskItemDao.queryLabelTaskItemPageByLabelTaskId(paramMap);
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
			}else {
				result = reIdLabelTaskItemDao.queryLabelTaskItemPageByLabelTaskId(paramMap);
			}

			String srcLabelTaskId = reIdTask.getSrc_predict_taskid();
			for(LabelTaskItem item : result) {
				if(srcLabelTaskId.equals(item.getPic_url())) {
					item.setDisplay_order2(1);
				}
				item.setDisplay(taskNameList.get(item.getPic_url()));
			}

			pageResult.setTotal(totalCount);
			pageResult.setData(result);
			pageResult.setCurrent(currPage);
		}

		return pageResult;
	}


	public PageResult queryReIdTaskShowResultPageByLabelTaskId(String reIdTaskId,int currPage, int pageSize,String token){
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		PageResult pageResult = new PageResult();

		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		paramMap.put("label_task_id", reIdTaskId);

		List<ReIDTaskShowResult> resultList = reIDTaskShowResultDao.queryReIDShowTaskResultPage(paramMap);
		int totalCount = reIDTaskShowResultDao.queryReIDShowTaskResultPageCount(paramMap);

		List<DisplayReIDTaskShowResult> returnList = new ArrayList<>();

		if(totalCount > 0) {
			int maxLength = 0;

			for(ReIDTaskShowResult item : resultList) {
				DisplayReIDTaskShowResult re = new DisplayReIDTaskShowResult();
				re.setReIdName(item.getReid_name());

				String mapStr = item.getRelated_info();

				Map<String,String> map =  gson.fromJson(mapStr, new TypeToken<Map<String,String>>() {
					private static final long serialVersionUID = 1L;}.getType());


				List<String> imgList = new ArrayList<>();
				imgList.addAll(map.keySet());
				Collections.sort(imgList);
				List<String> itemIdList = new ArrayList<>();
				for(String key : imgList) {
					itemIdList.add(map.get(key));
				}
				re.setImgList(imgList);
				re.setItemIdList(itemIdList);
				returnList.add(re);

				if(imgList.size() > maxLength) {
					maxLength = imgList.size();
				}
			}
		}

		pageResult.setTotal(totalCount);
		pageResult.setData(returnList);
		pageResult.setCurrent(currPage);

		//异步的执行一次更新
		if(timeMap.containsKey(reIdTaskId)) {
			Long lasttime = timeMap.get(reIdTaskId);
			if(Math.abs(lasttime - System.currentTimeMillis()) < 60000l) {
				return pageResult;
			}
		}
		logger.info("start to exe updateReIdResultToShowResultDB..useid=" + userId);
		pool.execute(()->{
			timeMap.put(reIdTaskId, System.currentTimeMillis());
			reIdSchedule.updateReIdResultToShowResultDB(userId,reIdTaskId);
		});
		logger.info("end to  updateReIdResultToShowResultDB..");
		return pageResult;
	}


	public void deleteReIdByIdAndName(String token, String reIDTaskId, String reIdName) {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("label_task_id", reIDTaskId);
		paramMap.put("reid_name", reIdName);

		ReIDTaskShowResult result = reIDTaskShowResultDao.queryReIDShowTaskResult(paramMap);

		if(result != null) {
			String relatedInfo = result.getRelated_info();
			Map<String,Object> map = JsonUtil.getMap(relatedInfo);
			List<String> itemIdList = new ArrayList<>();
			Map<String,String> itemMapLabelId = new HashMap<>();
			for(Entry<String,Object> entry : map.entrySet()) {
				String labelItemId = entry.getValue().toString();
				itemIdList.add(labelItemId);
				String img = entry.getKey();
				String labelId = ReIDUtil.getLabelId(img);
				itemMapLabelId.put(labelItemId, labelId);
			}
			Map<String,Object> tmpParamMap = new HashMap<>();
			tmpParamMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE));
			tmpParamMap.put("list", itemIdList);
			List<LabelTaskItem> list = reIdLabelTaskItemDao.queryLabelTaskItemByIdList(tmpParamMap);

			for(LabelTaskItem item : list) {
				List<Map<String,Object>> labelList = JsonUtil.getLabelList(item.getLabel_info());
				//logger.info("old labelInfo=" + item.getLabel_info());

				for(Map<String,Object> label : labelList) {
					String id = itemMapLabelId.get(item.getId());
					if(isEqual(id, label.get("id"))) {
						setReIdNull(label,reIdName);
					}
				}

				//logger.info("new labelinfo=" + JsonUtil.toJson(labelList));
				Map<String,Object> itemParamMap = new HashMap<>();
				itemParamMap.put("id", item.getId());
				itemParamMap.put("item_add_time", TimeUtil.getCurrentTimeStr());
				itemParamMap.put("label_info", JsonUtil.toJson(labelList));
				itemParamMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE));
				reIdLabelTaskItemDao.updateLabelTaskItem(itemParamMap);

			}
			reIDTaskShowResultDao.deleteByReIDTaskAndReIdName(paramMap);
		}
	}

	private boolean reNameReID(Map<String, Object> label,String reIdName,String newReIdName) {
		boolean result = false;
		if(isEqual(reIdName, label.get(Constants.REID_KEY))) {
			label.put(Constants.REID_KEY,newReIdName);
			result = true;
		}
		if(label.get("other") != null) {
			Map<String,Object> other = (Map<String,Object>)label.get("other");
			if(other.get("region_attributes") != null) {
				Map<String,Object> region = (Map<String,Object>)other.get("region_attributes");
				if(isEqual(reIdName, region.get(Constants.REID_KEY))) {
					region.put(Constants.REID_KEY,newReIdName);
					result = true;
				}
			}
		}
		return result;
	}

	private boolean isContainReID(Map<String, Object> label,String reIdName) {

		if(isEqual(reIdName, label.get(Constants.REID_KEY))) {
			return true;
		}
		if(label.get("other") != null) {
			Map<String,Object> other = (Map<String,Object>)label.get("other");
			if(other.get("region_attributes") != null) {
				Map<String,Object> region = (Map<String,Object>)other.get("region_attributes");
				if(isEqual(reIdName, region.get(Constants.REID_KEY))) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean setReIdNull(Map<String, Object> label,String reIdName) {
		boolean result = false;
		if(isEqual(reIdName, label.get(Constants.REID_KEY))) {
			label.remove(Constants.REID_KEY);
			result = true;
		}
		if(label.get("other") != null) {
			Map<String,Object> other = (Map<String,Object>)label.get("other");
			if(other.get("region_attributes") != null) {
				Map<String,Object> region = (Map<String,Object>)other.get("region_attributes");
				if(isEqual(reIdName, region.get(Constants.REID_KEY))) {
					region.remove(Constants.REID_KEY);
					result= true;
				}
			}
		}
		return result;
	}

	private boolean isEqual(String id, Object idObj) {
		if(id != null && idObj != null) {
			return id.equals(idObj.toString());
		}
		return false;
	}

	public void deleteAReIdImg(String token, String reIDTaskId, String reIdName, String imgName) {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("label_task_id", reIDTaskId);
		paramMap.put("reid_name", reIdName);

		ReIDTaskShowResult result = reIDTaskShowResultDao.queryReIDShowTaskResult(paramMap);

		if(result != null) {
			String relatedInfo = result.getRelated_info();
			Map<String,Object> map = JsonUtil.getMap(relatedInfo);
			List<String> itemIdList = new ArrayList<>();
			Map<String,String> itemMapLabelId = new HashMap<>();
			for(Entry<String,Object> entry : map.entrySet()) {
				String labelItemId = entry.getValue().toString();
				itemIdList.add(labelItemId);
				String img = entry.getKey();
				if(img.equals(imgName)) {
					String labelId = ReIDUtil.getLabelId(img);
					itemMapLabelId.put(labelItemId, labelId);
				}
			}
			Map<String,Object> tmpParamMap = new HashMap<>();
			tmpParamMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE));
			tmpParamMap.put("list", itemIdList);
			List<LabelTaskItem> list = reIdLabelTaskItemDao.queryLabelTaskItemByIdList(tmpParamMap);
			//logger.info("list size=" + list.size());
			for(LabelTaskItem item : list) {
				List<Map<String,Object>> labelList = JsonUtil.getLabelList(item.getLabel_info());
				boolean re = false;
				//logger.info("old labelInfo=" + item.getLabel_info());
				for(Map<String,Object> label : labelList) {
					String id = itemMapLabelId.get(item.getId());
					if(isEqual(id, label.get("id"))) {
						re = setReIdNull(label,reIdName);
						if(re) {
							break;
						}
					}
				}
				if(re) {
					//logger.info("new labelinfo=" + JsonUtil.toJson(labelList));
					Map<String,Object> itemParamMap = new HashMap<>();
					itemParamMap.put("id", item.getId());
					itemParamMap.put("item_add_time", TimeUtil.getCurrentTimeStr());
					itemParamMap.put("label_info", JsonUtil.toJson(labelList));
					itemParamMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE));
					reIdLabelTaskItemDao.updateLabelTaskItem(itemParamMap);
				}
			}

			map.remove(imgName);
			paramMap.put("related_info", JsonUtil.toJson(map));
			reIDTaskShowResultDao.updateShowResult(paramMap);

			//reIDTaskShowResultDao.deleteByReIDTaskAndReIdName(paramMap);
		}

	}

	public void modifyAReIdImg(String token, String reIDTaskId, String reIdName, String imgName, String newReIdName) {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("label_task_id", reIDTaskId);
		paramMap.put("reid_name", reIdName);

		ReIDTaskShowResult result = reIDTaskShowResultDao.queryReIDShowTaskResult(paramMap);
		String newItemId = null;
		if(result != null) {
			String relatedInfo = result.getRelated_info();
			Map<String,Object> map = JsonUtil.getMap(relatedInfo);
			List<String> itemIdList = new ArrayList<>();
			Map<String,String> itemMapLabelId = new HashMap<>();
			for(Entry<String,Object> entry : map.entrySet()) {
				String labelItemId = entry.getValue().toString();
				itemIdList.add(labelItemId);
				String img = entry.getKey();
				if(img.equals(imgName)) {
					String labelId = ReIDUtil.getLabelId(img);
					itemMapLabelId.put(labelItemId, labelId);
				}
			}
			Map<String,Object> tmpParamMap = new HashMap<>();
			tmpParamMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE));
			tmpParamMap.put("list", itemIdList);
			List<LabelTaskItem> list = reIdLabelTaskItemDao.queryLabelTaskItemByIdList(tmpParamMap);
			//logger.info("list size=" + list.size());
			for(LabelTaskItem item : list) {
				List<Map<String,Object>> labelList = JsonUtil.getLabelList(item.getLabel_info());
				boolean re = false;
				//logger.info("old labelInfo=" + item.getLabel_info());
				for(Map<String,Object> label : labelList) {
					String id = itemMapLabelId.get(item.getId());
					if(isEqual(id, label.get("id"))) {
						re = reNameReID(label,reIdName,newReIdName);
						if(re) {
							newItemId = item.getId();
							break;
						}
					}
				}
				if(re) {
					//logger.info("new labelinfo=" + JsonUtil.toJson(labelList));
					Map<String,Object> itemParamMap = new HashMap<>();
					itemParamMap.put("id", item.getId());
					itemParamMap.put("item_add_time", TimeUtil.getCurrentTimeStr());
					itemParamMap.put("label_info", JsonUtil.toJson(labelList));
					itemParamMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE));
					reIdLabelTaskItemDao.updateLabelTaskItem(itemParamMap);
					break;
				}
			}

			map.remove(imgName);
			paramMap.put("related_info", JsonUtil.toJson(map));
			reIDTaskShowResultDao.updateShowResult(paramMap);

		}

		//在新的reID标注中新增记录。
		Map<String,Object> newParamMap = new HashMap<>();
		newParamMap.put("label_task_id", reIDTaskId);
		newParamMap.put("reid_name", newReIdName);

		ReIDTaskShowResult newResult = reIDTaskShowResultDao.queryReIDShowTaskResult(newParamMap);

		if(newResult != null && newItemId != null) {
			String relatedInfo = newResult.getRelated_info();
			Map<String,Object> map = JsonUtil.getMap(relatedInfo);
			map.put(imgName, newItemId);
			newParamMap.put("related_info", JsonUtil.toJson(map));
			reIDTaskShowResultDao.updateShowResult(newParamMap);
		}
	}

	public void modifyReIdByIdAndName(String token, String reIDTaskId, String reIdName, String newReIdName) {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("label_task_id", reIDTaskId);
		paramMap.put("reid_name", reIdName);

		ReIDTaskShowResult result = reIDTaskShowResultDao.queryReIDShowTaskResult(paramMap);

		if(result != null) {
			String relatedInfo = result.getRelated_info();
			Map<String,Object> map = JsonUtil.getMap(relatedInfo);
			List<String> itemIdList = new ArrayList<>();
			Map<String,String> itemMapLabelId = new HashMap<>();
			for(Entry<String,Object> entry : map.entrySet()) {
				String labelItemId = entry.getValue().toString();
				itemIdList.add(labelItemId);
				String img = entry.getKey();
				String labelId = ReIDUtil.getLabelId(img);
				itemMapLabelId.put(labelItemId, labelId);
			}
			Map<String,Object> tmpParamMap = new HashMap<>();
			tmpParamMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE));
			tmpParamMap.put("list", itemIdList);
			List<LabelTaskItem> list = reIdLabelTaskItemDao.queryLabelTaskItemByIdList(tmpParamMap);

			for(LabelTaskItem item : list) {
				List<Map<String,Object>> labelList = JsonUtil.getLabelList(item.getLabel_info());
				//logger.info("old labelInfo=" + item.getLabel_info());

				for(Map<String,Object> label : labelList) {
					String id = itemMapLabelId.get(item.getId());
					if(isEqual(id, label.get("id"))) {
						reNameReID(label,reIdName,newReIdName);
					}
				}

				//logger.info("new labelinfo=" + JsonUtil.toJson(labelList));
				Map<String,Object> itemParamMap = new HashMap<>();
				itemParamMap.put("id", item.getId());
				itemParamMap.put("item_add_time", TimeUtil.getCurrentTimeStr());
				itemParamMap.put("label_info", JsonUtil.toJson(labelList));
				itemParamMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE));
				reIdLabelTaskItemDao.updateLabelTaskItem(itemParamMap);

			}
			reIDTaskShowResultDao.deleteByReIDTaskAndReIdName(paramMap);

			ReIDTaskShowResult newReID = new ReIDTaskShowResult();
			newReID.setLabel_task_id(reIDTaskId);
			newReID.setReid_name(newReIdName);
			newReID.setRelated_info(result.getRelated_info());

			reIDTaskShowResultDao.addBatchShowResultItem(Arrays.asList(newReID));
		}
	}

	public void deleteReIdLabel(String reIdTaskId, Integer startId, Integer endId,String one_reid_name, String token) {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		List<LabelTaskItem> taskItem = reIdLabelTaskItemDao.queryLabelTaskItemByLabelTaskIdOderbyImageNameAsc(TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE),reIdTaskId);
		List<LabelTaskItem> deleteItemList = new ArrayList<>();
		for(int i = startId - 1; i< taskItem.size() && i < endId; i++) {
			deleteItemList.add(taskItem.get(i));
		}
		for(LabelTaskItem deleteItem : deleteItemList) {
			Map<String,Object> paramMap = new HashMap<>();
			paramMap.put("id", deleteItem.getId());
			if(one_reid_name != null && one_reid_name.length() > 0) {
				List<Map<String,Object>> labelList = JsonUtil.getLabelList(deleteItem.getLabel_info());
				for(int i = 0; i <labelList.size(); i++) {
					Map<String,Object> label = labelList.get(i);
					if(isContainReID(label, one_reid_name)) {
						labelList.remove(i);
						break;
					}
				}
				paramMap.put("label_info", JsonUtil.toJson(labelList));
				if(labelList.size() == 0) {
					paramMap.put("label_status", Constants.LABEL_TASK_STATUS_NOT_FINISHED);
				}else {
					paramMap.put("label_status", Constants.LABEL_TASK_STATUS_FINISHED);
				}
			}else {
				paramMap.put("label_info", JsonUtil.toJson(new ArrayList<>()));
				paramMap.put("label_status", Constants.LABEL_TASK_STATUS_NOT_FINISHED);
			}
			paramMap.put("item_add_time", TimeUtil.getCurrentTimeStr());
			paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE));
			reIdLabelTaskItemDao.updateLabelTaskItem(paramMap);
		}

	}

	public List<ReIDTask> queryReIdTaskbyUser(String token) {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));

		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("user_id", userId);
		List<ReIDTask> labelTaskList = reIDTaskDao.queryReIDTaskByUser(paramMap);

		return labelTaskList;
	}

	public void addReIdResultAutoSort(String token, String reTaskId) {
		ThreadSchedule.execThread(()->{showResultSchedule.execReIDTaskNew(reTaskId);});
	}

	public String queryLabelProperty(String token, String reIDTaskId) {
		ReIDTask task = reIDTaskDao.queryReIDTaskById(reIDTaskId);
		
		return task.getTask_label_type_info();
		
	}

}
