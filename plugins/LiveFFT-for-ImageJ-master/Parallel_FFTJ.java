/*
 *  Copyright (C) 2008-2009 Piotr Wendykier
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import edu.emory.mathcs.parallelfftj.DoubleTransformer;
import edu.emory.mathcs.parallelfftj.FloatTransformer;
import edu.emory.mathcs.parallelfftj.OutputFrame;
import edu.emory.mathcs.parallelfftj.Transformer;
import edu.emory.mathcs.utils.ConcurrencyUtils;
import ij.*;
import ij.gui.*;
import ij.plugin.*;

/**
 * Parallel FFTJ - multithreaded FFT plugin for ImageJ. The code is derived from
 * Nick Linnenbrügger's <a href="http://rsb.info.nih.gov/ij/plugins/fftj.html" target="_blank">
 * FFTJ (version 2.0)</a>
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * @author Nick Linnenbrügger (nilin@web.de)
 */
public class Parallel_FFTJ implements PlugIn {
    private static final String plugInTitle = "Parallel FFTJ 1.4";
    private static final String outputFrameTitlePrefix = "Parallel FFTJ 1.4";

    private enum Precision {
        SINGLE("Single"), DOUBLE("Double");

        private final String name;

        private Precision(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    };

    private enum Direction {
        FORWARD("Forward"), INVERSE("Inverse");

        private final String name;

        private Direction(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    };

    public void run(String arg) {
        if (arg.equalsIgnoreCase("about")) {
            showAbout();
            return;
        }

        // check if appropriate version of ImageJ is installed
        if (IJ.versionLessThan("1.24t"))
            return;

        // check if Java, ver. 1.5 or higher is installed
        if (System.getProperty("java.version").compareTo("1.5.0") < 0) {
            IJ.error("This plugin has been developed and tested with Java, version 1.5 and higher.\n" + "Please upgrade your JVM.");
            return;
        }

        int[] wList = WindowManager.getIDList();
        if (wList == null) {
            IJ.noImage();
            return;
        }

        String[] titles1 = new String[wList.length];
        for (int i = 0; i < wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (imp != null)
                titles1[i] = imp.getTitle();
            else
                titles1[i] = "";
        }

        // add option '<none>' to image titles
        String[] titles2 = new String[wList.length + 1];
        titles2[0] = "<none>";
        for (int i = 0; i < wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (imp != null)
                titles2[i + 1] = imp.getTitle();
            else
                titles2[i + 1] = "";
        }

        GenericDialog gd = new GenericDialog(plugInTitle, IJ.getInstance());
        gd.addChoice("Real part of input:", titles1, titles1[0]);
        gd.addChoice("Imaginary part of input:", titles2, titles2[0]);
        gd.addChoice("Complex Number Precision:", new String[] { Precision.SINGLE.toString(), Precision.DOUBLE.toString() }, Precision.SINGLE.toString());
        gd.addChoice("FFT Direction:", new String[] { Direction.FORWARD.toString(), Direction.INVERSE.toString() }, Direction.FORWARD.toString());
        gd.addNumericField("Number of threads:", ConcurrencyUtils.getNumberOfProcessors(), 0);
        gd.showDialog();

        if (gd.wasCanceled()) {
            ConcurrencyUtils.shutdown();
            return;
        }

        ImagePlus real = WindowManager.getImage(wList[gd.getNextChoiceIndex()]);
        ImagePlus imaginary = null;
        int choiceIndex2 = gd.getNextChoiceIndex();
        if (choiceIndex2 != 0)
            imaginary = WindowManager.getImage(wList[choiceIndex2 - 1]);

        if (real.lockSilently() == false) {
            IJ.error("The specified real part already is in use.");
            return;
        }

        if ((imaginary != null) && (imaginary.lockSilently() == false) && (imaginary != real)) {
            IJ.error("The specified imaginary part already is in use.");
            real.unlock();
            return;
        }

        int nthreads = (int) gd.getNextNumber();
        if ((nthreads <= 0) || !(ConcurrencyUtils.isPowerOf2(nthreads))) {
            IJ.error("Number of threads has to be a positive power-of-two number");
            return;
        } else {
            ConcurrencyUtils.setNumberOfThreads(nthreads);
        }

        try {
            if (!isGrayscale(real) || ((imaginary != null) && (!isGrayscale(imaginary) || (real.getWidth() != imaginary.getWidth()) || (real.getHeight() != imaginary.getHeight()) || (real.getStackSize() != imaginary.getStackSize())))) {
                IJ.error("Real part of input must be a gray-scale image or image stack.\n" + "If an imaginary part is specified, it must be a gray-scale image or image\n" + "stack of the same size as the real part in all three dimensions.");
                return;
            }

            Transformer transformer = null;
            if (gd.getNextChoiceIndex() == 0) {
                transformer = new FloatTransformer(real.getStack(), (imaginary != null) ? imaginary.getStack() : null);
            } else {
                transformer = new DoubleTransformer(real.getStack(), (imaginary != null) ? imaginary.getStack() : null);
            }
            if (gd.getNextChoiceIndex() == 0) {
                IJ.showStatus("Calculating Fourier Transform ...");
                transformer.fft();
            } else {
                IJ.showStatus("Calculating Inverse Fourier Transform ...");
                transformer.ifft(true);
            }

            IJ.showStatus("");
            // create plugIn frame that gives choices for various output
            new OutputFrame(outputFrameTitlePrefix + " Real input: " + real.getTitle() + "; Imaginary input: " + ((imaginary != null) ? imaginary.getTitle() : "<none>"), transformer);
        } finally {
            real.unlock();
            if (imaginary != null)
                imaginary.unlock();
        }
    }

    static public void showAbout() {
        IJ.showMessage("About Parallel FFTJ 1.0 ...", "This plug-in has been written by Piotr Wendykier for Emory University.\nThe code is derived from FFTJ (version 2.0) written by Nick I. Linnenbruegger.\n");
    }

    private static boolean isGrayscale(ImagePlus image) {
        return ((image.getType() == ImagePlus.GRAY8) || (image.getType() == ImagePlus.GRAY16) || (image.getType() == ImagePlus.GRAY32));
    }

    public static void main(String args[]) {
        new ImageJ();
        IJ.open("D:\\Research\\Images\\head_mri\\paper\\head-blur.tif");
        IJ.open("D:\\Research\\Images\\head_mri\\paper\\head-psf.tif");
        IJ.runPlugIn("Parallel_FFTJ", null);
    }
}