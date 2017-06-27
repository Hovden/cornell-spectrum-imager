package com.spectrumimager.CSI;

import ij.*;
import ij.plugin.*;
import java.io.*;

// Plugin to handle file types which are not implemented
// directly in ImageJ through io.Opener
// NB: since there is no _ in the name it will not appear in Plugins menu
// -----
// Can be user modified so that your own specialised file types
// can be opened through File ... Open
// OR by drag and drop onto the ImageJ main panel
// OR by double clicking in the MacOS 9/X Finder
// -----
// Go to the point marked MODIFY HERE and modify to
// recognise and load your own file type
// -----
// Gregory Jefferis - 030629
// jefferis@stanford.edu

/**
 * Plugin to handle file types which are not implemented
 * directly in ImageJ through io.Opener.
 */
public class CSI_HandleExtraFileTypes extends ImagePlus implements PlugIn {
	static final int IMAGE_OPENED = -1;
	static final int PLUGIN_NOT_FOUND = -2;

	/** Called from io/Opener.java. */
	public void run(String path) {
		if (path.equals("")) return;
		File theFile = new File(path);
		String directory = theFile.getParent();
		String fileName = theFile.getName();
		if (directory == null) directory = "";

		// Try and recognise file type and load the file if recognised
		ImagePlus imp = openImage(directory, fileName, path);
		if (imp==null) {
			IJ.showStatus("");
			return; // failed to load file or plugin has opened and displayed it
		}

		ImageStack stack = imp.getStack();
		// get the title from the stack (falling back to the fileName)
		String title=imp.getTitle().equals("")?fileName:imp.getTitle();
		// set the stack of this HandleExtraFileTypes object
		// to that attached to the ImagePlus object returned by openImage()
		setStack(title, stack);
		// copy over the calibration info since it doesn't come with the ImageProcessor
		setCalibration(imp.getCalibration());
		// also copy the Show Info field over if it exists
		if (imp.getProperty("Info") != null)
			setProperty("Info", imp.getProperty("Info"));
		// copy the FileInfo
		setFileInfo(imp.getOriginalFileInfo());
		// copy dimensions
		if (IJ.getVersion().compareTo("1.38s")>=0)
			setDimensions(imp.getNChannels(), imp.getNSlices(), imp.getNFrames());
		if (IJ.getVersion().compareTo("1.41o")>=0)
			setOpenAsHyperStack(imp.getOpenAsHyperStack());
         
	}
	

	private Object tryOpen(String directory, String name, String path) {
		// set up a stream to read in 132 bytes from the file header
		// These can be checked for "magic" values which are diagnostic
		// of some image types
        InputStream is;
		byte[] buf = new byte[132];
        
		try {
			if (0 == path.indexOf("http://"))
				is = new java.net.URL(path).openStream();
			else
				is = new FileInputStream(path);
			is.read(buf, 0, 132);
			is.close();
		}
		catch (IOException e) {
			// couldn't open the file for reading
			return null;
		}
		name = name.toLowerCase();
		width = PLUGIN_NOT_FOUND;
        
		// Temporarily suppress "plugin not found" errors if LOCI Bio-Formats plugin is installed
		if (Menus.getCommands().get("Bio-Formats Importer")!=null && IJ.getVersion().compareTo("1.37u")>=0)
			IJ.suppressPluginNotFoundError();

		// OK now we get to the interesting bit

		// ****************** MODIFY HERE ******************
		// do what ever you have to do to recognise your own file type
		// and then call appropriate plugin using the above as models
		// e.g.:
		
		/*
		// A. Dent: Added XYZ handler
		// ----------------------------------------------
		// check if the file ends in .xyz, and bytes 0 and 1 equal 42
		if (name.endsWith(".xyz") && buf[0]==42 && buf[1]==42) {
		// Ok we've identified the file type - now load it
			return tryPlugIn("XYZ_Reader", path);
		}
		*/
        
		// Arjun: modified Gatan Digital Micrograph DM3 handler to work with Cornell Spectrum Imager
		// If CSI DM3 Reader doesn't exist, it still opens in the default DM3 Reader
		// ----------------------------------------------
		// check if the file ends in .DM3 or .dm3,
		// and bytes make an int value of 3 which is the DM3 version number
		if (name.endsWith(".dm3") && buf[0]==0 && buf[1]==0 && buf[2]==0 && buf[3]==3) {
			return tryCSIPlugIn("CSI_DM3_Reader", path);
		}
		
		// Arjun: Added TIA handler to work with Cornell Spectrum Imager 
		if (name.endsWith(".ser")) {
			return tryCSIPlugIn("CSI_TIA_Reader", path);
		}



		return null;
	}

	private ImagePlus openImage(String directory, String name, String path) {
		Object o = tryOpen(directory, name, path);
		// if an image was returned, assume success
		if (o instanceof ImagePlus) return (ImagePlus)o;

		return null;
		
	} // openImage

	/**
	* Attempts to open the specified path with the given plugin. If the
	* plugin extends the ImagePlus class (e.g., BioRad_Reader), set
	* extendsImagePlus to true, otherwise (e.g., LSM_Reader) set it to false.
	*
	* @return A reference to the plugin, if it was successful.
	*/
	private Object tryPlugIn(String className, String path) {
		Object o = IJ.runPlugIn(className, path);
		if (o instanceof ImagePlus) {
			// plugin extends ImagePlus class
			ImagePlus imp = (ImagePlus)o;
				if (imp.getWidth()==0)
					o = null; // invalid image
				else 
					width = IMAGE_OPENED; // success
		} else {
			// plugin does not extend ImagePlus; assume success
			width = IMAGE_OPENED;
		}
		return o;
	}
	
	private Object tryCSIPlugIn(String className, String path) {
		Object o = IJ.runPlugIn(className, path);
//		if (o instanceof ImagePlus) {
//			// plugin extends ImagePlus class
//			
//			ImagePlus imp = (ImagePlus)o;
//				if (imp.getWidth()==0)
//					o = null; // invalid image
//				else 
//					width = IMAGE_OPENED; // success
//		} 
//		else {
			// plugin does not extend ImagePlus; assume success
			width = IMAGE_OPENED;

//		}
		return o;
	}


}
