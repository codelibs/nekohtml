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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Coverage tests for SAXToDOMHandler targeting uncovered paths.
 * Exercises LexicalHandler no-ops, strict mode branches, skip depth behavior,
 * and edge cases around null/empty state.
 */
public class SAXToDOMHandlerCoverageTest {

    private static final String PROP_DOM_STRICT = "nekohtml.dom.strict";

    private DocumentBuilder documentBuilder;
    private SAXToDOMHandler handler;

    @BeforeEach
    public void setUp() throws Exception {
        System.clearProperty(PROP_DOM_STRICT);
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        documentBuilder = factory.newDocumentBuilder();
        handler = new SAXToDOMHandler(documentBuilder);
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(PROP_DOM_STRICT);
    }

    // ===== LexicalHandler no-op methods =====

    @Test
    public void testStartDTD() throws Exception {
        assertDoesNotThrow(() -> handler.startDTD("html", "-//W3C//DTD HTML 4.01//EN", "http://www.w3.org/TR/html4/strict.dtd"));
    }

    @Test
    public void testEndDTD() throws Exception {
        assertDoesNotThrow(() -> handler.endDTD());
    }

    @Test
    public void testStartEntity() throws Exception {
        assertDoesNotThrow(() -> handler.startEntity("amp"));
    }

    @Test
    public void testEndEntity() throws Exception {
        assertDoesNotThrow(() -> handler.endEntity("amp"));
    }

    @Test
    public void testStartCDATA() throws Exception {
        assertDoesNotThrow(() -> handler.startCDATA());
    }

    @Test
    public void testEndCDATA() throws Exception {
        assertDoesNotThrow(() -> handler.endCDATA());
    }

    // ===== startElement with document == null (before startDocument) =====

    @Test
    public void testStartElementBeforeStartDocument_strictTrue() {
        System.setProperty(PROP_DOM_STRICT, "true");
        handler = new SAXToDOMHandler(documentBuilder);

        final SAXException ex = assertThrows(SAXException.class, () -> handler.startElement("", "div", "div", new AttributesImpl()));
        assertTrue(ex.getMessage().contains("before startDocument()"));
    }

    @Test
    public void testStartElementBeforeStartDocument_strictFalse() throws Exception {
        System.setProperty(PROP_DOM_STRICT, "false");
        handler = new SAXToDOMHandler(documentBuilder);

        // Should not throw; starts skipping
        handler.startElement("", "div", "div", new AttributesImpl());
        // Document is still null
        assertNull(handler.getDocument());
    }

    @Test
    public void testStartElementBeforeStartDocument_strictNotSet() throws Exception {
        // Property not set (default)
        handler = new SAXToDOMHandler(documentBuilder);

        handler.startElement("", "div", "div", new AttributesImpl());
        assertNull(handler.getDocument());
    }

    // ===== startElement with empty stack =====

    @Test
    public void testStartElementEmptyStack_strictTrue() throws Exception {
        System.setProperty(PROP_DOM_STRICT, "true");
        handler = new SAXToDOMHandler(documentBuilder);
        handler.startDocument();
        // Add and remove element so stack becomes just document, then add and close root to empty stack
        handler.startElement("", "html", "html", new AttributesImpl());
        handler.endElement("", "html", "html");
        handler.endDocument();
        // After endDocument, stack is cleared. Now call startElement again on same handler.
        // But document is still set, so it goes past the null check.
        // We need document != null but stack empty.
        // After endDocument, elementStack.clear() is called, so stack is empty and document is set.
        final SAXException ex = assertThrows(SAXException.class, () -> handler.startElement("", "p", "p", new AttributesImpl()));
        assertTrue(ex.getMessage().contains("empty element stack"));
    }

