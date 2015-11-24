package com.DirectElectron.LiveFFT;
import java.util.Vector;

import edu.emory.mathcs.utils.ConcurrencyUtils;

import ij.IJ;
import ij.gui.GenericDialog;


/**
 * Handles the Dialog box that is created for users to set options 
 * related to Live FFT.
 * 
 * @sponsor Direct Electron (http://www.directelectron.com/)
 * @author Sunny Chow (sunny.chow@acm.org)
 *
 */
public class LiveFFTOptionsDlg {
	private int numThreads;
	private Vector<FFTProcessor> fftProcessors;
	private int fftSelection;
	
	/**
	 * Constructor.  Adds all the possible FFTProcessors to be selected
	 * by a user.
	 * 
	 */
	public LiveFFTOptionsDlg()
	{
		// Set default values
		this.numThreads = ConcurrencyUtils.getNumberOfProcessors();
		this.fftProcessors = new Vector<FFTProcessor>();
		this.fftProcessors.add(new FFTAutoscale());
		this.fftProcessors.add(new FFTAutoscaleMinMax());
		this.fftProcessors.add(new FFTAutoscaleAroundMean());
		this.fftProcessors.add(new FFTNoAutoscale());
		this.fftSelection = 0;
	}
	
	/**
	 * Displays a dialog allowing a user to select the number of concurrent threads
	 * as well as the FFTProcessor to use in transforming the image.
	 * 
	 * @return true if successful, false otherwise.
	 */
	public boolean showDialog()
	{
		GenericDialog dlg = new GenericDialog("Live FFT Options");
		dlg.addNumericField("Number of Concurrent Threads",  this.numThreads, 0);
		String[] fftDescriptions = new String[this.fftProcessors.size()];
		for (int i = 0; i < fftProcessors.size(); i++)
			fftDescriptions[i] = fftProcessors.elementAt(i).toString();
		
		dlg.addChoice("FFT Processor", fftDescriptions, this.fftProcessors.elementAt(this.fftSelection).toString());

        // Ugly hack to expose parameters for AutoScale only.  When more parameters are needed, make FFTProcessor expose a common
        // parameter value and add an action listener to the combobox for the generic dialog.
        dlg.addMessage("Parameters for AutoScale only.");
        dlg.addNumericField("Saturation percentage", ((FFTAutoscale) this.fftProcessors.elementAt(0)).SaturationPercentage, 2);
        dlg.addNumericField("Log scale factor", ((FFTAutoscale) this.fftProcessors.elementAt(0)).LogScale, 2);
		
		dlg.showDialog();
        if (dlg.wasCanceled()) return false;
		
		// From Parallel FFTJ
		int nthreads = (int)dlg.getNextNumber();
		// Validate parameters.
        if ((nthreads <= 0) || !(ConcurrencyUtils.isPowerOf2(nthreads))) {
            IJ.error("Number of threads has to be a positive power-of-two number");
            return false;
        } else {
            ConcurrencyUtils.setNumberOfThreads(nthreads);
        }
        
        this.numThreads = nthreads;
        String selection = dlg.getNextChoice();
        for (int i = 0; i < fftProcessors.size(); i++)
        {
        	if (selection.equals(this.fftProcessors.elementAt(i).toString()))
        	{
        		this.fftSelection = i;
        	}
        }
        
        if (this.fftProcessors.elementAt(this.fftSelection) instanceof FFTAutoscale)
        {
        	((FFTAutoscale) this.fftProcessors.elementAt(this.fftSelection)).SaturationPercentage = dlg.getNextNumber();
        	((FFTAutoscale) this.fftProcessors.elementAt(this.fftSelection)).LogScale = dlg.getNextNumber();
        }
        
		return true;
	}
	
	/**
	 * Returns the number of threads the user as elected to use to do the
	 * FFT.
	 * 
	 * @return number of threads.
	 */
	public int getNumberThreads()
	{
		return this.numThreads;
	}
	
	/**
	 * Returns the processor the user as elected to use to do the FFT
	 * processing
	 * 
	 * @return user selected FFTProcessor
	 */
	public FFTProcessor getSelectedFFT()
	{
		return this.fftProcessors.elementAt(this.fftSelection);
	}
}
