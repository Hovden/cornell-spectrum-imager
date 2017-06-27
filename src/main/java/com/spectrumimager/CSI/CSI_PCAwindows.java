package com.spectrumimager.CSI;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;

class CSI_PCAwindows {
	/**
	 * 
	 */
	private final CSI_Spectrum_Analyzer csi_Spectrum_Analyzer;

	/**
	 * @param csi_Spectrum_Analyzer
	 */
	CSI_PCAwindows(CSI_Spectrum_Analyzer csi_Spectrum_Analyzer) {
		this.csi_Spectrum_Analyzer = csi_Spectrum_Analyzer;
	}

	PlotWindow scree;
	PlotWindow spectrum;
	Plot[] spectra;
	ImagePlus maps;
	CSI_PCAlistener pcal;
	double sMax, c;

	void setup(PlotWindow scree, PlotWindow spectrum, Plot[] spectra, ImagePlus maps, double sMax, double c) {
		this.scree = scree;
		scree.addMouseListener(new CSI_EasterEggListener(this));
		this.spectrum = spectrum;
		this.spectra = spectra;
		this.maps = maps;
		this.sMax = sMax;
		this.c = c;
		pcal = new CSI_PCAlistener(this);
		maps.addImageListener(pcal);
	}

	ImagePlus filter(int components) {
		int depth = spectrum.getXValues().length;
		int width = maps.getWidth();
		int height = maps.getHeight();
		ImageStack imsf = new ImageStack(width, height);
		ImageProcessor ip;
		for (int i = 0; i < depth; i++) {
			ImageStack imscomps = new ImageStack(width, height);
			for (int comp = 0; comp < components; comp++) {
				ip = maps.getStack().getProcessor(comp + 1).duplicate();
				spectrum.drawPlot(spectra[comp]);
				ip.multiply((Math.exp(scree.getYValues()[comp]) - 1) * sMax / c * spectrum.getYValues()[i]);
				imscomps.addSlice("", ip);
			}
			ZProjector zp = new ZProjector(new ImagePlus("", imscomps));
			zp.setMethod(ZProjector.SUM_METHOD);
			zp.doProjection();
			imsf.addSlice("", zp.getProjection().getProcessor());
		}
		ImagePlus impf = new ImagePlus("filtered", imsf);
		impf.setCalibration(this.csi_Spectrum_Analyzer.img.getCalibration());
		impf.getCalibration().zOrigin = -spectrum.getXValues()[0] / impf.getCalibration().pixelDepth;
		return impf;
	}
}