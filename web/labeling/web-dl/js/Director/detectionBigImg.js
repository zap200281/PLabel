//


var regions;
var img=new Image();
var imginfo = {};
var ip = getIp();
var token = getCookie("token");

var label_task_id   = getSessionStorageMessage("label_task_id");
var label_task_name = getSessionStorageMessage("label_task_name");
var zip_object_name = getSessionStorageMessage("zip_object_name");
var dataset_id = getSessionStorageMessage("dataset_id");
var label_task={'id':label_task_id, "zip_object_name":zip_object_name, "dataset_id":dataset_id, "task_name" : label_task_name};


console.log("token=" + token);
var loadFinished=false; //判断是否加载完成，防止上下张快捷键过快，导致保存冲刷掉原有数据，保存空数据
var currentImage=true;


function getSessionStorageMessage(key){
	var value = sessionStorage.getItem(key);
	if(isEmpty(value)){
		return localStorage.getItem(key);
	}
	return value;
}

//获取元素绝对位置 获取鼠标坐标
function getElementLeft(element){
    var actualLeft = element.offsetLeft;
    var current = element.offsetParent;
    while (current !==null){
        actualLeft += (current.offsetLeft+current.clientLeft);
        current = current.offsetParent;
    }
    return actualLeft;
}

function getElementTop(element){
    var actualTop = element.offsetTop;
    var current = element.offsetParent;
    while (current !== null){
        actualTop += (current.offsetTop+current.clientTop);
        current = current.offsetParent;
    }
    return actualTop;
}

// canvas 矩形框集合
//by yunpeng zhai
//变量定义//变量重置
//
var verifyMap = {"1":"大小不合格","2":"颜色不合格","3":"其它不合格"};

var label_task_info;
var labeltastresult;

var rects=[];
var masks=[];
var pointShapes =[];
var copyrects;//复制变量
var copymasks;//复制使用的变量
var copyPointShapes;
var undoShape;

var color_dict = {"rect":"#13c90c","car":"#0099CC", "person":"#FF99CC","point":"#00cc00","pointselected":"red"};
var color_person = {"0":"#13c90c","1":"#fc0707","2":"#FF99CC","3":"#fceb07"};
var color_all={"0":"#13c90c","1":"#fc0707","2":"#FF99CC","3":"#fceb07","4":"#FF33FF","5":"#666600","6":"#4B0082","7":"#B8860B","8": "#000000","9":"#800000","10": "#FF00FF","11":"#8B4513","12":" #0000CD","13":" #008B8B","14":"#708090","15": "#00FFFF","16":"#FF00FF","17":"#9370DB","18":"#7CFC00","19":"#F08080","20":"#C71585"};
var SelectedRect;
var SelectedpointId;
var Selectedmask;
var SelectedPointShape;

var creatingmask;
var SelectedmaskpointId;
var tocreaterect=false;
var tocreatemask=false;
var toCreatePoint = false;
var boxcls = "person";
var mouseonpoint_mask;
var mouseonpoint_rect;
var mouseonrect_rect;
var mouseonmask_mask;
var old_click_x1;
var old_click_y1;
var stretch=false;
var stretch_mask=false;
var isDragging = false;
var showpopVar = false;
var widthstart,widthend;
var heightstart,heightend;

var dragging = false;
var startx = 0;
var starty = 0;
var imgScale = 1;

var fileindex=0;
var lastindex=false;

var isLabelChange = false;
var maxIdNum=0;

function reset_var(){
    SelectedRect=null;
    SelectedpointId=null;
	SelectedmaskpointId = null;
	Selectedmask = null;
	SelectedPointShape = null;
    creatingmask = null;
	toCreatePoint = false;
    tocreaterect=false;
	tocreatemask = false;
    boxcls = "person";
	mouseonpoint_mask = null;
    mouseonpoint_rect=null;
    mouseonrect_rect=null;
    mouseonmask_mask=null;
    old_click_x1=null;
    old_click_y1=null;
    stretch=false;
	stretch_mask = false;
	isDragging = false;
	showpopVar = false;
    widthstart,widthend=null;
    heightstart,heightend=null;
	imgScale = 1;
	dragging = false;
	startx = 0;
	starty = 0;
}

var label_attributes ;

//by yunpeng zhai
//基本对象的定义 点， 矩形， 多边形MASK
//
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
	this.other = {};//自定义属性
    this.other['region_attributes'] = {};
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
	this.other = {};//自定义属性 
    this.other['region_attributes'] = {};
	//{}  ->  key = "type" , value = {"type":"dropdown,radio,checkbox",  "description" : "..", "options":{"car" : "car", "person" : "person"}, "default_options" : {"car" : "car"}}
	 //        key = "color", value = {"type" : "text", "description" : "..", "def":"11111"}
};



function maskar(x0,y0,type,score=1.0){
  this.type = type;
  this.points = [new point(x0,y0)];
  this.finish = false;
  this.mouseonpoint = false;
  this.mouseonmask = false;
  this.isSelected = false;
  this.score = score;
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
  this.other = {};//自定义属性
  this.other['region_attributes'] = {};
}

function getShowShape(recttype){
	var tmp;
	if(recttype == "rect"){
		tmp = rects;
	}else if(recttype == "mask"){
		tmp = masks;
	}else if(recttype == "pointshape"){
		tmp = pointShapes;
	}
	return tmp;
}



function showPopup(topx,lefty,rectIndex,recttype) {

  if (topx > labelwindow.height){
    topx = labelwindow.height;
  }
  if (lefty > labelwindow.width){
    lefty = labelwindow.width;
  }
  _via_display_area_content_name = VIA_DISPLAY_AREA_CONTENT_NAME.IMAGE;

  regions = getShowShape(recttype);
  if(isEmpty(regions)){
    return;
  }
  _via_is_region_selected = true;
  annotation_editor_show(topx,lefty,rectIndex,recttype);
    // 弹框下拉焦点消失
  $("select").bind("focus", function(){
    if(this.blur){
      this.blur();
    }
  }); 
  
 }

//关闭弹窗函数，设置display为none，传一个参数，把true和false传进去
function hidePopup() {
  // document.getElementById("annotation_editor").style.display = "none";
  annotation_editor_remove();

  for (var i=0;i<rects.length;i++){
    rects[i].isSelected = false;  
  }

    // showPopup(bigLocY(x)+145, bigLocX(y) + 235,rectIndex,shapetype);
  for (var i=0;i<masks.length;i++){
      masks[i].isSelected = false;
  }


  for (var i=0;i<pointShapes.length;i++){
      pointShapes[i].isSelected = false;
  }

  drawRect();
  // document.getElementById("boxlabels").innerHTML=boxlabelshtml();
  // document.getElementById("labelcounttable").innerHTML=boxlabelcounthtml();

}


function copyOneBox(){
  copyrects=[];
  copymasks=[];
  copyPointShapes=[];
  for (var i=0;i<rects.length;i++){
    if (rects[i].isSelected==true){
        copyrects.push(rects[i]);
    }
  }

    // showPopup(bigLocY(x)+145, bigLocX(y) + 235,rectIndex,shapetype);
  for (var i=0;i<masks.length;i++){
      if (masks[i].isSelected==true){
        copymasks.push(masks[i]);
      }
  }


  for (var i=0;i<pointShapes.length;i++){
      if (pointShapes[i].isSelected==true){
        copyPointShapes.push(pointShapes[i]);
      }
  }
   currentImage=true;
}

function copy(){
  // copyrects=[];
  // copymasks=[];
  // copyPointShapes=[];
	copyrects = rects.slice();
  copymasks = masks.slice();
	copyPointShapes = pointShapes.slice();
  console.log("拷贝数据："+JSON.stringify(copyrects));
  currentImage=true;
}

