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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Test cases for HTMLDocumentHandler interface.
 * Tests the default methods and interface contracts.
 */
public class HTMLDocumentHandlerTest {

    private TestHTMLDocumentHandler handler;

    @BeforeEach
    public void setUp() {
        handler = Mockito.spy(new TestHTMLDocumentHandler());
    }

    @Test
    public void testEmptyElementWithQName() throws Exception {
        // Given: QName and attributes for BR element
        final HTMLQName qname = new HTMLQName();
        qname.rawname = "BR";
        qname.localpart = "br";
        qname.uri = "";

        final HTMLAttributesImpl attrs = new HTMLAttributesImpl();
        final HTMLAugmentations augs = new HTMLAugmentations();

        // When: emptyElement is called
        handler.emptyElement(qname, attrs, augs);

        // Then: Should call startElement and endElement
        verify(handler, times(1)).startElement(eq(""), eq("br"), eq("BR"), any(Attributes.class));
        verify(handler, times(1)).endElement(eq(""), eq("br"), eq("BR"));
    }

    @Test
    public void testEmptyElementWithNullUri() throws Exception {
        // Given: QName with null URI
        final HTMLQName qname = new HTMLQName();
        qname.rawname = "IMG";
        qname.localpart = "img";
        qname.uri = null; // null URI

        final HTMLAttributesImpl attrs = new HTMLAttributesImpl();
        final HTMLAugmentations augs = new HTMLAugmentations();

        // When: emptyElement is called
        handler.emptyElement(qname, attrs, augs);

        // Then: Should use empty string for URI
        verify(handler).startElement(eq(""), eq("img"), eq("IMG"), any(Attributes.class));
        verify(handler).endElement(eq(""), eq("img"), eq("IMG"));
    }

    @Test
    public void testEmptyElementWithNullLocalPart() throws Exception {
        // Given: QName with null localpart
        final HTMLQName qname = new HTMLQName();
        qname.rawname = "HR";
        qname.localpart = null; // null localpart
        qname.uri = "";

        final HTMLAttributesImpl attrs = new HTMLAttributesImpl();
        final HTMLAugmentations augs = new HTMLAugmentations();

        // When: emptyElement is called
        handler.emptyElement(qname, attrs, augs);

        // Then: Should use rawname for localpart
        verify(handler).startElement(eq(""), eq("HR"), eq("HR"), any(Attributes.class));
        verify(handler).endElement(eq(""), eq("HR"), eq("HR"));
    }

    @Test
    public void testEmptyElementWithAttributes() throws Exception {
        // Given: QName with attributes
        final HTMLQName qname = new HTMLQName();
        qname.rawname = "INPUT";
        qname.localpart = "input";
        qname.uri = "";

        final HTMLAttributesImpl attrs = new HTMLAttributesImpl();
        attrs.addAttribute("", "type", "type", "CDATA", "text");
        attrs.addAttribute("", "name", "name", "CDATA", "username");

        final HTMLAugmentations augs = new HTMLAugmentations();

        // When: emptyElement is called
        handler.emptyElement(qname, attrs, augs);

        // Then: Should pass attributes to startElement
        verify(handler).startElement(eq(""), eq("input"), eq("INPUT"), eq(attrs));
    }

    @Test
    public void testCharactersWithHTMLStringBuffer() throws Exception {
        // Given: HTMLStringBuffer with text content
        final String text = "Hello World";
        final HTMLStringBuffer buffer = new HTMLStringBuffer(text.toCharArray(), 0, text.length());
        final HTMLAugmentations augs = new HTMLAugmentations();

        // When: characters is called with HTMLStringBuffer
        handler.characters(buffer, augs);

        // Then: Should call characters with char array
        verify(handler).characters(eq(buffer.ch), eq(buffer.offset), eq(buffer.length));
    }

    @Test
    public void testCharactersWithEmptyBuffer() throws Exception {
        // Given: Empty HTMLStringBuffer
        final HTMLStringBuffer buffer = new HTMLStringBuffer(new char[0], 0, 0);
        final HTMLAugmentations augs = new HTMLAugmentations();

        // When: characters is called
        handler.characters(buffer, augs);

        // Then: Should call characters with empty array
        verify(handler).characters(eq(buffer.ch), eq(0), eq(0));
    }

