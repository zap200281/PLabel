package com.pcl.pojo.mybatis;

public class LoginInfo {

	private int user_id;
	private int login_error_time;
	private String last_login_time;
	private String extend1;
	public int getUser_id() {
		return user_id;
	}
	public void setUser_id(int user_id) {
		this.user_id = user_id;
	}
	public int getLogin_error_time() {
		return login_error_time;
	}
	public void setLogin_error_time(int login_error_time) {
		this.login_error_time = login_error_time;
	}
	public String getLast_login_time() {
		return last_login_time;
	}
	public void setLast_login_time(String last_login_time) {
		this.last_login_time = last_login_time;
	}
	public String getExtend1() {
		return extend1;
	}
	public void setExtend1(String extend1) {
		this.extend1 = extend1;
	}
}
