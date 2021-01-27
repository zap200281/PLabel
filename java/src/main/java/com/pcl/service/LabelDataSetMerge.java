package com.pcl.service;

public class LabelDataSetMerge {
	
	
	public static String getAlgRootPath(String path) {
		String os = System.getProperty("os.name"); 
		if(os.toLowerCase().startsWith("win")){ 
			return "D:\\jango\\faster-rcnn.pytorch\\";
		}else {
			return path;
		}
	}
	
	
	public static String getUserDataSetPath() {
		String os = System.getProperty("os.name"); 
		if(os.toLowerCase().startsWith("win")){ 
			return "D:\\media";
		}else {
			return System.getProperty("user.home") + "/userdataset";
		}
		
	}
	
	public static String getAllDownLoadFilePath() {
		String os = System.getProperty("os.name"); 
		if(os.toLowerCase().startsWith("win")){ 
			return "D:\\media";
		}else {
			return  "/data/allfile";
		}
		
	}
	
	
}
