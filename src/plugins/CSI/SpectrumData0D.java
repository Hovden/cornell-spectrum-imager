package plugins.CSI;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public class SpectrumData0D extends SpectrumData1D {
	SpectrumData0D(CSI_Spectrum_Analyzer csi_Spectrum_Analyzer, ImagePlus img) {
		super(csi_Spectrum_Analyzer, img);
		// TODO Auto-generated constructor stub
	}

	@Override
	double[] getProfile() {
		double[] values = new double[size];
		ImageProcessor ip = img.getProcessor();

		for (int i = 0; i < size; i++) {
			values[i] = ip.getf(i, 0);
		}
		return values;
	}

	@Override
	ImagePlus integrate(int fitStart, int fitEnd, int intStart, int intEnd) {
		ImagePlus imp = super.integrate(fitStart, fitEnd, intStart, intEnd);
		IJ.showMessage(imp.getTitle(), imp.getProcessor().getf(0, 0) + " total " + yLabel);
		return new ImagePlus();
	}
}