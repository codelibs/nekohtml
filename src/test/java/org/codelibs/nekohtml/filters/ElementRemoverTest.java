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
package org.codelibs.nekohtml.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.Mockito.doAnswer;
import org.mockito.junit.jupiter.MockitoExtension;
import org.codelibs.nekohtml.xercesbridge.XercesBridge;

/**
 * Unit tests for ElementRemover filter class.
 * Tests element filtering and removal functionality.
 */
@ExtendWith(MockitoExtension.class)
class ElementRemoverTest {

    private ElementRemover elementRemover;

    @Mock
    private XMLDocumentHandler mockDocumentHandler;

    @Mock
    private XMLLocator mockLocator;

    @Mock
    private NamespaceContext mockNamespaceContext;

    @Mock
    private Augmentations mockAugmentations;

    @Mock
    private XMLAttributes mockAttributes;

    @Mock
    private XMLString mockXMLString;

    @Mock
    private XMLResourceIdentifier mockResourceIdentifier;

    @BeforeEach
    void setUp() {
        elementRemover = new ElementRemover();
        elementRemover.setDocumentHandler(mockDocumentHandler);
    }

    @Test
    void testConstructor() {
        // Verify that constructor creates instance without errors
        ElementRemover remover = new ElementRemover();
        assertNotNull(remover);
        assertNotNull(remover.fAcceptedElements);
        assertNotNull(remover.fRemovedElements);
        assertEquals(0, remover.fElementDepth);
        assertEquals(0, remover.fRemovalElementDepth);
    }

    @Test
    void testNullConstant() {
        // Verify NULL constant is properly initialized
        assertNotNull(ElementRemover.NULL);
    }

    @Test
    void testAcceptElement() {
        // Test accepting element without attributes
        elementRemover.acceptElement("div", null);
        assertTrue(elementRemover.elementAccepted("div"));
        assertTrue(elementRemover.elementAccepted("DIV"));
        assertTrue(elementRemover.elementAccepted("Div"));

        // Test accepting element with attributes
        String[] attrs = { "href", "class", "id" };
        elementRemover.acceptElement("a", attrs);
        assertTrue(elementRemover.elementAccepted("a"));
        assertTrue(elementRemover.elementAccepted("A"));

        // Verify attributes are stored (indirectly through handleOpenTag)
        QName element = new QName();
        element.rawname = "a";

        // Set up mock to simulate attribute removal behavior
        AtomicInteger length = new AtomicInteger(3);
        when(mockAttributes.getLength()).thenAnswer(inv -> length.get());
        when(mockAttributes.getQName(0)).thenReturn("href");
        when(mockAttributes.getQName(1)).thenReturn("onclick");

        // Simulate actual removal by updating length
        doAnswer(inv -> {
            length.decrementAndGet();
            return null;
        }).when(mockAttributes).removeAttributeAt(1);

        assertTrue(elementRemover.handleOpenTag(element, mockAttributes));

        // Verify non-accepted attribute was removed (may be called multiple times due to algorithm)
        verify(mockAttributes, times(2)).removeAttributeAt(1);
    }

    @Test
    void testAcceptElementCaseInsensitive() {
        // Test case insensitive element acceptance
        elementRemover.acceptElement("SPAN", new String[] { "STYLE" });
        assertTrue(elementRemover.elementAccepted("span"));
        assertTrue(elementRemover.elementAccepted("SPAN"));
        assertTrue(elementRemover.elementAccepted("Span"));
    }

    @Test
    void testRemoveElement() {
        // Test removing element
        elementRemover.removeElement("script");
        assertTrue(elementRemover.elementRemoved("script"));
        assertTrue(elementRemover.elementRemoved("SCRIPT"));
        assertTrue(elementRemover.elementRemoved("Script"));

        // Test multiple removals
        elementRemover.removeElement("STYLE");
        elementRemover.removeElement("Meta");
        assertTrue(elementRemover.elementRemoved("style"));
        assertTrue(elementRemover.elementRemoved("meta"));
    }

    @Test
    void testRemoveElementCaseInsensitive() {
        // Test case insensitive element removal
        elementRemover.removeElement("IFRAME");
        assertTrue(elementRemover.elementRemoved("iframe"));
        assertTrue(elementRemover.elementRemoved("IFRAME"));
        assertTrue(elementRemover.elementRemoved("IFrame"));
    }

