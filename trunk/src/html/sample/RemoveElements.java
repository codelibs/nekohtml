/* 
 * (C) Copyright 2002-2004, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

package sample;

import org.cyberneko.html.HTMLConfiguration;
import org.cyberneko.html.filters.ElementRemover;

import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;

/**
 * This is a sample that illustrates how to use the 
 * <code>ElementRemover</code> filter.
 *
 * @author Andy Clark
 *
 * @version $Id: RemoveElements.java,v 1.3 2004/02/19 20:00:17 andyc Exp $
 */
public class RemoveElements {

    //
    // MAIN
    //

    /** Main. */
    public static void main(String[] argv) throws Exception {

        // create element remover filter
        ElementRemover remover = new ElementRemover();

        // set which elements to accept
        remover.acceptElement("b", null);
        remover.acceptElement("i", null);
        remover.acceptElement("u", null);
        remover.acceptElement("a", new String[] { "href" });

        // completely remove script elements
        remover.removeElement("script");

        // create writer filter
        org.cyberneko.html.filters.Writer writer =
            new org.cyberneko.html.filters.Writer();

        // setup filter chain
        XMLDocumentFilter[] filters = {
            remover,
            writer,
        };

        // create HTML parser
        XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        // parse documents
        for (int i = 0; i < argv.length; i++) {
            String systemId = argv[i];
            XMLInputSource source = new XMLInputSource(null, systemId, null);
            parser.parse(source);
        }

    } // main(String[])

} // class RemoveElements