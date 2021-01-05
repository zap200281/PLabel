package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.ReIDTaskResult;

@Mapper
public interface ReIDTaskResultDao {

	public int addBatchTaskItem(List<ReIDTaskResult> reTaskItemList);
	
	public List<ReIDTaskResult> queryReIDTaskResult(Map<String,Object> paramMap);
	
	public int deleteByReIDTaskId(String reIdTaskId);
	
}
