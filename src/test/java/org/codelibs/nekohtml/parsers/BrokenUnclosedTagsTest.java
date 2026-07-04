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

import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Category A: unclosed tags.
 *
 * <p>
 * These tests lock in the current (characterization) behavior of the parser when tags are
 * never explicitly closed. The current implementation closes unclosed elements in LIFO order
 * at EOF, preserving parent/child nesting.
 * </p>
 */
public class BrokenUnclosedTagsTest {

    @Test
    public void singleUnclosedInlineClosedAtEof() throws Exception {
        // characterization: a single unclosed <b> is closed at EOF
        final Document doc = parse("<b>text");
        assertEquals(1, count(doc, "//B"));
        assertEquals("text", firstText(doc, "//B").trim());
    }

    @Test
    public void nestedUnclosedAllPresent() throws Exception {
        // characterization: unclosed tags are closed at EOF in LIFO order, preserving nesting
        final Document doc = parse("<html><body><div><p>Text<span>More");
        assertEquals(1, count(doc, "//DIV"));
        assertEquals(1, count(doc, "//P"));
        assertEquals(1, count(doc, "//SPAN"));
        assertEquals(1, count(doc, "//DIV/P"));
        assertEquals(1, count(doc, "//P/SPAN"));
        assertTrue(firstText(doc, "//SPAN").contains("More"));
    }

    @Test
    public void pUnclosedInsideDivClosedByDivEnd() throws Exception {
        // characterization: <p> without a closing tag is swallowed when the ancestor </div> arrives
        final Document doc = parse("<div><p>a</div>");
        assertEquals(1, count(doc, "//DIV"));
        assertEquals(1, count(doc, "//P"));
        assertEquals(1, count(doc, "//DIV/P"));
        assertEquals("a", firstText(doc, "//P").trim());
    }

    @Test
    public void inlineMultipleUnclosedEofReverseOrder() throws Exception {
        // characterization: nested unclosed inline elements close in reverse (LIFO) order at EOF
        final Document doc = parse("<i>a<b>b");
        assertEquals(1, count(doc, "//I"));
        assertEquals(1, count(doc, "//B"));
        assertEquals(1, count(doc, "//I/B"));
        assertEquals("b", firstText(doc, "//B").trim());
    }

    @Test
    public void unclosedListItemsNestInsteadOfSibling() throws Exception {
        // characterization: consecutive unclosed <li> imply-close each other (HTML5), becoming
        // siblings under UL rather than nesting.
        final Document doc = parse("<ul><li>a<li>b");
        assertEquals(2, count(doc, "//LI"));
        assertEquals(0, count(doc, "//LI/LI"));
    }

    @Test
    public void unclosedTitleInHead() throws Exception {
        final Document doc = parse("<title>abc");
        assertEquals(1, count(doc, "//TITLE"));
        assertEquals("abc", firstText(doc, "//TITLE").trim());
    }

    @Test
    public void unclosedAnchorWithAttribute() throws Exception {
        final Document doc = parse("<a href=\"x\">link");
        final Element a = first(doc, "//A");
        assertNotNull(a);
        assertEquals("x", a.getAttribute("href"));
        assertEquals("link", firstText(doc, "//A").trim());
    }

    @Test
    public void deepUnclosedSameElementNests() throws Exception {
        // characterization: same-named unclosed elements keep nesting (no implicit close)
        final Document doc = parse("<div><div><div>deep");
        assertEquals(3, count(doc, "//DIV"));
        assertEquals(1, count(doc, "//DIV/DIV/DIV"));
        assertTrue(firstText(doc, "//DIV/DIV/DIV").contains("deep"));
    }

    @Test
    public void tableUnclosedElementsNestAndDoNotCloseAtEof() throws Exception {
        // characterization: unclosed table structure closes at EOF, preserving nesting; no implicit tbody
        final Document doc = parse("<table><tr><td>cell");
        assertEquals(1, count(doc, "//TABLE"));
        assertEquals(1, count(doc, "//TR"));
        assertEquals(1, count(doc, "//TD"));
        assertEquals(0, count(doc, "//TBODY"));
        assertEquals(1, count(doc, "//TABLE/TR/TD"));
        assertEquals("cell", firstText(doc, "//TD").trim());
    }

    @Test
    public void saxEndEventFiresAtEofForUnclosedElement() throws Exception {
        final List<String> events = saxEvents("<b>x");
        assertTrue(events.contains("end:B"));
        // characterization: the implicit HTML root and the synthesized BODY are auto-closed at EOF
        // too, after B
        assertEquals(List.of("start:HTML", "start:BODY", "start:B", "end:B", "end:BODY", "end:HTML"),
                events.stream().filter(e -> !e.startsWith("chars:")).toList());
    }

    @Test
    public void tripleUnclosedSameInlineElementNests() throws Exception {
        final Document doc = parse("<span>a<span>b<span>c");
        assertEquals(3, count(doc, "//SPAN"));
        assertEquals(1, count(doc, "//SPAN/SPAN/SPAN"));
        assertTrue(firstText(doc, "//SPAN/SPAN/SPAN").contains("c"));
    }

