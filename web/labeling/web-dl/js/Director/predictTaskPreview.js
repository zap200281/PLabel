

var img=new Image();
var imginfo = {};
var ip = getIp();
var token = getCookie("token");
console.log("token=" + token);
var previewtaskid = sessionStorage.getItem("predict_task_id");
console.log("previewtaskid=" + previewtaskid);
var datasetInfo;

canvas = document.getElementById("myCanvas");
context = canvas.getContext("2d");
// canvas.width = document.getElementById("myCanvas").offsetWidth;
// canvas.height = document.getElementById("myCanvas").offsetWidth/1280*720;

canvas.width = document.getElementById("win_canvas").offsetWidth;
canvas.height = document.getElementById("win_canvas").offsetWidth/1280*720;

var color_dict = {"car":"#0099CC", "person":"#FF99CC","point":"#00cc00","pointselected":"red"};
var color_person = {"0":"#13c90c","1":"#fc0707","2":"#FF99CC","3":"#fceb07"};

var rects=[];
var masks=[];
var pointShapes =[];

var fileindex =0;
var lastindex=false;
var labeltastresult;

var pageSize = 12;
var tableData;
var tablePageData;

page(0,pageSize);


if(!isEmpty(labeltastresult)){
	if(labeltastresult.length == 1){
		console.log("pic_image_field=" + labeltastresult[0].pic_image_field);
		var tmpVideoName= labeltastresult[0].pic_image_field;
		if(tmpVideoName.substring(tmpVideoName.length - 4) ==".mp4"){//视频预览
			files  = ip + tmpVideoName;//"/minio/label-img/1586502176987-test111.mp4";
            console.log("files:",files);
			var filename = files.substring(files.lastIndexOf("/") + 1);
			$("#videofilename").text(filename);
			document.getElementById("myVideo_div").style.display="block";
			document.getElementById("myCanvas_div").style.display="none";
            document.getElementById("myBigImg_div").style.display="none";
            $("#myVideo").attr("src", files);
			
		}
		else{//图片预览
		    document.getElementById("myVideo_div").style.display="none";
			document.getElementById("myCanvas_div").style.display="block";
            document.getElementById("myBigImg_div").style.display="none";
            loadimg();
            drawimage();
		}
	}
  else{//图片预览
	    document.getElementById("myVideo_div").style.display="none";
	    document.getElementById("myCanvas_div").style.display="block";
        document.getElementById("myBigImg_div").style.display="none";
        loadimg();
        drawimage();
	}
	
}




img.onload = function(){
	
    canvas.width = document.getElementById("win_canvas").offsetWidth;
    canvas.height = document.getElementById("win_canvas").offsetWidth/1280*720;
    //调整画布大小
    if ((img.width/img.height)<(canvas.width/canvas.height)){
      canvas.width=canvas.height * img.width / img.height;
    }
    else{
      canvas.height=canvas.width * img.height / img.width;
    }
	drawimage();
 }

function point(x,y){
    this.x = x;
    this.y = y;
    this.isSelected = false;
};

function pointShape(x,y,type,score=1.0){
	this.x = x;
    this.y = y;
    this.isSelected = false;
	this.type = type;
	this.score = score;
	this.id =""; //标识
	this.blurred=false;//模糊不清的; 记不清的; 难以区分的; 模棱两可的
	this.goodIllumination = false; //照明
	this.frontview = false;//正面图
}

function rectar(x1,y1,x2,y2, type, score=1.0){
    // this.x = x;
    // this.y = y;
    // this.width = width;
    // this.height = height;
    this.type = type;
    this.score = score;
    //0--1,
    //|  |
    //2--3
    this.points = [new point(x1,y1), new point(x1, y2),new point(x2, y1),new point(x2, y2)];
    this.getXYWH = function(){
      var x_min=Math.min(this.points[0].x,this.points[1].x,this.points[2].x,this.points[3].x);
      var x_max=Math.max(this.points[0].x,this.points[1].x,this.points[2].x,this.points[3].x);
      var y_min=Math.min(this.points[0].y,this.points[1].y,this.points[2].y,this.points[3].y);
      var y_max=Math.max(this.points[0].y,this.points[1].y,this.points[2].y,this.points[3].y);
      return [x_min,y_min,x_max-x_min,y_max-y_min];
    }
    this.getX1Y1X2Y2 = function(){
      var x_min=Math.min(this.points[0].x,this.points[1].x,this.points[2].x,this.points[3].x);
      var x_max=Math.max(this.points[0].x,this.points[1].x,this.points[2].x,this.points[3].x);
      var y_min=Math.min(this.points[0].y,this.points[1].y,this.points[2].y,this.points[3].y);
      var y_max=Math.max(this.points[0].y,this.points[1].y,this.points[2].y,this.points[3].y);
      return [x_min,y_min,x_max,y_max];
    }
    this.getdiapid = function(pid){//获取对角点
      var twooverlapped,fouroverlapped;
      for (var i=0;i<4;i++){
        if ((this.points[pid].x!=this.points[i].x)&&(this.points[pid].y!=this.points[i].y)){
          return i;
        }
        if ((this.points[pid].x!=this.points[i].x)||(this.points[pid].y!=this.points[i].y)){
           twooverlapped=i;
        }
        if (i!=pid) fouroverlapped=i;

      }
      if (twooverlapped)
        return twooverlapped;
      return fouroverlapped;
    }
    this.mouseonpoint = false;
    this.mouseonrect = false;
    this.isSelected = false;
	this.id =""; //标识
	this.blurred=false;//模糊不清的; 记不清的; 难以区分的; 模棱两可的
	this.goodIllumination = true; //照明
	this.frontview = true;//正面图
};



