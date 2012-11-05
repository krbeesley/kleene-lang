
//	PseudoTerminalInternalFrame.java
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

// Extends JInternalFrame, to be added to a JDesktopPane (which in turn
// is the contentPane of a JFrame).  Works like a primitive terminal,
// allowing users to type Kleene statements in a JTextField; pressing
// Enter causes the text to be transmitted--initially just to the
// historyArea, which is a TextAreaFIFO which extends JTextArea and does
// the trick of deleting old lines.

import javax.swing.* ;
import java.awt.* ;
import java.awt.event.* ;
import java.awt.im.InputContext ;
import java.io.File ;  
import java.io.Writer ;
import java.io.PipedWriter ;
import java.io.BufferedWriter ;

public class PseudoTerminalInternalFrame extends JInternalFrame {

    private TextAreaFIFO historyArea = null ;
    private JTextField inputField = null ;  // user enters text here
    //protected InputContext inputContext = null ;

    private final String newline = "\n" ;

    private Writer pwrtr ;
    private BufferedWriter bpwriter ;

	//private JFileChooser fcscript = null ; 
	// encoding file chooser for opening a Kleene script
	// 	need to specify the encoding
	private EncodingFileChooser efcscript ;

    // Constructor
    public PseudoTerminalInternalFrame(	String title, 
										int maxRows, 
										int rows, 
										int columns, 
										PipedWriter pwriter) {

		// set features/behavior of the InternalFrame
		super(title,
			  true,    // resizeable
			  false,   // closable
			  true,    // maximizable
			  true) ;  // iconifiable
		
		pwrtr = pwriter ;
		bpwriter = new BufferedWriter(pwrtr) ;

		// user.dir is the directory from which Kleene was launched, vs. user.home
		//fcscript = new JFileChooser(new File(System.getProperty("user.dir"))) ;
		//
		String defaultOSEncoding = System.getProperty("file.encoding") ;
		
		efcscript = new EncodingFileChooser(
				new File(System.getProperty("user.dir")),
				// pre-set the operating system's default encoding
				defaultOSEncoding,
				"Identify the script encoding.",
				"Your system default is " + defaultOSEncoding + ".") ;

		historyArea = new TextAreaFIFO(maxRows) ;
		historyArea.setRows(rows) ;
		historyArea.setColumns(columns) ;
		historyArea.setText("// Enter Kleene statements below, e.g.\n\n// $fst = abc*d+e? ;\n// draw $fst ;\n\n// Followed by RETURN/ENTER\n\n") ;

		historyArea.setEditable(false) ;
		historyArea.setLineWrap(true) ;
		historyArea.setWrapStyleWord(true) ;
		historyArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)) ;
		//historyArea.setFont() ;

		// wrap the historyArea inside a JScrollPane
		JScrollPane scrollPane = new JScrollPane(historyArea) ;
		// wrapping is on, so no Horizontal scroll policy is needed;

		// Choose one of these:
		//scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED) ;
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS) ;

		// interactive user types into the inputField,
		// then presses the 'Enter' key
		inputField = new JTextField(columns) ;
		//inputContext = inputField.getInputContext() ;
		//inputContext = InputContext.getInstance() ;
		//
		//inputField.setFont() ;
		//inputField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)) ;
		inputField.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent event) {
				// Request focus if not already here
				if (!(inputField.hasFocus())) { 
					inputField.requestFocus();
				}
			}
		});


		// in the JTextField, pressing 'enter' generates an ActionEvent
		inputField.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				
				// finish off any pending compositions (in a Java Input Method)
				getInputContext().endComposition() ;

				String input = inputField.getText() ;
				processInput(input) ;

				inputField.setText("") ;
				inputField.setCaretPosition(0) ;

			}
	    }) ;


		Container container = this.getContentPane() ;
		container.setLayout(new BorderLayout()) ;

		JPanel inputPanel = new JPanel(new BorderLayout()) ;
		JLabel inputLabel = new JLabel(">>") ;
		inputPanel.add(inputLabel, BorderLayout.WEST) ;
		inputPanel.add(inputField, BorderLayout.CENTER) ;

		container.add(scrollPane, BorderLayout.CENTER) ;
		container.add(inputPanel, BorderLayout.SOUTH) ;

		setJMenuBar(createTerminalMenuBar()) ;

		// setSize in KleeneGui.java

		// pack??
		pack() ;
		setVisible(true) ;

		addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent event) {
				// Request focus if not already here
				if (!(inputField.hasFocus())) { 
					inputField.requestFocus();
				}
			}
		});

		inputField.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.TEXT_CURSOR)) ;

		inputField.requestFocus() ; 
	}

		// Called from addActionListener above, and
		// from popup menus that generate text commands
	public void processInput(String input) {
		// Normalize the input here ?
		//
		// simple display in the History Area
		historyArea.append(input + "\n") ;
		// and then send it to the parser
		try {
			bpwriter.write(input) ;
			bpwriter.newLine() ;
			bpwriter.flush() ;
		} catch (java.io.IOException exc) {
			System.out.println("Failure to append to pwriter in terminal") ;
			historyArea.append(exc.getMessage()) ;
		}
	}

	private JMenuBar createTerminalMenuBar() {
		JMenuBar menuBar = new JMenuBar() ;

		JMenu editMenu = new JMenu("Edit") ;
		JMenuItem clearItem = new JMenuItem("Clear history") ;
		clearItem.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				clear() ;
			}
		}) ;
		JMenuItem fontItem = new JMenuItem("Select font...") ;
		fontItem.setEnabled(false) ;
		editMenu.add(clearItem) ;
		editMenu.add(fontItem) ;

		JMenu sourceMenu = new JMenu("Source") ;
		JMenuItem scriptItem = new JMenuItem("source (run script)...") ;
		scriptItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			//fcscript.addChoosableFileFilter(new KLFilter()) ;
			//fcscript.setAcceptAllFileFilterUsed(false) ;
			//
			efcscript.addChoosableFileFilter(new KLFilter()) ;

			//int returnVal = fcscript.showOpenDialog(getParent()) ;
			int returnVal = efcscript.showOpenDialog(getParent()) ;
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				//File file = fcscript.getSelectedFile() ;
				File file = efcscript.getSelectedFile() ;
				String enc = efcscript.getSelectedEncoding() ;

				// KRB: try to replace any Windows backslashes with
				// forward slashes
				String correctedPath = file.getPath().replaceAll("\\\\", "/") ;
				System.out.println("Source file selected: " + correctedPath) ;

				processInput("source " +
							"\"" + correctedPath + "\"" +
							", " + 
							"\"" + enc + "\" ; ") ;
			} 
			//else {
			//	System.out.println("Open command canceled by user.") ;
			//}

			}
		}) ;
		scriptItem.setEnabled(true) ;
		sourceMenu.add(scriptItem) ;

		JMenu saveMenu = new JMenu("Save") ;
		JMenuItem saveItem = new JMenuItem("Save history to file...") ;
		saveItem.setEnabled(false) ;
		JMenuItem appendItem = new JMenuItem("Append history to file...") ;
		appendItem.setEnabled(false) ;
		saveMenu.add(saveItem) ;
		saveMenu.add(appendItem) ;
		

		JMenu helpMenu = new JMenu("Help") ;
		JMenuItem aboutItem = new JMenuItem("About") ;
		aboutItem.setEnabled(false) ;

		helpMenu.addSeparator() ;

		JMenu submenu = new JMenu("Memory (experts only)") ;

		JMenuItem memoryItem = new JMenuItem("memory (Java memory report)") ;
		memoryItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				processInput("memory ;") ;
			}
		}) ;
		submenu.add(memoryItem) ;
		// memoryItem.setEnabled(false) ;
		
		JMenuItem cppmemoryItem = new JMenuItem("cppmemory (C++ memory report)") ;
		cppmemoryItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				processInput("cppmemory ;") ;
			}
		}) ;
		submenu.add(cppmemoryItem) ;
		cppmemoryItem.setEnabled(false) ;

		
		JMenuItem gcItem = new JMenuItem("gc (garbage collect)") ;
		gcItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				processInput("gc ;") ;
			}
		}) ;
		submenu.add(gcItem) ;
		// gcItem.setEnabled(false) ;
		
		JMenuItem fstsItem = new JMenuItem("fsts (report allocated - finalized)") ;
		fstsItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				processInput("fsts ;") ;
			}
		}) ;
		submenu.add(fstsItem) ;
		// fstsItem.setEnabled(false) ;
	
		JMenuItem symtabItem = new JMenuItem("symtab (main print items)") ;
		symtabItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				processInput("symtab ;") ;
			}
		}) ;
		submenu.add(symtabItem) ;
		// symtabItem.setEnabled(false) ;

		JMenuItem gsymtabItem = new JMenuItem("gsymtab (global print items)") ;
		gsymtabItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				processInput("gsymtab ;") ;
			}
		}) ;
		submenu.add(gsymtabItem) ;
		// gsymtabItem.setEnabled(false) ;

		helpMenu.add(aboutItem) ;
		helpMenu.add(submenu) ;

		// ** add pull-down menus to the menuBar

		menuBar.add(editMenu) ;
		menuBar.add(sourceMenu) ;
		menuBar.add(saveMenu) ;
		menuBar.add(helpMenu) ;

		return menuBar ;
    }

    public void appendToHistory(String s) {
		historyArea.append(s + newline) ;
    }

    public void clear() {
		historyArea.setText("") ;
    }

    public void append(String str) {
		historyArea.append(str) ;
    }

    public JTextField getInputField() {
		return inputField ;
    }
}
