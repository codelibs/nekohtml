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

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileReader;
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
 * Tests for HTML5 optional tag omission features.
 */
public class HTML5OptionalTagsTest {

    private static final String TEST_DATA_DIR = "src/test/resources/data/";

    @Test
    @DisplayName("HTML5 optional tags should parse correctly")
    void testHTML5OptionalTagsParsing() throws IOException, SAXException {
        DOMParser parser = new DOMParser();
        parser.parse(TEST_DATA_DIR + "test-html5-optional-tags.html");

        Document doc = parser.getDocument();
        assertNotNull(doc, "Document should be parsed successfully");

        Element root = doc.getDocumentElement();
        assertEquals("HTML", root.getNodeName());

        // Check that missing tags are properly inferred
        NodeList heads = doc.getElementsByTagName("HEAD");
        assertEquals(1, heads.getLength(), "Should have one head element");

        NodeList bodies = doc.getElementsByTagName("BODY");
        assertEquals(1, bodies.getLength(), "Should have one body element");

        // Check list items are properly closed
        NodeList listItems = doc.getElementsByTagName("LI");
        assertEquals(3, listItems.getLength(), "Should have three list items");

        // Check paragraphs are properly closed (including those in sections and articles)
        NodeList paragraphs = doc.getElementsByTagName("P");
        assertTrue(paragraphs.getLength() >= 2, "Should have at least two paragraphs");
    }

    @Test
    @DisplayName("HTML5 P tag omission should work correctly")
    void testHTML5PTagOmission() throws IOException, SAXException {
        String html = "<!DOCTYPE html><p>First paragraph<div>Block content</div><p>Second paragraph";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();
        NodeList paragraphs = doc.getElementsByTagName("P");
        assertEquals(2, paragraphs.getLength(), "Should have two paragraphs with auto-closed first P");

        NodeList divs = doc.getElementsByTagName("DIV");
        assertEquals(1, divs.getLength(), "Should have one div element");
    }

    @Test
    @DisplayName("HTML5 LI tag omission should work correctly")
    void testHTML5LITagOmission() throws IOException, SAXException {
        String html = "<!DOCTYPE html><ul><li>First item<li>Second item<li>Third item</ul>";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();
        NodeList listItems = doc.getElementsByTagName("LI");
        assertEquals(3, listItems.getLength(), "Should have three list items with auto-closed LI tags");

        Element firstLI = (Element) listItems.item(0);
        assertEquals("First item", firstLI.getTextContent().trim());
    }

    @Test
    @DisplayName("HTML5 DT/DD tag omission should work correctly")
    void testHTML5DTDDTagOmission() throws IOException, SAXException {
        String html = "<!DOCTYPE html><dl><dt>Term 1<dd>Definition 1<dt>Term 2<dd>Definition 2</dl>";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();
        NodeList dts = doc.getElementsByTagName("DT");
        assertEquals(2, dts.getLength(), "Should have two dt elements");

        NodeList dds = doc.getElementsByTagName("DD");
        assertEquals(2, dds.getLength(), "Should have two dd elements");
    }

    @Test
    @DisplayName("HTML5 table cell omission should work correctly")
    void testHTML5TableCellOmission() throws IOException, SAXException {
        String html = "<!DOCTYPE html><table><tr><td>Cell 1<td>Cell 2<th>Header 1<th>Header 2</tr></table>";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();
        NodeList tds = doc.getElementsByTagName("TD");
        assertEquals(2, tds.getLength(), "Should have two td elements");

        NodeList ths = doc.getElementsByTagName("TH");
        assertEquals(2, ths.getLength(), "Should have two th elements");
    }

    @Test
    @DisplayName("HTML5 canonical output should match expected")
    void testHTML5OptionalTagsCanonical() throws IOException {
        HTMLConfiguration config = new HTMLConfiguration();
        StringWriter writer = new StringWriter();
        Writer filter = new Writer(writer, "UTF-8");
        config.setDocumentHandler(filter);

        XMLInputSource source =
                new XMLInputSource(null, TEST_DATA_DIR + "test-html5-optional-tags.html", null, new FileReader(TEST_DATA_DIR
                        + "test-html5-optional-tags.html"), "UTF-8");
        config.parse(source);

        String result = writer.toString();

        // Check key elements are present in output
        assertTrue(result.contains("<HTML"), "Should contain HTML element");
        assertTrue(result.contains("<HEAD"), "Should contain HEAD element");
        assertTrue(result.contains("<BODY"), "Should contain BODY element");
        assertTrue(result.contains("<P"), "Should contain P elements");
        assertTrue(result.contains("<LI"), "Should contain LI elements");
        assertTrue(result.contains("<DT"), "Should contain DT elements");
        assertTrue(result.contains("<DD"), "Should contain DD elements");
    }
}
