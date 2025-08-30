/*
 * Copyright Marc Guillemot
 * Copyright 2002-2009 Andy Clark, Marc Guillemot
 * Copyright 2017-2024 Shinsuke Sugaya
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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.codelibs.nekohtml.filters.Writer;
import org.codelibs.nekohtml.parsers.DOMParser;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Tests for HTML5 foster parenting features.
 *
 * @author Shinsuke Sugaya
 */
public class HTML5FosterParentingTest {

    @Test
    @DisplayName("HTML5 foster parenting should handle text in table")
    void testHTML5FosterParentingText() throws IOException, SAXException {
        String html = "<!DOCTYPE html><table>Fostered text<tr><td>Cell content</td></tr></table>";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();

        // Check that table structure is preserved
        NodeList tables = doc.getElementsByTagName("TABLE");
        assertEquals(1, tables.getLength(), "Should have one table");

        NodeList trs = doc.getElementsByTagName("TR");
        assertEquals(1, trs.getLength(), "Should have one tr");

        NodeList tds = doc.getElementsByTagName("TD");
        assertEquals(1, tds.getLength(), "Should have one td");
    }

    @Test
    @DisplayName("HTML5 foster parenting should handle inline elements in table")
    void testHTML5FosterParentingInlineElements() throws IOException, SAXException {
        String html = "<!DOCTYPE html><table><span>Fostered span</span><tr><td>Cell</td></tr></table>";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();

        // Check that both span and table are present
        NodeList spans = doc.getElementsByTagName("SPAN");
        assertEquals(1, spans.getLength(), "Should have one span element");

        NodeList tables = doc.getElementsByTagName("TABLE");
        assertEquals(1, tables.getLength(), "Should have one table");

        NodeList trs = doc.getElementsByTagName("TR");
        assertEquals(1, trs.getLength(), "Should have one tr");
    }

    @Test
    @DisplayName("HTML5 foster parenting should not affect valid table content")
    void testHTML5FosterParentingValidContent() throws IOException, SAXException {
        String html = "<!DOCTYPE html><table><caption>Table Caption</caption><tr><td>Valid content</td></tr></table>";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();

        // Check that all table elements are preserved
        NodeList tables = doc.getElementsByTagName("TABLE");
        assertEquals(1, tables.getLength(), "Should have one table");

        NodeList captions = doc.getElementsByTagName("CAPTION");
        assertEquals(1, captions.getLength(), "Should have one caption");

        NodeList trs = doc.getElementsByTagName("TR");
        assertEquals(1, trs.getLength(), "Should have one tr");

        NodeList tds = doc.getElementsByTagName("TD");
        assertEquals(1, tds.getLength(), "Should have one td");
    }

    @Test
    @DisplayName("HTML5 foster parenting should handle nested table scenarios")
    void testHTML5FosterParentingNestedTables() throws IOException, SAXException {
        String html = "<!DOCTYPE html><table><table><tr><td>Inner table</td></tr></table><tr><td>Outer table</td></tr></table>";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();

        // Check that both tables are present
        NodeList tables = doc.getElementsByTagName("TABLE");
        assertTrue(tables.getLength() >= 1, "Should have at least one table");

        NodeList trs = doc.getElementsByTagName("TR");
        assertTrue(trs.getLength() >= 2, "Should have at least two tr elements");

        NodeList tds = doc.getElementsByTagName("TD");
        assertTrue(tds.getLength() >= 2, "Should have at least two td elements");
    }

    @Test
    @DisplayName("HTML5 foster parenting output should be well-formed")
    void testHTML5FosterParentingOutput() throws IOException {
        String html = "<!DOCTYPE html><table>Fostered<tr><td>Cell</td></tr></table>";

        HTMLConfiguration config = new HTMLConfiguration();
        StringWriter writer = new StringWriter();
        Writer filter = new Writer(writer, "UTF-8");
        config.setDocumentHandler(filter);

        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        config.parse(source);

        String result = writer.toString();

        // Check that output contains expected elements
        assertTrue(result.contains("<TABLE"), "Should contain table element");
        assertTrue(result.contains("<TR"), "Should contain tr element");
        assertTrue(result.contains("<TD"), "Should contain td element");
        assertTrue(result.contains("Cell"), "Should contain cell content");
    }
}