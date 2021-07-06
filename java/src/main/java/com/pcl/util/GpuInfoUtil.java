package com.pcl.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pcl.exception.LabelSystemException;


public class GpuInfoUtil {

	private static final String GPUUTIL = "| GPU-Util";
	
	private static Logger logger = LoggerFactory.getLogger(GpuInfoUtil.class);

	private static List<String> getCommandOutputInfo() throws LabelSystemException {
		String command = "nvidia-smi";
		List<String> reList = new ArrayList<>();
		String os = System.getProperty("os.name"); 
		if(!os.toLowerCase().startsWith("win")){
			logger.info("start runtime exe script." + command);
			try {
				Process p = Runtime.getRuntime().exec(command, null);
				p.waitFor(1, TimeUnit.SECONDS);
				try(BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "utf-8"))){
					String line = "";
					while ((line = reader.readLine()) != null) {
						logger.info(line + "\n");
						reList.add(line);
					}
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				//throw new LabelSystemException("Can not execute command." + e.getMessage());
			} 
		}
		return reList;
	}

	/**
	 * 获取可用的GPU
	 * @param useRation  gpu使用门限，如果GPU使用使用门限达到超过此值，则此GPU不可用。
	 * @return 可用的GPU集合
	 * @throws LabelSystemException
	 */
	public static List<Integer> getAvalibleGPUInfo(int useRation) throws LabelSystemException{


		List<int[]> gpuInfoList = GpuInfoUtil.getGPUInfo();

		List<Integer> re = new ArrayList<>();

		for(int [] gpuInfo : gpuInfoList) {
			if(gpuInfo[1] >= useRation) {
				continue;
			}
			re.add(gpuInfo[0]);
		}
		logger.info("availed gpu id: " + re.toString());
		return re;
	}
	
	/**
	 * 
	 * @param commandOutput
	 * @return  返回GPU个数列表，每个元素的第一个代表GPU ID，第二个代表当前GPU使用率，如80，则代表当前使用率为80%
	 * @throws LabelSystemException 
	 */
	public static List<int[]> getGPUInfo() throws LabelSystemException{
		
		List<String> commandOutputList = getCommandOutputInfo();
		
		return getGpuInfoFromStr(commandOutputList);
	}

	public static List<int[]> getGpuInfoFromStr(List<String> commandOutputList) {
		ArrayList<int[]> re = new ArrayList<>();
		boolean isStart = false;;
		int gpuUtilStartLoc = -1;
		for(int i = 0; i < commandOutputList.size(); i++) {
			String line = commandOutputList.get(i);

			if(line == null || line.trim().isEmpty()) {
				break;
			}
			if(line.indexOf(GPUUTIL) != -1) {
				gpuUtilStartLoc = line.indexOf(GPUUTIL);
			}
			if(line.startsWith("|=")) {
				//表格开始
				isStart = true;
			}
			if(isStart) {
				if(isStart || line.startsWith("+-")) {
					if(i + 2 < commandOutputList.size()) {
						String gpuLine = commandOutputList.get(i + 1);
						int gpuId = getGpuId(gpuLine);
						if(gpuId != -1) {
							String gpuUsageLine =  commandOutputList.get(i + 2);
							int gpuUsage = getGpuUsage(gpuUsageLine,gpuUtilStartLoc);
							if(gpuUsage != -1) {
								int gpuInfo[] = new int[] {gpuId,gpuUsage};
								re.add(gpuInfo);
								i+=2;
							}
						}
					}
				}
			}
		}
		return re;
	}

	private static int getGpuUsage(String gpuUsageLine, int gpuUtilStartLoc) {
		if(gpuUtilStartLoc != -1 && gpuUsageLine.startsWith("|") && gpuUsageLine.length() > (gpuUtilStartLoc + GPUUTIL.length()) ) {
			String gpuUtilStr = gpuUsageLine.substring(gpuUtilStartLoc + 1, gpuUtilStartLoc + GPUUTIL.length());
			gpuUtilStr = gpuUtilStr.trim();
			if(gpuUtilStr.endsWith("%")) {
				gpuUtilStr = gpuUtilStr.substring(0,gpuUtilStr.length() - 1);
			}
			return Integer.parseInt(gpuUtilStr);
		}

		return -1;
	}

	private static int getGpuId(String gpuLine) {
		if(gpuLine.startsWith("|")) {

			String gpuIdStr = gpuLine.substring(1,"| GPU".length());
			gpuIdStr = gpuIdStr.trim();

			return Integer.parseInt(gpuIdStr);
		}
		return -1;
	}


	public static void main(String[] args) throws LabelSystemException {
		String filePath1 = "D:\\javaapp\\workspace\\labelSystem\\src\\main\\resources\\static\\gpu3.txt";
		
		List<String> list = FileUtil.getAllLineList(filePath1, "utf-8");
		
		List<int[]> result = GpuInfoUtil.getGpuInfoFromStr(list);
		
		for(int[] gpuInfo : result) {
			System.out.println("gpuid:" + gpuInfo[0] + "   usage:" + gpuInfo[1]);
		}
	}

}


