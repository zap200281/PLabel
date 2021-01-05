package com.pcl.util.mmdetetcion;

import java.util.ArrayList;
import java.util.List;

public class IoUUtil {

	public static double getIoU(List<Object> box, List<Object> boxs) {


		double _x1 = Double.parseDouble(boxs.get(0).toString());
		double _y1 = Double.parseDouble(boxs.get(1).toString());
		double _x2 = Double.parseDouble(boxs.get(2).toString());
		double _y2 = Double.parseDouble(boxs.get(3).toString());

		double x1 = Math.max(Double.parseDouble(box.get(0).toString()),_x1);
		double y1 = Math.max(Double.parseDouble(box.get(1).toString()),_y1);
		double x2 = Math.min(Double.parseDouble(box.get(2).toString()),_x2);
		double y2 = Math.min(Double.parseDouble(box.get(3).toString()),_y2);

		double w = Math.max(0, x2 - x1);
		double h = Math.max(0, y2 - y1);

		double uArea = w * h;

		double area = (Double.parseDouble(box.get(2).toString()) - Double.parseDouble(box.get(0).toString())) * (Double.parseDouble(box.get(3).toString()) - Double.parseDouble(box.get(1).toString())) + (_x2 - _x1)*(_y2 - _y1) - uArea;


		return uArea / area;
	}

	public static double computeIoU(List<Object> box, List<Object> boxs){

		double p1[] = new double[4];
		double p2[] = new double[4];

		p1[0] = Double.parseDouble(box.get(0).toString());
		p1[1] = Double.parseDouble(box.get(1).toString());
		p1[2] = Double.parseDouble(box.get(2).toString());
		p1[3] = Double.parseDouble(box.get(3).toString());

		p2[0] = Double.parseDouble(boxs.get(0).toString());
		p2[1] = Double.parseDouble(boxs.get(1).toString());
		p2[2] = Double.parseDouble(boxs.get(2).toString());
		p2[3] = Double.parseDouble(boxs.get(3).toString());


		//如果存在一个轴上，某个框的最小坐标大于另外一个框的最大坐标，则两框无重合。
		if(p2[3]-p1[1]<0 ||(p1[3]-p2[1]<0) || (p1[2]-p2[0]<0) || (p2[2]-p1[0]<0) )
			return 0;
		//两框必有重合，计算重合面积
		double h,w;
		if(p1[0]<p2[0]){
			if(p1[2]>p2[2]) h=p2[2]-p2[0];
			else h=p1[2]-p2[0];
		}
		else{
			if(p1[2]>p2[2]) h=p2[2]-p1[0];
			else h=p1[2]-p1[0];
		}
		if(p1[1]<p2[1]){
			if(p1[3]>p2[3]) w=p2[3]-p2[1];
			else w=p1[3]-p2[1];
		}
		else{
			if(p1[3]>p2[3]) w=p2[3]-p1[1];
			else w=p1[3]-p1[1];
		}
		// 容斥原理计算IoU
		return (h*w)/((p1[2]-p1[0])*(p1[3]-p1[1])+(p2[2]-p2[0])*(p2[3]-p2[1])-h*w);
	}

	public static void main(String[] args) {

		List<Object> box1 = new ArrayList<>();
		box1.add(0);
		box1.add(0);
		box1.add(5);
		box1.add(5);


		List<Object> box2 = new ArrayList<>();

		box2.add(0.1);
		box2.add(0.1);
		box2.add(4.9);
		box2.add(4.9);

		System.out.println(IoUUtil.getIoU(box1, box2));

		System.out.println(IoUUtil.computeIoU(box1, box2));

	}

}
