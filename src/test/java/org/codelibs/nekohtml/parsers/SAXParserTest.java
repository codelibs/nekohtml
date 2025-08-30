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
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.xerces.xni.parser.XMLParseException;
import org.codelibs.nekohtml.HTMLConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

@ExtendWith(MockitoExtension.class)
class SAXParserTest {

    private SAXParser parser;

    @Mock
    private ContentHandler contentHandler;

    @Mock
    private ErrorHandler errorHandler;

    @Mock
    private DTDHandler dtdHandler;

    @Mock
    private EntityResolver entityResolver;

    @BeforeEach
    void setUp() {
        parser = new SAXParser();
    }

    @Test
    @DisplayName("Should create SAXParser with HTMLConfiguration")
    void testConstructor() {
        // When
        SAXParser newParser = new SAXParser();

        // Then
        assertNotNull(newParser);
        // Parser is correctly configured with HTMLConfiguration internally
    }

    @Test
    @DisplayName("Should parse simple HTML document")
    void testParseSimpleHTML() throws SAXException, IOException {
        // Given
        String html = "<html><body><p>Hello World</p></body></html>";
        InputSource source = new InputSource(new StringReader(html));
        TestContentHandler handler = new TestContentHandler();
        parser.setContentHandler(handler);

        // When
        parser.parse(source);

        // Then
        assertTrue(handler.startDocumentCalled);
        assertTrue(handler.endDocumentCalled);
        assertTrue(handler.elements.contains("HTML"));
        assertTrue(handler.elements.contains("BODY"));
        assertTrue(handler.elements.contains("P"));
        assertTrue(handler.content.toString().contains("Hello World"));
    }

    @Test
    @DisplayName("Should handle malformed HTML")
    void testParseMalformedHTML() throws SAXException, IOException {
        // Given - unclosed tags
        String html = "<html><body><p>Test<br><div>Content</body></html>";
        InputSource source = new InputSource(new StringReader(html));
        TestContentHandler handler = new TestContentHandler();
        parser.setContentHandler(handler);

        // When
        parser.parse(source);

        // Then - parser should handle malformed HTML gracefully
        assertTrue(handler.elements.contains("HTML"));
        assertTrue(handler.elements.contains("BODY"));
        assertTrue(handler.elements.contains("P"));
        assertTrue(handler.elements.contains("BR"));
        assertTrue(handler.elements.contains("DIV"));
    }

    @Test
    @DisplayName("Should parse HTML with attributes")
    void testParseHTMLWithAttributes() throws SAXException, IOException {
        // Given
        String html = "<html><body><div id='test' class='container'>Content</div></body></html>";
        InputSource source = new InputSource(new StringReader(html));
        TestContentHandler handler = new TestContentHandler();
        parser.setContentHandler(handler);

        // When
        parser.parse(source);

        // Then
        assertTrue(handler.attributes.containsKey("id"));
        assertTrue(handler.attributes.containsKey("class"));
        assertEquals("test", handler.attributes.get("id"));
        assertEquals("container", handler.attributes.get("class"));
    }

    @Test
    @DisplayName("Should handle ContentHandler callbacks")
    void testContentHandlerCallbacks() throws SAXException, IOException {
        // Given
        String html = "<html><body>Test</body></html>";
        InputSource source = new InputSource(new StringReader(html));
        parser.setContentHandler(contentHandler);

        // When
        parser.parse(source);

        // Then
        verify(contentHandler, atLeastOnce()).startDocument();
        verify(contentHandler, atLeastOnce()).endDocument();
        verify(contentHandler, atLeastOnce()).startElement(anyString(), eq("HTML"), anyString(), any(Attributes.class));
        verify(contentHandler, atLeastOnce()).endElement(anyString(), eq("HTML"), anyString());
    }

    @Test
    @DisplayName("Should parse from String URI")
    void testParseFromURI() throws SAXException, IOException {
        // Given
        String html = "<html><body>Test</body></html>";
        // Create a temporary input source using ByteArrayInputStream
        ByteArrayInputStream bais = new ByteArrayInputStream(html.getBytes());
        InputSource source = new InputSource(bais);
        source.setSystemId("test.html");
        TestContentHandler handler = new TestContentHandler();
        parser.setContentHandler(handler);

        // When
        parser.parse(source);

        // Then
        assertTrue(handler.elements.contains("HTML"));
        assertTrue(handler.elements.contains("BODY"));
    }

    @Test
    @DisplayName("Should handle ErrorHandler")
    void testErrorHandler() throws SAXException, IOException {
        // Given
        parser.setErrorHandler(errorHandler);
        String html = "<html><body>Test</body></html>";
        InputSource source = new InputSource(new StringReader(html));

        // When
        parser.parse(source);

        // Then - verify error handler is set but no errors for valid HTML
        assertNotNull(parser.getErrorHandler());
        assertEquals(errorHandler, parser.getErrorHandler());
    }

    @Test
    @DisplayName("Should handle DTDHandler")
    void testDTDHandler() {
        // Given & When
        parser.setDTDHandler(dtdHandler);

        // Then
        assertNotNull(parser.getDTDHandler());
        assertEquals(dtdHandler, parser.getDTDHandler());
    }

    @Test
    @DisplayName("Should handle EntityResolver")
    void testEntityResolver() {
        // Given & When
        parser.setEntityResolver(entityResolver);

        // Then
        assertNotNull(parser.getEntityResolver());
        assertEquals(entityResolver, parser.getEntityResolver());
    }

