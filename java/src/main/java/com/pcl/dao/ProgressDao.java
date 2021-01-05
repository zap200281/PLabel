package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.Progress;


@Mapper
public interface ProgressDao {

	public int addProgress(Progress progress);
	
	public int deleteProgress(String id);
	
	public Progress queryProgressById(String id);
	
	public int updateProgress(Map<String,Object> paramMap);
	
	public List<Progress> queryProgressByIdList(List<String> idList);
}
