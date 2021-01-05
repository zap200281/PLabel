package com.pcl.pojo.display;

import java.util.List;

public class DisplayReIDTaskShowResult {

	private String reIdName;
	
	private List<String> imgList;
	
	private List<String> itemIdList;

	public String getReIdName() {
		return reIdName;
	}

	public void setReIdName(String reIdName) {
		this.reIdName = reIdName;
	}

	public List<String> getImgList() {
		return imgList;
	}

	public void setImgList(List<String> imgList) {
		this.imgList = imgList;
	}

	public List<String> getItemIdList() {
		return itemIdList;
	}

	public void setItemIdList(List<String> itemIdList) {
		this.itemIdList = itemIdList;
	}


}
