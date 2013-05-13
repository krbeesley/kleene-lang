//	kleeneopenfst.cc
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

//	The JNI "bridge" 


#include <iostream>
#include <iomanip>
//#include <stdio.h>
#include <jni.h>

// 2012-04-18 fstlib.h is in
// tools/OpenFst/openfst-1.3.x/src/include/fst/
#include <fst/fstlib.h>

// 2012-04-18 where is info-main.h? defines FstInfo, seems obsolete
//#include "fst/../../bin/info-main.h"
// 2012-04-18 try this instead for FstInfo
// 2012-04-19 it seems no longer necessary or recommended to use
// the FstInfo class directly.
//#include "fst/script/info-impl.h"
//
// 2012-04-20 tested this, trying to get exception "Symbol not found:
// _FLAGS_fst_align" fixed, didn't work
//#include "fst/flags.h"	// seems to define various flags
//
// this fst.h includes fst/compat.h, which in turn includes fst/flags.h
#include <fst/fst.h>		// refers to FLAGS_fst_align and FLAGS_v

#include <fst/script/fst-class.h>  // defines ArcType() and FstType()

#include "OpenFstLibraryWrapper.h"
#include <string>
#include <set>
#include <vector>

// ICU
#include "unicode/utypes.h"
#include "unicode/uchar.h"		// functions like u_isupper() and u_toupper()
#include "unicode/unistr.h"	// support UnicodeString
#include "unicode/normlzr.h"	// support Normalizer

// include "unicode/uclean.h"
// KRB:  look into u_unit() and u_cleanup() for ICU
// supports call to u_cleanup();  which cleans up the system
// resources, such as allocated memory or open files used in all
// ICU libraries; called at the end of a main() 


using namespace fst ;

typedef VectorFst<LogArc> LogVectorFst ;

typedef StdArc::Label Label ;
typedef StdArc::Weight Weight ;
typedef StdArc::StateId StateId ;

// variant of Determinize, one arg, for use in determinizeInPlaceNative
// code supplied by Cyril Allauzen, 11 Nov 2008
// 20090501 PWS: Changed DeterminizeFstOptions to DeterminizeFstOptions<Arc> for OpenFst 1.1.
//namespace fst {

//template <class Arc>
//void Determinize(MutableFst<Arc> *fst) {
//	*fst = DeterminizeFst<Arc>(*fst, DeterminizeFstOptions<Arc>(CacheOptions(true, 0)));
//}

//}

namespace fst {

	template <class Arc>
	void DetInPlace(MutableFst<Arc> * fstp) {
		*fstp = DeterminizeFst<Arc>(*fstp, DeterminizeFstOptions<Arc>(CacheOptions(true, 0))) ;
	}
}


JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_helloWorldNative
  (JNIEnv *env, jclass cls)
{
	cout << "Hello, World, from the shared library (with OpenFst)" << endl ;
	return ;
}

JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_emptyLanguageFstNative
  (JNIEnv *env, jclass cls)
{
	// semiring generalization point

	// Previous code always added a Start State 
	// see emptyLanguageWithStartStateFstNative

	//StdVectorFst *fstp = new StdVectorFst() ;
	//fstp->AddState() ;   // will be state 0
	//fstp->SetStart(0) ;  // set 0 as the start state
	// Fst has a start state, no arcs, no final state;
	// Encodes the empty language (doesn't contain any string,
	// not even the empty string)
	//return (jlong)(uintptr_t) fstp ;

	// new:  OpenFst empty networks simply have no
	// states
	return (jlong)(uintptr_t) new StdVectorFst() ;
}

JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_emptyLanguageWithStartStateFstNative
  (JNIEnv *env, jclass cls)
{
	// semiring generalization point

	StdVectorFst *fstp = new StdVectorFst() ;
	fstp->AddState() ;   // will be state 0
	fstp->SetStart(0) ;  // set 0 as the start state
	// Fst has a start state, no arcs, no final state;
	// Encodes the empty language (doesn't contain any string,
	// not even the empty string)
	// This version gives more intuitive results with
	// Union and union-like operations when optimization
	// is manually turned off.
	return (jlong)(uintptr_t) fstp ;
}

// return ptr to new Fst encoding the empty string language
JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_emptyStringLanguageFstNative
  (JNIEnv *env, jclass cls)
{
	// semiring generalization point
	StdVectorFst *fstp = new StdVectorFst() ;
	fstp->AddState() ;   // will be state 0
	fstp->SetStart(0) ;  // set 0 as the start state
	fstp->SetFinal(0, 0.0) ;
	// Fst has one state, which is both the start state and
	// a final state.  It encodes the language that contains
	// just one string, the empty string.  Pass the pointer
	// back to Java as a long (jlong here).
	return (jlong)(uintptr_t) fstp ;
}

JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_oneArcFstNative
  (JNIEnv *env, jclass cls,
   jint icpv, jint ocpv, jfloat arc_weight, jfloat final_weight)
{
	// get an empty StdVectorFst
	StdVectorFst *fstp = new StdVectorFst() ;

	fstp->AddState() ;   // will be state 0
	fstp->SetStart(0) ;  // set 0 as the start state

	fstp->AddArc(0,   StdArc(icpv, ocpv, arc_weight, 1)) ;
	//          src          in    out   weight   dest
	
	fstp->AddState() ;  // will be state 1
	fstp->SetFinal(1, final_weight) ;

	// return the ptr to the two-state, one-arc net as jlong
	return (jlong)(uintptr_t) fstp ;
}

JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_universalLanguageFstNative
  (JNIEnv *env, jclass cls,
   jint other_id, jfloat arc_weight, jfloat final_weight)
{
	// semiring generalization point
	// get an empty StdVectorFst
	StdVectorFst *fstp = new StdVectorFst() ;

	fstp->AddState() ;   // will be state 0
	fstp->SetStart(0) ;  // set 0 as the start state

	fstp->AddState() ;  // will be state 1
	fstp->SetFinal(1, final_weight) ;  // and final

	fstp->AddArc(0,   StdArc(other_id, other_id, arc_weight, 1)) ;
	//          srcState     input     output    weight   destState
	
 	Closure(fstp, CLOSURE_STAR) ;  // works in place

	// return the ptr to the two-state, one-arc net as jlong
	return (jlong)(uintptr_t) fstp ;
}

JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_universalRelationFstNative
  (JNIEnv *env, jclass cls,
   jint other_id, jint other_nonid, jfloat arc_weight, jfloat final_weight)
{
	// semiring generalization point
	// get an empty StdVectorFst
	StdVectorFst *fstp = new StdVectorFst() ;

	// **************** create the states

	fstp->AddState() ;   // will be state 0
	fstp->SetStart(0) ;  // set 0 as the start state
	fstp->SetFinal(0, final_weight) ;  // and final

	fstp->AddState() ;  // will be state 1
	fstp->SetFinal(1, final_weight) ;  // final

	fstp->AddState() ;  // will be state 2
	fstp->SetFinal(2, final_weight) ;  // final

	// **************** create the arcs

	fstp->AddArc(0,   StdArc(other_id, other_id, arc_weight, 0)) ;
	//          srcState     input     output    weight   destState
	
	fstp->AddArc(0,   StdArc(other_nonid, other_nonid, arc_weight, 0)) ;

	fstp->AddArc(0,   StdArc(0, other_nonid, arc_weight, 1)) ;
	fstp->AddArc(1,   StdArc(0, other_nonid, arc_weight, 1)) ;

	fstp->AddArc(0,   StdArc(other_nonid, 0, arc_weight, 2)) ;
	fstp->AddArc(2,   StdArc(other_nonid, 0, arc_weight, 2)) ;

	// return the ptr to the two-state, one-arc net as jlong
	return (jlong)(uintptr_t) fstp ;
}

// return ptr to a new Fst encoding one string, which
// consists of characters listed in intArray
JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_oneStringFstNative
	(JNIEnv *env, jclass cls,
	 jintArray intArray)
{
	StdVectorFst *fstp = new StdVectorFst() ;
	fstp->AddState() ;   // will be state 0
	fstp->SetStart(0) ;

	jboolean isCopy ;
	jint * intArrayElements = env->GetIntArrayElements(intArray, &isCopy) ;
	jint size = env->GetArrayLength(intArray) ;

	int cpv ;

	if (size == 0) {
		fstp->SetFinal(0, 0.0) ;  // the empty string language
	} else { 
		int i ;
		for (i = 0; i < size; i++) {
			cpv = intArrayElements[i] ;
			fstp->AddState() ;  // will be state i+1
			fstp->AddArc(i, StdArc(cpv, cpv, 0.0, i+1)) ;
		}
		fstp->SetFinal(i, 0.0) ;
	}

	if (isCopy == JNI_TRUE) {
		env->ReleaseIntArrayElements(intArray, intArrayElements, 0) ;
	}
	return (jlong)(uintptr_t) fstp ;
}

JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_copyFstNative
  (JNIEnv *env, jclass cls,
   jlong fstpOrig)
{
	// get a ptr to a new empty Fst
	StdVectorFst * fstpNew = new StdVectorFst() ;
	// Map (FST, ptr to FST, a mapper)
	// here copies the first net into the second
	Map(*((StdVectorFst *)(uintptr_t) fstpOrig), fstpNew, IdentityMapper<StdArc>()) ;
	return (jlong)(uintptr_t) fstpNew ;
}


JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_concatIntoFirstNative
  (JNIEnv *env, jclass cls,
   jlong first, jlong second)
{
	// OpenFst Concat(), destructive on first argument
	Concat((StdVectorFst *)(uintptr_t) first, *((StdVectorFst *)(uintptr_t) second)) ;
	return ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_unionIntoFirstNative
  (JNIEnv *env, jclass cls,
   jlong first, jlong second)
{
	// OpenFst Union(); destructive on first argument
	Union((StdVectorFst *)(uintptr_t) first, *((StdVectorFst *)(uintptr_t) second)) ;
	return ;
}

JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_charRangeUnionFstNative
	(JNIEnv *env, jclass cls,
	 jint firstCpv, jint lastCpv)
{
	StdVectorFst *fstp = new StdVectorFst() ;

	fstp->AddState() ;   // will be state 0
	fstp->SetStart(0) ;  // set 0 as the start state

	fstp->AddState() ;  // will be state 1
	fstp->SetFinal(1, (float) 0.0) ;

	// count _down_ for more natural graph display

	for (int cpv = lastCpv; cpv >= firstCpv; cpv--) {
		fstp->AddArc(0, StdArc(cpv, cpv, (float) 0.0, 1)) ;
	}

	// KRB:  watch this RmEpsilon for appropriateness
	RmEpsilon(fstp) ;
	return (jlong)(uintptr_t) fstp ;
}

JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_differenceNative
  (JNIEnv *env, jclass cls,
   jlong first, jlong second)
{

	StdVectorFst * firstFstPtr = (StdVectorFst *)(uintptr_t) first ;
	StdVectorFst * secondFstPtr = (StdVectorFst *)(uintptr_t) second ;

	StdVectorFst * resultFstPtr = new StdVectorFst() ;
	// need to sort the output arcs of the first arg, or the input arcs
	// of the second arg
	ArcSort(secondFstPtr, StdILabelCompare()) ;
	Difference(*firstFstPtr, *secondFstPtr, resultFstPtr) ;
	return (jlong)(uintptr_t) resultFstPtr ;
}

JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_intersectNative
  (JNIEnv *env, jclass cls,
   jlong first, jlong second)
{

	StdVectorFst * firstFstPtr = (StdVectorFst *)(uintptr_t) first ;
	StdVectorFst * secondFstPtr = (StdVectorFst *)(uintptr_t) second ;

	StdVectorFst * resultFstPtr = new StdVectorFst() ;
	// need to sort the output arcs of the first arg, or the input arcs
	// of the second arg
	ArcSort(secondFstPtr, StdILabelCompare()) ;
	Intersect(*firstFstPtr, *secondFstPtr, resultFstPtr) ;
	return (jlong)(uintptr_t) resultFstPtr ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_substLabelInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr, jint orig, jint repl)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	// figure out how to template <class Arc>
	
	//vector< pair<typename Arc::Label, typename Arc::Label> > vect ;
	vector< pair<StdArc::Label, StdArc::Label> > vect ;

	//vect.push_back(pair<typename Arc::Label, typename Arc::Label> (orig, repl) ;
	vect.push_back(pair<StdArc::Label, StdArc::Label> ((StdArc::Label)orig, 
														(StdArc::Label)repl)) ;

	Relabel(fstp, vect, vect) ;

	if (repl == 0) {
		// may have introduced eps:eps labels
		RmEpsilon(fstp) ;
	}
}

// fixOtherInputBeforeCompose
// When composing A _o_ B, this program is called for B.
// Changes all OTHER_ID on the input side to OTHER_NONID.

// cf. fixOtherOutputBeforeCompose, which takes the A network
// and changes all OTHER_ID on the output side to OTHER_NONID.

