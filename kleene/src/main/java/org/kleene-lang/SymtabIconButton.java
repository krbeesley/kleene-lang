
//	SymtabIconButton.java
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

//	[short description here]

import javax.swing.JButton ;
import javax.swing.AbstractButton ;
import javax.swing.JPopupMenu ;
import javax.swing.JMenu ;
import javax.swing.JMenuItem ;
import javax.swing.ImageIcon ;
import javax.swing.JFileChooser ; 
import java.io.File ;
import java.awt.event.MouseListener ;
import java.awt.event.MouseAdapter ;
import java.awt.event.MouseEvent ;
import java.awt.event.ActionListener ;
import java.awt.event.ActionEvent ;
import java.awt.Font ; 


class SymtabIconButton extends JButton {
	String iconName ;
	JPopupMenu popup ;
	MouseListener popupListener ;
	PseudoTerminalInternalFrame terminal ;
	SymtabInternalFrame symtab ;
	Environment env ;

	//JFileChooser fcsave ;
	EncodingFileChooser efcWriteDOT ;
	EncodingFileChooser efcWriteXML ;

	// MouseListener is an Interface;
	// MouseAdapter implements MouseListener (empty methods)

	// Constructor
	public SymtabIconButton (String name, String iconFileName, 
			PseudoTerminalInternalFrame term, 
			SymtabInternalFrame sif, 
			//JFileChooser fcs, 
			EncodingFileChooser efcWriteDOT, 
			EncodingFileChooser efcWriteXML, 
			Environment environ) {
		// JButton can be constructed with a String label
		// and a graphic icon (e.g. with iconName "foo.gif")
		super(name, createImageIcon("images/icons/" + iconFileName)) ;

		iconName = name ;
		terminal = term ;
		symtab = sif ;
		//fcsave = fcs ;
		this.efcWriteDOT = efcWriteDOT ;
		this.efcWriteXML = efcWriteXML ;
		env = environ ;
		setVerticalTextPosition(AbstractButton.BOTTOM) ;
		setHorizontalTextPosition(AbstractButton.CENTER) ;

		// popup = new JPopupMenu() ;

		// There are different popup menus for different objects
	    if (name.startsWith("$&&"))
			popup = getSymtabFuncButtonPopupMenu() ;
		else if (name.startsWith("$&"))
			popup = getSymtabFuncButtonPopupMenu() ;
		else if (name.startsWith("$@&"))
			popup = getSymtabFuncButtonPopupMenu() ;
		else if (name.startsWith("$@"))
			popup = getSymtabArrayButtonPopupMenu() ;
		else if (name.startsWith("$>"))
			popup = getSymtabProdButtonPopupMenu() ;
		else if (name.startsWith("$"))
			popup = getSymtabFstButtonPopupMenu() ;

		else if (name.startsWith("&"))			// void function
			popup = getSymtabFuncButtonPopupMenu() ;

		else if (name.startsWith("#&&"))
			popup = getSymtabFuncButtonPopupMenu() ;
		else if (name.startsWith("#&"))
			popup = getSymtabFuncButtonPopupMenu() ;
		else if (name.startsWith("#@&"))
			popup = getSymtabFuncButtonPopupMenu() ;
		else if (name.startsWith("#@"))
			popup = getSymtabArrayButtonPopupMenu() ;
		else if (name.startsWith("#"))
			popup = getSymtabNumButtonPopupMenu() ;

		popupListener = new PopupListener(popup) ;

		// add mouse listener for this component, so that a JPopupMenu
		// can be associated with each instance
		addMouseListener(popupListener) ;

	}

	private static ImageIcon createImageIcon(String path) {
		java.net.URL imgURL = SymtabIconButton.class.getResource(path) ;
		if (imgURL != null) {
			return new ImageIcon(imgURL) ;
		} else {
			// or throw Exception?
			System.err.println("Couldn't find file: " + path) ;
			return null ;
		}
	}

