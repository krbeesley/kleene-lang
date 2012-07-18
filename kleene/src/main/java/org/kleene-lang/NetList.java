
//	NetList.java
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

import java.util.LinkedList ;
import java.util.Iterator ;  

class NetList {

	private LinkedList<Fst> linkedList ;


	private boolean fromSymtab ;   	// if true, then this NetList was retrieved
									// from a symbol table, and so needs
									// to be "protected"--in practice,
									// this may require working on a copy
	
	// Constructors
    public NetList(boolean fromsymtab) {
		linkedList = new LinkedList<Fst>() ;
		fromSymtab = fromsymtab ;
    }
    public NetList() {
		this(false) ;
    }

	// end Constructors

	// Instance (Object) Accessors
	//
	public LinkedList<Fst> getLinkedList() {
		return linkedList ;
	}

	public int size() {
		return linkedList.size() ;
	}

	public boolean getFromSymtab() {
		return fromSymtab ;
	}

	public Fst get(int i) {
		if (i >= 0 && i < linkedList.size()) {
			return linkedList.get(i) ;
		} else {
			throw new KleeneArgException("List index out of bounds: " + i) ;
		}
	}

	// Mutators
	
	public void setFromSymtab(boolean b) {
		fromSymtab = b ;
	}

	public void add(Fst fst) {
		linkedList.add(fst) ;
	}

	public void addAt(int index, Fst fst) {
		linkedList.add(index, fst) ;
	}

	public void set(int index, Fst fst) {
		linkedList.set(index, fst) ;
	}

	public Fst remove(int index) {
		return linkedList.remove(index) ;
	}

	public Fst pop() {
		return linkedList.pop() ;
	}

	public void push(Fst fst) {
		linkedList.push(fst) ;
	}

	// Iterator

	public Iterator<Fst> iterator() {
		return linkedList.iterator() ;
	}

	// Misc
	
	public boolean isEmpty() {
		return linkedList.isEmpty() ;
	}
}
