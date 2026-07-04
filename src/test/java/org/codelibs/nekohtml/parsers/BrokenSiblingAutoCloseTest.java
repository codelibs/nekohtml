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

/**
 * Characterization tests for the "no sibling auto-close" quirk (category D).
 *
 * <p>
 * {@code HTMLTagBalancerFilter} tracks open elements on a single generic stack and only
 * closes elements by exact tag-name match (see {@code endElement}). The element-metadata
 * tables ({@code HEAD_ELEMENTS}, {@code GENERIC_CONTAINERS}) are never consulted, so there is
 * no HTML5-style "starting this tag implicitly closes an open sibling of the same/related
 * kind" behavior. Consecutive same-kind (or paragraph/heading-vs-block) tags therefore NEST
 * instead of becoming siblings. This is a strong, intentional current quirk of NekoHTML 3.x.
 * </p>
 */
public class BrokenSiblingAutoCloseTest {

    @Test
    public void consecutiveParagraphsNestInsteadOfSibling() throws Exception {
        // characterization: current behavior (non-standard) - <p>a<p>b nests P inside P
        // rather than closing the first P when the second starts.
        final Document doc = parse("<p>a<p>b");
        assertEquals(2, count(doc, "//P"));
        assertEquals(1, count(doc, "//P/P"));
    }

    @Test
    public void tripleConsecutiveParagraphsNestThreeDeep() throws Exception {
        // characterization: current behavior (non-standard) - each new P nests one level
        // deeper than the previous, forming a P > P > P chain.
        final Document doc = parse("<p>a<p>b<p>c");
        assertEquals(3, count(doc, "//P"));
        assertEquals(2, count(doc, "//P/P"));
        assertEquals(1, count(doc, "//P/P/P"));
    }

    @Test
    public void consecutiveListItemsNestInsideUl() throws Exception {
        // characterization: current behavior (non-standard) - <li> does not close a
        // previous open <li>; the second LI nests inside the first.
        final Document doc = parse("<ul><li>x<li>y</ul>");
        assertEquals(1, count(doc, "//UL"));
        assertEquals(2, count(doc, "//LI"));
        assertEquals(1, count(doc, "//LI/LI"));
    }

    @Test
    public void consecutiveListItemsNestWithoutUlWrapper() throws Exception {
        // characterization: current behavior (non-standard) - nesting happens even
        // without a <ul> ancestor since there is no list-context tracking at all.
        final Document doc = parse("<li>a<li>b");
        assertEquals(2, count(doc, "//LI"));
        assertEquals(1, count(doc, "//LI/LI"));
    }

    @Test
    public void tripleListItemsNestThreeDeep() throws Exception {
        // characterization: current behavior (non-standard) - three consecutive <li>
        // form a chain LI > LI > LI.
        final Document doc = parse("<ol><li>1<li>2<li>3</ol>");
        assertEquals(3, count(doc, "//LI"));
        assertEquals(2, count(doc, "//LI/LI"));
        assertEquals(1, count(doc, "//LI/LI/LI"));
    }

    @Test
    public void dtFollowedByDdNests() throws Exception {
        // characterization: current behavior (non-standard) - <dd> after an open <dt>
        // nests DD inside DT instead of closing DT first.
        final Document doc = parse("<dl><dt>a<dd>b</dl>");
        assertEquals(1, count(doc, "//DL"));
        assertEquals(1, count(doc, "//DT"));
        assertEquals(1, count(doc, "//DD"));
        assertEquals(1, count(doc, "//DT/DD"));
    }

    @Test
    public void consecutiveDtElementsNest() throws Exception {
        // characterization: current behavior (non-standard) - two consecutive <dt>
        // nest rather than becoming siblings under <dl>.
        final Document doc = parse("<dl><dt>a<dt>b</dl>");
        assertEquals(2, count(doc, "//DT"));
        assertEquals(1, count(doc, "//DT/DT"));
    }

    @Test
    public void consecutiveOptionsNestInsideSelect() throws Exception {
        // characterization: current behavior (non-standard) - SELECT/OPTION have no
        // special-cased handling; the second OPTION nests inside the first.
        final Document doc = parse("<select><option>a<option>b</select>");
        assertEquals(1, count(doc, "//SELECT"));
        assertEquals(2, count(doc, "//OPTION"));
        assertEquals(1, count(doc, "//OPTION/OPTION"));
    }

