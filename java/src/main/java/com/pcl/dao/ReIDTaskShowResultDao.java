package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.ReIDTaskShowResult;

@Mapper
public interface ReIDTaskShowResultDao {

	public int addBatchShowResultItem(List<ReIDTaskShowResult> reTaskShowResultList);
	
	public ReIDTaskShowResult queryReIDShowTaskResult(Map<String,Object> paramMap);
	
	public List<ReIDTaskShowResult> queryReIDShowTaskResultPage(Map<String,Object> paramMap);
	
	public int queryReIDShowTaskResultPageCount(Map<String,Object> paramMap);
	
	public List<ReIDTaskShowResult> queryReIDShowTaskResultById(String reIdTaskId);
	
	public int deleteByReIDTaskId(String reIdTaskId);
	
	public int deleteByReIDTaskAndReIdName(Map<String,Object> paramMap);
	
	public int updateShowResult(Map<String,Object> paramMap);
	
}
