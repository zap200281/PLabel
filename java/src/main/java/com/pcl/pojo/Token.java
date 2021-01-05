package com.pcl.pojo;

import java.io.Serializable;

public class Token implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -707193229963731031L;

	private String token;
	
	private String userName;
	
	private String nickName;
	
	private int userType;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public int getUserType() {
		return userType;
	}

	public void setUserType(int userType) {
		this.userType = userType;
	}
	
}
