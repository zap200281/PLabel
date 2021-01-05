package com.pcl.dao;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.UserExtend;

@Mapper
public interface UserExtendDao {

	public int addUserExtend(UserExtend userExtend);
	
	public int deleteUserExtend(int user_id);
	
	public int updateUserExtendFuncTableName(UserExtend userExtend);
	
	public int updateUserExtendProperties(UserExtend userExtend);
	
	public UserExtend queryUserExtend(int user_id);
	
}
