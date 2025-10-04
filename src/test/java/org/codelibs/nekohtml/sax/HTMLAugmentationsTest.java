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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link HTMLAugmentations}.
 *
 * @author CodeLibs Project
 */
public class HTMLAugmentationsTest {

    private HTMLAugmentations augmentations;

    @BeforeEach
    public void setUp() {
        augmentations = new HTMLAugmentations();
    }

    @Test
    public void testDefaultConstructor() {
        // When: Creating with default constructor
        final HTMLAugmentations aug = new HTMLAugmentations();

        // Then: Should be empty (lazy initialization)
        assertNull(aug.getItem("any-key"), "New augmentations should have no items");
        assertFalse(aug.containsItem("any-key"), "New augmentations should not contain any items");
    }

    @Test
    public void testPutItem() {
        // When: Adding an item
        final Object result = augmentations.putItem("key1", "value1");

        // Then: Should return null (no previous value) and store item
        assertNull(result, "First put should return null");
        assertEquals("value1", augmentations.getItem("key1"), "Should store the item");
    }

    @Test
    public void testPutItemOverwrite() {
        // Given: Existing item
        augmentations.putItem("key1", "value1");

        // When: Overwriting with new value
        final Object result = augmentations.putItem("key1", "value2");

        // Then: Should return previous value and update
        assertEquals("value1", result, "Should return previous value");
        assertEquals("value2", augmentations.getItem("key1"), "Should have new value");
    }

    @Test
    public void testPutMultipleItems() {
        // When: Adding multiple items
        augmentations.putItem("key1", "value1");
        augmentations.putItem("key2", "value2");
        augmentations.putItem("key3", "value3");

        // Then: All items should be stored
        assertEquals("value1", augmentations.getItem("key1"), "Should have first item");
        assertEquals("value2", augmentations.getItem("key2"), "Should have second item");
        assertEquals("value3", augmentations.getItem("key3"), "Should have third item");
    }

    @Test
    public void testGetItemNonExistent() {
        // When: Getting non-existent item
        final Object result = augmentations.getItem("non-existent");

        // Then: Should return null
        assertNull(result, "Getting non-existent item should return null");
    }

    @Test
    public void testGetItemBeforePut() {
        // When: Getting item before any put (lazy initialization)
        final Object result = augmentations.getItem("key1");

        // Then: Should return null without NPE
        assertNull(result, "Getting item before initialization should return null");
    }

    @Test
    public void testGetItemAfterPut() {
        // Given: Item added
        augmentations.putItem("key1", "value1");

        // When: Getting the item
        final Object result = augmentations.getItem("key1");

        // Then: Should return the value
        assertEquals("value1", result, "Should get the stored value");
    }

    @Test
    public void testRemoveItem() {
        // Given: Existing item
        augmentations.putItem("key1", "value1");

        // When: Removing the item
        final Object result = augmentations.removeItem("key1");

        // Then: Should return the value and remove it
        assertEquals("value1", result, "Should return removed value");
        assertNull(augmentations.getItem("key1"), "Item should be removed");
    }

    @Test
    public void testRemoveItemNonExistent() {
        // Given: Item added
        augmentations.putItem("key1", "value1");

        // When: Removing non-existent item
        final Object result = augmentations.removeItem("non-existent");

        // Then: Should return null
        assertNull(result, "Removing non-existent item should return null");
        assertEquals("value1", augmentations.getItem("key1"), "Other items should remain");
    }

    @Test
    public void testRemoveItemBeforeInitialization() {
        // When: Removing item before any put (lazy initialization)
        final Object result = augmentations.removeItem("key1");

        // Then: Should return null without NPE
        assertNull(result, "Removing before initialization should return null");
    }

    @Test
    public void testRemoveAllItems() {
        // Given: Multiple items
        augmentations.putItem("key1", "value1");
        augmentations.putItem("key2", "value2");
        augmentations.putItem("key3", "value3");

        // When: Removing all items
        augmentations.removeAllItems();

        // Then: All items should be gone
        assertNull(augmentations.getItem("key1"), "All items should be removed");
        assertNull(augmentations.getItem("key2"), "All items should be removed");
        assertNull(augmentations.getItem("key3"), "All items should be removed");
        assertFalse(augmentations.containsItem("key1"), "Should not contain any items");
    }

    @Test
    public void testRemoveAllItemsBeforeInitialization() {
        // When: Removing all items before any put (lazy initialization)
        augmentations.removeAllItems();

        // Then: Should not throw NPE
        assertNull(augmentations.getItem("any-key"), "Should handle empty state");
    }