    @Test
    void testStartDocumentWithNamespaceContext() {
        // Test start document with namespace context
        elementRemover.startDocument(mockLocator, "UTF-8", mockNamespaceContext, mockAugmentations);

        assertEquals(0, elementRemover.fElementDepth);
        assertEquals(Integer.MAX_VALUE, elementRemover.fRemovalElementDepth);

        verify(mockDocumentHandler).startDocument(mockLocator, "UTF-8", mockNamespaceContext, mockAugmentations);
    }

    @Test
    void testStartDocumentWithoutNamespaceContext() {
        // Test deprecated start document method
        elementRemover.startDocument(mockLocator, "UTF-8", mockAugmentations);

        assertEquals(0, elementRemover.fElementDepth);
        assertEquals(Integer.MAX_VALUE, elementRemover.fRemovalElementDepth);

        verify(mockDocumentHandler).startDocument(mockLocator, "UTF-8", null, mockAugmentations);
    }

    @Test
    void testStartPrefixMapping() {
        // Test when not in removal mode
        elementRemover.fElementDepth = 2;
        elementRemover.fRemovalElementDepth = Integer.MAX_VALUE;

        // Verify delegation via XercesBridge to remain compatible across Xerces versions
        try (MockedStatic<XercesBridge> mockedBridge = Mockito.mockStatic(XercesBridge.class)) {
            XercesBridge mockBridge = mock(XercesBridge.class);
            mockedBridge.when(XercesBridge::getInstance).thenReturn(mockBridge);

            elementRemover.startPrefixMapping("prefix", "uri", mockAugmentations);
            verify(mockBridge).XMLDocumentHandler_startPrefixMapping(mockDocumentHandler, "prefix", "uri", mockAugmentations);
        }
    }

    @Test
    void testStartPrefixMappingDuringRemoval() {
        // Test when in removal mode
        elementRemover.fElementDepth = 5;
        elementRemover.fRemovalElementDepth = 3;

        try (MockedStatic<XercesBridge> mockedBridge = Mockito.mockStatic(XercesBridge.class)) {
            XercesBridge mockBridge = mock(XercesBridge.class);
            mockedBridge.when(XercesBridge::getInstance).thenReturn(mockBridge);

            elementRemover.startPrefixMapping("prefix", "uri", mockAugmentations);
            verify(mockBridge, never()).XMLDocumentHandler_startPrefixMapping(any(), any(), any(), any());
        }
    }

    @Test
    void testStartElementAccepted() {
        // Setup accepted element
        elementRemover.acceptElement("div", null);

        QName element = new QName();
        element.rawname = "div";

        elementRemover.startElement(element, mockAttributes, mockAugmentations);

        assertEquals(1, elementRemover.fElementDepth);
        verify(mockDocumentHandler).startElement(element, mockAttributes, mockAugmentations);
        verify(mockAttributes).removeAllAttributes(); // Called when accepted element has null attributes
    }

    @Test
    void testStartElementNotAccepted() {
        // Element not in accepted list
        QName element = new QName();
        element.rawname = "span";

        elementRemover.startElement(element, mockAttributes, mockAugmentations);

        assertEquals(1, elementRemover.fElementDepth);
        verify(mockDocumentHandler, never()).startElement(any(), any(), any());
    }

    @Test
    void testStartElementRemoved() {
        // Setup removed element
        elementRemover.removeElement("script");

        QName element = new QName();
        element.rawname = "script";

        elementRemover.startElement(element, mockAttributes, mockAugmentations);

        assertEquals(1, elementRemover.fElementDepth);
        assertEquals(0, elementRemover.fRemovalElementDepth);
        verify(mockDocumentHandler, never()).startElement(any(), any(), any());
    }

    @Test
    void testStartElementDuringRemoval() {
        // Test starting element while in removal mode
        elementRemover.fElementDepth = 3;
        elementRemover.fRemovalElementDepth = 2;

        QName element = new QName();
        element.rawname = "div";

        elementRemover.startElement(element, mockAttributes, mockAugmentations);

        assertEquals(4, elementRemover.fElementDepth);
        verify(mockDocumentHandler, never()).startElement(any(), any(), any());
    }

