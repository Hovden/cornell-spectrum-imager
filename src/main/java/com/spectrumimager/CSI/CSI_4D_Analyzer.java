package com.spectrumimager.CSI;

import ij.*;
import ij.plugin.filter.*;
import ij.plugin.ZProjector;
import ij.process.*;
import ij.gui.*;
import ij.gui.Roi.*;
import ij.measure.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.event.*;



/*
 * CSI_PAD_Analyzer is a plugin for ImageJ to view and manipulate PAD data.
 * Developed at Cornell University by Paul Cueva
 *   with support from the Muller Group
 *
 *   v1.5 CSI 211212
 */

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is CSI Diffraction Analyzer.
 *
 * The Initial Developer of the Original Code is
 * Paul Cueva <pdc23@cornell.edu>, Cornell University.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Paul Cueva <pdc23@cornell.edu>
 *   Robert Hovden <rmh244@cornell.edu>
 *   David A. Muller <david.a.muller@cornell.edu>
 *
 * ***** END LICENSE BLOCK ***** */
<<<<<<< HEAD:src/main/java/com/spectrumimager/CSI/CSI_4D_Analyzer.java
public class CSI_4D_Analyzer implements PlugInFilter, MouseListener, MouseMotionListener, Measurements, KeyListener, ImageListener, ItemListener, ChangeListener {
=======
public class CSI_PAD_Analyzer implements PlugInFilter, MouseListener, MouseMotionListener, Measurements, KeyListener, ImageListener, ItemListener, ChangeListener {
>>>>>>> 17b2fd426a6d49ac4f595b1afad9b914910322fd:src/plugins/CSI/CSI_PAD_Analyzer.java

    ImagePlus img; //Image data
    int width, height, zsize, tsize;
    double scale;
    ImagePlus realimage, kimage;
    Roi kroi, rroi;
    ImageProcessor fpk, fpr;
    ImageCanvas canvasr, canvask;
    int detectorMethod;
    JComboBox<String> comMethod;
    JSlider sldLogScale;

    /*
     * Load image data and start Cornell Diffraction Imager
     */
    public int setup(String arg, ImagePlus img) {         
        this.img = img;
        zsize = img.getNSlices();
        tsize = img.getNFrames();
        width = img.getWidth();
        height = img.getHeight();
        kimage = new ImagePlus("Diffraction Space - "+img.getTitle(), img.getProcessor());
        kimage.setRoi(width/2,height/2,1,1);
        realimage = NewImage.createFloatImage("Real Space - "+img.getTitle(), zsize, tsize, 2, NewImage.FILL_BLACK);
        updateRealImage();

        comMethod = new JComboBox<String>(new String[]{"Integrating Detector","Differential Phase Contrast","Center of Mass","Second Moment"});
        comMethod.addItemListener(this);
        comMethod.setVisible(true);
        Panel drop = new Panel();
        drop.add(comMethod);

        sldLogScale = new JSlider(-10,100,-10);
        sldLogScale.addChangeListener(this);
        sldLogScale.setVisible(true);
        Panel slid = new Panel();
        slid.add(sldLogScale);
        scale=0;

        kimage.show();
        canvask = kimage.getCanvas();
        kimage.getWindow().add(slid);
        IJ.run("Out [-]");
        IJ.run("In [+]");
<<<<<<< HEAD:src/main/java/com/spectrumimager/CSI/CSI_4D_Analyzer.java

=======
>>>>>>> 17b2fd426a6d49ac4f595b1afad9b914910322fd:src/plugins/CSI/CSI_PAD_Analyzer.java
        realimage.show();
        canvasr = realimage.getCanvas();
        realimage.getWindow().add(drop);
        IJ.run("Out [-]");
        IJ.run("In [+]");

        addListeners();
        img.hide();
        return DOES_ALL + NO_CHANGES;
    }

    /*
     * Initialize variables and create windows.
     */
    public void run(ImageProcessor ip) {  //this function runs upon opening
        if (img.getNDimensions() < 4) {
        	IJ.error("Diffraction analyzer needs a hyperstack.");
        }
    }

    void update(ComponentEvent e) {
        removeListeners();
        try{
        if (e.getComponent() == canvask)
                updateRealImage();
        else if (e.getComponent() == canvasr)
            updateKImage();
        } catch (Exception ex) {}
        addListeners();
    }

    void updateRealImage() {
        if (kimage.getRoi() != null) kroi = kimage.getRoi();
        if (kroi==null) return;
        int numpoints=kroi.getContainedPoints().length;
        if (numpoints == 0) return;
        int roiWidth=kroi.getBounds().width;
        int roiHeight=kroi.getBounds().height;

        double centerX=0, centerY=0;
        for (Point p : kroi) {
            centerX+= (double)p.x/numpoints;
            centerY+= (double)p.y/numpoints;
        }
        Point2D.Double center= new Point2D.Double(centerX,centerY);

        ImageStack stack = img.getStack();
        float[][] values = new float[2][tsize*zsize];
        Calibration cal = img.getCalibration();
        ImageProcessor ip;
        ImageStatistics stats;
        double[] multiplier = new double[]{0,0};
        for (int i = 1; i <= tsize*zsize; i++) {
            ip = stack.getProcessor(i);
            ip.setRoi(kroi);
            for (Point p : kroi){
                multiplier = getMultiplier(p,center,roiWidth,roiHeight,numpoints);
                values[0][i - 1]+= (float)ip.getf(p.x,p.y)*multiplier[0];
                values[1][i - 1]+= (float)ip.getf(p.x,p.y)*multiplier[1];
            }
        }

        ImageStack rstack = realimage.getStack();
        String[] sliceNames = getSliceNames();
        rstack.setProcessor(new FloatProcessor(zsize,tsize,values[0]),1);
        rstack.setSliceLabel(sliceNames[0],1);
        rstack.setProcessor(new FloatProcessor(zsize,tsize,values[1]),2);
        rstack.setSliceLabel(sliceNames[1],2);
        realimage.updateAndDraw();
        realimage.resetDisplayRange();

    }

