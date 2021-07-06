package com.pcl.model;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class DeleteNotNeedPicture {


	public static void deleteNotNeedPicture(String srcPath) {
		File annotation = new File(srcPath,"Annotations");
		File files[] = annotation.listFiles();
		
		File jpeg = new File(srcPath,"JPEGImages");
		File jpegFiles[] = jpeg.listFiles();
		
		Map<String,Integer> jpegMap = new HashMap<>();
		for(File file : jpegFiles) {
			jpegMap.put(file.getName(), 1);
		}
		
		int size = jpegMap.size();
		System.out.println("total image size :" + jpegMap.size());
		int count = 0;
		
		for(File file : files) {
			String imagName = file.getName().substring(0,file.getName().length() - 4) + ".jpg";
			if(jpegMap.containsKey(imagName)) {
				jpegMap.remove(imagName);
			}else {
				count++;
			}
		}
		
		System.out.println("xml to image size:" + (size - jpegMap.size()));
		System.out.println("neet to delete image size :" + jpegMap.size());
		System.out.println("data error count :" + count);
		
		for(Entry<String,Integer> entry : jpegMap.entrySet()) {
			String imagName = entry.getKey();
			File file = new File(jpeg,imagName);
			file.delete();
		}
	}
	
	
	public static void deleteNotEqualAnnotation(String srcPath) {
		File annotation = new File(srcPath,"Annotations");
		File files[] = annotation.listFiles();
	
		for(File file : files) {
			String imagName = getImgNameFromFile(file);
			String name1 = file.getName().substring(0,file.getName().length() - 4);
			String name2 = imagName;
			if(name2.length() > 4) {
				name2 = name2.substring(0,name2.length() -4);
			}
			if(!name1.equals(name2)) {
				file.delete();
				System.out.println("delete file:" + file.getAbsolutePath());
			}
		}
	}
	
	
	public static void main(String[] args) {

		String srcPath = "D:\\2021文档\\主动学习分类\\VOC2007";

		//deleteNotEqualAnnotation(srcPath);
		
		deleteNotNeedPicture(srcPath);

	}
	
	public static Document parsingXML(String xmlFilePath) {
        Document document = null;
        try {
            //创建解析器
            SAXReader reader=new SAXReader();//创建读取文件内容对象
            document=reader.read(xmlFilePath);//指定文件并读取

        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return document;
    }
	
	
	public static String  getImgNameFromFile(File file) {
		Document docFile = parsingXML(file.getAbsolutePath());
		
		Element readRoot = docFile.getRootElement();
	   
		String imgName = readRoot.elementText("filename");
		
		return imgName;
	}

}
