
//	EncodingFileChooser.java
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
//   based on an example by Natasha Lloyd

//	JFileChooser with the added option of selecting the encoding to use
//	for a file.

import java.io.UnsupportedEncodingException ;
import java.io.File ;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException ;
import java.nio.charset.UnsupportedCharsetException ;

import javax.swing.BorderFactory ; 

import java.util.ArrayList;
import java.util.Collection ; 

import javax.swing.JFileChooser;

public class EncodingFileChooser extends JFileChooser {

	private EncodingAccessory encodingAccessory ;

	private Collection<String> allEncodings ;

	String systemDefaultEncodingFixed() {
		String sysDefEncoding = System.getProperty("file.encoding") ;
		// on Windows, System.getProperty("file.encoding") usually returns
		// "Cp1252", which is not in allEncodings (while
		// "windows-1252" _is_ included), and similarly for other
		// "Cp" names.
		if (sysDefEncoding.startsWith("Cp")) {
			return "windows-" + sysDefEncoding.substring(2) ;
		} else {
			return sysDefEncoding ;
		}
	}
	String declaredEncodingFixed(String sysDefEncoding) {
		// on Windows, System.getProperty("file.encoding") usually returns
		// "Cp1252", which is not in allEncodings (while
		// "windows-1252" _is_ included), and similarly for other
		// "Cp" names.
		if (sysDefEncoding.startsWith("Cp")) {
			return "windows-" + sysDefEncoding.substring(2) ;
		} else {
			return sysDefEncoding ;
		}
	}
	String[] systemEncodings() {
		allEncodings = (Collection<String>) Charset.availableCharsets().keySet() ;

		// the .toArray() method of Collections, including ArrayLists,
		// can take an array arg that specifies
		// 	the runtime type of the returned array
		String[] encArray = {"a"} ; 	// dummy String array
		return allEncodings.toArray(encArray) ;
	}

	String[] allowedEncodings(ArrayList<String> encodings) {
		String[] encArray = {"a"} ;
		return encodings.toArray(encArray) ;
	}


	// Constructor constructor
	//
	// called from KleeneGUI
	public EncodingFileChooser(File file, String initEncoding, String northLabel, String southLabel, ArrayList<String> encodings) {
		super(file);
		allEncodings = new ArrayList<String>() ;
		allEncodings.addAll(encodings) ;
		setEncodingAccessory(allowedEncodings(encodings), 
							declaredEncodingFixed(initEncoding), northLabel, southLabel) ;
	}

	// called from PseudoTerminalInternalFrame, KleeneGUI
	public EncodingFileChooser(File file, String initEncoding, String northLabel, String southLabel) {
		super(file) ;
		setEncodingAccessory(systemEncodings(), 
							declaredEncodingFixed(initEncoding), northLabel, southLabel) ;
	}

	public EncodingFileChooser(File file) {
		super(file) ;
		setEncodingAccessory(systemEncodings(), systemDefaultEncodingFixed(), "", "") ;
	}
	/* end of Constructors */


	private void setEncodingAccessory(String[] encs, String defaultEncoding, String northLabel, String southLabel) {
		encodingAccessory = new EncodingAccessory(encs, defaultEncoding, northLabel, southLabel) ;
		setAccessory(encodingAccessory) ;

		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)) ;
	}

	
	/**
	 * Determines if an encoding with name <code>name</code> is a valid 
	 * selection for this file chooser.
	 * @param name A <code>Charset</code> name
	 * @return True if the encoding is valid.
	 */
	private boolean isValidEncoding(String name) {
		if (allEncodings.contains(name)) {
			return true;
		}
		// Try all the aliases, too
		try {
			Charset cs = Charset.forName(name);
			for (String csName : cs.aliases()) {
				if (allEncodings.contains(csName)) {
					return true;
				}
			}
		} catch(UnsupportedCharsetException e) {
		} catch(IllegalCharsetNameException e) {
		}
		return false;
	}
	
	/**
	 * @return The encoding selected by the user or <code>null</code> if 
	 * nothing was selected.
	 */
	public String getSelectedEncoding() {
		String selectedEncoding = ((EncodingAccessory)getAccessory()).getSelectedEncoding() ;
		if (isValidEncoding(selectedEncoding)) {
			return selectedEncoding ;
		} else {
			throw new IllegalArgumentException("Invalid encoding specified: " + selectedEncoding + ".") ;
		}
	}
}
