package com.pcl.service.schedule;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.pcl.constant.Constants;
import com.pcl.constant.LogConstants;
import com.pcl.constant.UserConstants;
import com.pcl.dao.AlgInstanceDao;
import com.pcl.dao.AlgModelDao;
import com.pcl.dao.DataSetDao;
import com.pcl.dao.PrePredictTaskDao;
import com.pcl.dao.ReIDLabelTaskItemDao;
import com.pcl.dao.ReIDTaskDao;
import com.pcl.dao.ReIDTaskResultDao;
import com.pcl.dao.ReIDTaskShowResultDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.mybatis.AlgInstance;
import com.pcl.pojo.mybatis.AlgModel;
import com.pcl.pojo.mybatis.DataSet;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.pojo.mybatis.PrePredictTask;
import com.pcl.pojo.mybatis.ReIDTask;
import com.pcl.pojo.mybatis.ReIDTaskResult;
import com.pcl.pojo.mybatis.ReIDTaskShowResult;
import com.pcl.service.MinioFileService;
import com.pcl.service.ObjectFileService;
import com.pcl.service.TokenManager;
import com.pcl.util.CocoAnnotationsUtil;
import com.pcl.util.FileUtil;
import com.pcl.util.ImageCutUtil;
import com.pcl.util.JsonUtil;
import com.pcl.util.ProcessExeUtil;
import com.pcl.util.ReIDUtil;
import com.pcl.util.TimeUtil;

@Service
public class ReIDSchedule {

	private static Logger logger = LoggerFactory.getLogger(ReIDSchedule.class);


	@Autowired
	private ReIDTaskResultDao reIDTaskResultDao;

	@Autowired
	private AlgModelDao algModelDao;

	@Autowired
	private ObjectFileService fileService;

	@Autowired
	private PrePredictTaskDao prePredictTaskDao;

	@Autowired
	private AlgInstanceDao algInstanceDao;

	@Autowired
	private DataSetDao dataSetDao;

	@Autowired
	private ReIDTaskDao reIDTaskDao;

	@Autowired
	private ReIDLabelTaskItemDao reIdLabelTaskItemDao;

	@Autowired
	private ReIDTaskShowResultDao reIDTaskShowResultDao;

	private ArrayBlockingQueue<ReIDTask> queue = new ArrayBlockingQueue<>(100000);

	private Gson gson = new Gson();

	public boolean addTask(ReIDTask reIdTask) {
		return queue.offer(reIdTask);
	}

	@PostConstruct
	public void init() {

		logger.info("start to init queue : ReIDSchedule ");
		//从数据库加载未完成的任务继续运行。
		loadTaskFromDb();

		logger.info("start to execute runnable : ReIDSchedule ");

		new Thread(new Runnable() {

			@Override
			public void run() {
				while(true) {
					try {
						ReIDTask reIDTask = queue.take();
						//execReIDTask(reIDTask);
						execReIDTaskNew(reIDTask);
					}catch (Exception e) {
						logger.info("Failed to exec reID task.");
						e.printStackTrace();
					}
				}
			}
		},"ReIdSchedule").start();
	}

	private void loadTaskFromDb() {
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("task_status", Constants.REID_TASK_STATUS_AUTO_PROGRESSING);
		paramMap.put("task_type", Constants.REID_TASK_TYPE_AUTO);
		List<ReIDTask> list = reIDTaskDao.queryReIDTaskByStatus(paramMap);

		for(ReIDTask reIdTask : list) {
			addTask(reIdTask);
		}

	}

