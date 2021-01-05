package com.pcl.util.delaunay;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import static java.lang.Math.PI;
import static java.lang.Math.max;

/**
 * 输入都是以(25,25)为圆心,25为半径的圆内采样
 */
public class Algs {
    public static double DummyAlg(ArrayList<dPoint> points, int granularity)
    {
        int mapsize = granularity+1;
        double trate = granularity/50.0;
        int len = points.size();
        double bestRadius = 25*25*(PI*Math.sqrt(3)/6.0);
        bestRadius = Math.sqrt(bestRadius/len);
        bestRadius = bestRadius*trate;
        boolean[][] map = new boolean[mapsize][mapsize];
        int ValidCount = 0;
        for(int i=0;i<mapsize;i++)
            for(int j=0;j<mapsize;j++) {
                map[i][j] = false;
                if((i-(double)mapsize/2)*(i-(double)mapsize/2)+(j-(double)mapsize/2)*(j-(double)mapsize/2) <= (double)mapsize*mapsize/4)
                    ValidCount++;
            }
        for(dPoint p:points)
        {
            double tx = p.x*trate;
            double ty = p.y*trate;
            int sx = (int)(tx - bestRadius);
            int sy = (int)(ty - bestRadius);
            for(int r=0;r<bestRadius*2+1;r++)
            {
                for(int c=0;c<bestRadius*2+1;c++)
                {
                    int rx = sx+r;
                    int ry = sy+c;
                    if(rx>=0 && rx<mapsize && ry>=0 && ry<mapsize)
                        if((tx-rx)*(tx-rx)+(ty-ry)*(ty-ry)<=bestRadius*bestRadius)
                            map[ry][rx]=true;
                }
            }
        }
        int covered = 0;
        for(int i=0;i<mapsize;i++)
            for(int j=0;j<mapsize;j++)
                if(map[i][j]==true)
                    covered ++;
        return (double)covered/ValidCount;
    }
}

class KDNode{
    public KDNode(){
        dividePoint = null;
        leftChild = null;
        rightChild = null;
    }
    dPoint dividePoint;
    KDNode leftChild;
    KDNode rightChild;
    boolean xaxis;
}
class KDTree{

    protected KDNode ROOT;
    protected ArrayList<KDNode> nn;
    protected double[] nnsdist;
    protected int k;
    public KDTree(ArrayList<dPoint> pointlist){
        ROOT = new KDNode();
        BuildTree(pointlist, ROOT, 0, pointlist.size(), true, true);
    }
    public void DFS()
    {
        DFS(ROOT.leftChild);
    }
    public ArrayList<dPoint> KNN(int k, dPoint center){
        this.k = k;
        this.nn = new ArrayList<>();
        this.nnsdist = new double[k];
        KNN(center, ROOT.leftChild);
        ArrayList<dPoint> knn = new ArrayList<>();
        for(KDNode x:nn)
        {
            knn.add(x.dividePoint.Copy());
        }
        return knn;
    }
    protected void KNN(dPoint center, KDNode root)
    {
        if(root==null)
            return;
//        System.out.println(root.dividePoint.x+","+root.dividePoint.y+(root.xaxis?",x,":",y,")+nn.size());
        //if goto left part
        boolean leftpart = root.xaxis && center.x<root.dividePoint.x || !root.xaxis && center.y<root.dividePoint.y;
        if(leftpart)
            KNN(center, root.leftChild);
        else
            KNN(center, root.rightChild);
        double cdist = (center.x-root.dividePoint.x)*(center.x-root.dividePoint.x)+
                (center.y-root.dividePoint.y)*(center.y-root.dividePoint.y);
        if(nn.size()<k)
        {
            nnsdist[nn.size()] = cdist;
            nn.add(root);
        }
        else
        {
            int maxidx = 0;
            for(int i=1;i<k;i++)
                if(nnsdist[i]>nnsdist[maxidx])
                    maxidx = i;
            //if nearer, replace the maximun distance nn point with current point
            if(nnsdist[maxidx]>cdist)
            {
                nn.set(maxidx, root);
                nnsdist[maxidx]=cdist;
            }
            if(root.xaxis && nnsdist[maxidx]<Math.abs(center.x-root.dividePoint.x))
                return;
            if(!root.xaxis && nnsdist[maxidx]<Math.abs(center.y-root.dividePoint.y))
                return;
        }
        if(leftpart)
            KNN(center, root.rightChild);
        else
            KNN(center, root.leftChild);
    }
    protected void DFS(KDNode root)
    {
        System.out.println(root.dividePoint.x+","+root.dividePoint.y);
        if(root.leftChild!=null)
            DFS(root.leftChild);
        if(root.rightChild!=null)
            DFS(root.rightChild);
    }
    protected void BuildTree(ArrayList<dPoint> pointlist,KDNode root , int start, int end, boolean xaxis, boolean left){
        if(start>=end)
            return;
        int mid = start;
        if(start+1<end) {
            if (xaxis)
                pointlist.subList(start, end).sort((dPoint p1, dPoint p2) -> Double.compare(p1.x, p2.x));
            else
                pointlist.subList(start, end).sort((dPoint p1, dPoint p2) -> Double.compare(p1.y, p2.y));
            mid = (start + end) / 2;
        }
        KDNode node = new KDNode();
        node.dividePoint = new dPoint(pointlist.get(mid).x, pointlist.get(mid).y);
        node.xaxis = xaxis;
        if(left)
            root.leftChild = node;
        else
            root.rightChild = node;
        BuildTree(pointlist,node,start,mid,!xaxis,true);
        BuildTree(pointlist,node,mid+1,end,!xaxis,false);
    }
    /*
    public static void main(String[] args) {
        ArrayList<dPoint> testlist = new ArrayList<dPoint>();
        testlist.add(new dPoint(1,2));
        testlist.add(new dPoint(2,3));
        testlist.add(new dPoint(9,1));
        testlist.add(new dPoint(3,1));
        testlist.add(new dPoint(0,5));
        KDTree tree = new KDTree(testlist);
        ArrayList<dPoint> knn = tree.KNN(3,new dPoint(3,3));
        for(dPoint x:knn)
        {
            System.out.println(x.x+","+x.y);
        }
    }
    */
}