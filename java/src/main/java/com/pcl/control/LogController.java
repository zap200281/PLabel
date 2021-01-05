package com.pcl.control;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.PageResult;
import com.pcl.service.LogSecService;

@RestController
@RequestMapping("/api")
public class LogController {

	private static Logger logger = LoggerFactory.getLogger(LogController.class);

	@Autowired
	HttpServletRequest request;
	
	@Autowired
	private LogSecService logService;

	
	@RequestMapping(value = "/queryLogSecInfo",method = RequestMethod.GET)
	public PageResult queryLogSecInfo(@RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize) throws LabelSystemException {
		
		String token = request.getHeader("authorization");
		logger.info("query log secure info. token=" + token  +" startPage=" +startPage + " pageSize=" + pageSize);
		return logService.queryLogSecInfo(token,startPage,pageSize);
	}
	
	
}
