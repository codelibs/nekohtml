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
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.firstText;
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.parse;
import static org.codelibs.nekohtml.parsers.ValidHtmlTestSupport.parseBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Characterization tests locking in {@code SimpleHTMLScanner}'s byte-stream decoding behavior:
 * which {@link org.xml.sax.InputSource} encoding label is honored, what happens on a {@code null}
 * encoding, whether {@code <meta charset>} / {@code http-equiv} declarations are auto-detected, and
 * how (or whether) a leading byte-order-mark is handled. All input HTML here is otherwise
 * well-formed; only the byte-level decoding path is under test.
 */
public class ValidEncodingBomTest {

    // -----------------------------------------------------------------
    // Explicit encoding labels
    // -----------------------------------------------------------------

    @Test
    public void isoLatin1DecodesByteStreamWithExplicitEncoding() throws Exception {
        final byte[] bytes = "<p>café</p>".getBytes("ISO-8859-1");
        final Document doc = parseBytes(bytes, "ISO-8859-1");
        assertEquals("café", firstText(doc, "//P"));
    }

    @Test
    public void utf8DecodesByteStreamWithNullEncodingDefaultingToUtf8() throws Exception {
        // characterization: a null InputSource encoding defaults to "UTF-8" (per SimpleHTMLScanner).
        final byte[] bytes = "<p>café</p>".getBytes("UTF-8");
        final Document doc = parseBytes(bytes, null);
        assertEquals("café", firstText(doc, "//P"));
    }

    @Test
    public void utf8DecodesByteStreamWithExplicitEncoding() throws Exception {
        final byte[] bytes = "<p>café</p>".getBytes("UTF-8");
        final Document doc = parseBytes(bytes, "UTF-8");
        assertEquals("café", firstText(doc, "//P"));
    }

    @Test
    public void shiftJisDecodesMultibyteJapaneseText() throws Exception {
        final byte[] bytes = "<p>日本語</p>".getBytes("Shift_JIS");
        final Document doc = parseBytes(bytes, "Shift_JIS");
        assertEquals("日本語", firstText(doc, "//P"));
    }

    @Test
    public void windows1252DecodesExtendedLatinAndEuroSign() throws Exception {
        final byte[] bytes = "<p>café €</p>".getBytes("windows-1252");
        final Document doc = parseBytes(bytes, "windows-1252");
        assertEquals("café €", firstText(doc, "//P"));
    }

    @Test
    public void isoLatin1DecodesAttributeValueWithExplicitEncoding() throws Exception {
        final byte[] bytes = "<p title=\"café\">x</p>".getBytes("ISO-8859-1");
        final Document doc = parseBytes(bytes, "ISO-8859-1");
        assertEquals("café", first(doc, "//P").getAttribute("title"));
    }

    @Test
    public void shiftJisDecodesAttributeValueWithExplicitEncoding() throws Exception {
        final byte[] bytes = "<p title=\"日本語\">x</p>".getBytes("Shift_JIS");
        final Document doc = parseBytes(bytes, "Shift_JIS");
        assertEquals("日本語", first(doc, "//P").getAttribute("title"));
    }

    // -----------------------------------------------------------------
    // Encoding mismatch (mojibake)
    // -----------------------------------------------------------------

    @Test
    public void utf8BytesDecodedAsIsoLatin1ProduceMojibake() throws Exception {
        // characterization: the UTF-8 two-byte sequence for 'é' (0xC3 0xA9) is mis-decoded, one byte
        // at a time, as ISO-8859-1, expanding a single character into two mojibake characters.
        final byte[] bytes = "<p>café</p>".getBytes("UTF-8");
        final Document doc = parseBytes(bytes, "ISO-8859-1");
        final String text = firstText(doc, "//P");
        assertEquals("cafÃ©", text);
        assertEquals(5, text.length());
        assertEquals('Ã', text.charAt(3));
        assertEquals('©', text.charAt(4));
    }

    // -----------------------------------------------------------------
    // <meta charset> / http-equiv are NOT auto-detected
    // -----------------------------------------------------------------

