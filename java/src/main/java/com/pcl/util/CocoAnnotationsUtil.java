package com.pcl.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.pcl.dao.ClassManageDao;
import com.pcl.pojo.mybatis.ClassManage;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.service.MinioFileService;
import com.pcl.service.ObjectFileService;

/**
   *   将标注信息写成coco格式
 * @author 邹安平
 *
 */
@Service
public class CocoAnnotationsUtil {
	
	@Autowired
	private ObjectFileService fileService;
	
	@Autowired
	private ClassManageDao classManageDao;
	
	private Gson gson = new Gson();

	//instances_train2017.json
	public Map<String,Object> getCocoJson(List<LabelTaskItem> itemList){
		
		List<ClassManage> allClass = classManageDao.queryAll();
		int maxClassId = getMaxClassId(allClass);
		Map<String,Integer> classMapId = getClassMapId(allClass);
		Map<String,String> classMapSuperClass = getClassMapSuperClass(allClass);
		
		Map<String,Object> re = new HashMap<>();
		re.put("type","instances");
		List<Map<String,Object>> imageList = new ArrayList<>();
		
		Map<String,Map<String,Object>> categories = new HashMap<>();
		List<Map<String,Object>> annotationList = new ArrayList<>();
		
		long time = System.nanoTime();
		time = time % 1000;
		time = time * 100000;
		int id = 0;
		int annid = 1;
		for(LabelTaskItem item : itemList) {
			id ++;
			String labelInfo = item.getLabel_info();
			if(Strings.isBlank(labelInfo)) {
				continue;
			}
			ArrayList<Map<String,Object>> labelList = JsonUtil.getLabelList(labelInfo);
			if(labelList.isEmpty()) {
				continue;
			}
			
			long imageId = getId(time, id);
			Map<String, Object> imageMap = getImageFileInfo(imageId, item);
			imageList.add(imageMap);
			
			for(Map<String,Object> label : labelList) {
				Map<String,Object> annotation = new HashMap<>();
				annotation.put("id", annid);
				annotation.put("image_id", imageId);
				
			
				String className = getClassName(label);
				if(Strings.isBlank(className)) {
					continue;
				}
				//String className = getClassName(label.get("class_name").toString());
				String superClassName = getStrOrNull(label.remove("super_class"));
				
				if(!classMapId.containsKey(className)) {
					maxClassId++;
					newACategory(maxClassId, classMapId, classMapSuperClass, className, superClassName);
				}
				int categoryId = classMapId.get(className);
				
				if(!categories.containsKey(className)) {
					Map<String,Object> categoryMap = new HashMap<>();
					categoryMap.put("name", className);
					categoryMap.put("id", classMapId.get(className));
					categoryMap.put("supercategory", classMapSuperClass.get(className));
					
					categories.put(className, categoryMap);
				}
				
				annotation.put("category_id", categoryId);
				List<Object> boxList = (List<Object>)label.remove("box");
				if(boxList != null) {//矩形标注
					int xmin = getIntStr(String.valueOf(boxList.get(0)));
					int ymin = getIntStr(String.valueOf(boxList.get(1)));
					int xmax = getIntStr(String.valueOf(boxList.get(2)));
					int ymax = getIntStr(String.valueOf(boxList.get(3)));
					
					List<Integer> bbox = new ArrayList<>();
					bbox.add(xmin);
					bbox.add(ymin);
					int o_width = Math.abs(xmax - xmin);
					int o_height = Math.abs(ymax - ymin);
					bbox.add(o_width);
					bbox.add(o_height);
					
					annotation.put("bbox", bbox);
					annotation.put("area", o_width * o_height);
				}
				
				List<Object> maskList = (List<Object>)label.remove("mask");
				if(maskList == null) {
					maskList = (List<Object>)label.remove("segmentation");
				}
				if(maskList != null) {
					//多边形标注
					annotation.put("segmentation", maskList);
					annotation.put("area", caculateArea(maskList));
				}else {
					annotation.put("segmentation", new ArrayList<>());
				}
				
				//点标注，只支持一个点
				List<Object> keypointsList = (List<Object>)label.remove("keypoints");
				if(keypointsList != null) {
					annotation.put("keypoints", keypointsList);
					annotation.put("num_keypoints", 1);
				}
			
				annotation.put("iscrowd", 0);
				annotation.put("ignore", 0);
				
				//addEleToMap(label, annotation);
			
				annotationList.add(annotation);
				annid++;
			}
		}
		
		
		
		List<Map<String,Object>> categoriesList = new ArrayList<>();
		for(Entry<String,Map<String,Object>> entry : categories.entrySet()) {
			categoriesList.add(entry.getValue());
		}
		
		re.put("images", imageList);
		re.put("categories", categoriesList);
		re.put("annotations", annotationList);
		
		return re;
	}
	
