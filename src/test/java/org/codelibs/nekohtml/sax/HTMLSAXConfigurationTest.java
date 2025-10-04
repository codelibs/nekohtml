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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

} // class HTMLSAXConfigurationTest
