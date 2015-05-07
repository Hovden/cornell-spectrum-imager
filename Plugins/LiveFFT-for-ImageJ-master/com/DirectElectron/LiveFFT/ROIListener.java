package com.DirectElectron.LiveFFT;
import ij.ImagePlus;

/**
 * Simple interface to accept callbacks whenever the ROI is updated.
 * 
 *
 * @sponsor Direct Electron (http://www.directelectron.com/)
 * @author Sunny Chow (sunny.chow@acm.org)
 *
 */
public interface ROIListener {
	/**
	 * This method is called whenever there is a change
	 * in the ROI of the image.
	 * 
	 * @param imp
	 */
	public void roiUpdated(ImagePlus imp);
}
