package com.pcl.service.schedule.retrain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pcl.dao.AlgInstanceDao;
import com.pcl.dao.AlgModelDao;
import com.pcl.dao.RetrainTaskDao;
import com.pcl.dao.UserDao;
import com.pcl.pojo.mybatis.AlgInstance;
import com.pcl.pojo.mybatis.AlgModel;
import com.pcl.pojo.mybatis.RetrainTask;
import com.pcl.service.schedule.PredictPersonProperty;

@Service
public class RetrainTaskFactory {

	@Autowired
	private MMDetectionRetrain mmdetectionRetrain;
	
	@Autowired
	private PersonPropertyRetrain personPropertyRetrain;
	
	@Autowired
	private AlgModelDao algModelDao;

	@Autowired
	private AlgInstanceDao algInstanceDao;

	
	public ATask getRetrainTask(RetrainTask retrainTask) {
		
		AlgModel algModel = algModelDao.queryAlgModelById(retrainTask.getAlg_model_id());
		if(algModel == null) {
			return null;
		}
		
		AlgInstance algInstance = algInstanceDao.queryAlgInstanceById(Integer.parseInt(algModel.getAlg_instance_id()));
		if(algInstance == null) {
			return null;
		}
		
		if(algInstance.getId() == 18) {//属性识别
			return personPropertyRetrain;
		}else {
			return mmdetectionRetrain;
		}
		
	}
	
}
