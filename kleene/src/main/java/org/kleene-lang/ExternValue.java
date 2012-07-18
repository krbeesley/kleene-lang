
//	ExternValue.java
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

// One of these objects is pushed on the interpreter stack as the value
// of a function that returns void.

public class ExternValue {
	Frame frame ;
    // Constructor
    public ExternValue(Frame frm) {
		frame = frm ;
	}

	public Frame getFrame() {
		return frame ;
	}

		// for completeness?
	void setFrame(Frame frm) {
		frame = frm ;
	}
}
