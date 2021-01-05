package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.PrePredictTask;

@Mapper
public interface PrePredictTaskDao {

	/**
	 * 分页查询
	 * @param paramMap
	 * @return
	 */
	public List<PrePredictTask> queryPrePredictTask(Map<String,Object> paramMap);
	
	public List<PrePredictTask> queryAllPrePredictTask();
	
	public List<PrePredictTask> queryPrePredictTaskByIdList(List<String> list);
	
	public List<PrePredictTask> queryAllPrePredictTaskByUser(int userId);
	
	/**
	 * 分页查询返回的总记录数
	 * @param paramMap
	 * @return
	 */
	public int queryPrePredictTaskCount(Map<String,Object> paramMap);
	
	public List<PrePredictTask> queryPrePredictTaskByStatus(String task_status);
	
	public int addPrePredictTask(PrePredictTask prePredictTask);
	
	public PrePredictTask queryPrePredictTaskById(String id);
	
	public int deletePrePredictTaskById(String id);
	
	public int updatePrePredictTaskStatus(Map<String,Object> paramMap);
	
	public int updatePrePredictTaskStatusDesc(Map<String,Object> paramMap);
}
