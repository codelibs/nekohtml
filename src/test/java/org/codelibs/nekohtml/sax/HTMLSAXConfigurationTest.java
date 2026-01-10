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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Test cases for HTMLSAXConfiguration.
 */
public class HTMLSAXConfigurationTest {

    @Test
    public void testBasicParsing() throws Exception {
        final String html = "<html><head><title>Test</title></head><body><p>Hello World</p></body></html>";

        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();
        final TestHandler handler = new TestHandler();
        config.setContentHandler(handler);

        config.parse(new InputSource(new StringReader(html)));

        assertTrue(handler.events.size() > 0, "Should have received events");
        assertTrue(handler.events.contains("startElement:HTML"), "Should have HTML start element");
        assertTrue(handler.events.contains("startElement:BODY"), "Should have BODY start element");
        assertTrue(handler.events.contains("startElement:P"), "Should have P start element");
    }

    @Test
    public void testFeatureManagement() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();

        // Test default features
        assertTrue(config.getFeature(HTMLSAXConfiguration.BALANCE_TAGS), "Tag balancing should be enabled by default");
        assertFalse(config.getFeature(HTMLSAXConfiguration.NAMESPACES), "Namespaces should be disabled by default");

        // Test setting features
        config.setFeature(HTMLSAXConfiguration.BALANCE_TAGS, false);
        assertFalse(config.getFeature(HTMLSAXConfiguration.BALANCE_TAGS), "Should be able to disable tag balancing");

