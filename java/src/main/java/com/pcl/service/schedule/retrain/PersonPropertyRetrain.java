package com.pcl.service.schedule.retrain;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;
import com.pcl.constant.Constants;
import com.pcl.constant.UserConstants;
import com.pcl.dao.AlgInstanceDao;
import com.pcl.dao.AlgModelDao;
import com.pcl.dao.LabelTaskItemDao;
import com.pcl.dao.RetrainTaskDao;
import com.pcl.dao.UserDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.mybatis.AlgInstance;
import com.pcl.pojo.mybatis.AlgModel;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.pojo.mybatis.RetrainTask;
import com.pcl.pojo.mybatis.User;
import com.pcl.service.LabelDataSetMerge;
import com.pcl.service.ObjectFileService;
import com.pcl.service.TokenManager;
import com.pcl.util.CocoAnnotationsUtil;
import com.pcl.util.ImageCutUtil;
import com.pcl.util.JsonUtil;
import com.pcl.util.ProcessExeUtil;
import com.pcl.util.ReIDUtil;
import com.pcl.util.TimeUtil;

@Service
public class PersonPropertyRetrain extends ATask{

	private static Logger logger = LoggerFactory.getLogger(PersonPropertyRetrain.class);

	@Autowired
	private AlgModelDao algModelDao;

	@Autowired
	private RetrainTaskDao retrainTaskDao;

	@Autowired
	private UserDao userDao;

	@Autowired
	private AlgInstanceDao algInstanceDao;

	@Autowired
	private LabelTaskItemDao labelTaskItemDao;

	@Autowired
	private ObjectFileService fileService;

	@Override
	public void doExecute(RetrainTask retrainTask, List<Integer> availableGpuIdList) {
		String algRootPath = null;
		String time = TimeUtil.getCurrentTimeStrByyyyyMMddHHmmss();
		String dataSetPath = null;
		try {
			//更新数据库状态为  进行中
			updateTaskProgressing(retrainTask.getId(),retrainTaskDao);

			AlgModel algModel = algModelDao.queryAlgModelById(retrainTask.getAlg_model_id());
			if(algModel == null) {
				logger.info("algModel is null. return. id=" + retrainTask.getAlg_model_id());
				return;
			}

			AlgInstance algInstance = algInstanceDao.queryAlgInstanceById(Integer.parseInt(algModel.getAlg_instance_id()));
			if(algInstance == null) {
				logger.info("algInstance is null. return. id=" + algModel.getAlg_instance_id());
				return;
			}

			algRootPath = LabelDataSetMerge.getAlgRootPath(algInstance.getAlg_root_dir());

			String trainScript = algModel.getTrain_script();
			dataSetPath = algRootPath + "data" +File.separator + time + File.separator;
			String trainDataSetPath = dataSetPath + "train"+ File.separator;
			String valDataSetPath = dataSetPath + "val"+ File.separator;
			//String imageDir =  dataSetPath + "image" + File.separator;
			//String jsonLabelDir =  dataSetPath + "label" + File.separator;
			new File(trainDataSetPath).mkdirs();
			new File(valDataSetPath).mkdirs();
			//下载图片及写标注信息
			collectImageToDir(retrainTask, trainDataSetPath, valDataSetPath);

			trainScript = trainScript.replace("{data_dir}", dataSetPath);

			//if(RETRAIN.equals(retrainTask.getRetrain_type())){//重训的时候，pretainModel为原来的模型
			trainScript = trainScript.replace("{pretainModel}", algModel.getModel_url());
			//}
			
			int gpunum = 0;
			if(availableGpuIdList.size() > 1) {
				if(availableGpuIdList.size() % 2 == 0) {
					gpunum = availableGpuIdList.size() - 2;
				}else {
					gpunum = availableGpuIdList.size() - 1;
				}
			}
			if(gpunum == 0) {
				gpunum = 1;
			}
			String saveDir = dataSetPath + "result";
			trainScript = trainScript.replace("{save_dir}", saveDir);
			
			String	script =  trainScript.replace("{gpunum}", String.valueOf(gpunum));
			
			
			
			Map<String,Object> paramMap = new HashMap<>();
			paramMap.put("id", retrainTask.getId());
			//更新数据库状态为  进行中
			paramMap.put("task_status", Constants.RETRAINTASK_STATUS_PROGRESSING);
			paramMap.put("confPath", saveDir);
			retrainTaskDao.updateRetrainTask(paramMap);
	
			logger.info("script=" + script);

			ProcessExeUtil.execScript(script, algRootPath, 3600 * 24 * 2);

			//正常结束，更新状态
			updateTaskFinish(retrainTask.getId(),Constants.RETRAINTASK_STATUS_FINISHED,"",retrainTaskDao);

			//将训练好的模型写到数据库中待用
			copyLastModelToRightPath(retrainTask, algRootPath, saveDir);
		}catch (Exception e) {
			updateTaskFinish(retrainTask.getId(),Constants.RETRAINTASK_STATUS_EXCEPTION,e.getMessage(),retrainTaskDao);
			logger.info(e.getMessage(),e);
		}

	}


