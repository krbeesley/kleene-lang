
//	ArgCounts.java	
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

public class ArgCounts {
	// wrapper to return two values from ASTarg_list
	private int positional_args_count ;
	private int named_args_count ;
	
	// Constructor
	public ArgCounts(int pos, int nam) {
		positional_args_count = pos ;
		named_args_count = nam ;
	}

	public int getPositionalArgsCount() {
		return positional_args_count ;
	}

	public int getNamedArgsCount() {
		return named_args_count ;
	}
}
