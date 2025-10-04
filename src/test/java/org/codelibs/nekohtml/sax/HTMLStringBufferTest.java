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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Test cases for HTMLStringBuffer.
 * Tests character buffer management and string operations.
 */
public class HTMLStringBufferTest {

    @Test
    public void testDefaultConstructor() {
        // When: Creating buffer with default constructor
        final HTMLStringBuffer buffer = new HTMLStringBuffer();

        // Then: Should have default size and empty content
        assertNotNull(buffer.ch, "Character array should be initialized");
        assertEquals(128, buffer.ch.length, "Default size should be 128");
        assertEquals(0, buffer.offset, "Offset should be 0");
        assertEquals(0, buffer.length, "Length should be 0");
    }

    @Test
    public void testConstructorWithSize() {
        // Given: Custom size
        final int size = 256;

        // When: Creating buffer with size
        final HTMLStringBuffer buffer = new HTMLStringBuffer(size);

        // Then: Should have specified size
        assertEquals(size, buffer.ch.length, "Size should match");
        assertEquals(0, buffer.offset, "Offset should be 0");
        assertEquals(0, buffer.length, "Length should be 0");
    }

    @Test
    public void testConstructorWithString() {
        // Given: Initial string
        final String str = "Hello World";

        // When: Creating buffer with string
        final HTMLStringBuffer buffer = new HTMLStringBuffer(str);

        // Then: Should contain the string
        assertEquals(str.length(), buffer.length, "Length should match string length");
        assertEquals(str, buffer.toString(), "Content should match");
    }

    @Test
    public void testConstructorWithCharArray() {
        // Given: Character array with offset
        final char[] chars = "  Hello  ".toCharArray();
        final int offset = 2;
        final int length = 5;

        // When: Creating buffer from char array
        final HTMLStringBuffer buffer = new HTMLStringBuffer(chars, offset, length);

        // Then: Should contain substring
        assertEquals(length, buffer.length, "Length should match");
        assertEquals("Hello", buffer.toString(), "Content should be 'Hello'");
    }

    @Test
    public void testClear() {
        // Given: Buffer with content
        final HTMLStringBuffer buffer = new HTMLStringBuffer("Test");

        // When: Clearing the buffer
        buffer.clear();

        // Then: Should be empty
        assertEquals(0, buffer.offset, "Offset should be 0");
        assertEquals(0, buffer.length, "Length should be 0");
        assertEquals("", buffer.toString(), "Content should be empty");
    }

    @Test
    public void testAppendChar() {
        // Given: Empty buffer
        final HTMLStringBuffer buffer = new HTMLStringBuffer();

        // When: Appending characters
        buffer.append('H');
        buffer.append('i');

        // Then: Should contain characters
        assertEquals(2, buffer.length, "Length should be 2");
        assertEquals("Hi", buffer.toString(), "Content should be 'Hi'");
    }

    @Test
    public void testAppendString() {
        // Given: Empty buffer
        final HTMLStringBuffer buffer = new HTMLStringBuffer();

        // When: Appending string
        buffer.append("Hello");
        buffer.append(" ");
        buffer.append("World");

        // Then: Should contain all strings
        assertEquals(11, buffer.length, "Length should be 11");
        assertEquals("Hello World", buffer.toString(), "Content should match");
    }

    @Test
    public void testAppendNullString() {
        // Given: Buffer
        final HTMLStringBuffer buffer = new HTMLStringBuffer();

        // When: Appending null string
        buffer.append((String) null);

        // Then: Should remain empty
        assertEquals(0, buffer.length, "Length should be 0");
        assertEquals("", buffer.toString(), "Content should be empty");
    }

    @Test
    public void testAppendEmptyString() {
        // Given: Buffer
        final HTMLStringBuffer buffer = new HTMLStringBuffer();

        // When: Appending empty string
        buffer.append("");

        // Then: Should remain empty
        assertEquals(0, buffer.length, "Length should be 0");
        assertEquals("", buffer.toString(), "Content should be empty");
    }

    @Test
    public void testAppendCharArray() {
        // Given: Buffer and char array
        final HTMLStringBuffer buffer = new HTMLStringBuffer();
        final char[] chars = "Test".toCharArray();

        // When: Appending char array
        buffer.append(chars, 0, chars.length);

        // Then: Should contain array content
        assertEquals(4, buffer.length, "Length should be 4");
        assertEquals("Test", buffer.toString(), "Content should be 'Test'");
    }

