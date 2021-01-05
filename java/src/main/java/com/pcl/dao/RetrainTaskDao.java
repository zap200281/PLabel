package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.RetrainTask;

@Mapper
public interface RetrainTaskDao {

	public RetrainTask queryRetrainTaskById(String id);
	
	public List<RetrainTask> queryRetrainTask(Map<String,Integer> paramMap);
	
	public List<RetrainTask> queryRetrainTaskByStatus(String task_status);
	
	public List<RetrainTask> queryLastRetrainTask(Map<String,Object> paramMap);
	
	public int queryRetrainTaskCount(Map<String,Integer> paramMap);
	
	public int addRetrainTask(RetrainTask retrainTask);
	
	public int updateRetrainTask(Map<String,Object> paramMap);
	
	public int deleteRetrainTaskById(String id);
}
