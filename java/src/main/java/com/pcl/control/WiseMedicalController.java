package com.pcl.control;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.pcl.pojo.Result;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api")
public class WiseMedicalController {

	private static Logger logger = LoggerFactory.getLogger(WiseMedicalController.class);

	@Autowired
	HttpServletRequest request;

	
	@ApiOperation(value="创建一个视频标注任务", notes="创建一个视频标注任务")
	@RequestMapping(value="/auto-label-cell", method = RequestMethod.POST, produces ="application/json;charset=utf-8")
	public Result autoLabelCellTask(@RequestParam("id") String id) {
		
		String url = "http://nat.cloudbastion.cn:62981/blood_cell/detect";
        //使用Restemplate来发送HTTP请求
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("multipart/form-data");
        headers.setContentType(type);
        
        String filePath = "D:\\2019文档\\标注系统\\医疗";
        String fileName = "xueye2.jpg";
        
    
    	HttpHeaders requestHeaders = new HttpHeaders();
    	requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
         
        FileSystemResource fileSystemResource = new FileSystemResource(filePath+"/"+fileName);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("image", fileSystemResource);
        form.add("arg_type","binary");
        form.add("file_name",fileName);

        
        HttpEntity<MultiValueMap<String, Object>> files = new HttpEntity<>(form, headers);

        String s = restTemplate.postForObject(url, files, String.class);

        logger.info("result=" + s);
        
        Result re = new Result();
        re.setMessage(s);
        
		return re;
	}

	
}
