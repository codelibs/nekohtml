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

import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.first;
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.parse;
import static org.codelibs.nekohtml.parsers.ValidHtmlTestSupport.childSignature;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Characterization tests for exact DOM child ordering, whitespace preservation, and HTML5 head/body
 * synthesis on VALID/well-formed HTML input (the scanner no longer fabricates a trailing newline).
 */
public class ValidNodeOrderingTest {

    @Test
    public void mixedContentChildSignatureExact() throws Exception {
        final Document doc = parse("<p>a <b>b</b> c</p>");
        assertEquals(List.of("text:a ", "elem:B", "text: c"), childSignature(first(doc, "//P")));
    }

    @Test
    public void whitespaceInsideDivIsNotCollapsed() throws Exception {
        final Document doc = parse("<div>  spaced  </div>");
        final String text = first(doc, "//DIV").getFirstChild().getNodeValue();
        assertTrue(text.contains("  spaced  "), text);
    }

    @Test
    public void multiLineTextKeepsEmbeddedNewline() throws Exception {
        final Document doc = parse("<p>l1\nl2</p>");
        final String text = first(doc, "//P").getFirstChild().getNodeValue();
        assertTrue(text.contains("l1\nl2"), text);
    }

    @Test
    public void trailingNewlineIsLastChildOfRoot() throws Exception {
        final Document doc = parse("<p>x</p>");
        final List<String> sig = childSignature(doc.getDocumentElement());
        // characterization: no fabricated trailing newline; the P content is wrapped in a synthesized
        // BODY (HTML5), so the root element's only/last child is BODY
        assertEquals("elem:BODY", sig.get(sig.size() - 1));
    }

    @Test
    public void nestedFormattingOrderPreservedForP() throws Exception {
        final Document doc = parse("<p><b><i>x</i></b></p>");
        final Node p = first(doc, "//P");
        assertEquals(List.of("elem:B"), childSignature(p));
    }

    @Test
    public void nestedFormattingOrderPreservedForB() throws Exception {
        final Document doc = parse("<p><b><i>x</i></b></p>");
        final Node b = first(doc, "//B");
        assertEquals(List.of("elem:I"), childSignature(b));
    }

    @Test
    public void nestedFormattingOrderPreservedForI() throws Exception {
        final Document doc = parse("<p><b><i>x</i></b></p>");
        final Node i = first(doc, "//I");
        assertEquals(List.of("text:x"), childSignature(i));
    }

    @Test
    public void adjacentTextRunsMergeIntoSingleTextChild() throws Exception {
        final Document doc = parse("<p>ab</p>");
        final Node p = first(doc, "//P");
        assertEquals(1, p.getChildNodes().getLength());
        assertEquals("ab", p.getFirstChild().getNodeValue());
    }

    @Test
    public void entityAcrossRunMergesIntoSingleDecodedTextChild() throws Exception {
        final Document doc = parse("<p>a&amp;b</p>");
        final Node p = first(doc, "//P");
        assertEquals(1, p.getChildNodes().getLength());
        assertEquals("a&b", p.getFirstChild().getNodeValue());
    }

    @Test
    public void betweenListItemsNewlinesAppearAsTextNodes() throws Exception {
        final Document doc = parse("<ul>\n<li>a</li>\n<li>b</li>\n</ul>");
        final List<String> sig = childSignature(first(doc, "//UL"));
        assertEquals(List.of("text:\n", "elem:LI", "text:\n", "elem:LI", "text:\n"), sig);
    }

    @Test
    public void commentInterleavingWithSurroundingText() throws Exception {
        final Document doc = parse("<div>a<!--c-->b</div>");
        final List<String> sig = childSignature(first(doc, "//DIV"));
        assertEquals(List.of("text:a", "comment:c", "text:b"), sig);
    }

    @Test
    public void crNormalizationRemovesCarriageReturnFromTextNodes() throws Exception {
        final Document doc = parse("<div>a\r\n<p>b</p>\r\nc</div>");
        final NodeList texts = doc.getElementsByTagName("*");
        for (int i = 0; i < texts.getLength(); i++) {
            final Node n = texts.item(i);
            for (Node child = n.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeType() == Node.TEXT_NODE) {
                    assertFalse(child.getNodeValue().contains("\r"), child.getNodeValue());
                }
            }
        }
    }

    @Test
    public void crlfBetweenElementsNormalizedToLf() throws Exception {
        final Document doc = parse("<div>a\r\nb</div>");
        final String text = first(doc, "//DIV").getFirstChild().getNodeValue();
        assertFalse(text.contains("\r"), text);
        assertTrue(text.contains("\n"), text);
    }

    @Test
    public void bareLfLineEndingPreservedAsSingleLf() throws Exception {
        final Document doc = parse("<p>x\ny</p>");
        final String text = first(doc, "//P").getFirstChild().getNodeValue();
        assertEquals("x\ny", text);
    }

    @Test
    public void multipleSiblingsWithTextAndElementsPreserveExactOrder() throws Exception {
        final Document doc = parse("<div>a<span>b</span>c<span>d</span>e</div>");
        final List<String> sig = childSignature(first(doc, "//DIV"));
        assertEquals(List.of("text:a", "elem:SPAN", "text:c", "elem:SPAN", "text:e"), sig);
    }

    @Test
    public void singleLevelListChildSignatureNoWhitespace() throws Exception {
        final Document doc = parse("<ul><li>1</li><li>2</li></ul>");
        final List<String> sig = childSignature(first(doc, "//UL"));
        assertEquals(List.of("elem:LI", "elem:LI"), sig);
    }

    @Test
    public void trailingNewlineAppearsEvenWhenRootHasAttributes() throws Exception {
        final Document doc = parse("<div id=\"x\">y</div>");
        final List<String> sig = childSignature(doc.getDocumentElement());
        // characterization: no fabricated trailing newline; the DIV is wrapped in a synthesized BODY,
        // so the root element's last child is BODY (attributes on DIV are irrelevant here)
        assertEquals("elem:BODY", sig.get(sig.size() - 1));
    }

    @Test
    public void commentBeforeTrailingNewlineAtRoot() throws Exception {
        final Document doc = parse("<p>x</p><!--c-->");
        final List<String> sig = childSignature(doc.getDocumentElement());
        // characterization: no fabricated trailing newline; the P and the comment are both wrapped in
        // a synthesized BODY, so the root element's only/last child is BODY
        assertEquals("elem:BODY", sig.get(sig.size() - 1));
    }

    @Test
    public void emptyElementBetweenTextRunsSplitsIntoSeparateTextNodes() throws Exception {
        final Document doc = parse("<p>before<br>after</p>");
        final List<String> sig = childSignature(first(doc, "//P"));
        assertEquals(List.of("text:before", "elem:BR", "text:after"), sig);
    }

    @Test
    public void whitespaceOnlyTextNodeBetweenBlockElementsIsPreserved() throws Exception {
        final Document doc = parse("<div><p>a</p> <p>b</p></div>");
        final List<String> sig = childSignature(first(doc, "//DIV"));
        assertEquals(List.of("elem:P", "text: ", "elem:P"), sig);
    }
}
