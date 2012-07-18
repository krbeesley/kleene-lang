
//	SplashWindow.java
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

// splash window that appears when Kleene is first launched

import java.awt.* ;
import java.awt.event.* ;
import java.net.URL ;
import javax.swing.* ;

class SplashWindow extends JWindow {

    // Constructor
    public SplashWindow (String title, String imageFilename, JFrame ownerFrame, int waitTime) {
		super (ownerFrame) ;  // the "owner" Frame, presumably the main JFrame
	         	             // of the application
		JLabel l = null ;
		if (imageFilename.equals("")) {
	    	l = new JLabel(title) ;
		} else {
	    	String pathname = imageFilename ;
	    	// this should work when run as an application
	    	ImageIcon icon = new ImageIcon(pathname) ;

	    	// but when packaged as a JAR, need the following
	    	if (icon == null || icon.getImageLoadStatus() == java.awt.MediaTracker.ERRORED) {
				// try to load it from the JAR file
				URL url = getClass().getResource(pathname) ;
				if (url != null) {
		    		icon = new ImageIcon(url) ;
		    		if (icon == null || icon.getImageLoadStatus() == java.awt.MediaTracker.ERRORED) {
						System.err.println("ERROR: Couldn't load \"" + url + "\"") ;
		    		}
				} else {
		    		System.err.println("ERROR: Couldn't load \"" + pathname + "\"") ;
				}
	    	}
	    	l = new JLabel(title, icon, JLabel.CENTER) ;

			// give the text a little border
			l.setBorder(BorderFactory.createEmptyBorder(20, 5, 5, 5)) ; 

	    	// set position of text relative to the graphic
	    	l.setVerticalTextPosition(JLabel.BOTTOM) ;
	    	l.setHorizontalTextPosition(JLabel.CENTER) ;
		}
		// add the JLabel to the contentPane of the Splash Window
		this.getContentPane().add(l, BorderLayout.CENTER) ;
		this.pack () ;  // adjust size of SplashWindow to the JLabel size

		// now set the location of the SplashWindow in middle of the
		//   physical screen
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
		Dimension labelSize = l.getPreferredSize() ;
		this.setLocation(screenSize.width/2 - (labelSize.width/2), 
		    screenSize.height/2 - (labelSize.height/2)) ;

		// allow a mouse click to dispose of the SplashWindow at any time
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
		    	setVisible(false) ;
		    	dispose() ;
			}
	    }) ;
	
		final int pause = waitTime ;  // waitTime arg to the Constructor

		// create a Runnable, something that can be run in a separate
		// thread (so that the application itself and the mouse-click
		// closing are always active)
		final Runnable closerRunner = new Runnable() {
			public void run() {
		    	setVisible(false) ;
		    	dispose() ;
			}
	    } ;
	
		// create another Runnable, to be launched in its own threat
		//   to sleep the prescibed period and then invoke the closerRunnable
		//   These threads ensure that the app and the click-closing always
		//   remain responsive.

		Runnable waitRunner = new Runnable() {
			public void run() {
		    	try {
					Thread.sleep(pause) ;
					// invokeAndWait() first executes any pending UI
					//  activities, then invokes closerRunner in
					//  another thread, so app and click-closing
					//  always remain responsive
					SwingUtilities.invokeAndWait(closerRunner) ;
		    	} catch (Exception e) {
					e.printStackTrace() ;
		    	}
			}
	    } ;

		// normal execution of the Constructor here

		setVisible(true) ;
		Thread splashThread = new Thread(waitRunner, "SplashThread") ;

		splashThread.start() ;
    }
}
