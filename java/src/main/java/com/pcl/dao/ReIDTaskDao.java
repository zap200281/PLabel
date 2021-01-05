package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.ReIDTask;

@Mapper
public interface ReIDTaskDao {

	public int addReIDTask(ReIDTask reIdTask);
	
	public ReIDTask queryReIDTaskById(String id);
	
	public List<ReIDTask> queryReIDTask(Map<String,Object> paramMap);
	
	public List<ReIDTask> queryReIDTaskByUser(Map<String,Object> paramMap);
	
	public List<ReIDTask> queryReIDTaskByStatus(Map<String,Object> paramMap);
	
	public int queryReIDTaskCount(Map<String,Object> paramMap);
	
	public int deleteReIDTask(String id);
	
	public int updateReIDTask(Map<String,Object> paramMap);
	
	public int updateReIDTaskSelfDefineInfo(Map<String,Object> paramMap);
	
}
