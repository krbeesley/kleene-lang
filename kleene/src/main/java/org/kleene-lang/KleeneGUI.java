
//	KleeneGUI.java
//
//	The Kleene Programming Language

//   Copyright 2006-2012 SAP AG

//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

//   Author: ken.beesley@sap.com (Kenneth R. Beesley)

//	Main source file for the Kleene GUI

import javax.swing.JComponent ;
import javax.swing.JDesktopPane ;
import javax.swing.JMenu ;
import javax.swing.JMenuItem ;
import javax.swing.JMenuBar ;
import javax.swing.JFrame ;
import javax.swing.ImageIcon ;
import javax.swing.JTextField ;
import javax.swing.JPanel ;
import javax.swing.BorderFactory ;
import javax.swing.JFileChooser ;
import javax.swing.JOptionPane ;  

import java.awt.event.* ;
import java.awt.* ;
import java.net.URL ;
import java.io.File ;
import java.io.InputStream ;
import java.io.InputStreamReader ; 
import java.io.BufferedReader ;
import java.io.IOException ;
import java.io.PipedWriter ;
import java.util.ArrayList ;  
import java.util.Random ;
import java.util.Date ;
import java.util.HashMap ;


public class KleeneGUI extends JFrame implements ActionListener {

    private ImageJDesktopPane desktop = null ;

    private PseudoTerminalInternalFrame terminal = null ;
    private SymtabInternalFrame symtab = null ;

    private String[] backgroundImageList = null ; 	// array of jpg file names in
													// images/backgrounds
    private int backgroundImageListLength ;
    private int backgroundImageListIndex ;
	private Environment env ;

	// fcLoadXML used (and re-used) in potentially-multiple SymtabInternalFrames, 
	// so that the user's preferred save directory is remembered
	private JFileChooser fcLoadXML ;

	private EncodingFileChooser efcWriteDOT ;
	private EncodingFileChooser efcWriteXML ;

	private HashMap<String, SymtabIconButton> symtabContents = null ;

    // Constructor
    public KleeneGUI(Environment environ, PipedWriter pwriter) {
		super("Kleene GUI") ;

		env = environ ;

		// Position the JFrame 50 x 50 pixels from the top left corner
		int inset = 50 ;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
		int guiFrameWidth = screenSize.width - inset*2 ;
		int guiFrameHeight = screenSize.height - inset*2 ;
		// set position and size of the whole KleeneGui (extends JFrame)
		this.setBounds(inset, inset, guiFrameWidth, guiFrameHeight) ;

		// start with user.dir (where Kleene was launched) or user.home?
		fcLoadXML = new JFileChooser(new File(System.getProperty("user.dir"))) ;

		ArrayList<String> supportedDOTencodings = new ArrayList<String>() ;
		supportedDOTencodings.add("UTF-8") ;
		supportedDOTencodings.add("ISO-8859-1") ;

		efcWriteDOT = new EncodingFileChooser(
							new File(System.getProperty("user.dir")),
							"UTF-8",
							"Specify the file encoding.",
							"UTF-8 is recommended.",
							supportedDOTencodings) ;

		// XML parsers are _required_ to handle only UTF-8 and UTF-16
		String[] supportedXMLencodings = { "UTF-8", "UTF-16" } ;

		efcWriteXML = new EncodingFileChooser(
							new File(System.getProperty("user.dir")),
							"UTF-8",
							"Specify the file encoding.",
							"UTF-8 is recommended."
							// , supportedXMLencodings
							) ;

		// this ImageJDesktopPane will be made the contentPane of the
		// KleeneGui (JFrame)
		desktop = new ImageJDesktopPane() ;
		env.setDesktop(desktop) ;

		backgroundImageList = new String[25] ; // max 25 images should be enough

		// get list of image file names in images/backgrounds/
		InputStream is = getClass().getResourceAsStream("images/backgrounds/listing") ;
		BufferedReader br = new BufferedReader(new InputStreamReader(is)) ;
		String str ;
		backgroundImageListIndex = 0 ;
		try {
			while ((str = br.readLine()) != null) {
				backgroundImageList[backgroundImageListIndex++] = str ;
			}
			br.close() ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}

		//backgroundImageListLength = backgroundImageListIndex ;
		//System.out.println("****** Background image count: " + backgroundImageListLength ) ;
		//for (int i = 0; i < backgroundImageListLength ; i++) {
		//    System.out.println(backgroundImageList[i]) ;
		//}
		//System.out.flush() ;

		Random rand = new Random(new Date().getTime()) ;
		backgroundImageListIndex = rand.nextInt(backgroundImageListLength) ;

		// to return Image
		// Image image = Toolkit.getDefaultToolkit().getImage(url) ;
		// but here we need to return an ImageIcon

		// set a border--line is easy and safe
		// desktop.setBorder(BorderFactory.createLineBorder(Color.BLACK)) ;
		// The difference with a Matte border is that you can specify the
		//    width on each side:  top, left, bottom, right
		desktop.setBorder(BorderFactory.createMatteBorder(1, 2, 2, 2, Color.BLACK)) ;
		// set a background image to fill the entire desktop background
		desktop.setImage(getNextBackgroundImageIcon()) ;
		desktop.setFillEntireArea(true) ;

		// Make dragging faster but uglier
		desktop.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE) ;

