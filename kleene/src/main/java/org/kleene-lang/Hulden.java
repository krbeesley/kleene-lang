
//	Hulden.java
//
//	The Kleene Programming Language

//   Copyright 2010-2012 SAP AG

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

// Implementation of Mans Hulden's algorithms for compiling
// alternation rules into transducers.

public class Hulden {

	OpenFstLibraryWrapper lib ;
	SymMap symmap ;

	public Hulden(OpenFstLibraryWrapper lib, SymMap symmap) {
		this.lib = lib ;
		this.symmap = symmap ;
	}

	// *******************************************************************
	// Definitions and functions for interpreting alternation rules 
	// (translated from Hulden's foma functions)
	// See in Depot:
	// ..Kleene/doc/internal/notes/rules/hulden/huldenrules/
	// rewritedefinitions-edited.script
	// *******************************************************************

	// Hulden's method involves defining, manipulating and using "triple"
	// languages wherein each string consists of symbol triples, e.g.
	// abcdef as a triple string contains two triples, with 'a' and 'd' on tape
	// 1, 'b' and 'e' on tape 2, and 'c' and 'f' on tape 3

	// Tape 1 contains I-symbols and O-symbols (letter 'O')
	// I-symbols are used "inside the action (rewrite part) of a rule"
	// The O-symbol, @O@, is used on tape 1 to mark triples that are outside the
	// action of a rule.

	// In Kleene, symbols used internally by the compiler are prefaced with __
	// or ** to avoid confusion with user-defined symbols.
	// The prefix __ means "do not use this symbol when promoting OTHER";
	// this is used in symbols referring to subnetworks in RTNs
	// (OpenFstRtnConventions).
	// Symbols beginning with ** are used temporarily when compiling
	// alternation rules.

	// Define the special-symbol names only here, in case they need to be changed.
	// Do not strew such strings all through the code.

	public String IOpenAndCloseSym = "**@I[]@" ;
	public String IOpenSym         = "**@I[@" ;
	public String ICloseSym		   = "**@I]@" ;
	public String ISym			   = "**@I@" ;

	// letter 'O', @O@ on tape 1 marks triples "outside" the action of a rule
	public String outsideMarkerSym = "**@O@" ;

	// ID, on tape 3, marks identity mappings of the symbol on tape 2
	public String idMarkerSym      = "**@ID@" ;

	// the hardEpsilon is used to compile alternation rules
	// N.B. @0@ has a zero, not to be confused with @O@ with letter 'O'
	public String hardEpsilonSym   = "**@0@" ;  // following Hulden's notation

	// N.B. needs to be considered in the promotion of OTHER
	// See also the tokenization of WORD_BOUNDARY in Kleene.jjt
	public  String ruleWordBoundarySym = "**@#@" ; // again following Hulden's notation

	public String ruleRightAngleSym = "**@>@" ;

	// for language-restriction rules
	public String restDelimSym = "**@RD@" ;


	// ********************************************************************

	public void SubstEpsilonInPlace(Fst fst, int orig) {
		if (fst.getSigma().contains(orig)) {
			lib.SubstLabelInPlace(fst, orig, lib.Epsilon) ;
			// and just remove the orig symbol from the sigma
			fst.getSigma().remove(orig) ;
		}
		// else nothing to do
	}

	public void SubstEpsilonInPlace(Fst fst, String origStr) {
		SubstEpsilonInPlace(fst, symmap.putsym(origStr)) ;
	}


	// ~$fst   as  .* - (.* $x .*)
	private Fst notContainsFst(Fst fst) {
		if (!lib.IsAcceptor(fst)) {
			throw new FstPropertyException("The argument to notContains() must be an acceptor.") ;
		}

		return lib.Difference(	lib.UniversalLanguageFst(),
							    	lib.Concat3Fsts( 
							   			lib.UniversalLanguageFst(), 
										fst, 
										lib.UniversalLanguageFst()
									)
								);
	}

	private Fst ignoreFst(Fst base, Fst fluff) {
		return lib.OutputProjection(
					lib.Compose(
						base,
						lib.KleeneStar(lib.Concat(
											lib.KleeneStar(lib.OneArcFst(lib.otherIdSym)),
											lib.KleeneStar(lib.Crossproduct(
															lib.OneArcFst(lib.Epsilon),
															fluff)
														)
									 			) 
									) 
						)
					) ;
	}
	
	// Hulden
	// define IOpen		["@I[@"|"@I[]@"] ;
	// I symbols that can open/start a mapping

	public Fst IOpen() {
		return lib.Union(lib.OneArcFst(IOpenSym), lib.OneArcFst(IOpenAndCloseSym)) ;
	}

	// Hulden
	// define IClose	["@I]@"|"@I[]@"] ;
	// I symbols that end a mapping

	private Fst IClose() {
		return lib.Union(lib.OneArcFst(ICloseSym), lib.OneArcFst(IOpenAndCloseSym)) ;
	}

	// Hulden
	// define ISyms		["@I@"|"@I[@"|"@I]@"|"@I[]@"] ;
	// covers all I-symbols

	private Fst ISyms() {
		return lib.Union4Fsts(
					lib.OneArcFst(ISym),
					lib.OneArcFst(IOpenSym),
					lib.OneArcFst(ICloseSym),
					lib.OneArcFst(IOpenAndCloseSym)
				) ;
	}

	// *********************************************************************

	// Tape1Sig() covers all the symbols that can appear on tape 1 of a triple
	// string/language
	// @O@ marks triples "outside" the action of a rule,
	// ISyms mark triples inside the action of a rule
	// Hulden:
	// define Tape1Sig ["@O@"|ISyms] ;

	private Fst Tape1Sig() {
		return lib.Union(
						lib.OneArcFst(outsideMarkerSym) ,
						ISyms()
					) ;
	}

	// Tape2Sig() covers all the symbols that can appear on tape 2 of a triple
	// string/language.  @ID@ symbols appear only on tape 3.
	// Hulden:
	// define Tape2Sig [?-Tape1Sig-"@ID@"] ;

