package com.pcl.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @BelongsProject: maven-demo
 * @BelongsPackage: com.aliyun.picture.demo
 * @Author: Guoyh
 * @CreateTime: 2018-10-12 15:25
 * @Description: 对比图片相似度
 */
public class ImageContrastUtil {
	// 对比方法
	public static Double imageComparison(InputStream sampleInputStream,InputStream contrastInputStream ) throws IOException {
		//获取灰度像素的比较数组
		int[] photoArrayTwo = getPhotoArray(contrastInputStream);
		int[] photoArrayOne = getPhotoArray(sampleInputStream);

		// 获取两个图的汉明距离
		int hammingDistance = getHammingDistance(photoArrayOne, photoArrayTwo);
		// 通过汉明距离计算相似度，取值范围 [0.0, 1.0]
		double similarity = calSimilarity(hammingDistance);

		//返回相似精度
		return  similarity;
	}

	// 将任意Image类型图像转换为BufferedImage类型，方便后续操作
	public static BufferedImage convertToBufferedFrom(Image srcImage) {
		BufferedImage bufferedImage = new BufferedImage(srcImage.getWidth(null),
				srcImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bufferedImage.createGraphics();
		g.drawImage(srcImage, null, null);
		g.dispose();
		return bufferedImage;
	}

	// 转换至灰度图
	public static BufferedImage toGrayscale(Image image) {
		BufferedImage sourceBuffered = convertToBufferedFrom(image);
		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
		ColorConvertOp op = new ColorConvertOp(cs, null);
		BufferedImage grayBuffered = op.filter(sourceBuffered, null);
		return grayBuffered;
	}

	// 缩放至32x32像素缩略图
	public static Image scale(Image image) {
		image = image.getScaledInstance(32, 32, Image.SCALE_SMOOTH);
		return image;
	}

	// 获取像素数组
	public static int[] getPixels(Image image) {
		int width = image.getWidth(null);
		int height = image.getHeight(null);
		int[] pixels = convertToBufferedFrom(image).getRGB(0, 0, width, height,
				null, 0, width);
		return pixels;
	}

	// 获取灰度图的平均像素颜色值
	public static int getAverageOfPixelArray(int[] pixels) {
		Color color;
		long sumRed = 0;
		for (int i = 0; i < pixels.length; i++) {
			color = new Color(pixels[i], true);
			sumRed += color.getRed();
		}
		int averageRed = (int) (sumRed / pixels.length);
		return averageRed;
	}

	// 获取灰度图的像素比较数组（平均值的离差）
	public static int[] getPixelDeviateWeightsArray(int[] pixels, final int averageColor) {
		Color color;
		int[] dest = new int[pixels.length];
		for (int i = 0; i < pixels.length; i++) {
			color = new Color(pixels[i], true);
			dest[i] = color.getRed() - averageColor > 0 ? 1 : 0;
		}
		return dest;
	}

	// 获取两个缩略图的平均像素比较数组的汉明距离（距离越大差异越大）
	public static int getHammingDistance(int[] a, int[] b) {
		int sum = 0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i] == b[i] ? 0 : 1;
		}
		return sum;
	}

	//获取灰度像素的比较数组
	public static int[] getPhotoArray(InputStream inputStream) throws IOException {
		Image image = ImageIO.read(inputStream);
		//        Image image = ImageIO.read(imageFile);
		// 转换至灰度
		image = toGrayscale(image);
		// 缩小成32x32的缩略图
		image = scale(image);
		// 获取灰度像素数组
		int[] pixels = getPixels(image);
		// 获取平均灰度颜色
		int averageColor = getAverageOfPixelArray(pixels);
		// 获取灰度像素的比较数组（即图像指纹序列）
		pixels = getPixelDeviateWeightsArray(pixels, averageColor);

		return pixels;
	}

	// 通过汉明距离计算相似度
	public static double calSimilarity(int hammingDistance) {
		int length = 32 * 32;
		double similarity = (length - hammingDistance) / (double) length;

		// 使用指数曲线调整相似度结果
		similarity = java.lang.Math.pow(similarity, 2);
		return similarity;
	}


	/**
	 * @param args
	 * @return void
	 * @author Guoyh
	 * @date 2018/10/12 15:27
	 */
	public static void main(String[] args) throws Exception {

		//(数据类型)(最小值+Math.random()*(最大值-最小值+1))
		String path = "D:\\avi\\test1";
		File files[] = new File(path).listFiles();

		List<String> list = new ArrayList<>();
		for(File file : files) {
			if(file.getName().endsWith(".jpg")) {
				list.add(file.getName());
			}
		}
		int total = list.size();
		int count96 = 0;
		int count97 = 0;
		int count98 = 0;
		int count99 = 0;
		Collections.sort(list);
		long start = System.currentTimeMillis();

		int[] first = getPhotoArray(new FileInputStream(new File(path,list.get(0))));
		String firstName = list.get(0);
		//FingerPrint first = new FingerPrint(ImageIO.read(new File(path,list.get(0)))); 
		//first.setName(list.get(0));
		String secondName = null;
		for(int i = 1; i <list.size(); i++) {
			int[] second = getPhotoArray(new FileInputStream(new File(path,list.get(i))));
			secondName = list.get(i);
			//FingerPrint second = new FingerPrint(ImageIO.read(new File(path,list.get(i))));
			//second.setName(list.get(i));
			// 获取两个图的汉明距离
			int hammingDistance = getHammingDistance(first, second);
			// 通过汉明距离计算相似度，取值范围 [0.0, 1.0]
			double si = calSimilarity(hammingDistance) *100;
			if(si > 97) {
				// count98++;
				if(si >97) {
					count97++;
				}
				if(si >98) {
					count98++;
				}
				if(si >99) {
					count99++;
				}
				System.out.println(secondName + " and " +  firstName + " similar is:" + si + " %");
			}else {
				first = second;
				firstName = secondName;
			}
		}
		System.out.println("cost:" + (System.currentTimeMillis() - start));
		System.out.println("重复96：" + count96 +" 重复97：" + count97 +" 重复98：" + count98 + " 重复99：" + count99 + "  总数：" + total);


	}
}