package com.pcl.model;

import java.io.File;

import com.pcl.util.FileUtil;

public class TempClean {


	public static void renameFileAddPrefix(String path,String prefix) {

		File file = new File(path);

		File files[] = file.listFiles();

		for(File tmpFile : files) {
			String name = tmpFile.getName();
			name = prefix + name;
			tmpFile.renameTo(new File(path,name));
		}

	}

	public static void renameFileAddPostfix(String path,String postfix) {

		File file = new File(path);

		File files[] = file.listFiles();

		for(File tmpFile : files) {
			String name = tmpFile.getName();

			String tmp = name.substring(0,name.length() - 4);

			tmp = tmp + postfix + name.substring(name.length() - 4);

			tmpFile.renameTo(new File(path,tmp));
		}

	}

	public static void deleteFile(String srcPath,String destPath) {
		File file = new File(srcPath);
		File files[] = file.listFiles();
		for(File tmpFile : files) {
			String name = tmpFile.getName();
			if(new File(destPath,name).exists()) {
				tmpFile.delete();
			}
		}
	}



	public static void main(String[] args) {
		
		//renameFileAddPostfix("D:\\code\\new\\Pedestrian-Detection\\annotations\\bak", "_zap");
		//renameFileAddPostfix("D:\\BaiduNetdiskDownload\\PASCAL_VOC\\VOCdevkit\\VOC2007\\JPEGImages", "_voc2007");
		
		//deleteFile("D:\\BaiduNetdiskDownload\\PASCAL_VOC\\VOCdevkit\\VOC2007\\Annotations","D:\\BaiduNetdiskDownload\\Pedestrain\\VOCdevkit\\VOC2007\\Annotations");
		
		//deleteFile("D:\\BaiduNetdiskDownload\\PASCAL_VOC\\VOCdevkit\\VOC2007\\JPEGImages","D:\\BaiduNetdiskDownload\\Pedestrain\\VOCdevkit\\VOC2007\\JPEGImages");
		
		String path = "D:\\BaiduNetdiskDownload\\Pedestrain\\VOCdevkit\\VOC2007\\JPEGImages";
		
		String destPath = "D:\\avi\\cityscape\\";
		
		File file = new File(path);
		File files[] = file.listFiles();
		for(File tmpFile : files) {
			String name = tmpFile.getName();
			if(name.endsWith("_zap.jpg")) {
				FileUtil.copyFile(tmpFile.getAbsolutePath(), destPath + name);
			}
		}
	
	
	}
}
