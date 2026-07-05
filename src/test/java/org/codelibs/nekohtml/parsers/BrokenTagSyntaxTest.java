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
 * characterization: {@code SimpleHTMLScanner} is a single-pass HTML5-leaning tokenizer. A tag name
 * must start with a letter; letters/digits/'-'/':'/'_'/'.' may follow. Whenever '<' is not
 * immediately followed by a valid tag-name start (and isn't part of a comment/DOCTYPE/CDATA/end-tag),
 * the scanner PRESERVES the '<' as a literal text character (HTML5 "invalid-first-character-of-tag-
 * name") and resumes plain-text scanning from the next character - nothing is dropped.
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
        // characterization: each bogus '<' is preserved as literal text (HTML5); the fragments merge
        // into one contiguous text run "<1><2><3>".
        final Document doc = parse("<p><1><2><3></p>");
        assertTrue(firstText(doc, "//P").contains("<1><2><3>"));
    }

    // ------------------------------------------------------------------
    // Unterminated tags (no closing '>' anywhere)
    // ------------------------------------------------------------------

    @Test
    public void unterminatedTagWithNoClosingAngleDropsLessThan() throws Exception {
        // "<b" at EOF is an incomplete start tag: per HTML5 the tokenizer discards an unfinished tag
        // at end-of-input, so no B element is created and, with no other content, the document has no
        // root element (consistent with truly-empty input).
        final Document doc = parse("<b");
        assertEquals(0, count(doc, "//B"));
        assertNull(doc.getDocumentElement());
    }

    @Test
    public void unterminatedTagWithAttributeLikeTextNoClosingAngle() throws Exception {
        // "<b attr" at EOF is likewise an unfinished start tag, discarded at end-of-input, leaving an
        // empty document with no root element.
        final Document doc = parse("<b attr");
        assertEquals(0, count(doc, "//B"));
        assertNull(doc.getDocumentElement());
    }

    @Test
    public void lessThanAtEndOfDocumentIsDroppedSilently() throws Exception {
        // characterization: a single trailing '<' never becomes an element, but it is preserved as a
        // literal text character (HTML5). That text is wrapped in a synthesized BODY, so the document
        // has exactly two elements: HTML and BODY.
        final Document doc = parse("<");
        assertNotNull(doc);
        assertEquals(2, count(doc, "//*"));
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
        // characterization: '_' IS a legal tag-name character (HTML5), so the element name is the full
        // "MY_TAG" (the name is not split at the underscore).
        final Document doc = parse("<my_tag>content");
        assertEquals(1, count(doc, "//*[local-name()='MY_TAG']"));
        assertTrue(firstText(doc, "//*[local-name()='MY_TAG']").contains("content"));
    }

    @Test
    public void underscoreTagEndTagNeverMatchesRemainsOpenUntilEof() throws Exception {
        // characterization: '_' is a legal name character, so the element is "MY_TAG" and the matching
        // "</my_tag>" closes it cleanly; "x" is its content and nothing leaks as text.
        final Document doc = parse("<my_tag>x</my_tag>");
        assertEquals(1, count(doc, "//*[local-name()='MY_TAG']"));
        final String text = firstText(doc, "//*[local-name()='MY_TAG']");
        assertTrue(text.contains("x"));
        assertFalse(text.contains("/my_tag>"));
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
        // characterization: "<<<" - each '<' that cannot start a tag is preserved as a literal text
        // character (HTML5). The "<<<" text run is wrapped in a synthesized BODY, so the document has
        // two elements: HTML and BODY.
        final Document doc = parse("<<<");
        assertNotNull(doc);
        assertEquals(2, count(doc, "//*"));
        assertEquals(1, count(doc, "//HTML"));
    }

    @Test
    public void mathLikeLessThanBetweenSpacesBecomesLiteralText() throws Exception {
        // characterization: "a < b" - the '<' is not followed by a letter, so it is preserved as a
        // literal text character (HTML5); the text stays "a < b".
        final Document doc = parse("<p>a < b</p>");
        final String text = firstText(doc, "//P");
        assertTrue(text.contains("a"));
        assertTrue(text.contains("b"));
        assertTrue(text.contains("a < b"));
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
        // characterization: "<123>" never produces an element of its own; only the implicit HTML root
        // and the synthesized BODY get start events (the leading '<' is preserved as text).
        final List<String> events = saxEvents("<123>text");
        final long startEventCount = events.stream().filter(e -> e.startsWith("start:")).count();
        assertEquals(2, startEventCount);
        assertEquals("start:HTML", events.get(0));
        assertTrue(events.stream().anyMatch(e -> e.startsWith("chars:") && e.contains("123>text")));
    }

}
