package com.pcl.service.schedule;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pcl.constant.Constants;
import com.pcl.constant.UserConstants;
import com.pcl.dao.PrePredictTaskResultDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.FileResult;
import com.pcl.pojo.mybatis.DataSet;
import com.pcl.pojo.mybatis.PrePredictTask;
import com.pcl.pojo.mybatis.PrePredictTaskResult;
import com.pcl.service.LabelDataSetMerge;
import com.pcl.service.ObjectFileService;
import com.pcl.service.TokenManager;
import com.pcl.util.TimeUtil;
import com.pcl.util.VideoUtil;

public class PredictVideo  {

	private static Logger logger = LoggerFactory.getLogger(PredictVideo.class);
	
	
	public static void preDeal(DataSet dataSet, String imageDir,ObjectFileService fileService) throws LabelSystemException {
		logger.info("start to down load video.");
		String tmpPath = LabelDataSetMerge.getUserDataSetPath() + File.separator + "video_" +  System.nanoTime();
		new File(tmpPath).mkdirs();
		String existVideoPath = fileService.downLoadFileFromMinio(dataSet.getZip_bucket_name(), dataSet.getZip_object_name(), tmpPath);
		//抽帧
		logger.info("start to draw frame.");
		Map<String,String> param = new HashMap<>();
		param.put("fps", "4");
		param.put("drawFrameType", Constants.CHOUZHEN_PERSECOND_FRAME);
		param.put("filenamePrefix", "");
		VideoUtil.chouZhen(new File(existVideoPath), imageDir, param);
	}
	
	public static void dealResultJson(PrePredictTask prePredictTask, String outputDir, int length, File jsonResultFile,ObjectFileService fileService,PrePredictTaskResultDao prePredictTaskResultDao) {
		//视频标注
			//删除结果文件
			String picUrl = null;
			if(jsonResultFile.exists()) {
				//保存结果文件到minio中
				FileResult jsonResult = fileService.uploadVideoFile(jsonResultFile);
				picUrl = jsonResult.getPublic_url();
				jsonResultFile.delete();
			}
			String destVideoName = outputDir + prePredictTask.getId() + ".mp4";
			String tableNamePos = TokenManager.getUserTablePos(prePredictTask.getUser_id(), UserConstants.PREDICT_SINGLE_TABLE);
			//合并图片为mp4格式的视频。
			VideoUtil.mergePictureToVideo(outputDir + "_", destVideoName, length); 
			//上传结果视频到minio中
			if(new File(destVideoName).exists()) {
				logger.info("upload to minio: " + destVideoName);
				FileResult fileResult = fileService.uploadVideoFile(new File(destVideoName));
				
				//保存minio路径到数据库中
				PrePredictTaskResult result = new PrePredictTaskResult();
				result.setId(UUID.randomUUID().toString().replaceAll("-",""));
				result.setItem_add_time(TimeUtil.getCurrentTimeStr());
				result.setPre_predict_task_id(prePredictTask.getId());
				result.setPic_image_field("/minio" + fileResult.getPublic_url());
				result.setPic_url("/minio" + picUrl);
				result.setUser_id(tableNamePos);
				prePredictTaskResultDao.addPrePredictTaskResult(result);
			}
			//web界面需要能预览
	}

}
