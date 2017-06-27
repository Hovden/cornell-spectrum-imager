package com.spectrumimager.CSI;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import ij.IJ;

class CSI_ResizeListener implements ComponentListener {

	/**
	 * 
	 */
	private final CSI_SpectrumData spectrumData;

	/**
	 * @param spectrumData
	 */
	CSI_ResizeListener(CSI_SpectrumData spectrumData) {
		this.spectrumData = spectrumData;
	}

	public void componentResized(ComponentEvent e) {
		this.spectrumData.plotHeight = Math.max(e.getComponent().getSize().height - this.spectrumData.marginHeight, 0);
		this.spectrumData.plotWidth = Math.max(e.getComponent().getSize().width - this.spectrumData.marginWidth, 0);
		IJ.run("Profile Plot Options...",
				"width=" + this.spectrumData.plotWidth + " height=" + this.spectrumData.plotHeight + " minimum=0 maximum=0");
		this.spectrumData.updateProfile();
	}

	public void componentMoved(ComponentEvent e) {
	}

	public void componentShown(ComponentEvent e) {
	}

	public void componentHidden(ComponentEvent e) {
	}

}