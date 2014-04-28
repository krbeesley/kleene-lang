
//	RrKleeneVisitor.java
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

// when the syntax is $>foo = regexp ; the ASTregexp on the RHS is sent
// a message jjtAccept() to accept this Visitor.  It walks the
// ASTregexp, checking each rrprod_id to make sure it is in a proper
// right-recursive position (leading to a final state).  If all the rr
// refs are OK, then a HashSet of the names is returned

import java.util.HashSet ;

public class RrKleeneVisitor implements KleeneVisitor
{
  HashSet<String> hs = null ;

  public RrKleeneVisitor() {
	  hs = new HashSet<String>() ;
  }



  public Object visit(SimpleNode node, Object data) {
	  System.out.println("Error: call to visit(SimpleNode) method in RrKleenevisitor") ;
	  return data ;
  }

  public Object visit(ASTprogram node, Object data) {
	  // should never be possible inside a regexp
	  return data ;
  }	
  public Object visit(ASTquit_statement node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTempty_statement node, Object data) {
	  // no op
	  return data ;
  }
  public Object visit(ASTstand_alone_block node, Object data) {
	  // should never be possible inside a regexp
	  return data ;
  }	
  public Object visit(ASTif_else_block node, Object data) {
	  // should never be possible inside a regexp
	  return data ;
  }	
  public Object visit(ASTloop_block node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTfunc_block node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTnet_assignment node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTnet_id node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTiterator_net_id node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTiterator_num_id node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTrrprod_definition node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTrrprod_id node, Object data) {
	  String image = node.getImage() ;
	  if (((Boolean)data).booleanValue()) {
		  // rrprod_id is possible at this point (i.e. we
		  // are in a right-recursive position
		  hs.add(image) ;
	  } else {
		  throw new RrRefException("Right-recursive reference "
		  + image + " found in illegal position in a production.") ;
	  }
	  return data ;
  }
  public Object visit(ASTnum_assignment node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTnum_id node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTnet_func_assignment node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTnet_list_func_assignment node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTnum_list_func_assignment node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTvoid_func_assignment node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTnet_func_definition node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTnet_list_func_definition node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTnum_list_func_definition node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTvoid_func_definition node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTnum_func_assignment node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTnum_func_definition node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTnet_func_func_assignment node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTvoid_func_func_assignment node, Object data) {
	  // not possible
	  return data ;
  }

  public Object visit(ASTnet_func_func_definition node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTvoid_func_func_definition node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTnet_func_func_id node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTnet_list_func_func_id node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTvoid_func_func_id node, Object data) {
	  // leaf node
	  return data ;
  }

  public Object visit(ASTnum_func_func_assignment node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTnum_func_func_definition node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTnum_func_func_id node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTnum_list_func_func_id node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTnet_list_assignment node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTnum_list_assignment node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTif_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTboolean_test node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTif_part node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTelsif_part node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTelse_part node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTforeach_net_iteration_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTforeach_num_iteration_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTwhile_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTuntil_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTreturn_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTbreak_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTsap_rtn_conventions_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTcontinue_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTexternal_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTexport_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTdelete_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTdelete_all_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASToptimize_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTgarbage_collect_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTmemory_report_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTfsts_report_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTsymtab_report_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTgsymtab_report_statement node, Object data) {
	  // not possible
	  return data ;
  }


//  public Object visit(ASTdelete_selected_statement node, Object data) {
	  // not possible
//	  return data ;
//  }

	public Object visit(ASTrmEpsilon_statement node, Object data) {
		// not possible
		return data ;
	}
	public Object visit(ASTdeterminize_statement node, Object data) {
		// not possible
		return data ;
	}
	public Object visit(ASTminimize_statement node, Object data) {
		// not possible
		return data ;
	}
	public Object visit(ASTsynchronize_statement node, Object data) {
		// not possible
		return data ;
	}

