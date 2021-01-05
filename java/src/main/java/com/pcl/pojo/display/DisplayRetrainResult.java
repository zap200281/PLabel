package com.pcl.pojo.display;

import java.util.List;

public class DisplayRetrainResult {

	private String id;
	
	private List<ValObject> valInfo;
	
	private List<TrainObject> trainInfo;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<ValObject> getValInfo() {
		return valInfo;
	}

	public void setValInfo(List<ValObject> valInfo) {
		this.valInfo = valInfo;
	}

	public List<TrainObject> getTrainInfo() {
		return trainInfo;
	}

	public void setTrainInfo(List<TrainObject> trainInfo) {
		this.trainInfo = trainInfo;
	}

}
