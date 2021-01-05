package com.pcl.dao;

import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.RetrainTaskMsgResult;

@Mapper
public interface RetrainTaskMsgResultDao {

	public int addRetrainTaskMsgResult(RetrainTaskMsgResult msgResult);
	
	public int updateRetrainTaskMsgResult(Map<String,Object> paramMap);
	
	public int deleteRetrainTaskMsgResultById(String id);
	
	public RetrainTaskMsgResult queryRetrainTaskMsgResultById(String id);
	
}