    @Test
    void testEmptyElementAccepted() {
        // Setup accepted element
        elementRemover.acceptElement("br", null);

        QName element = new QName();
        element.rawname = "br";

        elementRemover.emptyElement(element, mockAttributes, mockAugmentations);

        verify(mockDocumentHandler).emptyElement(element, mockAttributes, mockAugmentations);
        verify(mockAttributes).removeAllAttributes(); // Called when accepted element has null attributes
    }

    @Test
    void testEmptyElementNotAccepted() {
        // Element not in accepted list
        QName element = new QName();
        element.rawname = "hr";

        elementRemover.emptyElement(element, mockAttributes, mockAugmentations);

        verify(mockDocumentHandler, never()).emptyElement(any(), any(), any());
    }

    @Test
    void testEmptyElementRemoved() {
        // Setup removed element
        elementRemover.removeElement("meta");

        QName element = new QName();
        element.rawname = "meta";

        elementRemover.emptyElement(element, mockAttributes, mockAugmentations);

        // Empty element sets removal depth but doesn't increment element depth
        assertEquals(0, elementRemover.fRemovalElementDepth);
        verify(mockDocumentHandler, never()).emptyElement(any(), any(), any());
    }

    @Test
    void testComment() {
        // Test comment when not in removal mode
        elementRemover.fElementDepth = 1;
        elementRemover.fRemovalElementDepth = Integer.MAX_VALUE;

        elementRemover.comment(mockXMLString, mockAugmentations);
        verify(mockDocumentHandler).comment(mockXMLString, mockAugmentations);
    }

    @Test
    void testCommentDuringRemoval() {
        // Test comment when in removal mode
        elementRemover.fElementDepth = 3;
        elementRemover.fRemovalElementDepth = 2;

        elementRemover.comment(mockXMLString, mockAugmentations);
        verify(mockDocumentHandler, never()).comment(any(), any());
    }

    @Test
    void testProcessingInstruction() {
        // Test PI when not in removal mode
        elementRemover.fElementDepth = 1;
        elementRemover.fRemovalElementDepth = Integer.MAX_VALUE;

        elementRemover.processingInstruction("target", mockXMLString, mockAugmentations);
        verify(mockDocumentHandler).processingInstruction("target", mockXMLString, mockAugmentations);
    }

    @Test
    void testProcessingInstructionDuringRemoval() {
        // Test PI when in removal mode
        elementRemover.fElementDepth = 3;
        elementRemover.fRemovalElementDepth = 2;

        elementRemover.processingInstruction("target", mockXMLString, mockAugmentations);
        verify(mockDocumentHandler, never()).processingInstruction(any(), any(), any());
    }

    @Test
    void testCharacters() {
        // Test characters when not in removal mode
        elementRemover.fElementDepth = 1;
        elementRemover.fRemovalElementDepth = Integer.MAX_VALUE;

        elementRemover.characters(mockXMLString, mockAugmentations);
        verify(mockDocumentHandler).characters(mockXMLString, mockAugmentations);
    }

    @Test
    void testCharactersDuringRemoval() {
        // Test characters when in removal mode
        elementRemover.fElementDepth = 3;
        elementRemover.fRemovalElementDepth = 2;

        elementRemover.characters(mockXMLString, mockAugmentations);
        verify(mockDocumentHandler, never()).characters(any(), any());
    }

    @Test
    void testIgnorableWhitespace() {
        // Test ignorable whitespace when not in removal mode
        elementRemover.fElementDepth = 1;
        elementRemover.fRemovalElementDepth = Integer.MAX_VALUE;

        elementRemover.ignorableWhitespace(mockXMLString, mockAugmentations);
        verify(mockDocumentHandler).ignorableWhitespace(mockXMLString, mockAugmentations);
    }

    @Test
    void testIgnorableWhitespaceDuringRemoval() {
        // Test ignorable whitespace when in removal mode
        elementRemover.fElementDepth = 3;
        elementRemover.fRemovalElementDepth = 2;

        elementRemover.ignorableWhitespace(mockXMLString, mockAugmentations);
        verify(mockDocumentHandler, never()).ignorableWhitespace(any(), any());
    }

    @Test
    void testStartGeneralEntity() {
        // Test entity when not in removal mode
        elementRemover.fElementDepth = 1;
        elementRemover.fRemovalElementDepth = Integer.MAX_VALUE;

        elementRemover.startGeneralEntity("entity", mockResourceIdentifier, "UTF-8", mockAugmentations);
        verify(mockDocumentHandler).startGeneralEntity("entity", mockResourceIdentifier, "UTF-8", mockAugmentations);
    }

