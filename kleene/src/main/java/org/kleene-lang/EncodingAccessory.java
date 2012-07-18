
//	EncodingAccessory.java	
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

//	"Accessory" used in EncodingFileChooser.java to allow the user
//	to select the file encoding.

import java.awt.event.ActionEvent ;
import java.awt.BorderLayout ; 
import javax.swing.BorderFactory ;  
import javax.swing.JComboBox ;
import javax.swing.JPanel ;
import javax.swing.JLabel ;
import java.awt.Dimension ; 
import java.util.Vector ;

class EncodingAccessory extends JPanel {

	private JComboBox comboBox ;
	private JLabel northLabel ;
	private JLabel southLabel ;

	public EncodingAccessory(String[] encodings, String initialEncoding, String nLabel, String sLabel) {
		super(new BorderLayout()) ;

		comboBox = new JComboBox(encodings) ;
		//comboBox.setEditable(true) ;
		comboBox.setEditable(false) ;
		comboBox.setSelectedItem(initialEncoding) ;

		northLabel = new JLabel(" " + nLabel) ;
		southLabel = new JLabel(" " + sLabel) ;

		this.add(northLabel, BorderLayout.NORTH) ;
		this.add(comboBox, BorderLayout.CENTER) ;
		this.add(southLabel, BorderLayout.SOUTH) ;

		Dimension dim = comboBox.getPreferredSize() ;
		int northWidth = northLabel.getPreferredSize().width ;
		int southWidth = southLabel.getPreferredSize().width ;
		dim.width = (northWidth >= southWidth) ? northWidth : southWidth ;

		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)) ;
		//setPreferredSize(new Dimension(dim.width + 20, dim.height)) ; 
		setPreferredSize(new Dimension(dim.width + 20, 30)) ; 
	}

	public String getSelectedEncoding() {
		return (String) comboBox.getSelectedItem() ;
	}
}
