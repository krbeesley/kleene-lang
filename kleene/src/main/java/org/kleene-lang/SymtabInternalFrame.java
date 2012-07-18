
//	SymtabInternalFrame.java
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

// Extends JInternalFrame, to be added to a JDesktopPane (which in turn
// is the contentPane of a JFrame).  Displays icons for networks and
// functions defined.

import javax.swing.* ;
import javax.swing.event.* ;  
import java.awt.* ;
import java.awt.event.* ;
import java.io.File ;

public class SymtabInternalFrame extends JInternalFrame {

	private PseudoTerminalInternalFrame terminal ;
	private JFileChooser fc ;

	ScrollableFlowPanel iconDisplayPanel ;

    // Constructor
    public SymtabInternalFrame(String title, 
			PseudoTerminalInternalFrame t, 
			JFileChooser filech) {

		super(title,
	      true,    // resizeable
	      false,   // closable
	      true,    // maximizable
	      true) ;  // iconifiable

		terminal = t ;
		fc = filech ;

		// OLD, works
		//Container container = this.getContentPane() ;
		//container.setLayout(new FlowLayout()) ;
		
		// New, when the Symtab window fills up with Buttons,
		// allow scrolling.

		iconDisplayPanel = new ScrollableFlowPanel() ;  // see class definition below
		iconDisplayPanel.setLayout(new FlowLayout(FlowLayout.LEADING)) ;
		iconDisplayPanel.setOpaque(true) ;

		// wrap the iconDisplayPanel in a JScrollPane
		JScrollPane scrollPane = new JScrollPane(iconDisplayPanel) ;
		iconDisplayPanel.setPreferredSize(scrollPane.getPreferredSize()) ;

		//this.getContentPane().setLayout(new BorderLayout()) ;
		//this.getContentPane().add(scrollPane, BorderLayout.CENTER) ;
		setContentPane(scrollPane) ;


		// Now create and set the Menu Bar
	
		setJMenuBar( createSymtabMenuBar() ) ;

		// setSize is done in KleeneGui.java
		// pack??
		pack() ;
		setVisible(true) ;
    }

	public void addIcon(SymtabIconButton sib) {
		iconDisplayPanel.add(sib) ;
	}

	public void removeIcon(SymtabIconButton sib) {
		iconDisplayPanel.remove(sib) ;
	}

	public void validateIconDisplayPanel() {
			iconDisplayPanel.validate() ;
	}


	// workaround extension of JPanel that provides scrolling
	// just wrapping a normal JPanel with FlowLayout in a JScrollPane doesn't work right now.
	// This is a known problem.
	static private class ScrollableFlowPanel extends JPanel implements Scrollable {
		public void setBounds( int x, int y, int width, int height ) {
			super.setBounds( x, y, getParent().getWidth(), height );
		}
 
		public Dimension getPreferredSize() {
			return new Dimension( getWidth(), getPreferredHeight() );
		}
 
		public Dimension getPreferredScrollableViewportSize() {
			return super.getPreferredSize();
		}
 
		public int getScrollableUnitIncrement( Rectangle visibleRect, 
				                               int orientation, 
											   int direction ) {
			int hundredth = ( orientation ==  SwingConstants.VERTICAL
					? getParent().getHeight() : getParent().getWidth() ) / 100;
			return ( hundredth == 0 ? 1 : hundredth ); 
		}
 
		public int getScrollableBlockIncrement( Rectangle visibleRect, 
				                                int orientation, 
												int direction ) {
			return orientation == 
				SwingConstants.VERTICAL ? getParent().getHeight() : getParent().getWidth();
		}
 
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}
 
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
 
