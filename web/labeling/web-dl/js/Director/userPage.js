var token = getCookie("token");
var ip =  getIp();


var tableData;
var tablePageData;
var pageSize = 10;


function create_user(userName,password,nickName){
	console.log(token)

  var email = $('#email').val();
  var address = $('#address').val();
  var mobile = $('#mobile').val();
  var company = $('#company').val();
  var usertype = $('#user_type option:selected').val();
    $.ajax({
       type:"POST",
       contentType:'application/json',
       url: ip + "/api/addUser/",
       dataType:"json",
       async:false,
       headers: {
          // Accept: "text/html; q=1.0", 
          authorization:token,
        },
       // enctype:"multipart/form-data",
       data:JSON.stringify(
            {'username':userName,
             'nick_name':nickName,
             'password':password,
			 'is_superuser':usertype,
             'email':email,
             'address':address,
             'mobile':mobile,
             'company':company
           }),
       success:function(res){
        console.log(res)
        if(res.code == 0){
          alert("添加用户成功。");
		  result =true;
        }else{
           alert("添加用户失败。" + res.message);
		   result = false;
           return -1;
        }
      },
	   error:function(response) {
		  redirect(response);
       }
   });
}

var result;
function submit_create_user(){
    var userName = $('#userName').val();
    console.log("userName=" + userName);
    if(isEmpty(userName)){
      alert("用户名不能为空。");
      return;
    }
    
    var password = $('#password').val();
    if(isEmpty(password)){
      alert("密码不能为空。");
      return;
    }

    var nickName = $('#nickName').val();
    if(isEmpty(nickName)){
      alert("呢称不能为空。");
      return;
    }
    var mobile= $('#mobile').val();
    if (isEmpty(mobile)){
      alert("电话号码不能为空");
      return;
    }
    var reg = /(1[0-9]\d{9}$)/;
    if (mobile.length != 11 || !reg.test(mobile)) {
          alert("移动电话有误！");
          return;
    }
	
    result = true;
    create_user(userName,password,nickName);
    if (!result){
      return;
    }

    user_list(0,pageSize);
	$("#createUser").modal('hide');
	
    display_list();
  
}

function user_list(startPage,pageSize){
    $.ajax({
       type:"GET",
       url:ip + "/api/queryUserPage/",
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
        document.getElementById("message").style.display = "block";
        document.getElementById("messageInfo").style.display = "none";
		redirect(response);
      }
   });
}

function delete_user(){
   var stop = del();
  if (stop){
    return;
  }
  var Check = $("table[id='userDataTable'] input[type=checkbox]:checked");//在table中找input下类型为checkbox属性为选中状态的数据
        Check.each(function () {//遍历
              var row = $(this).parent("td").parent("tr");//获取选中行
              var id = row.find("[id='user_id']").html();//获取name='Sid'的值
              delete_user_byid(id);
          });
  user_list(0,pageSize);
  display_list();
}

function del(){
    if($("table[id='userDataTable'] input[type=checkbox]").is(":checked")) {
        if (confirm("确实要删除吗？")) {
            // alert("已经删除！");
            return false;
        } else {
            // alert("已经取消了删除操作");
            return true;
        }
    }else if($("table[id='userDataTable']").find("input").length=="0"){
        alert("暂无可删的数据！");
        return true;
    }else{
        alert("请先选择需要删除的选项！");
        return true;
    }
}

function delete_user_byid(user_id){
  $.ajax({
    type:"DELETE",
    url:ip + "/api/deleteUser/",
    headers: {
       authorization:token,
     },
    dataType:"json",
    async:false,
    data:{'userId': user_id},
	
    success:function(json){
     console.log(json);
	 if(json.code == 0){
        console.log("删除用户成功。");
     }else{
        alert("删除用户失败。" + json.message);
        return -1;
     }
   },
   error:function(response) {
		  redirect(response);
   }
 });
}

