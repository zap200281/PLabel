package com.pcl.service.schedule;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.pcl.constant.LogConstants;
import com.pcl.dao.LogInfoDao;
import com.pcl.dao.LogInfoHistoryDao;
import com.pcl.dao.ReportLabelTaskDao;
import com.pcl.pojo.mybatis.LogInfo;
import com.pcl.pojo.mybatis.ReportLabelTask;
import com.pcl.service.LabelDataSetMerge;
import com.pcl.util.FileUtil;
import com.pcl.util.JsonUtil;
import com.pcl.util.LabelInfoUtil;
import com.pcl.util.TimeUtil;


@Configuration      //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling   // 2.开启定时任务
public class ReportSchedule {

	@Autowired
	private LogInfoDao logInfoDao;
	
	@Autowired
	private LogInfoHistoryDao logInfoHistoryDao;

	@Autowired
	private ReportLabelTaskDao reportLabelTaskDao;

	private static Logger logger = LoggerFactory.getLogger(ReportSchedule.class);

	@Scheduled(cron = "0 0 6 ? * MON")//星期天凌晨6点执行一次。
	private void clearTmpFile() {
		long start = System.currentTimeMillis();
		logger.info("start to clear tmp file.");
		String tmpFilePath = LabelDataSetMerge.getUserDataSetPath();
		File tmpFile = new File(tmpFilePath);
		if(tmpFile.exists()) {
			File files[] = tmpFile.listFiles();
			for(File file : files) {
				logger.info("delete tmp dir:" + file.getAbsolutePath());
				FileUtil.delDir(file.getAbsolutePath());
			}
		}
		
		logger.info("clear history finished. cost=" + (System.currentTimeMillis() - start) / 1000 + " s.");
	}
	

	@Scheduled(cron = "0 0 6 * * ?")//每天凌晨6点执行一次。
	private void produceHistoryRecord() {
		long start = System.currentTimeMillis();
		
		logger.info("start to move log file.");
		int logFileMoveCount = 0;
		String userDir = System.getProperty("user.dir");
		if(new File(userDir).exists()) {
			File files[] = new File(userDir).listFiles();
			String destPath = userDir + File.separator + "logHistory" +File.separator;
			new File(destPath).mkdirs();
			if(files != null) {
				for(File file : files) {
					if(file.isFile() && file.getName().endsWith(".gz")) {
						FileUtil.copyFile(file.getAbsolutePath(), destPath + file.getName());
						file.delete();
						logFileMoveCount ++;
						logger.info("move file:" + file.getName() + " succeed.");
					}
				}
			}
		}
		logger.info("end to move log file. count=" + logFileMoveCount);
		
		
		//1、清除半年前的记录。
		//2、将7天前的日志，移动到history表中
		//3、将下载的临时文件目录清除。
		Map<String,Object> historyParamMap = new HashMap<>();
		int day = 180;
		historyParamMap.put("day", day);
		
		
		int count = logInfoHistoryDao.queryLogInfoPageForBeforeDayCount(historyParamMap);
		logger.info("start delete 180 days before records. size=" + count);
		int pageSize = 100;
		for(int i = 0; i < (count/pageSize) +1; i++) {
			historyParamMap.put("currPage", i * pageSize);
			historyParamMap.put("pageSize", pageSize);
			List<LogInfo> logList = logInfoHistoryDao.queryLogInfoPageForBeforeDay(historyParamMap);
			//删除180天之前的历史记录。
			for(LogInfo log : logList) {
				logInfoHistoryDao.deleteLogInfo(log.getId());
			}
		}
		
		
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("day", 7);
		
		count = logInfoDao.queryLogInfoPageForBeforeDayCount(paramMap);
		logger.info("start move 7 days before record to history. size=" + count);
		pageSize = 100;
		List<String> idList = new ArrayList<>();
		for(int i = 0; i < (count/pageSize) +1; i++) {
			paramMap.put("currPage", i * pageSize);
			paramMap.put("pageSize", pageSize);
			List<LogInfo> logList = logInfoDao.queryLogInfoPageForBeforeDay(paramMap);
			
			for(LogInfo log : logList) {
				//先将7天添加到历史表。
				logInfoHistoryDao.addLogInfo(log);
				idList.add(log.getId());
			}
		}
		
		logger.info("start move 7 days before record to history. real delete size=" + idList.size());
		for(String id : idList) {
			logInfoDao.deleteLogInfo(id);
		}
		
		logger.info("finished task,cost=" + (System.currentTimeMillis() - start));

	}
	
	