    @Test
    public void testAppendCharArrayWithOffset() {
        // Given: Buffer and char array with offset
        final HTMLStringBuffer buffer = new HTMLStringBuffer();
        final char[] chars = "  Hello  ".toCharArray();

        // When: Appending with offset
        buffer.append(chars, 2, 5);

        // Then: Should contain substring
        assertEquals(5, buffer.length, "Length should be 5");
        assertEquals("Hello", buffer.toString(), "Content should be 'Hello'");
    }

    @Test
    public void testAppendZeroLengthArray() {
        // Given: Buffer
        final HTMLStringBuffer buffer = new HTMLStringBuffer();
        final char[] chars = "Test".toCharArray();

        // When: Appending zero length
        buffer.append(chars, 0, 0);

        // Then: Should remain empty
        assertEquals(0, buffer.length, "Length should be 0");
        assertEquals("", buffer.toString(), "Content should be empty");
    }

    @Test
    public void testAppendHTMLStringBuffer() {
        // Given: Two buffers
        final HTMLStringBuffer buffer1 = new HTMLStringBuffer("Hello");
        final HTMLStringBuffer buffer2 = new HTMLStringBuffer(" World");

        // When: Appending one buffer to another
        buffer1.append(buffer2);

        // Then: Should contain combined content
        assertEquals(11, buffer1.length, "Length should be 11");
        assertEquals("Hello World", buffer1.toString(), "Content should be combined");
    }

    @Test
    public void testBufferExpansion() {
        // Given: Small buffer
        final HTMLStringBuffer buffer = new HTMLStringBuffer(5);

        // When: Appending more than initial capacity
        buffer.append("Hello");
        buffer.append(" World");

        // Then: Buffer should expand automatically
        assertTrue(buffer.ch.length >= 11, "Buffer should expand");
        assertEquals(11, buffer.length, "Length should be 11");
        assertEquals("Hello World", buffer.toString(), "Content should match");
    }

    @Test
    public void testLargeExpansion() {
        // Given: Very small buffer
        final HTMLStringBuffer buffer = new HTMLStringBuffer(2);

        // When: Appending large string
        final String largeString = "This is a much larger string than the initial buffer size";
        buffer.append(largeString);

        // Then: Buffer should expand to accommodate
        assertTrue(buffer.ch.length >= largeString.length(), "Buffer should expand sufficiently");
        assertEquals(largeString.length(), buffer.length, "Length should match string length");
        assertEquals(largeString, buffer.toString(), "Content should match");
    }

    @Test
    public void testMultipleAppends() {
        // Given: Buffer
        final HTMLStringBuffer buffer = new HTMLStringBuffer();

        // When: Multiple appends of different types
        buffer.append('H');
        buffer.append("ello");
        buffer.append(new char[] { ' ' }, 0, 1);
        buffer.append("World");

        // Then: All appends should work correctly
        assertEquals(11, buffer.length, "Length should be 11");
        assertEquals("Hello World", buffer.toString(), "Content should match");
    }

    @Test
    public void testToString() {
        // Given: Buffer with content
        final HTMLStringBuffer buffer = new HTMLStringBuffer("Test String");

        // Then: toString should return content
        assertEquals("Test String", buffer.toString(), "toString should return content");
    }

    @Test
    public void testToStringEmpty() {
        // Given: Empty buffer
        final HTMLStringBuffer buffer = new HTMLStringBuffer();

        // Then: toString should return empty string
        assertEquals("", buffer.toString(), "toString should return empty string");
    }

    @Test
    public void testToStringAfterClear() {
        // Given: Buffer with content that is then cleared
        final HTMLStringBuffer buffer = new HTMLStringBuffer("Test");
        buffer.clear();

        // Then: toString should return empty string
        assertEquals("", buffer.toString(), "toString should return empty string after clear");
    }

    @Test
    public void testToSAXCharacters() {
        // Given: Buffer with content
        final HTMLStringBuffer buffer = new HTMLStringBuffer("Test");

        // When: Getting SAX characters
        final Object[] saxChars = buffer.toSAXCharacters();

        // Then: Should return array with ch, offset, length
        assertNotNull(saxChars, "SAX characters should not be null");
        assertEquals(3, saxChars.length, "Should have 3 elements");
        assertEquals(buffer.ch, saxChars[0], "First element should be char array");
        assertEquals(buffer.offset, saxChars[1], "Second element should be offset");
        assertEquals(buffer.length, saxChars[2], "Third element should be length");
    }

