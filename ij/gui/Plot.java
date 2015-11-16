package ij.gui;
import java.awt.*;
import java.util.*;
import ij.*;
import ij.process.*;
import ij.util.*;
import ij.plugin.filter.Analyzer;
import ij.macro.Interpreter;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;

/** This class creates an image that line graphs, scatter plots and plots of vector fields
 *	(arrows) can be drawn on and displayed.
 *
 *	Note that the clone() operation is a shallow clone: objects like arrays, the plot objects,
 *	the ImagePlus etc. of the clone remain the same as those of the original.
 *
 * @author Wayne Rasband
 * @author Philippe CARL, CNRS, philippe.carl (AT) unistra.fr (log axes, arrows, ArrayList data)
 * @author Michael Schmid (axis grid/ticks, resizing/panning/changing range, high-resolution)
 */
public class Plot implements Cloneable {

	/** Text justification. */
	public static final int LEFT=ImageProcessor.LEFT_JUSTIFY, CENTER=ImageProcessor.CENTER_JUSTIFY, RIGHT=ImageProcessor.RIGHT_JUSTIFY;
	/** Legend positions */
	//NOTE: These have only bits of LEGEND_POSITION_MASK set. The flags of the legend are stored as flags of the legend PlotObject.
	public static final int TOP_LEFT=0x90, TOP_RIGHT=0xA0, BOTTOM_LEFT=0xB0, BOTTOM_RIGHT=0xC0, AUTO_POSITION=0x80;
	/** Masks out bits for legend positions; if all these bits are off, the legend is turned off */
	private static final int LEGEND_POSITION_MASK = 0xf0;
	/** Legend has its curves in bottom-to-top sequence (otherwise top to bottom) */
	public static final int LEGEND_BOTTOM_UP = 0x100;
	/** Legend erases background (otherwise transparent) */
	public static final int LEGEND_TRANSPARENT = 0x200;
	/** Display points using a circle (5 pixels in diameter if line thickness<=1, otherwise 7). */
	public static final int CIRCLE = 0;
	/** Display points using an X-shaped mark. */
	public static final int X = 1;
	/** Display points using a square box-shaped mark. */
	public static final int BOX = 3;
	/** Display points using an tiangular mark. */
	public static final int TRIANGLE = 4;
	/** Display points using an cross-shaped mark. */
	public static final int CROSS = 5;
	/** Display points using a single pixel. */
	public static final int DOT = 6;
	/** Connect points with solid lines. */
	public static final int LINE = 2;
	/** Draw black lines between the dots and a circle with the given color at each dot */
	public static final int CONNECTED_CIRCLES = 7;	
	/** flag for numeric labels of x-axis ticks */
	public static final int X_NUMBERS = 0x1;
	/** flag for numeric labels of x-axis ticks */
	public static final int Y_NUMBERS = 0x2;
	/** flag for drawing major ticks on linear (non-logarithmic) x axis */
	public static final int X_TICKS = 0x4;
	/** flag for drawing major ticks on linear (non-logarithmic) y axis */
	public static final int Y_TICKS = 0x8;
	/** flag for drawing vertical grid lines for x axis */
	public static final int X_GRID = 0x10;
	/** flag for drawing horizontal grid lines for y axis */
	public static final int Y_GRID = 0x20;
	/** flag for forcing frame to coincide with the grid/ticks in x direction (results in unused space) */
	public static final int X_FORCE2GRID = 0x40;
	/** flag for forcing frame to coincide with the grid/ticks in y direction (results in unused space) */
	public static final int Y_FORCE2GRID = 0x80;
	/** flag for drawing minor ticks on linear (non-logarithmic) x axis */
	public static final int X_MINOR_TICKS = 0x100;
	/** flag for drawing minor ticks on linear (non-logarithmic) y axis */
	public static final int Y_MINOR_TICKS = 0x200;
	/** flag for logarithmic x-axis */
	public static final int X_LOG_NUMBERS = 0x400;
	/** flag for logarithmic y axis */
	public static final int Y_LOG_NUMBERS = 0x800;
	/** flag for ticks (major and minor, if space) on logarithmic x axis */
	public static final int X_LOG_TICKS = 0x1000;
	/** flag for ticks (major and minor, if space) on logarithmic y axis */
	public static final int Y_LOG_TICKS = 0x2000;
	//leave 0x4000, 0x8000 reserved for broken axes?
	/** The default flags, will be modified by PlotWindow.noGridLines and PlotWindow.noTicks (see getDefaultFlags) */
	public static final int DEFAULT_FLAGS =	 X_NUMBERS + Y_NUMBERS + /*X_TICKS + Y_TICKS +*/
			X_GRID + Y_GRID + X_LOG_TICKS + Y_LOG_TICKS;

	/** Flag for copying from a template: copy x axis range. */
	// This must be 0x1 because bit shift operations are used for the other axes
	public static final int X_RANGE = 0x1;
	/** Flag for copying from a template: copy y axis range */
	public static final int Y_RANGE = 0x2;
	//0x4, 0x8 reserved for secondary axes
	/** Flag for copying from a template: copy text of axis labels */
	public static final int COPY_SIZE = 0x10;
	/** Flag for copying from a template: copy text of axis labels */
	public static final int COPY_LABELS = 0x20;
	/** Flag for copying from a template: copy legend */
	public static final int COPY_LEGEND = 0x40;

	/** The default margin width left of the plot frame (enough for 5-digit numbers such as unscaled 16-bit
	 *	@deprecated Not a fixed value any more, use getDrawingFrame() to get the drawing area */
	public static final int LEFT_MARGIN = 60;
	/** The default margin width right of the plot frame
	 *	@deprecated Not a fixed value any more, use getDrawingFrame() to get the drawing area */
	public static final int RIGHT_MARGIN = 20;
	/** The default margin width above the plot frame
	 *	@deprecated Not a fixed value any more, use getDrawingFrame() to get the drawing area */
	public static final int TOP_MARGIN = 13;
	/** The default margin width below the plot frame
	 *	@deprecated Not a fixed value any more, use getDrawingFrame() to get the drawing area */
	public static final int BOTTOM_MARGIN = 37;
	/** minimum width of frame area in plot */
	public static final int MIN_FRAMEWIDTH = 160;
	/** minimum width of frame area in plot */
	public static final int MIN_FRAMEHEIGHT = 90;
	/** key in ImagePlus properties to access the plot behind an ImagePlus */
	public static final String PROPERTY_KEY = "thePlot";

	private static final int MIN_X_GRIDSPACING = 45;	//minimum distance between grid lines or ticks along x at plot width 0
	private static final int MIN_Y_GRIDSPACING = 30;	//minimum distance between grid lines or ticks along y at plot height 0
	private final double MIN_LOG_RATIO = 3;				//If max/min ratio is less than this, force linear axis even if log required. should be >2
	private static final int LEGEND_PADDING = 4;		//pixels around legend text etc
	private static final int LEGEND_LINELENGTH = 15;	//length of lines in legend
	private static final int USUALLY_ENLARGE = 1, ALWAYS_ENLARGE = 2; //enlargeRange settings
	private static final double RELATIVE_ARROWHEAD_SIZE = 0.2; //arrow heads have 1/5 of vector length
	private static final int MIN_ARROWHEAD_LENGTH = 3;
	private static final int MAX_ARROWHEAD_LENGTH = 20;

	static final int ZOOM_AS_PREVIOUS = -20202020;	//when zooming at this coordinate, it rather uses the previous zoom center
	Rectangle frame = null;							//the clip frame, do not use for image scale
	float scale = 1.0f;								//everything will be scaled with this number so the plot can be resized
	//The following the margin sizes actually used. They are modified for font size and also scaled for high-resolution plots
	int leftMargin = LEFT_MARGIN, rightMargin = RIGHT_MARGIN, topMargin = TOP_MARGIN, bottomMargin = BOTTOM_MARGIN;
	int frameWidth;							//width corresponding to plot range; frame.width is larger by 1
	int frameHeight;						//height corresponding to plot range; frame.height is larger by 1

	double xMin = Double.NaN, xMax, yMin, yMax;		//current plot range, logarithm if log axis
	double[] currentMinMax = new double[]{Double.NaN, 0, Double.NaN, 0}; //current plot range, xMin, xMax, yMin, yMax but never logarithmic
	double[] defaultMinMax = new double[]{Double.NaN, 0, Double.NaN, 0}; //default plot range
	double[] savedMinMax = new double[]{Double.NaN, 0, Double.NaN, 0};	//keeps previous range for revert
	int[] enlargeRange;								//whether to enlarge the range slightly to avoid values at the axes (0=off, USUALLY_ENLARGE, ALWAYS_ENLARGE)
	boolean logXAxis, logYAxis;						//whether to really use log axis (never for small relative range)

	Font defaultFont = new Font("Helvetica", Font.PLAIN, PlotWindow.fontSize); //default font for labels etc.
	Font currentFont;								//font as changed by setFont or setFontSize
	Font xLabelFont;								//font for text labels
	Font yLabelFont;
	String xLabel;									//axis label
	String yLabel;
	int templateFlags = COPY_SIZE | COPY_LABELS | COPY_LEGEND;	//for passing on what should be kept when 'live' plotting (PlotMaker)
	private double xScale, yScale;					//pixels per data unit
	private int xBasePxl, yBasePxl;					//pixel coordinates corresponding to 0
	private double previousXZoom = Double.NaN;
	private double previousYZoom = Double.NaN;
	private int maxIntervals = 12;					//maximum number of intervals between ticks or grid lines
	private int tickLength = 7;						//length of major ticks
	private int minorTickLength = 3;				//length of minor ticks
	private Color gridColor = new Color(0xc0c0c0);	//light gray
	private Color frameColor = Color.black;
	private int flags;								//these define axis layout
	private ImageProcessor ip;
	private ImagePlus imp;							//if we have an ImagePlus, updateAndDraw on changes
	private boolean frozen;							//modifications (size, range, contents) don't update the ImageProcessor
	private String title;
	private boolean invertedLut;					//grayscale plots only, set in Edit>Options>Appearance
	private boolean isColor;						//whether a color plot is needed
	private boolean plotDrawn;
	private boolean antialiasedText = true;
	int plotWidth = PlotWindow.plotWidth;
	int plotHeight = PlotWindow.plotHeight;
	PlotMaker plotMaker;
	Vector<PlotObject> allPlotObjects = new Vector<PlotObject>();	//all curves, labels etc.
	PlotObject legend;								//the legend (if any)
	private Color currentColor;						//for next objects added
	private Color currentColor2;					//2nd color for next object added (e.g. line for CONNECTED_CIRCLES)
	float currentLineWidth;
	float frameLineWidth;
	private int currentJustification = LEFT;
	private boolean ignoreForce2Grid;				// after explicit setting of range (limits), ignore 'FORCE2GRID' flags
	//private boolean snapToMinorGrid;				// snap to grid when zooming to selection
	
	/** Construct a new PlotWindow.
	 *	Note that the data xValues, yValues passed with the constructor are plotted last,
	 *	with the settings (color, lineWidth) at the time when 'draw' or 'getProcessor' is called.
	 *	These data are plotted as a LINE.
	 * @param title the window title
	 * @param xLabel	the x-axis label
	 * @param yLabel	the y-axis label
	 * @param xValues	the x-coodinates, or null. If null and yValues is not null, integers starting at 0 will be used for x.
	 * @param yValues	the y-coodinates, or null for providing no data yet.
	 */
	public Plot(String title, String xLabel, String yLabel, float[] xValues, float[] yValues) {
		this(title, xLabel, yLabel, xValues, yValues, getDefaultFlags());
	}

	/** This version of the constructor accepts double arrays. */
	public Plot(String title, String xLabel, String yLabel, double[] xValues, double[] yValues) {
		this(title, xLabel, yLabel, xValues!=null?Tools.toFloat(xValues):null, yValues!=null?Tools.toFloat(yValues):null, getDefaultFlags());
	}

	/** This is a constructor that works with JavaScript. */
	public Plot(String dummy, String title, String xLabel, String yLabel, float[] xValues, float[] yValues) {
		this(title, xLabel, yLabel, xValues, yValues, getDefaultFlags());
	}

	/** This is a version of the constructor with no intial arrays. */
	public Plot(String title, String xLabel, String yLabel) {
		this(title, xLabel, yLabel, (float[])null, (float[])null, getDefaultFlags());
	}

	/** This is a version of the constructor with no intial arrays. */
	public Plot(String title, String xLabel, String yLabel, int flags) {
		this(title, xLabel, yLabel, (float[])null, (float[])null, flags);
	}	

	/** This version of the constructor has a 'flags' argument for
		controlling the appearance of ticks, grid, etc. */
	public Plot(String title, String xLabel, String yLabel, float[] xValues, float[] yValues, int flags) {
		this.title = title;
		this.flags = flags;
		setXYLabels(xLabel, yLabel);
		if (yValues != null && yValues.length>0) {
			addPoints(xValues, yValues, /*yErrorBars=*/null, LINE, /*label=*/null);
			allPlotObjects.get(0).flags = PlotObject.CONSTRUCTOR_DATA;
		}
	}

	/** This version of the constructor accepts double arrays and has a 'flags' argument. */
	public Plot(String title, String xLabel, String yLabel, double[] xValues, double[] yValues, int flags) {
		this(title, xLabel, yLabel, xValues!=null?Tools.toFloat(xValues):null, yValues!=null?Tools.toFloat(yValues):null, flags);
	}

	/** Returns the title of the image showing the plot (if any) or title of the plot */
	public String getTitle() {
		return imp == null ? title : imp.getTitle();
	}

	/** Sets the x-axis and y-axis range. Updates the image if existing. */

	public void setLimits(double xMin, double xMax, double yMin, double yMax) {
		defaultMinMax[0] = xMin;
		defaultMinMax[1] = xMax;
		defaultMinMax[2] = yMin;
		defaultMinMax[3] = yMax;
		enlargeRange = null;
		ignoreForce2Grid = true;
		if (plotDrawn)
			setLimitsToDefaults(true);
	}

	/** Returns the current limits as an array xMin, xMax, yMin, yMax.
	 *	Note that future versions might return a longer array (e.g. for y2 axis limits) */
	public double[] getLimits() {
		return new double[] {xMin, xMax, yMin, yMax};
	}

	/** Sets the canvas size in unscaled pixels and sets the scale to 1.0.
	 * If the scale remains 1.0, this will be the size of the resulting ImageProcessor.
	 * When not called, the canvas size is adjusted for the plot frame size specified
	 * in Edit>Options>Profile Plot Options. */
	public void setSize(int width, int height) {
		//IJ.log("setSize "+width+"x"+height+ " old: "+ip);
		if (ip != null && width == ip.getWidth() && height == ip.getHeight()) return;
		Dimension minSize = getMinimumSize();
		plotWidth = Math.max(width, minSize.width) - (leftMargin+rightMargin);
		plotHeight = Math.max(height, minSize.height) - (topMargin+bottomMargin);
		scale = 1.0f;
		ip = null;
		if (plotDrawn) updateImage();
	}

	/** The size of the plot including borders with axis labels etc., in pixels */
	public Dimension getSize() {
		if (ip == null)
			getBlankProcessor();
		return new Dimension(ip.getWidth(), ip.getHeight());
	}

