import ij.plugin.filter.PlugInFilter;
import ij.*;
import ij.plugin.ZProjector;
import ij.process.*;
import ij.gui.*;
import ij.util.Tools;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Random;
import ij.measure.*;
import java.awt.Rectangle;
import javax.swing.*;
import javax.swing.event.*;
import org.ujmp.core.*;	 //NOTE: This plugin requires JAMA, an external Java Matrix Package
import org.ujmp.core.calculation.*;	 //NOTE: This plugin requires UJMP, another external Java Matrix Package
import org.ujmp.jama.*;
import java.awt.Desktop;
import java.io.File;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.io.FileReader;
import java.io.FileWriter;



/*
 * CSI_Spectrum_Analyzer is a plugin for ImageJ to view and manipulate spectrum data.
 * Developed at Cornell University by Paul Cueva
 *   with support from Robert Hovden and the Muller Group
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
 * The Original Code is CSI Spectrum Analyzer.
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
public class CSI_Spectrum_Analyzer implements PlugInFilter {

    ImagePlus img; //Image data
    SpectrumData state;   //Image data class

    //CSI_Spectrum_Analyzer state variables
    boolean twoptcalib, isCalibrating, meanCentering = false, weightedPCA = false;

    //GUI Elements
    JButton butIntegrate, butHCMIntegrate, butPCA, butSubtract, butCancelCalibration, butCalibrate;
    ButtonGroup bgFit;
    JRadioButton radFast, radOversampled;
    JComboBox comFit;
    JSlider sldZoom, sldOffset, sldLeft, sldWidth, sldILeft, sldIWidth, sldCLeft, sldCRight;
    Panel panButtons, panCalibrateButtons, panSliders, panCalibrateL, panCalibrateR, panOver;
    Label labIntegrate, labSubtract, labCalibrate, labEnergy1, labEnergy2, labEnergy3, labEnergy4, labHover1, labHover2;
    TextField txtLeftCalibration, txtRightCalibration, txtEnergyCalibration, txtLeft, txtWidth, txtILeft, txtIWidth, txtOversampling;
    JMenuItem miTwoPointCalibration, miOnePointCalibration, miAbout, miDoc, miChangeColorCSI, miChangeColorCornell, miChangeColorCollegiate, miChangeColorCorporate;
    JPopupMenu pm;
    JCheckBoxMenuItem miScaleCounts, miMeanCentering, miWeightedPCA;
    JPanel panRad = new JPanel(), panAll= new JPanel();
    Color colZeroLine, colIntWindow,colSubtracted,colData,colDataFill,colBackFill, colBackgroundFit, colBackgroundWindow;

    /*
     * Load image data and start Cornell Spectrum Imager
     */
    public int setup(String arg, ImagePlus img) {
        Random rand = new Random();
        if (rand.nextDouble()<.04){
            IJ.showMessage("Reminder!","If you use CSI to produce published research, cite doi:10.1017/S1431927612000244.");
        }
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel()); //UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        
        try {
            FileReader fr = new FileReader(IJ.getDirectory("plugins")+"CSIconfig.txt");
            int c = fr.read();
            switch (c) {
                
                case 50:
                    colZeroLine = Color.red;
                    colIntWindow = Color.darkGray;
                    colSubtracted = Color.lightGray;
                    colData = Color.black;
                    colDataFill = new Color(179,27,27);
                    colBackFill = Color.white;
                    colBackgroundFit = Color.lightGray;
                    colBackgroundWindow = Color.gray;
                    break;
                case 51:
                    colZeroLine = new Color(128,0,0);
                    colIntWindow = new Color(128,128,158);
                    colSubtracted = new Color(192,192,222);
                    colData = Color.black;
                    colDataFill = new Color(15, 77, 146);
                    colBackFill = new Color(255,255,244);
                    colBackgroundFit = new Color(255,215,0);
                    colBackgroundWindow = new Color(245,205,0);
                    break;
                case 52:
                    colZeroLine = new Color(128,0,0);
                    colIntWindow = new Color(128,128,158);
                    colSubtracted = new Color(192,192,222);
                    colData = new Color(90, 190, 190);
                    colDataFill = new Color(90, 190, 190);
                    colBackFill = new Color(234,234,224);
                    colBackgroundFit = new Color(20,150,210);
                    colBackgroundWindow = new Color(20,150,210);
                    break;
                default:
                    colZeroLine = Color.red;
                    colIntWindow = Color.white;
                    colSubtracted = Color.lightGray;
                    colData = Color.black;
                    colDataFill = Color.darkGray;
                    colBackFill = new Color(160,165,160);
                    colBackgroundFit = new Color(0,128,0);
                    colBackgroundWindow = new Color(128,255,128);
                    break;
            }
            fr.close();
            
        } catch (Exception e){
            colZeroLine = Color.red;
            colIntWindow = Color.white;
            colSubtracted = Color.lightGray;
            colData = Color.black;
            colDataFill = Color.darkGray;
            colBackFill = new Color(160,165,160);
            colBackgroundFit = new Color(0,128,0);
            colBackgroundWindow = new Color(128,255,128);
        }

        if (System.getProperty("os.name").toLowerCase().indexOf( "mac" ) >= 0) {

        }

        if (IJ.versionLessThan("1.46")) {  //Check ImageJ version
			IJ.error("Error starting CSI: Cornell Spectrum Imager", "ImageJ version is too old.");
            return DONE; //Close plugin
        }
        this.img = img;
        if (img.getRoi() == null) { //Check that image is present and a region is selected
            if (img.getStackSize() == 1)
                img.setRoi(new Rectangle(0, img.getHeight()/2, img.getWidth(), 1));
            else
                img.setRoi(new Rectangle(0, 0, 10, 10));
            return DOES_ALL + NO_CHANGES;
        } else {
            return DOES_ALL + NO_CHANGES;
        }
    }

    /*
     * Initialize variables and create windows.
     */
    public void run(ImageProcessor ip) {  //this function runs upon opening
        Integer id = new Integer(img.getID());
        if (img.getStackSize() < 2) {
            if (img.getHeight() < 2) {
                state = new SpectrumData0D(); //Spectrum data is single spectrum
            } else
                state = new SpectrumData1D(); //Spectrum data is a linescan
        } else
            state = new SpectrumData2D();     //Spectrum data is a 2D map
        state.setup(img);  //TODO: move setup() into SpectrumData constructor
        state.pwin.getCanvas().disablePopupMenu(true);
        addInitialButtons(state.pwin);
        setupMenu(); //Creates the popup menu to be shown on right-clicks
        state.pwin.getCanvas().addMouseListener(new TestListener());
        state.pwin.addMouseListener(new TestListener());
        state.updateProfile();
        state.pwin.pack();
    }

    /*
     * Event handler for all of the elements in the gui.
     */
    private class TestListener implements ActionListener, ItemListener, MouseListener {
        

        public void actionPerformed(ActionEvent e) {
            Object b = e.getSource();
            if (b == miDoc) { //Display 'About' information
                try { 
			File pdfFile = new File(IJ.getDirectory("plugins")+"CSI Documentation.pdf");
			if (pdfFile.exists()) {
	 
				if (Desktop.isDesktopSupported()) {
					Desktop.getDesktop().open(pdfFile);
				} else {
					IJ.showMessage("AWT Desktop is not supported!");
				}
	 
			} else {
				IJ.showMessage("The documentation file does not exist!");
			}
	 
			//IJ.showMessage("Done");
	 
		  } catch (Exception ex) {
			ex.printStackTrace();
		}
            } else if (b == miAbout) { //Display 'About' information
                try {
                    IJ.openImage(IJ.getDirectory("plugins")+"CSI.png").show();
                } catch (Exception ex) {
                    IJ.showMessage("About CSI: Cornell Spectrum Imager",
                              "Spectrum analyzer ImageJ plugin developed at Cornell University \n \n"
                            + "                                  by Paul Cueva, Robert Hovden, and David A. Muller\n "
                            + "         School of Applied and Engineering Physics, Cornell University, Ithaca, NY 14853 \n "
                            + "                                 Kavli Institute at Cornell for Nanoscale Science\n \n"
                            + "                            with support from DOE BES, NSF MRSEC, and NYSTAR\n \n"
                            + "                                              version 1.6  14 02 2015");
                }
            }else if (b == miTwoPointCalibration) {
                if (!isCalibrating) {      //If not currently calibrating
                    isCalibrating = true;  // set twoppointcalibration mode to true
                    twoptcalib = true;     // and add calibration GUI elements
                    addCalibrateSliders(panSliders, panButtons);
                    state.pwin.pack();
                } else {
                    if (!twoptcalib) {					//If currently calibrating, but not
                        removeCalibrateSliders(panSliders, panButtons); // in two point mode, then remove
                        twoptcalib = true;				// current calibration GUI elements
                        addCalibrateSliders(panSliders, panButtons);    // and replace with two point GUI elements
                        state.pwin.pack();
                    }
                }
                state.updateProfile();
            } else if (b == miOnePointCalibration) {
                if (!isCalibrating) {     //If not currently calibrating
                    isCalibrating = true; // set onepointcalibration mode to true
                    twoptcalib = false;   // and add calibration GUI elements
                    addCalibrateSliders(panSliders, panButtons);
                    state.pwin.pack();
                } else {
                    if (twoptcalib) {									//If currently calibrating, but not
                        removeCalibrateSliders(panSliders, panButtons); // in one point mode, then remove
                        twoptcalib = false;								// current calibration GUI elements
                        addCalibrateSliders(panSliders, panButtons);	// and replace with one point GUI elements
                        state.pwin.pack();
                    }
                }
                state.updateProfile();
            } else if (b == miChangeColorCSI) {
                try {
                FileWriter fw = new FileWriter(IJ.getDirectory("plugins")+"CSIconfig.txt");
                fw.write((char)49);
                fw.close();
                } catch (Exception ex) {}
                colZeroLine = Color.red;
                colIntWindow = Color.white;
                colSubtracted = Color.lightGray;
                colData = Color.black;
                colDataFill = Color.darkGray;
                colBackFill = new Color(160,165,160);
                colBackgroundFit = new Color(0,128,0);
                colBackgroundWindow = new Color(128,255,128);
                state.updateProfile();
            }else if (b == miChangeColorCornell) {
                try {
                FileWriter fw = new FileWriter(IJ.getDirectory("plugins")+"CSIconfig.txt");
                fw.write((char)50);
                fw.close();
                } catch (Exception ex) {}
                colZeroLine = Color.red;
                colIntWindow = Color.darkGray;
                colSubtracted = Color.lightGray;
                colData = Color.black;
                colDataFill = new Color(179,27,27);
                colBackFill = Color.white;
                colBackgroundFit = Color.lightGray;
                colBackgroundWindow = Color.gray;
                state.updateProfile();
            }else if (b == miChangeColorCollegiate) {
                try {
                FileWriter fw = new FileWriter(IJ.getDirectory("plugins")+"CSIconfig.txt");
                fw.write((char)51);
                fw.close();
                } catch (Exception ex) {}
                colZeroLine = new Color(128,0,0);
                colIntWindow = new Color(128,128,158);
                colSubtracted = new Color(192,192,222);
                colData = Color.black;
                colDataFill = new Color(15, 77, 146);
                colBackFill = new Color(255,255,244);
                colBackgroundFit = new Color(255,215,0);
                colBackgroundWindow = new Color(245,205,0);
                state.updateProfile();
            }else if (b == miChangeColorCorporate) {
                try {
                FileWriter fw = new FileWriter(IJ.getDirectory("plugins")+"CSIconfig.txt");
                fw.write((char)52);
                fw.close();
                } catch (Exception ex) {}
                colZeroLine = new Color(128,0,0);
                colIntWindow = new Color(128,128,158);
                colSubtracted = new Color(192,192,222);
                colData = new Color(90, 190, 190);
                colDataFill = new Color(90, 190, 190);
                colBackFill = new Color(234,234,224);
                colBackgroundFit = new Color(20,150,210);
                colBackgroundWindow = new Color(20,150,210);
                state.updateProfile();
            }else if (b == butIntegrate) { //If integrate button was clicked
                if (comFit.getSelectedItem().equals("No Fit")){
                    state.integrate(state.X0, state.X1, state.iX0, state.iX1).show();
                }
                else
                    state.fitToModel(state.X0, state.X1, state.iX0, state.iX1).show(); //integrate data
                System.gc();
            } else if (b == butPCA) {
                if (weightedPCA)
                    state.weightedPCA(state.X0, state.X1, state.iX0, state.iX1);
                else
                    state.PCA(state.X0, state.X1, state.iX0, state.iX1);
                System.gc();
            } else if (b == butSubtract) { //If subtract button was clicked
                ImagePlus s =  state.subtract(state.X0, state.X1);
                s.show(); //subtract data
                System.gc();
            } else if (b == butCalibrate) {//If calibrate button was clicked
                state.recalibrate(); //calibrate data
            } else if (b == butCancelCalibration) {//If cancel calibration button was clicked
                isCalibrating = false; //Turn calibrating state to off
                removeCalibrateSliders(panSliders, panButtons); //remove calibration GUI elements
                state.pwin.pack();
                state.updateProfile();
            } else if (b == txtLeft) {
                Calibration cal = img.getCalibration();
                try {
                    sldLeft.setValue((int)(Double.parseDouble(txtLeft.getText())/cal.pixelDepth + cal.zOrigin));
                } catch (NumberFormatException nfe) {
                    txtLeft.setText(String.format("%.1f", state.x[state.X0]));
                }
            } else if (b == txtWidth) {
                Calibration cal = img.getCalibration();
                try {
                    sldWidth.setValue((int)(Double.parseDouble(txtWidth.getText())/cal.pixelDepth));
                } catch (NumberFormatException nfe) {
                    txtWidth.setText(String.format("%.1f", state.x[state.X1]-state.x[state.X0]));
                }
            } else if (b == txtILeft) {
                Calibration cal = img.getCalibration();
                try {
                    sldILeft.setValue((int)(Double.parseDouble(txtILeft.getText())/cal.pixelDepth + cal.zOrigin));
                } catch (NumberFormatException nfe) {
                    txtILeft.setText(String.format("%.1f", state.x[state.iX0]));
                }
            } else if (b == txtIWidth) {
                Calibration cal = img.getCalibration();
                try {
                    sldIWidth.setValue((int)(Double.parseDouble(txtIWidth.getText())/cal.pixelDepth));
                } catch (NumberFormatException nfe) {
                    txtIWidth.setText(String.format("%.1f", state.x[state.iX1]-state.x[state.iX0]));
                }
            } else if (b == radFast) {
                txtOversampling.setText("0.0");
                panOver.setVisible(false);
            } else if (b == radOversampled) {
                txtOversampling.setText("1.0");
                panOver.setVisible(true);
                state.pwin.pack();
            }
        }

        public void itemStateChanged(ItemEvent e) {
            Object b = e.getSource();
            if (b==miScaleCounts) {
                state.scaleCounts = miScaleCounts.getState();
                state.updateProfile();
            } else if (b == miMeanCentering) {
                meanCentering = miMeanCentering.getState();
            } else if (b == miWeightedPCA) {
                weightedPCA = miWeightedPCA.getState();
            } else if (b == comFit) { //If combo box (drop-down menu) is clicked
                String fitType = comFit.getSelectedItem().toString();
                if(fitType.equals("No Fit")) {				//Set fit state
                    state.setFit(SpectrumData.NO_FIT);				// to combo box selection
                } else if(fitType.equals("Constant")) {
                    state.setFit(SpectrumData.CONSTANT_FIT);
                } else if(fitType.equals("Linear")) {
                    state.setFit(SpectrumData.LINEAR_FIT);
                } else if(fitType.equals("Exponential")) {
                    state.setFit(SpectrumData.EXPONENTIAL_FIT);
                } else if(fitType.equals("Power")) {
                    state.setFit(SpectrumData.POWER_FIT);
                } else if(fitType.equals("LCPL")) {
                    state.setFit(SpectrumData.LCPL_FIT);
                }
                state.updateProfile();
            }
        }

        public void mouseClicked(MouseEvent e) {}

        public void mousePressed(MouseEvent e) {
            showPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            showPopup(e);
        }

        //Right click popup menu
        private void showPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                pm.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        public void mouseEntered(MouseEvent e) {
            Object b = e.getSource();
            if (b == butIntegrate) { //If integrate button was clicked
                labHover1.setForeground(Color.black);
                labHover1.setText("Sum the background subtracted data over the integration window.");
                labHover2.setForeground(Color.black);
                labHover2.setText("(Select \"No Fit\" to integrate raw spectra.)");
            } else if (b == butPCA) {
                labHover1.setForeground(Color.black);
                labHover1.setText("Perform Principal Component Analysis on backgound subtracted dataset");
                labHover2.setForeground(Color.black);
                labHover2.setText("over integration window (large window will take a long time).");
            } else if (b == butSubtract) { //If subtract button was clicked
                labHover1.setForeground(Color.black);
                labHover1.setText("Subtract extrapolated background from entire dataset.");
                labHover2.setForeground(Color.black);
                labHover2.setText("");
            } else if (b == butCalibrate) {//If calibrate button was clicked
                labHover1.setForeground(Color.black);
                labHover1.setText("Perform calibration.");
                labHover2.setForeground(Color.black);
                labHover2.setText("");
            } else if (b == butCancelCalibration) {//If cancel calibration button was clicked
                labHover1.setForeground(Color.black);
                labHover1.setText("Do not perform calibration.");
                labHover2.setForeground(Color.black);
                labHover2.setText("");
            } else if (b == txtLeft) {
                labHover1.setForeground(Color.black);
                labHover1.setText("Enter background window start energy directly.");
                labHover2.setForeground(Color.black);
                labHover2.setText("");
            } else if (b == txtWidth) {
                labHover1.setForeground(Color.black);
                labHover1.setText("Enter background window energy width directly.");
                labHover2.setForeground(Color.black);
                labHover2.setText("");
            } else if (b == txtILeft) {
                labHover1.setForeground(Color.black);
                labHover1.setText("Enter integration window start energy directly.");
                labHover2.setForeground(Color.black);
                labHover2.setText("");
            } else if (b == txtIWidth) {
                labHover1.setForeground(Color.black);
                labHover1.setText("Enter integration window energy width directly.");
                labHover2.setForeground(Color.black);
                labHover2.setText("");
            } else if (b == radFast) {
                labHover1.setForeground(Color.black);
                labHover1.setText("Perform standard background fits.");
                labHover2.setForeground(Color.black);
                labHover2.setText("");
            } else if (b == radOversampled) {
                labHover1.setForeground(Color.black);
                labHover1.setText("Perform locally averaged background fits.");
                labHover2.setForeground(Color.red);
                labHover2.setText("(Warning: Only use if probe size larger than pixel sampling.)");
            } else if (b == sldLeft) {
                labHover1.setForeground(Color.black);
                labHover1.setText("Position background window start energy.");
                labHover2.setForeground(Color.black);
                labHover2.setText("(Click slider button and use Left/Right arrows for fine positioning.)\n");
            } else if (b == sldWidth) {
                labHover1.setForeground(Color.black);
                labHover1.setText("Change background window energy width.");
                labHover2.setForeground(Color.black);
                labHover2.setText("(Click slider button and use Left/Right arrows for fine positioning.)\n");
            } else if (b == sldILeft) {
                labHover1.setForeground(Color.black);
                labHover1.setText("Position integration window start energy.");
                labHover2.setForeground(Color.black);
                labHover2.setText("(Click slider button and use Left/Right arrows for fine positioning.)\n");
            } else if (b == sldIWidth) {
                labHover1.setForeground(Color.black);
                labHover1.setText("Change integration window energy width.");
                labHover2.setForeground(Color.black);
                labHover2.setText("(Click slider button and use Left/Right arrows for fine positioning.)\n");
            } else if (b == sldZoom) {
                labHover1.setForeground(Color.black);
                labHover1.setText("Change zoom level.");
                labHover2.setForeground(Color.black);
                labHover2.setText("(Click slider button and use Left/Right arrows for fine positioning.)\n");
            } else if (b == sldOffset) {
                labHover1.setForeground(Color.black);
                labHover1.setText("Move viewing window.");
                labHover2.setForeground(Color.black);
                labHover2.setText("(Click slider button and use Left/Right arrows for fine positioning.)\n");
            } else if (b == comFit) {
                labHover1.setForeground(Color.black);
                labHover1.setText("Select funtional form for background fit.");
                labHover2.setForeground(Color.black);
                labHover2.setText("(LCPL = Linear Combination of 2 Power Laws.)");
            }
        }

        public void mouseExited(MouseEvent e) {
                labHover1.setForeground(Color.black);
                labHover1.setText("(Right-click for options/help)");
                labHover2.setForeground(Color.black);
                labHover2.setText("Copyright Cornell University 2015: \nCite doi:10.1017/S1431927612000244");
        }
    }

    /*
     * Event handler for all of the scroll bars.
     */
    private class ScrollListener implements ChangeListener {

        public void stateChanged(ChangeEvent e) {
            Object s = e.getSource();
            if (s == sldOffset) {  //The offset slider value changed
                state.windowOffset = sldOffset.getValue();
            } else if (s == sldZoom) { //The zoom slider value changed
                state.zoomfactor = Math.pow(2.0, sldZoom.getValue() / 10.0);
            } else if (s == sldLeft) { //The background position slider value changed
                state.X0 = sldLeft.getValue();
                txtLeft.setText(String.format("%.1f", state.x[state.X0]));
                if (sldLeft.getValue()+sldWidth.getValue()>=state.size) {
                    sldWidth.setValue(state.size-sldLeft.getValue()-1);
                    txtWidth.setText(String.format("%.1f", state.x[state.X1] - state.x[state.X0]));
                    sldWidth.repaint();
                }
                state.X1 = sldLeft.getValue()+sldWidth.getValue();
            }  else if (s == sldWidth) { //The background width slider value changed
                sldIWidth.repaint();
                if (sldLeft.getValue()+sldWidth.getValue()>=state.size) {
                    sldWidth.setValue(state.size-sldLeft.getValue()-1);
                }
                state.X1 = sldLeft.getValue()+sldWidth.getValue();
                txtWidth.setText(String.format("%.1f", state.x[state.X1] - state.x[state.X0]));
            } else if (s == sldILeft) { //The integration position slider value changed
                state.iX0 = sldILeft.getValue();
                txtILeft.setText(String.format("%.1f", state.x[state.iX0]));
                if (sldILeft.getValue()+sldIWidth.getValue()>=state.size) {
                    sldIWidth.setValue(state.size-sldILeft.getValue()-1);
                    txtIWidth.setText(String.format("%.1f", state.x[state.iX1] - state.x[state.iX0]));
                    sldIWidth.repaint();
                }
                state.iX1 = sldILeft.getValue()+sldIWidth.getValue();
            }  else if (s == sldIWidth) { //The integration width slider value changed
                if (sldILeft.getValue()+sldIWidth.getValue()>=state.size) {
                    sldIWidth.setValue(state.size-sldILeft.getValue()-1);
                    sldIWidth.repaint();
                }
                state.iX1 = sldILeft.getValue()+sldIWidth.getValue();
                txtIWidth.setText(String.format("%.1f", state.x[state.iX1] - state.x[state.iX0]));
            } else if (s == sldCLeft) {  //The left calibration slider value changed
                if (twoptcalib) {
                    if (sldCLeft.getValue() > sldCRight.getValue()) {
                        sldCRight.setValue(sldCLeft.getValue());
                        sldCRight.repaint();
                    }
                }
                state.cX0 = sldCLeft.getValue();
                txtLeftCalibration.setText(String.format("%.1f", state.x[state.cX0]));
            } else if (s == sldCRight) { //The right calibration slider value changed
                if (twoptcalib) {
                    if (sldCRight.getValue() < sldCLeft.getValue()) {
                        sldCLeft.setValue(sldCRight.getValue());
                        sldCLeft.repaint();
                    }
                }
                state.cX1 = sldCRight.getValue();
                txtRightCalibration.setText(String.format("%.1f", state.x[state.cX1]));
            }
            state.pwin.pack();
            state.updateProfile();
        }
    }

    /*
     * Adds all of the pertinent buttons to the gui.
     */
    private void addInitialButtons(Frame frame) {
        Panel zooming = new Panel();
        zooming.setLayout(new GridLayout(2, 2));
        zooming.add(new Label("Energy Window Zoom"));
        zooming.add(new Label("Energy Window Offset"));
        zooming.addMouseListener(new TestListener());
        sldZoom = new JSlider(0, 50, 0);
        sldZoom.addChangeListener(new ScrollListener());
        sldZoom.addMouseListener(new TestListener());
        zooming.add(sldZoom);
        sldOffset = new JSlider(0, state.size, 0);
        sldOffset.addChangeListener(new ScrollListener());
        sldOffset.addMouseListener(new TestListener());
        zooming.add(sldOffset);

        panButtons = new Panel(new GridBagLayout());
        panButtons.addMouseListener(new TestListener());
        String[] fits = {"No Fit", "Constant", "Exponential", "Linear", "Power", "LCPL"};
        comFit = new JComboBox();
        for (int i=0; i<fits.length; i++)
            comFit.addItem(fits[i]);
        state.setFit(SpectrumData.NO_FIT);
        comFit.addItemListener(new TestListener());
        comFit.addMouseListener(new TestListener());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        panButtons.add(comFit, c);

        bgFit = new ButtonGroup();
        radFast = new JRadioButton("Fast", true);
        radFast.addActionListener(new TestListener());
        radFast.addMouseListener(new TestListener());
        bgFit.add(radFast);
        radOversampled = new JRadioButton("Oversampled", false);
        radOversampled.addActionListener(new TestListener());
        radOversampled.addMouseListener(new TestListener());
        bgFit.add(radOversampled);
        panRad.add(radFast);
        panRad.add(radOversampled);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        panButtons.add(panRad,c);

        panOver = new Panel();
        Label lbl = new Label("Probe FWHM (pixels)");
        panOver.add(lbl);
        txtOversampling = new TextField("0.0");
        panOver.add(txtOversampling);
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 1;
        panOver.setVisible(false);
        panButtons.add(panOver,c);


        butSubtract = new JButton("Background Subtract Dataset");
        butSubtract.addActionListener(new TestListener());
        butSubtract.addMouseListener(new TestListener());
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        panButtons.add(butSubtract, c);

        panSliders = new Panel();
        panSliders.setLayout(new GridBagLayout());
        panSliders.addMouseListener(new TestListener());

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 3;
        JLabel labStart = new JLabel("Start");
        panSliders.add(labStart, c);
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 3;
        panSliders.add(new Label("Width"), c);

        sldLeft = new JSlider(0, state.size, 0);
        sldLeft.addChangeListener(new ScrollListener());
        sldLeft.addMouseListener(new TestListener());
        state.X0 = 0;
        sldWidth = new JSlider(0, state.size, state.size/20);
        sldWidth.addChangeListener(new ScrollListener());
        sldWidth.addMouseListener(new TestListener());
        state.X1 = state.X0 + state.size/20;

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 4;
        labSubtract = new Label("Background");
        panSliders.add(labSubtract, c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 4;
        panSliders.add(sldLeft, c);
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 4;
        panSliders.add(sldWidth, c);

        labEnergy1 = new Label("(" + state.xLabel + ")");
        txtLeft = new TextField(String.format("%.1f", state.x[state.X0]));
        txtLeft.addActionListener(new TestListener());;
        txtLeft.addMouseListener(new TestListener());;
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 5;
        Panel pleft = new Panel();
        pleft.add(txtLeft);
        pleft.add(labEnergy1);
        panSliders.add(pleft, c);
        labEnergy2 = new Label("(" + state.xLabel + ")");
        txtWidth = new TextField(String.format("%.1f", state.x[state.X1] - state.x[state.X0]));
        txtWidth.addActionListener(new TestListener());;
        txtWidth.addMouseListener(new TestListener());;
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 5;
        Panel pwidth = new Panel();
        pwidth.add(txtWidth);
        pwidth.add(labEnergy2);
        panSliders.add(pwidth, c);

        Panel spacer = new Panel();
        spacer.setPreferredSize(new Dimension(3,3));

        c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 0;
        panButtons.add(spacer, c);

        c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 1;
        panButtons.add(spacer, c);


        butIntegrate = new JButton("Integrate");
        butIntegrate.addActionListener(new TestListener());
        butIntegrate.addMouseListener(new TestListener());
        c = new GridBagConstraints();
        c.gridx = 4;
        c.gridy = 0;
        panButtons.add(butIntegrate, c);
        butHCMIntegrate = new JButton("HCM Integrate");
        butHCMIntegrate.addActionListener(new TestListener());

        butPCA = new JButton("PCA");
        butPCA.addActionListener(new TestListener());
        butPCA.addMouseListener(new TestListener());
        c = new GridBagConstraints();
        c.gridx = 4;
        c.gridy = 1;
        panButtons.add(butPCA, c);
        
        sldILeft = new JSlider(0, state.size, 0);
        sldILeft.addChangeListener(new ScrollListener());
        sldILeft.addMouseListener(new TestListener());
        state.iX0 = 0;
        sldIWidth = new JSlider(0, state.size, state.size/20);
        sldIWidth.addChangeListener(new ScrollListener());
        sldIWidth.addMouseListener(new TestListener());
        state.iX1 = state.iX0 + state.size/20;
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 6;
        labIntegrate = new Label("Integration");
        panSliders.add(labIntegrate, c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 6;
        panSliders.add(sldILeft, c);
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 6;
        panSliders.add(sldIWidth, c);

        labEnergy3 = new Label("(" + state.xLabel + ")");
        txtILeft = new TextField(String.format("%.1f", state.x[state.iX0]));
        txtILeft.addActionListener(new TestListener());
        txtILeft.addMouseListener(new TestListener());
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 7;
        Panel pileft = new Panel();
        pileft.add(txtILeft);
        pileft.add(labEnergy3);
        panSliders.add(pileft, c);
        labEnergy4 = new Label("(" + state.xLabel + ")");
        txtIWidth = new TextField(String.format("%.1f", state.x[state.iX1] - state.x[state.iX0]));
        txtIWidth.addActionListener(new TestListener());
        txtIWidth.addMouseListener(new TestListener());
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 7;
        Panel piwidth = new Panel();
        piwidth.add(txtIWidth);
        piwidth.add(labEnergy4);
        panSliders.add(piwidth, c);
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 8;
//        panSliders.add(new Label("(Right-click for options/help)"), c);
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 9;
//        panSliders.add(new Label("Copyright Cornell University 2010"), c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 8;
        labHover1 = new Label("(Right-click for options/help)");
        panSliders.add(labHover1, c);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 9;
        labHover2 = new Label("Copyright Cornell University 2010: \nCite doi:10.1017/S1431927612000244");
        panSliders.add(labHover2, c);
        if (Runtime.getRuntime().maxMemory() < 1000000000) {
            labHover1.setText("Warning: ImageJ running with less than 1GB of memory.");
            labHover1.setForeground(Color.red);
            labHover2.setText("set more in Edit>Options>Memory&Threads");
            labHover2.setForeground(Color.red);
        }
        panAll.setLayout(new BoxLayout(panAll, BoxLayout.Y_AXIS));
//        c = new GridBagConstraints();
//        c.gridx = 0;
//        c.gridy = 0;
        panAll.add(zooming);
//        c = new GridBagConstraints();
//        c.gridx = 0;
//        c.gridy = 1;
        panAll.add(panButtons);
//        c = new GridBagConstraints();
//        c.gridx = 0;
//        c.gridy = 2;
        panAll.add(panSliders);
        panAll.add(labHover1);
        panAll.add(labHover2);
        frame.add(panAll);
        butPCA.setPreferredSize(butIntegrate.getPreferredSize());
        comFit.setPreferredSize(new Dimension(panRad.getPreferredSize().width, butIntegrate.getPreferredSize().height));
        panOver.setPreferredSize(butSubtract.getPreferredSize());
    }

    /*
     * Adds elements of the gui used for recalibration.
     */
    private void addCalibrateSliders(Container conSliders, Container conButtons) {
        GridBagConstraints c = new GridBagConstraints();

        if (twoptcalib) {
            sldCLeft = new JSlider(0, state.size-1, state.cX0);
            sldCLeft.addChangeListener(new ScrollListener());
            sldCRight = new JSlider(0, state.size-1, state.cX1);
            sldCRight.addChangeListener(new ScrollListener());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            labCalibrate = new Label("Calibration");
            conSliders.add(labCalibrate, c);
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            conSliders.add(sldCLeft, c);
            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 1;
            conSliders.add(sldCRight, c);
            panCalibrateL = new Panel(new FlowLayout());
            txtLeftCalibration = new TextField(String.format("%.1f", state.x[state.cX0]));
            panCalibrateL.add(txtLeftCalibration);
            txtEnergyCalibration = new TextField(state.xLabel);
            panCalibrateL.add(txtEnergyCalibration);
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 2;
            conSliders.add(panCalibrateL, c);
            panCalibrateR = new Panel(new FlowLayout());
            txtRightCalibration = new TextField(String.format("%.1f", state.x[state.cX1]));
            panCalibrateR.add(txtRightCalibration);
            panCalibrateR.add(new Label("(" + state.xLabel + ")"));
            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 2;
            conSliders.add(panCalibrateR, c);
        } else {
            state.cX1 = state.size-1;
            sldCLeft = new JSlider(0, state.size-1, state.cX0);
            sldCLeft.addChangeListener(new ScrollListener());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            labCalibrate = new Label("Calibration");
            conSliders.add(labCalibrate, c);
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            conSliders.add(sldCLeft, c);
            panCalibrateL = new Panel(new FlowLayout());
            txtLeftCalibration = new TextField(String.format("%.1f", state.x[state.cX0]));
            panCalibrateL.add(txtLeftCalibration);
            txtEnergyCalibration = new TextField(state.xLabel);
            panCalibrateL.add(txtEnergyCalibration);
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 2;
            conSliders.add(panCalibrateL, c);

            panCalibrateR = new Panel(new FlowLayout());
            Label labChan = new Label("Channel Size");
            panCalibrateR.add(labChan);
            txtRightCalibration = new TextField(String.format("%.1f", state.x[1]-state.x[0]));
            panCalibrateR.add(txtRightCalibration);
            panCalibrateR.add(new Label("(" + state.xLabel + "/ch)"));
            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 1;
            conSliders.add(panCalibrateR, c);
        }

        panCalibrateButtons = new Panel(new FlowLayout());

        butCalibrate = new JButton("Do Calibration");
        butCalibrate.addActionListener(new TestListener());
        panCalibrateButtons.add(butCalibrate);

        butCancelCalibration = new JButton("Cancel Calibration");
        butCancelCalibration.addActionListener(new TestListener());
        panCalibrateButtons.add(butCancelCalibration);
        c.gridx = 1;
        c.gridy = 0;
        conSliders.add(panCalibrateButtons, c);
    }

    /*
     * Removes elements of the gui used for recalibration.
     */
    private void removeCalibrateSliders(Container conSliders, Container conButtons) {
        conSliders.remove(panCalibrateButtons);
        conSliders.remove(labCalibrate);
        conSliders.remove(sldCLeft);
        conSliders.remove(panCalibrateL);
        if (twoptcalib) {
            conSliders.remove(sldCRight);
        }
        conSliders.remove(panCalibrateR);
    }

    /*
     * Creates the popup menu to be shown on right-clicks
     */
    private void setupMenu() {
        pm = new JPopupMenu();
        JMenu optionsMenu = new JMenu("Options");
        miScaleCounts = new JCheckBoxMenuItem("Auto-scale background subtracted data.");
        miScaleCounts.addItemListener(new TestListener());
        optionsMenu.add(miScaleCounts);

        miMeanCentering = new JCheckBoxMenuItem("Do mean centering for the PCA.", false);
        miMeanCentering.addItemListener(new TestListener());
        optionsMenu.add(miMeanCentering);

        miWeightedPCA = new JCheckBoxMenuItem("Weight the PCA.", false);
        miWeightedPCA.addItemListener(new TestListener());
        optionsMenu.add(miWeightedPCA);

        
        JMenu colorMenu = new JMenu("Change color scheme.");
        miChangeColorCSI = new JMenuItem("CSI Classic");
        miChangeColorCSI.addActionListener(new TestListener());
        colorMenu.add(miChangeColorCSI);
        miChangeColorCornell = new JMenuItem("Cornell");
        miChangeColorCornell.addActionListener(new TestListener());
        colorMenu.add(miChangeColorCornell);
        miChangeColorCollegiate = new JMenuItem("Collegiate");
        miChangeColorCollegiate.addActionListener(new TestListener());
        colorMenu.add(miChangeColorCollegiate);
        miChangeColorCorporate = new JMenuItem("Corporate");
        miChangeColorCorporate.addActionListener(new TestListener());
        colorMenu.add(miChangeColorCorporate);
        optionsMenu.add(colorMenu);


        JMenu calibrationMenu = new JMenu("Calibrate Energy-axis");
        miTwoPointCalibration = new JMenuItem("Recalibrate with two sample points.");
        miTwoPointCalibration.addActionListener(new TestListener());
        calibrationMenu.add(miTwoPointCalibration);
        miOnePointCalibration = new JMenuItem("Recalibrate with one sample point and the channel size.");
        miOnePointCalibration.addActionListener(new TestListener());
        calibrationMenu.add(miOnePointCalibration);
        optionsMenu.add(calibrationMenu);

        pm.add(optionsMenu);

        JMenu helpMenu = new JMenu("Help");
        miAbout =  new JMenuItem("About CSI: Cornell Spectrum Imager");
        miAbout.addActionListener(new TestListener());
        helpMenu.add(miAbout);
        miDoc = new JMenuItem("Documentation");
        miDoc.addActionListener(new TestListener());
        helpMenu.add(miDoc);
        pm.add(helpMenu);
    }

    /*
     * Generic least squares fit class.
     */
    abstract class Fit {
        double ymin=1;
        public Jama.Matrix Residual;
        Jama.Matrix createFit(double[] x, Jama.Matrix y, int start, int end) {
            int s = end-start;
            int col = y.getColumnDimension();
            Jama.Matrix m = new Jama.Matrix(s, 2);
            Jama.Matrix n = new Jama.Matrix(s, col);
            Jama.Matrix coeffs = new Jama.Matrix(2, col);

            //ymin = (new JamaDenseDoubleMatrix2D(y)).getMinValue();
            //if (ymin>1)
            //    ymin = 1;
            //y.plusEquals(new Jama.Matrix(y.getRowDimension(), col, -ymin+1));

            for (int k = 0; k < s; k++) {
                m.set(k, 0, 1);
                m.set(k, 1, fx(x[k+start]));
                for (int i = 0; i < col; i++) {
                    n.set(k, i, fy(y.get(k+start, i)));
                }
            }


            try {
                coeffs = m.solve(n);
                Residual = m.times(coeffs).minus(n);
            } catch (Exception e) {
                return coeffs;
            }
            return coeffs;
        }

        protected abstract double getFitAtX(double c0, double c1, double xi);

        protected abstract double fx(double xi);

        protected abstract double fy(double yi);
    }

    /*
     * Fit class for when no fitting is desired.
     */
    private class NoFit extends Fit {
        @Override
        Jama.Matrix createFit(double[] x, Jama.Matrix y, int start, int end) {
            return new Jama.Matrix(2,y.getColumnDimension());
        }

        protected double getFitAtX(double c0, double c1, double xi) {return 0;}
        protected double fx(double xi) {return 0;}
        protected double fy(double yi) {return 0;}
    }

    /*
     * Fit class for a constant function.
     */
    private class ConstantFit extends Fit {
        @Override
        Jama.Matrix createFit(double[] x, Jama.Matrix y, int start, int end) {
            int s = end-start;
            int col = y.getColumnDimension();
            Jama.Matrix m = new Jama.Matrix(s, 1);
            Jama.Matrix n = new Jama.Matrix(s, col);
            Jama.Matrix coeffs = new Jama.Matrix(1, col);

            for (int k = 0; k < s; k++) {
                m.set(k, 0, 1);
                for (int i = 0; i < col; i++) {
                    n.set(k, i, fy(y.get(k+start, i)));
                }
            }
            try {
                coeffs = m.solve(n);
            } catch (Exception e) {
                return (new Jama.Matrix(2,1,1.0)).times(coeffs);
            }
            return (new Jama.Matrix(2,1,1.0)).times(coeffs);
        }
        protected double getFitAtX(double c0, double c1, double xi) {
            return c0;
        }
        protected double fx(double xi) {
            return 0;
        }
        protected double fy(double yi) {
            return yi;
        }
    }

    /*
     * Fit class for a linear function.
     */
    private class LinearFit extends Fit {
        protected double getFitAtX(double c0, double c1, double xi) {
            return c0 + c1*xi + ymin - 1;
        }
        protected double fx(double xi) {
            return xi;
        }
        protected double fy(double yi) {
            return yi;
        }
    }

    /*
     * Fit class for a exponential function.
     */
    private class ExponentialFit extends Fit {
        protected double getFitAtX(double c0, double c1, double xi) {
            if (c0==0&&c1==0)
                return 0;
            return Math.exp(c0 + c1*xi)+ymin-1;
        }
        protected double fx(double xi) {
            return xi;
        }
        protected double fy(double yi) {
            return Math.log(Math.max(1E-3, yi));
        }
    }

    /*
     * Fit class for a power law function.
     */
    private class PowerFit extends Fit {
        protected double getFitAtX(double c0, double c1, double xi) {
            if (c0==0&&c1==0)
                return 0;
            return Math.exp(c0 + Math.log(xi)*c1)+ymin-1;
        }
        protected double fx(double xi) {
            return Math.log(Math.max(1E-3, xi));
        }
        protected double fy(double yi) {
            return Math.log(Math.max(1E-3, yi));
        }
    }

    /*
     * Fit class for a linear combination of power laws (LCPL)
     */
    private class LCPLFit extends Fit {
        double R1 = 0, R2 = 0;
        double min = .2, max = .8;
        @Override
        Jama.Matrix createFit(double[] x, Jama.Matrix y, int start, int end) {
            double[] powerLawCoeffs = (new PowerFit()).createFit(x,y,start,end).getArray()[1];
            Arrays.sort(powerLawCoeffs);
            R1 = powerLawCoeffs[(int)(powerLawCoeffs.length*min)];
            R2 = Math.min(powerLawCoeffs[(int)(powerLawCoeffs.length*max)], 0);

            int s = end-start;
            int col = y.getColumnDimension();
            Jama.Matrix m = new Jama.Matrix(s, 2);
            Jama.Matrix n = new Jama.Matrix(s, col);
            Jama.Matrix coeffs = new Jama.Matrix(2, col);
            //ymin = (new JamaDenseDoubleMatrix2D(y)).getMinValue();
            //if (ymin>1)
            //    ymin = 1;
            //y.plusEquals(new Jama.Matrix(y.getRowDimension(), col, -ymin+1));

            for (int k = 0; k < s; k++) {
                m.set(k, 0, Math.pow(x[k+start], R1));
                m.set(k, 1, Math.pow(x[k+start], R2));
                for (int i = 0; i < col; i++) {
                    n.set(k, i, fy(y.get(k+start, i)));
                }
            }


            try {
                coeffs = m.solve(n);
                Residual = m.times(coeffs).minus(n);
            } catch (Exception e) {
                return coeffs;
            }
            return coeffs;
        }
        protected double getFitAtX(double c0, double c1, double xi) {
            if (c0==0&&c1==0)
//            if (c0==0)
                return 0;
            return c0*Math.pow(xi, R1)+c1*Math.pow(xi, R2);
        }
        protected double fx(double xi) {
            return 0;
        }
        protected double fy(double yi) {
            return yi;
        }
    }


    private class ModelFit {
        Jama.Matrix[] backgroundsAndEdges = null;
        
        void createModelNoG(double[] x, Jama.Matrix y, int bStart, int bEnd, int eStart, int eEnd, Fit fit) {
            Jama.Matrix bcoeffs = fit.createFit(x, y, bStart, bEnd);

            backgroundsAndEdges = new Jama.Matrix[y.getColumnDimension()];
            for (int p=0; p<y.getColumnDimension(); p++) {
                backgroundsAndEdges[p] = new Jama.Matrix(bEnd-bStart+eEnd-eStart,1);
                for (int e=0; e<bEnd-bStart; e++) {
                    backgroundsAndEdges[p].set(e,0, fit.getFitAtX(bcoeffs.get(0,p), bcoeffs.get(1,p), x[e+bStart]));
                } for (int e=0; e<eEnd-eStart; e++) {
                    backgroundsAndEdges[p].set(e+bEnd-bStart,0, fit.getFitAtX(bcoeffs.get(0,p), bcoeffs.get(1,p), x[e+eStart]));
                }
            }
        }

       double[][] createFitNoG(double[] x, Jama.Matrix y, int bStart, int bEnd, int eStart, int eEnd) {
            int sb = bEnd-bStart;
            int se = eEnd-eStart;
            int col = y.getColumnDimension();
            Jama.Matrix m;
            Jama.Matrix n;
            Jama.Matrix coeffs;
            double[][] bAndE = new double[2][col];
            double pix;
            
            for (int p=0; p<col; p++) {
                m = new Jama.Matrix(sb, 1);
                n = new Jama.Matrix(sb, 1);
                coeffs = new Jama.Matrix(1, 1);
                for (int e = 0; e < sb; e++) {
                    m.set(e, 0, backgroundsAndEdges[p].get(e,0));
                    n.set(e, 0, y.get(e+bStart, p));
                }
                try {
                    coeffs = m.solve(n);
                } catch (Exception e) {
                    return bAndE;
                }
                bAndE[0][p]=coeffs.get(0,0);
                pix = 0;
                for (int e = 0; e < se; e++) {
                    pix+=y.get(e+eStart, p)-bAndE[0][p]*backgroundsAndEdges[p].get(e+sb,0);
                }
                bAndE[1][p]=pix;
            }
            return bAndE;
        }
    }




    /*
     * Generic class for dealing with all of the data in the spectrum image.
     */
    private abstract class SpectrumData implements MouseListener, MouseMotionListener, Measurements, KeyListener {

        static final int NO_FIT=0;
        static final int CONSTANT_FIT=1;
        static final int LINEAR_FIT=2;
        static final int EXPONENTIAL_FIT=3;
        static final int POWER_FIT=4;
        static final int LCPL_FIT=5;

        boolean listenersRemoved, scaleCounts;

        double[] x,y,yfit,ysubtracted;
        int size, X0, X1, iX0, iX1, cX0, cX1, plotHeight, plotWidth, marginHeight, marginWidth;
        double zoomfactor, windowOffset; //Plot properties, zoom and offset
        String xLabel, yLabel; //Axis labels
        ImagePlus img;
        Fit fit;
        PlotWindow pwin;


        abstract ImagePlus integrate(int fitStart, int fitEnd, int intStart, int intEnd);
        abstract ImagePlus HCMintegrate(int fitStart, int fitEnd, int intStart, int intEnd);
        abstract void PCA(int fitStart, int fitEnd, int intStart, int intEnd);
        abstract void weightedPCA(int fitStart, int fitEnd, int intStart, int intEnd);
        abstract ImagePlus subtract(int fitStart, int fitEnd);
        abstract ImagePlus fitToModel(int fitStart, int fitEnd, int intStart, int intEnd);
        abstract ImagePlus fitToBosman(int fitStart, int fitEnd, int intStart, int intEnd);
        abstract double[] getProfile();
        abstract int getSize();

        /*
         * Initializes variables.
         */
        void setup(ImagePlus img) {
            this.img = img;
            size = getSize(); //number of slices in stack
            cX1 = size-1;
            y = getProfile();
            zoomfactor = 1;   //Default zoom factor 1x zoom.
            windowOffset = 0; //Default, no window offset
            setFit(NO_FIT);   //Default 'No Fit'
            if (y != null) {
                x = new double[y.length];
                Calibration cal = img.getCalibration();
                for (int i = 0; i < x.length; i++) {
                    x[i] = (i - cal.zOrigin) * cal.pixelDepth;
                }
                yLabel = cal.getValueUnit();
                xLabel = cal.getZUnit();
                updateProfile();
                ImageWindow win = img.getWindow();
                win.addWindowListener(win);
                ImageCanvas canvas = win.getCanvas();
                canvas.addMouseListener(this);
                canvas.addMouseMotionListener(this);
                canvas.addKeyListener(this);
                positionPlotWindow();
            }
        }

        private class ResizeListener implements ComponentListener {

        public void componentResized(ComponentEvent e) {
            plotHeight = Math.max(e.getComponent().getSize().height - marginHeight,0);
            plotWidth = Math.max(e.getComponent().getSize().width-marginWidth,0);
            IJ.run("Profile Plot Options...", "width="+plotWidth+" height="+plotHeight+" minimum=0 maximum=0");
            updateProfile();
        }

        public void componentMoved(ComponentEvent e) {}

        public void componentShown(ComponentEvent e) {}

        public void componentHidden(ComponentEvent e) {}

        }

        void updateProfile() {
           
            checkPlotWindow();
            if (listenersRemoved || y == null || y.length == 0) {
                return;
            }

            Plot plot = new Plot("CSI: Cornell Spectrum Imager - "+img.getTitle(), xLabel, yLabel, x, y);
            plot.setColor(colData);


            double[] a = Tools.getMinMax(x);
            double xmin = a[0] + (a[1] - a[0]) * windowOffset * (zoomfactor - 1) / (size * zoomfactor);
            double xmax = xmin + (a[1] - a[0]) / zoomfactor;
            double[] yrange = new double[(int)Math.ceil(size / zoomfactor)];
            System.arraycopy(y, (int) (windowOffset * (zoomfactor - 1) / zoomfactor), yrange, 0, (int)Math.ceil(size / zoomfactor));
            double[] ysubrange = new double[(int)Math.ceil(size / zoomfactor)];

            Jama.Matrix coeffs = fit.createFit(x, new Jama.Matrix(y, size), X0, X1);

            yfit = new double[size];
            ysubtracted = new double[size];
            double c0, c1;
            c0 = coeffs.get(0,0);
            c1 = coeffs.get(1,0);
            for (int j = 0; j < size; j++) {
                yfit[j] = fit.getFitAtX(c0, c1, x[j]);
                if (Math.abs(yfit[j]) > Math.abs(10*y[j]))
                    ysubtracted[j] = 0;
                else if ((j < X0)||yfit[j]==0)
                    ysubtracted[j] = 0;
                else
                    ysubtracted[j] = y[j]-yfit[j];
            }


            System.arraycopy(ysubtracted, (int) (windowOffset * (zoomfactor - 1) / zoomfactor), ysubrange, 0, (int)Math.ceil(size / zoomfactor));

            double scale;
            if ((Tools.getMinMax(ysubrange)[1]-Tools.getMinMax(ysubrange)[0])==0)
                scale = 1;
            else
                scale = (Tools.getMinMax(yrange)[1] - Tools.getMinMax(yrange)[0]) / (Tools.getMinMax(ysubrange)[1] - Tools.getMinMax(ysubrange)[0]);
            double ysubmin = Tools.getMinMax(ysubrange)[0];
            double ysubmax = Tools.getMinMax(ysubrange)[1];
            double yrangemin = Tools.getMinMax(yrange)[0];
            double shift = yrangemin - scale*ysubmin;
            if (scaleCounts) {
                for (int j = 0; j < size; j++) {
                    double ysj = ysubtracted[j];
                    ysj-= ysubmin;
                    ysj*= scale;
                    ysj+= yrangemin;
                    ysubtracted[j] = ysj;
                    ysj-= ysubmin;
                    ysj*= scale;
                    ysj+= yrangemin;
                }
            }

            System.arraycopy(ysubtracted, (int) (windowOffset * (zoomfactor - 1) / zoomfactor), ysubrange, 0, (int)Math.ceil(size / zoomfactor));

            double ymax;
            double ymin;
            if (scaleCounts) {
                ymin = Math.min(Tools.getMinMax(yrange)[0],yrange[0]);
                ymax = Math.max(Tools.getMinMax(yrange)[1],yrange[0]);
            }
            else{
                ymin = Math.min(Math.min(0, Tools.getMinMax(ysubrange)[0]), Tools.getMinMax(yrange)[0]);
                ymax = Math.max(Tools.getMinMax(yrange)[1], Tools.getMinMax(ysubrange)[1]);
            }

            plot.setLimits(xmin, xmax, Math.floor(ymin - 3*(ymax-ymin)/(plotHeight)), Math.ceil(ymax + 3*(ymax-ymin)/(plotHeight)));
            plot.setColor(Color.black);
            ImageProcessor ipplot = plot.getProcessor();
            if (pwin == null) {
                pwin = plot.show();
                pwin.noGridLines = true;
                pwin.addComponentListener(new ResizeListener());
            }
            
            FloodFiller ff = new FloodFiller(ipplot);
            ipplot.setColor(colDataFill);
            ff.fill(plot.LEFT_MARGIN+(plotWidth)/2, plot.TOP_MARGIN+1);
            ff.fill(plot.LEFT_MARGIN+(plotWidth)/2, plotHeight-1);
            double[] zero = new double[size];
            Arrays.fill(zero, 0.0);
            plot.setColor(Color.black);
            plot.addPoints(x, zero, Plot.LINE);
            ipplot.setColor(colBackFill);
            ff.fill(plot.LEFT_MARGIN+(plotWidth)/2, plot.TOP_MARGIN+1);
            if (ymin<0)
                ff.fill(plot.LEFT_MARGIN+(plotWidth)/2, plotHeight+plot.TOP_MARGIN-1);

            if (scaleCounts) {
                Arrays.fill(zero, shift);
                plot.setColor(colZeroLine);
                plot.addPoints(x, zero, Plot.LINE);
                ipplot.setColor(colZeroLine);
                ipplot.drawString(String.format("%.1f", ysubmin), 0, plotHeight -16);
                ipplot.drawString(String.format("%.1f", ysubmax), 0, plot.TOP_MARGIN+24);
            }

            plot.setColor(colBackgroundFit);
            plot.addPoints(x, yfit, Plot.LINE);
            plot.setColor(colSubtracted);
            plot.addPoints(x, ysubtracted, Plot.LINE);


            drawWindow(X0, X1, colBackgroundWindow, plot);
            drawWindow(iX0, iX1, colIntWindow, plot);

            if (isCalibrating) {
                drawWindow(cX0, cX1, Color.black, plot);
            }
            pwin.drawPlot(plot);
            marginWidth = pwin.getSize().width-pwin.plotWidth ;
            marginHeight = pwin.getSize().height-pwin.plotHeight;
        }

        void drawWindow(int xI, int xF, Color c, Plot plot) {
            ImageProcessor ipplot = plot.getProcessor();
            ipplot.setColor(c);
            int xIdraw = (int) (xI * zoomfactor - windowOffset * (zoomfactor - 1));
            int xFdraw = (int) (xF * zoomfactor - windowOffset * (zoomfactor - 1));
            if ((xIdraw > 0) && (xIdraw < size)) {
                ipplot.drawRect(plot.LEFT_MARGIN + ((plotWidth) * xIdraw) / size, plot.TOP_MARGIN, 1, (plotHeight));
            }
            if ((xFdraw > 0) && (xFdraw < size)) {
                ipplot.drawRect(plot.LEFT_MARGIN + ((plotWidth) * xFdraw) / size, plot.TOP_MARGIN, 1, (plotHeight));
            }
        }

        void setFit(int fitType) {
            switch (fitType) {
                case NO_FIT: {
                    fit = new NoFit();
                    break;
                }
                case CONSTANT_FIT: {
                    fit = new ConstantFit();
                    break;
                }
                case LINEAR_FIT: {
                    fit = new LinearFit();
                    break;
                }
                case EXPONENTIAL_FIT: {
                    fit = new ExponentialFit();
                    break;
                }
                case POWER_FIT: {
                    fit = new PowerFit();
                    break;
                }
                case LCPL_FIT: {
                    fit = new LCPLFit();
                    break;
                }
            }
        }

        void positionPlotWindow() {
            if (pwin == null || img == null) {
                return;
            }
            ImageWindow iwin = img.getWindow();
            if (iwin == null) {
                return;
            }
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            pwin.setSize(screen.width/3,screen.height/2); //SET the SIZE of CSI WINDOW
            //SETTING SIZE HERE, IS NOT ALWAYS CONSISTENT, THERE IS A BUG - RMH
            
            Dimension plotSize = pwin.getSize();
            Dimension imageSize = iwin.getSize();
            
            if (plotSize.width == 0 || imageSize.width == 0) {
                return;
            }
            Point imageLoc = iwin.getLocation();
            int w = imageLoc.x + imageSize.width + 10; //place window just right of spectrum data
            if (w + plotSize.width > screen.width) {
                w = screen.width - plotSize.width;
            }
            pwin.setLocation(w, imageLoc.y); //position window next to spectrum
            iwin.toFront();
        }

        /*
         * Gets the Z values through a single point at (x,y).
         */
        public void mousePressed(MouseEvent e) {
            Roi roi = img.getRoi();
            ImageStack stack = img.getStack();
            ImageProcessor ip;
            double[] values = new double[size];
            Rectangle r = roi.getBoundingRect();
            if ((r.width == 0 || r.height == 0) || (r.width == 1 && r.height == 1)) {
                int xpoint = e.getX();
                int ypoint = e.getY();
                float[] cTable = img.getCalibration().getCTable();
                for (int p = 1; p <= size; p++) {
                    ip = stack.getProcessor(p);
                    ip.setCalibrationTable(cTable);
                    values[p - 1] = ip.getPixelValue(xpoint, ypoint);
                }
                y = values;
                updateProfile();
            }
        }

        public void mouseDragged(MouseEvent e) {
            y = getProfile();
            updateProfile();
        }

        public void keyReleased(KeyEvent e) {
            y = getProfile();
            updateProfile();
        }


        /*
         * Stop listening for mouse and key events if the plot window has been closed.
         */
        void checkPlotWindow() {
            if (pwin == null) {
                return;
            }
            if (pwin.isVisible()) {
                return;
            }
            ImageWindow iwin = img.getWindow();
            if (iwin == null) {
                return;
            }
            ImageCanvas canvas = iwin.getCanvas();
            canvas.removeMouseListener(this);
            canvas.removeMouseMotionListener(this);
            canvas.removeKeyListener(this);
            pwin = null;
            listenersRemoved = true;
        }

        public void keyPressed(KeyEvent e) {}
        public void keyTyped(KeyEvent e) {}

        public void mouseReleased(MouseEvent e) {
            y = getProfile();
            updateProfile();
        }

        public void mouseExited(MouseEvent e) {}
        public void mouseClicked(MouseEvent e) {}
        public void mouseEntered(MouseEvent e) {}
        public void mouseMoved(MouseEvent e) {}

        private void recalibrateImage() {}
        private void recalibrate() {
            double e0, e1, b, m;
            if (twoptcalib) {
                try {
                    e0 = Double.parseDouble(txtLeftCalibration.getText());
                } catch (Exception e) {
                    IJ.error("Error", "Please enter a valid Energy 1.");
                    return;
                }
                try {
                    e1 = Double.parseDouble(txtRightCalibration.getText());
                } catch (Exception e) {
                    IJ.error("Error", "Please enter a valid Energy 2.");
                    return;
                }
            } else {
                try {
                    e0 = Double.parseDouble(txtLeftCalibration.getText());
                } catch (Exception e) {
                    IJ.error("Error", "Please enter a valid Energy 1.");
                    return;
                }
                try {
                    e1 = e0 + Double.parseDouble(txtRightCalibration.getText());
                    cX1 = cX0+1;
                } catch (Exception e) {
                    IJ.error("Error", "Please enter a valid Energy Per Channel.");
                    return;
                }
            }
            if (cX0>=cX1) {
                IJ.error("Error", "Can't calibrate: window is too small.");
                return;
            } else if (e0>=e1) {
                IJ.error("Error", "Can't calibrate: energy range is nonpositive.");
                return;
            }
            xLabel = txtEnergyCalibration.getText();

            m = (e1 - e0) / (cX1 - cX0);
            b = e0 - m * cX0;
            for (int i = 0; i < x.length; i++) {
                x[i] = m * i + b;
            }
            isCalibrating = false;
            Calibration cal = img.getCalibration();
            cal.pixelDepth = m;
            cal.zOrigin = -b/(m);
            cal.setZUnit(xLabel);
            labEnergy1.setText("("+xLabel+")");
            labEnergy2.setText("("+xLabel+")");
            labEnergy3.setText("("+xLabel+")");
            labEnergy4.setText("("+xLabel+")");
            txtLeft.setText(String.format("%.1f", state.x[state.X0]));
            txtWidth.setText(String.format("%.1f", state.x[state.X1]-state.x[state.X0]));
            txtILeft.setText(String.format("%.1f", state.x[state.iX0]));
            txtIWidth.setText(String.format("%.1f", state.x[state.iX1]-state.x[state.iX0]));
            recalibrateImage();

            updateProfile();
            removeCalibrateSliders(panSliders, panButtons);
            pwin.pack();
        }

        void updateProgress(double progress) {
            if (progress == 1)
                pwin.setTitle("CSI: Cornell Spectrum Imager - "+img.getTitle());
            else
                pwin.setTitle("(Working: %" + String.format("%.0f", progress * 100) + ") CSI: Cornell Spectrum Imager - "+img.getTitle());
        }
    }

	//Class for 2D spectrum maps.
    private class SpectrumData2D extends SpectrumData {

        int getSize() {
            return img.getStackSize();
        }

        ImagePlus fitToBosman(int fitStart, int fitEnd, int intStart, int intEnd) {
            ModelFit mf = new ModelFit();
            int width = img.getWidth();
            int height = img.getHeight();
            ImageStack stack = img.getStack();
            ImageProcessor ip;
            ImagePlus bos;

            Jama.Matrix yMat = new Jama.Matrix(fitEnd-fitStart, width * height);
            for (int k = fitStart; k < fitEnd; k++) {
                updateProgress(k * 1.0 / (2 * (fitEnd-fitStart)));
                ip = stack.getProcessor(k + 1);
                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        yMat.set(k-fitStart, width * i + j, ip.getf(j, i));
                    }
                }
            }

            long[] sizes = {(long)yMat.getRowDimension(),(long)yMat.getColumnDimension() };
            JamaDenseDoubleMatrix2D yMatUJMP = new JamaDenseDoubleMatrix2D( sizes );
            yMatUJMP.setWrappedObject(yMat);
            Matrix[] USV = yMatUJMP.svd();
            Matrix S = USV[1];
            GenericDialog gd = new GenericDialog("How many components for Bosman?");
            gd.addNumericField("Number of components:", 1, 3);
            gd.showDialog();
            int comp = (int)gd.getNextNumber();
            for (int i=comp;i<S.getRowCount(); i++) {
                updateProgress(i * 1.0 / (2 * (S.getRowCount())));
                S.setAsDouble(0.0,(long)i,(long)i);
            }
            ImageStack stackfilt = new ImageStack(0,0);
            try{
            Matrix filt = USV[2].mtimes(Calculation.NEW, true, S.mtimes(Calculation.NEW, true,USV[0].transpose(Calculation.NEW)));
            filt = filt.transpose(Calculation.NEW);
            stackfilt = new ImageStack(width, height);
            for (int i = 0; i<size; i++) {
                updateProgress(i * 1.0 / (2 * (size)) + .5);
                if (i>=fitStart&&i<fitEnd)
                    stackfilt.addSlice("", new FloatProcessor(width, height, filt.toDoubleArray()[i-fitStart]));
                else
                    stackfilt.addSlice("", new FloatProcessor(width, height));
            }

            } catch (Exception e) {
                IJ.error(e.toString());
            }
            bos = new ImagePlus("bosman", stackfilt);
            bos.setCalibration(img.getCalibration());
            bos.getCalibration().zOrigin = -x[fitStart]/bos.getCalibration().pixelDepth;
            bos.show();

            return bos;
        }

        ImagePlus fitToModel(int fitStart, int fitEnd, int intStart, int intEnd) {
            ModelFit mf = new ModelFit();
            int width = img.getWidth();
            int height = img.getHeight();
            ImageStack stack = img.getStack();
            double filtersize = 0;
            try{
                filtersize = Double.parseDouble(txtOversampling.getText());
            } catch (NumberFormatException nfe) {
                txtOversampling.setText("0.0");
            }
            if (filtersize > 0){
                img.saveRoi();
                img.setRoi(0,0,width, height);
                ImagePlus imgfilter = img.duplicate();
                img.restoreRoi();
//                IJ.run(imgfilter, "Median...", "radius="+filtersize+"1 stack");
                IJ.run(imgfilter, "Gaussian Blur...", "sigma="+filtersize*0.42466+" stack");
                stack = imgfilter.getStack();
            }
            ImageProcessor ip;
            ImageProcessor ipcoeff0;
            ImageProcessor ipcoeff1;

            Jama.Matrix yMat = new Jama.Matrix(size, width * height);
            for (int k = 0; k < size; k++) {
                updateProgress(k * 1.0 / (2 * size));
                ip = stack.getProcessor(k + 1);
                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        yMat.set(k, width * i + j, ip.getf(j, i));
                    }
                }
            }
            mf.createModelNoG(x, yMat, fitStart, fitEnd, intStart, intEnd, fit);