		private int getPreferredHeight() {
			int rv = 0;
			for ( int k = 0, count = getComponentCount(); k < count; k++ ) {
				Component comp = getComponent( k );
				Rectangle r = comp.getBounds();
				int height = r.y + r.height;
				if ( height > rv )
					rv = height;
			}
			rv += ( (FlowLayout) getLayout() ).getVgap();
			return rv;
		}
	}

    private JMenuBar createSymtabMenuBar() {
		JMenuBar menuBar = new JMenuBar() ;

		JMenu editMenu = new JMenu("Edit") ;
		JMenuItem selectAllItem = new JMenuItem("Select all") ;
		selectAllItem.setEnabled(false) ;
		selectAllItem.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
			}
		}) ;

		JMenuItem unselectAllItem = new JMenuItem("Unselect all") ;
		unselectAllItem.setEnabled(false) ;

		JMenuItem deleteSelectedItem = new JMenuItem("Delete selected") ;
		deleteSelectedItem.setEnabled(false) ;  
		deleteSelectedItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Delete-selected was selected.") ;
				terminal.processInput("delete selected ;" ) ;
			}
		}) ;

		JMenuItem deleteAllItem = new JMenuItem("Delete all") ;
		deleteAllItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Delete-all was selected.") ;
				terminal.processInput("delete all ;" ) ;
			}
		}) ;


		editMenu.add(selectAllItem) ;
		editMenu.add(unselectAllItem) ;
		editMenu.add(deleteSelectedItem) ;
		editMenu.add(deleteAllItem) ;

		JMenu loadMenu = new JMenu("Load") ;
		JMenuItem loadXmlItem = new JMenuItem("Load XML file...") ;
		loadXmlItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("Load XML file was selected.") ;
				fc.addChoosableFileFilter(new XMLFilter()) ;
				//fc.setAcceptAllFileFilterUsed(false) ;

				int returnVal = fc.showOpenDialog(getParent()) ;
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					// get the File in this case
					File file = fc.getSelectedFile() ;

					// get the whole file path   .getPath()
					String fullpath = file.getPath() ;

					// KRB: replace Windows backslashes with forward slashes
					String correctedFullPath = fullpath.replaceAll("\\\\", "/") ;

					// get the file name   .getName() for the default name of
					// variable, e.g. $foo from $foo.xml
					String filename = file.getName() ;
					if (filename.endsWith(".xml")) {
						filename = filename.substring(0, filename.length() - 4) ;
					}

					String iconName = (String) JOptionPane.showInternalInputDialog(
						getParent(), 
						"Load network as: ", 
						"Set Network Name",
						JOptionPane.QUESTION_MESSAGE,
						null,   // Icon
						null,   // array of selectionValue 
								// (set null to keep user options open)
				    	"$" + filename) ;  // default name
				
					terminal.processInput(iconName + " = " + "$&readXML(\"" + 
							     correctedFullPath + "\") ;") ;

				} 
				//else {
				//	System.out.println("Open command canceled by user.") ;
				//}
			}
		}) ;
		loadXmlItem.setEnabled(true) ;
		loadMenu.add(loadXmlItem) ;
		JMenuItem loadBinaryItem = new JMenuItem("Load binary file...") ;
		loadBinaryItem.setEnabled(false) ;
		loadMenu.add(loadBinaryItem) ;

		JMenu saveMenu = new JMenu("Save") ;
		JMenuItem saveSelectedItem = new JMenuItem("Save selected to file...") ;
		saveSelectedItem.setEnabled(false) ;
		JMenuItem saveAllItem = new JMenuItem("Save all to file...") ;
		saveAllItem.setEnabled(false) ;
		saveMenu.add(saveSelectedItem) ;
		saveMenu.add(saveAllItem) ;
	
		JMenu helpMenu = new JMenu("Help") ;
		JMenuItem aboutItem = new JMenuItem("About") ;
		aboutItem.setEnabled(false) ;
		helpMenu.add(aboutItem) ;

		menuBar.add(editMenu) ;
		menuBar.add(loadMenu) ;
		menuBar.add(saveMenu) ;
		menuBar.add(helpMenu) ;

		return menuBar ;
    }
}

