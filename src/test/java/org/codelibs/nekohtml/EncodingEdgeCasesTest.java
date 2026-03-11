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
package org.codelibs.nekohtml;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.codelibs.nekohtml.parsers.DOMParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Tests for character encoding edge cases, special characters, BOM handling,
 * and entity resolution edge cases.
 *
 * @author CodeLibs Project
 */
public class EncodingEdgeCasesTest {

    private DOMParser parser;

    @BeforeEach
    public void setUp() throws Exception {
        parser = new DOMParser();
    }

    private Document parseHTML(final String html) throws Exception {
        parser.parse(new InputSource(new StringReader(html)));
        return parser.getDocument();
    }

    private Document parseHTMLBytes(final byte[] bytes, final String encoding) throws Exception {
        final InputStream is = new ByteArrayInputStream(bytes);
        final InputSource source = new InputSource(is);
        if (encoding != null) {
            source.setEncoding(encoding);
        }
        parser.parse(source);
        return parser.getDocument();
    }

    // ========================================================================
    // BOM (Byte Order Mark) Tests
    // ========================================================================

    @Test
    public void testUTF8BOM() throws Exception {
        // Given: HTML with UTF-8 BOM
        final byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        final byte[] html = "<html><body>UTF-8 with BOM</body></html>".getBytes(StandardCharsets.UTF_8);
        final byte[] combined = new byte[bom.length + html.length];
        System.arraycopy(bom, 0, combined, 0, bom.length);
        System.arraycopy(html, 0, combined, bom.length, html.length);

        // When: Parsing
        final Document doc = parseHTMLBytes(combined, "UTF-8");

        // Then: Should parse correctly ignoring BOM
        assertNotNull(doc, "Document should be parsed");
        final String bodyText = doc.getElementsByTagName("BODY").item(0).getTextContent();
        assertEquals("UTF-8 with BOM", bodyText.trim(), "Should parse content correctly");
    }

    @Test
    public void testUTF16BEBOM() throws Exception {
        // Given: HTML with UTF-16 BE BOM
        final byte[] bom = new byte[] { (byte) 0xFE, (byte) 0xFF };
        final byte[] html = "<html><body>UTF-16 BE</body></html>".getBytes(StandardCharsets.UTF_16BE);
        final byte[] combined = new byte[bom.length + html.length];
        System.arraycopy(bom, 0, combined, 0, bom.length);
        System.arraycopy(html, 0, combined, bom.length, html.length);

        // When: Parsing
        final Document doc = parseHTMLBytes(combined, "UTF-16BE");

        // Then: Should parse correctly with BOM
        assertNotNull(doc, "Document should be parsed");
        final String bodyText = doc.getElementsByTagName("BODY").item(0).getTextContent();
        assertTrue(bodyText.contains("UTF-16"), "Should parse UTF-16 content");
    }

    @Test
    public void testUTF16LEBOM() throws Exception {
        // Given: HTML with UTF-16 LE BOM
        final byte[] bom = new byte[] { (byte) 0xFF, (byte) 0xFE };
        final byte[] html = "<html><body>UTF-16 LE</body></html>".getBytes(StandardCharsets.UTF_16LE);
        final byte[] combined = new byte[bom.length + html.length];
        System.arraycopy(bom, 0, combined, 0, bom.length);
        System.arraycopy(html, 0, combined, bom.length, html.length);

        // When: Parsing
        final Document doc = parseHTMLBytes(combined, "UTF-16LE");

        // Then: Should parse correctly with BOM
        assertNotNull(doc, "Document should be parsed");
        final String bodyText = doc.getElementsByTagName("BODY").item(0).getTextContent();
        assertTrue(bodyText.contains("UTF-16"), "Should parse UTF-16 content");
    }

    // ========================================================================
    // Special Unicode Characters
    // ========================================================================

    @Test
    public void testZeroWidthCharacters() throws Exception {
        // Given: HTML with zero-width characters
        final String html =
                "<html><body>" + "Zero\u200BWidth\u200BSpace " + "Zero\u200CWidth\u200CNon\u200CJoiner " + "Zero\u200DWidth\u200DJoiner"
                        + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should preserve zero-width characters
        assertNotNull(doc, "Document should be parsed");
        final String bodyText = doc.getElementsByTagName("BODY").item(0).getTextContent();
        assertTrue(bodyText.contains("\u200B"), "Should contain ZWSP");
        assertTrue(bodyText.contains("\u200C"), "Should contain ZWNJ");
        assertTrue(bodyText.contains("\u200D"), "Should contain ZWJ");
    }

