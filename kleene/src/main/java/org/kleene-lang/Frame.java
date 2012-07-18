
//	Frame.java
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

import java.util.HashMap ;
import java.util.HashSet ;
// HashMap<K,V>
// put() get()
// permits null key and null values

public class Frame {
    private Frame staticMother ;  // follow this link up the environment
    private Frame dynamicMother ; // return to this link (from a function
                                  //   call); in a Frame created for a
                                  // function call, the static and dynamic
                                  // mothers can be different

    // in the HashMap (the core of the symbol table), the keys
    // are always String, and the values are various kinds of Object
    private HashMap<String, Object> symtab ;

    /* Constructor */
    // declare constructor public?
    public Frame (Frame stat, Frame dyn) {
		staticMother = stat ;
		dynamicMother = dyn ;
		symtab = new HashMap<String, Object>() ;
    }

    public Frame getStaticMother() {
		return staticMother ;
    }

    public Frame getDynamicMother() {
		return dynamicMother ;
    }

    // the key should always be a String
    public void put (String key, Object value) {
		// check here to see if the key already exists?
		// or checked before in the Environment?
		symtab.put(key, value) ;
    }

    // the values can be several types of Object
    // N.B. I think that containsKey(), see below, is always checked before
    // calling get(), but make sure
    public Object get(String key) {
		if (symtab.containsKey(key)) {
			return symtab.get(key) ;
		} else {
			return null;
		}
    }

	public HashSet<String> keySet() {
		return new HashSet<String>(symtab.keySet()) ;
	}

	public void remove(String key) {
		if (symtab.containsKey(key)) {
			symtab.remove(key) ;
		} else {
			throw new SymtabException("Attempt to remove a non-existent entry.") ;
		}
	}

    public boolean containsKey(String key) {
		return symtab.containsKey(key) ;
    }

    // HashMap allows null key and null values
    // It may be possible to have an entry, for a local
    // variable/name, which is not yet bound, hence has
    // value null
    public boolean isDefined(String key) {
		if (symtab.containsKey(key)) {
			return (symtab.get(key) != null) ;
		} else {
			return false ;
		} 
    }
}
