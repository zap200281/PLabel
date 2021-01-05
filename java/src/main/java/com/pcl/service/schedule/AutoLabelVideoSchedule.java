package com.pcl.service.schedule;

import java.io.File;
import java.io.SequenceInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.pcl.constant.Constants;
import com.pcl.constant.UserConstants;
import com.pcl.dao.AlgInstanceDao;
import com.pcl.dao.AlgModelDao;
import com.pcl.dao.LabelTaskItemDao;
import com.pcl.pojo.mybatis.AlgInstance;
import com.pcl.pojo.mybatis.AlgModel;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.pojo.mybatis.PrePredictTaskResult;
import com.pcl.service.TokenManager;
import com.pcl.util.FileUtil;
import com.pcl.util.JsonUtil;
import com.pcl.util.StreamHandler;
import com.pcl.util.TimeUtil;
import com.pcl.util.VocAnnotationsUtil;

@Service
@EnableScheduling   // 2.开启定时任务
public class AutoLabelVideoSchedule {

	private static Logger logger = LoggerFactory.getLogger(AutoLabelVideoSchedule.class);

	@Value("${server.port}")
	private String port;

	@Value("${msgresttype:https}")
	private String msgresttype;

	@Value("${msgrestip:127.0.0.1}")
	private String msgrestip;

	@Autowired
	private AlgModelDao algModelDao;

	@Autowired
	private AlgInstanceDao algInstanceDao;

	@Autowired
	private VocAnnotationsUtil vocAnnotation;

	@Autowired
	private LabelTaskItemDao labelTaskItemDao;


	private final static int DISTINGUISH_ALG_MODEL_ID = 19;


	private Map<Integer,ProcessObj> processMap = new ConcurrentHashMap<>();


	@Scheduled(cron = "0 */2 * * * ?")//每隔2分钟执行一次。
	public void releaseProcess() {
		Set<Integer> algIdSet = processMap.keySet();
		for(Integer algId : algIdSet) {
			ProcessObj obj = processMap.get(algId);
			if((System.currentTimeMillis() - obj.lastLabelTime) > 1000 * 60 * 60 * 2) {//2小时
				Process tmpPro = obj.p;
				if(tmpPro != null) {
					logger.info("it time to destroy process.altId=" + algId);
					tmpPro.destroyForcibly();
					processMap.remove(algId);
				}
			}
		}
	}

	public void labelPicture(String picturePath,String taskId, LabelTaskItem body,int userId) {
		int tmpAlgId = body.getDisplay_order2();
		if(tmpAlgId == 0) {
			tmpAlgId = 5;
		}
		final int algId = tmpAlgId;

		logger.info("label picture use algid:" + algId);
		ProcessObj processObj = processMap.get(algId);
		if(processObj == null) {
			logger.info("create process obj...");
			processObj = new ProcessObj();
			AlgModel algModel = algModelDao.queryAlgModelById(algId);//
			AlgInstance algInstance = null;
			if(algModel != null) {
				algInstance = algInstanceDao.queryAlgInstanceById(Integer.parseInt(algModel.getAlg_instance_id()));
			}
			if(algInstance == null) {
				logger.info("the algInstance is null. algInstanceId=" + algModel.getAlg_instance_id());
				return;
			}

			String imageDir =  algInstance.getAlg_root_dir()  + "image_v_auto" + File.separator + System.nanoTime() + File.separator;
			FileUtil.delDir(imageDir);
			new File(imageDir).mkdirs();
			processObj.pictureRootPath = imageDir;
			processObj.lastLabelTime = System.currentTimeMillis();
			FileUtil.copyFile(picturePath, processObj.pictureRootPath + body.getId() + ".jpg");
			processMap.put(algId, processObj);

			String tmpScript = getScript(algModel);
			tmpScript += " --image_dir " + processObj.pictureRootPath;
			tmpScript += " --output_dir " + processObj.pictureRootPath;
			tmpScript += " --taskid " + taskId + "##" + userId;
			tmpScript += " --msgrest " +  msgresttype + "://" + msgrestip + ":" + port; 
			//https://127.0.0.1:" + port;

			final String script = tmpScript;
			final String rootPath = algInstance.getAlg_root_dir();
			final ProcessObj tmp = processObj;

			long timeSeconds = 24 * 3600;
			logger.info("exec script:" + script);
			ProcessBuilder pb = new ProcessBuilder("/bin/bash","-c",script);
			pb.directory(new File(rootPath));

			try {
				Process tmpProcess = pb.start();
				tmp.p = tmpProcess;

				logger.info("wait to " + timeSeconds + " seconds.");
				StreamHandler handler = new StreamHandler(new SequenceInputStream(tmpProcess.getInputStream(), tmpProcess.getErrorStream()),null);
				handler.start();

				tmpProcess.waitFor(timeSeconds, TimeUnit.SECONDS);
				tmpProcess.destroyForcibly();
			}catch(Exception e) {
				e.printStackTrace();
			}finally {
				processMap.remove(algId);
			}
		}else {
			processObj.lastLabelTime = System.currentTimeMillis();
			FileUtil.copyFile(picturePath, processObj.pictureRootPath + body.getId() + ".jpg");
			logger.info("finished copy picture to process obj... path=" + (processObj.pictureRootPath + body.getId() + ".jpg"));
		}

	}


