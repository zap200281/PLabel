package com.pcl.control;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.PageResult;
import com.pcl.pojo.Result;
import com.pcl.pojo.body.LabelTaskBody;
import com.pcl.pojo.display.DisplayLabelTask;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.service.LabelTaskService;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api")
public class LabelTaskController {

	private static Logger logger = LoggerFactory.getLogger(LabelTaskController.class);

	@Autowired
	HttpServletRequest request;
	
	@Autowired
	private LabelTaskService labelTaskService;
	
	
	@SuppressWarnings("unchecked")
	@ApiOperation(value="查询所有的人工标注任务", notes="返回所有的人工标注任务")
	@RequestMapping(value="/label-task", method = RequestMethod.GET)
	public List<DisplayLabelTask> queryLabelTask() throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryLabelTask  token =" + token);

		PageResult re = labelTaskService.queryLabelTask(token,0,1000);
		
		return (List<DisplayLabelTask>)re.getData();
		
	}
	
	
	
	@ApiOperation(value="删除指定的人工标注任务", notes="删除指定的人工标注任务")
	@RequestMapping(value="/label-task", method = RequestMethod.DELETE)
	public Result deleteLabelTaskById(@RequestParam("label_task_id") String labelTaskId) throws LabelSystemException{
		
	
		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			if(token == null) {
				throw new LabelSystemException("user not login.");
			}
			logger.info("delete LabelTask, id =" + labelTaskId + " token=" + token);
			labelTaskService.deleteLabelTaskById(token, labelTaskId);
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
	@ApiOperation(value="查询所有的人工标注任务", notes="返回所有的人工标注任务")
	@RequestMapping(value="/label-task-page", method = RequestMethod.GET)
	public PageResult queryLabelTaskPage(@RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryLabelTaskPage  token =" + token);

		return labelTaskService.queryLabelTask(token,startPage,pageSize);
		
	}
	
	@ApiOperation(value="查询所有的人工标注任务", notes="返回所有的人工标注任务")
	@RequestMapping(value="/medical-label-task-page", method = RequestMethod.GET)
	public PageResult queryLabelTaskPageForMedical(@RequestParam("appid") String appid, @RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryLabelTaskPageForMedical  token =" + token + " startPage=" + startPage + " pageSize=" + pageSize);

		return labelTaskService.queryLabelTaskPageForMedical(token,appid,startPage,pageSize);
		
	}
	
	
	
	@ApiOperation(value="查询指定的人工标注任务", notes="返回指定的人工标注任务")
	@RequestMapping(value="/label-task/{id}", method = RequestMethod.GET)
	public DisplayLabelTask queryLabelTaskById(@PathVariable String id) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryLabelTaskById  token =" + token);

		return labelTaskService.queryLabelTaskById(token,id);
		
	}
	
	
	@ApiOperation(value="根据数据集ID查询已经创建的标注任务列表", notes="返回指定的人工标注任务")
	@RequestMapping(value="/label-related-task/{datasetid}", method = RequestMethod.GET)
	public List<DisplayLabelTask> queryLabelTaskByRelatedDataSetId(@PathVariable String datasetid) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryLabelTaskById  token =" + token);

		return labelTaskService.queryLabelTaskByRelatedDataSetId(token, datasetid);
		
	}
	
	@ApiOperation(value="更新一个人工标注任务", notes="更新一个人工标注任务")
	@RequestMapping(value="/label-task-status", method = RequestMethod.PATCH, produces ="application/json;charset=utf-8")
	public Result updateLabelTaskStatus(@RequestParam("label_task_id") String labelTaskId,@RequestParam(value="verify_user_id",required=false, defaultValue="-1") int verifyUserId, @RequestParam("task_status") int taskStatus){
		logger.info("updateLabelTask, label_task_id =" + labelTaskId + ", verifyUserId=" + verifyUserId + ", taskStatus=" + taskStatus);

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			if(Strings.isNullOrEmpty(labelTaskId)) {
				throw new LabelSystemException("标注任务ID不能为空。");
			}
		
			labelTaskService.updateLabelTaskStatus(token, labelTaskId, verifyUserId,taskStatus);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
	@ApiOperation(value="更新一个人工标注任务", notes="更新一个人工标注任务")
	@RequestMapping(value="/label-task", method = RequestMethod.PATCH, produces ="application/json;charset=utf-8")
	public Result updateLabelTask(@RequestParam("label_task_id") String labelTaskId,@RequestParam("task_label_type_info") String taskLabelTypeInfo){
		logger.info("updateLabelTask, label_task_id =" + labelTaskId + ", taskLabelTypeInfo=" + taskLabelTypeInfo);

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			if(Strings.isNullOrEmpty(labelTaskId)) {
				throw new LabelSystemException("标注任务ID不能为空。");
			}
		
			labelTaskService.updateLabelTask(token, labelTaskId, taskLabelTypeInfo);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	@ApiOperation(value="创建一个人工标注任务", notes="创建一个人工标注任务")
	@RequestMapping(value="/label-task", method = RequestMethod.POST, produces ="application/json;charset=utf-8")
	public Result addLabelTask(@RequestBody LabelTaskBody body){
		logger.info("addLabelTask, body =" + body.getTaskName());

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			if(Strings.isNullOrEmpty(body.getRelateTaskId())) {
				throw new LabelSystemException("人工标注参数错误，要么关联自动标注任务，要么选择原始数据集。");
			}
		
			labelTaskService.addLabelTask(token, body);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	@ApiOperation(value="创建一个人工标注任务，图片路径为OBS上的一个目录", notes="创建一个人工标注任务，图片路径为OBS上的一个目录")
	@RequestMapping(value="/label-task-from-obs", method = RequestMethod.POST, produces ="application/json;charset=utf-8")
	public Result addLabelTaskFromObs(@RequestBody LabelTaskBody body,@RequestParam("obspath") String obspath){
		logger.info("addLabelTask, body =" + body.getTaskName());

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			if(token == null) {
				throw new LabelSystemException("user not login.");
			}
			labelTaskService.addLabelTaskFromObs(token, body, obspath);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
//	@ApiOperation(value="根据指定标注任务查询该任务所有要标注人工标图片信息", notes="返回所有的人工标注任务")
//	@RequestMapping(value="/label-task-item", method = RequestMethod.GET)
//	public List<LabelTaskItem> queryLabelItemByTaskId(String label_task) throws LabelSystemException{
//		
//		String token = request.getHeader("authorization");
//		if(token == null) {
//			throw new LabelSystemException("user not login.");
//		}
//		logger.info("queryLabelItemByTaskId  token =" + token);
//
//		return labelTaskService.queryLabelTaskItemByLabelTaskId(token,label_task);
//	}
	
	
	@ApiOperation(value="根据指定标注任务查询该任务所有要标注人工标图片信息", notes="返回所有的人工标注任务")
	@RequestMapping(value="/label-task-item-pic", method = RequestMethod.GET)
	public List<LabelTaskItem> queryLabelItemByTaskId(String label_task,String picImageListStr) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryLabelItemByTaskId  token =" + token);

		return labelTaskService.queryLabelTaskItemByLabelTaskIdAndPicImage(token,label_task, picImageListStr);
	}
	
	
	
	@ApiOperation(value="分页查询所有待标注的图片任务", notes="分页查询所有待标注的图片任务")
	@RequestMapping(value="/label-task-item-page", method = RequestMethod.GET)
	public PageResult queryLabelItemPageByTaskId(@RequestParam("label_task") String label_task,@RequestParam("startPage") Integer startPage, 
			@RequestParam("pageSize") Integer pageSize,
			@RequestParam(value="orderType",required=false, defaultValue="0") int orderType,
			@RequestParam(value="findLast",required=false, defaultValue="0") int findLast) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryLabelItemPageByTaskId  token =" + token + " orderType=" + orderType + " findLast=" +findLast + " startPage=" + startPage);

		
		return labelTaskService.queryLabelTaskItemPageByLabelTaskId(label_task, startPage, pageSize, orderType,findLast,token);
	}
	
	@ApiOperation(value="查询标注框数量", notes="返回查询标注框数量")
	@RequestMapping(value="/label-count", method = RequestMethod.GET)
	public void queryLabelCount() throws LabelSystemException {
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryLabelCount  token =" + token);
		
		labelTaskService.queryLabelCount(token);
	}
	
	
	
	@ApiOperation(value="更新通用图片标注信息", notes="")
	@RequestMapping(value="/label-task-item", method = RequestMethod.PATCH)
	public int updateLabelItem(@RequestBody LabelTaskItem body) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("updateLabelItem  token =" + token);

		return labelTaskService.updateLabelTaskItem(body,token);
	}
	
	@ApiOperation(value="更新通用图片标注信息", notes="")
	@RequestMapping(value="/label-task-item-status", method = RequestMethod.PATCH)
	public int updateLabelItemStatus(@RequestBody LabelTaskItem body) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("updateLabelItem  token =" + token);

		return labelTaskService.updateLabelTaskItemStatus(body,token);
	}
	
	@ApiOperation(value="更新通用图片标注信息", notes="")
	@RequestMapping(value="/label-task-item-verify-status", method = RequestMethod.PATCH)
	public int updateLabelItemVerifyStatus(@RequestBody LabelTaskItem body) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("updateLabelItem  token =" + token);

		return labelTaskService.updateLabelItemVerifyStatus(body,token);
	}
	
	@ApiOperation(value="更新CT影像人工标注信息", notes="")
	@RequestMapping(value="/label-task-dcm-item", method = RequestMethod.PATCH)
	public int updateLabelDcmItem(@RequestBody LabelTaskItem body) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("updateLabelDcmItem  token =" + token);
		
		return labelTaskService.updateLabelDcmTaskItem(body,token);
	}
	
	
	@ApiOperation(value="根据指定标注任务查询该任务所有要标注人工标图片信息", notes="返回所有的人工标注任务")
	@RequestMapping(value="/label-task-delete-label", method = RequestMethod.POST)
	public Result deleteLabel(@RequestParam("label_task_id") String labelTaskId,@RequestParam("start_id") Integer startId, @RequestParam("end_id") Integer endId, @RequestParam(value="one_name",required=false, defaultValue="")  String one_name) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("deleteLabel  token =" + token + "labelTaskId=" + labelTaskId + " start_id=" + startId + " endId=" + endId + " one_name=" + one_name);
		Result re = new Result();
		try {
			labelTaskService.deleteLabel(labelTaskId, startId, endId,one_name,token);
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
}
