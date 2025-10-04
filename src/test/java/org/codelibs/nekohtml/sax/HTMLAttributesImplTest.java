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
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Test class for {@link HTMLAttributesImpl}.
 *
 * @author CodeLibs Project
 */
public class HTMLAttributesImplTest {

    private HTMLAttributesImpl attributes;

    @BeforeEach
    public void setUp() {
        attributes = new HTMLAttributesImpl();
    }

    @Test
    public void testDefaultConstructor() {
        // When: Creating with default constructor
        final HTMLAttributesImpl attrs = new HTMLAttributesImpl();

        // Then: Should be empty
        assertEquals(0, attrs.getLength(), "New attributes should be empty");
    }

    @Test
    public void testCopyConstructor() {
        // Given: Source attributes with some values
        final AttributesImpl source = new AttributesImpl();
        source.addAttribute("", "id", "id", "CDATA", "test123");
        source.addAttribute("", "class", "class", "CDATA", "container");

        // When: Creating with copy constructor
        final HTMLAttributesImpl attrs = new HTMLAttributesImpl(source);

        // Then: Should copy all attributes
        assertEquals(2, attrs.getLength(), "Should have 2 attributes");
        assertEquals("test123", attrs.getValue("id"), "Should copy id attribute");
        assertEquals("container", attrs.getValue("class"), "Should copy class attribute");
    }

    @Test
    public void testAddAttributeWithIndex() {
        // When: Adding attribute with full parameters
        final int index = attributes.addAttributeWithIndex("http://example.com", "local", "prefix:local", "CDATA", "value1");

        // Then: Should return correct index and add attribute
        assertEquals(0, index, "First attribute should have index 0");
        assertEquals(1, attributes.getLength(), "Should have 1 attribute");
        assertEquals("value1", attributes.getValue(index), "Should have correct value");
        assertEquals("prefix:local", attributes.getQName(index), "Should have correct QName");
        assertEquals("local", attributes.getLocalName(index), "Should have correct local name");
        assertEquals("http://example.com", attributes.getURI(index), "Should have correct URI");
    }

    @Test
    public void testAddMultipleAttributesWithIndex() {
        // When: Adding multiple attributes
        final int index1 = attributes.addAttributeWithIndex("", "attr1", "attr1", "CDATA", "value1");
        final int index2 = attributes.addAttributeWithIndex("", "attr2", "attr2", "CDATA", "value2");
        final int index3 = attributes.addAttributeWithIndex("", "attr3", "attr3", "CDATA", "value3");

        // Then: Should return sequential indices
        assertEquals(0, index1, "First attribute should have index 0");
        assertEquals(1, index2, "Second attribute should have index 1");
        assertEquals(2, index3, "Third attribute should have index 2");
        assertEquals(3, attributes.getLength(), "Should have 3 attributes");
    }

    @Test
    public void testAddAttributeWithHTMLQName() {
        // Given: HTMLQName object
        final HTMLQName qname = new HTMLQName();
        qname.setValues("http://www.w3.org/1999/xhtml", "div", "div");

        // When: Adding attribute with HTMLQName
        final int index = attributes.addAttribute(qname, "CDATA", "container");

        // Then: Should add attribute correctly
        assertEquals(0, index, "First attribute should have index 0");
        assertEquals(1, attributes.getLength(), "Should have 1 attribute");
        assertEquals("container", attributes.getValue(index), "Should have correct value");
        assertEquals("div", attributes.getQName(index), "Should have correct QName");
        assertEquals("div", attributes.getLocalName(index), "Should have correct local name");
        assertEquals("http://www.w3.org/1999/xhtml", attributes.getURI(index), "Should have correct URI");
    }

    @Test
    public void testRemoveAttributeAt() {
        // Given: Attributes with 3 entries
        attributes.addAttributeWithIndex("", "attr1", "attr1", "CDATA", "value1");
        attributes.addAttributeWithIndex("", "attr2", "attr2", "CDATA", "value2");
        attributes.addAttributeWithIndex("", "attr3", "attr3", "CDATA", "value3");

        // When: Removing middle attribute
        attributes.removeAttributeAt(1);

        // Then: Should have 2 attributes remaining
        assertEquals(2, attributes.getLength(), "Should have 2 attributes after removal");
        assertEquals("value1", attributes.getValue(0), "First attribute should remain");
        assertEquals("value3", attributes.getValue(1), "Third attribute should shift to index 1");
    }

