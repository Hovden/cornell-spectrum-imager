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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import cern.colt.function.tfcomplex.FComplexRealFunction;
import cern.colt.matrix.AbstractMatrix;
import cern.colt.matrix.tfcomplex.FComplexMatrix2D;
import cern.colt.matrix.tfcomplex.FComplexMatrix3D;
import cern.colt.matrix.tfcomplex.impl.DenseFComplexMatrix2D;
import cern.colt.matrix.tfcomplex.impl.DenseFComplexMatrix3D;
import cern.colt.matrix.tfloat.FloatMatrix2D;
import cern.colt.matrix.tfloat.FloatMatrix3D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix2D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix3D;
import cern.jet.math.tfcomplex.FComplex;
import cern.jet.math.tfcomplex.FComplexFunctions;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Implementation of Transformer for single precision data.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * @author Nick Linnenbr�gger (nilin@web.de)
 */
public class FloatTransformer extends Transformer {
    private AbstractMatrix data;
    private int slices, rows, columns;
    private boolean hasImagPart = false;
    private boolean is2D = false;

    /**
     * Creates new instance of FloatTransformer
     * 
     * @param sourceReal
     *            real part
     */
    public FloatTransformer(ImageStack sourceReal) {
        this.setData(sourceReal, null);
    }

    /**
     * Creates new instance of FloatTransformer
     * 
     * @param sourceReal
     *            real part
     * @param sourceImag
     *            imaginary part
     */
    public FloatTransformer(ImageStack sourceReal, ImageStack sourceImag) {
        this.setData(sourceReal, sourceImag);
    }

    public void fft() {
        if (is2D) {
            if (hasImagPart) {
                ((DenseFComplexMatrix2D) data).fft2();
            } else {
                data = ((DenseFloatMatrix2D) data).getFft2();
            }
        } else {
            if (hasImagPart) {
                ((DenseFComplexMatrix3D) data).fft3();
            } else {
                data = ((DenseFloatMatrix3D) data).getFft3();
            }
        }
    }

    public void ifft(boolean scale) {
        if (is2D) {
            if (hasImagPart) {
                ((DenseFComplexMatrix2D) data).ifft2(scale);
            } else {
                data = ((DenseFloatMatrix2D) data).getIfft2(scale);
            }
        } else {
            if (hasImagPart) {
                ((DenseFComplexMatrix3D) data).ifft3(scale);
            } else {
                data = ((DenseFloatMatrix3D) data).getIfft3(scale);
            }
        }
    }

    public ImagePlus toImagePlus(SpectrumType type) {
        return this.toImagePlus(type, FourierDomainOriginType.AT_ZERO);
    }

