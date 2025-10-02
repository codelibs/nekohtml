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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.codelibs.nekohtml.HTMLEventInfo;
import org.codelibs.xerces.xni.Augmentations;
import org.codelibs.xerces.xni.QName;
import org.codelibs.xerces.xni.XMLAttributes;
import org.codelibs.xerces.xni.XMLDocumentHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for Identity filter class.
 * Tests the filtering of synthesized elements from the document stream.
 */
@ExtendWith(MockitoExtension.class)
class IdentityTest {

    private Identity identityFilter;

    @Mock
    private XMLDocumentHandler mockDocumentHandler;

    @Mock
    private QName mockQName;

    @Mock
    private XMLAttributes mockAttributes;

    @Mock
    private Augmentations mockAugmentations;

    @Mock
    private HTMLEventInfo mockEventInfo;

    @BeforeEach
    void setUp() {
        identityFilter = new Identity();
        identityFilter.setDocumentHandler(mockDocumentHandler);
    }

    @Test
    void testConstructor() {
        // Verify that constructor creates instance without errors
        Identity filter = new Identity();
        assertNotNull(filter);
    }

    @Test
    void testStartElementWithNullAugmentations() {
        // When augmentations is null, element should be passed through
        identityFilter.startElement(mockQName, mockAttributes, null);

        verify(mockDocumentHandler, times(1)).startElement(mockQName, mockAttributes, null);
    }

    @Test
    void testStartElementWithNonSynthesizedContent() {
        // Setup augmentations with non-synthesized content
        when(mockAugmentations.getItem(Identity.AUGMENTATIONS)).thenReturn(mockEventInfo);
        when(mockEventInfo.isSynthesized()).thenReturn(false);

        identityFilter.startElement(mockQName, mockAttributes, mockAugmentations);

        verify(mockDocumentHandler, times(1)).startElement(mockQName, mockAttributes, mockAugmentations);
    }

    @Test
    void testStartElementWithSynthesizedContent() {
        // Setup augmentations with synthesized content
        when(mockAugmentations.getItem(Identity.AUGMENTATIONS)).thenReturn(mockEventInfo);
        when(mockEventInfo.isSynthesized()).thenReturn(true);

        identityFilter.startElement(mockQName, mockAttributes, mockAugmentations);

        // Synthesized content should be filtered out
        verify(mockDocumentHandler, never()).startElement(any(), any(), any());
    }

    @Test
    void testStartElementWithNullEventInfo() {
        // When event info is null, element should be passed through
        when(mockAugmentations.getItem(Identity.AUGMENTATIONS)).thenReturn(null);

        identityFilter.startElement(mockQName, mockAttributes, mockAugmentations);

        verify(mockDocumentHandler, times(1)).startElement(mockQName, mockAttributes, mockAugmentations);
    }

    @Test
    void testEmptyElementWithNullAugmentations() {
        // When augmentations is null, element should be passed through
        identityFilter.emptyElement(mockQName, mockAttributes, null);

        verify(mockDocumentHandler, times(1)).emptyElement(mockQName, mockAttributes, null);
    }

    @Test
    void testEmptyElementWithNonSynthesizedContent() {
        // Setup augmentations with non-synthesized content
        when(mockAugmentations.getItem(Identity.AUGMENTATIONS)).thenReturn(mockEventInfo);
        when(mockEventInfo.isSynthesized()).thenReturn(false);

        identityFilter.emptyElement(mockQName, mockAttributes, mockAugmentations);

        verify(mockDocumentHandler, times(1)).emptyElement(mockQName, mockAttributes, mockAugmentations);
    }

    @Test
    void testEmptyElementWithSynthesizedContent() {
        // Setup augmentations with synthesized content
        when(mockAugmentations.getItem(Identity.AUGMENTATIONS)).thenReturn(mockEventInfo);
        when(mockEventInfo.isSynthesized()).thenReturn(true);

        identityFilter.emptyElement(mockQName, mockAttributes, mockAugmentations);

        // Synthesized content should be filtered out
        verify(mockDocumentHandler, never()).emptyElement(any(), any(), any());
    }

    @Test
    void testEmptyElementWithNullEventInfo() {
        // When event info is null, element should be passed through
        when(mockAugmentations.getItem(Identity.AUGMENTATIONS)).thenReturn(null);

        identityFilter.emptyElement(mockQName, mockAttributes, mockAugmentations);

        verify(mockDocumentHandler, times(1)).emptyElement(mockQName, mockAttributes, mockAugmentations);
    }

    @Test
    void testEndElementWithNullAugmentations() {
        // When augmentations is null, element should be passed through
        identityFilter.endElement(mockQName, null);

        verify(mockDocumentHandler, times(1)).endElement(mockQName, null);
    }

    @Test
    void testEndElementWithNonSynthesizedContent() {
        // Setup augmentations with non-synthesized content
        when(mockAugmentations.getItem(Identity.AUGMENTATIONS)).thenReturn(mockEventInfo);
        when(mockEventInfo.isSynthesized()).thenReturn(false);

        identityFilter.endElement(mockQName, mockAugmentations);

        verify(mockDocumentHandler, times(1)).endElement(mockQName, mockAugmentations);
    }

    @Test
    void testEndElementWithSynthesizedContent() {
        // Setup augmentations with synthesized content
        when(mockAugmentations.getItem(Identity.AUGMENTATIONS)).thenReturn(mockEventInfo);
        when(mockEventInfo.isSynthesized()).thenReturn(true);

        identityFilter.endElement(mockQName, mockAugmentations);

        // Synthesized content should be filtered out
        verify(mockDocumentHandler, never()).endElement(any(), any());
    }

