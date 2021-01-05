package com.pcl.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FileUtil {

	private static Logger logger = LoggerFactory.getLogger(FileUtil.class);
	
	

	private static String regex ="[\u4e00-\u9fa5]";
	
	public static String getRemoveChineseCharName(String fileName) {
		Pattern pat = Pattern.compile(regex);    
        Matcher mat = pat.matcher(fileName);   
		return mat.replaceAll("");
	}
	
	public static String getAllContent(String fileName,String charSet) {
		StringBuilder buffer = new StringBuilder();

		try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName),charSet))){
			String line;
			while((line = reader.readLine()) != null) {
				buffer.append(line);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return buffer.toString();
	}

	public static List<String> getAllLineList(String fileName,String charSet) {
		ArrayList<String> re = new ArrayList<>();

		try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName),charSet))){
			String line;
			while((line = reader.readLine()) != null) {
				re.add(line);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return re;
	}

	public static int getZipFileCount(File zipFile) throws IOException {
		logger.info("start deal file:" + zipFile.getAbsolutePath());
		int count = 0;
		try(ZipFile zip = new ZipFile(zipFile,Charset.forName("GBK"))){//解决中文文件夹乱码
			for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements();) {  
				ZipEntry entry = (ZipEntry) entries.nextElement();  
				if(entry.isDirectory()) {
					continue;
				}
				count++;
			}  
		}
		return count;
	}  

	public static int unZipFile(File zipFile,String destPath) throws IOException {
		logger.info("start unzip file:" + zipFile.getAbsolutePath() + " destPath=" + destPath);
		int count = 0;
		try(ZipFile zip = new ZipFile(zipFile,Charset.forName("GBK"))){//解决中文文件夹乱码

			File pathFile = new File(destPath);  
			if (!pathFile.exists()) {  
				pathFile.mkdirs();  
			} 
			for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements();) {  
				ZipEntry entry = (ZipEntry) entries.nextElement();  
				String zipEntryName = entry.getName(); 
				try(InputStream in = zip.getInputStream(entry)){
					String outPath = (destPath + "/" + zipEntryName).replaceAll("\\*", "/");  
					// 判断路径是否存在,不存在则创建文件路径  
					File file = new File(outPath.substring(0, outPath.lastIndexOf('/')));  
					if (!file.exists()) {  
						file.mkdirs();  
					}  
					// 判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压  
					if (new File(outPath).isDirectory()) {  
						continue;  
					} 
					count++;
					// 输出文件路径信息  
					try(FileOutputStream out = new FileOutputStream(outPath)){
						byte[] buf1 = new byte[1024];  
						int len;  
						while ((len = in.read(buf1)) > 0) {  
							out.write(buf1, 0, len);  
						}  
					}
				}

			}  

		}
		return count;
	}  
	
	public static int unZipFileFlat(File zipFile,String destPath) throws IOException {
		logger.info("start unzip file:" + zipFile.getAbsolutePath() + " destPath=" + destPath);
		int count = 0;
		try(ZipFile zip = new ZipFile(zipFile,Charset.forName("GBK"))){//解决中文文件夹乱码

			File pathFile = new File(destPath);  
			if (!pathFile.exists()) {  
				pathFile.mkdirs();  
			} 
			for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements();) {  
				ZipEntry entry = (ZipEntry) entries.nextElement();  
				String zipEntryName = entry.getName();
				
				if(entry.isDirectory()) {
					continue;
				}
				try(InputStream in = zip.getInputStream(entry)){
					if(zipEntryName.indexOf("/") != -1) {
						zipEntryName = zipEntryName.substring(zipEntryName.lastIndexOf("/") + 1);
					}
					String outPath = (destPath + "/" + zipEntryName).replaceAll("\\*", "/");  
					count++;
					// 输出文件路径信息  
					try(FileOutputStream out = new FileOutputStream(outPath)){
						byte[] buf1 = new byte[1024];  
						int len;  
						while ((len = in.read(buf1)) > 0) {  
							out.write(buf1, 0, len);  
						}  
					}
				}

			}  

		}
		return count;
	}  


	public static boolean delDir(String filePath) {
		boolean flag = true;
		if(filePath != null) {
			File file = new File(filePath);
			if(file.exists()) {
				File[] filePaths = file.listFiles();
				if(filePaths != null) {
					for(File f : filePaths) {
						if(f.isFile()) {
							f.delete();
						}
						if(f.isDirectory()){
							String fpath = f.getPath();
							delDir(fpath);
							f.delete();
						}
					}
				}
				file.delete();
			}
		}else {
			flag = false;
		}
		return flag;
	}


	public static void copyDir(String srcPath,String destPath) {
		File srcFile = new File(srcPath);
		File srcFiles[] = srcFile.listFiles();
		File destFile = new File(destPath);
		for(File tmpFile : srcFiles) {
			if(tmpFile.isDirectory()) {
				copyDir(tmpFile.getAbsolutePath(),destPath + File.separator + tmpFile.getName());
			}else {
				if(!destFile.exists()) {
					destFile.mkdir();
				}
				File tmpDestFile = new File(destPath,tmpFile.getName());
				try {
					Files.copy(tmpFile.toPath(), tmpDestFile.toPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	public static void copyFlatDir(String srcPath,String destPath) {
		File srcFile = new File(srcPath);
		File srcFiles[] = srcFile.listFiles();
		File destFile = new File(destPath);
		if(!destFile.exists()) {
			destFile.mkdir();
		}
		
		for(File tmpFile : srcFiles) {
			if(tmpFile.isDirectory()) {
				copyFlatDir(tmpFile.getAbsolutePath(),destPath);
			}else {
				File tmpDestFile = new File(destPath,tmpFile.getName());
				if(tmpDestFile.exists()) {
					continue;
				}
				try {
					Files.copy(tmpFile.toPath(), tmpDestFile.toPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	

	private static boolean isPreFix(String name,String preFix) {
		if(preFix == null) {
			return true;
		}
		return name.startsWith(preFix);
	}

	private static boolean isPostFix(String name,String postFix) {
		if(postFix == null) {
			return true;
		}
		return name.endsWith(postFix);
	}


	public static String getLastModifiedFile(String dir,String preFix, String posFix) {

		ArrayList<File> list = new ArrayList<>();

		File dirFiles[] = new File(dir).listFiles();
		if(dirFiles == null) {
			return null;
		}
		for(File tmpFile : dirFiles) {
			if(tmpFile.isDirectory()) {
				continue;
			}
			if(isPreFix(tmpFile.getName(), preFix) && isPostFix(tmpFile.getName(), posFix)) {
				list.add(tmpFile);
			}
		}
		list.sort(new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				if(o1.lastModified() > o2.lastModified()){
					return -1;
				}else {
					return 1;
				}
			}

		});

		if(list.size() > 0) {
			return list.get(0).getAbsolutePath();
		}
		return null;
	}

	public static void copyFile(String srcFilePath,String destFilePath) {

		try {
			Files.copy(new File(srcFilePath).toPath(), new File(destFilePath).toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


	public static List<File> getAllFileList(String tmpPath) {
		List<File> result = new ArrayList<>();
		if(!new File(tmpPath).exists()) {
			return result;
		}
		File files[] = new File(tmpPath).listFiles();
		for(File file : files) {
			if(file.isFile()) {
				result.add(file);
			}else {
				result.addAll(getAllFileList(file.getAbsolutePath()));
			}
		}
		return result;
	}


	/**
	 * 压缩成ZIP 方法
	 * @param srcFiles 需要压缩的文件列表
	 * @param destFile           压缩目标文件
	 * @param parentPath   压缩相对路径
	 * @throws RuntimeException 压缩失败会抛出运行时异常
	 */

	public static void toZip(List<File> srcFiles,String destFile,String parentPath)throws RuntimeException {

		long start = System.currentTimeMillis();
		File outFile = new File(destFile);
		outFile.getParentFile().mkdirs();
		int length = 1;
		if(parentPath.endsWith("/") ||parentPath.endsWith("\\")) {
			length = 0;
		}
		try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outFile))) {
			for (File srcFile : srcFiles) {
				byte[] buf = new byte[2048];
				String absolutePath = srcFile.getAbsolutePath();
				String entryName = absolutePath.substring(parentPath.length() + length);
				zos.putNextEntry(new ZipEntry(entryName.replace("\\", "/")));
				int len;
				try(FileInputStream in = new FileInputStream(srcFile)){
					while ((len = in.read(buf)) != -1){
						zos.write(buf, 0, len);
					}
					zos.closeEntry();
				}
			}
			long end = System.currentTimeMillis();
			logger.info("压缩完成，耗时：" + (end - start) +" ms");
		} catch (Exception e) {
			throw new RuntimeException("zip error from ZipUtils",e);
		}
	}
	
	
	public static void main(String[] args) {
		String srcPath = "D:\\2019文档\\问题定位\\5\\JPEGImages\\";
		String destPath = "D:\\2019文档\\问题定位\\6\\JPEGImages\\";
		
		copyFlatDir(srcPath, destPath);
		
		
		
	}

}