//            IJ.run("Convolve...", "text1=[-1 -4 -6 -4 -1\n-4 -16 -24 -16 -4\n-5 -20 -30 -20 -5\n0 0 0 0 0\n5 20 30 20 5\n4 16 24 16 4\n1 4 6 4 1\n] normalize stack");
//            IJ.run("Convolve...", "text1=[-1 -4 -5 0 5 4 1\n-4 -6 -20 0 20 16 4\n-6 -24 -30 0 30 24 6\n-4 -6 -20 0 20 16 4\n-1 -4 -5 0 5 4 1\n] normalize stack");
            stack = img.getStack();
            for (int k = 0; k < size; k++) {
                updateProgress(k * 1.0 / (2 * size) + .5);
                ip = stack.getProcessor(k + 1);
                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        yMat.set(k, width * i + j, ip.getf(j, i));
                    }
                }
            }
            double[][] coeffs;
            coeffs = mf.createFitNoG(x, yMat, fitStart, fitEnd, intStart, intEnd);
            updateProgress(1);
            ipcoeff0 = new FloatProcessor(width, height, coeffs[0]);
            ipcoeff1 = new FloatProcessor(width, height, coeffs[1]);
            return new ImagePlus("Integrated from " +
                    String.format("%.1f", state.x[intStart]) + " " + state.xLabel + " to " +
                    String.format("%.1f", state.x[intEnd]) + " " + state.xLabel + " of " + String.format("%.1f", filtersize) +
                    " oversampled background subtracted via " +
                    comFit.getSelectedItem().toString().toLowerCase() + " fit from " +
                    String.format("%.1f", state.x[fitStart]) + " " + state.xLabel + " to " +
                    String.format("%.1f", state.x[fitEnd]) + " " + state.xLabel +" "+img.getTitle(),ipcoeff1 );

        }

        ImagePlus subtract(int fitStart, int fitEnd) {
            int width = img.getWidth();
            int height = img.getHeight();
            ImageStack stack = img.getStack();
            double filtersize = 0;
            try{
                filtersize = Double.parseDouble(txtOversampling.getText());
            } catch (NumberFormatException nfe) {
                txtOversampling.setText("0.0");
            }
            if (filtersize > 0){
                img.saveRoi();
                img.setRoi(0,0,width, height);
                ImagePlus imgfilter = img.duplicate();
                img.restoreRoi();
//                IJ.run(imgfilter, "Median...", "radius="+filtersize+" stack");
                IJ.run(imgfilter, "Gaussian Blur...", "radius="+filtersize*0.42466+" stack");
                stack = imgfilter.getStack();
            }
            ImageStack stacksub = new ImageStack(width, height);
            ImageStack stackresidual = new ImageStack(width, height);
            ImageProcessor ipsub;
            ImageProcessor ip;
            ImageProcessor ipcoeff0;
            ImageProcessor ipcoeff1;
            ImageProcessor ipresidual = new FloatProcessor(1,1);
            ImagePlus imgsub;
            double pix;


            Jama.Matrix yMat = new Jama.Matrix(size, width * height);
            for (int k = 0; k < size; k++) {
                updateProgress(k * 1.0 / (2 * size));
                ip = stack.getProcessor(k + 1);
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        yMat.set(k, height * i + j, ip.getf(i, j));
                    }
                }
            }
            Jama.Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);
            stack = img.getStack();

