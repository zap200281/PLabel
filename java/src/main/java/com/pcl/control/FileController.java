package com.pcl.control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.pcl.pojo.FileResult;
import com.pcl.pojo.Progress;
import com.pcl.pojo.Result;
import com.pcl.service.LabelExportService;
import com.pcl.service.ObjectFileService;
import com.pcl.service.PCLSpecialExportService;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api")
public class FileController {

	private static Logger logger = LoggerFactory.getLogger(FileController.class);

	@Autowired
	private ObjectFileService fileService;

	@Autowired
	private LabelExportService labelExportService;

	@Autowired
	private HttpServletResponse response; 
	
	@Autowired
	HttpServletRequest request;
	
	@Autowired
	private PCLSpecialExportService pclSpecialExportService;

	@ResponseBody
	@ApiOperation(value="上传多个文件接口", notes="返回文件接口")
	@RequestMapping(value ="/common-files-upload", method = RequestMethod.POST)
	public List<FileResult> uploadFile(@RequestParam("files") MultipartFile[] files,@RequestParam("datasettype") String datasettype) {
		logger.info("upload files length=:" +  files.length);
		List<FileResult> re = new ArrayList<>();
		for(MultipartFile file : files) {
			re.addAll(fileService.uploadFile(file,datasettype));
		}
		return re;
	}
	
	
	

	@ResponseBody
	@ApiOperation(value="上传文件接口", notes="返回文件接口")
	@RequestMapping(value ="/common-file-upload-new", method = RequestMethod.POST)
	public List<FileResult> uploadFileNew(@RequestParam("files") MultipartFile files,@RequestParam("datasettype") String datasettype) {
		logger.info("upload file new :" +  files.getOriginalFilename());
		return fileService.uploadFile(files,datasettype);
	}

	//type=1,不带图片，2带图片，3
	@ApiOperation(value="导出标注数据文件接口", notes="返回导出查询进度信息的id")
	@RequestMapping(value ="/label-task-export", method = RequestMethod.GET)
	public Result downFile(@RequestParam("label_task_id") String labelTaskId,@RequestParam("needPicture") int type,@RequestParam(value="maxscore",required=false, defaultValue="1.1")  double maxscore,@RequestParam(value="minscore",required=false, defaultValue="0.0")  double minscore) {
		Result result = new Result();
		try {
			logger.info("export file label_task_id= :" +  labelTaskId + "  needPicture=" + type);
			result.setCode(0);
			result.setMessage(labelExportService.downloadLabelTaskFile(labelTaskId,type,maxscore,minscore));
		} catch (IOException e) {
			e.printStackTrace();
			result.setCode(1);
			result.setMessage(e.getMessage());
		}
		return result;
	}


	@ApiOperation(value="查询下载进度接口", notes="查询下载进度接口")
	@RequestMapping(value ="/query-download-progress", method = RequestMethod.GET)
	public Progress queryProgress(@RequestParam("taskId") String taskId) {

		return labelExportService.queryProgress(taskId);

	}

