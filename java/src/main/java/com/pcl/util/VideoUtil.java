package com.pcl.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pcl.constant.Constants;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.mybatis.VideoInfo;

public class VideoUtil {

	private static Logger logger = LoggerFactory.getLogger(VideoUtil.class);

	private static String regexDuration = "Duration: (.*?), start: (.*?), bitrate: (\\d*) kb\\/s";
	private static String regexVideo = "Video: (.*?), (.*?), (\\d*x\\d*)[,\\s]";
	private static String regexAudio = "Audio: (\\w*), (\\d*) Hz";

	private static String regexFps = ", (\\d*|\\d*.\\d*) fps";
	
	private static String ffprobeCmd="ffprobe -select_streams v -of csv -show_entries frame=pkt_pts_time,pkt_duration,pict_type {videoFileName} > {csvFileName}";
	private static String ffmpegCmd = "ffmpeg -i {videoFileName} -r {fps} {-s} -q:v 2 -f image2 {pictureFileName}%08d.jpg";
	private static String ffmpegKeyCmd = "ffmpeg -i {videoFileName} {-s} -vf \"select=eq(pict_type\\,I)\"  -vsync vfr -qscale:v 2 -f image2 {pictureFileName}%08d.jpg";
	private static String ffmpegInfo = "ffmpeg -i {videoFileName}";
	private static String ffmpegConcat = "ffmpeg -f concat -safe 0 -i {fileListName} -c copy {destFileName}";
    //time: 00:00:06.688
	private static String ffmpegChouZheng = "ffmpeg -ss {time} -i {fileListName} -vframes 1 {pictureName}";
	
	private static String ffmpegMergeVideoCmd = "ffmpeg -y -r 16 -i {picturePath}%08d.jpg -vcodec h264 {videoFileName}";
	
	public static void mergePictureToVideo(String picturePath,String videoFileName,int pictureNum) {
		String ffmpegMergeVideoExecCmd = ffmpegMergeVideoCmd.replace("{picturePath}", picturePath);
		ffmpegMergeVideoExecCmd = ffmpegMergeVideoExecCmd.replace("{videoFileName}", videoFileName);
		
		int time = pictureNum / 22  + 60;
		
		try {
			StringBuilder str = new StringBuilder();
			ProcessExeUtil.execScriptReturnOutputNotAsyn(ffmpegMergeVideoExecCmd, new File(videoFileName).getParentFile().getAbsolutePath(), time, str);
		}catch (Exception e) {
			logger.info(e.getMessage(),e);
		}
	}
	
