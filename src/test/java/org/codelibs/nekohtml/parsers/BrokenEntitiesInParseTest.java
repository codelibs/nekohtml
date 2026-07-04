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
 * Category K: HTML character entity resolution through the full parse path (DOMParser/SAXParser),
 * as opposed to direct table lookups already covered by {@code HTMLEntitiesTest}.
 *
 * <p>
 * characterization: entity resolution happens in {@code SimpleHTMLScanner#resolveEntities}, applied to
 * every text run and (with different rules) every attribute value. The trailing semicolon is optional
 * for both named and numeric references; invalid numeric code points become U+FFFD; and the "no
 * semicolon + next char is alnum/'='" HTML5 attribute-value-state guard only applies to attribute
 * values, not text.
 * </p>
 */
public class BrokenEntitiesInParseTest {

    @Test
    public void unknownNamedEntityLeftVerbatim() throws Exception {
        // characterization: unrecognized named entities are not decoded and are not even stripped of
        // their trailing semicolon.
        final Document doc = parse("<p>&foo;</p>");
        assertTrue(firstText(doc, "//P").contains("&foo;"));
    }

    @Test
    public void semicolonlessNamedEntityDecodesInText() throws Exception {
        // characterization: the trailing ';' is optional for named entities in text.
        final Document doc = parse("<p>&copy</p>");
        assertTrue(firstText(doc, "//P").contains("©"));
    }

    @Test
    public void semicolonlessStandardEntityAlsoDecodes() throws Exception {
        final Document doc = parse("<p>a &amp b</p>");
        assertTrue(firstText(doc, "//P").contains("a & b"));
    }

    @Test
    public void standardEntitiesDecodeAmpLtGtQuot() throws Exception {
        final Document doc = parse("<p>&amp;&lt;&gt;&quot;</p>");
        assertTrue(firstText(doc, "//P").contains("&<>\""));
    }

    @Test
    public void decimalNumericEntitiesDecode() throws Exception {
        final Document doc = parse("<p>&#65;&#66;</p>");
        assertTrue(firstText(doc, "//P").contains("AB"));
    }

    @Test
    public void hexNumericEntitiesDecodeLowerAndUpperXMarker() throws Exception {
        final Document doc = parse("<p>&#x41;&#X42;</p>");
        assertTrue(firstText(doc, "//P").contains("AB"));
    }

    @Test
    public void semicolonlessDecimalEntityStillDecodes() throws Exception {
        // characterization: the ';' is optional for numeric references too.
        final Document doc = parse("<p>&#65</p>");
        assertTrue(firstText(doc, "//P").contains("A"));
    }

    @Test
    public void nullCodePointBecomesReplacementChar() throws Exception {
        final Document doc = parse("<p>&#0;</p>");
        assertTrue(firstText(doc, "//P").contains("�"));
    }

    @Test
    public void surrogateCodePointBecomesReplacementChar() throws Exception {
        final Document doc = parse("<p>&#xD800;</p>");
        assertTrue(firstText(doc, "//P").contains("�"));
    }

    @Test
    public void noncharacterFfffCodePointBecomesReplacementChar() throws Exception {
        final Document doc = parse("<p>&#xFFFF;</p>");
        assertTrue(firstText(doc, "//P").contains("�"));
    }

    @Test
    public void controlCharacterNumericEntityBecomesReplacementChar() throws Exception {
        // characterization: XML-illegal control characters (other than tab/LF/CR) become U+FFFD too.
        final Document doc = parse("<p>&#1;</p>");
        assertTrue(firstText(doc, "//P").contains("�"));
    }

    @Test
    public void tabNewlineAndCarriageReturnNumericEntitiesArePreserved() throws Exception {
        // characterization: tab/LF/CR are the control-character exceptions and decode normally.
        final Document doc = parse("<p>a&#9;b&#10;c&#13;d</p>");
        final String text = firstText(doc, "//P");
        assertTrue(text.contains("a\tb"));
        assertTrue(text.contains("b\nc") || text.contains("b\r\nc"));
    }

    @Test
    public void nbspDecodesToNonBreakingSpace() throws Exception {
        final Document doc = parse("<p>&nbsp;end</p>");
        assertTrue(firstText(doc, "//P").contains(" end"));
    }

    @Test
    public void supplementaryPlaneNumericEntityDecodesToSurrogatePair() throws Exception {
        // U+1F600 GRINNING FACE - valid but outside the BMP.
        final Document doc = parse("<p>&#128512;</p>");
        final String text = firstText(doc, "//P");
        assertTrue(text.contains(new String(Character.toChars(0x1F600))));
    }

    @Test
    public void outOfUnicodeRangeNumericEntityBecomesReplacementChar() throws Exception {
        final Document doc = parse("<p>&#1114112;</p>");
        assertTrue(firstText(doc, "//P").contains("�"));
    }

    @Test
    public void entityAtStartAndEndOfTextIsDecoded() throws Exception {
        final Document doc = parse("<p>&lt;tag&gt;</p>");
        assertTrue(firstText(doc, "//P").contains("<tag>"));
    }

    @Test
    public void doubleAmpersandWithNoValidNameStaysLiteral() throws Exception {
        // characterization: "&&" never matches the entity pattern (no digits/hex/name follow the
        // first '&'), so both ampersands survive untouched.
        final Document doc = parse("<p>&&</p>");
        assertTrue(firstText(doc, "//P").contains("&&"));
    }

    @Test
    public void trailingAmpersandAtEndOfInputIsPreserved() throws Exception {
        final Document doc = parse("<p>text&</p>");
        assertTrue(firstText(doc, "//P").contains("text&"));
    }

    @Test
    public void invalidHexDigitsAfterHexMarkerStayLiteral() throws Exception {
        // characterization: "&#xZZ;" has no valid hex digits, so the entity pattern never matches
        // here and the whole sequence is left completely untouched.
        final Document doc = parse("<p>&#xZZ;</p>");
        assertTrue(firstText(doc, "//P").contains("&#xZZ;"));
    }

    @Test
    public void adjacentEntitiesAreResolvedInSinglePassNotRecursively() throws Exception {
        // characterization: "&amp;&amp;" decodes to "&&" in one pass; the resulting ampersands are
        // never re-scanned/re-decoded.
        final Document doc = parse("<p>&amp;&amp;</p>");
        assertTrue(firstText(doc, "//P").contains("&&"));
    }

    @Test
    public void multipleConsecutiveMixedEntityFormsAllDecode() throws Exception {
        final Document doc = parse("<p>&amp;&#65;&copy;</p>");
        assertTrue(firstText(doc, "//P").contains("&A©"));
    }

    @Test
    public void plainTextOutsideAnyElementIsAlsoEntityDecoded() throws Exception {
        // characterization: entity resolution runs on every text run regardless of the surrounding
        // element (or lack of one); here the implicit HTML root still gets decoded text.
        final Document doc = parse("Hello &amp; world");
        assertTrue(firstText(doc, "//HTML").contains("Hello & world"));
    }

    @Test
    public void attributeAmpCopyEqualsPreservedVerbatim() throws Exception {
        // characterization: HTML5 attribute-value-state guard - semicolon-less named entity followed
        // by '=' is left untouched in an attribute value (protects URLs like "?x=1&copy=2").
        final Document doc = parse("<a href=\"?x=1&copy=2\">x</a>");
        assertTrue(first(doc, "//A").getAttribute("href").contains("&copy=2"));
    }

    @Test
    public void textAmpCopyEqualsIsDecodedUnlikeAttribute() throws Exception {
        // characterization: the same semicolon-less "&copy=" sequence in TEXT (not an attribute value)
        // decodes normally, since the attribute-only guard does not apply there.
        final Document doc = parse("<p>a&copy=2 b</p>");
        assertTrue(firstText(doc, "//P").contains("a©=2 b"));
    }

    @Test
    public void saxCharactersEventCarriesDecodedEntityText() throws Exception {
        final List<String> events = saxEvents("<p>&amp;</p>");
        assertTrue(events.stream().anyMatch(e -> e.startsWith("chars:") && e.contains("&")));
    }

}