    double[] getMultiplier(Point p, Point2D.Double center, int roiWidth, int roiHeight, int numpoints) {
        switch (detectorMethod) {
            case 0: return new double[]{1,1.0/numpoints};
            case 1: return new double[]{(p.x>center.x) ? 1.0/roiWidth : -1.0/roiWidth, (p.y>center.y) ? 1.0/roiHeight : -1.0/roiHeight};
            case 2: return new double[]{(p.x-center.x)/roiWidth, (p.y-center.y)/roiHeight };
            case 3: return new double[]{(p.x-center.x)*(p.x-center.x)+(p.y-center.y)*(p.y-center.y),1.0/numpoints};
            default: return new double[]{1,1/numpoints};
        }
    }

    String[] getSliceNames() {
        switch (detectorMethod) {
            case 0: return new String[]{"Sum","Mean"};
            case 1: return new String[]{"DPC X", "DPC Y"};
            case 2: return new String[]{"COM X","COM Y"};
            case 3: return new String[]{"Second Moment","Mean"};
            default: return new String[]{"Sum","Mean"};
        }
    }

    void updateKImage() {
        if (realimage.getRoi() != null) rroi = realimage.getRoi();
        if (rroi == null) return;
        int numpoints=rroi.getContainedPoints().length;
        if (numpoints == 0) return;

        ImageStack stack = img.getStack();
        float[][] values = new float[height][width];
        float[][] f;

        if (numpoints==1)
            fpk = stack.getProcessor(rroi.getContainedPoints()[0].x + zsize*rroi.getContainedPoints()[0].y).duplicate();
        else {
                for (Point p : rroi) {
                    f=stack.getProcessor(p.x + zsize*p.y).getFloatArray();
                    for (int i = 0; i<f.length; i++)
                        for (int j = 0; j<f[i].length; j++)
                            values[i][j]+=scale*f[i][j]/numpoints;
                }
            fpk = new FloatProcessor(values);
        }
        if (scale!=0)
            fpk.applyMacro("v=log(1+"+Double.toString(scale)+"*v/"+Double.toString(fpk.getMax())+")");
        fpk.resetMinAndMax();
        kimage.setProcessor(fpk);
    }

    /*
     * Gets the Z values through a single point at (x,y).
     */
    public void mousePressed(MouseEvent e) {}

    public void mouseClicked(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {
        update(e);
    }

    public void mouseDragged(MouseEvent e) {
        update(e);
    }

    public void mouseMoved(MouseEvent e) {}

    public void keyReleased(KeyEvent e) {}

    public void keyPressed(KeyEvent e) {
        update(e);
    }

    public void keyTyped(KeyEvent e) {}

    public void imageClosed(ImagePlus imp) {
        if (imp==kimage || imp==realimage) {
            removeListeners();

            realimage.close();
            kimage.close();

            img.show();
        }
    }

    public void imageUpdated(ImagePlus imp) {}

    public void imageOpened(ImagePlus imp) {}

    public void itemStateChanged(ItemEvent e) {
        if (e.getSource()==comMethod) {
            detectorMethod=comMethod.getSelectedIndex();
            try {updateRealImage();}
            catch (Exception ex) {}
        }
    }

    public void stateChanged(ChangeEvent e) {
        if (e.getSource()==sldLogScale) {
            if (sldLogScale.getValue()==-10)
                scale = 0;
            else
                scale=Math.exp(sldLogScale.getValue()/10);
            try {updateKImage();}
            catch (Exception ex) {}
<<<<<<< HEAD:src/main/java/com/spectrumimager/CSI/CSI_4D_Analyzer.java
=======

>>>>>>> 17b2fd426a6d49ac4f595b1afad9b914910322fd:src/plugins/CSI/CSI_PAD_Analyzer.java
        }
    }

    void removeListeners() {
            kimage.removeImageListener(this);
            canvask.removeMouseListener(this);
            canvask.removeMouseMotionListener(this);
            canvask.removeKeyListener(this);

            realimage.removeImageListener(this);
            canvasr.removeMouseListener(this);
            canvasr.removeMouseMotionListener(this);
            canvasr.removeKeyListener(this);        
    }

    void addListeners() {
        kimage.addImageListener(this);
        canvask.addMouseListener(this);
        canvask.addMouseMotionListener(this);
        canvask.addKeyListener(this);

        realimage.addImageListener(this);
        canvasr.addMouseListener(this);
        canvasr.addMouseMotionListener(this);
        canvasr.addKeyListener(this);
    }

}