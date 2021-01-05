package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.VideoLabelTask;

@Mapper
public interface VideoLabelTaskDao {

	public int addVideoLabelTask(VideoLabelTask task);
	
	public int updateVideoLabelTask(Map<String,Object> paramMap);
	
	public int updateVideoLabelTaskStatus(Map<String,Object> paramMap);
	
	public VideoLabelTask queryVideoLabelTask(String id);
	
	public int deleteVideoLabelTask(String id);
	
	public List<VideoLabelTask> queryVideoLabelTaskPage(Map<String,Object> paramMap);
	
	public int queryVideoLabelTaskPageCount(Map<String,Object> paramMap);
	
}
