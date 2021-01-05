package com.pcl.control;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pcl.model.CreateTrainVal;
import com.pcl.pojo.Result;
import com.pcl.service.schedule.ThreadSchedule;
import com.pcl.util.FileUtil;
import com.pcl.util.JsonUtil;


import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/static")
public class TmpUtilController {
	private static Logger logger = LoggerFactory.getLogger(TmpUtilController.class);

	/**
	 * 将Path目录下所有xml文件中的src字符串替换成dest字符串
	 * @param labelTaskId
	 * @param src
	 * @param dest
	 * @return
	 */
	@ApiOperation(value="将Path目录下所有xml文件中的的行，满足src正则表达式替换成dest字符串", notes="")
	@RequestMapping(value ="/replace-str", method = RequestMethod.POST)
	public Result replace(@RequestParam("path") String path,@RequestParam("src") String src,@RequestParam("dest") String dest) {

		Result result = new Result();

		logger.info("path=" + path);
		logger.info("src=" + src + "  dest=" + dest);


		ThreadSchedule.execThread(new Runnable() {

			@Override
			public void run() {
				replaceFile(path, src, dest);
			}

			private void replaceFile(String path, String src, String dest) {
				File pathFile = new File(path);
				if(pathFile.isFile()) {
					if(pathFile.getName().toLowerCase().endsWith(".xml")){
						replaceAFile(pathFile, src, dest);
					}else {
						logger.info("not replace, file=" + path);
					}
				}else {
					File files[] = pathFile.listFiles();
					for(File file : files) {
						replaceFile(file.getAbsolutePath(), src, dest);
					}
				}
			}
		});


		result.setCode(0);
		result.setMessage("success");

		return result;

	}


