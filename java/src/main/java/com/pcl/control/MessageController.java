package com.pcl.control;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pcl.constant.Constants;
import com.pcl.exception.LabelSystemException;
import com.pcl.service.AutoLabelService;
import com.pcl.service.DataSetService;
import com.pcl.service.LabelTaskService;
import com.pcl.service.PrePredictTaskService;
import com.pcl.service.VideoLabelTaskService;
import com.pcl.service.schedule.LargeImgToDziSchedule;

@RestController
@RequestMapping("/api")
public class MessageController {

	private static Logger logger = LoggerFactory.getLogger(MessageController.class);

	@Autowired
	HttpServletRequest request;

	@Autowired
	private PrePredictTaskService prePredictTaskService;

	@Autowired
	private VideoLabelTaskService videoLabelTaskService;

	@Autowired
	private LabelTaskService labelTaskService;

	@Autowired
	private AutoLabelService autoLabelService;

	@Autowired
	private DataSetService dataSetService;
	
	@Autowired
	private LargeImgToDziSchedule largeImgToDziSchedule;


	private Gson gson = new Gson();

	public static int TYPE_AUTO_LABEL_PICTURE = 1;//自动标注图片的消息
	
	public static int TYPE_AUTO_LABEL_LUNG_PICTURE = 15;//自动标注图片的消息

	public static int TYPE_VIDEO_AUTO_LABEL_PICTURE = 11;//视频自动标注图片的消息

	@RequestMapping(value = "/message",method = RequestMethod.POST,produces ="application/json;charset=utf-8")
	public String receiveMsg(@RequestBody String msg) throws LabelSystemException {

		logger.info("receive msg. msg=" + msg);

		if(msg != null && msg.length() > 2) {
			msg = msg.replace("\\", "");
			msg = msg.substring(1,msg.length() - 1);
			Map<String,Object> msgMap = gson.fromJson(msg, new TypeToken<Map<String,Object>>() {}.getType());

			String typeStr = (String)msgMap.get("type");
			if(Strings.isEmpty(typeStr)) {
				return "failed.";
			}
			int type =  Integer.parseInt(typeStr);
			if(TYPE_AUTO_LABEL_PICTURE == type || TYPE_AUTO_LABEL_LUNG_PICTURE == type) {
				String tmpTaskId = (String)msgMap.get("taskid");
				int index = tmpTaskId.indexOf("##");
				String taskId = tmpTaskId;
				int userId = -1;
				if(index != -1) {
					taskId = tmpTaskId.substring(0,index);
					userId = Integer.parseInt(tmpTaskId.substring(index + 2));
					msgMap.put("userId", userId);
				}
				prePredictTaskService.updateProgressMsg(taskId, msgMap);
			}else if(TYPE_VIDEO_AUTO_LABEL_PICTURE == type) {
				String tmpTaskId = (String)msgMap.get("taskid");
				int index = tmpTaskId.indexOf("##");
				String taskId = tmpTaskId;
				int userId = -1;
				if(index != -1) {
					taskId = tmpTaskId.substring(0,index);
					userId = Integer.parseInt(tmpTaskId.substring(index + 2));
					msgMap.put("userId", userId);
				}
				String progressTaskId = "";
				if(taskId.equals(Constants.AUTO_LABLE_VIDEO_PICTURE_TASK)) {
					progressTaskId = videoLabelTaskService.upateAutoLabelInfo(msgMap);
				}else {
					progressTaskId = labelTaskService.upateAutoLabelInfo(msgMap);
				}
				autoLabelService.removeProgressByInfo(progressTaskId);
			}
		}

		return "success";
	}


	@RequestMapping(value = "/svsmessage", method = RequestMethod.POST,produces ="application/json;charset=utf-8")
	public String receiveSvsMsg(@RequestBody String msg) throws LabelSystemException {
		logger.info("receive msg. msg=" + msg);

//		if(msg != null && msg.length() > 2) {
//			msg = msg.replace("\\", "");
//			msg = msg.substring(1,msg.length() - 1);
//			Map<String,Object> msgMap = gson.fromJson(msg, new TypeToken<Map<String,Object>>() {}.getType());
//			String taskid = String.valueOf(msgMap.get("taskid"));
//			Map<String,Object> resultPath = (Map<String,Object>)msgMap.remove("resultpath");
//			msgMap.put("dzipath", resultPath.get("tilepath"));
//			msgMap.put("dzifilepath", resultPath.get("dzipath"));
//			logger.info("msg2=" + msgMap);
//			largeImgToDziSchedule.finishDealSvsData(taskid, msgMap);
//		}

		return "success";
	}

}
