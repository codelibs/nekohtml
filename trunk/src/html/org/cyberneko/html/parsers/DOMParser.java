/* 
 * (C) Copyright 2002-2003, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

package org.cyberneko.html.parsers;

import org.cyberneko.html.HTMLConfiguration;

/**
 * A DOM parser for HTML documents.
 *
 * @author Andy Clark
 *
 * @version $Id$
 */
public class DOMParser
    /***/
    extends org.apache.xerces.parsers.DOMParser {
    /***
    // NOTE: It would be better to extend from AbstractDOMParser but
    //       most users will find it easier if the API is just like the
    //       Xerces DOM parser. By extending directly from DOMParser,
    //       users can register SAX error handlers, entity resolvers,
    //       and the like. -Ac
    extends org.apache.xerces.parsers.AbstractDOMParser {
    /***/

    //
    // Constructors
    //

    /** Default constructor. */
    public DOMParser() {
        super(new HTMLConfiguration());
        /*** extending DOMParser ***/
        try {
            setProperty("http://apache.org/xml/properties/dom/document-class-name",
                                       "org.apache.html.dom.HTMLDocumentImpl");
        }
        catch (org.xml.sax.SAXNotRecognizedException e) {
            throw new RuntimeException("http://apache.org/xml/properties/dom/document-class-name property not recognized");
        }
        catch (org.xml.sax.SAXNotSupportedException e) {
            throw new RuntimeException("http://apache.org/xml/properties/dom/document-class-name property not supported");
        }
        /*** extending AbstractDOMParser ***
        fConfiguration.setProperty("http://apache.org/xml/properties/dom/document-class-name",
                                   "org.apache.html.dom.HTMLDocumentImpl");
        /***/
    } // <init>()

} // class DOMParser
