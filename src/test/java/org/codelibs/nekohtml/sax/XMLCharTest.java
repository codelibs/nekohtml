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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Test class for {@link XMLChar}.
 *
 * @author CodeLibs Project
 */
public class XMLCharTest {

    // isValid() tests

    @Test
    public void testIsValidTab() {
        // Given: Tab character (0x09)
        // When/Then: Should be valid
        assertTrue(XMLChar.isValid(0x09), "Tab should be valid XML character");
    }

    @Test
    public void testIsValidLineFeed() {
        // Given: Line feed character (0x0A)
        // When/Then: Should be valid
        assertTrue(XMLChar.isValid(0x0A), "Line feed should be valid XML character");
    }

    @Test
    public void testIsValidCarriageReturn() {
        // Given: Carriage return character (0x0D)
        // When/Then: Should be valid
        assertTrue(XMLChar.isValid(0x0D), "Carriage return should be valid XML character");
    }

    @Test
    public void testIsValidSpaceRange() {
        // Given: Characters in range 0x20-0xD7FF
        // When/Then: Should be valid
        assertTrue(XMLChar.isValid(0x20), "0x20 should be valid");
        assertTrue(XMLChar.isValid(0x7F), "0x7F should be valid");
        assertTrue(XMLChar.isValid(0xD7FF), "0xD7FF should be valid");
    }

    @Test
    public void testIsValidPrivateUseRange() {
        // Given: Characters in range 0xE000-0xFFFD
        // When/Then: Should be valid
        assertTrue(XMLChar.isValid(0xE000), "0xE000 should be valid");
        assertTrue(XMLChar.isValid(0xF000), "0xF000 should be valid");
        assertTrue(XMLChar.isValid(0xFFFD), "0xFFFD should be valid");
    }

    @Test
    public void testIsValidSupplementaryRange() {
        // Given: Characters in range 0x10000-0x10FFFF
        // When/Then: Should be valid
        assertTrue(XMLChar.isValid(0x10000), "0x10000 should be valid");
        assertTrue(XMLChar.isValid(0x50000), "0x50000 should be valid");
        assertTrue(XMLChar.isValid(0x10FFFF), "0x10FFFF should be valid");
    }

    @Test
    public void testIsValidInvalidCharacters() {
        // Given: Invalid characters
        // When/Then: Should be invalid
        assertFalse(XMLChar.isValid(0x00), "0x00 should be invalid");
        assertFalse(XMLChar.isValid(0x08), "0x08 should be invalid");
        assertFalse(XMLChar.isValid(0x0B), "0x0B should be invalid");
        assertFalse(XMLChar.isValid(0x0C), "0x0C should be invalid");
        assertFalse(XMLChar.isValid(0x0E), "0x0E should be invalid");
        assertFalse(XMLChar.isValid(0x1F), "0x1F should be invalid");
        assertFalse(XMLChar.isValid(0xD800), "0xD800 (surrogate) should be invalid");
        assertFalse(XMLChar.isValid(0xDFFF), "0xDFFF (surrogate) should be invalid");
        assertFalse(XMLChar.isValid(0xFFFE), "0xFFFE should be invalid");
        assertFalse(XMLChar.isValid(0xFFFF), "0xFFFF should be invalid");
        assertFalse(XMLChar.isValid(0x110000), "0x110000 (beyond Unicode) should be invalid");
    }

    @Test
    public void testIsValidBoundaryValues() {
        // Given: Boundary values
        // When/Then: Test boundaries
        assertFalse(XMLChar.isValid(0x1F), "Just before 0x20 should be invalid");
        assertTrue(XMLChar.isValid(0x20), "0x20 should be valid");
        assertTrue(XMLChar.isValid(0xD7FF), "0xD7FF should be valid");
        assertFalse(XMLChar.isValid(0xD800), "0xD800 should be invalid");
        assertFalse(XMLChar.isValid(0xDFFF), "0xDFFF should be invalid");
        assertTrue(XMLChar.isValid(0xE000), "0xE000 should be valid");
        assertTrue(XMLChar.isValid(0xFFFD), "0xFFFD should be valid");
        assertFalse(XMLChar.isValid(0xFFFE), "0xFFFE should be invalid");
    }

    // isInvalid() tests

    @Test
    public void testIsInvalidForValidCharacters() {
        // Given: Valid characters
        // When/Then: Should not be invalid
        assertFalse(XMLChar.isInvalid(0x09), "Tab should not be invalid");
        assertFalse(XMLChar.isInvalid(0x20), "Space should not be invalid");
        assertFalse(XMLChar.isInvalid('A'), "A should not be invalid");
    }

