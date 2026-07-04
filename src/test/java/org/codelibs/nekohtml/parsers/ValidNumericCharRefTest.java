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

import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.firstText;
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Characterization tests locking in end-to-end decimal and hexadecimal numeric character reference
 * decoding through the full {@code DOMParser} parse path, on VALID/well-formed HTML input.
 */
public class ValidNumericCharRefTest {

    @Test
    public void decimalNumericReferencesDecodeToLetters() throws Exception {
        final Document doc = parse("<p>&#65;&#66;&#67;</p>");
        assertEquals("ABC", firstText(doc, "//P"));
    }

    @Test
    public void hexNumericReferenceWithLowerXDecodesToLetter() throws Exception {
        final Document doc = parse("<p>&#x41;</p>");
        assertEquals("A", firstText(doc, "//P"));
    }

    @Test
    public void hexNumericReferenceWithUpperXDecodesToLetter() throws Exception {
        final Document doc = parse("<p>&#X41;</p>");
        assertEquals("A", firstText(doc, "//P"));
    }

    @Test
    public void hexNumericReferenceIsCaseInsensitiveForDigitsLowerCase() throws Exception {
        final Document doc = parse("<p>&#xd6;</p>");
        assertEquals("Ö", firstText(doc, "//P"));
    }

    @Test
    public void hexNumericReferenceIsCaseInsensitiveForDigitsUpperCase() throws Exception {
        final Document doc = parse("<p>&#xD6;</p>");
        assertEquals("Ö", firstText(doc, "//P"));
    }

    @Test
    public void decimalEquivalentOfHexReferenceDecodesToSameChar() throws Exception {
        final Document doc = parse("<p>&#214;</p>");
        assertEquals("Ö", firstText(doc, "//P"));
    }

    @Test
    public void decimalReferenceForAmpersandDecodesToAmpersand() throws Exception {
        final Document doc = parse("<p>&#38;</p>");
        assertEquals("&", firstText(doc, "//P"));
    }

    @Test
    public void decimalReferenceForCopyrightSignDecodes() throws Exception {
        final Document doc = parse("<p>&#169;</p>");
        assertEquals("©", firstText(doc, "//P"));
    }

    @Test
    public void decimalSupplementaryPlaneReferenceDecodesToSurrogatePair() throws Exception {
        final Document doc = parse("<p>&#128512;</p>");
        final String text = firstText(doc, "//P");
        assertEquals(2, text.length());
        assertEquals(0x1F600, text.codePointAt(0));
    }

    @Test
    public void hexSupplementaryPlaneReferenceDecodesToSurrogatePair() throws Exception {
        final Document doc = parse("<p>&#x1F600;</p>");
        final String text = firstText(doc, "//P");
        assertEquals(2, text.length());
        assertEquals(0x1F600, text.codePointAt(0));
    }

    @Test
    public void semicolonOptionalForDecimalReferenceFollowedBySpace() throws Exception {
        // characterization: the trailing ';' is optional for numeric references; a following space
        // terminates the digit run and is preserved as-is.
        final Document doc = parse("<p>&#65 X</p>");
        assertEquals("A X", firstText(doc, "//P"));
    }

    @Test
    public void nullCodePointReferenceBecomesReplacementChar() throws Exception {
        final Document doc = parse("<p>&#0;</p>");
        assertEquals("�", firstText(doc, "//P"));
    }

    @Test
    public void surrogateCodePointReferenceBecomesReplacementChar() throws Exception {
        final Document doc = parse("<p>&#xD800;</p>");
        assertEquals("�", firstText(doc, "//P"));
    }

    @Test
    public void outOfUnicodeRangeReferenceBecomesReplacementChar() throws Exception {
        final Document doc = parse("<p>&#x110000;</p>");
        assertEquals("�", firstText(doc, "//P"));
    }

    @Test
    public void tabReferenceIsPreserved() throws Exception {
        final Document doc = parse("<p>a&#9;b</p>");
        // characterization: tab (0x9) is one of the XML-legal control-character exceptions and
        // survives numeric-reference decoding unchanged.
        assertEquals("a\tb", firstText(doc, "//P"));
    }

    @Test
    public void newlineReferenceIsPreserved() throws Exception {
        final Document doc = parse("<p>a&#10;b</p>");
        // characterization: line-feed (0xA) survives decoding as a literal '\n' character.
        assertEquals("a\nb", firstText(doc, "//P"));
    }

    @Test
    public void carriageReturnReferenceIsPreserved() throws Exception {
        final Document doc = parse("<p>a&#13;b</p>");
        // characterization: carriage-return (0xD) survives decoding as a literal '\r' character; it is
        // not normalized to '\n' since normalization only applies to raw line endings in the source,
        // not to characters produced by entity decoding.
        assertEquals("a\rb", firstText(doc, "//P"));
    }

    @Test
    public void combinedTabNewlineCarriageReturnReferencesAreAllPreserved() throws Exception {
        final Document doc = parse("<p>&#9;&#10;&#13;</p>");
        assertEquals("\t\n\r", firstText(doc, "//P"));
    }

    @Test
    public void hexLowerCaseDigitsDecodeHeavyBlackHeart() throws Exception {
        final Document doc = parse("<p>&#x2764;</p>");
        assertEquals("❤", firstText(doc, "//P"));
    }

    @Test
    public void hexUpperCaseDigitsDecodeHeavyBlackHeart() throws Exception {
        final Document doc = parse("<p>&#X2764;</p>");
        assertEquals("❤", firstText(doc, "//P"));
    }

    @Test
    public void controlCharacterOutsideTabLfCrBecomesReplacementChar() throws Exception {
        // characterization: XML-illegal control characters other than tab/LF/CR become U+FFFD.
        final Document doc = parse("<p>&#1;</p>");
        assertEquals("�", firstText(doc, "//P"));
    }

    @Test
    public void noncharacterFffeCodePointBecomesReplacementChar() throws Exception {
        final Document doc = parse("<p>&#xFFFE;</p>");
        assertEquals("�", firstText(doc, "//P"));
    }

    @Test
    public void noncharacterFfffCodePointBecomesReplacementChar() throws Exception {
        final Document doc = parse("<p>&#xFFFF;</p>");
        assertEquals("�", firstText(doc, "//P"));
    }

    @Test
    public void multipleDecimalReferencesInSequenceAllDecode() throws Exception {
        final Document doc = parse("<p>&#72;&#101;&#108;&#108;&#111;</p>");
        assertEquals("Hello", firstText(doc, "//P"));
    }

    @Test
    public void mixedDecimalAndHexReferencesBothDecodeInSameText() throws Exception {
        final Document doc = parse("<p>&#65;&#x42;&#67;</p>");
        assertEquals("ABC", firstText(doc, "//P"));
    }
}
