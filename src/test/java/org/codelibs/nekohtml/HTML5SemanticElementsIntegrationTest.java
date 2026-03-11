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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Integration tests for HTML5 semantic elements (SEARCH, SLOT, HGROUP) in real-world scenarios.
 * These tests verify that recently added HTML5 elements work correctly in various contexts.
 *
 * @author CodeLibs Project
 */
public class HTML5SemanticElementsIntegrationTest {

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
    // SEARCH Element Tests
    // ========================================================================

    @Test
    public void testSearchElementInBody() throws Exception {
        // Given: HTML with SEARCH in BODY
        final String html = "<html><body><search><form><input type=\"search\"></form></search></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: SEARCH element should be present
        final NodeList searchElements = doc.getElementsByTagName("SEARCH");
        assertEquals(1, searchElements.getLength(), "Should have one SEARCH element");

        final Element search = (Element) searchElements.item(0);
        assertEquals("BODY", search.getParentNode().getNodeName(), "SEARCH should be in BODY");

        // And: Nested FORM should be present
        final NodeList forms = search.getElementsByTagName("FORM");
        assertEquals(1, forms.getLength(), "Should have FORM inside SEARCH");
    }

    @Test
    public void testSearchElementInHeader() throws Exception {
        // Given: HTML with SEARCH in HEADER
        final String html = "<html><body><header><search><input type=\"search\" placeholder=\"Search...\"></search></header></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: SEARCH should be in HEADER
        final NodeList headers = doc.getElementsByTagName("HEADER");
        assertEquals(1, headers.getLength(), "Should have HEADER element");

        final Element header = (Element) headers.item(0);
        final NodeList searchElements = header.getElementsByTagName("SEARCH");
        assertEquals(1, searchElements.getLength(), "HEADER should contain SEARCH");
    }

    @Test
    public void testSearchElementInAside() throws Exception {
        // Given: HTML with SEARCH in ASIDE (common pattern for sidebar search)
        final String html =
                "<html><body><main>Main content</main><aside><search><form><label>Filter:<input type=\"search\"></label></form></search></aside></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: SEARCH should be in ASIDE
        final NodeList asides = doc.getElementsByTagName("ASIDE");
        assertEquals(1, asides.getLength(), "Should have ASIDE element");

        final Element aside = (Element) asides.item(0);
        final NodeList searchElements = aside.getElementsByTagName("SEARCH");
        assertEquals(1, searchElements.getLength(), "ASIDE should contain SEARCH");
    }

    @Test
    public void testMultipleSearchElements() throws Exception {
        // Given: HTML with multiple SEARCH elements
        final String html =
                "<html><body>" + "<header><search><input type=\"search\" placeholder=\"Global search\"></search></header>"
                        + "<main><article><search><input type=\"search\" placeholder=\"Article search\"></search></article></main>"
                        + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should have two SEARCH elements
        final NodeList searchElements = doc.getElementsByTagName("SEARCH");
        assertEquals(2, searchElements.getLength(), "Should have two SEARCH elements");
    }

    @Test
    public void testSearchWithAutocompleteAndDatalist() throws Exception {
        // Given: HTML with SEARCH containing autocomplete and datalist
        final String html =
                "<html><body><search>" + "<input type=\"search\" list=\"suggestions\" autocomplete=\"on\">"
                        + "<datalist id=\"suggestions\">" + "<option value=\"Apple\">" + "<option value=\"Banana\">"
                        + "<option value=\"Cherry\">" + "</datalist>" + "</search></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: SEARCH should contain INPUT and DATALIST
        final NodeList searchElements = doc.getElementsByTagName("SEARCH");
        assertEquals(1, searchElements.getLength(), "Should have SEARCH element");

        final Element search = (Element) searchElements.item(0);
        final NodeList inputs = search.getElementsByTagName("INPUT");
        assertEquals(1, inputs.getLength(), "SEARCH should contain INPUT");

        final NodeList datalists = search.getElementsByTagName("DATALIST");
        assertEquals(1, datalists.getLength(), "SEARCH should contain DATALIST");

        final Element datalist = (Element) datalists.item(0);
        final NodeList options = datalist.getElementsByTagName("OPTION");
        assertEquals(3, options.getLength(), "DATALIST should have 3 options");
    }

