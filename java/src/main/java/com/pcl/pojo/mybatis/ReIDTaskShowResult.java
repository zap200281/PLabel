package com.pcl.pojo.mybatis;

public class ReIDTaskShowResult {

	private String label_task_id;//reidtask id
	
	private String reid_name;
	
	private String related_info;

	
	public String getReid_name() {
		return reid_name;
	}

	public void setReid_name(String reid_name) {
		this.reid_name = reid_name;
	}

	public String getRelated_info() {
		return related_info;
	}

	public void setRelated_info(String related_info) {
		this.related_info = related_info;
	}

	public String getLabel_task_id() {
		return label_task_id;
	}

	public void setLabel_task_id(String label_task_id) {
		this.label_task_id = label_task_id;
	}
	
}
