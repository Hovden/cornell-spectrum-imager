package ij.plugin;
import java.awt.*;
import java.io.*;
import java.util.*;
import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.frame.Editor;
import ij.plugin.Macro_Runner;
import ij.plugin.filter.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

/** Compiles and runs plugins using the javac compiler. */
public class Compiler implements PlugIn, FilenameFilter {

	private static final int TARGET14=0, TARGET15=1, TARGET16=2,  TARGET17=3,  TARGET18=4;
	private static final String[] targets = {"1.4", "1.5", "1.6", "1.7", "1.8"};
	private static final String TARGET_KEY = "javac.target";
	private static CompilerTool compilerTool;
	private static String dir, name;
	private static Editor errors;
	private static boolean generateDebuggingInfo;
	private static int target = (int)Prefs.get(TARGET_KEY, TARGET16);	
	private static boolean checkForUpdateDone;

	public void run(String arg) {
		if (arg.equals("edit"))
			edit();
		else if (arg.equals("options"))
			showDialog();
		else {
			if (arg!=null && arg.length()>0 && !arg.endsWith(".java"))
				IJ.error("Compiler", "File name must end with \".java\"");
			else
				compileAndRun(arg);
		}
	 }
	 
	void edit() {
		if (open("", "Open macro or plugin")) {
			Editor ed = (Editor)IJ.runPlugIn("ij.plugin.frame.Editor", "");
			if (ed!=null) ed.open(dir, name);
		}
	}
	
	void compileAndRun(String path) {
		if (!open(path, "Compile and Run Plugin..."))
			return;
		if (name.endsWith(".class")) {
			runPlugin(name.substring(0, name.length()-1));
			return;
		}
		if (!isJavac()) {
			//boolean pluginClassLoader = this.getClass().getClassLoader()==IJ.getClassLoader();
			//boolean contextClassLoader = Thread.currentThread().getContextClassLoader()==IJ.getClassLoader();
			if (IJ.debugMode) IJ.log("javac not found: ");
			if (!checkForUpdateDone) {
				checkForUpdate("/plugins/compiler/Compiler.jar", "1.48c");
				checkForUpdateDone = true;
			}
			Object compiler = IJ.runPlugIn("Compiler", dir+name);
			if (IJ.debugMode) IJ.log("plugin compiler: "+compiler);
			if (compiler==null) {
				boolean ok = Macro_Runner.downloadJar("/plugins/compiler/Compiler.jar");
				if (ok)
					IJ.runPlugIn("Compiler", dir+name);
			}
			return;
		}
		if (compile(dir+name))
			runPlugin(name);
	}
	
	private void checkForUpdate(String plugin, String currentVersion) {
		int slashIndex = plugin.lastIndexOf("/");
		if (slashIndex==-1 || !plugin.endsWith(".jar"))
			return;
		String className = plugin.substring(slashIndex+1, plugin.length()-4);
		File f = new File(Prefs.getImageJDir()+"plugins"+File.separator+"jars"+File.separator+className+".jar");
		if (!f.exists() || !f.canWrite()) {
			if (IJ.debugMode) IJ.log("checkForUpdate: jar not found ("+plugin+")");
			return;
		}
		String version = null;
		try {
			Class c = IJ.getClassLoader().loadClass("Compiler");
			version = "0.00a";
			Method m = c.getDeclaredMethod("getVersion", new Class[0]);
			version = (String)m.invoke(null, new Object[0]);
		}
		catch (Exception e) {}
		if (version==null) {
			if (IJ.debugMode) IJ.log("checkForUpdate: class not found ("+className+")");
			return;
		}
		if (version.compareTo(currentVersion)>=0) {
			if (IJ.debugMode) IJ.log("checkForUpdate: up to date ("+className+"  "+version+")");
			return;
		}
		boolean ok = Macro_Runner.downloadJar(plugin);
		if (IJ.debugMode) IJ.log("checkForUpdate: "+className+" "+version+" "+ok);
	}
	 
	boolean isJavac() {
		if (compilerTool==null)
			compilerTool=CompilerTool.getDefault();
		return compilerTool!=null;
	}

