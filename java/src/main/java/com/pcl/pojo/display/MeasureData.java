package com.pcl.pojo.display;

public class MeasureData {

	private int rectNum;//新建和更新的框数量
	
	private int propertiesNum;//设置的属性数量
	
	private int pictureNum;//处理的图片数量
	
	private int notValideNum;//不合格的标框数量
	
	private String index;

	public int getRectNum() {
		return rectNum;
	}

	public void setRectNum(int rectNum) {
		this.rectNum = rectNum;
	}

	public int getPropertiesNum() {
		return propertiesNum;
	}

	public void setPropertiesNum(int propertiesNum) {
		this.propertiesNum = propertiesNum;
	}

	public int getPictureNum() {
		return pictureNum;
	}

	public void setPictureNum(int pictureNum) {
		this.pictureNum = pictureNum;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public int getNotValideNum() {
		return notValideNum;
	}

	public void setNotValideNum(int notValideNum) {
		this.notValideNum = notValideNum;
	}



}
