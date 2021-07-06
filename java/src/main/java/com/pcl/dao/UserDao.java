package com.pcl.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.pcl.pojo.mybatis.User;

@Mapper
public interface UserDao {

	public int addUser(User user);
	
	public int updateUser(User user);
	
	public int updateUserIndentity(User user);

	public int updateUserPassword(User user);
	
	public int deleteUser(int id);
	
	public List<User> queryUser(String userName);
	
	public List<User> queryAll();
	
	public List<User> queryAllIdOrName();
	
	public User queryUserById(int id);

	public List<User> queryUserPage(Map<String,Integer> paramMap);
	
	/**
	 * 分页查询返回的总记录数
	 * @param paramMap
	 * @return
	 */
	public int queryUserCount(Map<String,Integer> paramMap);

	public List<User> queryVerifyUser();
}
