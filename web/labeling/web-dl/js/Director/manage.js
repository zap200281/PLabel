var token = getCookie("token");
var ip = getIp();


//login();
var tableData,label_task_list_res;
var uploadres;

// console.log(token);
function login(){
  $.ajax({
       type:"POST",//112.56:8000
       url:ip + "/api/api-jwt-auth/",
       dataType:"json",
       async:false,
       data:{'username':"zhaiyunpeng",
             'password':"pcl123456"},
       success:function(json){
        //userinfo=json;
        token = "JWT "+json.token;
        sessionStorage.setItem("token",token);
        // return json.token;
      }
   });
};
function pre_predict_task_create(task_name, zip_object_name, zip_bucket_name, user=2, alg_model=1){
    console.log(zip_object_name)
    console.log(zip_bucket_name)
	console.log(token)

	console.log(JSON.stringify({'task_name':task_name,
             'zip_object_name':zip_object_name,
             'zip_bucket_name':zip_bucket_name,
             'alg_model':1,
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
             'zip_object_name':zip_object_name,
             'zip_bucket_name':zip_bucket_name,
             'alg_model':1,
           }),
       success:function(res){
        alert("开始自动标注!")
        console.log(res)
        // return json.token;
      }
   });
}


 

function uploadfile(){
    var file = document.getElementById('exampleInputFile').files[0];
    if (!file){alert("请先选择文件！");return;}
    var formdata = new FormData();
    formdata.append("files", file);
    $.ajax({ 
        url: ip + "/api/common-file-upload/", 
        // url:"http://label.pcl-ai.ml/api/common-file-upload/", 
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
          $("#upload").attr({ disabled: "disabled" });
          $("#upload")[0].innerHTML="正在上传"
            // alert("正在上传...请等候。"); 
        },
        complete: function () {
          $("#upload").removeAttr("disabled");
          $("#upload")[0].innerHTML="文件上传"
        },
        success:function(res){ 
          if(res){ 
            alert("上传成功！预检任务就绪。"); 
          } 
          uploadres=res;
          pre_predict_task_create(uploadres[0].object_name+"预检",uploadres[0].object_name,uploadres[0].bucket_name);
          pre_predict_task_list();
          display_pre_predict_task_list();
          console.log(res); 
        }, 
        error:function(err){ 
            alert("网络连接失败,稍后重试",err); 
        } 
    })

};
function uploadfileforpred(){
    var file = document.getElementById('preInputFile').files[0];
    var formdata = new FormData();
    formdata.append("files", file);
    $.ajax({ 
        url:ip + "/api/common-file-upload/", 
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
        },
        complete: function () {
          // $("#upload").removeAttr("disabled");
          $("#uploadstate")[0].innerHTML="上传完成";
        },
        success:function(res){ 
          if(res){
             console.log(res);			  
            uploadres=res;
          }           
        }, 
        error:function(err){ 
		    console.log(err);
            alert("网络连接失败,稍后重试",err); 
        } 
    })
}
function submit_predtask(){
  pre_predict_task_create($("#predtaskname").val(),uploadres[0].object_name,uploadres[0].bucket_name);
  pre_predict_task_list();
  display_pre_predict_task_list();
  console.log(res);
}
function pre_predict_task_list(){
    $.ajax({
       type:"GET",
       url:ip + "/api/pre-predict-task/",
       headers: {
          authorization:token,
        },
       dataType:"json",
	   //data:{'startPage':0,
       //      'pageSize':10},
       async:false,
       success:function(json){
        tableData = json;
        console.log(json);
        // return json.token;
      }
   });
}

function delete_predicttask(){
  var Check = $("table[id='pre_predict_task_list'] input[type=checkbox]:checked");//在table中找input下类型为checkbox属性为选中状态的数据
        Check.each(function () {//遍历
              var row = $(this).parent("td").parent("tr");//获取选中行
              var id = row.find("[id='predicttask_id']").html();//获取name='Sid'的值
              delete_predicttask_byid(id);
          });
  pre_predict_task_list();
  display_pre_predict_task_list();
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
   }
 });
}

