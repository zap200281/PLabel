package com.pcl.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.pcl.constant.Constants;
import com.pcl.constant.UserConstants;
import com.pcl.dao.LabelTaskItemDao;
import com.pcl.dao.ProgressDao;
import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.Progress;
import com.pcl.pojo.body.AutoLabelBody;
import com.pcl.pojo.mybatis.LabelTaskItem;
import com.pcl.service.schedule.AutoLabelVideoSchedule;
import com.pcl.service.schedule.ThreadSchedule;
import com.pcl.service.schedule.TrackingSchedule;
import com.pcl.util.FileUtil;
import com.pcl.util.JsonUtil;
import com.pcl.util.TimeUtil;


@Service
public class AutoLabelService {

	private final static int WISE_MEDICAL_MODEL_ID = 10;

	private final static int FACE_MODEL_ID = 11;

	//private final static int TRACKING_MODEL_ID = 12;
	
	//private final static int TRACKING_MODEL_ID_NEW = 16;
	
	private final static int[] TRACKING_MODEL_ID = new int[] {12,16,20,21};

	private static Logger logger = LoggerFactory.getLogger(AutoLabelService.class);

	@Autowired
	private LabelTaskItemDao labelTaskItemDao;

	@Autowired
	private ObjectFileService fileService;

	@Autowired
	private AutoLabelVideoSchedule autoLabel;

	@Autowired
	private TrackingSchedule trackingSchedule;
	
	@Autowired
	private ProgressDao progressDao;

	//private Map<String,Progress> progressMap = new ConcurrentHashMap<>();
	
	public Progress queryLabelTask(String taskId) {
		Progress progress = progressDao.queryProgressById(taskId);
		if(progress != null) {
			long escTime = (System.currentTimeMillis()/1000) - progress.getStartTime();
			logger.info("escTime=" + escTime);
			double proD = escTime * 1.0/progress.getExceedTime();
			if(proD >= 1) {
				proD = 1;
			}
			progress.setProgress((int)(proD * 100));
			return progress;
		}
		return null;
	}
	
	public void removeProgress(String taskId) {
		progressDao.deleteProgress(taskId);
	}
	
	public void removeProgressByInfo(String taskId) {
		Progress tmp = progressDao.queryProgressById(taskId);
		if(tmp != null) {
			logger.info("pro tmp=" + JsonUtil.toJson(tmp));
			if(tmp.getInfo() != null && tmp.getInfo().equals("3")) {
				//在检测目标完成之后，还需要进行车辆类型及颜色分类检测，因此不能删除进度。
				Map<String,Object> paramMap = new HashMap<>();
				paramMap.put("id", tmp.getId());
				paramMap.put("status", 1);
				paramMap.put("progress", 50);
				progressDao.updateProgress(paramMap);
				return;
			}
			progressDao.deleteProgress(taskId);
		}
		
	}
	
	private void putProgress(Progress pro) {

		Progress tmp = progressDao.queryProgressById(pro.getId());
		if(tmp != null) {
			progressDao.deleteProgress(pro.getId());
		}
		
		progressDao.addProgress(pro);
	}

