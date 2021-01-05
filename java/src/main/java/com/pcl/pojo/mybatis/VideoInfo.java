package com.pcl.pojo.mybatis;

public class VideoInfo {

	private String id;
	private String dataset_id;
	private String minio_url;
	private String video_info;//frame info
	private String camera_number;
	private String camera_gps;
	private String camera_date;
	private String duration;
	private String startTime;
	private String bitrate;
	private String videoCode;
	private String videoFormat;
	private String resolutionRatio;
	private String audioCode;
	private String audioFrequncy;
	private String fps;
	
	private String tmpVideoPath;
	private String tmpFramePath;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getDataset_id() {
		return dataset_id;
	}
	public void setDataset_id(String dataset_id) {
		this.dataset_id = dataset_id;
	}
	public String getMinio_url() {
		return minio_url;
	}
	public void setMinio_url(String minio_url) {
		this.minio_url = minio_url;
	}
	public String getVideo_info() {
		return video_info;
	}
	public void setVideo_info(String video_info) {
		this.video_info = video_info;
	}
	public String getCamera_number() {
		return camera_number;
	}
	public void setCamera_number(String camera_number) {
		this.camera_number = camera_number;
	}
	public String getCamera_gps() {
		return camera_gps;
	}
	public void setCamera_gps(String camera_gps) {
		this.camera_gps = camera_gps;
	}
	public String getCamera_date() {
		return camera_date;
	}
	public void setCamera_date(String camera_date) {
		this.camera_date = camera_date;
	}
	public String getDuration() {
		return duration;
	}
	public void setDuration(String duration) {
		this.duration = duration;
	}
	public String getStartTime() {
		return startTime;
	}
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}
	public String getBitrate() {
		return bitrate;
	}
	public void setBitrate(String bitrate) {
		this.bitrate = bitrate;
	}
	public String getVideoCode() {
		return videoCode;
	}
	public void setVideoCode(String videoCode) {
		this.videoCode = videoCode;
	}
	public String getVideoFormat() {
		return videoFormat;
	}
	public void setVideoFormat(String videoFormat) {
		this.videoFormat = videoFormat;
	}
	public String getResolutionRatio() {
		return resolutionRatio;
	}
	public void setResolutionRatio(String resolutionRatio) {
		this.resolutionRatio = resolutionRatio;
	}
	public String getAudioCode() {
		return audioCode;
	}
	public void setAudioCode(String audioCode) {
		this.audioCode = audioCode;
	}
	public String getAudioFrequncy() {
		return audioFrequncy;
	}
	public void setAudioFrequncy(String audioFrequncy) {
		this.audioFrequncy = audioFrequncy;
	}
	public String getFps() {
		return fps;
	}
	public void setFps(String fps) {
		this.fps = fps;
	}
	public String getTmpVideoPath() {
		return tmpVideoPath;
	}
	public void setTmpVideoPath(String tmpVideoPath) {
		this.tmpVideoPath = tmpVideoPath;
	}
	public String getTmpFramePath() {
		return tmpFramePath;
	}
	public void setTmpFramePath(String tmpFramePath) {
		this.tmpFramePath = tmpFramePath;
	}

}
