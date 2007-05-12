/* 
 * (C) Copyright 2002-2005, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

package org.cyberneko.html.parsers;

import org.cyberneko.html.HTMLConfiguration;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.XNIException;

import org.w3c.dom.DOMException;

/**
 * A DOM parser for HTML documents.
 *
 * @author Andy Clark
 *
 * @version $Id: DOMParser.java,v 1.5 2005/02/14 03:56:54 andyc Exp $
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

    //
    // XMLDocumentHandler methods
    //

    /** Doctype declaration. */
    public void doctypeDecl(String root, String pubid, String sysid,
                            Augmentations augs) throws XNIException {
        
        // NOTE: Xerces HTML DOM implementation (up to and including
        //       2.5.0) throws a heirarchy request error exception 
        //       when a doctype node is appended to the tree. So, 
        //       don't insert this node into the tree for those 
        //       versions... -Ac

        String VERSION = org.apache.xerces.impl.Version.fVersion;
        boolean okay = true;
        if (VERSION.startsWith("Xerces-J 2.")) {
            okay = getParserSubVersion() > 5;
        }
        // REVISIT: As soon as XML4J is updated with the latest code
        //          from Xerces, then this needs to be updated to
        //          check XML4J's version. -Ac
        else if (VERSION.startsWith("XML4J")) {
            okay = false;
        }

        // if okay, insert doctype; otherwise, don't risk it
        if (okay) {
            super.doctypeDecl(root, pubid, sysid, augs);
        }

    } // doctypeDecl(String,String,String,Augmentations)

    //
    // Private static methods
    //

    /** Returns the parser's sub-version number. */
    private static int getParserSubVersion() {
        try {
            String VERSION = org.apache.xerces.impl.Version.fVersion;
            int index1 = VERSION.indexOf('.') + 1;
            int index2 = VERSION.indexOf('.', index1);
            if (index2 == -1) { index2 = VERSION.length(); }
            return Integer.parseInt(VERSION.substring(index1, index2));
        }
        catch (Exception e) {
            return -1;
        }
    } // getParserSubVersion():int

} // class DOMParser