    @Test
    public void unclosedListItemsClosedByAncestorEnd() throws Exception {
        // characterization: an explicit </ul> auto-closes any still-open <li> elements above it
        final Document doc = parse("<ul><li>a<li>b<li>c</ul>");
        assertEquals(1, count(doc, "//UL"));
        assertEquals(3, count(doc, "//LI"));
        // characterization: unclosed LIs imply-close each other (HTML5), becoming UL's direct children
        assertEquals(3, count(doc, "//UL/LI"));
        assertEquals(0, count(doc, "//UL/LI/LI"));
        assertEquals(0, count(doc, "//UL/LI/LI/LI"));
    }

    @Test
    public void multiLevelUnclosedClosedBySingleAncestorEndTag() throws Exception {
        // characterization: a single </div> auto-closes every unclosed descendant in one shot
        final Document doc = parse("<div><p>Text<span>More</div>");
        assertEquals(1, count(doc, "//DIV"));
        assertEquals(1, count(doc, "//P"));
        assertEquals(1, count(doc, "//SPAN"));
        assertEquals(1, count(doc, "//DIV/P/SPAN"));
    }

    @Test
    public void blockAndInlineMixedUnclosedNestsThreeDeep() throws Exception {
        final Document doc = parse("<blockquote><p>a<b>b");
        assertEquals(1, count(doc, "//BLOCKQUOTE"));
        assertEquals(1, count(doc, "//P"));
        assertEquals(1, count(doc, "//B"));
        assertEquals(1, count(doc, "//BLOCKQUOTE/P/B"));
        assertEquals("b", firstText(doc, "//B").trim());
    }

    @Test
    public void unclosedTitleUnderExplicitHead() throws Exception {
        final Document doc = parse("<html><head><title>t");
        assertEquals(1, count(doc, "//HEAD"));
        assertEquals(1, count(doc, "//TITLE"));
        assertEquals(1, count(doc, "//HEAD/TITLE"));
        assertEquals("t", firstText(doc, "//TITLE").trim());
    }

    @Test
    public void saxEventOrderForUnclosedNestedIsLifoAtEof() throws Exception {
        final List<String> events = saxEvents("<div><p>a<span>b");
        // characterization: end events fire in reverse-open order once EOF is reached
        final int endDiv = events.lastIndexOf("end:DIV");
        final int endP = events.lastIndexOf("end:P");
        final int endSpan = events.lastIndexOf("end:SPAN");
        assertTrue(endSpan < endP, "SPAN should close before P");
        assertTrue(endP < endDiv, "P should close before DIV");
    }

    @Test
    public void attributesArePreservedAcrossAutoCloseAtEof() throws Exception {
        final Document doc = parse("<div id=\"d1\"><p class=\"c1\">Text");
        final Element div = first(doc, "//DIV");
        final Element p = first(doc, "//P");
        assertNotNull(div);
        assertNotNull(p);
        assertEquals("d1", div.getAttribute("id"));
        assertEquals("c1", p.getAttribute("class"));
    }

    @Test
    public void unclosedDtDdNestRatherThanSibling() throws Exception {
        // characterization: <dd> implies the end of <dt> (HTML5), so DD becomes a sibling of DT rather
        // than nesting inside it.
        final Document doc = parse("<dl><dt>a<dd>b");
        assertEquals(1, count(doc, "//DL"));
        assertEquals(1, count(doc, "//DT"));
        assertEquals(1, count(doc, "//DD"));
        assertEquals(0, count(doc, "//DT/DD"));
    }

    @Test
    public void unclosedFormattingElementInsideUnclosedBlock() throws Exception {
        final Document doc = parse("<div><em>text");
        assertEquals(1, count(doc, "//DIV"));
        assertEquals(1, count(doc, "//EM"));
        assertEquals(1, count(doc, "//DIV/EM"));
    }

    @Test
    public void unclosedTableRowWithoutCellClosesAtEof() throws Exception {
        final Document doc = parse("<table><tr>");
        assertEquals(1, count(doc, "//TABLE"));
        assertEquals(1, count(doc, "//TR"));
        assertEquals(1, count(doc, "//TABLE/TR"));
        assertEquals(0, count(doc, "//TD"));
    }

    @Test
    public void unclosedNestedTableStructurePreservesAllLevels() throws Exception {
        final Document doc = parse("<table><tr><td><table><tr><td>inner");
        assertEquals(2, count(doc, "//TABLE"));
        assertEquals(2, count(doc, "//TR"));
        assertEquals(2, count(doc, "//TD"));
        assertEquals(1, count(doc, "//TABLE/TR/TD/TABLE/TR/TD"));
        assertTrue(firstText(doc, "//TABLE/TR/TD/TABLE/TR/TD").contains("inner"));
    }

    @Test
    public void unclosedHeadingElementClosedAtEof() throws Exception {
        final Document doc = parse("<h1>Heading");
        assertEquals(1, count(doc, "//H1"));
        assertEquals("Heading", firstText(doc, "//H1").trim());
    }
}
