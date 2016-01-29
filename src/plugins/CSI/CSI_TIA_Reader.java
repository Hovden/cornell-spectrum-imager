package plugins.CSI;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.OpenDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import plugins.ledatastream.LEDataInputStream;

/*

This plugin tries to import the *.ser-files (STEM, CCD) written by the TIA Program (Emispec) during the data aquisition. I used the on Dr. Chris Boothroyd website described file structure (http://www.microscopy.cen.dtu.dk/~cbb/info/TIAformat/index.html). I want to thank him for the given information on his website.

@author Steffen Schmidt; steffen.schmidt at cup.uni-muenchen.de
 * modified by Paul Cueva; pdc23 at cornell.edu
 * modifictaions: reads spectra into map as opposed to seperate plots, and adds tag info to the calibration.

In this plugin the ledatastream class of the Canadian Mind Products company is used (please see the additinal license rule). This plugin was donated to the ImageJ community, following all license rules of the ImageJ program will be applying to the source code.

The drag and drop support in ImageJ can be enabled by editing/compiling the HandleExtraFileTypes.java-file in the Input-Output folder. I added the line: if (name.endsWith(".ser")) {return tryPlugIn("TIA_Reader", path); in the tryOpen-class and recompiled it. 

 * included in v1.1 CSI 032911
 */

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is CSI TIA Reader.
 *
 * The Initial Developer of the Original Code is
 * Paul Cueva <pdc23@cornell.edu>, Cornell University.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Paul Cueva <pdc23@cornell.edu>
 *   Robert Hovden <rmh244@cornell.edu>
 *   David A. Muller <david.a.muller@cornell.edu>
 *
 * ***** END LICENSE BLOCK ***** */


public class CSI_TIA_Reader implements PlugIn {

	ImagePlus img;

	public void run(String arg) {
		String path = getPath(arg);
		if (null == path) {
			return;
		}
		if (!parse(path)) {
			return;
		}
	}

	private String getPath(String arg) {

		if (null != arg) {
			if (0 == arg.indexOf("http://")
					|| new File(arg).exists()) {
				return arg;
			}
		}
		OpenDialog od = new OpenDialog("Load SER File...", arg);
		String DIRECTORY = od.getDirectory();
		if (null == DIRECTORY) {
			return null; // dialog was canceled
		}
		DIRECTORY = DIRECTORY.replace('\\', '/'); // Windows-unix convention
		if (!DIRECTORY.endsWith("/")) {
			DIRECTORY += "/";
		}
		String FILENAME = od.getFileName();
		if (null == FILENAME) {
			return null; //dialog was canceled
		}
		return DIRECTORY + FILENAME;
	}

	private InputStream open(String path) throws Exception {
		if (0 == path.indexOf("http://")) {
			return new java.net.URL(path).openStream();
		}
		return new FileInputStream(path);
	}