function maskar(x0,y0,type){
  this.type = type;
  this.points = [new point(x0,y0)];
  this.finish = false;
  this.mouseonpoint = false;
  this.mouseonmask = false;
  this.isSelected = false;
  this.getX1Y1 = function(){return [this.points[0].x,this.points[0].y]}
  this.getBound = function(){
    mlen = this.points.length;
    var minX = 999999999, minY = 999999999, maxX = -1, maxY = -1;
    for (var i = 0; i < mlen; i ++){
      if(minX > this.points[i].x){
        minX = this.points[i].x;
      }
      if(maxX < this.points[i].x){
        maxX = this.points[i].x;
      }
      if(minY > this.points[i].y){
        minY = this.points[i].y;
      }
      if(maxY < this.points[i].y){
        maxY = this.points[i].y;
      }
    }
    return [minX, minY, maxX, maxY];
  }
  
  this.id =""; //标识
  this.blurred=false;//模糊不清的; 记不清的; 难以区分的; 模棱两可的
  this.goodIllumination = true; //照明
  this.frontview = true;//正面图
}


function loadimg(){
	img.src  = ip + labeltastresult[fileindex].pic_image_field;
	var fname = tableData[fileindex].pic_image_field.substring(tableData[fileindex].pic_image_field.lastIndexOf('/') + 1);
    $("#filename").text(fname);  
}

function clickfilelist(index){
  fileindex=index;
  loadimg();
  drawimage();
  showfilelist();
}

function clickNext(){
	if(fileindex<tableData.length-1)  {
		next();
	}else{
		if((tablePageData.current + 1) * pageSize >= tablePageData.total){
			return;
		}
		nextPage();
	}
}


function next(){

  if(fileindex<tableData.length-1)  {fileindex=fileindex+1;}
  loadimg();
  drawimage();
  showfilelist();
}

function clickLast(){
	if(fileindex == 0){
		prePage();
	}else{
		last();
	}
}

function last(){

  if(fileindex>0)  {fileindex=fileindex-1;} 
  loadimg();
  drawimage();
  showfilelist();  
}


function drawimage() {
	
	parse_labelinfo(labeltastresult[fileindex].label_info);
	
   // 清除画布，准备绘制
    context.clearRect(0, 0, canvas.width, canvas.heigth);
     // modal_context.cleararc 
     

    context.drawImage(img,0,0,canvas.width, canvas.height);
    
     for(var i=0; i<rects.length; i++) {
       var rect = rects[i];
       rectxywh = new Array(4);
       rectxywh_tmp = rect.getXYWH();
       rectxywh[0] = rectxywh_tmp[0] / canvas.width * canvas.width;
       rectxywh[1] = rectxywh_tmp[1] / canvas.height * canvas.height;
       rectxywh[2] = rectxywh_tmp[2] / canvas.width * canvas.width;
       rectxywh[3] = rectxywh_tmp[3] / canvas.height * canvas.height;
       // 绘制矩形

       context.lineWidth = 3;
       
       if(rect.type == "person"){
         context.strokeStyle = color_person[ i % 4];
       }else{
         context.strokeStyle=color_dict[rect.type];   
       }
       
       context.strokeRect(rectxywh[0],rectxywh[1],rectxywh[2],rectxywh[3]);
       context.font = "15px Georgia";
       context.fillStyle= context.strokeStyle;

       context.fillText(rect.type, rectxywh[0],rectxywh[1]-5);
       for(var j=0; j<4; j++){ 
         var p_tmp = rect.points[j];
         var p = new point(0,0);
         p.x = p_tmp.x/ canvas.width * canvas.width;;
         p.y = p_tmp.y / canvas.height * canvas.height;
         context.fillStyle = color_dict["point"];
         context.fillRect(p.x-3,p.y-3,6,6);
       }
     }
 
     
     for (var i=0; i<masks.length; i++){
	   console.log("start to draw mask.");
       context.strokeStyle="purple"
       var mask =masks[i];
       context.lineWidth = 1;
       for (var j=1; j<mask.points.length; j++){
         
         context.beginPath();
         context.moveTo(mask.points[j-1].x, mask.points[j-1].y);
         context.lineTo(mask.points[j].x,mask.points[j].y);
         context.stroke();
         // modal_context.closePath();
       }
       context.moveTo(mask.points[mask.points.length-1].x,mask.points[mask.points.length-1].y);
       context.lineTo(mask.points[0].x,mask.points[0].y);
       context.stroke();
       context.closePath();
       
       for (var j=0; j<mask.points.length; j++){
         var p = mask.points[j]
         context.fillStyle = color_dict["point"];
         context.fillRect(p.x-3,p.y-3,2,2);
       }
     }
 }
 
 
 //显示文件列表
