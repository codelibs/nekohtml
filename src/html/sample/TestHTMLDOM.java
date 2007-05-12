/* 
 * (C) Copyright 2002-2004, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

package sample;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * This program tests the NekoHTML parser's use of the HTML DOM
 * implementation by printing the class names of all the nodes in
 * the parsed document.
 *
 * @author Andy Clark
 *
 * @version $Id: TestHTMLDOM.java,v 1.3 2004/02/19 20:00:17 andyc Exp $
 */
public class TestHTMLDOM {

    //
    // MAIN
    //

    /** Main. */
    public static void main(String[] argv) throws Exception {
        DOMParser parser = new DOMParser();
        for (int i = 0; i < argv.length; i++) {
            parser.parse(argv[i]);
            print(parser.getDocument(), "");
        }
    } // main(String[])

    //
    // Public static methods
    //

    /** Prints a node's class name. */
    public static void print(Node node, String indent) {
        System.out.println(indent+node.getClass().getName());
        Node child = node.getFirstChild();
        while (child != null) {
            print(child, indent+" ");
            child = child.getNextSibling();
        }
    } // print(Node)

} // class TestHTMLDOM