	/** Sets the plot frame size in (unscaled) pixels. This size does not include the
	 *	borders with the axis labels.
	 *	This frame size in pixels divided by the data range defines the image scale.
	 *	This method does not check for the minimum size MIN_FRAMEWIDTH, MIN_FRAMEHEIGHT.
	 *	Note that the black frame will have an outer size that is one pixel larger
	 *	(when plotted with a linewidth of one pixel). */
	public void setFrameSize(int width, int height) {
		plotWidth = width;
		plotHeight = height;
		ip = null;
		if (plotDrawn) updateImage();
	}

	/** The minimum plot size including borders, in pixels (at scale=1) */
	public Dimension getMinimumSize() {
		return new Dimension(MIN_FRAMEWIDTH + leftMargin + rightMargin,
				MIN_FRAMEHEIGHT + topMargin + bottomMargin);
	}

	/** Adjusts the format with another plot as a template, using the current
	 *  (usually default) flags of this plot. Does not modify the properties of
	 *	individual data sets (marker symbol, color, line thickness...).
	 *	<code>plot</code> may be null; then the call has no effect. */
	public void useTemplate(Plot plot) {
		useTemplate(plot, templateFlags);
	}
	/** Adjusts the format with another plot as a template. Flags determine what to
	 *	copy from the template. Does not modify the properties of
	 *	individual data sets (marker symbol, color, line thickness...).
	 *	<code>plot</code> may be null; then the call has no effect. */
	public void useTemplate(Plot plot, int templateFlags) {
		if (plot == null) return;
		this.flags = plot.flags;
		this.defaultFont = plot.defaultFont;
		this.currentFont = plot.currentFont;
		this.xLabelFont = plot.xLabelFont;
		this.yLabelFont = plot.yLabelFont;
		this.currentLineWidth = plot.currentLineWidth;
		this.frameLineWidth = plot.frameLineWidth;
		this.currentColor = plot.currentColor;
		this.frameColor = plot.frameColor;
		if ((templateFlags & COPY_LABELS) != 0) {
			this.xLabel = plot.xLabel;
			this.yLabel = plot.yLabel;
		}
		if ((templateFlags & COPY_LEGEND) != 0) {
		}
		for (int i=0; i<currentMinMax.length; i++)
			if ((templateFlags>>(i/2)&0x1) != 0) {
				currentMinMax[i] = plot.currentMinMax[i];
				if (!plotDrawn) defaultMinMax[i] = plot.currentMinMax[i];
			}
		if ((templateFlags & COPY_LEGEND) != 0) {
			if (plot.legend != null)
				this.legend = plot.legend.clone();
			int plotPObjectIndex = 0;
			int plotPObjectNumber = plot.allPlotObjects.size();
			for (PlotObject plotObject : allPlotObjects) {
				if (plotObject.type == PlotObject.XY_DATA) {
					while(plotPObjectIndex<plotPObjectNumber && plot.allPlotObjects.get(plotPObjectIndex).type != PlotObject.XY_DATA)
						plotPObjectIndex++; //skip everything that has no label
					if (plotPObjectIndex>=plotPObjectNumber) break;
					plotObject.label = plot.allPlotObjects.get(plotPObjectIndex).label;
				}
			}
		}
		setFrameSize(plot.plotWidth, plot.plotHeight);
		this.templateFlags = templateFlags;
	}

	/** Sets the scale. Everything, including labels, line thicknesses, etc will be scaled by this factor.
	 *	Should be called before creating the plot. */
	public void setScale(float scale) {
		this.scale = scale;
		if (scale > 20f) scale = 20f;
		if (scale < 0.7f) scale = 0.7f;
		plotDrawn = false;
	}

	/** Sets the labels of the x and y axes. Labels may be null.
	 *	Call updateImage() thereafter to make the change visible (if it is shown already). */
	public void setXYLabels(String xLabel, String yLabel) {
		this.xLabel = xLabel!=null && xLabel.length()>0 ? xLabel : "";
		this.yLabel = yLabel!=null && yLabel.length()>0 ? yLabel : "";
	}

	/** Sets the maximum number of intervals in a plot.	 
	 *	Call updateImage() thereafter to make the change visible (if the image is shown already). */
	public void setMaxIntervals(int intervals) {
			maxIntervals = intervals;
	}

	/** Sets the length of the major tick in pixels.
	 *	Call updateImage() thereafter to make the change visible (if the image is shown already). */
	public void setTickLength(int tickLength) {
			tickLength = tickLength;
	}

	/** Sets the length of the minor tick in pixels. */
	public void setMinorTickLength(int minorTickLength) {
			minorTickLength = minorTickLength;
	}

	/** Sets the flags that control the axes format.
	 *	Does not modify the flags for logarithmic axes on/off and the FORCE2GRID flags.
	 *	Call updateImage() thereafter to make the change visible (if it is shown already). */
	public void setFormatFlags(int flags) {
		int unchangedFlags = X_LOG_NUMBERS | Y_LOG_NUMBERS | X_FORCE2GRID | Y_FORCE2GRID;
		flags = flags & (~unchangedFlags);	  //remove flags that should not be affected
		this.flags = (this.flags & unchangedFlags) | flags;
	}

	/** Returns the flags that control the axes */
	public int getFlags() {
		return flags;
	}

	/** Sets the X Axis format to Log or Linear.
	 *	Call updateImage() thereafter to make the change visible (if it is shown already). */
	public void setAxisXLog(boolean axisXLog) {
		flags = axisXLog ? flags | X_LOG_NUMBERS : flags & (~X_LOG_NUMBERS);
	}

	/** Sets the Y Axis format to Log or Linear.
	 *	Call updateImage() thereafter to make the change visible (if it is shown already). */
	public void setAxisYLog(boolean axisYLog) {
		flags = axisYLog ? flags | Y_LOG_NUMBERS : flags & (~Y_LOG_NUMBERS);
	}

	/** Sets whether to show major ticks at the x axis.
	 *	Call updateImage() thereafter to make the change visible (if the image is shown already). */

	public void setXTicks(boolean xTicks) {
		flags = xTicks ? flags | X_TICKS : flags & (~X_TICKS);
	}

	/** Sets whether to show major ticks at the y axis.
	 *	Call updateImage() thereafter to make the change visible (if the image is shown already). */
	public void setYTicks(boolean yTicks) {
		flags = yTicks ? flags | Y_TICKS : flags & (~Y_TICKS);
	}

	/** Sets whether to show minor ticks on the x axis (if linear). Also sets major ticks if true and no grid is set.
	 *	Call updateImage() thereafter to make the change visible (if the image is shown already). */
	public void setXMinorTicks(boolean xMinorTicks) {
		flags = xMinorTicks ? flags | X_MINOR_TICKS : flags & (~X_MINOR_TICKS);
		if (xMinorTicks && !hasFlag(X_GRID))
			flags |= X_TICKS;
	}

	/** Sets whether to show minor ticks on the y axis (if linear). Also sets major ticks if true and no grid is set.
	 *	Call updateImage() thereafter to make the change visible (if the image is shown already). */
	public void setYMinorTicks(boolean yMinorTicks) {
		flags = yMinorTicks ? flags | Y_MINOR_TICKS : flags & (~Y_MINOR_TICKS);
		if (yMinorTicks && !hasFlag(Y_GRID))
			flags |= Y_TICKS;
	}

	/** Sets the properties of the axes. Call updateImage() thereafter to make the change visible
	 *	(if the image is shown already). */
	public void setAxes(boolean xLog, boolean yLog, boolean xTicks, boolean yTicks, boolean xMinorTicks, boolean yMinorTicks,
			int tickLenght, int minorTickLenght) {
		setAxisXLog		  (xLog);
		setAxisYLog		  (yLog);
		setXMinorTicks	  (xMinorTicks);
		setYMinorTicks	  (yMinorTicks);
		setXTicks		  (xTicks);
		setYTicks		  (yTicks);
		setTickLength	  (tickLenght);
		setMinorTickLength(minorTickLenght);
	}

	/** Sets log scale in x. Call updateImage() thereafter to make the change visible
	 *	(if the image is shown already). */

	public void setLogScaleX() {
		setAxisXLog(true);
	}

	public void setLogScaleY() {
		setAxisYLog(true);
	}

	/** The default flags, taking PlotWindow.noGridLines, PlotWindow.noTicks into account */
	public static int getDefaultFlags() {
		int defaultFlags = 0;
		if (!PlotWindow.noGridLines) //note that log ticks are also needed because the range may span less than a decade, then no grid is visible
			defaultFlags |= X_GRID | Y_GRID | X_NUMBERS | Y_NUMBERS | X_LOG_TICKS | Y_LOG_TICKS;
		if (!PlotWindow.noTicks)
			defaultFlags |= X_TICKS | Y_TICKS | X_MINOR_TICKS | Y_MINOR_TICKS | X_NUMBERS | Y_NUMBERS | X_LOG_TICKS | Y_LOG_TICKS;
		return defaultFlags;
	}

	/** Adds a set of points to the plot or adds a curve if shape is set to LINE.
	 * @param xValues	the x coordinates, or null. If null, integers starting at 0 will be used for x.
	 * @param yValues	the y coordinates (must not be null)
	 * @param yErrorBars error bars in y, may be null
	 * @param shape		CIRCLE, X, BOX, TRIANGLE, CROSS, DOT, LINE, CONNECTED_CIRCLES
	 * @param label		Label for this curve or set of points, used for a legend and for listing the plots
	 */
	public void addPoints(float[] xValues, float[] yValues, float[] yErrorBars, int shape, String label) {
		if (xValues==null || xValues.length==0) {
			xValues = new float[yValues.length];
			for (int i=0; i<yValues.length; i++)
				xValues[i] = i;
		}
		allPlotObjects.add(new PlotObject(xValues, yValues, yErrorBars, shape, currentLineWidth, currentColor, currentColor2, label));
		if (plotDrawn) updateImage();
	}
	/** Adds a set of points to the plot or adds a curve if shape is set to LINE.
	 * @param x			the x coordinates
	 * @param y			the y coordinates
	 * @param shape		CIRCLE, X, BOX, TRIANGLE, CROSS, DOT, LINE, CONNECTED_CIRCLES
	 */
	public void addPoints(float[] x, float[] y, int shape) {
		addPoints(x, y, null, shape, null);
	}

	/** Adds a set of points to the plot using double arrays. */
	public void addPoints(double[] x, double[] y, int shape) {
		addPoints(Tools.toFloat(x), Tools.toFloat(y), shape);
	}
	
	/** This a version of addPoints that works with JavaScript. */
	public void addPoints(String dummy, float[] x, float[] y, int shape) {
		addPoints(x, y, shape);
	}

	/** Adds a set of points to the plot using double ArrayLists.
	 * Must be called before the plot is displayed. */	
	public void addPoints(ArrayList x, ArrayList y, int shape) {
		addPoints(getDoubleFromArrayList(x), getDoubleFromArrayList(y), shape);
	}	

	/** Adds a set of points to the plot or adds a curve if shape is set to LINE.
	 * @param x			the x-coodinates
	 * @param y			the y-coodinates
	 * @param errorBars			the vertical error bars, may be null
	 * @param shape		CIRCLE, X, BOX, TRIANGLE, CROSS, DOT or LINE
	 */
	public void addPoints(double[] x, double[] y, double[] errorBars, int shape) {
		addPoints(Tools.toFloat(x), Tools.toFloat(y), Tools.toFloat(errorBars), shape, null);
	}

	/** Adds a set of points to the plot using double ArrayLists.
	 * Must be called before the plot is displayed. */	
	public void addPoints(ArrayList x, ArrayList y, ArrayList z, int shape) {
		addPoints(getDoubleFromArrayList(x), getDoubleFromArrayList(y), getDoubleFromArrayList(z), shape);
	}	

	public double[] getDoubleFromArrayList(ArrayList list) {
		double[] targ = new double[list.size()];
		for (int i = 0; i < list.size(); i++)
			targ[i] = ((Double) list.get(i)).doubleValue();
		return targ;
	}
	
	/** Adds a set of points that will be drawn as ARROWs.
	 * @param x1		the x-coodinates of the beginning of the arrow
	 * @param y1		the y-coodinates of the beginning of the arrow
	 * @param x2		the x-coodinates of the end		  of the arrow
	 * @param y2		the y-coodinates of the end		  of the arrow
	 */
	public void drawVectors(double[] x1, double[] y1, double[] x2, double[] y2) {
		allPlotObjects.add(new PlotObject(Tools.toFloat(x1), Tools.toFloat(y1),
				Tools.toFloat(x2), Tools.toFloat(y2), currentLineWidth, currentColor));
	}

	public static double calculateDistance(int x1, int y1, int x2, int y2) {
		return java.lang.Math.sqrt((x2 - x1)*(double)(x2 - x1) + (y2 - y1)*(double)(y2 - y1));
	}

	/** Adds a set of vectors to the plot using double ArrayLists.
	 *	Does not support logarithmic axes.
	 *	Must be called before the plot is displayed. */ 
	public void drawVectors(ArrayList x1, ArrayList y1, ArrayList x2, ArrayList y2) {
		drawVectors(getDoubleFromArrayList(x1), getDoubleFromArrayList(y1), getDoubleFromArrayList(x2), getDoubleFromArrayList(y2));
	}	

	/** Adds vertical error bars to the last data passed to the plot (via the constructor or addPoints). */
	public void addErrorBars(float[] errorBars) {
		PlotObject mainObject = getLastCurveObject();
		if (mainObject != null)
			mainObject.yEValues = errorBars;
		else throw new RuntimeException("Plot can't add y error bars without data");
	}
	
	/** Adds vertical error bars to the last data passed to the plot (via the constructor or addPoints). */
	public void addErrorBars(double[] errorBars) {
		addErrorBars(Tools.toFloat(errorBars));
	}

	/** This is a version of addErrorBars that works with JavaScript. */
	public void addErrorBars(String dummy, float[] errorBars) {
		addErrorBars(errorBars);
	}

	/** Adds horizontal error bars to the last data passed to the plot (via the constructor or addPoints). */
	public void addHorizontalErrorBars(double[] xErrorBars) {
		PlotObject mainObject = getLastCurveObject();
		if (mainObject != null)
			mainObject.xEValues = Tools.toFloat(xErrorBars);
		else throw new RuntimeException("Plot can't add x error bars without data");
	}

	/** Draws text at the specified location, where (0,0)
	 * is the upper left corner of the the plot frame and (1,1) is
	 * the lower right corner. Uses the justification specified by setJustification(). */
	public void addLabel(double x, double y, String label) {
		allPlotObjects.add(new PlotObject(label, x, y, currentJustification, currentFont, currentColor, PlotObject.NORMALIZED_LABEL));
	}

	/* Draws text at the specified location, using the coordinate system defined
	 * by setLimits() and the justification specified by setJustification(). */
	public void addText(String label, double x, double y) {
		allPlotObjects.add(new PlotObject(label, x, y, currentJustification, currentFont, currentColor, PlotObject.LABEL));
	}

	/** Adds a legend. The legend will be always drawn last (on top of everything).
	 *	To modify the legend's style, call 'setFont' and 'setLineWidth' before 'setLegend'
	 *	@param labels labels of the points or curves in the sequence of the data added, tab-delimited or linefeed-delimited.
	 *	The labels of the datasets will be set to these values. If null or not given, the labels set
	 *	previously (if any) will be used.
	 *	@param flags  Bitwise or of position (AUTO_POSITION, TOP_LEFT etc.), ERASE_BACKGROUND, and LEGEND_BOTTOM_UP if desired.
	 *	Updates the image (if it is shown already). */
	public void setLegend(String labels, int flags) {
		legend = new PlotObject(labels, currentLineWidth == 0 ? 1 : currentLineWidth,
				currentFont == null ? defaultFont : currentFont, currentColor == null ? Color.black : currentColor, flags);
		if (plotDrawn) updateImage();
	}

