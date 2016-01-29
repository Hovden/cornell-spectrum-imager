package plugins.CSI;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.TextField;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.metal.MetalLookAndFeel;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

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

	ImagePlus img; // Image data
	SpectrumData state; // Image data class

	// CSI_Spectrum_Analyzer state variables
	boolean twoptcalib, isCalibrating, meanCentering = false, weightedPCA = false;

	// GUI Elements
	JButton butIntegrate, butHCMIntegrate, butPCA, butSubtract, butCancelCalibration, butCalibrate;
	ButtonGroup bgFit;
	JRadioButton radFast, radOversampled;
	JComboBox<String> comFit;
	JSlider sldZoom, sldOffset, sldLeft, sldWidth, sldILeft, sldIWidth, sldCLeft, sldCRight;
	Panel panButtons, panCalibrateButtons, panSliders, panCalibrateL, panCalibrateR, panOver;
	Label labIntegrate, labSubtract, labCalibrate, labEnergy1, labEnergy2, labEnergy3, labEnergy4, labHover1, labHover2;
	TextField txtLeftCalibration, txtRightCalibration, txtEnergyCalibration, txtLeft, txtWidth, txtILeft, txtIWidth,
	txtOversampling;
	JMenuItem miTwoPointCalibration, miOnePointCalibration, miAbout, miDoc, miChangeColorCSI, miChangeColorCornell,
	miChangeColorCollegiate, miChangeColorCorporate;
	JPopupMenu pm;
	JCheckBoxMenuItem miScaleCounts, miMeanCentering, miWeightedPCA;
	JPanel panRad = new JPanel(), panAll = new JPanel();
	Color colZeroLine, colIntWindow, colSubtracted, colData, colDataFill, colBackFill, colBackgroundFit,
	colBackgroundWindow;

	/*
	 * Load image data and start Cornell Spectrum Imager
	 */
	@Override
	public int setup(String arg, ImagePlus img) {
		Random rand = new Random();
		if (rand.nextDouble() < .04) {
			IJ.showMessage("Reminder!",
					"If you use CSI to produce published research, cite doi:10.1017/S1431927612000244.");
		}
		try {
			UIManager.setLookAndFeel(new MetalLookAndFeel()); // UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException e) {
			// may be unsupported in future
		}

		FileReader fr = null;
		int c = 0;
		try {
			fr = new FileReader(IJ.getDirectory("plugins") + "CSIconfig.txt");
			c = fr.read();
			fr.close();
		} catch (IOException e) {
			colZeroLine = Color.red;
			colIntWindow = Color.white;
			colSubtracted = Color.lightGray;
			colData = Color.black;
			colDataFill = Color.darkGray;
			colBackFill = new Color(160, 165, 160);
			colBackgroundFit = new Color(0, 128, 0);
			colBackgroundWindow = new Color(128, 255, 128);
		}

		switch (c) {

		case 50:
			colZeroLine = Color.red;
			colIntWindow = Color.darkGray;
			colSubtracted = Color.lightGray;
			colData = Color.black;
			colDataFill = new Color(179, 27, 27);
			colBackFill = Color.white;
			colBackgroundFit = Color.lightGray;
			colBackgroundWindow = Color.gray;
			break;
		case 51:
			colZeroLine = new Color(128, 0, 0);
			colIntWindow = new Color(128, 128, 158);
			colSubtracted = new Color(192, 192, 222);
			colData = Color.black;
			colDataFill = new Color(15, 77, 146);
			colBackFill = new Color(255, 255, 244);
			colBackgroundFit = new Color(255, 215, 0);
			colBackgroundWindow = new Color(245, 205, 0);
			break;
		case 52:
			colZeroLine = new Color(128, 0, 0);
			colIntWindow = new Color(128, 128, 158);
			colSubtracted = new Color(192, 192, 222);
			colData = new Color(90, 190, 190);
			colDataFill = new Color(90, 190, 190);
			colBackFill = new Color(234, 234, 224);
			colBackgroundFit = new Color(20, 150, 210);
			colBackgroundWindow = new Color(20, 150, 210);
			break;
		default:
			colZeroLine = Color.red;
			colIntWindow = Color.white;
			colSubtracted = Color.lightGray;
			colData = Color.black;
			colDataFill = Color.darkGray;
			colBackFill = new Color(160, 165, 160);
			colBackgroundFit = new Color(0, 128, 0);
			colBackgroundWindow = new Color(128, 255, 128);
			break;
		}

		if (IJ.versionLessThan("1.46")) { // Check ImageJ version
			IJ.error("Error starting CSI: Cornell Spectrum Imager", "ImageJ version is too old.");
			return DONE; // Close plugin
		}

		// Check that image is present and a region is selected
		this.img = img;
		if (img != null && img.getRoi() == null) {
			if (img.getStackSize() == 1) {
				img.setRoi(new Rectangle(0, img.getHeight() / 2, img.getWidth(), 1));
			} else {
				img.setRoi(new Rectangle(0, 0, 10, 10));
			}
		}

		return DOES_ALL + NO_CHANGES;
	}

	/*
	 * Initialize variables and create windows.
	 */
	@Override
	public void run(ImageProcessor ip) {
		if (img.getStackSize() < 2) {
			if (img.getHeight() < 2) {
				state = new SpectrumData0D(this, img); // Spectrum data is
				// single spectrum
			} else {
				state = new SpectrumData1D(this, img); // Spectrum data is a
				// linescan
			}
		} else {
			state = new SpectrumData2D(this, img); // Spectrum data is a 2D map
		}
		state.pwin.getCanvas().disablePopupMenu(true);
		addInitialButtons(state.pwin);
		setupMenu(); // Creates the popup menu to be shown on right-clicks
		state.pwin.getCanvas().addMouseListener(new TestListener(this));
		state.pwin.addMouseListener(new TestListener(this));
		state.updateProfile();
		state.pwin.pack();
	}

	/*
	 * Adds all of the pertinent buttons to the gui.
	 */
	private void addInitialButtons(Frame frame) {
		Panel zooming = new Panel();
		zooming.setLayout(new GridLayout(2, 2));
		zooming.add(new Label("Energy Window Zoom"));
		zooming.add(new Label("Energy Window Offset"));
		zooming.addMouseListener(new TestListener(this));
		sldZoom = new JSlider(0, 50, 0);
		sldZoom.addChangeListener(new ScrollListener(this));
		sldZoom.addMouseListener(new TestListener(this));
		zooming.add(sldZoom);
		sldOffset = new JSlider(0, state.size, 0);
		sldOffset.addChangeListener(new ScrollListener(this));
		sldOffset.addMouseListener(new TestListener(this));
		zooming.add(sldOffset);

		panButtons = new Panel(new GridBagLayout());
		panButtons.addMouseListener(new TestListener(this));
		String[] fits = { "No Fit", "Constant", "Exponential", "Linear", "Power", "LCPL" };
		comFit = new JComboBox<String>();
		for (int i = 0; i < fits.length; i++)
			comFit.addItem(fits[i]);
		state.setFit(SpectrumData.NO_FIT);
		comFit.addItemListener(new TestListener(this));
		comFit.addMouseListener(new TestListener(this));
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		panButtons.add(comFit, c);

		bgFit = new ButtonGroup();
		radFast = new JRadioButton("Fast", true);
		radFast.addActionListener(new TestListener(this));
		radFast.addMouseListener(new TestListener(this));
		bgFit.add(radFast);
		radOversampled = new JRadioButton("Oversampled", false);
		radOversampled.addActionListener(new TestListener(this));
		radOversampled.addMouseListener(new TestListener(this));
		bgFit.add(radOversampled);
		panRad.add(radFast);
		panRad.add(radOversampled);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		panButtons.add(panRad, c);

		panOver = new Panel();
		Label lbl = new Label("Probe FWHM (pixels)");
		panOver.add(lbl);
		txtOversampling = new TextField("0.0");
		panOver.add(txtOversampling);
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 1;
		panOver.setVisible(false);
		panButtons.add(panOver, c);

		butSubtract = new JButton("Background Subtract Dataset");
		butSubtract.addActionListener(new TestListener(this));
		butSubtract.addMouseListener(new TestListener(this));
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		panButtons.add(butSubtract, c);

		panSliders = new Panel();
		panSliders.setLayout(new GridBagLayout());
		panSliders.addMouseListener(new TestListener(this));

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
		sldLeft.addChangeListener(new ScrollListener(this));
		sldLeft.addMouseListener(new TestListener(this));
		state.X0 = 0;
		sldWidth = new JSlider(0, state.size, state.size / 20);
		sldWidth.addChangeListener(new ScrollListener(this));
		sldWidth.addMouseListener(new TestListener(this));
		state.X1 = state.X0 + state.size / 20;

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
		txtLeft.addActionListener(new TestListener(this));
		;
		txtLeft.addMouseListener(new TestListener(this));
		;
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 5;
		Panel pleft = new Panel();
		pleft.add(txtLeft);
		pleft.add(labEnergy1);
		panSliders.add(pleft, c);
		labEnergy2 = new Label("(" + state.xLabel + ")");
		txtWidth = new TextField(String.format("%.1f", state.x[state.X1] - state.x[state.X0]));
		txtWidth.addActionListener(new TestListener(this));
		;
		txtWidth.addMouseListener(new TestListener(this));
		;
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 5;
		Panel pwidth = new Panel();
		pwidth.add(txtWidth);
		pwidth.add(labEnergy2);
		panSliders.add(pwidth, c);

		Panel spacer = new Panel();
		spacer.setPreferredSize(new Dimension(3, 3));

		c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = 0;
		panButtons.add(spacer, c);

		c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = 1;
		panButtons.add(spacer, c);

		butIntegrate = new JButton("Integrate");
		butIntegrate.addActionListener(new TestListener(this));
		butIntegrate.addMouseListener(new TestListener(this));
		c = new GridBagConstraints();
		c.gridx = 4;
		c.gridy = 0;
		panButtons.add(butIntegrate, c);
		butHCMIntegrate = new JButton("HCM Integrate");
		butHCMIntegrate.addActionListener(new TestListener(this));

		butPCA = new JButton("PCA");
		butPCA.addActionListener(new TestListener(this));
		butPCA.addMouseListener(new TestListener(this));
		c = new GridBagConstraints();
		c.gridx = 4;
		c.gridy = 1;
		panButtons.add(butPCA, c);

		sldILeft = new JSlider(0, state.size, 0);
		sldILeft.addChangeListener(new ScrollListener(this));
		sldILeft.addMouseListener(new TestListener(this));
		state.iX0 = 0;
		sldIWidth = new JSlider(0, state.size, state.size / 20);
		sldIWidth.addChangeListener(new ScrollListener(this));
		sldIWidth.addMouseListener(new TestListener(this));
		state.iX1 = state.iX0 + state.size / 20;
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
		txtILeft.addActionListener(new TestListener(this));
		txtILeft.addMouseListener(new TestListener(this));
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 7;
		Panel pileft = new Panel();
		pileft.add(txtILeft);
		pileft.add(labEnergy3);
		panSliders.add(pileft, c);
		labEnergy4 = new Label("(" + state.xLabel + ")");
		txtIWidth = new TextField(String.format("%.1f", state.x[state.iX1] - state.x[state.iX0]));
		txtIWidth.addActionListener(new TestListener(this));
		txtIWidth.addMouseListener(new TestListener(this));
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
		// panSliders.add(new Label("(Right-click for options/help)"), c);
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 9;
		// panSliders.add(new Label("Copyright Cornell University 2010"), c);

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

		panAll.add(zooming);

		panAll.add(panButtons);

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
	void addCalibrateSliders(Container conSliders, Container conButtons) {
		GridBagConstraints c = new GridBagConstraints();

		if (twoptcalib) {
			sldCLeft = new JSlider(0, state.size - 1, state.cX0);
			sldCLeft.addChangeListener(new ScrollListener(this));
			sldCRight = new JSlider(0, state.size - 1, state.cX1);
			sldCRight.addChangeListener(new ScrollListener(this));
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
			state.cX1 = state.size - 1;
			sldCLeft = new JSlider(0, state.size - 1, state.cX0);
			sldCLeft.addChangeListener(new ScrollListener(this));
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
			txtRightCalibration = new TextField(String.format("%.1f", state.x[1] - state.x[0]));
			panCalibrateR.add(txtRightCalibration);
			panCalibrateR.add(new Label("(" + state.xLabel + "/ch)"));
			c = new GridBagConstraints();
			c.gridx = 2;
			c.gridy = 1;
			conSliders.add(panCalibrateR, c);
		}

		panCalibrateButtons = new Panel(new FlowLayout());

		butCalibrate = new JButton("Do Calibration");
		butCalibrate.addActionListener(new TestListener(this));
		panCalibrateButtons.add(butCalibrate);

		butCancelCalibration = new JButton("Cancel Calibration");
		butCancelCalibration.addActionListener(new TestListener(this));
		panCalibrateButtons.add(butCancelCalibration);
		c.gridx = 1;
		c.gridy = 0;
		conSliders.add(panCalibrateButtons, c);
	}

	/*
	 * Removes elements of the gui used for recalibration.
	 */
	void removeCalibrateSliders(Container conSliders, Container conButtons) {
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
		miScaleCounts.addItemListener(new TestListener(this));
		optionsMenu.add(miScaleCounts);

		miMeanCentering = new JCheckBoxMenuItem("Do mean centering for the PCA.", false);
		miMeanCentering.addItemListener(new TestListener(this));
		optionsMenu.add(miMeanCentering);

		miWeightedPCA = new JCheckBoxMenuItem("Weight the PCA.", false);
		miWeightedPCA.addItemListener(new TestListener(this));
		optionsMenu.add(miWeightedPCA);

		JMenu colorMenu = new JMenu("Change color scheme.");
		miChangeColorCSI = new JMenuItem("CSI Classic");
		miChangeColorCSI.addActionListener(new TestListener(this));
		colorMenu.add(miChangeColorCSI);
		miChangeColorCornell = new JMenuItem("Cornell");
		miChangeColorCornell.addActionListener(new TestListener(this));
		colorMenu.add(miChangeColorCornell);
		miChangeColorCollegiate = new JMenuItem("Collegiate");
		miChangeColorCollegiate.addActionListener(new TestListener(this));
		colorMenu.add(miChangeColorCollegiate);
		miChangeColorCorporate = new JMenuItem("Corporate");
		miChangeColorCorporate.addActionListener(new TestListener(this));
		colorMenu.add(miChangeColorCorporate);
		optionsMenu.add(colorMenu);

		JMenu calibrationMenu = new JMenu("Calibrate Energy-axis");
		miTwoPointCalibration = new JMenuItem("Recalibrate with two sample points.");
		miTwoPointCalibration.addActionListener(new TestListener(this));
		calibrationMenu.add(miTwoPointCalibration);
		miOnePointCalibration = new JMenuItem("Recalibrate with one sample point and the channel size.");
		miOnePointCalibration.addActionListener(new TestListener(this));
		calibrationMenu.add(miOnePointCalibration);
		optionsMenu.add(calibrationMenu);

		pm.add(optionsMenu);

		JMenu helpMenu = new JMenu("Help");
		miAbout = new JMenuItem("About CSI: Cornell Spectrum Imager");
		miAbout.addActionListener(new TestListener(this));
		helpMenu.add(miAbout);
		miDoc = new JMenuItem("Documentation");
		miDoc.addActionListener(new TestListener(this));
		helpMenu.add(miDoc);
		pm.add(helpMenu);
	}

	ImagePlus filter(int components, ImagePlus maps) {
		return null;
	}

}