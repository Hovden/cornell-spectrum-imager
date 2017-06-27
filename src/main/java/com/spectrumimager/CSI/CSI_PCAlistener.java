package com.spectrumimager.CSI;
import java.awt.Color;

import ij.ImageListener;
import ij.ImagePlus;
import ij.gui.Plot;
import ij.gui.PlotWindow;

class CSI_PCAlistener implements ImageListener {
	/**
	 * 
	 */
	private final CSI_PCAwindows pcAwindows;

	/**
	 * @param pcAwindows
	 */
	CSI_PCAlistener(CSI_PCAwindows pcAwindows) {
		this.pcAwindows = pcAwindows;
	}

	public void imageUpdated(ImagePlus imp) {
		if (imp == this.pcAwindows.maps) {
			int i = this.pcAwindows.maps.getSlice();
			Plot p = new Plot("Scree Plot", "Principal Component Number", "log(1+c*PC Amplitude/PCmax)",
					this.pcAwindows.scree.getXValues(), this.pcAwindows.scree.getYValues());
			p.setColor(Color.red);
			float[] ax = { (float) i };
			float[] ay = { this.pcAwindows.scree.getYValues()[i - 1] };
			p.addPoints(ax, ay, PlotWindow.X);
			p.addLabel(1.0 * i / this.pcAwindows.scree.getYValues().length, .5,
					"" + (Math.exp(this.pcAwindows.scree.getYValues()[i - 1]) - 1) * this.pcAwindows.sMax / this.pcAwindows.c);
			p.setColor(Color.black);
			this.pcAwindows.scree.drawPlot(p);

			this.pcAwindows.spectrum.drawPlot(this.pcAwindows.spectra[i - 1]);
		}
	}

	public void imageOpened(ImagePlus imp) {
		return;
	}

	public void imageClosed(ImagePlus imp) {
		if (imp == this.pcAwindows.maps)
			this.pcAwindows.maps.removeImageListener(this.pcAwindows.pcal);
	}
}