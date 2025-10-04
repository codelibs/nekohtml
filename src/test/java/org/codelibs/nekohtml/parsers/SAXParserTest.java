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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Test cases for SAXParser.
 * Tests the backward compatibility wrapper around HTMLSAXParser.
 */
public class SAXParserTest {

    private SAXParser parser;

    @BeforeEach
    public void setUp() throws Exception {
        parser = new SAXParser();
    }

    @Test
    public void testConstructor() throws Exception {
        // When: SAXParser is instantiated
        final SAXParser saxParser = new SAXParser();

        // Then: Parser should be created successfully
        assertNotNull(saxParser, "SAXParser should be instantiated");
    }

    @Test
    public void testBasicParsing() throws Exception {
        // Given: Simple HTML content
        final String html = "<html><body><p>Hello World</p></body></html>";
        final List<String> elements = new ArrayList<>();

        // When: Parsing HTML with content handler
        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: All elements should be captured
        assertEquals(3, elements.size(), "Should have 3 elements");
        assertEquals("HTML", elements.get(0), "First element should be HTML");
        assertEquals("BODY", elements.get(1), "Second element should be BODY");
        assertEquals("P", elements.get(2), "Third element should be P");
    }

    @Test
    public void testParsingWithAttributes() throws Exception {
        // Given: HTML with attributes
        final String html = "<div id=\"test\" class=\"container\">Content</div>";
        final List<String> attrs = new ArrayList<>();

        // When: Parsing HTML with attribute handling
        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                for (int i = 0; i < attributes.getLength(); i++) {
                    attrs.add(attributes.getQName(i) + "=" + attributes.getValue(i));
                }
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Attributes should be captured
        assertTrue(attrs.contains("id=test"), "Should have id attribute");
        assertTrue(attrs.contains("class=container"), "Should have class attribute");
    }

    @Test
    public void testTextContent() throws Exception {
        // Given: HTML with text content
        final String html = "<p>Hello World</p>";
        final StringBuilder text = new StringBuilder();

        // When: Parsing HTML with character handling
        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) throws SAXException {
                text.append(ch, start, length);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Text content should be captured
        assertTrue(text.toString().contains("Hello World"), "Text should contain 'Hello World'");
    }

    @Test
    public void testNestedElements() throws Exception {
        // Given: Nested HTML structure
        final String html = "<html><head><title>Test</title></head><body><h1>Heading</h1></body></html>";
        final List<String> elements = new ArrayList<>();

        // When: Parsing nested structure
        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: All nested elements should be captured in order
        assertEquals("HTML", elements.get(0), "First should be HTML");
        assertEquals("HEAD", elements.get(1), "Second should be HEAD");
        assertEquals("TITLE", elements.get(2), "Third should be TITLE");
        assertEquals("BODY", elements.get(3), "Fourth should be BODY");
        assertEquals("H1", elements.get(4), "Fifth should be H1");
    }

