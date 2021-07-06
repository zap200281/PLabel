var token = getCookie("token");
var ip = getIp();

var pageSize = 10;

var tableData;
var tablePageData;

var preDictTaskData;
var dataSetTaskData;

var userInfoData;


function setDataSetTask(){
	dataset_task_list();
	display_createdatasetlabel(0);
    getUser();
    dislpayUser();
}

function dataset_task_list(){
  $.ajax({
     type:"GET",
     url:ip + "/api/dataset/",
     headers: {
        authorization:token,
      },
     dataType:"json",
	 data:{'dateset_type':'[4]'},
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
  var html="<option value=\"\" selected=\"\">请选择</option>";
  for (var i=0;i<dataSetTaskData.length;i++){
    if (i==sindex){
        var row = "<option value=\""+dataSetTaskData[i].id+
              "\" selected=\"\">"+dataSetTaskData[i].task_name+
              "</option>";
        $("#datasetlabeltaskname").attr({value:dataSetTaskData[i].task_name + "-车流统计"});
    }else{
        var row = "<option value=\""+dataSetTaskData[i].id+
        "\">"+dataSetTaskData[i].task_name+
        "</option>";
      }
    html=html+row;
  }
  console.log(html);
  document.getElementById('dataset_list').innerHTML=html; 
}




function sele_Change(sele){
  var predictTaskName = $('#pre_predict_task_for_label option:selected').text();
  console.log("select predictTaskName =" + predictTaskName);
  $("#labeltaskname").attr({value:predictTaskName+"-车流统计"});
}

function dataset_sele_Change(sele){
  var dataset_listName = $('#dataset_list option:selected').text();
  console.log("select dataset_list =" + dataset_listName);
  $("#datasetlabeltaskname").attr({value:dataset_listName+"-车流统计"});
}



function submit_datasettask(){
  // console.log($('#labeltaskname').val());
  var task_name = $('#datasetlabeltaskname').val();
  if (isEmpty(task_name) || task_name.length > 32){
       alert("视频流标注任务名称不能为空或者不能超过32个字符。");
       return;
  }
  var dataset_id = $('#dataset_list option:selected').val();
  if (isEmpty(dataset_id)){
       alert("视频数据对象必须要填写。");
       return;
  }
  label_task_create(task_name, dataset_id);
  $("#labelDataModal").modal('hide');
  page(0,pageSize);
}


function label_task_create(task_name, dataset_id){
	
    var assign_user_id = $('#assign_user option:selected').val();
  	if(isEmpty(assign_user_id)){
  		assign_user_id = 0;
  	}

     $.ajax({
     type:"POST",
     contentType:'application/json',
     url:ip + "/api/video-count-task",
     dataType:"json",
     async:false,
     headers: {
        authorization:token, 
      },
     data:JSON.stringify({'task_name':task_name,
                          'assign_user_id':assign_user_id,
                          'dataset_id':dataset_id,
         }),
     success:function(res){
      alert("车流统计任务创建成功!");
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
       url:ip + "/api//video-count-task-page",
       headers: {
          authorization:token,
        },
       dataType:"json",
       data:{'startPage':current,
       'pageSize':pageSize},
       async:false,
       success:function(json){
        tablePageData = json;
        if(!isEmpty(tablePageData)){
          tableData = json.data;
        }
        
        console.log(json);
      },
	    error:function(response) {
		  redirect(response);
        }
   });
}


function dislpayUser(){
  var html="<option value=\"\" selected=\"\">请选择</option>";
  for (var i=0;i<userInfoData.length;i++){
        var row = "<option value=\""+userInfoData[i].id+
        "\">"+userInfoData[i].username+
        "</option>";
      
      html=html+row;
  }
  console.log(html);
  document.getElementById('assign_user').innerHTML=html; 
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


function delete_labeltask(){
  var stop = del();
  if (stop){
    return;
  }
  var Check = $("table[id='label_task_list'] input[type=checkbox]:checked");//在table中找input下类型为checkbox属性为选中状态的数据
        Check.each(function () {//遍历
              var row = $(this).parent("td").parent("tr");//获取选中行
              var id = row.find("[id='labeltask_id']").html();//获取name='Sid'的值
              delete_labeltask_byid(id);
          });
  page(0,pageSize);
}

function del(){
    if($("table[id='label_task_list'] input[type=checkbox]").is(":checked")) {
        if (confirm("确实要删除吗？")) {
            // alert("已经删除！");
            return false;
        } else {
            // alert("已经取消了删除操作");
            return true;
        }
    }else if($("table[id='label_task_list']").find("input").length=="0"){
        alert("暂无可删的数据！");
        return true;
    }else{
        alert("请先选择需要删除的选项！");
        return true;
    }
}

function delete_labeltask_byid(label_task_id){
  $.ajax({
    type:"DELETE",
    url:ip + "/api/video-count-task/",
    headers: {
       authorization:token,
     },
    dataType:"json",
    async:false,
    data:{'id': label_task_id},
    success:function(json){
      console.log(json);
   },
	    error:function(response) {
		  redirect(response);
        }
 });
}



function getTaskStatus(task_status){
	if(task_status == 1){
		return "标注中";
	}else{
		return "完成"
	}
}

function display_list(){

  var html="<tr>\
            <th></th>\
            <th id=\"labeltask_head\"></th>\
            <th>车流统计任务名称</th>\
            <th>任务归属者</th>\
            <th>任务开始时间</th>\
            <th>任务状态</th>\
            <th>操作</th>\
            </tr>";
 for (var i=0;i<tableData.length;i++){
    var videoInfo;
    if (!isEmpty(tableData[i].mainVideoInfo)){
        videoInfo = JSON.parse(tableData[i].mainVideoInfo);
    }
    else{
        videoInfo = {"resolutionRatio":""};
    }
    
    var row = "<tr>\
            <td><input type=\"checkbox\" class=\"flat-grey list-child\"/></td>\
            <td id=\"labeltask_id\">"+tableData[i].id+"</td>\
            <td>"+tableData[i].task_name+"</td>\
            <td>"+tableData[i].assign_user+"</td>\
            <td>"+tableData[i].task_add_time+"</td>\
            <td>" + getTaskStatus(tableData[i].task_status) + "</td>\
            <td>" + 
			"<a onclick=\"personLabel(\'" + tableData[i].id + "\' "+"," + "\'" + tableData[i].zip_object_name + "\' "+"," + "\'" + videoInfo.resolutionRatio + "\' )\" class=\"btn btn-xs btn-success\">开始"  + "车流统计</a>" + "&nbsp;&nbsp;&nbsp;<a onclick=\"setTaskId(\'"+tableData[i].id+"\');\" class=\"btn btn-xs btn-success\">导出标注数据</a>"  + "&nbsp;&nbsp;&nbsp;<a onclick=\"modify_taskstatus(\'"+tableData[i].id+"\');\" class=\"btn btn-xs btn-success\">修改</a>" 
			//"<a onclick=\"sessionStorage.setItem(\'label_task\',\'"+tableData[i].id+"\'); window.location.href=\'labeling.html\';\" class=\"btn btn-xs btn-success\">开始人工标注</a>" + "&nbsp;&nbsp;&nbsp;<a //onclick=\"downloadFile(\'"+tableData[i].id+"\');\" class=\"btn btn-xs btn-success\">导出标注数据</a>" 
			+
			"</td>\
            </tr>";

    html=html+row;
  }
  console.log(html);
  document.getElementById('label_task_list').innerHTML=html;

  $('#label_task_list tr').find('td:eq(1)').hide();
  $('#label_task_list tr').find('th:eq(1)').hide();
}



function personLabel(task_id, task_name,resolutionRatio){
	sessionStorage.setItem('label_task_id',task_id);
  sessionStorage.setItem('label_task_name',task_name);
  sessionStorage.setItem('resolutionRatio',resolutionRatio);

	window.location.href="labelingVideo.html";
}

function modify_taskstatus(task_id){
	$('#hidetaskid').val(task_id);
	$('#modifyTaskStatusModal').modal('show');
}

function submit_modify_taskstatus(){
	var task_id = $('#hidetaskid').val();
	var task_status = $('#task_status_value option:selected').val();
	$.ajax({
		 type:"PATCH",
		 url:ip + "/api/video-count-task-status",
		 dataType:"json",
		 async:false,
		 headers: {
			authorization:token, 
		  },
		 data:{'task_status':task_status,
			   'id':task_id
			 },
		 success:function(res){
		  //alert("视频标注任务状态更新成功!");
		  console.log(res);
		},
	    error:function(response) {
		  redirect(response);
        }
    });
	
    $('#modifyTaskStatusModal').modal('hide');
	
	var current = $('#displayPage1').text();
    console.log("开始刷新。current=" + current);
    if(current >= 1){
       page(current - 1,pageSize);
    }
}



function setTaskId(labeltaskid){
	$('#hide_labeltaskid').val(labeltaskid);
	document.getElementById("predtask_id").removeAttribute("disabled");
	$('#labeltaskexport').modal('show');
}


function downloadFile(){
	document.getElementById("predtask_id").setAttribute("disabled", true);
	var labeltaskid = $('#hide_labeltaskid').val();

    var taskreturnid = "";
	$.ajax({
	   type:"GET",
	   url:ip + "/api/video-label-file-download/",
	   headers: {
		  authorization:token,
		},
	   dataType:"json",
	   data:{
		   "label_task_id" : labeltaskid
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
		  progress = null;
		  console.log('query return null.');
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


function page(current,pageSize){
  list(current,pageSize);
  if(isEmpty(tablePageData)){
    return;
  }
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

page(0,pageSize);
