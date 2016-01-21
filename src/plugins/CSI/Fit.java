package plugins.CSI;

import org.ujmp.core.DenseMatrix;
import org.ujmp.core.Matrix;

/*
 * Generic least squares fit class.
 */
abstract class Fit {
	double ymin = 1;
	public Matrix Residual;

	Matrix createFit(double[] x, Matrix y, int start, int end) {
		long s = end - start;
		long col = y.getColumnCount();
		Matrix m = DenseMatrix.Factory.zeros(s, 2);
		Matrix n = DenseMatrix.Factory.zeros(s, col);
		Matrix coeffs = DenseMatrix.Factory.zeros(2, col);

		// ymin = (new JamaDenseDoubleMatrix2D(y)).getMinValue();
		// if (ymin>1)
		// ymin = 1;
		// y.plusEquals(new Jama.Matrix(y.getRowDimension(), col, -ymin+1));

		for (int k = 0; k < s; k++) {
			m.setAsDouble(1.0, k, 0);
			m.setAsDouble(fx(x[k + start]), k, 1);
			for (int i = 0; i < col; i++) {
				n.setAsDouble(fy(y.getAsDouble(k + start, i)), k, i);
			}
		}

		coeffs = m.solve(n);
		this.Residual = m.mtimes(coeffs).minus(n);
		return coeffs;
	}

	protected abstract double getFitAtX(double c0, double c1, double xi);

	protected abstract double fx(double xi);

	protected abstract double fy(double yi);
}