	//@Scheduled(cron = "59 40 * * * ?")
	@Scheduled(cron = "0 0 */4 * * ?")//每隔4个小时执行一次。
	//@Scheduled(cron = "*/30 * * * * ?")//每隔30秒执行一次。
	private void produceReport() {
		logger.info("start to exe produceReport: " + LocalDateTime.now());
		
		long start = System.currentTimeMillis();
		Map<String,Object> paramMap = new HashMap<>();
		//int day = 1;
		
		paramMap.put("startTime", TimeUtil.getCurrentDayStart());
		paramMap.put("endTime", TimeUtil.getCurrentDayEnd());
		//paramMap.put("user_id",5);
		int pageSize = 100;
		int count = logInfoDao.queryLogInfoPageForDayCount(paramMap);
		int total = count / pageSize + 1;
		logger.info("need deal records: " + count);
		HashSet<String> repeatPictureSet = new HashSet<>();
		//收集同一天对同一张图片进行多次操作情况。
		//collectRepeatCommitLog(paramMap, pageSize, total, repeatLogMap);
		//logger.info("it has " + repeatLogMap.size() + " repeat to a picture log.");
		
		Map<String,Map<String,Integer>> userDataMap = new HashMap<>();
		//dealRepeatCommitLog(repeatLogMap, userDataMap);
		
		int totalRecordCount = 0;
		logger.info("total=" + total);
		for(int i = 0; i < total; i++) {
			paramMap.put("currPage", i * pageSize);
			paramMap.put("pageSize", pageSize);
			List<LogInfo> list = logInfoDao.queryLogInfoPageForDay(paramMap);
			logger.info("record size=" + list.size());
			totalRecordCount += list.size();
			for(LogInfo log : list) {
				if(isLabelTask(log)) {
					String date = log.getOper_time_start().substring(0,10);
					
					//String pictureMapKey = log.getRecord_id() + "_" +  date + "_" + log.getUser_id();
					//if(repeatLogMap.containsKey(pictureMapKey)) {
					//	continue;
					//}
					Map<String,Integer> result = getResult(log);
//					if(date.equals("2020-06-04")) {
//						logger.info(log.getOper_id() + " data=" + result.toString());
//					}
					
					String labelUserId = dealNotValide(log);
					if(labelUserId != null && !labelUserId.isEmpty()) {
						//将不合格的数量加到对应用户的数据中
						Map<String,Integer> notValideResult = new HashMap<>();
						int notValide = 0;
						if(result.containsKey(LogConstants.NOT_VALIDE)) {
							notValide = result.get(LogConstants.NOT_VALIDE);
						}
						notValideResult.put(LogConstants.NOT_VALIDE, notValide);
						//logger.info("add not valid to userid: " + labelUserId + " notValide=" + notValide + " date=" + date);
						addUserDataToMap(labelUserId + "_" + date, userDataMap, notValideResult);
					}
					//判断图片是否有重复，如果有重复，则不重复添加。
					String userDateKey = log.getUser_id() + "_" + date;
					if(result.get(LogConstants.PICTUREUPDATE) > 0) {
						if(log.getExtend2() != null) {
							String pictureKey = userDateKey + "_" + log.getExtend2();
							if(repeatPictureSet.contains(pictureKey)) {
								result.put(LogConstants.PICTUREUPDATE, 0);
							}else {
								repeatPictureSet.add(pictureKey);
							}
						}else {
							result.put(LogConstants.PICTUREUPDATE, 0);
						}
					}
					addUserDataToMap(userDateKey, userDataMap, result);
				}
			}
		}
		
		logger.info("totalRecordCount=" + totalRecordCount);
		//logger.info("userData=" + userDataMap.toString());
		saveToDb(userDataMap);
		logger.info("finished produce report,cost=" + (System.currentTimeMillis() - start) + " ms");
	}
	
