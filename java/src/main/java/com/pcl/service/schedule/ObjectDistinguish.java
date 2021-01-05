package com.pcl.service.schedule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pcl.util.JsonUtil;
import com.pcl.util.VocAnnotationsUtil;

public class ObjectDistinguish {

	private static Logger logger = LoggerFactory.getLogger(ObjectDistinguish.class);

	public boolean isContainCar(String algModelType) {
		List<String> typeList = JsonUtil.getList(algModelType);
		if(typeList.contains("car")) {
			return true;
		}
		return false;
	}

	public Map<String,Object> getTypeColor(Map<String,Object> colorMap,Map<String,Object> typeMap,String id){
		
		Map<String,Object> re = new HashMap<>();
		Map<String,Object> region_attributes = new HashMap<>();
		region_attributes.put("id", id);
		if(colorMap.containsKey(id)) {
			re.put("color", colorMap.get(id));
			region_attributes.put("color", colorMap.get(id));
		}
		if(typeMap.containsKey(id)) {
			re.put("type", typeMap.get(id));
			region_attributes.put("type", typeMap.get(id));
		}
		
		Map<String,Object> other = new HashMap<>();
		other.put("region_attributes", region_attributes);

		re.put("other", other);
		
		return re;
	}

	public void saveImageSetTestTxt(String testTxtPath,Map<String,Object> map) {
		logger.info("writer test txt:" + testTxtPath);
		File testTxtFile = new File(testTxtPath);
		testTxtFile.getParentFile().mkdirs();
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(testTxtPath))){
			for(Entry<String,Object> entry : map.entrySet()) {
				String fileName = entry.getKey();
				writer.write(fileName.substring(0,fileName.lastIndexOf(".")));
				writer.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveToXml(String labelInfo,String imagePath,String xmlPath,VocAnnotationsUtil vocAnnotation) {

		ArrayList<Map<String,Object>> labelList = JsonUtil.getLabelList(labelInfo);
		for(int i = 0; i < labelList.size(); i++) {
			Map<String,Object> map = labelList.get(i);
			String className = String.valueOf(map.get("class_name"));
			if(className.equals("person")) {
				labelList.remove(i);
				i--;
			}
		}

		Document doc = vocAnnotation.getXmlDocument(labelList, new HashMap<>(), imagePath);
		if(doc != null) {

			FileWriter fileWriter = null;
			try {
				fileWriter = new FileWriter(xmlPath);
				OutputFormat format = new OutputFormat("\t", true);
				format.setTrimText(true);//去掉原来的空白(\t和换行和空格)！

				XMLWriter writer = new XMLWriter(fileWriter, format);
				// 把document对象写到out流中。
				writer.write(doc);
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else {
			logger.info("doc is null. imagePath=" + imagePath);
		}

	}


}