function showfilelist(){
    var htmlstr="";
    for (var i=0;i<labeltastresult.length;i++){
       var fname = labeltastresult[i].pic_image_field.substring(labeltastresult[i].pic_image_field.lastIndexOf('/') + 1);
       var lablebg=" style=\"cursor:pointer\"";
       if (i==fileindex){lablebg=" style=\"background:#eee;color:#5a5a5a;cursor:pointer;\"";}
       htmlstr = htmlstr+"<tr onclick=\"clickfilelist("+i+");\""+ lablebg+"><td>"+ fname+ "</td></tr>";
    };
    document.getElementById("filelist").innerHTML=htmlstr;
}
 

 
 
function list(current,pageSize){
	$.ajax({
       type:"GET",
       url:ip + "/api/pre-predict-task-item-page/",
       headers: {
          authorization:token,
        },
       dataType:"json",
	   data:{
		   'predictTaskId':previewtaskid,
		   'startPage':current,
		   'pageSize':pageSize
		   },
       async:false,
       success:function(json){
        tablePageData = json;
        tableData = json.data;
		labeltastresult = tableData;
		fileindex=0;
		if(lastindex){
			fileindex = pageSize - 1;
		}
        console.log(json);
        // return json.token;
      },
	   error:function(response) {
		  redirect(response);
       }
   });
}

function getCanvasLocationX(num){
	return Math.round(num * canvas.width/parseInt(img.width));
}

function getCanvasLocationY(num){
	return Math.round(num * canvas.height/parseInt(img.height));
}

function parse_labelinfo(labelinfo){
	rects.length = 0;
	masks.length = 0;
	pointShapes.length = 0;
	if(!isEmpty(labelinfo)){
		console.log(labelinfo);
		var label_arr = JSON.parse(labelinfo);
		console.log(label_arr);
		
		for(var i=0;i<label_arr.length;i++){
		  if(!isEmpty(label_arr[i].mask)){
			  console.log("start to parse mask.");
			  cls=label_arr[i].class_name;
			  var tmpMask = new maskar(getCanvasLocationX(label_arr[i].mask[0]),getCanvasLocationY(label_arr[i].mask[1]),cls);
			  
			  for(var j = 2; j < label_arr[i].mask.length; j+=2){
				  tmpMask.points.push(new point(getCanvasLocationX(label_arr[i].mask[j]),getCanvasLocationY(label_arr[i].mask[j+1])));
			  }
			  
			  if(!isEmpty(label_arr[i].id)){
			    tmpMask.id= label_arr[i].id;
		      }
		      if(!isEmpty(label_arr[i].blurred)){
			     tmpMask.blurred = label_arr[i].blurred;
		      }
		      if(!isEmpty(label_arr[i].goodIllumination)){
			      tmpMask.goodIllumination = label_arr[i].goodIllumination;
		      }
		      if(!isEmpty(label_arr[i].frontview)){
			     tmpMask.frontview = label_arr[i].frontview;
		      }
			  tmpMask.finish = true;
		      masks.push(tmpMask);
			  
		  }else if(!isEmpty(label_arr[i].box)){
			  x1 = getCanvasLocationX(label_arr[i].box[0]);
			  y1 = getCanvasLocationY(label_arr[i].box[1]);
		      x2 = getCanvasLocationX(label_arr[i].box[2]);
		      y2 = getCanvasLocationY(label_arr[i].box[3]);
		      cls=label_arr[i].class_name;
		      score = label_arr[i].score;
		      rect = new rectar(x1,y1,x2,y2,cls,score);
		      if(!isEmpty(label_arr[i].id)){
			    rect.id= label_arr[i].id;
		      }
		      if(!isEmpty(label_arr[i].blurred)){
			     rect.blurred = label_arr[i].blurred;
		      }
		      if(!isEmpty(label_arr[i].goodIllumination)){
			      rect.goodIllumination = label_arr[i].goodIllumination;
		      }
		      if(!isEmpty(label_arr[i].frontview)){
			     rect.frontview = label_arr[i].frontview;
		      }
		      rects.push(rect);
		 }else if(!isEmpty(label_arr[i].keypoints)){
		    cls=label_arr[i].class_name;
		    score = label_arr[i].score;
			var pointShapeObj = new pointShape(getCanvasLocationX(label_arr[i].keypoints[0]),getCanvasLocationY(label_arr[i].keypoints[1]),cls,score);
			pointShapes.push(pointShapeObj);
		 }
	    }
		console.log(rects);
		console.log(masks);
	}
} 




function page(current,pageSize){
  list(current,pageSize);
  showfilelist();
  //loadimg();
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
  if(startIndex < 10){
	  $('#startIndex').text(" " + (startIndex));
  }else{
	  $('#startIndex').text(startIndex);
  }
  var endIndex = pageData.current * pageSize + pageData.data.length;
  if(endIndex < 10){
	   $('#endIndex').text(" " + (endIndex));
  }else{
	   $('#endIndex').text(endIndex);
  }
 
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
 
 

