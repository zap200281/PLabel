package com.pcl.pojo.mybatis;

import java.io.Serializable;

public class AuthToken implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7904857821867510770L;
	
	private String token;
	
	private int userId;
	
	private String created;
	
	private long loginTime;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getCreated() {
		return created;
	}

	public void setCreated(String created) {
		this.created = created;
	}

	public long getLoginTime() {
		return loginTime;
	}

	public void setLoginTime(long loginTime) {
		this.loginTime = loginTime;
	}

}
