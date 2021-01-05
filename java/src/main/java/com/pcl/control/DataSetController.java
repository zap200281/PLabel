package com.pcl.control;

import java.util.ArrayList;
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
import com.pcl.constant.Constants;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.PageResult;
import com.pcl.pojo.Result;
import com.pcl.pojo.body.DramFrameBody;
import com.pcl.pojo.display.DisplayDataSet;
import com.pcl.pojo.mybatis.DataSet;
import com.pcl.pojo.mybatis.VideoInfo;
import com.pcl.service.DataSetService;
import com.pcl.util.JsonUtil;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api")
public class DataSetController {

	private static Logger logger = LoggerFactory.getLogger(DataSetController.class);

	@Autowired
	HttpServletRequest request;
	
	@Autowired
	private DataSetService dataSetService;
	

	@ApiOperation(value="删除指定的数据集任务", notes="删除指定的数据集任务")
	@RequestMapping(value="/dateset", method = RequestMethod.DELETE)
	public Result deleteDataSetById(@RequestParam("dateset_id") String dataSetId) throws LabelSystemException{
		
		logger.info("delete dataSetId, id =" + dataSetId);

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			dataSetService.deleteDataSetById(token, dataSetId);
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
	@ApiOperation(value="查询所有的数据集任务", notes="返回所有的数据集任务")
	@RequestMapping(value="/dataset-page", method = RequestMethod.GET)
	public PageResult queryDataSetPage(@RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryDataSetPage  token =" + token);

		return dataSetService.queryDataSet(token, startPage, pageSize);
	}
	
	@ApiOperation(value="查询所有的数据集任务", notes="返回所有的数据集任务")
	@RequestMapping(value="/dataset-byId", method = RequestMethod.GET)
	public DisplayDataSet  queryDataSetById(@RequestParam("datasetId") String dataSetId) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryDataSetById  token =" + token + " datesetId=" + dataSetId);
	
		
		return dataSetService.queryDataSetById(token, dataSetId);
	}
	
	
	@ApiOperation(value="查询所有的数据集任务", notes="返回所有的数据集任务")
	@RequestMapping(value="/dataset", method = RequestMethod.GET)
	public List<DisplayDataSet> queryDataSet(@RequestParam("dateset_type") String datesetType) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryDataSet  token =" + token + " datesetType=" + datesetType);
		List<String> typeList = new ArrayList<>();
		if(datesetType != null) {
			typeList.addAll(JsonUtil.getList(datesetType));
		}else {
			typeList.add(String.valueOf(Constants.DATASET_TYPE_DCM));
			typeList.add(String.valueOf(Constants.DATASET_TYPE_PICTURE));
			typeList.add(String.valueOf(Constants.DATASET_TYPE_SVS));
			typeList.add(String.valueOf(Constants.DATASET_TYPE_VIDEO));
		}
		return dataSetService.queryAllDataSet(token,typeList);
	}
	

	@ApiOperation(value="创建一个数据集", notes="创建一个数据集")
	@RequestMapping(value="/dataset", method = RequestMethod.POST, produces ="application/json;charset=utf-8")
	public Result addDataSetTask(@RequestBody DataSet body){
		logger.info("addDataset, body =" + body.getTask_name());

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			if(Strings.isNullOrEmpty(body.getTask_name()) && Strings.isNullOrEmpty(body.getZip_object_name())) {
				throw new LabelSystemException("创建数据集参数错误，要么关联自动标注任务，要么直接上传图片。");
			}
		
			dataSetService.addDataSet(token, body);
			
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
	@ApiOperation(value="修改一个数据集", notes="修改一个数据集")
	@RequestMapping(value="/updatedataset", method = RequestMethod.POST, produces ="application/json;charset=utf-8")
	public Result udpateDataSetTask(@RequestBody DataSet body){
		logger.info("updateDataset, body =" + body.getId());

		Result re = new Result();
		try {
			String token = request.getHeader("authorization");
			
			if(Strings.isNullOrEmpty(body.getId())) {
				throw new LabelSystemException("修改数据集参数错误。");
			}
			dataSetService.updateDataSet(token, body);
		
			re.setCode(0);
		}catch (Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
	@ApiOperation(value="分页查询所有待标注的图片任务", notes="分页查询所有待标注的图片任务")
	@RequestMapping(value="/dateset-item-page", method = RequestMethod.GET)
	public PageResult queryLabelItemPageByTaskId(@RequestParam("datasetId") String datasetId,@RequestParam("startPage") Integer startPage, @RequestParam("pageSize") Integer pageSize) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryLabelItemPageByTaskId  token =" + token);

		
		return dataSetService.queryDataSetPictureItemPage(datasetId, startPage, pageSize);
	}
	
	@ApiOperation(value="进行抽帧及参数配置", notes="进行抽帧及参数配置")
	@RequestMapping(value="/dateset-chouzhen", method = RequestMethod.POST)
	public String chouZhen(@RequestBody DramFrameBody body) throws LabelSystemException {
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("chouZhen  token =" + token);
		
		
		
		return dataSetService.chouZhen(token,body);
	}
	
	@ApiOperation(value="查询所有的数据集任务", notes="返回所有的数据集任务")
	@RequestMapping(value="/datasetVideoList", method = RequestMethod.GET)
	public List<VideoInfo> queryDataSetVideoList(@RequestParam("datasetId") String datasetId) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryDataSetVideoList  token =" + token + " datasetId=" + datasetId);
		
		return dataSetService.queryDataSetVideoList(token,datasetId);
	}
	
	
	@ApiOperation(value="进行视频合并", notes="视频合并")
	@RequestMapping(value="/datesetVideoConcat", method = RequestMethod.POST)
	public String videoConcat(@RequestParam("datasetId") String datasetId, @RequestParam("videoSetIdList") String videoSetIdList,@RequestParam("destFileName") String destFileName) throws LabelSystemException {
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("videoConcat  token =" + token + " videoSetIdList=" + videoSetIdList + " destFileName=" + destFileName);
		
		
		return dataSetService.videoConcat(token,datasetId,videoSetIdList,destFileName);
	}
	
	@ApiOperation(value="对Svs图片进行分层处理", notes="对Svs图片进行分层处理")
	@RequestMapping(value="/zoomsvs", method = RequestMethod.POST)
	public String zoomSvs(@RequestParam("datasetId") String datasetId) throws LabelSystemException {
		
		String token = request.getHeader("authorization");
		//if(token == null) {
		//	throw new LabelSystemException("user not login.");
		//}
		logger.info("videoConcat  token =" + token + " datasetId=" + datasetId);
		
		
		return dataSetService.zoomSvs(token,datasetId);
	}
	
	
}