	private void addUserDataToMap(String userDateKey,Map<String,Map<String,Integer>> userDataMap,Map<String,Integer> result) {
		Map<String,Integer> tmp = userDataMap.get(userDateKey);
		if(tmp == null) {
			userDataMap.put(userDateKey, result);
		}else {
			addMap(result,tmp);
		}
	}

	private String dealNotValide(LogInfo log) {
		if(log.getExtend1() != null && log.getExtend1().length() >0) {
			//审核状态更新，需要找到原始的标注用户
			Map<String,String> tmp = JsonUtil.getStrMap(log.getExtend1()) ;
			if(tmp.get("label_user_id") != null) {
				logger.info("tmp=" + tmp.toString());
				return tmp.get("label_user_id");
			}
		}
		return "";
	}

	private void dealRepeatCommitLog(Map<String, HashSet<String>> repeatLogMap,
			Map<String, Map<String, Integer>> userDataMap) {
		for(Entry<String,HashSet<String>> entry : repeatLogMap.entrySet()) {
			List<String> idList = new ArrayList<>();
			idList.addAll(entry.getValue());
			List<LogInfo> list = logInfoDao.queryLogInfoByIdList(idList);
			LogInfo startLogInfo = list.get(0);
			LogInfo endLogInfo = list.get(list.size() - 1);
			
			startLogInfo.setOper_json_content_new(endLogInfo.getOper_json_content_new());
			
			Map<String,Integer> result = getResult(startLogInfo);
			
			String date = startLogInfo.getOper_time_start().substring(0,10);
			
			String userDateKey = startLogInfo.getUser_id() + "_" + date;
			
			Map<String,Integer> tmp = userDataMap.get(userDateKey);
			if(tmp == null) {
				userDataMap.put(userDateKey, result);
			}else {
				addMap(result,tmp);
			}
		}
	}

	private void saveToDb(Map<String, Map<String, Integer>> userMap) {
		
		logger.info("userData=" + userMap.toString());
		
		for(Entry<String,Map<String,Integer>> entry : userMap.entrySet()) {
			
			ReportLabelTask reportLabelTask = new ReportLabelTask();
			String dateKey = entry.getKey();
			String user_id_str = dateKey.substring(0,dateKey.indexOf("_"));
			String date = dateKey.substring(dateKey.indexOf("_") +1);

			reportLabelTask.setUser_id(Integer.parseInt(user_id_str));
			reportLabelTask.setOper_time(date);
			reportLabelTask.setPictureUpdate(getItemValue(entry.getValue(),LogConstants.PICTUREUPDATE));
			reportLabelTask.setProperties(getItemValue(entry.getValue(),LogConstants.PROPERTIES));
			reportLabelTask.setRectAdd(getItemValue(entry.getValue(),LogConstants.RECTADD));
			reportLabelTask.setRectUpdate(getItemValue(entry.getValue(),LogConstants.RECTUPDATE));
			reportLabelTask.setNotValide(getItemValue(entry.getValue(),LogConstants.NOT_VALIDE));
			
			Map<String,Object> tmpParamMap = new HashMap<>();
			tmpParamMap.put("user_id", reportLabelTask.getUser_id());
			tmpParamMap.put("oper_time", reportLabelTask.getOper_time());
			List<ReportLabelTask> existTask = reportLabelTaskDao.queryReportLabelTask(tmpParamMap);
			if(existTask != null && existTask.size() > 0) {
				reportLabelTaskDao.deleteReportLabelTask(tmpParamMap);
			}
			reportLabelTaskDao.addReportLabelTask(reportLabelTask);
		}
	}
	
	private int getItemValue(Map<String,Integer> map,  String key) {
		if(map.containsKey(key)) {
			return map.get(key);
		}
		return 0;
	}