    @Test
    void testEndElementWithNullEventInfo() {
        // When event info is null, element should be passed through
        when(mockAugmentations.getItem(Identity.AUGMENTATIONS)).thenReturn(null);

        identityFilter.endElement(mockQName, mockAugmentations);

        verify(mockDocumentHandler, times(1)).endElement(mockQName, mockAugmentations);
    }

    @Test
    void testSynthesizedMethodWithNullAugmentations() {
        // Test the static synthesized method with null augmentations
        boolean result = Identity.synthesized(null);
        assertFalse(result);
    }

    @Test
    void testSynthesizedMethodWithNullEventInfo() {
        // Test when getItem returns null
        when(mockAugmentations.getItem(Identity.AUGMENTATIONS)).thenReturn(null);

        boolean result = Identity.synthesized(mockAugmentations);
        assertFalse(result);
    }

    @Test
    void testSynthesizedMethodWithSynthesizedEvent() {
        // Test when content is synthesized
        when(mockAugmentations.getItem(Identity.AUGMENTATIONS)).thenReturn(mockEventInfo);
        when(mockEventInfo.isSynthesized()).thenReturn(true);

        boolean result = Identity.synthesized(mockAugmentations);
        assertTrue(result);
    }

    @Test
    void testSynthesizedMethodWithNonSynthesizedEvent() {
        // Test when content is not synthesized
        when(mockAugmentations.getItem(Identity.AUGMENTATIONS)).thenReturn(mockEventInfo);
        when(mockEventInfo.isSynthesized()).thenReturn(false);

        boolean result = Identity.synthesized(mockAugmentations);
        assertFalse(result);
    }

    @Test
    void testFilterWithoutDocumentHandler() {
        // Test behavior when no document handler is set
        Identity filter = new Identity();

        // These should not throw exceptions even without a handler
        filter.startElement(mockQName, mockAttributes, null);
        filter.emptyElement(mockQName, mockAttributes, null);
        filter.endElement(mockQName, null);
    }

    @Test
    void testCompleteElementSequence() {
        // Test a complete element sequence with non-synthesized content
        when(mockAugmentations.getItem(Identity.AUGMENTATIONS)).thenReturn(mockEventInfo);
        when(mockEventInfo.isSynthesized()).thenReturn(false);

        // Simulate a complete element
        identityFilter.startElement(mockQName, mockAttributes, mockAugmentations);
        identityFilter.endElement(mockQName, mockAugmentations);

        // Verify both methods were called
        verify(mockDocumentHandler, times(1)).startElement(mockQName, mockAttributes, mockAugmentations);
        verify(mockDocumentHandler, times(1)).endElement(mockQName, mockAugmentations);
    }

    @Test
    void testCompleteElementSequenceWithSynthesized() {
        // Test a complete element sequence with synthesized content
        when(mockAugmentations.getItem(Identity.AUGMENTATIONS)).thenReturn(mockEventInfo);
        when(mockEventInfo.isSynthesized()).thenReturn(true);

        // Simulate a complete element
        identityFilter.startElement(mockQName, mockAttributes, mockAugmentations);
        identityFilter.endElement(mockQName, mockAugmentations);

        // Verify neither method was called (filtered out)
        verify(mockDocumentHandler, never()).startElement(any(), any(), any());
        verify(mockDocumentHandler, never()).endElement(any(), any());
    }

    @Test
    void testMixedSynthesizedAndNonSynthesized() {
        // Test mixing synthesized and non-synthesized elements
        Augmentations synthAugs = mock(Augmentations.class);
        HTMLEventInfo synthInfo = mock(HTMLEventInfo.class);
        when(synthAugs.getItem(Identity.AUGMENTATIONS)).thenReturn(synthInfo);
        when(synthInfo.isSynthesized()).thenReturn(true);

        Augmentations nonSynthAugs = mock(Augmentations.class);
        HTMLEventInfo nonSynthInfo = mock(HTMLEventInfo.class);
        when(nonSynthAugs.getItem(Identity.AUGMENTATIONS)).thenReturn(nonSynthInfo);
        when(nonSynthInfo.isSynthesized()).thenReturn(false);

        QName element1 = mock(QName.class);
        QName element2 = mock(QName.class);

        // Mix synthesized and non-synthesized elements
        identityFilter.startElement(element1, mockAttributes, synthAugs); // Should be filtered
        identityFilter.startElement(element2, mockAttributes, nonSynthAugs); // Should pass through
        identityFilter.endElement(element2, nonSynthAugs); // Should pass through
        identityFilter.endElement(element1, synthAugs); // Should be filtered

        // Verify only non-synthesized elements passed through
        verify(mockDocumentHandler, never()).startElement(eq(element1), any(), any());
        verify(mockDocumentHandler, times(1)).startElement(eq(element2), eq(mockAttributes), eq(nonSynthAugs));
        verify(mockDocumentHandler, times(1)).endElement(eq(element2), eq(nonSynthAugs));
        verify(mockDocumentHandler, never()).endElement(eq(element1), any());
    }

    @Test
    void testConstantValues() {
        // Verify constant values are as expected
        assertTrue(Identity.AUGMENTATIONS.equals("http://cyberneko.org/html/features/augmentations"));
        assertTrue(Identity.FILTERS.equals("http://cyberneko.org/html/properties/filters"));
    }
}