// Then the standard OpenFst Compose() operation is called,
// and the intersection of the intermediate levels can work
// as it should (no distinction between OTHER_ID and OTHER_NONID).

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_fixOtherInputBeforeComposeNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr, jint other_id, jint other_nonid)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	for (StateIterator<StdVectorFst> siter(*fstp) ;
			!siter.Done() ;
			siter.Next()) {
		// s is the int identifier of a state
		StateId s = siter.Value() ;

		// Now loop through the exit arcs of s.
		// Need to use MUTABLEArcIterator to be able to _change_
		// the values on the orig. arcs (this constructor needs a ptr
		// to an Fst rather than the Fst itself); the final stmt
		// in the loop needs to be aiter.SetValue(arc) to set the
		// new values in the Fst

		for (MutableArcIterator<StdVectorFst> aiter(fstp, s) ;
				!aiter.Done() ;
				aiter.Next()) {

			// arc is (for now) a copy of the original arc
			StdArc arc = aiter.Value() ;

			if (arc.ilabel == other_id)
				arc.ilabel = other_nonid ;

			// and here, the final step in the loop through the arcs,
			// the modified arc is set in the network being processed
			aiter.SetValue(arc) ;
		} // end of loop through the arcs of a state
	} // end of loop through the states
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_fixOtherOutputBeforeComposeNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr, jint other_id, jint other_nonid)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	for (StateIterator<StdVectorFst> siter(*fstp) ;
			!siter.Done() ;
			siter.Next()) {
		// s is the int identifier of a state
		StateId s = siter.Value() ;

		// Now loop through the exit arcs of s.
		// Need to use MUTABLEArcIterator to be able to _change_
		// the values on the orig. arcs (this constructor needs a ptr
		// to an Fst rather than the Fst itself); the final stmt
		// in the loop needs to be aiter.SetValue(arc) to set the
		// new values in the Fst

		for (MutableArcIterator<StdVectorFst> aiter(fstp, s) ;
				!aiter.Done() ;
				aiter.Next()) {

			// arc is (for now) a copy of the original arc
			StdArc arc = aiter.Value() ;

			if (arc.olabel == other_id)
				arc.olabel = other_nonid ;

			// and here, the final step in the loop through the arcs,
			// the modified arc is set in the network being processed
			aiter.SetValue(arc) ;
		} // end of loop through the arcs of a state
	} // end of loop through the states
}

    class ArcInfo {
      public:
	    StateId src_state_id ;
	    Label ilabel ;
	    Label olabel ;
	    Weight weight ;
	    StateId dest_state_id ;

	    // Constructor
	    ArcInfo (StateId s, Label i, Label o, Weight w, StateId d) 
	    : src_state_id(s), ilabel(i), olabel(o), weight(w), dest_state_id(d)
	    {
        } 
    } ;

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_fixOtherAfterComposeNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr, jint other_id, jint other_nonid)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	// empty vector to store pointers to ArcInfo objects
	vector<ArcInfo *> arcsToAdd ;
	vector<ArcInfo *>::iterator iter ;

	for (StateIterator<StdVectorFst> siter(*fstp) ;
			!siter.Done() ;
			siter.Next()) {
		// src_state_id is the int identifier of a state
		StateId src_state_id = siter.Value() ;

		// Now loop through the exit arcs of s.
		// Need to use a MUTABLEArcIterator to be able to _change_
		// the values on the orig. arcs (the constructor needs a ptr
		// to an Fst rather than the Fst itself); the final stmt
		// in the loop needs to be aiter.SetValue(arc) to set the
		// new values in the Fst

		for (MutableArcIterator<StdVectorFst> aiter(fstp, src_state_id) ;
				!aiter.Done() ;
				aiter.Next()) {

			// arc is (at first) a copy of the original arc
			StdArc arc = aiter.Value() ;

			if (arc.ilabel == other_id) {
				// N.B. do NOT join these two tests into one with &&
				if (arc.olabel != other_id) {
					// OTHER_ID:c and OTHER_ID:OTHER_NONID are impossible.
					// so change the ilabel to OTHER_NONID

					arc.ilabel = other_nonid ;
				}
				// else OTHER_ID:OTHER_ID is OK
			} else if (arc.ilabel == other_nonid) {
				if (arc.olabel == other_id) {
					// OTHER_NONID:OTHER_ID is impossible,
					// so change the olabel to OTHER_NONID

					arc.olabel = other_nonid ;
				} else if (arc.olabel == other_nonid) {
					// we have OTHER_NONID:OTHER_NONID

					// here we need to add a parallel arc 
					// other_id:other_id
					// Add it to set of arcsToAdd (these are
					// added later because the MutableArcIterator
					// is broken/confused if the number of arcs
					// is changed during iteration.

					arcsToAdd.push_back(new ArcInfo(src_state_id, 
						other_id, other_id, arc.weight, arc.nextstate)) ;
				}
				// else OTHER_NONID:c is OK
			} else if (arc.olabel == other_id) {
				// we have c:OTHER_ID, which is illegal,
				// so change the olabel to OTHER_NONID

				arc.olabel = other_nonid ;
			}

			// and here, the final step in the loop through the arcs,
			// the modified arc is set in the network being processed
			aiter.SetValue(arc) ;
		} // end of loop through the arcs of a state
	} // end of loop through the states

	// add the arcsToAdd here
	for (iter = arcsToAdd.begin(); iter != arcsToAdd.end(); iter++) {
		ArcInfo * aip = *iter ;
		fstp->AddArc(aip->src_state_id, 
			StdArc(aip->ilabel, aip->olabel, aip->weight, aip->dest_state_id)) ;
	}
	// should disappear at end of function
	// arcsToAdd.clear() ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_inputProjectionFixOtherInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr, jint other_id, jint other_nonid)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	for (StateIterator<StdVectorFst> siter(*fstp) ;
			!siter.Done() ;
			siter.Next()) {
		// src_state_id is the int identifier of a state
		StateId src_state_id = siter.Value() ;

		// Now loop through the exit arcs of s.
		// Need to use a MUTABLEArcIterator to be able to _change_
		// the values on the orig. arcs (the constructor needs a ptr
		// to an Fst rather than the Fst itself); the final stmt
		// in the loop needs to be aiter.SetValue(arc) to set the
		// new values in the Fst

		for (MutableArcIterator<StdVectorFst> aiter(fstp, src_state_id) ;
				!aiter.Done() ;
				aiter.Next()) {

			// arc is (at first) a copy of the original arc
			StdArc arc = aiter.Value() ;

			// Because the fst passed to this function should be the
			// result of taking the inputProj or the outputProj, the
			// ilabel and olabel should be the same; so if ilabel is
			// other_nonid, then olabel is other_nonid as well

			if (arc.ilabel == other_nonid) {
				// Special case
				// OTHER_NONID:something label
				// Label needs to be changed to OTHER_ID:OTHER_ID so that
				// the result will be an acceptor
				arc.ilabel = other_id ;
				arc.olabel = other_id ;
			} else {
				// make the olabel the same as the ilabel so that the
				// result will be an acceptor
				arc.olabel = arc.ilabel ;
			}

			// and here, the final step in the loop through the arcs,
			// the modified arc is set in the network being processed
			aiter.SetValue(arc) ;
		} // end of loop through the arcs of a state
	} // end of loop through the states
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_changeInputToEpsilonInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr, jint other_id, jint other_nonid)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	for (StateIterator<StdVectorFst> siter(*fstp) ;
			!siter.Done() ;
			siter.Next()) {
		// src_state_id is the int identifier of a state
		StateId src_state_id = siter.Value() ;

		// Now loop through the exit arcs of s.
		// Need to use a MUTABLEArcIterator to be able to _change_
		// the values on the orig. arcs (the constructor needs a ptr
		// to an Fst rather than the Fst itself); the final stmt
		// in the loop needs to be aiter.SetValue(arc) to set the
		// new values in the Fst

		for (MutableArcIterator<StdVectorFst> aiter(fstp, src_state_id) ;
				!aiter.Done() ;
				aiter.Next()) {

			// arc is (at first) a copy of the original arc
			StdArc arc = aiter.Value() ;

			if (arc.olabel == other_id) {
				// Special case, need to change to other_nonid
				// 	because the upper side is going to be changed
				// 	brute force to epsilon
				arc.olabel = other_nonid ;
			}

			arc.ilabel = 0 ;	// 0 is wired in as epsilon in OpenFst

			// and here, the final step in the loop through the arcs,
			// the modified arc is set in the network being processed
			aiter.SetValue(arc) ;
		} // end of loop through the arcs of a state
	} // end of loop through the states
}


JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_changeOutputToEpsilonInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr, jint other_id, jint other_nonid)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	for (StateIterator<StdVectorFst> siter(*fstp) ;
			!siter.Done() ;
			siter.Next()) {
		// src_state_id is the int identifier of a state
		StateId src_state_id = siter.Value() ;

		// Now loop through the exit arcs of s.
		// Need to use a MUTABLEArcIterator to be able to _change_
		// the values on the orig. arcs (the constructor needs a ptr
		// to an Fst rather than the Fst itself); the final stmt
		// in the loop needs to be aiter.SetValue(arc) to set the
		// new values in the Fst

		for (MutableArcIterator<StdVectorFst> aiter(fstp, src_state_id) ;
				!aiter.Done() ;
				aiter.Next()) {

			// arc is (at first) a copy of the original arc
			StdArc arc = aiter.Value() ;

			if (arc.ilabel == other_id) {
				// Special case, need to change to other_nonid
				// 	because the lower side is going to be changed
				// 	brute force to epsilon
				arc.ilabel = other_nonid ;
			}

			arc.olabel = 0 ;	// 0 is wired in as epsilon in OpenFst

			// and here, the final step in the loop through the arcs,
			// the modified arc is set in the network being processed
			aiter.SetValue(arc) ;
		} // end of loop through the arcs of a state
	} // end of loop through the states
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_outputProjectionFixOtherInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr, jint other_id, jint other_nonid)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	for (StateIterator<StdVectorFst> siter(*fstp) ;
			!siter.Done() ;
			siter.Next()) {
		// src_state_id is the int identifier of a state
		StateId src_state_id = siter.Value() ;

		// Now loop through the exit arcs of s.
		// Need to use a MUTABLEArcIterator to be able to _change_
		// the values on the orig. arcs (the constructor needs a ptr
		// to an Fst rather than the Fst itself); the final stmt
		// in the loop needs to be aiter.SetValue(arc) to set the
		// new values in the Fst

		for (MutableArcIterator<StdVectorFst> aiter(fstp, src_state_id) ;
				!aiter.Done() ;
				aiter.Next()) {

			// arc is (at first) a copy of the original arc
			StdArc arc = aiter.Value() ;

			// Because the fst passed to this function should be the
			// result of taking the inputProj or the outputProj, the
			// ilabel and olabel should be the same; so if ilabel is
			// other_nonid, then olabel is other_nonid as well

			if (arc.olabel == other_nonid) {
				// Special case
				// something:OTHER_NONID label
				// Label needs to be changed to OTHER_ID:OTHER_ID
				// so that the result will be an acceptor
				arc.ilabel = other_id ;
				arc.olabel = other_id ;
			} else {
				// make the ilabel the same as the olabel so that the
				// result will be an acceptor
				arc.ilabel = arc.olabel ;
			}

			// and here, the final step in the loop through the arcs,
			// the modified arc is set in the network being processed
			aiter.SetValue(arc) ;
		} // end of loop through the arcs of a state
	} // end of loop through the states
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_deleteOtherArcsInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr, jint other_id, jint other_nonid)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	// add a new "sink" state to the fst
	StateId sink = fstp->AddState() ;

	for (StateIterator<StdVectorFst> siter(*fstp) ;
			!siter.Done() ;
			siter.Next()) {
		// src_state_id is the int identifier of a state
		StateId src_state_id = siter.Value() ;

		// Now loop through the exit arcs of s.
		// Need to use a MUTABLEArcIterator to be able to _change_
		// the values on the orig. arcs (the constructor needs a ptr
		// to an Fst rather than the Fst itself); the final stmt
		// in the loop needs to be aiter.SetValue(arc) to set the
		// new values in the Fst

		for (MutableArcIterator<StdVectorFst> aiter(fstp, src_state_id) ;
				!aiter.Done() ;
				aiter.Next()) {

			// arc is (at first) a copy of the original arc
			StdArc arc = aiter.Value() ;


			if (   arc.ilabel == other_id || arc.ilabel == other_nonid
				|| arc.olabel == other_id || arc.olabel == other_nonid) {
				// then re-point the arc to the sink state
				arc.nextstate = sink ;
			}

			// and here, the final step in the loop through the arcs,
			// the modified arc is set in the network being processed
			aiter.SetValue(arc) ;
		} // end of loop through the arcs of a state
	} // end of loop through the states

	// now call Connect() to trim the network, deleting all states and arcs 
	// not on a path from the start state to a final state (this will delete
	// the sink state and all arcs pointing to it)
	Connect(fstp) ;
}

// cf. deleteOtherArcsInPlace

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_synchronizeAltRuleInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr, 
   jint ruleRightAngleSymVal, 
   jint hardEpsilonSymVal,
   jint other_id,
   jint other_nonid)
{
	// Purpose: final cleanup of networks compiled from alternation
	//		rules, where the "action" of a rule ab -> cd
	//		results in a subpath a:eps eps:c b:eps eps:d
	// PreProcess converts this to a sequence of labels a > c b > d

	// this synchronizeAltRuleInPlaceNative:
	// 
	// First Pass:
	// find subpaths    src_state x int1_state > int2_state y dest_state
	// and add new arc  src_state x:y dest_state

	// Second Pass:
	// then eliminate the > arcs

	// first pass adding x:y arcs

	// fstp is a pointer to a native OpenFst network
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	vector<ArcInfo *> arcsToAdd ;
	vector<ArcInfo *>::iterator iter ;

	Matcher<StdVectorFst> matcher(*fstp, MATCH_INPUT) ;

	// sort the arcs by input label for use with Matcher
	ArcSort(fstp, StdILabelCompare()) ;

	for (StateIterator<StdVectorFst> siter(*fstp);
       	!siter.Done();
       	siter.Next()) {

		// src_state for any new arcs
		StateId src_state = siter.Value() ;

		// loop through the arcs exiting the src_state
		for (ArcIterator<StdVectorFst> aiter1(*fstp, src_state);
			!aiter1.Done() ;
			aiter1.Next() ) {

			const StdArc &arc1 = aiter1.Value() ;

			Label ilabel = arc1.ilabel ;  // save the ilabel
			
			// look only at identity x:x arcs here
			if (ilabel == arc1.olabel) {

				// convert hardEpsilonSymVal to real epsilon here
				if (ilabel == hardEpsilonSymVal) {
					ilabel = 0 ;
				}
				
				StateId int1_state = arc1.nextstate ;

				matcher.SetState(int1_state) ;
				if (matcher.Find(ruleRightAngleSymVal)) {
					for (  ;
						!matcher.Done() ;
						matcher.Next()) {

						// an arc labeled >
						const StdArc &arc_rt_angle = matcher.Value() ;

						StateId int2_state = arc_rt_angle.nextstate ;

						for (ArcIterator<StdVectorFst> aiter3(*fstp, int2_state) ;
							!aiter3.Done() ;
							aiter3.Next() ) {
					
							const StdArc &arc3 = aiter3.Value() ;

							StateId dest_state = arc3.nextstate ;

							// ilabel and olabel should be the same
							Label olabel = arc3.olabel ;
							// convert hardEpsilonSymVal to real epsilon here
							if (olabel == hardEpsilonSymVal) {
								olabel = 0 ;
							}

							// where necessary, change other_id to
							// other_nonid

							if (ilabel == other_id && olabel != other_id)
								ilabel = other_nonid ;
							if (olabel == other_id && ilabel != other_id)
								olabel = other_nonid ;

							// ask Hulden about the weight
							arcsToAdd.push_back(new ArcInfo(src_state,
								ilabel, olabel, 0.0, dest_state)) ;
						}
					}
				}
			}
		}
	}

	// add the arcsToAdd here
	for (iter = arcsToAdd.begin(); iter != arcsToAdd.end(); iter++) {
		ArcInfo * aip = *iter ;
		fstp->AddArc(aip->src_state_id, 
			StdArc(aip->ilabel, aip->olabel, aip->weight, aip->dest_state_id)) ;
	}

	// second pass to eliminate > arcs

	// create a new sink state-- repoint > arcs to it
	StateId sink = fstp->AddState() ;
	// leave this sink state non-final

	for (StateIterator<StdVectorFst> siter(*fstp) ;
			!siter.Done() ;
			siter.Next()) {
		StateId src_state_id = siter.Value() ;

		// Now loop through the exit arcs of s.
		// Need to use a MUTABLEArcIterator to be able to _change_
		// the values on the orig. arcs (the constructor needs a ptr
		// to an Fst rather than the Fst itself); the final stmt
		// in the loop needs to be aiter.SetValue(arc) to set the
		// new values in the Fst

		for (MutableArcIterator<StdVectorFst> aiter(fstp, src_state_id) ;
				!aiter.Done() ;
				aiter.Next()) {

			// arc is (at first) a copy of the original arc
			StdArc arc = aiter.Value() ;


			if (arc.ilabel == ruleRightAngleSymVal) {
				// then re-point the arc to the sink state
				arc.nextstate = sink ;
			}

			// and here, the final step in the loop through the arcs,
			// the modified arc is set in the network being processed
			aiter.SetValue(arc) ;
		} // end of loop through the arcs of a state
	} // end of loop through the states

	// now call Connect() to trim the network, deleting all states and arcs 
	// not on a path from the start state to a final state (this will delete
	// the sink state and all arcs pointing to it)
	Connect(fstp) ;
}

JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_composeNative
  (JNIEnv *env, jclass cls,
   jlong first, jlong second)
{
	StdVectorFst * firstFstPtr = (StdVectorFst *)(uintptr_t) first ;
	StdVectorFst * secondFstPtr = (StdVectorFst *)(uintptr_t) second ;

	StdVectorFst * resultFstPtr = new StdVectorFst() ;  // on the heap
	// need to sort the output arcs of the first, or the input
	// arcs of the second
	ArcSort(secondFstPtr, StdILabelCompare()) ;
	Compose(*firstFstPtr, *secondFstPtr, resultFstPtr) ;
	return (jlong)(uintptr_t) resultFstPtr ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_kleeneStarInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	Closure((StdVectorFst *)(uintptr_t) fstPtr, CLOSURE_STAR) ;
	return ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_kleenePlusInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	Closure((StdVectorFst *)(uintptr_t) fstPtr, CLOSURE_PLUS) ;
	return ;
}

JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_iterateLowHighNative
	(JNIEnv *env, jclass cls,
	 jlong fstPtr, jlong low, jlong high)
{
	// basically a kind of concatenation;
	// start with an emptyStringLanguageFst 
	// (which recognizes only the empty string)
	StdVectorFst *resultFstp = new StdVectorFst() ;
	resultFstp->AddState() ;
	resultFstp->SetStart(0) ;
	resultFstp->SetFinal(0, 0.0) ;

	// result starts with exactly 'low' concatenations of fstPtr (low could be 0)
	for (int i = 0; i < low; i++) {
		Concat(resultFstp, *((StdVectorFst *)(uintptr_t) fstPtr)) ;
	}

	// for the rest, start with a copy of fstPtr
	StdVectorFst fstRest ;  // not on the heap
	// Map (FST, ptr to FST, a mapper)
	// here copies the first net into the second
	Map(*((StdVectorFst *)(uintptr_t) fstPtr), &fstRest, IdentityMapper<StdArc>()) ;

	if (high == -1) {   // pseudo-value (see OpenFstLibraryWrapper.java)
		// then it's open-ended; need to concatenate fstRest*
		Closure(&fstRest, CLOSURE_STAR) ;
		Concat(resultFstp, fstRest) ;
	} else {
		// there's an explicit high value
		//
		// need to concatenate high-low copies of _optionalized_ fstRest
		//

		// the easy way would be to get the start state of fstRest and
		// make it final, but what about the weight?
		// This should be safe, union in an emptyStringLanguageFst
		StdVectorFst emptyStringFst ;  // not on the heap
		emptyStringFst.AddState() ;    // will be 0
		emptyStringFst.SetStart(0) ;
		emptyStringFst.SetFinal(0, 0.0) ;

		Union(&fstRest, emptyStringFst) ;
		
		for (int i = 0; i < (high - low); i++) {
			Concat(resultFstp, fstRest) ;
		}
	}

	// KRB:  keep an eye on this
	RmEpsilon(resultFstp) ;
	return (jlong)(uintptr_t) resultFstp ;
}