	/** Sets the justification used by addLabel(), where <code>justification</code>
	 * is Plot.LEFT, Plot.CENTER or Plot.RIGHT. Default is LEFT. */
	public void setJustification(int justification) {
		currentJustification = justification;
	}
	
	/** Changes the drawing color for the next objects that will be added to the plot. 
	 *	For selecting the color of the curve passed with the constructor,
	 *	use <code>setColor</code> before <code>draw</code>.
	 *	The frame and labels are always drawn in black. */
	public void setColor(Color c) {
		currentColor = c;
		if (c.getRed() != c.getGreen() || c.getGreen() != c.getBlue())
			isColor = true;
		currentColor2 = null;
	}
	
	/** Changes the drawing color for the next objects that will be added to the plot.
	 *	It also sets secondary color: This is the color of the line for CONNECTED_CIRCLES,
	 *	and the color for filling open symbols (CIRCLE, BOX, TRIANGLE).
	 *	Set it to null or use the one-argument call setColor(color) to disable filling.
	 *	For selecting the color of the curve passed with the constructor,
	 *	use <code>setColor</code> before <code>draw</code>.
	 *	The frame and labels are always drawn in black. */
	public void setColor(Color c, Color c2) {
		currentColor = c;
		currentColor2 = c2;
		if (c.getRed() != c.getGreen() || c.getGreen() != c.getBlue() || (c2 != null && (
				c2.getRed() != c2.getGreen() || c2.getGreen() != c2.getBlue())))
			isColor = true;
	}
	
	/** Changes the line width for the next objects that will be added to the plot. */
	public void setLineWidth(int lineWidth) {
		currentLineWidth = lineWidth;
	}

	/** Changes the line width for the next objects that will be added to the plot. */
	public void setLineWidth(float lineWidth) {
		currentLineWidth = lineWidth;
	}

	/* Draws a line using the coordinate system defined by setLimits(). */
	public void drawLine(double x1, double y1, double x2, double y2) {
		allPlotObjects.add(new PlotObject(x1, y1, x2, y2, currentLineWidth, 0, currentColor, PlotObject.LINE));
	}
	
	/** Draws a line using a normalized 0-1, 0-1 coordinate system,
	 * with (0,0) at the top left and (1,1) at the lower right corner.
	 * This is the same coordinate system used by addLabel(x,y,label).
	 */
	public void drawNormalizedLine(double x1, double y1, double x2, double y2) {
		allPlotObjects.add(new PlotObject(x1, y1, x2, y2, currentLineWidth, 0, currentColor, PlotObject.NORMALIZED_LINE));
	}

	/* Draws a line using the coordinate system defined by setLimits(). */
	public void drawDottedLine(double x1, double y1, double x2, double y2, int step) {
		allPlotObjects.add(new PlotObject(x1, y1, x2, y2, currentLineWidth, step, currentColor, PlotObject.DOTTED_LINE));
	}

	/** Sets the font for all following drawLabel etc. operations. The currently set font when
	 *	displaying the plot determines the font of all labels & numbers
	 *	Call updateImage() thereafter to make the change visible (if the image is shown already). */
	public void setFont(Font font) {
		currentFont = font;
	}

	/** Sets the font size and style for all following drawLabel etc. operations. This leaves
	 *	the font name and style of the previously used fonts unchanged. The currently set font
	 *	when displaying the plot determines the font of the numbers at the axes.
	 *	That font also sets the default label font size, which may be overridden by
	 *	setAxisLabelFontSize or setXLabelFont, setYLabelFont.
	 *	Styles are defined in the Font class, e.g. Font.PLAIN, Font.BOLD.
	 *	Set <code>style</code> to -1 to leave the style unchanged.
	 *	Call updateImage() thereafter to make the change visible (if the image is shown already). */
	public void setFont(int style, float size) {
		if (size < 9) size = 9f;
		if (size > 24) size = 24f;
		Font previousFont = currentFont == null ? defaultFont : currentFont;
		if (style < 0) style = previousFont.getStyle();
		setFont(previousFont.deriveFont(size));
	}

	/** Sets the size of the x and y label font size and style. Styles are defined
	 *	in the Font class, e.g. Font.PLAIN, Font.BOLD.
	 *	Set <code>style</code> to -1 to leave the style unchanged.
	 *	Call updateImage() thereafter to make the change visible (if the image is shown already). */
	public void setAxisLabelFont(int style, float size) {
		if (size < 9) size = 9f;
		if (size > 33) size = 33f;
		Font usualFont = currentFont == null ? defaultFont : currentFont;
		if (xLabelFont == null) xLabelFont = usualFont;
		if (yLabelFont == null) yLabelFont = usualFont;
		setXLabelFont(xLabelFont.deriveFont(style < 0 ? xLabelFont.getStyle() : style, size));
		setYLabelFont(xLabelFont.deriveFont(style < 0 ? yLabelFont.getStyle() : style, size));
	}

	/** Sets the xLabelFont; must not be mull. If this method is not used, the last setFont
	 *	of setFontSize call before displaying the plot determines the font, or if neither
	 *	was called, the font size of the Profile Plot Options is used. */
	public void setXLabelFont(Font font) {
		xLabelFont = font;
	}

	/** Sets the yLabelFont; must not be null. */
	public void setYLabelFont(Font font) {
		yLabelFont = font;
	}

	/** Determines whether to use antialiased text (default true) */
	public void setAntialiasedText(boolean antialiasedText) {
		this.antialiasedText = antialiasedText;
	}

	/** Obsolete; replaced by setFont(). */
	public void changeFont(Font font) {
		setFont(font);
	}

	/** Get the x coordinates of the data set passed with the constructor (if not null)
	 *	or otherwise of the data set of the first 'addPoints'. Returns null if neither exists */
	public float[] getXValues() {
		PlotObject p = getMainCurveObject();
		return p==null ? null : p.xValues;
	}

	/** Get the y coordinates of the data set passed with the constructor (if not null)
	 *	or otherwise of the data set of the first 'addPoints'. Returns null if neither exists */
	public float[] getYValues() {
		PlotObject p = getMainCurveObject();
		return p==null ? null : p.yValues;
	}

	/** Sets the plot range to the initial value determined from minima&maxima or given by setLimits.
	 *	Updates the image if existing and updateImg is true */
	public void setLimitsToDefaults(boolean updateImg) {
		saveMinMax();
		System.arraycopy(defaultMinMax, 0, currentMinMax, 0, defaultMinMax.length);
		if (plotDrawn && updateImg) updateImage();
	}

	/** Sets the plot range to encompass all data. Updates the image if existing. */
	public void setLimitsToFit(boolean updateImg) {
		saveMinMax();
		currentMinMax = getMinAndMax(true, 0xff);
		if (plotDrawn && updateImg) updateImage();
	}

	/** reverts plot range to previous values and updates the image */
	public void setPreviousMinMax() {
		if (Double.isNaN(savedMinMax[0])) return;  //no saved values yet
		double[] swap = new double[currentMinMax.length];
		System.arraycopy(currentMinMax, 0, swap, 0, currentMinMax.length);
		System.arraycopy(savedMinMax, 0, currentMinMax, 0, currentMinMax.length);
		System.arraycopy(swap, 0, savedMinMax, 0, currentMinMax.length);
		updateImage();		
	}

	/** Draws the plot (if not done before) in an ImageProcessor and returns the ImageProcessor with the plot. */
	public ImageProcessor getProcessor() {
		draw();
		return ip;
	}

	/** Returns the plot as an ImagePlus.
	 *	If an ImagePlus for this plot already exists, displays the plot in that ImagePlus and returns it. */
	public ImagePlus getImagePlus() {
		if (plotDrawn) updateImage();
		else draw();
		if (imp != null) {
			if (imp.getProcessor() != ip) imp.setProcessor(ip);
			return imp;
		}
		ImagePlus imp = new ImagePlus(title, ip);
		imp.setGlobalCalibration(null);
		Calibration cal = imp.getCalibration();
		adjustCalibration(cal);
		if (this.imp == null)
			this.imp = imp;
		imp.setProperty(PROPERTY_KEY, this);
		return imp;
	}

	/** Sets the ImagePlus where the plot will be displayed. If the ImagePlus is not
	 *	known otherwise (e.g. from getImagePlus), this is needed for changes such as
	 *	zooming in to work correctly. It also sets the calibration of the ImagePlus. */
	public void setImagePlus(ImagePlus imp) {
		if (this.imp != null)
			this.imp.setProperty(PROPERTY_KEY, null);
		this.imp = imp;
		if (imp != null) {
			adjustCalibration(imp.getCalibration());
			imp.setGlobalCalibration(null);
			imp.setProperty(PROPERTY_KEY, this);
		}
	}

	/** Adjusts a Calibration object to fit the current axes.
	 *	For log axes, the calibration refers to the base-10 logarithm of the value */
	public void adjustCalibration(Calibration cal) {
		if (xMin == xMax)	//tiff images can't handle infinity in scale, see TiffEncoder.writeScale
			xScale = 1e6;
		if (yMin == yMax)
			yScale = 1e6;
		cal.xOrigin = xBasePxl-xMin*xScale;
		cal.pixelWidth	= 1.0/Math.abs(xScale); //Calibration must not have negative pixel size
		cal.yOrigin = yBasePxl+yMin*yScale;
		cal.pixelHeight = 1.0/Math.abs(yScale);
		cal.setInvertY(yScale >= 0);
		if (xMin == xMax)
			xScale = Double.POSITIVE_INFINITY;
		if (yMin == yMax)
			yScale = Double.POSITIVE_INFINITY;
	}

	/** Displays the plot in a PlotWindow and returns a reference to the PlotWindow. */
	public PlotWindow show() {
		if ((IJ.macroRunning() && IJ.getInstance()==null) || Interpreter.isBatchMode()) {
			imp = getImagePlus();
			WindowManager.setTempCurrentImage(imp);
			if (getMainCurveObject() != null) {
				imp.setProperty("XValues", getXValues()); // Allows values to be retrieved by 
				imp.setProperty("YValues", getYValues()); // by Plot.getValues() macro function
			}
			Interpreter.addBatchModeImage(imp);
			return null;
		}
		if (imp != null) {
			Window win = imp.getWindow();
			if (win instanceof PlotWindow && win.isVisible()) {
				updateImage();			// show in existing window
				return (PlotWindow)win;
			}
		}
		PlotWindow pw = new PlotWindow(this);
		if (imp == null)
			imp.setProperty(PROPERTY_KEY, null);
		imp = pw.getImagePlus();
		imp.setProperty(PROPERTY_KEY, this);
		if (IJ.isMacro() && imp!=null) // wait for plot to be displayed
			IJ.selectWindow(imp.getID());
		return pw;
	}

	/** Draws the plot specified for the first time. Does nothing if the plot has been drawn already.
	 *	Call getProcessor to retrieve the ImageProcessor with it.
	 *	Does no action with respect to the ImagePlus (it any) */
	public void draw() {
		//IJ.log("draw(); plotDrawn="+plotDrawn);
		if (plotDrawn) return;
		getInitialMinAndMax();
		getBlankProcessor();
		drawContents(ip);
	}

	/** Freezes or unfreezes the plot. In the frozen state, the plot cannot be resized or updated,
	 *	and the Plot class does no modifications to the ImageProcessor.
	 *	Changes are recorded nevertheless and become effective with <code>setFrozen(false)</code>. */
	public void setFrozen(boolean frozen) {
		this.frozen = frozen;
		if (!frozen) {	// unfreeze operations ...
			if (imp != null && ip != null) {
				ImageCanvas ic = imp.getCanvas();
				if (ic instanceof PlotCanvas)
					((PlotCanvas)ic).resetMagnification();
			}
			updateImage();
			ImageWindow win = imp.getWindow();
			if (win != null) win.updateImage(imp); //show any changes made during the frozen state
		}
	}

	public boolean isFrozen() {
		return frozen;
	}

	/** Draws the plot again, ignored if the plot has not been drawn before or the plot is frozen
	 *	If the ImagePlus exist, updates it and its calibration. */
	public void updateImage() {
		if (!plotDrawn || frozen) return;
		getBlankProcessor();
		drawContents(ip);
		if (imp == null) return;
		adjustCalibration(imp.getCalibration());
		imp.updateAndDraw();
	}

	/** Returns the rectangle where the data are plotted.
	 *	This rectangle includes the black outline frame at the top and left, but not at the bottom
	 *	and right (when the frame is plotted with 1 pxl width).
	 *	The image scale is its width or height in pixels divided by the data range in x or y. */
	public Rectangle getDrawingFrame() {
		if (frame == null)
			getBlankProcessor(); //setup frame if not done yet
			return new Rectangle(frame.x, frame.y, frameWidth, frameHeight);
	}

	/** Creates a new high-resolution plot by scaling it and displays that plot if showIt is true.
	 *	<code>title</code> may be null, then a default title is used. */
	public ImagePlus makeHighResolution(String title, float scale, boolean antialiasedText, boolean showIt) {
		Plot hiresPlot = null;
		try {
			hiresPlot = (Plot)clone();	//shallow clone, thus arrays&objects are not cloned
		} catch (Exception e) {return null;}
		hiresPlot.ip = null;
		hiresPlot.imp = null;
		if (!plotDrawn) hiresPlot.getInitialMinAndMax();
		hiresPlot.setScale(scale);
		hiresPlot.setAntialiasedText(antialiasedText);
		hiresPlot.defaultMinMax = currentMinMax.clone();
		ImageProcessor hiresIp = hiresPlot.getProcessor();
		if (title == null || title.length() == 0)
			title = getTitle()+"_HiRes";
		title = WindowManager.makeUniqueName(title);
		ImagePlus hiresImp = new ImagePlus(title, hiresIp);
		Calibration cal = hiresImp.getCalibration();
		hiresPlot.adjustCalibration(cal);
		if (showIt) {
			hiresImp.setGlobalCalibration(null);
			hiresImp.show();
		}
		hiresPlot.dispose(); //after drawing, we don't need the plot of the high-resolution image any more
		return hiresImp;
	}

	/** Releases the ImageProcessor and ImagePlus associated with the plot.
	 *	May help garbage collection because some garbage collectors
	 *	are said to be inefficient with circular references. */
	public void dispose() {
		if (imp != null)
			imp.setProperty(PROPERTY_KEY, null);
		imp = null;
		ip = null;
	}

	/** Converts pixels to calibrated coordinates. In contrast to the image calibration, also
	 *	works with log axes and inverted x axes */
	public double descaleX(int x) {
		if (xMin == xMax) return xMin;
		double xv = (x-xBasePxl)/xScale + xMin;
		if (logXAxis) xv = Math.pow(10, xv);
		return xv;
	}

	/** Converts pixels to calibrated coordinates. In contrast to the image calibration, also
	 *	works with log axes */
	public double descaleY(int y) {
		if (yMin == yMax) return yMin;
		double yv = (yBasePxl-y)/yScale +yMin;
		if (logYAxis) yv = Math.pow(10, yv);
		return yv;
	}


