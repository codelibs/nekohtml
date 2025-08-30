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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HTMLAugmentations}.
 *
 * These tests focus on the basic map-like behavior, clearing methods
 * compatibility (clear and removeAllItems), and the copy constructor's
 * special handling of {@link HTMLScanner.LocationItem} ensuring deep copy.
 */
public class HTMLAugmentationsTest {

    @Test
    public void putGetRemoveAndKeys() {
        final HTMLAugmentations augs = new HTMLAugmentations();

        // Initially empty
        assertNull(augs.getItem("missing"));
        assertFalse(augs.keys().hasMoreElements());

        // put returns previous value (null for first insert)
        assertNull(augs.putItem("k1", "v1"));
        assertEquals("v1", augs.getItem("k1"));

        // Replace existing value; put returns previous
        assertEquals("v1", augs.putItem("k1", "v2"));
        assertEquals("v2", augs.getItem("k1"));

        // Add more keys
        final Object marker = new Object();
        augs.putItem("k2", marker);
        augs.putItem("k3", Integer.valueOf(3));

        // keys() exposes all keys; order is not guaranteed
        final Set<String> keySet = toSet(augs.keys());
        assertEquals(Set.of("k1", "k2", "k3"), keySet);

        // remove returns previous value and removes mapping
        assertSame(marker, augs.removeItem("k2"));
        assertNull(augs.removeItem("k2")); // removing again yields null
        assertEquals(Set.of("k1", "k3"), toSet(augs.keys()));
    }

    @Test
    public void removeAllItemsClearsAllEntries() {
        final HTMLAugmentations augs = new HTMLAugmentations();
        augs.putItem("a", 1);
        augs.putItem("b", 2);

        augs.removeAllItems();
        assertFalse(augs.keys().hasMoreElements());

        // Can be reused after clearing
        augs.putItem("c", 3);
        assertEquals(Set.of("c"), toSet(augs.keys()));
    }

    @Test
    public void clearClearsAllEntries() {
        final HTMLAugmentations augs = new HTMLAugmentations();
        augs.putItem("x", "y");
        augs.putItem("z", "w");

        augs.clear();
        assertFalse(augs.keys().hasMoreElements());
    }

    @Test
    public void copyConstructorDeepCopiesLocationItemAndSharesOthers() {
        // Prepare original with both a LocationItem and a regular object
        final HTMLAugmentations original = new HTMLAugmentations();

        final HTMLScanner.LocationItem loc = new HTMLScanner.LocationItem();
        loc.setValues(1, 2, 3, 4, 5, 6);
        original.putItem("loc", loc);

        final Object other = new Object();
        original.putItem("other", other);

        // Invoke copy constructor (package-private) to exercise copy path
        final HTMLAugmentations copy = new HTMLAugmentations(original);

        // Regular objects are the same instance (shallow copy)
        assertSame(other, copy.getItem("other"));

        // LocationItem is deep-copied into a distinct instance with same values
        final Object copiedLocObj = copy.getItem("loc");
        assertTrue(copiedLocObj instanceof HTMLScanner.LocationItem);
        final HTMLScanner.LocationItem copiedLoc = (HTMLScanner.LocationItem) copiedLocObj;

        assertNotSame(loc, copiedLoc);
        assertEquals(1, copiedLoc.getBeginLineNumber());
        assertEquals(2, copiedLoc.getBeginColumnNumber());
        assertEquals(3, copiedLoc.getBeginCharacterOffset());
        assertEquals(4, copiedLoc.getEndLineNumber());
        assertEquals(5, copiedLoc.getEndColumnNumber());
        assertEquals(6, copiedLoc.getEndCharacterOffset());

        // Mutating the original LocationItem does not affect the copied one
        loc.setValues(10, 20, 30, 40, 50, 60);
        assertEquals(1, copiedLoc.getBeginLineNumber());
        assertEquals(2, copiedLoc.getBeginColumnNumber());
        assertEquals(3, copiedLoc.getBeginCharacterOffset());
        assertEquals(4, copiedLoc.getEndLineNumber());
        assertEquals(5, copiedLoc.getEndColumnNumber());
        assertEquals(6, copiedLoc.getEndCharacterOffset());

        // Keys are preserved in the copy
        final Set<String> copiedKeys = toSet(copy.keys());
        assertEquals(Set.of("loc", "other"), copiedKeys);
    }

