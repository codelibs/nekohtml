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
 * Characterization tests for void elements and self-closing syntax (category E).
 *
 * <p>
 * {@code SimpleHTMLScanner} immediately emits a matching {@code endElement} right after
 * {@code startElement} for the 13 HTML5 void elements (AREA, BASE, BR, COL, EMBED, HR, IMG,
 * INPUT, LINK, META, PARAM, SOURCE, TRACK, WBR), so {@code HTMLTagBalancerFilter} never
 * pushes them onto its element stack and they never contain children. A trailing
 * {@code /} in a start tag (e.g. {@code <div/>}, {@code <br/>}) is not treated specially by
 * the {@code START_TAG}/{@code ATTRIBUTE} regexes: it is simply discarded as unmatched
 * attribute text. For non-void elements this means the self-closing syntax is silently
 * ignored and the element remains open, wrapping whatever follows.
 * </p>
 */
public class BrokenVoidAndSelfClosingTest {

    @Test
    public void brIsVoidAndNeverGetsChildren() throws Exception {
        // characterization: current behavior - BR is emitted and immediately closed,
        // never pushed onto the stack, so it can have no children.
        final Document doc = parse("<div><br>text</div>");
        assertEquals(1, count(doc, "//BR"));
        assertEquals(0, count(doc, "//BR/*"));
        assertTrue(firstText(doc, "//DIV").contains("text"));
    }

    @Test
    public void hrIsVoid() throws Exception {
        final Document doc = parse("<div><hr>text</div>");
        assertEquals(1, count(doc, "//HR"));
        assertEquals(0, count(doc, "//HR/*"));
    }

    @Test
    public void imgIsVoidAndKeepsAttributes() throws Exception {
        final Document doc = parse("<div><img src=x>text</div>");
        assertEquals(1, count(doc, "//IMG"));
        final Element img = first(doc, "//IMG");
        assertEquals("x", img.getAttribute("src"));
        assertEquals(0, count(doc, "//IMG/*"));
    }

    @Test
    public void inputIsVoid() throws Exception {
        final Document doc = parse("<div><input>text</div>");
        assertEquals(1, count(doc, "//INPUT"));
        assertEquals(0, count(doc, "//INPUT/*"));
    }

    @Test
    public void metaIsVoid() throws Exception {
        final Document doc = parse("<meta charset=utf-8><p>x</p>");
        assertEquals(1, count(doc, "//META"));
        assertEquals(0, count(doc, "//META/*"));
    }

    @Test
    public void linkIsVoid() throws Exception {
        final Document doc = parse("<link rel=stylesheet><p>x</p>");
        assertEquals(1, count(doc, "//LINK"));
        assertEquals(0, count(doc, "//LINK/*"));
    }

    @Test
    public void wbrIsVoid() throws Exception {
        final Document doc = parse("<p>a<wbr>b</p>");
        assertEquals(1, count(doc, "//WBR"));
        assertEquals(0, count(doc, "//WBR/*"));
    }

    @Test
    public void colIsVoid() throws Exception {
        final Document doc = parse("<colgroup><col></colgroup>");
        assertEquals(1, count(doc, "//COL"));
        assertEquals(0, count(doc, "//COL/*"));
    }

    @Test
    public void embedIsVoid() throws Exception {
        final Document doc = parse("<embed src=x>");
        assertEquals(1, count(doc, "//EMBED"));
        assertEquals(0, count(doc, "//EMBED/*"));
    }

    @Test
    public void sourceIsVoid() throws Exception {
        final Document doc = parse("<video><source src=x></video>");
        assertEquals(1, count(doc, "//SOURCE"));
        assertEquals(0, count(doc, "//SOURCE/*"));
    }

    @Test
    public void trackIsVoid() throws Exception {
        final Document doc = parse("<video><track kind=captions></video>");
        assertEquals(1, count(doc, "//TRACK"));
        assertEquals(0, count(doc, "//TRACK/*"));
    }

    @Test
    public void baseIsVoid() throws Exception {
        final Document doc = parse("<base href=x><p>y</p>");
        assertEquals(1, count(doc, "//BASE"));
        assertEquals(0, count(doc, "//BASE/*"));
    }

    @Test
    public void paramIsVoid() throws Exception {
        final Document doc = parse("<object><param name=x></object>");
        assertEquals(1, count(doc, "//PARAM"));
        assertEquals(0, count(doc, "//PARAM/*"));
    }

    @Test
    public void areaIsVoid() throws Exception {
        final Document doc = parse("<map><area shape=rect></map>");
        assertEquals(1, count(doc, "//AREA"));
        assertEquals(0, count(doc, "//AREA/*"));
    }

