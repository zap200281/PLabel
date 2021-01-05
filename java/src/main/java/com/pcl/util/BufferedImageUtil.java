package com.pcl.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class BufferedImageUtil {
	
	
	

	public BufferedImage getGrayPicture(BufferedImage originalImage)  {  
		int green=0,red=0,blue=0,rgb;  
		int imageWidth = originalImage.getWidth();  
		int imageHeight = originalImage.getHeight();  
		for(int i = originalImage.getMinX();i < imageWidth ;i++)  
		{  
			for(int j = originalImage.getMinY();j < imageHeight ;j++)  
			{  
				//图片的像素点其实是个矩阵，这里利用两个for循环来对每个像素进行操作  
				Object data = originalImage.getRaster().getDataElements(i, j, null);//获取该点像素，并以object类型表示  
				red = originalImage.getColorModel().getRed(data);  
				blue = originalImage.getColorModel().getBlue(data);  
				green = originalImage.getColorModel().getGreen(data);  
				red = (red*3 + green*6 + blue*1)/10;  
				green = red;  
				blue = green;  
				/* 
				这里将r、g、b再转化为rgb值，因为bufferedImage没有提供设置单个颜色的方法，只能设置rgb。rgb最大为8388608，当大于这个值时，应减去255*255*255即16777216 
				 */  
				rgb = (red*256 + green)*256+blue;  
				if(rgb>8388608)  
				{  
					rgb = rgb - 16777216;  
				}  
				//将rgb值写回图片  
				originalImage.setRGB(i, j, rgb);  
			}  
		}  
		return originalImage;     
	} 
	
	public static void main(String[] args) throws IOException {
		
		BufferedImage bi = ImageIO.read(new File("D:\\2019文档\\问题定位\\1001.jpg"));
		
		String imagePath =  "D:\\dcm\\dest.jpg";
		
		BufferedImageUtil util = new BufferedImageUtil();
		
		bi = util.getGrayPicture(bi);
		
		
		ImageIO.write(bi, "jpg", new File(imagePath));
		
	}
	

}
