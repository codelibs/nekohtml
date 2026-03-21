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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Coverage tests for HTMLSAXParser methods not exercised by HTMLSAXParserTest.
 */
public class HTMLSAXParserCoverageTest {

    private HTMLSAXParser parser;

    @BeforeEach
    public void setUp() {
        parser = new HTMLSAXParser();
    }

    // ---- Constructor ----

    @Test
    public void testConstructorCreatesConfiguration() {
        assertNotNull(parser);
        assertNotNull(parser.fConfiguration, "Configuration should be initialized");
    }

    // ---- DTDHandler ----

    @Test
    public void testSetAndGetDTDHandler() {
        assertNull(parser.getDTDHandler(), "DTDHandler should initially be null");

        final DTDHandler handler = mock(DTDHandler.class);
        parser.setDTDHandler(handler);
        assertSame(handler, parser.getDTDHandler(), "Should return the same DTDHandler that was set");
    }

    // ---- EntityResolver ----

    @Test
    public void testSetAndGetEntityResolver() {
        assertNull(parser.getEntityResolver(), "EntityResolver should initially be null");

        final EntityResolver resolver = mock(EntityResolver.class);
        parser.setEntityResolver(resolver);
        assertSame(resolver, parser.getEntityResolver(), "Should return the same EntityResolver that was set");
    }

    // ---- ErrorHandler ----

    @Test
    public void testSetAndGetErrorHandler() {
        assertNull(parser.getErrorHandler(), "ErrorHandler should initially be null");

        final ErrorHandler handler = mock(ErrorHandler.class);
        parser.setErrorHandler(handler);
        assertSame(handler, parser.getErrorHandler(), "Should return the same ErrorHandler that was set");
    }

    // ---- ContentHandler round-trip ----

    @Test
    public void testGetContentHandlerReturnsSetHandler() {
        final ContentHandler handler = mock(ContentHandler.class);
        parser.setContentHandler(handler);
        assertSame(handler, parser.getContentHandler(), "Should return the same ContentHandler that was set");
    }

    @Test
    public void testGetContentHandlerInitiallyNull() {
        assertNull(parser.getContentHandler(), "ContentHandler should initially be null");
    }

    // ---- LexicalHandler ----

    @Test
    public void testSetAndGetLexicalHandler() {
        assertNull(parser.getLexicalHandler(), "LexicalHandler should initially be null");

        final LexicalHandler handler = mock(LexicalHandler.class);
        parser.setLexicalHandler(handler);
        assertSame(handler, parser.getLexicalHandler(), "Should return the same LexicalHandler that was set");
    }

    // ---- Feature ----

    @Test
    public void testSetAndGetFeatureNamespaces() throws Exception {
        final String feature = "http://xml.org/sax/features/namespaces";

        parser.setFeature(feature, true);
        assertTrue(parser.getFeature(feature), "Namespaces feature should be true after setting");

        parser.setFeature(feature, false);
        assertFalse(parser.getFeature(feature), "Namespaces feature should be false after setting");
    }

    @Test
    public void testSetAndGetFeatureBalanceTags() throws Exception {
        final String feature = "http://cyberneko.org/html/features/balance-tags";

        // balance-tags defaults to true
        assertTrue(parser.getFeature(feature), "balance-tags should default to true");

        parser.setFeature(feature, false);
        assertFalse(parser.getFeature(feature), "balance-tags should be false after setting");
    }

    // ---- Property (lexical-handler via setProperty/getProperty) ----

    @Test
    public void testSetAndGetPropertyLexicalHandler() throws Exception {
        final String property = "http://xml.org/sax/properties/lexical-handler";

        assertNull(parser.getProperty(property), "Lexical handler property should initially be null");

        final LexicalHandler handler = mock(LexicalHandler.class);
        parser.setProperty(property, handler);
        assertSame(handler, parser.getProperty(property), "Should return the same handler set via property");
    }

    // ---- parse(InputSource) with event verification ----

    @Test
    public void testParseInputSourceFiresContentHandlerEvents() throws Exception {
        final ContentHandler handler = mock(ContentHandler.class);
        parser.setContentHandler(handler);

        final String html = "<html><body><p>test</p></body></html>";
        parser.parse(new InputSource(new StringReader(html)));

        verify(handler).startDocument();
        verify(handler).endDocument();
    }

    // ---- parse(String systemId) ----

    @Test
    public void testParseSystemId(@TempDir final File tempDir) throws Exception {
        final File htmlFile = new File(tempDir, "test.html");
        try (FileWriter writer = new FileWriter(htmlFile)) {
            writer.write("<html><body><p>Hello</p></body></html>");
        }

        final EventCollector collector = new EventCollector();
        parser.setContentHandler(collector);

        parser.parse(htmlFile.toURI().toString());

        assertTrue(collector.events.contains("startDocument"), "Should fire startDocument");
        assertTrue(collector.events.contains("endDocument"), "Should fire endDocument");

        boolean foundP = false;
        for (final String event : collector.events) {
            if (event.equals("startElement:P")) {
                foundP = true;
                break;
            }
        }
        assertTrue(foundP, "Should have parsed P element from file");
    }

    // ---- LexicalHandler receives comment events ----

    @Test
    public void testLexicalHandlerReceivesComments() throws Exception {
        final LexicalHandler lexHandler = mock(LexicalHandler.class);
        parser.setLexicalHandler(lexHandler);
        parser.setContentHandler(new DefaultHandler());

        final String html = "<html><body><!-- a comment --></body></html>";
        parser.parse(new InputSource(new StringReader(html)));

        verify(lexHandler).comment(" a comment ".toCharArray(), 0, " a comment ".length());
    }

    /**
     * Simple handler that collects event names.
     */
    static class EventCollector extends DefaultHandler {
        final List<String> events = new ArrayList<>();

        @Override
        public void startDocument() throws SAXException {
            events.add("startDocument");
        }

        @Override
        public void endDocument() throws SAXException {
            events.add("endDocument");
        }

        @Override
        public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                throws SAXException {
            events.add("startElement:" + qName);
        }

        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            events.add("endElement:" + qName);
        }
    }

} // class HTMLSAXParserCoverageTest
