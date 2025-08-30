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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HTMLEventInfo} and, in particular,
 * the default implementation {@link HTMLEventInfo.SynthesizedItem}.
 */
public class HTMLEventInfoTest {

    /**
     * Verifies that a synthesized event reports the expected default
     * values for all location-related getters and the synthesized flag.
     */
    @Test
    @DisplayName("SynthesizedItem returns default values and synthesized=true")
    public void synthesizedItemDefaults() {
        final HTMLEventInfo info = new HTMLEventInfo.SynthesizedItem();

        // All location getters should return -1 for synthesized items
        // and the event should be marked as synthesized.
        assertAll(() -> assertEquals(-1, info.getBeginLineNumber(), "begin line"),
                () -> assertEquals(-1, info.getBeginColumnNumber(), "begin column"),
                () -> assertEquals(-1, info.getBeginCharacterOffset(), "begin offset"),
                () -> assertEquals(-1, info.getEndLineNumber(), "end line"),
                () -> assertEquals(-1, info.getEndColumnNumber(), "end column"),
                () -> assertEquals(-1, info.getEndCharacterOffset(), "end offset"),
                () -> assertTrue(info.isSynthesized(), "is synthesized"));
    }

    /**
     * Ensures that the textual representation is stable and informative.
     */
    @Test
    @DisplayName("SynthesizedItem.toString() returns 'synthesized'")
    public void synthesizedItemToString() {
        final HTMLEventInfo info = new HTMLEventInfo.SynthesizedItem();
        assertEquals("synthesized", info.toString());
    }

    /**
     * Calls the getters multiple times to assert idempotence and interface
     * polymorphism through the {@link HTMLEventInfo} type.
     */
    @Test
    @DisplayName("HTMLEventInfo polymorphic access is consistent")
    public void polymorphicAccessConsistency() {
        final HTMLEventInfo info = new HTMLEventInfo.SynthesizedItem();

        // First pass
        assertAll(() -> assertEquals(-1, info.getBeginLineNumber()), () -> assertEquals(-1, info.getBeginColumnNumber()),
                () -> assertEquals(-1, info.getBeginCharacterOffset()), () -> assertEquals(-1, info.getEndLineNumber()),
                () -> assertEquals(-1, info.getEndColumnNumber()), () -> assertEquals(-1, info.getEndCharacterOffset()));

        // Second pass to ensure the values are stable across invocations
        assertAll(() -> assertEquals(-1, info.getBeginLineNumber()), () -> assertEquals(-1, info.getBeginColumnNumber()),
                () -> assertEquals(-1, info.getBeginCharacterOffset()), () -> assertEquals(-1, info.getEndLineNumber()),
                () -> assertEquals(-1, info.getEndColumnNumber()), () -> assertEquals(-1, info.getEndCharacterOffset()));
    }
}
