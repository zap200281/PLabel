package com.pcl.pojo.mybatis;

public class LabelTaskItem {

	private String id;
	
	private String pic_url;
	
	//图片宽高
	private String pic_object_name;
	
	private String label_info;
	
	private String label_task_id;
	
	private String item_add_time;
	
	private String pic_image_field;
	
	private int label_status;
	
	private int display_order1;
	
	private int display_order2;
	
	private String display;
	
	private int verify_status;
	
	private String verify_desc;
	
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


	public String getPic_image_field() {
		return pic_image_field;
	}

	public void setPic_image_field(String pic_image_field) {
		this.pic_image_field = pic_image_field;
	}

	public String getLabel_task_id() {
		return label_task_id;
	}

	public void setLabel_task_id(String label_task_id) {
		this.label_task_id = label_task_id;
	}

	public int getLabel_status() {
		return label_status;
	}

	public void setLabel_status(int label_status) {
		this.label_status = label_status;
	}

	public int getDisplay_order1() {
		return display_order1;
	}

	public void setDisplay_order1(int display_order1) {
		this.display_order1 = display_order1;
	}

	public int getDisplay_order2() {
		return display_order2;
	}

	public void setDisplay_order2(int display_order2) {
		this.display_order2 = display_order2;
	}

	public String getDisplay() {
		return display;
	}

	public void setDisplay(String display) {
		this.display = display;
	}

	public int getVerify_status() {
		return verify_status;
	}

	public void setVerify_status(int verify_status) {
		this.verify_status = verify_status;
	}

	public String getVerify_desc() {
		return verify_desc;
	}

	public void setVerify_desc(String verify_desc) {
		this.verify_desc = verify_desc;
	}
	
}