	private boolean parse(String path) {

		//variables
		int FILE_OFFSET; //number of bytes for jumping to the data
		int NUMBER_IMAGES; //number data sets
		int OFFSET_ARRAY_OFFSET; //Offset to the data array offset
		int NUMBER_DIMENSIONS; //!!!
		int[] DATA_OFFSET; //field of data offset array values
		int[] DIMENSION_SIZE; //!!!
		double[] CALIBRATION_OFFSET; //!!!
		double[] CALIBRATION_DELTA; //!!!
		int[] CALIBRATION_ELEMENT; //!!!
		int[] DESCRIPTION_LENGTH; //!!!
		char[][] DESCRIPTION; //!!!
		int[] UNITS_LENGTH; //!!!
		char[][] UNITS; //!!!
		int DATA_TYPE_ID; //type of stored data 0x4120->1D; 0x4122->2D
		ImagePlus imp = null;

		//reading the header and the data offset array
		try {
			InputStream is = open(path);
			LEDataInputStream data = new LEDataInputStream(is);
			if (data.readShort() != 0x4949) {
				IJ.error("Doesn't seem to be a SER file");
				return false; //ByteOrder 18761=0x4949H indicates little-endian byte Ordering
			}
			data.readShort(); //SeriesID
			data.readShort(); //SeriesVersion
			DATA_TYPE_ID = data.readInt(); //DataTypeID
			data.readInt(); //TagTypeID
			data.readInt(); //TotalNumberElements
			NUMBER_IMAGES = data.readInt(); //ValidNumberElements
			OFFSET_ARRAY_OFFSET = data.readInt(); //OffsetArrayOffset
			NUMBER_DIMENSIONS=data.readInt(); //NumberDimension
			DIMENSION_SIZE = new int[NUMBER_DIMENSIONS]; //!!!
			CALIBRATION_OFFSET = new double[NUMBER_DIMENSIONS]; //!!!
			CALIBRATION_DELTA = new double[NUMBER_DIMENSIONS]; //!!!
			CALIBRATION_ELEMENT = new int[NUMBER_DIMENSIONS]; //!!!
			DESCRIPTION_LENGTH = new int[NUMBER_DIMENSIONS]; //!!!
			DESCRIPTION = new char[NUMBER_DIMENSIONS][]; //!!!
			UNITS_LENGTH = new int[NUMBER_DIMENSIONS]; //!!!
			UNITS = new char[NUMBER_DIMENSIONS][]; //!!!
			int count = 0;
			int location = 0;
			while (count < NUMBER_DIMENSIONS) {
				DIMENSION_SIZE[count] = data.readInt(); // DimensionSize
				CALIBRATION_OFFSET[count] = data.readDouble(); // CalibrationOffset
				CALIBRATION_DELTA[count] = data.readDouble(); // CalibrationDelta
				CALIBRATION_ELEMENT[count] = data.readInt(); // CalibrationElement
				DESCRIPTION_LENGTH[count] = data.readInt(); // DescriptionLength
				DESCRIPTION[count] = new char[DESCRIPTION_LENGTH[count]];
				for (int ccount =0; ccount<DESCRIPTION_LENGTH[count]; ccount++) {
					DESCRIPTION[count][ccount] = (char)data.readByte(); // Description
				}
				UNITS_LENGTH[count] = data.readInt(); // UnitsLength
				UNITS[count] = new char[UNITS_LENGTH[count]];
				for (int ccount =0; ccount<UNITS_LENGTH[count]; ccount++) {
					UNITS[count][ccount] = (char)data.readByte(); // Units
				}
				location+= 32 + DESCRIPTION_LENGTH[count] + UNITS_LENGTH[count];
				count++;
			}
			DATA_OFFSET = new int[NUMBER_IMAGES]; //configure the size of the data offset array
			data.skipBytes(OFFSET_ARRAY_OFFSET - 30 - location); //Data offset array - header format
			count = 0;
			while (count < NUMBER_IMAGES) {
				DATA_OFFSET[count] = data.readInt();
				count++;
			}
			data.close();
		} catch (Exception e) {
			IJ.error("Error opening file", e.getMessage());
			//IJ.error("Error opening file");
			return false;
		}

		// opening of the different data elements
		try {
			if (NUMBER_DIMENSIONS <= 1) {
				ImageStack ims = null;
				int count = 0;
				while (count < NUMBER_IMAGES) {
					if (DATA_TYPE_ID == 0x4122) {
						if (ims == null) {
							ImageProcessor ip = OpenImage(path, DATA_OFFSET[count]).getProcessor();
							ims = new ImageStack(ip.getWidth(), ip.getHeight());
							ims.addSlice("", ip);
						} else {
							ims.addSlice("", OpenImage(path, DATA_OFFSET[count]).getProcessor());
						}
					} // DataTypeID = 0x4122 indicates an image
					else if (DATA_TYPE_ID == 0x4120) {
						if (imp == null)
							imp = new ImagePlus();
						imp = OpenSpectra(path, DATA_OFFSET[count], imp.getProcessor());
					} // DataTypeID = 0x4120 indicates a spectrum
					else if (check_data_element(path, DATA_OFFSET[count])) 
						// guessing of the DataType
					{
						if (ims == null) {
							ImageProcessor ip = OpenImage(path, DATA_OFFSET[count]).getProcessor();
							ims = new ImageStack(ip.getWidth(), ip.getHeight());
							ims.addSlice("", ip);
						} else {
							ims.addSlice("", OpenImage(path, DATA_OFFSET[count]).getProcessor());
						}
					} else {
						if (imp == null)
							imp = new ImagePlus();
						imp = OpenSpectra(path, DATA_OFFSET[count], imp.getProcessor());
					}
					count++;
				}
				if (ims != null) {
					imp = new ImagePlus(path.substring(path.lastIndexOf("/") + 1), ims);
				}
			} else if (NUMBER_DIMENSIONS == 2) {

				LEDataInputStream data = new LEDataInputStream(open(path));
				data.skipBytes(DATA_OFFSET[0]); // jumping to the data element
				double Z_OFFSET = data.readDouble(); // CalibrationOffset
				double Z_WIDTH = data.readDouble(); // CalibrationDelta
				int Z_ELEMENT = data.readInt(); // CalibrationElement
				data.readShort(); // DataType
				int Z_DEPTH = data.readInt(); // ArrayLength
				data.close();

				float[][][] spectra =  new float[Z_DEPTH][DIMENSION_SIZE[0]][DIMENSION_SIZE[1]];
				for (int j=0; j<DIMENSION_SIZE[1]; j++)
					for (int i=0; i<DIMENSION_SIZE[0]; i++)
						if (i*DIMENSION_SIZE[1] + j<NUMBER_IMAGES) {
							IJ.showProgress(1.0*(j*DIMENSION_SIZE[0] + i + 1)/(DIMENSION_SIZE[0]*DIMENSION_SIZE[1]));
							OpenSpectra(path, DATA_OFFSET[j*DIMENSION_SIZE[0] + i], spectra, i, j);
						}
				ImageStack ims = new ImageStack(DIMENSION_SIZE[0], DIMENSION_SIZE[1]);
				for (int k=0; k<Z_DEPTH; k++)
					ims.addSlice((Z_OFFSET - (Z_WIDTH * Z_ELEMENT) + (k * Z_WIDTH))+" ev", new FloatProcessor(spectra[k]));

				imp = new ImagePlus(path.substring(path.lastIndexOf("/")+1), ims);

				Calibration cal = imp.getCalibration();
				cal.pixelDepth = Z_WIDTH;
				cal.zOrigin = Z_ELEMENT - Z_OFFSET/Z_WIDTH;
				if (CALIBRATION_DELTA[0]>1) {
					imp.getCalibration().pixelWidth = CALIBRATION_DELTA[0];
					imp.getCalibration().xOrigin = CALIBRATION_OFFSET[0];
					imp.getCalibration().setXUnit(new String(UNITS[0]));
				} else if (CALIBRATION_DELTA[0]>1E-3) {
					imp.getCalibration().pixelWidth = 1E3*CALIBRATION_DELTA[0];
					imp.getCalibration().xOrigin = 1E3*CALIBRATION_OFFSET[0];
					imp.getCalibration().setXUnit("milli"+new String(UNITS[0]));
				} else if (CALIBRATION_DELTA[0]>1E-6) {
					imp.getCalibration().pixelWidth = 1E6*CALIBRATION_DELTA[0];
					imp.getCalibration().xOrigin = 1E6*CALIBRATION_OFFSET[0];
					imp.getCalibration().setXUnit("micro"+new String(UNITS[0]));
				} else if (CALIBRATION_DELTA[0]>1E-9) {
					imp.getCalibration().pixelWidth = 1E9*CALIBRATION_DELTA[0];
					imp.getCalibration().xOrigin = 1E9*CALIBRATION_OFFSET[0];
					imp.getCalibration().setXUnit("nano"+new String(UNITS[0]));
				}
				if (CALIBRATION_DELTA[1]>1||CALIBRATION_DELTA[1]<-1) {
					imp.getCalibration().pixelHeight = CALIBRATION_DELTA[1];
					imp.getCalibration().yOrigin = CALIBRATION_OFFSET[1];
					imp.getCalibration().setYUnit(new String(UNITS[1]));
				} else if (CALIBRATION_DELTA[1]>1E-3||CALIBRATION_DELTA[1]<-1E-3) {
					imp.getCalibration().pixelHeight = 1E3*CALIBRATION_DELTA[1];
					imp.getCalibration().yOrigin = 1E3*CALIBRATION_OFFSET[1];
					imp.getCalibration().setYUnit("milli"+new String(UNITS[1]));
				} else if (CALIBRATION_DELTA[1]>1E-6||CALIBRATION_DELTA[1]<-1E-6) {
					imp.getCalibration().pixelHeight = 1E6*CALIBRATION_DELTA[1];
					imp.getCalibration().yOrigin = 1E6*CALIBRATION_OFFSET[1];
					imp.getCalibration().setYUnit("micro"+new String(UNITS[1]));
				} else if (CALIBRATION_DELTA[1]>1E-9||CALIBRATION_DELTA[1]<-1E-9) {
					imp.getCalibration().pixelHeight = 1E9*CALIBRATION_DELTA[1];
					imp.getCalibration().yOrigin = 1E9*CALIBRATION_OFFSET[1];
					imp.getCalibration().setYUnit("nano"+new String(UNITS[1]));
				}
			} else
				IJ.error("TIA_Reader currently doesn't support reading " + NUMBER_DIMENSIONS + " dimensional data.");
		} catch (Exception e) {
			IJ.error("Error opening Data series",e.toString());
			//IJ.error("Error opening Data series");
			return false;
		}

		if (imp != null) {
			imp.show();
			img = imp;
			IJ.resetMinAndMax();
		}
		return true;
	}

