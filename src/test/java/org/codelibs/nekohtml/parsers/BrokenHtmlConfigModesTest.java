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
package org.codelibs.nekohtml.parsers;

import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.codelibs.nekohtml.sax.HTMLSAXConfiguration;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Category O: config-dependent characterization tests. Covers the {@code nekohtml.dom.strict}
 * system property (unset / "false" / "true"), the {@code balance-tags} feature, and lexical
 * handler wiring on the public {@code DOMParser}/{@code SAXParser} API.
 *
 * <p>
 * {@code nekohtml.dom.strict} is a GLOBAL system property; every test that touches it saves and
 * restores the previous value in a {@code finally} block.
 * </p>
 */
public class BrokenHtmlConfigModesTest {

    private static final String PROPERTY_DOM_STRICT = "nekohtml.dom.strict";

    private static final String LEXICAL_HANDLER_PROPERTY = "http://xml.org/sax/properties/lexical-handler";

    // =========================================================================
    // nekohtml.dom.strict: unset (default) mode
    // =========================================================================

    @Test
    public void defaultModeDoesNotThrowOnUnclosedTags() throws Exception {
        final String prev = System.getProperty(PROPERTY_DOM_STRICT);
        try {
            System.clearProperty(PROPERTY_DOM_STRICT);
            // characterization: default (unset) mode never throws for unclosed tags
            assertDoesNotThrow(() -> parse("<div><p>a<span>b"));
        } finally {
            restoreProperty(prev);
        }
    }

    @Test
    public void defaultModeDoesNotThrowOnMismatchedTags() throws Exception {
        final String prev = System.getProperty(PROPERTY_DOM_STRICT);
        try {
            System.clearProperty(PROPERTY_DOM_STRICT);
            // characterization: default (unset) mode never throws for misnested formatting tags
            assertDoesNotThrow(() -> parse("<b><i>x</b></i>"));
        } finally {
            restoreProperty(prev);
        }
    }

    // =========================================================================
    // nekohtml.dom.strict: explicit "false" (warning-level) mode
    // =========================================================================

    @Test
    public void explicitFalseModeDoesNotThrowOnStrayEndTag() throws Exception {
        final String prev = System.getProperty(PROPERTY_DOM_STRICT);
        try {
            System.setProperty(PROPERTY_DOM_STRICT, "false");
            // characterization: explicit "false" mode logs at WARNING level but never throws
            assertDoesNotThrow(() -> parse("<div>a</span>b</div>"));
        } finally {
            restoreProperty(prev);
        }
    }

    @Test
    public void explicitFalseModeDoesNotThrowOnMismatchedTags() throws Exception {
        final String prev = System.getProperty(PROPERTY_DOM_STRICT);
        try {
            System.setProperty(PROPERTY_DOM_STRICT, "false");
            assertDoesNotThrow(() -> parse("<div><span></div></span>"));
        } finally {
            restoreProperty(prev);
        }
    }

    // =========================================================================
    // nekohtml.dom.strict: "true" (strict) mode
    // =========================================================================

    @Test
    public void strictModeDoesNotThrowOnWellFormedHtml() throws Exception {
        final String prev = System.getProperty(PROPERTY_DOM_STRICT);
        try {
            System.setProperty(PROPERTY_DOM_STRICT, "true");
            assertDoesNotThrow(() -> assertNotNull(parse("<html><body><p>ok</p></body></html>")));
        } finally {
            restoreProperty(prev);
        }
    }

    @Test
    public void strictModeDoesNotThrowOnUnclosedTagsHandledByBalancer() throws Exception {
        // characterization: the tag balancer normalizes unclosed tags into well-formed SAX events
        // before they ever reach SAXToDOMHandler, so strict mode does not throw here either
        final String prev = System.getProperty(PROPERTY_DOM_STRICT);
        try {
            System.setProperty(PROPERTY_DOM_STRICT, "true");
            assertDoesNotThrow(() -> parse("<div><p>a<span>b"));
        } finally {
            restoreProperty(prev);
        }
    }

    @Test
    public void strictModeDoesNotThrowOnMismatchedFormattingTagsHandledByBalancer() throws Exception {
        // characterization: the Adoption Agency Algorithm resolves misnested formatting tags before
        // the DOM builder sees them, so strict mode does not throw
        final String prev = System.getProperty(PROPERTY_DOM_STRICT);
        try {
            System.setProperty(PROPERTY_DOM_STRICT, "true");
            assertDoesNotThrow(() -> parse("<b><i>x</b></i>"));
        } finally {
            restoreProperty(prev);
        }
    }

    @Test
    public void strictModeDoesNotThrowOnStrayEndTagHandledByBalancer() throws Exception {
        final String prev = System.getProperty(PROPERTY_DOM_STRICT);
        try {
            System.setProperty(PROPERTY_DOM_STRICT, "true");
            assertDoesNotThrow(() -> parse("</b>"));
        } finally {
            restoreProperty(prev);
        }
    }

    @Test
    public void strictModeThrowsOnSecondTopLevelHtmlRoot() throws Exception {
        // characterization: once the balancer's implicit-HTML tracking flag has already fired and
        // the DOM element stack has unwound back to the Document node, a second top-level <html>
        // tries to append a second document-element child and Document rejects it. In strict mode
        // that DOM hierarchy violation is propagated as a SAXException.
        final String prev = System.getProperty(PROPERTY_DOM_STRICT);
        try {
            System.setProperty(PROPERTY_DOM_STRICT, "true");
            assertThrows(SAXException.class, () -> parse("<html>a</html><html>b</html>"));
        } finally {
            restoreProperty(prev);
        }
    }

