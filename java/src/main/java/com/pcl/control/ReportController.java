package com.pcl.control;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.PageResult;
import com.pcl.pojo.body.ReportBody;
import com.pcl.pojo.body.ReportMeasureBody;
import com.pcl.pojo.display.DisplayReportMeasure;
import com.pcl.service.ReportService;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api/report/")
public class ReportController {

	private static Logger logger = LoggerFactory.getLogger(ReportController.class);

	@Autowired
	HttpServletRequest request;
	
	@Autowired
	ReportService reportService;
	
	
	@ApiOperation(value="查询工作量报表信息", notes="返回所有人的工作量报表信息")
	@RequestMapping(value="/queryReportPage", method = RequestMethod.POST,produces ="application/json;charset=utf-8")
	public PageResult queryReportPage(@RequestBody ReportBody body) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryReportPage  token =" + token);

		return reportService.queryReportPage(token, body);
	}
	
	
	@ApiOperation(value="查询工作量度量信息", notes="返回工作量度量信息")
	@RequestMapping(value="/queryReportMeasure", method = RequestMethod.POST,produces ="application/json;charset=utf-8")
	public List<DisplayReportMeasure> queryReportMeasure(@RequestBody ReportMeasureBody body) throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryReportPage  token =" + token);

		return reportService.queryReportMeasure(token, body);
	}
	
}
