package com.pcl.util;

import javax.imageio.*;
import java.awt.image.*;
import java.awt.*;//Color
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PhotoDigest {

    public static int[] getData(String name) {
        try {
            BufferedImage img = ImageIO.read(new File(name));
            //BufferedImage slt = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            //slt.getGraphics().drawImage(img, 0, 0, 100, 100, null);
            // ImageIO.write(slt,"jpeg",new File("slt.jpg"));
            int[] data = new int[256];
            for (int x = 0; x < img.getWidth(); x++) {
                for (int y = 0; y < img.getHeight(); y++) {
                    int rgb = img.getRGB(x, y);
                    Color myColor = new Color(rgb);
                    int r = myColor.getRed();
                    int g = myColor.getGreen();
                    int b = myColor.getBlue();
                    data[(r + g + b) / 3]++;
                }
            }
            // data 就是所谓图形学当中的直方图的概念
            return data;
        } catch (Exception exception) {
            System.out.println("有文件没有找到,请检查文件是否存在或路径是否正确");
            return null;
        }
    }

    public static float compare(int[] s, int[] t) {
        try {
            float result = 0F;
            for (int i = 0; i < 256; i++) {
                int abs = Math.abs(s[i] - t[i]);
                int max = Math.max(s[i], t[i]);
                result += (1 - ((float) abs / (max == 0 ? 1 : max)));
            }
            return (result / 256) * 100;
        } catch (Exception exception) {
            return 0;
        }
    }
    
	public static void main(String[] args) throws Exception {

		//(数据类型)(最小值+Math.random()*(最大值-最小值+1))
		String path = "D:\\avi\\test";
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
		int count95 = 0;
		Collections.sort(list);
		long start = System.currentTimeMillis();

		int[] first = getData(new File(path,list.get(0)).getAbsolutePath());
		String firstName = list.get(0);
		//FingerPrint first = new FingerPrint(ImageIO.read(new File(path,list.get(0)))); 
		//first.setName(list.get(0));
		String secondName = null;
		for(int i = 1; i <list.size(); i++) {
			int[] second = getData(new File(path,list.get(i)).getAbsolutePath());
			secondName = list.get(i);
			//FingerPrint second = new FingerPrint(ImageIO.read(new File(path,list.get(i))));
			//second.setName(list.get(i));
			// 获取两个图的汉明距离
			float si = compare(first, second);
			//System.out.println(secondName + " and " +  firstName + " similar is:" + si + " %");
			// 通过汉明距离计算相似度，取值范围 [0.0, 1.0]
			if(si > 95) {
				count95++;
				if(si >97) {
					count97++;
				}
				if(si >98) {
					count98++;
				}
				if(si >96) {
					count96++;
				}
				System.out.println(secondName + " and " +  firstName + " similar is:" + si + " %");
			}else {
				first = second;
				firstName = secondName;
			}
		}
		System.out.println("cost:" + (System.currentTimeMillis() - start));
		System.out.println("重复96：" + count96 +" 重复97：" + count97 +" 重复98：" + count98 + " 重复95：" + count95 + "  总数：" + total);


	}
}
