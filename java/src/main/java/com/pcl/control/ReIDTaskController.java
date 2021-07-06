package com.pcl.control;

import java.util.List;
import java.util.Map;

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
import com.pcl.pojo.display.DisplayReIDTask;
import com.pcl.pojo.display.DisplayReIdTaskResult;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.pojo.mybatis.ReIDTask;
import com.pcl.service.ReIdTaskService;
import com.pcl.util.JsonUtil;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api")
public class ReIDTaskController {

	private static Logger logger = LoggerFactory.getLogger(ReIDTaskController.class);

	@Autowired
	HttpServletRequest request;
	
	@Autowired
	private ReIdTaskService reIdTaskService;
	

	@ApiOperation(value="删除指定的行人再识别标注任务", notes="删除指定的行人再识别标注任务")
	@RequestMapping(value="/reId-task", method = RequestMethod.DELETE)
	public Result deleteReIdTaskById(@RequestParam("reid_task_id") String reIDTaskId) throws LabelSystemException{
		
		logger.info("deleteReIdTaskById, id =" + reIDTaskId);

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			reIdTaskService.deleteReIDTask(token,reIDTaskId);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
	
	@ApiOperation(value="分页查询所有的ReID标注任务", notes="返回所有的ReID标注任务")
	@RequestMapping(value="/reId-task-page", method = RequestMethod.GET)
	public PageResult queryReIdTaskPage(@RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryReIdTaskPage  token =" + token);


		return reIdTaskService.queryReIDTask(token,startPage,pageSize);
		
	}
	
	
	@ApiOperation(value="查询当前用户所有的ReID标注任务", notes="返回所有的ReID标注任务")
	@RequestMapping(value="/reId-task-page-user", method = RequestMethod.GET)
	public List<ReIDTask> queryReIdTaskbyUser() throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryReIdTaskPage  token =" + token);


		return reIdTaskService.queryReIdTaskbyUser(token);
		
	}
	
	
	@ApiOperation(value="查询ReID任务所关联的任务ID及名称", notes="ReID任务所关联的任务ID及名称")
	@RequestMapping(value="/reId-related-taskname", method = RequestMethod.GET)
	public Map<String,String> queryReIdRelateTaskName(@RequestParam("reid_task_id") String reTaskId) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryReIdRelateTaskName  token =" + token);

