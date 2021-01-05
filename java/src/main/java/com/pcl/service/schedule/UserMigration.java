package com.pcl.service.schedule;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pcl.constant.Constants;
import com.pcl.dao.UserDao;
import com.pcl.pojo.mybatis.User;
import com.pcl.util.FileUtil;
import com.pcl.util.JsonUtil;
import com.pcl.util.SHAUtil;
import com.pcl.util.TimeUtil;

@Service
public class UserMigration {
	
	private static Logger logger = LoggerFactory.getLogger(UserMigration.class);

	String userJson = "[{\"password\":\"19902150\",\"address\":\"HIT campus, Xili Town\",\"mobile\":\"13751184856\",\"company\":\"PCL\",\"email\":\"chenfei.ye@foxmail.com\",\"username\":\"figo2150\"},{\"password\":\"Pcl@2020\",\"address\":\"ShenZhen\",\"mobile\":\"233233\",\"company\":\"pcl\",\"email\":\"1832292970@qq.com\",\"username\":\"zexiong6\"},{\"password\":\"15506875809\",\"address\":\"test\",\"mobile\":\"test\",\"company\":\"HIT\",\"email\":\"alfred_xtu@hotmail.com\",\"username\":\"test_aaaa\"},{\"password\":\"aiitzhyl\",\"address\":\"浙江杭州杭州湾智慧谷\",\"mobile\":\"17706516229\",\"company\":\"浙江省北大信息技术高等研究院\",\"email\":\"blacknepia@dingtalk.com\",\"username\":\"AIIT\"},{\"password\":\"podisme.\",\"address\":\"深圳市南山区\",\"mobile\":\"13612994471\",\"company\":\"HITsz\",\"email\":\"357771523@qq.com\",\"username\":\"podismine\"},{\"password\":\"yanhai1993@\",\"address\":\"北京市房山区北京时代广场2-1102\",\"mobile\":\"13240483331\",\"company\":\"北京浩辰星月科技有限公司\",\"email\":\"ai@yanhaiai.tech\",\"username\":\"Yanhai\"},{\"password\":\"123456\",\"mobile\":\"1333333333233\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"liuzxTest19\"},{\"password\":\"yanhai1993@\",\"address\":\"北京市房山区北京时代广场1101\",\"mobile\":\"13240483331\",\"company\":\"北京浩辰星月科技有限公司\",\"email\":\"ai@yanhaiai.tech\",\"username\":\"YanhaiAI\"},{\"password\":\"OpenI/O2020\",\"address\":\"深圳市南山区兴科一街2号\",\"mobile\":\"18310577051\",\"company\":\"鹏城实验室\",\"email\":\"lium@pcl.ac.cn\",\"username\":\"LiuMing\"},{\"password\":\"ABCabc123!\",\"address\":\"安徽合肥\",\"mobile\":\"15755170514\",\"company\":\"科大讯飞股份有限公司\",\"email\":\"syguo4@iflytek.com\",\"username\":\"guoshuyuan\"},{\"password\":\"yyyyyyyy\",\"address\":\"yyyyyyyy\",\"mobile\":\"yyyyyyyy\",\"company\":\"yyyyyyyy\",\"email\":\"1695064014@qq.com\",\"username\":\"yyyyyyyy\"},{\"password\":\"Pcl@2020\",\"address\":\"ShenZhen\",\"mobile\":\"1222\",\"company\":\"pcl\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"zexiong8\"},{\"password\":\"xiangyang1986\",\"address\":\"深圳市西丽兴科一街2号\",\"mobile\":\"15019290001\",\"company\":\"pcl\",\"email\":\"xiangy@pcl.ac.cn\",\"username\":\"xiangy\"},{\"password\":\"123456\",\"mobile\":\"1333333333233\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"liuzxTest21\"},{\"password\":\"123456\",\"mobile\":\"1333333333233\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"liuzxTest3\"},{\"password\":\"123456\",\"mobile\":\"1333333333233\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"liuzxTest22\"},{\"password\":\"uchiha9001\",\"address\":\"pcl.ac\",\"mobile\":\"15333519621\",\"company\":\"pcl\",\"email\":\"sxty32@126.com\",\"username\":\"palytoxin\"},{\"password\":\"Pcl@2020\",\"address\":\"ShenZhen\",\"mobile\":\"2224232\",\"company\":\"pcl\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"zexiong1\"},{\"password\":\"pcl123456\",\"address\":\"鹏城实验室\",\"mobile\":\"鹏城实验室\",\"company\":\"鹏城实验室\",\"email\":\"chenjc@pcl.ac.cn\",\"username\":\"chenjc\"},{\"password\":\"0@monica\",\"address\":\"合肥市望江西路666号\",\"mobile\":\"18117253719\",\"company\":\"科大讯飞\",\"email\":\"ihuangqian@163.com\",\"username\":\"ihuangqian\"},{\"password\":\"abchml00\",\"address\":\"深圳南山联想大厦\",\"mobile\":\"18129975179\",\"company\":\"联想\",\"email\":\"29431475@qq.com\",\"username\":\"黄茂林\"},{\"password\":\"123456\",\"mobile\":\"13333333332\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"liuzxTest1\"},{\"password\":\"123456\",\"address\":\"test\",\"mobile\":\"test\",\"company\":\"test\",\"email\":\"465468086@qq.com\",\"username\":\"testQ\"},{\"password\":\"qiuqiu1990512\",\"address\":\"pcl\",\"mobile\":\"13804608831\",\"company\":\"pcl\",\"email\":\"979026918@qq.com\",\"username\":\"tiang\"},{\"password\":\"Pcl@2020\",\"address\":\"pcl\",\"mobile\":\"pcl\",\"company\":\"pcl\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"zexiong7\"},{\"password\":\"123456\",\"address\":\"zz\",\"mobile\":\"zz\",\"company\":\"zz\",\"email\":\"775679709@qq.com\",\"username\":\"zhangbeijing\"},{\"password\":\"abchml00\",\"address\":\"深圳联想大厦\",\"mobile\":\"18129975179\",\"company\":\"联想\",\"email\":\"29431475@qq.com\",\"username\":\"lantis\"},{\"password\":\"Pcl@2020\",\"address\":\"ShenZhen\",\"mobile\":\"1233\",\"company\":\"pcl\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"zexiong21\"},{\"password\":\"pcl@123\",\"address\":\"深圳市南山区兴科一街2号\",\"mobile\":\"18680307716\",\"company\":\"鹏城实验室\",\"email\":\"zhangt02@pcl.ac.cn\",\"username\":\"imed-001\"},{\"password\":\"yyyyyy\",\"address\":\"abc\",\"mobile\":\"abc\",\"company\":\"abc\",\"email\":\"1695064014@qq.com\",\"username\":\"yyyyyy\"},{\"password\":\"qiuqiu\",\"address\":\"南山西丽\",\"mobile\":\"1360\",\"company\":\"PCL\",\"email\":\"qiujf@pcl.ac.cn\",\"username\":\"qiujf\"},{\"password\":\"OpenI/O2020\",\"address\":\"深圳南山区兴科一街2号\",\"mobile\":\"18310577051\",\"company\":\"鹏城实验室\",\"email\":\"lium@pcl.ac.cn\",\"username\":\"LIUM\"},{\"password\":\"Pcl@2020\",\"address\":\"pcl\",\"mobile\":\"pcl\",\"company\":\"pcl\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"zexiong30\"},{\"password\":\"123456\",\"mobile\":\"133333333343\",\"email\":\"lizhengyangjie@163.com\",\"username\":\"lj001\"},{\"password\":\"123456\",\"address\":\"111\",\"mobile\":\"123\",\"company\":\"鹏城实验室\",\"email\":\"zouap@pcl.ac.cn\",\"username\":\"zouap\"},{\"password\":\"qiuqiu1990512\",\"address\":\"pcl\",\"mobile\":\"13804608831\",\"company\":\"pcl\",\"email\":\"979026918@qq.com\",\"username\":\"tiangeng\"},{\"password\":\"123456\",\"mobile\":\"13333333332\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"liuzxTest2\"},{\"password\":\"123456\",\"address\":\"LL\",\"mobile\":\"LL\",\"company\":\"ll\",\"email\":\"775679709@qq.com\",\"username\":\"zhangsi\"},{\"password\":\"Pcl@2020\",\"address\":\"ShenZhen\",\"mobile\":\"36735635\",\"company\":\"pcl\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"zexiong4\"},{\"password\":\"123456\",\"mobile\":\"1333333333233\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"liuzxTest15\"},{\"password\":\"123456\",\"address\":\"test2\",\"mobile\":\"188\",\"company\":\"test1\",\"email\":\"lizhengyangjie@163.com\",\"username\":\"qiuzhangcheng\"},{\"password\":\"123456\",\"address\":\"pcl\",\"mobile\":\"pcl\",\"company\":\"pcl\",\"email\":\"chenjc@pcl.ac.cn\",\"username\":\"med1\"},{\"password\":\"Pcl@2020\",\"address\":\"ShenZhen\",\"mobile\":\"111\",\"company\":\"pcl\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"zexiong11\"},{\"password\":\"Pcl@2020\",\"address\":\"ShenZhen\",\"mobile\":\"212212232\",\"company\":\"pcl\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"zexiong2\"},{\"password\":\"123456\",\"mobile\":\"133333333343\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"liuzx02\"},{\"password\":\"twtyasdw\",\"address\":\"北京海淀中关村一街6号\",\"mobile\":\"324109108@qq.com\",\"company\":\"四环科技\",\"email\":\"324109108@qq.com\",\"username\":\"asdwtwty\"},{\"password\":\"Pcl@2020\",\"address\":\"ShenZhen\",\"mobile\":\"zexiong7\",\"company\":\"pcl\",\"email\":\"1832292970@qq.com\",\"username\":\"liuzx007\"},{\"password\":\"123456\",\"mobile\":\"1333333333233\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"liuzxTest20\"},{\"password\":\"123456\",\"mobile\":\"133333333343\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"liuzx01\"},{\"password\":\"andyhua7933626\",\"address\":\"华为坂田基地\",\"mobile\":\"18500137349\",\"company\":\"华为\",\"email\":\"andyhua12345@163.com\",\"username\":\"andyhua12345\"},{\"password\":\"Pcl@2020\",\"address\":\"1231\",\"mobile\":\"33\",\"company\":\"pcl\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"zexiong25\"},{\"password\":\"Pcl@123456\",\"mobile\":\"13777777777\",\"email\":\"465468086@qq.com\",\"username\":\"pcladmin\"},{\"password\":\"Pcl@2020\",\"address\":\"ShenZhen\",\"mobile\":\"242442\",\"company\":\"pcl\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"zexiong10\"},{\"password\":\"123456\",\"mobile\":\"1333333333\",\"email\":\"zhouap@pcl.an.com\",\"username\":\"zhouap\"},{\"password\":\"aabbcc\",\"address\":\"aabbcc\",\"mobile\":\"aabbcc\",\"company\":\"aabbcc\",\"email\":\"1695064014@qq.com\",\"username\":\"aabbcc\"},{\"password\":\"xiangyang1986\",\"address\":\"广东深圳\",\"mobile\":\"10000000000\",\"company\":\"PCL\",\"email\":\"xiangy@pcl.ac.cn\",\"username\":\"xy\"},{\"password\":\"123456\",\"mobile\":\"1333333333233\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"liuzxTest18\"},{\"password\":\"15506875809\",\"address\":\"15506875809\",\"mobile\":\"15506875809\",\"company\":\"HIT\",\"email\":\"alfred_xtu@hotmail.com\",\"username\":\"test_aaa\"},{\"password\":\"123456\",\"address\":\"深圳\",\"mobile\":\"133333333\",\"company\":\"HIT\",\"email\":\"lizhengyangjie@163.com\",\"username\":\"lijie003\"},{\"password\":\"123456\",\"mobile\":\"1333333333233\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"liuzxTest17\"},{\"password\":\"123456\",\"mobile\":\"1333333333233\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"liuzxTest13\"},{\"password\":\"Sandglass123\",\"address\":\"浙江杭州杭州湾智慧谷\",\"mobile\":\"18858196668\",\"company\":\"浙江省北大信息技术高等研究院\",\"email\":\"hongan6668@dingtalk.com\",\"username\":\"Hongan\"},{\"password\":\"pcl123456\",\"address\":\"鹏城实验室\",\"mobile\":\"鹏城实验室\",\"company\":\"鹏城实验室\",\"email\":\"chenjc@pcl.ac.cn\",\"username\":\"shizurucc\"},{\"password\":\"Pcl@2020\",\"address\":\"ShenZhen\",\"mobile\":\"36735635\",\"company\":\"pcl\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"zexiong5\"},{\"password\":\"123456\",\"address\":\"zzz\",\"mobile\":\"ccc\",\"company\":\"qqq\",\"email\":\"465468086@qq.com\",\"username\":\"test22\"},{\"password\":\"twtyasdw\",\"address\":\"北京海淀中关村一街6号\",\"mobile\":\"324109108@qq.com\",\"company\":\"四环科技\",\"email\":\"asdwtwty@163.com\",\"username\":\"xhf_wz\"},{\"password\":\"asdfadsf\",\"address\":\"哈工大深圳\",\"mobile\":\"18200982535\",\"company\":\"哈工大\",\"email\":\"myuweb@qq.com\",\"username\":\"myorz\"},{\"password\":\"pcl@123\",\"address\":\"No.1 Xingke Rd\",\"mobile\":\"18680307716\",\"company\":\"鹏城实验室\",\"email\":\"zhangt02@pcl.ac.cn\",\"username\":\"imed-002\"},{\"password\":\"Pcl@2020\",\"address\":\"ShenZhen\",\"mobile\":\"pcl\",\"company\":\"pcl\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"zexiong9\"},{\"password\":\"yyyy213\",\"address\":\"yyyy213\",\"mobile\":\"yyyy213\",\"company\":\"yyyy213\",\"email\":\"1695064014@qq.com\",\"username\":\"yyyy2132\"},{\"password\":\"HXH691314huang\",\"address\":\"侨香路4018号\",\"mobile\":\"17776686163\",\"company\":\"深圳爱生生命科技有限公司\",\"email\":\"shen3298@qq.com\",\"username\":\"Kevin\"},{\"password\":\"md293721\",\"address\":\"无\",\"mobile\":\"15776698661\",\"company\":\"无\",\"email\":\"717142627@qq.com\",\"username\":\"孟丹\"},{\"password\":\"hjl780000\",\"address\":\"万科云城\",\"mobile\":\"13247554735\",\"company\":\"鹏城实验室\",\"email\":\"17295146@qq.com\",\"username\":\"hujl\"},{\"password\":\"wewewewewew\",\"address\":\"22\",\"mobile\":\"\",\"company\":\"11\",\"email\":\"gsunfehud@163.com\",\"username\":\"12ee\"},{\"password\":\"Pcl@2020\",\"address\":\"pcl\",\"mobile\":\"pcl\",\"company\":\"pcl\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"zexiong100\"},{\"password\":\"pa123456\",\"address\":\"鹏程实验室\",\"mobile\":\"15377484715\",\"company\":\"人工智能\",\"email\":\"senluowanxiangt@163.com\",\"username\":\"berry\"},{\"password\":\"yxcoder\",\"address\":\"yxcoder\",\"mobile\":\"yxcoder\",\"company\":\"yxcoder\",\"email\":\"yxcoder\",\"username\":\"yxcoder\"},{\"password\":\"yyyy213\",\"address\":\"yyyy213\",\"mobile\":\"yyyy213\",\"company\":\"yyyy213\",\"email\":\"yyyy213@123.com\",\"username\":\"yyyy213\"},{\"password\":\"Pcl@2020\",\"address\":\"ShenZhen\",\"mobile\":\"1222\",\"company\":\"pcl\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"zexiong20\"},{\"password\":\"123456\",\"address\":\"ww\",\"mobile\":\"zz\",\"company\":\"qq\",\"email\":\"465468086@qq.com\",\"username\":\"qiuzhangcheng4\"},{\"password\":\"123456\",\"mobile\":\"1333333333233\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"liuzxTest14\"},{\"password\":\"OpenI/O2020\",\"address\":\"深圳市南山区兴科一街2号鹏城实验室\",\"mobile\":\"18310577051\",\"company\":\"PCL\",\"email\":\"lium@pcl.ac.cn\",\"username\":\"刘明\"},{\"password\":\"yxccccc\",\"address\":\"yxccccc\",\"mobile\":\"yxccccc\",\"company\":\"yxccccc\",\"email\":\"1695064014@qq.com\",\"username\":\"yxccccc\"},{\"password\":\"Pcl@123456\",\"mobile\":\"13777777777\",\"email\":\"zhangsansan@pcl.cn.com\",\"username\":\"xiaoai\"},{\"password\":\"123456\",\"mobile\":\"1333333333\",\"email\":\"zhouap@pcl.an.com\",\"username\":\"zhoupzh\"},{\"password\":\"2115959zhang\",\"address\":\"北京\",\"mobile\":\"18514752298\",\"company\":\"zyy\",\"email\":\"zhangyingyinghku@163.com\",\"username\":\"zyy\"},{\"password\":\"pa123456\",\"address\":\"湖北武汉\",\"mobile\":\"17786579160\",\"company\":\"人工智能\",\"email\":\"senluowanxiangt@163.com\",\"username\":\"berry1\"},{\"password\":\"zpz8023jxm\",\"address\":\"aa\",\"mobile\":\"177\",\"company\":\"pcl\",\"email\":\"hit172587zpz@163.com\",\"username\":\"zhoupzh1\"},{\"password\":\"123456\",\"mobile\":\"1333333333233\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"liuzxTest16\"},{\"password\":\"Sunyu1982\",\"address\":\"深圳市南山区兴科一街\",\"mobile\":\"15013757190\",\"company\":\"鹏城实验室\",\"email\":\"suny@pcl.ac.cn\",\"username\":\"suny\"},{\"password\":\"yyyyyyy\",\"address\":\"yyyyyyy\",\"mobile\":\"yyyyyyy\",\"company\":\"yyyyyyy\",\"email\":\"1695064014@qq.com\",\"username\":\"yyyyyyy\"},{\"password\":\"123456\",\"mobile\":\"133333333343\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"liuzx\"},{\"password\":\"Pcl@2020\",\"address\":\"pcl\",\"mobile\":\"pcl\",\"company\":\"pcl\",\"email\":\"liuzx@pcl.ac.cn\",\"username\":\"zexiong101\"},{\"password\":\"123456\",\"address\":\"深圳\",\"mobile\":\"178\",\"company\":\"HIT\",\"email\":\"lizhengyangjie@163.com\",\"username\":\"lijie009\"}]";
	