	public Fst Tape2Sig() {
		return lib.Difference(
						lib.Difference(lib.OneArcFst(lib.otherIdSym), 
										Tape1Sig()
						),
						lib.OneArcFst(idMarkerSym)
			) ;
	}

	// Tape3Sig() covers all the symbols that can appear on tape 3 of a triple
	// string/language
	// Hulden
	// define Tape3Sig [?-Tape1Sig] ;

	private Fst Tape3Sig() {
		return lib.Difference(
						lib.OneArcFst(lib.otherIdSym), 
						Tape1Sig()
					) ;
	}


	// Hulden
	// define SpecialSymbols ISyms|"@O@"|"@ID@"|"@0@"|"@#@" ;
	// N.B. distinguish @O@, with letter 'O' from @0@, with zero

	private Fst SpecialSymbolsAction() {
		return lib.Union5Fsts(
					ISyms(),
					lib.OneArcFst(outsideMarkerSym),
					lib.OneArcFst(idMarkerSym),
					lib.OneArcFst(hardEpsilonSym),
					lib.OneArcFst(ruleWordBoundarySym)
		) ;
	}

	// if the "parts" of a rule,  A -> B / L _ R contain OTHER,
	// then they get "contaminated" with the special symbols; use
	// CleanupSpecialSymbolsXXX(), to weed them out

	// used to exclude special symbols from A and B on the LHS,
	// A -> B
	// old version assumed that the fst was an acceptor 
	/*
	public Fst CleanupSpecialSymbolsAction(Fst fst) {
		if (fst.getContainsOther()) {
			return lib.Intersect(	fst, 
							  		notContainsFst(SpecialSymbolsAction())
							 	) ;
		}
		// else
		return fst ;
	}
	*/

	// new version, accommodating transducer rules, allows fst
	// to be a transducer
	public Fst CleanupSpecialSymbolsAction(Fst fst) {
		if (fst.getContainsOther()) {
			return lib.Compose3Fsts(	notContainsFst(SpecialSymbolsAction()),
										fst, 
							  			notContainsFst(SpecialSymbolsAction())
							 		) ;
		}
		// else
		return fst ;
	}

	private Fst SpecialSymbolsContext() {
		return lib.Union4Fsts(
					ISyms(),
					lib.OneArcFst(outsideMarkerSym),
					lib.OneArcFst(idMarkerSym),
					lib.OneArcFst(hardEpsilonSym)
					// do not include ruleWordBoundarySym here,
					// needs to be left in contexts
		) ;
	}

	// used to exclude special symbols from the Left and Right sides
	// of the context
	public Fst CleanupSpecialSymbolsContext(Fst fst) {
		if (fst.getContainsOther()) {
			return lib.Intersect(	fst, 
									notContainsFst(SpecialSymbolsContext())
							 	) ;
		}
		return fst ;
	}

	// *************************************************************************
						
	// Hulden's algorithm involves creating "two-tape" and "three-tape" strings,
	// or more generally two-tape or three-tape languages, wherein each string
	// consists of pairs, or triples.  In a two-tape string (in a two-tape
	// language) abcd has 'a' and 'c' on tape one and 'b' and 'd' on tape two.
	// In a three-tape string (in a three-tape language), abcdef has 'a' and 'd'
	// on tape one, 'b' and 'e' on tape two, and 'c' and 'f' on tape three.

	// Given a one-tape language lang, return the three-tape language
	// that has lang on tape 1

	// Hulden's 
	// define Tape1of3(X) [X .o. [? 0:? 0:?]*].l ;

	private Fst Tape1of3(Fst X) {
		// argument should be an Acceptor
		// if (!lib.IsAcceptor(lang.getFstPtr()))

		Fst temp = lib.KleeneStar(lib.Concat3Fsts(
									lib.OneArcFst(lib.otherIdSym),
									lib.OneArcFst(lib.Epsilon, lib.otherNonIdSym),
									lib.OneArcFst(lib.Epsilon, lib.otherNonIdSym)
									)
								) ;

		return lib.OutputProjection(lib.Compose(X, temp)) ;
	}

	// given a one-tape language lang, return the three-tape language that has
	// lang on tape 2

	// Hulden's 
	// define Tape2of3(X) [X .o. [0:? ? 0:?]*].l

	private Fst Tape2of3(Fst X) {
		// argument should be an Acceptor
		// if (!lib.IsAcceptor(lang.getFstPtr()))

		Fst temp = lib.KleeneStar(lib.Concat3Fsts(
										lib.OneArcFst(lib.Epsilon, lib.otherNonIdSym),
										lib.OneArcFst(lib.otherIdSym),
										lib.OneArcFst(lib.Epsilon, lib.otherNonIdSym)
										)
								) ;

		return lib.OutputProjection(lib.Compose(X, temp)) ;
	}

	// KRB experimental addition trying to handle a <- "" (left-arrow epenthesis rules)
	// 2012-09-23
	private Fst Tape3of3(Fst X) {
		// argument should be an Acceptor
		// if (!lib.IsAcceptor(lang.getFstPtr()))

		Fst temp = lib.KleeneStar(lib.Concat3Fsts(
										lib.OneArcFst(lib.Epsilon, lib.otherNonIdSym),
										lib.OneArcFst(lib.Epsilon, lib.otherNonIdSym),
										lib.OneArcFst(lib.otherIdSym)
										)
								) ;

		return lib.OutputProjection(lib.Compose(X, temp)) ;
	}
	

	// given a two-tape language lang, return the three-tape language that has
	// lang on tapes 2 and 3 (and anything on tape 1)

	// Hulden's
	// define Tape23of3(X) [X .o. [0:? ? ?]*].l ;

	private Fst Tape23of3(Fst X) {
		// argument should be an acceptor
		// if (!lib.IsAcceptor(lang.getFstPtr()))

		Fst temp = lib.KleeneStar(lib.Concat3Fsts(
										lib.OneArcFst(lib.Epsilon, lib.otherNonIdSym),
										lib.OneArcFst(lib.otherIdSym),
										lib.OneArcFst(lib.otherIdSym))
								) ;

		return lib.OutputProjection(lib.Compose(X, temp)) ;
	}