		return reIdTaskService.queryReIdTaskRelatedNameInfo(reTaskId);		
	}
	
	
	
	@ApiOperation(value="查询与标注框相似的所有标注框信息", notes="返回所有与标注框相似的所有标注框信息")
	@RequestMapping(value="/reId-dest-imgs", method = RequestMethod.GET)
	public List<DisplayReIdTaskResult> queryReIdDestImagePage(@RequestParam("reid_task_id") String reTaskId, @RequestParam("pic_image_field") String pic_image_field, @RequestParam("labelId") String labelId) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryReIdDestImagePage  token =" + token +" reid_task_id=" + reTaskId + " pic_image_field=" +pic_image_field + " labelId=" + labelId);

		return reIdTaskService.queryReIdDestImagePage(reTaskId,pic_image_field,labelId);		
	}
	
	
	@ApiOperation(value="根据时间算法查询与标注框相似的所有标注框信息", notes="根据时间算法查询与标注框相似的所有标注框信息")
	@RequestMapping(value="/reId-near-imgs", method = RequestMethod.GET)
	public Map<String,String> queryReIdNearImagePage(@RequestParam("reid_task_id") String reTaskId, @RequestParam("pic_image_field") String pic_image_field, @RequestParam("intervalTime") long intervalTime) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryReIdNearImagePage  token =" + token +" reid_task_id=" + reTaskId + " pic_image_field=" +pic_image_field + " intervalTime=" + intervalTime);

		return reIdTaskService.queryNearReID(token, reTaskId, pic_image_field, intervalTime);
	}
	
	@ApiOperation(value="复制标注信息到指定图片中", notes="")
	@RequestMapping(value="/reId-label-task-copy-item", method = RequestMethod.PATCH)
	public int copyReIDLabelToOhterItem(@RequestParam("reid_task_id") String reTaskId,@RequestParam("label_info") String label_info, @RequestParam("destFileId") String destFileId) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("copyReIDLabelToOhterItem  token =" + token + " destFileId=" + destFileId + " label_info=" + label_info);

		return reIdTaskService.copyReIDLabelToOhterItem(label_info,destFileId,token,reTaskId);
	}
	
	
	@ApiOperation(value="更新通用图片标注信息", notes="")
	@RequestMapping(value="/reId-label-task-item", method = RequestMethod.PATCH)
	public int updateReIDLabelTaskItem(@RequestBody LabelTaskItem body) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("updateReIDLabelTaskItem  token =" + token);

		return reIdTaskService.updateReIDLabelTaskItem(body,token);
	}
	
	@ApiOperation(value="查询指定的ReID标注任务", notes="返回指定的人工标注任务")
	@RequestMapping(value="/reId-task/{id}", method = RequestMethod.GET)
	public DisplayReIDTask queryReIDTaskById(@PathVariable String id) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryReIDTaskById  token =" + token);

		return reIdTaskService.queryReIDTaskById(token,id);
		
	}
	
	
	@ApiOperation(value="创建一个ReID标注任务", notes="创建一个ReID标注任务")
	@RequestMapping(value="/reId-task", method = RequestMethod.POST, produces ="application/json;charset=utf-8")
	public Result addReIdTask(@RequestBody ReIDTask body){
		logger.info("addReIdTask, body =" + JsonUtil.toJson(body));

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			if(Strings.isNullOrEmpty(body.getSrc_predict_taskid()) || Strings.isNullOrEmpty(body.getDest_predict_taskid())) {
				throw new LabelSystemException("行人再识别标注任务参数错误，源数据集ID与目标数据集ID都不不能为空。");
			}
		
			reIdTaskService.addReIdTask(token, body);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
	@ApiOperation(value="对ReID结果进行重新分类", notes="对ReID结果进行重新分类")
	@RequestMapping(value="/reId-result-auto-sort", method = RequestMethod.POST, produces ="application/json;charset=utf-8")
	public Result addReIdResultAutoSort(@RequestParam("reid_task_id") String reTaskId){
		logger.info("addReIdResultAutoSort, reTaskId =" + reTaskId);

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
		
			reIdTaskService.addReIdResultAutoSort(token, reTaskId);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	@ApiOperation(value="更新ReId到所有数据库", notes="更新ReId到所有数据库")
	@RequestMapping(value="/reId-task-update-reId", method = RequestMethod.POST, produces ="application/json;charset=utf-8")
	public Result updateReIDInfo(@RequestParam("reid_task_id") String reTaskId, @RequestParam("imageList") String imageListInfo, @RequestParam("reId")  String reId) {
		
		logger.info("updateReIdInfo, reTaskId =" + reTaskId + " reId=" + reId + " imageListInfo=" + imageListInfo);

		Result re = new Result();
		try {
			
			String token = request.getHeader("authorization");
			
			reIdTaskService.updateReIDInfo(token,reTaskId,imageListInfo,reId);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	

	@ApiOperation(value="根据指定标注任务查询该任务所有要标注人工标图片信息", notes="返回所有的人工标注任务")
	@RequestMapping(value="/reid-task-item", method = RequestMethod.GET)
	public PageResult queryReIdTaskItemByTaskId(@RequestParam("reid_task_id") String reIdTaskId,@RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize,@RequestParam(value="orderType",required=false, defaultValue="0") int orderType,
			@RequestParam(value="findLast",required=false, defaultValue="0") int findLast) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryReIdTaskItemByTaskId  token =" + token + "reid_task_id=" + reIdTaskId + " startPage=" +startPage + " findLast=" + findLast);

		return reIdTaskService.queryReIdTaskItemPageByLabelTaskId(reIdTaskId, startPage, pageSize,null,orderType,findLast,token);
	}
	
	@ApiOperation(value="根据指定标注任务查询该任务所有要标注人工标图片信息", notes="返回所有的人工标注任务")
	@RequestMapping(value="/reid-task-item-a-task", method = RequestMethod.GET)
	public PageResult queryReIdTaskItemByTaskIdAndRelatedTaskId(@RequestParam("reid_task_id") String reIdTaskId,@RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize,@RequestParam("related_task_id") String related_task_id,
			@RequestParam(value="orderType",required=false, defaultValue="0") int orderType,
			@RequestParam(value="findLast",required=false, defaultValue="0") int findLast) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryReIdTaskItemByTaskIdAndRelatedTaskId  token =" + token + "reid_task_id=" + reIdTaskId + " related_task_id=" + related_task_id +" startPage=" +startPage + " findLast=" + findLast);

		return reIdTaskService.queryReIdTaskItemPageByLabelTaskId(reIdTaskId, startPage, pageSize,related_task_id,orderType,findLast,token);
	}
	
	
	@ApiOperation(value="更新一个人工标注任务", notes="更新一个人工标注任务")
	@RequestMapping(value="/reId-task", method = RequestMethod.PATCH, produces ="application/json;charset=utf-8")
	public Result updateReIdTask(@RequestParam("reid_task_id") String reIdTaskId,@RequestParam("task_label_type_info") String taskLabelTypeInfo){
		logger.info("updateLabelTask, label_task_id =" + reIdTaskId + ", taskLabelTypeInfo=" + taskLabelTypeInfo);

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			if(Strings.isNullOrEmpty(reIdTaskId)) {
				throw new LabelSystemException("ReID标注任务ID不能为空。");
			}
		
			reIdTaskService.updateReIdTask(token, reIdTaskId, taskLabelTypeInfo);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
	@ApiOperation(value="根据指定标注任务查询该任务所有要标注人工标图片信息", notes="返回所有的人工标注任务")
	@RequestMapping(value="/reid-task-show-result", method = RequestMethod.GET)
	public PageResult queryReIdTaskShowResultByTaskId(@RequestParam("reid_task_id") String reIdTaskId,@RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryReIdTaskShowResultByTaskId  token =" + token + "reid_task_id=" + reIdTaskId);

		return reIdTaskService.queryReIdTaskShowResultPageByLabelTaskId(reIdTaskId, startPage, pageSize,token);
	}
	
	
	@ApiOperation(value="根据指定标注任务查询该任务所有要标注人工标图片信息", notes="返回所有的人工标注任务")
	@RequestMapping(value="/reid-task-delete-label", method = RequestMethod.POST)
	public Result deleteReIdLabel(@RequestParam("reid_task_id") String reIdTaskId,@RequestParam("start_id") Integer startId, @RequestParam("end_id") Integer endId, @RequestParam(value="one_reid_name",required=false, defaultValue="")  String one_reid_name) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("deleteReIdLabel  token =" + token + "reid_task_id=" + reIdTaskId + " start_id=" + startId + " endId=" + endId + " one_reid_name=" + one_reid_name);
		Result re = new Result();
		try {
			reIdTaskService.deleteReIdLabel(reIdTaskId, startId, endId,one_reid_name,token);
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
	@ApiOperation(value="删除指定的行人再识别标注任务", notes="删除指定的行人再识别标注任务")
	@RequestMapping(value="/reId-task-delete-reid", method = RequestMethod.DELETE)
	public Result deleteReIdByIdAndName(@RequestParam("reid_task_id") String reIDTaskId,@RequestParam("reid_name") String reIdName) throws LabelSystemException{
		
		logger.info("deleteReIdByIdAndName, id =" + reIDTaskId + " name=" + reIdName);

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			reIdTaskService.deleteReIdByIdAndName(token,reIDTaskId,reIdName);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	@ApiOperation(value="修改指定的行人再识别标注任务", notes="修改指定的行人再识别标注任务")
	@RequestMapping(value="/reId-task-modify-reid", method = RequestMethod.PATCH)
	public Result modifyReIdByIdAndName(@RequestParam("reid_task_id") String reIDTaskId,@RequestParam("reid_name") String reIdName,@RequestParam("new_reid_name") String newReIdName ) throws LabelSystemException{
		
		logger.info("modifyReIdByIdAndName, id =" + reIDTaskId + " old name=" + reIdName + " new name=" +newReIdName);

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			reIdTaskService.modifyReIdByIdAndName(token,reIDTaskId,reIdName,newReIdName);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	@ApiOperation(value="删除指定的行人再识别标注任务", notes="删除指定的行人再识别标注任务")
	@RequestMapping(value="/reId-task-delete-areidimg", method = RequestMethod.DELETE)
	public Result deleteAReIdImg(@RequestParam("reid_task_id") String reIDTaskId,@RequestParam("reid_name") String reIdName,@RequestParam("img_name") String imgName) throws LabelSystemException{
		
		logger.info("deleteAReIdImg, id =" + reIDTaskId + " name=" + reIdName + " imgName=" + imgName);

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			reIdTaskService.deleteAReIdImg(token,reIDTaskId,reIdName,imgName);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	@ApiOperation(value="修改指定的行人再识别标注", notes="修改指定的行人再识别标注")
	@RequestMapping(value="/reId-task-modify-areidimg", method = RequestMethod.PATCH)
	public Result modifyAReIdImg(@RequestParam("reid_task_id") String reIDTaskId,@RequestParam("reid_name") String reIdName,@RequestParam("img_name") String imgName,@RequestParam("new_reid_name") String newReIdName ) throws LabelSystemException{
		
		logger.info("modifyAReIdImg, id =" + reIDTaskId + " name=" + reIdName + " imgName=" + imgName + " new_reid_name=" +newReIdName);

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			reIdTaskService.modifyAReIdImg(token,reIDTaskId,reIdName,imgName,newReIdName);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	

}
