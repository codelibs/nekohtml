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
 * Test cases for HTMLSAXParser.
 */
public class HTMLSAXParserTest {

    @Test
    public void testSimpleHTML() throws Exception {
        final String html = "<html><head><title>Test</title></head><body><p>Hello World</p></body></html>";

        final HTMLSAXParser parser = new HTMLSAXParser();
        final TestHandler handler = new TestHandler();
        parser.setContentHandler(handler);

        parser.parse(new InputSource(new StringReader(html)));

        // Verify we received events
        assertTrue(handler.events.size() > 0, "Should have received SAX events");
        assertTrue(handler.events.contains("startDocument"), "Should have startDocument event");
        assertTrue(handler.events.contains("endDocument"), "Should have endDocument event");
        assertTrue(handler.events.contains("startElement:HTML"), "Should have HTML element");
        assertTrue(handler.events.contains("startElement:BODY"), "Should have BODY element");
        assertTrue(handler.events.contains("startElement:P"), "Should have P element");
    }

    @Test
    public void testMalformedHTML() throws Exception {
        // Test with unclosed tags - should still parse
        final String html = "<html><body><p>Unclosed paragraph<div>Nested div";

        final HTMLSAXParser parser = new HTMLSAXParser();
        final TestHandler handler = new TestHandler();
        parser.setContentHandler(handler);

        parser.parse(new InputSource(new StringReader(html)));

        // Should still receive events even with malformed HTML
        assertTrue(handler.events.size() > 0, "Should parse malformed HTML");
    }

    @Test
    public void testAttributes() throws Exception {
        final String html = "<html><body><a href='http://example.com' class='link'>Click</a></body></html>";

        final HTMLSAXParser parser = new HTMLSAXParser();
        final TestHandler handler = new TestHandler();
        parser.setContentHandler(handler);

        parser.parse(new InputSource(new StringReader(html)));

        // Check that we captured the attributes
        boolean foundHref = false;
        for (final String event : handler.events) {
            if (event.contains("attr:href")) {
                foundHref = true;
                break;
            }
        }
        assertTrue(foundHref, "Should have found href attribute");
    }

    @Test
    public void testCharacterData() throws Exception {
        final String html = "<html><body>Hello World</body></html>";

        final HTMLSAXParser parser = new HTMLSAXParser();
        final TestHandler handler = new TestHandler();
        parser.setContentHandler(handler);

        parser.parse(new InputSource(new StringReader(html)));

        // Check for character data
        boolean foundText = false;
        for (final String event : handler.events) {
            if (event.contains("characters:Hello World")) {
                foundText = true;
                break;
            }
        }
        assertTrue(foundText, "Should have found character data");
    }

    /**
     * Simple test handler that collects events.
     */
    static class TestHandler extends DefaultHandler {
        List<String> events = new ArrayList<>();

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

            // Record attributes
            for (int i = 0; i < attributes.getLength(); i++) {
                events.add("attr:" + attributes.getQName(i) + "=" + attributes.getValue(i));
            }
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

} // class HTMLSAXParserTest
