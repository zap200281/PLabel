package com.pcl.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pcl.pojo.DcmObj;

import ij.plugin.DICOM;

public class DcmUtil {

	private static Logger logger = LoggerFactory.getLogger(DcmUtil.class);

	public static DcmObj getImageByDcmFile(InputStream inputstream) {
		try {
			DICOM dicom = new DICOM(inputstream);
			dicom.run("Name");
			DcmObj dcmObj = new DcmObj();
			dcmObj.setImage((BufferedImage) dicom.getImage());
			dcmObj.setDate(String.valueOf(dicom.getProperty("0008,0021  Series Date")));
			dcmObj.setDesc(String.valueOf(dicom.getProperty("0008,103E  Series Description")));
			dcmObj.setName(String.valueOf(dicom.getProperty("0010,0010  Patient's Name")));
			return dcmObj;
		} catch (Exception e) {
			logger.info("error get image from dcm.",e);
			return null;
		}
		
		  //0010,0010  Patient's Name: 白胜
		  //0008,0021  Series Date
		  //0008,103E  Series Description:
	}
	
	public static DcmObj getImageByDcmFile(String filePath) {
		try {
			DICOM dicom = new DICOM();
			logger.info("dcm path =" + filePath);
			dicom.run(filePath);
			
			
			DcmObj dcmObj = new DcmObj();
			dcmObj.setImage((BufferedImage) dicom.getImage());
			dcmObj.setDate(String.valueOf(dicom.getProperty("0008,0021  Series Date")));
			dcmObj.setDesc(String.valueOf(dicom.getProperty("0008,103E  Series Description")));
			dcmObj.setName(String.valueOf(dicom.getProperty("0010,0010  Patient's Name")));
			return dcmObj;
		} catch (Exception e) {
			logger.info("error get image from dcm.",e);
			return null;
		}
		
		  //0010,0010  Patient's Name: 白胜
		  //0008,0021  Series Date
		  //0008,103E  Series Description:
	}

	/**
	 * 输入一个dicom文件的绝对路径和名字
	 * 获取一个jpg文件
	 */
	private static void create2(String filePath) {
		try {
			DICOM dicom = new DICOM(new FileInputStream(filePath));
			dicom.run("Name");
			
			BufferedImage bi = (BufferedImage) dicom.getImage();
			int width = bi.getWidth();
			int height = dicom.getHeight();
			//System.out.println("width: " + width + "\n" + "height: " + height);
			//String imagePath = filePath.substring(0,filePath.lastIndexOf(".")) + ".jpg";
			//ImageIO.write(bi, "jpg", new File(imagePath));
			//System.out.println("Hehe,Game over!!!");
			
			String intercept = dicom.getStringProperty("0028,1052  Rescale Intercept");
			String slope = dicom.getStringProperty("0028,1053  Rescale Slope");
			int interceptInt = Integer.parseInt(intercept.trim());
			int slopeInt = Integer.parseInt(slope.trim());
			
//			int maxHu = 0;
//			int minHu = -100000;
//			
//			for(int i = 0;i < width; i++) {
//				
//				for(int j = 0;j < height; j++) {
//					
//					
//					int rgb = bi.getRGB(i, j);
//					
//					int r = (rgb >> 16) & 0xFF;
//					int g = (rgb >> 8) & 0xFF;
//					int b = (rgb ) & 0xFF;
//					
//					int gray = (r + g + b) / 3;
//					
//					int al = rgb >> 24;
//					
//					System.out.println("gray=" + gray + " al=" + al);
//					int hu = rgb * slopeInt + interceptInt;
//					if(hu > maxHu) {
//						maxHu = hu;
//					}
//					if(hu < minHu) {
//						minHu = hu;
//					}
//				}
//				
//			}
//			System.out.println("maxHu=" + maxHu);
//			System.out.println("minHu=" + minHu);
			
			//System.out.println(dicom.getStringProperty("0020,0032  Image Position (Patient)"));
			//System.out.println(dicom.getStringProperty("0020,0037  Image Orientation (Patient)"));
			//System.out.println(dicom.getStringProperty("0010,0010  Patient's Name") + dicom.getStringProperty("0008,0021  Series Date") + dicom.getStringProperty("0008,103E  Series Description"));
			Properties pro = dicom.getProperties();
//			for(Entry<Object,Object> entry : pro.entrySet()) {
//				Object value = entry.getValue();
//				System.out.println(entry.getKey() + "##" + entry.getValue());
//			}
//			
			System.out.println(dicom.getProperties());
		} catch (Exception e) {
			System.out.println("错误" + e.getMessage());
		}

	}
	
