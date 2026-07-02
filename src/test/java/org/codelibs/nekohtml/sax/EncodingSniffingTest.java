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
package org.codelibs.nekohtml.sax;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.codelibs.nekohtml.parsers.DOMParser;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Tests for byte-stream encoding sniffing: BOM detection (UTF-8/UTF-16BE/UTF-16LE) and
 * {@code <meta charset>} / {@code <meta http-equiv="Content-Type">} pre-scan.
 *
 * @author CodeLibs Project
 */
public class EncodingSniffingTest {

    private Document parseBytes(final byte[] bytes, final String encoding) throws Exception {
        final DOMParser parser = new DOMParser();
        final InputStream stream = new ByteArrayInputStream(bytes);
        final InputSource source = new InputSource(stream);
        if (encoding != null) {
            source.setEncoding(encoding);
        }
        parser.parse(source);
        return parser.getDocument();
    }

    private static byte[] concat(final byte[] a, final byte[] b) {
        final byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    @Test
    public void testUtf8BomStrippedAndDecoded() throws Exception {
        final byte[] bytes = concat(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }, "<html><body>あ</body></html>".getBytes(UTF_8));
        final Document doc = parseBytes(bytes, null);
        assertEquals("あ", doc.getElementsByTagName("BODY").item(0).getTextContent());
    }

    @Test
    public void testUtf16LeBomDecoded() throws Exception {
        // Leading U+FEFF encodes to the FF FE UTF-16LE byte-order mark.
        final byte[] bytes = "﻿<html><body>あ</body></html>".getBytes(StandardCharsets.UTF_16LE);
        final Document doc = parseBytes(bytes, null);
        assertEquals("あ", doc.getElementsByTagName("BODY").item(0).getTextContent());
    }

    @Test
    public void testMetaCharsetSniffed() throws Exception {
        final byte[] bytes = "<html><head><meta charset=\"Shift_JIS\"></head><body>日本語</body></html>".getBytes("Shift_JIS");
        final Document doc = parseBytes(bytes, null);
        assertEquals("日本語", doc.getElementsByTagName("BODY").item(0).getTextContent());
    }

    @Test
    public void testMetaHttpEquivContentTypeSniffed() throws Exception {
        final byte[] bytes =
                "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=EUC-JP\"></head><body>日本語</body></html>"
                        .getBytes("EUC-JP");
        final Document doc = parseBytes(bytes, null);
        assertEquals("日本語", doc.getElementsByTagName("BODY").item(0).getTextContent());
    }

    @Test
    public void testExplicitEncodingStillWins() throws Exception {
        final byte[] bytes = "<html><body>日本語</body></html>".getBytes("EUC-JP");
        final Document doc = parseBytes(bytes, "EUC-JP");
        assertEquals("日本語", doc.getElementsByTagName("BODY").item(0).getTextContent());
    }

} // class EncodingSniffingTest
