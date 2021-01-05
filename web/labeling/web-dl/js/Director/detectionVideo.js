

var regions;
var img=new Image();
var imginfo = {};
var imgHeight;
var imgWidth;
var timeErr = 0.1
var ip = getIp();
var token = getCookie("token");
console.log("token=" + token);
var userType = getCookie("userType");

//获取元素绝对位置 获取鼠标坐标
function getElementLeft(element){
    var actualLeft = element.offsetLeft;
    var current = element.offsetParent;
    while (current !==null){
        actualLeft += (current.offsetLeft+current.clientLeft);
        current = current.offsetParent;
    }
    return actualLeft;
}

function getElementTop(element){
    var actualTop = element.offsetTop;
    var current = element.offsetParent;
    while (current !== null){
        actualTop += (current.offsetTop+current.clientTop);
        current = current.offsetParent;
    }
    return actualTop;
}

// canvas 矩形框集合
//by yunpeng zhai
//变量定义//变量重置
//

var label_task_id   = sessionStorage.getItem("label_task_id");
var label_task_name = sessionStorage.getItem("label_task_name");
var resolutionRatio = sessionStorage.getItem("resolutionRatio");

var reg=/^\d{2,4}[x]\d{2,4}/;
if (!reg.test(resolutionRatio)){
    resolutionRatio="3840x2160";
}

var label_task={'id':label_task_id, "zip_object_name":label_task_name,"resolutionRatio":resolutionRatio };
console.log("label_task=" + label_task);
var video_task_info;
var labeltastresult;

var label_attributes ;



function updateSetting(){
  // hidePopup();
  // show_region_attributes_update_panel();
  var set_attributes = document.getElementById("set_attributes")
  set_attributes.setAttribute('style', 'top:' + 185 + 'px;left:' + 330 + 'px;width:'+ 502+'px;position:absolute');
  update_attributes_update_panel();
}


function save_attribute(){
  var set_attributes = document.getElementById("set_attributes");

  var _via_attributes_str = JSON.stringify(_via_attributes['region']);
  update_labeltask(_via_attributes_str);

  set_attributes.style.display = "none";
}

function close_attribute(){
   set_attributes.style.display = "none";
   onload();
   document.getElementById("user_input_attribute_id").value='';
   document.getElementById('attribute_properties').innerHTML = '';
}


function close_exist_child_attributes(){
  var set_attributes = document.getElementById("atttibute_child");
    document.getElementById('atttibute_childe').innerHTML = '';

  //   var atttibute_child = document.getElementById('atttibute_child');
  // if ( atttibute_child ) {
  //   atttibute_child.style.display = "none";
  //   // p.remove();
  //   // 
  // }

// set_attributes.style.display = "none";

}




//边栏
//任务信息
function showtaskinfo(){

 var video_name = (label_task.zip_object_name).split("/");
  $('#task_info').text(video_name[video_name.length-1]);
  // $('#task_progress').text(label_task_info.task_status);
  
}

  


  function update_labeltask(){
    console.log("label_task_id=" + label_task.id);
    // console.log("task_label_type_info=" + task_label_type_info);
    
      $.ajax({
         type:"PATCH",
         url:ip + "/api/label-task/",
         headers: {
            authorization:token,
          },
         dataType:"json",
         data:{
           'label_task_id':label_task.id,
           // 'task_label_type_info':task_label_type_info,
         },
         async:false,
         success:function(json){
           console.log(json);
        },
	    error:function(response) {
		  redirect(response);
        }
     });
  }


function list(current,pageSize){
    $.ajax({
       type:"GET",
       url:ip + "/api/video-count-task-item",
       headers: {
          authorization:token,
        },
       dataType:"json",
     data:{
       'label_task':label_task.id,
       'startPage':current,
       'pageSize':pageSize
       },
       async:false,
       success:function(json){
        tablePageData = json;
        if (isEmpty(tablePageData)){
          return;
        }
        tableData = json.data;
    // labeltastresult = tableData;
      // fileindex=0;
      // if(lastindex){
      //   fileindex = pageSize - 1;
      // }
        console.log(json);
        // return json.token;
       },
	    error:function(response) {
		  redirect(response);
        }
   });
}

