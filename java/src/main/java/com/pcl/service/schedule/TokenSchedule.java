package com.pcl.service.schedule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.pcl.dao.AuthTokenDao;
import com.pcl.dao.UserExtendDao;
import com.pcl.pojo.mybatis.AuthToken;
import com.pcl.pojo.mybatis.UserExtend;
import com.pcl.service.TokenManager;
import com.pcl.util.JsonUtil;

   //1.主要用于标记配置类，兼备Component的效果。
@Configuration
@EnableScheduling   // 2.开启定时任务
public class TokenSchedule {

	private static Logger logger = LoggerFactory.getLogger(TokenSchedule.class);

	@Autowired
	private AuthTokenDao authTokenDao;
	
	@Autowired
	private UserExtendDao userExtendDao;
	
	@Value("${elapseTime:604800000}")
	private long elapseTime;//毫秒
	
	@PostConstruct
	private void loadTokenToCache() {

		List<AuthToken> authTokenList = authTokenDao.queryAuthToken(null);
		Map<String,Integer> tmp = new HashMap<>();
		if(authTokenList != null) {
			for(AuthToken authToken : authTokenList) {
				if(System.currentTimeMillis() - authToken.getLoginTime() > elapseTime) {
					//7天过期了，需要删除。
					logger.info("delete token:" +authToken.getToken() + " as elapse time 7 * 24 hours.");
					authTokenDao.delete(authToken.getToken());
					continue;
				}
				//logger.info("compare mem user_id=" + TokenManager.getUserIdByToken(authToken.getToken()) + " db user_id=" + authToken.getUserId());
				tmp.put(authToken.getToken(),  authToken.getUserId());
				if(TokenManager.getUserIdByToken(authToken.getToken()) == authToken.getUserId()) {
					continue;
				}
				
				logger.info("add token:" +authToken.getToken() + " user_id=" + authToken.getUserId());
				TokenManager.addToken(authToken.getToken(), authToken.getUserId());
				UserExtend extend = userExtendDao.queryUserExtend(authToken.getUserId());
				if(extend != null) {
					logger.info("start add user extend to cache. extend=" + JsonUtil.toJson(extend));
					TokenManager.addUserExtend(authToken.getUserId(), extend);
				}
			}
			TokenManager.removeElapseToken(tmp);
		}
	}

	@Scheduled(cron = "0 0/1 * * * ?")//每1分钟执行一次
	private void loadTokenRecode() {
		loadTokenToCache();
	}
	
	public static void main(String[] args) {
		System.out.println(7 * 24 * 3600 * 1000l);
		
	}

}
