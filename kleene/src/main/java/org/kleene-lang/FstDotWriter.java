
//	FstDotWriter.java
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

// Class that knows how to write DOT (GraphViz) source,
// representing States and Arcs, to file.  The Fst will in fact be
// iterated through by a native C++ function, which has access to
// the OpenFst iterators; but this C++ function will get passed an
// object of type FstDotWriter, and the native function will call
// back, via JNI, to the .writeDotState() .writeDotArc() methods
//
// This may seem roundabout, but it lets Java handle the Unicode
// I/O.  And the symmap (needed to map ints to String) is on the
// Java side.

import java.io.File ;  
import java.io.BufferedWriter ;
import java.io.IOException ;
import java.io.OutputStreamWriter ;
import java.io.FileOutputStream;
import org.apache.commons.lang3.StringEscapeUtils ;


public class FstDotWriter {

	private SymMap symmap ;
	private BufferedWriter out ;
	private String sigma ;
	private String encoding ;

	// Constructor
	public FstDotWriter(SymMap sm, File f, String sig, String encoding) {
		symmap = sm ;
		sigma = sig ;
		this.encoding = encoding ;

		// the File should be a full path to a file, constructed
		// appropriately on the calling side; the XML file is written there.

		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), encoding)) ;
		} catch (Exception e) {
			e.printStackTrace() ;
		}
	}

	public void initializeDot() {
		try {
			out.write("digraph FST {\n") ;
			out.write("  rankdir = LR;\n") ;

			out.write("  size = \"7.5,10\";\n") ;   // must allow for half-inch margins on page
			out.write("  page = \"8.5,11\";\n") ;

					//StringEscapeUtils.escapeXml(sigma)
					//StringEscapeUtils.escapeJava(sigma)
					//   for 2.4
					//StringEscapeUtils.escapeHtml(sigma)
					//   in 3.0
					//StringEscapeUtils.escapeHtml4(sigma)
					//StringEscapeUtils.escapeHtml4(sigma)

			String displaySigma = (" " + StringEscapeUtils.escapeJava(sigma))
				.replaceAll(" \\\\\\\\", " __LITERALBACKSLASH")  // literal backslash
				.replaceAll(" \\\\", " \\\\\\\\") 
				.replaceAll(" __LITERALBACKSLASH", " \\\\\\\\\\\\\\\\") ;
			// literalize backslashes for the dot source file
			

			out.write("  label = \"Alphabet: " + displaySigma + "\";\n" ) ;
			out.write("  center = \"true\";\n") ;
			out.write("  encoding = \"" + encoding + "\";\n" ) ;

			//out.write("  orientation = landscape;\n") ;
			//out.write("  rotate = \"90\";\n") ;

			out.write("  ranksep = \"0.3\";\n") ;
			out.write("  nodesep = \"0.2\";\n\n") ;
			out.write("  node[shape=circle, style=solid, fontsize=10] ;\n") ;
			out.write("  edge[fontsize=10, arrowhead=open] ;\n\n") ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	// called from C++ for each state with non-neutral weight
	public void writeDotState(int state_id, boolean is_start, boolean is_final, float weight) {
		String st = "" ;
		String fi = "" ;
		String we = "" ; 
		if (is_start) {
			try {
				// create a Start indication for the start state
				out.write("Start [shape=plaintext] ;\n") ;
				out.write("Start -> " + state_id + " [arrowhead=normal, arrowtail=dot] ;\n") ;
			} catch (IOException e) {
				e.printStackTrace() ;
			}
			st = " , style=bold" ;
		}
		if (is_final) {
			fi = " , shape=doublecircle" ;
			we = "/" + weight ;
		}
		try {
			out.write(state_id 
					  + " [label = \"" 
					  + state_id 
					  + we 
					  + "\"" 
					  + st 
					  + fi 
					  + "] ;\n") ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	// called from C++ for each state with neutral weight
	public void writeDotState(int state_id, boolean is_start, boolean is_final) {
		String st = "" ;
		String fi = "" ;
		if (is_start) {
			try {
				// create a Start indication for the start state
				out.write("Start [shape=plaintext] ;\n") ;
				out.write("Start -> " + state_id + " [arrowhead=normal, arrowtail=dot] ;\n") ;
			} catch (IOException e) {
				e.printStackTrace() ;
			}

			st = " , style=bold" ;
		}
		if (is_final) {
			fi = " , shape=doublecircle" ;
		}
		try {
			out.write(state_id + " [label = \"" + state_id + "\"" + st + fi + "] ;\n") ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	// called from C++, arc with non-neutral weight
	public void writeDotArc(int srcstate, int deststate, int input, int output, float weight) {
		// Changed from .escapeXml to .escapeJava 21 Nov 2008
		// May need other literalization.  This change was made to accommodate
		// literal backslashes e.g. for     $a = \\ ; 
		// input and output will be single backslash chars, but
		// need to generate 'dot' code with  0 -> 1 [label = "\\:\\" ] ;
		// to print out and see single backslashes in the graph.
		//

		//String isym = StringEscapeUtils.escapeXml(symmap.getsym(input)) ;
		String isym = StringEscapeUtils.escapeJava(symmap.getsym(input)) ;
		if (isym.equals("\\\\")) {
			isym = "\\\\\\\\" ;	// literal backslash
		} else if (isym.startsWith("\\")) {
			isym = "\\" + isym ;
		}

		//	for 2.4
		//String isym = StringEscapeUtils.escapeHtml(symmap.getsym(input)) ;
		//	for 3.0
		//String isym = StringEscapeUtils.escapeHtml3(symmap.getsym(input)) ;
		//String isym = StringEscapeUtils.escapeHtml4(symmap.getsym(input)) ;

		//String osym = StringEscapeUtils.escapeXml(symmap.getsym(output)) ;
		String osym = StringEscapeUtils.escapeJava(symmap.getsym(output)) ;
		if (osym.equals("\\\\")) {
			osym = "\\\\\\\\" ;	// literal backslash
		} else if (osym.startsWith("\\")) {
			osym = "\\" + osym ;
		}

		//	for 2.4
		//String osym = StringEscapeUtils.escapeHtml(symmap.getsym(output)) ;
		//	for 3.0
		//String osym = StringEscapeUtils.escapeHtml3(symmap.getsym(output)) ;
		//String osym = StringEscapeUtils.escapeHtml4(symmap.getsym(output)) ;

		// 0 -> 1 [label = "a:a/0.5"] ;
		String weightstr = "/" + weight ;
		try {
			out.write("  " + srcstate + " -> " + deststate + " [label = \"" + isym + ":" + osym + weightstr + "\"] ;\n") ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	// an arc with neutral weight
	public void writeDotArc(int srcstate, int deststate, int input, int output) {
		//String isym = StringEscapeUtils.escapeXml(symmap.getsym(input)) ;
		String isym = StringEscapeUtils.escapeJava(symmap.getsym(input)) ;
		if (isym.equals("\\\\")) {
			isym = "\\\\\\\\" ;	// literal backslash
		} else if (isym.startsWith("\\")) {
			isym = "\\" + isym ;
		}
		//String osym = StringEscapeUtils.escapeXml(symmap.getsym(output)) ;
		String osym = StringEscapeUtils.escapeJava(symmap.getsym(output)) ;
		if (osym.equals("\\\\")) {
			osym = "\\\\\\\\" ;	// literal backslash
		} else if (osym.startsWith("\\")) {
			osym = "\\" + osym ;
		}
		try {
			out.write("  " + srcstate + " -> " + deststate + " [label = \"" + isym + ":" + osym + "\"] ;\n") ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}

	public void terminateDot() {
		try {
			out.write("}\n") ;
			out.close() ;
		} catch (IOException e) {
			e.printStackTrace() ;
		}
	}
}

