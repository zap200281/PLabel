package com.pcl.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pcl.constant.Constants;
import com.pcl.constant.UserConstants;
import com.pcl.dao.AuthTokenDao;
import com.pcl.dao.LabelTaskItemDao;
import com.pcl.dao.PrePredictTaskResultDao;
import com.pcl.dao.ReIDLabelTaskItemDao;
import com.pcl.dao.UserDao;
import com.pcl.dao.UserExtendDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.PageResult;
import com.pcl.pojo.mybatis.LogSecInfo;
import com.pcl.pojo.mybatis.User;
import com.pcl.pojo.mybatis.UserExtend;
import com.pcl.util.JsonUtil;
import com.pcl.util.PwdCheckUtil;
import com.pcl.util.SHAUtil;
import com.pcl.util.TimeUtil;

@Service
public class UserService {

	private static Logger logger = LoggerFactory.getLogger(UserService.class);

	@Autowired
	private UserDao userDao;
	
	@Autowired
	private UserExtendDao userExtendDao;
	
	@Autowired
	private LabelTaskItemDao labelTaskItemDao;
	
	@Autowired
	private PrePredictTaskResultDao prePredictTaskResultDao;
	
	@Autowired
	private ReIDLabelTaskItemDao reIDLabelTaskItemDao;
	
	@Autowired
	private AuthTokenDao authTokenDao;
	
	@Value("${password.policy:2}")
	private int pwdPolicy;
	
	@Autowired
	private LogSecService logSecService;
	

	
	
	//private final static int DATASET_SINGLE_TABLE = 1;
	public int addMedicalUser(User user) throws LabelSystemException {
		logger.info("add medical user=" + JsonUtil.toJson(user));
		try {
			String savePasswordStr = SHAUtil.getEncriptStr(user.getPassword());
			user.setPassword(savePasswordStr);
			
			String timeStr = TimeUtil.getCurrentTimeStr();
			if(user.getDate_joined() == null) {
				user.setDate_joined(timeStr); 
			}
			if(user.getLast_login() == null) {
				user.setLast_login(timeStr);
			}
			if(user.getIs_superuser() == 0) {
				user.setIs_superuser(1);
			}
			
			logger.info("add user id:" + user.getId() + " name=" + user.getUsername());
			userDao.addUser(user);
			
			List<User> userList = userDao.queryUser(user.getUsername());
			int addUserID = userList.get(0).getId();
			
			LogSecInfo logSecInfo = new LogSecInfo();
			logSecInfo.setOper_id("addUser");
			logSecInfo.setLog_info("增加用户成功，用户名称：" + user.getUsername());
			logSecInfo.setUser_id(addUserID);
			logSecInfo.setOper_name("增加用户");
			logSecInfo.setOper_time_start(TimeUtil.getCurrentTimeStr());
			logSecInfo.setOper_time_end(TimeUtil.getCurrentTimeStr());
			
			logSecService.addSecLogInfo(logSecInfo);
			
			return userList.get(0).getId();
		}catch (Exception e) {
			e.printStackTrace();
			if(e.getMessage().indexOf("for key 'username'") != -1) {
				throw new LabelSystemException("用户名重复。");
			}else if(e.getMessage().indexOf("for key 'mobile'") != -1) {
				throw new LabelSystemException("移动电话已经被注册。");
			}
			throw new LabelSystemException(e.getMessage());
		}

	}
	

	public int addUser(String token, User user) throws LabelSystemException {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		
		User loginUser = userDao.queryUserById(userId);
		if(loginUser == null || loginUser.getIs_superuser() != Constants.USER_SUPER) {
			throw new LabelSystemException(user.getNick_name() + " 无权限添加用户。");
		}
		
		return addMedicalUser(user);

	}
	
	
	public int deleteUser(String token, int userId) throws LabelSystemException {
		int loginUserId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		User loginUser = userDao.queryUserById(loginUserId);
		if(loginUser == null || loginUser.getIs_superuser() != Constants.USER_SUPER) {
			throw new LabelSystemException(userId + " 无权限删除用户。");
		}
		//先删除token中的user
		authTokenDao.deleteTokenByUser(String.valueOf(userId));
		TokenManager.removeToken(token);
		try {
			User deleteUser = userDao.queryUserById(userId);
			userDao.deleteUser(userId);
			
			LogSecInfo logSecInfo = new LogSecInfo();
			logSecInfo.setOper_id("deleteUser");
			logSecInfo.setLog_info("删除用户成功，用户名称：" + deleteUser.getUsername());
			logSecInfo.setUser_id(userId);
			logSecInfo.setOper_name("删除用户");
			logSecInfo.setOper_time_start(TimeUtil.getCurrentTimeStr());
			logSecInfo.setOper_time_end(TimeUtil.getCurrentTimeStr());
			
			logSecService.addSecLogInfo(logSecInfo);
			
			return 1;
		}catch (Exception e) {
			logger.info(e.getMessage());
			throw new LabelSystemException("该用户有创建任务数据，不能删除。如果要删除，请先删除创建的任务或数据集。");
		}
	}

