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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.parser.XMLComponentManager;
import org.apache.xerces.xni.parser.XMLDocumentSource;
import org.codelibs.nekohtml.xercesbridge.XercesBridge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for DefaultFilter class.
 */
@ExtendWith(MockitoExtension.class)
class DefaultFilterTest {

    private DefaultFilter filter;

    @Mock
    private XMLDocumentHandler mockDocumentHandler;

    @Mock
    private XMLDocumentSource mockDocumentSource;

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
    private QName mockQName;

    @Mock
    private XMLResourceIdentifier mockResourceIdentifier;

    @Mock
    private XMLComponentManager mockComponentManager;

    @BeforeEach
    void setUp() {
        filter = new DefaultFilter();
    }

    @Test
    void testConstructor() {
        // Verify default constructor creates instance without errors
        DefaultFilter newFilter = new DefaultFilter();
        assertNull(newFilter.getDocumentHandler());
        assertNull(newFilter.getDocumentSource());
    }

    @Test
    void testSetAndGetDocumentHandler() {
        // Test setting and getting document handler
        filter.setDocumentHandler(mockDocumentHandler);
        assertEquals(mockDocumentHandler, filter.getDocumentHandler());

        // Test setting null
        filter.setDocumentHandler(null);
        assertNull(filter.getDocumentHandler());
    }

    @Test
    void testSetAndGetDocumentSource() {
        // Test setting and getting document source
        filter.setDocumentSource(mockDocumentSource);
        assertEquals(mockDocumentSource, filter.getDocumentSource());

        // Test setting null
        filter.setDocumentSource(null);
        assertNull(filter.getDocumentSource());
    }

    @Test
    void testStartDocumentWithNamespaceContext() {
        // Test with document handler set
        filter.setDocumentHandler(mockDocumentHandler);

        try (MockedStatic<XercesBridge> mockedBridge = Mockito.mockStatic(XercesBridge.class)) {
            XercesBridge mockBridge = mock(XercesBridge.class);
            mockedBridge.when(XercesBridge::getInstance).thenReturn(mockBridge);

            filter.startDocument(mockLocator, "UTF-8", mockNamespaceContext, mockAugmentations);

            verify(mockBridge).XMLDocumentHandler_startDocument(mockDocumentHandler, mockLocator, "UTF-8", mockNamespaceContext,
                    mockAugmentations);
        }
    }

    @Test
    void testStartDocumentWithNamespaceContextNoHandler() {
        // Test without document handler - should not throw exception
        filter.startDocument(mockLocator, "UTF-8", mockNamespaceContext, mockAugmentations);
        // No exception should be thrown
    }

    @Test
    void testStartDocumentWithoutNamespaceContext() {
        // Test deprecated method that calls the new method with null namespace context
        filter.setDocumentHandler(mockDocumentHandler);

        try (MockedStatic<XercesBridge> mockedBridge = Mockito.mockStatic(XercesBridge.class)) {
            XercesBridge mockBridge = mock(XercesBridge.class);
            mockedBridge.when(XercesBridge::getInstance).thenReturn(mockBridge);

            filter.startDocument(mockLocator, "UTF-8", mockAugmentations);

            verify(mockBridge).XMLDocumentHandler_startDocument(mockDocumentHandler, mockLocator, "UTF-8", null, mockAugmentations);
        }
    }

    @Test
    void testXmlDecl() {
        // Test with handler
        filter.setDocumentHandler(mockDocumentHandler);
        filter.xmlDecl("1.0", "UTF-8", "yes", mockAugmentations);
        verify(mockDocumentHandler).xmlDecl("1.0", "UTF-8", "yes", mockAugmentations);

        // Test without handler
        filter.setDocumentHandler(null);
        filter.xmlDecl("1.0", "UTF-8", "yes", mockAugmentations);
        // Should not throw exception
    }

