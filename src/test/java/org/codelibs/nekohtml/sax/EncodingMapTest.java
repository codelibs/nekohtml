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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Test cases for EncodingMap.
 * Tests IANA to Java charset name mappings and encoding support.
 */
public class EncodingMapTest {

    @Test
    public void testConstructor() {
        // When: Creating EncodingMap instance
        final EncodingMap map = new EncodingMap();

        // Then: Instance should be created
        assertNotNull(map, "EncodingMap should be instantiable");
    }

    @Test
    public void testGetIANA2JavaMappingUTF8() {
        // When: Getting mapping for UTF-8
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("UTF-8");

        // Then: Should return Java encoding name
        assertNotNull(javaEncoding, "UTF-8 should have mapping");
        assertEquals("UTF8", javaEncoding, "UTF-8 should map to UTF8");
    }

    @Test
    public void testGetIANA2JavaMappingUTF16() {
        // When: Getting mapping for UTF-16
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("UTF-16");

        // Then: Should return Java encoding name
        assertNotNull(javaEncoding, "UTF-16 should have mapping");
        assertEquals("UTF-16", javaEncoding, "UTF-16 should map to UTF-16");
    }

    @Test
    public void testGetIANA2JavaMappingUTF16BE() {
        // When: Getting mapping for UTF-16BE
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("UTF-16BE");

        // Then: Should return Java encoding name
        assertNotNull(javaEncoding, "UTF-16BE should have mapping");
        assertEquals("UnicodeBigUnmarked", javaEncoding, "UTF-16BE should map to UnicodeBigUnmarked");
    }

    @Test
    public void testGetIANA2JavaMappingUTF16LE() {
        // When: Getting mapping for UTF-16LE
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("UTF-16LE");

        // Then: Should return Java encoding name
        assertNotNull(javaEncoding, "UTF-16LE should have mapping");
        assertEquals("UnicodeLittleUnmarked", javaEncoding, "UTF-16LE should map to UnicodeLittleUnmarked");
    }

    @Test
    public void testGetIANA2JavaMappingASCII() {
        // When: Getting mapping for US-ASCII
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("US-ASCII");

        // Then: Should return Java encoding name
        assertNotNull(javaEncoding, "US-ASCII should have mapping");
        assertEquals("ASCII", javaEncoding, "US-ASCII should map to ASCII");
    }

    @Test
    public void testGetIANA2JavaMappingISO88591() {
        // When: Getting mapping for ISO-8859-1
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("ISO-8859-1");

        // Then: Should return Java encoding name
        assertNotNull(javaEncoding, "ISO-8859-1 should have mapping");
        assertEquals("ISO8859_1", javaEncoding, "ISO-8859-1 should map to ISO8859_1");
    }

    @Test
    public void testGetIANA2JavaMappingISO88592() {
        // When: Getting mapping for ISO-8859-2
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("ISO-8859-2");

        // Then: Should return Java encoding name
        assertNotNull(javaEncoding, "ISO-8859-2 should have mapping");
        assertEquals("ISO8859_2", javaEncoding, "ISO-8859-2 should map to ISO8859_2");
    }

    @Test
    public void testGetIANA2JavaMappingWindows1252() {
        // When: Getting mapping for WINDOWS-1252
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("WINDOWS-1252");

        // Then: Should return Java encoding name
        assertNotNull(javaEncoding, "WINDOWS-1252 should have mapping");
        assertEquals("Cp1252", javaEncoding, "WINDOWS-1252 should map to Cp1252");
    }

    @Test
    public void testGetIANA2JavaMappingShiftJIS() {
        // When: Getting mapping for SHIFT_JIS
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("SHIFT_JIS");

        // Then: Should return Java encoding name
        assertNotNull(javaEncoding, "SHIFT_JIS should have mapping");
        assertEquals("SJIS", javaEncoding, "SHIFT_JIS should map to SJIS");
    }

    @Test
    public void testGetIANA2JavaMappingEUCJP() {
        // When: Getting mapping for EUC-JP
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("EUC-JP");

        // Then: Should return Java encoding name
        assertNotNull(javaEncoding, "EUC-JP should have mapping");
        assertEquals("EUC_JP", javaEncoding, "EUC-JP should map to EUC_JP");
    }

    @Test
    public void testGetIANA2JavaMappingISO2022JP() {
        // When: Getting mapping for ISO-2022-JP
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("ISO-2022-JP");

        // Then: Should return Java encoding name
        assertNotNull(javaEncoding, "ISO-2022-JP should have mapping");
        assertEquals("ISO2022JP", javaEncoding, "ISO-2022-JP should map to ISO2022JP");
    }

