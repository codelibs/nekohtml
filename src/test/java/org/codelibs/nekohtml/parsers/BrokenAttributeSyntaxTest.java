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
        // characterization: a '>' inside a quoted value ends the START_TAG match early;
        // the quote character itself becomes part of the (unquoted) attribute value,
        // and the remaining "y">" leaks as sibling text
        final Document doc = parse("<a title=\"x>y\">");
        final Element a = first(doc, "//A");
        assertNotNull(a);
        assertEquals("\"x", a.getAttribute("title"));
        assertTrue(firstText(doc, "//HTML").contains("y\">"));
    }

    @Test
    public void unterminatedQuoteAttributeValueKeepsLeadingQuote() throws Exception {
        // characterization: an unterminated quoted value falls back to the unquoted alternative,
        // so the leading quote character is preserved literally in the attribute value
        final Document doc = parse("<a href=\"x>");
        final Element a = first(doc, "//A");
        assertNotNull(a);
        assertEquals("\"x", a.getAttribute("href"));
    }

    @Test
    public void duplicateAttributeLastValueWins() throws Exception {
        // characterization: duplicate attribute names are all parsed, but DOM Element.setAttribute()
        // is called once per occurrence in order, so the last occurrence wins
        final Document doc = parse("<a id=1 id=2>x</a>");
        final Element a = first(doc, "//A");
        assertEquals("2", a.getAttribute("id"));
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
        // characterization: a stray leading '=' does not match the attribute-name pattern and is
        // simply skipped; the following bare token is parsed as a normal valueless attribute
        final Document doc = parse("<a =x>y</a>");
        final Element a = first(doc, "//A");
        assertNotNull(a);
        assertEquals("", a.getAttribute("x"));
        assertEquals(1, a.getAttributes().getLength());
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
        // characterization: an attribute "name" cannot start with a digit; "123=" is silently skipped
        // and only the trailing bare "x" is recognized as a valueless attribute
        final Document doc = parse("<a 123=x>y</a>");
        final Element a = first(doc, "//A");
        assertEquals(1, a.getAttributes().getLength());
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
        // characterization: same fallback as double quotes, but for a single quote character
        final Document doc = parse("<a href='x>");
        final Element a = first(doc, "//A");
        assertNotNull(a);
        assertEquals("'x", a.getAttribute("href"));
    }

    @Test
    public void leadingHyphenOnAttributeNameIsDroppedAndNextLetterStartsName() throws Exception {
        // characterization: an attribute "name" cannot start with '-'; the parser resyncs on the next
        // letter, so "-foo=\"bar\"" is parsed as attribute "foo" with value "bar"
        final Document doc = parse("<div -foo=\"bar\">x</div>");
        final Element div = first(doc, "//DIV");
        assertFalse(div.hasAttribute("-foo"));
        assertEquals("bar", div.getAttribute("foo"));
    }

    @Test
    public void greaterThanInsideSingleQuotedValueEndsTagEarly() throws Exception {
        // characterization: mirrors the double-quote case; the tag ends at the first raw '>' regardless
        // of the (single) quote character, leaking the remainder as text
        final Document doc = parse("<a title='x>y'>");
        final Element a = first(doc, "//A");
        assertNotNull(a);
        assertEquals("'x", a.getAttribute("title"));
        assertTrue(firstText(doc, "//HTML").contains("y'>"));
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
