
//	Utils.java
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

import java.io.File ;
import java.io.FileInputStream ;
import java.io.IOException ;

public class Utils {

    public final static String jpeg = "jpeg";
    public final static String jpg = "jpg";
    public final static String gif = "gif";
    public final static String tiff = "tiff";
    public final static String tif = "tif";
    public final static String png = "png";
	public final static String xml = "xml" ;
	public final static String kl = "kl" ;  // Kleene .kl scripts

    /**
     * Return the extension of a file.
     */  
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }

	/**
	 * Return the Unicode encoding of a File that has a BOM
	 * else return the system default encoding
	 * @param File
	 */
	public String getEncoding(File file) {
		return getEncoding(file.getPath()) ;
	}

	/**
	 * Return the Unicode encoding of a file that has a BOM
	 * else return the system default encoding
	 * @param String, representing a file path
	 */
	public String getEncoding(String filepath) {
		// by default, return the system file.encoding
		String encoding = System.getProperty("file.encoding") ;
		FileInputStream fileInputStream = null ;

		try {
			fileInputStream = new FileInputStream(filepath) ;
			// FileInputStream provides a stream of raw bytes
			byte[] buffer = new byte[4] ;
			// will try to read 4 bytes, filling the buffer;
			// will return the number really read,
			// which can be less than 4
			int length = fileInputStream.read(buffer) ;

			if (length == 4) {
				// 4 bytes successfully read, could be UTF-32, UTF-16 or UTF-8
				// or no BOM at all
				if (buffer[0] == 0x00 && 
					buffer[1] == 0x00 && 
					buffer[2] == 0xFE && 
					buffer[3] == 0xFF) {
					// 	UTF-32BE 
					// 	The BE is used only when there is no BOM.
					encoding = "UTF-32" ;
				} else if (buffer[0] == 0xFF &&
						   buffer[1] == 0xFE &&
						   buffer[2] == 0x00 &&
						   buffer[3] == 0x00) {
					// UTF-32LE
					// The LE is used only when there is no BOM.
					encoding = "UTF-32" ;
				} else if (buffer[0] == 0xEF &&
						   buffer[1] == 0xBB &&
						   buffer[2] == 0xBF) {
					encoding = "UTF-8" ;
				} else if (buffer[0] == 0xFE && buffer[1] == 0xFF) {
					// UTF-16BE
					encoding = "UTF-16" ;
				} else if (buffer[0] == 0xFF && buffer[1] == 0xFE) {
					// UTF-16LE
					encoding = "UTF-16" ;
				}
			} else if (length == 3) {
				// for completeness
				// the file has only three bytes, could be UTF-16 or UTF-8
				if (buffer[0] == 0xEF &&
					buffer[1] == 0xBB &&
					buffer[2] == 0xBF) {
					encoding = "UTF-8" ;
				} else if (buffer[0] == 0xFE && buffer[1] == 0xFF) {
					// UTF-16BE
					encoding = "UTF-16" ;
				} else if (buffer[0] == 0xFF && buffer[1] == 0xFE) {
					// UTF-16LE
					encoding = "UTF-16" ;
				}
			} else if (length == 2) {
				// for completeness
				// the file has only two bytes, could be UTF-16
				if (buffer[0] == 0xFE && buffer[1] == 0xFF) {
					// UTF-16BE
					encoding = "UTF-16" ;
				} else if (buffer[0] == 0xFF && buffer[1] == 0xFE) {
					// UTF-16LE
					encoding = "UTF-16" ;
				}
			}
		} catch (IOException ex) {
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close() ;
				} catch (IOException ex) {
				}
			}
		}

		return encoding ;
	}
}

