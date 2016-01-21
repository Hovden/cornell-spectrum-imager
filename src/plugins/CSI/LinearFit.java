package plugins.CSI;

/*
 * Fit class for a linear function.
 */
class LinearFit extends Fit {
	protected double getFitAtX(double c0, double c1, double xi) {
		return c0 + c1 * xi + ymin - 1;
	}

	protected double fx(double xi) {
		return xi;
	}

	protected double fy(double yi) {
		return yi;
	}
}