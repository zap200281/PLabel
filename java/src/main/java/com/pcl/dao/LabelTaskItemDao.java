package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.pcl.pojo.mybatis.LabelTaskItem;

@Mapper
public interface LabelTaskItemDao {

	//public int addBatchLabelTaskItem(List<LabelTaskItem> labelTaskItemList);
	
	public int addBatchLabelTaskItemMap(Map<String,Object> paramMap);
	
	//public int addLabelTaskItem(LabelTaskItem labelTaskItem);
	
	public List<LabelTaskItem> queryLabelTaskItemByLabelTaskId(@Param("user_id") String tableNamePos,@Param("label_task_id") String label_task_id);
	
	public List<LabelTaskItem> queryLabelTaskItemByLabelTaskIdAndPicImage(Map<String,Object> paramMap);
	
	//--
	public LabelTaskItem queryLabelTaskItemById(@Param("user_id") String tableNamePos,String id);
	
	public int updateLabelTaskItem(Map<String,Object> paramMap);

	public int deleteLabelTaskById(@Param("user_id") String tableNamePos,String label_task_id);
	
	public int deleteLabelTaskItemById(@Param("user_id") String tableNamePos,String id);
	
	public int existTable(String tableName);
	
	public void createTable(@Param("tableName") String tableName);
	
	//public List<LabelTaskItem> queryLabelTaskItemAfterTime(Map<String,Object> paramMap);
	
	public List<Map<String,Object>> queryLabelTaskStatusByLabelTaskId(Map<String,Object> labelTaskIdListMap);
	
	public List<LabelTaskItem> queryLabelTaskItemPageByLabelTaskId(Map<String,Object> paramMap);
	
	public int queryLabelTaskItemPageCountByLabelTaskId(Map<String,Object> paramMap);
}
