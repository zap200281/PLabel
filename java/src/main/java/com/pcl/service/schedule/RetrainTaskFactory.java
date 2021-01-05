package com.pcl.service.schedule;

import com.pcl.pojo.mybatis.RetrainTask;

public class RetrainTaskFactory {

	public static ATask getRetrainTask(RetrainTask retrainTask) {
		
		return new MMDetectionRetrain();
		
	}
	
}
