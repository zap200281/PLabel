package com.pcl.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pcl.exception.LabelSystemException;

public class ProcessExeUtil {

	private static Logger logger = LoggerFactory.getLogger(ProcessExeUtil.class);

	
	public static void execScriptReturnOutput(String script, String algRootPath, int timeSeconds,StringBuilder re) throws LabelSystemException {

		String os = System.getProperty("os.name"); 
		if(!os.toLowerCase().startsWith("win")){
			logger.info("start runtime exe script." + script);
			long start = System.currentTimeMillis();
			CountDownLatch countDown = new CountDownLatch(1);
			Map<String,Object> error = new HashMap<>();

			new Thread(new Runnable() {
				@Override
				public void run() {

					try {
						ProcessBuilder pb = new ProcessBuilder("/bin/bash","-c",script);
						pb.directory(new File(algRootPath));
						Process p = pb.start();
						logger.info("wait to " + timeSeconds + " seconds.");
						error.put("process", p);

						StreamHandler handler1 = new StreamHandler(p.getErrorStream(),re);
						handler1.start();
						
						StreamHandler handler2 = new StreamHandler(p.getInputStream(),re);
						handler2.start();
						

						p.waitFor(timeSeconds, TimeUnit.SECONDS);
						p.destroyForcibly();
						logger.info("process finished.");
						countDown.countDown();
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
						error.put("error", e.getMessage());
					} 

				}
			}).start();

			try {
				countDown.await(timeSeconds, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}finally {
				logger.info("exec cmd continue. cost=" + (System.currentTimeMillis() - start)/1000 + "s");
				Object p = error.get("process");
				if(p != null) {
					logger.info("destroy process forcibly.");
					((Process)p).destroyForcibly();
				}
				if(error.get("error") != null) {
					throw new LabelSystemException(error.get("error").toString());
				}
			}
		}
	}



	public static void execScriptReturnOutputNotAsyn(String script, String algRootPath, int time,StringBuilder re) throws LabelSystemException {

		String os = System.getProperty("os.name"); 
		if(!os.toLowerCase().startsWith("win")){
			logger.info("start runtime exe script." + script);
			long start = System.currentTimeMillis();
			CountDownLatch countDown = new CountDownLatch(1);
			Map<String,Object> error = new HashMap<>();

			new Thread(new Runnable() {
				@Override
				public void run() {

					try {
						ProcessBuilder pb = new ProcessBuilder("/bin/bash","-c",script);
						pb.directory(new File(algRootPath));
						Process p = pb.start();
						logger.info("wait to " + time + " seconds.");
						error.put("process", p);

						try {
							BufferedReader reader = new BufferedReader(
									new InputStreamReader(new SequenceInputStream(p.getInputStream(), p.getErrorStream())));
							String line = null;
							while ((line = reader.readLine()) != null) {
								logger.info(line + "\n");
								if(re != null) {
									re.append(line);
								}
							}
						} catch (Exception e) {
							logger.info(e.getMessage());
						}
						
						p.waitFor(time, TimeUnit.SECONDS);
						p.destroyForcibly();
						logger.info("process finished.");
						countDown.countDown();
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
						error.put("error", e.getMessage());
					} 

				}
			}).start();

			try {
				countDown.await(time, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}finally {
				logger.info("exec cmd continue. cost=" + (System.currentTimeMillis() - start)/1000 + "s");
				Object p = error.get("process");
				if(p != null) {
					logger.info("destroy process forcibly.");
					((Process)p).destroyForcibly();
				}
				if(error.get("error") != null) {
					throw new LabelSystemException(error.get("error").toString());
				}
			}
		}
	}


	public static void execScript(String script, String algRootPath, int timeSeconds) throws LabelSystemException {
		execScriptReturnOutput(script, algRootPath, timeSeconds, null);
	}

}
