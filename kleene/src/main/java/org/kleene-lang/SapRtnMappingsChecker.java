
//	SapRtnMappingsChecker.java
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

// an instance is passed to checkSapRtnMappingsNative
// provides callbacks

public class SapRtnMappingsChecker {

	int input ;
	int output ;
	boolean illegalMapping ;

	SymMap symmap ;
	InterpreterKleeneVisitor interp ;
	Object data ;

	// Constructor
	public SapRtnMappingsChecker(SymMap sm, 
								InterpreterKleeneVisitor i, 
								Object d) {
		input = 0 ;
		output = 0 ;
		illegalMapping = false ;

		symmap = sm ;
		interp = i ;
		data = d ;
	}

	// callback functions, called by the native C++ function finding 
	// suspect labels 
	
	public void inputCpv(int i) {
		// will be a MCS
		// just save the value for now
		input = i ;
	}

	public void outputCpv(int o) {
		// will be a MCS
		output = o ;

		// The callback to outputCpv 

		// Coming from checkSapRtnMappingsNative,
		// input will not equal output (this is legal and ignored)
		// output will not be zero (epsilon), this also is legal
		//
		String isymbol = symmap.getsym(input) ;
		String osymbol = symmap.getsym(output) ;

		if (	isymbol.startsWith("$")
			||	osymbol.startsWith("$")) {
			// then there is an illegal mapping
			illegalMapping = true ;
			interp.outputInterpMessage("// SapRtn illegal mapping: " + isymbol + ":" + osymbol, data) ;
		}
	}

	public boolean foundIllegalMappings() {
		return illegalMapping ;
	}
}