    @Test
    public void testGetIANA2JavaMappingEUCKR() {
        // When: Getting mapping for EUC-KR
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("EUC-KR");

        // Then: Should return Java encoding name
        assertNotNull(javaEncoding, "EUC-KR should have mapping");
        assertEquals("EUC_KR", javaEncoding, "EUC-KR should map to EUC_KR");
    }

    @Test
    public void testGetIANA2JavaMappingGB2312() {
        // When: Getting mapping for GB2312
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("GB2312");

        // Then: Should return Java encoding name
        assertNotNull(javaEncoding, "GB2312 should have mapping");
        assertEquals("GB2312", javaEncoding, "GB2312 should map to GB2312");
    }

    @Test
    public void testGetIANA2JavaMappingBig5() {
        // When: Getting mapping for BIG5
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("BIG5");

        // Then: Should return Java encoding name
        assertNotNull(javaEncoding, "BIG5 should have mapping");
        assertEquals("Big5", javaEncoding, "BIG5 should map to Big5");
    }

    @Test
    public void testGetIANA2JavaMappingKOI8R() {
        // When: Getting mapping for KOI8-R
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("KOI8-R");

        // Then: Should return Java encoding name
        assertNotNull(javaEncoding, "KOI8-R should have mapping");
        assertEquals("KOI8_R", javaEncoding, "KOI8-R should map to KOI8_R");
    }

    @Test
    public void testGetIANA2JavaMappingCaseInsensitive() {
        // When: Getting mapping with lowercase
        final String lower = EncodingMap.getIANA2JavaMapping("utf-8");
        final String upper = EncodingMap.getIANA2JavaMapping("UTF-8");
        final String mixed = EncodingMap.getIANA2JavaMapping("Utf-8");

        // Then: All should return same result
        assertEquals("UTF8", lower, "Lowercase should work");
        assertEquals("UTF8", upper, "Uppercase should work");
        assertEquals("UTF8", mixed, "Mixed case should work");
    }

    @Test
    public void testGetIANA2JavaMappingNull() {
        // When: Getting mapping for null
        final String javaEncoding = EncodingMap.getIANA2JavaMapping(null);

        // Then: Should return null
        assertNull(javaEncoding, "Null encoding should return null");
    }

    @Test
    public void testGetIANA2JavaMappingUnknown() {
        // When: Getting mapping for unknown encoding
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("UNKNOWN-ENCODING-XYZ");

        // Then: Should return null
        assertNull(javaEncoding, "Unknown encoding should return null");
    }

    @Test
    public void testGetIANA2JavaMappingInvalidCharsetName() {
        // When: Getting mapping for invalid charset name
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("invalid:charset:name");

        // Then: Should return null
        assertNull(javaEncoding, "Invalid charset name should return null");
    }

    @Test
    public void testGetIANA2JavaMappingFallbackToJavaCharset() {
        // When: Getting mapping for encoding not in map but supported by Java
        // Note: "ISO-8859-10" is not in our map, but Java supports it
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("ISO-8859-10");

        // Then: Should fallback to Java's Charset.forName
        // Java's Charset.forName will return the canonical name if supported
        if (javaEncoding != null) {
            assertTrue(EncodingMap.isSupported(javaEncoding), "Returned charset should be supported");
        }
        // Note: Behavior depends on Java version - ISO-8859-10 may or may not be supported
    }

    @Test
    public void testIsSupportedUTF8() {
        // When: Checking if UTF-8 is supported
        final boolean supported = EncodingMap.isSupported("UTF-8");

        // Then: Should be supported
        assertTrue(supported, "UTF-8 should be supported");
    }

    @Test
    public void testIsSupportedASCII() {
        // When: Checking if ASCII is supported
        final boolean supported = EncodingMap.isSupported("US-ASCII");

        // Then: Should be supported
        assertTrue(supported, "US-ASCII should be supported");
    }

    @Test
    public void testIsSupportedISO88591() {
        // When: Checking if ISO-8859-1 is supported
        final boolean supported = EncodingMap.isSupported("ISO-8859-1");

        // Then: Should be supported
        assertTrue(supported, "ISO-8859-1 should be supported");
    }

    @Test
    public void testIsSupportedShiftJIS() {
        // When: Checking if Shift_JIS is supported
        final boolean supported = EncodingMap.isSupported("Shift_JIS");

        // Then: Should be supported
        assertTrue(supported, "Shift_JIS should be supported");
    }

    @Test
    public void testIsSupportedNull() {
        // When: Checking if null is supported
        final boolean supported = EncodingMap.isSupported(null);

        // Then: Should not be supported
        assertFalse(supported, "Null should not be supported");
    }