	private void collectRepeatCommitLog(Map<String, Object> paramMap, int pageSize, int total,
			Map<String, HashSet<String>> repeatLogMap) {
		for(int i = 0; i < total; i++) {
			paramMap.put("currPage", i * pageSize);
			paramMap.put("pageSize", pageSize);
			List<LogInfo> list = logInfoDao.queryLogInfoPageForDay(paramMap);
		
			for(LogInfo log : list) {
				if(isLabelTask(log)) {
					String date = log.getOper_time_start().substring(0,10);
					String pictureMapKey = log.getRecord_id() + "_" +  date + "_" + log.getUser_id();
					if(repeatLogMap.containsKey(pictureMapKey)){
						repeatLogMap.get(pictureMapKey).add(log.getId());
					}else {
						HashSet<String> set = new HashSet<>();
						set.add(log.getId());
						repeatLogMap.put(pictureMapKey, set);
					}
				}
			}
		}
		List<String> keyList = new ArrayList<>();
		keyList.addAll(repeatLogMap.keySet());
		for(String key : keyList) {
			if(repeatLogMap.get(key).size() <= 1) {
				repeatLogMap.remove(key);
			}
		}
	}

	private void addMap(Map<String, Integer> result, Map<String, Integer> tmp) {
		for(Entry<String,Integer> entry : result.entrySet()) {
			if(tmp.containsKey(entry.getKey())) {
				tmp.put(entry.getKey(), tmp.get(entry.getKey()) + entry.getValue());
			}else {
				tmp.put(entry.getKey(), entry.getValue());
			}
		}
	}

	private Map<String,Integer> getResult(LogInfo log){

		String oldLabelInfo = log.getOper_json_content_old();
		String newLabelInfo = log.getOper_json_content_new();
		List<Map<String,Object>> oldLabelList = null;
		List<Map<String,Object>> newLabelList = null;
		if(LogConstants.LOG_VEDIO_COUNT_LABEL_ITEM.equals(log.getOper_id())) {
			oldLabelList = Arrays.asList(JsonUtil.getMap(oldLabelInfo));
			newLabelList = Arrays.asList(JsonUtil.getMap(newLabelInfo));
		}else {
			oldLabelList = JsonUtil.getLabelList(oldLabelInfo);
			newLabelList = JsonUtil.getLabelList(newLabelInfo);
		}
		

		return LabelInfoUtil.getCompareResult(oldLabelList, newLabelList);
	}



	private boolean isLabelTask(LogInfo log) {

		return LogConstants.LOG_NORMAL_UPDATE_LABEL_ITEM.equals(log.getOper_id()) || 
				LogConstants.LOG_DCM_UPDATE_LABEL_ITEM.equals(log.getOper_id()) ||
				LogConstants.LOG_REID_UPDATE_LABEL_ITEM.equals(log.getOper_id()) ||
				LogConstants.LOG_VEDIO_COUNT_LABEL_ITEM.equals(log.getOper_id()) ||
				LogConstants.LOG_VEDIO_LABEL_TASK_ITEM.equals(log.getOper_id());

	}

