package com.pcl.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pcl.dao.LogSecInfoDao;
import com.pcl.dao.UserDao;
import com.pcl.pojo.PageResult;
import com.pcl.pojo.mybatis.LogSecInfo;
import com.pcl.pojo.mybatis.User;
import com.pcl.service.schedule.ThreadSchedule;

@Service
public class LogSecService {

	@Autowired
	private LogSecInfoDao logInfoDao;
	
	@Autowired
	private UserDao userDao;
	
	private static Logger logger = LoggerFactory.getLogger(LogSecService.class);
	
		
	public void addSecLogInfo(LogSecInfo logInfo) {
		logger.info("add log info: " + logInfo.getOper_id() + " user_id=" + logInfo.getUser_id());
		
		logInfo.setId(UUID.randomUUID().toString().replaceAll("-",""));
		
		ThreadSchedule.execLogThread(()->logInfoDao.addLogSecInfo(logInfo));
		
	}
	
	public PageResult queryLogSecInfo(String token,Integer currPage,Integer pageSize){
		
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		
		List<LogSecInfo> dbList = logInfoDao.queryLogSecInfoPageForDay(paramMap);
		int totalCount = logInfoDao.queryLogSecInfoPageForDayCount(paramMap);
		logger.info("total count=" + totalCount);
		List<User> userList = userDao.queryAll();
		HashMap<Integer,String> userIdForName = new HashMap<>();
		for(User user : userList) {
			userIdForName.put(user.getId(), user.getUsername());
		}
		for(LogSecInfo logInfo : dbList) {
			if(userIdForName.containsKey(logInfo.getUser_id())) {
				logInfo.setUser_name(userIdForName.get(logInfo.getUser_id()));
			}
		}
		PageResult re = new PageResult();
		re.setCurrent(currPage);
		re.setTotal(totalCount);
		re.setData(dbList);
		
		return re;
	}
	
}