    @Test
    public void testRemoveAttributeAtInvalidIndex() {
        // Given: Attributes with 2 entries
        attributes.addAttributeWithIndex("", "attr1", "attr1", "CDATA", "value1");
        attributes.addAttributeWithIndex("", "attr2", "attr2", "CDATA", "value2");

        // When: Trying to remove with invalid indices
        attributes.removeAttributeAt(-1);
        attributes.removeAttributeAt(5);

        // Then: Should not remove anything
        assertEquals(2, attributes.getLength(), "Should still have 2 attributes");
    }

    @Test
    public void testRemoveAllAttributes() {
        // Given: Attributes with several entries
        attributes.addAttributeWithIndex("", "attr1", "attr1", "CDATA", "value1");
        attributes.addAttributeWithIndex("", "attr2", "attr2", "CDATA", "value2");
        attributes.addAttributeWithIndex("", "attr3", "attr3", "CDATA", "value3");

        // When: Removing all attributes
        attributes.removeAllAttributes();

        // Then: Should be empty
        assertEquals(0, attributes.getLength(), "Should have no attributes");
    }

    @Test
    public void testSetNameWithParameters() {
        // Given: Attribute with initial values
        attributes.addAttributeWithIndex("http://old.com", "oldLocal", "old:oldLocal", "CDATA", "value1");

        // When: Setting new name
        attributes.setName(0, "http://new.com", "newLocal", "new:newLocal");

        // Then: Should update name information
        assertEquals("http://new.com", attributes.getURI(0), "Should have new URI");
        assertEquals("newLocal", attributes.getLocalName(0), "Should have new local name");
        assertEquals("new:newLocal", attributes.getQName(0), "Should have new QName");
        assertEquals("value1", attributes.getValue(0), "Should keep original value");
    }

    @Test
    public void testSetNameWithInvalidIndex() {
        // Given: Attribute with one entry
        attributes.addAttributeWithIndex("", "attr1", "attr1", "CDATA", "value1");

        // When: Setting name with invalid index
        attributes.setName(-1, "", "invalid", "invalid");
        attributes.setName(5, "", "invalid", "invalid");

        // Then: Should not affect existing attribute
        assertEquals("attr1", attributes.getQName(0), "Should keep original QName");
    }

    @Test
    public void testSetNameWithHTMLQName() {
        // Given: Attribute with initial values
        attributes.addAttributeWithIndex("", "oldAttr", "oldAttr", "CDATA", "value1");

        // When: Setting new name with HTMLQName
        final HTMLQName qname = new HTMLQName();
        qname.setValues("http://example.com", "newAttr", "newAttr");
        attributes.setName(0, qname);

        // Then: Should update name information
        assertEquals("http://example.com", attributes.getURI(0), "Should have new URI");
        assertEquals("newAttr", attributes.getLocalName(0), "Should have new local name");
        assertEquals("newAttr", attributes.getQName(0), "Should have new QName");
    }

    @Test
    public void testGetNonNormalizedValue() {
        // Given: Attribute with value
        attributes.addAttributeWithIndex("", "attr", "attr", "CDATA", "  value  ");

        // When: Getting non-normalized value
        final String value = attributes.getNonNormalizedValue(0);

        // Then: Should return same as getValue (SAX doesn't distinguish)
        assertEquals(attributes.getValue(0), value, "Non-normalized value should equal regular value");
        assertEquals("  value  ", value, "Should preserve whitespace");
    }

    @Test
    public void testSetNonNormalizedValue() {
        // Given: Attribute with initial value
        attributes.addAttributeWithIndex("", "attr", "attr", "CDATA", "old");

        // When: Setting non-normalized value
        attributes.setNonNormalizedValue(0, "  new value  ");

        // Then: Should update the value
        assertEquals("  new value  ", attributes.getValue(0), "Should update value");
        assertEquals("  new value  ", attributes.getNonNormalizedValue(0), "Non-normalized should match");
    }

    @Test
    public void testSetSpecified() {
        // Given: Attribute
        attributes.addAttributeWithIndex("", "attr", "attr", "CDATA", "value");

        // When: Setting specified flag (no-op in SAX)
        attributes.setSpecified(0, true);
        attributes.setSpecified(0, false);

        // Then: Should not throw exception (it's a no-op)
        assertEquals(1, attributes.getLength(), "Should still have attribute");
    }