/*
JNIEXPORT jboolean JNICALL
Java_OpenFstLibraryWrapper_isEquivalentNative
  (JNIEnv *env, jclass cls,
   jlong first,
   jlong second,
   jdouble delta)
{
	StdVectorFst * one = (StdVectorFst *)(uintptr_t) first ;
	StdVectorFst * two = (StdVectorFst *)(uintptr_t) second ;
	return (jboolean) Equivalent(one, two, (double) delta) ;
}
*/

JNIEXPORT jboolean JNICALL
Java_OpenFstLibraryWrapper_isAcceptorNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	if (((StdVectorFst *)(uintptr_t) fstPtr)->Properties(kAcceptor, true)) {
		return (jboolean) true ;
	} else {
		return (jboolean) false ;
	}
}

JNIEXPORT jboolean JNICALL
Java_OpenFstLibraryWrapper_isSemanticAcceptorNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr,
   jint other_nonid)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	if (fstp->Properties(kAcceptor, true)) {
		// It looks like an acceptor to OpenFst
		// but if it contains an arc labeled OTHER_NONID:OTHER_NONID, then
		// it's a semantic transducer
		for (StateIterator<StdVectorFst> siter(*fstp) ;
			!siter.Done() ;
			siter.Next()) {
			StateId s = siter.Value() ;
			for (ArcIterator<StdVectorFst> aiter(*fstp, s) ;
				!aiter.Done() ;
				aiter.Next()) {
				StdArc arc = aiter.Value() ;
				if (arc.ilabel == other_nonid && arc.olabel == other_nonid) {
					// then it's a semantic _transducer_ for Kleene
					return (jboolean) false ;
				}
			}
		}
		return (jboolean) true ;
	} else {
		return (jboolean) false ;
	}
}

JNIEXPORT jboolean JNICALL
Java_OpenFstLibraryWrapper_isCyclicNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	if (((StdVectorFst *)(uintptr_t) fstPtr)->Properties(kCyclic, true)) {
		return (jboolean) true ;
	} else {
		return (jboolean) false ;
	}
}

/*  My old approach
bool findInputEpsilonLoop(StdVectorFst *fstp, StateId s, set<StateId> &stateSet) {
	// try to find an input/upper epsilon loop from state s back
	// to itself, or another loop on the way to s
	
	if (stateSet.find(s) != stateSet.end()) 
		// found a loop
		return true ;
	// remember s as "visited"
	stateSet.insert(s) ;
	for (ArcIterator<StdVectorFst> aiter(*fstp, s) ;
		!aiter.Done() ;
		aiter.Next()) {
		StdArc arc = aiter.Value() ;
		// handle only arcs with epsilon on the input side
		if (arc.ilabel != 0)
			continue ;
		// here the ilabel is 0 (epsilon)
		// recursively look for an input epsilon loop from arc.nextstate
		if (findInputEpsilonLoop(fstp, arc.nextstate, stateSet))
				return true ;
	}
	return false ;
}

JNIEXPORT jboolean JNICALL
Java_OpenFstLibraryWrapper_isUBoundedNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr) {
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;
	set<StateId> stateSet ;	// reuse this set of "visited states"

	// if the machine has no cycles at all, then it's ubounded (and lbounded)
	if (fstp->Properties(kAcyclic, true))
		return (jboolean) true ;

	// loop through the states, looking for input/upper epsilon loops
	for (StateIterator<StdVectorFst> siter(*fstp) ;
		!siter.Done() ;
		siter.Next()) {
		stateSet.clear() ;
		if (findInputEpsilonLoop(fstp, siter.Value(), stateSet))
			// found an input epsilon loop, so not ubounded
			return (jboolean) false ;
	}
	// no input epsilon loops found
	return (jboolean) true ;
}
*/

// new approach from PaulDixon on OpenFst forum ("HasInputEpsilonCycle")
// His Original:
// Using the OpenFst visitor to find a topological order on the 
// epsilon input subgraph should work.
/*
template<class Arc>
bool HasInputEpsilonCycle(const Fst<Arc>& fst) {
  vector<typename Arc::StateId> order;
  bool acyclic = false;
  TopOrderVisitor<Arc> top_order_visitor(&order, &acyclic);
  DfsVisit(fst, &top_order_visitor, InputEpsilonArcFilter<Arc>());
  return !acyclic;
}
*/

JNIEXPORT jboolean JNICALL
Java_OpenFstLibraryWrapper_isUBoundedNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr) {
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;
	vector<StateId> order ;
	bool acyclic = false ;
	TopOrderVisitor<StdArc> top_order_visitor(&order, &acyclic) ;
	DfsVisit(*fstp, &top_order_visitor, InputEpsilonArcFilter<StdArc>()) ;
	return acyclic ;	// opposite logic from PaulDixon example
}

JNIEXPORT jboolean JNICALL
Java_OpenFstLibraryWrapper_isLBoundedNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr) {
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;
	vector<StateId> order ;
	bool acyclic = false ;
	TopOrderVisitor<StdArc> top_order_visitor(&order, &acyclic) ;
	DfsVisit(*fstp, &top_order_visitor, OutputEpsilonArcFilter<StdArc>()) ;
	return acyclic ;	// opposite logic from PaulDixon example
}

/*  my old original approach, see new _isLBoundedNative above
bool findOutputEpsilonLoop(StdVectorFst *fstp, StateId s, set<StateId> &stateSet) {
	// try to find an output/lower epsilon loop from state s back
	// to itself, or another loop on the way to s
	
	if (stateSet.find(s) != stateSet.end()) 
		// found a loop
		return true ;
	// remember s as "visited"
	stateSet.insert(s) ;
	for (ArcIterator<StdVectorFst> aiter(*fstp, s) ;
		!aiter.Done() ;
		aiter.Next()) {
		StdArc arc = aiter.Value() ;
		// handle only arcs with epsilon on the output side
		if (arc.olabel != 0)
			continue ;
		// here the olabel is 0 (epsilon)
		// recursively look for an output epsilon loop from arc.nextstate
		if (findOutputEpsilonLoop(fstp, arc.nextstate, stateSet))
				return true ;
	}
	return false ;
}

JNIEXPORT jboolean JNICALL
Java_OpenFstLibraryWrapper_isLBoundedNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr) {
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;
	set<StateId> stateSet ;	// reuse this set of "visited states"

	// if the machine has no cycles at all, then it's lbounded (and ubounded)
	if (fstp->Properties(kAcyclic, true))
		return (jboolean) true ;

	// loop through the states, looking for output/lower epsilon loops
	for (StateIterator<StdVectorFst> siter(*fstp) ;
		!siter.Done() ;
		siter.Next()) {
		stateSet.clear() ;
		if (findOutputEpsilonLoop(fstp, siter.Value(), stateSet))
			// found an output epsilon loop, so not lbounded
			return (jboolean) false ;
	}
	// no input epsilon loops found
	return (jboolean) true ;
}
*/

JNIEXPORT jboolean JNICALL
Java_OpenFstLibraryWrapper_isEmptyLanguageNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	if (((StdVectorFst *)(uintptr_t) fstPtr)->Start() == kNoStateId) {
		return (jboolean) true ;
	} else {
		return (jboolean) false ;
	}
}

JNIEXPORT jboolean JNICALL
Java_OpenFstLibraryWrapper_containsEmptyStringNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	StateId startStateId = fstp->Start() ;

	// if there is a real start state (i.e. the FST is not
	// completely empty, AND the start state is a final state,
	// then the FST accepts the empty string

	if (( startStateId != kNoStateId )
		&& 
		( fstp->Final(startStateId) != TropicalWeight::Zero() )
	   ) {
		return (jboolean) true ;
	} else {
		return (jboolean) false ;
	}
}

JNIEXPORT jboolean JNICALL
Java_OpenFstLibraryWrapper_isAcyclicNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	if (((StdVectorFst *)(uintptr_t) fstPtr)->Properties(kAcyclic, true)) {
		return (jboolean) true ;
	} else {
		return (jboolean) false ;
	}
}

JNIEXPORT jboolean JNICALL
Java_OpenFstLibraryWrapper_isStringNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	if (((StdVectorFst *)(uintptr_t) fstPtr)->Properties(kString, true)) {
		return (jboolean) true ;
	} else {
		return (jboolean) false ;
	}
}

JNIEXPORT jboolean JNICALL
Java_OpenFstLibraryWrapper_isNotStringNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	if (((StdVectorFst *)(uintptr_t) fstPtr)->Properties(kNotString, true)) {
		return (jboolean) true ;
	} else {
		return (jboolean) false ;
	}
}

JNIEXPORT jboolean JNICALL
Java_OpenFstLibraryWrapper_isWeightedNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
    if (((StdVectorFst *)(uintptr_t) fstPtr)->Properties(kWeighted, true)) {
		return (jboolean) true ;
	} else {
		return (jboolean) false ;
	}
}

JNIEXPORT jboolean JNICALL
Java_OpenFstLibraryWrapper_isUnweightedNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
    if (((StdVectorFst *)(uintptr_t) fstPtr)->Properties(kUnweighted, true)) {
		return (jboolean) true ;
	} else {
		return (jboolean) false ;
	}
}

JNIEXPORT jboolean JNICALL
Java_OpenFstLibraryWrapper_isEpsilonFreeNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
    if (((StdVectorFst *)(uintptr_t) fstPtr)->Properties(kNoEpsilons, true)) {
		return (jboolean) true ;
	} else {
		return (jboolean) false ;
	}
}

JNIEXPORT jboolean JNICALL
Java_OpenFstLibraryWrapper_isIDeterministicNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
    if (((StdVectorFst *)(uintptr_t) fstPtr)->Properties(kIDeterministic, true)) {
		return (jboolean) true ;
	} else {
		return (jboolean) false ;
	}
}

JNIEXPORT jboolean JNICALL
Java_OpenFstLibraryWrapper_isODeterministicNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
    if (((StdVectorFst *)(uintptr_t) fstPtr)->Properties(kODeterministic, true)) {
		return (jboolean) true ;
	} else {
		return (jboolean) false ;
	}
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_rmEpsilonInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
    // modifies the network in-place
	RmEpsilon((StdVectorFst *)(uintptr_t) fstPtr) ;
    return ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_connectInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
    // modifies the network in-place
	Connect((StdVectorFst *)(uintptr_t) fstPtr) ;
    return ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_sortInputArcsInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
    // modifies the network in-place
	ArcSort((StdVectorFst *)(uintptr_t) fstPtr, StdILabelCompare()) ;
    return ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_sortOutputArcsInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
    // modifies the network in-place
	ArcSort((StdVectorFst *)(uintptr_t) fstPtr, StdOLabelCompare()) ;
    return ;
}

// KRB:  experiment 2010-04-27 to see if Synchronize() will fix the
// non-optimal mapping found in abc->def, which comes out as
// a:0 0:d b:0 0:e c:0 0:f
// want to change that to a:d b:e c:f
JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_synchronizeInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
    // modifies the network in-place
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;
	*fstp = SynchronizeFst<StdArc>(*fstp,
		SynchronizeFstOptions(CacheOptions(true, 0))) ;
	// this will often leave eps:eps arcs
	RmEpsilon(fstp) ;

    return ;
}

// this native function straightforwardly wraps Determinize(),
// and returns a new FST, used only for the func call syntax
// ASTnet_determinize_func_call, e.g. in
// $newFst = $&determinize($origFst) ;
//
// see also determinizeInPlaceNative
JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_determinizeNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	StdVectorFst * resultFst = new StdVectorFst() ;
	Determinize(*((StdVectorFst *)(uintptr_t) fstPtr), resultFst) ;
    return (jlong)(uintptr_t) resultFst ;
}

// cf to optimizeInPlaceNative; determinizeInPlaceNative just does (safe)
// determinization
JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_determinizeInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	// Old
	//DetInPlace(((StdVectorFst *)(uintptr_t) fstPtr)) ;
    //return ;
	
	// New
	// KRB; semiring-generalization point
	// figure out how to template this on <class Arc>

	// StdVectorFst is a typedef for VectorFst<StdArc>
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;
	//VectorFst<StdArc> * fstp = (VectorFst<StdArc> *)(uintptr_t) fstPtr ;
	// or ???
	//MutableFst<Arc> * fstp = (MutableFst<Arc> *)(uintptr_t) fstPtr ;

	if (      fstp->Properties(kUnweighted, true)
		   && fstp->Properties(kAcceptor, true)
	   ) {

		// Unweighted acceptors are always mathematically safe for 
		//    Determinization.
    	// No need to Encode/Decode in this case

		// Determinize in place (the normal Determinize() algorithm actually
		//   creates a new output Fst)
		//   KRB semiring-generalization point

    	*fstp = DeterminizeFst<StdArc>(*fstp, 
                 DeterminizeFstOptions<StdArc>(CacheOptions(true, 0))) ;

	} else if (fstp->Properties(kAcyclic, true)) {

    	// Any acyclic network can be determinized (mathematically speaking)
		// safely.  HOWEVER, to Determinize() a network, i.e. to use the 
		// current Determinize() function supplied in OpenFst, the network
		// must also be Functional.  And there seems to be no test for
		// functionality, at least for now.
    	// To get around this restriction (and avoid crashes), 
    	// Encode() the labels, reducing the network to an acceptor (which
    	// is always functional)

		// KRB: semiring-generalization point
    	EncodeMapper<StdArc> encoder(kEncodeLabels, ENCODE) ;

		// to Encode(), need to pass a pointer to the EncodeMapper
    	Encode(fstp, &encoder) ;
		// KRB:  semiring-generalization point
    	*fstp = DeterminizeFst<StdArc>(*fstp, 
                 DeterminizeFstOptions<StdArc>(CacheOptions(true, 0)));
		// to Decode(), need to pass the encoder (not a ptr to it)
    	Decode(fstp, encoder) ;

	} else {
		// We have a cyclic network.
		//
		// Test for an idempotent semiring, i.e. where Plus(w, w) = w
		// The Tropical "standard" semiring is idempotent
		
		// KRB semiring-generalization point
		// See command-line fstinfo for how to get various kinds of
		//    information about a network, including the Arc type,
		//    which determines the semiring
		//string arcName = getArcName(fstp) ;
		string arcName = "standard" ;

		if (true)  // KRB: should be true only if idempotent
			//arcName == "standard"
			// || arcName == "whatever"
			  {

    		// If we reach here, the network is cyclic AND
    		// the semiring is idempotent.
			//
			// In this case, we need to encode both labels AND weights.
			// ???The semiring of the FST can be tested in C++ using
			// fstp->Type(), e.g.
			// if (fstp->Type() == "standard") 

			// KRB: semiring-generalization point
    		EncodeMapper<StdArc> encoder(kEncodeLabels | kEncodeWeights, ENCODE) ;

    		Encode(fstp, &encoder) ;
			// KRB:  semiring-generalization point
    		*fstp = DeterminizeFst<StdArc>(*fstp, 
                 	DeterminizeFstOptions<StdArc>(CacheOptions(true, 0)));
    		Decode(fstp, encoder) ;

		} 
		// else you can't determinize at all
	}
    return ;
}

