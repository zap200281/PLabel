package com.pcl.pojo.body;

import java.io.Serializable;

public class DramFrameBody implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String dateset_id;
	
	private String fps; //抽帧速率
	
	private int drawFrameType;
	
	private String fileNameFormate;
	
	private String baseDate;//视频基准时间
	
	private String filenamePrefix;
	
	private String widthHeight;
	
	private int isDeleteVideo;

	private int createAutoLabelTask;//为1，表示创建自动标注任务，使用Yolov3标注人，并删除空白图片
	
	public String getDateset_id() {
		return dateset_id;
	}

	public void setDateset_id(String dateset_id) {
		this.dateset_id = dateset_id;
	}

	public String getFps() {
		return fps;
	}

	public void setFps(String fps) {
		this.fps = fps;
	}

	public int getDrawFrameType() {
		return drawFrameType;
	}

	public void setDrawFrameType(int drawFrameType) {
		this.drawFrameType = drawFrameType;
	}

	public String getFileNameFormate() {
		return fileNameFormate;
	}

	public void setFileNameFormate(String fileNameFormate) {
		this.fileNameFormate = fileNameFormate;
	}

	public String getBaseDate() {
		return baseDate;
	}

	public void setBaseDate(String baseDate) {
		this.baseDate = baseDate;
	}

	public String getWidthHeight() {
		return widthHeight;
	}

	public void setWidthHeight(String widthHeight) {
		this.widthHeight = widthHeight;
	}

	public int getIsDeleteVideo() {
		return isDeleteVideo;
	}

	public void setIsDeleteVideo(int isDeleteVideo) {
		this.isDeleteVideo = isDeleteVideo;
	}

	public int getCreateAutoLabelTask() {
		return createAutoLabelTask;
	}

	public void setCreateAutoLabelTask(int createAutoLabelTask) {
		this.createAutoLabelTask = createAutoLabelTask;
	}

	public String getFilenamePrefix() {
		return filenamePrefix;
	}

	public void setFilenamePrefix(String filenamePrefix) {
		this.filenamePrefix = filenamePrefix;
	}

}
