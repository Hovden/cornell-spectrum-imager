package plugins.CSI;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import ij.gui.GenericDialog;

class EasterEggListener implements MouseListener {
	/**
	 * 
	 */
	private final PCAwindows pcAwindows;

	/**
	 * @param pcAwindows
	 */
	EasterEggListener(PCAwindows pcAwindows) {
		this.pcAwindows = pcAwindows;
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		if (e.isControlDown() && e.isShiftDown()) {
			GenericDialog gd = new GenericDialog("PCA filter");
			gd.addNumericField("Number of components:", 1, 3);
			gd.showDialog();
			if (!gd.wasCanceled())
				this.pcAwindows.filter((int) gd.getNextNumber()).show();
		}
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}
}