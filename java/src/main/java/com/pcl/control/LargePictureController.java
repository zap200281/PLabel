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
import com.pcl.pojo.mybatis.LargePictureTask;
import com.pcl.service.LargePictureService;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api")
public class LargePictureController {

	private static Logger logger = LoggerFactory.getLogger(LargePictureController.class);

	@Autowired
	HttpServletRequest request;

	@Autowired
	private LargePictureService largePictureService;

	@ApiOperation(value="创建一个超大图标注任务", notes="创建一个超大图标注任务")
	@RequestMapping(value="/large-picture-task", method = RequestMethod.POST, produces ="application/json;charset=utf-8")
	public Result addLargePictureTask(@RequestBody LargePictureTask largePictureTask) {

		logger.info("addLargePictureTask, taskName =" + largePictureTask.getTask_name());

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");

			largePictureService.addLargePictureTask(token, largePictureTask);

			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;

	}

	@ApiOperation(value="查询一个超大图标注任务", notes="查询一个超大图标注任务")
	@RequestMapping(value="/large-picture-task", method = RequestMethod.GET, produces ="application/json;charset=utf-8")
	public LargePictureTask queryLargePictureTask(@RequestParam("id") String id) {

		logger.info("queryLargePictureTask, id =" + id);

		String token = request.getHeader("authorization");

		return largePictureService.queryLargePictureTask(token, id);
	}
	
	@ApiOperation(value="删除一个超大图标注任务", notes="删除一个超大图标注任务")
	@RequestMapping(value="/large-picture-task", method = RequestMethod.DELETE, produces ="application/json;charset=utf-8")
	public int deleteLargePictureTask(@RequestParam("id") String id) {

		logger.info("deleteVideoLabelTask, id =" + id);

		String token = request.getHeader("authorization");

		return largePictureService.deleteLargePictureTask(token, id);
	}
	
	@ApiOperation(value="更新一个超大图标注任务", notes="更新一个超大图标注任务")
	@RequestMapping(value="/large-picture-task", method = RequestMethod.PATCH, produces ="application/json;charset=utf-8")
	public Result updateLargePictureTask(@RequestParam("id") String id,@RequestParam("task_label_type_info") String taskLabelTypeInfo){
		logger.info("updateVideoLabelTask, id =" + id + ", taskLabelTypeInfo=" + taskLabelTypeInfo);

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			if(Strings.isNullOrEmpty(id)) {
				throw new LabelSystemException("标注任务ID不能为空。");
			}
		
			largePictureService.updateLargePictureTask(token, id, taskLabelTypeInfo);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	@ApiOperation(value="更新一个超大图标注任务状态", notes="更新一个超大图标注任务状态")
	@RequestMapping(value="/large-picture-task-status", method = RequestMethod.PATCH, produces ="application/json;charset=utf-8")
	public Result updateLargePictureTaskStatus(@RequestParam("id") String id,@RequestParam("task_status") int task_status){
		logger.info("updateLargePictureTaskStatus, id =" + id + ", task_status=" + task_status);

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			if(Strings.isNullOrEmpty(id)) {
				throw new LabelSystemException("标注任务ID不能为空。");
			}
		
			largePictureService.updateLargePictureTaskStatus(token, id, task_status);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
	@ApiOperation(value="分页查询超大图标注任务", notes="分页查询超大图标注任务")
	@RequestMapping(value="/large-picture-task-page", method = RequestMethod.GET, produces ="application/json;charset=utf-8")
	public PageResult queryLargePictureTaskPage(@RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize) {

		logger.info("queryLargePictureTaskPage startPage=" + startPage);

		String token = request.getHeader("authorization");

		return largePictureService.queryLargePictureTaskPage(token, startPage,pageSize);
	}
	
	
	@ApiOperation(value="分页查询超大图标注任务", notes="分页查询超大图标注任务")
	@RequestMapping(value="/large-picture-task-page-medical", method = RequestMethod.GET, produces ="application/json;charset=utf-8")
	public PageResult queryLargePictureTaskPage(@RequestParam("appid") String appid,@RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize) {

		logger.info("queryLargePictureTaskPage startPage=" + startPage + " appid=" + appid + " pageSize=" + pageSize);

		String token = request.getHeader("authorization");

		return largePictureService.queryLargePictureTaskPage(token, appid, startPage,pageSize);
	}


	
	@ApiOperation(value="修改一个超大图标注信息", notes="")
	@RequestMapping(value="/large-picture-task-item", method = RequestMethod.PATCH)
	public int updatePictureTaskLabelItem(@RequestBody LabelTaskItem body) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("updatePictureTaskLabelItem  token =" + token);

		return largePictureService.updatePictureTaskLabelItem(body,token);
	}
	
	@ApiOperation(value="查询超大图标注信息", notes="")
	@RequestMapping(value="/large-picture-task-item", method = RequestMethod.GET)
	public List<LabelTaskItem> queryPictureTaskLabelItemPage(@RequestParam("label_task") String label_task) throws LabelSystemException {
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryPictureTaskLabelItemPage  token =" + token + " label_task=" + label_task);

		return largePictureService.queryPictureTaskLabelItem(label_task);
	}
	

	
}
