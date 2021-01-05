//二级导航
$(function(){
	$(".nav li").hover(function(){
		$(this).find("a:first").addClass("hov");
		$(this).find("div").slideDown(300);
	},function(){
		$(this).find("a:first").removeClass("hov");
		$(this).find("div").stop().slideUp(300);
	});
});

//tab内容切换
$(function(){
	var bp = $(".show-cont .show-cont-box");
	var sp = $(".show-tabs a");
	$('.show-tabs a:first').addClass("on");
	$('.show-cont .show-cont-box:first').show();
	sp.click(function(){
		i = $(this).index();
		if(bp.eq(i).css("display")!='list-item'){				
		bp.hide(),
		bp.eq(i).show(),
		sp.removeClass("on"),
		$(this).addClass("on")}
	});	
});

//幻灯片
(function($){
	$.fn.WIT_SetTab=function(iSet){
		/*
		 * Nav: 导航钩子；
		 * Field：切换区域
		 * K:初始化索引；
		 * CurCls：高亮样式；
		 * Auto：是否自动切换；
		 * AutoTime：自动切换时间；
		 * OutTime：淡入时间；
		 * InTime：淡出时间；
		 * CrossTime：鼠标无意识划过时间
		 * Ajax：是否开启ajax
		 * AjaxFun：开启ajax后执行的函数
		 */
		iSet=$.extend({Nav:null,Field:null,K:0,CurCls:'cur',Auto:false,AutoTime:5000,OutTime:800,InTime:800,CrossTime:100},iSet||{});
		var acrossFun=null,hasCls=false,autoSlide=null;
		//切换函数
		function changeFun(n){
			iSet.Field.filter(':visible').fadeOut(iSet.OutTime, function(){
				
			});
			iSet.Field.eq(n).fadeIn(iSet.InTime).siblings().fadeOut(iSet.OutTime);
			iSet.Nav.eq(n).addClass(iSet.CurCls).siblings().removeClass(iSet.CurCls);
		}
		//初始高亮第一个
		changeFun(iSet.K);
		//鼠标事件
		iSet.Nav.hover(function(){
			iSet.K=iSet.Nav.index(this);
			if(iSet.Auto){
				clearInterval(autoSlide);
			}
			hasCls = $(this).hasClass(iSet.CurCls);
			//避免无意识划过时触发
			acrossFun=setTimeout(function(){
				//避免当前高亮时划入再次触发
				if(!hasCls){
					changeFun(iSet.K);
				}
			},iSet.CrossTime);
		},function(){
			clearTimeout(acrossFun);
			//ajax调用
			if(iSet.Ajax){
				iSet.AjaxFun();
			}
			if(iSet.Auto){
				//自动切换
				autoSlide = setInterval(function(){
		            iSet.K++;
		            changeFun(iSet.K);
		            if (iSet.K == iSet.Field.size()) {
		                changeFun(0);
						iSet.K=0;
		            }
		        }, iSet.AutoTime)
			}
		}).eq(0).trigger('mouseleave');
	}
})(jQuery);



//关键函数：通过控制i ，来显示不通的幻灯片

function showImg(i){

	$("#b-img li")

		.eq(i).stop(true,true).fadeIn(800)

		.siblings("li").fadeOut(800);
	
	 	$("#b-btn span")

		.eq(i).addClass("hov")

		.siblings().removeClass("hov");

}



$(document).ready(function(){
	$("#b-img li").eq(0).show();
	$("#b-btn span").eq(0).addClass('hov'); 
	 var index = 0;
	 $("#b-btn span").mouseover(function(){
		index  =  $("#b-btn span").index(this);
		showImg(index);
	});	
	 var lenght=$("#b-img li").length;
	 var time=4500;

	  $(".sLeft").click(function(){
			if(index==0){return false}
			index=--index;
			showImg(index);
	 })
	 
	 $(".sRight").click(function(){
			if(index==lenght-1){return false}
			index=++index;
			showImg(index);
	 })

	 //滑入 停止动画，滑出开始动画.
	 $('#b-frame').hover(
	 	function(){
			  if(MyTime){
				 clearInterval(MyTime);
			  }
		 },function(){
			  	MyTime = setInterval(function(){
			    showImg(index)
				index++;
				if(index==lenght){index=0;}
			  } , time);
	 });

	 //自动开始

	 var MyTime = setInterval(function(){

		showImg(index)

		index++;

		if(index==lenght){index=0;}

	 } , time);

});