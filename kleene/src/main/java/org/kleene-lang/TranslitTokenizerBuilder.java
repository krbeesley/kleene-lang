
//	TranslitTokenizerBuilder.java
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

import java.util.HashSet ;
import java.util.Iterator ;
import java.util.TreeMap ;
import java.util.Comparator ; 
import java.util.Set ;
import java.util.Map ;

import com.ibm.icu.text.Transliterator ;      // from ICU4J

public class TranslitTokenizerBuilder {

	HashSet<Integer> multicharCpvSigma ;

	HashSet<Integer> multicharCpvInput ;
	HashSet<Integer> multicharCpvOutput ;

	//TreeMap <String, Integer> string2cpvInput ;
	//TreeMap <String, Integer> string2cpvOutput ;

	Comparator<String> sortLongerToShorter ;

  	// Transliterators are used to "tokenize" input Strings, finding and reducing
	// any multichar symbols into a single code point value from the PUA.
  	//
  	// Convert any String into a seq. of backslash-uHHHH Java escape sequences
  	// (will be an instance of a standard Any-Hex Transliterator supplied by ICU4J)
  	Transliterator tr2JavaEsc ;

	SymMap symmap ;   // the current SymMap, passed in (symbol to integer mapping)

  	// Constructor
 	public TranslitTokenizerBuilder(SymMap sm, 
									HashSet<Integer> sigma,
									OpenFstLibraryWrapper lib) {
		symmap = sm ;

		multicharCpvSigma = new HashSet<Integer>() ;

		multicharCpvInput = new HashSet<Integer>() ;
		multicharCpvOutput = new HashSet<Integer>() ;

		int lowestMcsCpv = sm.getStartPuaCpv() ;
		// lowest multichar symbol code point value
		//
		// Collect the multichar symbols in the sigma (which may not
		// appear on labels, if the network contains OTHER).
		int i ;
		for (Iterator<Integer> iter = sigma.iterator(); iter.hasNext(); ) {
			i = iter.next().intValue() ;
			if (i >= lowestMcsCpv) {
				multicharCpvSigma.add(i) ;
			}
		}

		lib.stripSpecialCharsOther(multicharCpvSigma) ;

		sortLongerToShorter = new Comparator<String>() {
			// the compare() method compares two objects and returns
			// neg   for a < b (a should be sorted before b)
			// 0     for a = b
			// pos   for a > b (a should be sorted after b)
			public int compare(String a, String b) {
				if (b.length() != a.length()) {
					return b.length() - a.length() ;
					// if String b is longer than String a,
					// then b is "less than" a, and should be
					// ordered before a.
				} else {
					return b.compareTo(a) ;
				}
			}
		} ;
    	// Use TreeMap rather than HashMap to maintain keys in order (in this case,
    	// longest to shortest); the order is needed to give longer matches priority
    	// over shorter matches when "tokenizing" multichar symbols in input strings
    	//string2cpvInput = new TreeMap <String, Integer> (sortLongerToShorter) ;

		//string2cpvOutput = new TreeMap <String, Integer> (sortLongerToShorter) ;


    	// Any-Hex/Java (equiv. to just Any-Hex) is supplied by ICU4J
    	// e.g. maps "ab" to "\u0061\u0062", etc.
    	tr2JavaEsc = Transliterator.getInstance("Any-Hex/Java") ;
	}  

	// Method called from C++  native method iterate4mcs, which iterates through the
	// whole Fst and calls back here for each (new) multichar symbol that it
	// finds on the upper/input side
	public void registerMcsInput(int cpv) {
		//String name = symmap.getsym(cpv) ;
		//string2cpvInput.put(name, cpv) ;
		//
		multicharCpvInput.add(cpv) ;
	}

	// same, but for the lower/output side
	public void registerMcsOutput(int cpv) {
		//String name = symmap.getsym(cpv) ;
		//string2cpvOutput.put(name, cpv) ;
		//
		multicharCpvOutput.add(cpv) ;
	}

