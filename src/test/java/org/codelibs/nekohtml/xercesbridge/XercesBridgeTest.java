/*
 * Tests for XercesBridge and version-specific behavior.
 *
 * Uses JUnit 5 and Mockito to validate delegation and no-op methods
 * across the bridge abstraction. Comments are in English as requested.
 */
package org.codelibs.nekohtml.xercesbridge;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLDocumentSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class XercesBridgeTest {

    @Mock
    private NamespaceContext namespaceContext;

    @Mock
    private XMLDocumentHandler documentHandler;

    @Mock
    private XMLDocumentFilter documentFilter;

    @Mock
    private XMLDocumentSource documentSource;

    @Mock
    private XMLLocator locator;

    @Mock
    private Augmentations augmentations;

    @Test
    @DisplayName("getInstance returns singleton and 2_3 bridge")
    void testGetInstanceSingletonAndClass() {
        // When
        final XercesBridge b1 = XercesBridge.getInstance();
        final XercesBridge b2 = XercesBridge.getInstance();

        // Then: singleton semantics
        assertSame(b1, b2, "XercesBridge should be a singleton instance");

        // And: with the configured Xerces version, 2_3 bridge should be selected
        assertTrue(b1.getClass().getName().endsWith("XercesBridge_2_3"),
                "Expected XercesBridge_2_3 to be selected for current Xerces version");
    }

    @Test
    @DisplayName("getVersion returns a non-empty version string")
    void testGetVersion() {
        final XercesBridge bridge = XercesBridge.getInstance();

        final String version = bridge.getVersion();
        assertNotNull(version, "Version string must not be null");
        assertFalse(version.isBlank(), "Version string must not be blank");
        // Version typically contains "Xerces" (e.g., "Xerces-J 2.x"); keep assertion resilient.
        assertTrue(version.toLowerCase().contains("xerces"), "Version should mention Xerces");
    }

    @Test
    @DisplayName("NamespaceContext_declarePrefix delegates to NamespaceContext.declarePrefix on 2_3")
    void testNamespaceContextDeclarePrefix() {
        final XercesBridge bridge = XercesBridge.getInstance();

        bridge.NamespaceContext_declarePrefix(namespaceContext, "p", "urn:test");

        // In 2_3, the bridge delegates to NamespaceContext.declarePrefix
        verify(namespaceContext, times(1)).declarePrefix("p", "urn:test");
        verifyNoMoreInteractions(namespaceContext);
    }

    @Test
    @DisplayName("XMLDocumentHandler_startDocument delegates to handler.startDocument")
    void testXMLDocumentHandlerStartDocumentDelegation() {
        final XercesBridge bridge = XercesBridge.getInstance();

        bridge.XMLDocumentHandler_startDocument(documentHandler, locator, "UTF-8", namespaceContext, augmentations);

        verify(documentHandler, times(1)).startDocument(locator, "UTF-8", namespaceContext, augmentations);
        verifyNoMoreInteractions(documentHandler);
    }

    @Test
    @DisplayName("XMLDocumentHandler_startPrefixMapping is a no-op")
    void testXMLDocumentHandlerStartPrefixMappingNoOp() {
        final XercesBridge bridge = XercesBridge.getInstance();

        // Should be a no-op; ensure it doesn't throw and doesn't touch the handler
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_startPrefixMapping(documentHandler, "p", "urn:test", augmentations));
        verifyNoInteractions(documentHandler);
    }

    @Test
    @DisplayName("XMLDocumentHandler_endPrefixMapping is a no-op")
    void testXMLDocumentHandlerEndPrefixMappingNoOp() {
        final XercesBridge bridge = XercesBridge.getInstance();

        // Should be a no-op; ensure it doesn't throw and doesn't touch the handler
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_endPrefixMapping(documentHandler, "p", augmentations));
        verifyNoInteractions(documentHandler);
    }

    @Test
    @DisplayName("XMLDocumentFilter_setDocumentSource delegates to filter.setDocumentSource")
    void testXMLDocumentFilterSetDocumentSourceDelegation() {
        final XercesBridge bridge = XercesBridge.getInstance();

        bridge.XMLDocumentFilter_setDocumentSource(documentFilter, documentSource);

        verify(documentFilter, times(1)).setDocumentSource(documentSource);
        verifyNoMoreInteractions(documentFilter);
    }

    @Test
    @DisplayName("NamespaceContext_declarePrefix handles null values gracefully")
    void testNamespaceContextDeclarePrefixWithNullValues() {
        final XercesBridge bridge = XercesBridge.getInstance();

        // Test with null prefix
        assertDoesNotThrow(() -> bridge.NamespaceContext_declarePrefix(namespaceContext, null, "urn:test"));

        // Test with null URI
        assertDoesNotThrow(() -> bridge.NamespaceContext_declarePrefix(namespaceContext, "p", null));

        // Test with null namespace context (will throw NPE as expected)
        assertThrows(NullPointerException.class, () -> bridge.NamespaceContext_declarePrefix(null, "p", "urn:test"));

        // Verify the method was called with the appropriate arguments
        verify(namespaceContext).declarePrefix(null, "urn:test");
        verify(namespaceContext).declarePrefix("p", null);
    }

    @Test
    @DisplayName("XMLDocumentHandler_startDocument handles null values gracefully")
    void testXMLDocumentHandlerStartDocumentWithNullValues() {
        final XercesBridge bridge = XercesBridge.getInstance();

        // Test with null locator
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_startDocument(documentHandler, null, "UTF-8", namespaceContext, augmentations));

        // Test with null encoding
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_startDocument(documentHandler, locator, null, namespaceContext, augmentations));

        // Test with null namespace context
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_startDocument(documentHandler, locator, "UTF-8", null, augmentations));

        // Test with null augmentations
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_startDocument(documentHandler, locator, "UTF-8", namespaceContext, null));

        // Verify all the calls were made
        verify(documentHandler).startDocument(null, "UTF-8", namespaceContext, augmentations);
        verify(documentHandler).startDocument(locator, null, namespaceContext, augmentations);
        verify(documentHandler).startDocument(locator, "UTF-8", null, augmentations);
        verify(documentHandler).startDocument(locator, "UTF-8", namespaceContext, null);
    }

    @Test
    @DisplayName("XMLDocumentHandler prefix mapping methods handle null values")
    void testXMLDocumentHandlerPrefixMappingWithNullValues() {
        final XercesBridge bridge = XercesBridge.getInstance();

        // startPrefixMapping with null values
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_startPrefixMapping(null, "p", "urn:test", augmentations));
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_startPrefixMapping(documentHandler, null, "urn:test", augmentations));
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_startPrefixMapping(documentHandler, "p", null, augmentations));
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_startPrefixMapping(documentHandler, "p", "urn:test", null));

        // endPrefixMapping with null values
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_endPrefixMapping(null, "p", augmentations));
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_endPrefixMapping(documentHandler, null, augmentations));
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_endPrefixMapping(documentHandler, "p", null));

        // These methods are no-ops, so handler should not be touched
        verifyNoInteractions(documentHandler);
    }

    @Test
    @DisplayName("XMLDocumentFilter_setDocumentSource handles null values")
    void testXMLDocumentFilterSetDocumentSourceWithNullValues() {
        final XercesBridge bridge = XercesBridge.getInstance();

        // Test with null filter (will throw NPE as expected)
        assertThrows(NullPointerException.class, () -> bridge.XMLDocumentFilter_setDocumentSource(null, documentSource));

        // Test with null source
        assertDoesNotThrow(() -> bridge.XMLDocumentFilter_setDocumentSource(documentFilter, null));

        // Verify the null source was passed to the filter
        verify(documentFilter).setDocumentSource(null);
    }

    @Test
    @DisplayName("Protected constructor is accessible to subclasses")
    void testProtectedConstructor() {
        // Create a test subclass to verify protected constructor accessibility
        class TestBridge extends XercesBridge {
            public TestBridge() {
                super(); // Calls protected constructor
            }

            @Override
            public String getVersion() {
                return "Test Version";
            }

            @Override
            public void XMLDocumentHandler_startDocument(XMLDocumentHandler documentHandler, XMLLocator locator, String encoding,
                    NamespaceContext nscontext, Augmentations augs) {
                // Test implementation
            }
        }

        // Should be able to instantiate the test bridge
        assertDoesNotThrow(() -> {
            TestBridge testBridge = new TestBridge();
            assertNotNull(testBridge);
            assertEquals("Test Version", testBridge.getVersion());
        });
    }
}
