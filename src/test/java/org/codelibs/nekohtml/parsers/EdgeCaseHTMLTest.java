package org.codelibs.nekohtml.parsers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Additional edge case tests for real-world malformed HTML patterns.
 * These tests cover patterns that are technically invalid but commonly found.
 */
public class EdgeCaseHTMLTest {

    @Test
    public void testFramesetWithBody() throws Exception {
        // Frameset and body mixed - invalid but exists in legacy sites
        final String html = "<html><frameset><frame src='a.html'></frameset><body><p>Text</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Parser should handle gracefully
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();
        assertTrue(((NodeList) xpath.evaluate("//FRAMESET | //BODY", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength() >= 1);
    }

    @Test
    public void testDeeplyNestedTables() throws Exception {
        // Nested tables without proper structure
        final String html =
                "<html><body><table><tr><td>" + "<table><tr><td>" + "<table><tr><td>Deep</td></tr></table>" + "</td></tr></table>"
                        + "</td></tr></table>" + "</body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();
        final NodeList tables = (NodeList) xpath.evaluate("//TABLE", doc, javax.xml.xpath.XPathConstants.NODESET);
        assertEquals(3, tables.getLength(), "Should have 3 nested tables");
    }

    @Test
    public void testScriptInUnexpectedLocation() throws Exception {
        // Script tag in table cell - technically valid but unusual
        final String html = "<html><body><table><tr><td><script>alert('test');</script><p>Text</p></td></tr></table></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();
        assertEquals(1, ((NodeList) xpath.evaluate("//TD/SCRIPT", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(1, ((NodeList) xpath.evaluate("//TD/P", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
    }

    @Test
    public void testStyleInBody() throws Exception {
        // Style tag in body - common in real HTML
        final String html = "<html><body><div><style>.test{color:red;}</style><p>Text</p></div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();
        assertEquals(1, ((NodeList) xpath.evaluate("//STYLE", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
    }

    @Test
    public void testBlockElementInListItem() throws Exception {
        // Block elements inside list items - HTML5 allows this
        final String html = "<html><body><ul><li><div><p>Paragraph in list</p></div></li></ul></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();
        assertEquals(1, ((NodeList) xpath.evaluate("//LI/DIV/P", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength(),
                "Should preserve LI > DIV > P structure");
    }

    @Test
    public void testTableWithoutTr() throws Exception {
        // Table with td directly - extremely malformed
        final String html = "<html><body><table><td>Cell</td></table></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Parser should handle by auto-inserting tbody/tr
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();
        assertEquals(1, ((NodeList) xpath.evaluate("//TD", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
    }

    @Test
    public void testDivClosingDifferentTag() throws Exception {
        // Closing tag doesn't match opening - classic error
        final String html = "<html><body><div><span>Text</div></span></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Should handle gracefully
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();
        assertTrue(((NodeList) xpath.evaluate("//DIV | //SPAN", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength() >= 1);
    }

    @Test
    public void testEmptyAttributes() throws Exception {
        // Empty and unquoted attributes - common in old HTML
        final String html = "<html><body><input type=text name= value=''><div class></div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();
        assertEquals(1, ((NodeList) xpath.evaluate("//INPUT", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(1, ((NodeList) xpath.evaluate("//DIV", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
    }

    @Test
    public void testMultipleBodies() throws Exception {
        // Multiple body tags - malformed but seen in practice
        final String html = "<html><body><p>First</p></body><body><p>Second</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Should handle gracefully
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();
        final NodeList paragraphs = (NodeList) xpath.evaluate("//P", doc, javax.xml.xpath.XPathConstants.NODESET);
        assertTrue(paragraphs.getLength() >= 1, "Should have at least one paragraph");
    }

    @Test
    public void testLegacyElementsNesting() throws Exception {
        // Legacy elements with modern HTML5
        final String html =
                "<html><body>" + "<center><section><article>" + "<font color='red'><p>Text</p></font>" + "</article></section></center>"
                        + "</body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // All elements should be present
        assertEquals(1, ((NodeList) xpath.evaluate("//CENTER", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(1, ((NodeList) xpath.evaluate("//SECTION", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(1, ((NodeList) xpath.evaluate("//ARTICLE", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(1, ((NodeList) xpath.evaluate("//FONT", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
    }

    @Test
    public void testFormNesting() throws Exception {
        // Nested forms - invalid but exists
        final String html = "<html><body><form><div><form><input type='text'></form></div></form></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Should handle gracefully
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();
        assertTrue(((NodeList) xpath.evaluate("//FORM", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength() >= 1);
        assertEquals(1, ((NodeList) xpath.evaluate("//INPUT", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
    }

    @Test
    public void testListWithInvalidStructure() throws Exception {
        // List with various invalid structures
        final String html = "<html><body>" + "<ul>" + "<div><li>Item 1</li></div>" + // div around li
                "<li><ul><li>Nested</li></ul></li>" + // proper nested list
                "Text node" + // text directly in ul
                "<p>Paragraph in list</p>" + // p in ul
                "</ul>" + "</body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Should have found LI elements
        assertTrue(((NodeList) xpath.evaluate("//LI", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength() >= 2);
    }

    @Test
    public void testDoctypeWithMalformedHTML() throws Exception {
        // DOCTYPE with malformed HTML following
        final String html = "<!DOCTYPE html>" + "<html>" + "<head><title>Test" + // unclosed title
                "<body>" + // no closing head
                "<div><p>Text" + // unclosed tags
                "</html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        assertEquals(1, ((NodeList) xpath.evaluate("//TITLE", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(1, ((NodeList) xpath.evaluate("//DIV", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        assertEquals(1, ((NodeList) xpath.evaluate("//P", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
    }
}
