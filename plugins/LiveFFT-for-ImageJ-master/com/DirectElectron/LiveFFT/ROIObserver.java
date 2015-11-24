package com.DirectElectron.LiveFFT;
import ij.ImagePlus;
import ij.gui.Roi;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

/**
 * Detects and reports all ROI changes for the ImagePlus object
 * that is passed in.
 * 
 * @author sunny
 *
 */

public class ROIObserver implements MouseMotionListener {
	
	/**
	 * Created to detect for ROI changes.
	 * @param imp
	 */
	public ROIObserver (ImagePlus imp )
	{
		imp.getWindow().getCanvas().addMouseMotionListener(this);
		this.image = imp;
		this.listeners = new Vector<ROIListener>(1);
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	public void mouseDragged(MouseEvent arg0) {
		Roi roi = this.image.getRoi();
		if (roi != null && roi.getBounds().width > 1 && roi.getBounds().height > 1)
		{
			this.fireEvent();
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	public void mouseMoved(MouseEvent arg0) {
	}
	

	/**
	 * Adds a listener to be notified whenever a user makes a change in the ROI.
	 * 
	 * @param listener
	 */
	public void addListener(ROIListener listener)
	{
		this.listeners.add(listener);
	}
	
	/**
	 * Removes a listener from being notified of ROI changes.
	 * 
	 * @param listener
	 */
	public void removeListener(ROIListener listener)
	{
		this.listeners.remove(listener);
	}
	
	/**
	 * Informs all listeners that an ROI change event has occurred.
	 */
	private void fireEvent()
	{
		for (ROIListener l : this.listeners) l.roiUpdated(this.image);
	}
	
	
	private ImagePlus image;
	private Vector<ROIListener> listeners;
}
