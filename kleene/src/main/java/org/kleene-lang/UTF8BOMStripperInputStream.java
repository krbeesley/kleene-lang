
//	UTF8BOMStripperInputStream.java
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

// Used to wrap the input stream when you know it's UTF-8, and so it might
// have a BOM on the front.  This class will skip the BOM, if it is present.

import java.io.InputStream ; 
import java.io.PushbackInputStream ;
import java.io.IOException ; 

public class UTF8BOMStripperInputStream extends PushbackInputStream {
	// just used for reading UTF8 files
	// checks only for possible UTF-8 BOM, which appears optionally
	// as 3 bytes at the beginning of the file
	//
	// The Constructor consumes the first three bytes, if-and-only-if they are 
	// 0xEF, 0xBB, 0xBF (the UTF-8 representation of the BOM)
	
	// Constructor
	UTF8BOMStripperInputStream(InputStream is) throws IOException {
		super(is, 3) ;

		byte[] b = new byte[3] ;

		// Check the first three bytes, to see if they are the UTF-8 BOM,
		// These bytes are EF BB BF

		// First try to read 3 bytes
		int numRead = read(b) ;

		int b0 = (0x000000FF & (int)b[0]) ;
		int b1 = (0x000000FF & (int)b[1]) ;
		int b2 = (0x000000FF & (int)b[2]) ;

		//System.out.println("In UTF8BOMStripper read: " + numRead + " bytes " + b0 + " " + b1 + " " + b2) ;

		if (numRead == 1) {
			unread(b[0]) ;
		} else if (numRead == 2) {
			unread(b[1]) ;
			unread(b[0]) ;
		} else if (numRead == 3) {
			if (b0 != 0xEF || b1 != 0xBB || b2 != 0xBF) {
				// can't be the UTF-8 BOM
				unread(b[2]) ;
				unread(b[1]) ;
				unread(b[0]) ;
			}
			// else it's the UTF-8 BOM, now consumed
		}
		// the other possible return is -1, indicating
		// EOF
	}
}


// Sample usage
/*
import java.io.BufferedReader ;
import java.io.InputStreamReader ; 
import java.io.FileInputStream ; 

public class BOMtest {

	public static void main(String[] args) {
		System.out.println("Hello, world!") ;

		try {
			BufferedReader br = new BufferedReader(
					new InputStreamReader(
						    new UTF8BOMStripperInputStream(new FileInputStream("foo")), 
						    "UTF-8"      )
					                     );
			String line ;
			while ((line = br.readLine()) != null) {
				System.out.println(line) ;
			}
		} catch (Exception e) {
			e.printStackTrace() ;
		}
	}
}
*/

