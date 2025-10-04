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
 * Test class for {@link HTMLQName}.
 *
 * @author CodeLibs Project
 */
public class HTMLQNameTest {

    @Test
    public void testDefaultConstructor() {
        // When: Creating with default constructor
        final HTMLQName qname = new HTMLQName();

        // Then: All fields should be null
        assertNull(qname.uri, "URI should be null");
        assertNull(qname.localpart, "Local part should be null");
        assertNull(qname.rawname, "Raw name should be null");
    }

    @Test
    public void testRawnameConstructor() {
        // When: Creating with rawname only
        final HTMLQName qname = new HTMLQName("div");

        // Then: URI should be null, localpart and rawname should match
        assertNull(qname.uri, "URI should be null");
        assertEquals("div", qname.localpart, "Local part should equal rawname");
        assertEquals("div", qname.rawname, "Raw name should be set");
    }

    @Test
    public void testFullConstructor() {
        // When: Creating with all parameters
        final HTMLQName qname = new HTMLQName("http://www.w3.org/1999/xhtml", "div", "html:div");

        // Then: All fields should be set correctly
        assertEquals("http://www.w3.org/1999/xhtml", qname.uri, "URI should be set");
        assertEquals("div", qname.localpart, "Local part should be set");
        assertEquals("html:div", qname.rawname, "Raw name should be set");
    }

    @Test
    public void testCopyConstructor() {
        // Given: Original QName
        final HTMLQName original = new HTMLQName("http://example.com", "element", "ns:element");

        // When: Creating copy
        final HTMLQName copy = new HTMLQName(original);

        // Then: All fields should be copied
        assertEquals(original.uri, copy.uri, "URI should be copied");
        assertEquals(original.localpart, copy.localpart, "Local part should be copied");
        assertEquals(original.rawname, copy.rawname, "Raw name should be copied");
    }

    @Test
    public void testCopyConstructorIndependence() {
        // Given: Original QName
        final HTMLQName original = new HTMLQName("http://example.com", "element", "ns:element");
        final HTMLQName copy = new HTMLQName(original);

        // When: Modifying copy
        copy.setValues("http://different.com", "different", "ns:different");

        // Then: Original should be unchanged
        assertEquals("http://example.com", original.uri, "Original URI should be unchanged");
        assertEquals("element", original.localpart, "Original local part should be unchanged");
        assertEquals("ns:element", original.rawname, "Original raw name should be unchanged");
    }

    @Test
    public void testSetValuesWithParameters() {
        // Given: QName instance
        final HTMLQName qname = new HTMLQName();

        // When: Setting values with parameters
        qname.setValues("http://www.w3.org/2000/svg", "circle", "svg:circle");

        // Then: All fields should be updated
        assertEquals("http://www.w3.org/2000/svg", qname.uri, "URI should be set");
        assertEquals("circle", qname.localpart, "Local part should be set");
        assertEquals("svg:circle", qname.rawname, "Raw name should be set");
    }

    @Test
    public void testSetValuesWithQName() {
        // Given: Source and target QNames
        final HTMLQName source = new HTMLQName("http://example.com", "test", "ns:test");
        final HTMLQName target = new HTMLQName();

        // When: Setting values from source
        target.setValues(source);

        // Then: All fields should be copied
        assertEquals(source.uri, target.uri, "URI should be copied");
        assertEquals(source.localpart, target.localpart, "Local part should be copied");
        assertEquals(source.rawname, target.rawname, "Raw name should be copied");
    }

    @Test
    public void testClear() {
        // Given: QName with values
        final HTMLQName qname = new HTMLQName("http://example.com", "element", "ns:element");

        // When: Clearing values
        qname.clear();

        // Then: All fields should be null
        assertNull(qname.uri, "URI should be null after clear");
        assertNull(qname.localpart, "Local part should be null after clear");
        assertNull(qname.rawname, "Raw name should be null after clear");
    }

