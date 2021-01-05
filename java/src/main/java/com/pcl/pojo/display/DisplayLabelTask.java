package com.pcl.pojo.display;

public class DisplayLabelTask {

	private String id;
	
	private String task_name;
	
	private String task_add_time;
	
	private String relate_task_name;
	
	private String relate_task_id;
	
	private String user;
	
	private String assign_user;
	
	private String task_status_desc ="";
	
	private int task_status;
	
	private int task_type;
	
	private String task_label_type_info;
	
	private int task_flow_type;
	
	//审核模式下，在界面可以直接调出其它用户的标注
	private String relate_other_label_task;
	
	private String verify_user="";
	
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


	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}



	public int getTask_type() {
		return task_type;
	}

	public void setTask_type(int task_type) {
		this.task_type = task_type;
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

	public String getAssign_user() {
		return assign_user;
	}

	public void setAssign_user(String assign_user) {
		this.assign_user = assign_user;
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

	public String getVerify_user() {
		return verify_user;
	}

	public void setVerify_user(String verify_user) {
		this.verify_user = verify_user;
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

	public int getTotal_label() {
		return total_label;
	}

	public void setTotal_label(int total_label) {
		this.total_label = total_label;
	}

	public String getRelate_task_id() {
		return relate_task_id;
	}

	public void setRelate_task_id(String relate_task_id) {
		this.relate_task_id = relate_task_id;
	}
}
