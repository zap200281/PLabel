package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.LabelTaskItem;

@Mapper
public interface LargePictureTaskItemDao {

	public int addBatchLabelTaskItem(List<LabelTaskItem> labelTaskItemList);
	
	public int addLabelTaskItem(LabelTaskItem labelTaskItem);
	
	public List<LabelTaskItem> queryLabelTaskItemByLabelTaskId(String label_task_id);
	
	
	public List<LabelTaskItem> queryLabelTaskItemByLabelTaskIdAndPicImage(Map<String,Object> paramMap);
	
	
	public LabelTaskItem queryLabelTaskItemById(String id);
	
	public int updateLabelTaskItem(Map<String,Object> paramMap);

	public int deleteLabelTaskByTaskId(String label_task_id);
	
	public int deleteLabelTaskById(String id);
	
	public List<Map<String,Object>> queryLabelTaskStatusByLabelTaskId(List<String> labelTaskIdList);
	
	public List<LabelTaskItem> queryLabelTaskItemPageByLabelTaskId(Map<String,Object> paramMap);
	
	public List<LabelTaskItem> queryLabelTaskItemPageByLabelTaskIdOrderByStartTime(Map<String,Object> paramMap);
	
	public int queryLabelTaskItemPageCountByLabelTaskId(Map<String,Object> paramMap);

	public List<LabelTaskItem> queryVideoLabelTaskItemByTime(Map<String, Object> paramMap);
}