	// Hulden's
	// define EPEXTEND  Tape1of3("@O@") | [ Tape1of3("@I[@" "@I@"* "@I]@" | "@I[]@" ) &
	// 										Tape2of3(~["@0@"*])
	// 									  ] ;
	// used for interpreting epenthesis rules (at least right-arrow epenthesis rules)
	private Fst EPEXTEND_RIGHT_ARROW() {
		Fst temp = lib.Union(
			Tape1of3(lib.OneArcFst(outsideMarkerSym)),

		 	lib.Intersect(	Tape1of3(lib.Union(	lib.Concat3Fsts(lib.OneArcFst(IOpenSym),
										 					  	lib.KleeneStar(lib.OneArcFst(ISym)),
															  	lib.OneArcFst(ICloseSym)
															   ),
									 			lib.OneArcFst(IOpenAndCloseSym)
									          )
							),
							Tape2of3(lib.Complement(lib.KleeneStar(lib.OneArcFst(hardEpsilonSym))))
			)
		) ;
		return temp ;
	}

	// Hulden's
	// define EPContextL(X) [?* X & ?* EPEXTEND] ;
	// used for interpreting epenthesis rules (at least right-arrow epenthesis rules)
	// modify
	// define EPContextL_RIGHT_ARROW(X) [?* X & ?* EPEXTEND_RIGHT_ARROW] ;
	public Fst EPContextL_RIGHT_ARROW(Fst X) {
		Fst fst = lib.Intersect(	lib.Concat(	lib.UniversalLanguageFst(), X),
									lib.Concat(	lib.UniversalLanguageFst(), EPEXTEND_RIGHT_ARROW())
								) ;
		return fst ;
	}

	// Hulden's
	// define EPContextR(X) [EPEXTEND ?* & X ?*] ;
	// used for interpreting epenthesis rules (at least right-arrow epenthesis rules)
	// modify
	// define EPContextR_RIGHT_ARROW(X) [EPEXTEND_RIGHT_ARROW ?* & X ?*] ;
	public Fst EPContextR_RIGHT_ARROW(Fst X) {
		Fst fst = lib.Intersect(	lib.Concat(	EPEXTEND_RIGHT_ARROW(), lib.UniversalLanguageFst()),
									lib.Concat(	X,        lib.UniversalLanguageFst())
								) ;
		return fst ;
	}

	// Hulden's
	// define EPEXTEND  Tape1of3("@O@") | [ Tape1of3("@I[@" "@I@"* "@I]@" | "@I[]@" ) &
	// 										Tape2of3(~["@0@"*])
	// 									  ] ;
	// used for interpreting epenthesis rules (modified for left-arrow epenthesis like a <- "" )
	// change Tape2of3 to Tape3of3 for EPEXTEND_LEFT_ARROW
	// i.e.
	// define EPEXTEND_LEFT_ARROW  Tape1of3("@O@") | [ Tape1of3("@I[@" "@I@"* "@I]@" | "@I[]@" ) &
	//											# difference here, Tape3of3 instead of Tape2of3
	//											Tape3of3(~["@0@"*])
	//								  			] ;

	private Fst EPEXTEND_LEFT_ARROW() {
		Fst temp = lib.Union(
			Tape1of3(lib.OneArcFst(outsideMarkerSym)),

		 	lib.Intersect(	Tape1of3(lib.Union(	lib.Concat3Fsts(lib.OneArcFst(IOpenSym),
										 					  	lib.KleeneStar(lib.OneArcFst(ISym)),
															  	lib.OneArcFst(ICloseSym)
															   ),
									 			lib.OneArcFst(IOpenAndCloseSym)
									          )
							),
							// the difference is here---Tape3of3 for a <- "" rather than Tape2of3, for "" -> a
							//Tape2of3(lib.Complement(lib.KleeneStar(lib.OneArcFst(hardEpsilonSym))))
							Tape3of3(lib.Complement(lib.KleeneStar(lib.OneArcFst(hardEpsilonSym))))
			)
		) ;
		return temp ;
	}

	// Hulden's
	// define EPContextL(X) [?* X & ?* EPEXTEND] ;
	// used for interpreting epenthesis rules (at least right-arrow epenthesis rules)
	// modify for left-arrow rules
	// define EPContextL_LEFT_ARROW(X) [?* X & ?* EPEXTEND_LEFT_ARROW] ;
	public Fst EPContextL_LEFT_ARROW(Fst X) {
		Fst fst = lib.Intersect(	lib.Concat(	lib.UniversalLanguageFst(), X),
									lib.Concat(	lib.UniversalLanguageFst(), EPEXTEND_LEFT_ARROW())
								) ;
		return fst ;
	}

	// Hulden's
	// define EPContextR(X) [EPEXTEND ?* & X ?*] ;
	// used for interpreting epenthesis rules (at least right-arrow epenthesis rules)
	// modify for left-arrow rules
	// define EPContextR_LEFT_ARROW(X) [EPEXTEND_LEFT_ARROW ?* & X ?*] ;
	public Fst EPContextR_LEFT_ARROW(Fst X) {
		Fst fst = lib.Intersect(	lib.Concat(	EPEXTEND_LEFT_ARROW(), lib.UniversalLanguageFst()),
									lib.Concat(	X,        lib.UniversalLanguageFst())
								) ;
		return fst ;
	}



	// *********************************************************************

	// given a one-tape language lang, return the two-tape language that has
	// lang on tape 1

	// Hulden's 
	// define Tape1of2(X) [X .o. [? 0:?]*].l ;

	private Fst Tape1of2(Fst X) {
		// argument should be an Acceptor
		// if (!lib.IsAcceptor(lang.getFstPtr()))

		Fst temp = lib.KleeneStar(lib.Concat(
										lib.OneArcFst(lib.otherIdSym),
										lib.OneArcFst(lib.Epsilon, lib.otherNonIdSym)
										)
								) ;
		return lib.OutputProjection(lib.Compose(X, temp)) ;
	}

	// given a one-tape language lang, return the two-tape language that has
	// lang on tape 2

