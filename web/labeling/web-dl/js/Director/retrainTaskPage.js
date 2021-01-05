var token = getCookie("token");
var ip = getIp();

var pageSize = 10;

var tableData;
var tablePageData;

var labelTaskData;
var algModelList;


function setModel(){
  loadModel();
  display_create_retrain_label();
  retrain_type_sele_Change("");
  setLabelDate();
  detection_type_sele_Change("");
  
  label_task_list();
  display_createlabel();
}

function label_task_list(){
  $.ajax({
     type:"GET",
     url:ip + "/api/label-task/",
     headers: {
        authorization:token,
      },
     dataType:"json",
     async:false,
     success:function(json){
      labelTaskData = json;
      console.log(json);
      // return json.token;
     },
	 error:function(response) {
		  redirect(response);
     }
 });
}

function display_createlabel(sindex=-1){
  var html="";
  for (var i=0;i<labelTaskData.length;i++){
    if (i==sindex){
        var row = "<option value=\""+labelTaskData[i].id+
              "\" selected=\"\">"+labelTaskData[i].task_name+
              "</option>";
    }else{
        var row = "<option value=\""+labelTaskData[i].id+
        "\">"+labelTaskData[i].task_name+
        "</option>";
      }
    html=html+row;
  }
  
  document.getElementById('retrain_data').innerHTML=html; 
   $(function() {
                $('#retrain_data').fSelect();
   });
}

function setLabelDate(){
	var date1 = new Date();
	date1.setDate(date1.getDate() - 7);
    var month = date1.getMonth()+1;
    if(month < 10){
       month = "0" + month;
	}		
	
	var day = date1.getDate();
	if(day <10){
		day = "0" + day;
	}
	
	var time = date1.getFullYear()+"-" + month + "-" + day + " 00:00:00";
	$("#label_date").val(time);
}


function display_create_retrain_label(){
  var html="<option value=\"\" selected=\"\">请选择</option>";
  for (var i=0;i<algModelList.length;i++){
        var row = "<option value=\""+algModelList[i].id+
        "\">"+algModelList[i].model_name+
        "</option>";
      
      html=html+row;
  }
  console.log(html);
  document.getElementById('pre_predict_model_for_retrain').innerHTML=html; 
}

function retrain_type_sele_Change(event){
	var retrain_type = $('#retrain_type option:selected').val();
	console.log("retrain_type=" + retrain_type);
	if(retrain_type == 1){
		document.getElementById("detection_type_div").style.display="block";
	}else{
		document.getElementById("detection_type_div").style.display="none";
	}
}

function detection_type_sele_Change(event){
	var detection_type = $('#detection_type option:selected').val();
	console.log("detection_type=" + detection_type);
	if(detection_type == 1){
		document.getElementById("detection_type_input_div").style.display="block";
	}else{
		document.getElementById("detection_type_input_div").style.display="none";
	}
}


