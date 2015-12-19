![CSI Logo](/images/CSIlogo.png)

Cornell Spectrum Imager (CSI)
======
 
[JOIN our Mailing List](http://eepurl.com/blODF9): The best way to learn about new CSI versions is through our mailing list. We promise not to share your information with any third party. We will RARELY send you email updates so don't worry about getting spammed weekly. By joining our mailing list, YOU ARE SIGNIFICANTLY HELPING us directly communicate to people interested in this project. Thank you.
 
Introduction
------------
 
Cornell Spectrum Imager is a free, open-source software tool for spectral analysis written by [Paul Cueva](http://www.paulcueva.com/), [Robert Hovden](http://www.roberthovden.com) and [David Muller](http://muller.research.engineering.cornell.edu/) from Cornell's School of Applied and Engineering Physics.
 
An overview of the tool can be found in the article "The Open-Source Cornell Spectrum Imager", Paul Cueva, Robert Hovden, Julia A. Mundy, David A. Muller, **Microscopy Today 21**, 40-45 (2013). [DOI: 10.1017/S1551929512000995][DOI2]

If this software is used in published research, please cite: "Data Processing For Atomic Resolution EELS", Paul Cueva, Robert Hovden, Julia A. Mundy, Huolin L. Xin, David A. Muller, Microscopy and Microanalysis 18, 667-675 (2012) [DOI:10.1017/S1431927612000244](http://journals.cambridge.org/action/displayAbstract?fromPage=online&aid=8653673)
 
Installation
------------

[Download ImageJ and CSI for Mac][Download Mac]

[Download ImageJ and CSI for Windows 64bit][Download Win] ([32bit][Download 32bit]) (On Windows 7 or Vista, don't install in 'Program Files', but a directory that has write permissions, like 'Documents'.)

[Download only the plugins][Download plugin]

You should also increase the allocated memory after installing by selecting `Edit > Options > Memory & Threads...`

Usage
------------

**Importing and Exporting Data**

How do I read a Gatan .dm3 or FEI/Emispec .ser file?

Click on the Open Spectrum Icon on the ImageJ CSI toolset bar (the yellow folder icon). You can also use the plugins directly at the menu Plugins..CSI.. if you have another toolset active. At present, ImageJ's own File..Open menu is not smart enough to read the Spectrum Image versions of Gatan or FEI files.

How do I save a spectrum image?

A Spectrum Image is a calibrated image stack. To avoid losing information, we recommend saving it as a TIFF file. From the ImageJ menu, select FILE..Save As..Tiff... The file can now be read back into ImageJ. Digital Micrograph can also read the file, but will lose the calibration data.

How do I read a spectrum image back into Digital Micrograph?

Gatan has not yet chosen to disclose the internal format of .dm3 files. Until they do, we recommend saving your spectrum image as Tiff. This can then be read (slowly) into Digital Micrograph, where they will appear as image stacks. You will then have to select from the DM menu Spectrum..Convert Data to..EELS, and then calibrate the energy axis.

How do I save a single spectrum?

Click on the Save... Button below the plot in the CSI:Cornell Spectrum Imager Window. This will save the energy axis and all plotted lines as a tab-separated text file.

Principal Components Analysis (PCA)

Why is it taking so long?

We are using UJMP's Singular Value Decomposition for our PCA. For a window with N channels, this diagonalizes a matrix with 2N dimensions, an operation that scales as O(N^3). This means that doubling the window size will take 8 times as long and increasing the window size by a factor of 10 will take 1000 times as long. The lesson is to start with a small window (<20 channels) and see how long it takes, before getting ambitious. There are faster algorithms for extracting the first few PCAs and someday we may implement them.

**General Troubleshooting**

I click on a button and nothing happens

ImageJ is likely out of memory, and not able to add new windows. You should increase the amount of memory available to imageJ by selecting Edit>Options>Memory, increasing the allocated memory and restarting the program. For very large data sets be sure to use the 64-bit version (e.g ImageJ64.app on the Mac), which is found in the ImageJ program folder.


[CSI]: https://code.google.com/archive/p/cornell-spectrum-imager/
[Fiji]: http://imagej.net/Fiji
[CSI wiki]: https://code.google.com/archive/p/cornell-spectrum-imager/wikis/Home.wiki
[Paul Cueva]: http://www.paulcueva.com/
[Robert Hovden]: http://www.roberthovden.com/
[DOI]: https://dx.doi.org/DOI:10.1017/S1431927612000244
[Download Mac]: https://storage.googleapis.com/google-code-archive-downloads/v1/code.google.com/cornell-spectrum-imager/ImageJ+CSI_v1.5.dmg
[Download Win]: https://storage.googleapis.com/google-code-archive-downloads/v1/code.google.com/cornell-spectrum-imager/CSI%20v1.5%20(64bit).exe
[Download 32bit]: https://storage.googleapis.com/google-code-archive-downloads/v1/code.google.com/cornell-spectrum-imager/CSI%20v1.5%20(32bit).exe
[Download plugin]: https://storage.googleapis.com/google-code-archive-downloads/v1/code.google.com/cornell-spectrum-imager/CSI_v1.5%20source.zip
[known bugs]: https://code.google.com/archive/p/cornell-spectrum-imager/issues
[DOI2]: https://dx.doi.org/10.1017/S1551929512000995
[FAQ]: https://code.google.com/archive/p/cornell-spectrum-imager/wikis/FrequentlyAskedQuestions.wiki
[papers]: https://code.google.com/archive/p/cornell-spectrum-imager/wikis/PapersUsingCSI.wiki
[IJ guide]: http://rsb.info.nih.gov/ij/docs/guide/user-guide.pdf
[Muller Group]: http://research.engineering.cornell.edu/muller/csi.cfm