    @Test
    public void testIsSpecified() {
        // Given: Attributes
        attributes.addAttributeWithIndex("", "attr1", "attr1", "CDATA", "value1");
        attributes.addAttributeWithIndex("", "attr2", "attr2", "CDATA", "value2");

        // When: Checking if specified
        final boolean specified0 = attributes.isSpecified(0);
        final boolean specified1 = attributes.isSpecified(1);
        final boolean specifiedInvalid = attributes.isSpecified(5);

        // Then: Valid indices should return true, invalid should return false
        assertTrue(specified0, "Valid index 0 should be specified");
        assertTrue(specified1, "Valid index 1 should be specified");
        assertFalse(specifiedInvalid, "Invalid index should not be specified");
    }

    @Test
    public void testIsSpecifiedWithNegativeIndex() {
        // Given: Attribute
        attributes.addAttributeWithIndex("", "attr", "attr", "CDATA", "value");

        // When: Checking with negative index
        final boolean specified = attributes.isSpecified(-1);

        // Then: Should return false
        assertFalse(specified, "Negative index should not be specified");
    }

    @Test
    public void testGetName() {
        // Given: Attribute with full name information
        attributes.addAttributeWithIndex("http://example.com", "local", "prefix:local", "CDATA", "value");

        // When: Getting name into HTMLQName
        final HTMLQName qname = new HTMLQName();
        attributes.getName(0, qname);

        // Then: Should populate QName correctly
        assertEquals("http://example.com", qname.uri, "Should have correct URI");
        assertEquals("local", qname.localpart, "Should have correct local part");
        assertEquals("prefix:local", qname.rawname, "Should have correct raw name");
    }

    @Test
    public void testGetNameWithInvalidIndex() {
        // Given: Attribute
        attributes.addAttributeWithIndex("", "attr", "attr", "CDATA", "value");

        // When: Getting name with invalid index
        final HTMLQName qname = new HTMLQName();
        qname.setValues("old", "old", "old");
        attributes.getName(5, qname);

        // Then: Should clear the QName
        assertNull(qname.uri, "URI should be null");
        assertNull(qname.localpart, "Local part should be null");
        assertNull(qname.rawname, "Raw name should be null");
    }

    @Test
    public void testGetNameWithNegativeIndex() {
        // Given: Attribute
        attributes.addAttributeWithIndex("", "attr", "attr", "CDATA", "value");

        // When: Getting name with negative index
        final HTMLQName qname = new HTMLQName();
        qname.setValues("old", "old", "old");
        attributes.getName(-1, qname);

        // Then: Should clear the QName
        assertNull(qname.uri, "URI should be null");
        assertNull(qname.localpart, "Local part should be null");
        assertNull(qname.rawname, "Raw name should be null");
    }

    @Test
    public void testInheritedAttributesImplMethods() {
        // Given: HTMLAttributesImpl instance
        attributes.addAttributeWithIndex("", "id", "id", "ID", "test123");
        attributes.addAttributeWithIndex("", "class", "class", "CDATA", "container");

        // When: Using inherited methods
        final String idValue = attributes.getValue("id");
        final String classValue = attributes.getValue("class");
        final int idIndex = attributes.getIndex("id");
        final String typeById = attributes.getType("id");

        // Then: Should work correctly
        assertEquals("test123", idValue, "Should get value by name");
        assertEquals("container", classValue, "Should get value by name");
        assertEquals(0, idIndex, "Should get index by name");
        assertEquals("ID", typeById, "Should get type by name");
    }

    @Test
    public void testSetValueInherited() {
        // Given: Attribute
        attributes.addAttributeWithIndex("", "attr", "attr", "CDATA", "old");

        // When: Setting value using inherited method
        attributes.setValue(0, "new");

        // Then: Should update value
        assertEquals("new", attributes.getValue(0), "Should have new value");
    }

    @Test
    public void testSetTypeInherited() {
        // Given: Attribute
        attributes.addAttributeWithIndex("", "attr", "attr", "CDATA", "value");

        // When: Setting type using inherited method
        attributes.setType(0, "ID");

        // Then: Should update type
        assertEquals("ID", attributes.getType(0), "Should have new type");
    }

    @Test
    public void testEmptyAttributes() {
        // When: Working with empty attributes
        final int length = attributes.getLength();
        final String value = attributes.getValue(0);

        // Then: Should handle empty state
        assertEquals(0, length, "Empty attributes should have length 0");
        assertNull(value, "Getting value from empty attributes should return null");
    }