	boolean compile(String path) {
		IJ.showStatus("compiling "+path);
		String classpath = getClassPath(path);
		Vector options = new Vector();
		if (generateDebuggingInfo)
			options.addElement("-g");
		validateTarget();
		options.addElement("-source");
		options.addElement(targets[target]);
		options.addElement("-target");
		options.addElement(targets[target]);
		options.addElement("-Xlint:unchecked");
		options.addElement("-deprecation");
		options.addElement("-classpath");
		options.addElement(classpath);
		
		Vector sources = new Vector();
		sources.add(path);
		
		if (IJ.debugMode){
			StringBuilder builder = new StringBuilder();
			builder.append("javac");
			for (int i=0; i< options.size(); i++){
				builder.append(" ");
				builder.append(options.get(i));
			}
			for (int i=0; i< sources.size(); i++){
				builder.append(" ");
				builder.append(sources.get(i));
			}
			IJ.log(builder.toString());
		}
		
		boolean errors = true;
		String s = "not compiled";
		if (compilerTool != null) {
			final StringWriter outputWriter = new StringWriter();
			errors = !compilerTool.compile(sources, options, outputWriter);
			s = outputWriter.toString();
		} else {
			errors = true;
		}
		
		if (errors)
			showErrors(s);
		else
			IJ.showStatus("done");
		return !errors;
	 }
	 
	 // Returns a string containing the Java classpath, 
	 // the path to the directory containing the plugin, 
	 // and paths to any .jar files in the plugins folder.
	 String getClassPath(String path) {
		long start = System.currentTimeMillis();
		StringBuffer sb = new StringBuffer();
		sb.append(System.getProperty("java.class.path"));
		File f = new File(path);
		if (f!=null)  // add directory containing file to classpath
			sb.append(File.pathSeparator + f.getParent());
		String pluginsDir = Menus.getPlugInsPath();
		if (pluginsDir!=null)
			addJars(pluginsDir, sb);
		return sb.toString();
	 }
	 
	// Adds .jar files in plugins folder, and subfolders, to the classpath
	void addJars(String path, StringBuffer sb) {
		String[] list = null;
		File f = new File(path);
		if (f.exists() && f.isDirectory())
			list = f.list();
		if (list==null) return;
		if (!path.endsWith(File.separator))
			path += File.separator;
		for (int i=0; i<list.length; i++) {
			File f2 = new File(path+list[i]);
			if (f2.isDirectory())
				addJars(path+list[i], sb);
			else if (list[i].endsWith(".jar")&&(list[i].indexOf("_")==-1||list[i].equals("loci_tools.jar")||list[i].contains("3D_Viewer"))) {
				sb.append(File.pathSeparator+path+list[i]);
				if (IJ.debugMode) IJ.log("javac classpath: "+path+list[i]);
			}
		}
	}
	
	void showErrors(String s) {
		if (errors==null || !errors.isVisible()) {
			errors = (Editor)IJ.runPlugIn("ij.plugin.frame.Editor", "");
			errors.setFont(new Font("Monospaced", Font.PLAIN, 12));
		}
		if (errors!=null)
			errors.display("Errors", s);
		IJ.showStatus("done (errors)");
	}

	 // open the .java source file
	 boolean open(String path, String msg) {
		boolean okay;
		String fileName, directory;
		if (path.equals("")) {
			if (dir==null) dir = IJ.getDirectory("plugins");
			OpenDialog od = new OpenDialog(msg, dir, name);
			directory = od.getDirectory();
			fileName = od.getFileName();
			okay = fileName!=null;
			String lcName = okay?fileName.toLowerCase(Locale.US):null;
			if (okay) {
				if (msg.startsWith("Compile")) {
					if (!(lcName.endsWith(".java")||lcName.endsWith(".class"))) {
						IJ.error("File name must end with \".java\" or \".class\".");
						okay = false;
					}
				} else if (!(lcName.endsWith(".java")||lcName.endsWith(".txt")||lcName.endsWith(".ijm")||lcName.endsWith(".js"))) {
					IJ.error("File name must end with \".java\", \".txt\" or \".js\".");
					okay = false;
				}
			}
		} else {
			int i = path.lastIndexOf('/');
			if (i==-1)
				i = path.lastIndexOf('\\');
			if (i>0) {
				directory = path.substring(0, i+1);
				fileName = path.substring(i+1);
			} else {
				directory = "";
				fileName = path;
			}
			okay = true;
		}
		if (okay) {
			name = fileName;
			dir = directory;
			Editor.setDefaultDirectory(dir);
		}
		return okay;
	}

