
//	Environment.java
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

import javax.swing.JDesktopPane ;

public class Environment {
    private Frame currentFrame ;
	private Frame globalFrame ;
	private JDesktopPane desktop = null ;

    // Only one Environment object is allocated in the main
    // program.  e.g.
    // Environment env = new Environment() ;
    // Then this unique env keeps track of the currentFrame, and the
    // interpreter calls env.put() and env.get() for putting and
    // getting.
	//
	
    /* Constructor:  only one Environment is allocated for each 
       Kleene parser */

    public Environment () {
		currentFrame = null ;  	// initial null value marks the end of the 
								// linked list of Frames in the environment
    }

    public Frame getCurrentFrame() {
		return currentFrame ;
    }

	public void setDesktop(JDesktopPane jdp) {
		desktop = jdp ;
	}

	public JDesktopPane getDesktop() {
		return desktop ;
	}

    // The staticMother link is the one to follow for the lookup in
    // the environment; the dynamic mother is the Frame of the
    // calling environment--when the function returns, the
    // dynamicMother Frame will again be re-made the currentFrame

	
	// Allocate a new daughter frame, specifying the staticMother
	// and the dynamicMother

    public void allocateFrame(Frame staticMother, Frame dynamicMother) {
		currentFrame = new Frame(staticMother, dynamicMother) ;
    }

	// The following method is used to allocate the global Frame
	//
	public void allocateGlobalFrame() {
		currentFrame = new Frame(currentFrame, currentFrame) ;
		globalFrame = currentFrame ;
	}

    // The following method is used to allocate additional (non-global) frames 
	// when Kleene
	// is first launched (currently just the "program" Frame);
	// It's also used to allocate a new Frame for evaluating a
	// stand_alone_block of statements.  In these cases, both mother
	// links are set to the currentFrame.

    public void allocateFrame() {
		// arguments are staticMother, dynamicMother
		// follow the staticMother chain for lookup
		currentFrame = new Frame(currentFrame, currentFrame) ;
    }

    // The following method is used to allocate a new Frame
	// for evaluating function calls.  In this case, the staticMother
	// link is the Frame in which the function was _defined_.  This
	// implements lexical scope.
	//
    public void allocateFrame(Frame staticFrame) {
		// arguments are staticMother, dynamicMother
		// follow the staticMother chain for lookup;
		// when the function terminates, and its Frame is released,
		// the dynamic mother Frame
		// becomes the currentFrame again (see releaseFrame() below)
		currentFrame = new Frame(staticFrame, currentFrame) ;
    }

    public void releaseFrame() {
		// A Frame is released when a function returns.
		// Reset currentFrame to the Frame referenced by the
		//  dynamicMother; in the case of a function call,
		//  the dynamicMother is the frame from which the function
		//  was called.
		currentFrame = currentFrame.getDynamicMother() ;
		// the released frame will be garbage collected
		//   (when appropriate--i.e. if no other refs to it)
    }

	// markExternalInCurrentFrame() called during evaluation of
	// external_statement, e.g.
	// external $foo ;
	// which (syntactically constrained) can appear only at the
	// start of a stand_alone_block or a func_block
	// Here $foo is always bound to an ExternValue in the currentFrame,
	// not any higher frame.  The ExternValue object contains a handle to
	// the higher frame where the variable is bound.

	public void markExternalInCurrentFrame(String key) {
		// First make sure that there is a binding of the key
		// in a "higher" Frame, on the static path,
		// otherwise the external declaration
		// makes no sense.
		if (currentFrame.containsKey(key)) {
			// attempt to declare a formal parameter in a function
			// block as external
			throw new SymtabException("Attempt to declare a formal parameter as external: " + key) ;
		}

		Frame frame = frameContainingKey1(key, currentFrame.getStaticMother()) ;
		if (frame == null) {
			throw new SymtabException("Attempt to declare " + key + " external when there is no external binding of that name.") ;
		}

		// In the currentFrame, bind the key to an instance of ExternValue
		//  containing a handle to the higher Frame containing the
		//  existing external binding.
		currentFrame.put(key, new ExternValue(frame)) ;
	}

	public void putGlobal(String key, Object value) {
		put1(key, value, globalFrame) ;
	}

