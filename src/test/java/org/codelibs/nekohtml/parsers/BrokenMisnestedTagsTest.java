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
 * Category B: misnested / mismatched tags.
 *
 * <p>
 * These tests lock in the current (characterization) behavior of the Adoption Agency
 * Algorithm (AAA) implementation for overlapping formatting elements, and the plain
 * auto-close recovery for overlapping non-formatting (block) elements. The AAA final tree
 * shape is intentionally asserted conservatively (presence/containment) since the exact
 * reconstruction is an implementation detail; non-formatting mismatches are asserted more
 * strictly where the current behavior is stable.
 * </p>
 */
public class BrokenMisnestedTagsTest {

    @Test
    public void misnestedFormattingKeepsBothElements() throws Exception {
        // characterization: <b><i>x</b></i> runs through AAA; both B and I survive in the tree
        final Document doc = parse("<b><i>text</b></i>");
        assertTrue(count(doc, "//B") >= 1);
        assertTrue(count(doc, "//I") >= 1);
        assertTrue(firstText(doc, "//HTML").contains("text"));
    }

    @Test
    public void mismatchedBlockRecovers() throws Exception {
        // characterization: non-formatting mismatch </div></span> keeps DIV, drops the stray SPAN close
        final Document doc = parse("<div><span></div></span>");
        assertEquals(1, count(doc, "//DIV"));
        assertEquals(1, count(doc, "//SPAN"));
        assertEquals(1, count(doc, "//DIV/SPAN"));
    }

    @Test
    public void crossingOverlapKeepsBothFormattingElements() throws Exception {
        // characterization: <b><u>x</b>y</u> -- overlap resolved by AAA, both survive
        final Document doc = parse("<b><u>x</b>y</u>");
        assertTrue(count(doc, "//B") >= 1);
        assertTrue(count(doc, "//U") >= 1);
        assertTrue(firstText(doc, "//HTML").contains("x"));
        assertTrue(firstText(doc, "//HTML").contains("y"));
    }

    @Test
    public void emStrongOverlapKeepsBothElements() throws Exception {
        final Document doc = parse("<em><strong>a</em>b</strong>");
        assertTrue(count(doc, "//EM") >= 1);
        assertTrue(count(doc, "//STRONG") >= 1);
        assertTrue(firstText(doc, "//HTML").contains("a"));
        assertTrue(firstText(doc, "//HTML").contains("b"));
    }

    @Test
    public void blockEndTagCrossingInlineFormattingKeepsBoth() throws Exception {
        // characterization: </p> closing while <b> is still open triggers standard auto-close,
        // not AAA (P is not a formatting element), so B ends up closed inside P.
        final Document doc = parse("<p><b>bold</p></b>");
        assertEquals(1, count(doc, "//P"));
        assertTrue(count(doc, "//B") >= 1);
        assertTrue(firstText(doc, "//P").contains("bold"));
    }

    @Test
    public void anchorFormattingCrossKeepsBothElements() throws Exception {
        // characterization: A is treated as a formatting element for AAA purposes
        final Document doc = parse("<a><b>x</a>y</b>");
        assertTrue(count(doc, "//A") >= 1);
        assertTrue(count(doc, "//B") >= 1);
        assertTrue(firstText(doc, "//HTML").contains("x"));
        assertTrue(firstText(doc, "//HTML").contains("y"));
    }

    @Test
    public void fontColorAttributeSurvivesMisnesting() throws Exception {
        final Document doc = parse("<font color=\"red\"><b>x</font>y</b>");
        final Element font = first(doc, "//FONT");
        assertNotNull(font);
        assertEquals("red", font.getAttribute("color"));
        assertTrue(count(doc, "//B") >= 1);
    }

    @Test
    public void misnestedAndUnclosedCombination() throws Exception {
        // characterization: misnesting plus an unclosed DIV -- all three elements still appear
        final Document doc = parse("<div><b><i>deep</div>");
        assertEquals(1, count(doc, "//DIV"));
        assertTrue(count(doc, "//B") >= 1);
        assertTrue(count(doc, "//I") >= 1);
        assertTrue(firstText(doc, "//DIV").contains("deep"));
    }

