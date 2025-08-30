/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.nekohtml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;

import org.apache.html.dom.HTMLDocumentImpl;
import org.codelibs.nekohtml.parsers.DOMFragmentParser;
import org.junit.jupiter.api.Test;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.html.HTMLDocument;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

/**
 * Unit tests for {@link DOMFragmentParser}.
 * @author Marc Guillemot
 *
 */
public class DOMFragmentParserTest {
    /**
     * See <a href="https://sourceforge.net/p/nekohtml/bugs/154/">Bug 154</a>.
     */
    @Test
    public void testAttrEndingWithCRAtEndOfStream() throws Exception {
        doTest("<a href=\"\r", "<A href=\"&#xa;\"/>");
    }

    /**
     * See <a href="http://sourceforge.net/support/tracker.php?aid=2828553">Bug 2828553</a>.
     */
    @Test
    public void testInvalidProcessingInstruction() throws Exception {
        doTest("<html><?9 ?></html>", "<HTML/>");
    }

    /**
     * See <a href="http://sourceforge.net/support/tracker.php?aid=2828534">Bug 2828534</a>.
     */
    @Test
    public void testInvalidAttributeName() throws Exception {
        doTest("<html 9='id'></html>", "<HTML/>");
    }

    private void doTest(final String html, final String expected) throws Exception {
        DOMFragmentParser parser = new DOMFragmentParser();
        HTMLDocument document = new HTMLDocumentImpl();

        DocumentFragment fragment = document.createDocumentFragment();
        InputSource source = new InputSource(new StringReader(html));
        parser.parse(source, fragment);
        //        final OutputFormat of = new OutputFormat();
        //        of.setOmitXMLDeclaration(true);
        //        XMLSerializer s = new XMLSerializer(of);
        //        StringWriter sw = new StringWriter();
        //        s.setOutputCharStream(sw);
        //        s.serialize(fragment);
        DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();

        DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");

        LSSerializer writer = impl.createLSSerializer();
        String str = writer.writeToString(fragment);

        final String xmlDecl = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>" + System.getProperty("line.separator");
        assertEquals(xmlDecl + expected, str);
    }

    public static void print(Node node, String indent) {
        System.out.println(indent + node.getClass().getName());
        Node child = node.getFirstChild();
        while (child != null) {
            print(child, indent + " ");
            child = child.getNextSibling();
        }
    }

    /**
     * HTMLTagBalancer field fSeenBodyElementEnd was not correctly reset as of 1.19.17  
     * @throws Exception
     */
    @Test
    public void testInstanceReuse() throws Exception {
        final String s = "<html><body><frame><frameset></frameset></html>";

        final DOMFragmentParser parser = new DOMFragmentParser();
        final HTMLDocument document = new HTMLDocumentImpl();

        final DocumentFragment fragment1 = document.createDocumentFragment();
        parser.parse(new InputSource(new StringReader(s)), fragment1);

        final DocumentFragment fragment2 = document.createDocumentFragment();
        parser.parse(new InputSource(new StringReader(s)), fragment2);

        final DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
        final DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");

        final LSSerializer writer = impl.createLSSerializer();
        final String str1 = writer.writeToString(fragment1);
        final String str2 = writer.writeToString(fragment2);
        assertEquals(str1, str2);
    }
}
