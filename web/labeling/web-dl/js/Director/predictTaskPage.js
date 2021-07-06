var token = getCookie("token");
var ip = getIp();

var pageSize = 10;

var tableData;
var tablePageData;

var uploadres;

var algModelList;

var algPropertyModelList;

var dataSetTaskData;


function setModel(){
  loadModel();
  loadPropertyModel();
  display_createlabel();
  
  setDataSetTask();
}

function display_createlabel(){
  var html="<option value=\"\" selected=\"\">请选择</option>";
  for (var i=0;i<algModelList.length;i++){
        var row = "<option value=\""+algModelList[i].id+
        "\">"+algModelList[i].model_name+
        "</option>";
      
      html=html+row;
  }
  console.log(html);
   document.getElementById('pre_predict_model').innerHTML=html; 
   
  html="<option value=\"\" selected=\"\">请选择</option>";
  for (var i=0;i<algPropertyModelList.length;i++){
        var row = "<option value=\""+algPropertyModelList[i].id+
        "\">"+algPropertyModelList[i].model_name+
        "</option>";
      
      html=html+row;
  }
  
  document.getElementById('needToDistiguishTypeOrColor').innerHTML=html; 
}

function loadPropertyModel(){

  $.ajax({
    type:"GET",
    contentType:'application/json',
    url: ip + "/api/queryAlgModelForProperty/",
    dataType:"json",
    async:false,
    headers: {
       // Accept: "text/html; q=1.0", 
       authorization:token,
     },
    // enctype:"multipart/form-data",
    success:function(jsonList){
     console.log(jsonList);
     algPropertyModelList = jsonList;
     // return json.token;
    },
	error:function(response) {
		redirect(response);
    }
});
}


function loadModel(){

  $.ajax({
    type:"GET",
    contentType:'application/json',
    url: ip + "/api/queryAlgModelForAutoLabel/",
    dataType:"json",
    async:false,
    headers: {
       // Accept: "text/html; q=1.0", 
       authorization:token,
     },
    // enctype:"multipart/form-data",
    success:function(jsonList){
     console.log(jsonList);
     algModelList = jsonList;
     // return json.token;
    },
	error:function(response) {
		redirect(response);
    }
});
}

function setDataSetTask(){
	dataset_task_list();
	display_createdatasetlabel(0);
	sele_Change("");
	
}

function sele_Change(sele){
  var dataSetName = $('#dataset_list option:selected').text();
  console.log("select dataSetName =" + dataSetName);
  $("#predtaskname").attr({value:dataSetName+"-自动标注"});
  $("#score_threshhold").val("0.8");
}

