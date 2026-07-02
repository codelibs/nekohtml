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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Coverage tests for {@link HTMLTagBalancerFilter}.
 * Targets uncovered paths: formatting bookkeeping, structure synthesis,
 * implied end tags, stray-end-tag suppression, null-handler delegation, and
 * document initialization.
 *
 * @author CodeLibs Project
 */
public class HTMLTagBalancerFilterCoverageTest {

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

    // ---------------------------------------------------------------
    // 1. Active formatting element bookkeeping
    // ---------------------------------------------------------------

    @Test
    public void testAddFormattingElement_addsOnce() {
        filter.addFormattingElement("B");
        filter.addFormattingElement("B"); // duplicate collapses to a single entry
        filter.addFormattingElement("I");

        assertEquals(2, filter.activeFormattingElements.size());
        assertTrue(filter.activeFormattingElements.contains("B"));
        assertTrue(filter.activeFormattingElements.contains("I"));
    }

    @Test
    public void testRemoveFormattingElement_returnsTrue() {
        filter.addFormattingElement("B");

        assertTrue(filter.removeFormattingElement("B"));
        assertFalse(filter.activeFormattingElements.contains("B"));
    }

    @Test
    public void testRemoveFormattingElement_returnsFalse() {
        assertFalse(filter.removeFormattingElement("B"));
    }

    @Test
    public void testRemoveFormattingElement_afterAddAndRemove() {
        filter.addFormattingElement("B");
        assertTrue(filter.removeFormattingElement("B"));
        // Second removal should return false
        assertFalse(filter.removeFormattingElement("B"));
    }