function traj_mode_verifyChange(traj_mode){
    $.ajax({
       type:"GET",
       url:ip + "/api/video-count-task-locus",
       headers: {
          authorization:token,
        },
       dataType:"json",
       data:{
         'label_task':label_task.id,
         'locus':traj_mode,
       },
       async:false,
       success:function(json){
        tableTrajData = json;
       },
	    error:function(response) {
		  redirect(response);
        }
   });
}


var pageSize = 8;
// var tableData;
var tablePageData;
var tableTrajData;

function page(current,pageSize){
  list(current,pageSize);
  if(isEmpty(tablePageData)){
          return;
  }
  display_video_info();
  setPage(tablePageData,pageSize);
}

function nextPage(){
   var current = $('#displayPage1').text();
   console.log("current=" + current);
   page(current,pageSize);
}

function prePage(){
  var current =$('#displayPage1').text();
  console.log("current=" + current);
  if(current > 1){
    console.log("current=" + (current - 2));
    page(current - 2,pageSize);
  } 
}

function goPage(){
   var goNum = $('#goNum').val();

    var pageTotal = $("#totalNum").text();
    var pageNum = parseInt(pageTotal/pageSize);
    if(pageTotal%pageSize!=0){
        pageNum += 1;
    }else {
        pageNum = pageNum;
    }
    if (goNum<=0){
      alert("请输入大于0的数值");
    }
    else if(goNum<=pageNum){
        page(goNum - 1,pageSize);
    }
    else{
        alert("不能超出总页码！");
    }
}

$("#goNum").keydown(function (e) {
    if (e.keyCode == 13) {
        goPage();
    }
});