    @Test
    public void sameFormattingElementNestedProperlyIsNotAAA() throws Exception {
        // contrast case: properly-nested same-name formatting elements are NOT touched by AAA
        final Document doc = parse("<i><i><i>x</i></i></i>");
        assertEquals(3, count(doc, "//I"));
        assertEquals(1, count(doc, "//I/I/I"));
        assertEquals("x", firstText(doc, "//I/I/I").trim());
    }

    @Test
    public void saxEventsForMisnestedFormattingAreLocked() throws Exception {
        final List<String> events = saxEvents("<b><i>x</b></i>");
        // characterization: exact SAX event sequence for the current AAA implementation
        assertEquals(List.of("start:HTML", "start:B", "start:I", "chars:x", "end:I", "end:B", "start:I", "end:I", "end:HTML"), events
                .stream().filter(e -> !e.equals("chars:\n")).toList());
    }

    @Test
    public void divSpanCrossMismatchWithTextKeepsDiv() throws Exception {
        final Document doc = parse("<div>a<span>b</div>c</span>");
        assertEquals(1, count(doc, "//DIV"));
        assertEquals(1, count(doc, "//SPAN"));
        assertTrue(firstText(doc, "//DIV").contains("a"));
        assertTrue(firstText(doc, "//DIV").contains("b"));
    }

    @Test
    public void listItemCrossingFormattingElementKeepsBoth() throws Exception {
        final Document doc = parse("<ul><li><b>x<li>y</b></ul>");
        assertEquals(1, count(doc, "//UL"));
        assertEquals(2, count(doc, "//LI"));
        assertTrue(count(doc, "//B") >= 1);
    }

    @Test
    public void headingCrossingFormattingElementKeepsBoth() throws Exception {
        final Document doc = parse("<h1><b>Head</h1>Text</b>");
        assertEquals(1, count(doc, "//H1"));
        assertTrue(count(doc, "//B") >= 1);
        assertTrue(firstText(doc, "//H1").contains("Head"));
    }

    @Test
    public void blockquoteCrossingFormattingElementKeepsBoth() throws Exception {
        final Document doc = parse("<blockquote><i>Quote</blockquote>End</i>");
        assertEquals(1, count(doc, "//BLOCKQUOTE"));
        assertTrue(count(doc, "//I") >= 1);
        assertTrue(firstText(doc, "//BLOCKQUOTE").contains("Quote"));
    }

    @Test
    public void strikeAndSCrossingKeepsBoth() throws Exception {
        final Document doc = parse("<s><strike>x</s>y</strike>");
        assertTrue(count(doc, "//S") >= 1);
        assertTrue(count(doc, "//STRIKE") >= 1);
        assertTrue(firstText(doc, "//HTML").contains("x"));
        assertTrue(firstText(doc, "//HTML").contains("y"));
    }

    @Test
    public void nobrCrossingBoldKeepsBoth() throws Exception {
        final Document doc = parse("<nobr><b>x</nobr>y</b>");
        assertTrue(count(doc, "//NOBR") >= 1);
        assertTrue(count(doc, "//B") >= 1);
    }

    @Test
    public void ttCodeCrossingKeepsBoth() throws Exception {
        final Document doc = parse("<tt><code>x</tt>y</code>");
        assertTrue(count(doc, "//TT") >= 1);
        assertTrue(count(doc, "//CODE") >= 1);
    }

    @Test
    public void smallBigCrossingKeepsBoth() throws Exception {
        final Document doc = parse("<small><big>x</small>y</big>");
        assertTrue(count(doc, "//SMALL") >= 1);
        assertTrue(count(doc, "//BIG") >= 1);
    }

    @Test
    public void threeLevelMisnestingKeepsAllThreeElements() throws Exception {
        final Document doc = parse("<b><i><u>x</b>y</u>z</i>");
        assertTrue(count(doc, "//B") >= 1);
        assertTrue(count(doc, "//I") >= 1);
        assertTrue(count(doc, "//U") >= 1);
        final String bodyText = firstText(doc, "//HTML");
        assertTrue(bodyText.contains("x"));
        assertTrue(bodyText.contains("y"));
        assertTrue(bodyText.contains("z"));
    }

    @Test
    public void saxStartElementsOrderForOverlapIsLocked() throws Exception {
        // characterization: no explicit <body> is given, and BODY is NOT auto-created
        final List<String> starts = saxStartElements("<div><span></div></span>");
        assertEquals(List.of("HTML", "DIV", "SPAN"), starts);
    }
}
