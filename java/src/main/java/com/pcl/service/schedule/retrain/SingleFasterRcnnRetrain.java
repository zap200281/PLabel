package com.pcl.service.schedule.retrain;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pcl.constant.Constants;
import com.pcl.dao.AlgInstanceDao;
import com.pcl.dao.AlgModelDao;
import com.pcl.dao.PrePredictTaskDao;
import com.pcl.dao.RetrainTaskDao;
import com.pcl.dao.RetrainTaskMsgResultDao;
import com.pcl.dao.UserDao;
import com.pcl.pojo.mybatis.AlgInstance;
import com.pcl.pojo.mybatis.AlgModel;
import com.pcl.pojo.mybatis.RetrainTask;
import com.pcl.pojo.mybatis.RetrainTaskMsgResult;
import com.pcl.pojo.mybatis.User;
import com.pcl.service.LabelDataSetMerge;
import com.pcl.service.schedule.LabelForPictureSchedule;
import com.pcl.util.ProcessExeUtil;

@Service
public class SingleFasterRcnnRetrain extends ATask{

	@Autowired
	private AlgModelDao algModelDao;

	@Autowired
	private RetrainTaskDao retrainTaskDao;
	
	@Autowired
	private RetrainTaskMsgResultDao retrainTaskMsgResultDao;

	@Autowired
	private UserDao userDao;

	@Autowired
	private AlgInstanceDao algInstanceDao;
	
	@Autowired
	private LabelForPictureSchedule labelSchedule;
	
	@Autowired
	private PrePredictTaskDao prePredictTaskDao;

	LabelDataSetMerge dataSetMerge = new LabelDataSetMerge();
	
	private static Logger logger = LoggerFactory.getLogger(SingleFasterRcnnRetrain.class);
	
	@Override
	public void doExecute(RetrainTask retrainTask, List<Integer> availableGpuIdList) {

		logger.info("start to exe retrain task.  use gpu:" + availableGpuIdList.toString());

		try {
			//更新数据库状态为  进行中
			updateTaskProgressing(retrainTask.getId(),retrainTaskDao);

			//住重训结果表中插入一条记录，用于动态更新
			addRetrainTaskMsgResult(retrainTask.getId(),retrainTask.getAlg_model_id());
			
			AlgModel algModel = algModelDao.queryAlgModelById(retrainTask.getAlg_model_id());
			
			String initScript = getTrainScript(retrainTask.getAlg_model_id(),algModel);

			User user = userDao.queryUserById(retrainTask.getUser_id());
			String userName = user.getUsername();

			String  cuda_usages_script = "export CUDA_VISIBLE_DEVICES=" + getGpuIdCommantStr(availableGpuIdList);
			String  messagesAuth = " --username " + userName + " --password pcl123456" + " --taskID " + retrainTask.getId();
			String  messageUrl = " --request_url " + "http://" + getIpv4ForLocalhost() + ":8080";
			String	 script =  cuda_usages_script + "&&" + initScript + messageUrl + messagesAuth;

			AlgInstance algInstance = algInstanceDao.queryAlgInstanceById(retrainTask.getAlg_model_id());
			if(algInstance == null) {
				return;
			}
			String algRootPath = LabelDataSetMerge.getAlgRootPath(algInstance.getAlg_root_dir());

			ProcessExeUtil.execScript(script, algRootPath, 3600 * 24 * 2);

			//正常结束，更新状态
			updateTaskFinish(retrainTask.getId(),Constants.RETRAINTASK_STATUS_FINISHED,"",retrainTaskDao);
			
			logger.info("Succeed to update retrain task status to finished.");
			
			//需要将关联的自动标注任务再次执行一下。
			labelSchedule.addTask(prePredictTaskDao.queryPrePredictTaskById(retrainTask.getPre_predict_task_id()));
			logger.info("re build predict task, id=" + retrainTask.getAlg_model_id());

		} catch (Exception e) {
			e.printStackTrace();
			updateTaskFinish(retrainTask.getId(),Constants.RETRAINTASK_STATUS_EXCEPTION,e.getMessage(),retrainTaskDao);
		}
	}

	

	private void addRetrainTaskMsgResult(String id,int alg_model_id) {
		
		RetrainTaskMsgResult msgResult = new RetrainTaskMsgResult();
		msgResult.setId(id);
		msgResult.setAlg_model_id(alg_model_id);
		msgResult.setStep_total("23");
		msgResult.setEpoch_total("20");
		retrainTaskMsgResultDao.addRetrainTaskMsgResult(msgResult);
	
	}

	private String getIpv4ForLocalhost() {
		InetAddress ip4;
		try {
			ip4 = Inet4Address.getLocalHost();
			return ip4.getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return "127.0.0.1";
	}
	
	





	
}
