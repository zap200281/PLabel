package com.pcl.service.schedule.retrain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pcl.constant.Constants;
import com.pcl.dao.RetrainTaskDao;
import com.pcl.pojo.mybatis.AlgModel;
import com.pcl.pojo.mybatis.RetrainTask;
import com.pcl.util.TimeUtil;

public abstract class ATask {
	
	public final static String RETRAIN = "0";
	
	public final static String INIT = "1";
	
	private static Logger logger = LoggerFactory.getLogger(ATask.class);

	public abstract void doExecute(RetrainTask retrainTask,List<Integer> availableGpuIdList);
	
	protected void updateTaskProgressing(String retrainTaskId,RetrainTaskDao retrainTaskDao) {
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", retrainTaskId);
		//更新数据库状态为  进行中
		paramMap.put("task_status", Constants.RETRAINTASK_STATUS_PROGRESSING);
		paramMap.put("task_status_desc", "");
		paramMap.put("task_start_time", TimeUtil.getCurrentTimeStr());
		retrainTaskDao.updateRetrainTask(paramMap);
	}
	
	protected void updateTaskFinish(String retrainTaskId,int retraintaskStatusFinished, String desc, RetrainTaskDao retrainTaskDao) {
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", retrainTaskId);
		paramMap.put("task_status", retraintaskStatusFinished);
		paramMap.put("task_status_desc", desc);
		paramMap.put("task_finished_time", TimeUtil.getCurrentTimeStr());
		retrainTaskDao.updateRetrainTask(paramMap);
	}
	
	protected String getGpuIdCommantStr(List<Integer> availableGpuIdList) {
		StringBuilder builder = new StringBuilder();
		if(availableGpuIdList.size() > 0) {
			for(Integer gpu : availableGpuIdList) {
				builder.append(gpu).append(",");
			}
			builder.deleteCharAt(builder.length() - 1);
		}
		return builder.toString(); 
	}

	protected String getTrainScript(int algModelId,AlgModel algModel) {

		if(algModel != null) {
			String trainScript = algModel.getTrain_script();
			trainScript = trainScript.replace("{configPath}", algModel.getConf_path());
			return trainScript;
		}

		return null;
	}


}
