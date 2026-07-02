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
package org.codelibs.nekohtml.sax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;

import org.codelibs.nekohtml.parsers.DOMParser;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * End-to-end tests verifying that the cyberneko {@code names/elems} and
 * {@code names/attrs} properties are honored through the full
 * {@link DOMParser} pipeline, and that lexical events (comments) at the
 * document level are preserved once they are routed through the tag
 * balancer.
 */
public class ConfigurationWiringTest {

    @Test
    public void testNamesElemsLowerHonored() throws Exception {
        final DOMParser p = new DOMParser();
        p.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
        p.setProperty("http://cyberneko.org/html/properties/names/attrs", "upper");
        p.parse(new InputSource(new StringReader("<HTML><BODY><DIV Class=\"x\">t</DIV></BODY></HTML>")));
        assertEquals("html", p.getDocument().getDocumentElement().getNodeName());
        final Element div = (Element) p.getDocument().getElementsByTagName("div").item(0);
        assertEquals("x", div.getAttribute("CLASS"));
    }

    @Test
    public void testNamesElemsMatchKeepsCase() throws Exception {
        final DOMParser p = new DOMParser();
        p.setProperty("http://cyberneko.org/html/properties/names/elems", "match");
        p.parse(new InputSource(new StringReader("<MyTag>x</MyTag>"))); // synthesized root is HTML regardless
        assertEquals(1, p.getDocument().getElementsByTagName("MyTag").getLength());
    }

    @Test
    public void testCommentBeforeHtmlAtDocumentLevel() throws Exception {
        final DOMParser p = new DOMParser();
        p.parse(new InputSource(new StringReader("<!--c--><html><body>x</body></html>")));
        assertEquals(Node.COMMENT_NODE, p.getDocument().getFirstChild().getNodeType());
    }

    @Test
    public void testInvalidNamesValueRejected() throws Exception {
        final DOMParser p = new DOMParser();
        assertThrows(SAXException.class, () -> p.setProperty("http://cyberneko.org/html/properties/names/elems", "sideways"));
    }

} // class ConfigurationWiringTest
