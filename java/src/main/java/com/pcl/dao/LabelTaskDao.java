package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.LabelTask;

@Mapper
public interface LabelTaskDao {

	public LabelTask queryLabelTaskById(String id);
	
	public List<LabelTask> queryLabelTaskByIds(List<String> ids);
	
	public List<LabelTask> queryLabelTaskByDataSetId(String dataSetId);
	
	public List<LabelTask> queryLabelTask(Map<String,Object> paramMap);
	
	public List<LabelTask> queryLabelTaskByUser(Map<String,Object> paramMap);
	
	public int queryLabelTaskCount(Map<String,Object> paramMap);
	
	public int updateLabelTask(Map<String,Object> paramMap);
	
	public int updateLabelTaskLabelCount(Map<String,Object> paramMap);
	

	public int updateLabelTaskStatus(Map<String,Object> paramMap);
	
	public int addLabelTask(LabelTask labelTask);
	
	public int deleteLabelTaskById(String id);
	
	public List<LabelTask> queryLabelTaskAfterTime(Map<String,Object> paramMap);
}