	// Hulden's 
	// define Tape2of2(X) [X .o. [0:? ?]*].l ;

	private Fst Tape2of2(Fst X) {
		// argument should be an Acceptor
		// if (!lib.IsAcceptor(lang.getFstPtr()))

		Fst temp = lib.KleeneStar(lib.Concat(
										lib.OneArcFst(lib.Epsilon, lib.otherNonIdSym),
										lib.OneArcFst(lib.otherIdSym)
										)
								) ;
		return lib.OutputProjection(lib.Compose(X, temp)) ;
	}

	// ***********************************************************************

	// Hulden:  Lower() means: a string on tape 3, ignoring possible (hard) epsilons
	// (@0@), and taking into account that a symbol on tape 3 may actually be
	// represented as @ID@, in which case we peek at the symbol on tape 2 in the
	// same location

	// KRB:  Lower(X) represents a triple string/language, with the symbols in
	// argument X (a triple string/language) generally on tape 3, possibly
	// interspersed with hard epsilons, and where the tape 3 symbol is @ID@
	// (indicating the id-mapping of the symbol on tape 2, leave the symbol from
	// X on tape 2 Elsewhere in the triples, any possible symbol can occur.

	// Hulden:
	// define Lower(X) [X .o.	[ 0:"@O@"	?			0:"@ID@"  # 'O' for outside
	//							| 0:ISyms	0:Tape2Sig	?
	//							| 0:ISyms	?			"@ID@
	//							| 0:ISyms	0:Tape2Sig	0:"@0@"		# hard epsilon
	//							]*
	//					].l ;   # take the lowerside

	public Fst Lower(Fst X) {
		Fst temp = lib.KleeneStar(
						lib.Union4Fsts(
							lib.Concat3Fsts(
									lib.OneArcFst(lib.Epsilon, outsideMarkerSym),
									lib.OneArcFst(lib.otherIdSym),
									lib.OneArcFst(lib.Epsilon, idMarkerSym)
							),
							lib.Concat3Fsts(
									lib.Crossproduct(
													lib.OneArcFst(lib.Epsilon),
													ISyms()),
									lib.Crossproduct(
													lib.OneArcFst(lib.Epsilon),
													Tape2Sig()),
									lib.OneArcFst(lib.otherIdSym)
							),
							lib.Concat3Fsts(
									lib.Crossproduct(
													lib.OneArcFst(lib.Epsilon),
													ISyms()),
									lib.OneArcFst(lib.otherIdSym),
									lib.OneArcFst(idMarkerSym)
							),
							lib.Concat3Fsts(
									lib.Crossproduct(
													lib.OneArcFst(lib.Epsilon),
													ISyms()),
									lib.Crossproduct(
													lib.OneArcFst(lib.Epsilon),
													Tape2Sig()),
									lib.OneArcFst(lib.Epsilon, hardEpsilonSym)
							)
						) 
					) ;
		return lib.OutputProjection(lib.Compose(X, temp)) ;
	}

	// Hulden:  the part of the definition that says
	// 0:"@O@"  ?  0:"@ID@
	// is the trick that makes Lower(X) consider the contents of tape 2 if it is
	// followed by an @ID@ on tape 3.

	// Hulden:  Upper(X) means: a 3-tape string with X on tape 2 
	// ignoring possible hard epsilons
	// (@0@)

	// Hulden:
	// define Upper(X) [X .o. 	[ 0:"@O@"	?		0:"@ID@"  # outside the action
	//							| 0:ISyms	?		0:Tape3Sig
	//							| 0:ISyms	0:"@0@"	0:[Tape3Sig-"@ID@"]
	//							]*
	//					].l ;

	public Fst Upper(Fst X) {
		Fst temp = lib.KleeneStar( 
					lib.Union3Fsts(
							lib.Concat3Fsts(
									lib.OneArcFst(lib.Epsilon, outsideMarkerSym),
									lib.OneArcFst(lib.otherIdSym),
									lib.OneArcFst(lib.Epsilon, idMarkerSym)
							),
							lib.Concat3Fsts(
									lib.Crossproduct(
											lib.OneArcFst(lib.Epsilon),
											ISyms()
									),
									lib.OneArcFst(lib.otherIdSym),
									lib.Crossproduct(
											lib.OneArcFst(lib.Epsilon),
											Tape3Sig()
									)
							),
							lib.Concat3Fsts(
									lib.Crossproduct(
											lib.OneArcFst(lib.Epsilon),
											ISyms()
									),
									lib.OneArcFst(lib.Epsilon, hardEpsilonSym),
									lib.Crossproduct(
										lib.OneArcFst(lib.Epsilon),
										lib.Difference(
												Tape3Sig(),
												lib.OneArcFst(idMarkerSym)
										)
									)
								)
							)
					) ;
		return lib.OutputProjection(lib.Compose(X, temp)) ;
	}

	// Hulden:
	// The reason I have a separate Upper() and Lower() function is because we
	// cannot use Tape2of3 or Tape3of3 to talk about actual strings that are on
	// tapes 2 and 3.  One reason is the presence of zeroes, and the other is that
	// the identity symbols on Tape 2 are marked as @ID@ on tape 3, so a
	// configuration such as @O@aa is really not possible (reflecting a symbol that
	// is in an identity relationship outside the action of a rule).  Instead it is
	// rendered as the triple @)@a@ID@.  Hence, when referring to the contents of
	// these tapes, this needs to be handled.

	// *********************************************************************

	// First symbol is [ and contains one more [ (longest match)
	
	// Hulden (OLD)
	// define Longest(X)	[Tape1of3(IOpen Tape1Sig* ["@O@"|IOpen] Tape1Sig*) &
	// Tape2of3(X/"@0@")] ;

	//public Fst Longest(Fst X) {
	//	return lib.Intersect(
	//				Tape1of3(
	//					lib.Concat4Fsts(
	//								IOpen(),
	//								lib.KleeneStar(Tape1Sig()),
	//								lib.Union(
	//									lib.OneArcFst(outsideMarkerSym),
	//									IOpen()
	//								),
	//								lib.KleeneStar(Tape1Sig())
	//					)
	//				),
	//				Tape2of3(ignoreFst(X, lib.OneArcFst(hardEpsilonSym)))
	//			) ;
	//}
	
