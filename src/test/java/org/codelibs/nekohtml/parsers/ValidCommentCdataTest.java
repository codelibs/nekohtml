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
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.parse;
import static org.codelibs.nekohtml.parsers.ValidHtmlTestSupport.childSignature;
import static org.codelibs.nekohtml.parsers.ValidHtmlTestSupport.lexicalEvents;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Characterization tests for comments and CDATA sections on VALID/well-formed HTML, exercised
 * through the SAX lexical-event stream ({@code lexicalEvents}) and through the DOM tree built by
 * {@code DOMParser}.
 */
public class ValidCommentCdataTest {

    // -----------------------------------------------------------------
    // Comments
    // -----------------------------------------------------------------

    @Test
    public void commentTextPreservesSurroundingWhitespace() throws Exception {
        final List<String> events = lexicalEvents("<p>a</p><!-- hi --><p>b</p>");
        // characterization: the space before/after "hi" is part of the captured comment text
        assertTrue(events.contains("comment: hi "), events.toString());
    }

    @Test
    public void commentAppearsAsDomCommentNodeWithExactValue() throws Exception {
        final Document doc = parse("<p>a</p><!-- hi --><p>b</p>");
        assertEquals(1, count(doc, "//comment()"));
        final List<String> sig = childSignature(doc.getDocumentElement());
        assertTrue(sig.contains("comment: hi "), sig.toString());
    }

    @Test
    public void commentIsSiblingOfSurroundingTextNodesInOrder() throws Exception {
        final Document doc = parse("<p>a<!--c-->b</p>");
        final List<String> sig = childSignature(first(doc, "//P"));
        assertEquals(List.of("text:a", "comment:c", "text:b"), sig);
    }

    @Test
    public void entitiesAreNotDecodedInsideComments() throws Exception {
        // characterization: comment content is captured verbatim; no entity resolution is applied
        final List<String> events = lexicalEvents("<!--&amp;--><p>x</p>");
        assertTrue(events.contains("comment:&amp;"), events.toString());
    }

    @Test
    public void entitiesNotDecodedInsideCommentAlsoHoldsInDom() throws Exception {
        final Document doc = parse("<!--&amp;--><p>x</p>");
        // characterization: a comment that appears before any real element has been opened is
        // attached directly to the Document node itself (a sibling of the auto-inserted HTML root),
        // not nested inside the HTML subtree
        assertEquals("&amp;", doc.getFirstChild().getNodeValue());
    }

    @Test
    public void multiLineCommentPreservesEmbeddedNewline() throws Exception {
        final List<String> events = lexicalEvents("<!--l1\nl2--><p>x</p>");
        assertTrue(events.contains("comment:l1\nl2"), events.toString());
    }

    @Test
    public void emptyCommentProducesEmptyCommentText() throws Exception {
        final List<String> events = lexicalEvents("<!----><p>x</p>");
        assertTrue(events.contains("comment:"), events.toString());
    }

    @Test
    public void emptyCommentProducesEmptyDomCommentNode() throws Exception {
        final Document doc = parse("<!----><p>x</p>");
        assertEquals(1, count(doc, "//comment()"));
        // characterization: same document-level attachment as above, since the comment precedes
        // the first real element
        assertEquals("", doc.getFirstChild().getNodeValue());
    }

    @Test
    public void commentContainingMarkupIsNotParsedAsTags() throws Exception {
        // characterization: text that looks like a tag inside a comment stays literal comment text
        final List<String> events = lexicalEvents("<!-- <p>not a tag</p> --><p>x</p>");
        assertTrue(events.contains("comment: <p>not a tag</p> "), events.toString());
        // only the real <p>x</p> produces a start:P event
        assertEquals(1, events.stream().filter(e -> e.equals("start:P")).count());
    }

    @Test
    public void twoConsecutiveCommentsBothAppearInOrder() throws Exception {
        final List<String> events = lexicalEvents("<!--first--><!--second--><p>x</p>");
        assertTrue(events.indexOf("comment:first") < events.indexOf("comment:second"), events.toString());
        assertTrue(events.indexOf("comment:second") < events.indexOf("start:P"), events.toString());
    }

    @Test
    public void commentBetweenListItemsDoesNotBreakDomStructure() throws Exception {
        final Document doc = parse("<ul><li>a</li><!-- x --><li>b</li></ul>");
        assertEquals(2, count(doc, "//UL/LI"));
        assertEquals(1, count(doc, "//comment()"));
    }

    // -----------------------------------------------------------------
    // CDATA
    // -----------------------------------------------------------------

    @Test
    public void cdataEmitsStartCdataCharsEndCdataInOrder() throws Exception {
        final List<String> events = lexicalEvents("<div><![CDATA[x <b>raw</b> & y]]></div>");
        assertTrue(events.contains("startCDATA"), events.toString());
        assertTrue(events.contains("endCDATA"), events.toString());
        // characterization: markup inside CDATA is not parsed, and '&' is not decoded
        assertTrue(events.contains("chars:x <b>raw</b> & y"), events.toString());
        final int startCdata = events.indexOf("startCDATA");
        final int chars = events.indexOf("chars:x <b>raw</b> & y");
        final int endCdata = events.indexOf("endCDATA");
        assertTrue(startCdata < chars, events.toString());
        assertTrue(chars < endCdata, events.toString());
    }

    @Test
    public void cdataDoesNotProduceElementEventsForEmbeddedTags() throws Exception {
        final List<String> events = lexicalEvents("<div><![CDATA[x <b>raw</b> & y]]></div>");
        assertTrue(events.stream().noneMatch(e -> e.equals("start:B")), events.toString());
    }

    @Test
    public void cdataAppearsAsPlainTextNodeInDomNotCdataSection() throws Exception {
        final Document doc = parse("<div><![CDATA[abc]]></div>");
        final List<String> sig = childSignature(first(doc, "//DIV"));
        // characterization: CDATA content is folded into a regular DOM Text node, not a CDATASection
        assertEquals(List.of("text:abc"), sig);
    }

    @Test
    public void emptyCdataEmitsNoCharsButStillEmitsStartAndEndCdata() throws Exception {
        final List<String> events = lexicalEvents("<div><![CDATA[]]></div>");
        assertTrue(events.contains("startCDATA"), events.toString());
        assertTrue(events.contains("endCDATA"), events.toString());
        final int startCdata = events.indexOf("startCDATA");
        final int endCdata = events.indexOf("endCDATA");
        // characterization: no "chars:" event is emitted for an empty CDATA section
        assertEquals(startCdata + 1, endCdata, events.toString());
    }

    @Test
    public void whitespaceOnlyCdataPreservedVerbatim() throws Exception {
        final List<String> events = lexicalEvents("<div><![CDATA[   ]]></div>");
        assertTrue(events.contains("chars:   "), events.toString());
    }

    @Test
    public void cdataMergesWithSurroundingTextIntoSingleDomTextNode() throws Exception {
        // characterization: the CDATA boundary is invisible to the DOM builder (startCDATA/endCDATA
        // are no-ops there), so text before/inside/after a CDATA section merges into one Text node
        final Document doc = parse("<div>a<![CDATA[b]]>c</div>");
        final List<String> sig = childSignature(first(doc, "//DIV"));
        assertEquals(List.of("text:abc"), sig);
    }

    @Test
    public void cdataEntitiesNotDecodedInDom() throws Exception {
        final Document doc = parse("<div><![CDATA[&amp;]]></div>");
        assertEquals("&amp;", first(doc, "//DIV").getTextContent());
    }
}