    // The Kleene interpreter has env, a handle to the unique
    // environment, and it calls env.put(key, value) when an
	// assignment statement is evaluated, e.g.
	// $foo = a*b+[c-g] ;
	// Latest complication: if the key is bound to an ExternValue in the
	// currentFrame (because it was declared external inside
	// a stand_alone_block) then we need to re-set the already
	// existing binding of key in some higher Frame.
	// Normally, set or reset the binding in the currentFrame.
	//
	public void put(String key, Object value) {
		put1(key, value, currentFrame) ; 
		// normal put in currentFrame, unless
		//     1.  the key was previously declared external, or
		//     2.  the key was previously referenced as a free (non-local)
		//     variable
	}

    private void put1(String key, Object value, Frame frame) {
		// generally, should bind the key to the value in the currentFrame
		// (i.e. as a local variable), but see putGlobal()

		if (frame.containsKey(key)) {
			// there is already a binding in this frame

			// retrieve the value
			Object obj = frame.get(key) ;

			if (obj instanceof ExternValue) {
				// Then the key was overtly declared, in this frame 
				// (syntactically, inside a stand_alone_block or func_block),
				// as external, i.e. not local; so retrieve, from the
				// ExternValue, the higher Frame where key is already bound 
				// to a real (not ExternValue) value.
		
				Frame higherFrame = ((ExternValue) obj).getFrame() ;
				if (!higherFrame.containsKey(key)) {
					// it should
					throw new SymtabException("ExternValue frame found not to contain key: " + key) ;
				}
				// re-set the binding of key in this higher frame
				put1(key, value, higherFrame) ;
			} else if (obj instanceof FreeVariable) {
				// Then the key was not declared external, but it was
				// already referenced as a free (non-local)
				// variable to this block/scope; so the attempt here to
				// set key as a local variable is confusing at least and
				// probably an error.  E.g
				// {
				//     $foo = a b c $bar ;   // $bar is referenced as a free variable here
				//     ...
				//     $bar = xyz ;			// confusing attempt to declare/set
				//     						// $bar as a local variable
				// }
				//
				// Also in this case:
				// {
				//     $foo = a b $foo ;  // RHS $foo is free, LHS $foo is local
				// }
					
				throw new SymtabException("Attempt to bind " + key + " as a local variable when it has already been referenced as a free variable in the current func_block or a stand_alone_block.") ;
			} else {
				// just re-set the existing (normal) binding of key in the frame
				frame.put(key, value) ;
			}
		} else {
			// just add a new binding of key to value in this frame
			frame.put(key, value) ;
		}
    }

    // When trying to .get() something from the environment,
    // start at currentFrame and move, as necessary, up the static
    // environment chain until you find it, or reach null (not
    // found)  Note that lookup proceeds up the _static_ links,
	// which implements lexical scope.
    public Object get(String key) {
		return get1(key, currentFrame) ;
    }

    private Object get1(String key, Frame frame) {
		if (frame == null) {
			// no environment, or reached the end of the environment chain
			return null ;  // key not found in the whole environment
		} else if (frame.containsKey(key)) {
			// key is already bound to _something_ in this frame
			Object obj = frame.get(key) ;

			if (obj instanceof ExternValue) {
				// In a stand_alone_block or a func_block, the key was
				// declared external, e.g.
				// external $foo ;
				// so that code in this block can actually _change_
				// a non-local variable.  The ExternValue saves a
				// handle to the higher non-local frame where the variable
				// was already bound to a real object value (not another
				// ExternValue).
				Frame higherFrame = ((ExternValue) obj).getFrame() ;
				if (!higherFrame.containsKey(key)) {
					// Error: it should contain the key
					throw new SymtabException("ExternValue frame found not to contain key: " + key) ;
				}
				return higherFrame.get(key) ;
			} 
			if (obj instanceof FreeVariable) {
				// Then the key was not declared external, 
				// but it was already referenced as a 
				// free (non-local) variable; the FreeVariable holds a
				// handle to the Frame where the real binding was found. See
				// below.
				Frame higherFrame = ((FreeVariable) obj).getFrame() ;
				if (!higherFrame.containsKey(key)) {
					// Error: it should contain the key
					throw new SymtabException("FreeVariable frame found not to contain key: " + key) ;
				}
				return higherFrame.get(key) ;
			}
			// Else the obj just found in this frame is a "normal" object.

			// In the currentFrame, add a binding for key to FreeVariable;
			// where the FreeVariable stores a handle to the frame where
			// the real value was just found.
			if (frame != currentFrame) {
				// Then in the currentFrame, record that key was used
				// as a free (non-local) variable, and save a handle to this frame
				// where it was found.  This binding in currentFrame
				// will be used to block any subsequent attempt to bind key 
				// as a local variable in currentFrame.
				currentFrame.put(key, new FreeVariable(frame)) ;
			}
			return obj ;
		} else {
			// move up a level in the environment, following
			// the staticMother handle, to continue the search
			return get1(key, frame.getStaticMother()) ;
		}
    }

