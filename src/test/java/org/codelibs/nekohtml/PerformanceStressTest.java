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
package org.codelibs.nekohtml;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;

import org.codelibs.nekohtml.parsers.DOMParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Performance and stress tests for NekoHTML parser.
 * Tests handling of large documents, deep nesting, and pathological patterns.
 *
 * @author CodeLibs Project
 */
public class PerformanceStressTest {

    private DOMParser parser;

    @BeforeEach
    public void setUp() {
        parser = new DOMParser();
    }

    private Document parseHTML(final String html) throws Exception {
        parser.parse(new InputSource(new StringReader(html)));
        return parser.getDocument();
    }

    // ========================================================================
    // Deep Nesting Tests
    // ========================================================================

    @Test
    @Timeout(10)
    public void testDeeplyNestedDivs100Levels() throws Exception {
        // Given: 100 levels of nested DIVs
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 100; i++) {
            html.append("<div id=\"level").append(i).append("\">");
        }
        html.append("Content");
        for (int i = 0; i < 100; i++) {
            html.append("</div>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle deep nesting
        assertNotNull(doc, "Document should be parsed");
        final NodeList divs = doc.getElementsByTagName("DIV");
        assertEquals(100, divs.getLength(), "Should have 100 DIV elements");
    }

    @Test
    @Timeout(10)
    public void testDeeplyNestedDivs500Levels() throws Exception {
        // Given: 500 levels of nested DIVs
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 500; i++) {
            html.append("<div>");
        }
        html.append("Deep content");
        for (int i = 0; i < 500; i++) {
            html.append("</div>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle very deep nesting
        assertNotNull(doc, "Document should be parsed");
        final NodeList divs = doc.getElementsByTagName("DIV");
        assertEquals(500, divs.getLength(), "Should have 500 DIV elements");
    }

    @Test
    @Timeout(10)
    public void testDeeplyNestedSpans200Levels() throws Exception {
        // Given: 200 levels of nested SPANs
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 200; i++) {
            html.append("<span>");
        }
        html.append("Text");
        for (int i = 0; i < 200; i++) {
            html.append("</span>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle nested inline elements
        assertNotNull(doc, "Document should be parsed");
        final NodeList spans = doc.getElementsByTagName("SPAN");
        assertEquals(200, spans.getLength(), "Should have 200 SPAN elements");
    }

    @Test
    @Timeout(10)
    public void testDeeplyNestedTables50Levels() throws Exception {
        // Given: 50 levels of nested tables
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 50; i++) {
            html.append("<table><tr><td>");
        }
        html.append("Table content");
        for (int i = 0; i < 50; i++) {
            html.append("</td></tr></table>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle nested tables
        assertNotNull(doc, "Document should be parsed");
        final NodeList tables = doc.getElementsByTagName("TABLE");
        assertEquals(50, tables.getLength(), "Should have 50 TABLE elements");
    }

    @Test
    @Timeout(15)
    public void testDeeplyNestedLists100Levels() throws Exception {
        // Given: 100 levels of nested lists
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 100; i++) {
            html.append("<ul><li>");
        }
        html.append("List item");
        for (int i = 0; i < 100; i++) {
            html.append("</li></ul>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle deeply nested lists
        assertNotNull(doc, "Document should be parsed");
        final NodeList uls = doc.getElementsByTagName("UL");
        assertEquals(100, uls.getLength(), "Should have 100 UL elements");
    }

    @Test
    @Timeout(10)
    public void testMixedDeepNesting() throws Exception {
        // Given: Mixed deep nesting of different elements
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 50; i++) {
            html.append("<div><span><p><section>");
        }
        html.append("Content");
        for (int i = 0; i < 50; i++) {
            html.append("</section></p></span></div>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle mixed nesting
        assertNotNull(doc, "Document should be parsed");
        assertEquals(50, doc.getElementsByTagName("DIV").getLength(), "Should have 50 DIVs");
        assertEquals(50, doc.getElementsByTagName("SPAN").getLength(), "Should have 50 SPANs");
    }

    // ========================================================================
    // Large Document Tests
    // ========================================================================

    @Test
    @Timeout(15)
    public void testLargeDocumentWith10000Elements() throws Exception {
        // Given: Document with 10,000 DIV elements
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 10000; i++) {
            html.append("<div id=\"div").append(i).append("\">Content ").append(i).append("</div>");
        }
        html.append("</body></html>");

        // When: Parsing
        final long startTime = System.currentTimeMillis();
        final Document doc = parseHTML(html.toString());
        final long elapsedTime = System.currentTimeMillis() - startTime;

        // Then: Should parse efficiently
        assertNotNull(doc, "Document should be parsed");
        final NodeList divs = doc.getElementsByTagName("DIV");
        assertEquals(10000, divs.getLength(), "Should have 10,000 DIV elements");

        // Performance check (should parse in reasonable time)
        assertTrue(elapsedTime < 10000, "Should parse 10k elements in less than 10 seconds, took " + elapsedTime + "ms");
    }

    @Test
    @Timeout(20)
    public void testLargeDocumentWith50000Elements() throws Exception {
        // Given: Document with 50,000 paragraph elements
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 50000; i++) {
            html.append("<p>Paragraph ").append(i).append("</p>");
        }
        html.append("</body></html>");

        // When: Parsing
        final long startTime = System.currentTimeMillis();
        final Document doc = parseHTML(html.toString());
        final long elapsedTime = System.currentTimeMillis() - startTime;

        // Then: Should handle large number of elements
        assertNotNull(doc, "Document should be parsed");
        final NodeList paragraphs = doc.getElementsByTagName("P");
        assertEquals(50000, paragraphs.getLength(), "Should have 50,000 P elements");

        System.out.println("Parsed 50k elements in " + elapsedTime + "ms");
    }

    @Test
    @Timeout(10)
    public void testVeryLongTextNode() throws Exception {
        // Given: Document with very long text node (1MB)
        final StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000000; i++) {
            longText.append("x");
        }
        final String html = "<html><body><div>" + longText.toString() + "</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle large text nodes
        assertNotNull(doc, "Document should be parsed");
        final NodeList divs = doc.getElementsByTagName("DIV");
        assertEquals(1, divs.getLength(), "Should have DIV element");

        final String content = divs.item(0).getTextContent();
        assertEquals(1000000, content.length(), "Text content should be 1MB");
    }

    @Test
    @Timeout(15)
    public void testManyAttributes() throws Exception {
        // Given: Element with 1000 attributes
        final StringBuilder html = new StringBuilder("<html><body><div ");
        for (int i = 0; i < 1000; i++) {
            html.append("attr").append(i).append("=\"value").append(i).append("\" ");
        }
        html.append(">Content</div></body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle many attributes
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertNotNull(div, "DIV should exist");

        // Note: Duplicate attributes may be merged, so we check if parsing completed successfully
        assertTrue(div.hasAttributes(), "DIV should have attributes");
    }

    @Test
    @Timeout(10)
    public void testVeryLongAttributeValue() throws Exception {
        // Given: Element with very long attribute value (100KB)
        final StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            longValue.append("x");
        }
        final String html = "<html><body><div data-long=\"" + longValue.toString() + "\">Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle long attribute values
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertNotNull(div, "DIV should exist");

        final String attrValue = div.getAttribute("data-long");
        assertEquals(100000, attrValue.length(), "Attribute value should be 100KB");
    }

    // ========================================================================
    // Pathological Patterns
    // ========================================================================

    @Test
    @Timeout(15)
    public void testManyUnclosedTags() throws Exception {
        // Given: Many unclosed tags requiring auto-closing
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 1000; i++) {
            html.append("<p>Paragraph ").append(i);
            // Intentionally not closing P tags
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Parser should auto-close tags
        assertNotNull(doc, "Document should be parsed");
        final NodeList paragraphs = doc.getElementsByTagName("P");
        assertEquals(1000, paragraphs.getLength(), "Should have 1000 P elements");
    }

    @Test
    @Timeout(15)
    public void testManyFormattingElements() throws Exception {
        // Given: Many nested formatting elements (stress test for AAA)
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 100; i++) {
            html.append("<b><i><u><strong><em>");
        }
        html.append("Text");
        for (int i = 0; i < 100; i++) {
            html.append("</em></strong></u></i></b>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle many formatting elements
        assertNotNull(doc, "Document should be parsed");
        assertEquals(100, doc.getElementsByTagName("B").getLength(), "Should have 100 B elements");
        assertEquals(100, doc.getElementsByTagName("I").getLength(), "Should have 100 I elements");
    }

    @Test
    @Timeout(15)
    public void testFormattingElementsAcrossBlockBoundaries() throws Exception {
        // Given: Formatting elements crossing many block boundaries (AAA stress test)
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 100; i++) {
            html.append("<b>Bold <div>Block ").append(i).append("</div> continues</b>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle AAA reconstruction
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("B").getLength() > 0, "Should have B elements");
        assertEquals(100, doc.getElementsByTagName("DIV").getLength(), "Should have 100 DIV elements");
    }

    @Test
    @Timeout(15)
    public void testManyEntities() throws Exception {
        // Given: Document with many HTML entities
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 10000; i++) {
            html.append("&lt;&gt;&amp;&quot;&nbsp;");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle many entities
        assertNotNull(doc, "Document should be parsed");
        final String bodyText = doc.getElementsByTagName("BODY").item(0).getTextContent();
        assertTrue(bodyText.contains("<"), "Should resolve &lt; entity");
        assertTrue(bodyText.contains(">"), "Should resolve &gt; entity");
        assertTrue(bodyText.contains("&"), "Should resolve &amp; entity");
    }

    @Test
    @Timeout(10)
    public void testManyComments() throws Exception {
        // Given: Document with many comments
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 1000; i++) {
            html.append("<!-- Comment ").append(i).append(" -->");
            html.append("<div>Content ").append(i).append("</div>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle many comments
        assertNotNull(doc, "Document should be parsed");
        final NodeList divs = doc.getElementsByTagName("DIV");
        assertEquals(1000, divs.getLength(), "Should have 1000 DIV elements");
    }

    @Test
    @Timeout(15)
    public void testVeryLongComment() throws Exception {
        // Given: Document with very long comment (1MB)
        final StringBuilder longComment = new StringBuilder("<!-- ");
        for (int i = 0; i < 1000000; i++) {
            longComment.append("x");
        }
        longComment.append(" -->");
        final String html = "<html><body>" + longComment.toString() + "<div>Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle long comments
        assertNotNull(doc, "Document should be parsed");
        final NodeList divs = doc.getElementsByTagName("DIV");
        assertEquals(1, divs.getLength(), "Should have DIV element");
    }

    @Test
    @Timeout(10)
    public void testComplexNestedStructure() throws Exception {
        // Given: Complex realistic document structure
        final StringBuilder html = new StringBuilder("<html><head><title>Test</title></head><body>");
        for (int i = 0; i < 100; i++) {
            html.append("<article>")
                    .append("<header><h1>Title ").append(i).append("</h1></header>")
                    .append("<section>")
                    .append("<p>Paragraph 1</p>")
                    .append("<p>Paragraph 2</p>")
                    .append("<ul><li>Item 1</li><li>Item 2</li><li>Item 3</li></ul>")
                    .append("</section>")
                    .append("<footer><p>Footer</p></footer>")
                    .append("</article>");
        }
        html.append("</body></html>");

        // When: Parsing
        final long startTime = System.currentTimeMillis();
        final Document doc = parseHTML(html.toString());
        final long elapsedTime = System.currentTimeMillis() - startTime;

        // Then: Should handle complex structures efficiently
        assertNotNull(doc, "Document should be parsed");
        assertEquals(100, doc.getElementsByTagName("ARTICLE").getLength(), "Should have 100 ARTICLEs");
        assertEquals(100, doc.getElementsByTagName("HEADER").getLength(), "Should have 100 HEADERs");
        assertEquals(100, doc.getElementsByTagName("SECTION").getLength(), "Should have 100 SECTIONs");
        assertEquals(100, doc.getElementsByTagName("FOOTER").getLength(), "Should have 100 FOOTERs");
        assertEquals(200, doc.getElementsByTagName("P").getLength(), "Should have 200 Ps");
        assertEquals(100, doc.getElementsByTagName("UL").getLength(), "Should have 100 ULs");
        assertEquals(300, doc.getElementsByTagName("LI").getLength(), "Should have 300 LIs");

        System.out.println("Parsed complex structure (100 articles) in " + elapsedTime + "ms");
    }

    @Test
    @Timeout(15)
    public void testWideStructure() throws Exception {
        // Given: Very wide structure (many siblings, not deep)
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 10000; i++) {
            html.append("<span>S").append(i).append("</span>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle wide structures
        assertNotNull(doc, "Document should be parsed");
        final NodeList spans = doc.getElementsByTagName("SPAN");
        assertEquals(10000, spans.getLength(), "Should have 10,000 SPAN elements");
    }

    @Test
    @Timeout(10)
    public void testManyEmptyElements() throws Exception {
        // Given: Many empty elements
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 5000; i++) {
            html.append("<div></div>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle many empty elements
        assertNotNull(doc, "Document should be parsed");
        final NodeList divs = doc.getElementsByTagName("DIV");
        assertEquals(5000, divs.getLength(), "Should have 5,000 empty DIV elements");
    }

    @Test
    @Timeout(15)
    public void testLargeTableWithManyCells() throws Exception {
        // Given: Large table with 100x100 cells
        final StringBuilder html = new StringBuilder("<html><body><table>");
        for (int i = 0; i < 100; i++) {
            html.append("<tr>");
            for (int j = 0; j < 100; j++) {
                html.append("<td>Cell ").append(i).append(",").append(j).append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</table></body></html>");

        // When: Parsing
        final long startTime = System.currentTimeMillis();
        final Document doc = parseHTML(html.toString());
        final long elapsedTime = System.currentTimeMillis() - startTime;

        // Then: Should handle large tables
        assertNotNull(doc, "Document should be parsed");
        final NodeList trs = doc.getElementsByTagName("TR");
        assertEquals(100, trs.getLength(), "Should have 100 rows");
        final NodeList tds = doc.getElementsByTagName("TD");
        assertEquals(10000, tds.getLength(), "Should have 10,000 cells");

        System.out.println("Parsed 100x100 table in " + elapsedTime + "ms");
    }

    @Test
    @Timeout(10)
    public void testManyScriptTags() throws Exception {
        // Given: Many script tags
        final StringBuilder html = new StringBuilder("<html><head>");
        for (int i = 0; i < 1000; i++) {
            html.append("<script>console.log(").append(i).append(");</script>");
        }
        html.append("</head><body>Content</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle many script tags
        assertNotNull(doc, "Document should be parsed");
        final NodeList scripts = doc.getElementsByTagName("SCRIPT");
        assertEquals(1000, scripts.getLength(), "Should have 1000 SCRIPT elements");
    }

    @Test
    @Timeout(10)
    public void testMixedContentStressTest() throws Exception {
        // Given: Mixed content with various element types
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 100; i++) {
            html.append("<div><span>Text</span></div>");
            html.append("<p>Paragraph <b>bold</b> <i>italic</i></p>");
            html.append("<ul><li>Item 1</li><li>Item 2</li></ul>");
            html.append("<table><tr><td>Cell</td></tr></table>");
            html.append("<!-- Comment -->");
            html.append("<section><article><h2>Title</h2></article></section>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle mixed content
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("DIV").getLength() > 0, "Should have DIVs");
        assertTrue(doc.getElementsByTagName("P").getLength() > 0, "Should have paragraphs");
        assertTrue(doc.getElementsByTagName("UL").getLength() > 0, "Should have lists");
        assertTrue(doc.getElementsByTagName("TABLE").getLength() > 0, "Should have tables");
    }
}
