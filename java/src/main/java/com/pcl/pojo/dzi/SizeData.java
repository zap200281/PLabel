package com.pcl.pojo.dzi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SizeData {
	
	@JsonProperty(value="Height")
    private String height;
	
	@JsonProperty(value="Width")
    private String width;
 
    public SizeData() {
    }

	public String getHeight() {
		return height;
	}

	public void setHeight(String height) {
		this.height = height;
	}

	public String getWidth() {
		return width;
	}

	public void setWidth(String width) {
		this.width = width;
	}
 

}
