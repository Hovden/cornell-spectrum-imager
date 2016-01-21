package plugins.CSI;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

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
 * The Original Code is CSI DM3 Reader.
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
public class CSI_Map_to_Line implements PlugInFilter
{
    public int setup(String arg, ImagePlus img) {
        IJ.run(img, "Reslice [/]...", "slice_count=1 rotate avoid");
        ImagePlus resliced = IJ.getImage();
        IJ.run(resliced, "Z Project...", "start=1 stop="+resliced.getStackSize()+" projection=[Sum Slices]");
        ImagePlus binned = IJ.getImage();
        resliced.close();
        binned.setTitle("Binned "+img.getTitle());
        binned.setCalibration(img.getCalibration());
        return DOES_ALL;
    }

    public void run(ImageProcessor ip)  {
        return;
    }

}