function setPage(pageData,pageSize){
  if (isEmpty(pageData)){
    return;
  }
  var startIndex = pageData.current * pageSize;
  if(pageData.total > 0){
	  startIndex = startIndex + 1;
  }	
  if(startIndex < 10){
    $('#startIndex').text(" " + (startIndex));
  }else{
    $('#startIndex').text(startIndex);
  }
  var endIndex = pageData.current * pageSize + pageData.data.length;
  if(endIndex < 10){
     $('#endIndex').text(" " + (endIndex));
  }else{
     $('#endIndex').text(endIndex);
  }
 
  $('#totalNum').text(pageData.total);
  $('#displayPage1').text(pageData.current + 1);

  console.log("set prePage status, pageData.current=" + pageData.current);

  if(pageData.current == 0){
    console.log("set prePage disabled.");
    $('#prePage').removeAttr("href");
  }
  else{
    $('#prePage').attr("href","javascript:prePage()");
  }

  if((pageData.current + 1) * pageSize >= pageData.total){
    console.log("set nextPage disabled.");
    $('#nextPage').removeAttr("href");
  }
  else{
    $('#nextPage').attr("href","javascript:nextPage()");
  }

  var pageTotal = pageData.total;
  var pageNum = parseInt(pageTotal/pageSize);
  if(pageTotal%pageSize!=0){
      pageNum += 1;
  }else {
      pageNum = pageNum;
  }
  $("#totalPageNum").text(pageNum);
}

 
  window.onload = function() {
    
    console.log("onload tasks/detect/index.html");
    var token = getCookie("token");
    if(typeof token == "undefined" || token == null || token == ""){
        console.log("token=" + token);
        window.location.href = "../../login.html";
    }else{
        var nickName = getCookie("nickName");
        console.log("nickName=" + nickName);
        $("#userNickName").text(nickName);
        $("#userNickName_bar").text(nickName);
    }

    labelwindow = document.getElementById("labelwin");
    // labelwindow.width = document.getElementById("tool0").offsetWidth;
    // labelwindow.height = document.getElementById("tool0").offsetWidth/1280*720;

    // canvas = document.getElementById("myCanvas");
    // context = canvas.getContext("2d");
    // canvas.width = document.getElementById("tool0").offsetWidth;
    // canvas.height = document.getElementById("tool0").offsetWidth/1280*720;
    
    video = document.getElementById("myVideo");
    video.width = document.getElementById("myVideo").offsetWidth;
    video.height = document.getElementById("myVideo").offsetWidth/1280*720;
	video.onmousedown = mouseClick;
  	video.onpause = pauseToDo;
	video.onplay = play;
  	
    get_init_atrribute(); //初始化结构属性
    car_category();
    traj_mode();
    if(userType==2){
      document.getElementById("verify").style.display="inline-block"
      traj_mode_verify();
    }

    road_id();
    get_color();

    files  = ip+ label_task.zip_object_name;//"/minio/label-img/1586502176987-test111.mp4";
    var WH = label_task.resolutionRatio.split("x") ;
    imgHeight =WH[1];
    imgWidth=WH[0];
    console.log("zip_object_name",files[0]);
     // File file2 = new File();
    $("video").attr("src", files);
 
    showtaskinfo();
  // _via_init();

    page(0,pageSize);
  

  };

      function get_init_atrribute(){
       _via_attributes = {'region':{}};
        var atti = "id"; // 属性名
       _via_attributes['region'][atti] = {};
       _via_attributes['region'][atti]["type"] = "text";
       _via_attributes['region'][atti]["description"] = "";
       _via_attributes['region'][atti]["default_value"] = "";



       atti = "traj_mode"; // 属性名
       _via_attributes['region'][atti] = {};
       _via_attributes['region'][atti]["type"] = "dropdown";
       _via_attributes['region'][atti]["description"] = "";
       _via_attributes['region'][atti]["options"] = {};
       _via_attributes['region'][atti]["options"]={"1":"DU","2":"UD","3":"RL","4":"LR","5":"DR","6":"UL","7":"RU","8":"LD","9":"DL","10":"UR","11":"RD","12":"LU","13":"其他"};
       _via_attributes['region'][atti]["default_options"] = {};

       atti = "road_id"; // 属性名
       _via_attributes['region'][atti] = {};
       _via_attributes['region'][atti]["type"] = "dropdown";
       _via_attributes['region'][atti]["description"] = "";
       _via_attributes['region'][atti]["options"] = {};
       _via_attributes['region'][atti]["options"]={"1":"1","2":"2","3":"3","4":"4","5":"5","6":"6"};
       _via_attributes['region'][atti]["default_options"] = {};

       atti = "type"; // 属性名
       _via_attributes['region'][atti] = {};
       _via_attributes['region'][atti]["type"] = "dropdown";
       _via_attributes['region'][atti]["description"] = "";
       _via_attributes['region'][atti]["options"] = {};
       _via_attributes['region'][atti]["options"] = {"1":"轿车", "2":"SUV","3":"越野车","4":"出租车","5":"商务车","6":"载人面包车","7":"军警车-轿车","8":"军警车-SUV","9":"军警车-越野车","10":"军警车-商务车","11":"军警车-载人面包车","12":"军警车-客车","13":"中型客车","14":"公交车","15":"大型客车","16":"货用面包车","17":"厢式货车","18":"微型货车","19":"皮卡车","20":"救援车","21":"大型货车","22":"渣土车","23":"挂车","24":"罐车","25":"混凝土搅拌车","26":"随车吊","27":"救护车","28":"三轮车","29":"其他"};
       _via_attributes['region'][atti]["default_options"] = {};
 
        atti = "color"; // 属性名
       _via_attributes['region'][atti] = {};
       _via_attributes['region'][atti]["type"] = "dropdown";
       _via_attributes['region'][atti]["description"] = "";
       _via_attributes['region'][atti]["options"] = {};
       _via_attributes['region'][atti]["options"] = {"1":"黑", "2":"白","3":"灰","4":"红","5":"蓝","6":"黄","7":"橙","8":"棕","9":"绿","10":"紫","11":"青","12":"粉","13":"银","14":"金","15":"混色","16":"其他","17":"未知"};
       _via_attributes['region'][atti]["default_options"] = {};
  }


//获取元素绝对位置 获取鼠标坐标
function getElementLeft(element){
    var actualLeft = element.offsetLeft;
    var current = element.offsetParent;
    while (current !==null){
        actualLeft += (current.offsetLeft+current.clientLeft);
        current = current.offsetParent;
    }
    return actualLeft;
}

//画布内鼠标响应操作
function mouseClick(e) {
  // if(e.button ==0){
                  

   var clickX = e.pageX - getElementLeft(video);
   var clickY = e.pageY - getElementTop(video);
   console.log("************:",clickX+","+clickY);
   var tmpshow = document.getElementById("labelShow_0");
   tmpshow.style.display = "block";
   tmpshow.style.left = clickX+5+"px";
   tmpshow.style.top = clickY-20+"px";
   document.getElementById("locInfor").innerHTML= "X:"+getRealLocationX(clickX)+";Y:"+getRealLocationY(clickY);
  // }
}

