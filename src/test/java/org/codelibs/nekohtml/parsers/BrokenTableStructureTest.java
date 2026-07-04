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
 * Characterization tests for broken table structures (category F).
 *
 * <p>
 * {@code HTMLTagBalancerFilter} has no table-specific logic at all: {@code TABLE},
 * {@code TBODY}, {@code THEAD}, {@code TFOOT}, {@code TR}, {@code TD}, {@code CAPTION} and
 * {@code COLGROUP} are handled exactly like any other element on the single generic element
 * stack. In particular there is no implicit {@code <tbody>} insertion, no requirement that
 * {@code TR}/{@code TD} appear inside a table context, and no auto-closing between
 * table-structure siblings (the same "no sibling auto-close" quirk as category D, applied
 * here to table elements specifically). This class focuses on those gaps; well-formed table
 * coverage already exists in {@code ComplexTableStructuresTest}.
 * </p>
 */
public class BrokenTableStructureTest {

    @Test
    public void tableDoesNotInsertImplicitTbody() throws Exception {
        // characterization: current behavior (non-standard) - no implicit <tbody> is
        // inserted; TR is a direct child of TABLE.
        final Document doc = parse("<table><tr><td>Cell</td></tr></table>");
        assertEquals(1, count(doc, "//TABLE"));
        assertEquals(1, count(doc, "//TR"));
        assertEquals(1, count(doc, "//TD"));
        assertEquals(0, count(doc, "//TBODY"));
        assertEquals(1, count(doc, "//TABLE/TR"));
        assertEquals("Cell", firstText(doc, "//TD"));
    }

    @Test
    public void tdWithoutTrStaysDirectlyInTable() throws Exception {
        // characterization: current behavior (non-standard) - TD is not forced into a
        // TR/TBODY context; it becomes a direct child of TABLE.
        final Document doc = parse("<table><td>x</td></table>");
        assertEquals(1, count(doc, "//TABLE"));
        assertEquals(0, count(doc, "//TR"));
        assertEquals(1, count(doc, "//TD"));
        assertEquals(1, count(doc, "//TABLE/TD"));
    }

    @Test
    public void unclosedTrIsAutoClosedWhenTableCloses() throws Exception {
        // characterization: TR left unclosed is auto-closed as part of the generic
        // "close everything above the matched tag" logic when </table> is reached -
        // this is ordinary stack unwinding, not table-specific behavior.
        final Document doc = parse("<table><tr><td>a</td></table>");
        assertEquals(1, count(doc, "//TABLE"));
        assertEquals(1, count(doc, "//TR"));
        assertEquals(1, count(doc, "//TABLE/TR"));
        assertEquals(1, count(doc, "//TR/TD"));
    }

    @Test
    public void tableWithBareTextDirectlyUnderTable() throws Exception {
        // characterization: current behavior (non-standard) - free text directly
        // inside <table> is kept as a text node child of TABLE (not relocated or
        // dropped as browsers do with foster parenting).
        final Document doc = parse("<table>text</table>");
        assertEquals(1, count(doc, "//TABLE"));
        assertTrue(firstText(doc, "//TABLE").contains("text"));
    }

    @Test
    public void nestedTablesBothPresent() throws Exception {
        final Document doc = parse("<table><tr><td><table><tr><td>inner</td></tr></table></td></tr></table>");
        assertEquals(2, count(doc, "//TABLE"));
        assertEquals(1, count(doc, "//TABLE//TABLE"));
        assertEquals("inner", firstText(doc, "//TABLE//TABLE//TD"));
    }

    @Test
    public void threeLevelsOfNestedTablesAllPresent() throws Exception {
        final Document doc =
                parse("<table><tr><td><table><tr><td><table><tr><td>Deep</td></tr></table></td></tr></table></td></tr></table>");
        assertEquals(3, count(doc, "//TABLE"));
        assertEquals(2, count(doc, "//TABLE//TABLE"));
        assertEquals(1, count(doc, "//TABLE//TABLE//TABLE"));
        assertEquals("Deep", firstText(doc, "//TABLE//TABLE//TABLE//TD"));
    }

    @Test
    public void tdStandaloneOutsideAnyTableIsStillParsed() throws Exception {
        // characterization: current behavior (non-standard) - TD has no requirement
        // of a TABLE ancestor; it is parsed as a normal element under the implicit
        // HTML root.
        final Document doc = parse("<td>x</td>");
        assertEquals(1, count(doc, "//TD"));
        assertEquals(0, count(doc, "//TABLE"));
        assertEquals("x", firstText(doc, "//TD"));
    }

    @Test
    public void trStandaloneOutsideAnyTableIsStillParsed() throws Exception {
        final Document doc = parse("<tr><td>x</td></tr>");
        assertEquals(1, count(doc, "//TR"));
        assertEquals(0, count(doc, "//TABLE"));
        assertEquals(1, count(doc, "//TR/TD"));
    }

    @Test
    public void captionBeforeTableStaysOutsideTable() throws Exception {
        // characterization: current behavior (non-standard) - a CAPTION appearing
        // before <table> is not relocated inside it; it remains a preceding sibling.
        final Document doc = parse("<caption>Before</caption><table><tr><td>x</td></tr></table>");
        assertEquals(1, count(doc, "//CAPTION"));
        assertEquals(0, count(doc, "//TABLE/CAPTION"));
        assertEquals(0, count(doc, "//TABLE//CAPTION"));
    }

