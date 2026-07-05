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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Broad robustness / backward-compatibility tests for HTML parsing.
 *
 * <p>
 * These tests focus on the guarantees a lenient, real-world HTML consumer (such
 * as a crawler that extracts visible text via the {@code //BODY} tree) depends
 * on: malformed input must still be recovered without dropping content, markup
 * that is not a tag must not leak into text, and raw-text/RCDATA elements must
 * keep their content literal. They complement the more targeted scanner and
 * tag-balancer unit tests and guard against silent regressions in the parsing
 * pipeline.
 * </p>
 *
 * @author CodeLibs Project
 */
public class HtmlParsingRobustnessTest {

    private static Document parse(final String html) throws Exception {
        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        return parser.getDocument();
    }

    private static int count(final Document doc, final String tag) {
        return doc.getElementsByTagName(tag).getLength();
    }

    private static String textOf(final Document doc, final String tag) {
        final NodeList nl = doc.getElementsByTagName(tag);
        return nl.getLength() == 0 ? null : norm(nl.item(0).getTextContent());
    }

    private static String bodyText(final Document doc) {
        return textOf(doc, "BODY");
    }

    /** Collapses ASCII whitespace runs and trims, so assertions are insensitive to incidental layout. */
    private static String norm(final String s) {
        return s == null ? null : s.replaceAll("[ \t\r\n\f]+", " ").trim();
    }

    // ------------------------------------------------------------------
    // Backward-compatibility content preservation (regression fixes)
    // ------------------------------------------------------------------

    @Test
    public void testUnterminatedQuotedAttributeKeepsElementAndText() throws Exception {
        // An unterminated quoted attribute value must not drop the tag and everything after it.
        final Document doc = parse("<html><body><div title=\"value'>Content</div></body></html>");
        assertEquals(1, count(doc, "DIV"), "DIV must survive an unterminated quoted attribute value");
        assertEquals("Content", bodyText(doc));
    }

    @Test
    public void testGtInsideClosedQuoteIsPartOfAttributeValue() throws Exception {
        final Document doc = parse("<html><body><div title=\"a>b\" href=\"x\">gt</div></body></html>");
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertEquals("a>b", div.getAttribute("title"), "'>' inside a closed quote must stay in the value");
        assertEquals("x", div.getAttribute("href"));
        assertEquals("gt", bodyText(doc));
    }

    @Test
    public void testUnclosedTitleDoesNotSwallowDocument() throws Exception {
        // An unclosed <title> (RCDATA) must not consume the rest of the document as text.
        final Document doc = parse("<html><head><title>un<body><p>c</body></html>");
        assertEquals(1, count(doc, "TITLE"));
        assertEquals("un", textOf(doc, "TITLE"));
        assertEquals(1, count(doc, "P"), "markup after an unclosed <title> must still parse");
        assertTrue(bodyText(doc).contains("c"));
    }

    @Test
    public void testScriptContentIsNotParsedAsMarkup() throws Exception {
        final Document doc = parse("<html><head><script>var x='<div>a</div>'; if(a<b){}</script></head><body>ok</body></html>");
        assertEquals(1, count(doc, "SCRIPT"));
        assertEquals(0, count(doc, "DIV"), "<div> inside <script> must stay raw text");
        assertEquals("var x='<div>a</div>'; if(a<b){}", textOf(doc, "SCRIPT"));
        assertEquals("ok", bodyText(doc));
    }

    @Test
    public void testUnclosedScriptRunsToEndOfInput() throws Exception {
        final Document doc = parse("<html><body><script>var x = '<div>a</div>';");
        assertEquals(1, count(doc, "SCRIPT"));
        assertEquals(0, count(doc, "DIV"), "unclosed <script> must not spawn a DIV element");
    }

    // ------------------------------------------------------------------
    // Unclosed tags and implied end tags
    // ------------------------------------------------------------------

    @Test
    public void testUnclosedParagraphsBecomeSiblings() throws Exception {
        final Document doc = parse("<html><body><p>one<p>two<p>three</body></html>");
        assertEquals(3, count(doc, "P"));
        assertEquals("onetwothree", bodyText(doc));
    }

    @Test
    public void testUnclosedListItemsBecomeSiblings() throws Exception {
        final Document doc = parse("<ul><li>a<li>b<li>c</ul>");
        assertEquals(3, count(doc, "LI"));
        assertEquals(1, count(doc, "UL"));
    }

    @Test
    public void testDefinitionListImpliedEnds() throws Exception {
        final Document doc = parse("<dl><dt>t1<dd>d1<dt>t2<dd>d2</dl>");
        assertEquals(2, count(doc, "DT"));
        assertEquals(2, count(doc, "DD"));
    }

    @Test
    public void testTableCellsAndRows() throws Exception {
        final Document doc = parse("<table><tr><td>a<td>b<tr><td>c<td>d</table>");
        assertEquals(2, count(doc, "TR"));
        assertEquals(4, count(doc, "TD"));
        assertEquals("abcd", bodyText(doc));
    }

    @Test
    public void testTableSections() throws Exception {
        final Document doc = parse("<table><thead><tr><th>h<tbody><tr><td>x</table>");
        assertEquals(1, count(doc, "THEAD"));
        assertEquals(1, count(doc, "TBODY"));
        assertEquals(1, count(doc, "TH"));
        assertEquals(1, count(doc, "TD"));
    }

    @Test
    public void testSelectOptionsBecomeSiblings() throws Exception {
        final Document doc = parse("<select><option>a<option>b</select>");
        assertEquals(2, count(doc, "OPTION"));
    }

    @Test
    public void testUnclosedDivsNest() throws Exception {
        final Document doc = parse("<html><body><div><div><div>deep</body></html>");
        assertEquals(3, count(doc, "DIV"));
        assertEquals("deep", bodyText(doc));
    }

    @Test
    public void testNestedListPreservesAllText() throws Exception {
        final Document doc = parse("<ul><li>a<ol><li>1<li>2</ol><li>b</ul>");
        assertEquals(1, count(doc, "OL"));
        assertEquals(1, count(doc, "UL"));
        assertEquals("a12b", bodyText(doc));
    }

    // ------------------------------------------------------------------
    // Misnested inline formatting (Adoption-Agency-style reconstruction)
    // ------------------------------------------------------------------

    @Test
    public void testMisnestedFormattingReopensInner() throws Exception {
        // <b><i>x</b>y : I and B close, then I is reopened so "y" stays italic.
        final Document doc = parse("<html><body><b><i>x</b>y</body></html>");
        assertEquals(1, count(doc, "B"));
        assertEquals(2, count(doc, "I"), "inner <i> reopened for the post-</b> text");
        assertEquals("xy", bodyText(doc));
        assertEquals("y", norm(doc.getElementsByTagName("I").item(1).getTextContent()));
    }

    @Test
    public void testStrongEmMisnestPreservesText() throws Exception {
        final Document doc = parse("<html><body><strong><em>x</strong>y</em>z</body></html>");
        assertEquals(2, count(doc, "EM"));
        assertEquals("xyz", bodyText(doc));
    }

    @Test
    public void testMisnestedAnchorsPreserveBothLinks() throws Exception {
        final Document doc = parse("<html><body><a href=\"/1\">one<a href=\"/2\">two</body></html>");
        assertEquals(2, count(doc, "A"));
        assertEquals("onetwo", bodyText(doc));
    }

    @Test
    public void testProperlyClosedInnerFormattingNotDuplicated() throws Exception {
        final Document doc = parse("<html><body><b><i>bi</i>afterB</b>tail</body></html>");
        assertEquals(1, count(doc, "B"));
        assertEquals(1, count(doc, "I"));
        assertEquals("biafterBtail", bodyText(doc));
    }

    // ------------------------------------------------------------------
    // Stray / orphan end tags
    // ------------------------------------------------------------------

    @Test
    public void testStrayEndTagIgnoredTextPreserved() throws Exception {
        final Document doc = parse("<html><body><p>text</span>more</p></body></html>");
        assertEquals(0, count(doc, "SPAN"), "stray </span> must not create a SPAN");
        assertEquals("textmore", bodyText(doc));
    }

    @Test
    public void testOrphanEndTagsIgnored() throws Exception {
        final Document doc = parse("<div>a</b>b</div>");
        assertEquals(0, count(doc, "B"));
        assertEquals("ab", textOf(doc, "DIV"));
    }

    // ------------------------------------------------------------------
    // Raw-text elements (script/style/xmp): content kept literal
    // ------------------------------------------------------------------

    @Test
    public void testStyleContentIsRawText() throws Exception {
        final Document doc = parse("<html><head><style>.a{color:red}/* <b> */</style></head><body>ok</body></html>");
        assertEquals(1, count(doc, "STYLE"));
        assertEquals(0, count(doc, "B"), "<b> inside a CSS comment must stay raw text");
        assertTrue(textOf(doc, "STYLE").contains("<b>"));
    }

    @Test
    public void testXmpContentIsRawText() throws Exception {
        final Document doc = parse("<xmp><b>literal</b></xmp>");
        assertEquals(1, count(doc, "XMP"));
        assertEquals(0, count(doc, "B"));
        assertEquals("<b>literal</b>", textOf(doc, "XMP"));
    }

    // ------------------------------------------------------------------
    // RCDATA elements (title/textarea): markup literal, entities resolved
    // ------------------------------------------------------------------

    @Test
    public void testTitleIsRcdata() throws Exception {
        final Document doc = parse("<title>a<b>b</b>c</title>x");
        assertEquals(1, count(doc, "TITLE"));
        assertEquals(0, count(doc, "B"), "markup inside <title> must stay literal, not an element");
        assertEquals("a<b>b</b>c", textOf(doc, "TITLE"));
        assertEquals("x", bodyText(doc));
    }

    @Test
    public void testTextareaIsRcdata() throws Exception {
        final Document doc = parse("<html><body><textarea><b>x</b> &amp; y</textarea></body></html>");
        assertEquals(1, count(doc, "TEXTAREA"));
        assertEquals(0, count(doc, "B"));
        assertEquals("<b>x</b> & y", textOf(doc, "TEXTAREA"));
    }

    // ------------------------------------------------------------------
    // Comments / PI / bogus comments / DOCTYPE must not leak into text
    // ------------------------------------------------------------------

    @Test
    public void testCommentNotInText() throws Exception {
        final Document doc = parse("<html><body><!-- secret -->visible</body></html>");
        assertEquals("visible", bodyText(doc));
    }

    @Test
    public void testUnterminatedCommentNotLeaked() throws Exception {
        final Document doc = parse("<html><body>before<!-- oops</body></html>");
        assertEquals("before", bodyText(doc));
    }

    @Test
    public void testProcessingInstructionNotLeaked() throws Exception {
        final Document doc = parse("<html><body><?php echo 1; ?>text</body></html>");
        assertEquals("text", bodyText(doc));
    }

    @Test
    public void testBogusCommentNotLeaked() throws Exception {
        final Document doc = parse("<html><body><!bogus>text</body></html>");
        assertEquals("text", bodyText(doc));
    }

    @Test
    public void testDoctypeIsConsumed() throws Exception {
        final Document doc = parse("<!DOCTYPE html><html><body>doc</body></html>");
        assertEquals(1, count(doc, "HTML"));
        assertEquals("doc", bodyText(doc));
    }

    // ------------------------------------------------------------------
    // Entities
    // ------------------------------------------------------------------

    @Test
    public void testBasicNamedEntities() throws Exception {
        final Document doc = parse("<p>&amp; &lt; &gt; &quot; &apos;</p>");
        assertEquals("& < > \" '", textOf(doc, "P"));
    }

    @Test
    public void testNumericEntities() throws Exception {
        assertEquals("ABC", textOf(parse("<p>&#65;&#66;&#67;</p>"), "P"));
    }

    @Test
    public void testHexEntities() throws Exception {
        assertEquals("AB", textOf(parse("<p>&#x41;&#x42;</p>"), "P"));
    }

    @Test
    public void testCommonSymbolEntities() throws Exception {
        final String t = textOf(parse("<p>&nbsp;&copy;&reg;</p>"), "P");
        assertTrue(t.contains("©"), "&copy; -> (C)");
        assertTrue(t.contains("®"), "&reg; -> (R)");
    }

    @Test
    public void testHtml5NamedEntities() throws Exception {
        final String t = parse("<p>&lpar;&NotEqualTilde;</p>").getElementsByTagName("P").item(0).getTextContent();
        assertTrue(t.startsWith("("), "&lpar; -> '('");
    }

    @Test
    public void testUnknownEntityLeftLiteral() throws Exception {
        assertTrue(textOf(parse("<p>&notarealentity;</p>"), "P").contains("&notarealentity;"));
    }

    @Test
    public void testEntityInAttributeValue() throws Exception {
        final Document doc = parse("<html><body><a href=\"?a=1&amp;b=2\">x</a></body></html>");
        assertEquals("?a=1&b=2", ((Element) doc.getElementsByTagName("A").item(0)).getAttribute("href"));
    }

    // ------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------

    @Test
    public void testSingleQuotedAttribute() throws Exception {
        assertEquals("x", ((Element) parse("<div id='x'>y</div>").getElementsByTagName("DIV").item(0)).getAttribute("id"));
    }

    @Test
    public void testUnquotedAttributes() throws Exception {
        final Element input = (Element) parse("<input type=text name=q value=hello>").getElementsByTagName("INPUT").item(0);
        assertEquals("text", input.getAttribute("type"));
        assertEquals("q", input.getAttribute("name"));
        assertEquals("hello", input.getAttribute("value"));
    }

    @Test
    public void testWhitespaceAroundEquals() throws Exception {
        final Element a = (Element) parse("<a href = \"x\">y</a>").getElementsByTagName("A").item(0);
        assertEquals("x", a.getAttribute("href"));
        assertTrue(a.getAttribute("x").isEmpty(), "no spurious 'x' attribute from the value");
    }

    @Test
    public void testBooleanAttributes() throws Exception {
        final Element div = (Element) parse("<div checked disabled>x</div>").getElementsByTagName("DIV").item(0);
        assertTrue(div.hasAttribute("checked"));
        assertTrue(div.hasAttribute("disabled"));
    }

    @Test
    public void testDuplicateAttributeFirstWins() throws Exception {
        assertEquals("1",
                ((Element) parse("<div data-a=\"1\" data-a=\"2\">x</div>").getElementsByTagName("DIV").item(0)).getAttribute("data-a"));
    }

    @Test
    public void testEmptyAttributeValue() throws Exception {
        final Element div = (Element) parse("<div class=\"\">x</div>").getElementsByTagName("DIV").item(0);
        assertTrue(div.hasAttribute("class"));
        assertEquals("", div.getAttribute("class"));
    }

    // ------------------------------------------------------------------
    // Void elements
    // ------------------------------------------------------------------

    @Test
    public void testVoidElements() throws Exception {
        final Document doc = parse("<html><body><br><hr><img src=x><p>after</p></body></html>");
        assertEquals(1, count(doc, "BR"));
        assertEquals(1, count(doc, "HR"));
        assertEquals(1, count(doc, "IMG"));
        assertEquals(1, count(doc, "P"));
        assertEquals("after", bodyText(doc));
    }

    @Test
    public void testVoidEndTagIgnored() throws Exception {
        final Document doc = parse("<html><body><br></br>x</body></html>");
        assertEquals(1, count(doc, "BR"), "</br> must not create a second BR");
        assertEquals("x", bodyText(doc));
    }

    // ------------------------------------------------------------------
    // HEAD / BODY synthesis and non-null document element
    // ------------------------------------------------------------------

    @Test
    public void testBodySynthesizedForContent() throws Exception {
        final Document doc = parse("<p>hi</p>");
        assertEquals(1, count(doc, "BODY"));
        assertEquals("BODY", doc.getElementsByTagName("P").item(0).getParentNode().getNodeName());
    }

    @Test
    public void testHeadSynthesizedForTitle() throws Exception {
        final Document doc = parse("<title>t</title><p>p</p>");
        assertEquals("HEAD", doc.getElementsByTagName("TITLE").item(0).getParentNode().getNodeName());
        assertEquals("BODY", doc.getElementsByTagName("P").item(0).getParentNode().getNodeName());
    }

    @Test
    public void testTextOnlyDocumentWrappedInBody() throws Exception {
        assertEquals("hello", bodyText(parse("hello")));
    }

    @Test
    public void testWhitespaceOnlyInputHasHtmlRoot() throws Exception {
        // A blank page must still yield a non-null document element (compatibility guarantee).
        final Element root = parse("   \n  ").getDocumentElement();
        assertNotNull(root, "whitespace-only input must have a document element");
        assertEquals("HTML", root.getNodeName());
    }

    @Test
    public void testCommentOnlyInputHasHtmlRoot() throws Exception {
        final Element root = parse("<!-- just a comment -->").getDocumentElement();
        assertNotNull(root, "comment-only input must have a document element");
        assertEquals("HTML", root.getNodeName());
    }

    @Test
    public void testEmptyInputHasNoRoot() throws Exception {
        assertNull(parse("").getDocumentElement(), "truly empty input has no document element");
    }

    // ------------------------------------------------------------------
    // Non-tag markup and robustness
    // ------------------------------------------------------------------

    @Test
    public void testLessThanInTextPreserved() throws Exception {
        final Document doc = parse("<html><body><p>5 < 2 and x</p></body></html>");
        assertTrue(bodyText(doc).contains("5 < 2 and x"), "a '<' not starting a tag stays as text");
    }

    @Test
    public void testDeeplyNestedDivsParse() throws Exception {
        final int n = 500;
        final StringBuilder sb = new StringBuilder("<html><body>");
        for (int i = 0; i < n; i++) {
            sb.append("<div>");
        }
        sb.append("deep");
        for (int i = 0; i < n; i++) {
            sb.append("</div>");
        }
        sb.append("</body></html>");
        final Document doc = parse(sb.toString());
        assertEquals(n, count(doc, "DIV"));
        assertEquals("deep", bodyText(doc));
    }

    @Test
    public void testManyStrayEndTagsHandled() throws Exception {
        final StringBuilder sb = new StringBuilder("<html><body><p>x</p>");
        for (int i = 0; i < 2000; i++) {
            sb.append("</span>");
        }
        sb.append("<p>y</p></body></html>");
        final Document doc = parse(sb.toString());
        assertEquals(0, count(doc, "SPAN"), "stray </span> tags must all be ignored");
        assertEquals(2, count(doc, "P"));
        assertEquals("xy", bodyText(doc));
    }
}
