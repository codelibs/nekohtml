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

import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.first;
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.firstText;
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Characterization tests locking in end-to-end HTML entity decoding (named and numeric) through the
 * full {@code DOMParser} parse path, for both text content and attribute values, on VALID/well-formed
 * HTML input.
 */
public class ValidEntityDecodingTest {

    // -----------------------------------------------------------------
    // Named entities in text
    // -----------------------------------------------------------------

    @Test
    public void ampEntityDecodesInText() throws Exception {
        final Document doc = parse("<p>Tom &amp; Jerry</p>");
        assertEquals("Tom & Jerry", firstText(doc, "//P"));
    }

    @Test
    public void ltGtEntitiesDecodeToAngleBrackets() throws Exception {
        final Document doc = parse("<p>&lt;tag&gt;</p>");
        assertEquals("<tag>", firstText(doc, "//P"));
    }

    @Test
    public void copyEntityDecodesToCopyrightSign() throws Exception {
        final Document doc = parse("<p>&copy; 2024</p>");
        assertEquals("© 2024", firstText(doc, "//P"));
    }

    @Test
    public void eacuteEntityDecodesInsideWord() throws Exception {
        final Document doc = parse("<p>caf&eacute;</p>");
        assertEquals("café", firstText(doc, "//P"));
    }

    @Test
    public void mdashEntityDecodesToEmDash() throws Exception {
        final Document doc = parse("<p>a &mdash; b</p>");
        assertEquals("a — b", firstText(doc, "//P"));
    }

    @Test
    public void nbspEntityDecodesToNonBreakingSpaceNotRegularSpace() throws Exception {
        final Document doc = parse("<p>&nbsp;end</p>");
        final String text = firstText(doc, "//P");
        // characterization: &nbsp; decodes to U+00A0 NO-BREAK SPACE, not a regular U+0020 space.
        assertEquals(' ', text.charAt(0));
        assertEquals(" end", text);
    }

    @Test
    public void quotEntityDecodesToDoubleQuote() throws Exception {
        final Document doc = parse("<p>&quot;q&quot;</p>");
        assertEquals("\"q\"", firstText(doc, "//P"));
    }

    @Test
    public void aposEntityDecodesToApostrophe() throws Exception {
        final Document doc = parse("<p>&apos;a&apos;</p>");
        assertEquals("'a'", firstText(doc, "//P"));
    }

    @Test
    public void upperCaseAmpEntityIsCaseSensitiveAndNotDecoded() throws Exception {
        // characterization: entity name lookup is case-sensitive; "AMP" is not a known entity name
        // (only lower-case "amp" is), so "&AMP;" is left completely literal.
        final Document doc = parse("<p>&AMP;</p>");
        assertEquals("&AMP;", firstText(doc, "//P"));
    }

    @Test
    public void unknownNamedEntityIsLeftLiteralInText() throws Exception {
        final Document doc = parse("<p>&foobar;</p>");
        assertEquals("&foobar;", firstText(doc, "//P"));
    }

    @Test
    public void semicolonOptionalForNamedEntityInText() throws Exception {
        // characterization: the trailing ';' is optional for named entities in text context.
        final Document doc = parse("<p>&copy 2024</p>");
        assertEquals("© 2024", firstText(doc, "//P"));
    }

    @Test
    public void regEntityDecodesToRegisteredSign() throws Exception {
        final Document doc = parse("<p>&reg;</p>");
        assertEquals("®", firstText(doc, "//P"));
    }

    @Test
    public void tradeEntityDecodesToTradeMarkSign() throws Exception {
        final Document doc = parse("<p>&trade;</p>");
        assertEquals("™", firstText(doc, "//P"));
    }

    @Test
    public void hellipEntityDecodesToHorizontalEllipsis() throws Exception {
        final Document doc = parse("<p>&hellip;</p>");
        assertEquals("…", firstText(doc, "//P"));
    }

