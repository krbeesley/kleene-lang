
//	OpenFstLibraryWrapper.java
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

// This file is a Java wrapper for the OpenFst library.  The
// function herein are called by the interpeter
// (InterpreterKleeneVisitor.java).  Perhaps someday other wrappers
// will be written, wrapping alternative libraries.
 
import java.util.ArrayList ;  
import java.util.HashSet ;
import java.util.Iterator ;

import com.ibm.icu.text.UCharacterIterator ;

public class OpenFstLibraryWrapper 
		// KRB:  If multiple libraries are ever used, make them all
		// implement LibraryChecker
		// implements LibraryChecker
		{

	Environment env ;
	SymMap symmap ;

	OpenFstLibraryChecker checker ;

	// Constructor constructor
	public OpenFstLibraryWrapper(Environment e, SymMap sm) {
		env = e ;
		symmap = sm ;
		checker = new OpenFstLibraryChecker(this) ;
	}

	// ****************************************************************
	//             	Native function declarations
	//				C++ functions defined in kleeneopenfst.cc
	// ****************************************************************

	// NATIVE METHODS
	// native methods
	// Native Methods
	// Declare native (C++) methods implemented in kleeneopenfst.cc,
	// which is linked with libfst.dylib (the OpenFst library built for OS X)
	//
	// N.B. that native methods declared 'private STATIC native ...' as below
	// are _class_ methods of the InterpreterKleeneVisitor class.
	// So on the JNI side, the signature is 
	// JNIEnv *env, jclass cls
	//
	// rather than   JNIEnv *env, jobject obj
	// for an _object_ method (called with some obj.nameOfNativeMethod()
	//
	// In this class, a native class method is simply called with
	// nativeMethod()        (not    someObject.nativeMethod() )
	
	// See if the shared library (.jnilib on OS X) is loading and working
	private static native void helloWorldNative() ;
	
	// Return ptr to an empty native fst (an fst that encodes the empty language)
	// no states at all
	private static native long emptyLanguageFstNative() ;

	// Return ptr to an empty native fst (an fst that encodes the empty
	// language) with one state, the start state, also final
	private static native long emptyLanguageWithStartStateFstNative() ;

	// Return ptr to a native fst that encodes the empty-string language
	// (the language that contains only the empty string)
	private static native long emptyStringLanguageFstNative() ;

	// return a ptr to a native sigma* fst, used in intersected_exp
    //private static native long sigmaStarNativeFst(int other_id,
	//	float arc_weight, float final_weight) ;
    private static native long universalLanguageFstNative(int other_id,
		float arc_weight, float final_weight) ;

	// return a ptr to a native universal relation fst, equivalent to
	// (.:.)* ((.:"")* | ("":.)*)
	// i.e. no two-sided epsilons
	// and any one-sided epsilons are on only one side, at the end
    private static native long universalRelationFstNative(
		int other_id, int other_nonid,
		float arc_weight, float final_weight) ;

	// return a ptr to a native fst with one start state, one final state,
	// and one labeled arc linking them.
	// Pass in input symbol (int cpv), output symbol (int cpv), 
	// arc weight,and final weight
	private static native long oneArcFstNative(int icpv, int ocpv, 
			float arc_weight, float final_weight) ;

	// construct a native fst of one path from an array of code point values
	private static native long oneStringFstNative(int[] cpvArray) ;

	private static native long copyFstNative(long fst) ;

	private static native void rmWeightDestFstNative(long fst) ;
	private static native long rmWeightFstNative(long fst) ;

	private static native void concatIntoFirstNative(long first, long second) ;
	private static native void  unionIntoFirstNative(long first, long second) ;

	private static native long charRangeUnionFstNative(int firstCpv, int lastCpv) ;

	private static native long differenceNative(long first, long second) ;
//	private static native boolean isEquivalentNative(long first, long second, double delta) ;
	private static native long intersectNative(long first, long second) ;
	private static native long composeNative(long first, long second) ;

	// take the argument fst and return the network for fst*
	private static native void kleeneStarInPlaceNative(long first) ;

	// take the argument fst and return the network for fst+
	private static native void kleenePlusInPlaceNative(long first) ;

	// implementing fst{n, m}
	private static native long iterateLowHighNative(long fst, long low, long high) ;

	// boolean tests
	private static native boolean isAcceptorNative(long fst) ;
	private static native boolean isSemanticAcceptorNative(long fst, int other_nonid) ;
	private static native boolean isWeightedNative(long fst) ;
	private static native boolean isUnweightedNative(long fst) ;
	private static native boolean isEpsilonFreeNative(long fst) ;
	private static native boolean isIDeterministicNative(long fst) ;
	private static native boolean isODeterministicNative(long fst) ;
	private static native boolean isEmptyLanguageNative(long fst) ;
	private static native boolean isCyclicNative(long fst) ;
	private static native boolean isAcyclicNative(long fst) ;
	private static native boolean isUBoundedNative(long fst) ;
	private static native boolean isLBoundedNative(long fst) ;

	private static native boolean isStringNative(long fst) ; 
	// i.e. just one-string language
	// in the syntax, #^isString(Fst fst) or
	//                #^isSingleStringLanguage(Fst fst)
	//
	private static native boolean containsEmptyStringNative(long fst) ;
	
	private static native boolean isNotStringNative(long fst) ;

	private static native String getShortFstInfoNative(long fst) ;
	private static native String getArcTypeNative(long fst) ;

	private static native void connectInPlaceNative(long fst) ;
	private static native void sortInputArcsInPlaceNative(long fst) ;
	private static native void sortOutputArcsInPlaceNative(long fst) ;

	private static native long reverseNative(long fst) ;
	private static native void invertInPlaceNative(long fst) ;
	private static native long shortestPathNative(long fst, int nshortest) ;
	private static native void inputProjectionInPlaceNative(long fst) ;
	private static native void outputProjectionInPlaceNative(long fst) ;

	private static native long randGenNative(long fst, long npathval, long max_lengthval) ;

	private static native void rmEpsilonInPlaceNative(long fst) ;  
	// std OpenFst RmEpsilon works in place (destructive)

	private static native long determinizeNative(long fst) ;       
	// std OpenFst Determinize(A, &B) does not work in place

	private static native void optimizeInPlaceNative(long fst, boolean
	determinize, boolean minimize, boolean rmepsilon) ;
	// called routinely to "optimize" networks during the interpretation
	// of regular expressions; the goal is to minimize the states and
	// arcs of a network as much as possible, using Determinize() and
	// Minimize(), and sometimes Encode/Decode, even if the result is not 
	// mathematically fully determinized or minimized

	private static native void determinizeInPlaceNative(long fst) ;
	// to be parallel to RmEpsilon(&A) and Minimize(&A) see kleeneopenfst.cc

	private static native void minimizeInPlaceNative(long fst) ;
	// std OpenFst Minimize() works in place (destructive)

	private static native void synchronizeInPlaceNative(long fstp) ;

	// start state, count of states/arcs, handled as int
	private static native int startStateNative(long fst) ;
	private static native int numStatesNative(long fst) ;
	private static native int numArcsNative(long fst) ;

	// number of paths handled as long (can be astronomical)
	private static native long numPathsNative(long fst) ;
	private static native void listAllStringsNative(long fst, int projection, 
												StringLister stringLister) ;

	private static native void fstDumpNative(long fst) ;

	private static native int  addStatesAndArcsNative(long dest, long src) ; 
	private static native long rrGrammarLinkNative(long fst, int[] ikeys, int[] ivals) ; 
	// ret. 0 for error, ret. 1 for successful return

	private static native void cppDeleteNative(long fst) ;

	private static native int[] getSingleStringNative(long fst) ;
	private static native int    getSingleArcLabelNative(long fst) ;

	private static native void fst2xmlNative(long fst, FstXmlWriter o, int cpv) ;
	private static native void fst2xmlStateOrientedNative(long fst, FstXmlWriterStateOriented o, int cpv) ;
	private static native void fst2dotNative(long fst, FstDotWriter o) ;

	//private static native void checkSapRtnMappingsNative(long fst,
	//												SapRtnMappingsChecker srmc, 
	//												int cpv) ;

	private static native void iterate4mcsNative(long fst, 
									TranslitTokenizerBuilder ttb, 
									int cpv) ;

	// used to create an Fst from XML
	private static native void addStatesNative(long fstp, int numStates) ;
	private static native void setStartNative(long fstp, int state) ;
	private static native void addArcNative(long fstp, 
									int src, 
									int i, 
									int o, 
									float weight, 
									int dest) ;
	private static native void addArcNeutralWeightNative(long fstp, 
									int src, 
									int i, 
									int o, 
									int dest) ;
	private static native void setFinalNative(long fstp, int state, float weight) ;
	private static native void setFinalNeutralWeightNative(long fstp, int state) ;

	// these two are used for debugging
	private static native void writeBinaryNative(long fstp, String str) ;
	//private static native void binary2txtNative(String str) ;

	private static native String getFstPtrStringNative(long fstp) ;

	private static native void expandOtherArcsNative(long fstp, 
		int[] intArray, int other_id, int other_nonid) ;

	private static native void fixOtherInputBeforeComposeNative(long fstp,
		int other_id, int other_nonid) ;

	private static native void fixOtherOutputBeforeComposeNative(long fstp,
		int other_id, int other_nonid) ;

	private static native void fixOtherAfterComposeNative(long fstp,
		int other_id, int other_nonid) ;

	private static native void inputProjectionFixOtherInPlaceNative(long fstp, int
		other_id, int other_nonid) ;
	private static native void outputProjectionFixOtherInPlaceNative(long fstp, int
		other_id, int other_nonid) ;

	private static native void changeInputToEpsilonInPlaceNative(long
	fstp, int other_id, int other_nonid) ;

	private static native void changeOutputToEpsilonInPlaceNative(long
	fstp, int other_id, int other_nonid) ;

	private static native void deleteOtherArcsInPlaceNative(long fstp, 
													int other_id, 
													int other_nonid) ;
	private static native int[] getLabelsNative(long fstp) ;
	private static native int[] getOutputLabelsNative(long fstp) ;

	private static native int hasCyclicDependenciesNative(int baseFstInt, 
												int[] symInts, 
												long[] netPtrs) ;
	private static native long expandRtnNative(int baseFstInt, 
												int[] symInts, 
												long[] netPtrs) ;  

	// old nativeSubstSymbolInPlace
	private static native void substLabelInPlaceNative(long fstp, 
													int orig, 
													int repl) ;


	private static native void synchronizeAltRuleInPlaceNative(long fstp, 
													int ruleRightAngleSymVal,
													int hardEpsilonSymVal,
													int other_id,
													int other_nonid) ;
	// add diacritic insensitivity to a network
	private static native int[] addDiacNative(long fstp, 
										boolean input, 	// input/upper side
										boolean output) ; // output/lower side
	// add case insensitivity to a network
	private static native int[] addCaseNative(long fstp, boolean all,
										boolean add_uc, // add uc for existing lc
										boolean add_lc, // add lc for existing uc
										boolean input, 	// input/upper side
										boolean output) ; // output/lower side
	private static native int[] convertCaseNative(long fstp, boolean all,
										boolean to_uc, 	// convert lc to uc
										boolean to_lc,	// convert uc to lc
										boolean input, 
										boolean output) ;

	// ****************************************************************
	//              End of native function declarations
	// ****************************************************************
	
	// load the C++ library that implements the native functions declared above
	// libkleeneopenfst.jnilib for OS X, libkleeneopenfst.so for Linux;
	// a dll for Windows
	static {
		System.loadLibrary("kleeneopenfst") ;
	}

	
	// ****************************************************************
	// 				Definitions
	// ****************************************************************
	
	// KRB:  This should be the only place where "OTHER_ID" and "OTHER_NONID"
	// are used as strings
	public String otherIdSym		= "OTHER_ID" ;
	public String otherNonIdSym 	= "OTHER_NONID" ;
	
	// ****************************************************************
	// 				Helper functions (mostly private)
	// ****************************************************************

    public boolean isTrue(Object obj) {
		if (obj instanceof Long) {
	    	return (((Long)obj).longValue() != 0L) ;
		} else {
		    // must be Double
		    // literals like 0.0 are assumed to be double in Java
		    return (((Double)obj).doubleValue() != 0.0) ;
		}
    }

	private void addSigma(Fst fstA, Fst fstB) {
		fstA.getSigma().addAll(fstB.getSigma()) ;
	}

	private void addSigmaOther(Fst fstA, Fst fstB) {
		fstA.getSigma().addAll(fstB.getSigma()) ;
		if (fstB.getContainsOther()) {
			fstA.setContainsOther(true) ;
		}
		if (fstB.getIsRtn()) {
			fstA.setIsRtn() ;
		}
	}

	public boolean isSapRtnConventions() {
		if ( ((Long)env.get("#KLEENErtnConventions")).longValue() ==
			 ((Long)env.get("#KLEENEsapRtnConventions")).longValue()
			 ) {
			return true ;
		} else {
			return false ;
		}
	}

	private boolean isTrivialRtnRefCrossproduct(Fst a, Fst b) {
		// called only when a and b are already known to be SAP RTNs
		// return true if like '$foo':'$foo'
		if (IsAcceptor(a) 
			&& IsAcceptor(b) 
			&& NumStates(a) == 2 
			&& NumStates(b) == 2 
			&& NumArcs(a) == 1 
			&& NumArcs(b) == 1 
			&& GetSingleArcLabel(a) == GetSingleArcLabel(b)) {
			return true ;
		}
		return false ;
	}

	// stringFromCpv is for code point values that represent
	//	defined Unicode characters
	public String stringFromCpv(int cpv) {
		if (Character.charCount(cpv) == 1) {
			// straightforward
			return String.valueOf((char) cpv) ;
		} else {
			// overhead of creating a char[] array
			return new String(Character.toChars(cpv)) ;
		}
	}

	// called when promoting OTHER, where special symbols
	// should not be considered
	public void stripSpecialCharsOther(HashSet<Integer> hs) {
		String symbolName ;
		int i ;

		if (isSapRtnConventions()) {  	
			// using '$foo' as a special symbol; so here,
			// symbols starting with '$' should not be
			// considered when promoting OTHER
			for (Iterator<Integer> iter = hs.iterator(); iter.hasNext(); ) {
				i = iter.next().intValue() ;

				symbolName = symmap.getsym(i) ;
				if(	 	symbolName.equals(otherIdSym)
					||	symbolName.equals(otherNonIdSym)
					||	symbolName.startsWith("__")
					||	symbolName.startsWith("$")) {
					iter.remove() ;
				}
			}
		} else {
			for (Iterator<Integer> iter = hs.iterator(); iter.hasNext(); ) {
				i = iter.next().intValue() ;

				// N.B. special symbols starting with "__", e.g. __$foo
				// should not be considered when promoting OTHER
				symbolName = symmap.getsym(i) ;
				if(	 	symbolName.equals(otherIdSym)
					||	symbolName.equals(otherNonIdSym)
					||	symbolName.startsWith("__")) {
					iter.remove() ;
				}
			}
		}
	}

	private Fst promoteSigmaOther(Fst fstA, Fst fstB) {
		return promoteSigmaOther(fstA, fstB.getSigma(), true) ;
	}

	private Fst promoteSigmaOther(Fst fstA, Fst fstB, boolean copyIfFromSymtab) {
		return promoteSigmaOther(fstA, fstB.getSigma(), copyIfFromSymtab) ;
	}

	private Fst promoteSigmaOther(Fst fstA, int[] symbolsAdded) {
		return promoteSigmaOther(fstA, symbolsAdded, true) ;
	}

	private Fst promoteSigmaOther(Fst fstA, 
										int[] symbolsAdded, 
										boolean copyIfFromSymtab) {
		int size = symbolsAdded.length ;
		if (size > 0) {
			HashSet<Integer> pseudoSigmaB = new HashSet<Integer>(size) ;
			for (int j = 0; j < size; j++) {
				pseudoSigmaB.add(new Integer(symbolsAdded[j])) ;
			}
			// sometimes the OTHER_NONID can be present
			pseudoSigmaB.remove(symmap.getint(otherNonIdSym)) ;
			// I don't think OTHER_ID can be present, but for safety...
			pseudoSigmaB.remove(symmap.getint(otherIdSym)) ;
			return promoteSigmaOther(fstA, pseudoSigmaB, copyIfFromSymtab) ;
		}
		return fstA ;
	}

	private Fst promoteSigmaOther(Fst fstA, HashSet<Integer> sigmaB) {
		return promoteSigmaOther(fstA, sigmaB, true) ;
	}

	private Fst promoteSigmaOther(Fst fstA, 
							HashSet<Integer> sigmaB, 
							boolean copyIfFromSymtab) {
		// look at fstA relative to sigma of fstB, copy and/or
		// modify fstA as necessary, to unify/promote the OTHER
		// of FstA

		Fst returnFst = fstA ;  // by default, return fstA

		if (returnFst.getContainsOther()) {

			// may need OTHER-arc expansion
			// Get HashSet   sigmaB - sigmaA
			HashSet<Integer> sigmaBnotInA = new HashSet<Integer>(sigmaB) ;
			sigmaBnotInA.removeAll(fstA.getSigma()) ;

			stripSpecialCharsOther(sigmaBnotInA) ; // sensitive to the RtnConventions

			// leaves a HashSet with all the symbols (ints)
			// in fstB that are not in fstA

			if (!(sigmaBnotInA.isEmpty())) {

				// then need to expandOtherArcs of fstA, 
				//     or a copy of it
				if (copyIfFromSymtab && fstA.getFromSymtab()) {
					// then need to copy it, to preserve the
					// integrity of the Fst in the symtab;
					//   expand the copy
					returnFst = CopyFst(fstA) ;
				}

				// convert sigmaBnotInA into a simple int[] array
				// for passing to native (C++) function
				int[] intArray = new int[sigmaBnotInA.size()] ;
				int n = 0 ;
				for (Iterator<Integer> iter =
					sigmaBnotInA.iterator();
					iter.hasNext() ;
					) {
					intArray[n++] = iter.next().intValue() ;
				}

				// add the sigmaBnotInA symbols to the sigma
				// of the returnFst

				returnFst.getSigma().addAll(sigmaBnotInA) ;
				expandOtherArcsNative(returnFst.getFstPtr(), 
						intArray,
						symmap.getint(otherIdSym),
						symmap.getint(otherNonIdSym)) ;
			} 
		}  // else no need to change fstA at all
		return returnFst ;
	}

	private Fst fixOtherBeforeCompose(Fst fst, boolean inputProj) {
		// inputProj:   true for input projection
		//              false for output projection

		Fst returnFst = fst ;  // by default, return fst

		if (fst.getContainsOther()) {

			if (fst.getFromSymtab()) {
				// then need to copy it, to preserve the
				// integrity of the Fst in the symtab;
				//   expand the copy
				// KRB: really need to copy only if there are
				// OTHER_ID labels on the output side
				returnFst = CopyFst(fst) ;
			}

			if (inputProj)  // input side
				// native function
				fixOtherInputBeforeComposeNative(returnFst.getFstPtr(),
						symmap.getint(otherIdSym),
						symmap.getint(otherNonIdSym)) ;
			else // do the same on the output side
				// native function
				fixOtherOutputBeforeComposeNative(returnFst.getFstPtr(),
						symmap.getint(otherIdSym),
						symmap.getint(otherNonIdSym)) ;
		}
		return returnFst ;
	}

	// called for ExpandRtn(), which currently works only with
	// OpenFstRtnConventions
	private void stripReferencesToSubnets(HashSet<Integer> hs) {
		String symbolName ;
		int i ;
		for (Iterator<Integer> iter = hs.iterator(); iter.hasNext(); ) {
			i = iter.next().intValue() ;

			symbolName = symmap.getsym(i) ;
			if(	symbolName.startsWith("__$")) {
				iter.remove() ;
			}
		}

	}

	
	// ****************************************************************
	// ****************************************************************
	// 			Functions called from the Interpreter
	// ****************************************************************
	// ****************************************************************

	// ************************************************************
	// 		Functions returning basic fsts
	// ************************************************************
	
	public Fst EmptyLanguageFst() {
		// return an empty Fst (empty language)
		return new Fst(emptyLanguageFstNative()) ;
	}

	public Fst EmptyLanguageWithStartStateFst() {
		// return an empty Fst (empty language)
		return new Fst(emptyLanguageWithStartStateFstNative()) ;
	}

	public Fst EmptyStringLanguageFst() {
		// language that contains only the empty string
		return new Fst(emptyStringLanguageFstNative()) ;
	}

	// used for notion of "any", Kleene syntax .
	public Fst SigmaFst() {
		// language of all single-symbol strings
		Fst sigmaFst = OneArcFst(otherIdSym) ;
		// OneArcFst calls setContainsOther()
		return sigmaFst ;
	}

	// used for notion of "any to any", Kleene syntax .:.
	public Fst SigmaSigmaFst() {
		Fst fst1 = OneArcFst(otherIdSym) ;
		// OneArcFst calls setContainsOther()
		Fst fst2 = OneArcFst(otherNonIdSym) ;
		fst1 = UnionIntoFirstInPlace(fst1, fst2) ;
		return fst1 ;
	}

	public Fst UniversalLanguageFst() {
		// semiring generalization point
		Fst universalLanguageFst = 
			new Fst(
				universalLanguageFstNative(
					symmap.getint(otherIdSym), 
					// weights here are for the tropical semiring
					(float) 0.0,   	// weight of the one arc
					(float) 0.0		// weight of the final state
				)
			) ;
		universalLanguageFst.setContainsOther(true) ;
		return universalLanguageFst ;
	}
	
	public Fst UniversalRelationFst() {
		// semiring generalization point
		Fst universalRelationFst = 
			new Fst(
				universalRelationFstNative(
					symmap.getint(otherIdSym),
					symmap.getint(otherNonIdSym),
					// weights for the tropical semiring
					(float) 0.0,   	// weight of the arcs
					(float) 0.0		// weight of the final states
				) 
			) ;
		universalRelationFst.setContainsOther(true) ;
		return universalRelationFst ;
	}

	// OneArcFst(int icpv, int ocpv) and variants are used to create a
	// minimal Fst consisting of a start state, a final state, and a single
	// linking arc labeled with the argument value(s)

	// this is the main version of OneArcFst()

	// semiring generalization point
	public Fst OneArcFst(int icpv, int ocpv, float arcWeight, float finalWeight) {
		int otherIdCpv = symmap.putsym(otherIdSym) ;
		int otherNonIdCpv = symmap.putsym(otherNonIdSym) ;

		Fst resultFst = new Fst(oneArcFstNative(icpv, ocpv, arcWeight, finalWeight)) ;

		if (icpv == otherIdCpv || icpv == otherNonIdCpv) {
			resultFst.setContainsOther(true) ; // OTHER is not put in the sigma
		} else if (icpv > 0) {  // not other, not neg, not zero (epsilon)
			resultFst.getSigma().add(icpv) ;
		}

		if (ocpv == otherIdCpv || ocpv == otherNonIdCpv) {
			resultFst.setContainsOther(true) ; // OTHER not put in the sigma
		} else if (ocpv > 0) {	// not other,not neg, not zero (epsilon)
			resultFst.getSigma().add(ocpv) ;
		}

		if (isSapRtnConventions()) {
			String istr = symmap.getsym(icpv) ;
			String ostr = symmap.getsym(ocpv) ;
			if ( 	(istr.length() > 1 && istr.startsWith("$"))
				||	(ostr.length() > 1 && ostr.startsWith("$"))) {
				resultFst.setIsRtn(true) ;
			}
		}
		return resultFst ;
	}

	// this is the most used (abbreviated) version of oneArcFst()

	public Fst OneArcFst(int cpv) {
		// semiring generalization point
		return OneArcFst(cpv, cpv, (float) 0.0, (float) 0.0) ;
	}

	public Fst OneArcFst(int icpv, int ocpv) {
		// semiring generalization point
		return OneArcFst(icpv, ocpv, (float) 0.0, (float) 0.0) ;
	}

	public Fst OneArcFst(String sym) {
		int cpv = symmap.putsym(sym) ;
		return OneArcFst(cpv, cpv) ;
	}

	public Fst OneArcFst(String isym, String osym) {
		return OneArcFst(symmap.putsym(isym), symmap.putsym(osym)) ;
	}

	public Fst OneArcFst(String isym, int ocpv) {
		return OneArcFst(symmap.putsym(isym), ocpv) ;
	}

	public Fst OneArcFst(int icpv, String osym) {
		return OneArcFst(icpv, symmap.putsym(osym)) ;
	}

	public Fst CharRangeUnionFst(int cpvFirst, int cpvLast) {
		return new Fst(charRangeUnionFstNative(cpvFirst, cpvLast)) ;
	}

	// handles Strings without MCSs
	public Fst FstFromString(String str) {
		Fst resultFst = new Fst() ;
		HashSet<Integer> sigma = resultFst.getSigma() ;

		// Handle BMP and supplementary characters.
		// Because the input is a standard Java String, 
		// not a regular expression, it cannot contain
		// Kleene multichar symbols.
		int len = str.codePointCount(0, str.length()) ;

		// the int[] is needed by the native function
		// oneStringFstNative()
		int[] intArray = new int[len] ;

		int cpv ;
		int index = 0 ;

		// iterate through the code point values, not just code units
		UCharacterIterator iter = UCharacterIterator.getInstance(str) ;

		while ((cpv = iter.nextCodePoint()) != UCharacterIterator.DONE) {
			// add the cpv to the sigma of the new network
			sigma.add(cpv) ;

			// Make sure that symbol is in the symbol map
			// N.B. in this case the String cannot contain
			// multichar symbols
			symmap.putsym(String.valueOf(cpv)) ;

			intArray[index++] = cpv ;
		}
		// there should be no possibility of OTHER

		resultFst.setFstPtr(oneStringFstNative(intArray)) ;
		return resultFst ;
	}

	public Fst FstFromCpvArray(int[] cpvArray) {
		Fst resultFst = new Fst() ;
		HashSet<Integer> sigma = resultFst.getSigma() ;

		for (int i = 0; i < cpvArray.length ; i++) {
			sigma.add(cpvArray[i]) ;
			// the symbol map should already have these values
		}
		// there should be no possibility of OTHER

		resultFst.setFstPtr(oneStringFstNative(cpvArray)) ;
		return resultFst ;
	}

	public Fst EmbeddedRtn(Fst baseFst, ArrayList<String> dependencies, 
									String subnetworksPrefix) {
		// currently subnetworksPrefix is "__SUBNETWORKS"
		Fst subnetworksFst = OneArcFst(symmap.putsym(subnetworksPrefix)) ;

		String prefix = "__" ;	// for OpenFstRtnConventions
		if (isSapRtnConventions()) {
			prefix = "" ;
		} 
		
		int last = dependencies.size() - 1 ;
		boolean optimize ;
		String dep ;
		Fst subFst ;

		Fst resultFst = baseFst ;
		if (baseFst.getFromSymtab()) {
			resultFst = CopyFst(baseFst) ;
		}

		for (int i = 0; i <= last; i++) {
			dep = dependencies.get(i) ;

			subFst = (Fst) env.get(dep) ;

			// to handle OTHER
			resultFst = promoteSigmaOther(resultFst, subFst) ;
			subFst = promoteSigmaOther(subFst, resultFst) ;

			System.out.println("subnetworksFst from symtab: " +
			subnetworksFst.getFromSymtab()) ;  

			Fst prefixedSub = Concat3Fsts(	
										subnetworksFst,
										OneArcFst(symmap.putsym(prefix + dep)),
										subFst
									) ;
			FstDump(prefixedSub) ;

			optimize = (i == last) ? true : false ;
			resultFst = UnionIntoFirstInPlace(resultFst, prefixedSub, optimize) ;
		}
		return resultFst ;
	}
	
	
	// ************************************************************
	// 		Functions testing Fsts
	// ************************************************************
	
	public boolean IsEmptyStringLanguage(Fst fst) {
		if (numStatesNative(fst.getFstPtr()) == 1 
			&& numArcsNative(fst.getFstPtr()) == 0 
			&& fst.getSigma().size() == 0) 
		{
			return true ;
		} else {
			return false ;
		}
	}

	public boolean ContainsEmptyString(Fst fst) {
		if (containsEmptyStringNative(fst.getFstPtr())) {
			return true ;
		} else {
			return false ;
		}
	}

	// ************************************************************
	// 		Main Finite-State Functions Called from the Interpreter
	// ************************************************************

	// semiring generalization point (float weight)
	public void AddArc(Fst a, int src, int i, int o, float weight, int dest) {
		addArcNative(a.getFstPtr(), src, i, o, weight, dest) ;
	}

	// semiring generalization point (weight)
	public void AddArcNeutralWeight(Fst a, int src, int i, int o, int dest) {
		addArcNeutralWeightNative(a.getFstPtr(), src, i, o, dest) ;
	}

	// add cases to a network (i.e. relax the existing casing)
	public void AddCaseInPlace(Fst a, boolean all,
										boolean add_uc, // add uc for existing lc
										boolean add_lc, // add lc for existing uc
										boolean input, 	// input/upper side
										boolean output) {
		int[] symbolsAdded = addCaseNative(a.getFstPtr(), all, add_uc, add_lc, input, output) ;
		// symbolsAdded, an array of int code point values, should represent defined
		// Unicode symbols--add them to the symmap here
		for (int i = 0; i < symbolsAdded.length; i++) {
			symmap.putsym(stringFromCpv(symbolsAdded[i])) ;
		}

		promoteSigmaOther(a, symbolsAdded) ;
		CorrectSigmaOtherInPlace(a) ;
		OptimizeInPlace(a) ;
	}

	// add diacritic insensitivity to a network
	public void AddDiacInPlace(Fst a, boolean input, boolean output) {
		int[] symbolsAdded = addDiacNative(a.getFstPtr(), input, output) ;
		promoteSigmaOther(a, symbolsAdded) ;
		CorrectSigmaOtherInPlace(a) ;
		OptimizeInPlace(a) ;
	}

	public void AddOtherId(SymMap symmap) {
		symmap.putsym(otherIdSym) ;
	}

	public void AddOtherNonId(SymMap symmap) {
		symmap.putsym(otherNonIdSym) ;
	}

	public void AddStates(Fst a, int numStates) {
		// used when building a network from XML
		addStatesNative(a.getFstPtr(), numStates) ;
	}

	public int AddStatesAndArcsInPlace(Fst a, Fst b) {
		// add to Fst a the states and arcs of Fst b
		// return the _new_ start state number of b
		a = promoteSigmaOther(a, b) ;
		b = promoteSigmaOther(b, a) ;

		// addStatesAndArcsNative is a modification of the 
		//	Concat algorithm
		int newStartStateB = addStatesAndArcsNative(a.getFstPtr(), b.getFstPtr()) ;
		addSigmaOther(a, b) ;
		return newStartStateB ;
	}

	public Fst ApplyToOneString(Fst testFst, int[] cpvArray, int direction) {
		checker.ApplyToOneString(testFst) ;  // cannot be an SAP RTN
		// direction 0 means generate
		// direction 1 means analyze

		// Call native function oneStringFstNative() to create a one-string Fst, 
		// using the code point values passed in an int array
		Fst inputFst = FstFromCpvArray(cpvArray) ;

		// For this testing, if testFst contains OTHER, and even if it is
		// anonymous (not from the symbol table), then a copy needs to be
		// made.  Otherwise, the sigma (and associated arcs) of the testFst
		// will accumulate symbols from the input strings as multiple inputs
		// are tested.  KRB: review this

		if (testFst.getContainsOther()) {
			testFst = CopyFst(testFst) ;
		}

		testFst = promoteSigmaOther(testFst, inputFst) ; 
		
		// I think, the inputFst cannot contain OTHER
		// inputFst = promoteSigmaOther(inputFst, testFst) ;

		// need this?  I don't think so, because inputFst cannot
		//      contain OTHER; this fix is to allow two networks,
		//		both containing OTHER, to compose successfully
		// testFst = fixOtherBeforeCompose(testFst, outputProj) ;
		// inputFst = fixOtherBeforeCompose(inputFst, inputProj) 
		
		Fst resultFst ;

		if (direction == 0) {
			// generation
			// compose the inputFst on top of the testFst and
			// extract the output projection
			resultFst = Compose(inputFst, testFst) ;
			OutputProjectionInPlace(resultFst) ;  	// forces optimization
		} else {
			// analysis
			// compose the inputFst on the bottom of the testFst
			// and extract the input projection
			resultFst = Compose(testFst, inputFst) ;
			InputProjectionInPlace(resultFst) ;		// forces optimization
		}

		return resultFst ;
	}

	public String ArcType(Fst a) {
		return getArcTypeNative(a.getFstPtr()) ;
	}

	public void CloseSigmaInPlace(Fst a, Fst b) {
		// promote the sigma of a, relative to b
		a = promoteSigmaOther(a, b) ;

		deleteOtherArcsInPlaceNative(a.getFstPtr(), symmap.getint(otherIdSym), symmap.getint(otherNonIdSym)) ;
		OptimizeInPlace(a) ;
	}

	public Fst Complement(Fst a) {
		// compute as (.* - a)
		// restrictions will be checked by the Difference operation
		Fst resultFst = Difference(UniversalLanguageFst(), a) ;
		return resultFst ;
	}

	public Fst Compose(Fst a, Fst b) {
		checker.Compose(a, b) ;

		// static constants (used in call to fixOtherBeforeCompose()
		final boolean inputProj = true ;
		final boolean outputProj = false ;

		a = promoteSigmaOther(a, b) ;
		b = promoteSigmaOther(b, a) ;

		// N.B. promoteSigmaOther() is called by a lot of functions besides
		// Compose()

		// KRB:  consider making a modification of promoteSigmaOther() with
		// a third argument to do what fixOtherBeforeCompose() does.

		// Modify OTHER, if necessary, to allow composition
		// using the standard OpenFst Compose() algorithm
		// (on the intermediate levels, OTHER_ID and OTHER_NONID
		// need to be reduced to the same int).

		// if OTHER_ID labels are on the output/lower side, change
		// them to OTHER_NONID
		a = fixOtherBeforeCompose(a, outputProj) ;

		// if OTHER_ID labels are on the input/upper side, change
		// them to OTHER_NONID
		b = fixOtherBeforeCompose(b, inputProj) ;

		// The required sorting of the Input arcs of the second arg 
		// (or the output arcs of the first arg) is now done in 
		// C++ in the native function.
		// Note that the native Compose() function is non-destructive and 
		//		returns a ptr to a completely new OpenFst (C++/Native) object.

		Fst resultFst = new Fst(composeNative(a.getFstPtr(), 
										b.getFstPtr())) ;
		// native
		fixOtherAfterComposeNative(resultFst.getFstPtr(),
			symmap.getint(otherIdSym), symmap.getint(otherNonIdSym)) ;

		addSigma(resultFst, a) ;
		addSigma(resultFst, b) ;

		CorrectSigmaOtherInPlace(resultFst) ;
		ConnectInPlace(resultFst) ;	// calls OptimizeInPlace

		return resultFst ;
	}

	public Fst Compose3Fsts(Fst one, Fst two, Fst three) {
		return Compose(
					Compose(one, two),
					three) ;
	}

	public Fst Concat(Fst a, Fst b) {
		return Concat(a, b, true) ;
		// true = default optimization of the result
	}

	public Fst Concat(Fst a, Fst b, boolean optimize) {
		checker.Concat(a, b) ;

		// OpenFst's Concat(first, second) is 
		//     destructive of the first arg, copy if necessary
		Fst resultFst = a ;
		if (a.getFromSymtab()) {
			resultFst = CopyFst(a) ;
		}

		resultFst = promoteSigmaOther(resultFst, b) ;
		b = promoteSigmaOther(b, resultFst) ;

		concatIntoFirstNative(resultFst.getFstPtr(), b.getFstPtr()) ;
		addSigmaOther(resultFst, b) ;
		if (optimize) {
			OptimizeInPlace(resultFst) ;
		}

		return resultFst ;
	}

	public Fst ConcatIntoFirstInPlace(Fst a, Fst b) {
		// never copies the first argument
		return ConcatIntoFirstInPlace(a, b, true) ;
	}

	public Fst ConcatIntoFirstInPlace(Fst a, Fst b, boolean optimize) {
		checker.Concat(a, b) ;

		// never copies the first argument

		a = promoteSigmaOther(a, b) ;
		b = promoteSigmaOther(b, a) ;

		concatIntoFirstNative(a.getFstPtr(), b.getFstPtr()) ;
		addSigmaOther(a, b) ;
		if (optimize) {
			OptimizeInPlace(a) ;
		}

		return a ;
	}

	public Fst Concat3Fsts(Fst one, Fst two, Fst three) {
		// native concatenation is destructive of the first argument
		Fst resultFst = EmptyStringLanguageFst() ;
		resultFst = ConcatIntoFirstInPlace(resultFst, one, false) ;
		resultFst = ConcatIntoFirstInPlace(resultFst, two, false) ;
		resultFst = ConcatIntoFirstInPlace(resultFst, three, true) ;
		return resultFst ;
	}

	public Fst Concat4Fsts(Fst one, Fst two, Fst three, Fst four) {
		// native concatenation is destructive of the first argument
		Fst resultFst = EmptyStringLanguageFst() ;
		resultFst = ConcatIntoFirstInPlace(resultFst, one, false) ;
		resultFst = ConcatIntoFirstInPlace(resultFst, two, false) ;
		resultFst = ConcatIntoFirstInPlace(resultFst, three, false) ;
		resultFst = ConcatIntoFirstInPlace(resultFst, four, true) ;
		return resultFst ;
	}

	Fst Concat5Fsts(Fst one, Fst two, Fst three, Fst four, Fst five) {
		// native concatenation is destructive of the first argument
		Fst resultFst = EmptyStringLanguageFst() ;
		resultFst = ConcatIntoFirstInPlace(resultFst, one, false) ;
		resultFst = ConcatIntoFirstInPlace(resultFst, two, false) ;
		resultFst = ConcatIntoFirstInPlace(resultFst, three, false) ;
		resultFst = ConcatIntoFirstInPlace(resultFst, four, false) ;
		resultFst = ConcatIntoFirstInPlace(resultFst, five, true) ;
		return resultFst ;
	}

	Fst Concat7Fsts(Fst one, Fst two, Fst three, Fst four, Fst five, Fst six, Fst
	seven) {
		// native concatenation is destructive of the first argument
		Fst resultFst = EmptyStringLanguageFst() ;
		resultFst = ConcatIntoFirstInPlace(resultFst, one, false) ;
		resultFst = ConcatIntoFirstInPlace(resultFst, two, false) ;
		resultFst = ConcatIntoFirstInPlace(resultFst, three, false) ;
		resultFst = ConcatIntoFirstInPlace(resultFst, four, false) ;
		resultFst = ConcatIntoFirstInPlace(resultFst, five, false) ;
		resultFst = ConcatIntoFirstInPlace(resultFst, six, false) ;
		resultFst = ConcatIntoFirstInPlace(resultFst, seven, true) ;
		return resultFst ;
	}

	public void ConnectInPlace(Fst a) {
		connectInPlaceNative(a.getFstPtr()) ;
		OptimizeInPlace(a) ;
	}

	public void ConvertCaseInPlace(Fst a, boolean all,
										boolean to_uc, 	// convert lc to uc
										boolean to_lc,	// convert uc to lc
										boolean input, 
										boolean output) {
		int[] symbolsAdded = convertCaseNative(a.getFstPtr(), all, to_uc, 
												to_lc, input, output) ;
		// symbolsAdded, an array of int code point values, should represent defined
		// Unicode symbols--add them to the symmap here
		for (int i = 0; i < symbolsAdded.length; i++) {
			symmap.putsym(stringFromCpv(symbolsAdded[i])) ;
		}

		promoteSigmaOther(a, symbolsAdded) ;
		CorrectSigmaOtherInPlace(a) ;
		OptimizeInPlace(a) ;
		
	}

	// copy a whole Fst, with sigma and OTHER
	public Fst CopyFst(Fst fst) {
		Fst newFst = new Fst(copyFstNative(fst.getFstPtr()), 
			false,  // not from the symbol table
			fst.getContainsOther(), 
			fst.getSigma()) ;
		return newFst ;
	}

	// In the interpreter, called only from ASTregexp;
	// for OTHER, still needs work to detect cases where
	// a symbol c always acts like OTHER and can be conflated with
	// OTHER
	void CorrectSigmaOtherInPlace(Fst fst) {
		// native function to retrieve all actual labels,
		// including OTHER_ID and OTHER_NONID
		int[] sigmaArray = GetLabels(fst) ;

		int other_id = symmap.getint(otherIdSym) ;
		int other_nonid = symmap.getint(otherNonIdSym) ;

		HashSet<Integer> hs = fst.getSigma() ;

		fst.setContainsOther(false) ;  // may be reset to true below
		fst.setIsRtn(false) ;		// may be reset to true below

		int val ;
		for (int j = 0; j < sigmaArray.length; j++) {
			val = sigmaArray[j] ;
			if (val == other_id || val == other_nonid) {
				fst.setContainsOther(true) ;
				break ;
			}
		}
		if (!fst.getContainsOther()) {
			hs.clear() ;
		}
		int cpv ;
		boolean sapRtnConv = isSapRtnConventions() ;

		for (int i = 0; i < sigmaArray.length; i++) {
			cpv = sigmaArray[i] ;
			if (cpv != other_id && cpv != other_nonid && cpv > 0) {
				// add it to the sigma
				hs.add(cpv) ;

				// make sure that it's in the symmap
				if (symmap.getsym(cpv) == null) {
					symmap.putsym(stringFromCpv(cpv)) ;
				}

				if (sapRtnConv) {
					String sym = symmap.getsym(cpv) ;
					if (sym.length() > 1 && sym.startsWith("$")) {
						fst.setIsRtn(true) ;
						// don't need to set it more than once
						sapRtnConv = false ;
					}
				}
			}
		}
	}

	public static void CppDelete(long aptr) {
		// called from jdelete method in the interpreter,
		// which is called from the finalize() method in
		// Fst.java, does self-deletion of OpenFst networks
		// when they are garbage-collected
		cppDeleteNative(aptr) ;
	}

	public Fst Crossproduct(Fst a, Fst b) {
		checker.Crossproduct(a, b) ;  // semiring and acceptor check

		Fst resultFst ;

		// special handling for superficial "mapping" of RTNs to epsilon,
		//		allow only '$sub':"" and "":'$sub',
		//		and disallow '$sub':z  and z:'$sub'

		if (isSapRtnConventions() && (a.getIsRtn() || b.getIsRtn())) {
			if (a.getIsRtn()) { 
				resultFst = a ;
				if (IsEmptyStringLanguage(b)) {
					if (a.getFromSymtab()) {
						resultFst = CopyFst(a) ;
					}
					changeOutputToEpsilonInPlaceNative(resultFst.getFstPtr(), 
													symmap.getint(otherIdSym), 
													symmap.getint(otherNonIdSym)) ;
				} else if (isTrivialRtnRefCrossproduct(a, b)) {
					System.out.println("TrivialRtnRefXProduct") ;  
					// i.e. if user wrote something like '$foo':'$foo'
					// allow it (avoiding the crossproduct operation)
					resultFst = a ;
				} else {
					throw new FstPropertyException("In SapRtnConventions, crossproduct A:B is illegal when A contains references to subnets, and B is not the empty string language.") ;
				}
			} else {	// b is the RTN
				resultFst = b ;
				if (IsEmptyStringLanguage(a)) {
					if (b.getFromSymtab()) {
						resultFst = CopyFst(b) ;
					}
					changeInputToEpsilonInPlaceNative(resultFst.getFstPtr(),
													symmap.getint(otherIdSym) ,
													symmap.getint(otherNonIdSym)) ;
				} else {
					throw new FstPropertyException("In SapRtnConventions, crossproduct A:B is illegal when B contains references to subnets, and A is not the empty string language.") ;
				}
			}
		} else {
			// Crossproduct is computed as
			// a _o_ UniversalRelation _o_ b
			resultFst = Compose3Fsts(a, UniversalRelationFst(), b) ;
		}
		return resultFst ;
	}

	public Fst Determinize(Fst a) {
		// non-destructive
		Fst resultFst = new Fst(determinizeNative(a.getFstPtr())) ;
		addSigmaOther(resultFst, a) ;
		OptimizeInPlace(resultFst) ;
		return resultFst ;
	}

	public void DeterminizeInPlace(Fst a) {
		determinizeInPlaceNative(a.getFstPtr()) ;
	}

	public Fst Difference(Fst a, Fst b) {
		checker.Difference(a, b) ;

		a = promoteSigmaOther(a, b) ;
		b = promoteSigmaOther(b, a) ;

		// The required sorting of Input arcs on the second arg (or the
		// output arcs of the first arg) is now done in C++ in the 
		// native function
		Fst resultFst = new Fst(differenceNative(a.getFstPtr(), b.getFstPtr())) ; 

		// N.B. difference can leave the result empty
		if (!isEmptyLanguageNative(resultFst.getFstPtr())) {
			addSigmaOther(resultFst, a) ;
		}
		CorrectSigmaOtherInPlace(resultFst) ;

		ConnectInPlace(resultFst) ;	// calls OptimizeInPlace

		return resultFst ;  
	}

/*
	public boolean Equivalent(Fst a, Fst b, Double delta) {
		checker.Equivalent(a, b) ;  // must be acceptors

		return isEquivalentNative(a.getFstPtr(), b.getFstPtr(), delta) ;
	}
*/

	public int Epsilon = 0 ;// in OpenFst, 0 wired in as epsilon

	public Fst ExpandRtn(Fst baseFst, int baseFstInt, ArrayList<String> subnetReferences) {
		// Loop through the list of dependencies (the names
		// of all the networks in the implied grammar) collecting
		// information to be sent to a native function that checks
		// for (illegal) cyclic dependencies

		// create long[] arrays for easy passing to native C++
		int[]  symInts = new int[subnetReferences.size() + 1] ;
		long[] netPtrs = new long[subnetReferences.size() + 1] ;

		String subnetName ;
		Fst subFst ;

		int arraySize = subnetReferences.size() ;
		for (int i = 0; i < arraySize; i++) {
			subnetName = subnetReferences.get(i) ;	// a String like "$foo"
			subFst = (Fst) env.get(subnetName) ;	// look it up
			symInts[i] = symmap.getint("__" + subnetName) ;
			netPtrs[i] = (long) subFst.getFstPtr() ;
		}
		// now add the information for the baseFst
		symInts[arraySize] = baseFstInt ;

		netPtrs[arraySize] = (long) baseFst.getFstPtr() ;

		int cycRet = HasCyclicDependencies(baseFstInt, symInts, netPtrs) ; 

		if (cycRet == -1) {
			throw new KleeneInterpreterException("Failed RTN expansion, problem allocating storage in HasCyclicDependencies.") ;
		} else if (cycRet == 1) {
			throw new KleeneInterpreterException("Failed RTN expansion, cyclic dependencies.") ;
		}

		// else no cyclic dependencies, proceed with the expansion
		Fst resultFst = baseFst ;

		if (baseFst.getFromSymtab()) {
			resultFst = CopyFst(baseFst) ;
			netPtrs[arraySize] = resultFst.getFstPtr() ;
		}

		for (int i = 0; i < arraySize; i++) {
			subnetName = subnetReferences.get(i) ;	// a String like "$foo"
			subFst = (Fst) env.get(subnetName) ;	// look it up

			// to handle OTHER
			resultFst = promoteSigmaOther(resultFst, subFst) ;
			subFst = promoteSigmaOther(subFst, resultFst) ;

			addSigmaOther(resultFst, subFst) ;
		}

		resultFst.setFstPtr(expandRtnNative(baseFstInt, symInts, netPtrs)) ;

		// strips references like __$foo; works only for
		// OpenFstRtnConventions at present
	 	stripReferencesToSubnets(resultFst.getSigma()) ;

		OptimizeInPlace(resultFst) ;
		return resultFst ;
	}

	public void FstDump(Fst a) {
		fstDumpNative(a.getFstPtr()) ;
	}

	public void Fst2dot(Fst a, FstDotWriter writer) {
		fst2dotNative(a.getFstPtr(), writer) ;
	}

	public void Fst2xml(Fst a, FstXmlWriter writer, int startPuaCpv) {
		fst2xmlNative(a.getFstPtr(), writer, startPuaCpv) ;
	}

	public void Fst2xmlStateOriented(Fst a, FstXmlWriterStateOriented writer, int startPuaCpv) {
		fst2xmlStateOrientedNative(a.getFstPtr(), writer, startPuaCpv) ;
	}

	public int[] GetLabels(Fst a) {
		return getLabelsNative(a.getFstPtr()) ;
	}

	public int[] GetOutputLabels(Fst a) {
		return getOutputLabelsNative(a.getFstPtr()) ;
	}

	public String GetShortFstInfo(Fst a) {
		return getShortFstInfoNative(a.getFstPtr()) ;
	}

	public int GetSingleArcLabel(Fst a) {
		// optimize first, then check the restrictions
		OptimizeInPlaceForce(a) ;
		checker.GetSingleArcLabel(a) ;

		return getSingleArcLabelNative(a.getFstPtr()) ;
	}

	public String GetSingleString(Fst a, String excMsg) {
		// optimize first, then check the restrictions
		OptimizeInPlaceForce(a) ;
		checker.GetSingleString(a, excMsg) ;  
		// make sure that there is only 1 path

		// return an array of code point values
		int[] cpvArray = getSingleStringNative(a.getFstPtr()) ;

		StringBuilder sb = new StringBuilder() ;
		for (int i = 0; i < cpvArray.length; i++) {
			sb.append(symmap.getsym(cpvArray[i])) ;
		}
		return sb.toString() ;
	}
		
	private int HasCyclicDependencies(int baseFstInt, int[] symInts, long[] netPtrs) {
		return hasCyclicDependenciesNative(baseFstInt, symInts, netPtrs) ;
	}

	// see also OutputProjectionInPlace()
	// forces optimization, used to extract a projection
	// after application (e.g. in 'test' in the GUI)
	public void InputProjectionInPlaceOptimize(Fst a) {
		inputProjectionInPlaceNative(a.getFstPtr()) ;
		CorrectSigmaOtherInPlace(a) ;
		OptimizeInPlaceForce(a) ;
	}

	public Fst InputProjection(Fst a) {
		// not destructive from the Kleene point of view;
		// but need to copy the argument
		// only if it's from the symbol table
		if (a.getFromSymtab()) {
			a = CopyFst(a) ;
		}

		inputProjectionFixOtherInPlaceNative(a.getFstPtr(), 
			symmap.putsym(otherIdSym), symmap.putsym(otherNonIdSym)) ;
		CorrectSigmaOtherInPlace(a) ;

		OptimizeInPlace(a) ;
		return a ;
	}

	public void InputProjectionInPlace(Fst a) {
		inputProjectionFixOtherInPlaceNative(a.getFstPtr(), 
			symmap.putsym(otherIdSym), symmap.putsym(otherNonIdSym)) ;
		CorrectSigmaOtherInPlace(a) ;

		OptimizeInPlace(a) ;
	}

	public Fst Intersect(Fst a, Fst b) {
		checker.Intersect(a, b) ;

		a = promoteSigmaOther(a, b) ;
		b = promoteSigmaOther(b, a) ;

		// The required sorting of the Input arcs of the second arg 
		// (or the output arcs of the first arg) is now done in 
		// C++ in the native function.
		// Note that intersect() is non-destructive and returns a 
		//		ptr to a completely new OpenFst (C++/Native) object.

		Fst resultFst = new Fst(intersectNative(a.getFstPtr(), b.getFstPtr())) ;

		// N.B. intersect can leave the result empty
		if (!isEmptyLanguageNative(resultFst.getFstPtr())) {
			addSigmaOther(resultFst, a) ;
			// not needed
			// addSigmaOther(resultFst, b)
		}
		CorrectSigmaOtherInPlace(resultFst) ;
		
		ConnectInPlace(resultFst) ;	// calls OptimizeInPlace

		return resultFst ;
	}

	public Fst Intersect3Fsts(Fst one, Fst two, Fst three) {
		Fst resultFst = Intersect(
								Intersect(one, two),
								three
							) ;
		return resultFst ;
	}

	public void InvertInPlace(Fst a) {
		invertInPlaceNative(a.getFstPtr()) ;
	}


	public boolean IsAcceptor(Fst a) {
		// isAcceptorNative is True iff the Fst looks
		// like an acceptor to OpenFst, i.e. all the
		// labels look like x:x
		// Note that if the Fst contains OTHER_NONID:OTHER_NONID
		// then it will look like an acceptor to OpenFst but
		// it's semantically a transducer.
		return isAcceptorNative(a.getFstPtr()) ;
	}
	public boolean IsSemanticAcceptor(Fst a) {
		return isSemanticAcceptorNative(a.getFstPtr(), symmap.getint(otherNonIdSym)) ;
	}
	public boolean IsWeighted(Fst a) {
		return isWeightedNative(a.getFstPtr()) ;
	}
	public boolean IsUnweighted(Fst a) {
		return isUnweightedNative(a.getFstPtr()) ;
	}
	public boolean IsEpsilonFree(Fst a) {
		return isEpsilonFreeNative(a.getFstPtr()) ;
	}
	public boolean IsIDeterministic(Fst a) {
		return isIDeterministicNative(a.getFstPtr()) ;
	}
	public boolean IsODeterministic(Fst a) {
		return isODeterministicNative(a.getFstPtr()) ;
	}
	public boolean IsEmptyLanguage(Fst a) {
		return isEmptyLanguageNative(a.getFstPtr()) ;
	}
	public boolean IsCyclic(Fst a) {
		return isCyclicNative(a.getFstPtr()) ;
	}
	public boolean IsAcyclic(Fst a) {
		return isAcyclicNative(a.getFstPtr()) ;
	}
	public boolean IsUBounded(Fst a) {
		return isUBoundedNative(a.getFstPtr()) ;
	}
	public boolean IsLBounded(Fst a) {
		return isLBoundedNative(a.getFstPtr()) ;
	}
	public boolean IsString(Fst a) {
		// encodes a language of 1 string
		return isStringNative(a.getFstPtr()) ;
	}
	public boolean IsNotString(Fst a) {
		return isNotStringNative(a.getFstPtr()) ;
	}

	public Fst Iterate(Fst a, long low, long high) {
		// a high of -1 means unlimited, from syntax  x{2,}
		long tempHigh = (high == -1L) ? low : high ;
		checker.Iterate(a, low, tempHigh) ;

		Fst resultFst = new Fst(iterateLowHighNative(a.getFstPtr(), low, high)) ;
		addSigmaOther(resultFst, a) ;
		OptimizeInPlace(resultFst) ;

		return resultFst ;
	}

	public void Iterate4mcs(Fst a, TranslitTokenizerBuilder ttb, int startPuaCpv) {
		iterate4mcsNative(a.getFstPtr(), ttb, startPuaCpv) ;
	}

	public Fst KleenePlus(Fst a) {
		Fst resultFst = a ;
		if (a.getFromSymtab()) {
			resultFst = CopyFst(a) ;
		}

		kleenePlusInPlaceNative(resultFst.getFstPtr()) ;
		OptimizeInPlace(resultFst) ;
		return resultFst ;
	}
	
	public Fst KleeneStar(Fst a) {
		Fst resultFst = a ;
		if (a.getFromSymtab()) {
			resultFst = CopyFst(a) ;
		}

		kleeneStarInPlaceNative(resultFst.getFstPtr()) ;
		OptimizeInPlace(resultFst) ;
		return resultFst ;
	}

	public void ListAllStrings(Fst a, int proj, StringLister lister) {
		listAllStringsNative(a.getFstPtr(), proj, lister) ;
	}

	public void MinimizeInPlace(Fst a) {
		checker.MinimizeInPlace(a) ;

		minimizeInPlaceNative(a.getFstPtr()) ;
	}

	void OptimizeInPlace(Fst fst) {
		boolean determinize = isTrue(env.get("#KLEENEdeterminize")) ;
		boolean minimize    = isTrue(env.get("#KLEENEminimize")) ;
		boolean rmepsilon   = isTrue(env.get("#KLEENErmepsilon")) ;

		if (determinize || minimize || rmepsilon) {
			optimizeInPlaceNative(fst.getFstPtr(), determinize, minimize, rmepsilon) ;
		} 
	}

	// called when user enters
	// optimize $a, $b, $c ;
	// or the functions $^optimize($a) or $^optimize!($a)
	// forces the optimization even if the user has turned
	// off #KLEENEdeterminize, #KLEENEminimize, #KLEENErmepsilon
	public void OptimizeInPlaceForce(Fst a) {
		optimizeInPlaceNative(a.getFstPtr(), true, true, true) ;
	}

	public int NumArcs(Fst a) {
		return numArcsNative(a.getFstPtr()) ;
	}

	public long NumPaths(Fst a) {
		return numPathsNative(a.getFstPtr()) ;
	}

	public int NumStates(Fst a) {
		return numStatesNative(a.getFstPtr()) ;
	}


	// see also InputProjectionInPlace()
	// forces optimization, used to extract a projection
	// after application (e.g. 'test' in the GUI)
	public void OutputProjectionInPlaceOptimize(Fst a) {
		outputProjectionInPlaceNative(a.getFstPtr()) ;
		CorrectSigmaOtherInPlace(a) ;
		OptimizeInPlaceForce(a) ;
	}

	public Fst OutputProjection(Fst a) {
		// not destructive from the Kleene point of view;
		// but need to copy the argument
		// only if it's from the symbol table
		if (a.getFromSymtab()) {
			a = CopyFst(a) ;
		}

		outputProjectionFixOtherInPlaceNative(a.getFstPtr(), 
			symmap.putsym(otherIdSym), symmap.putsym(otherNonIdSym)) ;
		CorrectSigmaOtherInPlace(a) ;

		OptimizeInPlace(a) ;
		return a ;
	}

	public void OutputProjectionInPlace(Fst a) {
		outputProjectionFixOtherInPlaceNative(a.getFstPtr(), 
			symmap.putsym(otherIdSym), symmap.putsym(otherNonIdSym)) ;
		CorrectSigmaOtherInPlace(a) ;

		OptimizeInPlace(a) ;
	}

	public Fst Reverse(Fst a) {
		// the OpenFst Reverse() is not destructive
		Fst resultFst = new Fst(reverseNative(a.getFstPtr())) ;
		addSigmaOther(resultFst, a) ;
		OptimizeInPlace(resultFst) ;
		return resultFst ;
	}

	public void RmEpsilonInPlace(Fst a) {
		rmEpsilonInPlaceNative(a.getFstPtr()) ;
	}
		
	// take whole Fst, modify in place to have neutral weights
	// semiring generalization point
	public void RmWeightDestFst(Fst fst) {
		// do not make a copy, just work in place
		rmWeightDestFstNative(fst.getFstPtr()) ;
		OptimizeInPlace(fst) ;
	}

	// semiring generalization point
	// non-destructive rmWeightFst
	// return new network (if necessary) with neutral weights,
	// (preserve sigma and OTHER); else modify in place.
	// rmWeightFstNative returns a new OpenFst Fst
	public Fst RmWeightFst(Fst fst) {
		Fst resultFst = fst ;
		if (fst.getFromSymtab()) {
			// native rmWeightFstNative() calls a non-destructive
			// version of Map(), doesn't modify the input, builds
			// and returns a new network
			resultFst = new Fst(rmWeightFstNative(fst.getFstPtr()),
				false, // not from the symbol table
				fst.getContainsOther(),
				fst.getSigma()) ;
		} else {
			// remove the weights in place.
			// native rmWeightDestFstNative() calls a destructive
			//	version of Map() that modifies the OpenFst network
			//	in place
			rmWeightDestFstNative(resultFst.getFstPtr()) ;
		}
		OptimizeInPlace(resultFst) ;
		return resultFst ;
	}

	public void RrGrammarLinkInPlace(Fst a, ArrayList<Integer> keys, 
											ArrayList<Integer> vals) {
		// For simplicity in passing values to rrGrammarLinkNative, convert
		// keys and vals to simple int[] arrays
		int ikeys[] = new int[keys.size()] ;
		for (int j = 0; j < keys.size(); j++) {
			ikeys[j] = keys.get(j).intValue() ;
		}
		int ivals[] = new int[vals.size()] ;
		for (int j = 0; j < vals.size(); j++) {
			ivals[j] = vals.get(j).intValue() ;
		}

		// rrGrammarLinkNative is a native function (see kleeneopenfst.cc)
		// a return of 0 indicates an error
		long res = rrGrammarLinkNative(a.getFstPtr(), ikeys, ivals) ;
		// this native func will loop through all the states, then
		//   through all the arcs for each state, searching for
		//   arcs with negative labels: (replace the labels with eps,
		//   replace the nextstate with the corresponding ivals value);
		//   will typically leave some states unconnected (old final states
		//	 that are no longer accessible)

		if (res == 0L) {
			throw new RightLinearGrammarException("Problem in rrGrammarLink; couldn't allocate space for IntArrayElements") ;
		}

		ConnectInPlace(a) ;
	}

	// semiring generalization point (weight)
	public void SetFinal(Fst a, int state, float weight) {
		setFinalNative(a.getFstPtr(), state, weight) ;
	}

	// semiring generalization point (weight)
	public void SetFinalNeutralWeight(Fst a, int state) {
		setFinalNeutralWeightNative(a.getFstPtr(), state) ;
	}

	public void SetStart(Fst a, int start) {
		// used when creating an Fst from XML
		setStartNative(a.getFstPtr(), start) ;
	}

	public Fst ShortestPath(Fst a, int nshortest) {
		System.out.println("Call to wrapper's ShortestPath") ;
		System.out.println("isSapRtnConventions: " + isSapRtnConventions()) ;
		System.out.println("getIsRtn: " + a.getIsRtn()) ;

		checker.ShortestPath(a) ;

		Fst resultFst = new Fst(shortestPathNative(a.getFstPtr(), nshortest)) ;
		addSigma(resultFst, a) ;
		CorrectSigmaOtherInPlace(resultFst) ;
		return resultFst ;
	}

	public int StartState(Fst a) {
		return startStateNative(a.getFstPtr()) ;
	}

	public void SubstLabelInPlace(Fst fst, int orig, int repl) {
		if (fst.getSigma().contains(orig)) {
			substLabelInPlaceNative(fst.getFstPtr(), orig, repl) ;

			if (!fst.getContainsOther()) {
				// remove the orig symbol from the sigma
				fst.getSigma().remove(orig) ;
				// add the repl symbol to the sigma (except for epsilon)
				if (repl != Epsilon) {
					fst.getSigma().add(repl) ;
				}
			} else // contains OTHER
				if (repl != Epsilon) {
					HashSet<Integer> sigmaB = new HashSet<Integer>() ;
					sigmaB.add(repl) ;

					fst = promoteSigmaOther(fst, sigmaB, false) ; // false = no copy
			}
			OptimizeInPlace(fst) ;
		}
		// else there's nothing to do
	}

	public Fst SymbolComplement(Fst a) {
		// . - a
		// a straightforward difference--all checking will
		// be done by Difference()
		return Difference(SigmaFst(), a) ;
	}

	public void SynchronizeAltRuleInPlace(Fst a, int ruleRightAngleSymVal, 
									int hardEpsilonSymVal) {
		synchronizeAltRuleInPlaceNative(a.getFstPtr(), 
									ruleRightAngleSymVal,
									hardEpsilonSymVal,
									symmap.getint(otherIdSym),
									symmap.getint(otherNonIdSym)) ;
	}

	public void SynchronizeInPlace(Fst a) {
		checker.SynchronizeInPlace(a) ;
		synchronizeInPlaceNative(a.getFstPtr()) ;
	}

	public Fst RandGen(Fst fst, long npathval, long max_lengthval) {
		// cf.  Difference (we're getting back a subset of the orig Fst)

		Fst resultFst = new Fst(randGenNative(fst.getFstPtr(), npathval, max_lengthval)) ;
		if (!isEmptyLanguageNative(resultFst.getFstPtr())) {
			addSigmaOther(resultFst, fst) ;
		}
		CorrectSigmaOtherInPlace(resultFst) ;
		ConnectInPlace(resultFst) ;	// calls Optimize
		return resultFst ;
	}


	// Calls to Union() are non-destructive, see also UnionIntoFirst()
	// 	which is destructive of the first argument
	public Fst Union(Fst a, Fst b) {
		// checking will be done in the following call
		return Union(a, b, true) ;  
		// true = default optimization of the result
	}

	// unions in real-life applications can sometimes involve
	// tens of thousands of operands--the optimize argument
	// can turn optimization off and on so that it can be
	// applied only on the final result, or only at intervals
	// determined by the calling program
	public Fst Union(Fst a, Fst b, boolean optimize) {
		checker.Union(a, b) ;

		// OpenFst's Union(first, second) is 
		//     destructive of the first arg, copy if necessary
		Fst resultFst = a ;
		if (a.getFromSymtab()) {
			resultFst = CopyFst(a) ;
		}

		resultFst = promoteSigmaOther(resultFst, b) ;
		b = promoteSigmaOther(b, resultFst) ;

		unionIntoFirstNative(resultFst.getFstPtr(), b.getFstPtr()) ;
		addSigmaOther(resultFst, b) ;
		if (optimize) {
			OptimizeInPlace(resultFst) ;
		}
		return resultFst ;
	}

	public Fst UnionIntoFirstInPlace(Fst a, Fst b) {
		return UnionIntoFirstInPlace(a, b, true) ;
	}

	public Fst UnionIntoFirstInPlace(Fst a, Fst b, boolean optimize) {
		checker.Union(a, b) ;

		// never copy the first argument

		// OpenFst's Union(first, second) is 
		//     destructive of the first arg
		a = promoteSigmaOther(a, b) ;
		b = promoteSigmaOther(b, a) ;

		unionIntoFirstNative(a.getFstPtr(), b.getFstPtr()) ;
		addSigmaOther(a, b) ;
		if (optimize) {
			OptimizeInPlace(a) ;
		}
		return a ;
	}

	public Fst Union3Fsts(Fst one, Fst two, Fst three) {
		// native union is destructive of the first argument
		Fst resultFst = EmptyLanguageFst() ; 
		resultFst = UnionIntoFirstInPlace(resultFst, one, false) ;
		resultFst = UnionIntoFirstInPlace(resultFst, two, false) ;
		resultFst = UnionIntoFirstInPlace(resultFst, three, true) ;
		return resultFst ;
	}

	public Fst Union4Fsts(Fst one, Fst two, Fst three, Fst four) {
		// native union is destructive of the first argument
		Fst resultFst = EmptyLanguageFst() ; 
		resultFst = UnionIntoFirstInPlace(resultFst, one, false) ;
		resultFst = UnionIntoFirstInPlace(resultFst, two, false) ;
		resultFst = UnionIntoFirstInPlace(resultFst, three, false) ;
		resultFst = UnionIntoFirstInPlace(resultFst, four, true) ;
		return resultFst ;
	}

	public Fst Union5Fsts(Fst one, Fst two, Fst three, Fst four, Fst five) {
		// native union is destructive of the first argument
		Fst resultFst = EmptyLanguageFst() ; 
		resultFst = UnionIntoFirstInPlace(resultFst, one, false) ;
		resultFst = UnionIntoFirstInPlace(resultFst, two, false) ;
		resultFst = UnionIntoFirstInPlace(resultFst, three, false) ;
		resultFst = UnionIntoFirstInPlace(resultFst, four, false) ;
		resultFst = UnionIntoFirstInPlace(resultFst, five, true) ;
		return resultFst ;
	}

}