//            ipcoeff0 = new FloatProcessor(height, width, coeffs.getArray()[0]);
//            (new ImagePlus("coeff0",ipcoeff0 )).show();
//            ipcoeff1 = new FloatProcessor(height, width, coeffs.getArray()[1]);
//            (new ImagePlus("coeff1",ipcoeff1 )).show();

            for (int k = 0; k < size; k++) {
                updateProgress(k * 1.0 / (2 * size) + .5);
                if (k < fitStart) {
                    stacksub.addSlice(stack.getSliceLabel(k+1), stack.getProcessor(k + 1).createProcessor(width, height));
                } else {
                    ipsub = stack.getProcessor(k + 1).duplicate();
                    stacksub.addSlice(stack.getSliceLabel(k+1), ipsub);
                    if (k>fitStart&&k<fitEnd) {
                        ipresidual = stack.getProcessor(k + 1).duplicate();
                        stackresidual.addSlice(stack.getSliceLabel(k+1), ipresidual);
                    }
                    for (int i = 0; i < width; i++) {
                        for (int j = 0; j < height; j++) {
                            pix = ipsub.getPixelValue(i, j) - fit.getFitAtX(coeffs.get(0, height * i + j), coeffs.get(1, height * i + j), x[k]);
                            ipsub.putPixelValue(i, j, pix);
                            if (k>fitStart&&k<fitEnd)
                                ipresidual.putPixelValue(i, j, Math.exp(fit.Residual.get(k-fitStart, height * i + j)));
                        }
                    }
                }
            }
