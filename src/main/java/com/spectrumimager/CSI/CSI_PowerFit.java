package com.spectrumimager.CSI;

/*
 * Fit class for a power law function.
 */
class CSI_PowerFit extends CSI_Fit {
	protected double getFitAtX(double c0, double c1, double xi) {
		if (c0 == 0 && c1 == 0)
			return 0;
		return Math.exp(c0 + Math.log(xi) * c1) + ymin - 1;
	}

	protected double fx(double xi) {
		return Math.log(Math.max(1E-3, xi));
	}

	protected double fy(double yi) {
		return Math.log(Math.max(1E-3, yi));
	}
}