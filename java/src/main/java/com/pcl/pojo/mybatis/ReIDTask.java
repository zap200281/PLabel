package com.pcl.pojo.mybatis;

public class ReIDTask {

	private String id;
	private String task_name;
	private String src_predict_taskid;
	private String dest_predict_taskid;
	private String task_start_time;
	private String task_finish_time;
	private int task_status;
	private int alg_model_id;
	private int user_id;
	private String src_bucket_name;
	private String dest_bucket_name;
	private String task_status_desc;
	
	private int assign_user_id;
	private String relate_other_label_task;
	private int task_flow_type;
	private int task_type;
	private String task_label_type_info;
	
	private int total_picture;
	private int finished_picture;
	
	private int reid_obj_type;
	
	private int verify_user_id;
	
	private int reid_auto_type;
	
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
	public int getUser_id() {
		return user_id;
	}
	public void setUser_id(int user_id) {
		this.user_id = user_id;
	}
	public String getSrc_bucket_name() {
		return src_bucket_name;
	}
	public void setSrc_bucket_name(String src_bucket_name) {
		this.src_bucket_name = src_bucket_name;
	}
	public String getDest_bucket_name() {
		return dest_bucket_name;
	}
	public void setDest_bucket_name(String dest_bucket_name) {
		this.dest_bucket_name = dest_bucket_name;
	}
	public String getTask_status_desc() {
		return task_status_desc;
	}
	public void setTask_status_desc(String task_status_desc) {
		this.task_status_desc = task_status_desc;
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
	public int getReid_obj_type() {
		return reid_obj_type;
	}
	public void setReid_obj_type(int reid_obj_type) {
		this.reid_obj_type = reid_obj_type;
	}
	public int getVerify_user_id() {
		return verify_user_id;
	}
	public void setVerify_user_id(int verify_user_id) {
		this.verify_user_id = verify_user_id;
	}
	public int getReid_auto_type() {
		return reid_auto_type;
	}
	public void setReid_auto_type(int reid_auto_type) {
		this.reid_auto_type = reid_auto_type;
	}
	
}
