package com.pcl.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.util.Strings;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pcl.constant.Constants;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.service.ObjectFileService;

@Service
public class VocAnnotationsUtil {

	private static Logger logger = LoggerFactory.getLogger(VocAnnotationsUtil.class);

	@Autowired
	private ObjectFileService fileService;

	//[{"class_name":"car","id":"1","box":["171","239","225","272"],"score":"0.75"},{"class_name":"car","id":"7","box":["246","233","330","278"],"score":"0.92"},{"class_name":"car","id":"13","box":["76","467","189","527"],"score":"0.46"}]

	public LabelTaskItem readLabelInfoFromXmlDocument(String filePath) {

		try(FileInputStream in = new FileInputStream(filePath)) {
			return readLabelInfoFromXmlDocument(in);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}


	public LabelTaskItem readLabelInfoFromXmlDocument(InputStream in) {

		SAXReader reader = new SAXReader();
		Document document;
		try {
			document = reader.read(in);
			Element root = document.getRootElement();

			LabelTaskItem item = new LabelTaskItem();
			item.setItem_add_time(TimeUtil.getCurrentTimeStr());

			Element size = root.element("size");
			String width = size.elementText("width");
			String height = size.elementText("height");
			item.setPic_object_name(width + "," + height);

			List<?> eleList = root.elements("object");
			ArrayList<Map<String,Object>> labelList = new ArrayList<>();
			int count = 1;
			for(Object obj : eleList) {

				if(obj instanceof Element) {
					Map<String,Object> label = new HashMap<>();
					labelList.add(label);
					Element ele = (Element)obj;

					Iterator<Element> iter = ele.elementIterator();
					while(iter.hasNext()) {
						Element tmpEle = iter.next();
						if(tmpEle.getName().equals("name")) {
							label.put("class_name", tmpEle.getText());
						}else if(tmpEle.getName().equals("bndbox")) {
							List<String> box = new ArrayList<>();
							box.add(tmpEle.elementText("xmin"));
							box.add(tmpEle.elementText("ymin"));
							box.add(tmpEle.elementText("xmax"));
							box.add(tmpEle.elementText("ymax"));
							label.put("box", box);
						}else if(tmpEle.getName().equals("segmentation")) {
							String seg = tmpEle.getText();
							String segs[] = seg.split(",");
							List<Object> mask = new ArrayList<>();
							for(String segEle : segs) {
								mask.add(segEle);
							}
							label.put("mask", mask);
						}else if(tmpEle.getName().equals("keypoints")) {
							String seg = tmpEle.getText();
							String segs[] = seg.split(",");
							List<Object> keypoints = new ArrayList<>();
							for(String segEle : segs) {
								keypoints.add(segEle);
							}
							label.put("keypoints", keypoints);
						}
						else if(tmpEle.getName().equals("id")) {
							label.put("id", tmpEle.getText());
						}else if(tmpEle.getName().equals("other")) {
							if(tmpEle.element("region_attributes") != null) {
								Map<String,Object> other = new HashMap<>();
								label.put("other", other);
								Map<String,Object> region_attributes = new HashMap<>();
								other.put("region_attributes", region_attributes);
								
								Element region_attributesEle = tmpEle.element("region_attributes");
								Iterator<Element> regionIter = region_attributesEle.elementIterator();
								while(regionIter.hasNext()) {
									Element regionTmpEle = regionIter.next();
									region_attributes.put(regionTmpEle.getName(), regionTmpEle.getText());
								}
							}
						}
						else if(tmpEle.getName().equals("region_attributes")) {
							String text = tmpEle.getText();
							if(text.trim().startsWith("{") && text.trim().endsWith("}")) {
								Map<String,Object> map = JsonUtil.getMap(text);
								Map<String,Object> other = new HashMap<>();
								label.put("other", other);
								Map<String,Object> region_attributes = new HashMap<>();
								other.put("region_attributes", region_attributes);
								region_attributes.putAll(map);
							}
						}
						else {
							label.put(tmpEle.getName(), tmpEle.getText());
						}
					}
					if(!label.containsKey("id")) {
						label.put("id", String.valueOf(count++));
					}
				}

			}
			item.setLabel_info(JsonUtil.toJson(labelList));

			return item;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	
	public Document getXmlDocument(ArrayList<Map<String,Object>> labelList,Map<String,Object> typeOrColorMapName,String imagePath) {

		if(labelList.isEmpty()) {
			return null;
		}
		String relativeFileName = imagePath.replace("\\", "/");
		String fileName = relativeFileName.substring(relativeFileName.lastIndexOf("/") +1);

		Map<String, Object> imageInfo = getImageFileInfo(imagePath);

		return produceDocment(typeOrColorMapName, labelList, fileName, imageInfo);
		
	}


	private Document produceDocment(Map<String, Object> typeOrColorMapName, ArrayList<Map<String, Object>> labelList,
			String fileName, Map<String, Object> imageInfo) {
		logger.info("write xml file start." + fileName);
		Document doc  = DocumentHelper.createDocument();
		Element root = doc.addElement("annotation");

		root.addElement("filename").addText(fileName);

		Element source = root.addElement("source");
		source.addElement("database").addText("The VOC2007 Database");
		source.addElement("annotation").addText("PASCAL VOC2007");
		source.addElement("image").addText("pcl");
		source.addElement("flickrid").addText("330391518");
		
		Element size = root.addElement("size");
		size.addElement("width").addText(String.valueOf(imageInfo.get("width")));
		size.addElement("height").addText(String.valueOf(imageInfo.get("height")));
		size.addElement("depth").addText(String.valueOf(3));

		for(Map<String,Object> label : labelList) {

			String className = CocoAnnotationsUtil.getClassName(label);
			if(className == null) {
				logger.info("may be error." + label.toString());
				continue;
			}

			Element objEle = root.addElement("object");
			objEle.addElement("name").addText(className);

			if(typeOrColorMapName.get("type") != null) {
				Map<String,Object> typeMap = (Map<String,Object>)typeOrColorMapName.get("type");
				if(typeMap.containsKey(className)) {
					objEle.addElement("category_name").addText(String.valueOf(typeMap.get(className)));
				}
			}

			List<Object> boxList = (List<Object>)label.remove("box");
			if(boxList != null) {
				Element bndBoxEle = objEle.addElement("bndbox");
				bndBoxEle.addElement("xmin").addText(getIntStr(String.valueOf(boxList.get(0))));
				bndBoxEle.addElement("ymin").addText(getIntStr(String.valueOf(boxList.get(1))));
				bndBoxEle.addElement("xmax").addText(getIntStr(String.valueOf(boxList.get(2))));
				bndBoxEle.addElement("ymax").addText(getIntStr(String.valueOf(boxList.get(3))));
			}
			Object maskObj = label.remove("mask");
			if(maskObj != null) {
				Element segmentationEle = objEle.addElement("segmentation");
				if(maskObj instanceof List) {
					segmentationEle.addText(getListStr((List<Object>)maskObj));
				}else {
					segmentationEle.addText(maskObj.toString());
				}
			}
			Object keypointsObj = label.remove("keypoints");
			if(keypointsObj != null) {
				Element keypointsEle = objEle.addElement("keypoints");
				if(keypointsObj instanceof List) {
					keypointsEle.addText(getListStr((List<Object>)keypointsObj));
				}else {
					keypointsEle.addText(keypointsObj.toString());
				}
			}

			Object score = label.remove("score");
			if(score != null) {
				objEle.addElement("truncated").addText("0");
				double scoreD = Double.valueOf(score.toString()); 
				if(scoreD > 0.8) {
					objEle.addElement("difficult").addText("0");
				}else {
					objEle.addElement("difficult").addText("1");
				}
			}else {
				objEle.addElement("truncated").addText("0");
				objEle.addElement("difficult").addText("0");
			}

			addEleToDoc(label, objEle,typeOrColorMapName);
		}

		return doc;
	}
	

	public Document getXmlDocument(LabelTaskItem item,Map<String,Object> pictureInfo,Map<String,Object> typeOrColorMapName) {

		String labelInfo = item.getLabel_info();

		ArrayList<Map<String,Object>> labelList = JsonUtil.getLabelList(labelInfo);
		if(labelList.isEmpty()) {
			return null;
		}
		String relativeFileName = item.getPic_image_field();
		String fileName = relativeFileName.substring(relativeFileName.lastIndexOf("/") +1);
		Map<String, Object> imageInfo = getImageFileInfo(item);
		
		return produceDocment(typeOrColorMapName, labelList, fileName, imageInfo);
	}

	private void addEleToDoc(Map<String,Object> label,Element objEle,Map<String,Object> typeOrColorMapName) {
		for(Entry<String,Object> tmp : label.entrySet()) {
			String key = tmp.getKey();
			if(key.equals("other")) {
				Object other = tmp.getValue();
				Element otherEle = objEle.addElement("other");
				if(!CocoAnnotationsUtil.isEmpty(other) && other instanceof Map) {
					addEleToDoc((Map<String,Object>)other, otherEle, typeOrColorMapName);
				}else if(!CocoAnnotationsUtil.isEmpty(other)) {
					objEle.addElement("other").addText(tmp.getValue().toString());
				}
			}else {
				if(key.equals("color")) {
					String color = String.valueOf(tmp.getValue());
					if(typeOrColorMapName.get("color") != null) {
						Map<String,Object> typeMap = (Map<String,Object>)typeOrColorMapName.get("color");
						if(typeMap.containsKey(color)) {
							objEle.addElement("color_name").addText(String.valueOf(typeMap.get(color)));
						}
					}
				}
				if(!CocoAnnotationsUtil.isEmpty(tmp.getValue()) && tmp.getValue() instanceof Map) {
					Element keyEle = objEle.addElement(key);
					addEleToDoc((Map<String,Object>)tmp.getValue(), keyEle, typeOrColorMapName);

				}else {
					objEle.addElement(key).addText(tmp.getValue().toString());
				}
			}
		}
	}

	private  Map<String, Object> getImageFileInfo(LabelTaskItem item) {

		Map<String,Object> imageMap = new HashMap<>();
		String widthHeigth = item.getPic_object_name();
		if(widthHeigth == null || widthHeigth.indexOf(",") != -1) {
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

		return imageMap;
	}

	private  Map<String, Object> getImageFileInfo(String imagePath) {

		Map<String,Object> imageMap = new HashMap<>();
		imageMap.put("width", 0);
		imageMap.put("height", 0);
		
		try(InputStream inputStream = new FileInputStream(imagePath)){
			BufferedImage sourceImg =ImageIO.read(inputStream);
			int width = sourceImg.getWidth();
			int height = sourceImg.getHeight();
			imageMap.put("width", width);
			imageMap.put("height", height);
		}catch (Exception e) {
			e.printStackTrace();
			logger.info("image not exist. absolute path=" + imagePath,e);
		}
		
		return imageMap;
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


	private String getIntStr(String doubleStr) {
		int index = doubleStr.indexOf(".");
		if(index != -1) {
			return doubleStr.substring(0,index);
		}
		return doubleStr;
	}
	
	
	
	public String getXmlPathByImagePath(String imagePath) {
		File imgFile = new File(imagePath);
		File parentFile = imgFile.getParentFile();
		String fileName = imgFile.getName();
		String xmlFileName = fileName.substring(0,fileName.lastIndexOf(".")) + ".xml";
		
		return parentFile.getParent() + File.separator + Constants.ANNOTATIONS + File.separator +  xmlFileName;
		
	}
	
	

	public static void main(String[] args) {
		String path = "D:\\2020文档\\问题定位\\0806\\5655665-人工标注_label (1)\\xml";
		VocAnnotationsUtil vocUtil = new VocAnnotationsUtil();
		File file = new File(path);

		File files[] = file.listFiles();
		ArrayList<LabelTaskItem> itemList = new ArrayList<>();
		for(File tmp : files) {
			LabelTaskItem item = vocUtil.readLabelInfoFromXmlDocument(tmp.getAbsolutePath());
			item.setPic_image_field("/minio/" + tmp.getName());
			itemList.add(item);
		}
		CocoAnnotationsUtil cocoUtil = new CocoAnnotationsUtil();
		Map<String,Object> cocoJson = cocoUtil.getTmpCocoJson(itemList);

		System.out.println(JsonUtil.toJson(cocoJson));
	}

}
