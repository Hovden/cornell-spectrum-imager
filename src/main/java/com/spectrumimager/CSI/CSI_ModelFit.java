package com.spectrumimager.CSI;

import org.ujmp.core.DenseMatrix;
import org.ujmp.core.Matrix;

class CSI_ModelFit {
	Matrix[] backgroundsAndEdges = null;

	void createModelNoG(double[] x, Matrix y, int bStart, int bEnd, int eStart, int eEnd, CSI_Fit fit) {
		Matrix bcoeffs = fit.createFit(x, y, bStart, bEnd);

		backgroundsAndEdges = new Matrix[(int) y.getColumnCount()];
		for (int p = 0; p < (int) y.getColumnCount(); p++) {
			backgroundsAndEdges[p] = DenseMatrix.Factory.zeros(bEnd - bStart + eEnd - eStart, 1);
			for (int e = 0; e < bEnd - bStart; e++) {
				backgroundsAndEdges[p].setAsDouble(
						fit.getFitAtX(bcoeffs.getAsDouble(0, p), bcoeffs.getAsDouble(1, p), x[e + bStart]), e, 0);
			}
			for (int e = 0; e < eEnd - eStart; e++) {
				backgroundsAndEdges[p].setAsDouble(
						fit.getFitAtX(bcoeffs.getAsDouble(0, p), bcoeffs.getAsDouble(1, p), x[e + eStart]),
						e + bEnd - bStart, 0);
			}
		}
	}

	double[][] createFitNoG(double[] x, Matrix y, int bStart, int bEnd, int eStart, int eEnd) {
		int sb = bEnd - bStart;
		int se = eEnd - eStart;
		long col = y.getColumnCount();
		Matrix m;
		Matrix n;
		Matrix coeffs;
		double[][] bAndE = new double[2][(int) col];
		double pix;

		for (int p = 0; p < col; p++) {
			m = DenseMatrix.Factory.zeros(sb, 1);
			n = DenseMatrix.Factory.zeros(sb, 1);
			coeffs = DenseMatrix.Factory.zeros(1, 1);
			for (int e = 0; e < sb; e++) {
				m.setAsDouble(backgroundsAndEdges[p].getAsDouble(e, 0), e, 0);
				n.setAsDouble(y.getAsDouble(e + bStart, p), e, 0);
			}
			try {
				coeffs = m.solve(n);
			} catch (Exception e) {
				return bAndE;
			}
			bAndE[0][p] = coeffs.getAsDouble(0, 0);
			pix = 0;
			for (int e = 0; e < se; e++) {
				pix += y.getAsDouble(e + eStart, p) - bAndE[0][p] * backgroundsAndEdges[p].getAsDouble(e + sb, 0);
			}
			bAndE[1][p] = pix;
		}
		return bAndE;
	}
}