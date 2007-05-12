/* 
 * (C) Copyright 2002-2005, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

package org.cyberneko.html.parsers;

import org.apache.xerces.parsers.AbstractSAXParser;
import org.cyberneko.html.HTMLConfiguration;

/**
 * A SAX parser for HTML documents.
 *
 * @author Andy Clark
 *
 * @version $Id: SAXParser.java,v 1.4 2005/02/14 03:56:54 andyc Exp $
 */
public class SAXParser
    extends AbstractSAXParser {

    //
    // Constructors
    //

    /** Default constructor. */
    public SAXParser() {
        super(new HTMLConfiguration());
    } // <init>()

} // class SAXParser
