package ij.gui;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import ij.*;
import ij.plugin.frame.Recorder;
import ij.plugin.ScreenGrabber;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.util.Tools;
import ij.macro.*;


/**
 * This class is a customizable modal dialog box. Here is an example
 * GenericDialog with one string field and two numeric fields:
 * <pre>
 *  public class Generic_Dialog_Example implements PlugIn {
 *    static String title="Example";
 *    static int width=512,height=512;
 *    public void run(String arg) {
 *      GenericDialog gd = new GenericDialog("New Image");
 *      gd.addStringField("Title: ", title);
 *      gd.addNumericField("Width: ", width, 0);
 *      gd.addNumericField("Height: ", height, 0);
 *      gd.showDialog();
 *      if (gd.wasCanceled()) return;
 *      title = gd.getNextString();
 *      width = (int)gd.getNextNumber();
 *      height = (int)gd.getNextNumber();
 *      IJ.newImage(title, "8-bit", width, height, 1);
 *   }
 * }
 * </pre>
* To work with macros, the first word of each component label must be 
* unique. If this is not the case, add underscores, which will be converted  
* to spaces when the dialog is displayed. For example, change the checkbox labels
* "Show Quality" and "Show Residue" to "Show_Quality" and "Show_Residue".
*/
public class GenericDialog extends Dialog implements ActionListener, TextListener, 
FocusListener, ItemListener, KeyListener, AdjustmentListener, WindowListener {

	protected Vector numberField, stringField, checkbox, choice, slider, radioButtonGroups;
	protected TextArea textArea1, textArea2;
	protected Vector defaultValues,defaultText,defaultStrings,defaultChoiceIndexes;
	protected Component theLabel;
	private Button cancel, okay, no, help;
	private String okLabel = "  OK  ";
	private String cancelLabel = "Cancel";
	private String helpLabel = "Help";
    private boolean wasCanceled, wasOKed;
    private int y;
    private int nfIndex, sfIndex, cbIndex, choiceIndex, textAreaIndex, radioButtonIndex;
	private GridBagLayout grid;
	private GridBagConstraints c;
	private boolean firstNumericField=true;
	private boolean firstSlider=true;
	private boolean invalidNumber;
	private String errorMessage;
	private boolean firstPaint = true;
	private Hashtable labels;
	private boolean macro;
	private String macroOptions;
	private int topInset, leftInset, bottomInset;
    private boolean customInsets;
    private Vector sliderIndexes;
    private Vector sliderScales;
    private Checkbox previewCheckbox;    // the "Preview" Checkbox, if any
    private Vector dialogListeners;             // the Objects to notify on user input
    private PlugInFilterRunner pfr;      // the PlugInFilterRunner for automatic preview
    private String previewLabel = " Preview";
    private final static String previewRunning = "wait...";
    private boolean recorderOn;         // whether recording is allowed
    private boolean yesNoCancel;
    private char echoChar;
    private boolean hideCancelButton;
    private boolean centerDialog = true;
    private String helpURL;
    private String yesLabel, noLabel;
    private boolean smartRecording;
    private Vector imagePanels;
    private static GenericDialog instance;

    /** Creates a new GenericDialog with the specified title. Uses the current image
    	image window as the parent frame or the ImageJ frame if no image windows
    	are open. Dialog parameters are recorded by ImageJ's command recorder but
    	this requires that the first word of each label be unique. */
	public GenericDialog(String title) {
		this(title, getParentFrame());
	}
	
	private static Frame getParentFrame() {
		Frame parent = WindowManager.getCurrentImage()!=null?
			(Frame)WindowManager.getCurrentImage().getWindow():IJ.getInstance()!=null?IJ.getInstance():new Frame();
		if (IJ.isMacOSX() && IJ.isJava18()) {
			ImageJ ij = IJ.getInstance();
			if (ij!=null && ij.isActive())
				parent = ij;
			else
				parent = null;
		}
		return parent;
	}

    /** Creates a new GenericDialog using the specified title and parent frame. */
    public GenericDialog(String title, Frame parent) {
		super(parent==null?new Frame():parent, title, true);
		if (Prefs.blackCanvas) {
			setForeground(SystemColor.controlText);
			setBackground(SystemColor.control);
		}
		//if (IJ.isLinux())
		//	setBackground(new Color(238, 238, 238));
		grid = new GridBagLayout();
		c = new GridBagConstraints();
		setLayout(grid);
		macroOptions = Macro.getOptions();
		macro = macroOptions!=null;
		addKeyListener(this);
		addWindowListener(this);
    }
    
	//void showFields(String id) {
	//	String s = id+": ";
	//	for (int i=0; i<maxItems; i++)
	//		if (numberField[i]!=null)
	//			s += i+"='"+numberField[i].getText()+"' ";
	//	IJ.write(s);
	//}

	/** Adds a numeric field. The first word of the label must be
		unique or command recording will not work.
	* @param label			the label
	* @param defaultValue	value to be initially displayed
	* @param digits			number of digits to right of decimal point
	*/
	public void addNumericField(String label, double defaultValue, int digits) {
		addNumericField(label, defaultValue, digits, 6, null);
	}

	/** Adds a numeric field. The first word of the label must be
		unique or command recording will not work.
	* @param label			the label
	* @param defaultValue	value to be initially displayed
	* @param digits			number of digits to right of decimal point
	* @param columns		width of field in characters
	* @param units			a string displayed to the right of the field
	*/
   public void addNumericField(String label, double defaultValue, int digits, int columns, String units) {
   		String label2 = label;
   		if (label2.indexOf('_')!=-1)
   			label2 = label2.replace('_', ' ');
		Label theLabel = makeLabel(label2);
		c.gridx = 0; c.gridy = y;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		if (firstNumericField)
			c.insets = getInsets(5, 0, 3, 0);
		else
			c.insets = getInsets(0, 0, 3, 0);
		grid.setConstraints(theLabel, c);
		add(theLabel);
		if (numberField==null) {
			numberField = new Vector(5);
			defaultValues = new Vector(5);
			defaultText = new Vector(5);
		}
		if (IJ.isWindows()) columns -= 2;
		if (columns<1) columns = 1;
		String defaultString = IJ.d2s(defaultValue, digits);
		if (Double.isNaN(defaultValue))
			defaultString = "";
		TextField tf = new TextField(defaultString, columns);
		if (IJ.isLinux()) tf.setBackground(Color.white);
		tf.addActionListener(this);
		tf.addTextListener(this);
		tf.addFocusListener(this);
		tf.addKeyListener(this);
		numberField.addElement(tf);
		defaultValues.addElement(new Double(defaultValue));
		defaultText.addElement(tf.getText());
		c.gridx = 1; c.gridy = y;
		c.anchor = GridBagConstraints.WEST;
		tf.setEditable(true);
		//if (firstNumericField) tf.selectAll();
		firstNumericField = false;
		if (units==null||units.equals("")) {
			grid.setConstraints(tf, c);
			add(tf);
		} else {
    		Panel panel = new Panel();
			panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    		panel.add(tf);
			panel.add(new Label(" "+units));
			grid.setConstraints(panel, c);
			add(panel);    		
		}
		if (Recorder.record || macro)
			saveLabel(tf, label);
		y++;
    }
    
    private Label makeLabel(String label) {
    	if (IJ.isMacintosh())
    		label += " ";
		return new Label(label);
    }
    
    private void saveLabel(Object component, String label) {
    	if (labels==null)
    		labels = new Hashtable();
    	if (label.length()>0) {
    		if (label.charAt(0)==' ')
    			label = label.trim();
			labels.put(component, label);
		}
    }
    
	/** Adds an 8 column text field.
	* @param label			the label
	* @param defaultText		the text initially displayed
	*/
	public void addStringField(String label, String defaultText) {
		addStringField(label, defaultText, 8);
	}

	/** Adds a text field.
	* @param label			the label
	* @param defaultText		text initially displayed
	* @param columns			width of the text field
	*/
	public void addStringField(String label, String defaultText, int columns) {
   		String label2 = label;
   		if (label2.indexOf('_')!=-1)
   			label2 = label2.replace('_', ' ');
		Label theLabel = makeLabel(label2);
		c.gridx = 0; c.gridy = y;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		boolean custom = customInsets;
		if (stringField==null) {
			stringField = new Vector(4);
			defaultStrings = new Vector(4);
			c.insets = getInsets(5, 0, 5, 0);
		} else
			c.insets = getInsets(0, 0, 5, 0);
		grid.setConstraints(theLabel, c);
		add(theLabel);
		if (custom) {
			if (stringField.size()==0)
				c.insets = getInsets(5, 0, 5, 0);
			else
				c.insets = getInsets(0, 0, 5, 0);
		}
		TextField tf = new TextField(defaultText, columns);
		if (IJ.isLinux()) tf.setBackground(Color.white);
		tf.setEchoChar(echoChar);
		echoChar = 0;
		tf.addActionListener(this);
		tf.addTextListener(this);
		tf.addFocusListener(this);
		tf.addKeyListener(this);
		c.gridx = 1; c.gridy = y;
		c.anchor = GridBagConstraints.WEST;
		grid.setConstraints(tf, c);
		tf.setEditable(true);
		add(tf);
		stringField.addElement(tf);
		defaultStrings.addElement(defaultText);
		if (Recorder.record || macro)
			saveLabel(tf, label);
		y++;
    }
    
    /** Sets the echo character for the next string field. */
    public void setEchoChar(char echoChar) {
    	this.echoChar = echoChar;
    }
    
	/** Adds a checkbox.
	* @param label			the label
	* @param defaultValue	the initial state
	*/
    public void addCheckbox(String label, boolean defaultValue) {
        addCheckbox(label, defaultValue, false);
    }

    /** Adds a checkbox; does not make it recordable if isPreview is true.
     * With isPreview true, the checkbox can be referred to as previewCheckbox
     * from hereon.
     */
    private void addCheckbox(String label, boolean defaultValue, boolean isPreview) {
    	String label2 = label;
   		if (label2.indexOf('_')!=-1)
   			label2 = label2.replace('_', ' ');
    	if (checkbox==null) {
    		checkbox = new Vector(4);
			c.insets = getInsets(15, 20, 0, 0);
    	} else
			c.insets = getInsets(0, 20, 0, 0);
		c.gridx = 0; c.gridy = y;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;
		Checkbox cb = new Checkbox(label2);
		grid.setConstraints(cb, c);
		cb.setState(defaultValue);
		cb.addItemListener(this);
		cb.addKeyListener(this);
		add(cb);
		checkbox.addElement(cb);
		//ij.IJ.write("addCheckbox: "+ y+" "+cbIndex);
        if (!isPreview &&(Recorder.record || macro)) //preview checkbox is not recordable
			saveLabel(cb, label);
        if (isPreview) previewCheckbox = cb;
		y++;
    }

    /** Adds a checkbox labelled "Preview" for "automatic" preview.
     * The reference to this checkbox can be retrieved by getPreviewCheckbox()
     * and it provides the additional method previewRunning for optical
     * feedback while preview is prepared.
     * PlugInFilters can have their "run" method automatically called for
     * preview under the following conditions:
     * - the PlugInFilter must pass a reference to itself (i.e., "this") as an
     *   argument to the AddPreviewCheckbox
     * - it must implement the DialogListener interface and set the filter
     *   parameters in the dialogItemChanged method.
     * - it must have DIALOG and PREVIEW set in its flags.
     * A previewCheckbox is always off when the filter is started and does not get
     * recorded by the Macro Recorder.
     *
     * @param pfr A reference to the PlugInFilterRunner calling the PlugInFilter
     * if automatic preview is desired, null otherwise.
     */
    public void addPreviewCheckbox(PlugInFilterRunner pfr) {
        if (previewCheckbox != null)
        	return;
    	ImagePlus imp = WindowManager.getCurrentImage();
		if (imp!=null && imp.isComposite() && ((CompositeImage)imp).getMode()==IJ.COMPOSITE)
			return;
        this.pfr = pfr;
        addCheckbox(previewLabel, false, true);
    }

    /** Add the preview checkbox with user-defined label; for details see the
     *  addPreviewCheckbox method with standard "Preview" label.
     * Adds the checkbox when the current image is a CompositeImage
     * in "Composite" mode, unlike the one argument version.
     * Note that a GenericDialog can have only one PreviewCheckbox.
     */
    public void addPreviewCheckbox(PlugInFilterRunner pfr, String label) {
        if (previewCheckbox!=null)
        	return;
    	//ImagePlus imp = WindowManager.getCurrentImage();
		//if (imp!=null && imp.isComposite() && ((CompositeImage)imp).getMode()==IJ.COMPOSITE)
		//	return;
        previewLabel = label;
        this.pfr = pfr;
        addCheckbox(previewLabel, false, true);
    }

    /** Adds a group of checkboxs using a grid layout.
	* @param rows			the number of rows
	* @param columns		the number of columns
	* @param labels			the labels
	* @param defaultValues	the initial states
	*/
    public void addCheckboxGroup(int rows, int columns, String[] labels, boolean[] defaultValues) {
    	addCheckboxGroup(rows, columns, labels, defaultValues, null);
    }

    /** Adds a group of checkboxs using a grid layout.
	* @param rows			the number of rows
	* @param columns		the number of columns
	* @param labels			the labels
	* @param defaultValues	the initial states
	* @param headings	the column headings
	* Example: http://imagej.nih.gov/ij/plugins/multi-column-dialog/index.html
	*/
    public void addCheckboxGroup(int rows, int columns, String[] labels, boolean[] defaultValues, String[] headings) {
    	Panel panel = new Panel();
    	int nRows = headings!=null?rows+1:rows;
    	panel.setLayout(new GridLayout(nRows, columns, 6, 0));
    	int startCBIndex = cbIndex;
    	if (checkbox==null)
    		checkbox = new Vector(12);
    	if (headings!=null) {
    		Font font = new Font("SansSerif", Font.BOLD, 12);
			for (int i=0; i<columns; i++) {
				if (i>headings.length-1 || headings[i]==null)
					panel.add(new Label(""));
				else {
					Label label = new Label(headings[i]);
					label.setFont(font);
					panel.add(label);
				}
			}
    	}
    	int i1 = 0;
    	int[] index = new int[labels.length];
    	for (int row=0; row<rows; row++) {
			for (int col=0; col<columns; col++) {
				int i2 = col*rows+row;
				if (i2>=labels.length) break;
				index[i1] = i2;
				String label = labels[i1];
				if (label==null || label.length()==0) {
					Label lbl = new Label("");
					panel.add(lbl);
					i1++;
					continue;
				}
				if (label.indexOf('_')!=-1)
   					label = label.replace('_', ' ');
				Checkbox cb = new Checkbox(label);
				checkbox.addElement(cb);
				cb.setState(defaultValues[i1]);
				cb.addItemListener(this);
				if (Recorder.record || macro)
					saveLabel(cb, labels[i1]);
				if (IJ.isLinux()) {
					Panel panel2 = new Panel();
					panel2.setLayout(new BorderLayout());
					panel2.add("West", cb);
					panel.add(panel2);
				} else
					panel.add(cb);
 				i1++;
			}
		}
		c.gridx = 0; c.gridy = y;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;
		c.insets = getInsets(10, 0, 0, 0);
		grid.setConstraints(panel, c);
		add(panel);
		y++;
    }

    /** Adds a radio button group.
	* @param label			group label (or null)
	* @param items		radio button labels
	* @param rows			number of rows
	* @param columns	number of columns
	* @param defaultItem		button initially selected
	*/
    public void addRadioButtonGroup(String label, String[] items, int rows, int columns, String defaultItem) {
    	Panel panel = new Panel();
    	int n = items.length;
     	panel.setLayout(new GridLayout(rows, columns, 0, 0));
		CheckboxGroup cg = new CheckboxGroup();
		for (int i=0; i<n; i++) {
			Checkbox cb = new Checkbox(items[i],cg,items[i].equals(defaultItem));
			cb.addItemListener(this);
			panel.add(cb);
		}
		if (radioButtonGroups==null)
			radioButtonGroups = new Vector();
		radioButtonGroups.addElement(cg);
		Insets insets = getInsets(5, 10, 0, 0);
		if (label==null || label.equals("")) {
			label = "rbg"+radioButtonGroups.size();
			insets.top += 5;
		} else {
			setInsets(10, insets.left, 0);
			addMessage(label);
			insets.top = 2;
			insets.left += 10;
		}
		c.gridx = 0; c.gridy = y;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(insets.top, insets.left, 0, 0);
		grid.setConstraints(panel, c);
		add(panel);
		if (Recorder.record || macro)
			saveLabel(cg, label);
		y++;
    }

    /** Adds a popup menu.
   * @param label	the label
   * @param items	the menu items
   * @param defaultItem	the menu item initially selected
   */
   public void addChoice(String label, String[] items, String defaultItem) {
   		String label2 = label;
   		if (label2.indexOf('_')!=-1)
   			label2 = label2.replace('_', ' ');
		Label theLabel = makeLabel(label2);
		c.gridx = 0; c.gridy = y;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		if (choice==null) {
			choice = new Vector(4);
			defaultChoiceIndexes = new Vector(4);
			c.insets = getInsets(5, 0, 5, 0);
		} else
			c.insets = getInsets(0, 0, 5, 0);
		grid.setConstraints(theLabel, c);
		add(theLabel);
		Choice thisChoice = new Choice();
		thisChoice.addKeyListener(this);
		thisChoice.addItemListener(this);
		for (int i=0; i<items.length; i++)
			thisChoice.addItem(items[i]);
		if (defaultItem!=null)
			thisChoice.select(defaultItem);
		else
			thisChoice.select(0);
		c.gridx = 1; c.gridy = y;
		c.anchor = GridBagConstraints.WEST;
		grid.setConstraints(thisChoice, c);
		add(thisChoice);
		choice.addElement(thisChoice);
		int index = thisChoice.getSelectedIndex();
		defaultChoiceIndexes.addElement(new Integer(index));
		if (Recorder.record || macro)
			saveLabel(thisChoice, label);
		y++;
    }
    
    /** Adds a message consisting of one or more lines of text. */
    public void addMessage(String text) {
    	addMessage(text, null, null);
    }

    /** Adds a message consisting of one or more lines of text,
    	which will be displayed using the specified font. */
    public void addMessage(String text, Font font) {
    	addMessage(text, font, null);
    }
    
    /** Adds a message consisting of one or more lines of text,
    	which will be displayed using the specified font and color. */
    public void addMessage(String text, Font font, Color color) {
    	theLabel = null;
    	if (text.indexOf('\n')>=0)
			theLabel = new MultiLineLabel(text);
		else
			theLabel = new Label(text);
		//theLabel.addKeyListener(this);
		c.gridx = 0; c.gridy = y;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;
		c.insets = getInsets(text.equals("")?0:10, 20, 0, 0);
		c.fill = GridBagConstraints.HORIZONTAL;
		grid.setConstraints(theLabel, c);
		if (font!=null)
			theLabel.setFont(font);
		if (color!=null)
			theLabel.setForeground(color);
		add(theLabel);
		c.fill = GridBagConstraints.NONE;
		y++;
    }
    
	/** Adds one or two (side by side) text areas.
	* @param text1	initial contents of the first text area
	* @param text2	initial contents of the second text area or null
	* @param rows	the number of rows
	* @param columns	the number of columns
	*/
    public void addTextAreas(String text1, String text2, int rows, int columns) {
		if (textArea1!=null) return;
		Panel panel = new Panel();
		Font font = new Font("SansSerif", Font.PLAIN, 14);
		textArea1 = new TextArea(text1,rows,columns,TextArea.SCROLLBARS_NONE);
		if (IJ.isLinux()) textArea1.setBackground(Color.white);
		textArea1.setFont(font);
		textArea1.addTextListener(this);
		panel.add(textArea1);
		if (text2!=null) {
			textArea2 = new TextArea(text2,rows,columns,TextArea.SCROLLBARS_NONE);
			if (IJ.isLinux()) textArea2.setBackground(Color.white);
			textArea2.setFont(font);
			panel.add(textArea2);
		}
		c.gridx = 0; c.gridy = y;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;
		c.insets = getInsets(15, 20, 0, 0);
		grid.setConstraints(panel, c);
		add(panel);
		y++;
    }
    
	/**
	* Adds a slider (scroll bar) to the dialog box.
	* Floating point values will be used if (maxValue-minValue)<=5.0
	* and either minValue or maxValue are non-integer.
	* @param label	 the label
	* @param minValue  the minimum value of the slider
	* @param maxValue  the maximum value of the slider
	* @param defaultValue  the initial value of the slider
	*/
	public void addSlider(String label, double minValue, double maxValue, double defaultValue) {
		if (defaultValue<minValue) defaultValue=minValue;
		if (defaultValue>maxValue) defaultValue=maxValue;
		int columns = 4;
		int digits = 0;
		double scale = 1.0;
		if ((maxValue-minValue)<=5.0 && (minValue!=(int)minValue||maxValue!=(int)maxValue||defaultValue!=(int)defaultValue)) {
			scale = 20.0;
			minValue *= scale;
			maxValue *= scale;
			defaultValue *= scale;
			digits = 2;
		}
   		String label2 = label;
   		if (label2.indexOf('_')!=-1)
   			label2 = label2.replace('_', ' ');
		Label theLabel = makeLabel(label2);
		c.gridx = 0; c.gridy = y;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		c.insets = new Insets(0, 0, 3, 0);
		grid.setConstraints(theLabel, c);
		add(theLabel);
		
		if (slider==null) {
			slider = new Vector(5);
			sliderIndexes = new Vector(5);
			sliderScales = new Vector(5);
		}
		Scrollbar s = new Scrollbar(Scrollbar.HORIZONTAL, (int)defaultValue, 1, (int)minValue, (int)maxValue+1);
		slider.addElement(s);
		s.addAdjustmentListener(this);
		s.setUnitIncrement(1);

		if (numberField==null) {
			numberField = new Vector(5);
			defaultValues = new Vector(5);
			defaultText = new Vector(5);
		}
		if (IJ.isWindows()) columns -= 2;
		if (columns<1) columns = 1;
		TextField tf = new TextField(IJ.d2s(defaultValue/scale, digits), columns);
		if (IJ.isLinux()) tf.setBackground(Color.white);
		tf.addActionListener(this);
		tf.addTextListener(this);
		tf.addFocusListener(this);
		tf.addKeyListener(this);
		numberField.addElement(tf);
		sliderIndexes.add(new Integer(numberField.size()-1));
		sliderScales.add(new Double(scale));
		defaultValues.addElement(new Double(defaultValue/scale));
		defaultText.addElement(tf.getText());
		tf.setEditable(true);
		firstSlider = false;
		
    	Panel panel = new Panel();
		GridBagLayout pgrid = new GridBagLayout();
		GridBagConstraints pc  = new GridBagConstraints();
		panel.setLayout(pgrid);
		pc.gridx = 0; pc.gridy = 0;
		pc.gridwidth = 1;
		pc.ipadx = 85;
		pc.anchor = GridBagConstraints.WEST;
		pgrid.setConstraints(s, pc);
		panel.add(s);
		pc.ipadx = 0;  // reset
		// text field
		pc.gridx = 1;
		pc.insets = new Insets(5, 5, 0, 0);
		pc.anchor = GridBagConstraints.EAST;
		pgrid.setConstraints(tf, pc);
    	panel.add(tf);
    	
		grid.setConstraints(panel, c);
		c.gridx = 1; c.gridy = y;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(0, 0, 0, 0);
		grid.setConstraints(panel, c);
		add(panel);
		y++;
		if (Recorder.record || macro)
			saveLabel(tf, label);
    }

    /** Adds a Panel to the dialog. */
    public void addPanel(Panel panel) {
    	addPanel(panel , GridBagConstraints.WEST, getInsets(5,0,0,0));
    }

    /** Adds a Panel to the dialog with custom contraint and insets. The
    	defaults are GridBagConstraints.WEST (left justified) and 
    	"new Insets(5, 0, 0, 0)" (5 pixels of padding at the top). */
    public void addPanel(Panel panel, int constraints, Insets insets) {
		c.gridx = 0; c.gridy = y;
		c.gridwidth = 2;
		c.anchor = constraints;
		c.insets = insets;
		grid.setConstraints(panel, c);
		add(panel);
		y++;
    }
    
	/** Adds an image to the dialog. */
    public void addImage(ImagePlus image) {
    	ImagePanel imagePanel = new ImagePanel(image);
    	addPanel(imagePanel);
    	if (imagePanels==null)
    		imagePanels = new Vector();
    	imagePanels.add(imagePanel);
    }

    
    /** Set the insets (margins), in pixels, that will be 
    	used for the next component added to the dialog.
    <pre>
    Default insets:
        addMessage: 0,20,0 (empty string) or 10,20,0
        addCheckbox: 15,20,0 (first checkbox) or 0,20,0
        addCheckboxGroup: 10,0,0 
        addRadioButtonGroup: 5,10,0 
        addNumericField: 5,0,3 (first field) or 0,0,3
        addStringField: 5,0,5 (first field) or 0,0,5
        addChoice: 5,0,5 (first field) or 0,0,5
     </pre>
    */
    public void setInsets(int top, int left, int bottom) {
    	topInset = top;
    	leftInset = left;
    	bottomInset = bottom;
    	customInsets = true;
    }
    
    /** Sets a replacement label for the "OK" button. */
    public void setOKLabel(String label) {
    	okLabel = label;
    }

    /** Sets a replacement label for the "Cancel" button. */
    public void setCancelLabel(String label) {
    	cancelLabel = label;
    }

    /** Sets a replacement label for the "Help" button. */
    public void setHelpLabel(String label) {
    	helpLabel = label;
    }

    /** Unchanged parameters are not recorder in 'smart recording' mode. */
    public void setSmartRecording(boolean smartRecording) {
    	this.smartRecording = smartRecording;
    }

    /** Make this a "Yes No Cancel" dialog. */
    public void enableYesNoCancel() {
    	enableYesNoCancel(" Yes ", " No ");
    }
    
    /** Make this a "Yes No Cancel" dialog with custom labels. Here is an example:
    	<pre>
        GenericDialog gd = new GenericDialog("YesNoCancel Demo");
        gd.addMessage("This is a custom YesNoCancel dialog");
        gd.enableYesNoCancel("Do something", "Do something else");
        gd.showDialog();
        if (gd.wasCanceled())
            IJ.log("User clicked 'Cancel'");
        else if (gd.wasOKed())
            IJ. log("User clicked 'Yes'");
        else
            IJ. log("User clicked 'No'");
    	</pre>
	*/
    public void enableYesNoCancel(String yesLabel, String noLabel) {
    	this.yesLabel = yesLabel;
    	this.noLabel = noLabel;
    	yesNoCancel = true;
    }

    /** No not display "Cancel" button. */
    public void hideCancelButton() {
    	hideCancelButton = true;
    }

	Insets getInsets(int top, int left, int bottom, int right) {
		if (customInsets) {
			customInsets = false;
			return new Insets(topInset, leftInset, bottomInset, 0);
		} else
			return new Insets(top, left, bottom, right);
	}

    /** Add an Object implementing the DialogListener interface. This object will
     * be notified by its dialogItemChanged method of input to the dialog. The first
     * DialogListener will be also called after the user has typed 'OK' or if the
     * dialog has been invoked by a macro; it should read all input fields of the
     * dialog.
     * For other listeners, the OK button will not cause a call to dialogItemChanged;
     * the CANCEL button will never cause such a call.
     * @param dl the Object that wants to listen.
     */    
    public void addDialogListener(DialogListener dl) {
        if (dialogListeners == null)
            dialogListeners = new Vector();
        dialogListeners.addElement(dl);
        if (IJ.debugMode) IJ.log("GenericDialog: Listener added: "+dl);
    }

	/** Returns true if the user clicked on "Cancel". */
    public boolean wasCanceled() {
    	if (wasCanceled)
    		Macro.abort();
    	return wasCanceled;
    }
    
	/** Returns true if the user has clicked on "OK" or a macro is running. */
    public boolean wasOKed() {
    	return wasOKed || macro;
    }

	/** Returns the contents of the next numeric field,
		or NaN if the field does not contain a number. */
   public double getNextNumber() {
		if (numberField==null)
			return -1.0;
		TextField tf = (TextField)numberField.elementAt(nfIndex);
		String theText = tf.getText();
        String label=null;
		if (macro) {
			label = (String)labels.get((Object)tf);
			theText = Macro.getValue(macroOptions, label, theText);
			//IJ.write("getNextNumber: "+label+"  "+theText);
		}	
		String originalText = (String)defaultText.elementAt(nfIndex);
		double defaultValue = ((Double)(defaultValues.elementAt(nfIndex))).doubleValue();
		double value;
		boolean skipRecording = false;
		if (theText.equals(originalText)) {
			value = defaultValue;
			if (smartRecording) skipRecording=true;
		} else {
			Double d = getValue(theText);
			if (d!=null)
				value = d.doubleValue();
			else {
				// Is the value a macro variable?
				if (theText.startsWith("&")) theText = theText.substring(1);
				Interpreter interp = Interpreter.getInstance();
				value = interp!=null?interp.getVariable2(theText):Double.NaN;
				if (Double.isNaN(value)) {
					invalidNumber = true;
					errorMessage = "\""+theText+"\" is an invalid number";
					value = Double.NaN;
					if (macro) {
						IJ.error("Macro Error", "Numeric value expected in run() function\n \n"
							+"   Dialog box title: \""+getTitle()+"\"\n"
							+"   Key: \""+label.toLowerCase(Locale.US)+"\"\n"
							+"   Value or variable name: \""+theText+"\"");
					}
                }
			}
		}
		if (recorderOn && !skipRecording) {
			recordOption(tf, trim(theText));
		}
		nfIndex++;
		return value;
    }
    
	private String trim(String value) {
		if (value.endsWith(".0"))
			value = value.substring(0, value.length()-2);
		if (value.endsWith(".00"))
			value = value.substring(0, value.length()-3);
		return value;
	}

	private void recordOption(Object component, String value) {
		String label = (String)labels.get(component);
		if (value.equals("")) value = "[]";
		Recorder.recordOption(label, value);
	}

	private void recordCheckboxOption(Checkbox cb) {
		String label = (String)labels.get((Object)cb);
		if (label!=null) {
			if (cb.getState()) // checked
				Recorder.recordOption(label);
			else if (Recorder.getCommandOptions()==null)
				Recorder.recordOption(" ");
		}
	}

 	protected Double getValue(String text) {
 		Double d;
 		try {d = new Double(text);}
		catch (NumberFormatException e){
			d = null;
		}
		return d;
	}

	public double parseDouble(String s) {
		if (s==null) return Double.NaN;
		double value = Tools.parseDouble(s);
		if (Double.isNaN(value)) {
			if (s.startsWith("&")) s = s.substring(1);
			Interpreter interp = Interpreter.getInstance();
			value = interp!=null?interp.getVariable2(s):Double.NaN;
		}
		return value;
	}
	
	/** Returns true if one or more of the numeric fields contained an  
		invalid number. Must be called after one or more calls to getNextNumber(). */
   public boolean invalidNumber() {
    	boolean wasInvalid = invalidNumber;
    	invalidNumber = false;
    	return wasInvalid;
    }
    
	/** Returns an error message if getNextNumber was unable to convert a 
		string into a number, otherwise, returns null. */
	public String getErrorMessage() {
		return errorMessage;
   	}

  	/** Returns the contents of the next text field. */
   public String getNextString() {
   		String theText;
		if (stringField==null)
			return "";
		TextField tf = (TextField)(stringField.elementAt(sfIndex));
		theText = tf.getText();
		if (macro) {
			String label = (String)labels.get((Object)tf);
			theText = Macro.getValue(macroOptions, label, theText);
			if (theText!=null && (theText.startsWith("&")||label.toLowerCase(Locale.US).startsWith(theText))) {
				// Is the value a macro variable?
				if (theText.startsWith("&")) theText = theText.substring(1);
				Interpreter interp = Interpreter.getInstance();
				String s = interp!=null?interp.getVariableAsString(theText):null;
				if (s!=null) theText = s;
			}
		}	
		if (recorderOn) {
			String s = theText;
			if (s!=null&&s.length()>=3&&Character.isLetter(s.charAt(0))&&s.charAt(1)==':'&&s.charAt(2)=='\\')
				s = s.replaceAll("\\\\", "\\\\\\\\");  // replace "\" with "\\" in Windows file paths
			if (!smartRecording || !s.equals((String)defaultStrings.elementAt(sfIndex)))
				recordOption(tf, s);
			else if (Recorder.getCommandOptions()==null)
				Recorder.recordOption(" ");
		}
		sfIndex++;
		return theText;
    }
    
  	/** Returns the state of the next checkbox. */
    public boolean getNextBoolean() {
		if (checkbox==null)
			return false;
		Checkbox cb = (Checkbox)(checkbox.elementAt(cbIndex));
		if (recorderOn)
			recordCheckboxOption(cb);
		boolean state = cb.getState();
		if (macro) {
			String label = (String)labels.get((Object)cb);
			String key = Macro.trimKey(label);
			state = isMatch(macroOptions, key+" ");
		}
		cbIndex++;
		return state;
    }
    
    // Returns true if s2 is in s1 and not in a bracketed literal (e.g., "[literal]")
    boolean isMatch(String s1, String s2) {
    	if (s1.startsWith(s2))
    		return true;
    	s2 = " " + s2;
    	int len1 = s1.length();
    	int len2 = s2.length();
    	boolean match, inLiteral=false;
    	char c;
    	for (int i=0; i<len1-len2+1; i++) {
    		c = s1.charAt(i);
     		if (inLiteral && c==']')
    			inLiteral = false;
    		else if (c=='[')
    			inLiteral = true;
    		if (c!=s2.charAt(0) || inLiteral || (i>1&&s1.charAt(i-1)=='='))
    			continue;
    		match = true;
			for (int j=0; j<len2; j++) {
				if (s2.charAt(j)!=s1.charAt(i+j))
					{match=false; break;}
			}
			if (match) return true;
    	}
    	return false;
    }
    
  	/** Returns the selected item in the next popup menu. */
    public String getNextChoice() {
		if (choice==null)
			return "";
		Choice thisChoice = (Choice)(choice.elementAt(choiceIndex));
		String item = thisChoice.getSelectedItem();
		if (macro) {
			String label = (String)labels.get((Object)thisChoice);
			item = Macro.getValue(macroOptions, label, item);
			if (item!=null && item.startsWith("&")) // value is macro variable
				item = getChoiceVariable(item);
		}	
		if (recorderOn)
			recordOption(thisChoice, item);
		choiceIndex++;
		return item;
    }
    
  	/** Returns the index of the selected item in the next popup menu. */
    public int getNextChoiceIndex() {
		if (choice==null)
			return -1;
		Choice thisChoice = (Choice)(choice.elementAt(choiceIndex));
		int index = thisChoice.getSelectedIndex();
		if (macro) {
			String label = (String)labels.get((Object)thisChoice);
			String oldItem = thisChoice.getSelectedItem();
			int oldIndex = thisChoice.getSelectedIndex();
			String item = Macro.getValue(macroOptions, label, oldItem);
			if (item!=null && item.startsWith("&")) // value is macro variable
				item = getChoiceVariable(item);
			thisChoice.select(item);
			index = thisChoice.getSelectedIndex();
			if (index==oldIndex && !item.equals(oldItem)) {
				// is value a macro variable?
				Interpreter interp = Interpreter.getInstance();
				String s = interp!=null?interp.getStringVariable(item):null;
				if (s==null)
					IJ.error(getTitle(), "\""+item+"\" is not a valid choice for \""+label+"\"");
				else
					item = s;
			}
		}	
		if (recorderOn) {
			int defaultIndex = ((Integer)(defaultChoiceIndexes.elementAt(choiceIndex))).intValue();
			if (!(smartRecording&&index==defaultIndex)) {
				String item = thisChoice.getSelectedItem();
				if (!(item.equals("*None*")&&getTitle().equals("Merge Channels")))
					recordOption(thisChoice, thisChoice.getSelectedItem());
			}
		}
		choiceIndex++;
		return index;
    }
    
  	/** Returns the selected item in the next radio button group. */
    public String getNextRadioButton() {
		if (radioButtonGroups==null)
			return null;
		CheckboxGroup cg = (CheckboxGroup)(radioButtonGroups.elementAt(radioButtonIndex));
		radioButtonIndex++;
		Checkbox checkbox = cg.getSelectedCheckbox();
		String item = "null";
		if (checkbox!=null)
			item = checkbox.getLabel();
		if (macro) {
			String label = (String)labels.get((Object)cg);
			item = Macro.getValue(macroOptions, label, item);
		}	
		if (recorderOn)
			recordOption(cg, item);
		return item;
    }

    private String getChoiceVariable(String item) {
		item = item.substring(1);
		Interpreter interp = Interpreter.getInstance();
		String s = interp!=null?interp.getStringVariable(item):null;
		if (s==null) {
			double value = interp!=null?interp.getVariable2(item):Double.NaN;
			if (!Double.isNaN(value)) {
				if ((int)value==value)
					s = ""+(int)value;
				else
					s = ""+value;
			}
		}
		if (s!=null)
			item = s;
		return item;
	}
    
  	/** Returns the contents of the next textarea. */
	public String getNextText() {
		String text;
		if (textAreaIndex==0 && textArea1!=null) {
			//textArea1.selectAll();
			text = textArea1.getText();
			textAreaIndex++;
			if (macro)
				text = Macro.getValue(macroOptions, "text1", text);
			if (recorderOn) {
				String text2 = text;
				String cmd = Recorder.getCommand();
				if (cmd!=null && cmd.equals("Convolve...")) {
					text2 = text.replaceAll("\n","\\\\n");
					if (!text.endsWith("\n")) text2 = text2 + "\\n";
				} else
					text2 = text.replace('\n',' ');
				Recorder.recordOption("text1", text2);
			}
		} else if (textAreaIndex==1 && textArea2!=null) {
			textArea2.selectAll();
			text = textArea2.getText();
			textAreaIndex++;
			if (macro)
				text = Macro.getValue(macroOptions, "text2", text);
			if (recorderOn)
				Recorder.recordOption("text2", text.replace('\n',' '));
		} else
			text = null;
		return text;
	}

	/** Displays this dialog box. */
	public void showDialog() {
		if (macro) {
			dispose();
			recorderOn = Recorder.record && Recorder.recordInMacros;
		} else {
			if (pfr!=null) // prepare preview (not in macro mode): tell the PlugInFilterRunner to listen
			pfr.setDialog(this);
			Panel buttons = new Panel();
			buttons.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
			cancel = new Button(cancelLabel);
			cancel.addActionListener(this);
			cancel.addKeyListener(this);
			if (yesNoCancel) {
				okLabel = yesLabel;
				no = new Button(noLabel);
				no.addActionListener(this);
				no.addKeyListener(this);
			}
			okay = new Button(okLabel);
			okay.addActionListener(this);
			okay.addKeyListener(this);
			boolean addHelp = helpURL!=null;
			if (addHelp) {
				help = new Button(helpLabel);
				help.addActionListener(this);
				help.addKeyListener(this);
			}
			if (IJ.isMacintosh()) {
				if (addHelp) buttons.add(help);
				if (yesNoCancel) buttons.add(no);
				if (!hideCancelButton) buttons.add(cancel);
				buttons.add(okay);
			} else {
				buttons.add(okay);
				if (yesNoCancel) buttons.add(no);;
				if (!hideCancelButton)
					buttons.add(cancel);
				if (addHelp) buttons.add(help);
			}
			c.gridx = 0; c.gridy = y;
			c.anchor = GridBagConstraints.EAST;
			c.gridwidth = 2;
			c.insets = new Insets(15, 0, 0, 0);
			grid.setConstraints(buttons, c);
			add(buttons);
			if (IJ.isMacintosh())
				setResizable(false);
			if (IJ.isMacOSX()&&IJ.isJava18())
				instance = this;
			pack();
			setup();
			if (centerDialog) GUI.center(this);
			setVisible(true);
			recorderOn = Recorder.record;
			IJ.wait(50);
		}
		/* For plugins that read their input only via dialogItemChanged, call it at least once */
		if (!wasCanceled && dialogListeners!=null && dialogListeners.size()>0) {
			resetCounters();
			((DialogListener)dialogListeners.elementAt(0)).dialogItemChanged(this,null);
			recorderOn = false;
		}
		resetCounters();
	}
	
    /** Reset the counters before reading the dialog parameters */
	private void resetCounters() {
		nfIndex = 0;        // prepare for readout
		sfIndex = 0;
		cbIndex = 0;
		choiceIndex = 0;
		textAreaIndex = 0;
		radioButtonIndex = 0;
		invalidNumber = false;
	}

	/** Returns the Vector containing the numeric TextFields. */
  	public Vector getNumericFields() {
  		return numberField;
  	}
    
  	/** Returns the Vector containing the string TextFields. */
  	public Vector getStringFields() {
  		return stringField;
  	}

  	/** Returns the Vector containing the Checkboxes. */
  	public Vector getCheckboxes() {
  		return checkbox;
  	}

  	/** Returns the Vector containing the Choices. */
  	public Vector getChoices() {
  		return choice;
  	}

  	/** Returns the Vector containing the sliders (Scrollbars). */
  	public Vector getSliders() {
  		return slider;
  	}

  	/** Returns the Vector that contains the RadioButtonGroups. */
  	public Vector getRadioButtonGroups() {
  		return radioButtonGroups;
  	}

  	/** Returns a reference to textArea1. */
  	public TextArea getTextArea1() {
  		return textArea1;
  	}

  	/** Returns a reference to textArea2. */
  	public TextArea getTextArea2() {
  		return textArea2;
  	}
  	
  	/** Returns a reference to the Label or MultiLineLabel created by the
  		last addMessage() call, or null if addMessage() was not called. */
  	public Component getMessage() {
  		return theLabel;
  	}

    /** Returns a reference to the Preview checkbox. */
    public Checkbox getPreviewCheckbox() {
        return previewCheckbox;
    }
    
    /** Returns 'true' if this dialog has a "Preview" checkbox and it is enabled. */
    public boolean isPreviewActive() {
        return previewCheckbox!=null && previewCheckbox.getState();
    }

	/** Returns references to the "OK" ("Yes"), "Cancel", 
		and if present, "No" buttons as an array. */
	public Button[] getButtons() {
  		Button[] buttons = new Button[3];
  		buttons[0] = okay;
  		buttons[1] = cancel;
  		buttons[2] = no;
		return buttons;
  	}

    /** Used by PlugInFilterRunner to provide visable feedback whether preview
    	is running or not by switching from "Preview" to "wait..."
     */
    public void previewRunning(boolean isRunning) {
        if (previewCheckbox!=null) {
            previewCheckbox.setLabel(isRunning ? previewRunning : previewLabel);
            if (IJ.isMacOSX()) repaint();   //workaround OSX 10.4 refresh bug
        }
    }
    
    /** Display dialog centered on the primary screen. */
    public void centerDialog(boolean b) {
    	centerDialog = b;
    }

    /* Display the dialog at the specified location. */
    public void setLocation(int x, int y) {
    	super.setLocation(x, y);
    	centerDialog = false;
    }
    
    public void setDefaultString(int index, String str) {
    	if (defaultStrings!=null && index>=0 && index<defaultStrings.size())
    		defaultStrings.set(index, str);
    }

    protected void setup() {
	}

	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source==okay || source==cancel | source==no) {
			wasCanceled = source==cancel;
			wasOKed = source==okay;
			dispose();
		} else if (source==help) {
			if (hideCancelButton) {
				if (helpURL!=null && helpURL.equals("")) {
            		notifyListeners(e);
            		return;
				} else {
					wasOKed = true;
					dispose();
				}
			}
			showHelp();
		} else
            notifyListeners(e);
	}
	
	public void textValueChanged(TextEvent e) {
        notifyListeners(e); 
		if (slider==null) return;
		Object source = e.getSource();
		for (int i=0; i<slider.size(); i++) {
			int index = ((Integer)sliderIndexes.get(i)).intValue();
			if (source==numberField.elementAt(index)) {
				TextField tf = (TextField)numberField.elementAt(index);
				double value = Tools.parseDouble(tf.getText());
				if (!Double.isNaN(value)) {
					Scrollbar sb = (Scrollbar)slider.elementAt(i);
					double scale = ((Double)sliderScales.get(i)).doubleValue();
					sb.setValue((int)(value*scale));
				}	
			}
		}
	}

	public void itemStateChanged(ItemEvent e) {
        notifyListeners(e); 
	}

	public void focusGained(FocusEvent e) {
		Component c = e.getComponent();
		if (c instanceof TextField)
			((TextField)c).selectAll();
	}

	public void focusLost(FocusEvent e) {
		Component c = e.getComponent();
		if (c instanceof TextField)
			((TextField)c).select(0,0);
	}

	public void keyPressed(KeyEvent e) { 
		int keyCode = e.getKeyCode(); 
		IJ.setKeyDown(keyCode); 
		if (keyCode==KeyEvent.VK_ENTER && textArea1==null && okay!=null && okay.isEnabled()) {
			wasOKed = true;
			if (IJ.isMacOSX())
				accessTextFields();
			dispose();
		} else if (keyCode==KeyEvent.VK_ESCAPE) { 
			wasCanceled = true; 
			dispose(); 
			IJ.resetEscape();
		} else if (keyCode==KeyEvent.VK_W && (e.getModifiers()&Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())!=0) { 
			wasCanceled = true; 
			dispose(); 
		} 
	} 
		

	void accessTextFields() {
		if (stringField!=null) {
			for (int i=0; i<stringField.size(); i++)
				((TextField)(stringField.elementAt(i))).getText();
		}
		if (numberField!=null) {
			for (int i=0; i<numberField.size(); i++)
				((TextField)(numberField.elementAt(i))).getText();
		}
	}

	public void keyReleased(KeyEvent e) {
		int keyCode = e.getKeyCode();
		IJ.setKeyUp(keyCode);
		int flags = e.getModifiers();
		boolean control = (flags & KeyEvent.CTRL_MASK) != 0;
		boolean meta = (flags & KeyEvent.META_MASK) != 0;
		boolean shift = (flags & e.SHIFT_MASK) != 0;
		if (keyCode==KeyEvent.VK_G && shift && (control||meta))
			new ScreenGrabber().run(""); 
	}
		
	public void keyTyped(KeyEvent e) {}

	public Insets getInsets() {
    	Insets i= super.getInsets();
    	return new Insets(i.top+10, i.left+10, i.bottom+10, i.right+10);
	}

	public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
		Object source = e.getSource();
		for (int i=0; i<slider.size(); i++) {
			if (source==slider.elementAt(i)) {
				Scrollbar sb = (Scrollbar)source;
				int index = ((Integer)sliderIndexes.get(i)).intValue();
				TextField tf = (TextField)numberField.elementAt(index);
				double scale = ((Double)sliderScales.get(i)).doubleValue();
				int digits = scale==1.0?0:2;
				tf.setText(""+IJ.d2s(sb.getValue()/scale,digits));
			}
		}
	}

    /** Notify any DialogListeners of changes having occurred
     *  If a listener returns false, do not call further listeners and disable
     *  the OK button and preview Checkbox (if it exists).
     *  For PlugInFilters, this ensures that the PlugInFilterRunner,
     *  which listens as the last one, is not called if the PlugInFilter has
     *  detected invalid parameters. Thus, unnecessary calling the run(ip) method
     *  of the PlugInFilter for preview is avoided in that case.
     */
    private void notifyListeners(AWTEvent e) {
        if (dialogListeners==null)
        	return;
        boolean everythingOk = true;
        for (int i=0; everythingOk && i<dialogListeners.size(); i++)
            try {
                resetCounters();
                if (!((DialogListener)dialogListeners.elementAt(i)).dialogItemChanged(this, e))
                    everythingOk = false; }         // disable further listeners if false (invalid parameters) returned
            catch (Exception err) {                 // for exceptions, don't cover the input by a window but
                IJ.beep();                          // show them at in the "Log"
                IJ.log("ERROR: "+err+"\nin DialogListener of "+dialogListeners.elementAt(i)+
                "\nat "+(err.getStackTrace()[0])+"\nfrom "+(err.getStackTrace()[1]));  //requires Java 1.4
            }
        boolean workaroundOSXbug = IJ.isMacOSX() && okay!=null && !okay.isEnabled() && everythingOk;
        if (previewCheckbox!=null)
            previewCheckbox.setEnabled(everythingOk);
        if (okay!=null)
            okay.setEnabled(everythingOk);
        if (workaroundOSXbug)
        	repaint(); // OSX 10.4 bug delays update of enabled until the next input
    }

	public void repaint() {
		super.repaint();
		if (imagePanels!=null) {
			for (int i=0; i<imagePanels.size(); i++)
				((ImagePanel)imagePanels.get(i)).repaint();
		}
	}

	public void paint(Graphics g) {
		super.paint(g);
		if (firstPaint) {
			if (numberField!=null && IJ.isMacOSX()) {
				// work around for bug on Intel Macs that caused 1st field to be un-editable
				TextField tf = (TextField)(numberField.elementAt(0));
				tf.setEditable(false);
				tf.setEditable(true);
			}
			if (numberField==null && stringField==null)
				okay.requestFocus();
			firstPaint = false;
		}
	}
    	
    public void windowClosing(WindowEvent e) {
		wasCanceled = true; 
		dispose(); 
    }
    
    /** Adds a "Help" button that opens the specified URL in the default browser.
    	With v1.46b or later, displays an HTML formatted message if
    	'url' starts with "<html>". There is an example at
    	http://imagej.nih.gov/ij/macros/js/DialogWithHelp.js
    */
    public void addHelp(String url) {
    	helpURL = url;
    }

	void showHelp() {
		if (helpURL.startsWith("<html>"))
			new HTMLDialog(this, "", helpURL);
		else {
			String macro = "run('URL...', 'url="+helpURL+"');";
			new MacroRunner(macro);
		}
	}
	
	protected boolean isMacro() {
		return macro;
	}
    
	public static GenericDialog getInstance() {
		return instance;
	}

	public void dispose() {
		super.dispose();
		instance = null;
	}

    public void windowActivated(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}
    public void windowClosed(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}

}
