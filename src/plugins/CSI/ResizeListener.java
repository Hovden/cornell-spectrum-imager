package plugins.CSI;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import ij.IJ;

class ResizeListener implements ComponentListener {

	/**
	 * 
	 */
	private final SpectrumData spectrumData;

	/**
	 * @param spectrumData
	 */
	ResizeListener(SpectrumData spectrumData) {
		this.spectrumData = spectrumData;
	}

	public void componentResized(ComponentEvent e) {
		this.spectrumData.plotHeight = Math.max(e.getComponent().getSize().height - this.spectrumData.marginHeight, 0);
		this.spectrumData.plotWidth = Math.max(e.getComponent().getSize().width - this.spectrumData.marginWidth, 0);
		IJ.run("Profile Plot Options...",
				"width=" + this.spectrumData.plotWidth + " height=" + this.spectrumData.plotHeight + " minimum=0 maximum=0");
		this.spectrumData.updateProfile();
	}

	public void componentMoved(ComponentEvent e) {
	}

	public void componentShown(ComponentEvent e) {
	}

	public void componentHidden(ComponentEvent e) {
	}

}