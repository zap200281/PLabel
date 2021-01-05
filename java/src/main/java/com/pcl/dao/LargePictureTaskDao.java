package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.LargePictureTask;

@Mapper
public interface LargePictureTaskDao {

	public int addLargePictureTask(LargePictureTask task);
	
	public int updateLargePictureTask(Map<String,Object> paramMap);
	
	public int updateLargePictureTaskStatus(Map<String,Object> paramMap);
	
	public LargePictureTask queryLargePictureTask(String id);
	
	public int deleteLargePictureTask(String id);
	
	public List<LargePictureTask> queryLargePictureTaskPage(Map<String,Object> paramMap);
	
	public int queryLargePictureTaskPageCount(Map<String,Object> paramMap);
	
	
	public List<LargePictureTask> queryLargePictureTaskByDataSetId(String dataset_id);
	
}
