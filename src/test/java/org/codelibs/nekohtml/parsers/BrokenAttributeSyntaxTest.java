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

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Characterization tests for broken/malformed attribute syntax (category H).
 * These tests lock in the current regex-based attribute tokenization behavior;
 * some of it is intentionally non-standard compared to a real HTML5 parser.
 */
public class BrokenAttributeSyntaxTest {

    @Test
    public void unquotedAttributesSplitOnWhitespace() throws Exception {
        // characterization: unquoted values are split on whitespace; the next bare token becomes a valueless attribute
        final Document doc = parse("<a b=c d>x</a>");
        final Element a = first(doc, "//A");
        assertEquals("c", a.getAttribute("b"));
        assertEquals("", a.getAttribute("d"));
    }

    @Test
    public void quotedGreaterThanEndsTagEarlyLeaksText() throws Exception {
        // characterization: the scanner is quote-aware, so a '>' inside a quoted value does NOT end
        // the tag early; the full value "x>y" is captured and no text leaks as sibling text
        final Document doc = parse("<a title=\"x>y\">");
        final Element a = first(doc, "//A");
        assertNotNull(a);
        assertEquals("x>y", a.getAttribute("title"));
        assertEquals("", firstText(doc, "//HTML"));
    }

    @Test
    public void unterminatedQuoteAttributeValueKeepsLeadingQuote() throws Exception {
        // characterization: an unterminated quoted value recovers to the value without the leading
        // quote (the quote is the delimiter, not part of the value = author intent)
        final Document doc = parse("<a href=\"x>");
        final Element a = first(doc, "//A");
        assertNotNull(a);
        assertEquals("x", a.getAttribute("href"));
    }

    @Test
    public void duplicateAttributeLastValueWins() throws Exception {
        // characterization: duplicate attribute names keep the FIRST occurrence (HTML5 rule: a
        // duplicate-attribute parse error drops later duplicates)
        final Document doc = parse("<a id=1 id=2>x</a>");
        final Element a = first(doc, "//A");
        assertEquals("1", a.getAttribute("id"));
    }

    @Test
    public void valuelessBooleanAttributeBecomesEmptyString() throws Exception {
        final Document doc = parse("<input disabled>");
        final Element input = first(doc, "//INPUT");
        assertEquals("", input.getAttribute("disabled"));
        assertTrue(input.hasAttribute("disabled"));
    }

    @Test
    public void bareEqualsSignIsSkippedAndNextTokenBecomesAttribute() throws Exception {
        // characterization: a stray leading '=' yields an empty attribute name, which is dropped;
        // the malformed token produces no attribute at all (no phantom "x" attribute)
        final Document doc = parse("<a =x>y</a>");
        final Element a = first(doc, "//A");
        assertNotNull(a);
        assertEquals("", a.getAttribute("x"));
        assertEquals(0, a.getAttributes().getLength());
    }

    @Test
    public void multipleEqualsSignsAreKeptInUnquotedValue() throws Exception {
        // characterization: unquoted value scanning does not stop at '=', so "a=b" is captured whole
        final Document doc = parse("<div class=a=b>x</div>");
        final Element div = first(doc, "//DIV");
        assertEquals("a=b", div.getAttribute("class"));
    }

    @Test
    public void ampCopyPreservedInAttributeValue() throws Exception {
        // characterization: a semicolon-less named entity followed by '=' is left undecoded in
        // attribute context (HTML5 attribute-value-state rule), preventing URL corruption
        final Document doc = parse("<a href=\"?x=1&copy=2\">x</a>");
        assertTrue(first(doc, "//A").getAttribute("href").contains("&copy=2"));
    }

    @Test
    public void attributeNameLowercasedValuePreserved() throws Exception {
        final Document doc = parse("<a HREF=\"X\">x</a>");
        final Element a = first(doc, "//A");
        assertTrue(a.hasAttribute("href"));
        assertFalse(a.hasAttribute("HREF"));
        assertEquals("X", a.getAttribute("href"));
    }

    @Test
    public void attributeSeparatedByNewlineInsideTag() throws Exception {
        final Document doc = parse("<div\ndata-x=\"1\">x</div>");
        final Element div = first(doc, "//DIV");
        assertEquals("1", div.getAttribute("data-x"));
    }

    @Test
    public void doubleEqualsKeepsLeadingEqualsInValue() throws Exception {
        // characterization: "b==c" is parsed as name "b" with unquoted value "=c"
        final Document doc = parse("<a b==c>x</a>");
        final Element a = first(doc, "//A");
        assertEquals("=c", a.getAttribute("b"));
    }