	@Autowired
	private UserDao userDao;
	
	//@PostConstruct
	public void init() {
		
		List<Map<String,Object>> userList = JsonUtil.getLabelList(userJson);
		
		int count = 0;
		
		for(Map<String,Object> userMap : userList) {
			String userName = userMap.get("username").toString();
			List<User> re = userDao.queryUser(userName);
			if(re != null && re.size() >0) {
				continue;
			}
			String password = getStrValue("password", userMap);
			
			
			User user = new User();
			user.setUsername(userName);
			user.setPassword(SHAUtil.getEncriptStr(password));
			user.setAddress(getStrValue("address", userMap));
			user.setCompany(getStrValue("company", userMap));
			user.setMobile(getStrValue("mobile", userMap));
			user.setIs_superuser(Constants.USER_LABEL);
			user.setEmail(getStrValue("email", userMap));
			user.setDate_joined(TimeUtil.getCurrentTimeStr());
			userDao.addUser(user);
			count++;
		}
		
		
		logger.info("insert user finished. total add user:" + count);
		
	}
	
	private String getStrValue(String key,Map<String,Object> map) {
		Object value = map.get(key);
		if(value != null && value.toString().length() > 0){
			return value.toString();
		}
		return null;
	}
	
	
	public static void main(String[] args) {
		String path = "C:\\Users\\邹安平\\Downloads";
		
		File files[] = new File(path).listFiles();
		
		List<File> fileList = new ArrayList<>();
		
		for(File file :  files) {
			if(file.isFile() && file.getName().endsWith("png")) {
				fileList.add(file);
			}
		}
		fileList.sort(new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return (int)(o1.lastModified() - o2.lastModified());
			}
		});
		int count = 0;
		for(File file : fileList) {
			count++;
			System.out.println(file.getName());
			FileUtil.copyFile(file.getAbsolutePath(), "D:\\2020文档\\叶老师标注\\video\\image" + count + ".png");
		}
		
	}
	
}
