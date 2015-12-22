package ij.plugin;
import ij.*;
import ij.gui.*;
import ij.util.Tools;
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;


/** This plugin implements the Help/Update ImageJ command. */
public class ImageJ_Updater implements PlugIn {
	private String notes;

	public void run(String arg) {
		if (arg.equals("menus"))
			{updateMenus(); return;}
		if (IJ.getApplet()!=null) return;
		URL url = getClass().getResource("/ij/IJ.class");
		String ij_jar = url == null ? null : url.toString().replaceAll("%20", " ");
		if (ij_jar==null || !ij_jar.startsWith("jar:file:")) {
			error("Could not determine location of ij.jar");
			return;
		}
		int exclamation = ij_jar.indexOf('!');
		ij_jar = ij_jar.substring(9, exclamation);
		if (IJ.debugMode) IJ.log("Updater (jar loc): "+ij_jar);
		File file = new File(ij_jar);
		if (!file.exists()) {
			error("File not found: "+file.getPath());
			return;
		}
		if (!file.canWrite()) {
			String msg = "No write access: "+file.getPath();
			error(msg);
			return;
		}
		String[] list = openUrlAsList(IJ.URL+"/download/jars/list.txt");
		int count = list.length + 3;
		String[] versions = new String[count];
		String[] urls = new String[count];
		String uv = getUpgradeVersion();
		if (uv==null) return;
		versions[0] = "v"+uv;
		urls[0] = IJ.URL+"/upgrade/ij.jar";
		if (versions[0]==null) return;
		for (int i=1; i<count-2; i++) {
			String version = list[i-1];
			versions[i] = version.substring(0,version.length()-1); // remove letter
			urls[i] = IJ.URL+"/download/jars/ij"
				+version.substring(1,2)+version.substring(3,6)+".jar";
		}
		versions[count-2] = "daily build";
		urls[count-2] = IJ.URL+"/ij.jar";
		versions[count-1] = "previous";
		urls[count-1] = IJ.URL+"/upgrade/ij2.jar";
		int choice = showDialog(versions);
		if (choice==-1 || !Commands.closeAll())
			return;
		//System.out.println("choice: "+choice);
		//for (int i=0; i<urls.length; i++) System.out.println("  "+i+" "+urls[i]);
		byte[] jar = null;
		if ("daily build".equals(versions[choice]) && notes!=null && notes.contains(" </title>"))
			jar = getJar("http://wsr.imagej.net/download/daily-build/ij.jar");
		if (jar==null)
			jar = getJar(urls[choice]);
		if (jar==null) {
			error("Unable to download ij.jar from "+urls[choice]);
			return;
		}
		Prefs.savePreferences();
		//System.out.println("saveJar: "+file);
		saveJar(file, jar);
		if (choice<count-2) // force macro Function Finder to download fresh list
			new File(IJ.getDirectory("macros")+"functions.html").delete();
		System.exit(0);
	}

	int showDialog(String[] versions) {
		GenericDialog gd = new GenericDialog("ImageJ Updater");
		gd.addChoice("Upgrade To:", versions, versions[0]);
		String msg = 
			"You are currently running v"+ImageJ.VERSION+ImageJ.BUILD+".\n"+
			" \n"+
			"If you click \"OK\", ImageJ will quit\n"+
			"and you will be running the upgraded\n"+
			"version after you restart ImageJ.\n";
		gd.addMessage(msg);
		gd.showDialog();
		if (gd.wasCanceled())
			return -1;
		else
			return gd.getNextChoiceIndex();
	}

	String getUpgradeVersion() {
		String url = IJ.URL+"/notes.html";
		notes = openUrlAsString(url, 20);
		if (notes==null) {
			error("Unable to connect to "+IJ.URL+". You\n"
				+"may need to use the Edit>Options>Proxy Settings\n"
				+"command to configure ImageJ to use a proxy server.");
			return null;
		}
		int index = notes.indexOf("Version ");
		if (index==-1) {
			error("Release notes are not in the expected format");
			return null;
		}
		String version = notes.substring(index+8, index+13);
		return version;
	}

	String openUrlAsString(String address, int maxLines) {
		StringBuffer sb;
		try {
			URL url = new URL(address);
			InputStream in = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			sb = new StringBuffer();
			int count = 0;
			String line;
			while ((line=br.readLine())!=null && count++<maxLines)
				sb.append (line + "\n");
			in.close ();
		} catch (IOException e) {sb = null;}
			return sb!=null?new String(sb):null;
	}

	byte[] getJar(String address) {
		//System.out.println("getJar: "+address);
		byte[] data;
		try {
			URL url = new URL(address);
			IJ.showStatus("Connecting to "+IJ.URL);
			URLConnection uc = url.openConnection();
			int len = uc.getContentLength();
			if (IJ.debugMode) IJ.log("Updater (url): "+ address + " "+ len);
			if (len<=0)
				return null;
			String name = address.contains("wsr")?"daily build (":"ij.jar (";
			IJ.showStatus("Downloading "+ name + IJ.d2s((double)len/1048576,1)+"MB)");
			InputStream in = uc.getInputStream();
			data = new byte[len];
			int n = 0;
			while (n < len) {
				int count = in.read(data, n, len - n);
				if (count<0)
					throw new EOFException();
	   			 n += count;
				IJ.showProgress(n, len);
			}
			in.close();
		} catch (IOException e) {
			if (IJ.debugMode) IJ.log(""+e);
			return null;
		}
		if (IJ.debugMode) IJ.wait(6000);
		return data;
	}

	/*Changes the name of ij.jar to ij-old.jar
	boolean renameJar(File f) {
		File backup = new File(Prefs.getImageJDir() + "ij-old.jar");
		if (backup.exists()) {
			if (!backup.delete()) {
				error("Unable to delete backup: "+backup.getPath());
				return false;
			}
		}
		if (!f.renameTo(backup)) {
			error("Unable to rename to ij-old.jar: "+f.getPath());
			return false;
		}
		return true;
	}
	*/

	void saveJar(File f, byte[] data) {
		try {
			FileOutputStream out = new FileOutputStream(f);
			out.write(data, 0, data.length);
			out.close();
		} catch (IOException e) {
		}
	}

	String[] openUrlAsList(String address) {
		IJ.showStatus("Connecting to "+IJ.URL);
		Vector v = new Vector();
		try {
			URL url = new URL(address);
			InputStream in = url.openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			while (true) {
				line = br.readLine();
				if (line==null) break;
				if (!line.equals("")) v.addElement(line);
			}
			br.close();
		} catch(Exception e) { }
		String[] lines = new String[v.size()];
		v.copyInto((String[])lines);
		IJ.showStatus("");
		return lines;
	}

	// Use reflection to get version since early versions
	// of ImageJ do not have the IJ.getVersion() method.
	/*
	String version() {
		String version = "";
		try {
			Class ijClass = ImageJ.class;
			Field field = ijClass.getField("VERSION");
			version = (String)field.get(ijClass);
		} catch (Exception ex) {}
		return version;
	}
	*/

	boolean isMac() {
		String osname = System.getProperty("os.name");
		return osname.startsWith("Mac");
	}
	
	void error(String msg) {
		IJ.error("ImageJ Updater", msg);
	}
	
	void updateMenus() {
		if (IJ.debugMode) {
			long start = System.currentTimeMillis();
			Menus.updateImageJMenus();
			IJ.log("Refresh Menus: "+(System.currentTimeMillis()-start)+" ms");
		} else
			Menus.updateImageJMenus();
	}

}
