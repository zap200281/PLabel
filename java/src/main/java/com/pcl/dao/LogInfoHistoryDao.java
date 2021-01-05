package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.LogInfo;

@Mapper
public interface LogInfoHistoryDao {

	public int addLogInfo(LogInfo logInfo);
	
	public int deleteLogInfo(String id);
	
	public List<LogInfo> queryLogInfoPageForDay(Map<String,Object> paramMap);
	
	public int queryLogInfoPageForDayCount(Map<String,Object> paramMap);
	
    public List<LogInfo> queryLogInfoPageForBeforeDay(Map<String,Object> paramMap);
	
	public int queryLogInfoPageForBeforeDayCount(Map<String,Object> paramMap);
	
	public List<LogInfo> queryLogInfoByIdList(List<String> idList);
	
}
