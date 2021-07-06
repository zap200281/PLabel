package com.pcl.service.schedule;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.pcl.constant.Constants;
import com.pcl.constant.UserConstants;
import com.pcl.dao.AlgInstanceDao;
import com.pcl.dao.AlgModelDao;
import com.pcl.dao.ProgressDao;
import com.pcl.dao.ReIDLabelTaskItemDao;
import com.pcl.dao.ReIDTaskDao;
import com.pcl.dao.ReIDTaskResultDao;
import com.pcl.dao.ReIDTaskShowResultDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.Progress;
import com.pcl.pojo.mybatis.AlgInstance;
import com.pcl.pojo.mybatis.AlgModel;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.pojo.mybatis.ReIDTask;
import com.pcl.pojo.mybatis.ReIDTaskResult;
import com.pcl.pojo.mybatis.ReIDTaskShowResult;
import com.pcl.service.MinioFileService;
import com.pcl.service.ObjectFileService;
import com.pcl.service.TokenManager;
import com.pcl.util.FileUtil;
import com.pcl.util.ProcessExeUtil;
import com.pcl.util.ReIDUtil;

@Service
public class ReIDShowResultSchedule {

	private static Logger logger = LoggerFactory.getLogger(ReIDShowResultSchedule.class);


	@Autowired
	private ReIDTaskResultDao reIDTaskResultDao;

	@Autowired
	private AlgModelDao algModelDao;

	@Autowired
	private ObjectFileService fileService;

	@Autowired
	private AlgInstanceDao algInstanceDao;

	@Autowired
	private ReIDTaskDao reIDTaskDao;
	@Autowired
	private ReIDTaskShowResultDao reIDTaskShowResultDao;
	
	@Autowired
	private ReIDLabelTaskItemDao reIdLabelTaskItemDao;
	
	@Autowired
	private ProgressDao progressDao;

	private Gson gson = new Gson();
	
	private void putProgress(Progress pro) {

		Progress tmp = progressDao.queryProgressById(pro.getId());
		if(tmp != null) {
			progressDao.deleteProgress(pro.getId());
		}
		
		progressDao.addProgress(pro);
	}