	private boolean check_data_element(String path, int byteoffset) throws Exception {

		//variables
		double PIXEL_WIDTH; //CalibrationDeltaX
		double PIXEL_HEIGHT; //CalibrationDeltaY
		short DATA_TYPE; //DataType

		// reading the header of the data elements
		InputStream is = open(path);
		LEDataInputStream data = new LEDataInputStream(is);
		data.skipBytes(byteoffset); //jumping to the data element field
		data.readDouble(); //CalibrationOffsetX
		PIXEL_WIDTH = data.readDouble(); //CalibrationDeltaX
		data.readInt();  //CalibrationElementX
		DATA_TYPE = data.readShort(); //may be DataType
		data.skipBytes(6); //jumping to the CalibrationDeltaY
		PIXEL_HEIGHT = data.readDouble(); //CalibrationDeltaY
		data.close(); //close file

		//guessing of the DataType; true indicates 2D - false indicates 1D
		if (DATA_TYPE == 0) {
			return true;
		} else if (PIXEL_WIDTH == PIXEL_HEIGHT) {
			return true;
		} else {
			return false;
		}
	}

	private ImagePlus OpenImage(String path, int byteoffset) throws Exception {

		//variables
		double PIXEL_WIDTH; //CalibrationDeltaX
		double PIXEL_HEIGHT; //CalibrationDeltaY
		short DATA_TYPE; //DataType
		int IMAGE_WIDTH; //ArraySizeX
		int IMAGE_HEIGHT; //ArraySizeY

		// reading calibration values
		InputStream is = open(path);
		LEDataInputStream data = new LEDataInputStream(is);
		data.skipBytes(byteoffset); //jumping to the 2D-data element field
		data.readDouble(); //CalibrationOffsetX
		PIXEL_WIDTH = data.readDouble(); //CalibrationDeltaX
		data.readInt();  //CalibrationElementX
		data.readDouble(); //CalibrationOffsetY
		PIXEL_HEIGHT = data.readDouble(); //CalibrationDeltaY
		data.readInt(); //CalibrationElementY
		DATA_TYPE = data.readShort(); //DataType
		IMAGE_WIDTH = data.readInt(); //ArraySizeX
		IMAGE_HEIGHT = data.readInt(); //ArraySizeY
		data.close();

		//opening of the image
		FileInfo fi = new FileInfo();
		fi.fileFormat = fi.RAW;
		fi.intelByteOrder = true;  //little-endian byte ordering
		int islash = path.lastIndexOf('/');
		if (0 == path.indexOf("http://")) {
			fi.url = path;
		} else {
			fi.directory = path.substring(0, islash + 1);
		}
		fi.fileName = path.substring(islash + 1);
		fi.width = IMAGE_WIDTH;
		fi.height = IMAGE_HEIGHT;
		if ((PIXEL_WIDTH * IMAGE_WIDTH) < 1E-5) {
			fi.pixelWidth = PIXEL_WIDTH / 1E-9;
			fi.pixelHeight = PIXEL_HEIGHT / 1E-9;
			fi.unit = "nm";
		} else if ((PIXEL_WIDTH * IMAGE_WIDTH) < 1E-2) {
			fi.pixelWidth = PIXEL_WIDTH / 1E-6;
			fi.pixelHeight = PIXEL_HEIGHT / 1E-6;
			fi.unit = "microns";
		} else if ((PIXEL_WIDTH * IMAGE_WIDTH) < 1E1) {
			fi.pixelWidth = PIXEL_WIDTH / 1E-3;
			fi.pixelHeight = PIXEL_HEIGHT / 1E-3;
			fi.unit = "mm";
		} else {
			fi.pixelWidth = PIXEL_WIDTH;
			fi.pixelHeight = PIXEL_HEIGHT;
			fi.unit = "m";
		}
		fi.nImages = 1;
		switch (DATA_TYPE) {
		case 1:
			fi.fileType = FileInfo.GRAY8;
			break;
		case 2:
			fi.fileType = FileInfo.GRAY16_UNSIGNED;
			break;
		case 3:
			fi.fileType = FileInfo.GRAY32_UNSIGNED;
			break;
		case 4:
			fi.fileType = FileInfo.GRAY8;
			break;
		case 5:
			fi.fileType = FileInfo.GRAY16_SIGNED;
			break;
		case 6:
			fi.fileType = FileInfo.GRAY32_INT;
			break;
		case 7:
			fi.fileType = FileInfo.GRAY32_FLOAT;
			break;
		case 8:
			fi.fileType = FileInfo.GRAY64_FLOAT;
			break;
		}
		fi.offset = byteoffset + 50;
		fi.whiteIsZero = false;
		FileOpener fo = new FileOpener(fi);
		ImagePlus imp = fo.open(false);
		IJ.run(imp, "Flip Vertically", "");
		Object obinfo = imp.getProperty("Info");
		if (null != obinfo) {
			imp.setProperty("Info", obinfo);
		}
		return imp;

	}