    @Test
    public void testSearchClosingWithAdjacentBlockElements() throws Exception {
        // Given: HTML with SEARCH followed by other block elements
        final String html =
                "<html><body>" + "<search><input type=\"search\"></search>" + "<nav><ul><li>Link 1</li></ul></nav>"
                        + "<article>Content</article>" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: All elements should be siblings in BODY
        final NodeList bodyChildren = doc.getElementsByTagName("BODY").item(0).getChildNodes();
        boolean hasSearch = false, hasNav = false, hasArticle = false;

        for (int i = 0; i < bodyChildren.getLength(); i++) {
            final String nodeName = bodyChildren.item(i).getNodeName();
            if ("SEARCH".equals(nodeName))
                hasSearch = true;
            if ("NAV".equals(nodeName))
                hasNav = true;
            if ("ARTICLE".equals(nodeName))
                hasArticle = true;
        }

        assertTrue(hasSearch, "BODY should contain SEARCH");
        assertTrue(hasNav, "BODY should contain NAV");
        assertTrue(hasArticle, "BODY should contain ARTICLE");
    }

    // ========================================================================
    // SLOT Element Tests (Web Components)
    // ========================================================================

    @Test
    public void testSlotElementBasic() throws Exception {
        // Given: HTML with SLOT element
        final String html = "<html><body><template><slot></slot></template></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: SLOT element should be present
        final NodeList slotElements = doc.getElementsByTagName("SLOT");
        assertEquals(1, slotElements.getLength(), "Should have one SLOT element");
    }

    @Test
    public void testSlotElementWithName() throws Exception {
        // Given: HTML with named SLOT
        final String html = "<html><body><template><slot name=\"header\"></slot><slot name=\"footer\"></slot></template></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should have two SLOT elements with name attributes
        final NodeList slotElements = doc.getElementsByTagName("SLOT");
        assertEquals(2, slotElements.getLength(), "Should have two SLOT elements");

        final Element slot1 = (Element) slotElements.item(0);
        assertEquals("header", slot1.getAttribute("name"), "First SLOT should have name='header'");

        final Element slot2 = (Element) slotElements.item(1);
        assertEquals("footer", slot2.getAttribute("name"), "Second SLOT should have name='footer'");
    }

    @Test
    public void testSlotWithFallbackContent() throws Exception {
        // Given: HTML with SLOT containing fallback content
        final String html = "<html><body><template><slot name=\"title\"><h2>Default Title</h2></slot></template></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: SLOT should contain H2 as fallback
        final NodeList slotElements = doc.getElementsByTagName("SLOT");
        assertEquals(1, slotElements.getLength(), "Should have SLOT element");

        final Element slot = (Element) slotElements.item(0);
        final NodeList h2Elements = slot.getElementsByTagName("H2");
        assertEquals(1, h2Elements.getLength(), "SLOT should contain H2 fallback");
    }

    @Test
    public void testMultipleSlotsInTemplate() throws Exception {
        // Given: HTML with multiple SLOTs in TEMPLATE
        final String html =
                "<html><body><template id=\"my-template\">" + "<header><slot name=\"header\">Default Header</slot></header>"
                        + "<main><slot>Default Content</slot></main>" + "<footer><slot name=\"footer\">Default Footer</slot></footer>"
                        + "</template></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should have three SLOT elements
        final NodeList slotElements = doc.getElementsByTagName("SLOT");
        assertEquals(3, slotElements.getLength(), "Should have three SLOT elements");

        // And: TEMPLATE should contain HEADER, MAIN, FOOTER
        final NodeList templates = doc.getElementsByTagName("TEMPLATE");
        assertEquals(1, templates.getLength(), "Should have TEMPLATE element");

        final Element template = (Element) templates.item(0);
        assertNotNull(template.getElementsByTagName("HEADER").item(0), "TEMPLATE should contain HEADER");
        assertNotNull(template.getElementsByTagName("MAIN").item(0), "TEMPLATE should contain MAIN");
        assertNotNull(template.getElementsByTagName("FOOTER").item(0), "TEMPLATE should contain FOOTER");
    }

    @Test
    public void testNestedSlotElements() throws Exception {
        // Given: HTML with nested SLOTs (edge case)
        final String html =
                "<html><body><template><div><slot name=\"outer\"><div><slot name=\"inner\">Fallback</slot></div></slot></div></template></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle nested SLOTs
        final NodeList slotElements = doc.getElementsByTagName("SLOT");
        assertEquals(2, slotElements.getLength(), "Should have two nested SLOT elements");
    }