	// new definition of Longest(X), Mans Hulden, 2013-01-18
	// define Longest(X)	[	Tape1of3(IOpen Tape1Sig* ["@O@"|IOpen] Tape1Sig*) 
	//						& 	Tape2of3(X/"@0@" - [?* "@0@"])
	//						] ;
	//
	//	2013-12-27 KRB I think there needs to be a LongestRightArrow (orig)
	//  and a new LongestLeftArrow for left-arrow rules

	// public Fst Longest(Fst X) {
	public Fst LongestRightArrow(Fst X) {
		return lib.Intersect(
					Tape1of3(
						lib.Concat4Fsts(
									IOpen(),
									lib.KleeneStar(Tape1Sig()),
									lib.Union(
										lib.OneArcFst(outsideMarkerSym),
										IOpen()
									),
									lib.KleeneStar(Tape1Sig())
						)
					),
					// Orig uses Tape2of3, Hulden suggests using Upper here
					//Tape2of3(lib.Difference(ignoreFst(X, lib.OneArcFst(hardEpsilonSym)),
					// YES.  Upper seems to work here
					Upper(lib.Difference(ignoreFst(X, lib.OneArcFst(hardEpsilonSym)),
											lib.Concat(	lib.KleeneStar(lib.OneArcFst(lib.otherIdSym)),
														lib.OneArcFst(hardEpsilonSym)
													  )
							
											)
					)
				) ;
	}

	public Fst LongestLeftArrow(Fst X) {
		return lib.Intersect(
					Tape1of3(
						lib.Concat4Fsts(
									IOpen(),
									lib.KleeneStar(Tape1Sig()),
									lib.Union(
										lib.OneArcFst(outsideMarkerSym),
										IOpen()
									),
									lib.KleeneStar(Tape1Sig())
						)
					),
					// difference here, with Tape3of3 instead of Tape2of3
					// Hulden suggests using Lower
					//Tape3of3(lib.Difference(ignoreFst(X, lib.OneArcFst(hardEpsilonSym)),
					// YES, Lower works here, Tape3of3 does not
					Lower(lib.Difference(ignoreFst(X, lib.OneArcFst(hardEpsilonSym)),
											lib.Concat(	lib.KleeneStar(lib.OneArcFst(lib.otherIdSym)),
														lib.OneArcFst(hardEpsilonSym)
													  )
							
											)
					)
				) ;
	}


	// First symbol is not [ and string contains [ (leftmost match)
	//
	// Beesley:  Leftmost(X) is used in Constraints when compiling
	// max or min rules:
	// define Constraints NotContain( Upper(L) Unrewritten(A-0) Upper(R)
	// 
	// define Constraints NotContain( Upper(L) (Unrewritten(A-0) |
	//                                          Leftmost(A-0)    |
	//                                          Longest(A-0)     ) Upper(R)
	// So, between contexts, don't allow A to be unrewritten
	//                       don't allow A to be other than Leftmost
	//                       don't allow A to be other than Longest

	// Hulden
	// define Leftmost(X)	[Upper(X) & Tape1of3("@O@" ?* IOpen ?*) ] ;
	// imposes a restriction based on tape 1
	//
	// 2014-01-01 
	//public Fst Leftmost(Fst x) {
	public Fst LeftmostRightArrow(Fst x) {
		return lib.Intersect(
					Upper(x),
					Tape1of3(lib.Concat4Fsts(
										lib.OneArcFst(outsideMarkerSym),
										lib.KleeneStar(lib.OneArcFst(lib.otherIdSym)),
										IOpen(),
										lib.KleeneStar(lib.OneArcFst(lib.otherIdSym))
							)
					)
			) ;
	}

	public Fst LeftmostLeftArrow(Fst x) {
		return lib.Intersect(
					Lower(x),
					Tape1of3(lib.Concat4Fsts(
										lib.OneArcFst(outsideMarkerSym),
										lib.KleeneStar(lib.OneArcFst(lib.otherIdSym)),
										IOpen(),
										lib.KleeneStar(lib.OneArcFst(lib.otherIdSym))
							)
					)
			) ;
	}


	// Beesley
	// first attempt to define Rightmost(X), would be
	// define Rightmost(X) [ Upper(X) & Tape1of3(?* IClose ?* "@O@")] ;
	// wrote to Hulden 2012-10-26; seems to work, but it now looks like Longest(X)
	// needs a LongestR2L(X) variant that I haven't figured out yet.
	//
	// 2013-12-27 KRB  I think that there needs to be a separate RightmostRightArrow (orig)
	// and a new RightmostLeftArrow for left-arrow rules
	
	//public Fst Rightmost(Fst x) {
	public Fst RightmostRightArrow(Fst x) {
		return lib.Intersect(
				Upper(x),
				Tape1of3(lib.Concat4Fsts(
									lib.KleeneStar(lib.OneArcFst(lib.otherIdSym)),
									IClose(),
									lib.KleeneStar(lib.OneArcFst(lib.otherIdSym)),
									lib.OneArcFst(outsideMarkerSym)
									)
					    )
				) ;
	}

	public Fst RightmostLeftArrow(Fst x) {
		return lib.Intersect(
				Lower(x),
				Tape1of3(lib.Concat4Fsts(
									lib.KleeneStar(lib.OneArcFst(lib.otherIdSym)),
									IClose(),
									lib.KleeneStar(lib.OneArcFst(lib.otherIdSym)),
									lib.OneArcFst(outsideMarkerSym)
									)
					    )
				) ;
	}


	// Last symbol is not ] (shortest match)

	// Hulden
	// define Shortest(X)	[Tape1of3("@I[@" \IClose*) & Tape2of3(X)] ;
	//
	// 2013-12-27 KRB I think that there needs to be a ShortestRightArrow (orig)
	// and a new ShortestLeftArrow for left-arrow rules

