package com.pcl.service.schedule;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadSchedule {

	private  static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(8,16,100,TimeUnit.MINUTES,new ArrayBlockingQueue<Runnable>(2000));

	private  static ThreadPoolExecutor exportThreadPool = new ThreadPoolExecutor(6,16,100,TimeUnit.MINUTES,new ArrayBlockingQueue<Runnable>(2000));

	private  static ThreadPoolExecutor logThreadPool = new ThreadPoolExecutor(4,4,100,TimeUnit.MINUTES,new ArrayBlockingQueue<Runnable>(2000));
	
	private  static ThreadPoolExecutor svsThreadPool = new ThreadPoolExecutor(1,1,100,TimeUnit.MINUTES,new ArrayBlockingQueue<Runnable>(2000));
	
	public static void execThread(Runnable runnable) {
		threadPool.execute(runnable);
	}
	
	public static void execExportThread(Runnable runnable) {
		exportThreadPool.execute(runnable);
	}
	
	public static void execSvsThreadPool(Runnable runnable) {
		svsThreadPool.execute(runnable);
	}
	
	public static void execLogThread(Runnable runnable) {
		logThreadPool.execute(runnable);
	}
	
	
}
