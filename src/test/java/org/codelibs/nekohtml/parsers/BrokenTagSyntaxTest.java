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
 * Category L: malformed tag names/syntax characterization tests.
 *
 * <p>
 * characterization: {@code SimpleHTMLScanner}'s start-tag pattern is
 * {@code <([a-zA-Z][a-zA-Z0-9-:]*)([^>]*)>} - a tag name must start with a letter, and only
 * letters/digits/'-'/':' may follow. Whenever '<' is not immediately followed by a valid tag-name
 * start (and isn't part of a comment/DOCTYPE/CDATA/end-tag), the scanner falls into its "unknown tag"
 * branch, which drops just that single '<' character and resumes plain-text scanning from the very
 * next character - it never consumes or hides the rest of the bogus markup.
 * </p>
 */
public class BrokenTagSyntaxTest {

    // ------------------------------------------------------------------
    // '<' not followed by a valid name start
    // ------------------------------------------------------------------

    @Test
    public void bareAngleBracketsBecomeText() throws Exception {
        // characterization: "<>" - '<' is dropped, ">" survives as text.
        final Document doc = parse("<p><></p>");
        assertEquals(1, count(doc, "//P"));
        assertTrue(firstText(doc, "//P").contains(">"));
    }

    @Test
    public void soleLessThanFollowedBySpaceBecomesText() throws Exception {
        // characterization: "< >" - '<' is dropped, " >" survives as text.
        final Document doc = parse("< >rest");
        assertTrue(firstText(doc, "//HTML").contains(">rest"));
    }

    @Test
    public void spaceThenLetterAfterLessThanIsNotATag() throws Exception {
        // characterization: "< div>" - the space right after '<' disqualifies it as a tag start;
        // '<' is dropped and "div>" survives as text (no DIV element is created).
        final Document doc = parse("< div>rest");
        assertEquals(0, count(doc, "//DIV"));
        assertTrue(firstText(doc, "//HTML").contains("div>rest"));
    }

    @Test
    public void digitStartedTagNameIsNotATag() throws Exception {
        // characterization: "<123>" - a tag name may not start with a digit; '<' is dropped and
        // "123>" survives as text.
        final Document doc = parse("<p><123>text</p>");
        assertTrue(firstText(doc, "//P").contains("123>text"));
    }

    @Test
    public void hyphenStartedTagNameIsNotATag() throws Exception {
        // characterization: "<-foo>" - a tag name may not start with '-'; '<' is dropped and
        // "-foo>" survives as text.
        final Document doc = parse("<p><-foo>text</p>");
        assertTrue(firstText(doc, "//P").contains("-foo>text"));
    }

    @Test
    public void tagNameCannotStartWithColon() throws Exception {
        // characterization: ':' is allowed inside a tag name but not as its first character.
        final Document doc = parse("<p><:tag>text</p>");
        assertEquals(0, count(doc, "//*[local-name()='TAG']"));
        assertTrue(firstText(doc, "//P").contains(":tag>text"));
    }

    @Test
    public void consecutiveMalformedNumericTagsBecomeLiteralText() throws Exception {
        // characterization: each bogus '<' is dropped independently; the surrounding fragments merge
        // into one contiguous text run.
        final Document doc = parse("<p><1><2><3></p>");
        assertTrue(firstText(doc, "//P").contains("1>2>3>"));
    }

    // ------------------------------------------------------------------
    // Unterminated tags (no closing '>' anywhere)
    // ------------------------------------------------------------------

    @Test
    public void unterminatedTagWithNoClosingAngleDropsLessThan() throws Exception {
        // characterization: "<b" at EOF - START_TAG requires a literal '>', which never appears, so
        // it can never match; '<' is dropped and "b" survives as text. No B element is created.
        final Document doc = parse("<b");
        assertEquals(0, count(doc, "//B"));
        assertTrue(firstText(doc, "//HTML").contains("b"));
    }

    @Test
    public void unterminatedTagWithAttributeLikeTextNoClosingAngle() throws Exception {
        final Document doc = parse("<b attr");
        assertEquals(0, count(doc, "//B"));
        assertTrue(firstText(doc, "//HTML").contains("b attr"));
    }

    @Test
    public void lessThanAtEndOfDocumentIsDroppedSilently() throws Exception {
        // characterization: a single trailing '<' never becomes an element; but the scanner's line
        // reader always appends a trailing '\n' to the raw content, and that lone newline is emitted
        // as a characters() event, which is enough to bootstrap the implicit HTML root - so the
        // document ends up with exactly one (empty) HTML element and nothing else.
        final Document doc = parse("<");
        assertNotNull(doc);
        assertEquals(1, count(doc, "//*"));
        assertEquals(1, count(doc, "//HTML"));
    }

    // ------------------------------------------------------------------
    // Valid-but-unusual tag names
    // ------------------------------------------------------------------

    @Test
    public void hyphenatedTagNameUppercased() throws Exception {
        final Document doc = parse("<my-tag>x</my-tag>");
        assertEquals(1, count(doc, "//*[local-name()='MY-TAG']"));
        assertTrue(firstText(doc, "//*[local-name()='MY-TAG']").contains("x"));
    }

    @Test
    public void hyphenatedTagNameKeepsHyphenatedAttributeToo() throws Exception {
        final Document doc = parse("<my-tag data-x=\"1\">x</my-tag>");
        assertEquals("1", first(doc, "//*[local-name()='MY-TAG']").getAttribute("data-x"));
    }

    @Test
    public void namespaceLikeColonTagNameIsPreserved() throws Exception {
        // characterization: ':' is a legal tag-name character, so a namespace-looking name round-trips
        // through a matching start/end tag pair like any ordinary element name. The element is created
        // via the non-namespace-aware DOM API, so XPath's local-name() strips the "NS:" prefix down to
        // just "TAG" (its localName is null); name() reports the full literal "NS:TAG" instead.
        final Document doc = parse("<ns:tag>x</ns:tag>");
        assertEquals(1, count(doc, "//*[name()='NS:TAG']"));
        assertEquals(1, count(doc, "//*[local-name()='TAG']"));
        assertTrue(firstText(doc, "//*[name()='NS:TAG']").contains("x"));
    }

    @Test
    public void saxStartElementsShowNamespaceLikeTagVerbatim() throws Exception {
        final List<String> starts = saxStartElements("<ns:tag>x</ns:tag>");
        assertTrue(starts.contains("NS:TAG"));
    }

    @Test
    public void tagNameMayContainDigitsAfterFirstLetter() throws Exception {
        // characterization (contrast with digitStartedTagNameIsNotATag): a leading letter makes
        // trailing digits perfectly legal in a tag name.
        final Document doc = parse("<h1>x</h1>");
        assertEquals(1, count(doc, "//H1"));
        assertTrue(firstText(doc, "//H1").contains("x"));
    }

    @Test
    public void underscoreInTagNameBreaksNameAtUnderscore() throws Exception {
        // characterization: '_' is not a legal tag-name character, so the name group stops at "my"
        // and "_tag" is left over to be (mis-)parsed as the attribute string.
        final Document doc = parse("<my_tag>content");
        assertEquals(1, count(doc, "//MY"));
        assertTrue(firstText(doc, "//MY").contains("content"));
    }

    @Test
    public void underscoreTagEndTagNeverMatchesRemainsOpenUntilEof() throws Exception {
        // characterization: the corresponding "</my_tag>" fails the END_TAG pattern for the same
        // reason (the underscore isn't a legal name character and no backtrack succeeds), so it is
        // never recognized as a closing tag either; its '<' is dropped and the rest leaks as text
        // inside the still-open MY element, which only gets closed implicitly at end of document.
        final Document doc = parse("<my_tag>x</my_tag>");
        assertEquals(1, count(doc, "//MY"));
        final String text = firstText(doc, "//MY");
        assertTrue(text.contains("x"));
        assertTrue(text.contains("/my_tag>"));
    }

    // ------------------------------------------------------------------
    // Multiple/odd '<' sequences
    // ------------------------------------------------------------------

    @Test
    public void doubledOpenAngleDropsFirstThenParsesRealTag() throws Exception {
        // characterization: "<<b>>" - the first '<' fails every check and is dropped; scanning then
        // resumes at the second '<', which starts a perfectly valid "<b>" tag; the final ">" becomes
        // B's text content.
        final Document doc = parse("<<b>>");
        assertEquals(1, count(doc, "//B"));
        assertTrue(firstText(doc, "//B").contains(">"));
    }

    @Test
    public void tripleAngleBracketsProduceNothing() throws Exception {
        // characterization: "<<<" - every character is '<', so the scanner just drops each one in
        // turn and never emits a characters() event for any of them. The only reason the document
        // isn't completely empty is the scanner's line reader appending a trailing '\n', which is
        // itself emitted as a characters() event and bootstraps the implicit HTML root.
        final Document doc = parse("<<<");
        assertNotNull(doc);
        assertEquals(1, count(doc, "//*"));
        assertEquals(1, count(doc, "//HTML"));
    }

    @Test
    public void mathLikeLessThanBetweenSpacesBecomesLiteralText() throws Exception {
        // characterization: "a < b" - the '<' is not followed by a letter, so it is dropped; "a " and
        // " b" merge into one text run around the gap left by the missing '<'.
        final Document doc = parse("<p>a < b</p>");
        final String text = firstText(doc, "//P");
        assertTrue(text.contains("a"));
        assertTrue(text.contains("b"));
        assertTrue(text.contains("a  b") || text.contains("a b"));
    }

    // ------------------------------------------------------------------
    // Self-closing slash and name resolution
    // ------------------------------------------------------------------

    @Test
    public void selfClosingSlashDoesNotCorruptTagName() throws Exception {
        // characterization: the trailing '/' is swallowed into the attribute-string group, not the
        // tag name, so the element name still resolves cleanly to "B".
        final Document doc = parse("<b/>x");
        assertEquals(1, count(doc, "//B"));
        assertTrue(firstText(doc, "//B").contains("x"));
    }

    @Test
    public void saxEventsForDigitStartedTagShowNoElementJustText() throws Exception {
        // characterization: only the implicit HTML root ever gets a start event; "<123>" never
        // produces one of its own.
        final List<String> events = saxEvents("<123>text");
        final long startEventCount = events.stream().filter(e -> e.startsWith("start:")).count();
        assertEquals(1, startEventCount);
        assertEquals("start:HTML", events.get(0));
        assertTrue(events.stream().anyMatch(e -> e.startsWith("chars:") && e.contains("123>text")));
    }

}
