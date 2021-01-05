package com.pcl.pojo.display;

import java.util.List;
import java.util.Map;

public class DisplayReIdTaskResult {

	private String taskName;//归属的任务名称 
	
	private List<Map<String,String>> imageInfoList;//每个元素是一张图片，key=为图片路径，value=labeltaskitem.id

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public List<Map<String,String>> getImageInfoList() {
		return imageInfoList;
	}

	public void setImageInfoList(List<Map<String,String>> imageInfoList) {
		this.imageInfoList = imageInfoList;
	}
	
}