	public void distinguishPictureCar(String picturePath,String taskId, LabelTaskItem body,int userId) {

		logger.info("distinguishPictureCar use algid:" + DISTINGUISH_ALG_MODEL_ID);

		AlgModel algModel = algModelDao.queryAlgModelById(DISTINGUISH_ALG_MODEL_ID);//
		AlgInstance algInstance = null;
		if(algModel != null) {
			algInstance = algInstanceDao.queryAlgInstanceById(Integer.parseInt(algModel.getAlg_instance_id()));
		}
		if(algInstance == null) {
			logger.info("the algInstance is null. algInstanceId=" + algModel.getAlg_instance_id());
			return;
		}


		String imageRootDir = algInstance.getAlg_root_dir()  + System.nanoTime() + File.separator + "VOC2007";

		String imageDir =  imageRootDir   + File.separator + Constants.JPEGIMAGES +File.separator;
		FileUtil.delDir(imageRootDir);
		new File(imageDir).mkdirs();

		if(new File(picturePath).exists()) {
			logger.info("the file is exist." + picturePath);
		}else {
			logger.info("the file is not exist." + picturePath);
		}
		
		String imageFullPath =imageDir + body.getId() + ".jpg";
		//拷贝图片
		FileUtil.copyFile(picturePath, imageFullPath);
		
		if(new File(imageFullPath).exists()) {
			logger.info("the file is exist." + imageFullPath);
		}else {
			logger.info("the file is not exist." + imageFullPath);
		}
		//生成Xml
		ObjectDistinguish objectDistinguish = new ObjectDistinguish();

		String xmlPath = vocAnnotation.getXmlPathByImagePath(imageFullPath);
		new File(xmlPath).getParentFile().mkdir();
		objectDistinguish.saveToXml(body.getLabel_info(),imageFullPath,xmlPath,vocAnnotation);


		Map<String,Object> fileMap = new HashMap<>();
		fileMap.put(body.getId() + ".jpg", body.getId() + ".jpg");
		String testTxtPath = imageRootDir + "/ImageSets/Main/test.txt";

		objectDistinguish.saveImageSetTestTxt(testTxtPath, fileMap);

		String distinguishJsonFile = imageRootDir + File.separator + "distiguish.json";
		String tmpScript = getDistiguishScript(imageRootDir,distinguishJsonFile,0);
		tmpScript += " --taskid " + taskId +"##" + userId;
		//tmpScript += " --msgrest " +  msgresttype + "://" + msgrestip + ":" + port; 
		//https://127.0.0.1:" + port;

		final String script = tmpScript;
		final String rootPath = algInstance.getAlg_root_dir();


		long timeSeconds = 24 * 3600;
		logger.info("exec script:" + script);
		ProcessBuilder pb = new ProcessBuilder("/bin/bash","-c",script);
		pb.directory(new File(rootPath));

		try {
			Process tmpProcess = pb.start();

			logger.info("wait to " + timeSeconds + " seconds.");
			StreamHandler handler = new StreamHandler(new SequenceInputStream(tmpProcess.getInputStream(), tmpProcess.getErrorStream()),null);
			handler.start();
			tmpProcess.waitFor(timeSeconds, TimeUnit.SECONDS);

			//读取结果
			if(new File(distinguishJsonFile).exists()) {
				String jsonStr = FileUtil.getAllContent(distinguishJsonFile, "utf-8");

				List<Map<String,Object>> list = JsonUtil.getLabelList(jsonStr);

				logger.info("writer distinguishJsonFile, size=" + list.size());

				for(Map<String,Object> map : list) {

					Object colorData = map.get("color");
					Object typeData = map.get("type");
					List<Map<String,Object>> colorDataList = (List<Map<String,Object>>)colorData;
					List<Map<String,Object>> typeDataList = (List<Map<String,Object>>)typeData;

					Map<String,Object> colorMap = getMap(colorDataList);
					Map<String,Object> typeMap = getMap(typeDataList);

					List<Map<String,Object>> labelList = JsonUtil.getLabelList(body.getLabel_info());
					for(Map<String,Object> label : labelList) {
						String id = label.get("id").toString();
						label.putAll(objectDistinguish.getTypeColor(colorMap, typeMap, id));
					}
					String newLabelInfo = JsonUtil.toJson(labelList);
					Map<String,Object> updateParamMap = new HashMap<>();
					updateParamMap.put("id", body.getId());
					updateParamMap.put("label_info", newLabelInfo);
					updateParamMap.put("item_add_time", TimeUtil.getCurrentTimeStr());
					updateParamMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE));
					logger.info("updateParamMap=" + updateParamMap);
					labelTaskItemDao.updateLabelTaskItem(updateParamMap);
				}
			}

			tmpProcess.destroyForcibly();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	private Map<String,Object> getMap(List<Map<String,Object>> list){
		Map<String,Object> re = new HashMap<>();
		for(Map<String,Object> tmp :list) {
			re.putAll(tmp);
		}
		return re;
	}

	private String getScript(AlgModel algModel) {
		if(algModel != null) {
			String execScript = algModel.getExec_script();
			if(algModel.getConf_path() != null) {
				execScript = execScript.replace("{configPath}", algModel.getConf_path());
			}
			if(algModel.getModel_url() != null) {
				execScript = execScript.replace("{modelPath}", algModel.getModel_url());
			}
			execScript = execScript.replace("demoForJava.py", "labelPictureForever.py");

			return execScript;
		}

		return null;
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

	private String getDistiguishScript(String imageDir,String outputDir,Integer gpuNum) {
		String script = getScript(DISTINGUISH_ALG_MODEL_ID);
		script += " --image_dir " + imageDir;
		script += " --output_dir " + outputDir;
		script += " --gpu " + gpuNum;

		return script;
	}

	class ProcessObj{
		Process p;
		long lastLabelTime;
		String pictureRootPath;
	}

}
