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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link SecuritySupport}.
 * This exercises privileged access helpers across file, classloader and property operations.
 */
public class SecuritySupportTest {

    /**
     * Simple custom ClassLoader that can provide a resource stream for a specific resource name.
     */
    static class ResourceClassLoader extends ClassLoader {
        private final String resourceName;
        private final byte[] data;

        ResourceClassLoader(final String resourceName, final byte[] data) {
            // Use null parent to avoid default delegation to system class loader for clarity
            super(null);
            this.resourceName = resourceName;
            this.data = data;
        }

        @Override
        public InputStream getResourceAsStream(final String name) {
            if (resourceName.equals(name)) {
                return new ByteArrayInputStream(data);
            }
            return super.getResourceAsStream(name);
        }
    }

    /**
     * Custom ClassLoader exposing a public constructor to control the parent for testing getParentClassLoader.
     */
    static class ParentAwareClassLoader extends ClassLoader {
        ParentAwareClassLoader(final ClassLoader parent) {
            super(parent);
        }
    }

    @Test
    public void testGetInstanceSingleton() {
        final SecuritySupport s1 = SecuritySupport.getInstance();
        final SecuritySupport s2 = SecuritySupport.getInstance();
        assertNotNull(s1, "getInstance should not return null");
        assertSame(s1, s2, "getInstance should return the singleton instance");
    }

    @Test
    public void testGetContextClassLoader() {
        final SecuritySupport support = SecuritySupport.getInstance();
        final ClassLoader original = Thread.currentThread().getContextClassLoader();
        final ClassLoader custom = new ResourceClassLoader("noop", new byte[0]);
        try {
            Thread.currentThread().setContextClassLoader(custom);
            final ClassLoader cl = support.getContextClassLoader();
            assertSame(custom, cl, "Should return current thread context class loader");
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @Test
    public void testGetSystemClassLoader() {
        final SecuritySupport support = SecuritySupport.getInstance();
        final ClassLoader expected = ClassLoader.getSystemClassLoader();
        final ClassLoader actual = support.getSystemClassLoader();
        assertSame(expected, actual, "Should return system class loader");
    }

    @Test
    public void testGetParentClassLoader() {
        final SecuritySupport support = SecuritySupport.getInstance();

        final ClassLoader system = ClassLoader.getSystemClassLoader();
        final ClassLoader child = new ParentAwareClassLoader(system);
        final ClassLoader parent = support.getParentClassLoader(child);
        assertSame(system, parent, "Should return explicit parent class loader");

        final ClassLoader bootstrapChild = new ParentAwareClassLoader(null);
        final ClassLoader bootstrapParent = support.getParentClassLoader(bootstrapChild);
        assertNull(bootstrapParent, "Parent should be null for bootstrap parent");
    }

    @Test
    public void testGetSystemProperty() {
        final SecuritySupport support = SecuritySupport.getInstance();
        final String key = "nekohtml.test.property";
        try {
            System.setProperty(key, "value123");
            assertEquals("value123", support.getSystemProperty(key), "Should read system property value");
        } finally {
            System.clearProperty(key);
        }

        // Non-existent property should return null
        assertNull(support.getSystemProperty("nekohtml.property.does.not.exist"));
    }

    @Test
    public void testGetFileInputStreamAndExistsAndLastModified(@TempDir final Path tmp) throws Exception {
        final SecuritySupport support = SecuritySupport.getInstance();

        // Create a temporary file with content
        final Path file = tmp.resolve("sample.txt");
        final byte[] content = "Hello SecuritySupport".getBytes(StandardCharsets.UTF_8);
        Files.write(file, content);

        // exists()
        assertTrue(support.getFileExists(file.toFile()), "Temporary file should exist");

        // lastModified()
        final long expectedLastModified = file.toFile().lastModified();
        final long actualLastModified = support.getLastModified(file.toFile());
        assertEquals(expectedLastModified, actualLastModified, "lastModified should match java.io.File value");

        // getFileInputStream()
        try (FileInputStream fis = support.getFileInputStream(file.toFile())) {
            final byte[] read = fis.readAllBytes();
            assertArrayEquals(content, read, "Read bytes should match written content");
        }

        // Non-existing file should throw FileNotFoundException
        final File missing = tmp.resolve("missing.txt").toFile();
        assertThrows(FileNotFoundException.class, () -> support.getFileInputStream(missing));
    }

    @Test
    public void testGetResourceAsStreamWithNullClassLoader() throws Exception {
        final SecuritySupport support = SecuritySupport.getInstance();

        // Use an existing test resource from the repo to avoid adding new files
        try (InputStream is = support.getResourceAsStream(null, "data/canonical/README.txt")) {
            assertNotNull(is, "System resource should be found via system class loader");
            // Just ensure it is readable; content is not asserted exactly as it may change
            final byte[] bytes = is.readAllBytes();
            assertTrue(bytes.length > 0, "README resource should not be empty");
        }

        // Non-existent resource should return null
        try (InputStream is = support.getResourceAsStream(null, "definitely-not-present-12345.txt")) {
            assertNull(is, "Unknown system resource should return null");
        }
    }

    @Test
    public void testGetResourceAsStreamWithCustomClassLoader() throws Exception {
        final SecuritySupport support = SecuritySupport.getInstance();
        final byte[] data = "custom-data".getBytes(StandardCharsets.UTF_8);
        final ClassLoader cl = new ResourceClassLoader("custom.txt", data);

        try (InputStream is = support.getResourceAsStream(cl, "custom.txt")) {
            assertNotNull(is, "Custom class loader should provide the resource");
            final byte[] bytes = is.readAllBytes();
            assertArrayEquals(data, bytes);
        }

        try (InputStream is = support.getResourceAsStream(cl, "other.txt")) {
            assertNull(is, "Unknown resource on custom class loader should return null");
        }
    }

    /**
     * Test parent ClassLoader loop detection.
     * The SecuritySupport.getParentClassLoader() method handles the case where
     * a ClassLoader's parent is itself (which would cause infinite loop).
     * Note: We can't directly override getParent() as it's final in Java 17+,
     * but the logic is tested through SecuritySupport's implementation.
     */
    @Test
    public void testGetParentClassLoaderWithSystemClassLoader() {
        final SecuritySupport support = SecuritySupport.getInstance();

        // Test with system class loader (which may have special parent handling)
        final ClassLoader system = ClassLoader.getSystemClassLoader();
        final ClassLoader systemParent = support.getParentClassLoader(system);
        // The parent of system class loader might be null or another loader
        // We just ensure it doesn't return itself (loop prevention)
        assertTrue(systemParent != system, "Parent should not equal self (loop prevention)");
    }

    /**
     * Test that non-existing file returns false for exists check.
     */
    @Test
    public void testGetFileExistsNonExistingFile(@TempDir final Path tmp) {
        final SecuritySupport support = SecuritySupport.getInstance();
        final File nonExistingFile = tmp.resolve("non-existing-file.txt").toFile();
        assertFalse(support.getFileExists(nonExistingFile), "Non-existing file should return false");
    }

    /**
     * Test lastModified for non-existing file (should return 0).
     */
    @Test
    public void testGetLastModifiedNonExistingFile(@TempDir final Path tmp) {
        final SecuritySupport support = SecuritySupport.getInstance();
        final File nonExistingFile = tmp.resolve("non-existing-file.txt").toFile();
        assertEquals(0L, support.getLastModified(nonExistingFile), "Non-existing file lastModified should return 0");
    }
}