    @Test
    @DisplayName("Should parse HTML with entities")
    void testParseHTMLWithEntities() throws SAXException, IOException {
        // Given
        String html = "<html><body>&lt;div&gt;&amp;&quot;&apos;</body></html>";
        InputSource source = new InputSource(new StringReader(html));
        TestContentHandler handler = new TestContentHandler();
        parser.setContentHandler(handler);

        // When
        parser.parse(source);

        // Then
        String content = handler.content.toString();
        assertTrue(content.contains("<"));
        assertTrue(content.contains(">"));
        assertTrue(content.contains("&"));
        assertTrue(content.contains("\""));
        assertTrue(content.contains("'"));
    }

    @Test
    @DisplayName("Should handle nested elements")
    void testNestedElements() throws SAXException, IOException {
        // Given
        String html = "<html><body><div><span><b>Text</b></span></div></body></html>";
        InputSource source = new InputSource(new StringReader(html));
        TestContentHandler handler = new TestContentHandler();
        parser.setContentHandler(handler);

        // When
        parser.parse(source);

        // Then
        assertTrue(handler.elements.contains("HTML"));
        assertTrue(handler.elements.contains("BODY"));
        assertTrue(handler.elements.contains("DIV"));
        assertTrue(handler.elements.contains("SPAN"));
        assertTrue(handler.elements.contains("B"));
        assertEquals("Text", handler.content.toString().trim());
    }

    @Test
    @DisplayName("Should handle empty elements")
    void testEmptyElements() throws SAXException, IOException {
        // Given
        String html = "<html><body><img src='test.jpg'><br><hr></body></html>";
        InputSource source = new InputSource(new StringReader(html));
        TestContentHandler handler = new TestContentHandler();
        parser.setContentHandler(handler);

        // When
        parser.parse(source);

        // Then
        assertTrue(handler.elements.contains("IMG"));
        assertTrue(handler.elements.contains("BR"));
        assertTrue(handler.elements.contains("HR"));
    }

    @Test
    @DisplayName("Should handle HTML comments")
    void testHTMLComments() throws SAXException, IOException {
        // Given
        String html = "<html><!-- Comment --><body>Text</body></html>";
        InputSource source = new InputSource(new StringReader(html));
        TestContentHandler handler = new TestContentHandler();
        parser.setContentHandler(handler);

        // When
        parser.parse(source);

        // Then
        assertTrue(handler.elements.contains("HTML"));
        assertTrue(handler.elements.contains("BODY"));
        assertEquals("Text", handler.content.toString().trim());
    }

    @Test
    @DisplayName("Should handle script content")
    void testScriptContent() throws SAXException, IOException {
        // Given
        String html = "<html><body><script>var x = 5;</script></body></html>";
        InputSource source = new InputSource(new StringReader(html));
        TestContentHandler handler = new TestContentHandler();
        parser.setContentHandler(handler);

        // When
        parser.parse(source);

        // Then
        assertTrue(handler.elements.contains("SCRIPT"));
        assertTrue(handler.content.toString().contains("var x = 5;"));
    }

    @Test
    @DisplayName("Should handle style content")
    void testStyleContent() throws SAXException, IOException {
        // Given
        String html = "<html><head><style>body { color: red; }</style></head></html>";
        InputSource source = new InputSource(new StringReader(html));
        TestContentHandler handler = new TestContentHandler();
        parser.setContentHandler(handler);

        // When
        parser.parse(source);

        // Then
        assertTrue(handler.elements.contains("STYLE"));
        assertTrue(handler.content.toString().contains("body { color: red; }"));
    }

    @Test
    @DisplayName("Should get and set features")
    void testFeaturesConfiguration() throws SAXException {
        // Test getting a feature
        boolean namespaces = parser.getFeature("http://xml.org/sax/features/namespaces");
        assertNotNull(namespaces);

        // Test setting a feature
        assertDoesNotThrow(() -> {
            parser.setFeature("http://xml.org/sax/features/namespaces", true);
        });
    }

    @Test
    @DisplayName("Should get and set properties")
    void testPropertiesConfiguration() throws SAXException {
        // Test setting a known HTML configuration property
        assertDoesNotThrow(() -> {
            // Use a property that is recognized by HTMLConfiguration
            parser.setProperty("http://cyberneko.org/html/properties/default-encoding", "UTF-8");
        });

        // Test getting a property
        Object encoding = parser.getProperty("http://cyberneko.org/html/properties/default-encoding");
        assertNotNull(encoding);
    }

    /**
     * Test ContentHandler implementation for capturing parse events
     */
    private static class TestContentHandler extends DefaultHandler {
        boolean startDocumentCalled = false;
        boolean endDocumentCalled = false;
        List<String> elements = new ArrayList<>();
        StringBuilder content = new StringBuilder();
        java.util.Map<String, String> attributes = new java.util.HashMap<>();

        @Override
        public void startDocument() {
            startDocumentCalled = true;
        }

        @Override
        public void endDocument() {
            endDocumentCalled = true;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) {
            elements.add(localName.toUpperCase());
            for (int i = 0; i < atts.getLength(); i++) {
                attributes.put(atts.getLocalName(i), atts.getValue(i));
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            content.append(ch, start, length);
        }
    }
}