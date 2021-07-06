package com.pcl.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.util.Strings;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.pcl.constant.Constants;
import com.pcl.constant.UserConstants;
import com.pcl.dao.DataSetDao;
import com.pcl.dao.LabelDcmTaskItemDao;
import com.pcl.dao.LabelTaskDao;
import com.pcl.dao.LabelTaskItemDao;
import com.pcl.dao.LargePictureTaskDao;
import com.pcl.dao.LargePictureTaskItemDao;
import com.pcl.dao.PrePredictTaskDao;
import com.pcl.dao.ProgressDao;
import com.pcl.dao.ReIDLabelTaskItemDao;
import com.pcl.dao.ReIDTaskDao;
import com.pcl.dao.ReIDTaskShowResultDao;
import com.pcl.dao.VideoCountTaskDao;
import com.pcl.dao.VideoCountTaskItemDao;
import com.pcl.dao.VideoLabelTaskDao;
import com.pcl.dao.VideoLabelTaskItemDao;
import com.pcl.pojo.Progress;
import com.pcl.pojo.mybatis.DataSet;
import com.pcl.pojo.mybatis.LabelTask;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.pojo.mybatis.LargePictureTask;
import com.pcl.pojo.mybatis.PrePredictTask;
import com.pcl.pojo.mybatis.ReIDTask;
import com.pcl.pojo.mybatis.ReIDTaskShowResult;
import com.pcl.pojo.mybatis.VideoCountTask;
import com.pcl.pojo.mybatis.VideoLabelTask;
import com.pcl.service.schedule.ThreadSchedule;
import com.pcl.util.CocoAnnotationsUtil;
import com.pcl.util.FileUtil;
import com.pcl.util.JsonUtil;
import com.pcl.util.PclJsonAnnotationsUtil;
import com.pcl.util.ReIDUtil;
import com.pcl.util.TimeUtil;
import com.pcl.util.VocAnnotationsUtil;

/**
 * 标注数据导出服务
 * @author 邹安平
 *
 */
@Service
public class LabelExportService {

	private static Logger logger = LoggerFactory.getLogger(LabelExportService.class);

	@Autowired
	private LabelTaskItemDao labelTaskItemDao;

	@Autowired
	private LabelDcmTaskItemDao labelDcmTaskItemDao;

	@Autowired
	private LabelTaskDao labelTaskDao;

	//@Autowired
	//private CocoAnnotationsUtil cocoUtil;

	@Autowired
	private VocAnnotationsUtil vocUtil;

	@Autowired
	private PclJsonAnnotationsUtil pclJsonUtil;

	@Autowired
	private ReIDLabelTaskItemDao reIdLabelTaskItemDao;

	@Autowired
	private ReIDTaskShowResultDao reIDTaskShowResultDao;

	@Autowired
	private ObjectFileService minioFileService;

	@Autowired
	private VideoCountTaskItemDao videoCountTaskItemDao;

	@Autowired
	private VideoCountTaskDao videoCountTaskDao;

	@Autowired
	private VideoLabelTaskDao videoLabelTaskDao;

	@Autowired
	private VideoLabelTaskItemDao videoLabelTaskItemDao;

	@Autowired
	private ReIDTaskDao reIdTaskDao;

	@Autowired
	private PrePredictTaskDao predictTaskDao;

	@Autowired
	private LargePictureTaskDao largePictureTaskDao;

	@Autowired
	private LargePictureTaskItemDao largePictureTaskItemDao;

	@Autowired
	private DataSetDao dataSetDao;

	@Autowired
	private ProgressDao progressDao;

	private Gson gson = new Gson();

	//private Map<String,Progress> progressMap = new ConcurrentHashMap<>();

	private static final String LABEL_POSTFIX = "label";
	private static final String DATASET_POSTFIX = "dataset";
	private static final String REID_POSTFIX = "reID";
	private static final String VIDEO_POSTFIX = "video";
	private static final String LARGEPICTURE_POSTFIX = "largepicture";
	private static final String VIDEO_COUNT_POSTFIX = "videocount";


	private void putProgress(Progress pro) {

		Progress tmp = progressDao.queryProgressById(pro.getId());
		if(tmp != null) {
			progressDao.deleteProgress(pro.getId());
		}

		progressDao.addProgress(pro);
	}

	private void updateProgress(String id,long process) {
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", id);
		paramMap.put("progress", process);
		progressDao.updateProgress(paramMap);
	}

	private void updateProgress(Progress pro,long process) {
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", pro.getId());
		paramMap.put("progress", (long)(process * pro.getRatio() + pro.getBase()));
		progressDao.updateProgress(paramMap);
	}
	
	
	public String downloadLabelTaskFile(String labelTaskId,int type,double maxscore,double minscore) throws IOException {
		List<String> idList = new ArrayList<>();
		LabelTask labelTask = null;
		if(labelTaskId.startsWith("[")) {
			//Json格式
			idList.addAll(JsonUtil.getList(labelTaskId));
		}else {
			idList.add(labelTaskId);
			labelTask = labelTaskDao.queryLabelTaskById(labelTaskId);
		}
		String key = System.nanoTime() + LABEL_POSTFIX;

		String relatedName =  System.nanoTime() + File.separator + TimeUtil.getCurrentTimeStrByyyyyMMddHHmmss() +"_" + LABEL_POSTFIX + ".zip";
		if(labelTask != null) {
			relatedName =  System.nanoTime() + File.separator + FileUtil.getRemoveChineseCharName(labelTask.getTask_name()) +"_" + LABEL_POSTFIX + ".zip";
		}

		Progress pro = new Progress();
		pro.setId(key);
		pro.setStartTime(System.currentTimeMillis() / 1000);
		pro.setExceedTime(10 * 60);
		pro.setRelatedFileName(relatedName);

		putProgress(pro);
		String fileName = LabelDataSetMerge.getUserDataSetPath() + File.separator + relatedName;

		ThreadSchedule.execExportThread(()->{
			try {
				int total = idList.size();
				pro.setRatio( 1.0 / total);
				List<String> fileList = new ArrayList<>();

				double base = 0;
				for(String tmpTaskId : idList) {


					LabelTask tmpTask = labelTaskDao.queryLabelTaskById(tmpTaskId);
					String tmpFileName=   LabelDataSetMerge.getUserDataSetPath() + File.separator + System.nanoTime() + File.separator + FileUtil.getRemoveChineseCharName(tmpTask.getTask_name()) +"_" + LABEL_POSTFIX + ".zip";;
					
					downloadLabelTaskFileWriter(tmpTask, tmpFileName, type, pro,maxscore,minscore);
					fileList.add(tmpFileName);

					base+= pro.getRatio();
					pro.setBase((long)(base * 100));

					//确保文件写完成了，进度再更新到100
				}
				File zipFile = new File(fileName) ;
				zipFile.getParentFile().mkdirs();
				try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
					for(String azipFileName : fileList) {
						File aZipFile = new File(azipFileName);
						zos.putNextEntry(new ZipEntry(aZipFile.getName()));
						byte buffer[] = new byte[2048];
						try(FileInputStream inputStream = new FileInputStream(aZipFile)) {
							while(true) {
								int length = inputStream.read(buffer);
								if(length < 0) {
									break;
								}
								zos.write(buffer, 0, length);
							}
							zos.closeEntry();
						}
					}
				}
				pro.setInfo("压缩完成。");
				//pro.setProgress(100l);
				updateProgress(pro.getId(), 100);
				logger.info("writer finished.fileName=" + fileName);
			} catch (Exception e) {
				pro.setId(e.getMessage());
				logger.info(e.getMessage(),e);
			}
		});

