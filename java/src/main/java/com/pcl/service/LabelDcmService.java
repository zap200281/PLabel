package com.pcl.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pcl.dao.LabelDcmTaskItemDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.DcmObj;
import com.pcl.pojo.body.DcmLabelBody;
import com.pcl.pojo.display.Dot;
import com.pcl.pojo.display.DoubleDot;
import com.pcl.pojo.display.DoubleThreeObject;
import com.pcl.pojo.display.ThreeObject;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.service.schedule.ThreadSchedule;
import com.pcl.util.JsonUtil;
import com.pcl.util.TimeUtil;


@Service
public class LabelDcmService {

	private static Logger logger = LoggerFactory.getLogger(LabelDcmService.class);

	//@Autowired
	//private LabelTaskDao labelTaskDao;

	@Autowired
	private LabelDcmTaskItemDao labelDcmTaskItemDao;

	@Autowired
	private ObjectFileService fileService;



	public List<ThreeObject> queryDcmThreeLabelInfo(String token,String labelTaskId) throws LabelSystemException{


		List<LabelTaskItem> list = labelDcmTaskItemDao.queryLabelTaskItemByLabelTaskId(labelTaskId);

		List<ThreeObject> result = new ArrayList<>();

		HashMap<String,ThreeObject> map = new HashMap<>();
		for(LabelTaskItem item : list) {
			List<Map<String,Object>> labelList = JsonUtil.getLabelList(item.getLabel_info());
			if(labelList.isEmpty()) {
				continue;
			}
			for(Map<String,Object> label : labelList) {
				Object maskObj = label.get("mask");
				if(maskObj == null) {
					continue;
				}

				Object objId = label.get("id");
				String id = "default";
				if(objId != null && !objId.toString().isEmpty()) {
					id = objId.toString();
				}
				
				ThreeObject threeObj = map.get(id);
				if(threeObj == null) {
					threeObj = new ThreeObject();
					threeObj.setId(id);
					threeObj.setDotList(new ArrayList<>());
					map.put(id, threeObj);
				}
				
				List<Dot> dotList = getAllDot(maskObj);
				threeObj.getDotList().add(dotList);
				logger.info("dotListsize=" + dotList.size());
			}
		}
		result.addAll(map.values());
		

		
		return result;
	}

	
	public List<DoubleThreeObject> queryDoubleDcmThreeLabelInfo(String token,String labelTaskId) throws LabelSystemException{
		logger.info("Get three double object.");
		List<DoubleThreeObject> result = new ArrayList<>();
		
		DoubleThreeObject threeObj = new DoubleThreeObject();
		threeObj.setId("default");
		threeObj.setDotList(new ArrayList<>());
		
		for(int i =0; i < 50; i++) {
			boolean isEnd = false;
			if( i == 0) {
				isEnd = true;
			}
			List<DoubleDot> re = getCircleDotList(i,isEnd);
			threeObj.getDotList().add(re);
		}
		
		for(int i =50; i < 100; i++) {
			boolean isEnd = false;
			if( i == 99) {
				isEnd = true;
			}
			List<DoubleDot> re = getCircleDotList(100 - i,isEnd);
			threeObj.getDotList().add(re);
		}
		
		result.add(threeObj);
		
		return result;
	}

	
	private List<Dot> getAllDot(Object maskObj){
		@SuppressWarnings("unchecked")
		List<Object> maskList = (List<Object>) maskObj;
		List<Dot> dotList = new ArrayList<>();
		maskList.add(maskList.get(0));//补齐终点到起点
		maskList.add(maskList.get(1));
		for(int i = 0; i < maskList.size() - 2; i+=2) {
			int startX = getInt(maskList.get(i));
			int startY = getInt(maskList.get(i+1));
			int endX = getInt(maskList.get(i + 2));
			int endY = getInt(maskList.get(i + 3));
			dotList.addAll(getDist(startX, startY, endX, endY));
		}
		return dotList;
	}
	
