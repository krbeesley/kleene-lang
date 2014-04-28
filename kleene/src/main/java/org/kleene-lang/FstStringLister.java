
//	FstStringLister.java
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

// Class that knows how to output Strings (to some suitable file or
// text object) found in an Fst.  The challenge is that the Fst is
// in the C++ universe and is being searched by a native (C++)
// function.  That native function will be passed an object of type
// FstStringLister and will call back to methods of this object.
//
// For now, just output to the terminal
// (PseudoTerminalInternalFrame)
//
// This may seem roundabout, but it lets Java handle the Unicode
// I/O.  And the symmap (needed to map ints to String) is on the
// Java side.

import java.util.Stack ;
import java.util.Iterator ;

public class FstStringLister implements StringLister {

	private PseudoTerminalInternalFrame terminal ;
	private SymMap symmap ;
	private Stack<Integer> intStack ;

	// Constructor
	public FstStringLister(PseudoTerminalInternalFrame ptif, SymMap sm) {
		terminal = ptif ;
		symmap = sm ;

		intStack = new Stack<Integer>() ;
	}

	public void push(int i) {
		intStack.push(new Integer(i)) ;
	}

	public void pop() {
		intStack.pop() ;
	}

	public void emit(float w) {
		int i ;
		// get String from intStack (basically a list of label integers)
		StringBuilder sb = new StringBuilder() ;
		// iterate through the integers, convert to StringBuffer (UTF-16)
		for (Iterator<Integer> iter = intStack.iterator(); iter.hasNext() ; ) {
			i = iter.next().intValue() ;
			// don't output [eps]
			if (i != 0) {
				sb.append(symmap.getsym(i)) ;
			}
		}

		terminal.appendToHistory(sb.toString() + " : " + w) ;
	}

	public void emitNoWeight() {
		int i ;
		// get String from intStack (basically a list of label integers)
		StringBuilder sb = new StringBuilder() ;
		// iterate through the integers, convert to StringBuffer (UTF-16)
		for (Iterator<Integer> iter = intStack.iterator(); iter.hasNext() ; ) {
			i = iter.next().intValue() ;
			// don't output [eps]
			if (i != 0) {
				sb.append(symmap.getsym(i)) ;
			}
		}

		terminal.appendToHistory(sb.toString()) ;
	}
}

