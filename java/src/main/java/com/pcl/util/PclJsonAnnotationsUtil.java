package com.pcl.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.service.MinioFileService;
import com.pcl.service.ObjectFileService;

@Service
public class PclJsonAnnotationsUtil {

	@Autowired
	private ObjectFileService fileService;

	public Map<String,Object> getJson(LabelTaskItem item,Map<String,Object> typeOrColorMapName){

		Map<String,Object> re = new HashMap<>();

		String labelInfo = item.getLabel_info();
		ArrayList<Map<String,Object>> labelList = JsonUtil.getLabelList(labelInfo);
		if(labelList.isEmpty()) {
			return re;
		}

		String fileName = item.getPic_image_field();
		fileName = fileName.substring(fileName.lastIndexOf("/") + 1);


		String widthHeigth = item.getPic_object_name();
		if(widthHeigth == null || widthHeigth.indexOf(",") == -1) {
			widthHeigth = fileService.getImageWidthHeight(item.getPic_image_field());
		}
		if(!Strings.isEmpty(widthHeigth)) {
			String tmp[] = widthHeigth.split(",");
			re.put("image_width", Integer.parseInt(tmp[0]));
			re.put("image_height", Integer.parseInt(tmp[1]));
		}else {
			re.put("image_width", 0);
			re.put("image_height", 0);
		}

		re.put("image_name", fileName);

		List<Map<String,Object>>  annotationList = new ArrayList<>();

		for(Map<String,Object> label : labelList) {
			Map<String,Object> annotation = new HashMap<>();
			String className = CocoAnnotationsUtil.getClassName(label);
			if(Strings.isBlank(className)) {
				//continue;
			}
			annotation.put("category_id", className);

			if(typeOrColorMapName.get("type") != null) {
				Map<String,Object> typeMap = (Map<String,Object>)typeOrColorMapName.get("type");
				if(typeMap.containsKey(className)) {
					annotation.put("category_name", typeMap.get(className));
				}
			}

			List<Object> boxList = (List<Object>)label.remove("box");
			if(boxList != null) {//矩形标注
				int xmin = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(0)));
				int ymin = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(1)));
				int xmax = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(2)));
				int ymax = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(3)));
				annotation.put("top", ymin);
				annotation.put("left", xmin);
				annotation.put("bottom", ymax);
				annotation.put("right", xmax);
			}else {
				List<Object> maskList = (List<Object>)label.remove("mask");
				if(maskList != null) {
					annotation.put("segmentation", getListStr(maskList));
				}
				List<Object> keypointsList = (List<Object>)label.remove("keypoints");
				if(keypointsList != null) {
					annotation.put("keypoints", getListStr(keypointsList));
				}
			}
			
		
			CocoAnnotationsUtil.addEleToMap(label, annotation);

			dealColor(typeOrColorMapName, annotation);

			annotationList.add(annotation);
		}

		re.put("bbox", annotationList);
		return re;
	}

	private void dealColor(Map<String, Object> typeOrColorMapName, Map<String, Object> annotation) {
		if(annotation.get("region_attributes") != null && annotation.get("region_attributes") instanceof Map) {
			Map<String,Object> region_attributesMap = (Map<String,Object>)annotation.get("region_attributes");
			if(region_attributesMap.containsKey("color")) {
				annotation.put("color_id", region_attributesMap.remove("color"));
			}
		}

		if(annotation.get("color_id") != null) {
			String color = String.valueOf(annotation.get("color_id"));
			if(typeOrColorMapName.get("color") != null) {
				Map<String,Object> typeMap = (Map<String,Object>)typeOrColorMapName.get("color");
				if(typeMap.containsKey(color)) {
					annotation.put("color_name", typeMap.get(color));
				}
			}
		}
	}

	public Map<String,Map<String,Object>> getReIdJson(LabelTaskItem item,Map<String,Object> typeOrColorMapName){

		Map<String,Map<String,Object>> re = new HashMap<>();

		String labelInfo = item.getLabel_info();
		ArrayList<Map<String,Object>> labelList = JsonUtil.getLabelList(labelInfo);
		if(labelList.isEmpty()) {
			return re;
		}

		String fileName = item.getPic_image_field();
		fileName = fileName.substring(fileName.lastIndexOf("/") + 1);

		Map<String,Object> picInfoMap = new HashMap<>();
		String widthHeigth = item.getPic_object_name();
		if(widthHeigth == null || widthHeigth.indexOf(",") != -1) {
			widthHeigth = fileService.getImageWidthHeight(item.getPic_image_field());
		}
		if(!Strings.isEmpty(widthHeigth)) {
			String tmp[] = widthHeigth.split(",");
			picInfoMap.put("image_width", Integer.parseInt(tmp[0]));
			picInfoMap.put("image_height", Integer.parseInt(tmp[1]));
		}else {
			picInfoMap.put("image_width", 0);
			picInfoMap.put("image_height", 0);
		}

		picInfoMap.put("image_name", fileName);



		for(Map<String,Object> label : labelList) {
			Map<String,Object> annotation = new HashMap<>();
			String className = CocoAnnotationsUtil.getClassName(label);
			if(Strings.isBlank(className)) {
				continue;
			}
			annotation.put("category_id", className);

			if(typeOrColorMapName.get("type") != null) {
				Map<String,Object> typeMap = (Map<String,Object>)typeOrColorMapName.get("type");
				if(typeMap.containsKey(className)) {
					annotation.put("category_name", typeMap.get(className));
				}
			}

			List<Object> boxList = (List<Object>)label.remove("box");
			if(boxList != null) {//矩形标注
				int xmin = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(0)));
				int ymin = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(1)));
				int xmax = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(2)));
				int ymax = CocoAnnotationsUtil.getIntStr(String.valueOf(boxList.get(3)));

				annotation.put("top", ymin);
				annotation.put("left", xmin);
				annotation.put("bottom", ymax);
				annotation.put("right", xmax);

			}else {
				continue;
			}

			CocoAnnotationsUtil.addEleToMap(label, annotation);

			dealColor(typeOrColorMapName, annotation);

			Object reId = annotation.get("reId");
			if(reId == null || reId.toString().isEmpty()) {
				continue;
			}
			annotation.putAll(picInfoMap);

			re.put(reId.toString(), annotation);
		}
		return re;
	}


	private String getStr(Object obj) {
		if(obj == null) {
			return "";
		}else {
			return obj.toString();
		}
	}

	private String getListStr(List<Object> list) {
		StringBuilder strB = new StringBuilder();
		for(Object obj : list) {
			strB.append(getStr(obj)).append(",");
		}
		if(strB.length() > 0) {
			strB.deleteCharAt(strB.length() - 1);
		}
		return strB.toString();
	}

	public LabelTaskItem readLabelInfoFromPclJson(InputStream in) {
		StringBuilder strBuild = new StringBuilder();
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(in))){
			String line = null;
			while(true) {
				line = reader.readLine();
				if(line == null) {
					break;
				}
				strBuild.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		Map<String,Object> map = JsonUtil.getMap(strBuild.toString());


		LabelTaskItem item = new LabelTaskItem();
		item.setItem_add_time(TimeUtil.getCurrentTimeStr());

		if( map.get("image_width") == null ||  map.get("image_height") == null) {
			return null;
		}

		String width = map.get("image_width").toString();
		String height = map.get("image_height").toString();
		item.setPic_object_name(width + "," + height);

		if(map.get("bbox") != null && map.get("bbox") instanceof List) {
			List<Map<String,Object>>  annotationList = (List<Map<String,Object>>)map.remove("bbox");
			ArrayList<Map<String,Object>> labelList = new ArrayList<>();
			int count = 1;
			for(Map<String,Object> objMap : annotationList) {

				Map<String,Object> label = new HashMap<>();
				labelList.add(label);

				label.put("class_name", objMap.remove("category_id"));

				List<Object> bndBoxList = new ArrayList<>();
				bndBoxList.add(getStrObj(objMap.remove("left")));
				bndBoxList.add(getStrObj(objMap.remove("top")));
				bndBoxList.add(getStrObj(objMap.remove("right")));
				bndBoxList.add(getStrObj(objMap.remove("bottom")));

				label.put("box", bndBoxList);

				Object segObj = objMap.remove("segmentation");
				if(segObj != null) {
					String seg = segObj.toString();
					String segs[] = seg.split(",");
					List<Object> mask = new ArrayList<>();
					for(String segEle : segs) {
						mask.add(segEle);
					}
					label.put("mask", mask);
				}
				Object keypointsObj = objMap.remove("keypoints");
				if(keypointsObj != null) {
					String seg = keypointsObj.toString();
					String segs[] = seg.split(",");
					List<Object> keypoints = new ArrayList<>();
					for(String segEle : segs) {
						keypoints.add(segEle);
					}
					label.put("keypoints", keypoints);
				}


				if(objMap.get("color_id") != null) {
					label.put("color", getStrObj(objMap.remove("color_id")));
				}

				Object other = objMap.remove("other");
				if(other != null) {
					Map<String,Object> otherMap = new HashMap<>();
					Map<String,Object> region_attributes = new HashMap<>();
					if(other instanceof Map) {
						Object region_attributesObj = ((Map<String,Object>)other).get("region_attributes");
						if(region_attributesObj != null && region_attributesObj instanceof Map) {
							region_attributes.putAll((Map<String,Object>)region_attributesObj);
						}
					}
					otherMap.put("region_attributes", region_attributes);
					label.put("other", otherMap);
				}

				Object region_attributes = objMap.remove("region_attributes");
				if(region_attributes != null) {
					Map<String,Object> otherMap = new HashMap<>();

					if(region_attributes instanceof Map) {
						for(Entry<String,Object> entry : ((Map<String,Object>)region_attributes).entrySet()) {
							entry.setValue(getStrObj(entry.getValue()));
						}

					}

					otherMap.put("region_attributes", region_attributes);
					label.put("other", otherMap);
				}


				Object id = objMap.remove("id");
				if(id != null) {
					label.put("id", getStrObj(id));
				}else {
					label.put("id", String.valueOf(count++));
				}

				label.putAll(objMap);

			}
			item.setLabel_info(JsonUtil.toJson(labelList));
		}

		return item;
	}

	private String getStrObj(Object obj) {
		if(obj == null || obj.toString().isEmpty()) {
			return "";
		}
		if(obj instanceof Double) {
			return String.valueOf(((Double)obj).intValue());
		}
		if(obj instanceof Float) {
			return String.valueOf(((Float)obj).intValue());
		}
		return obj.toString();
	}
}