// cf to determinizeInPlaceNative
JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_optimizeInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr,
   jboolean determinize,
   jboolean minimize,
   jboolean rmepsilon)
{
	// KRB; semiring-generalization point
	// figure out how to template this on <class Arc>

	// StdVectorFst is a typedef for VectorFst<StdArc>
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;
	//VectorFst<StdArc> * fstp = (VectorFst<StdArc> *)(uintptr_t) fstPtr ;
	// or ???
	//MutableFst<Arc> * fstp = (MutableFst<Arc> *)(uintptr_t) fstPtr ;

	if (      fstp->Properties(kUnweighted, true)
		   && fstp->Properties(kAcceptor, true)
	   ) {

		// Unweighted acceptors are always mathematically safe for 
		//    Determinization.
    	// No need to Encode/Decode in this case
		//
		// Cyril Allauzen recommends determinizing and minimizing before 
		// removing epsilons, if any.  Determinize() and Minimize() treat
		// epsilons like any other hard symbol.

		// Determinize in place (the normal Determinize() algorithm actually
		//   creates a new output Fst)
		//   KRB semiring-generalization point

		if (determinize) {
    		*fstp = DeterminizeFst<StdArc>(*fstp, 
                 DeterminizeFstOptions<StdArc>(CacheOptions(true, 0))) ;
		}

		// The Minimize() algorithm works in place
		if (minimize && fstp->Properties(kIDeterministic, true)) {
    		Minimize(fstp) ;
		}

    	if (rmepsilon && !(fstp->Properties(kNoEpsilons, true))) {

			// if not two-sided epsilon-free
			// RmEpsilon() works in place
			RmEpsilon(fstp) ;

			// And then determinize and minimize again
			// KRB:  semiring-generalization point

			if (determinize) {
    			*fstp = DeterminizeFst<StdArc>(*fstp, 
                 	DeterminizeFstOptions<StdArc>(CacheOptions(true, 0))) ;
			}

			if (minimize && fstp->Properties(kIDeterministic, true)) {
    			Minimize(fstp) ;
			}
		}
	} else if (fstp->Properties(kAcyclic, true)) {

    	// Any acyclic network can be determinized (mathematically speaking)
		// safely.  HOWEVER, to Determinize() a network, i.e. to use the 
		// current Determinize() function supplied in OpenFst, the network
		// must also be Functional.  And there seems to be no test for
		// functionality, at least for now.
    	// To get around this restriction (and avoid crashes), 
    	// Encode() the labels, reducing the network to an acceptor (which
    	// is always functional)

		// KRB: semiring-generalization point
    	EncodeMapper<StdArc> encoder(kEncodeLabels, ENCODE) ;

		if (determinize || minimize) {

			// to Encode(), need to pass a pointer to the EncodeMapper
    		Encode(fstp, &encoder) ;

			if (determinize) {
				// KRB:  semiring-generalization point
    			*fstp = DeterminizeFst<StdArc>(*fstp, 
                 DeterminizeFstOptions<StdArc>(CacheOptions(true, 0)));
			}

			if (minimize && fstp->Properties(kIDeterministic, true)) {
    			Minimize(fstp) ;
			}

			// to Decode(), need to pass the encoder (not a ptr to it)
    		Decode(fstp, encoder) ;
		}

    	if (rmepsilon && !(fstp->Properties(kNoEpsilons, true))) {
			RmEpsilon(fstp) ;

			if (determinize || minimize) {
    			Encode(fstp, &encoder) ;
				if (determinize) {
					// KRB:  semiring-generalization point
    				*fstp = DeterminizeFst<StdArc>(*fstp, 
                 		DeterminizeFstOptions<StdArc>(CacheOptions(true, 0)));
				}
				if (minimize && fstp->Properties(kIDeterministic, true)) {
    				Minimize(fstp) ;
				}
    			Decode(fstp, encoder) ;
			}
		}
	} else {
		// We have a cyclic network.
		//
		// Test for an idempotent semiring, i.e. where Plus(w, w) = w
		// The Tropical "standard" semiring is idempotent
		
		// KRB semiring-generalization point
		// See command-line fstinfo for how to get various kinds of
		//    information about a network, including the Arc type,
		//    which determines the semiring
		//string arcName = getArcName(fstp) ;
		string arcName = "standard" ;

		if (true)  // KRB: should be true only if idempotent
			//arcName == "standard"
			// || arcName == "whatever"
			  {

    		// If we reach here, the network is cyclic AND
    		// the semiring is idempotent.
			//
			// In this case, we need to encode both labels AND weights.
			// ???The semiring of the FST can be tested in C++ using
			// fstp->Type(), e.g.
			// if (fstp->Type() == "standard") 

			// KRB: semiring-generalization point
    		EncodeMapper<StdArc> encoder(kEncodeLabels | kEncodeWeights, ENCODE) ;

			if (determinize || minimize) {

    			Encode(fstp, &encoder) ;

				if (determinize) {
					// KRB:  semiring-generalization point
    				*fstp = DeterminizeFst<StdArc>(*fstp, 
                 		DeterminizeFstOptions<StdArc>(CacheOptions(true, 0)));
				}
				if (minimize && fstp->Properties(kIDeterministic, true)) {
    				Minimize(fstp) ;
				}
    			Decode(fstp, encoder) ;
			}

    		if (rmepsilon && !(fstp->Properties(kNoEpsilons, true))) {
				RmEpsilon(fstp) ;

				if (determinize || minimize) {
    				Encode(fstp, &encoder) ;
					if (determinize) {
						// KRB: semiring-generalization point
    					*fstp = DeterminizeFst<StdArc>(*fstp, 
                 			DeterminizeFstOptions<StdArc>(CacheOptions(true, 0)));
					}
					if (minimize && fstp->Properties(kIDeterministic, true)) {
    					Minimize(fstp) ;
					}
    				Decode(fstp, encoder) ;
				}
			}
		} else {
			// Not currently reachable while using only the Tropical
			// Semiring, which is idempotent

			// The semiring is not idempotent
			if (rmepsilon) {
				RmEpsilon(fstp) ;
			}
		}
	}
    return ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_minimizeInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	// KRB; semiring-generalization point
	// figure out how to template this on <class Arc>

	// StdVectorFst is a typedef for VectorFst<StdArc>
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;
	//VectorFst<StdArc> * fstp = (VectorFst<StdArc> *)(uintptr_t) fstPtr ;
	// or ???
	//MutableFst<Arc> * fstp = (MutableFst<Arc> *)(uintptr_t) fstPtr ;

	if (      fstp->Properties(kUnweighted, true)
		   && fstp->Properties(kAcceptor, true)
	   ) {

		// Unweighted acceptors are always mathematically safe for 
		//    Determinization and Minimization
    	// No need to Encode/Decode in this case

		// The Minimize() algorithm works in place
		// Make sure that the network is really perceived to be
		// deterministic by OpenFst (or Minimize() will crash)
		if (fstp->Properties(kIDeterministic, true)) {
    		Minimize(fstp) ;
		}
	} else if (fstp->Properties(kAcyclic, true)) {

    	// Any acyclic network can be determinized (mathematically speaking)
		// safely.  HOWEVER, to Determinize() a network, i.e. to use the 
		// current Determinize() function supplied in OpenFst, the network
		// must also be Functional.  And there seems to be no test for
		// functionality, at least for now.
    	// To get around this restriction (and avoid crashes), 
    	// Encode() the labels, reducing the network to an acceptor (which
    	// is always functional)

		// KRB: semiring-generalization point
    	EncodeMapper<StdArc> encoder(kEncodeLabels, ENCODE) ;

		// to Encode(), need to pass a pointer to the EncodeMapper
    	Encode(fstp, &encoder) ;

		// Make sure that OpenFst thinks the network is deterministic,
		// or Minimize() will crash
		if (fstp->Properties(kIDeterministic, true)) {
    		Minimize(fstp) ;
		}

		// to Decode(), need to pass the encoder (not a ptr to it)
    	Decode(fstp, encoder) ;
	} else {
		// We have a cyclic network.
		//
		// Test for an idempotent semiring, i.e. where Plus(w, w) = w
		// The Tropical "standard" semiring is idempotent
		
		// KRB semiring-generalization point
		// See command-line fstinfo for how to get various kinds of
		//    information about a network, including the Arc type,
		//    which determines the semiring
		//string arcName = getArcName(fstp) ;
		string arcName = "standard" ;

		if (true)  // KRB: should be true only if idempotent
			//arcName == "standard"
			// || arcName == "whatever"
			  {

    		// If we reach here, the network is cyclic AND
    		// the semiring is idempotent.
			//
			// In this case, we need to encode both labels AND weights.
			// ???The semiring of the FST can be tested in C++ using
			// fstp->Type(), e.g.
			// if (fstp->Type() == "standard") 

			// KRB: semiring-generalization point
    		EncodeMapper<StdArc> encoder(kEncodeLabels | kEncodeWeights, ENCODE) ;

    		Encode(fstp, &encoder) ;

			// Make sure that the network is perceived by OpenFst to
			// be deterministic, or Minimize() will crash.
			if (fstp->Properties(kIDeterministic, true)) {
    			Minimize(fstp) ;
			}
    		Decode(fstp, encoder) ;
		} 
	}
    return ;
}

JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_reverseNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	StdVectorFst * resultFst = new StdVectorFst() ;
	Reverse(*((StdVectorFst *)(uintptr_t) fstPtr), resultFst) ;

    return (jlong)(uintptr_t) resultFst ;
}

JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_shortestPathNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr, jint nshortest)
{
	StdVectorFst * resultFst = new StdVectorFst() ;
	ShortestPath(*((StdVectorFst *)(uintptr_t) fstPtr), resultFst, nshortest) ;

	RmEpsilon(resultFst) ;

    return (jlong)(uintptr_t) resultFst ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_invertInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	Invert((StdVectorFst *)(uintptr_t) fstPtr) ;
    return ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_inputProjectionInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	Project((StdVectorFst *)(uintptr_t) fstPtr, PROJECT_INPUT) ;
    return ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_outputProjectionInPlaceNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	Project((StdVectorFst *)(uintptr_t) fstPtr, PROJECT_OUTPUT) ;
    return ;
}

JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_randGenNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr, jlong npathval, jlong max_lengthval)
{
	// npathval is the number of paths to leave in the resultFst
	// max_lengthval is the maximum length of a path

	StdVectorFst * resultFst = new StdVectorFst() ;
	
	// Using the UniformArcSelector
	RandGen(*((StdVectorFst *)(uintptr_t) fstPtr), resultFst, 
			// N.B. space needed between > and >
			RandGenOptions<UniformArcSelector<StdArc> > (UniformArcSelector<StdArc>(), 
														(int) max_lengthval, 
														(size_t) npathval)) ;

	/* Using the LogProbArcSelector
	RandGen(*((StdVectorFst *)(uintptr_t) fstPtr), resultFst, 
			RandGenOptions<LogProbArcSelector<StdArc> > (LogProbArcSelector<StdArc>(), 
														(int) max_lengthval, 
														(size_t) npathval)) ;
	*/

    return (jlong)(uintptr_t) resultFst ;
}


JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_numPathsNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	if (fstp->Start() == kNoStateId) {
		return 0L ;
	}
	// if the fst has loops, then the language or relation 
	// encoded by the FST is infinite, just return -1
	if (fstp->Properties(kCyclic, true)) {
		return (jlong) -1 ;
	}
	// copy the FST input to a new unweighted network, 
	// over the Log Semiring
	LogVectorFst lfst ;

	// for each original StdArc, if the weight is _not_
	// TropicalWeight::Zero() then change it to LogWeight::One(),
	// else (when the weight is TropicalWeight::Zero()) change it to
	// LogWeight::Zero().  This is handily done with
	// RmWeightMapper(), which is in the library.

	Map(*fstp, &lfst, RmWeightMapper<StdArc, LogArc>()) ;

	vector<LogArc::Weight> distance ;
	ShortestDistance(lfst, &distance, true) ;

	// from the distance vector, get the weight for the start state
	LogArc::Weight w = distance[lfst.Start()] ;

	// w.Value() is the -log of the number of paths,
	// so make w positive and get the exp()
	return (jlong) exp((double)(-1.0 * w.Value())) ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_rmWeightDestFstNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	// semiring generalization point

	// for each original StdArc, if the weight is _not_
	// TropicalWeight::Zero() then change it to TropicalWeight::One(),
	// RmWeightMapper() is in the library.

	// Map in place

	Map(fstp, RmWeightMapper<StdArc>()) ;
}

JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_rmWeightFstNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	// semiring generalization point

	// produce a new unweighted network (doesn't change the
	// 	input network)

	StdVectorFst * newUnweightedFstp = new StdVectorFst() ;

	// for each original StdArc, if the weight is _not_
	// TropicalWeight::Zero() then change it to TropicalWeight::One(),
	// RmWeightMapper() is in the library.

	Map(*fstp, newUnweightedFstp, RmWeightMapper<StdArc, StdArc>()) ;

	// pass back a pointer to the new unweighted fst,
	// cast to jlong 
	return (jlong)(uintptr_t) newUnweightedFstp ;
}

// numStates, numArcs and start return int
// numPaths returns long
JNIEXPORT jint JNICALL
Java_OpenFstLibraryWrapper_numStatesNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
    return (jint) ((StdVectorFst *)(uintptr_t) fstPtr)->NumStates() ;
}

JNIEXPORT jint JNICALL
Java_OpenFstLibraryWrapper_numArcsNative
   (JNIEnv *env, jclass cls,
 	jlong fstPtr)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	jint count = 0 ;
	StateId s ;
	for (StateIterator<StdVectorFst> siter(*fstp) ;
			!siter.Done() ;
			siter.Next()) {
		s = siter.Value() ;
		count += fstp->NumArcs(s) ;
	}
	return count ;
}

JNIEXPORT jint JNICALL
Java_OpenFstLibraryWrapper_startStateNative
	(JNIEnv *env, jclass cls,
	  jlong fstPtr)
{
	return (jint) ((StdVectorFst *)(uintptr_t) fstPtr)->Start() ;
}

JNIEXPORT jint JNICALL
Java_OpenFstLibraryWrapper_addStatesAndArcsNative
  (JNIEnv *env, jclass cls,
   jlong destFstPtr, jlong srcFstPtr)
{
	StdVectorFst * destPtr = (StdVectorFst *)(uintptr_t) destFstPtr ;
	StdVectorFst * srcPtr = (StdVectorFst *)(uintptr_t) srcFstPtr ;

	uint64 propsDest = destPtr->Properties(kFstProperties, false) ;
	uint64 propsSrc  =  srcPtr->Properties(kFstProperties, false) ;

	StateId numStatesDest = destPtr->NumStates() ;

	for (StateIterator<StdVectorFst> siter(*srcPtr) ;
			!siter.Done() ;
			siter.Next()) {
		StateId s_dest = destPtr->AddState() ;
		StateId s_src = siter.Value() ;
		destPtr->SetFinal(s_dest, srcPtr->Final(s_src)) ; 
		for (ArcIterator<StdVectorFst> aiter(*srcPtr, s_src) ;
				!aiter.Done() ;
				aiter.Next()) {
			StdArc arc = aiter.Value() ;  // this is a copy of the orig. arc
			arc.nextstate += numStatesDest ;
			destPtr->AddArc(s_dest, arc) ;
		}
	}
	destPtr->SetProperties(ConcatProperties(propsDest, propsSrc), 
			               kFstProperties) ;
	// return the new number of the added Fst's old start state
    return (jint) (numStatesDest + srcPtr->Start()) ;
}

JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_rrGrammarLinkNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr, jintArray keyarr, jintArray valarr)
{
	// keyarr and valarr are the same length;
	// the values in keyarr are negative integers assigned
	// to $>name references, and stored on Arc labels;
	// Need to find that value in keyarr, get the index,
	// and use that index to retrieve the new nextstate
	// value from valarr

	jint *keys ;
	jint *vals ;
	keys = env->GetIntArrayElements(keyarr, NULL) ;
	if (keys == NULL) {
		return 0L ;  // couldn't allocate the space
	}
	jsize keyLen = env->GetArrayLength(keyarr) ;
	// N.B. release keys below
	vals = env->GetIntArrayElements(valarr, NULL) ;
	if (vals == NULL) {
		return 0L ; // couldn't allocate the space
	}
	// N.B. release vals below
	
	StdVectorFst * rootFstPtr = (StdVectorFst *)(uintptr_t) fstPtr ;

	// Iterate through all the states of the passed-in Fst,
	// and loop through the arcs exiting each state for any
	// negative int values (corresponding to right-linear 
	// references like $>foo)
	for (StateIterator<StdVectorFst> siter(*rootFstPtr) ;
			!siter.Done() ;
			siter.Next()) {
		// s is the int identifier of a state
		StateId s = siter.Value() ;

		// Now loop through the exit arcs of s.
		// Need to use MUTABLEArcIterator to be able to _change_
		// the values on the orig. arcs (this constructor needs a ptr
		// to an Fst rather than the Fst itself); the final stmt
		// in the loop needs to be aiter.SetValue(arc) to set the
		// new values in the Fst

		for (MutableArcIterator<StdVectorFst> aiter(rootFstPtr, s) ;
				!aiter.Done() ;
				aiter.Next()) {

			// arc is (for now) a copy of the original arc
			StdArc arc = aiter.Value() ;

			// if the ilabel is negative, then it corresponds to 
			// some right-linear reference like $>foo
			if (arc.ilabel < 0) {

				// The arc currently leads to a final state,
				// and that final state has a weight
				StateId origArcDest = arc.nextstate ;

				// Copy that that weight onto the modified arc
				arc.weight = rootFstPtr->Final(origArcDest) ;

				int index = -1 ;
				for (int i = 0; i < keyLen; i++) {
					// find the index of the negative value
					// in keys
					if (keys[i] == arc.ilabel) {
						index = i ;
						break ;
					}
				}
				// if index is still -1, then there's an error.

				// Change this arc to an eps:eps arc and repoint it at the
				// start state of the destination network (already
				// "added" to the start network)
				arc.ilabel = 0 ;  // make it an eps:eps arc
				arc.olabel = 0 ;  
				// (label 0 is reserved for epsilon in OpenFst)
			
				// repoint the arc to the start state corresponding to
				// the $>foo arc label
				arc.nextstate = vals[index] ;
				
			} // end of block handling right-linear reference

			// and here, the final step in the loop through the arcs,
			// the modified arc is set in the network being processed
			aiter.SetValue(arc) ;

		} // end of loop through the arcs of a state
	} // end of loop through the states

	// N.B. release these arrays or get a Memory Leak!
	env->ReleaseIntArrayElements(keyarr, keys, 0) ;
	env->ReleaseIntArrayElements(valarr, vals, 0) ;

    return (jlong)(uintptr_t) rootFstPtr ;  // successful return
}

JNIEXPORT jintArray JNICALL
Java_OpenFstLibraryWrapper_getLabelsNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	set<int> sigmaSet ;  // to contain an unknown number of
	// code point values from ilabel and olabel; sets do not
	// contain duplicates

	for (StateIterator<StdVectorFst> siter(*fstp) ;
			!siter.Done() ;
			siter.Next()) {
		StateId s = siter.Value() ;

		for (ArcIterator<StdVectorFst> aiter(*fstp, s); 
				!aiter.Done(); 
				aiter.Next()) {
			const StdArc &arc = aiter.Value() ;
			sigmaSet.insert((int)arc.ilabel) ;
			sigmaSet.insert((int)arc.olabel) ;
		}
	}

	// the sigmaSet will be of some arbitrary size
	int size = sigmaSet.size() ;

	// dynamically allocate an int[] array of that size
	int* nativeIntArray = new int[size] ;  // delete[] below

	int n = 0 ;
	// copy the set into the local int[] array
	for (set<int>::const_iterator iter = sigmaSet.begin() ;
		iter != sigmaSet.end() ;
		iter++) {
		nativeIntArray[n++] = *iter ;
	}

	jintArray retArr = env->NewIntArray(size) ;  
	// creates a jint array in the Java space
	// set the whole Java array directly from the local C++ array
	env->SetIntArrayRegion(retArr, 0, size, (jint *)nativeIntArray) ;

	delete[] nativeIntArray ;

	return retArr ;
}

JNIEXPORT jintArray JNICALL
Java_OpenFstLibraryWrapper_getOutputLabelsNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	set<int> sigmaSet ;  // to contain an unknown number of
	// code point values from ilabel and olabel; sets do not
	// contain duplicates

	for (StateIterator<StdVectorFst> siter(*fstp) ;
			!siter.Done() ;
			siter.Next()) {
		StateId s = siter.Value() ;

		for (ArcIterator<StdVectorFst> aiter(*fstp, s); 
				!aiter.Done(); 
				aiter.Next()) {
			const StdArc &arc = aiter.Value() ;
			sigmaSet.insert((int)arc.olabel) ;
		}
	}

	// the sigmaSet will be of some arbitrary size
	int size = sigmaSet.size() ;

	// dynamically allocate an int[] array of that size
	int* nativeIntArray = new int[size] ;  // delete[] below

	int n = 0 ;
	// copy the set into the local int[] array
	for (set<int>::const_iterator iter = sigmaSet.begin() ;
		iter != sigmaSet.end() ;
		iter++) {
		nativeIntArray[n++] = *iter ;
	}

	jintArray retArr = env->NewIntArray(size) ;  
	// creates a jint array in the Java space
	// set the whole Java array directly from the local C++ array
	env->SetIntArrayRegion(retArr, 0, size, (jint *)nativeIntArray) ;

	delete[] nativeIntArray ;

	return retArr ;
}

JNIEXPORT jint JNICALL
Java_OpenFstLibraryWrapper_hasCyclicDependenciesNative
  (JNIEnv *env, jclass cls,
   jint baseFstInt, jintArray symInts, jlongArray netPtrs)
{
	// symInts and netPtrs are the same length;
	// they represents the pairs needed by ReplaceFst()

	jint  *ints ;
	jlong *ptrs ;
	ints = env->GetIntArrayElements(symInts, NULL) ;
	if (ints == NULL) {
		return (jint)(uintptr_t) -1 ;  // couldn't allocate the space
	}
	jsize vectorLen = env->GetArrayLength(symInts) ;
	ptrs = env->GetLongArrayElements(netPtrs, NULL) ;
	if (ptrs == NULL) {
		return (jint)(uintptr_t) -1 ; // couldn't allocate the space
	}

	// ReplaceFst() must be called with a vector of pairs
	vector< pair< StdArc::Label, const Fst<StdArc>* > > pairlabelfsts ;

	for (int i = 0; i < vectorLen; i++) {
		pairlabelfsts.push_back( pair< StdArc::Label, const Fst<StdArc>* >
						((StdArc::Label)(ints[i]), (Fst<StdArc>*)(ptrs[i])) ) ;
	}

	// N.B. release these arrays or get a Memory Leak!
	env->ReleaseIntArrayElements(symInts, ints, 0) ;
	env->ReleaseLongArrayElements(netPtrs, ptrs, 0) ;

	// test for cyclic dependencies in a proposed replacement
	if (ReplaceFst<StdArc>(pairlabelfsts, baseFstInt).CyclicDependencies()) {
		return (jint)(uintptr_t) 1 ;
	} else {
		return (jint)(uintptr_t) 0 ;
	}
}

JNIEXPORT jlong JNICALL
Java_OpenFstLibraryWrapper_expandRtnNative
  (JNIEnv *env, jclass cls,
   jint baseFstInt, jintArray symInts, jlongArray netPtrs)
{
	// hasCyclicDependencies has already been called to
	// determine that the expansion is legal

	// symInts and netPtrs are the same length;
	// they represents the pairs needed by ReplaceFst()

	jint  *ints ;
	jlong *ptrs ;
	ints = env->GetIntArrayElements(symInts, NULL) ;
	if (ints == NULL) {
		return (jint)(uintptr_t) -1 ;  // couldn't allocate the space
	}
	jsize vectorLen = env->GetArrayLength(symInts) ;
	ptrs = env->GetLongArrayElements(netPtrs, NULL) ;
	if (ptrs == NULL) {
		return (jint)(uintptr_t) -1 ; // couldn't allocate the space
	}

	// ReplaceFst() must be called with a vector of pairs
	vector< pair< StdArc::Label, const Fst<StdArc>* > > pairlabelfsts ;

	for (int i = 0; i < vectorLen; i++) {
		pairlabelfsts.push_back( pair< StdArc::Label, const Fst<StdArc>* >
							((StdArc::Label)(ints[i]), (Fst<StdArc>*)(ptrs[i])) ) ;
	}

	// N.B. release these arrays or get a Memory Leak!
	env->ReleaseIntArrayElements(symInts, ints, 0) ;
	env->ReleaseLongArrayElements(netPtrs, ptrs, 0) ;

	StdVectorFst *fstp = new StdVectorFst() ;

	Replace<StdArc>(pairlabelfsts, fstp, baseFstInt, true) ;

	return (jlong)(uintptr_t) fstp ; 
}

// fstDump used in debugging dump
JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_fstDumpNative
  (JNIEnv *env, jclass cls,
   jlong fstPtrLong)
{
	cout << "Ptr value: " << fstPtrLong << endl ;
	StdVectorFst * fstPtr = (StdVectorFst *)(uintptr_t) fstPtrLong ;
	cout << "State count: " << fstPtr->NumStates() << endl ;
	cout << "Start state: " << fstPtr->Start()     << endl ;


	for (StateIterator<StdVectorFst> siter(*fstPtr) ;
			!siter.Done() ;
			siter.Next()) {
		StateId s = siter.Value() ;

		cout << s  ;
		if (fstPtr->Final(s) != TropicalWeight::Zero()) {
			cout << " FINAL" ;
		} 
		cout << endl ;

		for (ArcIterator<StdVectorFst> aiter(*fstPtr, s); 
				!aiter.Done(); 
				aiter.Next()) {
			const StdArc &arc = aiter.Value() ;
			// in an acceptor, the input and output labels are the same
			cout << "  " << setbase(16) << arc.ilabel << ":" << setbase(16) << arc.olabel << "/" << arc.weight << " -> " << arc.nextstate << endl ;
		}
	}

	return ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_cppDeleteNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr)
{
	// standard C++ delete command for objects
	// allocated on the Heap
#if 1   // normal code
	delete ((StdVectorFst *)(uintptr_t) fstPtr) ;
#else   // code for debugging C++ memory delete issues
    static const char * const pcPML_NO_DELETE = getenv("PML_NO_DELETE");
    if (pcPML_NO_DELETE) {  // skip delete if env var PML_NO_DELETE defined
    }
    else {
        delete ((StdVectorFst *)(uintptr_t) fstPtr) ;
    }
#endif	
	return ;
}

JNIEXPORT jstring JNICALL
Java_OpenFstLibraryWrapper_getFstPtrStringNative
	(JNIEnv *env, jclass cls,
	 jlong fstPtr)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	string s ;

	s += "Info from kleeneopenfst.cc: " ;

	char buf[30] ;
	sprintf(buf, "%llu", (uint64)fstPtr) ;

	s += buf ;

	return env->NewStringUTF(s.c_str()) ;
}

JNIEXPORT jstring JNICALL
Java_OpenFstLibraryWrapper_getShortFstInfoNative
	(JNIEnv *env, jclass cls,
	 jlong fstPtr)
{
	// KRB:  semiring-generalization point
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;
	// create an FstClass object (a class independent of ArcType)
	//fst::script::FstClass fstc(fstp) ;   // KRB worked in 1.3.2 but not 1.3.3
	fst::script::FstClass fstc(*fstp) ;   // need dereference in 1.3.3

	string s ;
	s += "FstType: " ;
	//s += info.FstType() ;  obsolete
	s += fstc.FstType() ;
	s += ", Semiring: " ;
	//s += info.ArcType() ;  obsolete
	s += fstc.ArcType() ;

	return env->NewStringUTF(s.c_str()) ;
}

JNIEXPORT jstring JNICALL
Java_OpenFstLibraryWrapper_getArcTypeNative
	(JNIEnv *env, jclass cls,
	 jlong fstPtr)
{
	// KRB:  semiring-generalization point
	// template <class Arc> ???
	VectorFst<StdArc> * fstp = (VectorFst<StdArc> *)(uintptr_t) fstPtr ;
	//fst::script::FstClass fstc(fstp) ;
	// previous line worked in 1.3.2 but not 1.3.3, see also line 2447
	fst::script::FstClass fstc(*fstp) ;  // need dereference in 1.3.3

	string s = fstc.ArcType() ;

	return env->NewStringUTF(s.c_str()) ;
}

// new version passes back an int array of code point values, converted to symbols and
// concatenated together in Java (which knows about multichar symbols)

JNIEXPORT jintArray JNICALL
Java_OpenFstLibraryWrapper_getSingleStringNative
	(JNIEnv *env, jclass cls,
	 jlong fstPtr)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;
	// when this function is called, the network should already
	// have been determined to be an acceptor, and to encode a
	// single string.
	vector<int> s ;
	StateId state_id = fstp->Start() ;

	if (state_id == kNoStateId) {
		// then the net is empty (denotes the empty language)
		// create an empty jintArray (in the Java space)
		jintArray retArr = env->NewIntArray(0) ;
		return retArr ;
	}

	while (fstp->Final(state_id) == TropicalWeight::Zero()) {
		// should just be one exiting arc, but need to access it through
		// an Iterator?
		for (ArcIterator<StdVectorFst> aiter(*fstp, state_id); !aiter.Done(); aiter.Next()) {
			const StdArc &arc = aiter.Value() ;
			// in an acceptor, the input and output labels are the same
			s.push_back(arc.ilabel) ;
			state_id = arc.nextstate ;  
		}
	}
	// s is a vector of int, each one representing a CPV (could include multichar
	// 	symbols)

	int size = s.size() ;

	// dynamically allocate an int[] array of that size (delete it below)
	int* nativeIntArray = new int[size] ;

	// copy the vector<int> into the local int array
	int n = 0 ;
	for (vector<int>::const_iterator iter = s.begin() ;
			iter != s.end() ;
			iter++) {
		nativeIntArray[n++] = *iter ;
	}

	jintArray retArr = env->NewIntArray(size) ;
	// creates a jint array in the Java space
	// set the whole Java array directly from the local int array
	env->SetIntArrayRegion(retArr, 0, size, (jint *)nativeIntArray) ;

	delete[] nativeIntArray ;
	
	return retArr ; 
}

