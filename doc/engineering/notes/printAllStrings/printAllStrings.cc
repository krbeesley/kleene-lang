// printAllStrings code originally from Roger Levy (posted on the OpenFst forum,
// followed by suggestions by Cyril Allauzen)
// Much modified.  Kenneth R. Beesley, starting 11 June 2008.

// StdFst       is a pre-defined typedef for Fst<StdArc>
// StdVectorFst is a pre-defined typedef for VectorFst<StdArc>
// StdArc implements the Tropical Semiring, wherein the weights are -log(probability), 
//        known as "costs"
// 
//      Mindtuning about Probabilities vs. Costs
//
//        The probability p ranges 0.0 <= p <= 1.0
//            where 1.0 is Certain, 0.0 would be Impossible
//
//            (E.g. If you have a bag of marbles, all black, and pick out one at random, the
//            probability that it is black is 1.0, i.e. Certain; the probability that it is 
//            white is 0.0, i.e. Impossible.
//            If you have a bag of marbles, half black and half white, and you pick out a
//            marble at random, the probability that it is black is .5.)
//
//            If you have a path in a weighted automaton, and each arc in the path has a
//            probability weight, these weights are combined along the path by multiplying
//            them (this "extend" or "extension" operation is generically known in OpenFst
//            as Times()).  Formally, this scheme, wherein the weights are probabilities,
//            and the extend operation is multiplication, is known as the "Real Semiring".
//
//        In practical NLP applications, it is often more convenient (and more efficient at
//        runtime) to recast probabilities as "costs", where a cost is -log(probability).  
//        For those, like myself, who haven't grappled with logarithms since high school, 
//        here's a summary of how logarithms work, and why translating probabilities into 
//        costs is attractive.
//
//        Some Fundamentals:
//
//		  log(1), for any logarithm base, is 0.0
//		  log(v), where 0.0 < v < 1.0, gets increasingly negative as v approaches 0.0
//		  log(0), for any base, is negative infinity
//
//        For example (computed in Python, using math.log(x), which is the "natural" log)
//
//		log(1)  =            0.0
//		log(.9) =           -0.10536051565782628
//		log(.8) =           -0.22314355131420971
//		log(.7) =           -0.35667494393873245
//		log(.6) =           -0.51082562376599072
//		log(.5) =           -0.69314718055994529
//		log(.4) =           -0.916290731874155
//		log(.3) =           -1.2039728043259361
//		log(.2) =           -1.6094379124341003
//		log(.1) =           -2.3025850929940455
//		log(.01) =          -4.6051701859880909
//		log(.001) =         -6.9077552789821368
//		log(.0001) =        -9.2103403719761818
//		log(.00001) =      -11.512925464970229
//		log(.0000000001) = -23.025850929940457
//
//         	and so on, with log(p) approaching negative infinity as p approaches 0.0.
//
//      	If we multiply each log() value by negative one, we get a positive "cost", 
//      	such that as the probability gets lower and lower (approaching zero), the 
//      	cost gets higher and higher (approaching positive infinity).
//
//      	Why go to the trouble?  Logarithms have an interesting feature: to multiply
//      	numbers, you can _add_ their logarithms and then take the antilog of the
//      	sum to get the result.  If weights in an automaton are expressed as 
//      	costs (as in the Tropical Semiring) then the extension operation (the 
//      	"Times()" operation) for combining weights along a path is (efficient) addition.  
//      	Intuitively, as you add cost weights along a path, the total cost increases.  
//      	Individual arc weights (costs) can be computed once at compile time, and at 
//      	runtime, weight extension via addition of costs is inherently more efficient 
//      	than multiplication of the corresponding probabilities.  This scheme, wherein
//      	weights are costs, and the extend operation is addition, is known as the Tropical
//      	Semiring.  In this semiring, the collection (or Plus()) operation is min(),
//      	which takes multiple solutions and returns just the one with the lowest cost
//      	(comparable to the solution with the highest probability, in the Real
//      	Semiring).
//
//
//      	// The following code assumes the Tropical Semiring, implemented in StdArc

// a few useful typedefs
typedef StdArc::Label   Label ;
typedef StdArc::Weight  Weight ;
typedef StdArc::StateId StateId ;
// need typedef for Times()?

// If you have an fst, you can pass it to printAllString() to cause the language of the fst
// to be printed/enumerated.  When dealing with a transducer, rather than an acceptor, you
// need to specify the input or the output language, or perhaps both.


enum Projection { INPUT_PROJECT, OUTPUT_PROJECT } ;
enum EpsTreatment { IGNORE_EPSILON, DISPLAY_EPSILON } ;

// here accept StdVectorFst (a typedef for VectorFst<StdArc>) 
// or more generally StdFst (a typedef for Fst<StdArc>) ?
void printAllStrings(const StdVectorFst &fst, Projection proj, EpsTreatment eps) {
	vector<int> labelseq(0) ;  // used to accumulate a sequence of labels (integers) along a path

	// pass the fst
	//          fst.Start()  (the start state is not necessarily state 0)
	//          Weight::One()  (the identity value for expansion (Times()), the operation
	//                that combines the weights (here TropicalWeight costs) along a path)
	//                In the Tropical Semiring, Weight::One() is 0.0
	//
	// Check the size of the language? Abort if too big, or print a finite random set?
	printAllStringsHelper(fst, fst.Start(), labelseq, Weight::One(), proj, eps) ;
}

void printAllStringsHelper(const StdVectorFst &fst, 
							StateId state, 
							vector<int> labelseq, 
							Weight cost,
							Projection proj,
							EpsTreatment eps) {
	if (fst.Final(state) != Weight::Zero())  // if the state is final, then
		// print out the current string.
		// In the Tropical Semiring, Weight::Zero() is Infinity.
		// The final weight is the cost (so far), combined with the final-state's exit
		// weight using the extend (Times()) operation.  vectorToString() is defined
		// below.
		cout << vectorToString(labelseq) << " with cost " << Times(cost, fst.Final(state)) << endl ;
	for (ArcIterator<StdVectorFst> aiter(fst, state); !aiter.Done(); aiter.Next()) {
		const StdArc &arc = aiter.Value() ;
		if (proj == input) {
			labelseq.push_back(arc.ilabel) ;
		} else {
			labelseq.push_back(arc.olabel) ;
		}
		printAllStringsHelper(fst, arc.nextstate, labelseq, Times(cost, arc.weight), proj, eps) ;
		labelseq.pop_back() ;
	}
}

// itos() called by vectorToString() below
string itos(int i) {   // convert int label to symbol name
	stringstream s ;
	s << i ;
	return s.str() ;
}  // in Kleene, need to look up each int in the symbol table

// think about options for display; here "dog" is displayed as <d,o,g>, which
// is especially valuable when you have symbols with multichar print names
string vectorToString(vector<int> v) {
	if (v.size() == 0)
		return "<>" ;
	string result = "<" + itos(v[0]) ;
	for (int i = 1; i < v.size(); i++) {
		result += "," + itos(v[i]) ;
	}
	return result + ">" ;
}

