var token = getCookie("token");
var ip =  getIp();


var tableData;
var tablePageData;
var pageSize = 10;

function report_list(startPage,pageSize,user_id, lastDay, startTime, endTime){
    $.ajax({
       type:"POST",
	   contentType:'application/json',
       url:ip + "/api/report/queryReportPage/",
       headers: {
          authorization:token,
        },
       dataType:"json",
	   data:JSON.stringify(
            {'startPage':startPage,
             'pageSize':pageSize,
              'user_id':user_id,
              'lastDay':lastDay,
              'startTime':startTime,
              'endTime':endTime,
           }),
       async:false,
       success:function(json){
          tablePageData = json;
          tableData = json.data;
          console.log(json);
       },
	   error:function(response) {
		  redirect(response);
       }
   });
}


function display_list(){

  var html="<tr>\
            <th>用户名称</th>\
			<th>标注图片数量</th>\
            <th>新建标注数量</th>\
            <th>修改标注数量</th>\
            <th>设置属性数量</th>\
			<th>不合格标注数量</th>\
            <th>统计日期</th>\
            </tr>";
   if (!isEmpty(tableData)){  
	  for (var i=0;i<tableData.length;i++){
		var row = "<tr>\
				<td>"+tableData[i].user_name+"</td>\
				<td>"+tableData[i].pictureUpdate+"</td>\
				<td>"+tableData[i].rectAdd+"</td>\
				<td>"+tableData[i].rectUpdate+"</td>\
				<td>"+tableData[i].properties+"</td>\
				<td>"+tableData[i].notValide+"</td>\
				<td>"+tableData[i].oper_time+"</td>\
				</tr>";
		html=html+row;
	  }
  } 
  console.log(html);
  document.getElementById('reportDataTable').innerHTML=html;
}

