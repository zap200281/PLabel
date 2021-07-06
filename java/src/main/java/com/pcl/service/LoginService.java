package com.pcl.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pcl.dao.AuthTokenDao;
import com.pcl.dao.LoginInfoDao;
import com.pcl.dao.UserDao;
import com.pcl.dao.UserExtendDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.Token;
import com.pcl.pojo.mybatis.AuthToken;
import com.pcl.pojo.mybatis.LogSecInfo;
import com.pcl.pojo.mybatis.LoginInfo;
import com.pcl.pojo.mybatis.User;
import com.pcl.pojo.mybatis.UserExtend;
import com.pcl.util.JsonUtil;
import com.pcl.util.SHAUtil;
import com.pcl.util.TimeUtil;

@Service
public class LoginService {

	private static Logger logger = LoggerFactory.getLogger(LoginService.class);
	
	@Autowired
	private UserDao userDao;
	
	@Autowired
	private UserExtendDao userExtendDao;
	
	@Autowired
	private AuthTokenDao authTokenDao;
	
	
	@Autowired
	private LogSecService logSecService;
	
	
	@Autowired
	private LoginInfoDao loginInfoDao;
	
	@Value("${password.loginError:5}")
	private int loginError;//登录错误次数达到5次锁定帐户30分钟
	
	private ConcurrentHashMap<String, List<Long>> ipForTime = new ConcurrentHashMap<>();


	public Token login(String userName,String password,String sourceIp) throws LabelSystemException {
		logger.info("IP:"+ sourceIp + " user:" + userName + " start to valid password." + password);

		List<Long> timeList = ipForTime.get(sourceIp);
		if(timeList == null) {
			timeList = new ArrayList<>();
			ipForTime.put(sourceIp, timeList);
		}
		timeList.add(System.currentTimeMillis());
		
		if(timeList.size() >5) {
			long last = timeList.remove(0);
			if((timeList.get(timeList.size() - 1) - last) < 60 * 1000) {
				throw new LabelSystemException(1000,"登录次数太过于频繁。");
			}
		}
	
		List<User> userList = userDao.queryUser(userName);
		if(userList == null || userList.isEmpty() || userList.size() > 1) {
			throw new LabelSystemException(1000,"User or Password error.");
		}
		
		User rightUser = userList.get(0);
		LoginInfo loginInfo = loginInfoDao.queryLoginInfo(rightUser.getId());
		if(loginInfo != null && loginInfo.getLogin_error_time() >= 5) {
			Date lastDate = TimeUtil.getDateTimebyStr(loginInfo.getLast_login_time());
			long lastTimeL = lastDate.getTime();
			logger.info("esc time=" + (System.currentTimeMillis() - lastTimeL));
			if((System.currentTimeMillis() - lastTimeL) < 30*60 * 1000) {
				throw new LabelSystemException(1001,"输入密码错误超过5次，帐户锁定30分钟。");
			}else {
				loginInfo.setLogin_error_time(0);
			}
			
		}
		
		String encriptPass = SHAUtil.getEncriptStr(password);
		logger.info("input password=" + encriptPass);
		logger.info("db password=" + rightUser.getPassword());
		if(encriptPass.equals(rightUser.getPassword())) {
			Token token = new Token();
			String tokenStr = TokenManager.getTokenByUserId(rightUser.getId());
			if(tokenStr != null) {
				//已经登录过则直接返回。
				token.setToken(tokenStr);
			}else {
				token.setToken(UUID.randomUUID().toString().replaceAll("-",""));
				//保存到数据库及缓存中
				saveTokenToDB(token.getToken(),rightUser.getId());
				TokenManager.addToken(token.getToken(), rightUser.getId());
				
				UserExtend extend = userExtendDao.queryUserExtend(rightUser.getId());
				if(extend != null) {
					logger.info("start add user extend to cache. extend=" + JsonUtil.toJson(extend));
					TokenManager.addUserExtend(rightUser.getId(), extend);
				}
				
			}
			token.setUserType(rightUser.getIs_superuser());
			token.setUserName(rightUser.getUsername());
			token.setNickName(rightUser.getNick_name());
			logger.info("user:" + userName + " login successfully.");
			logSecService.addSecLogInfo(getLoginLogSecInfo(rightUser,sourceIp));
			
			if(loginInfo != null) {
				loginInfo.setLast_login_time(TimeUtil.getCurrentTimeStr());
				loginInfo.setLogin_error_time(0);
				loginInfoDao.updateLoginInfo(loginInfo);
			}else {
				loginInfo = new LoginInfo();
				loginInfo.setUser_id(rightUser.getId());
				loginInfo.setLogin_error_time(0);
				loginInfo.setLast_login_time(TimeUtil.getCurrentTimeStr());
				loginInfoDao.addLoginInfo(loginInfo);
			}
			
			return token;
		}else {
			logger.info("user:" + userName + " login failed. password is not right.");
			
			if(loginInfo != null) {
				loginInfo.setLast_login_time(TimeUtil.getCurrentTimeStr());
				loginInfo.setLogin_error_time(loginInfo.getLogin_error_time() + 1);
				loginInfoDao.updateLoginInfo(loginInfo);
			}else {
				loginInfo = new LoginInfo();
				loginInfo.setUser_id(rightUser.getId());
				loginInfo.setLogin_error_time(1);
				loginInfo.setLast_login_time(TimeUtil.getCurrentTimeStr());
				loginInfoDao.addLoginInfo(loginInfo);
			}
			
			throw new LabelSystemException(1000,"User or Password error.");
		}
		
	}