    @Test
    public void testEndElements() throws Exception {
        // Given: HTML with explicit closing tags
        final String html = "<div><p>Text</p></div>";
        final List<String> endElements = new ArrayList<>();

        // When: Parsing with end element handler
        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void endElement(final String uri, final String localName, final String qName) throws SAXException {
                endElements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: End elements should be captured in reverse order
        assertEquals("P", endElements.get(0), "First end should be P");
        assertEquals("DIV", endElements.get(1), "Second end should be DIV");
    }

    @Test
    public void testDocumentEvents() throws Exception {
        // Given: HTML document
        final String html = "<html><body>Content</body></html>";
        final List<String> events = new ArrayList<>();

        // When: Parsing with document event handlers
        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startDocument() throws SAXException {
                events.add("START_DOCUMENT");
            }

            @Override
            public void endDocument() throws SAXException {
                events.add("END_DOCUMENT");
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Document events should be fired
        assertEquals("START_DOCUMENT", events.get(0), "First event should be START_DOCUMENT");
        assertEquals("END_DOCUMENT", events.get(events.size() - 1), "Last event should be END_DOCUMENT");
    }

    @Test
    public void testEmptyElements() throws Exception {
        // Given: HTML with void elements
        final String html = "<div><br><hr><img src=\"test.jpg\"></div>";
        final List<String> elements = new ArrayList<>();

        // When: Parsing void elements
        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Void elements should be captured
        assertTrue(elements.contains("BR"), "Should have BR element");
        assertTrue(elements.contains("HR"), "Should have HR element");
        assertTrue(elements.contains("IMG"), "Should have IMG element");
    }

    @Test
    public void testMalformedHTML() throws Exception {
        // Given: Malformed HTML (unclosed tags)
        final String html = "<div><p>Text<div>Another</div>";
        final List<String> elements = new ArrayList<>();

        // When: Parsing malformed HTML
        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Parser should handle malformed HTML gracefully
        assertTrue(elements.contains("DIV"), "Should parse DIV elements");
        assertTrue(elements.contains("P"), "Should parse P element");
    }

    @Test
    public void testSpecialCharacters() throws Exception {
        // Given: HTML with special characters
        final String html = "<p>Special: &lt;&gt;&amp;&quot;</p>";
        final StringBuilder text = new StringBuilder();

        // When: Parsing special characters
        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) throws SAXException {
                text.append(ch, start, length);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Special characters should be handled
        assertNotNull(text.toString(), "Text should be captured");
    }

    @Test
    public void testMultipleTextNodes() throws Exception {
        // Given: HTML with multiple text segments
        final String html = "<div>First <strong>bold</strong> Last</div>";
        final List<String> textSegments = new ArrayList<>();

        // When: Parsing text segments
        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) throws SAXException {
                final String text = new String(ch, start, length).trim();
                if (!text.isEmpty()) {
                    textSegments.add(text);
                }
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: All text segments should be captured
        assertTrue(textSegments.contains("First"), "Should have 'First' text");
        assertTrue(textSegments.contains("bold"), "Should have 'bold' text");
        assertTrue(textSegments.contains("Last"), "Should have 'Last' text");
    }

    @Test
    public void testComplexDocument() throws Exception {
        // Given: Complex HTML document
        final String html =
                "<html>" + "<head><meta charset=\"UTF-8\"><title>Test Page</title></head>"
                        + "<body><h1>Heading</h1><p>Paragraph 1</p><p>Paragraph 2</p></body>" + "</html>";

        final List<String> elements = new ArrayList<>();
        final List<String> text = new ArrayList<>();

        // When: Parsing complex document
        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }

            @Override
            public void characters(final char[] ch, final int start, final int length) throws SAXException {
                final String content = new String(ch, start, length).trim();
                if (!content.isEmpty()) {
                    text.add(content);
                }
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: All elements and text should be captured
        assertTrue(elements.contains("HTML"), "Should have HTML");
        assertTrue(elements.contains("HEAD"), "Should have HEAD");
        assertTrue(elements.contains("META"), "Should have META");
        assertTrue(elements.contains("TITLE"), "Should have TITLE");
        assertTrue(elements.contains("BODY"), "Should have BODY");
        assertTrue(elements.contains("H1"), "Should have H1");
        assertTrue(elements.contains("P"), "Should have P elements");

        assertTrue(text.contains("Test Page"), "Should have title text");
        assertTrue(text.contains("Heading"), "Should have heading text");
        assertTrue(text.contains("Paragraph 1"), "Should have first paragraph");
        assertTrue(text.contains("Paragraph 2"), "Should have second paragraph");
    }

    @Test
    public void testParsingSystemId() throws Exception {
        // Given: HTML content and system ID
        final String html = "<html><body>Test</body></html>";
        final List<String> elements = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        // When: Parsing with InputSource that has system ID
        final InputSource input = new InputSource(new StringReader(html));
        input.setSystemId("test.html");
        parser.parse(input);

        // Then: Parsing should succeed
        assertTrue(elements.contains("HTML"), "Should parse HTML element");
        assertTrue(elements.contains("BODY"), "Should parse BODY element");
    }

    @Test
    public void testInheritanceFromHTMLSAXParser() throws Exception {
        // Given: SAXParser instance
        final SAXParser saxParser = new SAXParser();

        // When/Then: SAXParser should be instance of HTMLSAXParser
        assertTrue(saxParser instanceof org.codelibs.nekohtml.sax.HTMLSAXParser,
                "SAXParser should extend HTMLSAXParser for backward compatibility");
    }

    @Test
    public void testSettersAndGetters() throws Exception {
        // Given: Content handler
        final DefaultHandler handler = new DefaultHandler();

        // When: Setting content handler
        parser.setContentHandler(handler);

        // Then: Content handler should be set
        assertEquals(handler, parser.getContentHandler(), "Content handler should be set correctly");
    }

    // ========== HTML5 Semantic Elements ==========

    @Test
    public void testHTML5SemanticElements() throws Exception {
        // Given: HTML5 semantic structure
        final String html =
                "<html><body><header>Header</header><nav>Nav</nav><main><article>Article</article>"
                        + "<section>Section</section><aside>Aside</aside></main><footer>Footer</footer></body></html>";
        final List<String> elements = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: All HTML5 semantic elements should be captured
        assertTrue(elements.contains("HEADER"), "Should have HEADER");
        assertTrue(elements.contains("NAV"), "Should have NAV");
        assertTrue(elements.contains("MAIN"), "Should have MAIN");
        assertTrue(elements.contains("ARTICLE"), "Should have ARTICLE");
        assertTrue(elements.contains("SECTION"), "Should have SECTION");
        assertTrue(elements.contains("ASIDE"), "Should have ASIDE");
        assertTrue(elements.contains("FOOTER"), "Should have FOOTER");
    }

    @Test
    public void testHTML5FormElements() throws Exception {
        // Given: HTML5 form elements
        final String html =
                "<form><input type=\"email\"><input type=\"number\"><input type=\"date\">"
                        + "<datalist><option>Option</option></datalist><output>Result</output></form>";
        final List<String> elements = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: HTML5 form elements should be captured
        assertTrue(elements.contains("FORM"), "Should have FORM");
        assertTrue(elements.contains("INPUT"), "Should have INPUT");
        assertTrue(elements.contains("DATALIST"), "Should have DATALIST");
        assertTrue(elements.contains("OPTION"), "Should have OPTION");
        assertTrue(elements.contains("OUTPUT"), "Should have OUTPUT");
    }

    @Test
    public void testHTML5MediaElements() throws Exception {
        // Given: HTML5 media elements
        final String html = "<video controls><source src=\"video.mp4\"></video>" + "<audio controls><source src=\"audio.mp3\"></audio>";
        final List<String> elements = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Media elements should be captured
        assertTrue(elements.contains("VIDEO"), "Should have VIDEO");
        assertTrue(elements.contains("AUDIO"), "Should have AUDIO");
        assertTrue(elements.contains("SOURCE"), "Should have SOURCE");
    }

    @Test
    public void testHTML5VoidElements() throws Exception {
        // Given: HTML5 void elements
        final String html =
                "<meta charset=\"UTF-8\"><link rel=\"stylesheet\"><base href=\"/\">"
                        + "<embed src=\"file.swf\"><wbr><track src=\"captions.vtt\">";
        final List<String> elements = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Void elements should be captured
        assertTrue(elements.contains("META"), "Should have META");
        assertTrue(elements.contains("LINK"), "Should have LINK");
        assertTrue(elements.contains("BASE"), "Should have BASE");
        assertTrue(elements.contains("EMBED"), "Should have EMBED");
        assertTrue(elements.contains("WBR"), "Should have WBR");
        assertTrue(elements.contains("TRACK"), "Should have TRACK");
    }

    // ========== Complex Structures ==========

    @Test
    public void testTableStructure() throws Exception {
        // Given: Complex table structure
        final String html =
                "<table><caption>Title</caption><thead><tr><th>Header</th></tr></thead>"
                        + "<tbody><tr><td>Data</td></tr></tbody><tfoot><tr><td>Footer</td></tr></tfoot></table>";
        final List<String> elements = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: All table elements should be captured
        assertTrue(elements.contains("TABLE"), "Should have TABLE");
        assertTrue(elements.contains("CAPTION"), "Should have CAPTION");
        assertTrue(elements.contains("THEAD"), "Should have THEAD");
        assertTrue(elements.contains("TBODY"), "Should have TBODY");
        assertTrue(elements.contains("TFOOT"), "Should have TFOOT");
        assertTrue(elements.contains("TR"), "Should have TR");
        assertTrue(elements.contains("TH"), "Should have TH");
        assertTrue(elements.contains("TD"), "Should have TD");
    }

    @Test
    public void testListElements() throws Exception {
        // Given: Various list types
        final String html =
                "<ul><li>Item 1</li><li>Item 2</li></ul>" + "<ol><li>First</li><li>Second</li></ol>"
                        + "<dl><dt>Term</dt><dd>Definition</dd></dl>";
        final List<String> elements = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: All list elements should be captured
        assertTrue(elements.contains("UL"), "Should have UL");
        assertTrue(elements.contains("OL"), "Should have OL");
        assertTrue(elements.contains("DL"), "Should have DL");
        assertTrue(elements.contains("LI"), "Should have LI");
        assertTrue(elements.contains("DT"), "Should have DT");
        assertTrue(elements.contains("DD"), "Should have DD");
    }

    @Test
    public void testScriptAndStyleElements() throws Exception {
        // Given: Script and style elements
        final String html =
                "<head><script type=\"text/javascript\">var x = 1;</script>"
                        + "<style type=\"text/css\">body { margin: 0; }</style></head>";
        final List<String> elements = new ArrayList<>();
        final List<String> textContent = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }

            @Override
            public void characters(final char[] ch, final int start, final int length) throws SAXException {
                final String text = new String(ch, start, length).trim();
                if (!text.isEmpty()) {
                    textContent.add(text);
                }
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Script and style should be captured
        assertTrue(elements.contains("SCRIPT"), "Should have SCRIPT");
        assertTrue(elements.contains("STYLE"), "Should have STYLE");
        assertNotNull(textContent, "Should have text content");
    }

    // ========== Attributes ==========

    @Test
    public void testDataAttributes() throws Exception {
        // Given: HTML5 data-* attributes
        final String html = "<div data-id=\"123\" data-name=\"test\" data-value=\"abc\">Content</div>";
        final List<String> dataAttrs = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).startsWith("data-")) {
                        dataAttrs.add(attributes.getQName(i) + "=" + attributes.getValue(i));
                    }
                }
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Data attributes should be captured
        assertTrue(dataAttrs.contains("data-id=123"), "Should have data-id");
        assertTrue(dataAttrs.contains("data-name=test"), "Should have data-name");
        assertTrue(dataAttrs.contains("data-value=abc"), "Should have data-value");
    }

    @Test
    public void testAriaAttributes() throws Exception {
        // Given: ARIA attributes
        final String html = "<button role=\"button\" aria-label=\"Close\" aria-hidden=\"false\">X</button>";
        final List<String> ariaAttrs = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).startsWith("aria-") || "role".equals(attributes.getQName(i))) {
                        ariaAttrs.add(attributes.getQName(i) + "=" + attributes.getValue(i));
                    }
                }
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: ARIA attributes should be captured
        assertTrue(ariaAttrs.contains("role=button"), "Should have role");
        assertTrue(ariaAttrs.contains("aria-label=Close"), "Should have aria-label");
        assertTrue(ariaAttrs.contains("aria-hidden=false"), "Should have aria-hidden");
    }

    @Test
    public void testBooleanAttributes() throws Exception {
        // Given: Boolean attributes
        final String html = "<input type=\"checkbox\" checked disabled readonly required>";
        final List<String> boolAttrs = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                for (int i = 0; i < attributes.getLength(); i++) {
                    boolAttrs.add(attributes.getQName(i));
                }
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Boolean attributes should be captured
        assertTrue(boolAttrs.contains("checked"), "Should have checked");
        assertTrue(boolAttrs.contains("disabled"), "Should have disabled");
        assertTrue(boolAttrs.contains("readonly"), "Should have readonly");
        assertTrue(boolAttrs.contains("required"), "Should have required");
    }

    // ========== Comment and Processing Instructions ==========

    @Test
    public void testCommentHandling() throws Exception {
        // Given: HTML with comments
        final String html = "<!--Comment 1--><div>Content</div><!--Comment 2-->";
        final List<String> elements = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Parsing should succeed despite comments
        assertTrue(elements.contains("DIV"), "Should parse DIV element");
    }

    @Test
    public void testIgnorableWhitespace() throws Exception {
        // Given: HTML with extra whitespace
        final String html = "  \n  <div>  \n  Text  \n  </div>  \n  ";
        final List<String> elements = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Should handle whitespace correctly
        assertTrue(elements.contains("DIV"), "Should parse DIV despite whitespace");
    }

    // ========== Edge Cases ==========

    @Test
    public void testEmptyDocument() throws Exception {
        // Given: Empty HTML
        final String html = "";
        final List<String> events = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startDocument() throws SAXException {
                events.add("START");
            }

            @Override
            public void endDocument() throws SAXException {
                events.add("END");
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Document events should still fire
        assertTrue(events.contains("START"), "Should fire start document");
        assertTrue(events.contains("END"), "Should fire end document");
    }

    @Test
    public void testVeryLongText() throws Exception {
        // Given: Very long text content
        final StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longText.append("Text ");
        }
        final String html = "<p>" + longText.toString() + "</p>";
        final StringBuilder captured = new StringBuilder();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) throws SAXException {
                captured.append(ch, start, length);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Long text should be handled
        assertTrue(captured.length() > 0, "Should capture long text");
    }

    @Test
    public void testManySiblings() throws Exception {
        // Given: Many sibling elements
        final StringBuilder html = new StringBuilder("<div>");
        for (int i = 0; i < 100; i++) {
            html.append("<p>Paragraph ").append(i).append("</p>");
        }
        html.append("</div>");

        final List<String> elements = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html.toString())));

        // Then: All siblings should be captured
        int pCount = 0;
        for (final String elem : elements) {
            if ("P".equals(elem)) {
                pCount++;
            }
        }
        assertEquals(100, pCount, "Should have 100 P elements");
    }

    @Test
    public void testDeeplyNestedElements() throws Exception {
        // Given: Deeply nested structure
        final StringBuilder html = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            html.append("<div>");
        }
        html.append("Content");
        for (int i = 0; i < 50; i++) {
            html.append("</div>");
        }

        final List<String> elements = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html.toString())));

        // Then: Deep nesting should be handled
        int divCount = 0;
        for (final String elem : elements) {
            if ("DIV".equals(elem)) {
                divCount++;
            }
        }
        assertEquals(50, divCount, "Should have 50 DIV elements");
    }

    @Test
    public void testMixedContent() throws Exception {
        // Given: Mixed content (text and elements)
        final String html = "<p>Text before <strong>bold</strong> text after <em>italic</em> end</p>";
        final List<String> elements = new ArrayList<>();
        final List<String> textParts = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }

            @Override
            public void characters(final char[] ch, final int start, final int length) throws SAXException {
                final String text = new String(ch, start, length).trim();
                if (!text.isEmpty()) {
                    textParts.add(text);
                }
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Mixed content should be handled
        assertTrue(elements.contains("STRONG"), "Should have STRONG");
        assertTrue(elements.contains("EM"), "Should have EM");
        assertTrue(textParts.contains("Text before"), "Should have text before");
        assertTrue(textParts.contains("bold"), "Should have bold text");
        assertTrue(textParts.contains("text after"), "Should have text after");
        assertTrue(textParts.contains("italic"), "Should have italic text");
    }

    @Test
    public void testUnicodeContent() throws Exception {
        // Given: Unicode content
        final String html = "<p>Hello 世界 \u263A Unicode: \uD83D\uDE00</p>";
        final StringBuilder text = new StringBuilder();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void characters(final char[] ch, final int start, final int length) throws SAXException {
                text.append(ch, start, length);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Unicode should be preserved
        assertTrue(text.toString().contains("世界"), "Should contain Japanese characters");
        assertTrue(text.toString().contains("Unicode"), "Should contain text");
    }

    @Test
    public void testMultipleAttributesOnElement() throws Exception {
        // Given: Element with many attributes
        final StringBuilder html = new StringBuilder("<div");
        for (int i = 0; i < 20; i++) {
            html.append(" attr").append(i).append("=\"value").append(i).append("\"");
        }
        html.append(">Content</div>");

        final List<String> attrs = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                for (int i = 0; i < attributes.getLength(); i++) {
                    attrs.add(attributes.getQName(i));
                }
            }
        });

        parser.parse(new InputSource(new StringReader(html.toString())));

        // Then: All attributes should be captured
        assertEquals(20, attrs.size(), "Should have 20 attributes");
    }

    @Test
    public void testDoctypeDeclaration() throws Exception {
        // Given: HTML with DOCTYPE
        final String html = "<!DOCTYPE html><html><body>Content</body></html>";
        final List<String> elements = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Should parse despite DOCTYPE
        assertTrue(elements.contains("HTML"), "Should parse HTML");
        assertTrue(elements.contains("BODY"), "Should parse BODY");
    }

    @Test
    public void testCaseInsensitiveTagNames() throws Exception {
        // Given: Mixed case HTML
        final String html = "<Html><Body><P>Text</p></BoDy></hTmL>";
        final List<String> elements = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Tags should be normalized
        assertTrue(elements.contains("HTML"), "Should have HTML");
        assertTrue(elements.contains("BODY"), "Should have BODY");
        assertTrue(elements.contains("P"), "Should have P");
    }

    @Test
    public void testFormComplexStructure() throws Exception {
        // Given: Complex form structure
        final String html =
                "<form action=\"/submit\" method=\"post\">" + "<fieldset><legend>Info</legend><label for=\"name\">Name:</label>"
                        + "<input type=\"text\" id=\"name\" name=\"name\" required>"
                        + "<select name=\"country\"><option value=\"us\">US</option></select>"
                        + "<textarea name=\"message\" rows=\"5\"></textarea>" + "<button type=\"submit\">Submit</button>"
                        + "</fieldset></form>";
        final List<String> elements = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: All form elements should be captured
        assertTrue(elements.contains("FORM"), "Should have FORM");
        assertTrue(elements.contains("FIELDSET"), "Should have FIELDSET");
        assertTrue(elements.contains("LEGEND"), "Should have LEGEND");
        assertTrue(elements.contains("LABEL"), "Should have LABEL");
        assertTrue(elements.contains("INPUT"), "Should have INPUT");
        assertTrue(elements.contains("SELECT"), "Should have SELECT");
        assertTrue(elements.contains("OPTION"), "Should have OPTION");
        assertTrue(elements.contains("TEXTAREA"), "Should have TEXTAREA");
        assertTrue(elements.contains("BUTTON"), "Should have BUTTON");
    }

    @Test
    public void testMetaTags() throws Exception {
        // Given: Various meta tags
        final String html =
                "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width\">"
                        + "<meta name=\"description\" content=\"Test page\"></head>";
        final List<String> metaContents = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                if ("META".equals(qName)) {
                    final String name = attributes.getValue("name");
                    final String content = attributes.getValue("content");
                    if (name != null && content != null) {
                        metaContents.add(name + "=" + content);
                    }
                }
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Meta tags should be captured
        assertTrue(metaContents.contains("viewport=width=device-width"), "Should have viewport meta");
        assertTrue(metaContents.contains("description=Test page"), "Should have description meta");
    }

    @Test
    public void testNestedListStructure() throws Exception {
        // Given: Nested list structure
        final String html = "<ul><li>Item 1<ul><li>Nested 1</li><li>Nested 2</li></ul></li><li>Item 2</li></ul>";
        final List<String> elements = new ArrayList<>();

        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
                    throws SAXException {
                elements.add(qName);
            }
        });

        parser.parse(new InputSource(new StringReader(html)));

        // Then: Nested structure should be captured
        int ulCount = 0, liCount = 0;
        for (final String elem : elements) {
            if ("UL".equals(elem))
                ulCount++;
            if ("LI".equals(elem))
                liCount++;
        }
        assertEquals(2, ulCount, "Should have 2 UL elements");
        assertEquals(4, liCount, "Should have 4 LI elements");
    }

} // class SAXParserTest