  public Object visit(ASTdraw_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTsigma_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTtest_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTassert_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTrequire_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTprint_statement node, Object data) {
	  // not possible
	  return data ;
  }  
  public Object visit(ASTpr_statement node, Object data) {
	  // not possible
	  return data ;
  }  
  public Object visit(ASTexception_statement node, Object data) {
	  // not possible
	  return data ;
  }  
  public Object visit(ASTprintln_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTsys_print_statement node, Object data) {
	  // not possible
	  return data ;
  }  
  public Object visit(ASTsys_println_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTtestTokensTextFile_statement node, Object
  data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTtestTokensXMLFile_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTinfo_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTwritexml_statement node, Object data) {
	  // not possible?
	  return data ;
  }
  public Object visit(ASTwritexml_state_oriented_statement node, Object data) {
	  // not possible?
	  return data ;
  }
  public Object visit(ASTwritedot_statement node, Object data) {
	  // not possible?
	  return data ;
  }
  public Object visit(ASTsource_statement node, Object data) {
	  // not possible
	  return data ;
  }
  public Object visit(ASTnumexp node, Object data) {
	  // no rr ref can appear in an ASTnumexp
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTslice_exp node, Object data) {
	  // no rr ref can appear in an ASTslice_exp
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTboolean_or_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTboolean_and_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTboolean_not_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTless_than_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTless_than_or_equal_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTgreater_or_equal_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTequal_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnot_equal_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTgreater_than_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTaddition_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTsubtraction_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTmult_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTdiv_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTmod_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTunary_minus_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_func_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_func_id node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTnum_func_anon_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_func_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_list_func_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_func_func_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_func_func_anon_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_list_func_func_anon_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTdec_int_literal node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASThex_int_literal node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTdec_float_literal node, Object data) {
	  // leaf node
	  return data ;
  }

	// this Visitor is called on an ASTrrProdRHS
  public Object visit(ASTrrProdRHS node, Object data) {
	  // always one child, an ASTregexp
	  // initially called with data  Boolean(true)
	  node.childrenAccept(this, data) ;
	  return hs ;
  }

  public Object visit(ASTregexp node, Object data) {
	  node.childrenAccept(this, data) ;
	  return data ;
  }
  public Object visit(ASTcomposed_exp node, Object data) {
	  // nothing below this point can be a valid rr ref like $>name
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_parallel_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTrule_lhs node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTrule_lhs_transducer node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTrule_lhs_upper node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTrule_lhs_lower node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTrule_lhs_markup node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTleft_markup_insertion node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTright_markup_insertion node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTrule_right_arrow_oblig node, Object data) {
	  // this is a leaf node
	  return data ;
  }
  public Object visit(ASTrule_right_arrow_opt node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTrule_transducer_right_arrow_oblig node, Object data) {
	  // this is a leaf node
	  return data ;
  }
  public Object visit(ASTrule_transducer_right_arrow_opt node, Object data) {
	  // leaf node
	  return data ;
  }
 public Object visit(ASTrule_transducer_left_arrow_oblig node, Object data) {
	  // this is a leaf node
	  return data ;
  }
  public Object visit(ASTrule_transducer_left_arrow_opt node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTrule_right_arrow_max_l2r_oblig node, Object data) {
	  // leaf node
	  return data ;
  }  
  public Object visit(ASTrule_right_arrow_max_l2r_opt node, Object data) {
	  // leaf node
	  return data ;
  }  
  public Object visit(ASTrule_transducer_right_arrow_max_l2r_oblig node, Object data) {
	  // leaf node
	  return data ;
  }    
  public Object visit(ASTrule_transducer_right_arrow_min_l2r_oblig node, Object data) {
	  // leaf node
	  return data ;
  }  
  public Object visit(ASTrule_transducer_right_arrow_min_l2r_opt node, Object data) {
	  // leaf node
	  return data ;
  }  
  public Object visit(ASTrule_transducer_right_arrow_max_l2r_opt node, Object data) {
	  // leaf node
	  return data ;
  } 
  /*
  public Object visit(ASTrule_right_arrow_max_r2l_oblig node, Object data) {
	  // leaf node
	  return data ;
  }  
  */
  /*
  public Object visit(ASTrule_right_arrow_max_r2l_opt node, Object data) {
	  // leaf node
	  return data ;
  } 
  */
  public Object visit(ASTrule_right_arrow_min_l2r_oblig node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTrule_right_arrow_min_l2r_opt node, Object data) {
	  // leaf node
	  return data ;
  }
  /*
  public Object visit(ASTrule_right_arrow_min_r2l_oblig node, Object data) {
	  // leaf node
	  return data ;
  }
  */
  /*
  public Object visit(ASTrule_right_arrow_min_r2l_opt node, Object data) {
	  // leaf node
	  return data ;
  }
  */
  public Object visit(ASTrule_left_arrow_oblig node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTrule_left_arrow_opt node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTrule_left_arrow_max_l2r_oblig node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTrule_left_arrow_max_l2r_opt node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTrule_transducer_left_arrow_max_l2r_oblig node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTrule_transducer_left_arrow_min_l2r_oblig node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTrule_transducer_left_arrow_max_l2r_opt node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTrule_transducer_left_arrow_min_l2r_opt node, Object data) {
	  // leaf node
	  return data ;
  }
  /*
  public Object visit(ASTrule_left_arrow_max_r2l_oblig node, Object data) {
	  // leaf node
	  return data ;
  }
  */
  /*
  public Object visit(ASTrule_left_arrow_max_r2l_opt node, Object data) {
	  // leaf node
	  return data ;
  }
  */
  public Object visit(ASTrule_left_arrow_min_l2r_oblig node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTrule_left_arrow_min_l2r_opt node, Object data) {
	  // leaf node
	  return data ;
  }
  /*
  public Object visit(ASTrule_left_arrow_min_r2l_oblig node, Object data) {
	  // leaf node
	  return data ;
  }
  */
  /*
  public Object visit(ASTrule_left_arrow_min_r2l_opt node, Object data) {
	  // leaf node
	  return data ;
  }
  */


