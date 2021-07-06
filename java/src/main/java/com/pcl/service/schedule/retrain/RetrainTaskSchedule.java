package com.pcl.service.schedule.retrain;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pcl.constant.Constants;
import com.pcl.dao.RetrainTaskDao;
import com.pcl.pojo.mybatis.RetrainTask;
import com.pcl.util.GpuInfoUtil;

@Service
public class RetrainTaskSchedule {

	private static Logger logger = LoggerFactory.getLogger(RetrainTaskSchedule.class);

	private ArrayBlockingQueue<RetrainTask> queue = new ArrayBlockingQueue<>(100000);

	private ATask retrainExecutor;
	
	@Autowired
	private RetrainTaskFactory retrainTaskFactory;

	@Autowired
	private RetrainTaskDao retrainTaskDao;
	
	@Value("${server.enable.gpu:true}")
	private boolean enable = true;
	
	public boolean addTask(RetrainTask retrainTask) {
		logger.info("add retrain task..");
		return queue.offer(retrainTask);
	}

	@PostConstruct
	public void init() {
		if(!enable) {
			logger.info("no gpu, so not start to run retrain schedule.");
			return;
		}
		
		
		
		logger.info("start to init queue : RetrainTaskSchedule ");
		//从数据库加载未完成的任务继续运行。
		loadTaskFromDb();

		logger.info("start to execute runnable : RetrainTaskSchedule ");
		new Thread(new Runnable() {

			@Override
			public void run() {
				while(true) {
					try {
						//判断是否有可用的GPU 
						List<Integer> availableGpuIdList = GpuInfoUtil.getAvalibleGPUInfo(60);
						
						if(availableGpuIdList.size() == 0) {
							logger.info("Not gpu available. wait 120 second.");

							Iterator<RetrainTask> iterator = queue.iterator();

							while(iterator.hasNext()) {
								RetrainTask task = iterator.next();
								Map<String,Object> paramMap = new HashMap<>();
								paramMap.put("id", task.getId());
								paramMap.put("task_status", Constants.RETRAINTASK_STATUS_WAITING);
								paramMap.put("task_status_desc", "No GPU Available.");
								retrainTaskDao.updateRetrainTask(paramMap);
							}
							waitSecond(120);
							continue;
						}
						
						RetrainTask retrainTask = queue.take();
						
						retrainExecutor = retrainTaskFactory.getRetrainTask(retrainTask);
						
						logger.info("has retrain task..");
						retrainExecutor.doExecute(retrainTask, availableGpuIdList);
						
					}catch (Exception e) {
						logger.info("Failed to retrain task.");
						e.printStackTrace();
					}
				}

			}
		},"RetrainTaskSchedule").start();
	}

	protected void waitSecond(int second) {
		try {
			Thread.sleep(second * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void loadTaskFromDb() {

		List<RetrainTask> notStartTaskList = retrainTaskDao.queryRetrainTaskByStatus(String.valueOf(Constants.RETRAINTASK_STATUS_WAITING));
		for(RetrainTask retrainTask  : notStartTaskList) {
			addTask(retrainTask);
		}
	}




}
