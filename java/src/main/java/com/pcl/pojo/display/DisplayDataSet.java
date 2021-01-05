package com.pcl.pojo.display;

import java.util.List;

public class DisplayDataSet {

	private String id;
	private String task_name;
	private String task_desc;
	private String task_add_time;
	private int datasetType;
	private int total;
	private String user;
	private String assign_user;
	
	private int task_status;
	private String task_status_desc;
	
	private String camera_number;
	private String camera_gps;
	private String camera_date;
	private List<String> videoSet;
	
	private String zip_object_name;
	private String zip_bucket_name;
	
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
	public String getTask_desc() {
		return task_desc;
	}
	public void setTask_desc(String task_desc) {
		this.task_desc = task_desc;
	}
	public String getTask_add_time() {
		return task_add_time;
	}
	public void setTask_add_time(String task_add_time) {
		this.task_add_time = task_add_time;
	}
	public int getDatasetType() {
		return datasetType;
	}
	public void setDatasetType(int datasetType) {
		this.datasetType = datasetType;
	}
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
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
	public String getCamera_number() {
		return camera_number;
	}
	public void setCamera_number(String camera_number) {
		this.camera_number = camera_number;
	}
	public String getCamera_gps() {
		return camera_gps;
	}
	public void setCamera_gps(String camera_gps) {
		this.camera_gps = camera_gps;
	}
	public String getCamera_date() {
		return camera_date;
	}
	public void setCamera_date(String camera_date) {
		this.camera_date = camera_date;
	}
	public List<String> getVideoSet() {
		return videoSet;
	}
	public void setVideoSet(List<String> videoSet) {
		this.videoSet = videoSet;
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

	
}
