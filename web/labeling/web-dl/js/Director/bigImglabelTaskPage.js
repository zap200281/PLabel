var token = getCookie("token");
var userType = getCookie("userType");
if(isEmpty(userType)){
	userType =1;
}
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
        $("#datasetlabeltaskname").attr({value:dataSetTaskData[i].task_name + "-人工标注"});
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


function setPredictTask(){
    pre_predict_task_list();
    display_createlabel(0);
	getUser();
    dislpayUser();
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
  var predictTaskName = $('#pre_predict_task_for_label option:selected').text();
  console.log("select predictTaskName =" + predictTaskName);
  $("#labeltaskname").attr({value:predictTaskName+"-人工标注"});
}

function dataset_sele_Change(sele){
  var dataset_listName = $('#dataset_list option:selected').text();
  console.log("select dataset_list =" + dataset_listName);
  $("#datasetlabeltaskname").attr({value:dataset_listName+"-人工标注"});
}



function display_createlabel(sindex=-1){
  var html="";
  for (var i=0;i<preDictTaskData.length;i++){
    if (i==sindex){
        var row = "<option value=\""+preDictTaskData[i].id+
              "\" selected=\"\">"+preDictTaskData[i].task_name+
              "</option>";
        $("#labeltaskname").attr({value:preDictTaskData[i].task_name + "-人工标注"});
    }else{
        var row = "<option value=\""+preDictTaskData[i].id+
        "\">"+preDictTaskData[i].task_name+
        "</option>";
      }
    html=html+row;
  }
  console.log(html);
  document.getElementById('pre_predict_task_for_label').innerHTML=html; 
}

var createsucced;





