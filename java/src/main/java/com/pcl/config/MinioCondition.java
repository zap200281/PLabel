package com.pcl.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class MinioCondition implements Condition {

	private static Logger logger = LoggerFactory.getLogger(MinioCondition.class);
	
	@Override
	public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata metadata) {
		String type = conditionContext.getEnvironment().getProperty("objectsave.type");
		logger.info("object save type11=" + type);
		if(type == null) {
			return true;
		}
        return "minio".equalsIgnoreCase(type);
	}

}