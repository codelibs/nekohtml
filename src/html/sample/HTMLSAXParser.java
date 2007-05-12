/* 
 * (C) Copyright 2002, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

package sample;

import org.apache.xerces.parsers.AbstractSAXParser;
import org.cyberneko.html.HTMLConfiguration;

/**
 * This sample shows how to extend a Xerces2 parser class, replacing
 * the default parser configuration with the NekoHTML configuration.
 *
 * @author Andy Clark
 *
 * @version $Id$
 */
public class HTMLSAXParser 
    extends AbstractSAXParser {

    //
    // Constructors
    //

    /** Default constructor. */
    public HTMLSAXParser() {
        super(new HTMLConfiguration());
    } // <init>()

} // class HTMLSAXParser