	/** Converts calibrated coordinates to pixel coordinates. In contrast to the image calibration, also
	 *	works with log x axis and inverted x axis */
	public double scaleXtoPxl(double x) {
		if (xMin == xMax) {
			if (x==xMin) return xBasePxl;
			else return x>xMin ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
		}
		if (logXAxis)
			return xBasePxl+(Math.log10(x)-xMin)*xScale;
		else
			return xBasePxl+(x-xMin)*xScale;
	}

	/** Converts calibrated coordinates to pixel coordinates. In contrast to the image calibration, also
	 *	works with log y axis */
	public double scaleYtoPxl(double y) {
		if (yMin == yMax) {
			if (y==xMin) return yBasePxl;
			else return y>yMin ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
		}
		if (logYAxis)
			return yBasePxl-(Math.log10(y)-yMin)*yScale;
		else
			return yBasePxl-(y-yMin)*yScale;
	}

	/** Calibrated coordinates to integer pixel coordinates */
	private int scaleX(double x) {
		if (xMin == xMax) {
			if (x==xMin) return xBasePxl;
			else return x>xMin ? Integer.MAX_VALUE : Integer.MIN_VALUE;
		}
		if (logXAxis)
			return xBasePxl+(int)Math.round((Math.log10(x)-xMin)*xScale);
		else
			return xBasePxl+(int)Math.round((x-xMin)*xScale);
	}

	/** Converts calibrated coordinates to pixel coordinates. In contrast to the image calibration, also
	 *	works with log axes */
	private int scaleY(double y) {
		if (yMin == yMax) {
			if (y==yMin) return yBasePxl;
			else return y>yMin ? Integer.MAX_VALUE : Integer.MIN_VALUE;
		}
		if (logYAxis)
			return yBasePxl-(int)Math.round((Math.log10(y)-yMin)*yScale);
		else
			return yBasePxl-(int)Math.round((y-yMin)*yScale);
	}

	/** Converts calibrated coordinates to pixel coordinates. In contrast to the image calibration, also
	 *	works with log axes and inverted x axes. Returns -1 instead NaN for log x axis and zero or negative x */
	private int scaleXWithOverflow(double x) {
		if (!logXAxis || x>0)
			return scaleX(x);
		else
			return xScale > 0 ? -1 : ip.getWidth();
	}

	/** Converts calibrated coordinates to pixel coordinates. In contrast to the image calibration, also
	 *	works with log axes and inverted x axes. Returns -1 instead NaN for log y axis and zero or negative y */
	private int scaleYWithOverflow(double y) {
		if (!logYAxis || y>0)
			return scaleY(y);
		else
			return yScale > 0 ? ip.getHeight() : -1;
	}

	/** Scales a value of the original plot for a high-resolution plot. Returns an integer number of pixels >=1 */
	int sc(float length) {
		int pixels = (int)(length*scale + 0.5);
		if (pixels < 1) pixels = 1;
		return pixels;
	}

	/** Scales a font of the original plot for a high-resolution plot. */
	Font scFont(Font font) {
		float size = font.getSize2D();
		return scale==1 ? font : font.deriveFont(size*scale);
	}

	/** Draws the plot contents (all PlotObjects and the frame and legend), without axes etc. */
	void drawContents(ImageProcessor ip) {
		makeRangeGetSteps();
		ip.setColor(Color.black);
		ip.setLineWidth(sc(1));
		float lineWidth = 1;
		Color color = Color.black;
		Font font = defaultFont;

		// draw all the plot objects in the sequence they were added, except for the one of the constructor
		for (PlotObject plotObject : allPlotObjects)
			if (!plotObject.hasFlag(PlotObject.CONSTRUCTOR_DATA)) {
				//properties lineWidth, Font, Color set for one object remain for the next object unless changed
				if (plotObject.lineWidth > 0)
					lineWidth = plotObject.lineWidth;
				else
					plotObject.lineWidth = lineWidth;
				if (plotObject.color != null)
					color = plotObject.color;
				else
					plotObject.color = color;
				if (plotObject.font != null)
					font = plotObject.font;
				else
					plotObject.font = font;
				//IJ.log("type="+plotObject.type+" color="+plotObject.color);
				drawPlotObject(plotObject, ip);
			}

		// draw the line passed with the constructor last, using the settings present when calling 'draw'
		if (allPlotObjects.size()>0 && allPlotObjects.get(0).hasFlag(PlotObject.CONSTRUCTOR_DATA)) {
			PlotObject mainPlotObject = allPlotObjects.get(0);
			if (mainPlotObject.lineWidth == 0)
				mainPlotObject.lineWidth = currentLineWidth == 0 ? 1 : currentLineWidth;
			lineWidth = mainPlotObject.lineWidth;
			if (mainPlotObject.color == null)
				mainPlotObject.color = currentColor == null ? Color.black : currentColor;
			drawPlotObject(mainPlotObject, ip);
		} else {
			if (currentLineWidth > 0) lineWidth = currentLineWidth; //linewidth when drawing determines frame linewidth
		}

		// finally draw the frame & legend
		frameLineWidth = lineWidth;
		if (frameLineWidth == 0) frameLineWidth = 1;
		if (frameLineWidth > 3) frameLineWidth = 3;
		ip.setLineWidth(sc(frameLineWidth));
		ip.setColor(frameColor);
		int x2 = frame.x + frame.width - 1;
		int y2 = frame.y + frame.height - 1;
		ip.moveTo(frame.x, frame.y);	// draw the frame. Can't use ip.drawRect because it is inconsistent for different lineWidths
		ip.lineTo(x2, frame.y);
		ip.lineTo(x2, y2);
		ip.lineTo(frame.x, y2);
		ip.lineTo(frame.x, frame.y);
		if (legend != null && (legend.flags & LEGEND_POSITION_MASK) != 0)
			drawPlotObject(legend, ip);

		plotDrawn = true;
	}

	/** Creates the processor if not existing, clears the background and prepares
	 *	it for plotting. Also called by the PlotWindow class to prepare the window. */
	ImageProcessor getBlankProcessor() { 
		float marginScale = 0.2f + 0.8f*(currentFont == null ? defaultFont : currentFont).getSize2D()/12f;
		if (marginScale < 0.7f) marginScale = 0.7f;
		if (marginScale > 2f) marginScale = 2f;
		leftMargin	 = sc(LEFT_MARGIN*marginScale);
		rightMargin	 = sc(RIGHT_MARGIN*marginScale);
		topMargin	 = sc(TOP_MARGIN*marginScale);
		bottomMargin = sc(BOTTOM_MARGIN*marginScale);
		frameWidth	= sc(plotWidth);
		frameHeight = sc(plotHeight);
		int width  = frameWidth + leftMargin + rightMargin;
		int height = frameHeight + topMargin + bottomMargin;
		if (ip == null || width != ip.getWidth() || height != ip.getHeight() || (isColor && (ip instanceof ByteProcessor))) {
			if (isColor) {
				ip = new ColorProcessor(width, height);
			} else {
				ip = new ByteProcessor(width, height);
				invertedLut = Prefs.useInvertingLut && !Interpreter.isBatchMode() && IJ.getInstance()!=null;
				if (invertedLut) ip.invertLut();
			}
			if (imp != null) imp.setProcessor(ip);
		}
		if (ip instanceof ColorProcessor)
			Arrays.fill((int[])(ip.getPixels()), 0xffffff);
		else
			Arrays.fill((byte[])(ip.getPixels()), invertedLut ? (byte)0 : (byte)0xff);

		ip.setColor(Color.black);
		ip.setFont(scFont(defaultFont));
		ip.setLineWidth(sc(1));
		ip.setAntialiasedText(antialiasedText);
		frame = new Rectangle(leftMargin, topMargin, frameWidth+1, frameHeight+1);
		return ip;
	}

	/** Calculate the actual range, major step interval and set variables for data <-> pixels scaling */
	double[] makeRangeGetSteps() {
		double[] steps = new double[2];
		logXAxis = hasFlag(X_LOG_NUMBERS);
		logYAxis = hasFlag(Y_LOG_NUMBERS);

		for (int i=0; i<currentMinMax.length; i+=2) {  //for x and y direction
			boolean logAxis = hasFlag(i==0 ? X_LOG_NUMBERS : Y_LOG_NUMBERS);
			//don't zoom in too much (otherwise float conversion to int pixels may be wrong)
			double range = currentMinMax[i+1]-currentMinMax[i];
			double mid = 0.5*(currentMinMax[i+1]+currentMinMax[i]);
			double relativeRange = Math.abs(range/mid);
			if (!logAxis)
				relativeRange = Math.min(relativeRange, Math.abs(range/(defaultMinMax[i+1]-defaultMinMax[i]))); 
			if (range != 0 && relativeRange<1e-4) {
				currentMinMax[i+1] = mid + 0.5*range*1e-4/relativeRange;
				currentMinMax[i] = mid - 0.5*range*1e-4/relativeRange;
			}
			//no log range if range is too small or not positive values
			if (logAxis) {
				double rangeRatio = currentMinMax[i+1]/currentMinMax[i];
				if (!(rangeRatio > MIN_LOG_RATIO || 1./rangeRatio > MIN_LOG_RATIO) ||
					!(currentMinMax[i] > 10*Float.MIN_VALUE)  || !(currentMinMax[i+1] > 10*Float.MIN_VALUE))
				logAxis = false;
			}
			//for log axes, temporarily work on the logarithm
			if (logAxis) {
				currentMinMax[i] = Math.log10(currentMinMax[i]);
				currentMinMax[i+1] = Math.log10(currentMinMax[i+1]);
			}
			// calculate grid or major tick interval
			if ((i==0 && !simpleXAxis()) || (i==2 && !simpleYAxis())) {
				int minGridspacing = i==0 ? MIN_X_GRIDSPACING : MIN_Y_GRIDSPACING;
				int frameSize = i==0 ? frameWidth : frameHeight;
				double step = Math.abs((currentMinMax[i+1] - currentMinMax[i]) *
						Math.max(1.0/maxIntervals, (float)sc(minGridspacing)/frameSize+0.06)); //the smallest allowable step
				step = niceNumber(step);
				if (logAxis && step < 1)
					step = 1;
				steps[i/2] = step;
				//modify limits to grid or minor ticks if desired
				boolean force2grid = hasFlag(i==0 ? X_FORCE2GRID : Y_FORCE2GRID) && !ignoreForce2Grid;
				if (force2grid) {
					int i1 = (int)Math.floor(Math.min(currentMinMax[i],currentMinMax[i+1])/step+1.e-10);
					int i2 = (int)Math.ceil (Math.max(currentMinMax[i],currentMinMax[i+1])/step-1.e-10);
					if (currentMinMax[i+1] > currentMinMax[i]) {	// care about inverted axes with max<min
						currentMinMax[i] = i1 * step;
						currentMinMax[i+1] = i2 * step;
					} else {
						currentMinMax[i] = i2 * step;
						currentMinMax[i+1] = i1 * step;
					}
				//} else if (snapToMinorGrid) {
				//	double stepForSnap = niceNumber(0.15*step);
				//	if (!logXAxis || stepForSnap >= 0.999) {	//don't snap on log axis if minor ticks are not full decades
				//		currentMinMax[i] = stepForSnap * Math.round(currentMinMax[i]/stepForSnap);
				//		currentMinMax[i+1] = stepForSnap * Math.round(currentMinMax[i+1]/stepForSnap);
				//	}
				} else if (enlargeRange != null) {
					// Enlarge range slightly (only in y direction) so that markers at min, max are not hidden by the frame
					range = currentMinMax[i+1]-currentMinMax[i];
					double tmpMin = currentMinMax[i] - 0.015*range;
					if (enlargeRange[i] == USUALLY_ENLARGE)		  // 'weak' enlarging: dont traverse zero
						currentMinMax[i] = (tmpMin*currentMinMax[i] <= 0) ? 0 : tmpMin;
					else if (enlargeRange[i] == ALWAYS_ENLARGE)	  // always enlarge
						currentMinMax[i] = tmpMin;
					double tmpMax = currentMinMax[i+1] + 0.015*range;
					if (enlargeRange[i+1] == USUALLY_ENLARGE)
						currentMinMax[i+1] = (tmpMax*currentMinMax[i+1] <= 0) ? 0 : tmpMax;
					else if (enlargeRange[i+1] == ALWAYS_ENLARGE)
						currentMinMax[i+1] = tmpMax;
				}
			}
			if (i==0) {
				xMin = currentMinMax[i];
				xMax = currentMinMax[i+1];
				logXAxis = logAxis;
			} else {
				yMin = currentMinMax[i];
				yMax = currentMinMax[i+1];
				logYAxis = logAxis;
			}
			if (logAxis) {
				currentMinMax[i] = Math.pow(10, currentMinMax[i]);
				currentMinMax[i+1] = Math.pow(10, currentMinMax[i+1]);
			}
		}
		//snapToMinorGrid = false;
		enlargeRange = null;
		ignoreForce2Grid = false;

		// calculate what we need to convert the data to screen pixels
		xBasePxl = leftMargin;
		yBasePxl = topMargin + frameHeight;
		xScale = frameWidth/(xMax-xMin);
		if (!(xMax-xMin!=0.0))	  //if range==0 (all data the same), or NaN shift zero level so one can see the curve
			xBasePxl += sc(10);
		yScale = frameHeight/(yMax-yMin);
		if (!(yMax-yMin!=0.0))
			yBasePxl -= sc(10);
		//IJ.log("x,yScale="+(float)xScale+","+(float)yScale+" xMin,max="+(float)xMin+","+(float)xMax+" yMin.max="+(float)yMin+","+(float)yMax);

		drawAxesTicksGridNumbers(steps);
		return steps;
	}

	void getInitialMinAndMax() {
		int axisFlags = 0;
		if (Double.isNaN(defaultMinMax[0])) axisFlags |= X_RANGE;
		if (Double.isNaN(defaultMinMax[2])) axisFlags |= Y_RANGE;
		if (axisFlags != 0)
			defaultMinMax = getMinAndMax(false, axisFlags);
		setLimitsToDefaults(false);			//use the range values to start with, but don't draw yet
	}

