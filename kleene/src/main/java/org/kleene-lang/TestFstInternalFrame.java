
//	TestFstInternalFrame.java
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

import javax.swing.JInternalFrame ;
import javax.swing.JMenu ;
import javax.swing.JMenuItem ;
import javax.swing.JMenuBar ;
import javax.swing.JTextField ;
import javax.swing.JLabel ;
import javax.swing.border.Border ; 
import javax.swing.BorderFactory ;
import javax.swing.Box ;
import javax.swing.BoxLayout ;
import javax.swing.JPanel ; 
import javax.swing.JOptionPane ;

import javax.swing.border.EtchedBorder ;

import java.awt.Container ;
import java.awt.Dimension ;
import java.awt.Color ;
import java.awt.Toolkit ; 

import java.awt.event.ActionListener ;
import java.awt.event.ActionEvent ; 

import com.ibm.icu.text.Transliterator ; 
import com.ibm.icu.text.UCharacterIterator ;

public class TestFstInternalFrame extends JInternalFrame {

	// This InternalFrame appears when you 'test' a network 
	//    inside the Kleene GUI.  It allows you to type in
	//    a single string for generation (match on the upper/"input"
	//    side of the network, or for analysis (match on the
	//    lower/"output" side of the network.
	//
	// The typed string goes into the 
	// 		genInputStringField or the
	// 		anaInputStringField
	// as appropriate.
	//
	// E.g. for generation, the user types a string into
	// the genInputStringField, and the automatically
	// calculated tokens of that string are displayed both 
	// as separated string names, in the genTokSymField,
	// and as hex digits, in the genTokHexField	
	JTextField genInputStringField = null ;  // user input typed here
	JTextField genTokSymField = null ;
	JTextField genTokHexField = null ;

	JTextField anaTokHexField = null ;
	JTextField anaTokSymField = null ;
	JTextField anaInputStringField = null ;  // user input typed here

	// handles set from the Constructor
	Environment env = null ;
	Transliterator trInput ;   // used to tokenize input for generation
							   //   of strings matched against the upper/
							   //   "input" side
	Transliterator trOutput ;  // used to tokenize input for analysis
							   //   of strings matched against the lower/
							   //   "output" side
	SymMap symmap ;
	Fst fst ;
	InterpreterKleeneVisitor interp ;
	Object data ;

