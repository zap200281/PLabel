package com.pcl.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.util.Strings;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

public class ReIDUtil {

	private static Gson gson = new Gson();
	
	public static String getLabel(String pic_image_field,String labelId) {
		
		String srcLabelInfo = pic_image_field.substring(pic_image_field.lastIndexOf("/") + 1);
		srcLabelInfo = srcLabelInfo.substring(0,srcLabelInfo.length() -4);
		srcLabelInfo = srcLabelInfo + "_" + labelId + ".jpg";
		
		return srcLabelInfo;
	}
	
    public static String getLabelId(String reidImageName) {
		
		String id = reidImageName.substring(reidImageName.lastIndexOf("_") + 1);
		
		id = id.replace(".jpg", "");
		
		
		return id;
	}
    
    public static String getPrimitivePicture(String reidImageName) {
		
    	int index = reidImageName.lastIndexOf("_");
    	
    	int index2 = reidImageName.lastIndexOf(".");
    	
		String primitivePicture = reidImageName.substring(0,index) + reidImageName.substring(index2);
		
	
		return primitivePicture;
	}
    
    public static List<Map<String,Object>> getLabelList(String jsonLabelInfo){
    	
		if(Strings.isBlank(jsonLabelInfo)) {
			return new ArrayList<>();
		}
		ArrayList<Map<String,Object>> labelList = gson.fromJson(jsonLabelInfo, new TypeToken<ArrayList<Map<String,Object>>>() {
			private static final long serialVersionUID = 1L;}.getType());
		return labelList;
    	
    }
	
	
}