	private void copyLastModelToRightPath(RetrainTask retrainTask, String algRootPath,String saveDir) {
		String newModel = saveDir + File.separator + "retrain" + File.separator + "img_model" + File.separator + "ckpt_retrain.pth";
	
		AlgModel algModelSrc = algModelDao.queryAlgModelById(retrainTask.getAlg_model_id());

		User user = userDao.queryUserById(retrainTask.getUser_id());
		String userName = user.getUsername();

		//往模型库插入一条记录
		
		String modelName = algModelSrc.getModel_name() + "(" + userName + ")";
		if(retrainTask.getRetrain_model_name() != null) {
			modelName = retrainTask.getRetrain_model_name();
		}
		List<AlgModel> modelList = algModelDao.queryAlgModel(modelName);
		if(modelList != null && modelList.size() > 0) {
			for(AlgModel tmpAlgModel : modelList) {
				algModelDao.delete(tmpAlgModel.getId());
			}
		}

		AlgModel algModelDest = new AlgModel();
		algModelDest.setExec_script(algModelSrc.getExec_script());
		algModelDest.setModel_name(modelName);
		algModelDest.setModel_type(2);
		algModelDest.setConf_path(algModelSrc.getConf_path());
		algModelDest.setModel_url(newModel.substring(algRootPath.length()));
		algModelDest.setTrain_script(algModelSrc.getTrain_script());
		algModelDest.setAlg_instance_id(algModelSrc.getAlg_instance_id());
		
		algModelDao.addAlgModel(algModelDest);

		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("modelPath", algModelDest.getModel_url());
		paramMap.put("id", retrainTask.getId());
		paramMap.put("task_status", Constants.RETRAINTASK_STATUS_FINISHED);
		retrainTaskDao.updateRetrainTask(paramMap);
	}


	private void collectImageToDir(RetrainTask retrainTask,String trainDataSetPath,String valDataSetPath) {


		String retrainData = retrainTask.getRetrain_data();
		List<String> labelTaskIdList = JsonUtil.getList(retrainData);

		String trainDataSetImagePath = trainDataSetPath + "image" + File.separator;
		String valDataSetImagePath = valDataSetPath + "image" + File.separator;
		new File(trainDataSetImagePath).mkdirs();
		new File(valDataSetImagePath).mkdirs();
		
		String trainDataSetLabelPath = trainDataSetPath + "label" + File.separator + "result.json";
		String valDataSetLabelPath = valDataSetPath + "label" + File.separator + "result.json";
		new File(trainDataSetLabelPath).getParentFile().mkdirs();
		new File(valDataSetLabelPath).getParentFile().mkdirs();
		
		Map<String,Map<String,String>> trainLabelMap = new HashMap<>();
		Map<String,Map<String,String>> valLabelMap = new HashMap<>();
		logger.info("labelTaskIdList=" + retrainData);
		
		for(String labelTaskId : labelTaskIdList) {
			List<LabelTaskItem> itemList = labelTaskItemDao.queryLabelTaskItemByLabelTaskId(TokenManager.getUserTablePos(retrainTask.getUser_id(), UserConstants.LABEL_TASK_SINGLE_TABLE),labelTaskId);
			int size = itemList.size();
			logger.info("item size=" + size);
			for(int i = 0; i < size ;i++) {
				LabelTaskItem item = itemList.get(i);
				if(i % 10 == 0) {
					ImageCutUtil.cutImageToPath(item, valDataSetImagePath, fileService);
					dealLabelJson(item,valLabelMap);
				}else {
					ImageCutUtil.cutImageToPath(item, trainDataSetImagePath, fileService);
					dealLabelJson(item,trainLabelMap);
				}
			}
		}

		writeLabelJsonToFile(trainLabelMap,trainDataSetLabelPath);
		writeLabelJsonToFile(valLabelMap,valDataSetLabelPath);

	}


	private void writeLabelJsonToFile(Map<String, Map<String, String>> trainLabelMap, String trainDataSetLabelPath) {
		String jsonContent = JsonUtil.toJson(trainLabelMap);
		logger.info("jsonContent=" + jsonContent);
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(trainDataSetLabelPath));) {
			writer.write(jsonContent);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void dealLabelJson(LabelTaskItem item, Map<String,Map<String,String>> labelMap) {
		String jsonLabelInfo = item.getLabel_info();
		ArrayList<Map<String,Object>> labelList = JsonUtil.getLabelList(jsonLabelInfo);
		if(labelList.isEmpty()) {
			logger.info("jsonLabelInfo is empty. jsonLabelInfo=" + jsonLabelInfo);
			return;
		}else {
			//logger.info("jsonLabelInfo is not empty. jsonLabelInfo=" + jsonLabelInfo);
		}
		for(Map<String,Object> label : labelList) {
			String id = label.get("id").toString();
			String type = CocoAnnotationsUtil.getClassName(label);
			//logger.info("type=" + type);
			if("person".equals(type)) {
				String jsonFileName = ReIDUtil.getLabel(item.getPic_image_field(), id);
				Map<String,String> tmpMap = new HashMap<>();
				Object other = label.get("other");
				if(other != null && other instanceof Map) {
					Object region_attributesObj = ((Map<String,Object>)other).get("region_attributes");
					if(region_attributesObj != null && region_attributesObj instanceof Map) {
						Map<String,Object> region_attributesMap = (Map<String,Object>)region_attributesObj;
						collectPersonProperty("property",region_attributesMap,tmpMap);
						collectPersonProperty("color",region_attributesMap,tmpMap);
						collectPersonProperty("otherprop",region_attributesMap,tmpMap);
					}
				}
				labelMap.put(jsonFileName, tmpMap);
			}
		}
	}


	private void collectPersonProperty(String key, Map<String, Object> region_attributesMap,
			Map<String, String> propertyMap) {
		Object tmp = region_attributesMap.get(key);
		if(tmp != null && tmp instanceof Map) {
			Map<String,Object> tmpMap = (Map<String,Object>)tmp;
			for(Entry<String,Object> entry : tmpMap.entrySet()) {
				if(entry.getValue() != null && entry.getValue().toString().equalsIgnoreCase("true")) {
					propertyMap.put(entry.getKey(), "1");
				}else {
					propertyMap.put(entry.getKey(), "0");
				}
			}
		}
	}
}
