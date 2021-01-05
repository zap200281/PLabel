package com.pcl.service.schedule;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

import javax.annotation.PostConstruct;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pcl.constant.Constants;
import com.pcl.dao.DataSetDao;
import com.pcl.dao.LargePictureTaskDao;
import com.pcl.dao.ProgressDao;
import com.pcl.pojo.Progress;
import com.pcl.pojo.mybatis.DataSet;
import com.pcl.pojo.mybatis.LargePictureTask;
import com.pcl.service.LabelDataSetMerge;
import com.pcl.service.ObjectFileService;
import com.pcl.util.FileUtil;
import com.pcl.util.JsonUtil;
import com.pcl.util.LargePictureUtil;

	
@Service
public class LargeImgToDziSchedule {
	
	private static Logger logger = LoggerFactory.getLogger(LargeImgToDziSchedule.class);

	private ArrayBlockingQueue<DataSet> queue = new ArrayBlockingQueue<>(10000);
	
	@Autowired
	private DataSetDao dataSetDao;
	
	@Autowired
	private ObjectFileService fileService;
	
	@Autowired
	private ProgressDao progressDao;
	
	@Autowired
	private LargePictureTaskDao largePictureTaskDao;
	
	@Value("${server.port}")
    private String port;
	
	@Value("${dzi.port:8020}")
	private int dziPort;
	
	@Value("${msgresttype:https}")
	private String msgresttype;

	@Value("${msgrestip:127.0.0.1}")
	private String msgrestip;
	
	@Value("${server.enable.bigimg:true}")
	private boolean enable = true;

	public boolean addTask(DataSet dataSet) {
		waitTime = 10;
	
		return queue.offer(dataSet);
	}
	
	private int waitTime = 10;
	