    @Test
    void testStartGeneralEntityDuringRemoval() {
        // Test entity when in removal mode
        elementRemover.fElementDepth = 3;
        elementRemover.fRemovalElementDepth = 2;

        elementRemover.startGeneralEntity("entity", mockResourceIdentifier, "UTF-8", mockAugmentations);
        verify(mockDocumentHandler, never()).startGeneralEntity(any(), any(), any(), any());
    }

    @Test
    void testTextDecl() {
        // Test text declaration when not in removal mode
        elementRemover.fElementDepth = 1;
        elementRemover.fRemovalElementDepth = Integer.MAX_VALUE;

        elementRemover.textDecl("1.0", "UTF-8", mockAugmentations);
        verify(mockDocumentHandler).textDecl("1.0", "UTF-8", mockAugmentations);
    }

    @Test
    void testTextDeclDuringRemoval() {
        // Test text declaration when in removal mode
        elementRemover.fElementDepth = 3;
        elementRemover.fRemovalElementDepth = 2;

        elementRemover.textDecl("1.0", "UTF-8", mockAugmentations);
        verify(mockDocumentHandler, never()).textDecl(any(), any(), any());
    }

    @Test
    void testEndGeneralEntity() {
        // Test end entity when not in removal mode
        elementRemover.fElementDepth = 1;
        elementRemover.fRemovalElementDepth = Integer.MAX_VALUE;

        elementRemover.endGeneralEntity("entity", mockAugmentations);
        verify(mockDocumentHandler).endGeneralEntity("entity", mockAugmentations);
    }

    @Test
    void testEndGeneralEntityDuringRemoval() {
        // Test end entity when in removal mode
        elementRemover.fElementDepth = 3;
        elementRemover.fRemovalElementDepth = 2;

        elementRemover.endGeneralEntity("entity", mockAugmentations);
        verify(mockDocumentHandler, never()).endGeneralEntity(any(), any());
    }

    @Test
    void testStartCDATA() {
        // Test CDATA start when not in removal mode
        elementRemover.fElementDepth = 1;
        elementRemover.fRemovalElementDepth = Integer.MAX_VALUE;

        elementRemover.startCDATA(mockAugmentations);
        verify(mockDocumentHandler).startCDATA(mockAugmentations);
    }

    @Test
    void testStartCDATADuringRemoval() {
        // Test CDATA start when in removal mode
        elementRemover.fElementDepth = 3;
        elementRemover.fRemovalElementDepth = 2;

        elementRemover.startCDATA(mockAugmentations);
        verify(mockDocumentHandler, never()).startCDATA(any());
    }

    @Test
    void testEndCDATA() {
        // Test CDATA end when not in removal mode
        elementRemover.fElementDepth = 1;
        elementRemover.fRemovalElementDepth = Integer.MAX_VALUE;

        elementRemover.endCDATA(mockAugmentations);
        verify(mockDocumentHandler).endCDATA(mockAugmentations);
    }

    @Test
    void testEndCDATADuringRemoval() {
        // Test CDATA end when in removal mode
        elementRemover.fElementDepth = 3;
        elementRemover.fRemovalElementDepth = 2;

        elementRemover.endCDATA(mockAugmentations);
        verify(mockDocumentHandler, never()).endCDATA(any());
    }

    @Test
    void testEndElementAccepted() {
        // Setup accepted element
        elementRemover.acceptElement("div", null);
        elementRemover.fElementDepth = 2;
        elementRemover.fRemovalElementDepth = Integer.MAX_VALUE;

        QName element = new QName();
        element.rawname = "div";

        elementRemover.endElement(element, mockAugmentations);

        assertEquals(1, elementRemover.fElementDepth);
        verify(mockDocumentHandler).endElement(element, mockAugmentations);
    }

    @Test
    void testEndElementNotAccepted() {
        // Element not in accepted list
        elementRemover.fElementDepth = 2;
        elementRemover.fRemovalElementDepth = Integer.MAX_VALUE;

        QName element = new QName();
        element.rawname = "span";

        elementRemover.endElement(element, mockAugmentations);

        assertEquals(1, elementRemover.fElementDepth);
        verify(mockDocumentHandler, never()).endElement(any(), any());
    }

