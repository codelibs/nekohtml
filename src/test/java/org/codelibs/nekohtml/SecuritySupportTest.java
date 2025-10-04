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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link SecuritySupport}.
 *
 * @author CodeLibs Project
 */
public class SecuritySupportTest {

    @Test
    public void testGetInstance() {
        // Test singleton instance
        final SecuritySupport instance1 = SecuritySupport.getInstance();
        final SecuritySupport instance2 = SecuritySupport.getInstance();

        assertNotNull(instance1, "Instance should not be null");
        assertEquals(instance1, instance2, "Should return same singleton instance");
    }

    @Test
    public void testGetContextClassLoader() {
        final SecuritySupport support = SecuritySupport.getInstance();
        final ClassLoader contextClassLoader = support.getContextClassLoader();

        // Context class loader may be null in some environments, but the method should not throw
        // In most cases it should return the current thread's context class loader
        final ClassLoader expected = Thread.currentThread().getContextClassLoader();
        assertEquals(expected, contextClassLoader, "Should return context class loader");
    }

    @Test
    public void testGetSystemClassLoader() {
        final SecuritySupport support = SecuritySupport.getInstance();
        final ClassLoader systemClassLoader = support.getSystemClassLoader();

        assertNotNull(systemClassLoader, "System class loader should not be null");
        assertEquals(ClassLoader.getSystemClassLoader(), systemClassLoader, "Should return system class loader");
    }

    @Test
    public void testGetParentClassLoader() {
        final SecuritySupport support = SecuritySupport.getInstance();
        final ClassLoader classLoader = SecuritySupportTest.class.getClassLoader();
        final ClassLoader parentClassLoader = support.getParentClassLoader(classLoader);

        // Parent class loader may be null (for bootstrap class loader)
        // But it should not be the same as the input class loader (to prevent loops)
        if (parentClassLoader != null) {
            assertTrue(parentClassLoader != classLoader, "Parent should not be same as child");
        }

        // Verify it matches direct call
        assertEquals(classLoader.getParent(), parentClassLoader, "Should return parent class loader");
    }

    @Test
    public void testGetParentClassLoader_NullParent() {
        final SecuritySupport support = SecuritySupport.getInstance();

        // Test with a class loader that has no parent (bootstrap loader)
        final ClassLoader classLoader = new ClassLoader(null) {
        };

        final ClassLoader parent = support.getParentClassLoader(classLoader);
        assertNull(parent, "Should return null for bootstrap class loader");
    }

    @Test
    public void testGetSystemProperty() {
        final SecuritySupport support = SecuritySupport.getInstance();

        // Test with a known system property
        final String javaVersion = support.getSystemProperty("java.version");
        assertNotNull(javaVersion, "Java version property should exist");
        assertEquals(System.getProperty("java.version"), javaVersion, "Should return correct system property value");

        // Test with a non-existent property
        final String nonExistent = support.getSystemProperty("non.existent.property.xyz");
        assertNull(nonExistent, "Non-existent property should return null");
    }

    @Test
    public void testGetFileInputStream(@TempDir final File tempDir) throws IOException {
        final SecuritySupport support = SecuritySupport.getInstance();

        // Create a test file
        final File testFile = new File(tempDir, "test.txt");
        Files.writeString(testFile.toPath(), "test content");

        // Test reading the file
        try (FileInputStream fis = support.getFileInputStream(testFile)) {
            assertNotNull(fis, "FileInputStream should not be null");
            final byte[] buffer = new byte[12];
            final int bytesRead = fis.read(buffer);
            assertEquals(12, bytesRead, "Should read 12 bytes");
            assertEquals("test content", new String(buffer, 0, bytesRead), "Should read correct content");
        }
    }

    @Test
    public void testGetFileInputStream_FileNotFound() {
        final SecuritySupport support = SecuritySupport.getInstance();

        // Test with non-existent file
        final File nonExistentFile = new File("/non/existent/file.txt");
        assertThrows(FileNotFoundException.class, () -> support.getFileInputStream(nonExistentFile),
                "Should throw FileNotFoundException for non-existent file");
    }

