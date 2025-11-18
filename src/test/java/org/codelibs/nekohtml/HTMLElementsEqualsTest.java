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

import org.junit.jupiter.api.Test;

/**
 * Tests for HTMLElements.Element.equals() method fix.
 * This test verifies that the equals() method properly compares Element objects
 * rather than incorrectly comparing with String objects.
 */
public class HTMLElementsEqualsTest {

    @Test
    public void testElementEqualsItself() {
        final HTMLElements.Element element = HTMLElements.getElement("DIV");
        assertTrue(element.equals(element), "Element should equal itself");
    }

    @Test
    public void testElementEqualsAnotherWithSameName() {
        final HTMLElements.Element div1 = HTMLElements.getElement("DIV");
        final HTMLElements.Element div2 = HTMLElements.getElement("DIV");
        assertTrue(div1.equals(div2), "Elements with same name should be equal");
        assertTrue(div2.equals(div1), "Equality should be symmetric");
    }

    @Test
    public void testElementNotEqualsElementWithDifferentName() {
        final HTMLElements.Element div = HTMLElements.getElement("DIV");
        final HTMLElements.Element span = HTMLElements.getElement("SPAN");
        assertFalse(div.equals(span), "Elements with different names should not be equal");
    }

    @Test
    public void testElementNotEqualsNull() {
        final HTMLElements.Element element = HTMLElements.getElement("DIV");
        assertFalse(element.equals(null), "Element should not equal null");
    }

    @Test
    public void testElementNotEqualsString() {
        final HTMLElements.Element element = HTMLElements.getElement("DIV");
        // This was the bug - equals() was incorrectly comparing with String
        assertFalse(element.equals("DIV"), "Element should not equal String");
    }

    @Test
    public void testElementNotEqualsOtherType() {
        final HTMLElements.Element element = HTMLElements.getElement("DIV");
        assertFalse(element.equals(Integer.valueOf(42)), "Element should not equal Integer");
        assertFalse(element.equals(new Object()), "Element should not equal Object");
    }

    @Test
    public void testHashCodeConsistency() {
        final HTMLElements.Element div1 = HTMLElements.getElement("DIV");
        final HTMLElements.Element div2 = HTMLElements.getElement("DIV");
        assertEquals(div1.hashCode(), div2.hashCode(), "Equal elements should have same hash code");
    }

    @Test
    public void testEqualsTransitivity() {
        final HTMLElements.Element div1 = HTMLElements.getElement("DIV");
        final HTMLElements.Element div2 = HTMLElements.getElement("DIV");
        final HTMLElements.Element div3 = HTMLElements.getElement("DIV");

        assertTrue(div1.equals(div2), "div1 equals div2");
        assertTrue(div2.equals(div3), "div2 equals div3");
        assertTrue(div1.equals(div3), "div1 equals div3 (transitivity)");
    }

    @Test
    public void testCaseInsensitiveElementRetrieval() {
        final HTMLElements.Element divUpper = HTMLElements.getElement("DIV");
        final HTMLElements.Element divLower = HTMLElements.getElement("div");
        final HTMLElements.Element divMixed = HTMLElements.getElement("DiV");

        assertTrue(divUpper.equals(divLower), "Case-insensitive retrieval should return equal elements");
        assertTrue(divUpper.equals(divMixed), "Case-insensitive retrieval should return equal elements");
    }

    @Test
    public void testFormattingElements() {
        final HTMLElements.Element bold = HTMLElements.getElement("B");
        final HTMLElements.Element italic = HTMLElements.getElement("I");
        final HTMLElements.Element strong = HTMLElements.getElement("STRONG");

        assertNotEquals(bold, italic, "Different formatting elements should not be equal");
        assertNotEquals(bold, strong, "Different formatting elements should not be equal");
    }

    @Test
    public void testHTML5SemanticElements() {
        final HTMLElements.Element article = HTMLElements.getElement("ARTICLE");
        final HTMLElements.Element section = HTMLElements.getElement("SECTION");
        final HTMLElements.Element nav = HTMLElements.getElement("NAV");

        assertNotEquals(article, section, "Different HTML5 elements should not be equal");
        assertNotEquals(section, nav, "Different HTML5 elements should not be equal");
    }
}
