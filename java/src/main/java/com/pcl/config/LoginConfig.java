package com.pcl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LoginConfig implements WebMvcConfigurer {
    
	@Value("${server.closeInterceptor:0}")
	private int closeInterceptor;
	
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
    	if(closeInterceptor == 0) {
    		//注册TestInterceptor拦截器
    		InterceptorRegistration registration = registry.addInterceptor(new UserLoginInterceptor());
    		registration.addPathPatterns("/**");                      //所有路径都被拦截
    		registration.excludePathPatterns("/**/api-jwt-auth**");

    		registration.excludePathPatterns("/minio/**"); 

    		registration.excludePathPatterns("/dcm/**"); 
    		registration.excludePathPatterns("/static/**"); 
    		registration.excludePathPatterns("/api/getdziimage/**"); 
    		registration.excludePathPatterns("/api/message/**"); 
    		registration.excludePathPatterns("/api/svsmessage/**"); 
    		registration.excludePathPatterns("/api/label-file-download/**"); 
    	}
        
    }
}