package com.pcl.service.schedule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pcl.constant.UserConstants;
import com.pcl.dao.AlgModelDao;
import com.pcl.dao.PrePredictTaskResultDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.mybatis.AlgModel;
import com.pcl.pojo.mybatis.PrePredictTask;
import com.pcl.pojo.mybatis.PrePredictTaskResult;
import com.pcl.service.TokenManager;
import com.pcl.util.FileUtil;
import com.pcl.util.JsonUtil;
import com.pcl.util.ProcessExeUtil;
import com.pcl.util.VocAnnotationsUtil;

public class PredictCarDistinguish {

	private static Logger logger = LoggerFactory.getLogger(PredictCarDistinguish.class);
	
	private final static int DISTINGUISH_CAR_PROPERTY_ALG_MODEL_ID = 19;
	

	private AlgModelDao algModelDao;
	
	private PrePredictTaskResultDao prePredictTaskResultDao;
	
	public PredictCarDistinguish() {
		
	}
	
	public PredictCarDistinguish(AlgModelDao algModelDao,PrePredictTaskResultDao prePredictTaskResultDao) {
		this.algModelDao = algModelDao;
		this.prePredictTaskResultDao = prePredictTaskResultDao;
	}

	public void dealDistinguishCarPropertyResult(Integer gpuNum, String tmpImageDir, String algRootPath, String outputDir,
			int length,  HashMap<String, PrePredictTaskResult> preResultMap,PrePredictTask prePredictTask)
					throws LabelSystemException {
		logger.info("start to distinguish car type and color.");
		//进行识别：
		//调用Python
		String distinguishJsonFile = outputDir + "distiguish.json";
		String distiguishScript = getDistiguishScript(tmpImageDir  + File.separator + "VOC2007",distinguishJsonFile,gpuNum);
		distiguishScript += " --taskid " + prePredictTask.getId() + "##" + prePredictTask.getUser_id();
		ProcessExeUtil.execScript(distiguishScript, algRootPath, length * 2 + 30);

		//读取Json文件，并保存
		if(new File(distinguishJsonFile).exists()) {
			String jsonStr = FileUtil.getAllContent(distinguishJsonFile, "utf-8");

			List<Map<String,Object>> list = JsonUtil.getLabelList(jsonStr);

			logger.info("writer distinguishJsonFile, size=" + list.size());

			for(Map<String,Object> map : list) {
				String fileName = map.get("filename").toString();
				Object colorData = map.get("color");
				Object typeData = map.get("type");
				List<Map<String,Object>> colorDataList = (List<Map<String,Object>>)colorData;
				List<Map<String,Object>> typeDataList = (List<Map<String,Object>>)typeData;


				Map<String,Object> colorMap = getMap(colorDataList);
				Map<String,Object> typeMap = getMap(typeDataList);

				PrePredictTaskResult result = preResultMap.get(fileName);
				if(result != null) {
					List<Map<String,Object>> labelList = JsonUtil.getLabelList(result.getLabel_info());
					for(Map<String,Object> label : labelList) {
						String id = label.get("id").toString();
						label.putAll(getTypeColor(colorMap, typeMap, id));
					}
					String newLabelInfo = JsonUtil.toJson(labelList);
					Map<String,Object> updateParamMap = new HashMap<>();
					updateParamMap.put("id", result.getId());
					updateParamMap.put("label_info", newLabelInfo);
					updateParamMap.put("user_id", TokenManager.getUserTablePos(prePredictTask.getUser_id(), UserConstants.PREDICT_SINGLE_TABLE));
					logger.info("updateParamMap=" + updateParamMap);
					prePredictTaskResultDao.updatePrePredictTaskResult(updateParamMap);
				}
			}

		}
	}
	
	private String getDistiguishScript(String imageDir,String outputDir,Integer gpuNum) {
		String script = getScript(DISTINGUISH_CAR_PROPERTY_ALG_MODEL_ID);
		script += " --image_dir " + imageDir;
		script += " --output_dir " + outputDir;
		script += " --gpu " + gpuNum;

		return script;
	}
	
	private Map<String,Object> getMap(List<Map<String,Object>> list){
		Map<String,Object> re = new HashMap<>();
		for(Map<String,Object> tmp :list) {
			re.putAll(tmp);
		}
		return re;
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
	
	public boolean isContainCar(String algModelType) {
		List<String> typeList = JsonUtil.getList(algModelType);
		if(typeList.contains("car")) {
			return true;
		}
		return false;
	}

	public Map<String,Object> getTypeColor(Map<String,Object> colorMap,Map<String,Object> typeMap,String id){
		
		Map<String,Object> re = new HashMap<>();
		Map<String,Object> region_attributes = new HashMap<>();
		region_attributes.put("id", id);
		if(colorMap.containsKey(id)) {
			re.put("color", colorMap.get(id));
			region_attributes.put("color", colorMap.get(id));
		}
		if(typeMap.containsKey(id)) {
			re.put("type", typeMap.get(id));
			region_attributes.put("type", typeMap.get(id));
		}
		
		Map<String,Object> other = new HashMap<>();
		other.put("region_attributes", region_attributes);

		re.put("other", other);
		
		return re;
	}

	public void saveImageSetTestTxt(String testTxtPath,Map<String,Object> map) {
		logger.info("writer test txt:" + testTxtPath);
		File testTxtFile = new File(testTxtPath);
		testTxtFile.getParentFile().mkdirs();
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(testTxtPath))){
			for(Entry<String,Object> entry : map.entrySet()) {
				String fileName = entry.getKey();
				writer.write(fileName.substring(0,fileName.lastIndexOf(".")));
				writer.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveToXml(String labelInfo,String imagePath,String xmlPath,VocAnnotationsUtil vocAnnotation) {

		ArrayList<Map<String,Object>> labelList = JsonUtil.getLabelList(labelInfo);
		for(int i = 0; i < labelList.size(); i++) {
			Map<String,Object> map = labelList.get(i);
			String className = String.valueOf(map.get("class_name"));
			if(className.equals("person")) {
				labelList.remove(i);
				i--;
			}
		}

		Document doc = vocAnnotation.getXmlDocument(labelList, new HashMap<>(), imagePath);
		if(doc != null) {

			FileWriter fileWriter = null;
			try {
				fileWriter = new FileWriter(xmlPath);
				OutputFormat format = new OutputFormat("\t", true);
				format.setTrimText(true);//去掉原来的空白(\t和换行和空格)！

				XMLWriter writer = new XMLWriter(fileWriter, format);
				// 把document对象写到out流中。
				writer.write(doc);
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else {
			logger.info("doc is null. imagePath=" + imagePath);
		}

	}


}