	// only show files with names ending in ".java"
	// doesn't work with Windows
	public boolean accept(File dir, String name) {
		return name.endsWith(".java")||name.endsWith(".macro")||name.endsWith(".txt");
	}
	
	// run the plugin using a new class loader
	void runPlugin(String name) {
		name = name.substring(0,name.length()-5); // remove ".java"
		new PlugInExecuter(name);
	}
	
	public void showDialog() {
		validateTarget();
		GenericDialog gd = new GenericDialog("Compile and Run");
		gd.addChoice("Target: ", targets, targets[target]);
		gd.setInsets(15,5,0);
		gd.addCheckbox("Generate debugging info (javac -g)", generateDebuggingInfo);
		gd.addHelp(IJ.URL+"/docs/menus/edit.html#compiler");
		gd.showDialog();
		if (gd.wasCanceled()) return;
		target = gd.getNextChoiceIndex();		
		generateDebuggingInfo = gd.getNextBoolean();
		validateTarget();
	}
	
	void validateTarget() {
		if (target<0 || target>TARGET18)
			target = TARGET16;
		if (target>TARGET15 && !(IJ.isJava16()||IJ.isJava17()||IJ.isJava18()))
			target = TARGET15;
		if (target>TARGET16 && !(IJ.isJava17()||IJ.isJava18()))
			target = TARGET16;
		if (target>TARGET17 && !IJ.isJava18())
			target = TARGET17;
		Prefs.set(TARGET_KEY, target);
	}
	
}


class PlugInExecuter implements Runnable {
	private String plugin;
	private Thread thread;

	/** Create a new object that runs the specified plugin
		in a separate thread. */
	PlugInExecuter(String plugin) {
		this.plugin = plugin;
		thread = new Thread(this, plugin);
		thread.setPriority(Math.max(thread.getPriority()-2, Thread.MIN_PRIORITY));
		thread.start();
	}

	public void run() {
		IJ.resetEscape();
		IJ.runPlugIn("ij.plugin.ClassChecker", "");
		runCompiledPlugin(plugin);
	}
	
	void runCompiledPlugin(String className) {
		if (IJ.debugMode) IJ.log("runCompiledPlugin: "+className);
		IJ.resetClassLoader();
		ClassLoader loader = IJ.getClassLoader();
		Object thePlugIn = null;
		try { 
			thePlugIn = (loader.loadClass(className)).newInstance(); 
			if (thePlugIn instanceof PlugIn)
 				((PlugIn)thePlugIn).run("");
 			else if (thePlugIn instanceof PlugInFilter)
				new PlugInFilterRunner(thePlugIn, className, "");
		}
		catch (ClassNotFoundException e) {
			if (className.indexOf('_')!=-1)
				IJ.error("Plugin or class not found: \"" + className + "\"\n(" + e+")");
		}
		catch (NoClassDefFoundError e) {
			String err = e.getMessage();
			int index = err!=null?err.indexOf("wrong name: "):-1;
			if (index>-1 && !className.contains(".")) {
				String className2 = err.substring(index+12, err.length()-1);
				className2 = className2.replace("/", ".");
				runCompiledPlugin(className2);
				return;
			}
			if (className.indexOf('_')!=-1)
				IJ.error("Plugin or class not found: \"" + className + "\"\n(" + e+")");
		}
		catch (Exception e) {
			//IJ.error(""+e);
			IJ.handleException(e); //Marcel Boeglin 2013.09.01
			//Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e); //IDE output
		}
	}
	
}

abstract class CompilerTool {
	public static class JavaxCompilerTool extends CompilerTool {
		protected static Class charsetC;
		protected static Class diagnosticListenerC;
		protected static Class javaFileManagerC;
		protected static Class toolProviderC;

