package com.pcl.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.util.Strings;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

public class JsonUtil {

	private static Gson gson = new Gson();
	
	public static String toJson(Object obj) {
		return gson.toJson(obj);
	}


	public static List<String> getList(String listStr){
		if(listStr == null || listStr.isEmpty()) {
			return new ArrayList<>();
		}
		return gson.fromJson(listStr, new TypeToken<List<String>>() {
			private static final long serialVersionUID = 1L;}.getType());
	}
	
	public static Map<String,Object> getMap(String listStr){
		Map<String,Object> re = new HashMap<>();
		if(Strings.isBlank(listStr)) {
			return re;
		}
		return gson.fromJson(listStr, new TypeToken<Map<String,Object>>() {
			private static final long serialVersionUID = 1L;}.getType());
	}
	
	public static Map<String,String> getStrMap(String listStr){
		Map<String,String> re = new HashMap<>();
		if(Strings.isBlank(listStr)) {
			return re;
		}
		return gson.fromJson(listStr, new TypeToken<Map<String,String>>() {
			private static final long serialVersionUID = 1L;}.getType());
	}
	
	
	public static ArrayList<Map<String,Object>> getLabelList(String labelInfo){
		ArrayList<Map<String,Object>> labelList = new ArrayList<>();
		if(Strings.isBlank(labelInfo)) {
			return labelList;
		}
		return gson.fromJson(labelInfo, new TypeToken<ArrayList<Map<String,Object>>>() {
			private static final long serialVersionUID = 1L;}.getType());
	}

	public static void main(String[] args) {
		
		System.out.println(String.format("%3d",12).replace(" ", "0"));
		
	}
	
}