    @Test
    public void testRightToLeftMarks() throws Exception {
        // Given: HTML with RTL and LTR marks
        final String html =
                "<html><body>" + "Text\u200Ewith\u200ELTR\u200Emarks " + "Text\u200Fwith\u200FRTL\u200Fmarks" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should preserve directional marks
        assertNotNull(doc, "Document should be parsed");
        final String bodyText = doc.getElementsByTagName("BODY").item(0).getTextContent();
        assertTrue(bodyText.contains("\u200E"), "Should contain LRM");
        assertTrue(bodyText.contains("\u200F"), "Should contain RLM");
    }

    @Test
    public void testCombiningCharacters() throws Exception {
        // Given: HTML with combining diacritics
        final String html = "<html><body>" + "e\u0301 " // é (e + combining acute)
                + "n\u0303 " // ñ (n + combining tilde)
                + "a\u0308 " // ä (a + combining diaeresis)
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should preserve combining characters
        assertNotNull(doc, "Document should be parsed");
        final String bodyText = doc.getElementsByTagName("BODY").item(0).getTextContent();
        assertTrue(bodyText.contains("\u0301"), "Should contain combining acute");
        assertTrue(bodyText.contains("\u0303"), "Should contain combining tilde");
        assertTrue(bodyText.contains("\u0308"), "Should contain combining diaeresis");
    }

    @Test
    public void testEmojiAndSupplementaryCharacters() throws Exception {
        // Given: HTML with emoji (supplementary characters)
        final String html = "<html><body>" + "😀😁😂🤣😃😄😅😆😉😊 " + "👍👎👏🙌🎉🎊🎈 " + "🌟⭐✨💫" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle emoji correctly
        assertNotNull(doc, "Document should be parsed");
        final String bodyText = doc.getElementsByTagName("BODY").item(0).getTextContent();
        assertTrue(bodyText.contains("😀"), "Should contain emoji");
        assertTrue(bodyText.contains("👍"), "Should contain thumbs up emoji");
        assertTrue(bodyText.contains("🌟"), "Should contain star emoji");
    }

    @Test
    public void testSurrogatePairs() throws Exception {
        // Given: HTML with characters requiring surrogate pairs
        final String html = "<html><body>" + "\uD834\uDD1E" // Musical symbol G clef (U+1D11E)
                + "\uD835\uDC00" // Mathematical bold capital A (U+1D400)
                + "\uD83D\uDE00" // Grinning face emoji (U+1F600)
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle surrogate pairs
        assertNotNull(doc, "Document should be parsed");
        final String bodyText = doc.getElementsByTagName("BODY").item(0).getTextContent();
        assertTrue(bodyText.length() > 0, "Should have content");
    }

    @Test
    public void testControlCharacters() throws Exception {
        // Given: HTML with control characters (allowed ones)
        final String html = "<html><body>" + "Tab:\t Newline:\n CarriageReturn:\r" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle control characters
        assertNotNull(doc, "Document should be parsed");
        final String bodyText = doc.getElementsByTagName("BODY").item(0).getTextContent();
        assertTrue(bodyText.contains("Tab:"), "Should contain tab context");
    }

    @Test
    public void testMultilingualContent() throws Exception {
        // Given: HTML with multiple languages
        final String html =
                "<html><body>" + "<p lang=\"ja\">日本語のテキスト</p>" + "<p lang=\"zh\">中文文本</p>" + "<p lang=\"ko\">한국어 텍스트</p>"
                        + "<p lang=\"ar\">نص عربي</p>" + "<p lang=\"ru\">Русский текст</p>" + "<p lang=\"el\">Ελληνικό κείμενο</p>"
                        + "<p lang=\"he\">טקסט עברי</p>" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle multilingual content
        assertNotNull(doc, "Document should be parsed");
        final NodeList paragraphs = doc.getElementsByTagName("P");
        assertEquals(7, paragraphs.getLength(), "Should have 7 paragraphs");

        // Verify Japanese text
        final Element jaP = (Element) paragraphs.item(0);
        assertTrue(jaP.getTextContent().contains("日本語"), "Should contain Japanese text");

        // Verify Chinese text
        final Element zhP = (Element) paragraphs.item(1);
        assertTrue(zhP.getTextContent().contains("中文"), "Should contain Chinese text");

        // Verify Arabic text
        final Element arP = (Element) paragraphs.item(3);
        assertTrue(arP.getTextContent().contains("عربي"), "Should contain Arabic text");
    }

    // ========================================================================
    // Entity Edge Cases
    // ========================================================================

    @Test
    public void testMalformedEntities() throws Exception {
        // Given: HTML with malformed entities
        final String html = "<html><body>" + "&invalid; " + "&notanentity; " + "&lt " // missing semicolon
                + "&gt " // missing semicolon
                + "&amp" // missing semicolon
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle malformed entities gracefully
        assertNotNull(doc, "Document should be parsed");
        final String bodyText = doc.getElementsByTagName("BODY").item(0).getTextContent();
        assertNotNull(bodyText, "Body should have text content");
    }