//            (new ImagePlus("Residual", stackresidual)).show();

            updateProgress(1);
            imgsub = new ImagePlus("Background subtracted via " +
                    comFit.getSelectedItem().toString().toLowerCase() + " fit from " +
                    String.format("%.1f", state.x[fitStart]) + " " + state.xLabel + " to " +
                    String.format("%.1f", state.x[fitEnd]) + " " + state.xLabel +" "+ img.getTitle(), stacksub);
            imgsub.setCalibration(img.getCalibration());
            imgsub.resetDisplayRange();

            return imgsub;
        }

        ImagePlus integrate(int fitStart, int fitEnd, int intStart, int intEnd) {
            int width = img.getWidth();
            int height = img.getHeight();
            ImageStack stack = img.getStack();
//            if (medianSize == 0){
//                stack = fitToBosman(fitStart, fitEnd, intStart, intEnd).getStack();
//            }
            ImageProcessor ip;
            ImageProcessor ipint = stack.getProcessor(1).createProcessor(width, height);
            ImagePlus imgint = new ImagePlus("Integrated from " +
                    String.format("%.1f", state.x[intStart]) + " " + state.xLabel + " to " +
                    String.format("%.1f", state.x[intEnd]) + " " + state.xLabel + " of background subtracted via " +
                    comFit.getSelectedItem().toString().toLowerCase() + " fit from " +
                    String.format("%.1f", state.x[fitStart]) + " " + state.xLabel + " to " +
                    String.format("%.1f", state.x[fitEnd]) + " " + state.xLabel +" " +img.getTitle(), ipint);
            double pix, c0, c1;



            Jama.Matrix yMat = new Jama.Matrix(size, width * height);
            for (int k = 0; k < size; k++) {
                updateProgress(k * 1.0 / (2 * size));
                ip = stack.getProcessor(k + 1);
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        yMat.set(k, height * i + j, ip.getf(i, j));
                    }
                }
            }
            Jama.Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);
            stack = img.getStack();

            for (int i = 0; i < width; i++) {
                updateProgress(.5 + i * 1.0 / (2 * width));
                for (int j = 0; j < height; j++) {
                    c0 = coeffs.get(0, height * i + j);
                    c1 = coeffs.get(1, height * i + j);
                    pix = stack.getProcessor(intStart + 1).getf(i, j);
                    pix-= fit.getFitAtX(c0, c1, x[intStart]);
                    pix+= stack.getProcessor(intEnd + 1).getf(i, j);
                    pix-= fit.getFitAtX(c0, c1, x[intEnd]);
                    for (int k = intStart + 1; k < intEnd - 1; k++) {
                        pix+= stack.getProcessor(k + 1).getf(i, j);
                        pix-= fit.getFitAtX(c0, c1, x[k]);
                    }
                    ipint.putPixelValue(i, j, pix);
                }
            }
            imgint.setCalibration(img.getCalibration());
            imgint.resetDisplayRange();
            updateProgress(1);
            return imgint;
        }

        ImagePlus HCMintegrate(int fitStart, int fitEnd, int intStart, int intEnd) {
            int width = img.getWidth();
            int height = img.getHeight();
            ImageStack stack = img.getStack();
            ImageProcessor ip;
            ImageProcessor ipint = stack.getProcessor(1).duplicate();
            ImagePlus imgint = new ImagePlus(img.getTitle() + " HCM integrated from " +
                    String.format("%.1f", state.x[intStart]) + " " + state.xLabel + " to " +
                    String.format("%.1f", state.x[intEnd]) + " " + state.xLabel, ipint);
            double pix, c0, c1, s, f;

            Jama.Matrix yMat = new Jama.Matrix(size, width * height);
            for (int k = 0; k < size; k++) {
                updateProgress(k * 1.0 / (2 * size));
                ip = stack.getProcessor(k + 1);
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        yMat.set(k, height * i + j, ip.getf(i, j));
                    }
                }
            }
            Jama.Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);

            for (int i = 0; i < width; i++) {
                updateProgress(.5 + i * 1.0 / (2 * width));
                for (int j = 0; j < height; j++) {
                    c0 = coeffs.get(0, height * i + j);
                    c1 = coeffs.get(1, height * i + j);
                    f = stack.getProcessor(intStart + 1).getf(i, j);
                    s = f-fit.getFitAtX(c0, c1, x[intStart]);
                    pix = s*s/(2*f);
                    f = stack.getProcessor(intEnd + 1).getf(i, j);
                    s = f-fit.getFitAtX(c0, c1, x[intEnd]);
                    pix+= s*s/(2*f);
                    for (int k = intStart + 1; k < intEnd - 1; k++) {
                        f = stack.getProcessor(k + 1).getf(i, j);
                        s = f-fit.getFitAtX(c0, c1, x[k]);
                        pix+= s*s/f;
                    }
                    ipint.putPixelValue(i, j, pix);
                }
            }
            imgint.setCalibration(img.getCalibration());
            imgint.resetDisplayRange();
            updateProgress(1);
            return imgint;
        }

        void PCA(int fitStart, int fitEnd, int pcaStart, int pcaEnd) {
            int width = img.getWidth();
            int height = img.getHeight();
            ImageStack stack = img.getStack();

            double filtersize = 0;
            try{
                filtersize = Double.parseDouble(txtOversampling.getText());
            } catch (NumberFormatException nfe) {
                txtOversampling.setText("0.0");
            }
            if (filtersize > 0){
                img.saveRoi();
                img.setRoi(0,0,width, height);
                ImagePlus imgfilter = img.duplicate();
                img.restoreRoi();
//                IJ.run(imgfilter, "Median...", "radius="+filtersize+" stack");
                IJ.run(imgfilter, "Gaussian Blur...", "radius="+filtersize*0.42466+" stack");
                stack = imgfilter.getStack();
            }

            ImageProcessor ip;
            ImageStack stackpca;
            ImageStack stacksmooth;
            Plot[] stackplot;
            double pix, c0, c1, f;
            double[] pcax = new double[pcaEnd-pcaStart];

            Jama.Matrix yMat = new Jama.Matrix(size, width * height);
            for (int k = 0; k < size; k++) {
                updateProgress(k/(size*4.0));
                ip = stack.getProcessor(k + 1);
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        yMat.set(k, width * j + i, ip.getf(i, j));
                    }
                }
            }
            Jama.Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);

            stack = img.getStack();
            yMat = new Jama.Matrix(pcaEnd-pcaStart, width*height);
            for (int k = pcaStart; k < pcaEnd; k++) {
                updateProgress((k-pcaStart)/((pcaEnd-pcaStart)*4.0) + .25);
                ip = stack.getProcessor(k + 1);
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        c0 = coeffs.get(0, width * j + i);
                        c1 = coeffs.get(1, width * j + i);
                        yMat.set(k-pcaStart, width * j + i, ip.getf(i, j)-fit.getFitAtX(c0, c1, x[k]));
                    }
                }
            }
            pwin.setTitle("(Working: %50) [Doing Singular Value Composition: may take a few minutes.]  CSI: Cornell Spectrum Imager - "+img.getTitle());

            long[] sizes = {(long)yMat.getRowDimension(),(long)yMat.getColumnDimension() };
            JamaDenseDoubleMatrix2D yMatUJMP = new JamaDenseDoubleMatrix2D( sizes );
            yMatUJMP.setWrappedObject(yMat);
            if (meanCentering)
                yMatUJMP.center(Calculation.ORIG, Matrix.ROW, true);
            Matrix[] USV = yMatUJMP.svd();
            Matrix S = USV[1];
            //Jama.SingularValueDecomposition pcasvd = yMat.svd();


            double[] s = new double[Math.min((int)USV[1].getRowCount(),(int)USV[1].getColumnCount())];
            double[] n = new double[s.length];
            double sMax = USV[1].max(Calculation.NEW, Matrix.ALL).getAsDouble((long)0,(long)0);
            double c = 1E4;
            for (int i = 0; i < n.length; i++) {
                s[i] = Math.log(1+c*USV[1].getAsDouble((long)i,(long)i)/sMax);
                n[i] = i+1;
                if (i<3)
                    S.setAsDouble(0, (long)i,(long)i);
            }
            Matrix V = USV[2];
            Matrix Vt = V.transpose();
            Matrix U = USV[0];
            Matrix Ut = U.transpose();
            //try{
            //    Matrix resid = (S.mtimes(Calculation.NEW, true, Vt));
            //    resid = resid.sum(Calculation.NEW, Matrix.ROW, true);
            //    (new ImagePlus(img.getTitle()+" residual", new FloatProcessor(width, height, resid.toDoubleArray()[0]))).show();
            //} catch (Exception e) {
            //    IJ.error(e.toString());
            //}
            //try{
            //    Matrix smoothed = V.mtimes(Calculation.NEW, true, S.mtimes(Calculation.NEW, true, U));
            //    smoothed = smoothed.transpose();
            //    stacksmooth = new ImageStack(width, height);
            //    for (int i = 0; i<smoothed.getRowCount(); i++) {
            //        stacksmooth.addSlice("", new FloatProcessor(width, height, smoothed.toDoubleArray()[i]));
            //    }
            //    (new ImagePlus(img.getTitle()+" PCA smoothed!", stacksmooth)).show();
            //} catch (Exception e) {
            //    IJ.error(e.toString());
            //}

            Plot scree = new Plot("Scree Plot", "Principal Component Number", "log(1+c*PC Amplitude/PCmax)", n,s);
            PlotWindow screewin = scree.show();

            System.arraycopy(x, pcaStart, pcax, 0, pcaEnd-pcaStart);
            stackplot = new Plot[s.length];
            stackpca = new ImageStack(width, height);
            double[] Ui = new double[pcax.length];
            try {
            for (int i = 0; i<s.length; i++) {
                updateProgress(.5*i/Ut.getRowCount() + .5);
                stackpca.addSlice("", new FloatProcessor(width, height, Vt.toDoubleArray()[i]));
                for (int j = 0; j<pcax.length; j++) {
                    Ui[j] = Ut.getAsDouble(i,j);
                }
                stackplot[i] = new Plot("PCA Spectra "+img.getTitle(), xLabel, yLabel, pcax, Ui);
            }
            } catch (Exception e) {
                IJ.error("wacky "+e.toString());
            }
            updateProgress(1);
            ImagePlus maps = new ImagePlus("PCA Concentrations "+img.getTitle(), stackpca);
            maps.show();
            maps.resetDisplayRange();
            PlotWindow spectrum = stackplot[0].show();
            PCAwindows PCAw = new PCAwindows();
            PCAw.setup(screewin, spectrum, stackplot, maps, sMax, c);
        }

        void weightedPCA(int fitStart, int fitEnd, int pcaStart, int pcaEnd) {
            int width = img.getWidth();
            int height = img.getHeight();
            ImageStack stack = img.getStack();
            ImageProcessor ip;
            ImageStack stackpca;
            Plot[] stackplot;
            ImagePlus imgpca;
            double pix, c0, c1, f;
            double[] pcax = new double[pcaEnd-pcaStart];

            Jama.Matrix yMat = new Jama.Matrix(size, width * height);
            for (int k = 0; k < size; k++) {
                updateProgress(k/(size*4.0));
                ip = stack.getProcessor(k + 1);
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        yMat.set(k, width * j + i, ip.getf(i, j));
                    }
                }
            }
            Jama.Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);

            yMat = new Jama.Matrix(pcaEnd-pcaStart, width*height);
            for (int k = pcaStart; k < pcaEnd; k++) {
                ip = stack.getProcessor(k + 1);
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        yMat.set(k-pcaStart, width * j + i, ip.getf(i, j));
                    }
                }
            }
            JamaDenseDoubleMatrix2D yMatUJMP = new JamaDenseDoubleMatrix2D( (long)yMat.getRowDimension(),(long)yMat.getColumnDimension() );
            yMatUJMP.setWrappedObject(yMat);

            Matrix g = yMatUJMP.sum(Calculation.NEW, Matrix.COLUMN, true).divide(yMatUJMP.getColumnCount()*1.0).abs(Calculation.ORIG).power(Calculation.ORIG, -.5);
            Matrix h = yMatUJMP.sum(Calculation.NEW, Matrix.ROW, true).divide(yMatUJMP.getRowCount()*1.0).abs(Calculation.ORIG).power(Calculation.ORIG, -.5);
            g = g.divide(g.getValueSum());
            h = h.divide(h.getValueSum());

            yMat = new Jama.Matrix(pcaEnd-pcaStart, width*height);
            for (int k = pcaStart; k < pcaEnd; k++) {
                updateProgress((k-pcaStart)/((pcaEnd-pcaStart)*4.0) + .25);
                ip = stack.getProcessor(k + 1);
                for (int j = 0; j < height; j++) {
                    for (int i = 0; i < width; i++) {
                        c0 = coeffs.get(0, width * j + i);
                        c1 = coeffs.get(1, width * j + i);
                        yMat.set(k-pcaStart, width * j + i, ip.getf(i, j)-fit.getFitAtX(c0, c1, x[k]));
                    }
                }
            }

            yMatUJMP.setWrappedObject(yMat);

            for (int i = 0; i < yMatUJMP.getRowCount(); i++) {
                for (int j = 0; j < yMatUJMP.getColumnCount(); j++) {
                    yMatUJMP.setAsDouble(yMatUJMP.getAsDouble(i,j)*g.getAsDouble(i,0)*h.getAsDouble(0,j), i, j);
                }
            }
            pwin.setTitle("(Working: %50) [Doing Singular Value Composition: may take a few minutes.]  CSI: Cornell Spectrum Imager - "+img.getTitle());
            if (meanCentering)
                yMatUJMP.center(Calculation.ORIG, Matrix.ROW, true);
            Matrix[] USV = yMatUJMP.svd();
            pwin.setTitle("(Working: %50) CSI: Cornell Spectrum Imager - "+img.getTitle());


            Matrix Vt = USV[2].transpose();
            Matrix U =  USV[0];
            double[] s = new double[Math.min((int)USV[1].getRowCount(),(int)USV[1].getColumnCount())];
            double[] n = new double[s.length];
            double sMax = USV[1].max(Calculation.NEW, Matrix.ALL).getAsDouble((long)0,(long)0);
            double c = 1E4;
            try{
            for (int i = 0; i < n.length; i++) {
                s[i] = Math.log(1+c*USV[1].getAsDouble((long)i,(long)i)/sMax);
                n[i] = i+1;
                for (int j = 0; j < yMatUJMP.getColumnCount(); j++) {
                    Vt.setAsDouble(Vt.getAsDouble(i,j)/h.getAsDouble(0,j), i, j);
                }
                for (int j = 0; j < yMatUJMP.getRowCount(); j++) {
                    U.setAsDouble(U.getAsDouble(j,i)/g.getAsDouble(i,0), j, i);
                }
            }
            } catch (Exception e) {
                IJ.error(e.toString());
            }

            Plot scree = new Plot("Scree Plot", "Principal Component Number", "log(1+c*PC Amplitude/PCmax)", n,s, Plot.DOT);
            PlotWindow screewin = scree.show();

            System.arraycopy(x, pcaStart, pcax, 0, pcaEnd-pcaStart);
            stackplot = new Plot[s.length];
            stackpca = new ImageStack(width, height);
            double[] Ui = new double[pcax.length];
            
            try{for (int i = 0; i<s.length; i++) {
                updateProgress(.5*i/U.getRowCount() + .5);
                stackpca.addSlice("", new FloatProcessor(width, height, Vt.toDoubleArray()[i]));
                for (int j = 0; j<pcax.length; j++) {
                    Ui[j] = U.getAsDouble(j,i);
                }
                stackplot[i] = new Plot("PCA Spectra "+img.getTitle(), xLabel, yLabel, pcax, Ui);
            }
            }catch (Exception e) {
                IJ.error(e.toString());
            }
            updateProgress(1);

            ImagePlus maps = new ImagePlus("PCA Concentrations "+img.getTitle(), stackpca);
            maps.show();
            maps.resetDisplayRange();
            PlotWindow spectrum = stackplot[0].show();
            PCAwindows PCAw = new PCAwindows();
            PCAw.setup(screewin, spectrum, stackplot, maps, sMax, c);
        }

        double[] getProfile() {
            Roi roi = img.getRoi();
            if (roi == null) {
                return null;
            }
            ImageStack stack = img.getStack();
            double[] values = new double[size];
            Calibration cal = img.getCalibration();
            ImageProcessor ip;
            ImageStatistics stats;
            for (int i = 1; i <= size; i++) {
                ip = stack.getProcessor(i);
                ip.setRoi(roi);
                stats = ImageStatistics.getStatistics(ip, MEAN, cal);
                values[i - 1] = (double) stats.mean;
            }
            double[] extrema = Tools.getMinMax(values);
            if (Math.abs(extrema[1]) == Double.MAX_VALUE) {
                return null;
            } else {
                return values;
            }
        }

        void recalibrateImage() {

            ImageStack ims = img.getStack();
            for (int i = 0; i < x.length; i++) {
                ims.setSliceLabel("("+x[i]+" "+xLabel+")",i);
            }
            img.setStack(ims);

        }

    }
    private class SpectrumData1D extends SpectrumData {
        int getSize() {
            return img.getWidth();
        }

        ImagePlus fitToModel(int fitStart, int fitEnd, int intStart, int intEnd) {
            return integrate(fitStart, fitEnd, intStart, intEnd);
        }
        ImagePlus fitToBosman(int fitStart, int fitEnd, int intStart, int intEnd) {
            return null;
        }

        ImagePlus integrate(int fitStart, int fitEnd, int intStart, int intEnd) {
            int height = img.getHeight();
            ImageProcessor ip = img.getProcessor();
            ImageProcessor ipint = ip.resize(1,height);
            double pix, c0, c1;
            ImagePlus imgint = new ImagePlus("Integrated from " +
                    String.format("%.1f", state.x[intStart]) + " " + state.xLabel + " to " +
                    String.format("%.1f", state.x[intEnd]) + " " + state.xLabel + "of background subtracted via " +
                    comFit.getSelectedItem().toString().toLowerCase() + " fit from " +
                    String.format("%.1f", state.x[fitStart]) + " " + state.xLabel + " to " +
                    String.format("%.1f", state.x[fitEnd]) + " " + state.xLabel +" "+ img.getTitle()+img.getTitle(), ipint);

            Jama.Matrix yMat = new Jama.Matrix(size, height);
            for (int k = 0; k < size; k++) {
                updateProgress(k * 1.0 / (2 * size));
                for (int i = 0; i < height; i++) {
                        yMat.set(k, i, ip.getf(k, i));
                }
            }
            Jama.Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);

            for (int i = 0; i < height; i++) {
                updateProgress(i * 1.0 / (2 * size) + .5);
                c0 = coeffs.get(0, i);
                c1 = coeffs.get(1, i);
                pix = ip.getf(intStart, i)/2 - fit.getFitAtX(c0, c1, x[intStart]);
                pix+= ip.getf(intEnd, i)/2 - fit.getFitAtX(c0, c1, x[intEnd]);
                for (int j = intStart+1; j < intEnd; j++) {
                        pix += ip.getf(j, i)/1 - fit.getFitAtX(c0, c1, x[j]);
                }
                ipint.putPixelValue(0, i, pix);
            }
            updateProgress(1);
            imgint.setCalibration(img.getCalibration());
            imgint.resetDisplayRange();
            imgint.setRoi(0,0,1,height);
            ProfilePlot pp = new ProfilePlot(imgint, true);
            pp.createWindow();
            return new ImagePlus();
        }

        ImagePlus HCMintegrate(int fitStart, int fitEnd, int intStart, int intEnd) {
            int height = img.getHeight();
            ImageProcessor ip = img.getProcessor();
            ImageProcessor ipint = ip.resize(1,height);
            ImagePlus imgint = new ImagePlus(img.getTitle() + " HCM integrated from " +
                    String.format("%.1f", x[intStart]) + " " + xLabel + " to " +
                    String.format("%.1f", x[intEnd]) + " " + xLabel, ipint);
            double pix, c0, c1, s ,f;

            Jama.Matrix yMat = new Jama.Matrix(size, height);
            for (int k = 0; k < size; k++) {
                updateProgress(k * 1.0 / (2 * size));
                for (int i = 0; i < height; i++) {
                        yMat.set(k, i, ip.getf(k, i));
                }
            }
            Jama.Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);

            for (int i = 0; i < height; i++) {
                updateProgress(i * 1.0 / (2 * size) + .5);
                c0 = coeffs.get(0, i);
                c1 = coeffs.get(1, i);
                f = ip.getf(intStart, i);
                s = f - fit.getFitAtX(c0, c1, x[intStart]);
                pix = s*s/(2*f);
                f = ip.getf(intEnd, i);
                s = f - fit.getFitAtX(c0, c1, x[intEnd]);
                pix+= s*s/(2*f);
                for (int j = intStart+1; j < intEnd; j++) {
                        f = ip.getf(j, i);
                        s = f - fit.getFitAtX(c0, c1, x[j]);
                        pix+= s*s/f;
                }
                ipint.putPixelValue(0, i, pix);
            }
            updateProgress(1);
            imgint.setCalibration(img.getCalibration());
            imgint.resetDisplayRange();
            return imgint;
        }

        void PCA(int fitStart, int fitEnd, int pcaStart, int pcaEnd) {
            int height = img.getHeight();
            ImageProcessor ip = img.getProcessor();
            ImageStack stackpca;
            Plot[] stackplot;
            double pix, c0, c1, f;
            double[] pcax = new double[pcaEnd-pcaStart];

            Jama.Matrix yMat = new Jama.Matrix(size, height);
            for (int k = 0; k < size; k++) {
                updateProgress(k/(size*4.0));
                for (int j = 0; j < height; j++) {
                    yMat.set(k, j, ip.getf(k, j));
                }
            }
            Jama.Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);

            yMat = new Jama.Matrix(pcaEnd-pcaStart, height);
            for (int k = pcaStart; k < pcaEnd; k++) {
                updateProgress((k-pcaStart)/((pcaEnd-pcaStart)*4.0) + .25);
                for (int j = 0; j < height; j++) {
                    c0 = coeffs.get(0, j);
                    c1 = coeffs.get(1, j);
                    yMat.set(k-pcaStart, j, ip.getf(k, j)-fit.getFitAtX(c0, c1, x[k]));
                }
            }
            pwin.setTitle("(Working: %50) [Doing Singular Value Composition: may take a few minutes.]  CSI: Cornell Spectrum Imager - "+img.getTitle());

            long[] sizes = {(long)yMat.getRowDimension(),(long)yMat.getColumnDimension() };
            JamaDenseDoubleMatrix2D yMatUJMP = new JamaDenseDoubleMatrix2D( sizes );
            yMatUJMP.setWrappedObject(yMat);
            if (meanCentering)
                yMatUJMP.center(Calculation.ORIG, Matrix.COLUMN, true);
            Matrix[] USV = yMatUJMP.svd();
            //Jama.SingularValueDecomposition pcasvd = yMat.svd();


            double[] s = new double[Math.min((int)USV[1].getRowCount(),(int)USV[1].getColumnCount())];
            double[] n = new double[s.length];
            double sMax = USV[1].max(Calculation.NEW, Matrix.ALL).getAsDouble((long)0,(long)0);
            double c = 1E4;
            for (int i = 0; i < n.length; i++) {
                s[i] = Math.log(1+c*USV[1].getAsDouble((long)i,(long)i)/sMax);
                n[i] = i+1;
            }
            double[] xConc = new double[yMat.getColumnDimension()];
            for (int i = 0; i < yMat.getColumnDimension(); i++) {
                xConc[i]=i;
            }
            Matrix V = USV[2].transpose();
            Matrix U = USV[0].transpose();

            Plot scree = new Plot("Scree Plot", "Principal Component Number", "log(1+c*PC Amplitude/PCmax)", n,s);
            PlotWindow screewin = scree.show();

            System.arraycopy(x, pcaStart, pcax, 0, pcaEnd-pcaStart);
            stackplot = new Plot[s.length];
            stackpca = new ImageStack(scree.getProcessor().getWidth(), scree.getProcessor().getHeight());
            double[] Ui = new double[pcax.length];
            try {
            for (int i = 0; i<s.length; i++) {
                updateProgress(.5*i/U.getRowCount() + .5);
                stackpca.addSlice("", (new Plot("", "position", yLabel, xConc, V.toDoubleArray()[i])).getProcessor());
                for (int j = 0; j<pcax.length; j++) {
                    Ui[j] = U.getAsDouble(i,j);
                }
                stackplot[i] = new Plot("PCA Spectra "+img.getTitle(), xLabel, yLabel, pcax, Ui); //The 1D Spectra Plot
            }
            } catch (Exception e) {
                IJ.error(e.toString());
            }
            updateProgress(1);
            ImagePlus maps = new ImagePlus("PCA Concentrations "+img.getTitle(), stackpca);
            maps.show();
            PlotWindow spectrum = stackplot[0].show();
            PCAwindows PCAw = new PCAwindows();
            PCAw.setup(screewin, spectrum, stackplot, maps, sMax, c);
        }

        void weightedPCA(int fitStart, int fitEnd, int pcaStart, int pcaEnd) {
            int height = img.getHeight();
            ImageProcessor ip = img.getProcessor();
            ImageStack stackpca;
            Plot[] stackplot;
            double pix, c0, c1, f;
            double[] pcax = new double[pcaEnd-pcaStart];

            Jama.Matrix yMat = new Jama.Matrix(size, height);
            for (int k = 0; k < size; k++) {
                updateProgress(k/(size*4.0));
                for (int j = 0; j < height; j++) {
                    yMat.set(k, j, ip.getf(k, j));
                }
            }
            Jama.Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);

            yMat = new Jama.Matrix(pcaEnd-pcaStart, height);
            for (int k = pcaStart; k < pcaEnd; k++) {
                for (int j = 0; j < height; j++) {
                    yMat.set(k-pcaStart, j, ip.getf(k, j));
                }
            }
            JamaDenseDoubleMatrix2D yMatUJMP = new JamaDenseDoubleMatrix2D( (long)yMat.getRowDimension(),(long)yMat.getColumnDimension() );
            yMatUJMP.setWrappedObject(yMat);

            Matrix g = yMatUJMP.sum(Calculation.NEW, Matrix.COLUMN, true).divide(yMatUJMP.getColumnCount()*1.0).abs(Calculation.ORIG).power(Calculation.ORIG, -.5);
            Matrix h = yMatUJMP.sum(Calculation.NEW, Matrix.ROW, true).divide(yMatUJMP.getRowCount()*1.0).abs(Calculation.ORIG).power(Calculation.ORIG, -.5);
            g = g.divide(g.getValueSum());
            h = h.divide(h.getValueSum());

            yMat = new Jama.Matrix(pcaEnd-pcaStart, height);
            for (int k = pcaStart; k < pcaEnd; k++) {
                updateProgress((k-pcaStart)/((pcaEnd-pcaStart)*4.0) + .25);
                for (int j = 0; j < height; j++) {
                    c0 = coeffs.get(0, j);
                    c1 = coeffs.get(1, j);
                    yMat.set(k-pcaStart, j, ip.getf(k, j)-fit.getFitAtX(c0, c1, x[k]));
                }
            }

            yMatUJMP.setWrappedObject(yMat);

            for (int i = 0; i < yMatUJMP.getRowCount(); i++) {
                for (int j = 0; j < yMatUJMP.getColumnCount(); j++) {
                    yMatUJMP.setAsDouble(yMatUJMP.getAsDouble(i,j)*g.getAsDouble(i,0)*h.getAsDouble(0,j), i, j);
                }
            }
            pwin.setTitle("(Working: %50) [Doing Singular Value Composition: may take a few minutes.]  CSI: Cornell Spectrum Imager - "+img.getTitle());

            if (meanCentering)
                yMatUJMP.center(Calculation.ORIG, Matrix.ROW, true);
            Matrix[] USV = yMatUJMP.svd();
            //Jama.SingularValueDecomposition pcasvd = yMat.svd();
            Matrix V = USV[2].transpose();
            Matrix U = USV[0].transpose();


            double[] s = new double[Math.min((int)USV[1].getRowCount(),(int)USV[1].getColumnCount())];
            double[] n = new double[s.length];
            double sMax = USV[1].max(Calculation.NEW, Matrix.ALL).getAsDouble((long)0,(long)0);
            double c = 1E4;
            try {
            for (int i = 0; i < n.length; i++) {
                s[i] = Math.log(1+c*USV[1].getAsDouble((long)i,(long)i)/sMax);
                n[i] = i+1;
                for (int j = 0; j < yMatUJMP.getColumnCount(); j++) {
                    V.setAsDouble(V.getAsDouble(i,j)/h.getAsDouble(0,j), i, j);
                }
                for (int j = 0; j < yMatUJMP.getRowCount(); j++) {
                    U.setAsDouble(U.getAsDouble(i,j)/g.getAsDouble(i,0), i, j);
                }
            }
            } catch (Exception e) {
                IJ.error(e.toString());
            }
            double[] xConc = new double[yMat.getColumnDimension()];
            for (int i = 0; i < yMat.getColumnDimension(); i++) {
                xConc[i]=i;
            }

            Plot scree = new Plot("Scree Plot", "Principal Component Number", "log(1+c*PC Amplitude/PCmax)", n,s);
            PlotWindow screewin = scree.show();

            System.arraycopy(x, pcaStart, pcax, 0, pcaEnd-pcaStart);
            stackplot = new Plot[s.length];
            stackpca = new ImageStack(scree.getProcessor().getWidth(), scree.getProcessor().getHeight());
            double[] Ui = new double[pcax.length];
            try {
            for (int i = 0; i<s.length; i++) {
                updateProgress(.5*i/U.getRowCount() + .5);
                stackpca.addSlice("", (new Plot("", xLabel, yLabel, xConc, V.toDoubleArray()[i])).getProcessor());
                for (int j = 0; j<pcax.length; j++) {
                    Ui[j] = U.getAsDouble(i,j);
                }
                stackplot[i] = new Plot("PCA Spectra "+img.getTitle(), xLabel, yLabel, pcax, Ui);
            }
            } catch (Exception e) {
                IJ.error(e.toString());
            }
            updateProgress(1);
            ImagePlus maps = new ImagePlus("PCA Concetrations "+img.getTitle(), stackpca);
            maps.show();
            PlotWindow spectrum = stackplot[0].show();
            PCAwindows PCAw = new PCAwindows();
            PCAw.setup(screewin, spectrum, stackplot, maps, sMax, c);
        }

        ImagePlus subtract(int fitStart, int fitEnd) {
            int height = img.getHeight();
            ImageProcessor ip = img.getProcessor();
            ImageProcessor ipsub = ip.createProcessor(size, height);
            ImagePlus imgsub = new ImagePlus("Background subtracted via " +
                    comFit.getSelectedItem().toString().toLowerCase() + " fit from " +
                    String.format("%.1f", state.x[fitStart]) + " " + state.xLabel + " to " +
                    String.format("%.1f", state.x[fitEnd]) + " " + state.xLabel +" "+ img.getTitle(), ipsub);
            double c0, c1;

            Jama.Matrix yMat = new Jama.Matrix(size, height);
            for (int k = 0; k < size; k++) {
                updateProgress(k * 1.0 / (2 * size));
                for (int j = 0; j < height; j++) {
                    yMat.set(k, j, ip.getf(k, j));
                }
            }
            Jama.Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);

            for (int i = 0; i < height; i++) {
                updateProgress(i * 1.0 / (2 * size) + .5);
                c0 = coeffs.get(0, i);
                c1 = coeffs.get(1, i);
                for (int j = fitStart; j < size; j++) {
                     ipsub.putPixelValue(j, i, ip.getf(j, i)/1 - fit.getFitAtX(c0, c1, x[j]));
                }
            }
            updateProgress(1);
            imgsub.setCalibration(img.getCalibration());
            imgsub.resetDisplayRange();
            return imgsub;
        }

        double[] getProfile() {
            Roi roi = img.getRoi();
            if (roi == null)
                return null;
            int ystart = (int)roi.getBounds().getY();
            int yend = ystart + (int)roi.getBounds().getHeight();
            double[] values = new double[size];
            ImageProcessor ip = img.getProcessor();

            for (int i=0; i<size; i++) {
                values[i] = 0;
                for (int j=ystart;j<yend;j++) {
                    values[i] += ip.getf(i, j);
                }
                values[i]/=(yend-ystart);
            }
            return values;
        }
        void recalibrateImage() {return;}
    }

    private class SpectrumData0D extends SpectrumData1D {
        @Override
        double[] getProfile() {
            double[] values = new double[size];
            ImageProcessor ip = img.getProcessor();

            for (int i=0; i<size; i++) {
                values[i] = ip.getf(i, 0);
            }
            return values;
        }

        @Override
        ImagePlus integrate(int fitStart, int fitEnd, int intStart, int intEnd) {
            ImagePlus imp = super.integrate(fitStart, fitEnd, intStart, intEnd);
            IJ.showMessage(imp.getTitle(), imp.getProcessor().getf(0,0) + " total " + yLabel);
            return new ImagePlus();
        }
    }

    class PCAwindows {
        PlotWindow scree;
        PlotWindow spectrum;
        Plot[] spectra;
        ImagePlus maps;
        PCAlistener pcal;
        double sMax, c;

        void setup(PlotWindow scree, PlotWindow spectrum, Plot[] spectra, ImagePlus maps, double sMax, double c) {
            this.scree = scree;
            scree.addMouseListener(new EasterEggListener());
            this.spectrum = spectrum;
            this.spectra = spectra;
            this.maps = maps;
            this.sMax = sMax;
            this.c = c;
            pcal = new PCAlistener();
            maps.addImageListener(pcal);
        }

        private class PCAlistener implements ImageListener {
            public void imageUpdated(ImagePlus imp) {
                if (imp == maps) {
                    int i = maps.getSlice();
                    Plot p = new Plot("Scree Plot", "Principal Component Number", "log(1+c*PC Amplitude/PCmax)", scree.getXValues(), scree.getYValues());
                    p.setColor(Color.red);
                    float[] ax = {(float)i};
                    float[] ay = {scree.getYValues()[i-1]};
                    p.addPoints(ax, ay, PlotWindow.X);
                    p.addLabel(1.0*i/scree.getYValues().length, .5, ""+(Math.exp(scree.getYValues()[i-1])-1)*sMax/c);
                    p.setColor(Color.black);
                    scree.drawPlot(p);

                    spectrum.drawPlot(spectra[i-1]);
                }
            }

            public void imageOpened(ImagePlus imp){return;}
            public void imageClosed(ImagePlus imp){
                if (imp == maps)
                    maps.removeImageListener(pcal);
            }
        }

        private class EasterEggListener implements MouseListener {
            public void mouseClicked(MouseEvent e) { }

            public void mousePressed(MouseEvent e) {
                if (e.isControlDown()&&e.isShiftDown()) {
                    GenericDialog gd = new GenericDialog("PCA filter");
                    gd.addNumericField("Number of components:", 1, 3);
                    gd.showDialog();
                    if (!gd.wasCanceled())
                        filter((int)gd.getNextNumber()).show();
                }
            }

            public void mouseReleased(MouseEvent e) { }

            public void mouseEntered(MouseEvent e) { }

            public void mouseExited(MouseEvent e) { }
        }

        ImagePlus filter(int components) {
            int depth = spectrum.getXValues().length;
            int width = maps.getWidth();
            int height = maps.getHeight();
            ImageStack imsf = new ImageStack(width, height);
            ImageProcessor ip;
            for (int i = 0; i<depth; i++) {
                ImageStack imscomps = new ImageStack(width, height);
                for (int comp = 0; comp<components; comp++) {
                    ip = maps.getStack().getProcessor(comp+1).duplicate();
                    spectrum.drawPlot(spectra[comp]);
                    ip.multiply((Math.exp(scree.getYValues()[comp])-1)*sMax/c * spectrum.getYValues()[i]);
                    imscomps.addSlice("",ip);
                }
                ZProjector zp = new ZProjector(new ImagePlus("", imscomps));
                zp.setMethod(ZProjector.SUM_METHOD);
                zp.doProjection();
                imsf.addSlice("",zp.getProjection().getProcessor());
            }
            ImagePlus impf = new ImagePlus("filtered", imsf);
            impf.setCalibration(img.getCalibration());
            impf.getCalibration().zOrigin = -spectrum.getXValues()[0]/impf.getCalibration().pixelDepth;
            return impf;
        }
    }

    ImagePlus filter(int components, ImagePlus maps) {
        return null;
    }

}