function display_pre_predict_task_list(){

  var html="<tr>\
            <th></th>\
            <th id=\"predicttask_head\"></th>\
            <th>自动标注任务名称</th>\
            <th>Zip包对象名称</th>\
            <th>算法模型</th>\
            <th>任务归属者</th>\
            <th>任务开始时间</th>\
            <th>任务状态</th>\
            <th>操作</th>\
            </tr>";
  for (var i=0;i<tableData.length;i++){
    var row = "<tr>\
            <td><input type=\"checkbox\" class=\"flat-grey list-child\"/></td>\
            <td id=\"predicttask_id\">"+tableData[i].id+"</td>\
            <td>"+tableData[i].task_name+"</td>\
            <td>"+tableData[i].zip_object_name+"</td>\
            <td>"+tableData[i].alg_model+"</td>\
            <td>"+tableData[i].user+"</td>\
            <td>"+tableData[i].task_start_time+"</td>\
            <td>"+tableData[i].task_status+"</td>\
            <td>"+"<a href=\"#labelModal\" data-toggle=\"modal\" onclick=\"fastcreatelabel("+i+");\" class=\"btn btn-xs btn-success\">创建人工校验</a>"+"</td>\
            </tr>";
    html=html+row;
  }
  console.log(html);
  document.getElementById('pre_predict_task_list').innerHTML=html;


  $('#pre_predict_task_list tr').find('td:eq(1)').hide();
  $('#pre_predict_task_list tr').find('th:eq(1)').hide();

}
function fastcreatelabel(predtaskid){
  $("#labeltaskname").attr({value:tableData[predtaskid].task_name+"-校验"});
  display_createlabel(predtaskid);
  // location.href='#labelModal';
}
function display_createlabel(sindex=-1){
  if(sindex==-1){
  var html="<option value=\"\" selected=\"\">请选择</option>";
  }
  for (var i=0;i<tableData.length;i++){
    if (i==sindex){
    var row = "<option value=\""+tableData[i].id+
              "\" selected=\"\">"+tableData[i].task_name+
              "</option>";
    }else{
    var row = "<option value=\""+tableData[i].id+
        "\">"+tableData[i].task_name+
        "</option>";
      }
    html=html+row;
  }
  console.log(html);
  document.getElementById('pre_predict_task_for_label').innerHTML=html; 
  document.getElementById('pre_predict_task_for_retrain').innerHTML=html; 
}
function submit_labeltask(){
  // console.log($('#labeltaskname').val());
  label_task_create($('#labeltaskname').val(), $('#pre_predict_task_for_label option:selected').val(),);
}
function label_task_create(task_name, pre_predict_task, user=2,){

    $.ajax({
       type:"POST",
       contentType:'application/json',
       url:ip + "/api/label-task/",
       dataType:"json",
       async:false,
       headers: {
          // Accept: "text/html; q=1.0", 
          authorization:token,
        },
       // enctype:"multipart/form-data",
       data:JSON.stringify({'task_name':task_name,
                            'user':user,
                            'pre_predict_task':pre_predict_task,//task id
           }),
       success:function(res){
        alert("标注任务创建成功!")
        console.log(res)
        // return json.token;
      }
   });
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
        label_task_list_res = json;
        console.log(json);
        // return json.token;
      }
   });
}

function delete_labeltask(){
  var Check = $("table[id='label_task_list'] input[type=checkbox]:checked");//在table中找input下类型为checkbox属性为选中状态的数据
        Check.each(function () {//遍历
              var row = $(this).parent("td").parent("tr");//获取选中行
              var id = row.find("[id='labeltask_id']").html();//获取name='Sid'的值
              delete_labeltask_byid(id);
          });
  label_task_list();
  display_label_task_list();
}

function delete_labeltask_byid(label_task_id){
  $.ajax({
    type:"DELETE",
    url:ip + "/api/label-task/",
    headers: {
       authorization:token,
     },
    dataType:"json",
    async:false,
    data:{'label_task_id': label_task_id},
    success:function(json){
     //retrain_list_res = json;
     console.log(json);
     // return json.token;
   }
 });
}


