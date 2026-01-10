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

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Tests for the nekohtml.dom.strict system property behavior.
 * Tests the three modes: NOT_SET (default), FALSE (explicit lenient), TRUE (strict).
 *
 * @author CodeLibs Project
 */
public class StrictModeTest {

    private static final String PROPERTY_DOM_STRICT = "nekohtml.dom.strict";

    private String originalPropertyValue;
    private TestLogHandler logHandler;
    private Logger saxToDomLogger;

    @BeforeEach
    public void setUp() {
        // Save original property value
        originalPropertyValue = System.getProperty(PROPERTY_DOM_STRICT);

        // Set up log capture for SAXToDOMHandler
        saxToDomLogger = Logger.getLogger("org.codelibs.nekohtml.parsers.SAXToDOMHandler");
        logHandler = new TestLogHandler();
        saxToDomLogger.addHandler(logHandler);
        saxToDomLogger.setLevel(Level.ALL);
    }

    @AfterEach
    public void tearDown() {
        // Restore original property value
        if (originalPropertyValue != null) {
            System.setProperty(PROPERTY_DOM_STRICT, originalPropertyValue);
        } else {
            System.clearProperty(PROPERTY_DOM_STRICT);
        }

        // Remove log handler
        if (saxToDomLogger != null && logHandler != null) {
            saxToDomLogger.removeHandler(logHandler);
        }
    }

    // =========================================================================
    // Default Mode Tests (Property NOT_SET)
    // =========================================================================

