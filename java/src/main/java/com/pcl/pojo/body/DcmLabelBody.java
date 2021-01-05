package com.pcl.pojo.body;

public class DcmLabelBody {

	private String direct;
	
	private int index;
	
	private String label_task_id;
	
	private String labelDcmItemTaskId; //用来区分某个组
	
	private String labelInfo;

	public String getDirect() {
		return direct;
	}

	public void setDirect(String direct) {
		this.direct = direct;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String getLabel_task_id() {
		return label_task_id;
	}

	public void setLabel_task_id(String label_task_id) {
		this.label_task_id = label_task_id;
	}

	public String getLabelDcmItemTaskId() {
		return labelDcmItemTaskId;
	}

	public void setLabelDcmItemTaskId(String labelDcmItemTaskId) {
		this.labelDcmItemTaskId = labelDcmItemTaskId;
	}

	public String getLabelInfo() {
		return labelInfo;
	}

	public void setLabelInfo(String labelInfo) {
		this.labelInfo = labelInfo;
	}
	
}
