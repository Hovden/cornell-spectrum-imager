package ij.gui;
import ij.*;
import ij.plugin.Colors;
import ij.io.RoiDecoder;
import ij.process.FloatPolygon;
import ij.measure.*;
import ij.util.Tools;
import ij.plugin.filter.Analyzer;
import ij.text.TextWindow;
import java.awt.*;
import java.util.*;


 /** Displays a dialog that allows the user to specify ROI properties such as color and line width. */
public class RoiProperties {
	private Roi roi;
	private String title;
	private boolean showName = true;
	private boolean showListCoordinates;
	private boolean addToOverlay;
	private boolean overlayOptions;
	private boolean existingOverlay;
	private boolean setPositions;
	private boolean listCoordinates;
	private boolean listProperties;
	private static final String[] justNames = {"Left", "Center", "Right"};
	private int nProperties;

	/** Constructs a ColorChooser using the specified title and initial color. */
	public RoiProperties(String title, Roi roi) {
		if (roi==null)
			throw new IllegalArgumentException("ROI is null");
		this.title = title;
		showName = title.startsWith("Prop");
		showListCoordinates = showName && title.endsWith(" ");
		nProperties = showListCoordinates?roi.getPropertyCount():0;
		addToOverlay = title.equals("Add to Overlay");
		overlayOptions = title.equals("Overlay Options");
		ImagePlus imp = WindowManager.getCurrentImage();
		if (overlayOptions) {
			Overlay overlay = imp!=null?imp.getOverlay():null;
			setPositions = roi.getPosition()!=0;
			if (overlay!=null)
				existingOverlay = true;
		}
		this.roi = roi;
	}
	
	private String decodeColor(Color color, Color defaultColor) {
		if (color==null)
			color = defaultColor;
		String str = "#"+Integer.toHexString(color.getRGB());
		if (str.length()==9 && str.startsWith("#ff"))
			str = "#"+str.substring(3);
		String lc = Colors.hexToColor(str);
		if (lc!=null) str = lc;
		return str;
	}
	
