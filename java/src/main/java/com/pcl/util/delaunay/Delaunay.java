package com.pcl.util.delaunay;
import javafx.scene.shape.TriangleMesh;

import java.util.ArrayList;


/*
using Bowyer-Watson Algorithm to generate Delaunay Triangle Mesh
 */
public class Delaunay {
    public ArrayList<mTriangle> triangles=new ArrayList<>();
    dPoint superp1 = new dPoint(0,0);
    dPoint superp2 = new dPoint(50,0);
    dPoint superp3 = new dPoint(50,50);
    dPoint superp4 = new dPoint(0,50);
    public Delaunay(ArrayList<dPoint> pointlist){
        /*init super triangle, all points in [0.0,50.0]*/
        mTriangle t1 = new mTriangle(superp1,superp2,superp4);
        mTriangle t2 = new mTriangle(superp2,superp3,superp4);
        t1.neighbor2=1;
        t2.neighbor3=0;
        triangles.add(t1);
        triangles.add(t2);
        int c=0;
        addPoint(new dPoint(10,10));
        addPoint(new dPoint(15,5));
        addPoint(new dPoint(35,10));
        addPoint(new dPoint(15,40));
        addPoint(new dPoint(25,30));
//        for(dPoint x:pointlist)
//            addPoint(x);
//        deleteSuperTriangle();
    }

    protected boolean useSuperTrianglePoints(int tidx){
        if(tidx<0 || tidx>=triangles.size())
        {
            System.out.println("useSuperCC error: tidx out of range");
            return false;
        }
        mTriangle t = triangles.get(tidx);
        if(t.a==superp1 || t.a==superp2 || t.a==superp3 || t.a==superp4)
            return true;
        if(t.b==superp1 || t.b==superp2 || t.b==superp3 || t.b==superp4)
            return true;
        if(t.c==superp1 || t.c==superp2 || t.c==superp3 || t.c==superp4)
            return true;
        return false;
    }

    protected void deleteSuperTriangle() {
        for(int i=0,len=triangles.size();i<len;i++){
            if(useSuperTrianglePoints(i))
            {
                triangles.remove(i);
                --len;
                --i;
            }
        }
    }

    protected void updateNeighbor(int tidx, int oldn, int newn){
        /* no neighbor */
        if(tidx<0)
            return;
        mTriangle t = triangles.get(tidx);
        if(t.neighbor1 == oldn)
            t.neighbor1 = newn;
        else if(t.neighbor2 == oldn)
            t.neighbor2 = newn;
        else if(t.neighbor3 == oldn)
            t.neighbor3 = newn;
        else
            return;
    }