	/** Gets the minimum and maximum values from the first XY_DATA or VECTOR plotObject or all such plotObjects;
	 *	axisFlags determine for which axis to calculate the min&max (X_RANGE for x axis, Y_RANGE for y axis);
	 *	for the other axes the limit is taken from defaultMinMax
	 *	Array elements returned are xMin, xMax, yMin, yMax. Also sets enlargeRange to tell which limits should be enlarged
	 *	beyond the minimum or maximum of the data */
	double[] getMinAndMax(boolean allObjects, int axisFlags) {
		//IJ.log("getMinAndMax flags="+axisFlags+" x0="+(float)defaultMinMax[0]+" y0="+defaultMinMax[2]);
		double[] allMinMax = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE};
		for (int i=0; i<allMinMax.length; i++)
			if (((axisFlags>>i/2) & 1)==0)	  //keep default min & max for this axis
				allMinMax[i] = defaultMinMax[i];
		enlargeRange = new int[allMinMax.length];
		for (PlotObject plotObject : allPlotObjects) {
			if (plotObject.type == PlotObject.XY_DATA || plotObject.type == PlotObject.ARROWS) {
				getMinAndMax(allMinMax, enlargeRange, plotObject, axisFlags);
				if (!allObjects) break;
			}
		}
		return allMinMax;
	}

	/** Gets the minimum and maximum values from an XY_DATA or VECTOR plotObject;
	 *	axisFlags determine for which axis (X_RANGE for x axis, Y_RANGE for y axis)
	 *	The minimum modifies allMinAndMax[0] (x), allMinAndMax[2] (y); the maximum modifies [1], [3].
	 *	Sets the enlargeRange variables */
	void getMinAndMax(double[] allMinAndMax, int[] enlargeRange, PlotObject plotObject, int axisFlags) {
		if (plotObject.type == PlotObject.XY_DATA) {
			if ((axisFlags & X_RANGE) != 0) {
				int suggestedEnlarge = 0;
				if (plotObject.shape==DOT || plotObject.yEValues != null) //these can't be seen if merging with the frame
					suggestedEnlarge = ALWAYS_ENLARGE;
				else if (plotObject.shape != LINE)
					suggestedEnlarge = USUALLY_ENLARGE;
				getMinAndMax(allMinAndMax, enlargeRange, suggestedEnlarge, 0, plotObject.xValues, plotObject.xEValues);
			}
			if ((axisFlags & Y_RANGE) != 0) {
				int suggestedEnlarge = 0;
				if (plotObject.shape==DOT || plotObject.xEValues != null) //these can't be seen if merging with the frame
					suggestedEnlarge = ALWAYS_ENLARGE;
				else if (plotObject.shape != LINE)
					suggestedEnlarge = USUALLY_ENLARGE;
				getMinAndMax(allMinAndMax, enlargeRange,  suggestedEnlarge, 2, plotObject.yValues, plotObject.yEValues);
			}
		} else if (plotObject.type == PlotObject.ARROWS) {
			if ((axisFlags & X_RANGE) != 0) {
				getMinAndMax(allMinAndMax, enlargeRange, ALWAYS_ENLARGE, 0, plotObject.xValues, null);
				getMinAndMax(allMinAndMax, enlargeRange, ALWAYS_ENLARGE, 0, plotObject.xEValues, null);
			}
			if ((axisFlags & Y_RANGE) != 0) {
				getMinAndMax(allMinAndMax, enlargeRange, ALWAYS_ENLARGE, 2, plotObject.yValues, null);
				getMinAndMax(allMinAndMax, enlargeRange, ALWAYS_ENLARGE, 2, plotObject.yEValues, null);
			}
		}
	}

	/** Gets the minimum and maximum values for a dataset, taking error bars (if not null) into account.
	 *	The minimum modifies allMinAndMax[axisIndex] the maximum modifies allMinAndMax[axisIndex+1]
	 *	If minAndMax is not null, it is extended to include the new min&max.
	 *	Also cares about whether the range should be enlarged to avoid hiding markers at the edges */
	void getMinAndMax(double[] allMinAndMax, int[] enlargeRange, int suggestedEnlarge,
			int axisIndex, float[] data, float[] errorBars) {
		int nMinEqual = 0, nMaxEqual = 0;
		for (int i=0; i<data.length; i++) {
			double v1 = data[i];
			double v2 = data[i];
			if (errorBars != null && i<errorBars.length) {
				v1 -= errorBars[i];
				v2 += errorBars[i];
			}
			if (v1 < allMinAndMax[axisIndex]) {
				allMinAndMax[axisIndex] = v1;
				nMinEqual = 1;
				enlargeRange[axisIndex] = suggestedEnlarge;
				if (suggestedEnlarge == 0 && i>0 && i<data.length-1) //for lines except at the end: also enlarge
					enlargeRange[axisIndex] = USUALLY_ENLARGE;
			} else if (v1 == allMinAndMax[axisIndex])
				nMinEqual++;
			if (v2 > allMinAndMax[axisIndex+1]) {
				allMinAndMax[axisIndex+1] = v2;
				nMaxEqual = 1;
				enlargeRange[axisIndex+1] = suggestedEnlarge;
				if (suggestedEnlarge == 0 && i>0 && i<data.length-1) //for lines except at the end: also enlarge
					enlargeRange[axisIndex+1] = USUALLY_ENLARGE;
			} else if (v2 == allMinAndMax[axisIndex+1])
				nMaxEqual++;
		}
		//lines with many points (>10%) at min or max?
		if (enlargeRange[axisIndex] == 0 && nMinEqual > 2 && nMinEqual*10 > data.length)
			enlargeRange[axisIndex] = USUALLY_ENLARGE;
		if (enlargeRange[axisIndex+1] == 0 && nMaxEqual > 2 && nMaxEqual*10 > data.length)
			enlargeRange[axisIndex+1] = USUALLY_ENLARGE;
		//all data at min or max?
		if (nMinEqual == data.length)
			enlargeRange[axisIndex] = ALWAYS_ENLARGE;
		if (nMaxEqual == data.length)
			enlargeRange[axisIndex+1] = ALWAYS_ENLARGE;
		//min or max set previously, but not with proper enlarge range?
		if (nMinEqual>0 && enlargeRange[axisIndex]<suggestedEnlarge)
			enlargeRange[axisIndex] = suggestedEnlarge;
		if (nMaxEqual>0 && enlargeRange[axisIndex+1]<suggestedEnlarge)
			enlargeRange[axisIndex+1] = suggestedEnlarge;
	}

	void saveMinMax() {
		System.arraycopy(currentMinMax, 0, savedMinMax, 0, currentMinMax.length);
	}

	/** Zooms to a range given in pixels and updates the image */
	void zoomToRect(Rectangle r) {
		saveMinMax();
		currentMinMax[0] = descaleX(r.x);
		currentMinMax[1] = descaleX(r.x + r.width);
		currentMinMax[2] = descaleY(r.y + r.height);
		currentMinMax[3] = descaleY(r.y);
		//snapToMinorGrid = true; //get nice bounds when zooming in
		updateImage();
	}

	/** Zooms in or out when the user clicks one of the overlay arrows at the axes.
	 *	Index numbers start with 0 at the 'down' arrow of the lower side of the x axis
	 *	and end with the up arrow at the upper side of the y axis. */
	void zoomOnRangeArrow(int arrowIndex) {
		int axisIndex = (arrowIndex / 4) * 2;  //0 for x, 2 for y
		double min = axisIndex==0 ? xMin : yMin;
		double max = axisIndex==0 ? xMax : yMax;
		double range = max - min;
		boolean isMin = (arrowIndex % 4) < 2;
		boolean shrinkRange = arrowIndex % 4 == 1 || arrowIndex % 4 == 2;
		double factor = Math.sqrt(2);
		if (shrinkRange) factor = 1.0/factor;
		if (isMin)
			min = max - range*factor;
		else
			max = min + range*factor;
		boolean logAxis = axisIndex==0 ? logXAxis : logYAxis;
		if (logAxis) {
			min = Math.pow(10, min);
			max = Math.pow(10, max);
		}
		currentMinMax[axisIndex] = min;
		currentMinMax[axisIndex+1] = max;
		updateImage();
	}

	/** Zooms in or out on a point x, y in screen coordinates. If x>0, default in both directions,
	 *	if the cursor is below the x axis, only in x direction, if the cursor is left of the y axis, only in y direction.
	 *	If x < 0, zooms on center; if x == ZOOM_AS_PREVIOUS, zooms on the center of the previous zoom
	 *	operation */

	void zoom(int x, int y, double zoomFactor) {
		boolean zoomIn = zoomFactor > 1.0;
		boolean zoomAsPrevious = x==ZOOM_AS_PREVIOUS && (!Double.isNaN(previousXZoom) || !Double.isNaN(previousYZoom));
		if (!zoomAsPrevious) {
			previousXZoom = Double.NaN;
			previousYZoom = Double.NaN;
			saveMinMax();
		}
		boolean cursorLeft = x >= 0 && x<leftMargin-1;
		boolean cursorBottom = y>topMargin+frameHeight+1;
		boolean zoomX = (!cursorLeft && !zoomAsPrevious) || (!Double.isNaN(previousXZoom) && zoomAsPrevious);
		boolean zoomY = cursorLeft || !cursorBottom || (!Double.isNaN(previousYZoom) && zoomAsPrevious);
		if (cursorLeft && cursorBottom) // if cursor is in bottom left corner, zoom in y as if cursor was outside
			x = -1;
		//IJ.log("x,y="+x+","+y+" zx="+zoomX+" zy="+zoomY+" zPrev="+zoomAsPrevious);
		for (int axisIndex = 0; axisIndex<currentMinMax.length; axisIndex+=2) {
			if (axisIndex==0 && !zoomX) continue;
			if (axisIndex==2 && !zoomY) continue;
			boolean logAxis = axisIndex==0 ? logXAxis : logYAxis;
			double min = axisIndex==0 ? xMin : yMin;
			double max = axisIndex==0 ? xMax : yMax;
			double mid = 0.5 * (min + max);
			if (zoomAsPrevious) {
				mid = axisIndex==0 ? previousXZoom : previousYZoom;
				if (logAxis) mid = Math.log10(mid);
			}
			double span = max - min;
			if (x >= 0) { //cursor inside? zoom on cursor
				mid = axisIndex==0 ? descaleX(x) : descaleY(y);
				if (logAxis) mid = Math.log10(mid);
			}
			if (axisIndex==0)
				previousXZoom = logAxis ? Math.pow(10, mid) : mid;
			else
				previousYZoom = logAxis ? Math.pow(10, mid) : mid;
			//IJ.log("d="+(axisIndex==0 ? "X":"Y")+" x,y="+x+","+y+" mid="+(float)mid);
			double newHalfSpan = 0.5 * span / zoomFactor;
			currentMinMax[axisIndex] = mid - newHalfSpan;
			currentMinMax[axisIndex+1] = mid + newHalfSpan;
			if (logAxis) {
				currentMinMax[axisIndex] = Math.pow(10, currentMinMax[axisIndex]);
				currentMinMax[axisIndex+1] = Math.pow(10, currentMinMax[axisIndex+1]);
			}
		}
		updateImage();
	}

	/** Moves the plot range by a given number of pixels and updates the image */
	void scroll(int dx, int dy) {
		if (logXAxis) {
			currentMinMax[0] /= Math.pow(10, dx/xScale);
			currentMinMax[1] /= Math.pow(10, dx/xScale);
		} else {
			currentMinMax[0] -= dx/xScale;
			currentMinMax[1] -= dx/xScale;
		}
		if (logYAxis) {
			currentMinMax[2] *= Math.pow(10, dy/yScale);
			currentMinMax[3] *= Math.pow(10, dy/yScale);
		} else {
			currentMinMax[2] += dy/yScale;
			currentMinMax[3] += dy/yScale;
		}
		updateImage();		
	}

	/** Whether to draw simple axes without ticks, grid and numbers only for min, max*/
	private boolean simpleXAxis() {
		return !hasFlag(X_TICKS | X_MINOR_TICKS | X_LOG_TICKS | X_GRID | X_NUMBERS);
	}

	private boolean simpleYAxis() {
		return !hasFlag(Y_TICKS | Y_MINOR_TICKS | Y_LOG_TICKS | Y_GRID | Y_NUMBERS);
	}

	/** Draws ticks, grid and axis label for each tick/grid line.
	 *	The grid or major tick spacing in each direction is given by steps */
	void drawAxesTicksGridNumbers(double[] steps) {
		Font scFont = scFont(currentFont==null ? defaultFont : currentFont);
		Font scFontMedium = scFont.deriveFont(scFont.getSize2D()*10f/12f); //for y-axis numbers if full size does not fit
		Font scFontSmall = scFont.deriveFont(scFont.getSize2D()*9f/12f);   //for subscripts
		int extraWidth = scFont.getSize()/3; //right-justified font has extra space at the right
		ip.setFont(scFont);
		FontMetrics fm = ip.getFontMetrics();
		int fontAscent = fm.getAscent();
		ip.setJustification(LEFT);
		int yOfXAxisNumbers = topMargin + frameHeight + fm.getHeight()*5/4 + sc(3);
		// ---	A l o n g	X	A x i s
		if (hasFlag(X_NUMBERS | (logXAxis ? (X_TICKS | X_MINOR_TICKS) : X_LOG_TICKS) + X_GRID)) {
			Font baseFont = scFont;
			boolean majorTicks = logXAxis ? hasFlag(X_LOG_TICKS) : hasFlag(X_TICKS);
			boolean minorTicks = hasFlag(X_MINOR_TICKS);
			double step = steps[0];
			int i1 = (int)Math.ceil (Math.min(xMin, xMax)/step-1.e-10);
			int i2 = (int)Math.floor(Math.max(xMin, xMax)/step+1.e-10);
			int digits = getDigits(xMin, xMax, step, 7);
			int y1 = topMargin;
			int y2 = topMargin + frameHeight;
			if (xMin==xMax) {
				if (hasFlag(X_NUMBERS)) {
					String s = IJ.d2s(xMin,getDigits(xMin, 0.001*xMin, 5));
					int y = yBasePxl;
					ip.drawString(s, xBasePxl-ip.getStringWidth(s)/2, yOfXAxisNumbers);
				}
			} else {
				if (hasFlag(X_NUMBERS)) {
					int w1 = ip.getStringWidth(IJ.d2s(currentMinMax[0], logXAxis ? -1 : digits));
					int w2 = ip.getStringWidth(IJ.d2s(currentMinMax[1], logXAxis ? -1 : digits));
					int wMax = Math.max(w1,w2);
					if (wMax > Math.abs(step*xScale)-sc(8)) {
						baseFont = scFontMedium;   //small font if there is not enough space for the numbers
						ip.setFont(baseFont);
					}
				}

				for (int i=0; i<=(i2-i1); i++) {
					double v = (i+i1)*step;
					int x = (int)Math.round((v - xMin)*xScale) + leftMargin;
					if (hasFlag(X_GRID)) {
						ip.setColor(gridColor);
						ip.drawLine(x, y1, x, y2);
						ip.setColor(Color.black);
					}
					if (majorTicks) {
						ip.drawLine(x, y1, x, y1+sc(tickLength));
						ip.drawLine(x, y2, x, y2-sc(tickLength));
					}
					if (hasFlag(X_NUMBERS)) {
						if (logXAxis || digits<0) {
							drawExpString(logXAxis ? Math.pow(10,v) : v, logXAxis ? -1 : -digits,
									x, yOfXAxisNumbers-fontAscent/2, CENTER, fontAscent, baseFont, scFontSmall);
						} else {
							String s = IJ.d2s(v,digits);
							ip.drawString(s, x-ip.getStringWidth(s)/2, yOfXAxisNumbers);
						}
					}
				}
				boolean haveMinorLogNumbers = i2-i1 < 2;		//nunbers on log minor ticks only if < 2 decades
				if (minorTicks	&& (!logXAxis || step > 1.1)) { //'standard' log minor ticks only for full decades
					step = niceNumber(step*0.19);				//non-log: 4 or 5 minor ticks per major tick
					if (logXAxis && step < 1) step = 1;
					i1 = (int)Math.ceil (Math.min(xMin,xMax)/step-1.e-10);
					i2 = (int)Math.floor(Math.max(xMin,xMax)/step+1.e-10);
					for (int i=i1; i<=i2; i++) {
						double v = i*step;
						int x = (int)Math.round((v - xMin)*xScale) + leftMargin;
						ip.drawLine(x, y1, x, y1+sc(minorTickLength));
						ip.drawLine(x, y2, x, y2-sc(minorTickLength));
					}
				} else if (logXAxis && majorTicks && Math.abs(xScale)>sc(MIN_X_GRIDSPACING)) {		//minor ticks for log
					int minorNumberLimit = haveMinorLogNumbers ? (int)(0.12*Math.abs(xScale)/(fm.charWidth('0')+sc(2))) : 0;   //more numbers on minor ticks when zoomed in
					i1 = (int)Math.floor(Math.min(xMin,xMax)-1.e-10);
					i2 = (int)Math.ceil (Math.max(xMin,xMax)+1.e-10);
					for (int i=i1; i<=i2; i++) {
						for (int m=2; m<10; m++) {
							double v = i+Math.log10(m);
							if (v > Math.min(xMin,xMax) && v < Math.max(xMin,xMax)) {
								int x = (int)Math.round((v - xMin)*xScale) + leftMargin;
								ip.drawLine(x, y1, x, y1+sc(minorTickLength));
								ip.drawLine(x, y2, x, y2-sc(minorTickLength));
								if (m<=minorNumberLimit)
									drawExpString(Math.pow(10,v), 0, x, yOfXAxisNumbers-fontAscent/2, CENTER, fontAscent, baseFont, scFontSmall);
							}	  
						}
					}
				}
			}
		}
		// ---	A l o n g	Y	A x i s
		int maxNumWidth = 0;
		if (hasFlag(Y_NUMBERS | (logYAxis ? (Y_TICKS | Y_MINOR_TICKS) : Y_LOG_TICKS) + Y_GRID)) {
			ip.setFont(scFont);
			ip.setJustification(RIGHT);
			Font baseFont = scFont;
			boolean majorTicks = logYAxis ? hasFlag(Y_LOG_TICKS) : hasFlag(Y_TICKS);
			boolean minorTicks = logYAxis ? hasFlag(Y_LOG_TICKS) : hasFlag(Y_MINOR_TICKS);
			double step = steps[1];
			int i1 = (int)Math.ceil (Math.min(yMin, yMax)/step-1.e-10);
			int i2 = (int)Math.floor(Math.max(yMin, yMax)/step+1.e-10);
			int digits = getDigits(yMin, yMax, step, 5);
			int x1 = leftMargin;
			int x2 = leftMargin + frameWidth;
			if (yMin==yMax) {
				if (hasFlag(Y_NUMBERS)) {
					String s = IJ.d2s(yMin,getDigits(yMin, 0.001*yMin, 5));
					maxNumWidth = ip.getStringWidth(s);
					int y = yBasePxl;
					ip.drawString(s, leftMargin-1, y+fontAscent/2+sc(1));
				}
			} else {
				int w1 = ip.getStringWidth(IJ.d2s(currentMinMax[2], logYAxis ? -1 : digits));
				int w2 = ip.getStringWidth(IJ.d2s(currentMinMax[3], logYAxis ? -1 : digits));
				int wMax = Math.max(w1,w2);
				if (hasFlag(Y_NUMBERS)) {
					if (wMax > leftMargin-sc(2)-extraWidth - (yLabel.length()>0 ? fm.getHeight() : 0)) {
						baseFont = scFontMedium;   //small font if there is not enough space for the numbers
						ip.setFont(baseFont);
					}
				}
				//IJ.log(IJ.d2s(currentMinMax[2],digits)+": w="+w1+"; "+IJ.d2s(currentMinMax[3],digits)+": w="+w2+baseFont+" Space="+(leftMargin-sc(4+5)-fm.getHeight()));
				for (int i=i1; i<=i2; i++) {
					double v = step==0 ? yMin : i*step;
					int y = topMargin + frameHeight - (int)Math.round((v - yMin)*yScale);
					if (hasFlag(Y_GRID)) {
						ip.setColor(gridColor);
						ip.drawLine(x1, y, x2, y);
						ip.setColor(Color.black);
					}
					if (majorTicks) {
						ip.drawLine(x1, y, x1+sc(tickLength), y);
						ip.drawLine(x2, y, x2-sc(tickLength), y);
					}
					if (hasFlag(Y_NUMBERS)) {
						int w = 0;
						if (logYAxis || digits<0) {
							w = drawExpString(logYAxis ? Math.pow(10,v) : v, logYAxis ? -1 : -digits,
									leftMargin, y, RIGHT, fontAscent, baseFont, scFontSmall);
						} else {
							String s = IJ.d2s(v,digits);
							w = ip.getStringWidth(s);
							ip.drawString(s, leftMargin-1, y+fontAscent*2/3+1);
						}
						if (w > maxNumWidth) maxNumWidth = w;
					}
				}
			}
			boolean haveMinorLogNumbers = i2-i1 < 2;		//nunbers on log minor ticks only if < 2 decades
			if (minorTicks	&& (!logYAxis || step > 1.1)) { //'standard' log minor ticks only for full decades
				step = niceNumber(step*0.19);				//non-log: 4 or 5 minor ticks per major tick
				if (logYAxis && step < 1) step = 1;
				i1 = (int)Math.ceil (Math.min(yMin,yMax)/step-1.e-10);
				i2 = (int)Math.floor(Math.max(yMin,yMax)/step+1.e-10);
				for (int i=i1; i<=i2; i++) {
					double v = i*step;
					int y = topMargin + frameHeight - (int)Math.round((v - yMin)*yScale);
					ip.drawLine(x1, y, x1+sc(minorTickLength), y);
					ip.drawLine(x2, y, x2-sc(minorTickLength), y);
				}
			}
			if (logYAxis && majorTicks && Math.abs(yScale)>sc(MIN_X_GRIDSPACING)) {		 //minor ticks for log within the decade
				int minorNumberLimit = haveMinorLogNumbers ? (int)(0.4*Math.abs(yScale)/fm.getHeight()) : 0;	//more numbers on minor ticks when zoomed in
				i1 = (int)Math.floor(Math.min(yMin,yMax)-1.e-10);
				i2 = (int)Math.ceil(Math.max(yMin,yMax)+1.e-10);
				for (int i=i1; i<=i2; i++) {
					for (int m=2; m<10; m++) {
						double v = i+Math.log10(m);
						if (v > Math.min(yMin,yMax) && v < Math.max(yMin,yMax)) {
							int y = topMargin + frameHeight - (int)Math.round((v - yMin)*yScale);
							ip.drawLine(x1, y, x1+sc(minorTickLength), y);
							ip.drawLine(x2, y, x2-sc(minorTickLength), y);
							if (m<=minorNumberLimit) {
								int w = drawExpString(Math.pow(10,v), 0, leftMargin-sc(1), y, RIGHT, fontAscent, baseFont, scFontSmall);
								if (w > maxNumWidth) maxNumWidth = w;
							}
						}	  
					}
				}
			}
		}
		// --- Write min&max of range if simple style without any axis format flags
		ip.setFont(scFont);
		ip.setJustification(LEFT);
		String xLabelToDraw = xLabel;
		String yLabelToDraw = yLabel;
		if (simpleYAxis()) { // y-axis min&max
			int digits = getDigits(yMin, yMax, 0.001*(yMax-yMin), 6);
			String s = IJ.d2s(yMax, digits);
			int sw = ip.getStringWidth(s);
			if ((sw+sc(4)) > leftMargin)
				ip.drawString(s, sc(4), topMargin-sc(4));
			else
				ip.drawString(s, leftMargin-ip.getStringWidth(s)-sc(4), topMargin+10);
			s = IJ.d2s(yMin, digits);
			sw = ip.getStringWidth(s);
			if ((sw+4)>leftMargin)
				ip.drawString(s, sc(4), topMargin+frame.height);
			else
				ip.drawString(s, leftMargin-ip.getStringWidth(s)-sc(4), topMargin+frame.height);
			if (logYAxis) yLabelToDraw += " (LOG)";
		}
		int y = yOfXAxisNumbers;
		if (simpleXAxis()) { // x-axis min&max
			int digits = getDigits(xMin, xMax, 0.001*(xMax-xMin), 7);
			ip.drawString(IJ.d2s(xMin,digits), leftMargin, y);
			String s = IJ.d2s(xMax,digits);
			ip.drawString(s, leftMargin + frame.width-ip.getStringWidth(s)+6, y);
			y -= fm.getHeight();
			if (logXAxis) xLabelToDraw += " (LOG)";
		} else
			y += sc(1);
		// --- Write x and y axis text labels
		ip.setFont(xLabelFont == null ? scFont : scFont(xLabelFont));
		ip.drawString(xLabelToDraw, leftMargin+(frame.width-ip.getStringWidth(xLabel))/2, y+ip.getFontMetrics().getHeight());
		if (yLabel.length() > 0) {
			int xOfYLabel = leftMargin-maxNumWidth-extraWidth-sc(4);
			if (xOfYLabel < 0) xOfYLabel = 0;
			drawYLabel(yLabelToDraw, xOfYLabel, topMargin, frame.height, yLabelFont == null ? scFont : scFont(yLabelFont));
		}
	}

	/** Returns the smallest "nice" number >= v. "Nice" numbers are .. 0.5, 1, 2, 5, 10, 20 ... */
	double niceNumber(double v) {
		double base = Math.pow(10,Math.floor(Math.log10(v)-1.e-6));
		if (v > 5.0000001*base) return 10*base;
		else if (v > 2.0000001*base) return 5*base;
		else return 2*base;
	}

	/** draw something like 1.2 10^-9; returns the width of the string drawn.
	 *	'Digits' should be >=0 for drawing the mantissa (=1.38 in this example), negative to draw only 10^exponent
	 *	Currently only supports center justification and right justification (y of center line)
	 *	Fonts baseFont, smallFont should be scaled already*/
	int drawExpString(double value, int digits, int x, int y, int justification, int fontAscent, Font baseFont, Font smallFont) {
		String base = "10";
		String exponent = null;
		String s = IJ.d2s(value, digits<=0 ? -1 : -digits);
		if (Tools.parseDouble(s) == 0) s = "0"; //don't write 0 as 0*10^0
		int ePos = s.indexOf('E');
		if (ePos < 0)
			base = s;	//can't have exponential format, e.g. NaN
		else {
			if (digits>=0) {
				base = s.substring(0,ePos);
				if (digits == 0)
					base = Integer.toString((int)Math.round(Tools.parseDouble(base)));
				base += "\u00B710"; //middot as multiplication symbol ".10"
			}
			exponent = s.substring(ePos+1);
		}
		//IJ.log(s+" -> "+base+"^"+exponent+"  maxAsc="+fontAscent+" font="+baseFont);
		ip.setJustification(RIGHT);
		int width = ip.getStringWidth(base);
		if (exponent != null) {
			ip.setFont(smallFont);
			int wExponent = ip.getStringWidth(exponent);
			width += wExponent;
			if (justification == CENTER) x += width/2;
			ip.drawString(exponent, x, y+fontAscent*3/10);
			x -= wExponent;
			ip.setFont(baseFont);
		}
		ip.drawString(base, x, y+fontAscent*7/10);
		return width;
	}

	// Number of digits to display the number n with resolution 'resolution';
	// (if n is integer and small enough to display without scientific notation,
	// no decimals are needed, irrespective of 'resolution')
	// Scientific notation is used for more than 'maxDigits' (must be >=3), and indicated
	// by a negative return value
	static int getDigits(double n, double resolution, int maxDigits) {
		if (n==Math.round(n) && Math.abs(n) < Math.pow(10,maxDigits-1)-1) //integers and not too big
			return 0;
		else
			return getDigits2(n, resolution, maxDigits);
	}

	// Number of digits to display the range between n1 and n2 with resolution 'resolution';
	// Scientific notation is used for more than 'maxDigits' (must be >=3), and indicated
	// by a negative return value
	static int getDigits(double n1, double n2, double resolution, int maxDigits) {
		if (n1==0 && n2==0) return 0;
		return getDigits2(Math.max(Math.abs(n1),Math.abs(n2)), resolution, maxDigits);
	}

	static int getDigits2(double n, double resolution, int maxDigits) {
		int log10ofN = (int)Math.floor(Math.log10(Math.abs(n))+1e-7);
		int digits = resolution != 0 ?
				-(int)Math.floor(Math.log10(Math.abs(resolution))+1e-7) : 
				Math.max(0, -log10ofN+maxDigits-2);
		int sciDigits = -Math.max((log10ofN+digits),1);
		//IJ.log("n="+(float)n+"digitsRaw="+digits+" log10ofN="+log10ofN+" sciDigits="+sciDigits);
		if (digits < -2 && log10ofN >= maxDigits)
			digits = sciDigits; //scientific notation for large numbers
		else if (digits < 0)
			digits = 0;
		else if (digits > maxDigits-1 && log10ofN < -2)
			digits = sciDigits; // scientific notation for small numbers
		return digits;
	}

	static boolean isInteger(double n) {
		return n==Math.round(n);
	}

	private void drawPlotObject(PlotObject plotObject, ImageProcessor ip) {
		//IJ.log("DRAWING type="+plotObject.type+" lineWidth="+plotObject.lineWidth+" shape="+plotObject.shape);
		ip.setColor(plotObject.color);
		ip.setLineWidth(sc(plotObject.lineWidth));
		int type = plotObject.type;
		switch (type) {
			case PlotObject.XY_DATA:
				ip.setClipRect(frame);
				if (plotObject.yEValues != null)
					drawVerticalErrorBars(plotObject.xValues, plotObject.yValues, plotObject.yEValues);
				if (plotObject.xEValues != null)
					drawHorizontalErrorBars(plotObject.xValues, plotObject.yValues, plotObject.xEValues);
				boolean drawMarker = plotObject.hasMarker();
				boolean drawLine = plotObject.hasCurve();
				if (plotObject.shape == CONNECTED_CIRCLES)
					ip.setColor(plotObject.color2 == null ? Color.black : plotObject.color2);
				if (drawLine)
					//draw line
					drawFloatPolyline(ip, plotObject.xValues, plotObject.yValues, 
							Math.min(plotObject.xValues.length, plotObject.yValues.length));
				if (drawMarker) {
					int markSize = plotObject.getMarkerSize();
					if (plotObject.hasFilledMarker()) {
						//fill markers with secondary color
						ip.setColor(plotObject.color2);
						ip.setLineWidth(1);
						for (int i=0; i<Math.min(plotObject.xValues.length, plotObject.yValues.length); i++)
							if ((!logXAxis || plotObject.xValues[i]>0) && (!logYAxis || plotObject.yValues[i]>0))
								fillShape(plotObject.shape, scaleX(plotObject.xValues[i]), scaleY(plotObject.yValues[i]), markSize);
						ip.setLineWidth(sc(plotObject.lineWidth));
					}
					// draw markers
					ip.setColor(plotObject.color);
					for (int i=0; i<Math.min(plotObject.xValues.length, plotObject.yValues.length); i++)
						if ((!logXAxis || plotObject.xValues[i]>0) && (!logYAxis || plotObject.yValues[i]>0))
							drawShape(plotObject.shape, scaleX(plotObject.xValues[i]), scaleY(plotObject.yValues[i]), markSize);
				}
				ip.setClipRect(null);
				break;
			case PlotObject.ARROWS:
				ip.setClipRect(frame);
				for (int i=0; i<plotObject.xValues.length; i++) {
					int xt1 = scaleX(plotObject.xValues[i]);
					int yt1 = scaleY(plotObject.yValues[i]);
					int xt2 = scaleX(plotObject.xEValues[i]);
					int yt2 = scaleY(plotObject.yEValues[i]);
					double dist = calculateDistance(xt1, yt1, xt2, yt2);
					if (xt1==xt2 && yt1==yt2)
						ip.drawDot(xt1, yt1);
					else if (dist < sc(1.5f*MIN_ARROWHEAD_LENGTH))
						ip.drawLine(xt1, yt1, xt2, yt2);
					else {
						int arrowHeadLength = (int)(dist*RELATIVE_ARROWHEAD_SIZE+0.5);
						if (arrowHeadLength > sc(MAX_ARROWHEAD_LENGTH)) arrowHeadLength = sc(MAX_ARROWHEAD_LENGTH);
						if (arrowHeadLength < sc(MIN_ARROWHEAD_LENGTH)) arrowHeadLength = sc(MIN_ARROWHEAD_LENGTH);
						drawArrow(xt1, yt1, xt2, yt2, arrowHeadLength);
					}
				}
				ip.setClipRect(null);
				break;
			case PlotObject.LINE:
				ip.setClipRect(frame);
				ip.drawLine(scaleX(plotObject.x), scaleY(plotObject.y), scaleX(plotObject.xEnd), scaleY(plotObject.yEnd));
				ip.setClipRect(null);
				break;
			case PlotObject.NORMALIZED_LINE:
				ip.setClipRect(frame);
				int ix1 = leftMargin + (int)(plotObject.x*frameWidth);
				int iy1 = topMargin	 + (int)(plotObject.y*frameHeight);
				int ix2 = leftMargin + (int)(plotObject.xEnd*frameWidth);
				int iy2 = topMargin	 + (int)(plotObject.yEnd*frameHeight);
				ip.drawLine(ix1, iy1, ix2, iy2);
				ip.setClipRect(null);
				break;
			case PlotObject.DOTTED_LINE:
				ip.setClipRect(frame);
				ix1 = scaleX(plotObject.x);
				iy1 = scaleY(plotObject.y);
				ix2 = scaleX(plotObject.xEnd);
				iy2 = scaleY(plotObject.yEnd);
				double length = calculateDistance(ix1, ix2, iy1, iy2) + 0.1;
				int n = (int)(length/plotObject.step);
				for (int i = 0; i<=n; i++)
					ip.drawDot(ix1 + (int)Math.round((ix2-ix1)*(double)i/n), iy1 + (int)Math.round((iy2-iy1)*(double)i/n));
				ip.setClipRect(null);
				break;
			case PlotObject.LABEL:
			case PlotObject.NORMALIZED_LABEL:
				ip.setJustification(plotObject.justification);
				if (plotObject.font != null)
					ip.setFont(scFont(plotObject.font));
				int xt = type==PlotObject.LABEL ? scaleX(plotObject.x) : leftMargin + (int)(plotObject.x*frameWidth);
				int yt = type==PlotObject.LABEL ? scaleY(plotObject.y) : topMargin + (int)(plotObject.y*frameHeight);
				ip.drawString(plotObject.label, xt, yt);
				break;
			case PlotObject.LEGEND:
				drawLegend(plotObject, ip);
				break;
		}
	}

	/** Draw the symbols for data points */
	void drawShape(int shape, int x, int y, int size) {
		int xbase = x-sc(size/2);
		int ybase = y-sc(size/2);
		int xend = x+sc(size/2);
		int yend = y+sc(size/2);
		switch(shape) {
			case X:
				ip.drawLine(xbase,ybase,xend,yend);
				ip.drawLine(xend,ybase,xbase,yend);
				break;
			case BOX:
				ip.drawLine(xbase,ybase,xend,ybase);
				ip.drawLine(xend,ybase,xend,yend);
				ip.drawLine(xend,yend,xbase,yend);
				ip.drawLine(xbase,yend,xbase,ybase);
				break;
			case TRIANGLE:
				ip.drawLine(x,ybase-sc(1),xend+sc(1),yend); //height must be odd, otherwise rounding leads to asymmetric shape
				ip.drawLine(x,ybase-sc(1),xbase-sc(1),yend);
				ip.drawLine(xend+sc(1),yend,xbase-sc(1),yend);
				break;
			case CROSS:
				ip.drawLine(xbase,y,xend,y);
				ip.drawLine(x,ybase,x,yend);
				break;
			case DOT:
				ip.drawDot(x, y); //uses current line width
				break;
			default: // CIRCLE, CONNECTED_CIRCLES: 5x5 oval approximated by 5x5 square without corners
				if (sc(size) < 5.01) {
					ip.drawLine(x-1, y-2, x+1, y-2);
					ip.drawLine(x-1, y+2, x+1, y+2);
					ip.drawLine(x+2, y+1, x+2, y-1);
					ip.drawLine(x-2, y+1, x-2, y-1);
				} else {
					int r = sc(0.5f*size-0.5f);
					ip.drawOval(x-r, y-r, 2*r, 2*r);
				}
				break;
		}
	}

	/** Fill the area of the symbols for data points (except for shape=DOT)
	 *	Note that ip.fill, ip.fillOval etc. can't be used here: they do not care about the clip rectangle */
	void fillShape(int shape, int x0, int y0, int size) {
		int r = sc(size/2)-1;
		switch(shape) {
			case BOX:
				int widthOrHeight = 2*sc(size/2);
				for (int dy=-r; dy<=r; dy++)
					for (int dx=-r; dx<=r; dx++)
						ip.drawDot(x0+dx, y0+dy);
				break;
			case TRIANGLE:
				int ybase = y0 - r - sc(1);
				int yend = y0 + r;
				double halfWidth = sc(size/2)+sc(1)-1;
				double hwStep = halfWidth/(yend-ybase+1);
				for (int y=yend; y>=ybase; y--, halfWidth -= hwStep) {
					int dx = (int)(Math.round(halfWidth));
					for (int x=x0-dx; x<=x0+dx; x++)
						ip.drawDot(x,y);
				}
				break;
			case CIRCLE:
				int rsquare = (r+1)*(r+1);
				for (int dy=-r; dy<=r; dy++)
					for (int dx=-r; dx<=r; dx++)
						if (dx*dx + dy*dy <= rsquare)
							ip.drawDot(x0+dx, y0+dy);
				break;
		}
	}

	/** Adds an arrow from position 1 to 2 given in pixels; 'size' is the length of the arrowhead
	 *	WARNING: Use as a public method is not supported any more because it is incompatible with rescaling */
	public void drawArrow(int x1, int y1, int x2, int y2, double size) {
		double dx = x2 - x1;
		double dy = y2 - y1;
		double ra = Math.sqrt(dx * dx + dy * dy);
		dx /= ra;
		dy /= ra;
		int x3 = (int) Math.round(x2 - dx * size);	//arrow base
		int y3 = (int) Math.round(y2 - dy * size);
		double r = 0.3 * size;
		int x4 = (int) Math.round(x3 + dy * r);
		int y4 = (int) Math.round(y3 - dx * r);
		int x5 = (int) Math.round(x3 - dy * r);
		int y5 = (int) Math.round(y3 + dx * r);
		ip.moveTo(x1, y1); ip.lineTo(x2, y2);
		ip.moveTo(x4, y4); ip.lineTo(x2, y2); ip.lineTo(x5, y5);
	}	

	private void drawVerticalErrorBars(float[] x, float[] y, float[] e) {
		int nPoints = Math.min(Math.min(x.length, y.length), e.length);
		for (int i=0; i<nPoints; i++) {
			if (Float.isNaN(x[i]) || Float.isNaN(y[i]) || (logXAxis && !(x[i] >0))) continue;
			int x0 = scaleX(x[i]);
			int yPlus = scaleYWithOverflow(y[i] + e[i]);
			int yMinus = scaleYWithOverflow(y[i] - e[i]);
			ip.moveTo(x0,yMinus);
			ip.lineTo(x0, yPlus);
		}
	}

	private void drawHorizontalErrorBars(float[] x, float[] y, float[] e) {
		int nPoints = Math.min(Math.min(x.length, y.length), e.length);
		float[] xpoints = new float[2];
		float[] ypoints = new float[2];
		for (int i=0; i<nPoints; i++) {
			if (Float.isNaN(x[i]) || Float.isNaN(y[i]) || (logXAxis && !(y[i] >0))) continue;
			int y0 = scaleY(y[i]);
			int xPlus = scaleXWithOverflow(x[i] + e[i]);
			int xMinus = scaleXWithOverflow(x[i] - e[i]);
			ip.moveTo(xMinus,y0);
			ip.lineTo(xPlus, y0);
		}		
	}

	/** Draw a polygon line; NaN values interrupt it. For reasonable performance, line segments that
	 *	are definitely completely out of the plot range are no drawn, otherwise a plot of 1000 points
	 *	may need many tens of seconds when zoomed in to e.g. 10-6 of the full range */
	void drawFloatPolyline(ImageProcessor ip, float[] x, float[] y, int n) {
		if (x==null || x.length==0) return;
		int x1, y1;
		boolean isNaN1;
		int x2 = scaleX(x[0]);
		int y2 = scaleY(y[0]);
		boolean isNaN2 = Float.isNaN(x[0]) || Float.isNaN(y[0]) || (logXAxis && x[0]<=0) || (logYAxis && y[0]<=0);
		for (int i=1; i<n; i++) {
			x1 = x2;
			y1 = y2;
			isNaN1 = isNaN2;
			x2 = scaleX(x[i]);
			y2 = scaleY(y[i]);
			isNaN2 = Float.isNaN(x[i]) || Float.isNaN(y[i]) || (logXAxis && x[i]<=0) || (logYAxis && y[i]<=0);
			if (!isNaN1 && !isNaN1)
				ip.drawLine(x1, y1, x2, y2);
		}
	}

	/** Vertical text for y axis label */
	void drawYLabel(String yLabel, int xRight, int yFrameTop, int frameHeight, Font scaledFont) {
		if (yLabel.equals(""))
			return;
		ip.setFont(scaledFont);
		FontMetrics fm = ip.getFontMetrics();
		int w =	 ip.getStringWidth(yLabel) + sc(5);
		int h =	 fm.getHeight()+sc(1);
		ImageProcessor label = new ByteProcessor(w, h);
		label.setAntialiasedText(antialiasedText);
		if (invertedLut)
			label.invertLut();
		label.setColor(Color.white);
		label.fill();
		label.setColor(Color.black);
		label.setFont(scaledFont);
		label.drawString(yLabel, 0, h);
		label = label.rotateLeft();
		int y2 = yFrameTop + (frameHeight-ip.getStringWidth(yLabel))/2;
		if (y2 < yFrameTop) y2 = yFrameTop;
		int x2 = Math.max(xRight-h, 0);
		ip.insert(label, x2, y2);
	}

	/** Draw the legend */
	void drawLegend(PlotObject legendObject, ImageProcessor ip) {
		ip.setFont (scFont(legendObject.font));
		String[] labels = null;
		if (legendObject.label != null)
			labels = legendObject.label.split("[\t\n]");
		int n = 0;
		int nLabels = 0;
		int maxStringWidth = 0;
		float maxLineThickness = 0;
		for (PlotObject plotObject : allPlotObjects)
			if (plotObject.type == PlotObject.XY_DATA) {
				if (labels != null && n < labels.length && labels[n].length()>0)
					plotObject.label = labels[n];	// sets the label
				if (plotObject.label != null) {		//label exists: was set now or previously
					nLabels++;
					int w = ip.getStringWidth(plotObject.label);
					if (w > maxStringWidth) maxStringWidth = w;
					if (plotObject.lineWidth > maxLineThickness) maxLineThickness = plotObject.lineWidth;
				}
				n++;
			}
		if (nLabels == 0) return;
		if (antialiasedText && scale > 1)		//fix incorrect width of large fonts
			maxStringWidth = (int)((1 + 0.004*scale) * maxStringWidth);
		int frameThickness = sc(legendObject.lineWidth > 0 ? legendObject.lineWidth : 1);
		FontMetrics fm = ip.getFontMetrics();
		ip.setJustification(LEFT);
		int lineHeight = fm.getHeight();
		int height = nLabels*lineHeight + 2*sc(LEGEND_PADDING);
		int width = maxStringWidth + sc(3*LEGEND_PADDING + LEGEND_LINELENGTH + maxLineThickness);
		int positionCode = legendObject.flags & LEGEND_POSITION_MASK;
		if (positionCode == AUTO_POSITION)
			positionCode = autoLegendPosition(width, height, frameThickness);
		Rectangle rect = legendRect(positionCode, width, height, frameThickness);
		int x0 = rect.x;
		int y0 = rect.y;

		ip.setColor(Color.white);
		ip.setLineWidth(1);
		if (!legendObject.hasFlag(LEGEND_TRANSPARENT)) {
			ip.setRoi(x0, y0, width, height);
			ip.fill();
		} else if (hasFlag(X_GRID | Y_GRID)) {	//erase grid
			int grid = ip instanceof ColorProcessor ? (gridColor.getRGB() & 0xffffff) : ip.getBestIndex(gridColor);
			for (int y=y0; y<y0+height; y++)
				for (int x=x0; x<x0+width; x++)
					if ((ip.getPixel(x, y) & 0xffffff) == grid)
						ip.drawDot(x, y);
		}
		ip.setLineWidth(frameThickness);
		ip.setColor(legendObject.color);
		ip.drawRect(x0-frameThickness/2, y0-frameThickness/2, width+frameThickness, height);
		boolean bottomUp = legendObject.hasFlag(LEGEND_BOTTOM_UP);
		int y = y0 + frameThickness/2 + sc(LEGEND_PADDING) + lineHeight/2;
		if (bottomUp) y += (nLabels-1) * lineHeight;
		int xText = x0 + frameThickness/2 + sc(2f*LEGEND_PADDING + LEGEND_LINELENGTH + maxLineThickness);
		int xMarker = x0 + frameThickness/2 + sc(LEGEND_PADDING + 0.5f*(LEGEND_LINELENGTH + maxLineThickness));
		for (PlotObject plotObject : allPlotObjects)
			if (plotObject.type == PlotObject.XY_DATA && plotObject.label != null) {
				if (plotObject.hasFilledMarker()) {
					ip.setColor(plotObject.color2);
					fillShape(plotObject.shape, xMarker, y, plotObject.getMarkerSize());
				}
				int lineWidth = sc(plotObject.lineWidth);
				ip.setLineWidth(lineWidth);
				if (plotObject.hasMarker()) {
					ip.setColor(plotObject.color);
					drawShape(plotObject.shape, xMarker, y, plotObject.getMarkerSize());
				}
				if (plotObject.hasCurve()) {
					Color c = plotObject.shape == CONNECTED_CIRCLES ?
							(plotObject.color2 == null ? Color.black : plotObject.color2) :
							plotObject.color;
					ip.setColor(c);
					ip.drawLine(x0+frameThickness/2+sc(LEGEND_PADDING)+lineWidth, y, xText-sc(LEGEND_PADDING)-lineWidth, y);
				}
				ip.setColor(plotObject.color);
				ip.setLineWidth(frameThickness);
				ip.drawString(plotObject.label, xText, y+ lineHeight/2);
				y += bottomUp ? -lineHeight : lineHeight;
			}
	}

	/** The legend area; positionCode should be TOP_LEFT, TOP_RIGHT, etc. */
	Rectangle legendRect(int positionCode, int width, int height, int frameThickness)  {
		boolean leftPosition = positionCode == TOP_LEFT || positionCode == BOTTOM_LEFT;
		boolean topPosition	 = positionCode == TOP_LEFT || positionCode == TOP_RIGHT;
		int x0 = (leftPosition) ?
				leftMargin + sc(2*LEGEND_PADDING) + frameThickness/2 :
				leftMargin + frameWidth - width - sc(2*LEGEND_PADDING) - frameThickness/2;
		int y0 = (topPosition) ?
				topMargin + sc(LEGEND_PADDING) + frameThickness/2 :
				topMargin + frameHeight - height - sc(LEGEND_PADDING) + frameThickness/2;
		if (hasFlag(Y_TICKS))
			x0 += (leftPosition ? 1 : -1) * sc(tickLength - LEGEND_PADDING);
		if (hasFlag(X_TICKS))
			y0 += (topPosition ? 1 : -1) * sc(tickLength - LEGEND_PADDING/2);
		return new Rectangle(x0, y0, width, height);
	}

	/** The position code of the legend position where the smallest amount of foreground pixels is covered */
	int autoLegendPosition(int width, int height, int frameThickness) {
		int background = ip instanceof ColorProcessor ? (0xffffff) : (ip.isInvertedLut() ? 0 : 0xff);
		int grid = ip instanceof ColorProcessor ? (gridColor.getRGB() & 0xffffff) : ip.getBestIndex(gridColor);
		int bestPosition = 0;
		int minCoveredPixels = Integer.MAX_VALUE;
		for (int positionCode : new int[]{TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT}) {
			Rectangle rect = legendRect(positionCode, width, height, frameThickness);
			int coveredPixels = 0;
			for (int y = rect.y - frameThickness/2; y <= rect.y + rect.height + frameThickness/2; y++)
				for (int x = rect.x - frameThickness/2; x <= rect.x + rect.width + frameThickness/2; x++) {
					int pixel = ip.getPixel(x, y) & 0xffffff;
					if (pixel != background && pixel != grid)
						coveredPixels ++;
				}
			if (coveredPixels < minCoveredPixels) {
				minCoveredPixels = coveredPixels;
				bestPosition = positionCode;
			}
		}
		return bestPosition;
	}

	/** Returns the x, y coordinates at the cursor position or the nearest point as a String */
	String getCoordinates(int x, int y) {
		if (frame==null) return "";
		String text = "";
		if (!frame.contains(x, y))
			return text;
		double xv = descaleX(x); // cursor location
		double yv = descaleY(y);
		boolean yIsValue = false;
		if (!hasMultiplePlots()) {
			PlotObject p = getMainCurveObject(); // display x and f(x) instead of cursor y
			if (p != null) {
				double bestDx = Double.MAX_VALUE;
				double xBest = 0, yBest = 0;
				for (int i=0; i<Math.min(p.xValues.length, p.yValues.length); i++) {
					double xp = p.xValues[i];
					if (Math.abs(xp-xv) < bestDx) {
						bestDx = Math.abs(xp-xv);
						xBest = xp;
						yBest = p.yValues[i];
					}
				}
				if (xScale != 0 && bestDx*xScale < 50) {	//ignore points more than 50 pixels away in x
					xv = xBest;
					yv = yBest;
					yIsValue = true;
				} else
					xv = Double.NaN;
			}
		}
		if (!Double.isNaN(xv)) {
			text =	"X=" + IJ.d2s(xv, getDigits(xv, 0.001*(xMax-xMin), 6))+", Y";
			if (yIsValue) text += "(X)";
			text +="="+ IJ.d2s(yv, getDigits(yv, 0.001*(yMax-yMin), 6));
		}
		return text;
		//}catch(Exception e){IJ.handleException(e);return "ERR";}
	}

	/** Returns a reference to the PlotObject having the data passed with the constructor or (if that was null)
	 *	the first x & y data added later. Otherwise returns null. */
	private PlotObject getMainCurveObject() {
		for (PlotObject plotObject : allPlotObjects) {
			if (plotObject.type == PlotObject.XY_DATA)
				return plotObject;
		}
		return null;
	}

	/** Returns a reference to the PlotObject with x & y data (points, curve) added last, or null if none. */
	private PlotObject getLastCurveObject() {
		for (int i=allPlotObjects.size()-1; i>=0; i--) {
			if (allPlotObjects.get(i).type == PlotObject.XY_DATA)
				return allPlotObjects.get(i);
		}
		return null;
	}

	/** returns whether there are several plots so that one cannot give a single y value for a given x value */
	private boolean hasMultiplePlots() {
		int nPlots = 0;
		for (PlotObject plotObject : allPlotObjects) {
			if (plotObject.type == PlotObject.ARROWS)
				return true;
			else if (plotObject.type == PlotObject.XY_DATA) {
				nPlots ++;
				if (nPlots > 1) return true;
			}
		}
		return nPlots > 1;
	}

	public void setPlotMaker(PlotMaker plotMaker) {
		this.plotMaker = plotMaker;
	}
	
	PlotMaker getPlotMaker() {
		return plotMaker;
	}

	/** Returns the labels of the datasets as linefeed-delimited String.
	 *	If the label is not set, a blank line is added */
	String getDataLabels() {
		String labels = "";
		boolean first = true;
		for (PlotObject plotObject : allPlotObjects)
			if (plotObject.type == PlotObject.XY_DATA) {
				if (first)
					first = false;
				else
					labels += '\n';
				if (plotObject.label != null) labels += plotObject.label;
			}
		return labels;
	}

	/** Creates a ResultsTable with the data of the plot. Returns an empty table if no data. 
	 *	Does not write the first x column if writeFirstXColumn is false.
	 *	x columns equal to the first x column are never written, independent of writeFirstXColumn */
	public ResultsTable getResultsTable(boolean writeFirstXColumn) {
		ResultsTable rt = new ResultsTable();
		rt.showRowNumbers(false);
		// find the longest x-value data set and count the data sets
		int nDataSets =	 0;
		int tableLength = 0;
		for (PlotObject plotObject : allPlotObjects)
			if (plotObject.xValues != null) {
				nDataSets++;
				tableLength = Math.max(tableLength, plotObject.xValues.length);
			}
		if (nDataSets == 0) return null;
		// enter columns one by one to lists of data and headings
		ArrayList<String> headings = new ArrayList<String>(2*nDataSets);
		ArrayList<float[]> data = new ArrayList<float[]>(2*nDataSets);
		int dataSetNumber = 0;
		int arrowsNumber = 0;
		PlotObject firstXYobject = null;
		boolean isFirstXYobject;
		for (PlotObject plotObject : allPlotObjects) {
			if (plotObject.type==PlotObject.XY_DATA) {
				boolean sameX =	 firstXYobject != null && Arrays.equals(firstXYobject.xValues, plotObject.xValues);
				boolean sameXY = sameX && Arrays.equals(firstXYobject.yValues, plotObject.yValues); //ignore duplicates (e.g. Markers plus Curve)
				boolean writeX = (firstXYobject==null && writeFirstXColumn) || !sameX;
				addToLists(headings, data, plotObject, dataSetNumber, writeX, /*writeY=*/!sameXY, nDataSets>1);
				if (firstXYobject == null) firstXYobject = plotObject;
				dataSetNumber++;
			} else if (plotObject.type==PlotObject.ARROWS) {
				addToLists(headings, data, plotObject, arrowsNumber, /*writeX=*/true, /*writeY=*/true, nDataSets>1);
				arrowsNumber++;
			}
		}
		// populate the ResultsTable
		int nColumns = headings.size();
		for (int line=0; line<tableLength; line++) {
			for (int col=0; col<nColumns; col++) {
				String heading = headings.get(col);
				float[] values = data.get(col);
				if (line<values.length)
					rt.setValue(heading, line, values[line]);
				else
					rt.setValue(heading, line, "");
			}
		}
		// set the decimals (precision) of the table columns
		nColumns = rt.getLastColumn() + 1;
		for (int i=0; i<nColumns; i++)
			rt.setDecimalPlaces(i, getPrecision(rt.getColumn(i)));

		return rt;
	}

	// when writing data in scientific mode, use at least 4 decimals behind the decimal point
	static final int MIN_SCIENTIFIC_DIGITS = 4;
	// when writing float data, precision should be at least 1e-5*data range
	static final double MIN_FLOAT_PRECISION = 1e-5;


	void addToLists(ArrayList<String> headings, ArrayList<float[]>data, PlotObject plotObject,
			int dataSetNumber, boolean writeX, boolean writeY, boolean multipleSets) {
		if (writeX) {
			String label = plotObject.type == PlotObject.ARROWS ? "XStart" : "X";
			if (multipleSets) label += dataSetNumber;
			headings.add(label);
			data.add(plotObject.xValues);
		}
		if (writeY) {
			String label = plotObject.type == PlotObject.ARROWS ? "YStart" : "Y";
			if (multipleSets) label += dataSetNumber;
			headings.add(label);
			data.add(plotObject.yValues);
		}
		if (plotObject.xEValues != null) {
			String label = plotObject.type == PlotObject.ARROWS ? "XEnd" : "XERR";
			if (multipleSets) label += dataSetNumber;
			headings.add(label);
			data.add(plotObject.xEValues);
		}
		if (plotObject.yEValues != null) {
			String label = plotObject.type == PlotObject.ARROWS ? "YEnd" : "ERR";
			if (multipleSets) label += dataSetNumber;
			headings.add(label);
			data.add(plotObject.yEValues);
		}
	}

	/** get the number of digits for writing a column to the results table or the clipboard */
	static int getPrecision(float[] values) {
		int setDigits = Analyzer.getPrecision();
		int measurements = Analyzer.getMeasurements();
		boolean scientificNotation = (measurements&Measurements.SCIENTIFIC_NOTATION)!=0;
		if (scientificNotation) {
			if (setDigits<MIN_SCIENTIFIC_DIGITS)
				setDigits = MIN_SCIENTIFIC_DIGITS;
			return -setDigits;
		}
		boolean allInteger = true;
		float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
		for (int i=0; i<values.length; i++) {
			if ((int)values[i]!=values[i] && !Float.isNaN(values[i])) {
				allInteger = false;
			if (values[i] < min) min = values[i];
			if (values[i] > max) max = values[i];
			}
		}
		if (allInteger)
			return 0;
		int digits = (max - min) > 0 ? getDigits(min, max, MIN_FLOAT_PRECISION*(max-min), 15) :
				getDigits(max, MIN_FLOAT_PRECISION*Math.abs(max), 15);
		if (setDigits>Math.abs(digits))
			digits = setDigits * (digits < 0 ? -1 : 1);		//use scientific notation if needed
		return digits;
	}

	/** Whether a given flag 'what' is set */
	boolean hasFlag(int what) {
		return (flags&what) != 0;
	}
}