	public  Map<String,Object> getTmpCocoJson(List<LabelTaskItem> itemList){
		
		List<ClassManage> allClass = new ArrayList<>();
		int maxClassId = -1;
		Map<String,Integer> classMapId = getClassMapId(allClass);
		Map<String,String> classMapSuperClass = getClassMapSuperClass(allClass);
		
		Map<String,Object> re = new HashMap<>();
		re.put("type","instances");
		List<Map<String,Object>> imageList = new ArrayList<>();
		
		Map<String,Map<String,Object>> categories = new HashMap<>();
		List<Map<String,Object>> annotationList = new ArrayList<>();
		
		long time = System.nanoTime();
		time = time % 1000;
		time = time * 100000;
		int id = 0;
		int annid = 1;
		for(LabelTaskItem item : itemList) {
			id ++;
			String labelInfo = item.getLabel_info();
			if(Strings.isBlank(labelInfo)) {
				continue;
			}
			ArrayList<Map<String,Object>> labelList = JsonUtil.getLabelList(labelInfo);
			if(labelList.isEmpty()) {
				continue;
			}
			
			long imageId = getId(time, id);
			Map<String, Object> imageMap = getImageFileInfo(imageId, item);
			imageList.add(imageMap);
			
			for(Map<String,Object> label : labelList) {
				Map<String,Object> annotation = new HashMap<>();
				annotation.put("id", annid);
				annotation.put("image_id", imageId);
				
			
				String className = getClassName(label);
				if(Strings.isBlank(className)) {
					continue;
				}
				//String className = getClassName(label.get("class_name").toString());
				String superClassName = getStrOrNull(label.remove("super_class"));
				
				if(!classMapId.containsKey(className)) {
					maxClassId++;
					newACategory(maxClassId, classMapId, classMapSuperClass, className, superClassName);
				}
				int categoryId = classMapId.get(className);
				
				if(!categories.containsKey(className)) {
					Map<String,Object> categoryMap = new HashMap<>();
					categoryMap.put("name", className);
					categoryMap.put("id", classMapId.get(className));
					categoryMap.put("supercategory", "none");
					
					categories.put(className, categoryMap);
				}
				
				annotation.put("category_id", categoryId);
				
				List<Object> boxList = (List<Object>)label.remove("box");
				if(boxList != null) {//矩形标注
					int xmin = getIntStr(String.valueOf(boxList.get(0)));
					int ymin = getIntStr(String.valueOf(boxList.get(1)));
					int xmax = getIntStr(String.valueOf(boxList.get(2)));
					int ymax = getIntStr(String.valueOf(boxList.get(3)));
					
					List<Integer> bbox = new ArrayList<>();
					bbox.add(xmin);
					bbox.add(ymin);
					int o_width = Math.abs(xmax - xmin);
					int o_height = Math.abs(ymax - ymin);
					bbox.add(o_width);
					bbox.add(o_height);
					
					annotation.put("bbox", bbox);
					annotation.put("area", o_width * o_height);
				}
				
				List<Object> maskList = (List<Object>)label.remove("mask");
				if(maskList == null) {
					maskList = (List<Object>)label.remove("segmentation");
				}
				if(maskList != null) {
					//多边形标注
					annotation.put("segmentation", maskList);
					annotation.put("area", caculateArea(maskList));
				}else {
					annotation.put("segmentation", new ArrayList<>());
				}
				
				//点标注，只支持一个点
				List<Object> keypointsList = (List<Object>)label.remove("keypoints");
				if(keypointsList != null) {
					annotation.put("keypoints", keypointsList);
					annotation.put("num_keypoints", 1);
				}
			
				annotation.put("iscrowd", 0);
				annotation.put("ignore", 0);
				
				//addEleToMap(label, annotation);
			
				annotationList.add(annotation);
				annid++;
			}
		}
		
		List<Map<String,Object>> categoriesList = new ArrayList<>();
		for(Entry<String,Map<String,Object>> entry : categories.entrySet()) {
			categoriesList.add(entry.getValue());
		}
		
		re.put("images", imageList);
		re.put("categories", categoriesList);
		re.put("annotations", annotationList);
		
		return re;
	}
	
