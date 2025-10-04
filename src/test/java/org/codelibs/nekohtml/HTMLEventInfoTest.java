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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Test class for {@link HTMLEventInfo} interface and {@link HTMLEventInfo.SynthesizedItem}.
 *
 * @author CodeLibs Project
 */
public class HTMLEventInfoTest {

    /**
     * Test that SynthesizedItem can be instantiated.
     */
    @Test
    public void testSynthesizedItemInstantiation() {
        final HTMLEventInfo.SynthesizedItem item = new HTMLEventInfo.SynthesizedItem();
        assertNotNull(item, "SynthesizedItem should be instantiated");
    }

    /**
     * Test that SynthesizedItem returns -1 for begin line number.
     */
    @Test
    public void testSynthesizedItemBeginLineNumber() {
        final HTMLEventInfo.SynthesizedItem item = new HTMLEventInfo.SynthesizedItem();
        assertEquals(-1, item.getBeginLineNumber(), "Begin line number should be -1 for synthesized item");
    }

    /**
     * Test that SynthesizedItem returns -1 for begin column number.
     */
    @Test
    public void testSynthesizedItemBeginColumnNumber() {
        final HTMLEventInfo.SynthesizedItem item = new HTMLEventInfo.SynthesizedItem();
        assertEquals(-1, item.getBeginColumnNumber(), "Begin column number should be -1 for synthesized item");
    }

    /**
     * Test that SynthesizedItem returns -1 for begin character offset.
     */
    @Test
    public void testSynthesizedItemBeginCharacterOffset() {
        final HTMLEventInfo.SynthesizedItem item = new HTMLEventInfo.SynthesizedItem();
        assertEquals(-1, item.getBeginCharacterOffset(), "Begin character offset should be -1 for synthesized item");
    }

    /**
     * Test that SynthesizedItem returns -1 for end line number.
     */
    @Test
    public void testSynthesizedItemEndLineNumber() {
        final HTMLEventInfo.SynthesizedItem item = new HTMLEventInfo.SynthesizedItem();
        assertEquals(-1, item.getEndLineNumber(), "End line number should be -1 for synthesized item");
    }

    /**
     * Test that SynthesizedItem returns -1 for end column number.
     */
    @Test
    public void testSynthesizedItemEndColumnNumber() {
        final HTMLEventInfo.SynthesizedItem item = new HTMLEventInfo.SynthesizedItem();
        assertEquals(-1, item.getEndColumnNumber(), "End column number should be -1 for synthesized item");
    }

    /**
     * Test that SynthesizedItem returns -1 for end character offset.
     */
    @Test
    public void testSynthesizedItemEndCharacterOffset() {
        final HTMLEventInfo.SynthesizedItem item = new HTMLEventInfo.SynthesizedItem();
        assertEquals(-1, item.getEndCharacterOffset(), "End character offset should be -1 for synthesized item");
    }

    /**
     * Test that SynthesizedItem returns true for isSynthesized.
     */
    @Test
    public void testSynthesizedItemIsSynthesized() {
        final HTMLEventInfo.SynthesizedItem item = new HTMLEventInfo.SynthesizedItem();
        assertTrue(item.isSynthesized(), "SynthesizedItem should return true for isSynthesized()");
    }

    /**
     * Test that SynthesizedItem toString returns "synthesized".
     */
    @Test
    public void testSynthesizedItemToString() {
        final HTMLEventInfo.SynthesizedItem item = new HTMLEventInfo.SynthesizedItem();
        assertEquals("synthesized", item.toString(), "SynthesizedItem toString should return 'synthesized'");
    }

    /**
     * Test that SynthesizedItem implements HTMLEventInfo interface.
     */
    @Test
    public void testSynthesizedItemImplementsInterface() {
        final HTMLEventInfo.SynthesizedItem item = new HTMLEventInfo.SynthesizedItem();
        assertTrue(item instanceof HTMLEventInfo, "SynthesizedItem should implement HTMLEventInfo interface");
    }

