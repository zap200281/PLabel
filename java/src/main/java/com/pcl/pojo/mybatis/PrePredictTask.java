package com.pcl.pojo.mybatis;

import java.io.Serializable;

public class PrePredictTask implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String id;
	
	private String task_name;
	
	private int delete_no_label_picture;
	
	private String task_start_time;
	
	private String task_finish_time;
	
	private int task_status;
	
	private int user_id;
	
	private int alg_model_id;
	
	private String alg_model_id_list;
	
	private String task_status_desc;
	
	private String dataset_id;
	
	private double score_threshhold;
	
	private int needToDistiguishTypeOrColor;
	
	private int delete_similar_picture;
	
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

	public int getAlg_model_id() {
		return alg_model_id;
	}

	public void setAlg_model_id(int alg_model_id) {
		this.alg_model_id = alg_model_id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getUser_id() {
		return user_id;
	}

	public void setUser_id(int user_id) {
		this.user_id = user_id;
	}

	public String getTask_status_desc() {
		return task_status_desc;
	}

	public void setTask_status_desc(String task_status_desc) {
		this.task_status_desc = task_status_desc;
	}

	public String getDataset_id() {
		return dataset_id;
	}

	public void setDataset_id(String dataset_id) {
		this.dataset_id = dataset_id;
	}

	public int getDelete_no_label_picture() {
		return delete_no_label_picture;
	}

	public void setDelete_no_label_picture(int delete_no_label_picture) {
		this.delete_no_label_picture = delete_no_label_picture;
	}

	public double getScore_threshhold() {
		return score_threshhold;
	}

	public void setScore_threshhold(double score_threshhold) {
		this.score_threshhold = score_threshhold;
	}

	public String getAlg_model_id_list() {
		return alg_model_id_list;
	}

	public void setAlg_model_id_list(String alg_model_id_list) {
		this.alg_model_id_list = alg_model_id_list;
	}

	public int getNeedToDistiguishTypeOrColor() {
		return needToDistiguishTypeOrColor;
	}

	public void setNeedToDistiguishTypeOrColor(int needToDistiguishTypeOrColor) {
		this.needToDistiguishTypeOrColor = needToDistiguishTypeOrColor;
	}

	public int getDelete_similar_picture() {
		return delete_similar_picture;
	}

	public void setDelete_similar_picture(int delete_similar_picture) {
		this.delete_similar_picture = delete_similar_picture;
	}


}
