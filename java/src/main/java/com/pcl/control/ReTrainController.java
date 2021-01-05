package com.pcl.control;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.pcl.pojo.body.RetrainTaskBody;
import com.pcl.pojo.display.DisplayRetrainResult;
import com.pcl.pojo.display.DisplayRetrainTask;
import com.pcl.service.RetrainTaskService;
import com.pcl.util.JsonUtil;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api")
public class ReTrainController {

	private static Logger logger = LoggerFactory.getLogger(ReTrainController.class);
	
	@Autowired
	private HttpServletRequest request;
	
	@Autowired
	private RetrainTaskService retrainTaskService;
	
	@SuppressWarnings("unchecked")
	@ApiOperation(value="查询所有的重训任务", notes="返回所有的重训任务")
	@RequestMapping(value="/retrain-task", method = RequestMethod.GET)
	public List<DisplayRetrainTask> queryRetrainTask() throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryRetrainTask  token =" + token);

		PageResult re = retrainTaskService.queryRetrainTask(token,0,10);
		
		return (List<DisplayRetrainTask>)re.getData();
		
	}
	
	@ApiOperation(value="分页查询所有的重训任务", notes="返回分页的重训任务")
	@RequestMapping(value="/retrain-task-page", method = RequestMethod.GET)
	public PageResult queryRetrainTaskPage(@RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryRetrainTaskPage  token =" + token);

		return retrainTaskService.queryRetrainTask(token,startPage,pageSize);
		
	}
	
	@ApiOperation(value="创建一个重训任务", notes="创建一个重训任务")
	@RequestMapping(value="/retrain-task", method = RequestMethod.POST, produces ="application/json;charset=utf-8")
	public Result addRetrainTask(@RequestBody RetrainTaskBody body){
	
		logger.info("addRetrainTask, body =" + JsonUtil.toJson(body));

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			retrainTaskService.addRetrainTask(token, body);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	

	
	@ApiOperation(value="删除一个重训任务", notes="删除一个重训任务")
	@RequestMapping(value="/retrain-task", method = RequestMethod.DELETE)
	public Result deleteRetrainTask(@RequestParam("retrain_task_id") String retrainTaskId){
	
		logger.info("delete RetrainTask, id =" + retrainTaskId);

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			retrainTaskService.deleteRetrainTaskId(token, retrainTaskId);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
	@ApiOperation(value="开始一个重训任务", notes="开始一个重训任务")
	@RequestMapping(value="/retrain-task-oper", method = RequestMethod.POST, produces ="application/json;charset=utf-8")
	public Result startRetrainTask(@RequestParam("retrain_task_id") String retrainTaskId){
	
		logger.info("start RetrainTask, id =" + retrainTaskId);

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			retrainTaskService.startRetrainTask(token, retrainTaskId);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
	@ApiOperation(value="重训任务消息接收接口", notes="接收重训任务过程中的消息")
	@RequestMapping(value="/re-train-result", method = RequestMethod.POST, produces ="application/json;charset=utf-8")
	public Result receiveRetrainTaskMessage(
			@RequestParam("id") String id,
			@RequestParam("username") String username,
			@RequestParam("net") String net,
			@RequestParam("dataset") String dataset,
			@RequestParam("epoch") String epoch,
			@RequestParam("step") String step,
			@RequestParam("loss") String loss,
			@RequestParam("lr") String lr
			){
	
		logger.info("received RetrainTask message, id =" + id + " epoch=" + epoch + " step=" + step + " loss=" + loss + " lr=");

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			Map<String,Object> paramMap = new HashMap<>();
			
			paramMap.put("id", id);
			paramMap.put("epoch_num", epoch);
			if(loss != null && loss.length() > 6) {
				loss = loss.substring(0,6);
			}
			paramMap.put("loss_train", loss);
			paramMap.put("lr", lr);
			paramMap.put("step_num", step);
	
			retrainTaskService.receiveRetrainTask(token, paramMap);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
	@ApiOperation(value="查询指定重训任务的消息", notes="查询指定重训任务的消息")
	@RequestMapping(value="/retrain-task-result", method = RequestMethod.GET)
	public DisplayRetrainResult queryRetrainTasResult(@RequestParam("retrain_task_id") String retrainTaskId) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryRetrainTasResult  retrainTaskId =" + retrainTaskId);

		DisplayRetrainResult re = retrainTaskService.queryRetrainTaskResult(token,retrainTaskId);
		
		return re;
		
	}
	

}