	@PostConstruct
	public void init() {
		
		if(!enable) {
			logger.info("not start big image schedule.");
			return;
		}
		logger.info("start to load big image db data.");
		
		loadTaskFromDb();
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				while(true) {
					try {
						//判断是否空闲
						if(isFree()) {
							logger.info("the dzi is free.");
							DataSet dataset = queue.take();
							
							logger.info("start to deal svs file to dzi. file=" + dataset.getZip_object_name());
							
							execSvsToDzi(dataset);
							
						}else {
							waitSecond(waitTime);
							waitTime += 1;
						}
					}catch (Exception e) {
						logger.info("Failed to predict label picture.");
						e.printStackTrace();
						waitSecond(waitTime);
						waitTime += 1;
					}
				}

			}

		
		},"LargeSvsSchedule").start();
	}

	private void execSvsToDzi(DataSet dataSet) {
		long start = System.currentTimeMillis();
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", dataSet.getId());
		paramMap.put("task_status", Constants.DATASET_PROCESS_ZOOM_SVS);
		dataSetDao.updateDataSet(paramMap);
		
		String tmpPath = LabelDataSetMerge.getUserDataSetPath() + File.separator + "svs_" +  System.nanoTime();
		//获取图片。
		try {
			new File(tmpPath).mkdirs();
			String downloadFilePath = fileService.downLoadFileFromMinio(dataSet.getZip_bucket_name(), dataSet.getZip_object_name(), tmpPath);
			File downloadFile = new File(downloadFilePath);
			
			long time = ( downloadFile.length() / 100 * 1000 * 1000 ) * 20;
			
			setProgress(dataSet.getId(),(int)time);

			LargePictureUtil.zoomSvsFile(downloadFile.getAbsolutePath(), dataSet.getId(),msgresttype,msgrestip,port,String.valueOf(dziPort));
			
			logger.info("finished deal svs picture, cost " + (System.currentTimeMillis() - start) + " ms");
			logger.info("wait to upload minio.");
			String dziPath = "/home/image2dzi/" + downloadFile.getName() + ".dzi";
			if(new File(dziPath).exists()) {
				Map<String,Object> dziMap = new HashMap<>();
				
				getDziInfo(dziMap, dziPath);
				
				String dzifilepath = "/home/image2dzi/"+ downloadFile.getName() + "_files";
				
				uploadFileToMinio(dataSet.getId(), dziMap, dzifilepath);
				
				if(dataSet.getVideoSet() != null && "autoAddLabelTask".equals(dataSet.getVideoSet())) {
					logger.info("autoAddLabelTask is true.");
					//更新标注任务状态
					List<LargePictureTask> re = largePictureTaskDao.queryLargePictureTaskByDataSetId(dataSet.getId());
					if(re != null) {
						for(LargePictureTask largeTask : re) {
							if(largeTask.getTask_status() == Constants.LARGE_TASK_STATUS_NOT_START) {
								Map<String,Object> statusMap = new HashMap<>();
								statusMap.put("task_status", Constants.LARGE_TASK_STATUS_START);
								statusMap.put("id", largeTask.getId());
								largePictureTaskDao.updateLargePictureTaskStatus(statusMap);
							}
						}
						
					}else {
						logger.info("not found large dataset.");
					}
				}
			}
			logger.info("finshed upload minio. total cost=" + (System.currentTimeMillis() - start) + " ms");
		}catch (Exception e) {
			logger.info(e.getMessage(),e);
		}finally {
			FileUtil.delDir(tmpPath);
		}
	}
	

	private void getDziInfo(Map<String, Object> msg, String dziPath) {
		File dziFile = new File(dziPath); 
		if(dziFile.exists()) {
			SAXReader reader = new SAXReader();
			Document document;
			try {
				document = reader.read(new FileInputStream(dziPath));
				Element root = document.getRootElement();
				msg.put("Format", root.attributeValue("Format"));
				msg.put("Overlap", root.attributeValue("Overlap"));
				msg.put("TileSize", root.attributeValue("TileSize"));
				
				Element size = root.element("Size");
				
				msg.put("Height", size.attributeValue("Height"));
				msg.put("Width", size.attributeValue("Width"));
			
			}catch (Exception e) {
				logger.info(e.getMessage(),e);
			}
			dziFile.delete();
		}
	}
	
	public void uploadFileToMinio(String dataSetId, Map<String, Object> msg,String dzifilepath) {
		String bucketName = UUID.randomUUID().toString().replaceAll("-","");
		
		if(dzifilepath != null && dzifilepath.length() >0) {
			File dziFile = new File(dzifilepath);
			File dziDirFiles[] = dziFile.listFiles();
			for(File dziDir : dziDirFiles) {
				String name = dziDir.getName();
				File jpegFiles[] = dziDir.listFiles();
				
				for(File jpegFile : jpegFiles) {
					String jpegName = jpegFile.getName();
					String objectName = name + "_" + jpegName;
					fileService.uploadSvsDziFile(jpegFile,objectName,bucketName);
					jpegFile.delete();
				}
			}
		}
		FileUtil.delDir(dzifilepath);
		
		msg.put("dziBucketName", bucketName);
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("id", dataSetId);
		paramMap.put("mainVideoInfo", JsonUtil.toJson(msg));
		paramMap.put("file_bucket_name", bucketName);
		paramMap.put("total", 1);
		paramMap.put("task_status", Constants.DATASET_PROCESS_PREPARE);
		dataSetDao.updateDataSet(paramMap);
		logger.info("finished upload file to minio.");
		progressDao.deleteProgress(dataSetId);
	}
	
	private void setProgress(String id, int time) {
		Progress pro = new Progress();
		pro.setId(id);
		pro.setStartTime(System.currentTimeMillis()/1000);
		pro.setExceedTime(time);
		
		Progress tmp = progressDao.queryProgressById(id);
		if(tmp != null) {
			progressDao.deleteProgress(id);
		}
		
		progressDao.addProgress(pro);
	}
	
	
	private boolean isFree() {
		
		String json = LargePictureUtil.getMsg(String.valueOf(dziPort));
		
		Map<String,Object> map = JsonUtil.getMap(json);
			
		if(map.get("isbusy") != null && map.get("isbusy").toString().equals("false")) {
			return true;
		}
		if(map.get("isbusy") != null && map.get("isbusy").toString().equals("true")) {
			if(map.get("percent") != null ) {
				String percent =map.get("percent").toString(); 
				double percentd = Double.parseDouble(percent);
				if(percentd >0.999) {
					logger.info("the percent is 100.");
					return true;
				}
			}
		}
		return false;
	}

	protected void waitSecond(int second) {
		try {
			Thread.sleep(second * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void loadTaskFromDb() {
		Map<String,Object> paramMap = new HashMap<>();
		List<String> typeList = new ArrayList<>();

		typeList.add(String.valueOf(Constants.DATASET_TYPE_SVS));
		paramMap.put("datasettypeList", typeList);
		paramMap.put("task_status", Constants.DATASET_PROCESS_NOT_START);
		List<DataSet> dataSetList = dataSetDao.queryDataSetByType(paramMap);

		for(DataSet dataSet : dataSetList) {
			addTask(dataSet);
		}
	}
}
