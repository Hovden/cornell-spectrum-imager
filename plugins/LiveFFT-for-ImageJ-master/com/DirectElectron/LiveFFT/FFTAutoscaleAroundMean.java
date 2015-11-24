package com.DirectElectron.LiveFFT;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Measurements;
import ij.process.ImageStatistics;

/**
 * Scales the return of the FFT based on the +- 3 standard deviations from the mean
 * for display.
 * 
 * @sponsor Direct Electron (http://www.directelectron.com/)
 * @author Sunny Chow (sunny.chow@acm.org)
 */
public class FFTAutoscaleAroundMean extends FFTNoAutoscale {
	/* (non-Javadoc)
	 * @see FFTNoAutoscale#toString()
	 */
	public String toString()
	{
		return "Autoscale around mean";
	}
	
	/* (non-Javadoc)
	 * @see FFTNoAutoscale#process(ij.ImageStack)
	 */
	public ImagePlus process(ImageStack src, ImagePlus output)
	{
		ImagePlus result = super.process(src, output);
		
		ImageStatistics stat = result.getStatistics(Measurements.STD_DEV & Measurements.MEAN);
		
		// Create custom LUT
		double zeroVal = stat.mean - stat.stdDev * 3;
		double satVal = stat.mean + stat.stdDev * 3;
		
		// Instruct ImageJ to use custom LUT
		result.setDisplayRange(zeroVal, satVal);
		
		return result;
	}
}
