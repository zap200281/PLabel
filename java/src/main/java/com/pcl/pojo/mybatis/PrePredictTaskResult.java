package com.pcl.pojo.mybatis;

public class PrePredictTaskResult {

	private String id;
	
	private String pic_url;
	
	private String pic_object_name;
	
	private String label_info;
	
	private String item_add_time;
	
	private String pre_predict_task_id;
	
	private String pic_image_field;
	
	private String user_id;//表名后缀

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPic_url() {
		return pic_url;
	}

	public void setPic_url(String pic_url) {
		this.pic_url = pic_url;
	}

	public String getPic_object_name() {
		return pic_object_name;
	}

	public void setPic_object_name(String pic_object_name) {
		this.pic_object_name = pic_object_name;
	}

	public String getLabel_info() {
		return label_info;
	}

	public void setLabel_info(String label_info) {
		this.label_info = label_info;
	}

	public String getItem_add_time() {
		return item_add_time;
	}

	public void setItem_add_time(String item_add_time) {
		this.item_add_time = item_add_time;
	}

	public String getPre_predict_task_id() {
		return pre_predict_task_id;
	}

	public void setPre_predict_task_id(String pre_predict_task_id) {
		this.pre_predict_task_id = pre_predict_task_id;
	}

	public String getPic_image_field() {
		return pic_image_field;
	}

	public void setPic_image_field(String pic_image_field) {
		this.pic_image_field = pic_image_field;
	}

	public String getUser_id() {
		return user_id;
	}

	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}
	
}
