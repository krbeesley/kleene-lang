
//	ImageJDesktopPane.java
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

// From an example posted by Nicolas Wright 21 Oct 1999

import java.awt.* ;
import javax.swing.* ;

public class ImageJDesktopPane extends JDesktopPane {
    ImageIcon image = null;
    boolean fillEntireArea = false;
    boolean tileImage = false;

    public ImageJDesktopPane() {
        super();
    }

    public void setFillEntireArea( boolean b ) {
        fillEntireArea = b;
        repaint();
    }

    public boolean getFillEntireArea() {
        return fillEntireArea;
    }

    public void setTileImage( boolean b ) {
        tileImage = b;
        repaint();
    }

    public boolean getTileImage() {
        return tileImage;
    }

    public void setImage( ImageIcon i ) {
        image = i;
        repaint();
    }

    public ImageIcon getImage() {
        return image;
    }

    public void paintComponent( Graphics g ) {
        if (isOpaque()) {
            super.paintComponent( g );
            if ( image != null ) {
                int width = getWidth() ;
		int height = getHeight();

                g.setColor( getBackground() );
                g.fillRect( 0, 0, width, height );
                if ( fillEntireArea )
                    g.drawImage( image.getImage(), 0, 0, width, height, this );
                else {
                    if ( !tileImage ) {
                        g.drawImage( image.getImage(),
                                     ( width-image.getIconWidth() )/2,
                                     ( height-image.getIconHeight() )/2,
                                     this );
                    } else {
                        int tileW = image.getIconWidth();
                        int tileH = image.getIconHeight();
                        for ( int ypos = 0; height - ypos > 0; ypos += tileH ) {
                            for ( int xpos = 0; width - xpos > 0; xpos += tileW ) {
                                image.paintIcon( null, g, xpos, ypos );
                            }
                        }
                    }
                }
            }
        }   
    }
}
