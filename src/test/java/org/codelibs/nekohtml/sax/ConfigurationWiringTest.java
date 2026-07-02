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
import java.util.ArrayList;
import java.util.List;

import org.codelibs.nekohtml.parsers.DOMParser;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

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

    // =========================================================================
    // Lexical routing across BALANCE_TAGS toggling
    // =========================================================================

    @Test
    public void testLexicalHandlerCommentDeliveredOnceWithDefaultBalanceTags() throws Exception {
        // Given: A parser with balance-tags left at its default (true)
        final HTMLSAXParser parser = new HTMLSAXParser();
        final RecordingLexicalHandler recorder = new RecordingLexicalHandler();
        parser.setProperty("http://xml.org/sax/properties/lexical-handler", recorder);
        parser.setContentHandler(new DefaultHandler());

        // When: Parsing a document-level comment
        parser.parse(new InputSource(new StringReader("<!--c--><html><body>x</body></html>")));

        // Then: The comment is delivered exactly once, with no duplicate routing
        assertEquals(List.of("c"), recorder.comments);
    }

    @Test
    public void testLexicalHandlerCommentDeliveredOnceWithBalanceTagsDisabled() throws Exception {
        // Given: A parser with balance-tags explicitly disabled
        final HTMLSAXParser parser = new HTMLSAXParser();
        parser.setFeature("http://cyberneko.org/html/features/balance-tags", false);
        final RecordingLexicalHandler recorder = new RecordingLexicalHandler();
        parser.setProperty("http://xml.org/sax/properties/lexical-handler", recorder);
        parser.setContentHandler(new DefaultHandler());

        // When: Parsing a document-level comment
        parser.parse(new InputSource(new StringReader("<!--c--><html><body>x</body></html>")));

        // Then: The comment is delivered exactly once (scanner routes directly to the lexical handler)
        assertEquals(List.of("c"), recorder.comments);
    }

    @Test
    public void testLexicalHandlerCommentDeliveredOnceAcrossBalanceTagsToggle() throws Exception {
        // Given: A single parser instance whose balance-tags feature is toggled over its lifetime
        final HTMLSAXParser parser = new HTMLSAXParser();
        final RecordingLexicalHandler recorder = new RecordingLexicalHandler();
        parser.setProperty("http://xml.org/sax/properties/lexical-handler", recorder);
        parser.setContentHandler(new DefaultHandler());

        final String html = "<!--c--><html><body>x</body></html>";

        // When: Disabling balance-tags and parsing
        parser.setFeature("http://cyberneko.org/html/features/balance-tags", false);
        parser.parse(new InputSource(new StringReader(html)));

        // Then: Exactly one comment is recorded (scanner -> lexical handler, balancer not in pipeline)
        assertEquals(List.of("c"), recorder.comments);

        // When: Re-enabling balance-tags after having been false, and parsing again
        recorder.comments.clear();
        parser.setFeature("http://cyberneko.org/html/features/balance-tags", true);
        parser.parse(new InputSource(new StringReader(html)));

        // Then: Exactly one comment is recorded (scanner -> balancer -> lexical handler, no duplicate delivery)
        assertEquals(List.of("c"), recorder.comments);
    }

    /**
     * Recording {@link LexicalHandler} that captures comment text so tests can assert on
     * exactly-once delivery (and detect duplicate delivery caused by routing bugs).
     */
    static class RecordingLexicalHandler implements LexicalHandler {
        final List<String> comments = new ArrayList<>();

        @Override
        public void startDTD(final String name, final String publicId, final String systemId) throws SAXException {
        }

        @Override
        public void endDTD() throws SAXException {
        }

        @Override
        public void startEntity(final String name) throws SAXException {
        }

        @Override
        public void endEntity(final String name) throws SAXException {
        }

        @Override
        public void startCDATA() throws SAXException {
        }

        @Override
        public void endCDATA() throws SAXException {
        }

        @Override
        public void comment(final char[] ch, final int start, final int length) throws SAXException {
            comments.add(new String(ch, start, length));
        }
    }

} // class ConfigurationWiringTest
