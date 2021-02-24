package com.pcl.control;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.pcl.pojo.Result;
import com.pcl.service.PCLSpecialExportService;

import io.swagger.annotations.ApiOperation;

//鹏城实验室相关需求特殊数据导出接口
@RestController
@RequestMapping("/api")
public class PCLSpecialExportController {

	private static Logger logger = LoggerFactory.getLogger(PCLSpecialExportController.class);

	@Autowired
	private HttpServletResponse response; 

	@Autowired
	private PCLSpecialExportService pclSpecialExportService;

	//type=1,不带图片，2带图片，3
	@ResponseBody
	@ApiOperation(value="异常事故检测事件级标注数据导出接口。", notes="异常事故检测事件级标注数据导出接口。")
	@RequestMapping(value ="/reid-task-exception-export", method = RequestMethod.GET)
	public Result exportExceptionFile(@RequestParam("label_task_id") String labelTaskId,@RequestParam("needPicture") int type) {
		Result result = new Result();
		try {
			logger.info("export file label_task_id= :" +  labelTaskId + "  needPicture=" + type);
			result.setCode(0);
			result.setMessage(pclSpecialExportService.exportExceptionFile(labelTaskId,type,response));
		} catch (IOException e) {
			e.printStackTrace();
			result.setCode(1);
			result.setMessage(e.getMessage());
		}
		return result;
	}


}