	// public Fst Shortest(Fst X) {
	public Fst ShortestRightArrow(Fst X) {
		return lib.Intersect(
					Tape1of3(lib.Concat(
								lib.OneArcFst(IOpenSym),
								lib.KleeneStar(lib.SymbolComplement(IClose()))
								)
					),
					// 2014-01-01 Hulden suggests use of Upper instead of Tape2of3,
					// seems to work for LongestRightArrow
					//Tape2of3(X)
					Upper(X)
				) ;
	}

	public Fst ShortestLeftArrow(Fst X) {
		return lib.Intersect(
					Tape1of3(lib.Concat(
								lib.OneArcFst(IOpenSym),
								lib.KleeneStar(lib.SymbolComplement(IClose()))
								)
					),
					// difference here, Tape3of3 instead of Tape2of3
					// 2014-01-01 Hulden suggests use of Lower here
					//Tape3of3(X)
					Lower(X)
				) ;
	}


	// An upper string (tape 2) X which is completely aligned with @O@ symbols,
	// i.e. no part of it is rewritten

	// Hulden
	// define Unrewritten(X)	[X .o. [0:"@O@" ? 0:?]* ].l ;
	//
	// KRB 2014-01-01 this works for both right-arrow and left-arrow rules
	// because when tape 1 has @O@ ("outside"), and tape 2 has a symbol,
	// then tape 3 has @ID@
	//
	public Fst Unrewritten(Fst X) {
		return lib.OutputProjection(lib.Compose(
									X,
									lib.KleeneStar(lib.Concat3Fsts(
													lib.OneArcFst(lib.Epsilon, outsideMarkerSym),
													lib.OneArcFst(lib.otherIdSym),
													lib.OneArcFst(lib.Epsilon, lib.otherNonIdSym)
													)
												)
									)
							) ;
	}

	// The language of all triple strings that do not contain triple string X

	// Hulden
	// define NotContain(X) ~[[Tape1Sig Tape2Sig Tape3Sig]* X ?*] ;

	public Fst NotContain(Fst X) {
		return lib.Complement(lib.Concat3Fsts(
										lib.KleeneStar(lib.Concat3Fsts(Tape1Sig(), 
																Tape2Sig(), 
																Tape3Sig())),
										X,
										lib.KleeneStar(lib.OneArcFst(lib.otherIdSym))
										)
							) ;
	}

	// CP(X,Y) yields the cross-product of two languages X and Y placed so that
	// tape 1 contains the relevant marker symbols and tapes 2 and 3 contains the
	// aligned strings from X and Y.
	// Example: CP(a, b c) yields the string "@I[@ a b @I]@ 0 c" which is
	// @I[@	@I]@
	//  a    0
	//	b	 c
	// in column format

	// Hulden
	// define Align2(X,Y) Tape1of2(X "@0@"*) 
	//					& Tape2of2(Y "@0@"*) 
	//  			    & ~[?* "@0@" "@0@" ?*] ;

	// Align2(X, Y) is called from CP(), see below
	private Fst Align2(Fst X, Fst Y) {
		return lib.Intersect3Fsts(
						Tape1of2(lib.Concat(
									X,
									lib.KleeneStar(lib.OneArcFst(hardEpsilonSym))
									)
						),
						Tape2of2(lib.Concat(
									Y,
									lib.KleeneStar(lib.OneArcFst(hardEpsilonSym))
								)
						),
						lib.Complement(lib.Concat4Fsts(
											lib.KleeneStar(lib.OneArcFst(lib.otherIdSym)),
											lib.OneArcFst(hardEpsilonSym),
											lib.OneArcFst(hardEpsilonSym),
											lib.KleeneStar(lib.OneArcFst(lib.otherIdSym))
									)
						)
					) ;
	}

	//define CP(X,Y) 	Tape1of3(ISyms*) & 
	//					Tape23of3(Align2(X,Y)) & 
	//					[ 	"@I[@"  ? ? 
	//						["@I@" ? ?]* 
	//						"@I]@" ? ?
	//
	//					| 	"@I[]@" ? ?
	//
	//					| 	0
	//					] ;


	public Fst CP(Fst X, Fst Y) {
		Fst align2 = Align2(X, Y) ;

		Fst resultFst = lib.Intersect3Fsts(

				Tape1of3(lib.KleeneStar(ISyms())),

				Tape23of3(align2),
				
				lib.Union3Fsts(
					lib.Concat3Fsts(
						lib.Concat3Fsts(
							lib.OneArcFst(IOpenSym),
							lib.OneArcFst(lib.otherIdSym),
							lib.OneArcFst(lib.otherIdSym)
						),
						lib.KleeneStar(lib.Concat3Fsts(
										lib.OneArcFst(ISym),
										lib.OneArcFst(lib.otherIdSym),
										lib.OneArcFst(lib.otherIdSym)
										)
						),
						lib.Concat3Fsts(
							lib.OneArcFst(ICloseSym),
							lib.OneArcFst(lib.otherIdSym),
							lib.OneArcFst(lib.otherIdSym)
						)
					),
					lib.Concat3Fsts(
							lib.OneArcFst(IOpenAndCloseSym),
							lib.OneArcFst(lib.otherIdSym),
							lib.OneArcFst(lib.otherIdSym)
					),
					lib.OneArcFst(lib.Epsilon)
				)
			) ;
		return resultFst ;
	}

	// to flatten a transducer in a transducer rule
	public Fst CPflatten(Fst T) {
		int hardEpsilonSymVal = symmap.putsym(hardEpsilonSym) ;
				// Use Flatten4Rule here instead of Align2
		Fst flatBigram = lib.Flatten4Rule(T, hardEpsilonSymVal) ;

		Fst resultFst = lib.Intersect3Fsts(
				Tape1of3(lib.KleeneStar(ISyms())),
				Tape23of3(flatBigram),
				lib.Union3Fsts(
					lib.Concat3Fsts(
						lib.Concat3Fsts(
							lib.OneArcFst(IOpenSym),
							lib.OneArcFst(lib.otherIdSym),
							lib.OneArcFst(lib.otherIdSym)
						),
						lib.KleeneStar(lib.Concat3Fsts(
										lib.OneArcFst(ISym),
										lib.OneArcFst(lib.otherIdSym),
										lib.OneArcFst(lib.otherIdSym)
										)
						),
						lib.Concat3Fsts(
							lib.OneArcFst(ICloseSym),
							lib.OneArcFst(lib.otherIdSym),
							lib.OneArcFst(lib.otherIdSym)
						)
					),
					lib.Concat3Fsts(
							lib.OneArcFst(IOpenAndCloseSym),
							lib.OneArcFst(lib.otherIdSym),
							lib.OneArcFst(lib.otherIdSym)
					),
					lib.OneArcFst(lib.Epsilon)
				)
			) ;
		return resultFst ;
	}

