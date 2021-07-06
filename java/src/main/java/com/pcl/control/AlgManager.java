package com.pcl.control;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pcl.exception.LabelSystemException;
import com.pcl.pojo.Result;
import com.pcl.pojo.mybatis.AlgInstance;
import com.pcl.pojo.mybatis.AlgModel;
import com.pcl.service.AlgService;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api")
public class AlgManager {

	private static Logger logger = LoggerFactory.getLogger(AlgManager.class);
	
	@Autowired
	private AlgService algService;
	
	@Autowired
	HttpServletRequest request;

	@ApiOperation(value="增加算法模型", notes="增加算法模型")
	@RequestMapping(value = "/addAlgModel",method = RequestMethod.POST)
	public Result addAlgModel(@RequestBody AlgModel algModel) {
		Result re = new Result();
		try {
			algService.addAlgModel(algModel);
			re.setCode(0);
		}catch(Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	@ApiOperation(value="删除算法模型", notes="删除算法模型")
	@RequestMapping(value = "/deleteAlgModel",method = RequestMethod.POST)
	public Result addAlgModel(@RequestParam("id")  int id) {
		Result re = new Result();
		try {
			algService.deleteAlgModel(id);
			re.setCode(0);
		}catch(Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
	@ApiOperation(value="查询所有的算法模型", notes="返回所有的算法模型")
	@RequestMapping(value="/queryAlgModel", method = RequestMethod.GET)
	public List<AlgModel> queryAlgModel() throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryAlgModel  token =" + token);

		return algService.queryAlgModel();
		
	}
	
	@ApiOperation(value="查询所有的算法模型", notes="返回所有的算法模型")
	@RequestMapping(value="/queryAlgModelForRetrain", method = RequestMethod.GET)
	public List<AlgModel> queryAlgModelForRetrain() throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryAlgModel  token =" + token);

		return algService.queryAlgModelForRetrain();
		
	}
	
	@ApiOperation(value="查询所有目标跟踪算法模型", notes="返回所有的所有目标跟踪算法模型")
	@RequestMapping(value="/queryAlgModelForTracking", method = RequestMethod.GET)
	public List<AlgModel> queryAlgModelForTracking() throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryAlgModelForTracking  token =" + token);

		return algService.queryAlgModelForTracking();
		
	}
	
	@ApiOperation(value="查询所有属性识别算法模型", notes="返回所有属性识别算法模型")
	@RequestMapping(value="/queryAlgModelForProperty", method = RequestMethod.GET)
	public List<AlgModel> queryAlgModelForProperty() throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryAlgModelForProperty  token =" + token);

		return algService.queryAlgModelForProperty();
		
	}
	
	@ApiOperation(value="查询所有的算法模型", notes="返回所有的算法模型")
	@RequestMapping(value="/queryAlgModelForAutoLabel", method = RequestMethod.GET)
	public List<AlgModel> queryAlgModelForAutoLabel() throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryAlgModelForAutoLabel  token =" + token);

		return algService.queryAlgModelForAutoLabel();
		
	}
	
	@ApiOperation(value="查询所有的算法模型", notes="返回所有的算法模型")
	@RequestMapping(value="/queryAlgModelForHandLabel", method = RequestMethod.GET)
	public List<AlgModel> queryAlgModelForHandLabel() throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryAlgModelForHandLabel  token =" + token);

		return algService.queryAlgModelForHandLabel();
		
	}
	
	@ApiOperation(value="查询所有的算法模型", notes="返回所有的算法模型")
	@RequestMapping(value="/queryAlgModelContainWiseMedical", method = RequestMethod.GET)
	public List<AlgModel> queryAlgModelContainWiseMedical() throws LabelSystemException{
		
		String token = request.getHeader("authorization");
		if(token == null) {
			throw new LabelSystemException("user not login.");
		}
		logger.info("queryAlgModelContainWiseMedical  token =" + token);

		return algService.queryAlgModelContainWiseMedical();
		
	}
	
	
	@ApiOperation(value="增加算法模型实例", notes="增加算法模型实例")
	@RequestMapping(value = "/addAlgInstance",method = RequestMethod.POST)
	public Result addAlgInstance(@RequestBody AlgInstance algInstance) {
		Result re = new Result();
		try {
			algService.addAlgInstance(algInstance);
			re.setCode(0);
		}catch(Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}
	
	
	@ApiOperation(value="删除算法模型实例", notes="删除算法模型实例")
	@RequestMapping(value = "/deleteAlgInstance",method = RequestMethod.POST)
	public Result deleteAlgInstance(@RequestParam("id")  int id) {
		Result re = new Result();
		try {
			algService.deleteAlgInstance(id);
			re.setCode(0);
		}catch(Exception e) {
			e.printStackTrace();
			re.setCode(1);
			re.setMessage(e.getMessage());
		}
		return re;
	}


}
