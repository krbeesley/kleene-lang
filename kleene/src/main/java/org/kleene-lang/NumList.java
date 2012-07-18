
//	NumList.java
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

class NumList {

	private LinkedList<Object> linkedList ;

	// allow only Long and Double objects to be added/set


	private boolean fromSymtab ;   	// if true, then this NetList was retrieved
									// from a symbol table, and so needs
									// to be "protected"--in practice,
									// this may require working on a copy
	
	// Constructors
    public NumList(boolean fromsymtab) {
		linkedList = new LinkedList<Object>() ;
		fromSymtab = fromsymtab ;
    }
    public NumList() {
		this(false) ;
    }

	// end Constructors

	// Instance (Object) Accessors
	//
	public LinkedList<Object> getLinkedList() {
		return linkedList ;
	}

	public int size() {
		return linkedList.size() ;
	}

	public boolean getFromSymtab() {
		return fromSymtab ;
	}

	public Object get(int i) {
		if (i >= 0 && i < linkedList.size()) {
			return linkedList.get(i) ;
		} else {
			throw new KleeneArgException("List index out of bounds: " + 1) ;
		}
	}

	// Mutators
	
	public void setFromSymtab(boolean b) {
		fromSymtab = b ;
	}

	public void add(Long l) {
		linkedList.add(l) ;
	}

	public void add(Double d) {
		linkedList.add(d) ;
	}

	public void addAt(int index, Long l) {
		linkedList.add(index, l) ;
	}

	public void addAt(int index, Double d) {
		linkedList.add(index, d) ;
	}

	public void set(int index, Long l) {
		linkedList.set(index, l) ;
	}

	public void set(int index, Double d) {
		linkedList.set(index, d) ;
	}

	public Object remove(int index) {
		return linkedList.remove(index) ;
	}

	public Object pop() {
		return linkedList.pop() ;
	}

	public void push(Long l) {
		linkedList.push(l) ;
	}

	public void push(Double d) {
		linkedList.push(d) ;
	}

	// Iterator
	
	public Iterator<Object> iterator() {
		return linkedList.iterator() ;
	}

	// Misc
	
	public boolean isEmpty() {
		return linkedList.isEmpty() ;
	}
}
