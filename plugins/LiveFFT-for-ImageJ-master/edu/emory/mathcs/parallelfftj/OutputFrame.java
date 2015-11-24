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

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import ij.*;

/**
 * Output frame.
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * @author Nick Linnenbrügger (nilin@web.de)
 */
public class OutputFrame extends Frame {
    private static final String AUTHOR_EMAIL = "piotr.wendykier@gmail.com";

    private Transformer transformer;

    private FourierDomainOriginType fdOrigin;

    private Button showRealBtn, showImagBtn;
    private Button showFrequencySpecBtn, showPhaseSpecBtn, showPowerSpecBtn;
    private Button showFrequencySpecLogBtn, showPowerSpecLogBtn;

    private CheckboxGroup originCBG;
    private Checkbox zeroOriginCB, centerOriginCB;

    /**
     * Creates new instance of OutputFrame
     * @param title the title of the frame
     * @param transformer transformer
     */
    public OutputFrame(String title, Transformer transformer) {
        super(title);

        this.transformer = transformer;

        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                setVisible(false);
                dispose();
            }
        });

        GridBagLayout gridBag = new GridBagLayout();
        GridBagConstraints constr = new GridBagConstraints();
        this.setLayout(gridBag);

        MyActionL myActionListener = new MyActionL();
        MyItemL myIListener = new MyItemL();

        constr.fill = GridBagConstraints.BOTH;
        constr.anchor = GridBagConstraints.WEST;
        constr.insets = new Insets(3, 3, 3, 3);
        constr.ipadx = 2;
        constr.ipady = 2;
        constr.gridheight = 1;
        constr.gridwidth = 1;

        constr.gridx = 0;
        constr.gridy = GridBagConstraints.RELATIVE;
        this.showRealBtn = this.makeButton("Show " + SpectrumType.REAL_PART.toString(), myActionListener, constr);
        this.showImagBtn = this.makeButton("Show " + SpectrumType.IMAG_PART.toString(), myActionListener, constr);
        this.showFrequencySpecBtn = this.makeButton("Show " + SpectrumType.FREQUENCY_SPECTRUM.toString(), myActionListener, constr);
        this.showFrequencySpecLogBtn = this.makeButton("Show " + SpectrumType.FREQUENCY_SPECTRUM_LOG.toString(), myActionListener, constr);
        this.showPhaseSpecBtn = this.makeButton("Show " + SpectrumType.PHASE_SPECTRUM.toString(), myActionListener, constr);
        this.showPowerSpecBtn = this.makeButton("Show " + SpectrumType.POWER_SPECTRUM.toString(), myActionListener, constr);
        this.showPowerSpecLogBtn = this.makeButton("Show " + SpectrumType.POWER_SPECTRUM_LOG.toString(), myActionListener, constr);

        constr.gridx = 1;
        constr.gridy = 0;
        this.originCBG = new CheckboxGroup();
        this.makeBoldLabel("Fourier Domain Origin", constr);

        constr.gridy = GridBagConstraints.RELATIVE;
        this.zeroOriginCB = this.makeCheckbox(FourierDomainOriginType.AT_ZERO.toString(), this.originCBG, myIListener, constr);
        this.centerOriginCB = this.makeCheckbox(FourierDomainOriginType.AT_CENTER.toString(), this.originCBG, myIListener, constr);

        this.fdOrigin = FourierDomainOriginType.AT_ZERO;
        this.originCBG.setSelectedCheckbox(this.zeroOriginCB);

        //this.setResizable( false );
        this.pack();
        this.setLocation(100, 100);
        this.setVisible(true);
    }

    private Button makeButton(String buttonLabel, ActionListener listener, GridBagConstraints constr) {
        Button button = new Button(buttonLabel);
        this.add(button, constr);
        button.addActionListener(listener);

        return button;
    }

    private Checkbox makeCheckbox(String label, CheckboxGroup cbg, ItemListener listener, GridBagConstraints constr) {
        Checkbox cb = new Checkbox(label, cbg, false);
        this.add(cb, constr);
        cb.addItemListener(listener);

        return cb;
    }

    private Label makeBoldLabel(String labelStr, GridBagConstraints constr) {
        Label label = new Label(labelStr);
        label.setFont(new Font("BoldLabelFont", Font.BOLD, 12));
        this.add(label, constr);

        return label;
    }

    class MyActionL implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            Button source = (Button) event.getSource();
            try {
                disableShowButtonsAndCheckboxes();

                if (source == showRealBtn)
                    transformer.toImagePlus(SpectrumType.REAL_PART, fdOrigin).show();
                else if (source == showImagBtn)
                    transformer.toImagePlus(SpectrumType.IMAG_PART, fdOrigin).show();
                else if (source == showFrequencySpecBtn)
                    transformer.toImagePlus(SpectrumType.FREQUENCY_SPECTRUM, fdOrigin).show();
                else if (source == showFrequencySpecLogBtn)
                    transformer.toImagePlus(SpectrumType.FREQUENCY_SPECTRUM_LOG, fdOrigin).show();
                else if (source == showPhaseSpecBtn)
                    transformer.toImagePlus(SpectrumType.PHASE_SPECTRUM, fdOrigin).show();
                else if (source == showPowerSpecBtn)
                    transformer.toImagePlus(SpectrumType.POWER_SPECTRUM, fdOrigin).show();
                else if (source == showPowerSpecLogBtn)
                    transformer.toImagePlus(SpectrumType.POWER_SPECTRUM_LOG, fdOrigin).show();

                enableShowButtonsAndCheckboxes();
            } catch (OutOfMemoryError exp) {
                IJ.error("Out of memory.\n" + "Try assigning more memory to Java Virtual Machine (JVM)\n" + "that runs ImageJ by appending the -Xmx<memory> parameter to call of JVM.\n" + "Example: 'java -Xmx256m -cp ij.jar ij.ImageJ' starts ImageJ with 256 MB.");
            } catch (Throwable exp) {
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                PrintWriter writer = new PrintWriter(outStream);
                exp.printStackTrace(writer);
                writer.close();

                exp.printStackTrace();

                IJ.error("Uncaught exception during Parallel FFTJ operation!\n" + "Please send a short description of your configuration and\n" + "what you did when this error occured to the author (" + AUTHOR_EMAIL + ").\n" + "Exception message: \"" + exp.getMessage() + "\".\n"
                        + "Exception stack trace:\n" + outStream.toString());
            }
        }
    }

    class MyItemL implements ItemListener {
        public void itemStateChanged(ItemEvent event) {
            ItemSelectable source = (ItemSelectable) event.getItemSelectable();

            if ((source instanceof Checkbox) && (((Checkbox) source).getCheckboxGroup() != null)) {
                CheckboxGroup cbg = ((Checkbox) source).getCheckboxGroup();
                if (cbg == originCBG) {
                    if (cbg.getSelectedCheckbox() == zeroOriginCB)
                        fdOrigin = FourierDomainOriginType.AT_ZERO;
                    else
                        fdOrigin = FourierDomainOriginType.AT_CENTER;
                }
            }
            repaint();
        }
    }

    public Insets getInsets() {
        Insets insets = super.getInsets();
        insets.left += 10;
        insets.right += 10;
        insets.top += 10;
        insets.bottom += 10;
        return insets;
    }

    private void disableShowButtonsAndCheckboxes() {
        this.showRealBtn.setEnabled(false);
        this.showImagBtn.setEnabled(false);
        this.showFrequencySpecBtn.setEnabled(false);
        this.showFrequencySpecLogBtn.setEnabled(false);
        this.showPhaseSpecBtn.setEnabled(false);
        this.showPowerSpecBtn.setEnabled(false);
        this.showPowerSpecLogBtn.setEnabled(false);

        this.zeroOriginCB.setEnabled(false);
        this.centerOriginCB.setEnabled(false);
    }

    private void enableShowButtonsAndCheckboxes() {
        this.showRealBtn.setEnabled(true);
        this.showImagBtn.setEnabled(true);
        this.showFrequencySpecBtn.setEnabled(true);
        this.showFrequencySpecLogBtn.setEnabled(true);
        this.showPhaseSpecBtn.setEnabled(true);
        this.showPowerSpecBtn.setEnabled(true);
        this.showPowerSpecLogBtn.setEnabled(true);

        this.zeroOriginCB.setEnabled(true);
        this.centerOriginCB.setEnabled(true);
    }
}