	@ResponseBody
	@ApiOperation(value="下载人工标注文件接口", notes="直接在response中返回文件流。")
	@RequestMapping(value ="/label-file-download", method = RequestMethod.GET)
	public void downFile(@RequestParam("taskId") String taskId) {
		try {
			labelExportService.downFile(response, taskId);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@ResponseBody
	@ApiOperation(value="下载文件接口", notes="返回文件接口")
	@RequestMapping(value ="/label-video-download", method = RequestMethod.GET)
	public void downVideoFile(@RequestParam("minio_path") String minio_path) {
		try {
			labelExportService.downVideoFile(response, minio_path);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@ResponseBody
	@ApiOperation(value="下载文件接口", notes="返回文件接口")
	@RequestMapping(value ="/dataset-picture-download", method = RequestMethod.GET)
	public Result exportDataSetFile(@RequestParam("data_set_id") String dataSetId) {

		Result result = new Result();
		try {
			result.setCode(0);
			result.setMessage(labelExportService.downDataSetFile(dataSetId));
		} catch (IOException e) {
			e.printStackTrace();
			result.setCode(1);
			result.setMessage(e.getMessage());
		}
		return result;
	}



	@ResponseBody
	@ApiOperation(value="下载视频流统计文件接口", notes="返回文件接口")
	@RequestMapping(value ="/video-label-file-download", method = RequestMethod.GET)
	public Result downVideoCountLabelFile(@RequestParam("label_task_id") String labelTaskId) {

		Result result = new Result();
		try {
			result.setCode(0);
			result.setMessage(labelExportService.downloadVideoCountLabelFile(labelTaskId));
		} catch (IOException e) {
			e.printStackTrace();
			result.setCode(1);
			result.setMessage(e.getMessage());
		}
		return result;

	}


	@ResponseBody
	@ApiOperation(value="下载视频标注文件接口", notes="返回文件接口")
	@RequestMapping(value ="/video-label-download", method = RequestMethod.GET)
	public Result downVideoLabelFile(@RequestParam("label_task_id") String labelTaskId,@RequestParam("needPicture") int needPicture) {

		Result result = new Result();

		boolean isNeedPicture = false;
		if(needPicture == 2) {
			isNeedPicture = true;
		}
		result.setCode(0);
		result.setMessage(labelExportService.downloadVideoLabelFile(labelTaskId,isNeedPicture));

		return result;
	}

	@ResponseBody
	@ApiOperation(value="下载超大图标注信息接口", notes="返回文件接口")
	@RequestMapping(value ="/large-picture-label-download", method = RequestMethod.GET)
	public Result downLargePictureLabelFile(@RequestParam("label_task_id") String labelTaskId,@RequestParam("needPicture") int needPicture) {

		Result result = new Result();

		boolean isNeedPicture = false;
		if(needPicture == 2) {
			isNeedPicture = true;
		}
		result.setCode(0);
		result.setMessage(labelExportService.downLargePictureLabelFile(labelTaskId,isNeedPicture));

		return result;

	}


	@ResponseBody
	@ApiOperation(value="下载文件接口", notes="返回文件接口")
	@RequestMapping(value ="/reid-data-download", method = RequestMethod.GET)
	public Result reIdDownFile(@RequestParam("reid_task_id") String reIdTaskId,@RequestParam("needPicture") String type) {

		Result result = new Result();
		try {
			result.setCode(0);
			if(type.equals("5")) {
				result.setMessage(pclSpecialExportService.exportExceptionFile(reIdTaskId, 5, response));
			}else {
				result.setMessage(labelExportService.downloadReIdTaskFile(reIdTaskId, type));
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			result.setCode(1);
			result.setMessage(e.getMessage());
		}
		return result;

	}
	
	@ResponseBody
	@ApiOperation(value="下载文件接口", notes="返回文件接口")
	@RequestMapping(value ="/multi-reid-data-download", method = RequestMethod.GET)
	public Result multiReIdDownFile(@RequestParam("reid_task_id_list") String reIdTaskIdList,@RequestParam("needPicture") String type) {
		logger.info("reIdTaskIdList=" +reIdTaskIdList);
		Result result = new Result();
		try {
			result.setCode(0);
			result.setMessage(labelExportService.downloadReIdTaskListFile(reIdTaskIdList, type));
		} catch (IOException e) {
			e.printStackTrace();
			result.setCode(1);
			result.setMessage(e.getMessage());
		}
		return result;

	}
	
	
	@ResponseBody
	@ApiOperation(value="导出所有ReID标注数据接口", notes="导出所有ReID标注数据接口")
	@RequestMapping(value ="/multi-all-data-export", method = RequestMethod.GET)
	public Result multiReIdAllDownFile(@RequestParam("needPicture") String type) {
		logger.info("type=" +type);
		Result result = new Result();
		try {
			String token = request.getHeader("authorization");
			result.setCode(0);
			result.setMessage(labelExportService.multiReIdAllDownFile(token, type));
		} catch (IOException e) {
			e.printStackTrace();
			result.setCode(1);
			result.setMessage(e.getMessage());
		}
		return result;

	}
	

	


}