    @Test
    public void testEqualsWithSameInstance() {
        // Given: QName instance
        final HTMLQName qname = new HTMLQName("http://example.com", "test", "test");

        // When: Comparing with itself
        final boolean result = qname.equals(qname);

        // Then: Should be equal
        assertTrue(result, "QName should equal itself");
    }

    @Test
    public void testEqualsWithIdenticalQName() {
        // Given: Two identical QNames
        final HTMLQName qname1 = new HTMLQName("http://example.com", "test", "ns:test");
        final HTMLQName qname2 = new HTMLQName("http://example.com", "test", "ns:test");

        // When: Comparing
        final boolean result = qname1.equals(qname2);

        // Then: Should be equal
        assertTrue(result, "Identical QNames should be equal");
    }

    @Test
    public void testEqualsWithDifferentRawname() {
        // Given: QNames with different rawnames
        final HTMLQName qname1 = new HTMLQName("http://example.com", "test", "ns:test");
        final HTMLQName qname2 = new HTMLQName("http://example.com", "test", "other:test");

        // When: Comparing
        final boolean result = qname1.equals(qname2);

        // Then: Should not be equal
        assertFalse(result, "QNames with different rawnames should not be equal");
    }

    @Test
    public void testEqualsWithDifferentUri() {
        // Given: QNames with different URIs
        final HTMLQName qname1 = new HTMLQName("http://example.com", "test", "ns:test");
        final HTMLQName qname2 = new HTMLQName("http://different.com", "test", "ns:test");

        // When: Comparing
        final boolean result = qname1.equals(qname2);

        // Then: Should not be equal
        assertFalse(result, "QNames with different URIs should not be equal");
    }

    @Test
    public void testEqualsWithNull() {
        // Given: QName instance
        final HTMLQName qname = new HTMLQName("http://example.com", "test", "test");

        // When: Comparing with null
        final boolean result = qname.equals(null);

        // Then: Should not be equal
        assertFalse(result, "QName should not equal null");
    }

    @Test
    public void testEqualsWithDifferentClass() {
        // Given: QName instance
        final HTMLQName qname = new HTMLQName("http://example.com", "test", "test");

        // When: Comparing with different class
        final boolean result = qname.equals("not a QName");

        // Then: Should not be equal
        assertFalse(result, "QName should not equal object of different class");
    }

    @Test
    public void testEqualsWithBothNullRawname() {
        // Given: QNames with null rawnames but same URI
        final HTMLQName qname1 = new HTMLQName();
        qname1.uri = "http://example.com";
        qname1.rawname = null;

        final HTMLQName qname2 = new HTMLQName();
        qname2.uri = "http://example.com";
        qname2.rawname = null;

        // When: Comparing
        final boolean result = qname1.equals(qname2);

        // Then: Should be equal
        assertTrue(result, "QNames with both null rawnames should be equal if URIs match");
    }

    @Test
    public void testEqualsWithOneNullRawname() {
        // Given: One QName with null rawname, one with value
        final HTMLQName qname1 = new HTMLQName();
        qname1.uri = "http://example.com";
        qname1.rawname = null;

        final HTMLQName qname2 = new HTMLQName();
        qname2.uri = "http://example.com";
        qname2.rawname = "test";

        // When: Comparing
        final boolean result = qname1.equals(qname2);

        // Then: Should not be equal
        assertFalse(result, "QNames with one null rawname should not be equal");
    }

    @Test
    public void testEqualsWithBothNullUri() {
        // Given: QNames with null URIs but same rawname
        final HTMLQName qname1 = new HTMLQName();
        qname1.uri = null;
        qname1.rawname = "test";

        final HTMLQName qname2 = new HTMLQName();
        qname2.uri = null;
        qname2.rawname = "test";

        // When: Comparing
        final boolean result = qname1.equals(qname2);

        // Then: Should be equal
        assertTrue(result, "QNames with both null URIs should be equal if rawnames match");
    }