    // ========================================================================
    // HGROUP Element Tests
    // ========================================================================

    @Test
    public void testHgroupWithMultipleHeadings() throws Exception {
        // Given: HTML with HGROUP containing multiple headings
        final String html = "<html><body><hgroup><h1>Main Title</h1><h2>Subtitle</h2></hgroup></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: HGROUP should contain both headings
        final NodeList hgroupElements = doc.getElementsByTagName("HGROUP");
        assertEquals(1, hgroupElements.getLength(), "Should have HGROUP element");

        final Element hgroup = (Element) hgroupElements.item(0);
        final NodeList h1Elements = hgroup.getElementsByTagName("H1");
        assertEquals(1, h1Elements.getLength(), "HGROUP should contain H1");

        final NodeList h2Elements = hgroup.getElementsByTagName("H2");
        assertEquals(1, h2Elements.getLength(), "HGROUP should contain H2");
    }

    @Test
    public void testHgroupWithAllHeadingLevels() throws Exception {
        // Given: HTML with HGROUP containing H1-H6
        final String html =
                "<html><body><hgroup>" + "<h1>Level 1</h1>" + "<h2>Level 2</h2>" + "<h3>Level 3</h3>" + "<h4>Level 4</h4>"
                        + "<h5>Level 5</h5>" + "<h6>Level 6</h6>" + "</hgroup></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: HGROUP should contain all heading levels
        final NodeList hgroupElements = doc.getElementsByTagName("HGROUP");
        assertEquals(1, hgroupElements.getLength(), "Should have HGROUP element");

        final Element hgroup = (Element) hgroupElements.item(0);
        for (int i = 1; i <= 6; i++) {
            final NodeList headings = hgroup.getElementsByTagName("H" + i);
            assertEquals(1, headings.getLength(), "HGROUP should contain H" + i);
        }
    }

    @Test
    public void testHgroupWithParagraph() throws Exception {
        // Given: HTML with HGROUP containing P (allowed in HTML5.2+)
        final String html = "<html><body><hgroup><h1>Title</h1><p>Additional context</p></hgroup></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: HGROUP should contain H1 and P
        final NodeList hgroupElements = doc.getElementsByTagName("HGROUP");
        assertEquals(1, hgroupElements.getLength(), "Should have HGROUP element");

        final Element hgroup = (Element) hgroupElements.item(0);
        assertEquals(1, hgroup.getElementsByTagName("H1").getLength(), "HGROUP should contain H1");
        assertEquals(1, hgroup.getElementsByTagName("P").getLength(), "HGROUP should contain P");
    }

    @Test
    public void testHgroupInHeader() throws Exception {
        // Given: HTML with HGROUP in HEADER
        final String html = "<html><body><header><hgroup><h1>Site Title</h1><h2>Tagline</h2></hgroup></header></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: HEADER should contain HGROUP
        final NodeList headers = doc.getElementsByTagName("HEADER");
        assertEquals(1, headers.getLength(), "Should have HEADER element");

        final Element header = (Element) headers.item(0);
        final NodeList hgroups = header.getElementsByTagName("HGROUP");
        assertEquals(1, hgroups.getLength(), "HEADER should contain HGROUP");
    }

    @Test
    public void testHgroupInArticle() throws Exception {
        // Given: HTML with HGROUP in ARTICLE
        final String html =
                "<html><body><article><hgroup><h1>Article Title</h1><h2>Author Name</h2></hgroup><p>Article content</p></article></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: ARTICLE should contain HGROUP
        final NodeList articles = doc.getElementsByTagName("ARTICLE");
        assertEquals(1, articles.getLength(), "Should have ARTICLE element");

        final Element article = (Element) articles.item(0);
        final NodeList hgroups = article.getElementsByTagName("HGROUP");
        assertEquals(1, hgroups.getLength(), "ARTICLE should contain HGROUP");
    }