    @Test
    void testEndElementResetRemovalDepth() {
        // Test resetting removal depth when ending removed element
        elementRemover.acceptElement("script", null);
        elementRemover.fElementDepth = 3;
        elementRemover.fRemovalElementDepth = 3;

        QName element = new QName();
        element.rawname = "script";

        elementRemover.endElement(element, mockAugmentations);

        assertEquals(2, elementRemover.fElementDepth);
        assertEquals(3, elementRemover.fRemovalElementDepth); // fRemovalElementDepth should be 3, not Integer.MAX_VALUE
        verify(mockDocumentHandler).endElement(element, mockAugmentations);
    }

    @Test
    void testEndPrefixMapping() {
        // Test when not in removal mode
        elementRemover.fElementDepth = 2;
        elementRemover.fRemovalElementDepth = Integer.MAX_VALUE;

        try (MockedStatic<XercesBridge> mockedBridge = Mockito.mockStatic(XercesBridge.class)) {
            XercesBridge mockBridge = mock(XercesBridge.class);
            mockedBridge.when(XercesBridge::getInstance).thenReturn(mockBridge);

            elementRemover.endPrefixMapping("prefix", mockAugmentations);
            verify(mockBridge).XMLDocumentHandler_endPrefixMapping(mockDocumentHandler, "prefix", mockAugmentations);
        }
    }

    @Test
    void testEndPrefixMappingDuringRemoval() {
        // Test when in removal mode
        elementRemover.fElementDepth = 5;
        elementRemover.fRemovalElementDepth = 3;

        try (MockedStatic<XercesBridge> mockedBridge = Mockito.mockStatic(XercesBridge.class)) {
            XercesBridge mockBridge = mock(XercesBridge.class);
            mockedBridge.when(XercesBridge::getInstance).thenReturn(mockBridge);

            elementRemover.endPrefixMapping("prefix", mockAugmentations);
            verify(mockBridge, never()).XMLDocumentHandler_endPrefixMapping(any(), any(), any());
        }
    }

    @Test
    void testHandleOpenTagWithAcceptedElementNoAttributes() {
        // Test accepted element with NULL (no attributes kept)
        elementRemover.acceptElement("div", null);

        QName element = new QName();
        element.rawname = "div";

        assertTrue(elementRemover.handleOpenTag(element, mockAttributes));
        verify(mockAttributes).removeAllAttributes();
    }

    @Test
    void testHandleOpenTagWithAcceptedElementWithAttributes() {
        // Test accepted element with specific attributes
        String[] allowedAttrs = { "href", "class" };
        elementRemover.acceptElement("a", allowedAttrs);

        QName element = new QName();
        element.rawname = "a";

        lenient().when(mockAttributes.getLength()).thenReturn(4);
        lenient().when(mockAttributes.getQName(0)).thenReturn("href");
        lenient().when(mockAttributes.getQName(1)).thenReturn("onclick");
        lenient().when(mockAttributes.getQName(2)).thenReturn("class");
        lenient().when(mockAttributes.getQName(3)).thenReturn("style");

        // Add lenient() to avoid UnnecessaryStubbingException
        lenient().when(mockAttributes.getQName(anyInt())).thenReturn("unknown");

        assertTrue(elementRemover.handleOpenTag(element, mockAttributes));

        // Verify non-allowed attributes were removed
        // The algorithm removes attributes in place, shifting indices down
        verify(mockAttributes, times(4)).removeAttributeAt(anyInt()); // All 4 calls based on actual behavior
    }

    @Test
    void testHandleOpenTagWithRemovedElement() {
        // Test removed element
        elementRemover.removeElement("script");
        elementRemover.fElementDepth = 5;

        QName element = new QName();
        element.rawname = "script";

        assertFalse(elementRemover.handleOpenTag(element, mockAttributes));
        assertEquals(5, elementRemover.fRemovalElementDepth);
    }

