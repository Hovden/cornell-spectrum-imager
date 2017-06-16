package plugins.CSI;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;

import org.ujmp.core.DenseMatrix;
import org.ujmp.core.Matrix;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import ij.util.Tools;

import plugins.CSI.ResizeListener;

/*
 * Generic class for dealing with all of the data in the spectrum image.
 */
public abstract class SpectrumData implements MouseListener, MouseMotionListener, Measurements, KeyListener {

	/**
	 * 
	 */
	protected final CSI_Spectrum_Analyzer csi_Spectrum_Analyzer;
	boolean listenersRemoved, scaleCounts;

	double[] x, y, yfit, ysubtracted;
	int size, X0, X1, iX0, iX1, cX0, cX1, plotHeight, plotWidth, marginHeight, marginWidth;
	double zoomfactor, windowOffset; // Plot properties, zoom and offset
	String xLabel, yLabel; // Axis labels
	ImagePlus img;
	Fit fit;
	PlotWindow pwin;

	/**
	 * @param csi_Spectrum_Analyzer
	 */
	SpectrumData(CSI_Spectrum_Analyzer csi_Spectrum_Analyzer, ImagePlus img)
	{
		this.csi_Spectrum_Analyzer = csi_Spectrum_Analyzer;
		this.img = img;
		size = getSize(); // number of slices in stack
		cX1 = size - 1;
		y = getProfile();
		zoomfactor = 1; // Default zoom factor 1x zoom.
		windowOffset = 0; // Default, no window offset
		setFit(NO_FIT); // Default 'No Fit'
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

	static final int NO_FIT = 0;
	static final int CONSTANT_FIT = 1;
	static final int LINEAR_FIT = 2;
	static final int EXPONENTIAL_FIT = 3;
	static final int POWER_FIT = 4;
	static final int LCPL_FIT = 5;

	abstract ImagePlus integrate(int fitStart, int fitEnd, int intStart, int intEnd);

	abstract ImagePlus HCMintegrate(int fitStart, int fitEnd, int intStart, int intEnd);

	abstract void PCA(int fitStart, int fitEnd, int intStart, int intEnd);

	abstract void weightedPCA(int fitStart, int fitEnd, int intStart, int intEnd);

	abstract ImagePlus subtract(int fitStart, int fitEnd);

	abstract ImagePlus fitToModel(int fitStart, int fitEnd, int intStart, int intEnd);

	abstract ImagePlus fitToBosman(int fitStart, int fitEnd, int intStart, int intEnd);

	abstract double[] getProfile();

	abstract int getSize();

	void updateProfile() {

		checkPlotWindow();
		if (listenersRemoved || y == null || y.length == 0) {
			return;
		}

		Plot plot = new Plot("CSI: Cornell Spectrum Imager - " + img.getTitle(), xLabel, yLabel, x, y);
		plot.setColor(this.csi_Spectrum_Analyzer.colData);

		double[] a = Tools.getMinMax(x);
		double xmin = a[0] + (a[1] - a[0]) * windowOffset * (zoomfactor - 1) / (size * zoomfactor);
		double xmax = xmin + (a[1] - a[0]) / zoomfactor;
		double[] yrange = new double[(int) Math.ceil(size / zoomfactor)];
		System.arraycopy(y, (int) (windowOffset * (zoomfactor - 1) / zoomfactor), yrange, 0,
				(int) Math.ceil(size / zoomfactor));
		double[] ysubrange = new double[(int) Math.ceil(size / zoomfactor)];

		/* 
		 * Previous versions of this code relied in Jama.Matrix having a constructor that allowed
		 * for the user to pass in an array that actually represented a matrix in column-major order
		 * and the number of rows that were in that "packed" matrix.
		 * However, UJMP dropped this "feature" (for good reason).
		 *
		 * To remedy this situation, we create an array that represents the same "packed" matrix as
		 * y, but in our case it is in row-major order (SINCE THAT'S HOW JAVA WORKS!!!). We then use
		 * that array to fill in our matrix.
		 * 
		 * In our case, since y is actually a matrix in column-major order of number of rows "size":
		 * y.length = size * X, where X is the number of columns.
		 * Therefore, the number of columns is y.length / size
		 * 
		 *  - Pedro R (Imxset21), Dec 22 2015
		 *  
		 *  
		 *  Fixed issue with incorrect numbering that caused spectrum analyzer to throw exception.
		 *  Needed to revise function to unpack column organized matrix. 
		 *  
		 *  - Danielle L, Jan 19 2016
		 *  
		 */
		final int y_num_cols = y.length / size;
		Matrix tmp_mat = DenseMatrix.Factory.zeros(size, y_num_cols);
	    for (int i = 0; i < size; i++) 
	    { 
	    	for (int j = 0; j < y_num_cols; j++) 
	    	{
	    		tmp_mat.setAsDouble(y[(j * size) + i], i, j);
	    	}
	    }
		
		
		Matrix coeffs = fit.createFit(
				x, 
				tmp_mat, // new Jama.Matrix(y, size),
				X0,
				X1);

		yfit = new double[size];
		ysubtracted = new double[size];
		double c0, c1;
		c0 = coeffs.getAsDouble(0, 0);
		c1 = coeffs.getAsDouble(1, 0);
		for (int j = 0; j < size; j++) {
			yfit[j] = fit.getFitAtX(c0, c1, x[j]);
			if (Math.abs(yfit[j]) > Math.abs(10 * y[j]))
				ysubtracted[j] = 0;
			else if ((j < X0) || yfit[j] == 0)
				ysubtracted[j] = 0;
			else
				ysubtracted[j] = y[j] - yfit[j];
		}

		System.arraycopy(ysubtracted, (int) (windowOffset * (zoomfactor - 1) / zoomfactor), ysubrange, 0,
				(int) Math.ceil(size / zoomfactor));

		double scale;
		if ((Tools.getMinMax(ysubrange)[1] - Tools.getMinMax(ysubrange)[0]) == 0)
			scale = 1;
		else
			scale = (Tools.getMinMax(yrange)[1] - Tools.getMinMax(yrange)[0])
					/ (Tools.getMinMax(ysubrange)[1] - Tools.getMinMax(ysubrange)[0]);
		double ysubmin = Tools.getMinMax(ysubrange)[0];
		double ysubmax = Tools.getMinMax(ysubrange)[1];
		double yrangemin = Tools.getMinMax(yrange)[0];
		double shift = yrangemin - scale * ysubmin;
		if (scaleCounts) {
			for (int j = 0; j < size; j++) {
				double ysj = ysubtracted[j];
				ysj -= ysubmin;
				ysj *= scale;
				ysj += yrangemin;
				ysubtracted[j] = ysj;
				ysj -= ysubmin;
				ysj *= scale;
				ysj += yrangemin;
			}
		}

		System.arraycopy(ysubtracted, (int) (windowOffset * (zoomfactor - 1) / zoomfactor), ysubrange, 0,
				(int) Math.ceil(size / zoomfactor));

		double ymax;
		double ymin;
		if (scaleCounts) {
			ymin = Math.min(Tools.getMinMax(yrange)[0], yrange[0]);
			ymax = Math.max(Tools.getMinMax(yrange)[1], yrange[0]);
		} else {
			ymin = Math.min(Math.min(0, Tools.getMinMax(ysubrange)[0]), Tools.getMinMax(yrange)[0]);
			ymax = Math.max(Tools.getMinMax(yrange)[1], Tools.getMinMax(ysubrange)[1]);
		}

		plot.setLimits(xmin, xmax, Math.floor(ymin - 3 * (ymax - ymin) / (plotHeight)),
				Math.ceil(ymax + 3 * (ymax - ymin) / (plotHeight)));
		plot.setColor(Color.black);
		ImageProcessor ipplot = plot.getProcessor();
		if (pwin == null) {
			pwin = plot.show();
			PlotWindow.noGridLines = true;
			pwin.addComponentListener(new ResizeListener(this));
		}
		pwin.drawPlot(plot);

		plot.setBackgroundColor(this.csi_Spectrum_Analyzer.colBackFill);
		plot.updateImage();

		FloodFiller ff = new FloodFiller(ipplot);
		ipplot.setColor(this.csi_Spectrum_Analyzer.colDataFill);
		ff.fill(Plot.LEFT_MARGIN + (plotWidth) / 2, Plot.TOP_MARGIN + 1);
		ff.fill(Plot.LEFT_MARGIN + (plotWidth) / 2, plotHeight - 1);
		double[] zero = new double[size];
		Arrays.fill(zero, 0.0);
		plot.setColor(Color.black);
		plot.addPoints(x, zero, Plot.LINE);
		ipplot.setColor(this.csi_Spectrum_Analyzer.colBackFill);
		ff.fill(Plot.LEFT_MARGIN + (plotWidth) / 2, Plot.TOP_MARGIN + 1);
		if (ymin < 0)
			ff.fill(Plot.LEFT_MARGIN + (plotWidth) / 2, plotHeight + Plot.TOP_MARGIN - 1);

		if (scaleCounts) {
			Arrays.fill(zero, shift);
			plot.setColor(this.csi_Spectrum_Analyzer.colZeroLine);
			plot.addPoints(x, zero, Plot.LINE);
			ipplot.setColor(this.csi_Spectrum_Analyzer.colZeroLine);
			ipplot.drawString(String.format("%.1f", ysubmin), 0, plotHeight - 16);
			ipplot.drawString(String.format("%.1f", ysubmax), 0, Plot.TOP_MARGIN + 24);
		}

		plot.setColor(this.csi_Spectrum_Analyzer.colBackgroundFit);
		plot.addPoints(x, yfit, Plot.LINE);
		plot.setColor(this.csi_Spectrum_Analyzer.colSubtracted);
		plot.addPoints(x, ysubtracted, Plot.LINE);

		drawWindow(X0, X1, this.csi_Spectrum_Analyzer.colBackgroundWindow, plot);
		drawWindow(iX0, iX1, this.csi_Spectrum_Analyzer.colIntWindow, plot);

		if (this.csi_Spectrum_Analyzer.isCalibrating) {
			drawWindow(cX0, cX1, Color.black, plot);
		}
		marginWidth = pwin.getSize().width - PlotWindow.plotWidth;
		marginHeight = pwin.getSize().height - PlotWindow.plotHeight;
	}

	void drawWindow(int xI, int xF, Color c, Plot plot) {
		ImageProcessor ipplot = plot.getProcessor();
		final Rectangle frame =  plot.getDrawingFrame();
		ipplot.setColor(c);
		double xIdraw = xI * zoomfactor/(size-1) - windowOffset * (zoomfactor - 1)/size;
		double xFdraw = xF * zoomfactor/(size-1) - windowOffset * (zoomfactor - 1)/size;
		if ((xIdraw > 0) && (xIdraw < size)) {
			ipplot.drawRect(Plot.LEFT_MARGIN + (int)(plotWidth * xIdraw) , Plot.TOP_MARGIN, 1, (plotHeight));
		}
		if ((xFdraw > 0) && (xFdraw < size)) {
			ipplot.drawRect(Plot.LEFT_MARGIN + (int)(plotWidth * xFdraw) , Plot.TOP_MARGIN, 1, (plotHeight));
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
        IJ.wait(200); //This helps reduce glitches
        
		if (pwin == null || img == null) {
			return;
		}
		ImageWindow iwin = img.getWindow();
		if (iwin == null) {
			return;
		}
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		pwin.setSize(screen.width / 3, screen.height / 4); // SET the SIZE
															// of CSI WINDOW
		// SETTING SIZE HERE, IS NOT ALWAYS CONSISTENT, THERE IS A BUG - RMH

		Dimension plotSize = pwin.getSize();
		Dimension imageSize = iwin.getSize();

		if (plotSize.width == 0 || imageSize.width == 0) {
			return;
		}
        
		Point imageLoc = iwin.getLocation();
        int x = imageLoc.x + imageSize.width+10; //place window just right of spec
        if (x + plotSize.width > screen.width)
            x = screen.width - plotSize.width;
        int y = imageLoc.y; //place vertically aligned with spec
        if (y + plotSize.height > screen.height)
            y = screen.height - plotSize.height;
        pwin.setLocation(x, y); // position window next to spectrum
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
	 * Stop listening for mouse and key events if the plot window has been
	 * closed.
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

	public void keyPressed(KeyEvent e) {
	}

	public void keyTyped(KeyEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
		y = getProfile();
		updateProfile();
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {
	}

	private void recalibrateImage() {
	}

	void recalibrate() {
		double e0, e1, b, m;
		if (this.csi_Spectrum_Analyzer.twoptcalib) {
			try {
				e0 = Double.parseDouble(this.csi_Spectrum_Analyzer.txtLeftCalibration.getText());
			} catch (Exception e) {
				IJ.error("Error", "Please enter a valid Energy 1.");
				return;
			}
			try {
				e1 = Double.parseDouble(this.csi_Spectrum_Analyzer.txtRightCalibration.getText());
			} catch (Exception e) {
				IJ.error("Error", "Please enter a valid Energy 2.");
				return;
			}
		} else {
			try {
				e0 = Double.parseDouble(this.csi_Spectrum_Analyzer.txtLeftCalibration.getText());
			} catch (Exception e) {
				IJ.error("Error", "Please enter a valid Energy 1.");
				return;
			}
			try {
				e1 = e0 + Double.parseDouble(this.csi_Spectrum_Analyzer.txtRightCalibration.getText());
				cX1 = cX0 + 1;
			} catch (Exception e) {
				IJ.error("Error", "Please enter a valid Energy Per Channel.");
				return;
			}
		}
		if (cX0 >= cX1) {
			IJ.error("Error", "Can't calibrate: window is too small.");
			return;
		} else if (e0 >= e1) {
			IJ.error("Error", "Can't calibrate: energy range is nonpositive.");
			return;
		}
		xLabel = this.csi_Spectrum_Analyzer.txtEnergyCalibration.getText();

		m = (e1 - e0) / (cX1 - cX0);
		b = e0 - m * cX0;
		for (int i = 0; i < x.length; i++) {
			x[i] = m * i + b;
		}
		this.csi_Spectrum_Analyzer.isCalibrating = false;
		Calibration cal = img.getCalibration();
		cal.pixelDepth = m;
		cal.zOrigin = -b / (m);
		cal.setZUnit(xLabel);
		this.csi_Spectrum_Analyzer.labEnergy1.setText("(" + xLabel + ")");
		this.csi_Spectrum_Analyzer.labEnergy2.setText("(" + xLabel + ")");
		this.csi_Spectrum_Analyzer.labEnergy3.setText("(" + xLabel + ")");
		this.csi_Spectrum_Analyzer.labEnergy4.setText("(" + xLabel + ")");
		this.csi_Spectrum_Analyzer.txtLeft.setText(String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.X0]));
		this.csi_Spectrum_Analyzer.txtWidth.setText(String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.X1] - this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.X0]));
		this.csi_Spectrum_Analyzer.txtILeft.setText(String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.iX0]));
		this.csi_Spectrum_Analyzer.txtIWidth.setText(String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.iX1] - this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.iX0]));
		recalibrateImage();

		updateProfile();
		this.csi_Spectrum_Analyzer.removeCalibrateSliders(this.csi_Spectrum_Analyzer.panSliders, this.csi_Spectrum_Analyzer.panButtons);
		pwin.pack();
	}

	void updateProgress(double progress) {
		if (progress == 1)
			pwin.setTitle("CSI: Cornell Spectrum Imager - " + img.getTitle());
		else
			pwin.setTitle("(Working: %" + String.format("%.0f", progress * 100)
					+ ") CSI: Cornell Spectrum Imager - " + img.getTitle());
	}
}