/** This class contains the data and properties for displaying a curve, a set of arrows, a line or a label in a plot
 *	Note that all properties such as lineWidths and Fonts have to be scaled up for high-resolution plots. */
class PlotObject implements Cloneable {
	/** constants for the type of objects */
	public final static int XY_DATA = 0, ARROWS = 1, LINE = 2, NORMALIZED_LINE = 3, DOTTED_LINE = 4,
			LABEL = 5, NORMALIZED_LABEL = 6, LEGEND = 7;
	/** flags */
	public final static int CONSTRUCTOR_DATA = 1;
	/** if true, the object is a label, o */
	public int type = XY_DATA;
	/** bitwise combination of flags, or the position of a legend */
	public int flags;
	/** The x and y data arrays, the error bars. These arrays also serve as x0, y0, x1, y1
	 *	arrays for plotting arrays of arrows */
	public float[] xValues, yValues, xEValues, yEValues;
	/** Type of the points, such as Plot.LINE, Plot.CROSS etc. */
	public int shape;
	/** The line width in pixels for 'small' plots */
	public float lineWidth;
	/** The color of the object, must not be null */
	public Color color;
	/** The secondary color (for filling closed symbols and for the line of CIRCLES_AND_LINE, may be null for unfilled/default */
	public Color color2;
	/* Labels and lines only: Position (NORMALIZED objects: relative units 0...1) */
	public double x, y;
	/* Lines only: End position*/
	public double xEnd, yEnd;
	/* Dotted lines only: step */
	public int step;
	/** A label for the y data of the curve, a text to draw, or the text of a legend (tab-delimited lines) */
	public String label;
	/** Labels only: Justification can be Plot.LEFT, Plot.CENTER or Plot.RIGHT */
	public int justification;
	/** Labels only: the font; maybe null for default */
	public Font font;