    protected boolean abEdgeNeighbor(int tidx1, int tidx2){
        mTriangle t1 = triangles.get(tidx1);
        mTriangle t2 = triangles.get(tidx2);
        if((t1.a==t2.a||t1.a==t2.b||t1.a==t2.c) && (t1.b==t2.a||t1.b==t2.b||t1.b==t2.c))
            return true;
        return false;
    }
    protected boolean bcEdgeNeighbor(int tidx1, int tidx2){
        mTriangle t1 = triangles.get(tidx1);
        mTriangle t2 = triangles.get(tidx2);
        if((t1.c==t2.a||t1.c==t2.b||t1.c==t2.c) && (t1.b==t2.a||t1.b==t2.b||t1.b==t2.c))
            return true;
        return false;
    }
    protected boolean acEdgeNeighbor(int tidx1, int tidx2){
        mTriangle t1 = triangles.get(tidx1);
        mTriangle t2 = triangles.get(tidx2);
        if((t1.a==t2.a||t1.a==t2.b||t1.a==t2.c) && (t1.c==t2.a||t1.c==t2.b||t1.c==t2.c))
            return true;
        return false;
    }
    protected boolean needUpdate(int tidx1, int tidx2){
        if(tidx1<0 || tidx2<0)
            return false;
        mTriangle t1 = triangles.get(tidx1);
        mTriangle t2 = triangles.get(tidx2);
        if(t2.inExternalCircle(t1.a) || t2.inExternalCircle(t1.b) || t2.inExternalCircle(t1.c))
            return true;
        if(t1.inExternalCircle(t2.a) || t1.inExternalCircle(t2.b) || t1.inExternalCircle(t2.c))
            return true;
        return false;
    }
    protected void updateQuadrilateral(int tidx1, int tidx2){
        if(tidx1<0 || tidx2<0)
            return;
        mTriangle t1,t2;
        t1=triangles.get(tidx1);
        t2=triangles.get(tidx2);
        mTriangle nt1,nt2;
        dPoint distinctP=null;
        if(t2.a!=t1.a && t2.a!=t1.b && t2.a!=t1.c)
            distinctP = t2.a;
        else if(t2.b!=t1.a && t2.b!=t1.b && t2.b!=t1.c)
            distinctP = t2.b;
        else if(t2.c!=t1.c && t2.c!=t1.b && t2.c!=t1.c)
            distinctP = t2.c;
        if(distinctP==null){
            System.out.println("updateQuadrilateral error: no distinct point");
            return;
        }
        ArrayList<Integer> nn = new ArrayList<>();
        if(t1.neighbor1!=tidx2 && t1.neighbor1>=0)
            nn.add(t1.neighbor1);
        if(t1.neighbor2!=tidx2 && t1.neighbor2>=0)
            nn.add(t1.neighbor2);
        if(t1.neighbor3!=tidx2 && t1.neighbor3>=0)
            nn.add(t1.neighbor3);
        if(t2.neighbor1!=tidx1 && t2.neighbor1>=0)
            nn.add(t2.neighbor1);
        if(t2.neighbor2!=tidx1 && t2.neighbor2>=0)
            nn.add(t2.neighbor2);
        if(t2.neighbor3!=tidx1 && t2.neighbor3>=0)
            nn.add(t2.neighbor3);
        if(abEdgeNeighbor(tidx1,tidx2)) {
//            System.out.println(tidx1+"abn");
            nt1 = new mTriangle(t1.a, t1.c, distinctP);
            nt2 = new mTriangle(distinctP, t1.b, t1.c);
        }
        else if(bcEdgeNeighbor(tidx1,tidx2)) {
//            System.out.println(tidx1+"bcn");
            nt1 = new mTriangle(t1.a, t1.b, distinctP);
            nt2 = new mTriangle(t1.a, distinctP, t1.c);
        }
        else {
//            System.out.println(tidx1+"acn");
            nt1 = new mTriangle(t1.a, t1.b, distinctP);
            nt2 = new mTriangle(distinctP, t1.b, t1.c);
        }
        triangles.set(tidx1,nt1);
        triangles.set(tidx2,nt2);
        for(int tdx:nn)
        {
            if(abEdgeNeighbor(tidx1,tdx))
            {
                nt1.neighbor1 = tdx;
                updateNeighbor(tdx,tidx1,tidx2);
                updateNeighbor(tdx,tidx2,tidx1);
            }
            if(bcEdgeNeighbor(tidx1,tdx))
            {
                nt1.neighbor2 = tdx;
                updateNeighbor(tdx,tidx1,tidx2);
                updateNeighbor(tdx,tidx2,tidx1);
            }
            if(acEdgeNeighbor(tidx1,tdx))
            {
                nt1.neighbor3 = tdx;
                updateNeighbor(tdx,tidx1,tidx2);
                updateNeighbor(tdx,tidx2,tidx1);
            }
            if(abEdgeNeighbor(tidx2,tdx))
            {
                nt2.neighbor1 = tdx;
                updateNeighbor(tdx,tidx1,tidx2);
                updateNeighbor(tdx,tidx2,tidx1);
            }
            if(bcEdgeNeighbor(tidx2,tdx))
            {
                nt2.neighbor2 = tdx;
                updateNeighbor(tdx,tidx1,tidx2);
                updateNeighbor(tdx,tidx2,tidx1);
            }
            if(acEdgeNeighbor(tidx2,tdx))
            {
                nt2.neighbor3 = tdx;
                updateNeighbor(tdx,tidx1,tidx2);
                updateNeighbor(tdx,tidx2,tidx1);
            }
        }
    }

