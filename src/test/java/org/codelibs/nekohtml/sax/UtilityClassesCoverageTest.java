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
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;

/**
 * Coverage tests for utility classes: HTMLAttributesImpl, HTMLAugmentations,
 * HTMLStringBuffer, HTMLQName, and HTMLSAXScanner.
 *
 * @author CodeLibs Project
 */
public class UtilityClassesCoverageTest {

    // ========================================================================
    // HTMLAttributesImpl tests
    // ========================================================================

    @Test
    public void testRemoveAttributeAtBeyondLength() {
        final HTMLAttributesImpl attrs = new HTMLAttributesImpl();
        attrs.addAttributeWithIndex("", "a", "a", "CDATA", "1");
        assertEquals(1, attrs.getLength());

        // index == getLength() should be a no-op
        attrs.removeAttributeAt(1);
        assertEquals(1, attrs.getLength());

        // index > getLength() should also be a no-op
        attrs.removeAttributeAt(5);
        assertEquals(1, attrs.getLength());
    }

    @Test
    public void testSetNameWithNegativeIndex() {
        final HTMLAttributesImpl attrs = new HTMLAttributesImpl();
        attrs.addAttributeWithIndex("", "a", "a", "CDATA", "1");

        // Negative index should be a no-op, not throw
        attrs.setName(-1, "http://ns", "b", "b");
        // Original attribute should be unchanged
        assertEquals("a", attrs.getLocalName(0));
        assertEquals("a", attrs.getQName(0));
    }

    @Test
    public void testIsSpecifiedWithEmptyAttributes() {
        final HTMLAttributesImpl attrs = new HTMLAttributesImpl();
        assertEquals(0, attrs.getLength());

        // index 0 on empty list should return false
        assertFalse(attrs.isSpecified(0));
    }

    @Test
    public void testGetNameAtExactBoundary() {
        final HTMLAttributesImpl attrs = new HTMLAttributesImpl();
        attrs.addAttributeWithIndex("http://ns", "attr", "ns:attr", "CDATA", "val");
        assertEquals(1, attrs.getLength());

        // index == getLength() should clear the qname
        final HTMLQName qname = new HTMLQName("initial");
        attrs.getName(1, qname);
        assertNull(qname.rawname);
        assertNull(qname.localpart);
        assertNull(qname.uri);

        // index within bounds should populate the qname
        final HTMLQName qname2 = new HTMLQName();
        attrs.getName(0, qname2);
        assertEquals("http://ns", qname2.uri);
        assertEquals("attr", qname2.localpart);
        assertEquals("ns:attr", qname2.rawname);
    }

    // ========================================================================
    // HTMLAugmentations tests
    // ========================================================================

    @Test
    public void testRemoveItemWhenDataIsNull() {
        final HTMLAugmentations aug = new HTMLAugmentations();
        // data is null (never initialized), removeItem should return null
        assertNull(aug.removeItem("nonexistent"));
    }

    @Test
    public void testRemoveAllItemsWhenDataIsNull() {
        final HTMLAugmentations aug = new HTMLAugmentations();
        // Should not throw when data is null
        assertDoesNotThrow(aug::removeAllItems);
    }

    @Test
    public void testPutItemLazyInitialization() {
        final HTMLAugmentations aug = new HTMLAugmentations();
        // data is null initially; first putItem should lazily create the map
        assertNull(aug.getItem("key"));

        final Object prev = aug.putItem("key", "value");
        assertNull(prev); // No previous value
        assertEquals("value", aug.getItem("key"));

        // Second putItem with same key returns previous value
        final Object prev2 = aug.putItem("key", "newValue");
        assertEquals("value", prev2);
        assertEquals("newValue", aug.getItem("key"));
    }

    // ========================================================================
    // HTMLStringBuffer tests
    // ========================================================================

    @Test
    public void testEnsureCapacityWithNonZeroOffset() {
        // Build a buffer, then manipulate offset to be non-zero
        final HTMLStringBuffer buf = new HTMLStringBuffer(4);
        buf.append("AB");
        // Manually shift offset to simulate non-zero offset scenario
        buf.offset = 2;
        buf.length = 0;
        buf.ch[2] = 'C';
        buf.ch[3] = 'D';
        buf.length = 2;

        // Now appending should trigger ensureCapacity with offset != 0
        // since offset(2) + newLength > ch.length(4)
        buf.append("EFG");
        assertEquals("CDEFG", buf.toString());
        // After expansion, offset should be reset to 0
        assertEquals(0, buf.offset);
    }

    @Test
    public void testToSAXCharactersReturnsCorrectTriple() {
        final HTMLStringBuffer buf = new HTMLStringBuffer("Hello");

        final Object[] result = buf.toSAXCharacters();
        assertEquals(3, result.length);
        assertSame(buf.ch, result[0]);
        assertEquals(buf.offset, result[1]);
        assertEquals(buf.length, result[2]);
        assertEquals(5, (int) result[2]);
    }

