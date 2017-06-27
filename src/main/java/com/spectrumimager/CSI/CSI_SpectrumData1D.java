package com.spectrumimager.CSI;

import org.ujmp.core.Matrix;
import org.ujmp.core.calculation.Calculation;
import org.ujmp.core.doublematrix.DenseDoubleMatrix2D;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.ProfilePlot;
import ij.gui.Roi;
import ij.process.ImageProcessor;

public class CSI_SpectrumData1D extends CSI_SpectrumData {

	/**
	 * @param csi_Spectrum_Analyzer
	 */
	CSI_SpectrumData1D(CSI_Spectrum_Analyzer csi_Spectrum_Analyzer, ImagePlus img) {
		super(csi_Spectrum_Analyzer, img);
	}

	int getSize() {
		return img.getWidth();
	}

	ImagePlus fitToModel(int fitStart, int fitEnd, int intStart, int intEnd) {
		return integrate(fitStart, fitEnd, intStart, intEnd);
	}

	ImagePlus fitToBosman(int fitStart, int fitEnd, int intStart, int intEnd) {
		return null;
	}

	ImagePlus integrate(int fitStart, int fitEnd, int intStart, int intEnd) {
		int height = img.getHeight();
		ImageProcessor ip = img.getProcessor();
		ImageProcessor ipint = ip.resize(1, height);
		double pix, c0, c1;
		ImagePlus imgint = new ImagePlus("Integrated from " + String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[intStart]) + " "
				+ this.csi_Spectrum_Analyzer.state.xLabel + " to " + String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[intEnd]) + " " + this.csi_Spectrum_Analyzer.state.xLabel
				+ "of background subtracted via " + this.csi_Spectrum_Analyzer.comFit.getSelectedItem().toString().toLowerCase() + " fit from "
				+ String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[fitStart]) + " " + this.csi_Spectrum_Analyzer.state.xLabel + " to "
				+ String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[fitEnd]) + " " + this.csi_Spectrum_Analyzer.state.xLabel + " " + img.getTitle()
				+ img.getTitle(), ipint);

		Matrix yMat = DenseDoubleMatrix2D.Factory.zeros(size, height);
		for (int k = 0; k < size; k++) {
			updateProgress(k * 1.0 / (2 * size));
			for (int i = 0; i < height; i++) {
				yMat.setAsDouble(ip.getf(k, i), k, i);
			}
		}
		Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);

		for (int i = 0; i < height; i++) {
			updateProgress(i * 1.0 / (2 * size) + .5);
			c0 = coeffs.getAsDouble(0, i);
			c1 = coeffs.getAsDouble(1, i);
			pix = ip.getf(intStart, i) / 2 - fit.getFitAtX(c0, c1, x[intStart]);
			pix += ip.getf(intEnd, i) / 2 - fit.getFitAtX(c0, c1, x[intEnd]);
			for (int j = intStart + 1; j < intEnd; j++) {
				pix += ip.getf(j, i) / 1 - fit.getFitAtX(c0, c1, x[j]);
			}
			ipint.putPixelValue(0, i, pix);
		}
		updateProgress(1);
		imgint.setCalibration(img.getCalibration());
		imgint.resetDisplayRange();
		imgint.setRoi(0, 0, 1, height);
		ProfilePlot pp = new ProfilePlot(imgint, true);
		pp.createWindow();
		return new ImagePlus();
	}

	ImagePlus HCMintegrate(int fitStart, int fitEnd, int intStart, int intEnd) {
		int height = img.getHeight();
		ImageProcessor ip = img.getProcessor();
		ImageProcessor ipint = ip.resize(1, height);
		ImagePlus imgint = new ImagePlus(
				img.getTitle() + " HCM integrated from " + String.format("%.1f", x[intStart]) + " " + xLabel
						+ " to " + String.format("%.1f", x[intEnd]) + " " + xLabel,
				ipint);
		double pix, c0, c1, s, f;

		Matrix yMat = DenseDoubleMatrix2D.Factory.zeros(size, height);
		for (int k = 0; k < size; k++) {
			updateProgress(k * 1.0 / (2 * size));
			for (int i = 0; i < height; i++) {
				yMat.setAsDouble(ip.getf(k, i), k, i);
			}
		}
		Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);

		for (int i = 0; i < height; i++) {
			updateProgress(i * 1.0 / (2 * size) + .5);
			c0 = coeffs.getAsDouble(0, i);
			c1 = coeffs.getAsDouble(1, i);
			f = ip.getf(intStart, i);
			s = f - fit.getFitAtX(c0, c1, x[intStart]);
			pix = s * s / (2 * f);
			f = ip.getf(intEnd, i);
			s = f - fit.getFitAtX(c0, c1, x[intEnd]);
			pix += s * s / (2 * f);
			for (int j = intStart + 1; j < intEnd; j++) {
				f = ip.getf(j, i);
				s = f - fit.getFitAtX(c0, c1, x[j]);
				pix += s * s / f;
			}
			ipint.putPixelValue(0, i, pix);
		}
		updateProgress(1);
		imgint.setCalibration(img.getCalibration());
		imgint.resetDisplayRange();
		return imgint;
	}

	void PCA(int fitStart, int fitEnd, int pcaStart, int pcaEnd) {
		int height = img.getHeight();
		ImageProcessor ip = img.getProcessor();
		ImageStack stackpca;
		Plot[] stackplot;
		double c0, c1;
		double[] pcax = new double[pcaEnd - pcaStart];

		Matrix yMat = DenseDoubleMatrix2D.Factory.zeros(size, height);
		for (int k = 0; k < size; k++) {
			updateProgress(k / (size * 4.0));
			for (int j = 0; j < height; j++) {
				yMat.setAsDouble(ip.getf(k, j), k, j);
			}
		}
		Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);

		yMat = DenseDoubleMatrix2D.Factory.zeros(pcaEnd - pcaStart, height);
		for (int k = pcaStart; k < pcaEnd; k++) {
			updateProgress((k - pcaStart) / ((pcaEnd - pcaStart) * 4.0) + .25);
			for (int j = 0; j < height; j++) {
				c0 = coeffs.getAsDouble(0, j);
				c1 = coeffs.getAsDouble(1, j);
				yMat.setAsDouble(ip.getf(k, j) - fit.getFitAtX(c0, c1, x[k]), k - pcaStart, j);
			}
		}
		pwin.setTitle(
				"(Working: %50) [Doing Singular Value Composition: may take a few minutes.]  CSI: Cornell Spectrum Imager - "
						+ img.getTitle());

		Matrix yMatUJMP = yMat;
		if (this.csi_Spectrum_Analyzer.meanCentering)
			yMatUJMP.center(Calculation.ORIG, Matrix.COLUMN, true);
		Matrix[] USV = yMatUJMP.svd();
		// Jama.SingularValueDecomposition pcasvd = yMat.svd();

		double[] s = new double[Math.min((int) USV[1].getRowCount(), (int) USV[1].getColumnCount())];
		double[] n = new double[s.length];
		double sMax = USV[1].max(Calculation.NEW, Matrix.ALL).getAsDouble((long) 0, (long) 0);
		double c = 1E4;
		for (int i = 0; i < n.length; i++) {
			s[i] = Math.log(1 + c * USV[1].getAsDouble((long) i, (long) i) / sMax);
			n[i] = i + 1;
		}
		double[] xConc = new double[(int) yMat.getColumnCount()];
		for (int i = 0; i < yMat.getColumnCount(); i++) {
			xConc[i] = i;
		}
		Matrix V = USV[2].transpose();
		Matrix U = USV[0].transpose();

		Plot scree = new Plot("Scree Plot", "Principal Component Number", "log(1+c*PC Amplitude/PCmax)", n, s);
		PlotWindow screewin = scree.show();

		System.arraycopy(x, pcaStart, pcax, 0, pcaEnd - pcaStart);
		stackplot = new Plot[s.length];
		stackpca = new ImageStack(scree.getProcessor().getWidth(), scree.getProcessor().getHeight());
		double[] Ui = new double[pcax.length];
		try {
			for (int i = 0; i < s.length; i++) {
				updateProgress(.5 * i / U.getRowCount() + .5);
				stackpca.addSlice("",
						(new Plot("", "position", yLabel, xConc, V.toDoubleArray()[i])).getProcessor());
				for (int j = 0; j < pcax.length; j++) {
					Ui[j] = U.getAsDouble(i, j);
				}
				stackplot[i] = new Plot("PCA Spectra " + img.getTitle(), xLabel, yLabel, pcax, Ui); // The
																									// 1D
																									// Spectra
																									// Plot
			}
		} catch (Exception e) {
			IJ.error(e.toString());
		}
		updateProgress(1);
		ImagePlus maps = new ImagePlus("PCA Concentrations " + img.getTitle(), stackpca);
		maps.show();
		PlotWindow spectrum = stackplot[0].show();
		CSI_PCAwindows PCAw = new CSI_PCAwindows(this.csi_Spectrum_Analyzer);
		PCAw.setup(screewin, spectrum, stackplot, maps, sMax, c);
	}

	void weightedPCA(int fitStart, int fitEnd, int pcaStart, int pcaEnd) {
		int height = img.getHeight();
		ImageProcessor ip = img.getProcessor();
		ImageStack stackpca;
		Plot[] stackplot;
		double c0, c1;
		double[] pcax = new double[pcaEnd - pcaStart];

		Matrix yMat = DenseDoubleMatrix2D.Factory.zeros(size, height);
		for (int k = 0; k < size; k++) {
			updateProgress(k / (size * 4.0));
			for (int j = 0; j < height; j++) {
				yMat.setAsDouble(ip.getf(k, j), k, j);
			}
		}
		Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);

		yMat = DenseDoubleMatrix2D.Factory.zeros(pcaEnd - pcaStart, height);
		for (int k = pcaStart; k < pcaEnd; k++) {
			for (int j = 0; j < height; j++) {
				yMat.setAsDouble(ip.getf(k, j), k - pcaStart, j);
			}
		}
		Matrix yMatUJMP = yMat;

		Matrix g = yMatUJMP.sum(Calculation.NEW, Matrix.COLUMN, true).divide(yMatUJMP.getColumnCount() * 1.0)
				.abs(Calculation.ORIG).power(Calculation.ORIG, -.5);
		Matrix h = yMatUJMP.sum(Calculation.NEW, Matrix.ROW, true).divide(yMatUJMP.getRowCount() * 1.0)
				.abs(Calculation.ORIG).power(Calculation.ORIG, -.5);
		g = g.divide(g.getValueSum());
		h = h.divide(h.getValueSum());

		yMat = DenseDoubleMatrix2D.Factory.zeros(pcaEnd - pcaStart, height);
		for (int k = pcaStart; k < pcaEnd; k++) {
			updateProgress((k - pcaStart) / ((pcaEnd - pcaStart) * 4.0) + .25);
			for (int j = 0; j < height; j++) {
				c0 = coeffs.getAsDouble(0, j);
				c1 = coeffs.getAsDouble(1, j);
				yMat.setAsDouble(ip.getf(k, j) - fit.getFitAtX(c0, c1, x[k]), k - pcaStart, j);
			}
		}

		yMatUJMP = yMat;

		for (int i = 0; i < yMatUJMP.getRowCount(); i++) {
			for (int j = 0; j < yMatUJMP.getColumnCount(); j++) {
				yMatUJMP.setAsDouble(yMatUJMP.getAsDouble(i, j) * g.getAsDouble(i, 0) * h.getAsDouble(0, j), i, j);
			}
		}
		pwin.setTitle(
				"(Working: %50) [Doing Singular Value Composition: may take a few minutes.]  CSI: Cornell Spectrum Imager - "
						+ img.getTitle());

		if (this.csi_Spectrum_Analyzer.meanCentering)
			yMatUJMP.center(Calculation.ORIG, Matrix.ROW, true);
		Matrix[] USV = yMatUJMP.svd();
		// Jama.SingularValueDecomposition pcasvd = yMat.svd();
		Matrix V = USV[2].transpose();
		Matrix U = USV[0].transpose();

		double[] s = new double[Math.min((int) USV[1].getRowCount(), (int) USV[1].getColumnCount())];
		double[] n = new double[s.length];
		double sMax = USV[1].max(Calculation.NEW, Matrix.ALL).getAsDouble((long) 0, (long) 0);
		double c = 1E4;
		try {
			for (int i = 0; i < n.length; i++) {
				s[i] = Math.log(1 + c * USV[1].getAsDouble((long) i, (long) i) / sMax);
				n[i] = i + 1;
				for (int j = 0; j < yMatUJMP.getColumnCount(); j++) {
					V.setAsDouble(V.getAsDouble(i, j) / h.getAsDouble(0, j), i, j);
				}
				for (int j = 0; j < yMatUJMP.getRowCount(); j++) {
					U.setAsDouble(U.getAsDouble(i, j) / g.getAsDouble(i, 0), i, j);
				}
			}
		} catch (Exception e) {
			IJ.error(e.toString());
		}
		double[] xConc = new double[(int) yMat.getColumnCount()];
		for (int i = 0; i < yMat.getColumnCount(); i++) {
			xConc[i] = i;
		}

		Plot scree = new Plot("Scree Plot", "Principal Component Number", "log(1+c*PC Amplitude/PCmax)", n, s);
		PlotWindow screewin = scree.show();

		System.arraycopy(x, pcaStart, pcax, 0, pcaEnd - pcaStart);
		stackplot = new Plot[s.length];
		stackpca = new ImageStack(scree.getProcessor().getWidth(), scree.getProcessor().getHeight());
		double[] Ui = new double[pcax.length];
		try {
			for (int i = 0; i < s.length; i++) {
				updateProgress(.5 * i / U.getRowCount() + .5);
				stackpca.addSlice("", (new Plot("", xLabel, yLabel, xConc, V.toDoubleArray()[i])).getProcessor());
				for (int j = 0; j < pcax.length; j++) {
					Ui[j] = U.getAsDouble(i, j);
				}
				stackplot[i] = new Plot("PCA Spectra " + img.getTitle(), xLabel, yLabel, pcax, Ui);
			}
		} catch (Exception e) {
			IJ.error(e.toString());
		}
		updateProgress(1);
		ImagePlus maps = new ImagePlus("PCA Concetrations " + img.getTitle(), stackpca);
		maps.show();
		PlotWindow spectrum = stackplot[0].show();
		CSI_PCAwindows PCAw = new CSI_PCAwindows(this.csi_Spectrum_Analyzer);
		PCAw.setup(screewin, spectrum, stackplot, maps, sMax, c);
	}

	ImagePlus subtract(int fitStart, int fitEnd) {
		int height = img.getHeight();
		ImageProcessor ip = img.getProcessor();
		ImageProcessor ipsub = ip.createProcessor(size, height);
		ImagePlus imgsub = new ImagePlus(
				"Background subtracted via " + this.csi_Spectrum_Analyzer.comFit.getSelectedItem().toString().toLowerCase() + " fit from "
						+ String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[fitStart]) + " " + this.csi_Spectrum_Analyzer.state.xLabel + " to "
						+ String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[fitEnd]) + " " + this.csi_Spectrum_Analyzer.state.xLabel + " " + img.getTitle(),
				ipsub);
		double c0, c1;

		Matrix yMat = DenseDoubleMatrix2D.Factory.zeros(size, height);
		for (int k = 0; k < size; k++) {
			updateProgress(k * 1.0 / (2 * size));
			for (int j = 0; j < height; j++) {
				yMat.setAsDouble(ip.getf(k, j), k, j);
			}
		}
		Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);

		for (int i = 0; i < height; i++) {
			updateProgress(i * 1.0 / (2 * size) + .5);
			c0 = coeffs.getAsDouble(0, i);
			c1 = coeffs.getAsDouble(1, i);
			for (int j = fitStart; j < size; j++) {
				ipsub.putPixelValue(j, i, ip.getf(j, i) / 1 - fit.getFitAtX(c0, c1, x[j]));
			}
		}
		updateProgress(1);
		imgsub.setCalibration(img.getCalibration());
		imgsub.resetDisplayRange();
		return imgsub;
	}

	double[] getProfile() {
		Roi roi = img.getRoi();
		if (roi == null)
			return null;
		int ystart = (int) roi.getBounds().getY();
		int yend = ystart + (int) roi.getBounds().getHeight();
		double[] values = new double[size];
		ImageProcessor ip = img.getProcessor();

		for (int i = 0; i < size; i++) {
			values[i] = 0;
			for (int j = ystart; j < yend; j++) {
				values[i] += ip.getf(i, j);
			}
			values[i] /= (yend - ystart);
		}
		return values;
	}

	void recalibrateImage() {
		return;
	}
}