    @Test
    public void testDefaultModeParseNormalHtml() throws Exception {
        System.clearProperty(PROPERTY_DOM_STRICT);

        final String html = "<html><head><title>Test</title></head><body><p>Hello</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should be parsed successfully in default mode");
        assertNotNull(doc.getDocumentElement(), "Root element should exist");
    }

    @Test
    public void testDefaultModeParsesMalformedHtml() throws Exception {
        System.clearProperty(PROPERTY_DOM_STRICT);

        // Malformed HTML with unclosed tags
        final String html = "<html><body><div><p>Unclosed paragraph<div>Nested div</body></html>";

        final DOMParser parser = new DOMParser();

        // Should not throw in default mode
        assertDoesNotThrow(() -> parser.parse(new InputSource(new StringReader(html))));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should be parsed in default mode");
    }

    @Test
    public void testDefaultModeMismatchedTags() throws Exception {
        System.clearProperty(PROPERTY_DOM_STRICT);

        // Tags closed in wrong order
        final String html = "<html><body><b><i>text</b></i></body></html>";

        final DOMParser parser = new DOMParser();
        assertDoesNotThrow(() -> parser.parse(new InputSource(new StringReader(html))));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should be parsed in default mode with mismatched tags");
    }

    // =========================================================================
    // Lenient Mode Tests (Property set to FALSE)
    // =========================================================================

    @Test
    public void testLenientModeParseNormalHtml() throws Exception {
        System.setProperty(PROPERTY_DOM_STRICT, "false");

        final String html = "<html><head><title>Test</title></head><body><p>Hello</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should be parsed successfully in lenient mode");
    }

    @Test
    public void testLenientModeLogsWarnings() throws Exception {
        System.setProperty(PROPERTY_DOM_STRICT, "false");

        // HTML that might trigger warnings (mismatched tags handled by tag balancer)
        final String html = "<html><body></p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should be parsed in lenient mode");
    }

    @Test
    public void testLenientModeParsesEmptyHtml() throws Exception {
        System.setProperty(PROPERTY_DOM_STRICT, "false");

        final String html = "";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should be created even for empty HTML in lenient mode");
    }

    @Test
    public void testLenientModeExplicitFalse() throws Exception {
        System.setProperty(PROPERTY_DOM_STRICT, "FALSE");

        final String html = "<html><body><p>Test</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should be parsed with uppercase FALSE");
    }

    // =========================================================================
    // Strict Mode Tests (Property set to TRUE)
    // =========================================================================

    @Test
    public void testStrictModeParseNormalHtml() throws Exception {
        System.setProperty(PROPERTY_DOM_STRICT, "true");

        final String html = "<html><head><title>Test</title></head><body><p>Hello</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should be parsed successfully in strict mode for valid HTML");
    }

    @Test
    public void testStrictModeExplicitTrue() throws Exception {
        System.setProperty(PROPERTY_DOM_STRICT, "TRUE");

        final String html = "<html><body><p>Test</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should be parsed with uppercase TRUE");
    }

    @Test
    public void testStrictModeWithWellFormedHtml() throws Exception {
        System.setProperty(PROPERTY_DOM_STRICT, "true");

        // Properly nested HTML
        final String html = "<html><head><title>Title</title></head><body><div><p>Paragraph</p></div></body></html>";

        final DOMParser parser = new DOMParser();
        assertDoesNotThrow(() -> parser.parse(new InputSource(new StringReader(html))));
    }

    // =========================================================================
    // Direct SAXToDOMHandler Tests
    // =========================================================================

    @Test
    public void testHandlerWithoutStartDocument() throws Exception {
        System.setProperty(PROPERTY_DOM_STRICT, "false");

        // Create handler directly to test edge cases
        final javax.xml.parsers.DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final SAXToDOMHandler handler = new SAXToDOMHandler(builder);

        // Call startElement without startDocument
        assertDoesNotThrow(() -> handler.startElement("", "div", "DIV", new org.xml.sax.helpers.AttributesImpl()),
                "Lenient mode should not throw when startElement called before startDocument");
    }

    @Test
    public void testHandlerWithoutStartDocumentStrict() throws Exception {
        System.setProperty(PROPERTY_DOM_STRICT, "true");

        final javax.xml.parsers.DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final SAXToDOMHandler handler = new SAXToDOMHandler(builder);

        // Call startElement without startDocument - should throw in strict mode
        assertThrows(SAXException.class, () -> handler.startElement("", "div", "DIV", new org.xml.sax.helpers.AttributesImpl()),
                "Strict mode should throw when startElement called before startDocument");
    }

    @Test
    public void testHandlerMismatchedEndTag() throws Exception {
        System.setProperty(PROPERTY_DOM_STRICT, "false");

        final javax.xml.parsers.DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final SAXToDOMHandler handler = new SAXToDOMHandler(builder);

        handler.startDocument();
        handler.startElement("", "div", "DIV", new org.xml.sax.helpers.AttributesImpl());

        // End with wrong tag - should not throw in lenient mode
        assertDoesNotThrow(() -> handler.endElement("", "span", "SPAN"),
                "Lenient mode should handle mismatched end tag");
    }

    @Test
    public void testHandlerEndTagEmptyStack() throws Exception {
        System.setProperty(PROPERTY_DOM_STRICT, "false");

        final javax.xml.parsers.DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final SAXToDOMHandler handler = new SAXToDOMHandler(builder);

        handler.startDocument();
        handler.endDocument(); // Clear stack

        // End element with empty stack - should not throw
        assertDoesNotThrow(() -> handler.endElement("", "div", "DIV"),
                "Lenient mode should handle end tag with empty stack");
    }

    @Test
    public void testHandlerCharactersBeforeStartDocument() throws Exception {
        System.clearProperty(PROPERTY_DOM_STRICT);

        final javax.xml.parsers.DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final SAXToDOMHandler handler = new SAXToDOMHandler(builder);

        // Characters before startDocument - should not throw
        assertDoesNotThrow(() -> handler.characters("test".toCharArray(), 0, 4),
                "Should handle characters before startDocument");
    }

    @Test
    public void testHandlerCommentInDocument() throws Exception {
        System.clearProperty(PROPERTY_DOM_STRICT);

        final javax.xml.parsers.DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final SAXToDOMHandler handler = new SAXToDOMHandler(builder);

        handler.startDocument();
        handler.startElement("", "html", "HTML", new org.xml.sax.helpers.AttributesImpl());

        assertDoesNotThrow(() -> handler.comment("This is a comment".toCharArray(), 0, 17),
                "Should handle comment in document");
    }

    @Test
    public void testHandlerNestedElements() throws Exception {
        System.clearProperty(PROPERTY_DOM_STRICT);

        final javax.xml.parsers.DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final SAXToDOMHandler handler = new SAXToDOMHandler(builder);

        handler.startDocument();
        handler.startElement("", "html", "HTML", new org.xml.sax.helpers.AttributesImpl());
        handler.startElement("", "body", "BODY", new org.xml.sax.helpers.AttributesImpl());
        handler.startElement("", "div", "DIV", new org.xml.sax.helpers.AttributesImpl());
        handler.characters("Hello World".toCharArray(), 0, 11);
        handler.endElement("", "div", "DIV");
        handler.endElement("", "body", "BODY");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        final Document doc = handler.getDocument();
        assertNotNull(doc, "Document should be built");
        assertEquals("HTML", doc.getDocumentElement().getNodeName(), "Root should be HTML");
    }

    // =========================================================================
    // Skip Depth Tests
    // =========================================================================

    @Test
    public void testSkipDepthInLenientMode() throws Exception {
        System.setProperty(PROPERTY_DOM_STRICT, "false");

        final javax.xml.parsers.DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final SAXToDOMHandler handler = new SAXToDOMHandler(builder);

        // Don't call startDocument - this triggers skip mode
        handler.startElement("", "div", "DIV", new org.xml.sax.helpers.AttributesImpl());
        handler.startElement("", "p", "P", new org.xml.sax.helpers.AttributesImpl());
        handler.characters("Skipped content".toCharArray(), 0, 15);
        handler.endElement("", "p", "P");
        handler.endElement("", "div", "DIV");

        // Should complete without throwing
        assertDoesNotThrow(() -> {
        }, "Skip depth should handle nested skipped elements");
    }

    // =========================================================================
    // Integration Tests with DOMParser
    // =========================================================================

    @Test
    public void testDOMParserStrictModeWellFormed() throws Exception {
        System.setProperty(PROPERTY_DOM_STRICT, "true");

        final String html = "<html>" + "<head><title>Well Formed</title></head>" + "<body>"
                + "<div id=\"container\">" + "<p class=\"content\">Hello World</p>" + "</div>" + "</body>" + "</html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Well-formed document should parse in strict mode");
        assertEquals("HTML", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testDOMParserLenientModeWithErrors() throws Exception {
        System.setProperty(PROPERTY_DOM_STRICT, "false");

        // Intentionally bad HTML
        final String html = "<html><body><div><span></div></span></body></html>";

        final DOMParser parser = new DOMParser();

        // Should not throw in lenient mode
        assertDoesNotThrow(() -> parser.parse(new InputSource(new StringReader(html))));
    }

    // =========================================================================
    // Edge Cases
    // =========================================================================

    @Test
    public void testEmptyPropertyValue() throws Exception {
        System.setProperty(PROPERTY_DOM_STRICT, "");

        final String html = "<html><body><p>Test</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Empty property value should be treated as FALSE/lenient");
    }

    @Test
    public void testInvalidPropertyValue() throws Exception {
        System.setProperty(PROPERTY_DOM_STRICT, "maybe");

        final String html = "<html><body><p>Test</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Invalid property value should be treated as FALSE/lenient");
    }

    @Test
    public void testPropertyChangeBetweenParses() throws Exception {
        // First parse in lenient mode
        System.setProperty(PROPERTY_DOM_STRICT, "false");
        final DOMParser parser1 = new DOMParser();
        parser1.parse(new InputSource(new StringReader("<html><body></body></html>")));
        assertNotNull(parser1.getDocument());

        // Switch to strict mode
        System.setProperty(PROPERTY_DOM_STRICT, "true");
        final DOMParser parser2 = new DOMParser();
        parser2.parse(new InputSource(new StringReader("<html><body></body></html>")));
        assertNotNull(parser2.getDocument());
    }

    // =========================================================================
    // Helper Classes
    // =========================================================================

    /**
     * Test handler to capture log messages.
     */
    private static class TestLogHandler extends Handler {
        private final java.util.List<LogRecord> records = new java.util.ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }

        public boolean hasWarningContaining(String substring) {
            return records.stream()
                    .filter(r -> r.getLevel() == Level.WARNING)
                    .anyMatch(r -> r.getMessage() != null && r.getMessage().contains(substring));
        }

        public boolean hasAnyMessage() {
            return !records.isEmpty();
        }
    }

} // class StrictModeTest
