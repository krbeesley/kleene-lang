
//	InterpData.java
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

// The interpreter (implemented as a Visitor, passes around a
// handle to this object as the "data" argument.  Just one object
// of this class is allocated and reused, calling reset() and
// resetForGUI() as necessary. 

import javax.swing.JPanel ;

public class InterpData {

    private boolean quitSession ;
    private boolean funcReturn ;
    private boolean loopBreak ;
    private boolean loopContinue ;
	private boolean inGUI ;
	private boolean isArg ;  // distinguish arg vs. param (similar syntax)
	private KleeneGUI gui ;

    // Constructor
    public InterpData() {
		this.reset() ;
    }

    public void reset() {
		quitSession = false ;
		funcReturn = false ;
		loopBreak = false ;
		loopContinue = false ;
		inGUI = false ;
		isArg = false ;
		gui = null ;
    }

	public void resetForGUI(KleeneGUI g) {
		quitSession = false ;
		funcReturn = false ;
		loopBreak = false ;
		loopContinue = false ;
		inGUI = true ;
		isArg = false ;
		gui = g ;
	}

    public boolean getQuitSession() {
		return quitSession ;
    }

    // need this at all?
    public void setQuitSession(boolean b) {
		quitSession = b ;
    }

    public boolean getFuncReturn() {
		return funcReturn ;
    }

    public void setFuncReturn(boolean b) {
		funcReturn = b ;
    }

    public boolean getLoopBreak() {
		return loopBreak ;
    }

    public void setLoopBreak(boolean b) {
		loopBreak = b ;
    }

    public boolean getLoopContinue() {
		return loopContinue ;
    }

    public void setLoopContinue(boolean b) {
		loopContinue = b ;
    }

	public boolean getInGUI() {
		return inGUI ;
	}

	public void setInGui() {
		inGUI = true ;
	}

	public boolean getIsArg() {
		return isArg ;
	}

	public void setIsArg(boolean b) {
		isArg = b ;
	}

	public KleeneGUI getGUI() {
		return gui ;
	}
}
