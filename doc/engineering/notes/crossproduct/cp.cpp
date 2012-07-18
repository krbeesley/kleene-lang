
walk through an acceptor and collect the Labels (integers)
(the ilabel and olabel are the same in an acceptor)

// various other includes
#include <set>

using namespace fst ;

JNIEXPORT void JNICALL
Java_OpenFstInterpreterKleeneVisitor_collectIlabels
  (JNIEnv *env, jclass cls,
   jlong fstp)
{

    // pass in long fstp from Java
    // cast to ptr to StdVectorFst and dereference
    StdVectorFst fst = *((StdVectorFst *) fstp) ;

	set<Label> ilabelSet ;

	// iterate through the states
    for (StateIterator<StdFst> siter(fst); !siter.Done(); siter.Next()) {
	    StateId state_id = siter.Value() ;  // StateId is a typedef for int

		// and for each state, iterate through its arcs
	    for (ArcIterator<StdFst> aiter(fst, state_id); !aiter.Done(); aiter.Next()) {
		    const StdArc &arc = aiter.Value() ;
		    Label ilabel = arc.ilabel ;
		    // collect these in some kind of C++ set (unique members)
			ilabelSet.insert(ilabel) ;
	    }
    }

	// temporary dump of the set
	// .begin() returns an iterator to the first element of the set
	ostream_iterator<int, char> out(cout, " ") ;
	copy(ilabelSet.begin(), ilabelSet.end(), out) ;
	cout << endl ;

	// maybe this
	//list<int>::iterator theIterator;
   	//for( theIterator = ilabelSet.begin(); theIterator != ilabelSet.end(); theIterator++ ) {
    // 	cout << *theIterator;
	//}
	//cout << endl ;
	
}