    @Test
    public void testIsInvalidForInvalidCharacters() {
        // Given: Invalid characters
        // When/Then: Should be invalid
        assertTrue(XMLChar.isInvalid(0x00), "0x00 should be invalid");
        assertTrue(XMLChar.isInvalid(0x08), "0x08 should be invalid");
        assertTrue(XMLChar.isInvalid(0xFFFE), "0xFFFE should be invalid");
    }

    // isNameStart() tests

    @Test
    public void testIsNameStartUppercaseLetters() {
        // Given: Uppercase letters A-Z
        // When/Then: Should be valid name start
        assertTrue(XMLChar.isNameStart('A'), "A should be name start");
        assertTrue(XMLChar.isNameStart('M'), "M should be name start");
        assertTrue(XMLChar.isNameStart('Z'), "Z should be name start");
    }

    @Test
    public void testIsNameStartLowercaseLetters() {
        // Given: Lowercase letters a-z
        // When/Then: Should be valid name start
        assertTrue(XMLChar.isNameStart('a'), "a should be name start");
        assertTrue(XMLChar.isNameStart('m'), "m should be name start");
        assertTrue(XMLChar.isNameStart('z'), "z should be name start");
    }

    @Test
    public void testIsNameStartSpecialCharacters() {
        // Given: Special name start characters
        // When/Then: Should be valid
        assertTrue(XMLChar.isNameStart(':'), "Colon should be name start");
        assertTrue(XMLChar.isNameStart('_'), "Underscore should be name start");
    }

    @Test
    public void testIsNameStartUnicodeRanges() {
        // Given: Various Unicode ranges for name start
        // When/Then: Should be valid
        assertTrue(XMLChar.isNameStart(0xC0), "0xC0 should be name start");
        assertTrue(XMLChar.isNameStart(0xD6), "0xD6 should be name start");
        assertTrue(XMLChar.isNameStart(0xD8), "0xD8 should be name start");
        assertTrue(XMLChar.isNameStart(0xF6), "0xF6 should be name start");
        assertTrue(XMLChar.isNameStart(0xF8), "0xF8 should be name start");
        assertTrue(XMLChar.isNameStart(0x2FF), "0x2FF should be name start");
        assertTrue(XMLChar.isNameStart(0x370), "0x370 should be name start");
        assertTrue(XMLChar.isNameStart(0x37D), "0x37D should be name start");
        assertTrue(XMLChar.isNameStart(0x200C), "0x200C should be name start");
        assertTrue(XMLChar.isNameStart(0x200D), "0x200D should be name start");
        assertTrue(XMLChar.isNameStart(0x2070), "0x2070 should be name start");
        assertTrue(XMLChar.isNameStart(0x3001), "0x3001 should be name start");
        assertTrue(XMLChar.isNameStart(0xF900), "0xF900 should be name start");
        assertTrue(XMLChar.isNameStart(0xFDF0), "0xFDF0 should be name start");
        assertTrue(XMLChar.isNameStart(0x10000), "0x10000 should be name start");
        assertTrue(XMLChar.isNameStart(0xEFFFF), "0xEFFFF should be name start");
    }

    @Test
    public void testIsNameStartInvalidCharacters() {
        // Given: Characters not valid for name start
        // When/Then: Should not be name start
        assertFalse(XMLChar.isNameStart('0'), "Digit should not be name start");
        assertFalse(XMLChar.isNameStart('-'), "Hyphen should not be name start");
        assertFalse(XMLChar.isNameStart('.'), "Period should not be name start");
        assertFalse(XMLChar.isNameStart(' '), "Space should not be name start");
        assertFalse(XMLChar.isNameStart(0x40), "@-symbol should not be name start");
    }

    // isName() tests

    @Test
    public void testIsNameForNameStartCharacters() {
        // Given: Name start characters
        // When/Then: Should also be valid name characters
        assertTrue(XMLChar.isName('A'), "A should be name character");
        assertTrue(XMLChar.isName('z'), "z should be name character");
        assertTrue(XMLChar.isName(':'), "Colon should be name character");
        assertTrue(XMLChar.isName('_'), "Underscore should be name character");
    }

    @Test
    public void testIsNameForDigits() {
        // Given: Digits 0-9
        // When/Then: Should be valid name characters (but not name start)
        assertTrue(XMLChar.isName('0'), "0 should be name character");
        assertTrue(XMLChar.isName('5'), "5 should be name character");
        assertTrue(XMLChar.isName('9'), "9 should be name character");
    }

