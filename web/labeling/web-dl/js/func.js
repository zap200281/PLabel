function getIp(){
   
    //return "http://192.168.62.129:8000";
    
    //return "http://192.168.62.129";
	return "http://127.0.0.1";
	
	//return "http://111.231.109.64:8000";
}


function isEmpty(str){
    if(typeof str == "undefined" || str == null || str == ""){
       return true;
    }
    return false;
}

function redirect(response){
	
	if("REDIRECT" == response.getResponseHeader("REDIRECT")){ //若HEADER中含有REDIRECT说明后端想重定向，
          var win = window;
          while(win != win.top){
                    win = win.top;
          }
          win.location.href = getIp() +"/login.html";//使用win.location.href去实现重定向到登录页面
    }
	
}

function setCookie(name,value)
{
    var Days = 7;
    var exp = new Date();
    exp.setTime(exp.getTime() + Days*24*60*60*1000);
    document.cookie = name + "="+ escape (value) + ";expires=" + exp.toGMTString();
}

//读取cookies
function getCookie(name)
{
    var arr,reg=new RegExp("(^| )"+name+"=([^;]*)(;|$)");
 
    if(arr=document.cookie.match(reg))
 
        return unescape(arr[2]);
    else
        return null;
}

function delCookie(name)
{
    var exp = new Date();
    exp.setTime(exp.getTime() - 1);
    var cval=getCookie(name);
    if(cval!=null)
        document.cookie= name + "="+cval+";expires="+exp.toGMTString();
}

//检查是否都符合 注册 要求 
function check_reg() 
{ 
    if(check_email(document.getElementById('email')) && check_web(document.getElementById('web'))) 
    { 
        return true; 
    }else{ 
        return false; 
    } 
} 
//检查密码长度不能少于6 
function check_len(thisObj){ 
    if(thisObj.value.length==0) 
    { 
        document.getElementById('show_pass').innerHTML="密码不能为空"; 
        return false; 
    }else{ 
        if (thisObj.value.length<6) 
        { 
            document.getElementById('show_pass').innerHTML="密码长度不少于6"; 
            return false; 
        } 
        document.getElementById('show_pass').innerHTML=""; 
        return true; 
    } 
} 
//检查俩次密码输入是否一致 
function check_pass(thisObj){ 
    var psw=document.getElementById('pass'); 
    if(psw.value.length==0) 
    { 
        document.getElementById('show_pass').innerHTML="密码不能为空"; 
        return false; 
    }else{ 
        document.getElementById('show_pass').innerHTML=""; 
        if (thisObj.value!=psw.value) 
        { 
            document.getElementById('show_repass').innerHTML="两次密码输入不正确"; 
            return false; 
        } 
        document.getElementById('show_repass').innerHTML=""; 
        return true; 
    } 
} 
//检查email是否正确 
function check_email(thisObj){ 
    var reg=/^([a-zA-Z\d][a-zA-Z0-9_]+@[a-zA-Z\d]+(\.[a-zA-Z\d]+)+)$/gi; 
    var rzt=thisObj.value.match(reg); 
    if(thisObj.value.length==0){ 
        document.getElementById('show_e').innerHTML="Email不能为空"; 
        return false; 
    }else{ 
        if (rzt==null) 
        { 
            document.getElementById('show_e').innerHTML="Email地址不正确"; 
            return false; 
        } 
        document.getElementById('show_e').innerHTML=""; 
        return true; 
    } 
} 
//检查web是否正确 
function check_web(thisObj){ 
    //var reg=/^\w+([\.\-]\w)*$/; 
    //var rzt=thisObj.value.match(reg); 
    if(thisObj.value.length==0){ 
        document.getElementById('show_web').innerHTML="主页不能为空"; 
        return false; 
    }else{ 
        /*if (rzt==null) 
        { 
            document.getElementById('show_web').innerHTML="主页地址不正确"; 
            return false; 
        } */
		var strRegex = "^((https|http|ftp|rtsp|mms)?://)"  
			+ "?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?" //ftp的user@  
			+ "(([0-9]{1,3}\.){3}[0-9]{1,3}" // IP形式的URL- 199.194.52.184  
			+ "|" // 允许IP和DOMAIN（域名） 
			+ "([0-9a-z_!~*'()-]+\.)*" // 域名- www.  
			+ "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\." // 二级域名  
			+ "[a-z]{2,6})" // first level domain- .com or .museum  
			+ "(:[0-9]{1,4})?" // 端口- :80  
			+ "((/?)|" // a slash isn't required if there is no file name  
			+ "(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$";  
		var re=new RegExp(strRegex);  
		if(!re.test(thisObj.value))
		{
			document.getElementById('show_web').innerHTML="主页地址不正确"; 
            return false;
		}
        document.getElementById('show_web').innerHTML=""; 
        return true; 
    } 
} 

