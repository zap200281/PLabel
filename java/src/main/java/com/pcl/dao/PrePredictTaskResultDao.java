package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pcl.pojo.mybatis.PrePredictTaskResult;

@Mapper
public interface PrePredictTaskResultDao {

	//public List<PrePredictTaskResult> selectAll(Map<String,Integer> paramMap);
	
	public List<PrePredictTaskResult> selectByPrePredictTaskId(@Param("user_id") String tableNamePos,String pre_predict_task_id);
	
	public int deletePrePredictTaskResultById(@Param("user_id") String tableNamePos,String pre_predict_task_id);
	
	public int addPrePredictTaskResult(PrePredictTaskResult prePredictTaskResult);
	
	public List<PrePredictTaskResult> queryPredictTaskItemPageByTaskId(Map<String,Object> paramMap);
	
	public int queryPredictTaskItemPageCountByTaskId(Map<String,Object> paramMap);
	
	public int updatePrePredictTaskResult(Map<String,Object> paramMap);
	
	public int existTable(String tableName);
	
	public void createTable(@Param("tableName") String tableName);
	
}
