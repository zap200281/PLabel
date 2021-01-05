package com.pcl.pojo.mybatis;

public class RetrainTaskMsgResult {
	  
	 private String id;
	 
	 
	 
	 
	 private String loss_train;
	 
	 private String epoch_num;
	 
	 private String epoch_total;
	 
	 private String step_num;
	 
	 private String step_total;
	 
	 private String learning_rate;
	 
	 private String accuracy_rate_train;
	 
	 private String item_add_time;
	 
	 private String item_cur_time;
	 
	 private int alg_model_id;
	 
	 private String lr;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLoss_train() {
		return loss_train;
	}

	public void setLoss_train(String loss_train) {
		this.loss_train = loss_train;
	}

	public String getEpoch_num() {
		return epoch_num;
	}

	public void setEpoch_num(String epoch_num) {
		this.epoch_num = epoch_num;
	}

	public String getEpoch_total() {
		return epoch_total;
	}

	public void setEpoch_total(String epoch_total) {
		this.epoch_total = epoch_total;
	}

	public String getStep_num() {
		return step_num;
	}

	public void setStep_num(String step_num) {
		this.step_num = step_num;
	}

	public String getStep_total() {
		return step_total;
	}

	public void setStep_total(String step_total) {
		this.step_total = step_total;
	}

	public String getLearning_rate() {
		return learning_rate;
	}

	public void setLearning_rate(String learning_rate) {
		this.learning_rate = learning_rate;
	}

	public String getAccuracy_rate_train() {
		return accuracy_rate_train;
	}

	public void setAccuracy_rate_train(String accuracy_rate_train) {
		this.accuracy_rate_train = accuracy_rate_train;
	}

	public String getItem_add_time() {
		return item_add_time;
	}

	public void setItem_add_time(String item_add_time) {
		this.item_add_time = item_add_time;
	}

	public String getItem_cur_time() {
		return item_cur_time;
	}

	public void setItem_cur_time(String item_cur_time) {
		this.item_cur_time = item_cur_time;
	}

	public int getAlg_model_id() {
		return alg_model_id;
	}

	public void setAlg_model_id(int alg_model_id) {
		this.alg_model_id = alg_model_id;
	}

	public String getLr() {
		return lr;
	}

	public void setLr(String lr) {
		this.lr = lr;
	}
	 
}
