var token = getCookie("token");
var ip =  getIp();


var tableData;
var tablePageData;
var pageSize = 10;




function log_list(startPage,pageSize){
    $.ajax({
       type:"GET",
       url:ip + "/api/queryLogSecInfo/",
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
      },
	  error:function(response) {
		  redirect(response);
      }
   });
}


function display_list(){

  var html="<tr>\
            <th></th>\
            <th id=\"log_head\"></th>\
            <th>用户名称</th>\
            <th>操作类型</th>\
			<th>操作信息</th>\
            <th>操作时间</th>\
            </tr>";
   if (isEmpty(tableData)){
    return;
   }         
  for (var i=0;i<tableData.length;i++){
    var row = "<tr>\
            <td><input type=\"checkbox\" class=\"flat-grey list-child\"/></td>\
            <td id=\"user_id\">"+tableData[i].id+"</td>\
            <td>"+tableData[i].user_name+"</td>\
            <td>"+tableData[i].oper_name+"</td>\
            <td>"+tableData[i].log_info+"</td>\
            <td>"+tableData[i].oper_time_start+"</td>\
            </tr>";
    html=html+row;
  }
  console.log(html);
  document.getElementById('logDataTable').innerHTML=html;


  $('#logDataTable tr').find('td:eq(1)').hide();
  $('#logDataTable tr').find('th:eq(1)').hide();

  
}




function page(current,pageSize){
  log_list(current,pageSize);
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