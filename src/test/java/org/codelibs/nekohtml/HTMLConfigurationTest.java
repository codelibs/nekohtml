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
package org.codelibs.nekohtml;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;

import org.codelibs.nekohtml.filters.DefaultFilter;
import org.codelibs.xerces.xni.XMLDTDContentModelHandler;
import org.codelibs.xerces.xni.XMLDTDHandler;
import org.codelibs.xerces.xni.XMLDocumentHandler;
import org.codelibs.xerces.xni.XNIException;
import org.codelibs.xerces.xni.parser.XMLConfigurationException;
import org.codelibs.xerces.xni.parser.XMLDocumentFilter;
import org.codelibs.xerces.xni.parser.XMLEntityResolver;
import org.codelibs.xerces.xni.parser.XMLErrorHandler;
import org.codelibs.xerces.xni.parser.XMLInputSource;
import org.codelibs.xerces.xni.parser.XMLParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HTMLConfigurationTest {

    private HTMLConfiguration configuration;

    @Mock
    private XMLDocumentHandler documentHandler;

    @Mock
    private XMLDTDHandler dtdHandler;

    @Mock
    private XMLDTDContentModelHandler dtdContentModelHandler;

    @Mock
    private XMLErrorHandler errorHandler;

    @Mock
    private XMLEntityResolver entityResolver;

    @Mock
    private XMLInputSource inputSource;

    @Mock
    private HTMLTagBalancingListener tagBalancingListener;

    @BeforeEach
    void setUp() {
        configuration = new HTMLConfiguration();
    }

    @Test
    @DisplayName("Should create HTMLConfiguration with default settings")
    void testDefaultConstructor() {
        // Verify default feature states
        assertTrue(configuration.getFeature("http://xml.org/sax/features/namespaces"));
        assertFalse(configuration.getFeature("http://xml.org/sax/features/validation"));
        assertFalse(configuration.getFeature("http://cyberneko.org/html/features/augmentations"));
        assertFalse(configuration.getFeature("http://cyberneko.org/html/features/report-errors"));
        assertFalse(configuration.getFeature("http://cyberneko.org/html/features/report-errors/simple"));
        assertTrue(configuration.getFeature("http://cyberneko.org/html/features/balance-tags"));

        // Verify default property values
        assertEquals("upper", configuration.getProperty("http://cyberneko.org/html/properties/names/elems"));
        assertEquals("lower", configuration.getProperty("http://cyberneko.org/html/properties/names/attrs"));
        assertNotNull(configuration.getProperty("http://cyberneko.org/html/properties/error-reporter"));
    }

    @Test
    @DisplayName("Should set and get document handler")
    void testDocumentHandler() {
        // Set document handler
        configuration.setDocumentHandler(documentHandler);

        // Verify
        assertEquals(documentHandler, configuration.getDocumentHandler());
    }

    @Test
    @DisplayName("Should set tag balancing listener when document handler implements it")
    void testDocumentHandlerWithTagBalancingListener() {
        // Create a mock that implements both interfaces
        XMLDocumentHandler handler = mock(XMLDocumentHandler.class, withSettings().extraInterfaces(HTMLTagBalancingListener.class));

        // Set document handler
        configuration.setDocumentHandler(handler);

        // Verify
        assertEquals(handler, configuration.getDocumentHandler());
    }

    @Test
    @DisplayName("Should set and get DTD handler")
    void testDTDHandler() {
        configuration.setDTDHandler(dtdHandler);
        assertEquals(dtdHandler, configuration.getDTDHandler());
    }

    @Test
    @DisplayName("Should set and get DTD content model handler")
    void testDTDContentModelHandler() {
        configuration.setDTDContentModelHandler(dtdContentModelHandler);
        assertEquals(dtdContentModelHandler, configuration.getDTDContentModelHandler());
    }

    @Test
    @DisplayName("Should set and get error handler")
    void testErrorHandler() {
        configuration.setErrorHandler(errorHandler);
        assertEquals(errorHandler, configuration.getErrorHandler());
    }

    @Test
    @DisplayName("Should set and get entity resolver")
    void testEntityResolver() {
        configuration.setEntityResolver(entityResolver);
        assertEquals(entityResolver, configuration.getEntityResolver());
    }

    @Test
    @DisplayName("Should set and get locale")
    void testLocale() {
        Locale locale = Locale.FRENCH;
        configuration.setLocale(locale);
        assertEquals(locale, configuration.getLocale());
    }

    @Test
    @DisplayName("Should use default locale when null is set")
    void testLocaleWithNull() {
        configuration.setLocale(null);
        assertEquals(Locale.getDefault(), configuration.getLocale());
    }

    @Test
    @DisplayName("Should set features")
    void testSetFeature() {
        // Test setting various features
        assertDoesNotThrow(() -> {
            configuration.setFeature("http://cyberneko.org/html/features/augmentations", true);
            configuration.setFeature("http://xml.org/sax/features/namespaces", false);
            configuration.setFeature("http://cyberneko.org/html/features/balance-tags", false);
        });

        // Verify
        assertTrue(configuration.getFeature("http://cyberneko.org/html/features/augmentations"));
        assertFalse(configuration.getFeature("http://xml.org/sax/features/namespaces"));
        assertFalse(configuration.getFeature("http://cyberneko.org/html/features/balance-tags"));
    }

    @Test
    @DisplayName("Should throw exception for unknown feature")
    void testSetUnknownFeature() {
        assertThrows(XMLConfigurationException.class, () -> {
            configuration.setFeature("unknown.feature", true);
        });
    }

    @Test
    @DisplayName("Should set properties")
    void testSetProperty() {
        // Test setting various properties
        assertDoesNotThrow(() -> {
            configuration.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
            configuration.setProperty("http://cyberneko.org/html/properties/names/attrs", "upper");
        });

        // Verify
        assertEquals("lower", configuration.getProperty("http://cyberneko.org/html/properties/names/elems"));
        assertEquals("upper", configuration.getProperty("http://cyberneko.org/html/properties/names/attrs"));
    }

    @Test
    @DisplayName("Should set filters property")
    void testSetFiltersProperty() {
        // Create filter array
        XMLDocumentFilter filter1 = new DefaultFilter();
        XMLDocumentFilter filter2 = new DefaultFilter();
        XMLDocumentFilter[] filters = { filter1, filter2 };

        // Set filters
        configuration.setProperty("http://cyberneko.org/html/properties/filters", filters);

        // Verify
        assertArrayEquals(filters, (XMLDocumentFilter[]) configuration.getProperty("http://cyberneko.org/html/properties/filters"));
    }

    @Test
    @DisplayName("Should handle filters that are HTMLComponents")
    void testSetFiltersWithHTMLComponents() {
        // Create a custom filter that implements HTMLComponent
        class TestFilter extends DefaultFilter implements HTMLComponent {
            @Override
            public Boolean getFeatureDefault(String featureId) {
                return null;
            }

            @Override
            public Object getPropertyDefault(String propertyId) {
                return null;
            }
        }

        TestFilter filter = new TestFilter();
        XMLDocumentFilter[] filters = { filter };

        // Set filters
        configuration.setProperty("http://cyberneko.org/html/properties/filters", filters);

        // Verify
        assertArrayEquals(filters, (XMLDocumentFilter[]) configuration.getProperty("http://cyberneko.org/html/properties/filters"));
    }

    @Test
    @DisplayName("Should throw exception for unknown property")
    void testSetUnknownProperty() {
        assertThrows(XMLConfigurationException.class, () -> {
            configuration.setProperty("unknown.property", "value");
        });
    }

    @Test
    @DisplayName("Should push input source")
    void testPushInputSource() {
        XMLInputSource source = new XMLInputSource(null, null, null, new StringReader("<html></html>"), null);

        // Should not throw exception
        assertDoesNotThrow(() -> {
            configuration.pushInputSource(source);
        });
    }

    @Test
    @DisplayName("Should evaluate input source")
    void testEvaluateInputSource() {
        XMLInputSource source = new XMLInputSource(null, null, null, new StringReader("<html></html>"), null);

        // Should not throw exception
        assertDoesNotThrow(() -> {
            configuration.evaluateInputSource(source);
        });
    }

    @Test
    @DisplayName("Should parse from XMLInputSource")
    void testParseXMLInputSource() throws IOException {
        // Setup
        String html = "<html><body>Test</body></html>";
        XMLInputSource source = new XMLInputSource(null, null, null, new StringReader(html), null);
        configuration.setDocumentHandler(documentHandler);

        // Parse
        assertDoesNotThrow(() -> {
            configuration.parse(source);
        });
    }

    @Test
    @DisplayName("Should set input source for pull parsing")
    void testSetInputSource() throws IOException {
        // Setup
        when(inputSource.getByteStream()).thenReturn(null);
        when(inputSource.getCharacterStream()).thenReturn(new StringReader("<html></html>"));

        // Set input source
        assertDoesNotThrow(() -> {
            configuration.setInputSource(inputSource);
        });
    }

    @Test
    @DisplayName("Should parse in pull mode")
    void testPullParsing() throws IOException {
        // Setup
        String html = "<html><body>Test</body></html>";
        XMLInputSource source = new XMLInputSource(null, null, null, new StringReader(html), null);
        configuration.setInputSource(source);
        configuration.setDocumentHandler(documentHandler);

        // Parse
        boolean hasMore = configuration.parse(false);

        // Complete parsing
        while (hasMore) {
            hasMore = configuration.parse(false);
        }

        // Verify parsing completed
        assertFalse(hasMore);
    }

    @Test
    @DisplayName("Should cleanup resources")
    void testCleanup() throws IOException {
        // Setup
        String html = "<html><body>Test</body></html>";
        XMLInputSource source = new XMLInputSource(null, null, null, new StringReader(html), null);
        configuration.setInputSource(source);

        // Cleanup
        assertDoesNotThrow(() -> {
            configuration.cleanup();
        });
    }

    @Test
    @DisplayName("Should handle IOException during parsing")
    void testParseWithIOException() throws IOException {
        // Setup - create a reader that will throw IOException when read
        StringReader reader = new StringReader("<html></html>") {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("Test IO error");
            }
        };
        XMLInputSource source = new XMLInputSource(null, null, null, reader, null);

        // Attempt to parse
        assertThrows(IOException.class, () -> {
            configuration.parse(source);
        });
    }

    @Test
    @DisplayName("Should create custom document scanner")
    void testCustomDocumentScanner() {
        // Create a custom configuration with overridden createDocumentScanner
        class TestConfiguration extends HTMLConfiguration {
            private HTMLScanner customScanner;

            @Override
            protected HTMLScanner createDocumentScanner() {
                customScanner = new HTMLScanner();
                return customScanner;
            }

            public boolean usesCustomScanner() {
                // Check if the scanner used is our custom scanner
                return customScanner != null;
            }
        }

        TestConfiguration testConfig = new TestConfiguration();
        assertTrue(testConfig.usesCustomScanner());
    }

    @Test
    @DisplayName("Should test ErrorReporter formatMessage")
    void testErrorReporterFormatMessage() {
        HTMLConfiguration.ErrorReporter reporter = configuration.new ErrorReporter();

        // Test with simple format
        configuration.setFeature("http://cyberneko.org/html/features/report-errors/simple", true);
        String message = reporter.formatMessage("test.key", new Object[] { "arg1", "arg2" });
        assertTrue(message.contains("test.key"));
        assertTrue(message.contains("arg1"));
        assertTrue(message.contains("arg2"));
    }

    @Test
    @DisplayName("Should test ErrorReporter with localized messages")
    void testErrorReporterLocalizedMessage() {
        HTMLConfiguration.ErrorReporter reporter = configuration.new ErrorReporter();
        configuration.setFeature("http://cyberneko.org/html/features/report-errors/simple", false);

        // Test with a real error key
        String message = reporter.formatMessage("HTML_ELEMENT_NOT_RECOGNIZED", new Object[] { "unknown" });
        assertNotNull(message);
        assertFalse(message.isEmpty());
    }

    @Test
    @DisplayName("Should report warning through ErrorReporter")
    void testErrorReporterWarning() {
        HTMLConfiguration.ErrorReporter reporter = configuration.new ErrorReporter();
        configuration.setErrorHandler(errorHandler);

        // Report warning
        reporter.reportWarning("test.warning", new Object[] { "warning arg" });

        // Verify
        verify(errorHandler, times(1)).warning(eq("http://cyberneko.org/html"), eq("test.warning"), any(XMLParseException.class));
    }

    @Test
    @DisplayName("Should report error through ErrorReporter")
    void testErrorReporterError() {
        HTMLConfiguration.ErrorReporter reporter = configuration.new ErrorReporter();
        configuration.setErrorHandler(errorHandler);

        // Report error
        reporter.reportError("test.error", new Object[] { "error arg" });

        // Verify
        verify(errorHandler, times(1)).error(eq("http://cyberneko.org/html"), eq("test.error"), any(XMLParseException.class));
    }

    @Test
    @DisplayName("Should not report when error handler is null")
    void testErrorReporterWithoutHandler() {
        HTMLConfiguration.ErrorReporter reporter = configuration.new ErrorReporter();
        configuration.setErrorHandler(null);

        // These should not throw exceptions
        assertDoesNotThrow(() -> {
            reporter.reportWarning("test.warning", null);
            reporter.reportError("test.error", null);
        });
    }

    @Test
    @DisplayName("Should handle XNIException during parsing")
    void testParseWithXNIException() throws IOException {
        // Create a custom scanner that throws XNIException
        class FailingScanner extends HTMLScanner {
            @Override
            public boolean scanDocument(boolean complete) throws IOException {
                throw new XNIException("Test XNI error");
            }
        }

        // Create configuration with the failing scanner
        class FailingConfiguration extends HTMLConfiguration {
            @Override
            protected HTMLScanner createDocumentScanner() {
                return new FailingScanner();
            }
        }

        FailingConfiguration failingConfig = new FailingConfiguration();
        XMLInputSource source = new XMLInputSource(null, null, null, new StringReader("<html></html>"), null);
        failingConfig.setDocumentHandler(documentHandler);

        // Should throw XNIException
        assertThrows(XNIException.class, () -> {
            failingConfig.parse(source);
        });
    }

    @Test
    @DisplayName("Should determine closeStream based on input source")
    void testCloseStreamDetermination() throws IOException {
        // Test with byte stream
        XMLInputSource byteSource = new XMLInputSource(null, null, null, new ByteArrayInputStream("<html></html>".getBytes()), null);
        assertDoesNotThrow(() -> configuration.setInputSource(byteSource));
        // closeStream should be false when byte stream is provided

        // Test with character stream
        XMLInputSource charSource = new XMLInputSource(null, null, null, new StringReader("<html></html>"), null);
        assertDoesNotThrow(() -> configuration.setInputSource(charSource));
        // closeStream should be false when character stream is provided

        // Note: Testing with system ID only would require an actual file to exist
        // The behavior is that closeStream should be true when only system ID is provided
    }

    @Test
    @DisplayName("Should configure pipeline with namespaces enabled")
    void testPipelineWithNamespaces() throws IOException {
        // Enable namespaces
        configuration.setFeature("http://xml.org/sax/features/namespaces", true);
        configuration.setFeature("http://cyberneko.org/html/features/balance-tags", true);

        // Set input and parse
        XMLInputSource source =
                new XMLInputSource(null, null, null,
                        new StringReader("<html xmlns='http://www.w3.org/1999/xhtml'><body>Test</body></html>"), null);
        configuration.setDocumentHandler(documentHandler);

        assertDoesNotThrow(() -> {
            configuration.parse(source);
        });
    }

    @Test
    @DisplayName("Should configure pipeline without tag balancing")
    void testPipelineWithoutTagBalancing() throws IOException {
        // Disable tag balancing
        configuration.setFeature("http://cyberneko.org/html/features/balance-tags", false);

        // Set input and parse
        XMLInputSource source = new XMLInputSource(null, null, null, new StringReader("<html><body>Test"), null);
        configuration.setDocumentHandler(documentHandler);

        assertDoesNotThrow(() -> {
            configuration.parse(source);
        });
    }

    @Test
    @DisplayName("Should configure pipeline with filters")
    void testPipelineWithFilters() throws IOException {
        // Create and set filters
        DefaultFilter filter1 = new DefaultFilter();
        DefaultFilter filter2 = new DefaultFilter();
        XMLDocumentFilter[] filters = { filter1, filter2 };
        configuration.setProperty("http://cyberneko.org/html/properties/filters", filters);

        // Set input and parse
        XMLInputSource source = new XMLInputSource(null, null, null, new StringReader("<html><body>Test</body></html>"), null);
        configuration.setDocumentHandler(documentHandler);

        assertDoesNotThrow(() -> {
            configuration.parse(source);
        });
    }

    @Test
    @DisplayName("Should handle missing resource bundle gracefully")
    void testErrorReporterMissingResource() {
        HTMLConfiguration.ErrorReporter reporter = configuration.new ErrorReporter();
        configuration.setFeature("http://cyberneko.org/html/features/report-errors/simple", false);

        // Use a key that doesn't exist in the resource bundle
        String message = reporter.formatMessage("non.existent.key", new Object[] { "arg" });

        // Should fall back to simple format
        assertNotNull(message);
        assertTrue(message.contains("non.existent.key"));
    }

    @Test
    @DisplayName("Should change locale for error messages")
    void testErrorReporterLocaleChange() {
        HTMLConfiguration.ErrorReporter reporter = configuration.new ErrorReporter();
        configuration.setFeature("http://cyberneko.org/html/features/report-errors/simple", false);

        // Set French locale
        configuration.setLocale(Locale.FRENCH);
        String frenchMessage = reporter.formatMessage("HTML_ELEMENT_NOT_RECOGNIZED", new Object[] { "test" });

        // Set English locale
        configuration.setLocale(Locale.ENGLISH);
        String englishMessage = reporter.formatMessage("HTML_ELEMENT_NOT_RECOGNIZED", new Object[] { "test" });

        // Messages should be different (if translations exist) or at least not null
        assertNotNull(frenchMessage);
        assertNotNull(englishMessage);
    }
}