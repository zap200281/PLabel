package com.pcl.constant;

public class Constants {

	public final static String JPEGIMAGES = "JPEGImages";
	
	public final static String ANNOTATIONS = "Annotations";
	
	public final static String REID_KEY = "reId";
	
	public final static String CHOUZHEN_KEY_FRAME = "0";
	
	public final static String CHOUZHEN_PERSECOND_FRAME = "1";
	
	public final static String CHOUZHEN_FILE_NAME_FORMAT_NUMBER = "0";
	
	public final static String CHOUZHEN_FILE_NAME_FORMAT_TIME = "1";
	
	public final static String CHOUZHEN_FILE_NAME_FORMAT_CAMERA_TIME = "2";
	
	
	public final static String REID_EXPORT_TYPE_REID_PICTURE = "1";//抠图导出
	
	public final static String REID_EXPORT_TYPE_REID_PICTURE_RENAME = "3";//抠图导出，使用数据集的名称重新命名ReID及标框
	
	public final static String REID_EXPORT_TYPE_REID_LABEL = "2";//仅仅导出标注信息
	
	public final static String REID_EXPORT_TYPE_REID_ONLY_CUT = "4";//仅仅导出抠图
	
	public final static String REID_EXPORT_TYPE_LABEL = "21";//仅仅导出标注信息
	
	public final static String AUTO_LABLE_PICTURE_TASK = "2";
	
	public final static String AUTO_LABLE_VIDEO_PICTURE_TASK = "1";
	
	public final static int USER_SUPER = 0; //超级用户
	
	public final static int USER_LABEL = 1; //标注人员
	
	public final static int USER_VERIFY = 2; //审核人员
	
	public final static int TASK_STATUS_FINISHED = 0;
	
	public final static int TASK_STATUS_PROGRESSING = 1;
	
	public final static int TASK_STATUS_EXCEPTION = 2;
	
	public final static int PREDICT_TASK_STATUS_FINISHED = 0;
	
	public final static int PREDICT_TASK_STATUS_PROGRESSING = 1;
	
	public final static int PREDICT_TASK_STATUS_WAIT_GPU = 3;//等待GPU中
	
	//public final static int PREDICT_TASK_STATUS_DELETE_SIMILAR = 101;//删除相似性图片
	
	//public final static int PREDICT_TASK_STATUS_DOWNLOAD_PIC = 102;//下载图片中
	
	//public final static int PREDICT_TASK_STATUS_START_TO_LABEL = 103;//下载图片中
	
	public final static int PREDICT_TASK_STATUS_EXCEPTION = 2;
	
	public final static int LABEL_TASK_STATUS_LABEL = 0;//标注中
	
	public final static int LABEL_TASK_STATUS_VERIFY = 1;//审核中
	
	public final static int LABEL_TASK_STATUS_FINISHED = 0;
	
	public final static int LABEL_TASK_STATUS_NOT_FINISHED = 1;
	
	public final static int LABEL_TASK_TYPE_AUTO = 1; 
	
	public final static int LABEL_TASK_TYPE_ORIGIN = 2; //图片
	
	public final static int LABEL_TASK_TYPE_ORIGIN_DCM = 3; //DCM
	
	public final static int LABEL_TASK_TYPE_VIDEO = 4; 
	
	public final static int LABEL_TASK_FLOW_TYPE_WORK = 1; // 标注工作模式
	
	public final static int LABEL_TASK_FLOW_TYPE_VERIFY = 2; //标注审核模式
	
	public final static int LABEL_TASK_FLOW_TYPE_MIAOD = 3; //主动学习标注模式
	
	public final static int RETRAINTASK_STATUS_FINISHED = 0;
	
	public final static int RETRAINTASK_STATUS_NOT_STARTED = 1;
	
	public final static int RETRAINTASK_STATUS_WAITING = 2;
	
	public final static int RETRAINTASK_STATUS_PROGRESSING = 3;
	
	public final static int RETRAINTASK_STATUS_EXCEPTION = 4;
	
	public final static int DATASET_TYPE_PICTURE = 1; //图片
	
	public final static int DATASET_PROCESS_CHOUZHEN = 1000; //视频抽帧
	
	public final static int DATASET_PROCESS_ZOOM_SVS = 3000; //SVS图片分层
	
	public final static int DATASET_PROCESS_CONCAT = 2000; //视频合并
	
	public final static int DATASET_PROCESS_PREPARE = 10; //视频已经准备好了。
	
	public final static int DATASET_PROCESS_NOT_START = 2; //视频已经准备好了。
	
	public final static int DATASET_TYPE_DCM = 2; //DCM,CT影像
	
	public final static int DATASET_TYPE_MEDICAL_CELL = 22; //医疗细胞检测图片
	
	public final static int DATASET_TYPE_SVS = 3;//病理
	
	public final static int DATASET_TYPE_VIDEO = 4;//视频
	
	public final static int DATASET_STATUS_ERROR = -1;//数据集状态错误
	
	public final static int REID_TASK_TYPE_AUTO = 1;//通过自动标注结果创建的行人再识别任务
	
	public final static int REID_TASK_TYPE_MANUAL =2;//通过数据集直接创建的行人再识别任务
	
	public final static int REID_TASK_OBJ_TYPE_PERSON = 0;//标注人
	
	public final static int REID_TASK_OBJ_TYPE_CAR = 1;//标注车
	
	public final static int REID_TASK_STATUS_FINISHED = 0;
	
	public final static int REID_TASK_STATUS_AUTO_PROGRESSING = 1;
	
	public final static int REID_TASK_STATUS_PROGRESSING = 2;
	
	public final static int REID_TASK_STATUS_AUTO_FINISHED = 3;
	
	public final static int REID_TASK_STATUS_EXCEPTION = -1;
	
	public final static int VIDEO_TASK_STATUS_START = 1;
	
	public final static int LARGE_TASK_STATUS_START = 1;
	
	public final static int LARGE_TASK_STATUS_NOT_START = 2;
	
	
	public final static int REPORT_MEASURE_TYPE_DAY = 0;
	
	public final static int REPORT_MEASURE_TYPE_WEEK = 1;
	
	public final static int REPORT_MEASURE_TYPE_MONTH = 2;
	
	//自动标注后，如果该图片没有自动检测到目标对象，则删除掉。
	public final static int AUTO_DELETE_NO_LABEL_PICTURE = 1;
	
	public final static int AUTO_DELETE_NO_LABEL_PICTURE_PRI = 2;//还删除原始数据集中的图片
	
	public final static int NOMAL_LABEL_PICTURE = 0;
	
	public final static int QUERY_ITEM_PAGE_FIND_LAST = 1;
	
	public final static int QUERY_ITEM_PAGE_MIAOD = 3;//主动学习分类
	
//	public final static int NEED_TO_DISTIGUISH_CAR_PROPERTY = 1;
//	
//	public final static int NEED_TO_DISTIGUISH_PERSON_PROPERTY = 2;
}
