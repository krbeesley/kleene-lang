
//	Fst.java
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

//	Each fst is encoded in a Java Fst object, which stores a pointer to
//	a native OpenFst fst.  In addition, the Java Fst object stores
//	the private sigma of the fst.

import java.util.HashSet ;
import java.util.Iterator ;

class Fst {
	// class variables, refer to as Fst.countOfFstsAllocated
	private static int countOfFstsAllocated = 0 ;
	private static int countOfCallsToFinalize = 0 ; 
	private static int countOfFstsFinalized = 0 ;

	// instance variables
	private long fstPtr ;   			// stores ptr to C++ FST object
	private HashSet<Integer> sigma ;	// private sigma for each Fst

	private boolean fromSymtab ;   	// if true, then this Fst was retrieved
									// from a symbol table, and so needs
									// to be "protected"--in practice,
									// this may require working on a copy
	private boolean containsOther ;
	private boolean isRtn ;
	
	// Constructors
    public Fst(long ptr, boolean fromsymtab, boolean containsother, HashSet<Integer> sig) {
		fstPtr = ptr ;
		fromSymtab = fromsymtab ;
		containsOther = containsother ;
		isRtn = false ;
		sigma = new HashSet<Integer>() ;
		sigma.addAll(sig) ;
		countOfFstsAllocated++ ;
    }
    public Fst(long ptr, boolean fromsymtab, boolean containsother) {
		fstPtr = ptr ;
		fromSymtab = fromsymtab ;
		containsOther = containsother ;
		isRtn = false ;
		sigma = new HashSet<Integer>() ;
		countOfFstsAllocated++ ;
    }
    public Fst(long ptr, boolean fromsymtab) {
		fstPtr = ptr ;
		fromSymtab = fromsymtab ;
		containsOther = false ;
		isRtn = false ;
		sigma = new HashSet<Integer>() ;
		countOfFstsAllocated++ ;
    }
	public Fst(long ptr) {
		this(ptr, false) ;
	}
	public Fst() {
		this(0L, false) ;
	}
	// end Constructors

	// Static (Class) Accessors
	//
	public static int getCountOfFstsAllocated() {
		return countOfFstsAllocated ;
	}
	public static int getCountOfCallsToFinalize() {
		return countOfCallsToFinalize ;
	}
	public static int getCountOfFstsFinalized() {
		return countOfFstsFinalized ;
	}

	// Instance (Object) Accessors
	//
	public long getFstPtr() {
		return fstPtr ;
	}

	public HashSet<Integer> getSigma() {
		return sigma ;
	}

	public boolean getFromSymtab() {
		return fromSymtab ;
	}

	public boolean getContainsOther() {
		return containsOther ;
	}

	public boolean getIsRtn() {
		return isRtn ;
	}

	// Mutators
	
	public void setFstPtr(long ptr) {
		fstPtr = ptr ;
	}

	public void setSigma(HashSet<Integer> hs) {
		sigma = hs ;
	}

	public void setFromSymtab(boolean b) {
		fromSymtab = b ;
	}

	public void setContainsOther(boolean b) {
		containsOther = b ;
	}

	public void setContainsOther() {
		containsOther = true ;
	}

	public void setIsRtn(boolean b) {
		isRtn = b ;
	}

	public void setIsRtn() {
		isRtn = true ;
	}

	//  To access the sigma, use fstPtr.getSigma()
	//
	//  Methods of HashMap:
	//  .add(Integer i)
	//  .add(int i)
	//  .addAll(HashSet<Integer> hs)
	//  .clear()
	//  .contains(Integer i)  looks at the value of the int,
	//                        not the identity of the Integer
	//                        objects
	//  .contains(int i)
	//  .containsAll(HashSet<Integer> hs)
	//  .isEmpty()
	//  .iterator()
	//  .remove(Integer i)
	//  .remove(int i)
	//  .removeAll(HashSet<Integer> hs)
	//  .retainAll(HashSet<Integer> hs)
	//  .size()
	//  .toString()  // uses String.valueOf(), separated by ", "

    // The finalize() method is invoked when (and if) the object is
	// garbage collected. 
	
    protected void finalize() throws Throwable {
		countOfCallsToFinalize++ ;
		try {
			// delete the native OpenFst network (a non-Java
			// structure)
			InterpreterKleeneVisitor.jdelete(fstPtr) ;
			countOfFstsFinalized++ ;
		} finally {
			// users of finalize() are encouraged to invoke
			// super.finalize() for safety
			super.finalize() ;
		}
    }
}