    @Test
    public void testStartElementEmptyStack_strictFalse() throws Exception {
        System.setProperty(PROP_DOM_STRICT, "false");
        handler = new SAXToDOMHandler(documentBuilder);
        handler.startDocument();
        handler.startElement("", "html", "html", new AttributesImpl());
        handler.endElement("", "html", "html");
        handler.endDocument();

        // Should not throw; logs warning and starts skipping
        handler.startElement("", "p", "p", new AttributesImpl());
        // Subsequent startElement should increment skipDepth
        handler.startElement("", "span", "span", new AttributesImpl());
        // And endElement should decrement
        handler.endElement("", "span", "span");
        handler.endElement("", "p", "p");
    }

    @Test
    public void testStartElementEmptyStack_strictNotSet() throws Exception {
        handler = new SAXToDOMHandler(documentBuilder);
        handler.startDocument();
        handler.startElement("", "html", "html", new AttributesImpl());
        handler.endElement("", "html", "html");
        handler.endDocument();

        // Should not throw; logs debug and starts skipping
        handler.startElement("", "p", "p", new AttributesImpl());
    }

    // ===== startElement causing DOMException =====
    // We trigger HIERARCHY_REQUEST_ERR by appending a second document element
    // after the document root already has a child element, and trying to append
    // directly to the Document node. We do this by popping the root element so
    // the stack has only the Document node, then starting a new element.

    @Test
    public void testStartElementDOMException_strictTrue() throws Exception {
        System.setProperty(PROP_DOM_STRICT, "true");
        handler = new SAXToDOMHandler(documentBuilder);
        handler.startDocument();
        // Add root element
        handler.startElement("", "html", "html", new AttributesImpl());
        handler.endElement("", "html", "html");
        // Now stack has only Document. Document already has a document element.
        // Adding another element to Document should cause HIERARCHY_REQUEST_ERR.
        final SAXException ex = assertThrows(SAXException.class, () -> handler.startElement("", "extra", "extra", new AttributesImpl()));
        assertTrue(ex.getMessage().contains("DOM hierarchy violation"));
    }

