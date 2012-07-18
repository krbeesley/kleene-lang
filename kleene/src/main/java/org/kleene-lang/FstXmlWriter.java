
//	FstXmlWriter.java
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

// Class that knows how to write XML elements, representing Arcs
// and Multichar symbols, to file.  The Fst will in fact be
// iterated through by a native C++ function, which has access to
// the OpenFst iterators; but this C++ function will get passed an
// object of type FstXmlWriter, and the native function will call
// back, via JNI, to the .writeArcElmt(), .writeFinalElmt()
//
// This may seem roundabout, but it lets Java handle the Unicode
// I/O.  And the symmap (needed to map ints to String) is on the
// Java side.

import java.io.File ;  
import java.io.BufferedWriter ;
import java.io.IOException ;
import java.io.OutputStreamWriter ;
import java.io.FileOutputStream;

import java.util.HashSet ;
import java.util.Iterator ;

// import org.apache.commons.lang.StringEscapeUtils ;
//
// KRB: I used to use StringEscapeUtils, which provides the
// convenient method .escapeXml(String).  However, in version
// 2.4 this method escaped all characters beyond 0x7F, i.e.
// all characters beyond the ASCII range.  This was found
// annoying during testing, and so I wrote my own EscapeXML
// class with method .escapeXML(String) that originally
// handled only the five special XML characters.  There are,
// however, other characters like newlines and tabs that 
// might be needed in XML, and which are probably best escaped.
//
// The StringEscapeUtils.escapeXML(String) method in release
// 3.0 is reputed NOT to escape characters beyond 0x7F.  But
// in Dec 2009 version 3.0 appears to be a "snapshot" only,
// and only 2.4 is available for easy download.
//
// As soon as 3.0 becomes fully available, switch back to
// using StringEscapeUtils.escapeXml(String).

public class FstXmlWriter {

	private String arcType ;
	private HashSet<Integer> sigma ;

	private SymMap symmap ;
	private boolean containsOther ;

	private BufferedWriter out ;
	private String encoding ;

	// Constructor
	public FstXmlWriter(String aType, 
						HashSet<Integer> sig, 
						SymMap sm, 
						boolean cOther, 
						File f,
						String encoding) {
		arcType = aType ;
		sigma = sig ;
		symmap = sm ;
		containsOther = cOther ;
		this.encoding = encoding ;

		// the File should be a full path to a file, constructed
		// appropriately on the calling side; the XML file is written there.

		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), encoding)) ;
		} catch (Exception e) {
			e.printStackTrace() ;
		}
	}

	public void initializeXml(int start, int numStates) {
		try {
			out.write("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n\n") ;
			out.write("<kleeneFst semiring=\"" + arcType + "\">\n") ;
			out.write("  <sigma containsOther=\"" + containsOther + "\">\n") ;
			for (Iterator<Integer> iter = sigma.iterator() ; iter.hasNext() ; ) {
				out.write("    <sym>" + 
						  EscapeXML.escapeXML(symmap.getsym(iter.next().intValue())) + 
						  "</sym>\n") ;
				// switch back to this when Apache Commons Lang 3.0 becomes fully
				// available
				// 		StringEscapeUtils.escapeXml(symmap.getsym(iter.next().intValue()))
			}
			out.write("  </sigma>\n") ;
			out.write("  <arcs start=\"" + start + "\" numStates=\"" + numStates + "\">\n") ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	// called from C++, arc with non-neutral weight
	public void writeArcElmt(int srcstate, int deststate, int input, int output, float weight) {
		// StringEscapeUtils.escapeXml() would be convenient, but it
		// maps any simple character > 0x7f to an escape sequence
		//String isym = StringEscapeUtils.escapeXml(symmap.getsym(input)) ;
		String isym = EscapeXML.escapeXML(symmap.getsym(input)) ;
		//String osym = StringEscapeUtils.escapeXml(symmap.getsym(output)) ;
		String osym = EscapeXML.escapeXML(symmap.getsym(output)) ;
		// <arc s="" d="" i="" o="" w=""/>
		try {
			out.write("    <arc s=\"" + srcstate + "\"" + 
				         	 " d=\"" + deststate + "\"" +
						 	 " i=\"" + isym + "\"" +
						 	 " o=\"" + osym + "\"" +
						 	 " w=\"" + weight + "\"/>\n" ) ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	// an arc with neutral weight
	public void writeArcElmt(int srcstate, int deststate, int input, int output) {
		//String isym = StringEscapeUtils.escapeXml(symmap.getsym(input)) ;
		String isym = EscapeXML.escapeXML(symmap.getsym(input)) ;
		//String osym = StringEscapeUtils.escapeXml(symmap.getsym(output)) ;
		String osym = EscapeXML.escapeXML(symmap.getsym(output)) ;
		// <arc s="" d="" i="" o=""/>
		try {
			out.write("    <arc s=\"" + srcstate + "\"" + 
				          	" d=\"" + deststate + "\"" +
						  	" i=\"" + isym + "\"" +
						  	" o=\"" + osym + "\"/>\n" ) ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	public void writeFinalElmt(int state, float weight) {
		// write to the same temp file as the arc elmts
		// <final s="" w=""/>
		try {
			out.write("    <final s=\"" + state + "\"" + " w=\"" + weight + "\"/>\n") ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	public void writeFinalElmt(int state) {
		// write to the same temp file as the arc elmts
		// <final s=""/>
		try {
			out.write("    <final s=\"" + state + "\"/>\n") ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	public void terminateXml() {
		try {
			out.write("  </arcs>\n") ;
			out.write("</kleeneFst>\n") ;
			out.close() ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}
}

