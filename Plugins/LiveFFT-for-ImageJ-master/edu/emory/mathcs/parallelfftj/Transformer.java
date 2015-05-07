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

package edu.emory.mathcs.parallelfftj;

import ij.ImagePlus;

/**
 * Transforms an image (2D and 3D) from the space domain to the Fourier domain.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * @author Nick Linnenbrügger (nilin@web.de)
 */
public abstract class Transformer {

    /**
     * Creates new image from the Fourier domain data assuming the Fourier
     * domain origin at (0,0,0).
     * 
     * @param type
     *            type of spectrum
     * @return image
     */
    public abstract ImagePlus toImagePlus(SpectrumType type);

    /**
     * Creates new image from the Fourier domain data.
     * 
     * @param type
     *            type of spectrum
     * @param fdOrigin
     *            type of Fourier domain origin
     * @return image
     */
    public abstract ImagePlus toImagePlus(SpectrumType type, FourierDomainOriginType fdOrigin);

    /**
     * Performs forward Fourier transform on the data.
     */
    public abstract void fft();

    /**
     * Performs inverse Fourier transform on the data.
     * 
     * @param scale
     *            if true then scaling is performed
     */
    public abstract void ifft(boolean scale);
}
