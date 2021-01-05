package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.LogSecInfo;

@Mapper
public interface LogSecInfoDao {

	public int addLogSecInfo(LogSecInfo logInfo);
	
	public int deleteLogSecInfo(String id);
	
	public List<LogSecInfo> queryLogSecInfoPageForDay(Map<String,Object> paramMap);
	
	public int queryLogSecInfoPageForDayCount(Map<String,Object> paramMap);
	
    public List<LogSecInfo> queryLogSecInfoPageForBeforeDay(Map<String,Object> paramMap);
	
	public int queryLogSecInfoPageForBeforeDayCount(Map<String,Object> paramMap);
	
	public List<LogSecInfo> queryLogSecInfoByIdList(List<String> idList);
	
}
