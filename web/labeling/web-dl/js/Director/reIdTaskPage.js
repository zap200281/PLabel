var token = getCookie("token");
var userName = getCookie("userName");
var ip = getIp();

var pageSize = 10;

var tableData;
var tablePageData;

var preDictTaskData;
var dataSetTaskData;

var userInfoData;
var autoReIDFlag=true;


var reidtasklist;
function multi_export(){
	reid_task_list();
	
	display_createlabelexportoption();
	document.getElementById("predtask_id").removeAttribute("disabled");
	$("#labeltaskexport").modal('show');
}

function reid_task_list(){
	 $.ajax({
       type:"GET",
       url:ip + "/api/reId-task-page-user/",
       headers: {
          authorization:token,
        },
       dataType:"json",
       async:false,
       success:function(json){
        reidtasklist = json;
        console.log(json);
      },
	  error:function(response) {
		redirect(response);
      }
   });
}



function downloadMultiFile(){

	var select = document.getElementById('multi_labeltaskid');
    var str = [];
    for(i=0;i<select.length;i++){
        if(select.options[i].selected){
            str.push(select[i].value);
        }
    }
	var isNeedPicture =  $('#isNeedPicture option:selected').val();
	if(str.length == 0 && str.length > 10){
        alert("最多选择10个任务导出，最少选择1个任务。");
		return;
	}
	console.log("str[0]=" + str[0] + "userName=" + userName);
	if(str.length == 1 && str[0] == "all"){
		//if(userName == "zouanping"){
			$.ajax({
	         type:"GET",
	         url:ip + "/api/multi-all-data-export/",
	         headers: {
		        authorization:token,
		     },
	         dataType:"json",
	         data:{
              "needPicture": isNeedPicture
		     },
	         async:false,
	         success:function(json){
		        taskreturnid = json.message;
		        console.log(json);
	         },
	        error:function(response) {
		       redirect(response);
             }
            });
			
			return;
		//}
	}
	
	document.getElementById("predtask_id").setAttribute("disabled", true);
	var labeltaskid = JSON.stringify(str);
	
    var taskreturnid = "";
	$.ajax({
	   type:"GET",
	   url:ip + "/api/multi-reid-data-download/",
	   headers: {
		  authorization:token,
		},
	   dataType:"json",
	   data:{
		   "reid_task_id_list" : labeltaskid,
           "needPicture": isNeedPicture,
		},
	   async:false,
	   success:function(json){
		 taskreturnid = json.message;
		 console.log(json);
	   },
	   error:function(response) {
		  redirect(response);
       }
   });
   console.log("taskreturnid=" +taskreturnid);
   if(!isEmpty(taskreturnid)){
	   setExportIntervalToDo(taskreturnid);
   }
}


var exportTimeId = [];
var exportCount;
var progress;

function setExportIntervalToDo(taskreturnid){
	exportCount=0;
	var tmpExportTimeId = self.setInterval("clockExport('" + taskreturnid +"')",1000);//5秒刷新
	exportTimeId.push(tmpExportTimeId);
	console.log("开始刷新。exportTimeId=" + tmpExportTimeId);
}

function clockExport(taskreturnid){
   exportCount++;
   if(exportCount > 600 ){
       for(var i = 0;i < exportTimeId.length; i++){
		    console.log("清除定时器1。exportTimeId=" + exportTimeId[i]);
		    window.clearInterval(exportTimeId[i]);
	   }
	   exportTimeId = [];
	   $("#autoLabel").modal('hide');
	   return;
   }
   $.ajax({
       type:"GET",
       url:ip + "/api/query-download-progress/",
       headers: {
          authorization:token,
        },
       dataType:"json",
	   data:{'taskId': taskreturnid},
       async:false,
       success:function(json){
         progress = json;
         console.log(json);
       },
	   error:function(response) {
		  redirect(response);
       }
   });
   if(!isEmpty(progress)){
     if(progress.progress >= 100){

	   var iSpeed = progress.progress;
	   bar.style.width=iSpeed+'%';
       document.getElementById('text-progress').innerHTML=iSpeed+'%' + ",开始下载文件。"
	   
	  for(var i = 0;i < exportTimeId.length; i++){
		    console.log("清除定时器2。exportTimeId=" + exportTimeId[i]);
		    window.clearInterval(exportTimeId[i]);
	   }
	   exportTimeId = [];
	   
	   var url = ip + "/api/label-file-download/";
       var $iframe = $('<iframe />');
       var $form = $('<form  method="get" target="_self"/>');
       $form.attr('action', url); //设置get的url地址

       $form.append('<input type="hidden"  name="taskId" value="' + taskreturnid + '" />');
    
       $iframe.append($form);
       $(document.body).append($iframe);
       $form[0].submit();//提交表单
       $iframe.remove();//移除框架
	   
	   $("#labeltaskexport").modal('hide');
	   bar.style.width='1%';
	   document.getElementById('text-progress').innerHTML="0%";
     }else{
	   //更新进度
	    var iSpeed = progress.progress;
	    bar.style.width=iSpeed+'%';
        document.getElementById('text-progress').innerHTML=iSpeed+'%'
     }
   }else{
	   //没有查到进度
	   exportCount = 600;
   }
}