JNIEXPORT jint JNICALL
Java_OpenFstLibraryWrapper_getSingleArcLabelNative
	(JNIEnv *env, jclass cls,
	 jlong fstPtr)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;
	// when this function is called, the network should already
	// have been determined to be an acceptor, with two states
	// and one arc.  Return the int label on that arc.
	int ilab ;
	for (ArcIterator<StdVectorFst> aiter(*fstp, fstp->Start()); !aiter.Done(); aiter.Next()) {
		const StdArc &arc = aiter.Value() ;
		// in an acceptor, the input and output labels are the same
		ilab = arc.ilabel ;
		break ;      // just get the first (only) arc label
	}
	return (jint) ilab ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_writeBinaryNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr, jstring jstr)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;
	
	// get a null-terminated JNI UTF8-string (just ASCII, in this case)
	const char *cstr = env->GetStringUTFChars(jstr, NULL) ;
	if (cstr == NULL) {
		return ;  // couldn't allocate the space for it
	}

	fstp->Write(cstr) ;

	// release here to avoid a memory leak
	env->ReleaseStringUTFChars(jstr, cstr) ;
	return ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_binary2txtNative
  (JNIEnv *env, jclass cls,
   jstring jstr)
{
	string cmd = "fstprint " ;
	const char *cstr = env->GetStringUTFChars(jstr, NULL) ;
	if (cstr == NULL) {
		return ;   // couldn't allocate the memory
	}
	cmd += cstr ;
	cmd += ".binary " ;
	cmd += cstr ;
	cmd += ".txt" ;
	cout << cmd.c_str() ;
	system("date") ;
	system(cmd.c_str()) ;
	env->ReleaseStringUTFChars(jstr, cstr) ;
	return ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_fst2xmlNative
	(JNIEnv *env, jclass cls,   // because it's a class (static) native method 
								//	of Interpreter
	 jlong fstPtr,              // the Java long representing a ptr to Fst 
	 							//	on the C++ side
	 jobject fstXmlWriter, 		// the FstXmlWriter object passed in--it knows
	 							//  how to write the XML file
	 jint lowestMcsCpv)			// any arc label (int) >= this value is a MCS
{
	// First need to get the class of the fstXmlWriter object
	jclass fstXmlWriterClass = env->GetObjectClass(fstXmlWriter) ;

	// Now get the methodIDs of the following methods of FstXmlWriter
	//      initializeXml
	//		writeArcElmt    (2 versions)
	//		writeFinalElmt  (2 versions)
	//		terminateXml
	
	jmethodID initializeXmlMID = env->GetMethodID(fstXmlWriterClass,
												"initializeXml",
												"(II)V") ;
	if (initializeXmlMID == 0) {
		return ;   // will cause a NoSuchMethodError to be thrown in Java
	}

	jmethodID writeArcElmtMID = env->GetMethodID(fstXmlWriterClass, 
	 											"writeArcElmt", 
												"(IIIIF)V") ;
	if (writeArcElmtMID == 0) {
		return ;
	}

	// second version of writeArcElmtMID (I,I,I,I)V (no weight)
	jmethodID writeArcElmtNeutralWeightMID = env->GetMethodID(fstXmlWriterClass,
															  "writeArcElmt",
															  "(IIII)V") ;
	if (writeArcElmtNeutralWeightMID == 0) {
		return ;
	}

	jmethodID writeFinalElmtMID = env->GetMethodID(fstXmlWriterClass,
												"writeFinalElmt",
												"(IF)V") ;
	if (writeFinalElmtMID == 0) {
		return ;
	}

	// second version of writeFinalElmtMID (I)V  (no weight)
	jmethodID writeFinalElmtNeutralWeightMID = env->GetMethodID(fstXmlWriterClass,
																"writeFinalElmt",
																"(I)V") ;
	if (writeFinalElmtNeutralWeightMID == 0) {
		return ;
	}

	jmethodID terminateXmlMID = env->GetMethodID(fstXmlWriterClass,
												"terminateXml",
												"()V") ;
	if (terminateXmlMID == 0) {
		return ;
	}

	// convert the Long (from Java) into a StdVectorFst ptr
	StdVectorFst *fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	// initialize the XML (headers, etc)
	env->CallVoidMethod(fstXmlWriter, initializeXmlMID, (int) fstp->Start(), (int) fstp->NumStates()) ;

	// now iterate through all the states and arcs, calling methods in
	// fstXmlWriter to write <arc> and <final> elmts

	for (StateIterator<StdVectorFst> siter(*fstp); !siter.Done(); siter.Next()) {

		// should count states from 0 (always--see OpenFst documentation)
		StateId src_state_id = siter.Value() ;

		// now loop through the arcs exiting this state

		for (ArcIterator<StdVectorFst> aiter(*fstp, src_state_id); 
		     !aiter.Done(); 
			 aiter.Next()) 
		{
			const StdArc &arc = aiter.Value() ;  // get the arc
			int i = arc.ilabel ;
			int o = arc.olabel ;
			StateId dest_state_id = arc.nextstate ;
			TropicalWeight w = arc.weight ;

			if (w == TropicalWeight::One()) {       // arc with the neutral weight
				env->CallVoidMethod(fstXmlWriter, writeArcElmtNeutralWeightMID,
									src_state_id, dest_state_id, i, o) ;
			} else {
				// arc with non-neutral weight
				env->CallVoidMethod(fstXmlWriter, writeArcElmtMID, src_state_id,
									dest_state_id, i, o, (float) w.Value()) ;
			}
		}

		TropicalWeight state_weight = fstp->Final(src_state_id) ;
		if (state_weight != TropicalWeight::Zero()) {
			// then it's a final state
			if (state_weight == TropicalWeight::One()) {  // final state with neutral weight
				env->CallVoidMethod(fstXmlWriter, writeFinalElmtNeutralWeightMID, src_state_id) ;
			} else {
				// final state with non-neutral weight
				env->CallVoidMethod(fstXmlWriter, writeFinalElmtMID, src_state_id,
			(float) state_weight.Value()) ;
			}
		}
	}
	// finally, terminate the XML
	env->CallVoidMethod(fstXmlWriter, terminateXmlMID) ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_fst2xmlStateOrientedNative
	(JNIEnv *env, jclass cls,   // because it's a class (static) native method 
								//	of Interpreter
	 jlong fstPtr,              // the Java long representing a ptr to Fst 
	 							//	on the C++ side
	 jobject fstXmlWriterStateOriented, // the FstXmlWriterStateOriented object passed in--it knows
	 							//  how to write the XML file
	 jint lowestMcsCpv)			// any arc label (int) >= this value is a MCS
{
	// First need to get the class of the fstXmlWriterStateOriented object
	jclass fstXmlWriterClass = env->GetObjectClass(fstXmlWriterStateOriented) ;
	// then use this class to get the IDs of various named methods (used
	// for callbacks)

	jmethodID initializeXmlMID = env->GetMethodID(fstXmlWriterClass,
												"initializeXml",
												"()V") ;
	if (initializeXmlMID == 0) {
		return ;   // will cause a NoSuchMethodError to be thrown in Java
	}

	//

	jmethodID startStatesElmtMID = env->GetMethodID(fstXmlWriterClass,
													"startStatesElmt",
													"(II)V") ;
	if (startStatesElmtMID == 0) {
		return ;   // will cause a NoSuchMethodError to be thrown in Java
	}

	//

	jmethodID endStatesElmtMID = env->GetMethodID(fstXmlWriterClass,
													"endStatesElmt",
													"()V") ;
	if (endStatesElmtMID == 0) {
		return ;   // will cause a NoSuchMethodError to be thrown in Java
	}

	//

	jmethodID startStateElmtMID = env->GetMethodID(fstXmlWriterClass,
													"startStateElmt",
													"(I)V") ;
	if (startStateElmtMID == 0) {
		return ;   // will cause a NoSuchMethodError to be thrown in Java
	}

	//

	jmethodID startStateFinalElmtMID = env->GetMethodID(fstXmlWriterClass,
													"startStateFinalElmt",
													"(IF)V") ;
	if (startStateFinalElmtMID == 0) {
		return ;   // will cause a NoSuchMethodError to be thrown in Java
	}

	//

	jmethodID startStateFinalElmtNeutralWeightMID = env->GetMethodID(fstXmlWriterClass,
													"startStateFinalElmtNeutralWeight",
													"(I)V") ;
	if (startStateFinalElmtNeutralWeightMID == 0) {
		return ;   // will cause a NoSuchMethodError to be thrown in Java
	}

	//

	jmethodID endStateElmtMID = env->GetMethodID(fstXmlWriterClass,
												"endStateElmt",
												"()V") ;
	if (endStateElmtMID == 0) {
		return ;   // will cause a NoSuchMethodError to be thrown in Java
	}

	//

	jmethodID writeArcElmtMID = env->GetMethodID(fstXmlWriterClass, 
	 											"writeArcElmt", 
												"(IIIF)V") ;
	if (writeArcElmtMID == 0) {
		return ;
	}

	//

	// second version of writeArcElmtMID (I,I,I)V (no weight)
	jmethodID writeArcElmtNeutralWeightMID = env->GetMethodID(fstXmlWriterClass,
															  "writeArcElmt",
															  "(III)V") ;
	if (writeArcElmtNeutralWeightMID == 0) {
		return ;
	}

	//

	jmethodID terminateXmlMID = env->GetMethodID(fstXmlWriterClass,
												"terminateXml",
												"()V") ;
	if (terminateXmlMID == 0) {
		return ;
	}

	// convert the Long (from Java) into a StdVectorFst ptr
	StdVectorFst *fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	// The Call-Backs start here

	// initialize the XML (headers, etc)
	env->CallVoidMethod(fstXmlWriterStateOriented, initializeXmlMID) ;
	
	// start the <states> element, with attrs showing the start state and
	// the total number of states
	env->CallVoidMethod(fstXmlWriterStateOriented, startStatesElmtMID, (int) fstp->Start(), (int) fstp->NumStates()) ;

	for (StateIterator<StdVectorFst> siter(*fstp); !siter.Done(); siter.Next()) {

		// should count states from 0 (always--see OpenFst documentation), but
		// state 0 is not necessarily the start state
		StateId src_state_id = siter.Value() ;

		TropicalWeight state_weight = fstp->Final(src_state_id) ;
		if (state_weight != TropicalWeight::Zero()) {
			// then it's a final state
			if (state_weight == TropicalWeight::One()) {  // final state with neutral weight
				env->CallVoidMethod(fstXmlWriterStateOriented, startStateFinalElmtNeutralWeightMID, (int) src_state_id) ;
			} else {
				// final state with non-neutral weight
				env->CallVoidMethod(fstXmlWriterStateOriented, startStateFinalElmtMID, (int) src_state_id, (float) state_weight.Value()) ;
			}
		} else {
			env->CallVoidMethod(fstXmlWriterStateOriented, startStateElmtMID, (int) src_state_id) ;
		}


		// now loop through the arcs exiting this state

		for (ArcIterator<StdVectorFst> aiter(*fstp, src_state_id); 
		     !aiter.Done(); 
			 aiter.Next()) 
		{
			const StdArc &arc = aiter.Value() ;  // get the arc
			int i = arc.ilabel ;
			int o = arc.olabel ;
			StateId dest_state_id = arc.nextstate ;
			TropicalWeight w = arc.weight ;

			if (w == TropicalWeight::One()) {       // arc with the neutral weight
				env->CallVoidMethod(fstXmlWriterStateOriented, writeArcElmtNeutralWeightMID,
									dest_state_id, i, o) ;
			} else {
				// arc with non-neutral weight
				env->CallVoidMethod(fstXmlWriterStateOriented, writeArcElmtMID,
									dest_state_id, i, o, (float) w.Value()) ;
			}
		}
		env->CallVoidMethod(fstXmlWriterStateOriented, endStateElmtMID) ;
	}
	// finally, terminate the XML
	env->CallVoidMethod(fstXmlWriterStateOriented, endStatesElmtMID) ;
	env->CallVoidMethod(fstXmlWriterStateOriented, terminateXmlMID) ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_fst2dotNative
	(JNIEnv *env, jclass cls,   // because it's a class (static) native method 
								//	of Interpreter
	 jlong fstPtr,              // the Java long representing a ptr to Fst 
	 							//	on the C++ side
	 jobject fstDotWriter 		// the FstDotWriter object passed in--it knows
	 							//  how to write the DOT source file
	)
{
	// First need to get the class of the fstDotWriter object
	jclass fstDotWriterClass = env->GetObjectClass(fstDotWriter) ;

	// Now get the methodIDs of the following methods of FstDotWriter
	//      initializeDot
	//		writeDotState
	//		writeDotArc  (2 versions)
	//		terminateDot
	
	jmethodID initializeDotMID = env->GetMethodID(fstDotWriterClass,
												"initializeDot",
												"()V") ;
	if (initializeDotMID == 0) {
		return ;   // will cause a NoSuchMethodError to be thrown in Java
	}

	jmethodID writeDotStateMID = env->GetMethodID(fstDotWriterClass, 
	 											"writeDotState", 
												"(IZZF)V") ;  // Z for boolean!
	if (writeDotStateMID == 0) {
		return ;
	}

	jmethodID writeDotStateNeutralWeightMID = env->GetMethodID(fstDotWriterClass, 
	 											"writeDotState", 
												"(IZZ)V") ;  // Z for boolean!
	if (writeDotStateNeutralWeightMID == 0) {
		return ;
	}


	// first version of writeArcElmtMID (IIIIF)V (with weight)
	jmethodID writeDotArcMID = env->GetMethodID(fstDotWriterClass,
												"writeDotArc",
												"(IIIIF)V") ;
	if (writeDotArcMID == 0) {
		return ;
	}

	// second version of writeArcElmtMID (IIII)V (no weight)
	jmethodID writeDotArcNeutralWeightMID = env->GetMethodID(fstDotWriterClass,
															  "writeDotArc",
															  "(IIII)V") ;
	if (writeDotArcNeutralWeightMID == 0) {
		return ;
	}

	jmethodID terminateDotMID = env->GetMethodID(fstDotWriterClass,
												"terminateDot",
												"()V") ;
	if (terminateDotMID == 0) {
		return ;
	}

	// convert the Long (from Java) into a StdVectorFst ptr
	StdVectorFst *fstp = (StdVectorFst *)(uintptr_t) fstPtr ;
	// get the start state id
	int start_id = fstp->Start() ;

	// call-back to Java method
	// initialize the DOT source (headers, etc)
	env->CallVoidMethod(fstDotWriter, initializeDotMID) ;

	// now iterate through all the states and arcs, calling methods in
	// fstDotWriter to write state and arc code
	//
	bool startb = false ;
	bool finalb = false ;
	float fweight = 0.0 ;
	
	for (StateIterator<StdVectorFst> siter(*fstp); !siter.Done(); siter.Next()) {

		// should count states from 0 (always--see OpenFst documentation)
		StateId src_state_id = siter.Value() ;

		startb = false ;
		finalb = false ;

		if (src_state_id == start_id) {
			startb = true ;
		}
		if (fstp->Final(src_state_id) != TropicalWeight::Zero()) {
			// then it's a final state
			finalb = true ;
		}

		TropicalWeight weight = fstp->Final(src_state_id) ;
		if (weight == TropicalWeight::One()) {
			// then it's the neutral weight
			env->CallVoidMethod(fstDotWriter, writeDotStateNeutralWeightMID, src_state_id, startb, finalb) ;
		} else {
			// it's a non-neutral weight
			float weightf = weight.Value() ;
			env->CallVoidMethod(fstDotWriter, writeDotStateMID, src_state_id, startb, finalb, weightf) ;
		}

		// now loop through the arcs exiting this state

		for (ArcIterator<StdVectorFst> aiter(*fstp, src_state_id); 
		     !aiter.Done(); 
			 aiter.Next()) 
		{
			const StdArc &arc = aiter.Value() ;  // get the arc
			int i = arc.ilabel ;
			int o = arc.olabel ;
			StateId dest_state_id = arc.nextstate ;
			TropicalWeight w = arc.weight ;

			if (w == TropicalWeight::One()) {       // arc with the neutral weight
				env->CallVoidMethod(fstDotWriter, writeDotArcNeutralWeightMID,
									src_state_id, dest_state_id, i, o) ;
			} else {
				// arc with non-neutral weight
				env->CallVoidMethod(fstDotWriter, writeDotArcMID, src_state_id,
									dest_state_id, i, o, (float) w.Value()) ;
			}
		}

	}
	// finally, terminate the XML
	env->CallVoidMethod(fstDotWriter, terminateDotMID) ;
}

// 2010-11-03 not used
JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_checkSapRtnMappingsNative
	(JNIEnv *env, jclass cls,   // because it's a class (static) native method 
								//	of Interpreter
	 jlong fstPtr,              // the Java long representing a ptr to Fst 
	 							//	on the C++ side
	 jobject sapRtnMappingsChecker, 		// object passed in--has callbacks
	 jint lowestMcsCpv)			// any arc label (int) >= this value is a MCS
{
	// First need to get the class of the sapRtnMappingsChecker object
	jclass sapRtnMappingsCheckerClass =
		env->GetObjectClass(sapRtnMappingsChecker) ;

	// Now get the methodIDs of the following callback 
	//	methods of SapRtnMappingsChecker
	//      upperCpv(int i)
	//		lowerCpv(int o)

	jmethodID inputCpvMID = env->GetMethodID(sapRtnMappingsCheckerClass,
												"inputCpv",
												"(I)V") ;
	if (inputCpvMID == 0) {
		return ;   // will cause a NoSuchMethodError to be thrown in Java
	}

	jmethodID outputCpvMID = env->GetMethodID(sapRtnMappingsCheckerClass,
												"outputCpv",
												"(I)V") ;
	if (outputCpvMID == 0) {
		return ;   // will cause a NoSuchMethodError to be thrown in Java
	}

	// convert the Long (from Java) into a StdVectorFst ptr
	StdVectorFst *fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	// now iterate through all the states and arcs, calling callback methods

	for (StateIterator<StdVectorFst> siter(*fstp); !siter.Done(); siter.Next()) {

		// should count states from 0 (always--see OpenFst documentation)
		StateId src_state_id = siter.Value() ;

		// now loop through the arcs exiting this state

		for (ArcIterator<StdVectorFst> aiter(*fstp, src_state_id); 
		     !aiter.Done(); 
			 aiter.Next()) 
		{
			const StdArc &arc = aiter.Value() ;  // get the arc
			Label i = arc.ilabel ;
			Label o = arc.olabel ;

			// no possible problem with identity mappings or
			// mappings to epsilon
			if (i == o || o == 0) {
				continue ;
			}

			if (i >= lowestMcsCpv || o >= lowestMcsCpv) {
				// two callbacks
				env->CallVoidMethod(sapRtnMappingsChecker, inputCpvMID,  (jint)i) ;
				env->CallVoidMethod(sapRtnMappingsChecker, outputCpvMID, (jint)o) ;
			}
		}
	}
}

// KRB:  ask Phil if the first param should be
// StdVectorFst * (uintptr_t) fstp
void listAllStringsHelper(StdVectorFst * fstp, StateId state, 
		Weight cost, int projection, JNIEnv *env, jobject stringLister, 
		jmethodID pushMID, jmethodID popMID, jmethodID emitMID) {

		if (fstp->Final(state) != Weight::Zero()) {
			// KRB:  semiring generalization point?
			// then the current state is final; emit a string (with its weight)
			env->CallVoidMethod(stringLister, emitMID, (jfloat) (Times(cost, fstp->Final(state)).Value())) ;
		}
		// now look for exit arcs from this state
		for (ArcIterator<StdVectorFst> aiter(*fstp, state); !aiter.Done(); aiter.Next()) {
			// get the current arcs
			const StdArc &arc = aiter.Value() ;

			if (projection == 0) {  // input side, push a label
				env->CallVoidMethod(stringLister, pushMID, (jint) arc.ilabel) ;
			} else {                // output side, push a label
				env->CallVoidMethod(stringLister, pushMID, (jint) arc.olabel) ;
			}
			// then recursively call, following the exit arc to a new state
			// KRB:  semiring generalization point?
			listAllStringsHelper(fstp, arc.nextstate, (Times(cost, arc.weight)), projection,
					env, stringLister, pushMID, popMID, emitMID) ;
			env->CallVoidMethod(stringLister, popMID) ;
		}
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_listAllStringsNative
	(JNIEnv *env, jclass cls,
	 jlong fstPtr,
	 jint projection,  // 0 for input, 1 for output
	 jobject stringLister)   // Java object that knows how to display Strings
{
	// it is up to the calling program to call numPaths to make
	// sure that the language/relation is finite
	StdVectorFst *fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	// stringLister is a handle to an object that implements StringLister
	jclass stringListerClass = env->GetObjectClass(stringLister) ;

	jmethodID pushMID = env->GetMethodID(stringListerClass, "push", "(I)V") ;
	if (pushMID == 0) {
		return ;
	}

	jmethodID popMID = env->GetMethodID(stringListerClass, "pop", "()V") ;
	if (popMID == 0) {
		return ;
	}

	jmethodID emitMID = env->GetMethodID(stringListerClass, "emit", "(F)V") ;
	if (emitMID == 0) {
		return ;
	}

	// start with the extension identity weight (0.0 for the tropical semiring)
	// KRB: semiring generalization point?
	listAllStringsHelper(fstp, fstp->Start(), Weight::One(), projection, 
			env, stringLister, pushMID, popMID, emitMID) ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_iterate4mcsNative
	(JNIEnv *env, jclass cls,   // because it's a class (static) native method 
								//	of Interpreter
	 jlong fstPtr,              // the Java long representing a ptr to Fst 
	 							//	on the C++ side
	 jobject ttb, 		// the TranslitTokenizerBuilder object passed in
	 jint lowestMcsCpv)			// any arc label (int) >= this value is a MCS
{
	// This function will make calls back to the Java object ttb;
	// First need to get the _class_ of the TranslitTokenizerBuilder object
	jclass translitTokenizerBuilderClass = env->GetObjectClass(ttb) ;

	// Now get the methodID of the .registerMcsInput() and .registerMcsOutput()
	// methods
	jmethodID registerMcsInputMID = env->GetMethodID(translitTokenizerBuilderClass, 
	 											"registerMcsInput", 
												"(I)V") ;
	jmethodID registerMcsOutputMID = env->GetMethodID(translitTokenizerBuilderClass, 
	 											"registerMcsOutput", 
												"(I)V") ;

	if (registerMcsInputMID == 0) {
		return ;  // will cause a NoSuchMethodError to be thrown in Java
	}
	if (registerMcsOutputMID == 0) {
		return ;
	}

	set<int> mcsCpvSetInput ;   // to hold the upper ("input") Multichar Symbol CPVs already found
	set<int> mcsCpvSetOutput ;  // to hold the lower ("output") Multichar Symbol CPVs already found

	set<int>::iterator it ;

	// convert the Long (from Java) into a StdVectorFst ptr
	StdVectorFst *fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	// now iterate through all the states and arcs, just looking for multichar
	// symbols; calling registerMcsInputMID or registerMcsOutputMID each 
	// time a new MCS is found

	for (StateIterator<StdVectorFst> siter(*fstp); !siter.Done(); siter.Next()) {
		// get a state
		StateId src_state_id = siter.Value() ;

		// now loop through the arcs exiting this state
		for (ArcIterator<StdVectorFst> aiter(*fstp, src_state_id); 
		     !aiter.Done(); 
			 aiter.Next()) 
		{
			const StdArc &arc = aiter.Value() ;  // get the arc
			int i = arc.ilabel ;  // get the input-side label
			if (i >= lowestMcsCpv) {
				// then it represents a multichar symbol (on the upper/input side)
				it = mcsCpvSetInput.find(i) ;
				if (it == mcsCpvSetInput.end()) {
					// this Cpv has not been seen yet (on the upper/input side)
					mcsCpvSetInput.insert(i) ;
					// call back to the registerMcsInput method of the ttb object
					env->CallVoidMethod(ttb, registerMcsInputMID, i) ;
				}
			}
			int o = arc.olabel ;  // get the output-side label
			if (o >= lowestMcsCpv) {
				// then it represents a multichar symbol (on the lower/output side)
				it = mcsCpvSetOutput.find(o) ;
				if (it == mcsCpvSetOutput.end()) {
					// this Cpv has not been seen yet (on the lower/output side)
					mcsCpvSetOutput.insert(o) ;
					// call back to the registerMcsOutput method of the ttb object
					env->CallVoidMethod(ttb, registerMcsOutputMID, o) ;
				}
			}
		}
	}
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_addStatesNative
	(JNIEnv *env, jclass cls, 
	 jlong fst, jint stateCount)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fst ;

	for (int i = 0; i < stateCount; i++) {
		fstp->AddState() ;
	}
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_setStartNative
	(JNIEnv *env, jclass cls, 
	 jlong fst, jint state_id)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fst ;

	fstp->SetStart(state_id) ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_setFinalNative
	(JNIEnv *env, jclass cls, 
	 jlong fst, jint state_id, jfloat w)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fst ;

	fstp->SetFinal(state_id, w) ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_setFinalNeutralWeightNative
	(JNIEnv *env, jclass cls, 
	 jlong fst, jint state_id)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fst ;

	fstp->SetFinal(state_id, TropicalWeight::One()) ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_addArcNative
	(JNIEnv *env, jclass cls, 
	 jlong fst, jint src_id, jint i, jint o, jfloat w, jint dest_id)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fst ;

	fstp->AddArc(src_id, StdArc(i, o, w, dest_id)) ;
}

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_addArcNeutralWeightNative
	(JNIEnv *env, jclass cls, 
	 jlong fst, jint src_id, jint i, jint o, jint dest_id)
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fst ;

	fstp->AddArc(src_id, StdArc(i, o, TropicalWeight::One(), dest_id)) ;
}

// N.B.  OTHER_ID:OTHER_NONID and OTHER_ID:c are illegal
// OTHER_NONID:c does not include c:c

JNIEXPORT void JNICALL
Java_OpenFstLibraryWrapper_expandOtherArcsNative
	(JNIEnv *env, jclass cls,  	// JNI boilerplate

	 jlong fstPtr,			// ptr to the network to modify
	 jintArray intArray,	// ints (symbols) to expand OTHER
	 jint otherIDlabel,		// int used for OTHER_ID
	 jint otherNonIDlabel)	// int used for OTHER_NONID
{
	StdVectorFst *fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	// standard JNI boilerplate to access the int array passed from Java
	jboolean isCopy ;
	jint * intArrayElements = 
			env->GetIntArrayElements(intArray, &isCopy) ;
	jint arrSize = env->GetArrayLength(intArray) ; 
	// index as intArrayElements[n]

	int clabel ;
	int dlabel ; 

	// empty vector to store pointers to ArcInfo objects "to be added"
	vector<ArcInfo *> arcsToAdd ;
	vector<ArcInfo *>::iterator iter ;

	// Now loop through all the states of the network
	for (StateIterator<StdFst> siter(*fstp); !siter.Done(); siter.Next()) {
		StateId src_state_id = siter.Value() ;  // basically an int
		// now loop through the arcs exiting src_state_id;
		// Note that in OpenFst an ArcIterator is invalidated if the 
		// number of states or arcs is modified; so need to iterate through, 
		// keeping a record of arcs to be added, in the arcsToAdd vector, 
		// and add them after the ArcIterator is through.

		for (ArcIterator<StdFst> aiter(*fstp, src_state_id); 
				!aiter.Done(); aiter.Next()) {
			const StdArc &arc = aiter.Value() ;
			Label ilabel = arc.ilabel ;
			Label olabel = arc.olabel ;
			Weight w = arc.weight ;
			StateId dest_state_id = arc.nextstate ;

			if (ilabel == otherNonIDlabel) {
				// both OTHER_NONID:o and OTHER_NONID:OTHER_NONID are possible
				// (OTHER_NONID:OTHER_ID is impossible)

				if (olabel != otherNonIDlabel) {
					// we have an OTHER_NONID:o label;
					// Add a parallel c:o arc for each c in intArrayElements
					for (int n = 0; n < arrSize; n++) {
						clabel = intArrayElements[n] ;
						// add a parallel c:o arc
						arcsToAdd.push_back(new ArcInfo(src_state_id, 
									clabel, olabel, w, dest_state_id)) ;
					}
				} else {
					// we have an OTHER_NONID:OTHER_NONID label;
					// Add a c:OTHER_NONID and an OTHER_NONID:c arc
					//   for all c in intArrayElements; 
					// Also, add a:b and b:a for all non-ident a
					//	 and b in the intArrayElements
					for (int n = 0; n < arrSize; n++) {
						clabel = intArrayElements[n] ;
						// add a parallel c:OTHER_NONID arc
						arcsToAdd.push_back(new ArcInfo(src_state_id,
									clabel, otherNonIDlabel, w, dest_state_id)) ;
						// and add a parallel OTHER_NONID:c arc
						arcsToAdd.push_back(new ArcInfo(src_state_id,
									otherNonIDlabel, clabel, w, dest_state_id)) ;
						// debug
						for (int m = n+1; m < arrSize; m++) {
							dlabel = intArrayElements[m] ;
							// add a:b
							arcsToAdd.push_back(new ArcInfo(src_state_id,
							clabel, dlabel, w, dest_state_id)) ;
							// add b:a
							arcsToAdd.push_back(new ArcInfo(src_state_id,
							dlabel, clabel, w, dest_state_id)) ;
						}
					}
				}
			} else if (olabel == otherNonIDlabel) {
				// then we have an i:OTHER_NONID label;
				// Add an i:c arc for all c in intArrayElements
				for (int n = 0; n < arrSize; n++) {
					clabel = intArrayElements[n] ;
					// add a parallel i:c arc
					arcsToAdd.push_back(new ArcInfo(src_state_id,
								ilabel, clabel, w, dest_state_id)) ;
				}
			} else if (ilabel == otherIDlabel) {
				// then we have an OTHER_ID:OTHER_ID label;
				// Add a c:c arc for each c in intArrayElements
				// (remember that OTHER_ID:OTHER:NON_ID, OTHER_NONID:OTHER_ID, 
				// i:OTHER_ID and OTHER_ID:o are illegal)
				for (int n = 0; n < arrSize; n++) { 
					clabel = intArrayElements[n] ;
					// add a parallel c:c arc
					arcsToAdd.push_back(new ArcInfo(src_state_id,
								clabel, clabel, w, dest_state_id)) ;
				}
			}
		}  // End loop through Arcs

		// Now actually add the new arcs to the network.  
		// (There will be at least one arc to add.)
		for (iter = arcsToAdd.begin(); iter != arcsToAdd.end(); iter++) {
			ArcInfo * aip = *iter ;
			fstp->AddArc(aip->src_state_id, 
					StdArc(aip->ilabel, aip->olabel, aip->weight, aip->dest_state_id)) ;
		}
		arcsToAdd.clear() ;
	}  // End loop through States

	// standard JNI boilerplate to release memory used to store the int array
	if (isCopy == JNI_TRUE) {
		env->ReleaseIntArrayElements(intArray, intArrayElements, 0) ;
	}
	// void function, no return
}

bool isCombiningDiacritic(UChar32 cpv) {
	if (
			(cpv >= 0x0300 && cpv <= 0x036F)
			// Combining Diacritical Marks

		||	(cpv >= 0x1DC0 && cpv <= 0x1DFF)	
			// Combining Diacritical Marks Supplement

		||	(cpv >= 0x20D0 && cpv <= 0x20FF)
			// Combining Diacritical Marks for Symbols

		||	(cpv >= 0xFE20 && cpv <= 0xFE2F) 
			// Combining Half Marks
		) {
		return true ;
	}
	return false ;
}

UChar32 testPrecomposed(UChar32 label, 
					UnicodeString & ustr, 
					Normalizer *normp, 
					UErrorCode errorCode) {
	if (u_isalpha(label) && label >= 0x00C0) {
		ustr.setTo(label) ;
		normp->setText(ustr, errorCode) ;
		UChar32 first = normp->first() ;
		UChar32 possDiacLabel = normp->next() ;
		if (first != label && isCombiningDiacritic(possDiacLabel)) {
			return first ;
		} 
	}
	return (UChar32) -1 ;
}

jintArray set2jintArray(JNIEnv *env, set<int> sigmaAddedSet) {
	// get the size of the set
	int size = sigmaAddedSet.size() ;

	// dynamically allocate an int[] array of that size
	int* nativeIntArray = new int[size] ;

	int n = 0 ;
	// copy the set into the int[] array
	for (set<int>::const_iterator iter = sigmaAddedSet.begin() ;
		iter != sigmaAddedSet.end() ;
		iter++) {
		nativeIntArray[n++] = *iter ;
	}

	jintArray retArr = env->NewIntArray(size) ;
	// creates a jint array in the Java space
	// set the whole Java array directly from the local C++ array
	env->SetIntArrayRegion(retArr, 0, size, (jint *)nativeIntArray) ;
	delete[] nativeIntArray ;

	return retArr ;
}


// add diacritic insensitivity to a network
JNIEXPORT jintArray JNICALL
Java_OpenFstLibraryWrapper_addDiacNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr,
   jboolean input,	// on the input side
   jboolean output)	// on the output side
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	set<int> sigmaAddedSet ;

	// empty vector to store pointers to ArcInfo objects (to be
	// added after the Iterators are safely finished)
	vector<ArcInfo *> arcsToAdd ;
	vector<ArcInfo *>::iterator iter ;

	StateId src_state_id ;

	UChar32 ilabel, olabel, newIlabel, newOlabel ;

	Normalizer * normp = new Normalizer("", UNORM_NFD) ;
	UErrorCode errorCode = U_ZERO_ERROR ;
	UnicodeString ustr ;

	for (StateIterator<StdVectorFst> siter(*fstp) ;
			!siter.Done() ;
			siter.Next()) {
		// src_state_id is the int identifier of a state

		src_state_id = siter.Value() ;

		// arcs may need to be added, but not modified in place;
		// so use an ArcIterator rather than MutableArcIterator

		for (ArcIterator<StdVectorFst> aiter(*fstp, src_state_id) ;
				!aiter.Done() ;
				aiter.Next()) {
			
			const StdArc &arc = aiter.Value() ;

			ilabel = (UChar32) arc.ilabel ;
			olabel = (UChar32) arc.olabel ;

			if (input) {
				if (isCombiningDiacritic(ilabel)) {
					// add a parallel arc with epsilon on the input side
					arcsToAdd.push_back(new ArcInfo(
									src_state_id,
									(Label) 0,  // epsilon
									(Label) olabel,
									arc.weight,
									arc.nextstate)) ;
				} else {
					newIlabel = testPrecomposed(ilabel, ustr, normp, errorCode) ;
					if (newIlabel > 0) {
						sigmaAddedSet.insert((int) newIlabel) ;
						arcsToAdd.push_back(new ArcInfo(
										src_state_id,
										(Label) newIlabel,
										(Label) olabel,
										arc.weight,
										arc.nextstate)) ;
					}
				}
			}
			if (output) {
				if (isCombiningDiacritic(olabel)) {
					// add a parallel arc with epsilon on the output side
					arcsToAdd.push_back(new ArcInfo(
									src_state_id,
									(Label) ilabel,
									(Label) 0,  // epsilon
									arc.weight,
									arc.nextstate)) ;
				} else {
					newOlabel = testPrecomposed(olabel, ustr, normp, errorCode) ;
					if (newOlabel > 0) {
						sigmaAddedSet.insert((int) newOlabel) ;
						arcsToAdd.push_back(new ArcInfo(
										src_state_id,
										(Label) ilabel,
										(Label) newOlabel,
										arc.weight,
										arc.nextstate)) ;
					}
				}
			}
			if (input && output) {
				if (isCombiningDiacritic(ilabel) && isCombiningDiacritic(olabel)) {
					// add a parallel eps:eps arc
					arcsToAdd.push_back(new ArcInfo(
												src_state_id,
												(Label) 0,
												(Label) 0,
												arc.weight,
												arc.nextstate)) ;
				} else {
					newIlabel = testPrecomposed(ilabel, ustr, normp, errorCode) ;
					newOlabel = testPrecomposed(olabel, ustr, normp, errorCode) ;
					if (newIlabel > 0 && newOlabel > 0) {
						sigmaAddedSet.insert((int) newIlabel) ;
						sigmaAddedSet.insert((int) newOlabel) ;
						arcsToAdd.push_back(new ArcInfo(
												src_state_id,
												(Label) newIlabel,
												(Label) newOlabel,
												arc.weight,
												arc.nextstate)) ;
					}
				}
			}
		} // end of loop through the arcs of a state
	} // end of loop through the states

	delete normp ; 

	// add the arcsToAdd here
	for (iter = arcsToAdd.begin(); iter != arcsToAdd.end(); iter++) {
		ArcInfo * aip = *iter ;
		fstp->AddArc(aip->src_state_id, 
			StdArc(aip->ilabel, aip->olabel, aip->weight, aip->dest_state_id)) ;
	}

	return set2jintArray(env, sigmaAddedSet) ;
}


JNIEXPORT jintArray JNICALL
Java_OpenFstLibraryWrapper_addCaseNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr,
   jboolean all,  	// true for All, false for init only
   jboolean add_uc,	// add uppercase arcs (for existing lc)
   jboolean add_lc,	// add lowercase arcs (for existing uc)
   jboolean input,	// on the input side
   jboolean output)	// on the output side
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	// addCaseNative typically causes arcs with new labels to
	// be added to the network; need to collect and return the
	// set of (potentially) new symbols (more precisely, 
	// their int code point values)
	set<int> sigmaAddedSet ;  // use a set to filter out duplicates

	// empty vector to store pointers to ArcInfo objects (to be
	// added after the Iterators are safely finished)
	vector<ArcInfo *> arcsToAdd ;
	vector<ArcInfo *>::iterator iter ;

	bool do_all = true ; // do at least the 1st loop

	StateId src_state_id ;

	UChar32 ilabel, olabel, newIlabel, newOlabel ;

	for (StateIterator<StdVectorFst> siter(*fstp) ;
			do_all && !siter.Done() ;
			siter.Next()) {
		// src_state_id is the int identifier of a state

		if (!all) {
			// just do the first symbols, i.e. from the start state
			// which is not necessarily the first returned by the
			// iterator
			do_all = false ;
			src_state_id = fstp->Start() ;
			// just do the arcs exiting the start state
		} else {
			src_state_id = siter.Value() ;
		}

		// arcs may need to be added, but not modified in place;
		// so use an ArcIterator rather than MutableArcIterator
	
		for (ArcIterator<StdVectorFst> aiter(*fstp, src_state_id) ;
				!aiter.Done() ;
				aiter.Next()) {
			
			const StdArc &arc = aiter.Value() ;

			ilabel = (UChar32) arc.ilabel ;
			olabel = (UChar32) arc.olabel ;

			// block for making a network case-insensitive
			if (add_uc && add_lc) {
				if (input && u_isalpha(ilabel)) {
					if (u_isupper(ilabel)) {
						// find A:whatever, add a:whatever
						newIlabel = u_tolower(ilabel) ;
						sigmaAddedSet.insert((int) newIlabel) ;
						arcsToAdd.push_back(new ArcInfo(
									src_state_id,
									(Label) newIlabel,
									(Label) olabel,
									arc.weight,
									arc.nextstate)) ;
					} else if (u_islower(ilabel)) {
						// find a:whatever, add A:whatever
						newIlabel = u_toupper(ilabel) ;
						sigmaAddedSet.insert((int) newIlabel) ;
						arcsToAdd.push_back(new ArcInfo(
									src_state_id,
									(Label) newIlabel,
									(Label) olabel,
									arc.weight,
									arc.nextstate)) ;
					}
				}
				if (output && u_isalpha(olabel)) {
					if (u_isupper(olabel)) {
						// find whatever:A, add whatever:a
						newOlabel = u_tolower(olabel) ;
						sigmaAddedSet.insert((int) newOlabel) ;
						arcsToAdd.push_back(new ArcInfo(
									src_state_id,
									(Label) ilabel,
									(Label) newOlabel,
									arc.weight,
									arc.nextstate)) ;
					} else if (u_islower(olabel)) {
						// find whatever:a, add whatever:A
						newOlabel = u_toupper(olabel) ;
						sigmaAddedSet.insert((int) newOlabel) ;
						arcsToAdd.push_back(new ArcInfo(
									src_state_id,
									(Label) ilabel,
									(Label) newOlabel,
									arc.weight,
									arc.nextstate)) ;
					}
				}

				// when doing both sides
				// handle a:a arcs and mixed-case a:A and A:a arcs
				// to get A:A arcs and mixed-case A:a and a:A arcs
				if (input && output) {
					if (u_isupper(ilabel)) {
						if (u_isupper(olabel)) {
							// find A:A, add a:a
							newIlabel = u_tolower(ilabel) ;
							newOlabel = u_tolower(olabel) ;
							sigmaAddedSet.insert((int) newIlabel) ;
							sigmaAddedSet.insert((int) newOlabel) ;
							arcsToAdd.push_back(new ArcInfo(
									src_state_id,
									(Label) newIlabel,
									(Label) newOlabel,
									arc.weight,
									arc.nextstate)) ;
						} else if (u_islower(olabel)) {
							// find A:a, add a:A
							newIlabel = u_tolower(ilabel) ;
							newOlabel = u_toupper(olabel) ;
							sigmaAddedSet.insert((int) newIlabel) ;
							sigmaAddedSet.insert((int) newOlabel) ;
							arcsToAdd.push_back(new ArcInfo(
									src_state_id,
									(Label) newIlabel,
									(Label) newOlabel,
									arc.weight,
									arc.nextstate)) ;
						}
					} else if (u_islower(ilabel)) {
						if (u_islower(olabel)) {
							// find a:a, add A:A
							newIlabel = u_toupper(ilabel) ;
							newOlabel = u_toupper(olabel) ;
							sigmaAddedSet.insert((int) newIlabel) ;
							sigmaAddedSet.insert((int) newOlabel) ;
							arcsToAdd.push_back(new ArcInfo(
									src_state_id,
									(Label) newIlabel,
									(Label) newOlabel,
									arc.weight,
									arc.nextstate)) ;
						} else if (u_isupper(olabel)) {
							// find a:A, add A:a
							newIlabel = u_toupper(ilabel) ;
							newOlabel = u_tolower(olabel) ;
							sigmaAddedSet.insert((int) newIlabel) ;
							sigmaAddedSet.insert((int) newOlabel) ;
							arcsToAdd.push_back(new ArcInfo(
									src_state_id,
									(Label) newIlabel,
									(Label) newOlabel,
									arc.weight,
									arc.nextstate)) ;
						}
					}
				}
			// just add uc
			} else if (add_uc) {
				// add arcs with uc labels where needed
				if (input && output) {
					if (u_islower(ilabel) || u_islower(olabel)) {
						// one side is lc, but the other could be a MCS
						// avoid trying to make MCS uc or lc
						// or adding it to sigmaAddedSet
						if (u_islower(ilabel)) {
							newIlabel = u_toupper(ilabel) ;
							sigmaAddedSet.insert((int) newIlabel) ;
						} else {
							newIlabel = ilabel ;
						}
						if (u_islower(olabel)) {
							newOlabel = u_toupper(olabel) ;
							sigmaAddedSet.insert((int) newOlabel) ;
						} else {
							newOlabel = olabel ;
						}
						arcsToAdd.push_back(new ArcInfo(
									src_state_id,
									(Label) newIlabel,
									(Label) newOlabel,
									arc.weight,
									arc.nextstate)) ;
					}
				} else if (input) {
					if (u_islower(ilabel)) {
						newIlabel = u_toupper(ilabel) ;
						sigmaAddedSet.insert((int) newIlabel) ;
						arcsToAdd.push_back(new ArcInfo(
									src_state_id,
									(Label) newIlabel,
									(Label) olabel,
									arc.weight,
									arc.nextstate)) ;
					}
				} else if (output) {
					if (u_islower(olabel)) {
						newOlabel = u_toupper(olabel) ;
						sigmaAddedSet.insert((int) newOlabel) ;
						arcsToAdd.push_back(new ArcInfo(
									src_state_id,
									(Label) ilabel,
									(Label) newOlabel,
									arc.weight,
									arc.nextstate)) ;
					}
				}
			} else if (add_lc) {
				// add arcs with lc labels where needed
				if (input && output) {
					if (u_isupper(ilabel) || u_isupper(olabel)) {
						// at least one side is uc
						// other side could be MCS
						// avoid trying to make a MCS lower case
						// or adding it to sigmaAddedSet
						if (u_isupper(ilabel)) {
							newIlabel = u_tolower(ilabel) ;
							sigmaAddedSet.insert((int) newIlabel) ;
						} else {
							newIlabel = ilabel ;
						}
						if (u_isupper(olabel)) {
							newOlabel = u_tolower(olabel) ;
							sigmaAddedSet.insert((int) newOlabel) ;
						} else {
							newOlabel = olabel ;
						}
						arcsToAdd.push_back(new ArcInfo(
									src_state_id,
									(Label) newIlabel,
									(Label) newOlabel,
									arc.weight,
									arc.nextstate)) ;
					}
				} else if (input) {
					if (u_isupper(ilabel)) {
						newIlabel = u_tolower(ilabel) ;
						sigmaAddedSet.insert((int) newIlabel) ;
						arcsToAdd.push_back(new ArcInfo(
									src_state_id,
									(Label) newIlabel,
									(Label) olabel,
									arc.weight,
									arc.nextstate)) ;
					}
				} else if (output) {
					if (u_isupper(olabel)) {
						newOlabel = u_tolower(olabel) ;
						sigmaAddedSet.insert((int) newOlabel) ;
						arcsToAdd.push_back(new ArcInfo(
									src_state_id,
									(Label) ilabel,
									(Label) newOlabel,
									arc.weight,
									arc.nextstate)) ;
					}
				}
			}
		} // end of loop through the arcs of a state
	} // end of loop through the states

	// add the arcsToAdd here
	for (iter = arcsToAdd.begin(); iter != arcsToAdd.end(); iter++) {
		ArcInfo * aip = *iter ;
		fstp->AddArc(aip->src_state_id, 
			StdArc(aip->ilabel, aip->olabel, aip->weight, aip->dest_state_id)) ;
	}

	// the sigmaAddedSet will be of some specific size
	int size = sigmaAddedSet.size() ;

	// dynamically allocate an int[] array of that size
	int* nativeIntArray = new int[size] ;  // need to delete below

	int n = 0 ;
	// copy the set into the int[] array
	for (set<int>::const_iterator iter = sigmaAddedSet.begin() ;
		iter != sigmaAddedSet.end() ;
		iter++) {
		nativeIntArray[n++] = *iter ;
	}

	jintArray retArr = env->NewIntArray(size) ;
	// creates a jint array in the Java space
	// set the whole Java array directly from the local C++ array
	env->SetIntArrayRegion(retArr, 0, size, (jint *)nativeIntArray) ;

	delete[] nativeIntArray ;  // to avoid a memory leak

	return retArr ;
}

