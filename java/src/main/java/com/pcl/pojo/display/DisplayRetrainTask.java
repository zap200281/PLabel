package com.pcl.pojo.display;

public class DisplayRetrainTask {

	private String id;
	
	private String task_name;
	
	private String user;
	
	private String task_status;
	
	private String task_start_time = "";
	
	private String task_finish_time = "";
	
	private String alg_model;
	
	private String pre_predict_task;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTask_name() {
		return task_name;
	}

	public void setTask_name(String task_name) {
		this.task_name = task_name;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getTask_status() {
		return task_status;
	}

	public void setTask_status(String task_status) {
		this.task_status = task_status;
	}

	public String getAlg_model() {
		return alg_model;
	}

	public void setAlg_model(String alg_model) {
		this.alg_model = alg_model;
	}

	public String getPre_predict_task() {
		return pre_predict_task;
	}

	public void setPre_predict_task(String pre_predict_task) {
		this.pre_predict_task = pre_predict_task;
	}

	public String getTask_start_time() {
		return task_start_time;
	}

	public void setTask_start_time(String task_start_time) {
		this.task_start_time = task_start_time;
	}

	public String getTask_finish_time() {
		return task_finish_time;
	}

	public void setTask_finish_time(String task_finish_time) {
		this.task_finish_time = task_finish_time;
	}
	
}
