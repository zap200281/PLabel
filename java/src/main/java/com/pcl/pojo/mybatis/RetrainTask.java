package com.pcl.pojo.mybatis;

public class RetrainTask {

	private String id;
	
	private String task_name;
	
	private String task_start_time;
	
	private String task_finish_time;
	
	private int task_status;
	
	private String task_status_desc;
	
	private int alg_model_id;
	
	private String pre_predict_task_id;
	
	private int user_id;
	
	private int pid;
	
	private String confPath;
	private String modelPath;
	

	private String retrain_type;
	

	private String retrain_data;
	

	private String detection_type;
	

	private String detection_type_input;
	
	private String retrain_model_name;
	
	private double testTrainRatio;

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

	public int getTask_status() {
		return task_status;
	}

	public void setTask_status(int task_status) {
		this.task_status = task_status;
	}

	public String getTask_status_desc() {
		return task_status_desc;
	}

	public void setTask_status_desc(String task_status_desc) {
		this.task_status_desc = task_status_desc;
	}

	public int getAlg_model_id() {
		return alg_model_id;
	}

	public void setAlg_model_id(int alg_model_id) {
		this.alg_model_id = alg_model_id;
	}

	public String getPre_predict_task_id() {
		return pre_predict_task_id;
	}

	public void setPre_predict_task_id(String pre_predict_task_id) {
		this.pre_predict_task_id = pre_predict_task_id;
	}

	public int getUser_id() {
		return user_id;
	}

	public void setUser_id(int user_id) {
		this.user_id = user_id;
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public String getConfPath() {
		return confPath;
	}

	public void setConfPath(String confPath) {
		this.confPath = confPath;
	}

	public String getModelPath() {
		return modelPath;
	}

	public void setModelPath(String modelPath) {
		this.modelPath = modelPath;
	}

	public String getRetrain_type() {
		return retrain_type;
	}

	public void setRetrain_type(String retrain_type) {
		this.retrain_type = retrain_type;
	}

	public String getRetrain_data() {
		return retrain_data;
	}

	public void setRetrain_data(String retrain_data) {
		this.retrain_data = retrain_data;
	}

	public String getDetection_type() {
		return detection_type;
	}

	public void setDetection_type(String detection_type) {
		this.detection_type = detection_type;
	}

	public String getDetection_type_input() {
		return detection_type_input;
	}

	public void setDetection_type_input(String detection_type_input) {
		this.detection_type_input = detection_type_input;
	}

	public String getRetrain_model_name() {
		return retrain_model_name;
	}

	public void setRetrain_model_name(String retrain_model_name) {
		this.retrain_model_name = retrain_model_name;
	}

	public double getTestTrainRatio() {
		return testTrainRatio;
	}

	public void setTestTrainRatio(double testTrainRatio) {
		this.testTrainRatio = testTrainRatio;
	}


}