JNIEXPORT jintArray JNICALL
Java_OpenFstLibraryWrapper_convertCaseNative
  (JNIEnv *env, jclass cls,
   jlong fstPtr,
   jboolean all,	// true for All, false for init only
   jboolean to_uc,	// convert lc to uc
   jboolean to_lc,	// convert uc to lc
   jboolean input,	// on the input side
   jboolean output)	// on the output side
{
	StdVectorFst * fstp = (StdVectorFst *)(uintptr_t) fstPtr ;

	set<int> sigmaAddedSet ;  // use a set to filter out duplicates

	bool do_all = true ;	// reset to false for initial only

	StateId src_state_id ;

	UChar32 ilabel, olabel, new_ilabel, new_olabel ;

	for (StateIterator<StdVectorFst> siter(*fstp) ;
			do_all && !siter.Done() ;
			siter.Next()) {
		// src_state_id is the int identifier of a state

		if (!all) {
			do_all = false ;	// will be false for init-only
			src_state_id = fstp->Start() ;
			// just do the arcs leaving the start state
		} else {
			src_state_id = siter.Value() ;
		}

		// Now loop through the exit arcs of state s.
		// Need to use a MUTABLEArcIterator to be able to _change_
		// the values on the orig. arcs (the constructor needs a ptr
		// to an Fst rather than the Fst itself); the final stmt
		// in the loop needs to be aiter.SetValue(arc) to set the
		// new values in the Fst

		for (MutableArcIterator<StdVectorFst> aiter(fstp, src_state_id) ;
				!aiter.Done() ;
				aiter.Next()) {
			// arc is (at first) a copy of the original arc
			StdArc arc = aiter.Value() ;

			ilabel = (UChar32) arc.ilabel ;
			olabel = (UChar32) arc.olabel ;

			new_ilabel = ilabel ;
			new_olabel = olabel ;

			if (to_uc) {
				// modify lc to uc labels where needed
				if (input) {
					if (u_islower(ilabel)) {
						new_ilabel = u_toupper(ilabel) ;
						sigmaAddedSet.insert((int) new_ilabel) ;
					}
				}
				if (output) {
					if (u_islower(olabel)) { 
						new_olabel = u_toupper(olabel) ;
						sigmaAddedSet.insert((int) new_olabel) ;
					}
				}
			}

			if (to_lc) {
				// add uc to lc labels where needed
				if (input) {
					if (u_isupper(ilabel)) {
						new_ilabel = u_tolower(ilabel) ;
						sigmaAddedSet.insert((int) new_ilabel) ;
					}
				}
				if (output) {
					if (u_isupper(olabel)) { 
						new_olabel = u_tolower(olabel) ;
						sigmaAddedSet.insert((int) new_olabel) ;
					}
				}
			}

			arc.ilabel = (Label) new_ilabel ;
			arc.olabel = (Label) new_olabel ;

			// and here, the final step in the loop through the arcs,
			// the modified arc is set in the network being processed
			aiter.SetValue(arc) ;
		} // end of loop through the arcs of a state
	} // end of loop through the states

	// the sigmaSet will be of some arbitrary size
	int size = sigmaAddedSet.size() ;

	// dynamically allocate an int[] array of that size,
	// and be sure to delete it below
	int* nativeIntArray = new int[size] ;  // delete[] below

	int n = 0 ;
	// copy the set into the local int[] array
	for (set<int>::const_iterator iter = sigmaAddedSet.begin() ;
		iter != sigmaAddedSet.end() ;
		iter++) {
		nativeIntArray[n++] = *iter ;
	}

	jintArray retArr = env->NewIntArray(size) ;  
	// creates a jint array in the Java space
	// set the whole Java array directly from the local C++ array
	env->SetIntArrayRegion(retArr, 0, size, (jint *)nativeIntArray) ;

	delete[] nativeIntArray ;

	return retArr ;
}