    @Test
    public void testNumericEntitiesOutOfRange() throws Exception {
        // Given: HTML with out-of-range numeric entities
        final String html = "<html><body>" + "&#xFFFFFFFF; " // way out of range
                + "&#999999999; " // huge decimal
                + "&#x110000; " // just beyond Unicode range
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle out-of-range entities
        assertNotNull(doc, "Document should be parsed");
        assertNotNull(doc.getElementsByTagName("BODY").item(0), "Body should exist");
    }

    @Test
    public void testIncompleteEntityAtEOF() throws Exception {
        // Given: HTML with incomplete entity at end of file
        final String html = "<html><body>Text &amp";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle incomplete entity
        assertNotNull(doc, "Document should be parsed");
        assertNotNull(doc.getElementsByTagName("BODY").item(0), "Body should exist");
    }

    @Test
    public void testEntitiesInAttributeValues() throws Exception {
        // Given: HTML with entities in attribute values
        final String html =
                "<html><body>" + "<div title=\"&lt;Click &amp; drag&gt;\">Content</div>"
                        + "<a href=\"?param1=value1&amp;param2=value2\">Link</a>" + "<input value=\"&quot;Quoted&quot;\">"
                        + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse attributes (entities may or may not be decoded depending on parser)
        assertNotNull(doc, "Document should be parsed");

        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        final String title = div.getAttribute("title");
        assertNotNull(title, "Should have title attribute");
        assertTrue(title.length() > 0, "Title should not be empty");

        final Element a = (Element) doc.getElementsByTagName("A").item(0);
        final String href = a.getAttribute("href");
        assertNotNull(href, "Should have href attribute");
        assertTrue(href.contains("param"), "Should have parameters");

        final Element input = (Element) doc.getElementsByTagName("INPUT").item(0);
        final String value = input.getAttribute("value");
        assertNotNull(value, "Should have value attribute");
    }

    @Test
    public void testAllCommonHTMLEntities() throws Exception {
        // Given: HTML with all common entities
        final String html =
                "<html><body>" + "&nbsp; &lt; &gt; &amp; &quot; &apos; " + "&copy; &reg; &trade; " + "&euro; &pound; &yen; &cent; "
                        + "&mdash; &ndash; &hellip; " + "&laquo; &raquo; &ldquo; &rdquo; &lsquo; &rsquo; "
                        + "&deg; &plusmn; &times; &divide; " + "&para; &sect; &dagger; &Dagger; " + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse HTML with entities (entities may be preserved or resolved)
        assertNotNull(doc, "Document should be parsed");
        final String bodyText = doc.getElementsByTagName("BODY").item(0).getTextContent();
        assertNotNull(bodyText, "Body text should not be null");
        assertTrue(bodyText.length() > 0, "Body should have content");

        // Note: Entity resolution depends on parser configuration
        // The test verifies the document can be parsed successfully
    }

    @Test
    public void testNumericCharacterReferences() throws Exception {
        // Given: HTML with numeric character references
        final String html = "<html><body>" + "&#65; " // A
                + "&#x41; " // A (hex)
                + "&#169; " // ©
                + "&#x00A9; " // © (hex)
                + "&#128512; " // 😀
                + "&#x1F600; " // 😀 (hex)
                + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse HTML with numeric references
        assertNotNull(doc, "Document should be parsed");
        final String bodyText = doc.getElementsByTagName("BODY").item(0).getTextContent();
        assertNotNull(bodyText, "Body text should not be null");
        assertTrue(bodyText.length() > 0, "Body should have content");

        // Note: Numeric character reference resolution depends on parser configuration
    }

    @Test
    public void testEntitiesWithoutSemicolon() throws Exception {
        // Given: HTML with entities without semicolons (legacy)
        final String html = "<html><body>" + "&lt &gt &amp &copy &reg" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle entities without semicolons
        assertNotNull(doc, "Document should be parsed");
        assertNotNull(doc.getElementsByTagName("BODY").item(0), "Body should exist");
    }

    // ========================================================================
    // Mixed Encoding Scenarios
    // ========================================================================

    @Test
    public void testMultipleMetaCharsetDeclarations() throws Exception {
        // Given: HTML with multiple conflicting charset declarations
        final String html =
                "<html><head>" + "<meta charset=\"ISO-8859-1\">" + "<meta charset=\"UTF-8\">"
                        + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=Windows-1252\">"
                        + "</head><body>Content</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle multiple charset declarations (first one typically wins)
        assertNotNull(doc, "Document should be parsed");
        final NodeList metas = doc.getElementsByTagName("META");
        assertTrue(metas.getLength() >= 2, "Should have multiple META elements");
    }

