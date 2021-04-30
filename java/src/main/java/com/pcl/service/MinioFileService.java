package com.pcl.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.xmlpull.v1.XmlPullParserException;

import com.pcl.constant.Constants;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.DcmObj;
import com.pcl.pojo.FileResult;
import com.pcl.util.BucketNameUtil;
import com.pcl.util.DcmUtil;

import io.minio.MinioClient;
import io.minio.ObjectStat;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidArgumentException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import io.minio.errors.NoResponseException;
import io.minio.errors.RegionConflictException;
import io.minio.messages.Item;


public class MinioFileService implements ObjectFileService{

	private static Logger logger = LoggerFactory.getLogger(MinioFileService.class);

	@Value("${minio.url}")
	private String minioUrl;

	@Value("${minio.username}")
	private String minioName = "minio";

	@Value("${minio.password}")
	private String minioPassword = "miniominio";


	private MinioClient minioClient;


	public InputStream getPicture(String bucketname,String fileName) throws Exception {
		MinioClient minioClient = getMinioClient();

		return minioClient.getObject(bucketname, fileName);
	}



	public DcmObj getDcmPicture(String fileName) throws Exception {

		String filePath = LabelDataSetMerge.getUserDataSetPath() + File.separator + "dcm" + File.separator +  fileName;
		if(!new File(filePath).exists()) {
			logger.info("get from minio.. fileName=" + fileName);
			return DcmUtil.getImageByDcmFile(getImageInputStream("/dcm/" + fileName));
		}
		logger.info("get from disk, filePath=" + filePath);
		return  DcmUtil.getImageByDcmFile(filePath);
	}


	private MinioClient getMinioClient() throws InvalidEndpointException, InvalidPortException {
		if(minioClient == null) {
			minioClient = new MinioClient(minioUrl, minioName, minioPassword); 
		}
		return minioClient;
	}