    public ImagePlus toImagePlus(SpectrumType type, FourierDomainOriginType fdOrigin) {
        if (this.data == null)
            throw new IllegalStateException("Data to process must be set first. Use method setData(). ");

        AbstractMatrix dataCopy = null;

        if (is2D) {

            if (fdOrigin == FourierDomainOriginType.AT_CENTER)
                dataCopy = this.getRearrangedDataReferences();
            else
                dataCopy = ((FComplexMatrix2D) data).copy();

            FloatProcessor ip = new FloatProcessor(this.columns, this.rows);

            switch (type) {
            case ABS:
                dataCopy = ((FComplexMatrix2D) dataCopy).assign(FComplexFunctions.abs).getRealPart();
                break;
            case IMAG_PART:
                dataCopy = ((FComplexMatrix2D) dataCopy).getImaginaryPart();
                break;
            case REAL_PART:
                dataCopy = ((FComplexMatrix2D) dataCopy).getRealPart();
                break;
            case FREQUENCY_SPECTRUM:
                dataCopy = ((FComplexMatrix2D) dataCopy).assign(FComplexFunctions.abs).getRealPart();
                break;
            case FREQUENCY_SPECTRUM_LOG:
                dataCopy = ((FComplexMatrix2D) dataCopy).assign(new FComplexRealFunction() {
                    public final float apply(float[] a) {
                        return (float) Math.log(1.0 + FComplex.abs(a));
                    }
                }).getRealPart();
                break;
            case PHASE_SPECTRUM:
                dataCopy = ((FComplexMatrix2D) dataCopy).assign(new FComplexRealFunction() {
                    public final float apply(float[] a) {
                        return (float) Math.atan2(a[1], a[0]);
                    }
                }).getRealPart();
                break;
            case POWER_SPECTRUM:
                dataCopy = ((FComplexMatrix2D) dataCopy).assign(new FComplexRealFunction() {
                    public final float apply(float[] a) {
                        return a[0] * a[0] + a[1] * a[1];
                    }
                }).getRealPart();
                break;
            case POWER_SPECTRUM_LOG:
                dataCopy = ((FComplexMatrix2D) dataCopy).assign(new FComplexRealFunction() {
                    public final float apply(float[] a) {
                        return (float) Math.log(1.0 + a[0] * a[0] + a[1] * a[1]);
                    }
                }).getRealPart();
                break;
            }
            assignPixelsToProcessor(ip, (FloatMatrix2D) dataCopy);
            ImagePlus imp = new ImagePlus(type.toString() + " With Origin " + fdOrigin.toString() + " (Single Precision )", ip);
            imp.changes = true;
            return imp;

        } else {
            if (fdOrigin == FourierDomainOriginType.AT_CENTER)
                dataCopy = this.getRearrangedDataReferences();
            else
                dataCopy = ((FComplexMatrix3D) data).copy();

            ImageStack stack = new ImageStack(this.columns, this.rows);

            switch (type) {
            case ABS:
                dataCopy = ((FComplexMatrix3D) dataCopy).assign(FComplexFunctions.abs).getRealPart();
                break;
            case IMAG_PART:
                dataCopy = ((FComplexMatrix3D) dataCopy).getImaginaryPart();
                break;
            case REAL_PART:
                dataCopy = ((FComplexMatrix3D) dataCopy).getRealPart();
                break;
            case FREQUENCY_SPECTRUM:
                dataCopy = ((FComplexMatrix3D) dataCopy).assign(FComplexFunctions.abs).getRealPart();
                break;
            case FREQUENCY_SPECTRUM_LOG:
                dataCopy = ((FComplexMatrix3D) dataCopy).assign(new FComplexRealFunction() {
                    public final float apply(float[] a) {
                        return (float) Math.log(1.0 + FComplex.abs(a));
                    }
                }).getRealPart();
                break;
            case PHASE_SPECTRUM:
                dataCopy = ((FComplexMatrix3D) dataCopy).assign(new FComplexRealFunction() {
                    public final float apply(float[] a) {
                        return (float) Math.atan2(a[1], a[0]);
                    }
                }).getRealPart();
                break;
            case POWER_SPECTRUM:
                dataCopy = ((FComplexMatrix3D) dataCopy).assign(new FComplexRealFunction() {
                    public final float apply(float[] a) {
                        return a[0] * a[0] + a[1] * a[1];
                    }
                }).getRealPart();
                break;
            case POWER_SPECTRUM_LOG:
                dataCopy = ((FComplexMatrix3D) dataCopy).assign(new FComplexRealFunction() {
                    public final float apply(float[] a) {
                        return (float) Math.log(1.0 + a[0] * a[0] + a[1] * a[1]);
                    }
                }).getRealPart();
                break;
            }
            assignPixelsToStack(stack, (FloatMatrix3D) dataCopy);
            ImagePlus imp = new ImagePlus(type.toString() + " With Origin " + fdOrigin.toString() + " (Single Precision )", stack);
            imp.changes = true;
            return imp;
        }

    }

