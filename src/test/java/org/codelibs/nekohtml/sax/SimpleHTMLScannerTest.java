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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.ext.LexicalHandler;

/**
 * Test class for {@link SimpleHTMLScanner}.
 *
 * @author CodeLibs Project
 */
public class SimpleHTMLScannerTest {

    private SimpleHTMLScanner scanner;
    private ContentHandler contentHandler;
    private LexicalHandler lexicalHandler;

    @BeforeEach
    public void setUp() {
        scanner = new SimpleHTMLScanner();
        contentHandler = mock(ContentHandler.class);
        lexicalHandler = mock(LexicalHandler.class);
    }

    @Test
    public void testSetAndGetContentHandler() {
        // When: Setting content handler
        scanner.setContentHandler(contentHandler);

        // Then: Should be able to get it back
        assertSame(contentHandler, scanner.getContentHandler());
    }

    @Test
    public void testSetAndGetDTDHandler() {
        // Given: A DTD handler
        final DTDHandler dtdHandler = mock(DTDHandler.class);

        // When: Setting DTD handler
        scanner.setDTDHandler(dtdHandler);

        // Then: Should be able to get it back
        assertSame(dtdHandler, scanner.getDTDHandler());
    }

    @Test
    public void testSetAndGetEntityResolver() {
        // Given: An entity resolver
        final EntityResolver entityResolver = mock(EntityResolver.class);

        // When: Setting entity resolver
        scanner.setEntityResolver(entityResolver);

        // Then: Should be able to get it back
        assertSame(entityResolver, scanner.getEntityResolver());
    }

    @Test
    public void testSetAndGetErrorHandler() {
        // Given: An error handler
        final ErrorHandler errorHandler = mock(ErrorHandler.class);

        // When: Setting error handler
        scanner.setErrorHandler(errorHandler);

        // Then: Should be able to get it back
        assertSame(errorHandler, scanner.getErrorHandler());
    }

    @Test
    public void testSetAndGetLexicalHandler() {
        // When: Setting lexical handler
        scanner.setLexicalHandler(lexicalHandler);

        // Then: Should be able to get it back
        assertSame(lexicalHandler, scanner.getLexicalHandler());
    }

