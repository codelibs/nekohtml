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
 * Characterization tests for broken/malformed comments, DOCTYPE, CDATA and
 * processing instructions (category I). These tests lock in the current
 * regex-based dispatch behavior of {@code SimpleHTMLScanner}, including
 * several intentionally non-standard behaviors and differences between the
 * {@code DOMParser} (which wires up a lexical handler) and the plain
 * {@code SAXParser} entry point used via {@code saxEvents}/{@code saxStartElements}
 * (which does not).
 */
public class BrokenCommentDoctypeCdataTest {

    @Test
    public void normalCommentBecomesDomCommentNodeByDefault() throws Exception {
        // characterization: DOMParser wires SAXToDOMHandler as the lexical handler,
        // so a well-formed comment DOES appear as a DOM comment node by default
        final Document doc = parse("<div><!-- comment --></div>");
        assertEquals(1, count(doc, "//comment()"));
        assertTrue(firstText(doc, "//comment()").contains("comment"));
    }

    @Test
    public void commentNodeIsSiblingOfSurroundingText() throws Exception {
        final Document doc = parse("<p>a<!--c-->b</p>");
        assertEquals(1, count(doc, "//P/comment()"));
        assertEquals("c", firstText(doc, "//comment()"));
    }

    @Test
    public void unclosedCommentLeaksAsText() throws Exception {
        // characterization: an unterminated comment runs to EOF as a comment (eof-in-comment, HTML5);
        // its content (including "unclosed") is preserved inside the comment node, not leaked as text
        final Document doc = parse("<div><!-- unclosed</div>");
        assertEquals(1, count(doc, "//comment()"));
        assertEquals(1, count(doc, "//DIV"));
        assertEquals(1, count(doc, "//DIV/comment()"));
        assertTrue(firstText(doc, "//comment()").contains("unclosed"));
    }

    @Test
    public void shortFormBangDashDashGreaterDoesNotMatchLeaksAsText() throws Exception {
        // characterization: "<!-->" is HTML5 "abrupt-closing of an empty comment" -> an empty
        // comment node (not leaked text)
        final Document doc = parse("<div><!--></div>");
        assertEquals(1, count(doc, "//comment()"));
        assertEquals("", firstText(doc, "//comment()"));
    }

    @Test
    public void tooShortDashCommentLeaksAsText() throws Exception {
        // characterization: "<!--->" is also HTML5 "abrupt-closing of an empty comment" -> an empty
        // comment node (not leaked text)
        final Document doc = parse("<!--->");
        assertEquals(1, count(doc, "//comment()"));
        assertEquals("", firstText(doc, "//comment()"));
    }

    @Test
    public void emptyCommentProducesEmptyCommentNode() throws Exception {
        final Document doc = parse("<!---->");
        assertEquals(1, count(doc, "//comment()"));
        assertEquals("", firstText(doc, "//comment()"));
    }

    @Test
    public void commentImmediatelyFollowedByTextOutsideIt() throws Exception {
        final Document doc = parse("<!--x-->y");
        assertEquals(1, count(doc, "//comment()"));
        assertEquals("x", firstText(doc, "//comment()"));
        assertEquals(1, count(doc, "//text()[contains(.,'y')]"));
    }

    @Test
    public void nestedCommentMarkersAreAbsorbedIntoFirstComment() throws Exception {
        // characterization: the non-greedy COMMENT regex closes at the FIRST "-->", so a nested
        // "<!--" marker is swallowed as literal content of the outer comment, not parsed on its own
        final Document doc = parse("<!-- <!-- -->");
        assertEquals(1, count(doc, "//comment()"));
        assertEquals("<!--", firstText(doc, "//comment()").trim());
    }

    @Test
    public void doctypeDoesNotAppearInDom() throws Exception {
        // characterization: <!DOCTYPE> is consumed by the scanner and never reaches the DOM
        final Document doc = parse("<!DOCTYPE html><html><body>x</body></html>");
        assertEquals(1, count(doc, "//BODY"));
        assertTrue(firstText(doc, "//BODY").contains("x"));
        assertEquals(0, count(doc, "//text()[contains(.,'DOCTYPE')]"));
    }

    @Test
    public void lowercaseDoctypeKeywordWithUppercaseValueHandledNormally() throws Exception {
        // characterization: the exact-case gate accepts a fully-lowercase "<!doctype" keyword,
        // regardless of the case used in the doctype name itself
        final Document doc = parse("<!doctype HTML><p>x</p>");
        assertEquals(1, count(doc, "//P"));
        assertEquals("x", firstText(doc, "//P"));
        assertEquals(0, count(doc, "//text()[contains(.,'doctype')]"));
    }

