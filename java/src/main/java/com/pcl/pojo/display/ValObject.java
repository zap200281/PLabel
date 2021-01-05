package com.pcl.pojo.display;

public class ValObject {

	public int getEpoch() {
		return epoch;
	}

	public void setEpoch(int epoch) {
		this.epoch = epoch;
	}

	public double getBbox_mAP_50() {
		return bbox_mAP_50;
	}

	public void setBbox_mAP_50(double bbox_mAP_50) {
		this.bbox_mAP_50 = bbox_mAP_50;
	}

	private int epoch;
	
	private double bbox_mAP_50;

}