	/** Displays the dialog box and returns 'false' if the user cancels it. */
	public boolean showDialog() {
		String name= roi.getName();
		boolean isRange = name!=null && name.startsWith("range:");
		String nameLabel = isRange?"Range:":"Name:";
		if (isRange) name = name.substring(7);
		if (name==null) name = "";
		if (!isRange && (roi instanceof ImageRoi) && !overlayOptions)
			return showImageDialog(name);
		Color strokeColor = roi.getStrokeColor();
		Color fillColor = roi.getFillColor();
		double strokeWidth = roi.getStrokeWidth();
		boolean isText = roi instanceof TextRoi;
		boolean isLine = roi.isLine();
		boolean isPoint = roi instanceof PointRoi;
		int justification = TextRoi.LEFT;
		double angle = 0.0;
		boolean antialias = true;
		if (isText) {
			TextRoi troi = (TextRoi)roi;
			Font font = troi.getCurrentFont();
			strokeWidth = font.getSize();
			angle = troi.getAngle();
			justification = troi.getJustification();
			antialias = troi.getAntialiased();
		}
		String position = ""+roi.getPosition();
		int cpos = roi.getCPosition();
		int zpos = roi.getZPosition();
		int tpos = roi.getTPosition();
		if (cpos>0 || zpos>0 || tpos>0)
			position = cpos +","+zpos+","+tpos;
		if (position.equals("0"))
			position = "none";
		String linec = Colors.colorToString(strokeColor);
		String fillc = Colors.colorToString(fillColor);
		if (IJ.isMacro()) {
			fillc = "none";
			setPositions = false;
		}
		int digits = (int)strokeWidth==strokeWidth?0:1;
		GenericDialog gd = new GenericDialog(title);
		if (showName) {
			gd.addStringField(nameLabel, name, 15);
			gd.addStringField("Position:", position);
		}
		if (isText) {
			gd.addStringField("Stroke color:", linec);
			gd.addNumericField("Font size:", strokeWidth, digits, 4, "points");
			digits = (int)angle==angle?0:1;
			gd.addNumericField("Angle:", angle, digits, 4, "degrees");
			gd.setInsets(0, 0, 0);
			gd.addChoice("Justification:", justNames, justNames[justification]);
		} else {
			if (isPoint)
				gd.addStringField("Stroke (point) color:", linec);
			else {
				gd.addStringField("Stroke color:", linec);
				gd.addNumericField("Width:", strokeWidth, digits);
			}
		}
		if (!isLine) {
			if (isPoint) {
				int index = ((PointRoi)roi).getPointType();
				gd.addChoice("Point type:", PointRoi.types, PointRoi.types[index]);
				index = ((PointRoi)roi).getSize();
				gd.addChoice("Size:", PointRoi.sizes, PointRoi.sizes[index]);
			} else {
				gd.addMessage("");
				gd.addStringField("Fill color:", fillc);
			}
		}
		if (addToOverlay)
			gd.addCheckbox("New overlay", false);
		if (overlayOptions) {
			if (existingOverlay) {
				gd.addCheckbox("Apply to current overlay", false);
			}
			gd.addCheckbox("Set stack positions", setPositions);
		}
		if (isText)
			gd.addCheckbox("Antialiased text", antialias);
		if (showListCoordinates) {
			int n = roi.getFloatPolygon().npoints;
			gd.addCheckbox("List coordinates ("+n+")", listCoordinates);
			if (nProperties>0)
				gd.addCheckbox("List properties ("+nProperties+")", listProperties);
			else {
				gd.setInsets(5,20,0);
				gd.addMessage("No properties");
			}
		}
		if (showName && "".equals(name) && "none".equals(position) && "none".equals(fillc))
			gd.setSmartRecording(true);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		String position2 = "";
		if (showName) {
			name = gd.getNextString();
			if (!isRange) roi.setName(name.length()>0?name:null);
			position2 = gd.getNextString();
		}
		linec = gd.getNextString();
		if (!isPoint)
			strokeWidth = gd.getNextNumber();
		if (isText) {
			angle = gd.getNextNumber();
			justification = gd.getNextChoiceIndex();
		}
		if (!isLine) {
			if (isPoint) {
				int index = gd.getNextChoiceIndex();
				((PointRoi)roi).setPointType(index);
				index = gd.getNextChoiceIndex();
				((PointRoi)roi).setSize(index);
			} else
				fillc = gd.getNextString();
		}
		boolean applyToOverlay = false;
		boolean newOverlay = addToOverlay?gd.getNextBoolean():false;
		if (overlayOptions) {
			if (existingOverlay)
				applyToOverlay = gd.getNextBoolean();
			setPositions = gd.getNextBoolean();
			roi.setPosition(setPositions?1:0);
		}
		if (isText)
			antialias = gd.getNextBoolean();
		if (showListCoordinates) {
			listCoordinates = gd.getNextBoolean();
			if (nProperties>0)
				listProperties = gd.getNextBoolean();
		}
		strokeColor = Colors.decode(linec, null);
		fillColor = Colors.decode(fillc, null);
		if (isText) {
			TextRoi troi = (TextRoi)roi;
			Font font = troi.getCurrentFont();
			if ((int)strokeWidth!=font.getSize()) {
				font = new Font(font.getName(), font.getStyle(), (int)strokeWidth);
				troi.setCurrentFont(font);
			}
			troi.setAngle(angle);
			if (justification!=troi.getJustification())
				troi.setJustification(justification);
			troi.setAntialiased(antialias);
		} else
			roi.setStrokeWidth((float)strokeWidth);
		if (showName)
			setPosition(roi, position, position2);
		roi.setStrokeColor(strokeColor);
		roi.setFillColor(fillColor);
		if (newOverlay) roi.setName("new-overlay");
		if (applyToOverlay) {
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp==null)
				return true;
			Overlay overlay = imp.getOverlay();
			if (overlay==null)
				return true;
			Roi[] rois = overlay.toArray();
			for (int i=0; i<rois.length; i++) {
				rois[i].setStrokeColor(strokeColor);
				rois[i].setStrokeWidth((float)strokeWidth);
				rois[i].setFillColor(fillColor);
			}
			imp.draw();
		}
		if (listCoordinates)
			listCoordinates(roi);
		if (listProperties && nProperties>0)
			listProperties(roi);
		return true;
	}
	
	private void setPosition(Roi roi, String pos1, String pos2) {
		if (pos1.equals(pos2))
			return;
		if (pos2.equals("none") || pos2.equals("0")) {
			roi.setPosition(0);
			return;
		}
		String[] positions = Tools.split(pos2, " ,");
		if (positions.length==1) {
			double stackPos = Tools.parseDouble(positions[0]);
			if (!Double.isNaN(stackPos))
				roi.setPosition((int)stackPos);
			return;
		}
		if (positions.length==3) {
			int[] pos = new int[3];
			for (int i=0; i<3; i++) {
				double dpos = Tools.parseDouble(positions[i]);
				if (Double.isNaN(dpos))
					return;
				else
					pos[i] = (int)dpos;
			}
			roi.setPosition(pos[0], pos[1], pos[2]);
			return;
		}
	}
		
	public boolean showImageDialog(String name) {
		GenericDialog gd = new GenericDialog(title);
		gd.addStringField("Name:", name, 15);
		gd.addNumericField("Opacity (0-100%):", ((ImageRoi)roi).getOpacity()*100.0, 0);
		if (addToOverlay)
			gd.addCheckbox("New Overlay", false);
		gd.showDialog();
		if (gd.wasCanceled()) return false;
		name = gd.getNextString();
		roi.setName(name.length()>0?name:null);
		double opacity = gd.getNextNumber()/100.0;
		((ImageRoi)roi).setOpacity(opacity);
		boolean newOverlay = addToOverlay?gd.getNextBoolean():false;
		if (newOverlay) roi.setName("new-overlay");
		return true;
	}
	
	void listCoordinates(Roi roi) {
		if (roi==null) return;
		boolean allIntegers = true;
		FloatPolygon fp = roi.getFloatPolygon();
		//FloatPolygon fp  = ((PolygonRoi)roi).getNonSplineFloatCoordinates();
		ImagePlus imp = roi.getImage();
		String title = "Coordinates";
		if (imp!=null) {
			Calibration cal = imp.getCalibration();
			if (cal.pixelWidth!=1.0 || cal.pixelHeight!=1.0) {
				for (int i=0; i<fp.npoints; i++) {
					fp.xpoints[i] *= cal.pixelWidth;
					fp.ypoints[i] *= cal.pixelHeight;
				}
				allIntegers = false;
			}
			title = imp.getTitle();
		}
		if (allIntegers) {
			for (int i=0; i<fp.npoints; i++) {
				if ((int)fp.xpoints[i]!=fp.xpoints[i] || (int)fp.ypoints[i]!=fp.ypoints[i]) {
					allIntegers = false;
					break;
				}
			}
		}
		ResultsTable rt = new ResultsTable();
		rt.setPrecision(allIntegers?0:Analyzer.getPrecision());
		for (int i=0; i<fp.npoints; i++) {
			rt.incrementCounter();
			rt.addValue("X", fp.xpoints[i]);
			rt.addValue("Y", fp.ypoints[i]);
		}
		rt.showRowNumbers(false);
		rt.show("XY_"+title);
	}
	
	void listProperties(Roi roi) {
		String props = roi.getProperties();
		if (props==null) return;
		props = props.replaceAll(": ", "\t");
		new TextWindow("Properties", "Key\tValue", props, 300, 300);
	}

}