    @Test
    public void selfClosingBrSlashIsStillVoidBr() throws Exception {
        // characterization: <br/> parses as a normal void BR; the trailing '/' is
        // discarded by attribute parsing and has no special effect (BR is already void).
        final Document doc = parse("<br/>");
        assertEquals(1, count(doc, "//BR"));
    }

    @Test
    public void selfClosingBrWithSpaceSlashIsVoidBr() throws Exception {
        final Document doc = parse("<br />");
        assertEquals(1, count(doc, "//BR"));
    }

    @Test
    public void selfClosingImgSlashKeepsAttributeAndIsVoid() throws Exception {
        final Document doc = parse("<img src=\"x\"/>");
        assertEquals(1, count(doc, "//IMG"));
        assertEquals("x", first(doc, "//IMG").getAttribute("src"));
    }

    @Test
    public void nonVoidSelfClosingDivIsIgnoredAndStaysOpen() throws Exception {
        // characterization: current behavior (non-standard) - the '/' before '>' on a
        // non-void element is discarded; DIV remains open and wraps the following SPAN.
        final Document doc = parse("<div/><span>x</span>");
        assertEquals(1, count(doc, "//DIV"));
        assertEquals(1, count(doc, "//DIV//SPAN"));
    }

    @Test
    public void nonVoidSelfClosingSpanWrapsFollowingText() throws Exception {
        // characterization: current behavior (non-standard) - <span/>text keeps SPAN
        // open, so the following text becomes a child of SPAN rather than a sibling.
        final Document doc = parse("<span/>text");
        assertEquals(1, count(doc, "//SPAN"));
        assertTrue(firstText(doc, "//SPAN").contains("text"));
    }

    @Test
    public void nonVoidSelfClosingPWrapsFollowingText() throws Exception {
        // characterization: current behavior (non-standard) - <p/>x keeps P open and
        // absorbs the following text as its content.
        final Document doc = parse("<p/>x");
        assertEquals(1, count(doc, "//P"));
        assertTrue(firstText(doc, "//P").contains("x"));
    }

    @Test
    public void unknownElementSelfClosingIsAlsoIgnored() throws Exception {
        // characterization: current behavior (non-standard) - unknown tag names are not
        // in VOID_ELEMENTS, so <foo/> behaves like any other non-void self-closing tag:
        // it stays open and wraps whatever follows.
        final Document doc = parse("<foo/>bar</foo>");
        assertEquals(1, count(doc, "//*[local-name()='FOO']"));
        assertTrue(firstText(doc, "//*[local-name()='FOO']").contains("bar"));
    }

    @Test
    public void strayVoidEndTagBrIsHarmless() throws Exception {
        // characterization: current behavior - </br> is a stray end tag for an element
        // that was never pushed onto the stack (BR is void), so it is passed straight
        // through by the balancer as an end event with no corresponding open element.
        final Document doc = parse("<p>a</br>b</p>");
        assertEquals(0, count(doc, "//BR"));
        assertEquals(1, count(doc, "//P"));
    }

    @Test
    public void consecutiveVoidImgElementsDoNotNest() throws Exception {
        // characterization: since void elements are never pushed onto the stack, two
        // consecutive <img> tags cannot nest (unlike non-void elements in category D);
        // both are always siblings under the same parent.
        final Document doc = parse("<div><img>text<img></div>");
        assertEquals(2, count(doc, "//IMG"));
        assertEquals(0, count(doc, "//IMG/IMG"));
        assertEquals(2, count(doc, "//DIV/IMG"));
    }

    @Test
    public void saxEventsForVoidBrShowImmediateEndBeforeFollowingText() throws Exception {
        // characterization: SAX event order confirms end:BR fires immediately after start:BR, before
        // the following characters event. BR and the text are wrapped in a synthesized BODY (HTML5),
        // and "after" is emitted with no fabricated trailing newline; the implicit HTML root is closed
        // at EOF (no explicit <html> was given).
        final List<String> events = saxEvents("<br>after");
        final int startIdx = events.indexOf("start:BR");
        final int endIdx = events.indexOf("end:BR");
        assertTrue(startIdx >= 0);
        assertEquals(startIdx + 1, endIdx);
        assertEquals(List.of("start:HTML", "start:BODY", "start:BR", "end:BR", "chars:after", "end:BODY", "end:HTML"), events);
    }

    @Test
    public void voidElementWithSelfClosingSlashHasNoChildrenEvenWithAttributes() throws Exception {
        final Document doc = parse("<input type=\"text\" name=\"x\"/><p>after</p>");
        assertEquals(1, count(doc, "//INPUT"));
        final Element input = first(doc, "//INPUT");
        assertEquals("text", input.getAttribute("type"));
        assertEquals("x", input.getAttribute("name"));
        assertEquals(0, count(doc, "//INPUT/*"));
        assertEquals(1, count(doc, "//P"));
    }
}