function display_createlabelexportoption(sindex=-1){
  var html="";
  for (var i=0;i<reidtasklist.length;i++){
    if (i==sindex){
        var row = "<option value=\""+reidtasklist[i].id+
              "\" selected=\"true\">"+reidtasklist[i].task_name+
              "</option>";
    }else{
        var row = "<option value=\""+reidtasklist[i].id+
        "\" >"+reidtasklist[i].task_name+
        "</option>";
      }
    html=html+row;
  }
  var row = "<option value=\"all\" >全选</option>";
  html=html+row;
  console.log(html);
  document.getElementById('multi_labeltaskid').innerHTML=html;
  $('#multi_labeltaskid').val('');
  $(function() {
                $('#multi_labeltaskid').fSelect();
   });
}



function setDataSetTask(){
	dataset_task_list();
	display_createdatasetlabel(0);
    getUser();
    dislpayUser('dataset_assign_user');
	dataset_sele_Change("");
}

function dataset_task_list(){
  $.ajax({
     type:"GET",
     url:ip + "/api/dataset/",
     headers: {
        authorization:token,
      },
     dataType:"json",
	 data:{'dateset_type':'[1,2,3,4]'},
     async:false,
     success:function(json){
      dataSetTaskData = json;
      console.log(json);
      // return json.token;
     },
	 error:function(response) {
		redirect(response);
     }
 });
}

function display_createdatasetlabel(sindex=-1){
  var html="";
  for (var i=0;i<dataSetTaskData.length;i++){
    if (i==sindex){
        var row = "<option value=\""+dataSetTaskData[i].id+
              "\" selected=\"\">"+dataSetTaskData[i].task_name+
              "</option>";
    }else{
        var row = "<option value=\""+dataSetTaskData[i].id+
        "\">"+dataSetTaskData[i].task_name+
        "</option>";
      }
    html=html+row;
  }
  console.log(html);
  document.getElementById('src_dataset_taskid').innerHTML=html; 
  document.getElementById('dest_dataset_taskid').innerHTML=html;
  $(function() {
                $('#dest_dataset_taskid').fSelect();
   });
}


function setReIDAutoTask(){
    pre_predict_task_list();
    display_createlabel(0);
	getUser();
    dislpayUser('auto_assign_user');
	sele_Change("");
}

function pre_predict_task_list(){
  $.ajax({
     type:"GET",
     url:ip + "/api/pre-predict-taskforLabel/",
     headers: {
        authorization:token,
      },
     dataType:"json",
     async:false,
     success:function(json){
      preDictTaskData = json;
      console.log(json);
      // return json.token;
     },
	 error:function(response) {
		redirect(response);
     }
 });
}

function sele_Change(sele){
  var predictTaskName = $('#src_auto_taskid option:selected').text();
  console.log("select predictTaskName =" + predictTaskName);
  $("#reid_auto_taskname").attr({value:predictTaskName+"-自动标注结果ReID标注"});
}

function dataset_sele_Change(sele){
  var dataset_listName = $('#src_dataset_taskid option:selected').text();
  console.log("select dataset_list =" + dataset_listName);
  $("#reid_dataset_taskname").attr({value:dataset_listName+"-数据集ReID标注"});
}