function paste(){
    if(currentImage==true){
        alert("当前页暂不能粘贴");
        return;
    }
	if(!isEmpty(copyrects)){
		var tmpRects = copyrects.slice();
		// copyrects.length = 0;
		for(var i = 0; i <tmpRects.length; i++){
            if (maxIdNum< parseInt(tmpRects[i].other["region_attributes"]["id"])){
                  maxIdNum = parseInt(tmpRects[i].other["region_attributes"]["id"]);
            }
            else{
                tmpRects[i].other["region_attributes"]["id"] = maxIdNum+1;
                tmpRects[i].id = maxIdNum+1;
                maxIdNum = maxIdNum +1;
            }
            tmpRects[i].isSelected=false;
			rects.push(tmpRects[i]);
		}
        console.log("粘贴数据："+JSON.stringify(rects));
	}
	if(!isEmpty(copymasks)){
		var tmpMasks = copymasks.slice();
		// copymasks.length = 0;
		for(var i = 0; i <tmpMasks.length; i++){
      if (maxIdNum< parseInt(tmpMasks[i].other["region_attributes"]["id"])){
          maxIdNum = parseInt(tmpMasks[i].other["region_attributes"]["id"]);
      }
      else{
        tmpMasks[i].other["region_attributes"]["id"] = maxIdNum+1;
        tmpMasks[i].id = maxIdNum+1;
        maxIdNum = maxIdNum +1;
      }
      tmpMasks[i].isSelected=false;
			masks.push(tmpMasks[i]);
		}
	}
	if(!isEmpty(copyPointShapes)){
		var tmpPointShapes = copyPointShapes.slice();
		// copyPointShapes.length = 0;
		for(var i = 0; i <tmpPointShapes.length; i++){
      if (maxIdNum< parseInt(tmpPointShapes[i].other["region_attributes"]["id"])){ //不会出现重复的框
          maxIdNum = parseInt(tmpPointShapes[i].other["region_attributes"]["id"]);
      }
      else{
        tmpPointShapes[i].other["region_attributes"]["id"] = maxIdNum+1; //会出现重复的目标框
        tmpPointShapes[i].id = maxIdNum+1;
        maxIdNum = maxIdNum +1;
      }
      tmpPointShapes[i]=false;
			pointShapes.push(tmpPointShapes[i]);
		}
	}
	drawRect();
  updateLabelHtml();
  currentImage=true;
}


function undo(){//删除矩形框或者mask
  hidePopup();
  if(undoShape){
    var index = rects.indexOf(undoShape);
    if (index!=-1){
      rects.splice(index, 1);
    }
    index = masks.indexOf(undoShape);
    if (index!=-1){
      masks.splice(index, 1);
    }
    index = masks.indexOf(undoShape);
    if (index!=-1){
      pointShapes.splice(index, 1);
    }
  }

  drawRect();
  updateLabelHtml();
}

function updateLabelHtml(){
   document.getElementById("boxlabels").innerHTML=boxlabelshtml();
     document.getElementById("labelcounttable").innerHTML=boxlabelcounthtml();
}


//边栏
//任务信息
function showtaskinfo(){
 var file_name = (label_task.zip_object_name).split("/");
 $('#task_file').text(file_name[file_name.length-1]);
 $('#task_info').text(label_task.task_name);
}

function updateSetting(){
  hidePopup();
  // show_region_attributes_update_panel();
  var set_attributes = document.getElementById("set_attributes")
  set_attributes.setAttribute('style', 'top:' + 185 + 'px;left:' + 50 + 'px;width:'+ 502+'px;position:absolute');
  update_attributes_update_panel();
  $('#message_panel').css('display','block');
  $('#message_panel .content').css('display','inline');
}


function save_attribute(){
  var set_attributes = document.getElementById("set_attributes");

  var _via_attributes_str = JSON.stringify(_via_attributes['region']);
  update_labeltask(_via_attributes_str);

  set_attributes.style.display = "none";
  document.getElementById('message_panel').style.display = "none";
  document.getElementById('message_panel_content').style.display = "none";
}

function close_attribute(){
   set_attributes.style.display = "none";
   onload();
   document.getElementById("user_input_attribute_id").value='';
   document.getElementById('attribute_properties').innerHTML = '';
   document.getElementById('attribute_options').innerHTML = '';
   // document.getElementById("type_color").style.display = "none";
   // document.getElementById("type_car").style.display = "none";
   document.getElementById('message_panel').style.display = "none";
   document.getElementById('message_panel_content').style.display = "none";
}


function close_exist_child_attributes(){
    var set_attributes = document.getElementById("atttibute_child");
    document.getElementById('atttibute_childe').innerHTML = '';
}

function moveAllShape(x,y){
	for (var i=0;i<rects.length;i++){
		moverect(rects[i],x,y);
	}
	for (var i=0;i<masks.length;i++){
		moveMask(masks[i],x,y);
	}
	for(var i =0; i < pointShapes.length;i++){
	  pointShapes[i].x += x;
	  pointShapes[i].y += y;
    }
	drawRect();
}

function moveLeftOnePx(){
	moveAllShape(-1,0);
}

function moveRightOnePx(){
	moveAllShape(1,0);
}

function moveUpOnePx(){
	moveAllShape(0,-1);
}

function moveDownOnePx(){
	moveAllShape(0,1);
}

function moveSingleShape(x,y) {
    if(SelectedRect) {
        var index = rects.indexOf(SelectedRect);
        moverect(rects[index], x, y);
    }
    if(Selectedmask){
        var index = masks.indexOf(Selectedmask);
        moveMask(masks[index], x, y);
    }
    if(SelectedPointShape) {
        var index = pointShapes.indexOf(SelectedPointShape);
        pointShapes[index].x += x;
        pointShapes[index].y += y;
    }
    drawRect();
}

function moveLeftSinglePx() {
    moveSingleShape(-1,0);
}
function moveRightSinglePx(){
    moveSingleShape(1,0);
}

function moveUpSinglePx(){
    moveSingleShape(0,-1);
}

function moveDownSinglePx(){
    moveSingleShape(0,1);
}
function createRectLabel(){  
	createrect(getDefaultType());
}

function getDefaultType(){
	var tmpType = "";
	if(!isEmpty(_via_attributes['region']['type']["default_options"])){
		 if(_via_attributes['region']['type']['type'] == "dropdown" || _via_attributes['region']['type']['type'] == "radio" || _via_attributes['region']['type']['type'] == "checkbox"){
			  var tmp = _via_attributes['region']['type']["default_options"];
			  console.log("aa=" + tmp);
			  for(var key in tmp){
			     if(tmp[key] == true){
                   tmpType = key;
                   break;				 
			     }				
		      }
		 }
	 }
	 console.log("defaulttyype=" + tmpType);
	 return tmpType;
}

function createMaskLabel(){
	createmask(getDefaultType());
}

function createPointLabel(){
	createPoint(getDefaultType());
}

function createPoint(classes){
  boxcls = classes;
  toCreatePoint = true;
}

function createrect(classes){
  boxcls = classes;
  tocreaterect = true;
}
function createmask(classes){
  boxcls = classes;
  tocreatemask = true;
}
function cancel(){
  hidePopup();
  tocreaterect=false;
  tocreatemask=false;
  if (creatingmask){
    creatingmask.finish=true;
    creatingmask = null;
  }
  // boxcls = null;
  drawRect();
}
function deleterect(){//删除矩形框或者mask
  hidePopup();
  removeOverlay();
  if(SelectedRect){
    var index = rects.indexOf(SelectedRect);
    rects.splice(index, 1);
    // regions.splice( index, 1);
    SelectedRect=null;
  }
  if(Selectedmask){
    var index = masks.indexOf(Selectedmask);
    masks.splice(index, 1);
    // regions.splice( index+100, 1);
    Selectedmask=null;
  }

  if(SelectedPointShape){
	var index = pointShapes.indexOf(SelectedPointShape);
    pointShapes.splice(index, 1);
    // regions.splice( index+200, 1);
    SelectedPointShape=null;
  }
  drawRect();
  updateLabelHtml();
  save();
}


function deleteAllRect(){
  hidePopup();
  removeOverlay();
  rects.length=0;
  masks.length=0;
  pointShapes.length=0;
  drawRect();
  updateLabelHtml();
  save();

}

function clearCanvas() {
  // 去除所有矩形
  rects = [];
  masks=[];
  pointShapes = [];
  // 重新绘制画布.
  drawRect();
}

//对象高级操作
//
//标注位置校正 移动，拉伸
function moverect(rect, x, y){
  //x y 为拖动坐标增量
  for(var i =0; i < rect.points.length;i++){
	  rect.points[i].x += x;
	  rect.points[i].y += y;
  }
}

function moveMask(mask, x, y){
  //x y 为目标位置的左上角
  for(var i =0; i < mask.points.length;i++){
	  mask.points[i].x += x;
	  mask.points[i].y += y;
  }
}