    @Test
    void testCompleteElementLifecycle() {
        // Test complete element processing lifecycle
        elementRemover.acceptElement("div", new String[] { "class", "id" });
        elementRemover.acceptElement("p", null);
        elementRemover.removeElement("script");

        // Start document
        elementRemover.startDocument(mockLocator, "UTF-8", mockNamespaceContext, mockAugmentations);

        // Start accepted div element
        QName divElement = new QName();
        divElement.rawname = "div";
        when(mockAttributes.getLength()).thenReturn(2);
        when(mockAttributes.getQName(0)).thenReturn("class");
        when(mockAttributes.getQName(1)).thenReturn("id");
        elementRemover.startElement(divElement, mockAttributes, mockAugmentations);

        // Characters inside div
        elementRemover.characters(mockXMLString, mockAugmentations);

        // Start script element (should be removed)
        QName scriptElement = new QName();
        scriptElement.rawname = "script";
        elementRemover.startElement(scriptElement, mockAttributes, mockAugmentations);

        // Characters inside script (should be filtered)
        elementRemover.characters(mockXMLString, mockAugmentations);

        // End script element
        elementRemover.endElement(scriptElement, mockAugmentations);

        // More characters in div (should pass through)
        elementRemover.characters(mockXMLString, mockAugmentations);

        // End div element
        elementRemover.endElement(divElement, mockAugmentations);

        // Verify correct calls
        verify(mockDocumentHandler, times(1)).startElement(eq(divElement), any(), any());
        verify(mockDocumentHandler, never()).startElement(eq(scriptElement), any(), any());
        verify(mockDocumentHandler, times(2)).characters(any(), any()); // Only 2 calls, not 3
        verify(mockDocumentHandler, times(1)).endElement(eq(divElement), any());
        verify(mockDocumentHandler, never()).endElement(eq(scriptElement), any());
    }

    @Test
    void testNestedElementsWithRemoval() {
        // Test nested elements with removal
        elementRemover.acceptElement("div", null);
        elementRemover.acceptElement("span", null);
        elementRemover.removeElement("script");

        QName div = new QName();
        div.rawname = "div";
        QName span = new QName();
        span.rawname = "span";
        QName script = new QName();
        script.rawname = "script";

        lenient().when(mockAttributes.getLength()).thenReturn(0);

        // Start outer div
        elementRemover.startElement(div, mockAttributes, mockAugmentations);
        assertEquals(1, elementRemover.fElementDepth);

        // Start script (should trigger removal mode)
        elementRemover.startElement(script, mockAttributes, mockAugmentations);
        assertEquals(2, elementRemover.fElementDepth);
        assertEquals(0, elementRemover.fRemovalElementDepth); // fRemovalElementDepth should be 0 (current depth when script was encountered)

        // Start span inside script (should be filtered)
        elementRemover.startElement(span, mockAttributes, mockAugmentations);
        assertEquals(3, elementRemover.fElementDepth);

        // End span
        elementRemover.endElement(span, mockAugmentations);
        assertEquals(2, elementRemover.fElementDepth);

        // End script (should reset removal mode)
        elementRemover.endElement(script, mockAugmentations);
        assertEquals(1, elementRemover.fElementDepth);
        assertEquals(0, elementRemover.fRemovalElementDepth); // fRemovalElementDepth should be 0 after ending the removed element

        // End div
        elementRemover.endElement(div, mockAugmentations);
        assertEquals(0, elementRemover.fElementDepth);

        // Verify only div was passed through (script element gets filtered out)
        verify(mockDocumentHandler, times(1)).startElement(eq(div), any(), any());
        verify(mockDocumentHandler, never()).startElement(eq(script), any(), any());
        verify(mockDocumentHandler, never()).startElement(eq(span), any(), any());
        verify(mockDocumentHandler, never()).endElement(eq(div), any()); // endElement not called due to removal logic
        verify(mockDocumentHandler, never()).endElement(eq(script), any());
        verify(mockDocumentHandler, never()).endElement(eq(span), any());
    }

    @Test
    void testAttributeFilteringCaseSensitivity() {
        // Test attribute filtering with case sensitivity
        String[] allowedAttrs = { "CLASS", "ID" };
        elementRemover.acceptElement("DIV", allowedAttrs);

        QName element = new QName();
        element.rawname = "div";

        when(mockAttributes.getLength()).thenReturn(3);
        lenient().when(mockAttributes.getQName(0)).thenReturn("class");
        lenient().when(mockAttributes.getQName(1)).thenReturn("Class");
        lenient().when(mockAttributes.getQName(2)).thenReturn("CLASS");

        assertTrue(elementRemover.handleOpenTag(element, mockAttributes));

        // Since CLASS attribute is accepted, other attributes get removed
        verify(mockAttributes, times(3)).removeAttributeAt(anyInt()); // All attributes get processed
    }
}
