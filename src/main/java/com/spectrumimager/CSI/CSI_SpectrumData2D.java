package com.spectrumimager.CSI;

import org.ujmp.core.Matrix;
import org.ujmp.core.calculation.Calculation;
import org.ujmp.core.doublematrix.DenseDoubleMatrix2D;
import org.ujmp.core.doublematrix.impl.BlockDenseDoubleMatrix2D;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.util.Tools;

// Class for 2D spectrum maps.
public class CSI_SpectrumData2D extends CSI_SpectrumData {

	CSI_SpectrumData2D(CSI_Spectrum_Analyzer csi_Spectrum_Analyzer, ImagePlus img) {
		super(csi_Spectrum_Analyzer, img);
	}

	int getSize() {
		return img.getStackSize();
	}

	ImagePlus fitToBosman(int fitStart, int fitEnd, int intStart, int intEnd) {
		CSI_ModelFit mf = new CSI_ModelFit();
		int width = img.getWidth();
		int height = img.getHeight();
		ImageStack stack = img.getStack();
		ImageProcessor ip;
		ImagePlus bos;

		DenseDoubleMatrix2D yMat = new BlockDenseDoubleMatrix2D(fitEnd - fitStart, width * height);
		for (int k = fitStart; k < fitEnd; k++) {
			updateProgress(k * 1.0 / (2 * (fitEnd - fitStart)));
			ip = stack.getProcessor(k + 1);
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					yMat.setDouble(ip.getf(j, i), k - fitStart, width * i + j);
				}
			}
		}

		DenseDoubleMatrix2D yMatUJMP = yMat;
		Matrix[] USV = yMatUJMP.svd();
		Matrix S = USV[1];
		GenericDialog gd = new GenericDialog("How many components for Bosman?");
		gd.addNumericField("Number of components:", 1, 3);
		gd.showDialog();
		int comp = (int) gd.getNextNumber();
		for (int i = comp; i < S.getRowCount(); i++) {
			updateProgress(i * 1.0 / (2 * (S.getRowCount())));
			S.setAsDouble(0.0, (long) i, (long) i);
		}
		ImageStack stackfilt = new ImageStack(0, 0);
		try {
			Matrix filt = USV[2].mtimes(Calculation.NEW, true,
					S.mtimes(Calculation.NEW, true, USV[0].transpose(Calculation.NEW)));
			filt = filt.transpose(Calculation.NEW);
			stackfilt = new ImageStack(width, height);
			for (int i = 0; i < size; i++) {
				updateProgress(i * 1.0 / (2 * (size)) + .5);
				if (i >= fitStart && i < fitEnd)
					stackfilt.addSlice("", new FloatProcessor(width, height, filt.toDoubleArray()[i - fitStart]));
				else
					stackfilt.addSlice("", new FloatProcessor(width, height));
			}

		} catch (Exception e) {
			IJ.error(e.toString());
		}
		bos = new ImagePlus("bosman", stackfilt);
		bos.setCalibration(img.getCalibration());
		bos.getCalibration().zOrigin = -x[fitStart] / bos.getCalibration().pixelDepth;
		bos.show();

		return bos;
	}

	ImagePlus fitToModel(int fitStart, int fitEnd, int intStart, int intEnd) {
		CSI_ModelFit mf = new CSI_ModelFit();
		int width = img.getWidth();
		int height = img.getHeight();
		ImageStack stack = img.getStack();
		double filtersize = 0;
		try {
			filtersize = Double.parseDouble(this.csi_Spectrum_Analyzer.txtOversampling.getText());
		} catch (NumberFormatException nfe) {
			this.csi_Spectrum_Analyzer.txtOversampling.setText("0.0");
		}
		if (filtersize > 0) {
			img.saveRoi();
			img.setRoi(0, 0, width, height);
			ImagePlus imgfilter = img.duplicate();
			img.restoreRoi();
			// IJ.run(imgfilter, "Median...", "radius="+filtersize+"1
			// stack");
			IJ.run(imgfilter, "Gaussian Blur...", "sigma=" + filtersize * 0.42466 + " stack");
			stack = imgfilter.getStack();
		}
		ImageProcessor ip;
		ImageProcessor ipcoeff1;

		Matrix yMat = DenseDoubleMatrix2D.Factory.zeros(size, width * height);
		for (int k = 0; k < size; k++) {
			updateProgress(k * 1.0 / (2 * size));
			ip = stack.getProcessor(k + 1);
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					yMat.setAsDouble(ip.getf(j, i), k, width * i + j);
				}
			}
		}
		mf.createModelNoG(x, yMat, fitStart, fitEnd, intStart, intEnd, fit);

		// IJ.run("Convolve...", "text1=[-1 -4 -6 -4 -1\n-4 -16 -24 -16
		// -4\n-5 -20 -30 -20 -5\n0 0 0 0 0\n5 20 30 20 5\n4 16 24 16 4\n1 4
		// 6 4 1\n] normalize stack");
		// IJ.run("Convolve...", "text1=[-1 -4 -5 0 5 4 1\n-4 -6 -20 0 20 16
		// 4\n-6 -24 -30 0 30 24 6\n-4 -6 -20 0 20 16 4\n-1 -4 -5 0 5 4 1\n]
		// normalize stack");
		stack = img.getStack();
		for (int k = 0; k < size; k++) {
			updateProgress(k * 1.0 / (2 * size) + .5);
			ip = stack.getProcessor(k + 1);
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					yMat.setAsDouble(ip.getf(j, i), k, width * i + j);
				}
			}
		}
		double[][] coeffs;
		coeffs = mf.createFitNoG(x, yMat, fitStart, fitEnd, intStart, intEnd);
		updateProgress(1);
		ipcoeff1 = new FloatProcessor(width, height, coeffs[1]);
		return new ImagePlus(
				"Integrated from " + String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[intStart]) + " " + this.csi_Spectrum_Analyzer.state.xLabel + " to "
						+ String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[intEnd]) + " " + this.csi_Spectrum_Analyzer.state.xLabel + " of "
						+ String.format("%.1f", filtersize) + " oversampled background subtracted via "
						+ this.csi_Spectrum_Analyzer.comFit.getSelectedItem().toString().toLowerCase() + " fit from "
						+ String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[fitStart]) + " " + this.csi_Spectrum_Analyzer.state.xLabel + " to "
						+ String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[fitEnd]) + " " + this.csi_Spectrum_Analyzer.state.xLabel + " " + img.getTitle(),
				ipcoeff1);

	}

	ImagePlus subtract(int fitStart, int fitEnd) {
		int width = img.getWidth();
		int height = img.getHeight();
		ImageStack stack = img.getStack();
		double filtersize = 0;
		try {
			filtersize = Double.parseDouble(this.csi_Spectrum_Analyzer.txtOversampling.getText());
		} catch (NumberFormatException nfe) {
			this.csi_Spectrum_Analyzer.txtOversampling.setText("0.0");
		}
		if (filtersize > 0) {
			img.saveRoi();
			img.setRoi(0, 0, width, height);
			ImagePlus imgfilter = img.duplicate();
			img.restoreRoi();
			// IJ.run(imgfilter, "Median...", "radius="+filtersize+"
			// stack");
			IJ.run(imgfilter, "Gaussian Blur...", "radius=" + filtersize * 0.42466 + " stack");
			stack = imgfilter.getStack();
		}
		ImageStack stacksub = new ImageStack(width, height);
		ImageStack stackresidual = new ImageStack(width, height);
		ImageProcessor ipsub;
		ImageProcessor ip;
		ImageProcessor ipresidual = new FloatProcessor(1, 1);
		ImagePlus imgsub;
		double pix;

		Matrix yMat = DenseDoubleMatrix2D.Factory.zeros(size, width * height);
		for (int k = 0; k < size; k++) {
			updateProgress(k * 1.0 / (2 * size));
			ip = stack.getProcessor(k + 1);
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					yMat.setAsDouble(ip.getf(i, j), k, height * i + j);
				}
			}
		}
		Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);
		stack = img.getStack();

		// ipcoeff0 = new FloatProcessor(height, width,
		// coeffs.getArray()[0]);
		// (new ImagePlus("coeff0",ipcoeff0 )).show();
		// ipcoeff1 = new FloatProcessor(height, width,
		// coeffs.getArray()[1]);
		// (new ImagePlus("coeff1",ipcoeff1 )).show();

		for (int k = 0; k < size; k++) 
		{
			updateProgress(k * 1.0 / (2 * size) + .5);
			if (k < fitStart) 
			{
				stacksub.addSlice(stack.getSliceLabel(k + 1),
						stack.getProcessor(k + 1).createProcessor(width, height));
			} else {
				ipsub = stack.getProcessor(k + 1).duplicate();
				stacksub.addSlice(stack.getSliceLabel(k + 1), ipsub);
				if (k > fitStart && k < fitEnd) 
				{
					ipresidual = stack.getProcessor(k + 1).duplicate();
					stackresidual.addSlice(stack.getSliceLabel(k + 1), ipresidual);
				}
				for (int i = 0; i < width; i++) 
				{
					for (int j = 0; j < height; j++) 
					{
						pix = ipsub.getPixelValue(i, j)
								- fit.getFitAtX(coeffs.getAsDouble(0, height * i + j), coeffs.getAsDouble(1, height * i + j), x[k]);
						ipsub.putPixelValue(i, j, pix);
						if (k > fitStart && k < fitEnd)
						{
							ipresidual.putPixelValue(i, j,
									Math.exp(fit.Residual.getAsDouble(k - fitStart, height * i + j)));
						}
					}
				}
			}
		}
		// (new ImagePlus("Residual", stackresidual)).show();

		updateProgress(1);
		imgsub = new ImagePlus(
				"Background subtracted via " + this.csi_Spectrum_Analyzer.comFit.getSelectedItem().toString().toLowerCase() + " fit from "
						+ String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[fitStart]) + " " + this.csi_Spectrum_Analyzer.state.xLabel + " to "
						+ String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[fitEnd]) + " " + this.csi_Spectrum_Analyzer.state.xLabel + " " + img.getTitle(),
				stacksub);
		imgsub.setCalibration(img.getCalibration());
		imgsub.resetDisplayRange();

		return imgsub;
	}

	ImagePlus integrate(int fitStart, int fitEnd, int intStart, int intEnd) {
		int width = img.getWidth();
		int height = img.getHeight();
		ImageStack stack = img.getStack();
		// if (medianSize == 0){
		// stack = fitToBosman(fitStart, fitEnd, intStart,
		// intEnd).getStack();
		// }
		ImageProcessor ip;
		ImageProcessor ipint = stack.getProcessor(1).createProcessor(width, height);
		ImagePlus imgint = new ImagePlus(
				"Integrated from " + String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[intStart]) + " " + this.csi_Spectrum_Analyzer.state.xLabel + " to "
						+ String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[intEnd]) + " " + this.csi_Spectrum_Analyzer.state.xLabel
						+ " of background subtracted via " + this.csi_Spectrum_Analyzer.comFit.getSelectedItem().toString().toLowerCase()
						+ " fit from " + String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[fitStart]) + " " + this.csi_Spectrum_Analyzer.state.xLabel + " to "
						+ String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[fitEnd]) + " " + this.csi_Spectrum_Analyzer.state.xLabel + " " + img.getTitle(),
				ipint);
		double pix, c0, c1;

		Matrix yMat = DenseDoubleMatrix2D.Factory.zeros(size, width * height);
		for (int k = 0; k < size; k++) {
			updateProgress(k * 1.0 / (2 * size));
			ip = stack.getProcessor(k + 1);
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					yMat.setAsDouble(ip.getf(i, j), k, height * i + j);
				}
			}
		}
		Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);
		stack = img.getStack();

		for (int i = 0; i < width; i++) {
			updateProgress(.5 + i * 1.0 / (2 * width));
			for (int j = 0; j < height; j++) {
				c0 = coeffs.getAsDouble(0, height * i + j);
				c1 = coeffs.getAsDouble(1, height * i + j);
				pix = stack.getProcessor(intStart + 1).getf(i, j);
				pix -= fit.getFitAtX(c0, c1, x[intStart]);
				pix += stack.getProcessor(intEnd + 1).getf(i, j);
				pix -= fit.getFitAtX(c0, c1, x[intEnd]);
				for (int k = intStart + 1; k < intEnd - 1; k++) {
					pix += stack.getProcessor(k + 1).getf(i, j);
					pix -= fit.getFitAtX(c0, c1, x[k]);
				}
				ipint.putPixelValue(i, j, pix);
			}
		}
		imgint.setCalibration(img.getCalibration());
		imgint.resetDisplayRange();
		updateProgress(1);
		return imgint;
	}

	ImagePlus HCMintegrate(int fitStart, int fitEnd, int intStart, int intEnd) {
		int width = img.getWidth();
		int height = img.getHeight();
		ImageStack stack = img.getStack();
		ImageProcessor ip;
		ImageProcessor ipint = stack.getProcessor(1).duplicate();
		ImagePlus imgint = new ImagePlus(
				img.getTitle() + " HCM integrated from " + String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[intStart]) + " "
						+ this.csi_Spectrum_Analyzer.state.xLabel + " to " + String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[intEnd]) + " " + this.csi_Spectrum_Analyzer.state.xLabel,
				ipint);
		double pix, c0, c1, s, f;

		Matrix yMat = DenseDoubleMatrix2D.Factory.zeros(size, width * height);
		for (int k = 0; k < size; k++) {
			updateProgress(k * 1.0 / (2 * size));
			ip = stack.getProcessor(k + 1);
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < height; j++) {
					yMat.setAsDouble(ip.getf(i, j), k, height * i + j);
				}
			}
		}
		Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);

		for (int i = 0; i < width; i++) {
			updateProgress(.5 + i * 1.0 / (2 * width));
			for (int j = 0; j < height; j++) {
				c0 = coeffs.getAsDouble(0, height * i + j);
				c1 = coeffs.getAsDouble(1, height * i + j);
				f = stack.getProcessor(intStart + 1).getf(i, j);
				s = f - fit.getFitAtX(c0, c1, x[intStart]);
				pix = s * s / (2 * f);
				f = stack.getProcessor(intEnd + 1).getf(i, j);
				s = f - fit.getFitAtX(c0, c1, x[intEnd]);
				pix += s * s / (2 * f);
				for (int k = intStart + 1; k < intEnd - 1; k++) {
					f = stack.getProcessor(k + 1).getf(i, j);
					s = f - fit.getFitAtX(c0, c1, x[k]);
					pix += s * s / f;
				}
				ipint.putPixelValue(i, j, pix);
			}
		}
		imgint.setCalibration(img.getCalibration());
		imgint.resetDisplayRange();
		updateProgress(1);
		return imgint;
	}

	void PCA(int fitStart, int fitEnd, int pcaStart, int pcaEnd) {
		int width = img.getWidth();
		int height = img.getHeight();
		ImageStack stack = img.getStack();

		double filtersize = 0;
		try {
			filtersize = Double.parseDouble(this.csi_Spectrum_Analyzer.txtOversampling.getText());
		} catch (NumberFormatException nfe) {
			this.csi_Spectrum_Analyzer.txtOversampling.setText("0.0");
		}
		if (filtersize > 0) {
			img.saveRoi();
			img.setRoi(0, 0, width, height);
			ImagePlus imgfilter = img.duplicate();
			img.restoreRoi();
			// IJ.run(imgfilter, "Median...", "radius="+filtersize+"
			// stack");
			IJ.run(imgfilter, "Gaussian Blur...", "radius=" + filtersize * 0.42466 + " stack");
			stack = imgfilter.getStack();
		}

		ImageProcessor ip;
		ImageStack stackpca;
		Plot[] stackplot;
		double c0, c1;
		double[] pcax = new double[pcaEnd - pcaStart];

		Matrix yMat = DenseDoubleMatrix2D.Factory.zeros(size, width * height);
		for (int k = 0; k < size; k++) {
			updateProgress(k / (size * 4.0));
			ip = stack.getProcessor(k + 1);
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					yMat.setAsDouble(ip.getf(i, j), k, width * j + i);
				}
			}
		}
		Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);

		stack = img.getStack();
		yMat = DenseDoubleMatrix2D.Factory.zeros(pcaEnd - pcaStart, width * height);
		for (int k = pcaStart; k < pcaEnd; k++) {
			updateProgress((k - pcaStart) / ((pcaEnd - pcaStart) * 4.0) + .25);
			ip = stack.getProcessor(k + 1);
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					c0 = coeffs.getAsDouble(0, width * j + i);
					c1 = coeffs.getAsDouble(1, width * j + i);
					yMat.setAsDouble(ip.getf(i, j) - fit.getFitAtX(c0, c1, x[k]), k - pcaStart, width * j + i);
				}
			}
		}
		pwin.setTitle(
				"(Working: %50) [Doing Singular Value Composition: may take a few minutes.]  CSI: Cornell Spectrum Imager - "
						+ img.getTitle());
		
		Matrix yMatUJMP = yMat;
		if (this.csi_Spectrum_Analyzer.meanCentering)
			yMatUJMP.center(Calculation.ORIG, Matrix.ROW, true);
		Matrix[] USV = yMatUJMP.svd();
		Matrix S = USV[1];
		// Jama.SingularValueDecomposition pcasvd = yMat.svd();

		double[] s = new double[Math.min((int) USV[1].getRowCount(), (int) USV[1].getColumnCount())];
		double[] n = new double[s.length];
		double sMax = USV[1].max(Calculation.NEW, Matrix.ALL).getAsDouble((long) 0, (long) 0);
		double c = 1E4;
		for (int i = 0; i < n.length; i++) {
			s[i] = Math.log(1 + c * USV[1].getAsDouble((long) i, (long) i) / sMax);
			n[i] = i + 1;
			if (i < 3)
				S.setAsDouble(0, (long) i, (long) i);
		}
		Matrix V = USV[2];
		Matrix Vt = V.transpose();
		Matrix U = USV[0];
		Matrix Ut = U.transpose();
		// try{
		// Matrix resid = (S.mtimes(Calculation.NEW, true, Vt));
		// resid = resid.sum(Calculation.NEW, Matrix.ROW, true);
		// (new ImagePlus(img.getTitle()+" residual", new
		// FloatProcessor(width, height, resid.toDoubleArray()[0]))).show();
		// } catch (Exception e) {
		// IJ.error(e.toString());
		// }
		// try{
		// Matrix smoothed = V.mtimes(Calculation.NEW, true,
		// S.mtimes(Calculation.NEW, true, U));
		// smoothed = smoothed.transpose();
		// stacksmooth = new ImageStack(width, height);
		// for (int i = 0; i<smoothed.getRowCount(); i++) {
		// stacksmooth.addSlice("", new FloatProcessor(width, height,
		// smoothed.toDoubleArray()[i]));
		// }
		// (new ImagePlus(img.getTitle()+" PCA smoothed!",
		// stacksmooth)).show();
		// } catch (Exception e) {
		// IJ.error(e.toString());
		// }

		Plot scree = new Plot("Scree Plot", "Principal Component Number", "log(1+c*PC Amplitude/PCmax)", n, s);
		PlotWindow screewin = scree.show();

		System.arraycopy(x, pcaStart, pcax, 0, pcaEnd - pcaStart);
		stackplot = new Plot[s.length];
		stackpca = new ImageStack(width, height);
		double[] Ui = new double[pcax.length];
		try {
			for (int i = 0; i < s.length; i++) {
				updateProgress(.5 * i / Ut.getRowCount() + .5);
				stackpca.addSlice("", new FloatProcessor(width, height, Vt.toDoubleArray()[i]));
				for (int j = 0; j < pcax.length; j++) {
					Ui[j] = Ut.getAsDouble(i, j);
				}
				stackplot[i] = new Plot("PCA Spectra " + img.getTitle(), xLabel, yLabel, pcax, Ui);
			}
		} catch (Exception e) {
			IJ.error("wacky " + e.toString());
		}
		updateProgress(1);
		ImagePlus maps = new ImagePlus("PCA Concentrations " + img.getTitle(), stackpca);
		maps.show();
		maps.resetDisplayRange();
		PlotWindow spectrum = stackplot[0].show();
		CSI_PCAwindows PCAw = new CSI_PCAwindows(this.csi_Spectrum_Analyzer);
		PCAw.setup(screewin, spectrum, stackplot, maps, sMax, c);
	}

	void weightedPCA(int fitStart, int fitEnd, int pcaStart, int pcaEnd) {
		int width = img.getWidth();
		int height = img.getHeight();
		ImageStack stack = img.getStack();
		ImageProcessor ip;
		ImageStack stackpca;
		Plot[] stackplot;
		double c0, c1;
		double[] pcax = new double[pcaEnd - pcaStart];

		Matrix yMat = DenseDoubleMatrix2D.Factory.zeros(size, width * height);
		for (int k = 0; k < size; k++) {
			updateProgress(k / (size * 4.0));
			ip = stack.getProcessor(k + 1);
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					yMat.setAsDouble(ip.getf(i, j), k, width * j + i);
				}
			}
		}
		Matrix coeffs = fit.createFit(x, yMat, fitStart, fitEnd);

		yMat = DenseDoubleMatrix2D.Factory.zeros(pcaEnd - pcaStart, width * height);
		for (int k = pcaStart; k < pcaEnd; k++) {
			ip = stack.getProcessor(k + 1);
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					yMat.setAsDouble(ip.getf(i, j), k - pcaStart, width * j + i);
				}
			}
		}
		Matrix yMatUJMP = yMat;

		Matrix g = yMatUJMP.sum(Calculation.NEW, Matrix.COLUMN, true).divide(yMatUJMP.getColumnCount() * 1.0)
				.abs(Calculation.ORIG).power(Calculation.ORIG, -.5);
		Matrix h = yMatUJMP.sum(Calculation.NEW, Matrix.ROW, true).divide(yMatUJMP.getRowCount() * 1.0)
				.abs(Calculation.ORIG).power(Calculation.ORIG, -.5);
		g = g.divide(g.getValueSum());
		h = h.divide(h.getValueSum());

		yMat = DenseDoubleMatrix2D.Factory.zeros(pcaEnd - pcaStart, width * height);
		for (int k = pcaStart; k < pcaEnd; k++) {
			updateProgress((k - pcaStart) / ((pcaEnd - pcaStart) * 4.0) + .25);
			ip = stack.getProcessor(k + 1);
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					c0 = coeffs.getAsDouble(0, width * j + i);
					c1 = coeffs.getAsDouble(1, width * j + i);
					yMat.setAsDouble(ip.getf(i, j) - fit.getFitAtX(c0, c1, x[k]), k - pcaStart, width * j + i);
				}
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
		pwin.setTitle("(Working: %50) CSI: Cornell Spectrum Imager - " + img.getTitle());

		Matrix Vt = USV[2].transpose();
		Matrix U = USV[0];
		double[] s = new double[Math.min((int) USV[1].getRowCount(), (int) USV[1].getColumnCount())];
		double[] n = new double[s.length];
		double sMax = USV[1].max(Calculation.NEW, Matrix.ALL).getAsDouble((long) 0, (long) 0);
		double c = 1E4;
		try {
			for (int i = 0; i < n.length; i++) {
				s[i] = Math.log(1 + c * USV[1].getAsDouble((long) i, (long) i) / sMax);
				n[i] = i + 1;
				for (int j = 0; j < yMatUJMP.getColumnCount(); j++) {
					Vt.setAsDouble(Vt.getAsDouble(i, j) / h.getAsDouble(0, j), i, j);
				}
				for (int j = 0; j < yMatUJMP.getRowCount(); j++) {
					U.setAsDouble(U.getAsDouble(j, i) / g.getAsDouble(i, 0), j, i);
				}
			}
		} catch (Exception e) {
			IJ.error(e.toString());
		}

		Plot scree = new Plot("Scree Plot", "Principal Component Number", "log(1+c*PC Amplitude/PCmax)", n, s,
				Plot.DOT);
		PlotWindow screewin = scree.show();

		System.arraycopy(x, pcaStart, pcax, 0, pcaEnd - pcaStart);
		stackplot = new Plot[s.length];
		stackpca = new ImageStack(width, height);
		double[] Ui = new double[pcax.length];

		try {
			for (int i = 0; i < s.length; i++) {
				updateProgress(.5 * i / U.getRowCount() + .5);
				stackpca.addSlice("", new FloatProcessor(width, height, Vt.toDoubleArray()[i]));
				for (int j = 0; j < pcax.length; j++) {
					Ui[j] = U.getAsDouble(j, i);
				}
				stackplot[i] = new Plot("PCA Spectra " + img.getTitle(), xLabel, yLabel, pcax, Ui);
			}
		} catch (Exception e) {
			IJ.error(e.toString());
		}
		updateProgress(1);

		ImagePlus maps = new ImagePlus("PCA Concentrations " + img.getTitle(), stackpca);
		maps.show();
		maps.resetDisplayRange();
		PlotWindow spectrum = stackplot[0].show();
		CSI_PCAwindows PCAw = new CSI_PCAwindows(this.csi_Spectrum_Analyzer);
		PCAw.setup(screewin, spectrum, stackplot, maps, sMax, c);
	}

	double[] getProfile() {
		Roi roi = img.getRoi();
		if (roi == null) {
			return null;
		}
		ImageStack stack = img.getStack();
		double[] values = new double[size];
		Calibration cal = img.getCalibration();
		ImageProcessor ip;
		ImageStatistics stats;
		for (int i = 1; i <= size; i++) {
			ip = stack.getProcessor(i);
			ip.setRoi(roi);
			stats = ImageStatistics.getStatistics(ip, MEAN, cal);
			values[i - 1] = (double) stats.mean;
		}
		double[] extrema = Tools.getMinMax(values);
		if (Math.abs(extrema[1]) == Double.MAX_VALUE) {
			return null;
		} else {
			return values;
		}
	}

	void recalibrateImage() {

		ImageStack ims = img.getStack();
		for (int i = 0; i < x.length; i++) {
			ims.setSliceLabel("(" + x[i] + " " + xLabel + ")", i);
		}
		img.setStack(ims);

	}

}