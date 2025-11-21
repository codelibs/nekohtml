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

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Tests for HTMLTagBalancerFilter enhancements including null/empty qName validation
 * and performance improvements.
 */
public class HTMLTagBalancerFilterEnhancementsTest {

    private HTMLTagBalancerFilter filter;
    private List<String> startElements;
    private List<String> endElements;

    @BeforeEach
    public void setUp() {
        filter = new HTMLTagBalancerFilter();
        startElements = new ArrayList<>();
        endElements = new ArrayList<>();

        filter.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes atts)
                    throws SAXException {
                startElements.add(qName);
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                endElements.add(qName);
            }
        });
    }

    /**
     * Test that null qName is handled gracefully in startElement
     */
    @Test
    public void testStartElementWithNullQName() throws Exception {
        filter.startDocument();

        // Should not throw exception, just log warning and ignore
        assertDoesNotThrow(() -> filter.startElement("", "test", null, new AttributesImpl()));

        assertTrue(startElements.isEmpty(), "No element should be added for null qName");
    }

    /**
     * Test that empty qName is handled gracefully in startElement
     */
    @Test
    public void testStartElementWithEmptyQName() throws Exception {
        filter.startDocument();

        // Should not throw exception, just log warning and ignore
        assertDoesNotThrow(() -> filter.startElement("", "test", "", new AttributesImpl()));

        assertTrue(startElements.isEmpty(), "No element should be added for empty qName");
    }

    /**
     * Test that null qName is handled gracefully in endElement
     */
    @Test
    public void testEndElementWithNullQName() throws Exception {
        filter.startDocument();

        // Should not throw exception, just log warning and ignore
        assertDoesNotThrow(() -> filter.endElement("", "test", null));

        assertTrue(endElements.isEmpty(), "No element should be ended for null qName");
    }

    /**
     * Test that empty qName is handled gracefully in endElement
     */
    @Test
    public void testEndElementWithEmptyQName() throws Exception {
        filter.startDocument();

        // Should not throw exception, just log warning and ignore
        assertDoesNotThrow(() -> filter.endElement("", "test", ""));

        assertTrue(endElements.isEmpty(), "No element should be ended for empty qName");
    }

    /**
     * Test normal operation still works after null qName handling
     */
    @Test
    public void testNormalOperationAfterNullQName() throws Exception {
        filter.startDocument();

        // Start with HTML to avoid auto-initialization
        filter.startElement("", "html", "HTML", new AttributesImpl());

        // Try null qName
        filter.startElement("", "test", null, new AttributesImpl());

        // Then normal element
        filter.startElement("", "div", "DIV", new AttributesImpl());

        assertEquals(2, startElements.size(), "HTML and DIV should be added");
        assertEquals("HTML", startElements.get(0), "First element should be HTML");
        assertEquals("DIV", startElements.get(1), "Second element should be DIV");
    }

    /**
     * Test complex HTML with mixed null and valid qNames
     */
    @Test
    public void testMixedNullAndValidQNames() throws Exception {
        filter.startDocument();

        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "body", null, new AttributesImpl()); // null qName
        filter.startElement("", "div", "DIV", new AttributesImpl());
        filter.startElement("", "p", "", new AttributesImpl()); // empty qName
        filter.endElement("", "p", "");
        filter.endElement("", "div", "DIV");
        filter.endElement("", "body", null);
        filter.endElement("", "html", "HTML");

        filter.endDocument();

        // Only valid elements should be tracked
        assertTrue(startElements.contains("HTML"), "HTML should be started");
        assertTrue(startElements.contains("DIV"), "DIV should be started");
        assertFalse(startElements.contains(null), "null should not be in start elements");
        assertFalse(startElements.contains(""), "empty string should not be in start elements");
    }

    /**
     * Test performance improvement with deeply nested formatting elements
     * This tests that the optimized pop/push operations work correctly
     */
    @Test
    public void testDeeplyNestedFormattingElements() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();
        final List<String> elements = new ArrayList<>();

        config.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes atts) {
                elements.add("START:" + qName);
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                elements.add("END:" + qName);
            }
        });

        // Create deeply nested formatting elements
        final String html = "<div><b><i><strong><em>Text</em></strong></i></b></div>";
        config.parse(new InputSource(new StringReader(html)));

        // Verify all elements are properly balanced
        assertTrue(elements.contains("START:DIV"), "DIV should start");
        assertTrue(elements.contains("START:B"), "B should start");
        assertTrue(elements.contains("START:I"), "I should start");
        assertTrue(elements.contains("START:STRONG"), "STRONG should start");
        assertTrue(elements.contains("START:EM"), "EM should start");

        assertTrue(elements.contains("END:EM"), "EM should end");
        assertTrue(elements.contains("END:STRONG"), "STRONG should end");
        assertTrue(elements.contains("END:I"), "I should end");
        assertTrue(elements.contains("END:B"), "B should end");
        assertTrue(elements.contains("END:DIV"), "DIV should end");
    }

    /**
     * Test performance with many sibling elements
     */
    @Test
    public void testManySiblingElements() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();
        final List<String> elements = new ArrayList<>();

        config.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes atts) {
                elements.add(qName);
            }
        });

        // Create many sibling div elements
        final StringBuilder html = new StringBuilder("<body>");
        for (int i = 0; i < 100; i++) {
            html.append("<div>Content ").append(i).append("</div>");
        }
        html.append("</body>");

        config.parse(new InputSource(new StringReader(html.toString())));

        // Verify all divs were parsed
        long divCount = elements.stream().filter(e -> "DIV".equals(e)).count();
        assertEquals(100, divCount, "Should parse all 100 div elements");
    }

    /**
     * Test that null ContentHandler doesn't cause errors
     */
    @Test
    public void testNullContentHandler() throws Exception {
        final HTMLTagBalancerFilter filterWithNullHandler = new HTMLTagBalancerFilter();
        filterWithNullHandler.setContentHandler(null);

        filterWithNullHandler.startDocument();
        assertDoesNotThrow(() -> filterWithNullHandler.startElement("", "div", "DIV", new AttributesImpl()));
        assertDoesNotThrow(() -> filterWithNullHandler.endElement("", "div", "DIV"));
        filterWithNullHandler.endDocument();
    }

    /**
     * Test balancing with null qName in the middle of valid structure
     */
    @Test
    public void testBalancingWithNullQNameInMiddle() throws Exception {
        filter.startDocument();

        // Start with HTML to prevent auto-initialization
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "div", "DIV", new AttributesImpl());
        filter.startElement("", "p", "P", new AttributesImpl());
        filter.endElement("", "invalid", null); // null qName - should be ignored
        filter.endElement("", "p", "P");
        filter.endElement("", "div", "DIV");
        filter.endElement("", "html", "HTML");

        filter.endDocument();

        // Verify proper balancing despite null qName
        assertEquals(3, startElements.size(), "Should have HTML, DIV, P");
        assertEquals(3, endElements.size(), "Should have P, DIV, HTML");
        assertEquals("P", endElements.get(0), "First end should be P");
        assertEquals("DIV", endElements.get(1), "Second end should be DIV");
        assertEquals("HTML", endElements.get(2), "Third end should be HTML");
    }

    /**
     * Test that empty qName doesn't break element stack
     */
    @Test
    public void testEmptyQNameDoesNotBreakStack() throws Exception {
        filter.startDocument();

        // Start with HTML to prevent auto-initialization
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "div", "DIV", new AttributesImpl());
        filter.startElement("", "", "", new AttributesImpl()); // empty qName - should be ignored
        filter.startElement("", "span", "SPAN", new AttributesImpl());
        filter.endElement("", "span", "SPAN");
        filter.endElement("", "div", "DIV");
        filter.endElement("", "html", "HTML");

        filter.endDocument();

        // Stack should be properly maintained
        assertTrue(startElements.contains("HTML"), "HTML should be in start elements");
        assertTrue(startElements.contains("DIV"), "DIV should be in start elements");
        assertTrue(startElements.contains("SPAN"), "SPAN should be in start elements");
        assertEquals(3, startElements.size(), "Should have exactly 3 start elements");
    }

    /**
     * Test multiple consecutive null qNames
     */
    @Test
    public void testMultipleConsecutiveNullQNames() throws Exception {
        filter.startDocument();

        // Start with HTML to prevent auto-initialization
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "div", "DIV", new AttributesImpl());
        filter.startElement("", null, null, new AttributesImpl()); // ignored
        filter.startElement("", null, null, new AttributesImpl()); // ignored
        filter.startElement("", "", "", new AttributesImpl()); // ignored
        filter.startElement("", "span", "SPAN", new AttributesImpl());

        assertEquals(3, startElements.size(), "Should only have valid elements");
        assertEquals("HTML", startElements.get(0));
        assertEquals("DIV", startElements.get(1));
        assertEquals("SPAN", startElements.get(2));
    }
}
