
//	FuncValue.java
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

// Stores the value of a function in the symbol table; modified 7
// Aug 2008 to store a ParamArrayList (basically an ArrayList of
// ParamSlot objects) rather than the old ASTparam_list (which was
// just an AST).  A ParamSlot may store a default value for a
// param.

import java.util.ArrayList ;
import java.util.Iterator ; 

public class FuncValue {

	// the staticFrame is the Frame in which the function was defined;
	// free (non-local) variables in the function body are resolved 
	// through the static link, thus implementing "lexical scope"
	
    private Frame staticFrame ;
    private ArrayList<ParamSlot> paramArrayList ;
    private ASTfunc_block funcBlock ;

    // Constructor
    public FuncValue (Frame f, ArrayList<ParamSlot> pal, ASTfunc_block fb) {
		staticFrame = f ;
		paramArrayList = pal ;
		funcBlock = fb ;
    }

    public Frame getStaticFrame() {
		return staticFrame ;
    }

    public ArrayList<ParamSlot> getParamArrayList() {
		// need to return a new deep copy for each function call
		ArrayList<ParamSlot> deepCopy = new ArrayList<ParamSlot>() ;

		for (Iterator<ParamSlot> iter = paramArrayList.iterator(); iter.hasNext(); ) {
			ParamSlot ps = iter.next() ;
			deepCopy.add(new ParamSlot(ps.getName(), ps.getDefault())) ;
		}

		return deepCopy ;
    }

    public ASTfunc_block getFuncBlock() {
		return funcBlock ;
    }
}