    /**
     * Do rearrangement (swap quadrants) on a new copy of the data
     * 
     * a b c d e --> c d e a b a b c d e f --> d e f a b c (in all three
     * dimensions)
     * 
     * @return rearranged data
     */
    private AbstractMatrix getRearrangedDataReferences() {
        if (this.data == null)
            throw new IllegalStateException("Data to process must be set first. Use method setData(). ");

        if (is2D) {
            int[] center = new int[] { (int) Math.round(this.rows / 2d), (int) Math.round(this.columns / 2d) };
            return circShift_2D((DenseFComplexMatrix2D) data, center);
        } else {
            int[] center = new int[] { (int) Math.round(this.slices / 2d), (int) Math.round(this.rows / 2d), (int) Math.round(this.columns / 2d) };
            return circShift_3D((DenseFComplexMatrix3D) data, center);
        }
    }

    /**
     * Sets the data for this class
     * 
     * @param sourceReal
     *            real part
     * @param sourceImag
     *            imaginary part
     */
    private void setData(ImageStack sourceReal, ImageStack sourceImag) {
        if (sourceReal == null)
            throw new IllegalArgumentException("Source stack with real part cannot be 'null'.");

        if ((sourceImag != null) && ((sourceReal.getWidth() != sourceImag.getWidth()) || (sourceReal.getHeight() != sourceImag.getHeight()) || (sourceReal.getSize() != sourceImag.getSize())))
            throw new IllegalArgumentException("Source stacks with real and imaginary part must be\n" + "of the same size in x-, y-, and z-direction.");

        this.slices = sourceReal.getSize();
        this.rows = sourceReal.getHeight();
        this.columns = sourceReal.getWidth();

        if (this.slices == 1) {
            is2D = true;
            if (sourceImag != null) {
                hasImagPart = true;
                this.data = new DenseFComplexMatrix2D(this.rows, this.columns);
                assignPixelsToMatrix_2D(sourceReal.getProcessor(1), sourceImag.getProcessor(1), (FComplexMatrix2D) data);
            } else {
                hasImagPart = false;
                this.data = assignPixelsToMatrix_2D(sourceReal.getProcessor(1));
            }

        } else {
            is2D = false;
            if (sourceImag != null) {
                hasImagPart = true;
                this.data = new DenseFComplexMatrix3D(this.slices, this.rows, this.columns);
                assignPixelsToMatrix_3D(sourceReal, sourceImag, (FComplexMatrix3D) data);
            } else {
                hasImagPart = false;
                this.data = new DenseFloatMatrix3D(this.slices, this.rows, this.columns);
                assignPixelsToMatrix_3D(sourceReal, (FloatMatrix3D) data);
            }
        }
    }