	private ImagePlus OpenSpectra(String path, int byteoffset, ImageProcessor ip) throws Exception {
		//variables
		double PIXEL_WIDTH; //CalibrationDelta
		double CALIBRATION_OFFSET; //CalibrationOffset
		int CALIBRATION_ELEMENT; //CalibrationElement
		short DATA_TYPE; //DataType
		int IMAGE_WIDTH; //ArrayLength
		int IMAGE_HEIGHT;
		ImagePlus imp;

		// reading the calibration data
		LEDataInputStream data = new LEDataInputStream(open(path));
		data.skipBytes(byteoffset); //jumping to the data element

		CALIBRATION_OFFSET = data.readDouble(); //CalibrationOffset
		PIXEL_WIDTH = data.readDouble(); //CalibrationDelta
		CALIBRATION_ELEMENT = data.readInt();  //CalibrationElement
		DATA_TYPE = data.readShort(); //DataType
		IMAGE_WIDTH = data.readInt(); //ArrayLength

		if (ip == null) {
			IMAGE_HEIGHT = 1;
			ip = new FloatProcessor(IMAGE_WIDTH, IMAGE_HEIGHT);
		}
		else {
			ImageProcessor ipOld = ip.duplicate();
			IMAGE_HEIGHT = ip.getHeight()+1;
			ip = ipOld.createProcessor(IMAGE_WIDTH, IMAGE_HEIGHT);
			ip.insert(ipOld,0,0);
		}

		//opening of the spectra
		int count = 0;
		while (count < IMAGE_WIDTH) {
			// x[count] = 0.001f * ((float) CALIBRATION_OFFSET - ((float) PIXEL_WIDTH * (float) CALIBRATION_ELEMENT) + ((float) count * (float) PIXEL_WIDTH)); //setting in x in keV
			switch (DATA_TYPE) {
			case 1:
				return null;
			case 2:
				ip.setf(count, IMAGE_HEIGHT-1,data.readShort());
				break;
			case 3:
				ip.setf(count, IMAGE_HEIGHT-1,data.readInt());
				break;
			case 4:
				return null;
			case 5:
				ip.setf(count, IMAGE_HEIGHT-1,data.readShort());
				break;
			case 6:
				ip.setf(count, IMAGE_HEIGHT-1,data.readInt());
				break;
			case 7:
				ip.setf(count, IMAGE_HEIGHT-1,data.readFloat());
				break;
			case 8:
				return null;
			case 9:
				return null;
			case 10:
				return null;
			}
			count++;
		}
		data.close();
		imp = new ImagePlus(path.substring(path.lastIndexOf("/")+1), ip);
		Calibration cal = imp.getCalibration();
		cal.pixelDepth = PIXEL_WIDTH;
		cal.zOrigin = CALIBRATION_ELEMENT - CALIBRATION_OFFSET/PIXEL_WIDTH;
		return imp;
		//					IJ.log ("Calibration-Delta-x: " + (PIXEL_WIDTH) + " / Length of data field: " + (IMAGE_WIDTH));
		//plotting of the data

	}

