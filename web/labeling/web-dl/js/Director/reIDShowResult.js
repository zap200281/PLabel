var token = getCookie("token");
var ip =  getIp();

var reid_task_id = sessionStorage.getItem("reid_task_id");

var tableData;
var tablePageData;
var pageSize = 40;



function list(reid_task_id,startPage,pageSize){
	if(isEmpty(reid_task_id)){
		console.log("没有指定reid_task_id");
		return;
	}
	
    $.ajax({
       type:"GET",
       url:ip + "/api/reid-task-show-result/",
       headers: {
          authorization:token,
        },
       dataType:"json",
	     data:{'reid_task_id':reid_task_id,
			   'startPage':startPage,
               'pageSize':pageSize},
       async:false,
       success:function(json){
        tablePageData = json;
        tableData = json.data;
        console.log(json);
        // return json.token;
       },
	   error:function(response) {
		  redirect(response);
       }
   });
}


function display_list(){

  if(isEmpty(tableData) || isEmpty(tableData[0])){
     document.getElementById('reIdShowResultTable').innerHTML="<div>还没有ReID数据，加紧标注吧...</div>";
     return;
  }

  var maxRowShow = 12;
  var head = tableData[0];
  var ht = "<tr>";
  
  ht += "<th>ReID标识</th>";
  ht += "<th>操作</th>";
  var maxLength = 1;
  for(var i = 0; i< tableData.length; i++){
	  var rowData = tableData[i].imgList;
	  if(rowData.length > maxLength){
		  maxLength = rowData.length;
	  }
  }
  
  // for(var i = 0; i < maxLength; i++){
	 //  ht += "<th>标注框" + (i + 1) + "</th>";
  // }
  var minTitleLen=Math.min(maxRowShow,maxLength);
  for(var i = 0; i < minTitleLen; i++){
    ht += "<th>标注框第" + (i + 1) + "列</th>";
  }
  ht += "</tr>";
  
  for(var i = 0; i< tableData.length; i++){
	  var row = "<tr>";
	  var rowData = tableData[i].imgList;
	  
	  row += "<td>" + tableData[i].reIdName +  "</td>";
	  
	  row += "<td><a onclick=\"deleteReId(\'"+reid_task_id+"\',\'" + tableData[i].reIdName +"\');\" class=\"btn btn-xs btn-success\">删除</a>&nbsp;&nbsp;&nbsp;<a onclick=\"modifyReId(\'"+reid_task_id+"\',\'" + tableData[i].reIdName +"\',\'" + rowData[j] + "\');\" class=\"btn btn-xs btn-success\">修改</a></td>";
	  
	  for(var j = 0; j < rowData.length; j++){
		  var imgName = rowData[j].substring(rowData[j].lastIndexOf('/') + 1);
		  
      if (j>=maxRowShow){
        row += "<td style=\"border-top:none\">"; //换行后上边界线不显示
      }
      else{
        row += "<td>";
      }
		  
		  row += "<div >"
          row += "<img src=\"" + ip + rowData[j] + "\" width=\"60\" height=\"120\" alt=\"" + imgName + "\">";
          //只有一列数据时，改成删除整行
         
          row += "<div>" + imgName +  "&nbsp;&nbsp;&nbsp; <a onclick=\"deteleAReIdImage(\'"+reid_task_id+"\',\'" + tableData[i].reIdName +"\',\'" + rowData[j] + "\');\">删除</a> <a onclick=\"modifyAReIdImage(\'"+reid_task_id+"\',\'" + tableData[i].reIdName +"\',\'" + rowData[j] + "\');\">修改</a> </div>";
          
          row += " </div>";
		  row += "</td>";
      //判断是否换行，可能会出现多行
		  if (j >= maxRowShow-1 && (j%(maxRowShow)==maxRowShow-1)){ //由于j从0开始
          row  += "</tr>" + "<td style=\"border-top:none\"></td>"+"<td style=\"border-top:none\"></td>";
      }
		  //row += "<td><img src=\"" + ip + rowData[j] + "\" width=\"60\" height=\"120\" alt=\"" + imgName + "\"></td>";
	  }
    for (var j=rowData.length;j < minTitleLen;j++){
         row += "<td ></td>"; //上边界线补齐
    }
	  
	  ht += row;
  }
  
  //console.log(ht);
  document.getElementById('reIdShowResultTable').innerHTML=ht;
}

function deteleAReIdImage(reid_task_id,reIdName,imgName){
	console.log("delete reId name:" + reIdName);
	if(confirm("确定要删除吗？")){
		$.ajax({
		   type:"DELETE",
		   url:ip + "/api/reId-task-delete-areidimg/",
		   headers: {
			  authorization:token,
			},
		   dataType:"json",
		   data:{
			   "reid_task_id" : reid_task_id,
			   "reid_name": reIdName,
			   "img_name": imgName
			},
		   async:false,
		   success:function(json){
			 console.log(json);
		   },
	       error:function(response) {
		     redirect(response);
           }
	   });
		
	   page(getTmpCurrent(),pageSize);
	}
	
}