    /**
     * Copies pixel values from matrix <code>X</code> to image processor
     * <code>ip</code>.
     * 
     * @param ip
     *            image processor
     * @param X
     *            matrix
     * 
     */
    private static void assignPixelsToProcessor(final FloatProcessor ip, final FloatMatrix2D X) {
        final int rows = X.rows();
        final int cols = X.columns();
        final float[] px = (float[]) ip.getPixels();
        int np = ConcurrencyUtils.getNumberOfProcessors();
        if (X.isView()) {
            if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
                Future[] futures = new Future[np];
                int k = rows / np;
                for (int j = 0; j < np; j++) {
                    final int startrow = j * k;
                    final int stoprow;
                    if (j == np - 1) {
                        stoprow = rows;
                    } else {
                        stoprow = startrow + k;
                    }
                    futures[j] = ConcurrencyUtils.submit(new Runnable() {
                        public void run() {
                            for (int r = startrow; r < stoprow; r++) {
                                for (int c = 0; c < cols; c++) {
                                    px[c + cols * r] = (float) X.getQuick(r, c);
                                }
                            }
                        }
                    });
                }
                try {
                    for (int j = 0; j < np; j++) {
                        futures[j].get();
                    }
                } catch (ExecutionException ex) {
                    ex.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        px[c + cols * r] = (float) X.getQuick(r, c);
                    }
                }
            }
        } else {
            final float[] elems = (float[]) X.elements();
            if ((np > 1) && (rows * cols >= ConcurrencyUtils.getThreadsBeginN_2D())) {
                Future[] futures = new Future[np];
                int k = rows / np;
                for (int j = 0; j < np; j++) {
                    final int startrow = j * k;
                    final int stoprow;
                    if (j == np - 1) {
                        stoprow = rows;
                    } else {
                        stoprow = startrow + k;
                    }
                    futures[j] = ConcurrencyUtils.submit(new Runnable() {
                        public void run() {
                            int idx = startrow * cols;
                            for (int r = startrow; r < stoprow; r++) {
                                for (int c = 0; c < cols; c++) {
                                    px[idx] = (float) elems[idx];
                                    idx++;
                                }
                            }
                        }
                    });
                }
                try {
                    for (int j = 0; j < np; j++) {
                        futures[j].get();
                    }
                } catch (ExecutionException ex) {
                    ex.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                int idx = 0;
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        px[idx] = (float) elems[idx];
                        idx++;
                    }
                }
            }
        }
        ip.setMinAndMax(0, 0);
    }

    /**
     * Copies pixel values from real matrix <code>X</code> to image stack
     * <code>stack</code>
     * 
     * @param stack
     *            image stack
     * @param X
     *            real matrix
     * @param cmY
     *            color model
     */
    private static void assignPixelsToStack(final ImageStack stack, final FloatMatrix3D X) {
        final int slices = X.slices();
        final int rows = X.rows();
        final int cols = X.columns();
        for (int s = 0; s < slices; s++) {
            FloatProcessor ip = new FloatProcessor(cols, rows);
            assignPixelsToProcessor(ip, X.viewSlice(s));
            stack.addSlice(null, ip, s);
        }
    }

    /**
     * Computes the circular shift of a matrix.
     * 
     * @param M
     *            complex matrix.
     * @param center
     *            indices of center of <code>M</code>.
     * @return shifted matrix
     */
    private static FComplexMatrix2D circShift_2D(FComplexMatrix2D M, int[] center) {

        int rows = M.rows();
        int cols = M.columns();
        int cr = center[0];
        int cc = center[1];
        FComplexMatrix2D P1 = new DenseFComplexMatrix2D(rows, cols);
        P1.viewPart(0, 0, rows - cr, cols - cc).assign(M.viewPart(cr, cc, rows - cr, cols - cc));
        P1.viewPart(0, cols - cc, rows - cr, cc).assign(M.viewPart(cr, 0, rows - cr, cc));
        P1.viewPart(rows - cr, 0, cr, cols - cc).assign(M.viewPart(0, cc, cr, cols - cc));
        P1.viewPart(rows - cr, cols - cc, cr, cc).assign(M.viewPart(0, 0, cr, cc));
        return P1;
    }

    /**
     * Computes the circular shift of a matrix.
     * 
     * @param M
     *            complex matrix.
     * @param center
     *            indices of center of <code>M</code>.
     * @return shifted matrix
     */
    private static FComplexMatrix3D circShift_3D(FComplexMatrix3D M, int[] center) {
        int slices = M.slices();
        int rows = M.rows();
        int cols = M.columns();
        int cs = center[0];
        int cr = center[1];
        int cc = center[2];
        FComplexMatrix3D P1 = new DenseFComplexMatrix3D(slices, rows, cols);

        P1.viewPart(0, 0, 0, slices - cs, rows - cr, cols - cc).assign(M.viewPart(cs, cr, cc, slices - cs, rows - cr, cols - cc));
        P1.viewPart(0, rows - cr, 0, slices - cs, cr, cols - cc).assign(M.viewPart(cs, 0, cc, slices - cs, cr, cols - cc));
        P1.viewPart(0, 0, cols - cc, slices - cs, rows - cr, cc).assign(M.viewPart(cs, cr, 0, slices - cs, rows - cr, cc));
        P1.viewPart(0, rows - cr, cols - cc, slices - cs, cr, cc).assign(M.viewPart(cs, 0, 0, slices - cs, cr, cc));

        P1.viewPart(slices - cs, 0, 0, cs, rows - cr, cols - cc).assign(M.viewPart(0, cr, cc, cs, rows - cr, cols - cc));
        P1.viewPart(slices - cs, 0, cols - cc, cs, rows - cr, cc).assign(M.viewPart(0, cr, 0, cs, rows - cr, cc));
        P1.viewPart(slices - cs, rows - cr, 0, cs, cr, cols - cc).assign(M.viewPart(0, 0, cc, cs, cr, cols - cc));
        P1.viewPart(slices - cs, rows - cr, cols - cc, cs, cr, cc).assign(M.viewPart(0, 0, 0, cs, cr, cc));
        return P1;
    }

    /**
     * Copies pixel values from image processors <code>realIp</code> and
     * <code>imagIp</code> to the complex matrix <code>X</code>
     * 
     * @param realIp
     * @param imagIp
     * @param X
     */
    private static void assignPixelsToMatrix_2D(final ImageProcessor realIp, final ImageProcessor imagIp, final FComplexMatrix2D X) {
        final float[] realPixels;
        final float[] imagPixels;
        if (realIp instanceof FloatProcessor) {
            realPixels = (float[]) realIp.getPixels();
        } else {
            realPixels = (float[]) realIp.convertToFloat().getPixels();
        }
        if (imagIp instanceof FloatProcessor) {
            imagPixels = (float[]) imagIp.getPixels();
        } else {
            imagPixels = (float[]) imagIp.convertToFloat().getPixels();
        }
        final int rows = X.rows();
        final int columns = X.columns();
        final int columnStride = X.columnStride();
        final int rowStride = X.rowStride();
        final int zero = (int)X.index(0, 0);
        final float[] elems = (float[]) X.elements();
        int np = ConcurrencyUtils.getNumberOfProcessors();
        if ((np > 1) && (X.size() >= ConcurrencyUtils.getThreadsBeginN_2D())) {
            Future[] futures = new Future[np];
            int k = rows / np;
            for (int j = 0; j < np; j++) {
                final int startrow = j * k;
                final int stoprow;
                if (j == np - 1) {
                    stoprow = rows;
                } else {
                    stoprow = startrow + k;
                }
                futures[j] = ConcurrencyUtils.submit(new Runnable() {

                    public void run() {
                        int idx = zero + startrow * rowStride;
                        int j = startrow * columns;
                        for (int r = startrow; r < stoprow; r++) {
                            for (int i = idx, c = 0; c < columns; c++) {
                                elems[i] = realPixels[j];
                                elems[i + 1] = imagPixels[j];
                                i += columnStride;
                                j++;
                            }
                            idx += rowStride;
                        }
                    }
                });
            }
            try {
                for (int j = 0; j < np; j++) {
                    futures[j].get();
                }
            } catch (ExecutionException ex) {
                ex.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            int idx = zero;
            int j = 0;
            for (int r = 0; r < rows; r++) {
                for (int i = idx, c = 0; c < columns; c++) {
                    elems[i] = realPixels[j];
                    elems[i + 1] = imagPixels[j];
                    i += columnStride;
                    j++;
                }
                idx += rowStride;
            }
        }
    }

    /**
     * Copies pixel values from image stacks <code>realStack</code> and
     * <code>imagStack</code> to the complex matrix <code>X</code>
     * 
     * @param realStack
     * @param imagStack
     * @param X
     */
    private static void assignPixelsToMatrix_3D(final ImageStack realStack, final ImageStack imagStack, final FComplexMatrix3D X) {
        int slices = X.slices();
        int np = ConcurrencyUtils.getNumberOfProcessors();
        if ((np > 1) && (X.size() >= ConcurrencyUtils.getThreadsBeginN_3D())) {
            Future[] futures = new Future[np];
            int k = slices / np;
            for (int j = 0; j < np; j++) {
                final int startslice = j * k;
                final int stopslice;
                if (j == np - 1) {
                    stopslice = slices;
                } else {
                    stopslice = startslice + k;
                }
                futures[j] = ConcurrencyUtils.submit(new Runnable() {
                    public void run() {
                        for (int s = startslice; s < stopslice; s++) {
                            ImageProcessor realIp = realStack.getProcessor(s + 1);
                            ImageProcessor imagIp = imagStack.getProcessor(s + 1);
                            assignPixelsToMatrix_2D(realIp, imagIp, X.viewSlice(s));
                        }
                    }
                });
            }
            try {
                for (int j = 0; j < np; j++) {
                    futures[j].get();
                }
            } catch (ExecutionException ex) {
                ex.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            for (int s = 0; s < slices; s++) {
                ImageProcessor realIp = realStack.getProcessor(s + 1);
                ImageProcessor imagIp = imagStack.getProcessor(s + 1);
                assignPixelsToMatrix_2D(realIp, imagIp, X.viewSlice(s));
            }
        }
    }

    /**
     * Copies pixel values from image processor <code>ip</code> to matrix
     * <code>X</code>.
     * 
     * @param ip
     *            image processor
     * @return matrix
     * 
     */
    public static FloatMatrix2D assignPixelsToMatrix_2D(final ImageProcessor ip) {
        FloatMatrix2D X;
        if (ip instanceof FloatProcessor) {
            X = new DenseFloatMatrix2D(ip.getHeight(), ip.getWidth(), ((float[]) ip.getPixels()).clone(), 0, 0, ip.getWidth(), 1, false);
        } else {
            X = new DenseFloatMatrix2D(ip.getHeight(), ip.getWidth(), (float[]) ip.convertToFloat().getPixels(), 0, 0, ip.getWidth(), 1, false);
        }
        return X;
    }

    /**
     * Copies pixel values from image processor <code>ip</code> to matrix
     * <code>X</code>.
     * 
     * @param ip
     *            image processor
     * @param X
     *            matrix
     */
    private static void assignPixelsToMatrix_2D(final ImageProcessor ip, final FloatMatrix2D X) {
        if (ip instanceof FloatProcessor) {
            X.assign((float[]) ip.getPixels());
        } else {
            X.assign((float[]) ip.convertToFloat().getPixels());
        }
    }

    /**
     * Copies pixel values from image stack <code>stack</code> to matrix
     * <code>X</code>
     * 
     * @param stack
     *            image stack
     * @param X
     *            matrix
     */
    private static void assignPixelsToMatrix_3D(final ImageStack stack, final FloatMatrix3D X) {
        int slices = X.slices();
        int np = ConcurrencyUtils.getNumberOfProcessors();
        if ((np > 1) && (X.size() >= ConcurrencyUtils.getThreadsBeginN_3D())) {
            Future[] futures = new Future[np];
            int k = slices / np;
            for (int j = 0; j < np; j++) {
                final int startslice = j * k;
                final int stopslice;
                if (j == np - 1) {
                    stopslice = slices;
                } else {
                    stopslice = startslice + k;
                }
                futures[j] = ConcurrencyUtils.submit(new Runnable() {
                    public void run() {
                        for (int s = startslice; s < stopslice; s++) {
                            ImageProcessor ip = stack.getProcessor(s + 1);
                            assignPixelsToMatrix_2D(ip, X.viewSlice(s));
                        }
                    }
                });
            }
            try {
                for (int j = 0; j < np; j++) {
                    futures[j].get();
                }
            } catch (ExecutionException ex) {
                ex.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            for (int s = 0; s < slices; s++) {
                ImageProcessor ip = stack.getProcessor(s + 1);
                assignPixelsToMatrix_2D(ip, X.viewSlice(s));
            }
        }
    }
}