document.onkeydown = function(e){
    switch(e.keyCode){
        case 32:
          e.preventDefault();
    }
}

document.onkeyup=function(e){  
    console.log(e.keyCode)
    console.log(window.event);
    e=e||window.event;  
    e.preventDefault();
    obj = e.srcElement||e.target;
    if( obj != null && obj !=undefined ){
      if(obj.type == "textarea" || obj.type=='text' || obj.type=="button" || obj.type=="select"){
        //console.log("obj.type:"+obj.type);
          return ;
      }
    }

    switch(e.keyCode){  
      case 87: //W
        // boxcls = classes;
        createRectLabel();
   
        break; 
      case 68:
        deleterect();
        break;
      case 27:
        cancel();
        break;
      case 83:
        save();
        break;
      case 81:
        last();
        break;
      case 69:
        next();
        break;
      case 32:
        playOrPause();
        break;
    };
}

var videoPauseTime;
var isplay = false;//默认是不播放的
var video = document.getElementById("myVideo");
var DurationTime;

function clearPoint(){
	console.log("clear all point.");
	for (i=0;i<5;i++){
       var tmp="labelShow_"+i;
       document.getElementById(tmp).style.display="none"
	}
}
	

function playOrPause(){
    if(isplay) {//判断当前播属放状态
      isplay = false;
      video.pause();
      videoPauseTime = video.currentTime;
      console.log("current111111=" +(video.currentTime) + "video.duration :=" + video.duration );
      }
    else {
      isplay = true;
	  clearPoint();
      video.play();
    }
}
// v.addEventListener('play', function() {var i=window.setInterval(function() {ctx.drawImage(v,0,0,270,135)},20);},false);
video.ontimeupdate =function(){ 
  console.log("current333333333333333333");
 
  if (userType==2){
       var indexs = isExite();
        //重新赋值不显示
        for (i=0;i<5;i++){
          var tmp="labelShow_"+i;
          document.getElementById(tmp).style.display="none"
        }
        // document.getElementById("inforShow").style.display="none";
        //不为空显示原点
        if (!isEmpty(indexs)){
            document.getElementById("inforShow").style.display="none";
            drawPoint(indexs);
            DurationTime = video.currentTime;
        }
        if (parseFloat(video.currentTime -DurationTime)>1){
            for (i=0;i<5;i++){
              var tmp="labelShow_"+i;
              document.getElementById(tmp).style.display="none";
            }
          // document.getElementById("inforShow").style.display="none";
        }
    }
};
function isExite(){
    var eqIndexs = [];
    var videoTime = video.currentTime;
    if (isEmpty(tableTrajData)){
      return;
    }
    for (var i=0;i<tableTrajData.length;i++){   
        var label_info = JSON.parse(tableTrajData[i].label_info);
        var time = strms2time(label_info.time);
        // console.log("videoTime:"+parseInt(videoTime));
        //  console.log("time:"+parseInt(time));
        if (Math.abs(parseFloat(videoTime)-parseFloat(time))<timeErr){
          // console.log("666666666666666666");
            eqIndexs.push(i);
        }
    }

    return eqIndexs;
}