    // Constructor
    public TestFstInternalFrame(String title, Environment e, 
			Transliterator trIn, Transliterator trOut, SymMap sm, Fst f, 
			InterpreterKleeneVisitor i, Object d) {

		super(title,
	      true,    // resizeable
	      true,    // closable
	      true,    // maximizable
	      true) ;  // iconifiable

		trInput = trIn ;  
		trOutput = trOut ;

		symmap = sm ;
		env = e ;  
		fst = f ;
		interp = i ;
		data = d ;

		// Swing set-up of the InternalFrame

		setJMenuBar( createTestFstMenuBar() ) ;  // see end of this class

		JPanel container = new JPanel() ;
		setContentPane(container) ;
		container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS)) ;
		container.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)) ;

		Border etchedBorder = 
			BorderFactory.createEtchedBorder(EtchedBorder.RAISED) ;

		Dimension labelDim = new Dimension(120, 0) ;

		// Populate the container
	
		JPanel genLabelPanel = new JPanel() ;
		JLabel genLabel = new JLabel("Input for Generation", JLabel.CENTER) ;
		genLabel.setForeground(Color.RED) ;
		genLabelPanel.add(genLabel) ;
		genLabelPanel.setBorder(etchedBorder) ;
		container.add(genLabelPanel) ;

		container.add(Box.createRigidArea(new Dimension(0, 5))) ;

		JPanel genInputStringPanel = new JPanel() ;
		genInputStringPanel.setLayout(new BoxLayout(genInputStringPanel, 
												BoxLayout.LINE_AXIS)) ;
		JLabel genStringLabel = new JLabel("String >>") ;
		genStringLabel.setForeground(Color.RED) ;
		genStringLabel.setPreferredSize(labelDim) ;
		genInputStringField = new JTextField(40) ;
		genInputStringField.setEditable(true) ;
		genInputStringField.setCaretPosition(0) ;
		genInputStringPanel.add(genStringLabel) ;
		genInputStringPanel.add(Box.createHorizontalGlue()) ;
		genInputStringPanel.add(genInputStringField) ;
		container.add(genInputStringPanel) ;

		container.add(Box.createRigidArea(new Dimension(0, 5))) ;

		JPanel genTokSymPanel = new JPanel() ;
		genTokSymPanel.setLayout(new BoxLayout(genTokSymPanel, 
											BoxLayout.LINE_AXIS)) ;
		JLabel genTokSymLabel = new JLabel("Symbols") ;
		genTokSymLabel.setPreferredSize(labelDim) ; 
		genTokSymField = new JTextField(40) ;
		genTokSymField.setEditable(false) ;
		genTokSymPanel.add(genTokSymLabel) ;
		genTokSymPanel.add(Box.createHorizontalGlue()) ;
		genTokSymPanel.add(genTokSymField) ;
		container.add(genTokSymPanel) ;

		container.add(Box.createRigidArea(new Dimension(0, 5))) ;

		JPanel genTokHexPanel = new JPanel() ;
		genTokHexPanel.setLayout(new BoxLayout(genTokHexPanel, 
											BoxLayout.LINE_AXIS)) ;
		JLabel genTokHexLabel = new JLabel("Code point values") ;
		genTokHexLabel.setPreferredSize(labelDim) ;
		genTokHexField = new JTextField(40) ;
		genTokHexField.setEditable(false) ;
		genTokHexPanel.add(genTokHexLabel) ;
		genTokHexPanel.add(Box.createHorizontalGlue()) ;
		genTokHexPanel.add(genTokHexField) ;
		container.add(genTokHexPanel) ;

		container.add(Box.createRigidArea(new Dimension(0, 10))) ;

		JPanel fstLabelPanel = new JPanel() ;
		fstLabelPanel.setOpaque(true) ;
		fstLabelPanel.setBackground(Color.PINK) ;
		JLabel fstLabel = new JLabel("FST", JLabel.CENTER) ;
		fstLabel.setForeground(Color.BLUE) ;
		fstLabelPanel.add(fstLabel) ;
		fstLabelPanel.setBorder(BorderFactory.createMatteBorder(2,2,2,2, 
															Color.BLACK)) ;
		container.add(fstLabelPanel) ;

		container.add(Box.createRigidArea(new Dimension(0, 10))) ;

		JPanel anaTokHexPanel = new JPanel() ;
		anaTokHexPanel.setLayout(new BoxLayout(anaTokHexPanel, 
											BoxLayout.LINE_AXIS)) ;
		JLabel anaTokHexLabel = new JLabel("Code point values") ;
		anaTokHexLabel.setPreferredSize(labelDim) ;
		anaTokHexField = new JTextField(40) ;
		anaTokHexField.setEditable(false) ;
		anaTokHexPanel.add(anaTokHexLabel) ;
		anaTokHexPanel.add(Box.createHorizontalGlue()) ;
		anaTokHexPanel.add(anaTokHexField) ;
		container.add(anaTokHexPanel) ;

		container.add(Box.createRigidArea(new Dimension(0, 5))) ;

		JPanel anaTokSymPanel = new JPanel() ;
		anaTokSymPanel.setLayout(new BoxLayout(anaTokSymPanel, 
											BoxLayout.LINE_AXIS)) ;
		JLabel anaTokSymLabel = new JLabel("Symbols") ;
		anaTokSymLabel.setPreferredSize(labelDim) ;
		anaTokSymField = new JTextField(40) ;
		anaTokSymField.setEditable(false) ;
		anaTokSymPanel.add(anaTokSymLabel) ;
		anaTokSymPanel.add(Box.createHorizontalGlue()) ;
		anaTokSymPanel.add(anaTokSymField) ;
		container.add(anaTokSymPanel) ;

		container.add(Box.createRigidArea(new Dimension(0, 5))) ;

		JPanel anaInputStringPanel = new JPanel() ;
		anaInputStringPanel.setLayout(new BoxLayout(anaInputStringPanel, 
												BoxLayout.LINE_AXIS)) ;
		JLabel anaInputStringLabel = new JLabel("String >>") ;
		anaInputStringLabel.setForeground(Color.RED) ; 
		anaInputStringLabel.setPreferredSize(labelDim) ;
		anaInputStringField = new JTextField(40) ;
		anaInputStringField.setEditable(true) ;
		anaInputStringField.setCaretPosition(0) ;
		anaInputStringPanel.add(anaInputStringLabel) ;
		anaInputStringPanel.add(Box.createHorizontalGlue()) ;
		anaInputStringPanel.add(anaInputStringField) ;
		container.add(anaInputStringPanel) ;

		container.add(Box.createRigidArea(new Dimension(0, 5))) ;

		JPanel anaLabelPanel = new JPanel() ;
		JLabel anaLabel = new JLabel("Input for Analysis", JLabel.CENTER) ;
		anaLabel.setForeground(Color.RED) ;
		anaLabelPanel.add(anaLabel) ;
		anaLabelPanel.setBorder(etchedBorder) ;
		container.add(anaLabelPanel) ;

		anaInputStringField.requestFocus() ;

		// End of Swing set-up

		// when user types in a string for Analysis (look up, 
		// matched on lower side of the Fst)
		// the lower side is the "output" side in OpenFst, but
		// we can still apply the Fst to a string on the "output"
		// side.
		anaInputStringField.addActionListener(new ActionListener() {

			// anaInputStringField is a JTextField, and when the
			// user presses the Enter key, this actionPerformed()
			// method is invoked

			public void actionPerformed(ActionEvent e) {
				
				// finish off any pending compositions 
				// (in a Java Input Method)
				getInputContext().endComposition() ;

				// get the String typed by the user; this is
				// a Java String, and so happens to be UTF-16
				String input = anaInputStringField.getText() ;

				// highlight the user-typed string to facilitate
				// any subsequent input
				anaInputStringField.selectAll() ;

				// trOutput should be defined to reduce each MCS to its
				// code point value, which may occupy one or two 
				// 16-bit code units (Java Strings being UTF-16)
				String cpvstr = trOutput.transliterate(input) ;
				// cpvstr should now be (abstractly) a sequence of 
				// Unicode code point values, including supplementary 
				// code point values used to encode supplementary characters.
				// Kleene multichar symbols are also given code point
				// values in the supplementary area.
				//
				// Get the count of _code points_  (not code units);
				// .codePointCount() added in Java 1.5
				int inputlen = cpvstr.codePointCount(0, cpvstr.length()) ;

				// the point here is to reduce the input string to
				// an array of int, of length inputlen, each element 
				// representing a Unicode code point value

				StringBuilder tokenizedHex = new StringBuilder() ;
				StringBuilder tokenizedSym = new StringBuilder() ;
				int[] intArray = new int[inputlen] ;

				// UCharacterIterator
				// knows how to iterate through the code point values of
				// a Java String (not the 16-bit code units, but the 
				// Unicode code point values!)
				UCharacterIterator iter = 
						UCharacterIterator.getInstance(cpvstr) ;

				int codepoint ;
				int index = 0 ;
				while ((codepoint = iter.nextCodePoint()) 
						!= UCharacterIterator.DONE) {
					// the BMP chars typed by the user might not yet
					// be in the symmap (any multichar symbols should
					// already be in the symmap, else they couldn't
					// have been recognized by the tokenizer)
					if (Character.charCount(codepoint) == 1) {  // if BMP
						symmap.putsym(String.valueOf((char) codepoint));
					}

					tokenizedHex.append(Integer.toHexString(codepoint) + " ") ;
					tokenizedSym.append(symmap.getsym(codepoint) + " ") ;
					intArray[index++] = codepoint ;
				}

				anaTokHexField.setText(tokenizedHex.toString()) ;
				anaTokSymField.setText(tokenizedSym.toString()) ;  

				// intArray is now an array of ints, being the code point 
				// values of all the symbols in the input string; 
				// the native function 
				// oneStringFst takes such an array of code point values and
				// returns a one-string Fst.
				//
				// call back to the Interpreter, which calls a C++
				// function to build a one-path network from the
				// intArray.  And that one-path network is then
				// composed with the fst being tested.
				try {
					interp.testAnalyze(fst, intArray, data) ;
				} catch (RuntimeException re) {
					interp.outputInterpMessage(re.getMessage(), data) ;
					re.printStackTrace() ; 
				}
			}
	    }) ;

		// for when the user types in a string for generation 
		// (matches on the upper side of the Fst)
		// the upper side is the "Input" side for OpenFst
		genInputStringField.addActionListener(new ActionListener() {

			// genInputStringField is a JTextField, and when the
			// user presses the Enter key, this actionPerformed()
			// method is invoked

			public void actionPerformed(ActionEvent e) {
				
				// finish off any pending compositions 
				// (in a Java Input Method)
				getInputContext().endComposition() ;

				// get the String typed by the user; this is
				// a Java String, and so happens to be UTF-16
				String input = genInputStringField.getText() ;

				// highlight the text just typed by the user,
				// to facilitate any subsequent input
				genInputStringField.selectAll() ;

				// trInput should be defined to reduce each MCS to its
				// code point value, which may occupy one or two 
				// 16-bit code units (Java Strings being UTF-16)
				String cpvstr = trInput.transliterate(input) ;
				// cpvstr should now be (abstractly) a sequence of 
				// Unicode code point values, including supplementary 
				// code point values used to encode supplementary characters.
				// Kleene multichar symbols are also given code point
				// values in the supplementary area.
				//
				// Get the count of _code points_  (not code units);
				// .codePointCount() added in Java 1.5
				int inputlen = cpvstr.codePointCount(0, cpvstr.length()) ;

				// the point here is to reduce the input string to
				// an array of int, each element representing a Unicode
				// code point value

				StringBuilder tokenizedHex = new StringBuilder() ;
				StringBuilder tokenizedSym = new StringBuilder() ;
				int[] intArray = new int[inputlen] ;

				// UCharacterIterator
				// knows how to iterate through the code point values of
				// a Java String (not the 16-bit code units, but the 
				// code point values!)
				UCharacterIterator iter = 
						UCharacterIterator.getInstance(cpvstr) ;

				int codepoint ;
				int index = 0 ;
				while ((codepoint = iter.nextCodePoint()) 
						!= UCharacterIterator.DONE) {
					// BMP characters typed by the user might not yet
					// be in the symmap, add them here.  Any
					// multichar symbols should already be in the
					// symmap, else they could not have been
					// recognized by the tokenizer.
					if (Character.charCount(codepoint) == 1) { // if BMP
						symmap.putsym(String.valueOf((char) codepoint)) ;
					}

					tokenizedHex.append(Integer.toHexString(codepoint) + " ") ;
					tokenizedSym.append(symmap.getsym(codepoint) + " ") ;
					intArray[index++] = codepoint ;
				}

				genTokHexField.setText(tokenizedHex.toString()) ;
				genTokSymField.setText(tokenizedSym.toString()) ; 

				// call back to the Interpreter, which calls a C++
				// function to build a one-path network from the
				// intArray.  And that one-path network is then
				// composed with the fst being tested.
				try {
					interp.testGenerate(fst, intArray, data) ;
				} catch (RuntimeException re) {
					interp.outputInterpMessage(re.getMessage(), data) ;
					re.printStackTrace() ; 
				}

			}
	    }) ;

		// Need to set the size, and perhaps the position, in KleeneGui.java
		// by calling pack(), setSize() or setBounds()

		pack() ;

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
		setLocation((int) (screenSize.getWidth() / 2.0), 
				    (int) (screenSize.getHeight() / 3.5)) ;

		setVisible(true) ; 
    }



    private JMenuBar createTestFstMenuBar() {
		JMenuBar menuBar = new JMenuBar() ;

		JMenu optionsMenu = new JMenu("Options") ;
		JMenuItem tokenizeItem = new JMenuItem("Tokenize") ;
		tokenizeItem.setEnabled(false) ;
		tokenizeItem.addActionListener( new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
			}
	    }) ;
		optionsMenu.add(tokenizeItem) ;

		JMenu helpMenu = new JMenu("Help") ;
		JMenuItem aboutTestingItem = new JMenuItem("Testing") ;
		aboutTestingItem.setEnabled(true) ;
		aboutTestingItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showInternalMessageDialog(env.getDesktop(), 
"Testing allows you to \"apply the network to input\", and see the output.\nYou specify the input as either a simple string or as a regular expression.") ;
			}
		});
		helpMenu.add(aboutTestingItem) ;
		JMenuItem aboutSideItem = new JMenuItem("Generation vs. analysis") ;
		aboutSideItem.setEnabled(true) ;
		aboutSideItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showInternalMessageDialog(env.getDesktop(), 
"To generate, enter input on the upper (aka \"input\") side.\nTo analyze, enter input on the lower (aka \"output\") side.") ;
			}
		});
		helpMenu.add(aboutSideItem) ;
		JMenuItem aboutTokenizationItem = new JMenuItem("Input string tokenization") ;
		aboutTokenizationItem.setEnabled(true) ;
		aboutTokenizationItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showInternalMessageDialog(env.getDesktop(), 
"A simple input string will be automatically \"tokenized\" into individual symbols,\nwhich will include any multicharacter symbols in the sigma of the Fst.\nTokenization is deterministic, from beginning to end, preferring the longest matches.") ;
			}
		});
		helpMenu.add(aboutTokenizationItem) ;


		menuBar.add(optionsMenu) ;
		menuBar.add(helpMenu) ;

		return menuBar ;
    }
}

