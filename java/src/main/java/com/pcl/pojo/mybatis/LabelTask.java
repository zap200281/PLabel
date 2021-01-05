package com.pcl.pojo.mybatis;

import java.io.Serializable;

public class LabelTask implements Serializable{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7783202723852695548L;

	private String id;
	
	private String task_name;
	
	private String task_add_time;
	
	private String relate_task_id;
	
	private String relate_task_name;
	
	private int user_id;

	private int total_picture;
	
	private int finished_picture;
	
	private int task_type;
	
	private String task_label_type_info;
	
	private int assign_user_id;
	
	//工作流，审核模式或者标注模式
	private int task_flow_type;
	
	//审核模式下，在界面可以直接调出其它用户的标注
	private String relate_other_label_task;
	
	private int verify_user_id;
	
	private int task_status;

	private String task_status_desc;
	
	private int total_label;
	
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

	public String getTask_add_time() {
		return task_add_time;
	}

	public void setTask_add_time(String task_add_time) {
		this.task_add_time = task_add_time;
	}


	public int getUser_id() {
		return user_id;
	}

	public void setUser_id(int user_id) {
		this.user_id = user_id;
	}


	public int getTotal_picture() {
		return total_picture;
	}

	public void setTotal_picture(int total_picture) {
		this.total_picture = total_picture;
	}

	public int getFinished_picture() {
		return finished_picture;
	}

	public void setFinished_picture(int finished_picture) {
		this.finished_picture = finished_picture;
	}

	public int getTask_type() {
		return task_type;
	}

	public void setTask_type(int task_type) {
		this.task_type = task_type;
	}

	public String getRelate_task_id() {
		return relate_task_id;
	}

	public void setRelate_task_id(String relate_task_id) {
		this.relate_task_id = relate_task_id;
	}

	public String getRelate_task_name() {
		return relate_task_name;
	}

	public void setRelate_task_name(String relate_task_name) {
		this.relate_task_name = relate_task_name;
	}

	public String getTask_label_type_info() {
		return task_label_type_info;
	}

	public void setTask_label_type_info(String task_label_type_info) {
		this.task_label_type_info = task_label_type_info;
	}

	public int getAssign_user_id() {
		return assign_user_id;
	}

	public void setAssign_user_id(int assign_user_id) {
		this.assign_user_id = assign_user_id;
	}


	public String getRelate_other_label_task() {
		return relate_other_label_task;
	}

	public void setRelate_other_label_task(String relate_other_label_task) {
		this.relate_other_label_task = relate_other_label_task;
	}

	public int getTask_flow_type() {
		return task_flow_type;
	}

	public void setTask_flow_type(int task_flow_type) {
		this.task_flow_type = task_flow_type;
	}

	public int getVerify_user_id() {
		return verify_user_id;
	}

	public void setVerify_user_id(int verify_user_id) {
		this.verify_user_id = verify_user_id;
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

	public int getTotal_label() {
		return total_label;
	}

	public void setTotal_label(int total_label) {
		this.total_label = total_label;
	}


}