	public String getImageWidthHeight(String relativeMinioUrl) {
		try {
			MinioClient minioClient = getMinioClient();
			if(relativeMinioUrl.startsWith("/minio/")) {
				String tmp[] = relativeMinioUrl.split("/");
				int length = tmp.length;
				try(InputStream inputStream = (minioClient.getObject(tmp[length-2], tmp[length-1]))){
					BufferedImage sourceImg =ImageIO.read(inputStream);
					int width = sourceImg.getWidth();
					int height = sourceImg.getHeight();
					return width + "," + height;
				}
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return "";
	}

	public BufferedImage getBufferedImage(String relativeMinioUrl) {
		try {
			MinioClient minioClient = getMinioClient();
			if(relativeMinioUrl.startsWith("/minio/")) {
				String tmp[] = relativeMinioUrl.split("/");
				int length = tmp.length;
				try(InputStream inputStream = (minioClient.getObject(tmp[length-2], tmp[length-1]))){
					BufferedImage sourceImg =ImageIO.read(inputStream);

					return sourceImg;
				}
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return null;
	}

	public InputStream getImageInputStream(String relativeMinioUrl) {
		try {
			MinioClient minioClient = getMinioClient();
			if(relativeMinioUrl.startsWith("/minio/") || relativeMinioUrl.startsWith("/dcm/")) {
				String tmp[] = relativeMinioUrl.split("/");
				int length = tmp.length;
				return minioClient.getObject(tmp[length-2], tmp[length-1]);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return null;
	}


	public void deleteFileFromMinio(String bucketname,String objectName,String fileBucketName) {
		try {
			MinioClient minioClient = getMinioClient();
			minioClient.removeObject(bucketname, objectName);

			if(fileBucketName != null &&  minioClient.bucketExists(fileBucketName)) {
				Iterable<Result<Item>> result = minioClient.listObjects(fileBucketName);
				result.forEach(
						(temp)->{
							try {
								Item item = temp.get();
								minioClient.removeObject(fileBucketName, item.objectName());
							}catch (Exception e) {
								logger.info("error.",e);
							}
						}
						);
				minioClient.removeBucket(fileBucketName);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}


	public void deleteFileFromMinio(String bucketname,String objectName) {
		try {
			MinioClient minioClient = getMinioClient();
			minioClient.removeObject(bucketname, objectName);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteFileFromMinio(String relatetiveUrl) {
		try {
			String tmp[] = relatetiveUrl.split("/");
			int length = tmp.length;
			MinioClient minioClient = getMinioClient();
			logger.info("bucketname=" + tmp[length-2] + " objectname=" + tmp[length-1]);
			minioClient.removeObject(tmp[length-2], tmp[length-1]);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}



	public boolean isExistMinioFile(String bucketname,String objectName) {
		try {
			MinioClient minioClient = getMinioClient();
			ObjectStat obj = minioClient.statObject(bucketname, objectName);
			if(obj != null) {
				return true;
			}
		}catch (Exception e) {
			//e.printStackTrace();
			logger.info("the file is not exist, bucketname=" + bucketname +  " objectName=" + objectName);
			logger.info(e.getMessage());
		}
		return false;
	}
	
	public boolean isExistMinioFileAndDeleteNotComplete(String bucketname,String objectName) {
		try {
			MinioClient minioClient = getMinioClient();
			ObjectStat obj = minioClient.statObject(bucketname, objectName);
			if(obj != null) {
				if(obj.length() > 20) {
					return true;
				}else {
					//不是完整的，删除
					minioClient.removeObject(bucketname, objectName);
				}
			}
		}catch (Exception e) {
			//e.printStackTrace();
			logger.info("the file is not exist, bucketname=" + bucketname +  " objectName=" + objectName);
			logger.info(e.getMessage());
		}
		return false;
	}

	public long getMinioObjectLength(String bucketname,String objectName) {
		try {
			MinioClient minioClient = getMinioClient();
			ObjectStat obj = minioClient.statObject(bucketname, objectName);
			if(obj != null) {
				return obj.length();
			}
		}catch (Exception e) {
			//e.printStackTrace();
			logger.info("the file is not exist, bucketname=" + bucketname +  " objectName=" + objectName);
			logger.info(e.getMessage());
		}
		return -1;
	}


	public void removeBucketName(String fileBucketName) {
		try {
			MinioClient minioClient = getMinioClient();
			if(minioClient.bucketExists(fileBucketName)) {
				Iterable<Result<Item>> result = minioClient.listObjects(fileBucketName);
				result.forEach(
						(temp)->{
							try {
								Item item = temp.get();
								minioClient.removeObject(fileBucketName, item.objectName());
							}catch (Exception e) {
								logger.info("error.",e);
							}
						}
						);
				minioClient.removeBucket(fileBucketName);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}

	}


	public void uploadPictureFile(List<File> fileList,String bucketName){
		long start = System.currentTimeMillis();
		logger.info("upload video picture file start.");
		try {
			MinioClient minioClient = getMinioClient();
			if(!minioClient.bucketExists(bucketName)) {
				minioClient.makeBucket(bucketName);
			}
			for(File file : fileList) {
				minioClient.putObject(bucketName, file.getName(), file.getAbsolutePath());
			}
			logger.info("succeed to upload video picture file end. cost=" + (System.currentTimeMillis() - start));
		} catch (InvalidEndpointException | InvalidPortException | InvalidKeyException | InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | NoResponseException | ErrorResponseException | InternalException | InvalidArgumentException | IOException | XmlPullParserException| RegionConflictException e) {
			e.printStackTrace();
			logger.info("error to upload video picture file end.");
		} 

	}

	public void uploadSvsDziFile(File file, String objectName, String bucketName){
		//long start = System.currentTimeMillis();
		try {
			MinioClient minioClient = getMinioClient();
			if(!minioClient.bucketExists(bucketName)) {
				minioClient.makeBucket(bucketName);
			}
			minioClient.putObject(bucketName, objectName, file.getAbsolutePath());
			//logger.info("succeed to upload file end. cost=" + (System.currentTimeMillis() - start));
		} catch (InvalidEndpointException | InvalidPortException | InvalidKeyException | InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | NoResponseException | ErrorResponseException | InternalException | InvalidArgumentException | IOException | XmlPullParserException| RegionConflictException e) {
			e.printStackTrace();
			logger.info("error to upload video picture file end.");
		} 

	}

	public FileResult uploadVideoFile(File file) {

		logger.info("upload video file start.");
		long start = System.currentTimeMillis();
		FileResult re = new FileResult();
		re.setCode("1");
		re.setOrigin_file_name(file.getName());

		try {
			MinioClient minioClient = getMinioClient();

			String bucketName = BucketNameUtil.getBuketName(String.valueOf(Constants.DATASET_TYPE_VIDEO));

			if(!minioClient.bucketExists(bucketName)) {
				minioClient.makeBucket(bucketName);
			}

			String objectName = file.getName();

			if(isExistMinioFile(bucketName, objectName)) {
				objectName = System.nanoTime() + "_" + objectName;
			}

			minioClient.putObject(bucketName, objectName, file.getAbsolutePath(), "application/octet-stream");

			re.setCode("0");
			re.setBucket_name(bucketName);
			re.setEtag("");
			re.setObject_name(objectName);
			re.setPublic_url("/" + bucketName + "/" + objectName);
			//re.setPresigned_url(url);
			logger.info("succeed to upload file end. cost=" + (System.currentTimeMillis() - start));
		} catch (InvalidEndpointException | InvalidPortException | InvalidKeyException | InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | NoResponseException | ErrorResponseException | InternalException | InvalidArgumentException | IOException | XmlPullParserException| RegionConflictException e) {
			e.printStackTrace();
			logger.info("error to upload file end.");
			re.setCode_msg(e.getMessage());
		} 

		return re;

	}

	public List<FileResult> uploadFile(MultipartFile zipFile,String dataSetType) {
		long start = System.currentTimeMillis();
		ArrayList<FileResult> reList = new ArrayList<>();

		logger.info("upload file start.");

		FileResult re = new FileResult();
		re.setCode("1");
		re.setOrigin_file_name(zipFile.getOriginalFilename());

		try {
			MinioClient minioClient = getMinioClient();

			String bucketName = BucketNameUtil.getBuketName(dataSetType);

			if(!minioClient.bucketExists(bucketName)) {
				minioClient.makeBucket(bucketName);
			}

			String objectName = zipFile.getOriginalFilename();

			if(isExistMinioFile(bucketName, objectName)) {
				objectName = System.nanoTime() + "_" + objectName;
			}
			minioClient.putObject(bucketName, objectName, zipFile.getInputStream(), "application/octet-stream");

			//String url = minioClient.getObjectUrl(bucketName, objectName);

			re.setCode("0");
			re.setBucket_name(bucketName);
			re.setEtag("");
			re.setObject_name(objectName);
			re.setPublic_url("/" + bucketName + "/" + objectName);
			//re.setPresigned_url(url);
			logger.info("succeed to upload file end. cost=" + (System.currentTimeMillis() - start));
		} catch (InvalidEndpointException | InvalidPortException | InvalidKeyException | InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | NoResponseException | ErrorResponseException | InternalException | InvalidArgumentException | IOException | XmlPullParserException| RegionConflictException e) {
			e.printStackTrace();
			logger.info("error to upload file end.");
			re.setCode_msg(e.getMessage());
		} 

		reList.add(re);
		return reList;
	}


	public List<String> unZipFileToMinio(File zipFile,String bucketName) throws Exception {
		List<String> result = new ArrayList<>();

		MinioClient minioClient = getMinioClient();
		if(!minioClient.bucketExists(bucketName)) {
			minioClient.makeBucket(bucketName);
		}

		try(ZipFile zip = new ZipFile(zipFile,Charset.forName("GBK"))){//解决中文文件夹乱码
			for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements();) {  
				ZipEntry entry = (ZipEntry) entries.nextElement();  
				String zipEntryName = entry.getName();
				if(entry.isDirectory() || zipEntryName.endsWith(".json") || zipEntryName.endsWith(".xml") || zipEntryName.endsWith(".txt")) {
					continue;
				}
				int index = zipEntryName.lastIndexOf("/");
				if(index != -1) {
					zipEntryName = zipEntryName.substring(index + 1);
				}

				try(InputStream in = zip.getInputStream(entry)){
					try {
						minioClient.putObject(bucketName, zipEntryName, in, "application/octet-stream");
					}catch (Exception e) {
						logger.info("put object error. zipEntryName=" + zipEntryName,e);
					}
					result.add(zipEntryName);
				}
			}  
		}
		return result;
	}  





	public String downLoadFileFromMinio(String bucketName, String objectName,String destPath) throws LabelSystemException {
		try {
			logger.info("start to download file:" + bucketName + "/" + objectName + ", destPath=" + destPath);
			MinioClient minioClient = getMinioClient();
			try(InputStream intpuStream = minioClient.getObject(bucketName, objectName);
					FileOutputStream outputStream = new FileOutputStream(destPath + File.separator + objectName)){
				byte buffer[] = new byte[2048];
				while(true) {
					int length = intpuStream.read(buffer);
					if(length < 0) {
						break;
					}
					outputStream.write(buffer, 0, length);
				}
			}
			logger.info("succeed to download file success.");
			return destPath + File.separator + objectName;

		} catch (InvalidEndpointException | InvalidPortException | InvalidKeyException | InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | NoResponseException | ErrorResponseException | InternalException | InvalidArgumentException | IOException | XmlPullParserException e) {
			e.printStackTrace();
			logger.info("error to upload file end.");
			throw new LabelSystemException("Failed to download file.");
		}
	}

	public String downLoadFileFromMinioAndSetPictureName(String bucketName, String objectName,String pictureName) throws LabelSystemException {
		try {
			//logger.info("start to download file:" + bucketName + "/" + objectName + ", pictureName=" + pictureName);
			MinioClient minioClient = getMinioClient();
			try(InputStream intpuStream = minioClient.getObject(bucketName, objectName);
					FileOutputStream outputStream = new FileOutputStream(pictureName)){
				byte buffer[] = new byte[2048];
				while(true) {
					int length = intpuStream.read(buffer);
					if(length < 0) {
						break;
					}
					outputStream.write(buffer, 0, length);
				}
			}
			logger.info("succeed to download file success. minioName=" + bucketName + "/" + objectName + ", pictureName=" + pictureName);
			return pictureName;
		} catch (InvalidEndpointException | InvalidPortException | InvalidKeyException | InvalidBucketNameException | NoSuchAlgorithmException | InsufficientDataException | NoResponseException | ErrorResponseException | InternalException | InvalidArgumentException | IOException | XmlPullParserException e) {
			e.printStackTrace();
			logger.info("error to upload file end.");
			throw new LabelSystemException("Failed to download file.");
		}
	}

	public static void main(String[] args) {
		MinioFileService service = new MinioFileService();

		try {
			service.downLoadFileFromMinio("label-img", "1575882384404-zap-images1.zip", "D:\\");
		} catch (LabelSystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}



	@Override
	public List<String> listAllFile(String obsPath) {
		
		return new ArrayList<>();
	}



	@Override
	public InputStream getInputStream(String bucketname, String fileName, long offset) throws Exception {
		MinioClient minioClient = getMinioClient();
		return minioClient.getObject(bucketname, fileName,offset);
	}



	@Override
	public InputStream getDziPicture(String bucketname, String fileName) throws Exception {
		return getPicture(bucketname, fileName);
	}

}