	public PageResult queryUser(String token, Integer currPage, Integer pageSize) throws LabelSystemException {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		User loginUser = userDao.queryUserById(userId);
		if(loginUser == null || loginUser.getIs_superuser() != Constants.USER_SUPER) {
			throw new LabelSystemException(userId + " 无权限查看用户。");
		}
		
		Map<String,Integer> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		List<User> dbList = userDao.queryUserPage(paramMap);
		int totalCount = userDao.queryUserCount(paramMap);
		
		PageResult re = new PageResult();
		re.setCurrent(currPage);
		re.setTotal(totalCount);
		re.setData(dbList);
		
		return re;
	}
	
	public User queryUserById(int userId) {
		
		return userDao.queryUserById(userId);
		
	}
	
	public List<User> queryAllUser(String token) throws LabelSystemException {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		User loginUser = userDao.queryUserById(userId);
		if(loginUser == null) {
			throw new LabelSystemException(userId + " 无效的用户。");
		}

		return userDao.queryAllIdOrName();
	}
	
	public Map<Integer,String> getAllUser(){
		List<User> userList = userDao.queryAll();
		HashMap<Integer,String> userIdForName = new HashMap<>();
		for(User user : userList) {
			userIdForName.put(user.getId(), user.getUsername());
		}
		return userIdForName;
	}


	public List<User> queryAllUserBySuperUser(String token) throws LabelSystemException {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		User loginUser = userDao.queryUserById(userId);
		if(loginUser == null) {
			throw new LabelSystemException("非法用户。user_id=" + userId);
		}
		if(loginUser.getIs_superuser() == Constants.USER_SUPER) {
			return userDao.queryAll();
		}else {
			return Arrays.asList(loginUser);
		}
	}


	public void updateUserPassword(String token, int user_id, String newPassword,String oldPassword) throws LabelSystemException {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		User loginUser = userDao.queryUserById(userId);
		if(loginUser.getIs_superuser() != Constants.USER_SUPER) {
			if(user_id != userId) {
				throw new LabelSystemException("无权限修改。");
			}
		}
		
		if(loginUser.getIs_superuser() != Constants.USER_SUPER) {//超级用户直接修改。
			User updateUser = userDao.queryUserById(user_id);
			String oldPasswordStr = SHAUtil.getEncriptStr(oldPassword);
			if(!oldPasswordStr.equals(updateUser.getPassword())) {
				throw new LabelSystemException("旧密码不对，修改错误。");
			}
		}
		if(UserConstants.POLICY_MIDDLE == pwdPolicy || UserConstants.POLICY_COMPLEX == pwdPolicy) {
			if(!PwdCheckUtil.checkPasswordLength(newPassword, "8", "24")){
				throw new LabelSystemException("密码长度要大于8位小于24位字符。");
			}
			if(UserConstants.POLICY_MIDDLE == pwdPolicy) {
				if(!(PwdCheckUtil.checkContainDigit(newPassword) && PwdCheckUtil.checkContainCase(newPassword))) {
					throw new LabelSystemException("密码中要包括数字和字母。");
				}
			}
			if(UserConstants.POLICY_COMPLEX == pwdPolicy) {
				if(!(PwdCheckUtil.checkContainDigit(newPassword) && PwdCheckUtil.checkContainCase(newPassword) && PwdCheckUtil.checkContainSpecialChar(newPassword))) {
					throw new LabelSystemException("密码中要包括数字和字母及特殊字符。");
				}
			}
		}
		
		logger.info("update password. user=" + loginUser.getUsername());
		String savePasswordStr = SHAUtil.getEncriptStr(newPassword);
		User user = new User();
		user.setId(user_id);
		user.setPassword(savePasswordStr);
		userDao.updateUserPassword(user);
		
		LogSecInfo logSecInfo = new LogSecInfo();
		logSecInfo.setOper_id("updateUser");
		logSecInfo.setLog_info("更新用户密码成功，用户名称：" + loginUser.getUsername());
		logSecInfo.setUser_id(userId);
		logSecInfo.setOper_name("更新用户");
		logSecInfo.setOper_time_start(TimeUtil.getCurrentTimeStr());
		logSecInfo.setOper_time_end(TimeUtil.getCurrentTimeStr());
		
		logSecService.addSecLogInfo(logSecInfo);
	}


