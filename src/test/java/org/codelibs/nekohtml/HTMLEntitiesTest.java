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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * Test class for {@link HTMLEntities}.
 *
 * @author CodeLibs Project
 */
public class HTMLEntitiesTest {

    /**
     * Test that HTMLEntities can be instantiated.
     */
    @Test
    public void testInstantiation() {
        final HTMLEntities entities = new HTMLEntities();
        assertNotNull(entities, "HTMLEntities should be instantiated");
    }

    /**
     * Test getting character value for common Latin-1 entities.
     */
    @Test
    public void testGetLatin1Entities() {
        // Test common Latin-1 entities
        assertEquals(160, HTMLEntities.get("nbsp"), "nbsp should map to character 160 (non-breaking space)");
        assertEquals(169, HTMLEntities.get("copy"), "copy should map to character 169 (copyright symbol)");
        assertEquals(174, HTMLEntities.get("reg"), "reg should map to character 174 (registered trademark)");
        assertEquals(176, HTMLEntities.get("deg"), "deg should map to character 176 (degree symbol)");
        assertEquals(177, HTMLEntities.get("plusmn"), "plusmn should map to character 177 (plus-minus)");
    }

    /**
     * Test getting character value for special HTML entities.
     */
    @Test
    public void testGetSpecialEntities() {
        // Test special HTML entities
        assertEquals(338, HTMLEntities.get("OElig"), "OElig should map to character 338");
        assertEquals(339, HTMLEntities.get("oelig"), "oelig should map to character 339");
        assertEquals(352, HTMLEntities.get("Scaron"), "Scaron should map to character 352");
        assertEquals(376, HTMLEntities.get("Yuml"), "Yuml should map to character 376");
    }

    /**
     * Test getting character value for symbol entities.
     */
    @Test
    public void testGetSymbolEntities() {
        // Test mathematical and technical symbols
        assertEquals(8704, HTMLEntities.get("forall"), "forall should map to character 8704");
        assertEquals(8706, HTMLEntities.get("part"), "part should map to character 8706");
        assertEquals(8707, HTMLEntities.get("exist"), "exist should map to character 8707");
        assertEquals(8709, HTMLEntities.get("empty"), "empty should map to character 8709");
        assertEquals(8721, HTMLEntities.get("sum"), "sum should map to character 8721");
        assertEquals(8734, HTMLEntities.get("infin"), "infin should map to character 8734 (infinity)");
    }

    /**
     * Test getting character value for XML built-in entities.
     */
    @Test
    public void testGetXMLBuiltinEntities() {
        // Test XML built-in entities
        assertEquals(60, HTMLEntities.get("lt"), "lt should map to character 60 (<)");
        assertEquals(62, HTMLEntities.get("gt"), "gt should map to character 62 (>)");
        assertEquals(38, HTMLEntities.get("amp"), "amp should map to character 38 (&)");
        assertEquals(34, HTMLEntities.get("quot"), "quot should map to character 34 (\")");
        assertEquals(39, HTMLEntities.get("apos"), "apos should map to character 39 (')");
    }

    /**
     * Test getting character value for non-existent entity.
     */
    @Test
    public void testGetNonExistentEntity() {
        assertEquals(-1, HTMLEntities.get("nonexistent"), "Non-existent entity should return -1");
        assertEquals(-1, HTMLEntities.get("foobar"), "Unknown entity should return -1");
        assertEquals(-1, HTMLEntities.get(""), "Empty entity name should return -1");
    }

    /**
     * Test reverse lookup: getting entity name from character value.
     */
    @Test
    public void testGetEntityNameFromChar() {
        // Test reverse lookup for common characters
        assertEquals("nbsp", HTMLEntities.get(160), "Character 160 should map to 'nbsp'");
        assertEquals("copy", HTMLEntities.get(169), "Character 169 should map to 'copy'");
        assertEquals("reg", HTMLEntities.get(174), "Character 174 should map to 'reg'");
        assertEquals("lt", HTMLEntities.get(60), "Character 60 should map to 'lt'");
        assertEquals("gt", HTMLEntities.get(62), "Character 62 should map to 'gt'");
        assertEquals("amp", HTMLEntities.get(38), "Character 38 should map to 'amp'");
        assertEquals("quot", HTMLEntities.get(34), "Character 34 should map to 'quot'");
    }

