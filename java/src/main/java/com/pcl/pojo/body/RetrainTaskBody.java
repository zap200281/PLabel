package com.pcl.pojo.body;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RetrainTaskBody implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@JsonProperty(value="task_name")
	private String taskName;
	
	@JsonProperty(value="alg_model")
	private int algModel;
	
	@JsonProperty(value="pre_predict_task")
	private String prePredictTaskId;
	
	@JsonProperty(value="retrain_type")
	private String retrain_type;
	
	@JsonProperty(value="retrain_data")
	private String retrain_data;
	
	@JsonProperty(value="detection_type")
	private String detection_type;
	
	@JsonProperty(value="detection_type_input")
	private String detection_type_input;
	
	@JsonProperty(value="retrain_model_name")
	private String retrain_model_name;
	
	//测试集，训练集数据比率
	@JsonProperty(value="test_train_ratio")
	private double testTrainRatio;

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

	public String getPrePredictTaskId() {
		return prePredictTaskId;
	}

	public void setPrePredictTaskId(String prePredictTaskId) {
		this.prePredictTaskId = prePredictTaskId;
	}

	public String getRetrain_type() {
		return retrain_type;
	}

	public void setRetrain_type(String retrain_type) {
		this.retrain_type = retrain_type;
	}

	public String getRetrain_data() {
		return retrain_data;
	}

	public void setRetrain_data(String retrain_data) {
		this.retrain_data = retrain_data;
	}

	public String getDetection_type() {
		return detection_type;
	}

	public void setDetection_type(String detection_type) {
		this.detection_type = detection_type;
	}

	public String getDetection_type_input() {
		return detection_type_input;
	}

	public void setDetection_type_input(String detection_type_input) {
		this.detection_type_input = detection_type_input;
	}

	public String getRetrain_model_name() {
		return retrain_model_name;
	}

	public void setRetrain_model_name(String retrain_model_name) {
		this.retrain_model_name = retrain_model_name;
	}

	public double getTestTrainRatio() {
		return testTrainRatio;
	}

	public void setTestTrainRatio(double testTrainRatio) {
		this.testTrainRatio = testTrainRatio;
	}


}
