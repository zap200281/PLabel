package com.pcl.service.schedule;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pcl.constant.LogConstants;
import com.pcl.constant.UserConstants;
import com.pcl.dao.AlgInstanceDao;
import com.pcl.dao.AlgModelDao;
import com.pcl.dao.PrePredictTaskResultDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.mybatis.AlgInstance;
import com.pcl.pojo.mybatis.AlgModel;
import com.pcl.pojo.mybatis.PrePredictTask;
import com.pcl.pojo.mybatis.PrePredictTaskResult;
import com.pcl.service.LabelDataSetMerge;
import com.pcl.service.TokenManager;
import com.pcl.util.FileUtil;
import com.pcl.util.ImageCutUtil;
import com.pcl.util.JsonUtil;
import com.pcl.util.ProcessExeUtil;
import com.pcl.util.ReIDUtil;

public class PredictPersonProperty {

	private static Logger logger = LoggerFactory.getLogger(PredictPersonProperty.class);

	private AlgModelDao algModelDao;

	private PrePredictTaskResultDao prePredictTaskResultDao;
	
	private AlgInstanceDao algInstanceDao;

	public PredictPersonProperty() {

	}

	public PredictPersonProperty(AlgModelDao algModelDao,PrePredictTaskResultDao prePredictTaskResultDao,AlgInstanceDao algInstanceDao) {
		this.algModelDao = algModelDao;
		this.prePredictTaskResultDao = prePredictTaskResultDao;
		this.algInstanceDao = algInstanceDao;
	}


	public void dealDistinguishPersonPropertyResult(Integer gpuNum,HashMap<String, PrePredictTaskResult> preResultMap,PrePredictTask prePredictTask,int algModelId)
					throws LabelSystemException {
		logger.info("start to distinguish person type and color.");
		
		String algRootPath = getAlgRootPath(algModelId);
		
		String tmpImageDir = algRootPath  + System.nanoTime() ;
		
		String outputDir = tmpImageDir + "/output/";
		new File(tmpImageDir).mkdir();
		new File(outputDir).mkdir();
		//进行识别：
		//调用Python
		String distinguishJsonFile = outputDir + "result.json";
		
		HashMap<String, PrePredictTaskResult> tmpPreResultMap = new HashMap<>();
		
		for(Entry<String,PrePredictTaskResult> entry : preResultMap.entrySet()) {
			String srcImagePath = entry.getKey();
			String jsonLabelInfo = entry.getValue().getLabel_info();
			logger.info("srcImagePath=" + srcImagePath);
			logger.info("jsonLabelInfo=" + jsonLabelInfo);
			cutImageToPath(jsonLabelInfo, tmpImageDir, srcImagePath);
			tmpPreResultMap.put(new File(srcImagePath).getName(), entry.getValue());
		}
		
		logger.info("finished cut person image.");
		
		String distiguishScript = getDistiguishScript(tmpImageDir,outputDir,gpuNum,algModelId);
		
		distiguishScript += " --taskid " + prePredictTask.getId() + "##" + prePredictTask.getUser_id();
	
		
		ProcessExeUtil.execScript(distiguishScript, algRootPath, new File(tmpImageDir).listFiles().length * 2 + 30);

		//读取Json文件，并保存
		if(new File(distinguishJsonFile).exists()) {
			dealPersonPropertyResultJson(preResultMap, prePredictTask, distinguishJsonFile, tmpPreResultMap);
		}
		//删除下载的图片
		FileUtil.delDir(tmpImageDir);
	}

