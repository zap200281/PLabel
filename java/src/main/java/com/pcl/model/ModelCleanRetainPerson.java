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

public class ModelCleanRetainPerson {

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
	}


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
		    	   if(!name.equals("car")) {
		    		   continue;
		    	   }
		    	   isRetain = true;
		    	   Element bndbox = nodeEle.element("bndbox");
		    	   String xminStr = bndbox.elementText("xmin");
		    	   String yminStr = bndbox.elementText("ymin");
		    	   String xmaxStr = bndbox.elementText("xmax");
		    	   String ymaxStr = bndbox.elementText("ymax");
		    	   	   
		    	   Element object = writerRoot.addElement("object");
		    	   object.addElement("name").addText(name);
		    	   
		           object.addElement("truncated").addText("0");
		    	  
		    	   object.addElement("difficult").addText("0");
		    	   
		    	   Element bndboxEle = object.addElement("bndbox");
		    	   bndboxEle.addElement("xmin").addText(xminStr);
		    	   bndboxEle.addElement("ymin").addText(yminStr);
		    	   bndboxEle.addElement("xmax").addText(xmaxStr);
		    	   bndboxEle.addElement("ymax").addText(ymaxStr);
		    	   
		       }
		    }
		}
		
		if(isRetain) {
			//String filePath = file.getParentFile().getParentFile().getAbsolutePath() + File.separator + "new" + File.separator; 
			String xmlPath =  file.getAbsolutePath();
			
			File xmlPathFile = new File(xmlPath);
			if(!xmlPathFile.getParentFile().exists()) {
				xmlPathFile.getParentFile().mkdirs();
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
			
			//拷贝图片
			
//			String destImgName = filePath + "JPEGImages" + File.separator + imgName;
//			File destImgNameFile = new File(destImgName);
//			if(!destImgNameFile.getParentFile().exists()) {
//				destImgNameFile.getParentFile().mkdirs();
//			}
//			
//		
//			
//			try {
//				Files.copy(new File(srcImgName).toPath(), destImgNameFile.toPath());
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
			
		}
	}
	
	public static void main(String[] args) {
		String filePath = "D:\\tmp";
		
		cleanXml(filePath);
		
		//dealAXml(new File(filePath));
		
	}
}
