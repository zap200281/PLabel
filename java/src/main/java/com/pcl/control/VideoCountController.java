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

import com.google.common.base.Strings;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.PageResult;
import com.pcl.pojo.Result;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.pojo.mybatis.VideoCountTask;
import com.pcl.service.VideoCountTaskService;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api")
public class VideoCountController {

	private static Logger logger = LoggerFactory.getLogger(VideoCountController.class);

	@Autowired
	HttpServletRequest request;

	@Autowired
	private VideoCountTaskService videoCountTaskService;

	@ApiOperation(value="创建一个视频流统计任务", notes="创建一个视频流统计标注任务")
	@RequestMapping(value="/video-count-task", method = RequestMethod.POST, produces ="application/json;charset=utf-8")
	public Result addVideoCountTask(@RequestBody VideoCountTask videoCountTask) {

		logger.info("addVideoCountTask, taskName =" + videoCountTask.getTask_name());

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");

			videoCountTaskService.addVideoCountTask(token, videoCountTask);

			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;

	}

	@ApiOperation(value="查询一个视频流统计任务", notes="查询一个视频流统计标注任务")
	@RequestMapping(value="/video-count-task", method = RequestMethod.GET, produces ="application/json;charset=utf-8")
	public VideoCountTask queryVideoCountTask(@RequestParam("id") String id) {

		logger.info("queryVideoCountTask, id =" + id);

		String token = request.getHeader("authorization");

		return videoCountTaskService.queryVideoCountTask(token, id);
	}
	
	@ApiOperation(value="查询一个视频流统计任务", notes="查询一个视频流统计标注任务")
	@RequestMapping(value="/video-count-task", method = RequestMethod.DELETE, produces ="application/json;charset=utf-8")
	public int deleteVideoCountTask(@RequestParam("id") String id) {

		logger.info("deleteVideoCountTask, id =" + id);

		String token = request.getHeader("authorization");

		return videoCountTaskService.deleteVideoCountTask(token, id);
	}
	
	@ApiOperation(value="分页查询视频流统计任务", notes="分页查询视频流统计标注任务")
	@RequestMapping(value="/video-count-task-page", method = RequestMethod.GET, produces ="application/json;charset=utf-8")
	public PageResult queryVideoCountTaskPage(@RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize) {

		logger.info("queryVideoCountTaskPage startPage=" + startPage);

		String token = request.getHeader("authorization");

		return videoCountTaskService.queryVideoCountTaskPage(token, startPage,pageSize);
	}
	
	
	@ApiOperation(value="增加一个标注信息", notes="")
	@RequestMapping(value="/video-count-task-item", method = RequestMethod.POST)
	public int addVideoLabelItem(@RequestBody LabelTaskItem body) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("addVideoLabelItem  token =" + token);

		return videoCountTaskService.addLabelTaskItem(body,token);
	}
	
	@ApiOperation(value="删除一个标注信息", notes="")
	@RequestMapping(value="/video-count-task-item", method = RequestMethod.DELETE)
	public int deleteVideoLabelItem(@RequestParam("id") String id) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("deleteVideoLabelItem  token =" + token);

		return videoCountTaskService.deleteLabelTaskItem(id,token);
	}
	
	@ApiOperation(value="增加一个标注信息", notes="")
	@RequestMapping(value="/video-count-task-item", method = RequestMethod.PATCH)
	public int updateVideoLabelItem(@RequestBody LabelTaskItem body) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("updateVideoLabelItem  token =" + token);

		return videoCountTaskService.updateLabelTaskItem(body,token);
	}
	
	@ApiOperation(value="分页查询视频流标注信息", notes="")
	@RequestMapping(value="/video-count-task-item", method = RequestMethod.GET)
	public PageResult queryVideoCountTaskItemPage(@RequestParam("label_task") String label_task,@RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize) throws LabelSystemException {
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryVideoCountTaskItemPage  token =" + token + " label_task=" + label_task + " startPage=" + startPage);

		return videoCountTaskService.queryVideoCountTaskItemPage(label_task, startPage, pageSize);
	}
	
	@ApiOperation(value="根据轨迹查询所有视频流标注信息", notes="")
	@RequestMapping(value="/video-count-task-locus", method = RequestMethod.GET)
	public List<LabelTaskItem> queryVideoCountTaskItemByLocus(@RequestParam("label_task") String label_task,@RequestParam("locus") String locus) throws LabelSystemException {
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryVideoCountTaskItemByLocus  token =" + token + " label_task=" + label_task + " locus=" + locus);

		return videoCountTaskService.queryVideoCountTaskItemByLocus(label_task, locus);
	}
	
	
	@ApiOperation(value="更新一个视频标注任务", notes="更新一个视频标注任务")
	@RequestMapping(value="/video-count-task-status", method = RequestMethod.PATCH, produces ="application/json;charset=utf-8")
	public Result updateVideoCountLabelTaskStatus(@RequestParam("id") String id,@RequestParam("task_status") int task_status){
		logger.info("updateVideoCountLabelTaskStatus, id =" + id + ", task_status=" + task_status);

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			if(Strings.isNullOrEmpty(id)) {
				throw new LabelSystemException("标注任务ID不能为空。");
			}
		
			videoCountTaskService.updateVideoCountLabelTaskStatus(token, id, task_status);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}

}
