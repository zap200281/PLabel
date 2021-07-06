package com.pcl.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.AlgModel;

@Mapper
public interface AlgModelDao {

	public int addAlgModel(AlgModel algModel);
	
	public int delete(int id);
	
	public List<AlgModel> queryAlgModel(String alg_name);
	
	public AlgModel queryAlgModelById(int id);
	
	public List<AlgModel> queryAlgModelAll();
	
	public List<AlgModel> queryAlgModelContainWiseMedical();
	
	public List<AlgModel> queryAlgModelForTracking();
	
	public List<AlgModel> queryAlgModelForHandLabel();
	
	public List<AlgModel> queryAlgModelForAutoLabel();
	
	//属性识别的模型
	public List<AlgModel> queryAlgModelForProperty();
	
}