    @Test
    public void testParseSimpleHTML() throws Exception {
        // Given: Simple HTML content
        final String html = "<html><body>Hello World</body></html>";
        scanner.setContentHandler(contentHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing the HTML
        scanner.parse(input);

        // Then: Should emit proper SAX events
        verify(contentHandler).startDocument();
        verify(contentHandler).startElement(eq(""), eq("HTML"), eq("HTML"), any(Attributes.class));
        verify(contentHandler).startElement(eq(""), eq("BODY"), eq("BODY"), any(Attributes.class));
        verify(contentHandler, atLeastOnce()).characters(any(char[].class), anyInt(), anyInt());
        verify(contentHandler).endElement(eq(""), eq("BODY"), eq("BODY"));
        verify(contentHandler).endElement(eq(""), eq("HTML"), eq("HTML"));
        verify(contentHandler).endDocument();
    }

    @Test
    public void testParseWithAttributes() throws Exception {
        // Given: HTML with attributes
        final String html = "<div id=\"test\" class=\"container\">Content</div>";
        scanner.setContentHandler(contentHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing the HTML
        scanner.parse(input);

        // Then: Should capture attributes correctly
        verify(contentHandler).startElement(eq(""), eq("DIV"), eq("DIV"), argThat(attrs -> {
            return attrs.getLength() == 2 && "test".equals(attrs.getValue("id")) && "container".equals(attrs.getValue("class"));
        }));
    }

    @Test
    public void testParseVoidElements() throws Exception {
        // Given: HTML with void elements
        final String html = "<div><br><hr><img src=\"test.png\"><meta charset=\"UTF-8\"></div>";
        scanner.setContentHandler(contentHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing the HTML
        scanner.parse(input);

        // Then: Void elements should have both start and end events
        verify(contentHandler).startElement(eq(""), eq("BR"), eq("BR"), any(Attributes.class));
        verify(contentHandler).endElement(eq(""), eq("BR"), eq("BR"));
        verify(contentHandler).startElement(eq(""), eq("HR"), eq("HR"), any(Attributes.class));
        verify(contentHandler).endElement(eq(""), eq("HR"), eq("HR"));
        verify(contentHandler).startElement(eq(""), eq("IMG"), eq("IMG"), any(Attributes.class));
        verify(contentHandler).endElement(eq(""), eq("IMG"), eq("IMG"));
        verify(contentHandler).startElement(eq(""), eq("META"), eq("META"), any(Attributes.class));
        verify(contentHandler).endElement(eq(""), eq("META"), eq("META"));
    }

    @Test
    public void testParseComment() throws Exception {
        // Given: HTML with comment
        final String html = "<html><!-- This is a comment --><body>Content</body></html>";
        scanner.setContentHandler(contentHandler);
        scanner.setLexicalHandler(lexicalHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing the HTML
        scanner.parse(input);

        // Then: Should emit comment event
        verify(lexicalHandler).comment(argThat(ch -> {
            final String comment = new String(ch).trim();
            return comment.contains("This is a comment");
        }), anyInt(), anyInt());
    }

    @Test
    public void testParseDoctype() throws Exception {
        // Given: HTML with DOCTYPE
        final String html = "<!DOCTYPE html><html><body>Content</body></html>";
        scanner.setContentHandler(contentHandler);
        scanner.setLexicalHandler(lexicalHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing the HTML
        scanner.parse(input);

        // Then: Should emit DTD events
        verify(lexicalHandler).startDTD(eq("html"), isNull(), isNull());
        verify(lexicalHandler).endDTD();
    }

    @Test
    public void testParseNestedElements() throws Exception {
        // Given: Nested HTML elements
        final String html = "<html><head><title>Test</title></head><body><div><p>Text</p></div></body></html>";
        scanner.setContentHandler(contentHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing the HTML
        scanner.parse(input);

        // Then: Should handle nested structure correctly
        verify(contentHandler).startElement(eq(""), eq("HTML"), eq("HTML"), any(Attributes.class));
        verify(contentHandler).startElement(eq(""), eq("HEAD"), eq("HEAD"), any(Attributes.class));
        verify(contentHandler).startElement(eq(""), eq("TITLE"), eq("TITLE"), any(Attributes.class));
        verify(contentHandler).endElement(eq(""), eq("TITLE"), eq("TITLE"));
        verify(contentHandler).endElement(eq(""), eq("HEAD"), eq("HEAD"));
        verify(contentHandler).startElement(eq(""), eq("BODY"), eq("BODY"), any(Attributes.class));
        verify(contentHandler).startElement(eq(""), eq("DIV"), eq("DIV"), any(Attributes.class));
        verify(contentHandler).startElement(eq(""), eq("P"), eq("P"), any(Attributes.class));
        verify(contentHandler).endElement(eq(""), eq("P"), eq("P"));
        verify(contentHandler).endElement(eq(""), eq("DIV"), eq("DIV"));
        verify(contentHandler).endElement(eq(""), eq("BODY"), eq("BODY"));
        verify(contentHandler).endElement(eq(""), eq("HTML"), eq("HTML"));
    }

    @Test
    public void testParseWithByteStream() throws Exception {
        // Given: HTML as byte stream
        final String html = "<html><body>Test</body></html>";
        final ByteArrayInputStream stream = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
        scanner.setContentHandler(contentHandler);
        final InputSource input = new InputSource(stream);
        input.setEncoding("UTF-8");

        // When: Parsing the HTML
        scanner.parse(input);

        // Then: Should parse successfully
        verify(contentHandler).startDocument();
        verify(contentHandler).endDocument();
    }

    @Test
    public void testParseWithNoContentHandler() throws Exception {
        // Given: Scanner without content handler
        final String html = "<html><body>Test</body></html>";
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing the HTML
        scanner.parse(input);

        // Then: Should not throw exception, just return early
        // No assertions needed - just verify no exception thrown
    }

    @Test
    public void testParseTextContent() throws Exception {
        // Given: HTML with text content
        final String html = "<p>Hello World</p>";
        final List<String> textContent = new ArrayList<>();
        scanner.setContentHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) {
                textContent.add(new String(ch, start, length));
            }
        });
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing the HTML
        scanner.parse(input);

        // Then: Should capture text content
        assertTrue(textContent.stream().anyMatch(text -> text.contains("Hello World")));
    }

    @Test
    public void testParseWhitespacePreservation() throws Exception {
        // Given: HTML with whitespace between elements
        final String html = "<div>Text1</div> <div>Text2</div>";
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

        // When: Parsing the HTML
        scanner.parse(input);

        // Then: Should preserve whitespace between elements
        assertTrue(textContent.size() >= 3, "Should have at least 3 text nodes (Text1, whitespace, Text2)");
        assertTrue(textContent.stream().anyMatch(text -> text.trim().isEmpty()), "Should contain whitespace text node");
    }

    @Test
    public void testParseAttributesWithDifferentQuotes() throws Exception {
        // Given: Attributes with different quote styles (no hyphens in attribute names due to regex limitation)
        final String html = "<div id=\"double\" class='single' datavalue=unquoted></div>";
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

        // When: Parsing the HTML
        scanner.parse(input);

        // Then: Should parse all attribute types correctly
        assertEquals(1, capturedAttrs.size(), "Should have one DIV element");
        final Attributes attrs = capturedAttrs.get(0);
        assertEquals("double", attrs.getValue("id"), "Double-quoted attribute should be parsed");
        assertEquals("single", attrs.getValue("class"), "Single-quoted attribute should be parsed");
        assertEquals("unquoted", attrs.getValue("datavalue"), "Unquoted attribute should be parsed");
    }

    @Test
    public void testParseBooleanAttribute() throws Exception {
        // Given: HTML with boolean attribute (no value)
        final String html = "<input type=\"checkbox\" checked>";
        scanner.setContentHandler(contentHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing the HTML
        scanner.parse(input);

        // Then: Boolean attribute should have empty string value
        verify(contentHandler).startElement(eq(""), eq("INPUT"), eq("INPUT"), argThat(attrs -> {
            return "checkbox".equals(attrs.getValue("type")) && "".equals(attrs.getValue("checked"));
        }));
    }

    @Test
    public void testParseMultilineComment() throws Exception {
        // Given: Multi-line comment
        final String html = "<html><!-- Line 1\nLine 2\nLine 3 --><body></body></html>";
        scanner.setContentHandler(contentHandler);
        scanner.setLexicalHandler(lexicalHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing the HTML
        scanner.parse(input);

        // Then: Should handle multi-line comment
        verify(lexicalHandler).comment(any(char[].class), anyInt(), anyInt());
    }

    @Test
    public void testParseEmptyElement() throws Exception {
        // Given: Empty element
        final String html = "<div></div>";
        scanner.setContentHandler(contentHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing the HTML
        scanner.parse(input);

        // Then: Should emit start and end events
        verify(contentHandler).startElement(eq(""), eq("DIV"), eq("DIV"), any(Attributes.class));
        verify(contentHandler).endElement(eq(""), eq("DIV"), eq("DIV"));
    }

    @Test
    public void testParseAllVoidElements() throws Exception {
        // Given: All HTML5 void elements
        final String html = "<area><base><br><col><embed><hr><img><input><link><meta><param><source><track><wbr>";
        scanner.setContentHandler(contentHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing the HTML
        scanner.parse(input);

        // Then: All void elements should have end events
        final String[] voidElements =
                { "AREA", "BASE", "BR", "COL", "EMBED", "HR", "IMG", "INPUT", "LINK", "META", "PARAM", "SOURCE", "TRACK", "WBR" };
        for (final String element : voidElements) {
            verify(contentHandler).startElement(eq(""), eq(element), eq(element), any(Attributes.class));
            verify(contentHandler).endElement(eq(""), eq(element), eq(element));
        }
    }

    @Test
    public void testParseSpecialCharactersInText() throws Exception {
        // Given: Text with special characters
        final String html = "<p>&lt;&gt;&amp;</p>";
        final List<String> textContent = new ArrayList<>();
        scanner.setContentHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) {
                textContent.add(new String(ch, start, length));
            }
        });
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing the HTML
        scanner.parse(input);

        // Then: Should capture decoded special characters (join chunks since characters() may be called multiple times)
        final String allText = String.join("", textContent);
        assertTrue(allText.contains("<>&"), "Should decode &lt;&gt;&amp; to <>&");
    }

    @Test
    public void testParseUnicodeContent() throws Exception {
        // Given: HTML with Unicode characters
        final String html = "<p>日本語テスト</p>";
        final List<String> textContent = new ArrayList<>();
        scanner.setContentHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) {
                textContent.add(new String(ch, start, length));
            }
        });
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing the HTML
        scanner.parse(input);

        // Then: Should handle Unicode correctly
        assertTrue(textContent.stream().anyMatch(text -> text.contains("日本語テスト")));
    }

    @Test
    public void testGetPropertyLexicalHandler() throws Exception {
        // Given: Scanner with lexical handler set
        scanner.setLexicalHandler(lexicalHandler);

        // When: Getting lexical handler property
        final Object property = scanner.getProperty("http://xml.org/sax/properties/lexical-handler");

        // Then: Should return the lexical handler
        assertSame(lexicalHandler, property);
    }

    @Test
    public void testGetPropertyUnrecognized() {
        // When: Getting unrecognized property
        // Then: Should throw SAXNotRecognizedException
        assertThrows(SAXNotRecognizedException.class, () -> {
            scanner.getProperty("unrecognized-property");
        });
    }

    @Test
    public void testSetPropertyLexicalHandler() throws Exception {
        // When: Setting lexical handler via property
        scanner.setProperty("http://xml.org/sax/properties/lexical-handler", lexicalHandler);

        // Then: Should be set correctly
        assertSame(lexicalHandler, scanner.getLexicalHandler());
    }

    @Test
    public void testSetPropertyUnrecognized() {
        // When: Setting unrecognized property
        // Then: Should not throw exception (properties not yet implemented)
        assertDoesNotThrow(() -> {
            scanner.setProperty("unrecognized-property", "value");
        });
    }

    @Test
    public void testGetFeatureThrowsException() {
        // When: Getting any feature
        // Then: Should throw SAXNotRecognizedException
        assertThrows(SAXNotRecognizedException.class, () -> {
            scanner.getFeature("any-feature");
        });
    }

    @Test
    public void testSetFeatureDoesNotThrow() {
        // When: Setting any feature
        // Then: Should not throw exception (features not yet implemented)
        assertDoesNotThrow(() -> {
            scanner.setFeature("any-feature", true);
        });
    }

    @Test
    public void testParseSystemIdNotSupported() {
        // Given: InputSource with only systemId that cannot be opened
        scanner.setContentHandler(contentHandler);
        final InputSource input = new InputSource();
        input.setSystemId("http://example.com/nonexistent.html");

        // When/Then: Should throw SAXException when SystemId cannot be opened
        final SAXException exception = assertThrows(SAXException.class, () -> {
            scanner.parse(input);
        });
        assertTrue(exception.getMessage().contains("Cannot open SystemId"), "Expected message about unable to open SystemId, got: "
                + exception.getMessage());
    }

    @Test
    public void testParseNoInputSource() {
        // Given: InputSource with no content
        scanner.setContentHandler(contentHandler);
        final InputSource input = new InputSource();

        // When/Then: Should throw SAXException for no input
        final SAXException exception = assertThrows(SAXException.class, () -> {
            scanner.parse(input);
        });
        assertTrue(exception.getMessage().contains("No input source available"));
    }

    @Test
    public void testParseStringSystemId() throws Exception {
        // Given: System ID string that cannot be opened
        scanner.setContentHandler(contentHandler);

        // When/Then: Should throw SAXException when SystemId cannot be opened
        final SAXException exception = assertThrows(SAXException.class, () -> {
            scanner.parse("http://example.com/nonexistent.html");
        });
        assertTrue(exception.getMessage().contains("Cannot open SystemId"), "Expected message about unable to open SystemId, got: "
                + exception.getMessage());
    }

    @Test
    public void testParseComplexHTML() throws Exception {
        // Given: Complex HTML structure
        final String html =
                "<!DOCTYPE html>\n" + "<html>\n" + "  <head>\n" + "    <meta charset=\"UTF-8\">\n" + "    <title>Test Page</title>\n"
                        + "  </head>\n" + "  <body>\n" + "    <!-- Main content -->\n" + "    <div id=\"main\">\n"
                        + "      <h1>Title</h1>\n" + "      <p>Paragraph 1</p>\n" + "      <p>Paragraph 2</p>\n" + "      <br>\n"
                        + "      <img src=\"test.png\" alt=\"Test\">\n" + "    </div>\n" + "  </body>\n" + "</html>";

        final List<String> elements = new ArrayList<>();
        scanner.setContentHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
                elements.add(qName);
            }
        });
        scanner.setLexicalHandler(lexicalHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing the HTML
        scanner.parse(input);

        // Then: Should parse complete structure
        assertTrue(elements.contains("HTML"));
        assertTrue(elements.contains("HEAD"));
        assertTrue(elements.contains("META"));
        assertTrue(elements.contains("TITLE"));
        assertTrue(elements.contains("BODY"));
        assertTrue(elements.contains("DIV"));
        assertTrue(elements.contains("H1"));
        assertTrue(elements.contains("P"));
        assertTrue(elements.contains("BR"));
        assertTrue(elements.contains("IMG"));
        verify(lexicalHandler).startDTD(eq("html"), isNull(), isNull());
        verify(lexicalHandler).comment(any(char[].class), anyInt(), anyInt());
    }

    @Test
    public void testParseEmptyHTML() throws Exception {
        // Given: Empty HTML string
        final String html = "";
        scanner.setContentHandler(contentHandler);
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing empty HTML
        scanner.parse(input);

        // Then: Should only emit document events
        verify(contentHandler).startDocument();
        verify(contentHandler).endDocument();
        verify(contentHandler, never()).startElement(anyString(), anyString(), anyString(), any(Attributes.class));
    }

    @Test
    public void testParseOnlyText() throws Exception {
        // Given: Only text content (no tags)
        final String html = "Just plain text";
        final List<String> textContent = new ArrayList<>();
        scanner.setContentHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) {
                textContent.add(new String(ch, start, length));
            }
        });
        final InputSource input = new InputSource(new StringReader(html));

        // When: Parsing plain text
        scanner.parse(input);

        // Then: Should emit text as characters
        assertTrue(textContent.stream().anyMatch(text -> text.contains("Just plain text")));
    }

} // class SimpleHTMLScannerTest
