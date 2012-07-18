
//	ASTdec_float_literal.java
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

//	Hand-modified class to override the one automatically generated
//	by JavaCC/JJTree

public class ASTdec_float_literal extends SimpleNode {
    private String image ;

    public void setImage(String s) {
	image = s ;
    }

    public String getImage() {
	return image ;
    }

    // for better dump() results for nodes with String image stored
    // overrides method in SimpleNode.java

    public String toString() { return
              KleeneTreeConstants.jjtNodeName[id] + ": " + image ; } ;

    // inside Kleene, 'float' values are stored as double

    private double doubleValue ;

    public void setDoubleValue(float f) {
	doubleValue = (double) f ;
    }

    public void setDoubleValue(double d) {
	doubleValue = d ;
    }

    public double getDoubleValue() {
	return doubleValue ;
    }
    
    // the 'int' here is the node id
  public ASTdec_float_literal(int id) {
    super(id);
  }

  public ASTdec_float_literal(Kleene p, int id) {
    super(p, id);
  }

  /** Accept the visitor. **/
  public Object jjtAccept(KleeneVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
