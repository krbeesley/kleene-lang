
//	InfoWriter.java
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

// Class that knows how to write XML elements, representing
// Arcs and Multichar symbols, to file.  The Fst will in fact 
// be iterated through by a native C++ function, which has access
// to the OpenFst iterators; but this C++ function will get passed
// an object of type FstXmlWriter, and the native function will
// call back, via JNI, to the .writeArcElmt(), .writeFinalElmt()
//
// This may seem roundabout, but it lets Java handle the Unicode
// I/O.  And the symmap (needed to map ints to String) is on the
// Java side.

import java.io.File ;  
import java.io.BufferedWriter ;
import java.io.IOException ;
import java.io.OutputStreamWriter ;
import java.io.FileOutputStream;

public class InfoWriter {

	private BufferedWriter out ;

	// Constructor
	public InfoWriter(File f, String encoding) {
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), encoding)) ;
		} catch (Exception e) {
			e.printStackTrace() ;
		}
	}

	public void writeLine(String str) {
		try {
			out.write(str) ;
		} catch (IOException ioe) {
			ioe.printStackTrace() ;
		}
	}

	public void close() {
		try {
			out.close() ;
		} catch (IOException ioe) {
			ioe.printStackTrace() ;
		}
	}

}

