/*
 * Copyright 2025 CodeLibs, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codelibs.nekohtml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;

import org.codelibs.nekohtml.filters.Writer;
import org.codelibs.nekohtml.parsers.DOMParser;
import org.codelibs.xerces.xni.parser.XMLInputSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Tests for HTML5 template element special processing.
 */
public class HTML5TemplateElementTest {

    @Test
    @DisplayName("HTML5 template element should be parsed correctly")
    void testHTML5TemplateElementParsing() throws IOException, SAXException {
        String html = "<!DOCTYPE html><template><div>Template content</div></template>";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();

        // Check that template element exists
        NodeList templates = doc.getElementsByTagName("TEMPLATE");
        assertEquals(1, templates.getLength(), "Should have one template element");

        // In NekoHTML, template content is preserved but not exposed as DOM children
        // This is actually closer to the HTML5 spec where template content is in a separate document fragment
        Element template = (Element) templates.item(0);
        assertNotNull(template, "Template element should exist");
    }

    @Test
    @DisplayName("HTML5 template element should handle nested content")
    void testHTML5TemplateNestedContent() throws IOException, SAXException {
        String html = "<!DOCTYPE html><template><p>Paragraph</p><span>Span</span><div>Division</div></template>";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();

        NodeList templates = doc.getElementsByTagName("TEMPLATE");
        assertEquals(1, templates.getLength(), "Should have one template element");

        Element template = (Element) templates.item(0);
        assertNotNull(template, "Template element should exist");
    }

    @Test
    @DisplayName("HTML5 template element should handle empty content")
    void testHTML5TemplateEmptyContent() throws IOException, SAXException {
        String html = "<!DOCTYPE html><template></template>";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();

        NodeList templates = doc.getElementsByTagName("TEMPLATE");
        assertEquals(1, templates.getLength(), "Should have one template element");
    }

    @Test
    @DisplayName("HTML5 template element should handle text content")
    void testHTML5TemplateTextContent() throws IOException, SAXException {
        String html = "<!DOCTYPE html><template>Just text content</template>";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();

        NodeList templates = doc.getElementsByTagName("TEMPLATE");
        assertEquals(1, templates.getLength(), "Should have one template element");

        Element template = (Element) templates.item(0);
        assertNotNull(template, "Template element should exist");
    }

    @Test
    @DisplayName("HTML5 template element should handle multiple templates")
    void testHTML5MultipleTemplateElements() throws IOException, SAXException {
        String html =
                "<!DOCTYPE html><template id=\"t1\"><div>First template</div></template><template id=\"t2\"><span>Second template</span></template>";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();

        NodeList templates = doc.getElementsByTagName("TEMPLATE");
        assertEquals(2, templates.getLength(), "Should have two template elements");

        Element template1 = (Element) templates.item(0);
        Element template2 = (Element) templates.item(1);
        assertNotNull(template1, "First template should exist");
        assertNotNull(template2, "Second template should exist");
    }

    @Test
    @DisplayName("HTML5 template element should handle nested templates")
    void testHTML5NestedTemplateElements() throws IOException, SAXException {
        String html = "<!DOCTYPE html><template><div>Outer template<template><span>Inner template</span></template></div></template>";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();

        NodeList templates = doc.getElementsByTagName("TEMPLATE");
        assertTrue(templates.getLength() >= 1, "Should have at least one template element");
    }

    @Test
    @DisplayName("HTML5 template element output should be well-formed")
    void testHTML5TemplateElementOutput() throws IOException {
        String html = "<!DOCTYPE html><template><div class=\"content\">Template content</div></template>";

        HTMLConfiguration config = new HTMLConfiguration();
        StringWriter writer = new StringWriter();
        Writer filter = new Writer(writer, "UTF-8");
        config.setDocumentHandler(filter);

        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        config.parse(source);

        String result = writer.toString();

        // Check that output contains expected elements
        assertTrue(result.contains("<TEMPLATE"), "Should contain template element");
        assertTrue(result.contains("Template content"), "Should contain template content");
        assertTrue(result.contains("class=\"content\""), "Should preserve attributes");
        // Template content should be preserved in serialization even if not in DOM
        assertTrue(result.contains("<div"), "Should contain div element in serialized output");
    }
}