	public static void main(String[] args) {
		//String oldJson = "[{\"class_name\":\"car\",\"score\":1,\"box\":[1490,1132,1622,1302],\"id\":\"1\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"1\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[1635,1873,1820,2148],\"id\":\"2\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"2\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[1659,941,1767,1080],\"id\":\"3\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"3\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2070,1061,2178,1222],\"id\":\"4\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"4\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[3646,1657,3803,1722],\"id\":\"5\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"5\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[1829,1512,1968,1719],\"id\":\"6\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"6\"}}},{\"class_name\":\"6\",\"score\":1,\"box\":[1302,1413,1465,1629],\"id\":\"7\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"6\",\"id\":\"7\",\"color\":\"5\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[3239,1595,3396,1663],\"id\":\"8\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"8\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2779,1503,2924,1564],\"id\":\"9\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"9\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[3439,1623,3606,1697],\"id\":\"10\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"10\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2594,1478,2733,1537],\"id\":\"11\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"11\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2427,1429,2557,1493],\"id\":\"12\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"12\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[3528,1574,3670,1639],\"id\":\"13\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"13\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2181,1432,2316,1493],\"id\":\"14\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"14\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[3353,1561,3488,1626],\"id\":\"15\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"15\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2579,43,2606,71],\"id\":\"16\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"16\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2245,1407,2378,1469],\"id\":\"17\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"17\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2579,71,2603,99],\"id\":\"18\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"18\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[3035,1456,3306,1580],\"id\":\"19\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"19\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2579,80,2606,108],\"id\":\"20\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"20\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[3023,1546,3174,1614],\"id\":\"21\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"21\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2597,34,2622,59],\"id\":\"22\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"22\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2594,19,2619,43],\"id\":\"23\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"23\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2594,71,2622,96],\"id\":\"24\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"24\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2363,1456,2501,1515],\"id\":\"25\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"25\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[3485,1595,3646,1663],\"id\":\"26\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"26\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2560,71,2588,99],\"id\":\"27\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"27\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2511,219,2545,250],\"id\":\"28\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"28\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[1647,1176,1712,1247],\"id\":\"29\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"29\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2215,1423,2350,1484],\"id\":\"30\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"30\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2563,56,2588,83],\"id\":\"31\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"31\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2582,28,2609,56],\"id\":\"32\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"32\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2301,179,2335,210],\"id\":\"33\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"33\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2582,1493,2717,1552],\"id\":\"34\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"34\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2606,19,2631,43],\"id\":\"35\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"35\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2403,1441,2538,1503],\"id\":\"36\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"36\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[3297,1571,3454,1639],\"id\":\"37\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"37\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2360,500,2406,543],\"id\":\"38\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"38\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2282,700,2329,744],\"id\":\"39\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"39\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2477,336,2517,370],\"id\":\"40\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"40\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2597,80,2622,108],\"id\":\"41\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"41\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2979,1475,3091,1537],\"id\":\"42\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"42\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2353,1472,2486,1531],\"id\":\"43\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"43\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2591,86,2619,114],\"id\":\"44\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"44\"}}}]";
		//String newJson = "[{\"class_name\":\"car\",\"score\":1,\"box\":[1490,1132,1622,1302],\"id\":\"1\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"1\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[1635,1873,1820,2148],\"id\":\"2\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"2\"}}},{\"class_name\":\"3\",\"score\":1,\"box\":[1659,941,1767,1080],\"id\":\"3\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"3\",\"id\":\"3\",\"color\":\"2\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2070,1061,2178,1222],\"id\":\"4\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"4\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[3646,1657,3803,1722],\"id\":\"5\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"5\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[1829,1512,1968,1719],\"id\":\"6\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"6\"}}},{\"class_name\":\"6\",\"score\":1,\"box\":[1302,1413,1465,1629],\"id\":\"7\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"6\",\"id\":\"7\",\"color\":\"5\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[3239,1595,3396,1663],\"id\":\"8\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"8\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2779,1503,2924,1564],\"id\":\"9\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"9\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[3439,1623,3606,1697],\"id\":\"10\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"10\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2594,1478,2733,1537],\"id\":\"11\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"11\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2427,1429,2557,1493],\"id\":\"12\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"12\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[3528,1574,3670,1639],\"id\":\"13\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"13\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2181,1432,2316,1493],\"id\":\"14\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"14\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[3353,1561,3488,1626],\"id\":\"15\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"15\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2579,43,2606,71],\"id\":\"16\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"16\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2245,1407,2378,1469],\"id\":\"17\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"17\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2579,71,2603,99],\"id\":\"18\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"18\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[3035,1456,3306,1580],\"id\":\"19\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"19\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2579,80,2606,108],\"id\":\"20\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"20\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[3023,1546,3174,1614],\"id\":\"21\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"21\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2597,34,2622,59],\"id\":\"22\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"22\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2594,19,2619,43],\"id\":\"23\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"23\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2594,71,2622,96],\"id\":\"24\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"24\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2363,1456,2501,1515],\"id\":\"25\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"25\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[3485,1595,3646,1663],\"id\":\"26\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"26\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2560,71,2588,99],\"id\":\"27\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"27\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2511,219,2545,250],\"id\":\"28\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"28\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[1647,1176,1712,1247],\"id\":\"29\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"29\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2215,1423,2350,1484],\"id\":\"30\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"30\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2563,56,2588,83],\"id\":\"31\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"31\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2582,28,2609,56],\"id\":\"32\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"32\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2301,179,2335,210],\"id\":\"33\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"33\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2582,1493,2717,1552],\"id\":\"34\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"34\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2606,19,2631,43],\"id\":\"35\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"35\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2403,1441,2538,1503],\"id\":\"36\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"36\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[3297,1571,3454,1639],\"id\":\"37\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"37\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2360,500,2406,543],\"id\":\"38\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"38\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2282,700,2329,744],\"id\":\"39\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"39\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2477,336,2517,370],\"id\":\"40\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"40\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2597,80,2622,108],\"id\":\"41\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"41\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2979,1475,3091,1537],\"id\":\"42\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"42\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2353,1472,2486,1531],\"id\":\"43\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"43\"}}},{\"class_name\":\"car\",\"score\":1,\"box\":[2591,86,2619,114],\"id\":\"44\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"car\",\"id\":\"44\"}}}]";
		String oldJson = "[{\"class_name\":\"person\",\"score\":1,\"box\":[491,6,575,180],\"id\":\"0\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"0\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[171,188,477,621],\"id\":\"1\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"1\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[611,6,694,181],\"id\":\"2\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"2\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[768,219,903,379],\"id\":\"3\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"3\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[54,351,377,697],\"id\":\"4\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"4\",\"verify\":\"1\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[225,174,452,458],\"id\":\"5\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"5\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[659,218,968,648],\"id\":\"6\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"6\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[780,229,951,429],\"id\":\"7\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"7\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[463,5,542,179],\"id\":\"8\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"8\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[646,10,725,181],\"id\":\"9\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"9\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[243,217,368,417],\"id\":\"10\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"10\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[519,7,607,181],\"id\":\"11\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"11\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[423,389,706,712],\"id\":\"13\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"13\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[504,2,598,298],\"id\":\"14\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"14\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[585,4,663,191],\"id\":\"15\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"15\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[486,96,698,559],\"id\":\"16\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"16\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[794,213,899,322],\"id\":\"17\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"17\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[265,211,403,493],\"id\":\"18\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"18\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[610,337,908,699],\"id\":\"19\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"19\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[554,1,692,219],\"id\":\"20\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"20\"}}}]";
		String newJson = "[{\"class_name\":\"person\",\"score\":1,\"box\":[491,6,575,180],\"id\":\"0\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"0\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[171,188,477,621],\"id\":\"1\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"1\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[611,6,694,181],\"id\":\"2\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"2\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[768,219,903,379],\"id\":\"3\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"3\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[54,351,377,697],\"id\":\"4\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"4\",\"verify\":\"1\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[225,174,452,458],\"id\":\"5\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"5\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[659,218,968,648],\"id\":\"6\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"6\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[780,229,951,429],\"id\":\"7\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"7\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[463,5,542,179],\"id\":\"8\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"8\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[646,10,725,181],\"id\":\"9\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"9\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[243,217,368,417],\"id\":\"10\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"10\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[519,7,607,181],\"id\":\"11\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"11\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[423,389,706,712],\"id\":\"13\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"13\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[504,2,598,298],\"id\":\"14\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"14\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[585,4,663,191],\"id\":\"15\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"15\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[486,96,698,559],\"id\":\"16\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"16\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[794,213,899,322],\"id\":\"17\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"17\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[265,211,403,493],\"id\":\"18\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"18\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[610,337,908,699],\"id\":\"19\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"19\",\"verify\":\"1\"}}},{\"class_name\":\"person\",\"score\":1,\"box\":[554,1,692,219],\"id\":\"20\",\"blurred\":false,\"goodIllumination\":true,\"frontview\":true,\"other\":{\"region_attributes\":{\"type\":\"person\",\"id\":\"20\"}}}]";
		
		LogInfo logInfo = new LogInfo();
		logInfo.setOper_json_content_old(oldJson);
		logInfo.setOper_json_content_new(newJson);
		logInfo.setUser_id(5);

		ReportSchedule rep = new ReportSchedule();
		
		Map<String,Integer> re = rep.getResult(logInfo);

		System.out.println(re);
	}

}