	public Frame frameContainingKey(String key) {
		return frameContainingKey1(key, currentFrame) ;
	}

	private Frame frameContainingKey1(String key, Frame frame) {
		if (frame == null) {
			// no environment, or reached the end of the environment chain
			return null ;  // key not found in the whole environment
		} else if (frame.containsKey(key)) {
			Object obj = frame.get(key) ;

			if (obj instanceof ExternValue) {
				Frame higherFrame = ((ExternValue) obj).getFrame() ;
				if (!higherFrame.containsKey(key)) {
					throw new SymtabException("Frame referenced by ExternValue does not contain binding: " + key) ;
				}
				return frameContainingKey1(key, higherFrame) ;
			}
			if (obj instanceof FreeVariable) {
				Frame higherFrame = ((FreeVariable) obj).getFrame() ;
				if (!higherFrame.containsKey(key)) {
					throw new SymtabException("Frame referenced by FreeVariable does not contain binding: " + key) ;
				}
				return frameContainingKey1(key, higherFrame) ;
			}
			// else the obj bound to key in this frame is a "normal" object;
			// return the _frame_
			return frame ;
		} else {
			// Not found in frame, so
			// move up a static level in the linked list of
			// Frames in the environment (following 
			// the staticMother handle, implementing "lexical scope")
			return frameContainingKey1(key, frame.getStaticMother()) ;
		}
	}

	// For Frame safety, remove(key) should normally work only on keys
	// bound in the currentFrame.  It would be inappropriate for a
	// function, for example, to be able to remove a key binding outside
	// of its own (local) scope.  The exception is if, in the currentFrame,
	// the key was overtly declared external (in an external_statement) in
	// the currentFrame.  Then it's appropriate to find the binding in
	// a "higher" Frame and delete it.
	//
	
	public Frame remove(String key) {
		return remove1(key, currentFrame) ;
	}

	private Frame remove1(String key, Frame frame) {
		if (frame.containsKey(key)) {
			Object obj = frame.get(key) ;

			if (obj instanceof ExternValue) {
				// In this frame, key was overtly declared to be external,
				// so it can be changed and deleted/removed.
				// Find the "higher" frame where key is bound
				Frame higherFrame = ((ExternValue) obj).getFrame() ;
				frame.remove(key) ; // remove the key-ExternValue binding in this frame
				// and restart at that higherFrame
				return remove1(key, higherFrame) ;
			} 
			if (obj instanceof FreeVariable) {
				// This attempt to delete a non-local variable is illegal.
				// (the non-local variable was used as a free variable, 
				// to retrieve its value, which is legal (and this caused
				// the key-FreeVariable entry to be added here; but such a
				// non-local variable cannot be changed or deleted unless it
				// was declared external.
				throw new SymtabException("Attempt to delete a non-local variable, not declared external: " + key) ;
			} 
			// else the obj should be a "normal" object;
			// remove it and pass back a handle to this frame
			frame.remove(key) ;
			return frame ;
		} else {
			throw new SymtabException("Attempt to delete non-existent key from the symbol table(s): " + key) ;
		}
	}

	// exportToDynamicMotherFrame() is called by the interpreter for
	// export_statements, e.g.
	// export $foo ;
	// $foo should be bound locally to a real value
	// (not ExternValue).  export statements are syntactically allowed
	// only inside a stand_alone_block, so there should always be a
	// dynamic mother frame of the currentFrame

	public void exportToDynamicMotherFrame(String key) {
		if (!currentFrame.containsKey(key)) {
			throw new SymtabException("Attempt to export a variable not bound in the local frame: " + key) ;
		} 

		if (currentFrame.get(key) instanceof ExternValue) {
			throw new SymtabException("Attempt to export a variable declared external in the local frame: " + key) ;
		} 
		
		// get the value of the variable in the current frame, and
		// set or re-set the key to that value in the dynamicMother Frame
		currentFrame.getDynamicMother().put(key, currentFrame.get(key)) ;
	}
}
