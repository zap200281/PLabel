package com.pcl.util.delaunay;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import static java.lang.Math.PI;

public class MainFrame extends JFrame {
    int r = 10;
    ArrayList<dPoint> pointlist=null;
    CPane cpane=null;
    JButton fileinLabel=null;
    JLabel radiusLabel=new JLabel("Radius: --");
    JLabel scoreLabel=new JLabel("RawScore: --");
    JLabel transscoreLabel=new JLabel("TScore: --");
    JLabel knnLabel=new JLabel("KNN: ");
    JSpinner kSpinner = new JSpinner();
    KDTree kdtree=null;
    Delaunay dtriangles;
    ArrayList<dPoint> knn=null;
    boolean drawCircle = false;
    boolean drawMesh = true;

    public MainFrame()
    {
        init();
    }
    protected void init()
    {
        cpane = new CPane(this,r);
        cpane.setBackground(Color.white);
        cpane.setBounds(0,0,505,505);
        cpane.setVisible(true);
        cpane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dPoint realloc = new dPoint();
                realloc.x = e.getX()-cpane.getX();
                realloc.x = realloc.x*50.0/cpane.getWidth();
                realloc.y = e.getY()-cpane.getY();
                realloc.y = realloc.y*50.0/cpane.getHeight();
                if(kdtree!=null)
                {
                    int k = (int)kSpinner.getValue();
                    if(k>0 && k<=pointlist.size())
                        knn = kdtree.KNN(k,realloc);
                }
                cpane.repaint();
                super.mouseClicked(e);
            }
        });
        add(cpane);
        fileinLabel = new JButton("Add File");
        fileinLabel.setBounds(510,25,150,200);
        fileinLabel.setBackground(Color.pink);
        fileinLabel.setTransferHandler(new TransferHandler(){
            @Override
            public boolean importData(JComponent comp, Transferable t)
            {
                try {
                    Object o = t.getTransferData(DataFlavor.javaFileListFlavor);

                    String filepath = o.toString();
                    if (filepath.startsWith("[")) {
                        filepath = filepath.substring(1);
                    }
                    if (filepath.endsWith("]")) {
                        filepath = filepath.substring(0, filepath.length() - 1);
                    }
                    int index=0;
                    for(int i=0;i<filepath.length();i++)
                        if(filepath.charAt(i)=='\\')
                            index=i;
                    String fPath=filepath.substring(0, index+1);
                    String fName=filepath.substring(index+1);
                    System.out.println(fPath+fName);
                    pointlist = new ArrayList<>();
                    File file = new File(fPath+fName);
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    String tempString = reader.readLine();
                    while((tempString = reader.readLine()) != null) {
                        String[] sp = tempString.split(" ");
                        pointlist.add(new dPoint(Double.parseDouble(sp[0]),Double.parseDouble(sp[1])));
                    }
                    double DummyScore = Algs.DummyAlg(pointlist,500);

                    double bestRadius = 25*25*(PI*Math.sqrt(3)/6.0);
                    bestRadius = Math.sqrt(bestRadius/pointlist.size())/50;
                    radiusLabel.setText("Radius: "+bestRadius);
                    scoreLabel.setText("RawScore: "+DummyScore);
                    transscoreLabel.setText("TScore: "+DummyScore/Math.sqrt(3)/PI*6);
                    kdtree = new KDTree(pointlist);
                    cpane.repaint();
                    dtriangles = new Delaunay(pointlist);
                    System.out.println(dtriangles.triangles.size());
                    return true;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
        }
            @Override
            public boolean canImport(JComponent comp, DataFlavor[] flavors) {
                for (int i = 0; i < flavors.length; i++) {
                    if (DataFlavor.javaFileListFlavor.equals(flavors[i])) {
                        return true;
                    }
                }
                return false;
            }
        });
        add(fileinLabel);
        radiusLabel.setBounds(510,250,190,30);
        scoreLabel.setBounds(510,280,190,30);
        transscoreLabel.setBounds(510,310,190,30);
        knnLabel.setBounds(510,450,50,20);
        kSpinner.setBounds(560,450,50,20);
        add(radiusLabel);
        add(scoreLabel);
        add(transscoreLabel);
        add(knnLabel);
        add(kSpinner);
    }

    public static void main(String[] args) {
        MainFrame frame = new MainFrame();
        frame.setLayout(null);
        frame.setBackground(Color.white);
        frame.setSize(510,550);
        frame.setVisible(true);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pointlist=new ArrayList<>();
        frame.dtriangles = new Delaunay(null);

    }
}