var carTotal =0;
function getTotal(){
   carTotal =0;
   for (var i=0;i<tableTrajData.length;i++){
    var label_info = JSON.parse(tableTrajData[i].label_info);
    if (strms2time(label_info.time)<=video.currentTime+timeErr){
       carTotal = carTotal + 1;
    }
   }
}
function drawPoint(eqIndexs){
    var maxLen = Math.min(eqIndexs.length,5);
    var html ="<tr>\
              <th>时间戳</th>\
              <th>轨迹形式</th>\
              <th>车道号</th>\
              <th>车辆类别</th>\
              <th>颜色</th>\
              <th>总的过车数</th>\
                </tr>";
    for (i=0;i<maxLen;i++){
           // carTotal = carTotal+1;
           getTotal();
           var tmiId = "labelShow_"+i;
           var tmpshow = document.getElementById(tmiId);

           var label_info = JSON.parse(tableTrajData[eqIndexs[i]].label_info);
           if (!isEmpty(label_info.loc)){
               var xy = label_info.loc.split(";");
               tmpshow.style.display = "block";
               tmpshow.style.left = getCanvasLocationX(parseInt(xy[0].split(":")[1]))+5+"px";
               tmpshow.style.top = getCanvasLocationY(parseInt(xy[1].split(":")[1]))-20+"px";
           }

           var row = "<tr>\
              <td>"+label_info.time+"</td>\
              <td>"+label_info.traj_mode+"</td>\
              <td>"+label_info.road_id+"</td>\
              <td>"+_via_attributes['region']["type"]["options"][label_info.car_category]+"</td>\
              <td>"+ _via_attributes['region']["color"]["options"][label_info.color]+"</td>"
           html = html + row;  
           if (i==0){
             html= html + "<td rowspan ="+ maxLen+ "align=\"center\">"+carTotal+ "</td>" ;
           } 
           // else{
           //  html= html + "<td>"+ "</td>"
           // } 
             
              
          html = html +  "</tr>";
    }
    document.getElementById("inforShow").style.display="block";
    document.getElementById("inforShow").innerHTML = html;

}



function play() {
  // document.getElementById("videoInfo").style.display = 'none';
  var video =  document.getElementById("myVideo");//document.getElementsByTagName('video')[0];
  clearPoint();
  video.play();  
}

function pauseToDo(){
  console.log("current334444433");
  var video =  document.getElementById("myVideo");//document.getElementsByTagName('video')[0];
  videoPauseTime = video.currentTime;
  document.getElementById("time").value = time2strms(videoPauseTime);
  document.getElementById("videoInfo").style.display = 'block';
}


function pause() {

  var video =  document.getElementById("myVideo");//document.getElementsByTagName('video')[0];
  video.pause();
 
  pauseToDo();

}

  function time2strms (t) {
  var hh = Math.floor(t / 3600);
  var mm = Math.floor( (t - hh * 3600) / 60 );
  var ss = Math.floor( t - hh*3600 - mm*60 );
  var ms = Math.floor( (t - Math.floor(t) ) * 1000 );
  if ( hh < 10 ) {
    hh = '0' + hh;
  }
  if ( mm < 10 ) {
    mm = '0' + mm;
  }
  if ( ss < 10 ) {
    ss = '0' + ss;
  }
  //毫秒保留三位小数
  if ( ms < 10 ) {
    ms = '00' + ms;
  }
  else if ( ms < 100 ) {
    ms = '0' + ms;
  }
  return hh + ':' + mm + ':' + ss + '.' + ms;
    // return hh + ':' + mm + ':' + ss ;
}


function strms2time (str) {
      var strTime = str.split(":");
      var t  = parseInt(strTime[0])*3600+parseFloat(strTime[1])*60 + parseFloat(strTime[2]);
      return t ;
}


function playSlow(){
  video.playbackRate=0.5;
}

function playFast(){
  video.playbackRate=1.5;
}

function playNormal(){
  video.playbackRate=1;
}



function selectVelocity(value){
   var video =  document.getElementById("myVideo");
  switch(value){
      case '1':
              video.playbackRate= 0.5;
              break;
      case '2':
              video.playbackRate= 1;
              break;
      case '3':
              video.playbackRate= 1.5;
              break;
      case '4':
              video.playbackRate= 2;
              break;
      case '5':
              video.playbackRate= 2.5;
              break;
      case '6':
              video.playbackRate= 3;
              break;
      case '7':
              video.playbackRate= 0.2;
              break;
  }

}


function traj_mode(){

  var html =html_get();
  document.getElementById('traj_mode').innerHTML=html; 

}

function html_get(){
      var html="<option value=\"\" selected=\"\">请选择</option>";
  // for (var i=0;i<userInfoData.length;i++){
     for (var i in _via_attributes['region']["traj_mode"]["options"]){
       var row = "<option value=\""+_via_attributes['region']["traj_mode"]["options"][i]+
        "\">"+_via_attributes['region']["traj_mode"]["options"][i]+
         "</option>";
        html=html+row; 
     }
     return html;

}
function traj_mode_verify(){

  var html= html_get();
  document.getElementById('traj_mode_verify').innerHTML=html; 


}


