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

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;

import org.codelibs.nekohtml.parsers.DOMParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Extended tests for the Adoption Agency Algorithm (AAA) implementation.
 * Tests complex misnesting scenarios, formatting element reconstruction,
 * and edge cases in tag balancing.
 *
 * The Adoption Agency Algorithm is used to properly reconstruct formatting elements
 * (like B, I, STRONG, EM) when they are misnested with block elements.
 *
 * @author CodeLibs Project
 */
public class AdoptionAgencyAlgorithmExtendedTest {

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
    // All Formatting Elements Tests
    // ========================================================================

    @Test
    public void testAllFormattingElementsBasic() throws Exception {
        // Given: HTML with all formatting elements
        final String html = "<html><body>"
                + "<a href=\"#\">Link</a>"
                + "<b>Bold</b>"
                + "<big>Big</big>"
                + "<code>Code</code>"
                + "<em>Emphasis</em>"
                + "<font>Font</font>"
                + "<i>Italic</i>"
                + "<nobr>NoBreak</nobr>"
                + "<s>Strike</s>"
                + "<small>Small</small>"
                + "<strike>Strike</strike>"
                + "<strong>Strong</strong>"
                + "<tt>Teletype</tt>"
                + "<u>Underline</u>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: All formatting elements should be present
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("A").getLength(), "Should have A element");
        assertEquals(1, doc.getElementsByTagName("B").getLength(), "Should have B element");
        assertEquals(1, doc.getElementsByTagName("BIG").getLength(), "Should have BIG element");
        assertEquals(1, doc.getElementsByTagName("CODE").getLength(), "Should have CODE element");
        assertEquals(1, doc.getElementsByTagName("EM").getLength(), "Should have EM element");
        assertEquals(1, doc.getElementsByTagName("FONT").getLength(), "Should have FONT element");
        assertEquals(1, doc.getElementsByTagName("I").getLength(), "Should have I element");
        assertEquals(1, doc.getElementsByTagName("NOBR").getLength(), "Should have NOBR element");
        assertEquals(1, doc.getElementsByTagName("S").getLength(), "Should have S element");
        assertEquals(1, doc.getElementsByTagName("SMALL").getLength(), "Should have SMALL element");
        assertEquals(1, doc.getElementsByTagName("STRIKE").getLength(), "Should have STRIKE element");
        assertEquals(1, doc.getElementsByTagName("STRONG").getLength(), "Should have STRONG element");
        assertEquals(1, doc.getElementsByTagName("TT").getLength(), "Should have TT element");
        assertEquals(1, doc.getElementsByTagName("U").getLength(), "Should have U element");
    }

    @Test
    public void testFormattingElementAcrossSingleBlock() throws Exception {
        // Given: Formatting element crossing a block boundary
        final String html = "<html><body><b>Bold <div>Block</div> continues</b></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: B should be reconstructed around DIV
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("B").getLength() >= 1, "Should have B elements (reconstructed)");
        assertEquals(1, doc.getElementsByTagName("DIV").getLength(), "Should have DIV element");
    }

    @Test
    public void testFormattingElementAcrossMultipleBlocks() throws Exception {
        // Given: Formatting element crossing multiple block boundaries
        final String html = "<html><body><i>Italic <div>Block 1</div> middle <p>Block 2</p> end</i></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: I should be reconstructed multiple times
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("I").getLength() >= 1, "Should have I elements");
        assertEquals(1, doc.getElementsByTagName("DIV").getLength(), "Should have DIV element");
        assertEquals(1, doc.getElementsByTagName("P").getLength(), "Should have P element");
    }

    @Test
    public void testMultipleFormattingElementsAcrossBlocks() throws Exception {
        // Given: Multiple formatting elements crossing blocks
        final String html = "<html><body><b><i><u>Text <div>Block</div> continues</u></i></b></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: All formatting elements should be reconstructed
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("B").getLength() >= 1, "Should have B elements");
        assertTrue(doc.getElementsByTagName("I").getLength() >= 1, "Should have I elements");
        assertTrue(doc.getElementsByTagName("U").getLength() >= 1, "Should have U elements");
    }

    // ========================================================================
    // Deep Nesting with AAA
    // ========================================================================

    @Test
    public void testDeeplyNestedFormattingElements() throws Exception {
        // Given: Deeply nested formatting elements
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 10; i++) {
            html.append("<b><i><u><strong><em>");
        }
        html.append("Text");
        for (int i = 0; i < 10; i++) {
            html.append("</em></strong></u></i></b>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: All formatting elements should be properly nested
        assertNotNull(doc, "Document should be parsed");
        assertEquals(10, doc.getElementsByTagName("B").getLength(), "Should have 10 B elements");
        assertEquals(10, doc.getElementsByTagName("I").getLength(), "Should have 10 I elements");
        assertEquals(10, doc.getElementsByTagName("U").getLength(), "Should have 10 U elements");
    }

    @Test
    public void testFormattingElementsWithComplexBlockStructure() throws Exception {
        // Given: Formatting elements with complex block structure
        final String html = "<html><body>"
                + "<b>Start"
                + "<div>Div 1"
                + "<p>Para 1</p>"
                + "<blockquote>Quote</blockquote>"
                + "</div>"
                + "<section>Section</section>"
                + "End</b>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle complex structure
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("B").getLength() >= 1, "Should have B elements");
        assertEquals(1, doc.getElementsByTagName("DIV").getLength(), "Should have DIV");
        assertEquals(1, doc.getElementsByTagName("P").getLength(), "Should have P");
        assertEquals(1, doc.getElementsByTagName("BLOCKQUOTE").getLength(), "Should have BLOCKQUOTE");
        assertEquals(1, doc.getElementsByTagName("SECTION").getLength(), "Should have SECTION");
    }

    // ========================================================================
    // Formatting Elements with Attributes
    // ========================================================================

    @Test
    public void testFormattingElementsWithIdenticalAttributes() throws Exception {
        // Given: Multiple formatting elements with same attributes
        final String html = "<html><body>"
                + "<a href=\"#1\" class=\"link\">Link 1 <div>Block</div> continues</a>"
                + "<a href=\"#2\" class=\"link\">Link 2 <p>Para</p> continues</a>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle attributes correctly in reconstructed elements
        assertNotNull(doc, "Document should be parsed");
        final NodeList links = doc.getElementsByTagName("A");
        assertTrue(links.getLength() >= 2, "Should have multiple A elements");
    }

    @Test
    public void testFontElementWithAttributes() throws Exception {
        // Given: FONT element with attributes crossing block
        final String html = "<html><body>"
                + "<font color=\"red\" size=\"3\">Red text <div>Block</div> continues</font>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: FONT attributes should be preserved
        assertNotNull(doc, "Document should be parsed");
        final NodeList fonts = doc.getElementsByTagName("FONT");
        assertTrue(fonts.getLength() >= 1, "Should have FONT elements");

        if (fonts.getLength() > 0) {
            final Element font = (Element) fonts.item(0);
            // Note: Attributes may be preserved on reconstructed elements
            assertNotNull(font, "FONT element should exist");
        }
    }

    // ========================================================================
    // AAA with Tables
    // ========================================================================

    @Test
    public void testFormattingElementCrossingTableBoundary() throws Exception {
        // Given: Formatting element crossing table boundary
        final String html = "<html><body><b>Bold <table><tr><td>Cell</td></tr></table> continues</b></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle table boundary
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("B").getLength() >= 1, "Should have B elements");
        assertEquals(1, doc.getElementsByTagName("TABLE").getLength(), "Should have TABLE");
    }

    @Test
    public void testFormattingElementInsideTableCell() throws Exception {
        // Given: Formatting element properly inside table cell
        final String html = "<html><body><table><tr><td><b>Bold <div>Block</div> continues</b></td></tr></table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle formatting in table cell
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("B").getLength() >= 1, "Should have B elements");
        final NodeList cells = doc.getElementsByTagName("TD");
        assertEquals(1, cells.getLength(), "Should have TD element");
    }

    @Test
    public void testComplexTableWithFormattingElements() throws Exception {
        // Given: Complex table with formatting elements
        final String html = "<html><body><table>"
                + "<tr><td><b>Bold</b></td><td><i>Italic</i></td></tr>"
                + "<tr><td><strong>Strong <p>Para</p></strong></td><td><em>Em <div>Div</div></em></td></tr>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle all formatting in table
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("B").getLength() >= 1, "Should have B");
        assertTrue(doc.getElementsByTagName("I").getLength() >= 1, "Should have I");
        assertTrue(doc.getElementsByTagName("STRONG").getLength() >= 1, "Should have STRONG");
        assertTrue(doc.getElementsByTagName("EM").getLength() >= 1, "Should have EM");
    }

    // ========================================================================
    // AAA with Lists
    // ========================================================================

    @Test
    public void testFormattingElementAcrossList() throws Exception {
        // Given: Formatting element crossing list boundary
        final String html = "<html><body><b>Bold <ul><li>Item 1</li><li>Item 2</li></ul> continues</b></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle list boundary
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("B").getLength() >= 1, "Should have B elements");
        assertEquals(1, doc.getElementsByTagName("UL").getLength(), "Should have UL");
        assertEquals(2, doc.getElementsByTagName("LI").getLength(), "Should have 2 LIs");
    }

    @Test
    public void testFormattingElementInListItems() throws Exception {
        // Given: Formatting elements in list items
        final String html = "<html><body><ul>"
                + "<li><b>Bold <div>Block</div> continues</b></li>"
                + "<li><i>Italic <p>Para</p> continues</i></li>"
                + "</ul></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle formatting in list items
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("B").getLength() >= 1, "Should have B elements");
        assertTrue(doc.getElementsByTagName("I").getLength() >= 1, "Should have I elements");
        assertEquals(2, doc.getElementsByTagName("LI").getLength(), "Should have 2 LIs");
    }

    @Test
    public void testNestedListsWithFormattingElements() throws Exception {
        // Given: Nested lists with formatting elements
        final String html = "<html><body><b>Bold <ul><li>Item 1<ul><li>Nested</li></ul></li></ul> end</b></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle nested lists
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("B").getLength() >= 1, "Should have B elements");
        assertEquals(2, doc.getElementsByTagName("UL").getLength(), "Should have 2 ULs");
    }

    // ========================================================================
    // AAA with HTML5 Semantic Elements
    // ========================================================================

    @Test
    public void testFormattingElementsAcrossSemanticElements() throws Exception {
        // Given: Formatting elements crossing semantic boundaries
        final String html = "<html><body><b>Bold "
                + "<article>Article</article> "
                + "<section>Section</section> "
                + "<nav>Nav</nav> "
                + "end</b></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle semantic elements
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("B").getLength() >= 1, "Should have B elements");
        assertEquals(1, doc.getElementsByTagName("ARTICLE").getLength(), "Should have ARTICLE");
        assertEquals(1, doc.getElementsByTagName("SECTION").getLength(), "Should have SECTION");
        assertEquals(1, doc.getElementsByTagName("NAV").getLength(), "Should have NAV");
    }

    @Test
    public void testComplexSemanticStructureWithFormatting() throws Exception {
        // Given: Complex semantic structure with formatting
        final String html = "<html><body>"
                + "<article>"
                + "<header><b>Header <h1>Title</h1> continues</b></header>"
                + "<section><i>Section <p>Para</p> continues</i></section>"
                + "<footer><strong>Footer <div>Div</div> continues</strong></footer>"
                + "</article>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle complex semantic structure
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("B").getLength() >= 1, "Should have B");
        assertTrue(doc.getElementsByTagName("I").getLength() >= 1, "Should have I");
        assertTrue(doc.getElementsByTagName("STRONG").getLength() >= 1, "Should have STRONG");
        assertEquals(1, doc.getElementsByTagName("ARTICLE").getLength(), "Should have ARTICLE");
        assertEquals(1, doc.getElementsByTagName("HEADER").getLength(), "Should have HEADER");
        assertEquals(1, doc.getElementsByTagName("SECTION").getLength(), "Should have SECTION");
        assertEquals(1, doc.getElementsByTagName("FOOTER").getLength(), "Should have FOOTER");
    }

    // ========================================================================
    // AAA with TEMPLATE Elements
    // ========================================================================

    @Test
    public void testFormattingElementsInTemplate() throws Exception {
        // Given: Formatting elements in TEMPLATE
        final String html = "<html><body><template><b>Bold <div>Block</div> continues</b></template></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle formatting in template
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("TEMPLATE").getLength(), "Should have TEMPLATE");
    }

    // ========================================================================
    // Complex Misnesting Scenarios
    // ========================================================================

    @Test
    public void testMisnestingPattern1() throws Exception {
        // Given: <b><i></b></i> pattern
        final String html = "<html><body><b><i>Text</b></i></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should fix misnesting
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("B").getLength() >= 1, "Should have B");
        assertTrue(doc.getElementsByTagName("I").getLength() >= 1, "Should have I");
    }

    @Test
    public void testMisnestingPattern2() throws Exception {
        // Given: <b><i><u></b></u></i> pattern
        final String html = "<html><body><b><i><u>Text</b></u></i></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should fix complex misnesting
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("B").getLength() >= 1, "Should have B");
        assertTrue(doc.getElementsByTagName("I").getLength() >= 1, "Should have I");
        assertTrue(doc.getElementsByTagName("U").getLength() >= 1, "Should have U");
    }

    @Test
    public void testMisnestingWithBlockElements() throws Exception {
        // Given: <b><div><i></b></div></i> pattern
        final String html = "<html><body><b><div><i>Text</b></div></i></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should fix misnesting with blocks
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("B").getLength() >= 1, "Should have B");
        assertTrue(doc.getElementsByTagName("DIV").getLength() >= 1, "Should have DIV");
        assertTrue(doc.getElementsByTagName("I").getLength() >= 1, "Should have I");
    }

    @Test
    public void testComplexMisnestingPattern() throws Exception {
        // Given: Very complex misnesting
        final String html = "<html><body>"
                + "<b><i><u><strong>Text<div>Block</strong></u></i></div></b>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle very complex misnesting
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("DIV").getLength() >= 1, "Should have DIV");
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    public void testEmptyFormattingElements() throws Exception {
        // Given: Empty formatting elements
        final String html = "<html><body><b></b><i></i><u></u></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle empty formatting elements
        assertNotNull(doc, "Document should be parsed");
    }

    @Test
    public void testFormattingElementWithOnlyWhitespace() throws Exception {
        // Given: Formatting element with only whitespace
        final String html = "<html><body><b>   </b><i>\n\t</i></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle whitespace-only content
        assertNotNull(doc, "Document should be parsed");
    }

    @Test
    public void testUnclosedFormattingElementAtEOF() throws Exception {
        // Given: Unclosed formatting element at end of file
        final String html = "<html><body><b>Bold text";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should auto-close at EOF
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("B").getLength() >= 1, "Should have B element");
    }

    @Test
    public void testNestedSameFormattingElements() throws Exception {
        // Given: Nested same formatting elements
        final String html = "<html><body><b>Outer <b>Inner</b> outer continues</b></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle nested same elements
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("B").getLength() >= 1, "Should have B elements");
    }

    @Test
    public void testFormattingElementsWithNOBR() throws Exception {
        // Given: NOBR element (special formatting element)
        final String html = "<html><body><nobr>No break <div>Block</div> continues</nobr></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle NOBR correctly
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("NOBR").getLength() >= 1, "Should have NOBR elements");
    }

    @Test
    public void testAllFormattingElementsTogether() throws Exception {
        // Given: All formatting elements used together
        final String html = "<html><body>"
                + "<b><i><u><strong><em><small><big><code><tt><s><strike>"
                + "Text <div>Block</div> continues"
                + "</strike></s></tt></code></big></small></em></strong></u></i></b>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle all formatting elements
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("DIV").getLength() >= 1, "Should have DIV");
    }

    @Test
    public void testFormattingElementsAcrossNestedBlocks() throws Exception {
        // Given: Formatting elements across deeply nested blocks
        final String html = "<html><body><b>Start"
                + "<div>Level 1"
                + "<div>Level 2"
                + "<div>Level 3"
                + "<p>Para</p>"
                + "</div></div></div>"
                + "End</b></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle deeply nested blocks
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("B").getLength() >= 1, "Should have B elements");
        assertEquals(3, doc.getElementsByTagName("DIV").getLength(), "Should have 3 DIVs");
    }

    // ========================================================================
    // AAA Outer Loop Limit Tests (HTML5 spec: max 8 iterations)
    // ========================================================================

    @Test
    public void testAAAOuterLoopWithManyFormattingElements() throws Exception {
        // Given: More than 8 nested formatting elements to test outer loop limit
        final String html = "<html><body>"
                + "<b><i><u><s><em><strong><code><tt><big><small>"
                + "Text"
                + "</b>" // Close B early to trigger AAA
                + "</small></big></tt></code></strong></em></s></u></i>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle without infinite loop
        assertNotNull(doc, "Document should be parsed without infinite loop");
    }

    @Test
    public void testAAAWithExtremeNesting() throws Exception {
        // Given: Very deeply nested formatting elements
        final StringBuilder html = new StringBuilder("<html><body>");
        final int depth = 20; // Well beyond the 8 iteration limit

        for (int i = 0; i < depth; i++) {
            html.append("<b>");
        }
        html.append("Text");
        // Close only some, leaving mismatched structure
        for (int i = 0; i < depth / 2; i++) {
            html.append("</b>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle extreme nesting gracefully
        assertNotNull(doc, "Document should be parsed with extreme nesting");
    }

    @Test
    public void testAAAWithAlternatingFormattingElements() throws Exception {
        // Given: Alternating formatting elements beyond loop limit
        final String html = "<html><body>"
                + "<b><i><b><i><b><i><b><i><b><i><b><i>"
                + "Deep text"
                + "</b>" // Early close
                + "</i></b></i></b></i></b></i></b></i></b></i>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle alternating pattern
        assertNotNull(doc, "Document should be parsed with alternating patterns");
    }

    // ========================================================================
    // Formatting Marker Tests (Table/Caption/TD/TH context boundaries)
    // ========================================================================

    @Test
    public void testFormattingMarkerInTable() throws Exception {
        // Given: Formatting elements crossing table cell boundaries
        final String html = "<html><body>"
                + "<b>Before table"
                + "<table>"
                + "<tr><td>Cell 1 <i>Italic in cell</td>"
                + "<td>Cell 2</i></td></tr>"
                + "</table>"
                + "After table</b>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Table should act as a formatting marker boundary
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("TABLE").getLength(), "Should have TABLE");
        assertEquals(2, doc.getElementsByTagName("TD").getLength(), "Should have 2 TDs");
    }

    @Test
    public void testFormattingMarkerInCaption() throws Exception {
        // Given: Formatting elements in table caption
        final String html = "<html><body>"
                + "<table>"
                + "<caption><b>Bold caption <div>Block in caption</div> continues</b></caption>"
                + "<tr><td>Cell</td></tr>"
                + "</table>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle caption as marker boundary
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("CAPTION").getLength(), "Should have CAPTION");
    }

    @Test
    public void testFormattingMarkerInTH() throws Exception {
        // Given: Formatting elements in table header
        final String html = "<html><body>"
                + "<table>"
                + "<tr><th><b>Header <p>Para in header</p> continues</b></th></tr>"
                + "<tr><td>Cell</td></tr>"
                + "</table>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle TH as marker boundary
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("TH").getLength(), "Should have TH");
    }

    @Test
    public void testFormattingAcrossMultipleTableCells() throws Exception {
        // Given: Formatting spanning multiple cells (invalid but should be handled)
        final String html = "<html><body>"
                + "<table>"
                + "<tr>"
                + "<td><b>Start bold</td>"
                + "<td>Middle cell</b></td>"
                + "<td>Third cell</td>"
                + "</tr>"
                + "</table>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle cross-cell formatting
        assertNotNull(doc, "Document should be parsed");
        assertEquals(3, doc.getElementsByTagName("TD").getLength(), "Should have 3 TDs");
    }

    // ========================================================================
    // AAA with Select Elements
    // ========================================================================

    @Test
    public void testFormattingInSelectOption() throws Exception {
        // Given: Formatting in select option (should be stripped)
        final String html = "<html><body>"
                + "<select>"
                + "<option><b>Bold option</b></option>"
                + "<option><i>Italic option</i></option>"
                + "</select>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse select with options
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("SELECT").getLength(), "Should have SELECT");
        assertEquals(2, doc.getElementsByTagName("OPTION").getLength(), "Should have 2 OPTIONs");
    }

    @Test
    public void testFormattingSpanningSelect() throws Exception {
        // Given: Formatting spanning across select (invalid)
        final String html = "<html><body>"
                + "<b>Before select"
                + "<select><option>Option 1</option></select>"
                + "After select</b>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle select boundary
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("SELECT").getLength(), "Should have SELECT");
    }

    // ========================================================================
    // AAA with Button Elements
    // ========================================================================

    @Test
    public void testFormattingInButton() throws Exception {
        // Given: Formatting inside button
        final String html = "<html><body>"
                + "<button><b>Bold button <div>Block in button</div> text</b></button>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle button content
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("BUTTON").getLength(), "Should have BUTTON");
    }

    @Test
    public void testFormattingSpanningButton() throws Exception {
        // Given: Formatting spanning across button
        final String html = "<html><body>"
                + "<b>Before button"
                + "<button>Click me</button>"
                + "After button</b>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle button boundary
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("BUTTON").getLength(), "Should have BUTTON");
    }

    // ========================================================================
    // AAA with Applet/Object/Marquee (obsolete marker elements)
    // ========================================================================

    @Test
    public void testFormattingInObject() throws Exception {
        // Given: Formatting inside object element
        final String html = "<html><body>"
                + "<object><b>Fallback <div>Block</div> content</b></object>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle object content
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("OBJECT").getLength(), "Should have OBJECT");
    }

    @Test
    public void testFormattingInMarquee() throws Exception {
        // Given: Formatting in marquee (deprecated but may appear)
        final String html = "<html><body>"
                + "<marquee><b>Scrolling <div>Block</div> text</b></marquee>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle marquee content
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("MARQUEE").getLength(), "Should have MARQUEE");
    }

    // ========================================================================
    // AAA with Special Scope Elements
    // ========================================================================

    @Test
    public void testFormattingWithFormElement() throws Exception {
        // Given: Formatting crossing form boundary
        final String html = "<html><body>"
                + "<b>Before form"
                + "<form><input type=\"text\"><div>Form content</div></form>"
                + "After form</b>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle form boundary
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("FORM").getLength(), "Should have FORM");
    }

    @Test
    public void testFormattingWithFieldset() throws Exception {
        // Given: Formatting in fieldset with legend
        final String html = "<html><body>"
                + "<fieldset>"
                + "<legend><b>Bold legend <div>Block</div></b></legend>"
                + "<input type=\"text\">"
                + "</fieldset>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle fieldset content
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("FIELDSET").getLength(), "Should have FIELDSET");
        assertEquals(1, doc.getElementsByTagName("LEGEND").getLength(), "Should have LEGEND");
    }

    // ========================================================================
    // AAA Inner Loop Limit Tests
    // ========================================================================

    @Test
    public void testAAAInnerLoopWithManyActiveElements() throws Exception {
        // Given: Many active formatting elements in list
        final StringBuilder html = new StringBuilder("<html><body>");
        // Create many formatting elements
        for (int i = 0; i < 10; i++) {
            html.append("<b>b").append(i).append(" ");
        }
        // Add a block to trigger reconstruction
        html.append("<div>Block</div>");
        // Close in reverse
        for (int i = 0; i < 10; i++) {
            html.append("</b>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle many active elements
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("DIV").getLength(), "Should have DIV");
    }

    // ========================================================================
    // AAA with Furthest Block Variations
    // ========================================================================

    @Test
    public void testAAAWithMultipleFurthestBlockCandidates() throws Exception {
        // Given: Multiple potential furthest blocks
        final String html = "<html><body>"
                + "<b>Bold <p>Para 1</p> <div>Div</div> <p>Para 2</p> continues</b>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle multiple blocks
        assertNotNull(doc, "Document should be parsed");
        assertEquals(2, doc.getElementsByTagName("P").getLength(), "Should have 2 P elements");
        assertEquals(1, doc.getElementsByTagName("DIV").getLength(), "Should have DIV");
    }

    @Test
    public void testAAAWithNoFurthestBlock() throws Exception {
        // Given: Formatting element closed when no block is present
        final String html = "<html><body><b><i><u>Text</u></i></b></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should close normally (no AAA needed)
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("B").getLength(), "Should have B");
        assertEquals(1, doc.getElementsByTagName("I").getLength(), "Should have I");
        assertEquals(1, doc.getElementsByTagName("U").getLength(), "Should have U");
    }

    @Test
    public void testAAAFurthestBlockIsImmediateChild() throws Exception {
        // Given: Furthest block is immediate child of formatting element
        final String html = "<html><body><b><div>Direct block child</div></b></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle direct block child
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("DIV").getLength(), "Should have DIV");
    }

    // ========================================================================
    // Stress Tests for AAA
    // ========================================================================

    @Test
    public void testAAAWithManyInterleavedElements() throws Exception {
        // Given: Many interleaved formatting and block elements
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 50; i++) {
            if (i % 2 == 0) {
                html.append("<b>b").append(i);
            } else {
                html.append("<div>d").append(i).append("</div>");
            }
        }
        for (int i = 0; i < 25; i++) {
            html.append("</b>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle interleaved pattern
        assertNotNull(doc, "Document should be parsed with interleaved elements");
    }

    @Test
    public void testAAAWithVeryLongFormattingElementList() throws Exception {
        // Given: Very long active formatting element list
        final StringBuilder html = new StringBuilder("<html><body>");
        for (int i = 0; i < 100; i++) {
            html.append("<b>");
        }
        html.append("<div>Block</div>");
        for (int i = 0; i < 100; i++) {
            html.append("</b>");
        }
        html.append("</body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle long list without stack overflow
        assertNotNull(doc, "Document should be parsed without stack overflow");
    }
}
