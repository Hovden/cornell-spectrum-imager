package ij.plugin;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.io.*;
import ij.plugin.filter.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/** This plugin implements most of the Edit/Options/Colors command. */
public class Colors implements PlugIn, ItemListener {
	public static final String[] colors = {"red","green","blue","magenta","cyan","yellow","orange","black","white"};
	private static final String[] colors2 = {"Red","Green","Blue","Magenta","Cyan","Yellow","Orange","Black","White"};
	private Choice fchoice, bchoice, schoice;
	private Color fc2, bc2, sc2;

 	public void run(String arg) {
		showDialog();
	}

	void showDialog() {
		Color fc =Toolbar.getForegroundColor();
		String fname = getColorName(fc, "black");
		Color bc =Toolbar.getBackgroundColor();
		String bname = getColorName(bc, "white");
		Color sc =Roi.getColor();
		String sname = getColorName(sc, "yellow");
		GenericDialog gd = new GenericDialog("Colors");
		gd.addChoice("Foreground:", colors, fname);
		gd.addChoice("Background:", colors, bname);
		gd.addChoice("Selection:", colors, sname);
		Vector choices = gd.getChoices();
		if (choices!=null) {
			fchoice = (Choice)choices.elementAt(0);
			bchoice = (Choice)choices.elementAt(1);
			schoice = (Choice)choices.elementAt(2);
			fchoice.addItemListener(this);
			bchoice.addItemListener(this);
			schoice.addItemListener(this);
		}
		
		gd.showDialog();
		if (gd.wasCanceled()) {
			if (fc2!=fc) Toolbar.setForegroundColor(fc);
			if (bc2!=bc) Toolbar.setBackgroundColor(bc);
			if (sc2!=sc) {
				Roi.setColor(sc);
				ImagePlus imp = WindowManager.getCurrentImage();
				if (imp!=null && imp.getRoi()!=null) imp.draw();
			}
			return;
		}
		fname = gd.getNextChoice();
		bname = gd.getNextChoice();
		sname = gd.getNextChoice();
		fc2 = getColor(fname, Color.black);
		bc2 = getColor(bname, Color.white);
		sc2 = getColor(sname, Color.yellow);
		if (fc2!=fc) Toolbar.setForegroundColor(fc2);
		if (bc2!=bc) Toolbar.setBackgroundColor(bc2);
		if (sc2!=sc) {
			Roi.setColor(sc2);
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp!=null) imp.draw();
			Toolbar.getInstance().repaint();
		}
	}
	
	public static String getColorName(Color c, String defaultName) {
		if (c==null) return defaultName;
		String name = defaultName;
		if (name!=null && name.length()>0 && Character.isUpperCase(name.charAt(0))) {
			if (c.equals(Color.red)) name = colors2[0];
			else if (c.equals(Color.green)) name = colors2[1];
			else if (c.equals(Color.blue)) name = colors2[2];
			else if (c.equals(Color.magenta)) name = colors2[3];
			else if (c.equals(Color.cyan)) name = colors2[4];
			else if (c.equals(Color.yellow)) name = colors2[5];
			else if (c.equals(Color.orange)) name = colors2[6];
			else if (c.equals(Color.black)) name = colors2[7];
			else if (c.equals(Color.white)) name = colors2[8];
		} else {
			if (c.equals(Color.red)) name = colors[0];
			else if (c.equals(Color.green)) name = colors[1];
			else if (c.equals(Color.blue)) name = colors[2];
			else if (c.equals(Color.magenta)) name = colors[3];
			else if (c.equals(Color.cyan)) name = colors[4];
			else if (c.equals(Color.yellow)) name = colors[5];
			else if (c.equals(Color.orange)) name = colors[6];
			else if (c.equals(Color.black)) name = colors[7];
			else if (c.equals(Color.white)) name = colors[8];
		}
		return name;
	}
	
	public static Color getColor(String name, Color defaultColor) {
		if (name==null) return defaultColor;
		name = name.toLowerCase(Locale.US);
		Color c = defaultColor;
		if (name.equals(colors[0])) c = Color.red;
		else if (name.equals(colors[1])) c = Color.green;
		else if (name.equals(colors[2])) c = Color.blue;
		else if (name.equals(colors[3])) c = Color.magenta;
		else if (name.equals(colors[4])) c = Color.cyan;
		else if (name.equals(colors[5])) c = Color.yellow;
		else if (name.equals(colors[6])) c = Color.orange;
		else if (name.equals(colors[7])) c = Color.black;
		else if (name.equals(colors[8])) c = Color.white;
		return c;
	}

	public static Color decode(String hexColor, Color defaultColor) {
		Color color = getColor(hexColor, Color.gray);
		if (color==Color.gray) {
			if (hexColor.startsWith("#"))
				hexColor = hexColor.substring(1);
			int len = hexColor.length();
			if (!(len==6 || len==8))
				return defaultColor;
			float alpha = len==8?parseHex(hexColor.substring(0,2)):1f;
			if (len==8)
				hexColor = hexColor.substring(2);
			float red = parseHex(hexColor.substring(0,2));
			float green = parseHex(hexColor.substring(2,4));
			float blue = parseHex(hexColor.substring(4,6));
			color = new Color(red, green, blue, alpha);
		}
		return color;
	}

	public static int getRed(String hexColor) {
		return decode(hexColor, Color.black).getRed();
	}

	public static int getGreen(String hexColor) {
		return decode(hexColor, Color.black).getGreen();
	}

	public static int getBlue(String hexColor) {
		return decode(hexColor, Color.black).getBlue();
	}

	/** Converts a hex color (e.g., "ffff00") into "red", "green", "yellow", etc.
		Returns null if the color is not one of the eight primary colors. */
	public static String hexToColor(String hex) {
		if (hex==null) return null;
		if (hex.startsWith("#"))
			hex = hex.substring(1);
		String color = null;
		if (hex.equals("ff0000")) color = "red";
		else if (hex.equals("00ff00")) color = "green";
		else if (hex.equals("0000ff")) color = "blue";
		else if (hex.equals("000000")) color = "black";
		else if (hex.equals("ffffff")) color = "white";
		else if (hex.equals("ffff00")) color = "yellow";
		else if (hex.equals("00ffff")) color = "cyan";
		else if (hex.equals("ff00ff")) color = "magenta";
		return color;
	}
	
	/** Converts a hex color (e.g., "ffff00") into "Red", "Green", "Yellow", etc.
		Returns null if the color is not one of the eight primary colors. */
	public static String hexToColor2(String hex) {
		if (hex==null) return null;
		if (hex.startsWith("#"))
			hex = hex.substring(1);
		String color = null;
		if (hex.equals("ff0000")) color = "Red";
		else if (hex.equals("00ff00")) color = "Green";
		else if (hex.equals("0000ff")) color = "Blue";
		else if (hex.equals("000000")) color = "Black";
		else if (hex.equals("ffffff")) color = "White";
		else if (hex.equals("ffff00")) color = "Yellow";
		else if (hex.equals("00ffff")) color = "Cyan";
		else if (hex.equals("ff00ff")) color = "Magenta";
		else if (hex.equals("ffc800")) color = "Orange";
		return color;
	}

	/** Converts a Color into a string ("red", "green", #aa55ff, etc.). */
	public static String colorToString(Color color) {
		String str = color!=null?"#"+Integer.toHexString(color.getRGB()):"none";
		if (str.length()==9 && str.startsWith("#ff"))
			str = "#"+str.substring(3);
		String str2 = hexToColor(str);
		return str2!=null?str2:str;
	}

	/** Converts a Color into a string ("Red", "Green", #aa55ff, etc.). */
	public static String colorToString2(Color color) {
		String str = color!=null?"#"+Integer.toHexString(color.getRGB()):"None";
		if (str.length()==9 && str.startsWith("#ff"))
			str = "#"+str.substring(3);
		String str2 = hexToColor2(str);
		return str2!=null?str2:str;
	}

	private static float parseHex(String hex) {
		float value = 0f;
		try {value=Integer.parseInt(hex,16);}
		catch(Exception e) { }
		return value/255f;
	}

	public void itemStateChanged(ItemEvent e) {
		Choice choice = (Choice)e.getSource();
		String item = choice.getSelectedItem();
		Color color = getColor(item, Color.black);
		if (choice==fchoice)
			Toolbar.setForegroundColor(color);
		else if (choice==bchoice)
			Toolbar.setBackgroundColor(color);
		else if (choice==schoice) {
			Roi.setColor(color);
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp!=null && imp.getRoi()!=null) imp.draw();
			Toolbar.getInstance().repaint();
		}
	}
	
	public static String[] getColors(String... moreColors) {
		ArrayList names = new ArrayList();
		for (String arg: moreColors) {
			if (arg!=null && arg.length()>0 && (!Character.isLetter(arg.charAt(0))||arg.equals("None")))
				names.add(arg);
		}
		for (String arg: colors2)
			names.add(arg);
		return (String[])names.toArray(new String[names.size()]);
	}

}
