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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

/**
 * Test class for {@link HTMLSAXScanner}.
 *
 * @author CodeLibs Project
 */
public class HTMLSAXScannerTest {

    private HTMLSAXScanner scanner;
    private ContentHandler contentHandler;
    private LexicalHandler lexicalHandler;

    @BeforeEach
    public void setUp() {
        scanner = new HTMLSAXScanner();
        contentHandler = mock(ContentHandler.class);
        lexicalHandler = mock(LexicalHandler.class);
    }

    @Test
    public void testDefaultConstructor() {
        // When: Creating with default constructor
        final HTMLSAXScanner s = new HTMLSAXScanner();

        // Then: Should be initialized
        assertNotNull(s, "Scanner should be created");
        assertNull(s.getParent(), "Parent should be null");
    }

    @Test
    public void testConstructorWithParent() {
        // Given: Parent XMLReader
        final XMLReader parent = mock(XMLReader.class);

        // When: Creating with parent
        final HTMLSAXScanner s = new HTMLSAXScanner(parent);

        // Then: Should be initialized with parent
        assertNotNull(s, "Scanner should be created");
        assertSame(parent, s.getParent(), "Parent should be set");
    }

    @Test
    public void testSetContentHandler() {
        // When: Setting content handler
        scanner.setContentHandler(contentHandler);

        // Then: Should be set in both scanner and underlying SimpleHTMLScanner
        assertSame(contentHandler, scanner.getContentHandler(), "Content handler should be set");
    }

    @Test
    public void testSetAndGetLexicalHandler() {
        // When: Setting lexical handler
        scanner.setLexicalHandler(lexicalHandler);

        // Then: Should be able to get it back
        assertSame(lexicalHandler, scanner.getLexicalHandler(), "Lexical handler should be set");
    }

    @Test
    public void testSetLexicalHandlerNull() {
        // When: Setting lexical handler to null
        scanner.setLexicalHandler(null);

        // Then: Should be null
        assertNull(scanner.getLexicalHandler(), "Lexical handler should be null");
    }

