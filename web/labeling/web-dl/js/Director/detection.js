//
/**
 * @author zhaiyunpeng
 * @copyright 2019
 * javascript for person and vehicle detection
 */

var regions;
var img=new Image();
var imginfo = {};
var ip = getIp();
var token = getCookieOrMessage("token");
var userType = getCookieOrMessage("userType");
console.log("token=" + token);
var loadFinished=false; //判断是否加载完成，防止上下张快捷键过快，导致保存冲刷掉原有数据，保存空数据
var currentImage=true;

if(userType == 2){
  $('#title_id').text("通用图片审核");
}


function getCookieOrMessage(key){
	var value = getCookie(key);
	
	return value;
}

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
var label_task = getSessionStorageMessage("label_task");
var label_task_status = getSessionStorageMessage("label_task_status");  //1表示审核
console.log("label_task=" + label_task);
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

  if (topx > canvas.height){
    topx = canvas.height;
  }
  if (lefty > canvas.width){
    lefty = canvas.width;
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
  document.getElementById("boxlabels").innerHTML=boxlabelshtml();
  document.getElementById("labelcounttable").innerHTML=boxlabelcounthtml();

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
    index = pointShapes.indexOf(undoShape);
    if (index!=-1){
      pointShapes.splice(index, 1);
    }
  }

  drawRect();
  updateLabelHtml();
}

function updateSetting(){
  hidePopup();
  // show_region_attributes_update_panel();
  var set_attributes = document.getElementById("set_attributes")
  set_attributes.setAttribute('style', 'top:' + 185 + 'px;left:'+ 50 +'px;width:'+ 502+'px;position:absolute');
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
  document.getElementById('message_panel_content').style.display = 'none';
}

function close_attribute(){
   set_attributes.style.display = "none";
   onload();
   document.getElementById("user_input_attribute_id").value='';
   document.getElementById('attribute_properties').innerHTML = '';
   document.getElementById('attribute_options').innerHTML = '';
   document.getElementById("type_color").style.display = "none";
   document.getElementById("type_car").style.display = "none";
   document.getElementById('message_panel').style.display = "none";
   document.getElementById('message_panel_content').style.display = 'none';
}


