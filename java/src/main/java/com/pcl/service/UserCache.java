package com.pcl.service;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserCache {

	private static Logger logger = LoggerFactory.getLogger(UserCache.class);
	
	private static ConcurrentHashMap<String, Integer> usrCache = new ConcurrentHashMap<String, Integer>();
	
	private static ConcurrentHashMap<Integer,String> userIdForToken = new ConcurrentHashMap<>();
	
	static void addTokenToCache(String token,Integer user_id) {
		usrCache.put(token, user_id);
		userIdForToken.put(user_id, token);
		logger.info("add token,user_id to cache, token=" + token + " usr_id=" + user_id);
	}
	
	static void removeCache(String token) {
		Integer user_id = usrCache.remove(token);
		logger.info("remove token,user_id from cache, token=" + token + " usr_id=" + user_id);
	}
	
	public static boolean isValideToken(String token) {
		return usrCache.containsKey(token);
	}
	
	public static boolean isContainUserId(Integer user_id) {
		return userIdForToken.containsKey(user_id);
	}
	
	
	
}