		return key;
	}

	private Map<String,Object> getTypeOrColorName(Map<String,Object> typeKeyValue){
		Map<String,Object> re = new HashMap<>();
		dealMapName(typeKeyValue, re,"type");
		dealMapName(typeKeyValue, re,"color");
		return re;
	}

	private void dealMapName(Map<String, Object> typeKeyValue, Map<String, Object> re,String name) {
		if(typeKeyValue.get(name) != null) {
			Map<String,Object> typeMap = (Map<String,Object>)typeKeyValue.get(name);
			if(typeMap.get("options") != null) {
				Map<String,Object> typeNameMap = (Map<String,Object>)typeMap.get("options");
				re.put(name, typeNameMap);
			}
		}
	}

	public void downloadLabelTaskFileWriter(LabelTask labelTask,String fileName,int type,Progress pro,double maxscore,double minscore) throws IOException {

		Map<String,Object> typeKeyValue = JsonUtil.getMap(labelTask.getTask_label_type_info());
		Map<String,Object> typeOrColorMapName = getTypeOrColorName(typeKeyValue);

		int userId = labelTask.getUser_id();

		long start = System.currentTimeMillis();

		long total = 0;
		int count = 0;

		File zipFile = new File(fileName) ;
		zipFile.getParentFile().mkdirs();
		try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {

			//写XML格式
			Map<String,Object> dataSetInfo = getDataSetInfo(labelTask.getRelate_task_id(),labelTask);


			Map<String,Object> tmpParam = new HashMap<>();
			tmpParam.put("label_task_id", labelTask.getId());
			tmpParam.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE));
			//int count = 0;

			if(labelTask.getTask_type() == Constants.LABEL_TASK_TYPE_ORIGIN_DCM) {
				total = labelDcmTaskItemDao.queryLabelTaskItemPageCountByLabelTaskId(tmpParam);
			}else {
				total = labelTaskItemDao.queryLabelTaskItemPageCountByLabelTaskId(tmpParam);
			}

			int pageSize = 1000;
			for(int i = 0; i < (total/pageSize) +1; i++) {
				tmpParam.put("currPage", i * pageSize);
				tmpParam.put("pageSize", pageSize);
				if(labelTask.getTask_type() == Constants.LABEL_TASK_TYPE_ORIGIN_DCM) {
					List<LabelTaskItem> itemList = labelDcmTaskItemDao.queryLabelTaskItemPageByLabelTaskId(tmpParam);
					for(LabelTaskItem item : itemList) {
						writerDataToZip(type, pro, typeOrColorMapName, total, count, zos, dataSetInfo, item,maxscore,minscore);
						count++;
					}
				}else {
					List<LabelTaskItem> itemList =labelTaskItemDao.queryLabelTaskItemPageByLabelTaskId(tmpParam);
					for(LabelTaskItem item : itemList) {
						writerDataToZip(type, pro, typeOrColorMapName, total, count, zos, dataSetInfo, item,maxscore,minscore);
						count++;
					}
				}
			}

			long end = System.currentTimeMillis();

			logger.info("finished zip, filename=" +fileName + " ,耗时：" + (end - start) +" ms");
		} catch (Exception e) {
			throw new RuntimeException("zip error from ZipUtils",e);
		}

	}

	private void writerDataToZip(int type, Progress pro, Map<String, Object> typeOrColorMapName, long total, int count,
			ZipOutputStream zos, Map<String, Object> dataSetInfo, LabelTaskItem item,double maxscore,double minscore) throws IOException {
		if(type ==3 ) {
			//抠图
			writeCutImageToOutputZip(item, zos,maxscore,minscore);

		}else {
			writeXml(item, zos,dataSetInfo,typeOrColorMapName);
			writeJson(item,zos,dataSetInfo,typeOrColorMapName);
			if(type == 2) {
				try(InputStream intpuStream = minioFileService.getImageInputStream(item.getPic_image_field())){
					if(intpuStream == null) {
						return;
					}
					String name = item.getPic_image_field().substring(item.getPic_image_field().lastIndexOf("/") + 1);
					zos.putNextEntry(new ZipEntry("img/" + name));
					byte buffer[] = new byte[2048];
					while(true) {
						int length = intpuStream.read(buffer);
						if(length < 0) {
							break;
						}
						zos.write(buffer, 0, length);
					}
					zos.closeEntry();
				}
			}
		}
		if(count != total) {
			long progressInt = (long)((count *1.0 / total) * 100);

			updateProgress(pro,progressInt);
			//pro.setProgress((long)((count *1.0 / total) * 100));
		}
	}

	private int writeCutImageToOutputZip(LabelTaskItem item, ZipOutputStream zos,double maxscore,double minscore) {
		int count = 0;
		String jsonLabelInfo = item.getLabel_info();
		if(Strings.isBlank(jsonLabelInfo)) {
			logger.info("jsonLabelInfo is null. jsonLabelInfo=" + jsonLabelInfo);
			return count;
		}
		ArrayList<Map<String,Object>> labelList = gson.fromJson(jsonLabelInfo, new TypeToken<ArrayList<Map<String,Object>>>() {
			private static final long serialVersionUID = 1L;}.getType());
		if(labelList.isEmpty()) {
			logger.info("jsonLabelInfo is empty. jsonLabelInfo=" + jsonLabelInfo);
			return count;
		}

		BufferedImage bufferImage = minioFileService.getBufferedImage(item.getPic_image_field());
		if(bufferImage == null) {
			logger.info("image is null. path=" + item.getPic_image_field());
			return count;
		}

		String imageName = item.getPic_image_field();
		imageName = imageName.substring(imageName.lastIndexOf("/") + 1);
		imageName = imageName.substring(0,imageName.length() - 4);
		for(Map<String,Object> label : labelList) {

			Object idObj = label.get("id");
			if(idObj == null) {
				continue;
			}
			String id = idObj.toString();
			
			double score = getScore(label);
			
			if(score > maxscore || score < minscore) {
				continue;
			}

			List<Object> boxList = (List<Object>)label.get("box");
			if(boxList != null) {//矩形标注
				int xmin = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(0)));
				int ymin = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(1)));
				int xmax = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(2)));
				int ymax = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(3)));
				if(xmax-xmin <=0 || ymax - ymin <=0) {
					continue;
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
					BufferedImage subImage = bufferImage.getSubimage(xmin, ymin, xmax-xmin, ymax-ymin);
					String name = imageName + "_" + id + ".jpg";
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					ImageIO.write(subImage, "jpg", out);
					byte data[] = out.toByteArray();
					zos.putNextEntry(new ZipEntry("img/" + name));
					zos.write(data, 0, data.length);
					zos.closeEntry();
				} catch (Exception e) {
					logger.info(e.getMessage());
				}
			}
		}
		return count;

	}



	private double getScore(Map<String, Object> label) {
		Object scoreObj = label.get("score");
		if(scoreObj != null && scoreObj.toString().length() > 0) {
			return Double.parseDouble(scoreObj.toString());
		}
		return 1.0;
	}

	private void writeJson(LabelTaskItem item, ZipOutputStream zos,Map<String,Object> pictureInfo,Map<String,Object> typeOrColorMapName) throws IOException {
		Map<String,Object> jsonMap = pclJsonUtil.getJson(item,typeOrColorMapName);
		String relativeFileName = item.getPic_image_field();
		jsonMap.putAll(pictureInfo);
		writeJson(jsonMap, relativeFileName, zos,"json");
	}

	private void writeJson(Map<String,Object> jsonMap,String imageName, ZipOutputStream zos,String writerDir) throws IOException {
		String relativeFileName = imageName;
		String fileName = relativeFileName.substring(relativeFileName.lastIndexOf("/") +1);
		fileName = fileName.substring(0,fileName.lastIndexOf(".")) + ".json";
		String json = gson.toJson(jsonMap);
		byte[] xmlBytes = json.getBytes("utf-8");
		zos.putNextEntry(new ZipEntry(writerDir + "/" + fileName));
		int len = xmlBytes.length;
		zos.write(xmlBytes, 0, len);
		zos.closeEntry();
	}

	private void writeXml(LabelTaskItem item,ZipOutputStream zos,Map<String,Object> pictureInfo,Map<String,Object> typeOrColorMapName) throws IOException {
		Document doc = vocUtil.getXmlDocument(item,pictureInfo,typeOrColorMapName);
		if(doc == null) {
			return;
		}
		String relativeFileName = item.getPic_image_field();
		String fileName = relativeFileName.substring(relativeFileName.lastIndexOf("/") +1);
		fileName = fileName.substring(0,fileName.lastIndexOf(".")) +  ".xml";
		StringWriter strWriter = new StringWriter();
		OutputFormat format = new OutputFormat("\t", true);
		format.setTrimText(true);//去掉原来的空白(\t和换行和空格)！

		XMLWriter writer = new XMLWriter(strWriter, format);
		// 把document对象写到out流中。
		writer.write(doc);
		byte[] xmlBytes = strWriter.toString().getBytes("utf-8");
		zos.putNextEntry(new ZipEntry("xml/" + fileName));
		int len = xmlBytes.length;
		zos.write(xmlBytes, 0, len);
		zos.closeEntry();
	}



	public String downloadReIdTaskFile(String reIdTaskId,String type) throws IOException {
		ReIDTask reIdtask = reIdTaskDao.queryReIDTaskById(reIdTaskId);

		String key = reIdTaskId + REID_POSTFIX;

		String taskName = FileUtil.getRemoveChineseCharName(reIdtask.getTask_name());
		
		String relatedName =  System.nanoTime() + File.separator + taskName + "_ReID" + ".zip";
		if(type.equals(Constants.REID_EXPORT_TYPE_REID_PICTURE)) {
			relatedName =  System.nanoTime() + File.separator + taskName + "_ReID_CutImage" + ".zip";
		}else if(type.equals(Constants.REID_EXPORT_TYPE_REID_PICTURE_RENAME)) {
			relatedName =  System.nanoTime() + File.separator + taskName + "_ReID_CutImage_DS" + ".zip";
		}

		String fileName = LabelDataSetMerge.getUserDataSetPath() + File.separator + relatedName;
		Progress pro = new Progress();
		pro.setId(key);
		pro.setStartTime(System.currentTimeMillis() / 1000);
		pro.setExceedTime(10 * 60);
		pro.setRelatedFileName(relatedName);
		pro.setBase(0);
		pro.setRatio(1);
		putProgress(pro);

		ThreadSchedule.execExportThread(()->{
			try {
				downloadReIdTaskFileWriter(reIdtask,type, fileName, pro);
			} catch (Exception e) {
				pro.setId(e.getMessage());
				logger.info(e.getMessage(),e);
			}
		});

		return key;

	}

	public void downloadReIdTaskFileWriter(ReIDTask reIdtask,String type,String fileName1,Progress pro) throws IOException {
		File zipFile = new File(fileName1) ;
		zipFile.getParentFile().mkdirs();

		Map<String,Object> typeKeyValue = JsonUtil.getMap(reIdtask.getTask_label_type_info());
		Map<String,Object> typeOrColorMapName = getTypeOrColorName(typeKeyValue);

		//int total = 0;

		if(type.equals(Constants.REID_EXPORT_TYPE_REID_PICTURE)) {
			writerReIDCutImage(reIdtask, pro, zipFile, typeOrColorMapName);
		}else if(type.equals(Constants.REID_EXPORT_TYPE_REID_PICTURE_RENAME)) {
			writerReIDCutImageReName(reIdtask, pro, zipFile, typeOrColorMapName);
		}
		else if(type.equals(Constants.REID_EXPORT_TYPE_REID_LABEL)) {
			writeReIDWithPrimitivePic(reIdtask, pro, zipFile, typeOrColorMapName);
		}else if(type.equals(Constants.REID_EXPORT_TYPE_REID_ONLY_CUT)) {
			writerReIDOnlyCutImage(reIdtask, pro, zipFile, typeOrColorMapName);
		}else if(type.equals(Constants.REID_EXPORT_TYPE_LABEL)) {
			writeReIDWithLabel(reIdtask, pro, zipFile, typeOrColorMapName);
		}

	}

	private void writerReIDOnlyCutImage(ReIDTask reIdtask, Progress pro, File zipFile,
			Map<String, Object> typeOrColorMapName) {
		int total = 0;
		int count = 0;
		List<ReIDTaskShowResult> showList = reIDTaskShowResultDao.queryReIDShowTaskResultById(reIdtask.getId());
		StringBuilder strBuilder = new StringBuilder();
		long start = System.currentTimeMillis();

		total = showList.size();
		logger.info("writer cut image.");
		try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
			for(ReIDTaskShowResult item : showList) {
				strBuilder.append(item.getReid_name()).append("    ");

				String mapStr = item.getRelated_info();
				Map<String,String> map =  gson.fromJson(mapStr, new TypeToken<Map<String,String>>() {
					private static final long serialVersionUID = 1L;}.getType());

				List<String> imgList = new ArrayList<>();
				imgList.addAll(map.keySet());
				Collections.sort(imgList);
				for(int i = 0; i < imgList.size(); i++) {
					String imgPath = imgList.get(i);
					String entryFileName = imgPath.substring(imgPath.lastIndexOf("/") + 1);
					strBuilder.append(entryFileName);
					if(i < imgList.size() - 1) {
						strBuilder.append(" ");
					}

					try(InputStream intpuStream = minioFileService.getImageInputStream(imgPath)){
						if(intpuStream == null) {
							logger.info("the imagepath stream is null. imgpath=" + imgPath);
							continue;
						}
						zos.putNextEntry(new ZipEntry(item.getReid_name() + "/" + entryFileName));
						byte buffer[] = new byte[2048];
						while(true) {
							int length = intpuStream.read(buffer);
							if(length < 0) {
								break;
							}
							zos.write(buffer, 0, length);
						}

						zos.closeEntry();
					}
				}
				strBuilder.append("\n\r");
				count ++;
				if(count != total) {
					//pro.setProgress((long)((count *1.0 / total) * 100));
					updateProgress(pro,(long)((count *1.0 / total) * 100));
				}
			}
			zos.putNextEntry(new ZipEntry("reid_desc.csv"));
			byte[] bytes = strBuilder.toString().getBytes("utf-8");
			zos.write(bytes, 0, bytes.length);
			zos.closeEntry();

			long end = System.currentTimeMillis();
			logger.info("finished zip, cost: " + (end - start) +" ms");
		} catch (Exception e) {
			throw new RuntimeException("zip error from ZipUtils",e);
		}
		//pro.setProgress(100l);
		updateProgress(pro, 100);
	}

	private void writerReIDCutImageReName(ReIDTask reIdtask, Progress pro, File zipFile,
			Map<String, Object> typeOrColorMapName) {

		int total = 0;
		int count = 0;


		String primitiveId = reIdtask.getSrc_predict_taskid();
		String datasetName = "";
		if(reIdtask.getTask_type() == Constants.REID_TASK_TYPE_AUTO) {
			PrePredictTask pretask = predictTaskDao.queryPrePredictTaskById(primitiveId);
			if(pretask != null) {
				primitiveId = pretask.getDataset_id();
			}
		}
		DataSet dataSet  = dataSetDao.queryDataSetById(primitiveId);
		if(dataSet != null) {
			datasetName = dataSet.getTask_name();
		}

		List<ReIDTaskShowResult> showList = reIDTaskShowResultDao.queryReIDShowTaskResultById(reIdtask.getId());
		StringBuilder strBuilder = new StringBuilder();
		long start = System.currentTimeMillis();

		List<LabelTaskItem> reList = reIdLabelTaskItemDao.queryLabelTaskItemByLabelTaskId(TokenManager.getUserTablePos(reIdtask.getUser_id(), UserConstants.REID_TASK_SINGLE_TABLE),reIdtask.getId());

		total = showList.size() + reList.size();
		logger.info("writer cut image.");
		HashMap<String,HashMap<String,String>> renameMap = new HashMap<>();
		try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
			for(ReIDTaskShowResult item : showList) {
				String reidName = item.getReid_name();

				strBuilder.append(datasetName + "-" + reidName).append("    ");

				String mapStr = item.getRelated_info();
				Map<String,String> map =  gson.fromJson(mapStr, new TypeToken<Map<String,String>>() {
					private static final long serialVersionUID = 1L;}.getType());

				List<String> imgList = new ArrayList<>();
				imgList.addAll(map.keySet());
				Collections.sort(imgList);

				String imagePrefix = "";

				int indexTmp1 = datasetName.indexOf("-");
				if(indexTmp1 >=0) {
					indexTmp1 = datasetName.indexOf("-", indexTmp1 + 1);
					if(indexTmp1 > 0) {
						int indexTmp2 = datasetName.indexOf("-", indexTmp1 + 1);
						if(indexTmp2 != -1) {
							imagePrefix = datasetName.substring(indexTmp1 + 1, indexTmp2);
						}
					}
				}

				for(int i = 0; i < imgList.size(); i++) {
					String imgPath = imgList.get(i);

					String entryFileName = imagePrefix + "C" + String.format("%3d",i+1).replace(" ", "0") + ".jpg";
					//imgPath.substring(imgPath.lastIndexOf("/") + 1);

					String primitivePic = ReIDUtil.getPrimitivePicture(imgPath);
					String picName = primitivePic.substring(primitivePic.lastIndexOf("/") + 1);

					HashMap<String,String> reidReNameMap = renameMap.get(reidName);
					if(reidReNameMap == null) {
						reidReNameMap = new HashMap<>();
						renameMap.put(reidName, reidReNameMap);
					}
					reidReNameMap.put(picName, entryFileName);

					strBuilder.append(entryFileName);
					if(i < imgList.size() - 1) {
						strBuilder.append(" ");
					}

					try(InputStream intpuStream = minioFileService.getImageInputStream(imgPath)){
						if(intpuStream == null) {
							logger.info("the imagepath stream is null. imgpath=" + imgPath);
							continue;
						}
						zos.putNextEntry(new ZipEntry(datasetName + "-" + reidName + "/" + entryFileName));
						byte buffer[] = new byte[2048];
						while(true) {
							int length = intpuStream.read(buffer);
							if(length < 0) {
								break;
							}
							zos.write(buffer, 0, length);
						}

						zos.closeEntry();
					}catch(Exception e) {
						logger.info("the picture is not exist.imgPath=" +imgPath);
					}
				}
				strBuilder.append("\n\r");
				count ++;
				if(count != total) {
					//pro.setProgress((long)((count *1.0 / total) * 100));
					updateProgress(pro,(long)((count *1.0 / total) * 100));
				}
			}
			zos.putNextEntry(new ZipEntry("reid_desc.csv"));
			byte[] bytes = strBuilder.toString().getBytes("utf-8");
			zos.write(bytes, 0, bytes.length);
			zos.closeEntry();

			Map<String,Map<String,Object>> datasetInfo = new HashMap<>();
			logger.info("writer json label.");
			for(int i = 0; i < reList.size(); i++) {
				LabelTaskItem item = reList.get(i);
				Map<String,Map<String,Object>> reIdMap = pclJsonUtil.getReIdJson(item,typeOrColorMapName);

				String dataSetIdOrPredictId = item.getPic_url();
				if(datasetInfo.get(dataSetIdOrPredictId) == null) {
					Map<String,Object> dataSetInfo = getDataSetInfo(dataSetIdOrPredictId,reIdtask);
					datasetInfo.put(dataSetIdOrPredictId, dataSetInfo);
				}

				if(reIdMap.size() > 0) {
					for(Entry<String,Map<String,Object>> entry : reIdMap.entrySet()) {
						Map<String,Object> jsonMap = entry.getValue();
						if(datasetInfo.get(dataSetIdOrPredictId) != null) {
							jsonMap.putAll(datasetInfo.get(dataSetIdOrPredictId));
						}
						String picName = item.getPic_image_field().substring( item.getPic_image_field().lastIndexOf("/") + 1);
						HashMap<String,String> tmp = renameMap.get(entry.getKey());
						String jsonName =null;
						if(tmp != null ) {
							jsonName = tmp.get(picName);
						}else {
							logger.info("entry.getKey()=" + entry.getKey() + " picName=" + picName);
						}
						if(jsonName == null) {
							jsonName = item.getPic_image_field();
						}else {
							jsonName = "/" + jsonName;
						}

						writeJson(jsonMap, jsonName, zos, datasetName + "-" + entry.getKey());

					}
				}
				count ++;
				if(count != total) {
					//pro.setProgress((long)((count *1.0 / total) * 100));
					updateProgress(pro, (long)((count *1.0 / total) * 100));
				}
			}

			long end = System.currentTimeMillis();
			logger.info("finished zip, cost: " + (end - start) +" ms");
		} catch (Exception e) {
			throw new RuntimeException("zip error from ZipUtils",e);
		}
		//pro.setProgress(100l);
		updateProgress(pro, 100);

	}
	
	private void writeReIDWithLabel(ReIDTask reIdtask, Progress pro, File zipFile,
			Map<String, Object> typeOrColorMapName) {
		int total = 0;
		int count = 0;
		List<LabelTaskItem> reList = reIdLabelTaskItemDao.queryLabelTaskItemByLabelTaskId(TokenManager.getUserTablePos(reIdtask.getUser_id(), UserConstants.REID_TASK_SINGLE_TABLE),reIdtask.getId());
		total = reList.size();

		try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
			long start = System.currentTimeMillis();
			//写json格式
			for(int i = 0; i < total; i++) {
				LabelTaskItem item = reList.get(i);
				writeJson(item, zos, new HashMap<>(), typeOrColorMapName);
				count ++;
				if(count != total) {
					//pro.setProgress((long)((count *1.0 / total) * 100));
					updateProgress(pro, (long)((count *1.0 / total) * 100));
				}
			}
			long end = System.currentTimeMillis();
			logger.info("finished zip, cost: " + (end - start) +" ms");
		} catch (Exception e) {
			throw new RuntimeException("zip error from ZipUtils",e);
		}
		//pro.setProgress(100l);
		updateProgress(pro, 100);
	}
	

	private void writeReIDWithPrimitivePic(ReIDTask reIdtask, Progress pro, File zipFile,
			Map<String, Object> typeOrColorMapName) {
		int total = 0;
		int count = 0;
		List<LabelTaskItem> reList = reIdLabelTaskItemDao.queryLabelTaskItemByLabelTaskId(TokenManager.getUserTablePos(reIdtask.getUser_id(), UserConstants.REID_TASK_SINGLE_TABLE),reIdtask.getId());
		total = reList.size();

		try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
			long start = System.currentTimeMillis();
			//写json格式
			Map<String,Map<String,Object>> datasetInfo = new HashMap<>();


			for(int i = 0; i < total; i++) {
				LabelTaskItem item = reList.get(i);
				Map<String,Map<String,Object>> reIdMap = pclJsonUtil.getReIdJson(item,typeOrColorMapName);

				String dataSetIdOrPredictId = item.getPic_url();
				if(datasetInfo.get(dataSetIdOrPredictId) == null) {
					Map<String,Object> dataSetInfo = getDataSetInfo(dataSetIdOrPredictId,reIdtask);
					datasetInfo.put(dataSetIdOrPredictId, dataSetInfo);
				}

				if(reIdMap.size() > 0) {
					for(Entry<String,Map<String,Object>> entry : reIdMap.entrySet()) {
						Map<String,Object> jsonMap = entry.getValue();
						if(datasetInfo.get(dataSetIdOrPredictId) != null) {
							jsonMap.putAll(datasetInfo.get(dataSetIdOrPredictId));
						}
						writeJson(jsonMap, item.getPic_image_field(), zos, entry.getKey());
						try(InputStream intpuStream = minioFileService.getImageInputStream(item.getPic_image_field())){
							if(intpuStream == null) {
								logger.info("the imagepath stream is null. imgpath=" + item.getPic_image_field());
								continue;
							}
							String name = item.getPic_image_field().substring(item.getPic_image_field().lastIndexOf("/") + 1);
							zos.putNextEntry(new ZipEntry(entry.getKey() + "/" + name));
							byte buffer[] = new byte[2048];
							while(true) {
								int length = intpuStream.read(buffer);
								if(length < 0) {
									break;
								}
								zos.write(buffer, 0, length);
							}
							zos.closeEntry();
						}
					}
				}
				count ++;
				if(count != total) {
					//pro.setProgress((long)((count *1.0 / total) * 100));
					updateProgress(pro, (long)((count *1.0 / total) * 100));
				}
			}

			long end = System.currentTimeMillis();
			logger.info("finished zip, cost: " + (end - start) +" ms");
		} catch (Exception e) {
			throw new RuntimeException("zip error from ZipUtils",e);
		}
		//pro.setProgress(100l);
		updateProgress(pro, 100);
	}

	private void writerReIDCutImage(ReIDTask reIdtask, Progress pro, File zipFile,
			Map<String, Object> typeOrColorMapName) {
		int total = 0;
		int count = 0;
		List<ReIDTaskShowResult> showList = reIDTaskShowResultDao.queryReIDShowTaskResultById(reIdtask.getId());
		StringBuilder strBuilder = new StringBuilder();
		long start = System.currentTimeMillis();

		List<LabelTaskItem> reList = reIdLabelTaskItemDao.queryLabelTaskItemByLabelTaskId(TokenManager.getUserTablePos(reIdtask.getUser_id(), UserConstants.REID_TASK_SINGLE_TABLE),reIdtask.getId());

		total = showList.size() + reList.size();
		logger.info("writer cut image.reIdtask=" + reIdtask.getId());
		try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
			for(ReIDTaskShowResult item : showList) {
				strBuilder.append(item.getReid_name()).append("    ");

				String mapStr = item.getRelated_info();
				Map<String,String> map =  gson.fromJson(mapStr, new TypeToken<Map<String,String>>() {
					private static final long serialVersionUID = 1L;}.getType());

				List<String> imgList = new ArrayList<>();
				imgList.addAll(map.keySet());
				Collections.sort(imgList);
				for(int i = 0; i < imgList.size(); i++) {
					String imgPath = imgList.get(i);
					String entryFileName = imgPath.substring(imgPath.lastIndexOf("/") + 1);
					strBuilder.append(entryFileName);
					if(i < imgList.size() - 1) {
						strBuilder.append(" ");
					}

					try(InputStream intpuStream = minioFileService.getImageInputStream(imgPath)){
						if(intpuStream == null) {
							logger.info("the imagepath stream is null. imgpath=" + imgPath);
							continue;
						}
						zos.putNextEntry(new ZipEntry(item.getReid_name() + "/" + entryFileName));
						byte buffer[] = new byte[2048];
						while(true) {
							int length = intpuStream.read(buffer);
							if(length < 0) {
								break;
							}
							zos.write(buffer, 0, length);
						}

						zos.closeEntry();
					}
				}
				strBuilder.append("\n\r");
				count ++;
				if(count != total) {
					//pro.setProgress((long)((count *1.0 / total) * 100));
					updateProgress(pro,(long)((count *1.0 / total) * 100));
				}
			}
			zos.putNextEntry(new ZipEntry("reid_desc.csv"));
			byte[] bytes = strBuilder.toString().getBytes("utf-8");
			zos.write(bytes, 0, bytes.length);
			zos.closeEntry();

			Map<String,Map<String,Object>> datasetInfo = new HashMap<>();
			logger.info("writer json label.");
			for(int i = 0; i < reList.size(); i++) {
				LabelTaskItem item = reList.get(i);
				Map<String,Map<String,Object>> reIdMap = pclJsonUtil.getReIdJson(item,typeOrColorMapName);

				String dataSetIdOrPredictId = item.getPic_url();
				if(datasetInfo.get(dataSetIdOrPredictId) == null) {
					Map<String,Object> dataSetInfo = getDataSetInfo(dataSetIdOrPredictId,reIdtask);
					datasetInfo.put(dataSetIdOrPredictId, dataSetInfo);
				}

				if(reIdMap.size() > 0) {
					for(Entry<String,Map<String,Object>> entry : reIdMap.entrySet()) {
						Map<String,Object> jsonMap = entry.getValue();
						if(datasetInfo.get(dataSetIdOrPredictId) != null) {
							jsonMap.putAll(datasetInfo.get(dataSetIdOrPredictId));
						}
						writeJson(jsonMap, item.getPic_image_field(), zos, entry.getKey());

					}
				}
				count ++;
				if(count != total) {
					//pro.setProgress((long)((count *1.0 / total) * 100));
					updateProgress(pro, (long)((count *1.0 / total) * 100));
				}
			}

			long end = System.currentTimeMillis();
			logger.info("finished zip, cost: " + (end - start) +" ms");
		} catch (Exception e) {
			throw new RuntimeException("zip error from ZipUtils",e);
		}
		//pro.setProgress(100l);
		updateProgress(pro, 100);
	}

	private Map<String, Object> getDataSetInfo(String dataSetIdOrPredictId,LabelTask labelTask) {
		HashMap<String,Object> result = new HashMap<>();
		if(labelTask == null) {
			return result;
		}
		String dataSetId = dataSetIdOrPredictId;
		if(Constants.LABEL_TASK_TYPE_AUTO == labelTask.getTask_type()) {
			PrePredictTask task = predictTaskDao.queryPrePredictTaskById(dataSetIdOrPredictId);
			dataSetId = task.getDataset_id();
		}
		getDataSetMap(result, dataSetId);

		return result;
	}

	private void getDataSetMap(HashMap<String, Object> result, String dataSetId) {
		DataSet dataSet = dataSetDao.queryDataSetById(dataSetId);
		if(dataSet != null) {
			if(dataSet.getCamera_number() != null) {
				result.put("camera_id", dataSet.getCamera_number());
			}
			if(dataSet.getCamera_gps() != null) {
				result.put("camera_gps", dataSet.getCamera_gps());
			}
			if(dataSet.getDataset_type() == Constants.DATASET_TYPE_VIDEO) {
				result.put("video_name", dataSet.getZip_object_name());
			}
		}
	}


	private Map<String, Object> getDataSetInfo(String dataSetIdOrPredictId,ReIDTask reIdTask) {
		HashMap<String,Object> result = new HashMap<>();
		if(reIdTask == null) {
			return result;
		}
		String dataSetId = dataSetIdOrPredictId;
		if(Constants.REID_TASK_TYPE_AUTO == reIdTask.getTask_type()) {
			PrePredictTask task = predictTaskDao.queryPrePredictTaskById(dataSetIdOrPredictId);
			if(task != null) {
				dataSetId = task.getDataset_id();
			}else {
				return result;
			}
		}
		getDataSetMap(result, dataSetId);

		return result;
	}

	public String downloadVideoCountLabelFile(String labelTaskId) throws IOException {
		VideoCountTask task = videoCountTaskDao.queryVideoCountTask(labelTaskId);
		if(task == null) {
			logger.info("The task is not exists.labelTaskId=" + labelTaskId);
			return "";
		}
		String key = labelTaskId + VIDEO_COUNT_POSTFIX;
		String relatedName =  System.nanoTime() + File.separator + FileUtil.getRemoveChineseCharName(task.getTask_name()) + "_" + VIDEO_COUNT_POSTFIX + ".zip";
		String fileName = LabelDataSetMerge.getUserDataSetPath() + File.separator + relatedName;
		Progress pro = new Progress();
		pro.setId(key);
		pro.setStartTime(System.currentTimeMillis() / 1000);
		pro.setExceedTime(10 * 60);
		pro.setRelatedFileName(relatedName);
		putProgress(pro);

		ThreadSchedule.execExportThread(()->{
			try {
				downloadVideoCountLabelFileWriter(task, fileName, pro);
			} catch (Exception e) {
				pro.setId(e.getMessage());
				logger.info(e.getMessage(),e);
			}
		});

		return key;
	}


	public void downloadVideoCountLabelFileWriter(VideoCountTask task,String fileName,Progress pro) throws IOException {

		File zipFile = new File(fileName) ;
		zipFile.getParentFile().mkdirs();
		//pro.setProgress(10);
		updateProgress(pro.getId(), 10);
		String jsonFileName = task.getZip_object_name();
		jsonFileName = jsonFileName.substring(jsonFileName.lastIndexOf("/") +1);
		int dotIndex = jsonFileName.lastIndexOf(".");
		jsonFileName = jsonFileName.substring(0,dotIndex);

		StringBuilder strB = new StringBuilder();
		List<LabelTaskItem> labelTaskItemList  = videoCountTaskItemDao.queryLabelTaskItemByLabelTaskId(task.getId());
		for(LabelTaskItem item : labelTaskItemList) {
			String labelJson = item.getLabel_info();
			strB.append(labelJson);
			strB.append("\r\n");
		}

		long start = System.currentTimeMillis();
		byte[] bytes = strB.toString().getBytes("utf-8");
		//pro.setProgress(50);
		updateProgress(pro.getId(), 50);
		try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
			zos.putNextEntry(new ZipEntry(jsonFileName + ".json"));
			int len = bytes.length;
			zos.write(bytes, 0, len);
			zos.closeEntry();
			long end = System.currentTimeMillis();
			logger.info("finished zip, cost: " + (end - start) +" ms");
		} catch (Exception e) {
			throw new RuntimeException("zip error from ZipUtils",e);
		}
		//pro.setProgress(100l);
		updateProgress(pro.getId(), 100);
	}

	public String downloadVideoLabelFile(String labelTaskId,boolean isNeedPicture)  {
		VideoLabelTask task = videoLabelTaskDao.queryVideoLabelTask(labelTaskId);
		String key = labelTaskId + VIDEO_POSTFIX;
		String relatedName =  System.nanoTime() + File.separator + FileUtil.getRemoveChineseCharName(task.getTask_name()) + "_" + VIDEO_POSTFIX + ".zip";
		String fileName = LabelDataSetMerge.getUserDataSetPath() + File.separator + relatedName;
		Progress pro = new Progress();
		pro.setId(key);
		pro.setStartTime(System.currentTimeMillis() / 1000);
		pro.setExceedTime(10 * 60);
		pro.setRelatedFileName(relatedName);
		putProgress(pro);

		ThreadSchedule.execExportThread(()->{
			try {
				downloadVideoLabelFileWriter(task,fileName,isNeedPicture, pro);
			} catch (Exception e) {
				pro.setId(e.getMessage());
				logger.info(e.getMessage(),e);
			}
		});

		return key;

	}

	public void downloadVideoLabelFileWriter(VideoLabelTask task,String fileName,boolean isNeedPicture,Progress pro) throws IOException {


		Map<String,Object> typeKeyValue = JsonUtil.getMap(task.getTask_label_type_info());

		Map<String,Object> typeOrColorMapName = getTypeOrColorName(typeKeyValue);

		String videoName = task.getZip_object_name();
		videoName = videoName.substring(videoName.lastIndexOf("/") + 1);

		List<LabelTaskItem> reList =videoLabelTaskItemDao.queryLabelTaskItemByLabelTaskId(task.getId());

		File zipFile = new File(fileName) ;
		zipFile.getParentFile().mkdirs();
		int total = reList.size();
		int count = 0;

		long start = System.currentTimeMillis();
		try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
			//写json格式
			for(LabelTaskItem item : reList) {
				if(item.getPic_image_field() == null) {
					continue;
				}
				Map<String,Object> jsonMap = pclJsonUtil.getJson(item,typeOrColorMapName);
				jsonMap.put("video_name", videoName);
				if(item.getPic_url() != null) {
					jsonMap.put("timestamp", item.getPic_url().replace(":", "-").replace(".", "-"));
				}
				String relativeFileName = item.getPic_image_field();

				writeJson(jsonMap,relativeFileName,zos,"json");

				if(isNeedPicture) {
					try(InputStream intpuStream = minioFileService.getImageInputStream(item.getPic_image_field())){
						if(intpuStream == null) {
							logger.info("the imagepath stream is null. imgpath=" + item.getPic_image_field());
							continue;
						}
						String name = item.getPic_image_field().substring(item.getPic_image_field().lastIndexOf("/") + 1);
						zos.putNextEntry(new ZipEntry("img/" + name));
						byte buffer[] = new byte[2048];
						while(true) {
							int length = intpuStream.read(buffer);
							if(length < 0) {
								break;
							}
							zos.write(buffer, 0, length);
						}
						zos.closeEntry();
					}
				}
				count ++;
				if(count != total) {
					//pro.setProgress((long)((count *1.0 / total) * 100));
					updateProgress(pro.getId(), (long)((count *1.0 / total) * 100));
				}
			}
			long end = System.currentTimeMillis();
			logger.info("finished zip, cost: " + (end - start) +" ms");
		} catch (Exception e) {
			throw new RuntimeException("zip error from ZipUtils",e);
		}
		//pro.setProgress(100l);
		updateProgress(pro.getId(), 100);
	}


	public String downDataSetFile(String dataSetId) throws IOException {

		DataSet dataSet = dataSetDao.queryDataSetById(dataSetId);

		String key = dataSet.getId() + DATASET_POSTFIX;

		String relatedName =  System.nanoTime() + File.separator + FileUtil.getRemoveChineseCharName(dataSet.getTask_name()) + "_dataSet" + ".zip";

		String fileName = LabelDataSetMerge.getUserDataSetPath() + File.separator + relatedName;
		Progress pro = new Progress();
		pro.setId(key);
		pro.setStartTime(System.currentTimeMillis() / 1000);
		pro.setExceedTime(10 * 60);
		pro.setRelatedFileName(relatedName);
		putProgress(pro);


		ThreadSchedule.execExportThread(()->{
			try {
				downDataSetFileWriter(dataSetId, fileName, pro);
			} catch (Exception e) {
				pro.setId(e.getMessage());
				logger.info(e.getMessage(),e);
			}
		});

		return key;
	}

	public void downDataSetFileWriter(String dataSetId,String fileName,Progress pro) throws IOException {

		DataSet dataSet = dataSetDao.queryDataSetById(dataSetId);

		long start = System.currentTimeMillis();
		File zipFile = new File(fileName) ;
		zipFile.getParentFile().mkdirs();
		//int total = itemList.size();
		int count = 0;

		Map<String,Object> tmpParam = new HashMap<>();
		tmpParam.put("label_task_id", dataSetId);
		tmpParam.put("user_id", TokenManager.getUserTablePos(dataSet.getUser_id(), UserConstants.LABEL_TASK_SINGLE_TABLE));

		int total =labelTaskItemDao.queryLabelTaskItemPageCountByLabelTaskId(tmpParam);

		try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
			int pageSize = 1000;
			for(int i = 0; i < (count/pageSize) +1; i++) {
				tmpParam.put("currPage", i * pageSize);
				tmpParam.put("pageSize", pageSize);
				List<LabelTaskItem> itemList =labelTaskItemDao.queryLabelTaskItemPageByLabelTaskId(tmpParam);
				//写json格式
				count = writerItemListToZip(pro, itemList, total, count, zos);
			}
			long end = System.currentTimeMillis();
			logger.info("finished zip, cost: " + (end - start) +" ms");
		} catch (Exception e) {
			throw new RuntimeException("zip error from ZipUtils",e);
		}
		//pro.setProgress(100l);
		updateProgress(pro.getId(), 100);
	}

	private int writerItemListToZip(Progress pro, List<LabelTaskItem> itemList, int total, int count, ZipOutputStream zos)
			throws IOException {
		for(LabelTaskItem item : itemList) {
			String relativeFileName = item.getPic_image_field();
			String entryFileName = relativeFileName.substring(relativeFileName.lastIndexOf("/") +1);
			try(InputStream intpuStream = minioFileService.getImageInputStream(relativeFileName)){
				if(intpuStream == null) {
					logger.info("the imagepath stream is null. imgpath=" + item.getPic_image_field());
					continue;
				}
				zos.putNextEntry(new ZipEntry(entryFileName));
				byte buffer[] = new byte[2048];
				while(true) {
					int length = intpuStream.read(buffer);
					if(length < 0) {
						break;
					}
					zos.write(buffer, 0, length);
				}
				zos.closeEntry();
			}
			count ++;
			if(count != total) {
				//pro.setProgress((long)((count *1.0 / total) * 100));
				updateProgress(pro.getId(), (long)((count *1.0 / total) * 100));
			}
		}
		return count;
	}

	public Progress queryProgress(String taskId) {

		return progressDao.queryProgressById(taskId);
	}

	public void downFile(HttpServletResponse response, String taskId) throws IOException {
		logger.info("start to down file, taskId=" + taskId);
		Progress pro = queryProgress(taskId);
		if(pro == null) {
			logger.info("The task is null. taskId=" + taskId);
			return;
		}

		if(pro.getRelatedFileName().startsWith("[")) {
			List<String> fileList = JsonUtil.getList(pro.getRelatedFileName());
			response.setContentType("application/force-download");// 设置强制下载不打开
			response.setHeader("Content-Disposition", "attachment;fileName="+ new String("multi_zip.zip".getBytes("GB2312"),"ISO-8859-1"));  
			long start = System.currentTimeMillis();
			try(ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
				HashMap<String,String> tmpFileNameMap = new HashMap<>();
				int count = 0;
				for(String filename : fileList) {
					count++;
					String absolutePath = LabelDataSetMerge.getUserDataSetPath() + File.separator + filename;
					File downFile = new File(absolutePath);
					if(!downFile.exists()) {
						logger.info("down load file not exist. file=" + absolutePath);
					}else {
						logger.info("start to down load file. path=" + absolutePath);
					}
					String entryName = downFile.getName();
					if(tmpFileNameMap.get(entryName) != null) {
						entryName = count + "_" + entryName;
					}
					tmpFileNameMap.put(entryName, entryName);
					try(InputStream intpuStream = new FileInputStream(downFile)){

						zos.putNextEntry(new ZipEntry(entryName));
						byte buffer[] = new byte[2048];
						while(true) {
							int length = intpuStream.read(buffer);
							if(length < 0) {
								break;
							}
							zos.write(buffer, 0, length);
						}
						zos.closeEntry();
					} catch (Exception e) {
						throw new RuntimeException("zip error from ZipUtils",e);
					}finally {
						downFile.delete();
					}
					long end = System.currentTimeMillis();
					logger.info("finished zip, cost: " + (end - start) +" ms");
				}
			}catch (Exception e) {
				logger.info("error",e);
			}

		}else {
			//单个文件
			String absolutePath = LabelDataSetMerge.getUserDataSetPath() + File.separator + pro.getRelatedFileName();
			File downFile = new File(absolutePath);
			if(!downFile.exists()) {
				logger.info("down load file not exist. file=" + absolutePath);
			}else {
				logger.info("start to down load file. path=" + absolutePath);
			}

			response.setContentType("application/force-download");// 设置强制下载不打开
			response.setHeader("Content-Disposition", "attachment;fileName="+ new String(downFile.getName().getBytes("GB2312"),"ISO-8859-1"));  

			long start = System.currentTimeMillis();
			try(OutputStream output = response.getOutputStream()) {
				//写json格式
				try(InputStream intpuStream = new FileInputStream(downFile)){
					byte buffer[] = new byte[2048];
					while(true) {
						int length = intpuStream.read(buffer);
						if(length < 0) {
							break;
						}
						output.write(buffer, 0, length);
					}
					output.flush();
				}
				long end = System.currentTimeMillis();
				logger.info("finished zip, cost: " + (end - start) +" ms");
			} catch (Exception e) {
				throw new RuntimeException("zip error from ZipUtils",e);
			}finally {
				downFile.delete();
			}

		}


	}

	public void downVideoFile(HttpServletResponse response, String minio_path) throws IOException {

		logger.info("minio_path=" + minio_path);

		String fileName = minio_path;
		if(minio_path.startsWith("/minio/") || minio_path.startsWith("/dcm/")) {
			String tmp[] = minio_path.split("/");
			int length = tmp.length;
			fileName =  tmp[length-1];
		}

		response.setContentType("application/force-download");// 设置强制下载不打开
		response.setHeader("Content-Disposition", "attachment;fileName="+ new String(fileName.getBytes("GB2312"),"ISO-8859-1"));  

		long start = System.currentTimeMillis();
		try(OutputStream output = response.getOutputStream()) {
			//写json格式
			try(InputStream intpuStream = minioFileService.getImageInputStream(minio_path)){
				byte buffer[] = new byte[2048];
				while(true) {
					int length = intpuStream.read(buffer);
					if(length < 0) {
						break;
					}
					output.write(buffer, 0, length);
				}
				output.flush();
			}
			long end = System.currentTimeMillis();
			logger.info("finished zip, cost: " + (end - start) +" ms");
		} catch (Exception e) {
			throw new RuntimeException("zip error from ZipUtils",e);
		}
	}

	public String downLargePictureLabelFile(String labelTaskId, boolean isNeedPicture) {
		LargePictureTask task = largePictureTaskDao.queryLargePictureTask(labelTaskId);
		String key = labelTaskId + LARGEPICTURE_POSTFIX;
		String relatedName =  System.nanoTime() + File.separator + FileUtil.getRemoveChineseCharName(task.getTask_name()) + "_" + LARGEPICTURE_POSTFIX + ".zip";
		String fileName = LabelDataSetMerge.getUserDataSetPath() + File.separator + relatedName;
		Progress pro = new Progress();
		pro.setId(key);
		pro.setStartTime(System.currentTimeMillis() / 1000);
		pro.setExceedTime(10 * 60);
		pro.setRelatedFileName(relatedName);
		putProgress(pro);

		ThreadSchedule.execExportThread(()->{
			try {
				downloadLargePictureLabelFileWriter(task,fileName,isNeedPicture, pro);
			} catch (Exception e) {
				pro.setId(e.getMessage());
				logger.info(e.getMessage(),e);
			}
		});

		return key;
	}

	public void downloadLargePictureLabelFileWriter(LargePictureTask task,String fileName,boolean isNeedPicture,Progress pro) throws IOException {


		//Map<String,Object> typeKeyValue = JsonUtil.getMap(task.getTask_label_type_info());

		//Map<String,Object> typeOrColorMapName = getTypeOrColorName(typeKeyValue);

		//String videoName = task.getZip_object_name();
		//videoName = videoName.substring(videoName.lastIndexOf("/") + 1);

		String pictureName = task.getZip_object_name();

		Map<String,Object> imageInfo = JsonUtil.getMap(task.getMainVideoInfo());

		Object pictureWidthObj = imageInfo.get("width");
		Object pictureHeightObj = imageInfo.get("height");

		int width = (int)Double.parseDouble(pictureWidthObj.toString());
		int heigth = (int)Double.parseDouble(pictureHeightObj.toString());

		List<LabelTaskItem> reList =largePictureTaskItemDao.queryLabelTaskItemByLabelTaskId(task.getId());

		File zipFile = new File(fileName) ;
		zipFile.getParentFile().mkdirs();
		int total = 1;
		int count = 0;

		long start = System.currentTimeMillis();
		try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
			//写json格式
			for(LabelTaskItem item : reList) {
				//				if(item.getPic_image_field() == null) {
				//					continue;
				//				}
				item.setPic_image_field(pictureName);
				item.setPic_object_name(width + "," + heigth);
				Map<String,Object> jsonMap = pclJsonUtil.getJson(item,new HashMap<>());
				//jsonMap.put("video_name", videoName);
				//				if(item.getPic_url() != null) {
				//					jsonMap.put("timestamp", item.getPic_url().replace(":", "-").replace(".", "-"));
				//				}


				writeJson(jsonMap,pictureName,zos,"json");

				//				if(isNeedPicture) {
				//					try(InputStream intpuStream = minioFileService.getImageInputStream(item.getPic_image_field())){
				//						String name = item.getPic_image_field().substring(item.getPic_image_field().lastIndexOf("/") + 1);
				//						zos.putNextEntry(new ZipEntry("img/" + name));
				//						byte buffer[] = new byte[2048];
				//						while(true) {
				//							int length = intpuStream.read(buffer);
				//							if(length < 0) {
				//								break;
				//							}
				//							zos.write(buffer, 0, length);
				//						}
				//						zos.closeEntry();
				//					}
				//				}
				count ++;
				if(count != total) {
					//pro.setProgress((long)((count *1.0 / total) * 100));
					updateProgress(pro.getId(), (long)((count *1.0 / total) * 100));
				}
			}
			long end = System.currentTimeMillis();
			logger.info("finished zip, cost: " + (end - start) +" ms");
		} catch (Exception e) {
			throw new RuntimeException("zip error from ZipUtils",e);
		}
		//pro.setProgress(100l);
		updateProgress(pro.getId(), 100);
	}
	
	
	public String downloadReIdTaskListFile(String reIdTaskIdList, String type) throws IOException {
		List<String> reidList = JsonUtil.getList(reIdTaskIdList);
		return downloadReIdTaskListFile(reidList, type, LabelDataSetMerge.getUserDataSetPath(),true);
	}
	
	
	private String downloadReIdTaskListFile(List<String> reidList, String type,String tmpPath,boolean isNeedNanoTime) throws IOException {
		
		logger.info("reidList size=" + reidList.size());
		String key = UUID.randomUUID().toString().replaceAll("-","") + REID_POSTFIX;

		List<String> fileNameList = new ArrayList<>();

		Progress pro = new Progress();
		pro.setId(key);
		pro.setStartTime(System.currentTimeMillis() / 1000);
		pro.setExceedTime(10 * 60);

		pro.setRatio(1.0/reidList.size());


		for(String reIdTaskId : reidList) {
			logger.info("query reIdTaskId=" + reIdTaskId);
			String prefix = "";
			if(isNeedNanoTime) {
				prefix = System.nanoTime() + File.separator;
			}
			ReIDTask reIdtask = reIdTaskDao.queryReIDTaskById(reIdTaskId);
			String taskName = FileUtil.getRemoveChineseCharName(reIdtask.getTask_name());
			String relatedName =  prefix + taskName + "_ReID" + ".zip";
			if(type.equals(Constants.REID_EXPORT_TYPE_REID_PICTURE)) {
				relatedName =  prefix + taskName + "_ReID_CutImage" + ".zip";
			}else if(type.equals(Constants.REID_EXPORT_TYPE_REID_PICTURE_RENAME)) {
				relatedName =  prefix + taskName + "_ReID_CutImage_DS" + ".zip";
			}
			fileNameList.add(relatedName);
		}
		String fileJsonStr = JsonUtil.toJson(fileNameList);
		if(fileJsonStr.length() > 10000) {
			fileJsonStr = "";
		}
		pro.setRelatedFileName(fileJsonStr);
		putProgress(pro);

		ThreadSchedule.execExportThread(()->{
			try {
				double base = 0.0;
				for(int i = 0; i <reidList.size(); i++) {
					logger.info("start deal " + (i + 1) + " task.");
					String reIdTaskId = reidList.get(i);
					ReIDTask reIdtask = reIdTaskDao.queryReIDTaskById(reIdTaskId);

					String relatedName = fileNameList.get(i);

					String fileName = tmpPath + File.separator + relatedName;

					downloadReIdTaskFileWriter(reIdtask,type, fileName, pro);

					base+= pro.getRatio();

					pro.setBase((long)(base * 100));

				}
			} catch (Exception e) {
				pro.setId(e.getMessage());
				logger.info(e.getMessage(),e);
			}

			updateProgress(pro.getId(),100);
		});
		return key;

	}


	
	public String multiReIdAllDownFile(String token, String type) throws IOException {
		
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("user_id", userId);
		List<ReIDTask> labelTaskList = reIdTaskDao.queryReIDTaskByUser(paramMap);
		
		List<String> reIdList = new ArrayList<>();
		for(ReIDTask task : labelTaskList) {
			
			reIdList.add(task.getId());
		}
		String tmpPath = LabelDataSetMerge.getAllDownLoadFilePath();
		
		return downloadReIdTaskListFile(reIdList, type, tmpPath,false);
	}



}
