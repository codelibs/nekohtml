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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Tests for attribute edge cases including malformed attributes, special characters,
 * boolean attributes, and various attribute value formats.
 *
 * @author CodeLibs Project
 */
public class AttributeEdgeCasesTest {

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
    // Attribute Name Edge Cases
    // ========================================================================

    @Test
    public void testAttributeWithUnicodeCharacters() throws Exception {
        // Given: Attributes with Unicode characters
        final String html = "<html><body><div data-日本語=\"value\" data-中文=\"value2\">Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle Unicode in attribute names
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertNotNull(div, "DIV should exist");
        assertTrue(div.hasAttributes(), "DIV should have attributes");
    }

    @Test
    public void testDuplicateAttributeNames() throws Exception {
        // Given: Element with duplicate attribute names (invalid but common)
        final String html = "<html><body><div class=\"first\" class=\"second\" class=\"third\">Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle duplicate attributes (typically first or last wins)
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertNotNull(div, "DIV should exist");
        assertTrue(div.hasAttribute("class"), "Should have class attribute");

        // Note: Behavior with duplicates is parser-dependent
        final String classValue = div.getAttribute("class");
        assertNotNull(classValue, "Class value should not be null");
    }

    @Test
    public void testAttributesWithColons() throws Exception {
        // Given: Attributes with colons (namespace-like)
        final String html = "<html><body><div xml:lang=\"en\" xlink:href=\"#\" xmlns:custom=\"http://example.com\">Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle colon in attribute names
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertNotNull(div, "DIV should exist");
        assertTrue(div.hasAttributes(), "Should have attributes");
    }

    @Test
    public void testAttributeNameWithHyphens() throws Exception {
        // Given: Attributes with hyphens (data attributes)
        final String html = "<html><body><div data-test-value=\"123\" data-user-id=\"456\" aria-label=\"Test\">Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle hyphens in attribute names
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertTrue(div.hasAttribute("data-test-value"), "Should have data-test-value");
        assertTrue(div.hasAttribute("data-user-id"), "Should have data-user-id");
        assertTrue(div.hasAttribute("aria-label"), "Should have aria-label");

        assertEquals("123", div.getAttribute("data-test-value"), "Should have correct value");
        assertEquals("456", div.getAttribute("data-user-id"), "Should have correct value");
        assertEquals("Test", div.getAttribute("aria-label"), "Should have correct value");
    }

    @Test
    public void testAttributeNameWithUnderscores() throws Exception {
        // Given: Attributes with underscores
        final String html = "<html><body><div data_value=\"test\" my_custom_attr=\"value\">Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle underscores in attribute names
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertTrue(div.hasAttributes(), "Should have attributes");
    }

    @Test
    public void testCaseSensitivityOfAttributeNames() throws Exception {
        // Given: Same attribute with different cases
        final String html = "<html><body><div Class=\"upper\" class=\"lower\" CLASS=\"all-caps\">Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: In HTML mode, attribute names are case-insensitive
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertNotNull(div, "DIV should exist");

        // HTML normalizes attribute names to lowercase
        assertTrue(div.hasAttribute("class") || div.hasAttribute("CLASS"), "Should have class attribute");
    }

    // ========================================================================
    // Attribute Value Edge Cases
    // ========================================================================

    @Test
    public void testUnquotedAttributeValues() throws Exception {
        // Given: Unquoted attribute values
        final String html = "<html><body><div id=test123 class=myclass data-value=123>Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle unquoted values
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertEquals("test123", div.getAttribute("id"), "Should have id");
        assertEquals("myclass", div.getAttribute("class"), "Should have class");
        assertEquals("123", div.getAttribute("data-value"), "Should have data-value");
    }

    @Test
    public void testSingleQuotedAttributeValues() throws Exception {
        // Given: Single-quoted attribute values
        final String html = "<html><body><div id='test' class='myclass' title='It\\'s a test'>Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle single quotes
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertEquals("test", div.getAttribute("id"), "Should have id");
        assertEquals("myclass", div.getAttribute("class"), "Should have class");
    }

    @Test
    public void testDoubleQuotedAttributeValues() throws Exception {
        // Given: Double-quoted attribute values
        final String html = "<html><body><div id=\"test\" class=\"myclass\" title=\"A \\\"quoted\\\" string\">Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle double quotes
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertEquals("test", div.getAttribute("id"), "Should have id");
        assertEquals("myclass", div.getAttribute("class"), "Should have class");
    }

    @Test
    public void testMixedQuoteTypes() throws Exception {
        // Given: Mixed quote types (malformed but common)
        final String html = "<html><body><div title=\"value'>Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle mixed quotes gracefully
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertNotNull(div, "DIV should exist");
    }

    @Test
    public void testAttributeValueWithNewlines() throws Exception {
        // Given: Attribute values with newlines
        final String html = "<html><body><div title=\"Line 1\nLine 2\nLine 3\">Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle newlines in attribute values
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        final String title = div.getAttribute("title");
        assertNotNull(title, "Title should exist");
    }

