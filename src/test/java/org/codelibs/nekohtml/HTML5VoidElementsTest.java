/*
 * Copyright Marc Guillemot
 * Copyright 2002-2009 Andy Clark, Marc Guillemot
 * Copyright 2017-2024 Shinsuke Sugaya
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codelibs.nekohtml;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Unit tests for HTML5 void elements.
 *
 * @author Shinsuke Sugaya
 */
public class HTML5VoidElementsTest {

    @Test
    @DisplayName("All HTML5 void elements should be defined as EMPTY")
    void testHTML5VoidElements() {
        // HTML5 void elements according to the specification
        String[] voidElementNames =
                { "AREA", "BASE", "BR", "COL", "EMBED", "HR", "IMG", "INPUT", "LINK", "META", "PARAM", "SOURCE", "TRACK", "WBR" };

        for (String elementName : voidElementNames) {
            HTMLElements.Element element = HTMLElements.getElement(elementName);
            assertNotNull(element, elementName + " element should exist");
            assertTrue(element.isEmpty(), elementName + " should be a void (empty) element");
            assertFalse(element.isContainer(), elementName + " should not be a container element");
        }
    }

    @Test
    @DisplayName("HTML5 non-void elements should not be EMPTY")
    void testHTML5NonVoidElements() {
        // HTML5 elements that are commonly misunderstood as void but are actually containers
        String[] containerElementNames = { "AUDIO", "VIDEO", "CANVAS", "PICTURE", "PROGRESS", "METER", "OUTPUT" };

        for (String elementName : containerElementNames) {
            HTMLElements.Element element = HTMLElements.getElement(elementName);
            assertNotNull(element, elementName + " element should exist");
            assertFalse(element.isEmpty(), elementName + " should NOT be a void (empty) element");
            assertTrue(element.isContainer(), elementName + " should be a container element");
        }
    }

    @Test
    @DisplayName("Legacy void elements should remain EMPTY")
    void testLegacyVoidElements() {
        // Legacy HTML void elements that should remain void
        String[] legacyVoidElements = { "BASEFONT", "FRAME", "ISINDEX", "KEYGEN" };

        for (String elementName : legacyVoidElements) {
            HTMLElements.Element element = HTMLElements.getElement(elementName);
            assertNotNull(element, elementName + " element should exist");
            assertTrue(element.isEmpty(), elementName + " should remain a void (empty) element");
            assertFalse(element.isContainer(), elementName + " should not be a container element");
        }
    }

    @Test
    @DisplayName("Void element constants should be correctly defined")
    void testVoidElementConstants() {
        // Test that the element constants match the expected values
        assertEquals("AREA", HTMLElements.getElement(HTMLElements.AREA).name);
        assertEquals("BASE", HTMLElements.getElement(HTMLElements.BASE).name);
        assertEquals("BR", HTMLElements.getElement(HTMLElements.BR).name);
        assertEquals("COL", HTMLElements.getElement(HTMLElements.COL).name);
        assertEquals("EMBED", HTMLElements.getElement(HTMLElements.EMBED).name);
        assertEquals("HR", HTMLElements.getElement(HTMLElements.HR).name);
        assertEquals("IMG", HTMLElements.getElement(HTMLElements.IMG).name);
        assertEquals("INPUT", HTMLElements.getElement(HTMLElements.INPUT).name);
        assertEquals("LINK", HTMLElements.getElement(HTMLElements.LINK).name);
        assertEquals("META", HTMLElements.getElement(HTMLElements.META).name);
        assertEquals("PARAM", HTMLElements.getElement(HTMLElements.PARAM).name);
        assertEquals("SOURCE", HTMLElements.getElement(HTMLElements.SOURCE).name);
        assertEquals("TRACK", HTMLElements.getElement(HTMLElements.TRACK).name);
        assertEquals("WBR", HTMLElements.getElement(HTMLElements.WBR).name);

        // All should be empty
        assertTrue(HTMLElements.getElement(HTMLElements.AREA).isEmpty());
        assertTrue(HTMLElements.getElement(HTMLElements.BASE).isEmpty());
        assertTrue(HTMLElements.getElement(HTMLElements.BR).isEmpty());
        assertTrue(HTMLElements.getElement(HTMLElements.COL).isEmpty());
        assertTrue(HTMLElements.getElement(HTMLElements.EMBED).isEmpty());
        assertTrue(HTMLElements.getElement(HTMLElements.HR).isEmpty());
        assertTrue(HTMLElements.getElement(HTMLElements.IMG).isEmpty());
        assertTrue(HTMLElements.getElement(HTMLElements.INPUT).isEmpty());
        assertTrue(HTMLElements.getElement(HTMLElements.LINK).isEmpty());
        assertTrue(HTMLElements.getElement(HTMLElements.META).isEmpty());
        assertTrue(HTMLElements.getElement(HTMLElements.PARAM).isEmpty());
        assertTrue(HTMLElements.getElement(HTMLElements.SOURCE).isEmpty());
        assertTrue(HTMLElements.getElement(HTMLElements.TRACK).isEmpty());
        assertTrue(HTMLElements.getElement(HTMLElements.WBR).isEmpty());
    }

    @Test
    @DisplayName("Container elements should not be EMPTY")
    void testContainerElementsNotEmpty() {
        // HTML5 elements that should be containers, not empty
        String[] containerElements =
                { "DIV", "SPAN", "P", "SECTION", "ARTICLE", "ASIDE", "NAV", "HEADER", "FOOTER", "MAIN", "AUDIO", "VIDEO", "CANVAS",
                        "PICTURE", "PROGRESS", "METER", "OUTPUT" };

        for (String elementName : containerElements) {
            HTMLElements.Element element = HTMLElements.getElement(elementName);
            assertNotNull(element, elementName + " element should exist");
            assertFalse(element.isEmpty(), elementName + " should not be empty");
        }
    }
}