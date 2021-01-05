package com.pcl.pojo.dzi;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TitleSource implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6525806954714788667L;
	@JsonProperty(value="Image")
	private ResData image;

	public ResData getImage() {
		return image;
	}

	public void setImage(ResData image) {
		this.image = image;
	}
}