	private void OpenSpectra(String path, int byteoffset, float[][][] spectra, int i, int j) throws Exception {
		//variables
		double PIXEL_WIDTH; //CalibrationDelta
		double CALIBRATION_OFFSET; //CalibrationOffset
		int CALIBRATION_ELEMENT; //CalibrationElement
		short DATA_TYPE; //DataType
		int DATA_DEPTH; //ArrayLength
		int IMAGE_WIDTH = spectra[0].length;
		int IMAGE_HEIGHT =  spectra[0][0].length;

		// reading the calibration data
		LEDataInputStream data = new LEDataInputStream(open(path));
		data.skipBytes(byteoffset); //jumping to the data element

		CALIBRATION_OFFSET = data.readDouble(); //CalibrationOffset
		PIXEL_WIDTH = data.readDouble(); //CalibrationDelta
		CALIBRATION_ELEMENT = data.readInt();  //CalibrationElement
		DATA_TYPE = data.readShort(); //DataType
		DATA_DEPTH = data.readInt(); //ArrayLength

		//opening of the spectra
		int count = 0;
		while (count < DATA_DEPTH) {

			switch (DATA_TYPE) {
			case 1:
				return;
			case 2:
				spectra[count][i][j] = data.readShort();
				break;
			case 3:
				spectra[count][i][j] = data.readInt();
				break;
			case 4:
				return;
			case 5:
				spectra[count][i][j] = data.readShort();
				break;
			case 6:
				spectra[count][i][j] = data.readInt();
				break;
			case 7:
				spectra[count][i][j] = data.readFloat();
				break;
			case 8:
				return;
			case 9:
				return;
			case 10:
				return;
			}
			count++;
		}
		data.close();
		

	}

}
