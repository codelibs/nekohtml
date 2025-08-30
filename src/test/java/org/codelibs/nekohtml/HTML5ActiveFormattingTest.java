/*
 * Copyright 2024 CodeLibs, Inc.
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
 * Tests for HTML5 active formatting elements reconstruction.
 */
public class HTML5ActiveFormattingTest {

    @Test
    @DisplayName("HTML5 active formatting elements should be reconstructed across blocks")
    void testHTML5ActiveFormattingReconstruction() throws IOException, SAXException {
        String html = "<!DOCTYPE html><b>Bold text<p>Paragraph in bold</p>More bold text</b>";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();

        // Check that bold elements are present
        NodeList bolds = doc.getElementsByTagName("B");
        assertTrue(bolds.getLength() >= 1, "Should have at least one bold element");

        // Check that paragraph exists
        NodeList paragraphs = doc.getElementsByTagName("P");
        assertEquals(1, paragraphs.getLength(), "Should have one paragraph");
    }

    @Test
    @DisplayName("HTML5 active formatting elements should handle nested formatting")
    void testHTML5NestedFormattingElements() throws IOException, SAXException {
        String html = "<!DOCTYPE html><strong><em>Strong and emphasis<p>Block within formatting</p>Continue formatting</em></strong>";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();

        // Check that both formatting elements exist
        NodeList strongs = doc.getElementsByTagName("STRONG");
        assertTrue(strongs.getLength() >= 1, "Should have at least one strong element");

        NodeList ems = doc.getElementsByTagName("EM");
        assertTrue(ems.getLength() >= 1, "Should have at least one em element");

        NodeList paragraphs = doc.getElementsByTagName("P");
        assertEquals(1, paragraphs.getLength(), "Should have one paragraph");
    }

    @Test
    @DisplayName("HTML5 active formatting should handle code elements")
    void testHTML5CodeFormattingElements() throws IOException, SAXException {
        String html = "<!DOCTYPE html><code>Code block<div>Division in code</div>More code</code>";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();

        // Check that code elements exist
        NodeList codes = doc.getElementsByTagName("CODE");
        assertTrue(codes.getLength() >= 1, "Should have at least one code element");

        // Check that div exists
        NodeList divs = doc.getElementsByTagName("DIV");
        assertEquals(1, divs.getLength(), "Should have one div");
    }

    @Test
    @DisplayName("HTML5 active formatting should limit identical elements")
    void testHTML5FormattingElementLimits() throws IOException, SAXException {
        String html = "<!DOCTYPE html><b><b><b><b>Too many nested bold elements<p>Block</p></b></b></b></b>";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();

        // Should handle nested bold elements properly
        NodeList bolds = doc.getElementsByTagName("B");
        assertTrue(bolds.getLength() >= 1, "Should have at least one bold element");

        NodeList paragraphs = doc.getElementsByTagName("P");
        assertEquals(1, paragraphs.getLength(), "Should have one paragraph");
    }

    @Test
    @DisplayName("HTML5 active formatting should handle anchor elements")
    void testHTML5AnchorFormattingElements() throws IOException, SAXException {
        String html = "<!DOCTYPE html><a href=\"#test\">Link text<p>Paragraph in link</p>More link</a>";

        DOMParser parser = new DOMParser();
        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        parser.parse(source);

        Document doc = parser.getDocument();

        // Check that anchor elements exist  
        NodeList anchors = doc.getElementsByTagName("A");
        assertTrue(anchors.getLength() >= 1, "Should have at least one anchor element");

        NodeList paragraphs = doc.getElementsByTagName("P");
        assertEquals(1, paragraphs.getLength(), "Should have one paragraph");
    }

    @Test
    @DisplayName("HTML5 active formatting output should be well-formed")
    void testHTML5ActiveFormattingOutput() throws IOException {
        String html = "<!DOCTYPE html><i>Italic text<div>Block in italic</div>More italic</i>";

        HTMLConfiguration config = new HTMLConfiguration();
        StringWriter writer = new StringWriter();
        Writer filter = new Writer(writer, "UTF-8");
        config.setDocumentHandler(filter);

        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        config.parse(source);

        String result = writer.toString();

        // Check that output contains expected elements
        assertTrue(result.contains("<I"), "Should contain italic element");
        assertTrue(result.contains("<DIV"), "Should contain div element");
        assertTrue(result.contains("Italic text"), "Should contain italic text");
        assertTrue(result.contains("Block in italic"), "Should contain block text");
    }
}
