package com.pcl.pojo.display;

import java.util.List;

public class ThreeObject {

	private String id;
	
	private List<List<Dot>> dotList;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<List<Dot>> getDotList() {
		return dotList;
	}

	public void setDotList(List<List<Dot>> dotList) {
		this.dotList = dotList;
	}
	
}
