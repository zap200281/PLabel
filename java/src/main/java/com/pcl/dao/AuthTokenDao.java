package com.pcl.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.AuthToken;

@Mapper
public interface AuthTokenDao {

	public int saveAuthToken(AuthToken token) ;
	
	public int delete(String token);
	
	public int deleteTokenByUser(String userId);
	
	public List<AuthToken> queryAuthToken(String token);
}
