package com.pcl.pojo;

import java.io.Serializable;
import java.util.List;

public class PageResult implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7259528465591542090L;

	private int total;
	
	private int current;
	
	private List<?> data;

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public int getCurrent() {
		return current;
	}

	public void setCurrent(int current) {
		this.current = current;
	}


	public List<?> getData() {
		return data;
	}

	public void setData(List<?> data) {
		this.data = data;
	}
	
	
}