function display_list(){

  var html="<tr>\
            <th></th>\
            <th id=\"user_head\"></th>\
            <th>用户名称</th>\
            <th>用户呢称</th>\
			<th>用户类型</th>\
            <th>Email</th>\
            <th>地址</th>\
            <th>移动电话</th>\
            <th>公司</th>\
            <th>上次登录时间</th>\
			<th>操作</th>\
            </tr>";
   if (isEmpty(tableData)){
    return;
   }         
  for (var i=0;i<tableData.length;i++){
    var row = "<tr>\
            <td><input type=\"checkbox\" class=\"flat-grey list-child\"/></td>\
            <td id=\"user_id\">"+tableData[i].id+"</td>\
            <td>"+tableData[i].username+"</td>\
            <td>"+tableData[i].nick_name+"</td>\
			<td>"+ getUserType(tableData[i].is_superuser) + "</td>\
            <td>"+tableData[i].email+"</td>\
            <td>"+tableData[i].address+"</td>\
            <td>"+tableData[i].mobile+"</td>\
            <td>"+tableData[i].company+"</td>\
            <td>"+tableData[i].last_login+"</td>\
			 <td>"+
            "<a onclick=\"modify_password(\'" + i + "\');\" class=\"btn btn-xs btn-success\">修改密码</a>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;<a onclick=\"modifyUserExtendFuncTableName(\'" + i + "\');\" class=\"btn btn-xs btn-success\">设置</a>"
			+"</td>\
            </tr>";
    html=html+row;
  }
  console.log(html);
  document.getElementById('userDataTable').innerHTML=html;


  $('#userDataTable tr').find('td:eq(1)').hide();
  $('#userDataTable tr').find('th:eq(1)').hide();

  
}

function getUserType(usertype){
	if(usertype == 0){
		return "管理员";
	}else if(usertype == 1){
		return "标注人员";
	}else if(usertype == 2){
		return "审核人员";
	}
	return "其它";
}

function modify_password(index){
	
	$('#hide_user_id').val(tableData[index].id);
	
	$("#modifyPasswordModal").modal('show');
	
}

function modifyUserExtendFuncTableName(index){
	
	$('#hide_user_id_1').val(tableData[index].id);
	 $(function() {
         $('#funcTableNameSelect').fSelect();
     });
	$("#modifyUserExtendFuncTableName").modal('show');

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


function submit_modifyUserExtendFuncTableName(){
	var user_id = $('#hide_user_id_1').val();
	var funcTableNameSelect = getmultiplyvalue('funcTableNameSelect');
	
	$.ajax({
       type:"POST",
       url: ip + "/api/updateUserExtendTableName/",
       dataType:"json",
       async:false,
       headers: {
          authorization:token,
        },
       data:{
		   'user_id':user_id,
           'funcTableName':funcTableNameSelect
           },
       success:function(res){
        console.log(res);
      },
	   error:function(response) {
		  redirect(response);
       }
   });
   $("#modifyUserExtendFuncTableName").modal('hide');
}


function modify_sigle_password(){
	var user_id = -1;
	 $.ajax({
       type:"GET",
       url:ip + "/api/queryUserIdByToken/",
       headers: {
          authorization:token,
        },
       dataType:"json",
	   data:{},
       async:false,
       success:function(json){
        user_id = json;
        console.log(json);
       },
	   error:function(response) {
	 	  redirect(response);
       }
     });
	 
    console.log('user_id='+ user_id);
	if(user_id == -1){
		alert("用户登录信息错误，不能修改密码。请退出重新登录后再试。");
		return;
	}
	
	$('#hide_user_id').val(user_id);
	console.log('2222');
	$("#modifyPasswordModal").modal('show');
	
}

function submit_modify_password(){
	var user_id = $('#hide_user_id').val();
	var new_password_1 = $('#new_password_1').val();
	var old_password = $('#old_password').val();
	var new_password_2 = $('#new_password_2').val();
	if(new_password_1 != new_password_2){
		alert("两次密码不一样。");
		return;
	}
	$.ajax({
       type:"POST",
       url: ip + "/api/updateUserPassword/",
       dataType:"json",
       async:false,
       headers: {
          authorization:token,
        },
       data:{'user_id':user_id,
             'newPassword':new_password_1,
			 'oldPassword':old_password
           },
       success:function(res){
        console.log(res)
        if(res.code == 0){
            console.log("修改密码成功。");
        }else{
           alert("修改用户密码失败。" + res.message);
           return -1;
        }
      },
	   error:function(response) {
		  redirect(response);
       }
   });
}



function page(current,pageSize){
  user_list(current,pageSize);
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
    var goNum = $('#goNum').val();

    var pageTotal = $("#totalNum").text();
    var pageNum = parseInt(pageTotal/pageSize);
    if(pageTotal%pageSize!=0){
        pageNum += 1;
    }else {
        pageNum = pageNum;
    }
    if (e.keyCode == 13) {
        if (goNum<=0){
          alert("请输入大于0的数值");
        }
        else if(goNum<=pageNum){
            goPage();
        }
        else{
            alert("不能超出总页码！");
        }
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