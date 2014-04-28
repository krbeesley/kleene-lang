
//	OpenFstLibraryChecker.java
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

// Class with methods to do semantic checking, so see if an
// operation is legal for the operand(s).  Called from
// OpenFstLibraryWrapper.java

public class OpenFstLibraryChecker {

	OpenFstLibraryWrapper lib ;

	// Constructor constructor
	public OpenFstLibraryChecker(OpenFstLibraryWrapper lw) {
		lib = lw ;
	}

	public void ApplyToOneString(Fst testFst) {
		if (lib.isSapRtnConventions() && testFst.getIsRtn()) {
			throw new FstPropertyException("SAP RTNs cannot be composed or tested in the GUI.") ;
		}
	}

	public void Compose(Fst a, Fst b) {
		// check for same semiring

		// check for RTN complications
		if (lib.isSapRtnConventions() && (a.getIsRtn() || b.getIsRtn())) {
			throw new FstPropertyException("SAP RTNs cannot be composed.") ;
		}
	}

	public void Concat(Fst a, Fst b) {
		// check for same semiring
	}

	public void Crossproduct(Fst a, Fst b) {
		// check for same semiring

		// the two args must be acceptors
		if (!lib.IsAcceptor(a)) {
			throw new FstPropertyException("The first argument (and second argument) to an fst crossproduct must be an Acceptor.") ;
		}

		if (!lib.IsAcceptor(b)) {
			throw new FstPropertyException("The second argument (and first argument) to an fst crossproduct must be an Acceptor.") ;
		}
	}

	public void Difference(Fst a, Fst b) {
		// check for same semiring

		// check for RTN complications
		if (lib.isSapRtnConventions() && (a.getIsRtn() || b.getIsRtn())) {
			throw new FstPropertyException("SAP RTNs cannot be subtracted or complemented.") ;
		}

		// the first arg must be an acceptor
		if (!lib.IsAcceptor(a)) {
			throw new FstPropertyException("The first argument to an fst difference must be an Acceptor.") ;
		}

		// the second arg must be 1) acceptor, 2) unweighted, 
		// 3) epsilonFree, and 4) deterministic
		if (!lib.IsAcceptor(b)) {
			throw new FstPropertyException("The second argument to an fst difference must be an Acceptor.") ;
		}
		if (!lib.IsUnweighted(b)) {
			throw new FstPropertyException("The second argument to an fst difference must be unweighted.") ;
		}
		if (!lib.IsEpsilonFree(b)) {
			// epsilon removal done in place on the network
			// (work on a copy?)
			lib.RmEpsilonInPlace(b) ;
		}
		if (!lib.IsIDeterministic(b)) {
			// IDeterministic is for "input" deterministic,
			// but this is an acceptor, so IDeterministic
			// and ODeterministic are the same here.

			// It has already been determined that this FST is 
			// (Acceptor and !Weighted)
			// so OpenFst Determinize() (i.e. sequentialize) can be called

			lib.DeterminizeInPlace(b) ;
			// KRB:  possibly call optimizeInPlace() here?
		} 
	}


	public void Equivalent(Fst a, Fst b) {
		// the two arguments must be acceptors, epsilon-free
		// they can be weighted
		if (!lib.IsAcceptor(a)) {
			throw new KleeneArgException("The first argument (and the second argument) to #^equivalent() must denote a language, not a relation.") ;
		}
		if (!lib.IsAcceptor(b)) {
			throw new KleeneArgException("The second argument (and the first argument) to #^equivalent() must denote a language, not a relation.") ;
		}
		// They are acceptors
		// Now make sure that they are epsilon-free
		if (!lib.IsEpsilonFree(a)) {
			throw new KleeneArgException("The first argument (and the second argument) to #^equivalent() must be epsilon-free.") ;		}
		if (!lib.IsEpsilonFree(b)) {
			throw new KleeneArgException("The second argument (and the first argument) to #^equivalent() must be epsilon-free.") ;	
		}
	}

	public void RandEquivalent(Fst a, Fst b) {
		// no obvious semantic restrictions
	}

	public void GetSingleArcLabel(Fst a) {
		// the argument must denote a one-string language (non-empty),
		// two states, one arc
		if (!lib.IsAcceptor(a)) {
			throw new KleeneArgException("Argument to explode must denote a language, not a relation.") ;
		}
		if (!lib.IsString(a)) {
			throw new KleeneArgException("Argument to explode must denote a language of one string") ;
		}
		int stateCount = lib.NumStates(a) ;

		int arcCount = lib.NumArcs(a) ;
		if (stateCount != 2 || arcCount != 1) {
			throw new KleeneArgException("Argument to explode must denote a two-state, one-arc network") ;
		}
	}

	public void GetSingleString(Fst a, String excMsg) {
		if (	!lib.IsAcceptor(a) 
			|| 	!lib.IsString(a)) {
				throw new KleeneArgException(excMsg) ;
		}
	}

	public void Intersect(Fst a, Fst b) {
		// check for same semiring

		// check RTN complications
		if (lib.isSapRtnConventions() && (a.getIsRtn() || b.getIsRtn())) {
			throw new FstPropertyException("SAP RTNs cannot be intersected.") ;
		}

		if (!lib.IsAcceptor(a) ||
			!lib.IsAcceptor(b)) {
			throw new FstPropertyException("The arguments to intersection must be acceptors.") ;
		}
	}
		
	public void Iterate(Fst a, long low, long high) {
		// a high of -1 means unlimited, from syntax x{2,}
		//
		// check semiring
		// check RTN complications
		if (low < 0L) {
			throw new IterationException("The iteration low value must be greater than or equal to 0") ;
		}
		if (high < 0L) {
			throw new IterationException("The iteration high value must be greater than or equal to 0") ;
		}
		if (high != -1L && low > high) {
			throw new IterationException("The iteration low value must be less than or equal to the high value.") ;
		}

	}

	public void MinimizeInPlace(Fst a) {
		if (!lib.IsIDeterministic(a)) {
			throw new FstPropertyException("The network argument to $^minimize() must be deterministic.") ;
		}
	}

	public void ShortestPath(Fst a) {

		System.out.println("Call to checker's ShortestPath") ;

		System.out.println("isSapRtnConventions: " +
								lib.isSapRtnConventions()) ;

		System.out.println("getIsRtn(): " + a.getIsRtn()) ;

		// check for RTN complications
		if (lib.isSapRtnConventions() && a.getIsRtn()) {
			throw new FstPropertyException("SAP RTNs cannot be used with shortestPath.") ;
		}
	}

	public void SynchronizeInPlace(Fst a) {
		// KRB:  you can synchronize any acyclic network (an acyclic
		// network always has "bounded delay", the delay being at most the
		// number of states)
		// if there are cycles (loops), then the cycles must have delay=0,
		// i.e. for every cycle c, the length of the input of c must be
		// equal to the length of the output of c.
		// I'm not quite sure how to test for this right now.
		if (lib.IsCyclic(a)) {
			throw new FstPropertyException("The network argument to $^synchronize() must be acyclic.") ;
		}
	}

	public void Union(Fst a, Fst b) {
		// check for same semiring
	}
}