    @Test
    public void strictModeThrowsOnElementAfterHtmlRootFullyClosed() throws Exception {
        // characterization: same DOM hierarchy violation as above, triggered by any element (not
        // just another <html>) arriving after the root has already fully closed
        final String prev = System.getProperty(PROPERTY_DOM_STRICT);
        try {
            System.setProperty(PROPERTY_DOM_STRICT, "true");
            assertThrows(SAXException.class, () -> parse("<html></html><p>x</p>"));
        } finally {
            restoreProperty(prev);
        }
    }

    // =========================================================================
    // balance-tags feature: settable on the public DOMParser/SAXParser
    // =========================================================================

    @Test
    public void balanceTagsFeatureSettableOnPublicSaxParser() throws Exception {
        final SAXParser parser = new SAXParser();
        // characterization: the public SAXParser exposes setFeature/getFeature for balance-tags
        assertTrue(parser.getFeature(HTMLSAXConfiguration.BALANCE_TAGS));
        parser.setFeature(HTMLSAXConfiguration.BALANCE_TAGS, false);
        assertFalse(parser.getFeature(HTMLSAXConfiguration.BALANCE_TAGS));
    }

    @Test
    public void balanceTagsFeatureSettableOnPublicDomParser() throws Exception {
        final DOMParser parser = new DOMParser();
        assertTrue(parser.getFeature(HTMLSAXConfiguration.BALANCE_TAGS));
        parser.setFeature(HTMLSAXConfiguration.BALANCE_TAGS, false);
        assertFalse(parser.getFeature(HTMLSAXConfiguration.BALANCE_TAGS));
    }

    @Test
    public void balanceTagsOffChangesConsecutiveParagraphEventStream() throws Exception {
        // With balancing ON (default helper), consecutive <p> nest: start P, start P, end P, end P
        final List<String> balanced = saxEvents("<p>a<p>b");

        // With balancing OFF, the tag balancer filter is removed from the pipeline entirely, so no
        // auto-closing/auto-nesting happens; raw scanner events pass straight through.
        final SAXParser parser = new SAXParser();
        parser.setFeature(HTMLSAXConfiguration.BALANCE_TAGS, false);
        final List<String> unbalanced = collectEvents(parser, "<p>a<p>b");

        // characterization: turning balance-tags off produces an observably different event stream
        assertNotEquals(balanced, unbalanced);
    }

    @Test
    public void balanceTagsOffSkipsImplicitHtmlRootInsertion() throws Exception {
        // characterization: ensureDocumentInitialized() lives inside HTMLTagBalancerFilter, so with
        // balance-tags off (filter removed from pipeline) no implicit HTML root is auto-inserted
        final SAXParser parser = new SAXParser();
        parser.setFeature(HTMLSAXConfiguration.BALANCE_TAGS, false);
        final List<String> events = collectEvents(parser, "Hello<b>x</b>");
        assertFalse(events.contains("start:HTML"));
        assertTrue(events.contains("start:B"));
    }

    // =========================================================================
    // Lexical handler wiring
    // =========================================================================

    @Test
    public void defaultSaxParserHasNoLexicalHandlerSoCommentsAreSilentlyConsumed() throws Exception {
        // characterization: a plain SAXParser with no registered lexical handler silently drops
        // comment content; it never leaks into character events
        final List<String> events = saxEvents("<p>a</p><!--secret--><p>b</p>");
        assertFalse(events.stream().anyMatch(e -> e.contains("secret")));
    }

    @Test
    public void customLexicalHandlerReceivesCommentEvents() throws Exception {
        final SAXParser parser = new SAXParser();
        final StringBuilder commentText = new StringBuilder();
        parser.setProperty(LEXICAL_HANDLER_PROPERTY, new LexicalHandler() {
            @Override
            public void startDTD(final String name, final String publicId, final String systemId) {
            }

            @Override
            public void endDTD() {
            }

            @Override
            public void startEntity(final String name) {
            }

            @Override
            public void endEntity(final String name) {
            }

            @Override
            public void startCDATA() {
            }

            @Override
            public void endCDATA() {
            }

            @Override
            public void comment(final char[] ch, final int start, final int length) {
                commentText.append(ch, start, length);
            }
        });
        parser.setContentHandler(new DefaultHandler());
        parser.parse(new InputSource(new StringReader("<!--hello-->")));

        // characterization: registering a lexical handler via setProperty makes comment content observable
        assertEquals("hello", commentText.toString());
    }

    @Test
    public void domParserDefaultWiresLexicalHandlerProducingCommentNodes() throws Exception {
        // characterization: DOMParser always wires its SAXToDOMHandler as the lexical handler, so
        // comments become actual DOM Comment nodes (unlike a plain SAXParser)
        final Document doc = parse("<p>a</p><!--hello--><p>b</p>");
        assertEquals(1, count(doc, "//comment()"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static void restoreProperty(final String prev) {
        if (prev == null) {
            System.clearProperty(PROPERTY_DOM_STRICT);
        } else {
            System.setProperty(PROPERTY_DOM_STRICT, prev);
        }
    }

    /** Collects SAX startElement/endElement events (as "start:X"/"end:X") using a caller-supplied parser instance. */
    private static List<String> collectEvents(final SAXParser parser, final String html) throws Exception {
        final List<String> events = new ArrayList<>();
        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
                events.add("start:" + qName);
            }

            @Override
            public void endElement(final String uri, final String localName, final String qName) {
                events.add("end:" + qName);
            }
        });
        parser.parse(new InputSource(new StringReader(html)));
        return events;
    }
}