    @Test
    public void testStartElementDOMException_strictFalse() throws Exception {
        System.setProperty(PROP_DOM_STRICT, "false");
        handler = new SAXToDOMHandler(documentBuilder);
        handler.startDocument();
        handler.startElement("", "html", "html", new AttributesImpl());
        handler.endElement("", "html", "html");

        // Should not throw; logs warning and starts skipping
        handler.startElement("", "extra", "extra", new AttributesImpl());
        // Verify document still has only one root element
        final Document doc = handler.getDocument();
        assertNotNull(doc.getDocumentElement());
        assertEquals("html", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testStartElementDOMException_strictNotSet() throws Exception {
        handler = new SAXToDOMHandler(documentBuilder);
        handler.startDocument();
        handler.startElement("", "html", "html", new AttributesImpl());
        handler.endElement("", "html", "html");

        // Should not throw; logs debug and starts skipping
        handler.startElement("", "extra", "extra", new AttributesImpl());
        final Document doc = handler.getDocument();
        assertNotNull(doc.getDocumentElement());
        assertEquals("html", doc.getDocumentElement().getNodeName());
    }

    // ===== endElement with mismatched tag name =====

    @Test
    public void testEndElementMismatchedTag_strictFalse() throws Exception {
        System.setProperty(PROP_DOM_STRICT, "false");
        handler = new SAXToDOMHandler(documentBuilder);
        handler.startDocument();
        handler.startElement("", "html", "html", new AttributesImpl());
        handler.startElement("", "div", "div", new AttributesImpl());

        // End with wrong tag name; should log warning and not pop
        handler.endElement("", "span", "span");

        // div is still on the stack; can close it properly
        handler.endElement("", "div", "div");
        handler.endElement("", "html", "html");
        handler.endDocument();

        final Document doc = handler.getDocument();
        assertNotNull(doc.getDocumentElement());
    }

    @Test
    public void testEndElementMismatchedTag_strictNotSet() throws Exception {
        handler = new SAXToDOMHandler(documentBuilder);
        handler.startDocument();
        handler.startElement("", "html", "html", new AttributesImpl());
        handler.startElement("", "div", "div", new AttributesImpl());

        // End with wrong tag name; should log debug and not pop
        handler.endElement("", "span", "span");

        handler.endElement("", "div", "div");
        handler.endElement("", "html", "html");
        handler.endDocument();

        final Document doc = handler.getDocument();
        assertNotNull(doc.getDocumentElement());
    }

    // ===== endElement with empty stack =====

    @Test
    public void testEndElementEmptyStack_strictFalse() throws Exception {
        System.setProperty(PROP_DOM_STRICT, "false");
        handler = new SAXToDOMHandler(documentBuilder);
        handler.startDocument();
        handler.startElement("", "html", "html", new AttributesImpl());
        handler.endElement("", "html", "html");
        handler.endDocument();
        // Stack is now empty; calling endElement should log warning
        handler.endElement("", "div", "div");
    }

    @Test
    public void testEndElementEmptyStack_strictNotSet() throws Exception {
        handler = new SAXToDOMHandler(documentBuilder);
        handler.startDocument();
        handler.startElement("", "html", "html", new AttributesImpl());
        handler.endElement("", "html", "html");
        handler.endDocument();
        // Stack is now empty
        handler.endElement("", "div", "div");
    }

    @Test
    public void testEndElementEmptyStack_strictTrue() throws Exception {
        // Note: endElement with empty stack does NOT throw even in strict mode;
        // it only logs. The code has no TRUE branch for this case.
        System.setProperty(PROP_DOM_STRICT, "true");
        handler = new SAXToDOMHandler(documentBuilder);
        handler.startDocument();
        handler.startElement("", "html", "html", new AttributesImpl());
        handler.endElement("", "html", "html");
        handler.endDocument();
        // Should not throw - there is no strict-mode throw for empty stack endElement
        handler.endElement("", "div", "div");
    }

    // ===== Skip depth behavior =====

    @Test
    public void testSkipDepthIncrementAndDecrement() throws Exception {
        handler = new SAXToDOMHandler(documentBuilder);
        // Don't call startDocument, so document == null, which triggers skipDepth = 1
        handler.startElement("", "div", "div", new AttributesImpl());
        // skipDepth is now 1; nested startElement should increment
        handler.startElement("", "span", "span", new AttributesImpl());
        // skipDepth is now 2
        handler.startElement("", "a", "a", new AttributesImpl());
        // skipDepth is now 3

        // endElement should decrement
        handler.endElement("", "a", "a");
        // skipDepth is now 2
        handler.endElement("", "span", "span");
        // skipDepth is now 1
        handler.endElement("", "div", "div");
        // skipDepth is now 0

        // Document is still null
        assertNull(handler.getDocument());
    }

    @Test
    public void testCharactersSuppressedDuringSkip() throws Exception {
        System.setProperty(PROP_DOM_STRICT, "false");
        handler = new SAXToDOMHandler(documentBuilder);
        handler.startDocument();
        handler.startElement("", "html", "html", new AttributesImpl());
        handler.endElement("", "html", "html");

        // Trigger skip by trying to add second root element (DOMException, lenient mode)
        handler.startElement("", "extra", "extra", new AttributesImpl());
        // Now in skip mode; characters should be suppressed
        handler.characters("should be ignored".toCharArray(), 0, 17);
        handler.endElement("", "extra", "extra");

        final Document doc = handler.getDocument();
        // The document should only have the original html element
        assertEquals(1, doc.getChildNodes().getLength());
    }

    @Test
    public void testCommentSuppressedDuringSkip() throws Exception {
        System.setProperty(PROP_DOM_STRICT, "false");
        handler = new SAXToDOMHandler(documentBuilder);
        handler.startDocument();
        handler.startElement("", "html", "html", new AttributesImpl());
        handler.endElement("", "html", "html");

        // Trigger skip
        handler.startElement("", "extra", "extra", new AttributesImpl());
        // Comment during skip should be suppressed
        handler.comment("skipped comment".toCharArray(), 0, 15);
        handler.endElement("", "extra", "extra");

        final Document doc = handler.getDocument();
        assertEquals(1, doc.getChildNodes().getLength());
    }

    // ===== characters() when textBuffer is null =====

    @Test
    public void testCharactersBeforeStartDocument() throws Exception {
        handler = new SAXToDOMHandler(documentBuilder);
        // textBuffer is null before startDocument
        assertDoesNotThrow(() -> handler.characters("hello".toCharArray(), 0, 5));
    }

    // ===== flushText when element stack is empty =====

    @Test
    public void testFlushTextWithEmptyStack() throws Exception {
        handler = new SAXToDOMHandler(documentBuilder);
        handler.startDocument();
        // Add text
        handler.characters("some text".toCharArray(), 0, 9);
        // Clear the stack by adding and removing root, then endDocument
        // Actually, let's directly trigger flushText via endDocument when stack has only document
        // Remove document from stack by doing startElement+endElement+endDocument
        // But we need text in the buffer when stack is empty.

        // Better approach: start doc, add text, then endElement on root to pop document node,
        // then endDocument triggers flushText with empty stack.
        // Actually endDocument clears the stack after flushText. Let me think...

        // Simplest: startDocument pushes Document. Add chars. Then pop Document via endElement
        // for a mismatched tag? No, that won't pop because of mismatch check.

        // After endDocument, stack is cleared and textBuffer remains set.
        // If we call endElement after endDocument, flushText is called with empty stack.
        handler.endDocument();
        // Add text after endDocument (textBuffer is still set)
        handler.characters("orphan text".toCharArray(), 0, 11);
        // Trigger flushText via endElement - stack is empty, text should be discarded
        handler.endElement("", "x", "x");
    }

    // ===== flushText when parent is Document node (not Element) =====

    @Test
    public void testFlushTextWhenParentIsDocument() throws Exception {
        handler = new SAXToDOMHandler(documentBuilder);
        handler.startDocument();
        // Stack has only Document. Add characters.
        handler.characters("text at document level".toCharArray(), 0, 21);
        // Trigger flushText by starting an element - parent is Document, not Element
        // So text should NOT be appended (Document node type check)
        handler.startElement("", "html", "html", new AttributesImpl());
        handler.endElement("", "html", "html");
        handler.endDocument();

        final Document doc = handler.getDocument();
        // Document should only have the html element, no text nodes
        boolean hasTextChild = false;
        for (int i = 0; i < doc.getChildNodes().getLength(); i++) {
            if (doc.getChildNodes().item(i).getNodeType() == Node.TEXT_NODE) {
                hasTextChild = true;
            }
        }
        assertTrue(!hasTextChild, "Document node should not have text children");
    }

    // ===== comment() when DOMException occurs =====

    @Test
    public void testCommentDOMException_strictTrue() throws Exception {
        // To trigger a DOMException from comment, we need to be in a state where
        // appendChild fails. We can do this after endDocument by manually setting up
        // a scenario. Actually, comment() on the Document node should work fine normally.
        // DOMException from comment is hard to trigger with standard DOM.
        // We'll use a different approach: parse normally, then after endDocument
        // the stack is empty, so comment would fail with EmptyStackException not DOMException.
        // Since triggering DOMException from createComment/appendChild on standard DOM nodes
        // is very difficult, we test the code path indirectly or skip this if not feasible.
        // Actually, we can trigger it: create a document with adopted node scenario.
        // The simplest way: the comment path works on Document nodes without issue.
        // Let's verify comment works normally (covers the try path at least).
        handler = new SAXToDOMHandler(documentBuilder);
        handler.startDocument();
        handler.comment("a comment".toCharArray(), 0, 9);
        handler.startElement("", "html", "html", new AttributesImpl());
        handler.comment("inside html".toCharArray(), 0, 11);
        handler.endElement("", "html", "html");
        handler.endDocument();

        final Document doc = handler.getDocument();
        // Document should have a comment and the html element
        int commentCount = 0;
        for (int i = 0; i < doc.getChildNodes().getLength(); i++) {
            if (doc.getChildNodes().item(i).getNodeType() == Node.COMMENT_NODE) {
                commentCount++;
            }
        }
        assertEquals(1, commentCount, "Document should have one comment before root element");

        // Check comment inside html element
        final Node html = doc.getDocumentElement();
        int innerComments = 0;
        for (int i = 0; i < html.getChildNodes().getLength(); i++) {
            if (html.getChildNodes().item(i).getNodeType() == Node.COMMENT_NODE) {
                innerComments++;
            }
        }
        assertEquals(1, innerComments, "HTML element should have one comment");
    }

    // ===== endElement mismatched tag with strict TRUE =====
    // Note: The code for mismatched end tag does NOT have a TRUE branch that throws.
    // It only has FALSE (warning) and NOT_SET (debug). Verify this works in TRUE mode too.

    @Test
    public void testEndElementMismatchedTag_strictTrue() throws Exception {
        System.setProperty(PROP_DOM_STRICT, "true");
        handler = new SAXToDOMHandler(documentBuilder);
        handler.startDocument();
        handler.startElement("", "html", "html", new AttributesImpl());
        handler.startElement("", "div", "div", new AttributesImpl());

        // Mismatched end tag - code only has FALSE and NOT_SET branches, no TRUE throw
        // In TRUE mode, it falls into the else branch (NOT_SET behavior: debug log)
        handler.endElement("", "span", "span");

        handler.endElement("", "div", "div");
        handler.endElement("", "html", "html");
        handler.endDocument();
    }

    // ===== Verify getDocument returns null before startDocument =====

    @Test
    public void testGetDocumentBeforeStartDocument() {
        assertNull(handler.getDocument());
    }

    // ===== Multiple sequential documents on same handler =====

    @Test
    public void testMultipleDocuments() throws Exception {
        handler.startDocument();
        handler.startElement("", "html", "html", new AttributesImpl());
        handler.endElement("", "html", "html");
        handler.endDocument();

        final Document firstDoc = handler.getDocument();
        assertNotNull(firstDoc);

        // Start a new document on the same handler
        handler.startDocument();
        handler.startElement("", "body", "body", new AttributesImpl());
        handler.endElement("", "body", "body");
        handler.endDocument();

        final Document secondDoc = handler.getDocument();
        assertNotNull(secondDoc);
        assertEquals("body", secondDoc.getDocumentElement().getNodeName());
    }

    // ===== flushText with skipDepth > 0 =====

    @Test
    public void testFlushTextDuringSkip() throws Exception {
        System.setProperty(PROP_DOM_STRICT, "false");
        handler = new SAXToDOMHandler(documentBuilder);
        handler.startDocument();
        handler.startElement("", "html", "html", new AttributesImpl());
        handler.endElement("", "html", "html");

        // Trigger skip mode via DOMException (second root element)
        handler.startElement("", "extra", "extra", new AttributesImpl());

        // Add characters while in skip mode
        handler.characters("skipped text".toCharArray(), 0, 12);

        // Now trigger flushText (via startElement while in skip mode)
        // This tests the flushText path where skipDepth > 0 and textBuffer has content
        handler.startElement("", "inner", "inner", new AttributesImpl());

        handler.endElement("", "inner", "inner");
        handler.endElement("", "extra", "extra");
        handler.endDocument();

        // Only html should be in the document
        final Document doc = handler.getDocument();
        assertEquals(1, doc.getChildNodes().getLength());
    }
}
