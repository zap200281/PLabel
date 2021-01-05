package com.pcl.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pcl.dao.AlgInstanceDao;
import com.pcl.dao.AlgModelDao;
import com.pcl.pojo.mybatis.AlgInstance;
import com.pcl.pojo.mybatis.AlgModel;
import com.pcl.util.TimeUtil;

@Service
public class AlgService {

	private static Logger logger = LoggerFactory.getLogger(AlgService.class);

	@Autowired
	private AlgModelDao algModelDao;
	
	@Autowired
	private AlgInstanceDao algInstanceDao;
	
	
	public int addAlgModel(AlgModel algModel) {
		logger.info("start add alg model, modelName=" + algModel.getModel_name());
		return algModelDao.addAlgModel(algModel);
	}
	
	public int deleteAlgModel(int id) {
		logger.info("start delete alg model, alg model id=" + id);
		return algModelDao.delete(id);
	}
	
	
	public List<AlgModel> queryAlgModel() {
		logger.info("start query all alg model");
		List<AlgModel> reList = algModelDao.queryAlgModelAll();
		for(AlgModel algModel : reList) {
			algModel.setExec_script(null);
		}
		return reList;
	}
	
	
	public List<AlgModel> queryAlgModelContainWiseMedical() {
		logger.info("start query queryAlgModelContainWiseMedical ");
		List<AlgModel> reList = algModelDao.queryAlgModelContainWiseMedical();
		for(AlgModel algModel : reList) {
			algModel.setExec_script(null);
		}
		return reList;
	}
	
	
	public int deleteAlgInstance(int id) {
		logger.info("start delete alg instance, alg instance id=" + id);
		return algInstanceDao.delete(id);
	}
	
	
	public int addAlgInstance(AlgInstance algInstance) {
		logger.info("start add alg instance, alg name=" + algInstance.getAlg_name());
		
		algInstance.setAdd_time(TimeUtil.getCurrentTimeStr());
		
		return algInstanceDao.addAlgInstance(algInstance);
	}

	public List<AlgModel> queryAlgModelForRetrain() {
		List<AlgModel> dbList = algModelDao.queryAlgModelAll();
		List<AlgModel> returnList = new ArrayList<>();
		for(AlgModel algModel : dbList) {
			if(algModel.getTrain_script() != null) {
				returnList.add(algModel);
			}
		}
		return returnList;
	}

	public List<AlgModel> queryAlgModelForTracking() {
		List<AlgModel> dbList = algModelDao.queryAlgModelForTracking();
		
		return dbList;
	}
	
	
	public List<AlgModel> queryAlgModelForAutoLabel() {
		List<AlgModel> dbList = algModelDao.queryAlgModelForAutoLabel();
		
		return dbList;
	}
	
	public List<AlgModel> queryAlgModelForHandLabel() {
		List<AlgModel> dbList = algModelDao.queryAlgModelForHandLabel();
		
		return dbList;
	}
	
}