	public void execReIDTaskNew(String reIdTaskid) {
		ReIDTask reIDTask = reIDTaskDao.queryReIDTaskById(reIdTaskid);
		
		long start = System.currentTimeMillis();
		Progress progress = new Progress();
		progress.setId(reIdTaskid);
		progress.setStartTime(System.currentTimeMillis()/1000);
		progress.setExceedTime(300);
		putProgress(progress);
		
		try {

			AlgModel algModel = algModelDao.queryAlgModelById(reIDTask.getAlg_model_id());

			if(algModel == null) {
				logger.info("the algInstance is null. modelId=" + reIDTask.getAlg_model_id());
				throw new LabelSystemException("自动标注所选择的算法模型不存在。");
			}

			AlgInstance algInstance = algInstanceDao.queryAlgInstanceById(Integer.parseInt(algModel.getAlg_instance_id()));
			if(algInstance == null) {
				logger.info("the algInstance is null. algInstanceId=" + algModel.getAlg_instance_id());
				throw new LabelSystemException("自动标注所选择的算法模型不存在。");
			}
			String script = algModel.getExec_script();

			String dataDir = getDataDir(algInstance.getAlg_root_dir());

			String everyDataDir = dataDir;
			String everyDataDirSrc = everyDataDir + "/query/";
			new File(everyDataDirSrc).mkdirs();
			String everyDataDirDest = everyDataDir + "/bounding_box_test/";
			new File(everyDataDirDest).mkdirs();
			
			
			Map<String,String> imageNameForReIdMap = copyImageToSrcAndDest(everyDataDirSrc,everyDataDirDest,reIDTask);

			File resultJsonFile = new File(everyDataDir, "test.json");
			resultJsonFile.delete();

			String trainDir = everyDataDir + "/bounding_box_train/";
			new File(trainDir).mkdirs();

			script = script.replace("{data_dir}", everyDataDir);
			ProcessExeUtil.execScript(script,algInstance.getAlg_root_dir(),600);

			if(resultJsonFile.exists()) {
				logger.info("delete exist result firstly.");
				reIDTaskResultDao.deleteByReIDTaskId(reIDTask.getId());//先删除
				logger.info("save result to db.");
				String content = FileUtil.getAllContent(resultJsonFile.getAbsolutePath(), "utf-8");
				Map<String,List<String>> labelMap = gson.fromJson(content, new TypeToken<Map<String,List<String>>>() {
					private static final long serialVersionUID = 1L;}.getType());

				logger.info("size = " + labelMap.size());

				List<ReIDTaskResult> resultList = new ArrayList<>();
				HashSet<String> imgReIDSet = new HashSet<>();
				for(Entry<String,List<String>> entry : labelMap.entrySet()) {
					String srcImgName = entry.getKey();

					List<String> destImgList = entry.getValue();
					List<Map<String,String>> realList = new ArrayList<>();//需要List，因为得分高低要有序
	
					for(String tmp : destImgList) {
						String diskCutDestImgMinioPath = "/minio/" + reIDTask.getId() + "/" + tmp;
						if(tmp.equals(srcImgName)) {
							continue;
						}
						Map<String,String> info = new HashMap<>();
						info.put(diskCutDestImgMinioPath, imageNameForReIdMap.get(tmp));
						realList.add(info);
					}
					if(realList.size() == 0) {
						logger.info("dest size =0");
						continue;
					}
					ReIDTaskResult re = new ReIDTaskResult();
					re.setId(reIDTask.getId());
					re.setLabel_task_id("-1");
					re.setLabel_task_name("showresult");
					re.setSrc_image_info(srcImgName);
					re.setRelated_info(gson.toJson(realList));
					resultList.add(re);
					
				}
				if(!resultList.isEmpty()) {
					reIDTaskResultDao.addBatchTaskItem(resultList);
				}else {
					logger.info("error, size = 0");
				}
				
				
			}
			//FileUtil.delDir(everyDataDir);
			logger.info("Finished ReID show result auto deal, cost:" + ((System.currentTimeMillis() - start) / 1000) + "s");
		}catch (Exception e) {
			logger.error("ReId error: " + e.getMessage(),e);
		}finally {
			progressDao.deleteProgress(reIdTaskid);
		}
	}

	private Map<String,String> copyImageToSrcAndDest(String everyDataDirSrc,String everyDataDirDest,ReIDTask reIDTask) throws LabelSystemException {
		HashMap<String,String> re = new HashMap<>();
		List<ReIDTaskShowResult> showList = reIDTaskShowResultDao.queryReIDShowTaskResultById(reIDTask.getId());
		long start = System.currentTimeMillis();
		logger.info("download cut image start.");

		for(ReIDTaskShowResult item : showList) {
			String mapStr = item.getRelated_info();
			Map<String,String> map =  gson.fromJson(mapStr, new TypeToken<Map<String,String>>() {
				private static final long serialVersionUID = 1L;}.getType());

			for(Entry<String,String> entry : map.entrySet()) {
				String tmp[] = entry.getKey().split("/");
				int length = tmp.length;
				fileService.downLoadFileFromMinio(tmp[length-2], tmp[length-1], everyDataDirSrc);
				fileService.downLoadFileFromMinio(tmp[length-2], tmp[length-1], everyDataDirDest);
				re.put(tmp[length-1], item.getReid_name());
			}
			
		}
		long end = System.currentTimeMillis();
		logger.info("download image finished, cost: " + (end - start) +" ms");
		return re;
	}

	private String getDataDir(String algRootPath) {
		return algRootPath + "data" + File.separator + System.nanoTime();
	}



}