	private int getInt(Object obj) {
		return (int)Double.parseDouble(obj.toString());
	}

	private List<Dot> getDist(int startX,int startY,int endX, int endY) {
		List<Dot> dotList = new ArrayList<>();
		dotList.add(new Dot(startX,startY));

		double k = (endY- startY)*1.0 /(endX - startX);

		if(Math.abs(k) > 1) {
			//logger.info("k > 1 startX=" + startX + " startY=" + startY + " endX=" + endX + " endY=" + endY);
			//Y轴加1递进
			k = (endX - startX) * 1.0 / (endY- startY);
			if(endY >= startY) {
				for(int i = startY + 1; i< endY; i++) {
					int x =(int)( k * (i - startY) + startX);
					dotList.add(new Dot(x,i));
				}
			}else {
				for(int i = startY - 1; i> endY; i--) {
					int x =(int)( k * (i - startY) + startX);
					dotList.add(new Dot(x,i));
				}
			}
		}else {
			//X轴加1递进
			if(endX >= startX) {
				for(int i = startX + 1; i< endX; i++) {
					int y =(int)( k * (i - startX) + startY);
					dotList.add(new Dot(i,y));
				}
			}else {
				for(int i = startX - 1; i> endX; i--) {
					int y =(int)( k * (i - startX) + startY);
					dotList.add(new Dot(i,y));
				}
			}
		}
		return dotList;
	}

	private final static String RLAP = "RLAP";
	private final static String APSI = "APSI";
	private final static String RLSI = "RLSI";

	private ConcurrentHashMap<String, List<BufferedImage>> cache = new ConcurrentHashMap<>();

	/**
	 * direct: RLAP,APSI,RLSI
	 * index: 当direct=RLAP时，index=0; 
	 *                            当direct=APSI时，index的范围为0--原始图像的宽度， 
	 *                            当direct=RLSI时，index的范围为0--原始图像的高度
	 * @throws Exception 
	 */
	public DcmObj getDcmObj(String labelDcmItemTaskId,String path,String direct,int index) throws Exception {
		DcmObj dcmObj = null;
		logger.info("get dcm obj: labelDcmItemTaskId=" + labelDcmItemTaskId +" path=" + path + " direct=" + direct + " index=" + index);
		if(RLAP.equals(direct)) {
			//logger.info("create RLAP image.");
			dcmObj = fileService.getDcmPicture(path);
			ThreadSchedule.execThread(()->{
				loadCacheFromMinio(labelDcmItemTaskId);
			});
		}else if(APSI.equals(direct)) {
			//logger.info("create APSI image.");
			List<BufferedImage> cacheImage = loadCacheFromMinio(labelDcmItemTaskId);
			BufferedImage newImage = createAPSI(cacheImage, index);
			dcmObj = new DcmObj();
			dcmObj.setImage(newImage);
		}else if(RLSI.equals(direct)) {
			//logger.info("create RLSI image.");
			List<BufferedImage> cacheImage = loadCacheFromMinio(labelDcmItemTaskId);
			BufferedImage newImage = createRLSI(cacheImage, index);
			dcmObj = new DcmObj();
			dcmObj.setImage(newImage);
		}
		return dcmObj;
	}

	private synchronized List<BufferedImage> loadCacheFromMinio(String labelDcmItemTaskId) {
		LabelTaskItem item = labelDcmTaskItemDao.queryLabelTaskItemById(labelDcmItemTaskId);
		String name = item.getPic_url();
		String label_task_id = item.getLabel_task_id();
		String key = label_task_id + name;
		List<BufferedImage> cacheBuff = cache.get(key);
		if(cacheBuff == null) {
			cacheBuff = new ArrayList<>();
			loadCache(cacheBuff,label_task_id,name);
			if(cache.size() > 10) {
				logger.info("cache size > 10, so clear it.");
				cache.clear();
			}
			cache.put(key, cacheBuff);
		}
		return cacheBuff;
	}

