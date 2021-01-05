package com.pcl.pojo.body;

import java.io.Serializable;
import java.util.List;

public class ReportBody implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int startPage;
	
	private int pageSize;
	
	private List<Object> user_id;
	
	private String lastDay;
	
	private String startTime;
	
	private String endTime;

	public int getStartPage() {
		return startPage;
	}

	public void setStartPage(int startPage) {
		this.startPage = startPage;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}



	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public String getLastDay() {
		return lastDay;
	}

	public void setLastDay(String lastDay) {
		this.lastDay = lastDay;
	}

	public List<Object> getUser_id() {
		return user_id;
	}

	public void setUser_id(List<Object> user_id) {
		this.user_id = user_id;
	}

}