    protected void addPoint(dPoint p) {
        int Tidx=0;
        /* find which triangle p is in */
        for(;Tidx<triangles.size();Tidx++)
            if(triangles.get(Tidx).inTriangle(p))
                break;
        if(Tidx>=triangles.size())
        {
            System.out.println("addPoint error: point out of super triangles");
            return;
        }
        mTriangle oldt = triangles.get(Tidx);
        mTriangle nt1,nt2,nt3;
        /* replace the old triangle by three new triangle*/
        nt1=new mTriangle(oldt.a,oldt.b,p);
        nt1.neighbor1 = oldt.neighbor1;
        nt1.neighbor2 = triangles.size();
        nt1.neighbor3 = triangles.size()+1;
        // this one has no need to update
        // updateNeighbor(oldt.neighbor1, Tidx, Tidx);

        nt2=new mTriangle(oldt.b,oldt.c,p);
        nt2.neighbor1 = oldt.neighbor2;
        nt2.neighbor2 = triangles.size()+1;
        nt2.neighbor3 = Tidx;
        updateNeighbor(oldt.neighbor2, Tidx, triangles.size());

        nt3=new mTriangle(oldt.c,oldt.a,p);
        nt3.neighbor1 = oldt.neighbor2;
        nt3.neighbor2 = Tidx;
        nt3.neighbor3 = triangles.size();
        updateNeighbor(oldt.neighbor3, Tidx, triangles.size()+1);

        triangles.set(Tidx,nt1);
        triangles.add(nt2);
        triangles.add(nt3);

        if(needUpdate(Tidx,nt1.neighbor1))
            updateQuadrilateral(Tidx,nt1.neighbor1);
        else if(needUpdate(triangles.size()-2,nt2.neighbor1))
            updateQuadrilateral(triangles.size()-2,nt2.neighbor1);
        else if(needUpdate(triangles.size()-1,nt3.neighbor1))
            updateQuadrilateral(triangles.size()-1,nt3.neighbor1);

    }
}


class mTriangle{
    dPoint a;
    dPoint b;
    dPoint c;
    dPoint center;
    double Rsq;
    /** share points a&b*/
    int neighbor1;
    /** share points b&c*/
    int neighbor2;
    /** share points c&a*/
    int neighbor3;
    public mTriangle(dPoint p1, dPoint p2, dPoint p3){
        this.a = p1;
        this.b = p2;
        this.c = p3;
        this.neighbor1 = -1;
        this.neighbor2 = -1;
        this.neighbor3 = -1;
        center = new dPoint();
        getCenter();
    }
    /* calculate External circle center*/
    protected void getCenter(){
        double A1,A2,B1,B2,C1,C2;
        /* l1(b,c):A1x+B1y+C1*/
        A1 = c.x-b.x;
        B1 = c.y-b.y;
        C1 = (-(b.y+c.y)*B1-A1*(b.x+c.x))/2;
        /* l2(a,c):A2x+B2y+C2*/
        A2 = c.x-a.x;
        B2 = c.y-a.y;
        C2 = (-(a.y+c.y)*B2-A2*(a.x+c.x))/2;
        if(A1!=0 && A2!=0 && B1!=0 && B2!=0)
        {
            if(A1*B2==B1*A2)
            {
                System.out.println("Triangle error: edge parallel");
                return;
            }
            center.y = (C1*A2-C2*A1)/(A1*B2-B1*A2);
            center.x = -(B1*center.y+C1)/A1;
            Rsq = dPoint.sdist(center,c);
            return;
        }
        else
        {
            if((A1==0 && B1==0) || (A2==0 && B2==0))
            {
                System.out.println("Triangle error: same two point");
                return;
            }
            if((A1==0 && A2==0) || (B1==0 && B2==0))
            {
                System.out.println("Triangle error: three points collinear");
                return;
            }
            if(A1==0)
            {
                center.y = -C1/B1;
                center.x = -(B2*center.y+C2)/A2;
                Rsq = dPoint.sdist(center,c);
                return;
            }
            if(A2==0)
            {
                center.y = -C2/B2;
                center.x = -(B1*center.y+C1)/A1;
                Rsq = dPoint.sdist(center,c);
                return;
            }
            if(B1==0)
            {
                center.x = -C1/A1;
                center.y = -(C2+A2*center.x)/B2;
                Rsq = dPoint.sdist(center,c);
                return;
            }
            center.x = -C2/A2;
            center.y = -(C1+A1*center.x)/B1;
            Rsq = dPoint.sdist(center,c);
            return;
        }
    }

    /* judge if p is in triangle */
    public boolean inTriangle(dPoint p)
    {
        /* cross prod*/
        dPoint MA = new dPoint(p.x - a.x,p.y - a.y);
        dPoint MB = new dPoint(p.x - b.x,p.y - b.y);
        dPoint MC = new dPoint(p.x - c.x,p.y - c.y);
        double cp1 = MA.x * MB.y - MA.y * MB.x;
        double cp2 = MB.x * MC.y - MB.y * MC.x;
        double cp3 = MC.x * MA.y - MC.y * MA.x;
        if(cp1>=0 && cp2>=0 && cp3>=0)
            return true;
        if(cp1<=0 && cp2<=0 && cp3<=0)
            return true;
        return false;
    }

    public boolean inExternalCircle(dPoint p)
    {
        if(dPoint.sdist(p,center)<=Rsq)
            return true;
        return false;
    }
}