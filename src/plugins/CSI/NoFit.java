package plugins.CSI;

import org.ujmp.core.DenseMatrix;
import org.ujmp.core.Matrix;

/*
 * Fit class for when no fitting is desired.
 */
class NoFit extends Fit {
	@Override
	Matrix createFit(double[] x, Matrix y, int start, int end) {
		// No fit doesn't have a well-defined residual, so we're just using 0s
		this.Residual = DenseMatrix.Factory.zeros(end - start, y.getColumnCount());
		return DenseMatrix.Factory.zeros(2, y.getColumnCount());
	}

	protected double getFitAtX(double c0, double c1, double xi) {
		return 0;
	}

	protected double fx(double xi) {
		return 0;
	}

	protected double fy(double yi) {
		return 0;
	}
}