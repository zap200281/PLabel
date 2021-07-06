package com.pcl.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeUtil {

	public static String getCurrentTimeStr() {
		Date date = new Date();
		SimpleDateFormat df3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return df3.format(date);
	}

	public static String getBeforeDayTimeStr(int beforeDay) {

		Calendar calendar = Calendar.getInstance();  
		calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) - beforeDay);  
		Date today = calendar.getTime();  

		SimpleDateFormat df3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return df3.format(today);
	}

	public static String getCurrentDayStart() {
		Date date = new Date();
		SimpleDateFormat df3 = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
		return df3.format(date);
	}
	
	public static String getCurrentDayEnd() {
		Date date = new Date();
		SimpleDateFormat df3 = new SimpleDateFormat("yyyy-MM-dd 23:59:59");
		return df3.format(date);
	}

	public static String getBeforeDayStr(int beforeDay) {

		Calendar calendar = Calendar.getInstance();  
		calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) - beforeDay);  
		Date today = calendar.getTime();  

		SimpleDateFormat df3 = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
		return df3.format(today);
	}

	public static String getCurrentTimeStrByyyyyMMddHHmmss() {
		Date date = new Date();
		SimpleDateFormat df3 = new SimpleDateFormat("yyyyMMddHHmmss");
		return df3.format(date);
	}

	public static Date getDateTimebyStr(String timeStr) {

		SimpleDateFormat df3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			return df3.parse(timeStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}



	public static String getCurrentDayStr() {
		Date date = new Date();
		SimpleDateFormat df3 = new SimpleDateFormat("yyyy-MM-dd");
		return df3.format(date);
	}

	public static String getCurrentDayStryyyyMMdd() {
		Date date = new Date();
		SimpleDateFormat df3 = new SimpleDateFormat("yyyyMMdd");
		return df3.format(date);
	}

	
}
