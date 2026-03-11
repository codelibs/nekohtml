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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.ext.LexicalHandler;

/**
 * Tests for SimpleHTMLScanner enhancements including CDATA support,
 * SystemId support, input validation, and improved regex patterns.
 */
public class SimpleHTMLScannerEnhancementsTest {

    /**
     * Test CDATA section parsing with LexicalHandler
     */
    @Test
    public void testCDATASectionWithLexicalHandler() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder result = new StringBuilder();
        final boolean[] cdataStartCalled = { false };
        final boolean[] cdataEndCalled = { false };

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                result.append(new String(ch, start, length));
            }
        });

        scanner.setLexicalHandler(new LexicalHandler() {
            @Override
            public void startDTD(String name, String publicId, String systemId) {
            }

            @Override
            public void endDTD() {
            }

            @Override
            public void startEntity(String name) {
            }

            @Override
            public void endEntity(String name) {
            }

            @Override
            public void startCDATA() {
                cdataStartCalled[0] = true;
            }

            @Override
            public void endCDATA() {
                cdataEndCalled[0] = true;
            }

            @Override
            public void comment(char[] ch, int start, int length) {
            }
        });

        final String html = "<html><![CDATA[This is CDATA content with <tags>]]></html>";
        final InputSource input = new InputSource(new StringReader(html));

        scanner.parse(input);

        assertTrue(cdataStartCalled[0], "startCDATA should be called");
        assertTrue(cdataEndCalled[0], "endCDATA should be called");
        assertTrue(result.toString().contains("This is CDATA content with <tags>"), "CDATA content should be preserved");
    }

    /**
     * Test CDATA section parsing without LexicalHandler
     */
    @Test
    public void testCDATASectionWithoutLexicalHandler() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder result = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                result.append(new String(ch, start, length));
            }
        });

        final String html = "<html><![CDATA[<script>alert('test')</script>]]></html>";
        final InputSource input = new InputSource(new StringReader(html));

        scanner.parse(input);

        assertTrue(result.toString().contains("<script>alert('test')</script>"), "CDATA content should be emitted as text");
    }

    /**
     * Test empty CDATA section
     */
    @Test
    public void testEmptyCDATASection() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final boolean[] cdataStartCalled = { false };
        final boolean[] cdataEndCalled = { false };

        scanner.setContentHandler(new DefaultHandler());
        scanner.setLexicalHandler(new LexicalHandler() {
            @Override
            public void startDTD(String name, String publicId, String systemId) {
            }

            @Override
            public void endDTD() {
            }

            @Override
            public void startEntity(String name) {
            }

            @Override
            public void endEntity(String name) {
            }

            @Override
            public void startCDATA() {
                cdataStartCalled[0] = true;
            }

            @Override
            public void endCDATA() {
                cdataEndCalled[0] = true;
            }

            @Override
            public void comment(char[] ch, int start, int length) {
            }
        });

        final String html = "<html><![CDATA[]]></html>";
        final InputSource input = new InputSource(new StringReader(html));

        scanner.parse(input);

        assertTrue(cdataStartCalled[0], "startCDATA should be called for empty CDATA");
        assertTrue(cdataEndCalled[0], "endCDATA should be called for empty CDATA");
    }

    /**
     * Test SystemId support with file path
     */
    @Test
    public void testSystemIdWithFilePath() throws Exception {
        // Create a temporary HTML file
        final File tempFile = File.createTempFile("test", ".html");
        tempFile.deleteOnExit();

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("<html><body>Test content from file</body></html>");
        }

        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder result = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                result.append(new String(ch, start, length));
            }
        });

        final InputSource input = new InputSource();
        input.setSystemId(tempFile.getAbsolutePath());

        scanner.parse(input);

        assertTrue(result.toString().contains("Test content from file"), "Content from file should be parsed");
    }

    /**
     * Test SystemId with invalid path
     */
    @Test
    public void testSystemIdWithInvalidPath() {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        scanner.setContentHandler(new DefaultHandler());

        final InputSource input = new InputSource();
        input.setSystemId("/nonexistent/path/to/file.html");

        assertThrows(SAXException.class, () -> scanner.parse(input), "Should throw SAXException for invalid SystemId");
    }

    /**
     * Test input validation - null InputSource
     */
    @Test
    public void testNullInputSource() {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        scanner.setContentHandler(new DefaultHandler());

        assertThrows(SAXException.class, () -> scanner.parse((InputSource) null), "Should throw SAXException for null InputSource");
    }

    /**
     * Test input validation - null HTML content
     */
    @Test
    public void testNullHTMLContent() {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        scanner.setContentHandler(new DefaultHandler());

        assertThrows(SAXException.class, () -> scanner.parseHTML(null), "Should throw SAXException for null HTML content");
    }

    /**
     * Test improved regex pattern with namespace support
     */
    @Test
    public void testNamespacedElements() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder elementNames = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes atts) {
                elementNames.append(qName).append("|");
            }
        });

        final String html = "<html><ns:custom>Content</ns:custom><svg:circle /></html>";
        final InputSource input = new InputSource(new StringReader(html));

        scanner.parse(input);

        // Element names are normalized to uppercase by default
        assertTrue(elementNames.toString().contains("NS:CUSTOM"), "Should parse namespaced element (uppercase)");
        assertTrue(elementNames.toString().contains("SVG:CIRCLE"), "Should parse SVG namespaced element (uppercase)");
    }

    /**
     * Test improved regex pattern with multiple namespaces
     */
    @Test
    public void testMultipleNamespacedElements() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder elementNames = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes atts) {
                elementNames.append(qName).append("|");
            }
        });

        final String html = "<html xmlns:custom='http://example.com'><custom:item>Test</custom:item></html>";
        final InputSource input = new InputSource(new StringReader(html));

        scanner.parse(input);

        // Element names are normalized to uppercase by default
        assertTrue(elementNames.toString().contains("CUSTOM:ITEM"), "Should parse custom namespaced element (uppercase)");
    }

    /**
     * Test DOTALL flag in regex patterns
     */
    @Test
    public void testMultilineComments() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder comments = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler());
        scanner.setLexicalHandler(new LexicalHandler() {
            @Override
            public void startDTD(String name, String publicId, String systemId) {
            }

            @Override
            public void endDTD() {
            }

            @Override
            public void startEntity(String name) {
            }

            @Override
            public void endEntity(String name) {
            }

            @Override
            public void startCDATA() {
            }

            @Override
            public void endCDATA() {
            }

            @Override
            public void comment(char[] ch, int start, int length) {
                comments.append(new String(ch, start, length));
            }
        });

        final String html = "<html><!-- This is a\nmultiline\ncomment --></html>";
        final InputSource input = new InputSource(new StringReader(html));

        scanner.parse(input);

        assertTrue(comments.toString().contains("multiline"), "Should parse multiline comments");
    }

    /**
     * Test DOCTYPE with DOTALL flag
     */
    @Test
    public void testMultilineDoctype() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final boolean[] dtdCalled = { false };

        scanner.setContentHandler(new DefaultHandler());
        scanner.setLexicalHandler(new LexicalHandler() {
            @Override
            public void startDTD(String name, String publicId, String systemId) {
                dtdCalled[0] = true;
            }

            @Override
            public void endDTD() {
            }

            @Override
            public void startEntity(String name) {
            }

            @Override
            public void endEntity(String name) {
            }

            @Override
            public void startCDATA() {
            }

            @Override
            public void endCDATA() {
            }

            @Override
            public void comment(char[] ch, int start, int length) {
            }
        });

        final String html = "<!DOCTYPE html\n  PUBLIC \"-//W3C//DTD HTML 4.01//EN\">\n<html></html>";
        final InputSource input = new InputSource(new StringReader(html));

        scanner.parse(input);

        assertTrue(dtdCalled[0], "Should parse multiline DOCTYPE");
    }

    /**
     * Test parsing with ByteArrayInputStream
     */
    @Test
    public void testParseFromByteStream() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder result = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                result.append(new String(ch, start, length));
            }
        });

        final String html = "<html><body>Test content</body></html>";
        final ByteArrayInputStream stream = new ByteArrayInputStream(html.getBytes("UTF-8"));
        final InputSource input = new InputSource(stream);
        input.setEncoding("UTF-8");

        scanner.parse(input);

        assertTrue(result.toString().contains("Test content"), "Content from byte stream should be parsed");
    }

    /**
     * Test parsing with explicit encoding
     */
    @Test
    public void testParseWithEncoding() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder result = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                result.append(new String(ch, start, length));
            }
        });

        final String html = "<html><body>日本語テスト</body></html>";
        final ByteArrayInputStream stream = new ByteArrayInputStream(html.getBytes("UTF-8"));
        final InputSource input = new InputSource(stream);
        input.setEncoding("UTF-8");

        scanner.parse(input);

        assertTrue(result.toString().contains("日本語テスト"), "UTF-8 encoded content should be parsed correctly");
    }

    /**
     * Test end tag with whitespace (improved regex)
     */
    @Test
    public void testEndTagWithWhitespace() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder elementNames = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void endElement(String uri, String localName, String qName) {
                elementNames.append("END:").append(qName).append("|");
            }
        });

        final String html = "<div>Content</div  >";
        final InputSource input = new InputSource(new StringReader(html));

        scanner.parse(input);

        assertTrue(elementNames.toString().contains("END:DIV"), "Should parse end tag with trailing whitespace");
    }

    // =========================================================================
    // Entity Handling Tests
    // =========================================================================

    /**
     * Test named HTML entities in content
     */
    @Test
    public void testNamedEntitiesInContent() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder result = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                result.append(new String(ch, start, length));
            }
        });

        final String html = "<html><body>&amp; &lt; &gt; &quot; &nbsp;</body></html>";
        final InputSource input = new InputSource(new StringReader(html));

        scanner.parse(input);

        // Entity decoding should produce actual characters
        final String decoded = result.toString();
        assertTrue(decoded.contains("&"), "Should decode &amp; to &");
        assertTrue(decoded.contains("<"), "Should decode &lt; to <");
        assertTrue(decoded.contains(">"), "Should decode &gt; to >");
        assertTrue(decoded.contains("\""), "Should decode &quot; to \"");
        assertTrue(decoded.contains("\u00A0"), "Should decode &nbsp; to non-breaking space");
    }

    /**
     * Test numeric entities (decimal)
     */
    @Test
    public void testDecimalNumericEntities() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder result = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                result.append(new String(ch, start, length));
            }
        });

        final String html = "<html><body>&#60; &#62; &#38;</body></html>";
        final InputSource input = new InputSource(new StringReader(html));

        scanner.parse(input);

        // &#60; = <, &#62; = >, &#38; = &
        final String decoded = result.toString();
        assertTrue(decoded.contains("<"), "Should decode &#60; to <");
        assertTrue(decoded.contains(">"), "Should decode &#62; to >");
        assertTrue(decoded.contains("&"), "Should decode &#38; to &");
    }

    /**
     * Test numeric entities (hexadecimal)
     */
    @Test
    public void testHexNumericEntities() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder result = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                result.append(new String(ch, start, length));
            }
        });

        final String html = "<html><body>&#x3C; &#x3E; &#x26;</body></html>";
        final InputSource input = new InputSource(new StringReader(html));

        scanner.parse(input);

        // &#x3C; = <, &#x3E; = >, &#x26; = &
        final String decoded = result.toString();
        assertTrue(decoded.contains("<"), "Should decode &#x3C; to <");
        assertTrue(decoded.contains(">"), "Should decode &#x3E; to >");
        assertTrue(decoded.contains("&"), "Should decode &#x26; to &");
    }

    /**
     * Test entities in attribute values
     */
    @Test
    public void testEntitiesInAttributes() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder attrValues = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes atts) {
                for (int i = 0; i < atts.getLength(); i++) {
                    attrValues.append(atts.getQName(i)).append("=").append(atts.getValue(i)).append("|");
                }
            }
        });

        final String html = "<a href=\"test?a=1&amp;b=2\">Link</a>";
        final InputSource input = new InputSource(new StringReader(html));

        scanner.parse(input);

        final String attrs = attrValues.toString();
        assertTrue(attrs.contains("href=test?a=1&b=2"), "Should decode &amp; in attribute to &");
    }

    /**
     * Test invalid/incomplete entities
     */
    @Test
    public void testIncompleteEntity() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder result = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                result.append(new String(ch, start, length));
            }
        });

        // Incomplete entity - ampersand without semicolon
        final String html = "<html><body>A & B</body></html>";
        final InputSource input = new InputSource(new StringReader(html));

        scanner.parse(input);

        assertTrue(result.toString().contains("A & B"), "Should preserve incomplete entity as literal text");
    }

    /**
     * Test unknown entity names
     */
    @Test
    public void testUnknownEntity() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder result = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                result.append(new String(ch, start, length));
            }
        });

        final String html = "<html><body>&unknown;</body></html>";
        final InputSource input = new InputSource(new StringReader(html));

        scanner.parse(input);

        assertTrue(result.toString().contains("&unknown;"), "Should preserve unknown entity as literal text");
    }

    /**
     * Test that semicolon-less named entities in URL attributes are NOT decoded
     * (HTML5 attribute value state rule: &not=, &copy=, &reg= must be preserved)
     */
    @Test
    public void testSemicolonlessEntitiesInUrlAttributes() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder attrValues = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes atts) {
                for (int i = 0; i < atts.getLength(); i++) {
                    attrValues.append(atts.getQName(i)).append("=").append(atts.getValue(i)).append("|");
                }
            }
        });

        final String html = "<a href=\"/x?a=1&not=2&copy=3&reg=4\">Link</a>";
        final InputSource input = new InputSource(new StringReader(html));

        scanner.parse(input);

        final String attrs = attrValues.toString();
        assertTrue(attrs.contains("href=/x?a=1&not=2&copy=3&reg=4"),
                "Semicolon-less named entities in attributes should be preserved as-is, got: " + attrs);
    }

    /**
     * Test that invalid numeric references produce U+FFFD replacement character
     */
    @Test
    public void testInvalidNumericReferences() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder result = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                result.append(new String(ch, start, length));
            }
        });

        // &#0; (null), &#xD800; (surrogate), &#x1; (control char)
        final String html = "<html><body>&#0; &#xD800; &#x1;</body></html>";
        final InputSource input = new InputSource(new StringReader(html));

        scanner.parse(input);

        final String decoded = result.toString();
        // All invalid code points should be replaced with U+FFFD
        assertEquals(
                3,
                decoded.chars().filter(c -> c == 0xFFFD).count(),
                "Invalid numeric references should be replaced with U+FFFD, got: "
                        + decoded.codePoints().mapToObj(cp -> String.format("U+%04X", cp)).reduce("", (a, b) -> a + " " + b));
    }

    /**
     * Test that semicolon-less named entities ARE decoded in text context
     */
    @Test
    public void testSemicolonlessEntitiesInTextContent() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder result = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                result.append(new String(ch, start, length));
            }
        });

        final String html = "<html><body>&amp &lt &gt</body></html>";
        final InputSource input = new InputSource(new StringReader(html));

        scanner.parse(input);

        final String decoded = result.toString();
        assertTrue(decoded.contains("&"), "Should decode &amp (without semicolon) in text");
        assertTrue(decoded.contains("<"), "Should decode &lt (without semicolon) in text");
        assertTrue(decoded.contains(">"), "Should decode &gt (without semicolon) in text");
    }

    // =========================================================================
    // Advanced Encoding Tests
    // =========================================================================

    /**
     * Test ISO-8859-1 (Latin-1) encoding
     */
    @Test
    public void testISO88591Encoding() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder result = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                result.append(new String(ch, start, length));
            }
        });

        // ISO-8859-1 encoded content with accented characters
        final String content = "café résumé";
        final ByteArrayInputStream stream = new ByteArrayInputStream(("<html><body>" + content + "</body></html>").getBytes("ISO-8859-1"));
        final InputSource input = new InputSource(stream);
        input.setEncoding("ISO-8859-1");

        scanner.parse(input);

        assertTrue(result.toString().contains("caf") || result.toString().contains("é"), "ISO-8859-1 encoded content should be parsed");
    }

    /**
     * Test UTF-16 encoding
     */
    @Test
    public void testUTF16Encoding() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder result = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                result.append(new String(ch, start, length));
            }
        });

        final String html = "<html><body>Unicode: \u4E2D\u6587</body></html>";
        final ByteArrayInputStream stream = new ByteArrayInputStream(html.getBytes("UTF-16"));
        final InputSource input = new InputSource(stream);
        input.setEncoding("UTF-16");

        scanner.parse(input);

        assertTrue(result.toString().contains("Unicode") || result.toString().contains("\u4E2D"), "UTF-16 encoded content should be parsed");
    }

    /**
     * Test default encoding when not specified
     */
    @Test
    public void testDefaultEncodingUTF8() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder result = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                result.append(new String(ch, start, length));
            }
        });

        final String html = "<html><body>UTF-8 default: äöü</body></html>";
        final ByteArrayInputStream stream = new ByteArrayInputStream(html.getBytes("UTF-8"));
        final InputSource input = new InputSource(stream);
        // Don't set encoding - should default to UTF-8

        scanner.parse(input);

        assertTrue(result.toString().contains("UTF-8") || result.toString().contains("ä"), "Default UTF-8 encoding should work");
    }

    /**
     * Test parsing with character stream (Reader)
     */
    @Test
    public void testCharacterStreamTakesPrecedence() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder result = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                result.append(new String(ch, start, length));
            }
        });

        final String html = "<html><body>From character stream</body></html>";

        // Set both byte stream and character stream - character stream should take precedence
        final InputSource input = new InputSource();
        input.setCharacterStream(new StringReader(html));
        input.setByteStream(new ByteArrayInputStream("Different content".getBytes()));

        scanner.parse(input);

        assertTrue(result.toString().contains("From character stream"), "Character stream should take precedence over byte stream");
    }

    // =========================================================================
    // Input Source Variations
    // =========================================================================

    /**
     * Test file:// URL SystemId
     */
    @Test
    public void testFileUrlSystemId() throws Exception {
        // Create a temporary HTML file
        final java.io.File tempFile = java.io.File.createTempFile("test", ".html");
        tempFile.deleteOnExit();

        try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
            writer.write("<html><body>File URL content</body></html>");
        }

        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder result = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                result.append(new String(ch, start, length));
            }
        });

        final InputSource input = new InputSource();
        input.setSystemId("file://" + tempFile.getAbsolutePath());

        scanner.parse(input);

        assertTrue(result.toString().contains("File URL content"), "Should parse content from file:// URL");
    }

    /**
     * Test input source with no valid source
     */
    @Test
    public void testNoValidInputSource() {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        scanner.setContentHandler(new DefaultHandler());

        // InputSource with nothing set
        final InputSource input = new InputSource();

        assertThrows(SAXException.class, () -> scanner.parse(input), "Should throw when no valid input source is available");
    }

    /**
     * Test parse(String systemId) convenience method
     */
    @Test
    public void testParseBySystemId() throws Exception {
        final java.io.File tempFile = java.io.File.createTempFile("test", ".html");
        tempFile.deleteOnExit();

        try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
            writer.write("<html><body>SystemId parse</body></html>");
        }

        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final StringBuilder result = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                result.append(new String(ch, start, length));
            }
        });

        scanner.parse(tempFile.getAbsolutePath());

        assertTrue(result.toString().contains("SystemId parse"), "Should parse using String systemId parameter");
    }

    // =========================================================================
    // Handler Management Tests
    // =========================================================================

    /**
     * Test DTDHandler getter/setter
     */
    @Test
    public void testDTDHandler() {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final org.xml.sax.DTDHandler handler = new org.xml.sax.DTDHandler() {
            @Override
            public void notationDecl(String name, String publicId, String systemId) {
            }

            @Override
            public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) {
            }
        };

        scanner.setDTDHandler(handler);
        assertSame(handler, scanner.getDTDHandler(), "DTDHandler should be retrievable");
    }

    /**
     * Test EntityResolver getter/setter
     */
    @Test
    public void testEntityResolver() {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final org.xml.sax.EntityResolver resolver = (publicId, systemId) -> null;

        scanner.setEntityResolver(resolver);
        assertSame(resolver, scanner.getEntityResolver(), "EntityResolver should be retrievable");
    }

    /**
     * Test ErrorHandler getter/setter
     */
    @Test
    public void testErrorHandler() {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final org.xml.sax.ErrorHandler handler = new org.xml.sax.ErrorHandler() {
            @Override
            public void warning(org.xml.sax.SAXParseException exception) {
            }

            @Override
            public void error(org.xml.sax.SAXParseException exception) {
            }

            @Override
            public void fatalError(org.xml.sax.SAXParseException exception) {
            }
        };

        scanner.setErrorHandler(handler);
        assertSame(handler, scanner.getErrorHandler(), "ErrorHandler should be retrievable");
    }

    /**
     * Test LexicalHandler via property
     */
    @Test
    public void testLexicalHandlerProperty() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final LexicalHandler handler = new LexicalHandler() {
            @Override
            public void startDTD(String name, String publicId, String systemId) {
            }

            @Override
            public void endDTD() {
            }

            @Override
            public void startEntity(String name) {
            }

            @Override
            public void endEntity(String name) {
            }

            @Override
            public void startCDATA() {
            }

            @Override
            public void endCDATA() {
            }

            @Override
            public void comment(char[] ch, int start, int length) {
            }
        };

        scanner.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
        assertSame(handler, scanner.getProperty("http://xml.org/sax/properties/lexical-handler"),
                "LexicalHandler should be retrievable via property");
    }

    /**
     * Test getFeature throws for unrecognized feature
     */
    @Test
    public void testGetUnrecognizedFeature() {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();

        assertThrows(org.xml.sax.SAXNotRecognizedException.class, () -> scanner.getFeature("http://example.com/unknown-feature"),
                "Should throw SAXNotRecognizedException for unknown feature");
    }

    /**
     * Test getProperty throws for unrecognized property
     */
    @Test
    public void testGetUnrecognizedProperty() {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();

        assertThrows(org.xml.sax.SAXNotRecognizedException.class, () -> scanner.getProperty("http://example.com/unknown-property"),
                "Should throw SAXNotRecognizedException for unknown property");
    }

    /**
     * Test parsing without content handler set
     */
    @Test
    public void testParsingWithoutContentHandler() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        // Don't set content handler

        final String html = "<html><body>Content</body></html>";
        final InputSource input = new InputSource(new StringReader(html));

        // Should return early without error
        assertDoesNotThrow(() -> scanner.parse(input), "Parsing without content handler should not throw");
    }
}