		public boolean compile(List sources, List options, StringWriter log) {
			try {
				Object javac = getJavac();

				Class[] getStandardFileManagerTypes = new Class[] { diagnosticListenerC, Locale.class, charsetC };
				Method getStandardFileManager = javac.getClass().getMethod("getStandardFileManager", getStandardFileManagerTypes);
				Object fileManager = getStandardFileManager.invoke(javac, new Object[] { null, null, null });

				Class[] getJavaFileObjectsFromStringsTypes = new Class[] { Iterable.class };
				Method getJavaFileObjectsFromStrings = fileManager.getClass().getMethod("getJavaFileObjectsFromStrings", getJavaFileObjectsFromStringsTypes);
				Object compilationUnits = getJavaFileObjectsFromStrings.invoke(fileManager, new Object[] { sources });

				Class[] getTaskParamTypes = new Class[] { Writer.class, javaFileManagerC, diagnosticListenerC, Iterable.class, Iterable.class, Iterable.class };
				Method getTask = javac.getClass().getMethod("getTask", getTaskParamTypes);
				Object task = getTask.invoke(javac, new Object[] { log, fileManager, null, options, null, compilationUnits });

				Method call = task.getClass().getMethod("call", new Class[0]);
				Object result = call.invoke(task, new Object[0]);

				return Boolean.TRUE.equals(result);
			} catch (Exception e) {
				PrintWriter printer = new PrintWriter(log);
				e.printStackTrace(printer);
				printer.flush();
			}
			return false;
		}

		protected Object getJavac() throws Exception {
			if (charsetC==null)
				charsetC = Class.forName("java.nio.charset.Charset");
			if (diagnosticListenerC==null)
				diagnosticListenerC = Class.forName("javax.tools.DiagnosticListener");
			if (javaFileManagerC==null)
				javaFileManagerC = Class.forName("javax.tools.JavaFileManager");
			if (toolProviderC==null)
				toolProviderC = Class.forName("javax.tools.ToolProvider");
			Method get = toolProviderC.getMethod("getSystemJavaCompiler", new Class[0]);
			return get.invoke(null, new Object[0]);
		}
	}

	public static class LegacyCompilerTool extends CompilerTool {
		protected static Class javacC;

		boolean areErrors(String s) {
			boolean errors = s != null && s.length() > 0;
			if (errors && s.indexOf("1 warning") > 0 && s.indexOf("[deprecation] show()") > 0)
				errors = false;
			// if(errors&&s.startsWith("Note:com.sun.tools.javac")&&s.indexOf("error")==-1)
			// errors = false;
			return errors;
		}

		public boolean compile(List sources, List options, StringWriter log) {
			try {
				final String[] args = new String[sources.size() + options.size()];
				int argsIndex = 0;
				for (int optionsIndex = 0; optionsIndex < options.size(); optionsIndex++)
					args[argsIndex++] = (String) options.get(optionsIndex);
				for (int sourcesIndex = 0; sourcesIndex < sources.size(); sourcesIndex++)
					args[argsIndex++] = (String) sources.get(sourcesIndex);
				Object javac = getJavac();
				Class[] compileTypes = new Class[] { String[].class, PrintWriter.class };
				Method compile = javacC.getMethod("compile", compileTypes);
				PrintWriter printer = new PrintWriter(log);
				Object result = compile.invoke(javac, new Object[] { args, printer });
				printer.flush();
				return Integer.valueOf(0).equals(result) | areErrors(log.toString());
			} catch (Exception e) {
				e.printStackTrace(new PrintWriter(log));
			}
			return false;
		}

		protected Object getJavac() throws Exception {
			if (javacC==null)
				javacC = Class.forName("com.sun.tools.javac.Main");
			return javacC.newInstance();
		}
	}

	public static CompilerTool getDefault() {
		CompilerTool javax = new JavaxCompilerTool();
		if (javax.isSupported()) {
			if (IJ.debugMode) IJ.log("javac: using javax.tool.JavaCompiler");
			return javax;
		}
		CompilerTool legacy = new LegacyCompilerTool();
		if (legacy.isSupported()) {
			if (IJ.debugMode) IJ.log("javac: using com.sun.tools.javac");
			return legacy;
		}
		return null;
	}

	public abstract boolean compile(List sources, List options, StringWriter log);

	protected abstract Object getJavac() throws Exception;

	public boolean isSupported() {
		try {
			return null != getJavac();
		} catch (Exception e) {
			return false;
		}
	}
}

