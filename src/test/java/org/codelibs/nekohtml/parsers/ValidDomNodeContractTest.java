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

import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.count;
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.first;
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.firstText;
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.nodes;
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.parse;
import static org.codelibs.nekohtml.parsers.ValidHtmlTestSupport.childSignature;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Characterization tests for the DOM node contract ({@code Node}/{@code Element}/{@code Document}
 * behavior) produced by {@code DOMParser} on VALID/well-formed HTML input.
 */
public class ValidDomNodeContractTest {

    // -----------------------------------------------------------------
    // Combined structure: comment + attribute + mixed content
    // -----------------------------------------------------------------

    @Test
    public void documentElementIsUppercaseHtml() throws Exception {
        final Document doc = parse("<html><body><!--c--><p id=\"x\">t<br>u</p></body></html>");
        assertEquals("HTML", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void getElementsByTagNameFindsSingleP() throws Exception {
        final Document doc = parse("<html><body><!--c--><p id=\"x\">t<br>u</p></body></html>");
        assertEquals(1, doc.getElementsByTagName("P").getLength());
    }

    @Test
    public void pElementHasIdAttributeViaNamedNodeMap() throws Exception {
        final Document doc = parse("<html><body><!--c--><p id=\"x\">t<br>u</p></body></html>");
        final Element p = (Element) doc.getElementsByTagName("P").item(0);
        assertEquals("x", p.getAttributes().getNamedItem("id").getNodeValue());
    }

    @Test
    public void pChildSignatureCapturesTextBrTextInOrder() throws Exception {
        final Document doc = parse("<html><body><!--c--><p id=\"x\">t<br>u</p></body></html>");
        final Element p = (Element) doc.getElementsByTagName("P").item(0);
        assertEquals(List.of("text:t", "elem:BR", "text:u"), childSignature(p));
    }

    @Test
    public void brElementHasNoChildNodes() throws Exception {
        final Document doc = parse("<html><body><!--c--><p id=\"x\">t<br>u</p></body></html>");
        final Element br = (Element) doc.getElementsByTagName("BR").item(0);
        assertFalse(br.hasChildNodes());
    }

    @Test
    public void commentNodeExistsWithExpectedValue() throws Exception {
        final Document doc = parse("<html><body><!--c--><p id=\"x\">t<br>u</p></body></html>");
        final NodeList comments = nodes(doc, "//comment()");
        assertEquals(1, comments.getLength());
        final Node c = comments.item(0);
        assertEquals(Node.COMMENT_NODE, c.getNodeType());
        assertEquals("c", c.getNodeValue());
        assertTrue(c instanceof Comment);
    }

    // -----------------------------------------------------------------
    // Owner document
    // -----------------------------------------------------------------

    @Test
    public void ownerDocumentIsSameForRootElement() throws Exception {
        final Document doc = parse("<html><body><p id=\"x\">t</p></body></html>");
        assertSame(doc, doc.getDocumentElement().getOwnerDocument());
    }

    @Test
    public void ownerDocumentIsSameForNestedElement() throws Exception {
        final Document doc = parse("<html><body><p id=\"x\">t</p></body></html>");
        final Element p = (Element) doc.getElementsByTagName("P").item(0);
        assertSame(doc, p.getOwnerDocument());
    }

    @Test
    public void ownerDocumentIsSameForTextNode() throws Exception {
        final Document doc = parse("<p>a</p>");
        final Element p = first(doc, "//P");
        final Node text = p.getFirstChild();
        assertEquals(Node.TEXT_NODE, text.getNodeType());
        assertSame(doc, text.getOwnerDocument());
    }

    @Test
    public void ownerDocumentIsSameForCommentNode() throws Exception {
        final Document doc = parse("<p>a<!--c-->b</p>");
        final NodeList comments = nodes(doc, "//comment()");
        assertSame(doc, comments.item(0).getOwnerDocument());
    }

    @Test
    public void ownerDocumentNeverNullAcrossMultipleElements() throws Exception {
        final Document doc = parse("<html><body><div><p>a</p><span>b</span></div></body></html>");
        final NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            assertNotNull(all.item(i).getOwnerDocument());
            assertSame(doc, all.item(i).getOwnerDocument());
        }
    }

    // -----------------------------------------------------------------
    // Node types
    // -----------------------------------------------------------------

    @Test
    public void documentNodeTypeIsDocument() throws Exception {
        final Document doc = parse("<p>a</p>");
        assertEquals(Node.DOCUMENT_NODE, doc.getNodeType());
        assertEquals(9, doc.getNodeType());
    }

    @Test
    public void documentElementNodeTypeIsElement() throws Exception {
        final Document doc = parse("<p>a</p>");
        assertEquals(Node.ELEMENT_NODE, doc.getDocumentElement().getNodeType());
        assertEquals(1, doc.getDocumentElement().getNodeType());
    }

    @Test
    public void textNodeTypeIsText() throws Exception {
        final Document doc = parse("<p>a</p>");
        final Node text = first(doc, "//P").getFirstChild();
        assertEquals(Node.TEXT_NODE, text.getNodeType());
        assertEquals(3, text.getNodeType());
    }

    @Test
    public void commentNodeTypeIsComment() throws Exception {
        final Document doc = parse("<p>a<!--c-->b</p>");
        final Node comment = nodes(doc, "//comment()").item(0);
        assertEquals(Node.COMMENT_NODE, comment.getNodeType());
        assertEquals(8, comment.getNodeType());
    }

    // -----------------------------------------------------------------
    // getElementsByTagName("*") document order
    // -----------------------------------------------------------------

    @Test
    public void getElementsByTagNameWildcardReturnsDocumentOrder() throws Exception {
        final Document doc = parse("<html><body><div><p>a</p><span>b</span></div></body></html>");
        final NodeList all = doc.getElementsByTagName("*");
        final StringBuilder names = new StringBuilder();
        for (int i = 0; i < all.getLength(); i++) {
            if (i > 0) {
                names.append(",");
            }
            names.append(all.item(i).getNodeName());
        }
        // characterization: HTML and BODY are present as real elements in document order
        assertEquals("HTML,BODY,DIV,P,SPAN", names.toString());
    }

    // -----------------------------------------------------------------
    // Text content
    // -----------------------------------------------------------------

    @Test
    public void firstTextReturnsSimpleParagraphText() throws Exception {
        final Document doc = parse("<p>a</p>");
        assertEquals("a", firstText(doc, "//P"));
    }

    // -----------------------------------------------------------------
    // Attributes NamedNodeMap
    // -----------------------------------------------------------------

    @Test
    public void attributesNamedNodeMapHasThreeEntries() throws Exception {
        final Document doc = parse("<div id=\"a\" class=\"b\" data-x=\"1\">t</div>");
        final Element div = first(doc, "//DIV");
        assertEquals(3, div.getAttributes().getLength());
    }

    @Test
    public void attributesNamedNodeMapReadsClassAndDataAttribute() throws Exception {
        final Document doc = parse("<div id=\"a\" class=\"b\" data-x=\"1\">t</div>");
        final Element div = first(doc, "//DIV");
        assertEquals("b", div.getAttributes().getNamedItem("class").getNodeValue());
        assertEquals("1", div.getAttributes().getNamedItem("data-x").getNodeValue());
    }

    @Test
    public void attributesNamedNodeMapReturnsNullForMissingAttribute() throws Exception {
        final Document doc = parse("<div id=\"a\" class=\"b\" data-x=\"1\">t</div>");
        final Element div = first(doc, "//DIV");
        assertNull(div.getAttributes().getNamedItem("nope"));
    }

    // -----------------------------------------------------------------
    // Multiple same-name elements
    // -----------------------------------------------------------------

    @Test
    public void getElementsByTagNameLiFindsTwoItems() throws Exception {
        final Document doc = parse("<ul><li>1</li><li>2</li></ul>");
        assertEquals(2, doc.getElementsByTagName("LI").getLength());
    }

    @Test
    public void liItemsHaveExpectedTextInOrder() throws Exception {
        final Document doc = parse("<ul><li>1</li><li>2</li></ul>");
        final NodeList lis = doc.getElementsByTagName("LI");
        assertEquals("1", lis.item(0).getTextContent());
        assertEquals("2", lis.item(1).getTextContent());
    }

    // -----------------------------------------------------------------
    // Empty element
    // -----------------------------------------------------------------

    @Test
    public void brElementParsedAloneHasNoChildNodes() throws Exception {
        final Document doc = parse("<br>");
        final Element br = first(doc, "//BR");
        assertFalse(br.hasChildNodes());
    }

    @Test
    public void brElementParsedAloneIsUnderAutoInsertedHtmlOnly() throws Exception {
        final Document doc = parse("<br>");
        // characterization: only HTML is auto-inserted; HEAD/BODY are never auto-created
        assertEquals("HTML", doc.getDocumentElement().getNodeName());
        assertEquals(0, count(doc, "//BODY"));
    }

    // -----------------------------------------------------------------
    // NamedNodeMap absence on elements without attributes
    // -----------------------------------------------------------------

    @Test
    public void elementWithoutAttributesHasEmptyNamedNodeMap() throws Exception {
        final Document doc = parse("<p>a</p>");
        final Element p = first(doc, "//P");
        final NamedNodeMap attrs = p.getAttributes();
        assertNotNull(attrs);
        assertEquals(0, attrs.getLength());
    }
}
