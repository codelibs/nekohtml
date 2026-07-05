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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Test class for {@link HTMLTagBalancerFilter}.
 *
 * @author CodeLibs Project
 */
public class HTMLTagBalancerFilterTest {

    private HTMLTagBalancerFilter filter;
    private ContentHandler contentHandler;
    private LexicalHandler lexicalHandler;

    @BeforeEach
    public void setUp() {
        filter = new HTMLTagBalancerFilter();
        contentHandler = mock(ContentHandler.class);
        lexicalHandler = mock(LexicalHandler.class);
        filter.setContentHandler(contentHandler);
        filter.setLexicalHandler(lexicalHandler);
    }

    @Test
    public void testDefaultConstructor() {
        // When: Creating with default constructor
        final HTMLTagBalancerFilter f = new HTMLTagBalancerFilter();

        // Then: Should be initialized
        assertNotNull(f, "Filter should be created");
    }

    @Test
    public void testConstructorWithParent() {
        // Given: Parent XMLReader
        final XMLReader parent = mock(XMLReader.class);

        // When: Creating with parent
        final HTMLTagBalancerFilter f = new HTMLTagBalancerFilter(parent);

        // Then: Should be initialized with parent
        assertNotNull(f, "Filter should be created");
        assertSame(parent, f.getParent(), "Parent should be set");
    }

    @Test
    public void testSetContentHandler() {
        // Given: New content handler
        final ContentHandler handler = mock(ContentHandler.class);

        // When: Setting content handler
        filter.setContentHandler(handler);

        // Then: Should be set
        assertSame(handler, filter.getContentHandler(), "Content handler should be set");
    }

    @Test
    public void testSetLexicalHandler() {
        // Given: New lexical handler
        final LexicalHandler handler = mock(LexicalHandler.class);

        // When: Setting lexical handler
        filter.setLexicalHandler(handler);

        // Then: Should be set (can't directly verify, but test delegation)
        assertDoesNotThrow(() -> filter.startDTD("html", null, null));
    }

    @Test
    public void testSetDocumentLocator() throws Exception {
        // Given: Locator
        final Locator locator = mock(Locator.class);

        // When: Setting document locator
        filter.setDocumentLocator(locator);

        // Then: Should delegate to content handler
        verify(contentHandler).setDocumentLocator(locator);
    }

    @Test
    public void testSetDocumentLocatorWithoutHandler() {
        // Given: Filter without content handler
        filter.setContentHandler(null);
        final Locator locator = mock(Locator.class);

        // When: Setting document locator
        // Then: Should not throw exception
        assertDoesNotThrow(() -> filter.setDocumentLocator(locator));
    }

    @Test
    public void testStartDocument() throws Exception {
        // When: Starting document
        filter.startDocument();

        // Then: Should delegate and clear element stack
        verify(contentHandler).startDocument();
    }

    @Test
    public void testStartDocumentClearsStack() throws Exception {
        // Given: Elements on stack from previous document
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "body", "BODY", new AttributesImpl());

        // When: Starting new document
        reset(contentHandler);
        filter.startDocument();

