package com.pcl.pojo.mybatis;

public class AlgModel {

	private int id;
	
	private String model_name;
	
	private String local_path;
	
	private String model_url;
	
	private int model_type;
	
	private String alg_instance_id;
	
	private String exec_script;
	
	private String train_script;
	
	private String conf_path;
	
	private String type_list;
	
	private double threshold;
	
	private int a_picture_cost_time;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getModel_name() {
		return model_name;
	}

	public void setModel_name(String model_name) {
		this.model_name = model_name;
	}

	public String getLocal_path() {
		return local_path;
	}

	public void setLocal_path(String local_path) {
		this.local_path = local_path;
	}

	public String getModel_url() {
		return model_url;
	}

	public void setModel_url(String model_url) {
		this.model_url = model_url;
	}

	public String getAlg_instance_id() {
		return alg_instance_id;
	}

	public void setAlg_instance_id(String alg_instance_id) {
		this.alg_instance_id = alg_instance_id;
	}

	public String getExec_script() {
		return exec_script;
	}

	public void setExec_script(String exec_script) {
		this.exec_script = exec_script;
	}

	public String getTrain_script() {
		return train_script;
	}

	public void setTrain_script(String train_script) {
		this.train_script = train_script;
	}

	public String getConf_path() {
		return conf_path;
	}

	public void setConf_path(String conf_path) {
		this.conf_path = conf_path;
	}

	public String getType_list() {
		return type_list;
	}

	public void setType_list(String type_list) {
		this.type_list = type_list;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public int getA_picture_cost_time() {
		return a_picture_cost_time;
	}

	public void setA_picture_cost_time(int a_picture_cost_time) {
		this.a_picture_cost_time = a_picture_cost_time;
	}

	public int getModel_type() {
		return model_type;
	}

	public void setModel_type(int model_type) {
		this.model_type = model_type;
	}
	
	
}