    @Test
    public void testAppendSingleCharTriggeringExpansion() {
        // Create a buffer that is exactly full
        final HTMLStringBuffer buf = new HTMLStringBuffer(2);
        buf.append('A');
        buf.append('B');
        assertEquals(2, buf.length);

        // This append should trigger expansion
        buf.append('C');
        assertEquals("ABC", buf.toString());
        assertEquals(3, buf.length);
        assertTrue(buf.ch.length >= 3);
    }

    // ========================================================================
    // HTMLQName tests
    // ========================================================================

    @Test
    public void testEqualsWithDifferentLocalpartButSameRawname() {
        // equals only checks rawname and uri, not localpart
        final HTMLQName q1 = new HTMLQName("http://ns", "local1", "raw");
        final HTMLQName q2 = new HTMLQName("http://ns", "local2", "raw");

        // Should be equal because rawname and uri match
        assertEquals(q1, q2);
    }

    @Test
    public void testHashCodeConsistency() {
        final HTMLQName q1 = new HTMLQName("http://ns", "local", "raw");
        final HTMLQName q2 = new HTMLQName("http://ns", "local", "raw");

        assertEquals(q1.hashCode(), q2.hashCode());

        // Multiple calls return the same value
        final int h = q1.hashCode();
        assertEquals(h, q1.hashCode());
        assertEquals(h, q1.hashCode());
    }

    @Test
    public void testHashCodeWithNullFields() {
        final HTMLQName q1 = new HTMLQName();
        // Should not throw with null rawname and null uri
        final int h = q1.hashCode();
        assertEquals(h, q1.hashCode());

        // Two null qnames should have same hashCode
        final HTMLQName q2 = new HTMLQName();
        assertEquals(q1.hashCode(), q2.hashCode());
        assertEquals(q1, q2);
    }

    @Test
    public void testEqualsWithNullRawname() {
        final HTMLQName q1 = new HTMLQName();
        q1.rawname = null;
        q1.uri = "http://ns";

        final HTMLQName q2 = new HTMLQName();
        q2.rawname = "raw";
        q2.uri = "http://ns";

        // q1.rawname is null, q2.rawname is not -> not equal
        assertNotEquals(q1, q2);
    }

    @Test
    public void testEqualsWithNullUri() {
        final HTMLQName q1 = new HTMLQName(null, "local", "raw");
        final HTMLQName q2 = new HTMLQName("http://ns", "local", "raw");

        // q1.uri is null, q2.uri is not -> not equal
        assertNotEquals(q1, q2);
    }

    // ========================================================================
    // HTMLSAXScanner tests
    // ========================================================================

    @Test
    public void testGetPropertyForUnrecognizedProperty() {
        final HTMLSAXScanner scanner = new HTMLSAXScanner();

        // Unrecognized property should throw SAXNotRecognizedException
        assertThrows(SAXNotRecognizedException.class, () -> scanner.getProperty("http://example.com/unknown-property"));
    }

    @Test
    public void testGetFeatureReturnValue() throws Exception {
        final HTMLSAXScanner scanner = new HTMLSAXScanner();

        // The standard SAX2 "namespaces" feature name is recognized (symmetric with setFeature)
        // and reported as false, since namespace processing is not implemented.
        assertFalse(scanner.getFeature("http://xml.org/sax/features/namespaces"));

        // A genuinely unknown feature name still throws.
        assertThrows(SAXNotRecognizedException.class, () -> scanner.getFeature("http://example.com/unknown-feature"));
    }

    @Test
    public void testGetPropertyForLexicalHandler() throws Exception {
        final HTMLSAXScanner scanner = new HTMLSAXScanner();

        // Without setting a lexical handler, should return null
        assertNull(scanner.getProperty("http://xml.org/sax/properties/lexical-handler"));
    }

    @Test
    public void testSetDocumentLocator() {
        final HTMLSAXScanner scanner = new HTMLSAXScanner();

        // setDocumentLocator is inherited from XMLFilterImpl; should not throw
        final Locator locator = new Locator() {
            @Override
            public String getPublicId() {
                return null;
            }

            @Override
            public String getSystemId() {
                return "test.html";
            }

            @Override
            public int getLineNumber() {
                return 1;
            }

            @Override
            public int getColumnNumber() {
                return 1;
            }
        };

        assertDoesNotThrow(() -> scanner.setDocumentLocator(locator));
    }

    @Test
    public void testProcessingInstruction() {
        final HTMLSAXScanner scanner = new HTMLSAXScanner();

        // processingInstruction is inherited from XMLFilterImpl
        // Without a parent set, XMLFilterImpl silently does nothing in JDK 21+
        assertDoesNotThrow(() -> scanner.processingInstruction("target", "data"));
    }

    @Test
    public void testSkippedEntity() {
        final HTMLSAXScanner scanner = new HTMLSAXScanner();

        // skippedEntity is inherited from XMLFilterImpl
        // Without a parent set, XMLFilterImpl silently does nothing in JDK 21+
        assertDoesNotThrow(() -> scanner.skippedEntity("entity"));
    }

}