    @Test
    public void testEqualsWithOneNullUri() {
        // Given: One QName with null URI, one with value
        final HTMLQName qname1 = new HTMLQName();
        qname1.uri = null;
        qname1.rawname = "test";

        final HTMLQName qname2 = new HTMLQName();
        qname2.uri = "http://example.com";
        qname2.rawname = "test";

        // When: Comparing
        final boolean result = qname1.equals(qname2);

        // Then: Should not be equal
        assertFalse(result, "QNames with one null URI should not be equal");
    }

    @Test
    public void testHashCodeConsistency() {
        // Given: QName instance
        final HTMLQName qname = new HTMLQName("http://example.com", "test", "ns:test");

        // When: Getting hash code multiple times
        final int hash1 = qname.hashCode();
        final int hash2 = qname.hashCode();

        // Then: Should be consistent
        assertEquals(hash1, hash2, "Hash code should be consistent");
    }

    @Test
    public void testHashCodeEqualObjects() {
        // Given: Two equal QNames
        final HTMLQName qname1 = new HTMLQName("http://example.com", "test", "ns:test");
        final HTMLQName qname2 = new HTMLQName("http://example.com", "test", "ns:test");

        // When: Getting hash codes
        final int hash1 = qname1.hashCode();
        final int hash2 = qname2.hashCode();

        // Then: Hash codes should be equal
        assertEquals(hash1, hash2, "Equal objects should have equal hash codes");
    }

    @Test
    public void testHashCodeWithNullValues() {
        // Given: QName with null values
        final HTMLQName qname = new HTMLQName();

        // When: Getting hash code
        final int hash = qname.hashCode();

        // Then: Should not throw exception
        assertNotNull(hash, "Hash code should be calculable with null values");
    }

    @Test
    public void testHashCodeDifferentRawname() {
        // Given: QNames with different rawnames
        final HTMLQName qname1 = new HTMLQName("http://example.com", "test", "ns:test");
        final HTMLQName qname2 = new HTMLQName("http://example.com", "test", "other:test");

        // When: Getting hash codes
        final int hash1 = qname1.hashCode();
        final int hash2 = qname2.hashCode();

        // Then: Hash codes should likely be different (not guaranteed but probable)
        assertNotEquals(hash1, hash2, "Different rawnames should likely produce different hash codes");
    }

    @Test
    public void testToStringWithFullValues() {
        // Given: QName with all values
        final HTMLQName qname = new HTMLQName("http://www.w3.org/1999/xhtml", "div", "html:div");

        // When: Converting to string
        final String str = qname.toString();

        // Then: Should contain all parts
        assertTrue(str.contains("QName{"), "Should start with QName{");
        assertTrue(str.contains("uri=\"http://www.w3.org/1999/xhtml\""), "Should contain URI");
        assertTrue(str.contains("localpart=\"div\""), "Should contain local part");
        assertTrue(str.contains("rawname=\"html:div\""), "Should contain raw name");
        assertTrue(str.contains("}"), "Should end with }");
    }

    @Test
    public void testToStringWithNullUri() {
        // Given: QName without URI
        final HTMLQName qname = new HTMLQName("test");

        // When: Converting to string
        final String str = qname.toString();

        // Then: Should not contain URI but have other parts
        assertFalse(str.contains("uri="), "Should not contain URI field");
        assertTrue(str.contains("localpart=\"test\""), "Should contain local part");
        assertTrue(str.contains("rawname=\"test\""), "Should contain raw name");
    }

    @Test
    public void testToStringWithAllNulls() {
        // Given: QName with null values
        final HTMLQName qname = new HTMLQName();

        // When: Converting to string
        final String str = qname.toString();

        // Then: Should handle nulls gracefully
        assertTrue(str.contains("QName{"), "Should start with QName{");
        assertTrue(str.contains("rawname=\"null\""), "Should contain null rawname");
        assertTrue(str.contains("}"), "Should end with }");
    }