    @Test
    public void mixedCaseDoctypeKeywordIsNotRecognizedAndLeaksAsText() throws Exception {
        // characterization: the DOCTYPE keyword is ASCII case-insensitive (HTML5), so "<!DocType"
        // is recognized as a DOCTYPE and consumed, not leaked as text
        final Document doc = parse("<!DocType html><p>x</p>");
        assertEquals(1, count(doc, "//P"));
        assertEquals("x", firstText(doc, "//P"));
        assertEquals(0, count(doc, "//text()[contains(.,'DocType')]"));
    }

    @Test
    public void cdataEmitsInnerTextAsCharacters() throws Exception {
        final Document doc = parse("<div><![CDATA[ text ]]></div>");
        assertEquals(1, count(doc, "//DIV"));
        assertTrue(firstText(doc, "//DIV").contains("text"));
    }

    @Test
    public void cdataContentEntitiesAreNotDecoded() throws Exception {
        // characterization: CDATA content is emitted verbatim (no resolveEntities call),
        // unlike normal text content where "&amp;" would decode to "&"
        final Document doc = parse("<div><![CDATA[ &amp; ]]></div>");
        assertTrue(firstText(doc, "//DIV").contains("&amp;"));
    }

    @Test
    public void unterminatedCdataDropsAngleBracketLeaksRestAsText() throws Exception {
        // characterization: an unterminated CDATA section recovers by emitting its inner content as
        // characters (the "x" content is preserved); the "![CDATA[" syntax is not leaked as text
        final Document doc = parse("<div><![CDATA[ x</div>");
        assertEquals(1, count(doc, "//DIV"));
        assertFalse(firstText(doc, "//DIV").contains("![CDATA["));
        assertTrue(firstText(doc, "//DIV").contains("x"));
    }

    @Test
    public void xmlProcessingInstructionLeaksAsText() throws Exception {
        // characterization: processing instructions become bogus comments (HTML5); "?xml ...?" is a
        // comment node, not ordinary text
        final Document doc = parse("<?xml version=\"1.0\"?><p>x</p>");
        assertEquals(1, count(doc, "//P"));
        assertEquals("x", firstText(doc, "//P"));
        assertEquals(0, count(doc, "//text()[contains(.,'?xml')]"));
    }

    @Test
    public void phpProcessingInstructionLeaksAsText() throws Exception {
        // characterization: a PHP processing instruction becomes a bogus comment (HTML5), not leaked text
        final Document doc = parse("<?php echo 1 ?>");
        assertEquals(0, count(doc, "//text()[contains(.,'php')]"));
    }

    @Test
    public void entityDeclarationLeaksAsText() throws Exception {
        // characterization: other "<!" constructs (e.g. <!ENTITY>) become bogus comments (HTML5), not text
        final Document doc = parse("<!ENTITY foo>");
        assertEquals(0, count(doc, "//text()[contains(.,'ENTITY')]"));
    }

    @Test
    public void saxCommentsAreSilentlyConsumedWithoutLexicalHandler() throws Exception {
        // characterization: saxEvents() registers no lexical handler; the scanner still matches
        // and consumes the comment (advancing past it) but never emits its content as characters,
        // so the comment text disappears entirely (unlike CDATA, which always emits characters)
        final List<String> events = saxEvents("<p>a<!--c-->b</p>");
        assertTrue(events.contains("chars:a"));
        assertTrue(events.contains("chars:b"));
        assertFalse(events.stream().anyMatch(e -> e.startsWith("chars:") && e.substring("chars:".length()).contains("c")));
    }

    @Test
    public void saxStartElementsUnaffectedByComments() throws Exception {
        // characterization: the tag balancer synthesizes implicit HTML and BODY start elements around
        // the body content; the interleaved comment does not affect the start-element stream
        final List<String> names = saxStartElements("<div><!--c--><p>x</p></div>");
        assertEquals(List.of("HTML", "BODY", "DIV", "P"), names);
    }

    @Test
    public void doctypeConsumedSilentlyInSaxPathToo() throws Exception {
        // characterization: unlike comments, DOCTYPE consumption does not depend on a lexical
        // handler being present; it advances past the construct unconditionally
        final List<String> events = saxEvents("<!DOCTYPE html><p>x</p>");
        assertTrue(events.contains("start:P"));
        assertFalse(events.stream().anyMatch(e -> e.startsWith("chars:") && e.substring("chars:".length()).contains("DOCTYPE")));
    }

    @Test
    public void cdataStillEmitsCharactersEvenWithoutLexicalHandlerViaSax() throws Exception {
        // characterization: CDATA has an explicit fallback to emit characters when no lexical
        // handler is set, unlike comments which are silently dropped in that case
        final List<String> events = saxEvents("<![CDATA[ hi ]]>");
        assertTrue(events.stream().anyMatch(e -> e.startsWith("chars:") && e.contains("hi")));
    }

    @Test
    public void commentBetweenSiblingElementsDoesNotBreakTree() throws Exception {
        final Document doc = parse("<ul><li>a</li><!-- x --><li>b</li></ul>");
        assertEquals(2, count(doc, "//UL/LI"));
        assertEquals(1, count(doc, "//comment()"));
    }
}