	public static void createRLSI(String filePath, int row) throws IOException {
		
		File files[] = new File(filePath).listFiles();
		List<String> filePathList = new ArrayList<>();
		for(File file : files) {
			if(file.getAbsolutePath().endsWith(".jpg")) {
				filePathList.add(file.getAbsolutePath());
			}			
		}
		Collections.sort(filePathList);
		
		int width = 512;
		int height = filePathList.size();
		
		BufferedImage newImage = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		
		for(int i = 0; i < filePathList.size(); i++) {
			try {
				BufferedImage srcBuff = ImageIO.read(new File(filePathList.get(i)));
				
				for(int j = 0; j < width; j++) {
					int rgb = srcBuff.getRGB(j, row);
					newImage.setRGB(j, i, rgb);
				}
			
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		ImageIO.write(newImage, "jpg", new File("D:\\dcm\\temp\\AI_SHENG_QUN_P0564472\\rlsi",row+".jpg"));

		
	}
	
	public static void createAPSI(String filePath, int col) throws IOException {
		File files[] = new File(filePath).listFiles();
		List<String> filePathList = new ArrayList<>();
		for(File file : files) {
			if(file.getAbsolutePath().endsWith(".jpg")) {
				filePathList.add(file.getAbsolutePath());
			}			
		}
		Collections.sort(filePathList);
		
		int width = 512;
		int height = filePathList.size();
		
		BufferedImage newImage = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		
		for(int i = 0; i < filePathList.size(); i++) {
			try {
				BufferedImage srcBuff = ImageIO.read(new File(filePathList.get(i)));
				
				for(int j = 0; j < width; j++) {
					int rgb = srcBuff.getRGB(col, j);
					newImage.setRGB(j, i, rgb);
				}
			
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		ImageIO.write(newImage, "jpg", new File("D:\\dcm\\temp\\AI_SHENG_QUN_P0564472\\apsi",col+".jpg"));
		
	}
	

	public static void main(String args[]) {
		//      create("test1.dcm");    //在本地目录生成test1.dcm.jpg图片文件
//		String path = "D:\\dcm\\temp\\AI_SHENG_QUN_P0564472\\1_25_LUNG_0005_0";
//		File files[] = new File(path).listFiles();
//		for(File file : files) {
//			create2(file.getAbsolutePath());
//		}
		
//		try {
//			createRLSI(path, 256);
//			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		//create2("D:\\dcm\\testdcm\\00000001.dcm");   //在电脑dicom文件夹下生成test1.dcm.jpg图片文件
		create2("D:\\dcm\\temp\\AI_SHENG_QUN_P0564472\\1_25_LUNG_0005_0\\AI_SHENG_QUN.CT.A.0005.0001.2020.02.20.07.13.03.31250.353054993.IMA");
		//create2("D:\\dcm\\temp\\AI_SHENG_QUN_P0564472\\1_25_LUNG_0005_0\\AI_SHENG_QUN.CT.A.0005.0002.2020.02.20.07.13.03.31250.353055022.IMA");
		//create2("D:\\dcm\\temp\\AI_SHENG_QUN_P0564472\\1_25_LUNG_0005_0\\AI_SHENG_QUN.CT.A.0005.0003.2020.02.20.07.13.03.31250.353055051.IMA");
	}

}
