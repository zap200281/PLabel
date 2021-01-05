package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pcl.pojo.mybatis.LabelTaskItem;

@Mapper
public interface ReIDLabelTaskItemDao {

	public int addBatchLabelTaskItem(Map<String,Object> paramMap);
	
	//public int addLabelTaskItem(LabelTaskItem labelTaskItem);
	
	public List<LabelTaskItem> queryLabelTaskItemByLabelTaskId(@Param("user_id") String tableNamePos,String label_task_id);
	
	public List<LabelTaskItem> queryLabelTaskItemByLabelTaskIdOderbyImageNameAsc(@Param("user_id") String tableNamePos,String label_task_id);
	
	public List<LabelTaskItem> queryLabelTaskItemByIdList(Map<String,Object> paramMap);
	
	//public List<LabelTaskItem> queryLabelTaskItemByLabelTaskIdAndPicImage(Map<String,Object> paramMap);
	
	public LabelTaskItem queryLabelTaskItemById(@Param("user_id") String tableNamePos,String id);
	
	public int updateLabelTaskItem(Map<String,Object> paramMap);

	public int deleteLabelTaskById(@Param("user_id") String tableNamePos,String label_task_id);
	
	//public List<Map<String,Object>> queryLabelTaskStatusByLabelTaskId(List<String> labelTaskIdList);
	
	public List<LabelTaskItem> queryLabelTaskItemPageByLabelTaskId(Map<String,Object> paramMap);
	
	public int queryLabelTaskItemPageCountByLabelTaskId(Map<String,Object> paramMap);
	
	public List<LabelTaskItem> queryLabelTaskItemByReIdAndLabelTaskId(Map<String,Object> paramMap);
	
	public int existTable(String tableName);
	
	public void createTable(@Param("tableName") String tableName);
	
}
