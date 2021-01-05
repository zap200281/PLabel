package com.pcl.control;

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
import com.pcl.pojo.Progress;
import com.pcl.pojo.Result;
import com.pcl.pojo.body.AutoLabelBody;
import com.pcl.service.AutoLabelService;
import com.pcl.util.JsonUtil;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api")
public class AutoLabelController {

	private static Logger logger = LoggerFactory.getLogger(AutoLabelController.class);

	@Autowired
	HttpServletRequest request;
	
	@Autowired
	private AutoLabelService autoLabelService;
	
	@ApiOperation(value="自动标注一张图片", notes="自动标注一张图片")
	@RequestMapping(value="/auto-label-task", method = RequestMethod.POST, produces ="application/json;charset=utf-8")
	public Result autoLabelCellTask(@RequestBody AutoLabelBody body) throws LabelSystemException {
		
		logger.info("autoLabelCellTask, body =" +JsonUtil.toJson(body));
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryLabelTask  token =" + token);
		
		Result re = new Result();
		try {

			autoLabelService.autoLabelTask(token,body);
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	@ApiOperation(value="自动标注一张图片", notes="自动标注一张图片")
	@RequestMapping(value="/query-auto-label-task-progress", method = RequestMethod.GET)
	public Progress queryAutoLabelProgress(@RequestParam("taskId") String taskId) {
		return autoLabelService.queryLabelTask(taskId);
	}
	
}