	// return popup menu suitable for Fst, e.g. $foo
	private JPopupMenu getSymtabFstButtonPopupMenu() {
		JPopupMenu pm = new JPopupMenu() ;

		// If you use a common actionPerformed(),
		// invertItem.setActionCommand("invert") ;
		//   sets the name actionPerformed will get with
		//   e.getActionCommand()

		// Here use anonymous instance of ActionListener, avoids need
		// for lots of if ("Delete".equals(e.getActionCommand))
		// checking in a common actionPerformed.  It also keeps the
		// action to be performed visually close to the item, for
		// easier debugging.
		//
		JMenuItem drawItem = new JMenuItem("draw") ;
		drawItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput("draw " + iconName + " ;" ) ;
			}
		}) ;
		pm.add(drawItem) ;

		JMenuItem infoItem = new JMenuItem("info") ;
		infoItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput("info " + iconName + " ;" ) ;
			}
		}) ;
		// infoItem.setEnabled(false) ;
		pm.add(infoItem) ;

		JMenuItem sigmaItem = new JMenuItem("sigma") ;
		sigmaItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput("sigma " + iconName + " ;" ) ;
			}
		}) ;
		pm.add(sigmaItem) ;

		JMenuItem testItem = new JMenuItem("test") ;
		testItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput("test " + iconName + ", \"" + iconName + "\" ;" ) ;
			}
		}) ;
		pm.add(testItem) ;  


		pm.addSeparator() ;

		JMenuItem writeDotItem = new JMenuItem("writeDOT ...") ;
		writeDotItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//fcsave.setSelectedFile(new File(fcsave.getCurrentDirectory().getPath() 
				//		+ "/" + iconName.substring(1) + ".dot")) ; 
				efcWriteDOT.setSelectedFile(new File(efcWriteDOT.getCurrentDirectory().getPath() 
						+ "/" + iconName.substring(1) + ".dot")) ; 
				//int returnVal = fcsave.showSaveDialog(symtab) ;
				int returnVal = efcWriteDOT.showSaveDialog(symtab) ;
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					//String pathName = fcsave.getSelectedFile().getPath() ;
					String pathName = efcWriteDOT.getSelectedFile().getPath() ;
					// On Windows systems, the pathName will typically contain backslashes.
					// Replace them with forward slashes.
					String correctedPathName = pathName.replaceAll("\\\\", "/") ;

					String encoding = efcWriteDOT.getSelectedEncoding() ;

					terminal.processInput("writeDOT " + 
											iconName + 
											", " +
											"\"" + correctedPathName + "\"" +
											", " +
											"\"" + encoding + "\" ;"
											) ; 
				} 
				//else {
				//	System.out.println("Open command cancelled by user\n") ;
				//}
			}
		}) ;
		writeDotItem.setEnabled(true) ;
		pm.add(writeDotItem) ;

		JMenuItem writeXMLItem = new JMenuItem("writeXML ...") ;
		writeXMLItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//fcsave.setSelectedFile(new File(fcsave.getCurrentDirectory().getPath() + "/" + iconName.substring(1) + ".xml")) ; 
				efcWriteXML.setSelectedFile(new File(efcWriteXML.getCurrentDirectory().getPath() + "/" + iconName.substring(1) + ".xml")) ; 
				//int returnVal = fcsave.showSaveDialog(symtab) ;
				int returnVal = efcWriteXML.showSaveDialog(symtab) ;
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					//String pathName = fcsave.getSelectedFile().getPath() ;
					String pathName = efcWriteXML.getSelectedFile().getPath() ;
					// On Windows systems, the pathName will typically contain backslashes.
					// Replace them with forward slashes.
					String correctedPathName = pathName.replaceAll("\\\\", "/") ;

					String encoding = efcWriteXML.getSelectedEncoding() ;

					terminal.processInput("writeXML " + 
											iconName + 
											", " +
											"\"" + correctedPathName + "\"" +
											", " +
											"\"" + encoding + "\" ;"
											) ; 
				} 
				//else {
				//	System.out.println("Open command cancelled by user\n") ;
				//}
			}
		}) ;
		//writeXMLItem.setEnabled(true) ;
		pm.add(writeXMLItem) ;

		JMenuItem writeBinaryItem = new JMenuItem("writeBinary ...") ;
		writeBinaryItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("writeBinary selected") ;
			}
		}) ;
		writeBinaryItem.setEnabled(false) ;
		pm.add(writeBinaryItem) ;

		pm.addSeparator() ;

		JMenuItem invertItem = new JMenuItem("invert") ;
		invertItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput(iconName + " = " + "$&invert!(" + iconName + ") ;") ;
			}
		}) ;
		pm.add(invertItem) ;

		JMenuItem reverseItem = new JMenuItem("reverse") ;
		reverseItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput(iconName + " = " + "$&reverse(" + iconName + ") ;") ;
			}
		}) ;
		pm.add(reverseItem) ;

		JMenuItem inputProjItem = new JMenuItem("inputProjection (upperside)") ;
		inputProjItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput(iconName + " = " + "$&inputProject!(" + iconName + ") ;") ;
			}
		}) ;
		pm.add(inputProjItem) ;

		JMenuItem outputProjItem = new JMenuItem("outputProjection (lowerside)") ;
		outputProjItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput(iconName + " = " + "$&outputProject!(" + iconName + ") ;") ;
			}
		});
		pm.add(outputProjItem) ;

		JMenuItem rmWeightItem = new JMenuItem("rmWeight") ;
		rmWeightItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput(iconName + " = " + "$&rmWeight!(" + iconName + ") ;") ;
			}
		});
		pm.add(rmWeightItem) ;

		JMenuItem closeSigmaItem = new JMenuItem("closeSigma") ;
		closeSigmaItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput(iconName + " = " + "$&closeSigma!(" + iconName + ", \"\") ;") ;
			}
		});
		pm.add(closeSigmaItem) ;

		//pm.addSeparator() ;

		JMenu submenu = new JMenu("Experts only") ;

		JMenuItem optimizeItem = new JMenuItem("optimize") ;
		optimizeItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput("optimize " + iconName + " ;") ;
			}
		}) ;
		submenu.add(optimizeItem) ;
		// optimizeItem.setEnabled(false) ;
		
		pm.addSeparator() ;

		JMenuItem determinizeItem = new JMenuItem("determinize") ;
		determinizeItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput("determinize " + iconName + " ;") ;
			}
		}) ;
		submenu.add(determinizeItem) ;
		// determinizeItem.setEnabled(false) ;

		JMenuItem minimizeItem = new JMenuItem("minimize") ;
		minimizeItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput("minimize " + iconName + " ;") ;
			}
		}) ;
		submenu.add(minimizeItem) ;
		// minimizeItem.setEnabled(false) ;

		JMenuItem rmEpsilonItem = new JMenuItem("rmEpsilon") ;
		rmEpsilonItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput("rmEpsilon " + iconName + " ;") ;
			}
		}) ;
		submenu.add(rmEpsilonItem) ;

		pm.add(submenu) ; 

		pm.addSeparator() ;

		JMenuItem deleteItem = new JMenuItem("delete") ;
		deleteItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput("delete " + iconName + " ;" ) ;
			}
		}) ;
		pm.add(deleteItem) ;

		return pm ;
	}

	// return popup menu suitable for Production, e.g. $>root
	private JPopupMenu getSymtabProdButtonPopupMenu() {
		JPopupMenu pm = new JPopupMenu() ;

		JMenuItem infoItem = new JMenuItem("info") ;
		infoItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// debug
				System.out.println("info selected") ;
			}
		}) ;
		infoItem.setEnabled(false) ;
		pm.add(infoItem) ;

		pm.addSeparator() ;

		JMenuItem deleteItem = new JMenuItem("delete") ;
		deleteItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput("delete " + iconName + " ;" ) ;
			}
		}) ;
		pm.add(deleteItem) ;

		return pm ;
	}

	// return popup menu suitable for Num, e.g. #sum
	private JPopupMenu getSymtabNumButtonPopupMenu() {
		JPopupMenu pm = new JPopupMenu() ;

		JMenuItem infoItem = new JMenuItem("info") ;
		infoItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput("info " + iconName + " ;") ;
			}
		}) ;
		pm.add(infoItem) ;

		pm.addSeparator() ;

		JMenuItem deleteItem = new JMenuItem("delete") ;
		deleteItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput("delete " + iconName + " ;" ) ;
			}
		}) ;
		pm.add(deleteItem) ;

		return pm ;
	}

	// return popup menu suitable for a function, e.g. $&sum
	private JPopupMenu getSymtabFuncButtonPopupMenu() {
		JPopupMenu pm = new JPopupMenu() ;

		JMenuItem infoItem = new JMenuItem("info") ;
		infoItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("info selected") ;
			}
		}) ;
		infoItem.setEnabled(false) ;
		pm.add(infoItem) ;

		pm.addSeparator() ;

		JMenuItem deleteItem = new JMenuItem("delete") ;
		deleteItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput("delete " + iconName + " ;" ) ;
			}
		}) ;
		pm.add(deleteItem) ;

		return pm ;
	}

	private JPopupMenu getSymtabArrayButtonPopupMenu() {
		JPopupMenu pm = new JPopupMenu() ;

		JMenuItem infoItem = new JMenuItem("info") ;
		infoItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput("info " + iconName + " ;") ;  
			}
		}) ;
		pm.add(infoItem) ;

		pm.addSeparator() ;

		JMenuItem deleteItem = new JMenuItem("delete") ;
		deleteItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				terminal.processInput("delete " + iconName + " ;" ) ;
			}
		}) ;
		pm.add(deleteItem) ;

		return pm ;
	}


	// MouseListener is an Interface
	// MouseAdapter implements MouseListener, with empty methods for
	// .mouseClicked(MouseEvent e)    // click and release
	// .mouseEntered(MouseEvent e)
	// .mouseExited(MouseEvent e)
	// .mousePressed(MouseEvent e)  // on initial press
	// .mouseReleased(MouseEvent e) // on release
	// so just override what you need

	class PopupListener extends MouseAdapter {
		JPopupMenu popup ;
		Font prevFont = null ;
		Font italFont = new Font("sansserif", Font.ITALIC, 11) ;

		// Constructor
		public PopupListener(JPopupMenu pm) {
			popup = pm ;
		}
	
		// override two (empty) methods from MouseAdapter;
		// The mouse action needed to launch a popup menu
		// varies from one platform to another.  On OS X,
		// a popup menu should appear when you press the right
		// button; different for Windows (when you release the
		// right button?)
		// boolean .isPopupTrigger() is a provided test that
		//   allows this code to be platform-independent

		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e) ;
		}
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e) ;
		}

		// when the mouse enters a symtab icon, turn the font to italic
		public void mouseEntered(MouseEvent e) {
			prevFont = e.getComponent().getFont() ;
			e.getComponent().setFont(italFont) ;
			//e.getComponent().setColor() ;
		}

		// when the mouse leaves a symtab icon, turn the font back to
		// the original
		public void mouseExited(MouseEvent e) {
			e.getComponent().setFont(prevFont) ;
			//e.getComponent().setColor() ;
		}

		public void mouseClicked(MouseEvent e) {
			// check for double-click
			if (e.getClickCount() == 2) {
				terminal.processInput("draw " + iconName + " ;") ; 
			}
		}

		private void maybeShowPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				popup.show(e.getComponent(), e.getX(), e.getY()) ;
			}
		}
	}
}
 

