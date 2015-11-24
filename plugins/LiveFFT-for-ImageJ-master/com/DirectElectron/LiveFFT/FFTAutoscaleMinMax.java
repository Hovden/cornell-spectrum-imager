package com.DirectElectron.LiveFFT;

import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Measurements;
import ij.process.ImageStatistics;


/**
 * Scales the return of the FFT by the min and max values for display.
 * 
 * @sponsor Direct Electron (http://www.directelectron.com/)
 * @author Sunny Chow (sunny.chow@acm.org)
 * 
 */
public class FFTAutoscaleMinMax extends FFTNoAutoscale {
	/* (non-Javadoc)
	 * @see FFTNoAutoscale#toString()
	 */
	public String toString()
	{
		return "Autoscale with min/max";
	}
	
	/* (non-Javadoc)
	 * @see FFTNoAutoscale#process(ij.ImageStack)
	 */
	public ImagePlus process(ImageStack source, ImagePlus output)
	{
		ImagePlus result = super.process(source, output);
		
		// Calculate min and max
		ImageStatistics stat = result.getStatistics(Measurements.MIN_MAX);

		// Instruct ImageJ to use custom LUT
		result.setDisplayRange(stat.min, stat.max);
		
		return result;
	}
}
