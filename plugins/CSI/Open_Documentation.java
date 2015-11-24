import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.io.*;

public class Open_Documentation implements PlugIn {

	public void run(String arg) {
	
	/* To open a PDF using the plugin-----------------------*/
		 String fileName = "plugins"+ File.separator + "CSI" + File.separator + "Documentation.pdf";
		try {
			 
			File pdfFile = new File(fileName);
			if (pdfFile.exists()) {
	 
				if (Desktop.isDesktopSupported()) {
					Desktop.getDesktop().open(pdfFile);
				} else {
					IJ.showMessage("AWT Desktop is not supported!");
				}
	 
			} 
			else {
				IJ.showMessage("The file "+ fileName + " does not exist!");
			}
	 
			IJ.showMessage(fileName);
	 
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		
		
		/*--------To open a URL--------------------
		
		 try {		
			Process p=Runtime.getRuntime().exec("cmd /c start http://bit.ly/Nv2kMf");
		}
		catch(IOException e) {
			e.printStackTrace();
		}
			
		------------------------*/

		
		
		
	}
	

}