    @Test
    public void testIsNameForHyphenAndPeriod() {
        // Given: Hyphen and period
        // When/Then: Should be valid name characters
        assertTrue(XMLChar.isName('-'), "Hyphen should be name character");
        assertTrue(XMLChar.isName('.'), "Period should be name character");
    }

    @Test
    public void testIsNameForCombiningCharacters() {
        // Given: Combining characters
        // When/Then: Should be valid name characters
        assertTrue(XMLChar.isName(0xB7), "0xB7 should be name character");
        assertTrue(XMLChar.isName(0x0300), "0x0300 should be name character");
        assertTrue(XMLChar.isName(0x036F), "0x036F should be name character");
        assertTrue(XMLChar.isName(0x203F), "0x203F should be name character");
        assertTrue(XMLChar.isName(0x2040), "0x2040 should be name character");
    }

    @Test
    public void testIsNameInvalidCharacters() {
        // Given: Invalid name characters
        // When/Then: Should not be name characters
        assertFalse(XMLChar.isName(' '), "Space should not be name character");
        assertFalse(XMLChar.isName('!'), "! should not be name character");
        assertFalse(XMLChar.isName('@'), "@ should not be name character");
        assertFalse(XMLChar.isName('#'), "# should not be name character");
    }

    // isValidName() tests

    @Test
    public void testIsValidNameSimple() {
        // Given: Simple valid names
        // When/Then: Should be valid
        assertTrue(XMLChar.isValidName("element"), "element should be valid name");
        assertTrue(XMLChar.isValidName("Element"), "Element should be valid name");
        assertTrue(XMLChar.isValidName("_element"), "_element should be valid name");
        assertTrue(XMLChar.isValidName("element1"), "element1 should be valid name");
    }

    @Test
    public void testIsValidNameWithColon() {
        // Given: Name with colon (namespace prefix)
        // When/Then: Should be valid
        assertTrue(XMLChar.isValidName("ns:element"), "ns:element should be valid name");
        assertTrue(XMLChar.isValidName(":element"), ":element should be valid name");
    }

    @Test
    public void testIsValidNameWithHyphenAndPeriod() {
        // Given: Name with hyphen and period
        // When/Then: Should be valid
        assertTrue(XMLChar.isValidName("element-name"), "element-name should be valid name");
        assertTrue(XMLChar.isValidName("element.name"), "element.name should be valid name");
        assertTrue(XMLChar.isValidName("element-1.0"), "element-1.0 should be valid name");
    }

    @Test
    public void testIsValidNameNull() {
        // Given: Null name
        // When/Then: Should be invalid
        assertFalse(XMLChar.isValidName(null), "Null should be invalid name");
    }

    @Test
    public void testIsValidNameEmpty() {
        // Given: Empty name
        // When/Then: Should be invalid
        assertFalse(XMLChar.isValidName(""), "Empty string should be invalid name");
    }

    @Test
    public void testIsValidNameStartingWithDigit() {
        // Given: Name starting with digit
        // When/Then: Should be invalid
        assertFalse(XMLChar.isValidName("1element"), "Name starting with digit should be invalid");
        assertFalse(XMLChar.isValidName("9test"), "Name starting with 9 should be invalid");
    }

    @Test
    public void testIsValidNameStartingWithHyphen() {
        // Given: Name starting with hyphen
        // When/Then: Should be invalid
        assertFalse(XMLChar.isValidName("-element"), "Name starting with hyphen should be invalid");
    }

    @Test
    public void testIsValidNameStartingWithPeriod() {
        // Given: Name starting with period
        // When/Then: Should be invalid
        assertFalse(XMLChar.isValidName(".element"), "Name starting with period should be invalid");
    }

    @Test
    public void testIsValidNameWithInvalidCharacter() {
        // Given: Name with invalid characters
        // When/Then: Should be invalid
        assertFalse(XMLChar.isValidName("element name"), "Name with space should be invalid");
        assertFalse(XMLChar.isValidName("element!"), "Name with ! should be invalid");
        assertFalse(XMLChar.isValidName("element@test"), "Name with @ should be invalid");
        assertFalse(XMLChar.isValidName("element#1"), "Name with # should be invalid");
    }

    @Test
    public void testIsValidNameUnicode() {
        // Given: Unicode names
        // When/Then: Should be valid
        assertTrue(XMLChar.isValidName("element日本語"), "Name with Japanese should be valid");
        assertTrue(XMLChar.isValidName("日本語"), "Japanese name should be valid");
        assertTrue(XMLChar.isValidName("élément"), "Name with accents should be valid");
        assertTrue(XMLChar.isValidName("Элемент"), "Russian name should be valid");
    }

