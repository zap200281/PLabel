var token = getCookie("token");
var ip = getIp();

var pageSize = 12;

var tableData;
var tablePageData;

var dataSetData;

var uploadres;


function endWith(str, endStr){
    var d=str.length-endStr.length;
    return (d>=0&&str.lastIndexOf(endStr)==d)
}

function upload_multivideo_fileforpred(){
  var files = document.getElementById('multiVideoFile').files;
  
  if(isEmpty(files)){
	  alert("文件为空。");
	  return;
  }
  
  if(files.length > 20){
	  alert("不能超过20个视频。");
	  return;
  }

  for(var i = 0; i < files.length; i++){
	 var filename = files[i].name;
     console.log("filename=" + filename);
	 var filesize = files[i].size.toFixed(1);
	 console.log("file size=" + filesize);
//	 if(filesize > 3* 1024 * 1024 * 1024){
//		 alert("请选择压缩文件大小超过了3000M，不能上传。");
//		 return;
//	 }
	 console.log("file size=" + files[i].size.toFixed(1));
	 if(endWith(filename,".zip") || endWith(filename,".gz") || endWith(filename,".rar")){
		 alert("请直接选择视频文件，不能压缩。");
		 return;
	 }
	 if(isChineseChar(filename) || isFullwidthChar(filename) || filename.indexOf(" ") != -1){
		 alert("视频文件名称不支持中文、中文标点符号及空格。");
		 return;
	 }
  }
  
  
  
  var formdata = new FormData();
  for(var i = 0; i < files.length; i++){
	    formdata.append("files", files[i]);
  }
  
  formdata.append("datasettype", "4");
  $.ajax({ 
      url:ip + "/api/common-files-upload/", 
      type:"post", 
      headers: {
        // Accept: "application/json; charset=utf-8",
        authorization:token,
      },
      data:formdata, 
      enctype:"multipart/form-data",
      processData:false, 
      contentType:false, 
      beforeSend: function(){
      // Handle the beforeSend event
        // $("#uploadstate").attr({ disabled: "disabled" });
        $("#mul_video_uploadstate")[0].innerHTML="正在上传";
        document.getElementById("btnVideoSubmit").setAttribute("disabled", true);//设置不可点击
      },
      complete: function () {
        // $("#upload").removeAttr("disabled");
        //$("#mul_video_uploadstate")[0].innerHTML="上传完成";
      },
      success:function(res){ 
        if(res){
           console.log(res);			  
           uploadres=res;
		   var htmlstr = "";
		   for(var i = 0; i < uploadres.length; i++){
			   htmlstr += "<p>" + uploadres[i].origin_file_name + "</p>";
		   }
		   $("#mul_video_uploadstate")[0].innerHTML= "上传完成";
           document.getElementById("btnVideoSubmit").removeAttribute("disabled");//去掉不可点击
		   for(var i = 0; i < uploadSingleFileTimeId.length;i++){
			   console.log("清除定时器4。exportTimeId=" + uploadSingleFileTimeId[i]);
		       window.clearInterval(uploadSingleFileTimeId[i]);
		   }
		   uploadSingleFileTimeId=[];
		   multifileuploadbar.style.width='100%';
           document.getElementById('multifileupload-text-progress').innerHTML='100%';
		   if(isEmpty($('#mul_video_datasetname').val())){
			  var filename = files[0].name;
			  filename = filename.substring(0,filename.lastIndexOf("."));
		      $("#mul_video_datasetname").val(filename);
		   }
        }           
      }, 
	  xhr: function () {
                //获取ajax中的ajaxSettings的xhr对象  为他的upload属性绑定progress事件的处理函数
                var myXhr = $.ajaxSettings.xhr();
                if (myXhr.upload) {
                    //检查其属性upload是否存在
                    myXhr.upload.addEventListener("progress", resultProgressMulti, false);
                }
                return myXhr;
      },	
      error:function(err){ 
          console.log(err);
          $("#mul_video_uploadstate")[0].innerHTML= "上传失败，请重新上传";
          document.getElementById('multiVideoFile').value=''; //失败清空数据
		  for(var i = 0; i < uploadSingleFileTimeId.length;i++){
			   console.log("清除定时器4。exportTimeId=" + uploadSingleFileTimeId[i]);
		       window.clearInterval(uploadSingleFileTimeId[i]);
		  }
		  uploadSingleFileTimeId=[];
		  
          alert("网络连接失败,稍后重试",err); 
      } 
  })
	
}
 //上传进度回调函数