    @Test
    public void ndashEntityDecodesToEnDash() throws Exception {
        final Document doc = parse("<p>&ndash;</p>");
        assertEquals("–", firstText(doc, "//P"));
    }

    @Test
    public void lsquoEntityDecodesToLeftSingleQuote() throws Exception {
        final Document doc = parse("<p>&lsquo;</p>");
        assertEquals("‘", firstText(doc, "//P"));
    }

    @Test
    public void rsquoEntityDecodesToRightSingleQuote() throws Exception {
        final Document doc = parse("<p>&rsquo;</p>");
        assertEquals("’", firstText(doc, "//P"));
    }

    @Test
    public void ldquoEntityDecodesToLeftDoubleQuote() throws Exception {
        final Document doc = parse("<p>&ldquo;</p>");
        assertEquals("“", firstText(doc, "//P"));
    }

    @Test
    public void rdquoEntityDecodesToRightDoubleQuote() throws Exception {
        final Document doc = parse("<p>&rdquo;</p>");
        assertEquals("”", firstText(doc, "//P"));
    }

    @Test
    public void degEntityDecodesToDegreeSign() throws Exception {
        final Document doc = parse("<p>&deg;</p>");
        assertEquals("°", firstText(doc, "//P"));
    }

    @Test
    public void timesEntityDecodesToMultiplicationSign() throws Exception {
        final Document doc = parse("<p>&times;</p>");
        assertEquals("×", firstText(doc, "//P"));
    }

    @Test
    public void divideEntityDecodesToDivisionSign() throws Exception {
        final Document doc = parse("<p>&divide;</p>");
        assertEquals("÷", firstText(doc, "//P"));
    }

    // -----------------------------------------------------------------
    // Named entities in attribute values
    // -----------------------------------------------------------------

    @Test
    public void ampEntityDecodesInHrefAttribute() throws Exception {
        final Document doc = parse("<a href=\"?a=1&amp;b=2\">x</a>");
        assertEquals("?a=1&b=2", first(doc, "//A").getAttribute("href"));
    }

    @Test
    public void copyEntityDecodesInsideAttributeValue() throws Exception {
        final Document doc = parse("<a href=\"x&copy;z\">x</a>");
        assertEquals("x©z", first(doc, "//A").getAttribute("href"));
    }

    @Test
    public void numericHexEntityDecodesInAltAttribute() throws Exception {
        final Document doc = parse("<img alt=\"&#169; me\">");
        assertEquals("© me", first(doc, "//IMG").getAttribute("alt"));
    }

    @Test
    public void semicolonlessNamedEntityFollowedByEqualsStaysLiteralInAttribute() throws Exception {
        // characterization: HTML5 attribute-value-state guard - a semicolon-less named entity
        // immediately followed by '=' or an alphanumeric is NOT decoded in an attribute value, so
        // query strings like "?not=1&copy=2" survive untouched.
        final Document doc = parse("<a href=\"q?not=1&copy=2\">x</a>");
        assertEquals("q?not=1&copy=2", first(doc, "//A").getAttribute("href"));
    }

    @Test
    public void ltGtEntitiesDecodeInTitleAttribute() throws Exception {
        final Document doc = parse("<a title=\"&lt;x&gt;\">x</a>");
        assertEquals("<x>", first(doc, "//A").getAttribute("title"));
    }

    @Test
    public void aposEntityDecodesInAttributeValue() throws Exception {
        final Document doc = parse("<a title=\"&apos;quoted&apos;\">x</a>");
        assertEquals("'quoted'", first(doc, "//A").getAttribute("title"));
    }

    @Test
    public void semicolonlessNamedEntityFollowedByNonAlnumDecodesInAttribute() throws Exception {
        // characterization: the attribute-value-state guard only blocks decoding when the next
        // character is alphanumeric or '='; a following space still allows decoding.
        final Document doc = parse("<a title=\"&copy 2024\">x</a>");
        assertEquals("© 2024", first(doc, "//A").getAttribute("title"));
    }
}