function dataset_task_list(){
  $.ajax({
     type:"GET",
     url:ip + "/api/dataset/",
     headers: {
        authorization:token,
      },
     dataType:"json",
	 data:{'dateset_type':'[1,3,4]'},
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
	var row ="";
    if (i==sindex){
        row = "<option value=\""+dataSetTaskData[i].id+
              "\" selected=\"\">"+dataSetTaskData[i].task_name+
              "</option>";
    }else{
        row = "<option value=\""+dataSetTaskData[i].id+
        "\">"+dataSetTaskData[i].task_name+
        "</option>";
      }
    html=html+row;
  }
  console.log(html);
  document.getElementById('dataset_list').innerHTML=html; 
}



function pre_predict_task_create(task_name, dataset_id, alg_model,delete_no_label_picture,needToDistiguishTypeOrColor){

	if (isEmpty(task_name) || task_name.length > 32){
       alert("自动标注任务名称不能为空或者不能超过32个字符。");
       return false;
    }
    if(isEmpty(alg_model)){
      alert("请选择算法模型。")
      return false;
    }
	if(isEmpty(dataset_id)){
	  alert("请选择数据集。")
      return false;
	}
	
	var delete_similar_picture = $("#delete_similar_picture").val();
	var score_threshhold = $("#score_threshhold").val();
	if(isEmpty(score_threshhold)){
		score_threshhold = 0;
	}
	var regPos = /^\d+(\.\d+)?$/; //非负浮点数
    if(!regPos.test(score_threshhold)){
			 alert("删除低于指定得分的标注填写的值为0--0.99之间。");
			 return false;
	}else{
		if(score_threshhold >=1){
			alert("删除低于指定得分的标注填写的值为0--0.99之间。");
			return false;
		}
	}
	
    var re = true;

	console.log(token)

	console.log(JSON.stringify({'task_name':task_name,
             'dataset_id':dataset_id,
             'alg_model':alg_model,
           }))
    $.ajax({
       type:"POST",
       contentType:'application/json',
       url: ip + "/api/pre-predict-task/",
       dataType:"json",
       async:false,
       headers: {
          // Accept: "text/html; q=1.0", 
          authorization:token,
        },
       // enctype:"multipart/form-data",
       data:JSON.stringify({'task_name':task_name,
             'dataset_id':dataset_id,
             'alg_model':alg_model,
			 'delete_no_label_picture':delete_no_label_picture,
			 'needToDistiguishTypeOrColor':needToDistiguishTypeOrColor,
			 'delete_similar_picture':delete_similar_picture,
			 'score_threshhold':score_threshhold
           }),
       success:function(res){
			console.log(res)
			if(res.code == 0){
			  alert("开始自动标注!")
			}else{
			  alert("创建自动标注任务失败，" + res.message);
			  re = false;
			}
       },
	   error:function(res){
		   console.log(res);
		   re = false;
		   redirect(response);
	   }
   });

    return re;
}


function submit_predtask(){
  var res = pre_predict_task_create($("#predtaskname").val(),$("#dataset_list").val(),$("#pre_predict_model").val(),$("#delete_no_label_picture").val(),$("#needToDistiguishTypeOrColor").val());
  if (res==false){
    return;
  }
  $("#prepredModal").modal('hide');

  pre_predict_task_list(0,pageSize);

  display_pre_predict_task_list();

}

function pre_predict_task_list(startPage,pageSize){
    $.ajax({
       type:"GET",
       url:ip + "/api/pre-predict-task-page/",
       headers: {
          authorization:token,
        },
       dataType:"json",
	     data:{'startPage':startPage,
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

function delete_predicttask(){
  var stop = del();
  if (stop){
    return;
  }
  var Check = $("table[id='pre_predict_task_list'] input[type=checkbox]:checked");//在table中找input下类型为checkbox属性为选中状态的数据
        Check.each(function () {//遍历
              var row = $(this).parent("td").parent("tr");//获取选中行
              var id = row.find("[id='predicttask_id']").html();//获取name='Sid'的值
              delete_predicttask_byid(id);
          });
  pre_predict_task_list(0,pageSize);
  display_pre_predict_task_list();
}

function del(){
    if($("table[id='pre_predict_task_list'] input[type=checkbox]").is(":checked")) {
        if (confirm("确实要删除吗？")) {
            // alert("已经删除！");
            return false;
        } else {
            // alert("已经取消了删除操作");
            return true;
        }
    }else if($("table[id='pre_predict_task_list']").find("input").length=="0"){
        alert("暂无可删的数据！");
        return true;
    }else{
        alert("请先选择需要删除的选项！");
        return true;
    }
}


function delete_predicttask_byid(predict_task_id){
  $.ajax({
    type:"DELETE",
    url:ip + "/api/pre-predict-task/",
    headers: {
       authorization:token,
     },
    dataType:"json",
    async:false,
    data:{'predict_task_id': predict_task_id},
    success:function(json){
     //retrain_list_res = json;
     console.log(json);
     // return json.token;
    },
	error:function(response) {
		redirect(response);
    }
 });
}

function getdelete_similar_picture(delete_similar_picture){
	if(delete_similar_picture == 0){
		return ""
	}else if(delete_similar_picture == 1){
		return "98%";
	}else if(delete_similar_picture ==2 ){
		return "97%";
	}
}

function getdelete_no_label_picture(delete_no_label_picture){
	if(delete_no_label_picture == 0){
		return "";
	}else if(delete_no_label_picture == 1){
		return "删除空白图片";
	}else if(delete_no_label_picture ==2 ){
		return "删除空白图片及数据集中原始图片";
	}
}

function display_pre_predict_task_list(){

  var html="<tr>\
            <th></th>\
            <th id=\"predicttask_head\"></th>\
            <th>自动标注任务名称</th>\
            <th>数据集名称</th>\
            <th>算法模型</th>\
			<th>标注前删除大于相似度的图片</th>\
			<th>自动标注后图片删除</th>\
			<th>删除低于指定得分的标注</th>\
            <th>任务归属者</th>\
            <th>任务开始时间</th>\
            <th>任务状态</th>\
			<th>操作</th>\
            </tr>";
  if(!isEmpty(tableData)){
    for (var i=0;i<tableData.length;i++){
        var row = "<tr>\
            <td><input type=\"checkbox\" class=\"flat-grey list-child\"/></td>\
            <td id=\"predicttask_id\">"+tableData[i].id+"</td>\
            <td>"+tableData[i].task_name+"</td>\
            <td>"+tableData[i].dataset_name+"</td>\
            <td>"+tableData[i].alg_model+"</td>\
			<td>"+getdelete_similar_picture(tableData[i].delete_similar_picture)+"</td>\
			<td>"+ getdelete_no_label_picture(tableData[i].delete_no_label_picture) + "</td>\
			<td>"+tableData[i].score_threshhold+"</td>\
            <td>"+tableData[i].user+"</td>\
            <td>"+tableData[i].task_start_time+"</td>\
            <td>" + getStatusDesc(tableData[i].task_status,tableData[i].task_status_desc)  + "</td>\
			<td>"+
			
			getHtml(tableData[i].task_status,tableData[i].id);
			
			+"</td>\
            </tr>";
        html=html+row;
    }
  }
  console.log(html);
  document.getElementById('pre_predict_task_list').innerHTML=html;


  $('#pre_predict_task_list tr').find('td:eq(1)').hide();
  $('#pre_predict_task_list tr').find('th:eq(1)').hide();
  
  setIntervalToDo();
}

var timeId;

function setIntervalToDo(){
	var isNeedToSetInterval = false;
	if(!isEmpty(tableData)){
        for (var i=0;i<tableData.length;i++){
		    if(tableData[i].task_status == "1" || tableData[i].task_status == "3"){
				console.log("有任务在进行中。需要自动刷新。");
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


function getStatusDesc(task_status,task_status_desc){
	var re = "";
    if(task_status == "0") {
		re = "完成";
	}else if(task_status == "1") {
		re = "进行中";
	}else if(task_status == "2"){
		re = "异常";
	}else if(task_status == "3"){
		re = "排队中";
	}
	if(!isEmpty(task_status_desc)){
		re = re +"," + task_status_desc
	}
	return re;
}


function getHtml(task_status, task_id){
	if(task_status == "0"){
		return "<a onclick=\"sessionStorage.setItem(\'predict_task_id\',\'"+task_id +"\'); window.location.href=\'predictTaskPreview.html\'; \" class=\"btn btn-xs btn-success\">查看结果</a>"
	}else{
		return "<a onclick=\"sessionStorage.setItem(\'predict_task_id\',\'"+task_id +"\'); \" class=\"btn btn-xs btn-fail\">查看结果</a>"
	}
}


function page(current,pageSize){
  pre_predict_task_list(current,pageSize);
  display_pre_predict_task_list();
  setPage(tablePageData,pageSize);
  sessionStorage.setItem('predict_task_page',current);
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
var tmpCurrent = sessionStorage.getItem("predict_task_page");
if(isEmpty(tmpCurrent)){
	tmpCurrent = 0;
}
page(tmpCurrent,pageSize);
