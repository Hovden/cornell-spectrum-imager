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

/**
 * Types of spectrum. 
 *
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * @author Nick Linnenbrügger (nilin@web.de)
 */

public enum SpectrumType {
    ABS("Abs"), IMAG_PART("Imaginary Part"), REAL_PART("Real Part"), FREQUENCY_SPECTRUM("Frequency Spectrum"),
    FREQUENCY_SPECTRUM_LOG ("Frequency Spectrum (logarithmic)"), PHASE_SPECTRUM("Phase Spectrum"),
    POWER_SPECTRUM("Power Spectrum"), POWER_SPECTRUM_LOG("Power Spectrum (logarithmic)");
    
    private final String name;

    private SpectrumType(String name) {
        this.name = name;
    }
    
    public String toString() {
        return name;
    }
}
