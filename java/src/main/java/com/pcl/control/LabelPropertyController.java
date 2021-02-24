package com.pcl.control;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.pcl.pojo.Result;
import com.pcl.service.LabelPropertyService;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api")
public class LabelPropertyController {
	
	private static Logger logger = LoggerFactory.getLogger(LabelPropertyController.class);
	
	@Autowired
	private LabelPropertyService  labelPropertyService;

	@Autowired
	private HttpServletResponse response; 
	
	@Autowired
	HttpServletRequest request;

	@ResponseBody
	@ApiOperation(value="导入指定任务的标注属性", notes="返回导入结果")
	@RequestMapping(value ="/task-import-label-property", method = RequestMethod.POST)
	public Result importLabelPropertyJson(@RequestParam("jsonContent") String jsonContent,@RequestParam("taskType") String taskType,@RequestParam("taskId") String taskId) {
		
		String token = request.getHeader("authorization");
		logger.info("importLabelPropertyJson, jsonContent =" + jsonContent + " taskType=" + taskType + " taskId=" + taskId);
		return labelPropertyService.importLabelPropertyJson(token,jsonContent,taskType,taskId);
	}
	
	@ApiOperation(value="根据指定标注任务查询该任务导出该标注任务设置的属性", notes="返回标注属性文件流")
	@RequestMapping(value="/task-export-label-property", method = RequestMethod.GET)
	public void exportLabelProperty(@RequestParam("task_id") String taskId,@RequestParam("type") String type) {
		String token = request.getHeader("authorization");
		logger.info("queryLabelProperty, taskId =" + taskId + " type=" + type);
		labelPropertyService.queryLabelProperty(token,taskId,type,response);
		
	}
}
