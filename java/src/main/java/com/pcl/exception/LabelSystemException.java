package com.pcl.exception;

public class LabelSystemException extends Exception {

	
	private int errorcode;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2072120204206054361L;
	
	public LabelSystemException(String message) {
		super(message);
	}
	

	public LabelSystemException(int errorcode, String message) {
		super(message);
		this.errorcode = errorcode;
	}


	public int getErrorcode() {
		return errorcode;
	}


}
