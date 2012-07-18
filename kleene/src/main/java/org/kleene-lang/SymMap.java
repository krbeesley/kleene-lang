
//	SymMap.java
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

// relates symbol names to the integer code-point value used to store
// the symbol on arcs; and vice-versa.  There is only one instance of
// the SymMap class, inside the one single Environment instance
 
import com.ibm.icu.text.Normalizer ;
import com.ibm.icu.text.Normalizer.Mode ;
import com.ibm.icu.text.UTF16 ;
import com.ibm.icu.text.UCharacterIterator ;
import java.util.HashMap ;
import java.util.TreeMap ;
import java.util.Comparator ;

public class SymMap {

	private int startPuaCpv ; // don't change

	private int nextPuaCpv ;  // used for multichar symbols
	private int nextNegCpv ;  // temp; used for $>foo rrprod_ids
	private int limitPua ;

	// sym2int for symbolString-to-int mapping; use a TreeMap here
	// rather than a HashMap so that the String keys can be
	// kept ordered (longer ones first) as they are added.
	// The Map Entries are later retrieved (in order) from the
	// TreeMap to generate rules to make a Transliterator that
	// performs the input-string "tokenization" that finds multi-
	// character symbols.
	private TreeMap<String, Integer> sym2int ;

	// int2sym is for int-to-symbolString mapping
	private HashMap<Integer, String> int2sym ;

	private Normalizer.Mode nmode ;

	// Constructors
	public SymMap (Normalizer.Mode m, int startPuaP, int limitPuaP) {
		nmode = m ;  // e.g. Normalizer.NFC or Normalizer.NFD
		nextNegCpv = -1 ;
		startPuaCpv = startPuaP ;
		nextPuaCpv = startPuaP ;
		limitPua = limitPuaP ;

		// in the TreeMap, keep the entries ordered "Longest First";
		// this facilitates generating rules for an ICU Transliterator later.
		// The ordering is done by a Comparator.  (The transliterator performs
		// the "tokenization" of the input string, and in this tokenization,
		// longer matches have precedence over shorter ones.)
		sym2int = new TreeMap<String, Integer>(new Comparator<String>() {
			public int compare(String a, String b) {
				if (b.length() != a.length()) {
					// if b is longer than a, then the positive return means
					// "switch the order"
					return b.length() - a.length() ;
				} else {
					return b.compareTo(a) ;  // Std String lexicographic comparison
											// this order is not significant here
				}
			}
		}) ;
		int2sym = new HashMap<Integer, String>() ;

		// Special Case:  OpenFst reserves integer 0 for the epsilon

		sym2int.put("<eps>", new Integer(0)) ;  // only case in Kleene where two String labels
		sym2int.put("[eps]", new Integer(0)) ;  //   map to the same integer
	    int2sym.put(new Integer(0), "[eps]") ;  // for output, show epsilon as [eps]
	}

	public SymMap () {
		// default NFC, start with Plane 15 PUA (unlimited)
		this(Normalizer.NFC, 0xF0000, Integer.MAX_VALUE) ;
	}

	public SymMap (Normalizer.Mode nmode) {
		this(nmode, 0xF0000, Integer.MAX_VALUE) ;
	}
	// end Constructors

	// return the next available PUA code point in the designated PUA range
	// (not formally restricted to the official PUA ranges--but assume it
	// for now)
	private int getNextPuaCpv() throws SymMapException {
		// skip some reserved values in Plane 15 and Plane 16
		if (nextPuaCpv == 0xFFFFF || nextPuaCpv == 0x10FFFF) {
			nextPuaCpv++ ;
		}
		if (nextPuaCpv > limitPua) {
			throw new SymMapException("SymMap: exhausted designated range of PUA codepoints") ;
		}
		return nextPuaCpv++ ;
	}

	private int getNextNegCpv() throws SymMapException {
		return nextNegCpv-- ;
	}


	// public putsym(key) stores a Unicode-normalized version of the key
	// with its normal Unicode code point value (for a single Unicode
	// character, including supplementary characters), or with a code
	// point value from a Unicode private use area, by default from
	// Plane 15 or 16.
	public int putsym(String key) throws SymMapException {
		int cpv ;

		String normalized = Normalizer.normalize(key, nmode) ;

		Integer intobj = sym2int.get(normalized) ;

		if (intobj == null) {  // if not already stored
			// then need to put it, with a suitable cpv
			if (UTF16.countCodePoint(normalized) == 1) {
				// Use the standard Unicode code point value
				UCharacterIterator iter = UCharacterIterator.getInstance(normalized) ;
				cpv = iter.currentCodePoint() ;
				// this even handles supplementary code point values,
				// but there must be a better way to do this than to
				// create a UCharacterIterator?
			} else {
				// it is a multichar symbol like [Noun] or a rrprod_id like
				// $>foo
				if (normalized.startsWith("$>")) {
					// then it's a rrprod_id (a temporary arc label, later
					// turned into an eps:eps arc leading to the start
					// state of a rr production (see kleeneopenfst.cc,
					// rrGrammarLink()
					cpv = getNextNegCpv() ;
				} else {
					// it's a multichar symbol; just assign a Unicode
					// Private Use Area cpv (or above)
					cpv = getNextPuaCpv() ;
				}
			}

			sym2int.put(normalized, new Integer(cpv)) ;
			int2sym.put(new Integer(cpv), normalized) ;
		} else {
			// it's already in the symbol table, just extract the code point value
			cpv = intobj.intValue() ;
		}

		return cpv ;
	}

	public boolean containsKey(String key) {
		if (sym2int.containsKey(key)) {
			return true ;
		} else {
			return false ;
		}
	}

	public int getint(String key) {
		String normalizedString = Normalizer.normalize(key, nmode) ;
		return sym2int.get(normalizedString).intValue() ;
	}

	public String getsym(int i) {
		if (int2sym.containsKey(i)) {
			return int2sym.get(i) ;
		} else {
			return null ;
		}
	}

	public int getStartPuaCpv() {
		return startPuaCpv ;
	}
}

