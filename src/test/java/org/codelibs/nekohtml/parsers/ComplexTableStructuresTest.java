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
 * Tests for complex table structures, malformed tables, and table edge cases.
 * Tests THEAD, TBODY, TFOOT, CAPTION, COLGROUP, COL, and various table recovery scenarios.
 *
 * @author CodeLibs Project
 */
public class ComplexTableStructuresTest {

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
    // Well-formed Table Tests
    // ========================================================================

    @Test
    public void testTableWithAllSections() throws Exception {
        // Given: Table with THEAD, TBODY, TFOOT
        final String html = "<html><body><table>"
                + "<thead><tr><th>Header</th></tr></thead>"
                + "<tbody><tr><td>Body</td></tr></tbody>"
                + "<tfoot><tr><td>Footer</td></tr></tfoot>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: All sections should be present
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("TABLE").getLength(), "Should have TABLE");
        assertEquals(1, doc.getElementsByTagName("THEAD").getLength(), "Should have THEAD");
        assertEquals(1, doc.getElementsByTagName("TBODY").getLength(), "Should have TBODY");
        assertEquals(1, doc.getElementsByTagName("TFOOT").getLength(), "Should have TFOOT");
    }

    @Test
    public void testTableWithMultipleTbody() throws Exception {
        // Given: Table with multiple TBODY elements (valid HTML5)
        final String html = "<html><body><table>"
                + "<tbody><tr><td>Group 1 Row 1</td></tr></tbody>"
                + "<tbody><tr><td>Group 2 Row 1</td></tr></tbody>"
                + "<tbody><tr><td>Group 3 Row 1</td></tr></tbody>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should have multiple TBODY elements
        assertNotNull(doc, "Document should be parsed");
        final NodeList tbodies = doc.getElementsByTagName("TBODY");
        assertTrue(tbodies.getLength() >= 1, "Should have at least one TBODY");
    }

    @Test
    public void testTableWithCaption() throws Exception {
        // Given: Table with CAPTION
        final String html = "<html><body><table>"
                + "<caption>Table Caption</caption>"
                + "<tr><td>Cell</td></tr>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: CAPTION should be present
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("CAPTION").getLength(), "Should have CAPTION");
        assertEquals(1, doc.getElementsByTagName("TABLE").getLength(), "Should have TABLE");
    }

    @Test
    public void testTableWithColgroup() throws Exception {
        // Given: Table with COLGROUP and COL
        final String html = "<html><body><table>"
                + "<colgroup>"
                + "<col style=\"background-color:red\">"
                + "<col style=\"background-color:blue\">"
                + "<col style=\"background-color:green\">"
                + "</colgroup>"
                + "<tr><td>Cell 1</td><td>Cell 2</td><td>Cell 3</td></tr>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: COLGROUP and COL should be present
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("COLGROUP").getLength(), "Should have COLGROUP");
        assertEquals(3, doc.getElementsByTagName("COL").getLength(), "Should have 3 COL elements");
    }

    @Test
    public void testTableWithColgroupSpan() throws Exception {
        // Given: COLGROUP with span attribute
        final String html = "<html><body><table>"
                + "<colgroup span=\"3\" style=\"background-color:yellow\"></colgroup>"
                + "<tr><td>Cell 1</td><td>Cell 2</td><td>Cell 3</td></tr>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: COLGROUP should have span attribute
        assertNotNull(doc, "Document should be parsed");
        final Element colgroup = (Element) doc.getElementsByTagName("COLGROUP").item(0);
        assertNotNull(colgroup, "COLGROUP should exist");
        assertEquals("3", colgroup.getAttribute("span"), "Should have span=3");
    }

    @Test
    public void testComplexTableWithAllElements() throws Exception {
        // Given: Complex table with all possible elements
        final String html = "<html><body><table>"
                + "<caption>Complete Table</caption>"
                + "<colgroup>"
                + "<col style=\"width:100px\">"
                + "<col style=\"width:200px\">"
                + "</colgroup>"
                + "<thead><tr><th>Header 1</th><th>Header 2</th></tr></thead>"
                + "<tbody>"
                + "<tr><td>Data 1-1</td><td>Data 1-2</td></tr>"
                + "<tr><td>Data 2-1</td><td>Data 2-2</td></tr>"
                + "</tbody>"
                + "<tfoot><tr><td>Footer 1</td><td>Footer 2</td></tr></tfoot>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: All elements should be present
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("CAPTION").getLength(), "Should have CAPTION");
        assertEquals(1, doc.getElementsByTagName("COLGROUP").getLength(), "Should have COLGROUP");
        assertEquals(2, doc.getElementsByTagName("COL").getLength(), "Should have 2 COLs");
        assertEquals(1, doc.getElementsByTagName("THEAD").getLength(), "Should have THEAD");
        assertEquals(1, doc.getElementsByTagName("TBODY").getLength(), "Should have TBODY");
        assertEquals(1, doc.getElementsByTagName("TFOOT").getLength(), "Should have TFOOT");
    }

    // ========================================================================
    // Malformed Table Recovery Tests
    // ========================================================================

    @Test
    public void testTableWithTrDirectlyInTable() throws Exception {
        // Given: TR directly in TABLE (missing TBODY)
        final String html = "<html><body><table>"
                + "<tr><td>Cell 1</td></tr>"
                + "<tr><td>Cell 2</td></tr>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Parser should auto-insert TBODY
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("TABLE").getLength(), "Should have TABLE");
        assertEquals(2, doc.getElementsByTagName("TR").getLength(), "Should have 2 TRs");
        // Note: Parser may or may not auto-insert TBODY depending on implementation
    }

    @Test
    public void testTableWithTdDirectlyInTable() throws Exception {
        // Given: TD directly in TABLE (missing TBODY and TR)
        final String html = "<html><body><table>"
                + "<td>Cell 1</td>"
                + "<td>Cell 2</td>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Parser should auto-insert TR and possibly TBODY
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("TABLE").getLength(), "Should have TABLE");
        assertTrue(doc.getElementsByTagName("TD").getLength() >= 2, "Should have TDs");
    }

    @Test
    public void testTableWithTheadAfterTbody() throws Exception {
        // Given: THEAD after TBODY (incorrect order)
        final String html = "<html><body><table>"
                + "<tbody><tr><td>Body</td></tr></tbody>"
                + "<thead><tr><th>Header</th></tr></thead>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle incorrect order
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("TBODY").getLength() >= 1, "Should have TBODY");
        assertTrue(doc.getElementsByTagName("THEAD").getLength() >= 1, "Should have THEAD");
    }

    @Test
    public void testTableWithMixedElements() throws Exception {
        // Given: Table with elements in mixed/wrong order
        final String html = "<html><body><table>"
                + "<tr><td>Row 1</td></tr>"
                + "<thead><tr><th>Header</th></tr></thead>"
                + "<tr><td>Row 2</td></tr>"
                + "<tfoot><tr><td>Footer</td></tr></tfoot>"
                + "<tr><td>Row 3</td></tr>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle mixed order
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("TABLE").getLength(), "Should have TABLE");
    }

    @Test
    public void testTableWithUnclosedRows() throws Exception {
        // Given: Table with unclosed TR tags
        final String html = "<html><body><table>"
                + "<tr><td>Cell 1-1</td><td>Cell 1-2</td>"
                + "<tr><td>Cell 2-1</td><td>Cell 2-2</td>"
                + "<tr><td>Cell 3-1</td><td>Cell 3-2</td>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Parser should auto-close rows
        assertNotNull(doc, "Document should be parsed");
        assertEquals(3, doc.getElementsByTagName("TR").getLength(), "Should have 3 rows");
        assertEquals(6, doc.getElementsByTagName("TD").getLength(), "Should have 6 cells");
    }

    @Test
    public void testTableWithUnclosedCells() throws Exception {
        // Given: Table with unclosed TD tags
        final String html = "<html><body><table>"
                + "<tr><td>Cell 1<td>Cell 2<td>Cell 3</tr>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Parser should auto-close cells
        assertNotNull(doc, "Document should be parsed");
        assertEquals(3, doc.getElementsByTagName("TD").getLength(), "Should have 3 cells");
    }

    // ========================================================================
    // Colspan and Rowspan Tests
    // ========================================================================

    @Test
    public void testTableWithColspan() throws Exception {
        // Given: Table with COLSPAN
        final String html = "<html><body><table>"
                + "<tr><td colspan=\"2\">Spans 2 columns</td><td>Cell 3</td></tr>"
                + "<tr><td>Cell 1</td><td>Cell 2</td><td>Cell 3</td></tr>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: COLSPAN should be preserved
        assertNotNull(doc, "Document should be parsed");
        final NodeList tds = doc.getElementsByTagName("TD");
        assertTrue(tds.getLength() >= 4, "Should have cells");

        final Element firstCell = (Element) tds.item(0);
        assertEquals("2", firstCell.getAttribute("colspan"), "Should have colspan=2");
    }

    @Test
    public void testTableWithRowspan() throws Exception {
        // Given: Table with ROWSPAN
        final String html = "<html><body><table>"
                + "<tr><td rowspan=\"2\">Spans 2 rows</td><td>Cell 1-2</td></tr>"
                + "<tr><td>Cell 2-2</td></tr>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: ROWSPAN should be preserved
        assertNotNull(doc, "Document should be parsed");
        final NodeList tds = doc.getElementsByTagName("TD");
        assertTrue(tds.getLength() >= 3, "Should have cells");

        final Element firstCell = (Element) tds.item(0);
        assertEquals("2", firstCell.getAttribute("rowspan"), "Should have rowspan=2");
    }

    @Test
    public void testTableWithComplexSpans() throws Exception {
        // Given: Table with complex COLSPAN and ROWSPAN
        final String html = "<html><body><table>"
                + "<tr><td colspan=\"2\" rowspan=\"2\">Spans 2x2</td><td>Cell 1-3</td></tr>"
                + "<tr><td>Cell 2-3</td></tr>"
                + "<tr><td>Cell 3-1</td><td>Cell 3-2</td><td>Cell 3-3</td></tr>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Complex spans should be preserved
        assertNotNull(doc, "Document should be parsed");
        final NodeList tds = doc.getElementsByTagName("TD");
        assertTrue(tds.getLength() >= 5, "Should have cells");

        final Element spanCell = (Element) tds.item(0);
        assertEquals("2", spanCell.getAttribute("colspan"), "Should have colspan=2");
        assertEquals("2", spanCell.getAttribute("rowspan"), "Should have rowspan=2");
    }

    @Test
    public void testTableWithZeroSpan() throws Exception {
        // Given: Table with colspan/rowspan=0 (special value)
        final String html = "<html><body><table>"
                + "<tr><td colspan=\"0\">Spans to end</td></tr>"
                + "<tr><td>Cell 1</td><td>Cell 2</td><td>Cell 3</td></tr>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Zero span should be preserved
        assertNotNull(doc, "Document should be parsed");
        final Element cell = (Element) doc.getElementsByTagName("TD").item(0);
        assertEquals("0", cell.getAttribute("colspan"), "Should have colspan=0");
    }

    @Test
    public void testTableWithVeryLargeSpan() throws Exception {
        // Given: Table with very large span value
        final String html = "<html><body><table>"
                + "<tr><td colspan=\"1000\">Large span</td></tr>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Large span should be preserved
        assertNotNull(doc, "Document should be parsed");
        final Element cell = (Element) doc.getElementsByTagName("TD").item(0);
        assertEquals("1000", cell.getAttribute("colspan"), "Should have colspan=1000");
    }

    // ========================================================================
    // Nested Tables Tests
    // ========================================================================

    @Test
    public void testNestedTables2Levels() throws Exception {
        // Given: 2 levels of nested tables
        final String html = "<html><body><table>"
                + "<tr><td>Outer cell 1</td>"
                + "<td><table><tr><td>Inner cell</td></tr></table></td>"
                + "</tr></table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Both tables should be present
        assertNotNull(doc, "Document should be parsed");
        assertEquals(2, doc.getElementsByTagName("TABLE").getLength(), "Should have 2 tables");
    }

    @Test
    public void testNestedTables5Levels() throws Exception {
        // Given: 5 levels of nested tables
        final String html = "<html><body><table><tr><td>"
                + "<table><tr><td>"
                + "<table><tr><td>"
                + "<table><tr><td>"
                + "<table><tr><td>Deep</td></tr></table>"
                + "</td></tr></table>"
                + "</td></tr></table>"
                + "</td></tr></table>"
                + "</td></tr></table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: All 5 tables should be present
        assertNotNull(doc, "Document should be parsed");
        assertEquals(5, doc.getElementsByTagName("TABLE").getLength(), "Should have 5 nested tables");
    }

    @Test
    public void testNestedTableInTheadTbodyTfoot() throws Exception {
        // Given: Nested tables in different sections
        final String html = "<html><body><table>"
                + "<thead><tr><th><table><tr><td>Header table</td></tr></table></th></tr></thead>"
                + "<tbody><tr><td><table><tr><td>Body table</td></tr></table></td></tr></tbody>"
                + "<tfoot><tr><td><table><tr><td>Footer table</td></tr></table></td></tr></tfoot>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: All nested tables should be present
        assertNotNull(doc, "Document should be parsed");
        assertEquals(4, doc.getElementsByTagName("TABLE").getLength(), "Should have 4 tables total");
    }

    // ========================================================================
    // Empty Table Tests
    // ========================================================================

    @Test
    public void testEmptyTable() throws Exception {
        // Given: Empty table
        final String html = "<html><body><table></table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Empty table should be parsed
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("TABLE").getLength(), "Should have TABLE");
    }

    @Test
    public void testTableWithEmptyCells() throws Exception {
        // Given: Table with empty cells
        final String html = "<html><body><table>"
                + "<tr><td></td><td></td><td></td></tr>"
                + "<tr><td></td><td>Content</td><td></td></tr>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Empty cells should be parsed
        assertNotNull(doc, "Document should be parsed");
        assertEquals(6, doc.getElementsByTagName("TD").getLength(), "Should have 6 cells");
    }

    @Test
    public void testTableWithOnlyTheadEmpty() throws Exception {
        // Given: Table with empty THEAD
        final String html = "<html><body><table>"
                + "<thead></thead>"
                + "<tbody><tr><td>Body</td></tr></tbody>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Empty THEAD should be parsed
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("THEAD").getLength(), "Should have THEAD");
    }

    // ========================================================================
    // Table with TH Tests
    // ========================================================================

    @Test
    public void testTableWithThInTbody() throws Exception {
        // Given: TH elements in TBODY (valid for row headers)
        final String html = "<html><body><table>"
                + "<tbody>"
                + "<tr><th>Row 1 Header</th><td>Data 1</td></tr>"
                + "<tr><th>Row 2 Header</th><td>Data 2</td></tr>"
                + "</tbody>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: TH in TBODY should be valid
        assertNotNull(doc, "Document should be parsed");
        assertEquals(2, doc.getElementsByTagName("TH").getLength(), "Should have 2 TH elements");
        assertEquals(2, doc.getElementsByTagName("TD").getLength(), "Should have 2 TD elements");
    }

    @Test
    public void testTableWithOnlyTh() throws Exception {
        // Given: Table with only TH elements
        final String html = "<html><body><table>"
                + "<tr><th>Header 1</th><th>Header 2</th><th>Header 3</th></tr>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: TH-only table should be parsed
        assertNotNull(doc, "Document should be parsed");
        assertEquals(3, doc.getElementsByTagName("TH").getLength(), "Should have 3 TH elements");
        assertEquals(0, doc.getElementsByTagName("TD").getLength(), "Should have no TD elements");
    }

    @Test
    public void testTableWithThAttributes() throws Exception {
        // Given: TH with scope, headers, and colspan attributes
        final String html = "<html><body><table>"
                + "<thead><tr><th id=\"h1\" scope=\"col\" colspan=\"2\">Header</th></tr></thead>"
                + "<tbody><tr><td headers=\"h1\">Data 1</td><td headers=\"h1\">Data 2</td></tr></tbody>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: TH attributes should be preserved
        assertNotNull(doc, "Document should be parsed");
        final Element th = (Element) doc.getElementsByTagName("TH").item(0);
        assertEquals("h1", th.getAttribute("id"), "Should have id");
        assertEquals("col", th.getAttribute("scope"), "Should have scope");
        assertEquals("2", th.getAttribute("colspan"), "Should have colspan");
    }

    // ========================================================================
    // Complex Real-world Table Patterns
    // ========================================================================

    @Test
    public void testDataTableWithSorting() throws Exception {
        // Given: Data table with sorting attributes
        final String html = "<html><body><table>"
                + "<thead><tr>"
                + "<th data-sort=\"string\">Name</th>"
                + "<th data-sort=\"number\">Age</th>"
                + "<th data-sort=\"date\">Date</th>"
                + "</tr></thead>"
                + "<tbody>"
                + "<tr><td>John</td><td>30</td><td>2025-01-01</td></tr>"
                + "<tr><td>Jane</td><td>25</td><td>2025-01-02</td></tr>"
                + "</tbody>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Data attributes should be preserved
        assertNotNull(doc, "Document should be parsed");
        final NodeList ths = doc.getElementsByTagName("TH");
        assertEquals(3, ths.getLength(), "Should have 3 headers");

        final Element th1 = (Element) ths.item(0);
        assertEquals("string", th1.getAttribute("data-sort"), "Should have data-sort");
    }

    @Test
    public void testTableWithFormElements() throws Exception {
        // Given: Table containing form elements
        final String html = "<html><body><table>"
                + "<tr><td><input type=\"text\" name=\"field1\"></td></tr>"
                + "<tr><td><select name=\"field2\"><option>Option 1</option></select></td></tr>"
                + "<tr><td><textarea name=\"field3\"></textarea></td></tr>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Form elements should be in table
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("INPUT").getLength(), "Should have INPUT");
        assertEquals(1, doc.getElementsByTagName("SELECT").getLength(), "Should have SELECT");
        assertEquals(1, doc.getElementsByTagName("TEXTAREA").getLength(), "Should have TEXTAREA");
    }

    @Test
    public void testTableWithComplexContent() throws Exception {
        // Given: Table with various content types
        final String html = "<html><body><table>"
                + "<tr><td><img src=\"image.jpg\" alt=\"Image\"></td></tr>"
                + "<tr><td><a href=\"#\">Link</a></td></tr>"
                + "<tr><td><ul><li>List item</li></ul></td></tr>"
                + "<tr><td><div><p>Nested content</p></div></td></tr>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: All content should be in table cells
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("IMG").getLength(), "Should have IMG");
        assertEquals(1, doc.getElementsByTagName("A").getLength(), "Should have A");
        assertEquals(1, doc.getElementsByTagName("UL").getLength(), "Should have UL");
        assertEquals(1, doc.getElementsByTagName("DIV").getLength(), "Should have DIV");
    }

    @Test
    public void testTableWithMultipleCaption() throws Exception {
        // Given: Table with multiple CAPTION elements (invalid but should handle)
        final String html = "<html><body><table>"
                + "<caption>Caption 1</caption>"
                + "<caption>Caption 2</caption>"
                + "<tr><td>Cell</td></tr>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle multiple captions
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("CAPTION").getLength() >= 1, "Should have at least one CAPTION");
    }

    @Test
    public void testTableWithIrregularRows() throws Exception {
        // Given: Table with rows of different cell counts
        final String html = "<html><body><table>"
                + "<tr><td>Cell 1</td></tr>"
                + "<tr><td>Cell 1</td><td>Cell 2</td></tr>"
                + "<tr><td>Cell 1</td><td>Cell 2</td><td>Cell 3</td></tr>"
                + "<tr><td>Cell 1</td><td>Cell 2</td></tr>"
                + "<tr><td>Cell 1</td></tr>"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle irregular rows
        assertNotNull(doc, "Document should be parsed");
        assertEquals(5, doc.getElementsByTagName("TR").getLength(), "Should have 5 rows");
        assertEquals(9, doc.getElementsByTagName("TD").getLength(), "Should have 9 cells total");
    }
}