function showHideRoadId(text){
    if (text.substr(0, 1) ==  "D"){
        document.getElementById('divRoadId').style.display = 'block';
		
    }
    else{
      document.getElementById('divRoadId').style.display = 'none';
	  $('#road_id').val("");
    }
 }

function car_category(){



    var html="<option value=\"\" selected=\"\">请选择</option>";

     for (var i in _via_attributes['region']["type"]["options"]){
       var row = "<option value=\""+i+
        "\">"+_via_attributes['region']["type"]["options"][i]+
         "</option>";
        html=html+row; 
     }
  console.log(html);
  document.getElementById('car_category').innerHTML=html; 


}

function road_id(){

    var html="<option value=\"\" selected=\"\">请选择</option>";

     for (var i in _via_attributes['region']["road_id"]["options"]){
       var row = "<option value=\""+i+
        "\">"+_via_attributes['region']["road_id"]["options"][i]+
         "</option>";
        html=html+row; 
     }
  console.log(html);
  document.getElementById('road_id').innerHTML=html; 


}

function get_color(){

    var html="<option value=\"\" selected=\"\">请选择</option>";

     for (var i in _via_attributes['region']["color"]["options"]){
       var row = "<option value=\""+i+
        "\">"+_via_attributes['region']["color"]["options"][i]+
         "</option>";
        html=html+row; 
     }
  console.log(html);
  document.getElementById('color').innerHTML=html; 


}


function submitVideoInfo(){

   var time = document.getElementById("time").value;
   if(isEmpty(time)){
	   alert("时间戳不能为空，请输入时间戳。");
	   return;
   }
   
  var traj_mode = $("#traj_mode option:selected").val();
  if(isEmpty(traj_mode)){
	   alert("轨迹线不能为空，请选择轨迹线。");
	   return;
  }
  
  var road_id = $('#road_id option:selected').val();
  if(traj_mode=="DU"){
	  if(isEmpty(road_id)){
		   alert("当轨迹线是DU时，请选择车道号。");
	       return;
	  }
  }
  
  var car_category = $('#car_category option:selected').val();
  if(isEmpty(car_category)){
	   alert("车辆类型不能为空，请选择车辆类型。");
	   return;
  }
  var color = $('#color option:selected').val();
  if(isEmpty(color)){
     alert("车辆颜色不能为空，请选择车辆颜色。");
     return;
  }
  // var content = $("#locInfor").html();
   var content = document.getElementById("locInfor").innerText;  
   if(isEmpty(content)){
       alert("坐标不能为空，请点击标注位置");
       return;
   }

  video_info_create();
  page(0, pageSize);
  display_video_info();
}


function getRealLocationX(num){
  var loc = Math.round(num*imgWidth/video.width);
  // if (img.width<600){
  //   loc = Math.round(num);
  // }
  if(loc <= 0){
        loc = 1;
    }
  if(loc > imgWidth){
    loc = imgWidth -1;
  }
  return loc;
}

function getRealLocationY(num){
  var loc = Math.round(num*imgHeight/video.height);
  // if (img.width<600){
  //   loc = Math.round(num);
  // }
  if(loc <= 0){
        loc = 1;
    }
  if( loc > imgHeight){
    loc = imgHeight -1;
  }
  return loc;
}

function getCanvasLocationX(num){
  return Math.round(num*video.width/parseInt(imgWidth));
}

function getCanvasLocationY(num){
  return Math.round(num*video.height/parseInt(imgHeight));
}



function video_info_create(task_name_id){
  
  var time = document.getElementById("time").value;
  var traj_mode = $("#traj_mode option:selected").val();
  var road_id = $('#road_id option:selected').val();
  var car_category = $('#car_category option:selected').val();
  var color = $('#color option:selected').val();
  var loc =  document.getElementById("locInfor").innerText;
  // var locX =tmpLoc.split(";")[0].split(":")[1];
  // var locY =tmpLoc.split(";")[1].split(":")[1];
   
   // var loc = "X:"+getRealLocationX(locX)+";Y:"+getRealLocationY(locY);


  var label_info = {"time":time, "traj_mode":traj_mode,"road_id":road_id, "car_category":car_category,"color":color,"loc":loc};
  var label_info_str = JSON.stringify(label_info);
  
    $.ajax({
       type:"POST",
       contentType:'application/json',
       url:ip + "/api/video-count-task-item",
       dataType:"json",
       async:false,
       headers: {
          authorization:token,
        },
       data:JSON.stringify({'label_info':label_info_str,
                            'label_task_id':label_task.id,//task id
           }),
       success:function(res){
        alert("视频流任务保存成功!");
        console.log(res);
       },
	    error:function(response) {
		  redirect(response);
        }
   });
   
}