function isRightLatitude(lat){
	var latreg = /^(\-|\+)?([0-8]?\d{1}\.\d{0,6}|90\.0{0,6}|[0-8]?\d{1}|90)$/;
	
	if(!latreg.test(lat)){
		return false;
		//console.log("纬度整数部分为0-90,小数部分为0到6位!");
	}
	return true;
}

function isRightLongitude(longitude){
	var longrg = /^(\-|\+)?(((\d|[1-9]\d|1[0-7]\d|0{1,3})\.\d{0,6})|(\d|[1-9]\d|1[0-7]\d|0{1,3})|180\.0{0,6}|180)$/;
    if(!longrg.test(longitude)){
	   return false;
       //console.log("经度整数部分为0-180,小数部分为0到6位!");
    }
	return true;
}

function isRightDateTime_dot(time){
	//2020-05-20 14:20:20
	if(time.length != 19){
		return false;
	}
	return strDateTime(time);
}

function strDateTime(str)
{
	var reg = /^(\d{1,4})(-|\/)(\d{1,2})\2(\d{1,2}) (\d{1,2}):(\d{1,2}):(\d{1,2})$/;
	var r = str.match(reg);
	if(r==null){
		return false;
	}
	var d= new Date(r[1], r[3]-1,r[4],r[5],r[6],r[7]);
	return (d.getFullYear()==r[1]&&(d.getMonth()+1)==r[3]&&d.getDate()==r[4]&&d.getHours()==r[5]&&d.getMinutes()==r[6]&&d.getSeconds()==r[7]);
}

function isRightDateTime_yyyyMMddHHmmss(time){
	//20200520142020
	if(time.length != 14){
		return false;
	}
	var year = time.substring(0,4);
	var month = time.substring(4,6);
	var day = time.substring(6,8);
	var hour = time.substring(8,10);
	var minute = time.substring(10,12);
	var second = time.substring(12,14);
	console.log("year=" + year + " month=" + month + "day=" + day);
	var d= new Date(year,month-1,day,hour,minute,second);
	return (d.getFullYear()==year && (d.getMonth()+1)== month&& d.getDate()== day && d.getHours()==hour && d.getMinutes()==minute && d.getSeconds()==second);
	
}

function isDateString(strDate){
	var strSeparator = "-"; //日期分隔符
	var strDateArray;
	var intYear;
	var intMonth;
	var intDay;
	var boolLeapYear;
	var ErrorMsg = ""; //出错信息
	strDateArray = strDate.split(strSeparator);
	//没有判断长度,其实2008-8-8也是合理的//strDate.length != 10 ||
	if(strDateArray.length != 3) {
		ErrorMsg += "日期格式必须为: yyyy-MM-dd";
		return ErrorMsg;
	}
	intYear = parseInt(strDateArray[0],10);
	intMonth = parseInt(strDateArray[1],10);
	intDay = parseInt(strDateArray[2],10);
	if(isNaN(intYear)||isNaN(intMonth)||isNaN(intDay)) {
		ErrorMsg += "日期格式错误: 年月日必须为纯数字";
		return ErrorMsg;
	}
	if(intMonth>12 || intMonth<1) {
		ErrorMsg += "日期格式错误: 月份必须介于1和12之间";
		return ErrorMsg;
	}
	if((intMonth==1||intMonth==3||intMonth==5||intMonth==7
	||intMonth==8||intMonth==10||intMonth==12)
	&&(intDay>31||intDay<1)) {
		ErrorMsg += "日期格式错误: 大月的天数必须介于1到31之间";
		return ErrorMsg;
	}
	if((intMonth==4||intMonth==6||intMonth==9||intMonth==11)
	&&(intDay>30||intDay<1)) {
		ErrorMsg += "日期格式错误: 小月的天数必须介于1到31之间";
		return ErrorMsg;
	}
	if(intMonth==2){
	if(intDay < 1) {
		ErrorMsg += "日期格式错误: 日期必须大于或等于1";
		return ErrorMsg;
	}
	boolLeapYear = false;
	if((intYear%100) == 0){
		if((intYear%400) == 0)
			boolLeapYear = true;
	}
	else{
		if((intYear % 4) == 0)
			boolLeapYear = true;
		}
		if(boolLeapYear){
			if(intDay > 29) {
				ErrorMsg += "日期格式错误: 闰年的2月份天数不能超过29";
				return ErrorMsg;
			}
		} else {
			if(intDay > 28) {
				ErrorMsg += "日期格式错误: 非闰年的2月份天数不能超过28";
				return ErrorMsg;
			}
		}
	}
   return ErrorMsg;
} 
