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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.xml.sax.SAXException;
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

    // ========== Error Handling and Edge Cases ==========

    @Test
    public void testStartElementWithoutStartDocument_NonStrictMode() throws Exception {
        // Given: startElement called before startDocument (empty stack scenario)
        // When: Not in strict mode (default)
        final String originalProperty = System.getProperty("nekohtml.dom.strict");
        try {
            System.clearProperty("nekohtml.dom.strict");

            // When: startElement is called before startDocument
            handler.startElement("", "html", "HTML", new AttributesImpl());
            handler.startElement("", "body", "BODY", new AttributesImpl());
            handler.endElement("", "body", "BODY");
            handler.endElement("", "html", "HTML");

            // Then: No exception should be thrown
            // The handler should skip the elements gracefully
        } finally {
            if (originalProperty != null) {
                System.setProperty("nekohtml.dom.strict", originalProperty);
            } else {
                System.clearProperty("nekohtml.dom.strict");
            }
        }
    }

    @Test
    public void testStartElementWithoutStartDocument_StrictMode() {
        // Given: startElement called before startDocument (empty stack scenario)
        // When: In strict mode
        final String originalProperty = System.getProperty("nekohtml.dom.strict");
        try {
            System.setProperty("nekohtml.dom.strict", "true");

            // Then: SAXException should be thrown
            final SAXException exception = assertThrows(SAXException.class, () -> {
                handler.startElement("", "html", "HTML", new AttributesImpl());
            }, "Should throw SAXException in strict mode");

            assertTrue(exception.getMessage().contains("startDocument") || exception.getMessage().contains("not been initialized"),
                    "Exception message should mention startDocument or initialization");
        } finally {
            if (originalProperty != null) {
                System.setProperty("nekohtml.dom.strict", originalProperty);
            } else {
                System.clearProperty("nekohtml.dom.strict");
            }
        }
    }

    @Test
    public void testUnbalancedTags_NonStrictMode() throws Exception {
        // Given: More endElement calls than startElement (causes empty stack)
        // When: Not in strict mode
        final String originalProperty = System.getProperty("nekohtml.dom.strict");
        try {
            System.clearProperty("nekohtml.dom.strict");

            handler.startDocument();
            handler.startElement("", "html", "HTML", new AttributesImpl());
            handler.startElement("", "body", "BODY", new AttributesImpl());
            handler.endElement("", "body", "BODY");
            handler.endElement("", "html", "HTML");

            // Extra endElement call that pops document from stack
            handler.endElement("", "document", "DOCUMENT");

            // Now try to start a new element with empty stack
            handler.startElement("", "div", "DIV", new AttributesImpl());

            // Then: No exception should be thrown
            // The handler should skip the element gracefully
        } finally {
            if (originalProperty != null) {
                System.setProperty("nekohtml.dom.strict", originalProperty);
            } else {
                System.clearProperty("nekohtml.dom.strict");
            }
        }
    }

    @Test
    public void testUnbalancedTags_StrictMode() throws Exception {
        // Given: endElement calls that should properly match elements
        // When: In strict mode
        final String originalProperty = System.getProperty("nekohtml.dom.strict");
        try {
            System.setProperty("nekohtml.dom.strict", "true");

            handler.startDocument();
            handler.startElement("", "html", "HTML", new AttributesImpl());

            // Mismatched endElement call - doesn't match top element (HTML)
            // With the fix, this will NOT pop from the stack since names don't match
            // So the stack still has HTML and document
            handler.endElement("", "document", "DOCUMENT");

            // Now properly close HTML
            handler.endElement("", "html", "HTML");

            // Now close the document element by its actual name
            handler.endElement("", "#document", "#document");

            // Then: SAXException should be thrown when trying to start element with empty stack
            final SAXException exception = assertThrows(SAXException.class, () -> {
                handler.startElement("", "div", "DIV", new AttributesImpl());
            }, "Should throw SAXException in strict mode");

            assertTrue(exception.getMessage().contains("empty element stack"), "Exception message should mention empty stack");
        } finally {
            if (originalProperty != null) {
                System.setProperty("nekohtml.dom.strict", originalProperty);
            } else {
                System.clearProperty("nekohtml.dom.strict");
            }
        }
    }

    @Test
    public void testEmptyStack_ExplicitNonStrictMode() throws Exception {
        // Given: Empty stack scenario with property explicitly set to false
        // When: Property is explicitly set to "false"
        final String originalProperty = System.getProperty("nekohtml.dom.strict");
        try {
            System.setProperty("nekohtml.dom.strict", "false");

            // When: startElement is called before startDocument
            handler.startElement("", "html", "HTML", new AttributesImpl());

            // Then: No exception should be thrown
            // Warning should be logged (but we can't easily verify logging in this test)
        } finally {
            if (originalProperty != null) {
                System.setProperty("nekohtml.dom.strict", originalProperty);
            } else {
                System.clearProperty("nekohtml.dom.strict");
            }
        }
    }

    @Test
    public void testEmptyStackWithChildElements_NonStrictMode() throws Exception {
        // Given: Empty stack with attempt to add parent and children
        // When: Not in strict mode
        final String originalProperty = System.getProperty("nekohtml.dom.strict");
        try {
            System.clearProperty("nekohtml.dom.strict");

            // When: startElement is called before startDocument, followed by children
            handler.startElement("", "html", "HTML", new AttributesImpl());
            handler.startElement("", "body", "BODY", new AttributesImpl());
            handler.startElement("", "p", "P", new AttributesImpl());
            handler.characters("Text".toCharArray(), 0, 4);
            handler.endElement("", "p", "P");
            handler.endElement("", "body", "BODY");
            handler.endElement("", "html", "HTML");

            // Then: All elements should be skipped gracefully, no exception
        } finally {
            if (originalProperty != null) {
                System.setProperty("nekohtml.dom.strict", originalProperty);
            } else {
                System.clearProperty("nekohtml.dom.strict");
            }
        }
    }

    /**
     * Test XPath queries on DOM built by SAXToDOMHandler - Basic element queries
     */
    @Test
    public void testXPathBasicElementQueries() throws Exception {
        final String html =
                "<html lang=\"en\">" + "<head>" + "<title>Test Page</title>" + "<meta name=\"description\" content=\"Test description\">"
                        + "<link rel=\"canonical\" href=\"https://example.com/page\">" + "</head>" + "<body>" + "<h1>Hello World</h1>"
                        + "<p>First paragraph</p>" + "<p>Second paragraph</p>" + "</body>" + "</html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Create XPath
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Test basic element queries
        final String bodyText = (String) xpath.evaluate("//BODY", doc, javax.xml.xpath.XPathConstants.STRING);
        assertTrue(bodyText.contains("Hello World"), "XPath //BODY should return body text");

        final String h1Text = (String) xpath.evaluate("//H1", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Hello World", h1Text.trim(), "XPath //H1 should return H1 text");

        final String titleText = (String) xpath.evaluate("//TITLE", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Test Page", titleText.trim(), "XPath //TITLE should return title text");
    }

    /**
     * Test XPath attribute access
     */
    @Test
    public void testXPathAttributeAccess() throws Exception {
        final String html =
                "<html lang=\"en\">" + "<head>" + "<title>Test Page</title>" + "<meta name=\"description\" content=\"Test description\">"
                        + "<meta name=\"keywords\" content=\"test, xpath, html\">"
                        + "<link rel=\"canonical\" href=\"https://example.com/page\">" + "<link rel=\"stylesheet\" href=\"/styles.css\">"
                        + "</head>" + "<body>" + "<div class=\"container\" id=\"main\">" + "<p>Content</p>" + "</div>" + "</body>"
                        + "</html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Create XPath
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Test attribute access
        final String lang = (String) xpath.evaluate("//HTML/@lang", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("en", lang, "XPath //HTML/@lang should return 'en'");

        final String description =
                (String) xpath.evaluate("//META[@name='description']/@content", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Test description", description, "XPath //META[@name='description']/@content should return description");

        final String canonicalHref = (String) xpath.evaluate("//LINK[@rel='canonical']/@href", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("https://example.com/page", canonicalHref, "XPath //LINK[@rel='canonical']/@href should return canonical URL");

        final String divClass = (String) xpath.evaluate("//DIV/@class", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("container", divClass, "XPath //DIV/@class should return 'container'");

        final String divId = (String) xpath.evaluate("//DIV/@id", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("main", divId, "XPath //DIV/@id should return 'main'");
    }

    /**
     * Test XPath position predicates
     */
    @Test
    public void testXPathPositionPredicates() throws Exception {
        final String html =
                "<html>" + "<head><title>Test</title></head>" + "<body>" + "<p>First</p>" + "<p>Second</p>" + "<p>Third</p>"
                        + "<link rel=\"canonical\" href=\"https://first.com\">" + "<link rel=\"canonical\" href=\"https://second.com\">"
                        + "<div>Div 1</div>" + "<div>Div 2</div>" + "<div>Div 3</div>" + "</body>" + "</html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Create XPath
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Test position predicates
        final String firstP = (String) xpath.evaluate("//P[1]", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("First", firstP.trim(), "XPath //P[1] should return first paragraph");

        final String secondP = (String) xpath.evaluate("//P[2]", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Second", secondP.trim(), "XPath //P[2] should return second paragraph");

        final String lastP = (String) xpath.evaluate("//P[last()]", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Third", lastP.trim(), "XPath //P[last()] should return last paragraph");

        final String firstCanonicalHref =
                (String) xpath.evaluate("//LINK[@rel='canonical'][1]/@href", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("https://first.com", firstCanonicalHref, "XPath //LINK[@rel='canonical'][1]/@href should return first canonical URL");

        final String secondCanonicalHref =
                (String) xpath.evaluate("//LINK[@rel='canonical'][2]/@href", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("https://second.com", secondCanonicalHref,
                "XPath //LINK[@rel='canonical'][2]/@href should return second canonical URL");
    }

    /**
     * Test XPath with complex predicates
     */
    @Test
    public void testXPathComplexPredicates() throws Exception {
        final String html =
                "<html>" + "<head>" + "<meta name=\"description\" content=\"Main description\">"
                        + "<meta name=\"keywords\" content=\"test, xpath\">" + "<meta property=\"og:title\" content=\"OG Title\">"
                        + "</head>" + "<body>" + "<div class=\"content\" data-id=\"123\">" + "<p class=\"intro\">Introduction</p>"
                        + "<p class=\"body\">Body text</p>" + "</div>" + "<div class=\"sidebar\" data-id=\"456\">"
                        + "<p class=\"info\">Sidebar info</p>" + "</div>" + "</body>" + "</html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Create XPath
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Test complex predicates
        final String metaDescription =
                (String) xpath.evaluate("//META[@name='description']/@content", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Main description", metaDescription, "Should get meta description");

        final String metaKeywords =
                (String) xpath.evaluate("//META[@name='keywords']/@content", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("test, xpath", metaKeywords, "Should get meta keywords");

        final String ogTitle = (String) xpath.evaluate("//META[@property='og:title']/@content", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("OG Title", ogTitle, "Should get OG title");

        final String contentDiv = (String) xpath.evaluate("//DIV[@class='content']/@data-id", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("123", contentDiv, "Should get data-id from content div");

        final String sidebarDiv = (String) xpath.evaluate("//DIV[@class='sidebar']/@data-id", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("456", sidebarDiv, "Should get data-id from sidebar div");

        final String introText = (String) xpath.evaluate("//P[@class='intro']", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Introduction", introText.trim(), "Should get intro paragraph text");
    }

    /**
     * Test XPath with various HTML structures
     */
    @Test
    public void testXPathVariousHTMLStructures() throws Exception {
        final String html =
                "<html lang=\"ja\">" + "<head>" + "<meta charset=\"UTF-8\">"
                        + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                        + "<meta name=\"description\" content=\"日本語のテスト\">" + "<link rel=\"canonical\" href=\"https://example.jp/test\">"
                        + "<link rel=\"alternate\" hreflang=\"en\" href=\"https://example.com/test\">"
                        + "<link rel=\"alternate\" hreflang=\"ja\" href=\"https://example.jp/test\">" + "<title>テストページ</title>" + "</head>"
                        + "<body>" + "<header>" + "<nav aria-label=\"main navigation\">" + "<a href=\"/\" aria-current=\"page\">Home</a>"
                        + "<a href=\"/about\">About</a>" + "</nav>" + "</header>" + "<main>" + "<article>" + "<h1>メインタイトル</h1>"
                        + "<p class=\"lead\">リード文</p>" + "<section>" + "<h2>セクション1</h2>" + "<p>内容1</p>" + "</section>" + "<section>"
                        + "<h2>セクション2</h2>" + "<p>内容2</p>" + "</section>" + "</article>" + "</main>" + "<footer>"
                        + "<p>&copy; 2024 Example</p>" + "</footer>" + "</body>" + "</html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Create XPath
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Test various queries
        final String lang = (String) xpath.evaluate("//HTML/@lang", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("ja", lang, "Should get HTML lang attribute");

        final String description =
                (String) xpath.evaluate("//META[@name='description']/@content", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("日本語のテスト", description, "Should get Japanese description");

        final String canonical = (String) xpath.evaluate("//LINK[@rel='canonical']/@href", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("https://example.jp/test", canonical, "Should get canonical URL");

        final String firstAlternate =
                (String) xpath.evaluate("//LINK[@rel='alternate'][1]/@hreflang", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("en", firstAlternate, "Should get first alternate hreflang");

        final String h1Text = (String) xpath.evaluate("//H1", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("メインタイトル", h1Text.trim(), "Should get H1 text");

        final String navAriaLabel = (String) xpath.evaluate("//NAV/@aria-label", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("main navigation", navAriaLabel, "Should get nav aria-label");

        final String firstSectionH2 = (String) xpath.evaluate("//SECTION[1]/H2", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("セクション1", firstSectionH2.trim(), "Should get first section H2");

        final String footerText = (String) xpath.evaluate("//FOOTER/P", doc, javax.xml.xpath.XPathConstants.STRING);
        assertTrue(footerText.contains("2024 Example"), "Should get footer text");
    }

    /**
     * Test XPath with malformed HTML
     */
    @Test
    public void testXPathWithMalformedHTML() throws Exception {
        // HTML with missing closing tags and nested structure issues
        final String html =
                "<html>" + "<head>" + "<title>Malformed HTML Test" + "<meta name=\"description\" content=\"Test\">" + "</head>" + "<body>"
                        + "<div class=\"outer\">" + "<div class=\"inner\">" + "<p>Unclosed paragraph" + "<div class=\"nested\">"
                        + "<span>Nested content</span>" + "</div>" + "</body>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null even with malformed HTML");

        // Create XPath
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Test that XPath still works with malformed HTML
        final String title = (String) xpath.evaluate("//TITLE", doc, javax.xml.xpath.XPathConstants.STRING);
        assertTrue(title.contains("Malformed HTML Test"), "Should get title even from malformed HTML");

        final String metaContent =
                (String) xpath.evaluate("//META[@name='description']/@content", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Test", metaContent, "Should get meta content from malformed HTML");

        final String outerDiv = (String) xpath.evaluate("//DIV[@class='outer']", doc, javax.xml.xpath.XPathConstants.STRING);
        assertNotNull(outerDiv, "Should find outer div");

        final String spanText = (String) xpath.evaluate("//SPAN", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Nested content", spanText.trim(), "Should get span text from malformed HTML");
    }

    /**
     * Test XPath with data attributes and ARIA attributes
     */
    @Test
    public void testXPathWithDataAndAriaAttributes() throws Exception {
        final String html =
                "<html>" + "<body>" + "<div data-id=\"123\" data-name=\"test\" aria-label=\"Main content\">"
                        + "<button data-action=\"submit\" aria-pressed=\"false\">Submit</button>"
                        + "<input data-validation=\"required\" aria-required=\"true\" type=\"text\">"
                        + "<section data-section=\"intro\" aria-labelledby=\"intro-heading\">"
                        + "<h2 id=\"intro-heading\">Introduction</h2>" + "</section>" + "</div>" + "</body>" + "</html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Create XPath
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Test data attributes
        final String dataId = (String) xpath.evaluate("//DIV/@data-id", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("123", dataId, "Should get data-id attribute");

        final String dataName = (String) xpath.evaluate("//DIV/@data-name", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("test", dataName, "Should get data-name attribute");

        final String dataAction = (String) xpath.evaluate("//BUTTON/@data-action", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("submit", dataAction, "Should get data-action from button");

        // Test ARIA attributes
        final String ariaLabel = (String) xpath.evaluate("//DIV/@aria-label", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Main content", ariaLabel, "Should get aria-label attribute");

        final String ariaPressed = (String) xpath.evaluate("//BUTTON/@aria-pressed", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("false", ariaPressed, "Should get aria-pressed from button");

        final String ariaRequired = (String) xpath.evaluate("//INPUT/@aria-required", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("true", ariaRequired, "Should get aria-required from input");

        final String ariaLabelledby = (String) xpath.evaluate("//SECTION/@aria-labelledby", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("intro-heading", ariaLabelledby, "Should get aria-labelledby from section");

        // Test predicates with data and ARIA attributes
        final String sectionWithData =
                (String) xpath.evaluate("//SECTION[@data-section='intro']/H2", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Introduction", sectionWithData.trim(), "Should get section content using data-section predicate");
    }

    /**
     * Test XPath text extraction from real-world complex HTML structure
     * (similar to Fess documentation pages)
     */
    @Test
    public void testXPathComplexRealWorldHTML() throws Exception {
        // Complex HTML structure similar to Fess documentation pages
        final String html =
                "<!DOCTYPE html>\n"
                        + "<html lang=\"ja\">\n"
                        + "<head>\n"
                        + "  <meta charset=\"UTF-8\">\n"
                        + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                        + "  <title>検索フォームの配置 | Fess 15.3</title>\n"
                        + "</head>\n"
                        + "<body>\n"
                        + "  <header>\n"
                        + "    <nav class=\"navbar\">\n"
                        + "      <a href=\"/\" class=\"logo\">Fess</a>\n"
                        + "      <ul class=\"nav-menu\">\n"
                        + "        <li><a href=\"/docs\">ドキュメント</a></li>\n"
                        + "        <li><a href=\"/download\">ダウンロード</a></li>\n"
                        + "        <li><a href=\"/forum\">フォーラム</a></li>\n"
                        + "        <li><a href=\"https://github.com/codelibs/fess\">GitHub</a></li>\n"
                        + "      </ul>\n"
                        + "    </nav>\n"
                        + "  </header>\n"
                        + "  <div class=\"container\">\n"
                        + "    <aside class=\"sidebar\">\n"
                        + "      <nav class=\"side-menu\">\n"
                        + "        <h3>設定ガイド</h3>\n"
                        + "        <ul>\n"
                        + "          <li><a href=\"#\">インストール</a></li>\n"
                        + "          <li><a href=\"#\">検索設定</a></li>\n"
                        + "          <li><a href=\"#\" class=\"active\">検索フォームの配置</a></li>\n"
                        + "          <li><a href=\"#\">管理機能</a></li>\n"
                        + "          <li><a href=\"#\">API設定</a></li>\n"
                        + "        </ul>\n"
                        + "      </nav>\n"
                        + "    </aside>\n"
                        + "    <main class=\"main-content\">\n"
                        + "      <article>\n"
                        + "        <h1>検索フォームの配置</h1>\n"
                        + "        <p>既存のウェブサイトに検索フォームを配置して、Fessの検索結果ページへ誘導することができます。</p>\n"
                        + "        <section>\n"
                        + "          <h2>検索フォーム</h2>\n"
                        + "          <p>検索フォームのコードは以下の通りです。ウェブサイトに配置してください。</p>\n"
                        + "          <pre><code>&lt;form id=\"searchForm\" method=\"get\" action=\"https://search.n2sm.co.jp/search/\"&gt;\n"
                        + "  &lt;input id=\"query\" type=\"text\" name=\"q\" maxlength=\"1000\" autocomplete=\"off\"&gt;\n"
                        + "  &lt;input type=\"submit\" name=\"search\" value=\"検索\"&gt;\n"
                        + "&lt;/form&gt;</code></pre>\n"
                        + "        </section>\n"
                        + "        <section>\n"
                        + "          <h2>サジェスト機能</h2>\n"
                        + "          <p>検索フォームにサジェスト機能を追加することができます。jQueryとsuggestor.jsを利用します。</p>\n"
                        + "          <pre><code>&lt;script type=\"text/javascript\" src=\"https://search.n2sm.co.jp/js/jquery-3.6.3.min.js\"&gt;&lt;/script&gt;\n"
                        + "&lt;script type=\"text/javascript\" src=\"https://search.n2sm.co.jp/js/suggestor.js\"&gt;&lt;/script&gt;\n"
                        + "&lt;script&gt;\n" + "$(function(){\n" + "  $('#query').suggestor({\n" + "    ajaxinfo: {\n"
                        + "      url: 'https://search.n2sm.co.jp/api/v1/suggest-words',\n" + "      fn: '_default,_japanese'\n"
                        + "    },\n" + "    boxCssInfo: {\n" + "      border: '1px solid #ccc',\n"
                        + "      '-webkit-box-shadow': '0 2px 4px rgba(0,0,0,.2)',\n" + "      'box-shadow': '0 2px 4px rgba(0,0,0,.2)',\n"
                        + "      'z-index': '10000'\n" + "    }\n" + "  });\n" + "});\n" + "&lt;/script&gt;</code></pre>\n"
                        + "        </section>\n" + "        <section>\n" + "          <h2>注意事項</h2>\n" + "          <ul>\n"
                        + "            <li>検索フォームのactionには、Fessの検索結果ページのURLを指定してください。</li>\n" + "            <li>クエリパラメータ名は「q」です。</li>\n"
                        + "            <li>入力フィールドの最大長は1000文字です。</li>\n" + "            <li>サジェスト機能はオプションです。必要に応じて追加してください。</li>\n"
                        + "          </ul>\n" + "        </section>\n" + "      </article>\n" + "    </main>\n" + "  </div>\n"
                        + "  <footer>\n" + "    <div class=\"footer-content\">\n"
                        + "      <p>&copy; 2024 CodeLibs Project. Licensed under Apache License 2.0.</p>\n"
                        + "      <ul class=\"footer-links\">\n" + "        <li><a href=\"/license\">ライセンス</a></li>\n"
                        + "        <li><a href=\"/support\">サポート</a></li>\n" + "        <li><a href=\"/contact\">お問い合わせ</a></li>\n"
                        + "      </ul>\n" + "    </div>\n" + "  </footer>\n" + "</body>\n" + "</html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Create XPath
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Extract BODY text content using XPath
        final String bodyText = (String) xpath.evaluate("//BODY", doc, javax.xml.xpath.XPathConstants.STRING);
        assertNotNull(bodyText, "Body text should not be null");
        assertFalse(bodyText.trim().isEmpty(), "Body text should not be empty");

        // Verify all major content sections are present in the extracted text

        // 1. Header navigation
        assertTrue(bodyText.contains("Fess"), "Should contain site name 'Fess'");
        assertTrue(bodyText.contains("ドキュメント"), "Should contain navigation item 'ドキュメント'");
        assertTrue(bodyText.contains("ダウンロード"), "Should contain navigation item 'ダウンロード'");
        assertTrue(bodyText.contains("フォーラム"), "Should contain navigation item 'フォーラム'");

        // 2. Sidebar menu
        assertTrue(bodyText.contains("設定ガイド"), "Should contain sidebar title '設定ガイド'");
        assertTrue(bodyText.contains("インストール"), "Should contain sidebar item 'インストール'");
        assertTrue(bodyText.contains("検索設定"), "Should contain sidebar item '検索設定'");
        assertTrue(bodyText.contains("検索フォームの配置"), "Should contain active menu item '検索フォームの配置'");
        assertTrue(bodyText.contains("管理機能"), "Should contain sidebar item '管理機能'");
        assertTrue(bodyText.contains("API設定"), "Should contain sidebar item 'API設定'");

        // 3. Main content heading
        assertTrue(bodyText.contains("検索フォームの配置"), "Should contain main heading '検索フォームの配置'");

        // 4. Introduction paragraph
        assertTrue(bodyText.contains("既存のウェブサイトに検索フォームを配置して"), "Should contain introduction text");
        assertTrue(bodyText.contains("Fessの検索結果ページへ誘導することができます"), "Should contain introduction continuation");

        // 5. Section headings
        assertTrue(bodyText.contains("検索フォーム"), "Should contain section heading '検索フォーム'");
        assertTrue(bodyText.contains("サジェスト機能"), "Should contain section heading 'サジェスト機能'");
        assertTrue(bodyText.contains("注意事項"), "Should contain section heading '注意事項'");

        // 6. Code examples and explanations
        assertTrue(bodyText.contains("検索フォームのコードは以下の通りです"), "Should contain form explanation");
        assertTrue(bodyText.contains("jQueryとsuggestor.jsを利用します"), "Should contain suggest explanation");

        // 7. List items from notes section
        assertTrue(bodyText.contains("検索フォームのactionには"), "Should contain note about action attribute");
        assertTrue(bodyText.contains("クエリパラメータ名は「q」です"), "Should contain note about query parameter");
        assertTrue(bodyText.contains("入力フィールドの最大長は1000文字です"), "Should contain note about maxlength");
        assertTrue(bodyText.contains("サジェスト機能はオプションです"), "Should contain note about optional suggest feature");

        // 8. Footer content
        assertTrue(bodyText.contains("CodeLibs Project"), "Should contain footer organization name");
        assertTrue(bodyText.contains("Apache License 2.0"), "Should contain license information");
        assertTrue(bodyText.contains("ライセンス"), "Should contain footer link 'ライセンス'");
        assertTrue(bodyText.contains("サポート"), "Should contain footer link 'サポート'");
        assertTrue(bodyText.contains("お問い合わせ"), "Should contain footer link 'お問い合わせ'");

        // Verify specific XPath queries work correctly
        final String h1Text = (String) xpath.evaluate("//H1", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("検索フォームの配置", h1Text.trim(), "Should get H1 heading");

        final String firstH2 = (String) xpath.evaluate("(//H2)[1]", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("検索フォーム", firstH2.trim(), "Should get first H2 heading");

        final String secondH2 = (String) xpath.evaluate("(//H2)[2]", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("サジェスト機能", secondH2.trim(), "Should get second H2 heading");

        final String thirdH2 = (String) xpath.evaluate("(//H2)[3]", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("注意事項", thirdH2.trim(), "Should get third H2 heading");

        // Verify attribute extraction
        final String htmlLang = (String) xpath.evaluate("//HTML/@lang", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("ja", htmlLang, "Should get HTML lang attribute");

        final String navClass = (String) xpath.evaluate("//NAV[@class='navbar']/@class", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("navbar", navClass, "Should get navbar class attribute");

        // Verify count of specific elements
        final javax.xml.xpath.XPathExpression sectionCountExpr = xpath.compile("count(//SECTION)");
        final Double sectionCount = (Double) sectionCountExpr.evaluate(doc, javax.xml.xpath.XPathConstants.NUMBER);
        assertEquals(3.0, sectionCount, "Should have 3 SECTION elements");
    }

    /**
     * Test XPath text extraction preserves Japanese characters and whitespace
     */
    @Test
    public void testXPathTextExtractionJapanese() throws Exception {
        final String html =
                "<html>\n" + "<body>\n" + "  <div>\n" + "    <h1>日本語のタイトル</h1>\n" + "    <p>これは日本語のテキストです。改行や\n" + "    空白も含まれています。</p>\n"
                        + "    <ul>\n" + "      <li>項目１</li>\n" + "      <li>項目２</li>\n" + "      <li>項目３</li>\n" + "    </ul>\n"
                        + "    <div class=\"note\">\n" + "      <strong>注意：</strong>重要な情報です。\n" + "    </div>\n" + "  </div>\n"
                        + "</body>\n" + "</html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Create XPath
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Extract BODY text
        final String bodyText = (String) xpath.evaluate("//BODY", doc, javax.xml.xpath.XPathConstants.STRING);

        // Verify Japanese content is preserved
        assertTrue(bodyText.contains("日本語のタイトル"), "Should contain Japanese title");
        assertTrue(bodyText.contains("これは日本語のテキストです"), "Should contain Japanese paragraph text");
        assertTrue(bodyText.contains("項目１"), "Should contain list item 1");
        assertTrue(bodyText.contains("項目２"), "Should contain list item 2");
        assertTrue(bodyText.contains("項目３"), "Should contain list item 3");
        assertTrue(bodyText.contains("注意"), "Should contain note label");
        assertTrue(bodyText.contains("重要な情報です"), "Should contain note content");

        // Verify individual elements
        final String h1 = (String) xpath.evaluate("//H1", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("日本語のタイトル", h1.trim(), "Should extract H1 text correctly");

        final String p = (String) xpath.evaluate("//P", doc, javax.xml.xpath.XPathConstants.STRING);
        assertTrue(p.contains("これは日本語のテキストです"), "Should extract paragraph text");

        final String firstLi = (String) xpath.evaluate("//LI[1]", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("項目１", firstLi.trim(), "Should extract first list item");

        final String strong = (String) xpath.evaluate("//STRONG", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("注意：", strong.trim(), "Should extract strong text");
    }

    /**
     * Test that SECTION elements remain as children of MAIN element
     * This test verifies proper parent-child relationships for HTML5 semantic elements
     */
    @Test
    public void testMainSectionParentChildRelationship() throws Exception {
        // HTML structure similar to Fess documentation pages
        final String html =
                "<html>\n" + "<body>\n" + "  <header>\n" + "    <h1>Site Header</h1>\n" + "  </header>\n" + "  <aside>\n"
                        + "    <nav>Sidebar Navigation</nav>\n" + "  </aside>\n" + "  <main>\n" + "    <article>\n"
                        + "      <h1>Article Title</h1>\n" + "      <section>\n" + "        <h2>Section 1</h2>\n"
                        + "        <p>Content 1</p>\n" + "      </section>\n" + "      <section>\n" + "        <h2>Section 2</h2>\n"
                        + "        <p>Content 2</p>\n" + "      </section>\n" + "      <section>\n" + "        <h2>Section 3</h2>\n"
                        + "        <p>Content 3</p>\n" + "      </section>\n" + "    </article>\n" + "  </main>\n" + "  <footer>\n"
                        + "    <p>Footer Content</p>\n" + "  </footer>\n" + "</body>\n" + "</html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Create XPath
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Verify MAIN element exists
        final org.w3c.dom.Node mainNode = (org.w3c.dom.Node) xpath.evaluate("//MAIN", doc, javax.xml.xpath.XPathConstants.NODE);
        assertNotNull(mainNode, "MAIN element should exist");

        // Verify ARTICLE is a child of MAIN
        final org.w3c.dom.Node articleNode = (org.w3c.dom.Node) xpath.evaluate("//MAIN/ARTICLE", doc, javax.xml.xpath.XPathConstants.NODE);
        assertNotNull(articleNode, "ARTICLE should be a child of MAIN");

        // Verify SECTION elements are children of ARTICLE (which is inside MAIN)
        final javax.xml.xpath.XPathExpression sectionCountExpr = xpath.compile("count(//MAIN/ARTICLE/SECTION)");
        final Double sectionCount = (Double) sectionCountExpr.evaluate(doc, javax.xml.xpath.XPathConstants.NUMBER);
        assertEquals(3.0, sectionCount, "Should have 3 SECTION elements as children of ARTICLE (inside MAIN)");

        // Verify SECTION elements have correct content
        final String section1H2 = (String) xpath.evaluate("//MAIN/ARTICLE/SECTION[1]/H2", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Section 1", section1H2.trim(), "First section should have correct heading");

        final String section2H2 = (String) xpath.evaluate("//MAIN/ARTICLE/SECTION[2]/H2", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Section 2", section2H2.trim(), "Second section should have correct heading");

        final String section3H2 = (String) xpath.evaluate("//MAIN/ARTICLE/SECTION[3]/H2", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Section 3", section3H2.trim(), "Third section should have correct heading");

        // Print DOM structure for debugging
        System.out.println("=== DOM Structure ===");
        printDOMStructure(mainNode, 0);
    }

    /**
     * Test that SECTION elements directly under MAIN remain as children
     * (without ARTICLE wrapper)
     */
    @Test
    public void testMainWithDirectSectionChildren() throws Exception {
        // HTML structure with SECTION directly under MAIN (no ARTICLE wrapper)
        final String html =
                "<html>\n" + "<body>\n" + "  <header>\n" + "    <h1>Site Header</h1>\n" + "  </header>\n" + "  <aside>\n"
                        + "    <nav>Sidebar</nav>\n" + "  </aside>\n" + "  <main>\n" + "    <h1>Main Title</h1>\n" + "    <section>\n"
                        + "      <h2>Section 1</h2>\n" + "      <p>Content 1</p>\n" + "    </section>\n" + "    <section>\n"
                        + "      <h2>Section 2</h2>\n" + "      <p>Content 2</p>\n" + "    </section>\n" + "    <section>\n"
                        + "      <h2>Section 3</h2>\n" + "      <p>Content 3</p>\n" + "    </section>\n" + "  </main>\n" + "  <footer>\n"
                        + "    <p>Footer</p>\n" + "  </footer>\n" + "</body>\n" + "</html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Create XPath
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Verify MAIN element exists
        final org.w3c.dom.Node mainNode = (org.w3c.dom.Node) xpath.evaluate("//MAIN", doc, javax.xml.xpath.XPathConstants.NODE);
        assertNotNull(mainNode, "MAIN element should exist");

        // Print DOM structure for debugging
        System.out.println("=== DOM Structure (MAIN with direct SECTION children) ===");
        printDOMStructure(mainNode, 0);

        // Verify SECTION elements are DIRECT children of MAIN
        final javax.xml.xpath.XPathExpression sectionCountExpr = xpath.compile("count(//MAIN/SECTION)");
        final Double sectionCount = (Double) sectionCountExpr.evaluate(doc, javax.xml.xpath.XPathConstants.NUMBER);
        assertEquals(3.0, sectionCount, "Should have 3 SECTION elements as DIRECT children of MAIN");

        // Verify SECTION elements have correct content
        final String section1H2 = (String) xpath.evaluate("//MAIN/SECTION[1]/H2", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Section 1", section1H2.trim(), "First section should have correct heading");

        final String section2H2 = (String) xpath.evaluate("//MAIN/SECTION[2]/H2", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Section 2", section2H2.trim(), "Second section should have correct heading");

        final String section3H2 = (String) xpath.evaluate("//MAIN/SECTION[3]/H2", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Section 3", section3H2.trim(), "Third section should have correct heading");

        // Verify main's H1 is also a direct child
        final String mainH1 = (String) xpath.evaluate("//MAIN/H1", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Main Title", mainH1.trim(), "MAIN's H1 should be a direct child");
    }

    /**
     * Test complex page structure similar to Fess documentation
     * Verifies that SECTION elements are properly nested under MAIN
     */
    @Test
    public void testFessLikePageStructure() throws Exception {
        // HTML structure mimicking Fess documentation pages
        final String html =
                "<!DOCTYPE html>\n" + "<html lang=\"ja\">\n" + "<body>\n" + "  <header>\n" + "    <nav class=\"navbar\">Navigation</nav>\n"
                        + "  </header>\n" + "  <div class=\"container\">\n" + "    <aside class=\"sidebar\">\n"
                        + "      <nav>Sidebar Menu</nav>\n" + "    </aside>\n" + "    <main class=\"main-content\">\n"
                        + "      <article>\n" + "        <h1>検索フォームの配置</h1>\n" + "        <p>既存のウェブサイトに検索フォームを配置できます。</p>\n"
                        + "        <section>\n" + "          <h2>検索フォーム</h2>\n" + "          <p>検索フォームのコードは以下の通りです。</p>\n"
                        + "        </section>\n" + "        <section>\n" + "          <h2>サジェスト機能</h2>\n"
                        + "          <p>サジェスト機能を追加できます。</p>\n" + "        </section>\n" + "        <section>\n"
                        + "          <h2>注意事項</h2>\n" + "          <p>以下の点に注意してください。</p>\n" + "        </section>\n" + "      </article>\n"
                        + "    </main>\n" + "  </div>\n" + "  <footer>\n" + "    <p>Footer Content</p>\n" + "  </footer>\n" + "</body>\n"
                        + "</html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Create XPath
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Verify MAIN element exists
        final org.w3c.dom.Node mainNode = (org.w3c.dom.Node) xpath.evaluate("//MAIN", doc, javax.xml.xpath.XPathConstants.NODE);
        assertNotNull(mainNode, "MAIN element should exist");

        // Print DOM structure under MAIN
        System.out.println("=== DOM Structure for Fess-like page (checking MAIN's children) ===");
        printDOMStructure(mainNode, 0);

        // Check that MAIN has expected children
        final org.w3c.dom.NodeList mainChildren = mainNode.getChildNodes();
        int elementChildCount = 0;
        boolean hasArticle = false;
        boolean hasHeader = false;
        boolean hasAside = false;

        for (int i = 0; i < mainChildren.getLength(); i++) {
            final org.w3c.dom.Node child = mainChildren.item(i);
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                elementChildCount++;
                final String nodeName = child.getNodeName();
                System.out.println("MAIN's child element: " + nodeName);

                if ("ARTICLE".equals(nodeName)) {
                    hasArticle = true;
                }
                if ("HEADER".equals(nodeName)) {
                    hasHeader = true;
                }
                if ("ASIDE".equals(nodeName)) {
                    hasAside = true;
                }
            }
        }

        // MAIN should have ARTICLE as child (HEADER and ASIDE should NOT be children of MAIN)
        assertTrue(hasArticle, "MAIN should have ARTICLE as a child");
        assertFalse(hasHeader, "HEADER should NOT be a child of MAIN (it should be a sibling)");
        assertFalse(hasAside, "ASIDE should NOT be a child of MAIN (it should be a sibling)");

        // Verify SECTION elements are children of ARTICLE (inside MAIN)
        final javax.xml.xpath.XPathExpression sectionCountExpr = xpath.compile("count(//MAIN/ARTICLE/SECTION)");
        final Double sectionCount = (Double) sectionCountExpr.evaluate(doc, javax.xml.xpath.XPathConstants.NUMBER);
        assertEquals(3.0, sectionCount, "Should have 3 SECTION elements as children of ARTICLE (inside MAIN)");

        // Verify specific content
        final String h1 = (String) xpath.evaluate("//MAIN/ARTICLE/H1", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("検索フォームの配置", h1.trim(), "Should get correct H1 content");

        final String firstSectionH2 = (String) xpath.evaluate("//MAIN/ARTICLE/SECTION[1]/H2", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("検索フォーム", firstSectionH2.trim(), "First section should have correct heading");

        final String secondSectionH2 = (String) xpath.evaluate("//MAIN/ARTICLE/SECTION[2]/H2", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("サジェスト機能", secondSectionH2.trim(), "Second section should have correct heading");

        final String thirdSectionH2 = (String) xpath.evaluate("//MAIN/ARTICLE/SECTION[3]/H2", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("注意事項", thirdSectionH2.trim(), "Third section should have correct heading");
    }

    /**
     * Test that malformed HTML with ASIDE before MAIN doesn't break structure
     * This reproduces the issue where ASIDE causes subsequent elements to be misplaced
     */
    @Test
    public void testMalformedHTMLWithAsideBeforeMain() throws Exception {
        // HTML with ASIDE that might be malformed or unclosed
        final String html =
                "<html>\n" + "<body>\n" + "  <header>\n" + "    <h1>Site Header</h1>\n" + "  </header>\n" + "  <aside class=\"sidebar\">\n"
                        + "    <nav>Sidebar Menu</nav>\n" + "  <!-- ASIDE may not be properly closed in malformed HTML -->\n"
                        + "  <main>\n" + "    <h1>Main Content</h1>\n" + "    <section>\n" + "      <h2>Section 1</h2>\n"
                        + "      <p>Content 1</p>\n" + "    </section>\n" + "    <section>\n" + "      <h2>Section 2</h2>\n"
                        + "      <p>Content 2</p>\n" + "    </section>\n" + "  </main>\n" + "  <footer>\n" + "    <p>Footer</p>\n"
                        + "  </footer>\n" + "</body>\n" + "</html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Create XPath
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Verify MAIN element exists
        final org.w3c.dom.Node mainNode = (org.w3c.dom.Node) xpath.evaluate("//MAIN", doc, javax.xml.xpath.XPathConstants.NODE);
        assertNotNull(mainNode, "MAIN element should exist even with malformed ASIDE");

        System.out.println("=== DOM Structure with ASIDE before MAIN ===");
        final org.w3c.dom.Node bodyNode = (org.w3c.dom.Node) xpath.evaluate("//BODY", doc, javax.xml.xpath.XPathConstants.NODE);
        printDOMStructure(bodyNode, 0);

        // Verify SECTION elements are children of MAIN
        final javax.xml.xpath.XPathExpression sectionCountExpr = xpath.compile("count(//MAIN/SECTION)");
        final Double sectionCount = (Double) sectionCountExpr.evaluate(doc, javax.xml.xpath.XPathConstants.NUMBER);
        assertEquals(2.0, sectionCount, "Should have 2 SECTION elements as DIRECT children of MAIN (even with malformed ASIDE before MAIN)");

        // Verify content is accessible
        final String mainH1 = (String) xpath.evaluate("//MAIN/H1", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Main Content", mainH1.trim(), "MAIN's H1 should be accessible");

        final String section1H2 = (String) xpath.evaluate("//MAIN/SECTION[1]/H2", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Section 1", section1H2.trim(), "First section should have correct heading");

        final String section2H2 = (String) xpath.evaluate("//MAIN/SECTION[2]/H2", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("Section 2", section2H2.trim(), "Second section should have correct heading");
    }

    /**
     * Test that properly closed ASIDE before MAIN maintains correct structure
     */
    @Test
    public void testWellFormedHTMLWithAsideBeforeMain() throws Exception {
        // HTML with properly closed ASIDE
        final String html =
                "<html>\n" + "<body>\n" + "  <header>\n" + "    <h1>Site Header</h1>\n" + "  </header>\n" + "  <aside class=\"sidebar\">\n"
                        + "    <nav>Sidebar Menu</nav>\n" + "  </aside>\n" + "  <main>\n" + "    <h1>Main Content</h1>\n"
                        + "    <section>\n" + "      <h2>Section 1</h2>\n" + "      <p>Content 1</p>\n" + "    </section>\n"
                        + "    <section>\n" + "      <h2>Section 2</h2>\n" + "      <p>Content 2</p>\n" + "    </section>\n"
                        + "  </main>\n" + "  <footer>\n" + "    <p>Footer</p>\n" + "  </footer>\n" + "</body>\n" + "</html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Create XPath
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Verify MAIN element exists
        final org.w3c.dom.Node mainNode = (org.w3c.dom.Node) xpath.evaluate("//MAIN", doc, javax.xml.xpath.XPathConstants.NODE);
        assertNotNull(mainNode, "MAIN element should exist");

        System.out.println("=== DOM Structure with properly closed ASIDE before MAIN ===");
        final org.w3c.dom.Node bodyNode = (org.w3c.dom.Node) xpath.evaluate("//BODY", doc, javax.xml.xpath.XPathConstants.NODE);
        printDOMStructure(bodyNode, 0);

        // Verify BODY's children
        final org.w3c.dom.NodeList bodyChildren = bodyNode.getChildNodes();
        int elementChildCount = 0;
        boolean hasHeader = false;
        boolean hasAside = false;
        boolean hasMain = false;
        boolean hasFooter = false;

        for (int i = 0; i < bodyChildren.getLength(); i++) {
            final org.w3c.dom.Node child = bodyChildren.item(i);
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                elementChildCount++;
                final String nodeName = child.getNodeName();
                System.out.println("BODY's child element: " + nodeName);

                if ("HEADER".equals(nodeName)) {
                    hasHeader = true;
                }
                if ("ASIDE".equals(nodeName)) {
                    hasAside = true;
                }
                if ("MAIN".equals(nodeName)) {
                    hasMain = true;
                }
                if ("FOOTER".equals(nodeName)) {
                    hasFooter = true;
                }
            }
        }

        // All should be direct children of BODY
        assertTrue(hasHeader, "HEADER should be a child of BODY");
        assertTrue(hasAside, "ASIDE should be a child of BODY");
        assertTrue(hasMain, "MAIN should be a child of BODY (not a child of ASIDE!)");
        assertTrue(hasFooter, "FOOTER should be a child of BODY (not a child of ASIDE!)");

        // Verify SECTION elements are children of MAIN
        final javax.xml.xpath.XPathExpression sectionCountExpr = xpath.compile("count(//MAIN/SECTION)");
        final Double sectionCount = (Double) sectionCountExpr.evaluate(doc, javax.xml.xpath.XPathConstants.NUMBER);
        assertEquals(2.0, sectionCount, "Should have 2 SECTION elements as DIRECT children of MAIN");
    }

    /**
     * Helper method to print DOM structure
     */
    private void printDOMStructure(final org.w3c.dom.Node node, final int level) {
        final String indent = "  ".repeat(level);
        if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
            System.out.println(indent + "<" + node.getNodeName() + ">");
            final org.w3c.dom.NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                final org.w3c.dom.Node child = children.item(i);
                if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    printDOMStructure(child, level + 1);
                }
            }
        }
    }

} // class SAXToDOMHandlerTest
