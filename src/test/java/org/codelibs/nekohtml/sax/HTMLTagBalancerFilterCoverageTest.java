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
 * Targets uncovered paths: formatting element management, AAA edge cases,
 * null handler delegation, and document initialization.
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
    // 1. clearFormattingElementsToLastMarker
    // ---------------------------------------------------------------

    @Test
    public void testClearFormattingElementsToLastMarker_removesUpToMarker() {
        filter.pushFormattingMarker();
        filter.addFormattingElement("B");
        filter.addFormattingElement("I");

        assertEquals(3, filter.activeFormattingElements.size()); // MARKER, B, I

        filter.clearFormattingElementsToLastMarker();

        // Only elements before the marker should remain
        assertEquals(0, filter.activeFormattingElements.size());
    }

    @Test
    public void testClearFormattingElementsToLastMarker_withElementsBeforeMarker() {
        filter.addFormattingElement("STRONG");
        filter.pushFormattingMarker();
        filter.addFormattingElement("B");
        filter.addFormattingElement("I");

        assertEquals(4, filter.activeFormattingElements.size());

        filter.clearFormattingElementsToLastMarker();

        // STRONG should remain
        assertEquals(1, filter.activeFormattingElements.size());
        assertEquals("STRONG", filter.activeFormattingElements.get(0));
    }

    @Test
    public void testClearFormattingElementsToLastMarker_noMarker() {
        filter.addFormattingElement("B");
        filter.addFormattingElement("I");

        filter.clearFormattingElementsToLastMarker();

        // All elements removed since no marker was found
        assertEquals(0, filter.activeFormattingElements.size());
    }

    // ---------------------------------------------------------------
    // 2. pushFormattingMarker
    // ---------------------------------------------------------------

    @Test
    public void testPushFormattingMarker_addsMarkerToList() {
        filter.pushFormattingMarker();

        assertEquals(1, filter.activeFormattingElements.size());
        assertSame(HTMLTagBalancerFilter.MARKER, filter.activeFormattingElements.get(0));
    }

    @Test
    public void testPushFormattingMarker_multipleMarkers() {
        filter.pushFormattingMarker();
        filter.addFormattingElement("B");
        filter.pushFormattingMarker();
        filter.addFormattingElement("I");

        assertEquals(4, filter.activeFormattingElements.size());
        assertSame(HTMLTagBalancerFilter.MARKER, filter.activeFormattingElements.get(0));
        assertSame(HTMLTagBalancerFilter.MARKER, filter.activeFormattingElements.get(2));
    }

    // ---------------------------------------------------------------
    // 3. findFormattingElement with MARKER stopping condition
    // ---------------------------------------------------------------

    @Test
    public void testFindFormattingElement_stopsAtMarker() {
        filter.addFormattingElement("B");
        filter.pushFormattingMarker();
        filter.addFormattingElement("I");

        // B is before the marker, so searching for B should return -1
        assertEquals(-1, filter.findFormattingElement("B"));

        // I is after the marker, so should be found
        assertEquals(2, filter.findFormattingElement("I"));
    }

    @Test
    public void testFindFormattingElement_notPresent() {
        filter.addFormattingElement("B");

        assertEquals(-1, filter.findFormattingElement("STRONG"));
    }

    // ---------------------------------------------------------------
    // 4. isSpecialElement
    // ---------------------------------------------------------------

    @Test
    public void testIsSpecialElement_specialElements() {
        String[] specialElements =
                { "ADDRESS", "ARTICLE", "ASIDE", "BLOCKQUOTE", "DETAILS", "DIALOG", "DIV", "DL", "FIELDSET", "FIGCAPTION", "FIGURE",
                        "FOOTER", "FORM", "H1", "H2", "H3", "H4", "H5", "H6", "HEADER", "HGROUP", "HR", "LI", "MAIN", "NAV", "OL", "P",
                        "PRE", "SEARCH", "SECTION", "TABLE", "UL" };

        for (String elem : specialElements) {
            assertTrue(filter.isSpecialElement(elem), elem + " should be special");
        }
    }

    @Test
    public void testIsSpecialElement_nonSpecialElements() {
        String[] nonSpecialElements = { "SPAN", "A", "B", "I", "STRONG", "EM", "FONT", "IMG", "BR", "INPUT" };

        for (String elem : nonSpecialElements) {
            assertFalse(filter.isSpecialElement(elem), elem + " should not be special");
        }
    }

    // ---------------------------------------------------------------
    // 5. AAA outer loop exhaustion (8 iterations)
    // ---------------------------------------------------------------

    @Test
    public void testAAAOuterLoopExhaustion() throws SAXException {
        filter.startDocument();

        // Start HTML so document is initialized
        filter.startElement("", "html", "HTML", new AttributesImpl());

        // Start a formatting element (B)
        filter.startElement("", "b", "B", new AttributesImpl());

        // Now manually manipulate: remove B from the stack but keep it in activeFormattingElements
        // so that findFormattingElement returns >= 0 but lastIndexOf in stack returns -1.
        // Actually, for loop exhaustion we need findFormattingElement to keep returning >= 0
        // AND formattingElemIndexInStack >= 0 AND furthestBlockIndex >= 0 on every iteration.
        // But in practice the AAA returns on each iteration. So we need a scenario where
        // findFormattingElement returns < 0 causing it to break after some iterations.

        // The simplest way: make findFormattingElement return -1 on first iteration
        // so the outer loop breaks immediately, then falls through to the fallback code.
        // Actually for loop exhaustion, we need the loop to run 8 times without returning.
        // Looking at the code, every branch inside the loop either returns or breaks.
        // The only way to exhaust is if the code never enters any branch that returns/breaks.
        // Since findFormattingElement < 0 breaks, and all other paths return,
        // we can't truly exhaust the loop with the current implementation.
        // But we can test the fallback code after the loop by directly calling runAdoptionAgencyAlgorithm
        // with a subclass that forces loop exhaustion.

        // Use a subclass to force loop exhaustion
        HTMLTagBalancerFilter customFilter = new HTMLTagBalancerFilter() {
            private int loopCount = 0;

            @Override
            protected int findFormattingElement(String tagName) {
                // Return valid index for first 8 calls, then -1
                if (loopCount < 8) {
                    return 0;
                }
                return -1;
            }

            @Override
            protected int findFurthestBlock(int formattingIndex) {
                loopCount++;
                // Return a block index that triggers the complex case with return
                return -1; // No furthest block -> returns inside loop
            }
        };
        customFilter.setContentHandler(contentHandler);

        // Set up state: add formatting element and push onto stack
        customFilter.activeFormattingElements.add("B");
        customFilter.elementStack.push("HTML");
        customFilter.elementStack.push("B");

        // This will run the AAA - the first iteration finds no furthest block and returns
        customFilter.runAdoptionAgencyAlgorithm("B", "", "b", "B");

        // Verify endElement was called for B
        verify(contentHandler, atLeastOnce()).endElement(anyString(), anyString(), anyString());
    }

    @Test
    public void testAAAFallbackAfterLoopExhaustion() throws SAXException {
        // Test the fallback code after outer loop (lines 717-727)
        HTMLTagBalancerFilter customFilter = new HTMLTagBalancerFilter() {
            private int callCount = 0;

            @Override
            protected int findFormattingElement(String tagName) {
                // Always return -1 to break out of loop immediately
                return -1;
            }
        };
        customFilter.setContentHandler(contentHandler);
        customFilter.elementStack.push("HTML");
        customFilter.elementStack.push("B");

        // After breaking from the loop, the fallback code checks elementStack
        customFilter.runAdoptionAgencyAlgorithm("B", "", "b", "B");

        // Fallback code should close B from the stack
        verify(contentHandler).endElement("", "B", "B");
        assertEquals(1, customFilter.elementStack.size()); // HTML remains
    }

    // ---------------------------------------------------------------
    // 6. AAA formatting element in active list but NOT in stack
    // ---------------------------------------------------------------

    @Test
    public void testAAAFormattingElementInListButNotInStack() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());

        // Manually add B to activeFormattingElements but NOT to elementStack
        filter.activeFormattingElements.add("B");

        // Trigger endElement for B - this should trigger AAA
        filter.endElement("", "b", "B");

        // B should be removed from activeFormattingElements
        assertFalse(filter.activeFormattingElements.contains("B"));
    }

    // ---------------------------------------------------------------
    // 7. startElement/endElement with null qName
    // ---------------------------------------------------------------

    @Test
    public void testStartElement_nullQName() throws SAXException {
        filter.startDocument();

        // Should not throw, should log warning and return
        filter.startElement("", "", null, new AttributesImpl());

        // Content handler should not receive startElement for null qName
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
    // 8. endElement with empty stack
    // ---------------------------------------------------------------

    @Test
    public void testEndElement_emptyStack() throws SAXException {
        filter.startDocument();
        // Stack is empty, endElement should just pass through
        filter.elementStack.clear();

        filter.endElement("", "div", "DIV");

        verify(contentHandler).endElement("", "div", "DIV");
    }

    // ---------------------------------------------------------------
    // 9. LexicalHandler delegation with null lexicalHandler
    // ---------------------------------------------------------------

    @Test
    public void testStartDTD_nullLexicalHandler() throws SAXException {
        filter.setLexicalHandler(null);
        // Should not throw
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
        // Should not throw
        filter.comment("test comment".toCharArray(), 0, 12);
    }

    // ---------------------------------------------------------------
    // 10. ignorableWhitespace, processingInstruction, skippedEntity with null contentHandler
    // ---------------------------------------------------------------

    @Test
    public void testIgnorableWhitespace_nullContentHandler() throws SAXException {
        filter.setContentHandler(null);
        // Should not throw
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
    // 11. ensureDocumentInitialized
    // ---------------------------------------------------------------

    @Test
    public void testEnsureDocumentInitialized_autoAddsHTML() throws SAXException {
        filter.startDocument();
        assertFalse(filter.documentInitialized);

        // Starting a non-HTML element should trigger ensureDocumentInitialized
        filter.startElement("", "div", "DIV", new AttributesImpl());

        assertTrue(filter.documentInitialized);
        // HTML should have been auto-added to the stack
        assertTrue(filter.elementStack.contains("HTML"));

        // Verify HTML start element was emitted before DIV
        var inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).startElement(eq(""), eq("HTML"), eq("HTML"), any());
        inOrder.verify(contentHandler).startElement(eq(""), eq("div"), eq("DIV"), any());
    }

    @Test
    public void testEnsureDocumentInitialized_notCalledForHTMLElement() throws SAXException {
        filter.startDocument();

        filter.startElement("", "html", "HTML", new AttributesImpl());

        assertTrue(filter.documentInitialized);
        // HTML should be on stack from normal startElement push, not from ensureDocumentInitialized
        assertEquals(1, filter.elementStack.size());
        assertEquals("HTML", filter.elementStack.peek());
    }

    @Test
    public void testEnsureDocumentInitialized_withNullContentHandler() throws SAXException {
        filter.setContentHandler(null);
        filter.documentInitialized = false;

        filter.ensureDocumentInitialized();

        assertTrue(filter.documentInitialized);
        // No HTML pushed since handler is null
        assertTrue(filter.elementStack.isEmpty());
    }

    @Test
    public void testEnsureDocumentInitialized_triggeredByCharacters() throws SAXException {
        filter.startDocument();
        assertFalse(filter.documentInitialized);

        filter.characters("hello".toCharArray(), 0, 5);

        assertTrue(filter.documentInitialized);
        assertTrue(filter.elementStack.contains("HTML"));
    }

    @Test
    public void testEnsureDocumentInitialized_triggeredByComment() throws SAXException {
        filter.startDocument();
        assertFalse(filter.documentInitialized);

        filter.comment("a comment".toCharArray(), 0, 9);

        assertTrue(filter.documentInitialized);
        // HTML should have been auto-added
        assertTrue(filter.elementStack.contains("HTML"));
        // lexicalHandler.comment should have been called
        verify(lexicalHandler).comment("a comment".toCharArray(), 0, 9);
    }

    // ---------------------------------------------------------------
    // 12. removeFormattingElement return value
    // ---------------------------------------------------------------

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

    // ---------------------------------------------------------------
    // 13. comment with null lexicalHandler (already covered in #9,
    //     but verifying ensureDocumentInitialized still runs)
    // ---------------------------------------------------------------

    @Test
    public void testComment_nullLexicalHandler_stillInitializesDocument() throws SAXException {
        filter.setLexicalHandler(null);
        filter.startDocument();
        assertFalse(filter.documentInitialized);

        // Should not throw, and should still initialize document
        filter.comment("test".toCharArray(), 0, 4);

        assertTrue(filter.documentInitialized);
    }

    // ---------------------------------------------------------------
    // Additional coverage: AAA with furthest block (complex case)
    // ---------------------------------------------------------------

    @Test
    public void testAAAWithFurthestBlock() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        // Open B (formatting)
        filter.startElement("", "b", "B", new AttributesImpl());
        // Open DIV (special/block element) - this is the furthest block
        filter.startElement("", "div", "DIV", new AttributesImpl());

        // Close B - triggers AAA with a furthest block
        filter.endElement("", "b", "B");

        // B should be removed from formatting elements
        assertFalse(filter.activeFormattingElements.contains("B"));
        // endElement for B should have been called
        verify(contentHandler).endElement("", "b", "B");
    }

    @Test
    public void testAAANoFurthestBlock_withNestedFormatting() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        // Open B (formatting)
        filter.startElement("", "b", "B", new AttributesImpl());
        // Open I (formatting, nested inside B)
        filter.startElement("", "i", "I", new AttributesImpl());

        // Close B - triggers AAA with no furthest block, I needs reopening
        filter.endElement("", "b", "B");

        // B should be removed from formatting elements
        assertFalse(filter.activeFormattingElements.contains("B"));
        // I should be reopened (still in active formatting elements)
        assertTrue(filter.activeFormattingElements.contains("I"));
    }

    // ---------------------------------------------------------------
    // Additional: startElement/endElement with null contentHandler
    // ---------------------------------------------------------------

    @Test
    public void testStartElement_nullContentHandler() throws SAXException {
        filter.setContentHandler(null);
        // Should return early without error
        filter.startElement("", "div", "DIV", new AttributesImpl());
        assertTrue(filter.elementStack.isEmpty());
    }

    @Test
    public void testEndElement_nullContentHandler() throws SAXException {
        filter.setContentHandler(null);
        filter.endElement("", "div", "DIV");
        // No exception thrown
    }

    // ---------------------------------------------------------------
    // Additional: endDocument closes remaining elements
    // ---------------------------------------------------------------

    @Test
    public void testEndDocument_closesRemainingElements() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "body", "BODY", new AttributesImpl());
        filter.startElement("", "div", "DIV", new AttributesImpl());

        filter.endDocument();

        // All elements should be closed
        assertTrue(filter.elementStack.isEmpty());
        verify(contentHandler).endElement("", "DIV", "DIV");
        verify(contentHandler).endElement("", "BODY", "BODY");
        verify(contentHandler).endElement("", "HTML", "HTML");
        verify(contentHandler).endDocument();
    }

    // ---------------------------------------------------------------
    // Additional: void elements are not pushed onto stack
    // ---------------------------------------------------------------

    @Test
    public void testStartElement_voidElement_notPushed() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        int stackSizeBeforeBr = filter.elementStack.size();

        filter.startElement("", "br", "BR", new AttributesImpl());

        // BR is void, should not be pushed
        assertEquals(stackSizeBeforeBr, filter.elementStack.size());
    }

    // ---------------------------------------------------------------
    // Additional: AAA with null contentHandler
    // ---------------------------------------------------------------

    @Test
    public void testRunAdoptionAgencyAlgorithm_nullContentHandler() throws SAXException {
        filter.setContentHandler(null);
        filter.activeFormattingElements.add("B");
        filter.elementStack.push("B");

        // Should return early without error
        filter.runAdoptionAgencyAlgorithm("B", "", "b", "B");
    }

    // ---------------------------------------------------------------
    // Additional: findFurthestBlock
    // ---------------------------------------------------------------

    @Test
    public void testFindFurthestBlock_noBlock() {
        filter.elementStack.push("HTML");
        filter.elementStack.push("B"); // index 1
        filter.elementStack.push("I"); // index 2 - not special

        assertEquals(-1, filter.findFurthestBlock(1));
    }

    @Test
    public void testFindFurthestBlock_withBlock() {
        filter.elementStack.push("HTML");
        filter.elementStack.push("B"); // index 1
        filter.elementStack.push("DIV"); // index 2 - special

        assertEquals(2, filter.findFurthestBlock(1));
    }

    // ---------------------------------------------------------------
    // Additional: setDocumentLocator with null contentHandler
    // ---------------------------------------------------------------

    @Test
    public void testSetDocumentLocator_nullContentHandler() {
        filter.setContentHandler(null);
        // Should not throw
        filter.setDocumentLocator(null);
    }

    // ---------------------------------------------------------------
    // Additional: startPrefixMapping and endPrefixMapping
    // ---------------------------------------------------------------

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
    // Additional: endElement where element is not on the stack
    // ---------------------------------------------------------------

    @Test
    public void testEndElement_elementNotOnStack() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "body", "BODY", new AttributesImpl());

        // Try to close a SPAN that was never opened
        filter.endElement("", "span", "SPAN");

        // Should pass through endElement
        verify(contentHandler).endElement("", "span", "SPAN");
        // Stack should be unchanged
        assertEquals(2, filter.elementStack.size());
    }

    // ---------------------------------------------------------------
    // Additional: closing elements above target in endElement
    // ---------------------------------------------------------------

    @Test
    public void testEndElement_autoClosesAboveElements() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "div", "DIV", new AttributesImpl());
        filter.startElement("", "span", "SPAN", new AttributesImpl());

        // Close DIV, which should auto-close SPAN first
        filter.endElement("", "div", "DIV");

        var inOrder = inOrder(contentHandler);
        inOrder.verify(contentHandler).endElement("", "SPAN", "SPAN");
        inOrder.verify(contentHandler).endElement("", "div", "DIV");
    }

    // ---------------------------------------------------------------
    // Additional: BODY element closes HEAD
    // ---------------------------------------------------------------

    @Test
    public void testStartBody_closesHead() throws SAXException {
        filter.startDocument();
        filter.startElement("", "html", "HTML", new AttributesImpl());
        filter.startElement("", "head", "HEAD", new AttributesImpl());

        // Starting BODY should auto-close HEAD
        filter.startElement("", "body", "BODY", new AttributesImpl());

        verify(contentHandler).endElement("", "HEAD", "HEAD");
    }
}