    @Test
    public void testAttributeValueWithTabs() throws Exception {
        // Given: Attribute values with tabs
        final String html = "<html><body><div title=\"Col1\tCol2\tCol3\">Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle tabs in attribute values
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        final String title = div.getAttribute("title");
        assertNotNull(title, "Title should exist");
    }

    @Test
    public void testAttributeValueWithSpecialCharacters() throws Exception {
        // Given: Attribute values with special characters
        final String html = "<html><body><div data-value=\"<>&'\\\"!@#$%^&*()_+-={}[]|:;,.<>?\">Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle special characters
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertTrue(div.hasAttribute("data-value"), "Should have data-value attribute");
    }

    @Test
    public void testEmptyAttributeValue() throws Exception {
        // Given: Empty attribute values
        final String html = "<html><body><div id=\"\" class=\"\" title=\"\">Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle empty values
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertEquals("", div.getAttribute("id"), "Should have empty id");
        assertEquals("", div.getAttribute("class"), "Should have empty class");
        assertEquals("", div.getAttribute("title"), "Should have empty title");
    }

    @Test
    public void testVeryLongAttributeValue() throws Exception {
        // Given: Very long attribute value
        final StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longValue.append("x");
        }
        final String html = "<html><body><div data-long=\"" + longValue.toString() + "\">Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle long attribute values
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        final String value = div.getAttribute("data-long");
        assertEquals(10000, value.length(), "Should have full length value");
    }

    @Test
    public void testAttributeValueWithBackslashes() throws Exception {
        // Given: Attribute values with backslashes
        final String html = "<html><body><div data-path=\"C:\\Users\\Test\\file.txt\">Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle backslashes
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        final String path = div.getAttribute("data-path");
        assertTrue(path.contains("\\"), "Should contain backslashes");
    }

    // ========================================================================
    // Boolean Attributes
    // ========================================================================

    @Test
    public void testBooleanAttributesWithoutValues() throws Exception {
        // Given: Boolean attributes without values
        final String html = "<html><body>"
                + "<input type=\"checkbox\" checked>"
                + "<input type=\"text\" disabled>"
                + "<input type=\"text\" readonly>"
                + "<button autofocus>Button</button>"
                + "<script async></script>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle boolean attributes
        assertNotNull(doc, "Document should be parsed");
        final NodeList inputs = doc.getElementsByTagName("INPUT");
        assertTrue(inputs.getLength() >= 3, "Should have input elements");
    }

    @Test
    public void testBooleanAttributesWithValues() throws Exception {
        // Given: Boolean attributes with values (valid in HTML)
        final String html = "<html><body>"
                + "<input type=\"checkbox\" checked=\"checked\">"
                + "<input type=\"text\" disabled=\"disabled\">"
                + "<input type=\"text\" readonly=\"readonly\">"
                + "<option selected=\"selected\">Option</option>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle boolean attributes with values
        assertNotNull(doc, "Document should be parsed");
        final Element checkbox = (Element) doc.getElementsByTagName("INPUT").item(0);
        assertTrue(checkbox.hasAttribute("checked"), "Should have checked attribute");
    }

    @Test
    public void testBooleanAttributesWithInvalidValues() throws Exception {
        // Given: Boolean attributes with invalid values
        final String html = "<html><body>"
                + "<input type=\"checkbox\" checked=\"false\">"
                + "<input type=\"text\" disabled=\"no\">"
                + "<input type=\"text\" readonly=\"0\">"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse attributes (presence = true regardless of value)
        assertNotNull(doc, "Document should be parsed");
        final NodeList inputs = doc.getElementsByTagName("INPUT");
        assertEquals(3, inputs.getLength(), "Should have 3 inputs");
    }

    @Test
    public void testAllBooleanHTMLAttributes() throws Exception {
        // Given: All common boolean HTML attributes
        final String html = "<html><body>"
                + "<input checked disabled readonly required autofocus>"
                + "<button formnovalidate>Button</button>"
                + "<video autoplay controls loop muted>Video</video>"
                + "<ol reversed>List</ol>"
                + "<details open>Details</details>"
                + "<script async defer></script>"
                + "<iframe seamless></iframe>"
                + "<track default>"
                + "<option selected>Option</option>"
                + "<optgroup disabled>Group</optgroup>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle all boolean attributes
        assertNotNull(doc, "Document should be parsed");
        assertTrue(doc.getElementsByTagName("INPUT").getLength() > 0, "Should have INPUT");
        assertTrue(doc.getElementsByTagName("BUTTON").getLength() > 0, "Should have BUTTON");
        assertTrue(doc.getElementsByTagName("VIDEO").getLength() > 0, "Should have VIDEO");
    }

    // ========================================================================
    // Special Attribute Cases
    // ========================================================================

    @Test
    public void testAttributeWithoutValue() throws Exception {
        // Given: Attribute without = sign (treated as boolean)
        final String html = "<html><body><div data-test>Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle attribute without value
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertTrue(div.hasAttribute("data-test"), "Should have data-test attribute");
    }

    @Test
    public void testMultipleSpacesBetweenAttributes() throws Exception {
        // Given: Multiple spaces between attributes
        final String html = "<html><body><div    id=\"test\"     class=\"myclass\"    title=\"title\"    >Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle extra spaces
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertEquals("test", div.getAttribute("id"), "Should have id");
        assertEquals("myclass", div.getAttribute("class"), "Should have class");
        assertEquals("title", div.getAttribute("title"), "Should have title");
    }

    @Test
    public void testAttributesWithNewlinesBetweenThem() throws Exception {
        // Given: Attributes on multiple lines
        final String html = "<html><body><div\n"
                + "id=\"test\"\n"
                + "class=\"myclass\"\n"
                + "title=\"title\"\n"
                + ">Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle multiline attributes
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertEquals("test", div.getAttribute("id"), "Should have id");
        assertEquals("myclass", div.getAttribute("class"), "Should have class");
    }

    @Test
    public void testManyAttributesOnSingleElement() throws Exception {
        // Given: Element with many attributes
        final StringBuilder html = new StringBuilder("<html><body><div ");
        for (int i = 0; i < 100; i++) {
            html.append("attr").append(i).append("=\"value").append(i).append("\" ");
        }
        html.append(">Content</div></body></html>");

        // When: Parsing
        final Document doc = parseHTML(html.toString());

        // Then: Should handle many attributes
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertTrue(div.hasAttributes(), "Should have attributes");

        final NamedNodeMap attrs = div.getAttributes();
        assertTrue(attrs.getLength() > 0, "Should have multiple attributes");
    }

    @Test
    public void testDataAttributes() throws Exception {
        // Given: Various data attributes
        final String html = "<html><body>"
                + "<div data-id=\"123\" data-user-name=\"John Doe\" data-timestamp=\"2025-01-01\" data-json='{\"key\":\"value\"}'>Content</div>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle data attributes
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertEquals("123", div.getAttribute("data-id"), "Should have data-id");
        assertTrue(div.hasAttribute("data-user-name"), "Should have data-user-name");
        assertTrue(div.hasAttribute("data-timestamp"), "Should have data-timestamp");
        assertTrue(div.hasAttribute("data-json"), "Should have data-json");
    }

    @Test
    public void testAriaAttributes() throws Exception {
        // Given: ARIA attributes
        final String html = "<html><body>"
                + "<div role=\"button\" aria-label=\"Close\" aria-hidden=\"false\" aria-expanded=\"true\" aria-controls=\"menu\">Button</div>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle ARIA attributes
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertEquals("button", div.getAttribute("role"), "Should have role");
        assertEquals("Close", div.getAttribute("aria-label"), "Should have aria-label");
        assertEquals("false", div.getAttribute("aria-hidden"), "Should have aria-hidden");
        assertEquals("true", div.getAttribute("aria-expanded"), "Should have aria-expanded");
    }

    @Test
    public void testEventHandlerAttributes() throws Exception {
        // Given: Event handler attributes
        final String html = "<html><body>"
                + "<button onclick=\"alert('clicked')\" onmouseover=\"highlight()\" onmouseout=\"unhighlight()\">Button</button>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle event handler attributes
        assertNotNull(doc, "Document should be parsed");
        final Element button = (Element) doc.getElementsByTagName("BUTTON").item(0);
        assertTrue(button.hasAttribute("onclick"), "Should have onclick");
        assertTrue(button.hasAttribute("onmouseover"), "Should have onmouseover");
        assertTrue(button.hasAttribute("onmouseout"), "Should have onmouseout");
    }

    @Test
    public void testStyleAttribute() throws Exception {
        // Given: Style attribute with complex CSS
        final String html = "<html><body>"
                + "<div style=\"color: red; background-color: blue; margin: 10px; padding: 5px 10px; font-size: 14px;\">Content</div>"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle style attribute
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertTrue(div.hasAttribute("style"), "Should have style attribute");
        final String style = div.getAttribute("style");
        assertTrue(style.contains("color"), "Style should contain color");
    }

    @Test
    public void testClassAttributeWithMultipleValues() throws Exception {
        // Given: Class attribute with multiple space-separated values
        final String html = "<html><body><div class=\"class1 class2 class3 class4 class5\">Content</div></body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle multiple classes
        assertNotNull(doc, "Document should be parsed");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        final String classes = div.getAttribute("class");
        assertTrue(classes.contains("class1"), "Should contain class1");
        assertTrue(classes.contains("class2"), "Should contain class2");
        assertTrue(classes.contains("class5"), "Should contain class5");
    }

    @Test
    public void testAttributeWithURL() throws Exception {
        // Given: Attributes with URLs
        final String html = "<html><body>"
                + "<a href=\"https://example.com/path?param1=value1&param2=value2#anchor\">Link</a>"
                + "<img src=\"/images/photo.jpg\" alt=\"Photo\">"
                + "<link rel=\"stylesheet\" href=\"/css/styles.css\">"
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle URLs in attributes
        assertNotNull(doc, "Document should be parsed");
        final Element a = (Element) doc.getElementsByTagName("A").item(0);
        assertTrue(a.getAttribute("href").contains("example.com"), "Should have href with URL");
    }
}
