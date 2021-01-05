package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.VideoCountTask;

@Mapper
public interface VideoCountTaskDao {

	public int addVideoCountTask(VideoCountTask task);
	
	public VideoCountTask queryVideoCountTask(String id);
	
	public int deleteVideoCountTask(String id);
	
	public List<VideoCountTask> queryVideoCountTaskPage(Map<String,Object> paramMap);
	
	public int queryVideoCountTaskPageCount(Map<String,Object> paramMap);

	public int updateVideoCountLabelTaskStatus(Map<String, Object> paramMap);
	
}
