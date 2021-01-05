package com.pcl.util.mmdetetcion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import com.pcl.util.FileUtil;

public class DataSetClassReplaceUtil {

	private static String CLASSES_PREFIX = "CLASSES =";
	
	public void replaceClass(List<String> typeList,String pyPath) {
		
		List<String> allLine = FileUtil.getAllLineList(pyPath, "utf-8");
		List<String> newLineList = new ArrayList<>();
		for(int i = 0; i <allLine.size(); i++) {
			String line = allLine.get(i);
			if(line.trim().startsWith(CLASSES_PREFIX)) {
				int j = i;
				for(; j < allLine.size(); j++) {
					String endLine = allLine.get(j);
					if(endLine.trim().endsWith(")")){
						break;
					}
				}
				StringBuilder strBuilder = new StringBuilder();
				for(int k=i; k <=j; k++) {
					strBuilder.append(allLine.get(k));
				}
				i=j;
				
				List<String> oldTypeList = getTypeList(strBuilder);
				for(String tmpType : typeList) {
					if(!oldTypeList.contains("'" + tmpType + "'")) {
						oldTypeList.add("'" + tmpType + "'");
					}
				}
				
				String tmpLine = line.substring(0,line.indexOf(CLASSES_PREFIX));
				tmpLine += CLASSES_PREFIX + "(";
				for(String type : oldTypeList) {
					tmpLine += type + ",";
				}
				tmpLine +=")";

				newLineList.add(tmpLine);
				
				continue;
			}
			
			
			
			newLineList.add(line);
		}
		
		try(BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pyPath),"utf-8"))){
			for(String line : newLineList) {
				bufferedWriter.write(line);
				bufferedWriter.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<String> getTypeList(StringBuilder strBuilder) {
		List<String> re = new ArrayList<>();
		String tmp = strBuilder.toString();
		int firstIndex = tmp.indexOf("(");
		int endIndex = tmp.lastIndexOf(")");
		
		String sub = tmp.substring(firstIndex+1,endIndex);
		String subType[] = sub.split(",");
		
		for(String tmpType : subType) {
			if(tmpType.trim().length() > 0) {
				re.add(tmpType.trim());
			}
		}
		return re;
	}
	
	public static void main(String[] args) {
		String path = "D:\\2019文档\\问题定位\\0720";
		
		File files[] = new File(path).listFiles();
		DataSetClassReplaceUtil vocclassUtil = new DataSetClassReplaceUtil();
		
		List<String> newTypeList = new ArrayList<>();
		newTypeList.add("dddd4");
		
		for(File file : files) {
			if(file.isFile() && file.getName().endsWith(".py")) {
				
				vocclassUtil.replaceClass(newTypeList, file.getAbsolutePath());
				
			}
		}
		
	}
	
}
