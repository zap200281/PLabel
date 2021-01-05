package com.pcl.pojo.display;

public class DisplayReIDTask {
	
	private String id;
	
	private String task_name;
	
	private String task_start_time;
	
	private String relate_task_name;
	
	private String user;
	
	private String assign_user;
	
	private String task_status ="";
	
	private String task_status_desc ="";
	
	private int costTime;
	
	private int task_type;
	
	private String task_label_type_info;
	
	private int task_flow_type;
	
	//审核模式下，在界面可以直接调出其它用户的标注
	private String relate_other_label_task;
	
	private String src_predict_taskid;
	
	private String dest_predict_taskid;
	
	private int reid_obj_type;

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



	public String getRelate_task_name() {
		return relate_task_name;
	}

	public void setRelate_task_name(String relate_task_name) {
		this.relate_task_name = relate_task_name;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getAssign_user() {
		return assign_user;
	}

	public void setAssign_user(String assign_user) {
		this.assign_user = assign_user;
	}

	public String getTask_status() {
		return task_status;
	}

	public void setTask_status(String task_status) {
		this.task_status = task_status;
	}

	public int getTask_type() {
		return task_type;
	}

	public void setTask_type(int task_type) {
		this.task_type = task_type;
	}

	public String getTask_label_type_info() {
		return task_label_type_info;
	}

	public void setTask_label_type_info(String task_label_type_info) {
		this.task_label_type_info = task_label_type_info;
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

	public String getSrc_predict_taskid() {
		return src_predict_taskid;
	}

	public void setSrc_predict_taskid(String src_predict_taskid) {
		this.src_predict_taskid = src_predict_taskid;
	}

	public String getDest_predict_taskid() {
		return dest_predict_taskid;
	}

	public void setDest_predict_taskid(String dest_predict_taskid) {
		this.dest_predict_taskid = dest_predict_taskid;
	}

	public String getTask_start_time() {
		return task_start_time;
	}

	public void setTask_start_time(String task_start_time) {
		this.task_start_time = task_start_time;
	}

	public int getReid_obj_type() {
		return reid_obj_type;
	}

	public void setReid_obj_type(int reid_obj_type) {
		this.reid_obj_type = reid_obj_type;
	}

	public String getTask_status_desc() {
		return task_status_desc;
	}

	public void setTask_status_desc(String task_status_desc) {
		this.task_status_desc = task_status_desc;
	}

	public int getCostTime() {
		return costTime;
	}

	public void setCostTime(int costTime) {
		this.costTime = costTime;
	}
}
