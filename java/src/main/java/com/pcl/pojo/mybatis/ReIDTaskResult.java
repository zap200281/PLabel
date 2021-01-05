package com.pcl.pojo.mybatis;

public class ReIDTaskResult {

	private String id;//任务id
	
	private String label_task_id;
	
	private String label_task_name;//用于此条记录归属那个目的标注框所在的任务
	
	private String src_image_info;//源标注框信息
	
	private String related_info;//与源标注框相关的标注信息

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRelated_info() {
		return related_info;
	}

	public void setRelated_info(String related_info) {
		this.related_info = related_info;
	}

	public String getSrc_image_info() {
		return src_image_info;
	}

	public void setSrc_image_info(String src_image_info) {
		this.src_image_info = src_image_info;
	}

	public String getLabel_task_name() {
		return label_task_name;
	}

	public void setLabel_task_name(String label_task_name) {
		this.label_task_name = label_task_name;
	}

	public String getLabel_task_id() {
		return label_task_id;
	}

	public void setLabel_task_id(String label_task_id) {
		this.label_task_id = label_task_id;
	}

	
	
}
