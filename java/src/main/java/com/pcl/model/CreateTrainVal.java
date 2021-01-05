package com.pcl.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.pcl.control.TmpUtilController;
import com.pcl.util.FileUtil;

public class CreateTrainVal {

	private static Logger logger = LoggerFactory.getLogger(CreateTrainVal.class);
	
	public void createTrainVal(String srcPath,boolean isWriteTest) {
		String trainDestPath = srcPath + File.separator + "ImageSets" + File.separator + "Main" + File.separator + "trainval.txt";
		String testDestPath = srcPath + File.separator + "ImageSets" + File.separator + "Main" + File.separator + "test.txt";
		
		File annotation = new File(srcPath,"Annotations");
		
		File files[] = annotation.listFiles();
		
		
		File destFile = new File(trainDestPath);
		if(!destFile.getParentFile().exists()) {
			destFile.getParentFile().mkdirs();
		}
		
		int count = 0;
		try(BufferedWriter trainWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(trainDestPath), "utf-8"));
				BufferedWriter testWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(testDestPath), "utf-8"))){
			for(File file : files) {
				String name = file.getName();
				name = name.substring(0,name.length() - 4);
				
				String srcImgName = file.getParentFile().getParentFile().getAbsolutePath() + File.separator + "JPEGImages" + File.separator + name + ".jpg";
				if(!new File(srcImgName).exists()) {
					logger.info("The img not exists. path=" + srcImgName + " so delete the xml:" + file.getAbsolutePath());
					file.delete();
					continue;
				}
				
				if(count % 10 ==0 && isWriteTest) {
					testWriter.write(name);
					testWriter.newLine();
				}else {
					trainWriter.write(name);
					trainWriter.newLine();
				}
				count ++;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void createTrainVal(String srcPath,double ratio) {
		String trainDestPath = srcPath + File.separator + "ImageSets" + File.separator + "Main" + File.separator + "trainval.txt";
		String testDestPath = srcPath + File.separator + "ImageSets" + File.separator + "Main" + File.separator + "test.txt";
		
		File annotation = new File(srcPath,"Annotations");
		
		File files[] = annotation.listFiles();
		
		int total = files.length;
		
		int testNum = (int)(total * ratio);
		
		File destFile = new File(trainDestPath);
		if(!destFile.getParentFile().exists()) {
			destFile.getParentFile().mkdirs();
		}
		
		Random r = new Random();
		HashMap<Integer,Integer> testMap = new HashMap<>();
		while(testMap.size() <testNum) {
			int re = r.nextInt(total);
			testMap.put(re, re);
		}
		
		try(BufferedWriter trainWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(trainDestPath), "utf-8"));
				BufferedWriter testWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(testDestPath), "utf-8"))){
			for(int i = 0; i < total; i++) {
				File file = files[i];
				String name = file.getName();
				name = name.substring(0,name.length() - 4);
				
				String srcImgName = file.getParentFile().getParentFile().getAbsolutePath() + File.separator + "JPEGImages" + File.separator + name + ".jpg";
				if(!new File(srcImgName).exists()) {
					logger.info("The img not exists. path=" + srcImgName + " so delete the xml:" + file.getAbsolutePath());
					file.delete();
					continue;
				}
				
				if(testMap.containsKey(i)) {
					testWriter.write(name);
					testWriter.newLine();
				}else {
					trainWriter.write(name);
					trainWriter.newLine();
				}
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	public void copyTestPicture(String srcPath) {
		String testDestPath = srcPath + File.separator + "ImageSets" + File.separator + "Main" + File.separator + "test.txt";
		
		List<String> allLine = FileUtil.getAllLineList(testDestPath, "utf-8");
		
		String picturePath = srcPath + File.separator + "JPEGImages" + File.separator;
		
		String destPath = srcPath + File.separator + "JPEGImages_test" + File.separator;
		
		for(String name : allLine) {
			
			File srcFile = new File(picturePath,name + ".jpg");
			
			File destFile = new File(destPath,name + ".jpg");
			if(srcFile.exists()) {
				try {
					Files.copy(srcFile, destFile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	
	
	public static void main(String[] args) {
		
		String srcPath = "D:\\BaiduNetdiskDownload\\Pedestrain\\VOCdevkit\\VOC2007";
		
		CreateTrainVal create = new CreateTrainVal();
		create.createTrainVal(srcPath, true);
		//create.copyTestPicture(srcPath);

	}
	
}
