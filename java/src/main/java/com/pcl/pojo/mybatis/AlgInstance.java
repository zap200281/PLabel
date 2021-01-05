package com.pcl.pojo.mybatis;

public class AlgInstance {

	private int id;
	
	private String alg_name;
	
	private String add_time;
	
	//目标检测    目标分割
	private String alg_type_name;
	
	private String alg_root_dir;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getAlg_name() {
		return alg_name;
	}

	public void setAlg_name(String alg_name) {
		this.alg_name = alg_name;
	}

	public String getAdd_time() {
		return add_time;
	}

	public void setAdd_time(String add_time) {
		this.add_time = add_time;
	}

	public String getAlg_type_name() {
		return alg_type_name;
	}

	public void setAlg_type_name(String alg_type_name) {
		this.alg_type_name = alg_type_name;
	}

	public String getAlg_root_dir() {
		return alg_root_dir;
	}

	public void setAlg_root_dir(String alg_root_dir) {
		this.alg_root_dir = alg_root_dir;
	}
	
}