	/** Constructor for XY_DATA, i.e., a curve or set of points */
	PlotObject(float[] xValues, float[] yValues, float[] yErrorBars, int shape, float lineWidth, Color color, Color color2, String yLabel) {
		this.type = XY_DATA;
		this.xValues = xValues;
		this.yValues = yValues;
		this.yEValues = yErrorBars;
		this.shape = shape;
		this.lineWidth = lineWidth;
		this.color = color;
		this.color2 = color2;
		this.label = yLabel;
	}

	/** Constructor for a set of arrows */
	PlotObject(float[] x1, float[] y1, float[] x2, float[] y2, float lineWidth, Color color) {
		this.type = ARROWS;
		this.xValues = x1;
		this.yValues = y1;
		this.xEValues = x2;
		this.yEValues = y2;
		this.lineWidth = lineWidth;
		this.color = color;
	}

	/** Constructor for a line */
	PlotObject(double x, double y, double xEnd, double yEnd, float lineWidth, int step, Color color, int type) {
		this.type = type;
		this.x = x;
		this.y = y;
		this.xEnd = xEnd;
		this.yEnd = yEnd;
		this.lineWidth = lineWidth;
		this.step = step;
		this.color = color;
	}

	/** Constructor for a label or NORMALIZED_LABEL */
	PlotObject(String label, double x, double y, int justification, Font font, Color color, int type) {
		this.type = type;
		this.label = label;
		this.x = x;
		this.y = y;
		this.justification = justification;
		this.font = font;
		this.color = color;
	}