    @Test
    public void consecutiveOptionsNestWithoutSelectWrapper() throws Exception {
        // characterization: current behavior (non-standard) - nesting occurs even
        // without an enclosing <select>.
        final Document doc = parse("<option>a<option>b");
        assertEquals(2, count(doc, "//OPTION"));
        assertEquals(1, count(doc, "//OPTION/OPTION"));
    }

    @Test
    public void headingFollowedByDifferentHeadingNests() throws Exception {
        // characterization: current behavior (non-standard) - <h1> then <h2> nests the
        // H2 inside the open H1 (headings do not auto-close each other).
        final Document doc = parse("<h1>a<h2>b");
        assertEquals(1, count(doc, "//H1"));
        assertEquals(1, count(doc, "//H2"));
        assertEquals(1, count(doc, "//H1/H2"));
    }

    @Test
    public void consecutiveSameHeadingsNest() throws Exception {
        // characterization: current behavior (non-standard) - same-tag consecutive
        // headings also nest, identical to the paragraph case.
        final Document doc = parse("<h1>a<h1>b");
        assertEquals(2, count(doc, "//H1"));
        assertEquals(1, count(doc, "//H1/H1"));
    }

    @Test
    public void trFollowedByTrNestsOutsideTableContext() throws Exception {
        // characterization: current behavior (non-standard) - <tr><tr> with no table
        // context at all still nests the second TR inside the first.
        final Document doc = parse("<tr><tr>");
        assertEquals(2, count(doc, "//TR"));
        assertEquals(1, count(doc, "//TR/TR"));
    }

    @Test
    public void consecutiveTdElementsNest() throws Exception {
        // characterization: current behavior (non-standard) - a second <td> nests
        // inside the still-open first <td> rather than closing it.
        final Document doc = parse("<td>a<td>b");
        assertEquals(2, count(doc, "//TD"));
        assertEquals(1, count(doc, "//TD/TD"));
    }

    @Test
    public void paragraphContainingBlockDivNests() throws Exception {
        // characterization: current behavior (non-standard) - HTML5 requires <div>
        // (a block element) to implicitly close an open <p>; NekoHTML 3.x does not
        // implement this, so DIV nests inside the still-open P.
        final Document doc = parse("<p>a<div>b");
        assertEquals(1, count(doc, "//P"));
        assertEquals(1, count(doc, "//DIV"));
        assertEquals(1, count(doc, "//P/DIV"));
    }

    @Test
    public void listItemsAtEofCloseInLifoOrderPreservingNesting() throws Exception {
        // characterization: current behavior (non-standard) - unclosed nested LIs are
        // closed in LIFO order at EOF, keeping the nested parent/child structure intact.
        final Document doc = parse("<ul><li>x<li>y");
        assertEquals(1, count(doc, "//UL"));
        assertEquals(2, count(doc, "//LI"));
        assertEquals(1, count(doc, "//UL/LI"));
        assertEquals(1, count(doc, "//UL/LI/LI"));
    }

    @Test
    public void siblingParagraphsOnlyFormWhenExplicitlyClosed() throws Exception {
        // characterization: explicitly closing the first <p> before the second starts
        // is the only way to get true P siblings, confirming nesting (not the DOM
        // sibling model) is what happens when the close tag is omitted.
        final Document doc = parse("<p>a</p><p>b</p>");
        assertEquals(2, count(doc, "//P"));
        assertEquals(0, count(doc, "//P/P"));
    }

    @Test
    public void saxEventsForConsecutiveParagraphsShowNestedStartsBeforeAnyEnd() throws Exception {
        // characterization: current behavior (non-standard) - the SAX event stream shows
        // both start:P events happening before either end:P, proving the second P is
        // opened while the first is still open (i.e. nested), not sibling-replaced.
        final List<String> events = saxEvents("<p>a<p>b");
        final int firstStart = events.indexOf("start:P");
        final int secondStart = events.subList(firstStart + 1, events.size()).indexOf("start:P") + firstStart + 1;
        final int firstEnd = events.indexOf("end:P");
        assertTrue(firstStart >= 0 && secondStart > firstStart);
        assertTrue(firstEnd > secondStart);
    }

    @Test
    public void consecutiveDdElementsNestWithoutDt() throws Exception {
        // characterization: current behavior (non-standard) - <dd> elements nest even
        // when there is no preceding <dt>.
        final Document doc = parse("<dd>a<dd>b");
        assertEquals(2, count(doc, "//DD"));
        assertEquals(1, count(doc, "//DD/DD"));
    }
}
