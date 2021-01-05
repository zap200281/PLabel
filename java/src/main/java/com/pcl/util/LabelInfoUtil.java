package com.pcl.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pcl.constant.LogConstants;

public class LabelInfoUtil {

	private static Logger logger = LoggerFactory.getLogger(LabelInfoUtil.class);

	public static Map<String,Integer> getCompareResult(List<Map<String,Object>> oldLabelList,List<Map<String,Object>> newLabelList){
		Map<String,Integer> result = new HashMap<>();

		Map<String,Map<String,Object>> oldLabelMap = getIdMap(oldLabelList);
		Map<String,Map<String,Object>> newLabelMap = getIdMap(newLabelList);

		int rectUpdate = 0;
		int rectAdd = 0;
		int properties = 0;

		for(Entry<String,Map<String,Object>> newEntry : newLabelMap.entrySet()) {
			String id = newEntry.getKey();
			if(oldLabelMap.containsKey(id)) {
				//更新
				int singleProperties =  getPropertiesCount(oldLabelMap.get(id), newEntry.getValue(),result);
				if(singleProperties > 0) {//只有更新了属性之后，才能算作更新。
					rectUpdate++;
				}
				properties += singleProperties;
			}else {
				//增加标注框
				rectAdd ++;
				properties += getPropertiesCount(new HashMap<>(), newEntry.getValue(),result);
			}
		}
		result.put(LogConstants.RECTUPDATE, rectUpdate);
		result.put(LogConstants.RECTADD, rectAdd);
		result.put(LogConstants.PROPERTIES, properties);

		if(rectUpdate > 0 || rectAdd > 0 || properties > 0) {
			result.put(LogConstants.PICTUREUPDATE, 1);//该图片有更新
		}else {
			result.put(LogConstants.PICTUREUPDATE, 0);
		}
		return result;
	}


	private static int getPropertiesCount(Map<String,Object> oldMap, Map<String,Object> newMap, Map<String,Integer> result) {
		int count = 0;
		for(Entry<String,Object> entry : newMap.entrySet()) {
			String key = entry.getKey();
			if(isFilter(key)) {
				continue;
			}
			if(key.equals("other")) {
				count += equalOtherResult(entry.getValue(),oldMap.get(key),result);
			}else {
				int re = equalResult(entry.getValue(),oldMap.get(key));
				if(re > 0 && key.equals("box")) {
					result.put("box", 1);
				}
				count += re;
			}

		}
		return count;
	}

	private static int equalOtherResult(Object newValue, Object oldValue,Map<String,Integer> result) {
		int count = 0;
		if(equalResult(newValue, oldValue) == 1) {
			if(newValue != null && oldValue != null) {
				Object newRegion_attributes = ((Map<String,Object>)newValue).get(LogConstants.REGION_ATTRIBUTES);
				Object oldRegion_attributes = ((Map<String,Object>)oldValue).get(LogConstants.REGION_ATTRIBUTES);
				if(oldRegion_attributes != null && newRegion_attributes != null) {
					@SuppressWarnings("unchecked")
					Map<String,Object> oldRegion = ((Map<String,Object>)oldRegion_attributes);
					Map<String,Object> newRegion = ((Map<String,Object>)newRegion_attributes);

					for(Entry<String,Object> entry : newRegion.entrySet()) {
						String key = entry.getKey();
						if(key.equals(LogConstants.VERIFY_FIELD)) {
							//logger.info("newRegion=" + newRegion + " oldRegion=" +oldRegion);
						}
						int re = equalResult(entry.getValue(),oldRegion.get(key));
						if(key.equals(LogConstants.VERIFY_FIELD) && re > 0 &&  newRegion.get(LogConstants.VERIFY_FIELD).toString().length() > 0 && !LogConstants.VERIFY_FIELD_RESULT_VALID_0.equals(String.valueOf(entry.getValue()))) {
							if(result.get(LogConstants.NOT_VALIDE) != null) {
								result.put(LogConstants.NOT_VALIDE, result.get(LogConstants.NOT_VALIDE) + 1);
							}else {
								result.put(LogConstants.NOT_VALIDE, 1);
							}
						}
						count += re;
					}
				}
			}else{
				//				if(newValue == null && oldValue != null) {
				//					Object oldRegion_attributes = ((Map<String,Object>)oldValue).get(LogConstants.REGION_ATTRIBUTES);
				//					if(newRegion_attributes != null) {
				//						Map<String,Object> newRegion = ((Map<String,Object>)newRegion_attributes);
				//						return getAttrSize(newRegion);
				//					}
				//				}
				if(newValue != null && oldValue == null) {
					Object newRegion_attributes = ((Map<String,Object>)newValue).get(LogConstants.REGION_ATTRIBUTES);
					if(newRegion_attributes != null) {
						Map<String,Object> newRegion = ((Map<String,Object>)newRegion_attributes);
						if(newRegion.get(LogConstants.VERIFY_FIELD) != null  && newRegion.get(LogConstants.VERIFY_FIELD).toString().length() > 0) {
							//logger.info("newRegion=" + newRegion);
							if(!newRegion.get(LogConstants.VERIFY_FIELD).toString().equals(LogConstants.VERIFY_FIELD_RESULT_VALID_0)) {
								if(result.get(LogConstants.NOT_VALIDE) != null) {
									result.put(LogConstants.NOT_VALIDE, result.get(LogConstants.NOT_VALIDE) + 1);
								}else {
									result.put(LogConstants.NOT_VALIDE, 1);
								}
							}
						}
						return getAttrSize(newRegion);
					}
				}
			}

		}
		return count;
	}