	/** Constructor for the legend. <code>flags</code> is bitwise or of TOP_LEFT etc. and ERASE_BACKGROUND if desired */
	PlotObject(String labels, float lineWidth, Font font, Color color, int flags) {
		this.type = LEGEND;
		this.label = labels;
		this.lineWidth = lineWidth;
		this.font = font;
		this.color = color;
		this.flags = flags;
	}

	/** Whether a given flag 'what' is set */
	boolean hasFlag(int what) {
		return (flags&what) != 0;
	}

	/** Whether an XY_DATA object has a curve to draw */
	boolean hasCurve() {
		return type == XY_DATA && (shape == Plot.LINE || shape == Plot.CONNECTED_CIRCLES);
	}

	/** Whether an XY_DATA object has markers to draw */
	boolean hasMarker() {
		return type == XY_DATA && (shape == Plot.CIRCLE || shape == Plot.X || shape == Plot.BOX || shape == Plot.TRIANGLE ||
				shape == Plot.CROSS || shape == Plot.DOT || shape == Plot.CONNECTED_CIRCLES);
	}

	/** Whether an XY_DATA object has markers that can be filled */
	boolean hasFilledMarker() {
		return type == XY_DATA && color2 != null && (shape == Plot.CIRCLE || shape == Plot.BOX || shape == Plot.TRIANGLE);
	}

	/** Size of the markers for an XY_DATA object with markers */
	int getMarkerSize () {
		return lineWidth<=1 ? 5 : 7;
	}

	/** A shallow clone and does not duplicate arrays*/
	public PlotObject clone() {
		try {
			return (PlotObject)(super.clone());
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}