	// Hulden
	// define DeleteFirstSymbol(X) [X .o. [?:0  ?*]].l ;

	public Fst DeleteFirstSymbol(Fst X) {
		return lib.OutputProjection(lib.Compose(
								X,
								lib.Concat(
									lib.OneArcFst(lib.otherNonIdSym, lib.Epsilon),
									lib.KleeneStar(lib.OneArcFst(lib.otherIdSym))
								)
							 )
						   ) ;
	}

	// Hulden, message 2012-09-25
	// added AlignMarkup() and CPMarkup, parallel to Align2() and CP() above, for use
	// in compiling markup rules, e.g.   a b ->  x ... y /  left _ right
	// or in general                       X ->  Y ... Z
	//
	// define AlignMarkup(X,Y,Z) [ Tape1of2("@0@"*) & Tape2of2(Y) ]
	// 							 [ Tape1of2(X) & Tape2of2("@ID@"*) ]
	// 							 [ Tape1of2("@0@"*) & Tape2of2(Z) ] ;
	//
	// N.B. Hulden's orig. formulas work for right-arrow rules   X -> Y ... Z
	// need a modification for left-arrow  Y ... Z <-  X    see below
	
	private Fst AlignMarkupRightArrow(Fst X, Fst Y, Fst Z) {
		return lib.Concat3Fsts	(
					lib.Intersect(
									Tape1of2(lib.KleeneStar(lib.OneArcFst(hardEpsilonSym))),
									Tape2of2(Y)
							     ),
					lib.Intersect(
									Tape1of2(X),
									Tape2of2(lib.KleeneStar(lib.OneArcFst(idMarkerSym)))
								 ),
					lib.Intersect(
									Tape1of2(lib.KleeneStar(lib.OneArcFst(hardEpsilonSym))),
									Tape2of2(Z)
						         )
							) ;
	}

	// here the X is still the input (now from the lower side)
	// the Y is still the left insertion
	// the Z is still the right insertion

	private Fst AlignMarkupLeftArrow(Fst X, Fst Y, Fst Z) {
		return lib.Concat3Fsts	(
					lib.Intersect(
									// here the Y is on tape 1 of 2 (later tape 2 of 3)
									Tape1of2(Y),
									Tape2of2(lib.KleeneStar(lib.OneArcFst(hardEpsilonSym)))
							     ),
					lib.Intersect(
									// I don't think this changes, still X above ID
									Tape1of2(X),
									Tape2of2(lib.KleeneStar(lib.OneArcFst(idMarkerSym)))
								 ),
					lib.Intersect(
									// here the Z is on tape 1 of 2 (later tape 2 of 3)
									Tape1of2(Z),
									Tape2of2(lib.KleeneStar(lib.OneArcFst(hardEpsilonSym)))
						         )
							) ;
	}


	//
	// Hulden, message 2012-09-25, also rewriteexample13.script
	// define CPMarkup(X,Y,Z)	Tape1of3(ISyms*)
	// 						&   Tape23of3(AlignMarkup(X,Y,Z))
	// 						&	[	"@I[@" ? ?
	// 							    [ "@I@" ? ? ]*
	// 							    "@I]@" ? ?
	//
	// 							|   "@I[]@" ? ?
	//
	// 							|	0
	// 							]
	// 						;
	//
	// N.B. Hulden's formulas for work right-arrow rules   X  ->  Y ... Z
	// need a modification for left-arrow rules   Y ... Z  <-  X   see below

	public Fst CPMarkupRightArrow(Fst X, Fst Y, Fst Z) {
		Fst resultFst = lib.Intersect3Fsts(
				// 1
				Tape1of3(lib.KleeneStar(ISyms())),
				// 2
				Tape23of3(AlignMarkupRightArrow(X, Y, Z)),
				// 3
				lib.Union3Fsts(
					// 1
					lib.Concat3Fsts(
						lib.Concat3Fsts(
							lib.OneArcFst(IOpenSym),
							lib.OneArcFst(lib.otherIdSym),
							lib.OneArcFst(lib.otherIdSym)
						),
						lib.KleeneStar(lib.Concat3Fsts(
										lib.OneArcFst(ISym),
										lib.OneArcFst(lib.otherIdSym),
										lib.OneArcFst(lib.otherIdSym)
										)
						),
						lib.Concat3Fsts(
							lib.OneArcFst(ICloseSym),
							lib.OneArcFst(lib.otherIdSym),
							lib.OneArcFst(lib.otherIdSym)
						)
					),
					// 2
					lib.Concat3Fsts(
							lib.OneArcFst(IOpenAndCloseSym),
							lib.OneArcFst(lib.otherIdSym),
							lib.OneArcFst(lib.otherIdSym)
					),
					// 3
					lib.OneArcFst(lib.Epsilon)
				)
			) ;
		return resultFst ;
	}


//  For left-arrow markup rules    Y ... Z <-  X
//  X is still the input (here on the lower side)
//  Y is still the left insertion
//  Z is still the right insertion
//define CPMarkupLeftArrow(X,Y,Z)		Tape1of3(ISyms*)
//								&   Tape23of3(AlignMarkupLeftArrow(X,Y,Z))
//								&	[     "@I[@" ? ?
//							    	    [ "@I@"  ? ? ]*
// 							    	      "@I]@" ? ?
//
// 									|   "@I[]@" ? ?
//
// 									|	0
// 									]
// 								;

