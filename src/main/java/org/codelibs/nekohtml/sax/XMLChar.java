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

/**
 * XML character validation utilities based on XML 1.0 specification.
 * This is a replacement for Xerces XMLChar utility class.
 *
 * @author CodeLibs Project
 */
public class XMLChar {

    /** Private constructor to prevent instantiation of utility class. */
    private XMLChar() {
    }

    /**
     * Checks if a character is a valid XML character according to XML 1.0.
     *
     * @param c The character to check
     * @return true if valid, false otherwise
     */
    public static boolean isValid(final int c) {
        return (c == 0x09) || (c == 0x0A) || (c == 0x0D) || (c >= 0x20 && c <= 0xD7FF) || (c >= 0xE000 && c <= 0xFFFD)
                || (c >= 0x10000 && c <= 0x10FFFF);
    }

    /**
     * Checks if a character is invalid for XML.
     *
     * @param c The character to check
     * @return true if invalid, false otherwise
     */
    public static boolean isInvalid(final int c) {
        return !isValid(c);
    }

    /**
     * Checks if a character is a valid name start character according to XML 1.0.
     *
     * @param c The character to check
     * @return true if valid name start, false otherwise
     */
    public static boolean isNameStart(final int c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == ':' || c == '_' || (c >= 0xC0 && c <= 0xD6)
                || (c >= 0xD8 && c <= 0xF6) || (c >= 0xF8 && c <= 0x2FF) || (c >= 0x370 && c <= 0x37D) || (c >= 0x37F && c <= 0x1FFF)
                || (c >= 0x200C && c <= 0x200D) || (c >= 0x2070 && c <= 0x218F) || (c >= 0x2C00 && c <= 0x2FEF)
                || (c >= 0x3001 && c <= 0xD7FF) || (c >= 0xF900 && c <= 0xFDCF) || (c >= 0xFDF0 && c <= 0xFFFD)
                || (c >= 0x10000 && c <= 0xEFFFF);
    }

    /**
     * Checks if a character is a valid name character according to XML 1.0.
     *
     * @param c The character to check
     * @return true if valid name character, false otherwise
     */
    public static boolean isName(final int c) {
        return isNameStart(c) || (c >= '0' && c <= '9') || c == '-' || c == '.' || c == 0xB7 || (c >= 0x0300 && c <= 0x036F)
                || (c >= 0x203F && c <= 0x2040);
    }

    /**
     * Checks if a string is a valid XML name.
     *
     * @param name The string to check
     * @return true if valid name, false otherwise
     */
    public static boolean isValidName(final String name) {
        if (name == null || name.length() == 0) {
            return false;
        }

        final int first = name.codePointAt(0);
        if (!isNameStart(first)) {
            return false;
        }

        for (int i = Character.charCount(first); i < name.length();) {
            final int c = name.codePointAt(i);
            if (!isName(c)) {
                return false;
            }
            i += Character.charCount(c);
        }

        return true;
    }

    /**
     * Checks if a character is whitespace according to XML.
     *
     * @param c The character to check
     * @return true if whitespace, false otherwise
     */
    public static boolean isSpace(final int c) {
        return c == ' ' || c == '\n' || c == '\t' || c == '\r';
    }

} // class XMLChar
