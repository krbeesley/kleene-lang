
//walk through an acceptor and collect the Labels (integers)
//(the ilabel and olabel are the same in an acceptor)

// various other includes
#include <set>

using namespace std ;

int main() {

	set<int> ilabelSet ;
	ilabelSet.insert(10) ;
	ilabelSet.insert(5) ;
	ilabelSet.insert(2) ;
	ilabelSet.insert(1) ;


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