  public Object visit(ASTrestriction_exp node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTrestriction_lhs node, Object data) {
	  return data ;
  }
  // for rules that compile into transducers
  public Object visit(ASTrule_rhs node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  // for => restrictions that compile into acceptors
  public Object visit(ASTrestriction_rhs node, Object data) {
	  return data ;
  }
  // for rules that compile into transducers
  public Object visit(ASTone_level_rule_context node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  // for rules that compile into transducers
  public Object visit(ASTtwo_level_rule_context node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  // for => restrictions that compile into acceptors
  public Object visit(ASTrestriction_context node, Object data) {
	  return data ;
  }
  // for rules that compile into transducers
  public Object visit(ASTleft_rule_context node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  // for rules that compile into transducers
  public Object visit(ASTright_rule_context node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

  // for => restrictions that compile into acceptors
  public Object visit(ASTleft_restriction_context node, Object data) {
	  return data ;
  }
  // for => restrictions that compile into acceptors
  public Object visit(ASTright_restriction_context node, Object data) {
	  return data ;
  }
  public Object visit(ASTwhere_clauses node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTwhere_matched_clause node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTwhere_mixed_clause node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

  public Object visit(ASTelmt_of_net_list_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTunioned_exp node, Object data) {
	  boolean origRrAllow = ((Boolean)data).booleanValue() ;
	  int daughterCount = node.jjtGetNumChildren() ;
	  // walk each of the daughters with the origRrAllow value,
	  // e.g. it is possible to have a union of $>name references,
	  // and all can be right-recursive
	  for (int i = 0; i < daughterCount; i++) {
		  	node.jjtGetChild(i).jjtAccept(this, new
			Boolean(origRrAllow)) ;
	  }
	  return data ;
  }	
  public Object visit(ASTintersected_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTdifference_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTrand_input_statement node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTrand_output_statement node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTconcatenation_exp node, Object data) {
	  boolean origRrAllow = ((Boolean)data).booleanValue() ;
	  int daughterCount = node.jjtGetNumChildren() ;
	  // in every child but the last, a reference to $>whatever
	  // is not valid (rrAllow is false)
	  for (int i = 0; i < daughterCount - 1; i++) {
		  	node.jjtGetChild(i).jjtAccept(this, new Boolean(false)) ;
	  }
	  // the last daughter of a concatenation can be right-recursive,
	  // so pass along the original data value
	  node.jjtGetChild(daughterCount - 1).jjtAccept(this, new
	  Boolean(origRrAllow)) ;
	  return data ;
  }
  public Object visit(ASTcomplement_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTkleene_star node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTkleene_plus node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASToptional node, Object data) {
	  // allow  $>name? at the end of a production?
	  // much like an alternation. try passing along the
	  // input data for now
	  node.childrenAccept(this, data) ;
	  return data ;
  }
  public Object visit(ASTiterated_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTiteration_low_high node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTiteration_low node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTiteration_exact node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTiteration_high node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTweight_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTcrossproduct_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTlit_char node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTmultichar_symbol node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTsquare_bracket_multichar_symbol node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTdouble_quoted_string node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTdouble_quoted_char node, Object data) {
	  // char inside a double-quoted string
	  // leaf node
	  return data ;
  }
  public Object visit(ASTchar_union node, Object data) {
	  // [abc]
	  // leaf node
	  return data ;
  }
  public Object visit(ASTcomplement_char_union node, Object data) {
	  // [^abc]
	  // leaf node
	  return data ;
  }
  public Object visit(ASTchar_range node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTnet_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  } 
  public Object visit(ASTvoid_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }  
  public Object visit(ASTnet_reverse_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_invert_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_invert_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_flatten_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_flatten_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_flatten4rule_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_flatten4rule_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_optimize_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_optimize_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

  public Object visit(ASTnet_rmepsilon_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_rmepsilon_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_determinize_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_determinize_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_minimize_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_minimize_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_synchronize_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_synchronize_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_case_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_diac_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

// @@@@

  public Object visit(ASTnet_shortestPath_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_inputproj_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_inputproj_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_outputproj_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_outputproj_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_close_sigma_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_close_sigma_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_copy_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_rm_weight_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_rm_weight_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_subst_symbol_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_subst_symbol_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_eq_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_eq_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_read_xml_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_rand_gen_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_start_func_call node, Object data) {
	  // $>foo = a b $^start($>foo) c d ;  causes a recursion problem
	  // so don't allow $^start($>anything) inside the RHS of a
	  // production
	  if (true)
	  	throw new RrRefException("Illegal call to $^start() inside the RHS of a production.") ;
	  // perhaps be able to catch this in the parsing itself?
	  else
	  	return data ;
  }
  public Object visit(ASTlng_pathcount_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTlng_statecount_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTlng_arccount_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }  
  public Object visit(ASTlng_get_int_cpv_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTlng_arity_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

// boolean functions

  public Object visit(ASTlng_is_rtn_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTlng_is_cyclic_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTlng_is_ubounded_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTlng_is_lbounded_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTlng_is_acceptor_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTlng_is_transducer_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTlng_output_labels_include_cpv_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

  public Object visit(ASTlng_is_weighted_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

  public Object visit(ASTlng_is_ideterministic_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

  public Object visit(ASTlng_is_odeterministic_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

  public Object visit(ASTlng_is_epsilonfree_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

  public Object visit(ASTlng_is_empty_language_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

  public Object visit(ASTlng_is_empty_string_language_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

  public Object visit(ASTlng_contains_empty_string_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

  public Object visit(ASTlng_is_string_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

  public Object visit(ASTlng_contains_other_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }


/*
  public Object visit(ASTlng_is_universal_language_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
*/


  public Object visit(ASTlng_equivalent_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

  public Object visit(ASTlng_rand_equivalent_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

  public Object visit(ASTnum_abs_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_to_string_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_char_for_cpv_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_implode_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_explode_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_get_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_getlast_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_get_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_getlast_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_head_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_head_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_pop_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_pop_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_remove_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_removelast_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_remove_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_removelast_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_list_get_slice_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_list_get_slice_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_list_push_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_list_push_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_list_add_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_list_add_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_list_addat_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_list_addat_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_list_set_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_list_set_dest_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_list_tail_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_list_copy_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_list_tail_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_list_copy_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_list_get_sigma_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_list_get_sigma_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_get_net_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_sub_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_embed_rtn_subnets_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_expand_rtn_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTdbl_ceil_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTdbl_floor_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTlng_round_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTlng_long_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTlng_size_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTlng_is_empty_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTdbl_double_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTdbl_rint_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTdbl_log_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }









  public Object visit(ASTnet_func_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTvoid_func_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

  public Object visit(ASTnet_func_id node, Object data) {
	  // leaf node
	  return data ;
  }
   public Object visit(ASTvoid_func_id node, Object data) {
	  // leaf node
	  return data ;
  }

  public Object visit(ASTnet_func_anon_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTvoid_func_anon_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_func_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_list_func_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTvoid_func_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_func_func_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_list_func_func_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTvoid_func_func_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

  public Object visit(ASTnet_func_func_anon_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTvoid_func_func_anon_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
//  public Object visit(ASTarr_index_exp node, Object data) {
//	  node.childrenAccept(this, new Boolean(false)) ;
//	  return data ;
//  }
//  public Object visit(ASTnet_list_ref node, Object data) {
//	  node.childrenAccept(this, new Boolean(false)) ;
//	  return data ;
//  }
  public Object visit(ASTnet_list_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_list_id node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTnet_list_lit node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_list_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_list_func_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_list_func_id node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTnet_list_func_anon_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnet_list_func_func_anon_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
//  public Object visit(ASTnum_list_ref node, Object data) {
//	  node.childrenAccept(this, new Boolean(false)) ;
//	  return data ;
//  }
  public Object visit(ASTnum_list_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_list_id node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTnum_list_lit node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_list_func_call node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_list_func_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_list_func_func_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnum_list_func_id node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTnum_list_func_anon_exp node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTany node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTany_any node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTepsilon node, Object data) {
	  // leaf node
	  return data ;
  }
  public Object visit(ASTarg_list node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTpositional_args node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTnamed_args node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

  public Object visit(ASTparam_list node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASTrequired_params node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }
  public Object visit(ASToptional_params node, Object data) {
	  node.childrenAccept(this, new Boolean(false)) ;
	  return data ;
  }

	// the following with_assignment nodes found both in
  // param_list/optional_params and in
  // arg_list/named_args
	public Object visit(ASTnet_id_with_assignment node, Object data) {
		  node.childrenAccept(this, new Boolean(false)) ;
		return data ;
	}
	public Object visit(ASTnet_func_id_with_assignment node, Object data) {
		  node.childrenAccept(this, new Boolean(false)) ;
		return data ;
	}
	public Object visit(ASTvoid_func_id_with_assignment node, Object data) {
		  node.childrenAccept(this, new Boolean(false)) ;
		return data ;
	}

	public Object visit(ASTnet_func_func_id_with_assignment node, Object data) {
		  node.childrenAccept(this, new Boolean(false)) ;
		return data ;
	}
	public Object visit(ASTvoid_func_func_id_with_assignment node, Object data) {
		  node.childrenAccept(this, new Boolean(false)) ;
		return data ;
	}


	public Object visit(ASTnet_list_id_with_assignment node, Object data) {
		  node.childrenAccept(this, new Boolean(false)) ;
		return data ;
	}
	public Object visit(ASTnet_list_func_id_with_assignment node, Object data) {
		  node.childrenAccept(this, new Boolean(false)) ;
		return data ;
	}
	//public Object visit(ASTnet_list_func_func_id_with_assignment node, Object data) {
	//	  node.childrenAccept(this, new Boolean(false)) ;
	//	return data ;
	//}



	public Object visit(ASTnum_id_with_assignment node, Object data) {
		  node.childrenAccept(this, new Boolean(false)) ;
		return data ;
	}
	public Object visit(ASTnum_func_id_with_assignment node, Object data) {
		  node.childrenAccept(this, new Boolean(false)) ;
		return data ;
	}
	public Object visit(ASTnum_func_func_id_with_assignment node, Object data) {
		  node.childrenAccept(this, new Boolean(false)) ;
		return data ;
	}


	public Object visit(ASTnum_list_id_with_assignment node, Object data) {
		  node.childrenAccept(this, new Boolean(false)) ;
		return data ;
	}
	public Object visit(ASTnum_list_func_id_with_assignment node, Object data) {
		  node.childrenAccept(this, new Boolean(false)) ;
		return data ;
	}
	//public Object visit(ASTnum_list_func_func_id_with_assignment node, Object data) {
	//	  node.childrenAccept(this, new Boolean(false)) ;
	//	return data ;
	//}


}
