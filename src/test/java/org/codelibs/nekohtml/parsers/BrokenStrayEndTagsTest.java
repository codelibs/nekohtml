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
 * Category C: stray / orphan end tags.
 *
 * <p>
 * These tests lock in the current (characterization) behavior when an end tag has no
 * matching open start tag (or closes an element that isn't the current top of stack). The
 * tag balancer now SUPPRESSES such stray end tags -- it never forwards an {@code endElement}
 * without a prior matching {@code startElement} -- so a stray end tag never produces or removes
 * an element in the resulting DOM and never appears in the SAX event stream.
 * </p>
 */
public class BrokenStrayEndTagsTest {

    @Test
    public void strayEndTagProducesNoElement() throws Exception {
        // characterization: an </b> with no matching <b> is ignored by the DOM layer
        final Document doc = parse("</b>");
        assertEquals(0, count(doc, "//B"));
    }

    @Test
    public void strayEndDivWithTrailingText() throws Exception {
        final Document doc = parse("</div>text");
        assertEquals(0, count(doc, "//DIV"));
        assertTrue(firstText(doc, "//HTML").contains("text"));
    }

    @Test
    public void doubleEndTagIsIgnoredTheSecondTime() throws Exception {
        // characterization: the first </p> closes P normally; the second is a stray extra end tag
        final Document doc = parse("<p>x</p></p>");
        assertEquals(1, count(doc, "//P"));
        assertEquals("x", firstText(doc, "//P").trim());
    }

    @Test
    public void excessiveEndTagsCollapseToOneElement() throws Exception {
        final Document doc = parse("<body>x</body></body></html>");
        assertEquals(1, count(doc, "//BODY"));
        assertTrue(firstText(doc, "//BODY").contains("x"));
    }

    @Test
    public void strayEndTagForVoidBrProducesNoElement() throws Exception {
        // characterization: </br> alone (no preceding <br>) never creates a BR element
        final Document doc = parse("</br>");
        assertEquals(0, count(doc, "//BR"));
    }

    @Test
    public void strayEndTagForVoidImgProducesNoElement() throws Exception {
        final Document doc = parse("</img>");
        assertEquals(0, count(doc, "//IMG"));
    }

    @Test
    public void strayEndTagForVoidHrProducesNoElement() throws Exception {
        final Document doc = parse("</hr>");
        assertEquals(0, count(doc, "//HR"));
    }

    @Test
    public void strayEndTagBetweenTextFragmentsIsDropped() throws Exception {
        final Document doc = parse("text</span>more");
        assertEquals(0, count(doc, "//SPAN"));
        final String text = firstText(doc, "//HTML");
        assertTrue(text.contains("text"));
        assertTrue(text.contains("more"));
    }

    @Test
    public void innerMismatchedEndKeepsParent() throws Exception {
        // characterization: an internal stray </span> is ignored; DIV and its text survive intact
        final Document doc = parse("<div>a</span>b</div>");
        assertEquals(1, count(doc, "//DIV"));
        assertEquals(0, count(doc, "//SPAN"));
        assertTrue(firstText(doc, "//DIV").contains("a"));
        assertTrue(firstText(doc, "//DIV").contains("b"));
    }

    @Test
    public void saxEventsStillReportStrayEndTagEvenThoughDomIgnoresIt() throws Exception {
        // characterization: the tag balancer suppresses a stray end tag (no matching open start) at
        // the SAX level too, so the event stream does NOT record end:B -- the stream stays balanced.
        final List<String> events = saxEvents("</b>");
        assertFalse(events.contains("end:B"));
    }

    @Test
    public void saxEventsForInnerMismatchedEndAreLocked() throws Exception {
        // characterization: BODY is synthesized around the DIV, and the stray </span> (no matching
        // open SPAN) is suppressed, so no end:SPAN appears and the stream stays balanced.
        final List<String> events = saxEvents("<div>a</span>b</div>");
        assertEquals(List.of("start:HTML", "start:BODY", "start:DIV", "chars:a", "chars:b", "end:DIV", "end:BODY", "end:HTML"), events
                .stream().filter(e -> !e.equals("chars:\n")).toList());
    }

    @Test
    public void strayEndTagFollowedByValidElementDoesNotAffectIt() throws Exception {
        final Document doc = parse("</b><p>ok</p>");
        assertEquals(0, count(doc, "//B"));
        assertEquals(1, count(doc, "//P"));
        assertEquals("ok", firstText(doc, "//P").trim());
    }

    @Test
    public void multipleStrayEndTagsInARowProduceNoElements() throws Exception {
        final Document doc = parse("</b></i></u>");
        assertEquals(0, count(doc, "//B"));
        assertEquals(0, count(doc, "//I"));
        assertEquals(0, count(doc, "//U"));
    }

    @Test
    public void strayEndTagForUnknownElementDoesNotCrash() throws Exception {
        final Document doc = parse("</foo>");
        assertNotNull(doc);
        assertEquals(0, count(doc, "//FOO"));
    }

    @Test
    public void strayEndTagInsideValidStructureIsIgnored() throws Exception {
        final Document doc = parse("<div><p>x</p></span></div>");
        assertEquals(1, count(doc, "//DIV"));
        assertEquals(1, count(doc, "//P"));
        assertEquals(0, count(doc, "//SPAN"));
        assertEquals("x", firstText(doc, "//P").trim());
    }

    @Test
    public void voidElementClosesOnStartAndExplicitEndTagIsStrayExtra() throws Exception {
        // characterization: <br> auto-closes at the start tag itself; the following </br> is a
        // separate, unmatched end tag and does not remove or duplicate the BR element.
        final Document doc = parse("<div><br></br></div>");
        assertEquals(1, count(doc, "//BR"));
        assertEquals(0, count(doc, "//BR/*"));
    }

    @Test
    public void saxEventsDoubleEndTagOnlyClosesOnce() throws Exception {
        final List<String> events = saxEvents("<p>x</p></p>");
        final long endPCount = events.stream().filter(e -> e.equals("end:P")).count();
        // characterization: the second </p> has no matching open P (already popped by the first), so
        // it is suppressed and not forwarded; only one balanced end:P is emitted.
        assertEquals(1, endPCount);
    }

    @Test
    public void strayEndTagAtVeryStartOfDocumentStillInitializesHtmlRoot() throws Exception {
        // characterization: even though the stray end tag itself does not trigger HTML root
        // creation, subsequent content does, and the HTML root is still created exactly once.
        final Document doc = parse("</b>ok");
        assertEquals(1, count(doc, "//HTML"));
        assertTrue(firstText(doc, "//HTML").contains("ok"));
    }

    @Test
    public void strayClosingTagForTableCellOutsideTableIsIgnored() throws Exception {
        final Document doc = parse("</td>text");
        assertEquals(0, count(doc, "//TD"));
        assertTrue(firstText(doc, "//HTML").contains("text"));
    }
}
