package com.spectrumimager.CSI;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;

/*
 * Event handler for all of the elements in the gui.
 */
class CSI_TestListener implements ActionListener, ItemListener, MouseListener {

	/**
	 * 
	 */
	private final CSI_Spectrum_Analyzer csi_Spectrum_Analyzer;

	/**
	 * @param csi_Spectrum_Analyzer
	 */
	CSI_TestListener(CSI_Spectrum_Analyzer csi_Spectrum_Analyzer) {
		this.csi_Spectrum_Analyzer = csi_Spectrum_Analyzer;
	}

	public void actionPerformed(ActionEvent e) {
		Object b = e.getSource();
		if (b == this.csi_Spectrum_Analyzer.miDoc) { // Display 'About' information
			try {
				File pdfFile = new File(IJ.getDirectory("plugins") + "CSI_Documentation.pdf");
				if (pdfFile.exists()) {

					if (Desktop.isDesktopSupported()) {
						Desktop.getDesktop().open(pdfFile);
					} else {
						IJ.showMessage("AWT Desktop is not supported!");
					}

				} else {
					IJ.showMessage("The documentation file does not exist!");
				}

				// IJ.showMessage("Done");

			} catch (NullPointerException | IOException ex) {
				ex.printStackTrace();
			}
		} else if (b == this.csi_Spectrum_Analyzer.miAbout) { // Display 'About' information
			try {
				IJ.openImage(IJ.getDirectory("plugins") + "CSI.png").show();
			} catch (Exception ex) {
				IJ.showMessage("About CSI: Cornell Spectrum Imager",
						"Spectrum analyzer ImageJ plugin developed at Cornell University \n \n"
								+ "                                  by Paul Cueva, Robert Hovden, and David A. Muller\n "
								+ "         School of Applied and Engineering Physics, Cornell University, Ithaca, NY 14853 \n "
								+ "                                 Kavli Institute at Cornell for Nanoscale Science\n \n"
								+ "                            with support from DOE BES, NSF MRSEC, and NYSTAR\n \n"
								+ "                                              version 1.6  14 02 2015");
			}
		} else if (b == this.csi_Spectrum_Analyzer.miTwoPointCalibration) {
			if (!this.csi_Spectrum_Analyzer.isCalibrating) { // If not currently calibrating
				this.csi_Spectrum_Analyzer.isCalibrating = true; // set two ppoint calibration mode to true
				this.csi_Spectrum_Analyzer.twoptcalib = true; // and add calibration GUI elements
				this.csi_Spectrum_Analyzer.addCalibrateSliders(this.csi_Spectrum_Analyzer.panSliders,
						this.csi_Spectrum_Analyzer.panButtons);
				this.csi_Spectrum_Analyzer.state.pwin.pack();
			} else {
				if (!this.csi_Spectrum_Analyzer.twoptcalib) { // If currently calibrating, but not
					this.csi_Spectrum_Analyzer.removeCalibrateSliders(this.csi_Spectrum_Analyzer.panSliders,
							this.csi_Spectrum_Analyzer.panButtons); // in two point mode, then remove

					this.csi_Spectrum_Analyzer.twoptcalib = true; // current calibration GUI elements

					this.csi_Spectrum_Analyzer.addCalibrateSliders(this.csi_Spectrum_Analyzer.panSliders,
							this.csi_Spectrum_Analyzer.panButtons); // and replace with two point GUI elements

					this.csi_Spectrum_Analyzer.state.pwin.pack();
				}
			}
			this.csi_Spectrum_Analyzer.state.updateProfile();
		} else if (b == this.csi_Spectrum_Analyzer.miOnePointCalibration) {
			if (!this.csi_Spectrum_Analyzer.isCalibrating) { // If not currently calibrating
				this.csi_Spectrum_Analyzer.isCalibrating = true; // set one point calibration mode to true
				this.csi_Spectrum_Analyzer.twoptcalib = false; // and add calibration GUI elements
				this.csi_Spectrum_Analyzer.addCalibrateSliders(this.csi_Spectrum_Analyzer.panSliders, this.csi_Spectrum_Analyzer.panButtons);
				this.csi_Spectrum_Analyzer.state.pwin.pack();
			} else {
				if (this.csi_Spectrum_Analyzer.twoptcalib) { // If currently calibrating, but not
					this.csi_Spectrum_Analyzer.removeCalibrateSliders(this.csi_Spectrum_Analyzer.panSliders, this.csi_Spectrum_Analyzer.panButtons); // in
					// one point mode, then remove
					this.csi_Spectrum_Analyzer.twoptcalib = false; // current calibration GUI elements
					this.csi_Spectrum_Analyzer.addCalibrateSliders(this.csi_Spectrum_Analyzer.panSliders, this.csi_Spectrum_Analyzer.panButtons); // and
					// replace with one point GUI elements
					this.csi_Spectrum_Analyzer.state.pwin.pack();
				}
			}
			this.csi_Spectrum_Analyzer.state.updateProfile();
		} else if (b == this.csi_Spectrum_Analyzer.miChangeColorCSI) {
			try {
				FileWriter fw = new FileWriter(IJ.getDirectory("plugins") + "CSI_config.txt");
				fw.write((char) 49);
				fw.close();
			} catch (IOException ex) {
			}
			this.csi_Spectrum_Analyzer.colZeroLine = Color.red;
			this.csi_Spectrum_Analyzer.colIntWindow = Color.white;
			this.csi_Spectrum_Analyzer.colSubtracted = Color.lightGray;
			this.csi_Spectrum_Analyzer.colData = Color.black;
			this.csi_Spectrum_Analyzer.colDataFill = Color.darkGray;
			this.csi_Spectrum_Analyzer.colBackFill = new Color(160, 165, 160);
			this.csi_Spectrum_Analyzer.colBackgroundFit = new Color(0, 128, 0);
			this.csi_Spectrum_Analyzer.colBackgroundWindow = new Color(128, 255, 128);
			this.csi_Spectrum_Analyzer.state.updateProfile();
		} else if (b == this.csi_Spectrum_Analyzer.miChangeColorCornell) {
			try {
				FileWriter fw = new FileWriter(IJ.getDirectory("plugins") + "CSI_config.txt");
				fw.write((char) 50);
				fw.close();
			} catch (IOException ex) {
			}
			this.csi_Spectrum_Analyzer.colZeroLine = Color.red;
			this.csi_Spectrum_Analyzer.colIntWindow = Color.darkGray;
			this.csi_Spectrum_Analyzer.colSubtracted = Color.lightGray;
			this.csi_Spectrum_Analyzer.colData = Color.black;
			this.csi_Spectrum_Analyzer.colDataFill = new Color(179, 27, 27);
			this.csi_Spectrum_Analyzer.colBackFill = Color.white;
			this.csi_Spectrum_Analyzer.colBackgroundFit = Color.lightGray;
			this.csi_Spectrum_Analyzer.colBackgroundWindow = Color.gray;
			this.csi_Spectrum_Analyzer.state.updateProfile();
		} else if (b == this.csi_Spectrum_Analyzer.miChangeColorCollegiate) {
			try {
				FileWriter fw = new FileWriter(IJ.getDirectory("plugins") + "CSI_config.txt");
				fw.write((char) 51);
				fw.close();
			} catch (IOException ex) {
			}
			this.csi_Spectrum_Analyzer.colZeroLine = new Color(128, 0, 0);
			this.csi_Spectrum_Analyzer.colIntWindow = new Color(128, 128, 158);
			this.csi_Spectrum_Analyzer.colSubtracted = new Color(192, 192, 222);
			this.csi_Spectrum_Analyzer.colData = Color.black;
			this.csi_Spectrum_Analyzer.colDataFill = new Color(15, 77, 146);
			this.csi_Spectrum_Analyzer.colBackFill = new Color(255, 255, 244);
			this.csi_Spectrum_Analyzer.colBackgroundFit = new Color(255, 215, 0);
			this.csi_Spectrum_Analyzer.colBackgroundWindow = new Color(245, 205, 0);
			this.csi_Spectrum_Analyzer.state.updateProfile();
		} else if (b == this.csi_Spectrum_Analyzer.miChangeColorCorporate) {
			try {
				FileWriter fw = new FileWriter(IJ.getDirectory("plugins") + "CSI_config.txt");
				fw.write((char) 52);
				fw.close();
			} catch (IOException ex) {
			}
			this.csi_Spectrum_Analyzer.colZeroLine = new Color(128, 0, 0);
			this.csi_Spectrum_Analyzer.colIntWindow = new Color(128, 128, 158);
			this.csi_Spectrum_Analyzer.colSubtracted = new Color(192, 192, 222);
			this.csi_Spectrum_Analyzer.colData = new Color(90, 190, 190);
			this.csi_Spectrum_Analyzer.colDataFill = new Color(90, 190, 190);
			this.csi_Spectrum_Analyzer.colBackFill = new Color(234, 234, 224);
			this.csi_Spectrum_Analyzer.colBackgroundFit = new Color(20, 150, 210);
			this.csi_Spectrum_Analyzer.colBackgroundWindow = new Color(20, 150, 210);
			this.csi_Spectrum_Analyzer.state.updateProfile();
		} else if (b == this.csi_Spectrum_Analyzer.butIntegrate) { // If integrate button was clicked
			if (this.csi_Spectrum_Analyzer.comFit.getSelectedItem().equals("No Fit")) {
				this.csi_Spectrum_Analyzer.state.integrate(this.csi_Spectrum_Analyzer.state.X0, this.csi_Spectrum_Analyzer.state.X1, this.csi_Spectrum_Analyzer.state.iX0, this.csi_Spectrum_Analyzer.state.iX1).show();
			} else
				this.csi_Spectrum_Analyzer.state.fitToModel(this.csi_Spectrum_Analyzer.state.X0, this.csi_Spectrum_Analyzer.state.X1, this.csi_Spectrum_Analyzer.state.iX0, this.csi_Spectrum_Analyzer.state.iX1).show(); // integrate
																					// data
			System.gc();
		} else if (b == this.csi_Spectrum_Analyzer.butPCA) {
			if (this.csi_Spectrum_Analyzer.weightedPCA)
				this.csi_Spectrum_Analyzer.state.weightedPCA(this.csi_Spectrum_Analyzer.state.X0, this.csi_Spectrum_Analyzer.state.X1, this.csi_Spectrum_Analyzer.state.iX0, this.csi_Spectrum_Analyzer.state.iX1);
			else
				this.csi_Spectrum_Analyzer.state.PCA(this.csi_Spectrum_Analyzer.state.X0, this.csi_Spectrum_Analyzer.state.X1, this.csi_Spectrum_Analyzer.state.iX0, this.csi_Spectrum_Analyzer.state.iX1);
			System.gc();
		} else if (b == this.csi_Spectrum_Analyzer.butSubtract) { // If subtract button was clicked
			ImagePlus s = this.csi_Spectrum_Analyzer.state.subtract(this.csi_Spectrum_Analyzer.state.X0, this.csi_Spectrum_Analyzer.state.X1);
			s.show(); // subtract data
			System.gc();
		} else if (b == this.csi_Spectrum_Analyzer.butCalibrate) {// If calibrate button was clicked
			this.csi_Spectrum_Analyzer.state.recalibrate(); // calibrate data
		} else if (b == this.csi_Spectrum_Analyzer.butCancelCalibration) {// If cancel calibration
												// button was clicked
			this.csi_Spectrum_Analyzer.isCalibrating = false; // Turn calibrating state to off
			this.csi_Spectrum_Analyzer.removeCalibrateSliders(this.csi_Spectrum_Analyzer.panSliders, this.csi_Spectrum_Analyzer.panButtons); // remove
															// calibration
															// GUI elements
			this.csi_Spectrum_Analyzer.state.pwin.pack();
			this.csi_Spectrum_Analyzer.state.updateProfile();
		} else if (b == this.csi_Spectrum_Analyzer.txtLeft) {
			Calibration cal = this.csi_Spectrum_Analyzer.img.getCalibration();
			try {
				this.csi_Spectrum_Analyzer.sldLeft.setValue((int) (Double.parseDouble(this.csi_Spectrum_Analyzer.txtLeft.getText()) / cal.pixelDepth + cal.zOrigin));
			} catch (NumberFormatException nfe) {
				this.csi_Spectrum_Analyzer.txtLeft.setText(String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.X0]));
			}
		} else if (b == this.csi_Spectrum_Analyzer.txtWidth) {
			Calibration cal = this.csi_Spectrum_Analyzer.img.getCalibration();
			try {
				this.csi_Spectrum_Analyzer.sldWidth.setValue((int) (Double.parseDouble(this.csi_Spectrum_Analyzer.txtWidth.getText()) / cal.pixelDepth));
			} catch (NumberFormatException nfe) {
				this.csi_Spectrum_Analyzer.txtWidth.setText(String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.X1] - this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.X0]));
			}
		} else if (b == this.csi_Spectrum_Analyzer.txtILeft) {
			Calibration cal = this.csi_Spectrum_Analyzer.img.getCalibration();
			try {
				this.csi_Spectrum_Analyzer.sldILeft.setValue((int) (Double.parseDouble(this.csi_Spectrum_Analyzer.txtILeft.getText()) / cal.pixelDepth + cal.zOrigin));
			} catch (NumberFormatException nfe) {
				this.csi_Spectrum_Analyzer.txtILeft.setText(String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.iX0]));
			}
		} else if (b == this.csi_Spectrum_Analyzer.txtIWidth) {
			Calibration cal = this.csi_Spectrum_Analyzer.img.getCalibration();
			try {
				this.csi_Spectrum_Analyzer.sldIWidth.setValue((int) (Double.parseDouble(this.csi_Spectrum_Analyzer.txtIWidth.getText()) / cal.pixelDepth));
			} catch (NumberFormatException nfe) {
				this.csi_Spectrum_Analyzer.txtIWidth.setText(String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.iX1] - this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.iX0]));
			}
		} else if (b == this.csi_Spectrum_Analyzer.radFast) {
			this.csi_Spectrum_Analyzer.txtOversampling.setText("0.0");
			this.csi_Spectrum_Analyzer.panOver.setVisible(false);
		} else if (b == this.csi_Spectrum_Analyzer.radOversampled) {
			this.csi_Spectrum_Analyzer.txtOversampling.setText("1.0");
			this.csi_Spectrum_Analyzer.panOver.setVisible(true);
			this.csi_Spectrum_Analyzer.state.pwin.pack();
		}
	}

	public void itemStateChanged(ItemEvent e) {
		Object b = e.getSource();
		if (b == this.csi_Spectrum_Analyzer.miScaleCounts) {
			this.csi_Spectrum_Analyzer.state.scaleCounts = this.csi_Spectrum_Analyzer.miScaleCounts.getState();
			this.csi_Spectrum_Analyzer.state.updateProfile();
		} else if (b == this.csi_Spectrum_Analyzer.miMeanCentering) {
			this.csi_Spectrum_Analyzer.meanCentering = this.csi_Spectrum_Analyzer.miMeanCentering.getState();
		} else if (b == this.csi_Spectrum_Analyzer.miWeightedPCA) {
			this.csi_Spectrum_Analyzer.weightedPCA = this.csi_Spectrum_Analyzer.miWeightedPCA.getState();
		} else if (b == this.csi_Spectrum_Analyzer.comFit) { // If combo box (drop-down menu) is
									// clicked
			String fitType = this.csi_Spectrum_Analyzer.comFit.getSelectedItem().toString();
			if (fitType.equals("No Fit")) { // Set fit state
				this.csi_Spectrum_Analyzer.state.setFit(CSI_SpectrumData.NO_FIT); // to combo box selection
			} else if (fitType.equals("Constant")) {
				this.csi_Spectrum_Analyzer.state.setFit(CSI_SpectrumData.CONSTANT_FIT);
			} else if (fitType.equals("Linear")) {
				this.csi_Spectrum_Analyzer.state.setFit(CSI_SpectrumData.LINEAR_FIT);
			} else if (fitType.equals("Exponential")) {
				this.csi_Spectrum_Analyzer.state.setFit(CSI_SpectrumData.EXPONENTIAL_FIT);
			} else if (fitType.equals("Power")) {
				this.csi_Spectrum_Analyzer.state.setFit(CSI_SpectrumData.POWER_FIT);
			} else if (fitType.equals("LCPL")) {
				this.csi_Spectrum_Analyzer.state.setFit(CSI_SpectrumData.LCPL_FIT);
			}
			this.csi_Spectrum_Analyzer.state.updateProfile();
		}
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		showPopup(e);
	}

	public void mouseReleased(MouseEvent e) {
		showPopup(e);
	}

	// Right click popup menu
	private void showPopup(MouseEvent e) {
		if (e.isPopupTrigger()) {
			this.csi_Spectrum_Analyzer.pm.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	public void mouseEntered(MouseEvent e) {
		Object b = e.getSource();
		if (b == this.csi_Spectrum_Analyzer.butIntegrate) { // If integrate button was clicked
			this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover1.setText("Sum the background subtracted data over the integration window.");
			this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover2.setText("(Select \"No Fit\" to integrate raw spectra.)");
		} else if (b == this.csi_Spectrum_Analyzer.butPCA) {
			this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover1.setText("Perform Principal Component Analysis on backgound subtracted dataset");
			this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover2.setText("over integration window (large window will take a long time).");
		} else if (b == this.csi_Spectrum_Analyzer.butSubtract) { // If subtract button was clicked
			this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover1.setText("Subtract extrapolated background from entire dataset.");
			this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover2.setText("");
		} else if (b == this.csi_Spectrum_Analyzer.butCalibrate) {// If calibrate button was clicked
			this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover1.setText("Perform calibration.");
			this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover2.setText("");
		} else if (b == this.csi_Spectrum_Analyzer.butCancelCalibration) {// If cancel calibration
												// button was clicked
			this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover1.setText("Do not perform calibration.");
			this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover2.setText("");
		} else if (b == this.csi_Spectrum_Analyzer.txtLeft) {
			this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover1.setText("Enter background window start energy directly.");
			this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover2.setText("");
		} else if (b == this.csi_Spectrum_Analyzer.txtWidth) {
			this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover1.setText("Enter background window energy width directly.");
			this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover2.setText("");
		} else if (b == this.csi_Spectrum_Analyzer.txtILeft) {
			this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover1.setText("Enter integration window start energy directly.");
			this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover2.setText("");
		} else if (b == this.csi_Spectrum_Analyzer.txtIWidth) {
			this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover1.setText("Enter integration window energy width directly.");
			this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover2.setText("");
		} else if (b == this.csi_Spectrum_Analyzer.radFast) {
			this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover1.setText("Perform standard background fits.");
			this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover2.setText("");
		} else if (b == this.csi_Spectrum_Analyzer.radOversampled) {
			this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover1.setText("Perform locally averaged background fits.");
			this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.red);
			this.csi_Spectrum_Analyzer.labHover2.setText("(Warning: Only use if probe size larger than pixel sampling.)");
		} else if (b == this.csi_Spectrum_Analyzer.sldLeft) {
			this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover1.setText("Position background window start energy.");
			this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover2.setText("(Click slider button and use Left/Right arrows for fine positioning.)\n");
		} else if (b == this.csi_Spectrum_Analyzer.sldWidth) {
			this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover1.setText("Change background window energy width.");
			this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover2.setText("(Click slider button and use Left/Right arrows for fine positioning.)\n");
		} else if (b == this.csi_Spectrum_Analyzer.sldILeft) {
			this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover1.setText("Position integration window start energy.");
			this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover2.setText("(Click slider button and use Left/Right arrows for fine positioning.)\n");
		} else if (b == this.csi_Spectrum_Analyzer.sldIWidth) {
			this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover1.setText("Change integration window energy width.");
			this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover2.setText("(Click slider button and use Left/Right arrows for fine positioning.)\n");
		} else if (b == this.csi_Spectrum_Analyzer.sldZoom) {
			this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover1.setText("Change zoom level.");
			this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover2.setText("(Click slider button and use Left/Right arrows for fine positioning.)\n");
		} else if (b == this.csi_Spectrum_Analyzer.sldOffset) {
			this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover1.setText("Move viewing window.");
			this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover2.setText("(Click slider button and use Left/Right arrows for fine positioning.)\n");
		} else if (b == this.csi_Spectrum_Analyzer.comFit) {
			this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover1.setText("Select funtional form for background fit.");
			this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.black);
			this.csi_Spectrum_Analyzer.labHover2.setText("(LCPL = Linear Combination of 2 Power Laws.)");
		}
	}

	public void mouseExited(MouseEvent e) {
		this.csi_Spectrum_Analyzer.labHover1.setForeground(Color.black);
		this.csi_Spectrum_Analyzer.labHover1.setText("(Right-click for options/help)");
		this.csi_Spectrum_Analyzer.labHover2.setForeground(Color.black);
		this.csi_Spectrum_Analyzer.labHover2.setText("Copyright Cornell University 2015: \nCite doi:10.1017/S1431927612000244");
	}
}