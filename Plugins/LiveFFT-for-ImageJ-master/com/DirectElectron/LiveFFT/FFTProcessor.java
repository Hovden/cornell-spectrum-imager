package com.DirectElectron.LiveFFT;
import ij.ImagePlus;
import ij.ImageStack;

/**
 * Encapsulates the functionality for the FFT transform and subsequent display
 * mapping.
 * 
 * @sponsor Direct Electron (http://www.directelectron.com/)
 * @author Sunny Chow (sunny.chow@acm.org)
 *
 */
public interface FFTProcessor {
	/**
	 * Processes the image and applies an appropiate look up table for display.
	 * 
	 * @param source
	 * @return
	 */
	public ImagePlus process(ImageStack source, ImagePlus output);
}
