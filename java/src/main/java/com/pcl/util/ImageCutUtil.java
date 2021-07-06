package com.pcl.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.service.ObjectFileService;

public class ImageCutUtil {
	
	private static Logger logger = LoggerFactory.getLogger(ImageCutUtil.class);
	
	
	public static int cutImageToPath(LabelTaskItem item, String destCutImagePath,ObjectFileService fileService) {
		int count = 0;
		String jsonLabelInfo = item.getLabel_info();
		ArrayList<Map<String,Object>> labelList = JsonUtil.getLabelList(jsonLabelInfo);
		if(labelList.isEmpty()) {
			logger.info("jsonLabelInfo is empty. jsonLabelInfo=" + jsonLabelInfo);
			return count;
		}

		BufferedImage bufferImage = fileService.getBufferedImage(item.getPic_image_field());
		if(bufferImage == null) {
			logger.info("image is null. path=" + item.getPic_image_field());
			return count;
		}

		String imageName = item.getPic_image_field();
		imageName = imageName.substring(imageName.lastIndexOf("/") + 1);
		imageName = imageName.substring(0,imageName.length() - 4);
		for(Map<String,Object> label : labelList) {
			if(ImageCutUtil.cutImage(label, imageName, destCutImagePath, bufferImage) == 0) {
				count++;
			}
		}
		return count;

	}
	

	public static int cutImage(Map<String,Object> label,String imageName,String destCutImagePath,BufferedImage bufferImage) {

		Object idObj = label.get("id");
		if(idObj == null) {
			return -1;
		}
		String id = idObj.toString();
		List<Object> boxList = (List<Object>)label.get("box");
		if(boxList != null) {//矩形标注
			int xmin = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(0)));
			int ymin = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(1)));
			int xmax = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(2)));
			int ymax = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(3)));
			if(xmax-xmin <=0 || ymax - ymin <=0) {
				return -1;
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
			BufferedImage subImage = bufferImage.getSubimage(xmin, ymin, xmax-xmin, ymax-ymin);
			String name = imageName + "_" + id + ".jpg";
			try {
				//logger.info("cut img  name=" + name);
				ImageIO.write(subImage, "jpg", new File(destCutImagePath,name));
				return 0;
			} catch (Exception e) {
				e.printStackTrace();
				return -1;
			}
		}else {
			return -1;
		}
	}


}