    @Test
    public void metaCharsetTagIsNotAutoDetectedForDecoding() throws Exception {
        final String html = "<html><head><meta charset=\"Shift_JIS\"></head><body><p>x</p></body></html>";
        // characterization: the document bytes are actually UTF-8, but the byte stream is parsed
        // with the default (UTF-8) decoding regardless of the <meta charset> declaration.
        final Document doc = parseBytes(html.getBytes("UTF-8"), null);
        assertEquals(1, count(doc, "//META"));
        assertEquals("x", firstText(doc, "//P"));
    }

    @Test
    public void metaCharsetAttributeValueIsPreservedVerbatim() throws Exception {
        final String html = "<html><head><meta charset=\"Shift_JIS\"></head><body><p>x</p></body></html>";
        final Document doc = parseBytes(html.getBytes("UTF-8"), null);
        // characterization: the meta tag itself still parses normally; only its charset value has no
        // effect on decoding.
        assertEquals("Shift_JIS", first(doc, "//META").getAttribute("charset"));
    }

    @Test
    public void httpEquivContentTypeMetaIsNotAutoDetectedForDecoding() throws Exception {
        final String html =
                "<html><head>" + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=Shift_JIS\"></head>"
                        + "<body><p>y</p></body></html>";
        final Document doc = parseBytes(html.getBytes("UTF-8"), null);
        assertEquals(1, count(doc, "//META"));
        assertEquals("y", firstText(doc, "//P"));
    }

    // -----------------------------------------------------------------
    // Byte-order-mark handling
    // -----------------------------------------------------------------

    @Test
    public void utf8BomIsNotStrippedAndSurvivesAsLeadingTextCharacter() throws Exception {
        // characterization: SimpleHTMLScanner does not detect or strip a leading UTF-8 BOM; decoded as
        // UTF-8, the three BOM bytes (EF BB BF) become a single U+FEFF character that shows up as a
        // leading text node sibling of <P>, not merged into <P>'s own text.
        final byte[] bom = concat(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }, "<p>x</p>".getBytes("UTF-8"));
        final Document doc = parseBytes(bom, "UTF-8");

        assertEquals("x", firstText(doc, "//P"));
        final Node htmlFirstChild = first(doc, "//HTML").getFirstChild();
        assertEquals(Node.TEXT_NODE, htmlFirstChild.getNodeType());
        assertEquals(1, htmlFirstChild.getNodeValue().length());
        assertEquals(0xFEFF, (int) htmlFirstChild.getNodeValue().charAt(0));
    }

    @Test
    public void utf8BomIsNotStrippedWithNullEncodingEither() throws Exception {
        final byte[] bom = concat(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }, "<p>x</p>".getBytes("UTF-8"));
        final Document doc = parseBytes(bom, null);

        assertEquals("x", firstText(doc, "//P"));
        final Node htmlFirstChild = first(doc, "//HTML").getFirstChild();
        assertEquals(Node.TEXT_NODE, htmlFirstChild.getNodeType());
        assertEquals(0xFEFF, (int) htmlFirstChild.getNodeValue().charAt(0));
    }

    @Test
    public void documentElementIsStillTheSoleRootChildDespiteLeadingBom() throws Exception {
        final byte[] bom = concat(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }, "<p>x</p>".getBytes("UTF-8"));
        final Document doc = parseBytes(bom, "UTF-8");
        // characterization: the BOM character is folded into <HTML>'s text content, not left as a
        // sibling of <HTML> at the Document level.
        assertEquals(1, doc.getChildNodes().getLength());
        assertEquals("HTML", doc.getDocumentElement().getNodeName());
    }

    // -----------------------------------------------------------------
    // Character stream bypasses byte decoding entirely
    // -----------------------------------------------------------------

    @Test
    public void characterStreamParsingIgnoresEncodingEntirely() throws Exception {
        // characterization: parsing a java.io.StringReader-backed InputSource never touches
        // InputSource#getEncoding(); the already-decoded Java String is used verbatim.
        final Document doc = parse("<p>café</p>");
        assertEquals("café", firstText(doc, "//P"));
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private static byte[] concat(final byte[] a, final byte[] b) throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(a);
        bos.write(b);
        final byte[] result = bos.toByteArray();
        assertNotNull(result);
        return result;
    }
}
