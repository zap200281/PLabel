package com.pcl.pojo.mybatis;

import javax.validation.constraints.NotNull;

public class User {

	private int id;
	
	@NotNull
	private String username;
	
	private String nick_name;
	
	//0为超级用户，1为标注用户，2为审核人员
	private int is_superuser;
	
	private int is_staff;
	
	private int is_active;
	
	private String date_joined;
	
	@NotNull	
	private String password;
	
	private String last_login;
	
	private String first_name;
	
	private String last_name;
	
	private String email;
	
	private String address;
	
	private String mobile;
	
	private String company;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}


	public String getFirst_name() {
		return first_name;
	}

	public void setFirst_name(String first_name) {
		this.first_name = first_name;
	}

	public String getLast_name() {
		return last_name;
	}

	public void setLast_name(String last_name) {
		this.last_name = last_name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getNick_name() {
		return nick_name;
	}

	public void setNick_name(String nick_name) {
		this.nick_name = nick_name;
	}

	public String getLast_login() {
		return last_login;
	}

	public void setLast_login(String last_login) {
		this.last_login = last_login;
	}

	public int getIs_superuser() {
		return is_superuser;
	}

	public void setIs_superuser(int is_superuser) {
		this.is_superuser = is_superuser;
	}

	public int getIs_staff() {
		return is_staff;
	}

	public void setIs_staff(int is_staff) {
		this.is_staff = is_staff;
	}

	public int getIs_active() {
		return is_active;
	}

	public void setIs_active(int is_active) {
		this.is_active = is_active;
	}

	public String getDate_joined() {
		return date_joined;
	}

	public void setDate_joined(String date_joined) {
		this.date_joined = date_joined;
	}


	
}