	public void dealPersonPropertyResultJson(HashMap<String, PrePredictTaskResult> preResultMap,
			PrePredictTask prePredictTask, String distinguishJsonFile,
			HashMap<String, PrePredictTaskResult> tmpPreResultMap) {
		String jsonStr = FileUtil.getAllContent(distinguishJsonFile, "utf-8");

		Map<String,Object> map =  JsonUtil.getMap(jsonStr);

		logger.info("writer distinguishJsonFile, size=" + map.size());

		for(Entry<String,Object> entry : map.entrySet()) {
			String fileName = entry.getKey();
			PrePredictTaskResult result = tmpPreResultMap.get(fileName);
			String labelId = "0";
			if(result == null) {
				String srcImage = ReIDUtil.getPrimitivePicture(fileName);
				labelId = ReIDUtil.getLabelId(fileName);
				logger.info("srcImage=" + srcImage);
				result = tmpPreResultMap.get(srcImage);
			}
			if(result != null) {
				List<Map<String,Object>> labelList = JsonUtil.getLabelList(result.getLabel_info());
				for(Map<String,Object> label : labelList) {
					String id = label.get("id").toString();
					if(id.equals(labelId)) {
						Map<String,Object> attrMap = (Map<String,Object>)entry.getValue();
						Map<String,Object> newOther = null;
						if(label.get("other") != null ) {
							newOther = (Map<String,Object>)label.get("other");
						}else {
							newOther = new HashMap<>();
							label.put("other", newOther);
						}
						Map<String,Object> newregion_attributes = null;
						if(newOther.get(LogConstants.REGION_ATTRIBUTES) != null) {
							newregion_attributes = (Map<String,Object>)newOther.get(LogConstants.REGION_ATTRIBUTES);
						}else {
							newregion_attributes = new HashMap<>();
							newregion_attributes.put("id", id);
							newregion_attributes.put("type", label.get("class_name"));
							newOther.put(LogConstants.REGION_ATTRIBUTES, newregion_attributes);
						}
						newregion_attributes.putAll(attrMap);
					}
				}
				String newLabelInfo = JsonUtil.toJson(labelList);
				result.setLabel_info(newLabelInfo);
			}
		}
		
		for(Entry<String,PrePredictTaskResult> entry: preResultMap.entrySet()) {
			Map<String,Object> updateParamMap = new HashMap<>();
			updateParamMap.put("id", entry.getValue().getId());
			updateParamMap.put("label_info", entry.getValue().getLabel_info());
			updateParamMap.put("user_id", TokenManager.getUserTablePos(prePredictTask.getUser_id(), UserConstants.PREDICT_SINGLE_TABLE));
			logger.info("updateParamMap=" + updateParamMap);
			prePredictTaskResultDao.updatePrePredictTaskResult(updateParamMap);
		}
	}

	private String getDistiguishScript(String imageDir,String outputDir,Integer gpuNum,int algModelId) {
		String script = getScript(algModelId);
		script += " --image_dir " + imageDir;
		script += " --output_dir " + outputDir;
		script += " --gpu " + gpuNum;

		return script;
	}

	private String getAlgRootPath(int modelId) throws LabelSystemException {
		AlgModel algModel = algModelDao.queryAlgModelById(modelId);

		if(algModel == null) {
			logger.info("the algInstance is null. modelId=" + modelId);
			throw new LabelSystemException("自动标注所选择的算法模型不存在。");
		}

		AlgInstance algInstance = algInstanceDao.queryAlgInstanceById(Integer.parseInt(algModel.getAlg_instance_id()));
		if(algInstance == null) {
			logger.info("the algInstance is null. algInstanceId=" + algModel.getAlg_instance_id());
			throw new LabelSystemException("自动标注所选择的算法模型不存在。");
		}
		String algRootPath = LabelDataSetMerge.getAlgRootPath(algInstance.getAlg_root_dir());
		return algRootPath;
	}
	

	private String getScript(int algModelId) {
		AlgModel algModel = algModelDao.queryAlgModelById(algModelId);
		if(algModel != null) {
			String execScript = algModel.getExec_script();
			if(algModel.getConf_path() != null) {
				execScript = execScript.replace("{configPath}", algModel.getConf_path());
			}
			if(algModel.getModel_url() != null) {
				execScript = execScript.replace("{modelPath}", algModel.getModel_url());
			}
			return execScript;
		}

		return null;
	}
	

	
	
	private int cutImageToPath(String jsonLabelInfo, String destCutImagePath,String srcImagePath)  {
		int count = 0;
	
		if(Strings.isBlank(jsonLabelInfo)) {
			logger.info("jsonLabelInfo is null. jsonLabelInfo=" + jsonLabelInfo);
			return count;
		}
		ArrayList<Map<String,Object>> labelList = JsonUtil.getLabelList(jsonLabelInfo);
		if(labelList.isEmpty()) {
			logger.info("jsonLabelInfo is empty. jsonLabelInfo=" + jsonLabelInfo);
			return count;
		}

		BufferedImage bufferImage = null;
		try {
			bufferImage = ImageIO.read(new File(srcImagePath));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		if(bufferImage == null) {
			logger.info("image is null. path=" + srcImagePath);
			return count;
		}

		String imageName = new File(srcImagePath).getName();
		imageName = imageName.substring(imageName.lastIndexOf("/") + 1);
		imageName = imageName.substring(0,imageName.length() - 4);
		for(Map<String,Object> label : labelList) {
			if(ImageCutUtil.cutImage(label, imageName, destCutImagePath, bufferImage) == 0) {
				count++;
			}else {
				FileUtil.copyFile(srcImagePath, destCutImagePath + File.separator +  new File(srcImagePath).getName());
			}
		}
		return count;

	}

}
