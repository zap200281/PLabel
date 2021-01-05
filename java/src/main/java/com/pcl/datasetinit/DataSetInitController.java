package com.pcl.datasetinit;

import java.io.File;
import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pcl.constant.Constants;
import com.pcl.exception.LabelSystemException;
import com.pcl.util.FileUtil;

@RestController
@RequestMapping("/api")
public class DataSetInitController {

	
	@RequestMapping(value = "/datasetinit",method = RequestMethod.POST)
	public String login(@RequestParam("path") String path,@RequestParam("format") String format) throws LabelSystemException {

		File file = new File(path);
		
		if(!file.exists()) {
			return "输入的路径不存在。path=" + path;
		}
		
		if(!(format.equalsIgnoreCase("coco") || format.equalsIgnoreCase("voc"))) {
			return "支持格式为coco或者voc，其它不支持。";
		}
		
		
		if(format.equalsIgnoreCase("voc")) {
			String picturePath = path + File.separator + Constants.JPEGIMAGES;
			String annanotation = path + File.separator + Constants.ANNOTATIONS;
			
			List<File> allFileList = FileUtil.getAllFileList(annanotation);
			
			for(File annFile : allFileList) {
				
				
			}
		
		}
		
		return "sucess.";
	}
	
}