	private static int getAttrSize(Map<String,Object> newRegion) {
		int size = 0;
		for(Entry<String,Object> entry : newRegion.entrySet()) {
			if(entry.getKey().equals("id") || entry.getKey().equals("type")) {
				continue;
			}
			if(!isEmpty(entry.getValue())) {
				size++;
			}
		}
		return size;
	}

	private static boolean isEmpty(Object value) {
		if(value == null || value.toString().length() == 0) {
			return true;
		}
		return false;
	}

	private static int equalResult(Object value, Object value1) {
		if(isEmpty(value) && !isEmpty(value1)) {
			return 1;
		}
		if(!isEmpty(value) && isEmpty(value1)) {
			return 1;
		}
		if(value != null && value1 != null) {
			if(value instanceof List && value1 instanceof List) {
				//box的相等要特殊处理
				List<Object> valueList = (List<Object>)value;
				List<Object> value1List = (List<Object>)value1;
				if(valueList.size() != value1List.size()) {
					return 1;
				}
				for(int i = 0; i <valueList.size(); i++) {
					Object valueObj = valueList.get(i);
					Object value1Obj = value1List.get(i);
					double valueD = getDouble(valueObj);
					double value1D = getDouble(value1Obj);
					if(Math.abs(valueD - value1D) >= 2) {
						return 1;
					}
				}
			}else {
				if(value instanceof Double || value1 instanceof Double) {
					if(value instanceof Double) {
						value = (int)(Double.parseDouble(value1.toString()));
					}
					if(value1 instanceof Double) {
						value1 = (int)(Double.parseDouble(value1.toString()));
					}
				}

				if(value.toString().equals(value1.toString())) {//相等
					return 0;
				}else {
					return 1;
				}
			}
		}
		return 0;
	}

	private static double getDouble(Object valueObj) {
		if(valueObj instanceof String) {
			return Double.parseDouble((String)valueObj);
		}else if(valueObj instanceof Double) {
			return (double)valueObj;
		}else if(valueObj instanceof Integer) {
			return (Integer)valueObj;
		}else if(valueObj instanceof Float) {
			return (Float)valueObj;
		}
		return 0;
	}

	private static boolean isFilter(String key) {
		if(key.equals("blurred") || key.equals("goodIllumination") || key.equals("frontview") || key.equals("score")) {
			return true;
		}
		return false;
	}


	private static Map<String,Map<String,Object>> getIdMap(List<Map<String,Object>> labelList){
		Map<String,Map<String,Object>> result = new HashMap<>();
		for(Map<String,Object> label : labelList) {
			Object id = label.get("id");
			if(id != null && id instanceof Double) {
				id = (int)((double)id);
			}
			if(id == null || id.toString().length() == 0) {
				id = label.get("box");
				if(id == null) {
					id = label.get("mask");
					if(id == null) {
						label.get("keypoints");
					}
				}
			}
			if(id != null) {
				result.put(id.toString(), label);
			}else {
				result.put(label.toString(), label);
			}
		}
		return result;
	}


}