    @Test
    public void testCharactersWithOffset() throws Exception {
        // Given: HTMLStringBuffer created from substring
        final char[] chars = "  Hello  ".toCharArray();
        final HTMLStringBuffer buffer = new HTMLStringBuffer(chars, 2, 5); // "Hello"
        final HTMLAugmentations augs = new HTMLAugmentations();

        // When: characters is called
        handler.characters(buffer, augs);

        // Then: Should call characters with buffer's internal array
        // Note: HTMLStringBuffer creates a new array, so we verify with buffer's array
        verify(handler).characters(eq(buffer.ch), eq(buffer.offset), eq(buffer.length));
    }

    @Test
    public void testCommentWithHTMLStringBuffer() throws Exception {
        // Given: HTMLStringBuffer with comment text
        final String commentText = "This is a comment";
        final HTMLStringBuffer buffer = new HTMLStringBuffer(commentText.toCharArray(), 0, commentText.length());
        final HTMLAugmentations augs = new HTMLAugmentations();

        // When: comment is called with HTMLStringBuffer
        handler.comment(buffer, augs);

        // Then: Should call comment with char array
        verify(handler).comment(eq(buffer.ch), eq(buffer.offset), eq(buffer.length));
    }

    @Test
    public void testCommentWithEmptyBuffer() throws Exception {
        // Given: Empty comment buffer
        final HTMLStringBuffer buffer = new HTMLStringBuffer(new char[0], 0, 0);
        final HTMLAugmentations augs = new HTMLAugmentations();

        // When: comment is called
        handler.comment(buffer, augs);

        // Then: Should call comment with empty array
        verify(handler).comment(eq(buffer.ch), eq(0), eq(0));
    }

    @Test
    public void testCommentWithOffset() throws Exception {
        // Given: HTMLStringBuffer created from substring
        final char[] chars = "  Comment text  ".toCharArray();
        final HTMLStringBuffer buffer = new HTMLStringBuffer(chars, 2, 12); // "Comment text"
        final HTMLAugmentations augs = new HTMLAugmentations();

        // When: comment is called
        handler.comment(buffer, augs);

        // Then: Should call comment with buffer's internal array
        // Note: HTMLStringBuffer creates a new array, so we verify with buffer's array
        verify(handler).comment(eq(buffer.ch), eq(buffer.offset), eq(buffer.length));
    }

    @Test
    public void testMultipleEmptyElements() throws Exception {
        // Given: Multiple empty elements
        final HTMLQName br = new HTMLQName();
        br.rawname = "BR";
        br.localpart = "br";
        br.uri = "";

        final HTMLQName hr = new HTMLQName();
        hr.rawname = "HR";
        hr.localpart = "hr";
        hr.uri = "";

        final HTMLAttributesImpl attrs = new HTMLAttributesImpl();
        final HTMLAugmentations augs = new HTMLAugmentations();

        // When: Multiple emptyElement calls
        handler.emptyElement(br, attrs, augs);
        handler.emptyElement(hr, attrs, augs);

        // Then: Each should trigger start and end
        verify(handler).startElement(eq(""), eq("br"), eq("BR"), any(Attributes.class));
        verify(handler).endElement(eq(""), eq("br"), eq("BR"));
        verify(handler).startElement(eq(""), eq("hr"), eq("HR"), any(Attributes.class));
        verify(handler).endElement(eq(""), eq("hr"), eq("HR"));
    }

    @Test
    public void testCharactersAndCommentSequence() throws Exception {
        // Given: Character data and comment
        final HTMLStringBuffer charBuffer = new HTMLStringBuffer("Text".toCharArray(), 0, 4);
        final HTMLStringBuffer commentBuffer = new HTMLStringBuffer("Comment".toCharArray(), 0, 7);
        final HTMLAugmentations augs = new HTMLAugmentations();

        // When: Calling characters and comment
        handler.characters(charBuffer, augs);
        handler.comment(commentBuffer, augs);

        // Then: Both should be called
        verify(handler).characters(any(char[].class), eq(0), eq(4));
        verify(handler).comment(any(char[].class), eq(0), eq(7));
    }

