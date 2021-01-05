package com.pcl.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.pcl.constant.Constants;
import com.pcl.dao.ReportLabelTaskDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.PageResult;
import com.pcl.pojo.body.ReportBody;
import com.pcl.pojo.body.ReportMeasureBody;
import com.pcl.pojo.display.DisplayReportMeasure;
import com.pcl.pojo.display.MeasureData;
import com.pcl.pojo.mybatis.ReportLabelTask;
import com.pcl.pojo.mybatis.User;

@Service
public class ReportService {

	@Autowired
	private ReportLabelTaskDao reportLabelTaskDao;

	@Autowired
	private UserService userService;
	
	private Gson gson = new Gson();

	private static Logger logger = LoggerFactory.getLogger(ReportService.class);

	
	public PageResult queryReportPage(String token, ReportBody body) throws LabelSystemException {

		logger.info("query report ,body =" + gson.toJson(body));
		
		PageResult pageResult = new PageResult();
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		
		User user = userService.queryUserById(userId);
		
		if(user == null) {
			throw new LabelSystemException("非法用户。");
		}
		int currPage = body.getStartPage();
		int pageSize = body.getPageSize();
		
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("currPage", currPage * pageSize);
		paramMap.put("pageSize", pageSize);
		if(user.getIs_superuser() != Constants.USER_SUPER) {//不是超级用户，只能看自己的报表
			paramMap.put("user_id_list",  Arrays.asList(userId));
		}else {
			if(body.getUser_id() != null && body.getUser_id().size() > 0) {
				paramMap.put("user_id_list", body.getUser_id());
			}
		}
		if(!Strings.isEmpty(body.getLastDay())) {
			paramMap.put("day", Integer.parseInt(body.getLastDay()));
		}
		if(!Strings.isEmpty(body.getStartTime())  && !Strings.isEmpty(body.getEndTime())) {
			paramMap.put("startTime", body.getStartTime());
			paramMap.put("endTime", body.getEndTime());
		}
	
		List<ReportLabelTask> reportTaskList = reportLabelTaskDao.queryAllReportLabelTaskPage(paramMap);

		int totalCount = reportLabelTaskDao.queryAllReportLabelTaskPageCount(paramMap);

		Map<Integer,String> userIdNameMap = userService.getAllUser();
		for(ReportLabelTask re : reportTaskList) {
			re.setUser_name(userIdNameMap.get(re.getUser_id()));
			re.setOper_time(re.getOper_time().substring(0,10));
		}
		pageResult.setTotal(totalCount);
		pageResult.setData(reportTaskList);
		pageResult.setCurrent(currPage);
		
		return pageResult;
	}

	private static List<List<String>> getDaysList(LocalDate localDate,int measureType,int measureValue) {
		List<List<String>> re = new ArrayList<>();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		if(measureType == Constants.REPORT_MEASURE_TYPE_DAY) {
			LocalDate newlocalDate = localDate.minusDays(measureValue - 1);
			for(int i =0; i < measureValue; i++) {
				List<String> tmp = new ArrayList<>();
				tmp.add(newlocalDate.format(formatter));
				newlocalDate = newlocalDate.plusDays(1);
				re.add(tmp);
			}
		}else if(measureType == Constants.REPORT_MEASURE_TYPE_WEEK) {
			LocalDate previousOrSameMonday = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			previousOrSameMonday = previousOrSameMonday.minusWeeks(measureValue - 1);
			for(int i = 0; i<measureValue; i++) {
				List<String> tmp = new ArrayList<>();
				for(int j = 0;j < 7; j++) {
					tmp.add(previousOrSameMonday.format(formatter));
					if(previousOrSameMonday.compareTo(localDate) == 0) {
						break;
					}
					previousOrSameMonday = previousOrSameMonday.plusDays(1);
				}
				re.add(tmp);
			}
		}else if(measureType == Constants.REPORT_MEASURE_TYPE_MONTH) {
			
			LocalDate firstDayOfMonth = localDate.with(TemporalAdjusters.firstDayOfMonth());
			firstDayOfMonth = firstDayOfMonth.minusMonths(measureValue - 1);
			LocalDate secondDayOfMonth = firstDayOfMonth.plusDays(1);
			for(int i = 0; i<measureValue; i++) {
				List<String> tmp = new ArrayList<>();
				for(int j = 0; j < 32; j++) {
					tmp.add(firstDayOfMonth.format(formatter));
					if(secondDayOfMonth.getMonthValue() != firstDayOfMonth.getMonthValue() || firstDayOfMonth.compareTo(localDate) == 0) {
						break;
					}
					secondDayOfMonth = secondDayOfMonth.plusDays(1);
					firstDayOfMonth = firstDayOfMonth.plusDays(1);
				}
				re.add(tmp);
				
				secondDayOfMonth = secondDayOfMonth.plusDays(1);
				firstDayOfMonth = firstDayOfMonth.plusDays(1);
			}
		}
		return re;
	}
	

