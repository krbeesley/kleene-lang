
//	EscapeXML.java
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

// This class was created because the Apache Commons class
// StringEscapeUtils, with method .escapeXml(String) used to escape all
// characters greater than 0x7F (i.e. beyond the ASCII range).  This
// is no longer done in 3.0 (still just "snapshots" in Dec 2009).

import java.text.CharacterIterator ;
import java.text.StringCharacterIterator ;

class EscapeXML {

	// clean but less efficient? see function below
	//static public String escapeXML(String str) {
    //    return str.replaceAll("&","&amp;")
    //             .replaceAll("<", "&lt;")
    //             .replaceAll(">", "&gt;")
    //             .replaceAll("\"", "&quot;")
    //             .replaceAll("'", "&apos;");
    //}  

    public static String escapeXML(String s) {
        StringBuilder result = new StringBuilder();
        StringCharacterIterator i = new StringCharacterIterator(s);
        char c =  i.current();
        while (c != CharacterIterator.DONE ){
            switch (c) {
                case '<':
                    result.append("&lt;");
                    break;
                case '>':
                    result.append("&gt;");
					break ;
                case '"':
                    result.append("&quot;");
					break ;
                case '\'':
                    result.append("&apos;");
					break ;
                case '&':
                    result.append("&amp;");
					break ;

				case '\n':
					result.append("&#x000A;") ;
					break ;
				case '\t':
					result.append("&#x0009;") ;
					break ;
				case '\b':
					result.append("&#x0008;") ;
					break ;
				case '\r':
					result.append("&#x000D;") ;
					break ;
				case '\f':
					result.append("&#x000C;") ;
					break ;

                default:
                    result.append(c);
					break ;
            }
            c = i.next();
        }
        return result.toString();
    }
}