    @Test
    public void testRemoveNonExistingKey() {
        final HTMLAugmentations augs = new HTMLAugmentations();

        // Remove non-existing key returns null
        assertNull(augs.removeItem("nonExisting"));

        // Add and remove
        augs.putItem("key", "value");
        assertEquals("value", augs.removeItem("key"));

        // Remove again returns null
        assertNull(augs.removeItem("key"));

        // Get non-existing key returns null
        assertNull(augs.getItem("nonExisting"));
    }

    @Test
    public void copyConstructorWithEmptySource() {
        final HTMLAugmentations empty = new HTMLAugmentations();
        final HTMLAugmentations copy = new HTMLAugmentations(empty);

        // Copy is also empty
        assertFalse(copy.keys().hasMoreElements());

        // Adding to original doesn't affect copy
        empty.putItem("key", "value");
        assertFalse(copy.keys().hasMoreElements());

        // Adding to copy doesn't affect original
        copy.putItem("copyKey", "copyValue");
        assertEquals(1, toSet(empty.keys()).size());
        assertEquals(1, toSet(copy.keys()).size());
    }

    @Test
    public void testMultipleKeysEnumeration() {
        final HTMLAugmentations augs = new HTMLAugmentations();

        // Add multiple entries
        for (int i = 0; i < 10; i++) {
            augs.putItem("key" + i, "value" + i);
        }

        // Verify all keys are present
        final Set<String> keySet = toSet(augs.keys());
        assertEquals(10, keySet.size());
        for (int i = 0; i < 10; i++) {
            assertTrue(keySet.contains("key" + i));
            assertEquals("value" + i, augs.getItem("key" + i));
        }

        // Enumeration can be iterated multiple times
        final Enumeration<String> keys1 = augs.keys();
        final Enumeration<String> keys2 = augs.keys();
        assertEquals(toSet(keys1), toSet(keys2));
    }

    @Test
    public void testLargeNumberOfEntries() {
        final HTMLAugmentations augs = new HTMLAugmentations();
        final int count = 1000;

        // Add many entries
        for (int i = 0; i < count; i++) {
            assertNull(augs.putItem("k" + i, Integer.valueOf(i)));
        }

        // Verify all entries
        assertEquals(count, toSet(augs.keys()).size());
        for (int i = 0; i < count; i++) {
            assertEquals(Integer.valueOf(i), augs.getItem("k" + i));
        }

        // Clear and verify empty
        augs.removeAllItems();
        assertFalse(augs.keys().hasMoreElements());

        // Can still add after clearing large number
        augs.putItem("new", "value");
        assertEquals("value", augs.getItem("new"));
    }

    @Test
    public void testCopyConstructorPreservesAllTypes() {
        final HTMLAugmentations original = new HTMLAugmentations();
        original.putItem("string", "value");
        original.putItem("integer", Integer.valueOf(42));
        original.putItem("object", new Object());

        final HTMLAugmentations copy = new HTMLAugmentations(original);

        // All types of values are copied
        assertEquals("value", copy.getItem("string"));
        assertEquals(Integer.valueOf(42), copy.getItem("integer"));
        assertNotNull(copy.getItem("object"));
        assertEquals(3, toSet(copy.keys()).size());
    }

    @Test
    public void testReplaceValueMultipleTimes() {
        final HTMLAugmentations augs = new HTMLAugmentations();

        // Replace value multiple times
        assertNull(augs.putItem("key", "v1"));
        assertEquals("v1", augs.putItem("key", "v2"));
        assertEquals("v2", augs.putItem("key", "v3"));
        assertEquals("v3", augs.putItem("key", "v4"));

        assertEquals("v4", augs.getItem("key"));
    }

    private static Set<String> toSet(final Enumeration<String> e) {
        final List<String> list = Collections.list(e);
        return list.stream().collect(Collectors.toSet());
    }
}