function display_label_task_list(){

  var html="<tr>\
            <th></th>\
            <th id=\"labeltask_head\"></th>\
            <th>人工标注任务名称</th>\
            <th>关联的自动任务</th>\
            <th>任务归属者</th>\
            <th>任务开始时间</th>\
            <th>任务状态</th>\
            <th>操作</th>\
            </tr>";
  for (var i=0;i<label_task_list_res.length;i++){
    var row = "<tr>\
            <td><input type=\"checkbox\" class=\"flat-grey list-child\"/></td>\
            <td id=\"labeltask_id\">"+label_task_list_res[i].id+"</td>\
            <td>"+label_task_list_res[i].task_name+"</td>\
            <td>"+label_task_list_res[i].pre_predict_task+"</td>\
            <td>"+label_task_list_res[i].user+"</td>\
            <td>"+label_task_list_res[i].task_add_time+"</td>\
            <td>"+label_task_list_res[i].task_status+"</td>\
            <td>"+"<a onclick=\"sessionStorage.setItem(\'label_task\',\'"+label_task_list_res[i].id+"\'); window.location.href=\'labeling.html\';\" class=\"btn btn-xs btn-success\">开始人工校验</a>"+"</td>\
            </tr>";
    html=html+row;
  }
  console.log(html);
  document.getElementById('label_task_list').innerHTML=html;

  $('#label_task_list tr').find('td:eq(1)').hide();
  $('#label_task_list tr').find('th:eq(1)').hide();
}

function submit_retraintask(){
  retrain_task_create($("#retrainTaskName").val(),$('#pre_predict_model_for_retrain option:selected').val(),$('#pre_predict_task_for_retrain option:selected').val(),);
  retrain_task_list();
  display_retrain_task_list();
  console.log(retrain_list_res);
}

function retrain_task_create(task_name, alg_model, pre_predict_task_id){

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
                         'pre_predict_task':pre_predict_task_id,//task id
        }),
    success:function(res){
     alert("重训任务创建成功!")
     console.log(res)
   }
});

}

function retrain_task_start(retrain_task_id){

  $.ajax({
    type:"POST",
    contentType:'application/json',
    url:ip + "/api/retrain-task-oper/",
    dataType:"json",
    async:false,
    headers: {
       // Accept: "text/html; q=1.0", 
       authorization:token,
     },
    // enctype:"multipart/form-data",
    data:{'retrain_task_id': retrain_task_id},
    success:function(res){
     alert("重训任务开始!")
     console.log(res)
   }
});

}

function delete_retraintask(){

      var Check = $("table[id='retrain_task_list'] input[type=checkbox]:checked");//在table中找input下类型为checkbox属性为选中状态的数据
      Check.each(function () {//遍历
            var row = $(this).parent("td").parent("tr");//获取选中行
            var id = row.find("[id='retrain_id']").html();//获取name='Sid'的值
            delete_retraintask_byid(id);
        });

     retrain_task_list();
     display_retrain_task_list();
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
   }
 });


}


function retrain_task_list(){
  $.ajax({
   type:"GET",
   url:ip + "/api/retrain-task/",
   headers: {
      authorization:token,
    },
   dataType:"json",
   async:false,
   success:function(json){
    retrain_list_res = json;
    console.log(json);
    // return json.token;
  }
});
}


function display_retrain_task_list(){

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
  for (var i=0;i<retrain_list_res.length;i++){
    var row = "<tr>\
            <td><input type=\"checkbox\" class=\"flat-grey list-child\"/></td>\
            <td id=\"retrain_id\">"+retrain_list_res[i].id+"</td>\
            <td>"+retrain_list_res[i].task_name+"</td>\
            <td>"+retrain_list_res[i].pre_predict_task+"</td>\
            <td>"+retrain_list_res[i].user+"</td>\
            <td>"+retrain_list_res[i].alg_model+"</td>\
            <td>"+retrain_list_res[i].task_start_time+"</td>\
            <td>"+retrain_list_res[i].task_status+"</td>\
            <td>"+"<a onclick=\"sessionStorage.setItem(\'retrain_task\',\'"+retrain_list_res[i].id+"\'); window.location.href=\'retrain.html\';\" class=\"btn btn-xs btn-success\">查看结果</a>"+"</td>\
            </tr>";
    html=html+row;
  }
  console.log(html);
  document.getElementById('retrain_task_list').innerHTML=html;

  $('#retrain_task_list tr').find('td:eq(1)').hide();
  $('#retrain_task_list tr').find('th:eq(1)').hide();

  //$('#retrain_task_list tr').find('retrain_id').hide();
  //$('#retrain_task_list tr').find('retrain_head').hide();

}

pre_predict_task_list();
display_pre_predict_task_list();
label_task_list();
display_label_task_list();
display_createlabel();

retrain_task_list();
display_retrain_task_list();
window.onload = function() {
  // var token = login();
  };