function page(current,pageSize){
  report_list(current,pageSize,selectUser, lastDay, startTime, endTime);
  display_list();
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
  $('#startIndex').text(startIndex);
  $('#endIndex').text(pageData.current * pageSize + pageData.data.length);
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






var userInfoData;

function getUser(){
  $.ajax({
     type:"GET",
     url:ip + "/api/queryAllUserBySuperUser/",
     headers: {
        authorization:token,
      },
     dataType:"json",
     async:false,
     success:function(json){
        userInfoData = json;
        console.log(json);
     },
	 error:function(response) {
		redirect(response);
     }
  });
}

function display_username(sindex=-1){
  var html="";
  if (isEmpty(userInfoData)){
    return;
  }
  for (var i=0;i<userInfoData.length;i++){
    if (i==sindex){
        var row = "<option value=\""+userInfoData[i].id+
              "\" selected=\"\">"+userInfoData[i].username+
              "</option>";
    }else{
        var row = "<option value=\""+userInfoData[i].id+
        "\"  selected=\"\">"+userInfoData[i].username+
        "</option>";
      }
    html=html+row;
  }
  document.getElementById('user_name').innerHTML=html; 
   $(function() {
                $('#user_name').fSelect();
   });
}



function display_usernameNext(sindex=-1){
  var html="";
  if (isEmpty(userInfoData)){
    return;
  }
  for (var i=0;i<userInfoData.length;i++){
    if (i==sindex){
        var row = "<option value=\""+userInfoData[i].id+
              "\" selected=\"\">"+userInfoData[i].username+
              "</option>";
    }else{
        var row = "<option value=\""+userInfoData[i].id+
        "\">"+userInfoData[i].username+
        "</option>";
      }
    html=html+row;
  }
  document.getElementById('user_name_next').innerHTML=html; 
   $(function() {
                $('#user_name_next').fSelect();
   });
}


function showHideDefTime(value){
  if (value == '4'){
      document.getElementById("selfDefTime").style.display = "inline-block";
  }
  else{
    document.getElementById("selfDefTime").style.display = "none";
  }
}


var minTime;
var maxTime = '';
var limitMon = 3;
var selectUser;
var lastDay;
var startTime;
var endTime;



function getmultiplyvalue(comid){
    var select = document.getElementById(comid);
    var str = [];
    for(i=0;i<select.length;i++){
        if(select.options[i].selected){
            str.push(select[i].value);
        }
    }
    console.log("str:" + str);
    return str;
}


function getTime(){
  var val = document.getElementById('selectTime').value;
  switch (val){
    case '1':
          lastDay = '1';
          startTime = '';
          endTime ='';
          break;
    case '2':
          lastDay = '7';
          startTime = '';
          endTime ='';
          break;
    case '3':
          lastDay = '30';
          startTime = '';
          endTime ='';
          break;
    case '4': 
         lastDay = '';
         startTime = document.getElementById('startTime').value;
         endTime =  document.getElementById('endTime').value;

  } 

}

function selectData(){

    selectUser = getmultiplyvalue('user_name');
    if (isEmpty(selectUser)){
      alert("请选择用户");
    }
    getTime();
    page(0,pageSize);
}

getUser();
display_username();
getTime();
selectUser = getmultiplyvalue('user_name');
// limitTime();
page(0,pageSize);


$(function(){
  $(".group span").mousemove(function(){
    var thisIndex = $(this).index();
    $(this).addClass("active").siblings().removeClass("active");
     $(".pubPart").hide().eq(thisIndex).show();
     getUser();
     display_username();
     // display_createlabel(0);
  });
});


display_usernameNext();

function getMeasType(){
  var val = document.getElementById('selectType').value;
  document.getElementById("measNum").value='';
  switch (val){
    case '1':
          measureType = '0';  
          document.getElementById("measNum").value = '30';       
          // document.getElementById("measNum").setAttribute("placeholder","最大值为30");
          break;
    case '2':
          measureType = '1';
          document.getElementById("measNum").value = '24';
          // document.getElementById("measNum").setAttribute("placeholder","最大值为24");
          break;
    case '3':
          measureType = '2';
          document.getElementById("measNum").value = '12';
          // document.getElementById("measNum").setAttribute("placeholder","最大值为12");
          break;
  } 

}


var measureType = '0';
var measureValue;
var user_id_next;
var typeData;
var comColor;
function selectDataNext(){
    var err = 0;
    user_id_next = getmultiplyvalue('user_name_next');
    measureValue = document.getElementById("measNum").value;
    err = isOutRange(measureType,user_id_next);
    if (err == -1){
      return err;
    }
    getTypeData();
    displayData();    
}

function isOutRange(measureType,user_id_next){
    var value = document.getElementById("measNum").value;
    if (isEmpty(value)){
          alert("请输入数量");
          return -1;
    }
    if (value<=0){
          alert("请输入大于0的数量值");
          return -1;
    }

    var selectTypeValue = document.getElementById("selectType").value;
    if (isEmpty(selectTypeValue)){
          alert("请选择度量方式");
          return -1;
    }

    if (user_id_next.length == 0){
          alert("请选择用户");
          return -1;
    }

    switch (measureType){
    case '0':
          if(value > 30){
            alert("请输入小于等于30的数字");
            return -1;
          }
          break;
    case '1':
          if(value > 24){
             alert("请输入小于等于24的数字");
             return -1;
          }
          break;
    case '2':
          if(value > 12){
             alert("请输入小于等于12的数字")
             return -1;
          }
          break;
  } 

}



function getTypeData(){

    $.ajax({
       type:"POST",
     contentType:'application/json',
       url:ip + "/api/report/queryReportMeasure",
       headers: {
          authorization:token,
        },
       dataType:"json",
       data:JSON.stringify(
            {'measureType':measureType,
             'measureValue':measureValue,
              'user_id':user_id_next,
           }),
       async:false,
       success:function(json){
        typeData = json;
        if(isEmpty(typeData)){
          alert("未检索到用户数据")
        }
        console.log(json);
       },
       error:function(response) {
          document.getElementById("message").style.display = "block";
          document.getElementById("messageInfo").style.display = "none";
          alert('获取数据失败');
		  redirect(response);
       }
	    
   });

}



function displayData(){
    //消除已存在的
  $("#displayDataRect").removeAttr("_echarts_instance_").empty();
  $("#displayDataNotValideRect").removeAttr("_echarts_instance_").empty();
  $("#displayDataPicture").removeAttr("_echarts_instance_").empty();
  $("#displayDataProperties").removeAttr("_echarts_instance_").empty();
  

    var typeStr;
    switch(measureType){
      case '0':
             typeStr = '天';
             break;
      case '1':
             typeStr = '周';
             break;
      case '2':
             typeStr = '月';
             break; 

    }

    var option;
    var xdata = [];      
    var allUserDataRect=[];
	var allUserDataNotValideRect=[];
    var allUserDataPicture=[];
    var allUserDataProperties=[];
    //获取数据
    for (var i=0;i<typeData[0].dataList.length;i++){
      xdata.push(typeData[0].dataList[i].index);
    }
    for (var i = 0; i<typeData.length;i++){
      var oneUserDataRect=[];
	  var oneUserDataNotValideRect=[];	 
      var oneUserDataPicture=[];
      var oneUserDataProperties=[];  
       
      for (var j=0; j<typeData[i].dataList.length;j++){
          oneUserDataRect.push(typeData[i].dataList[j].rectNum); 
		  oneUserDataNotValideRect.push(typeData[i].dataList[j].notValideNum); 
          oneUserDataPicture.push(typeData[i].dataList[j].pictureNum);
          oneUserDataProperties.push(typeData[i].dataList[j].propertiesNum);
      }
      allUserDataRect.push(oneUserDataRect);
	  allUserDataNotValideRect.push(oneUserDataNotValideRect);
      allUserDataPicture.push(oneUserDataPicture);
      allUserDataProperties.push(oneUserDataProperties);
    }
    console.log("allUserDataRect" +allUserDataRect);
    console.log("allUserDataPicture" +allUserDataPicture);
    // 加载图表rect 
    var dataChart = echarts.init(document.getElementById('displayDataRect')); 
    option = getOption(typeStr,typeData,xdata,allUserDataRect,'标注框的数量');   
    dataChart.setOption(option);  
	
	dataChart = echarts.init(document.getElementById('displayDataNotValideRect')); 
    option = getOption(typeStr,typeData,xdata,allUserDataNotValideRect,'不合格的标注数量');   
    dataChart.setOption(option); // 加载图表rect   

    // 加载图表Picture 
    dataChart = echarts.init(document.getElementById('displayDataPicture')); 
    option = getOption(typeStr,typeData,xdata,allUserDataPicture,'标注图片的数量');   
    dataChart.setOption(option); // 加载图表rect   

    // 加载图表Properties 
    dataChart = echarts.init(document.getElementById('displayDataProperties')); 
    option = getOption(typeStr,typeData,xdata,allUserDataProperties,'标注属性的数量');   
    dataChart.setOption(option); // 加载图表rect   
}

function getOption(typeStr,typeData,xdata,ydata,name){

   var option = {
              title : {
                  text: '',


                  textStyle: {
                        fontSize: 12,
                    },
                  left:'center',

                  subtext: '',

              },
              tooltip : {
                  trigger: 'axis'
              },
               legend: {
              orient: 'vertical',
              top:'top',　　
               },
              toolbox: {
                  show : false,
                  feature : {
                      mark : {show: true},
                      dataView : {show: false, readOnly: false},
                      magicType : {show: true, type: ['line', 'bar']},
                      restore : {show: false},
                      saveAsImage : {show: true}
                  }
              },
              calculable : true,
              xAxis : [
                  {
                      name: typeStr,
                      type : 'category',
                      boundaryGap : true,
                      data : xdata,
                      axisTick: {
                                 alignWithLabel: true
                               },
                  }
              ],
              yAxis : [
                  {
                      name:name,
                      type : 'value',
                      axisLine : {onZero: true},
                  }
              ],
              series : [
              ]
      }; 


         // 分用户显示
     var allUserSeries=[];
     for (var i = 0; i<typeData.length;i++){ 
      var color = randomColor();
      var userSer = {
            name:typeData[i].user_name +name,
            type:'bar',
            barMaxWidth:10,
            data:ydata[i],
                  lineStyle: {
                      normal: {
                          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
                              offset: 0,
                              // color: 'rgba(16, 79, 193,1)'
                              color:color
                          }, {
                              offset: 1,
                              color: color //'rgba(125, 178, 244,1)'
                          }], false)
                      }
                  },
                  itemStyle: {
                      normal: {
                          color: color,
                      }
                      },
                  markPoint : {
                      data : [
                          {type : 'max', name: '最大值'},
                          {type : 'min', name: '最小值'}
                      ]
                  },

              } 
      allUserSeries.push(userSer); 
      
     } 
      option.series= allUserSeries; 

    return option;

}

function randomColor(){
    var r=parseInt(Math.random()*256);
    var g=parseInt(Math.random()*256);
    var b=parseInt(Math.random()*256);
    var rgba="rgb("+r+","+g+","+b+",1)";
    return rgba;
}