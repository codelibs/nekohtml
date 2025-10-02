package org.codelibs.nekohtml.xercesbridge;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.lang.reflect.Constructor;

import org.codelibs.xerces.impl.Version;
import org.codelibs.xerces.xni.Augmentations;
import org.codelibs.xerces.xni.NamespaceContext;
import org.codelibs.xerces.xni.XMLDocumentHandler;
import org.codelibs.xerces.xni.XMLLocator;
import org.codelibs.xerces.xni.parser.XMLDocumentFilter;
import org.codelibs.xerces.xni.parser.XMLDocumentSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link XercesBridge_2_2}.
 *
 * These tests instantiate the bridge via reflection to ensure we exercise
 * the 2.2-specific implementation regardless of the Xerces version on the classpath.
 */
public class XercesBridge_2_2Test {

    /**
     * Helper to create a new instance using the protected constructor.
     */
    private XercesBridge_2_2 newBridge() throws Exception {
        final Constructor<XercesBridge_2_2> ctor = XercesBridge_2_2.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    @Test
    @DisplayName("Constructor does not throw and getVersion matches Xerces")
    void constructorAndGetVersion() throws Exception {
        // When creating an instance, the constructor calls getVersion() and should not throw.
        final XercesBridge_2_2 bridge = newBridge();

        // Then: getVersion returns the same value as Xerces' Version utility.
        final String versionFromBridge = bridge.getVersion();
        final String versionFromXerces = Version.getVersion();

        assertNotNull(versionFromBridge, "Bridge version should not be null");
        assertFalse(versionFromBridge.isEmpty(), "Bridge version should not be empty");
        assertEquals(versionFromXerces, versionFromBridge, "Bridge should delegate to Xerces Version.getVersion()");
    }

    @Test
    @DisplayName("startDocument delegates to XMLDocumentHandler.startDocument")
    void startDocumentDelegation() throws Exception {
        final XercesBridge_2_2 bridge = newBridge();

        final XMLDocumentHandler handler = mock(XMLDocumentHandler.class);
        final XMLLocator locator = mock(XMLLocator.class);
        final NamespaceContext nsContext = mock(NamespaceContext.class);
        final Augmentations augs = mock(Augmentations.class);

        final String encoding = "UTF-8";

        // When
        bridge.XMLDocumentHandler_startDocument(handler, locator, encoding, nsContext, augs);

        // Then: verify direct delegation
        verify(handler, times(1)).startDocument(locator, encoding, nsContext, augs);
        verifyNoMoreInteractions(handler);
    }

    @Test
    @DisplayName("startPrefixMapping is a no-op in 2.2 bridge")
    void startPrefixMappingIsNoOp() throws Exception {
        final XercesBridge_2_2 bridge = newBridge();

        final XMLDocumentHandler handler = mock(XMLDocumentHandler.class);
        final Augmentations augs = mock(Augmentations.class);

        // When: call the method which is a no-op for this bridge
        bridge.XMLDocumentHandler_startPrefixMapping(handler, "p", "urn:test", augs);

        // Then: no interaction with the handler is expected
        verifyNoInteractions(handler);
    }

    @Test
    @DisplayName("setDocumentSource delegates to XMLDocumentFilter.setDocumentSource")
    void setDocumentSourceDelegation() throws Exception {
        final XercesBridge_2_2 bridge = newBridge();

        final XMLDocumentFilter filter = mock(XMLDocumentFilter.class);
        final XMLDocumentSource source = mock(XMLDocumentSource.class);

        // When
        bridge.XMLDocumentFilter_setDocumentSource(filter, source);

        // Then: verify direct delegation
        verify(filter, times(1)).setDocumentSource(source);
        verifyNoMoreInteractions(filter);
    }

    @Test
    @DisplayName("XMLDocumentHandler_startDocument handles null values")
    void startDocumentWithNullValues() throws Exception {
        final XercesBridge_2_2 bridge = newBridge();
        final XMLDocumentHandler handler = mock(XMLDocumentHandler.class);
        final XMLLocator locator = mock(XMLLocator.class);
        final NamespaceContext nsContext = mock(NamespaceContext.class);
        final Augmentations augs = mock(Augmentations.class);

        // Test with null locator
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_startDocument(handler, null, "UTF-8", nsContext, augs));
        verify(handler).startDocument(null, "UTF-8", nsContext, augs);

        // Test with null encoding
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_startDocument(handler, locator, null, nsContext, augs));
        verify(handler).startDocument(locator, null, nsContext, augs);

        // Test with null namespace context
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_startDocument(handler, locator, "UTF-8", null, augs));
        verify(handler).startDocument(locator, "UTF-8", null, augs);

        // Test with null augmentations
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_startDocument(handler, locator, "UTF-8", nsContext, null));
        verify(handler).startDocument(locator, "UTF-8", nsContext, null);

        // Test with null handler (should throw NPE)
        assertThrows(NullPointerException.class, () -> bridge.XMLDocumentHandler_startDocument(null, locator, "UTF-8", nsContext, augs));
    }

    @Test
    @DisplayName("XMLDocumentHandler_startPrefixMapping handles null values")
    void startPrefixMappingWithNullValues() throws Exception {
        final XercesBridge_2_2 bridge = newBridge();
        final XMLDocumentHandler handler = mock(XMLDocumentHandler.class);
        final Augmentations augs = mock(Augmentations.class);

        // All combinations of null values should work (no-op method)
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_startPrefixMapping(null, "p", "urn:test", augs));
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_startPrefixMapping(handler, null, "urn:test", augs));
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_startPrefixMapping(handler, "p", null, augs));
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_startPrefixMapping(handler, "p", "urn:test", null));
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_startPrefixMapping(null, null, null, null));

        // Still a no-op, no interactions
        verifyNoInteractions(handler);
    }

    @Test
    @DisplayName("XMLDocumentFilter_setDocumentSource handles null values")
    void setDocumentSourceWithNullValues() throws Exception {
        final XercesBridge_2_2 bridge = newBridge();
        final XMLDocumentFilter filter = mock(XMLDocumentFilter.class);
        final XMLDocumentSource source = mock(XMLDocumentSource.class);

        // Test with null source (should pass through)
        assertDoesNotThrow(() -> bridge.XMLDocumentFilter_setDocumentSource(filter, null));
        verify(filter).setDocumentSource(null);

        // Test with null filter (should throw NPE)
        assertThrows(NullPointerException.class, () -> bridge.XMLDocumentFilter_setDocumentSource(null, source));
    }

    @Test
    @DisplayName("Inherited methods from parent class")
    void inheritedMethods() throws Exception {
        final XercesBridge_2_2 bridge = newBridge();
        final XMLDocumentHandler handler = mock(XMLDocumentHandler.class);
        final NamespaceContext nsContext = mock(NamespaceContext.class);
        final Augmentations augs = mock(Augmentations.class);

        // endPrefixMapping is inherited from XercesBridge (no-op)
        assertDoesNotThrow(() -> bridge.XMLDocumentHandler_endPrefixMapping(handler, "p", augs));
        verifyNoInteractions(handler);

        // NamespaceContext_declarePrefix is inherited from XercesBridge (no-op in base)
        assertDoesNotThrow(() -> bridge.NamespaceContext_declarePrefix(nsContext, "p", "urn:test"));
        // Base class implementation is no-op, so no interactions expected
        verifyNoInteractions(nsContext);
    }
}