    /**
     * Test that multiple SynthesizedItem instances behave consistently.
     */
    @Test
    public void testMultipleSynthesizedItemsConsistency() {
        final HTMLEventInfo.SynthesizedItem item1 = new HTMLEventInfo.SynthesizedItem();
        final HTMLEventInfo.SynthesizedItem item2 = new HTMLEventInfo.SynthesizedItem();

        // All instances should return the same values
        assertEquals(item1.getBeginLineNumber(), item2.getBeginLineNumber(), "All instances should return same begin line number");
        assertEquals(item1.getBeginColumnNumber(), item2.getBeginColumnNumber(), "All instances should return same begin column number");
        assertEquals(item1.getBeginCharacterOffset(), item2.getBeginCharacterOffset(),
                "All instances should return same begin character offset");
        assertEquals(item1.getEndLineNumber(), item2.getEndLineNumber(), "All instances should return same end line number");
        assertEquals(item1.getEndColumnNumber(), item2.getEndColumnNumber(), "All instances should return same end column number");
        assertEquals(item1.getEndCharacterOffset(), item2.getEndCharacterOffset(), "All instances should return same end character offset");
        assertEquals(item1.isSynthesized(), item2.isSynthesized(), "All instances should return same isSynthesized value");
        assertEquals(item1.toString(), item2.toString(), "All instances should return same toString value");
    }

    /**
     * Test that SynthesizedItem can be used polymorphically as HTMLEventInfo.
     */
    @Test
    public void testSynthesizedItemPolymorphism() {
        final HTMLEventInfo eventInfo = new HTMLEventInfo.SynthesizedItem();

        assertEquals(-1, eventInfo.getBeginLineNumber(), "Begin line number should be -1");
        assertEquals(-1, eventInfo.getBeginColumnNumber(), "Begin column number should be -1");
        assertEquals(-1, eventInfo.getBeginCharacterOffset(), "Begin character offset should be -1");
        assertEquals(-1, eventInfo.getEndLineNumber(), "End line number should be -1");
        assertEquals(-1, eventInfo.getEndColumnNumber(), "End column number should be -1");
        assertEquals(-1, eventInfo.getEndCharacterOffset(), "End character offset should be -1");
        assertTrue(eventInfo.isSynthesized(), "Should be synthesized");
    }

    /**
     * Test custom implementation of HTMLEventInfo interface.
     */
    @Test
    public void testCustomHTMLEventInfoImplementation() {
        final HTMLEventInfo customInfo = new HTMLEventInfo() {
            @Override
            public int getBeginLineNumber() {
                return 10;
            }

            @Override
            public int getBeginColumnNumber() {
                return 5;
            }

            @Override
            public int getBeginCharacterOffset() {
                return 100;
            }

            @Override
            public int getEndLineNumber() {
                return 15;
            }

            @Override
            public int getEndColumnNumber() {
                return 20;
            }

            @Override
            public int getEndCharacterOffset() {
                return 200;
            }

            @Override
            public boolean isSynthesized() {
                return false;
            }
        };

        assertEquals(10, customInfo.getBeginLineNumber(), "Custom begin line number should be 10");
        assertEquals(5, customInfo.getBeginColumnNumber(), "Custom begin column number should be 5");
        assertEquals(100, customInfo.getBeginCharacterOffset(), "Custom begin character offset should be 100");
        assertEquals(15, customInfo.getEndLineNumber(), "Custom end line number should be 15");
        assertEquals(20, customInfo.getEndColumnNumber(), "Custom end column number should be 20");
        assertEquals(200, customInfo.getEndCharacterOffset(), "Custom end character offset should be 200");
        assertEquals(false, customInfo.isSynthesized(), "Custom implementation should not be synthesized");
    }

} // class HTMLEventInfoTest