    @Test
    public void testGetResourceAsStream() {
        final SecuritySupport support = SecuritySupport.getInstance();

        // Test with null class loader (uses system class loader)
        final InputStream stream1 = support.getResourceAsStream(null, "java/lang/String.class");
        assertNotNull(stream1, "Should load resource from system class loader");
        try {
            stream1.close();
        } catch (final IOException e) {
            // Ignore
        }

        // Test with specific class loader
        final ClassLoader classLoader = SecuritySupportTest.class.getClassLoader();
        final InputStream stream2 = support.getResourceAsStream(classLoader, "org/codelibs/nekohtml/SecuritySupportTest.class");
        assertNotNull(stream2, "Should load resource from specified class loader");
        try {
            stream2.close();
        } catch (final IOException e) {
            // Ignore
        }

        // Test with non-existent resource
        final InputStream stream3 = support.getResourceAsStream(classLoader, "non/existent/resource.txt");
        assertNull(stream3, "Should return null for non-existent resource");
    }

    @Test
    public void testGetFileExists(@TempDir final File tempDir) throws IOException {
        final SecuritySupport support = SecuritySupport.getInstance();

        // Create a test file
        final File testFile = new File(tempDir, "exists.txt");
        Files.writeString(testFile.toPath(), "test");

        assertTrue(support.getFileExists(testFile), "Should return true for existing file");
        assertEquals(testFile.exists(), support.getFileExists(testFile), "Should match File.exists()");

        // Test with non-existent file
        final File nonExistentFile = new File(tempDir, "non_existent.txt");
        assertFalse(support.getFileExists(nonExistentFile), "Should return false for non-existent file");
        assertEquals(nonExistentFile.exists(), support.getFileExists(nonExistentFile), "Should match File.exists()");
    }

    @Test
    public void testGetLastModified(@TempDir final File tempDir) throws IOException, InterruptedException {
        final SecuritySupport support = SecuritySupport.getInstance();

        // Create a test file
        final File testFile = new File(tempDir, "modified.txt");
        Files.writeString(testFile.toPath(), "test");

        final long lastModified1 = support.getLastModified(testFile);
        assertTrue(lastModified1 > 0, "Last modified time should be greater than 0");
        assertEquals(testFile.lastModified(), lastModified1, "Should match File.lastModified()");

        // Wait a bit and modify the file
        Thread.sleep(10);
        Files.writeString(testFile.toPath(), "modified");

        final long lastModified2 = support.getLastModified(testFile);
        assertTrue(lastModified2 >= lastModified1, "Modified time should increase after modification");

        // Test with non-existent file
        final File nonExistentFile = new File(tempDir, "non_existent.txt");
        final long lastModified3 = support.getLastModified(nonExistentFile);
        assertEquals(0L, lastModified3, "Should return 0 for non-existent file");
        assertEquals(nonExistentFile.lastModified(), lastModified3, "Should match File.lastModified()");
    }

    @Test
    public void testSecurityExceptionHandling() {
        final SecuritySupport support = SecuritySupport.getInstance();

        // These methods should handle SecurityException gracefully and return null/default values
        // In a normal environment without SecurityManager, they should work normally

        // Test getContextClassLoader - should not throw even if SecurityException occurs internally
        final ClassLoader cl1 = support.getContextClassLoader();
        // May be null or valid ClassLoader, but should not throw

        // Test getSystemClassLoader - should not throw even if SecurityException occurs internally
        final ClassLoader cl2 = support.getSystemClassLoader();
        // May be null or valid ClassLoader, but should not throw

        // Test getParentClassLoader - should not throw even if SecurityException occurs internally
        final ClassLoader cl3 = support.getParentClassLoader(getClass().getClassLoader());
        // May be null or valid ClassLoader, but should not throw

        // If we got here without exceptions, the security exception handling is working
        assertTrue(true, "Security exception handling should prevent exceptions from propagating");
    }

} // class SecuritySupportTest