function stretchmask(mask, pid, x, y){
  console.log("pid=" + pid + " mask.points.length=" + mask.points.length);
  mask.points[pid].x=x;
  mask.points[pid].y=y;
}
function stretchrect(rect, pid, x, y){
  //x y 为所拖动的点的目标位置
  diapid = rect.getdiapid(pid);
  rect.points[pid].x=x;
  rect.points[pid].y=y;
  var first=true;
  for (var j=0;j<4;j++){
    if((j!=diapid)&&(j!=pid)){
      if (first){
        rect.points[j].x = x;
        rect.points[j].y = rect.points[diapid].y;
        first = false;
      }
      else{
        rect.points[j].x = rect.points[diapid].x;
        rect.points[j].y = y;
      }
    }
  };
}

//图片操作
//
//
// function loadimg(){
//   reset_var();
//   var picturePath = labeltastresult[fileindex].pic_image_field;
//   console.log("picture:" + ip + picturePath);
//   img.src = ip + picturePath;
//   var html ="文件名：" + picturePath.substring(picturePath.lastIndexOf("/") + 1) + ", 第" + (tablePageData.current * pageSize + fileindex + 1) + "个文件，共" + tablePageData.total +  "个文件。"
//   document.getElementById("float_text").innerHTML = html;
// }
function save(){
  hidePopup();
  var re = updatelabel(fileindex);
  if (re=true){
    //window.alert("保存成功!");
  }
  else{window.alert("保存失败!");}
}


function clearCache(){
  rects = [];
  masks = [];
  pointShapes = [];
}




//画布操作、
//
//
function onpoint(x,y,pt){
    var biasx = Math.abs(x - pt.x);
    var biasy = Math.abs(y - pt.y);
    if ((biasx<=10) && (biasy<=10)){
      return true;
    }
    else{
      return false;
    }
}
function onrect(x,y,recta){
    rectxywh = recta.getXYWH();
    widthstart=rectxywh[0];
    widthend=rectxywh[0]+rectxywh[2];

    heightstart=rectxywh[1];
    heightend=rectxywh[1]+rectxywh[3];
    if ((x>=widthstart&&x<(widthend))&&(y>=heightstart)&&(y<(heightend))){
      return true;
    }
    else{
      return false;
    }
}

function clearboard(){
  context.clearRect(0, 0, canvas.width, canvas.height);
}

function fillMask(mask){
  context.beginPath();
  context.moveTo(bigLocX(mask.points[0].x), bigLocY(mask.points[0].y));
  for (var i=1; i<mask.points.length; i++){
    context.lineTo(bigLocX(mask.points[i].x), bigLocY(mask.points[i].y));
  }
  context.closePath();
  context.fill();
}


//判断一个点是否在多边形中。
//核心是从这个点出发垂直线或水平线，与多边形相交的次数一定是奇数
function onmask_new(x,y, mask,vs) {

    var vs = mask.points;
 
    var inside = false;
    for (var i = 0, j = vs.length - 1; i < vs.length; j = i++) {
        var xi = vs[i].x, yi = vs[i].y;
        var xj = vs[j].x, yj = vs[j].y;
        
        var intersect = ((yi > y) != (yj > y))
            && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
        if (intersect) inside = !inside;
    }
    
    return inside;
};

function removeOverlay(){
   for (i=0;i<rects.length;i++){
       var id = "overlays-div_"+i;
       viewer.removeOverlay(id); 
    }
    paper.project.clear();
   
}

function drawRect(x=null,y=null,resize=0) {
    removeOverlay();
    // 清楚画布

 //    //遍历画所有矩形框
    drawRectShape();
    //遍历画所有Mask矩形框
   // viewer.
    drawMaskShape(x,y);
    drawPointShape();

    
}


function drawPointShape(){
  
  var radius = 500/zoom;
  for(var i = 0; i < pointShapes.length; i++){

      var circle = new paper.Path.Circle(new paper.Point(pointShapes[i].x, pointShapes[i].y),radius+5);
      circle.strokeColor = "rgba(97, 216, 162)";
     var dict = _via_attributes['region']['type']["options"];
     var color_num=0;
     for (var key in dict){
        if (key==pointShapes[i].other.region_attributes.type){
           circle.strokeColor=color_all[String(color_num)]; 
           break;
        }
        color_num++;
     }

      circle.strokeWidth = radius;

      if(pointShapes[i].isSelected){
        console.log("choose a point.");
        circle.fillColor= "rgba(97, 216, 162, 0.8)";
      // context.fill();
      }

      paper.view.draw();

  }
}
//画所有矩形框
function drawRectShape(){
	
  var drawLineWidth = 1;
  for(var i=0; i<rects.length; i++) {
      var rect = rects[i];
      rectxywh = rect.getXYWH();
	  
	    x1y1x2y2 = rect.getX1Y1X2Y2();
     var tmp_color="rgba(97, 216, 162)";
     var dict = _via_attributes['region']['type']["options"];
     var color_num=0;
     for (var key in dict){
        if (key==rects[i].other.region_attributes.type){
           tmp_color=color_all[String(color_num)]; 
           break;
        }
        color_num++;
     }

	    // var viewportRect = viewer.viewport.imageToViewportRectangle(x1y1x2y2[0],x1y1x2y2[1],rectxywh[2],rectxywh[3]); 
      var elt = document.createElement("div");       //创建一个div对象作为overlays
      elt.id = "overlays-div_"+i;  
      elt.style="border:2px solid "+tmp_color;// rgba(97, 216, 162)";   
      if (rect.isSelected) {
        elt.style="background:rgba(97, 216, 162, 0.7);background-clip:border-box; border:2px solid "+tmp_color;//" rgba(97, 216, 162)";       
      }
      else if(rect.mouseonrect || rect.mouseonpoint){//鼠标移到矩形框中或者在矩形点上
        elt.style="background:rgba(97, 216, 162, 0.2);background-clip:border-box; border:2px solid "+tmp_color;//" rgba(97, 216, 162)";
      }
 
       //执行添加overlay的函数
      viewer.addOverlay({
           element: elt,                              //overlay的元素名称（上面创建的div）
           // location: new OpenSeadragon.Rect(viewportRect.x, viewportRect.y,viewportRect.width,viewportRect.height), //设置overlay在view上的位置
           location: new OpenSeadragon.Rect(x1y1x2y2[0]/bigImgW, x1y1x2y2[1]/bigImgW,rectxywh[2]/bigImgW,rectxywh[3]/bigImgW), //设置overlay在view上的位置
      });



      for(var j=0; j<4; j++){
        var p = rect.points[j]
        if (rect.mouseonpoint){
           var topLeft = new paper.Point(p.x-5, p.y-5);
           var rect_paper = new paper.Rectangle(topLeft, 10);
           var path_rect = new paper.Path.Rectangle(rect_paper, 0);
           path_rect.fillColor =   color_dict["pointselected"];
           paper.view.draw();
        }
        else{
            var topLeft = new paper.Point(p.x-5, p.y-5);
            var rect_paper = new paper.Rectangle(topLeft, 10);
            var path_rect = new paper.Path.Rectangle(rect_paper, 0);
            path_rect.fillColor =   color_dict["point"];
            paper.view.draw();
        }
      }
    }
}





function selectColor(regions,i){
    var dict = _via_attributes['region']['type']["options"];
     var color_num=0;
     for (var key in dict){
        if (key==regions[i].other.region_attributes.type){
           path.strokeColor=color_all[String(color_num)]; 
           break;
        }
        color_num++;
     }
}


function resetSelectShapeEmpty(){

	 if (Selectedmask != null){
        console.log("set select mask is null.");
        if (SelectedmaskpointId != null){
          Selectedmask.points[SelectedmaskpointId].isSelected = false;
          SelectedmaskpointId = null;
        }
        Selectedmask.isSelected = false;
        Selectedmask = null;
    } 
	  if (SelectedRect != null){
		  console.log("set select rect is null.");
          if (SelectedpointId != null){
              SelectedRect.points[SelectedpointId].isSelected = false;
              SelectedpointId= null;
          }
          SelectedRect.isSelected = false;
          SelectedRect = null;
      } 
	  
	  if(SelectedPointShape != null){
		  SelectedPointShape = null;
	  }
}



