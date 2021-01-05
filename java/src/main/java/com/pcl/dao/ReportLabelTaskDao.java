package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.ReportLabelTask;

@Mapper
public interface ReportLabelTaskDao {

	public int addReportLabelTask(ReportLabelTask reportLabelTask);
	
	public List<ReportLabelTask> queryReportLabelTask(Map<String,Object> paramMap);
	
	
	public int deleteReportLabelTask(Map<String,Object> paramMap);
	
	public List<ReportLabelTask> queryAllReportLabelTaskPage(Map<String,Object> paramMap);
	
	public List<ReportLabelTask> queryAllReportLabelTaskForMeasure(Map<String,Object> paramMap);
	
	public int queryAllReportLabelTaskPageCount(Map<String,Object> paramMap);
	
}
