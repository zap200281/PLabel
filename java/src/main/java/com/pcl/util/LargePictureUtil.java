package com.pcl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class LargePictureUtil {

	private static Logger logger = LoggerFactory.getLogger(LargePictureUtil.class);

	public static void zoomSvsFile(String svsFilePath,String taskId,String msgresttype, String msgrestIp, String port,String dziport) {


		logger.info("start to zoom file=" + svsFilePath);

		String url = "http://127.0.0.1:" + dziport + "/image2dzi";
		//使用Restemplate来发送HTTP请求
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		MediaType type = MediaType.parseMediaType("multipart/form-data");
		headers.setContentType(type);


		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
		form.add("taskId", taskId);
		form.add("picturepath",svsFilePath);
		form.add("rest",msgresttype + "://" + msgrestIp +":" + port + "/api/svsmessage");

		
		HttpEntity<MultiValueMap<String, Object>> files = new HttpEntity<>(form, headers);


		String jsonResult = restTemplate.postForObject(url, files, String.class);
		logger.info("result=" + jsonResult);

	}
	
	
	public static String getMsg(String dziport){
		String url = "http://127.0.0.1:" + dziport;
		//使用Restemplate来发送HTTP请求
		RestTemplate restTemplate = new RestTemplate();

		String jsonResult = restTemplate.getForObject(url, String.class);
		logger.info("result=" + jsonResult);
		return jsonResult;
	}
}