function clickPointInRect(clickX,clickY){

      // //查看点击的点是否是Rect的标注点，如果是，则着重显示该点。
      for(var i=rects.length-1; i>=0; i--) {
        var rect = rects[i];
        var rectpoints = rect.points
        for (var j=0;j<rectpoints.length;j++){
          var pt = rectpoints[j];
		  //console.log("clickX,clickY=" + clickX + "," + clickY + "   smallLocX,smallLocY=" + smallLocX(clickX) + "," +smallLocY(clickY) + " pt.x,pt.y=" + pt.x + "," + pt.y);
          if (onpoint(clickX,clickY,pt)){
            pt.isSelected=true; 
            rect.isSelected=true;
            SelectedRect = rect;
            SelectedpointId = j;
            stretch = true;//允许拉伸
            console.log("choose rect 3333:"+stretch);
            //更新显示
            drawRect(clickX,clickY);
            //停止搜索
            return true;

          }
        }
      }
      
      for(var i=rects.length-1; i>=0; i--) {
          var rect = rects[i];
          if (onrect(clickX,clickY,rect)){
            SelectedRect = rect;
     
            old_click_x1=clickX;
            old_click_y1=clickY;

            //选择新圆圈
            rect.isSelected = true;
            //允许拖拽
            isDragging = true;
            //更新显示
            drawRect(clickX,clickY)
      	    xy = rect.getX1Y1X2Y2();
      	    console.log("choose rect 2:  top=" + clickX + " left=" + clickY + " x1=" + xy[0] + " y1=" + xy[1] + " x2=" + xy[2] + " y2=" + xy[3] + " startx=" + startx + " starty=" + starty);
            select_only_region(i);
             var point = new OpenSeadragon.Point(xy[2],xy[3]);
             var zoom = viewer.viewport.getZoom();
            var xyweb = viewer.viewport.imageToViewerElementCoordinates(point);
            showPopup(xyweb.y+5, xyweb.x+5 , i,"rect");
      		  // // showPopup(bigLocY(xy[3])+145, bigLocX(xy[2]) + 235,i,"rect");
            document.getElementById("boxlabels").innerHTML=boxlabelshtml();
            document.getElementById("labelcounttable").innerHTML=boxlabelcounthtml();
                  //停止搜索
            return true;
          }
     }
	 return false;
}

function clickPointInMask(clickX,clickY){
  //查看点击的点是否是Mask的标注点，如果是，则着重显示该点。
      for(var i=masks.length-1; i>=0; i--) {
        var mask = masks[i];
        var maskpoints = mask.points
        for (var j=0;j<maskpoints.length;j++){
          var pt = maskpoints[j];
          if (onpoint(clickX,clickY,pt)){
            mask.isSelected=true;
            Selectedmask = mask;
            stretch_mask = true;
            SelectedmaskpointId=j;
            //更新显示
            drawRect(clickX,clickY);
      
            //停止搜索
            return true;
          }
        }
      }

      //检查点击的点是否在某个Mask的范围内，如果是，则该Mask的范围内背景色需要修改，显示该Mask的标注信息
      for(var i=masks.length-1; i>=0; i--) {
          var mask = masks[i];
      
          if (onmask_new(clickX,clickY,mask)){
            Selectedmask = mask;
            console.log("at mask.clickX=" + clickX + " clicky=" + clickY);
            mask.isSelected=true;
            Selectedmask = mask;

            // 允许被拉伸
            stretch_mask =true;
            //允许拖动
            isDragging = true;
      
            old_click_x1=clickX;
            old_click_y1=clickY;
            
            //更新显示
            drawRect(clickX,clickY);
      
            //弹出标注信息框
          var bound = mask.getBound();
            var new_region_id =i;
            regions = masks;
            select_only_region(new_region_id);

            var point = new OpenSeadragon.Point(bound[2],bound[3]);
            var xyweb = viewer.viewport.imageToViewerElementCoordinates(point);
            showPopup(xyweb.y+5, xyweb.x+5 , i,"mask");
            document.getElementById("boxlabels").innerHTML=boxlabelshtml();
            document.getElementById("labelcounttable").innerHTML=boxlabelcounthtml();
      
            //停止搜索
            return true;
          }else{
        //console.log("not at mask.clickX=" + clickX + " clicky=" + clickY);
      }
       }
     return false;
}

function clickPointInPointShape(clickX,clickY){
	var t = -1;
	for(var i = 0; i < pointShapes.length; i++){
		if (onpoint(clickX,clickY,pointShapes[i])){
			console.log("clickX=," + clickX + "clickY=," + clickY + "  pointShapes[i].x=" + + pointShapes[i].x + "pointShapes[i].y=" + pointShapes[i].y);
			pointShapes[i].isSelected=true;
			SelectedPointShape = pointShapes[i];
			t = i;
		}else{
			pointShapes[i].isSelected=false;
		}
	}
	if( t >= 0){
		drawRect(clickX,clickY)
		select_only_region(t);

    var point = new OpenSeadragon.Point(SelectedPointShape.x,SelectedPointShape.y);
    var xyweb = viewer.viewport.imageToViewerElementCoordinates(point);
    showPopup(xyweb.y+5, xyweb.x+5 , t,"pointshape");

		document.getElementById("boxlabels").innerHTML=boxlabelshtml();
		document.getElementById("labelcounttable").innerHTML=boxlabelcounthtml();
		return true;
	}
	return false;
}

mouseTracker = new OpenSeadragon.MouseTracker({
      element: "openseadragon1",
        pressHandler:  evt =>{
          canvasClick(evt);
       },
        exitHandler:  evt =>{
           stopDragging(evt);
       },

        releaseHandler:  evt =>{
          stopDragging(evt);
        },

       moveHandler:  evt =>{
          dragRect(evt);
       },
       dragHandler: evt =>{
          dragRect(evt);
       },
       // dragEndHandler:evt =>{
       //    dragEnd_handler(evt);
       // },


    }).setTracking(true);


function canvasShiftClick(){

   viewer.addHandler('canvas-press', function(event) {
        canvasClick(event);
  });
   viewer.addHandler('canvas-key', function(event) {
            event.preventDefaultAction=true; //canvas禁用键盘操作
  });

}

//交互
//画布内鼠标响应操作
 function canvasClick(event){


      var webPoint = event.position;

      // Convert that to viewport coordinates, the lingua franca of OpenSeadragon coordinates.
      var viewportPoint = viewer.viewport.pointFromPixel(webPoint);

      // Convert from viewport coordinates to image coordinates.
      var imagePoint = viewer.viewport.viewportToImageCoordinates(viewportPoint);


      // Show the results.
      console.log("45544444444444");
      console.log(webPoint.toString(), viewportPoint.toString(), imagePoint.toString());
      
      if (tocreaterect){
          var rect=new rectar(imagePoint.x,imagePoint.y,imagePoint.x,imagePoint.y,boxcls);
          rects.push(rect);   
          console.log("draw rect...10");
          showpopVar = true;
          tocreaterect=false;
          hidePopup();
      }
      if (tocreatemask){
        showpopVar = true;
        hidePopup();
        if (creatingmask == null){
          var mask=new maskar(imagePoint.x,imagePoint.y,boxcls);
          masks.push(mask);
          Selectedmask = mask;
          creatingmask = mask;
        }
        else{
          if (onpoint(imagePoint.x,imagePoint.y,creatingmask.points[0])){
            

            Selectedmask.finish=true;
            creatingmask=null;
            tocreatemask=false;

      
            var new_region_id =masks.length-1;
            var bound = masks[new_region_id].getBound();
            regions = masks;
            drawRect(imagePoint.x,imagePoint.y);
            set_region_annotations_to_default_value( new_region_id );
            select_only_region(new_region_id);
            var pointt = new OpenSeadragon.Point(bound[2],bound[3]);
            var xyweb = viewer.viewport.imageToViewerElementCoordinates(pointt);
            showPopup(xyweb.y+5, xyweb.x+5 , new_region_id,"mask");
            resetSelectShapeEmpty();
            document.getElementById("boxlabels").innerHTML=boxlabelshtml();
            document.getElementById("labelcounttable").innerHTML=boxlabelcounthtml();
            // undoShape = masks[new_region_id];
            return;
          }
          creatingmask.points.push(new point(imagePoint.x,imagePoint.y))
        }
      }

      if(toCreatePoint){
         // console.log("point x,y=" + clickX + "," + clickY);
         var pointObj = new pointShape(imagePoint.x,imagePoint.y,boxcls);
         pointShapes.push(pointObj);
         var len_points = pointShapes.length;
         var new_region_id =len_points-1;
         regions = pointShapes;
         set_region_annotations_to_default_value(new_region_id);
         toCreatePoint = false;
         // undoShape = pointShapes[new_region_id];
      }
      //将选择信息设置为空，在下面重新查找设置
      resetSelectShapeEmpty();

       if(clickPointInPointShape(imagePoint.x,imagePoint.y)){
        return; 
       }
      //如果点在Rect内，已经画完了，则返回。
      if(clickPointInRect(imagePoint.x,imagePoint.y)){
        return;
      }
     //如果点在Mask内，已经画完了，则返回。
     if(clickPointInMask(imagePoint.x,imagePoint.y)){
       return;
     }
      

     drawRect(imagePoint.x,imagePoint.y);
     hidePopup();
   
     dragging = true;
     old_click_x1 =  imagePoint.x;
     old_click_y1 =  imagePoint.y;
 }   



