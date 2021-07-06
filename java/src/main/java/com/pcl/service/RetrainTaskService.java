package com.pcl.service;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pcl.constant.Constants;
import com.pcl.dao.AlgInstanceDao;
import com.pcl.dao.AlgModelDao;
import com.pcl.dao.RetrainTaskDao;
import com.pcl.dao.RetrainTaskMsgResultDao;
import com.pcl.dao.UserDao;
import com.pcl.pojo.PageResult;
import com.pcl.pojo.body.RetrainTaskBody;
import com.pcl.pojo.display.DisplayRetrainResult;
import com.pcl.pojo.display.DisplayRetrainTask;
import com.pcl.pojo.display.TrainObject;
import com.pcl.pojo.display.ValObject;
import com.pcl.pojo.mybatis.AlgInstance;
import com.pcl.pojo.mybatis.AlgModel;
import com.pcl.pojo.mybatis.RetrainTask;
import com.pcl.pojo.mybatis.RetrainTaskMsgResult;
import com.pcl.pojo.mybatis.User;
import com.pcl.service.schedule.PredictPersonProperty;
import com.pcl.service.schedule.retrain.RetrainTaskSchedule;
import com.pcl.util.FileUtil;
import com.pcl.util.TimeUtil;

import org.slf4j.Logger;


@Service
public class RetrainTaskService {

	@Autowired
	private RetrainTaskDao retrainTaskDao;

	@Autowired
	private UserDao userDao;
	
	@Autowired
	private AlgModelDao algModelDao;
	
	@Autowired
	private AlgInstanceDao algInstanceDao;
	
	@Autowired
	private RetrainTaskSchedule retrainTaskSchedule;
	
	@Autowired
	private RetrainTaskMsgResultDao retrainTaskMsgResultDao;
	
	private Gson gson = new Gson();
	
	private static Logger logger = LoggerFactory.getLogger(RetrainTaskService.class);
	
	public int addRetrainTask(String token,RetrainTaskBody body) {
		
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		
		RetrainTask task = new RetrainTask();
		
		task.setTask_name(body.getTaskName());
		task.setAlg_model_id(body.getAlgModel());
		task.setId(UUID.randomUUID().toString().replaceAll("-",""));
		task.setPre_predict_task_id(body.getPrePredictTaskId());
		task.setTask_status(Constants.RETRAINTASK_STATUS_PROGRESSING);
		task.setUser_id(userId);
		task.setDetection_type(body.getDetection_type());
		task.setDetection_type_input(body.getDetection_type_input());
		task.setRetrain_data(body.getRetrain_data());
		task.setRetrain_type(body.getRetrain_type());
		task.setRetrain_model_name(body.getRetrain_model_name());
		task.setTestTrainRatio(body.getTestTrainRatio());
		
		int re = retrainTaskDao.addRetrainTask(task);
		
		startRetrainTask(task);
		
		return re;
	}
	
	
	

	public PageResult queryRetrainTask(String token, int currPage, int pageSize) {

		PageResult pageResult = new PageResult();
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		List<DisplayRetrainTask> re = new ArrayList<>();

		pageResult.setData(re);
		
		Map<String,Integer> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		paramMap.put("user_id", userId);

		List<RetrainTask> dbTaskList = retrainTaskDao.queryRetrainTask(paramMap);
		int total = retrainTaskDao.queryRetrainTaskCount(paramMap);

		pageResult.setTotal(total);
		pageResult.setCurrent(currPage);
		
		if(dbTaskList != null && dbTaskList.size() > 0) {
			List<User> userList = userDao.queryAll();
			HashMap<Integer,String> userIdForName = new HashMap<>();
			for(User user : userList) {
				userIdForName.put(user.getId(), user.getUsername());
			}
			
			List<AlgModel> allModelList = algModelDao.queryAlgModelAll();
			
			HashMap<Integer,String> modelIdForName = new HashMap<>();
			for(AlgModel algModel : allModelList) {
				modelIdForName.put(algModel.getId(), algModel.getModel_name());
			}

			for(RetrainTask task : dbTaskList) {
				DisplayRetrainTask disPlayTask = new DisplayRetrainTask();

				disPlayTask.setId(task.getId());
				disPlayTask.setTask_name(task.getTask_name());
				disPlayTask.setTask_start_time(getTaskStartTime(task.getTask_start_time()));
				disPlayTask.setTask_finish_time(task.getTask_finish_time());

				if(userIdForName.containsKey(task.getUser_id())) {
					disPlayTask.setUser(userIdForName.get(task.getUser_id()));
				}
				
				if(modelIdForName.containsKey(task.getAlg_model_id())) {
					disPlayTask.setAlg_model(modelIdForName.get(task.getAlg_model_id()));
				}

				disPlayTask.setTask_status(getTaskStatus(task.getTask_status()));
				
				disPlayTask.setPre_predict_task(task.getPre_predict_task_id());
				
				re.add(disPlayTask);
			}
		}
		return pageResult;
	}
	
	
	private String getTaskStartTime(String startTime) {
		if(startTime == null) {
			return "未开始";
		}
		return startTime;
	}

	
	private String getTaskStatus(int task_status) {
		String re = "";
		if(task_status == Constants.RETRAINTASK_STATUS_FINISHED) {
			re = "完成";
		}else if(task_status == Constants.RETRAINTASK_STATUS_PROGRESSING) {
			re = "进行中";
		}else if(task_status == Constants.RETRAINTASK_STATUS_EXCEPTION){
			re = "异常";
		}else if(task_status == Constants.RETRAINTASK_STATUS_NOT_STARTED) {
			re = "未开始";
		}else if(task_status == Constants.RETRAINTASK_STATUS_WAITING) {
			re = "等待调度";
		}
		return re;
	}


