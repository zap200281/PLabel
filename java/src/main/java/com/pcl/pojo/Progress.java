package com.pcl.pojo;

public class Progress {

	private String id;
	
	private long startTime;//second
	
	private long totalTime;
	
	private long progress;
	
	private long base;
	
	private double ratio=1;
	
	private long exceedTime;//second
	
	private String info;
	
	private String relatedFileName;
	
	private int status;  //0：完成，1：进行中， 2：异常，3：超时
	
	private String taskId;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getTotalTime() {
		return totalTime;
	}

	public void setTotalTime(long totalTime) {
		this.totalTime = totalTime;
	}

	public long getExceedTime() {
		return exceedTime;
	}

	public void setExceedTime(long exceedTime) {
		this.exceedTime = exceedTime;
	}

	public long getProgress() {
		return progress;
	}

	public void setProgress(long progress) {
		this.progress = progress;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public String getRelatedFileName() {
		return relatedFileName;
	}

	public void setRelatedFileName(String relatedFileName) {
		this.relatedFileName = relatedFileName;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getTaskId() {
		return taskId;
	}

	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}

	public long getBase() {
		return base;
	}

	public void setBase(long base) {
		this.base = base;
	}

	public double getRatio() {
		return ratio;
	}

	public void setRatio(double ratio) {
		this.ratio = ratio;
	}


	
}