// canvas.onmousemove =dragRect; 

function canvasShiftMouseMove(){
   viewer.addHandler('canvas-drag', function(event) {

     var webPoint = event.position;

      console.log("webPoint45:"+webPoint.toString());
      dragRect(event);
  });
}

function dragRect(event){


    // mouseTracker.setTracking(false);
    var webPoint = event.position;

    // Convert that to viewport coordinates, the lingua franca of OpenSeadragon coordinates.
    var viewportPoint = viewer.viewport.pointFromPixel(webPoint);

    // Convert from viewport coordinates to image coordinates.
    var imagePoint = viewer.viewport.viewportToImageCoordinates(viewportPoint);

    // Show the results.
    console.log(webPoint.toString(), viewportPoint.toString(), imagePoint.toString());
    

    if (tocreaterect || tocreatemask){
      drawRect(imagePoint.x,imagePoint.y);
    }
    //悬停变色
    var found=false;
    for(var i=rects.length-1; i>=0; i--) {
      var rect = rects[i];
      var rectpoints = rect.points
      for (var j=0;j<rectpoints.length;j++){
        var pt = rectpoints[j];
        if (onpoint(imagePoint.x,imagePoint.y,pt)){
          rect.mouseonpoint=true;
          mouseonpoint_rect = rect;             
          //更新显示
          drawRect(imagePoint.x,imagePoint.y);
          //停止搜索
          found=true;
          break;
        }
      }
      if (found){
        break;
      }
    }
    for(var i=masks.length-1; i>=0; i--) {
        var mask = masks[i];
        var maskpoints = mask.points
        for (var j=0;j<maskpoints.length;j++){
          var pt = maskpoints[j];
          if (onpoint(imagePoint.x,imagePoint.y,pt)){
            mask.mouseonpoint=true;
            mouseonpoint_mask = mask;             
            //更新显示
            drawRect(imagePoint.x,imagePoint.y);
            //停止搜索
            found=true;
            break;
          }
        }
        if (found){
          break;
        }
    }


    if ((!found)&& (mouseonpoint_rect!=null)&&(!stretch)) {
      mouseonpoint_rect.mouseonpoint=false;
      mouseonpoint_rect=null;
      drawRect(imagePoint.x,imagePoint.y);
    }
    if ((!found)&& (mouseonpoint_mask!=null)&&(!stretch)) {
        mouseonpoint_mask.mouseonpoint=false;
        mouseonpoint_mask=null;
        drawRect(imagePoint.x,imagePoint.y);
    }

    if (!found){//不在点上
        for(var i=rects.length-1; i>=0; i--) {
          var rect = rects[i];
          if (onrect(imagePoint.x,imagePoint.y,rect)){
            if (mouseonrect_rect) {mouseonrect_rect.mouseonrect=false;}
            rect.mouseonrect=true;
            mouseonrect_rect = rect;             
            //更新显示
            drawRect(imagePoint.x,imagePoint.y);
            //停止搜索
            found=true;
            break;
          }
        }
        for(var i=masks.length-1; i>=0; i--) {
          var mask = masks[i];
          var maskpoints = mask.points
          if (onmask_new(imagePoint.x,imagePoint.y,mask)){
            if (mouseonmask_mask) {mouseonmask_mask.mouseonmask=false;}
            mask.mouseonmask=true;
            mouseonmask_mask = mask;             
            //更新显示
            drawRect(imagePoint.x,imagePoint.y);
            //停止搜索
            found=true;
            break;
          }
        }

        if((!found)&& (mouseonrect_rect!=null)){
          mouseonrect_rect.mouseonrect=false;
          mouseonrect_rect=null;
          drawRect(imagePoint.x,imagePoint.y);
        }
        if((!found)&& (mouseonmask_mask!=null)){
          mouseonmask_mask.mouseonmask=false;
          mouseonmask_mask=null;
          drawRect(imagePoint.x,imagePoint.y);
        }

    }
        // 判断矩形是否开始拖拽
    if (isDragging == true) {
      viewer.panHorizontal=false;
      viewer.panVertical=false
      // 判断拖拽对象是否存在
      if (SelectedRect != null) {
        // 将圆圈移动到鼠标位置
    //console.log("old(" + old_click_x1 + "," + old_click_y1 +") new(" + x + "," + y + ")");
        moverect(SelectedRect, imagePoint.x-old_click_x1, imagePoint.y-old_click_y1);
        old_click_x1 =  imagePoint.x;
        old_click_y1 =  imagePoint.y;
        // 更新画布
         drawRect(imagePoint.x,imagePoint.y);
      }
      else if(Selectedmask != null){
          
          moveMask(Selectedmask, imagePoint.x-old_click_x1, imagePoint.y-old_click_y1);
          old_click_x1 =  imagePoint.x;
          old_click_y1 =  imagePoint.y;

          // 更新画布
          drawRect(imagePoint.x,imagePoint.y);
      }
   
     hidePopup();
    }
              //判断是否开始拉伸
    console.log("stretchstretchstretch:"+stretch);
    if (stretch) {
      viewer.panHorizontal=false;
      viewer.panVertical=false
      stretchrect(SelectedRect, SelectedpointId, imagePoint.x, imagePoint.y);
       drawRect(imagePoint.x,imagePoint.y);
      document.getElementById("boxlabels").innerHTML=boxlabelshtml();
      document.getElementById("labelcounttable").innerHTML=boxlabelcounthtml();

    }
    //判断mask是否开始拉伸
    if (stretch_mask && SelectedmaskpointId != null) {
        viewer.panHorizontal=false;
        viewer.panVertical=false
        stretchmask(Selectedmask, SelectedmaskpointId, imagePoint.x, imagePoint.y);
        drawRect(imagePoint.x,imagePoint.y);
    }

   if(dragging){
      viewer.panHorizontal=true;
      viewer.panVertical=true
      var dx = imagePoint.x- old_click_x1;
      var dy = imagePoint.y - old_click_y1;
     
     startx -= dx;
     starty -= dy;
     console.log("x=" + imagePoint.x + " y=" + imagePoint.y +  " move x=" + dx + " move y=" + dy + " startx=" +startx + ",starty=" + starty);
     drawRect(imagePoint.x,imagePoint.y);
     old_click_x1 = imagePoint.x;
     old_click_y1 = imagePoint.y;
  }
}

function canvasShiftMouseOut(){
   viewer.addHandler('canvas-exit', function(event) {
         stopDragging(event);
  });
}



function canvasShiftMouseUp(){
   viewer.addHandler('canvas-release', function(event) {

       stopDragging(event);
  });
}


function stopDragging(event){
 
             // if (hit_item) {
             //       window.viewer.setMouseNavEnabled(true);
             // }
      // hit_item = null; 
  isDragging = false;
  dragging = false;
  stretch=false;
  stretch_mask=false;
  //console.log("choose rect 10:")
  if (tocreaterect) {
      drawRect(-1,-1);
  }
  if(showpopVar){
      showpopVar= false;
      document.getElementById("boxlabels").innerHTML=boxlabelshtml();
      document.getElementById("labelcounttable").innerHTML=boxlabelcounthtml();
      for(var i=rects.length-1; i>=0; i--) {
            var rect = rects[i];
            if (rect.isSelected){
               xy = rect.getX1Y1X2Y2();
               console.log("create rect show:   x1=" + xy[0] + " y1=" + xy[1] + " x2=" + xy[2] + " y2=" + xy[3] + " startx=" + startx + ",starty=" + starty);
              // wang
              // var original_img_region = new file_region();
              var new_region_id =i;
              regions = rects;
              set_region_annotations_to_default_value( new_region_id );
              select_only_region(new_region_id);
              var point = new OpenSeadragon.Point(xy[2],xy[3]);
              var xyweb = viewer.viewport.imageToViewerElementCoordinates(point);
              showPopup(xyweb.y+5, xyweb.x+5 , i,"rect");
              //停止搜索
              return;
            }
      }

      for(var i=masks.length-1; i>=0; i--) {
          var mask = masks[i];
          if (mask.isSelected && mask.finish){

             var new_region_id =i;
             regions = masks;
             select_only_region(new_region_id);
             set_region_annotations_to_default_value(new_region_id);
             var bound = mask.getBound();
             var point = new OpenSeadragon.Point(bound[2],bound[3]);
             var xyweb = viewer.viewport.imageToViewerElementCoordinates(point);
             showPopup(xyweb.y+5, xyweb.x+5  ,i,"mask");
             console.log("create mask--show...13");
            //停止搜索
            return;
          }
      }


  }
}