	public void autoLabelTask(String token, AutoLabelBody body) throws LabelSystemException {
		
		int userId = TokenManager.getUserIdByToken(TokenManager.getServerToken(token));
		body.setUserId(userId);
		
		if(body.getTask_type() !=  null && body.getTask_type().equals(String.valueOf(Constants.DATASET_TYPE_PICTURE))) {
			
			LabelTaskItem item = labelTaskItemDao.queryLabelTaskItemById(TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE),body.getLabel_task_id());
			String picturePath = LabelDataSetMerge.getUserDataSetPath() + File.separator +  System.nanoTime();
			if(item != null) {
				Progress progress = new Progress();
				progress.setId(body.getTaskId());
				progress.setStartTime(System.currentTimeMillis()/1000);
				progress.setExceedTime(15);
				
				if(body.getLabel_option() == 3) {//如果还需要进行车辆及颜色识别，则超时时间增加15秒，并设置标志位，防止在检测完成后删除进度。
					progress.setInfo("3");
					progress.setExceedTime(15 + 15);
				}
				
				putProgress(progress);
				
				new File(picturePath).mkdirs();
				String relate_url = item.getPic_image_field();
				String tmp[] = relate_url.split("/");
				int length = tmp.length;
				fileService.downLoadFileFromMinio(tmp[length-2], tmp[length-1], picturePath);
				
				String pictureAbsolutePath = picturePath + File.separator + tmp[length-1];
				
				if(body.getModel() == WISE_MEDICAL_MODEL_ID) {
					ThreadSchedule.execThread(()->{
						try {
							autoLabelCell(body,picturePath,tmp[length-1],userId);
						}catch (Exception e) {
							logger.info(e.getMessage(),e);
						}finally {
							removeProgress(body.getTaskId());
							FileUtil.delDir(picturePath);
						}
					});
				}else {
					ThreadSchedule.execThread(()->{//此处有bug，可能会占满线程池
						try {
							item.setDisplay_order2(body.getModel());
							autoLabel.labelPicture(pictureAbsolutePath,Constants.AUTO_LABLE_PICTURE_TASK,item,userId);
						}catch (Exception e) {
							logger.info(e.getMessage(),e);
						}finally {
							//removeProgress(body.getTaskId());
							//FileUtil.delDir(picturePath);
						}
					});
					if(body.getLabel_option() == 3) {
						logger.info("enter to distiguish car type and color.");
						//识别车辆颜色
						ThreadSchedule.execThread(()->{
							Progress p = progressDao.queryProgressById(body.getTaskId());
							int count = 0;
							logger.info("p =." + p);
							while(p != null && count < 30) {
								//logger.info("p != null.");
								if(p.getStatus() == 1) {//目标检测已经完成。
									break;
								}
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								count ++;
								p = progressDao.queryProgressById(body.getTaskId());
							}
							
							logger.info("start to distiguish car type and color.");
							LabelTaskItem tmpItem = labelTaskItemDao.queryLabelTaskItemById(TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE),body.getLabel_task_id());
							autoLabel.distinguishPictureCar(pictureAbsolutePath, Constants.AUTO_LABLE_PICTURE_TASK, tmpItem,userId);
							removeProgress(body.getTaskId());
							FileUtil.delDir(picturePath);
						});
						
					}else {
						FileUtil.delDir(picturePath);
					}
					
				}
			}else {
				logger.info("the item is null, body=" + JsonUtil.toJson(body));
			}
			

		}else {
			if(isTrackingModel(body.getModel())){
				logger.info("start to tracking..");
				Progress progress = new Progress();
				progress.setId(body.getTaskId());
				progress.setStartTime(System.currentTimeMillis()/1000);
				int exceedTime =(int)( Math.abs(body.getStartIndex() - body.getEndIndex()) * 0.5) + 60;
				progress.setExceedTime(exceedTime);

				putProgress(progress);

				ThreadSchedule.execThread(()->{
					try {
						trackingSchedule.tracking(body,exceedTime);
					} catch (LabelSystemException e) {
						e.printStackTrace();
					}finally {
						removeProgress(body.getTaskId());
					}
				});
			}
			logger.info("the item is null, body=" + JsonUtil.toJson(body));
		}
	}


	private boolean isTrackingModel(int model) {
		for(int tmp :TRACKING_MODEL_ID) {
			if(tmp == model) {
				return true;
			}
		}
		return false;
	}

	private void autoLabelCell(AutoLabelBody body,String filePath,String fileName,int userId) {

		String url = "http://nat.cloudbastion.cn:62981/blood_cell/detect";
		//使用Restemplate来发送HTTP请求
		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = new HttpHeaders();
		MediaType type = MediaType.parseMediaType("multipart/form-data");
		headers.setContentType(type);

		FileSystemResource fileSystemResource = new FileSystemResource(filePath+"/"+fileName);
		MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
		form.add("image", fileSystemResource);
		form.add("arg_type","binary");
		form.add("file_name",fileName);


		HttpEntity<MultiValueMap<String, Object>> files = new HttpEntity<>(form, headers);

		//{'bbox': [[211, 170], [267, 170], [267, 219], [211, 219]], 'name': 'cell'}
		String jsonResult = restTemplate.postForObject(url, files, String.class);
		logger.info("result=" + jsonResult);
		String newLabelJson = getLabelJsonResult(jsonResult);

		Map<String,Object> paramMap = new HashMap<>();
		paramMap.put("id", body.getLabel_task_id());
		paramMap.put("label_info", newLabelJson);
		paramMap.put("label_status", Constants.LABEL_TASK_STATUS_FINISHED);
		paramMap.put("pic_object_name", body.getPic_object_name());
		paramMap.put("item_add_time", TimeUtil.getCurrentTimeStr());
		paramMap.put("user_id", TokenManager.getUserTablePos(userId, UserConstants.LABEL_TASK_SINGLE_TABLE));
		
		labelTaskItemDao.updateLabelTaskItem(paramMap);

	}


	private String getLabelJsonResult(String jsonResult) {
		if(jsonResult != null) {
			String tmps[] = jsonResult.split("\\}\\{");

			ArrayList<Map<String,Object>> labelInfoList = new ArrayList<>();
			int idCount = 0;
			for(String tmp : tmps) {
				if(!tmp.startsWith("{")) {
					tmp = "{" + tmp;
				}
				if(!tmp.endsWith("}")) {
					tmp = tmp + "}";
				}
				Map<String,Object> tmpLabelMap = JsonUtil.getMap(tmp);

				Map<String,Object> label = new HashMap<>();
				label.put("class_name", tmpLabelMap.get("name"));

				List<Object> boxList = new ArrayList<>();

				List<List<Object>> bboxList = (List<List<Object>>)tmpLabelMap.get("bbox");
				boxList.add(getIntStr(bboxList.get(0).get(0)));
				boxList.add(getIntStr(bboxList.get(0).get(1)));
				boxList.add(getIntStr(bboxList.get(2).get(0)));
				boxList.add(getIntStr(bboxList.get(2).get(1)));

				label.put("box", boxList);
				label.put("id", String.valueOf(++idCount));
				labelInfoList.add(label);
			}
			return JsonUtil.toJson(labelInfoList);
		}
		return "";
	}

	private String getIntStr(Object obj) {
		String doubleStr = obj.toString();
		int index = doubleStr.indexOf(".");
		if(index != -1) {
			return doubleStr.substring(0,index);
		}
		return doubleStr;
	}

	public static void main(String[] args) {
		AutoLabelService s = new AutoLabelService();

		String json = "{'bbox': [[50, 48], [229, 48], [229, 204], [50, 204]], 'name': 'cell'}{'bbox': [[440, 101], [611, 101], [611, 268], [440, 268]], 'name': 'cell'}{'bbox': [[567, 257], [749, 257], [749, 417], [567, 417]], 'name': 'cell'}{'bbox': [[379, 262], [579, 262], [579, 455], [379, 455]], 'name': 'cell'}{'bbox': [[310, 19], [497, 19], [497, 197], [310, 197]], 'name': 'cell'}{'bbox': [[140, 314], [342, 314], [342, 493], [140, 493]], 'name': 'cell'}{'bbox': [[226, 206], [392, 206], [392, 355], [226, 355]], 'name': 'cell'}{'bbox': [[163, 133], [308, 133], [308, 258], [163, 258]], 'name': 'cell'}";

		System.out.println(s.getLabelJsonResult(json));


	}

}
