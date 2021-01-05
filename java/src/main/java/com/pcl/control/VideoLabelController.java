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

import com.google.common.base.Strings;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.PageResult;
import com.pcl.pojo.Result;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.pojo.mybatis.VideoLabelTask;
import com.pcl.service.VideoLabelTaskService;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api")
public class VideoLabelController {

	private static Logger logger = LoggerFactory.getLogger(VideoLabelController.class);

	@Autowired
	HttpServletRequest request;

	@Autowired
	private VideoLabelTaskService videoLabelTaskService;

	@ApiOperation(value="创建一个视频标注任务", notes="创建一个视频标注任务")
	@RequestMapping(value="/video-label-task", method = RequestMethod.POST, produces ="application/json;charset=utf-8")
	public Result addVideoLabelTask(@RequestBody VideoLabelTask videoLabelTask) {

		logger.info("addVideoCountTask, taskName =" + videoLabelTask.getTask_name());

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");

			videoLabelTaskService.addVideoLabelTask(token, videoLabelTask);

			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;

	}

	@ApiOperation(value="查询一个视频标注任务", notes="查询一个视频流标注任务")
	@RequestMapping(value="/video-label-task", method = RequestMethod.GET, produces ="application/json;charset=utf-8")
	public VideoLabelTask queryVideoLabelTask(@RequestParam("id") String id) {

		logger.info("queryVideoLabelTask, id =" + id);

		String token = request.getHeader("authorization");

		return videoLabelTaskService.queryVideoLabelTask(token, id);
	}
	
	@ApiOperation(value="删除一个视频标注任务", notes="删除一个视频标注任务")
	@RequestMapping(value="/video-label-task", method = RequestMethod.DELETE, produces ="application/json;charset=utf-8")
	public int deleteVideoLabelTask(@RequestParam("id") String id) {

		logger.info("deleteVideoLabelTask, id =" + id);

		String token = request.getHeader("authorization");

		return videoLabelTaskService.deleteVideoLabelTask(token, id);
	}
	
	@ApiOperation(value="更新一个视频标注任务", notes="更新一个视频标注任务")
	@RequestMapping(value="/video-label-task", method = RequestMethod.PATCH, produces ="application/json;charset=utf-8")
	public Result updateVideoLabelTask(@RequestParam("id") String id,@RequestParam("task_label_type_info") String taskLabelTypeInfo){
		logger.info("updateVideoLabelTask, id =" + id + ", taskLabelTypeInfo=" + taskLabelTypeInfo);

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			if(Strings.isNullOrEmpty(id)) {
				throw new LabelSystemException("标注任务ID不能为空。");
			}
		
			videoLabelTaskService.updateVideoLabelTask(token, id, taskLabelTypeInfo);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	@ApiOperation(value="更新一个视频标注任务", notes="更新一个视频标注任务")
	@RequestMapping(value="/video-label-task-status", method = RequestMethod.PATCH, produces ="application/json;charset=utf-8")
	public Result updateVideoLabelTaskStatus(@RequestParam("id") String id,@RequestParam("task_status") int task_status){
		logger.info("updateVideoLabelTaskStatus, id =" + id + ", task_status=" + task_status);

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			if(Strings.isNullOrEmpty(id)) {
				throw new LabelSystemException("标注任务ID不能为空。");
			}
		
			videoLabelTaskService.updateVideoLabelTaskStatus(token, id, task_status);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
	@ApiOperation(value="分页查询视频标注任务", notes="分页查询视频标注任务")
	@RequestMapping(value="/video-label-task-page", method = RequestMethod.GET, produces ="application/json;charset=utf-8")
	public PageResult queryVideoLabelTaskPage(@RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize) {

		logger.info("queryVideoLabelTaskPage startPage=" + startPage);

		String token = request.getHeader("authorization");

		return videoLabelTaskService.queryVideoLabelTaskPage(token, startPage,pageSize);
	}
	
	
	@ApiOperation(value="增加一个标注信息", notes="")
	@RequestMapping(value="/video-label-task-item", method = RequestMethod.POST)
	public String addVideoLabelItem(@RequestBody LabelTaskItem body) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("addVideoLabelItem  token =" + token + " label_task_id=" + body.getLabel_task_id() + " time=" + body.getPic_url() + " width,height=" + body.getPic_object_name());

		return videoLabelTaskService.addLabelTaskItem(body,token);
	}
	
	@ApiOperation(value="删除一个视频标注信息", notes="")
	@RequestMapping(value="/video-label-task-item", method = RequestMethod.DELETE)
	public int deleteVideoLabelItem(@RequestParam("id") String id) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("deleteVideoLabelItem  token =" + token);

		return videoLabelTaskService.deleteLabelTaskItem(id,token);
	}
	
	@ApiOperation(value="修改一个视频标注信息", notes="")
	@RequestMapping(value="/video-label-task-item", method = RequestMethod.PATCH)
	public int updateVideoLabelItem(@RequestBody LabelTaskItem body) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("updateVideoLabelItem  token =" + token);

		return videoLabelTaskService.updateLabelTaskItem(body,token);
	}
	
	@ApiOperation(value="分页查询视频标注信息", notes="")
	@RequestMapping(value="/video-label-task-item", method = RequestMethod.GET)
	public PageResult queryVideoLabelTaskItemPage(@RequestParam("label_task") String label_task,@RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize,@RequestParam(value="orderBy",required=false, defaultValue="0") int orderBy) throws LabelSystemException {
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryVideoLabelTaskItemPage  token =" + token + " label_task=" + label_task + " startPage=" + startPage + " orderBy=" + orderBy);

		return videoLabelTaskService.queryVideoLabelTaskItemPage(label_task, startPage, pageSize,orderBy);
	}
	
	@ApiOperation(value="根据时间查询视频标注信息", notes="")
	@RequestMapping(value="/video-label-task-item-bytime", method = RequestMethod.GET)
	public LabelTaskItem queryVideoLabelTaskItemByTime(@RequestParam("label_task") String label_task,@RequestParam("time") String time) throws LabelSystemException {
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryVideoLabelTaskItemPage  token =" + token + " label_task=" + label_task + " time=" + time);

		return videoLabelTaskService.queryVideoLabelTaskItemByTime(label_task, time);
	}
	
}