    @Test
    public void captionInsideTableIsChildOfTable() throws Exception {
        final Document doc = parse("<table><caption>Cap</caption><tr><td>x</td></tr></table>");
        assertEquals(1, count(doc, "//TABLE/CAPTION"));
        assertEquals("Cap", firstText(doc, "//CAPTION"));
    }

    @Test
    public void multipleCaptionsAreBothRetained() throws Exception {
        // characterization: current behavior (non-standard) - a second <caption> is
        // not rejected or merged; both are kept as children of TABLE (no sibling
        // auto-close applies here either, but since CAPTION is closed explicitly each
        // time, the two end up as true siblings rather than nested).
        final Document doc = parse("<table><caption>C1</caption><caption>C2</caption><tr><td>x</td></tr></table>");
        assertEquals(2, count(doc, "//TABLE/CAPTION"));
        assertEquals(0, count(doc, "//CAPTION/CAPTION"));
    }

    @Test
    public void explicitEmptyTbodyHasNoChildren() throws Exception {
        final Document doc = parse("<table><tbody></tbody></table>");
        assertEquals(1, count(doc, "//TBODY"));
        assertEquals(0, count(doc, "//TBODY/*"));
    }

    @Test
    public void consecutiveTdWithoutClosingNestInsideTr() throws Exception {
        // characterization: current behavior (non-standard) - the same "no sibling
        // auto-close" quirk (category D) applies inside a table row: a second <td>
        // nests inside the still-open first <td> instead of becoming its sibling.
        final Document doc = parse("<table><tr><td>a<td>b</tr></table>");
        assertEquals(1, count(doc, "//TABLE"));
        assertEquals(1, count(doc, "//TR"));
        assertEquals(2, count(doc, "//TD"));
        assertEquals(1, count(doc, "//TD/TD"));
    }

    @Test
    public void tfootBeforeTbodyOrderIsPreservedNotReordered() throws Exception {
        // characterization: current behavior (non-standard) - TFOOT appearing before
        // TBODY in the source is not reordered to the canonical thead/tbody/tfoot
        // order; document order is preserved.
        final Document doc = parse("<table><tfoot><tr><td>f</td></tr></tfoot><tbody><tr><td>b</td></tr></tbody></table>");
        assertEquals(1, count(doc, "//TFOOT"));
        assertEquals(1, count(doc, "//TBODY"));
        final List<String> children =
                saxStartElements("<table><tfoot><tr><td>f</td></tr></tfoot><tbody><tr><td>b</td></tr></tbody></table>");
        assertTrue(children.indexOf("TFOOT") < children.indexOf("TBODY"));
    }

    @Test
    public void trOutsideAnyRowGroupIsNotWrappedInImplicitTbody() throws Exception {
        // characterization: current behavior (non-standard) - a TR sibling of an
        // explicit THEAD is left as a direct child of TABLE; it is not swept into an
        // implicit TBODY the way a browser would.
        final Document doc = parse("<table><thead><tr><th>h</th></tr></thead><tr><td>d</td></tr></table>");
        assertEquals(0, count(doc, "//TBODY"));
        assertEquals(2, count(doc, "//TR"));
        assertEquals(1, count(doc, "//TABLE/TR"));
    }

    @Test
    public void unclosedTbodyStaysOpenAcrossFollowingTheadNoAutoClose() throws Exception {
        // characterization: current behavior (non-standard) - with no table-specific
        // logic, an unclosed <tbody> is never implicitly closed by a following
        // <thead>; THEAD instead nests inside the still-open TBODY.
        final Document doc = parse("<table><tbody><tr><td>a</td></tr><thead><tr><th>h</th></tr></thead></table>");
        assertEquals(1, count(doc, "//TBODY"));
        assertEquals(1, count(doc, "//THEAD"));
        assertEquals(1, count(doc, "//TBODY/THEAD"));
        assertEquals(0, count(doc, "//TABLE/THEAD"));
        assertEquals(1, count(doc, "//TBODY/TR"));
    }

    @Test
    public void colgroupWithVoidColChildrenHasNoGrandchildren() throws Exception {
        final Document doc = parse("<table><colgroup><col><col></colgroup><tr><td>x</td></tr></table>");
        assertEquals(1, count(doc, "//COLGROUP"));
        assertEquals(2, count(doc, "//COLGROUP/COL"));
        assertEquals(0, count(doc, "//COL/*"));
    }

    @Test
    public void colgroupStandaloneOutsideTableIsStillParsed() throws Exception {
        final Document doc = parse("<colgroup><col></colgroup>");
        assertEquals(0, count(doc, "//TABLE"));
        assertEquals(1, count(doc, "//COLGROUP"));
        assertEquals(1, count(doc, "//COLGROUP/COL"));
    }

    @Test
    public void saxEventsShowNoSyntheticTbodyEvents() throws Exception {
        // characterization: SAX-level proof (not just the DOM projection) that no
        // synthetic start:TBODY/end:TBODY events are ever emitted.
        final List<String> events = saxStartElements("<table><tr><td>Cell</td></tr></table>");
        assertFalse(events.contains("TBODY"));
        assertEquals(List.of("HTML", "TABLE", "TR", "TD"), events);
    }
}