    @Test
    public void testIsValidNameSupplementaryCharacters() {
        // Given: Name with supplementary characters (beyond BMP)
        final String nameWithSupplementary = "element" + new String(Character.toChars(0x10000));
        // When/Then: Should be valid (0x10000 is valid name start)
        assertTrue(XMLChar.isValidName(nameWithSupplementary), "Name with supplementary character should be valid");
    }

    @Test
    public void testIsValidNameOnlyUnderscore() {
        // Given: Name with only underscore
        // When/Then: Should be valid
        assertTrue(XMLChar.isValidName("_"), "Single underscore should be valid name");
        assertTrue(XMLChar.isValidName("__"), "Double underscore should be valid name");
    }

    @Test
    public void testIsValidNameOnlyColon() {
        // Given: Name with only colon
        // When/Then: Should be valid
        assertTrue(XMLChar.isValidName(":"), "Single colon should be valid name");
        assertTrue(XMLChar.isValidName("::"), "Double colon should be valid name");
    }

    @Test
    public void testIsValidNameLongName() {
        // Given: Very long name
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append('a');
        }
        final String longName = sb.toString();

        // When/Then: Should be valid
        assertTrue(XMLChar.isValidName(longName), "Very long name should be valid");
    }

    // isSpace() tests

    @Test
    public void testIsSpaceForWhitespaceCharacters() {
        // Given: XML whitespace characters
        // When/Then: Should be space
        assertTrue(XMLChar.isSpace(' '), "Space should be whitespace");
        assertTrue(XMLChar.isSpace('\n'), "Line feed should be whitespace");
        assertTrue(XMLChar.isSpace('\t'), "Tab should be whitespace");
        assertTrue(XMLChar.isSpace('\r'), "Carriage return should be whitespace");
    }

    @Test
    public void testIsSpaceForNonWhitespaceCharacters() {
        // Given: Non-whitespace characters
        // When/Then: Should not be space
        assertFalse(XMLChar.isSpace('a'), "Letter should not be whitespace");
        assertFalse(XMLChar.isSpace('0'), "Digit should not be whitespace");
        assertFalse(XMLChar.isSpace('_'), "Underscore should not be whitespace");
        assertFalse(XMLChar.isSpace(0x0B), "Vertical tab should not be XML whitespace");
        assertFalse(XMLChar.isSpace(0x0C), "Form feed should not be XML whitespace");
        assertFalse(XMLChar.isSpace(0xA0), "Non-breaking space should not be XML whitespace");
    }

    @Test
    public void testIsSpaceIntegerValues() {
        // Given: Integer values for whitespace
        // When/Then: Should match character values
        assertTrue(XMLChar.isSpace(0x20), "0x20 (space) should be whitespace");
        assertTrue(XMLChar.isSpace(0x09), "0x09 (tab) should be whitespace");
        assertTrue(XMLChar.isSpace(0x0A), "0x0A (line feed) should be whitespace");
        assertTrue(XMLChar.isSpace(0x0D), "0x0D (carriage return) should be whitespace");
    }

    @Test
    public void testIsValidNameComplexUnicode() {
        // Given: Complex Unicode name
        final String name = "xml:日本語-element.1_test";
        // When/Then: Should be valid
        assertTrue(XMLChar.isValidName(name), "Complex Unicode name should be valid");
    }

    @Test
    public void testNameBoundaryConditions() {
        // Given: Boundary condition characters for name
        // When/Then: Test boundaries
        assertTrue(XMLChar.isName('9'), "9 should be name char");
        assertTrue(XMLChar.isName(':' - 1), "Character before : (9) should be name char"); // ':' - 1 = '9' which is valid
        assertTrue(XMLChar.isName(':'), ": should be name char and name start");
        assertTrue(XMLChar.isName('_'), "_ should be name char and name start");
        assertFalse(XMLChar.isNameStart(':' - 1), "9 should not be name start (but is name char)");
        assertTrue(XMLChar.isNameStart(':'), ": should be name start");
    }

    @Test
    public void testIsValidNameAllValidNameCharacters() {
        // Given: Name with all types of valid characters
        final String name = "Aa:_09-.";
        // When/Then: Should be valid if first char is name start
        assertTrue(XMLChar.isValidName(name), "Name with all valid characters should be valid");
    }

} // class XMLCharTest