	public static String getPictureFromVideo(String time,File videoFile,String pictureName) {
		
		String ffmpegChouZhengCmd = ffmpegChouZheng.replace("{time}", time); 
		ffmpegChouZhengCmd = ffmpegChouZhengCmd.replace("{fileListName}", videoFile.getAbsolutePath());
		ffmpegChouZhengCmd = ffmpegChouZhengCmd.replace("{pictureName}", pictureName);

		try {
			StringBuilder str = new StringBuilder();
			ProcessExeUtil.execScriptReturnOutputNotAsyn(ffmpegChouZhengCmd, videoFile.getParentFile().getAbsolutePath(), 60, str);
		}catch (Exception e) {
			logger.info(e.getMessage(),e);
		}
		return pictureName;
	}
	
	
	public static VideoInfo getVideoObj(String vedioStrInfo) {

		VideoInfo obj = new VideoInfo();
		try {
			PatternCompiler compiler = new Perl5Compiler();
			if(vedioStrInfo != null && vedioStrInfo.length()  > 0) {

				org.apache.oro.text.regex.Pattern patternDuration = compiler
						.compile(regexDuration, Perl5Compiler.CASE_INSENSITIVE_MASK);
				PatternMatcher matcherDuration = new Perl5Matcher();
				if (matcherDuration.contains(vedioStrInfo, patternDuration)) {
					org.apache.oro.text.regex.MatchResult re = matcherDuration
							.getMatch();
					obj.setDuration(re.group(1));
					obj.setStartTime(re.group(2));
					obj.setBitrate(re.group(3));
					logger.info("提取出播放时间  ===" + re.group(1));
					logger.info("开始时间        =====" + re.group(2));
					logger.info("bitrate 码率 单位 kb==" + re.group(3));
				}

				org.apache.oro.text.regex.Pattern patternVideo =
						compiler.compile(regexVideo,Perl5Compiler.CASE_INSENSITIVE_MASK);
				PatternMatcher matcherVideo = new Perl5Matcher();

				if(matcherVideo.contains(vedioStrInfo, patternVideo)){
					org.apache.oro.text.regex.MatchResult re = matcherVideo.getMatch();
					obj.setVideoCode(re.group(1));
					obj.setVideoFormat(re.group(2));
					obj.setResolutionRatio(re.group(3));

					

					
					logger.info("编码格式  ===" +re.group(1));
					logger.info("视频格式 ===" +re.group(2));
					logger.info("分辨率  ===" +re.group(3));
				}

				org.apache.oro.text.regex.Pattern patternAudio =
						compiler.compile(regexAudio,Perl5Compiler.CASE_INSENSITIVE_MASK);
				PatternMatcher matcherAudio = new Perl5Matcher();

				if(matcherAudio.contains(vedioStrInfo, patternAudio)){
					org.apache.oro.text.regex.MatchResult re = matcherAudio.getMatch();
					obj.setAudioCode(re.group(1));
					obj.setAudioFrequncy(re.group(2));
					logger.info("音频编码             ===" +re.group(1));
					logger.info("音频采样频率  ===" +re.group(2));
				}
				
				org.apache.oro.text.regex.Pattern fpsPatter = compiler
						.compile(regexFps, Perl5Compiler.CASE_INSENSITIVE_MASK);
				PatternMatcher fpsMather = new Perl5Matcher();
				if (fpsMather.contains(vedioStrInfo, fpsPatter)) {
					org.apache.oro.text.regex.MatchResult re = fpsMather
							.getMatch();
					
					logger.info("fps ===" +re.group(1));
					obj.setFps(re.group(1));
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return obj;
	}

	public static VideoInfo getVideoObj(File videoFile) {
		String videoInfoScriptStr = ffmpegInfo.replace("{videoFileName}", videoFile.getAbsolutePath()); 
		try {
			StringBuilder str = new StringBuilder();
			ProcessExeUtil.execScriptReturnOutputNotAsyn(videoInfoScriptStr, videoFile.getParentFile().getAbsolutePath(), 60, str);
			return getVideoObj(str.toString());	
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	
	public static String concat(List<File> videoFileList, Map<String,String> param) throws IOException, LabelSystemException {
		String destFileName = param.get("destFileName");
		String path = videoFileList.get(0).getParent();
		File destFile = new File(path,destFileName); 
		File txtFile = new File(path,"filelist.txt");
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(txtFile))){
			for(File file : videoFileList) {
				writer.write("file '" + file.getName() + "'");
				writer.newLine();
			}
		}
		String script = ffmpegConcat.replace("{fileListName}", txtFile.getAbsolutePath());
		script = script.replace("{destFileName}", destFile.getAbsolutePath());
		
		int time = 10 * 30;//10分钟
		
		exeCmd(videoFileList.get(0), script, time);
		
		return destFile.getAbsolutePath();
	}
	

	/**
	 * 抽帧
	 * @param videoFile 视频文件
	 * @param imagePath 抽帧后目的路径
	 * @param param 抽帧参数
	 * @throws LabelSystemException
	 */
	public static void chouZhen(File videoFile,String imagePath,Map<String,String> param) throws LabelSystemException {
		VideoInfo videoObj = VideoUtil.getVideoObj(videoFile);
		
		chouZhenForPath(videoObj, videoFile, param, imagePath);
		
	}
	
	/**
	 * 抽关键帧
	 * @param videoFile
	 * @param param
	 * @return
	 * @throws LabelSystemException 
	 */
	public static String chouZhen(VideoInfo videoObj, File videoFile,Map<String,String> param) throws LabelSystemException {
		String tmpFilePath = videoFile.getParentFile().getAbsolutePath() + File.separator + System.nanoTime() + File.separator;
		new File(tmpFilePath).mkdir();

		chouZhenForPath(videoObj, videoFile, param, tmpFilePath);
		
		return tmpFilePath;
	}


	private static void chouZhenForPath(VideoInfo videoObj, File videoFile, Map<String, String> param,
			String tmpFilePath) throws LabelSystemException {
		String drawFrameType = param.get("drawFrameType");
		String script = null;

		String fps = param.get("fps");
		if(fps == null) {
			fps = "0.5";//默认每2秒抽一帧
		}
		double fpsDouble = Double.parseDouble(fps);
		String videoName = videoFile.getName();
		videoName = videoName.substring(0,videoName.lastIndexOf("."));
		if(param.get("filenamePrefix") != null) {
			videoName = param.get("filenamePrefix");
		}
		
		String widthHeight = param.get("-s");
		if(widthHeight == null || widthHeight.length() == 0) {
			widthHeight = "";
		}else {
			widthHeight = " -s " + widthHeight;
		}
		
		if(drawFrameType != null) {
			if(Constants.CHOUZHEN_KEY_FRAME.equals(drawFrameType)) {
				String ffmpegKeyCmdStr = ffmpegKeyCmd.replace("{videoFileName}", videoFile.getAbsolutePath());
				script = ffmpegKeyCmdStr.replace("{pictureFileName}", tmpFilePath + videoName + "_");
				script = script.replace("{-s}", widthHeight);
			}else if(Constants.CHOUZHEN_PERSECOND_FRAME.equals(drawFrameType)) {
				String ffmpegCmdStr = ffmpegCmd.replace("{videoFileName}", videoFile.getAbsolutePath());
				script = ffmpegCmdStr.replace("{fps}", fps);
				script = script.replace("{pictureFileName}", tmpFilePath + videoName + "_");
				script = script.replace("{-s}", widthHeight);
			}
		}

		
		int exceedTime = getExceetTime(videoObj, fpsDouble);

		logger.info("chou key frame start.exceedTime=" + exceedTime + " s");
		exeCmd(videoFile, script, exceedTime);//默认最长15分钟。
		logger.info("chou key frame end.");

		//重新命名图片

		String fileNameFormate = param.get("fileNameFormate");
		if(fileNameFormate != null) {
			if(Constants.CHOUZHEN_FILE_NAME_FORMAT_TIME.equals(fileNameFormate) || Constants.CHOUZHEN_FILE_NAME_FORMAT_CAMERA_TIME.equals(fileNameFormate)) {
				String frameCsv = tmpFilePath + "frames.csv";
				String ffprobeCmdStr = ffprobeCmd.replace("{videoFileName}", videoFile.getAbsolutePath());
				ffprobeCmdStr = ffprobeCmdStr.replace("{csvFileName}", frameCsv);
				logger.info("output frame and time start.");
				exeCmd(videoFile, ffprobeCmdStr,2400);//半个小时
				logger.info("output frame and time end.");
				reNameFile(param, tmpFilePath, drawFrameType, fpsDouble, videoName, fileNameFormate, frameCsv);
			}
		}
	}


	public static int getExceetTime(VideoInfo videoObj, double fpsDouble) {
		int exceedTime = 15 * 60;
		if(videoObj != null) {//动态计算超时时间。
			int totalTimeSecond = getIntSecond(videoObj.getDuration());
			logger.info("vedio total time:" + totalTimeSecond);
			exceedTime = (int)(totalTimeSecond / (16 / fpsDouble)) + 3 * 60;//24小时
		}
		return exceedTime;
	}


	public static int getIntSecond(String duration) {
		int re = 0;
		int first = duration.indexOf(":");
		if(first != -1) {
			String hour = duration.substring(0,first);
			re += Integer.parseInt(hour) * 3600;
			int second = duration.indexOf(":", first + 1);
			if(second != -1) {
				String min = duration.substring(first + 1, second);
				re += Integer.parseInt(min) * 60;
			}
		}
		return re;
	}

	public static void reNameFile(Map<String, String> param, String tmpFilePath, String drawFrameType,
			double fpsDouble, String videoName, String fileNameFormate, String frameCsv) {
		List<String[]> framesList = readTimeForFrame(frameCsv);
		new File(frameCsv).delete();//删除csv文件。
		HashMap<Integer,String> numberMap = new HashMap<>();
		int count = 0;
		String cameraDate = param.get("cameraDate");
		if(Constants.CHOUZHEN_KEY_FRAME.equals(drawFrameType)) {
			for(String [] frames : framesList) {
				if(frames[3].equals("I")) {//关键帧
					numberMap.put(count++, convertToName(frames[1],fileNameFormate,cameraDate) + ".jpg");
				}
			}
		}else if(Constants.CHOUZHEN_PERSECOND_FRAME.equals(drawFrameType)) {
			double current = 0;
			fpsDouble = 1.0 / fpsDouble;
			double total = 0;
			int length = framesList.size();
			for(int i = 0; i < length; i++) {
				String frames[] = framesList.get(i);
				if(Math.abs(current - total) < 0.05 || i == length - 1) {
					total += fpsDouble;
					numberMap.put(count++, convertToName(frames[1],fileNameFormate,cameraDate) + ".jpg");
				}
				current = Double.parseDouble(frames[1]);
			}
		}
		List<File> pictureList = FileUtil.getAllFileList(tmpFilePath);
		List<String> nameList = new ArrayList<>();
		for(File file : pictureList) {
			nameList.add(file.getName());
		}
		logger.info("the draw frames picture total=" + nameList.size());
		Collections.sort(nameList);
		int tmpLen = nameList.size();
		for(int i = 0; i < tmpLen; i++) {
			String oldFileName = nameList.get(i);
			String newFileName = numberMap.get(i);
			if(newFileName != null) {
				newFileName = videoName + "_" + newFileName;
				File tmpFile = new File(tmpFilePath,oldFileName);
				tmpFile.renameTo(new File(tmpFilePath,newFileName));
			}
		}
	}



	private static String getTwoNumber(int a) {
		if(a < 10) {
			return "0" + a;
		}else {
			return String.valueOf(a);
		}
	}

	public static String convertToName(String pkt_pts_time, String fileNameFormate, String cameraDate) {
		double time = Double.parseDouble(pkt_pts_time);
		int hour = (int)time / 3600;
		int minute = ((int)time - hour * 3600)/60;
		int second = (int)time - minute * 60 - hour * 3600;
		String dotNum = "000";
		if(pkt_pts_time.indexOf(".") != -1) {
			dotNum = pkt_pts_time.substring(pkt_pts_time.indexOf(".") + 1);
			if(dotNum.length() > 3) {
				dotNum = dotNum.substring(0,3);
			}
		}
		Date date = null;
		if(cameraDate != null) {
			date = getRightDate(cameraDate);
		}
		if(Constants.CHOUZHEN_FILE_NAME_FORMAT_TIME.equals(fileNameFormate)) {
			return getTwoNumber(hour)  + getTwoNumber(minute) + getTwoNumber(second) +"_" + dotNum;

		}else if(Constants.CHOUZHEN_FILE_NAME_FORMAT_CAMERA_TIME.equals(fileNameFormate)) {
			if(date != null) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				calendar.add(Calendar.HOUR_OF_DAY, hour);
				calendar.add(Calendar.MINUTE, minute);
				calendar.add(Calendar.SECOND, second);
				calendar.add(Calendar.MILLISECOND, Integer.parseInt(dotNum));
				date = calendar.getTime();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
				return sdf.format(date);
			}else {
				return getTwoNumber(hour)  + getTwoNumber(minute) + getTwoNumber(second) +"_" + dotNum;
			}
		}
		return null;
	}

	private static Date getRightDate(String cameraDate) {
		String dataFormat[] = {"yyyy-MM-dd HH:mm:ss","yyyyMMddHHmmss","yyyyMMdd HH:mm:ss"};
		for(String format : dataFormat) {
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			try {
				return sdf.parse(cameraDate);
			} catch (ParseException e) {
				logger.info("parse error, cameraDate=" + cameraDate  + " format=" + format);
				logger.info( e.getMessage());
			}
		}
		return null;
	}

	private static List<String[]> readTimeForFrame(String frameCsv) {

		List<String> allLines = FileUtil.getAllLineList(frameCsv, "utf-8");
		List<String[]> re = new ArrayList<>();
		for(String tmp : allLines) {
			String[] tmps = tmp.split(",");
			re.add(tmps);
		}
		return re;
	}

	private static void exeCmd(File videoFile, String script, int timeSeconds) throws LabelSystemException {
		String os = System.getProperty("os.name"); 
		if(!os.toLowerCase().startsWith("win")){
			logger.info("start runtime exe script." + script);
			File outputFile = videoFile.getParentFile();

			ProcessExeUtil.execScript(script, outputFile.getAbsolutePath(), timeSeconds);

		}
	}



	public static void main(String[] args) throws MalformedPatternException {
//		Map<String,String> param = new HashMap<>();
//		param.put("cameraDate", "2020-02-12 14:30:30");
//
//		String tmpFilePath = "D:\\avi\\newimage\\";
//
//		String drawFrameType = "1";
//
//		double fpsDouble = 2;
//
//		String videoName = "test";
//
//		String fileNameFormate = "2";
//
//		String frameCsv = "D:\\avi\\frames.csv";
//
//		reNameFile(param, tmpFilePath, drawFrameType, fpsDouble, videoName, fileNameFormate, frameCsv);
	
		String vedioStrInfo = "Input #0, mov,mp4,m4a,3gp,3g2,mj2, from '/home/label/userdataset/video_5444471412718824/ch420190304162955-20190304201031.mp4':\r\n" + 
				"  Metadata:\r\n" + 
				"    major_brand     : isom\r\n" + 
				"    minor_version   : 0\r\n" + 
				"    compatible_brands: mp41avc1\r\n" + 
				"    creation_time   : 2020-01-13T11:09:03.000000Z\r\n" + 
				"    encoder         : vlc 3.0.5 stream output\r\n" + 
				"    encoder-eng     : vlc 3.0.5 stream output\r\n" + 
				"  Duration: 01:58:23.80, start: 0.000000, bitrate: 839 kb/s\r\n" + 
				"    Stream #0:0(eng): Audio: mp3 (mp4a / 0x6134706D), 44100 Hz, stereo, s16p, 127 kb/s (default)\r\n" + 
				"    Metadata:\r\n" + 
				"      creation_time   : 2020-01-13T11:09:03.000000Z\r\n" + 
				"      handler_name    : SoundHandler\r\n" + 
				"    Stream #0:1(eng): Video: hevc (Main) (hev1 / 0x31766568), yuv420p(tv), 1280x720, 704 kb/s, 25.63 fps, 25 tbr, 90k tbn, 25 tbc (default)\r\n" + 
				"    Metadata:\r\n" + 
				"      creation_time   : 2020-01-13T11:09:03.000000Z\r\n" + 
				"      handler_name    : VideoHandler\r\n" + 
				"";
		
		
		vedioStrInfo = "Input #0, matroska,webm, from 'D:\\avi\\00000746.mkv':\r\n" + 
				"  Metadata:\r\n" + 
				"    encoder         : libebml v1.3.7 + libmatroska v1.5.0\r\n" + 
				"    creation_time   : 2019-07-11T07:48:31.000000Z\r\n" + 
				"  Duration: 01:30:01.22, start: 0.000000, bitrate: 3129 kb/s\r\n" + 
				"    Stream #0:0: Video: hevc (Main), yuv420p(tv, bt709), 1920x804, SAR 1:1 DAR 160:67, 24 fps, 24 tbr, 1k tbn, 24 tbc (default)\r\n" + 
				"    Metadata:\r\n" + 
				"      BPS-eng         : 2870267\r\n" + 
				"      DURATION-eng    : 01:30:01.167000000\r\n" + 
				"      NUMBER_OF_FRAMES-eng: 129628\r\n" + 
				"      NUMBER_OF_BYTES-eng: 1937849377\r\n" + 
				"      _STATISTICS_WRITING_APP-eng: mkvmerge v32.0.0 ('Astral Progressions') 64-bit\r\n" + 
				"      _STATISTICS_WRITING_DATE_UTC-eng: 2019-07-11 07:48:31\r\n" + 
				"      _STATISTICS_TAGS-eng: BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES\r\n" + 
				"    Stream #0:1(chi): Audio: aac (LC), 48000 Hz, stereo, fltp (default)\r\n" + 
				"    Metadata:\r\n" + 
				"      title           : 鍥借\r\n" + 
				"      BPS-eng         : 127871\r\n" + 
				"      DURATION-eng    : 01:30:01.216000000\r\n" + 
				"      NUMBER_OF_FRAMES-eng: 253182\r\n" + 
				"      NUMBER_OF_BYTES-eng: 86332370\r\n" + 
				"      _STATISTICS_WRITING_APP-eng: mkvmerge v32.0.0 ('Astral Progressions') 64-bit\r\n" + 
				"      _STATISTICS_WRITING_DATE_UTC-eng: 2019-07-11 07:48:31\r\n" + 
				"      _STATISTICS_TAGS-eng: BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES\r\n" + 
				"    Stream #0:2(chi): Audio: aac (LC), 48000 Hz, stereo, fltp\r\n" + 
				"    Metadata:\r\n" + 
				"      title           : 绮よ\r\n" + 
				"      BPS-eng         : 127943\r\n" + 
				"      DURATION-eng    : 01:30:01.216000000\r\n" + 
				"      NUMBER_OF_FRAMES-eng: 253182\r\n" + 
				"      NUMBER_OF_BYTES-eng: 86381325\r\n" + 
				"      _STATISTICS_WRITING_APP-eng: mkvmerge v32.0.0 ('Astral Progressions') 64-bit\r\n" + 
				"      _STATISTICS_WRITING_DATE_UTC-eng: 2019-07-11 07:48:31\r\n" + 
				"      _STATISTICS_TAGS-eng: BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES";
		
		VideoInfo videoObj = getVideoObj(vedioStrInfo);
		
		System.out.println(videoObj.getDuration());
		
		System.out.println(getIntSecond(videoObj.getDuration()));
		
		PatternCompiler compiler = new Perl5Compiler();
		
		org.apache.oro.text.regex.Pattern patternDuration = compiler
				.compile(regexFps, Perl5Compiler.CASE_INSENSITIVE_MASK);
		PatternMatcher matcherDuration = new Perl5Matcher();
		if (matcherDuration.contains(vedioStrInfo, patternDuration)) {
			org.apache.oro.text.regex.MatchResult re = matcherDuration
					.getMatch();
			
			logger.info("fps ===" +re.group(1));
			
		}
	}

}
