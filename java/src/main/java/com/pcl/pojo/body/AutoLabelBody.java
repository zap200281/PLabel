package com.pcl.pojo.body;

import java.io.Serializable;

public class AutoLabelBody implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String label_task_id;
	
	private String taskId;
	
	private int startIndex;
	
	private int endIndex;
	
	private int model;
	
	private String pic_object_name;
	
	private String task_type;
	
	private String label_type;
	
	private int label_id;
	
	private int label_option;
	
	private int userId;

	public String getLabel_task_id() {
		return label_task_id;
	}

	public void setLabel_task_id(String label_task_id) {
		this.label_task_id = label_task_id;
	}

	public int getModel() {
		return model;
	}

	public void setModel(int model) {
		this.model = model;
	}

	public String getPic_object_name() {
		return pic_object_name;
	}

	public void setPic_object_name(String pic_object_name) {
		this.pic_object_name = pic_object_name;
	}

	public String getTask_type() {
		return task_type;
	}

	public void setTask_type(String task_type) {
		this.task_type = task_type;
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}

	public int getLabel_id() {
		return label_id;
	}

	public void setLabel_id(int label_id) {
		this.label_id = label_id;
	}

	public int getLabel_option() {
		return label_option;
	}

	public void setLabel_option(int label_option) {
		this.label_option = label_option;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getLabel_type() {
		return label_type;
	}

	public void setLabel_type(String label_type) {
		this.label_type = label_type;
	}
	
}