    @Test
    public void testParseWithInputSource() throws Exception {
        // Given: HTML content
        final String html = "<html><body><p>Test</p></body></html>";
        scanner.setContentHandler(contentHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing
        scanner.parse(input);

        // Then: Should delegate to SimpleHTMLScanner
        verify(contentHandler).startDocument();
        verify(contentHandler).endDocument();
        verify(contentHandler, atLeastOnce()).startElement(anyString(), anyString(), anyString(), any(Attributes.class));
    }

    @Test
    public void testParseWithSystemId() throws Exception {
        // Given: System ID (should throw as not implemented in SimpleHTMLScanner)
        scanner.setContentHandler(contentHandler);

        // When/Then: Should throw SAXException
        assertThrows(Exception.class, () -> {
            scanner.parse("http://example.com/test.html");
        });
    }

    @Test
    public void testParseComplexHTML() throws Exception {
        // Given: Complex HTML
        final String html =
                "<!DOCTYPE html>\n" + "<html>\n" + "  <head>\n" + "    <title>Test</title>\n" + "  </head>\n" + "  <body>\n"
                        + "    <div id=\"main\">\n" + "      <p>Paragraph</p>\n" + "    </div>\n" + "  </body>\n" + "</html>";

        final List<String> elements = new ArrayList<>();
        scanner.setContentHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
                elements.add(qName);
            }
        });
        scanner.setLexicalHandler(lexicalHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing
        scanner.parse(input);

        // Then: Should parse all elements
        assertTrue(elements.contains("HTML"), "Should parse HTML");
        assertTrue(elements.contains("HEAD"), "Should parse HEAD");
        assertTrue(elements.contains("TITLE"), "Should parse TITLE");
        assertTrue(elements.contains("BODY"), "Should parse BODY");
        assertTrue(elements.contains("DIV"), "Should parse DIV");
        assertTrue(elements.contains("P"), "Should parse P");

        // Verify lexical handler received DTD
        verify(lexicalHandler).startDTD(eq("html"), isNull(), isNull());
        verify(lexicalHandler).endDTD();
    }

    @Test
    public void testGetFeatureThrowsException() {
        // When: Getting feature
        // Then: Should throw SAXNotRecognizedException (delegated to SimpleHTMLScanner)
        assertThrows(SAXNotRecognizedException.class, () -> {
            scanner.getFeature("any-feature");
        });
    }

    @Test
    public void testSetFeatureDoesNotThrow() {
        // When: Setting feature
        // Then: Should not throw (delegated to SimpleHTMLScanner which doesn't throw)
        assertDoesNotThrow(() -> {
            scanner.setFeature("any-feature", true);
        });
    }

    @Test
    public void testGetPropertyLexicalHandler() throws Exception {
        // Given: Lexical handler set
        scanner.setLexicalHandler(lexicalHandler);

        // When: Getting lexical handler property
        final Object property = scanner.getProperty("http://xml.org/sax/properties/lexical-handler");

        // Then: Should return the lexical handler
        assertSame(lexicalHandler, property, "Should return lexical handler");
    }

    @Test
    public void testGetPropertyUnrecognized() {
        // When: Getting unrecognized property
        // Then: Should throw SAXNotRecognizedException (delegated to SimpleHTMLScanner)
        assertThrows(SAXNotRecognizedException.class, () -> {
            scanner.getProperty("unrecognized-property");
        });
    }

    @Test
    public void testSetPropertyLexicalHandler() throws Exception {
        // When: Setting lexical handler via property
        scanner.setProperty("http://xml.org/sax/properties/lexical-handler", lexicalHandler);

        // Then: Should be set
        assertSame(lexicalHandler, scanner.getLexicalHandler(), "Lexical handler should be set via property");
    }

    @Test
    public void testSetPropertyUnrecognized() {
        // When: Setting unrecognized property
        // Then: Should not throw (delegated to SimpleHTMLScanner)
        assertDoesNotThrow(() -> {
            scanner.setProperty("unrecognized-property", "value");
        });
    }

    @Test
    public void testContentHandlerDelegation() throws Exception {
        // Given: HTML content
        final String html = "<div>Test</div>";
        scanner.setContentHandler(contentHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing
        scanner.parse(input);

        // Then: Should delegate to content handler (may have multiple character calls)
        verify(contentHandler).startDocument();
        verify(contentHandler).startElement(eq(""), eq("DIV"), eq("DIV"), any(Attributes.class));
        verify(contentHandler, atLeastOnce()).characters(any(char[].class), anyInt(), anyInt());
        verify(contentHandler).endElement(eq(""), eq("DIV"), eq("DIV"));
        verify(contentHandler).endDocument();
    }

    @Test
    public void testLexicalHandlerDelegation() throws Exception {
        // Given: HTML with comment
        final String html = "<!-- Comment --><div>Test</div>";
        scanner.setContentHandler(contentHandler);
        scanner.setLexicalHandler(lexicalHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing
        scanner.parse(input);

        // Then: Should delegate to lexical handler
        verify(lexicalHandler).comment(any(char[].class), anyInt(), anyInt());
    }

    @Test
    public void testParseWithAttributes() throws Exception {
        // Given: HTML with attributes
        final String html = "<div id=\"test\" class=\"container\">Content</div>";
        final List<Attributes> capturedAttrs = new ArrayList<>();
        scanner.setContentHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
                if ("DIV".equals(qName)) {
                    capturedAttrs.add(new org.xml.sax.helpers.AttributesImpl(attributes));
                }
            }
        });
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing
        scanner.parse(input);

        // Then: Should capture attributes
        assertEquals(1, capturedAttrs.size(), "Should have one DIV element");
        final Attributes attrs = capturedAttrs.get(0);
        assertEquals("test", attrs.getValue("id"), "Should have id attribute");
        assertEquals("container", attrs.getValue("class"), "Should have class attribute");
    }

    @Test
    public void testParseVoidElements() throws Exception {
        // Given: HTML with void elements
        final String html = "<div><br><hr><img src=\"test.png\"></div>";
        final List<String> elements = new ArrayList<>();
        scanner.setContentHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
                elements.add(qName);
            }
        });
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing
        scanner.parse(input);

        // Then: Should parse void elements
        assertTrue(elements.contains("DIV"), "Should have DIV");
        assertTrue(elements.contains("BR"), "Should have BR");
        assertTrue(elements.contains("HR"), "Should have HR");
        assertTrue(elements.contains("IMG"), "Should have IMG");
    }

    @Test
    public void testParseNestedElements() throws Exception {
        // Given: Nested HTML
        final String html = "<div><p><span>Text</span></p></div>";
        final List<String> elements = new ArrayList<>();
        scanner.setContentHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
                elements.add(qName);
            }
        });
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing
        scanner.parse(input);

        // Then: Should parse nested structure
        assertEquals(3, elements.size(), "Should have 3 elements");
        assertEquals("DIV", elements.get(0), "First should be DIV");
        assertEquals("P", elements.get(1), "Second should be P");
        assertEquals("SPAN", elements.get(2), "Third should be SPAN");
    }

    @Test
    public void testParseWithDTD() throws Exception {
        // Given: HTML with DOCTYPE
        final String html =
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">"
                        + "<html><body>Test</body></html>";
        scanner.setContentHandler(contentHandler);
        scanner.setLexicalHandler(lexicalHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing
        scanner.parse(input);

        // Then: Should report the DOCTYPE name along with the PUBLIC and SYSTEM identifiers
        verify(lexicalHandler).startDTD(eq("html"), eq("-//W3C//DTD HTML 4.01//EN"), eq("http://www.w3.org/TR/html4/strict.dtd"));
        verify(lexicalHandler).endDTD();
    }

    @Test
    public void testParseTextContent() throws Exception {
        // Given: HTML with text
        final String html = "<p>Hello World</p>";
        final List<String> textContent = new ArrayList<>();
        scanner.setContentHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) {
                textContent.add(new String(ch, start, length));
            }
        });
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing
        scanner.parse(input);

        // Then: Should capture text
        assertTrue(textContent.stream().anyMatch(text -> text.contains("Hello World")), "Should have text content");
    }

    @Test
    public void testParseEmptyHTML() throws Exception {
        // Given: Empty HTML
        final String html = "";
        scanner.setContentHandler(contentHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing
        scanner.parse(input);

        // Then: Should only emit document events
        verify(contentHandler).startDocument();
        verify(contentHandler).endDocument();
        verify(contentHandler, never()).startElement(anyString(), anyString(), anyString(), any(Attributes.class));
    }

    @Test
    public void testParseWithComment() throws Exception {
        // Given: HTML with comment
        final String html = "<div><!-- This is a comment -->Content</div>";
        scanner.setContentHandler(contentHandler);
        scanner.setLexicalHandler(lexicalHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing
        scanner.parse(input);

        // Then: Should emit comment event
        verify(lexicalHandler).comment(any(char[].class), anyInt(), anyInt());
    }

    @Test
    public void testMultipleParses() throws Exception {
        // Given: Multiple HTML documents
        final String html1 = "<div>First</div>";
        final String html2 = "<span>Second</span>";
        scanner.setContentHandler(contentHandler);

        // When: Parsing multiple times
        scanner.parse(new InputSource(new StringReader(html1)));
        reset(contentHandler);
        scanner.setContentHandler(contentHandler);
        scanner.parse(new InputSource(new StringReader(html2)));

        // Then: Both should parse successfully
        verify(contentHandler).startDocument();
        verify(contentHandler).startElement(eq(""), eq("SPAN"), eq("SPAN"), any(Attributes.class));
        verify(contentHandler).endDocument();
    }

    @Test
    public void testInheritedXMLFilterMethods() throws Exception {
        // Given: Scanner with parent
        final XMLReader parent = mock(XMLReader.class);
        final HTMLSAXScanner s = new HTMLSAXScanner(parent);

        // When: Accessing parent
        final XMLReader retrievedParent = s.getParent();

        // Then: Should return parent
        assertSame(parent, retrievedParent, "Should return parent XMLReader");
    }

    @Test
    public void testSetParent() {
        // Given: New parent
        final XMLReader parent = mock(XMLReader.class);

        // When: Setting parent
        scanner.setParent(parent);

        // Then: Should be set
        assertSame(parent, scanner.getParent(), "Parent should be set");
    }

    @Test
    public void testUnicodeContent() throws Exception {
        // Given: HTML with Unicode
        final String html = "<p>日本語テスト</p>";
        final List<String> textContent = new ArrayList<>();
        scanner.setContentHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) {
                textContent.add(new String(ch, start, length));
            }
        });
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing
        scanner.parse(input);

        // Then: Should handle Unicode
        assertTrue(textContent.stream().anyMatch(text -> text.contains("日本語テスト")), "Should handle Unicode text");
    }

    @Test
    public void testWhitespacePreservation() throws Exception {
        // Given: HTML with whitespace
        final String html = "<div>Text1</div>  <div>Text2</div>";
        final List<String> textContent = new ArrayList<>();
        scanner.setContentHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) {
                final String text = new String(ch, start, length);
                if (text.length() > 0) {
                    textContent.add(text);
                }
            }
        });
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing
        scanner.parse(input);

        // Then: Should preserve whitespace
        assertTrue(textContent.size() >= 3, "Should have at least 3 text nodes");
        assertTrue(textContent.stream().anyMatch(text -> text.trim().isEmpty()), "Should contain whitespace node");
    }

    @Test
    public void testComplexAttributeValues() throws Exception {
        // Given: HTML with complex attributes (no hyphens due to SimpleHTMLScanner regex limitation)
        final String html = "<div datavalue=\"test&amp;value\" title=\"Title with 'quotes'\">Content</div>";
        final List<Attributes> capturedAttrs = new ArrayList<>();
        scanner.setContentHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
                if ("DIV".equals(qName)) {
                    capturedAttrs.add(new org.xml.sax.helpers.AttributesImpl(attributes));
                }
            }
        });
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing
        scanner.parse(input);

        // Then: Should handle complex attributes
        assertEquals(1, capturedAttrs.size(), "Should have one DIV");
        final Attributes attrs = capturedAttrs.get(0);
        assertNotNull(attrs.getValue("datavalue"), "Should have datavalue attribute");
        assertNotNull(attrs.getValue("title"), "Should have title attribute");
    }

} // class HTMLSAXScannerTest