    @Test
    public void testContainsItem() {
        // Given: Existing item
        augmentations.putItem("key1", "value1");

        // When: Checking if item exists
        final boolean exists = augmentations.containsItem("key1");

        // Then: Should return true
        assertTrue(exists, "Should contain the item");
    }

    @Test
    public void testContainsItemNonExistent() {
        // Given: Some items
        augmentations.putItem("key1", "value1");

        // When: Checking for non-existent item
        final boolean exists = augmentations.containsItem("non-existent");

        // Then: Should return false
        assertFalse(exists, "Should not contain non-existent item");
    }

    @Test
    public void testContainsItemBeforeInitialization() {
        // When: Checking for item before any put (lazy initialization)
        final boolean exists = augmentations.containsItem("key1");

        // Then: Should return false without NPE
        assertFalse(exists, "Should return false before initialization");
    }

    @Test
    public void testContainsItemAfterRemove() {
        // Given: Item added and then removed
        augmentations.putItem("key1", "value1");
        augmentations.removeItem("key1");

        // When: Checking if item exists
        final boolean exists = augmentations.containsItem("key1");

        // Then: Should return false
        assertFalse(exists, "Should not contain removed item");
    }

    @Test
    public void testToStringEmpty() {
        // When: Converting empty augmentations to string
        final String result = augmentations.toString();

        // Then: Should return empty map representation
        assertEquals("{}", result, "Empty augmentations should return {}");
    }

    @Test
    public void testToStringWithItems() {
        // Given: Items added
        augmentations.putItem("key1", "value1");
        augmentations.putItem("key2", "value2");

        // When: Converting to string
        final String result = augmentations.toString();

        // Then: Should return map representation
        assertTrue(result.contains("key1"), "Should contain key1");
        assertTrue(result.contains("value1"), "Should contain value1");
        assertTrue(result.contains("key2"), "Should contain key2");
        assertTrue(result.contains("value2"), "Should contain value2");
    }

    @Test
    public void testToStringAfterRemoveAll() {
        // Given: Items added and then removed
        augmentations.putItem("key1", "value1");
        augmentations.putItem("key2", "value2");
        augmentations.removeAllItems();

        // When: Converting to string
        final String result = augmentations.toString();

        // Then: Should return empty map representation
        assertEquals("{}", result, "Should return {} after removing all items");
    }

    @Test
    public void testPutNullKey() {
        // When: Adding item with null key (allowed by HashMap)
        final Object result = augmentations.putItem(null, "value");

        // Then: Should store with null key
        assertNull(result, "First put should return null");
        assertEquals("value", augmentations.getItem(null), "Should store value with null key");
    }

    @Test
    public void testPutNullValue() {
        // When: Adding null value
        augmentations.putItem("key1", null);

        // Then: Should store null value
        assertTrue(augmentations.containsItem("key1"), "Should contain the key");
        assertNull(augmentations.getItem("key1"), "Should return null value");
    }

    @Test
    public void testComplexValueTypes() {
        // Given: Different value types
        final Integer intValue = 123;
        final Boolean boolValue = true;
        final Double doubleValue = 45.67;
        final Object objectValue = new Object();

        // When: Storing different types
        augmentations.putItem("int", intValue);
        augmentations.putItem("bool", boolValue);
        augmentations.putItem("double", doubleValue);
        augmentations.putItem("object", objectValue);

        // Then: All should be retrievable
        assertEquals(intValue, augmentations.getItem("int"), "Should store Integer");
        assertEquals(boolValue, augmentations.getItem("bool"), "Should store Boolean");
        assertEquals(doubleValue, augmentations.getItem("double"), "Should store Double");
        assertEquals(objectValue, augmentations.getItem("object"), "Should store Object");
    }

    @Test
    public void testEmptyStringKey() {
        // When: Adding item with empty string key
        augmentations.putItem("", "value");

        // Then: Should store with empty key
        assertEquals("value", augmentations.getItem(""), "Should store value with empty key");
        assertTrue(augmentations.containsItem(""), "Should contain empty key");
    }

    @Test
    public void testLargeNumberOfItems() {
        // When: Adding many items
        for (int i = 0; i < 1000; i++) {
            augmentations.putItem("key" + i, "value" + i);
        }

        // Then: All should be retrievable
        for (int i = 0; i < 1000; i++) {
            assertEquals("value" + i, augmentations.getItem("key" + i), "Should have item " + i);
            assertTrue(augmentations.containsItem("key" + i), "Should contain item " + i);
        }
    }

    @Test
    public void testRemoveAndReAdd() {
        // Given: Item added, removed, and re-added
        augmentations.putItem("key1", "value1");
        augmentations.removeItem("key1");
        augmentations.putItem("key1", "value2");

        // When: Getting the item
        final Object result = augmentations.getItem("key1");

        // Then: Should have the new value
        assertEquals("value2", result, "Should have re-added value");
    }

