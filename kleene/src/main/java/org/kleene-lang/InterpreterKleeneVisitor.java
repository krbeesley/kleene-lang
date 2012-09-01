
//	InterpreterKleeneVisitor.java
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

// An interpreter for the Kleene language based on a Java
// finite-state library defined in FSLib (the current FSLib is a
// Java wrapper of the OpenFst library).  This class deals with the
// AST trees coming from the Kleene parser and the interpreter
// stack.  It calls methods in the Java wrapping of the
// finite-state library (currently based on OpenFst) .

import java.util.Stack ;
import com.ibm.icu.text.UTF16 ;
import com.ibm.icu.text.UCharacterIterator ;  // see htj UCharacterIterator

import java.io.* ;

import org.dom4j.Document ;
import org.dom4j.DocumentException ;
import org.dom4j.io.SAXReader ;
import org.dom4j.Element ;
import org.dom4j.Attribute ;
import org.dom4j.ElementPath ;
import org.dom4j.ElementHandler ;
import java.util.HashSet ;
import java.util.Set ;
import java.util.List ;
import java.util.ArrayList ; 
import java.util.Iterator ;
import com.sun.syndication.io.XmlReader ;

//import org.apache.commons.lang.StringEscapeUtils ;

import com.ibm.icu.text.Transliterator ;


// local StreamFlusher class

class StreamFlusher extends Thread {
	InputStream is ;
	String type ;

	StreamFlusher(InputStream is, String type) {
		this.is = is ;
		this.type = type ;
	}

	public void run() {
		try {
			InputStreamReader isr = new InputStreamReader(is) ;
			BufferedReader br = new BufferedReader(isr) ;
			String line = null ;
			while ((line = br.readLine()) != null) 
				System.out.println(type + ">" + line) ;
		}
		catch (IOException ioe) {
			ioe.printStackTrace() ;
		}
	}
}

public class InterpreterKleeneVisitor implements KleeneVisitor {
    Environment env ;   // env keeps track of Frames and tables
	// that map variable names to their values
	
	SymMap symmap ;  // symmap provides a two-way mapping between
	// alphabetic and multichar symbols and the integers used to store
	// them on FST arcs;  Provide methods
	// 1.  int symmap.putsym(String)	// adds it only if not already added
	// 2.  int symmap.getint(String)   // returns null if not found
	// 3.  String symmap.getsym(int)

	// KRB: if another library is ever used, make OpenFstLibraryWrapper
	// implement an interface named LibraryWrapper, and then change
	// the following line to 
	// LibraryWrapper lib;
	OpenFstLibraryWrapper lib ;
	Hulden hulden ;

	Frame mainFrame ;  // corresponds to GUI symbol-table window

	// Constructor constructor
	// (called just once--only one interpreter is used in Kleene)
    public InterpreterKleeneVisitor(Environment e) {
		env = e ;

		stack = new Stack<Object>() ;	// only one stack is used
		symmap = new SymMap() ;  // see other Constructor options
								// only one SymMap is used

		// KRB: if another library is ever used, parameterize
		// which one is loaded here
		lib = new OpenFstLibraryWrapper(env, symmap) ;	// only one is used
		hulden = new Hulden(lib, symmap) ;	// only one is used
								// Mans Hulden's algorithms

		// add OTHER_ID and OTHER_NONID this way because the representation
		// of OTHER could change for a new library;  do not refer directly
		// to "OTHER_ID" or "OTHER_NONID" in this file

		lib.AddOtherId(symmap) ;
		lib.AddOtherNonId(symmap) ;
    }

	void outputInterpMessage(String msg, Object data) {
		if (((InterpData) data).getInGUI()) {
			PseudoTerminalInternalFrame terminal = 
					((InterpData)data).getGUI().getTerminal() ;

			terminal.appendToHistory(msg) ;
		} else {
			System.out.println(msg) ;
		}
	}

	// for OTHER display
	private void getSigmaStrings(Fst fst, StringBuilder sbhex, StringBuilder sb) {
		HashSet<Integer> sigma = fst.getSigma() ;

		int cpv ;

		for ( Iterator<Integer> iter = sigma.iterator() ;
			      iter.hasNext() ; ) {
			cpv = iter.next().intValue() ;
				
			sbhex.append(Integer.toString(cpv, 16) + " ") ;
			sb.append(symmap.getsym(cpv)) ;
			if (iter.hasNext()) {
				sb.append(", ") ;
			} else {
				sb.append(" ") ;
			}
		}
	}

	Fst interpRestrictionExp(Fst lhs, Fst rhs, boolean mayHaveWordBoundaryInContexts) {
		// mayHaveWordBoundaryInContexts means that the restriction expression 
		//    is a stand-alone
		//    regular expression.  If False, then this func is being called as
		// 	  part of the algorithm for compiling alternation rules, where
		//    the contexts do not have the word boundary in contexts.  This
		// 	  trick is needed to avoid the problem of word boundaries in
		//	  in restriction expressions vs. word boundaries in alternation rules.

		// to be safe, need to make sure that lhs does not
		// contain the restriction delimiter (if it contains OTHER, then
		// it would normally include the restriction delimiter; so if it
		// contains OTHER, then add restDelimSym to the sigma
		if (lhs.getContainsOther()) {
			if (lhs.getFromSymtab()) {
				// then work on a copy to avoid corrupting an FST in the
				// symbol table
				lhs = lib.CopyFst(lhs) ;
			}
			// then just add the restriction delimiter to the sigma
			lhs.getSigma().add(symmap.putsym(hulden.restDelimSym)) ;
		} // else it has a finite sigma, and is already safe

		// Hulden:  [ \x* x lhs x \x* ]
		Fst oneMinuend = lib.Concat5Fsts(	hulden.NotRestDelimStarFst(),
										lib.OneArcFst(hulden.restDelimSym), 
										lhs,
										lib.OneArcFst(hulden.restDelimSym), 
										hulden.NotRestDelimStarFst()
									);

		// "One" is the language of all strings that have at least one lhs
		// in an illegal context.
		// Hulden:	define One [\x* x lhs x \x*] - [\x* [L x \x* x R] \x*]
		Fst One = lib.Difference(oneMinuend, rhs) ; 

		Fst Two = One ;
		// Hulden:	define Two `[One, x, 0] ;
		hulden.SubstEpsilonInPlace(Two, hulden.restDelimSym) ;

		// why this apparent null operation? here and below?
		// At one time, at least, it appeared that OpenFst was not
		// perceiving correctly that Two contained epsilons after the
		// call to SubstEpsilonInPlace()
		Two = lib.Concat(Two, lib.EmptyStringLanguageFst()) ; 

		lib.OptimizeInPlace(Two) ;

		Fst Three = null ;

		// The following subtraction is needed only if the restriction expression
		// is stand-alone and can have # in the contexts (it does not have #
		// when the restriction is part of the algorithm for compiling alternation
		// rules).
		if (mayHaveWordBoundaryInContexts) {
			// Hulden:   define Three [.#. \.#.* .#.] - Two 
			// take the complement with respect to .#. \.#.* .#.
			// so that any .#. in the restriction contexts is constrained
			// to be on the beginning or end
		 	Three = lib.Difference(lib.Concat3Fsts(
												lib.OneArcFst(hulden.ruleWordBoundarySym),
												newNotWordBoundStarFst(),
												lib.OneArcFst(hulden.ruleWordBoundarySym)
											   ),
									Two) ;
			hulden.SubstEpsilonInPlace(Three, hulden.ruleWordBoundarySym) ;
		} else {
			Three = lib.Complement(Two) ;
		}
		Three = lib.Concat(Three, lib.EmptyStringLanguageFst()) ;
		lib.OptimizeInPlace(Three) ;

		return Three ;
	}

	Fst interpRestrictionContext(Fst leftContext, Fst rightContext) {

		// First make sure that leftContext and rightContext do not
		// contain the restDelimSym (i.e. iff they contain
		// OTHER, then these contexts DO contain the restDelimSym;
		// then add the restDelimSym to the sigma so that OTHER
		// doesn't cover it)

		if (leftContext.getContainsOther()) {
			if (leftContext.getFromSymtab()) {
				// then work with a copy to avoid changing something
				// in the symbol table
				leftContext = lib.CopyFst(leftContext) ;
			}
			leftContext.getSigma().add(symmap.putsym(hulden.restDelimSym)) ;
		}
		if (rightContext.getContainsOther()) {
			if (rightContext.getFromSymtab()) {
				rightContext = lib.CopyFst(rightContext) ;
			}
			rightContext.getSigma().add(symmap.putsym(hulden.restDelimSym)) ;
		}

		// each individual context in a restriction expression gets
		// interpreted as
		// leftContext  __RD  [\__RD]*  __RD  rightContext

		Fst resultFst = lib.Concat5Fsts(leftContext,
									lib.OneArcFst(hulden.restDelimSym),
									hulden.NotRestDelimStarFst(),
									lib.OneArcFst(hulden.restDelimSym),
									rightContext
								) ;
		lib.OptimizeInPlace(resultFst) ;

		return resultFst ;
	}

	Fst newNotWordBoundStarFst() {
		int ruleWordBoundaryCpv = symmap.putsym(hulden.ruleWordBoundarySym) ;

		// Hulden \.#.*
		Fst notWordBoundStar = lib.UniversalLanguageFst() ;
		notWordBoundStar.getSigma().add(ruleWordBoundaryCpv) ;
		return notWordBoundStar ;
	}

	int getIntValue(Object obj) {
		if (obj instanceof Long) {
			return ((Long)obj).intValue() ;
		} else if (obj instanceof Double) {
			return ((Double)obj).intValue() ;
		} else {
			throw new KleeneArgException("Object sent to getIntValue is neither Long nor Double") ;
		}
	}


	// interpret an alternation rule, from rule-bits pushed on the stack
	Fst interpRule(boolean rightArrow, boolean obligatory) {
		// For one stand-alone or multiple parallel-compiled rules.
		// Parallel rules are inside a call to
		// $^__parallel(a -> b / l _ r, ...)

		// Hulden's algorithm for compiling rules, and especially 
		// parallel rules, does not allow straightforward compositionality.  
		// At this top level you kind of
		// need access to all the little bits of the rules.
		
		// Top of stack is an Integer with a rule count, which will be
		// 1 unless we're dealing with parallel-compiled rules.
		int ruleCount = ((Integer)stack.pop()).intValue() ;

		// Initialize some structures for collecting restrictions/contexts
		// for all the rules.

		// In the definition of Base there is one CP(A,B), i.e. CP(upper, lower), 
		// representing the LHS, for each rule; they are unioned together, so
		// start with an empty-language fst and union the CPs into it
		Fst BaseCPs = lib.EmptyLanguageFst() ;

		// Rule compilation involves the calculation of a restriction statement,
		// which may have multiple contexts (one for each individual context).
		// Each context L _ R interprets as [L x \x* x R], where x is a special
		// restriction delimiter (here **RD).  These networks, one for each
		// context, get unioned together, and then the resulting
		// unionOfContexts is surrounded with \x*, i.e. 
		// [\x* unionOfContexts \x*]

		Fst unionOfContexts = lib.EmptyLanguageFst() ;

		// The Constraints, one for each context (there can be more than one 
		// context per rule) are intersected together; start with .*, the
		// universal language and intersect into it.
		// Constraints are used only for obligatory rules.
		Fst Constraints = null ;
		
		if (obligatory) {
			// start out allowing anything
			Constraints = lib.UniversalLanguageFst() ;
		}

		// *********** loop through the rule(s) **********************

		for (int i = 0; i < ruleCount; i++) {
			// at the stop of the stack, for the first rule,
			// there is A, then B
			// call CleanupSpecialSymbolsAction() here to keep A and B
			//	from being "infected" with the special symbols
			//	used during rule compilation (if A and B contain
			//	OTHER)

			Fst A = (Fst) stack.pop() ;
			Fst B = (Fst) stack.pop() ;

			if (A.getContainsOther()) {
				A = hulden.CleanupSpecialSymbolsAction(A) ;
			}
			if (B.getContainsOther()) {
				B = hulden.CleanupSpecialSymbolsAction(B) ;
			}

			// work with copies of A and B (may not be from the symtab,
			// but still need to be preserved here)--UnionIntoFirstInPlace() is
			// destructive of the first argument, as desired here.
			BaseCPs = lib.UnionIntoFirstInPlace(BaseCPs, 
			                               hulden.CP(lib.CopyFst(A), lib.CopyFst(B))) ;

			// Get the count of contexts for this rule
			int contextCount = ((Integer)stack.pop()).intValue() ;

			// loop through the contexts

			for (int j = 0; j < contextCount; j++) {
				// Pop first the left context, 
				// then pop the right context

				// N.B.  Upper() or Lower() has already been called on L and R, and
				// CleanupSpecialSymbolsContext() was called inside 
				//	Upper() and Lower()
				Fst L = (Fst)stack.pop() ;
				Fst R = (Fst)stack.pop() ;

				Fst cp = hulden.CP(lib.CopyFst(A), lib.CopyFst(B)) ;

				Fst dfs = hulden.DeleteFirstSymbol(cp) ;

				// There is one context restriction for each context
				// each context L _ DeleteFirstSymbol(CP(A,B)) R

				Fst restContextFst = interpRestrictionContext(lib.CopyFst(L),
															  lib.Concat(dfs, lib.CopyFst(R))
															  ) ;
				// UnionIntoFirstInPlace() is destructive of the first argument, as
				// desired here
				unionOfContexts = lib.UnionIntoFirstInPlace(unionOfContexts, 
				                                       restContextFst) ;

				// KRB:  shouldn't need to use lib.CopyFst(L), lib.CopyFst(R)
				// and lib.CopyFst(A) in augmenting Constraints, but try
				// them for now.

				if (obligatory) {
					Fst unrewritten = null ;

					if (rightArrow) {
						// in a rightArrow obligatory rule, need to  
						// make sure that no unrewritten As occur
						// in context
						unrewritten = hulden.Unrewritten(lib.Difference(
													lib.CopyFst(A),
													lib.OneArcFst(lib.Epsilon)
													)
						) ;
					} else {
						// in a leftArrow obligatory rule, need to
						// make sure that no unrewritten Bs occur
						// in context
						unrewritten = hulden.Unrewritten(lib.Difference(
													lib.CopyFst(B),
													lib.OneArcFst(lib.Epsilon)
													)
						) ;
					}

					Fst notContain = hulden.NotContain(lib.Concat3Fsts(
											 			lib.CopyFst(L),
														unrewritten,
														lib.CopyFst(R)
														)
								   				) ;
					Constraints = 
						lib.Intersect(Constraints, notContain) ;
				}
			}	// end of loop through contexts
		}	// end of loop through rules

		Fst Outside = lib.Concat3Fsts(	
									lib.OneArcFst(hulden.outsideMarkerSym),
									lib.Difference(
											hulden.Tape2Sig(),
											lib.OneArcFst(hulden.ruleWordBoundarySym)
									),
									lib.OneArcFst(hulden.idMarkerSym)
								 ) ;

		Fst Base = lib.Concat3Fsts(
								hulden.Boundary(), 
								lib.KleeneStar(lib.Union(Outside, BaseCPs)), 
								hulden.Boundary()
							   ) ;

		// surround with \x* unionOfContexts \x*
		Fst restrictionRhsFst = lib.Concat3Fsts(
											hulden.NotRestDelimStarFst(),
											unionOfContexts,
											hulden.NotRestDelimStarFst()
											) ;

		Fst Context = interpRestrictionExp(
									hulden.IOpen(),
									restrictionRhsFst,
									false   // no word boundary in contexts
									) ;

		// The result is the intersection of Base, Context and Constraints.
		// Context makes sure that any rule "action" upper -> lower 
		//     is inside a valid context.
		// Constraints makes sure that no unrewritten upper appears in a 
		//     context where it should be rewritten.  
		// Optional rules ->? do not use these Constraints.

		Fst Rewrite ;

		if (obligatory) {
			Rewrite = lib.Intersect3Fsts(Base, Context, Constraints) ;
		} else {
			Rewrite = lib.Intersect(Base, Context) ;
		}

		return Rewrite ;
	}


	// *******************************************************
	// End of functions written to implement alternation rules
	// *******************************************************


	private static Document parseXML(String str) 
							throws DocumentException {
		SAXReader reader = new SAXReader() ;
		Document document = reader.read(str) ;
		return document ;
	}

	private String basicNetListInfo(NetList netList) {
		return "Network list value: Size: " + netList.size() ;
	}

	private String basicFstInfo(Fst fst) {

		// Collect basic information about the new Fst for display

		int stateCount = lib.NumStates(fst) ;
		String stateStr = Integer.toString(stateCount) ;
		if (stateCount == 1) {
			stateStr += " state, " ;
		} else {
			stateStr += " states, " ;
		}
		int arcCount = lib.NumArcs(fst) ;
		String arcStr = Integer.toString(arcCount) ;
		if (arcCount == 1) {
			arcStr += " arc, " ;
		} else {
			arcStr += " arcs, " ;
		}
		String pathStr ;
		if (lib.IsCyclic(fst)) {
			pathStr = "Cyclic, " ;
		} else {
			long pathCount = lib.NumPaths(fst) ;
			pathStr = Long.toString(pathCount) ;
			if (pathCount == 1) {
				pathStr += " path, " ;
			} else {
				pathStr += " paths, ";
			}
		}

		String arityStr ;
		if (lib.IsAcceptor(fst)) {
			// looks like an acceptor to OpenFst
			if (!fst.getContainsOther()) {
				// simple case, does NOT contain OTHER
				arityStr = "Acceptor, " ;
			} else if (lib.IsSemanticAcceptor(fst)) {
				arityStr = "Acceptor, " ;
			} else {
				arityStr = "Semantic Transducer, " ;
			}
		} else {
			// definitely a transducer
			arityStr = "Transducer, " ;
		}

		String weightStr ;
		if (lib.IsUnweighted(fst)) {
			weightStr = "Unweighted, " ;
		} else {
			weightStr = "Weighted, " ;
		}
		String otherStr ;
		if (fst.getContainsOther()) {
			otherStr = "Contains OTHER" ;
		} else {
			otherStr = "Closed Sigma" ;
		}
		String sapRtnStr = "" ;
		if (fst.getIsRtn()) {
			sapRtnStr = ", SAP RTN" ;
		}

		// GetShortFstInfo() is a library function that uses info-main.h,
		// 2012-04-19 KRB info-main.h seems to be obsolete.  The FstInfo
		// class is now defined in fst/script/info-impl.h, but the FstInfo
		// class shouldn't be used directly anymore.
		//
		//	FstInfo objects; it is possible and might be useful to get much
		//  more info from this FstInfo object.  But for a net like
		//  $net = abc ; 
		//  the <FstInfo>.NumArcs() reports 4 arcs, which might include the
		//	exit arc, but such a counting did not appear to be consistent
		String infoString = lib.GetShortFstInfo(fst) +
												", " +
												stateStr +
												arcStr +
												pathStr +
												arityStr +
												weightStr +
												otherStr +
												sapRtnStr
												;
		return infoString ;
	}

	private String getFullpath(String userTyped) {
		File file = new File(userTyped) ;
		String fullpath = "" ;

		if (userTyped.startsWith("~/")) {
			// rooted at the user's home directory
			fullpath = System.getProperty("user.home") + userTyped.substring(1) ;
		} else {
			try {
				fullpath = file.getCanonicalPath() ;
			} catch (IOException ioe) {
				// KRB--how to handle this?
				ioe.printStackTrace() ;
			}
		}
		return fullpath ;
	}

    // During the interpretation of the AST, the value of a node is typically 
	// pushed onto the stack
    Stack<Object> stack ;

	public void setMainFrame() {
		mainFrame = env.getCurrentFrame() ;
	}

	// InterpreterKleeneVisitor.jdelete is called from
	// the finalize() method of Fst.java; only this interpreter
	// should know about the underlying native C++ library
	public static void jdelete(long ptr) {
		OpenFstLibraryWrapper.CppDelete(ptr) ;
		return ;
	}

    public void reset() {
		stack.clear() ;
		return ;
    }

	// Limit the number of analysis/generation results shown in the GUI
	// KRB: magic number
	int resultDisplayLimit = 100 ;
	int generate = 0 ;
	int analyze = 1 ;

	private void listOutputStrings(String msg, Fst resultFst, Object data) {
		PseudoTerminalInternalFrame terminal = 
					((InterpData)data).getGUI().getTerminal() ;
		terminal.appendToHistory("\n" + msg + "\n") ;

		long stringCount = lib.NumPaths(resultFst) ;
		if (stringCount == 0) {
			terminal.appendToHistory("(resulting language is empty)") ;
		} else if (stringCount == -1) {
			// then has loops, infinite language
			terminal.appendToHistory("(resulting language is infinite)") ;
		} else if (stringCount <= resultDisplayLimit) {
			// then just list them (parameterize this figure later)
			FstStringLister lister = new FstStringLister(terminal, symmap) ;
			// second arg 0 is for input side, 
			//					1 for output side
			lib.ListAllStrings(resultFst, 0, lister) ;
			// ptr to fst, 0 for input projection, false for printEpsilons
			// the projection doesn't matter here, of course, because the
			// network was already reduced above to the input projection
		} else {
			terminal.appendToHistory("(resulting language exceeds resultDisplayLimit: " + resultDisplayLimit + ")") ;
		}
	}

	// testAnalyze() is called from the 'test' function (in the GUI)
	// apply the testFst to one string (an array of CPVs)
	public void testAnalyze(Fst testFst, int[] cpvArray, Object data) {
		Fst resultFst = lib.ApplyToOneString(testFst, cpvArray, analyze) ;

		env.put("$analysisresult", resultFst) ;
		// add an Icon to the Symtab window
		addToGUISymtab("$analysisresult", SymtabIcons.NET_IMAGE, data) ;

		listOutputStrings("Analysis results:", resultFst, data) ;
	}

	// testGenerate() is called from the 'test' function (in the GUI)
	public void testGenerate(Fst testFst, int[] cpvArray, Object data) {
		// For comments, see testAnalyze() above
		Fst resultFst = lib.ApplyToOneString(testFst, cpvArray, generate) ;

		env.put("$generationresult", resultFst) ;
		// add an Icon to the Symtab window
		addToGUISymtab("$generationresult", SymtabIcons.NET_IMAGE, data) ;

		listOutputStrings("Generation results:", resultFst, data) ;
	}

	void writeXMLhelper(Fst fst, String fullpath, String encoding) {
		FstXmlWriter fstXmlWriter = 
			new FstXmlWriter(lib.ArcType(fst),
							fst.getSigma(), 
							symmap, 
							fst.getContainsOther(), 
							new File(fullpath),
							encoding) ;

		// Call Fst2xml to iterate through the native Fst 
		//	states and arcs.  (I can't do this from Java.) It will make calls
		// back to methods in the fstXmlWriter to do the actual output 
		//	to file.
		// (The C++ code has iterators, but Unicode file output 
		//	from C++ is not
		// worth the trouble.  Even if the C++ code were written to 
		//	write the XML
		// directly, it would still have to make calls back to the 
		//	symmap method
		// .getsym(i)
		// to convert the int-value labels to strings.

		lib.Fst2xml(fst, fstXmlWriter, symmap.getStartPuaCpv()) ;
	}

	private Fst xml2fst(String filepath) throws Exception {

		// read an XML file representing a network, return the network

		final Fst fst = lib.EmptyLanguageFst() ;

		final HashSet<Integer> sigma = fst.getSigma() ;

		SAXReader reader = new SAXReader() ;  // SAXReader from dom4j

		// each SAXReader handler must define onStart() and onEnd() methods

		// when the kleeneFst element is first found
		reader.addHandler("/kleeneFst", new ElementHandler() {
			public void onStart(ElementPath path) {
				Element current = path.getCurrent() ;

				// semiring is an attribute on the kleeneFst node
				String semiring = current.attribute("semiring").getValue() ;
			}
			public void onEnd(ElementPath path) {}
		}) ;

		reader.addHandler("/kleeneFst/sigma", new ElementHandler() {
			public void onStart(ElementPath path) {
				if
					(path.getCurrent().attribute("containsOther").getValue().equals("true"))
					{
						fst.setContainsOther(true) ;
					} else {
						fst.setContainsOther(false) ;
					}
				;
			}
			public void onEnd(ElementPath path) {}
		}) ;

		reader.addHandler("/kleeneFst/sigma/sym", new ElementHandler() {
			public void onStart(ElementPath path) {}
			public void onEnd(ElementPath path) {
				Element sym = path.getCurrent() ;
				sigma.add(symmap.putsym(sym.getText())) ;

				sym.detach() ;
			}
		}) ;

		// when the arcs element is first found
		reader.addHandler("/kleeneFst/arcs", new ElementHandler() {
			public void onStart(ElementPath path) {
				// grab the two attrs and convert to int
				int startState = 
					Integer.parseInt(path.getCurrent().attribute("start").getValue()) ;
				int numStates =  
					Integer.parseInt(path.getCurrent().attribute("numStates").getValue())  ;
				lib.AddStates(fst, numStates) ; 
				// native function, add this many  
				// states to the new Fst

				lib.SetStart(fst, startState) ; // set the start state
			}
			public void onEnd(ElementPath path) {}
		}) ;  

		// handle each whole arc element
		reader.addHandler("/kleeneFst/arcs/arc",
				new ElementHandler() {

					// in an ElementHandler, need to supply .onStart(),
					// called when the start tag is found, and
					// .onEnd(), which is called when the end tag is found.

					public void onStart(ElementPath path) {}
					public void onEnd(ElementPath path) {

						// retrieve the entire arc element
						Element arc = path.getCurrent() ;

						// these two are always present
						int src_id = Integer.parseInt(arc.attribute("s").getValue()) ;
						int dest_id = Integer.parseInt(arc.attribute("d").getValue()) ;

						// there will be either one io attr xor separate i and o attrs
						// (keep the io option to facilitate hand-written XML files)

						String input ;
						String output ;
						Attribute io = arc.attribute("io") ;
						if (io != null) {
							input = io.getValue() ;
							output = io.getValue() ;
						} else {
							input = arc.attribute("i").getValue() ;
							output = arc.attribute("o").getValue() ;
						}

						if (!symmap.containsKey(input)) {
							// symbol name in XML file not in the 
							//  	current internal symtab
							symmap.putsym(input) ;
						}
						if (!symmap.containsKey(output)) {
							symmap.putsym(output) ;
						}

						// the w attr is optional in the arc elmt
						Attribute w = arc.attribute("w") ;
						if (w != null) {
							// call AddArc to add an arc to the fst 
							//		being built from the XML file description
							// semiring generalization point
							lib.AddArc(fst, src_id, 
								symmap.getint(input), 
								symmap.getint(output),
								Float.parseFloat(w.getValue()), 
								dest_id) ;

						} else {
							// semiring generalization point
							lib.AddArcNeutralWeight(fst, src_id, 
								symmap.getint(input),
								symmap.getint(output),
								dest_id) ;
						}

						arc.detach() ;
					}
		}) ;
		
		// for each full final element
		reader.addHandler("/kleeneFst/arcs/final",
				new ElementHandler() {
					public void onStart(ElementPath path) {}
					public void onEnd(ElementPath path) {
						Element arc = path.getCurrent() ;

						// s attr is always present
						int src_id = Integer.parseInt(arc.attribute("s").getValue()) ;

						// the w attr is optional
						Attribute w = arc.attribute("w") ;
						if (w != null) {
							lib.SetFinal(fst, src_id, 
									Float.parseFloat(w.getValue())) ; 
						} else {
							lib.SetFinalNeutralWeight(fst, src_id) ;
						}

						arc.detach() ;
					}
		}) ;

		Document document = null ;

		// the actual XML reading/parsing is done here
		try {
			// XmlReader detects the encoding of the XML document and
			// handles BOMs, including the UTF-8 BOMs that Java usually
			// chokes on
			document = reader.read(new XmlReader(new FileInputStream(filepath))) ;

			// Old, pre-XmlReader code
			//if (encoding.equals("UTF-8")) {
			//	// then need to work around SUN's irresponsible decision not to
			//	//  handle the optional UTF-8 BOM correctly
			//	document = reader.read(new InputStreamReader(
			//				new UTF8BOMStripperInputStream(new FileInputStream(filepath)),
			//				"UTF-8")
			//					  ) ;
			//} else {
			//	document = reader.read(new InputStreamReader(
			//									new FileInputStream(filepath),
			//									encoding)
			//					  ) ;
			//}
		} catch (DocumentException de) {
			// dom4j DocumentException extends Exception
			de.printStackTrace() ;
			throw de ;
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace() ;
			throw fnfe ;
		} catch (Exception e) {
			e.printStackTrace() ; 
			throw e ;
		}

		lib.CorrectSigmaOtherInPlace(fst) ;

		return fst ;
	}

	// KRB:  refine the type checking; perhaps store the return type
	// somehow in the FuncValue object
	private boolean typeMatch(String id, Object obj) {
		// check longer prefixes first

		if (id.startsWith("$^^") && obj instanceof FuncValue) 
			return true ;
		else if (id.startsWith("$^") && obj instanceof FuncValue)
			return true ;

		else if (id.startsWith("$@^^") && obj instanceof FuncValue)
			return true ;
		else if (id.startsWith("$@^") && obj instanceof FuncValue)
			return true ;
		else if (id.startsWith("$@") && obj instanceof NetList)
			return true ;
		
		else if (id.startsWith("$") && obj instanceof Fst)
			return true ;

		else if (id.startsWith("#^^") && obj instanceof FuncValue)
			return true ;
		else if (id.startsWith("#^") && obj instanceof FuncValue)
			return true ;

		else if (id.startsWith("#@^^") && obj instanceof FuncValue)
		 	return true ;
		else if (id.startsWith("#@^") && obj instanceof FuncValue)
			return true ;
		else if (id.startsWith("#@") && obj instanceof NumList)
			return true ;
	
		// void and voidvoid functions
		else if (id.startsWith("^^") && obj instanceof FuncValue)
			return true ;
		else if (id.startsWith("^") && obj instanceof FuncValue)
			return true ;

		else if (id.startsWith("#") && ((obj instanceof Long) || (obj instanceof Double)))
			return true ;
		else
			return false;
	}

	private int getParamIndex(String id, ArrayList<ParamSlot> pal) {
		int index = 0 ;
		for (Iterator<ParamSlot> iter = pal.iterator(); iter.hasNext() ; ) {
			ParamSlot ps = iter.next() ;
			if (ps.getName().equals(id)) {
				return index ;
			}
			index++ ;
		}
		return -1 ;  // not found
	}

	// bind parameters params in a function call

    private void bind_params(ArrayList<ParamSlot> pal, ArgCounts ac, Object data) {
		// The pal (param array list) comes from the FuncObject itself, indicating
		//    the parameters that the function expects.
		// The ArgCounts object was created when the arg_list was evaluated, and it
		//    contains positional_arg_count and named_arg_count
		int positional_args_count = ac.getPositionalArgsCount() ;
		int named_args_count = ac.getNamedArgsCount() ;

		// the arguments should be on the stack;
		// values (corresponding to positional args) on top
		// with NamedArgs (corresponding to named args) underneath,
		// so all the arguments are popped off the stack in their original
		// syntactic order.

		int param_count = pal.size() ;  // the number of params to bind

		// for each positional argument, there must be a corresponding
		// required or optional parameter (with or without a default value); 
		// First assign params left-to-right from all positional arguments.

		if (positional_args_count > param_count) {
			throw new FuncCallException("Function called with more positional arguments than there are parameters.") ;
		}

		// Loop through the positional_args, if any, and bind the
		// corresponding ParamSlots l->r in the pal.  The positional-arg values
		// are on the stack, pushed on in reverse syntactic order, so they
		// can be popped off in the original syntactic order.
		for (int i = 0; i < positional_args_count; i++) {
			// get the i-th ParamSlot
			ParamSlot ps = pal.get(i) ;
			// pop the corresponding argument value off the stack
			Object obj = stack.pop() ;
			if (typeMatch(ps.getName(), obj)) {
				ps.setValue(obj) ;
			} else {
				throw new FuncCallException("Attempt to set arg named " + ps.getName() + " with a positional argument of the incorrect type.") ;
			}
		}

		// Now handle the named arguments

		for (int i = 0; i < named_args_count; i++) {
			// Each named arg is represented as a NamedArg object on the stack.
			NamedArg na = (NamedArg) stack.pop() ;
			String id = na.getName() ;
			// Find the parameter with that id
			int ind = getParamIndex(id, pal) ;
			if (ind == -1) {
				throw new FuncCallException("Attempt to set non-existent param named " + id + ".") ;
			}
			ParamSlot ps = pal.get(ind) ;

			if (ps.hasValue()) {
				// was already set by a positional argument
				throw new FuncCallException("Attempt to set param " + id + " twice, with both a positional and a named argument.") ;
			}
			// no type problem if the names matched;
			// set the optional parameter value from the named argument
			ps.setValue(na.getValue()) ;
		}

		// now pass through the ArrayList making sure that all Param Slots
		// have been set, one way or another.
		
		for (int i = 0; i < param_count; i++) {
			ParamSlot ps = pal.get(i) ;
			if (!ps.hasValue()) {
				if (ps.hasDefault()) {
					ps.setValueToDefault() ;
				} else {
					throw new FuncCallException("Param " + ps.getName() + 
					" not set by either a positional or a named argument.") ;
				}
			}
		}

		// Finally, actually bind the param names to the arg values, 
		// in the current Frame
		
		for (int i = 0; i < param_count; i++) {
			ParamSlot ps = pal.get(i) ;
			env.put(ps.getName(), ps.getValue()) ;
		}
	}

	private void addToGUISymtab(String id, String iconFileName, Object data) {
		((InterpData)data).getGUI().addSymtabIconButton(id, iconFileName) ;
	}

	private void removeFromGUISymtab(String id, Object data) {
		((InterpData)data).getGUI().removeSymtabIconButton(id) ;
	}

	private boolean isOpenFstRtnConventions() {
		if ( ((Long)env.get("#KLEENErtnConventions")).longValue() ==
			 ((Long)env.get("#KLEENEopenFstRtnConventions")).longValue()
			 ) {
			return true ;
		} else {
			return false ;
		}
	}

	// End of helper functions

	/**************  visit methods for an AST produced by the parser ******
	 *
	 * in classic Visitor fashion, the root of the AST is sent a message to
	 * "accept" this Visitor
	 *
	 **********************************************************************/

    // the method for SimpleNode should never be called,
	//   but for some reason it seems to be required
    public Object visit(SimpleNode node, Object data) {
		System.out.println("Error: call to visit(SimpleNode) method in InterpreterKleeneVisitor") ;
		return data ;
    }
    public Object visit(ASTprogram node, Object data) {
		// Not currently used?  Scripts are parsed and executed
		// one statement at a time?
		node.childrenAccept(this, data) ;
		return data ;
    }
    public Object visit(ASTstand_alone_block node, Object data) {
		// these blocks are groups of statements, evaluated in 
		//		a new Frame

		env.allocateFrame() ;
		int childCount = node.jjtGetNumChildren() ;
		for (int i = 0 ; i < childCount ; i++) {
			try {
		    	node.jjtGetChild(i).jjtAccept(this, data) ;
		    	if (((InterpData)data).getLoopContinue() || 
					((InterpData)data).getLoopBreak() ||
					((InterpData)data).getFuncReturn() ||
					((InterpData)data).getQuitSession()) {
					// bail out of this block, but don't reset
					// the fields in data
					break ;
	    		}
			} catch (RuntimeException re) {
				env.releaseFrame() ;
				throw re ;
			}
		}
		env.releaseFrame() ;
		return data ;
    }
    public Object visit(ASTif_else_block node, Object data) {
		// these blocks seen in if-else statements
		// see ASTloop_block for the blocks in while and until
		// see ASTfunc_block for the blocks seen in function definitions
		// see ASTstand_alone_block for block-grouped statements
		int childCount = node.jjtGetNumChildren() ;
		for (int i = 0 ; i < childCount ; i++) {
		    node.jjtGetChild(i).jjtAccept(this, data) ;
		    if (((InterpData)data).getLoopContinue() || 
				((InterpData)data).getLoopBreak() ||
				((InterpData)data).getFuncReturn() ||
				((InterpData)data).getQuitSession()) {
				// bail out of this if-then block, but don't reset
				// the fields in data
				break ;
	    	}
		}
		return data ;
    }
    public Object visit(ASTloop_block node, Object data) {
		// these blocks seen in while and until statements
		// see ASTblock for the blocks in if-then
		// see ASTfunc_block for the blocks seen in function 
		// definitions

		int childCount = node.jjtGetNumChildren() ;
		for (int i = 0 ; i < childCount ; i++) {
	    	node.jjtGetChild(i).jjtAccept(this, data) ;
	    	if (((InterpData)data).getLoopContinue() || 
				((InterpData)data).getLoopBreak() ||
				((InterpData)data).getFuncReturn() ||
				((InterpData)data).getQuitSession()) {
				// bail out of this if-then block, but don't reset
				// the fields in data
				break ;
	    	}
		}
		return data ;
    }
    public Object visit(ASTfunc_block node, Object data) {
		int childCount = node.jjtGetNumChildren() ;
		for (int i = 0 ; i < childCount ; i++) {
	    	node.jjtGetChild(i).jjtAccept(this, data) ;
	    	// there shouldn't be break or next statements immediately
	    	//   in such a block (syntactic? or semantic error?)
	    	// if it hit a return stmt during the execution of this stmt
	    	// (at any depth), then .getFuncReturn() will be true
	    	if (((InterpData)data).getFuncReturn()) {
	        	((InterpData)data).setFuncReturn(false) ;
				break ;
	    	} else if (((InterpData)data).getQuitSession()) {
				// don't reset the data field here
				break ;
	    	}
		}
		return data ;
    }
	public Object visit(ASTsap_rtn_conventions_statement node, Object data) {
		Long l = (Long)env.get("#KLEENEsapRtnConventions") ;
		// use putGlobal() to cause the binding to be made in the
		// global symbol table, so it will be available when
		// processing functions defined in .kleene/global/predefined.kl
		env.putGlobal("#KLEENErtnConventions", env.get("#KLEENEsapRtnConventions")) ;
		outputInterpMessage("// Setting SapRtnConventions", data) ; 
		return data ;
	}
    public Object visit(ASTcontinue_statement node, Object data) {
		// "return" the information that a 'continue' command was
		//    encountered, in the data
		((InterpData)data).setLoopContinue(true) ;
	 	return data ;
    }
    public Object visit(ASTbreak_statement node, Object data) {
		// "return" the information that a 'break' command was
		//    encountered, in the data
		((InterpData)data).setLoopBreak(true) ;
	 	return data ;
    }
    public Object visit(ASTquit_statement node, Object data) {
		// "return" the information that a 'quit' command was
		//     encountered, in the data
		((InterpData)data).setQuitSession(true) ;
	 	return data ;
    }
	public Object visit(ASTempty_statement node, Object data) {
		// parser found an isolated semicolon
		// no-op
		return data ;
	}
    public Object visit(ASTnet_assignment node, Object data) {
		// Syntax:  $net = a*b+(dog|cat)s? ;

		// two daughters:  
		//		net_id
		//		regexp
		
		// Don't evaluate the zeroth daughter (which would return
		// a network value; rather just get the image of this LHS

		String net_id = ((ASTnet_id)node.jjtGetChild(0)).getImage() ;

		// If net_id is already bound in the current symtab, either to a
		// normal object or to an ExternValue (containing a handle to the
		// non-local Frame where net_id is bound), then this assignment will
		// just reset the existing binding.  
		
		// Otherwise, if there is no local
		// binding of net_id at all, this assignment statement is intended
		// to both declare a new _local_ net_id key and to bind it to the
		// value of the right-hand-side in the current local symtab.
		// Problem: if the right-hand-side refers to (i.e. retrieves the
		// value of) net_id as a free (i.e. non-local) variable, then the
		// result is confusing and usually an error.

		// So when, on the RHS, a variable is free (not found in the 
		//     currentFrame)
		// then the currentFrame is given a placeholder binding of 
		//     key->FreeVariable,
		// to block any subsequent attempt to give key a real local binding in
		// currentFrame

		node.jjtGetChild(1).jjtAccept(this, data) ;
		// Should leave an Fst object on the stack (see Fst.java)
		Fst fst = (Fst)(stack.pop()) ;

		// relate the net_id and the fst in the current Frame
		env.put(net_id, fst) ;

		// Now if the current Frame is the main Frame, add
		// an appropriate SymbolIconButton to the window.  In other
		// words, do NOT try to add a SymbolIconButton if the
		// current Frame is a temporary one used to process a 
		// function call, or a stand_alone_block,
		// and do NOT try to add a SymbolIconButton
		// if the interpreter is working on a startup script (i.e.
		// is not operating within the interactive GUI)

		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			// then we're in the GUI

			// add an Icon to the Symtab window
			addToGUISymtab(net_id, SymtabIcons.NET_IMAGE, data) ;
			PseudoTerminalInternalFrame terminal = 
					((InterpData)data).getGUI().getTerminal() ;
			terminal.appendToHistory("// " + basicFstInfo(fst)) ;
		}

