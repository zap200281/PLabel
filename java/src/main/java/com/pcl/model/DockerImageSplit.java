package com.pcl.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.springframework.util.ResourceUtils;


public class DockerImageSplit {

	
	public static void main(String []args) {
	
		try(FileInputStream in = new FileInputStream(ResourceUtils.getFile("config.properties").getAbsolutePath())){
			Properties properties = new Properties();
			properties.load(in);
			System.out.println("start to oper file.");
			String oper = properties.getProperty("oper");
			
			if(oper != null) {
				System.out.println("oper=" + oper);
				if(oper.equals("split")) {
					splitFile(properties);
				}else if(oper.equals("merge")) {
					mergeFile(properties);
				}
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		

	}

	private static void mergeFile(Properties properties) {
		String fileName = properties.getProperty("file");
		System.out.println("dest file=" + fileName);
		try(FileOutputStream outputStream = new FileOutputStream(new File(fileName))){
			for(int i = 0; i < 1000; i++) {
				String name = fileName + "_" + i;
				System.out.println("find " + i + "th  file=" + name);
				File file = new File(name) ;
				if(file.exists()) {
					System.out.println("success find " + i + "th  file=" + name);
					System.out.println("start read " + name + " data  write to " + fileName);
					byte buffer[] = new byte[2048];
					try(FileInputStream inputStream = new FileInputStream(file)){
						int tmpLength = -1; 
						while((tmpLength = inputStream.read(buffer)) != -1) {
							outputStream.write(buffer,0,tmpLength);
						}
					}
					System.out.println("end read " + name + " data  write to " + fileName);
				}else {
					System.out.println(" not find " + i + "th  file=" + name + "   so exit.");
					break;
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

	private static void splitFile(Properties properties) throws IOException, FileNotFoundException {
		int size = 5 * 1024 * 1024 * 1024; //5G;
		String sizeS = properties.getProperty("size");
		if(sizeS != null) {
			size = Integer.parseInt(sizeS);
		}
		System.out.println("split size=" + size);
		String fileName = properties.getProperty("file");
		File file = new File(fileName);
		if(file.exists()) {
			System.out.println("source file=" + fileName);
			byte buffer[] = new byte[2048];
			try(FileInputStream inputStream = new FileInputStream(file)){
				int tmpLength = -1; 
				int count = 0;
				int fileIndex = 0;
				String splitFileName = fileName +"_" + fileIndex;
				FileOutputStream outputStream = null;
				while((tmpLength = inputStream.read(buffer)) != -1) {
					count += tmpLength;
					if(outputStream == null) {
						outputStream = new FileOutputStream(new File(splitFileName));
						System.out.println("dest file=" + splitFileName);
					}
					outputStream.write(buffer,0,tmpLength);
					if(count > size) {
						fileIndex ++;
						splitFileName = fileName +"_" + fileIndex;
						outputStream.flush();
						outputStream.close();
						outputStream = null;
						count=0;
					}
				}
				
				if(outputStream != null) {
					outputStream.flush();
					outputStream.close();
					outputStream = null;
				}
			}
			
		}
	}
	
}
