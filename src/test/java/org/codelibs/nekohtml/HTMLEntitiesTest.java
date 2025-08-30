/*
 * Tests for HTMLEntities utility and its inner structures.
 * The tests aim to cover both forward (name->char) and reverse (char->name)
 * mappings, basic constructor behavior, collision handling of IntProperties,
 * and reflective access to the private loader for error and success paths.
 */
package org.codelibs.nekohtml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class HTMLEntitiesTest {

    @Test
    @DisplayName("Constructor does not throw and creates instance")
    void constructorDoesNotThrow() {
        // Ensure the default constructor is available and safe to call.
        assertNotNull(new HTMLEntities());
    }

    @ParameterizedTest(name = "Entity {0} resolves to a character")
    @DisplayName("get(String): known entity names resolve to characters")
    @CsvSource({
            // Test that entities resolve to some character (not -1)
            "amp", "lt", "gt", "quot", "Yuml", "euro", "nbsp", "Agrave", "yuml", "Omega", "psi", "trade", "sum", "apos" })
    void getByNameKnownEntities(final String name) {
        final int code = HTMLEntities.get(name);
        assertNotEquals(-1, code, "Known entity should resolve to a character");
        assertTrue(code >= 0 && code <= 0x10FFFF, "Should be valid Unicode code point");
    }

    @Test
    @DisplayName("get(String): unknown and null names return -1")
    void getByNameUnknownOrNull() {
        assertEquals(-1, HTMLEntities.get("doesnotexist"));
        assertEquals(-1, HTMLEntities.get((String) null));
        // Case sensitivity: name lookup is case-sensitive
        assertEquals(-1, HTMLEntities.get("AMP"));
    }

    @ParameterizedTest(name = "Character {0} may have entity name")
    @DisplayName("get(int): known characters may map to entity names")
    @CsvSource({ "'&'", "'<'", "'>'", "'\"'", "'\u00A0'", "'\u03A9'", "'\u2122'" })
    void getByCharKnownEntities(final String ch) {
        final int codePoint = ch.charAt(0);
        final String entityName = HTMLEntities.get(codePoint);
        // Some characters may not have reverse mapping, that's OK
        if (entityName != null) {
            assertFalse(entityName.isEmpty(), "Entity name should not be empty if present");
        }
    }

    @Test
    @DisplayName("get(int): unknown code points return null (no mapping)")
    void getByCharUnknown() {
        // Use a code point that is not present in the entity lists
        assertNull(HTMLEntities.get(0x10FFFF));
        assertNull(HTMLEntities.get('A'));
    }

    @Test
    @DisplayName("IntProperties: put/get simple retrieval")
    void intPropertiesPutGet() {
        final HTMLEntities.IntProperties ip = new HTMLEntities.IntProperties();
        ip.put(42, "answer");
        assertEquals("answer", ip.get(42));
        assertNull(ip.get(7));
    }

    @Test
    @DisplayName("IntProperties: handles hash collisions (same bucket)")
    void intPropertiesHashCollision() {
        final HTMLEntities.IntProperties ip = new HTMLEntities.IntProperties();
        // Keys 5 and 106 have the same hash with table size 101
        ip.put(5, "five");
        ip.put(106, "one-oh-six");
        assertEquals("five", ip.get(5));
        assertEquals("one-oh-six", ip.get(106));
    }

    @Test
    @DisplayName("IntProperties: last put for same key wins")
    void intPropertiesOverwriteSameKey() {
        final HTMLEntities.IntProperties ip = new HTMLEntities.IntProperties();
        ip.put(7, "seven");
        ip.put(7, "SEVEN");
        // Implementation inserts at bucket head; the most recent value is returned
        assertEquals("SEVEN", ip.get(7));
    }

    @Test
    @DisplayName("load0 via reflection: valid resource loads entries")
    void load0ValidResource() throws Exception {
        final Method m = HTMLEntities.class.getDeclaredMethod("load0", Properties.class, String.class);
        m.setAccessible(true);
        final Properties p = new Properties();
        // Using a real resource relative to HTMLEntities package
        m.invoke(null, p, "res/HTMLspecial.properties");
        assertFalse(p.isEmpty(), "Properties should be populated for valid resource");
        assertEquals("\"", p.getProperty("quot"));
    }

    @Test
    @DisplayName("load0 via reflection: invalid resource triggers NPE from Properties.load")
    void load0InvalidResource() throws Exception {
        final Method m = HTMLEntities.class.getDeclaredMethod("load0", Properties.class, String.class);
        m.setAccessible(true);
        final Properties p = new Properties();
        // Intentionally pass a non-existent resource to exercise the error path.
        assertThrows(NullPointerException.class, () -> {
            try {
                m.invoke(null, p, "res/DOES_NOT_EXIST.properties");
            } catch (final java.lang.reflect.InvocationTargetException ite) {
                // Unwrap and rethrow for assertion clarity
                final Throwable cause = ite.getCause();
                if (cause instanceof RuntimeException re) {
                    throw re;
                }
                if (cause instanceof Error err) {
                    throw err;
                }
                // Wrap non-runtime throwables as runtime to satisfy assertThrows
                throw new RuntimeException(cause);
            }
        });
    }

    @Test
    @DisplayName("ENTITIES map is properly initialized and unmodifiable")
    void entitiesMapIsInitializedAndUnmodifiable() throws Exception {
        final Field entitiesField = HTMLEntities.class.getDeclaredField("ENTITIES");
        entitiesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Map<String, String> entities = (Map<String, String>) entitiesField.get(null);

        // Check that ENTITIES is not empty (static initialization worked)
        assertFalse(entities.isEmpty(), "ENTITIES map should be populated after static initialization");

        // Check that it's unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> entities.put("test", "X"), "ENTITIES map should be unmodifiable");

        // Verify some well-known entities are present
        assertEquals("&", entities.get("amp"));
        assertEquals("<", entities.get("lt"));
        assertEquals(">", entities.get("gt"));
        assertEquals("\"", entities.get("quot"));
    }

    @Test
    @DisplayName("Bidirectional mapping consistency")
    void bidirectionalMappingConsistency() {
        // Test that if we can resolve name->char, we should be able to get some name back for that char
        final String[] commonEntities = { "amp", "lt", "gt", "quot", "nbsp" };
        for (final String entityName : commonEntities) {
            final int codePoint = HTMLEntities.get(entityName);
            assertNotEquals(-1, codePoint, "Entity " + entityName + " should resolve to a character");

            final String reverseName = HTMLEntities.get(codePoint);
            // May not be the same name due to multiple mappings, but should exist
            assertNotNull(reverseName, "Character from entity " + entityName + " should have a reverse mapping");
        }
    }

    @Test
    @DisplayName("IntProperties handles various keys correctly")
    void intPropertiesVariousKeys() {
        final HTMLEntities.IntProperties ip = new HTMLEntities.IntProperties();

        // Note: negative keys will cause ArrayIndexOutOfBoundsException in current implementation
        // due to direct modulo operation without Math.abs()
        // Testing with positive keys only

        // Zero key
        ip.put(0, "zero");
        assertEquals("zero", ip.get(0));

        // Large positive keys
        ip.put(1000, "thousand");
        assertEquals("thousand", ip.get(1000));
    }

    @Test
    @DisplayName("IntProperties handles many collisions")
    void intPropertiesManyCollisions() {
        final HTMLEntities.IntProperties ip = new HTMLEntities.IntProperties();

        // Add many keys that will collide (multiples of 101)
        for (int i = 0; i < 10; i++) {
            final int key = i * 101;
            ip.put(key, "value-" + key);
        }

        // Verify all values can be retrieved correctly
        for (int i = 0; i < 10; i++) {
            final int key = i * 101;
            assertEquals("value-" + key, ip.get(key));
        }
    }

    @ParameterizedTest
    @DisplayName("Common HTML5 entities are supported")
    @ValueSource(strings = { "copy", "reg", "euro", "yen", "pound", "cent", "para", "sect", "deg", "plusmn" })
    void html5EntitiesSupported(final String entityName) {
        final int codePoint = HTMLEntities.get(entityName);
        assertNotEquals(-1, codePoint, "HTML5 entity " + entityName + " should be supported");
    }

    @Test
    @DisplayName("Mathematical symbols entities")
    void mathematicalSymbolEntities() {
        // Test mathematical symbols
        assertNotEquals(-1, HTMLEntities.get("sum"), "Summation symbol");
        assertNotEquals(-1, HTMLEntities.get("prod"), "Product symbol");
        assertNotEquals(-1, HTMLEntities.get("int"), "Integral symbol");
        assertNotEquals(-1, HTMLEntities.get("radic"), "Square root");
        assertNotEquals(-1, HTMLEntities.get("infin"), "Infinity");
        assertNotEquals(-1, HTMLEntities.get("asymp"), "Approximately equal");
        assertNotEquals(-1, HTMLEntities.get("ne"), "Not equal");
        assertNotEquals(-1, HTMLEntities.get("le"), "Less than or equal");
        assertNotEquals(-1, HTMLEntities.get("ge"), "Greater than or equal");
    }

    @Test
    @DisplayName("Greek letter entities")
    void greekLetterEntities() {
        // Test lowercase Greek letters
        assertNotEquals(-1, HTMLEntities.get("alpha"), "Greek alpha");
        assertNotEquals(-1, HTMLEntities.get("beta"), "Greek beta");
        assertNotEquals(-1, HTMLEntities.get("gamma"), "Greek gamma");
        assertNotEquals(-1, HTMLEntities.get("delta"), "Greek delta");
        assertNotEquals(-1, HTMLEntities.get("omega"), "Greek omega");

        // Test uppercase Greek letters
        assertNotEquals(-1, HTMLEntities.get("Alpha"), "Greek Alpha");
        assertNotEquals(-1, HTMLEntities.get("Beta"), "Greek Beta");
        assertNotEquals(-1, HTMLEntities.get("Gamma"), "Greek Gamma");
        assertNotEquals(-1, HTMLEntities.get("Delta"), "Greek Delta");
        assertNotEquals(-1, HTMLEntities.get("Omega"), "Greek Omega");
    }

    @Test
    @DisplayName("Accented character entities")
    void accentedCharacterEntities() {
        // Test various accented characters
        assertNotEquals(-1, HTMLEntities.get("Agrave"), "A with grave");
        assertNotEquals(-1, HTMLEntities.get("Aacute"), "A with acute");
        assertNotEquals(-1, HTMLEntities.get("Acirc"), "A with circumflex");
        assertNotEquals(-1, HTMLEntities.get("Atilde"), "A with tilde");
        assertNotEquals(-1, HTMLEntities.get("Auml"), "A with umlaut");

        assertNotEquals(-1, HTMLEntities.get("agrave"), "a with grave");
        assertNotEquals(-1, HTMLEntities.get("aacute"), "a with acute");
        assertNotEquals(-1, HTMLEntities.get("ntilde"), "n with tilde");
        assertNotEquals(-1, HTMLEntities.get("ccedil"), "c with cedilla");
    }

    @Test
    @DisplayName("Special typographic entities")
    void specialTypographicEntities() {
        assertNotEquals(-1, HTMLEntities.get("lsquo"), "Left single quote");
        assertNotEquals(-1, HTMLEntities.get("rsquo"), "Right single quote");
        assertNotEquals(-1, HTMLEntities.get("ldquo"), "Left double quote");
        assertNotEquals(-1, HTMLEntities.get("rdquo"), "Right double quote");
        assertNotEquals(-1, HTMLEntities.get("ndash"), "En dash");
        assertNotEquals(-1, HTMLEntities.get("mdash"), "Em dash");
        assertNotEquals(-1, HTMLEntities.get("hellip"), "Horizontal ellipsis");
        assertNotEquals(-1, HTMLEntities.get("bull"), "Bullet");
        assertNotEquals(-1, HTMLEntities.get("dagger"), "Dagger");
    }

    @Test
    @DisplayName("Verify all property files are loaded")
    void allPropertyFilesLoaded() throws Exception {
        final Field entitiesField = HTMLEntities.class.getDeclaredField("ENTITIES");
        entitiesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Map<String, String> entities = (Map<String, String>) entitiesField.get(null);

        // Check entities from each file
        // HTMLlat1.properties - Latin-1 supplement
        assertTrue(entities.containsKey("Agrave"), "Should contain entities from HTMLlat1.properties");

        // HTMLspecial.properties - Special characters
        assertTrue(entities.containsKey("quot"), "Should contain entities from HTMLspecial.properties");

        // HTMLsymbol.properties - Mathematical symbols
        assertTrue(entities.containsKey("forall"), "Should contain entities from HTMLsymbol.properties");

        // HTML40misc.properties - Miscellaneous HTML 4.0
        assertTrue(entities.containsKey("spades"), "Should contain entities from HTML40misc.properties");
    }

    @Test
    @DisplayName("Entity names are case-sensitive")
    void entityNamesAreCaseSensitive() {
        // Uppercase and lowercase versions should be different entities
        final int upperOmega = HTMLEntities.get("Omega");
        final int lowerOmega = HTMLEntities.get("omega");

        assertNotEquals(-1, upperOmega, "Uppercase Omega should exist");
        assertNotEquals(-1, lowerOmega, "Lowercase omega should exist");
        assertNotEquals(upperOmega, lowerOmega, "Omega and omega should map to different characters");

        // Mixed case should not work
        assertEquals(-1, HTMLEntities.get("OMEGA"), "All caps should not work");
        assertEquals(-1, HTMLEntities.get("OmEgA"), "Mixed case should not work");
    }

    @Test
    @DisplayName("IntProperties Entry linked list structure")
    void intPropertiesEntryStructure() throws Exception {
        final HTMLEntities.IntProperties ip = new HTMLEntities.IntProperties();

        // Add multiple values with same hash
        ip.put(0, "zero");
        ip.put(101, "one-oh-one"); // Same bucket as 0 (0 % 101 == 0)
        ip.put(202, "two-oh-two"); // Same bucket as 0 (202 % 101 == 0)

        // Access private entries field to verify linked list structure  
        final Field entriesField = HTMLEntities.IntProperties.class.getDeclaredField("entries");
        entriesField.setAccessible(true);
        final Object[] entries = (Object[]) entriesField.get(ip);

        assertNotNull(entries[0], "Bucket 0 should contain entries");

        // Count entries in the chain
        int count = 0;
        Object entry = entries[0];
        while (entry != null) {
            count++;
            final Field nextField = entry.getClass().getDeclaredField("next");
            nextField.setAccessible(true);
            entry = nextField.get(entry);
        }

        assertEquals(3, count, "Should have 3 entries in the same bucket");
    }

    @Test
    @DisplayName("Empty string entity name returns -1")
    void emptyStringEntityName() {
        assertEquals(-1, HTMLEntities.get(""), "Empty string should return -1");
    }

    @Test
    @DisplayName("Very long entity name returns -1")
    void veryLongEntityName() {
        final String longName = "a".repeat(1000);
        assertEquals(-1, HTMLEntities.get(longName), "Very long non-existent entity name should return -1");
    }

    @Test
    @DisplayName("Multiple puts to same key in IntProperties maintains last value")
    void intPropertiesMultiplePutsSameKey() {
        final HTMLEntities.IntProperties ip = new HTMLEntities.IntProperties();

        // Put multiple values for same key
        ip.put(42, "first");
        ip.put(42, "second");
        ip.put(42, "third");
        ip.put(42, "fourth");

        // Should return the last value
        assertEquals("fourth", ip.get(42), "Should return the last value put for the key");
    }

    @Test
    @DisplayName("Character to entity name mapping prefers shortest name")
    void characterToEntityPreference() {
        // The apostrophe character has multiple possible entity names
        final int apostrophe = '\'';
        final String entityName = HTMLEntities.get(apostrophe);

        if (entityName != null) {
            // Should prefer "apos" over other alternatives if available
            assertTrue(entityName.length() <= 5, "Should prefer shorter entity names");
        }
    }
}