    @Test
    void testDoctypeDecl() {
        // Test with handler
        filter.setDocumentHandler(mockDocumentHandler);
        filter.doctypeDecl("html", "publicId", "systemId", mockAugmentations);
        verify(mockDocumentHandler).doctypeDecl("html", "publicId", "systemId", mockAugmentations);

        // Test without handler
        filter.setDocumentHandler(null);
        filter.doctypeDecl("html", "publicId", "systemId", mockAugmentations);
        // Should not throw exception
    }

    @Test
    void testComment() {
        // Test with handler
        filter.setDocumentHandler(mockDocumentHandler);
        filter.comment(mockXMLString, mockAugmentations);
        verify(mockDocumentHandler).comment(mockXMLString, mockAugmentations);

        // Test without handler
        filter.setDocumentHandler(null);
        filter.comment(mockXMLString, mockAugmentations);
        // Should not throw exception
    }

    @Test
    void testProcessingInstruction() {
        // Test with handler
        filter.setDocumentHandler(mockDocumentHandler);
        filter.processingInstruction("target", mockXMLString, mockAugmentations);
        verify(mockDocumentHandler).processingInstruction("target", mockXMLString, mockAugmentations);

        // Test without handler
        filter.setDocumentHandler(null);
        filter.processingInstruction("target", mockXMLString, mockAugmentations);
        // Should not throw exception
    }

    @Test
    void testStartElement() {
        // Test with handler
        filter.setDocumentHandler(mockDocumentHandler);
        filter.startElement(mockQName, mockAttributes, mockAugmentations);
        verify(mockDocumentHandler).startElement(mockQName, mockAttributes, mockAugmentations);

        // Test without handler
        filter.setDocumentHandler(null);
        filter.startElement(mockQName, mockAttributes, mockAugmentations);
        // Should not throw exception
    }

    @Test
    void testEmptyElement() {
        // Test with handler
        filter.setDocumentHandler(mockDocumentHandler);
        filter.emptyElement(mockQName, mockAttributes, mockAugmentations);
        verify(mockDocumentHandler).emptyElement(mockQName, mockAttributes, mockAugmentations);

        // Test without handler
        filter.setDocumentHandler(null);
        filter.emptyElement(mockQName, mockAttributes, mockAugmentations);
        // Should not throw exception
    }

    @Test
    void testCharacters() {
        // Test with handler
        filter.setDocumentHandler(mockDocumentHandler);
        filter.characters(mockXMLString, mockAugmentations);
        verify(mockDocumentHandler).characters(mockXMLString, mockAugmentations);

        // Test without handler
        filter.setDocumentHandler(null);
        filter.characters(mockXMLString, mockAugmentations);
        // Should not throw exception
    }

    @Test
    void testIgnorableWhitespace() {
        // Test with handler
        filter.setDocumentHandler(mockDocumentHandler);
        filter.ignorableWhitespace(mockXMLString, mockAugmentations);
        verify(mockDocumentHandler).ignorableWhitespace(mockXMLString, mockAugmentations);

        // Test without handler
        filter.setDocumentHandler(null);
        filter.ignorableWhitespace(mockXMLString, mockAugmentations);
        // Should not throw exception
    }

    @Test
    void testStartGeneralEntity() {
        // Test with handler
        filter.setDocumentHandler(mockDocumentHandler);
        filter.startGeneralEntity("entity", mockResourceIdentifier, "UTF-8", mockAugmentations);
        verify(mockDocumentHandler).startGeneralEntity("entity", mockResourceIdentifier, "UTF-8", mockAugmentations);

        // Test without handler
        filter.setDocumentHandler(null);
        filter.startGeneralEntity("entity", mockResourceIdentifier, "UTF-8", mockAugmentations);
        // Should not throw exception
    }

    @Test
    void testTextDecl() {
        // Test with handler
        filter.setDocumentHandler(mockDocumentHandler);
        filter.textDecl("1.0", "UTF-8", mockAugmentations);
        verify(mockDocumentHandler).textDecl("1.0", "UTF-8", mockAugmentations);

        // Test without handler
        filter.setDocumentHandler(null);
        filter.textDecl("1.0", "UTF-8", mockAugmentations);
        // Should not throw exception
    }

