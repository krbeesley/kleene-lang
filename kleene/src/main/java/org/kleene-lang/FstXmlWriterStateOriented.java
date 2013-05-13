//	FstXmlWriterStateOriented.java
//
//	The Kleene Programming Language

//   Copyright 2013 SAP AG

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
// and Multichar symbols, to file, in a state-oriented XML language.
// (Compare to FstXmlWriter.java, which is more arc-oriented.)
// The Fst will in fact be
// iterated through by a native C++ function, which has access to
// the OpenFst iterators; but this C++ function will get passed an
// object of type FstXmlWriterStateOriented, and the native function 
// will call back, via JNI, to the methods.
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

public class FstXmlWriterStateOriented {

	private String arcType ;
	private HashSet<Integer> sigma ;

	private SymMap symmap ;
	private boolean containsOther ;
	private boolean ibounded ;
	private File file ;

	private BufferedWriter out ;
	private String name ;
	private String encoding ;

	// Constructor
	public FstXmlWriterStateOriented(String aType, 
						HashSet<Integer> sig, 
						SymMap sm, 
						boolean cOther, 
						boolean ibound,
						File f,
						String name,
						String encoding) {
		arcType = aType ;
		sigma = sig ;
		symmap = sm ;
		containsOther = cOther ;
		ibounded = ibound ;
		file = f ;
		this.name = name ;
		this.encoding = encoding ;

		// the File should be a full path to a file, constructed
		// appropriately on the calling side; the XML file is written there.

		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding)) ;
		} catch (Exception e) {
			e.printStackTrace() ;
		}
	}

	public void initializeXml() {
		try {
			out.write("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>\n\n") ;
			// N.B. use the attribute name "ibounded", for "input side bounded" in the XML
			out.write("<kleeneFstStateOriented name=\"" + name + "\" semiring=\"" + arcType + "\" ibounded=\"" + ibounded + "\">\n") ;
			out.write("  <sigma containsOther=\"" + containsOther + "\">\n") ;
			int cpv ;
			for (Iterator<Integer> iter = sigma.iterator() ; iter.hasNext() ; ) {
				cpv = iter.next().intValue() ;
				out.write("    <sym name=\"" + 
						  EscapeXML.escapeXML(symmap.getsym(cpv)) + 
						  "\" cpv=\"" + cpv + "\"/>\n") ;
				// switch back to this when Apache Commons Lang 3.0 becomes fully
				// available
				// 		StringEscapeUtils.escapeXml(symmap.getsym(iter.next().intValue()))
			}
			out.write("  </sigma>\n") ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	public void startStatesElmt(int start, int numStates) {
		try {
			out.write("  <states start=\"" + start + "\" numStates=\"" + numStates + "\">\n") ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	public void endStatesElmt() {
		try {
			out.write("  </states>\n") ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	// for non-final states
	public void startStateElmt(int num) {
		try {
			out.write("    <state num=\"" + num + "\">\n") ;
			// out.write("      <arcs>\n") ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	// for final state with a weight (an exit weight)
	public void startStateFinalElmt(int num, float weight) {
		try {
			out.write("    <state num=\"" + num + "\" final=\"true\" w=\"" + weight + "\">\n") ;
			//out.write("      <arcs>\n") ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	// for final state with a neutral weight
	public void startStateFinalElmtNeutralWeight(int num) {
		try {
			out.write("    <state num=\"" + num + "\" final=\"true\">\n") ;
			//out.write("      <arcs>\n") ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	public void endStateElmt() {
		try {
			//out.write("      </arcs>\n") ;
			out.write("    </state>\n") ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	// called from C++, arc with non-neutral weight
	public void writeArcElmt(int deststate, int input, int output, float weight) {
		// StringEscapeUtils.escapeXml() would be convenient, but it
		// maps any simple character > 0x7f to an escape sequence
		//String isym = StringEscapeUtils.escapeXml(symmap.getsym(input)) ;
		String isym = EscapeXML.escapeXML(symmap.getsym(input)) ;
		//String osym = StringEscapeUtils.escapeXml(symmap.getsym(output)) ;
		String osym = EscapeXML.escapeXML(symmap.getsym(output)) ;
		// <arc d="" i="" icpv="" o="" ocpv="" w=""/>
		try {
			out.write("        <arc" + 
				         	 " d=\"" + deststate + "\"" +
						 	 " i=\"" + isym + "\"" +
							 " icpv=\"" + input + "\"" +
						 	 " o=\"" + osym + "\"" +
							 " ocpv=\"" + output + "\"" +
						 	 " w=\"" + weight + "\"/>\n" ) ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	// an arc with neutral weight
	public void writeArcElmt(int deststate, int input, int output) {
		//String isym = StringEscapeUtils.escapeXml(symmap.getsym(input)) ;
		String isym = EscapeXML.escapeXML(symmap.getsym(input)) ;
		//String osym = StringEscapeUtils.escapeXml(symmap.getsym(output)) ;
		String osym = EscapeXML.escapeXML(symmap.getsym(output)) ;
		// <arc d="" i="" icpv="" o="" ocpv=""/>
		try {
			out.write("        <arc" + 
				          	" d=\"" + deststate + "\"" +
						  	" i=\"" + isym + "\"" +
							" icpv=\"" + input + "\"" +
						  	" o=\"" + osym + "\"" +
							" ocpv=\"" + output + "\"/>\n" ) ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	public void terminateXml() {
		try {
			out.write("</kleeneFstStateOriented>\n") ;
			out.close() ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}
}

