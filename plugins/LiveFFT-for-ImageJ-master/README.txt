Live_FFT README
author: Sunny Chow 
email: sunny.chow@acm.org
sponsor: Direct Electron (http://www.directelectron.com)

Description: 
Live_FFT is an ImageJ plugin that dynamically performs a FFT transform on every image updated.  This plugin is particularly useful when a user wants to see the FFT of an image stream that is updated continuously in  ImageJ and Micro Manager.

Dependencies:
This project is built with Eclipse and expects the following packages to be installed.
1. ImageJ (http://rsbweb.nih.gov/ij/)
2. ParallelFFTJ (http://sites.google.com/site/piotrwendykier/software/parallelfftj)
3. Parallel Colt (http://sites.google.com/site/piotrwendykier/software/parallelcolt)

Build instructions:
-- Adding downloaded files into Eclipse --
1. Start a new workspace in Eclipse.
2. Extract files into the directory selected for the workspace
2. From the main Eclipse menu, select:
  File > Import > General > Existing Projects Into Workspace
	and when prompted, select the "Live_FFT" folder

-- Setting the Build Path --
4. In the Package Explorer, Right click on "plugins".
5. Select "Properties". Go to the "Libraries" tab.
6. Set the correct path to the java libraries: 
	"ij.jar"  
	"parallel_fftj-1.3.jar" 
	"parallelcolt-0.9.4.jar"

-- Building --
9. Edit "build.xml"  Change the pluginsDir property to location of your ImageJ plugins folder.
10. Main Menu > Window > Show View > Ant 
11. Add file: "build.xml"
12. Double click on "compress" to begin build
