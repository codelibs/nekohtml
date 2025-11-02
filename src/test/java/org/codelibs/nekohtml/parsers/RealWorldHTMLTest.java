package org.codelibs.nekohtml.parsers;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class RealWorldHTMLTest {

    @Test
    public void testNoscriptMainParsing() throws Exception {
        // Test NOSCRIPT with IFRAME followed by MAIN
        final String html =
                "<html><body><noscript><iframe src=\"test\"></iframe></noscript><main><section>content</section></main></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Create XPath
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Check MAIN element
        final Node main = (Node) xpath.evaluate("//MAIN", doc, javax.xml.xpath.XPathConstants.NODE);
        System.out.println("=== MAIN exists: " + (main != null));
        if (main != null) {
            Node parent = main.getParentNode();
            System.out.println("=== MAIN's parent: " + parent.getNodeName());
        }

        // Check SECTION count
        final NodeList sections = (NodeList) xpath.evaluate("//SECTION", doc, javax.xml.xpath.XPathConstants.NODESET);
        System.out.println("=== SECTION count: " + sections.getLength());

        // Check BODY children
        final Node body = (Node) xpath.evaluate("//BODY", doc, javax.xml.xpath.XPathConstants.NODE);
        System.out.println("=== BODY's children:");
        printDirectChildren(body);
    }

    @Test
    public void testMainDivsSectionParsing() throws Exception {
        // Test MAIN with nested DIVs followed by SECTION - WITH closing tags
        final String htmlClosed =
                "<html><body><main><aside><div><div>content</div></div></aside><section>test</section></main></body></html>";

        final DOMParser parser1 = new DOMParser();
        parser1.parse(new InputSource(new java.io.StringReader(htmlClosed)));

        final Document doc1 = parser1.getDocument();
        assertNotNull(doc1, "Document should not be null");

        javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        javax.xml.xpath.XPath xpath = factory.newXPath();

        Node section = (Node) xpath.evaluate("//SECTION", doc1, javax.xml.xpath.XPathConstants.NODE);
        System.out
                .println("=== (With closing tags) SECTION's parent: " + (section != null ? section.getParentNode().getNodeName() : "N/A"));

        // Test MAIN with nested DIVs followed by SECTION - WITHOUT closing main/body tags (like Fess HTML)
        final String htmlUnclosed = "<html><body><main><aside><div><div>content</div></div></aside><section>test</section>";

        final DOMParser parser2 = new DOMParser();
        parser2.parse(new InputSource(new java.io.StringReader(htmlUnclosed)));

        final Document doc2 = parser2.getDocument();
        assertNotNull(doc2, "Document should not be null");

        factory = javax.xml.xpath.XPathFactory.newInstance();
        xpath = factory.newXPath();

        section = (Node) xpath.evaluate("//SECTION", doc2, javax.xml.xpath.XPathConstants.NODE);
        System.out.println("=== (Without closing tags) SECTION's parent: "
                + (section != null ? section.getParentNode().getNodeName() : "N/A"));

        NodeList mainSections = (NodeList) xpath.evaluate("//MAIN/SECTION", doc2, javax.xml.xpath.XPathConstants.NODESET);
        System.out.println("=== (Without closing tags) SECTION under MAIN: " + mainSections.getLength());
    }

    @Test
    public void testSimpleSectionParsing() throws Exception {
        // Simple HTML with section tag
        final String html = "<html><body><main><section><h2>Test</h2></section></main></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new java.io.StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Create XPath
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Check SECTION count
        final NodeList sections = (NodeList) xpath.evaluate("//SECTION", doc, javax.xml.xpath.XPathConstants.NODESET);
        System.out.println("=== SECTION count in simple HTML: " + sections.getLength());

        // Print DOM structure
        final Node body = (Node) xpath.evaluate("//BODY", doc, javax.xml.xpath.XPathConstants.NODE);
        System.out.println("=== DOM Structure ===");
        printDOMStructure(body, 0, 5);
    }

    @Test
    public void testFessRealPage() throws Exception {
        // Enable strict DOM mode to see actual errors
        System.setProperty("nekohtml.dom.strict", "false"); // Use false to see warnings but not fail

        // Load Fess documentation page from test resources
        final InputStream input = getClass().getResourceAsStream("/data/fess-search-form.html");
        assertNotNull(input, "Test HTML file should exist");

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(input));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Create XPath
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Check BODY element
        final Node body = (Node) xpath.evaluate("//BODY", doc, javax.xml.xpath.XPathConstants.NODE);
        assertNotNull(body, "BODY element should exist");

        System.out.println("=== BODY's direct children ===");
        printDirectChildren(body);

        // Check if MAIN exists anywhere
        final NodeList allMains = (NodeList) xpath.evaluate("//MAIN", doc, javax.xml.xpath.XPathConstants.NODESET);
        System.out.println("\n=== Total MAIN elements found: " + allMains.getLength() + " ===");
        if (allMains.getLength() > 0) {
            final Node mainNode = allMains.item(0);
            Node parent = mainNode.getParentNode();
            System.out.println("=== MAIN's parent chain ===");
            int level = 0;
            while (parent != null && level < 5) {
                System.out.println("  " + "  ".repeat(level) + "↑ " + parent.getNodeName());
                parent = parent.getParentNode();
                level++;
            }
        }

        // Check MAIN element
        final Node main = (Node) xpath.evaluate("//MAIN", doc, javax.xml.xpath.XPathConstants.NODE);
        if (main != null) {
            System.out.println("\n=== MAIN's direct children ===");
            printDirectChildren(main);
        } else {
            System.out.println("\n!!! MAIN element not found !!!");
        }

        // Check SECTION count
        final NodeList sections = (NodeList) xpath.evaluate("//SECTION", doc, javax.xml.xpath.XPathConstants.NODESET);
        System.out.println("\n=== Total SECTION elements found: " + sections.getLength());

        // Check where sections actually are with more specific XPath queries
        System.out.println("=== XPath location tests ===");
        System.out.println("  //HTML/SECTION count: "
                + ((NodeList) xpath.evaluate("//HTML/SECTION", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        System.out.println("  //BODY/SECTION count: "
                + ((NodeList) xpath.evaluate("//BODY/SECTION", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());
        System.out.println("  //MAIN//SECTION count (descendants): "
                + ((NodeList) xpath.evaluate("//MAIN//SECTION", doc, javax.xml.xpath.XPathConstants.NODESET)).getLength());

        // Check MAIN/SECTION relationship
        final NodeList mainSections = (NodeList) xpath.evaluate("//MAIN/SECTION", doc, javax.xml.xpath.XPathConstants.NODESET);
        System.out.println("=== SECTION elements directly under MAIN: " + mainSections.getLength());

        // Check MAIN/ARTICLE/SECTION relationship
        final NodeList articleSections = (NodeList) xpath.evaluate("//MAIN/ARTICLE/SECTION", doc, javax.xml.xpath.XPathConstants.NODESET);
        System.out.println("=== SECTION elements under MAIN/ARTICLE: " + articleSections.getLength());

        // Print full DOM structure
        System.out.println("\n=== Full DOM Structure (depth 8) ===");
        printDOMStructure(body, 0, 8); // Increased depth to see sections

        // Print all SECTION locations
        System.out.println("\n=== All SECTION elements locations ===");
        for (int i = 0; i < sections.getLength(); i++) {
            final Node section = sections.item(i);
            Node parent = section.getParentNode();
            System.out.println("SECTION #" + (i + 1) + " parent chain:");
            int level = 0;
            while (parent != null && level < 5) {
                System.out.println("  " + "  ".repeat(level) + "↑ " + parent.getNodeName());
                parent = parent.getParentNode();
                level++;
            }
        }
    }

    private void printDirectChildren(final Node node) {
        final NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                System.out.println("  - " + child.getNodeName());
            }
        }
    }

    private void printDOMStructure(final Node node, final int level, final int maxDepth) {
        if (node == null || level > maxDepth) {
            return;
        }

        final String indent = "  ".repeat(level);
        System.out.println(indent + "<" + node.getNodeName() + ">");

        final NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                printDOMStructure(child, level + 1, maxDepth);
            }
        }
    }
}
