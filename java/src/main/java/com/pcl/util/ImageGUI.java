package com.pcl.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JDialog;
 
public class ImageGUI extends JComponent {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private BufferedImage image;
 
  public ImageGUI() {
 
  }
 
  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2d = (Graphics2D)g;
    if(image == null) {
      g2d.setPaint(Color.BLACK);
      g2d.fillRect(0, 0, this.getWidth(), this.getHeight());
    } else {
      g2d.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), null);
      System.out.println("show frame...");
    }
  }
 
  public void createWin(String title) {
    JDialog ui = new JDialog();
    ui.setTitle(title);
    ui.getContentPane().setLayout(new BorderLayout());
    ui.getContentPane().add(this, BorderLayout.CENTER);
    ui.setSize(new Dimension(330, 240));
    ui.setVisible(true);
  }
 
  public void createWin(String title, Dimension size) {
    JDialog ui = new JDialog();
    ui.setTitle(title);
    ui.getContentPane().setLayout(new BorderLayout());
    ui.getContentPane().add(this, BorderLayout.CENTER);
    ui.setSize(size);
    ui.setVisible(true);
  }
 
  public void imshow(BufferedImage image) {
    this.image = image;
    this.repaint();
  }
 
}