var tableData;

function display_video_info(){

  var html="<tr>\
            <th></th>\
            <th id=\"predicttask_head\"></th>\
            <th>时间戳</th>\
            <th>轨迹形式</th>\
            <th>车道号</th>\
            <th>车辆类别</th>\
            <th>颜色</th>\
            <th>点坐标</th>\
            </tr>";
  for (var i=0;i<tableData.length;i++){
   
    var label_info = JSON.parse(tableData[i].label_info);
    // var locInfo = label_info.loc;
    // var x = getCanvasLocationX(locInfo.split(";")[0].split(':')[1]);
    // var y =  getCanvasLocationY(locInfo.split(";")[1].split(':')[1]);
    // var loc = "X:"+x+";Y:"+y;
    var row = "<tr onclick=\"clicklist("+i+");\">\
            <td><input type=\"checkbox\" class=\"flat-grey list-child\"/></td>\
            <td id=\"video_info_id\">"+tableData[i].id+"</td>\
            <td>"+label_info.time+"</td>\
            <td>"+label_info.traj_mode+"</td>\
            <td>"+label_info.road_id+"</td>\
            <td>"+_via_attributes['region']["type"]["options"][label_info.car_category]+"</td>\
            <td>"+ _via_attributes['region']["color"]["options"][label_info.color]+"</td>\
            <td>"+label_info.loc+ "</td>"
            "</tr>";
    html=html+row;
  }
  console.log(html);
  document.getElementById('video_info_list').innerHTML=html;


  $('#video_info_list tr').find('td:eq(1)').hide();
  $('#video_info_list tr').find('th:eq(1)').hide();

}

function clicklist(i){
  var label_info = JSON.parse(tableData[i].label_info);
  document.getElementById("time").value = label_info.time;
  var video =  document.getElementById("myVideo");//document.getElementsByTagName('video')[0];
  video.currentTime = strms2time(label_info.time);
  
  var tmpshow = document.getElementById("labelShow_0");
  if(!isEmpty(label_info.loc)){
    var xy = label_info.loc.split(";");
    tmpshow.style.display = "block";
    tmpshow.style.left = getCanvasLocationX(parseInt(xy[0].split(":")[1]))+5+"px";
    tmpshow.style.top = getCanvasLocationY(parseInt(xy[1].split(":")[1]))-20+"px";
  }
  document.getElementById("locInfor").innerHTML=label_info.loc;
  
}

function del() {
    if($("table[id='video_info_list'] input[type=checkbox]").is(":checked")) {
        if (confirm("确实要删除吗？")) {
            // alert("已经删除！");
            return false;
        } else {
            // alert("已经取消了删除操作");
            return true;
        }
    }else if($("table[id='video_info_list']").find("input").length=="0"){
        alert("暂无可删的数据！");
        return true;
    }else{
        alert("请先选择需要删除的选项！");
        return true;
    }
  }

function delete_video_info(){
  var stop = del();
  if (stop){
    return;
  }
  var Check = $("table[id='video_info_list'] input[type=checkbox]:checked");//在table中找input下类型为checkbox属性为选中状态的数据
        Check.each(function () {//遍历
              var row = $(this).parent("td").parent("tr");//获取选中行
              var id = row.find("[id='video_info_id']").html();//获取name='Sid'的值

              delete_video_info_id(id);
          });
  page(0,pageSize);
  // display_pre_predict_task_list();
}

function delete_video_info_id(video_info_id){
  console.log("id="+video_info_id);
  $.ajax({
    type:"DELETE",
    url:ip + "/api/video-count-task-item",
    headers: {
       authorization:token,
     },
    dataType:"json",
    async:false,
    data:{
      'id': video_info_id,       
    },
    success:function(json){
      console.log(json);
     // alert("视频流信息删除成功!");
    },
	error:function(response) {
		  redirect(response);
    }
 });
}



