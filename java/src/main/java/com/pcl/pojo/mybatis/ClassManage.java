package com.pcl.pojo.mybatis;

public class ClassManage {

	private int id;
	
	private String class_name;
	
	private String class_desc;
	
	private String super_class_name;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getClass_name() {
		return class_name;
	}

	public void setClass_name(String class_name) {
		this.class_name = class_name;
	}

	public String getClass_desc() {
		return class_desc;
	}

	public void setClass_desc(String class_desc) {
		this.class_desc = class_desc;
	}

	public String getSuper_class_name() {
		return super_class_name;
	}

	public void setSuper_class_name(String super_class_name) {
		this.super_class_name = super_class_name;
	}
	
}
