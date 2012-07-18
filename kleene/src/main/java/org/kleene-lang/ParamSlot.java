
//	ParamSlot.java
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

public class ParamSlot {
	private String name ;
	private Object def ;
	private Object value ;

	// Constructors
	public ParamSlot(String n, Object d) {
		name = n ;
		def = d ;
		value = null ;
	}
	public ParamSlot(String n) {
		name = n ;
		def = null ;
		value = null ;
	}

	public String getName() {
		return name ;
	}

	public boolean hasValue() {
		if (value == null)
			return false ;
		return true ;
	}

	public boolean hasDefault() {
		if (def == null)
			return false ;
		return true ;
	}

	public void setValue(Object v) {
		value = v ;
	}

	public Object getValue() {
		return value ;
	}

	public Object getDefault() {
		return def ;
	}

	public void setValueToDefault() {
		value = def ;
	}

}
