package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.DataSet;

@Mapper
public interface DataSetDao {
     
	public DataSet queryDataSetById(String id);
	
	public List<DataSet> queryDataSetByType(Map<String,Object> paramMap);
	
	public List<DataSet> queryAllDataSet();
	
	public List<DataSet> queryAllDataSetByIdList(List<String> list);
	
	public List<DataSet> queryDataSet(Map<String,Object> paramMap);
	
	public int queryDataSetCount(Map<String,Object> paramMap);
	
	public int addDataSet(DataSet dataSet);
	
	public int deleteDataSetById(String id);
	
	public int updateDataSet(Map<String,Object> paramMap);
}