    @Test
    void testEndGeneralEntity() {
        // Test with handler
        filter.setDocumentHandler(mockDocumentHandler);
        filter.endGeneralEntity("entity", mockAugmentations);
        verify(mockDocumentHandler).endGeneralEntity("entity", mockAugmentations);

        // Test without handler
        filter.setDocumentHandler(null);
        filter.endGeneralEntity("entity", mockAugmentations);
        // Should not throw exception
    }

    @Test
    void testStartCDATA() {
        // Test with handler
        filter.setDocumentHandler(mockDocumentHandler);
        filter.startCDATA(mockAugmentations);
        verify(mockDocumentHandler).startCDATA(mockAugmentations);

        // Test without handler
        filter.setDocumentHandler(null);
        filter.startCDATA(mockAugmentations);
        // Should not throw exception
    }

    @Test
    void testEndCDATA() {
        // Test with handler
        filter.setDocumentHandler(mockDocumentHandler);
        filter.endCDATA(mockAugmentations);
        verify(mockDocumentHandler).endCDATA(mockAugmentations);

        // Test without handler
        filter.setDocumentHandler(null);
        filter.endCDATA(mockAugmentations);
        // Should not throw exception
    }

    @Test
    void testEndElement() {
        // Test with handler
        filter.setDocumentHandler(mockDocumentHandler);
        filter.endElement(mockQName, mockAugmentations);
        verify(mockDocumentHandler).endElement(mockQName, mockAugmentations);

        // Test without handler
        filter.setDocumentHandler(null);
        filter.endElement(mockQName, mockAugmentations);
        // Should not throw exception
    }

    @Test
    void testEndDocument() {
        // Test with handler
        filter.setDocumentHandler(mockDocumentHandler);
        filter.endDocument(mockAugmentations);
        verify(mockDocumentHandler).endDocument(mockAugmentations);

        // Test without handler
        filter.setDocumentHandler(null);
        filter.endDocument(mockAugmentations);
        // Should not throw exception
    }

    @Test
    void testStartPrefixMapping() {
        filter.setDocumentHandler(mockDocumentHandler);

        try (MockedStatic<XercesBridge> mockedBridge = Mockito.mockStatic(XercesBridge.class)) {
            XercesBridge mockBridge = mock(XercesBridge.class);
            mockedBridge.when(XercesBridge::getInstance).thenReturn(mockBridge);

            filter.startPrefixMapping("prefix", "uri", mockAugmentations);

            verify(mockBridge).XMLDocumentHandler_startPrefixMapping(mockDocumentHandler, "prefix", "uri", mockAugmentations);
        }
    }

    @Test
    void testStartPrefixMappingNoHandler() {
        // Test without handler - should not throw exception
        filter.startPrefixMapping("prefix", "uri", mockAugmentations);
        // No exception should be thrown
    }

    @Test
    void testEndPrefixMapping() {
        filter.setDocumentHandler(mockDocumentHandler);

        try (MockedStatic<XercesBridge> mockedBridge = Mockito.mockStatic(XercesBridge.class)) {
            XercesBridge mockBridge = mock(XercesBridge.class);
            mockedBridge.when(XercesBridge::getInstance).thenReturn(mockBridge);

            filter.endPrefixMapping("prefix", mockAugmentations);

            verify(mockBridge).XMLDocumentHandler_endPrefixMapping(mockDocumentHandler, "prefix", mockAugmentations);
        }
    }

    @Test
    void testEndPrefixMappingNoHandler() {
        // Test without handler - should not throw exception
        filter.endPrefixMapping("prefix", mockAugmentations);
        // No exception should be thrown
    }

    @Test
    void testGetRecognizedFeatures() {
        // Default implementation returns null
        assertNull(filter.getRecognizedFeatures());
    }

    @Test
    void testGetFeatureDefault() {
        // Default implementation returns null
        assertNull(filter.getFeatureDefault("feature.id"));
    }

    @Test
    void testGetRecognizedProperties() {
        // Default implementation returns null
        assertNull(filter.getRecognizedProperties());
    }

