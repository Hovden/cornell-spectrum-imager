package com.spectrumimager.CSI;

import java.util.Arrays;

import org.ujmp.core.DenseMatrix;
import org.ujmp.core.Matrix;

/*
 * Fit class for a linear combination of power laws (LCPL)
 */
class CSI_LCPLFit extends CSI_Fit {
	double R1 = 0, R2 = 0;
	double min = .2, max = .8;

	@Override
	Matrix createFit(double[] x, Matrix y, int start, int end) {
		double[] powerLawCoeffs = (new CSI_PowerFit()).createFit(x, y, start, end).toDoubleArray()[1];
		Arrays.sort(powerLawCoeffs);
		R1 = powerLawCoeffs[(int) (powerLawCoeffs.length * min)];
		R2 = Math.min(powerLawCoeffs[(int) (powerLawCoeffs.length * max)], 0);

		int s = end - start;
		long col = y.getColumnCount();
		Matrix m = DenseMatrix.Factory.zeros(s, 2);
		Matrix n = DenseMatrix.Factory.zeros(s, col);
		Matrix coeffs = DenseMatrix.Factory.zeros(2, col);

		for (int k = 0; k < s; k++) {
			m.setAsDouble(Math.pow(x[k + start], R1), k, 0);
			m.setAsDouble(Math.pow(x[k + start], R2), k, 1);
			for (int i = 0; i < col; i++) {
				n.setAsDouble(fy(y.getAsDouble(k + start, i)), k, i);
			}
		}

		coeffs = m.solve(n);
		this.Residual = m.mtimes(coeffs).minus(n);

		return coeffs;
	}

	protected double getFitAtX(double c0, double c1, double xi) {
		if (c0 == 0 && c1 == 0)
			// if (c0==0)
			return 0;
		return c0 * Math.pow(xi, R1) + c1 * Math.pow(xi, R2);
	}

	protected double fx(double xi) {
		return 0;
	}

	protected double fy(double yi) {
		return yi;
	}
}