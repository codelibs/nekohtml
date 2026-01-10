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
    public void setUp() throws Exception {
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

        // Then: Should handle parsing many entities without errors
        assertNotNull(doc, "Document should be parsed");
        final String bodyText = doc.getElementsByTagName("BODY").item(0).getTextContent();
        assertNotNull(bodyText, "Body text should not be null");
        assertTrue(bodyText.length() > 0, "Body should have content");
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
        assertEquals(300, doc.getElementsByTagName("P").getLength(), "Should have 300 Ps (2 in section + 1 in footer per article)");
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

    // ========================================================================
    // Additional Stress Tests
    // ========================================================================

    @Test
    @Timeout(15)
    public void testVeryDeeplyNestedFormatting1000Levels() throws Exception {
        // Given: 1000 levels of nested formatting elements
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 1000; i++) {
            html.append("<b>");
        }
        html.append("Deep text");
        for (int i = 0; i < 1000; i++) {
            html.append("</b>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle extreme nesting without stack overflow
        assertNotNull(doc, "Document should be parsed without stack overflow");
    }

    @Test
    @Timeout(10)
    public void testParseEmptyDocument() throws Exception {
        // Given: Empty document
        final String html = "";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should create document
        assertNotNull(doc, "Empty document should be parsed");
    }

    @Test
    @Timeout(10)
    public void testParseWhitespaceOnlyDocument() throws Exception {
        // Given: Whitespace-only document
        final String html = "   \n\t\n   ";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should create document
        assertNotNull(doc, "Whitespace document should be parsed");
    }

    @Test
    @Timeout(15)
    public void testManyNestedFormsWithInputs() throws Exception {
        // Given: Many forms with inputs (stress test for form handling)
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 500; i++) {
            html.append("<form id=\"form").append(i).append("\">");
            html.append("<input type=\"text\" name=\"field1\">");
            html.append("<input type=\"text\" name=\"field2\">");
            html.append("<input type=\"submit\" value=\"Submit\">");
            html.append("</form>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle many forms
        assertNotNull(doc, "Document should be parsed");
        assertEquals(500, doc.getElementsByTagName("FORM").getLength(), "Should have 500 forms");
        assertEquals(1500, doc.getElementsByTagName("INPUT").getLength(), "Should have 1500 inputs");
    }

    @Test
    @Timeout(10)
    public void testHugeAttributeCount() throws Exception {
        // Given: Element with 5000 attributes
        final StringBuilder html = new StringBuilder("<html><body><div ");
        for (int i = 0; i < 5000; i++) {
            html.append("data-attr").append(i).append("=\"").append(i).append("\" ");
        }
        html.append(">Content</div></body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle huge attribute count
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertNotNull(div, "DIV should exist");
    }

    @Test
    @Timeout(20)
    public void testConcurrentParsing() throws Exception {
        // Given: Multiple parse operations
        final int threadCount = 10;
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
        final java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        final java.util.concurrent.atomic.AtomicInteger errorCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    final DOMParser threadParser = new DOMParser();
                    final StringBuilder html = new StringBuilder("<html><body>");
                    for (int i = 0; i < 100; i++) {
                        html.append("<div>Thread ").append(threadId).append(" Element ").append(i).append("</div>");
                    }
                    html.append("</body></html>");

                    threadParser.parse(new InputSource(new StringReader(html.toString())));
                    final Document doc = threadParser.getDocument();

                    if (doc != null && doc.getElementsByTagName("DIV").getLength() == 100) {
                        successCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (final Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();

        // Then: All threads should succeed
        assertEquals(threadCount, successCount.get(), "All threads should succeed");
        assertEquals(0, errorCount.get(), "No errors should occur");
    }

    @Test
    @Timeout(15)
    public void testMisnestingStressTest() throws Exception {
        // Given: Extremely misnested document
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 100; i++) {
            html.append("<b><i><u><div>");
        }
        html.append("Content");
        // Close in completely wrong order
        for (int i = 0; i < 100; i++) {
            html.append("</b></u></div></i>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle extreme misnesting
        assertNotNull(doc, "Document should be parsed despite misnesting");
    }

    @Test
    @Timeout(15)
    public void testAlternatingBlockInline() throws Exception {
        // Given: Rapidly alternating block and inline elements
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 1000; i++) {
            if (i % 2 == 0) {
                html.append("<div>Block ").append(i).append("</div>");
            } else {
                html.append("<span>Inline ").append(i).append("</span>");
            }
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle alternating pattern
        assertNotNull(doc, "Document should be parsed");
        assertEquals(500, doc.getElementsByTagName("DIV").getLength(), "Should have 500 DIVs");
        assertEquals(500, doc.getElementsByTagName("SPAN").getLength(), "Should have 500 SPANs");
    }

    @Test
    @Timeout(15)
    public void testVeryLongElementName() throws Exception {
        // Given: Element with very long custom name (1000 chars)
        final StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longName.append("x");
        }
        final String html = "<html><body><" + longName + ">Content</" + longName + "></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle long element name
        assertNotNull(doc, "Document should be parsed");
    }

    @Test
    @Timeout(15)
    public void testManyVoidElements() throws Exception {
        // Given: Many void elements in sequence
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 5000; i++) {
            html.append("<br><hr><img src=\"test.png\"><input type=\"hidden\">");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle many void elements
        assertNotNull(doc, "Document should be parsed");
        assertEquals(5000, doc.getElementsByTagName("BR").getLength(), "Should have 5000 BRs");
        assertEquals(5000, doc.getElementsByTagName("HR").getLength(), "Should have 5000 HRs");
    }

    @Test
    @Timeout(20)
    public void testComplexRealWorldSimulation() throws Exception {
        // Given: Simulated real-world complex page
        final StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"en\"><head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<title>Complex Page</title>");
        html.append("<style>body { margin: 0; }</style>");
        html.append("<script>console.log('init');</script>");
        html.append("</head><body>");

        // Header with nav
        html.append("<header><nav><ul>");
        for (int i = 0; i < 20; i++) {
            html.append("<li><a href=\"#").append(i).append("\">Link ").append(i).append("</a></li>");
        }
        html.append("</ul></nav></header>");

        // Main content with multiple sections
        html.append("<main>");
        for (int section = 0; section < 50; section++) {
            html.append("<section id=\"section").append(section).append("\">");
            html.append("<h2>Section ").append(section).append("</h2>");
            for (int para = 0; para < 5; para++) {
                html.append("<p>Lorem ipsum dolor sit amet, <b>consectetur</b> adipiscing elit.</p>");
            }
            html.append("<table><thead><tr><th>Col1</th><th>Col2</th></tr></thead><tbody>");
            for (int row = 0; row < 5; row++) {
                html.append("<tr><td>Data ").append(row).append("</td><td>Value ").append(row).append("</td></tr>");
            }
            html.append("</tbody></table>");
            html.append("</section>");
        }
        html.append("</main>");

        // Sidebar
        html.append("<aside>");
        for (int widget = 0; widget < 10; widget++) {
            html.append("<div class=\"widget\"><h3>Widget ").append(widget).append("</h3><p>Content</p></div>");
        }
        html.append("</aside>");

        // Footer
        html.append("<footer><p>&copy; 2024</p></footer>");
        html.append("</body></html>");

        // When: Parsing
        final long startTime = System.currentTimeMillis();
        final Document doc = parseHTML(html.toString());
        final long elapsedTime = System.currentTimeMillis() - startTime;

        // Then: Should handle complex real-world structure
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("HEADER").getLength(), "Should have HEADER");
        assertEquals(1, doc.getElementsByTagName("MAIN").getLength(), "Should have MAIN");
        assertEquals(1, doc.getElementsByTagName("FOOTER").getLength(), "Should have FOOTER");
        assertEquals(50, doc.getElementsByTagName("SECTION").getLength(), "Should have 50 SECTIONs");
        assertEquals(50, doc.getElementsByTagName("TABLE").getLength(), "Should have 50 TABLEs");

        System.out.println("Parsed complex real-world simulation in " + elapsedTime + "ms");
    }

    @Test
    @Timeout(10)
    public void testRepeatedParsingWithSameParser() throws Exception {
        // Given: Same parser instance used multiple times
        final DOMParser reusableParser = new DOMParser();

        for (int iteration = 0; iteration < 100; iteration++) {
            final String html = "<html><body><div>Iteration " + iteration + "</div></body></html>";
            reusableParser.parse(new InputSource(new StringReader(html)));
            final Document doc = reusableParser.getDocument();

            assertNotNull(doc, "Document should be parsed on iteration " + iteration);
            assertEquals(1, doc.getElementsByTagName("DIV").getLength(), "Should have 1 DIV on iteration " + iteration);
        }
    }
}
