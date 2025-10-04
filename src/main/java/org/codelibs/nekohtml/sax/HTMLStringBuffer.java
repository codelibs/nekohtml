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
 * String buffer for HTML parsing that efficiently manages character data.
 * This is a replacement for Xerces XNI XMLStringBuffer.
 *
 * @author CodeLibs Project
 */
public class HTMLStringBuffer {

    /** The character buffer. */
    public char[] ch;

    /** The offset into the buffer. */
    public int offset;

    /** The length of the data in the buffer. */
    public int length;

    /**
     * Constructs a string buffer with the default size (128 characters).
     */
    public HTMLStringBuffer() {
        this(128);
    }

    /**
     * Constructs a string buffer with the specified size.
     *
     * @param size The initial buffer size
     */
    public HTMLStringBuffer(final int size) {
        ch = new char[size];
        offset = 0;
        length = 0;
    }

    /**
     * Constructs a string buffer initialized with the specified string.
     *
     * @param s The initial string content
     */
    public HTMLStringBuffer(final String s) {
        this(s.length());
        append(s);
    }

    /**
     * Constructs a string buffer initialized with the specified character array.
     *
     * @param ch The character array
     * @param offset The offset into the array
     * @param length The length of data to use
     */
    public HTMLStringBuffer(final char[] ch, final int offset, final int length) {
        this(length);
        append(ch, offset, length);
    }

    /**
     * Clears the buffer.
     */
    public void clear() {
        offset = 0;
        length = 0;
    }

    /**
     * Appends a character to the buffer.
     *
     * @param c The character to append
     */
    public void append(final char c) {
        ensureCapacity(length + 1);
        ch[offset + length] = c;
        length++;
    }

    /**
     * Appends a string to the buffer.
     *
     * @param s The string to append
     */
    public void append(final String s) {
        if (s != null && s.length() > 0) {
            final int slen = s.length();
            ensureCapacity(length + slen);
            s.getChars(0, slen, ch, offset + length);
            length += slen;
        }
    }

    /**
     * Appends characters from an array to the buffer.
     *
     * @param ch The character array
     * @param offset The offset into the array
     * @param length The number of characters to append
     */
    public void append(final char[] ch, final int offset, final int length) {
        if (length > 0) {
            ensureCapacity(this.length + length);
            System.arraycopy(ch, offset, this.ch, this.offset + this.length, length);
            this.length += length;
        }
    }

    /**
     * Appends another string buffer to this buffer.
     *
     * @param s The string buffer to append
     */
    public void append(final HTMLStringBuffer s) {
        append(s.ch, s.offset, s.length);
    }

    /**
     * Ensures the buffer has at least the specified capacity.
     *
     * @param newLength The required capacity
     */
    private void ensureCapacity(final int newLength) {
        if (offset + newLength > ch.length) {
            // Calculate new size (double current size or use required size, whichever is larger)
            int newSize = ch.length * 2;
            if (newSize < offset + newLength) {
                newSize = offset + newLength;
            }

            final char[] newch = new char[newSize];
            System.arraycopy(ch, offset, newch, 0, length);
            ch = newch;
            offset = 0;
        }
    }

    /**
     * Returns the buffer contents as a string.
     *
     * @return The string representation
     */
    @Override
    public String toString() {
        return length > 0 ? new String(ch, offset, length) : "";
    }

    /**
     * Converts to a character array for SAX ContentHandler.characters().
     *
     * @return Array containing [ch, offset, length] for use with SAX
     */
    public Object[] toSAXCharacters() {
        return new Object[] { ch, offset, length };
    }

} // class HTMLStringBuffer