	private void execReIDTaskNew(ReIDTask reIDTask) {
		long start = System.currentTimeMillis();
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", reIDTask.getId());

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

			Map<String,Object> tmpParamMap = new HashMap<>();
			tmpParamMap.put("label_task_id", reIDTask.getId());
			tmpParamMap.put("pic_url", reIDTask.getSrc_predict_taskid());
			tmpParamMap.put("user_id", TokenManager.getUserTablePos(reIDTask.getUser_id(), UserConstants.REID_TASK_SINGLE_TABLE));
			List<LabelTaskItem> srclabelTaskItemList = reIdLabelTaskItemDao.queryLabelTaskItemByReIdAndLabelTaskId(tmpParamMap);
			int srcLabelItemNum = srclabelTaskItemList.size() ;
			logger.info("srcLabelItemNum=" + srcLabelItemNum);
			int reIdStartNum = 0;

			Map<String,Integer> reIdNumMap = new HashMap<>();

			int maxPicNum = 200;

			//大于1000张，则走此策略

			paramMap.put("task_status", Constants.REID_TASK_STATUS_AUTO_PROGRESSING);
			paramMap.put("task_status_desc", srcLabelItemNum + 30);//预计需要花费的时间
			reIDTaskDao.updateReIDTask(paramMap);

			List<String> destIdList = JsonUtil.getList(reIDTask.getDest_predict_taskid());

			Map<String,LabelTaskItem> imagesForSrcLabelItemId = new HashMap<>();
			for(LabelTaskItem item : srclabelTaskItemList) {
				String imageName = item.getPic_image_field();
				imageName = imageName.substring(imageName.lastIndexOf("/") + 1);
				imagesForSrcLabelItemId.put(imageName, item);
			}

			for(String destId : destIdList) {
				tmpParamMap.put("pic_url", destId);
				List<LabelTaskItem> destTaskItemList = reIdLabelTaskItemDao.queryLabelTaskItemByReIdAndLabelTaskId(tmpParamMap);
				int destLabelItemNum = destTaskItemList.size();
				logger.info("destLabelItemNum=" + destLabelItemNum);
				String labelTaskName = getLabelTaskName(destId,reIDTask);

				Map<String,LabelTaskItem> imagesForDestLabelItemId = new HashMap<>();
				for(LabelTaskItem item : destTaskItemList) {
					String imageName = item.getPic_image_field();
					imageName = imageName.substring(imageName.lastIndexOf("/") + 1);
					imagesForDestLabelItemId.put(imageName, item);
				}
				//一次处理200张
				for(int i = 0; i < srcLabelItemNum; i += maxPicNum) {
					String everyDataDir = dataDir + File.separator + i;
					//String everyDataDirSrc = everyDataDir + "/market1501/test/query/";
					//String everyDataDirDest = everyDataDir + "/market1501/test/bounding_box_test/";
					String everyDataDirSrc = everyDataDir + "/query/";
					String everyDataDirDest = everyDataDir + "/bounding_box_test/";

					if(!new File(everyDataDirSrc).exists()) {
						//如果存在，则不需要再剪切了
						cutLabelImageToPath(srclabelTaskItemList,i,i + maxPicNum, everyDataDirSrc);
					}

					FileUtil.delDir(everyDataDirDest);//目的不能重复,需要每次删除
					if(i >= destLabelItemNum) {
						continue;
					}
					cutLabelImageToPath(destTaskItemList,i, i + maxPicNum, everyDataDirDest);

					File resultJsonFile = new File(everyDataDir, "test.json");
					resultJsonFile.delete();

					//String trainDir = everyDataDir + "/market1501/test/bounding_box_train/";
					String trainDir = everyDataDir + "/bounding_box_train/";
					new File(trainDir).mkdirs();

					script = script.replace("{data_dir}", everyDataDir);
					ProcessExeUtil.execScript(script,algInstance.getAlg_root_dir(),600);

					if(resultJsonFile.exists()) {
						logger.info("save result to db.");
						String content = FileUtil.getAllContent(resultJsonFile.getAbsolutePath(), "utf-8");
						Map<String,List<String>> labelMap = gson.fromJson(content, new TypeToken<Map<String,List<String>>>() {
							private static final long serialVersionUID = 1L;}.getType());

						logger.info("size = " + labelMap.size());

						List<ReIDTaskResult> resultList = new ArrayList<>();
						for(Entry<String,List<String>> entry : labelMap.entrySet()) {
							String srcImgName = entry.getKey();
							int reIdNum = reIdStartNum + 1;
							if(reIdNumMap.containsKey(srcImgName)) {
								reIdNum = reIdNumMap.get(srcImgName);
							}else {
								reIdNumMap.put(srcImgName, reIdNum);
								reIdStartNum++;
							}

							String diskCutSrcImgPath = everyDataDirDest + srcImgName;
							//String diskCutSrcImgMinioPath = "/minio/" + reIDTask.getId() + "/" + srcImgName;
							if(!fileService.isExistMinioFile(reIDTask.getId(), srcImgName)) {
								if(new File(diskCutSrcImgPath).exists()) {
									fileService.uploadPictureFile(Arrays.asList(new File(diskCutSrcImgPath)), reIDTask.getId());
								}else {
									logger.info("img is not exist. path =" + diskCutSrcImgPath);
								}
							}

							List<String> destImgList = entry.getValue();
							List<Map<String,String>> realList = new ArrayList<>();//需要List，因为得分高低要有序
							boolean update = true;
							for(String tmp : destImgList) {
								//上传小图到minio中
								String diskCutDestImgPath = everyDataDirDest + tmp;
								String diskCutDestImgMinioPath = "/minio/" + reIDTask.getId() + "/" + tmp;
								if(new File(diskCutDestImgPath).exists()) {
									fileService.uploadPictureFile(Arrays.asList(new File(diskCutDestImgPath)), reIDTask.getId());
								}else {
									logger.info("img is not exist. path =" + diskCutDestImgPath);
								}
								if(tmp.equals(srcImgName)) {
									continue;
								}
								if(update) {
									//logger.info("udpate dest reid=" + reIdNum + " dest tmp=" +tmp);
									updateLabelTaskItemReId(tmp, imagesForDestLabelItemId, reIdNum,reIDTask);
									update = false;
								}
								Map<String,String> info = new HashMap<>();
								String realImageName = tmp.substring(0,tmp.lastIndexOf("_"));
								realImageName = realImageName + ".jpg";
								info.put(diskCutDestImgMinioPath, imagesForDestLabelItemId.get(realImageName).getId());
								realList.add(info);

							}
							ReIDTaskResult re = new ReIDTaskResult();
							re.setId(reIDTask.getId());
							re.setLabel_task_id(destId);
							re.setLabel_task_name(labelTaskName);
							re.setSrc_image_info(srcImgName);
							re.setRelated_info(gson.toJson(realList));
							resultList.add(re);

							//logger.info("udpate src reid=" + reIdNum + " dest tmp=" + srcImgName);
							if(!update) { //大于一个才进行更新
								updateLabelTaskItemReId(srcImgName, imagesForSrcLabelItemId, reIdNum,reIDTask);
							}
						}
						if(!resultList.isEmpty()) {
							reIDTaskResultDao.addBatchTaskItem(resultList);
						}else {
							logger.info("error, size = 0");
						}
					}

				}
			}

			updateReIdResultToShowResultDB(reIDTask.getUser_id(),reIDTask.getId());
			//FileUtil.delDir(dataDir);//删除

			paramMap.put("task_status", Constants.REID_TASK_STATUS_AUTO_FINISHED);
			paramMap.put("task_status_desc", "自动分类完成，共耗时：" + ((System.currentTimeMillis() - start) / 1000) + "s");
			logger.info("Finished ReID Task, cost:" + ((System.currentTimeMillis() - start) / 1000) + "s");
		}catch (Exception e) {
			logger.error("ReId error: " + e.getMessage(),e);
			paramMap.put("task_status", Constants.REID_TASK_STATUS_EXCEPTION);
			String message = e.getMessage();
			if(message.length() > 500) {
				message = message.substring(0,500);
			}
			paramMap.put("task_status_desc", message);
		}finally {
			reIDTaskDao.updateReIDTask(paramMap);	
		}
	}

	private void updateLabelTaskItemReId(String srcImgName,Map<String,LabelTaskItem> imagesForSrcLabelItemId,int reIDNum,ReIDTask reIDTask) {
		if(reIDTask.getReid_auto_type() == 0) {//默认不进行自动ReID标注
			logger.info("not auto label reId");
			return;
		}
		String realSrcImageName =  srcImgName.substring(0,srcImgName.lastIndexOf("_"));
		realSrcImageName = realSrcImageName + ".jpg";
		LabelTaskItem srcItem = imagesForSrcLabelItemId.get(realSrcImageName);
		String labelId = ReIDUtil.getLabelId(srcImgName);
		List<Map<String,Object>> labelListMap = JsonUtil.getLabelList(srcItem.getLabel_info());
		for(Map<String,Object> map : labelListMap) {
			if(map.get(LogConstants.LABEL_ID) != null && labelId.equals(map.get(LogConstants.LABEL_ID).toString())) {
				map.put(Constants.REID_KEY, String.valueOf(reIDNum));
				Map<String,Object> region_attributes = new HashMap<>();
				region_attributes.putAll(map);

				Map<String,Object> other = new HashMap<>();
				map.put(LogConstants.LABEL_OHTER, other);
				other.put(LogConstants.REGION_ATTRIBUTES, region_attributes);
				break;
			}
		}
		String newLabelInfo = JsonUtil.toJson(labelListMap);
		srcItem.setLabel_info(newLabelInfo);
		Map<String,Object> labelTaskItemParamMap = new HashMap<>();
		labelTaskItemParamMap.put("id", srcItem.getId());
		labelTaskItemParamMap.put("label_info", newLabelInfo);
		labelTaskItemParamMap.put("item_add_time", TimeUtil.getCurrentTimeStr());
		labelTaskItemParamMap.put("user_id", TokenManager.getUserTablePos(reIDTask.getUser_id(), UserConstants.REID_TASK_SINGLE_TABLE));
		reIdLabelTaskItemDao.updateLabelTaskItem(labelTaskItemParamMap);


	}

	/**
	 * 将reId的结果更新到数据库中，方便界面进行分页查询
	 * @param reTaskId
	 */
	public void updateReIdResultToShowResultDB(int userId,String reTaskId) {
		
		List<LabelTaskItem> itemList = reIdLabelTaskItemDao.queryLabelTaskItemByLabelTaskId(TokenManager.getUserTablePos(userId, UserConstants.REID_TASK_SINGLE_TABLE),reTaskId);

		if(itemList != null) {
			HashMap<String,Map<String,String>> reIdNameMap = new HashMap<>();
			for(LabelTaskItem item : itemList) {
				List<Map<String,Object>> labelList = ReIDUtil.getLabelList(item.getLabel_info());
				if(labelList.isEmpty()) {
					//logger.info("jsonLabelInfo is null. image=" + item.getPic_object_name());
					continue;
				}
				for(Map<String,Object> label : labelList) {
					Object reIdObj = label.get(Constants.REID_KEY);
					if(reIdObj == null || Strings.isBlank(reIdObj.toString())) {
						continue;
					}

					Object idObj = label.get("id");
					if(idObj == null || Strings.isBlank(idObj.toString())) {
						logger.info("it exists error, id is null. reId=" + reIdObj.toString() + " reTaskId=" + reTaskId + " item.id=" + item.getId());
						continue; 
					}
					String reIdName = reIdObj.toString().trim();
					String tmpPicName =ReIDUtil.getLabel(item.getPic_image_field(), idObj.toString());

					String imgName = "/minio/" + reTaskId + "/" + tmpPicName;
					if(!fileService.isExistMinioFileAndDeleteNotComplete(reTaskId, tmpPicName)) {
						logger.info("create image to minio.imgName=" + imgName);
						createImage(tmpPicName,label,item.getPic_image_field(),reTaskId);
					}
					Map<String,String> tmp = reIdNameMap.get(reIdName);
					if(tmp == null) {
						tmp = new HashMap<>();
						reIdNameMap.put(reIdName, tmp);
					}
					tmp.put(imgName, item.getId());
				}

			}

			Map<String,String> existsMap = new HashMap<>();
			List<ReIDTaskShowResult> list = reIDTaskShowResultDao.queryReIDShowTaskResultById(reTaskId);
			for(ReIDTaskShowResult tmp : list) {
				existsMap.put(tmp.getReid_name(), tmp.getLabel_task_id());
			}
			List<ReIDTaskShowResult> addList = new ArrayList<>();
			//更新
			for(Entry<String,Map<String,String>> entry : reIdNameMap.entrySet()) {
				String reIdName = entry.getKey();
				Map<String,String> relatedMap = entry.getValue();
				Object value = existsMap.remove(reIdName);
				if(value != null) {
					//update
					Map<String,Object> paramMap = new HashMap<>();
					paramMap.put("label_task_id",  reTaskId);
					paramMap.put("reid_name", reIdName);
					paramMap.put("related_info", gson.toJson(relatedMap));
					//logger.info("update relatedMap=" + paramMap);
					reIDTaskShowResultDao.updateShowResult(paramMap);
				}else {
					//create
					logger.info("reIDName=" + reIdName);
					ReIDTaskShowResult addRe = new ReIDTaskShowResult();
					addRe.setLabel_task_id(reTaskId);
					addRe.setReid_name(reIdName);
					addRe.setRelated_info(gson.toJson(relatedMap));
					addList.add(addRe);
				}
			}
			if(addList.size() > 0) {
				reIDTaskShowResultDao.addBatchShowResultItem(addList);
			}
			for(Entry<String,String> entry : existsMap.entrySet()) {
				Map<String,Object> paramMap = new HashMap<>();
				paramMap.put("label_task_id",  entry.getValue());
				paramMap.put("reid_name", entry.getKey());
				logger.info("reid_name=" + entry.getKey() + " reidTaskId=" + entry.getValue() + " is not exist. need to delete.");
				reIDTaskShowResultDao.deleteByReIDTaskAndReIdName(paramMap);
			}

		}
	}

	public void createImage(String imgName, Map<String, Object> label, String pic_image_field,String reIdTaskId) {
		BufferedImage bufferImage = fileService.getBufferedImage(pic_image_field);
		if(bufferImage == null) {
			logger.info("image is null. path=" + pic_image_field);
			return;
		}

		@SuppressWarnings("unchecked")
		List<Object> boxList = (List<Object>)label.get("box");
		if(boxList != null) {//矩形标注
			int xmin = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(0)));
			int ymin = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(1)));
			int xmax = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(2)));
			int ymax = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(3)));
			if(xmax-xmin <=0 || ymax - ymin <=0) {
				return;
			}
			if(xmin < 0) {
				xmin = 0;
			}
			if(xmax <= 0) {
				xmax = 1;
			}
			if(xmax >= bufferImage.getWidth()) {
				xmax =  bufferImage.getWidth();
			}
			if(xmin >= bufferImage.getWidth()) {
				xmin = bufferImage.getWidth() - 1;
			}
			if(ymax >= bufferImage.getHeight()) {
				ymax = bufferImage.getHeight();
			}
			if(ymin >= bufferImage.getHeight()) {
				ymin = bufferImage.getHeight() - 1;
			}
			try {
				BufferedImage subImage1 = cutBufferedImage1(bufferImage, xmin, ymin, xmax - xmin, ymax-ymin);
				BufferedImage subImage2 = cutBufferedImage1(bufferImage, xmin, ymin, xmax - xmin, ymax-ymin);
				BufferedImage subImage = subImage2;
				if(subImage1.getData().getDataBuffer().getSize() < subImage2.getData().getDataBuffer().getSize()) {
					subImage = subImage2;
				}
				
				String name = imgName;
				File tmpImage = new File(System.getProperty("user.dir"),name);
				try {
					if(tmpImage.exists()) {
						tmpImage.delete();
					}
					ImageIO.write(subImage, "jpg", tmpImage);
					fileService.uploadPictureFile(Arrays.asList(tmpImage), reIdTaskId);
				} catch (IOException e) {
					e.printStackTrace();
				}finally {
					tmpImage.delete();
				}
			}catch (Exception e) {
				logger.info(e.getMessage(),e);
			}
		}
	}
	
	public BufferedImage cutBufferedImage2(BufferedImage srcBfImg, int x, int y, int width, int height) {
		BufferedImage subImage = srcBfImg.getSubimage(x, y, width, height);
		
		return subImage;
	}

	public BufferedImage cutBufferedImage1(BufferedImage srcBfImg, int x, int y, int width, int height) {
		BufferedImage cutedImage = null;
		CropImageFilter cropFilter = new CropImageFilter(x, y, width, height);  
		Image img = Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(srcBfImg.getSource(), cropFilter));  
		cutedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);  
		Graphics g = cutedImage.getGraphics();  
		g.drawImage(img, 0, 0, null);  
		g.dispose(); 
		return cutedImage;
	}

	private void execReIDTask(ReIDTask reIDTask) {
		long start = System.currentTimeMillis();
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", reIDTask.getId());

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

			String trainDir = dataDir + "/market1501/test/bounding_box_train/";
			new File(trainDir).mkdirs();

			script = script.replace("{data_dir}", dataDir);

			List<LabelTaskItem> labelTaskItemList = reIdLabelTaskItemDao.queryLabelTaskItemByLabelTaskId(TokenManager.getUserTablePos(reIDTask.getUser_id(), UserConstants.REID_TASK_SINGLE_TABLE),reIDTask.getId());

			Map<String, Map<String, String>> imagesForLabelItemId = getImageForLabelItemIdMap(labelTaskItemList);

			Map<String, String> srcLabelItemMap = imagesForLabelItemId.get(reIDTask.getSrc_predict_taskid());

			int srcLabelItemNum = srcLabelItemMap == null? 0: srcLabelItemMap.size();
			int destlabelItemNum = labelTaskItemList.size() - srcLabelItemNum;

			paramMap.put("task_status", Constants.REID_TASK_STATUS_AUTO_PROGRESSING);
			paramMap.put("task_status_desc", (srcLabelItemNum * destlabelItemNum) / 10 + 30);//预计需要花费的时间
			reIDTaskDao.updateReIDTask(paramMap);	

			int srcImageNum = createSrcQueryImage(labelTaskItemList,reIDTask,dataDir);
			uploadCutImageToMinio(reIDTask,getSrcQueryImagePath(dataDir));

			String destIdStrs = reIDTask.getDest_predict_taskid();
			List<String> destIdList = JsonUtil.getList(destIdStrs);

			for(String destId : destIdList) {//对于每个关联的标注任务都要处理一次

				String labelTaskName = getLabelTaskName(destId,reIDTask);

				int destImageNum = createDestTestImage(labelTaskItemList,destId,dataDir);

				int size = FileUtil.getAllFileList(getDestTestImagePath(dataDir)).size();
				if(size == 0) {
					logger.info("the labelTaskName=" + labelTaskName + " id=" + destId + "  has no label info.");
					continue;
				}
				uploadCutImageToMinio(reIDTask,getDestTestImagePath(dataDir));
				File resultJsonFile = new File(dataDir, "test.json");
				resultJsonFile.delete();

				ProcessExeUtil.execScript(script,algInstance.getAlg_root_dir(),30 + (srcImageNum * destImageNum) / 100);

				readResultAndSaveToDb(resultJsonFile,reIDTask,destId, labelTaskName,imagesForLabelItemId.get(destId));
			}

			FileUtil.delDir(dataDir);//删除

			paramMap.put("task_status", Constants.REID_TASK_STATUS_AUTO_FINISHED);
			paramMap.put("task_status_desc", "自动分类完成，共耗时：" + ((System.currentTimeMillis() - start) / 1000) + "s");
			logger.info("Finished ReID Task, cost:" + ((System.currentTimeMillis() - start) / 1000) + "s");
		}catch (Exception e) {
			logger.error("ReId error: " + e.getMessage(),e);
			paramMap.put("task_status", Constants.REID_TASK_STATUS_EXCEPTION);
			String message = e.getMessage();
			if(message.length() > 500) {
				message = message.substring(0,500);
			}
			paramMap.put("task_status_desc", message);
		}finally {
			reIDTaskDao.updateReIDTask(paramMap);	
		}
	}

	private Map<String, Map<String, String>> getImageForLabelItemIdMap(List<LabelTaskItem> labelTaskItemList) {
		Map<String,Map<String,String>> imagesForLabelItemId = new HashMap<>();
		for(LabelTaskItem item : labelTaskItemList) {
			Map<String,String> tmp = imagesForLabelItemId.get(item.getPic_url());
			if(tmp == null) {
				tmp = new HashMap<>();
				imagesForLabelItemId.put(item.getPic_url(), tmp);
			}
			String imageName = item.getPic_image_field();
			imageName = imageName.substring(imageName.lastIndexOf("/") + 1);
			tmp.put(imageName, item.getId());
		}
		return imagesForLabelItemId;
	}

	private String getLabelTaskName(String destId, ReIDTask reIDTask) {
		if(reIDTask.getTask_type() == Constants.REID_TASK_TYPE_AUTO) {
			PrePredictTask task = prePredictTaskDao.queryPrePredictTaskById(destId);
			if(task != null) {
				return task.getTask_name();
			}
		}else {
			DataSet dataSet = dataSetDao.queryDataSetById(destId);
			if(dataSet != null) {
				return dataSet.getTask_name();
			}
		}
		return "";
	}

	private int cutLabelImageToPath(List<LabelTaskItem> labelTaskItemList, int start,int end, String destDir) {
		FileUtil.delDir(destDir);
		new File(destDir).mkdirs();
		int count = 0;
		int size = labelTaskItemList.size();
		for(int i = start; i < end && i < size; i++) {
			LabelTaskItem item = labelTaskItemList.get(i);
			count += ImageCutUtil.cutImageToPath(item, destDir,fileService);
		}
		return count;
	}



	private int createDestTestImage(List<LabelTaskItem> labelTaskItemList, String destId, String dataDir) {
		String destImagePath = getDestTestImagePath(dataDir);
		FileUtil.delDir(destImagePath);
		new File(destImagePath).mkdirs();
		int count = 0;
		for(LabelTaskItem item : labelTaskItemList) {
			String taskId = item.getPic_url();
			if(destId.equals(taskId)) {
				count += ImageCutUtil.cutImageToPath(item, destImagePath,fileService);
			}
		}
		return count;
	}

	private int createSrcQueryImage(List<LabelTaskItem> labelTaskItemList,ReIDTask reIDTask,String dataDir) {
		String srcTaskId = reIDTask.getSrc_predict_taskid();
		logger.info("item size=" + labelTaskItemList.size());
		String srcCutImagePath =  getSrcQueryImagePath(dataDir);

		FileUtil.delDir(srcCutImagePath);
		new File(srcCutImagePath).mkdirs();
		int count = 0;
		for(LabelTaskItem item : labelTaskItemList) {
			String taskId = item.getPic_url();
			if(srcTaskId.equals(taskId)) {
				count += ImageCutUtil.cutImageToPath(item, srcCutImagePath,fileService);
			}
		}
		return count;
	}

	private void readResultAndSaveToDb(File resultJsonFile,ReIDTask reIDTask,String destId,String labelTaskName,Map<String,String> imageForLabelItemId) throws LabelSystemException {
		if(resultJsonFile.exists()) {

			logger.info("save result to db.");
			String content = FileUtil.getAllContent(resultJsonFile.getAbsolutePath(), "utf-8");

			Map<String,List<String>> labelMap = gson.fromJson(content, new TypeToken<Map<String,List<String>>>() {
				private static final long serialVersionUID = 1L;}.getType());

			logger.info("size = " + labelMap.size());

			List<ReIDTaskResult> resultList = new ArrayList<>();
			for(Entry<String,List<String>> entry : labelMap.entrySet()) {
				String srcImgName = entry.getKey();
				List<String> destImgList = entry.getValue();
				List<Map<String,String>> realList = new ArrayList<>();//需要List，因为得分高低要有序
				for(String tmp : destImgList) {
					if(tmp.equals(srcImgName)) {
						continue;
					}
					Map<String,String> info = new HashMap<>();
					String realImageName = tmp.substring(0,tmp.lastIndexOf("_"));
					realImageName = realImageName + ".jpg";
					info.put("/minio/" + reIDTask.getId() + "/" + tmp, imageForLabelItemId.get(realImageName));
					realList.add(info);
				}
				ReIDTaskResult re = new ReIDTaskResult();
				re.setId(reIDTask.getId());
				re.setLabel_task_id(destId);
				re.setLabel_task_name(labelTaskName);
				re.setSrc_image_info(srcImgName);
				re.setRelated_info(gson.toJson(realList));
				resultList.add(re);
			}
			if(!resultList.isEmpty()) {
				reIDTaskResultDao.addBatchTaskItem(resultList);
			}else {
				logger.info("error, size = 0");
			}
		}else {
			throw new LabelSystemException("自动行人再识别算法结果异常。");
		}

	}
	//30分钟
	private void uploadCutImageToMinio(ReIDTask reIDTask,String dataDir) {
		String destMinioBucketName = reIDTask.getId();
		List<File> fileList = FileUtil.getAllFileList(dataDir);
		fileService.uploadPictureFile(fileList, destMinioBucketName);
	}

	private String getDataDir(String algRootPath) {
		return algRootPath + "data" + File.separator + System.nanoTime();
	}

	private String getDestTestImagePath(String dataDir) {
		return dataDir + "/market1501/test/bounding_box_test/";
	}

	private String getSrcQueryImagePath(String dataDir) {
		return dataDir +  "/market1501/test/query/";
	}




}