function reIdAuto(){
	
	$("#hide_reidtaskid").val(reid_task_id);
	bar_1.style.width='1%';
	$("#text-progress_1").html("0%");
	$("#predtask_id_1").removeAttr("disabled");
	$("#autoReIdProgress").modal('show');
}

function submitReidAuto(){
	$("#predtask_id_1").attr("disabled","true");
	$.ajax({
		   type:"POST",
		   url:ip + "/api/reId-result-auto-sort/",
		   headers: {
			  authorization:token,
			},
		   dataType:"json",
		   data:{
			   "reid_task_id" : reid_task_id
			},
		   async:false,
		   success:function(json){
			console.log(json);
		   },
	       error:function(response) {
		     redirect(response);
           }
	 });
	 
	 //query progress
	 setIntervalToDo();
	
}

var timeId;
var count;
var tmpProgress;

function setIntervalToDo(){
	count=0;
	timeId = self.setInterval("clock()",1000);//5秒刷新
	console.log("开始刷新。timeId=" + timeId);
}

function clock(){
   count++;
   if(count > 600 ){
	   console.log("清除定时器。timeId=" + timeId);
	   window.clearInterval(timeId);
	   timeId = null;
	   $("#autoReIdProgress").modal('hide');
	   return;
   }
   
   $.ajax({
       type:"GET",
       url:ip + "/api/query-auto-label-task-progress/",
       headers: {
          authorization:token,
        },
       dataType:"json",
	   data:{'taskId': reid_task_id},
       async:false,
       success:function(json){
          tmpProgress = json;
          console.log(json);
       },
	   error:function(response) {
		  tmpProgress = null;
		  console.log('task has finished.');
		  redirect(response);
       }
   });
   if(isEmpty(tmpProgress)){
	   console.log("清除定时器。timeId=" + timeId);
	   bar_1.style.width='100%';
	   $("#text-progress_1").val('100%');
	   window.clearInterval(timeId);
	   timeId = null;
	  
	   $("#autoReIdProgress").modal('hide');
   }else{
	   //更新进度
	    var iSpeed = tmpProgress.progress;
	    bar_1.style.width=iSpeed+'%';
		$("#text-progress_1").html(iSpeed+'%');
   }
  
}


function modifyAReIdImage(reid_task_id,reIdName,imgName){
	console.log("modify reId name:" + reIdName);
	$("#hide_retaskid").val(reid_task_id);
	$("#hide_oldreidname").val(reIdName);
	$("#hide_cutimagename").val(imgName);
	$("#new_reid_name").val("");
	
	getSimilarImage(reid_task_id,imgName);
	showImage();
	
	$("#modify_new_reid_name").modal('show');
	
}

var tableBoxData;

function getSimilarImage(reid_task_id,imgName){
	
	 $.ajax({
	        type:"GET",
	        url:ip + "/api/reId-dest-imgs",
	        headers: {
	           authorization:token,
	         },
	        dataType:"json",
	        data:{
	         'reid_task_id':reid_task_id,
	         'pic_image_field':imgName,
	         'labelId':"-1"
			 },
	        async:false,
	        success:function(json){
	         tableBoxData = json;
	         console.log(json);
	        },
	        error:function(response) {
		     redirect(response);
            }
	    });
	
}


function showImage(){
 
	 
	 if(isEmpty(tableBoxData)){
		 console.log("tableBoxData is null.");
         //alert("未自动识别到相同目标。");
		 return;
	 }

  
     var row=" <div style=\"display:block\">";
	 //var row=" <div style=\"display:block\">";
     //row= row+"<hr style=\" margin:5px;border-top:1px solid #dddd\"></hr>"
     var grouplen = tableBoxData.length;
	 
	 var totalLength = 0;
	 for(var i = 0; i < grouplen; i++){
		 var imageList = tableBoxData[i].imageInfoList;
		 totalLength += imageList.length;
	 }
	 if(totalLength == 0){
		 //hideShowImage();
         //alert("未在其它视频或者图片中自动识别到相同目标。")
         return; 
	 }
	 console.log("grouplen:",grouplen)
	 for(var i = 0; i < grouplen; i++){
		 
     // row += " <div style=\" text-align:center;\"> " + tableBoxData[i].taskName + " </div>";
		 var imageList = tableBoxData[i].imageInfoList;
		 var maxlen = 4;
		 if(imageList.length < maxlen){
			 maxlen = imageList.length;
		 }
		 var isFirst = 0;
		 for (var j=0;j< maxlen; j++){
 
  			for(var key in  imageList[j]){
  				 console.log("key=" + key);
  				 var imageName = key.substring(key.lastIndexOf("/") + 1);
  				 //console.log("imageName=" + imageName);
            //var tmp = " <div class=\"show_image\" style=\'vertical-align:bottom;'\>" + "<div style=\"word-break: break-all;word-wrap: break-word\">" + imageList[j][key] + "</div>" + "<img src=" + ip + key + " id=\'reference_object\'/>" +  
                   //  "  </div>";
                console.log("nnn:"+imageList[j][key])
			    var tmp = "<td style=\"border-top:none;text-align:center\"> " + "<div><div style=\"word-break: break-all;word-wrap: break-word;text-align:center;font-weight: bold;color:#F00;font-size:20px\">" + imageList[j][key] + "</div>" + "<img  src=" + ip + key + " id=\'reference_object\'/></div>"+"</td>";    
                row = row+tmp;
                if (isFirst==0){
                	$('#new_reid_name').val(imageList[j][key]);
                	isFirst=1;
                }

  	        }
	        if(j%2==1){
             	row = row+"<tr></tr>" ////换行，每行两个
            } 
      }
 
      //row += "</div></div><hr style=\" margin:5px;border-top:1px solid #dddd\"></hr>";
	  row += "</div>";
	 }
	 
    
   document.getElementById('autodestimage').innerHTML=row;
}



