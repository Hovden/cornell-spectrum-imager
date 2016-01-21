package plugins.CSI;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/*
 * Event handler for all of the scroll bars.
 */
class ScrollListener implements ChangeListener {

	/**
	 * 
	 */
	private final CSI_Spectrum_Analyzer csi_Spectrum_Analyzer;

	/**
	 * @param csi_Spectrum_Analyzer
	 */
	ScrollListener(CSI_Spectrum_Analyzer csi_Spectrum_Analyzer) {
		this.csi_Spectrum_Analyzer = csi_Spectrum_Analyzer;
	}

	public void stateChanged(ChangeEvent e) {
		Object s = e.getSource();
		if (s == this.csi_Spectrum_Analyzer.sldOffset) { // The offset slider value changed
			this.csi_Spectrum_Analyzer.state.windowOffset = this.csi_Spectrum_Analyzer.sldOffset.getValue();
		} else if (s == this.csi_Spectrum_Analyzer.sldZoom) { // The zoom slider value changed
			this.csi_Spectrum_Analyzer.state.zoomfactor = Math.pow(2.0, this.csi_Spectrum_Analyzer.sldZoom.getValue() / 10.0);
		} else if (s == this.csi_Spectrum_Analyzer.sldLeft) { // The background position slider value
									// changed
			this.csi_Spectrum_Analyzer.state.X0 = this.csi_Spectrum_Analyzer.sldLeft.getValue();
			this.csi_Spectrum_Analyzer.txtLeft.setText(String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.X0]));
			if (this.csi_Spectrum_Analyzer.sldLeft.getValue() + this.csi_Spectrum_Analyzer.sldWidth.getValue() >= this.csi_Spectrum_Analyzer.state.size) {
				this.csi_Spectrum_Analyzer.sldWidth.setValue(this.csi_Spectrum_Analyzer.state.size - this.csi_Spectrum_Analyzer.sldLeft.getValue() - 1);
				this.csi_Spectrum_Analyzer.txtWidth.setText(String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.X1] - this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.X0]));
				this.csi_Spectrum_Analyzer.sldWidth.repaint();
			}
			this.csi_Spectrum_Analyzer.state.X1 = this.csi_Spectrum_Analyzer.sldLeft.getValue() + this.csi_Spectrum_Analyzer.sldWidth.getValue();
		} else if (s == this.csi_Spectrum_Analyzer.sldWidth) { // The background width slider value
									// changed
			this.csi_Spectrum_Analyzer.sldIWidth.repaint();
			if (this.csi_Spectrum_Analyzer.sldLeft.getValue() + this.csi_Spectrum_Analyzer.sldWidth.getValue() >= this.csi_Spectrum_Analyzer.state.size) {
				this.csi_Spectrum_Analyzer.sldWidth.setValue(this.csi_Spectrum_Analyzer.state.size - this.csi_Spectrum_Analyzer.sldLeft.getValue() - 1);
			}
			this.csi_Spectrum_Analyzer.state.X1 = this.csi_Spectrum_Analyzer.sldLeft.getValue() + this.csi_Spectrum_Analyzer.sldWidth.getValue();
			this.csi_Spectrum_Analyzer.txtWidth.setText(String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.X1] - this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.X0]));
		} else if (s == this.csi_Spectrum_Analyzer.sldILeft) { // The integration position slider value
									// changed
			this.csi_Spectrum_Analyzer.state.iX0 = this.csi_Spectrum_Analyzer.sldILeft.getValue();
			this.csi_Spectrum_Analyzer.txtILeft.setText(String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.iX0]));
			if (this.csi_Spectrum_Analyzer.sldILeft.getValue() + this.csi_Spectrum_Analyzer.sldIWidth.getValue() >= this.csi_Spectrum_Analyzer.state.size) {
				this.csi_Spectrum_Analyzer.sldIWidth.setValue(this.csi_Spectrum_Analyzer.state.size - this.csi_Spectrum_Analyzer.sldILeft.getValue() - 1);
				this.csi_Spectrum_Analyzer.txtIWidth.setText(String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.iX1] - this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.iX0]));
				this.csi_Spectrum_Analyzer.sldIWidth.repaint();
			}
			this.csi_Spectrum_Analyzer.state.iX1 = this.csi_Spectrum_Analyzer.sldILeft.getValue() + this.csi_Spectrum_Analyzer.sldIWidth.getValue();
		} else if (s == this.csi_Spectrum_Analyzer.sldIWidth) { // The integration width slider value
										// changed
			if (this.csi_Spectrum_Analyzer.sldILeft.getValue() + this.csi_Spectrum_Analyzer.sldIWidth.getValue() >= this.csi_Spectrum_Analyzer.state.size) {
				this.csi_Spectrum_Analyzer.sldIWidth.setValue(this.csi_Spectrum_Analyzer.state.size - this.csi_Spectrum_Analyzer.sldILeft.getValue() - 1);
				this.csi_Spectrum_Analyzer.sldIWidth.repaint();
			}
			this.csi_Spectrum_Analyzer.state.iX1 = this.csi_Spectrum_Analyzer.sldILeft.getValue() + this.csi_Spectrum_Analyzer.sldIWidth.getValue();
			this.csi_Spectrum_Analyzer.txtIWidth.setText(String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.iX1] - this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.iX0]));
		} else if (s == this.csi_Spectrum_Analyzer.sldCLeft) { // The left calibration slider value
									// changed
			if (this.csi_Spectrum_Analyzer.twoptcalib) {
				if (this.csi_Spectrum_Analyzer.sldCLeft.getValue() > this.csi_Spectrum_Analyzer.sldCRight.getValue()) {
					this.csi_Spectrum_Analyzer.sldCRight.setValue(this.csi_Spectrum_Analyzer.sldCLeft.getValue());
					this.csi_Spectrum_Analyzer.sldCRight.repaint();
				}
			}
			this.csi_Spectrum_Analyzer.state.cX0 = this.csi_Spectrum_Analyzer.sldCLeft.getValue();
			this.csi_Spectrum_Analyzer.txtLeftCalibration.setText(String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.cX0]));
		} else if (s == this.csi_Spectrum_Analyzer.sldCRight) { // The right calibration slider value
										// changed
			if (this.csi_Spectrum_Analyzer.twoptcalib) {
				if (this.csi_Spectrum_Analyzer.sldCRight.getValue() < this.csi_Spectrum_Analyzer.sldCLeft.getValue()) {
					this.csi_Spectrum_Analyzer.sldCLeft.setValue(this.csi_Spectrum_Analyzer.sldCRight.getValue());
					this.csi_Spectrum_Analyzer.sldCLeft.repaint();
				}
			}
			this.csi_Spectrum_Analyzer.state.cX1 = this.csi_Spectrum_Analyzer.sldCRight.getValue();
			this.csi_Spectrum_Analyzer.txtRightCalibration.setText(String.format("%.1f", this.csi_Spectrum_Analyzer.state.x[this.csi_Spectrum_Analyzer.state.cX1]));
		}
		this.csi_Spectrum_Analyzer.state.pwin.pack();
		this.csi_Spectrum_Analyzer.state.updateProfile();
	}
}