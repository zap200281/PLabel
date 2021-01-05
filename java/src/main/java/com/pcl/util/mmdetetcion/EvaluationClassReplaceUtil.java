package com.pcl.util.mmdetetcion;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import com.pcl.util.FileUtil;

public class EvaluationClassReplaceUtil {
	
	private static String VOC_CLASSES_PREFIX = "def voc_classes()";
	
	private static String COCO_CLASSES_PREFIX = "def coco_classes()";

	public void replaceClass(List<String> typeList,String pyPath) {
		
		List<String> allLine = FileUtil.getAllLineList(pyPath, "utf-8");
		List<String> newLineList = new ArrayList<>();
		for(int i = 0; i <allLine.size(); i++) {
			String line = allLine.get(i);
			if(line.trim().startsWith(VOC_CLASSES_PREFIX) || line.trim().startsWith(COCO_CLASSES_PREFIX)) {
				newLineList.add(line);
				int j = i + 1;
				String startLine = allLine.get(j);
				newLineList.add(startLine);
				j++;
				
				StringBuilder newTypeBuilder = new StringBuilder();
				
				StringBuilder strBuilder = new StringBuilder();
				for(;j < allLine.size(); j++) {
					String tmp = allLine.get(j);
					if(tmp.trim().endsWith("]")) {
						break;
					}
					int index = tmp.indexOf("'");
					if(index != -1 && newTypeBuilder.length() == 0) {
						newTypeBuilder.append(tmp.substring(0,index));
					}
					strBuilder.append(tmp);
				}
				
				
				List<String> oldTypeList = getTypeList(strBuilder);
				for(String tmpType : typeList) {
					if(!oldTypeList.contains("'" + tmpType + "'")) {
						oldTypeList.add("'" + tmpType + "'");
					}
				}
				
				for(String tmpType : oldTypeList) {
					newTypeBuilder.append(tmpType).append(",");
				}
				newTypeBuilder.deleteCharAt(newTypeBuilder.length() - 1);
				newLineList.add(newTypeBuilder.toString());
				
				newLineList.add(allLine.get(j));
				i = j;
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

		String subType[] = tmp.split(",");
		
		for(String tmpType : subType) {
			if(tmpType.trim().length() > 0) {
				re.add(tmpType.trim());
			}
		}
		return re;
	}
	
	public static void main(String[] args) {
		String path = "D:\\2019文档\\问题定位\\0720\\class_names.py";
		
		List<String> newTypeList = new ArrayList<>();
		newTypeList.add("dddd4");
		
		EvaluationClassReplaceUtil util = new EvaluationClassReplaceUtil();
		
		util.replaceClass(newTypeList, path);
		
		
	}
	
}