function display_createlabel(sindex=-1){
  var html="";
  for (var i=0;i<preDictTaskData.length;i++){
    if (i==sindex){
        var row = "<option value=\""+preDictTaskData[i].id+
              "\" selected=\"\">"+preDictTaskData[i].task_name+
              "</option>";
    }else{
        var row = "<option value=\""+preDictTaskData[i].id+
        "\">"+preDictTaskData[i].task_name+
        "</option>";
      }
    html=html+row;
  }
  document.getElementById('src_auto_taskid').innerHTML=html; 
  document.getElementById('dest_auto_taskid').innerHTML=html; 
   $(function() {
                $('#dest_auto_taskid').fSelect();
   });
}


function submit_dataset_reidtask(){
  // console.log($('#labeltaskname').val());
  var task_name = $('#reid_dataset_taskname').val();
  if(isEmpty(task_name)){
    alert("ReID任务名称不能为空。");
    return;
  }
  if (isEmpty(task_name) || task_name.length > 32){
       alert("ReID任务名称不能为空或者不能超过32个字符。");
       return;
  }
  var src_taskid = $('#src_dataset_taskid option:selected').val();
  var dest_taskid = getmultiplyvalue('dest_dataset_taskid');
  if(isEmpty(src_taskid) ){
	  alert("请选择源数据集对象。");
	  return;
  }
  if(isEmpty(dest_taskid)){
	  alert("请选择对照数据集对象。");
	  return;
  }
  
  //$('#dest_dataset_taskid option:selected').val();
  var assign_user_id =  $('#dataset_assign_user option:selected').val();
  var reid_dataset_obj_type = $('#reid_dataset_obj_type option:selected').val();
  if(isEmpty(reid_dataset_obj_type)){
	  alert("请选择标注对象。");
	  return;
  }
  
  
  $("#reIdDataModal").modal('hide');
  reid_task_create(task_name, src_taskid, dest_taskid,assign_user_id, 2,reid_dataset_obj_type,0);
  page(0,pageSize);
}

function submit_auto_reidtask(){
  // console.log($('#labeltaskname').val());
  var task_name = $('#reid_auto_taskname').val();
  if (isEmpty(task_name)){
    alert("ReID任务名称不能为空。");
    return;
  }
  if (isEmpty(task_name) || task_name.length > 32){
       alert("ReID任务名称不能为空或者不能超过32个字符。")
       return;
  }
  var src_taskid = $('#src_auto_taskid option:selected').val();
  //var dest_taskid = $('#dest_auto_taskid option:selected').val();
  var dest_taskid = getmultiplyvalue('dest_auto_taskid');
  if(isEmpty(src_taskid) ){
	  alert("请选择源自动标注结果。");
	  return;
  }
  if(isEmpty(dest_taskid)){
	  alert("请选择对照自动标注结果。");
	  return;
  }
  var assign_user_id =  $('#auto_assign_user option:selected').val();
  var reid_obj_type =  $('#reid_obj_type option:selected').val();
  if(isEmpty(reid_obj_type)){
	  alert("请选择标注对象。");
	  return;
  }
  
  var reid_auto_type =  $('#reid_auto_type option:selected').val();
  if(isEmpty(reid_auto_type)){
	  alert("是否自动进行ReID标注选项不能为空。");
	  return;
  }
  
  $("#reIdAutoDataModal").modal('hide');
  reid_task_create(task_name, src_taskid, dest_taskid,assign_user_id, 1,reid_obj_type,reid_auto_type);
  page(0,pageSize);

}

function getmultiplyvalue(comid){
    var select = document.getElementById(comid);
    var str = [];
    for(i=0;i<select.length;i++){
        if(select.options[i].selected){
            str.push(select[i].value);
        }
    }
	if(str.length > 0){
        return JSON.stringify(str);
	}else{
		return "";
	}
}


