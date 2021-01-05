package com.pcl.control;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.body.DcmLabelBody;
import com.pcl.pojo.display.DoubleThreeObject;
import com.pcl.pojo.display.ThreeObject;
import com.pcl.service.LabelDcmService;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api")
public class LabelDcmController {
	
	
	@Autowired
	LabelDcmService labelDcmService;
	
	@Autowired
	HttpServletRequest request;

	@ApiOperation(value="查询所有的人工标注任务", notes="返回所有的人工标注任务")
	@RequestMapping(value="/query-three-dcm", method = RequestMethod.GET)
	public List<ThreeObject> queryDcmThreeLabelInfo(@RequestParam("label_task_id") String labelTaskId) throws LabelSystemException{

		String token = request.getHeader("authorization");
		
		return labelDcmService.queryDcmThreeLabelInfo(token,labelTaskId);
	}
	
	@ApiOperation(value="查询所有的人工标注任务", notes="返回所有的人工标注任务")
	@RequestMapping(value="/query-three-dcm-double", method = RequestMethod.GET)
	public List<DoubleThreeObject> queryDcmDoubleThreeLabelInfo(@RequestParam("label_task_id") String labelTaskId) throws LabelSystemException{

		String token = request.getHeader("authorization");
		
		return labelDcmService.queryDoubleDcmThreeLabelInfo(token,labelTaskId);
	}
	
	@ApiOperation(value="查询所有的人工标注任务", notes="返回所有的人工标注任务")
	@RequestMapping(value="/save-three-label", method = RequestMethod.GET)
	public void saveDcmLabelInfo(@RequestBody DcmLabelBody dcmLabelBody) throws LabelSystemException{

		String token = request.getHeader("authorization");
		
		labelDcmService.saveDcmLabelInfo(dcmLabelBody,token);
		
		//return labelDcmService.queryDcmThreeLabelInfo(token,labelTaskId);
	}
}