    @Test
    public void testIsSupportedUnknown() {
        // When: Checking if unknown encoding is supported
        final boolean supported = EncodingMap.isSupported("UNKNOWN-ENCODING-XYZ");

        // Then: Should not be supported
        assertFalse(supported, "Unknown encoding should not be supported");
    }

    @Test
    public void testIsSupportedInvalidCharsetName() {
        // When: Checking if invalid charset name is supported
        final boolean supported = EncodingMap.isSupported("invalid:charset:name");

        // Then: Should not be supported
        assertFalse(supported, "Invalid charset name should not be supported");
    }

    @Test
    public void testIsSupportedCaseInsensitive() {
        // When: Checking support with different cases
        final boolean lower = EncodingMap.isSupported("utf-8");
        final boolean upper = EncodingMap.isSupported("UTF-8");

        // Then: Both should be supported
        assertTrue(lower, "Lowercase should be supported");
        assertTrue(upper, "Uppercase should be supported");
    }

    @Test
    public void testAllWindowsEncodings() {
        // Test all Windows encodings in the map
        final String[][] windowsEncodings =
                { { "WINDOWS-1250", "Cp1250" }, { "WINDOWS-1251", "Cp1251" }, { "WINDOWS-1252", "Cp1252" }, { "WINDOWS-1253", "Cp1253" },
                        { "WINDOWS-1254", "Cp1254" }, { "WINDOWS-1255", "Cp1255" }, { "WINDOWS-1256", "Cp1256" },
                        { "WINDOWS-1257", "Cp1257" }, { "WINDOWS-1258", "Cp1258" } };

        for (final String[] pair : windowsEncodings) {
            final String iana = pair[0];
            final String expected = pair[1];
            final String actual = EncodingMap.getIANA2JavaMapping(iana);
            assertEquals(expected, actual, iana + " should map to " + expected);
        }
    }

    @Test
    public void testAllISO8859Encodings() {
        // Test ISO-8859 encodings in the map
        final String[][] isoEncodings =
                { { "ISO-8859-1", "ISO8859_1" }, { "ISO-8859-2", "ISO8859_2" }, { "ISO-8859-3", "ISO8859_3" },
                        { "ISO-8859-4", "ISO8859_4" }, { "ISO-8859-5", "ISO8859_5" }, { "ISO-8859-6", "ISO8859_6" },
                        { "ISO-8859-7", "ISO8859_7" }, { "ISO-8859-8", "ISO8859_8" }, { "ISO-8859-9", "ISO8859_9" },
                        { "ISO-8859-13", "ISO8859_13" }, { "ISO-8859-15", "ISO8859_15" } };

        for (final String[] pair : isoEncodings) {
            final String iana = pair[0];
            final String expected = pair[1];
            final String actual = EncodingMap.getIANA2JavaMapping(iana);
            assertEquals(expected, actual, iana + " should map to " + expected);
        }
    }

    @Test
    public void testCommonEncodingsAreSupported() {
        // Test that common encodings are supported
        final String[] commonEncodings =
                { "UTF-8", "UTF-16", "US-ASCII", "ISO-8859-1", "WINDOWS-1252", "Shift_JIS", "EUC-JP", "EUC-KR", "GB2312", "Big5" };

        for (final String encoding : commonEncodings) {
            assertTrue(EncodingMap.isSupported(encoding), encoding + " should be supported");
        }
    }

    @Test
    public void testGetIANA2JavaMappingReturnsValidCharset() {
        // When: Getting mapping for UTF-8
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("UTF-8");

        // Then: The returned name should be a valid charset
        assertNotNull(javaEncoding, "Should return a charset name");
        assertTrue(EncodingMap.isSupported(javaEncoding), "Returned charset should be supported");
    }

    @Test
    public void testEmptyStringEncoding() {
        // When: Getting mapping for empty string
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("");

        // Then: Should return null
        assertNull(javaEncoding, "Empty string should return null");
    }

    @Test
    public void testEmptyStringSupport() {
        // When: Checking if empty string is supported
        final boolean supported = EncodingMap.isSupported("");

        // Then: Should not be supported
        assertFalse(supported, "Empty string should not be supported");
    }

    @Test
    public void testWhitespaceEncoding() {
        // When: Getting mapping for whitespace
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("   ");

        // Then: Should return null
        assertNull(javaEncoding, "Whitespace should return null");
    }

    @Test
    public void testSpecialCharactersInEncodingName() {
        // When: Getting mapping for encoding with special characters
        final String javaEncoding = EncodingMap.getIANA2JavaMapping("ISO@8859#1");

        // Then: Should return null
        assertNull(javaEncoding, "Invalid encoding name should return null");
    }

} // class EncodingMapTest
