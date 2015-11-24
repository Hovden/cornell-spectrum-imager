package com.DirectElectron.LiveFFT;

import edu.emory.mathcs.parallelfftj.FloatTransformer;

import edu.emory.mathcs.parallelfftj.FourierDomainOriginType;
import edu.emory.mathcs.parallelfftj.SpectrumType;
import edu.emory.mathcs.parallelfftj.Transformer;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ContrastEnhancer;

/**
 * Implements SerialEM's method for scaling the FFT values for display.
 * 
 * @sponsor Direct Electron (http://www.directelectron.com/)
 * @author Sunny Chow (sunny.chow@acm.org)
 */
class FFTAutoscale implements FFTProcessor {
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	
	public double LogScale = 0.1;
	
	public double SaturationPercentage = 0.4;
	
	public String toString()
	{
		return "Autoscale";
	}
	
	public FFTAutoscale()
	{
	}
	/* (non-Javadoc)
	 * The output is passed in only to retrieve the previous min and max values.
	 * @see FFTProcessor#process(ij.ImageStack)
	 */
	public ImagePlus process(ImageStack source, ImagePlus output) {
		Transformer processor= new FloatTransformer(source, null);
		processor.fft();
		ImagePlus result = processor.toImagePlus(SpectrumType.POWER_SPECTRUM, FourierDomainOriginType.AT_CENTER);
		// Get Raw data.
		Object rawData = result.getStack().getProcessor(1).getPixels();
		if (rawData instanceof float[])
		{
			float[] fdata = (float[])rawData;
			
			// Get the maximum value for the center image.
			int cSz = result.getWidth() / 10;
			int rSz = result.getHeight() / 10;
			if (cSz < 2) cSz = 2;
			if (rSz < 2) rSz = 2;
			double maxVal = Double.MIN_VALUE;
			
			//Start at an offset from the center.
			int startR = result.getHeight() / 2 + 1;
			int startC = result.getWidth() / 2 + 1;
			int startInd = startR * result.getWidth() + 1;
			

			for (int r = startR ; r < rSz + startR; r++)
			{
				for (int c = startC; c < cSz + startC; c++)
				{
					if (maxVal < fdata[startInd + c])
					{
						maxVal = fdata[startInd + c];
					}
				}
				startInd += result.getWidth();
			}	
			
			
			// Determine the mean of the border.
			double sum = 0;
			startInd = result.getWidth() * (result.getHeight() - 1);
			for (int c = 0; c < result.getWidth(); c++)
			{
				sum += fdata[startInd + c];
			}
			
			double logScale = this.LogScale;
			
			if (sum > 0)
			{
				logScale /= sum / result.getWidth();
			}
			double scale = 255. / Math.log10(logScale * maxVal);
			
			startInd = 0;
			for (int c = 0; c < result.getWidth() * result.getHeight(); c++)
			{
				fdata[c] = (float)(scale * Math.log10(logScale * fdata[c] + 1.)); 
			}
				
			result.setDisplayRange(0, 255);
			
			// Stretch the histogram
			ContrastEnhancer enhancer = new ContrastEnhancer();
			enhancer.stretchHistogram(result, this.SaturationPercentage);
			
		}

		return result;
	}
}