    @Test
    public void testFormattingElementsTrackedThroughStartElement() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "body", "BODY", new AttributesImpl());
        filter.startElement("", "b", "B", new AttributesImpl());

        assertTrue(filter.activeFormattingElements.contains("B"));

        // Closing B removes it from the active list.
        filter.endElement("", "b", "B");
        assertFalse(filter.activeFormattingElements.contains("B"));
    }

    // ---------------------------------------------------------------
    // 2. isOnStack
    // ---------------------------------------------------------------

    @Test
    public void testIsOnStack() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "body", "BODY", new AttributesImpl());
        filter.startElement("", "div", "DIV", new AttributesImpl());

        assertTrue(filter.isOnStack("HTML"));
        assertTrue(filter.isOnStack("BODY"));
        assertTrue(filter.isOnStack("DIV"));
        assertFalse(filter.isOnStack("SPAN"));
    }

    // ---------------------------------------------------------------
    // 3. Implied end tags
    // ---------------------------------------------------------------

    @Test
    public void testImpliedEndTag_liClosesLi() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "body", "BODY", new AttributesImpl());
        filter.startElement("", "ul", "UL", new AttributesImpl());
        filter.startElement("", "li", "LI", new AttributesImpl());

        reset(contentHandler);

        // Second LI implicitly closes the first one.
        filter.startElement("", "li", "LI", new AttributesImpl());

        var inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).endElement("", "li", "LI");
        inOrder.verify(contentHandler).startElement(eq(""), eq("li"), eq("LI"), any());
        // The two LI elements are siblings (UL still open).
        assertTrue(filter.isOnStack("UL"));
    }

    @Test
    public void testImpliedEndTag_pClosesP() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "body", "BODY", new AttributesImpl());
        filter.startElement("", "p", "P", new AttributesImpl());

        reset(contentHandler);

        filter.startElement("", "p", "P", new AttributesImpl());

        verify(contentHandler).endElement("", "p", "P");
    }

    // ---------------------------------------------------------------
    // 4. Structure synthesis
    // ---------------------------------------------------------------

    @Test
    public void testBodySynthesizedForContent() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());

        // A block element with no explicit body opens one.
        filter.startElement("", "div", "DIV", new AttributesImpl());

        assertTrue(filter.bodyOpened);
        var inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).startElement(eq(""), eq("html"), eq("HTML"), any());
        inOrder.verify(contentHandler).startElement(eq(""), eq("BODY"), eq("BODY"), any());
        inOrder.verify(contentHandler).startElement(eq(""), eq("div"), eq("DIV"), any());
    }

    @Test
    public void testHeadSynthesizedForTitle() throws SAXException {
        filter.startDocument();

        // A bare TITLE (no html/head) synthesizes HTML then HEAD.
        filter.startElement("", "title", "TITLE", new AttributesImpl());

        assertTrue(filter.isOnStack("HEAD"));
        var inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).startElement(eq(""), eq("HTML"), eq("HTML"), any());
        inOrder.verify(contentHandler).startElement(eq(""), eq("HEAD"), eq("HEAD"), any());
        inOrder.verify(contentHandler).startElement(eq(""), eq("title"), eq("TITLE"), any());
    }

    @Test
    public void testDuplicateHtmlIgnored() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "html", "HTML", new AttributesImpl()); // duplicate

        // Only one HTML start should have been emitted.
        verify(contentHandler, times(1)).startElement(eq(""), eq("html"), eq("HTML"), any());
    }

    @Test
    public void testHeadStartIgnoredAfterBody() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "body", "BODY", new AttributesImpl());

        reset(contentHandler);

        filter.startElement("", "head", "HEAD", new AttributesImpl());

        // HEAD after BODY is ignored.
        verify(contentHandler, never()).startElement(eq(""), eq("head"), eq("HEAD"), any());
    }

    @Test
    public void testStartBody_closesHead() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "head", "HEAD", new AttributesImpl());

        // Starting BODY should auto-close HEAD.
        filter.startElement("", "body", "BODY", new AttributesImpl());

        verify(contentHandler).endElement("", "head", "HEAD");
        assertTrue(filter.headClosed);
        assertTrue(filter.bodyOpened);
    }

    // ---------------------------------------------------------------
    // 5. startElement/endElement with null qName
    // ---------------------------------------------------------------

    @Test
    public void testStartElement_nullQName() throws SAXException {
        filter.startDocument();

        filter.startElement("", "", null, new AttributesImpl());

        verify(contentHandler, never()).startElement(eq(""), eq(""), isNull(), any());
    }

    @Test
    public void testStartElement_emptyQName() throws SAXException {
        filter.startDocument();

        filter.startElement("", "", "", new AttributesImpl());

        verify(contentHandler, never()).startElement(eq(""), eq(""), eq(""), any());
    }

    @Test
    public void testEndElement_nullQName() throws SAXException {
        filter.startDocument();

        filter.endElement("", "", null);

        verify(contentHandler, never()).endElement(eq(""), eq(""), isNull());
    }

    @Test
    public void testEndElement_emptyQName() throws SAXException {
        filter.startDocument();

        filter.endElement("", "", "");

        verify(contentHandler, never()).endElement(eq(""), eq(""), eq(""));
    }

    // ---------------------------------------------------------------
    // 6. Stray end tags are ignored (not passed through)
    // ---------------------------------------------------------------

    @Test
    public void testEndElement_emptyStackIgnored() throws SAXException {
        filter.startDocument();
        // Stack is empty; a stray end tag is ignored.
        filter.endElement("", "div", "DIV");

        verify(contentHandler, never()).endElement(anyString(), anyString(), anyString());
    }

    @Test
    public void testEndElement_notOnStackIgnored() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "body", "BODY", new AttributesImpl());

        reset(contentHandler);

        // A SPAN that was never opened is ignored.
        filter.endElement("", "span", "SPAN");

        verify(contentHandler, never()).endElement(eq(""), eq("span"), eq("SPAN"));
        assertEquals(2, filter.elementStack.size());
    }

    @Test
    public void testEndElement_voidElementIgnored() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "body", "BODY", new AttributesImpl());
        filter.startElement("", "br", "BR", new AttributesImpl()); // void, not pushed

        reset(contentHandler);

        filter.endElement("", "br", "BR");

        verify(contentHandler, never()).endElement(eq(""), eq("br"), eq("BR"));
    }

    // ---------------------------------------------------------------
    // 7. LexicalHandler delegation with null lexicalHandler
    // ---------------------------------------------------------------

    @Test
    public void testStartDTD_nullLexicalHandler() throws SAXException {
        filter.setLexicalHandler(null);
        filter.startDTD("html", "-//W3C//DTD HTML 4.01//EN", "http://www.w3.org/TR/html4/strict.dtd");
    }

    @Test
    public void testEndDTD_nullLexicalHandler() throws SAXException {
        filter.setLexicalHandler(null);
        filter.endDTD();
    }

    @Test
    public void testStartEntity_nullLexicalHandler() throws SAXException {
        filter.setLexicalHandler(null);
        filter.startEntity("amp");
    }

    @Test
    public void testEndEntity_nullLexicalHandler() throws SAXException {
        filter.setLexicalHandler(null);
        filter.endEntity("amp");
    }

    @Test
    public void testStartCDATA_nullLexicalHandler() throws SAXException {
        filter.setLexicalHandler(null);
        filter.startCDATA();
    }

    @Test
    public void testEndCDATA_nullLexicalHandler() throws SAXException {
        filter.setLexicalHandler(null);
        filter.endCDATA();
    }

    @Test
    public void testComment_nullLexicalHandler() throws SAXException {
        filter.setLexicalHandler(null);
        filter.comment("test comment".toCharArray(), 0, 12);
    }

    // ---------------------------------------------------------------
    // 8. ignorableWhitespace, processingInstruction, skippedEntity with null contentHandler
    // ---------------------------------------------------------------

    @Test
    public void testIgnorableWhitespace_nullContentHandler() throws SAXException {
        filter.setContentHandler(null);
        filter.ignorableWhitespace(" ".toCharArray(), 0, 1);
    }

    @Test
    public void testProcessingInstruction_nullContentHandler() throws SAXException {
        filter.setContentHandler(null);
        filter.processingInstruction("xml", "version=\"1.0\"");
    }

    @Test
    public void testSkippedEntity_nullContentHandler() throws SAXException {
        filter.setContentHandler(null);
        filter.skippedEntity("nbsp");
    }

    // ---------------------------------------------------------------
    // 9. ensureDocumentInitialized
    // ---------------------------------------------------------------

    @Test
    public void testEnsureDocumentInitialized_autoAddsHTML() throws SAXException {
        filter.startDocument();
        assertFalse(filter.htmlOpened);

        // Starting a non-HTML element should trigger ensureDocumentInitialized.
        filter.startElement("", "div", "DIV", new AttributesImpl());

        assertTrue(filter.htmlOpened);
        assertTrue(filter.isOnStack("HTML"));

        var inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).startElement(eq(""), eq("HTML"), eq("HTML"), any());
        inOrder.verify(contentHandler).startElement(eq(""), eq("div"), eq("DIV"), any());
    }

    @Test
    public void testEnsureDocumentInitialized_notCalledForHTMLElement() throws SAXException {
        filter.startDocument();

        filter.startElement("", "html", "HTML", new AttributesImpl());

        assertTrue(filter.htmlOpened);
        assertEquals(1, filter.elementStack.size());
        assertEquals("HTML", filter.elementStack.peek().tagName);
    }

    @Test
    public void testEnsureDocumentInitialized_withNullContentHandler() throws SAXException {
        filter.setContentHandler(null);
        filter.htmlOpened = false;

        filter.ensureDocumentInitialized();

        assertTrue(filter.htmlOpened);
        // No HTML pushed since handler is null.
        assertTrue(filter.elementStack.isEmpty());
    }

    @Test
    public void testEnsureDocumentInitialized_triggeredByCharacters() throws SAXException {
        filter.startDocument();
        assertFalse(filter.htmlOpened);

        filter.characters("hello".toCharArray(), 0, 5);

        assertTrue(filter.htmlOpened);
        assertTrue(filter.isOnStack("HTML"));
        assertTrue(filter.bodyOpened);
    }

    @Test
    public void testWhitespaceCharacters_doNotInitialize() throws SAXException {
        filter.startDocument();
        assertFalse(filter.htmlOpened);

        // Whitespace-only content does not force structure synthesis.
        filter.characters("   ".toCharArray(), 0, 3);

        assertFalse(filter.htmlOpened);
        assertFalse(filter.bodyOpened);
        verify(contentHandler).characters(any(), anyInt(), anyInt());
    }

    @Test
    public void testComment_doesNotInitializeDocument() throws SAXException {
        filter.startDocument();
        assertFalse(filter.htmlOpened);

        // A comment before <html> no longer forces document initialization.
        filter.comment("a comment".toCharArray(), 0, 9);

        assertFalse(filter.htmlOpened);
        assertTrue(filter.elementStack.isEmpty());
        verify(lexicalHandler).comment("a comment".toCharArray(), 0, 9);
    }

    // ---------------------------------------------------------------
    // 10. startElement/endElement with null contentHandler
    // ---------------------------------------------------------------

    @Test
    public void testStartElement_nullContentHandler() throws SAXException {
        filter.setContentHandler(null);
        filter.startElement("", "div", "DIV", new AttributesImpl());
        assertTrue(filter.elementStack.isEmpty());
    }

    @Test
    public void testEndElement_nullContentHandler() throws SAXException {
        filter.setContentHandler(null);
        filter.endElement("", "div", "DIV");
    }

    // ---------------------------------------------------------------
    // 11. endDocument closes remaining elements
    // ---------------------------------------------------------------

    @Test
    public void testEndDocument_closesRemainingElements() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "body", "BODY", new AttributesImpl());
        filter.startElement("", "div", "DIV", new AttributesImpl());

        filter.endDocument();

        assertTrue(filter.elementStack.isEmpty());
        verify(contentHandler).endElement("", "div", "DIV");
        verify(contentHandler).endElement("", "body", "BODY");
        verify(contentHandler).endElement("", "html", "HTML");
        verify(contentHandler).endDocument();
    }

    // ---------------------------------------------------------------
    // 12. void elements are not pushed onto the stack
    // ---------------------------------------------------------------

    @Test
    public void testStartElement_voidElement_notPushed() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "body", "BODY", new AttributesImpl());
        int sizeBeforeBr = filter.elementStack.size();

        filter.startElement("", "br", "BR", new AttributesImpl());

        assertEquals(sizeBeforeBr, filter.elementStack.size());
    }

    // ---------------------------------------------------------------
    // 13. setDocumentLocator / prefix mapping with null contentHandler
    // ---------------------------------------------------------------

    @Test
    public void testSetDocumentLocator_nullContentHandler() {
        filter.setContentHandler(null);
        filter.setDocumentLocator(null);
    }

    @Test
    public void testStartPrefixMapping_nullContentHandler() throws SAXException {
        filter.setContentHandler(null);
        filter.startPrefixMapping("ns", "http://example.com");
    }

    @Test
    public void testEndPrefixMapping_nullContentHandler() throws SAXException {
        filter.setContentHandler(null);
        filter.endPrefixMapping("ns");
    }

    // ---------------------------------------------------------------
    // 14. endElement auto-closes elements above the target
    // ---------------------------------------------------------------

    @Test
    public void testEndElement_autoClosesAboveElements() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "body", "BODY", new AttributesImpl());
        filter.startElement("", "div", "DIV", new AttributesImpl());
        filter.startElement("", "span", "SPAN", new AttributesImpl());

        reset(contentHandler);

        // Close DIV, which should auto-close SPAN first.
        filter.endElement("", "div", "DIV");

        var inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).endElement("", "span", "SPAN");
        inOrder.verify(contentHandler).endElement("", "div", "DIV");
    }

    // ---------------------------------------------------------------
    // 15. Balanced formatting-end reconstruction (one-shot formatting)
    // ---------------------------------------------------------------

    @Test
    public void testFormattingEnd_reopensContainerNotFormatting() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "body", "BODY", new AttributesImpl());
        filter.startElement("", "b", "B", new AttributesImpl());
        filter.startElement("", "div", "DIV", new AttributesImpl());

        reset(contentHandler);

        // </b> while DIV is still open: close DIV and B, reopen DIV (not B).
        filter.endElement("", "b", "B");

        var inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).endElement("", "div", "DIV");
        inOrder.verify(contentHandler).endElement("", "b", "B");
        inOrder.verify(contentHandler).startElement(eq(""), eq("div"), eq("DIV"), any());

        // DIV is reopened; B is one-shot and stays closed.
        assertTrue(filter.isOnStack("DIV"));
        assertFalse(filter.isOnStack("B"));
    }

    @Test
    public void testFormattingEnd_innerFormattingStaysClosed() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "body", "BODY", new AttributesImpl());
        filter.startElement("", "b", "B", new AttributesImpl());
        filter.startElement("", "i", "I", new AttributesImpl());

        reset(contentHandler);

        // </b> while I is still open: close I and B, no reopen (I is formatting).
        filter.endElement("", "b", "B");

        var inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).endElement("", "i", "I");
        inOrder.verify(contentHandler).endElement("", "b", "B");
        verify(contentHandler, never()).startElement(eq(""), eq("i"), eq("I"), any());

        assertFalse(filter.isOnStack("I"));
        assertFalse(filter.isOnStack("B"));
    }
}