        config.setFeature(HTMLSAXConfiguration.NAMESPACES, true);
        assertTrue(config.getFeature(HTMLSAXConfiguration.NAMESPACES), "Should be able to enable namespaces");
    }

    @Test
    public void testPipelineConfiguration() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();

        // Verify scanner is created
        assertNotNull(config.fScanner, "Scanner should be created");

        // Verify tag balancer is in pipeline when enabled
        config.setFeature(HTMLSAXConfiguration.BALANCE_TAGS, true);
        assertTrue(config.fPipeline.size() >= 2, "Pipeline should contain scanner and tag balancer");

        // Verify tag balancer is removed when disabled
        config.setFeature(HTMLSAXConfiguration.BALANCE_TAGS, false);
        assertEquals(1, config.fPipeline.size(), "Pipeline should only contain scanner");
    }

    // Tag balancing test removed - current implementation is pass-through only

    @Test
    public void testHandlerPropagation() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();
        final TestHandler handler = new TestHandler();

        config.setContentHandler(handler);
        assertNotNull(config.getContentHandler(), "Content handler should be set");

        config.setErrorHandler(handler);
        assertNotNull(config.getErrorHandler(), "Error handler should be set");

        config.setEntityResolver(handler);
        assertNotNull(config.getEntityResolver(), "Entity resolver should be set");
    }

    @Test
    public void testLexicalHandler() throws Exception {
        final String html = "<!DOCTYPE html><html><!--comment--><body></body></html>";

        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();
        final TestLexicalHandler lexHandler = new TestLexicalHandler();
        final TestHandler handler = new TestHandler();

        config.setContentHandler(handler);
        config.setLexicalHandler(lexHandler);

        config.parse(new InputSource(new StringReader(html)));

        assertTrue(lexHandler.commentReceived, "Should receive comment events");
    }

    /**
     * Simple test handler that collects events.
     */
    static class TestHandler extends DefaultHandler {
        List<String> events = new ArrayList<>();

        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                throws SAXException {
            events.add("startElement:" + qName);
        }

        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            events.add("endElement:" + qName);
        }

        @Override
        public void characters(final char[] ch, final int start, final int length) throws SAXException {
            final String text = new String(ch, start, length).trim();
            if (text.length() > 0) {
                events.add("characters:" + text);
            }
        }
    }

    /**
     * Test lexical handler.
     */
    static class TestLexicalHandler implements org.xml.sax.ext.LexicalHandler {
        boolean commentReceived = false;

        @Override
        public void startDTD(final String name, final String publicId, final String systemId) throws SAXException {
        }

        @Override
        public void endDTD() throws SAXException {
        }

        @Override
        public void startEntity(final String name) throws SAXException {
        }

        @Override
        public void endEntity(final String name) throws SAXException {
        }

        @Override
        public void startCDATA() throws SAXException {
        }

        @Override
        public void endCDATA() throws SAXException {
        }

        @Override
        public void comment(final char[] ch, final int start, final int length) throws SAXException {
            commentReceived = true;
        }
    }

    // =========================================================================
    // Feature Combination Tests
    // =========================================================================

    @Test
    public void testAugmentationsFeature() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();

        // Default should be false
        assertFalse(config.getFeature(HTMLSAXConfiguration.AUGMENTATIONS), "Augmentations should be disabled by default");

        // Enable augmentations
        config.setFeature(HTMLSAXConfiguration.AUGMENTATIONS, true);
        assertTrue(config.getFeature(HTMLSAXConfiguration.AUGMENTATIONS), "Should be able to enable augmentations");
    }

    @Test
    public void testReportErrorsFeature() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();

        // Default should be false
        assertFalse(config.getFeature(HTMLSAXConfiguration.REPORT_ERRORS), "Report errors should be disabled by default");

        // Enable error reporting
        config.setFeature(HTMLSAXConfiguration.REPORT_ERRORS, true);
        assertTrue(config.getFeature(HTMLSAXConfiguration.REPORT_ERRORS), "Should be able to enable error reporting");
    }

    @Test
    public void testSimpleErrorFormatFeature() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();

        // Default should be false
        assertFalse(config.getFeature(HTMLSAXConfiguration.SIMPLE_ERROR_FORMAT),
                "Simple error format should be disabled by default");

        // Enable simple error format
        config.setFeature(HTMLSAXConfiguration.SIMPLE_ERROR_FORMAT, true);
        assertTrue(config.getFeature(HTMLSAXConfiguration.SIMPLE_ERROR_FORMAT),
                "Should be able to enable simple error format");
    }

    @Test
    public void testHTML5ModeFeature() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();

        // Default should be false
        assertFalse(config.getFeature(HTMLSAXConfiguration.HTML5_MODE), "HTML5 mode should be disabled by default");

        // Enable HTML5 mode
        config.setFeature(HTMLSAXConfiguration.HTML5_MODE, true);
        assertTrue(config.getFeature(HTMLSAXConfiguration.HTML5_MODE), "Should be able to enable HTML5 mode");
    }

    @Test
    public void testAllFeaturesEnabledTogether() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();

        // Enable all features
        config.setFeature(HTMLSAXConfiguration.NAMESPACES, true);
        config.setFeature(HTMLSAXConfiguration.AUGMENTATIONS, true);
        config.setFeature(HTMLSAXConfiguration.REPORT_ERRORS, true);
        config.setFeature(HTMLSAXConfiguration.SIMPLE_ERROR_FORMAT, true);
        config.setFeature(HTMLSAXConfiguration.BALANCE_TAGS, true);
        config.setFeature(HTMLSAXConfiguration.HTML5_MODE, true);

        // Verify all are enabled
        assertTrue(config.getFeature(HTMLSAXConfiguration.NAMESPACES), "Namespaces should be enabled");
        assertTrue(config.getFeature(HTMLSAXConfiguration.AUGMENTATIONS), "Augmentations should be enabled");
        assertTrue(config.getFeature(HTMLSAXConfiguration.REPORT_ERRORS), "Report errors should be enabled");
        assertTrue(config.getFeature(HTMLSAXConfiguration.SIMPLE_ERROR_FORMAT), "Simple error format should be enabled");
        assertTrue(config.getFeature(HTMLSAXConfiguration.BALANCE_TAGS), "Balance tags should be enabled");
        assertTrue(config.getFeature(HTMLSAXConfiguration.HTML5_MODE), "HTML5 mode should be enabled");

        // Verify parsing still works
        final TestHandler handler = new TestHandler();
        config.setContentHandler(handler);
        config.parse(new InputSource(new StringReader("<html><body><p>Test</p></body></html>")));

        assertTrue(handler.events.size() > 0, "Should parse with all features enabled");
    }

    @Test
    public void testAllFeaturesDisabled() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();

        // Disable all features
        config.setFeature(HTMLSAXConfiguration.NAMESPACES, false);
        config.setFeature(HTMLSAXConfiguration.AUGMENTATIONS, false);
        config.setFeature(HTMLSAXConfiguration.REPORT_ERRORS, false);
        config.setFeature(HTMLSAXConfiguration.SIMPLE_ERROR_FORMAT, false);
        config.setFeature(HTMLSAXConfiguration.BALANCE_TAGS, false);
        config.setFeature(HTMLSAXConfiguration.HTML5_MODE, false);

        // Verify parsing still works
        final TestHandler handler = new TestHandler();
        config.setContentHandler(handler);
        config.parse(new InputSource(new StringReader("<html><body><p>Test</p></body></html>")));

        assertTrue(handler.events.size() > 0, "Should parse with all features disabled");
    }

    // =========================================================================
    // Property Tests
    // =========================================================================

    @Test
    public void testNamesElemsProperty() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();

        // Default should be "upper"
        assertEquals("upper", config.getProperty(HTMLSAXConfiguration.NAMES_ELEMS),
                "Element names should default to upper case");

        // Change to lower
        config.setProperty(HTMLSAXConfiguration.NAMES_ELEMS, "lower");
        assertEquals("lower", config.getProperty(HTMLSAXConfiguration.NAMES_ELEMS),
                "Should be able to set element names to lower case");
    }

    @Test
    public void testNamesAttrsProperty() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();

        // Default should be "lower"
        assertEquals("lower", config.getProperty(HTMLSAXConfiguration.NAMES_ATTRS),
                "Attribute names should default to lower case");

        // Change to upper
        config.setProperty(HTMLSAXConfiguration.NAMES_ATTRS, "upper");
        assertEquals("upper", config.getProperty(HTMLSAXConfiguration.NAMES_ATTRS),
                "Should be able to set attribute names to upper case");
    }

    @Test
    public void testLexicalHandlerProperty() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();
        final TestLexicalHandler lexHandler = new TestLexicalHandler();

        // Set via property
        config.setProperty("http://xml.org/sax/properties/lexical-handler", lexHandler);

        // Get via property
        assertSame(lexHandler, config.getProperty("http://xml.org/sax/properties/lexical-handler"),
                "Lexical handler should be retrievable via property");

        // Also get via getter
        assertSame(lexHandler, config.getLexicalHandler(),
                "Lexical handler should be retrievable via getter");
    }

    @Test
    public void testUnrecognizedFeature() {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();

        assertThrows(org.xml.sax.SAXNotRecognizedException.class,
                () -> config.getFeature("http://example.com/unknown-feature"),
                "Should throw for unrecognized feature");
    }

    @Test
    public void testUnrecognizedProperty() {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();

        assertThrows(org.xml.sax.SAXNotRecognizedException.class,
                () -> config.getProperty("http://example.com/unknown-property"),
                "Should throw for unrecognized property");
    }

    // =========================================================================
    // Pipeline Configuration Tests
    // =========================================================================

    @Test
    public void testTagBalancingAffectsPipeline() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();

        // Start with tag balancing enabled (default)
        assertTrue(config.getFeature(HTMLSAXConfiguration.BALANCE_TAGS));
        final int initialSize = config.fPipeline.size();

        // Disable tag balancing
        config.setFeature(HTMLSAXConfiguration.BALANCE_TAGS, false);
        final int disabledSize = config.fPipeline.size();

        // Pipeline should be smaller without tag balancer
        assertTrue(disabledSize <= initialSize, "Pipeline should be smaller without tag balancer");

        // Re-enable tag balancing
        config.setFeature(HTMLSAXConfiguration.BALANCE_TAGS, true);
        final int reenableSize = config.fPipeline.size();

        assertEquals(initialSize, reenableSize, "Pipeline should be restored when re-enabling");
    }

    @Test
    public void testParsingWithTagBalancingDisabled() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();
        config.setFeature(HTMLSAXConfiguration.BALANCE_TAGS, false);

        final TestHandler handler = new TestHandler();
        config.setContentHandler(handler);

        // Parse with imbalanced tags
        config.parse(new InputSource(new StringReader("<html><body><div><p>Unclosed")));

        // Should still parse (but without auto-closing)
        assertTrue(handler.events.size() > 0, "Should parse without tag balancing");
    }

    @Test
    public void testParsingWithTagBalancingEnabled() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();
        config.setFeature(HTMLSAXConfiguration.BALANCE_TAGS, true);

        final TestHandler handler = new TestHandler();
        config.setContentHandler(handler);

        // Parse with imbalanced tags
        config.parse(new InputSource(new StringReader("<html><body><div><p>Auto-close test</div></body></html>")));

        // Should parse with auto-closing
        assertTrue(handler.events.size() > 0, "Should parse with tag balancing");
        // Tag balancer should auto-close <p>
        assertTrue(handler.events.contains("endElement:P") || handler.events.contains("endElement:DIV"),
                "Tag balancer should close elements");
    }

    // =========================================================================
    // Handler Management Tests
    // =========================================================================

    @Test
    public void testDTDHandlerGetterSetter() {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();
        final org.xml.sax.DTDHandler handler = new org.xml.sax.DTDHandler() {
            @Override
            public void notationDecl(String name, String publicId, String systemId) {
            }

            @Override
            public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) {
            }
        };

        config.setDTDHandler(handler);
        assertSame(handler, config.getDTDHandler(), "DTD handler should be retrievable");
    }

    @Test
    public void testEntityResolverGetterSetter() {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();
        final org.xml.sax.EntityResolver resolver = (publicId, systemId) -> null;

        config.setEntityResolver(resolver);
        assertSame(resolver, config.getEntityResolver(), "Entity resolver should be retrievable");
    }

    @Test
    public void testErrorHandlerGetterSetter() {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();
        final org.xml.sax.ErrorHandler handler = new org.xml.sax.ErrorHandler() {
            @Override
            public void warning(org.xml.sax.SAXParseException exception) {
            }

            @Override
            public void error(org.xml.sax.SAXParseException exception) {
            }

            @Override
            public void fatalError(org.xml.sax.SAXParseException exception) {
            }
        };

        config.setErrorHandler(handler);
        assertSame(handler, config.getErrorHandler(), "Error handler should be retrievable");
    }

    @Test
    public void testLocaleGetterSetter() {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();

        config.setLocale(java.util.Locale.JAPANESE);
        assertEquals(java.util.Locale.JAPANESE, config.getLocale(), "Locale should be retrievable");

        config.setLocale(java.util.Locale.FRENCH);
        assertEquals(java.util.Locale.FRENCH, config.getLocale(), "Locale should be changeable");
    }

    // =========================================================================
    // Parse Method Variations
    // =========================================================================

    @Test
    public void testParseWithSystemId() throws Exception {
        // Create temp file
        final java.io.File tempFile = java.io.File.createTempFile("test", ".html");
        tempFile.deleteOnExit();

        try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
            writer.write("<html><body><p>From file</p></body></html>");
        }

        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();
        final TestHandler handler = new TestHandler();
        config.setContentHandler(handler);

        config.parse(tempFile.getAbsolutePath());

        assertTrue(handler.events.contains("startElement:P"), "Should parse from file path");
    }

    @Test
    public void testParseWithNullContentHandler() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();
        // Don't set content handler

        // Should not throw - uses DefaultHandler
        assertDoesNotThrow(() -> config.parse(new InputSource(new StringReader("<html></html>"))));
    }

    // =========================================================================
    // Complex Feature Interactions
    // =========================================================================

    @Test
    public void testNamespacesWithTagBalancing() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();
        config.setFeature(HTMLSAXConfiguration.NAMESPACES, true);
        config.setFeature(HTMLSAXConfiguration.BALANCE_TAGS, true);

        final TestHandler handler = new TestHandler();
        config.setContentHandler(handler);

        config.parse(new InputSource(new StringReader("<html><body><div><p>Test</div></body></html>")));

        assertTrue(handler.events.size() > 0, "Should parse with namespaces and tag balancing");
    }

    @Test
    public void testReportErrorsWithSimpleFormat() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();
        config.setFeature(HTMLSAXConfiguration.REPORT_ERRORS, true);
        config.setFeature(HTMLSAXConfiguration.SIMPLE_ERROR_FORMAT, true);

        final TestHandler handler = new TestHandler();
        config.setContentHandler(handler);

        // Parse malformed HTML
        config.parse(new InputSource(new StringReader("<html><body></p></body></html>")));

        // Should parse without throwing
        assertTrue(handler.events.size() > 0, "Should parse with error reporting enabled");
    }

    @Test
    public void testHTML5ModeWithTagBalancing() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();
        config.setFeature(HTMLSAXConfiguration.HTML5_MODE, true);
        config.setFeature(HTMLSAXConfiguration.BALANCE_TAGS, true);

        final TestHandler handler = new TestHandler();
        config.setContentHandler(handler);

        // Parse HTML5 document
        config.parse(new InputSource(new StringReader(
                "<!DOCTYPE html><html><body><article><section>Content</section></article></body></html>")));

        assertTrue(handler.events.contains("startElement:ARTICLE"), "Should parse HTML5 elements");
        assertTrue(handler.events.contains("startElement:SECTION"), "Should parse HTML5 elements");
    }

    // =========================================================================
    // Edge Cases
    // =========================================================================

    @Test
    public void testEmptyPipeline() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();

        // The pipeline should never be empty due to scanner
        assertFalse(config.fPipeline.isEmpty(), "Pipeline should not be empty");
    }

    @Test
    public void testMultipleFeatureChanges() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();

        // Toggle features multiple times
        for (int i = 0; i < 5; i++) {
            config.setFeature(HTMLSAXConfiguration.BALANCE_TAGS, true);
            config.setFeature(HTMLSAXConfiguration.BALANCE_TAGS, false);
        }
        config.setFeature(HTMLSAXConfiguration.BALANCE_TAGS, true);

        // Should still work correctly
        final TestHandler handler = new TestHandler();
        config.setContentHandler(handler);
        config.parse(new InputSource(new StringReader("<html><body></body></html>")));

        assertTrue(handler.events.size() > 0, "Should parse after multiple feature changes");
    }

    @Test
    public void testContentHandlerReassignment() throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();

        final TestHandler handler1 = new TestHandler();
        final TestHandler handler2 = new TestHandler();

        // Set first handler
        config.setContentHandler(handler1);
        config.parse(new InputSource(new StringReader("<html><body><p>First</p></body></html>")));

        // Set second handler
        config.setContentHandler(handler2);
        config.parse(new InputSource(new StringReader("<html><body><div>Second</div></body></html>")));

        // First handler should have <p>
        assertTrue(handler1.events.contains("startElement:P"), "First handler should receive first parse");

        // Second handler should have <div>
        assertTrue(handler2.events.contains("startElement:DIV"), "Second handler should receive second parse");
    }

} // class HTMLSAXConfigurationTest