function modifyReId(reid_task_id,reIdName){
	console.log("modify row reId name:" + reIdName);
	$("#hide_old_row_reidname").val(reIdName);
	$("#hide_row_retaskid").val(reid_task_id);
	$("#new_row_reid_name").val("");
	$("#modify_new_a_row_reid_name").modal('show');
}

function modify_new_row_reid_name(){
    var reid_task_id = $("#hide_row_retaskid").val();
	var reIdName = $("#hide_old_row_reidname").val();
	var new_reid_name = $("#new_row_reid_name").val();
	console.log("modify reId name:" + reIdName);
	if(isEmpty(new_reid_name)){
		alert("新的ReID标识不能为空。");
		return;
	}
	
	if(new_reid_name.length > 32){
		alert("新的ReID标识不能大于32个字符。");
		return;
	}
	$("#modify_new_a_row_reid_name").modal('hide');
	$.ajax({
		   type:"PATCH",
		   url:ip + "/api/reId-task-modify-reid/",
		   headers: {
			  authorization:token,
			},
		   dataType:"json",
		   data:{
			   "reid_task_id" : reid_task_id,
			   "reid_name": reIdName,
			   "new_reid_name":new_reid_name
			},
		   async:false,
		   success:function(json){
			console.log(json);
		   },
	       error:function(response) {
		     redirect(response);
           }
	 });
	 page(getTmpCurrent(),pageSize);
}


function modify_new_reid_name(){
	
	var reid_task_id = $("#hide_retaskid").val();
	var reIdName = $("#hide_oldreidname").val();
	var imgName = $("#hide_cutimagename").val();
	var new_reid_name = $("#new_reid_name").val();
	console.log("modify reId name:" + reIdName);
	if(isEmpty(new_reid_name)){
		alert("新的ReID标识不能为空。");
		return;
	}
	
	if(new_reid_name.length > 32){
		alert("新的ReID标识不能大于32个字符。");
		return;
	}
	$("#modify_new_reid_name").modal('hide');
	$.ajax({
		   type:"PATCH",
		   url:ip + "/api/reId-task-modify-areidimg/",
		   headers: {
			  authorization:token,
			},
		   dataType:"json",
		   data:{
			   "reid_task_id" : reid_task_id,
			   "reid_name": reIdName,
			   "img_name": imgName,
			   "new_reid_name":new_reid_name
			},
		   async:false,
		   success:function(json){
			console.log(json);
		   },
	       error:function(response) {
		     redirect(response);
           }
	 });
	 page(getTmpCurrent(),pageSize);
}


function deleteReId(reid_task_id,reIdName){
	console.log("delete reId name:" + reIdName);
	if(confirm("确定要删除吗？")){
		$.ajax({
		   type:"DELETE",
		   url:ip + "/api/reId-task-delete-reid/",
		   headers: {
			  authorization:token,
			},
		   dataType:"json",
		   data:{
			   "reid_task_id" : reid_task_id,
			   "reid_name": reIdName,
			},
		   async:false,
		   success:function(json){
			console.log(json);
		   },
	       error:function(response) {
		     redirect(response);
           }
	   });
		
	   page(getTmpCurrent(),pageSize);
	}
}


function setTaskId(){
	if(isEmpty(reid_task_id)){
		alert("ReID任务号为空，不能导出。");
		return;
	}
    $('#hide_labeltaskid').val(reid_task_id);
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
	   url:ip + "/api/reid-data-download/",
	   headers: {
		  authorization:token,
		},
	   dataType:"json",
	   data:{
		   "reid_task_id" : labeltaskid,
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
  list(reid_task_id,current,pageSize);
  display_list();
  setPage(tablePageData,pageSize);
  sessionStorage.setItem('reid_showResult_page',current);
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

function getTmpCurrent(){
	var tmpCurrent = sessionStorage.getItem("reid_showResult_page");
    if(isEmpty(tmpCurrent)){
	  tmpCurrent = 0;
    }
	return tmpCurrent;
}


page(getTmpCurrent(),pageSize);