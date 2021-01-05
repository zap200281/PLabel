package com.pcl.util;

import com.pcl.constant.Constants;

public class BucketNameUtil {

	
	public static String getRightBucketName(String bucketName) {
		if(bucketName.length() < 3) {
			return "label-def";
		}
		bucketName = bucketName.replace("_", "-");
		StringBuilder str = new StringBuilder();
		for(int i = 0; i < bucketName.length(); i++) {
			char t = bucketName.charAt(i);
			if(isValid(t)) {
				str.append(t);
			}
			if(str.length() > 62) {
				break;
			}
		}
		if(str.length() < 3) {
			return "label-def";
		}
		
		return str.toString().toLowerCase();
	}
	
	
	private static boolean isValid(char t) {
		if(isNumber(t) || isChar(t)) {
			return true;
		}
		if(t == '.' || t == '-') {
			return true;
		}
		return false;
	}


	private static boolean isChar(char t) {
		if(t >= 'a' && t <= 'z') {
			return true;
		}
		if(t >= 'A' && t <= 'Z') {
			return true;
		}
		return false;
	}


	private static boolean isNumber(char t) {
		if(t >= '0' && t <= '9') {
			return true;
		}
		return false;
	}


	public static String getBuketName(String dataSetType) {
		if(dataSetType.equals(String.valueOf(Constants.DATASET_TYPE_VIDEO))) {
			return "label-video";
		}else if(dataSetType.equals(String.valueOf(Constants.DATASET_TYPE_PICTURE))){
			return "label-img";
		}else if(dataSetType.equals(String.valueOf(Constants.DATASET_TYPE_DCM))){
			return "label-dcm";
		}else {
			return "label-def";
		}
	}
	
	public static void main(String[] args) {
		
		System.out.println(getRightBucketName("aaa.d_bbb.mp4"));
		System.out.println(getRightBucketName("aaa.d_bbb。中文aaa"));
	}
	
}
