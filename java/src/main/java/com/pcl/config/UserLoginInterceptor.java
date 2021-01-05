package com.pcl.config;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.pcl.service.TokenManager;

public class UserLoginInterceptor implements  HandlerInterceptor {

	private static Logger logger = LoggerFactory.getLogger(UserLoginInterceptor.class);

	
	
	/**
	 * 在请求处理之前进行调用（Controller方法调用之前）
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		//        System.out.println("执行了TestInterceptor的preHandle方法");
		try {
			//统一拦截（查询当前session是否存在user）(这里user会在每次登陆成功后，写入session)
			String token = request.getHeader("authorization");
			if(token == null ) {
				logger.info("1 redirect login.html request url:" + request.getRequestURI());
				reDirect(request, response);
				//response.sendRedirect("http://127.0.0.1/login.html");
			}else {
				if(TokenManager.getUserIdByToken(TokenManager.getServerToken(token)) != -1) {
					return true;
				}else {
					
					logger.info("2 redirect login.html request url:" + request.getRequestURI() + " token=" + TokenManager.getServerToken(token));
					reDirect(request, response);
					//response.sendRedirect("http://127.0.0.1/login.html");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;//如果设置为false时，被请求时，拦截器执行到此处将不会继续操作
		//如果设置为true时，请求将会继续执行后面的操作
		//return true;
	}

	public void reDirect(HttpServletRequest request, HttpServletResponse response) throws IOException{
        //获取当前请求的路径
        String basePath = request.getScheme() + "://" + request.getServerName() + ":"  + request.getServerPort()+request.getContextPath();
        //如果request.getHeader("X-Requested-With") 返回的是"XMLHttpRequest"说明就是ajax请求，需要特殊处理
        if("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))){
            //告诉ajax我是重定向
            response.setHeader("REDIRECT", "REDIRECT");
            //告诉ajax我重定向的路径
            //logger.info(basePath+"/login.html");
            response.setHeader("CONTENTPATH", "http://127.0.0.1/login.html");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }else{
            response.sendRedirect(basePath + "/login.html");
        }
    }
	
	/**
	 * 请求处理之后进行调用，但是在视图被渲染之前（Controller方法调用之后）
	 */
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
		//         System.out.println("执行了TestInterceptor的postHandle方法");
	}

	/**
	 * 在整个请求结束之后被调用，也就是在DispatcherServlet 渲染了对应的视图之后执行（主要是用于进行资源清理工作）
	 */
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		//        System.out.println("执行了TestInterceptor的afterCompletion方法");
	}

}