	public List<User> queryVerifyUser(String token) throws LabelSystemException {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		User loginUser = userDao.queryUserById(userId);
		if(loginUser == null) {
			throw new LabelSystemException(userId + " 无效的用户。");
		}
		return userDao.queryVerifyUser();
	}


	public int queryUserIdByToken(String token) throws LabelSystemException {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		User loginUser = userDao.queryUserById(userId);
		if(loginUser == null) {
			throw new LabelSystemException(userId + " 无效的用户。");
		}
		return userId;
	}


	public void updateUserExtendTableName(String token, int user_id, String funcTableInfo) throws LabelSystemException {
		List<String> tableInfoList = JsonUtil.getList(funcTableInfo);
		
		if(tableInfoList.isEmpty()) {
			return;
		}
		
		UserExtend userExtend = new UserExtend();
		userExtend.setUser_id(user_id);
		userExtend.setFuncTableName(funcTableInfo);
		userExtend.setOperTime(TimeUtil.getCurrentTimeStr());
		
		if(userExtendDao.queryUserExtend(user_id) == null) {
		   userExtendDao.addUserExtend(userExtend);
		}else {
		   userExtendDao.updateUserExtendFuncTableName(userExtend);
		}
		
		//创建表
		for(String tableInfo : tableInfoList) {
			if(UserConstants.LABEL_TASK_SINGLE_TABLE == Integer.parseInt(tableInfo)) {
				String tableName = UserConstants.LABEL_TASK_SINGLE_TABLE_NAME + user_id;
				if(labelTaskItemDao.existTable(tableName) == 0) {
					labelTaskItemDao.createTable(tableName);
				}
				
			}
			else if(UserConstants.PREDICT_SINGLE_TABLE == Integer.parseInt(tableInfo)) {
				String tableName = UserConstants.PREDICT_SINGLE_TABLE_NAME + user_id;
				if(prePredictTaskResultDao.existTable(tableName) == 0) {
					prePredictTaskResultDao.createTable(tableName);
				}
			}
			else if(UserConstants.REID_TASK_SINGLE_TABLE == Integer.parseInt(tableInfo)) {
				String tableName = UserConstants.REID_TASK_SINGLE_TABLE_NAME + user_id;
				if(reIDLabelTaskItemDao.existTable(tableName) == 0) {
					reIDLabelTaskItemDao.createTable(tableName);
				}
			}
		}
	}


	public void updateMedicalUserPassword(String userName, String newPassword) throws LabelSystemException {
		
		List<User> loginUserList = userDao.queryUser(userName);
		if(loginUserList == null || loginUserList.isEmpty()) {
			return;
		}
		User loginUser = loginUserList.get(0);
		logger.info("update password. user=" + loginUser.getUsername());
		String savePasswordStr = SHAUtil.getEncriptStr(newPassword);
		User user = new User();
		user.setId(loginUser.getId());
		user.setPassword(savePasswordStr);
		userDao.updateUserPassword(user);
		
		LogSecInfo logSecInfo = new LogSecInfo();
		logSecInfo.setOper_id("updateUser");
		logSecInfo.setLog_info("更新用户密码成功，用户名称：" + loginUser.getUsername());
		logSecInfo.setUser_id(loginUser.getId());
		logSecInfo.setOper_name("更新用户");
		logSecInfo.setOper_time_start(TimeUtil.getCurrentTimeStr());
		logSecInfo.setOper_time_end(TimeUtil.getCurrentTimeStr());
		
		logSecService.addSecLogInfo(logSecInfo);
	}


	public void updateUserIdentity(String token, int user_id, int identity) throws LabelSystemException {
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		User loginUser = userDao.queryUserById(userId);
		if(loginUser.getIs_superuser() != Constants.USER_SUPER) {
			if(user_id != userId) {
				throw new LabelSystemException("无权限修改。");
			}
		}
		
		if(!(identity == 1 || identity == 2)) {
			throw new LabelSystemException("只能将用户身份修改成标注人员或者审核人员。");
		}
		
		logger.info("update identity. user=" + loginUser.getUsername() + " identity=" + identity);

		User user = new User();
		user.setId(user_id);
		user.setIs_superuser(identity);
		userDao.updateUserIndentity(user);
		
		LogSecInfo logSecInfo = new LogSecInfo();
		logSecInfo.setOper_id("updateUser");
		logSecInfo.setLog_info("更新用户身份成功，用户身份：" + identity);
		logSecInfo.setUser_id(userId);
		logSecInfo.setOper_name("更新用户");
		logSecInfo.setOper_time_start(TimeUtil.getCurrentTimeStr());
		logSecInfo.setOper_time_end(TimeUtil.getCurrentTimeStr());
		
		logSecService.addSecLogInfo(logSecInfo);
	}
	

}