	public void startRetrainTask(String token, String retrainTaskId) {
		RetrainTask retrainTask = retrainTaskDao.queryRetrainTaskById(retrainTaskId);
		
		startRetrainTask(retrainTask);
	}
	
	private void startRetrainTask(RetrainTask retrainTask) {
		retrainTaskSchedule.addTask(retrainTask);
	}




	public int receiveRetrainTask(String token, Map<String,Object> paramMap) {
		
		paramMap.put("item_cur_time",TimeUtil.getCurrentTimeStr());
		
		return retrainTaskMsgResultDao.updateRetrainTaskMsgResult(paramMap);
		
	}




	public int deleteRetrainTaskId(String token, String retrainTaskId) {
		
		retrainTaskMsgResultDao.deleteRetrainTaskMsgResultById(retrainTaskId);
		
		return retrainTaskDao.deleteRetrainTaskById(retrainTaskId);
	}


	public DisplayRetrainResult queryRetrainTaskResult(String token, String retrainTaskId) {
		
		DisplayRetrainResult re = new DisplayRetrainResult();
		re.setId(retrainTaskId);
		
		String path = getJsonPath(retrainTaskId);
		
		logger.info("path =" + path);
		
		List<ValObject> valList = new ArrayList<>();
		List<TrainObject> trainList = new ArrayList<>();
		
		if(path != null) {
			List<String> allLineList = FileUtil.getAllLineList(path, "utf-8");
			for(String line : allLineList) {
				//logger.info("line=" + line);
				Map<String,Object> jsonMap = gson.fromJson(line, new TypeToken<Map<String,Object>>(){}.getType());
				String model = String.valueOf(jsonMap.get("mode"));
				int epoch = getIntValue(jsonMap.get("epoch"));
				if(model.equals("train")) {
					TrainObject train = new TrainObject();
					train.setEpoch(epoch);
					train.setIter(getIntValue(jsonMap.get("iter")));
					train.setLoss(Double.parseDouble(String.valueOf(jsonMap.get("loss"))));
					trainList.add(train);
				}else if(model.equals("val")) {
					ValObject val = new ValObject();
					val.setEpoch(epoch);
					Object mAP = jsonMap.get("bbox_mAP_50");
					if(mAP == null) {
						mAP = jsonMap.get("mAP");
					}
					val.setBbox_mAP_50(Double.parseDouble(String.valueOf(mAP)));
					valList.add(val);
				}
			}
		}
		
		re.setTrainInfo(trainList);
		re.setValInfo(valList);
	
		return re;
	}

	private int getIntValue(Object value) {
		double d = Double.parseDouble(String.valueOf(value));
		return (int)Math.round(d);
	}

	private String getJsonPath(String retrainTaskId) {
		
		String os = System.getProperty("os.name"); 
		if(os.toLowerCase().startsWith("win")){
			
			URL url = this.getClass().getResource("/static/20200213_040821.log.json");
			
			return url.getFile();
			
		}else {
			
			RetrainTask restrainTask = retrainTaskDao.queryRetrainTaskById(retrainTaskId);
			
			int algModelId = restrainTask.getAlg_model_id();
			
			if(isPersonProperty(algModelId)) {
				String logDir = restrainTask.getConfPath();
				logger.info("json path=" + logDir + File.separator + "retrain/log/log_retrain.json");
				return logDir + File.separator + "retrain/log/log_retrain.json";
			}else {
				AlgModel algModel = algModelDao.queryAlgModelById(algModelId);
				AlgInstance algInstance = algInstanceDao.queryAlgInstanceById(Integer.parseInt(algModel.getAlg_instance_id()));
				String algRootPath = LabelDataSetMerge.getAlgRootPath(algInstance.getAlg_root_dir());
				String confPy = restrainTask.getConfPath();
				String dir = getWorkDir(algRootPath,confPy);
				String lastLogJson = FileUtil.getLastModifiedFile(dir, null, ".log.json");
				return lastLogJson;
			}
		}
	}
	
	private boolean isPersonProperty(int algModelId) {
		AlgModel algModel = algModelDao.queryAlgModelById(algModelId);
		if(algModel == null) {
			return false;
		}
		
		AlgInstance algInstance = algInstanceDao.queryAlgInstanceById(Integer.parseInt(algModel.getAlg_instance_id()));
		if(algInstance == null) {
			return false;
		}
		
		if(algInstance.getId() == 18) {
			return true;
		}
		return false;
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


	public RetrainTaskMsgResult queryRetrainTasResult(String token, String retrainTaskId) {
		
		return retrainTaskMsgResultDao.queryRetrainTaskMsgResultById(retrainTaskId);
		
	}
	

	
}