    @Test
    public void testMultipleRemoveAllItems() {
        // Given: Items added
        augmentations.putItem("key1", "value1");
        augmentations.putItem("key2", "value2");

        // When: Calling removeAllItems multiple times
        augmentations.removeAllItems();
        augmentations.removeAllItems();
        augmentations.removeAllItems();

        // Then: Should handle gracefully
        assertNull(augmentations.getItem("key1"), "Should remain empty");
        assertFalse(augmentations.containsItem("key1"), "Should not contain items");
    }

    @Test
    public void testSequentialOperations() {
        // When: Performing sequence of operations
        augmentations.putItem("key1", "value1");
        assertEquals("value1", augmentations.getItem("key1"));

        augmentations.putItem("key2", "value2");
        assertEquals("value2", augmentations.getItem("key2"));

        augmentations.removeItem("key1");
        assertNull(augmentations.getItem("key1"));
        assertEquals("value2", augmentations.getItem("key2"));

        augmentations.putItem("key3", "value3");
        assertEquals("value3", augmentations.getItem("key3"));

        augmentations.removeAllItems();
        assertNull(augmentations.getItem("key2"));
        assertNull(augmentations.getItem("key3"));

        // Then: Should end in clean state
        assertFalse(augmentations.containsItem("key1"), "Should not contain any items");
        assertFalse(augmentations.containsItem("key2"), "Should not contain any items");
        assertFalse(augmentations.containsItem("key3"), "Should not contain any items");
    }

    @Test
    public void testHTMLQNameAsValue() {
        // Given: HTMLQName as value
        final HTMLQName qname = new HTMLQName("http://example.com", "element", "ns:element");

        // When: Storing HTMLQName
        augmentations.putItem("qname", qname);

        // Then: Should store and retrieve correctly
        final Object result = augmentations.getItem("qname");
        assertSame(qname, result, "Should return same HTMLQName instance");
        assertTrue(result instanceof HTMLQName, "Should be HTMLQName instance");
    }

    @Test
    public void testHTMLAttributesImplAsValue() {
        // Given: HTMLAttributesImpl as value
        final HTMLAttributesImpl attrs = new HTMLAttributesImpl();
        attrs.addAttributeWithIndex("", "id", "id", "CDATA", "test");

        // When: Storing HTMLAttributesImpl
        augmentations.putItem("attributes", attrs);

        // Then: Should store and retrieve correctly
        final Object result = augmentations.getItem("attributes");
        assertSame(attrs, result, "Should return same HTMLAttributesImpl instance");
        assertTrue(result instanceof HTMLAttributesImpl, "Should be HTMLAttributesImpl instance");
    }

    @Test
    public void testCaseSensitiveKeys() {
        // When: Adding items with case-different keys
        augmentations.putItem("Key", "value1");
        augmentations.putItem("key", "value2");
        augmentations.putItem("KEY", "value3");

        // Then: Should treat as different keys
        assertEquals("value1", augmentations.getItem("Key"), "Should be case sensitive");
        assertEquals("value2", augmentations.getItem("key"), "Should be case sensitive");
        assertEquals("value3", augmentations.getItem("KEY"), "Should be case sensitive");
    }

    @Test
    public void testSpecialCharactersInKeys() {
        // When: Adding items with special characters in keys
        augmentations.putItem("key-with-dash", "value1");
        augmentations.putItem("key.with.dot", "value2");
        augmentations.putItem("key:with:colon", "value3");
        augmentations.putItem("key/with/slash", "value4");

        // Then: Should handle special characters
        assertEquals("value1", augmentations.getItem("key-with-dash"), "Should handle dash");
        assertEquals("value2", augmentations.getItem("key.with.dot"), "Should handle dot");
        assertEquals("value3", augmentations.getItem("key:with:colon"), "Should handle colon");
        assertEquals("value4", augmentations.getItem("key/with/slash"), "Should handle slash");
    }

    @Test
    public void testUnicodeKeys() {
        // When: Adding items with Unicode keys
        augmentations.putItem("キー", "日本語");
        augmentations.putItem("键", "中文");
        augmentations.putItem("ключ", "русский");

        // Then: Should handle Unicode correctly
        assertEquals("日本語", augmentations.getItem("キー"), "Should handle Japanese");
        assertEquals("中文", augmentations.getItem("键"), "Should handle Chinese");
        assertEquals("русский", augmentations.getItem("ключ"), "Should handle Russian");
    }

} // class HTMLAugmentationsTest