    @Test
    public void testHgroupInSection() throws Exception {
        // Given: HTML with HGROUP in SECTION
        final String html =
                "<html><body><section><hgroup><h2>Section Title</h2><h3>Section Subtitle</h3></hgroup><p>Content</p></section></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: SECTION should contain HGROUP
        final NodeList sections = doc.getElementsByTagName("SECTION");
        assertEquals(1, sections.getLength(), "Should have SECTION element");

        final Element section = (Element) sections.item(0);
        final NodeList hgroups = section.getElementsByTagName("HGROUP");
        assertEquals(1, hgroups.getLength(), "SECTION should contain HGROUP");
    }

    @Test
    public void testMultipleHgroups() throws Exception {
        // Given: HTML with multiple HGROUPs
        final String html =
                "<html><body>" + "<article><hgroup><h1>First Article</h1><h2>First Subtitle</h2></hgroup><p>Content 1</p></article>"
                        + "<article><hgroup><h1>Second Article</h1><h2>Second Subtitle</h2></hgroup><p>Content 2</p></article>"
                        + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should have two HGROUP elements
        final NodeList hgroups = doc.getElementsByTagName("HGROUP");
        assertEquals(2, hgroups.getLength(), "Should have two HGROUP elements");
    }

    @Test
    public void testHgroupWithBlockElement() throws Exception {
        // Given: HTML with HGROUP containing block element
        final String html = "<html><body><hgroup><h1>Title</h1><h2>Subtitle</h2></hgroup><div>After HGROUP</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: HGROUP should be properly parsed
        final NodeList hgroups = doc.getElementsByTagName("HGROUP");
        assertEquals(1, hgroups.getLength(), "Should have HGROUP element");

        final Element hgroup = (Element) hgroups.item(0);
        assertEquals(1, hgroup.getElementsByTagName("H1").getLength(), "HGROUP should contain H1");
        assertEquals(1, hgroup.getElementsByTagName("H2").getLength(), "HGROUP should contain H2");

        // DIV should exist in document
        final NodeList bodyDivs = doc.getElementsByTagName("DIV");
        assertEquals(1, bodyDivs.getLength(), "DIV should exist in document");
    }

    // ========================================================================
    // Complex Semantic Element Combinations
    // ========================================================================

    @Test
    public void testComplexSemanticStructureWithNewElements() throws Exception {
        // Given: Complex HTML using SEARCH, SLOT, HGROUP together
        final String html =
                "<html><body>" + "<header>" + "<hgroup><h1>Website Title</h1><h2>Tagline</h2></hgroup>"
                        + "<search><input type=\"search\" placeholder=\"Search site...\"></search>" + "</header>" + "<main>" + "<article>"
                        + "<hgroup><h2>Article Title</h2><h3>Article Subtitle</h3></hgroup>" + "<p>Article content here</p>" + "</article>"
                        + "<aside>" + "<search><form><input type=\"search\" placeholder=\"Filter...\"></form></search>" + "</aside>"
                        + "</main>" + "<template id=\"card\">" + "<slot name=\"title\"><h3>Default Title</h3></slot>"
                        + "<slot name=\"content\"><p>Default content</p></slot>" + "</template>" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: All elements should be properly structured
        assertEquals(2, doc.getElementsByTagName("HGROUP").getLength(), "Should have 2 HGROUP elements");
        assertEquals(2, doc.getElementsByTagName("SEARCH").getLength(), "Should have 2 SEARCH elements");
        assertEquals(2, doc.getElementsByTagName("SLOT").getLength(), "Should have 2 SLOT elements");

        // And: Verify proper nesting
        final NodeList headers = doc.getElementsByTagName("HEADER");
        assertEquals(1, headers.getLength(), "Should have HEADER");

        final Element header = (Element) headers.item(0);
        assertNotNull(header.getElementsByTagName("HGROUP").item(0), "HEADER should contain HGROUP");
        assertNotNull(header.getElementsByTagName("SEARCH").item(0), "HEADER should contain SEARCH");
    }

    @Test
    public void testSearchInMain() throws Exception {
        // Given: SEARCH in MAIN element
        final String html =
                "<html><body><main><search><form action=\"/search\"><input type=\"search\" name=\"q\"></form></search></main></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: SEARCH should be in MAIN
        final NodeList mains = doc.getElementsByTagName("MAIN");
        assertEquals(1, mains.getLength(), "Should have MAIN element");

        final Element main = (Element) mains.item(0);
        final NodeList searches = main.getElementsByTagName("SEARCH");
        assertEquals(1, searches.getLength(), "MAIN should contain SEARCH");
    }