	private void loadCache(List<BufferedImage> cacheBuff, String label_task_id, String name) {
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("label_task_id", label_task_id);
		paramMap.put("pic_url", name);
		
		List<LabelTaskItem> itemList = labelDcmTaskItemDao.queryLabelTaskItemByLabelTaskIdAndName(paramMap);
		logger.info("load " + name + " to cache,size=" + itemList.size() + ", label_task_id=" + label_task_id);
		for(LabelTaskItem item : itemList) {
			try {
				String relativePath = item.getPic_image_field();
				if(relativePath.startsWith("/dcm/")) {
					relativePath = relativePath.substring(5);
				}
				DcmObj dcmObj = fileService.getDcmPicture(relativePath);
				cacheBuff.add(dcmObj.getImage());
				
				if(item.getPic_object_name() == null) {
					Map<String,Object> tmpParamMap = new HashMap<>();
					tmpParamMap.put("id", item.getId());
					tmpParamMap.put("pic_object_name", dcmObj.getImage().getWidth() + "," + dcmObj.getImage().getHeight());
					tmpParamMap.put("display_order1", itemList.size());
					tmpParamMap.put("item_add_time", TimeUtil.getCurrentTimeStr());
					labelDcmTaskItemDao.updateLabelTaskItem(tmpParamMap);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	private  BufferedImage createRLSI(List<BufferedImage> cacheImage, int row) throws IOException {
		int width = cacheImage.get(0).getWidth();
		int actureHeight = cacheImage.size();
		int height = actureHeight;
//		
//		int add = 0;
//		if(actureHeight < width) {
//			add = (width - actureHeight) / 2;
//		}
		BufferedImage newImage = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
//		for(int i = 0; i < add; i++) {
//			for(int j = 0; j < width; j++) {
//				newImage.setRGB(j, i, 0);
//			}
//		}
		for(int i = 0; i < cacheImage.size(); i++) {
			BufferedImage srcBuff = cacheImage.get(i);
			for(int j = 0; j < width; j++) {
				int rgb = srcBuff.getRGB(j, row);
				newImage.setRGB(j, i, rgb);
			}
		}
//		for(int i = cacheImage.size() + add; i < height; i++) {
//			for(int j = 0; j < width; j++) {
//				newImage.setRGB(j, i, 0);
//			}
//		}
		return newImage;
	}


	private BufferedImage createAPSI(List<BufferedImage> cacheImage, int col) throws IOException {
		int width = cacheImage.get(0).getHeight();
		
		int actureHeight = cacheImage.size();
		
		int height = actureHeight;
//		int add = 0;
//		if(actureHeight < width) {
//			add = (width - actureHeight) / 2;
//		}
		BufferedImage newImage = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
//		for(int i = 0; i < add; i++) {
//			for(int j = 0; j < width; j++) {
//				newImage.setRGB(j, i, 0);
//			}
//		}
		for(int i = 0; i < cacheImage.size(); i++) {
			BufferedImage srcBuff = cacheImage.get(i);
			for(int j = 0; j < width; j++) {
				int rgb = srcBuff.getRGB(col, j);
				newImage.setRGB(j, i, rgb);
			}
		}
//		for(int i = cacheImage.size() + add; i < height; i++) {
//			for(int j = 0; j < width; j++) {
//				newImage.setRGB(j, i, 0);
//			}
//		}
		return newImage;		
	}

	private ConcurrentHashMap<String, List<Map<String,List<Dot>>>> dcmLabelCache = new ConcurrentHashMap<>();
	
	public void saveDcmLabelInfo(DcmLabelBody dcmLabelBody,String token) {
		String labelInfo = dcmLabelBody.getLabelInfo();
		//要考虑用户删除的情况
		List<Map<String,Object>> newLabelList = JsonUtil.getLabelList(labelInfo);
		
		//转换成像素点的标注
		Map<String,List<Dot>> pixLabel = convertToPixLabel(newLabelList);
		
		LabelTaskItem item = labelDcmTaskItemDao.queryLabelTaskItemById(dcmLabelBody.getLabelDcmItemTaskId());
		String name = item.getPic_url();
		String label_task_id = item.getLabel_task_id();
		String key = label_task_id + name;
		
		List<Map<String,List<Dot>>> labelCache = dcmLabelCache.get(key);
		if(labelCache == null) {
			labelCache = loadLabelFromDb(label_task_id,name);
		}
		
		if(RLAP.equals(dcmLabelBody.getDirect())){
			labelCache.set(dcmLabelBody.getIndex(), pixLabel);
		}else if(APSI.equals(dcmLabelBody.getDirect())) {
			int xIndex = dcmLabelBody.getIndex();  //X坐标
			//将所有像素按照X分布到各个图片中
			List<Map<String,List<Dot>>> newTmpLabel = splitPixLabelByXToEveryImage(pixLabel,labelCache.size(),false);
			
			for(int i = 0; i < labelCache.size(); i++) {//代表z轴遍历，即一张横截面图片
				Map<String,List<Dot>> oldImageLabel = labelCache.get(i);
				Map<String,List<Dot>> newImageLabel = newTmpLabel.get(i);
				if(oldImageLabel.isEmpty()) {
					labelCache.set(i, newImageLabel);
				}else {
					//删除所有xIndex的坐标，并合并新的坐标
					for(Entry<String,List<Dot>> entry : oldImageLabel.entrySet()) {
						List<Dot> oldDotList = entry.getValue();
						for(int j = 0; j <oldDotList.size(); j++) {
							if(oldDotList.get(j).getX() == xIndex) {
								oldDotList.remove(j);
								j--;
							}
						}
						List<Dot> newDotList = newImageLabel.remove(entry.getKey());
						mergeDot(oldDotList,newDotList);
					}
					
					oldImageLabel.putAll(newImageLabel);
				}
			}
			
	
		}else if(RLSI.equals(dcmLabelBody.getDirect())) {
			int yIndex = dcmLabelBody.getIndex(); 
			List<Map<String,List<Dot>>> newTmpLabel = splitPixLabelByXToEveryImage(pixLabel,labelCache.size(),true);

			for(int i = 0; i < labelCache.size(); i++) {//代表z轴遍历，即一张横截面图片
				Map<String,List<Dot>> oldImageLabel = labelCache.get(i);
				Map<String,List<Dot>> newImageLabel = newTmpLabel.get(i);
				if(oldImageLabel.isEmpty()) {
					labelCache.set(i, newImageLabel);
				}else {
					//删除所有xIndex的坐标，并合并新的坐标
					for(Entry<String,List<Dot>> entry : oldImageLabel.entrySet()) {
						List<Dot> oldDotList = entry.getValue();
						for(int j = 0; j <oldDotList.size(); j++) {
							if(oldDotList.get(j).getY() == yIndex) {
								oldDotList.remove(j);
								j--;
							}
						}
						List<Dot> newDotList = newImageLabel.remove(entry.getKey());
						mergeDot(oldDotList,newDotList);
					}
					
					oldImageLabel.putAll(newImageLabel);
				}
			}
			
		}
		
		
		
	}

	private void mergeDot(List<Dot> oldDotList, List<Dot> newDotList) {
		for(Dot dot : newDotList) {
			int max = Integer.MAX_VALUE;
			int index = -1;
			for(int i=0; i < oldDotList.size(); i++) {
				int first = i;
				int second = i+1;
				if(second >= oldDotList.size()) {
					second = 0;
				}
				int dist = getDist(dot,oldDotList.get(first)) + getDist(dot,oldDotList.get(second));
				if(dist < max) {
					max = dist;
					index = first;
				}
			}
			logger.info("index=" + index);
			oldDotList.add(index + 1, dot);
		}
	}
	
	private int getDist(Dot dot1,Dot dot2) {
		return (dot1.getX() - dot2.getX()) *  (dot1.getX() - dot2.getX()) + (dot1.getY() - dot2.getY()) *  (dot1.getY() - dot2.getY());
	}
	

	private List<Map<String, List<Dot>>> splitPixLabelByXToEveryImage(Map<String, List<Dot>> pixLabel, int size, boolean isY) {
		List<Map<String,List<Dot>>> re = new ArrayList<>();
		
		
		for(Entry<String,List<Dot>> entry : pixLabel.entrySet()) {
			List<Dot> tmpDotList = entry.getValue();
			
			Map<Integer,List<Dot>> tmpMap = new HashMap<>();
			
			for(Dot dot : tmpDotList) {
				int tmpKey = dot.getX();
				if(isY) {
					tmpKey = dot.getY();
				}
				List<Dot> tmpList = tmpMap.get(tmpKey);
				if(tmpList == null) {
					tmpList = new ArrayList<>();
					tmpMap.put(tmpKey, tmpList);
				}
				tmpList.add(dot);
			}
			
			
			for(int i = 0; i < size; i++) {
				Map<String,List<Dot>> tmp = re.get(i);
				if(tmp == null) {
					tmp = new HashMap<>();
					re.add(tmp);
				}
				List<Dot> tmpList = tmpMap.get(i);
				if(tmpList != null) {
					tmp.put(entry.getKey(),tmpList);
				}
				re.add(tmp);
			}
		}
		return re;
	}
	
	

	private List<Map<String,List<Dot>>> loadLabelFromDb(String label_task_id, String name) {
		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("label_task_id", label_task_id);
		paramMap.put("pic_url", name);
		
		List<LabelTaskItem> itemList = labelDcmTaskItemDao.queryLabelTaskItemByLabelTaskIdAndName(paramMap);
		logger.info("load " + name + " to cache,size=" + itemList.size() + ", label_task_id=" + label_task_id);
		List<Map<String,List<Dot>>> result = new ArrayList<>();
		for(LabelTaskItem item : itemList) {
			result.add(convertToPixLabel( JsonUtil.getLabelList(item.getLabel_info())));
		}
		
		return result;
	}

	private Map<String,List<Dot>> convertToPixLabel(List<Map<String, Object>> newLabelList) {
		Map<String,List<Dot>> re = new HashMap<>();
		
		for(Map<String,Object> labelMap : newLabelList) {
			Object maskObj = labelMap.get("mask");
			if(maskObj == null) {
				continue;
			}
			List<Dot> dotList = getAllDot(maskObj);
			String type = (String)labelMap.get("class_name");
			re.put(type, dotList);
		}
		
		return re;
	}
	
	int dotNum = 60;
	
	
	
	List<DoubleDot> getCircleDotList(int r,boolean isEnd){
		List<DoubleDot> result = new ArrayList<>();
		
		if(isEnd) {
			DoubleDot dot = new DoubleDot();
			dot.setX(r);
			dot.setY(r);
			result.add(dot);
			return result;
		}
		
		for(int i = 0; i< dotNum; i++) {
			double hudu = (2*Math.PI / 360) * (360 / dotNum) * i;
			double x = Math.cos(hudu) * r;
			double y =  Math.sin(hudu) * r;
			DoubleDot dot = new DoubleDot();
			dot.setX(x);
			dot.setY(y);
			result.add(dot);
		}
		return result;
	}
	
	



	private static void outputDot() {
		LabelDcmService ld = new LabelDcmService();
//		
//		List<Dot> tmp = new ArrayList<>();
//		tmp.add(new Dot(1,1));
//		tmp.add(new Dot(2,1));
//		tmp.add(new Dot(3,1));
//		tmp.add(new Dot(3,0));
//		tmp.add(new Dot(2,0));
//		tmp.add(new Dot(1,0));
//		
//		List<Dot> tmp2 = new ArrayList<>();
//		tmp2.add(new Dot(4,1));
//		
//		ld.mergeDot(tmp, tmp2);
//		
//		System.out.println(tmp);
		
//		System.out.println(Math.cos(0));
//		System.out.println(Math.cos(90));
//		System.out.println(Math.cos(180));
//		System.out.println(Math.cos(270));
		

		
		ArrayList<Double> x = new ArrayList<>();
		ArrayList<Double> y = new ArrayList<>();
		ArrayList<Double> z = new ArrayList<>();

		ArrayList<Double> all = new ArrayList<>();
		
		StringBuilder strBuild = new StringBuilder();
		strBuild.append("pts = np.array([");
		for(int i =1; i <3; i++) {
			List<DoubleDot> re = ld.getCircleDotList(i, false);
			System.out.println("re.size=" + re.size());
			for(int j = 0; j <re.size(); j++) {
				x.add( re.get(j).getX());
				y.add( re.get(j).getY());
				z.add(i * 1.0d);
				
				all.add( re.get(j).getX());
				all.add( re.get(j).getY());
				all.add(i * 1.0d);
			
				strBuild.append("[" + String.format("%." + 2 + "f", re.get(j).getX()) + "," + 
				                      String.format("%." + 2 + "f", re.get(j).getY()) + "," + 
                                      String.format("%." + 2 + "f", i * 1.0d)  + 
						
						"],");
			}
		}
		
		strBuild.deleteCharAt(strBuild.length() - 1);
		strBuild.append("])");
		
		System.out.println("u = np.array([" + getStr(x, 2) + "])");
		
		System.out.println("v = np.array([" + getStr(y, 2) + "])");
		
		System.out.println("z = np.array(" + z + ")");
		
		System.out.println("var arr = [" + getStr(all, 2) + "]");
		
		System.out.println(strBuild);
	}
	
	private static void outputDotAndFace() {
		LabelDcmService ld = new LabelDcmService();
		List<DoubleDot> f1 = ld.getCircleDotList(1, false);
		List<DoubleDot> f2 = ld.getCircleDotList(2, false);
		List<Integer> face = getFace(f1, f2);
		
		ArrayList<Double> all = new ArrayList<>();
		for(int j = 0; j <f1.size(); j++) {
			all.add( f1.get(j).getX());
			all.add( f1.get(j).getY());
			all.add( 1.0d);
		}
		
		for(int j = 0; j <f2.size(); j++) {
			all.add( f2.get(j).getX());
			all.add( f2.get(j).getY());
			all.add( 2.0d);
		}
		System.out.println("var arr = [" + getStr(all, 2) + "];");
		
		System.out.println("var faceIndex =" + face + ";");
		
	}
	
	
	private static List<Integer> getFace(List<DoubleDot> f1,List<DoubleDot> f2){
		List<Integer> faceList = new ArrayList<>();
		
		int f1Size = f1.size();

		
		int f2Start = 0;
		
		for(int i = 0; i < f1Size - 1;) {
			if(i < f2Start) {
				int p1 = i;
				int p2 = i+1;
				int p3 = f1Size + f2Start;
				i++;
				faceList.add(p3);
				faceList.add(p2);
				faceList.add(p1);
			}else {
				int p1 = f1Size + f2Start;
				int p2 = f1Size + f2Start + 1;
				int p3 = i;
				f2Start++;
				faceList.add(p1);
				faceList.add(p2);
				faceList.add(p3);
			}
			
		}
		return faceList;
		
	}
	
	
	private static String getStr(ArrayList<Double> x,int num) {
		String re = "";
		for(Double value : x) {
			re +=String.format("%." + num + "f", value) + ",";
		}
		return re.substring(0,re.length()  - 1);
	}
	
	public static void main(String[] args) {
		//outputDot();
		outputDotAndFace();
	}
	
}