	// X is still the input (here on the lower side)
	// Y is still the left insertion
	// Z is still the right insertion
	public Fst CPMarkupLeftArrow(Fst X, Fst Y, Fst Z) {
		Fst resultFst = lib.Intersect3Fsts(
				// 1
				Tape1of3(lib.KleeneStar(ISyms())),
				// 2
				Tape23of3(AlignMarkupLeftArrow(X, Y, Z)),
				// 3
				lib.Union3Fsts(
					lib.Concat3Fsts(
						lib.Concat3Fsts(
							lib.OneArcFst(IOpenSym),
							lib.OneArcFst(lib.otherIdSym),
							lib.OneArcFst(lib.otherIdSym)
						),
						lib.KleeneStar(lib.Concat3Fsts(
										lib.OneArcFst(ISym),
										lib.OneArcFst(lib.otherIdSym),
										lib.OneArcFst(lib.otherIdSym)
										)
						),
						lib.Concat3Fsts(
							lib.OneArcFst(ICloseSym),
							lib.OneArcFst(lib.otherIdSym),
							lib.OneArcFst(lib.otherIdSym)
						)
					),

					lib.Concat3Fsts(
							lib.OneArcFst(IOpenAndCloseSym),
							lib.OneArcFst(lib.otherIdSym),
							lib.OneArcFst(lib.otherIdSym)
					),

					lib.OneArcFst(lib.Epsilon)
				)
			) ;
		return resultFst ;
	}


	// Hulden
	// define Boundary  "@O@"  "@#@"  "@ID@"  ;
	// N.B. a triple

	public Fst Boundary() {
		return lib.Concat3Fsts(
						lib.OneArcFst(outsideMarkerSym),
						lib.OneArcFst(ruleWordBoundarySym),
						lib.OneArcFst(idMarkerSym)
					) ;
	}

	public Fst NotRestDelimStarFst() {
		int restDelimCpv = symmap.putsym(restDelimSym) ;

		// for (. - restDelim)* (\x* in Hulden's description);
		// start with a UniversalLanguageFst
		// then just add restDelimCpv to the sigma

		// semiring generalization point

		Fst notRestDelimStar = lib.UniversalLanguageFst() ;
		notRestDelimStar.getSigma().add(restDelimCpv) ;
		return notRestDelimStar ;
	}

	public Fst CleanupRule(Fst rule) {

		// see message 1 May 2009 from Mans Hulden

		// Hulden
		// define RemoveBoundary ["@O@":0 "@#@":0 "@ID@":0 ?* "@O@":0 "@#@":0 "@ID@":0] ;

		Fst RemoveBoundary = lib.Concat7Fsts(
									lib.OneArcFst(outsideMarkerSym, lib.Epsilon),
									lib.OneArcFst(ruleWordBoundarySym, lib.Epsilon),
									lib.OneArcFst(idMarkerSym, lib.Epsilon),
									lib.KleeneStar(lib.OneArcFst(lib.otherIdSym)),
									lib.OneArcFst(outsideMarkerSym, lib.Epsilon),
									lib.OneArcFst(ruleWordBoundarySym, lib.Epsilon),
									lib.OneArcFst(idMarkerSym, lib.Epsilon)
							) ;

		// Hulden
		// define PreProcess 	~[?^3*  ?  "@0@"  "@ID@"  ?*]
		//					.o.	~$[%>]
		//					.o.	[ [?:0  ?  "@ID@":0]
		//						| [?:0  ?  0:%>  \"@ID@"]
		//						]* ;

		Fst PreProcess = 
			lib.Compose3Fsts(
				lib.Complement(lib.Concat5Fsts(
									lib.KleeneStar(lib.Concat3Fsts(
														lib.OneArcFst(lib.otherIdSym),
														lib.OneArcFst(lib.otherIdSym),
														lib.OneArcFst(lib.otherIdSym)
											 	)
									),
									lib.OneArcFst(lib.otherIdSym),
									lib.OneArcFst(hardEpsilonSym),
									lib.OneArcFst(idMarkerSym),
									lib.KleeneStar(lib.OneArcFst(lib.otherIdSym))
								)
				), 

				notContainsFst(lib.OneArcFst(ruleRightAngleSym)),

				lib.KleeneStar(
						lib.Union(
							lib.Concat3Fsts(
									lib.OneArcFst(lib.otherNonIdSym, lib.Epsilon),
									lib.OneArcFst(lib.otherIdSym),
									lib.OneArcFst(idMarkerSym, lib.Epsilon)
							) ,
							lib.Concat4Fsts(
									lib.OneArcFst(lib.otherNonIdSym, lib.Epsilon),
									lib.OneArcFst(lib.otherIdSym),
									lib.OneArcFst(lib.Epsilon, ruleRightAngleSym),
									lib.SymbolComplement(lib.OneArcFst(idMarkerSym))
							)
						)
				)
			) ;
			
		Fst ResultFst = lib.OutputProjection(
							lib.Compose3Fsts(	rule,
												RemoveBoundary,
												PreProcess
											)
						) ;

		// call native function to synchronize ab -> cd,
		// now a subpath that looks like  a > c b > d

		lib.SynchronizeAltRuleInPlace(ResultFst,
								  symmap.getint(ruleRightAngleSym),
								  symmap.getint(hardEpsilonSym)) ;

		SubstEpsilonInPlace(ResultFst, idMarkerSym) ;
		SubstEpsilonInPlace(ResultFst, hardEpsilonSym) ;
		SubstEpsilonInPlace(ResultFst, ISym) ;
		SubstEpsilonInPlace(ResultFst, IOpenSym) ;
		SubstEpsilonInPlace(ResultFst, IOpenAndCloseSym) ;
		SubstEpsilonInPlace(ResultFst, ICloseSym) ;
		SubstEpsilonInPlace(ResultFst, outsideMarkerSym) ;
		SubstEpsilonInPlace(ResultFst, ruleWordBoundarySym) ;
		SubstEpsilonInPlace(ResultFst, ruleRightAngleSym) ;

		lib.OptimizeInPlace(ResultFst) ;

		return ResultFst ;
	}
}