function resultProgressMulti(e) {
    if (e.lengthComputable) {
            var percent = e.loaded / e.total * 100;
			console.log("percent=" + percent);
			
			var realPercent = percent / 2;
			
            //$(".show_result").html(percent + "%");
            var percentStr = String(realPercent);
            if (percentStr == "50") {
                percentStr = "50.0";
            }
            iSpeed = percentStr.substring(0, percentStr.indexOf("."));
			multifileuploadbar.style.width=iSpeed+'%';
            document.getElementById('multifileupload-text-progress').innerHTML=iSpeed+'%';
			
			if(percent == 100){
			    uploadSingleFileProgress=50;
                var tmpTimeId = self.setInterval("uploadMultiFile('50')",1500);//1.5秒刷新		
			    uploadSingleFileTimeId.push(tmpTimeId);		
			}
    }
 }
 function uploadMultiFile(times){
	uploadSingleFileProgress++;
	if(uploadSingleFileProgress > 100){
		uploadSingleFileProgress = 100;
	}
    multifileuploadbar.style.width=uploadSingleFileProgress+'%';
	document.getElementById('multifileupload-text-progress').innerHTML=uploadSingleFileProgress+'%';
 }

function submit_video_dataset(){
  var datasetname=$('#mul_video_datasetname').val();
  var datasetdesc=$('#mul_video_datasetdesc').val();
  var camera_number = $('#mul_video_camera_number').val();
  var camera_gps = $('#mul_video_camera_gps').val();
  var camera_date = $('#mul_video_camera_date').val();
  if(!check(datasetname,datasetdesc,camera_gps,camera_date)){
	  return;
  }
  dataset_create(datasetname,datasetdesc, "4",$('#mul_video_assign_user option:selected').val(),camera_number,camera_gps,camera_date);
  
  $("#videoDatasetModal").modal('hide');
  $('#mul_video_datasetname').val("");
  $('#mul_video_datasetdesc').val("");
  $('#multiVideoFile').val("");
  
  $('#mul_video_camera_number').val("");
  $('#mul_video_camera_gps').val("");
  $('#mul_video_camera_date').val("");
  uploadres = null;
  $("#mul_video_uploadstate")[0].innerHTML="";

  page(0,pageSize);
	
}

function check(datasetname,datasetdesc,camera_gps,camera_date){
	 if (isEmpty(datasetname) || datasetname.length > 32){
       alert("数据集任务名称不能为空或者不能超过32个字符。")
       return false;
     }

     if(!isEmpty(datasetdesc) && datasetname.length > 500){
       alert("数据集任务描述不能超过500个字符。")
       return false;
     }
	
	 if(!isEmpty(camera_date)){
		 if(!isRightDateTime_dot(camera_date) && !isRightDateTime_yyyyMMddHHmmss(camera_date)){
			  alert("日期格式错误，请输入正确日期格式。");
			  return false;
		 }
	 }
	  
	 if(!isEmpty(camera_gps)){
		  var tmp_gps = camera_gps.split(",");
		  if(tmp_gps.length != 2){
			  alert("请输入正确的经纬度，用英文逗号隔开。");
			  return false;
		  }
		  if(!isRightLongitude(tmp_gps[0])){
			  alert("请输入正确的经度，经度整数部分为0-180,小数部分为0到6位。");
			  return false;
		  }
		  if(!isRightLatitude(tmp_gps[1])){
			  alert("请输入正确的纬度，纬度整数部分为0-90,小数部分为0到6位。");
			  return false;
		  }
	 }
	 return true;
}


