package com.pcl.pojo.body;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PrePredictTaskBody implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@JsonProperty(value="task_name")
	private String taskName;
	

	@JsonProperty(value="alg_model")
	private int algModel;
	
	@JsonProperty(value="dataset_id")
	private String dataSetId;
	
	@JsonProperty(value="delete_no_label_picture")
	private int deleteNoLabelPicture;
	
	@JsonProperty(value="score_threshhold")
	private double score_threshhold;
	
	@JsonProperty(value="needToDistiguishTypeOrColor")
	private int needToDistiguishTypeOrColor;
	
	@JsonProperty(value="delete_similar_picture")
	private int delete_similar_picture;
	
	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}


	public int getAlgModel() {
		return algModel;
	}

	public void setAlgModel(int algModel) {
		this.algModel = algModel;
	}

	public String getDataSetId() {
		return dataSetId;
	}

	public void setDataSetId(String dataSetId) {
		this.dataSetId = dataSetId;
	}

	public int getDeleteNoLabelPicture() {
		return deleteNoLabelPicture;
	}

	public void setDeleteNoLabelPicture(int deleteNoLabelPicture) {
		this.deleteNoLabelPicture = deleteNoLabelPicture;
	}

	public double getScore_threshhold() {
		return score_threshhold;
	}

	public void setScore_threshhold(double score_threshhold) {
		this.score_threshhold = score_threshhold;
	}

	public int getNeedToDistiguishTypeOrColor() {
		return needToDistiguishTypeOrColor;
	}

	public void setNeedToDistiguishTypeOrColor(int needToDistiguishTypeOrColor) {
		this.needToDistiguishTypeOrColor = needToDistiguishTypeOrColor;
	}

	public int getDelete_similar_picture() {
		return delete_similar_picture;
	}

	public void setDelete_similar_picture(int delete_similar_picture) {
		this.delete_similar_picture = delete_similar_picture;
	}

}
