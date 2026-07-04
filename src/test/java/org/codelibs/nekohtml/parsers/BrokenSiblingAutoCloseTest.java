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
 * Characterization tests for HTML5-style implied sibling auto-close (category D).
 *
 * <p>
 * {@code HTMLTagBalancerFilter} consults an {@code IMPLIED_CLOSE} table so that starting a tag
 * implicitly closes an open sibling of the same/related kind (LI closes LI, P closes P, DD/DT
 * close each other, TD/TH, TR, OPTION, table sections, and a block container such as DIV closes
 * an open P). Consecutive same-kind (or paragraph-vs-block) tags therefore become SIBLINGS, not
 * nested. Headings still nest (H1..H6 only imply closing P), which the two heading tests below
 * still confirm.
 * </p>
 */
public class BrokenSiblingAutoCloseTest {

    @Test
    public void consecutiveParagraphsNestInsteadOfSibling() throws Exception {
        // characterization: <p>a<p>b - the second <p> implies the end of the first (HTML5), so the
        // two P become siblings rather than nested.
        final Document doc = parse("<p>a<p>b");
        assertEquals(2, count(doc, "//P"));
        assertEquals(0, count(doc, "//P/P"));
    }

    @Test
    public void tripleConsecutiveParagraphsNestThreeDeep() throws Exception {
        // characterization: each new <p> closes the previous open <p> (HTML5), forming three sibling
        // P rather than a P > P > P chain.
        final Document doc = parse("<p>a<p>b<p>c");
        assertEquals(3, count(doc, "//P"));
        assertEquals(0, count(doc, "//P/P"));
        assertEquals(0, count(doc, "//P/P/P"));
    }

    @Test
    public void consecutiveListItemsNestInsideUl() throws Exception {
        // characterization: <li> closes a previous open <li> (HTML5), so the two LI become siblings
        // under UL rather than nested.
        final Document doc = parse("<ul><li>x<li>y</ul>");
        assertEquals(1, count(doc, "//UL"));
        assertEquals(2, count(doc, "//LI"));
        assertEquals(0, count(doc, "//LI/LI"));
    }

    @Test
    public void consecutiveListItemsNestWithoutUlWrapper() throws Exception {
        // characterization: the implied close applies even without a <ul> ancestor, so the two LI
        // become siblings rather than nested.
        final Document doc = parse("<li>a<li>b");
        assertEquals(2, count(doc, "//LI"));
        assertEquals(0, count(doc, "//LI/LI"));
    }

    @Test
    public void tripleListItemsNestThreeDeep() throws Exception {
        // characterization: three consecutive <li> become three siblings under OL (HTML5 implied
        // close), not a chain LI > LI > LI.
        final Document doc = parse("<ol><li>1<li>2<li>3</ol>");
        assertEquals(3, count(doc, "//LI"));
        assertEquals(0, count(doc, "//LI/LI"));
        assertEquals(0, count(doc, "//LI/LI/LI"));
    }

    @Test
    public void dtFollowedByDdNests() throws Exception {
        // characterization: <dd> after an open <dt> closes the DT first (HTML5), so DT and DD become
        // siblings rather than DD nesting inside DT.
        final Document doc = parse("<dl><dt>a<dd>b</dl>");
        assertEquals(1, count(doc, "//DL"));
        assertEquals(1, count(doc, "//DT"));
        assertEquals(1, count(doc, "//DD"));
        assertEquals(0, count(doc, "//DT/DD"));
    }

    @Test
    public void consecutiveDtElementsNest() throws Exception {
        // characterization: two consecutive <dt> become siblings under <dl> (HTML5 implied close),
        // not nested.
        final Document doc = parse("<dl><dt>a<dt>b</dl>");
        assertEquals(2, count(doc, "//DT"));
        assertEquals(0, count(doc, "//DT/DT"));
    }

    @Test
    public void consecutiveOptionsNestInsideSelect() throws Exception {
        // characterization: a second <option> implies the end of the first (HTML5), so the two
        // OPTION become siblings under SELECT rather than nested.
        final Document doc = parse("<select><option>a<option>b</select>");
        assertEquals(1, count(doc, "//SELECT"));
        assertEquals(2, count(doc, "//OPTION"));
        assertEquals(0, count(doc, "//OPTION/OPTION"));
    }

    @Test
    public void consecutiveOptionsNestWithoutSelectWrapper() throws Exception {
        // characterization: the implied close applies even without an enclosing <select>, so the two
        // OPTION become siblings.
        final Document doc = parse("<option>a<option>b");
        assertEquals(2, count(doc, "//OPTION"));
        assertEquals(0, count(doc, "//OPTION/OPTION"));
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
        // characterization: <tr><tr> - a second <tr> implies the end of the first (HTML5), so the two
        // TR become siblings even with no table context.
        final Document doc = parse("<tr><tr>");
        assertEquals(2, count(doc, "//TR"));
        assertEquals(0, count(doc, "//TR/TR"));
    }

    @Test
    public void consecutiveTdElementsNest() throws Exception {
        // characterization: a second <td> implies the end of the first (HTML5), so the two TD become
        // siblings rather than nested.
        final Document doc = parse("<td>a<td>b");
        assertEquals(2, count(doc, "//TD"));
        assertEquals(0, count(doc, "//TD/TD"));
    }

    @Test
    public void paragraphContainingBlockDivNests() throws Exception {
        // characterization: <div> (a block element) implicitly closes an open <p> (HTML5), so DIV
        // becomes a sibling of P rather than nesting inside the still-open P.
        final Document doc = parse("<p>a<div>b");
        assertEquals(1, count(doc, "//P"));
        assertEquals(1, count(doc, "//DIV"));
        assertEquals(0, count(doc, "//P/DIV"));
    }

    @Test
    public void listItemsAtEofCloseInLifoOrderPreservingNesting() throws Exception {
        // characterization: the second <li> closes the first (HTML5 implied close), so both LI are
        // direct children of UL (siblings) and are closed at EOF; no nested LI/LI remains.
        final Document doc = parse("<ul><li>x<li>y");
        assertEquals(1, count(doc, "//UL"));
        assertEquals(2, count(doc, "//LI"));
        assertEquals(2, count(doc, "//UL/LI"));
        assertEquals(0, count(doc, "//UL/LI/LI"));
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
        // characterization: the SAX event stream shows the first P being closed (end:P) BEFORE the
        // second start:P, proving the second <p> closes the first (sibling model), not nested.
        final List<String> events = saxEvents("<p>a<p>b");
        final int firstStart = events.indexOf("start:P");
        final int secondStart = events.subList(firstStart + 1, events.size()).indexOf("start:P") + firstStart + 1;
        final int firstEnd = events.indexOf("end:P");
        assertTrue(firstStart >= 0 && secondStart > firstStart);
        assertTrue(firstEnd > firstStart && firstEnd < secondStart);
    }

    @Test
    public void consecutiveDdElementsNestWithoutDt() throws Exception {
        // characterization: consecutive <dd> elements close each other (HTML5 implied close) and
        // become siblings even when there is no preceding <dt>.
        final Document doc = parse("<dd>a<dd>b");
        assertEquals(2, count(doc, "//DD"));
        assertEquals(0, count(doc, "//DD/DD"));
    }
}