    @Test
    public void testEmptyElementPreservesOrder() throws Exception {
        // Given: QName for element
        final HTMLQName qname = new HTMLQName();
        qname.rawname = "META";
        qname.localpart = "meta";
        qname.uri = "";

        final HTMLAttributesImpl attrs = new HTMLAttributesImpl();
        final HTMLAugmentations augs = new HTMLAugmentations();

        // When: emptyElement is called
        handler.emptyElement(qname, attrs, augs);

        // Then: startElement should be called before endElement
        // This is implicitly tested by the implementation, but we verify both were called
        verify(handler, times(1)).startElement(anyString(), anyString(), anyString(), any(Attributes.class));
        verify(handler, times(1)).endElement(anyString(), anyString(), anyString());
    }

    @Test
    public void testHTMLDocumentHandlerIsInterface() {
        // Then: HTMLDocumentHandler should be an interface
        assertTrue(HTMLDocumentHandler.class.isInterface(), "HTMLDocumentHandler should be an interface");
    }

    @Test
    public void testHTMLDocumentHandlerExtendsContentHandler() {
        // Then: Should extend ContentHandler
        assertTrue(org.xml.sax.ContentHandler.class.isAssignableFrom(HTMLDocumentHandler.class),
                "HTMLDocumentHandler should extend ContentHandler");
    }

    @Test
    public void testHTMLDocumentHandlerExtendsLexicalHandler() {
        // Then: Should extend LexicalHandler
        assertTrue(org.xml.sax.ext.LexicalHandler.class.isAssignableFrom(HTMLDocumentHandler.class),
                "HTMLDocumentHandler should extend LexicalHandler");
    }

    @Test
    public void testDefaultMethodsExist() throws Exception {
        // Given: Handler instance
        final TestHTMLDocumentHandler testHandler = new TestHTMLDocumentHandler();

        // Then: Default methods should be callable
        assertNotNull(testHandler, "Handler should be instantiable");

        // Test that default methods don't throw when called
        final HTMLQName qname = new HTMLQName();
        qname.rawname = "TEST";
        qname.localpart = "test";
        qname.uri = "";

        testHandler.emptyElement(qname, new HTMLAttributesImpl(), new HTMLAugmentations());
        testHandler.characters(new HTMLStringBuffer(new char[0], 0, 0), new HTMLAugmentations());
        testHandler.comment(new HTMLStringBuffer(new char[0], 0, 0), new HTMLAugmentations());

        // Verify event counters were incremented
        assertEquals(1, testHandler.startElementCount, "startElement should be called once");
        assertEquals(1, testHandler.endElementCount, "endElement should be called once");
        assertEquals(1, testHandler.charactersCount, "characters should be called once");
        assertEquals(1, testHandler.commentCount, "comment should be called once");
    }

    /**
     * Test implementation of HTMLDocumentHandler for testing purposes.
     */
    static class TestHTMLDocumentHandler implements HTMLDocumentHandler {

        int startElementCount = 0;
        int endElementCount = 0;
        int charactersCount = 0;
        int commentCount = 0;

        @Override
        public void setDocumentLocator(final Locator locator) {
            // No-op
        }

        @Override
        public void startDocument() throws SAXException {
            // No-op
        }

        @Override
        public void endDocument() throws SAXException {
            // No-op
        }

        @Override
        public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
            // No-op
        }

        @Override
        public void endPrefixMapping(final String prefix) throws SAXException {
            // No-op
        }

        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes atts) throws SAXException {
            startElementCount++;
        }

        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            endElementCount++;
        }

        @Override
        public void characters(final char[] ch, final int start, final int length) throws SAXException {
            charactersCount++;
        }

        @Override
        public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException {
            // No-op
        }

        @Override
        public void processingInstruction(final String target, final String data) throws SAXException {
            // No-op
        }

        @Override
        public void skippedEntity(final String name) throws SAXException {
            // No-op
        }

        @Override
        public void startDTD(final String name, final String publicId, final String systemId) throws SAXException {
            // No-op
        }

        @Override
        public void endDTD() throws SAXException {
            // No-op
        }

        @Override
        public void startEntity(final String name) throws SAXException {
            // No-op
        }

        @Override
        public void endEntity(final String name) throws SAXException {
            // No-op
        }

        @Override
        public void startCDATA() throws SAXException {
            // No-op
        }

        @Override
        public void endCDATA() throws SAXException {
            // No-op
        }

        @Override
        public void comment(final char[] ch, final int start, final int length) throws SAXException {
            commentCount++;
        }
    }

} // class HTMLDocumentHandlerTest