function uploadfileforpred(){
  var file = document.getElementById('preInputFile').files[0];
  var filename = $('#preInputFile').val();
  console.log("filename=" + filename);
  var filesize = file.size.toFixed(1);
  console.log("file size=" + filesize);
//  if(filesize > 3* 1024 * 1024 * 1024){
//	   $('#preInputFile').val('');
//	   alert("请选择压缩文件大小超过了3000M，不能上传。");
//	   return;
//  }
  
  var datasettype = $('#dataset_type option:selected').val();
  if(datasettype==4){
	  if(endWith(filename,".zip") || endWith(filename,".gz") || endWith(filename,".rar")){
		  alert("请直接选择视频文件，不能压缩。");
		  $('#preInputFile').val('');
		  return;
	  }
  }
  if(datasettype==3){
      if(filesize < 20* 1024 * 1024){
         $('#preInputFile').val('');
         alert("选择文件小于20M了，不能上传。");
         return;
      }
      if(endWith(filename,".zip") || endWith(filename,".gz") || endWith(filename,".rar")){
          alert("请直接选择超大图像文件，不能压缩。");
          $('#preInputFile').val('');
          return;
      }
  }
  if(datasettype==1 || datasettype==2){
	  if(!endWith(filename,".zip")){
		  alert("请选择.zip格式的压缩文件。");
		  $('#preInputFile').val('');
		  return;
	  }
  }
  if(isChineseChar(filename) || isFullwidthChar(filename) || filename.indexOf(" ") != -1){
		 alert("上传的文件名称不支持中文及中文标点符号、空格。");
		 $('#preInputFile').val('');
		 return;
  }
  
  var formdata = new FormData();
  formdata.append("files", file);
  formdata.append("datasettype", datasettype);
  $.ajax({ 
      url:ip + "/api/common-file-upload-new/", 
      type:"post", 
      headers: {
        // Accept: "application/json; charset=utf-8",
        authorization:token,
      },
      data:formdata, 
      enctype:"multipart/form-data",
      processData:false, 
      contentType:false, 
      beforeSend: function(){
      // Handle the beforeSend event
        // $("#uploadstate").attr({ disabled: "disabled" });
        $("#uploadstate")[0].innerHTML="正在上传";
        document.getElementById("btnSubmit").setAttribute("disabled", true);//设置不可点击
      },
      // complete: function () {
      //   // $("#upload").removeAttr("disabled");
      //   $("#uploadstate")[0].innerHTML="上传完成";
      //   document.getElementById("btnSubmit").removeAttribute("disabled");//去掉不可点击
      // },
      success:function(res){ 
        if(res){
           console.log(res);			  
           uploadres=res;
           $("#uploadstate")[0].innerHTML="上传完成";
           document.getElementById("btnSubmit").removeAttribute("disabled");//去掉不可点击
		   for(var i = 0; i < uploadSingleFileTimeId.length;i++){
			   console.log("清除定时器3。exportTimeId=" + uploadSingleFileTimeId[i]);
		       window.clearInterval(uploadSingleFileTimeId[i]);
		   }
		   uploadSingleFileTimeId=[];
		   fileuploadbar.style.width='100%';
           document.getElementById('fileupload-text-progress').innerHTML='100%';
		   if(isEmpty($('#datasetname').val())){
			    var filename = file.name;
			    filename = filename.substring(0,filename.lastIndexOf("."));
		        $('#datasetname').val(filename);
		   }
        }           
      },
      xhr: function () {
                //获取ajax中的ajaxSettings的xhr对象  为他的upload属性绑定progress事件的处理函数
                var myXhr = $.ajaxSettings.xhr();
                if (myXhr.upload) {
                    //检查其属性upload是否存在
                    myXhr.upload.addEventListener("progress", resultProgress, false);
                }
                return myXhr;
      },	  
      error:function(err){ 
        console.log(err);
        $("#uploadstate")[0].innerHTML="上传失败，请重新上传";
        document.getElementById('preInputFile').value=''; //失败清空数据
		for(var i = 0; i < uploadSingleFileTimeId.length;i++){
			   console.log("清除定时器3。exportTimeId=" + uploadSingleFileTimeId[i]);
		       window.clearInterval(uploadSingleFileTimeId[i]);
		}
		uploadSingleFileTimeId=[];
        alert("网络连接失败,稍后重试",err); 
      } 
  })
}

 //上传进度回调函数
function resultProgress(e) {
    if (e.lengthComputable) {
            var percent = e.loaded / e.total * 100;
			console.log("percent=" + percent);
			
			var realPercent = percent / 2;
			
            //$(".show_result").html(percent + "%");
            var percentStr = String(realPercent);
            if (percentStr == "50") {
                percentStr = "50.0";
            }
            iSpeed = percentStr.substring(0, percentStr.indexOf("."));
			fileuploadbar.style.width=iSpeed+'%';
            document.getElementById('fileupload-text-progress').innerHTML=iSpeed+'%';
			
			if(percent == 100){
			   uploadSingleFileProgress=50;
               var tmpTimeId = self.setInterval("uploadSingleFile('50')",1500);//1.5秒刷新		
			   uploadSingleFileTimeId.push(tmpTimeId);		
			}
    }
 }
 var uploadSingleFileTimeId = [];
 var uploadSingleFileProgress;
 function uploadSingleFile(times){
	uploadSingleFileProgress++;
	if(uploadSingleFileProgress > 100){
		uploadSingleFileProgress = 100;
	}
    fileuploadbar.style.width=uploadSingleFileProgress+'%';
	document.getElementById('fileupload-text-progress').innerHTML=uploadSingleFileProgress+'%';
 }


