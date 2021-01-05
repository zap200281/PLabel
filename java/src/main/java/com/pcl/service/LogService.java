package com.pcl.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pcl.dao.LogInfoDao;
import com.pcl.pojo.mybatis.LogInfo;
import com.pcl.service.schedule.ThreadSchedule;

@Service
public class LogService {

	@Autowired
	private LogInfoDao logInfoDao;
	
	private static Logger logger = LoggerFactory.getLogger(LogService.class);
	
		
	public void addLogInfo(LogInfo logInfo) {
		logger.info("add log info: " + logInfo.getOper_id() + " user_id=" + logInfo.getUser_id());
		
		logInfo.setId(UUID.randomUUID().toString().replaceAll("-",""));
		
		ThreadSchedule.execLogThread(()->logInfoDao.addLogInfo(logInfo));
		
	}
	
}
