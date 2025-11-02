package org.codelibs.nekohtml.parsers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Tests for parsing malformed/broken HTML from real-world scenarios.
 * These tests verify that the parser can handle various broken HTML patterns
 * commonly found in actual websites.
 */
public class MalformedHTMLTest {

    @Test
    public void testDeeplyNestedDivs() throws Exception {
        // 10 levels of nested divs - common in real websites
        final String html =
                "<html><body>" + "<div><div><div><div><div>" + "<div><div><div><div><div>" + "Content" + "</div></div></div></div></div>"
                        + "</div></div></div></div></div>" + "</body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Verify structure is preserved
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();
        final NodeList divs = (NodeList) xpath.evaluate("//DIV", doc, javax.xml.xpath.XPathConstants.NODESET);
        assertEquals(10, divs.getLength(), "Should have 10 DIV elements");
    }

    @Test
    public void testTableWithoutTbody() throws Exception {
        // Table without tbody - very common in real HTML
        final String html = "<html><body><table><tr><td>Cell</td></tr></table></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Verify TD is accessible (tbody should be auto-inserted)
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();
        final NodeList cells = (NodeList) xpath.evaluate("//TD", doc, javax.xml.xpath.XPathConstants.NODESET);
        assertEquals(1, cells.getLength(), "Should have 1 TD element");
    }

    @Test
    public void testNestedListsWithoutLi() throws Exception {
        // Nested lists without li wrapper - malformed but exists
        final String html = "<html><body><ul><ul><li>Item</li></ul></ul></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();
        final NodeList items = (NodeList) xpath.evaluate("//LI", doc, javax.xml.xpath.XPathConstants.NODESET);
        assertEquals(1, items.getLength(), "Should have 1 LI element");
    }

    @Test
    public void testBlockElementInA() throws Exception {
        // Block element inside anchor - HTML5 allows this
        final String html = "<html><body><a href='#'><div>Click me</div></a></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Verify DIV is inside A
        final NodeList divInA = (NodeList) xpath.evaluate("//A/DIV", doc, javax.xml.xpath.XPathConstants.NODESET);
        assertEquals(1, divInA.getLength(), "DIV should be inside A element");
    }

    @Test
    public void testComplexSemanticNesting() throws Exception {
        // Complex HTML5 semantic nesting
        final String html =
                "<html><body>" + "<main>" + "<article>" + "<section>" + "<div>" + "<header><h1>Title</h1></header>"
                        + "<div><p>Content</p></div>" + "<footer><p>Footer</p></footer>" + "</div>" + "</section>" + "</article>"
                        + "</main>" + "</body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Verify all elements exist
        assertEquals(1, ((NodeList) xpath.evaluate("//MAIN", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(1, ((NodeList) xpath.evaluate("//ARTICLE", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(1, ((NodeList) xpath.evaluate("//SECTION", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(1, ((NodeList) xpath.evaluate("//HEADER", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(1, ((NodeList) xpath.evaluate("//FOOTER", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
    }

    @Test
    public void testUnclosedTags() throws Exception {
        // Multiple unclosed tags - should be auto-closed
        final String html = "<html><body><div><p>Text<span>More";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Should not crash and should have all elements
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();
        assertEquals(1, ((NodeList) xpath.evaluate("//DIV", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(1, ((NodeList) xpath.evaluate("//P", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(1, ((NodeList) xpath.evaluate("//SPAN", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
    }

    @Test
    public void testMismatchedTags() throws Exception {
        // Mismatched opening/closing tags
        final String html = "<html><body><div><span></div></span></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Parser should handle gracefully
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();
        assertTrue(((NodeList) xpath.evaluate("//DIV", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength() >= 1);
    }

    @Test
    public void testScriptInBody() throws Exception {
        // Script tag in body (not in head) - very common
        final String html = "<html><body><div><script>alert('test');</script><p>Text</p></div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // SCRIPT should be in the document
        assertEquals(1, ((NodeList) xpath.evaluate("//SCRIPT", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        // P should follow SCRIPT
        assertEquals(1, ((NodeList) xpath.evaluate("//P", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
    }

    @Test
    public void testFormWithComplexNesting() throws Exception {
        // Form with various nested elements including table
        final String html =
                "<html><body>" + "<form>" + "<div>" + "<table><tr><td><input type='text'></td></tr></table>" + "</div>"
                        + "<fieldset><legend>Legend</legend><input type='submit'></fieldset>" + "</form>" + "</body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Verify all form elements exist
        assertEquals(1, ((NodeList) xpath.evaluate("//FORM", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(1, ((NodeList) xpath.evaluate("//FIELDSET", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(1, ((NodeList) xpath.evaluate("//LEGEND", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(2, ((NodeList) xpath.evaluate("//INPUT", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
    }

    @Test
    public void testMinifiedHTML() throws Exception {
        // Minified HTML without whitespace
        final String html = "<html><body><div><section><article><p>Text</p></article></section></div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Verify structure
        assertEquals(1, ((NodeList) xpath.evaluate("//DIV/SECTION/ARTICLE/P", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength(),
                "Nested structure should be preserved");
    }

    @Test
    public void testLegacyHTML() throws Exception {
        // Legacy HTML with center, font tags
        final String html = "<html><body><center><font color='red'><b>Bold Red Text</b></font></center></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        assertEquals(1, ((NodeList) xpath.evaluate("//CENTER", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(1, ((NodeList) xpath.evaluate("//FONT", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(1, ((NodeList) xpath.evaluate("//B", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
    }
}