    @Test
    public void testMultipleOperations() {
        // Given: Series of operations
        // Add attributes
        attributes.addAttributeWithIndex("", "attr1", "attr1", "CDATA", "value1");
        attributes.addAttributeWithIndex("", "attr2", "attr2", "CDATA", "value2");
        attributes.addAttributeWithIndex("", "attr3", "attr3", "CDATA", "value3");

        // Modify
        attributes.setValue(1, "modified");
        attributes.setName(2, "", "renamed", "renamed");

        // Remove one
        attributes.removeAttributeAt(0);

        // Then: Should have correct final state
        assertEquals(2, attributes.getLength(), "Should have 2 attributes");
        assertEquals("modified", attributes.getValue(0), "First attribute should be modified attr2");
        assertEquals("renamed", attributes.getQName(1), "Second attribute should be renamed attr3");
    }

    @Test
    public void testAttributeTypesPreservation() {
        // Given: Attributes with different types
        attributes.addAttributeWithIndex("", "id", "id", "ID", "test123");
        attributes.addAttributeWithIndex("", "class", "class", "CDATA", "container");
        attributes.addAttributeWithIndex("", "ref", "ref", "IDREF", "link");

        // When: Retrieving types
        final String type1 = attributes.getType(0);
        final String type2 = attributes.getType(1);
        final String type3 = attributes.getType(2);

        // Then: Should preserve different types
        assertEquals("ID", type1, "Should preserve ID type");
        assertEquals("CDATA", type2, "Should preserve CDATA type");
        assertEquals("IDREF", type3, "Should preserve IDREF type");
    }

    @Test
    public void testNamespaceHandling() {
        // Given: Attributes with namespaces
        attributes.addAttributeWithIndex("http://www.w3.org/1999/xhtml", "div", "html:div", "CDATA", "container");
        attributes.addAttributeWithIndex("http://www.w3.org/2000/svg", "circle", "svg:circle", "CDATA", "shape");

        // When: Retrieving namespace information
        final String uri1 = attributes.getURI(0);
        final String uri2 = attributes.getURI(1);
        final String qname1 = attributes.getQName(0);
        final String qname2 = attributes.getQName(1);

        // Then: Should handle namespaces correctly
        assertEquals("http://www.w3.org/1999/xhtml", uri1, "Should have XHTML namespace");
        assertEquals("http://www.w3.org/2000/svg", uri2, "Should have SVG namespace");
        assertEquals("html:div", qname1, "Should have prefixed QName");
        assertEquals("svg:circle", qname2, "Should have prefixed QName");
    }

    @Test
    public void testEmptyStringValues() {
        // Given: Attributes with empty string values
        attributes.addAttributeWithIndex("", "", "", "CDATA", "");

        // When: Retrieving values
        final String uri = attributes.getURI(0);
        final String local = attributes.getLocalName(0);
        final String qname = attributes.getQName(0);
        final String value = attributes.getValue(0);

        // Then: Should handle empty strings
        assertEquals("", uri, "URI should be empty string");
        assertEquals("", local, "Local name should be empty string");
        assertEquals("", qname, "QName should be empty string");
        assertEquals("", value, "Value should be empty string");
    }

    @Test
    public void testLargeNumberOfAttributes() {
        // Given: Many attributes
        for (int i = 0; i < 100; i++) {
            attributes.addAttributeWithIndex("", "attr" + i, "attr" + i, "CDATA", "value" + i);
        }

        // When: Checking length and accessing
        final int length = attributes.getLength();
        final String firstValue = attributes.getValue(0);
        final String lastValue = attributes.getValue(99);

        // Then: Should handle many attributes
        assertEquals(100, length, "Should have 100 attributes");
        assertEquals("value0", firstValue, "Should access first attribute");
        assertEquals("value99", lastValue, "Should access last attribute");
    }

    @Test
    public void testCopyConstructorIndependence() {
        // Given: Original attributes
        final AttributesImpl original = new AttributesImpl();
        original.addAttribute("", "attr1", "attr1", "CDATA", "value1");

        final HTMLAttributesImpl copy = new HTMLAttributesImpl(original);

        // When: Modifying copy
        copy.setValue(0, "modified");
        copy.addAttributeWithIndex("", "attr2", "attr2", "CDATA", "value2");

        // Then: Original should be unchanged
        assertEquals("value1", original.getValue(0), "Original should be unchanged");
        assertEquals(1, original.getLength(), "Original should have 1 attribute");
        assertEquals("modified", copy.getValue(0), "Copy should be modified");
        assertEquals(2, copy.getLength(), "Copy should have 2 attributes");
    }

} // class HTMLAttributesImplTest