    @Test
    public void testMetaCharsetVariations() throws Exception {
        // Given: HTML with various meta charset formats
        final String html =
                "<html><head>" + "<meta charset=\"utf-8\">" + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">"
                        + "<meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8\">" + "</head><body>Test</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse various charset formats
        assertNotNull(doc, "Document should be parsed");
        final NodeList metas = doc.getElementsByTagName("META");
        assertEquals(3, metas.getLength(), "Should have 3 META elements");
    }

    @Test
    public void testXMLDeclarationVsMetaCharset() throws Exception {
        // Given: HTML with both XML declaration and meta charset
        final String html =
                "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"
                        + "<html><head><meta charset=\"UTF-8\"></head><body>Content</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle both declarations
        assertNotNull(doc, "Document should be parsed");
        assertNotNull(doc.getElementsByTagName("BODY").item(0), "Body should exist");
    }

    // ========================================================================
    // Special Character Edge Cases
    // ========================================================================

    @Test
    public void testNonBreakingSpaces() throws Exception {
        // Given: HTML with various types of spaces
        final String html =
                "<html><body>" + "Regular space " + "Non-breaking&nbsp;space " + "En\u2002space " + "Em\u2003space " + "Thin\u2009space "
                        + "Hair\u200Aspace" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should parse HTML with various space types
        assertNotNull(doc, "Document should be parsed");
        final String bodyText = doc.getElementsByTagName("BODY").item(0).getTextContent();
        assertNotNull(bodyText, "Body text should not be null");
        assertTrue(bodyText.length() > 0, "Body should have content");

        // Unicode spaces in source are preserved
        assertTrue(bodyText.contains("\u2002"), "Should contain en space");
        assertTrue(bodyText.contains("\u2003"), "Should contain em space");
    }

    @Test
    public void testSpecialPunctuationCharacters() throws Exception {
        // Given: HTML with special punctuation
        final String html =
                "<html><body>" + "Quotes: \u201C\u201D \u2018\u2019 " + "Dashes: \u2013 \u2014 " + "Ellipsis: \u2026 " + "Bullet: \u2022 "
                        + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should preserve special punctuation
        assertNotNull(doc, "Document should be parsed");
        final String bodyText = doc.getElementsByTagName("BODY").item(0).getTextContent();
        assertTrue(bodyText.contains("\u201C"), "Should contain left double quote");
        assertTrue(bodyText.contains("\u2013"), "Should contain en dash");
        assertTrue(bodyText.contains("\u2026"), "Should contain ellipsis");
        assertTrue(bodyText.contains("\u2022"), "Should contain bullet");
    }

    @Test
    public void testMathematicalSymbols() throws Exception {
        // Given: HTML with mathematical symbols
        final String html = "<html><body>" + "∞ ≠ ≤ ≥ ± × ÷ √ ∑ ∏ ∫ ∂ ∇" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should preserve mathematical symbols
        assertNotNull(doc, "Document should be parsed");
        final String bodyText = doc.getElementsByTagName("BODY").item(0).getTextContent();
        assertTrue(bodyText.contains("∞"), "Should contain infinity symbol");
        assertTrue(bodyText.contains("≠"), "Should contain not equal symbol");
        assertTrue(bodyText.contains("√"), "Should contain square root symbol");
    }

    @Test
    public void testCurrencySymbols() throws Exception {
        // Given: HTML with various currency symbols
        final String html = "<html><body>" + "$ € £ ¥ ₹ ₽ ₩ ¢ ฿" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should preserve currency symbols
        assertNotNull(doc, "Document should be parsed");
        final String bodyText = doc.getElementsByTagName("BODY").item(0).getTextContent();
        assertTrue(bodyText.contains("€"), "Should contain euro symbol");
        assertTrue(bodyText.contains("£"), "Should contain pound symbol");
        assertTrue(bodyText.contains("¥"), "Should contain yen symbol");
    }

    @Test
    public void testMixedDirectionalText() throws Exception {
        // Given: HTML with mixed LTR and RTL text
        final String html =
                "<html><body>" + "<p dir=\"ltr\">Left-to-right text with עברית embedded</p>"
                        + "<p dir=\"rtl\">טקסט מימין לשמאל with English embedded</p>" + "<p>Mixed: Hello שלום مرحبا</p>" + "</body></html>";

        // When: Parsing
        final Document doc = parseHTML(html);

        // Then: Should handle mixed directional text
        assertNotNull(doc, "Document should be parsed");
        final NodeList paragraphs = doc.getElementsByTagName("P");
        assertEquals(3, paragraphs.getLength(), "Should have 3 paragraphs");

        final Element p1 = (Element) paragraphs.item(0);
        assertEquals("ltr", p1.getAttribute("dir"), "Should have dir=ltr");

        final Element p2 = (Element) paragraphs.item(1);
        assertEquals("rtl", p2.getAttribute("dir"), "Should have dir=rtl");
    }
}