class CPane extends JPanel{
    MainFrame father = null;
    int r;
    double pr = 1.5;
    Shape edge;
    public CPane(MainFrame father , int r)
    {
        this.father = father;
        this.r = r;
    }
    protected dPoint transloc(dPoint raw){
        dPoint tp = new dPoint();
        tp.x = raw.x/50*(getWidth()-5);
        tp.y = raw.y/50*(getWidth()-5);
        return tp;
    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        edge = new Arc2D.Float(0,0,getWidth()-5,getHeight()-5,0,360,Arc2D.CHORD);
        g2.setColor(new Color(150,150,150,100));
        for(int i=0;i<getHeight();i+=r) {
            g2.drawLine(0, i, getWidth(), i);
            g2.drawLine(i, 0, i, getHeight());
        }
        g2.setColor(Color.black);
        g2.draw(edge);
        g2.setColor(Color.blue);
        if(father.pointlist!=null)
        {
            double trate = (getWidth()-5)/(double)50;
            double bestRadius = 25*25*(PI*Math.sqrt(3)/6.0);
            bestRadius = Math.sqrt(bestRadius/father.pointlist.size());
            bestRadius = bestRadius*trate;
            for(dPoint p:father.pointlist)
            {
                dPoint tp = transloc(p);
                if(father.drawCircle)
                    g2.draw(new Arc2D.Double(tp.x-bestRadius,tp.y-bestRadius,bestRadius*2,bestRadius*2,0,360,Arc2D.CHORD));
//                else
//                    g2.fill(new Arc2D.Double(tp.x-pr,tp.y-pr,pr*2,pr*2,0,360,Arc2D.CHORD));
            }
            if(father.knn!=null)
            {
                g2.setColor(Color.red);
                for(dPoint p:father.knn)
                {
                    dPoint tp = transloc(p);
                    if(father.drawCircle)
                        g2.draw(new Arc2D.Double(tp.x-bestRadius,tp.y-bestRadius,bestRadius*2,bestRadius*2,0,360,Arc2D.CHORD));
//                    else
//                        g2.fill(new Arc2D.Double(tp.x-pr,tp.y-pr,pr*2,pr*2,0,360,Arc2D.CHORD));
                }
            }
            if(father.dtriangles!=null)
            {
                for(mTriangle x:father.dtriangles.triangles)
                {
                    dPoint tp1,tp2,tp3;
                    tp1 = transloc(x.a);
                    tp2 = transloc(x.b);
                    tp3 = transloc(x.c);
                    g2.draw(new Line2D.Double(tp1.x,tp1.y,tp2.x,tp2.y));
                    g2.draw(new Line2D.Double(tp2.x,tp2.y,tp3.x,tp3.y));
                    g2.draw(new Line2D.Double(tp1.x,tp1.y,tp3.x,tp3.y));
                }
            }
        }
    }
}

class dPoint{
    public static double sdist(dPoint p1, dPoint p2){
        return (p1.x-p2.x)*(p1.x-p2.x)+(p1.y-p2.y)*(p1.y-p2.y);
    }
    public static boolean same(dPoint p1, dPoint p2){
        return (p1.x==p2.x)&&(p1.y==p2.y);
    }
    double x,y;
    public dPoint(){x=0;y=0;};
    public dPoint(double dx, double dy){x=dx;y=dy;}
    public dPoint(float dx, float dy){x=dx;y=dy;}
    public dPoint Copy(){
        return new dPoint(x,y);
    }
}