	public static void addEleToMap(Map<String,Object> label,Map<String,Object> annotation) {
		for(Entry<String,Object> tmp : label.entrySet()) {
			String key = tmp.getKey();
			if(key.equals("other")) {
				Object other = tmp.getValue();
				if(!isEmpty(other) && other instanceof Map) {
					Map<String,Object> otherMap = (Map<String,Object>)other;
					if(otherMap.get("region_attributes") != null && otherMap.get("region_attributes") instanceof Map) {
						Map<String,Object> region_attributesMap = (Map<String,Object>)otherMap.get("region_attributes");
						for(Entry<String,Object> tmpEle : region_attributesMap.entrySet()) {
							annotation.put(key, tmp.getValue());
						}
					}
				}else if(!isEmpty(other)) {
					annotation.put(key, tmp.getValue());
				}
			}else {
				if(!isEmpty(tmp.getValue())) {
					annotation.put(key, tmp.getValue());
				}
			}
		}
	}
	
	public static boolean isEmpty(Object obj) {
		if(obj == null) {
			return true;
		}
		if(obj.toString().isEmpty()) {
			return true;
		}
		return false;
	}

	public static String getClassName(Map<String, Object> label) {
		Object obj = label.remove("class_name");
		if(obj != null && !Strings.isEmpty(obj.toString())) {
			return obj.toString();
		}else {
			Object other = label.get("other");
			if(other != null) {
				Map<String,Object> otherMap = (Map<String,Object>)other;
				Object tmpObj = otherMap.remove("type");
				if(tmpObj != null && !Strings.isEmpty(tmpObj.toString())) {
					return tmpObj.toString();
				}
			}
		}
		return null;
	}

	
	public static String getStrValue(Map<String, Object> label,String key) {
		Object obj = label.get(key);
		if(obj != null && !Strings.isEmpty(obj.toString())) {
			return obj.toString();
		}else {
			Object other = label.get("other");
			if(other != null) {
				Map<String,Object> otherMap = (Map<String,Object>)other;
				Object region_attributesObj = otherMap.get("region_attributes");
				if(region_attributesObj != null && region_attributesObj instanceof Map) {
					Map<String,Object> region_attributesMap = (Map<String,Object>)region_attributesObj;
					obj = region_attributesMap.get(key);
					if(obj != null && !Strings.isEmpty(obj.toString())) {
						return obj.toString();
					}
				}
			}
		}
		return null;
	}
	
	
	public static Object getObjValue(Map<String, Object> label,String key) {
		Object obj = label.get(key);
		if(obj != null) {
			return obj;
		}else {
			Object other = label.get("other");
			if(other != null) {
				Map<String,Object> otherMap = (Map<String,Object>)other;
				Object region_attributesObj = otherMap.get("region_attributes");
				if(region_attributesObj != null && region_attributesObj instanceof Map) {
					Map<String,Object> region_attributesMap = (Map<String,Object>)region_attributesObj;
					return region_attributesMap.get(key);
				}
			}
		}
		return null;
	}
	
