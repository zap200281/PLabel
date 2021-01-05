var token = getCookie("token");
var ip = getIp();

var viewer = OpenSeadragon({
               id: "openseadragon1",
               prefixUrl: "js/openseadragon-bin-2.4.2/images/",
              /*			  
			  tileSources:{
                  tileSource: ip + "/api/getTitleSource",//后台接口地址
                  loadTilesWithAjax:true,//使用ajax请求
                  success:function(res){
                    console.log(res)//成功回调
                  },
                  error:function(err){
                    console.log(err)//失败回调
                  }
                }
				*/
            });


var tileSource = new OpenSeadragon.TileSource;
getimage('81a30f18f5f548b5831d6d6d2b6a03c9');

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
	  
	  var data = json.Image;
      
	  tileSource = {//装载tileSource 
            Image: {
                xmlns: data.xmlns,
                Url: ip + data.Url,
                Overlap: data.Overlap,
                TileSize: data.TileSize,
                Format: data.Format,
                Size: {
                    Height: data.Size.Height,
                    Width: data.Size.Width
                }
            }
        };
        viewer.open(tileSource);
    }
 });
}
