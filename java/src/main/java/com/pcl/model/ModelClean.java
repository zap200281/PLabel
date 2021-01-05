package com.pcl.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class ModelClean {

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
		int total = 0;
		for(File file : files) {

			int count = dealAXml(file);
			if(count == 0) {
				total++;
				//file.delete();
			}

		}
		System.out.println("total=" + total);
	}


	private static int getInt(String numberValue) {
		try {
			return  Integer.parseInt(numberValue);
		}catch(java.lang.NumberFormatException e) {
			return (int)Double.parseDouble(numberValue);
		}
	}

	public static int dealAXml(File file) {
		Document docFile = parsingXML(file.getAbsolutePath());

		Element readRoot = docFile.getRootElement();


		int width = -1;
		int height = -1;

		Element readSize = readRoot.element("size");


		width = Integer.parseInt(readSize.elementText("width"));
		height = Integer.parseInt(readSize.elementText("height"));

		Document doc  = DocumentHelper.createDocument();
		Element writerRoot = doc.addElement("annotation");


		writerRoot.addElement("filename").addText(readRoot.elementText("filename"));	

		Element sizeEle = writerRoot.addElement("size");
		sizeEle.addElement("width").addText(readSize.elementText("width"));
		sizeEle.addElement("height").addText(readSize.elementText("height"));
		sizeEle.addElement("depth").addText(readSize.elementText("depth"));

		int count = 0;

		int length = readRoot.nodeCount();
		for(int i=0; i<length; i++) {
			Node node= readRoot.node(i);


			if(node instanceof Element){

				if(node.getName().equals("object")) {
					Element nodeEle = (Element)node;
					String name = nodeEle.elementText("name");
					if(! name.equals("pedestrian")) {
						continue;
					}
					count++;
					Element bndbox = nodeEle.element("bndbox");
					String xminStr = bndbox.elementText("xmin");
					String yminStr = bndbox.elementText("ymin");
					String xmaxStr = bndbox.elementText("xmax");
					String ymaxStr = bndbox.elementText("ymax");

					int xmin = getInt(xminStr);
					int ymin = getInt(yminStr);
					int xmax = getInt(xmaxStr);
					int ymax = getInt(ymaxStr);

					if( xmax >= width || ymax >= height) {
						System.out.println(file.getName() + "  xmin=" + xmin + " xmax=" + xmax + " ymin=" + ymin + " ymax=" + ymax + " width=" +width + " height=" + height);
						if(xmax >= width) {
							xmax = width -1;
						}
						if(ymax >= height) {
							ymax = height - 1;
						}

					}

					if(xmin <= 0 || ymin <= 0 | xmin >= xmax || ymin >= ymax) {


						System.out.println(file.getName() + "  xmin=" + xmin + " xmax=" + xmax + " ymin=" + ymin + " ymax=" + ymax + " width=" +width + " height=" + height);
						if(xmin <= 0) {
							xmin = 1;
						}
						if(ymin <= 0) {
							ymin = 1;
						}

						if(xmin >= xmax) {
							continue;
						}
						if(ymin >=ymax) {
							continue;
						}

					}


					Element object = writerRoot.addElement("object");
					object.addElement("name").addText("person");

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

		String xmlPath =  file.getParentFile().getParentFile().getAbsolutePath() + File.separator + "bak" + File.separator + file.getName();
		File destPath = new File(xmlPath);
		if(!destPath.exists()) {
			destPath.getParentFile().mkdirs();
		}
		try(Writer out = new PrintWriter(xmlPath, "utf-8")){
			OutputFormat format = new OutputFormat("\t", true);
			format.setTrimText(true);//去掉原来的空白(\t和换行和空格)！
			XMLWriter writer = new XMLWriter(out, format);
			// 把document对象写到out流中。
			writer.write(doc);
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		};

		return count;
	}

	public static void main(String[] args) {
		String filePath = "D:\\code\\new\\Pedestrian-Detection\\annotations\\xmls";

		cleanXml(filePath);

		//dealAXml(new File(filePath));

	}
}