	public List<DisplayReportMeasure> queryReportMeasure(String token, ReportMeasureBody body) {
		logger.info("query report Measure ,body =" + gson.toJson(body));
		
		Map<Integer,String> userIdNameMap = userService.getAllUser();
		List<DisplayReportMeasure> resultList = new ArrayList<>();
		
		LocalDate localDate = LocalDate.now();
		int measureType = body.getMeasureType();
		int measureValue = body.getMeasureValue();
		List<List<String>> daysList = getDaysList(localDate, measureType, measureValue);
		List<String> indexList = getIndexList(daysList,measureType);
		int days = getDays(daysList);
		
		Map<Integer,List<MeasureData>> resultMap = new HashMap<>();
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("day", days);
		if(body.getUser_id() != null && body.getUser_id().size() > 0) {
			paramMap.put("user_id_list", body.getUser_id());
		}
		List<ReportLabelTask> reportTaskList = reportLabelTaskDao.queryAllReportLabelTaskForMeasure(paramMap);
		logger.info("report List size, reportTaskList=" + reportTaskList.size());
		for(ReportLabelTask reportTask : reportTaskList) {
			List<MeasureData> dataList = resultMap.get(reportTask.getUser_id());
			if(dataList == null) {
				dataList = new ArrayList<>();
				resultMap.put(reportTask.getUser_id(), dataList);
			}
			
			MeasureData data = new MeasureData();
			data.setPictureNum(reportTask.getPictureUpdate());
			data.setPropertiesNum(reportTask.getProperties());
			data.setRectNum(reportTask.getRectAdd() + reportTask.getRectUpdate());
			data.setNotValideNum(reportTask.getNotValide());
			data.setIndex(reportTask.getOper_time().substring(0,10));
			dataList.add(data);
		}
		
		for(Entry<Integer,List<MeasureData>> entry : resultMap.entrySet()) {
			DisplayReportMeasure measure = new DisplayReportMeasure();
			measure.setUser_id(entry.getKey());
			measure.setUser_name(userIdNameMap.get(measure.getUser_id()));
			
			
			Map<String,MeasureData> dayKeyMap = new HashMap<>();
			for(MeasureData data : entry.getValue()) {
				dayKeyMap.put(data.getIndex(), data);
			}
			
			List<MeasureData> dataList = new ArrayList<>();
			
			for(int i = 0; i < daysList.size(); i++) {
				MeasureData data = new MeasureData();
				data.setIndex(indexList.get(i));
				List<String> dayGroupList = daysList.get(i);
				for(String day : dayGroupList) {
					if(dayKeyMap.containsKey(day)) {
						MeasureData srcRep = dayKeyMap.get(day);
						data.setRectNum(srcRep.getRectNum() + data.getRectNum());
						data.setPictureNum(srcRep.getPictureNum() + data.getPictureNum());
						data.setPropertiesNum(srcRep.getPropertiesNum() +  data.getPropertiesNum());
						data.setNotValideNum(srcRep.getNotValideNum() +  data.getNotValideNum());
					}
				}
				dataList.add(data);
			}
			
			measure.setDataList(dataList);
			resultList.add(measure);
		}
		
		return resultList;
	}
	
	private List<String> getIndexList(List<List<String>> daysList, int measureType) {
		List<String> resultList = new ArrayList<>();
		if(measureType == Constants.REPORT_MEASURE_TYPE_DAY) {
			for(List<String> tmp : daysList) {
				resultList.add(tmp.get(0));
			}
		}else if(measureType == Constants.REPORT_MEASURE_TYPE_WEEK) {
			int size = daysList.size();
			for(int i = 0; i< size; i++) {
				resultList.add("Last " + (size - i) + " week");
			}
		}else if(measureType == Constants.REPORT_MEASURE_TYPE_MONTH) {
			for(List<String> tmp : daysList) {
				resultList.add(tmp.get(0).substring(0,7));
			}
		}
		return resultList;
	}

	private int getDays(List<List<String>> daysList) {
		int count = 0;
		for(List<String> tmpList : daysList) {
			count = count + tmpList.size();
		}
		return count;
	}
	
	
	
	public static void main(String[] args) {
		
		LocalDate localDate = LocalDate.now();
		
		System.out.println(localDate.minusDays(2));
		
		System.out.println(localDate.with(TemporalAdjusters.firstDayOfMonth()));
		
		System.out.println(localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
		
		LocalDate newDate = localDate.minusWeeks(4);
		
		//天数相差
		System.out.println(localDate.until(newDate,ChronoUnit.DAYS));
		
		LocalDate newDate2 = localDate.minusDays(2);
		System.out.println(newDate2);
		
		System.out.println(newDate2.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
		
		Gson gson = new Gson();
		
		System.out.println(gson.toJson(getDaysList(LocalDate.now(),0, 10)));
		System.out.println(gson.toJson(getDaysList(LocalDate.now(),1, 4)));
		System.out.println(gson.toJson(getDaysList(LocalDate.now(),2, 1)));
		System.out.println(gson.toJson(getDaysList(LocalDate.now(),2, 2)));
		System.out.println(gson.toJson(getDaysList(LocalDate.now(),2, 3)));
		
	}
	
}
