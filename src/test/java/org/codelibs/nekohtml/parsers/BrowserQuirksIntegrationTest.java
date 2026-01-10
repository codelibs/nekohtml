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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Integration tests for browser quirks mode compatibility and implied element handling.
 * Tests common HTML patterns that browsers handle gracefully but are technically invalid.
 *
 * @author CodeLibs Project
 */
public class BrowserQuirksIntegrationTest {

    private DOMParser parser;

    @BeforeEach
    public void setUp() throws Exception {
        parser = new DOMParser();
    }

    private Document parseHTML(final String html) throws Exception {
        parser.parse(new InputSource(new StringReader(html)));
        return parser.getDocument();
    }

    // =========================================================================
    // Implied Element Tests
    // =========================================================================

    @Test
    public void testImpliedHtmlElement() throws Exception {
        // Given: Document without explicit HTML tag
        final String html = "<head><title>Test</title></head><body><p>Content</p></body>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: HTML element should exist
        assertNotNull(doc.getDocumentElement(), "Document should have root element");
    }

    @Test
    public void testImpliedHeadElement() throws Exception {
        // Given: Document without explicit HEAD tag but with HEAD content
        final String html = "<html><title>Test</title><body><p>Content</p></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse successfully
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("TITLE").getLength(), "TITLE should exist");
    }

    @Test
    public void testImpliedBodyElement() throws Exception {
        // Given: Document without explicit BODY tag
        final String html = "<html><head><title>Test</title></head><p>Content</p></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse successfully
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("P").getLength(), "P should exist");
    }

    @Test
    public void testImpliedTbodyInTable() throws Exception {
        // Given: Table without explicit TBODY
        final String html = "<html><body><table><tr><td>Cell 1</td><td>Cell 2</td></tr></table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Table should parse successfully
        assertEquals(1, doc.getElementsByTagName("TABLE").getLength(), "TABLE should exist");
        assertEquals(1, doc.getElementsByTagName("TR").getLength(), "TR should exist");
        assertEquals(2, doc.getElementsByTagName("TD").getLength(), "TD elements should exist");
    }

    @Test
    public void testImpliedColgroup() throws Exception {
        // Given: Table with COL but no COLGROUP
        final String html = "<html><body><table><col width=\"100\"><tr><td>Cell</td></tr></table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse successfully
        assertEquals(1, doc.getElementsByTagName("TABLE").getLength(), "TABLE should exist");
        assertEquals(1, doc.getElementsByTagName("COL").getLength(), "COL should exist");
    }

    // =========================================================================
    // Table Quirks Tests
    // =========================================================================

    @Test
    public void testTableWithMissingCloseTags() throws Exception {
        // Given: Table with missing close tags (common in legacy HTML)
        final String html = "<html><body><table>" + "<tr><td>Row 1, Cell 1<td>Row 1, Cell 2" + "<tr><td>Row 2, Cell 1<td>Row 2, Cell 2"
                + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Table should be properly structured
        assertEquals(1, doc.getElementsByTagName("TABLE").getLength(), "TABLE should exist");
        assertEquals(2, doc.getElementsByTagName("TR").getLength(), "Should have 2 rows");
        assertEquals(4, doc.getElementsByTagName("TD").getLength(), "Should have 4 cells");
    }

    @Test
    public void testNestedTables() throws Exception {
        // Given: Nested tables
        final String html = "<html><body>" + "<table><tr><td>" + "<table><tr><td>Inner cell</td></tr></table>" + "</td></tr></table>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Both tables should exist
        assertEquals(2, doc.getElementsByTagName("TABLE").getLength(), "Should have 2 tables");
    }

    @Test
    public void testTableWithCaptionAfterRows() throws Exception {
        // Given: Table with CAPTION after TR (invalid but common)
        final String html = "<html><body><table>" + "<tr><td>Cell</td></tr>" + "<caption>Table Caption</caption>" + "</table></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse without error
        assertEquals(1, doc.getElementsByTagName("TABLE").getLength(), "TABLE should exist");
        assertEquals(1, doc.getElementsByTagName("CAPTION").getLength(), "CAPTION should exist");
    }

    // =========================================================================
    // Form Element Quirks
    // =========================================================================

    @Test
    public void testFormWithOrphanedInputs() throws Exception {
        // Given: Form with inputs outside form tag
        final String html = "<html><body>" + "<form action=\"#\">" + "<input type=\"text\" name=\"inside\">" + "</form>"
                + "<input type=\"text\" name=\"outside\">" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Both inputs should exist
        assertEquals(2, doc.getElementsByTagName("INPUT").getLength(), "Should have 2 inputs");
        assertEquals(1, doc.getElementsByTagName("FORM").getLength(), "FORM should exist");
    }

    @Test
    public void testNestedForms() throws Exception {
        // Given: Nested forms (invalid HTML but may appear)
        final String html = "<html><body>" + "<form action=\"outer\">" + "<form action=\"inner\">" + "<input type=\"text\">" + "</form>"
                + "</form>" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse without throwing
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("INPUT").getLength() >= 1, "Input should exist");
    }

    @Test
    public void testFormWithSelectWithoutClosingOption() throws Exception {
        // Given: SELECT with unclosed OPTION tags
        final String html = "<html><body>" + "<select>" + "<option>Option 1" + "<option>Option 2" + "<option>Option 3" + "</select>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: All options should exist
        assertEquals(1, doc.getElementsByTagName("SELECT").getLength(), "SELECT should exist");
        assertEquals(3, doc.getElementsByTagName("OPTION").getLength(), "Should have 3 options");
    }

    // =========================================================================
    // List Element Quirks
    // =========================================================================

    @Test
    public void testUnclosedListItems() throws Exception {
        // Given: List with unclosed LI tags
        final String html = "<html><body>" + "<ul>" + "<li>Item 1" + "<li>Item 2" + "<li>Item 3" + "</ul>" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: All list items should exist
        assertEquals(1, doc.getElementsByTagName("UL").getLength(), "UL should exist");
        assertEquals(3, doc.getElementsByTagName("LI").getLength(), "Should have 3 list items");
    }

    @Test
    public void testNestedListsWithUnclosedItems() throws Exception {
        // Given: Nested lists with unclosed LI
        final String html = "<html><body>" + "<ul>" + "<li>Item 1" + "<ul>" + "<li>Nested 1" + "<li>Nested 2" + "</ul>" + "<li>Item 2"
                + "</ul>" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse correctly
        assertEquals(2, doc.getElementsByTagName("UL").getLength(), "Should have 2 ULs");
        assertEquals(4, doc.getElementsByTagName("LI").getLength(), "Should have 4 LIs");
    }

    @Test
    public void testDefinitionListQuirks() throws Exception {
        // Given: DL with unclosed DT/DD
        final String html = "<html><body>" + "<dl>" + "<dt>Term 1" + "<dd>Definition 1" + "<dt>Term 2" + "<dd>Definition 2" + "</dl>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: All elements should exist
        assertEquals(1, doc.getElementsByTagName("DL").getLength(), "DL should exist");
        assertEquals(2, doc.getElementsByTagName("DT").getLength(), "Should have 2 DTs");
        assertEquals(2, doc.getElementsByTagName("DD").getLength(), "Should have 2 DDs");
    }

    // =========================================================================
    // Paragraph and Block Element Quirks
    // =========================================================================

    @Test
    public void testParagraphAutoClose() throws Exception {
        // Given: Paragraphs auto-closing when block element starts
        final String html = "<html><body>" + "<p>Para 1" + "<p>Para 2" + "<p>Para 3" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: All paragraphs should exist
        assertEquals(3, doc.getElementsByTagName("P").getLength(), "Should have 3 paragraphs");
    }

    @Test
    public void testDivInsideParagraph() throws Exception {
        // Given: DIV inside P (should auto-close P)
        final String html = "<html><body>" + "<p>Before" + "<div>Block content</div>" + "After" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should have both elements
        assertTrue(doc.getElementsByTagName("P").getLength() >= 1, "P should exist");
        assertEquals(1, doc.getElementsByTagName("DIV").getLength(), "DIV should exist");
    }

    @Test
    public void testHeadingsAutoClose() throws Exception {
        // Given: Unclosed headings
        final String html = "<html><body>" + "<h1>Heading 1" + "<h2>Heading 2" + "<h3>Heading 3" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: All headings should exist
        assertEquals(1, doc.getElementsByTagName("H1").getLength(), "H1 should exist");
        assertEquals(1, doc.getElementsByTagName("H2").getLength(), "H2 should exist");
        assertEquals(1, doc.getElementsByTagName("H3").getLength(), "H3 should exist");
    }

    // =========================================================================
    // Inline Element in Block Context
    // =========================================================================

    @Test
    public void testInlineElementsInBody() throws Exception {
        // Given: Inline elements directly in body
        final String html = "<html><body>Text <b>bold</b> more text <i>italic</i></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Inline elements should be preserved
        assertEquals(1, doc.getElementsByTagName("B").getLength(), "B should exist");
        assertEquals(1, doc.getElementsByTagName("I").getLength(), "I should exist");
    }

    @Test
    public void testBlockInsideInline() throws Exception {
        // Given: Block element inside inline (invalid)
        final String html = "<html><body><span><div>Block in span</div></span></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse (may restructure)
        assertTrue(doc.getElementsByTagName("SPAN").getLength() >= 1, "SPAN should exist");
        assertEquals(1, doc.getElementsByTagName("DIV").getLength(), "DIV should exist");
    }

    // =========================================================================
    // Script and Style in Wrong Places
    // =========================================================================

    @Test
    public void testScriptInBody() throws Exception {
        // Given: Script in body (valid but tested for quirks)
        final String html = "<html><body>" + "<p>Before script</p>" + "<script>var x = 1;</script>" + "<p>After script</p>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: All elements should exist
        assertEquals(2, doc.getElementsByTagName("P").getLength(), "Should have 2 paragraphs");
        assertEquals(1, doc.getElementsByTagName("SCRIPT").getLength(), "SCRIPT should exist");
    }

    @Test
    public void testStyleInBody() throws Exception {
        // Given: Style in body (quirks mode)
        final String html = "<html><body>" + "<style>p { color: red; }</style>" + "<p>Styled paragraph</p>" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse
        assertEquals(1, doc.getElementsByTagName("STYLE").getLength(), "STYLE should exist");
        assertEquals(1, doc.getElementsByTagName("P").getLength(), "P should exist");
    }

    // =========================================================================
    // Attribute Quirks
    // =========================================================================

    @Test
    public void testBooleanAttributes() throws Exception {
        // Given: Boolean attributes without values
        final String html = "<html><body>" + "<input type=\"checkbox\" checked disabled readonly>" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Attributes should exist
        final NodeList inputs = doc.getElementsByTagName("INPUT");
        assertEquals(1, inputs.getLength(), "INPUT should exist");

        final Element input = (Element) inputs.item(0);
        assertTrue(input.hasAttribute("checked") || input.hasAttribute("CHECKED"), "checked attribute should exist");
    }

    @Test
    public void testUnquotedAttributeValues() throws Exception {
        // Given: Unquoted attribute values
        final String html = "<html><body>" + "<div id=myid class=myclass data-value=123>" + "Content" + "</div>" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse attributes
        final NodeList divs = doc.getElementsByTagName("DIV");
        assertEquals(1, divs.getLength(), "DIV should exist");

        final Element div = (Element) divs.item(0);
        assertEquals("myid", div.getAttribute("id"), "id should be parsed");
        assertEquals("myclass", div.getAttribute("class"), "class should be parsed");
    }

    @Test
    public void testMixedQuoteStyles() throws Exception {
        // Given: Mixed single and double quotes
        final String html = "<html><body>" + "<a href=\"http://example.com\" title='Example Site'>Link</a>" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse both
        final NodeList links = doc.getElementsByTagName("A");
        assertEquals(1, links.getLength(), "A should exist");

        final Element a = (Element) links.item(0);
        assertEquals("http://example.com", a.getAttribute("href"), "href should be parsed");
        assertEquals("Example Site", a.getAttribute("title"), "title should be parsed");
    }

    // =========================================================================
    // DOCTYPE Variations
    // =========================================================================

    @Test
    public void testHTML5Doctype() throws Exception {
        // Given: HTML5 DOCTYPE
        final String html = "<!DOCTYPE html><html><body><p>HTML5</p></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("P").getLength(), "P should exist");
    }

    @Test
    public void testHTML4StrictDoctype() throws Exception {
        // Given: HTML 4.01 Strict DOCTYPE
        final String html = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" "
                + "\"http://www.w3.org/TR/html4/strict.dtd\">" + "<html><body><p>HTML 4.01</p></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse
        assertNotNull(doc, "Document should be parsed");
    }

    @Test
    public void testXHTMLDoctype() throws Exception {
        // Given: XHTML DOCTYPE
        final String html = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
                + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" + "<html><body><p>XHTML</p></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse
        assertNotNull(doc, "Document should be parsed");
    }

    @Test
    public void testMissingDoctype() throws Exception {
        // Given: No DOCTYPE (quirks mode trigger in browsers)
        final String html = "<html><body><p>No DOCTYPE</p></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse in quirks mode
        assertNotNull(doc, "Document should be parsed");
        assertEquals(1, doc.getElementsByTagName("P").getLength(), "P should exist");
    }

    // =========================================================================
    // Character Encoding Edge Cases
    // =========================================================================

    @Test
    public void testMetaCharsetHTML5() throws Exception {
        // Given: HTML5 charset meta
        final String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body>Test</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse
        assertEquals(1, doc.getElementsByTagName("META").getLength(), "META should exist");
    }

    @Test
    public void testMetaContentType() throws Exception {
        // Given: Legacy content-type meta
        final String html = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"></head><body>Test</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse
        assertEquals(1, doc.getElementsByTagName("META").getLength(), "META should exist");
    }

    // =========================================================================
    // Void Element Handling
    // =========================================================================

    @Test
    public void testVoidElementsWithClosingTags() throws Exception {
        // Given: Void elements with explicit closing tags (allowed in HTML)
        final String html = "<html><body>" + "<br></br>" + "<hr></hr>" + "<img src=\"test.png\"></img>" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Void elements should exist
        assertTrue(doc.getElementsByTagName("BR").getLength() >= 1, "BR should exist");
        assertTrue(doc.getElementsByTagName("HR").getLength() >= 1, "HR should exist");
        assertTrue(doc.getElementsByTagName("IMG").getLength() >= 1, "IMG should exist");
    }

    @Test
    public void testVoidElementsWithSlash() throws Exception {
        // Given: Void elements with trailing slash (XHTML style)
        final String html = "<html><body>" + "<br />" + "<hr />" + "<img src=\"test.png\" />" + "<input type=\"text\" />" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse correctly
        assertTrue(doc.getElementsByTagName("BR").getLength() >= 1, "BR should exist");
        assertTrue(doc.getElementsByTagName("IMG").getLength() >= 1, "IMG should exist");
    }

    // =========================================================================
    // Real-World HTML Patterns
    // =========================================================================

    @Test
    public void testTypicalWebPageStructure() throws Exception {
        // Given: Typical web page structure
        final String html = "<!DOCTYPE html>" + "<html lang=\"en\">" + "<head>" + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" + "<title>Test Page</title>"
                + "<link rel=\"stylesheet\" href=\"style.css\">" + "<script src=\"script.js\"></script>" + "</head>" + "<body>" + "<header>"
                + "<nav><ul><li><a href=\"#\">Home</a></li></ul></nav>" + "</header>" + "<main>" + "<article>"
                + "<h1>Article Title</h1>" + "<p>Article content.</p>" + "</article>" + "</main>" + "<footer>"
                + "<p>&copy; 2024</p>" + "</footer>" + "</body>" + "</html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: All major sections should exist
        assertEquals(1, doc.getElementsByTagName("HEADER").getLength(), "HEADER should exist");
        assertEquals(1, doc.getElementsByTagName("MAIN").getLength(), "MAIN should exist");
        assertEquals(1, doc.getElementsByTagName("FOOTER").getLength(), "FOOTER should exist");
        assertEquals(1, doc.getElementsByTagName("ARTICLE").getLength(), "ARTICLE should exist");
    }

    @Test
    public void testTypicalFormStructure() throws Exception {
        // Given: Typical form structure
        final String html = "<html><body>" + "<form action=\"/submit\" method=\"post\">" + "<fieldset>"
                + "<legend>User Information</legend>" + "<label for=\"name\">Name:</label>"
                + "<input type=\"text\" id=\"name\" name=\"name\">" + "<label for=\"email\">Email:</label>"
                + "<input type=\"email\" id=\"email\" name=\"email\">" + "</fieldset>" + "<button type=\"submit\">Submit</button>"
                + "</form>" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Form elements should exist
        assertEquals(1, doc.getElementsByTagName("FORM").getLength(), "FORM should exist");
        assertEquals(1, doc.getElementsByTagName("FIELDSET").getLength(), "FIELDSET should exist");
        assertEquals(1, doc.getElementsByTagName("LEGEND").getLength(), "LEGEND should exist");
        assertEquals(2, doc.getElementsByTagName("LABEL").getLength(), "Should have 2 LABELs");
        assertEquals(2, doc.getElementsByTagName("INPUT").getLength(), "Should have 2 INPUTs");
    }

} // class BrowserQuirksIntegrationTest
