package com.pcl.pojo.display;

import java.util.List;

public class DisplayReportMeasure {

	private int user_id;
	
	private String user_name;
	
	private List<MeasureData> dataList;//列表长度代表了纵坐标的长度

	public int getUser_id() {
		return user_id;
	}

	public void setUser_id(int user_id) {
		this.user_id = user_id;
	}

	public String getUser_name() {
		return user_name;
	}

	public void setUser_name(String user_name) {
		this.user_name = user_name;
	}

	public List<MeasureData> getDataList() {
		return dataList;
	}

	public void setDataList(List<MeasureData> dataList) {
		this.dataList = dataList;
	}
	
}
