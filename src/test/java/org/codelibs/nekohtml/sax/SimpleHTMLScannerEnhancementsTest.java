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

        assertTrue(elementNames.toString().contains("ns:custom"), "Should parse namespaced element");
        assertTrue(elementNames.toString().contains("svg:circle"), "Should parse SVG namespaced element");
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

        assertTrue(elementNames.toString().contains("custom:item"), "Should parse custom namespaced element");
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
}
