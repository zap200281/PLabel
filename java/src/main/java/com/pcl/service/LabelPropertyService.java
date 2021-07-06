package com.pcl.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pcl.dao.LabelTaskDao;
import com.pcl.dao.ReIDTaskDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.Result;
import com.pcl.pojo.mybatis.LabelTask;
import com.pcl.pojo.mybatis.ReIDTask;
import com.pcl.util.JsonUtil;

//标注属性导入导出服务
@Service
public class LabelPropertyService {

	@Autowired
	private LabelTaskDao labelTaskDao;

	@Autowired
	private ReIDTaskDao reIdTaskDao;
	
	private static Logger logger = LoggerFactory.getLogger(LabelExportService.class);
	
	public void queryLabelProperty(String token, String taskId,String type,HttpServletResponse response) {
		logger.info("start to writer label property json.");
		response.setContentType("application/force-download");// 设置强制下载不打开
		response.setHeader("Content-Disposition", "attachment;fileName="+ taskId + ".json");  
		String json = "";
		try(OutputStream fos = response.getOutputStream()) {
			if(type.equals("reid")) {
				ReIDTask task = reIdTaskDao.queryReIDTaskById(taskId);
				if(task != null) {
					json = task.getTask_label_type_info();
				}
			}else if(type.equals("labeltask")) {
				LabelTask task = labelTaskDao.queryLabelTaskById(taskId);
				if(task != null) {
					json = task.getTask_label_type_info();
				}
			}
			logger.info("json=" + json);
			fos.write(json.getBytes("utf-8"));
			fos.flush();
		} catch (IOException e) {

			e.printStackTrace();
		}
		logger.info("end to writer label property json.");
	}

	public Result importLabelPropertyJson(String token, String jsonContent, String taskType, String taskId) {
	
		try {
			checkLabelJson(jsonContent);
		} catch (LabelSystemException e) {
			Result re = new Result();
			re.setCode(1);
			re.setMessage(e.getMessage());
			return re;
		}
	
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", taskId);
		paramMap.put("task_label_type_info", jsonContent);
		if(taskType.equals("reid")) {
			reIdTaskDao.updateReIDTaskSelfDefineInfo(paramMap);
		}else if(taskType.equals("labeltask")) {
			labelTaskDao.updateLabelTask(paramMap);
		}
		Result re = new Result();
		re.setCode(0);
		return re;
	}
	
	public static void checkLabelJson(String jsonContent) throws LabelSystemException{
		Map<String,Object> jsonMap = JsonUtil.getMap(jsonContent);
		
		if(jsonMap == null) {
			throw new LabelSystemException("导入的Json非法。");
		}
		
		if(jsonMap.get("id") == null) {
			throw new LabelSystemException("导入的标注Json中必需包括id及type属性。");
		}
		
		for(Entry<String,Object> entry :jsonMap.entrySet()) {
			Object valueObj = entry.getValue();
			if(!(valueObj instanceof Map)) {
				throw new LabelSystemException("导入的标注Json中属性格式错误。");
			}
			
			Map<String,Object> valueMap = (Map<String,Object>)valueObj;
			if(valueMap.get("type") == null) {
				throw new LabelSystemException("导入的标注Json中属性没有标注类别。");
			}
			String typeValue = valueMap.get("type").toString();
			if(!Arrays.asList("dropdown","text","checkbox","radio").contains(typeValue)) {
				throw new LabelSystemException("导入的标注Json中属性标注类别应该为：dropdown,text,checkbox,radio四种之一。");
			}
		}
		
	}


}
