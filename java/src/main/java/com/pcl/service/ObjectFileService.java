package com.pcl.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.DcmObj;
import com.pcl.pojo.FileResult;

public interface ObjectFileService {

	
	public InputStream getInputStream(String bucketname,String fileName,long offset) throws Exception;

	public InputStream getPicture(String bucketname,String fileName) throws Exception;
	
	public InputStream getDziPicture(String bucketname,String fileName) throws Exception;

	public DcmObj getDcmPicture(String fileName) throws Exception;

	public String getImageWidthHeight(String relativeMinioUrl);

	public BufferedImage getBufferedImage(String relativeMinioUrl);

	public InputStream getImageInputStream(String relativeMinioUrl);

	public void deleteFileFromMinio(String bucketname,String objectName,String fileBucketName);

	public void deleteFileFromMinio(String bucketname,String objectName);

	public void deleteFileFromMinio(String relatetiveUrl);

	public boolean isExistMinioFile(String bucketname,String objectName);
	
	public boolean isExistMinioFileAndDeleteNotComplete(String bucketname,String objectName);

	public long getMinioObjectLength(String bucketname,String objectName);

	public void removeBucketName(String fileBucketName);

	public void uploadPictureFile(List<File> fileList,String bucketName);

	public void uploadSvsDziFile(File file, String objectName, String bucketName);
	public FileResult uploadVideoFile(File file) ;

	public List<FileResult> uploadFile(MultipartFile zipFile,String dataSetType) ;

	public List<String> unZipFileToMinio(File zipFile,String bucketName) throws Exception;

	public String downLoadFileFromMinio(String bucketName, String objectName,String destPath) throws LabelSystemException ;

	public String downLoadFileFromMinioAndSetPictureName(String bucketName, String objectName,String pictureName) throws LabelSystemException;
	
	public List<String> listAllFile(String obsPath);
}