function parse_labelinfo(labelinfo){
  	rects.length = 0;

  	if(!isEmpty(labelinfo)){
    		// console.log("标注信息："+labelinfo);
    		var label_arr = JSON.parse(labelinfo);
    		
    		for(var i=0;i<label_arr.length;i++){
          if(!isEmpty(label_arr[i].mask)){
                cls=label_arr[i].class_name;
                var tmpMask = new maskar(label_arr[i].mask[0],label_arr[i].mask[1],cls);
                
                for(var j = 2; j < label_arr[i].mask.length; j+=2){
                    tmpMask.points.push(new point(label_arr[i].mask[j],label_arr[i].mask[j+1]));
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
                if(!isEmpty(label_arr[i].other)){
                  tmpMask.other = label_arr[i].other;
                }else if (isEmpty(label_arr[i].other)){ 
                  tmpMask.other["region_attributes"]["type"] = tmpMask.type;
                  tmpMask.other["region_attributes"]["id"] = tmpMask.id;
                }

                tmpMask.finish = true;
                if (maxIdNum< parseInt(tmpMask.other["region_attributes"]["id"])){
                    maxIdNum = parseInt(tmpMask.other["region_attributes"]["id"]);
                }
                masks.push(tmpMask);
            
          }      
          else if(!isEmpty(label_arr[i].box)){
    			    x1 = label_arr[i].box[0];
    			    y1 = label_arr[i].box[1];
    		      x2 = label_arr[i].box[2];
    		      y2 = label_arr[i].box[3];
    		      cls   =label_arr[i].class_name;
    		      score = label_arr[i].score;
    		      var rect = new rectar(x1,y1,x2,y2,cls,score);
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
    			  
              if(!isEmpty(label_arr[i].other)){
                rect.other = label_arr[i].other;
              }
              // else if (isEmpty(label_arr[i].other) && (isEmpty(label_attributes) || Object.keys(label_attributes).length == 0)){ 
              else if (isEmpty(label_arr[i].other)){ 
                rect.other["region_attributes"]["type"] = rect.type;
                rect.other["region_attributes"]["id"] = rect.id;
              }

              if (maxIdNum< parseInt(rect.other["region_attributes"]["id"])){
                  maxIdNum = parseInt(rect.other["region_attributes"]["id"]);
              }
    		      rects.push(rect);
    		  }
          else if(!isEmpty(label_arr[i].keypoints)){
              cls=label_arr[i].class_name;
              score = label_arr[i].score;
              var pointShapeObj = new pointShape(label_arr[i].keypoints[0],label_arr[i].keypoints[1],cls,score);
              if(!isEmpty(label_arr[i].other)){
                 pointShapeObj.other = label_arr[i].other;
              }
              else if (isEmpty(label_arr[i].other)){
                  pointShapeObj.other["region_attributes"]["type"] = pointShapeObj.type;
                  pointShapeObj.other["region_attributes"]["id"] = pointShapeObj.id;
              }

             if (maxIdNum< parseInt(pointShapeObj.other["region_attributes"]["id"])){
                maxIdNum = parseInt(pointShapeObj.other["region_attributes"]["id"]);
              }
             pointShapes.push(pointShapeObj);
          }

  	   }
  	}
    document.getElementById("boxlabels").innerHTML=boxlabelshtml();
    document.getElementById("labelcounttable").innerHTML=boxlabelcounthtml();

    // 加载值：需要打开已有属性和属性值，需要回传_via_attributes， _via_img_metadata两个值的获取
    //暂时默认设置只有类别属性
    set_display_area_content( VIA_DISPLAY_AREA_CONTENT_NAME.IMAGE );
} 

function getHtmlStr(shape,shapetype,rectIndex){
	 var lablebg="";
     if (shape.isSelected){
		 lablebg=" style=\"background:#eee;color:#5a5a5a;\"";
     var tmp = "tr_" + shapetype + "_" + Math.max(rectIndex-1,0);
     var $objTr = $("#"+tmp); //找到要定位的地方  tr 
     var objTr = $objTr; //转化为dom对象 
      $("#labelpanel").animate({scrollTop:objTr.offsetTop},0); //滚动条滚动

	   }
	 
	 var tmpType = shape.type;
	 //console.log("tmpType =" + tmpType);
	 if(!isEmpty(shape.other["region_attributes"]["type"])){
		 tmpType = shape.other["region_attributes"]["type"];
	 }
	 
	 var shapeswidthHeigth = "";
	 if (shapetype == "bbox"){
		 xywh = shape.getXYWH();
		 shapeswidthHeigth = parseInt(xywh[2]) + "," + parseInt(xywh[3]);
	 }
	 var xy=shape.points;
	 var x ;
	 var y ;
	 if (shapetype == "point"){
		  x = shape.x;
		  y = shape.y;
	  }
	  else{
		  x = xy[0].x;
		  y = xy[0].y;
	  }
      return "<tr id=\"tr_" + shapetype + "_" + rectIndex + "\" onclick=\"highlight("+x+","+y+", "+rectIndex+", '"+shapetype+"');\""  +lablebg  + "><td style=\"width:10px\"></td> <td>"+ tmpType + "</td><td>" + shapetype + "</td><td>" + shapeswidthHeigth + "</td></tr>";
}
function setStyle(rectIndex,shapetype){
	for (var i=0;i<rects.length;i++){
       document.getElementById("tr_bbox_" + i).style = "";
    }
	for (var i=0;i<pointShapes.length;i++){
       document.getElementById("tr_point_" + i).style = "";
    }
	for (var i=0;i<masks.length;i++){
       document.getElementById("tr_mask_" + i).style = "";
    }
	document.getElementById("tr_" + shapetype + "_" + rectIndex).style = "background:#eee;color:#5a5a5a;";
}

function highlight(x,y,rectIndex,shapetype){
  setStyle(rectIndex,shapetype);
  Selectedmask = null;
  SelectedRect = null;
  SelectedPointShape = null;
  if (shapetype=="bbox"){
	for (var i=0;i<rects.length;i++){
      rects[i].isSelected = false;
      if (i==rectIndex){
         rects[i].isSelected = true;
		 SelectedRect = rects[i];
      }
    }
	for (var i=0;i<pointShapes.length;i++){
       pointShapes[i].isSelected = false;
    }
	for (var i=0;i<masks.length;i++){
       masks[i].isSelected = false;
    }
  }
  else if (shapetype=="point"){
	for (var i=0;i<rects.length;i++){
        rects[i].isSelected = false;
    }
	for (var i=0;i<pointShapes.length;i++){
       pointShapes[i].isSelected = false;
       if (i==rectIndex){
         pointShapes[i].isSelected = true;
		 SelectedPointShape = pointShapes[i];
       }
    }
	for (var i=0;i<masks.length;i++){
       masks[i].isSelected = false;
    }
  }
  else{
	SelectedRect = null;
	for (var i=0;i<rects.length;i++){
        rects[i].isSelected = false;
    } 
	for (var i=0;i<pointShapes.length;i++){
       pointShapes[i].isSelected = false;
    }
	for (var i=0;i<masks.length;i++){
       masks[i].isSelected = false;
       if (i==rectIndex){
          masks[i].isSelected = true;
		  Selectedmask = masks[i];
       }
    }
  }
   drawRect(x,y);
}



function boxlabelshtml(){
  var htmlstr="";
  for (var i=0;i<rects.length;i++){
     htmlstr = htmlstr + getHtmlStr(rects[i],'bbox',i);
  }
  for (var i=0;i<masks.length;i++){
    htmlstr = htmlstr + getHtmlStr(masks[i],'mask',i);
  }
  for(var i = 0; i < pointShapes.length; i++){
	 htmlstr = htmlstr + getHtmlStr(pointShapes[i],'point',i);
  }
  return htmlstr;
}

function boxlabelCount(labelMap,shape){
	
	 var tmpType = shape.type;
	 
	 if(!isEmpty(shape.other["region_attributes"]["type"])){
		 tmpType = shape.other["region_attributes"]["type"];
	 }
	
	 if(labelMap[tmpType] == null){
        labelMap[tmpType] = 1;
     }else{
        labelMap[tmpType] = labelMap[tmpType] + 1;
     }
}

function boxlabelcounthtml(){
  var htmlstr="";
  var labelMap = {};
  for (var i=0;i<rects.length;i++){
	 boxlabelCount(labelMap,rects[i]);
  }
  for (var i=0;i<masks.length;i++){
	 boxlabelCount(labelMap,masks[i]);
  }
  for (var i=0;i<pointShapes.length;i++){
	 boxlabelCount(labelMap,pointShapes[i]);
  }
  for (item in labelMap){
    htmlstr = htmlstr + "<tr><td style=\"width:10px\"></td> <td>"+ item + "</td><td>" + labelMap[item] +"</td></tr>";
  }
  return htmlstr;
}



function getRealLocationX(num){
	var loc = Math.round(num*img.width/canvas.width);
  // if (img.width<600){
  //   loc = Math.round(num);
  // }
	if(loc <= 0){
        loc = 1;
    }
	if(loc > img.width){
		loc = img.width -1;
	}
	return loc;
}

function getRealLocationY(num){
	var loc = Math.round(num*img.height/canvas.height);
  // if (img.width<600){
  //   loc = Math.round(num);
  // }
	if(loc <= 0){
        loc = 1;
    }
	if( loc > img.height){
		loc = img.height -1;
	}
	return loc;
}

function getCanvasLocationX(num){
	return Math.round(num*canvas.width/parseInt(img.width));
}

function getCanvasLocationY(num){
	return Math.round(num*canvas.height/parseInt(img.height));
}




var timeId;
var count;
var progress;


function updatelabel(fileindex){
  var label_list=[]
  for (var i=0;i<rects.length;i++){
      x1y1x2y2=rects[i].getX1Y1X2Y2();
      rects[i].type = '';//regions[i].other.["region_attributes"]["type"];
	  var label= {'class_name':rects[i].type, "score":1.0};
	  if(!isEmpty(rects[i].other.region_attributes.type)){
		  label['class_name'] = rects[i].other.region_attributes.type;
	  }
    
    if(!isEmpty(rects[i].other.region_attributes.id)){
      rects[i].id = rects[i].other.region_attributes.id;
    }  
     
	  var xmin = Math.round(x1y1x2y2[0]);
	  var ymin = Math.round(x1y1x2y2[1]);
    var xmax = Math.round(x1y1x2y2[2]);
	  var ymax = Math.round(x1y1x2y2[3]);
      
    label['box']=[xmin,ymin, xmax,ymax];
	  if(isEmpty(rects[i].id)){
	     label['id'] = i+'';
	  }else{
		 label['id'] = rects[i].id+''; 
	  }
	  label['blurred']=rects[i].blurred;
	  label['goodIllumination']=rects[i].goodIllumination;
	  label['frontview']=rects[i].frontview;
      label['other']=rects[i].other;
      console.log(label);
      label_list.push(label);
  }

    for (var i=0;i<masks.length;i++){
      masks[i].type = '';//regions[i].other.["region_attributes"]["type"];
    var label = {'class_name':masks[i].type, "score":1.0};
    if(!isEmpty(masks[i].other.region_attributes.type)){
      label['class_name'] = masks[i].other.region_attributes.type;
    }
    if(!isEmpty(masks[i].other.region_attributes.id)){
      masks[i].id = masks[i].other.region_attributes.id;
    }
    var labelMaskList = [];
    for(var j = 0; j < masks[i].points.length; j++){
      labelMaskList.push(Math.round(masks[i].points[j].x));
      labelMaskList.push(Math.round(masks[i].points[j].y));
    }
    label['mask'] = labelMaskList;
    label['id'] = masks[i].id+'';
    label['blurred']=masks[i].blurred;
    label['goodIllumination']=masks[i].goodIllumination;
    label['frontview']=masks[i].frontview;
    label['other']=masks[i].other;
      console.log(label);
    label_list.push(label);
  }
  
  for (var i=0;i<pointShapes.length;i++){
      pointShapes[i].type =  '';//regions[i].other.["region_attributes"]["type"];
    var label = {'class_name':pointShapes[i].type, "score":1.0};
    
    if(!isEmpty(pointShapes[i].other.region_attributes.type)){
      label['class_name'] = pointShapes[i].other.region_attributes.type;
    }
    if(!isEmpty(pointShapes[i].other.region_attributes.id)){
      pointShapes[i].id = pointShapes[i].other.region_attributes.id;
    }
    var pointList = [];
   
    pointList.push(Math.round(pointShapes[i].x));
    pointList.push(Math.round(pointShapes[i].y));
    pointList.push(2);//0表示这个关键点没有标注（这种情况下x=y=v=0），1表示这个关键点标注了但是不可见(被遮挡了），2 表示这个关键点标注了同时也可见
    label['keypoints'] = pointList;
    label['id'] = pointShapes[i].id+'';
    label['blurred']=pointShapes[i].blurred;
    label['goodIllumination']=pointShapes[i].goodIllumination;
    label['frontview']=pointShapes[i].frontview;
      label['other']=pointShapes[i].other;
      console.log(label);
    label_list.push(label);
  }

  labelinfo_jsonstr = JSON.stringify(label_list);
  labeltastresult.label_info=labelinfo_jsonstr;
  if(label_list.length > 0){ 
        labeltastresult.label_status=0;
  }
  else{
    labeltastresult.label_status=1;
  }
  
  $.ajax({
       type:"PATCH",
       url:ip + "/api/large-picture-task-item",
	   contentType:'application/json',
       headers: {
          authorization:token,
        },
       dataType:"json",
       async:false,
	   data:JSON.stringify({'id':label_task_info.id,
                            'label_info':labelinfo_jsonstr,
                            'label_status': labeltastresult.label_status,
                            'pic_object_name' : bigImgW + "," + bigImgH,
           }),
			 
       success:function(json){
		   return true;
	   },
	   error:function(response) {
		   redirect(response);
       }
   });
  return false;
}




var viewer;
var overlay;
var data;
function getimage(datasetid){
  $.ajax({
     type:"GET",
     url:ip + "/api/getTitleSource",
     headers: {
        authorization:token,
      },
   contentType:'application/json',
     dataType:"json",
   data:{'dateset_id':datasetid},
     async:false,
     success:function(json){
      
      console.log(json);
    
      data = json.Image;
      
      tileSource = {//装载tileSource 
              Image: {
                  xmlns: data.xmlns,
                  Url: ip + data.Url,
                  Overlap:data.Overlap,
                  TileSize: data.TileSize,
                  Format: data.Format,
                  Size: {
                      Height: data.Size.Height,
                      Width: data.Size.Width,
                  }
              },
        success:function(){
         },
	   error:function(response) {
		   redirect(response);
       }

        };
          
        var tmp = viewer.open(tileSource);
        // drawMaskShape(overlay);
      }
       
    });

  viewer.addHandler("open",function() {
    // console.log("图像打开成功");
   });

   overlay = viewer.paperjsOverlay();

}

var path;
var zoom;
function drawMaskShape(x,y){

    var tmp_point = new OpenSeadragon.Point(x, y);
    var webPoint = viewer.viewport.imageToViewerElementCoordinates(tmp_point);
    zoom = viewer.viewport.getZoom(true);
    var minzoom = viewer.viewport.getMinZoom(true);
    var maxzoom = viewer.viewport.getMaxZoom(true);
    var linew = parseInt(230/zoom);
    for (var i=0; i<masks.length; i++){

        path = new paper.Path();

        path.strokeColor = "rgba(97, 216, 162)";
        path.strokeWidth = linew;
 
        var mask =masks[i];
        if (mask.isSelected){
           path.strokeWidth = linew+5;
        }
        else{
           path.strokeWidth = linew +3;
        }

       selectColor(masks,i);
       // var transformed_point = new paper.Point(x, y);
       // console.log("transformed_point:"+x+","+y);

        for (var j=0; j<mask.points.length; j++){
           path.add(new paper.Point(mask.points[j].x, mask.points[j].y));
           paper.view.draw();
        }

        if(mask.finish){

            path.add(new paper.Point(mask.points[0].x, mask.points[0].y));
            paper.view.draw(); 
            // 绘制Mask
            if (mask.isSelected) {
              // context.fillStyle = "rgba(97, 216, 162, 0.5)";
              path.fillColor = "rgba(97, 216, 162, 0.7)";//"rgba(100,150,185,0.8)";
              // fillMask(mask);
            }
            else if(mask.mouseonmask || mask.mouseonpoint){
              path.fillColor = "rgba(97, 216, 162, 0.2)";//"rgba(100,150,185,0.5)";
            }

        }
        else{
          if(x&&y){
            path.add(new paper.Point(x, y));  
            paper.view.draw();            
          }
        }
        for (var j=0; j<mask.points.length; j++){
          var p = mask.points[j]
          if (onpoint(x,y,p)){
             var topLeft = new paper.Point(mask.points[j].x-5, mask.points[j].y-5);
             var rect = new paper.Rectangle(topLeft, 10);
             var path_rect = new paper.Path.Rectangle(rect, 0);
             path_rect.fillColor =  color_dict["pointselected"];
             paper.view.draw();  
          }
          else{

             var topLeft = new paper.Point(mask.points[j].x-5, mask.points[j].y-5);
             var rect = new paper.Rectangle(topLeft, 10);
             var path_rect = new paper.Path.Rectangle(rect, 0);
             path_rect.fillColor =   color_dict["point"];
          }
      }

    }  
}

function showBigImg(){
   
    viewer = OpenSeadragon({
                   id: "openseadragon1",
                   prefixUrl: "js/openseadragon-bin-2.4.2/images/",
                   showNavigator: true,
                   navigatorPosition: "TOP_RIGHT",
                  //  navigatorPosition: "ABSOLUTE",
                  // navigatorTop:      "0px",
                  // navigatorLeft:     width-200,
                  navigatorHeight:   "180px",
                  navigatorWidth:    "280px",
                  panHorizontal:false,
                  panVertical:false,
                  showNavigationControl:false,  
                  zoomPerClick:'1',

                });

    var tileSource = new OpenSeadragon.TileSource;
    getimage(dataset_id);
}


var pageSize = 20;
var tableData;
var tablePageData;
var bigImgW;
var bigImgH;

    // show_region = document.getElementById("show_region");
    // show_region.width =document.getElementById("tool0").offsetWidth;
    // show_region.height = document.getElementById("tool0").offsetWidth/1280*720;

  window.onload = function() {
    
    console.log("onload js\Director\detectionBigImg.js");
	
	var medical_flag = getSessionStorageMessage("medical_flag");
	if(!isEmpty(medical_flag)){
	   $('.left-side').toggleClass("collapse-left");
       $(".right-side").toggleClass("strech");
       document.getElementById("hiddenLeft").style.display="none";
	   document.getElementById("logotitle").style.display="none";
	}
	
	
    var token = getCookie("token");
    if(typeof token == "undefined" || token == null || token == ""){
        console.log("token=" + token);
        window.location.href = "../../login.html";
    }else{
        var nickName = getCookie("nickName");
        console.log("nickName=" + nickName);
        $("#userNickName").text(nickName);
        $("#userNickName_bar").text(nickName);
    }

	label_task_id   = getSessionStorageMessage("label_task_id");
	label_task_name = getSessionStorageMessage("label_task_name");
	zip_object_name = getSessionStorageMessage("zip_object_name");
	dataset_id = getSessionStorageMessage("dataset_id");
	
	console.log("dataset_id=" + dataset_id + "label_task_id=" + label_task_id + " zip_object_name=" + zip_object_name);

    labelwindow = document.getElementById("labelwin");
    labelwindow.width = document.getElementById("tool0").offsetWidth;
    labelwindow.height = document.getElementById("tool0").offsetWidth/1280*720;

    var width = document.getElementById("tool0").offsetWidth;
    document.getElementById("openseadragon1").style.width = width+'px';
    document.getElementById("openseadragon1").style.height = width/1280*720 + 'px';
    if (!isEmpty(viewer)){
          document.getElementById("openseadragon1").innerHTML = "";
    }

    showBigImg();
    canvasShiftClick();
    canvasShiftMouseMove();
    canvasShiftMouseUp();
    canvasShiftMouseOut();

    bigImgW =JSON.parse(data.Size.Width);
    bigImgH = JSON.parse(data.Size.Height);

    showtaskinfo();
    get_labeltask();
    parse_labelinfo(labeltastresult.label_info);  


	  _via_init();

    get_init_atrribute();
  	if(!isEmpty(label_task_info.task_label_type_info)){
  		label_attributes = JSON.parse(label_task_info.task_label_type_info);
  	}
    if (isEmpty(label_attributes)){
        get_init_atrribute();
    }
    else{

      if (Object.keys(label_attributes).length == 0){
          get_init_atrribute();
       }
     else{
        _via_attributes["region"] = label_attributes;
     }

    }
    drawRect();  
  
  };



function get_labeltask(){
      $.ajax({
       type:"GET",
       url:ip + "/api/large-picture-task-item",
       headers: {
          authorization:token,
        },
       dataType:"json",
       data:{
       'label_task':label_task.id,
       },
       async:false,
       success:function(json){
        label_task_info = json[0];
        console.log(label_task_info);
        labeltastresult=label_task_info;
      },
	  error:function(response) {
		redirect(response);
      }
   });
}


    // 加载保存好的属性结构,如果不存在，加载默认的类别信息
    function get_init_atrribute(){
       _via_attributes = {'region':{}};
        var atti = "id"; // 属性名
       _via_attributes['region'][atti] = {};
       _via_attributes['region'][atti]["type"] = "text";
       _via_attributes['region'][atti]["description"] = "";
       _via_attributes['region'][atti]["default_value"] = "";

       atti = "type"; // 属性名
       _via_attributes['region'][atti] = {};
       _via_attributes['region'][atti]["type"] = "dropdown";
       _via_attributes['region'][atti]["description"] = "";
       _via_attributes['region'][atti]["options"] = {};
       _via_attributes['region'][atti]["options"]["1"] = "肿瘤病灶";
       _via_attributes['region'][atti]["options"]["2"] = "肝肿瘤";
       _via_attributes['region'][atti]["options"]["3"] = "肺部肿瘤";
       _via_attributes['region'][atti]["options"]["4"] = "神经胶质肿瘤";
       _via_attributes['region'][atti]["options"]["5"] = "肠道肿瘤";
       _via_attributes['region'][atti]["default_options"] = {};
 
    }
  
  function model_sele_Change(event){
	var modelid = $('#predict_model option:selected').val();
	console.log("predict_model=" + modelid);
	if(modelid == 12){
		document.getElementById("tracking_startid_div").style.display="block";
		document.getElementById("tracking_endid_div").style.display="block";
	}else{
		document.getElementById("tracking_startid_div").style.display="none";
		document.getElementById("tracking_endid_div").style.display="none";
	}
}
  

  function update_labeltask(task_label_type_info){
    // console.log("label_task_id=" + label_task_info.id);
    // console.log("task_label_type_info=" + task_label_type_info);
    
      $.ajax({
         type:"PATCH",
         url:ip + "/api/large-picture-task",
         headers: {
            authorization:token,
          },
         dataType:"json",
         data:{
           'id':label_task.id,
           'task_label_type_info':task_label_type_info,
         },
         async:false,
         success:function(json){
           console.log(json);
         },
	     error:function(response) {
		  redirect(response);
         }
     });
  }



  document.onkeydown = function(e){
      switch(e.keyCode){
          case 37:
          case 38:
          case 39:
          case 40:
            e.preventDefault();
      }
  }
  document.onkeyup=function(e){  
      console.log(e.keyCode)
      console.log(window.event);
      e=e||window.event;  
      e.preventDefault(); 
	  
	  
	  obj = e.srcElement||e.target;
      if( obj != null && obj !=undefined ){
          if(obj.type == "textarea" || obj.type=='text'){
			//console.log("obj.type:"+obj.type);
            return ;
          }
       }
	  
      switch(e.keyCode){  
        case 87: //W
          // boxcls = classes;
          createRectLabel();     
          break; 
        case 68:
          deleterect();
          break;
        case 27:
          cancel();
          break;
        case 83:
          save();
          break;
        case 37:
          moveLeftSinglePx();
          break;
        case 38:
          moveUpSinglePx();
          break;
        case 39:
          moveRightSinglePx();
          break;
        case 40:
          moveDownSinglePx();
          break;
      };
  }
