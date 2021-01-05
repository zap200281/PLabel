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
import com.pcl.pojo.body.PrePredictTaskBody;
import com.pcl.pojo.display.DisplayPrePredictTask;
import com.pcl.pojo.display.DisplaySimplePrePredictTask;
import com.pcl.service.PrePredictTaskService;
import com.pcl.util.JsonUtil;

@RestController
@RequestMapping("/api")
public class PrePredictTaskController {

	private static Logger logger = LoggerFactory.getLogger(PrePredictTaskController.class);

	@Autowired
	HttpServletRequest request;

	@Autowired
	private PrePredictTaskService predictTaskService;
	
	@RequestMapping(value = "/pre-predict-task-page",method = RequestMethod.GET)
	public PageResult queryPrePredictTaskPage(@RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize) throws LabelSystemException {
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryPrePredictTask  token =" + token + ", startPage=" + startPage + " pageSize=" + pageSize);

		return  predictTaskService.selectPredictTask(token,startPage,pageSize);
	}
	
	@RequestMapping(value = "/pre-predict-task-item-page",method = RequestMethod.GET)
	public PageResult queryPrePredictTaskResultItemPage(@RequestParam("predictTaskId") String predictTaskId, @RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize) throws LabelSystemException {
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryPrePredictTaskResultItemPage  token =" + token + " predictTaskId=" + predictTaskId +  ", startPage=" + startPage + " pageSize=" + pageSize);

		return  predictTaskService.queryPredictTaskItemByTaskId(token,predictTaskId, startPage, pageSize);
	}
	
	
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/pre-predict-task",method = RequestMethod.GET)
	public List<DisplayPrePredictTask> queryPrePredictTask() throws LabelSystemException {
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryPrePredictTask  token =" + token);

		PageResult re = predictTaskService.selectPredictTask(token,0,10);
		
		return (List<DisplayPrePredictTask>)re.getData();
	}
	

	@RequestMapping(value = "/pre-predict-taskforLabel",method = RequestMethod.GET)
	public List<DisplaySimplePrePredictTask> queryPrePredictTaskForLabel() throws LabelSystemException {
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryPrePredictTask  token =" + token);

		return predictTaskService.queryAllPredictTask(token);
	}


	@RequestMapping(value = "/pre-predict-task",method = RequestMethod.POST, produces ="application/json;charset=utf-8")
	public Result addPrePredictTask(@RequestBody PrePredictTaskBody body) {
		logger.info("addPrePredictTask, body =" + JsonUtil.toJson(body));

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			predictTaskService.addPrePredictTask(token, body);
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	@RequestMapping(value = "/pre-predict-task",method = RequestMethod.DELETE)
	public Result deletePrePredictTask(@RequestParam("predict_task_id") String predictTaskId) {
		logger.info("delete PrePredictTask, id =" + predictTaskId);

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			predictTaskService.deletePrePredictTaskById(token, predictTaskId);
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}


}