	public Transliterator getTranslitTokenizer(boolean for_input_side) {

    	//  Now create a custom Transliterator for input strings, which
    	//  reduces multichar-symbols to single code points, moving
    	//  left-to-right, preferring the longest match at each position
		//
		HashSet<Integer> multicharCpvForRules ;
		HashSet<Integer> multicharCpvDiff ;

		if (for_input_side) {
			// Need multichar symbols:  Sigma - (Output - Input)
			// i.e. all the multichar symbols in the sigma, minus
			// those that appear exclusively on the output side.
			// Start with a shallow copy of the multichar code point values 
			// on the output labels
			multicharCpvDiff = (HashSet<Integer>) multicharCpvOutput.clone() ;
			// remove the multichar code point values on the input labels,
			// leaving the code point values that appear only on output labels
			multicharCpvDiff.removeAll(multicharCpvInput) ;

		} else {
			// need multichar symbols: Sigma - (Input - Output)
			// i.e. all the multichar symbols in the sigma, minus
			// those that appear exclusively on the input side
			// start with a shallow copy of the multichar code point values 
			// on the input labels
			multicharCpvDiff = (HashSet<Integer>) multicharCpvInput.clone() ;
			// remove the multichar code point values on the output labels,
			// leaving the code point values that appear only on input labels
			multicharCpvDiff.removeAll(multicharCpvOutput) ;
		}
		// now get a shallow copy of the multichar code point values in the sigma
		multicharCpvForRules = (HashSet<Integer>) multicharCpvSigma.clone() ;
		// and remove the code point values that appear only on the other side
		multicharCpvForRules.removeAll(multicharCpvDiff) ;

		// now need to get the strings for these code point values and sort
		// them longest-to-shortest in a TreeMap (that uses a suitable
		// Comparator)

		TreeMap<String, Integer> ruleMap = new TreeMap<String, Integer> (sortLongerToShorter) ;

		int cpv ;
		for (Iterator iter = multicharCpvForRules.iterator(); iter.hasNext(); ) {
			cpv = ((Integer)iter.next()).intValue() ;
			ruleMap.put(symmap.getsym(cpv), cpv) ;
		}

		// convert back to a set view
		Set <Map.Entry<String, Integer>> set = ruleMap.entrySet() ;

		// Construct a StringBuilder containing rules; used to build the
		// final Transliterator.
		StringBuilder rulebuf = new StringBuilder() ;
    	rulebuf.append("use variable range 0xF000 0xF4FF; ") ;
    	// Undocumented feature of Transliterator:  it uses some of the PUA,
    	// by default _ALL_ of the PUA (0xF000 through 0xF8FF), for its own
    	// internal variables, preventing my use of that area; but you can 
    	// tell Transliterator to limit itself to a subportion, e.g. 0xF000
    	// to 0xF4FF, as shown above, leaving me free to use the rest (Kleene
		// limits itself to Plane 15 and up

    	// Now loop through the entries; need to create a String of "transliteration"
    	// rules of the form
    	// input > output
    	// for all multichar symbols.
    	// To avoid problems with special characters, both input and output will
    	// be expressed in the rule-string as sequences of backslash-uHHHH sequences.
		// The mapping to backslash-uHHHH sequences is done by tr2JavaEsc, which
		// is a Transliterator of type Any-Hex/Java, which is supplied by ICU.  It
		// maps any Java String to a sequence of backslash-uHHHH sequences.
	
    	String key ;
    	int val ;
    	// loop through the entries in the (sorted) set 
    	for (Map.Entry<String, Integer> me: set) {
        	key = me.getKey() ;
			val = me.getValue().intValue() ;  // a value in the surrogate range
			// not representable in one 16-bit code unit
			if (key.length() > 1) {
	    		rulebuf.append(tr2JavaEsc.transliterate(key) + " > " 
                         + tr2JavaEsc.transliterate(stringFromCpv(val)) + " ; ") ;
			} else {
				// shouldn't they all be multichar symbols, screened already?
				System.out.println("See TranslitTokenizerBuilder, problem XXX") ;
			}
    	}

		// Creates a rulebuf that looks like this (for [Noun], [Sg],
		// [Pl]) with linebreaks inserted for readability
		// use variable range 0xF000 0xF4FF; 
		//    \u005B\u004E\u006F\u0075\u006E\u005D > \uDB80\uDC03 ; 
		//    \u005B\u0053\u0067\u005D > \uDB80\uDC04 ; 
		//    \u005B\u0050\u006C\u005D > \uDB80\uDC05 ; 
		//
		// Note that the output surrogate values get represented as the
		// sequence of a high and a low surrogate, e.g.
		// \uDB80\uDC03 represents 0xF0003,
		// because the result is still a Java String, which means UTF-16.


		String trID ;
		if (for_input_side) {
			trID = "TokMCSinput" ;
		} else {
			trID = "TokMCSoutput" ;
		}

		// debug
		//System.out.println("rulebuf for " + trID) ;
		//System.out.println(rulebuf.toString()) ;

    	// now create the custom Transliterator from the set of rules
    	Transliterator trTok = Transliterator.createFromRules(trID, 
															rulebuf.toString(), 
															Transliterator.FORWARD) ;
		return trTok ;
	}

  	String stringFromCpv (int cpv) {
    	if (Character.charCount(cpv) == 1) {
      		return String.valueOf((char) cpv) ;
    	} else {
      		// overhead of creating a char[] array and discarding it
      		return new String(Character.toChars(cpv)) ;
    	}
  	}
}