function close_exist_child_attributes(){
  var set_attributes = document.getElementById("atttibute_child");
    document.getElementById('atttibute_childe').innerHTML = '';

  //   var atttibute_child = document.getElementById('atttibute_child');
  // if ( atttibute_child ) {
  //   atttibute_child.style.display = "none";
  //   // p.remove();
  //   // 
  // }

// set_attributes.style.display = "none";

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
	//$("#hiddenLeft").click();
	
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
function loadimg(){
  reset_var();
  var picturePath = labeltastresult[fileindex].pic_image_field;
  console.log("picture:" + ip + picturePath);
  img.src = ip + picturePath;
  var html ="文件名：" + picturePath.substring(picturePath.lastIndexOf("/") + 1) + ", 第" + (tablePageData.current * pageSize + fileindex + 1) + "个文件，共" + tablePageData.total +  "个文件。"
  document.getElementById("float_text").innerHTML = html;
}
function save(){
  hidePopup();
  var re = updatelabel(fileindex);
  if (re=true){
    //window.alert("保存成功!");
  }
  else{window.alert("保存失败!");}
  showfilelist();
}


function clearCache(){
  rects = [];
  masks = [];
  pointShapes = [];
}

function next(){
  currentImage = false;
  hidePopup();
  if(fileindex<labeltastresult.length-1)  {
    if(loadFinished==true){
        updatelabel(fileindex);
    }
    clearCache();
    fileindex=fileindex+1;	 
    loadimg();
    showfilelist();
  }else{
	  if((tablePageData.current + 1) * pageSize < tablePageData.total){
           if (loadFinished==true){
		      updatelabel(fileindex);
            }
           clearCache();
		   nextPage();
	  }
  }
  loadFinished=false;
}
function last(){  
currentImage = false;  
  hidePopup();
  if(fileindex>0){
      if(loadFinished==true){
         loadFinished=false;   
         updatelabel(fileindex);
      }
	  clearCache();
	  fileindex=fileindex-1;
	  loadimg();
      showfilelist();  
  }else{
	  var current = $('#displayPage1').text();
	  if(current > 1){
	     lastindex = true;
         if(loadFinished==true){
            loadFinished=false;   
		    updatelabel(fileindex);
         }
		 clearCache();
         prePage();
	     lastindex = false;
	  }
  }  
}
function clickfilelist(index){
  hidePopup();
  clearCache();
  fileindex = index;
  loadimg();
  showfilelist();
  currentImage = false;
}


//画布操作、
//
//
function onpoint(x,y,pt){
    var biasx = Math.abs(x - pt.x);
    var biasy = Math.abs(y - pt.y);
    if ((biasx<=3) && (biasy<=3)){
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


function drawRect(x=null,y=null,resize=0) {
    //document.getElementById("boxlabels").innerHTML=boxlabelshtml();
    //document.getElementById("labelcounttable").innerHTML=boxlabelcounthtml();
	

    //清除画布，准备绘制
    context.clearRect(0, 0, canvas.width, canvas.height);

    var imgWidth = canvas.width;
    var imgHeight = canvas.height
    if(imgScale > 1){//当没有放大时，就用画布的大小
		imgWidth = canvas.width * imgScale;
		imgHeight = canvas.height * imgScale;
	}
     
    //console.log("startx=" + startx + " starty=" + starty + " img.width=" + img.width + " imgWidth=" + imgWidth + " imgScale=" + imgScale + " img.width / (canvas.width * imgScale)=" + (img.width / (canvas.width * imgScale)));
	context.drawImage(
        img, //规定要使用的图像、画布或视频。
        startx, starty, //开始剪切的 x 坐标位置。
        img.width, img.height,  //被剪切图像的高度。
		//imgWidth, imgHeight,
        0, 0,//在画布上放置图像的 x 、y坐标位置。
        imgWidth, imgHeight  //要使用的图像的宽度、高度
		//canvas.width,canvas.height
    );
 
    //context.drawImage(img, 0, 0, imgWidth, imgHeight, 0, 0, canvas.width, canvas.height);
    //context.drawImage(img,0,0,canvas.width,canvas.height);
	
    //遍历画所有矩形框
    drawRectShape();
 
    //创建Rect时，显示横竖两条黑线
    if (tocreaterect && (x!=-1)){
      //row
      context.strokeStyle="black"
      context.beginPath();
      context.moveTo(0,y);
      context.lineTo(canvas.width,y);
 
      context.moveTo(x,0);
      context.lineTo(x,canvas.height);
      context.stroke();
      context.closePath();
    }
	//遍历画所有Mask矩形框
    drawMaskShape(x,y);
 
    //遍历画所有的圆点
    drawPointShape();
    
}

function drawPointShape(){
	for(var i = 0; i < pointShapes.length; i++){
		context.strokeStyle="yellow";
        selectColor(pointShapes,i);
		context.beginPath();//标志开始一个路径
		context.arc(bigLocX(pointShapes[i].x),bigLocY(pointShapes[i].y),5,0,2*Math.PI);//在canvas中绘制圆形
		if(pointShapes[i].isSelected){
			console.log("choose a point.");
			context.fillStyle = "rgba(97, 216, 162, 0.5)";
			context.fill();
	    }
        context.stroke()
	}
}

function bigLocX(x){
	var tmp = img.width / (canvas.width * imgScale);
	return x * imgScale - startx/tmp ;
}

function bigLocY(y){
	var tmp = img.height / (canvas.height * imgScale);
	return y * imgScale - starty /tmp;
}

function smallLocX(x){
   var tmp = img.width /canvas.width;
   return x / imgScale + startx/tmp;
}

function smallLocY(y){
   var tmp = img.height /canvas.height;
   return y / imgScale + starty / tmp;
}
	

//画所有矩形框
function drawRectShape(){
	
  var drawLineWidth = 1;
  for(var i=0; i<rects.length; i++) {
      var rect = rects[i];
      rectxywh = rect.getXYWH();
	  
	  x1y1x2y2 = rect.getX1Y1X2Y2();
	  
      // 绘制矩形
      if (rect.isSelected) {
        context.fillStyle = "rgba(97, 216, 162, 0.7)";
        //context.fillStyle = "rgba(10, 114, 6, 0.5)";
        //context.fillRect(bigLocX(rectxywh[0]),bigLocY(rectxywh[1]),bigLocX(rectxywh[2]),bigLocY(rectxywh[3]));
		context.fillRect(bigLocX(x1y1x2y2[0]),bigLocY(x1y1x2y2[1]),bigLocX(x1y1x2y2[2]) - bigLocX(x1y1x2y2[0]),bigLocY(x1y1x2y2[3]) - bigLocY(x1y1x2y2[1]));
        context.lineWidth = drawLineWidth;
      }
      else if(rect.mouseonrect || rect.mouseonpoint){//鼠标移到矩形框中或者在矩形点上
        context.fillStyle = "rgba(97, 216, 162, 0.2)";
        //context.fillStyle = "rgba(10, 114, 6, 0.5)";
        //context.fillRect(bigLocX(rectxywh[0]),bigLocY(rectxywh[1]),bigLocX(rectxywh[2]),bigLocY(rectxywh[3]));
		context.fillRect(bigLocX(x1y1x2y2[0]),bigLocY(x1y1x2y2[1]),bigLocX(x1y1x2y2[2]) - bigLocX(x1y1x2y2[0]),bigLocY(x1y1x2y2[3]) - bigLocY(x1y1x2y2[1]));
        context.lineWidth = drawLineWidth;
      }
      else{
        context.lineWidth = drawLineWidth;
      }

      // if(rect.type == "person"){
      //   context.strokeStyle = color_person[ i % 4];
      // }else{
      context.strokeStyle=color_dict["rect"];   //初始颜色
      // }
      selectColor(rects,i);
      setVerifyLineWidth(rect);
      //context.strokeRect(bigLocX(rectxywh[0]),bigLocY(rectxywh[1]),bigLocX(rectxywh[2]),bigLocY(rectxywh[3]),rect.color);

      context.strokeRect(bigLocX(x1y1x2y2[0]),bigLocY(x1y1x2y2[1]),bigLocX(x1y1x2y2[2]) - bigLocX(x1y1x2y2[0]),bigLocY(x1y1x2y2[3]) - bigLocY(x1y1x2y2[1]),rect.color);

	  setVerifyText(rect,bigLocX(x1y1x2y2[0]),bigLocY(x1y1x2y2[1]));

      for(var j=0; j<4; j++){
        var p = rect.points[j]
        // context.fillStyle = "green";
        // context.fillRect(p.x-3,p.y-3,6,6);
        if (rect.mouseonpoint){
          context.fillStyle = color_dict["pointselected"];
          context.fillRect(bigLocX(p.x)-5,bigLocY(p.y)-5,10,10);
        }
        else{
          context.fillStyle = color_dict["point"];
          context.fillRect(bigLocX(p.x)-3,bigLocY(p.y)-3,6,6);
        }
      }
    }
}



function setVerifyText(rect,x,y){
	if(!isEmpty(rect.other.region_attributes.verify)){
		if(rect.other.region_attributes.verify != 0){
			context.font = "15px Georgia";
			context.fillStyle= context.strokeStyle;
			context.fillText(verifyMap[rect.other.region_attributes.verify], x,y-5);
		}
	}
}
	

function setVerifyLineWidth(rect){
	if(!isEmpty(rect.other.region_attributes.verify)){
		if(rect.other.region_attributes.verify != 0){
			context.lineWidth = 3;
			context.strokeStyle = "#DC143C";
		}
	}
}

function selectColor(regions,i){
    var dict = _via_attributes['region']['type']["options"];
     var color_num=0;
     for (var key in dict){
        if (key==regions[i].other.region_attributes.type){
           context.strokeStyle=color_all[String(color_num)]; 
           break;
        }
        color_num++;
     }
}

function drawMaskShape(x,y){
	   for (var i=0; i<masks.length; i++){
      context.strokeStyle="purple"
      var mask =masks[i];
      if (mask.isSelected){
        context.lineWidth = 3;
      }
      else{
		  context.lineWidth = 2;
      }

      selectColor(masks,i);

      for (var j=1; j<mask.points.length; j++){
        context.beginPath();
        context.moveTo(bigLocX(mask.points[j-1].x), bigLocY(mask.points[j-1].y));
        context.lineTo(bigLocX(mask.points[j].x),bigLocY(mask.points[j].y));
        context.stroke();
        // context.closePath();
      }
      if(mask.finish){
        context.moveTo(bigLocX(mask.points[mask.points.length-1].x), bigLocY(mask.points[mask.points.length-1].y));
        context.lineTo(bigLocX(mask.points[0].x), bigLocY(mask.points[0].y));
        context.stroke();
        context.closePath();
        // 绘制Mask
        if (mask.isSelected) {
          context.fillStyle = "rgba(97, 216, 162, 0.5)";
          fillMask(mask);
        }
        else if(mask.mouseonmask || mask.mouseonpoint){
          context.fillStyle = "rgba(97, 216, 162, 0.2)";
          fillMask(mask);
        }
      }
	  else{
        if(x&&y){
          context.beginPath();
          context.moveTo(bigLocX(mask.points[mask.points.length-1].x),bigLocY(mask.points[mask.points.length-1].y));
          context.lineTo(x,y);
          context.stroke();
          // context.closePath();              
        }
      }

      for (var j=0; j<mask.points.length; j++){
        var p = mask.points[j]
        if (onpoint(smallLocX(x),smallLocY(y),p)){
          context.fillStyle = color_dict["pointselected"];
          context.fillRect(bigLocX(p.x)-5,bigLocY(p.y)-5,10,10);
        }
        else{
          context.fillStyle = color_dict["point"];
          context.fillRect(bigLocX(p.x)-3,bigLocY(p.y)-3,6,6);
        }
      }
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


function clickPointInMask(clickX,clickY){
	//查看点击的点是否是Mask的标注点，如果是，则着重显示该点。
      for(var i=masks.length-1; i>=0; i--) {
        var mask = masks[i];
        var maskpoints = mask.points
        for (var j=0;j<maskpoints.length;j++){
          var pt = maskpoints[j];
          if (onpoint(smallLocX(clickX),smallLocY(clickY),pt)){
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
		  
          if (onmask_new(smallLocX(clickX),smallLocY(clickY),mask)){
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
            drawRect();
			
      			//弹出标注信息框
      		var bound = mask.getBound();
            var new_region_id =i;
            regions = masks;
            select_only_region(new_region_id);
            showPopup(bigLocY(bound[3])+5, bigLocX(bound[2])+5,i,"mask");
  			     // showPopup(bigLocY(bound[3])+145, bigLocX(bound[2]) + 235,i,"mask");
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

function clickPointInRect(clickX,clickY){

      // //查看点击的点是否是Rect的标注点，如果是，则着重显示该点。
      for(var i=rects.length-1; i>=0; i--) {
        var rect = rects[i];
        var rectpoints = rect.points
        for (var j=0;j<rectpoints.length;j++){
          var pt = rectpoints[j];
		  //console.log("clickX,clickY=" + clickX + "," + clickY + "   smallLocX,smallLocY=" + smallLocX(clickX) + "," +smallLocY(clickY) + " pt.x,pt.y=" + pt.x + "," + pt.y);
          if (onpoint(smallLocX(clickX),smallLocY(clickY),pt)){
            pt.isSelected=true; 
            rect.isSelected=true;
            SelectedRect = rect;
            SelectedpointId = j;
            stretch = true;//允许拉伸
            console.log("choose rect 3333:");
            //更新显示
            drawRect();
            //停止搜索
            return true;

          }
        }
      }
   
      for(var i=rects.length-1; i>=0; i--) {
          var rect = rects[i];
          if (onrect(smallLocX(clickX),smallLocY(clickY),rect)){
            SelectedRect = rect;
     
            old_click_x1=clickX;
            old_click_y1=clickY;

            //选择新圆圈
            rect.isSelected = true;
            //允许拖拽
            isDragging = true;
            //更新显示
            drawRect();
      	    xy = rect.getX1Y1X2Y2();
      	    console.log("choose rect 2:  top=" + clickX + " left=" + clickY + " x1=" + xy[0] + " y1=" + xy[1] + " x2=" + xy[2] + " y2=" + xy[3] + " startx=" + startx + " starty=" + starty);
            select_only_region(i);
            showPopup(bigLocY(xy[3])+5, bigLocX(xy[2])+5 , i,"rect");
      		  // showPopup(bigLocY(xy[3])+145, bigLocX(xy[2]) + 235,i,"rect");
            document.getElementById("boxlabels").innerHTML=boxlabelshtml();
            document.getElementById("labelcounttable").innerHTML=boxlabelcounthtml();
                  //停止搜索
            return true;
          }
     }
	 return false;
}

function clickPointInPointShape(clickX,clickY){
	var t = -1;
	for(var i = 0; i < pointShapes.length; i++){
		if (onpoint(smallLocX(clickX),smallLocY(clickY),pointShapes[i])){
			console.log("clickX=," + clickX + "clickY=," + clickY + "  pointShapes[i].x=" + + pointShapes[i].x + "pointShapes[i].y=" + pointShapes[i].y);
			pointShapes[i].isSelected=true;
			SelectedPointShape = pointShapes[i];
			t = i;
		}else{
			pointShapes[i].isSelected=false;
		}
	}
	if( t >= 0){
		drawRect();
		select_only_region(t);
		showPopup(bigLocY(SelectedPointShape.y) +5, bigLocX(SelectedPointShape.x) +5,t,"pointshape");
			// showPopup(bigLocY(SelectedPointShape.y) + 165, bigLocX(SelectedPointShape.x) + 250,t,"pointshape");
			//showPopup(bigLocY(SelectedPointShape.y) + 165, bigLocX(SelectedPointShape.x) + 250,t,"pointshape");
		document.getElementById("boxlabels").innerHTML=boxlabelshtml();
		document.getElementById("labelcounttable").innerHTML=boxlabelcounthtml();
		return true;
	}
	return false;
}

    
//交互
//画布内鼠标响应操作
function canvasClick(e) {
      // 取得画布上被单击的点
	  
	  
	  
      var clickX = e.pageX - getElementLeft(canvas);
      var clickY = e.pageY - getElementTop(canvas);
	  
	  var smallClickX = smallLocX(clickX);
	  var smallClickY = smallLocY(clickY);
	  
      if (tocreaterect){
        var rect=new rectar(smallClickX,smallClickY,smallClickX,smallClickY,boxcls);
        rects.push(rect);   
    		console.log("draw rect...10");
    		showpopVar = true;
        tocreaterect=false;
		    hidePopup();
        undoShape = rect;
      }
      if (tocreatemask){
    		showpopVar = true;
    		hidePopup();
        if (creatingmask == null){
          var mask=new maskar(smallClickX,smallClickY,boxcls);
          masks.push(mask);
          Selectedmask = mask;
          creatingmask = mask;
        }
        else{
          if (onpoint(smallClickX,smallClickY,creatingmask.points[0])){
            Selectedmask.finish=true;
            creatingmask=null;
            tocreatemask=false;
			      drawRect();
            var new_region_id =masks.length-1;
            var bound = masks[new_region_id].getBound();
            regions = masks;
            set_region_annotations_to_default_value( new_region_id );
            select_only_region(new_region_id);
            showPopup(bigLocY(bound[3])+5, bigLocX(bound[2])+5,new_region_id,"mask");
             // showPopup(bigLocY(bound[3])+145, bigLocX(bound[2]) + 235,i,"mask");
            document.getElementById("boxlabels").innerHTML=boxlabelshtml();
            document.getElementById("labelcounttable").innerHTML=boxlabelcounthtml();
            undoShape = masks[new_region_id];
            resetSelectShapeEmpty();
            return;
          }
          creatingmask.points.push(new point(smallClickX,smallClickY))
        }
      }

	 if(toCreatePoint){
		 console.log("point x,y=" + clickX + "," + clickY);
		 var pointObj = new pointShape(smallClickX,smallClickY,boxcls);
		 pointShapes.push(pointObj);
		 var len_points = pointShapes.length;
		 var new_region_id =len_points-1;
		 regions = pointShapes;
		 set_region_annotations_to_default_value(new_region_id);
		 toCreatePoint = false;
     undoShape = pointShapes[new_region_id];
	 }
     //将选择信息设置为空，在下面重新查找设置
     resetSelectShapeEmpty();
	 
	 if(clickPointInPointShape(clickX,clickY)){
		return; 
	 }
	 
	 //如果点在Mask内，已经画完了，则返回。
	 if(clickPointInMask(clickX,clickY)){
		 return;
	 }
	 //如果点在Rect内，已经画完了，则返回。
	 if(clickPointInRect(clickX,clickY)){
		 return;
	 }

     //如果点不在标注区域内
     drawRect(clickX,clickY);
	   hidePopup();
	 
	   dragging = true;
     old_click_x1 =  clickX;
     old_click_y1 =  clickY;
}


function dragRect(e) {
      // 取得鼠标位置
      var x = e.pageX - getElementLeft(canvas);
      var y = e.pageY - getElementTop(canvas);
	  
	  var smallX = smallLocX(x);
	  var smallY = smallLocY(y);
	  
      if (tocreaterect || tocreatemask){
        drawRect(x,y);
      }

      //悬停变色
      var found=false;
      for(var i=rects.length-1; i>=0; i--) {
        var rect = rects[i];
        var rectpoints = rect.points
        for (var j=0;j<rectpoints.length;j++){
          var pt = rectpoints[j];
          if (onpoint(smallX,smallY,pt)){
            rect.mouseonpoint=true;
            mouseonpoint_rect = rect;             
            //更新显示
            drawRect(x,y);
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
          if (onpoint(smallX,smallY,pt)){
            mask.mouseonpoint=true;
            mouseonpoint_mask = mask;             
            //更新显示
            drawRect(x,y);
            //停止搜索
            found=true;
            break;
          }
        }
        if (found){
          break;
        }
      }
      if ((!found)&& (mouseonpoint_mask!=null)&&(!stretch)) {
        mouseonpoint_mask.mouseonpoint=false;
        mouseonpoint_mask=null;
        drawRect(x,y);
      }
      if ((!found)&& (mouseonpoint_rect!=null)&&(!stretch)) {
        mouseonpoint_rect.mouseonpoint=false;
        mouseonpoint_rect=null;
        drawRect(x,y);
      }
      if (!found){//不在点上
        for(var i=rects.length-1; i>=0; i--) {
          var rect = rects[i];
          if (onrect(smallX,smallY,rect)){
            if (mouseonrect_rect) {mouseonrect_rect.mouseonrect=false;}
            rect.mouseonrect=true;
            mouseonrect_rect = rect;             
            //更新显示
            drawRect(x,y);
            //停止搜索
            found=true;
            break;
          }
        }

        for(var i=masks.length-1; i>=0; i--) {
          var mask = masks[i];
          var maskpoints = mask.points
          if (onmask_new(smallX,smallY,mask)){
            if (mouseonmask_mask) {mouseonmask_mask.mouseonmask=false;}
            mask.mouseonmask=true;
            mouseonmask_mask = mask;             
            //更新显示
            drawRect(x,y);
            //停止搜索
            found=true;
            break;
          }
        }

        if((!found)&& (mouseonrect_rect!=null)){
          mouseonrect_rect.mouseonrect=false;
          mouseonrect_rect=null;
          drawRect(x,y);
        }

        if((!found)&& (mouseonmask_mask!=null)){
          mouseonmask_mask.mouseonmask=false;
          mouseonmask_mask=null;
          drawRect(x,y);
        }
      }

      // 判断矩形是否开始拖拽
      if (isDragging == true) {
        // 判断拖拽对象是否存在
        if (SelectedRect != null) {
          // 将圆圈移动到鼠标位置
		  //console.log("old(" + old_click_x1 + "," + old_click_y1 +") new(" + x + "," + y + ")");
          moverect(SelectedRect, x-old_click_x1, y-old_click_y1);
		  old_click_x1 =  x;
		  old_click_y1 =  y;
          // 更新画布
          drawRect();
        }else if(Selectedmask != null){
		  moveMask(Selectedmask, x-old_click_x1, y-old_click_y1);
		  old_click_x1 =  x;
		  old_click_y1 =  y;
          // 更新画布
          drawRect();
		}
      hidePopup();
      }
      //判断是否开始拉伸
      if (stretch) {
        stretchrect(SelectedRect, SelectedpointId, smallLocX(x), smallLocY(y));
        drawRect();
        document.getElementById("boxlabels").innerHTML=boxlabelshtml();
        document.getElementById("labelcounttable").innerHTML=boxlabelcounthtml();

      }
      //判断mask是否开始拉伸
      if (stretch_mask && SelectedmaskpointId != null) {
        stretchmask(Selectedmask, SelectedmaskpointId, smallLocX(x), smallLocY(y));
        drawRect();
      }
	  if(dragging){
         var dx = x - old_click_x1;
		 var dy = y - old_click_y1;
		 
		 startx -= dx;
		 starty -= dy;
		 console.log("x=" + x + " y=" + y +  " move x=" + dx + " move y=" + dy + " startx=" +startx + ",starty=" + starty);
		 drawRect();
		 old_click_x1 = x;
		 old_click_y1 = y;
	  }
 };

function stopDragging() {
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
            showPopup(bigLocY(xy[3])+5, bigLocX(xy[2])+5 , i,"rect");

			     // showPopup(bigLocY(xy[3])+145, bigLocX(xy[2]) + 235, i,"rect");
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
              showPopup(bigLocY(bound[3])+5, bigLocX(bound[2])+5 ,i,"mask");
		         // showPopup(bigLocY(bound[3])+145, bigLocX(bound[2]) + 235,i,"mask");
		        	console.log("create mask--show...13");
            //停止搜索
            return;
          }
      }

    for(var i=pointShapes.length-1; i>=0; i--) {
        var pointShape = pointShapes[i];
        if (pointShape.isSelected && pointShape.finish){
           var new_region_id =i;
           regions = pointShapes;
           select_only_region(new_region_id);
           set_region_annotations_to_default_value(new_region_id);
           // var bound = mask.getBound();
           showPopup(bigLocY(pointShape.x)+5, bigLocX(pointShape.y)+5 ,i,"pointShapes");
           // showPopup(bigLocY(pointShape.x)+145, bigLocX(pointShape.y) + 235,i,"pointShapes");
            console.log("create mask--show...13");
          //停止搜索
          return;
        }
    }
	  
  }
};

function updateLabelHtml(){
	 document.getElementById("boxlabels").innerHTML=boxlabelshtml();
     document.getElementById("labelcounttable").innerHTML=boxlabelcounthtml();
}


//边栏
//任务信息
function showtaskinfo(){

  $('#task_info').text(label_task_info.task_name);
  $('#task_progress').text(label_task_info.task_status_desc);
  
}

var orderType = 1;
var findLast = 0;

function  showOrder(value){
  hidePopup();
  if (value == "1"){ //文件名排序
     orderType = 1;
     findLast  = 0;
  }
  else if (value == "2"){
    orderType = 0;
    findLast  = 0;
  }

  page(0,pageSize);  
}

function skipLast(){
  hidePopup();
  if (orderType == 0){
    return ;
  }
    findLast  = 1;
    page(0,pageSize); 

}

function isVerified(){
	if(userType == 2 && label_task_status == 1){
		return true;
	}
	return false;
}

//显示文件列表
function showfilelist(){
    var htmlstr="";
    for (var i=0;i<labeltastresult.length;i++){
       var fname = labeltastresult[i].pic_image_field.substring(labeltastresult[i].pic_image_field.lastIndexOf('/') + 1);
       var isfinished = labeltastresult[i].label_status;
	   if(isVerified()){
		   isfinished = labeltastresult[i].verify_status - 1;
	   }
       var lablebg=" style=\"cursor:pointer\"";
       var finish="未完成";
       if (isfinished=="0"){finish="已完成";}
       if (i==fileindex){lablebg=" style=\"background:#eee;color:#5a5a5a;cursor:pointer;\"";}
	   var classStr = "";
	   if (isfinished=="0"){
		   // classStr = " class=\"btn btn-xs btn-success\"";
       classStr = "class=\"file-select\"";//
	   }
	   if(isVerified()){
		   htmlstr = htmlstr+"<tr onclick=\"clickfilelist("+i+");\""+ lablebg+"> <td width=\"70\"" +"style=\"vertical-align:middle\""+ classStr + ">"+"<button"+classStr+" type=\"button\" onclick=\"changeVerifyStatus("+i+");\" style=\"border:none;background:none\">"+finish +"</button>"+"</td><td>"+ fname+ "</td></tr>"; 
	   }else{
	       htmlstr = htmlstr+"<tr onclick=\"clickfilelist("+i+");\""+ lablebg+"> <td width=\"70\"" +"style=\"vertical-align:middle\""+ classStr + ">"+"<button"+classStr+" type=\"button\" onclick=\"changeStatus("+i+");\" style=\"border:none;background:none\">"+finish +"</button>"+"</td><td>"+ fname+ "</td></tr>";
	   }
       
    };
    document.getElementById("filelist").innerHTML=htmlstr;
}

function changeStatus(i){
   var tmpId='changeStatus_'+i;
  var htmlstr= "<div class=\"panel\" style=background:#ffff;border: 1px solid rgba(0,0,0,0.2);border-radius: 6px;top:10px;"
     +">"
     +"<label for=\"st\" style=\"margin:15px;color:#333\"> 标注状态修改为:</label>"+
      "<select id="+tmpId+" name=\"标注状态\"  style=\"text-align:center;border-radius:4px;\">"+
      "<option value=0 selected=\"\">已完成</option>"+
      "<option value=1 >未完成</option>"+
      "</select>"+
      "<br />"+
      "<button onclick=\"updateStatus("+i+");\" class=\"btn btn-default\" style=\"margin:0  auto;display: block;border-radius:5px;width: 50px;\">提交</button>"
    +"</div>"

   document.getElementById("labelStatus").innerHTML=htmlstr;
}

// function changeStatus(i){
//    var tmpId='changeStatus_'+i;
//   var htmlstr= "<div class=\"panel\" style=background:#ffff;border: 1px solid rgba(0,0,0,0.2);border-radius: 6px;top:10px;"
//      +">"
//      +"<label for=\"st\" style=\"margin:15px;color:#333\"> 标注状态修改为:</label>"+
//       "<select id="+tmpId+" name=\"标注状态\"  style=\"width:100px;text-align:center;border-radius:4px;\">"+
//       "<option value=0 selected=\"\">已完成</option>"+
//       "<option value=1 >未完成</option>"+
//       "</select>"+
//       "<br />"+
//       "<button onclick=\"updateStatus("+i+");\" class=\"btn btn-default\" style=\"margin:0  auto;display: block;border-radius:5px;width: 50px;\">提交</button>"
//     +"</div>"

//    document.getElementById("labelStatus").innerHTML=htmlstr;
// }

function changeVerifyStatus(i){
   var tmpId='changeStatus_'+i;
  var htmlstr= "<div class=\"panel\" style=background:#ffff;border: 1px solid rgba(0,0,0,0.2);border-radius: 6px;top:10px;"
     +">"
     +"<label for=\"st\" style=\"margin:15px;color:#333\"> 审核状态修改为:</label>"+
      "<select id="+tmpId+" name=\"审核状态\"  style=\"width:100px;text-align:center;border-radius:4px;\">"+
      "<option value=1 selected=\"\">已完成</option>"+
      "<option value=0 >未完成</option>"+
      "</select>"+
      "<br />"+
      "<button onclick=\"updateVerifyStatus("+i+");\" class=\"btn btn-default\" style=\"margin:0  auto;display: block;border-radius:5px;width: 50px;\">提交</button>"
    +"</div>"

   document.getElementById("labelStatus").innerHTML=htmlstr;
}

function updateStatus(i){
  var status_id = 'changeStatus_'+i;
  var label_status=document.getElementById(status_id).value;
  labeltastresult[fileindex].label_status = label_status;
  $.ajax({
         type:"PATCH",
         url:ip + "/api/label-task-item-status/",
         contentType:'application/json',
         headers: {
            authorization:token,
          },
         dataType:"json",
         async:false,
         data:JSON.stringify({'id':labeltastresult[fileindex].id,
                              'label_status':label_status,
         }),
         success:function(json){
          showfilelist();
          document.getElementById("labelStatus").innerHTML = "";
          return true;
        },
	    error:function(response) {
		  redirect(response);
        }
   });


}

function updateVerifyStatus(i){
  var status_id = 'changeStatus_'+i;
  var verify_status=document.getElementById(status_id).value;
  labeltastresult[fileindex].verify_status = verify_status;
  $.ajax({
         type:"PATCH",
         url:ip + "/api/label-task-item-verify-status/",
         contentType:'application/json',
         headers: {
            authorization:token,
          },
         dataType:"json",
         async:false,
         data:JSON.stringify({'id':labeltastresult[fileindex].id,
                           'verify_status':verify_status,
         }),
      
         success:function(json){
           showfilelist();
           document.getElementById("labelStatus").innerHTML = "";
           return true;
         },
	     error:function(response) {
		  redirect(response);
         }
   });

}

	//this.id =""; //标识
	//this.blurred=false;//模糊不清的; 记不清的; 难以区分的; 模棱两可的
	//this.goodIllumination = true; //照明
	//this.frontview = true;//正面图
function parse_labelinfo(labelinfo){
	rects.length = 0;
	masks.length = 0;
	pointShapes.length = 0;
	if(!isEmpty(labelinfo)){
		// console.log("标注信息："+labelinfo);
		var label_arr = JSON.parse(labelinfo);
		
		for(var i=0;i<label_arr.length;i++){
		  if(!isEmpty(label_arr[i].mask)){
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
			  x1 = getCanvasLocationX(label_arr[i].box[0]);
			  y1 = getCanvasLocationY(label_arr[i].box[1]);
		      x2 = getCanvasLocationX(label_arr[i].box[2]);
		      y2 = getCanvasLocationY(label_arr[i].box[3]);
		      cls=label_arr[i].class_name;
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
			  var pointShapeObj = new pointShape(getCanvasLocationX(label_arr[i].keypoints[0]),getCanvasLocationY(label_arr[i].keypoints[1]),cls,score);
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
		//console.log(rects);
		//console.log(masks);
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
     var $objTr = $("#"+tmp) ; //找到要定位的地方  tr 
     var objTr = $objTr[0]; //转化为dom对象 
     if (!isEmpty(objTr)){
      $("#labelpanel").animate({scrollTop:objTr.offsetTop},0); //滚动条滚动
     }
	 }
	 
	 var tmpType = shape.type;
	 //console.log("tmpType =" + tmpType);
	 if(!isEmpty(shape.other["region_attributes"]["type"])){
		 tmpType = shape.other["region_attributes"]["type"];
	 }
	 
	 var shapeswidthHeigth = "";
	 if (shapetype == "bbox"){
		 xywh = shape.getXYWH();
		 shapeswidthHeigth = getRealLocationX(xywh[2]) + "," + getRealLocationY(xywh[3]);
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
  drawRect();
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

function setAutoModel(){
  hidePopup();
  loadModel();
  display_createlabel();
}

var algModelList;
function loadModel(){

    $.ajax({
      type:"GET",
      contentType:'application/json',
      url: ip + "/api/queryAlgModelForHandLabel/",
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

function display_createlabel(){
  var html="<option value=\"\" selected=\"\">请选择</option>";
  for (var i=0;i<algModelList.length;i++){
        if (i==2){
        var row = "<option selected value=\""+algModelList[i].id+
        "\">"+algModelList[i].model_name+
        "</option>";
        }
        else{
          var row = "<option value=\""+algModelList[i].id+
        "\">"+algModelList[i].model_name+
        "</option>";
        }

      
      html=html+row;
  }
  console.log(html);
  $('#tracking_startid').val('1');
  $('#tracking_endid').val(tablePageData.total);
  document.getElementById('predict_model').innerHTML=html; 
  document.getElementById("tracking_startid_div").style.display="none";
  document.getElementById("tracking_endid_div").style.display="none";
  document.getElementById("label_id_div").style.display="none";
  //document.getElementById("labelOption_div").style.display="none";
  document.getElementById("labelOption").innerHTML="<option value=\"2\" selected=\"\">请选择</option> <option value=\"3\">识别车的类型与颜色</option>";
  document.getElementById('predtask_id').disabled=false; 
  
  $('#label_id').val('1');
  
  bar.style.width='1%';
  document.getElementById('text-progress').innerHTML='0%';
}

function submit_predtask(){
	var item_id = labeltastresult[fileindex].id;
	var predict_model_val = $('#predict_model').val();
	if(isEmpty(predict_model_val)){
		alert("请选择标注任务使用的模型。");
		return;
	}
	if(labeltastresult[fileindex].label_status == 0 && !(predict_model_val == 12 || predict_model_val == 16 || predict_model_val == 20 || predict_model_val == 21)){
		alert("标注状态已经完成，不能进行自动检测。");
		return;
	}
	var task_type = "1";
	var start_id = 0;
	var end_id = 0;
	var label_id = 1;
	var label_option = $('#labelOption').val();
	if(predict_model_val == 12 || predict_model_val == 16 || predict_model_val == 20 || predict_model_val == 21){
		task_type = "2";
		start_id = $('#tracking_startid').val();
		end_id = $('#tracking_endid').val();
		label_id = $('#label_id').val();
		if(start_id < 1 || start_id > tablePageData.total){
			alert("请输入正确的追踪起始图片ID，范围为1到" + tablePageData.total);
			return;
		}
		
		if(end_id < 1 || end_id > tablePageData.total){
			alert("请输入正确的追踪结束图片ID，范围为1到" + tablePageData.total);
			return;
		}
		if(label_id < 1){
			alert("标注框ID为大于等于1的整数。");
			return;
		}
	}
	document.getElementById('predtask_id').disabled=true; 
	$.ajax({
         type:"POST",
         url:ip + "/api/auto-label-task",
         contentType:'application/json',
         headers: {
            authorization:token,
          },
         dataType:"json",
         async:false,
         data:JSON.stringify({'label_task_id':item_id,
                              'model':predict_model_val,
                              'pic_object_name' : img.width + "," + img.height,
                              'task_type':task_type,
							  'startIndex':start_id,
							  'endIndex':end_id,
							  'taskId':label_task,
							  'label_id':label_id,
							  'label_option':label_option
          }),        
          success:function(res){
             console.log('创建数据信息');
          },
	      error:function(response) {
		     redirect(response);
          }
         });
     
     setIntervalToDo();
}

var timeId;
var count;
var progress;

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
	   $("#autoLabel").modal('hide');
	   return;
   }
   
   $.ajax({
       type:"GET",
       url:ip + "/api/query-auto-label-task-progress/",
       headers: {
          authorization:token,
        },
       dataType:"json",
	   data:{'taskId': label_task},
       async:false,
       success:function(json){
        progress = json;
        console.log(json);
      },
	  error:function(response) {
		  redirect(response);
      }
   });
   if(isEmpty(progress)){
	   console.log("清除定时器。timeId=" + timeId);
	   bar.style.width='100%';
       document.getElementById('text-progress').innerHTML='100%';
	   window.clearInterval(timeId);
	   timeId = null;
	   var current = $('#displayPage1').text();
	   $("#autoLabel").modal('hide');
	   console.log("开始获取结果。current=" + current);
       if(current >= 1){
           pageReload(current - 1,pageSize,fileindex);
       }
   }else{
	   //更新进度
	    var iSpeed = progress.progress;
	    bar.style.width=iSpeed+'%';
        document.getElementById('text-progress').innerHTML=iSpeed+'%'
   }
   /*
   var current = $('#displayPage1').text();
   console.log("开始刷新。current=" + current);
   if(current >= 1){
       pageReload(current - 1,pageSize,fileindex);
       if(!isEmpty(labeltastresult[fileindex].label_info)){
             window.clearInterval(timeId);
             timeId = null;
       }
   }
   */
}

function submit_deletelabel(){
	
	var start_id = $('#delete_startid').val();
	var end_id = $('#delete_endid').val();
	if(start_id < 1 || start_id > tablePageData.total){
			alert("请输入正确的起始图片ID，范围为1到" + tablePageData.total);
			return;
	}
		
	if(end_id < 1 || end_id > tablePageData.total){
			alert("请输入正确的结束图片ID，范围为1到" + tablePageData.total);
			return;
	}
    //var one_reid_name =  $('#one_reid_name').val();
    $.ajax({
         type:"POST",
         url:ip + "/api/label-task-delete-label",
         headers: {
            authorization:token,
          },
         dataType:"json",
         async:false,
         data:{'label_task_id':label_task_info.id,
               'start_id':start_id,
               'end_id' : end_id
			   //'one_reid_name':one_reid_name
               },        
         success:function(res){
             console.log('创建数据信息');
         },
	     error:function(response) {
		    redirect(response);
         }
         });
	
	$("#deleteLabel").modal('hide');
	var current = $('#displayPage1').text();

    if(current >= 1){
	   pageReload(current - 1,pageSize,fileindex);
	}
	
}


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
     
	  var xmin = getRealLocationX(x1y1x2y2[0]);
	  var ymin = getRealLocationY(x1y1x2y2[1]);
      var xmax = getRealLocationX(x1y1x2y2[2]);
	  var ymax = getRealLocationY(x1y1x2y2[3]);
      
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
      //console.log(label);
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
		  labelMaskList.push(getRealLocationX(masks[i].points[j].x));
		  labelMaskList.push(getRealLocationY(masks[i].points[j].y));
	  }
	  label['mask'] = labelMaskList;
	  label['id'] = masks[i].id+'';
	  label['blurred']=masks[i].blurred;
	  label['goodIllumination']=masks[i].goodIllumination;
	  label['frontview']=masks[i].frontview;
      label['other']=masks[i].other;
      //console.log(label);
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
	 
	  pointList.push(getRealLocationX(pointShapes[i].x));
	  pointList.push(getRealLocationY(pointShapes[i].y));
	  pointList.push(2);//0表示这个关键点没有标注（这种情况下x=y=v=0），1表示这个关键点标注了但是不可见(被遮挡了），2 表示这个关键点标注了同时也可见
	  label['keypoints'] = pointList;
	  label['id'] = pointShapes[i].id+'';
	  label['blurred']=pointShapes[i].blurred;
	  label['goodIllumination']=pointShapes[i].goodIllumination;
	  label['frontview']=pointShapes[i].frontview;
      label['other']=pointShapes[i].other;
      //console.log(label);
	  label_list.push(label);
  }
  
  labelinfo_jsonstr = JSON.stringify(label_list);
  labeltastresult[fileindex].label_info=labelinfo_jsonstr;
  var flag = 0;
  if(isVerified()){
	  labeltastresult[fileindex].verify_status=1;
	  flag = 100;
  }else{
    if(label_list.length > 0){ 
          labeltastresult[fileindex].label_status=0;
      }
    else{
        labeltastresult[fileindex].label_status=1;
    }  
  }
  
  $.ajax({
       type:"PATCH",
       url:ip + "/api/label-task-item/",
	   contentType:'application/json',
       headers: {
          authorization:token,
        },
       dataType:"json",
       async:false,
	   data:JSON.stringify({'id':labeltastresult[fileindex].id,
                            'label_info':labelinfo_jsonstr,
                            'label_status': labeltastresult[fileindex].label_status,
							'verify_status': labeltastresult[fileindex].verify_status,
                            'pic_object_name' : img.width + "," + img.height,
							'display_order2':flag
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

function get_labeltask(){
    $.ajax({
       type:"GET",
       url:ip + "/api/label-task/"+label_task+"/",
       headers: {
          authorization:token,
        },
       dataType:"json",
       async:false,
       success:function(json){
         label_task_info = json;
         console.log(label_task_info);
		 page(0,pageSize);
       },
	   error:function(response) {
		  redirect(response);
       }
   });
}


  function update_labeltask(task_label_type_info){
	  console.log("label_task_id=" + label_task_info.id);
	  console.log("task_label_type_info=" + task_label_type_info);
	  
      $.ajax({
         type:"PATCH",
         url:ip + "/api/label-task/",
         headers: {
            authorization:token,
          },
         dataType:"json",
         data:{
           'label_task_id':label_task_info.id,
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


function list(current,pageSize,index=0){
    $.ajax({
       type:"GET",
       url:ip + "/api/label-task-item-page/",
       headers: {
          authorization:token,
        },
       dataType:"json",
	   data:{
		   'label_task':label_task_info.id,
		   'startPage':current,
		   'pageSize':pageSize,
		   'orderType': orderType,
           'findLast':findLast,
	   },
       async:false,
       success:function(json){
			tablePageData = json;
			tableData = json.data;
			labeltastresult = tableData;
			fileindex=index;
			if(lastindex){
				fileindex = pageSize - 1;
			}
        //console.log(json);
        // return json.token;
        },
	    error:function(response) {
		  redirect(response);
        }
   });
}

var pageSize = 20;
var tableData;
var tablePageData;

function pageReload(current,pageSize,fileindex){
  list(current,pageSize,fileindex);
  loadimg();
  showtaskinfo();
  showfilelist();
	  
  //display_list();
  setPage(tablePageData,pageSize);
}

function page(current,pageSize){
  list(current,pageSize);
  loadimg();
  showtaskinfo();
  showfilelist();
	  
  //display_list();
  setPage(tablePageData,pageSize);
}

function nextPage(){
   hidePopup();
   var current = $('#displayPage1').text();
   console.log("current=" + current);
   findLast = 0;
   page(current,pageSize);
}

function prePage(){
  hidePopup();
  var current =$('#displayPage1').text();
  console.log("current=" + current);
  if(current > 1){
    console.log("current=" + (current - 2));
    findLast = 0;
    page(current - 2,pageSize);
  } 
}

function goPage(){
   hidePopup();
   var goNum = $('#goNum').val();
    findLast = 0;
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
  var startIndex = pageData.current * pageSize;
  if(startIndex < 10){
	  $('#startIndex').text(" " + (pageData.current * pageSize + 1));
  }else{
	  $('#startIndex').text(pageData.current * pageSize + 1);
  }
  var endIndex = pageData.current * pageSize + pageData.data.length;
  if(endIndex < 10){
	   $('#endIndex').text(" " + (pageData.current * pageSize + pageData.data.length));
  }else{
	   $('#endIndex').text(pageData.current * pageSize + pageData.data.length);
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

 
    canvas = document.getElementById("myCanvas");
    context = canvas.getContext("2d");
    var maxWidth  = document.getElementById("tool0").offsetWidth;
    var maxHeight = document.getElementById("tool0").offsetWidth/1280*720;
    canvas.width = maxWidth;//document.getElementById("tool0").offsetWidth;
    canvas.height = maxHeight;//document.getElementById("tool0").offsetWidth/1280*720;

    // show_region = document.getElementById("show_region");
    // show_region.width =document.getElementById("tool0").offsetWidth;
    // show_region.height = document.getElementById("tool0").offsetWidth/1280*720;

  window.onload = function() {

    canvas    = document.getElementById("myCanvas");
    context   = canvas.getContext("2d");
    maxWidth  = document.getElementById("tool0").offsetWidth;
    maxHeight = document.getElementById("tool0").offsetWidth/1280*720;
    canvas.width = maxWidth;
    canvas.height = maxHeight;
	
    canvas.onmousedown = canvasClick;
    canvas.onmouseup = stopDragging;
    canvas.onmouseout = stopDragging;
    canvas.onmousemove =dragRect; 
    
	var token = getCookieOrMessage("token");
	console.log("page load token=" + token);
    if(!isEmpty(token)){
		console.log("this is not null.token =" + token);
		doLoad();
	}
  };
  
function doLoad(){
	token = getCookieOrMessage("token");
    userType = getCookieOrMessage("userType");
	
	var medical_flag = getSessionStorageMessage("medical_flag");
	if(!isEmpty(medical_flag)){
	   $('.left-side').toggleClass("collapse-left");
       $(".right-side").toggleClass("strech");
       document.getElementById("hiddenLeft").style.display="none";
	   document.getElementById("logotitle").style.display="none";
	}
	
    if(typeof token == "undefined" || token == null || token == ""){
        console.log("token=" + token);
		console.log("onload tasks/detect/index.html");
        window.location.href = "../../login.html";
    }else{
        var nickName = getCookieOrMessage("nickName");
        console.log("nickName=" + nickName);
        $("#userNickName").text(nickName);
        $("#userNickName_bar").text(nickName);
    }
	label_task = getSessionStorageMessage("label_task");
    label_task_status = getSessionStorageMessage("label_task_status");  //1表示审核
    get_labeltask();	  
    drawRect();
	_via_init();
    //加载保存好的属性结构,如果不存在，加载默认的类别信息
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
	
}
  
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
       _via_attributes['region'][atti]["options"]["car"] = "car";
       _via_attributes['region'][atti]["options"]["person"] = "person";
	   _via_attributes['region'][atti]["options"]["non-motor"] = "non-motor";
       _via_attributes['region'][atti]["default_options"] = {};
}
	
function model_sele_Change(event){
	var modelid = $('#predict_model option:selected').val();
	console.log("predict_model=" + modelid);
	if(modelid == 12 || modelid == 16 || modelid == 20 || modelid == 21){
		document.getElementById("tracking_startid_div").style.display="block";
		document.getElementById("tracking_endid_div").style.display="block";
		if(modelid == 12 || modelid == 16){
		    document.getElementById("label_id_div").style.display="block";
		}else{
		    document.getElementById("label_id_div").style.display="none";
		}
		if(modelid == 21){
			document.getElementById("labelOption").innerHTML="<option value=\"10\" selected=\"\">person</option> <option value=\"11\">car</option>";
		}else if(modelid == 20){
			document.getElementById("labelOption").innerHTML="<option value=\"10\" selected=\"\">person</option>";
		}else{
		    document.getElementById("labelOption").innerHTML="<option value=\"0\" selected=\"\">合并已有的标注信息</option> <option value=\"1\">删除已有的标注信息</option>";
		}
	}else{
		
		document.getElementById("tracking_startid_div").style.display="none";
		document.getElementById("tracking_endid_div").style.display="none";
		document.getElementById("label_id_div").style.display="none";
		if(modelid == 5 || modelid == 6 || modelid == 7 ){
		   document.getElementById("labelOption").innerHTML="<option value=\"2\" selected=\"\">请选择</option> <option value=\"3\">识别车的类型与颜色</option>";
		}
	}
}

function export_attribute(){
     var url = ip + "/api/task-export-label-property/";
	 var $iframe = $('<iframe />');
     var $form = $('<form  method="get" target="_self"/>');
	$form.attr('action', url); //设置get的url地址

	$form.append('<input type="hidden"  name="task_id" value="' + label_task + '" />');
	$form.append('<input type="hidden"  name="type" value="labeltask" />');
		
	$iframe.append($form);
	$(document.body).append($iframe);
	$form[0].submit();//提交表单
    $iframe.remove();//移除框架

}

function import_attribute(){
   
   $('#datasetModal').modal('show');

}	

function submit_import_property(){
	var jsonContent = $("#jsoninput").val();
	if(!isJSON(jsonContent)){
		alert("输入格式非法。");
		return;
	}
	$.ajax({
         type:"POST",
         url:ip + "/api/task-import-label-property/",
         headers: {
            authorization:token,
          },
         dataType:"json",
         data:{
           'jsonContent':jsonContent,
		   'taskType':"labeltask",
		   'taskId':label_task
         },
         async:false,
         success:function(json){
           console.log(json);
         },
	     error:function(response) {
		  redirect(response);
         }
     });
	$('#datasetModal').modal('hide');
}

function isJSON(str) {
    if (typeof str == 'string') {
        try {
            var obj=JSON.parse(str);
            if(typeof obj == 'object' && obj ){
                return true;
            }else{
                return false;
            }

        } catch(e) {
            console.log('error：'+str+'!!!'+e);
            return false;
        }
    }
    console.log('It is not a string!');
	return false;
}	
 
  
  img.onload = function(){
    loadFinished = false;
    // 初始设置画布大小，最大值宽和高
    canvas.width = maxWidth;//document.getElementById("tool0").offsetWidth;
    canvas.height = maxHeight;//document.getElementById("tool0").offsetWidth/1280*720;
    //调整画布大小
    if ((img.width/img.height)<(canvas.width/canvas.height)){
      canvas.width=canvas.height * img.width / img.height;
    }
    else{
      canvas.height=canvas.width * img.height / img.width;
    }

    maxIdNum=0;
    parse_labelinfo(labeltastresult[fileindex].label_info);
    drawRect();
	   $('#filepanel').slimScroll({
      alwaysVisible: true, //是否 始终显示组件(未更改库文件时,鼠标移开滚动条消失)
     });
	canvas.onmousewheel = canvas.onwheel = function (event) {    //滚轮放大缩小
	    console.log("primivate x,y=" + event.clientX + "," + event.clientY);//鼠标在屏幕上的位置
        event.wheelDelta = event.wheelDelta ? event.wheelDelta : (event.deltalY * (-40));  //获取当前鼠标的滚动情况
        if (event.wheelDelta > 0) {// 放大
                imgScale +=0.1;
				console.log("imgScale=" + imgScale);
        } else {//  缩小
            imgScale -=0.1;
            if(imgScale< 1) {//最小缩放1
                imgScale = 1;
            }
			console.log("imgScale=" + imgScale);
        }
        drawRect();   //重新绘制图片
    };
    loadFinished=true;
	
  };
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
        case 81:
          last();
          break;
        case 69:
          next();
          break;
        case 90://u
          undo();
          break;
        case 67://c
          copyOneBox();
          break;
        case 86://v
          paste();
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