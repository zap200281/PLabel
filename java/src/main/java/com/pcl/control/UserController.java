package com.pcl.control;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.PageResult;
import com.pcl.pojo.Result;
import com.pcl.pojo.mybatis.User;
import com.pcl.service.UserService;

@RestController
@RequestMapping("/api")
public class UserController {

	private static Logger logger = LoggerFactory.getLogger(UserController.class);

	@Autowired
	HttpServletRequest request;
	
	@Autowired
	private UserService userService;

	@RequestMapping(value = "/addUser",method = RequestMethod.POST)
	public Result addUser(@RequestBody User user) throws LabelSystemException {
		Result re = new Result();
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		try {
			int userId = userService.addUser(token,user);
			re.setCode(0);
			re.setMessage(String.valueOf(userId));
		} catch (LabelSystemException e) {
			logger.info(e.getMessage());
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	@RequestMapping(value = "/addMedicalUser",method = RequestMethod.POST)
	public Result addMedicalUser(@RequestBody User user,@RequestParam("append") String append) throws LabelSystemException {
		Result re = new Result();
		if(!append.equals("72a7b30d-28ba-4c47-8448-e89f0dd0f716")) {
			throw new LabelSystemException("非法的注册用户。");
		}
		try {
			int userId = userService.addMedicalUser(user);
			re.setCode(0);
			re.setMessage(String.valueOf(userId));
		} catch (LabelSystemException e) {
			logger.info(e.getMessage());
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	@RequestMapping(value = "/updateMedicalUserPassword",method = RequestMethod.POST)
	public Result updateMedicalUserPassword(@RequestParam("userName") String userName,@RequestParam("newPassword") String newPassword,@RequestParam("append") String append) throws LabelSystemException {
		Result re = new Result();
		if(!append.equals("72a7b30d-28ba-4c47-8448-e89f0dd0f716")) {
			throw new LabelSystemException("非法的注册用户。");
		}
		try {
			userService.updateMedicalUserPassword(userName,newPassword);
			re.setCode(0);
			re.setMessage("success");
		} catch (LabelSystemException e) {
			logger.info(e.getMessage());
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	@RequestMapping(value = "/updateUserPassword",method = RequestMethod.POST)
	public Result updateUserPassword(@RequestParam("user_id") int user_id,@RequestParam("newPassword") String newPassword,@RequestParam("oldPassword") String oldPassword) throws LabelSystemException {
		Result re = new Result();
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		try {
			userService.updateUserPassword(token,user_id,newPassword,oldPassword);
			re.setCode(0);
			re.setMessage("success");
		} catch (LabelSystemException e) {
			logger.info(e.getMessage());
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	/**
	 * 
	 * @param user_id  用户id
	 * @param identity  1为标注人员，2为审核人员
	 * @return
	 * @throws LabelSystemException
	 */
	@RequestMapping(value = "/updateUserIdentity",method = RequestMethod.POST)
	public Result updateUserIdentity(@RequestParam("user_id") int user_id,@RequestParam("identity") int identity) throws LabelSystemException {
		Result re = new Result();
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		try {
			userService.updateUserIdentity(token,user_id,identity);
			re.setCode(0);
			re.setMessage("success");
		} catch (LabelSystemException e) {
			logger.info(e.getMessage());
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
	@RequestMapping(value = "/queryUserPage",method = RequestMethod.GET)
	public PageResult queryUserPage(@RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize) throws LabelSystemException {
	
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("query user   token =" + token + ", startPage=" + startPage + " pageSize=" + pageSize);
		PageResult re = userService.queryUser(token,startPage,pageSize);
		return re;
	}
	
	@RequestMapping(value = "/queryAllUser",method = RequestMethod.GET)
	public List<User> queryAllUser() throws LabelSystemException {
	
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("query  all user   token =" + token);
		return userService.queryAllUser(token);
	}
	
	
	@RequestMapping(value = "/queryAllUserBySuperUser",method = RequestMethod.GET)
	public List<User> queryAllUserBySuperUser() throws LabelSystemException {
	
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryAllUserBySuperUser   token =" + token);
		return userService.queryAllUserBySuperUser(token);
	}
	
	

	@RequestMapping(value = "/queryVerifyUser",method = RequestMethod.GET)
	public List<User> queryVerifyUser() throws LabelSystemException {
	
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryVerifyUser   token =" + token);
		return userService.queryVerifyUser(token);
	}
	
	@RequestMapping(value = "/queryUserIdByToken",method = RequestMethod.GET)
	public int queryUserIdByToken() throws LabelSystemException {
	
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryUserIdByToken   token =" + token);
		return userService.queryUserIdByToken(token);
	}
	

	@RequestMapping(value = "/deleteUser",method = RequestMethod.DELETE)
	public Result deleteUser(@RequestParam("userId") int userId)  {
		Result re = new Result();
		re.setCode(1);
		re.setMessage("用户不存在。");
		String token = request.getHeader("authorization");
		if(token == null) {
			return re;
		}
		logger.info("delete User token =" + token);
		try {
			if(1  == userService.deleteUser(token, userId)) {
				re.setCode(0);
				re.setMessage(String.valueOf(userId));
			}
		} catch (LabelSystemException e) {
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}

	
	
	@RequestMapping(value = "/updateUserExtendTableName",method = RequestMethod.POST)
	public Result updateUserExtendTableName(@RequestParam("user_id") int user_id,@RequestParam("funcTableName") String funcTableInfo) throws LabelSystemException {
		Result re = new Result();
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("updateUserExtendTableName User user_id =" + user_id + " funcTableName=" + funcTableInfo);
		try {
			userService.updateUserExtendTableName(token,user_id,funcTableInfo);
			re.setCode(0);
			re.setMessage("success");
		} catch (LabelSystemException e) {
			logger.info(e.getMessage());
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
}