	@ApiOperation(value="将Path目录下所有xml文件中的替换成车类型、颜色识别需要的数据", notes="")
	@RequestMapping(value ="/convertToCarColor", method = RequestMethod.POST)
	public Result convertToCarColor(@RequestParam("path") String srcPath,@RequestParam("destpath") String destPath) {
		Result result = new Result();

		logger.info("src=" + srcPath + "  dest=" + destPath);

		ThreadSchedule.execThread(new Runnable() {

			@Override
			public void run() {
				File pathFile = new File(srcPath);
				File files[] = pathFile.listFiles();
				for(File file : files) {
					replaceFile(file.getAbsolutePath(), destPath);
				}
			}

			private void replaceFile(String srcpath, String destpath) {

				SAXReader reader = new SAXReader();
				Document document;
				try {
					File srcFile = new File(srcpath);
					document = reader.read(new FileInputStream(new File(srcpath)));
					Element root = document.getRootElement();

					Document destDoc  = DocumentHelper.createDocument();
					Element destRoot = destDoc.addElement("annotation");

					destRoot.addElement("filename").addText(srcFile.getName());
					Element destsource = destRoot.addElement("source");
					destsource.addElement("database").addText("The VOC2007 Database");
					destsource.addElement("annotation").addText("PASCAL VOC2007");
					destsource.addElement("image").addText("pcl");
					destsource.addElement("flickrid").addText("330391518");

					Element size = root.element("size");
					String width = size.elementText("width");
					String height = size.elementText("height");

					Element destSize = destRoot.addElement("size");
					destSize.addElement("width").addText(width);
					destSize.addElement("height").addText(height);
					destSize.addElement("depth").addText(String.valueOf(3));


					List<?> eleList = root.elements("object");

					


					for(Object obj : eleList) {

						if(obj instanceof Element) {
							Element destObjEle = destRoot.addElement("object");

							Element ele = (Element)obj;

							Iterator<Element> iter = ele.elementIterator();
							while(iter.hasNext()) {
								Element tmpEle = iter.next();
								if(tmpEle.getName().equals("name")) {
									//label.put("class_name", tmpEle.getText());
									//
									destObjEle.addElement("name").addText(tmpEle.getText());

								}else if(tmpEle.getName().equals("bndbox")) {
									Element destbndbox = destObjEle.addElement("bndbox");
									destbndbox.addElement("xmin").addText(tmpEle.elementText("xmin"));
									destbndbox.addElement("ymin").addText(tmpEle.elementText("ymin"));
									destbndbox.addElement("xmax").addText(tmpEle.elementText("xmax"));
									destbndbox.addElement("ymax").addText(tmpEle.elementText("ymax"));

								}
								else if(tmpEle.getName().equals("id")) {
									destObjEle.addElement("id").addText(tmpEle.getText());

								}else if(tmpEle.getName().equals("other")) {
									if(tmpEle.element("region_attributes") != null) {

										Map<String,Object> region_attributes = new HashMap<>();

										Element region_attributesEle = tmpEle.element("region_attributes");
										Iterator<Element> regionIter = region_attributesEle.elementIterator();
										while(regionIter.hasNext()) {
											Element regionTmpEle = regionIter.next();
											if(regionTmpEle.getName().equals("type") || regionTmpEle.getName().equals("color") ) {
												String value = regionTmpEle.getText();
												try {
													int valueInt = Integer.parseInt(value);
													region_attributes.put(regionTmpEle.getName(), valueInt);
												}catch (Exception e) {
													e.printStackTrace();
												}
												
											}
										}
										destObjEle.addElement("region_attributes").addText(region_attributes.toString());
									}
								}

								else {
									destObjEle.addElement(tmpEle.getName()).addText(tmpEle.getText());

								}
							}

						}

					}


					File destFile = new File(destpath,srcFile.getName());

					FileWriter strWriter = new FileWriter(destFile);

					OutputFormat format = new OutputFormat("\t", true);
					format.setTrimText(true);//去掉原来的空白(\t和换行和空格)！

					XMLWriter writer = new XMLWriter(strWriter, format);
					// 把document对象写到out流中。
					writer.write(destDoc);

					writer.close();
					strWriter.close();

				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		});

		result.setCode(0);
		result.setMessage("success");

		return result;
	}

	private void replaceAFile(File file,String src,String dest) {

		logger.info("replace file: " + file.getAbsolutePath());

		List<String> allLineList = FileUtil.getAllLineList(file.getAbsolutePath(), "utf-8");

		for(int i = 0; i < allLineList.size(); i++) {
			String line = allLineList.get(i);

			line = line.replaceFirst(src, dest);

			allLineList.set(i, line);

		}

		try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"))){
			for(String line : allLineList) {
				writer.write(line);
				writer.newLine();
			}
		} catch (UnsupportedEncodingException e) {

			e.printStackTrace();
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}


	@ApiOperation(value="将Path目录下xml文件和jpg文件组成voc格式的数据集", notes="")
	@RequestMapping(value ="/createVocDataSet", method = RequestMethod.POST)
	public Result createVocDataSet(@RequestParam("srcpath") String srcpath,@RequestParam("dataSetPath") String dataSetPath) {

		Result result = new Result();

		logger.info("srcpath=" + srcpath + "  dataSetPath=" + dataSetPath);

		ThreadSchedule.execThread(new Runnable() {
			@Override
			public void run() {
				File srcFile = new File(srcpath);

				File jpegPath = new File(dataSetPath + "/JPEGImages");
				jpegPath.mkdirs();

				File annotationPath = new File(dataSetPath + "/Annotations");
				annotationPath.mkdirs();

				splitVocDataSet(dataSetPath, srcFile);
			}

			private void splitVocDataSet(String dataSetPath, File srcFile) {
				if(srcFile.isFile()) {
					if(srcFile.getName().endsWith(".jpg") || srcFile.getName().endsWith(".jpeg")) {
						File destFile = new File(dataSetPath + "/JPEGImages",srcFile.getName());
						//JPEGImages
						FileUtil.copyFile(srcFile.getAbsolutePath(), destFile.getAbsolutePath());		
					}
					if(srcFile.getName().endsWith(".xml")) {
						//Annotations
						File destFile = new File(dataSetPath + "/Annotations",srcFile.getName());
						FileUtil.copyFile(srcFile.getAbsolutePath(), destFile.getAbsolutePath());		
					}

				}else if(srcFile.isDirectory()) {
					File tmpFiles[] = srcFile.listFiles();
					for(File tmpFile : tmpFiles) {
						splitVocDataSet(dataSetPath, tmpFile);
					}
				}
			}
		});

		result.setCode(0);
		result.setMessage("success");

		return result;
	}

	
	@ApiOperation(value="将VOC数据集下的数据文件划分成训练集及测试集，根据指定的比率划分，比如0.1,则表示10张图片中，1张是测试集", notes="")
	@RequestMapping(value ="/filterVocDataSetTrain", method = RequestMethod.POST)
	public Result filterVocDataSetTrain(@RequestParam("dataSetPath") String dataSetPath) {

		Result result = new Result();

		logger.info("dataSetPath=" + dataSetPath);

		ThreadSchedule.execThread(new Runnable() {
			
				@Override
				public void run() {
					File pathFile = new File(dataSetPath);
					File files[] = pathFile.listFiles();
					for(File file : files) {
						boolean t  = isValideFile(file.getAbsolutePath());
						if(!t) {
							logger.info("detele file name : " + file.getName());
							file.delete();
						}
					}
				}

				private boolean isValideFile(String srcpath) {

					SAXReader reader = new SAXReader();
					Document document;
					try {
						document = reader.read(new FileInputStream(new File(srcpath)));
						Element root = document.getRootElement();
						List<?> eleList = root.elements("object");
						for(Object obj : eleList) {
							if(obj instanceof Element) {
								Element ele = (Element)obj;
								Iterator<Element> iter = ele.elementIterator();
								while(iter.hasNext()) {
									Element tmpEle = iter.next();
									if(tmpEle.getName().equals("region_attributes")) {
										
										return true;
									}
								}

							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					return false;
				}
			});


		result.setCode(0);
		result.setMessage("success");

		return result;
	}

	
	
	@ApiOperation(value="将VOC数据集下的数据文件划分成训练集及测试集，根据指定的比率划分，比如0.1,则表示10张图片中，1张是测试集", notes="")
	@RequestMapping(value ="/createVocDataSetTrainTestRatio", method = RequestMethod.POST)
	public Result createVocDataSetTrainTestRatio(@RequestParam("dataSetPath") String dataSetPath, @RequestParam("ratio") double ratio) {

		Result result = new Result();

		logger.info("dataSetPath=" + dataSetPath + "  ratio=" + ratio);

		ThreadSchedule.execThread(new Runnable() {
			@Override
			public void run() {

				CreateTrainVal create = new CreateTrainVal();
				create.createTrainVal(dataSetPath, ratio);


			}
		});


		result.setCode(0);
		result.setMessage("success");

		return result;
	}



	@ApiOperation(value="将Path目录下所有zip文件解压", notes="")
	@RequestMapping(value ="/unzip", method = RequestMethod.POST)
	public Result unzip(@RequestParam("zipPath") String path,@RequestParam("destPath") String destPath) {

		Result result = new Result();

		logger.info("zipPath=" + path + "  destPath=" + destPath);

		ThreadSchedule.execThread(new Runnable() {

			@Override
			public void run() {
				unZipPathAllFile(path,destPath);
			}

			private void unZipPathAllFile(String path,String destPath) {
				File pathFile = new File(path);
				if(pathFile.isFile()) {
					if(pathFile.getName().toLowerCase().endsWith(".zip")) {
						logger.info("file =" + pathFile.getAbsolutePath());
						unzipAFile(pathFile,destPath);
					}else {
						logger.info("file is not zip file.");
					}
				}else {
					if(pathFile.isDirectory()) {
						File files[] = new File(path).listFiles();
						logger.info("dir =" + pathFile.getAbsolutePath());
						for(File file : files) {
							unZipPathAllFile(file.getAbsolutePath(),destPath);
						}
					}else {
						logger.info("path is not zip file or dir.");
					}
				}
			}

			private void unzipAFile(File zipFile,String destPath) {
				logger.info("unzip file:" + zipFile.getAbsolutePath());
				try(ZipFile zip = new ZipFile(zipFile,Charset.forName("GBK"))){//解决中文文件夹乱码
					for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements();) {  
						ZipEntry entry = (ZipEntry) entries.nextElement();  
						String zipEntryName = entry.getName();

						int index = zipEntryName.lastIndexOf("/");
						if(index != -1) {
							zipEntryName = zipEntryName.substring(index + 1);
						}
						File destFile = new File(destPath,entry.getName());
						destFile.getParentFile().mkdirs();
						try(InputStream in = zip.getInputStream(entry);
								FileOutputStream output = new FileOutputStream(destFile)){
							byte buffer[] = new byte[2048];
							while(true) {
								int length = in.read(buffer);
								if(length < 0) {
									break;
								}
								output.write(buffer, 0, length);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}  
				} catch (IOException e1) {
					e1.printStackTrace();
				}

			}
		});

		result.setCode(0);
		result.setMessage("success");

		return result;

	}

	public static void main(String[] args) {
		String srcPath = args[0];
		String destPath = args[1];


	}


}
