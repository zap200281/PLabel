package com.pcl.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamHandler extends Thread {
	private static Logger logger = LoggerFactory.getLogger(StreamHandler.class);
	private InputStream in;
	private StringBuilder strBuild;

	public StreamHandler(InputStream in,StringBuilder strBuild) {
		this.in = in;
		this.strBuild = strBuild;
	}

	public void run() {
		try(BufferedReader reader = new BufferedReader(
				new InputStreamReader(in))) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				logger.info(line + "\n");
				if(strBuild != null) {
					strBuild.append(line);
				}
			}
		} catch (Exception e) {
			logger.info(e.getMessage());
		}
		logger.info("finished read inputstream.");
	}
}
