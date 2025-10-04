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

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps IANA encoding names to Java charset names.
 * This is a replacement for Xerces EncodingMap utility.
 *
 * @author CodeLibs Project
 */
public class EncodingMap {

    /** Mapping from IANA names to Java charset names. */
    private static final Map<String, String> IANA_TO_JAVA_MAP = new HashMap<>();

    static {
        // Common IANA to Java charset mappings
        IANA_TO_JAVA_MAP.put("UTF-8", "UTF8");
        IANA_TO_JAVA_MAP.put("UTF-16", "UTF-16");
        IANA_TO_JAVA_MAP.put("UTF-16BE", "UnicodeBigUnmarked");
        IANA_TO_JAVA_MAP.put("UTF-16LE", "UnicodeLittleUnmarked");
        IANA_TO_JAVA_MAP.put("US-ASCII", "ASCII");
        IANA_TO_JAVA_MAP.put("ISO-8859-1", "ISO8859_1");
        IANA_TO_JAVA_MAP.put("ISO-8859-2", "ISO8859_2");
        IANA_TO_JAVA_MAP.put("ISO-8859-3", "ISO8859_3");
        IANA_TO_JAVA_MAP.put("ISO-8859-4", "ISO8859_4");
        IANA_TO_JAVA_MAP.put("ISO-8859-5", "ISO8859_5");
        IANA_TO_JAVA_MAP.put("ISO-8859-6", "ISO8859_6");
        IANA_TO_JAVA_MAP.put("ISO-8859-7", "ISO8859_7");
        IANA_TO_JAVA_MAP.put("ISO-8859-8", "ISO8859_8");
        IANA_TO_JAVA_MAP.put("ISO-8859-9", "ISO8859_9");
        IANA_TO_JAVA_MAP.put("ISO-8859-13", "ISO8859_13");
        IANA_TO_JAVA_MAP.put("ISO-8859-15", "ISO8859_15");
        IANA_TO_JAVA_MAP.put("WINDOWS-1250", "Cp1250");
        IANA_TO_JAVA_MAP.put("WINDOWS-1251", "Cp1251");
        IANA_TO_JAVA_MAP.put("WINDOWS-1252", "Cp1252");
        IANA_TO_JAVA_MAP.put("WINDOWS-1253", "Cp1253");
        IANA_TO_JAVA_MAP.put("WINDOWS-1254", "Cp1254");
        IANA_TO_JAVA_MAP.put("WINDOWS-1255", "Cp1255");
        IANA_TO_JAVA_MAP.put("WINDOWS-1256", "Cp1256");
        IANA_TO_JAVA_MAP.put("WINDOWS-1257", "Cp1257");
        IANA_TO_JAVA_MAP.put("WINDOWS-1258", "Cp1258");
        IANA_TO_JAVA_MAP.put("SHIFT_JIS", "SJIS");
        IANA_TO_JAVA_MAP.put("EUC-JP", "EUC_JP");
        IANA_TO_JAVA_MAP.put("ISO-2022-JP", "ISO2022JP");
        IANA_TO_JAVA_MAP.put("EUC-KR", "EUC_KR");
        IANA_TO_JAVA_MAP.put("GB2312", "GB2312");
        IANA_TO_JAVA_MAP.put("BIG5", "Big5");
        IANA_TO_JAVA_MAP.put("KOI8-R", "KOI8_R");
    }

    /**
     * Gets the Java charset name for an IANA encoding name.
     *
     * @param ianaEncoding The IANA encoding name (case-insensitive)
     * @return The Java charset name, or null if not found
     */
    public static String getIANA2JavaMapping(final String ianaEncoding) {
        if (ianaEncoding == null) {
            return null;
        }

        final String upper = ianaEncoding.toUpperCase();

        // Check our mapping table first
        String javaEncoding = IANA_TO_JAVA_MAP.get(upper);
        if (javaEncoding != null) {
            return javaEncoding;
        }

        // Try to use Java's charset directly
        try {
            final Charset charset = Charset.forName(ianaEncoding);
            return charset.name();
        } catch (final IllegalCharsetNameException | UnsupportedCharsetException e) {
            // Encoding not supported
            return null;
        }
    }

    /**
     * Checks if an encoding is supported.
     *
     * @param encoding The encoding name
     * @return true if supported, false otherwise
     */
    public static boolean isSupported(final String encoding) {
        if (encoding == null) {
            return false;
        }

        try {
            return Charset.isSupported(encoding);
        } catch (final IllegalCharsetNameException e) {
            return false;
        }
    }

} // class EncodingMap
