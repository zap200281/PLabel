package com.pcl.control;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.pcl.pojo.DcmObj;
import com.pcl.pojo.dzi.ResData;
import com.pcl.pojo.dzi.SizeData;
import com.pcl.pojo.dzi.TitleSource;
import com.pcl.service.DataSetService;
import com.pcl.service.LabelDcmService;
import com.pcl.service.ObjectFileService;

@Controller
public class PictureController {

	private static Logger logger = LoggerFactory.getLogger(PictureController.class);


	@Autowired
	private HttpServletResponse response;

	@Autowired
	private HttpServletRequest request;

	@Autowired
	private ObjectFileService fileService;

	@Autowired
	private LabelDcmService labelDcmService;

	@Autowired
	private DataSetService dataSetService;

	private Map<String,String> pathMap = new ConcurrentHashMap<>();

	@CrossOrigin(origins = "*", maxAge=3600)
	@RequestMapping(method = RequestMethod.GET, value = "/minio/{bucketname}/{filename:.+}")  
	@ResponseBody  
	public void getFileForDataSet_3(@PathVariable String bucketname,@PathVariable String filename) {

		if(filename.toLowerCase().endsWith(".mp4")) {
			logger.info("Get mp4 video: " + bucketname + File.separator + filename);
			getVideoStream(bucketname, filename);

		}else {
			logger.info("Get picture: " + bucketname + File.separator + filename);
			response.setContentType("image/jpg;charset=utf-8");
			try(InputStream inStream = fileService.getPicture(bucketname, filename);
					OutputStream outStream = response.getOutputStream()){

				byte[] buf = new byte[1024 * 16];
				int len = 0;
				while ((len = inStream.read(buf)) != -1){
					outStream.write(buf, 0, len);
				}
				outStream.flush();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}  


	@CrossOrigin(origins = "*", maxAge=3600)
	@RequestMapping(method = RequestMethod.GET, value = "/minio/{bucketname}/{tmppath}/**")  
	@ResponseBody  
	public void getFileForObsPath(@PathVariable String bucketname,@PathVariable String tmppath) {

		StringBuffer str = request.getRequestURL();
		String url = str.toString();
		url = url.replace("%20", " ");
		int tmpIndex = url.indexOf("/minio/");
		String relativeUrl = url.substring(tmpIndex);
		logger.info("obs get picture: " +relativeUrl);
		response.setContentType("image/jpg;charset=utf-8");
		try(InputStream inStream = fileService.getImageInputStream(relativeUrl);
				OutputStream outStream = response.getOutputStream()){
			byte[] buf = new byte[1024 * 16];
			int len = 0;
			while ((len = inStream.read(buf)) != -1){
				outStream.write(buf, 0, len);
			}
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}  


	private void getVideoStream(String bucketName,String fileName) {
		logger.info("start to read inputstream....");
		long start = System.currentTimeMillis();
		BufferedInputStream bis = null;
		try(InputStream ins = fileService.getPicture(bucketName, fileName);
				OutputStream outStream = response.getOutputStream()){

			long p = 0L;
			long toLength = 0L;
			long contentLength = 0L;
			int rangeSwitch = 0; // 0,从头开始的全文下载；1,从某字节开始的下载（bytes=27000-）；2,从某字节开始到某字节结束的下载（bytes=27000-39000）
			long fileLength = fileService.getMinioObjectLength(bucketName,fileName);
			String rangBytes = "";

			bis = new BufferedInputStream(ins);

			// tell the client to allow accept-ranges
			response.reset();
			response.setHeader("Accept-Ranges", "bytes");

			// client requests a file block download start byte
			String range = request.getHeader("Range");
			logger.info("range=" +range);
			if (range != null && range.trim().length() > 0 && !"null".equals(range)) {
				response.setStatus(javax.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT);
				rangBytes = range.replaceAll("bytes=", "");
				if (rangBytes.endsWith("-")) { // bytes=270000-
					rangeSwitch = 1;
					p = Long.parseLong(rangBytes.substring(0, rangBytes.indexOf("-")));
					contentLength = fileLength - p; // 客户端请求的是270000之后的字节（包括bytes下标索引为270000的字节）
				} else { // bytes=270000-320000
					rangeSwitch = 2;
					String temp1 = rangBytes.substring(0, rangBytes.indexOf("-"));
					String temp2 = rangBytes.substring(rangBytes.indexOf("-") + 1, rangBytes.length());
					p = Long.parseLong(temp1);
					toLength = Long.parseLong(temp2);
					contentLength = toLength - p + 1; // 客户端请求的是 270000-320000 之间的字节
				}
			} else {
				contentLength = fileLength;
			}
			logger.info("contentLength=" + contentLength);
			// 如果设设置了Content-Length，则客户端会自动进行多线程下载。如果不希望支持多线程，则不要设置这个参数。
			// Content-Length: [文件的总大小] - [客户端请求的下载的文件块的开始字节]
			response.setHeader("Content-Length", new Long(contentLength).toString());

			// 断点开始
			// 响应的格式是:
			// Content-Range: bytes [文件块的开始字节]-[文件的总大小 - 1]/[文件的总大小]
			if (rangeSwitch == 1) {
				String contentRange = new StringBuffer("bytes ").append(new Long(p).toString()).append("-")
						.append(new Long(fileLength - 1).toString()).append("/")
						.append(new Long(fileLength).toString()).toString();
				response.setHeader("Content-Range", contentRange);
				bis.skip(p);
			} else if (rangeSwitch == 2) {
				String contentRange = range.replace("=", " ") + "/" + new Long(fileLength).toString();
				response.setHeader("Content-Range", contentRange);
				bis.skip(p);
			} else {
				String contentRange = new StringBuffer("bytes ").append("0-").append(fileLength - 1).append("/")
						.append(fileLength).toString();
				response.setHeader("Content-Range", contentRange);
			}


			response.setContentType("application/octet-stream");
			response.addHeader("Content-Disposition", "attachment;filename=" + fileName);

			OutputStream out = response.getOutputStream();
			int n = 0;
			long readLength = 0;
			int bsize = 1024;
			byte[] bytes = new byte[bsize];
			if (rangeSwitch == 2) {
				// 针对 bytes=27000-39000 的请求，从27000开始写数据
				while (readLength <= contentLength - bsize) {
					n = bis.read(bytes);
					readLength += n;
					out.write(bytes, 0, n);
				}
				if (readLength <= contentLength) {
					n = bis.read(bytes, 0, (int) (contentLength - readLength));
					out.write(bytes, 0, n);
				}
			} else {
				while ((n = bis.read(bytes)) != -1) {
					out.write(bytes, 0, n);
				}
			}
			out.flush();
		} catch (IOException ie) {
			// 忽略 ClientAbortException 之类的异常
			logger.info(ie.getMessage());
		} catch (Exception e) {
			logger.info(e.getMessage());
		}
		logger.info("cost time=" + (System.currentTimeMillis() - start));
	}


	/**
	 * direct: RLAP,APSI,RLSI
	 * index: 当direct=RLAP时，index=0; 
	 *                            当direct=APSI时，index的范围为0--原始图像的宽度， 
	 *                            当direct=RLSI时，index的范围为0--原始图像的高度
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/dcm/**")  
	@ResponseBody  
	public void getFileForDcm(@RequestParam(value="label_dcm_taskId",required=false) String labelDcmItemTaskId,@RequestParam(value="direct",required=false, defaultValue="RLAP") String direct,@RequestParam(value="index",required=false, defaultValue="0") int index) {

		StringBuffer str = request.getRequestURL();
		String url = str.toString();
		url = url.replace("%20", " ");
		int tmpIndex = url.indexOf("/dcm/");
		String path = url.substring(tmpIndex + 5);
		//logger.info("Get dcm picture: " + path);


		response.setContentType("image/jpg;charset=utf-8");

		try(OutputStream outStream = response.getOutputStream()){
			DcmObj dcmObj = null;
			if(labelDcmItemTaskId == null) {
				dcmObj = fileService.getDcmPicture(path);
			}else {
				dcmObj = labelDcmService.getDcmObj(labelDcmItemTaskId, path, direct, index);
			}
			ImageIO.write(dcmObj.getImage(), "jpg", outStream);
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}  

	/**
	 * direct: RLAP,APSI,RLSI
	 * index: 当direct=RLAP时，index=0; 
	 *                            当direct=APSI时，index的范围为0--原始图像的宽度， 
	 *                            当direct=RLSI时，index的范围为0--原始图像的高度
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/static/**")  
	@ResponseBody  
	public void getFileForStatic() {

		StringBuffer str = request.getRequestURL();

		String url = str.toString();
		url = url.replace("%20", " ");
		int tmpIndex = url.indexOf("/static/");

		String path = url.substring(tmpIndex + "/static/".length());

		response.setHeader("content-type", "application/octet-stream");
		response.setContentType("application/octet-stream");
		logger.info("path=" + path);
		try(OutputStream outStream = response.getOutputStream()){
			InputStream inStream = this.getClass().getResourceAsStream("/static/" + path);
			if(inStream != null) {
				byte[] buf = new byte[1024 * 16];
				int len = 0;
				while ((len = inStream.read(buf)) != -1){
					outStream.write(buf, 0, len);
				}
			}
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}  



	///api/getdziimage

	/**
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/api/getTitleSource")  
	@ResponseBody  
	public TitleSource getTitleSource(@RequestParam("dateset_id") String datasetid) {

		logger.info("getTitleSource start.dateset_id" + datasetid);

		Map<String,Object> dziInfoMap = dataSetService.getDziInfo(datasetid);

		if(!dziInfoMap.isEmpty()) {
			return getTitleSource(getStr(dziInfoMap.get("Format")), getStr(dziInfoMap.get("Overlap")), getStr(dziInfoMap.get("TileSize")), datasetid, getStr(dziInfoMap.get("Width")), getStr(dziInfoMap.get("Height")));
		}

		return new TitleSource();
	}  

	private String getStr(Object obj) {
		if(obj != null) {
			if(obj instanceof Double || obj instanceof Float) {
				return String.valueOf((int)obj);
			}else {
				return obj.toString();
			}
		}
		return "";

	}


	private TitleSource getTitleSource(String format,String overlap,String tilesize,String dataset_id,String width,String height) {

		ResData resdata = new ResData();
		resdata.setFormat(format);
		resdata.setOverlap(overlap);
		resdata.setTileSize(tilesize);
		resdata.setUrl("/api/getdziimage/" + dataset_id + "/");
		resdata.setXmlns("http://schemas.microsoft.com/deepzoom/2009");

		SizeData size = new SizeData();

		size.setWidth(width);
		size.setHeight(height);
		resdata.setSize(size);

		TitleSource titleSource = new TitleSource();
		titleSource.setImage(resdata);

		return titleSource;
	}


	@CrossOrigin(origins = "*", maxAge=3600)
	@RequestMapping(method = RequestMethod.GET, value = "/api/getdziimage/{datasetid}/{level}/{filename:.+}")  
	@ResponseBody  
	public void getdziimage(@PathVariable String datasetid,@PathVariable String level,@PathVariable String filename) {

		StringBuffer str = request.getRequestURL();


		String dziBucketName = "";
		if(pathMap.containsKey(datasetid)) {
			dziBucketName = pathMap.get(datasetid);
		}else {
			Map<String,Object> dziInfoMap = dataSetService.getDziInfo(datasetid);
			if(!dziInfoMap.isEmpty() && dziInfoMap.get("dziBucketName") != null) {
				dziBucketName = dziInfoMap.get("dziBucketName").toString();
			}
			pathMap.put(datasetid, dziBucketName);
		}
		String objectName = level + "_" + filename;
		//String picturePath=path + "/" +  level + "/" + filename;
		response.setContentType("image/jpeg;charset=utf-8");
		logger.info("url=" + str.toString() + " miniopath=" + dziBucketName + "/" + objectName);
		try(InputStream inStream = fileService.getDziPicture(dziBucketName, objectName);
				OutputStream outStream = response.getOutputStream()){

			byte[] buf = new byte[1024 * 16];
			int len = 0;
			while ((len = inStream.read(buf)) != -1){
				outStream.write(buf, 0, len);
			}
			outStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}  

}
