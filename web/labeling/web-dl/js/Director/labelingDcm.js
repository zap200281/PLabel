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
var token = getCookie("token");
console.log("token=" + token);
var imgRadial = new Image();
var imgChord = new Image();
var clickNums=0;
var currentImage=true;

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

function getSessionStorageMessage(key){
	var value = sessionStorage.getItem(key);
	if(isEmpty(value)){
		return localStorage.getItem(key);
	}
	return value;
}

// canvas 矩形框集合
//by yunpeng zhai
//变量定义//变量重置
//

var label_task = getSessionStorageMessage("label_task");
console.log("label_task=" + label_task);
var label_task_info;
var labeltastresult;

var rects=[];
var masks=[];
var pointShapes =[];
var copyrects;//复制变量
var copymasks;//复制使用的变量
var copyPointShapes;

var color_dict = {"car":"#0099CC", "inflammation":"#FF99CC","point":"#00cc00","pointselected":"red"};
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
var boxcls = "inflammation";
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
    boxcls = "inflammation";
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
	this.isEqual = function(other_pointShape){
		if(other_pointShape.x == this.x && other_pointShape.y == this.y && other_pointShape.type == this.type){
			return true;
		}
		return false;
	}
};

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
	
	this.isEqual = function (other_rectar){
		if(other_rectar.type != this.type){
			return false;
		}
		for(var i = 0; i < this.points.length; i++){
			if(other_rectar.points[i].x != this.points[i].x ||  other_rectar.points[i].y != this.points[i].y){
			  return false;
		    }
		}
		return true;
	}
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
  this.other = {};//自定义属性
  this.other['region_attributes'] = {};
  this.isEqual = function (other_maskar){
		if(other_maskar.type != this.type){
			return false;
		}
		for(var i = 0; i < this.points.length; i++){
			if(other_maskar.points[i].x != this.points[i].x ||  other_maskar.points[i].y != this.points[i].y){
			   return false;
		    }
		}
		return true;
	}
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
  
  //弹框下拉焦点消失
  $("select").bind("focus", function(){
    if(this.blur){
      this.blur();
    }
  });
}


// function showPopup(topx,lefty,rectIndex,recttype) {

//     if (topx > canvas.height){
//     topx = canvas.height;
//   }
//   if (lefty > canvas.width){
//     lefty = canvas.width;
//   }
  
// 	var tmp = getShowShape(recttype);
// 	if(isEmpty(tmp)){
// 		return;
// 	}
	
// 	console.log("rect type=" + tmp[rectIndex].type + " rect rectIndex" + rectIndex + " topx=" + topx + ", lefty=" + lefty);
// 	$('#hiderectIndex').val(rectIndex);
// 	$('#hiderectType').val(recttype);
// 	$('#type__1').val(tmp[rectIndex].type);
// 	if(isEmpty(tmp[rectIndex].id)){
// 		$('#idvalue').val(rectIndex);
// 	}else{
// 		$('#idvalue').val(tmp[rectIndex].id);
// 	}
// 	// $("#image_quality__1__blur").prop("checked",tmp[rectIndex].blurred);
// 	// $("#image_quality__1__good_illumination").prop("checked",tmp[rectIndex].goodIllumination);
// 	// $("#image_quality__1__frontal").prop("checked",tmp[rectIndex].frontview);
	
// 	var annotation_editor = document.getElementById("annotation_editor");
// 	annotation_editor.style.top = topx + 'px';
// 	annotation_editor.style.left = lefty  + 'px';
//     annotation_editor.style.display = "flex";
	
//  }

　　　 　//关闭弹窗函数，设置display为none，传一个参数，把true和false传进去
// function hidePopup() {
//   document.getElementById("annotation_editor").style.display = "none";
// }

// function annotation_editor_on_metadata_update(com){
	
// 	console.log("id=" + com.id + " hiderectIndex=" + $('#hiderectIndex').val());
// 	var rectIndex = $('#hiderectIndex').val();
// 	var recttype = $('#hiderectType').val();
// 	var tmp = getShowShape(recttype);
// 	if(isEmpty(tmp) && rectIndex >=0){
// 		return;
// 	}
// 	tmp[rectIndex].type = $('#type__1').val();
// 	tmp[rectIndex].id = $('#idvalue').val();
// 	// 标注CT时，不需要该表单，给默认false
// 	tmp[rectIndex].blurred =  false;
// 	tmp[rectIndex].goodIllumination =  false;
// 	tmp[rectIndex].frontview = false;
// 	// tmp[rectIndex].blurred =  $("#image_quality__1__blur").is(":checked");
// 	// tmp[rectIndex].goodIllumination =  $("#image_quality__1__good_illumination").is(":checked");
// 	// tmp[rectIndex].frontview = $("#image_quality__1__frontal").is(":checked");
	
// }
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

}
function copy(){
	copyrects = rects.slice();
    copymasks = masks.slice();
	copyPointShapes = pointShapes.slice();
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
			rects.push(tmpRects[i]);
		}
	}
	if(!isEmpty(copymasks)){
		var tmpMasks = copymasks.slice();
		// copymasks.length = 0;
		for(var i = 0; i <tmpMasks.length; i++){
			masks.push(tmpMasks[i]);
		}
	}
	if(!isEmpty(copyPointShapes)){
		var tmpPointShapes = copyPointShapes.slice();
		// copyPointShapes.length = 0;
		for(var i = 0; i <tmpPointShapes.length; i++){
			pointShapes.push(tmpPointShapes[i]);
		}
	}
	drawRect();
  currentImage=true;
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
function canvasChange(){
    canvas.onmousedown = canvasClick;
    canvas.onmouseup   = stopDragging;
    canvas.onmouseout  = stopDragging;
    canvas.onmousemove = dragRect; 
	
	/**
	//邹安平：先不需要三维显示
    radialCanvas.onmousedown = canvasClick;
    radialCanvas.onmouseup   = stopDragging;
    radialCanvas.onmouseout  = stopDragging;
    radialCanvas.onmousemove = dragRect; 
	*/

}
function createRectLabel(){
  canvasChange();
  statesLine=false;
  createrect("inflammation");
}

function createMaskLabel(){
  canvasChange();
  statesLine=false;
  createmask("inflammation");
}

function createPointLabel(){
  canvasChange();
  statesLine=false;
  createPoint("inflammation");
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
    SelectedRect=null;
  }
  if(Selectedmask){
    var index = masks.indexOf(Selectedmask);
    masks.splice(index, 1);
    Selectedmask=null;
  }

  if(SelectedPointShape){
	var index = pointShapes.indexOf(SelectedPointShape);
    pointShapes.splice(index, 1);
    SelectedPointShape=null;
  }
  drawRect();
  // updateLabelHtml();
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
  console.log("picture:" + ip + labeltastresult[fileindex].pic_image_field);
  img.src = ip + labeltastresult[fileindex].pic_image_field;
}
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

