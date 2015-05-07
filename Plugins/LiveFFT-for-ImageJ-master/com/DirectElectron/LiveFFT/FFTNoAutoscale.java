package com.DirectElectron.LiveFFT;
import edu.emory.mathcs.parallelfftj.FloatTransformer;
import edu.emory.mathcs.parallelfftj.FourierDomainOriginType;
import edu.emory.mathcs.parallelfftj.SpectrumType;
import edu.emory.mathcs.parallelfftj.Transformer;
import ij.ImagePlus;
import ij.ImageStack;

/**
 * Uses ParallelFFTJ (http://sites.google.com/site/piotrwendykier/software/parallelfftj) to 
 * compute the FFT transform.
 * 
 * @sponsor Direct Electron (http://www.directelectron.com/)
 * @author Sunny Chow (sunny.chow@acm.org)
 */
public class FFTNoAutoscale implements FFTProcessor {

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return "None";
	}
	
	/* (non-Javadoc)
	 * @see FFTProcessor#process(ij.ImageStack)
	 */
	public ImagePlus process(ImageStack source, ImagePlus output) {
		Transformer processor= new FloatTransformer(source, null);
		processor.fft();
		ImagePlus result = processor.toImagePlus(SpectrumType.POWER_SPECTRUM_LOG, FourierDomainOriginType.AT_CENTER);

		return result;
	}
}
