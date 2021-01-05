package com.pcl.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pcl.pojo.mybatis.UserExtend;

public class TokenManager {

	private static Logger logger = LoggerFactory.getLogger(TokenManager.class);
	private static ConcurrentHashMap<String,Integer> tokenForUserId = new ConcurrentHashMap<>();
	
	private static ConcurrentHashMap<Integer,String> userIdForToken = new ConcurrentHashMap<>();
	
	private static ConcurrentHashMap<Integer,UserExtend> userIdForExtend = new ConcurrentHashMap<>();
	
	
	public static void addToken(String token, int userId) {
		tokenForUserId.put(token, userId);
		userIdForToken.put(userId, token);
	}
	
	public static void addUserExtend(int userId,UserExtend userExtend) {
		userIdForExtend.put(userId, userExtend);
	}
	
	public static String getUserTablePos(int userId,int func) {
		UserExtend extend = userIdForExtend.get(userId);
		if(extend == null) {
			return "";
		}else {
			return "_" + userId;
		}
	}
	
	public static UserExtend getUserExtend(int userId) {
		return userIdForExtend.get(userId);
	}
	
	
	public static String getTokenByUserId(int userId) {

		if(userIdForToken.containsKey(userId)) {
			return userIdForToken.get(userId);
		}
		
		return null;
	}
	
	public static void removeElapseToken(Map<String,Integer> dbTokenMap) {
		List<String> removeTokenList = new ArrayList<>();
		for(Entry<String,Integer> entry : tokenForUserId.entrySet()) {
			if(dbTokenMap.containsKey(entry.getKey())){
				if(entry.getValue().intValue() == dbTokenMap.get(entry.getKey()).intValue()) {
					continue;
				}
			}
			removeTokenList.add(entry.getKey());
		}
		for(String token : removeTokenList) {
			
			removeToken(token);
		}	
	}
	
	
	public static int getUserIdByToken(String token) {
		
		if(tokenForUserId.containsKey(token)) {
			return tokenForUserId.get(token);
		}
		
		
		return -1;
	}
	


	public static void removeToken(String token) {
		if(tokenForUserId.containsKey(token)) {
			int userId = tokenForUserId.remove(token);
			userIdForToken.remove(userId);
			logger.info("delete token :" +token + " user_id=" + userId);
		};
	}
	

	public static String getServerToken(String webToken) {
		return webToken.substring(4);//remove JWT 
	}
	
}
