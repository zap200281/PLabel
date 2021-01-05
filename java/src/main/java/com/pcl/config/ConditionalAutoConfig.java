package com.pcl.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import com.pcl.service.MinioFileService;
import com.pcl.service.ObjectFileService;
import com.pcl.service.obs.OBSFileService;

@Configuration
public class ConditionalAutoConfig {

	private static Logger logger = LoggerFactory.getLogger(ConditionalAutoConfig.class);
	
	@Bean
    @Conditional(MinioCondition.class)
	public ObjectFileService getMinioObjectFileService() {
		logger.info("minio service init.");
		return new MinioFileService();
	}
	
	
	@Bean
    @Conditional(OBSCondition.class)
	public ObjectFileService getOBSObjectFileService() {
		logger.info("ObjectFileService service init.");
		return new OBSFileService();
	}
	
}