	public void loginOut(String token,String userName,String sourceIP) throws LabelSystemException {
		
		logger.info("user:" + userName + " token:" + token + " start to login out.");
		List<User> userList = userDao.queryUser(userName);
		if(userList == null || userList.isEmpty() || userList.size() > 1) {
			throw new LabelSystemException(1000,"User or Password error.");
		}
		User rightUser = userList.get(0);
		String serverToken = TokenManager.getServerToken(token);
		TokenManager.removeToken(serverToken);
		authTokenDao.delete(serverToken);
		logSecService.addSecLogInfo(getLoginOutLogSecInfo(rightUser,sourceIP));
		logger.info("token:" + token + " login  out successfully.");
	}
	
	
	private LogSecInfo getLoginLogSecInfo(User rightUser,String sourceIp) {
		LogSecInfo logInfo = new LogSecInfo();
		
		logInfo.setLog_info("用户登录成功。IP:" + sourceIp);
		logInfo.setUser_id(rightUser.getId());
		logInfo.setOper_id("login");
		logInfo.setOper_name("用户登录");
		logInfo.setOper_time_start(TimeUtil.getCurrentTimeStr());
		logInfo.setOper_time_end(TimeUtil.getCurrentTimeStr());
		
		return logInfo;
		
	}
	
	private LogSecInfo getLoginOutLogSecInfo(User rightUser,String sourceIp) {
		LogSecInfo logInfo = new LogSecInfo();
		
		logInfo.setLog_info("用户退出登录成功。IP:" + sourceIp);
		logInfo.setUser_id(rightUser.getId());
		logInfo.setOper_id("loginout");
		logInfo.setOper_name("用户退出登录");
		logInfo.setOper_time_start(TimeUtil.getCurrentTimeStr());
		logInfo.setOper_time_end(TimeUtil.getCurrentTimeStr());
		return logInfo;
	}
	

	private void saveTokenToDB(String token, int id) {
		AuthToken authToken = new AuthToken();
		authToken.setToken(token);
		authToken.setUserId(id);
		authToken.setCreated(TimeUtil.getCurrentTimeStr());
		authToken.setLoginTime(System.currentTimeMillis());
		
		authTokenDao.deleteTokenByUser(String.valueOf(id));
		
		authTokenDao.saveAuthToken(authToken);
		
		
	}
}