	public static float caculateArea(List<Object> maskList) {
		Point vertex[] = new Point[maskList.size() / 2];
		for(int i = 0; i < maskList.size(); i += 2) {
			Point tmpPoint = new Point();
			tmpPoint.x = getIntStr(String.valueOf(maskList.get(i)));
			tmpPoint.y = getIntStr(String.valueOf(maskList.get(i + 1)));
			vertex[i/2] = tmpPoint;
		}
		
		return caculate(vertex, vertex.length);
	}
	
	public static int getIntStr(String doubleStr) {
		int index = doubleStr.indexOf(".");
		if(index != -1) {
			doubleStr = doubleStr.substring(0,index);
		}
		return Integer.parseInt(doubleStr);
	}
	
	public static float caculate(Point vertex[],int pointNum){
    	int i=0;
    	float temp=0;
    	for(;i<pointNum-1;i++)
    	{
    		temp+=(vertex[i].x-vertex[i+1].x)*(vertex[i].y+vertex[i+1].y);
    	}
    	temp+=(vertex[i].x-vertex[0].x)*(vertex[i].y+vertex[0].y);
    	return temp/2;
    }

	
	
	private void newACategory(int maxClassId, Map<String, Integer> classMapId, Map<String, String> classMapSuperClass,
			String className, String superClassName) {
		
		classMapId.put(className, maxClassId);
		if(superClassName != null) {
			classMapSuperClass.put(className, superClassName);
		}else {
			classMapSuperClass.put(className, className);
		}
//		ClassManage classManage = new ClassManage();
//		classManage.setId(maxClassId);
//		classManage.setClass_name(className);
//		classManage.setSuper_class_name(superClassName);
//		classManageDao.addClassManage(classManage);

	}

	private String getStrOrNull(Object object) {
		if(object != null) {
			return object.toString();
		}
		return null;
	}

	private int getMaxClassId(List<ClassManage> allClass) {
		int max = -1;
		for(ClassManage clazz : allClass) {
			if(clazz.getId() > max) {
				max = clazz.getId();
			}
		}
		return max;
	}

	private Map<String, String> getClassMapSuperClass(List<ClassManage> allClass) {
		Map<String,String> re = new HashMap<>();
		for(ClassManage clazz : allClass) {
			if(!Strings.isEmpty(clazz.getSuper_class_name())) {
				re.put(clazz.getClass_name(), clazz.getSuper_class_name());
			}else {
				re.put(clazz.getClass_name(), clazz.getClass_name());
			}
		}
		return re;
	}

	private Map<String, Integer> getClassMapId(List<ClassManage> allClass) {
		Map<String,Integer> re = new HashMap<>();
		for(ClassManage clazz : allClass) {
			re.put(clazz.getClass_name(), clazz.getId());
		}
		return re;
	}

	private  Map<String, Object> getImageFileInfo(long imageId, LabelTaskItem item) {
		
		String fileName = item.getPic_image_field();
		fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
		Map<String,Object> imageMap = new HashMap<>();
		
		String widthHeigth = item.getPic_object_name();
		if(widthHeigth == null || widthHeigth.indexOf(",") == -1) {
			widthHeigth = fileService.getImageWidthHeight(item.getPic_image_field());
		}
		if(!Strings.isEmpty(widthHeigth)) {
			String tmp[] = widthHeigth.split(",");
			imageMap.put("width", Integer.parseInt(tmp[0]));
			imageMap.put("height", Integer.parseInt(tmp[1]));
		}else {
			imageMap.put("width", 0);
			imageMap.put("height", 0);
		}
		imageMap.put("id", imageId);
		imageMap.put("file_name", fileName);
		return imageMap;
	}


	private  long getId(long time, int id) {
		return id;
	}
	
	static class Point{
		public int x;
		
		public int y;
		
	}
	
	
	public static void main(String[] args) {
		List<Object> tmp = new ArrayList<>();

		tmp.add(174);
		tmp.add(139);
		tmp.add(282);
		tmp.add(139);
		tmp.add(282);
		tmp.add(366);
		tmp.add(174);
		tmp.add(366);
		
		System.out.println(caculateArea(tmp));
		
	}
}
