package com.spectrumimager.CSI;

import org.ujmp.core.DenseMatrix;
import org.ujmp.core.Matrix;

/*
 * Fit class for a constant function.
 */
class CSI_ConstantFit extends CSI_Fit {
	@Override
	Matrix createFit(double[] x, Matrix y, int start, int end) {
		int s = end - start;
		long col = y.getColumnCount();
		Matrix m = DenseMatrix.Factory.zeros(s, 1);
		Matrix n = DenseMatrix.Factory.zeros(s, col);
		Matrix coeffs = DenseMatrix.Factory.zeros(1, col);

		for (int k = 0; k < s; k++) {
			m.setAsDouble(1, k, 0);
			for (int i = 0; i < col; i++) {
				n.setAsDouble(fy(y.getAsDouble(k + start, i)), k, i);
			}
		}

		coeffs = m.solve(n);
		
		// Basically stolen from the abstract class, since we don't really know
		// how to define the residual for a constant fit
		this.Residual = m.mtimes(coeffs).minus(n);
		
		return (DenseMatrix.Factory.ones(2, 1)).mtimes(coeffs);
	}

	protected double getFitAtX(double c0, double c1, double xi) {
		return c0;
	}

	protected double fx(double xi) {
		return 0;
	}

	protected double fy(double yi) {
		return yi;
	}
}