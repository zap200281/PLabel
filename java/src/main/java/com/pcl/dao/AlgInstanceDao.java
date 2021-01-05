package com.pcl.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.AlgInstance;

@Mapper
public interface AlgInstanceDao {

	public int addAlgInstance(AlgInstance algInstance);
	
	public int delete(int id);
	
	public List<AlgInstance> queryAlgInstance(String alg_name);
	
	public AlgInstance queryAlgInstanceById(int id);
	
}