		return data ;
    }
    public Object visit(ASTnet_id node, Object data) {
		// Called only when the net_id is on the RHS (_not_ the LHS)
		// of an assignment statement (i.e. inside a regular
		// expression or right-linear production); the value of a net_id
		// like $mynet is an Fst

		String net_id = node.getImage() ;

		// with the String name, retrieve the Fst from the environment
		Fst fst = (Fst) env.get(net_id) ;
		if (fst != null) {
			// The net_id was found in a symbol table.
			//
			// Fst is a Java wrapper object around a long int that stores a
			// (basically C++) pointer to a network.  Need to mark this
			// Fst as being "fromSymtab" if, as here, the value came from a
			// symbol table (and therefore cannot be changed, i.e. must be
			// persistent in case the net_id is referred to again).
			fst.setFromSymtab(true) ;
			// leave the Fst on the stack
			stack.push(fst) ;
		} else {
			// attempt to refer to (use) an undefined variable
			throw new UndefinedIdException("Undefined net_id: " + net_id) ;
		}
		return data ;
    }
    public Object visit(ASTiterator_net_id node, Object data) {
		// foreach ($iterator_net_id in $@arr) { }

		// should never be called; used only to get the image

		return data ;
    }
    public Object visit(ASTiterator_num_id node, Object data) {
		// foreach ($iterator_num_id in #@arr) { }

		// should never be called; used only to get the image

		return data ;
    }
    public Object visit(ASTrrprod_definition node, Object data) {
		// Definition of a "production" in a right-recursive 
		//		phrase-structure grammar
		// Syntax:  $>foo = a b c $>bar ;
		// parser returns AST tree (_not_ evaluated at this time)
		// rrprod_definition
		//     rrprod_id: $>foo
		//     rrProdRHS
		//         regexp
		//
		// Need to allocate an RrKleeneVisitor and tell the AST regexp
		// daughter to accept it.  This "right-recursive" visitor 
		//	 1) checks to make sure that any rrprod_id references are really 
		//			right-linear in the AST, and 
		//   2) returns a HashSet of such "dependencies".
		
		// This method directly handles a rrprod_id on the LHS.
		// Do not eval the rrprod_id, but just get the Image directly
		// (When a rrprod_id on the RHS is _evaluated_, see ASTrrprod_id.)
		String rrprod_id = ((ASTrrprod_id) node.jjtGetChild(0)).getImage() ;
		symmap.putsym(rrprod_id) ;  // needs to be in the symbol table
		// Note that an rrprod_id gets a negative integer value

		// get the RHS AST (_not_ evaluated now) for storage in the 
		//    symbol table
		ASTrrProdRHS rhs = (ASTrrProdRHS) node.jjtGetChild(1) ;

		// Do NOT call .jjtAccept(this, data) here.
		// Just check the AST for well-formedness and save it in the 
		//    symbol table
		// (along with a HashSet of its dependencies).

		// Tell the RHS to accept a special visitor that checks to make
		// sure that the RHS contains only valid right-recursive references,
		// and returns a HashSet of them.
		// KRB:  Could the same RrKleeneVisitor be re-used?
		RrKleeneVisitor rkv = new RrKleeneVisitor() ;

		// The Boolean data value indicates whether an rrprod_id can appear
		// (in a right-recursive position); start with true.  
		// The constructor of the
		// RrKleeneVisitor instantiates the HashSet returned; it
		// contains all the rr references like $>foo found on the
		// RHS (and the Visitor makes sure that any such references
		// are in legal places).
		HashSet<String> hs = 
			(HashSet<String>) rhs.jjtAccept(rkv, new Boolean(true)) ;

		// save the AST, with the set of dependencies, in the symbol table for
		// later evaluation as part of a whole grammar of rr productions.  
		// The value of the rrprod_id in the symbol table is an RrProdObject.
		env.put(rrprod_id, new RrProdObject(rhs, hs)) ;

		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			addToGUISymtab(rrprod_id, SymtabIcons.RRPROD_IMAGE, data) ;
		}

		return data ;
    }
    public Object visit(ASTrrprod_id node, Object data) {
		// This method is for rrprod_id ($>foo) on the RHS of a 
		//   right-linear production;
		// Compare to handling of multichar symbols

		String image = node.getImage() ;
		int negcpv = symmap.putsym(image) ;  
		// will be a NEGATIVE integer

		// A special temporary value; arcs labeled with negative
		// labels will be turned into eps:eps arcs, and repointed to
		// the start state of the target Fst, (via call to the $^start()
		// function; see rrGrammarConnect in kleeneopenfst.cc

		// create a two-state, one arc Fst with a 
		//		negative negcpv:negcpv label

		stack.push(lib.OneArcFst(negcpv)) ;

		return data ;
    }
    public Object visit(ASTnum_assignment node, Object data) {
		// two children 
		//		num_id
		//		numexp
		// Syntax:  #num = 2 + 2 ;

		// See comments for ASTnet_assignment

		String num_id = ((ASTnum_id)node.jjtGetChild(0)).getImage() ;

		node.jjtGetChild(1).jjtAccept(this, data) ;

		// internally in Kleene, all "ints" are stored as Long,
		// and all "floats" are stored as Double
		Object obj = stack.pop() ;  // a Long or Double object
		env.put(num_id, obj) ;

		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			addToGUISymtab(num_id, SymtabIcons.NUM_IMAGE, data) ;

			PseudoTerminalInternalFrame terminal = 
				((InterpData)data).getGUI().getTerminal() ;

			String value ;
			if (obj instanceof Long) {
				value = "Long value: " + Long.toString((Long)obj) ;
			} else {
				value = "Double value: " + Double.toString((Double)obj) ;
			}
			terminal.appendToHistory("// " + value) ;
		}
		return data ;
    }
    public Object visit(ASTnum_id node, Object data) {
		// called only when the num_id is on the RHS
		// need to look up and push the value from the environment
		String num_id = node.getImage() ;
		Object obj = env.get(num_id) ;
		if (obj != null) {
			if (obj instanceof Long) {
				stack.push((Long)obj) ;
			} else if (obj instanceof Double) {
				stack.push((Double)obj) ;
			}
	    	return data ;
		} else {
	    	throw new UndefinedIdException("Undefined num_id: " + num_id) ; 
		}
    }
    public Object visit(ASTnet_func_assignment node, Object data) {
		// For a function "assignment", involving an equal sign, and
		//		returning a net (Fst) value, e.g.
		// $^myfunc = $^($a, $b) { return $a | $b ; }
		// or
		// $^yourfunc = $^myfunc ; // creates an alias

		String func_id = ((ASTnet_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		// should leave a FuncValue on the stack

		//  Bind the func_id to the FuncValue in the environment
		env.put(func_id, stack.pop()) ;

		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			addToGUISymtab(func_id, SymtabIcons.FUNC_IMAGE, data) ;
		}
		return data ;
    }
    public Object visit(ASTnet_list_func_assignment node, Object data) {
		// For a function "assignment", involving an equal sign, and
		//		returning a net list value, e.g.
		// $@^myfunc = $@^($a, $b) { return $a | $b ; }
		// or
		// $@^yourfunc = $@^myfunc ; // creates an alias

		String func_id = ((ASTnet_list_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		// should leave a FuncValue on the stack

		//  Bind the func_id to the FuncValue in the environment
		env.put(func_id, stack.pop()) ;

		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			addToGUISymtab(func_id, SymtabIcons.FUNC_IMAGE, data) ;
		}
		return data ;
    }
    public Object visit(ASTnum_list_func_assignment node, Object data) {
		// For a function "assignment", involving an equal sign, and
		//		returning a num list value, e.g.
		// #@^myfunc = #@^($a, $b) { return $a | $b ; }
		// or
		// #@^yourfunc = #@^myfunc ; // creates an alias

		String func_id = ((ASTnum_list_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		// should leave a FuncValue on the stack

		//  Bind the func_id to the FuncValue in the environment
		env.put(func_id, stack.pop()) ;

		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			addToGUISymtab(func_id, SymtabIcons.FUNC_IMAGE, data) ;
		}
		return data ;
    }
    public Object visit(ASTvoid_func_assignment node, Object data) {
		// For a function "assignment", involving an equal sign, and
		//		returning nothing.

		String func_id = ((ASTvoid_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		// should leave a FuncValue on the stack
		env.put(func_id, stack.pop()) ;

		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			addToGUISymtab(func_id, SymtabIcons.FUNC_IMAGE, data) ;
		}
		return data ;
    }
    public Object visit(ASTnet_func_definition node, Object data) {
		// For a net function "definition", e.g.
		// Syntax:  $^myfunc($a, $b...) {  }
		// three daughters in the AST
		//    net_func_id
		//    param_list  (with 0,1 or 2 daughters)
		//        required_params
		//        optional_params (with default values indicated)
		//    func_block

		String funcName = ((ASTnet_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;   // ASTparam_list
		// should leave an ArrayList<ParamSlot> on the stack
		ArrayList<ParamSlot> pal = (ArrayList<ParamSlot>)stack.pop() ;
		
		// add to the environment (symtab)
		// A FuncValue always carries a handle to the current frame, i.e. the
		// 	frame in which the function was defined.  References are searched
		//	through the static links in the Environment, and this implements
		//	lexical scope.
		env.put(funcName, new FuncValue(env.getCurrentFrame(),  
										// the static frame
					                    pal, 
								        (ASTfunc_block) node.jjtGetChild(2))) ;
		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			addToGUISymtab(funcName, SymtabIcons.FUNC_IMAGE, data) ;
		}

		return data ;
    }
    public Object visit(ASTnet_list_func_definition node, Object data) {
		// For a net arr function "definition", e.g.
		// Syntax:  $@^myfunc($a, $b...) {  }
		// three daughters in the AST
		//    net_list_func_id
		//    param_list  (with 0,1 or 2 daughters)
		//        required_params
		//        optional_params (with default values indicated)
		//    func_block

		String funcName = ((ASTnet_list_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;   // ASTparam_list
		// should leave an ArrayList<ParamSlot> on the stack
		ArrayList<ParamSlot> pal = (ArrayList<ParamSlot>)stack.pop() ;
		
		// add to the environment (symtab)
		// A FuncValue always carries a handle to the current frame, i.e. the
		// 	frame in which the function was defined.  References are searched
		//	through the static links in the Environment, and this implements
		//	lexical scope.
		env.put(funcName, new FuncValue(env.getCurrentFrame(),  
										// the static frame
					                    pal, 
								        (ASTfunc_block) node.jjtGetChild(2))) ;
		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			addToGUISymtab(funcName, SymtabIcons.FUNC_IMAGE, data) ;
		}

		return data ;
    }
    public Object visit(ASTnum_list_func_definition node, Object data) {
		// For a num arr function "definition", e.g.
		// Syntax:  #@^myfunc(1, 2...) {  }
		// three daughters in the AST
		//    num_list_func_id
		//    param_list  (with 0,1 or 2 daughters)
		//        required_params
		//        optional_params (with default values indicated)
		//    func_block

		String funcName = ((ASTnum_list_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;   // ASTparam_list
		// should leave an ArrayList<ParamSlot> on the stack
		ArrayList<ParamSlot> pal = (ArrayList<ParamSlot>)stack.pop() ;
		
		// add to the environment (symtab)
		// A FuncValue always carries a handle to the current frame, i.e. the
		// 	frame in which the function was defined.  References are searched
		//	through the static links in the Environment, and this implements
		//	lexical scope.
		env.put(funcName, new FuncValue(env.getCurrentFrame(),  
										// the static frame
					                    pal, 
								        (ASTfunc_block) node.jjtGetChild(2))) ;
		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			addToGUISymtab(funcName, SymtabIcons.FUNC_IMAGE, data) ;
		}

		return data ;
    }
    public Object visit(ASTvoid_func_definition node, Object data) {
		// for a void function "definition", e.g.
		// ^myfunc (#num) { info #num ; }
		// three daughters in the AST, now look like
		//    void_func_id
		//    param_list  (with 0,1 or 2 daughters)
		//        required_params
		//        optional_params (with default values indicated)
		//    func_block

		String funcName = ((ASTvoid_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;   // ASTparam_list
		// should leave an ArrayList<ParamSlot> on the stack
		ArrayList<ParamSlot> pal = (ArrayList<ParamSlot>) stack.pop() ;
		
		// add to the environment (symtab)
		env.put(funcName, new FuncValue(env.getCurrentFrame(),  
										// the static frame
					                    pal, 
								        (ASTfunc_block) node.jjtGetChild(2))) ;
		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			addToGUISymtab(funcName, SymtabIcons.FUNC_IMAGE, data) ;
		}

		return data ;
    }
    public Object visit(ASTnum_func_assignment node, Object data) {
		String func_id = ((ASTnum_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		env.put(func_id, stack.pop()) ;
		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			addToGUISymtab(func_id, SymtabIcons.FUNC_IMAGE, data) ;
		}
		return data ;
    }
    public Object visit(ASTnum_func_definition node, Object data) {
		// three daughters:  num_func_id  param_list  func_block
		String funcName = ((ASTnum_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		// Should leave an ArrayList on the stack
		ArrayList<ParamSlot> pal = (ArrayList<ParamSlot>) stack.pop() ;
		env.put(funcName, new FuncValue(env.getCurrentFrame(), 
					                    pal,
										(ASTfunc_block) node.jjtGetChild(2))) ;
		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			addToGUISymtab(funcName, SymtabIcons.FUNC_IMAGE, data) ;
		}
		return data ;
    }
    public Object visit(ASTnet_func_func_assignment node, Object data) {
		// See ASTnet_assignment for comments
		String funcFunc_id = ((ASTnet_func_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		env.put(funcFunc_id, stack.pop()) ;
		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			addToGUISymtab(funcFunc_id, SymtabIcons.FUNC_IMAGE, data) ;
		}
		return data ;
    }
    public Object visit(ASTvoid_func_func_assignment node, Object data) {
		String funcFunc_id = ((ASTvoid_func_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		// should leave a FuncValue object on the stack
		env.put(funcFunc_id, stack.pop()) ;
		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			addToGUISymtab(funcFunc_id, SymtabIcons.FUNC_IMAGE, data) ;
		}
		return data ;
    }
    public Object visit(ASTnet_func_func_definition node, Object data) {
		// 3 daughters
		String funcFuncName = ((ASTnet_func_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		ArrayList<ParamSlot> pal = (ArrayList<ParamSlot>) stack.pop() ;
		env.put(funcFuncName, new FuncValue(env.getCurrentFrame(), 
					                        pal, 
											(ASTfunc_block) node.jjtGetChild(2))) ;
		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			addToGUISymtab(funcFuncName, SymtabIcons.FUNC_IMAGE, data) ;
		}

		return data ;
    }
    public Object visit(ASTvoid_func_func_definition node, Object data) {
		// 3 daughters
		String funcFuncName = ((ASTvoid_func_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		ArrayList<ParamSlot> pal = (ArrayList<ParamSlot>) stack.pop() ;
		env.put(funcFuncName, new FuncValue(env.getCurrentFrame(), 
					                        pal, 
											(ASTfunc_block) node.jjtGetChild(2))) ;
		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			addToGUISymtab(funcFuncName, SymtabIcons.FUNC_IMAGE, data) ;
		}

		return data ;
    }
    public Object visit(ASTnet_func_func_id node, Object data) {
		// This method is called for net_func_func_id on the right-hand-side,
		// so it needs to successfully look up the value (or throw
		// an exception); for an assignment, e.g. $^^func() =
		// this visit method is not called for the LHS id
		String net_func_func_id = node.getImage() ;
		Object obj = env.get(net_func_func_id) ;
		if (obj != null) {
			stack.push(obj) ;
		} else {
			throw new UndefinedIdException("Undefined net_func_func_id: " + 
			net_func_func_id) ;
		}
		return data ;
    }
    public Object visit(ASTnet_list_func_func_id node, Object data) {
		// This method is called for net_list_func_func_id on the right-hand-side,
		// so it needs to successfully look up the value (or throw
		// an exception); for an assignment, e.g. $@^^func() =
		// this visit method is not called for the LHS id
		String net_list_func_func_id = node.getImage() ;
		Object obj = env.get(net_list_func_func_id) ;
		if (obj != null) {
			stack.push(obj) ;
		} else {
			throw new UndefinedIdException("Undefined net_list_func_func_id: " + 
			net_list_func_func_id) ;
		}
		return data ;
    }
    public Object visit(ASTvoid_func_func_id node, Object data) {
		// this is called for void_func_func_id on the right-hand-side,
		// so it needs to successfully look up the value (or throw
		// an exception); for an assignment, e.g. ^^func() =
		// this visit method is not called for the LHS id
		String void_func_func_id = node.getImage() ;
		Object obj = env.get(void_func_func_id) ;
		if (obj != null) {
			stack.push(obj) ;
		} else {
			throw new UndefinedIdException("Undefined void_func_func_id: " + void_func_func_id) ;
		}
		return data ;
    }
    public Object visit(ASTnum_func_func_assignment node, Object data) {
		String funcFunc_id = ((ASTnum_func_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		// should leave a FuncValue object on the stack
		env.put(funcFunc_id, stack.pop()) ;
		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			addToGUISymtab(funcFunc_id, SymtabIcons.FUNC_IMAGE, data) ;
		}
		return data ;
    }
    public Object visit(ASTnum_func_func_definition node, Object data) {
		// three daughters:  num_func_func_id  param_list  func_block
		String funcFuncName = ((ASTnum_func_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		ArrayList<ParamSlot> pal = (ArrayList<ParamSlot>) stack.pop() ;
		env.put(funcFuncName, new FuncValue(env.getCurrentFrame(),
					                        pal, 
											(ASTfunc_block) node.jjtGetChild(2))) ;
		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			addToGUISymtab(funcFuncName, SymtabIcons.FUNC_IMAGE, data) ;
		}
		return data ;
    }
    public Object visit(ASTnum_func_func_id node, Object data) {
		// this method is called for a num_func_func_id on the RHS;
		// need to look up and push the value from the environment
		String num_func_func_id = node.getImage() ;
		Object obj = env.get(num_func_func_id) ;
		if (obj != null) {
			stack.push(obj) ;
		} else {
			throw new UndefinedIdException("Undefined num_func_func_id: " + 
			num_func_func_id) ; 
		}
		return data ;
    }
    public Object visit(ASTnet_list_assignment node, Object data) {
		
		String net_list_id = ((ASTnet_list_id)node.jjtGetChild(0)).getImage() ;

		node.jjtGetChild(1).jjtAccept(this, data) ;
		// child 1 could be net_list_id, net_list_lit or net_list_func_call
		// Should leave a NetList object on the stack (see Fst.java)
		NetList netList = (NetList)(stack.pop()) ;

		// relate the net_list_id and the NetList in the current Frame
		env.put(net_list_id, netList) ;

		// Now if the current Frame is the main Frame, add
		// an appropriate SymbolIconButton to the window.  In other
		// words, do NOT try to add a SymbolIconButton if the
		// current Frame is a temporary one used to process a 
		// function call, or a stand_alone_block,
		// and do NOT try to add a SymbolIconButton
		// if the interpreter is working on a startup script (i.e.
		// is not operating within the interactive GUI)

		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			// then we're in the GUI

			// add an Icon to the Symtab window
			addToGUISymtab(net_list_id, SymtabIcons.NET_ARR_IMAGE, data) ;
			PseudoTerminalInternalFrame terminal = 
					((InterpData)data).getGUI().getTerminal() ;
			terminal.appendToHistory("// " + basicNetListInfo(netList)) ;
		}

		return data ;
    }
    public Object visit(ASTnum_list_assignment node, Object data) {
		
		String num_list_id = ((ASTnum_list_id)node.jjtGetChild(0)).getImage() ;

		node.jjtGetChild(1).jjtAccept(this, data) ;
		// child 1 could be num_list_id, num_list_lit or num_list_func_call
		// Should leave a NumList object on the stack (see Fst.java)
		NumList numList = (NumList)(stack.pop()) ;

		// relate the num_list_id and the NumList in the current Frame
		env.put(num_list_id, numList) ;

		// Now if the current Frame is the main Frame, add
		// an appropriate SymbolIconButton to the window.  In other
		// words, do NOT try to add a SymbolIconButton if the
		// current Frame is a temporary one used to process a 
		// function call, or a stand_alone_block,
		// and do NOT try to add a SymbolIconButton
		// if the interpreter is working on a startup script (i.e.
		// is not operating within the interactive GUI)

		if (((InterpData)data).getInGUI() == true 
			&& env.getCurrentFrame() == mainFrame) {
			// then we're in the GUI

			// add an Icon to the Symtab window
			addToGUISymtab(num_list_id, SymtabIcons.NUM_ARR_IMAGE, data) ;
			PseudoTerminalInternalFrame terminal = 
					((InterpData)data).getGUI().getTerminal() ;
			terminal.appendToHistory("// Number list value: Size " + numList.size()) ;
		}

		return data ;

    }
    public Object visit(ASTif_statement node, Object data) {
		// daughters are if_part  elsif_part*  else_part?
		int childCount = node.jjtGetNumChildren() ;
		for (int i = 0; i < childCount; i++) {
			node.jjtGetChild(i).jjtAccept(this, data) ;
			// break loop for the first daughter that succeeds (returns true)
			if (lib.isTrue(stack.pop())) {
				break ;
			}
		}
		return data ;
    }
    public Object visit(ASTboolean_test node, Object data) {
		// just an arithmetic expression
		node.childrenAccept(this, data) ;
		// leaves a Long or Double value on the stack
		return data ;
    }
    public Object visit(ASTif_part node, Object data) {
		// two daughters:  boolean_test   block (or other stmt)
		node.jjtGetChild(0).jjtAccept(this, data) ;
		if (lib.isTrue(stack.pop())) {
			node.jjtGetChild(1).jjtAccept(this, data) ;
			// "return" true to block evaluation of elsif or else blocks
			stack.push(new Long(1L)) ;  
		} else {
			// "return" false
			stack.push(new Long(0L)) ;
		}
		return data ;
    }
    public Object visit(ASTelsif_part node, Object data) {
		// two daughters:  boolean_test   block (or other stmt)
		node.jjtGetChild(0).jjtAccept(this, data) ;
		if (lib.isTrue(stack.pop())) {
			node.jjtGetChild(1).jjtAccept(this, data) ;
			stack.push(new Long(1L)) ;
		} else {
			stack.push(new Long(0L)) ;
		}
		return data ;
    }
    public Object visit(ASTelse_part node, Object data) {
		// one daughter:  block (or other stmt)
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// Yes, return "true" here.  ASTif_statement always pops off
		// the value "returned"
		stack.push(new Long(1L)) ;
		return data ;
    }
	public Object visit(ASTforeach_net_iteration_statement node, Object data) {
		// foreach ($foo in $@arr) {    }

		// three daughters
		//		iterator_net_id()
		//		net_list_exp()
		//		loop_block || statement

		String net_id = ((ASTiterator_net_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		NetList netList = (NetList)(stack.pop()) ;

		Object obj = node.jjtGetChild(2) ;  // an AST

		// do not allocate a new frame (cf. Python)

		if (obj instanceof ASTloop_block) {
			ASTloop_block loop_block = (ASTloop_block) obj ;

			for(Iterator<Fst> i = netList.iterator(); i.hasNext(); ) {
				// bind the net_id in the new Frame
				env.put(net_id, i.next()) ;
				// execute the block
				loop_block.jjtAccept(this, data) ;

				if (((InterpData)data).getLoopContinue()) {
					((InterpData)data).setLoopContinue(false) ;
					continue ;
				} else if (((InterpData)data).getLoopBreak()) {
					((InterpData)data).setLoopBreak(false) ;
					break ;
				} else if (((InterpData)data).getFuncReturn()) {
					// a 'return' statement
					// don't reset data here
					break ;
				} else if (((InterpData)data).getQuitSession()) {
					// a 'quit' statement
					// don't reset data here
					break ;
				}
			}
		} else {
			for(Iterator<Fst> i = netList.iterator(); i.hasNext(); ) {
				env.put(net_id, i.next()) ;
				// execute the statement
				node.jjtGetChild(2).jjtAccept(this, data) ;
			}
		}

		return data ;
	}
	public Object visit(ASTforeach_num_iteration_statement node, Object data) {
		// foreach (#foo in #@arr) {    }

		// three daughters
		//		iterator_num_id()
		//		num_list_exp()
		//		loop_block || statement

		String num_id = ((ASTiterator_num_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		NumList numList = (NumList)(stack.pop()) ;

		Object obj = node.jjtGetChild(2) ;  // an AST

		if (obj instanceof ASTloop_block) {

			ASTloop_block loop_block = (ASTloop_block) obj ;

			for(Iterator i = numList.iterator(); i.hasNext(); ) {
				// bind the net_id in the new Frame
				env.put(num_id, i.next()) ;
				// execute the block
				loop_block.jjtAccept(this, data) ;

				if (((InterpData)data).getLoopContinue()) {
					((InterpData)data).setLoopContinue(false) ;
					continue ;
				} else if (((InterpData)data).getLoopBreak()) {
					((InterpData)data).setLoopBreak(false) ;
					break ;
				} else if (((InterpData)data).getFuncReturn()) {
					// a 'return' statement
					// don't reset data here
					break ;
				} else if (((InterpData)data).getQuitSession()) {
					// a 'quit' statement
					// don't reset data here
					break ;
				}
			}
		} else {
			for(Iterator i = numList.iterator(); i.hasNext(); ) {
				env.put(num_id, i.next()) ;
				// execute the statement
				node.jjtGetChild(2).jjtAccept(this, data) ;
			}
		}

		return data ;
	}
    public Object visit(ASTwhile_statement node, Object data) {
		// two daughters:  boolean_test and ( loop_block || statement )
		ASTboolean_test boolean_test = (ASTboolean_test) node.jjtGetChild(0) ;
		Object obj = node.jjtGetChild(1) ;

		if (obj instanceof ASTloop_block) {
			while (true) {
				// re-evaluate the boolean_test (an AST) each time;
				// should leave a Long or a Double on the stack
				boolean_test.jjtAccept(this, data) ;
				if (lib.isTrue(stack.pop())) {
					// then eval the block (an AST)
					node.jjtGetChild(1).jjtAccept(this, data) ;

					if (((InterpData)data).getLoopContinue()) {
						((InterpData)data).setLoopContinue(false) ;
						continue ;
					} else if (((InterpData)data).getLoopBreak()) {
						((InterpData)data).setLoopBreak(false) ;
						break ;
					} else if (((InterpData)data).getFuncReturn()) {
						// a 'return' statement
						// don't reset data here
						break ;
					} else if (((InterpData)data).getQuitSession()) {
						// a 'quit' statement
						// don't reset data here
						break ;
					}
				} else {
					// end of the while loop
					break ;
				}
			}
		} else {
			while (true) {
				boolean_test.jjtAccept(this, data) ;
				if (lib.isTrue(stack.pop())) {
					//statement.jjtAccept(this, data) ;
					node.jjtGetChild(1).jjtAccept(this, data) ;
				}
			}
		}
		return data ;
    }
    public Object visit(ASTuntil_statement node, Object data) {
		// two daughters:  boolean_test and ( loop_block | statement )
		ASTboolean_test boolean_test = (ASTboolean_test) node.jjtGetChild(0) ;

		Object obj = node.jjtGetChild(1) ;

		if (obj instanceof ASTloop_block) {
			while (true) {
				// re-evaluate the boolean_test AST each time;
				// should leave a Long or a Double on the stack
				boolean_test.jjtAccept(this, data) ;
				// N.B. the logical not (!) here, compared to while_statement above
				if (!lib.isTrue(stack.pop())) {
					node.jjtGetChild(1).jjtAccept(this, data) ;

					if (((InterpData)data).getLoopContinue()) {
						((InterpData)data).setLoopContinue(false) ;
						continue ;
					} else if (((InterpData)data).getLoopBreak()) {
						((InterpData)data).setLoopBreak(false) ;
						break ;
					} else if (((InterpData)data).getFuncReturn()) {
						// don't reset data here
						break ;
					} else if (((InterpData)data).getQuitSession()) {
						// don't reset data here
						break ;
					}
				} else {
					break ;
				}
			}
		} else {
			while (true) {
				boolean_test.jjtAccept(this, data) ;
				if (!lib.isTrue(stack.pop())) {
					node.jjtGetChild(1).jjtAccept(this, data) ;
				}
			}
		}
		return data ;
    }
    public Object visit(ASTreturn_statement node, Object data) {
		// in a void function, will have no daughters
		// else it will have exactly one daughter; this is syntactically
		// constrained, either 0 or 1 daughter

		int childCount = node.jjtGetNumChildren() ;
		if (childCount != 0) {
			node.jjtGetChild(0).jjtAccept(this, data) ;
			// should leave an object on the stack
		}

		// if childCount == 0, then it was a bare return ; stmt
		// should be in a void function

		((InterpData)data).setFuncReturn(true) ;
		// this causes the function block to terminate

		return data ;
    }
    public Object visit(ASTnumexp node, Object data) {
		// should be just one child
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves a Long or Double on the stack
		return data ;
    }
    public Object visit(ASTboolean_or_exp node, Object data) {
		// should be two daughters
		// do a short-stop, left-to-right evaluation
		node.jjtGetChild(0).jjtAccept(this, data) ;
		if (lib.isTrue(stack.pop())) {
			// no need to evaluate the second child
			stack.push(new Long(1L)) ;
		} else {
			// evaluate the second daughter
			node.jjtGetChild(1).jjtAccept(this, data) ;
			if (lib.isTrue(stack.pop()))
				stack.push(new Long(1L)) ;
			else
				stack.push(new Long(0L)) ;
		} 
		return data ;
    }
    public Object visit(ASTboolean_and_exp node, Object data) {
		// should be two daughters
		// do a short-stop, left-to-right evaluation
		node.jjtGetChild(0).jjtAccept(this, data) ;
		if (lib.isTrue(stack.pop())) {
			// So far so good.
			// now evaluate the second daughter
			node.jjtGetChild(1).jjtAccept(this, data) ;
			if (lib.isTrue(stack.pop()))
				// then both are true
				stack.push(new Long(1L)) ;  // leave "true" on the stack
			else
				stack.push(new Long(0L)) ;  // leave "false" on the stack
		} else 
			// the first daughter was false, so the whole boolean_and_exp is false
			stack.push(new Long(0L)) ;  // leave "false" on the stack
		return data ;
    }
    public Object visit(ASTboolean_not_exp node, Object data) {
		// should be only one daughter, syntactically constrained
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// the _not_ is handled here
		if (lib.isTrue(stack.pop()))
			stack.push(new Long(0L)) ;  // then return False
		else
			stack.push(new Long(1L)) ;  // else return True
		return data ;
    }
    public Object visit(ASTless_than_exp node, Object data) {
		// two daughters, either Long or Double
		node.childrenAccept(this, data) ;
		Object second = stack.pop() ;
		Object first = stack.pop() ;
		if (first instanceof Long) {
			if (second instanceof Long)
				if (((Long)first).longValue() < ((Long)second).longValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
			else
				if (((Long)first).longValue() < ((Double)second).doubleValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
		} else {
			if (second instanceof Long)
				if (((Double)first).doubleValue() < ((Long)second).longValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
			else
				if (((Double)first).doubleValue() < ((Double)second).doubleValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
		}
		return data ;
    }
    public Object visit(ASTless_than_or_equal_exp node, Object data) {
		// two daughters, either Long or Double
		node.childrenAccept(this, data) ;
		Object second = stack.pop() ;
		Object first = stack.pop() ;
		if (first instanceof Long) {
			if (second instanceof Long)
				if (((Long)first).longValue() <= ((Long)second).longValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
			else
				if (((Long)first).longValue() <= ((Double)second).doubleValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
		} else {
			if (second instanceof Long)
				if (((Double)first).doubleValue() <= ((Long)second).longValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
			else
				if (((Double)first).doubleValue() <= ((Double)second).doubleValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
		}
		return data ;
    }
    public Object visit(ASTgreater_than_exp node, Object data) {
		// two daughters, either Long or Double
		node.childrenAccept(this, data) ;
		Object second = stack.pop() ;
		Object first = stack.pop() ;
		if (first instanceof Long) {
			if (second instanceof Long)
				if (((Long)first).longValue() > ((Long)second).longValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
			else
				if (((Long)first).longValue() > ((Double)second).doubleValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
		} else {
			if (second instanceof Long)
				if (((Double)first).doubleValue() > ((Long)second).longValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
			else
				if (((Double)first).doubleValue() > ((Double)second).doubleValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
		}
		return data ;
    }
    public Object visit(ASTgreater_or_equal_exp node, Object data) {
		// two daughters, either Long or Double
		node.childrenAccept(this, data) ;
		Object second = stack.pop() ;
		Object first = stack.pop() ;
		if (first instanceof Long) {
			if (second instanceof Long)
				if (((Long)first).longValue() >= ((Long)second).longValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
			else
				if (((Long)first).longValue() >= ((Double)second).doubleValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
		} else {
			if (second instanceof Long)
				if (((Double)first).doubleValue() >= ((Long)second).longValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
			else
				if (((Double)first).doubleValue() >= ((Double)second).doubleValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
		}
		return data ;
    }
    public Object visit(ASTequal_exp node, Object data) {
		// two daughters, either Long or Double
		node.childrenAccept(this, data) ;
		Object second = stack.pop() ;
		Object first = stack.pop() ;
		if (first instanceof Long) {
			if (second instanceof Long)
				if (((Long)first).longValue() == ((Long)second).longValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
			else 
				if (((Long)first).longValue() == ((Double)second).doubleValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
		} else {
			if (second instanceof Long)
				if (((Double)first).doubleValue() == ((Long)second).longValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
			else 
				if (((Double)first).doubleValue() == ((Double)second).doubleValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
		}
		return data ;
    }
    public Object visit(ASTnot_equal_exp node, Object data) {
		// two daughters, either Long or Double
		node.childrenAccept(this, data) ;
		Object second = stack.pop() ;
		Object first = stack.pop() ;
		if (first instanceof Long) {
			if (second instanceof Long)
				if (((Long)first).longValue() != ((Long)second).longValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
			else 
				if (((Long)first).longValue() != ((Double)second).doubleValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
		} else {
			if (second instanceof Long)
				if (((Double)first).doubleValue() != ((Long)second).longValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
			else
				if (((Double)first).doubleValue() != ((Double)second).doubleValue())
					stack.push(new Long(1L)) ;
				else
					stack.push(new Long(0L)) ;
		}
		return data ;
    }
    public Object visit(ASTaddition_exp node, Object data) {
		// always two daughters (Long or Double), constrained syntactically
		node.childrenAccept(this, data) ;

		Object second = stack.pop() ;  // N.B. pop off the 2nd arg first
		Object first = stack.pop() ;
		if (first instanceof Long) {
			if (second instanceof Long) {
				// both args are Long
				stack.push(new Long(((Long)first).longValue() +
							((Long)second).longValue())) ;
			} else {
				// one arg Long, one Double, promoted to Double
				stack.push(new Double(((Long)first).longValue() +
							  ((Double)second).doubleValue())) ;
			}
		} else {  // first is Double
			if (second instanceof Double) {
				// both args are Double
				stack.push(new Double(((Double)first).doubleValue() +
							  ((Double)second).doubleValue())) ;
			} else {
				// one arg Double, one Long, promotes to Double
				stack.push(new Double(((Double)first).doubleValue() +
							  ((Long)second).longValue())) ;
			}
		}
		return data ;
    }
    public Object visit(ASTsubtraction_exp node, Object data) {
		// N.B. "subtraction" means arithmetic subtraction in
		// Kleene;
		// See "difference" for subtraction of Fsts
		// always two daughters (Long or Double), syntactically constrained
		node.childrenAccept(this, data) ;

		Object second = stack.pop() ;  // N.B. pop off the 2nd arg first
		Object first = stack.pop() ;
		if (first instanceof Long) {
			if (second instanceof Long) {
				// both args are Long
				stack.push(new Long(((Long)first).longValue() -
							((Long)second).longValue())) ;
			} else {
				// first arg Long, second Double, promotes to Double
				stack.push(new Double(((Long)first).longValue() -
							  ((Double)second).doubleValue())) ;
			}
		} else {  // first is Double
			if (second instanceof Double) {
				// both args are Double
				stack.push(new Double(((Double)first).doubleValue() -
							  ((Double)second).doubleValue())) ;
			} else {
				// first arg Double, second Long, promotes to Double
				stack.push(new Double(((Double)first).doubleValue() -
							  ((Long)second).longValue())) ;
			}
		}
		return data ;
    }
    public Object visit(ASTmult_exp node, Object data) {
		// always two daughters (Long or Double), syntactically constrained
		node.childrenAccept(this, data) ;

		Object second = stack.pop() ;  // N.B. pop off the 2nd arg first
		Object first = stack.pop() ;
		if (first instanceof Long) {
			if (second instanceof Long) {
				// both args are Long
				stack.push(new Long(((Long)first).longValue() *
							((Long)second).longValue())) ;
			} else {
				// first arg Long, second Double, promotes to Double
				stack.push(new Double(((Long)first).longValue() *
							  ((Double)second).doubleValue())) ;
			}
		} else {  // first is Double
			if (second instanceof Double) {
				// both args are Double
				stack.push(new Double(((Double)first).doubleValue() *
							  ((Double)second).doubleValue())) ;
			} else {
				// first arg Double, second Long, promotes to Double
				stack.push(new Double(((Double)first).doubleValue() *
							  ((Long)second).longValue())) ;
			}
		}
		return data ;
    }
    public Object visit(ASTdiv_exp node, Object data) {
		// always two daughters (Long or Double)
		node.childrenAccept(this, data) ;

		Object second = stack.pop() ;  // N.B. pop off the 2nd arg first
		Object first = stack.pop() ;
		if (first instanceof Long) {
			if (second instanceof Long) {
				// both args are Long
				stack.push(new Long(((Long)first).longValue() /
							((Long)second).longValue())) ;
			} else {
				// first arg Long, second Double, promotes to Double
				stack.push(new Double(((Long)first).longValue() /
							  ((Double)second).doubleValue())) ;
			}
		} else {  // first is Double
			if (second instanceof Double) {
				// both args are Double
				stack.push(new Double(((Double)first).doubleValue() /
							  ((Double)second).doubleValue())) ;
			} else {
				// first arg Double, second Long, promotes to Double
				stack.push(new Double(((Double)first).doubleValue() /
							  ((Long)second).longValue())) ;
			}
		}
		return data ;
    }
    public Object visit(ASTmod_exp node, Object data) {
		// always two daughters (Long or Double)
		node.childrenAccept(this, data) ;

		Object second = stack.pop() ;  // N.B. pop off the 2nd arg first
		Object first = stack.pop() ;
		if (first instanceof Long) {
			if (second instanceof Long) {
				// both args are Long
				stack.push(new Long(((Long)first).longValue() %
							((Long)second).longValue())) ;
			} else {
				// first arg Long, second Double, promotes to Double
				stack.push(new Double(((Long)first).longValue() %
							  ((Double)second).doubleValue())) ;
			}
		} else {  // first is Double
			if (second instanceof Double) {
				// both args are Double
				stack.push(new Double(((Double)first).doubleValue() %
							  ((Double)second).doubleValue())) ;
			} else {
				// first arg Double, second Long, promotes to Double
				stack.push(new Double(((Double)first).doubleValue() %
							  ((Long)second).longValue())) ;
			}
		}
		return data ;
    }
    public Object visit(ASTunary_minus_exp node, Object data) {
		node.childrenAccept(this, data) ;
		Object obj = stack.pop() ;
		if (obj instanceof Long) {
			stack.push(new Long(((Long)obj).longValue() * -1)) ;
		} else {
			stack.push(new Double(((Double)obj).doubleValue() * -1.0)) ;
		}
		return data ;
	}
	public Object visit(ASTnum_func_call node, Object data) {
		//  E.g. #^add(1,2) or 
		//       #^(...){...}(1,2)
		//  two daughters:  num_func_exp  arg_list
		//  the num_func_exp could be num_func_id or an anon function,
		//  so need to evaluate it to get the value
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// It should leave a FuncValue object on the stack
		FuncValue funcValue = (FuncValue) stack.pop() ;
		// the second daughter is an argument list
		node.jjtGetChild(1).jjtAccept(this, data) ;
		// the top object left on the stack is a count of the arguments
		ArgCounts ac = (ArgCounts) stack.pop() ;

		// Now allocate a new Frame for the execution of the function call
		// The FuncValue object stores a handle to the "static" Frame
		//   in which it was originally defined.  (Free variables are
		//   looked up starting at this static Frame, implementing
		//   Lexical Scope.)
		env.allocateFrame(funcValue.getStaticFrame()) ;

		// now bind the formal params to the args
		try {
			bind_params(funcValue.getParamArrayList(), ac, data) ;
			// may throw FuncCallException, a kind of RuntimeException
			// catch any Exceptions and release the Frame before rethrowing
		} catch (RuntimeException re) {
			env.releaseFrame() ;
			throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}

		// now execute the body of the function (a func_block)
		try {
			funcValue.getFuncBlock().jjtAccept(this, data) ;
		} catch (RuntimeException re) {
			env.releaseFrame() ;
			throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}

		// normal release of the Frame created for the execution of the func call
		env.releaseFrame() ;

		// Check the return value; there should be a Long or Double left on
		// the stack.
		Object obj = stack.peek() ;
		if (obj == null) {
			throw new FuncCallException("Num valued function call fails to return a Long or Double.") ;
		} else if (!((obj instanceof Long) | (obj instanceof Double))) {
			throw new FuncCallException("Number-valued function call returns incorrect type.") ;
		}

		return data ;
    }
    public Object visit(ASTnum_func_exp node, Object data) {
		// could be a num_func_id or a num_func_anon_exp
 		node.jjtGetChild(0).jjtAccept(this, data) ;
		return data ;
  	}
    public Object visit(ASTnum_func_id node, Object data) {
		// this method is called for a num_func_id on the RHS;
		// need to look up and push the value from the environment
		String num_func_id = node.getImage() ;
		Object obj = env.get(num_func_id) ;
		if (obj != null) {
			stack.push(obj) ;
		} else {
			throw new UndefinedIdException("Undefined num_func_id: " + num_func_id) ; 
		}
		return data ;
	}
	public Object visit(ASTnum_func_anon_exp node, Object data) {
		// two daughters:  param_list  func_block
		node.jjtGetChild(0).jjtAccept(this, data) ;
		ArrayList<ParamSlot> pal = (ArrayList<ParamSlot>) stack.pop() ;
		stack.push(new FuncValue(env.getCurrentFrame(), 
					             pal, 
								 (ASTfunc_block) node.jjtGetChild(1))) ;
		return data ;
    }
    public Object visit(ASTnum_func_func_call node, Object data) {
		//  E.g. #^^increment_by(1) or 
		//       #^^(...){...}(1)
		//  two daughters:  num_func_func_exp  arg_list
		//  the num_func_func_exp could be num_func_func_id or an 
		//  anon function,
		//  so need to evaluate it to get the value
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// should leave a FuncValue on the stack
		FuncValue funcValue = (FuncValue) stack.pop() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		// the top object left on the stack is a count of the arguments
		ArgCounts ac = (ArgCounts) stack.pop() ;

		// Now allocate a new Frame for the execution of the function call
		// The FuncValue object stores a handle to the "static" Frame
		//   in which it was originally defined.  (Free variables are
		//   looked up starting at this static Frame, implementing
		//   Lexical Scope.)
		env.allocateFrame(funcValue.getStaticFrame()) ;

		// now bind the formal params to the args
		try {
			bind_params(funcValue.getParamArrayList(), ac, data) ;
			// may throw FuncCallException, a kind of RuntimeException
			// catch all exceptions and release the frame before rethrowing
		} catch (RuntimeException re) {
			env.releaseFrame() ;
			throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}

		// now execute the body of the function (a func_block)
		try {
			funcValue.getFuncBlock().jjtAccept(this, data) ;
		} catch (RuntimeException re) {
			env.releaseFrame() ;
			throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}

		// normal release of the Frame created for the execution of the func call
		env.releaseFrame() ;

		// Check the return value; there should be a FuncValue left on
		// the stack.
		Object obj = stack.peek() ;
		if (obj == null) {
			throw new FuncCallException("NumFuncFunc valued function call fails to return a value.") ;
		} else if (!(obj instanceof FuncValue)) {
			throw new FuncCallException("NumFuncFunc valued function call returns incorrect type.") ;
		}
		return data ;
    }
    public Object visit(ASTnum_list_func_func_call node, Object data) {
		//  E.g. #@^^name(1) or 
		//       #@^^(...){...}(1)
		//  two daughters:  num_list_func_func_exp  arg_list
		//  the num_list_func_func_exp could be num_list_func_func_id or
		//  an anonymous function,
		//  so need to evaluate it to get the function value
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// should leave a FuncValue on the stack
		FuncValue funcValue = (FuncValue) stack.pop() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		// the top object left on the stack is a count of the arguments
		ArgCounts ac = (ArgCounts) stack.pop() ;

		// Now allocate a new Frame for the execution of the function call
		// The FuncValue object stores a handle to the "static" Frame
		//   in which it was originally defined.  (Free variables are
		//   looked up starting at this static Frame, implementing
		//   Lexical Scope.)
		env.allocateFrame(funcValue.getStaticFrame()) ;

		// now bind the formal params to the args
		try {
			bind_params(funcValue.getParamArrayList(), ac, data) ;
			// may throw FuncCallException, a kind of RuntimeException
			// catch all exceptions and release the frame before rethrowing
		} catch (RuntimeException re) {
			env.releaseFrame() ;
			throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}

		// now execute the body of the function (a func_block)
		try {
			funcValue.getFuncBlock().jjtAccept(this, data) ;
		} catch (RuntimeException re) {
			env.releaseFrame() ;
			throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}

		// normal release of the Frame created for the execution of the func call
		env.releaseFrame() ;

		// Check the return value; there should be a FuncValue left on
		// the stack.
		Object obj = stack.peek() ;
		if (obj == null) {
			throw new FuncCallException("NumListFuncFunc valued function call fails to return a value.") ;
		} else if (!(obj instanceof FuncValue)) {
			throw new FuncCallException("NumListFuncFunc valued function call returns incorrect type.") ;
		}
		return data ;
    }
    public Object visit(ASTnet_list_func_func_call node, Object data) {
		//  E.g. $@^^name(1) or 
		//       $@^^(...){...}(1)
		//  two daughters:  net_list_func_func_exp  arg_list
		//  the net_list_func_func_exp could be net_list_func_func_id or 
		//  an anonymous function,
		//  so need to evaluate it to get the function value
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// should leave a FuncValue on the stack
		FuncValue funcValue = (FuncValue) stack.pop() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		// the top object left on the stack is a count of the arguments
		ArgCounts ac = (ArgCounts) stack.pop() ;

		// Now allocate a new Frame for the execution of the function call
		// The FuncValue object stores a handle to the "static" Frame
		//   in which it was originally defined.  (Free variables are
		//   looked up starting at this static Frame, implementing
		//   Lexical Scope.)
		env.allocateFrame(funcValue.getStaticFrame()) ;

		// now bind the formal params to the args
		try {
			bind_params(funcValue.getParamArrayList(), ac, data) ;
			// may throw FuncCallException, a kind of RuntimeException
			// catch all exceptions and release the frame before rethrowing
		} catch (RuntimeException re) {
			env.releaseFrame() ;
			throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}

		// now execute the body of the function (a func_block)
		try {
			funcValue.getFuncBlock().jjtAccept(this, data) ;
		} catch (RuntimeException re) {
			env.releaseFrame() ;
			throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}

		// normal release of the Frame created for the execution of the func call
		env.releaseFrame() ;

		// Check the return value; there should be a FuncValue left on
		// the stack.
		Object obj = stack.peek() ;
		if (obj == null) {
			throw new FuncCallException("NetListFuncFunc valued function call fails to return a value.") ;
		} else if (!(obj instanceof FuncValue)) {
			throw new FuncCallException("NetListFuncFunc valued function call returns incorrect type.") ;
		}
		return data ;
    }
    public Object visit(ASTnum_func_func_exp node, Object data) {
		node.jjtGetChild(0).jjtAccept(this, data) ;
		return data ;
    }
    public Object visit(ASTnum_list_func_func_exp node, Object data) {
		node.jjtGetChild(0).jjtAccept(this, data) ;
		return data ;
    }
    public Object visit(ASTnet_list_func_func_exp node, Object data) {
		node.jjtGetChild(0).jjtAccept(this, data) ;
		return data ;
    }
    public Object visit(ASTnum_func_func_anon_exp node, Object data) {
		// two daughters:  param_list  func_block
		node.jjtGetChild(0).jjtAccept(this, data) ;
		ArrayList<ParamSlot> pal = (ArrayList<ParamSlot>) stack.pop() ;
		stack.push(new FuncValue(env.getCurrentFrame(), 
					             pal, 
								 (ASTfunc_block) node.jjtGetChild(1))) ;
		return data ;
    }
    public Object visit(ASTdec_int_literal node, Object data) {
		stack.push(new Long(node.getLongValue())) ;
		return data ;
    }
    public Object visit(ASThex_int_literal node, Object data) {
		stack.push(new Long(node.getLongValue())) ;
		return data ;
    }
    public Object visit(ASTdec_float_literal node, Object data) {
		stack.push(new Double(node.getDoubleValue())) ;
		return data ;
    }
	public Object visit(ASTrrProdRHS node, Object data) {
		// always has a single daughter: ASTregexp
		node.jjtGetChild(0).jjtAccept(this, data) ;
		return data ;
	}
    public Object visit(ASTregexp node, Object data) {
		// There should be just one daughter
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// and should leave an Fst node on the stack
		Fst fst = (Fst) stack.peek() ;

		lib.CorrectSigmaOtherInPlace(fst) ;
		return data ;
    }
    public Object visit(ASTcomposed_exp node, Object data) {
		// cf. to method for intersected_exp

		// A _o_ B _o_ C ...  is parsed into a flat AST 
		//		(treated like an n-ary operation)
		// there will always be at least two daughters 
		//		(this is syntactically constrained)
		// Beware memory leaks (because the OpenFst Compose(A,B) is 
		//		non-destructive, creating a new OpenFst (C++) object)

		// start by getting a new sigma* Java Fst (hasOther will be 
		// set to true); all the daughter Fsts will be composed
		// onto it (on the "bottom") one at a time
		Fst resultFst = lib.UniversalLanguageFst() ;

		int daughterCount = node.jjtGetNumChildren() ;

		// Loop through the daughters, composing each one with
		//    ("on the bottom of") the resultFst
		for (int i = 0; i < daughterCount; i++) {
			node.jjtGetChild(i).jjtAccept(this, data) ;
			// leaves an Fst object on the stack

			Fst daughterFst = (Fst)stack.pop() ;

			resultFst = lib.Compose(resultFst, daughterFst) ;
		}
		
		stack.push(resultFst) ;
		return data ;
    }
    public Object visit(ASTrule_lhs node, Object data) {
		// a -> b   i.e.  upper -> lower, or
		// a <- b   being upper <- lower
		// two daughters, both subtypes of regexp
		// For ease of conceptualization, first push the lower side,
		// then the upper side on top of the lower side
		node.jjtGetChild(1).jjtAccept(this, data) ; 
		node.jjtGetChild(0).jjtAccept(this, data) ; 
		// leave the two results on the stack (upper above lower)
		return data ;
    }
    public Object visit(ASTrule_right_arrow_oblig node, Object data) {
		// The most commonly used kind of alternation rule
		// a -> b / left _ right
		// a -> b / l1 _ r1 || l2 _ r2 ...   (one rule with mult. contexts)
		// two daughters: rule_lhs (e.g.  a -> b, i.e. upper -> lower)
		//				  rule_rhs (with one or more ASTcontext daughters)


		int daughterCount = node.jjtGetNumChildren() ;  // 1 or 2

		if (daughterCount == 1) {
			// no overt rule_rhs
			// so both contexts are empty (the empty string language)
			Fst right_context = lib.EmptyStringLanguageFst() ;

			Fst left_context = lib.EmptyStringLanguageFst() ;


			// push the right_context first (doesn't matter here)
			stack.push(hulden.Upper(right_context)) ;
			stack.push(hulden.Upper(left_context)) ;
			// leave on top of the stack a count of the contexts
			stack.push(new Integer(1)) ;
		} else {
			// Evaluate the explicit rule_rhs, which will contain one or more contexts;
			// leave the results on the stack (consisting, from the top, with an
			// Integer with a contextCount, followed by L, R (and more L, R pairs
			// if the rule has multiple contexts)
			
			((InterpData)data).setIsRightArrowRule(true) ; 
			// So that any default contexts can be interpreted properly 
			// as matching on the upper side (the default for right_arrow rules)

			node.jjtGetChild(1).jjtAccept(this, data) ;

			((InterpData)data).setIsRightArrowRule(false) ;
		}

		// will leave on the stack, for one context
		// Integer (value 1)
		// Fst (left context)
		// Fst (right context)

		// will leave on the stack, for two contexts
		// Integer (value 2)
		// Fst (1st left context)
		// Fst (1st right context)
		// Fst (2nd left context)
		// Fst (2nd right context)

		// etc.

		// *********************************************************************

		// Now eval the rule_lhs, get networks for A and B (Upper and Lower),
		// and leave them on the stack, A above B
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// two networks left on the stack, upper language (A) above lower language (B)

		// If compiling a set of parallel rules, then interpretation of
		// the overall rule, from the various bits, will need to be done
		// at a higher level, that of the $^__parallel() function

		// But if we aren't dealing with parallel rules, then we need to
		// push an Integer (value 1) to indicate that there is just one
		// rule

		if (!(((InterpData)data).getIsParallelRule())) {
			stack.push(new Integer(1)) ;
			Fst Rewrite = interpRule(true, true) ;  // works from Objects on the stack
								// rightArrow, oblig

			// see Hulden's rewritecleanup.script

			Rewrite = hulden.CleanupRule(Rewrite) ;

			stack.push(Rewrite) ;
			// If a set of parallel rules is being compiled, then these
			// interpRule() and cleanupRule() functions will be called 
			// at a higher level.
		}

		return data ;
    }
    public Object visit(ASTrule_right_arrow_opt node, Object data) {
		// The most commonly used kind of OPTIONAL alternation rule
		// a ->? b
		// a ->? b / left _ right
		// a ->? b / l1 _ r1 || l2 _ r2 ...   (one rule with mult. contexts)
		// two daughters: rule_lhs (e.g.  a ->? b, i.e. upper ->? lower)
		//				  rule_rhs (with one or more ASTcontext daughters)


		int daughterCount = node.jjtGetNumChildren() ;  // 1 or 2

		if (daughterCount == 1) {
			// no overt rule_rhs
			// so both contexts are empty (the empty string language)
			Fst right_context = lib.EmptyStringLanguageFst() ;

			Fst left_context = lib.EmptyStringLanguageFst() ;

			// push the right_context first (doesn't matter here)
			stack.push(hulden.Upper(right_context)) ;
			stack.push(hulden.Upper(left_context)) ;
			// leave on top of the stack a count of the contexts
			stack.push(new Integer(1)) ;
		} else {
			// Evaluate the explicit rule_rhs, which will contain one or more contexts;
			// leave the results on the stack (consisting, from the top, with an
			// Integer with a contextCount, followed by L, R (and more L, R pairs
			// if the rule has multiple contexts)
			
			((InterpData)data).setIsRightArrowRule(true) ; 
			// So that any default contexts can be interpreted properly 
			// as matching on the upper side (the default for right_arrow rules)

			node.jjtGetChild(1).jjtAccept(this, data) ;

			((InterpData)data).setIsRightArrowRule(false) ;
		}

		// will leave on the stack, for one context
		// Integer (value 1)
		// Fst (left context)
		// Fst (right context)

		// will leave on the stack, for two contexts
		// Integer (value 2)
		// Fst (1st left context)
		// Fst (1st right context)
		// Fst (2nd left context)
		// Fst (2nd right context)

		// etc.

		// *********************************************************************

		// Now eval the rule_lhs, get networks for A and B (Upper and Lower),
		// and leave them on the stack, A above B
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// two networks left on the stack, upper language (A) above lower language (B)

		// If compiling a set of parallel rules, then interpretation of
		// the overall rule, from the various bits, will need to be done
		// at a higher level, that of the $^__parallel() function

		// But if we aren't dealing with parallel rules, then we need to
		// push an Integer (value 1) to indicate that there is just one
		// rule

		if (!(((InterpData)data).getIsParallelRule())) {
			stack.push(new Integer(1)) ;
			Fst Rewrite = interpRule(true, false) ;  // works from Objects on the stack
							// rightArrow, opt (not obligatory)

			// see Hulden's rewritecleanup.script

			Rewrite = hulden.CleanupRule(Rewrite) ; 

			stack.push(Rewrite) ;
			// If a set of parallel rules is being compiled, then these
			// interpRule() and cleanupRule() functions will be called 
			// at a higher level.
		}

		return data ;
    }
    public Object visit(ASTrule_left_arrow_oblig node, Object data) {
		// a <- b / left _ right
		// a <- b / l1 _ r1 || l2 _ r2 ...   (one rule with mult. contexts)
		// two daughters: rule_lhs (e.g.  a <- b, i.e. upper <- lower)
		//				  rule_rhs (with one or more ASTcontext daughters)

		int daughterCount = node.jjtGetNumChildren() ;  // 1 or 2

		if (daughterCount == 1) {
			// no overt rule_rhs
			// so both contexts are empty (the empty string language)
			Fst right_context = lib.EmptyStringLanguageFst() ;

			Fst left_context = lib.EmptyStringLanguageFst() ;

			// push the right_context first (doesn't matter here)
			stack.push(hulden.Lower(right_context)) ;
			stack.push(hulden.Lower(left_context)) ;
			// leave on top of the stack a count of the contexts
			stack.push(new Integer(1)) ;
		} else {
			// Evaluate the explicit rule_rhs, which will contain one or more contexts;
			// leave the results on the stack (consisting, from the top, with an
			// Integer with a contextCount, followed by L, R (and more L, R pairs
			// if the rule has multiple contexts)
			
			((InterpData)data).setIsRightArrowRule(false) ; 
			// So that any default contexts can be interpreted properly 
			// as matching on the lower side (the default for left_arrow rules)

			node.jjtGetChild(1).jjtAccept(this, data) ;
		}

		// will leave on the stack, for one context
		// Integer (value 1)
		// Fst (left context)
		// Fst (right context)

		// will leave on the stack, for two contexts
		// Integer (value 2)
		// Fst (1st left context)
		// Fst (1st right context)
		// Fst (2nd left context)
		// Fst (2nd right context)

		// etc.

		// *********************************************************************

		// Now eval the rule_lhs, get networks for A and B (Upper and Lower),
		// and leave them on the stack, A above B
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// two networks left on the stack, upper language (A) above lower language (B)

		// If compiling a set of parallel rules, then interpretation of
		// the overall rule, from the various bits, will need to be done
		// at a higher level, that of the $^__parallel() function

		// But if we aren't dealing with parallel rules, then we need to
		// push an Integer (value 1) to indicate that there is just one
		// rule

		if (!(((InterpData)data).getIsParallelRule())) {
			stack.push(new Integer(1)) ;
			Fst Rewrite = interpRule(false, true) ;  // works from Objects on the stack
								// leftArrow, oblig
								// (first false means not rightArrow)

			// see Hulden's rewritecleanup.script

			Rewrite = hulden.CleanupRule(Rewrite) ;

			stack.push(Rewrite) ;
			// If a set of parallel rules is being compiled, then these
			// interpRule() and cleanupRule() functions will be called 
			// at a higher level.
		}

		return data ;
    }
    public Object visit(ASTrule_left_arrow_opt node, Object data) {
		// a <-? b / left _ right
		// a <-? b / l1 _ r1 || l2 _ r2 ...   (one rule with mult. contexts)
		// two daughters: rule_lhs (e.g.  a <- b, i.e. upper <- lower)
		//				  rule_rhs (with one or more ASTcontext daughters)

		int daughterCount = node.jjtGetNumChildren() ;  // 1 or 2

		if (daughterCount == 1) {
			// no overt rule_rhs
			// so both contexts are empty (the empty string language)
			Fst right_context = lib.EmptyStringLanguageFst() ;

			Fst left_context = lib.EmptyStringLanguageFst() ;

			// push the right_context first (doesn't matter here)
			stack.push(hulden.Lower(right_context)) ;
			stack.push(hulden.Lower(left_context)) ;
			// leave on top of the stack a count of the contexts
			stack.push(new Integer(1)) ;
		} else {
			// Evaluate the explicit rule_rhs, which will contain one or more contexts;
			// leave the results on the stack (consisting, from the top, with an
			// Integer with a contextCount, followed by L, R (and more L, R pairs
			// if the rule has multiple contexts)
			
			((InterpData)data).setIsRightArrowRule(false) ; 
			// So that any default contexts can be interpreted properly 
			// as matching on the lower side (the default for left_arrow rules)

			node.jjtGetChild(1).jjtAccept(this, data) ;
		}

		// will leave on the stack, for one context
		// Integer (value 1)
		// Fst (left context)
		// Fst (right context)

		// will leave on the stack, for two contexts
		// Integer (value 2)
		// Fst (1st left context)
		// Fst (1st right context)
		// Fst (2nd left context)
		// Fst (2nd right context)

		// etc.

		// *********************************************************************

		// Now eval the rule_lhs, get networks for A and B (Upper and Lower),
		// and leave them on the stack, A above B
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// two networks left on the stack, upper language (A) above lower language (B)

		// If compiling a set of parallel rules, then interpretation of
		// the overall rule, from the various bits, will need to be done
		// at a higher level, that of the $^__parallel() function

		// But if we aren't dealing with parallel rules, then we need to
		// push an Integer (value 1) to indicate that there is just one
		// rule

		if (!(((InterpData)data).getIsParallelRule())) {
			stack.push(new Integer(1)) ;
			Fst Rewrite = interpRule(false, false) ;  // works from Objects on the stack
								// leftArrow, opt (not oblig)
								// (first false means not rightArrow)

			// see Hulden's rewritecleanup.script

			Rewrite = hulden.CleanupRule(Rewrite) ;

			stack.push(Rewrite) ;
			// If a set of parallel rules is being compiled, then these
			// interpRule() and cleanupRule() functions will be called 
			// at a higher level.
		}

		return data ;
    }

    public Object visit(ASTrestriction_exp node, Object data) {
		// 2 daughters:  lhs: some kind of regexp node
		//					e.g. lit_char or concatenated_exp
		//               restriction_rhs
		// e.g.  abc => left _ right
		// e.g.  abc => foo _ bar || fum _ fang 

		// Evaluate the 0th child, the LHS of the restriction
		// expression, e.g. the b in   b => l _ r
		// For now, just get the straightforward Fst
		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst lhs = (Fst)stack.pop() ;

		// Now evaluate the RHS of the restriction expression
		// (of type ASTrestriction_rhs) which can contain multiple
		// contexts
		node.jjtGetChild(1).jjtAccept(this, data) ;

		// Hulden:
		// for one context L _ R, where x is a special restriction delimiter
		// 		[\x* [L x \x* x R] \x* ]
		// for two contexts,  L1 _ R1 || L2 _ R2
		//		[\x* [ [L1 x \x* x R1] | [L2 x \x* x R2] ] \x*]
		// etc.

		Fst rhs = (Fst)stack.pop() ;  
		Fst resultFst = interpRestrictionExp(lhs, rhs, true) ;
					// true means that it may have # in the contexts
		stack.push(resultFst) ;

		return data ;
    }

    public Object visit(ASTlongest_exp node, Object data) {
		System.out.println("Interp: ASTlongest_exp node not implemented.") ;
		return data ;
    }
    public Object visit(ASTshortest_exp node, Object data) {
		System.out.println("Interp: ASTshortest_exp node not implemented.") ;
		return data ;
    }

	// for rules that compile into transducers
    public Object visit(ASTrule_rhs node, Object data) {
		// a rule_rhs will contain one or more 'context' daughters

		int contextCount = node.jjtGetNumChildren() ;

		// for ease of concepturalization/testing, push the daughters
		// in reverse order; leave the results on the stack

		for (int i = contextCount - 1; i >= 0; i--) {
			node.jjtGetChild(i).jjtAccept(this, data) ;
		}
		// Finally, leave a context count on top of the stack
		stack.push(new Integer(contextCount)) ;  
		return data ;
    }

	// for => restrictions that compile into acceptors
	public Object visit(ASTrestriction_rhs node, Object data) {
		// Syntax:  RHS of   a => left _ right
		//                   a => left1 _ right1 || left2 _ right2 ...
		// will contain one or more ASTrestriction_context node daughters

		// Hulden
		// each individual context L _ R will be interpreted first as [L x \x* x R]
		// where x is a special restriction-delimiter symbol (here **RD).
		// These get unioned together, and then the result is surrounded
		// with \x*, i.e. the whole rhs is
		// [ \x*  unionOfContexts \x* ]

		// Interpret all the daughters, each one interprets as an
		// Fst that is unioned into 

		Fst unionOfContexts = lib.EmptyLanguageFst() ;

		int daughterCount = node.jjtGetNumChildren() ;

		// loop through the daughers, one for each context;
		// each daughter is of type ASTrestrictionContext
		for (int i = 0; i < daughterCount; i++) {
			node.jjtGetChild(i).jjtAccept(this, data) ;

			Fst daughterFst = (Fst)stack.pop() ;

			unionOfContexts = lib.UnionIntoFirstInPlace(unionOfContexts,
													daughterFst) ;
		}

		//  Hulden:  [ \x* unionOfContext \x* ] for whole RHS
		Fst resultFst = lib.Concat3Fsts(
									hulden.NotRestDelimStarFst(),
									unionOfContexts,
									hulden.NotRestDelimStarFst()
								) ;
		stack.push(resultFst) ;
		return data ;
    }

	// for rules that compile into transducers
    public Object visit(ASTcontext node, Object data) {
		// daughters:

		// leftContext _ rightContext
		// leftContext _
		//             _ rightContext
		//      	   _

		int daughterCount = node.jjtGetNumChildren() ; // 2, 1 or 0

		if (daughterCount == 2) {
			// then we have a non-empty left_context daughter 0, 
			//	and a non-empty right_context daughter 1

			// Push the rightContext first
			node.jjtGetChild(1).jjtAccept(this, data) ;
			// Then push the leftContext on top of it
			node.jjtGetChild(0).jjtAccept(this, data) ;
		} else if (daughterCount == 1) {
			// Then we have a leftContext xor a rightContext;
			// an empty context is the empty string language

			node.jjtGetChild(0).jjtAccept(this, data) ;
			Fst oneContext = (Fst)stack.pop() ;

			Fst emptyStringFst = lib.EmptyStringLanguageFst() ;

			if (node.jjtGetChild(0) instanceof ASTleft_context) {
				// Then the right context is empty, push it first.
				// Because we created the context here, we need to
				// worry about which side it matches on.
				if (((InterpData)data).getIsRightArrowRule()) {
					stack.push(hulden.Upper(emptyStringFst)) ;
				} else {
					stack.push(hulden.Lower(emptyStringFst)) ;
				}
				// Then push the left_context on top of it
				stack.push(oneContext) ;
			} else {
				// the oneContext must be the right_context, push it first
				stack.push(oneContext) ;
				// Then push the left_context, an empty string language,
				// on top of it.  Worry here about upper vs. lower.
				if (((InterpData)data).getIsRightArrowRule()) {
					stack.push(hulden.Upper(emptyStringFst)) ;
				} else {
					stack.push(hulden.Lower(emptyStringFst)) ;
				}
			}
		} else {
			// both contexts are empty (the empty string language)
			Fst right_context = lib.EmptyStringLanguageFst() ;

			Fst left_context = lib.EmptyStringLanguageFst() ;

			if (((InterpData)data).getIsRightArrowRule()) {
				// Then both match on the upper side
				stack.push(hulden.Upper(right_context)) ;
				stack.push(hulden.Upper(left_context)) ;
			} else {
				// Both match on the lower side
				stack.push(hulden.Lower(right_context)) ;
				stack.push(hulden.Lower(left_context)) ;
			}
		}
		return data ;
    }

	// for => restrictions that compile into acceptors
	public Object visit(ASTrestriction_context node, Object data) {
		// Can have
		//		1.  left_restriction_context and right_restriction_context
		//		2.  just a left_restriction_context
		//		3.  just a right_restriction_context
		//		4.  no daughters at all

		int daughterCount = node.jjtGetNumChildren() ;

		// get the values for the two contexts (just straightforward
		// FSTs for now)
		Fst leftContext  ;
		Fst rightContext ; 

		if (daughterCount == 2) {
			// there is both a leftContext and a rightContext
			node.jjtGetChild(0).jjtAccept(this, data) ;
			leftContext = (Fst)stack.pop() ;

			node.jjtGetChild(1).jjtAccept(this, data) ;
			rightContext = (Fst)stack.pop() ;
		} else if (daughterCount == 1) {
			// just a rightContext, or a leftContext
			// (the missing context is just the empty string language
			node.jjtGetChild(0).jjtAccept(this, data) ;
			if (node.jjtGetChild(0) instanceof ASTleft_restriction_context) {
				leftContext = (Fst)stack.pop() ;

				rightContext = lib.EmptyStringLanguageFst() ;
			} else {
				leftContext = lib.EmptyStringLanguageFst() ;

				rightContext = (Fst)stack.pop() ;
			}
		} else {
			// no left or right context
			leftContext = lib.EmptyStringLanguageFst() ;
			rightContext = lib.EmptyStringLanguageFst() ;
		}

		Fst resultFst = interpRestrictionContext(leftContext, rightContext) ;

		stack.push(resultFst) ;
		return data ;
	}
	// for rules that compile into transducers
    public Object visit(ASTleft_context node, Object data) {
		// just one daughter, either default_context or upper_context or lower_context
		node.jjtGetChild(0).jjtAccept(this, data) ;
		return data ;
    }
	// for => restrictions that compile into acceptors
	public Object visit(ASTleft_restriction_context node, Object data) {
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// just leave the resulting Fst on the stack
		return data ;
	}
	// for rules that compile into transducers
    public Object visit(ASTright_context node, Object data) {
		// just one daughter, either default_context, or upper_context or lower_context
		node.jjtGetChild(0).jjtAccept(this, data) ;
		return data ;
    }
	// for => restrictions that compile into acceptors
	public Object visit(ASTright_restriction_context node, Object data) {
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// just leave the resulting Fst on the stack
		return data ;
	}
    public Object visit(ASTdefault_context node, Object data) {
		// Alternation rule context, not marked overtly as upper or lower;
		// so assign the default:  Upper for a right-arrow rule
		//						   Lower for a left-arrow rule


		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst defaultContext = (Fst) stack.pop() ;
		if (((InterpData)data).getIsRightArrowRule()) {
			stack.push(hulden.Upper(defaultContext)) ;
		} else {
			stack.push(hulden.Lower(defaultContext)) ;
		}
		return data ;
    }
    public Object visit(ASTupper_context node, Object data) {
		// a rule context, overtly marked as upper side

		node.jjtGetChild(0).jjtAccept(this, data) ;
		stack.push(hulden.Upper((Fst) stack.pop())) ;
		return data ;
    }
	public Object visit(ASTlower_context node, Object data) {
		// a rule context, overtly marked as lower side

		node.jjtGetChild(0).jjtAccept(this, data) ;
		stack.push(hulden.Lower((Fst) stack.pop())) ;
		return data ;
    }
    public Object visit(ASTwhere_matched_clause node, Object data) {
		System.out.println("Interp: ASTwhere_matched_clause node not implemented.") ;
		return data ;
    }
    public Object visit(ASTelmt_of_net_list_exp node, Object data) {
		System.out.println("Interp: ASTelmt_of_net_list_exp node not implemented.") ;
		return data ;
    }
    public Object visit(ASTdifference_exp node, Object data) {
		// N.B. "difference" here means Fst difference;
		// "subtraction" is the term used herein for the arithmetic operation
		// there should be exactly two daughters, syntactically constrained

		node.childrenAccept(this, data) ;
		// the second arg will be on top of the stack
		Fst secondFst = (Fst)stack.pop() ;
		Fst firstFst = (Fst)stack.pop() ;

		Fst resultFst = lib.Difference(firstFst, secondFst) ;

		stack.push(resultFst) ;
		return data ;
    }
	public Object visit(ASTunioned_exp node, Object data) {
		// The args to be unioned are parsed into a flat AST 
		//    (union is treated like an n-ary operation)

		int last = node.jjtGetNumChildren() - 1 ;

		// start with a new empty FST, union all the argument Fsts into it
		// use EmptyLanguageWithStartStateFst instead of EmptyLanguageFst,
		// this one creates a one-state Fst (start state, not final),
		// which gives expected results of something like a|b when
		// optimization is turned off
		Fst resultFst = lib.EmptyLanguageWithStartStateFst() ;
		
		// some unions, especially in lexicon-like right-recursive 
		// phrase-structure grammars, can get very long, 
		// so optimize only at intervals; KRB: magic number
		int optimizeInterval = 1000 ;

		boolean optimize ;

		for (int i = 0; i <= last; i++) {
			node.jjtGetChild(i).jjtAccept(this, data) ;
			Fst daughterFst = (Fst)stack.pop() ;

			if ((i == last) || ((i % optimizeInterval) == 0)) {
				optimize = true ;
			} else {
				optimize = false ;
			}
			resultFst = lib.UnionIntoFirstInPlace(resultFst, daughterFst, optimize) ;
		}
		stack.push(resultFst) ;
		return data ;
    }
    public Object visit(ASTintersected_exp node, Object data) {
		// KRB:  compare to method for ASTcomposed_exp
		// A & B & C ...  is parsed into a flat AST 
		//		(treated like an n-ary operation)
		// there will always be at least two daughters 
		//		(this is syntactically constrained)

		// get a new sigma* Java Fst (hasOther will be set to true)
		Fst resultFst = lib.UniversalLanguageFst() ; // see Java func above

		int daughterCount = node.jjtGetNumChildren() ;
		// Loop through the daughters, intersecting each one into the result
		for (int i = 0; i < daughterCount; i++) {
			node.jjtGetChild(i).jjtAccept(this, data) ;
			Fst daughterFst = (Fst)stack.pop() ;

			resultFst = lib.Intersect(resultFst, daughterFst) ;
		}

		stack.push(resultFst) ;
		return data ;
    }
    public Object visit(ASTconcatenation_exp node, Object data) {
		// Concatenation is parsed/evaluated as an n-ary operation 
		int last = node.jjtGetNumChildren() - 1 ;

		// start with an empty-string-language Fst
		// concatenate the daughter Fsts into it
		Fst resultFst = lib.EmptyStringLanguageFst() ;
		boolean optimize ;

		for (int i = 0; i <= last; i++) {
			node.jjtGetChild(i).jjtAccept(this, data) ;
			Fst daughterFst = (Fst)stack.pop() ;

			optimize = (i == last) ? true : false ;
			resultFst = lib.Concat(resultFst, daughterFst, optimize) ;
		}		
		stack.push(resultFst) ;
		return data ;
    }
    public Object visit(ASTcrossproduct_exp node, Object data) {
		// there should be exactly two daughters, syntactically
		// constrained 

		node.childrenAccept(this, data) ;
		Fst secondFst = (Fst)stack.pop() ;
		Fst firstFst = (Fst)stack.pop() ;

		Fst resultFst = lib.Crossproduct(firstFst, secondFst) ;

		stack.push(resultFst) ;
		return data ;
    }

    public Object visit(ASTweight_exp node, Object data) {
		// KRB:  just float (Double) values for now (Tropical semiring)
		// user might use just an int (Long), so convert as necessary
		node.jjtGetChild(0).jjtAccept(this, data) ;

		// semiring generalization point

		float weight ;
		Object obj = stack.pop() ;
		if (obj instanceof Double) {
			weight = ((Double)obj).floatValue() ;
		} else {
			weight = ((Long)obj).floatValue() ;
		}

		// Create a two-state, one arc Fst with eps:eps label and 
		//    indicated arc weight,
		// and Tropical weight neutral 0.0 final weight

		stack.push(lib.OneArcFst(lib.Epsilon, lib.Epsilon, weight, (float) 0.0)) ;

		return data ;
    }
    public Object visit(ASTcomplement_exp node, Object data) {
		// syntax is   ~A  where A is any regular expression
		// calculated as (.* - A)
		
		// there should be just one daughter, an ASTregexp
		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst positiveFst = (Fst)stack.pop() ;

		Fst resultFst = lib.Complement(positiveFst) ;
		
		stack.push(resultFst) ;
		return data ;
    }
    public Object visit(ASTkleene_star node, Object data) {
		// Syntax:  x*
		// should be just one daughter
		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst fst = (Fst)stack.pop() ;

		Fst resultFst = lib.KleeneStar(fst) ;

		stack.push(resultFst) ;
		return data ;
    }
    public Object visit(ASTkleene_plus node, Object data) {
		// Syntax:   x+
		// should be just one daughter
		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst fst = (Fst)stack.pop() ;

		Fst resultFst = lib.KleenePlus(fst) ;

		stack.push(resultFst) ;
		return data ;
    }
    public Object visit(ASToptional node, Object data) {
		// just one argument, syntactically constrained
		node.jjtGetChild(0).jjtAccept(this, data) ; // leaves an Fst on stack
		Fst fst = (Fst) stack.pop() ;

		Fst resultFst = lib.UnionIntoFirstInPlace(lib.EmptyStringLanguageFst(),
											fst) ;
		stack.push(resultFst) ;
		return data ;
    }
    public Object visit(ASTiterated_exp node, Object data) {
		// two daughters:  zeroth is a regexp
		//                 first is either  ASTiteration_exact     syntax was {4}
		//                          or      ASTiteration_low       syntax was {2,}
		//                          or		ASTiteration_low_high  syntax was {2,4}
		//                          or      ASTiteration_high      syntax was {,4} 
		//                                                            equiv. to {0,4}

		node.jjtGetChild(0).jjtAccept(this, data) ; // leaves an Fst on the stack
		Fst fst = (Fst) stack.pop() ;

		Fst resultFst ;

		node.jjtGetChild(1).jjtAccept(this, data) ; 
		// leaves one or two Longs on the stack

		// check the type of Child(1) to see if one or two Longs 
		// remain on the stack, and how to treat them.
		Object obj = node.jjtGetChild(1) ;
		if (obj instanceof ASTiteration_low_high) {
			// there will be two Long values on the stack.
			// the high value will be on top of the stack
			long high = ((Long)stack.pop()).longValue() ;
			long low = ((Long)stack.pop()).longValue() ;
			resultFst = lib.Iterate(fst, low, high) ;
		} else if (obj instanceof ASTiteration_low) {
			// just one Long on the stack
			long low = ((Long)stack.pop()).longValue() ;
			// use -1 "high" arg to indicate unlimited
			resultFst = lib.Iterate(fst, low, -1L) ;
		} else if (obj instanceof ASTiteration_high) {
			// just one Long on the stack
			long high = ((Long)stack.pop()).longValue() ;
			// the low value is 0
			resultFst = lib.Iterate(fst, 0L, high) ;
		} else {
			// it's ASTiteration_exact, just one Long on the stack
			long exact = ((Long)stack.pop()).longValue() ;
			resultFst = lib.Iterate(fst, exact, exact) ;
		}

		stack.push(resultFst) ;
		return data ;
    }
    public Object visit(ASTiteration_low_high node, Object data) {
		node.childrenAccept(this, data) ;
		// should usually leave two Long objects on the stack (but might be Double)
		// syntactically, you could have complicated arithmetic expressions.
		// Leave just Long objects on the stack (in the right order).
		//
		// the object left on top of the stack holds the high value
		Object high = stack.pop() ;
		Object low = stack.pop() ;

		if (low instanceof Long) {
			stack.push(low) ;
		} else {
			stack.push(new Long(((Double)low).longValue())) ;
		}

		if (high instanceof Long) {
			stack.push(high) ;
		} else {
			stack.push(new Long(((Double)high).longValue())) ;
		}

		// leaves the high Long on top of the stack, 
		// with the low Long just under it
		return data ;
    }
    public Object visit(ASTiteration_low node, Object data) {
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// object on the Stack usually a Long, but could be a Double
		Object low = stack.pop() ;
		if (low instanceof Long) {
			stack.push(low) ;
		} else {
			stack.push(new Long(((Double)low).longValue())) ;
		}
		return data ;
    }
    public Object visit(ASTiteration_exact node, Object data) {
		node.jjtGetChild(0).jjtAccept(this, data) ;
		Object exact = stack.pop() ;
		if (exact instanceof Long) {
			stack.push(exact) ;
		} else {
			stack.push(new Long(((Double)exact).longValue())) ;
		}
		return data ;
    }
    public Object visit(ASTiteration_high node, Object data) {
		node.jjtGetChild(0).jjtAccept(this, data) ;
		Object high = stack.pop() ;
		if (high instanceof Long) {
			stack.push(high) ;
		} else {
			stack.push(new Long(((Double)high).longValue())) ;
		}
		return data ;
    }
    public Object visit(ASTlit_char node, Object data) {
		String image = node.getImage() ;

		// KRB:  possible normalization point
		// see SymMap.java
		int cpv = symmap.putsym(image) ;

		// create a two-state, one arc Fst with cpv:cpv label
		// semiring generalization point
	
		stack.push(lib.OneArcFst(cpv)) ;
		return data ;
    }
    public Object visit(ASTmultichar_symbol node, Object data) {
		String image = node.getImage() ;

		if (image.startsWith("__")) {
			throw new KleeneInterpreterException("Multicharacter symbols starting with __ (two underscores) are reserved for internal system use.") ;
		} else if (image.startsWith("**")) {
			throw new KleeneInterpreterException("Multicharacter symbols starting with ** (two asterisks) are reserved for internal system use.") ;
		} else if (image.equals("OTHER_ID")) {
			throw new KleeneInterpreterException("The multicharacter symbol OTHER_ID is reserved for internal system use.") ;
		} else if (image.equals("OTHER_NONID")) {
			throw new KleeneInterpreterException("The multicharacter symbol OTHER_NONID is reserved for internal system use.") ;
		} else if (image.equals("KLEENE**@#@")) {
			// tokenizer finds unliteralized #, in a rule/restriction
			// context, and resets the image to "KLEENE**@#@"
			// change it here to **@#@
			image = "**@#@" ;
		} 

		int cpv = symmap.putsym(image) ;

		// create a two-state, one arc Fst with cpv:cpv label
		// semiring generalization point
		stack.push(lib.OneArcFst(cpv)) ;
		return data ;
    }
    public Object visit(ASTsquare_bracket_multichar_symbol node, Object data) {
		String image = node.getImage() ;

		if (image.startsWith("__")) {
			throw new KleeneInterpreterException("Multicharacter symbols starting with __ (two underscores) are reserved for internal system use.") ;
		}
		if (image.startsWith("**")) {
			throw new KleeneInterpreterException("Multicharacter symbols starting with ** (two asterisks) are reserved for internal system use.") ;
		}

		int cpv = symmap.putsym(image) ;

		// create a two-state, one arc Fst with cpv:cpv label
		stack.push(lib.OneArcFst(cpv)) ;
		return data ;
    }

    public Object visit(ASTdouble_quoted_string node, Object data) {
		// There can be any number of characters (including zero) in the string
		int last = node.jjtGetNumChildren() - 1 ;
		// The daughters are all of type ASTdouble_quoted_char

		Fst resultFst = lib.EmptyStringLanguageFst() ;

		boolean optimize ;	// optimize only on the last loop
		
		for (int i = 0; i <= last; i++) {
			node.jjtGetChild(i).jjtAccept(this, data) ;
			// OpenFst's Concat(first, second) is destructive of the first arg
			Fst daughterFst = (Fst)stack.pop() ;

			optimize = (i == last) ? true : false ;

			resultFst = lib.Concat(resultFst, daughterFst, optimize) ;
		}

		stack.push(resultFst) ;
		return data ;
    }
    public Object visit(ASTdouble_quoted_char node, Object data) {
		// A double_quoted_char is a character inside double quotes
		String image = node.getImage() ;

		// KRB:  possible Unicode normalization point
		// KRB:  ignore, for initial testing, the possibility of
		// double_quoted_char matching a letter plus following combining diacritics;
		// take care of this when dealing with robust normalization;  see
		// SymMap.java; cf to ASTlit_char
		int cpv = symmap.putsym(image) ;

		// get a two-state, one arc Fst with cpv:cpv label
		// no need to optimize, I think
		stack.push(lib.OneArcFst(cpv)) ;

		return data ;
    }
    public Object visit(ASTchar_union node, Object data) {
		// Syntax is [aeiou], [A-Za-z0-9], [abcm-z], etc.
		int last = node.jjtGetNumChildren() - 1 ;

		// use EmptyLanguageWithStartStateFst instead of EmptyLanguageFst,
		// the former creates a one-state network (start, not final), and
		// gives more intuitive results when setOptimize is set to false
		Fst resultFst = lib.EmptyLanguageWithStartStateFst() ;

		// there are four possible types of daughter: 
		//      char_range, 
		//		square_bracket_char
		//		square_bracket_lit_hyphen
		//		square_bracket_multichar_symbol
		// (all result in an Fst pushed on the stack)
		boolean optimize ;
		for (int i = 0; i <= last; i++) {
			node.jjtGetChild(i).jjtAccept(this, data) ;
			// OpenFst's Union(first, second) is destructive 
			//		of the first arg
			Fst daughterFst = (Fst)stack.pop() ;

			optimize = (i == last) ? true : false ;

			resultFst = lib.UnionIntoFirstInPlace(resultFst, daughterFst, optimize) ;
		}
		
		stack.push(resultFst) ;
		return data ;
    }
    public Object visit(ASTcomplement_char_union node, Object data) {
		// Syntax:  [^abc]  [^a-z] etc.
		int last = node.jjtGetNumChildren() - 1 ;

		// Compute as . - [...]

		// First, compute the Fst covering the characters in [^...]
		// Cf ASTchar_union

		// start with an empty FST, union into it
		// use EmptyLanguageWithStartStateFst instead of EmptyLanguageFst,
		// the former creates a one-state (start, not final) fst and gives
		// more intuitive results when setOptimize is set to false
		Fst charUnionFst = lib.EmptyLanguageWithStartStateFst() ;

		boolean optimize ;

		for (int i = 0; i <= last; i++) {
			node.jjtGetChild(i).jjtAccept(this, data) ;
			// will leave an Fst object on the stack

			Fst daughterFst = (Fst)stack.pop() ;

			optimize = (i == last) ? true : false ;

			charUnionFst = lib.UnionIntoFirstInPlace(charUnionFst, daughterFst,
			optimize) ;
		}
		
		// Second, get a network for ., subtract charUnionFst

		Fst resultFst = lib.Difference(lib.SigmaFst(), charUnionFst) ;

		stack.push(resultFst) ;
		return data ;
    }
    public Object visit(ASTchar_range node, Object data) {
		// Syntax:   a-z  inside [...] or [^...]
		//    just a shorthand for a symbol union
		// always two daughters, always ASTlit_char?

		String imageFirst = ((ASTlit_char)node.jjtGetChild(0)).getImage() ;
		String imageLast  = ((ASTlit_char)node.jjtGetChild(1)).getImage() ;
		// KRB:  ignore, for initial testing, the possibility of
		// lit_char matching a letter plus following combining diacritics;
		// take care of this when dealing with robust normalization;  see
		// SymMap.java

		int cpvFirst = symmap.putsym(imageFirst) ;
		int cpvLast  = symmap.putsym(imageLast) ;

		if (cpvFirst > cpvLast) {
			// then the range is impossible/badly formed
			throw new CharRangeException("In a character range [x-y], the code point value of y must be greater or equal to the code point value of x.") ;
		}

		Fst resultFst = lib.CharRangeUnionFst(cpvFirst, cpvLast) ;


		// need to put the whole range of chars in the symmap
		// for proper 'dot' display of labels in networks; and
		// add them to the sigma
		for (int cpv = cpvFirst; cpv <= cpvLast; cpv++) {
			symmap.putsym(lib.stringFromCpv(cpv)) ;
			resultFst.getSigma().add(cpv) ;
			// no need to worry about OTHER
		}

		stack.push(resultFst) ;
		return data ;
    }
    public Object visit(ASTnet_func_call node, Object data) {
		// Usual Syntax:  $^myunion(a, b)    has an Fst value
		//  e.g. $^myunion($a, $b)   $^myfunc($a, $b, $c=abc, $d=def) or
		//  $^myfunc($a = a*b+, $b = [a-m]+) 
		//
		//  net_func_call always has two daughters:
		//
		//  net_func_call
		//      net_func_exp  
		//      arg_list
		//
		//  N.B. net_func_exp could be 
		//       net_func_id     e.g. $^myfunc  OR
		//       $^ exp    e.g. $^(...){...}   OR
		//       net_func_func_call e.g.  $^^func(...)
		//  so net_func_exp needs to be evaluated 
		//  (will leave a FuncValue object on the stack)

		// Evaluate daughter 0, the net_func_exp
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// Should leave a FuncValue on the stack
		FuncValue funcValue = (FuncValue) stack.pop() ;

		// now evaluate the arg_list, which can have 0 to 2 daughters
		// If either daughter is present, it is non-empty.
		// If both daughters are present, 
		//		positional_args is always before named_args

		// arg_list
		//     positional_args
		//     named_args
		//
		// N.B. the arguments have to be evaluated in the current Frame,
		// before allocating a new daughter frame for the execution of the
		// function body.  If either daughter is present, it is non-empty.

		// Evaluate daughter 1, the arg_list
		node.jjtGetChild(1).jjtAccept(this, data) ;
		// should leave on the stack, from top down
		// 1.  an ArgCounts object (containing positional_args_count 
		//			and named_args_count)
		// 2.  the positional arguments (in syntactic order), 
		//			the number being positional_args_count
		// 3.  the named arguments (each represented by a NamedArg object), 
		//			the number being named_args_count
		//
		// first pop off the ArgCounts object, 
		//    leaving the evaluated args (if any) on the stack
		ArgCounts ac = (ArgCounts) stack.pop() ;

		//  Now allocate a new Frame for execution of this function call
		//  (N.B. released below when the function returns)
		env.allocateFrame(funcValue.getStaticFrame()) ;

		// now bind the formal params to the arg values, in the new Frame
		try {
	    	bind_params(funcValue.getParamArrayList(), ac, data) ;
			// may throw FuncCallException, a kind of RuntimeException
			// catch all Exceptions and release the frame before rethrowing
		} catch (RuntimeException re) {
	    	env.releaseFrame() ;
	    	throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}
		// now execute the body of the function (a func_block); more precisely,
		// send a message to the function block telling it to accept this 
		// IntepreterVisitor

		try {
			funcValue.getFuncBlock().jjtAccept(this, data) ;
		} catch (RuntimeException re) {
			env.releaseFrame() ;
			throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}

		// normal release of the Frame created for the execution of this func call
		env.releaseFrame() ;

		// Check return value; 
		//	there should be an Fst object left on the stack
		// (this is ASTnet_func_call) by a return stmt in the funcBlock.  
		// Make sure that the object left on the stack 
		//		is a Java Fst object.
		Object obj = stack.peek() ;
		if (obj == null) {
			throw new FuncCallException("Net valued function call failed to return a net.") ;
		} else if (!(obj instanceof Fst)) {
			throw new FuncCallException("Net valued function call returns incorrect type.") ;
		}
		return data ;
    }
    public Object visit(ASTvoid_func_call node, Object data) {
		//  e.g. ^myfoo($a, $b)   
		//       ^myfunc($a, $b, $c=abc, $d=def) or
		//  ^myfunc($a = a*b+, $b = [a-m]+) 
		//
		//  void_func_call
		//      void_func_exp  
		//      arg_list
		//
		//  N.B. void_func_exp could be 
		//       void_func_id     or
		//       ^ exp
		//       void_func_func_call
		//  so it needs to be evaluated
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// Should leave a FuncValue on the stack
		FuncValue funcValue = (FuncValue) stack.pop() ;

		// now evaluate the arg_list, which can have up to two daughters
		// arg_list
		//     positional_args
		//     named_args
		//
		// N.B. that the arguments have to be evaluated in the current Frame,
		// before allocating a new daughter frame for the execution of the
		// function body.  If either daughter is present, it is non-empty.

		node.jjtGetChild(1).jjtAccept(this, data) ;
		// should leave on the stack, from top down
		// 1.  an ArgCounts object
		// 2.  the positional arguments (in syntactic order)
		// 3.  the named arguments (each represented by a NamedArg object)
		//
		// first pop off the ArgCounts object, leaving the evaluated args
		ArgCounts ac = (ArgCounts) stack.pop() ;

		//  Now allocate a new Frame for the execution of this function call
		//  (N.B. released below when the function returns)
		env.allocateFrame(funcValue.getStaticFrame()) ;

		// now bind the formal params to the arg values, in the new Frame
		try {
	    	bind_params(funcValue.getParamArrayList(), ac, data) ;
			// may throw FuncCallException, a kind of RuntimeException
			// catch all Exceptions and release the frame before rethrowing
		} catch (RuntimeException re) {
	    	env.releaseFrame() ;
	    	throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}

		// a void function should return no real value; 
		// push a special VoidValue object here; it should be on top of the
		// stack when the function returns (else there was some error)

		stack.push(new VoidValue()) ;

		// now execute the body of the function (a func_block)
		try {
			funcValue.getFuncBlock().jjtAccept(this, data) ;
		} catch (RuntimeException re) {
			env.releaseFrame() ;
			throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}

		// normal release of the Frame created for the execution of this func call
		env.releaseFrame() ;

		// Check return value; there should be a VoidValue object left on the stack
		Object obj = stack.peek() ;
		if (obj == null) {
			throw new FuncCallException("Void function call failed to return properly.") ;
		} else if (!(obj instanceof VoidValue)) {
			throw new FuncCallException("Net valued function call returns incorrect type.") ;
		}
		stack.pop() ;  // get rid of the VoidValue object
		return data ;
    }
	public Object visit(ASTnet_reverse_func_call node, Object data) {
		// just $^__reverse($arg)  built-in, 
		// 		wrapped as $^reverse($arg)
		// one daughter, syntactically constrained
		//		ASTregexp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst fst = (Fst) stack.pop() ;

		// lib.Reverse() is not destructive.
		Fst resultFst = lib.Reverse(fst) ;

		stack.push(resultFst) ;
		return data ;
	}
	public Object visit(ASTnet_shortestPath_func_call node, Object data) {
		// just $^__shortestPath($arg, #nshortest)  built-in, 
		//     wrapped as $^shortestPath($arg, #nshortest=1)
		// two daughters: ASTregexp  ASTnumexp (constrained by the parser)
		node.childrenAccept(this, data) ;
		// top Object on the stack should be a Long (perhaps Double)
		Object obj = stack.pop() ;
		int nshortest ;
		if (obj instanceof Long) {
			nshortest = ((Long)obj).intValue() ;
		} else {
			nshortest = ((Double)obj).intValue() ;
		}
		// next, an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;
		// OpenFst ShortestPath, returns a new Fst.
		if (nshortest < 0) {
			throw new KleeneArgException("The second arg to shortestPath must be >= 0") ;
		}
		Fst resultFst = lib.ShortestPath(fst, nshortest) ;

		stack.push(resultFst) ;
		return data ;
	}
	public Object visit(ASTnet_invert_func_call node, Object data) {
		// just $^__invert($arg)  built-in, wrapped as $^invert($arg)
		// just one daughter: ASTregexp (constrained by the parser)
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		if (fst.getFromSymtab()) {
			// then need to work on a copy
			fst = lib.CopyFst(fst) ;
		} 
		lib.InvertInPlace(fst) ;
		stack.push(fst) ;

		return data ;
	}
	public Object visit(ASTnet_invert_dest_func_call node, Object data) {
		// just $^__invert!($arg)  built-in, wrapped as $^invert!($arg)
		// just one daughter: ASTregexp (constrained by the parser)
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		// Don't make a copy
		// just invert the original network in place
		lib.InvertInPlace(fst) ;
		stack.push(fst) ;
		return data ;
	}
	public Object visit(ASTnet_optimize_func_call node, Object data) {
		// just $^__optimize($arg)  built-in, wrapped as $^optimize($arg)
		// just one daughter: ASTregexp (constrained by the parser)
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;
		Fst resultFst ;
		if (fst.getFromSymtab()) {
			// then need to work on a copy
			resultFst = lib.CopyFst(fst) ;
		} else {
			resultFst = fst ;
		}
		// force the optimization
		lib.OptimizeInPlaceForce(resultFst) ; 
		stack.push(resultFst) ;
		return data ;
	}
	public Object visit(ASTnet_optimize_dest_func_call node, Object data) {
		// just $^__optimize!($arg)  built-in, wrapped as $^optimize!($arg)
		// just one daughter: ASTregexp (constrained by the parser)
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;
		// Don't make a copy
		// just optimize the original network in place
		// force the optimization
		lib.OptimizeInPlaceForce(fst) ;
		stack.push(fst) ;
		return data ;
	}
	public Object visit(ASTnet_rmepsilon_func_call node, Object data) {
		// the function call is not destructive (i.e. returns a new net
		// if the argument is from a symbol table)
		// just $^__rmEpsilon($arg)  built-in, wrapped as $^rmepsilon and
		// $^rmEpsilon
		// just one daughter: ASTregexp (constrained by the parser)
		// N.B. this Kleene function does not work in place for a network
		//   that comes from the symbol table (and so has a name, or alias
		//   names) bound to it
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;
		if (fst.getFromSymtab()) {
			// then need to work on and return a copy
			fst = lib.CopyFst(fst) ;
		} 
		lib.RmEpsilonInPlace(fst) ;
		stack.push(fst) ;
		return data ;
	}
	public Object visit(ASTnet_rmepsilon_dest_func_call node, Object data) {
		// the function call is destructive (i.e. works in place)
		// just $^__rmEpsilon!($arg)  built-in, wrapped as $^rmepsilon! and
		// $^rmEpsilon!
		// just one daughter: ASTregexp (constrained by the parser)
		// N.B. this Kleene function does not work in place for a network
		//   that comes from the symbol table (and so has a name, or alias
		//   names) bound to it
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;
		lib.RmEpsilonInPlace(fst) ;
		stack.push(fst) ;
		return data ;
	}
 	public Object visit(ASTnet_determinize_func_call node, Object data) {
		// not destructive
		// just $^__determinize($arg) built-in, wrapped as $^determinize()
		// just one daughter: ASTregexp  (constrained by the parser)
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;
		if (fst.getFromSymtab()) {
			// then need to work on and return a copy
			fst = lib.CopyFst(fst) ;
		} 
		lib.DeterminizeInPlace(fst) ;
		// N.B. there is an lib.Determinze(fst) that is always
		// non-destructive, always returning a new Fst, but there's
		// no point in returning a new Fst if the input Fst is
		// not from the symbol table
		stack.push(fst) ;
		return data ;
	}
 	public Object visit(ASTnet_determinize_dest_func_call node, Object data) {
		// destructive
		// just $^__determinize!($arg) built-in, wrapped as $^determinize!()
		// just one daughter: ASTregexp  (constrained by the parser)
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;
		// Don't make a copy
		lib.DeterminizeInPlace(fst) ;
		stack.push(fst) ;
		return data ;
	}
	public Object visit(ASTnet_minimize_func_call node, Object data) {
		// this function call is not destructive, (i.e. returns a new net)
		// just $^__minimize($arg)  built-in, wrapped as $^minimize()
		// just one daughter: ASTregexp (constrained by the parser)
		// N.B. the OpenFst Minimize() works in place (destructive),
		//   so if the input comes from the symbol table (and so has
		//   a name, or names, linked to it, then we have to work on
		//   and return a copy
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		if (fst.getFromSymtab()) {
			// then need to work on a copy
			fst = lib.CopyFst(fst) ;
		}
		lib.MinimizeInPlace(fst) ;
		stack.push(fst) ;
		return data ;
	}
	public Object visit(ASTnet_minimize_dest_func_call node, Object data) {
		// destructive, (i.e. works in place)
		// just $^__minimize!($arg)  built-in, wrapped as $^minimize!()
		// just one daughter: ASTregexp (constrained by the parser)
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		// Don't make a copy
		lib.MinimizeInPlace(fst) ;
		stack.push(fst) ;
		return data ;
	}
 	public Object visit(ASTnet_synchronize_func_call node, Object data) {
		// not destructive
		// just $^__synchronize($arg) built-in, wrapped as $^synchronize()
		// just one daughter: ASTregexp  (constrained by the parser)
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;
		if (fst.getFromSymtab()) {
			// then need to work on and return a copy
			fst = lib.CopyFst(fst) ;
		} 
		lib.SynchronizeInPlace(fst) ;
		stack.push(fst) ;
		return data ;
	}
 	public Object visit(ASTnet_synchronize_dest_func_call node, Object data) {
		// destructive
		// just $^__synchronize!($arg) built-in, wrapped as $^synchronize!()
		// just one daughter: ASTregexp  (constrained by the parser)
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;
		// Don't make a copy
		lib.SynchronizeInPlace(fst) ;
		stack.push(fst) ;
		return data ;
	}
	public Object visit(ASTnet_diac_func_call node, Object data) {
		// built-in $^__diac(), wrapped various ways in predefined.kl
		// 3 args (syntactically constrained)

		//	0.  destructive	numexp() boolean

		//	1.  fst			regexp()
		//	2.	projection	regexp() should denote a language of one string
		//								"input"  (or "upper"),
		//								"output" (or "lower"),
		//								or "both"
		
		node.jjtGetChild(0).jjtAccept(this, data) ;
		boolean destructive = lib.isTrue(stack.pop()) ;	// true for destructive

		node.jjtGetChild(1).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		node.jjtGetChild(2).jjtAccept(this, data) ;
		Fst projFst = (Fst) stack.pop() ;

		String projString = lib.GetSingleString(projFst, 
			"Last arg to convertCase must denote a language of exactly one string: input, upper, output, lower or both").trim().toLowerCase() ;

		boolean input = false ;
		boolean output = false ;
		if (	projString.equals("input") 
			||	projString.equals("upper")) {
			input = true ;
		} else if (		projString.equals("output")
					||	projString.equals("lower")) {
			output = true ;
		} else if (		projString.equals("both")) {
			input = true ;
			output = true ;
		} else {
			throw new FuncCallException("Last arg to convertCase must be: input, upper, output, lower or both") ;
		}
			
		Fst workFst = fst ;
		if (!destructive) {
			workFst = lib.CopyFst(fst) ;
		}

		lib.AddDiacInPlace(workFst, input, output) ;

		stack.push(workFst) ;
		return data ;
	}
	public Object visit(ASTnet_case_func_call node, Object data) {
		// built-in $^__case(), wrapped various ways in predefined.kl
		// 7 args (syntactically constrained)

		//	0.  destructive	numexp() boolean
		//	1.	convert		numexp() boolean (true for convert, false for add/allow)
		//	2.  all			numexp() boolean (true for whole path, false for init)

		//	3.  fst			regexp()
		//	4.	to_uc		numexp() boolean
		//	5.	to_lc		numexp() boolean
		//	6.	projection	regexp() should denote a language of one string
		//								"input"  (or "upper"),
		//								"output" (or "lower"),
		//								or "both"
		
		node.jjtGetChild(0).jjtAccept(this, data) ;
		boolean destructive = lib.isTrue(stack.pop()) ;	// true for destructive

		node.jjtGetChild(1).jjtAccept(this, data) ;
		boolean convert = lib.isTrue(stack.pop()) ;	// true for convert, false for add/allow

		node.jjtGetChild(2).jjtAccept(this, data) ;
		boolean all = lib.isTrue(stack.pop()) ;  //  T means whole path, F means init only

		// **************

		node.jjtGetChild(3).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		node.jjtGetChild(4).jjtAccept(this, data) ;
		boolean uc = lib.isTrue(stack.pop()) ;

		node.jjtGetChild(5).jjtAccept(this, data) ;
		boolean lc = lib.isTrue(stack.pop()) ;

		node.jjtGetChild(6).jjtAccept(this, data) ;
		Fst projFst = (Fst) stack.pop() ;

		String projString = lib.GetSingleString(projFst, 
		"Last arg to convertCase must denote a language of exactly one string: input, upper, output, lower, or both").trim().toLowerCase() ;

		boolean input = false ;
		boolean output = false ;
		if (	projString.equals("input") 
			||	projString.equals("upper")) {
			input = true ;
		} else if (		projString.equals("output")
					||	projString.equals("lower")) {
			output = true ;
		} else if (		projString.equals("both")) {
			input = true ;
			output = true ;
		} else {
			throw new FuncCallException("Last arg to convertCase must be: input, upper, output, lower or both") ;
		}
			
		if (!destructive) {
			fst = lib.CopyFst(fst) ;
		}

		if (convert) {
			lib.ConvertCaseInPlace(fst, all, uc, lc, input, output) ;
		} else {
			// add case variants
			// the result should contain all the original arcs, 
			// typically plus some new added arcs (a semantic union); 
			lib.AddCaseInPlace(fst, all, uc, lc, input, output) ;
		}

		stack.push(fst) ;
		return data ;
	}

	public Object visit(ASTnet_inputproj_dest_func_call node, Object data) {
		// syntax is the $^__inputproj!($arg)  built-in
		// 		wrapped as $^inputproj!($arg)
		// one daughter, syntactically constrained
		//		ASTregexp
		
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		lib.InputProjectionInPlace(fst) ;

		stack.push(fst) ;
		return data ;
	}
	public Object visit(ASTnet_inputproj_func_call node, Object data) {
		// syntax is the $^__inputproj($arg)  built-in
		// one daughter, syntactically constrained
		//		ASTregexp
	
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		// not destructive
		Fst resultFst = lib.InputProjection(fst) ;

		stack.push(resultFst) ;
		return data ;
	}
	public Object visit(ASTnet_outputproj_dest_func_call node, Object data) {
		// syntax is the $^__outputproj!($arg)  built-in
		// 		wrapped as $^outputproj!($arg)
		// one daughter, syntactically constrained
		//		ASTregexp
	
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		lib.OutputProjectionInPlace(fst) ;

		stack.push(fst) ;
		return data ;
	}
	public Object visit(ASTnet_outputproj_func_call node, Object data) {
		// syntax is the $^__outputproj($arg)  built-in
		// 		wrapped as $^outputproj($arg)
		// one daughter, syntactically constrained
		//		ASTregexp
	
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		// not destructive
		Fst resultFst = lib.OutputProjection(fst) ;

		stack.push(resultFst) ;
		return data ;
	}
	public Object visit(ASTnet_close_sigma_func_call node, Object data) {
		// this function call is not destructive (i.e. returns a new net)
		// just $^__closeSigma($fst, $base)  built-in, wrapped as
		// $^closeSigma($fst, $base="")
		// See also ASTnet_close_sigma_dest_func_call
		// two daughters: (constrained by the parser)
		//		regexp
		//		regexp
		
		node.childrenAccept(this, data) ;
		Fst base = (Fst) stack.pop() ;
		Fst fst =  (Fst) stack.pop() ;

		// non-destructive
		if (fst.getFromSymtab()) {
			fst = lib.CopyFst(fst) ;
		}

		// first promotes the sigma of fst relative to base
		// then deletes the OTHER arcs in fst, in place
		lib.CloseSigmaInPlace(fst, base) ;

		stack.push(fst) ;
		return data ;
	}
	public Object visit(ASTnet_close_sigma_dest_func_call node, Object data) {
		// this function call is destructive (i.e. always operated on the arg in place
		// just $^__closeSigma!($fst, $base)  built-in, wrapped as
		// $^closeSigma!($fst, $base="")
		// See also ASTnet_close_sigma_func_call
		// two daughters: (constrained by the parser)
		//		regexp
		//		regexp
		
		node.childrenAccept(this, data) ;
		Fst base = (Fst) stack.pop() ;
		Fst fst =  (Fst) stack.pop() ;

		// don't make a copy

		lib.CloseSigmaInPlace(fst, base) ;

		stack.push(fst) ;
		return data ;
	}
	public Object visit(ASTnet_copy_func_call node, Object data) {
		// just $^__copy($arg)  built-in
		// wrapped as $^copy($arg) in predefined.kl
		// should be just one daughter: arg_list
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;
		Fst resultFst = lib.CopyFst(fst) ;
		stack.push(resultFst) ;
		return data ;
	}
	public Object visit(ASTnet_rm_weight_dest_func_call node, Object data) {
		// just $^__rmWeight!($arg)  built-in
		// wrapped as $^rmWeight!($fst) in predefined.kl
		// one daughter, syntactically constrained
		//		ASTregexp
	
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		lib.RmWeightDestFst(fst) ;  // destructive
		
		stack.push(fst) ;
		return data ;
	}
	public Object visit(ASTnet_rm_weight_func_call node, Object data) {
		// just $^__rmWeight($arg)  built-in
		// wrapped as $^rmWeight($fst) in predefined.kl
		// one daughter, syntactically constrained
		//		ASTregexp
	
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		// RmWeightFst is a Java function, non-destructive
		// will copy fst only if necessary
		Fst resultFst = lib.RmWeightFst(fst) ;

		stack.push(resultFst) ;
		return data ;
	}
	public Object visit(ASTnet_subst_symbol_dest_func_call node, Object data) {
		// just $^__substSymbol!($net, $old, $new)
		// wrapped as $^substSymbol!() in predefined.kl
		// exactly three daughters (syntactically constrained)

		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst resultFst = (Fst) stack.pop() ;
		// do not copy--work on this Fst in place

		node.jjtGetChild(1).jjtAccept(this, data) ;
		Fst oldSymFst = (Fst) stack.pop() ;
		// determinize, minimize and epsremove this net
		// even if user has set #KLEENEdeterminize, etc. to false
		lib.OptimizeInPlaceForce(oldSymFst) ;

		node.jjtGetChild(2).jjtAccept(this, data) ;
		Fst newSymFst = (Fst) stack.pop() ;
		lib.OptimizeInPlaceForce(newSymFst) ;

		// oldSymFst should have a single arc and label
		if (!lib.IsString(oldSymFst) || 
			(lib.NumArcs(oldSymFst) != 1)) {
			throw new KleeneArgException("Second arg to substSymbol!() must denote a one-arc acceptor.") ;
		}
		if (oldSymFst.getSigma().size() != 1) {
			throw new KleeneArgException("Second arg to substSymbol!() must be a normal symbol.") ;
		}

		// newSymFst should have a single arc and label, unless it 
		// denotes the emptyStringLanguage, in which case the
		// cpv is 0 (wired in value of epsilon)

		int oldCpv ;
		int newCpv ;

		if (lib.IsEmptyStringLanguage(newSymFst)) {
			// epsilon special case
			// the Fst denotes the empty-string language
			// in OpenFst, 0 is wired in as the int value of epsilon
			newCpv = lib.Epsilon ;
		} else {
			// the usual case, need a one-string, one-symbol fst
			if (!lib.IsString(newSymFst) ||
				(lib.NumArcs(newSymFst) != 1)) {
				throw new KleeneArgException("Third arg to substSymbol!() must denote a one-arc acceptor.")
			;
			}
			if (newSymFst.getSigma().size() != 1) {
				throw new KleeneArgException("Third arg to substSymbol!() must be a normal symbol.") ;
			}
			newCpv = ((Integer)newSymFst.getSigma().toArray()[0]).intValue() ;
		}

		oldCpv = ((Integer)oldSymFst.getSigma().toArray()[0]).intValue() ;

		lib.SubstLabelInPlace(resultFst, oldCpv, newCpv) ;
		stack.push(resultFst) ;
		return data ;
	}

	public Object visit(ASTnet_subst_symbol_func_call node, Object data) {
		// just $^__substSymbol($net, $old, $new)
		// wrapped as $^substSymbol() in predefined.kl
		// exactly three daughters (syntactically constrained)

		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		Fst resultFst = fst ;

		// non-destructive; copy if necessary
		if (fst.getFromSymtab()) {
			resultFst = lib.CopyFst(fst) ;
		} 

		node.jjtGetChild(1).jjtAccept(this, data) ;
		Fst oldSymFst = (Fst) stack.pop() ;
		// determinize, minimize and epsremove this net
		// even if user has set #KLEENEdeterminize, etc. to false
		lib.OptimizeInPlaceForce(oldSymFst) ;

		node.jjtGetChild(2).jjtAccept(this, data) ;
		Fst newSymFst = (Fst) stack.pop() ;
		lib.OptimizeInPlaceForce(newSymFst) ;

		// oldSymFst should have a single arc and label
		if (!lib.IsString(oldSymFst) || 
			(lib.NumArcs(oldSymFst) != 1)) {
			throw new KleeneArgException("Second arg to substSymbol() must denote a one-arc acceptor.") ;
		}
		if (oldSymFst.getSigma().size() != 1) {
			throw new KleeneArgException("Second arg to substSymbol() must be a normal symbol.") ;
		}

		// newSymFst should have a single arc and label, unless it 
		// denotes the emptyStringLanguage, in which case the
		// cpv is 0 (wired in value of epsilon)

		int oldCpv ;
		int newCpv ;

		if (lib.IsEmptyStringLanguage(newSymFst)) {
			// epsilon special case
			// the Fst denotes the empty-string language
			newCpv = lib.Epsilon ;
		} else {
			// the usual case, need a one-string, one-symbol fst
			if (!lib.IsString(newSymFst) ||
				(lib.NumArcs(newSymFst) != 1)) {
				throw new KleeneArgException("Third arg to substSymbol() must denote a one-arc acceptor.")
			;
			}
			if (newSymFst.getSigma().size() != 1) {
				throw new KleeneArgException("Third arg to substSymbol() must be a normal symbol.") ;
			}
			newCpv = ((Integer)newSymFst.getSigma().toArray()[0]).intValue() ;
		}

		oldCpv = ((Integer)oldSymFst.getSigma().toArray()[0]).intValue() ;

		lib.SubstLabelInPlace(resultFst, oldCpv, newCpv) ;
		stack.push(resultFst) ;
		return data ;
	}
	public Object visit(ASTnet_read_xml_call node, Object data) {
		// just $^__readXML($filepath)  built-in
		// wrapped with $^readXML($filepath)
		// just one daughter for $^__readXML() (syntactically
		// constrained by the parser)
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst pathFst = (Fst) stack.pop() ;

		String userTyped = lib.GetSingleString(pathFst, "First arg to readXML must denote a language of exactly one string, denoting a file path") ;

		String fullpath = getFullpath(userTyped) ;

		Fst resultFst ;
		try {
			resultFst = xml2fst(fullpath) ;
			// xml2fst, Java function, see above
		} catch (Exception e) {
			// catch the hard Exception and
			// throw a RuntimeException here, so that Kleene can recover
			throw new FuncCallException("Problem in $^readXML() reading indicated file.") ;
		}

		stack.push(resultFst) ;
		return data ;
	} 
	public Object visit(ASTnet_start_func_call node, Object data) {
		// syntax:   $^start($>foo)   a built-in function-like regexp
		// Not really implemented as a function, cannot be aliased, i.e.
		// $^debut = $^start  is not legal
		// The value of the statement, if successful, is an Fst.

		// just one daughter: ASTrrprod_id, which includes the name
		// of the start production 
		// (This is constrained syntactically by the parser)
		// Don't evaluate daughter.  Just retrieve the image.
		String rrprod_id = ((ASTrrprod_id)node.jjtGetChild(0)).getImage() ;

		// The rrprod_id argument to $^start() will be the root of the 
		//		right-linear grammar,
		// but rrprod_id may refer to other productions, which may, 
		//		in turn, refer
		// to other productions, etc.  (even circular references).
		// Need to check that all the RrDependencies ("right-recursive") 
		//		of $>foo are defined,
		// and that all the dependencies of the dependencies are defined,
		// etc.
		
		// Check and list dependencies (the productions)
		// for the whole implied grammar, keep them in an ArrayList
		// (I tried to use a HashSet, but this proved to be
		// impossible to iterate through AND increase in size)
		ArrayList<String> dependencies = new ArrayList<String>() ;
		dependencies.add(rrprod_id) ;   
		// Start with the current rrprod_id (the start); use ArrayList
		// so that dependencies of the overall grammar are added only
		// once (no duplicates).  The order of objects in the ArrayList
		// is constant and starts at index 0

		// Now loop through the ArrayList of dependencies, adding new ones 
		// as they appear
		// (use a for-loop so that the size can grow during iteration--
		// tried to use HashSet, but this proved impossible)
		for (int i = 0; i < dependencies.size(); i++) {
			String dep = dependencies.get(i) ;
			// Look up the dependency name (getting back an RrProdObject)
			// if successful (i.e. is in the symbol table).
			RrProdObject rrProdObject = (RrProdObject) env.get(dep) ;
			if (rrProdObject == null) {
		    	throw new UndefinedIdException("Undefined rrprod_id: " + 
					dep) ;
			}
			// also check net_id and other _id references? or catch during
			// interpretation?  
			// The dependencies of each defined production are stored 
			// in the symbol table as part of the RrProdObject, 
			// as a HashSet
			HashSet<String> hs = rrProdObject.getRrDependencies() ;
			if (!hs.isEmpty()) {
		        for (Iterator<String> iter = hs.iterator() ; 
					 iter.hasNext(); ) {
		        	String s = iter.next() ;
					// if the overall list of dependencies does not yet
					// contain s, then add it
		        	if (!dependencies.contains(s)) {
		            	dependencies.add(s) ;
		        	}
				}
		    }
		}

		// Reaching here, the whole Rr grammar has been defined.
		// (All the required Rr productions are available in the
		// symbol table.)

		// Create an Fst result.
		// Need to copy all the states and arcs of networks 
		//		of the productions
		// into the result network, keeping track of the 
		//		new startStateNum of each network.  
		// (This is a modification of the concatenation
		// algorithm of OpenFst, minus the code that creates an 
		//		epsilon arc from the final state(s) of the first 
		//		network to the start state of the second.
		//
		// Try to compile each network in the grammar.  Initially
		// each right-recursive reference $>foo is treated much 
		// like a multichar-symbol, but with
		// a negative code point value (a negative label value on an arc).
		// These negative code point values should not be added to
		// the sigma.
		
		Fst  resultFst = null ;

		// Instead of a HashMap, use two parallel ArrayLists,
		// later converted to int[], to pass easily to a C++ 
		// native function that stitches the network together.

		// ArrayLists expand as necessary; avoid any preconceived
		// size limit.
		ArrayList<Integer> keys = new ArrayList<Integer>() ;
		ArrayList<Integer> vals = new ArrayList<Integer>() ;
		//  will have neg ints (representing $>foo refs) mapped 
		// to positive numbers corresponding to start states of the
		// various dependencies (productions) in the overall grammar.
		
		// Again loop through the list of dependencies (the names
		// of all the productions in the implied grammar);
		// they are "defined" (stored as ASTs) but not yet
		// "evaluated" into FSTs

		for (int i = 0; i < dependencies.size(); i++) {
			String rrprod = dependencies.get(i) ;

			RrProdObject rrProdObject = (RrProdObject) env.get(rrprod) ;
			// get the AST (.getRHS()), 
			// and evaluate it to produce an Fst object
			ASTrrProdRHS astRrProdRHS = rrProdObject.getRHS() ;
			astRrProdRHS.jjtAccept(this, data) ;
			// should leave an Fst object on the stack, for one production
			Fst fst = (Fst)stack.pop() ;

			int startStateNum ;

			if (i == 0) {
				// the zeroth is the root production
				resultFst = lib.CopyFst(fst) ; 
				startStateNum = lib.StartState(resultFst) ;
			} else {
				// returns the _new_ start state number of fst
				startStateNum =
					lib.AddStatesAndArcsInPlace(resultFst, fst) ;

			}

			// A rrprod_id like $>foo is stored with a NEGATIVE int value
			// on an Fst arc.
			int negcpv = symmap.getint(rrprod) ;

			// effectively create a Map from neg. int keys 
			//	(right-linear labels)to non-neg.
			// integers that represent the new state number of the start
			// state of this particular Fst (for one of the productions
			// of the overall grammar); 
		 	keys.add(negcpv) ;  // will be a negative int value
			vals.add(startStateNum) ;
	 	}

		// Now need to "stitch" it all together

		lib.RrGrammarLinkInPlace(resultFst, keys, vals) ;

		stack.push(resultFst) ;
		return data ;
	}
	public Object visit(ASTlng_pathcount_func_call node, Object data) {
		// just #^__pathCount($arg)  built-in, wrapped as #^pathCount($arg)
		// should be just one daughter: arg_list with one net
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;
		long count = lib.NumPaths(fst) ;  // returns -1 for cyclic networks

		stack.push(new Long(count)) ;
		return data ;
	}
	public Object visit(ASTlng_statecount_func_call node, Object data) {
		// just #^__stateCount($arg)  built-in, wrapped as #^stateCount($arg)
		// should be just one daughter: arg_list with one net
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;
		int count = lib.NumStates(fst) ;
		// in Kleene, all ints are stored internally as Long
		stack.push(new Long(count)) ;
		return data ;
	}
	public Object visit(ASTlng_arccount_func_call node, Object data) {
		// just #^__arcCount($arg)  built-in, wrapped as #^arcCount($arg)
		// should be just one daughter: arg_list with one net
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;
		int count = lib.NumArcs(fst) ;
		// in Kleene, ints are always stored as Long
		stack.push(new Long(count)) ;
		return data ;
	}
	public Object visit(ASTlng_arity_func_call node, Object data) {
		// just #^__arity($arg)  built-in, wrapped as #^arity($arg)
		// should be just one daughter: arg_list with one net
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		if (lib.IsAcceptor(fst))
			stack.push(new Long(1)) ;
		else 
			stack.push(new Long(2)) ;
		return data ;
	}

	// boolean functions
	
	public Object visit(ASTlng_is_rtn_func_call node, Object data) {
		// just #^__isRtn($arg)  built-in, wrapped as #^isRtn($arg)
		// should be just one daughter: arg_list with one net
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		if (fst.getIsRtn())
			stack.push(new Long(1)) ;
		else 
			stack.push(new Long(0)) ;
		return data ;
	}
	public Object visit(ASTlng_is_cyclic_func_call node, Object data) {
		// just #^__isCyclic($net)  built-in, wrapped as #^isCyclic($net)
		// should be just one daughter: arg_list with one net
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		if (lib.IsCyclic(fst))
			stack.push(new Long(1)) ;
		else 
			stack.push(new Long(0)) ;
		return data ;
	}
	public Object visit(ASTlng_is_acceptor_func_call node, Object data) {
		// just #^__isAcceptor($net)  built-in, wrapped as #^isAcceptor($net)
		// should be just one daughter: arg_list with one net
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		if (lib.IsSemanticAcceptor(fst))
			stack.push(new Long(1)) ;
		else 
			stack.push(new Long(0)) ;
		return data ;
	}
	public Object visit(ASTlng_is_transducer_func_call node, Object data) {
		// just #^__isTransducer($net)  built-in, wrapped as #^isTransducer($net)
		// should be just one daughter: arg_list with one net
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		if (!(lib.IsSemanticAcceptor(fst)))
			stack.push(new Long(1)) ;
		else 
			stack.push(new Long(0)) ;
		return data ;
	}

	public Object visit(ASTlng_is_weighted_func_call node, Object data) {
		// just #^__isWeighted($net)  built-in, wrapped as #^isWeighted($net)
		// should be just one daughter: arg_list with one net
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		if (lib.IsWeighted(fst))
			stack.push(new Long(1)) ;
		else 
			stack.push(new Long(0)) ;
		return data ;
	}

	public Object visit(ASTlng_is_ideterministic_func_call node, Object data) {
		// just #^__isIDeterministic($net)  built-in, wrapped as #^isIDeterministic($net)
		// should be just one daughter: arg_list with one net
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		if (lib.IsIDeterministic(fst))
			stack.push(new Long(1)) ;
		else 
			stack.push(new Long(0)) ;
		return data ;
	}

	public Object visit(ASTlng_is_odeterministic_func_call node, Object data) {
		// just #^__isODeterministic($net)  built-in, wrapped as #^isODeterministic($net)
		// should be just one daughter: arg_list with one net
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		if (lib.IsODeterministic(fst))
			stack.push(new Long(1)) ;
		else 
			stack.push(new Long(0)) ;
		return data ;
	}

	public Object visit(ASTlng_is_epsilonfree_func_call node, Object data) {
		// just #^__isEpsilonFree($net)  built-in, wrapped as #^isEpsilonFree($net)
		// should be just one daughter: arg_list with one net
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		if (lib.IsEpsilonFree(fst))
			stack.push(new Long(1)) ;
		else 
			stack.push(new Long(0)) ;
		return data ;
	}

	public Object visit(ASTlng_is_empty_language_func_call node, Object data) {
		// just #^__isEmptyLanguage($net)  built-in, wrapped as #^isEmptyLanguage($net)
		// should be just one daughter: arg_list with one net
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		if (lib.IsEmptyLanguage(fst))
			stack.push(new Long(1)) ;
		else 
			stack.push(new Long(0)) ;
		return data ;
	}

	public Object visit(ASTlng_is_empty_string_language_func_call node, Object data) {
		// just #^__isEmptyStringLanguage($net) built-in, wrapped as #^isEmptyStringLanguage($net)
		// should be just one daughter: arg_list with one net
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		if (lib.IsEmptyStringLanguage(fst))
			stack.push(new Long(1)) ;
		else 
			stack.push(new Long(0)) ;
		return data ;
	}

	public Object visit(ASTlng_contains_empty_string_func_call node, Object data) {
		// just #^__containsEmptyString($net) built-in, wrapped as #^containsEmptyString($net)
		// should be just one daughter: arg_list with one net
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		if (lib.ContainsEmptyString(fst))
			stack.push(new Long(1)) ;
		else 
			stack.push(new Long(0)) ;
		return data ;
	}

	public Object visit(ASTlng_is_string_func_call node, Object data) {
		// just #^__isString($net)  built-in, wrapped as #^isString($net) and
		// 										  	     #^isSingleStringLanguage($net)
		// should be just one daughter: arg_list with one net
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		if (lib.IsString(fst))
			stack.push(new Long(1)) ;
		else 
			stack.push(new Long(0)) ;
		return data ;
	}

	public Object visit(ASTlng_contains_other_func_call node, Object data) {
		// just #^__containsOther($net)  built-in, wrapped as #^containsOther($net)
		// should be just one daughter: arg_list with one net
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		if (fst.getContainsOther())
			stack.push(new Long(1)) ;
		else 
			stack.push(new Long(0)) ;
		return data ;
	}


/*	not needed?  see definition in predefined.kl
	public Object visit(ASTlng_is_universal_language_func_call node, Object data) {
		// just #^__isUniversalLanguage($net)  built-in, wrapped as #^isUniversalLanguage($net)
		// should be just one daughter: arg_list with one net
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves an Fst object on the stack
		Fst fst = (Fst) stack.pop() ;

		// IsUniversalLanguage doesn't exist yet
		if (lib.IsUniversalLanguage(fst))
			stack.push(new Long(1)) ;
		else 
			stack.push(new Long(0)) ;
		return data ;
	}
*/


/*
	public Object visit(ASTlng_equivalent_func_call node, Object data) {
		// just #^__equivalent($a, $b, #num)  built-in, wrapped as #^equivalent()
		// should be just three daughters, syntactically constrained
		node.childrenAccept(this, data) ;

		double d ;

		Object num = stack.pop() ;
		// third arg could be Long or Double
		if (num instanceof Long) {
			d = ((Long)num).doubleValue() ;
		} else {
			d = ((Double)num).doubleValue() ;
		}

		Fst b = (Fst) stack.pop() ;
		Fst a = (Fst) stack.pop() ;

		if (lib.Equivalent(a, b, d))
			stack.push(new Long(1)) ;	// true
		else 
			stack.push(new Long(0)) ;	// false
		return data ;
	}
*/

	public Object visit(ASTnum_abs_func_call node, Object data) {
		// just #^__abs(numexp) built-in
		// should be just one daughter: arg_list with one numexp
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves a Long or Double object on the stack
		Object obj = stack.pop() ;

		// produce either Long or Double, according to the input
		if (obj instanceof Long) {
			stack.push(new   Long(Math.abs(((Long)obj).longValue()))) ;
		} else {
			stack.push(new Double(Math.abs(((Double)obj).doubleValue()))) ;
		}
		return data;
	}
	public Object visit(ASTnet_to_string_func_call node, Object data) {
		// convert a number (Long or Float) to a string
		// just $^__toString(numexp) built-in
		// should be just one daughter: arg_list with one numexp
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves a Long or Double object on the stack
		Object obj = stack.pop() ;

		String str ;

		// produce either Long or Double, according to the input
		if (obj instanceof Long) {
			str = Long.toString(((Long)obj).longValue()) ;
		} else {
			str = Double.toString(((Double)obj).doubleValue()) ;
		}

		// Because all digits in a number string are in the BMP,
		// this str will be composed of BMP Unicode characters
		// convert to an array of int
		int len = str.length() ;
		int[] cpvArray = new int[len] ;

		for (int index=0; index < len; index++) {
			int cpv = str.codePointAt(index) ;
			symmap.putsym(String.valueOf((char) cpv)) ;
			cpvArray[index] = cpv ;
		}
		Fst resultFst = lib.FstFromCpvArray(cpvArray) ;
		stack.push(resultFst) ;

		return data;
	}
	public Object visit(ASTnet_implode_func_call node, Object data) {
		// The $^__implode(regexp) built-in, wrapped as $^implode(),
		// implodes a single string of characters into a single symbol, 
		// typically a multichar symbol,
		//    and return a network with one arc, labeled with that symbol

		// there should be just one daughter: arg_list with one regexp
		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst fstOrig = (Fst) stack.pop() ;

		String str = lib.GetSingleString(fstOrig, "Arg to implode must denote a language of exactly one string.") ;

		if (str.length() == 0) {
			throw new KleeneArgException("Argument to implode must denote a language of one non-empty string.") ;
		}

		// get the code point value (cpv)
		int cpv = symmap.putsym(str) ;  // might exist in symmap, or not
		// get back the int value

		// create a one-arc Fst with the arc labeled with the codepoint
		stack.push(lib.OneArcFst(cpv)) ;
		return data;
	}
	public Object visit(ASTnet_explode_func_call node, Object data) {
		// just #^__explode(regexp) built-in
		// take a two-state, one arc acceptor, get the label 
		//    on the one arc
		//    (an int, typically mapping to a multichar name), 
		//		get the print name,
		//    make a new one-path network for the exploded 
		//		string of characters in that name

		// there should be just one daughter, acceptor,
		// one string (non-empty), two states, one arc
		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst fst = (Fst) stack.pop() ;

		// get the single arc label
		int label = lib.GetSingleArcLabel(fst) ;

		// get the print name of the symbol 
		//	(typically a multichar symbol name)
		String str = symmap.getsym(label) ;

		// KRB
		// the str will be composed of BMP Unicode characters 
		//		(keep an eye on this--it
		// 		depends on what is allowed in a multichar symbol)
		int len = str.length() ;
		// create an array of ints (code point values)
		int[] cpvArray = new int[len] ;
		for (int index=0; index < len; index++) {
			int cpv = str.codePointAt(index) ;
			symmap.putsym(String.valueOf((char) cpv)) ;
			cpvArray[index] = cpv ;
		}
		Fst resultFst = lib.FstFromCpvArray(cpvArray) ;
		stack.push(resultFst) ;

		return data;
	}
	public Object visit(ASTnet_get_func_call node, Object data) {
		// $^__get($@arr, 0) wrapped as $^get($@arr, 0)
		// two daughters
		//		net_list_exp
		//		numexp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NetList netList = (NetList)(stack.pop()) ;

		node.jjtGetChild(1).jjtAccept(this, data) ;
		Object obj = stack.pop() ;
		int index ;
		if (obj instanceof Long) {
			index = ((Long)obj).intValue() ;
		} else {
			index = ((Double)obj).intValue() ;
		}

		int size = netList.size() ;
		if (index < 0 || index >= size) {
			throw new KleeneArgException("Illegal index to $^get(): " +
			index) ;
		}

		Fst element = netList.get(index) ;
		stack.push(element) ;
		return data ;
	}
	public Object visit(ASTnet_getlast_func_call node, Object data) {
		// $^__getLast($@arr) wrapped as $^getLast($@arr)
		// two daughters
		//		net_list_exp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NetList netList = (NetList)(stack.pop()) ;

		if (netList.isEmpty()) {
			throw new KleeneArgException("The argument list is empty.") ;
		}
		int size = netList.size() ;

		Fst element = netList.get(size - 1) ;
		stack.push(element) ;
		return data ;
	}
	public Object visit(ASTnum_get_func_call node, Object data) {
		// #^__get(#@arr, 0) wrapped as #^get(#@arr, 0)
		// two daughters
		//		num_list_exp
		//		numexp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NumList numList = (NumList)(stack.pop()) ;

		node.jjtGetChild(1).jjtAccept(this, data) ;
		Object obj = stack.pop() ;
		int index ;
		if (obj instanceof Long) {
			index = ((Long)obj).intValue() ;
		} else {
			index = ((Double)obj).intValue() ;
		}

		int size = numList.size() ;
		if (index < 0 || index >= size) {
			throw new KleeneArgException("Illegal index to #^get(): " + index) ;
		}

		Object element = numList.get(index) ;  // Long or Double
		stack.push(element) ;
		return data ;
	}
	public Object visit(ASTnum_getlast_func_call node, Object data) {
		// #^__getLast(#@arr) wrapped as #^getLast(#@arr)
		// one daughter
		//		num_list_exp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NumList numList = (NumList)(stack.pop()) ;

		if (numList.isEmpty()) {
			throw new KleeneArgException("The argument list is empty.") ;
		}
		int size = numList.size() ;

		Object element = numList.get(size - 1) ;  // Long or Double
		stack.push(element) ;
		return data ;
	}
	public Object visit(ASTnet_head_func_call node, Object data) {
		// $^__head($@arr) wrapped as $^head($@arr)
		// one daughter
		//		net_list_exp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NetList netList = (NetList)(stack.pop()) ;

		if (netList.isEmpty()) {
			throw new KleeneArgException("The argument list is empty.") ;
		}
		int size = netList.size() ;

		Fst element = netList.get(0) ;
		stack.push(element) ;
		return data ;
	}
	public Object visit(ASTnum_head_func_call node, Object data) {
		// #^__head(#@arr) wrapped as #^head(#@arr)
		// one daughter
		//		num_list_exp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NumList numList = (NumList)(stack.pop()) ;

		if (numList.isEmpty()) {
			throw new KleeneArgException("The argument list is empty.") ;
		}

		Object element = numList.get(0) ;
		stack.push(element) ;
		return data ;
	}
	public Object visit(ASTnet_pop_dest_func_call node, Object data) {
		// $^__pop!($@arr) wrapped as $^pop!($@arr)
		// one daughter
		//		net_list_exp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NetList netList = (NetList)(stack.pop()) ;

		if (netList.isEmpty()) {
			throw new KleeneArgException("The argument list is empty.") ;
		}

		Fst element = netList.pop() ;
		stack.push(element) ;
		return data ;
	}
	public Object visit(ASTnet_remove_dest_func_call node, Object data) {
		// $^__remove!($@arr, #index)
		// two daughters
		//		net_list_exp
		//		numexp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NetList netList = (NetList)(stack.pop()) ;

		node.jjtGetChild(1).jjtAccept(this, data) ;
		Object obj = stack.pop() ;

		int index = 0 ;
		if (obj instanceof Long) {
			index = ((Long)obj).intValue() ;
		} else {
			index = ((Double)obj).intValue() ;
		}

		NetList resultList = netList ;

		int size = resultList.size() ;
		if (index < 0 || index >= size) {
			throw new KleeneArgException("Illegal index to $^remove!(): "
			+ index) ;
		}

		Fst removedElement = (Fst) (resultList.remove(index)) ;

		stack.push(removedElement) ;
		return data ;
	}
	public Object visit(ASTnet_removelast_dest_func_call node, Object data) {
		// $^__removeLast!($@arr)
		// one daughter
		//		net_list_exp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NetList netList = (NetList)(stack.pop()) ;

		NetList resultList = netList ;

		if (netList.isEmpty()) {
			throw new KleeneArgException("The argument list is empty.") ;
		}
		int size = resultList.size() ;

		Fst removedElement = (Fst) (resultList.remove(size - 1)) ;

		stack.push(removedElement) ;
		return data ;
	}
	public Object visit(ASTnum_pop_dest_func_call node, Object data) {
		// #^__pop!(#@arr) wrapped as #^pop!(#@arr)
		// one daughter
		//		num_list_exp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NumList numList = (NumList)(stack.pop()) ;

		if (numList.isEmpty()) {
			throw new KleeneArgException("The argument list is empty.") ;
		}

		Object removedElement = numList.pop() ;
		stack.push(removedElement) ;
		return data ;
	}
	public Object visit(ASTnum_remove_dest_func_call node, Object data) {
		// #^__remove!(#@arr, #index)
		// two daughters
		//		num_list_exp
		//		numexp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NumList numList = (NumList)(stack.pop()) ;

		node.jjtGetChild(1).jjtAccept(this, data) ;
		Object obj = stack.pop() ;

		int index = 0 ;
		if (obj instanceof Long) {
			index = ((Long)obj).intValue() ;
		} else {
			index = ((Double)obj).intValue() ;
		}

		NumList resultList = numList ;

		int size = resultList.size() ;
		if (index < 0 || index >= size) {
			throw new KleeneArgException("Illegal index to #^remove!(): "
			+ index) ;
		}

		Object removedElement = resultList.remove(index) ;

		stack.push(removedElement) ;
		return data ;
	}
	public Object visit(ASTnum_removelast_dest_func_call node, Object data) {
		// #^__removeLast!(#@arr)
		// one daughter
		//		num_list_exp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NumList numList = (NumList)(stack.pop()) ;

		NumList resultList = numList ;

		if (resultList.isEmpty()) {
			throw new KleeneArgException("The argument list is empty.") ;
		}
		int size = resultList.size() ;

		Object removedElement = resultList.remove(size - 1) ;

		stack.push(removedElement) ;
		return data ;
	}
	public Object visit(ASTslice_exp node, Object data) {
		// two numexp daughters
		node.childrenAccept(this, data) ;
		// should leave two objects on the stack (Long or Double)
		return data ;
	}
	public Object visit(ASTnet_list_get_slice_func_call node, Object data) {
		// $^getSlice($@arr, 0, 3, 5:8), cannot be aliased
		// 	because it has a variable number of arguments
		// at least two daughters
		//		net_list_exp
		//	    ( numexp || slice_exp )+      

		int childCount = node.jjtGetNumChildren() ;

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NetList netList = (NetList)(stack.pop()) ;

		NetList resultList = new NetList() ;

		int indexLow, indexHigh ;
		Object obj, objLow, objHigh ;
		int size ;

		for (int i = 1; i < childCount; i++) {
			if (node.jjtGetChild(i) instanceof ASTnumexp) {
				node.jjtGetChild(i).jjtAccept(this, data) ;
				// should leave a Long or Double on the stack

				obj = stack.pop() ;
				resultList.add(netList.get(getIntValue(obj))) ;

			} else if (node.jjtGetChild(i) instanceof ASTslice_exp) {
				node.jjtGetChild(i).jjtAccept(this, data) ;
				// should leave two objects on the stack, High on top of Low
				// could be Long or Double

				objHigh = stack.pop() ;
				objLow = stack.pop() ;

				indexLow = getIntValue(objLow) ;
				indexHigh = getIntValue(objHigh) ;

				// indexLow is inclusive; indexHigh is exclusive
				
				if (indexLow > indexHigh) {
					throw new KleeneArgException("Illegal relative values of range " + indexLow + ".." + indexHigh) ;
				}

				size = netList.size() ;

				if (indexLow < 0 || indexLow >= size) {
					throw new KleeneArgException("Illegal low value of range " + indexLow + ".." + indexHigh) ;
				}

				if (indexHigh < 0 || indexHigh > size) {
					throw new KleeneArgException("Illegal high value of range " + indexLow + ".." + indexHigh) ;
				}

				for (int j = indexLow; j < indexHigh; j++) {
					resultList.add(netList.get(j)) ;
				}
			}
		}

		stack.push(resultList) ;
		return data ;
	}
	public Object visit(ASTnum_list_get_slice_func_call node, Object data) {
		// #^getSlice(#@arr, 0, 3, 5:8), cannot be aliased
		// 	because it has a variable number of arguments
		// at least two daughters
		//		num_list_exp
		//	    ( numexp || slice_exp )+      

		int childCount = node.jjtGetNumChildren() ;

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NumList numList = (NumList)(stack.pop()) ;

		NumList resultList = new NumList() ;

		int indexLow, indexHigh ;
		Object obj, objLow, objHigh ;
		int size ;

		Object longOrDouble ;

		for (int i = 1; i < childCount; i++) {
			if (node.jjtGetChild(i) instanceof ASTnumexp) {
				node.jjtGetChild(i).jjtAccept(this, data) ;
				// should leave a Long or Double on the stack

				obj = stack.pop() ;

				longOrDouble = numList.get(getIntValue(obj)) ;

				if (longOrDouble instanceof Long) {
					resultList.add((Long)longOrDouble) ;
				} else {
					resultList.add((Double)longOrDouble) ;
				}
			} else if (node.jjtGetChild(i) instanceof ASTslice_exp) {
				node.jjtGetChild(i).jjtAccept(this, data) ;
				// should leave two objects on the stack, High on top of Low
				// could be Long or Double

				objHigh = stack.pop() ;
				objLow = stack.pop() ;

				indexLow = getIntValue(objLow) ;
				indexHigh = getIntValue(objHigh) ;

				// indexLow is inclusive; indexHigh is exclusive
				
				if (indexLow > indexHigh) {
					throw new KleeneArgException("Illegal relative values of range " + indexLow + ".." + indexHigh) ;
				}

				size = numList.size() ;

				if (indexLow < 0 || indexLow >= size) {
					throw new KleeneArgException("Illegal low value of range " + indexLow + ".." + indexHigh) ;
				}

				if (indexHigh < 0 || indexHigh > size) {
					throw new KleeneArgException("Illegal high value of range " + indexLow + ".." + indexHigh) ;
				}

				for (int j = indexLow; j < indexHigh; j++) {
					longOrDouble = numList.get(j) ;
					if (longOrDouble instanceof Long) {
						resultList.add((Long)longOrDouble) ;
					} else {
						resultList.add((Double)longOrDouble) ;
					}
				}
			}
		}

		stack.push(resultList) ;
		return data ;
	}
	public Object visit(ASTnet_list_push_dest_func_call node, Object data) {
		// $^__push!($fst, $@arr)
		// two daughters
		//		regexp
		//		net_list_exp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst fst = (Fst)(stack.pop()) ;

		node.jjtGetChild(1).jjtAccept(this, data) ;
		NetList netList = (NetList)(stack.pop()) ;

		NetList resultList = netList ;

		resultList.push(fst) ;

		stack.push(resultList) ;
		return data ;
	}
	public Object visit(ASTnum_list_push_dest_func_call node, Object data) {
		// #^__push!(#num, #@arr)
		// two daughters
		//		numexp
		//		num_list_exp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		Object longOrDouble = stack.pop() ;

		node.jjtGetChild(1).jjtAccept(this, data) ;
		NumList numList = (NumList)(stack.pop()) ;

		NumList resultList = numList ;

		if (longOrDouble instanceof Long) {
			resultList.push((Long)longOrDouble) ;
		} else {
			resultList.push((Double)longOrDouble) ;
		}

		stack.push(resultList) ;
		return data ;
	}
	public Object visit(ASTnet_list_add_dest_func_call node, Object data) {
		// $^__add!($@arr, $fst)
		// two daughters
		//		net_list_exp
		//		regexp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NetList netList = (NetList)(stack.pop()) ;

		node.jjtGetChild(1).jjtAccept(this, data) ;
		Fst fst = (Fst)(stack.pop()) ;

		NetList resultList = netList ;

		resultList.add(fst) ;

		stack.push(resultList) ;
		return data ;
	}
	public Object visit(ASTnum_list_add_dest_func_call node, Object data) {
		// #^__add!(#@arr, #num)
		// two daughters
		//		num_list_exp
		//		numexp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NumList numList = (NumList)(stack.pop()) ;

		node.jjtGetChild(1).jjtAccept(this, data) ;
		Object longOrDouble = stack.pop() ;

		NumList resultList = numList ;

		if (longOrDouble instanceof Long) {
			resultList.add((Long)longOrDouble) ;
		} else {
			resultList.add((Double)longOrDouble) ;
		}

		stack.push(resultList) ;
		return data ;
	}
	public Object visit(ASTnet_list_addat_dest_func_call node, Object data) {
		// $^__add!($@arr, #index, $fst)
		// three daughters
		//		net_list_exp
		//		numexp
		//		regexp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NetList netList = (NetList)(stack.pop()) ;

		node.jjtGetChild(1).jjtAccept(this, data) ;
		Object obj = stack.pop() ;

		int index = 0 ;
		if (obj instanceof Long) {
			index = ((Long)obj).intValue() ;
		} else {
			index = ((Double)obj).intValue() ;
		}

		node.jjtGetChild(2).jjtAccept(this, data) ;
		Fst fst = (Fst)(stack.pop()) ;

		NetList resultList = netList ;

		int size = resultList.size() ;
		if (index < 0 || index > size) {
			throw new KleeneArgException("Illegal index to $@^addAt!(): "
			+ index) ;
		}

		resultList.addAt(index, fst) ;

		stack.push(resultList) ;
		return data ;
	}
	public Object visit(ASTnum_list_addat_dest_func_call node, Object data) {
		// #^__add!(#@arr, #index, #num)
		// three daughters
		//		num_list_exp
		//		numexp
		//		numexp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NumList numList = (NumList)(stack.pop()) ;

		node.jjtGetChild(1).jjtAccept(this, data) ;
		Object obj = stack.pop() ;

		int index = 0 ;
		if (obj instanceof Long) {
			index = ((Long)obj).intValue() ;
		} else {
			index = ((Double)obj).intValue() ;
		}

		node.jjtGetChild(2).jjtAccept(this, data) ;
		Object longOrDouble = stack.pop() ;

		NumList resultList = numList ;

		int size = resultList.size() ;
		if (index < 0 || index > size) {
			throw new KleeneArgException("Illegal index to #@^addAt!(): "
			+ index) ;
		}

		if (longOrDouble instanceof Long) {
			resultList.addAt(index, (Long)longOrDouble) ;
		} else {
			resultList.addAt(index, (Double)longOrDouble) ;
		}

		stack.push(resultList) ;
		return data ;
	}
	public Object visit(ASTnet_list_set_dest_func_call node, Object data) {
		// $^__set!($@arr, #index, $fst)
		// three daughters
		//		net_list_exp
		//		numexp
		//		regexp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NetList netList = (NetList)(stack.pop()) ;

		node.jjtGetChild(1).jjtAccept(this, data) ;
		Object obj = stack.pop() ;

		int index = 0 ;
		if (obj instanceof Long) {
			index = ((Long)obj).intValue() ;
		} else {
			index = ((Double)obj).intValue() ;
		}

		node.jjtGetChild(2).jjtAccept(this, data) ;
		Fst fst = (Fst)(stack.pop()) ;

		NetList resultList = netList ;

		int size = resultList.size() ;
		if (index < 0 || index >= size) {
			throw new KleeneArgException("Illegal index to $@^set!(): "
			+ index) ;
		}

		resultList.set(index, fst) ;

		stack.push(resultList) ;
		return data ;
	}
	public Object visit(ASTnum_list_set_dest_func_call node, Object data) {
		// #^__set!(#@arr, #index, #num)
		// three daughters
		//		num_list_exp
		//		numexp
		//		numexp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NumList numList = (NumList)(stack.pop()) ;

		node.jjtGetChild(1).jjtAccept(this, data) ;
		Object obj = stack.pop() ;

		int index = 0 ;
		if (obj instanceof Long) {
			index = ((Long)obj).intValue() ;
		} else {
			index = ((Double)obj).intValue() ;
		}

		node.jjtGetChild(2).jjtAccept(this, data) ;
		Object longOrDouble = stack.pop() ;

		NumList resultList = numList ;

		int size = resultList.size() ;
		if (index < 0 || index >= size) {
			throw new KleeneArgException("Illegal index to #@^set!(): "
			+ index) ;
		}

		if (longOrDouble instanceof Long) {
			resultList.set(index, (Long)longOrDouble) ;
		} else {
			resultList.set(index, (Double)longOrDouble) ;
		}

		stack.push(resultList) ;
		return data ;
	}
	public Object visit(ASTnet_list_tail_func_call node, Object data) {
		// $^__tail($@arr)
		// one daughter
		//		net_list_exp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NetList netList = (NetList)(stack.pop()) ;

		// as in Scheme, Haskell and Scala, fail if the argument
		// list is empty
		if (netList.isEmpty()) {
			throw new KleeneArgException("The argument list is empty.") ;
		}

		NetList resultList = new NetList() ;

		int size = netList.size() ;

		// new object has all but the zeroth element
		for (int i = 1; i < size; i++) {
			resultList.add(netList.get(i)) ;
		}

		stack.push(resultList) ;
		return data ;
	}
	public Object visit(ASTnet_list_copy_func_call node, Object data) {
		// $^__copy($@arr)
		// one daughter
		//		net_list_exp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NetList netList = (NetList)(stack.pop()) ;

		NetList resultList = new NetList() ;

		int size = netList.size() ;

		// shallow copy
		for (int i = 0; i < size; i++) {
			resultList.add(netList.get(i)) ;
		}

		stack.push(resultList) ;
		return data ;
	}
	public Object visit(ASTnum_list_tail_func_call node, Object data) {
		// #^__tail(#@arr)
		// one daughter
		//		num_list_exp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NumList numList = (NumList)(stack.pop()) ;

		// as in Scheme, Haskell and Scala, fail if the argument
		// list is empty
		if (numList.isEmpty()) {
			throw new KleeneArgException("The argument list is empty.") ;
		}

		NumList resultList = new NumList() ;

		int size = numList.size() ;

		Object obj = null ;

		// new object has all but the zeroth element
		for (int i = 1; i < size; i++) {
			obj = numList.get(i) ;
			if (obj instanceof Long) {
				resultList.add((Long)obj) ;
			} else {
				resultList.add((Double)obj) ;
			}
		}

		stack.push(resultList) ;
		return data ;
	}
	public Object visit(ASTnum_list_copy_func_call node, Object data) {
		// #^__copy(#@arr)
		// one daughter
		//		num_list_exp

		node.jjtGetChild(0).jjtAccept(this, data) ;
		NumList numList = (NumList)(stack.pop()) ;

		NumList resultList = new NumList() ;

		int size = numList.size() ;

		Object obj = null ;

		// shallow copy
		for (int i = 0; i < size; i++) {
			obj = numList.get(i) ;
			if (obj instanceof Long) {
				resultList.add((Long)obj) ;
			} else {
				resultList.add((Double)obj) ;
			}
		}

		stack.push(resultList) ;
		return data ;
	}
	public Object visit(ASTnet_list_get_sigma_func_call node, Object data) {
		// $@^__getSigma($fst)
		// one daughter
		//		regexp
		// return the sigma as an array of nets, one for each symbol in
		// the sigma (minus special chars starting "__" that should not be 
		// considered when promoting OTHER)

		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst fst = (Fst)(stack.pop()) ;

		NetList resultList = new NetList() ;

		String specialSymbolPrefix = "__" ;
		HashSet<Integer> sigma = fst.getSigma() ;
		String symbolName = "" ;

		if (!sigma.isEmpty()) {
			for (Iterator<Integer> iter = sigma.iterator(); iter.hasNext(); ) {
				int cpv = iter.next().intValue() ;
				symbolName = symmap.getsym(cpv) ;
				if (!(symbolName.startsWith(specialSymbolPrefix))) {
					resultList.add(lib.OneArcFst(cpv)) ;
				}
			}
		}

		stack.push(resultList) ;
		return data ;
	}
	public Object visit(ASTnum_list_get_sigma_func_call node, Object data) {
		// #@^__getSigma($fst)
		// one daughter
		//		regexp
		// return the sigma as an array of Integer, one for each symbol in
		// the sigma (minus special chars starting "__" that should not be
		// considered when promoting OTHER)

		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst fst = (Fst)(stack.pop()) ;

		NumList resultList = new NumList() ;

		String specialSymbolPrefix = "__" ;
		HashSet<Integer> sigma = fst.getSigma() ;
		String symbolName = "" ;

		if (!sigma.isEmpty()) {
			for (Iterator<Integer> iter = sigma.iterator(); iter.hasNext(); ) {
				int cpv = iter.next().intValue() ;
				symbolName = symmap.getsym(cpv) ;
				if (!(symbolName.startsWith(specialSymbolPrefix))) {
					resultList.add(new Long(cpv)) ;
				}
			}
		}

		stack.push(resultList) ;
		return data ;
	}
	public Object visit(ASTnet_get_net_func_call node, Object data) {
		// one regexp daughter, syntactically constrained
		// should represent a language of one string

		// return a handle to a network, given its name

		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst netNameFst = (Fst)(stack.pop()) ;
		lib.OptimizeInPlaceForce(netNameFst) ;
		//lib.FstDump(netNameFst) ;

		String net_id = lib.GetSingleString(netNameFst, 
			"Argument to $^getNet() must denote a language of exactly one string") ;

		if (net_id.length() == 0) {
			throw new KleeneArgException("Second arg to info (filepath) must denote a non-empty string") ;
		}

		// with the String name of the fst, retrieve the Fst from the environment
		Fst fst = (Fst) env.get(net_id) ;
		if (fst != null) {
			// The net_id was found in a symbol table.
			//
			// Fst is a Java wrapper object around a long int that stores a
			// (basically C++) pointer to a network.  Need to mark this
			// Fst as being "fromSymtab" if, as here, the value came from a
			// symbol table (and therefore cannot be changed, i.e. must be
			// persistent in case the net_id is referred to again).
			fst.setFromSymtab(true) ;
			// leave the Fst on the stack
			stack.push(fst) ;
			return data ;
		} else {
			// attempt to refer to (use) an undefined variable
			throw new UndefinedIdException("Undefined net_id: " + net_id) ;
		}
	}
	public Object visit(ASTnet_sub_func_call node, Object data) {
		// one ASTnet_id daughter, constrained by the parser, carries the image

		// return a one-arc network that represents a reference to
		// a subnetwork (to appear inside an RTN)

		boolean openFstRtnConv = isOpenFstRtnConventions() ;
		// currently either OpenFstRtnConventions, or
		//					SapRtnConventions

		// In OpenFstRtnConventions, a reference to a subnet $foo is
		// an arc labeled $eps:'__$foo'

		// In SapRtnConventions, a reference to a subnet $foo is
		// an arc labeled '$foo':'$foo' (the user can also manually
		// encode $^sub($foo):$eps, which indicates, to the runtime
		// code, a mapping to epsilon)

		String specialCharPrefix = "" ;
		
		if (openFstRtnConv) {
			specialCharPrefix = "__" ;
		} else {
			specialCharPrefix = "" ;
		}

		// get the image from the daughter
		String name = specialCharPrefix + ((ASTnet_id)node.jjtGetChild(0)).getImage() ;

		if (openFstRtnConv) {
			stack.push(lib.OneArcFst(0, name)) ;	// OpenFstRtnConventions
		} else {
			stack.push(lib.OneArcFst(name, name)) ;	// SapRtnConventions
		}

		return data ;
	}

	// currently works from the sigma, finding all __$ references whether they
	// are on the input or output side
	private void findDependencies(Fst fst, ArrayList<String> dependencies) {
		String subnetReferencePrefix = "" ;
		if (isOpenFstRtnConventions()) {
			subnetReferencePrefix = "__$" ;
		} else {
			subnetReferencePrefix = "$" ;
		}
		HashSet<Integer> sigma = fst.getSigma() ;
		String symbolName = "" ;
		String netIdName = "" ;

		if (!sigma.isEmpty()) {
			for (Iterator<Integer> iter = sigma.iterator(); iter.hasNext(); ) {
				// the name should look like "a", "b", and perhaps
				// "__$sub" (for OpenFstRtnConventions) or "$sub" (for SapRtnConventions)
				symbolName = symmap.getsym(iter.next().intValue()) ;
				if (symbolName.startsWith(subnetReferencePrefix)) {
					// save the subnet name, minus any prefix
					netIdName = symbolName.substring(symbolName.indexOf("$")) ;
					// avoid duplicates
					if (!dependencies.contains(netIdName)) {
						dependencies.add(netIdName) ;
					}
				}
			}
		}
	}

	public Object visit(ASTnet_embed_rtn_subnets_func_call node, Object data) {

		// evaluate the argument, a regexp, leaving an Fst object on the stack
		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst baseFst = (Fst)(stack.pop()) ;

		// Check and list dependencies (the subnetworks)
		// for the whole implied RTN, keep them in an ArrayList
		// (HashSet would be convenient, but you can't iterate through it
		// and add objects to it during the iteration).  HashSet would keep
		// out duplicates automatically--it's a bit harder with ArrayList.

		ArrayList<String> dependencies = new ArrayList<String>() ;

		// find all subnets referred to by the baseFst
		findDependencies(baseFst, dependencies) ;

		// The order of objects in the ArrayList is constant and starts at index 0

		// Now loop through the ArrayList of dependencies, which is a set (no
		// duplicates) adding new dependencies as they appear
		// (use a for-loop so that the size can grow during iteration--
		// I tried to use HashSet, but this proved impossible)

		String dep ;

		// keep a set of dependencies that are not defined (any
		// undefined dependency is an error)
		HashSet<String> not_defined = new HashSet<String>() ;

		for (int i = 0; i < dependencies.size(); i++) {
			dep = dependencies.get(i) ;

			// Look up the dependency name in the symbol table.
			Fst fst = (Fst) env.get(dep) ;
			if (fst == null) {
				// add it to the set of undefined dependencies
				not_defined.add(dep) ;
				continue ;
		    }
			// find any additional dependencies of this dependency
			findDependencies(fst, dependencies) ;
		}

		// if any dependencies are not defined, throw an exception
		if (!not_defined.isEmpty()) {
			throw new UndefinedIdException("Undefined networks: " +
				not_defined.toString()) ;
		}

		// Reaching here, the whole RTN grammar has been defined.
		// (All the required networks are available in the
		// symbol table.)

		// Create an Fst result that incorporates the subnetworks,
		// unioning them in (with special prefixes) with the base
		// network.

		// EmbeddedRtn is not destructive; copies baseFst
		// if it comes from a symbol table
		Fst resultFst = lib.EmbeddedRtn(baseFst, 
											dependencies,
											"__SUBNETWORKS") ;
		stack.push(resultFst) ;
		return data ;
	}

	// current used only by net_expand_rtn_func, which works only
	// with OpenFstRtnConventions
	private void findSubnetReferences(Fst fst, ArrayList<String> subnetReferences) {
		String subnetReferencePrefix = "__$" ;

		// look for output-side symbols that refer to subnetworks
		// first need to get the output-side sigma

		int[] outputSigmaArray = lib.GetOutputLabels(fst) ;

		String symbolName ;
		String netIdName ;
		for (int j = 0; j < outputSigmaArray.length; j++) {
			symbolName = symmap.getsym(outputSigmaArray[j]) ;
			if (symbolName.startsWith(subnetReferencePrefix)) {
				// strip the prefix
				netIdName = symbolName.substring(symbolName.indexOf("$")) ;
				// avoid duplicates
				if (!subnetReferences.contains(netIdName)) {
					System.out.println("New reference: " + netIdName) ;
					subnetReferences.add(netIdName) ;
				}
			}
		}
	}

	public Object visit(ASTnet_expand_rtn_func_call node, Object data) {

		// syntax  $^expandRtn(regexp)

		if (lib.isSapRtnConventions()) {
			throw new KleeneInterpreterException("$^expandRtn() is not yet implemented under SapRtnConventions") ;
		}

		// evaluate the one argument, a regexp, 
		// leaving an Fst object on the stack
		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst baseFst = (Fst)(stack.pop()) ;

		int baseFstInt ;
		if (baseFst.getFromSymtab()) {
			// then reach down into the AST to get the image
			String net_id = ((ASTnet_id)(node.jjtGetChild(0).jjtGetChild(0))).getImage() ;
			baseFstInt = symmap.putsym("__" + net_id) ;
		} else {
			baseFstInt = -1000000 ;	// KRB: magic number
		}
		
		// a HashSet would be more convenient, but you can't iterate through
		// it and add to it
		ArrayList<String> subnetReferences = new ArrayList<String>() ;

		// find subnet references in the baseFst
		findSubnetReferences(baseFst, subnetReferences) ;


		// The order of objects in the ArrayList is constant and starts at index 0

		// Now loop through the ArrayList of subnetReferences, adding new ones 
		// as they appear
		// (use a for-loop so that the size can grow during iteration--
		// I tried to use HashSet and an Iterator, but this proved impossible)

		// Collect a set of undefined networks (if any)
		HashSet<String> not_defined = new HashSet<String>() ;

		String subnetName ;
		Fst subFst ;

		for (int i = 0; i < subnetReferences.size(); i++) {
			subnetName = subnetReferences.get(i) ;

			// Look up the subnet name in the symbol table.
			subFst = (Fst) env.get(subnetName) ;
			if (subFst == null) {
				// add it to the list
				not_defined.add(subnetName) ;
				continue ;
		    }
			// also look for subnet references in this subnet
			findSubnetReferences(subFst, subnetReferences) ;
		}

		// bail out here, with a useful Exception message, if any
		// of the required subnets are not defined.
		if (!not_defined.isEmpty()) {
			throw new UndefinedIdException("Failed RTN expansion, undefined networks: " +
				not_defined.toString()) ;
		}

		// Reaching here, the whole RTN grammar has been defined.
		// (All the required networks are available in the
		// symbol table.)

		Fst resultFst = lib.ExpandRtn(baseFst, baseFstInt, subnetReferences) ;

		stack.push(resultFst) ;
		return data ;
	}

	public Object visit(ASTdbl_ceil_func_call node, Object data) {
		// just #^__ceil(numexp) built-in
		// should be just one daughter: arg_list with one numexp
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves a Long or Double object on the stack
		Object o = stack.pop() ;

		// Math.ceil() always produces Double
		if (o instanceof Long) {
			stack.push(new Double(Math.ceil(((Long)o).doubleValue()))) ;
		} else {
			stack.push(new Double(Math.ceil(((Double)o).doubleValue()))) ;
		}
		return data;
	}
	public Object visit(ASTdbl_floor_func_call node, Object data) {
		// just #^__floor(numexp) built-in
		// should be just one daughter: arg_list with one numexp
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves a Long or Double object on the stack
		Object o = stack.pop() ;

		// Math.floor() always produces Double
		if (o instanceof Long) {
			stack.push(new Double(Math.floor(((Long)o).doubleValue()))) ;
		} else {
			stack.push(new Double(Math.floor(((Double)o).doubleValue()))) ;
		}
		return data;
	}
	public Object visit(ASTlng_round_func_call node, Object data) {
		// just #^__round(numexp) built-in
		// should be just one daughter: arg_list with one numexp
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves a Long or Double object on the stack
		Object o = stack.pop() ;

		// Math.round() always produces Long from a Double arg
		if (o instanceof Long) {
			stack.push(new Long(Math.round(((Long)o).doubleValue()))) ;
		} else {
			stack.push(new Long(Math.round(((Double)o).doubleValue()))) ;
		}
		return data;
	}
	public Object visit(ASTlng_long_func_call node, Object data) {
		// basically a cast to Long
		// just #^__long(numexp) built-in, wrapped as #^long() and $^int()
		// should be just one daughter: arg_list with one numexp
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves a Long or Double object on the stack
		Object o = stack.pop() ;

		// always produce Long
		if (o instanceof Long) {
			stack.push(new Long(((Long)o).longValue())) ;
		} else {
			stack.push(new Long(((Double)o).longValue())) ;
		}
		return data;
	}
	public Object visit(ASTlng_size_func_call node, Object data) {
		// just #^size(exp) wired-in

		// one daughter
		//		NetList  or NumList

		node.jjtGetChild(0).jjtAccept(this, data) ;
		Object list = stack.pop() ;

		Long size ;

		if (list instanceof NetList) {
			NetList netList = (NetList)list ;
			size = new Long((long)(netList.size())) ;
		} else if (list instanceof NumList) {
			NumList numList = (NumList)list ;
			size = new Long((long)(numList.size())) ;
		} else {
			throw new KleeneArgException("Type problem with argument to $^size") ;
		}

		stack.push(size) ;
		return data;
	}
	public Object visit(ASTlng_is_empty_func_call node, Object data) {
		// one daughter
		//		NetList  or NumList

		node.jjtGetChild(0).jjtAccept(this, data) ;
		Object list = stack.pop() ;

		long size ;

		if (list instanceof NetList) {
			size = (long)(((NetList)list).size()) ;
		} else if (list instanceof NumList) {
			size = (long)(((NumList)list).size()) ;
		} else {
			throw new KleeneArgException("Type problem with argument to $^isEmpty") ;
		}

		if (size == 0L) {
			// isEmpty is true
			stack.push(new Long(1L)) ;
		} else {
			// isEmpty is false
			stack.push(new Long(0L)) ;
		}

		return data;
	}
	public Object visit(ASTdbl_double_func_call node, Object data) {
		// basically a cast to Double
		// just #^__double(numexp) built-in, 
		//		wrapped as #^double() and #^float()
		// should be just one daughter: arg_list with one numexp
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves a Long or Double object on the stack
		Object o = stack.pop() ;

		// always produce Long
		if (o instanceof Long) {
			stack.push(new Double(((Long)o).doubleValue())) ;
		} else {
			stack.push(new Double(((Double)o).doubleValue())) ;
		}
		return data;
	}
	public Object visit(ASTdbl_rint_func_call node, Object data) {
		// just #^__rint(numexp) built-in
		// should be just one daughter: arg_list with one numexp
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leaves a Long or Double object on the stack
		Object o = stack.pop() ;

		// Math.rint(double) always produce Double
		if (o instanceof Long) {
			stack.push(new Double(Math.rint(((Long)o).doubleValue()))) ;
		} else {
			stack.push(new Double(Math.rint(((Double)o).doubleValue()))) ;
		}
		return data;
	}
   	public Object visit(ASTnet_func_exp node, Object data) {
		// daughter should be net_func_id or net_func_anon_exp or
		// net_func_func_call
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// should leave a FuncValue object on the stack
		return data ;
	}
	public Object visit(ASTvoid_func_exp node, Object data) {
		// daughter should be void_func_id or void_func_anon_exp or
		// void_func_func_call
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// should leave a FuncValue object on the stack
		return data ;
	}
    public Object visit(ASTnet_func_id node, Object data) {
		// this is called for net_func_id on the right-hand-side,
		// so it needs to successfully look up the FuncValue value 
		// (or throw
		// an exception); for an assignment, e.g. $^func =
		// this visit method is not called
		String net_func_id = node.getImage() ;
		// value from environment should be a FuncValue object
		Object obj = env.get(net_func_id) ;
		if (obj != null) {
			stack.push(obj) ;
		} else {
			throw new UndefinedIdException("Undefined net_func_id: " + net_func_id) ;
		}
		return data ;
    }
	public Object visit(ASTvoid_func_id node, Object data) {
		// this is called for void_func_id on the right-hand-side,
		// so it needs to successfully look up the value (or throw
		// an exception); for an assignment, e.g. ^func() =
		// this visit method is not called
		String void_func_id = node.getImage() ;
		// value from environment should be a FuncValue object
		Object obj = env.get(void_func_id) ;
		if (obj != null) {
			stack.push(obj) ;
			return data ;
		} else {
			throw new UndefinedIdException("Undefined void_func_id: " + void_func_id) ;
		}
    }
    public Object visit(ASTnet_func_anon_exp node, Object data) {
		// two daughters: param_list, func_block
		// collect them into a new FuncValue object 
		//		and push it on the stack
		// Don't evaluate the func_block, just get the AST itself
		node.jjtGetChild(0).jjtAccept(this, data) ;
		ArrayList<ParamSlot> pal = (ArrayList<ParamSlot>) stack.pop() ;
		// a FuncValue also needs to store a handle to the frame
		//   in which it was defined (to get lexical scoping)
		stack.push(new FuncValue(env.getCurrentFrame(), 
								 pal, 
								 (ASTfunc_block) node.jjtGetChild(1))) ;
		return data ;
    }
    public Object visit(ASTvoid_func_anon_exp node, Object data) {
		// two daughters: param_list func_block
		// collect them into a FuncValue object and push it on the stack
		node.jjtGetChild(0).jjtAccept(this, data) ;
		ArrayList<ParamSlot> pal = (ArrayList<ParamSlot>) stack.pop() ;
		stack.push(new FuncValue(env.getCurrentFrame(), 
								 pal, 
								 (ASTfunc_block) node.jjtGetChild(1))) ;
		return data ;
    }
    public Object visit(ASTnet_func_func_call node, Object data) {
		//  e.g. $^^add_suff(ing) or
		//       $^^(....){...}(ing)
		//  two daughters:  net_func_func_exp  arg_list
		//  N.B. net_func_func_exp could be net_func_func_id or a 
		//	an anonymous function,
		//  so it needs to be evaluated
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// should leave a FuncValue object on the stack
		FuncValue funcValue = (FuncValue) stack.pop() ;
		// now eval the argument list
		node.jjtGetChild(1).jjtAccept(this, data) ;
		// the object on top of the Stack is a count of the 
		//		function arguments
		ArgCounts ac = (ArgCounts) stack.pop() ;

		// allocate a new Frame for the execution of the function call
		env.allocateFrame(funcValue.getStaticFrame()) ;

		// now bind the formal params to the arg values, in the new Frame
		try {
	    	bind_params(funcValue.getParamArrayList(), ac, data) ;
			// may throw FuncCallException, a kind of RuntimeException
			// catch all Exceptions and release the Frame before rethrowing
		} catch (RuntimeException re) {
	    	env.releaseFrame() ;
	    	throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}
		// now execute the body of the function (a func_block)
		try {
			funcValue.getFuncBlock().jjtAccept(this, data) ;
		} catch (RuntimeException re) {
			env.releaseFrame() ;
			throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}

		// normal release of the new Frame created for the 
		//		execution of this func call
		env.releaseFrame() ;

		// Check return value; there should be an FuncValue 
		//		object left on the stack
		// by a return stmt in the funcBlock.
		Object obj = stack.peek() ;
		if (obj == null) {
			throw new FuncCallException("NetFunc valued function call fails to return a net function.") ;
		} else if (!(obj instanceof FuncValue)) {
			throw new FuncCallException("NetFunc valued function call returns incorrect type.") ;
		}
		return data ;
    }
    public Object visit(ASTvoid_func_func_call node, Object data) {
		//  e.g. ^^add_suff(ing) or
		//       ^^(....){...}(ing)
		//  two daughters:  void_func_func_exp  arg_list
		//  N.B. void_func_func_exp could be void_func_func_id or 
		//	an anonymous function,
		//  so it needs to be evaluated
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// should leave a FuncValue object on the stack
		FuncValue funcValue = (FuncValue) stack.pop() ;
		// now eval the argument list
		node.jjtGetChild(1).jjtAccept(this, data) ;
		// the object on top of the Stack is a count of the 
		//		function arguments
		ArgCounts ac = (ArgCounts) stack.pop() ;

		// allocate a new Frame for the execution of the function call
		env.allocateFrame(funcValue.getStaticFrame()) ;

		// now bind the formal params to the arg values, in the new Frame
		try {
	    	bind_params(funcValue.getParamArrayList(), ac, data) ;
			// may throw FuncCallException, a kind of RuntimeException
			// catch all Exceptions and release the Frame before rethrowing
		} catch (RuntimeException re) {
	    	env.releaseFrame() ;
	    	throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}
		// now execute the body of the function (a func_block)
		try {
			funcValue.getFuncBlock().jjtAccept(this, data) ;
		} catch (RuntimeException re) {
			env.releaseFrame() ;
			throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}

		// normal release of the new Frame created for the 
		//		execution of this func call
		env.releaseFrame() ;

		// Check return value; there should be an FuncValue object left on the stack
		// by a return stmt in the funcBlock.
		Object obj = stack.peek() ;
		if (obj == null) {
			throw new FuncCallException("VoidFunc valued function call fails to return a void function.") ;
		} else if (!(obj instanceof FuncValue)) {
			throw new FuncCallException("VoidFunc valued function call returns incorrect type.") ;
		}
		return data ;
    }
    public Object visit(ASTnet_func_func_exp node, Object data) {
		// daughter should be net_func_func_id or net_func_func_anon_exp
		node.jjtGetChild(0).jjtAccept(this, data) ;
		return data ;
    }
    public Object visit(ASTvoid_func_func_exp node, Object data) {
		// daughter should be void_func_func_id 
		//		or void_func_func_anon_exp
		node.jjtGetChild(0).jjtAccept(this, data) ;
		return data ;
    }
    public Object visit(ASTnet_func_func_anon_exp node, Object data) {
		// two daughters: param_list func_block
		node.jjtGetChild(0).jjtAccept(this, data) ;
		ArrayList<ParamSlot> pal = (ArrayList<ParamSlot>) stack.pop() ;
		stack.push(new FuncValue(env.getCurrentFrame(), 
					             pal, 
								 (ASTfunc_block) node.jjtGetChild(1))) ;
		return data ;
    }
	public Object visit(ASTvoid_func_func_anon_exp node, Object data) {
		// two daughters: param_list func_block
		node.jjtGetChild(0).jjtAccept(this, data) ;
		ArrayList<ParamSlot> pal = (ArrayList<ParamSlot>) stack.pop() ;
		stack.push(new FuncValue(env.getCurrentFrame(), 
					             pal, 
								 (ASTfunc_block) node.jjtGetChild(1))) ;
		return data ;
    }
//    public Object visit(ASTarr_index_exp node, Object data) {
//		System.out.println("Interp: ASTarr_index_exp node not implemented.") ;
//		// e.g. (1)
//		return data ;
//    }
//    public Object visit(ASTnet_list_ref node, Object data) {
//		System.out.println("Interp: ASTnet_list_ref node not implemented.") ;
//		return data ;
//    }
    public Object visit(ASTnet_list_exp node, Object data) {
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leave the result (a handle to a NetList) on the stack
		return data ;
    }
    public Object visit(ASTnet_list_id node, Object data) {
		// Called only when the net_list_id is on the RHS (_not_ the LHS)
		// of an assignment statement, e.g.  $@foo

		String net_list_id = node.getImage() ;

		// with the String name, retrieve the NetList from the environment
		NetList netList = (NetList) env.get(net_list_id) ;
		if (netList != null) {
			// The net_list_id was found in a symbol table.
			//
			// NetList is a Java wrapper object around LinkedList<Fst> 
			// that stores handles to a set of Fst objects.
			// Need to mark this
			// Array as being "fromSymtab" if, as here, the value came from a
			// symbol table (and therefore cannot be changed, i.e. must be
			// persistent in case the net_id is referred to again).
			netList.setFromSymtab(true) ;
			// leave the NetList on the stack
			stack.push(netList) ;
		} else {
			// attempt to refer to (use) an undefined variable
			throw new UndefinedIdException("Undefined net_list_id: " + net_list_id) ;
		}
		return data ;
    }
    public Object visit(ASTnet_list_lit node, Object data) {
		// zero or more regexp daughters
		// $@(a, b, $bar) 

		NetList netList = new NetList() ;
		int childcount = node.jjtGetNumChildren() ;

		Fst fst = null ;

		for (int i = 0; i < childcount; i++) {
			node.jjtGetChild(i).jjtAccept(this, data) ;
			fst = (Fst)(stack.pop()) ;
			netList.add(fst) ;
		}
		// leave the result on the stack
		stack.push(netList) ;

		return data ;
    }
    public Object visit(ASTnet_list_func_call node, Object data) {
		// Usual Syntax:  $@^myunion(a, b)    has a NetList value
		//
		//  net_list_func_call always has two daughters:
		//      net_list_func_exp  
		//      arg_list
		//
		//  N.B. net_list_func_exp could be 
		//       net_list_func_id     e.g. $@^myfunc  OR
		//       net_list_func_anon_exp    e.g. $^(...){...}
		//  so net_list_func_exp needs to be evaluated 

		// Evaluate daughter 0, the net_list_func_exp
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// Should leave a FuncValue on the stack
		FuncValue funcValue = (FuncValue) stack.pop() ;

		// now evaluate the arg_list, which can have 0 to 2 daughters
		// arg_list
		//     positional_args
		//     named_args

		// If either daughter is present, it is non-empty.
		// If both daughters are present, 
		//		positional_args is always before named_args

		// N.B. the arguments have to be evaluated in the current Frame,
		// before allocating a new daughter frame for the execution of the
		// function body.  

		// Evaluate daughter 1, the arg_list
		node.jjtGetChild(1).jjtAccept(this, data) ;
		// should leave on the stack, from top down
		// 1.  an ArgCounts object (containing positional_args_count 
		//			and named_args_count)
		// 2.  the positional arguments (in syntactic order), 
		//			the number being positional_args_count
		// 3.  the named arguments (each represented by a NamedArg object), 
		//			the number being named_args_count
		//
		// first pop off the ArgCounts object, 
		//    leaving the evaluated args (if any) on the stack
		ArgCounts ac = (ArgCounts) stack.pop() ;

		//  Now allocate a new Frame for execution of this function call
		//  (N.B. released below when the function returns)
		env.allocateFrame(funcValue.getStaticFrame()) ;

		// now bind the formal params to the arg values, in the new Frame
		try {
	    	bind_params(funcValue.getParamArrayList(), ac, data) ;
			// may throw FuncCallException, a kind of RuntimeException
			// catch all Exceptions and release the frame before rethrowing
		} catch (RuntimeException re) {
	    	env.releaseFrame() ;
	    	throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}
		// now execute the body of the function (a func_block); more precisely,
		// send a message to the function block telling it to accept this 
		// IntepreterVisitor

		try {
			funcValue.getFuncBlock().jjtAccept(this, data) ;
		} catch (RuntimeException re) {
			env.releaseFrame() ;
			throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}

		// normal release of the Frame created for the execution of this func call
		env.releaseFrame() ;

		// Check return value; 
		//	there should be a NetList object left on the stack
		// (this is ASTnet_list_func_call) by a return stmt in the funcBlock.  
		Object obj = stack.peek() ;
		if (obj == null) {
			throw new FuncCallException("Net list valued function call failed to return a net list.") ;
		} else if (!(obj instanceof NetList)) {
			throw new FuncCallException("Net list valued function call returns incorrect type.") ;
		}
		return data ;
    }
    public Object visit(ASTnet_list_func_exp node, Object data) {
		node.jjtGetChild(0).jjtAccept(this, data) ;
		return data ;
    }
    public Object visit(ASTnet_list_func_id node, Object data) {
		// this is called for net_list_func_id on the right-hand-side,
		// so it needs to successfully look up the FuncValue value 
		// (or throw
		// an exception); for an assignment, e.g. $@^func =
		// this visit method is not called on the LHS
		String net_list_func_id = node.getImage() ;
		// value from environment should be a FuncValue object
		Object obj = env.get(net_list_func_id) ;
		if (obj != null) {
			stack.push(obj) ;
		} else {
			throw new UndefinedIdException("Undefined net_list_func_id: " + net_list_func_id) ;
		}
		return data ;
    }
    public Object visit(ASTnet_list_func_anon_exp node, Object data) {
		// two daughters: param_list, func_block
		// collect them into a new FuncValue object 
		//		and push it on the stack
		// Don't evaluate the func_block, just get the AST itself
		node.jjtGetChild(0).jjtAccept(this, data) ;
		ArrayList<ParamSlot> pal = (ArrayList<ParamSlot>) stack.pop() ;
		// a FuncValue also needs to store a handle to the frame
		//   in which it was defined (to get lexical scoping)
		stack.push(new FuncValue(env.getCurrentFrame(), 
								 pal, 
								 (ASTfunc_block) node.jjtGetChild(1))) ;
		return data ;
    }
    public Object visit(ASTnet_list_func_func_anon_exp node, Object data) {
		// two daughters: param_list, func_block
		// collect them into a new FuncValue object 
		//		and push it on the stack
		// Don't evaluate the func_block, just get the AST itself
		node.jjtGetChild(0).jjtAccept(this, data) ;
		ArrayList<ParamSlot> pal = (ArrayList<ParamSlot>) stack.pop() ;
		// a FuncValue also needs to store a handle to the frame
		//   in which it was defined (to get lexical scoping)
		stack.push(new FuncValue(env.getCurrentFrame(), 
								 pal, 
								 (ASTfunc_block) node.jjtGetChild(1))) ;
		return data ;
    }
    public Object visit(ASTnum_list_func_func_anon_exp node, Object data) {
		// two daughters: param_list, func_block
		// collect them into a new FuncValue object 
		//		and push it on the stack
		// Don't evaluate the func_block, just get the AST itself
		node.jjtGetChild(0).jjtAccept(this, data) ;
		ArrayList<ParamSlot> pal = (ArrayList<ParamSlot>) stack.pop() ;
		// a FuncValue also needs to store a handle to the frame
		//   in which it was defined (to get lexical scoping)
		stack.push(new FuncValue(env.getCurrentFrame(), 
								 pal, 
								 (ASTfunc_block) node.jjtGetChild(1))) ;
		return data ;
    }
//    public Object visit(ASTnum_list_ref node, Object data) {
//		System.out.println("Interp: ASTnum_func_ref node not implemented.") ;
//		// e.g.  #@mylist(3)
//		return data ;
//    }
    public Object visit(ASTnum_list_exp node, Object data) {
  		node.jjtGetChild(0).jjtAccept(this, data) ;
		// leave the result (a handle to a NumList) on the stack
		return data ;
    }
    public Object visit(ASTnum_list_id node, Object data) {
		// Called only when the num_list_id is on the RHS (_not_ the LHS)
		// of an assignment statement, e.g.  #@foo

		String num_list_id = node.getImage() ;

		// with the String name, retrieve the NumList from the environment
		NumList numList = (NumList) env.get(num_list_id) ;
		if (numList != null) {
			// The num_list_id was found in a symbol table.
			//
			// NumList is a Java wrapper object around LinkedList<Object> 
			// that stores handles to a set of Long or Double objects.
			// Need to mark this
			// Array as being "fromSymtab" if, as here, the value came from a
			// symbol table (and therefore cannot be changed, i.e. must be
			// persistent in case the net_id is referred to again).
			numList.setFromSymtab(true) ;
			// leave the NumList on the stack
			stack.push(numList) ;
		} else {
			// attempt to refer to (use) an undefined variable
			throw new UndefinedIdException("Undefined num_list_id: " + num_list_id) ;
		}
		return data ;
    }
    public Object visit(ASTnum_list_lit node, Object data) {
		// zero or more numexp daughters
		// #@(1, 2, #num) 

		NumList numList = new NumList() ;
		int childcount = node.jjtGetNumChildren() ;

		Object obj = null ; // will be Long or Double

		for (int i = 0; i < childcount; i++) {
			node.jjtGetChild(i).jjtAccept(this, data) ;
			obj = stack.pop() ;

			// NumList has .add() methods for Long and Double
			if (obj instanceof Long) {
				numList.add((Long)obj) ;
			} else if (obj instanceof Double) {
				numList.add((Double)obj) ; 
			} else {
				throw new KleeneArgException("Illegal value in number list literal.")
				;
			}
		}
		// leave the result on the stack
		stack.push(numList) ;

		return data ;
    }
    public Object visit(ASTnum_list_func_call node, Object data) {
		// Usual Syntax:  #@^__tail(#@arr)    has a NumList value
		//
		//  num_list_func_call always has two daughters:
		//      num_list_func_exp  
		//      arg_list
		//
		//  N.B. num_list_func_exp could be 
		//       num_list_func_id     e.g. #@^myfunc  OR
		//       num_list_func_anon_exp    e.g. #^(...){...}
		//  so num_list_func_exp needs to be evaluated 

		// Evaluate daughter 0, the num_list_func_exp
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// Should leave a FuncValue on the stack
		FuncValue funcValue = (FuncValue) stack.pop() ;

		// now evaluate the arg_list, which can have 0 to 2 daughters
		// arg_list
		//     positional_args
		//     named_args

		// If either daughter is present, it is non-empty.
		// If both daughters are present, 
		//		positional_args is always before named_args

		// N.B. the arguments have to be evaluated in the current Frame,
		// before allocating a new daughter frame for the execution of the
		// function body.  

		// Evaluate daughter 1, the arg_list
		node.jjtGetChild(1).jjtAccept(this, data) ;
		// should leave on the stack, from top down
		// 1.  an ArgCounts object (containing positional_args_count 
		//			and named_args_count)
		// 2.  the positional arguments (in syntactic order), 
		//			the number being positional_args_count
		// 3.  the named arguments (each represented by a NamedArg object), 
		//			the number being named_args_count
		//
		// first pop off the ArgCounts object, 
		//    leaving the evaluated args (if any) on the stack
		ArgCounts ac = (ArgCounts) stack.pop() ;

		//  Now allocate a new Frame for execution of this function call
		//  (N.B. released below when the function returns)
		env.allocateFrame(funcValue.getStaticFrame()) ;

		// now bind the formal params to the arg values, in the new Frame
		try {
	    	bind_params(funcValue.getParamArrayList(), ac, data) ;
			// may throw FuncCallException, a kind of RuntimeException
			// catch all Exceptions and release the frame before rethrowing
		} catch (RuntimeException re) {
	    	env.releaseFrame() ;
	    	throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}
		// now execute the body of the function (a func_block); more precisely,
		// send a message to the function block telling it to accept this 
		// IntepreterVisitor

		try {
			funcValue.getFuncBlock().jjtAccept(this, data) ;
		} catch (RuntimeException re) {
			env.releaseFrame() ;
			throw re ;
		} // catch (Exception e) {
		//	env.releaseFrame() ;
		//	throw e ;
		//}

		// normal release of the Frame created for the execution of this func call
		env.releaseFrame() ;

		// Check return value; 
		//	there should be a NumList object left on the stack
		// (this is ASTnum_list_func_call) by a return stmt in the funcBlock.  
		Object obj = stack.peek() ;
		if (obj == null) {
			throw new FuncCallException("Number list valued function call failed to return a number list.") ;
		} else if (!(obj instanceof NumList)) {
			throw new FuncCallException("Number list valued function call returns incorrect type.") ;
		}
		return data ;
    }
    public Object visit(ASTnum_list_func_exp node, Object data) {
		node.jjtGetChild(0).jjtAccept(this, data) ;
		return data ;
    }
    public Object visit(ASTnum_list_func_id node, Object data) {
		// this is called for num_list_func_id on the right-hand-side,
		// so it needs to successfully look up the FuncValue value 
		// (or throw
		// an exception); for an assignment, e.g. #@^func =
		// this visit method is not called on the LHS
		String num_list_func_id = node.getImage() ;
		// value from environment should be a FuncValue object
		Object obj = env.get(num_list_func_id) ;
		if (obj != null) {
			stack.push(obj) ;
		} else {
			throw new UndefinedIdException("Undefined num_list_func_id: " + num_list_func_id) ;
		}
		return data ;
    }
    public Object visit(ASTnum_list_func_func_id node, Object data) {
		// this is called for num_list_func_func_id on the right-hand-side,
		// so it needs to successfully look up the FuncValue value 
		// (or throw
		// an exception); for an assignment, e.g. #@^func =
		// this visit method is not called on the LHS
		String num_list_func_func_id = node.getImage() ;
		// value from environment should be a FuncValue object
		Object obj = env.get(num_list_func_func_id) ;
		if (obj != null) {
			stack.push(obj) ;
		} else {
			throw new UndefinedIdException("Undefined num_list_func_func_id: " + num_list_func_func_id) ;
		}
		return data ;
    }
    public Object visit(ASTnum_list_func_anon_exp node, Object data) {
		// two daughters: param_list, func_block
		// collect them into a new FuncValue object 
		//		and push it on the stack
		// Don't evaluate the func_block, just get the AST itself
		node.jjtGetChild(0).jjtAccept(this, data) ;
		ArrayList<ParamSlot> pal = (ArrayList<ParamSlot>) stack.pop() ;
		// a FuncValue also needs to store a handle to the frame
		//   in which it was defined (to get lexical scoping)
		stack.push(new FuncValue(env.getCurrentFrame(), 
								 pal, 
								 (ASTfunc_block) node.jjtGetChild(1))) ;
		return data ;
    }
    public Object visit(ASTany node, Object data) {
		// syntax is just  .   (dot)
		stack.push(lib.SigmaFst()) ;
		return data ;
    }
	public Object visit(ASTany_any node, Object data) {
		// syntax is just  .:. (perhaps with whitespace), tokenized
		//   as a single token (may contain whitespace)

		stack.push(lib.SigmaSigmaFst()) ;
		return data ;
	}
	public Object visit(ASTepsilon node, Object data) {
		// syntax is just U+03F5 or _e_
		// denotes the empty string language
		stack.push(lib.EmptyStringLanguageFst()) ;
		return data ;
	}
    public Object visit(ASTarg_list node, Object data) {
		// any positional arguments must appear before any named
		//    arguments (syntactically constrained)
		//
		// arg_list             0 to 2 daughters
		// 	   positional_args
		// 	   		XXX_exp
		// 	   		XXX_exp
		// 	   		...
		// 	   named_args
		// 	       XXX_with_assignment
		// 	       XXX_with_assignment
		// 	       ...
		//
		// if either _args daughter is there, it is non-empty

		int positional_args_count = 0 ;
		int named_args_count = 0 ;

		int childCount = node.jjtGetNumChildren() ;
		if (childCount == 2) {
			// then there are both positional and named args;
			// N.B.  DO THE NAMED ARGS FIRST!! All arg values will be
			// pushed on the stack in the reverse of their syntactic order,
			// so that they can be popped off in the original 
			//		syntactic order.

			// evaluate the named_args first (child 1)
			node.jjtGetChild(1).jjtAccept(this, data) ;
			// leaves an Integer object on top of the stack
			named_args_count = ((Integer)stack.pop()).intValue() ;
			// and it leaves the names-values on the stack in 
			//		NamedArg objects

			// then evaluate the positional_args (child 0)
			node.jjtGetChild(0).jjtAccept(this, data) ;
			// leaves an Integer object on top of the stack
			positional_args_count = ((Integer)stack.pop()).intValue() ;
			// and it leaves the arg value objects on the stack

		} else if (childCount == 1) {
			// then there are positional XOR named args
			Object obj = node.jjtGetChild(0) ;
			if (obj instanceof ASTpositional_args) {
				node.jjtGetChild(0).jjtAccept(this, data) ;
				positional_args_count = ((Integer)stack.pop()).intValue() ;
			} else {
				node.jjtGetChild(0).jjtAccept(this, data) ;
				named_args_count = ((Integer)stack.pop()).intValue() ;
			}
		}
		// leave on top of the stack an ArgCounts object that records the
		// number of positional_args and the number of named_args
		stack.push(new ArgCounts(positional_args_count, named_args_count)) ;
		return data ;
    }
    public Object visit(ASTpositional_args node, Object data) {
		int positional_args_count = node.jjtGetNumChildren() ;
		// The daughters are various kinds of _exp
		//
		// loop backwards through the positional args, pushing the values
		// on the stack (so that they can be popped off in syntactic order)
		for (int i = positional_args_count - 1; i >= 0; i--) {
			node.jjtGetChild(i).jjtAccept(this, data) ;
			// leaves a value object (FST, Long, Double, whatever...) 
			//		on the stack
		}
		// leave on top of the stack the count
		stack.push(new Integer(positional_args_count)) ;
		return data ;
    }
    public Object visit(ASTnamed_args node, Object data) {
		int named_args_count = node.jjtGetNumChildren() ;
		// each daughter is an XXX_with_assignment
		//
		// loop backwards through the named args, pushing NamedArg 
		//		objects on the stack
		// (so that they can be popped off in the original syntactic order)
		//	--IN FACT, I DON'T THINK THE RELATIVE ORDER OF NAMED _ARGS_ 
		//	IS SIGNIFICANT, but I'm
		// pushing all the args on the stack in reverse syntactic order
		for (int i = named_args_count - 1 ; i >= 0 ; i--) {
			((InterpData)data).setIsArg(true) ;
			node.jjtGetChild(i).jjtAccept(this, data) ;
			// should leave a NamedArg object on the stack
		}
		// add on top of the stack the count
		stack.push(new Integer(named_args_count)) ;
		return data ;
    }
    public Object visit(ASTparam_list node, Object data) {
		// param_list            0 to 2 daughters
		//     required_params   (names, no default value indicated)
		//     optional_params   (names with default values indicated)
		//
		// both the required_params and optional_params are optional 
		//	(so param_list may be empty)
		//
		// when a function is defined, it may have required_params 
		//		and/or optional_params
		//  	(the optional params have default values indicated).  
		// The default values, 
		// if any, need to be computed and stored at define time (in an
		// ArrayList<ParamSlot>).

		ArrayList<ParamSlot> pal = new ArrayList<ParamSlot>() ;  
		// this pal will be pushed on the stack
		// will consist of ParamSlot objects, in syntactic order

		// initially assume no params at all
		ASTrequired_params required_params = null ;
		ASToptional_params optional_params = null ;

		int paramListChildCount = node.jjtGetNumChildren() ;

		if (paramListChildCount == 2) {
			// then there is an ASTrequired_params, followed by
			// an ASToptional_params
			required_params = (ASTrequired_params) node.jjtGetChild(0) ;
			optional_params = (ASToptional_params) node.jjtGetChild(1) ;
		} else if (paramListChildCount == 1) {
			Object obj = node.jjtGetChild(0) ;
			if (obj instanceof ASTrequired_params) {
				required_params = (ASTrequired_params) obj ;
			} else {
				optional_params = (ASToptional_params) obj ;
			}
		}

		// required params do not have default values
		if (required_params != null) {
			int required_param_count = required_params.jjtGetNumChildren() ;

			for (int i = 0 ; i < required_param_count ; i++) {
				// process the params in syntactic order
				Object p = required_params.jjtGetChild(i) ;
				// need to get the image and store it in a ParamSlot
				String image = "" ;

				if (p instanceof ASTnet_id) {
					image = ((ASTnet_id)p).getImage() ;
				} else if (p instanceof ASTnet_func_id) {
					image = ((ASTnet_func_id)p).getImage() ;
				} else if (p instanceof ASTnet_func_func_id) {
					image = ((ASTnet_func_func_id)p).getImage() ;

				} else if (p instanceof ASTnet_list_id) {
					image = ((ASTnet_list_id)p).getImage() ;
				} else if (p instanceof ASTnet_list_func_id) {
					image = ((ASTnet_list_func_id)p).getImage() ;
				} else if (p instanceof ASTnet_list_func_func_id) {
					image = ((ASTnet_list_func_func_id)p).getImage() ;

				} else if (p instanceof ASTnum_id) {
					image = ((ASTnum_id)p).getImage() ;
				} else if (p instanceof ASTnum_func_id) {
					image = ((ASTnum_func_id)p).getImage() ;
				} else if (p instanceof ASTnum_func_func_id) {
					image = ((ASTnum_func_func_id)p).getImage() ;

				} else if (p instanceof ASTnum_list_id) {
					image = ((ASTnum_list_id)p).getImage() ;
				} else if (p instanceof ASTnum_list_func_id) {
					image = ((ASTnum_list_func_id)p).getImage() ;
				} else if (p instanceof ASTnum_list_func_func_id) {
					image = ((ASTnum_list_func_func_id)p).getImage() ;

				} else {
					throw new UndefinedIdException("Error checking required params.  See interpreter ASTparam_list") ;
				}

				// add a ParamSlot to the pal (name, but no default value)
				pal.add(new ParamSlot(image)) ;
			}
		}

		if (optional_params != null) {
			// optional params in function definitions have default 
			//	values indicated
			int optional_param_count = optional_params.jjtGetNumChildren() ;
			for (int i = 0 ; i < optional_param_count ; i++) {
				// Optional params are much like assignment statements; 
				//	need to get the assigned ID name (the LHS) as a String,
				//  without actually evaluating it.  
				// Each daughter of optional params has the form
				//
				// XXX_with_assignment
				//     XXX                            child 0
				//     an expression of that type     child 1
				//
				// Each pair (id-value) will be pushed on the stack 
				//		as a ParamSlot object
			
				((InterpData)data).setIsArg(false) ;  
				// arg vs. param, this is a parameter
				optional_params.jjtGetChild(i).jjtAccept(this, data) ;
				// the "param" (not arg) setting tells the method to push a 
				//    ParamSlot rather than a NamedArg

				// add the new ParamSlot to the pal, with the default value
				pal.add((ParamSlot)stack.pop()) ;
			}
		}

		stack.push(pal) ;
		return data ;
	}
   	public Object visit(ASTrequired_params node, Object data) {
		// not called directly
		// see ASTparam_list
		return data ;
	}
	public Object visit(ASToptional_params node, Object data) {
		// not called directly
		// see ASTparam_list 
		return data ;
	}


	// the following with_assignment nodes found both in
	// param_list/optional_params and in arg_list/named_args ;
	// Called with data being InterpData, as usual, with
	// .getIsArg() either true (an argument) or false (a parameter)
	// for .getIsArg() == true push a NamedArg object
	// for .getIsArg() == false push a ParamSlot object

	public Object visit(ASTnet_id_with_assignment node, Object data) {
		String image = ((ASTnet_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		if (((InterpData)data).getIsArg()) {
			stack.push(new NamedArg(image, stack.pop())) ;
		} else {
			stack.push(new ParamSlot(image, stack.pop())) ;
		}
		return data ;
	}
	public Object visit(ASTnet_func_id_with_assignment node, Object data) {
		String image = ((ASTnet_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		if (((InterpData)data).getIsArg()) {
			stack.push(new NamedArg(image, stack.pop())) ;
		} else {
			stack.push(new ParamSlot(image, stack.pop())) ;
		}
		return data ;
	}
	public Object visit(ASTvoid_func_id_with_assignment node, Object data) {
		String image = ((ASTvoid_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		if (((InterpData)data).getIsArg()) {
			stack.push(new NamedArg(image, stack.pop())) ;
		} else {
			stack.push(new ParamSlot(image, stack.pop())) ;
		}
		return data ;
	}
	public Object visit(ASTnet_func_func_id_with_assignment node, 
						Object data) {
		String image = 
			((ASTnet_func_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		if (((InterpData)data).getIsArg()) {
			stack.push(new NamedArg(image, stack.pop())) ;
		} else {
			stack.push(new ParamSlot(image, stack.pop())) ;
		}
		return data ;
	}
	public Object visit(ASTvoid_func_func_id_with_assignment node, 
						Object data) {
		String image = 
			((ASTvoid_func_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		if (((InterpData)data).getIsArg()) {
			stack.push(new NamedArg(image, stack.pop())) ;
		} else {
			stack.push(new ParamSlot(image, stack.pop())) ;
		}
		return data ;
	}
	public Object visit(ASTnet_list_id_with_assignment node, Object data) {
		String image = ((ASTnet_list_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		if (((InterpData)data).getIsArg()) {
			stack.push(new NamedArg(image, stack.pop())) ;
		} else {
			stack.push(new ParamSlot(image, stack.pop())) ;
		}
		return data ;
	}
	public Object visit(ASTnet_list_func_id_with_assignment node, 
						Object data) {
		String image = 
			((ASTnet_list_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		if (((InterpData)data).getIsArg()) {
			stack.push(new NamedArg(image, stack.pop())) ;
		} else {
			stack.push(new ParamSlot(image, stack.pop())) ;
		}
		return data ;
	}
	//public Object visit(ASTnet_list_func_func_id_with_assignment node, Object data) {
	//	String image = ((ASTnet_list_func_func_id)node.jjtGetChild(0)).getImage() ;
	//	node.jjtGetChild(1).jjtAccept(this, data) ;
	//	if (((InterpData)data).getIsArg()) {
	//		stack.push(new NamedArg(image, stack.pop()) ;
	//	} else {
	//		stack.push(new ParamSlot(image, stack.pop()) ;
	//	}
	//	return data ;
	//}

	public Object visit(ASTnum_id_with_assignment node, Object data) {
		String image = ((ASTnum_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		if (((InterpData)data).getIsArg()) {
			stack.push(new NamedArg(image, stack.pop())) ;
		} else {
			stack.push(new ParamSlot(image, stack.pop())) ;
		}
		return data ;
	}
	public Object visit(ASTnum_func_id_with_assignment node, Object data) {
		String image = ((ASTnum_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		if (((InterpData)data).getIsArg()) {
			stack.push(new NamedArg(image, stack.pop())) ;
		} else {
			stack.push(new ParamSlot(image, stack.pop())) ;
		}
		return data ;
	}
	public Object visit(ASTnum_func_func_id_with_assignment node, 
						Object data) {
		String image = ((ASTnum_func_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		if (((InterpData)data).getIsArg()) {
			stack.push(new NamedArg(image, stack.pop())) ;
		} else {
			stack.push(new ParamSlot(image, stack.pop())) ;
		}
		return data ;
	}
	public Object visit(ASTnum_list_id_with_assignment node, Object data) {
		String image = ((ASTnum_list_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		if (((InterpData)data).getIsArg()) {
			stack.push(new NamedArg(image, stack.pop())) ;
		} else {
			stack.push(new ParamSlot(image, stack.pop())) ;
		}
		return data ;
	}
	public Object visit(ASTnum_list_func_id_with_assignment node, 
						Object data) {
		String image = ((ASTnum_list_func_id)node.jjtGetChild(0)).getImage() ;
		node.jjtGetChild(1).jjtAccept(this, data) ;
		if (((InterpData)data).getIsArg()) {
			stack.push(new NamedArg(image, stack.pop())) ;
		} else {
			stack.push(new ParamSlot(image, stack.pop())) ;
		}
		return data ;
	}
	//public Object visit(ASTnum_list_func_func_id_with_assignment node, Object data) {
	//	String image = ((ASTnum_list_func_func_id)node.jjtGetChild(0)).getImage() ;
	//	node.jjtGetChild(1).jjtAccept(this, data) ;
	//	if (((InterpData)data).getIsArg()) {
	//		stack.push(new NamedArg(image, stack.pop()) ;
	//	} else {
	//		stack.push(new ParamSlot(image, stack.pop()) ;
	//	}
	//	return data ;
	//}

	public Object visit(ASTexternal_statement node, Object data) {
		// one or more children, of various _id types
		int numChildren = node.jjtGetNumChildren() ;
		String img = null ;
		for (int i = 0; i < numChildren; i++) {
			Object idobj = node.jjtGetChild(i) ;

			if (idobj instanceof ASTnet_id) {
				img = ((ASTnet_id)idobj).getImage() ;

			} else if (idobj instanceof ASTrrprod_id) {
				img = ((ASTrrprod_id)idobj).getImage() ;

			} else if (idobj instanceof ASTnet_func_id) {
				img = ((ASTnet_func_id)idobj).getImage() ;
			} else if (idobj instanceof ASTnet_func_func_id) {
				img = ((ASTnet_func_func_id)idobj).getImage() ;

			} else if (idobj instanceof ASTnet_list_id) {
				img = ((ASTnet_list_id)idobj).getImage() ;
			} else if (idobj instanceof ASTnet_list_func_id) {
				img = ((ASTnet_list_func_id)idobj).getImage() ;
			} else if (idobj instanceof ASTnet_list_func_func_id) {
				img = ((ASTnet_list_func_func_id)idobj).getImage() ;

			} else if (idobj instanceof ASTnum_id) {
				img = ((ASTnum_id)idobj).getImage() ;
			} else if (idobj instanceof ASTnum_func_id) {
				img = ((ASTnum_func_id)idobj).getImage() ;
			} else if (idobj instanceof ASTnum_func_func_id) {
				img = ((ASTnum_func_func_id)idobj).getImage() ;

			} else if (idobj instanceof ASTnum_list_id) {
				img = ((ASTnum_list_id)idobj).getImage() ;
			} else if (idobj instanceof ASTnum_list_func_id) {
				img = ((ASTnum_list_func_id)idobj).getImage() ;
			} else if (idobj instanceof ASTnum_list_func_func_id) {
				img = ((ASTnum_list_func_func_id)idobj).getImage() ;

			} else if (idobj instanceof ASTvoid_func_id) {
				img = ((ASTvoid_func_id)idobj).getImage() ;
			} else if (idobj instanceof ASTvoid_func_func_id) {
				img = ((ASTvoid_func_func_id)idobj).getImage() ;
			}

			// bind the img to an ExternValue object
			// in the current frame
			env.markExternalInCurrentFrame(img) ;
		}
		return data ;
	}

	public Object visit(ASTexport_statement node, Object data) {
		// one or more children, of various _id types
		int numChildren = node.jjtGetNumChildren() ;
		String img = null ;
		String icon = null ;

		for (int i = 0; i < numChildren; i++) {
			Object idobj = node.jjtGetChild(i) ;

			if (idobj instanceof ASTnet_id) {
				img = ((ASTnet_id)idobj).getImage() ;
				icon = SymtabIcons.NET_IMAGE ;

			} else if (idobj instanceof ASTrrprod_id) {
				img = ((ASTrrprod_id)idobj).getImage() ;
				icon = SymtabIcons.RRPROD_IMAGE ;

			} else if (idobj instanceof ASTnet_func_id) {
				img = ((ASTnet_func_id)idobj).getImage() ;
				icon = SymtabIcons.FUNC_IMAGE ;

			} else if (idobj instanceof ASTnet_func_func_id) {
				img = ((ASTnet_func_func_id)idobj).getImage() ;
				icon = SymtabIcons.FUNC_IMAGE ;

			} else if (idobj instanceof ASTnet_list_id) {
				img = ((ASTnet_list_id)idobj).getImage() ;
				icon = SymtabIcons.FUNC_IMAGE ;	// KRB fix
			} else if (idobj instanceof ASTnet_list_func_id) {
				img = ((ASTnet_list_func_id)idobj).getImage() ;
				icon = SymtabIcons.FUNC_IMAGE ;	// KRB fix
			} else if (idobj instanceof ASTnet_list_func_func_id) {
				img = ((ASTnet_list_func_func_id)idobj).getImage() ;
				icon = SymtabIcons.FUNC_IMAGE ;	// KRB fix

			} else if (idobj instanceof ASTnum_id) {
				img = ((ASTnum_id)idobj).getImage() ;
				icon = SymtabIcons.NUM_IMAGE ;

			} else if (idobj instanceof ASTnum_func_id) {
				img = ((ASTnum_func_id)idobj).getImage() ;
				icon = SymtabIcons.FUNC_IMAGE ;

			} else if (idobj instanceof ASTnum_func_func_id) {
				img = ((ASTnum_func_func_id)idobj).getImage() ;
				icon = SymtabIcons.FUNC_IMAGE ;

			} else if (idobj instanceof ASTnum_list_id) {
				img = ((ASTnum_list_id)idobj).getImage() ;
				icon = SymtabIcons.FUNC_IMAGE ;	// KRB fix

			} else if (idobj instanceof ASTnum_list_func_id) {
				img = ((ASTnum_list_func_id)idobj).getImage() ;
				icon = SymtabIcons.FUNC_IMAGE ;	// KRB fix

			} else if (idobj instanceof ASTnum_list_func_func_id) {
				img = ((ASTnum_list_func_func_id)idobj).getImage() ;
				icon = SymtabIcons.FUNC_IMAGE ;	// KRB fix

			} else if (idobj instanceof ASTvoid_func_id) {
				img = ((ASTvoid_func_id)idobj).getImage() ;
				icon = SymtabIcons.FUNC_IMAGE ;
			} else if (idobj instanceof ASTvoid_func_func_id) {
				img = ((ASTvoid_func_id)idobj).getImage() ;
				icon = SymtabIcons.FUNC_IMAGE ;
			}

			// Find the value bound to img in the currentFrame
			// and "export" that name-value binding to the
			// dynamicMother Frame
			env.exportToDynamicMotherFrame(img) ;

			if (((InterpData)data).getInGUI() == true 
				&& env.getCurrentFrame().getDynamicMother() == mainFrame) {

				// add an Icon to the Symtab window
				addToGUISymtab(img, icon, data) ;
			}
		}
		return data ;
	}

	public Object visit(ASTdelete_statement node, Object data) {
		// one or more children, of various _id types
		int numChildren = node.jjtGetNumChildren() ;
		String img = null ;
		for (int i = 0; i < numChildren; i++) {
			Object idobj = node.jjtGetChild(i) ;

			if (idobj instanceof ASTnet_id) {

				// DON'T TRY TO DELETE THE NATIVE OBJECT HERE; 
				// THERE MAY BE MULTIPLE IDs
				// REFERRING TO THE SAME NATIVE FST

				img = ((ASTnet_id)idobj).getImage() ;

			} else if (idobj instanceof ASTrrprod_id) {
				img = ((ASTrrprod_id)idobj).getImage() ;

			} else if (idobj instanceof ASTnet_func_id) {
				img = ((ASTnet_func_id)idobj).getImage() ;
			} else if (idobj instanceof ASTnet_func_func_id) {
				img = ((ASTnet_func_func_id)idobj).getImage() ;

			} else if (idobj instanceof ASTnet_list_id) {
				img = ((ASTnet_list_id)idobj).getImage() ;
			} else if (idobj instanceof ASTnet_list_func_id) {
				img = ((ASTnet_list_func_id)idobj).getImage() ;
			//} else if (idobj instanceof ASTnet_list_func_func_id) {
			//	img = ((ASTnet_list_func_func_id)idobj).getImage() ;

			} else if (idobj instanceof ASTnum_id) {
				img = ((ASTnum_id)idobj).getImage() ;
			} else if (idobj instanceof ASTnum_func_id) {
				img = ((ASTnum_func_id)idobj).getImage() ;
			} else if (idobj instanceof ASTnum_func_func_id) {
				img = ((ASTnum_func_func_id)idobj).getImage() ;

			} else if (idobj instanceof ASTnum_list_id) {
				img = ((ASTnum_list_id)idobj).getImage() ;
			} else if (idobj instanceof ASTnum_list_func_id) {
				img = ((ASTnum_list_func_id)idobj).getImage() ;
			} else if (idobj instanceof ASTnum_list_func_func_id) {
				img = ((ASTnum_list_func_func_id)idobj).getImage() ;

			} else if (idobj instanceof ASTvoid_func_id) {
				img = ((ASTvoid_func_id)idobj).getImage() ;
			} else if (idobj instanceof ASTvoid_func_func_id) {
				img = ((ASTvoid_func_func_id)idobj).getImage() ;
			}

			Frame foundFrame = null ;
			foundFrame= env.remove(img) ; // frame where img was
												// found and removed
			if (foundFrame == mainFrame) {
				// Returns the Frame where a real key-object entry was
				// removed, else null
				// if the GUI is active, remove the icon from the GUI symtab window
				if (((InterpData)data).getInGUI()) {
					removeFromGUISymtab(img, data) ;
				}
			}
		}
		return data ;
	}
	public Object visit(ASTdelete_all_statement node, Object data) {
		// no children
		// find all networks, productions, etc. defined in 
		// currentFrame and delete them

		Frame currentFrame = env.getCurrentFrame() ;
		HashSet<String> keySet = currentFrame.keySet() ;

		boolean inGUI = false ;
		PseudoTerminalInternalFrame terminal = null ;

		if (((InterpData)data).getInGUI() == true) { 
			inGUI = true ;
			terminal =  ((InterpData)data).getGUI().getTerminal() ;
		} 
		
		for (Iterator<String> i = keySet.iterator() ; i.hasNext(); ) {
			String key = i.next() ;

			Object obj = currentFrame.get(key) ;

			// don't try to delete entries where the value is
			// a FreeVariable, indicating that the key would
			// looked up in this scope as a free variable.
			if (!(obj instanceof FreeVariable)) {

				Frame foundFrame = null ;
				foundFrame = env.remove(key) ;

				if (foundFrame == mainFrame) {
					if (inGUI) {
						removeFromGUISymtab(key, data) ;
					}
				}
				if (inGUI) {
					terminal.appendToHistory("// Deleting " + key) ;
				}
			}
		}
		return data ;
	}
	public Object visit(ASToptimize_statement node, Object data) {
		// syntax:   optimize  $foo, $bar ... ;
		//     or    optimize! $foo, $bar ... ;
		// (optimization is always done in place)
		// one or more children, all of type net_id(), syntactically constrained
		int numChildren = node.jjtGetNumChildren() ;
		for (int i = 0; i < numChildren; i++) {
			node.jjtGetChild(i).jjtAccept(this, data) ;
			// look up the net_id, leaves handle to an Fst object on the stack
			Fst fst = (Fst)stack.pop() ;
			// force the optimization, even if user has set
			// default optimization (or parts: determinize, minimize,
			// rmepsilon) to false
			lib.OptimizeInPlaceForce(fst) ;	
		}
		return data ;
	}

	private void runGC(boolean inGUI, 
					   PseudoTerminalInternalFrame terminal) {
		Runtime runtime = Runtime.getRuntime() ;

		int gcIterations = 12 ; // KRB: magic number
		for (int i = 0; i < gcIterations; i++) {
			if (inGUI) {
				terminal.appendToHistory("// Outer Iteration") ;
				terminal.appendToHistory("// Before      After") ;
			} else {
				System.out.println("// Outer Iteration") ;
				System.out.println("// Before      After") ;
			}
			_runGC(inGUI, terminal, runtime) ;
		}
	}
	private long getMemInUse(Runtime runtime) {
		return runtime.totalMemory() - runtime.freeMemory() ;
	}
	private void _runGC(boolean inGUI, 
						PseudoTerminalInternalFrame terminal, 
						Runtime runtime) {
		long memInUseAfter = getMemInUse(runtime), memInUseBefore = Long.MAX_VALUE ;
		for (int j = 0; 
		     (memInUseAfter < memInUseBefore) && (j < 10);  // KRB: magic number
			 j++
			) {
			runtime.runFinalization() ;
			runtime.gc() ;
			Thread.currentThread().yield() ;

			memInUseBefore = memInUseAfter ;
			memInUseAfter = getMemInUse(runtime) ;
			if (inGUI) {
				terminal.appendToHistory(memInUseBefore + "    " + memInUseAfter) ;
			} else {
				System.out.println(memInUseBefore + "    " + memInUseAfter) ;
			}
		}
	}		
	public Object visit(ASTgarbage_collect_statement node, Object data) {
		boolean inGUI = ((InterpData)data).getInGUI() ;
		PseudoTerminalInternalFrame terminal = null ;
		if (inGUI) { 
			terminal = ((InterpData)data).getGUI().getTerminal() ;
			terminal.appendToHistory("// Suggesting Java garbage collection") ;
		}

		runGC(inGUI, terminal) ;

		return data ;
	}
	public Object visit(ASTmemory_report_statement node, Object data) {
		Runtime r = Runtime.getRuntime() ;
		long max = r.maxMemory() ;
		long total = r.totalMemory() ;
		long free = r.freeMemory() ;
		long inuse = total - free ;

		if (((InterpData)data).getInGUI() == true) { 
			PseudoTerminalInternalFrame terminal =  ((InterpData)data).getGUI().getTerminal() ;

			terminal.appendToHistory("// " + max + " max memory that Java will try to use") ;
			terminal.appendToHistory("// " + total + " total memory") ;
			terminal.appendToHistory("// " + free + " free") ;
			terminal.appendToHistory("// " + inuse + " in use (total - free)" ) ;
		} else {
			System.out.println("// " + max + " max memory that Java will try to use") ;
			System.out.println("// " + total + " total memory") ;
			System.out.println("// " + free + " free memory") ;
			System.out.println("// " + inuse + " memory in use (total - free)" ) ;
		}
		return data ;
	}
	public Object visit(ASTfsts_report_statement node, Object data) {
		int fstsAllocated = Fst.getCountOfFstsAllocated() ;
		int callsToFinalize = Fst.getCountOfCallsToFinalize() ;
		int fstsFinalized = Fst.getCountOfFstsFinalized() ;
		int fstsOpen = fstsAllocated - fstsFinalized ;

		if (((InterpData)data).getInGUI() == true) { 
			PseudoTerminalInternalFrame terminal =  ((InterpData)data).getGUI().getTerminal() ;

			terminal.appendToHistory("// " + fstsAllocated + " allocated") ;
			terminal.appendToHistory("// " + callsToFinalize + " calls to finalize()") ;
			terminal.appendToHistory("// " + fstsFinalized + " finalized") ;
			terminal.appendToHistory("// " + fstsOpen      + " open") ;
		} else {
			System.out.println("// " + fstsAllocated + " allocated") ;
			System.out.println("// " + callsToFinalize + " calls to finalize()") ;
			System.out.println("// " + fstsFinalized + " finalized") ;
			System.out.println("// " + fstsOpen      + " open") ;
		}
		return data ;
	}
	public Object visit(ASTsymtab_report_statement node, Object data) {
		HashSet<String> hs = mainFrame.keySet() ; 
		Iterator<String> i = hs.iterator() ;

		PseudoTerminalInternalFrame terminal = null ;
		if (((InterpData)data).getInGUI() == true) {
			terminal = ((InterpData)data).getGUI().getTerminal() ;
		}

		while(i.hasNext()) {
			String id = i.next() ;
			if (terminal != null) {
				terminal.appendToHistory("// " + id) ;
			} else {
				System.out.println("// " + id) ;
			}
		}
		return data ;
	}
	public Object visit(ASTgsymtab_report_statement node, Object data) {
		HashSet<String> hs = mainFrame.getStaticMother().keySet() ; 
		Iterator<String> i = hs.iterator() ;

		PseudoTerminalInternalFrame terminal = null ;
		if (((InterpData)data).getInGUI() == true) {
			terminal = ((InterpData)data).getGUI().getTerminal() ;
		}

		while(i.hasNext()) {
			String id = i.next() ;
			if (terminal != null) {
				terminal.appendToHistory("// " + id) ;
			} else {
				System.out.println("// " + id) ;
			}
		}
		return data ;
	}

	// see also ASTnet_rmepsilon_func_call
	public Object visit(ASTrmEpsilon_statement node, Object data) {
		// one or more children, all net_id()s
		// syntax:  rmEpsilon  regex [(comma)? regex]* ;
		// These rmEpsilon operations are to be done In Place
		int numChildren = node.jjtGetNumChildren() ;
		
		for (int i = 0; i < numChildren; i++) {
			node.jjtGetChild(i).jjtAccept(this, data) ;
			Fst fst = (Fst) stack.pop() ;
			lib.RmEpsilonInPlace(fst) ;
		}
		return data ;
	}

	// see also ASTnet_determinize_func_call

	// BOTH ARE DANGEROUS -- could be called on networks that are
	// not determinizable

	public Object visit(ASTdeterminize_statement node, Object data) {
		// one or more children, net_id()s
		// syntax:  determinize  regex [(comma)? regex]* ;
		// or       determinize! ...
		// typically:  determinize $net ;
		// These determinize operations are to be done In Place, so that any
		//   other aliases to $net also point to the determinized result.
		int numChildren = node.jjtGetNumChildren() ;
		
		for (int i = 0; i < numChildren; i++) {
			node.jjtGetChild(i).jjtAccept(this, data) ;
			Fst fst = (Fst) stack.pop() ;

			lib.DeterminizeInPlace(fst) ;
		}
		return data ;
    }

	// see also ASTnet_minimize_func_call

	public Object visit(ASTminimize_statement node, Object data) {
		// one or more children, net_id()s
		// syntax:  minimize  regex [(comma)? regex]* ;
		// These minimize operations are to be done In Place
		int numChildren = node.jjtGetNumChildren() ;

		boolean inGUI = false ;
		PseudoTerminalInternalFrame terminal = null ;

		if (((InterpData)data).getInGUI() == true) { 
			inGUI = true ;
			terminal =  ((InterpData)data).getGUI().getTerminal() ;
		} 
		
		for (int i = 0; i < numChildren; i++) {
			ASTnet_id obj = (ASTnet_id) node.jjtGetChild(i) ;
			String img = obj.getImage() ;
			obj.jjtAccept(this, data) ;
			Fst fst = (Fst) stack.pop() ;
			if (!lib.IsIDeterministic(fst)) {
				String warningMsg = "// WARNING: The network argument to be minimized, " + img + 
				", must first be determinized." ;
 
				if (inGUI) {
					terminal.appendToHistory(warningMsg) ;
				} else {
					System.out.println(warningMsg) ;
				}
			} else {
				// the network is deterministic, so it can be minimized
				lib.MinimizeInPlace(fst) ;
			}
		}
		return data ;
    }

	public Object visit(ASTsynchronize_statement node, Object data) {
		// one or more children, net_id()s
		// syntax:  synchronize  regex [(comma)? regex]* ;
		// or       synchronize! ...
		// typically:  synchronize $net ;
		// These synchronize operations are to be done In Place, so that any
		//   other aliases to $net also point to the synchronized result.
		int numChildren = node.jjtGetNumChildren() ;
		
		for (int i = 0; i < numChildren; i++) {
			node.jjtGetChild(i).jjtAccept(this, data) ;
			Fst fst = (Fst) stack.pop() ;

			lib.SynchronizeInPlace(fst) ;
		}
		return data ;
    }

	public Object visit(ASTinfo_statement node, Object data) {
		// one daughter:  some kind of expression, (output to the GUI) 
		// or two:        some kind of expression, filepath
		//                    (output to file, default encoding of
		//                    the OS)
		// or three:      some kind of expression, filepath, encoding
		//						(output to file, user-specified
		//						encoding)

		int numChildren = node.jjtGetNumChildren() ;

		String filePath = "" ;
		String encoding = "-" ;  // OS default

		if (numChildren >= 2) {
			// set the filepath for the output
			// Child #1 should indicate the file name
			node.jjtGetChild(1).jjtAccept(this, data) ;
			Fst filePathFst = (Fst)(stack.pop()) ;

			filePath = lib.GetSingleString(filePathFst, "Second arg to info (filepath) must denote a language of exactly one string.") ;

			if (filePath.length() == 0) {
				throw new KleeneArgException("Second arg to info (filepath) must denote a language of exactly one non-empty string") ;
			}

		}

		if (numChildren == 3) {
			// set the encoding
			// Child #2 should specify the encoding
			node.jjtGetChild(2).jjtAccept(this, data) ;
			Fst encodingFst = (Fst)(stack.pop()) ;

			encoding = lib.GetSingleString(encodingFst, "Third arg to info (encoding) must denote a language of exactly one string.") ;

			if (encoding.length() == 0) {
				throw new KleeneArgException("Third arg to info (encoding) must denote a non-empty string") ;
			}

		}

		InfoWriter infoWriter = null ;
		// InfoWriter Java class knows how to write output,
		// takes care of the encoding

		if (numChildren >= 2) {
			String fullpath = getFullpath(filePath) ;

			if (encoding.equals("-")) {  // get current OS encoding
				encoding = System.getProperty("file.encoding") ;
			}
			infoWriter = new InfoWriter(new File(fullpath), encoding) ;
		}

		// if the output is to file, or we are in the GUI (i.e. just one daughter)
		// (else it's in a script, with no output file indicated, 
		//    and output makes no sense)

		boolean inGUI = ((InterpData)data).getInGUI() ;


			// Get the object for which info is desired
			node.jjtGetChild(0).jjtAccept(this, data) ;
			// Should leave object on the stack
			Object obj = stack.pop() ;
		
			PseudoTerminalInternalFrame terminal = null ;
			if (numChildren == 1 && inGUI) {
				// output to the GUI terminal
				terminal = ((InterpData)data).getGUI().getTerminal() ;
			}

			String infoString = "" ;

			if (obj instanceof Fst) {
				Fst fst = (Fst)obj ; 
				// see Java func basicFstInfo() above
				infoString = "// " + basicFstInfo(fst) ;
			} else if (obj instanceof Long) {
				infoString = "// Long value: " + ((Long)obj).longValue() ;
			} else if (obj instanceof Double) {
				infoString = "// Double value: " + ((Double)obj).doubleValue() ;
			} else if (obj instanceof NetList) {
				infoString = "// Network list value: " + "Size: " + ((NetList)obj).size() ;
			} else if (obj instanceof NumList) {
				infoString = "// Number list value: " + "Size: " + ((NumList)obj).size() ;
			} else {
				infoString = "// No info display implemented yet for this datatype." ;
			}

			if (numChildren == 1) {
				if (inGUI) {
					terminal.appendToHistory(infoString) ;
				} else {
					System.out.println(infoString) ;
				}
			} else {
				infoWriter.writeLine(infoString) ;
			}

		if (infoWriter != null) {
			infoWriter.close() ;
		}

		return data ;
	}

	public Object visit(ASTtest_statement node, Object data) {
		// syntax:  test $foo ;
		//			test a*b+[d-g]? ;
		//			test $foo, "$foo" ;
		// the first required daughter, any ASTregexp, is the
		//		network to be tested
		// test makes no sense outside the GUI

		if (((InterpData)data).getInGUI()) {
			// evaluate the zeroth child (the regexp)
			node.jjtGetChild(0).jjtAccept(this, data) ;
			// should leave an Fst object on the stack
			Fst fst = (Fst)(stack.pop()) ;

			// the second daughter, the test-window title, is optional
			String title ;
			if (node.jjtGetNumChildren() == 2) {
				// then a title was specified for the test window
				node.jjtGetChild(1).jjtAccept(this, data) ;
				Fst pathFst = (Fst)(stack.pop()) ;

				title = lib.GetSingleString(pathFst, "Second arg to test must denote a language of exactly one string.") ;
			} else {
				title = "Anonymous Fst" ;
			}

			// TranslitTokenizerBuilder is a class that knows how to make 
			//		ICU4J Transliterators to tokenize a raw input string 
			//		(including finding user-defined multichar symbols); 
			// In the Xerox/PARC tradition, separate Transliterators are 
			//		made for the upper ("input" for OpenFst) and 
			// 		lower ("output" for OpenFst) sides

			TranslitTokenizerBuilder ttb = 
					new TranslitTokenizerBuilder(symmap, fst.getSigma(), lib) ;

			// Iterate4mcs (knows how to iterate 
			//		through the Fst)
			// Calls back to 	.registerMcsInput() or 
			//					.registerMcsOutput() method 
			// 	in the TranslitTokenizerBuilder ttb for each new 
			//	Multichar Symbol found on the input or output side, 
			//	respectively.  It needs to know the SymMap's
			// 	.getStartPuaCpv() so that it knows what a multichar
			//	symbol is (in the network being visited, the labels
			//	are just integers)

			lib.Iterate4mcs(fst, ttb, symmap.getStartPuaCpv()) ;

			boolean inputSide = true ;
			boolean outputSide = false ;

			// used to tokenize string input for generation 
			// (matched against the upper/"input" side)
			// 'true' arg means to consider only the multi-char 
			//		symbols on the Input Side
			Transliterator trInput = ttb.getTranslitTokenizer(inputSide) ;  

			// used to tokenize string input for analysis (matched against 
			//    the lower/"output" side)
			// 'false' arg means to consider only multi-char symbols 
			//		on the Output Side
			Transliterator trOutput = ttb.getTranslitTokenizer(outputSide) ;  

			// display special JInternalFrame where the user can type 
			//		in input for testing
			TestFstInternalFrame tfif = 
				new TestFstInternalFrame(title, 
						env, trInput, trOutput, symmap, fst, this, data) ;

			KleeneGUI g = ((InterpData)data).getGUI() ;

			g.showTestFstInternalFrame(tfif) ;
			tfif.moveToFront() ; 

		}
		return data ;  
	}
	public Object visit(ASTassert_statement node, Object data) {
		// daughters:
		// 		numexp()		// boolean, required
		// 		regexp()		// optional, should denote a single-string language

		// evaluate the numexp(), interpret as boolean (true or false)
		// if not true, then throw an AssertException
		node.jjtGetChild(0).jjtAccept(this, data) ;
		if (!lib.isTrue(stack.pop())) {
			String msg = "" ;
			if (node.jjtGetNumChildren() == 2) {
				node.jjtGetChild(1).jjtAccept(this, data) ;
				Fst msgFst = (Fst)(stack.pop()) ;
				msg = lib.GetSingleString(msgFst, "Second arg to assert() must denote a language of exactly one string") ;
			}
			throw new AssertException(msg) ;
		}
		return data ;
	}
	public Object visit(ASTrequire_statement node, Object data) {
		// daughters:
		// 		numexp()		// boolean, required
		// 		regexp()		// optional, should denote a single-string language

		// evaluate the numexp(), interpret as boolean (true or false)
		// if not true, then throw an AssertException
		node.jjtGetChild(0).jjtAccept(this, data) ;
		if (!lib.isTrue(stack.pop())) {
			String msg = "" ;
			if (node.jjtGetNumChildren() == 2) {
				node.jjtGetChild(1).jjtAccept(this, data) ;
				Fst msgFst = (Fst)(stack.pop()) ;
				msg = lib.GetSingleString(msgFst, "Second arg to require() must denote a language of exactly one string") ;
			}
			throw new RequireException(msg) ;
		}
		return data ;
	}
	public Object visit(ASTprint_statement node, Object data) {
		// the first required daughter, an ASTregexp; this is the 
		//		net to list
		// second optional daughter is the separator to print after 
		//		each string

		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst fst = (Fst)(stack.pop()) ;

		if (!lib.IsAcceptor(fst)) {
			throw new KleeneArgException("First arg to print must denote a language, not a relation.") ;
		}

		String sepString = "\n" ;  // default newline separator
		if (node.jjtGetNumChildren() == 2) {
			node.jjtGetChild(1).jjtAccept(this, data) ;
			Fst sepFst = (Fst)(stack.pop()) ;

			sepString = lib.GetSingleString(sepFst, "Second arg to print must denote a language of exactly one string.") ;
		} 

		int displayLimit = 100 ;

		long stringCount = lib.NumPaths(fst) ;
		PseudoTerminalInternalFrame terminal = null ;
		if (((InterpData)data).getInGUI()) {
			terminal = ((InterpData)data).getGUI().getTerminal() ;
		}

		if (stringCount == 0) {
			String msg = "(language is empty)" ;
			if (terminal != null) {
				terminal.appendToHistory(msg) ;
			} else {
				System.out.println(msg) ;
			}
		} else if (stringCount == -1) {
			// then has loops, infinite language
			String msg = "(language is infinite)" ;
			if (terminal != null) {
				terminal.appendToHistory(msg) ;
			} else {
				System.out.println(msg) ;
			}
		} else if (stringCount <= displayLimit) {
			// then just list them (parameterize this figure later)
			if (terminal != null) {
				FstStringLister lister = new FstStringLister(terminal, symmap) ;
				// native function;  second arg 0 is for input side, 1 for output side
				lib.ListAllStrings(fst, 0, lister) ; 
			} else {
				FstSystemStringLister sysLister = new FstSystemStringLister(symmap, sepString) ;
				lib.ListAllStrings(fst, 0, sysLister) ;
			}
		} else {
			String msg = "(language exceeds displayLimit: " + displayLimit + ")" ;
			if (terminal != null) {
				terminal.appendToHistory(msg) ;
			} else {
				System.out.println(msg) ;
			}
		}

		return data ;
	}

	public Object visit(ASTexception_statement node, Object data) {
		// one required daughter, an ASTregexp; this is the 
		//		exception message (it must denote a language of one
		//		string)

		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst fst = (Fst)(stack.pop()) ;

		try {
			String excMsg = lib.GetSingleString(fst, 
			"The message to an exception statement must denote a language of exactly one string.") ;
			throw new KleeneInterpreterException(excMsg) ;
		} catch (Exception exc) {
			exc.printStackTrace() ;
			return data ;
		}
	}

	public Object visit(ASTprintln_statement node, Object data) {
		// one daughter, an ASTregexp; this is the net to list

		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst fst = (Fst)(stack.pop()) ;

		if (!lib.IsAcceptor(fst)) {
			throw new KleeneArgException("First arg to print must denote a language, not a relation.") ;
		}

		String sepString = "\n" ;
		
		int displayLimit = 100 ;

		long stringCount = lib.NumPaths(fst) ;
		PseudoTerminalInternalFrame terminal = null ;
		if (((InterpData)data).getInGUI()) {
			terminal = ((InterpData)data).getGUI().getTerminal() ;
		}

		if (stringCount == 0) {
			String msg = "(language is empty)" ;
			if (terminal != null) {
				terminal.appendToHistory(msg) ;
			} else {
				System.out.println(msg) ;
			}
		} else if (stringCount == -1) {
			// then has loops, infinite language
			String msg = "(language is infinite)" ;
			if (terminal != null) {
				terminal.appendToHistory(msg) ;
			} else {
				System.out.println(msg) ;
			}
		} else if (stringCount <= displayLimit) {
			// then just list them (parameterize this figure later)
			if (terminal != null) {
				FstStringLister lister = new FstStringLister(terminal, symmap) ;
				// native function;  second arg 0 is for input side, 1 for output side
				lib.ListAllStrings(fst, 0, lister) ; 
			} else {
				FstSystemStringLister sysLister = new FstSystemStringLister(symmap, sepString) ;
				lib.ListAllStrings(fst, 0, sysLister) ;
			}
		} else {
			String msg = "(language exceeds displayLimit: " + displayLimit + ")" ;
			if (terminal != null) {
				terminal.appendToHistory(msg) ;
			} else {
				System.out.println(msg) ;
			}
		}
		return data ; 
	}
	public Object visit(ASTsys_print_statement node, Object data) {
		// the first required daughter, an ASTregexp; this is the 
		//		net to list
		// second optional daughter is the separator to print after 
		//		each string

		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst fst = (Fst)(stack.pop()) ;

		if (!lib.IsAcceptor(fst)) {
			throw new KleeneArgException("First arg to print must denote a language, not a relation.") ;
		}

		String sepString = "\n" ;
		if (node.jjtGetNumChildren() == 2) {
			node.jjtGetChild(1).jjtAccept(this, data) ;
			Fst sepFst = (Fst)(stack.pop()) ;

			sepString = lib.GetSingleString(sepFst, "Second arg to print must denote a language of exactly one string.") ;
		} 

		int displayLimit = 100 ;

		long stringCount = lib.NumPaths(fst) ;

		if (stringCount == 0) {
			String msg = "(language is empty)" ;
			System.out.println(msg) ;
		} else if (stringCount == -1) {
			// then has loops, infinite language
			String msg = "(language is infinite)" ;
			System.out.println(msg) ;
		} else if (stringCount <= displayLimit) {
			// then just list them (parameterize this figure later)
			FstSystemStringLister sysLister = new FstSystemStringLister(symmap, sepString) ;
			lib.ListAllStrings(fst, 0, sysLister) ;
		} else {
			String msg = "(language exceeds displayLimit: " + displayLimit + ")" ;
			System.out.println(msg) ;
		}

		return data ;
	}

	public Object visit(ASTsys_println_statement node, Object data) {
		// one daughter, an ASTregexp; this is the net to list

		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst fst = (Fst)(stack.pop()) ;

		if (!lib.IsAcceptor(fst)) {
			throw new KleeneArgException("First arg to print must denote a language, not a relation.") ;
		}

		String sepString = "\n" ;
		
		int displayLimit = 100 ;

		long stringCount = lib.NumPaths(fst) ;

		if (stringCount == 0) {
			String msg = "(language is empty)" ;
			System.out.println(msg) ;
		} else if (stringCount == -1) {
			// then has loops, infinite language
			String msg = "(language is infinite)" ;
			System.out.println(msg) ;
		} else if (stringCount <= displayLimit) {
			// then just list them (parameterize this figure later)
			FstSystemStringLister sysLister = new FstSystemStringLister(symmap, sepString) ;
			lib.ListAllStrings(fst, 0, sysLister) ;
		} else {
			String msg = "(language exceeds displayLimit: " + displayLimit + ")" ;
			System.out.println(msg) ;
		}
		return data ; 
	}

	public Object visit(ASTtestTokensTextFile_statement node, Object data) {
		// Total: 11 regexp arguments, syntactically constrained
		// 
		// 0.  the Fst to test

		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst testFst = (Fst)(stack.pop()) ;

		// 1.  path of the input file

		node.jjtGetChild(1).jjtAccept(this, data) ;
		Fst tempFst = (Fst)(stack.pop()) ;

		String inputFilePath = lib.GetSingleString(tempFst, 
			"Second arg to testTokensTextFile must denote a language of exactly one string.") ;

		if (inputFilePath.length() == 0) {
			throw new KleeneArgException("Second arg to testTokensTextFile must denote a language of exactly one non-empty string") ;
		}

		// 2.  encoding of the input file

		node.jjtGetChild(2).jjtAccept(this, data) ;
		tempFst = (Fst)(stack.pop()) ;

		String inputFileEncoding = lib.GetSingleString(tempFst, "Third arg to testTokensTextFile must denote a language of exactly one string.") ;

		if (inputFileEncoding.length() == 0) {
			throw new KleeneArgException("Third arg to testTokensTextFile must denote one non-empty string") ;
		}

		// 3.  path of the output file

		node.jjtGetChild(3).jjtAccept(this, data) ;
		tempFst = (Fst)(stack.pop()) ;

		String outputFilePath= lib.GetSingleString(tempFst, "Fourth arg to testTokensTextFile must denote a language of exactly one string.") ;

		if (outputFilePath.length() == 0) {
			throw new KleeneArgException("Fourth arg to testTokensTextFile must denote one non-empty string") ;
		}

		// 4.  encoding of the output file

		node.jjtGetChild(4).jjtAccept(this, data) ;
		tempFst = (Fst)(stack.pop()) ;

		String outputFileEncoding = lib.GetSingleString(tempFst, "Fifth arg to testTokensTextFile must denote a language of exactly one string.") ;

		if (outputFileEncoding.length() == 0) {
			throw new KleeneArgException("Fifth arg to testTokensTextFile must denote one non-empty string") ;
		}

		//    		And for the XML output

		// 5.  name of the root element

		node.jjtGetChild(5).jjtAccept(this, data) ;
		tempFst = (Fst)(stack.pop()) ;

		String rootElmtName = lib.GetSingleString(tempFst, "Sixth arg to testTokensTextFile must denote a language of exactly one string.") ;

		if (rootElmtName.length() == 0) {
			throw new KleeneArgException("Sixth arg to testTokensTextFile must denote one non-empty string") ;
		}

		// 6.  name of the token element

		node.jjtGetChild(6).jjtAccept(this, data) ;
		tempFst = (Fst)(stack.pop()) ;

		String tokenElmtName = lib.GetSingleString(tempFst, "Seventh arg to testTokensTextFile must denote a language of exactly one string.") ;
		
		if (tokenElmtName.length() == 0) {
			throw new KleeneArgException("Seventh arg to testTokensTextFile must denote one non-empty string") ;
		}

		// 7.  name of the input element

		node.jjtGetChild(7).jjtAccept(this, data) ;
		tempFst = (Fst)(stack.pop()) ;

		String inputElmtName = lib.GetSingleString(tempFst, "Eighth arg to testTokensTextFile must denote a language of exactly one string.") ;
		
		if (inputElmtName.length() == 0) {
			throw new KleeneArgException("Eighth arg to testTokensTextFile must denote one non-empty string") ;
		}

		// 8.  name of the outputs element (N.B. plural)

		node.jjtGetChild(8).jjtAccept(this, data) ;
		tempFst = (Fst)(stack.pop()) ;

		String outputsElmtName = lib.GetSingleString(tempFst, "Ninth arg to testTokensTextFile must denote a language of exactly one string.") ;
		
		if (outputsElmtName.length() == 0) {
			throw new KleeneArgException("Ninth arg to testTokensTextFile must denote one non-empty string") ;
		}

		// 9.  name of the output element  (N.B. singular)

		node.jjtGetChild(9).jjtAccept(this, data) ;
		tempFst = (Fst)(stack.pop()) ;

		String outputElmtName = lib.GetSingleString(tempFst, "Tenth arg to testTokensTextFile must denote a language of exactly one string.") ;
		
		if (outputElmtName.length() == 0) {
			throw new KleeneArgException("Tenth arg to testTokensTextFile must denote one non-empty string") ;
		}

		// 10.  name of the weight attr in the output elmt

		node.jjtGetChild(10).jjtAccept(this, data) ;
		tempFst = (Fst)(stack.pop()) ;

		String weightAttrName = lib.GetSingleString(tempFst, "Eleventh arg to testTokensTextFile must denote a language of exactly one string.") ;
		
		if (weightAttrName.length() == 0) {
			throw new KleeneArgException("Eleventh arg to testTokensTextFile must denote one non-empty string") ;
		}

		String fullpath = getFullpath(inputFilePath) ;

		TranslitTokenizerBuilder ttb = new TranslitTokenizerBuilder(symmap, testFst.getSigma(), lib) ;
		lib.Iterate4mcs(testFst, ttb, symmap.getStartPuaCpv()) ;
		Transliterator trInput = ttb.getTranslitTokenizer(true) ;  // true for input side

		try {
			BufferedReader in = null ;
			if (inputFileEncoding.equals("default") || inputFileEncoding.equals("-")) {
				// get the current default encoding of the operating system
				inputFileEncoding = System.getProperty("file.encoding") ;
			}
			if (inputFileEncoding.equals("UTF-8")) {
				in = new BufferedReader(new InputStreamReader(
													new UTF8BOMStripperInputStream(new FileInputStream(fullpath)) ,
													inputFileEncoding
															)
										) ;
			} else {
				in = new BufferedReader(new InputStreamReader(
															new FileInputStream(fullpath) ,
															inputFileEncoding
															)
										) ;
			}

			// now try to open the output file 
			fullpath = getFullpath(outputFilePath) ;

			BufferedWriter out = null ;
			if (outputFileEncoding.equals("default") || outputFileEncoding.equals("-")) {
				// get the current default encoding of the operating system
				outputFileEncoding = System.getProperty("file.encoding") ;
			}
			out = new BufferedWriter(new OutputStreamWriter(
															new FileOutputStream(fullpath) ,
															outputFileEncoding
															)
				  );

			out.write("<?xml version=\"1.0\" encoding=\"" + outputFileEncoding + "\"?>") ;
			out.newLine() ;
			out.write("<" + rootElmtName + ">") ;
			out.newLine() ;

			// read the input string/words, one per line, from the input file, write output to the output file

			XMLOutputLister xmlOutputLister = new XMLOutputLister(symmap, out, outputElmtName, weightAttrName) ;

			String token ;   // one per line in the input file

			Fst modifiedTestFst ;

			while ((token = in.readLine()) != null) {
				String cpvstr = trInput.transliterate(token) ; 
				// converts cpvstr to a sequence of code pt values, and
				// each one could fill one or two 16-bit code units;
				// this is where multichar symbols are reduced to their
				// code point values

				// get length in Unicode characters (not code units)
				int inputlen = cpvstr.codePointCount(0, cpvstr.length()) ;
				// allocate an int array to hold those code-point values,
				//    one int per code point value
				int[] cpvArray = new int[inputlen] ;

				// UCharacterIterator knows how to iterate over a String and
				// return the Unicode-Character code point values
				UCharacterIterator iter = UCharacterIterator.getInstance(cpvstr) ;

				// we need to build each input string into a one-path Fst

				// store the codepoints in the int array (which will be passed to
				//    oneStringNativeFst(), a native method
				int codepoint ;
				int index = 0 ;
				while ((codepoint = iter.nextCodePoint()) != UCharacterIterator.DONE) {
					// any multichar symbols will already be in the
					// symmap, or they wouldn't have been identified;
					// but BMP characters may not yet be in the symmap
					if (Character.charCount(codepoint) == 1) {
						symmap.putsym(String.valueOf((char) codepoint)) ;
					}
					cpvArray[index++] = codepoint ;
				}

				// 0 arg means generate
				Fst compFst = lib.ApplyToOneString(testFst, cpvArray, 0) ;

				// prepare to list the output strings (and their weights)
				long stringCount = lib.NumPaths(compFst) ;

				// XML output for this input token

				out.write("  <" + tokenElmtName + ">") ;
				out.newLine() ;

				// be careful to escape XML special chars in line; 
				// N.B. escapeXml also escapes non-ASCII Unicode letters
				//out.write("    <" + inputElmtName + ">" + 
				//  StringEscapeUtils.escapeXml(token) + "</" + 
				//  inputElmtName + ">") ;

				out.write("    <" + inputElmtName + ">" + 
				          EscapeXML.escapeXML(token) + 
						  "</" + inputElmtName + ">") ;
				out.newLine() ;

				out.write("    <" + outputsElmtName + ">") ;
				out.newLine() ;

				if (stringCount == 0) {
					// output nothing
				} else if (stringCount == -1) {
					// means that the composedFstPtr has loops, 
					//	denotes an infinite language
					out.write("      <infinite/>") ;
					out.newLine() ;
				} else {
					// native function listAllStrings will find all 
					//		strings in the Fst
					// and make callbacks to xmlOutputLister, 
					//		which knows how to output them as XML elements
					lib.ListAllStrings(compFst, 1, xmlOutputLister) ;
				}
		
				out.write("    </" + outputsElmtName + ">") ;
				out.newLine() ;

				out.write("  </" + tokenElmtName + ">") ;
				out.newLine() ;
			}
			in.close() ;

			out.write("</" + rootElmtName + ">") ;
			out.newLine() ;
			out.flush() ;
			out.close() ; 
		} catch (Exception e) {
			System.out.println("Exception found while testing input from file.") ;
			e.printStackTrace() ;
		}
		return data ;
	}

	// testTokensXMLFile_statement
	// Reads XML output from testTokensTextFile, 
	//	 which has an <input></input>
	//   element for each token.  Extract out just these input strings,
	//	 run them again, and produce another XML output file.  This allows
	//	 comparison of the previous XML output with new XML output, for
	//   regression-testing.

	public Object visit(ASTtestTokensXMLFile_statement node, Object data) {
		// Total: 11 regexp arguments, syntactically constrained
		// 
		// 0.  the Fst to test

		node.jjtGetChild(0).jjtAccept(this, data) ;
		Fst testFst = (Fst)(stack.pop()) ;

		// 1.  path of the input file

		node.jjtGetChild(1).jjtAccept(this, data) ;
		Fst tempFst = (Fst)(stack.pop()) ;

		String inputFilePath = lib.GetSingleString(tempFst, "Second arg to testTokensXMLFile must denote a language of exactly one string.") ;
		
		if (inputFilePath.length() == 0) {
			throw new KleeneArgException("Second arg to testTokensXMLFile must denote exactly one non-empty string") ;
		}

		// 2. argument supplying the name of the element holding
		//      the input strings, by default, "input", i.e.
		//      <input>...</input>
		// N.B. in testTokensTextFile, this argument specifies the
		// encoding of the input file, which is not needed for XML,
		// which either has an explicit "encoding" specification, or
		// is UTF-8 by default

		node.jjtGetChild(2).jjtAccept(this, data) ;
		tempFst = (Fst)(stack.pop()) ;

		String srcInputElmtName = lib.GetSingleString(tempFst, "Third arg to testTokensXMLFile must denote a language of exactly one string.") ;
		
		if (srcInputElmtName.length() == 0) {
			throw new KleeneArgException("Third arg to testTokensXMLFile must denote one non-empty string") ;
		}

		// 3.  path of the output file

		node.jjtGetChild(3).jjtAccept(this, data) ;
		tempFst = (Fst)(stack.pop()) ;

		String outputFilePath = lib.GetSingleString(tempFst, "Fourth arg to testTokensXMLFile must denote a language of exactly one string.") ;
		
		if (outputFilePath.length() == 0) {
			throw new KleeneArgException("Fourth arg to testTokensXMLFile must denote one non-empty string") ;
		}

		// 4.  encoding of the output file

		node.jjtGetChild(4).jjtAccept(this, data) ;
		tempFst = (Fst)(stack.pop()) ;

		String outputFileEncoding = lib.GetSingleString(tempFst, "Fifth arg to testTokensXMLFile must denote a language of exactly one string.") ;
		
		if (outputFileEncoding.length() == 0) {
			throw new KleeneArgException("Fifth arg to testTokensXMLFile must denote one non-empty string") ;
		}

		//    		And for the XML output

		// 5.  name of the root element

		node.jjtGetChild(5).jjtAccept(this, data) ;
		tempFst = (Fst)(stack.pop()) ;

		String rootElmtName = lib.GetSingleString(tempFst, "Sixth arg to testTokensXMLFile must denote a language of exactly one string.") ;
		
		if (rootElmtName.length() == 0) {
			throw new KleeneArgException("Sixth arg to testTokensXMLFile must denote one non-empty string") ;
		}

		// 6.  name of the token element

		node.jjtGetChild(6).jjtAccept(this, data) ;
		tempFst = (Fst)(stack.pop()) ;

		String tokenElmtName = lib.GetSingleString(tempFst, "Seventh arg to testTokensXMLFile must denote a language of exactly one string.") ;
		
		if (tokenElmtName.length() == 0) {
			throw new KleeneArgException("Seventh arg to testTokensXMLFile must denote one non-empty string") ;
		}

		// 7.  name of the input element

		node.jjtGetChild(7).jjtAccept(this, data) ;
		tempFst = (Fst)(stack.pop()) ;

		String inputElmtName = lib.GetSingleString(tempFst, "Eighth arg to testTokensXMLFile must denote a language of exactly one string.") ;
		
		if (inputElmtName.length() == 0) {
			throw new KleeneArgException("Eighth arg to testTokensXMLFile must denote one non-empty string") ;
		}

		// 8.  name of the outputs element (N.B. plural)

		node.jjtGetChild(8).jjtAccept(this, data) ;
		tempFst = (Fst)(stack.pop()) ;

		String outputsElmtName = lib.GetSingleString(tempFst, "Ninth arg to testTokensXMLFile must denote a language of exactly one string.") ;
		
		if (outputsElmtName.length() == 0) {
			throw new KleeneArgException("Ninth arg to testTokensXMLFile must denote one non-empty string") ;
		}

		// 9.  name of the output element  (N.B. singular)

		node.jjtGetChild(9).jjtAccept(this, data) ;
		tempFst = (Fst)(stack.pop()) ;

		String outputElmtName = lib.GetSingleString(tempFst, "Tenth arg to testTokensXMLFile must denote a language of exactly one string.") ;
		
		if (outputElmtName.length() == 0) {
			throw new KleeneArgException("Tenth arg to testTokensXMLFile must denote one non-empty string") ;
		}

		// 10.  name of the weight attr in the output elmt

		node.jjtGetChild(10).jjtAccept(this, data) ;
		tempFst = (Fst)(stack.pop()) ;

		String weightAttrName = lib.GetSingleString(tempFst, "Eleventh arg to testTokensXMLFile must denote a language of exactly one string.") ;
		
		if (weightAttrName.length() == 0) {
			throw new KleeneArgException("Eleventh arg to testTokensXMLFile must denote one non-empty string") ;
		}

		String fullpath = getFullpath(inputFilePath) ;

		TranslitTokenizerBuilder ttb = new TranslitTokenizerBuilder(symmap, testFst.getSigma(), lib) ;
		lib.Iterate4mcs(testFst, ttb, symmap.getStartPuaCpv()) ;
		Transliterator trInput = ttb.getTranslitTokenizer(true) ;  // true for input side

		try {
			// try to read/parse the XML input file

			Document doc = null ;

			doc = parseXML(fullpath) ;  // dom4j

			// Read all the <input></input> elements into a list
			// N.B. by default, the name of the element is "input",
			// but in general it is specified in arg srcInputElmtName
			List list = doc.selectNodes("//" + srcInputElmtName) ;

			// now try to open the output file 

			fullpath = getFullpath(outputFilePath) ;

			BufferedWriter out = null ;
			if (outputFileEncoding.equals("default") 
				|| outputFileEncoding.equals("-")) {
				// get the current default encoding of the operating system
				outputFileEncoding = System.getProperty("file.encoding") ;
			}
			out = 
				new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(fullpath) ,
													outputFileEncoding)
				  );

			out.write("<?xml version=\"1.0\" encoding=\"" + 
						outputFileEncoding + "\"?>") ;
			out.newLine() ;
			out.write("<" + rootElmtName + ">") ;
			out.newLine() ;

			XMLOutputLister xmlOutputLister = 
					new XMLOutputLister(symmap, 
			                            out, 
										outputElmtName, 
										weightAttrName) ;

			// Loop through the <input></input> elements, extracting and
			//	running the text string from each one; write output to
			//	the output file

			String token ;

			Fst modifiedTestFst ;

			for ( Iterator it = list.iterator(); it.hasNext(); ) {
				Element inputElmt = (Element) it.next() ;
				token = inputElmt.getText() ;

				String cpvstr = trInput.transliterate(token) ; 
				// converts cpvstr to a sequence of code pt values, and
				// each one could fill one or two 16-bit code units;
				// this is where multichar symbols are reduced to their
				// code point values

				// get length in Unicode characters (not code units)
				int inputlen = cpvstr.codePointCount(0, cpvstr.length()) ;
				// allocate an int array to hold those code-point values,
				//    one int per code point value
				int[] cpvArray = new int[inputlen] ;

				// UCharacterIterator knows how to iterate over a 
				//	String and
				// return the Unicode-Character code point values
				UCharacterIterator 
					iter = UCharacterIterator.getInstance(cpvstr) ;

				// we need to build each input string into a one-path Fst

				// store the codepoints in the int array 
				//		(which will be passed to
				//    oneStringNativeFst(), a native method
				int codepoint ;
				int index = 0 ;
				while ((codepoint = iter.nextCodePoint()) 
						!= UCharacterIterator.DONE) {
					// any multichar symbols will already be in the
					// symmap, or they wouldn't have been identified;
					// but BMP characters may not yet be in the symmap
					if (Character.charCount(codepoint) == 1) {
						symmap.putsym(String.valueOf((char) codepoint)) ;
					}
					cpvArray[index++] = codepoint ;
				}

				// 0 arg for generation, apply the inputFst to the "input"
				// side of testFst
				Fst compFst = lib.ApplyToOneString(testFst, cpvArray, 0) ;

				// prepare to list the output strings (and their weights)
				long stringCount = lib.NumPaths(compFst) ;

				// XML output for this input token

				out.write("  <" + tokenElmtName + ">") ;
				out.newLine() ;

				// be careful to escape XML special chars in line; 
				// N.B. escapeXml also escapes non-ASCII Unicode letters
				//out.write("    <" + inputElmtName + ">" + 
				//          StringEscapeUtils.escapeXml(token) + 
				//          "</" + inputElmtName + ">") ;

				out.write("    <" + inputElmtName + ">" + 
				          EscapeXML.escapeXML(token) + 
						  "</" + inputElmtName + ">") ;
				out.newLine() ;

				out.write("    <" + outputsElmtName + ">") ;
				out.newLine() ;

				if (stringCount == 0) {
					// output nothing
				} else if (stringCount == -1) {
					// means that the compFstPtr has loops, 
					//		denotes an infinite language
					out.write("      <infinite/>") ;
					out.newLine() ;
				} else {
					// native function listAllStrings will find all 
					//		strings in the Fst
					// and make callbacks to xmlOutputLister, 
					//		which knows how to output
					// them as XML elements
					lib.ListAllStrings(compFst, 1, xmlOutputLister) ;
				}
		
				out.write("    </" + outputsElmtName + ">") ;
				out.newLine() ;

				out.write("  </" + tokenElmtName + ">") ;
				out.newLine() ;
			}

			out.write("</" + rootElmtName + ">") ;
			out.newLine() ;
			out.flush() ;
			out.close() ; 

		} catch (Exception e) {
			// KRB:  review this
			System.out.println("Exception found while testing input from file.") ;
			e.printStackTrace() ;
		}
		return data ;
	}
	private String getOsName() {
		String os = "" ;
		if (System.getProperty("os.name").toLowerCase().indexOf("mac") > -1) {
			os = "osx" ;
		} else if (System.getProperty("os.name").toLowerCase().indexOf("linux") > -1) {
			os = "linux" ;
		} else if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
			os = "windows" ;
		} else {
			os = "unknown" ;
		}
		return os ;
	}
	private Document parseXMLPrefs(String filepath) throws Exception {
		SAXReader reader = new SAXReader() ;
		Document document = null ; 
		// Document document = reader.read(filepath) ;

		try {
			// the encoding should be UTF-8
			// then need to work around SUN's irresponsible decision not to
			//  handle the optional UTF-8 BOM correctly
			document = reader.read(new InputStreamReader(
												new UTF8BOMStripperInputStream(new FileInputStream(filepath)),
												"UTF-8")
								  ) ;
		} catch (Exception e) {
			e.printStackTrace() ; 
			throw e ;
		}

		return document ;
	}

	// called in ASTdraw_statement
	private String getPref(Document doc, String xmlPath) {
		String osName = getOsName() ;
		// first see if there is a user-specified override of the default value
		String value =  
			((Element) doc.selectSingleNode("/prefs/" + osName + "/user/" +
			xmlPath)).getTextTrim() ;
		// if the user setting is empty
		if (value.equals("")) {
			// then get the default setting
			value = ((Element) doc.selectSingleNode("/prefs/" + osName +
				"/default/" + xmlPath)).getTextTrim() ;
		}
		return value ;
	}

	public Object visit(ASTdraw_statement node, Object data) {
		// debug
		// one daughter:  regexp

		// KRB: does this make any sense outside of the GUI???
		// KRB: review this whole method

		node.jjtGetChild(0).jjtAccept(this, data) ;
		// Should leave an Fst object on the stack (see Fst.java)
		Fst fst = (Fst)(stack.pop()) ;

		// magic numbers for now; limits on the size of a network
		// that will be drawn
		long stateLimit = 400L ;
		long arcLimit = 400L ;	

		long nstates = lib.NumStates(fst) ;
		long narcs   = lib.NumArcs(fst) ;

		if (nstates > stateLimit) {
			// don't try to draw it
			outputInterpMessage("// Fst contains over " + stateLimit + 
			" states, which is generally too much for the dot application to handle.",
			data) ;

			return data ;
		}

		if (narcs > arcLimit) {
			// don't try to draw it
			outputInterpMessage("// Fst contains over " + arcLimit + 
			" arcs, which is generally too much for the dot application to handle.", data)
			;

			return data ;
		}

		String userHomeDir = System.getProperty("user.home") ;  
		// to find temp files like ~/.kleene/tmp/last.dot
		// and ~/.kleene/prefs/prefs.xml

		String osName = getOsName() ;
		// defaults
		String tmpdir = "" ;
		String prefsPath = "" ;
		String dotSrcPath = "" ;
		String slashSep = "/" ;

		if (osName.equals("windows")) {
		    slashSep = "\\";  
			// only really needed for command shell (cmd /c), 
			//		else can use "/"
		}

		StringBuilder sbhex = new StringBuilder() ;
		StringBuilder sb	= new StringBuilder() ;
		getSigmaStrings(fst, sbhex, sb) ;

		// On Linux and OS X, the basic Kleene directory is ~/.kleene
		// On Windows, this maps to C:\Documents and Settings\
		//		\<username>\.kleene
		tmpdir = userHomeDir + slashSep + ".kleene" + slashSep + "tmp" ;
		dotSrcPath = tmpdir + slashSep + "last.dot" ;
		prefsPath = userHomeDir + slashSep + ".kleene" + 
					slashSep + "prefs" + slashSep + "prefs.xml" ;


		// an FstDotWriter object knows how to write a GraphViz .dot source file (to a specified
		// file; here written to last.lot in the user's tmp/ directory)
		FstDotWriter fstDotWriter = 
				new FstDotWriter(symmap, new File(dotSrcPath), sb.toString(), "UTF-8") ;

		// call Fst2dot traverses an OpenFst Fst directly and generates 
		//	dot code (by making callbacks to methods in the Java fstDotWriter)
		lib.Fst2dot(fst, fstDotWriter) ;
		// we should now have tmp/last.dot  (a GraphViz dot source file
		// describing a network diagram)
		
		// If the osName is "osx" and a native Graphviz.app is installed
		// in /Applications, then things are simple.  Just call Graphviz directly
		// on the .dot source file.  "open -a Graphviz /path/to/last.lot"
		// No need to generate PostScript and then call a viewer to see it.
		
		File nativeGraphviz = new File ("/Applications/Graphviz.app") ;
		if (osName.equals("osx") && nativeGraphviz.exists()) {
			try {
				Process proc = Runtime.getRuntime().exec("open -a Graphviz " + dotSrcPath) ;
				try {
					if (proc.waitFor() != 0) {
						System.err.println("Problem calling native OS X GraphViz: exit value " + proc.exitValue()) ;
					}
				} catch (InterruptedException e) {
					System.err.println(e) ;
				} finally {
				}
			} catch (Exception e) {
				System.err.println(e) ;
			}
		} else {
			// Need to do it the hard way.
			//
			// Take the .dot source file and call 'dot' to generate a graphics file, e.g. .ps
			// Then take the graphics file and call a viewer application
			// The location of the 'dot' application, the graphics format, and the view
			// application are specified in the user-specific pref.xml file

			// Access the user-specific prefs.xml file
			// type Document is a Java object representing an XML document (typically
			// read from an XML file into memory)
			Document doc = null ;
			try {
				doc = parseXMLPrefs(prefsPath) ;	// parse the user's prefs/prefs.xml
			} catch (Exception e) {
				// KRB:  review this
				System.out.println("Problem reading ~/.kleene/prefs/prefs.xml") ;
				e.printStackTrace() ;
			}

			// Navigate to platform-specific and user-specific dot, format, 
			//	viewer elmts in the prefs.xml file

			// get the path to the "dot" application
			String dotpath = getPref(doc, "dot/dotpath") ;

			// get the file format the dot should produce, e.g. ps or pdf
			String dotflag = getPref(doc, "dot/dotflag") ;

			// get the path to the viewer application
			String dotview = getPref(doc, "dot/viewer")  ;

			// Trouble with generating/displaying PDF directly; 
			// If you generate .ps and 'open' it, the orientation=landscape
			// and center="true" are reflected correctly in the display (the
			// ps is converted automatically to pdf)
			// But if you generate the PDF file directly and 'open' it, the
			// orientation is wrong and the centering command is ignored.
			// PostScript seems more reliable right now.

			// ****************** Call 'dot' from Java **********************

			// construct the 'dot' command string to be launched by ProcessBuilder

			// Command shell prefix needed for ProcessBuilder is opsys-specific.
			String cmdShell, cmdShellOpts;
			if (osName.equals("windows")) {
				cmdShell = "cmd" ;
				cmdShellOpts = "/c" ;
			} else {
				// for Linux and OS X (valued of osName will be "osx")
				cmdShell = "/bin/sh" ;
				cmdShellOpts = "-c" ;
			}

			// Use doublequotes to support filenames with embedded spaces.
			// Initial blank prevents undesired doublequote removal by 
			//		Windows cmd.exe (see "cmd /?").
			String cmd = " \"" + dotpath + "\"" + " -T" + dotflag + 
						" \"" + tmpdir + slashSep + "last.dot\"" + " > " +
						 "\"" + tmpdir + slashSep + "last." + dotflag + "\"" ;

			// calling 'dot' from Java, from the .dot source file,
			// it should generate a graphics file, e.g. .ps (PostScript)

			try {
				ProcessBuilder pb = 
					new ProcessBuilder(cmdShell, cmdShellOpts, cmd) ;
				Process p = pb.start() ;

				StreamFlusher errorFlusher = 
					new StreamFlusher(p.getErrorStream(), "ERROR") ;
				StreamFlusher outputFlusher = 
					new StreamFlusher(p.getInputStream(), "OUTPUT") ;

				errorFlusher.start() ;
				outputFlusher.start() ;

				int exitVal = p.waitFor() ;
			} catch (Exception e) {
				e.printStackTrace() ;
			}

			// ******************* Now launch the viewer app from Java

			//KRB: putting double quotes around dotview currently 
			//	works for Linux, at least
			// with the current default dotview string:  /usr/bin/kghostview
			// which doesn't contain command-line options

			if (osName.equals("osx")) {
				// KRB: putting double quotes around dotview breaks 
				//	drawing for OS X,
				// where the dotview string is
				// /usr/bin/open -a /Applications/Preview.app/Contents/MacOS/Preview
				// (having three fields and two spaces)
				cmd = dotview + " \"" + tmpdir + slashSep + "last." + dotflag + "\"" ;
			} else {
				// Phil: fix for Windows (and seems to work for Linux)
				// Use doublequotes to support filenames with embedded spaces.
				// Initial blank prevents undesired doublequote removal by Windows cmd.exe (see "cmd /?").
				cmd = " \"" + dotview + "\" \"" + tmpdir + slashSep + "last." + dotflag + "\"" ;
			}

			//  launching the viewer on the ps, pdf (or whatever) file generated by 'dot'

			try {
				ProcessBuilder pb = 
					new ProcessBuilder(cmdShell, cmdShellOpts, cmd) ;
				Process p = pb.start() ;

				StreamFlusher errorFlusher = 
					new StreamFlusher(p.getErrorStream(), "ERROR") ;
				StreamFlusher outputFlusher = 
					new StreamFlusher(p.getInputStream(), "OUTPUT") ;

				errorFlusher.start() ;
				outputFlusher.start() ;

				// if active, this stmt causes the viewer window to be 'modal', causing
				// Kleene to suspend operations until the viewer is closed
				//int exitVal = p.waitFor() ;
			} catch (Exception e) {
				e.printStackTrace() ;
			}

			// need to drain stdout stderr and inputStream in separate threads?
		}

		return data ;
	}
	public Object visit(ASTsigma_statement node, Object data) {
		// Makes sense only in the GUI

		if (((InterpData)data).getInGUI()) {
			// Should be just one Fst daughter, syntactically constrained
			node.jjtGetChild(0).jjtAccept(this, data) ;
			Fst fst = (Fst)(stack.pop()) ;
			
			PseudoTerminalInternalFrame terminal = 
				((InterpData)data).getGUI().getTerminal() ;
			
			//String str = fst.getSigma().toString() ;
			//terminal.appendToHistory(str) ;

			StringBuilder sbhex = new StringBuilder() ;
			StringBuilder sb    = new StringBuilder() ;

			getSigmaStrings(fst, sbhex, sb) ;

			terminal.appendToHistory("{ " + sbhex.toString() + "}") ;
			terminal.appendToHistory("{ " + sb.toString()    + "}") ;
			if (fst.getContainsOther()) {
				terminal.appendToHistory("Contains OTHER") ;
			}
		}
		return data ;
	}
	public Object visit(ASTwritexml_statement node, Object data) {
		// Either one, two or three daughters:
		// fst (, filepath (, encoding)?)?
		//   The first represents the Fst to be drawn as XML
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// Should leave an Fst object on the stack
		Fst fst = (Fst)(stack.pop()) ;

		String path = "out.xml" ;	// default
		if (node.jjtGetNumChildren() >= 2) {
			node.jjtGetChild(1).jjtAccept(this, data) ;
			Fst pathFst = (Fst)(stack.pop()) ;

			path = lib.GetSingleString(pathFst, "Second arg to writeXML must denote a language of exactly one string.") ;
			
			if (path.length() == 0) {
				throw new KleeneArgException("Second arg to writeXML must denote a non-empty string") ;
			}
		}

		String encoding = "UTF-8" ;	// default
		if (node.jjtGetNumChildren() == 3) {
			node.jjtGetChild(2).jjtAccept(this, data) ;
			Fst encodingFst = (Fst)(stack.pop()) ;

			encoding = lib.GetSingleString(encodingFst, "Second arg to writeXML must denote a language of exactly one string.") ;
			
			if (path.length() == 0) {
				throw new KleeneArgException("Second arg to writeXML must denote a non-empty string") ;
			}
		}

		String fullpath = getFullpath(path) ;

		// note that the FstXmlWriter gets the filepath, so it knows where
		// to write the file

		writeXMLhelper(fst, fullpath, encoding) ;
		
		return data ;  
	}
	public Object visit(ASTwritedot_statement node, Object data) {
		// either one, two or three daughters
		// regexp (, filepath (, encoding)?)?
		// the first represents the Fst to be drawn as DOT source
		node.jjtGetChild(0).jjtAccept(this, data) ;
		// Should leave an Fst object on the stack
		Fst fst = (Fst)(stack.pop()) ;

		String path = "out.dot" ;	// default
		if (node.jjtGetNumChildren() >= 2) {
			node.jjtGetChild(1).jjtAccept(this, data) ;
			Fst pathFst = (Fst)(stack.pop()) ;

			path = lib.GetSingleString(pathFst, "Second arg to writeDOT must denote a language of exactly one string.") ;
			
			if (path.length() == 0) {
				throw new KleeneArgException("Second arg to writeDOT must denote a non-empty string") ;
			}
		}

		String encoding = "UTF-8" ;  // default
		if (node.jjtGetNumChildren() == 3) {
			node.jjtGetChild(2).jjtAccept(this, data) ;
			Fst encodingFst = (Fst)(stack.pop()) ;

			encoding = lib.GetSingleString(encodingFst, "Third arg to writeDOT must denote a language of exactly one string.") ;
			
			if (encoding.length() == 0) {
				throw new KleeneArgException("Third arg to writeDOT must denote a non-empty string") ;
			}
		}

		StringBuilder sbhex = new StringBuilder() ;
		StringBuilder sb	= new StringBuilder() ;
		getSigmaStrings(fst, sbhex, sb) ;

		String fullpath = getFullpath(path) ;

		// note that the FstDotWriter gets the filepath, so it knows where
		// to write the file
		FstDotWriter fstDotWriter = 
			new FstDotWriter(symmap, new File(fullpath), sb.toString(),
			encoding) ;

		// Call Fst2dot to iterate through the Fst, 
		//	it will make calls
		// back to methods in the fstDotWriter to do the actual output 
		//	to file.
		// (The C++ code has iterators, but Unicode file output 
		//	from C++ is not
		// worth the trouble.  Even if the C++ code were written to 
		//	write the DOT
		// directly, it would still have to make calls back to the 
		//	symmap method
		// .getsym(i) to convert the int-value labels to strings.

		lib.Fst2dot(fst, fstDotWriter) ;
		
		return data ;  
	}
	public Object visit(ASTsource_statement node, Object data) {
		// Syntax:
		// source regexp() ;  
		// one filepath, to be read in the default encoding of the
		// operating system; 

		// source regexp(), regexp() ;  // pair of (filepath, encoding)

		// source regexp(), regexp(), regexp(), regexp(), ... ;  
		// pairs of (filepath,
		// encoding)
		// 
		// each regexp() semantically limited
		// to encoding a single string

		int childCount = node.jjtGetNumChildren() ;
		String pathstring = "" ;
		String fullpath = "" ;
		String encoding = "" ;

		for (int p = 0; 
			 (childCount == 1 && p == 0) || p <= (childCount - 2) ; 
			 p += 2) {
			// get the path string
			node.jjtGetChild(p).jjtAccept(this, data) ;
			// Should leave an Fst object on the stack
			Fst pathFst = (Fst)(stack.pop()) ;

			pathstring = lib.GetSingleString(pathFst, "Each arg to 'source' must denote a language of exactly one string.") ;
			
			if (pathstring.length() == 0) {
				throw new KleeneArgException("Each path arg to 'source' must denote a language of exactly one non-empty string") ;
			}
			File file = new File(pathstring) ;

			fullpath = getFullpath(pathstring) ;

			// get the encoding ("default" iff childCount == 1)

			if (childCount == 1) {
				// get the current default encoding of the operating system
				encoding = System.getProperty("file.encoding") ;
			} else {
				node.jjtGetChild(p + 1).jjtAccept(this, data) ;
				Fst encodingFst = (Fst)(stack.pop()) ;

				encoding = lib.GetSingleString(encodingFst, "Each arg to 'source' must denote a language of exactly one string.") ;
				if (encoding.length() == 0) {
					throw new KleeneArgException("The encoding argument to source must be a non-empty string.") ;
				}
			}

			// Now have fullpath and encoding

			if (((InterpData)data).getInGUI()) {

				//                   path, encoding, inGUI
				Kleene.runScript(fullpath, encoding, true) ;

			} else {

				// not in a GUI, so handle like a command-line script
				Kleene.runScript(fullpath, encoding, false) ;
			}
		}
		return data ;
	}
}