    @Test
    public void adjacentQuotedAttributesWithoutWhitespaceSeparator() throws Exception {
        // characterization: no whitespace is required between a quoted value and the next attribute name
        final Document doc = parse("<a b=\"c\"d=\"e\">x</a>");
        final Element a = first(doc, "//A");
        assertEquals("c", a.getAttribute("b"));
        assertEquals("e", a.getAttribute("d"));
    }

    @Test
    public void numericAttributeNamePrefixIsDropped() throws Exception {
        // characterization: the name "123" is read but is not a valid XML Name, so the whole
        // attribute is dropped; no phantom valueless "x" attribute is fabricated
        final Document doc = parse("<a 123=x>y</a>");
        final Element a = first(doc, "//A");
        assertEquals(0, a.getAttributes().getLength());
        assertEquals("", a.getAttribute("x"));
        assertFalse(a.hasAttribute("123"));
    }

    @Test
    public void bareAttributeFollowedByAttributeWithValue() throws Exception {
        final Document doc = parse("<a b c=d>x</a>");
        final Element a = first(doc, "//A");
        assertEquals("", a.getAttribute("b"));
        assertEquals("d", a.getAttribute("c"));
    }

    @Test
    public void selfClosingSlashIsNotParsedAsAttribute() throws Exception {
        final Document doc = parse("<a href=\"x\" />y</a>");
        final Element a = first(doc, "//A");
        assertEquals("x", a.getAttribute("href"));
        assertEquals(1, a.getAttributes().getLength());
    }

    @Test
    public void numericEntityDecodedInAttributeValue() throws Exception {
        // characterization: numeric character references always decode, even in attribute context
        final Document doc = parse("<a href=\"a&#38;b\">x</a>");
        assertEquals("a&b", first(doc, "//A").getAttribute("href"));
    }

    @Test
    public void namedEntityWithSemicolonDecodesInAttribute() throws Exception {
        // characterization: once a trailing ';' is present, the attribute-context suppression rule
        // does not apply and the named entity decodes normally
        final Document doc = parse("<a href=\"?x=1&copy;2\">x</a>");
        assertTrue(first(doc, "//A").getAttribute("href").contains("©2"));
    }

    @Test
    public void namedEntityWithoutSemicolonDecodesWhenFollowedByNonAlnum() throws Exception {
        // characterization: the attribute suppression rule only triggers when the char right after
        // the entity name is alnum or '='; a following space still lets it decode
        final Document doc = parse("<a title=\"x &copy y\">x</a>");
        assertTrue(first(doc, "//A").getAttribute("title").contains("©"));
    }

    @Test
    public void unterminatedSingleQuoteAttributeValueKeepsLeadingQuote() throws Exception {
        // characterization: same recovery as double quotes — the leading single quote is the
        // delimiter and is not part of the recovered value
        final Document doc = parse("<a href='x>");
        final Element a = first(doc, "//A");
        assertNotNull(a);
        assertEquals("x", a.getAttribute("href"));
    }

    @Test
    public void leadingHyphenOnAttributeNameIsDroppedAndNextLetterStartsName() throws Exception {
        // characterization: the name "-foo" is read but is not a valid XML Name, so the attribute is
        // dropped entirely; no "foo" attribute is fabricated (drop, not rename)
        final Document doc = parse("<div -foo=\"bar\">x</div>");
        final Element div = first(doc, "//DIV");
        assertFalse(div.hasAttribute("-foo"));
        assertEquals("", div.getAttribute("foo"));
    }

    @Test
    public void greaterThanInsideSingleQuotedValueEndsTagEarly() throws Exception {
        // characterization: mirrors the double-quote case; the scanner is quote-aware, so the '>'
        // inside the single-quoted value does not end the tag and nothing leaks as text
        final Document doc = parse("<a title='x>y'>");
        final Element a = first(doc, "//A");
        assertNotNull(a);
        assertEquals("x>y", a.getAttribute("title"));
        assertEquals("", firstText(doc, "//HTML"));
    }

    @Test
    public void mixedSingleAndDoubleQuotesOnDifferentAttributes() throws Exception {
        final Document doc = parse("<div id='a' class=\"b\">x</div>");
        final Element div = first(doc, "//DIV");
        assertEquals("a", div.getAttribute("id"));
        assertEquals("b", div.getAttribute("class"));
    }

    @Test
    public void manyAttributeAssignmentsAllParsedInOrder() throws Exception {
        final Document doc = parse("<a b=1 c=2 d=3 e=4>x</a>");
        final Element a = first(doc, "//A");
        assertEquals("1", a.getAttribute("b"));
        assertEquals("2", a.getAttribute("c"));
        assertEquals("3", a.getAttribute("d"));
        assertEquals("4", a.getAttribute("e"));
        assertEquals(4, a.getAttributes().getLength());
    }
}