function loadModel(){

  $.ajax({
    type:"GET",
    contentType:'application/json',
    url: ip + "/api/queryAlgModelForRetrain/",
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

function setPredictTask(){
 
    setModel();
}



function submit_retraintask(){
   var taskName = $("#retrainTaskName").val();
   if (isEmpty(taskName) || taskName.length > 32){
       alert("重训任务名称不能为空或者不能超过32个字符。");
       return;
   }
   //if (isEmpty($('#pre_predict_model_for_retrain option:selected').val())){
   //   alert("重训任务模型不能为空。")
   //   return;
   //}
   var retrain_type = $("#retrain_type").val();
   var retrain_data = getmultiplyvalue('retrain_data');
   var detection_type = $("#detection_type").val();
   var detection_type_input = $("#detection_type_input").val();
   var retrain_model_name =  $("#retrain_model_name").val();
   var reg = /^[0-9a-zA-Z_]+$/
   if(!reg.test(retrain_model_name)){
      alert("请输入英文字母，数字或者下划线组成的模型名称。");
      return;
   }
   
   var test_train_ratio = $("#test_train_ratio").val();
   
   
   $("#retrainModal").modal('hide');
   
   retrain_task_create(taskName,$('#pre_predict_model_for_retrain option:selected').val(),retrain_type,retrain_data,detection_type,detection_type_input,retrain_model_name,test_train_ratio);
   page(0,pageSize);
   console.log(tableData);
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

function retrain_task_create(task_name, alg_model, retrain_type,retrain_data,detection_type,detection_type_input,retrain_model_name,test_train_ratio){

  $.ajax({
    type:"POST",
    contentType:'application/json',
    url:ip + "/api/retrain-task/",
    dataType:"json",
    async:false,
    headers: {
       // Accept: "text/html; q=1.0", 
       authorization:token,
     },
    // enctype:"multipart/form-data",
    data:JSON.stringify({'task_name':task_name,
                         'alg_model':alg_model,
						 'retrain_model_name':retrain_model_name,
						 'retrain_type':retrain_type,
						 'retrain_data':retrain_data,
						 'detection_type':detection_type,
						 'detection_type_input':detection_type_input,
						 'test_train_ratio':test_train_ratio
    }),
    success:function(res){
       alert("重训任务创建成功!")
       console.log(res)
    },
	error:function(response) {
	   redirect(response);
    }
});

}


function delete_retraintask(){
      var stop = del();
      if (stop){
        return;
      }
      var Check = $("table[id='retrain_task_list'] input[type=checkbox]:checked");//在table中找input下类型为checkbox属性为选中状态的数据
      Check.each(function () {//遍历
            var row = $(this).parent("td").parent("tr");//获取选中行
            var id = row.find("[id='retrain_id']").html();//获取name='Sid'的值
            delete_retraintask_byid(id);
        });

    page(0,pageSize);
}

function del(){
    if($("table[id='retrain_task_list'] input[type=checkbox]").is(":checked")) {
        if (confirm("确实要删除吗？")) {
            // alert("已经删除！");
            return false;
        } else {
            // alert("已经取消了删除操作");
            return true;
        }
    }else if($("table[id='retrain_task_list']").find("input").length=="0"){
        alert("暂无可删的数据！");
        return true;
    }else{
        alert("请先选择需要删除的选项！");
        return true;
    }
}

function delete_retraintask_byid(retrain_task_id){
  $.ajax({
    type:"DELETE",
    url:ip + "/api/retrain-task/",
    headers: {
       authorization:token,
     },
    dataType:"json",
    async:false,
    data:{'retrain_task_id': retrain_task_id},
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


function list(current,pageSize){
  $.ajax({
   type:"GET",
   url:ip + "/api/retrain-task-page/",
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
    // return json.token;
   },
   error:function(response) {
	   redirect(response);
   }
});
}


function display_list(){

  var html="<tr>\
            <th></th>\
            <th id=\"retrain_head\"></th>\
            <th>训练任务名称</th>\
            <th>重训预检任务</th>\
            <th>任务归属者</th>\
            <th>算法模型</th>\
            <th>任务开始时间</th>\
            <th>任务状态</th>\
            <th>操作</th>\
            </tr>";
  for (var i=0;i<tableData.length;i++){
    var row = "<tr>\
            <td><input type=\"checkbox\" class=\"flat-grey list-child\"/></td>\
            <td id=\"retrain_id\">"+tableData[i].id+"</td>\
            <td>"+tableData[i].task_name+"</td>\
            <td>"+tableData[i].pre_predict_task+"</td>\
            <td>"+tableData[i].user+"</td>\
            <td>"+tableData[i].alg_model+"</td>\
            <td>"+tableData[i].task_start_time+"</td>\
            <td>"+tableData[i].task_status+"</td>\
            <td>"+"<a onclick=\"sessionStorage.setItem(\'retrain_task\',\'"+tableData[i].id+"\'); window.location.href=\'retrainResult.html\';\" class=\"btn btn-xs btn-success\">查看结果</a>"+"</td>\
            </tr>";
    html=html+row;
  }
  console.log(html);
  document.getElementById('retrain_task_list').innerHTML=html;

  $('#retrain_task_list tr').find('td:eq(1)').hide();
  $('#retrain_task_list tr').find('th:eq(1)').hide();

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
