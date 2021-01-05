package com.pcl.pojo.display;

public class DisplayPrePredictTask {

	private String id;
	
	private String task_name;
	
	private String zip_object_name;
	
	private String task_start_time;
	
	private int task_status;
	
	private String task_status_desc;

	private String user;
	
	private String alg_model;
	
	private String zip_bucket_name;
	
	private String dataset_name;

	private double score_threshhold;
	
	private int delete_similar_picture;
	
	private int delete_no_label_picture;
	
	private int needToDistiguishTypeOrColor;
	
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

	public String getZip_object_name() {
		return zip_object_name;
	}

	public void setZip_object_name(String zip_object_name) {
		this.zip_object_name = zip_object_name;
	}

	public String getTask_start_time() {
		return task_start_time;
	}

	public void setTask_start_time(String task_start_time) {
		this.task_start_time = task_start_time;
	}


	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getAlg_model() {
		return alg_model;
	}

	public void setAlg_model(String alg_model) {
		this.alg_model = alg_model;
	}

	public String getZip_bucket_name() {
		return zip_bucket_name;
	}

	public void setZip_bucket_name(String zip_bucket_name) {
		this.zip_bucket_name = zip_bucket_name;
	}

	public String getDataset_name() {
		return dataset_name;
	}

	public void setDataset_name(String dataset_name) {
		this.dataset_name = dataset_name;
	}

	public String getTask_status_desc() {
		return task_status_desc;
	}

	public void setTask_status_desc(String task_status_desc) {
		this.task_status_desc = task_status_desc;
	}

	public int getTask_status() {
		return task_status;
	}

	public void setTask_status(int task_status) {
		this.task_status = task_status;
	}

	public double getScore_threshhold() {
		return score_threshhold;
	}

	public void setScore_threshhold(double score_threshhold) {
		this.score_threshhold = score_threshhold;
	}

	public int getDelete_similar_picture() {
		return delete_similar_picture;
	}

	public void setDelete_similar_picture(int delete_similar_picture) {
		this.delete_similar_picture = delete_similar_picture;
	}

	public int getDelete_no_label_picture() {
		return delete_no_label_picture;
	}

	public void setDelete_no_label_picture(int delete_no_label_picture) {
		this.delete_no_label_picture = delete_no_label_picture;
	}

	public int getNeedToDistiguishTypeOrColor() {
		return needToDistiguishTypeOrColor;
	}

	public void setNeedToDistiguishTypeOrColor(int needToDistiguishTypeOrColor) {
		this.needToDistiguishTypeOrColor = needToDistiguishTypeOrColor;
	}
	
}
