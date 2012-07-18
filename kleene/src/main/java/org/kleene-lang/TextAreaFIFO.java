
//	TextAreaFIFO.java
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

//  A scrolling text area, used as part of PseudoTerminalInternalFrame	

import javax.swing.JTextArea ;
import javax.swing.SwingUtilities ;
import javax.swing.event.* ;
import javax.swing.text.* ;

public class TextAreaFIFO extends JTextArea implements DocumentListener {

	// This class based on example in 
	// http://forums.sun.com/thread.jspa?threadID=409418

	// An instance of this TextAreaFIFO (the "History Area") appears in 
	// the PseudoTerminal of the Kleene GUI. Input lines typed by the
	// user, or read from a pre-edited script, are .append()-ed to this
	// widget

	// A Swing text widget, like JTextArea, uses a "Document" to represent
	// its content.  Document events occur when he content of a Document
	// changes in any way.

    private int maxLines ;  // maximum number of lines retained in the Document
	private Document document ;
	private Element  root ;

    // Constructor
    public TextAreaFIFO( int lines ) {
		maxLines = lines ;
		document = this.getDocument() ;
		root = document.getDefaultRootElement() ;


		// you attach a document listener to a text component's Document,
		// not to the text widget itself
		document.addDocumentListener( this ) ;
    }

    // Three methods implementing DocumentListener: insertUpdate(),
	// removeUpdate(), changedUpdate()

	// insertUpdate() is called whenever text is inserted into the 
	// listened-to Document--in our case, text is inserted when the
	// .append() method is called.
    public void insertUpdate( DocumentEvent e ) {
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
		    	removeLines() ;  // see below
			}
	    });
    }
	// we don't need these two
    public void removeUpdate( DocumentEvent e ) {}
    public void changedUpdate( DocumentEvent e ) {}
	// end of methods implementing DocumentListener


	// removeLines() function should be invoked whenever .append()
	// is called, inserting text into the Document.  Its job is to
	// throw away the oldest lines in the Document when maxLines
	// is exceeded.
	
    private void removeLines() {
		while ( root.getElementCount() > maxLines ) {
	    	Element firstLine = root.getElement(0) ;

	    	try {
				document.remove( 0, firstLine.getEndOffset() ) ;
	    	} catch ( BadLocationException ble ) {
				System.out.println("Problem found when removing oldest lines from TextAreaFIFO") ;
				ble.printStackTrace() ;
	    	}
		}
		setCaretPosition( document.getLength() ) ;
    }
}