    @Test
    void testGetPropertyDefault() {
        // Default implementation returns null
        assertNull(filter.getPropertyDefault("property.id"));
    }

    @Test
    void testReset() {
        // Default implementation does nothing - just verify no exception
        filter.reset(mockComponentManager);
    }

    @Test
    void testSetFeature() {
        // Default implementation does nothing - just verify no exception
        filter.setFeature("feature.id", true);
        filter.setFeature("feature.id", false);
    }

    @Test
    void testSetProperty() {
        // Default implementation does nothing - just verify no exception
        filter.setProperty("property.id", "value");
        filter.setProperty("property.id", null);
    }

    @Test
    void testMergeIdenticalArrays() {
        // Test when both arrays are the same reference
        String[] array = { "a", "b", "c" };
        String[] result = DefaultFilter.merge(array, array);
        assertEquals(array, result);
    }

    @Test
    void testMergeFirstArrayNull() {
        // Test when first array is null
        String[] array2 = { "d", "e", "f" };
        String[] result = DefaultFilter.merge(null, array2);
        assertArrayEquals(array2, result);
    }

    @Test
    void testMergeSecondArrayNull() {
        // Test when second array is null
        String[] array1 = { "a", "b", "c" };
        String[] result = DefaultFilter.merge(array1, null);
        assertArrayEquals(array1, result);
    }

    @Test
    void testMergeBothArraysNull() {
        // Test when both arrays are null
        String[] result = DefaultFilter.merge(null, null);
        assertNull(result);
    }

    @Test
    void testMergeTwoDistinctArrays() {
        // Test merging two distinct arrays
        String[] array1 = { "a", "b", "c" };
        String[] array2 = { "d", "e", "f" };
        String[] result = DefaultFilter.merge(array1, array2);

        String[] expected = { "a", "b", "c", "d", "e", "f" };
        assertArrayEquals(expected, result);
    }

    @Test
    void testMergeEmptyArrays() {
        // Test merging empty arrays
        String[] array1 = {};
        String[] array2 = {};
        String[] result = DefaultFilter.merge(array1, array2);

        assertEquals(0, result.length);
    }

    @Test
    void testMergeOneEmptyOneNonEmpty() {
        // Test merging empty array with non-empty array
        String[] array1 = {};
        String[] array2 = { "a", "b" };
        String[] result = DefaultFilter.merge(array1, array2);

        assertArrayEquals(array2, result);
    }

    @Test
    void testMergeWithDuplicateValues() {
        // Test that duplicate values are preserved (not deduplicated)
        String[] array1 = { "a", "b" };
        String[] array2 = { "b", "c" };
        String[] result = DefaultFilter.merge(array1, array2);

        String[] expected = { "a", "b", "b", "c" };
        assertArrayEquals(expected, result);
    }

    @Test
    void testDocumentHandlerChaining() {
        // Verify that setting a handler and then calling methods properly delegates
        filter.setDocumentHandler(mockDocumentHandler);

        // Call multiple methods to verify chaining works
        filter.startElement(mockQName, mockAttributes, mockAugmentations);
        filter.characters(mockXMLString, mockAugmentations);
        filter.endElement(mockQName, mockAugmentations);

        // Verify all calls were delegated
        verify(mockDocumentHandler).startElement(mockQName, mockAttributes, mockAugmentations);
        verify(mockDocumentHandler).characters(mockXMLString, mockAugmentations);
        verify(mockDocumentHandler).endElement(mockQName, mockAugmentations);
    }

    @Test
    void testNullParameterHandling() {
        // Test that null parameters are properly passed through
        filter.setDocumentHandler(mockDocumentHandler);

        filter.xmlDecl(null, null, null, null);
        verify(mockDocumentHandler).xmlDecl(null, null, null, null);

        filter.doctypeDecl(null, null, null, null);
        verify(mockDocumentHandler).doctypeDecl(null, null, null, null);

        filter.processingInstruction(null, null, null);
        verify(mockDocumentHandler).processingInstruction(null, null, null);
    }
}