package com.pcl.dao;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.LoginInfo;

@Mapper
public interface LoginInfoDao {

	public int addLoginInfo(LoginInfo logInfo);
	
	public int deleteLoginInfo(int user_id);
	
	public LoginInfo queryLoginInfo(int user_id);
	
	public int updateLoginInfo(LoginInfo logInfo);
	
}
