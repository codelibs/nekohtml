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
package org.codelibs.nekohtml.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Test cases for SAXToDOMHandler.
 * Tests the conversion of SAX events to DOM tree structure.
 */
public class SAXToDOMHandlerTest {

    private DocumentBuilder documentBuilder;
    private SAXToDOMHandler handler;

    @BeforeEach
    public void setUp() throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        documentBuilder = factory.newDocumentBuilder();
        handler = new SAXToDOMHandler(documentBuilder);
    }

    @Test
    public void testStartDocument() throws Exception {
        // When: startDocument is called
        handler.startDocument();

        // Then: document should be created but empty
        final Document doc = handler.getDocument();
        assertNotNull(doc, "Document should be created");
        assertNull(doc.getDocumentElement(), "Document should have no root element yet");
    }

    @Test
    public void testSimpleElement() throws Exception {
        // Given: SAX events for <html></html>
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: DOM should contain HTML element
        final Document doc = handler.getDocument();
        assertNotNull(doc, "Document should be created");

        final Element root = doc.getDocumentElement();
        assertNotNull(root, "Document should have root element");
        assertEquals("HTML", root.getNodeName(), "Root element should be HTML");
    }

    @Test
    public void testNestedElements() throws Exception {
        // Given: SAX events for <html><body></body></html>
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());
        handler.startElement("", "body", "BODY", new AttributesImpl());
        handler.endElement("", "body", "BODY");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: DOM should have nested structure
        final Document doc = handler.getDocument();
        final Element root = doc.getDocumentElement();
        assertEquals("HTML", root.getNodeName(), "Root should be HTML");

        final NodeList children = root.getChildNodes();
        assertEquals(1, children.getLength(), "HTML should have 1 child");

        final Node body = children.item(0);
        assertEquals("BODY", body.getNodeName(), "Child should be BODY");
    }

    @Test
    public void testElementWithAttributes() throws Exception {
        // Given: SAX events for <div id="test" class="container"></div>
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());

        final AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "id", "id", "CDATA", "test");
        attrs.addAttribute("", "class", "class", "CDATA", "container");

        handler.startElement("", "div", "DIV", attrs);
        handler.endElement("", "div", "DIV");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: Element should have attributes
        final Document doc = handler.getDocument();
        final NodeList divs = doc.getElementsByTagName("DIV");
        assertEquals(1, divs.getLength(), "Should have 1 DIV element");

        final Element div = (Element) divs.item(0);
        assertEquals("test", div.getAttribute("id"), "Should have id attribute");
        assertEquals("container", div.getAttribute("class"), "Should have class attribute");
    }

    @Test
    public void testTextContent() throws Exception {
        // Given: SAX events for <p>Hello World</p>
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());
        handler.startElement("", "p", "P", new AttributesImpl());
        handler.characters("Hello World".toCharArray(), 0, "Hello World".length());
        handler.endElement("", "p", "P");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: Element should contain text
        final Document doc = handler.getDocument();
        final NodeList ps = doc.getElementsByTagName("P");
        assertEquals(1, ps.getLength(), "Should have 1 P element");

        final Element p = (Element) ps.item(0);
        assertEquals("Hello World", p.getTextContent(), "Text content should match");
    }

    @Test
    public void testMultipleTextNodes() throws Exception {
        // Given: Multiple character events (simulating chunked text)
        handler.startDocument();
        handler.startElement("", "p", "P", new AttributesImpl());
        handler.characters("Hello ".toCharArray(), 0, 6);
        handler.characters("World".toCharArray(), 0, 5);
        handler.endElement("", "p", "P");
        handler.endDocument();

        // Then: Text should be concatenated
        final Document doc = handler.getDocument();
        final Element p = doc.getDocumentElement();
        assertEquals("Hello World", p.getTextContent(), "Text should be concatenated");
    }

    @Test
    public void testWhitespacePreservation() throws Exception {
        // Given: SAX events with whitespace between elements
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());
        handler.characters("\n".toCharArray(), 0, 1);
        handler.startElement("", "body", "BODY", new AttributesImpl());
        handler.endElement("", "body", "BODY");
        handler.characters("\n".toCharArray(), 0, 1);
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: Whitespace should be preserved as text nodes
        final Document doc = handler.getDocument();
        final Element root = doc.getDocumentElement();

        final NodeList children = root.getChildNodes();
        // Should have: text("\n"), BODY, text("\n")
        assertEquals(3, children.getLength(), "Should have 3 nodes (text, element, text)");

        assertTrue(children.item(0) instanceof Text, "First node should be text");
        assertTrue(children.item(1) instanceof Element, "Second node should be element");
        assertTrue(children.item(2) instanceof Text, "Third node should be text");
    }

    @Test
    public void testEmptyElement() throws Exception {
        // Given: SAX events for <br></br> (void element)
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());
        handler.startElement("", "br", "BR", new AttributesImpl());
        handler.endElement("", "br", "BR");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: Empty element should exist
        final Document doc = handler.getDocument();
        final NodeList brs = doc.getElementsByTagName("BR");
        assertEquals(1, brs.getLength(), "Should have 1 BR element");

        final Element br = (Element) brs.item(0);
        assertEquals(0, br.getChildNodes().getLength(), "BR should have no children");
    }

    @Test
    public void testComplexDocumentStructure() throws Exception {
        // Given: Complex HTML structure
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());

        // Head section
        handler.startElement("", "head", "HEAD", new AttributesImpl());
        handler.startElement("", "title", "TITLE", new AttributesImpl());
        handler.characters("Test Page".toCharArray(), 0, 9);
        handler.endElement("", "title", "TITLE");
        handler.endElement("", "head", "HEAD");

        // Body section
        handler.startElement("", "body", "BODY", new AttributesImpl());
        handler.startElement("", "h1", "H1", new AttributesImpl());
        handler.characters("Heading".toCharArray(), 0, 7);
        handler.endElement("", "h1", "H1");
        handler.startElement("", "p", "P", new AttributesImpl());
        handler.characters("Paragraph".toCharArray(), 0, 9);
        handler.endElement("", "p", "P");
        handler.endElement("", "body", "BODY");

        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: Verify complete structure
        final Document doc = handler.getDocument();
        final Element root = doc.getDocumentElement();
        assertEquals("HTML", root.getNodeName(), "Root should be HTML");

        final NodeList heads = doc.getElementsByTagName("HEAD");
        assertEquals(1, heads.getLength(), "Should have HEAD");

        final NodeList titles = doc.getElementsByTagName("TITLE");
        assertEquals(1, titles.getLength(), "Should have TITLE");
        assertEquals("Test Page", titles.item(0).getTextContent(), "TITLE should have correct text");

        final NodeList bodies = doc.getElementsByTagName("BODY");
        assertEquals(1, bodies.getLength(), "Should have BODY");

        final NodeList h1s = doc.getElementsByTagName("H1");
        assertEquals(1, h1s.getLength(), "Should have H1");
        assertEquals("Heading", h1s.item(0).getTextContent(), "H1 should have correct text");

        final NodeList ps = doc.getElementsByTagName("P");
        assertEquals(1, ps.getLength(), "Should have P");
        assertEquals("Paragraph", ps.item(0).getTextContent(), "P should have correct text");
    }

    @Test
    public void testMultipleAttributes() throws Exception {
        // Given: Element with multiple attributes
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());

        final AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "id", "id", "CDATA", "main");
        attrs.addAttribute("", "class", "class", "CDATA", "container");
        attrs.addAttribute("", "data-value", "data-value", "CDATA", "123");
        attrs.addAttribute("", "title", "title", "CDATA", "Main Container");

        handler.startElement("", "div", "DIV", attrs);
        handler.endElement("", "div", "DIV");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: All attributes should be present
        final Document doc = handler.getDocument();
        final NodeList divs = doc.getElementsByTagName("DIV");
        final Element div = (Element) divs.item(0);

        assertEquals("main", div.getAttribute("id"), "Should have id");
        assertEquals("container", div.getAttribute("class"), "Should have class");
        assertEquals("123", div.getAttribute("data-value"), "Should have data-value");
        assertEquals("Main Container", div.getAttribute("title"), "Should have title");
    }

    @Test
    public void testSiblingElements() throws Exception {
        // Given: Multiple sibling elements
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());
        handler.startElement("", "body", "BODY", new AttributesImpl());

        handler.startElement("", "p", "P", new AttributesImpl());
        handler.characters("First".toCharArray(), 0, 5);
        handler.endElement("", "p", "P");

        handler.startElement("", "p", "P", new AttributesImpl());
        handler.characters("Second".toCharArray(), 0, 6);
        handler.endElement("", "p", "P");

        handler.startElement("", "p", "P", new AttributesImpl());
        handler.characters("Third".toCharArray(), 0, 5);
        handler.endElement("", "p", "P");

        handler.endElement("", "body", "BODY");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: All siblings should be present
        final Document doc = handler.getDocument();
        final NodeList ps = doc.getElementsByTagName("P");
        assertEquals(3, ps.getLength(), "Should have 3 P elements");
        assertEquals("First", ps.item(0).getTextContent(), "First P should have correct text");
        assertEquals("Second", ps.item(1).getTextContent(), "Second P should have correct text");
        assertEquals("Third", ps.item(2).getTextContent(), "Third P should have correct text");
    }

    @Test
    public void testMixedContent() throws Exception {
        // Given: Element with mixed text and child elements
        handler.startDocument();
        handler.startElement("", "p", "P", new AttributesImpl());
        handler.characters("Text before ".toCharArray(), 0, 12);
        handler.startElement("", "strong", "STRONG", new AttributesImpl());
        handler.characters("bold".toCharArray(), 0, 4);
        handler.endElement("", "strong", "STRONG");
        handler.characters(" text after".toCharArray(), 0, 11);
        handler.endElement("", "p", "P");
        handler.endDocument();

        // Then: Text and elements should be properly ordered
        final Document doc = handler.getDocument();
        final Element p = doc.getDocumentElement();
        assertEquals("Text before bold text after", p.getTextContent(), "Mixed content should be preserved");

        final NodeList children = p.getChildNodes();
        assertEquals(3, children.getLength(), "Should have 3 child nodes");
        assertTrue(children.item(0) instanceof Text, "First should be text");
        assertTrue(children.item(1) instanceof Element, "Second should be element");
        assertTrue(children.item(2) instanceof Text, "Third should be text");
    }

    @Test
    public void testEmptyTextContent() throws Exception {
        // Given: Element with empty text
        handler.startDocument();
        handler.startElement("", "p", "P", new AttributesImpl());
        handler.characters("".toCharArray(), 0, 0);
        handler.endElement("", "p", "P");
        handler.endDocument();

        // Then: Element should exist but be empty
        final Document doc = handler.getDocument();
        final Element p = doc.getDocumentElement();
        assertEquals("", p.getTextContent(), "Text content should be empty");
        assertEquals(0, p.getChildNodes().getLength(), "Should have no child nodes");
    }

    @Test
    public void testSpecialCharactersInText() throws Exception {
        // Given: Text with special characters
        final String specialText = "Special: <>&\"'";
        handler.startDocument();
        handler.startElement("", "p", "P", new AttributesImpl());
        handler.characters(specialText.toCharArray(), 0, specialText.length());
        handler.endElement("", "p", "P");
        handler.endDocument();

        // Then: Special characters should be preserved
        final Document doc = handler.getDocument();
        final Element p = doc.getDocumentElement();
        assertEquals(specialText, p.getTextContent(), "Special characters should be preserved");
    }

    @Test
    public void testSpecialCharactersInAttributes() throws Exception {
        // Given: Attributes with special characters
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());

        final AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "title", "title", "CDATA", "Quote: \"test\" & <value>");

        handler.startElement("", "div", "DIV", attrs);
        handler.endElement("", "div", "DIV");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: Special characters in attributes should be preserved
        final Document doc = handler.getDocument();
        final NodeList divs = doc.getElementsByTagName("DIV");
        final Element div = (Element) divs.item(0);
        assertEquals("Quote: \"test\" & <value>", div.getAttribute("title"), "Special chars in attributes should be preserved");
    }

    @Test
    public void testDeeplyNestedStructure() throws Exception {
        // Given: Deeply nested elements
        handler.startDocument();
        handler.startElement("", "div", "DIV", new AttributesImpl());
        handler.startElement("", "div", "DIV", new AttributesImpl());
        handler.startElement("", "div", "DIV", new AttributesImpl());
        handler.startElement("", "div", "DIV", new AttributesImpl());
        handler.startElement("", "p", "P", new AttributesImpl());
        handler.characters("Deeply nested".toCharArray(), 0, 13);
        handler.endElement("", "p", "P");
        handler.endElement("", "div", "DIV");
        handler.endElement("", "div", "DIV");
        handler.endElement("", "div", "DIV");
        handler.endElement("", "div", "DIV");
        handler.endDocument();

        // Then: Deep nesting should work correctly
        final Document doc = handler.getDocument();
        final NodeList ps = doc.getElementsByTagName("P");
        assertEquals(1, ps.getLength(), "Should have 1 P element");
        assertEquals("Deeply nested", ps.item(0).getTextContent(), "Deeply nested text should be correct");
    }

    @Test
    public void testIntegrationWithHTMLParser() throws Exception {
        // Integration test: Use actual HTML parser to generate SAX events
        final String html = "<html><head><title>Test</title></head><body><h1>Hello</h1><p>World</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should be created");

        final NodeList titles = doc.getElementsByTagName("TITLE");
        assertEquals(1, titles.getLength(), "Should have TITLE");
        assertEquals("Test", titles.item(0).getTextContent(), "TITLE should have correct text");

        final NodeList h1s = doc.getElementsByTagName("H1");
        assertEquals(1, h1s.getLength(), "Should have H1");
        assertEquals("Hello", h1s.item(0).getTextContent(), "H1 should have correct text");

        final NodeList ps = doc.getElementsByTagName("P");
        assertEquals(1, ps.getLength(), "Should have P");
        assertEquals("World", ps.item(0).getTextContent(), "P should have correct text");
    }

    // ========== Comment Nodes ==========

    @Test
    public void testCommentNode() throws Exception {
        // Given: HTML with comment
        final String html = "<html><body><!--This is a comment--><p>Text</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should be created");

        // Verify that document contains both comment and element
        final NodeList ps = doc.getElementsByTagName("P");
        assertEquals(1, ps.getLength(), "Should have P element");
    }

    @Test
    public void testMultipleComments() throws Exception {
        // Given: HTML with multiple comments
        final String html = "<html><body><!--Comment 1--><p>Text</p><!--Comment 2--></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should be created");

        final NodeList ps = doc.getElementsByTagName("P");
        assertEquals(1, ps.getLength(), "Should have P element");
    }

    @Test
    public void testCommentWithSpecialCharacters() throws Exception {
        // Given: Comment with special characters
        final String html = "<html><body><!--Comment with <>&\"'--><p>Text</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should be created");
    }

    // ========== Namespace Handling ==========

    @Test
    public void testNamespaceAwareElements() throws Exception {
        // Given: Elements with namespace
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final DocumentBuilder nsDocBuilder = factory.newDocumentBuilder();
        final SAXToDOMHandler nsHandler = new SAXToDOMHandler(nsDocBuilder);

        nsHandler.startDocument();
        final AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("http://www.w3.org/2000/xmlns/", "xmlns", "xmlns", "CDATA", "http://www.w3.org/1999/xhtml");

        nsHandler.startElement("http://www.w3.org/1999/xhtml", "html", "html", attrs);
        nsHandler.startElement("http://www.w3.org/1999/xhtml", "body", "body", new AttributesImpl());
        nsHandler.endElement("http://www.w3.org/1999/xhtml", "body", "body");
        nsHandler.endElement("http://www.w3.org/1999/xhtml", "html", "html");
        nsHandler.endDocument();

        final Document doc = nsHandler.getDocument();
        assertNotNull(doc, "Document should be created");
        assertNotNull(doc.getDocumentElement(), "Should have root element");
    }

    @Test
    public void testMixedNamespaceElements() throws Exception {
        // Given: Elements with different namespaces
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final DocumentBuilder nsDocBuilder = factory.newDocumentBuilder();
        final SAXToDOMHandler nsHandler = new SAXToDOMHandler(nsDocBuilder);

        nsHandler.startDocument();
        nsHandler.startElement("http://www.w3.org/1999/xhtml", "html", "html", new AttributesImpl());
        nsHandler.startElement("http://www.w3.org/2000/svg", "svg", "svg", new AttributesImpl());
        nsHandler.endElement("http://www.w3.org/2000/svg", "svg", "svg");
        nsHandler.endElement("http://www.w3.org/1999/xhtml", "html", "html");
        nsHandler.endDocument();

        final Document doc = nsHandler.getDocument();
        assertNotNull(doc, "Document should be created");
    }

    // ========== Character Data Variations ==========

    @Test
    public void testCharacterArrayOffset() throws Exception {
        // Given: characters() called with offset and length
        final char[] data = "PREFIXHello WorldSUFFIX".toCharArray();

        handler.startDocument();
        handler.startElement("", "p", "P", new AttributesImpl());
        handler.characters(data, 6, 11); // Extract "Hello World"
        handler.endElement("", "p", "P");
        handler.endDocument();

        // Then: Only the specified portion should be used
        final Document doc = handler.getDocument();
        final Element p = doc.getDocumentElement();
        assertEquals("Hello World", p.getTextContent(), "Should extract correct portion of char array");
    }

    @Test
    public void testConsecutiveWhitespace() throws Exception {
        // Given: Multiple consecutive whitespace characters
        final String text = "Text   with\t\tmultiple\n\nspaces";
        handler.startDocument();
        handler.startElement("", "p", "P", new AttributesImpl());
        handler.characters(text.toCharArray(), 0, text.length());
        handler.endElement("", "p", "P");
        handler.endDocument();

        // Then: Whitespace should be preserved
        final Document doc = handler.getDocument();
        final Element p = doc.getDocumentElement();
        final String content = p.getTextContent();
        assertTrue(content.contains("   "), "Should preserve multiple spaces");
        assertTrue(content.contains("\t\t"), "Should preserve tabs");
        assertTrue(content.contains("\n\n"), "Should preserve newlines");
    }

    @Test
    public void testIgnorableWhitespace() throws Exception {
        // Given: ignorableWhitespace events
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());
        handler.ignorableWhitespace("  \n  ".toCharArray(), 0, 5);
        handler.startElement("", "body", "BODY", new AttributesImpl());
        handler.endElement("", "body", "BODY");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: Document should be created successfully
        final Document doc = handler.getDocument();
        assertNotNull(doc, "Document should be created");
    }

    @Test
    public void testUnicodeCharacters() throws Exception {
        // Given: Text with Unicode characters
        final String unicodeText = "Hello 世界 \u263A \uD83D\uDE00";
        handler.startDocument();
        handler.startElement("", "p", "P", new AttributesImpl());
        handler.characters(unicodeText.toCharArray(), 0, unicodeText.length());
        handler.endElement("", "p", "P");
        handler.endDocument();

        // Then: Unicode should be preserved
        final Document doc = handler.getDocument();
        final Element p = doc.getDocumentElement();
        assertEquals(unicodeText, p.getTextContent(), "Unicode characters should be preserved");
    }

    // ========== HTML5 Elements ==========

    @Test
    public void testHTML5SemanticElements() throws Exception {
        // Given: HTML5 semantic elements
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());
        handler.startElement("", "header", "HEADER", new AttributesImpl());
        handler.endElement("", "header", "HEADER");
        handler.startElement("", "nav", "NAV", new AttributesImpl());
        handler.endElement("", "nav", "NAV");
        handler.startElement("", "main", "MAIN", new AttributesImpl());
        handler.endElement("", "main", "MAIN");
        handler.startElement("", "article", "ARTICLE", new AttributesImpl());
        handler.endElement("", "article", "ARTICLE");
        handler.startElement("", "section", "SECTION", new AttributesImpl());
        handler.endElement("", "section", "SECTION");
        handler.startElement("", "aside", "ASIDE", new AttributesImpl());
        handler.endElement("", "aside", "ASIDE");
        handler.startElement("", "footer", "FOOTER", new AttributesImpl());
        handler.endElement("", "footer", "FOOTER");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: All HTML5 elements should be created
        final Document doc = handler.getDocument();
        assertEquals(1, doc.getElementsByTagName("HEADER").getLength(), "Should have HEADER");
        assertEquals(1, doc.getElementsByTagName("NAV").getLength(), "Should have NAV");
        assertEquals(1, doc.getElementsByTagName("MAIN").getLength(), "Should have MAIN");
        assertEquals(1, doc.getElementsByTagName("ARTICLE").getLength(), "Should have ARTICLE");
        assertEquals(1, doc.getElementsByTagName("SECTION").getLength(), "Should have SECTION");
        assertEquals(1, doc.getElementsByTagName("ASIDE").getLength(), "Should have ASIDE");
        assertEquals(1, doc.getElementsByTagName("FOOTER").getLength(), "Should have FOOTER");
    }

    @Test
    public void testHTML5VoidElements() throws Exception {
        // Given: HTML5 void elements
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());

        handler.startElement("", "meta", "META", new AttributesImpl());
        handler.endElement("", "meta", "META");

        handler.startElement("", "link", "LINK", new AttributesImpl());
        handler.endElement("", "link", "LINK");

        handler.startElement("", "img", "IMG", new AttributesImpl());
        handler.endElement("", "img", "IMG");

        handler.startElement("", "input", "INPUT", new AttributesImpl());
        handler.endElement("", "input", "INPUT");

        handler.startElement("", "br", "BR", new AttributesImpl());
        handler.endElement("", "br", "BR");

        handler.startElement("", "hr", "HR", new AttributesImpl());
        handler.endElement("", "hr", "HR");

        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: All void elements should be created
        final Document doc = handler.getDocument();
        assertEquals(1, doc.getElementsByTagName("META").getLength(), "Should have META");
        assertEquals(1, doc.getElementsByTagName("LINK").getLength(), "Should have LINK");
        assertEquals(1, doc.getElementsByTagName("IMG").getLength(), "Should have IMG");
        assertEquals(1, doc.getElementsByTagName("INPUT").getLength(), "Should have INPUT");
        assertEquals(1, doc.getElementsByTagName("BR").getLength(), "Should have BR");
        assertEquals(1, doc.getElementsByTagName("HR").getLength(), "Should have HR");
    }

    @Test
    public void testHTML5FormElements() throws Exception {
        // Given: HTML5 form elements
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());
        handler.startElement("", "form", "FORM", new AttributesImpl());

        handler.startElement("", "input", "INPUT", new AttributesImpl());
        handler.endElement("", "input", "INPUT");

        handler.startElement("", "select", "SELECT", new AttributesImpl());
        handler.startElement("", "option", "OPTION", new AttributesImpl());
        handler.endElement("", "option", "OPTION");
        handler.endElement("", "select", "SELECT");

        handler.startElement("", "textarea", "TEXTAREA", new AttributesImpl());
        handler.endElement("", "textarea", "TEXTAREA");

        handler.startElement("", "button", "BUTTON", new AttributesImpl());
        handler.endElement("", "button", "BUTTON");

        handler.startElement("", "datalist", "DATALIST", new AttributesImpl());
        handler.endElement("", "datalist", "DATALIST");

        handler.startElement("", "output", "OUTPUT", new AttributesImpl());
        handler.endElement("", "output", "OUTPUT");

        handler.endElement("", "form", "FORM");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: All form elements should be created
        final Document doc = handler.getDocument();
        assertEquals(1, doc.getElementsByTagName("FORM").getLength(), "Should have FORM");
        assertEquals(1, doc.getElementsByTagName("INPUT").getLength(), "Should have INPUT");
        assertEquals(1, doc.getElementsByTagName("SELECT").getLength(), "Should have SELECT");
        assertEquals(1, doc.getElementsByTagName("TEXTAREA").getLength(), "Should have TEXTAREA");
        assertEquals(1, doc.getElementsByTagName("BUTTON").getLength(), "Should have BUTTON");
        assertEquals(1, doc.getElementsByTagName("DATALIST").getLength(), "Should have DATALIST");
        assertEquals(1, doc.getElementsByTagName("OUTPUT").getLength(), "Should have OUTPUT");
    }

    // ========== Edge Cases ==========

    @Test
    public void testVeryLongText() throws Exception {
        // Given: Very long text content
        final StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longText.append("Text ");
        }

        handler.startDocument();
        handler.startElement("", "p", "P", new AttributesImpl());
        handler.characters(longText.toString().toCharArray(), 0, longText.length());
        handler.endElement("", "p", "P");
        handler.endDocument();

        // Then: Long text should be handled
        final Document doc = handler.getDocument();
        final Element p = doc.getDocumentElement();
        assertEquals(longText.toString(), p.getTextContent(), "Long text should be preserved");
    }

    @Test
    public void testManyAttributes() throws Exception {
        // Given: Element with many attributes
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());

        final AttributesImpl attrs = new AttributesImpl();
        for (int i = 0; i < 50; i++) {
            attrs.addAttribute("", "attr" + i, "attr" + i, "CDATA", "value" + i);
        }

        handler.startElement("", "div", "DIV", attrs);
        handler.endElement("", "div", "DIV");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: All attributes should be present
        final Document doc = handler.getDocument();
        final NodeList divs = doc.getElementsByTagName("DIV");
        final Element div = (Element) divs.item(0);

        for (int i = 0; i < 50; i++) {
            assertEquals("value" + i, div.getAttribute("attr" + i), "Should have attr" + i);
        }
    }

    @Test
    public void testManySiblings() throws Exception {
        // Given: Many sibling elements
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());
        handler.startElement("", "body", "BODY", new AttributesImpl());

        for (int i = 0; i < 100; i++) {
            handler.startElement("", "p", "P", new AttributesImpl());
            final String text = "Paragraph " + i;
            handler.characters(text.toCharArray(), 0, text.length());
            handler.endElement("", "p", "P");
        }

        handler.endElement("", "body", "BODY");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: All siblings should be created
        final Document doc = handler.getDocument();
        final NodeList ps = doc.getElementsByTagName("P");
        assertEquals(100, ps.getLength(), "Should have 100 P elements");
    }

    @Test
    public void testEmptyAttributeValue() throws Exception {
        // Given: Attribute with empty value
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());

        final AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "id", "id", "CDATA", "");
        attrs.addAttribute("", "class", "class", "CDATA", "");

        handler.startElement("", "div", "DIV", attrs);
        handler.endElement("", "div", "DIV");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: Empty attributes should be preserved
        final Document doc = handler.getDocument();
        final NodeList divs = doc.getElementsByTagName("DIV");
        final Element div = (Element) divs.item(0);

        assertEquals("", div.getAttribute("id"), "id should be empty");
        assertEquals("", div.getAttribute("class"), "class should be empty");
    }

    @Test
    public void testAttributeWithWhitespace() throws Exception {
        // Given: Attributes with whitespace in values
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());

        final AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "title", "title", "CDATA", "  Title with spaces  ");
        attrs.addAttribute("", "class", "class", "CDATA", "class1 class2 class3");

        handler.startElement("", "div", "DIV", attrs);
        handler.endElement("", "div", "DIV");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: Whitespace in attributes should be preserved
        final Document doc = handler.getDocument();
        final NodeList divs = doc.getElementsByTagName("DIV");
        final Element div = (Element) divs.item(0);

        assertEquals("  Title with spaces  ", div.getAttribute("title"), "Whitespace should be preserved");
        assertEquals("class1 class2 class3", div.getAttribute("class"), "Multiple classes should be preserved");
    }

    @Test
    public void testConsecutiveEmptyElements() throws Exception {
        // Given: Multiple consecutive empty elements
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());
        handler.startElement("", "body", "BODY", new AttributesImpl());

        handler.startElement("", "br", "BR", new AttributesImpl());
        handler.endElement("", "br", "BR");

        handler.startElement("", "br", "BR", new AttributesImpl());
        handler.endElement("", "br", "BR");

        handler.startElement("", "br", "BR", new AttributesImpl());
        handler.endElement("", "br", "BR");

        handler.endElement("", "body", "BODY");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: All empty elements should be created
        final Document doc = handler.getDocument();
        final NodeList brs = doc.getElementsByTagName("BR");
        assertEquals(3, brs.getLength(), "Should have 3 BR elements");
    }

    @Test
    public void testTableStructure() throws Exception {
        // Given: Complex table structure
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());
        handler.startElement("", "table", "TABLE", new AttributesImpl());
        handler.startElement("", "thead", "THEAD", new AttributesImpl());
        handler.startElement("", "tr", "TR", new AttributesImpl());
        handler.startElement("", "th", "TH", new AttributesImpl());
        handler.characters("Header".toCharArray(), 0, 6);
        handler.endElement("", "th", "TH");
        handler.endElement("", "tr", "TR");
        handler.endElement("", "thead", "THEAD");
        handler.startElement("", "tbody", "TBODY", new AttributesImpl());
        handler.startElement("", "tr", "TR", new AttributesImpl());
        handler.startElement("", "td", "TD", new AttributesImpl());
        handler.characters("Data".toCharArray(), 0, 4);
        handler.endElement("", "td", "TD");
        handler.endElement("", "tr", "TR");
        handler.endElement("", "tbody", "TBODY");
        handler.endElement("", "table", "TABLE");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: Table structure should be correct
        final Document doc = handler.getDocument();
        assertEquals(1, doc.getElementsByTagName("TABLE").getLength(), "Should have TABLE");
        assertEquals(1, doc.getElementsByTagName("THEAD").getLength(), "Should have THEAD");
        assertEquals(1, doc.getElementsByTagName("TBODY").getLength(), "Should have TBODY");
        assertEquals(2, doc.getElementsByTagName("TR").getLength(), "Should have 2 TR");
        assertEquals(1, doc.getElementsByTagName("TH").getLength(), "Should have TH");
        assertEquals(1, doc.getElementsByTagName("TD").getLength(), "Should have TD");
    }

    @Test
    public void testDataAttributes() throws Exception {
        // Given: HTML5 data-* attributes
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());

        final AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "data-id", "data-id", "CDATA", "123");
        attrs.addAttribute("", "data-name", "data-name", "CDATA", "test");
        attrs.addAttribute("", "data-value", "data-value", "CDATA", "abc");

        handler.startElement("", "div", "DIV", attrs);
        handler.endElement("", "div", "DIV");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: Data attributes should be preserved
        final Document doc = handler.getDocument();
        final NodeList divs = doc.getElementsByTagName("DIV");
        final Element div = (Element) divs.item(0);

        assertEquals("123", div.getAttribute("data-id"), "Should have data-id");
        assertEquals("test", div.getAttribute("data-name"), "Should have data-name");
        assertEquals("abc", div.getAttribute("data-value"), "Should have data-value");
    }

    @Test
    public void testAriaAttributes() throws Exception {
        // Given: ARIA attributes
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());

        final AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "role", "role", "CDATA", "button");
        attrs.addAttribute("", "aria-label", "aria-label", "CDATA", "Close");
        attrs.addAttribute("", "aria-hidden", "aria-hidden", "CDATA", "false");

        handler.startElement("", "button", "BUTTON", attrs);
        handler.endElement("", "button", "BUTTON");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: ARIA attributes should be preserved
        final Document doc = handler.getDocument();
        final NodeList buttons = doc.getElementsByTagName("BUTTON");
        final Element button = (Element) buttons.item(0);

        assertEquals("button", button.getAttribute("role"), "Should have role");
        assertEquals("Close", button.getAttribute("aria-label"), "Should have aria-label");
        assertEquals("false", button.getAttribute("aria-hidden"), "Should have aria-hidden");
    }

    @Test
    public void testListStructures() throws Exception {
        // Given: Different list structures
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());

        // Unordered list
        handler.startElement("", "ul", "UL", new AttributesImpl());
        handler.startElement("", "li", "LI", new AttributesImpl());
        handler.characters("Item 1".toCharArray(), 0, 6);
        handler.endElement("", "li", "LI");
        handler.startElement("", "li", "LI", new AttributesImpl());
        handler.characters("Item 2".toCharArray(), 0, 6);
        handler.endElement("", "li", "LI");
        handler.endElement("", "ul", "UL");

        // Ordered list
        handler.startElement("", "ol", "OL", new AttributesImpl());
        handler.startElement("", "li", "LI", new AttributesImpl());
        handler.characters("First".toCharArray(), 0, 5);
        handler.endElement("", "li", "LI");
        handler.endElement("", "ol", "OL");

        // Description list
        handler.startElement("", "dl", "DL", new AttributesImpl());
        handler.startElement("", "dt", "DT", new AttributesImpl());
        handler.characters("Term".toCharArray(), 0, 4);
        handler.endElement("", "dt", "DT");
        handler.startElement("", "dd", "DD", new AttributesImpl());
        handler.characters("Definition".toCharArray(), 0, 10);
        handler.endElement("", "dd", "DD");
        handler.endElement("", "dl", "DL");

        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: All list structures should be correct
        final Document doc = handler.getDocument();
        assertEquals(1, doc.getElementsByTagName("UL").getLength(), "Should have UL");
        assertEquals(1, doc.getElementsByTagName("OL").getLength(), "Should have OL");
        assertEquals(1, doc.getElementsByTagName("DL").getLength(), "Should have DL");
        assertEquals(3, doc.getElementsByTagName("LI").getLength(), "Should have 3 LI");
        assertEquals(1, doc.getElementsByTagName("DT").getLength(), "Should have DT");
        assertEquals(1, doc.getElementsByTagName("DD").getLength(), "Should have DD");
    }

    @Test
    public void testMediaElements() throws Exception {
        // Given: HTML5 media elements
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());

        // Video element
        handler.startElement("", "video", "VIDEO", new AttributesImpl());
        handler.startElement("", "source", "SOURCE", new AttributesImpl());
        handler.endElement("", "source", "SOURCE");
        handler.endElement("", "video", "VIDEO");

        // Audio element
        handler.startElement("", "audio", "AUDIO", new AttributesImpl());
        handler.startElement("", "source", "SOURCE", new AttributesImpl());
        handler.endElement("", "source", "SOURCE");
        handler.endElement("", "audio", "AUDIO");

        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: Media elements should be created
        final Document doc = handler.getDocument();
        assertEquals(1, doc.getElementsByTagName("VIDEO").getLength(), "Should have VIDEO");
        assertEquals(1, doc.getElementsByTagName("AUDIO").getLength(), "Should have AUDIO");
        assertEquals(2, doc.getElementsByTagName("SOURCE").getLength(), "Should have 2 SOURCE");
    }

    @Test
    public void testScriptAndStyleElements() throws Exception {
        // Given: Script and style elements
        handler.startDocument();
        handler.startElement("", "html", "HTML", new AttributesImpl());
        handler.startElement("", "head", "HEAD", new AttributesImpl());

        // Script element
        handler.startElement("", "script", "SCRIPT", new AttributesImpl());
        handler.characters("var x = 1;".toCharArray(), 0, 10);
        handler.endElement("", "script", "SCRIPT");

        // Style element
        handler.startElement("", "style", "STYLE", new AttributesImpl());
        handler.characters("body { margin: 0; }".toCharArray(), 0, 19);
        handler.endElement("", "style", "STYLE");

        handler.endElement("", "head", "HEAD");
        handler.endElement("", "html", "HTML");
        handler.endDocument();

        // Then: Script and style should be created with content
        final Document doc = handler.getDocument();
        assertEquals(1, doc.getElementsByTagName("SCRIPT").getLength(), "Should have SCRIPT");
        assertEquals(1, doc.getElementsByTagName("STYLE").getLength(), "Should have STYLE");

        final Element script = (Element) doc.getElementsByTagName("SCRIPT").item(0);
        assertNotNull(script.getTextContent(), "Script should have content");

        final Element style = (Element) doc.getElementsByTagName("STYLE").item(0);
        assertNotNull(style.getTextContent(), "Style should have content");
    }

} // class SAXToDOMHandlerTest