function submit_datasettask(){
  console.log($('#datasetlabeltaskname').val());
  var task_name = $('#datasetlabeltaskname').val();
  if (isEmpty(task_name) || task_name.length > 32){
       alert("人工标注任务名称不能为空或者不能超过32个字符。");
       return;
   }
  var assign_user_id = $('#assign_user option:selected').val();
  if(isEmpty(assign_user_id)){
		assign_user_id = 0;
  }
  var dataset_id = $('#dataset_list option:selected').val();
  if(isEmpty(dataset_id)){
	   alert("数据集对象不能为空。");
       return;
  }
  createsucced = true;
  label_task_create(task_name, dataset_id);
  if(createsucced){
	 $("#labelDataModal").modal('hide');
  }
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
     url:ip + "/api/large-picture-task",
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
      alert("视频标注任务创建成功!");
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
       url:ip + "/api/large-picture-task-page",
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


var otherUserLabelTaskData;

function flow_type_sele_Change(sele){
	
	var task_flow_type = $('#task_flow_type option:selected').val();
	if(task_flow_type == 2){
		  var datasetid = $('#dataset_list option:selected').val();
          getOtherUserLabelTaskByDataSetId(datasetid);
		  var html = "<p>请选择该数据集要审核的标注任务</p>";
		  for(var i = 0; i < otherUserLabelTaskData.length; i++){
			  html += "<p><input type=\"checkbox\" name=\"category\" value=\"" + otherUserLabelTaskData[i].id + "\"/>" + otherUserLabelTaskData[i].task_name + "(标注人：" + otherUserLabelTaskData[i].assign_user + ")"  + "</p>";
		  }
		  document.getElementById('related_task_list').innerHTML=html; 
	}else{
		document.getElementById('related_task_list').innerHTML= ""; 
	}
}

function getOtherUserLabelTaskByDataSetId(datasetid){
    $.ajax({
       type:"GET",
       url:ip + "/api/label-related-task/" + datasetid + "/",
       headers: {
          authorization:token,
        },
       dataType:"json",
       async:false,
       success:function(json){
        otherUserLabelTaskData = json;
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
  document.getElementById('label_assign_user').innerHTML=html;
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
    url:ip + "/api/large-picture-task",
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
function getTaskTypeDesc(task_type){
	return "超大图像";
}

function getLabelDesc(task_flow_type){
	if(task_flow_type == 2){
		return "审核";
	}else{
		return "人工"
	}
}

// function getTaskSataus(task_status,task_status_desc){
// 	if(task_status == 0){
// 		return "标注中：" + task_status_desc;
// 	}else if(task_status == 1){
// 		return "审核中：" + task_status_desc;
// 	}
// }

function getTaskStatus(task_status){
  if(task_status == 1){
    return "标注中";
  }else if(task_status == 0){
    return "完成";
  }else{
  return "未知";
  }
}


function getLabel(id,dataset_id,zip_object_name,task_name){
    return "<a onclick=\"personLabel(\'" + id + "\',\'" + dataset_id + "\',\'" + zip_object_name + "\',\'" + task_name + "\')\" class=\"btn btn-xs btn-success\">开始超大图像标注</a>&nbsp;&nbsp;&nbsp;"; 
}


function display_list(){

  var html="<tr>\
            <th></th>\
            <th id=\"labeltask_head\"></th>\
            <th>标注任务名称</th>\
            <th>数据类型</th>\
            <th>标注人员</th>\
            <th>任务开始时间</th>\
            <th>任务状态</th>\
            <th>操作</th>\
            </tr>";
 for (var i=0;i<tableData.length;i++){
    var row = "<tr>\
            <td><input type=\"checkbox\" class=\"flat-grey list-child\"/></td>\
            <td id=\"labeltask_id\">"+tableData[i].id+"</td>\
            <td>"+tableData[i].task_name+"</td>\
            <td>"+ getTaskTypeDesc(tableData[i].task_type) +"</td>\
            <td>"+tableData[i].assign_user+"</td>\
            <td>"+tableData[i].task_add_time+"</td>\
            <td>"+getTaskStatus(tableData[i].task_status)+"</td>\
            <td>" + 
			getLabel(tableData[i].id,tableData[i].dataset_id,tableData[i].zip_object_name,tableData[i].task_name) + "<a onclick=\"setTaskId(\'"+tableData[i].id+"\');\" class=\"btn btn-xs btn-success\">导出标注数据</a>" 
			+ 
			"</td>\
            </tr>";

    html=html+row;
  }
  //console.log(html);
  document.getElementById('label_task_list').innerHTML=html;

  $('#label_task_list tr').find('td:eq(1)').hide();
  $('#label_task_list tr').find('th:eq(1)').hide();
}


function personLabel(taskid,dataset_id,zip_object_name,task_name){
    sessionStorage.setItem('label_task_id',taskid);
    sessionStorage.setItem('label_task_name',task_name);
    sessionStorage.setItem('zip_object_name',zip_object_name);
    sessionStorage.setItem('dataset_id',dataset_id);
	  window.location.href="labelingBigImg.html";
}

function setMultiTaskId(){
	var Check = $("table[id='label_task_list'] input[type=checkbox]:checked");//在table中找input下类型为checkbox属性为选中状态的数据
	if(Check.length == 0){
		 alert("请选择一个或者多个标注数据进行导出。");
		 return;
	}
	var taskList = [];
    Check.each(function () {//遍历
        var row = $(this).parent("td").parent("tr");//获取选中行
        var id = row.find("[id='labeltask_id']").html();//获取name='Sid'的值
        taskList.push(id);
        //$('#hide_labeltaskid').val(id);
    });
    
    setTaskId(JSON.stringify(taskList));
}



function setTaskId(labeltaskid){
	$('#hide_labeltaskid').val(labeltaskid);
	bar.style.width='1%';
	document.getElementById('text-progress').innerHTML="0%";
	document.getElementById("predtask_id").removeAttribute("disabled");
	
	$('#labeltaskexport').modal('show');
}

function downloadFile(){
	document.getElementById("predtask_id").setAttribute("disabled", true);
	var labeltaskid = $('#hide_labeltaskid').val();
    var isNeedPicture =  $('#isNeedPicture option:selected').val();

	var taskreturnid = "";
	$.ajax({
       type:"GET",
       url:ip + "/api/large-picture-label-download",
       headers: {
          authorization:token,
        },
       dataType:"json",
	   data:{
		   "label_task_id" : labeltaskid,
           "needPicture" : 	isNeedPicture	   
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
	   setIntervalToDo(taskreturnid);
   }
}


var timeId=[];
var count;
var progress;

function setIntervalToDo(taskreturnid){
	count=0;
	var tmpTimeId = self.setInterval("clock('" + taskreturnid +"')",1000);//5秒刷新
	timeId.push(tmpTimeId);
	console.log("开始刷新。timeId=" + tmpTimeId);
}

function clock(taskreturnid){
   count++;
   if(count > 600 ){
       for(var i = 0;i < timeId.length; i++){
		    console.log("清除定时器1。exportTimeId=" + timeId[i]);
		    window.clearInterval(timeId[i]);
	   }
	   timeId = [];
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
		   
		    for(var i = 0;i < timeId.length; i++){
		       console.log("清除定时器2。exportTimeId=" + timeId[i]);
		       window.clearInterval(timeId[i]);
	        }
	        timeId = [];
		   
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
	   }else{
		   //更新进度
			var iSpeed = progress.progress;
			bar.style.width=iSpeed+'%';
			document.getElementById('text-progress').innerHTML=iSpeed+'%'
	   }
   }else{
	   count = 600;
   }
}


function page(current,pageSize){
  list(current,pageSize);
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
