package com.pcl.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.ClassManage;

@Mapper
public interface ClassManageDao {

	public int addClassManage(ClassManage clazz);
	
	
	public List<ClassManage> queryAll();
	
}
