package com.pcl.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.VideoInfo;

@Mapper
public interface DataSetVideoInfoDao {

	public int addBatchVideoInfo(List<VideoInfo> videoInfoList);

	public int addVideoInfo(VideoInfo videoInfo);
	
	public int deleteVideoInfoById(String id);
	
	public int deleteVideoInfoByDataSetId(String dataset_id);
	
	public List<VideoInfo> queryVideoInfoByDataSetId(String dataset_id);
	
	public VideoInfo queryVideoInfoById(String id);
}