    @Test
    public void testSequentialAppends() {
        // Given: Buffer
        final HTMLStringBuffer buffer = new HTMLStringBuffer(10);

        // When: Sequential appends
        for (int i = 0; i < 5; i++) {
            buffer.append(String.valueOf(i));
        }

        // Then: All appends should be in order
        assertEquals("01234", buffer.toString(), "Sequential appends should preserve order");
    }

    @Test
    public void testAppendAfterClear() {
        // Given: Buffer with content
        final HTMLStringBuffer buffer = new HTMLStringBuffer("Initial");

        // When: Clearing and appending new content
        buffer.clear();
        buffer.append("New Content");

        // Then: Should contain only new content
        assertEquals("New Content", buffer.toString(), "Should contain new content only");
        assertEquals(11, buffer.length, "Length should be 11");
    }

    @Test
    public void testOffsetManagement() {
        // Given: Buffer that will trigger expansion
        final HTMLStringBuffer buffer = new HTMLStringBuffer(5);

        // When: Appending to trigger expansion
        buffer.append("Hello");
        final int lengthBeforeExpansion = buffer.length;
        buffer.append(" World"); // This will trigger expansion

        // Then: Offset should be reset to 0 after expansion
        assertEquals(0, buffer.offset, "Offset should be 0 after expansion");
        assertEquals(lengthBeforeExpansion + 6, buffer.length, "Length should be correct");
    }

    @Test
    public void testCharArrayIntegrity() {
        // Given: Buffer with content
        final HTMLStringBuffer buffer = new HTMLStringBuffer("Test");

        // When: Getting content as char array
        final char[] expected = "Test".toCharArray();

        // Then: Internal array should match (considering offset and length)
        final char[] actual = new char[buffer.length];
        System.arraycopy(buffer.ch, buffer.offset, actual, 0, buffer.length);
        assertArrayEquals(expected, actual, "Character array content should match");
    }

    @Test
    public void testSpecialCharacters() {
        // Given: String with special characters
        final String special = "Special: <>&\"'\n\t";

        // When: Creating buffer with special characters
        final HTMLStringBuffer buffer = new HTMLStringBuffer(special);

        // Then: Should preserve special characters
        assertEquals(special, buffer.toString(), "Special characters should be preserved");
    }

    @Test
    public void testUnicodeCharacters() {
        // Given: String with Unicode characters
        final String unicode = "Unicode: \u65e5\u672c\u8a9e テスト";

        // When: Creating buffer with Unicode
        final HTMLStringBuffer buffer = new HTMLStringBuffer(unicode);

        // Then: Should preserve Unicode characters
        assertEquals(unicode, buffer.toString(), "Unicode characters should be preserved");
        assertEquals(unicode.length(), buffer.length, "Length should match Unicode string length");
    }

    @Test
    public void testLargeContent() {
        // Given: Large content
        final StringBuilder large = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            large.append("Line ").append(i).append("\n");
        }
        final String largeContent = large.toString();

        // When: Creating buffer with large content
        final HTMLStringBuffer buffer = new HTMLStringBuffer(largeContent);

        // Then: Should handle large content
        assertEquals(largeContent.length(), buffer.length, "Length should match");
        assertEquals(largeContent, buffer.toString(), "Content should match");
    }

    @Test
    public void testRepeatedExpansion() {
        // Given: Very small buffer
        final HTMLStringBuffer buffer = new HTMLStringBuffer(1);

        // When: Repeatedly appending to force multiple expansions
        for (int i = 0; i < 100; i++) {
            buffer.append('x');
        }

        // Then: Should handle repeated expansions
        assertEquals(100, buffer.length, "Length should be 100");
        assertTrue(buffer.ch.length >= 100, "Buffer should have expanded sufficiently");
    }

    @Test
    public void testAppendSelf() {
        // Given: Buffer with content
        final HTMLStringBuffer buffer = new HTMLStringBuffer("Test");

        // When: Appending buffer to itself
        buffer.append(buffer);

        // Then: Should duplicate content
        assertEquals("TestTest", buffer.toString(), "Content should be duplicated");
        assertEquals(8, buffer.length, "Length should be 8");
    }

} // class HTMLStringBufferTest