    @Test
    public void testHgroupInFooter() throws Exception {
        // Given: HGROUP in FOOTER (less common but valid)
        final String html =
                "<html><body><footer><hgroup><h2>Footer Section</h2><h3>Additional Info</h3></hgroup><p>Footer content</p></footer></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: FOOTER should contain HGROUP
        final NodeList footers = doc.getElementsByTagName("FOOTER");
        assertEquals(1, footers.getLength(), "Should have FOOTER element");

        final Element footer = (Element) footers.item(0);
        final NodeList hgroups = footer.getElementsByTagName("HGROUP");
        assertEquals(1, hgroups.getLength(), "FOOTER should contain HGROUP");
    }

    @Test
    public void testSearchWithComplexFormElements() throws Exception {
        // Given: SEARCH with complex form containing multiple inputs
        final String html =
                "<html><body><search><form>" + "<fieldset>" + "<legend>Advanced Search</legend>"
                        + "<label>Keyword: <input type=\"search\" name=\"keyword\"></label>"
                        + "<label>Category: <select name=\"category\"><option>All</option><option>News</option></select></label>"
                        + "<label>Date: <input type=\"date\" name=\"date\"></label>" + "<button type=\"submit\">Search</button>"
                        + "</fieldset>" + "</form></search></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: SEARCH should contain complex form structure
        final NodeList searches = doc.getElementsByTagName("SEARCH");
        assertEquals(1, searches.getLength(), "Should have SEARCH element");

        final Element search = (Element) searches.item(0);
        assertEquals(1, search.getElementsByTagName("FORM").getLength(), "SEARCH should contain FORM");
        assertEquals(1, search.getElementsByTagName("FIELDSET").getLength(), "SEARCH should contain FIELDSET");
        assertEquals(1, search.getElementsByTagName("LEGEND").getLength(), "SEARCH should contain LEGEND");
        assertEquals(1, search.getElementsByTagName("SELECT").getLength(), "SEARCH should contain SELECT");
        assertEquals(1, search.getElementsByTagName("BUTTON").getLength(), "SEARCH should contain BUTTON");
    }

    @Test
    public void testSlotInCustomElement() throws Exception {
        // Given: SLOT in custom element context
        final String html =
                "<html><body><template><custom-card><slot name=\"header\"></slot><slot></slot><slot name=\"footer\"></slot></custom-card></template></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle SLOT in custom element
        final NodeList slots = doc.getElementsByTagName("SLOT");
        assertEquals(3, slots.getLength(), "Should have 3 SLOT elements");

        // Note: Custom elements are parsed as unknown elements, which is expected
        final NodeList customCards = doc.getElementsByTagName("CUSTOM-CARD");
        assertEquals(1, customCards.getLength(), "Should have CUSTOM-CARD element");
    }

    @Test
    public void testEmptySearchElement() throws Exception {
        // Given: Empty SEARCH element
        final String html = "<html><body><search></search></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Empty SEARCH should be parsed correctly
        final NodeList searches = doc.getElementsByTagName("SEARCH");
        assertEquals(1, searches.getLength(), "Should have SEARCH element");

        final Element search = (Element) searches.item(0);
        assertEquals(0, search.getChildNodes().getLength(), "SEARCH should be empty");
    }

    @Test
    public void testEmptyHgroupElement() throws Exception {
        // Given: Empty HGROUP element (technically invalid but should parse)
        final String html = "<html><body><hgroup></hgroup></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Empty HGROUP should be parsed
        final NodeList hgroups = doc.getElementsByTagName("HGROUP");
        assertEquals(1, hgroups.getLength(), "Should have HGROUP element");
    }

    @Test
    public void testSlotOutsideTemplate() throws Exception {
        // Given: SLOT outside TEMPLATE (unusual but should parse)
        final String html = "<html><body><div><slot name=\"test\">Fallback</slot></div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: SLOT should be parsed even outside TEMPLATE
        final NodeList slots = doc.getElementsByTagName("SLOT");
        assertEquals(1, slots.getLength(), "Should have SLOT element");

        final Element slot = (Element) slots.item(0);
        assertEquals("test", slot.getAttribute("name"), "SLOT should have name attribute");
    }
}
