package com.pcl.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map.Entry;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import com.pcl.util.FileUtil;

public class PersonForThree {

	public static Document parsingXML(String xmlFilePath) {
		Document document = null;
		try {
			//创建解析器
			SAXReader reader=new SAXReader();//创建读取文件内容对象
			document=reader.read(xmlFilePath);//指定文件并读取

		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return document;
	}


	public static void cleanXml(String path) {

		File files[] = new File(path).listFiles();

		for(File file : files) {
			System.out.println("deal file=" + file.getAbsolutePath());

			dealAXml(file);
		}

		for(Entry<Integer,Integer> entry : rationMap.entrySet()) {
			System.out.println(entry.getKey() + " : " + entry.getValue());
		}

	}

	private static int getInt(String numberValue) {
		try {
			return  Integer.parseInt(numberValue);
		}catch(java.lang.NumberFormatException e) {
			return (int)Double.parseDouble(numberValue);
		}
	}

	private static HashMap<Integer,Integer> rationMap = new HashMap<>();

	public static void dealAXml(File file) {
		Document docFile = parsingXML(file.getAbsolutePath());

		Element readRoot = docFile.getRootElement();

		Element readSize = readRoot.element("size");

		Document doc  = DocumentHelper.createDocument();
		Element writerRoot = doc.addElement("annotation");

		String imgName = readRoot.elementText("filename");

		String srcImgName = file.getParentFile().getParentFile().getAbsolutePath() + File.separator + "JPEGImages" + File.separator + imgName;

		if(!new File(srcImgName).exists()) {
			System.out.println("The img not exists. path=" + srcImgName);
			return;
		}

		writerRoot.addElement("filename").addText(readRoot.elementText("filename"));	

		Element sizeEle = writerRoot.addElement("size");
		sizeEle.addElement("width").addText(readSize.elementText("width"));
		sizeEle.addElement("height").addText(readSize.elementText("height"));
		sizeEle.addElement("depth").addText(readSize.elementText("depth"));

		boolean isRetain = false;

		int length = readRoot.nodeCount();
		for(int i=0; i<length; i++) {
			Node node= readRoot.node(i);

			if(node instanceof Element){

				if(node.getName().equals("object")) {
					Element nodeEle = (Element)node;
					String name = nodeEle.elementText("name");
					if(!name.equals("person")) {
						continue;
					}
					isRetain = true;
					Element bndbox = nodeEle.element("bndbox");
					String xminStr = bndbox.elementText("xmin");
					String yminStr = bndbox.elementText("ymin");
					String xmaxStr = bndbox.elementText("xmax");
					String ymaxStr = bndbox.elementText("ymax");

					int xmin = getInt(xminStr);
					int ymin = getInt(yminStr);
					int xmax = getInt(xmaxStr);
					int ymax = getInt(ymaxStr);

					countRatio(xmax - xmin,  ymax - ymin);

					Element object = writerRoot.addElement("object");
					object.addElement("name").addText(name);

					if(nodeEle.element("pose") != null) {
						object.addElement("pose").addText(nodeEle.elementText("pose"));
					}
					if(nodeEle.element("truncated") != null) {
						object.addElement("truncated").addText(nodeEle.elementText("truncated"));
					}else {
						object.addElement("truncated").addText("0");
					}
					if(nodeEle.element("difficult") != null) {
						object.addElement("difficult").addText(nodeEle.elementText("difficult"));
					}else {
						object.addElement("difficult").addText("0");
					}


					Element bndboxEle = object.addElement("bndbox");
					bndboxEle.addElement("xmin").addText(String.valueOf(xmin));
					bndboxEle.addElement("ymin").addText(String.valueOf(ymin));
					bndboxEle.addElement("xmax").addText(String.valueOf(xmax));
					bndboxEle.addElement("ymax").addText(String.valueOf(ymax));

				}
			}
		}

		//		if(isRetain) {
		//			String filePath = file.getParentFile().getParentFile().getAbsolutePath() + File.separator + "new" + File.separator; 
		//			String xmlPath =  filePath + "Annotations" + File.separator + file.getName();
		//			
		//			File xmlPathFile = new File(xmlPath);
		//			if(!xmlPathFile.getParentFile().exists()) {
		//				xmlPathFile.getParentFile().mkdirs();
		//			}
		//			
		//			try(Writer out = new PrintWriter(xmlPath, "utf-8")){
		//				OutputFormat format = new OutputFormat("\t", true);
		//				format.setTrimText(true);//去掉原来的空白(\t和换行和空格)！
		//				XMLWriter writer = new XMLWriter(out, format);
		//				// 把document对象写到out流中。
		//				writer.write(doc);
		//				writer.close();
		//			} catch (FileNotFoundException | UnsupportedEncodingException e) {
		//				e.printStackTrace();
		//			} catch (IOException e) {
		//				e.printStackTrace();
		//			};
		//	
		//		}
	}


	private static void countRatio(int width,int height) {
		double ration = height / width * 1.0d;
		ration *= 10;

		int tmp = (int) ration;

		if(rationMap.containsKey(tmp)) {
			int value = rationMap.get(tmp);
			value++;
			rationMap.put(tmp, value);
		}else {
			rationMap.put(tmp, 1);
		}
	}


	public static void copyBusXmlAndImage(String path,String destPath) {

		File files[] = new File(path).listFiles();
		if(!new File(destPath).exists()) {
			new File(destPath).mkdir();
		}
		int count = 0;
		for(File file : files) {
			if(file.getName().startsWith("image-") ||
					file.getName().startsWith("2image-")
					|| file.getName().startsWith("3image-")
					|| file.getName().startsWith("4image-")
					) {
				count ++;
				System.out.println("deal file=" + file.getAbsolutePath());
				FileUtil.copyFile(file.getAbsolutePath(), destPath + File.separator + file.getName());

			}
		}
		System.out.println(count);
	}


	public static void main(String[] args) {
		String filePath = "D:\\BaiduNetdiskDownload\\Pedestrain\\VOCdevkit\\VOC2007\\Annotations";
		String destPath = "D:\\BaiduNetdiskDownload\\Pedestrain\\VOCdevkit\\VOC2007\\Annotations_bus";
		copyBusXmlAndImage(filePath,destPath);

		filePath = "D:\\BaiduNetdiskDownload\\Pedestrain\\VOCdevkit\\VOC2007\\JPEGImages";
		destPath = "D:\\BaiduNetdiskDownload\\Pedestrain\\VOCdevkit\\VOC2007\\JPEGImages_bus";
		copyBusXmlAndImage(filePath,destPath);
		//dealAXml(new File(filePath));

	}
}