function next(){
  currentImage = false;
  hidePopup();
  if(fileindex<labeltastresult.length-1)  {
     updatelabel(fileindex);
     clearCache();
     fileindex=fileindex+1;	 
	 loadimg();
     showfilelist();
  }else{
	  if((tablePageData.current + 1) * pageSize < tablePageData.total){
		   updatelabel(fileindex);
           clearCache();
		   nextPage();
	  }
  }
}
function last(){
  currentImage = false;
  hidePopup();
  if(fileindex>0)  {
	  updatelabel(fileindex);
	  clearCache();
	  fileindex=fileindex-1;
	  loadimg();
      showfilelist();  
  }else{
	  var current = $('#displayPage1').text();
	  if(current > 1){
	     lastindex = true;
		 updatelabel(fileindex);
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
    if ((biasx<=5) && (biasy<=5)){
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

    //清除画布，准备绘制
    context.clearRect(0, 0, canvas.width, canvas.height);

    var imgWidth = canvas.width;
    var imgHeight = canvas.height;
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
   if( document.getElementById("showLightConstrast").style.display =='block'){
       changeBrightnessContrast(img,canvas,context);
   }

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
      // context.stroke();
      // context.closePath();
      //col
      // context.beginPath();
      context.moveTo(x,0);
      context.lineTo(x,canvas.height);
      context.stroke();
      context.closePath();
    }
	//遍历画所有Mask矩形框
    drawMaskShape(x,y);
 
    //遍历画所有的圆点
    drawPointShape();
    //画标志信息
    if (statesLine){
      // textCrossDraw();
      initeDrawCrossLine();
    }
    
}



var radial_x;
var radial_y;
var isRadialDraw=false;

var chord_x;
var chord_y;
var isChordDraw=false;

var cross_x;
var cross_y;
var isCrossDraw=false;
var statesLine = false;
function drawLine(Canvas,context,x,y,isDraw){
   if (isDraw){
      x=Math.max(0,x);
      y=Math.max(0,y);
      x=Math.min(x,Canvas.width-1);
      y=Math.min(y,Canvas.height-1);
          // 取得鼠标位置
      context.lineWidth = 2;
      context.strokeStyle="red"
      context.beginPath();
      context.moveTo(0,y);
      context.lineTo(Canvas.width,y);

      context.moveTo(x,0);
      context.lineTo(x,Canvas.height);
      context.stroke();
      context.closePath();
      var textX = getRealLocX(Canvas,x)+" PX";
      context.fillStyle ="#000000";
      context.font="bolder 14px Arial";
      context.fillText(textX, x+5, 15);   
    }
}
function clearCanvas(img,canvas,context){
  context.clearRect(0, 0, canvas.width, canvas.height);
}
function drawImage(img,canvas,context){
  context.clearRect(0, 0, canvas.width, canvas.height);
  context.drawImage(img,0,0,canvas.width,canvas.height);
  // canvas.style.verticalAlign="middle";
// context.textAlign="center";   
  //changeBrightnessContrast(img,canvas,context);
  if( document.getElementById("showLightConstrast").style.display =='block'){
       changeBrightnessContrast(img,canvas,context);
   }
}


//获取图片
function getRadialImage(index_height){
    var locIndex_height =parseInt(getRealLocY(radialCanvas,index_height));
      //console.log("yyyyyyyyyyyyyyy坐标："+locIndex_height);
    imgRadial.src =ip + labeltastresult[fileindex].pic_image_field +"?label_dcm_taskId=" + labeltastresult[fileindex].id + "&&direct=RLSI"+"&&index="+ locIndex_height;
}

function getChordImage(index_width){
    var locIndex_width =parseInt(getRealLocX(chordCanvas,index_width));
    imgChord.src =ip + labeltastresult[fileindex].pic_image_field +"?label_dcm_taskId=" + labeltastresult[fileindex].id + "&&direct=APSI"+"&&index="+ locIndex_width;
}
imgRadial.onload = function(){
    radialCanvas.width = canvas.width;
    // radialCanvas.height = maxHeight;
    //调整画布大小
    // if ((imgRadial.width/imgRadial.height)<=(radialCanvas.width/radialCanvas.height)){
      // radialCanvas.width=radialCanvas.height * imgRadial.width / imgRadial.height;
    // }
    // else{
      radialCanvas.height=radialCanvas.width * imgRadial.height / imgRadial.width;
    // }
    initeDrawRadialLine();
    // brightnessContrast(imgRadial,radialCanvas,contextRadial);
}

imgChord.onload=function(){
    chordCanvas.width = canvas.width; 
    chordCanvas.height = maxHeight;
    //调整画布大小
    // if ((imgChord.width/imgChord.height)<=(chordCanvas.width/chordCanvas.height)){
      // chordCanvas.width=chordCanvas.height * imgChord.width / imgChord.height;
    // }
    // else{
      chordCanvas.height=chordCanvas.width * imgChord.height / imgChord.width;
    // }
    initeDrawChordLine();
    // brightnessContrast(imgChord,chordCanvas,contextChord);
}

function drawImageInite(img,canvas,context){
      // 清除画布，准备绘制
        context.clearRect(0, 0, canvas.width, canvas.height);
        context.drawImage(img,0,0,canvas.width,canvas.height);
// canvas.style.verticalAlign="middle";
       if( document.getElementById("showLightConstrast").style.display =='block'){
           changeBrightnessContrast(img,canvas,context);
       }
}

function textRadialDraw(){
  /**
  //邹安平：先不需要三维显示
  contextRadial.font="bolder 14px Arial ";
  contextRadial.fillStyle ="red";
  contextRadial.fillText("R", 10, radialCanvas.height/2);
  contextRadial.fillText("I", radialCanvas.width-15, radialCanvas.height/2);
  contextRadial.fillText("S", radialCanvas.width/2,15);
  contextRadial.fillText("I", radialCanvas.width/2, radialCanvas.height-10);
  //邹安平：先不需要三维显示
  **/
}

function textChordDraw(){
	 /**
  //邹安平：先不需要三维显示
  contextChord.font="bolder 14px Arial ";
  contextChord.fillStyle ="red";
  contextChord.fillText("A", 10, chordCanvas.height/2);
  contextChord.fillText("F", chordCanvas.width-15, chordCanvas.height/2);
  contextChord.fillText("S", chordCanvas.width/2,15);
  contextChord.fillText("I", chordCanvas.width/2, chordCanvas.height-10);
   //邹安平：先不需要三维显示
  **/
}
function textCrossDraw(){
	/**
  //邹安平：先不需要三维显示
  context.font="bolder 14px Arial ";
  context.fillStyle ="red";
  context.fillText("R", 10, canvas.height/2);
  context.fillText("I", canvas.width-15, canvas.height/2);
  context.fillText("A", canvas.width/2,15);
  context.fillText("P", canvas.width/2, canvas.height-10);
  //邹安平：先不需要三维显示
  **/
}


//切面1显示,横切面
function initeDrawCrossLine(){
    // drawImage(img,canvas,context);
    isCrossDraw=true;
    drawLine(canvas,context,cross_x,cross_y,isCrossDraw);
    isCrossDraw =false;
    textCrossDraw();
}
function crossDrawRect(x=null,y=null){x=null,y=null
    context.clearRect(0, 0, canvas.width, canvas.height);
    var imgWidth = canvas.width;
    var imgHeight = canvas.height;
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
// canvas.style.verticalAlign="middle";
   //changeBrightnessContrast(img,canvas,context);
   if( document.getElementById("showLightConstrast").style.display =='block'){
       changeBrightnessContrast(img,canvas,context);
   }

    drawRectShape();
    //遍历画所有Mask矩形框
    drawMaskShape(x,y);
 
    //遍历画所有的圆点
    drawPointShape();

}
function drawCrossRadialChord(e){
  cross_x = parseInt(e.pageX - getElementLeft(canvas));
  cross_y = parseInt(e.pageY - getElementTop(canvas));
  // drawImage(img,canvas,context);
  // drawRect();
  crossDrawRect();
  textCrossDraw();
  drawLine(canvas,context,cross_x,cross_y,isCrossDraw);
  //切面2
  // console.log("yyyyyyyyyyyyyyy坐标："+cross_y);
  getRadialImage(smallLocY(cross_y));
  isRadialDraw=true;
  radial_x=smallLocX(cross_x);
  drawImage(imgRadial,radialCanvas,contextChord);
  drawLine(radialCanvas,contextRadial,radial_x,radial_y,isRadialDraw);
  textRadialDraw();
  //切面3
  getChordImage(smallLocY(cross_x));
  isChordDraw=true;
  chord_x=smallLocX(cross_y);
  drawImage(imgChord,chordCanvas,contextChord);
  drawLine(chordCanvas,contextChord,chord_x,chord_y,isChordDraw);
  textChordDraw();
}

function startDrawCrossLine(e){
  isCrossDraw=true;
  drawCrossRadialChord(e);

}
function stopDrawCrossLine(e){
  // isRadialDraw=false;
  isCrossDraw=false;
}
function drawCrossLine(e){
  if (isCrossDraw){
    drawCrossRadialChord(e);
  }
}



//切面2显示，弦切面
function initeDrawRadialLine(){
    isRadialDraw=true;
    drawImageInite(imgRadial,radialCanvas,contextRadial); 
    drawLine(radialCanvas,contextRadial,radial_x,radial_y,isRadialDraw);
    isRadialDraw =false;
    textRadialDraw();

}


function drawRadialChord(e){
  radial_x = parseInt(e.pageX - getElementLeft(radialCanvas));
  radial_y = parseInt(e.pageY - getElementTop(radialCanvas));
  drawImage(imgRadial,radialCanvas,contextRadial);
  textRadialDraw();
  drawLine(radialCanvas,contextRadial,radial_x,radial_y,isRadialDraw);
  //径切面 切面3
  getChordImage(radial_x);
  isChordDraw=true;
  chord_y=radial_y;
  drawImage(imgChord,chordCanvas,contextChord);
  drawLine(chordCanvas,contextChord,chord_x,chord_y,isChordDraw);
  textChordDraw();
  //横切面，切面1
  // var getIndex = parseInt(getRealLocZ(radialCanvas,radial_y))+1;
  // var numInex = parseInt(getIndex/pageSize);
  // if(numInex>0){
  //   for (var i=0;i<numInex;i++){
  //     next();
  //   }
  // }
  // fileindex=getIndex-numInex*pageSize; 
  // cross_x = radial_x;
  // loadimg(fileindex);

}

function startDrawRadialLine(e){
  isRadialDraw=true;
  drawRadialChord(e);
}
function stopDrawRadialLine(e){
  isRadialDraw=false;
}
function drawRadialLine(e){
  if (isRadialDraw){
    drawRadialChord(e);
  }
}



// 切面3显示，径切面
function initeDrawChordLine(){
    isChordDraw=true;
    drawImageInite(imgChord,chordCanvas,contextChord);
    drawLine(chordCanvas,contextChord,chord_x,chord_y,isChordDraw);
    isChordDraw =false;
    textChordDraw();
}
function drawChordRadial(e){
  chord_x = parseInt(e.pageX - getElementLeft(chordCanvas));
  chord_y = parseInt(e.pageY - getElementTop(chordCanvas));
  drawImage(imgChord,chordCanvas,contextChord);
  drawLine(chordCanvas,contextChord,chord_x,chord_y,isChordDraw);
  textChordDraw();
  //切面2弦切面
  getRadialImage(chord_y);
  isRadialDraw=true;
  radial_y=chord_y;
  drawImage(imgRadial,radialCanvas,contextRadial);
  drawLine(radialCanvas,contextRadial,radial_x,radial_y,isRadialDraw);
  textRadialDraw();

    //横切面，切面1

  // var getIndex = parseInt(getRealLocZ(chordCanvas,chord_y))+1;
  // var numInex = parseInt(getIndex/pageSize);
  // if(numInex>0){
  //   for (var i=0;i<numInex;i++){
  //     next();
  //   }
  // }
  // fileindex=getIndex-numInex*pageSize;
  // loadimg(fileindex);
}
function startDrawChordLine(e){
    isChordDraw=true;
    drawChordRadial(e);
}
function stopDrawChordLine(e){
  isChordDraw=false;
}

function drawChordLine(e){
  if (isChordDraw){
      drawChordRadial(e);
  }
}



function drawPointShape(){
	for(var i = 0; i < pointShapes.length; i++){
		context.strokeStyle="yellow";
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
	
  for(var i=0; i<rects.length; i++) {
      var rect = rects[i];
      rectxywh = rect.getXYWH();
	  
	  x1y1x2y2 = rect.getX1Y1X2Y2();
	  
      // 绘制矩形
      if (rect.isSelected) {
        context.fillStyle = "rgba(97, 216, 162, 0.9)";
        //context.fillStyle = "rgba(10, 114, 6, 0.5)";
        //context.fillRect(bigLocX(rectxywh[0]),bigLocY(rectxywh[1]),bigLocX(rectxywh[2]),bigLocY(rectxywh[3]));
		context.fillRect(bigLocX(x1y1x2y2[0]),bigLocY(x1y1x2y2[1]),bigLocX(x1y1x2y2[2]) - bigLocX(x1y1x2y2[0]),bigLocY(x1y1x2y2[3]) - bigLocY(x1y1x2y2[1]));
        context.lineWidth = 3;
      }
      else if(rect.mouseonrect || rect.mouseonpoint){//鼠标移到矩形框中或者在矩形点上
        context.fillStyle = "rgba(97, 216, 162, 0.3)";
        //context.fillStyle = "rgba(10, 114, 6, 0.5)";
        //context.fillRect(bigLocX(rectxywh[0]),bigLocY(rectxywh[1]),bigLocX(rectxywh[2]),bigLocY(rectxywh[3]));
		context.fillRect(bigLocX(x1y1x2y2[0]),bigLocY(x1y1x2y2[1]),bigLocX(x1y1x2y2[2]) - bigLocX(x1y1x2y2[0]),bigLocY(x1y1x2y2[3]) - bigLocY(x1y1x2y2[1]));
        context.lineWidth = 3;
      }
      else{
        context.lineWidth = 2;
      }

      // if(rect.type == "inflammation"){
      //   context.strokeStyle = color_person[ i % 4];
      // }else{
      //   context.strokeStyle=color_dict[rect.type];   
      // }
      context.strokeStyle=color_dict["rect"];   //初始颜色
      selectColor(rects,i);
      //context.strokeRect(bigLocX(rectxywh[0]),bigLocY(rectxywh[1]),bigLocX(rectxywh[2]),bigLocY(rectxywh[3]),rect.color);

      context.strokeRect(bigLocX(x1y1x2y2[0]),bigLocY(x1y1x2y2[1]),bigLocX(x1y1x2y2[2]) - bigLocX(x1y1x2y2[0]),bigLocY(x1y1x2y2[3]) - bigLocY(x1y1x2y2[1]),rect.color);

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
        // if (onpoint(smallLocX(x),smallLocY(y),p)){
        if (mask.mouseonpoint){
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
		return true;
	}
	return false;
}

    
//交互
//画布内鼠标响应操作
function canvasClick(e) {
      // 取得画布上被单击的点
      console.log("屏幕上的点："+e.pageX+","+e.pageY);
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
      }
      if (tocreatemask){
    		showpopVar = true;
    		//hidePopup();
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
            // drawRect(x,y);
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
          // drawRect(x,y);
        }

        if((!found)&& (mouseonmask_mask!=null)){
          mouseonmask_mask.mouseonmask=false;
          mouseonmask_mask=null;
          // drawRect(x,y);
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
      }
      //判断是否开始拉伸
      if (stretch && SelectedpointId != null) {
        stretchrect(SelectedRect, SelectedpointId, smallLocX(x), smallLocY(y));
        drawRect();
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
     console.log("old_click_x1:"+old_click_x1+",old_click_y1:"+old_click_y1)
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
			console.log("create mask--show...13");
            //停止搜索
            return;
          }
      }
    // for(var i=pointShapes.length-1; i>=0; i--) {
    //     var pointShape = pointShapes[i];
    //     if (pointShape.isSelected && pointShape.finish){
    //        var new_region_id =i;
    //        regions = pointShapes;
    //        select_only_region(new_region_id);
    //        set_region_annotations_to_default_value(new_region_id);
    //        // var bound = mask.getBound();
    //        showPopup(bigLocY(pointShape.x)+5, bigLocX(pointShape.y)+5 ,i,"pointShapes");
    //        // showPopup(bigLocY(pointShape.x)+145, bigLocX(pointShape.y) + 235,i,"pointShapes");
    //         console.log("create mask--show...13");
    //       //停止搜索
    //       return;
    //     }
    // }
  }
};

//边栏
//任务信息
function showtaskinfo(){
  $('#task_info').text(label_task_info.task_name);
  $('#task_progress').text(label_task_info.task_status_desc);
  
}
var orderType = 1;
var findLast = 0;

function  showOrder(value){
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
//显示文件列表
function showfilelist(){
    var htmlstr="";
    for (var i=0;i<labeltastresult.length;i++){
       var fname = labeltastresult[i].pic_image_field.substring(labeltastresult[i].pic_image_field.lastIndexOf('/') + 1);
  	   if(!isEmpty(labeltastresult[i].pic_object_name)){
  		   fname = fname + "(" + labeltastresult[i].pic_object_name + ")";
  	   }
       var isfinished = labeltastresult[i].label_status;
       var lablebg=" style=\"cursor:pointer\"";
       var finish="未完成";
       if (isfinished=="0"){finish="已完成";}
       if (i==fileindex){lablebg=" style=\"background:#eee;color:#5a5a5a;cursor:pointer;\"";}
       var classStr = "";
       if (isfinished=="0"){
         // classStr = "class=\"btn btn-xs btn-success\"";
         classStr = "class=\"file-select\"";//
       }
       htmlstr = htmlstr+"<tr onclick=\"clickfilelist("+i+");\""+ lablebg+"> <td width=\"60\"" +"style=\"vertical-align:middle\""+ classStr + ">"+finish+"</td><td>"+ fname+ "</td></tr>";
    };
    document.getElementById("filelist").innerHTML=htmlstr;
}


function get_maskar(label_arr){
	 cls=label_arr.class_name;
	 var tmpMask = new maskar(getCanvasLocationX(label_arr.mask[0]),getCanvasLocationY(label_arr.mask[1]),cls);
			  
	 for(var j = 2; j < label_arr.mask.length; j+=2){
		tmpMask.points.push(new point(getCanvasLocationX(label_arr.mask[j]),getCanvasLocationY(label_arr.mask[j+1])));
	 }
			  
	 if(!isEmpty(label_arr.id)){
		tmpMask.id= label_arr.id;
	 }
	 if(!isEmpty(label_arr.blurred)){
		tmpMask.blurred = label_arr.blurred;
	 }
	 if(!isEmpty(label_arr.goodIllumination)){
		tmpMask.goodIllumination = label_arr.goodIllumination;
     }
	 if(!isEmpty(label_arr.frontview)){
		tmpMask.frontview = label_arr.frontview;
	 }
   if(!isEmpty(label_arr.other)){
       tmpMask.other = label_arr.other;
    }
   else if (isEmpty(label_arr.other)){ 
      tmpMask.other["region_attributes"]["type"] = tmpMask.type;
      tmpMask.other["region_attributes"]["id"] = tmpMask.id;
    }

    if (maxIdNum< parseInt(tmpMask.other["region_attributes"]["id"])){
        maxIdNum = parseInt(tmpMask.other["region_attributes"]["id"]);
    }
	 tmpMask.finish = true;
	 return tmpMask;
}

function get_rectar(label_arr){
	x1 = getCanvasLocationX(label_arr.box[0]);
	y1 = getCanvasLocationY(label_arr.box[1]);
	x2 = getCanvasLocationX(label_arr.box[2]);
	y2 = getCanvasLocationY(label_arr.box[3]);
	cls=label_arr.class_name;
	score = label_arr.score;
	rect = new rectar(x1,y1,x2,y2,cls,score);
	if(!isEmpty(label_arr.id)){
		rect.id= label_arr.id;
	}
	if(!isEmpty(label_arr.blurred)){
		rect.blurred = label_arr.blurred;
	}
	if(!isEmpty(label_arr.goodIllumination)){
		rect.goodIllumination = label_arr.goodIllumination;
	}
	if(!isEmpty(label_arr.frontview)){
		rect.frontview = label_arr.frontview;
	}
  if(!isEmpty(label_arr.other)){
    rect.other = label_arr.other;
  }
  else if (isEmpty(label_arr.other)){ 
    rect.other["region_attributes"]["type"] = rect.type;
    rect.other["region_attributes"]["id"] = rect.id;
  }
  if (maxIdNum< parseInt(rect.other["region_attributes"]["id"])){
      maxIdNum = parseInt(rect.other["region_attributes"]["id"]);
  }
	return rect;
}

function get_pointShape(label_arr){
	cls=label_arr.class_name;
	score = label_arr.score;
	var pointShapeObj = new pointShape(getCanvasLocationX(label_arr.keypoints[0]),getCanvasLocationY(label_arr.keypoints[1]),cls,score);
  if(!isEmpty(label_arr.other)){
     pointShapeObj.other = label_arr.other;
  }
  else if (isEmpty(label_arr.other)){
      pointShapeObj.other["region_attributes"]["type"] = pointShapeObj.type;
      pointShapeObj.other["region_attributes"]["id"] = pointShapeObj.id;
  }

  if (maxIdNum< parseInt(pointShapeObj.other["region_attributes"]["id"])){
      maxIdNum = parseInt(pointShapeObj.other["region_attributes"]["id"]);
  }
  return pointShapeObj;
}

	//this.id =""; //标识
	//this.blurred=false;//模糊不清的; 记不清的; 难以区分的; 模棱两可的
	//this.goodIllumination = true; //照明
	//this.frontview = true;//正面图
function parse_labelinfo(labelinfo,isclear){
	if(isclear){
	  rects.length = 0;
	  masks.length = 0;
	  pointShapes.length = 0;
	}
	if(!isEmpty(labelinfo)){
		console.log(labelinfo);
		var label_arr = JSON.parse(labelinfo);
		//console.log(label_arr);
		
		for(var i=0;i<label_arr.length;i++){
		  if(!isEmpty(label_arr[i].mask)){
			  var tmp = get_maskar(label_arr[i]);
			  var add = true;
			  if(!isclear){
				  //判断是否重复
				  for(var j = 0; j < masks.length; j++){
					  if(masks[j].isEqual(tmp)){
						  add = false;
					  }
				  }
			  }
			  if(add){
			     masks.push(tmp);
			  }
		  }else if(!isEmpty(label_arr[i].box)){
			  
			  var tmp = get_rectar(label_arr[i]);
			  var add = true;
			  if(!isclear){
				  //判断是否重复
				  for(var j = 0; j < rects.length; j++){
					  if(rects[j].isEqual(tmp)){
						  add = false;
					  }
				  }
			  }
			  if(add){
			     rects.push(tmp);
			  }
		      
		 }else if(!isEmpty(label_arr[i].keypoints)){
			 var tmp = get_pointShape(label_arr[i]);
			 var add = true;
			 if(!isclear){
				  //判断是否重复
				  for(var j = 0; j < pointShapes.length; j++){
					  if(pointShapes[j].isEqual(tmp)){
						  add = false;
					  }
				  }
			  }
			 if(add){
			     pointShapes.push(tmp);
			 }
		 }
	    }
		//console.log(rects);
		//console.log(masks);
	}
} 


function getHtmlStr(shape,shapetype){
	 var lablebg="";
     if (shape.isSelected){
		 lablebg=" style=\"background:#eee;color:#5a5a5a;\"";
	 }
	 var shapescore = "";
	 if(!isEmpty(shape.score)){
		 shapescore = shape.score;
	 }
     return "<tr"+ lablebg+"><td style=\"width:10px\"></td> <td>"+ shape.type+ "</td><td>" + shapetype + "</td><td>" + shapescore + "</td></tr>";
}

function boxlabelshtml(){
  var htmlstr="";
  for (var i=0;i<rects.length;i++){
     htmlstr = htmlstr + getHtmlStr(rects[i],'bbox');
  }
  for (var i=0;i<masks.length;i++){
    htmlstr = htmlstr + getHtmlStr(masks[i],'mask');
  }
  for(var i = 0; i < pointShapes.length; i++){
	 htmlstr = htmlstr + getHtmlStr(pointShapes[i],'point');
  }
  return htmlstr;
}

function boxlabelCount(labelMap,type){
	 if(labelMap[type] == null){
        labelMap[type] = 1;
     }else{
        labelMap[type] = labelMap[type] + 1;
     }
}

function boxlabelcounthtml(){
  var htmlstr="";
  var labelMap = {};
  for (var i=0;i<rects.length;i++){
	 boxlabelCount(labelMap,rects[i].type);
  }
  for (var i=0;i<masks.length;i++){
	 boxlabelCount(labelMap,masks[i].type);
  }
  for (var i=0;i<pointShapes.length;i++){
	 boxlabelCount(labelMap,pointShapes[i].type);
  }
  for (item in labelMap){
    htmlstr = htmlstr + "<tr><td style=\"width:10px\"></td> <td>"+ item + "</td><td>" + labelMap[item] +"</td></tr>";
  }
  return htmlstr;
}

//x,y,z,相对于横切面而言，z为图像序列，x：横切面图像的宽，y:横切面图像的高
function getRealLocX(canvas,num){
  var loc = Math.round(num*sliceW/canvas.width);
  if(loc < 0){
        loc = 0;
    }
  if(loc >=sliceW){
    loc = sliceW -1;
  }
  return loc;
}
function getRealLocY(canvas,num){
  var loc = Math.round(num*sliceH/canvas.height);
  if(loc < 0){
        loc = 0;
    }
  if( loc >= sliceH){
    loc = sliceH -1;
  }
  return loc;
}

function getRealLocZ(canvas,num){
  var loc = Math.round(num*sliceN/canvas.height);
  if(loc < 0){
        loc = 0;
    }
  if( loc >= sliceH){
    loc = sliceN -1;
  }
  return loc;
}

function getCanvasLocY(canvas,num){
  var locCanvas=Math.round(num*canvas.height/parseInt(img.height));
  if (locCanvas<=0){
     locCanvas = 1;
  }
  if( locCanvas > canvas.height){
    locCanvas = canvas.height -1;
  }

  return locCanvas;
}

function getRealLocationX(num){
	var loc = Math.round(num*img.width/canvas.width);
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

function loadModel(){

    $.ajax({
      type:"GET",
      contentType:'application/json',
      url: ip + "/api/queryAlgModelContainWiseMedical/",
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

function loadData(){
    $.ajax({
      type:"GET",
      contentType:'application/json',
      url: ip + "/api/query-three-dcm",
      dataType:"json",
      data:{
       'label_task_id':label_task_info.id,
       },
      async:false,
      headers: {
         // Accept: "text/html; q=1.0", 
         authorization:token,
       },
      // enctype:"multipart/form-data",
      success:function(jsonData){
       console.log(jsonData);
	   //console.log("dataLength = " + jsonData[0].dotList.length);
       dotListStr = jsonData;       
      },
	  error:function(response) {
		  redirect(response);
      }
    });
   return dotListStr;
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
  document.getElementById('predict_model').innerHTML=html; 
}


function submit_predtask(){
  var item_id = labeltastresult[fileindex].id;
  
  var predict_model_val = $('#predict_model').val();
  
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
                              'task_type':"1",
               }),        
           success:function(res){
             console.log('创建数据信息');
          },
	      error:function(response) {
		    redirect(response);
          }
         });
     $("#autoLabel").modal('hide');
     setIntervalToDo();
}

var timeId;
var count;

function setIntervalToDo(){
  count=0;
  timeId = self.setInterval("clock()",1000);//5秒刷新
  console.log("开始刷新。timeId=" + timeId);
}

function clock(){
   count++;
   if(count > 8 ){
     console.log("清除定时器。timeId=" + timeId);
     window.clearInterval(timeId);
     timeId = null;
   }
   var current = $('#displayPage1').text();
   console.log("开始刷新。current=" + current);
   if(current >= 1){
       pageReload(current - 1,pageSize,fileindex);
       if(!isEmpty(labeltastresult[fileindex].label_info)){
             window.clearInterval(timeId);
             timeId = null;
       }
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
       label['id'] = i;
    }else{
     label['id'] = rects[i].id; 
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
      labelMaskList.push(getRealLocationX(masks[i].points[j].x));
      labelMaskList.push(getRealLocationY(masks[i].points[j].y));
    }
    label['mask'] = labelMaskList;
    label['id'] = masks[i].id;
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
   
    pointList.push(getRealLocationX(pointShapes[i].x));
    pointList.push(getRealLocationY(pointShapes[i].y));
    pointList.push(2);//0表示这个关键点没有标注（这种情况下x=y=v=0），1表示这个关键点标注了但是不可见(被遮挡了），2 表示这个关键点标注了同时也可见
    label['keypoints'] = pointList;
    label['id'] = pointShapes[i].id;
    label['blurred']=pointShapes[i].blurred;
    label['goodIllumination']=pointShapes[i].goodIllumination;
    label['frontview']=pointShapes[i].frontview;
      label['other']=pointShapes[i].other;
      console.log(label);
    label_list.push(label);
  }
  
  labelinfo_jsonstr = JSON.stringify(label_list);
  labeltastresult[fileindex].label_info=labelinfo_jsonstr;
  console.log("labeltastresult[fileindex].id=" + labeltastresult[fileindex].id + "   json=" + labelinfo_jsonstr);
  if(label_list.length > 0){
     labeltastresult[fileindex].label_status=0;
  }
  $.ajax({
       type:"PATCH",
	   contentType:'application/json',
       url:ip + "/api/label-task-dcm-item/",
       headers: {
          authorization:token,
        },
       dataType:"json",
       async:false,
	   data:JSON.stringify({'id':labeltastresult[fileindex].id,
                            'label_info':labelinfo_jsonstr,
                            'label_status':"0",
                            'pic_object_name' : img.width + "," + img.height
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
        //console.log(label_task_info);
		page(0,pageSize);
      },
	  error:function(response) {
		  redirect(response);
      }
   });
}

function showTaskFlowVerify(){
	var html = "";
	if(label_task_info.task_flow_type == 2){
		//审核者模式
	    html += "<span>勾选某个任务显示其标注信息：</span>";
		var relate_other_label_task = label_task_info.relate_other_label_task;
		if(!isEmpty(relate_other_label_task)){
		    var other_label_task_info = JSON.parse(relate_other_label_task);
			for(var key in other_label_task_info){
                 html += "<input type=\"checkbox\" name=\"category\" value=\"" + key + "\" onclick=\"click_this(this);\"/><span>" + other_label_task_info[key]  + "</span>"
　　　　　　}
            console.log("html =" + html);
		}
	}
	document.getElementById("task_flow_verify").innerHTML=html;
}

var otherLabelInfo;
function click_this(event){
	console.log("label taskid=" + event.value);
	console.log("label check=" + event.checked);
	var check = event.checked;
	var labeltaskid=  event.value;
	var image_path = labeltastresult[fileindex].pic_image_field;
	
	if(check){
	   if(isEmpty(otherLabelInfo) || isEmpty(otherLabelInfo[image_path])){
		  var picImageList = [];
		  for(var i = 0; i < labeltastresult.length; i++){
			 picImageList.push(labeltastresult[i].pic_image_field);
		  } 
		  getOtherUserLabelInfo(labeltaskid,picImageList);
	   }

	   //console.log("bb==" + otherLabelInfo[labeltaskid]);
	
	   if(!isEmpty(otherLabelInfo) && !isEmpty(otherLabelInfo[labeltaskid])){
		  var tmpTaskInfo = otherLabelInfo[labeltaskid];
		  var tmpLabelInfo = tmpTaskInfo[image_path];
		  if(!isEmpty(tmpLabelInfo)){
		     parse_labelinfo(tmpLabelInfo,false);
		     drawRect();
		  }
	   }
	}else{
		if(!isEmpty(otherLabelInfo) && !isEmpty(otherLabelInfo[labeltaskid])){
			var tmpTaskInfo = otherLabelInfo[labeltaskid];
		    var tmpLabelInfo = tmpTaskInfo[image_path];
			if(!isEmpty(tmpLabelInfo)){
                //删除矩形框或者mask			    
				var label_arr = JSON.parse(tmpLabelInfo);
		        for(var i=0;i<label_arr.length;i++){
		          if(!isEmpty(label_arr[i].mask)){
					  //删除mask
			          var tmpMask = get_maskar(label_arr[i]);
			          for(var j = 0; j < masks.length; j++){
				          if(tmpMask.isEqual(masks[j])){
							  masks.splice(j, 1);
							  break;
						  }
			          }
			      }
				  else if(!isEmpty(label_arr[i].box)){
					  var tmpRect = get_rectar(label_arr[i]);
			          for(var j = 0; j < rects.length; j++){
				          if(tmpRect.isEqual(rects[j])){
							  rects.splice(j, 1);
							  break;
						  }
			          }
				  }
				  else if(!isEmpty(label_arr[i].keypoints)){
					  var tmpPoint = get_pointShape(label_arr[i]);
			          for(var j = 0; j < pointShapes.length; j++){
				          if(tmpPoint.isEqual(pointShapes[j])){
							  pointShapes.splice(j, 1);
							  break;
						  }
			          }
				  }
				}
			}
			drawRect();
		}
	}
}

function getOtherUserLabelInfo(labeltaskid,picImageList){
	 var picImageListStr = JSON.stringify(picImageList);
	 $.ajax({
       type:"GET",
       url:ip + "/api/label-task-item-pic/",
       headers: {
          authorization:token,
        },
       dataType:"json",
	   data:{
		   'label_task':labeltaskid,
		   'picImageListStr':picImageListStr
		   },
       async:false,
       success:function(json){
		 otherLabelInfo = {};
		 var tmpLabelMap = {};
		 for(var i = 0; i < json.length; i++){
			 tmpLabelMap[json[i].pic_image_field] = json[i].label_info;
		 }
		 otherLabelInfo[labeltaskid] = tmpLabelMap;
         //console.log(json);
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
		   'pageSize':pageSize
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
  
    var pageNum = parseInt(pageData.total/pageSize);
    if(pageData.total%pageSize!=0){
        pageNum += 1;
    }else {
        pageNum = pageNum;
    }
   $("#totalPageNum").text(pageNum);

}
   var   labelwindow = document.getElementById("labelwin");
   labelwindow.style.width = parseInt(document.getElementById("tool0").offsetWidth*0.95)+'px';
   labelwindow.style.height = parseInt(document.getElementById("tool0").offsetWidth*0.6)+'px';
   labelWidth=parseInt(document.getElementById("tool0").offsetWidth*0.95);
   labelheight=parseInt(document.getElementById("tool0").offsetWidth*0.6);

  var maxWidth=parseInt(labelWidth/2-10);
  var maxHeight=parseInt(labelheight/2-10);
  var ishiddernav=false;
  window.onload = function() {
  
    console.log("onload tasks/detect/index.html");
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
	
	var medical_flag = getSessionStorageMessage("medical_flag");
	console.log("medical_flag=" + medical_flag);
	if(!isEmpty(medical_flag) && !ishiddernav){
	   ishiddernav = true;
	   $('.left-side').toggleClass("collapse-left");
       $(".right-side").toggleClass("strech");
       document.getElementById("hiddenLeft").style.display="none";
	   document.getElementById("logotitle").style.display="none";
	}

    labelwindow = document.getElementById("labelwin");
    labelwindow.style.width = parseInt(document.getElementById("tool0").offsetWidth*0.95)+'px';
    labelwindow.style.height = parseInt(document.getElementById("tool0").offsetWidth*0.6)+'px';
    labelWidth=parseInt(document.getElementById("tool0").offsetWidth*0.95);
    labelheight=parseInt(document.getElementById("tool0").offsetWidth*0.6);
    console.log("labelWidth:"+labelWidth);
    // $("#labelwin").jScrollPane();
    // $('#labelwin').mousewheel(function(event, delta, deltaX, deltaY) {
    //   if (window.console && console.log) {
    //     console.log(delta, deltaX, deltaY);
    // }
    // }); 
    //maxWidth=parseInt(labelWidth/2-10);
    //maxHeight=parseInt(labelheight/2-10);
	
	maxWidth=parseInt(labelWidth-50);
    maxHeight=parseInt(labelheight-50);
	
    canvas = document.getElementById("myCanvas");
    context = canvas.getContext("2d");
    canvas.width = maxWidth;
    canvas.height = maxHeight;


    /**
    //邹安平：先不需要三维显示
    divMyCanvas = document.getElementById("divMyCanvas");
    divMyCanvas.style.width=maxWidth+'px';
    divMyCanvas.style.height=maxHeight+'px';

    divRadial = document.getElementById("divRadial");
    divRadial.style.width=maxWidth+'px';
    divRadial.style.height=maxHeight+'px';


    divChord = document.getElementById("divChord");
    divChord.style.width=maxWidth+'px';
    divChord.style.height=maxHeight+'px';
   
    divRender = document.getElementById("divRender");
    divRender.style.width=maxWidth+'px';
    divRender.style.height=maxHeight+'px'; 

    radialCanvas = document.getElementById("radialCanvas");
    contextRadial = radialCanvas.getContext("2d");
    radialCanvas.width = maxWidth;
    radialCanvas.height = maxHeight;

    
    chordCanvas = document.getElementById("chordCanvas");
    contextChord = chordCanvas.getContext("2d");
    contextChord.textAlign="end";
    chordCanvas.width = maxWidth;
    chordCanvas.height = maxHeight;
	//邹安平：先不需要三维显示
   **/
   
    CTtools = document.getElementById("CTtools");
    CTtools.style.width=parseInt(document.getElementById("tool0").offsetWidth*0.28)+ 'px';
    CTtools.style.height=parseInt(parseInt(document.getElementById("tool0").offsetWidth*0.08)/2-5)*2+'px';
    CTtools.style.left=labelWidth+'px'
	
	console.log("width="+ CTtools.style.width);
	console.log("height="+ CTtools.style.height);
	
    // renderCanvas = document.getElementById("renderCanvas");
    // renderCanvas.style.width = maxWidth+'px';
    // renderCanvas.style.height = renderCanvas.width/1280*720;
    get_labeltask();
  
    showTaskFlowVerify();
    drawRect();
    //canvasAdapt();
	
	/**
	//邹安平：先不需要三维显示
    radial_y = parseInt(radialCanvas.height/2);
    radial_x= parseInt(radialCanvas.width/2);
    chord_y = parseInt(chordCanvas.height/2);
    chord_x= parseInt(chordCanvas.width/2);
    cross_y = parseInt(canvas.height/2);
    cross_x= parseInt(canvas.width/2);
    getChordImage(chord_x);
    getRadialImage(radial_y);
	
    initeDrawRadialLine();
    initeDrawChordLine();
	
	radialCanvas.onmousedown=startDrawRadialLine;
    radialCanvas.onmouseup=stopDrawRadialLine;
    radialCanvas.onmousemove = drawRadialLine;
    radialCanvas.onmouseout = stopDrawRadialLine;

    chordCanvas.onmousedown=startDrawChordLine;
    chordCanvas.onmouseup = stopDrawChordLine;
    chordCanvas.onmousemove = drawChordLine;
    chordCanvas.onmouseout = stopDrawChordLine;
	//邹安平：先不需要三维显示
	**/
    // initeDrawCrossLine();
    canvas.onmousedown = canvasClick;
    canvas.onmouseup = stopDragging;
    canvas.onmouseout = stopDragging;
    canvas.onmousemove = dragRect; 

    _via_init();

    label_attributes = JSON.parse(label_task_info.task_label_type_info);
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

   /**
   //邹安平：先不需要三维显示
   ctRender();
   //邹安平：先不需要三维显示
   **/
};

 function setLightContrast(){


    if (clickNums%2 == 0){

        var widthContrast = document.getElementById("panel_light").offsetWidth-30;
      // getInt();

        var end_num = 255;
        var start_num = -255;

        $('.range-slider-light').jRange({
            from: start_num,
            to: end_num,
            step: 1,
            // scale: [0,25,50,75,100],
            format: '%s',
            width: widthContrast,
            showLabels: true,
            // isRange : true
             onstatechange:function () {
               
                slider();
          },
          });

          end_num = 5;
          start_num = -5;
          $('.range-slider-contrast').jRange({
            from: start_num,
            to: end_num,
            step: 0.1,
            // scale: [0,25,50,75,100],
            format: '%s',
            width: widthContrast,
            showLabels: true,
            // isRange : true
             onstatechange:function () {
               slider();
          },
          });
            document.getElementById("showLightConstrast").style.display="block";
          } 
          else
          {
            document.getElementById("showLightConstrast").style.display="none";
          }

         clickNums=clickNums+1;   
    
  }

var beta;
var alpha;

function slider(){
         img.onload();
         imgChord.onload();
         imgRadial.onload();
}
function changeBrightnessContrast(img,canvas,context){
       
        alpha=parseFloat($(".range-slider-contrast").val());
        beta=parseInt($(".range-slider-light").val());
        var imgData=context.getImageData(0,0,canvas.width,canvas.height);
        dataChange(imgData);
        context.putImageData(imgData, 0, 0);
}

function dataChange(imgData){
          for (var i = 0; i < imgData.data.length; i += 4) {
            imgData.data[i] = parseInt(alpha * imgData.data[i]+beta); //R(0-255)
            if (imgData.data[i]>=255){
              imgData.data[i] = 255;
            }
            // console.log("type:",typeof(imgData.data[i]));
            if (imgData.data[i]<=0){
              imgData.data[i] = 0;
            }

            imgData.data[i+1] = parseInt(alpha *imgData.data[i+1]+beta); //R(0-255)
            if (imgData.data[i+1]>=255){
              imgData.data[i+1] = 255;
            }
            if (imgData.data[i+1]<=0){
              imgData.data[i+1] = 0;
            }

            imgData.data[i+2] = parseInt(alpha *imgData.data[i+2]+beta); //R(0-255)
            imgData.data[i+3] =(imgData.data[i+3]);
            if (imgData.data[i+2]>=255){
              imgData.data[i+2] = 255;
            }
            if (imgData.data[i+2]<=0){
              imgData.data[i+2] = 0;
            }
        }
}


function drawContrastLine(){
  var contrastConvas = document.getElementById("canvasContrastLine");
  var contrastContext = contrastConvas.getContext("2d");
  contrastContext.font = '16px Mono';
  // contrastContext.fillStyle = 'white';
  // contrastContext.fillRect(0, 0,
  //                                  canvasTimeLineMark.width,
  //                                  canvasTimeLineMark.height);

  // draw arrow
  contrastContext.fillStyle = 'black';
  contrastContext.strokeRect(0,0, contrastConvas.width,5);
  // contrastContext.beginPath();
  // contrastContext.moveTo(0, contrastConvas);
  // contrastContext.lineTo(cx - linehb2, lineh);
  // contrastContext.lineTo(cx + linehb2, lineh);
  // contrastContext.moveTo(cx, linehn[2]);
  // contrastContext.fill();
}
function set3DModel(){
    statesLine = true;
    initeDrawCrossLine();
    loadimg(fileindex);
    canvas.onmousedown = startDrawCrossLine;
    canvas.onmouseup = stopDrawCrossLine;
    canvas.onmouseout = stopDrawCrossLine;
    canvas.onmousemove = drawCrossLine; 
    ctRender();
}
var sliceW;
var sliceH;
var sliceN;
function canvasAdapt(){
    
    var tmpWH = labeltastresult[fileindex]['pic_object_name'].split(",");
    sliceW = tmpWH[0];
    sliceH =tmpWH[1];
    sliceN = labeltastresult[fileindex]['display_order1'];
    canvas.width  = maxWidth;
    canvas.height = maxHeight;
	
	
    //调整画布大小
    if ((sliceW/sliceW)<=(canvas.width/canvas.height)){
      canvas.width=canvas.height * sliceW / sliceH;
    }
    else{
      canvas.height=canvas.width * sliceH / sliceW;
    }
	canvas.style.marginTop        = (maxHeight- canvas.height)/2+'px';
    canvas.style.marginLeft       = (maxWidth- canvas.width)/2+'px';
	
	
   
    /**
	//邹安平：先不需要三维显示
    radialCanvas.width = canvas.width;
    radialCanvas.height=radialCanvas.width * sliceN / sliceW;
    
    chordCanvas.width = canvas.width; 
    chordCanvas.height=chordCanvas.width * sliceN / sliceH;
    
    radialCanvas.style.marginTop  = (maxHeight- radialCanvas.height)/2+'px';
    chordCanvas.style.marginTop   = (maxHeight- chordCanvas.height)/2+'px';
    
    radialCanvas.style.marginLeft = (maxWidth- radialCanvas.width)/2+'px';
    chordCanvas.style.marginLeft  = (maxWidth- chordCanvas.width)/2+'px';
	//邹安平：先不需要三维显示
	**/
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
       _via_attributes['region'][atti]["options"]["1"] = "inflammation";
       _via_attributes['region'][atti]["options"]["2"] = "vessel";
       _via_attributes['region'][atti]["default_options"] = {};

  }
  img.onload = function(){
    // canvas.width = img.width;
    // canvas.height = img.height;
        // 初始设置画布大小，最大值宽和高
    canvas.width = maxWidth;
    canvas.height = maxHeight;
    //调整画布大小
    if ((img.width/img.height)<=(canvas.width/canvas.height)){
      canvas.width=canvas.height * img.width / img.height;
    }
    else{
      canvas.height=canvas.width * img.height / img.width;
    }
    // divRadial.style.marginLeft=maxWidth-canvas.width+'px';
    maxIdNum=0;
    parse_labelinfo(labeltastresult[fileindex].label_info,true);
	
	  var tmp = document.getElementsByName("category");
	   if(!isEmpty(tmp)){
  	   console.log("tmp.length=" + tmp.length);
  	   for(var i = 0; i <tmp.length; i++){
  		  if(tmp[i].checked){
  			  labeltaskid = tmp[i].value;
  			  image_path = labeltastresult[fileindex].pic_image_field;
                if(!isEmpty(otherLabelInfo) && !isEmpty(otherLabelInfo[labeltaskid])){
  		          var tmpTaskInfo = otherLabelInfo[labeltaskid];
  		          var tmpLabelInfo = tmpTaskInfo[image_path];
  		          if(!isEmpty(tmpLabelInfo)){
  		             parse_labelinfo(tmpLabelInfo,false);
  		          }
  	          }
		    }			   
	     }
	   }
     drawRect();
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

  };

  


  var init_chord_x;
  var init_radial_y;
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





// ////线
// 
function dist(dot1,dot2) {
 var distxy = Math.abs(dot1.x - dot2.x) +  Math.abs(dot1.y - dot2.y) ;
 return distxy;
  // body...
}


var total=[];


function extractline(circle1,circle2){
    var minDis=100000;
    var minMidIndex = 0;
    var geometry = new THREE.Geometry(); 
    var tmpPoints = [];
    for(var kk=0;kk<circle2.length;kk++){
      var disDot=dist(circle1[0],circle2[kk]);
      if (disDot<minDis){
          minDis=dist(circle1[0],circle2[kk]);
          minMidIndex=kk;
          // break;
      }
    }
    tmpPoints.push(circle1[0]);
    tmpPoints.push(circle2[minMidIndex]);
      // addLine(circle1[0],circle2[minMidIndex]);

    for(var j=1; j<circle1.length;j++){
      var minDis=100000;
      var secMinDis=100000;
      var minIndex = 0;
      var secMinIndex = 0;
      var start_ = Math.max(0,minMidIndex-5);
      var end_=Math.min(circle2.length,minMidIndex+5);

      for(var kk=start_;kk<end_;kk++){
        var disDot=dist(circle1[j],circle2[kk]);
        if (disDot<minDis){
            minDis=disDot;
            minIndex=kk;
        }
      }
      minMidIndex= minIndex;
      tmpPoints.push(circle1[j]);
      tmpPoints.push(circle2[minIndex]);
      // addLine(circle1[j],circle2[minIndex]);
    } 
    addLines(tmpPoints);

}

function extractLineModel_new(){
  for (var i=0; i< total.length-1;i++){
   var circle1 = total[i];
   var circle2 = total[i+1];
    extractline(circle1,circle2);
    extractline(circle2,circle1);
  }
}


// function extractLineModel(){
//   for (var i=0; i< total.length-1;i++){
//    var circle1 = total[i];
//    var circle2 = total[i+1];

//     for(var j=0; j<circle1.length;j++){
//       var minDis=100000;
//       var secMinDis=100000;
//       var minIndex = -1;
//       var secMinIndex = -1;

//       for(var kk=0;kk<circle2.length;kk++){
//         var disDot=dist(circle1[j],circle2[kk]);
//         if (disDot<minDis){
//             minDis=dist(circle1[j],circle2[kk]);
//             minIndex=kk;
//         }
//       }
//       var  maxScope = Math.min(circle2.length,minIndex+5);
//       for(var kk=Math.max(0,minIndex-5);kk<maxScope;kk++){
    
//         var disDot=dist(circle1[j],circle2[kk]);
//         if (disDot<secMinDis && kk!=minIndex){
//             secMinDis=dist(circle1[j],circle2[kk]);
//             secMinIndex=kk;
//         }
//       }

//          // if(dist(circle1[j],circle2[kk])<=4){
//           // console.log("xy坐标：("+circle1[j].x+','+circle1[j].y+','+circle1[j].z+'),('+circle2[kk].x+','+circle2[kk].y+','+circle2[kk].z+')');
//           addLine(circle1[j],circle2[minIndex]);
//           addLine(circle1[j],circle2[secMinIndex]);
//     } 
//   }
// }

function addLines(points){
  var geometry = new THREE.Geometry();
  geometry.vertices=points;

  var material = new THREE.LineBasicMaterial({
    color: 0xFF0000
  });
  var line = new THREE.Line(geometry, material);
  scene.add(line); //线条对象添加到场景中
}
// function addLine(point1,point2){
//     var geometry = new THREE.Geometry();
//     geometry.vertices.push(point1);
//     geometry.vertices.push(point2);

//     var material = new THREE.LineBasicMaterial({
//       color: 0xFF0000
//     });
//     var line = new THREE.Line(geometry, material);
//     scene.add(line); //线条对象添加到场景中
// }

// function addPoint(point){
//     var geometry = new THREE.Geometry();
//     geometry.vertices.push(point);
//     var material = new THREE.PointsMaterial({
//       size: 2,
//     color: 0xFF0000});
//     var points = new THREE.Points(geometry, material);
//     scene.add(point); //线条对象添加到场景中
// }




var minX;
var minY;

function getMinxy(dotList){

    minX=dotList[0][0].x;
    minY=dotList[0][0].y;

    var num_len = dotList.length;
    for (var num = 0;num<num_len;num++){
        var N = dotList[num].length; //分段数量
         for (var i = 0; i < N; i++) {
              var angle = 2 * Math.PI / N * i;
              var x = dotList[num][i].x;
              var y = dotList[num][i].y;
              if (x<minX){
                minX=x;
              }
              if (y<minY){
                minY=y;
              }
        }
    }
}
 // 构造点线面
function getData(scene,dotList,isPoint=true,isLine=true){


    var num_len= dotList.length;
    var one_data=[];
    for (var num = 0;num<num_len;num++){
        var geometry = new THREE.Geometry(); //声明一个几何体对象Geometry

        var totalxy=[];

        var N = dotList[num].length; //分段数量
        // 批量生成圆弧上的顶点数据
        for (var i = 0; i < N; i++) {
              // var angle = 2 * Math.PI / N * i;
              var x = (dotList[num][i].x-minX);
              var y = (dotList[num][i].y-minY);
              geometry.vertices.push(new THREE.Vector3(x, y, num));
             
              totalxy.push(new THREE.Vector3(x, y, num/2));
              // mydata.push(x*10,y*10,num*10);
        }
       var len = geometry.vertices.length;
        geometry.vertices.push(geometry.vertices[len-N]);
        if (isLine){
                            //材质对象
          var material = new THREE.LineBasicMaterial({
            color: 0xFF0000
          });
          //线条模型对象
          var line = new THREE.Line(geometry, material);
          scene.add(line); //线条对象添加到场景中
        }
        if (isPoint){
          var material_points = new THREE.PointsMaterial({
            size: 1,
            color: 0xFF0000,
            transparent:true,//是否透明
            opacity : 0.5,//透明度
        });
          var points = new THREE.Points(geometry, material_points);
          scene.add(points); //线条对象添加到场景中
        }
        one_data.push(totalxy);
    }
    return one_data;
    // console.log("alldata:"+mydata);
}




function ctRender(){

    document.getElementById('divRender').innerHTML="";
    /**
     * 创建场景对象Scene
    //  */

    
    var scene = new THREE.Scene();
    // /**
    // /**
    //  * 光源设置
    //  */
    //点光源
    var pointLight = new THREE.PointLight(0x4488ee);
    pointLight.position.set(400, 200, 300); //点光源位置
    scene.add(pointLight); //点光源添加到场景中
    // var point = new THREE.PointLight(0x4488ee);
    pointLight.position.set(-400, -200, 300); //点光源位置
    scene.add(pointLight); //点光源添加到场景中
    pointLight.position.set(400, -200, 300); //点光源位置
    scene.add(pointLight); //点光源添加到场景中
    pointLight.position.set(-400, 200, 300); //点光源位置
    scene.add(pointLight); //点光源添加到场景中

// 
    // 环境光
    var ambient = new THREE.AmbientLight(0xffffff);
    scene.add(ambient);

     // var data_3d="http://192.168.62.16:8080/api/query-three-dcm?label_task_id=075ec5606d804af4832f8ab26a1bf671";
    var  data_3d = loadData();

    if (!isEmpty(data_3d)){

      var dotList=data_3d[0].dotList;
      // dotList.length=3;
      getMinxy(dotList);
      // linePointModel(isPoint=true,isLine=true);
      var timeDate= new Date();
      var startTime=timeDate.getTime();
      for (i=0;i<data_3d.length;i++){
          dotList = data_3d[i].dotList;
          var onetmp_data=getData(scene,dotList,isPoint=true,isLine=false);
          total.push(onetmp_data);
      }
    }
    var endTime = new Date().getTime();
    console.log('getData耗时：'+(endTime-startTime)/1000+"秒")
    // extractLineModel_new();
    console.log('extractLineModel:'+(new Date().getTime()-endTime)/1000+"秒");
    var extractLineTime = new Date().getTime();
    // extractMeshModel1();
  /**
     * 相机设置
     */
//    var axisHelper = new THREE.AxisHelper(250); //坐标轴显示
//    scene.add(axisHelper);
    var width = maxWidth; //窗口宽度
    var height = maxHeight; //窗口高度
    var k = width / height; //窗口宽高比
    var s = 200; //三维场景显示范围控制系数，系数越大，显示的范围越大
    //创建相机对象
    var camera = new THREE.OrthographicCamera(-s * k, s * k, s, -s, 1, 1000);
    camera.position.set(maxWidth/2, maxHeight/2, 300); //设置相机位置
    camera.lookAt(scene.position); //设置相机方向(指向的场景对象)
    /**
     * 创建渲染器对象
     */
    var renderer = new THREE.WebGLRenderer();
    renderer.setSize(width, height);//设置渲染区域尺寸
    renderer.setClearColor(0x000000, 1); //设置背景颜色0xb9d3ff
    document.getElementById('divRender').appendChild(renderer.domElement);
    // 执行渲染操作   指定场景、相机作为参数
    render();
    var controls = new THREE.OrbitControls(camera,renderer.domElement);//创建控件对象
    controls.addEventListener('change', render);//监听鼠标、键盘事件
    console.log('render:'+(new Date().getTime()-extractLineTime)/1000+"秒");
    function render() {
        renderer.render(scene,camera);//执行渲染操作
    }
}