    /**
     * Test reverse lookup for non-existent character mapping.
     */
    @Test
    public void testGetEntityNameFromNonExistentChar() {
        assertNull(HTMLEntities.get(0), "Character 0 should have no entity mapping");
        assertNull(HTMLEntities.get(1), "Character 1 should have no entity mapping");
        assertNull(HTMLEntities.get(9999), "Character 9999 should have no entity mapping");
        // Note: Negative values cause ArrayIndexOutOfBoundsException in IntProperties
        // This is expected behavior as character values should be non-negative
    }

    /**
     * Test bidirectional mapping consistency.
     */
    @Test
    public void testBidirectionalMapping() {
        // Test that entity -> char -> entity produces the same result
        final String entityName = "nbsp";
        final int charValue = HTMLEntities.get(entityName);
        final String reversedName = HTMLEntities.get(charValue);

        assertEquals(160, charValue, "nbsp should map to 160");
        assertEquals(entityName, reversedName, "Character 160 should map back to 'nbsp'");
    }

    /**
     * Test multiple common entities for consistency.
     */
    @Test
    public void testMultipleEntitiesConsistency() {
        final String[] entities = { "nbsp", "copy", "reg", "lt", "gt", "amp", "quot", "apos" };

        for (final String entity : entities) {
            final int charValue = HTMLEntities.get(entity);
            assertTrue(charValue > 0, "Entity '" + entity + "' should have a positive character value");

            final String reversedEntity = HTMLEntities.get(charValue);
            assertNotNull(reversedEntity, "Character " + charValue + " should have an entity name");
        }
    }

    /**
     * Test Greek letter entities.
     */
    @Test
    public void testGreekLetterEntities() {
        assertEquals(913, HTMLEntities.get("Alpha"), "Alpha should map to character 913");
        assertEquals(914, HTMLEntities.get("Beta"), "Beta should map to character 914");
        assertEquals(915, HTMLEntities.get("Gamma"), "Gamma should map to character 915");
        assertEquals(916, HTMLEntities.get("Delta"), "Delta should map to character 916");
        assertEquals(945, HTMLEntities.get("alpha"), "alpha should map to character 945");
        assertEquals(946, HTMLEntities.get("beta"), "beta should map to character 946");
        assertEquals(947, HTMLEntities.get("gamma"), "gamma should map to character 947");
    }

    /**
     * Test arrow entities.
     */
    @Test
    public void testArrowEntities() {
        assertEquals(8592, HTMLEntities.get("larr"), "larr should map to character 8592 (left arrow)");
        assertEquals(8593, HTMLEntities.get("uarr"), "uarr should map to character 8593 (up arrow)");
        assertEquals(8594, HTMLEntities.get("rarr"), "rarr should map to character 8594 (right arrow)");
        assertEquals(8595, HTMLEntities.get("darr"), "darr should map to character 8595 (down arrow)");
    }

    /**
     * Test currency entities.
     */
    @Test
    public void testCurrencyEntities() {
        assertEquals(8364, HTMLEntities.get("euro"), "euro should map to character 8364 (€)");
        assertEquals(163, HTMLEntities.get("pound"), "pound should map to character 163 (£)");
        assertEquals(165, HTMLEntities.get("yen"), "yen should map to character 165 (¥)");
    }

    /**
     * Test case sensitivity of entity names.
     */
    @Test
    public void testCaseSensitivity() {
        // Entity names are case-sensitive
        assertEquals(913, HTMLEntities.get("Alpha"), "Alpha (uppercase) should map to character 913");
        assertEquals(945, HTMLEntities.get("alpha"), "alpha (lowercase) should map to character 945");

        // Non-existent case variant should return -1
        assertEquals(-1, HTMLEntities.get("ALPHA"), "ALPHA (all caps) should not exist");
        assertEquals(-1, HTMLEntities.get("NBSP"), "NBSP (all caps) should not exist");
    }

