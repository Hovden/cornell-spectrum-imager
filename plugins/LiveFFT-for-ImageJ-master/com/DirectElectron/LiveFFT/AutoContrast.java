package com.DirectElectron.LiveFFT;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ImageStatistics;

/**
 * Based on the ImageJ method of doing autocontrast.
 * in ContrastAdjuster.java
 * @author sunny
 *
 */
public class AutoContrast {
	public double Min;
	public double Max;
	private int autoThreshold;
	public AutoContrast(){
		this.Min = 0;
		this.Max = 256;
		this.autoThreshold = 5000;
	}
	
	public void process(ImagePlus imp)
	{
		ImageStatistics stats = getUncalibratedStats(imp);

		this.Min = stats.histMin;
		this.Max = stats.histMax;
		
		int limit = stats.pixelCount/10;
		int[] histogram = stats.histogram;
		// Don't want a dynamically changing threshold for our purposes.
		//this.autoThreshold = (this.autoThreshold < 10) ? 5000 : this.autoThreshold / 2;
		int threshold = stats.pixelCount / this.autoThreshold;
		double hMin = this.overThreshold(histogram, threshold, limit, 0, 255) * stats.binSize + stats.histMin;
		double hMax = this.overThreshold(histogram, threshold, limit, 255, 0) * stats.binSize + stats.histMin;
		if (hMax > hMin)
		{
			this.Min = hMin;
			this.Max = hMax;
		}
	}
	
	public int overThreshold(int[] histogram, int threshold, int limit,int start, int end)
	{
		int inc = end - start > 0 ? + 1 : -1 ;
		int cur = start;
		for (; cur != end; cur += inc)
		{
			if (threshold < histogram[cur] && histogram[cur] < limit)
				break;
		}
		return cur;
	}
	
	public ImageStatistics getUncalibratedStats(ImagePlus imp)
	{
		Calibration cal = imp.getCalibration();
		ImageStatistics stats;
		try 
		{
			imp.setCalibration(null);
			stats = imp.getStatistics();
		}
		finally
		{
			imp.setCalibration(cal);				
		}
		return stats;
	}
}
