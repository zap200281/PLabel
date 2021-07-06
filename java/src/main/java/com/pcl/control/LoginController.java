package com.pcl.control;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.Result;
import com.pcl.pojo.Token;
import com.pcl.pojo.display.LoginResult;
import com.pcl.service.LoginService;

@RestController
@RequestMapping("/api")
public class LoginController {

	@Autowired
	private LoginService loginService;

	@Autowired
	HttpServletRequest request;
	
	@RequestMapping(value = "/api-jwt-auth",method = RequestMethod.POST)
	public LoginResult login(@RequestParam("username") String userName,@RequestParam("password") String password) throws LabelSystemException {
		LoginResult result = new LoginResult();
		try {
			Token token = loginService.login(userName, password,getRemoteIP(request));
			result.setCode(0);
			result.setToken(token.getToken());
			result.setUserName(token.getUserName());
			result.setUserType(token.getUserType());
			result.setNickName(token.getNickName());
		} catch (Exception e) {
			result.setCode(1);
			result.setMessage(e.getMessage());
		}
		return result;
	}
	
	@RequestMapping(value = "/api-jwt-loginout",method = RequestMethod.POST)
	public Result loginOut(@RequestParam("username") String userName,@RequestParam("token") String token) throws LabelSystemException {
		Result re = new Result();
		
		loginService.loginOut(token, userName,getRemoteIP(request));
		
		return re;
	}
	
	private String getRemoteIP(HttpServletRequest request) {
        String ip = "127.0.0.1";
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null) {
            //对于通过多个代理的情况，最后IP为客户端真实IP,多个IP按照','分割
            int position = ip.indexOf(",");
            if (position > 0) {
                ip = ip.substring(0, position);
            }
        }
        return ip;
    }
	
	
}