		// initialize desktop to hold two JInternalFrames

		int borderWidth = 10 ;
		int intFrameSep = 20 ;
		int barHeight = 15 ;

		int intFrameWidth = ((guiFrameWidth - borderWidth*2) - intFrameSep*3) / 2 ;
		int intFrameHeight = (guiFrameHeight - barHeight*3) -  intFrameSep*2 ;

		terminal = new PseudoTerminalInternalFrame("Kleene Terminal", 500, 25, 50, pwriter) ;
		terminal.setBounds(intFrameSep, intFrameSep, intFrameWidth, intFrameHeight) ;
		terminal.setVisible(true) ;

		symtab = new SymtabInternalFrame("Symtab", terminal, fcLoadXML) ;
		symtab.setBounds(intFrameSep + intFrameWidth + intFrameSep, intFrameSep,
				intFrameWidth, intFrameHeight) ;

		// Now add the two JInternalFrames to the GUI
		desktop.add(terminal) ;
		desktop.add(symtab) ;

		symtabContents = new HashMap<String, SymtabIconButton>() ;

		this.setContentPane(desktop) ;
		this.setJMenuBar(createGuiMenuBar()) ;
	
		try {
			terminal.setSelected(true) ;
		} catch (java.beans.PropertyVetoException e) {
		}
	}

	private ImageIcon getNextBackgroundImageIcon() {
		backgroundImageListIndex += 1 ;
		if (backgroundImageListIndex >= backgroundImageListLength) {
			backgroundImageListIndex = 0 ;
		}

		URL fileUrl = getClass().getResource("images/backgrounds/" + 
							 backgroundImageList[backgroundImageListIndex]) ;

		if (fileUrl != null) {
			return new ImageIcon(fileUrl) ;
		} else {
			System.err.println("Could not find image file.  " + backgroundImageListIndex + " : " + backgroundImageList[backgroundImageListIndex]) ;
			return null ;
		}
    }

    private JMenuBar createGuiMenuBar() {
		JMenuBar menuBar = new JMenuBar() ;

		// Set up a (pull-down) menu
		JMenu kleeneMenu = new JMenu("Kleene") ;
		// kleeneMenu.setMnemonic(KeyEvent.VK_T) ;
		//
		JMenuItem aboutItem = new JMenuItem("About Kleene") ;
		aboutItem.setActionCommand("aboutKleene") ;
		aboutItem.addActionListener(this) ;
		//aboutItem.setEnabled(false) ;

		JMenuItem licenseItem = new JMenuItem("License") ;
		licenseItem.setActionCommand("license") ;
		licenseItem.addActionListener(this) ;
		//licenseItem.setEnabled(false) ;

		JMenuItem sourcesItem = new JMenuItem("Source Code") ;
		sourcesItem.setActionCommand("sources") ;
		sourcesItem.addActionListener(this) ;
		//sourcesItem.setEnabled(false) ;
		
		JMenuItem preferencesItem = new JMenuItem("Preferences") ;
		// preferencesItem.setMnemonic(KeyEvent.VK_P) ;
		preferencesItem.setActionCommand("preferences") ;
		preferencesItem.addActionListener(this) ;
		//preferencesItem.setEnabled(false) ;
		

		JMenuItem quitItem = new JMenuItem("Quit") ;
		// quitItem.setMnemonic(KeyEvent.VK_Q) ;
		// When this item is selected, the actionPerformed() method
		// will catch/handle the action
		quitItem.setActionCommand("quit") ;
		quitItem.addActionListener(this) ; // see actionPerformed() method below

		kleeneMenu.add(aboutItem) ;
		kleeneMenu.add(licenseItem) ;
		kleeneMenu.add(sourcesItem) ;
		kleeneMenu.addSeparator() ;
		kleeneMenu.add(preferencesItem) ;
		kleeneMenu.addSeparator() ;
		kleeneMenu.add(quitItem) ;

		// Another pull-down menu
		JMenu windowsMenu = new JMenu("Windows") ;
		// windowsMenu.setMnemonic(KeyEvent.VK_W) ;

		// Now create and add the menu item(s)
		//JMenuItem newTermFrameItem = new JMenuItem("New Terminal frame") ;
		// newTermFrameItem.setMnemonic(KeyEvent.VK_N) ;
		// When this item is selected, the actionPerformed() method
		// will catch/handle the action
		//newTermFrameItem.setActionCommand("newTerminal") ;
		//newTermFrameItem.addActionListener(this) ;
		//newTermFrameItem.setEnabled(false) ;
		// i.e. use the actionPerformed method in this class
		//windowsMenu.add(newTermFrameItem) ;

		// Another pull-down menu
		JMenu backgroundMenu = new JMenu("Background") ;
		JMenuItem newBackgroundItem = new JMenuItem("Change background") ;
		newBackgroundItem.setActionCommand("newBackground") ;
		newBackgroundItem.addActionListener(this) ;
		backgroundMenu.add(newBackgroundItem) ;

		// Another pull-down menu
		JMenu semiringMenu = new JMenu("Semiring") ;

		JMenuItem tropicalItem = new JMenuItem("Tropical Semiring (Default)") ;
		tropicalItem.setActionCommand("tropicalSemiring") ;
		tropicalItem.addActionListener(this) ;
		tropicalItem.setEnabled(false) ;

		JMenuItem logItem = new JMenuItem("Log Semiring") ;
		logItem.setActionCommand("logSemiring") ;
		logItem.addActionListener(this) ;
		logItem.setEnabled(false) ;

		JMenuItem realItem = new JMenuItem("Real Semiring") ;
		realItem.setActionCommand("realSemiring") ;
		realItem.addActionListener(this) ;
		realItem.setEnabled(false) ;

		JMenuItem booleanItem = new JMenuItem("Boolean Semiring") ;
		booleanItem.setActionCommand("booleanSemiring") ;
		booleanItem.addActionListener(this) ;
		booleanItem.setEnabled(false) ;

		semiringMenu.add(tropicalItem) ;
		semiringMenu.add(logItem) ;
		semiringMenu.add(realItem) ;
		semiringMenu.add(booleanItem) ;

		// Another pull-down menu
		JMenu helpMenu = new JMenu("Help") ;

		JMenuItem aboutOpenFstItem = new JMenuItem("About OpenFst") ;
		// aboutItem.setMnemonic(KeyEvent.VK_A) ;
		aboutOpenFstItem.setEnabled(false) ;

		helpMenu.add(aboutOpenFstItem) ;

		// Now add the various pull-down menus to the menu bar,
		// in the order desired.
		menuBar.add(kleeneMenu) ;
		menuBar.add(windowsMenu) ;
		menuBar.add(backgroundMenu) ;
		menuBar.add(semiringMenu) ;
		menuBar.add(helpMenu) ;

		return menuBar ;
    }

    public void actionPerformed(ActionEvent e) {
		String actionCommand = e.getActionCommand() ;
		if ("newTerminal".equals(actionCommand)) {

		} else if ("newBackground".equals(actionCommand)) {
			desktop.setImage(getNextBackgroundImageIcon()) ;
		} else if ("quit".equals(actionCommand)) {
			quit() ;  // see below
		} else if ("aboutKleene".equals(actionCommand)) {
			JOptionPane.showInternalMessageDialog(desktop, 
				"Kleene version " + KleeneGuiConstants.VERSION_NUMBER
                + "\nCopyright \u00a9 " + KleeneGuiConstants.COPYRIGHT_YEAR_RANGE + " SAP AG. "
                + "\nApache License, Version 2.0."
				) ;
		} else if ("license".equals(actionCommand)) {
			JOptionPane.showInternalMessageDialog(desktop, 
					"Kleene is a free and open-source project, \nreleased 4 May 2012 by SAP AG \nunder the Apache License, Version 2.0.\nSee the file LICENSE in the Kleene source release \nor at \nhttp://www.apache.org/licenses/LICENSE-2.0 \nor \nhttp://commons.apache.org/license.html") ;
		} else if ("sources".equals(actionCommand)) {
			JOptionPane.showInternalMessageDialog(desktop,
					"The Kleene source files are available from GitHub:\n$ git clone git://github.com/krbeesley/kleene-lang.git") ;
		} else if ("preferences".equals(actionCommand)) {
			JOptionPane.showInternalInputDialog(desktop, "Enter your name") ;
		}
    }

	public SymtabInternalFrame getSymtabInternalFrame() {
		return symtab ;
	}

    public void appendToHistory(String s) {
		terminal.appendToHistory(s) ;
    }

    protected void quit() {
		System.exit(0) ;
    }

    public JTextField getInputField() {
		return terminal.getInputField() ;
    }

	public PseudoTerminalInternalFrame getTerminal() {
		return terminal ;
	}

	public void addSymtabIconButton(String name, String iconFileName) {
		SymtabIconButton sib = new SymtabIconButton(name, iconFileName, terminal, symtab, efcWriteDOT, efcWriteXML, env) ;
		// remove existing symtab icon (if any)
		removeSymtabIconButton(name) ;  // see just below

		symtabContents.put(name, sib) ;
		// OLD
		//symtab.getContentPane().add(sib) ;
		//
		// NEW with scrolling symtab contentPane
		symtab.addIcon(sib) ;

		// after adding or removing daughters,
		// it seems that (on Linux) you need to
		// validate to get an automatic update
		symtab.validateIconDisplayPanel() ;
		symtab.getContentPane().validate() ;
		symtab.getContentPane().repaint() ;
		symtab.repaint() ;
		// desktop.repaint() ;
	}

	public void removeSymtabIconButton(String name) {
		//System.out.println("***Call to KleeneGUI.removeSymtabIconButton: " + name) ;
		if (symtabContents.containsKey(name)) {
			// need to remove the old one first
			// remove from symtabContents
			SymtabIconButton oldObj = symtabContents.get(name) ;
			symtabContents.remove(name) ; // removes entry with this key
			// now actually remove the SymtabIconButton from the symtab
			// JInternalFrame; .remove() gets forwarded to the
			// ContentPane
			//
			// OLD
			//symtab.getContentPane().remove(oldObj) ;
			//
			// NEW
			symtab.removeIcon(oldObj) ;
			
			// after adding or removing daughters,
			// it seems (on Linux) that you need to
			// validate to get an automatic update
			symtab.validateIconDisplayPanel() ;
			symtab.getContentPane().validate() ;
			symtab.getContentPane().repaint() ;
			symtab.repaint() ;
			// desktop.repaint() ;  
		}
	}

	public void showTestFstInternalFrame(TestFstInternalFrame tfif) {
		desktop.add(tfif) ;
	}
}

