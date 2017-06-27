package com.spectrumimager.CSI;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class CSI_4D_Reslice implements PlugInFilter {
	ImagePlus img;
	public int setup(String arg, ImagePlus img) {
		this.img = img;
		return DOES_ALL+STACK_REQUIRED;
	}
	public void run(ImageProcessor ip) {
		IJ.run("Select All");
		String name=img.getTitle();
		int[] whczt = img.getDimensions();
		IJ.run("Reslice [/]...", "output=1.000 start=Top avoid");
		IJ.selectWindow(name);
		IJ.getImage().close();
		IJ.selectWindow("Reslice of "+name);
		IJ.run("Stack to Hyperstack...", "order=xyczt(default) channels="+whczt[0]+" slices="+whczt[4]+" frames=1 display=Grayscale");
		IJ.run("Reslice [/]...", "output=1.000 start=Left avoid");
		IJ.selectWindow("Reslice of "+name);
		IJ.getImage().close();
		IJ.selectWindow("Reslice of Reslice of "+name);
		IJ.run("Stack to Hyperstack...", "order=xyczt(default) channels=1 slices="+whczt[0]+" frames="+whczt[1]+" display=Grayscale");
		IJ.getImage().setTitle("4D Reslice of "+name);
	}
}