package com.pcl.pojo.body;

import java.util.List;

public class ReportMeasureBody {

	private int measureType;  //度量维度，0,代表天， 1，代表周，2代表月
	
	private int measureValue;  //在选择度量维度的情况下， 多长时间的数据,
	                      //比如measureType=0，measureValue=10，则代表显示以每天的工作量进行比较。即纵坐标是每天工作量值，横坐标单位是天，最大值为10
	                      // 比如measureType=2，measureValue=12,则代表显示每个月的工作量进行比较。即纵坐标是每个月的工作量值，横坐标单位是月，最大值为12.
	private List<Object> user_id;   //用户ID值，可以多个，如果多个，则每个用户2维图表合并显示，即每个横坐标上的柱子有多个。
	
	public int getMeasureType() {
		return measureType;
	}
	public void setMeasureType(int measureType) {
		this.measureType = measureType;
	}
	public int getMeasureValue() {
		return measureValue;
	}
	public void setMeasureValue(int measureValue) {
		this.measureValue = measureValue;
	}
	public List<Object> getUser_id() {
		return user_id;
	}
	public void setUser_id(List<Object> user_id) {
		this.user_id = user_id;
	}
}
