package com.spectrumimager.CSI;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.gui.GenericDialog;

public class CSI_4D_Format_EMPAD implements PlugInFilter {
	ImagePlus img;
	public int setup(String arg, ImagePlus img) {
		this.img = img;
		return DOES_ALL+STACK_REQUIRED;
	}
	public void run(ImageProcessor ip) {
		int nSlices = img.getNSlices();
		int width=(int)Math.sqrt(nSlices);
		int height=(int)Math.sqrt(nSlices);
		GenericDialog gd = new GenericDialog("Format PAD data...");
		gd.addNumericField("Width:", width,0);
		gd.addNumericField("Height:", height,0);
		gd.showDialog();
		if (gd.wasCanceled()) return;
		width = (int)gd.getNextNumber();
		height = (int)gd.getNextNumber();
		img.setRoi(2, 2, 124, 124);
		ImagePlus imgc=img.duplicate();
		img.close();
		imgc.show();
		IJ.run("Stack to Hyperstack...", "order=xyczt(default) channels=1 slices="+width+" frames="+height+" display=Grayscale");
		IJ.resetMinAndMax();
	}
}