function dataset_task_list(){
  $.ajax({
     type:"GET",
     url:ip + "/api/dataset-page/",
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



function submit_dataset(){
  // console.log($('#datasetname').val());
  var datasetname=$('#datasetname').val();
  var datasetdesc=$('#datasetdesc').val();
  var camera_number = $('#camera_number').val();
  var camera_gps = $('#camera_gps').val();
  var camera_date = $('#camera_date').val();
  
  if(!check(datasetname,datasetdesc,camera_gps,camera_date)){
	  return;
  }
  
  dataset_create(datasetname,datasetdesc, $('#dataset_type option:selected').val(),$('#assign_user option:selected').val(),camera_number,camera_gps,camera_date);
  
  $("#datasetModal").modal('hide');
  $('#datasetname').val("");
  $('#datasetdesc').val("");
  $('#preInputFile').val("");
  
  $('#camera_number').val("");
  $('#camera_gps').val("");
  $('#camera_date').val("");
  
  uploadres = null;
  $("#uploadstate")[0].innerHTML="";
  
  page(0,pageSize);
}

var count = 0;
var videoList;

function heBing(index){
	
	count = 0;
	
	var tabelhead="<thead><tr>"+
	   "<th>勾选</th><th id=\"dataset_head\"></th><th>序号</th><th>视频名称</th> <th>时长</th></tr> </thead>";
	
	//var videoList = ['count.mp4','test.mp4'];
	listVideoSet(tableData[index].id);
	
	$('#hidevideo_datasetid').val(tableData[index].id);
	
	var tabelContent="<tbody>";
	if(!isEmpty(videoList)){
		for(var i = 0; i <videoList.length; i++){
			tabelContent +="<tr>"
			tabelContent +="<td><input id=\"video_checkbox\" type=\"checkbox\" onchange=\"checkboxOnclick(this)\"/></td>";
			tabelContent += "<td id=\"video_info_id\">"+ videoList[i].id + "</td>";
			tabelContent += "<td id=\"number_checkbox\"></td>";
			tabelContent += "<td>" +videoList[i].minio_url.substring(videoList[i].minio_url.lastIndexOf("/") + 1) + "</td>";
			tabelContent += "<td>" + videoList[i].duration + "</td>"
			tabelContent +="</tr>"
	    }
		tabelContent +="</tbody>";
	}
	
	var html = tabelhead + tabelContent;
	
	console.log(html);
	
	document.getElementById('myTable').innerHTML=html;

    $('#myTable tbody tr').find('td:eq(1)').hide();
    $('#myTable thead tr').find('th:eq(1)').hide();
	

	$(function() {
      $("#myTable").tablesorter();
    });
	
	$("#videoHeBingModal").modal('show');
}

function isChineseChar(str){   
   var reg = /[\u4E00-\u9FA5\uF900-\uFA2D]/;
   return reg.test(str);
}
//同理，是否含有全角符号的函数
function isFullwidthChar(str){
   var reg = /[\uFF00-\uFFEF]/;
   return reg.test(str);
}

function submit_concat_video(){
	var valuemap = new Map();
	
	var orderMap = new Map();
	
	for(var i = 0; i <videoList.length; i++){
		valuemap.set((i+1)+"",i+1);//字符串作key
	}
	
	var Check = $("table[id='myTable'] input[type=checkbox]:checked");//在table中找input下类型为checkbox属性为选中状态的数据
        Check.each(function () {//遍历
              var row = $(this).parent("td").parent("tr");//获取选中行
              var id = row.find("[id='number_checkbox']").html();//获取用户设置的序号值  
              var video_id = row.find("[id='video_info_id']").html();//获取用户设置的序号值  
              valuemap.delete(id);
			  orderMap.set(id,video_id);
     });
	
	console.log("valuemap.size=" + valuemap.size);
	if(valuemap.size > 0){
		for (var [key, value] of valuemap) {
          console.log('key', key);
          console.log('value', value);
        }   
		alert("视频的顺序不对，请重新调整。确保所有视频的序号不为空且唯一。");
		return;
	}
	var orderList = [];
    for(var i = 0; i <videoList.length; i++){
		orderList.push(orderMap.get((i+1)+""));
	} 	
	console.log("orderList =" + orderList);
	
	var datasetId = $('#hidevideo_datasetid').val();
	var destFileName =  $('#concat_video_name').val();
	
	if(isEmpty(destFileName) || isChineseChar(destFileName) || isFullwidthChar(destFileName) || destFileName.indexOf(" ") != -1){
		alert("视频的合并名称不能为空，也不能包含中文及中文标点、空格。");
		return;
	}
	
	var index= destFileName.lastIndexOf(".");
    //获取后缀
    if(index <= 0){
		destFileName = destFileName + ".mp4";
	}
	
	
	console.log("datasetId=" + datasetId);
	console.log("destFileName=" + destFileName);
	
	$.ajax({
       type:"POST",
       url:ip + "/api/datesetVideoConcat/",
       dataType:"json",
       async:false,
       headers: {
          authorization:token,
        },
       data:{ 
	          'datasetId':datasetId,
	          'videoSetIdList':JSON.stringify(orderList),
			  'destFileName':destFileName
           },
       success:function(res){
        console.log(res);
       },
	   error:function(response) {
		  redirect(response);
       }
   });
   
   $("#videoHeBingModal").modal('hide');
   $('#concat_video_name').val("");
   var current =$('#displayPage1').text();
   page(current - 1,pageSize);
}



function listVideoSet(datasetId){
      $.ajax({
       type:"GET",
       url:ip + "/api/datasetVideoList/",
       headers: {
          authorization:token,
        },
       dataType:"json",
       data:{'datasetId':datasetId},
       async:false,
       success:function(json){
        videoList = json;
        console.log(json);
       },
	   error:function(response) {
		  redirect(response);
       }
   });
}

function checkboxOnclick1(){
	console.log("checkbox id=");
}

function checkboxOnclick(checkbox){
	if ( checkbox.checked == true){
		 count++;
		 var row = $(checkbox).parent("td").parent("tr");//获取选中行
         var td = row.find("[id='number_checkbox']");//获取列
		 td.html("" + count);
		 //console.log(td);
    }else{
         count--;
		 var row = $(checkbox).parent("td").parent("tr");//获取选中行
         var td = row.find("[id='number_checkbox']");//获取列
		 td.html("");
    }
}


function dataset_create(datasetname,datasetdesc, datasettype,assign_user_id,camera_number,camera_gps,camera_date){

    var zip_object_name = "";
    var zip_bucket_name = "";
	var videoSet =[];
    if(!isEmpty(uploadres)){
      zip_object_name = uploadres[0].object_name;
      zip_bucket_name = uploadres[0].bucket_name;
	  
	  if(uploadres.length > 1){
		  for(var i = 0; i < uploadres.length; i++){
			  console.log(uploadres[i].public_url);
			  videoSet.push(uploadres[i].public_url);
		  }
	  }
    }
	console.log("zip_object_name=" + zip_object_name);
	console.log("zip_bucket_name=" + zip_bucket_name);
    $.ajax({
       type:"POST",
       contentType:'application/json',
       url:ip + "/api/dataset/",
       dataType:"json",
       async:false,
       headers: {
          authorization:token,
        },
       data:JSON.stringify({'task_name':datasetname,
							'task_desc':datasetdesc,
                            'zip_object_name':zip_object_name,
                            'zip_bucket_name':zip_bucket_name,
                            'dataset_type':datasettype,
                            'assign_user_id':assign_user_id,
							'camera_number':camera_number,
			                'camera_gps':camera_gps,
				            'camera_date':camera_date,
							'videoSet':JSON.stringify(videoSet)

           }),
       success:function(res){
        alert("数据集任务创建成功!");
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
       url:ip + "/api/dataset-page/",
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
       },
	   error:function(response) {
		  redirect(response);
       }
   });
}

function delete_dataset(){
  var stop = del();
  if (stop){
    return;
  }
  var Check = $("table[id='dataset_list'] input[type=checkbox]:checked");//在table中找input下类型为checkbox属性为选中状态的数据
        Check.each(function () {//遍历
              var row = $(this).parent("td").parent("tr");//获取选中行
              var id = row.find("[id='dataset_id']").html();//获取name='Sid'的值
              delete_dataset_byid(id);
          });
  page(0,pageSize);
}


function del() {
    if($("table[id='dataset_list'] input[type=checkbox]").is(":checked")) {
        if (confirm("确实要删除吗？")) {
            // alert("已经删除！");
            return false;
        } else {
            // alert("已经取消了删除操作");
            return true;
        }
    }else if($("table[id='dataset_list']").find("input").length=="0"){
        alert("暂无可删的数据！");
        return true;
    }else{
        alert("请先选择需要删除的选项！");
        return true;
    }
  }

function delete_dataset_byid(label_task_id){
  $.ajax({
    type:"DELETE",
    url:ip + "/api/dateset/",
    headers: {
       authorization:token,
     },
    dataType:"json",
    async:false,
    data:{'dateset_id': label_task_id},
    success:function(json){
      console.log(json);
    },
	error:function(response) {
		  redirect(response);
    }
 });
}

function getDatasetType(datasetType){
	if(datasetType == 1){
		return "图片";
	}else if(datasetType == 2){
		return "CT影像";
	}else if(datasetType == 3){
       return "超大图像";
    }
    else if(datasetType == 4){
		return "视频";
	}
	else{
		return "其它";
	}
}

function getChouzheng(index, datasetType){
	if(datasetType == 4){
		if(isEmpty(getHeBing(index, datasetType))){//先合并再抽帧
		   return "&nbsp;&nbsp;&nbsp;<a onclick=\"chou_zhen(\'" + index + "\');\" class=\"btn btn-xs btn-success\">抽帧</a>";
		}
	}
	return "";
}

function getHeBing(index, datasetType){
	if(datasetType == 4){
		if(!isEmpty(tableData[index].videoSet) && tableData[index].videoSet.length > 1){
			return "&nbsp;&nbsp;&nbsp;<a onclick=\"heBing(\'" + index + "\');\" class=\"btn btn-xs btn-success\">合并</a>";
		}
	}
	return "";
}

function display_list(){
  var len = 0;
  if(tableData != null){
	  len = tableData.length;
  }
  var html="<tr>\
            <th></th>\
            <th id=\"dataset_head\"></th>\
            <th>数据集名称</th>\
            <th>数据集类型</th>\
			<th>数据集描述</th>\
			<th>数据集创建者</th>\
			<th>数据集图片数量</th>\
			<th>摄像头编号</th>\
			<th>摄像头坐标</th>\
			<th>拍摄时间</th>\
			<th>指派给</th>\
			<th>创建时间</th>\
            <th>操作</th>\
            </tr>";
  for (var i=0;i<len;i++){
    var row = "<tr>\
            <td><input type=\"checkbox\" class=\"flat-grey list-child\"/></td>\
            <td id=\"dataset_id\">"+tableData[i].id+"</td>\
            <td>"+tableData[i].task_name+"</td>\
            <td>"+getDatasetType(tableData[i].datasetType)+"</td>\
			<td>"+tableData[i].task_desc+"</td>\
			<td>"+tableData[i].user+"</td>\
			<td>"+getStatus(tableData[i].total,tableData[i].task_status,tableData[i].task_status_desc)+"</td>\
			<td>"+tableData[i].camera_number+"</td>\
			<td>"+tableData[i].camera_gps+"</td>\
			<td>"+tableData[i].camera_date+"</td>\
            <td>"+tableData[i].assign_user+"</td>\
            <td>"+tableData[i].task_add_time+"</td>\
            <td>"+
			getOper(i,tableData[i].id,tableData[i].datasetType,tableData[i].task_status)
			+"</td>\
            </tr>";
    html=html+row;
  }
  //console.log(html);
  document.getElementById('dataset_list').innerHTML=html;

  $('#dataset_list tr').find('td:eq(1)').hide();
  $('#dataset_list tr').find('th:eq(1)').hide();
  
  setIntervalToDo();
}

function getOper(index,id,datasetType,status){
	if(status == 2){
	    return "";
	}else{
		return "<a onclick=\"sessionStorage.setItem(\'dataset\',\'"+id+"\'); window.location.href=\'datasetPreview.html\'\" class=\"btn btn-xs btn-success\">预览</a>"  + 
            "&nbsp;&nbsp;&nbsp;<a onclick=\"modify_this(\'" + index + "\');\" class=\"btn btn-xs btn-success\">修改</a>"
			
			+ getChouzheng(index,datasetType)
			
			+ getHeBing(index,datasetType);
	}
	
}

function getStatus(total,status,status_desc){
	if(status == 1000){
		if(isEmpty(status_desc)){
			status_desc = "0%";
		}
		return "抽帧进度:" + status_desc;
	}else if(status == 2000){
		if(isEmpty(status_desc)){
			status_desc = "0%";
		}
		return "合并进度:" + status_desc;
	}else if(status == 2){
		if(isEmpty(status_desc)){
			status_desc = "数据处理中";
		}
		return status_desc;
	}
	return total;
}

var timeId = [];
var tmpCount;

function setIntervalToDo(){
	var isNeedToSetInterval = false;
	var maxtime= 3000;
	if(!isEmpty(tableData)){
        for (var i=0;i<tableData.length;i++){
		    if(tableData[i].task_status == 1000 || tableData[i].task_status==2000 || tableData[i].task_status==2){
				console.log("有任务在进行中。需要自动刷新。");
				isNeedToSetInterval = true;//有任务在进行中才刷新，否则不刷新。
				if(tableData[i].task_status == 2){
			        maxtime = 500;
		        }
				break;
			}
	    }
	}
	if(!isEmpty(timeId)){
		for(var i =0; i < timeId.length ;i++){
			console.log("清除定时器。timeId=" + timeId[i]);
		    window.clearInterval(timeId[i]);
		}
		timeId = [];
	}
	if(isNeedToSetInterval){
		tmpCount = 0;
		var tmpTimeId = self.setInterval("clock('" + maxtime +"')",5000);//5秒刷新
		timeId.push(tmpTimeId);
		console.log("开始刷新。tmpTimeId=" + tmpTimeId);
	}
}

function clock(maxTime){
   tmpCount = tmpCount + 1;
   if(tmpCount > maxTime){
	    for(var i =0; i < timeId.length ;i++){
			console.log("清除定时器。timeId=" + timeId[i]);
		    window.clearInterval(timeId[i]);
		}
		timeId = [];
	    return;
   }
   var current = $('#displayPage1').text();
   console.log("开始刷新。current=" + current);
   if(current >= 1){
       page(current - 1,pageSize);
   }
}

var chouzhen_datasetname;

function chou_zhen(index){
  chouzhen_datasetname = tableData[index].camera_date;
	$('#chou_zhen_hidedatasetid').val( tableData[index].id);
	$('#chouzhen_datasetname').val( tableData[index].task_name);
	$('#chouzhen_camera_date').val( tableData[index].camera_date);
	$('#chouzhen_option').val("0");//默认抽关键帧
	$('#chouzhen_num_persecond').attr("disabled","true");
    $('#chouzhen_num_persecond').val("0.5");//默认每秒抽一帧
	$('#chouzhen_filename_type').val("0");
	$('#chouzhen_camera_date').attr("disabled","true");
	$('#isDeleteVideo').val("0");
    $("#chouzhengModal").modal('show');
}


function submit_chouzhen(){
	var id= $('#chou_zhen_hidedatasetid').val();
	var chouzhen_option = $('#chouzhen_option option:selected').val();
	var chouzhen_num_persecond= $('#chouzhen_num_persecond').val();
	if(!isEmpty(chouzhen_num_persecond)){
		//校验是否是数字
		var regPos = /^\d+(\.\d+)?$/; //非负浮点数
		if(!regPos.test(chouzhen_num_persecond)){
			 alert("每秒抽帧数量格式不对，请输入正确的值。");
			 return false;
		}
	}
	var chouzhen_filename_type =  $('#chouzhen_filename_type option:selected').val();
	var chouzhen_camera_date = $('#chouzhen_camera_date').val();
	var createAutoLabelTask = $('#createAutoLabelTask option:selected').val();
	if(!isEmpty(chouzhen_camera_date)){
		 if(!isRightDateTime_dot(chouzhen_camera_date) && !isRightDateTime_yyyyMMddHHmmss(chouzhen_camera_date)){
			  alert("日期格式错误，请输入正确日期格式。");
			  return false;
		 }
	 }
	var widthHeight =  $('#chouzhen_width_height').val();
	if(!isEmpty(widthHeight)){
		//校验是否是正确的 1280x760 这样的格式
		var tmpIndex = widthHeight.toLowerCase().indexOf("x");
		if(tmpIndex == -1){
			alert("指定图片分辨率格式错误，请用小写的x或者大写X字母隔开。"); 
			return false;
		}
		var width = widthHeight.substring(0,tmpIndex);
		var height = widthHeight.substring(tmpIndex + 1);
		if (!(/(^[1-9]\d*$)/.test(width))) { 
　　　　　　alert("指定图片分辨率的宽度不是正整数。"); 
　　　　　　return false; 
　　　　}
        if (!(/(^[1-9]\d*$)/.test(height))) { 
　　　　　　alert("指定图片分辨率的高度不是正整数。"); 
　　　　　　return false; 
　　　　}
	}
	 var isDeleteVideo = $('#isDeleteVideo option:selected').val();
	 $.ajax({
       type:"POST",
       contentType:'application/json',
       url:ip + "/api/dateset-chouzhen/",
       dataType:"json",
       async:false,
       headers: {
          authorization:token,
        },
       data:JSON.stringify({'dateset_id':id,
							'fps':chouzhen_num_persecond,
                            'drawFrameType':chouzhen_option,
							'fileNameFormate':chouzhen_filename_type,
							'baseDate':chouzhen_camera_date,
							'widthHeight':widthHeight,
							'isDeleteVideo':isDeleteVideo,
							'createAutoLabelTask':createAutoLabelTask
							
           }),
       success:function(res){
         console.log("抽帧任务执行中!");
         console.log(res);
       },
	   error:function(response) {
		  redirect(response);
       }
	  });
	
	$("#chouzhengModal").modal('hide');
	
	$('#chouzhen_num_persecond').val("");
	var current =$('#displayPage1').text();
    page(current - 1,pageSize);
}


function modify_this(index){
	
	$('#hidedatasetid').val( tableData[index].id);
	$('#datasetdesc_modify').val( tableData[index].task_desc);
	
	getUser();
    dislpayUser(tableData[index].assign_user);
	
	$("#modifyDatasetModal").modal('show');
	
}

function submit_modify_dataset(){
	var id= $('#hidedatasetid').val();
    var datasetdesc = $('#datasetdesc_modify').val();
	var assign_user_id = $('#assign_user_modify option:selected').val();
	
    $.ajax({
       type:"POST",
       contentType:'application/json',
       url:ip + "/api/updatedataset/",
       dataType:"json",
       async:false,
       headers: {
          authorization:token,
        },
       data:JSON.stringify({'id':id,
							'task_desc':datasetdesc,
                            'assign_user_id':assign_user_id
           }),
       success:function(res){
        console.log("数据集任务修改成功!");
        console.log(res);
       },
	   error:function(response) {
		  redirect(response);
       }
	  });
	
	$("#modifyDatasetModal").modal('hide');
	
    $('#hidedatasetid').val("");
    $('#datasetdesc_modify').val("");
     
	var current =$('#displayPage1').text();
    page(current - 1,pageSize);
}


function page(current,pageSize){
  list(current,pageSize);
  display_list();
  setPage(tablePageData,pageSize);
  sessionStorage.setItem('dataset_task_page',current);
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

  var pageNum = parseInt(pageData.total/pageSize);
  if(pageData.total%pageSize!=0){
      pageNum += 1;
  }else {
      pageNum = pageNum;
  }
 $("#totalPageNum").text(pageNum);

}

var tmpCurrent = sessionStorage.getItem("dataset_task_page");
if(isEmpty(tmpCurrent)){
	tmpCurrent = 0;
}
page(tmpCurrent,pageSize);




function setUserData(type){
  getUser();
  dislpayUser("");
  if (isEmpty($('#preInputFile').val())){      
    document.getElementById("btnSubmit").setAttribute("disabled", true);//设置不可点击
  }
  if (isEmpty($('#multiVideoFile').val())){
    document.getElementById("btnVideoSubmit").setAttribute("disabled", true);//设置不可点击
  }
  
  fileuploadbar.style.width='1%';
  document.getElementById('fileupload-text-progress').innerHTML='0%';
  
  multifileuploadbar.style.width='1%';
  document.getElementById('multifileupload-text-progress').innerHTML='0%';
}

function dislpayUser(username){
  var html="<option value=\"\" selected=\"\">请选择</option>";
  for (var i=0;i<userInfoData.length;i++){
	    var row = "<option value=\""+userInfoData[i].id+
        "\">"+userInfoData[i].username+
        "</option>";
	    if(userInfoData[i].username == username){
			row = "<option value=\""+userInfoData[i].id+
              "\" selected=\"true\">" + userInfoData[i].username+
                "</option>";
		}
      html=html+row;
  }
  console.log(html);
  document.getElementById('assign_user').innerHTML=html; 
  document.getElementById('assign_user_modify').innerHTML=html; 
  document.getElementById('mul_video_assign_user').innerHTML=html; 
  
}

function dataset_type_sele_Change(event){
	var dataset_type = $('#dataset_type option:selected').val();
	console.log("dataset_type=" + dataset_type);
	if(dataset_type == 4 || dataset_type == 1){
		document.getElementById("camera_number_div").style.display="block";
		document.getElementById("camera_gps_div").style.display="block";
		document.getElementById("camera_date_div").style.display="block";
	}else{
		document.getElementById("camera_number_div").style.display="none";
		document.getElementById("camera_gps_div").style.display="none";
		document.getElementById("camera_date_div").style.display="none";
	}
  if(dataset_type == 3){
     document.getElementById("labelInfo").innerHTML = "请选择文件进行上传(最大3000M，最小20M)<font color=red>*</font>";
     document.getElementById("fileDescri").innerText = "不压缩文件: 单个.svs或.tif文件";
  }
  else{
     document.getElementById("labelInfo").innerHTML = "请选择文件进行上传(最大3000M)<font color=red>*</font>";
     document.getElementById("fileDescri").innerText = "图片/CT影像：zip文件";
  }
}

function chouzhen_option_sele_Change(event){
	var chouzhen_option = $('#chouzhen_option option:selected').val();
	console.log("chouzhen_option=" + chouzhen_option);
	if(chouzhen_option == 0){
		 $('#chouzhen_num_persecond').attr("disabled","true");
	}else{
		$('#chouzhen_num_persecond').removeAttr("disabled");
	}
}

function chouzhen_filename_type_sele_Change(event){
	var chouzhen_filename_type = $('#chouzhen_filename_type option:selected').val();
	console.log("chouzhen_filename_type=" + chouzhen_filename_type);
	if(chouzhen_filename_type == 0 || chouzhen_filename_type == 1){
		$('#chouzhen_camera_date').val('');
		$('#chouzhen_camera_date').attr("disabled","true");
	}else{
		$('#chouzhen_camera_date').removeAttr("disabled");
	}
	  $('#chouzhen_camera_date').val( chouzhen_datasetname);
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

function setTaskId(){
	var Check = $("table[id='dataset_list'] input[type=checkbox]:checked");//在table中找input下类型为checkbox属性为选中状态的数据
	if(Check.length != 1){
		 alert("请选择一个仅且一个数据集进行导出。");
		 return;
	}
	 
    Check.each(function () {//遍历
        var row = $(this).parent("td").parent("tr");//获取选中行
        var id = row.find("[id='dataset_id']").html();//获取name='Sid'的值
        $('#hide_labeltaskid').val(id);
    });
    document.getElementById("predtask_id").removeAttribute("disabled");
	$('#labeltaskexport').modal('show');
}

function downloadFile(){
	document.getElementById("predtask_id").setAttribute("disabled", true);
	var labeltaskid = $('#hide_labeltaskid').val();
    var taskreturnid = "";
	$.ajax({
	   type:"GET",
	   url:ip + "/api/dataset-picture-download/",
	   headers: {
		  authorization:token,
		},
	   dataType:"json",
	   data:{
		   "data_set_id" : labeltaskid 
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

  