function reid_task_create(task_name, src_taskid, dest_taskid,assign_user_id,  taskType, reid_obj_type,reid_auto_type=0){
	
   
	console.log("task_name=" + task_name);
	console.log("src_taskid=" + src_taskid);
	console.log("dest_taskid=" + dest_taskid);
	console.log("assign_user_id=" + assign_user_id);
	console.log("taskType=" + taskType);
	console.log("reid_auto_type=" + reid_auto_type);
	
	
    $.ajax({
       type:"POST",
       contentType:'application/json',
       url:ip + "/api/reId-task/",
       dataType:"json",
       async:false,
       headers: {
          authorization:token,
        },
       data:JSON.stringify({'task_name':task_name,
                            'assign_user_id':assign_user_id,
                            'src_predict_taskid':src_taskid,
                            'dest_predict_taskid':dest_taskid,
                            "task_type": taskType,
							"reid_obj_type": reid_obj_type,
							"reid_auto_type":reid_auto_type
							
           }),
       success:function(res){
        alert("ReID标注任务创建成功!");
        console.log(res);
      },
	  error:function(response) {
		redirect(response);
      }
   });
}

function list(current,pageSize){
      $.ajax({
       type:"GET",
       url:ip + "/api/reId-task-page/",
       headers: {
          authorization:token,
        },
       dataType:"json",
       data:{'startPage':current,
             'pageSize':pageSize},
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



function dislpayUser(assign_user){
  var html="";
  for (var i=0;i<userInfoData.length;i++){
      var row = "<option value=\""+userInfoData[i].id+
        "\">"+userInfoData[i].username+
        "</option>";
      if(userName == userInfoData[i].username){
		  row = "<option value=\""+userInfoData[i].id+
        "\" selected=\"\">"+userInfoData[i].username+
        "</option>";
	  }
	  
      html=html+row;
  }
  console.log(html);
  document.getElementById(assign_user).innerHTML=html; 
}


function getUser(){
    $.ajax({
       type:"GET",
       url:ip + "/api/queryAllUser/",
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


function delete_reidtask(){
  var stop = del();
  if (stop){
    return;
  }
  var Check = $("table[id='reid_task_list'] input[type=checkbox]:checked");//在table中找input下类型为checkbox属性为选中状态的数据
        Check.each(function () {//遍历
              var row = $(this).parent("td").parent("tr");//获取选中行
              var id = row.find("[id='reidtask_id']").html();//获取name='Sid'的值
              delete_reidtask_byid(id);
          });
  page(0,pageSize);
}

function del(){
    if($("table[id='reid_task_list'] input[type=checkbox]").is(":checked")) {
        if (confirm("确实要删除吗？")) {
            // alert("已经删除！");
            return false;
        } else {
            // alert("已经取消了删除操作");
            return true;
        }
    }else if($("table[id='reid_task_list']").find("input").length=="0"){
        alert("暂无可删的数据！");
        return true;
    }else{
        alert("请先选择需要删除的选项！");
        return true;
    }
}
function delete_reidtask_byid(reid_task_id){
  $.ajax({
    type:"DELETE",
    url:ip + "/api/reId-task/",
    headers: {
       authorization:token,
     },
    dataType:"json",
    async:false,
    data:{'reid_task_id': reid_task_id},
    success:function(json){
      console.log(json);
    },
	error:function(response) {
		redirect(response);
    }
 });
}

function getTaskTypeDesc(task_type){
	if(task_type == 1){
		return "自动标注结果";
	}else if(task_type == 2){
		return "原始数据集-图片";
	}else if(task_type == 3){
		return "原始数据集-CT影像";
	}else if(task_type == 4){
		return "原始数据集-视频";
	}
	return "其它";
}

function getLabelDesc(task_flow_type){
	
	return "ReID"
	
}

function getRelateTaskName(relate_task_name){
	if(!isEmpty(relate_task_name)){
	   return relate_task_name;
	}else{
	   return "摄像头1视频";
	}
}


function display_list(){

  var html="<tr>\
            <th></th>\
            <th id=\"labeltask_head\"></th>\
            <th>标注任务名称</th>\
            <th>关联的源任务名称</th>\
            <th>数据类型</th>\
			<th>标注对象</th>\
            <th>任务归属者</th>\
            <th>任务开始时间</th>\
            <th>任务状态</th>\
            <th>操作</th>\
            </tr>";
 for (var i=0;i<tableData.length;i++){
    var row = "<tr>\
            <td><input type=\"checkbox\" class=\"flat-grey list-child\"/></td>\
            <td id=\"reidtask_id\">"+tableData[i].id+"</td>\
            <td>"+tableData[i].task_name+"</td>\
            <td>"+ getRelateTaskName(tableData[i].relate_task_name) +"</td>\
            <td>"+ getTaskTypeDesc(tableData[i].task_type) +"</td>\
			<td>"+ getreid_obj_type(tableData[i].reid_obj_type) +"</td>\
            <td>"+tableData[i].assign_user+"</td>\
            <td>"+tableData[i].task_start_time+"</td>\
            <td>"+ getStatus(tableData[i].task_status,tableData[i].costTime,tableData[i].task_status_desc)+"</td>\
            <td>" + 
            getHtml(tableData[i].task_status,tableData[i].id, tableData[i].task_type, tableData[i].task_flow_type)
			+
			"</td>\
            </tr>";

    html=html+row;
  }
  console.log(html);
  document.getElementById('reid_task_list').innerHTML=html;

  $('#reid_task_list tr').find('td:eq(1)').hide();
  $('#reid_task_list tr').find('th:eq(1)').hide();
  
  setIntervalToDo();
}
var timeId;
function setIntervalToDo(){
	var isNeedToSetInterval = false;
	if(!isEmpty(tableData)){
        for (var i=0;i<tableData.length;i++){
		    if(tableData[i].task_status == 1){
				console.log("有自动分类任务在进行中。需要自动刷新。");
				isNeedToSetInterval = true;//有任务在进行中才刷新，否则不刷新。
			}
	    }
	}
	if(!isEmpty(timeId)){
		console.log("清除定时器。timeId=" + timeId);
		window.clearInterval(timeId);
		timeId = null;
	}
	if(isNeedToSetInterval){
		timeId = self.setInterval("clock()",5000);//5秒刷新
		console.log("开始刷新。timeId=" + timeId);
	}
}

function clock(){
   
   var current = $('#displayPage1').text();
   console.log("开始刷新。current=" + current);
   if(current >= 1){
       page(current - 1,pageSize);
   }
}

function getreid_obj_type(reid_obj_type){
	if(reid_obj_type == 0){
		return "标注人";
	}else if(reid_obj_type == 1){
		return "标注车";
	}
}


function getStatus(task_status,costTime,task_status_desc){
	if(task_status == 1){
		var re = "自动ReID分类中";
		if(!isEmpty(task_status_desc)){
			var regPos = /^\d+$/;
			if(regPos.test(task_status_desc)){
				var ratio = costTime / task_status_desc;
				if(ratio > 1){
					ratio = 1;
				}
				re += "进度：" + Math.round(ratio * 100) + "%";
			}
		}
		
		return re;
	}else{
		return "创建成功";
	}
}


function getHtml(task_status, task_id, task_type, task_flow_type){
  if(task_status == 1){
      return "<a onclick=\"\" class=\"btn btn-xs btn-fail\">开始" + getLabelDesc(task_flow_type) + "标注</a>" + "&nbsp;&nbsp;&nbsp;<a onclick=\"\" class=\"btn btn-xs btn-fail\">查看ReID结果</a>" ;
  }else{
	  return "<a onclick=\"personLabel(\'" + task_id + "\'," + task_type + ")\" class=\"btn btn-xs btn-success\">开始" + getLabelDesc(task_flow_type) + "标注</a>" + "&nbsp;&nbsp;&nbsp;<a onclick=\"sessionStorage.setItem(\'reid_task_id\',\'"+task_id +"\'); window.location.href=\'reIDShowResult.html\';\" class=\"btn btn-xs btn-success\">查看ReID结果</a>" ;
  }
}

function personLabel(taskid, task_type){

	sessionStorage.setItem('reid_task',taskid);
    sessionStorage.setItem('task_type',task_type);
    window.location.href="labelingReID.html";
}


function page(current,pageSize){
  list(current,pageSize);
  display_list();
  setPage(tablePageData,pageSize);
  sessionStorage.setItem('reid_task_page',current);
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
var tmpCurrent = sessionStorage.getItem("reid_task_page");
if(isEmpty(tmpCurrent)){
	tmpCurrent = 0;
}
page(tmpCurrent,pageSize);