        // Then: Stack should be cleared
        filter.endDocument();
        verify(contentHandler).startDocument();
        verify(contentHandler).endDocument();
        // No elements should be auto-closed from previous document
    }

    @Test
    public void testEndDocument() throws Exception {
        // When: Ending document
        filter.endDocument();

        // Then: Should delegate
        verify(contentHandler).endDocument();
    }

    @Test
    public void testEndDocumentClosesOpenElements() throws Exception {
        // Given: Open elements
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "body", "BODY", new AttributesImpl());

        // When: Ending document
        reset(contentHandler);
        filter.endDocument();

        // Then: Should close open elements
        final InOrder inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).endElement("", "body", "BODY");
        inOrder.verify(contentHandler).endElement("", "html", "HTML");
        inOrder.verify(contentHandler).endDocument();
    }

    @Test
    public void testStartPrefixMapping() throws Exception {
        // When: Starting prefix mapping
        filter.startPrefixMapping("ns", "http://example.com");

        // Then: Should delegate
        verify(contentHandler).startPrefixMapping("ns", "http://example.com");
    }

    @Test
    public void testEndPrefixMapping() throws Exception {
        // When: Ending prefix mapping
        filter.endPrefixMapping("ns");

        // Then: Should delegate
        verify(contentHandler).endPrefixMapping("ns");
    }

    @Test
    public void testStartElementNonVoid() throws Exception {
        // When: Starting non-void element
        final AttributesImpl attrs = new AttributesImpl();
        filter.startElement("", "div", "DIV", attrs);

        // Then: Should delegate and track element
        verify(contentHandler).startElement("", "div", "DIV", attrs);
    }

    @Test
    public void testStartElementVoid() throws Exception {
        // When: Starting void element (BR)
        final AttributesImpl attrs = new AttributesImpl();
        filter.startElement("", "br", "BR", attrs);

        // Then: Should delegate but not track element
        verify(contentHandler).startElement("", "br", "BR", attrs);
    }

    @Test
    public void testStartElementBodyClosesHead() throws Exception {
        // Given: HEAD is open
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "head", "HEAD", new AttributesImpl());
        filter.startElement("", "title", "TITLE", new AttributesImpl());

        reset(contentHandler);

        // When: Starting BODY
        final AttributesImpl bodyAttrs = new AttributesImpl();
        filter.startElement("", "body", "BODY", bodyAttrs);

        // Then: Should close TITLE and HEAD first, then start BODY
        final InOrder inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).endElement("", "title", "TITLE");
        inOrder.verify(contentHandler).endElement("", "head", "HEAD");
        inOrder.verify(contentHandler).startElement("", "body", "BODY", bodyAttrs);
    }

    @Test
    public void testStartElementFramesetClosesHead() throws Exception {
        // Given: HEAD is open
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "head", "HEAD", new AttributesImpl());

        reset(contentHandler);

        // When: Starting FRAMESET
        final AttributesImpl framesetAttrs = new AttributesImpl();
        filter.startElement("", "frameset", "FRAMESET", framesetAttrs);

        // Then: Should close HEAD first, then start FRAMESET
        final InOrder inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).endElement("", "head", "HEAD");
        inOrder.verify(contentHandler).startElement("", "frameset", "FRAMESET", framesetAttrs);
    }

    @Test
    public void testStartElementWithoutHandler() {
        // Given: Filter without content handler
        filter.setContentHandler(null);

        // When: Starting element
        // Then: Should not throw exception
        assertDoesNotThrow(() -> filter.startElement("", "div", "DIV", new AttributesImpl()));
    }

    @Test
    public void testEndElementMatching() throws Exception {
        // Given: Matching element on stack
        filter.startElement("", "div", "DIV", new AttributesImpl());

        reset(contentHandler);

        // When: Ending matching element
        filter.endElement("", "div", "DIV");

        // Then: Should delegate
        verify(contentHandler).endElement("", "div", "DIV");
    }

    @Test
    public void testEndElementAutoClosesNested() throws Exception {
        // Given: Nested elements
        filter.startElement("", "div", "DIV", new AttributesImpl());
        filter.startElement("", "p", "P", new AttributesImpl());
        filter.startElement("", "span", "SPAN", new AttributesImpl());

        reset(contentHandler);

        // When: Ending DIV (closing over SPAN and P)
        filter.endElement("", "div", "DIV");

        // Then: Should auto-close SPAN and P first (auto-close preserves the original localName)
        final InOrder inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).endElement("", "span", "SPAN");
        inOrder.verify(contentHandler).endElement("", "p", "P");
        inOrder.verify(contentHandler).endElement("", "div", "DIV");
    }

    @Test
    public void testEndElementNotOnStack() throws Exception {
        // Given: Element not on stack
        filter.startElement("", "div", "DIV", new AttributesImpl());

        reset(contentHandler);

        // When: Ending element not on stack
        filter.endElement("", "span", "SPAN");

        // Then: Stray end tag should be ignored (HTML5 spec: never pass through unbalanced ends)
        verify(contentHandler, never()).endElement(eq(""), eq("span"), eq("SPAN"));
    }

    @Test
    public void testEndElementEmptyStack() throws Exception {
        // Given: Empty stack
        // When: Ending element
        filter.endElement("", "div", "DIV");

        // Then: Stray end tag should be ignored (nothing is open)
        verify(contentHandler, never()).endElement(anyString(), anyString(), anyString());
    }

    @Test
    public void testEndElementVoidElement() throws Exception {
        // Given: Void element (BR) started but not tracked
        filter.startElement("", "br", "BR", new AttributesImpl());

        reset(contentHandler);

        // When: Ending void element (shouldn't be on stack)
        filter.endElement("", "br", "BR");

        // Then: The end tag for a void element is ignored (never on the stack)
        verify(contentHandler, never()).endElement(eq(""), eq("br"), eq("BR"));
    }

    @Test
    public void testEndElementWithoutHandler() {
        // Given: Filter without content handler
        filter.setContentHandler(null);

        // When: Ending element
        // Then: Should not throw exception
        assertDoesNotThrow(() -> filter.endElement("", "div", "DIV"));
    }

    @Test
    public void testCharacters() throws Exception {
        // When: Receiving characters
        final char[] ch = "Hello World".toCharArray();
        filter.characters(ch, 0, ch.length);

        // Then: Should delegate
        verify(contentHandler).characters(ch, 0, ch.length);
    }

    @Test
    public void testIgnorableWhitespace() throws Exception {
        // When: Receiving ignorable whitespace
        final char[] ch = "   ".toCharArray();
        filter.ignorableWhitespace(ch, 0, ch.length);

        // Then: Should delegate
        verify(contentHandler).ignorableWhitespace(ch, 0, ch.length);
    }

    @Test
    public void testProcessingInstruction() throws Exception {
        // When: Receiving processing instruction
        filter.processingInstruction("xml-stylesheet", "type=\"text/css\"");

        // Then: Should delegate
        verify(contentHandler).processingInstruction("xml-stylesheet", "type=\"text/css\"");
    }

    @Test
    public void testSkippedEntity() throws Exception {
        // When: Receiving skipped entity
        filter.skippedEntity("entity");

        // Then: Should delegate
        verify(contentHandler).skippedEntity("entity");
    }

    @Test
    public void testStartDTD() throws Exception {
        // When: Starting DTD
        filter.startDTD("html", "-//W3C//DTD HTML 4.01//EN", "http://www.w3.org/TR/html4/strict.dtd");

        // Then: Should delegate to lexical handler
        verify(lexicalHandler).startDTD("html", "-//W3C//DTD HTML 4.01//EN", "http://www.w3.org/TR/html4/strict.dtd");
    }

    @Test
    public void testStartDTDWithoutHandler() {
        // Given: Filter without lexical handler
        filter.setLexicalHandler(null);

        // When: Starting DTD
        // Then: Should not throw exception
        assertDoesNotThrow(() -> filter.startDTD("html", null, null));
    }

    @Test
    public void testEndDTD() throws Exception {
        // When: Ending DTD
        filter.endDTD();

        // Then: Should delegate to lexical handler
        verify(lexicalHandler).endDTD();
    }

    @Test
    public void testStartEntity() throws Exception {
        // When: Starting entity
        filter.startEntity("entity");

        // Then: Should delegate to lexical handler
        verify(lexicalHandler).startEntity("entity");
    }

    @Test
    public void testEndEntity() throws Exception {
        // When: Ending entity
        filter.endEntity("entity");

        // Then: Should delegate to lexical handler
        verify(lexicalHandler).endEntity("entity");
    }

    @Test
    public void testStartCDATA() throws Exception {
        // When: Starting CDATA
        filter.startCDATA();

        // Then: Should delegate to lexical handler
        verify(lexicalHandler).startCDATA();
    }

    @Test
    public void testEndCDATA() throws Exception {
        // When: Ending CDATA
        filter.endCDATA();

        // Then: Should delegate to lexical handler
        verify(lexicalHandler).endCDATA();
    }

    @Test
    public void testComment() throws Exception {
        // When: Receiving comment
        final char[] ch = "This is a comment".toCharArray();
        filter.comment(ch, 0, ch.length);

        // Then: Should delegate to lexical handler
        verify(lexicalHandler).comment(ch, 0, ch.length);
    }

    @Test
    public void testComplexHTMLStructure() throws Exception {
        // Given: Complex HTML with tag balancing needed
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "head", "HEAD", new AttributesImpl());
        filter.startElement("", "meta", "META", new AttributesImpl()); // void element
        filter.startElement("", "title", "TITLE", new AttributesImpl());
        filter.characters("Test".toCharArray(), 0, 4);
        filter.endElement("", "title", "TITLE");
        // Not closing HEAD explicitly - BODY will auto-close it
        filter.startElement("", "body", "BODY", new AttributesImpl()); // Should auto-close HEAD
        filter.startElement("", "div", "DIV", new AttributesImpl());
        filter.characters("Content".toCharArray(), 0, 7);
        // Not closing DIV explicitly
        filter.endElement("", "body", "BODY"); // Should auto-close DIV
        filter.endElement("", "html", "HTML");
        filter.endDocument();

        // Then: Verify correct closing sequence
        final InOrder inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).startDocument();
        inOrder.verify(contentHandler).startElement(eq(""), eq("html"), eq("HTML"), any());
        inOrder.verify(contentHandler).startElement(eq(""), eq("head"), eq("HEAD"), any());
        inOrder.verify(contentHandler).startElement(eq(""), eq("meta"), eq("META"), any()); // void - no end
        inOrder.verify(contentHandler).startElement(eq(""), eq("title"), eq("TITLE"), any());
        inOrder.verify(contentHandler).characters(any(), anyInt(), anyInt());
        inOrder.verify(contentHandler).endElement("", "title", "TITLE"); // explicit close
        inOrder.verify(contentHandler).endElement("", "head", "HEAD"); // auto-closed by BODY (original localName preserved)
        inOrder.verify(contentHandler).startElement(eq(""), eq("body"), eq("BODY"), any());
        inOrder.verify(contentHandler).startElement(eq(""), eq("div"), eq("DIV"), any());
        inOrder.verify(contentHandler).characters(any(), anyInt(), anyInt());
        inOrder.verify(contentHandler).endElement("", "div", "DIV"); // auto-closed above BODY (original localName preserved)
        // BODY/HTML end tags defer their own close to end-of-document, so BODY is closed
        // (as an entry above HTML) when </html> arrives, and HTML is closed at endDocument.
        inOrder.verify(contentHandler).endElement("", "body", "BODY");
        inOrder.verify(contentHandler).endElement("", "html", "HTML");
        inOrder.verify(contentHandler).endDocument();
    }

    @Test
    public void testAllVoidElements() throws Exception {
        // When: Starting all void elements
        final String[] voidElements =
                { "AREA", "BASE", "BR", "COL", "EMBED", "HR", "IMG", "INPUT", "LINK", "META", "PARAM", "SOURCE", "TRACK", "WBR" };

        filter.startDocument();
        for (final String element : voidElements) {
            filter.startElement("", element.toLowerCase(), element, new AttributesImpl());
        }
        filter.endDocument();

        // Then: Void elements should not be on stack (no auto-close at end)
        verify(contentHandler, times(1)).startDocument();
        verify(contentHandler, times(1)).endDocument();
        for (final String element : voidElements) {
            verify(contentHandler).startElement(eq(""), eq(element.toLowerCase()), eq(element), any());
        }
    }

    @Test
    public void testHeadElementsRecognition() throws Exception {
        // Given: HEAD elements
        final String[] headElements = { "TITLE", "META", "LINK", "STYLE", "SCRIPT", "BASE" };

        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "head", "HEAD", new AttributesImpl());

        for (final String element : headElements) {
            filter.startElement("", element.toLowerCase(), element, new AttributesImpl());
        }

        // When: Starting BODY (should close HEAD and its children)
        reset(contentHandler);
        filter.startElement("", "body", "BODY", new AttributesImpl());

        // Then: Should have closed HEAD elements
        verify(contentHandler, atLeastOnce()).endElement(anyString(), anyString(), anyString());
        verify(contentHandler).startElement(eq(""), eq("body"), eq("BODY"), any());
    }

    @Test
    public void testCaseInsensitiveTagNames() throws Exception {
        // When: Using mixed case tag names
        filter.startElement("", "div", "div", new AttributesImpl()); // lowercase
        filter.startElement("", "p", "P", new AttributesImpl()); // uppercase

        reset(contentHandler);

        filter.endElement("", "p", "P");
        filter.endElement("", "div", "DIV"); // Different case

        // Then: Should handle case-insensitively
        final InOrder inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).endElement("", "p", "P");
        inOrder.verify(contentHandler).endElement("", "div", "DIV");
    }

    @Test
    public void testNestedIdenticalElements() throws Exception {
        // When: Nesting identical elements
        filter.startElement("", "div", "DIV", new AttributesImpl());
        filter.startElement("", "div", "DIV", new AttributesImpl());
        filter.startElement("", "div", "DIV", new AttributesImpl());

        reset(contentHandler);

        // Closing middle DIV first
        filter.endElement("", "div", "DIV");

        // Then: Should close innermost DIV (preserves original localName)
        verify(contentHandler, times(1)).endElement("", "div", "DIV");
    }

    @Test
    public void testMalformedHTMLRecovery() throws Exception {
        // Given: Malformed HTML - closing tags in wrong order
        filter.startElement("", "div", "DIV", new AttributesImpl());
        filter.startElement("", "p", "P", new AttributesImpl());
        filter.startElement("", "span", "SPAN", new AttributesImpl());

        reset(contentHandler);

        // When: Closing P (should auto-close SPAN first)
        filter.endElement("", "p", "P");

        // Then: Should auto-close SPAN before P
        final InOrder inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).endElement("", "span", "SPAN");
        inOrder.verify(contentHandler).endElement("", "p", "P");
    }

    @Test
    public void testEmptyDocument() throws Exception {
        // When: Processing empty document
        filter.startDocument();
        filter.endDocument();

        // Then: Should handle gracefully
        verify(contentHandler).startDocument();
        verify(contentHandler).endDocument();
    }

    @Test
    public void testDelegationWithNullHandlers() throws Exception {
        // Given: All handlers set to null
        filter.setContentHandler(null);
        filter.setLexicalHandler(null);

        // When: Calling various methods
        // Then: Should not throw exceptions
        assertDoesNotThrow(() -> {
            filter.startDocument();
            filter.startElement("", "div", "DIV", new AttributesImpl());
            filter.characters("text".toCharArray(), 0, 4);
            filter.endElement("", "div", "DIV");
            filter.endDocument();
            filter.startDTD("html", null, null);
            filter.comment("comment".toCharArray(), 0, 7);
        });
    }

    // =========================================================================
    // AAA (Adoption Agency Algorithm) Tests
    // =========================================================================

    @Test
    public void testAAABasicMisnesting() throws Exception {
        // Given: Basic misnested formatting elements <b><i>text</b></i>
        filter.startDocument();
        filter.startElement("", "p", "P", new AttributesImpl());
        filter.startElement("", "b", "B", new AttributesImpl());
        filter.startElement("", "i", "I", new AttributesImpl());
        filter.characters("text".toCharArray(), 0, 4);

        reset(contentHandler);

        // When: Closing B out of order (formatting reconstruction)
        filter.endElement("", "b", "B");

        // Then: Close I, close B, and reopen I so formatting continues past the misnested </b>
        // (Adoption-Agency-style reconstruction of the active formatting elements). The reopened
        // element keeps its original localName casing; the event stream stays balanced.
        final InOrder inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).endElement("", "i", "I");
        inOrder.verify(contentHandler).endElement("", "b", "B");
        inOrder.verify(contentHandler).startElement(eq(""), eq("i"), eq("I"), any());
    }

    @Test
    public void testAAAFormattingElementTracking() throws Exception {
        // Given: Starting multiple formatting elements
        filter.startDocument();
        filter.startElement("", "b", "B", new AttributesImpl());
        filter.startElement("", "i", "I", new AttributesImpl());
        filter.startElement("", "u", "U", new AttributesImpl());

        reset(contentHandler);

        // When: Properly closing all elements
        filter.endElement("", "u", "U");
        filter.endElement("", "i", "I");
        filter.endElement("", "b", "B");

        // Then: Should close in reverse order (each close preserves the original localName)
        final InOrder inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).endElement("", "u", "U");
        inOrder.verify(contentHandler).endElement("", "i", "I");
        inOrder.verify(contentHandler).endElement("", "b", "B");
    }

    @Test
    public void testAAAWithFurthestBlock() throws Exception {
        // Given: Formatting element crossing block boundary <b>text<p>para</b></p>
        filter.startDocument();
        filter.startElement("", "div", "DIV", new AttributesImpl());
        filter.startElement("", "b", "B", new AttributesImpl());
        filter.characters("text".toCharArray(), 0, 4);
        filter.startElement("", "p", "P", new AttributesImpl()); // P is a furthest block

        reset(contentHandler);

        // When: Closing B (which is before P in stack)
        filter.endElement("", "b", "B");

        // Then: AAA should handle the furthest block case
        verify(contentHandler, atLeastOnce()).endElement(anyString(), anyString(), anyString());
    }

    @Test
    public void testAAAComplexNesting() throws Exception {
        // Given: Complex nested formatting elements <b>text1<i>text2<u>text3</b>text4</i>text5</u>
        filter.startDocument();
        filter.startElement("", "p", "P", new AttributesImpl());
        filter.startElement("", "b", "B", new AttributesImpl());
        filter.characters("text1".toCharArray(), 0, 5);
        filter.startElement("", "i", "I", new AttributesImpl());
        filter.characters("text2".toCharArray(), 0, 5);
        filter.startElement("", "u", "U", new AttributesImpl());
        filter.characters("text3".toCharArray(), 0, 5);

        reset(contentHandler);

        // When: Closing B (should trigger AAA)
        filter.endElement("", "b", "B");

        // Then: AAA should restructure the elements
        verify(contentHandler, atLeastOnce()).endElement(anyString(), anyString(), anyString());
    }

    @Test
    public void testAAAWithNonFormattingElement() throws Exception {
        // Given: Non-formatting element
        filter.startDocument();
        filter.startElement("", "div", "DIV", new AttributesImpl());
        filter.characters("text".toCharArray(), 0, 4);

        reset(contentHandler);

        // When: Closing DIV (should not trigger AAA)
        filter.endElement("", "div", "DIV");

        // Then: Should close normally without AAA
        verify(contentHandler, times(1)).endElement("", "div", "DIV");
    }

    @Test
    public void testAAAFormattingElementNotInActiveList() throws Exception {
        // Given: Formatting element closed but not in active list
        filter.startDocument();
        filter.startElement("", "div", "DIV", new AttributesImpl());

        reset(contentHandler);

        // When: Closing B that was never opened
        filter.endElement("", "b", "B");

        // Then: A formatting end tag with no matching open element is ignored
        verify(contentHandler, never()).endElement(eq(""), eq("b"), eq("B"));
    }

    @Test
    public void testAAAStrongEmElements() throws Exception {
        // Given: Using strong and em elements <em><strong>text1</em>text2</strong>
        filter.startDocument();
        filter.startElement("", "p", "P", new AttributesImpl());
        filter.startElement("", "em", "EM", new AttributesImpl());
        filter.startElement("", "strong", "STRONG", new AttributesImpl());
        filter.characters("text1".toCharArray(), 0, 5);

        reset(contentHandler);

        // When: Closing EM out of order (formatting reconstruction)
        filter.endElement("", "em", "EM");

        // Then: Close STRONG, close EM, and reopen STRONG so formatting continues past the
        // misnested </em> (Adoption-Agency-style reconstruction). The event stream stays balanced.
        final InOrder inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).endElement("", "strong", "STRONG");
        inOrder.verify(contentHandler).endElement("", "em", "EM");
        inOrder.verify(contentHandler).startElement(eq(""), eq("strong"), eq("STRONG"), any());
    }

    @Test
    public void testAAAClearsOnNewDocument() throws Exception {
        // Given: Formatting elements in previous document
        filter.startDocument();
        filter.startElement("", "b", "B", new AttributesImpl());
        filter.startElement("", "i", "I", new AttributesImpl());
        filter.endDocument();

        // When: Starting new document
        filter.startDocument();
        filter.startElement("", "p", "P", new AttributesImpl());

        reset(contentHandler);

        // When: Closing I from new document (I from the previous document was cleared)
        filter.endElement("", "i", "I");

        // Then: I is not open in the new document, so the stray end tag is ignored
        verify(contentHandler, never()).endElement(eq(""), eq("i"), eq("I"));
    }

} // class HTMLTagBalancerFilterTest
