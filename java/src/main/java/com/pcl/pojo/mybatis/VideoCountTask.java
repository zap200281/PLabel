package com.pcl.pojo.mybatis;

public class VideoCountTask {

	private String id;
	private String task_name;
	private String dataset_id;
	private String task_add_time;
	private String task_finish_time;
	private int task_status;
	private String zip_object_name;
	private String zip_bucket_name;
	private int user_id;
	private int assign_user_id;
	private String task_status_desc;
	
	private String user;
	private String assign_user;
	
	private int verify_user_id;
	
	private String mainVideoInfo;
	
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

	
	public String getTask_finish_time() {
		return task_finish_time;
	}
	public void setTask_finish_time(String task_finish_time) {
		this.task_finish_time = task_finish_time;
	}

	public int getUser_id() {
		return user_id;
	}
	public void setUser_id(int user_id) {
		this.user_id = user_id;
	}
	public int getAssign_user_id() {
		return assign_user_id;
	}
	public void setAssign_user_id(int assign_user_id) {
		this.assign_user_id = assign_user_id;
	}
	public String getTask_status_desc() {
		return task_status_desc;
	}
	public void setTask_status_desc(String task_status_desc) {
		this.task_status_desc = task_status_desc;
	}
	public String getTask_add_time() {
		return task_add_time;
	}
	public void setTask_add_time(String task_add_time) {
		this.task_add_time = task_add_time;
	}
	public String getDataset_id() {
		return dataset_id;
	}
	public void setDataset_id(String dataset_id) {
		this.dataset_id = dataset_id;
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
	public String getZip_object_name() {
		return zip_object_name;
	}
	public void setZip_object_name(String zip_object_name) {
		this.zip_object_name = zip_object_name;
	}
	public String getZip_bucket_name() {
		return zip_bucket_name;
	}
	public void setZip_bucket_name(String zip_bucket_name) {
		this.zip_bucket_name = zip_bucket_name;
	}
	public int getTask_status() {
		return task_status;
	}
	public void setTask_status(int task_status) {
		this.task_status = task_status;
	}
	public int getVerify_user_id() {
		return verify_user_id;
	}
	public void setVerify_user_id(int verify_user_id) {
		this.verify_user_id = verify_user_id;
	}
	public String getMainVideoInfo() {
		return mainVideoInfo;
	}
	public void setMainVideoInfo(String mainVideoInfo) {
		this.mainVideoInfo = mainVideoInfo;
	}

}
