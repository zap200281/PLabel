package com.pcl.pojo;

import java.io.Serializable;

public class Result implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1397414930556820203L;

	private int code;
	
	private String message;

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
}