    @Test
    public void testToStringFormat() {
        // Given: QName with specific values
        final HTMLQName qname = new HTMLQName("http://example.com", "element", "ns:element");

        // When: Converting to string
        final String str = qname.toString();

        // Then: Should follow expected format
        assertEquals("QName{uri=\"http://example.com\",localpart=\"element\",rawname=\"ns:element\"}", str, "Should follow expected format");
    }

    @Test
    public void testPublicFieldAccess() {
        // Given: QName instance
        final HTMLQName qname = new HTMLQName();

        // When: Directly accessing fields
        qname.uri = "http://example.com";
        qname.localpart = "test";
        qname.rawname = "ns:test";

        // Then: Fields should be accessible and modifiable
        assertEquals("http://example.com", qname.uri, "URI should be accessible");
        assertEquals("test", qname.localpart, "Local part should be accessible");
        assertEquals("ns:test", qname.rawname, "Raw name should be accessible");
    }

    @Test
    public void testEmptyStrings() {
        // Given: QName with empty strings
        final HTMLQName qname = new HTMLQName("", "", "");

        // When: Checking values
        // Then: Should store empty strings (not null)
        assertEquals("", qname.uri, "URI should be empty string");
        assertEquals("", qname.localpart, "Local part should be empty string");
        assertEquals("", qname.rawname, "Raw name should be empty string");
    }

    @Test
    public void testEqualsWithEmptyStrings() {
        // Given: QNames with empty strings
        final HTMLQName qname1 = new HTMLQName("", "", "");
        final HTMLQName qname2 = new HTMLQName("", "", "");

        // When: Comparing
        final boolean result = qname1.equals(qname2);

        // Then: Should be equal
        assertTrue(result, "QNames with empty strings should be equal");
    }

    @Test
    public void testSetValuesOverwriting() {
        // Given: QName with initial values
        final HTMLQName qname = new HTMLQName("http://old.com", "old", "ns:old");

        // When: Setting new values multiple times
        qname.setValues("http://new1.com", "new1", "ns:new1");
        qname.setValues("http://new2.com", "new2", "ns:new2");

        // Then: Should have latest values
        assertEquals("http://new2.com", qname.uri, "Should have latest URI");
        assertEquals("new2", qname.localpart, "Should have latest local part");
        assertEquals("ns:new2", qname.rawname, "Should have latest raw name");
    }

    @Test
    public void testComplexRawname() {
        // Given: QName with complex raw name
        final HTMLQName qname = new HTMLQName("http://example.com", "element", "prefix1:prefix2:element");

        // When: Checking values
        // Then: Should handle complex raw names
        assertEquals("prefix1:prefix2:element", qname.rawname, "Should handle complex raw name with multiple colons");
    }

    @Test
    public void testUnicodeValues() {
        // Given: QName with Unicode values
        final HTMLQName qname = new HTMLQName("http://例.jp", "要素", "接頭辞:要素");

        // When: Checking values
        // Then: Should handle Unicode correctly
        assertEquals("http://例.jp", qname.uri, "Should handle Unicode in URI");
        assertEquals("要素", qname.localpart, "Should handle Unicode in local part");
        assertEquals("接頭辞:要素", qname.rawname, "Should handle Unicode in raw name");
    }

    @Test
    public void testLongValues() {
        // Given: QName with very long values
        final String longUri = "http://example.com/" + "a".repeat(1000);
        final String longLocal = "element" + "b".repeat(1000);
        final String longRaw = "prefix:" + "c".repeat(1000);

        final HTMLQName qname = new HTMLQName(longUri, longLocal, longRaw);

        // When: Checking values
        // Then: Should handle long values
        assertEquals(longUri, qname.uri, "Should handle long URI");
        assertEquals(longLocal, qname.localpart, "Should handle long local part");
        assertEquals(longRaw, qname.rawname, "Should handle long raw name");
    }

} // class HTMLQNameTest
