package com.pcl.pojo.body;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LabelTaskBody implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6917797918258047684L;

	@JsonProperty(value="task_name")
	private String taskName;
	
	@JsonProperty(value="task_flow_type")
	private int task_flow_type;
	
	@JsonProperty(value="relate_other_label_task")
	private String relate_other_label_task;
	
	@JsonProperty(value="relate_task_id")
	private String relateTaskId;
	
	@JsonProperty(value="relate_task_name")
	private String relateTaskName;
	
	@JsonProperty(value="taskType")
	private int taskType;
	
	@JsonProperty(value="taskLabelTypeInfo")
	private String taskLabelTypeInfo;
	
	@JsonProperty(value="assign_user_id")
	private int assign_user_id;
	
	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}


	public int getTaskType() {
		return taskType;
	}

	public void setTaskType(int taskType) {
		this.taskType = taskType;
	}

	public String getRelateTaskId() {
		return relateTaskId;
	}

	public void setRelateTaskId(String relateTaskId) {
		this.relateTaskId = relateTaskId;
	}

	public String getTaskLabelTypeInfo() {
		return taskLabelTypeInfo;
	}

	public void setTaskLabelTypeInfo(String taskLabelTypeInfo) {
		this.taskLabelTypeInfo = taskLabelTypeInfo;
	}

	public int getTask_flow_type() {
		return task_flow_type;
	}

	public void setTask_flow_type(int task_flow_type) {
		this.task_flow_type = task_flow_type;
	}

	public String getRelate_other_label_task() {
		return relate_other_label_task;
	}

	public void setRelate_other_label_task(String relate_other_label_task) {
		this.relate_other_label_task = relate_other_label_task;
	}

	public int getAssign_user_id() {
		return assign_user_id;
	}

	public void setAssign_user_id(int assign_user_id) {
		this.assign_user_id = assign_user_id;
	}

	public String getRelateTaskName() {
		return relateTaskName;
	}

	public void setRelateTaskName(String relateTaskName) {
		this.relateTaskName = relateTaskName;
	}
	
}