    /**
     * Test IntProperties hash collision handling.
     * This tests the internal IntProperties class's ability to handle hash collisions.
     */
    @Test
    public void testIntPropertiesHashCollisions() {
        // Create multiple entities and ensure they can all be retrieved
        // The IntProperties uses modulo 101, so we can test collision handling

        // Get several entities that should work correctly
        final int nbsp = HTMLEntities.get("nbsp");
        final int copy = HTMLEntities.get("copy");
        final int reg = HTMLEntities.get("reg");

        // Verify reverse lookups work correctly
        assertEquals("nbsp", HTMLEntities.get(nbsp));
        assertEquals("copy", HTMLEntities.get(copy));
        assertEquals("reg", HTMLEntities.get(reg));

        // Test multiple lookups to ensure hash table integrity
        for (int i = 0; i < 10; i++) {
            assertEquals("nbsp", HTMLEntities.get(160), "Multiple lookups should return consistent results");
            assertEquals("copy", HTMLEntities.get(169), "Multiple lookups should return consistent results");
        }
    }

    /**
     * Test boundary values for character codes.
     */
    @Test
    public void testBoundaryValues() {
        // Test very low character values
        final String lowEntity = HTMLEntities.get(32); // space
        // Space might not have an entity, so we just ensure it doesn't crash

        // Test high character values
        final String highEntity = HTMLEntities.get(9999);
        assertNull(highEntity, "Very high character value should have no entity");

        // Note: Negative values would cause ArrayIndexOutOfBoundsException in IntProperties
        // This is expected behavior as Unicode character values are always non-negative
        // The implementation doesn't need to handle invalid negative character codes
    }

    /**
     * Test that null entity name is handled gracefully.
     */
    @Test
    public void testNullEntityName() {
        final int result = HTMLEntities.get((String) null);
        assertEquals(-1, result, "Null entity name should return -1");
    }

    /**
     * Test common punctuation entities.
     */
    @Test
    public void testPunctuationEntities() {
        assertEquals(8211, HTMLEntities.get("ndash"), "ndash should map to character 8211 (en dash)");
        assertEquals(8212, HTMLEntities.get("mdash"), "mdash should map to character 8212 (em dash)");
        assertEquals(8216, HTMLEntities.get("lsquo"), "lsquo should map to character 8216 (left single quote)");
        assertEquals(8217, HTMLEntities.get("rsquo"), "rsquo should map to character 8217 (right single quote)");
        assertEquals(8220, HTMLEntities.get("ldquo"), "ldquo should map to character 8220 (left double quote)");
        assertEquals(8221, HTMLEntities.get("rdquo"), "rdquo should map to character 8221 (right double quote)");
    }

    /**
     * Test accented character entities.
     */
    @Test
    public void testAccentedCharacterEntities() {
        assertEquals(192, HTMLEntities.get("Agrave"), "Agrave should map to character 192 (À)");
        assertEquals(193, HTMLEntities.get("Aacute"), "Aacute should map to character 193 (Á)");
        assertEquals(194, HTMLEntities.get("Acirc"), "Acirc should map to character 194 (Â)");
        assertEquals(224, HTMLEntities.get("agrave"), "agrave should map to character 224 (à)");
        assertEquals(225, HTMLEntities.get("aacute"), "aacute should map to character 225 (á)");
        assertEquals(226, HTMLEntities.get("acirc"), "acirc should map to character 226 (â)");
    }

    /**
     * Test that {@code load0} handles a missing resource gracefully (no NPE), instead of
     * letting {@code Properties.load(null)} throw a {@link NullPointerException}.
     */
    @Test
    public void testLoad0MissingResourceDoesNotThrow() {
        final Properties props = new Properties();
        assertDoesNotThrow(() -> HTMLEntities.load0(props, "res/DoesNotExist.properties"),
                "A missing resource should be logged and skipped, not thrown as an NPE");
        assertTrue(props.isEmpty(), "Properties should be left unchanged when the resource is missing");
    }

} // class HTMLEntitiesTest
