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

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Additional coverage tests for {@link ObjectFactory} targeting uncovered paths:
 * <ul>
 *   <li>findJarServiceProvider() via META-INF/services lookup</li>
 *   <li>findClassLoader() fallback paths with custom classloaders</li>
 *   <li>findProviderClass() fallback with classloader that cannot find class</li>
 *   <li>Properties file caching in createObject</li>
 *   <li>createObject with explicit propertiesFilename parameter</li>
 * </ul>
 *
 * @author CodeLibs Project
 */
public class ObjectFactoryCoverageTest {

    private ClassLoader originalContextClassLoader;

    @BeforeEach
    public void setUp() {
        originalContextClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @AfterEach
    public void tearDown() {
        Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        // Reset static fields for properties caching by clearing any test system properties
        System.clearProperty("com.example.Factory");
        System.clearProperty("test.coverage.factory");
    }

    // -----------------------------------------------------------------------
    // findJarServiceProvider() tests
    // -----------------------------------------------------------------------

    @Test
    public void testJarServiceProviderDiscovery() {
        // The test resources contain META-INF/services/com.example.Factory
        // which points to org.codelibs.nekohtml.testclasses.SimpleProvider
        final Object result = ObjectFactory.createObject("com.example.Factory", null, null);

        assertNotNull(result, "Should discover provider via META-INF/services");
        assertEquals("org.codelibs.nekohtml.testclasses.SimpleProvider", result.getClass().getName(),
                "Should be an instance of SimpleProvider");
    }

    @Test
    public void testJarServiceProviderTakesPrecedenceOverFallback() {
        // META-INF/services/com.example.Factory points to SimpleProvider
        // Fallback is String - but service provider should win
        final Object result = ObjectFactory.createObject("com.example.Factory", null, "java.lang.String");

        assertNotNull(result);
        assertEquals("org.codelibs.nekohtml.testclasses.SimpleProvider", result.getClass().getName(),
                "Service provider should take precedence over fallback");
    }

    @Test
    public void testSystemPropertyTakesPrecedenceOverJarServiceProvider() {
        // System property should override META-INF/services
        System.setProperty("com.example.Factory", "java.lang.StringBuilder");
        try {
            final Object result = ObjectFactory.createObject("com.example.Factory", null, "java.lang.String");

            assertNotNull(result);
            assertTrue(result instanceof StringBuilder, "System property should take precedence over jar service provider");
        } finally {
            System.clearProperty("com.example.Factory");
        }
    }

    @Test
    public void testJarServiceProviderNotFoundReturnsNull() {
        // No META-INF/services file for this factory ID, and no fallback
        assertThrows(ObjectFactory.ConfigurationError.class, () -> {
            ObjectFactory.createObject("non.existent.service.factory", null, null);
        }, "Should throw ConfigurationError when no service provider and no fallback");
    }

    @Test
    public void testJarServiceProviderWithInvalidClassName() {
        // Create a service file pointing to a non-existent class
        // We need a META-INF/services file with an invalid class name
        // Use a custom classloader to provide such a file
        final ClassLoader customCl = new ClassLoader(Thread.currentThread().getContextClassLoader()) {
            @Override
            public InputStream getResourceAsStream(final String name) {
                if ("META-INF/services/test.invalid.service".equals(name)) {
                    return new ByteArrayInputStream("com.nonexistent.InvalidClass".getBytes(StandardCharsets.UTF_8));
                }
                return super.getResourceAsStream(name);
            }
        };

        Thread.currentThread().setContextClassLoader(customCl);

        assertThrows(ObjectFactory.ConfigurationError.class, () -> {
            ObjectFactory.createObject("test.invalid.service", null, null);
        }, "Should throw ConfigurationError for invalid class in service file");
    }

    @Test
    public void testJarServiceProviderWithEmptyServiceFile() {
        // Create a service file that is empty
        final ClassLoader customCl = new ClassLoader(Thread.currentThread().getContextClassLoader()) {
            @Override
            public InputStream getResourceAsStream(final String name) {
                if ("META-INF/services/test.empty.service".equals(name)) {
                    return new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
                }
                return super.getResourceAsStream(name);
            }
        };

        Thread.currentThread().setContextClassLoader(customCl);

        // Empty service file should cause findJarServiceProvider to return null,
        // then fallback is used
        final Object result = ObjectFactory.createObject("test.empty.service", null, "java.lang.String");
        assertNotNull(result);
        assertTrue(result instanceof String, "Should fall back to fallback class when service file is empty");
    }

    // -----------------------------------------------------------------------
    // findClassLoader() fallback paths
    // -----------------------------------------------------------------------

    @Test
    public void testFindClassLoaderWithCustomContextClassLoader() {
        // Set a custom context classloader that is not in the system classloader chain
        final ClassLoader customCl = new URLClassLoader(new URL[0], null);
        Thread.currentThread().setContextClassLoader(customCl);

        final ClassLoader result = ObjectFactory.findClassLoader();

        assertNotNull(result, "Should return a classloader");
        // When context CL is not in the system CL chain, should return context CL
        assertSame(customCl, result, "Should return the custom context classloader");
    }

    @Test
    public void testFindClassLoaderWithNullContextClassLoader() {
        // Set context classloader to null
        Thread.currentThread().setContextClassLoader(null);

        final ClassLoader result = ObjectFactory.findClassLoader();

        // When context CL is null, it will match the chain at some point
        // (null == null at boot classloader), so it falls through to the inner loop
        assertNotNull(result, "Should still return a valid classloader");
    }

    @Test
    public void testFindClassLoaderWithSystemClassLoaderAsContext() {
        // Set context classloader to the system classloader itself
        final ClassLoader systemCl = ClassLoader.getSystemClassLoader();
        Thread.currentThread().setContextClassLoader(systemCl);

        final ClassLoader result = ObjectFactory.findClassLoader();

        assertNotNull(result, "Should return a valid classloader");
    }

    @Test
    public void testFindClassLoaderWithParentOfSystemAsContext() {
        // Set context classloader to the parent of the system classloader
        final ClassLoader systemCl = ClassLoader.getSystemClassLoader();
        final ClassLoader parentCl = systemCl.getParent();
        if (parentCl != null) {
            Thread.currentThread().setContextClassLoader(parentCl);

            final ClassLoader result = ObjectFactory.findClassLoader();

            assertNotNull(result, "Should return a valid classloader");
        }
        // If parent is null, skip - no meaningful test possible
    }

    // -----------------------------------------------------------------------
    // findProviderClass() fallback paths
    // -----------------------------------------------------------------------

    @Test
    public void testFindProviderClassFallbackToCurrentClassLoader() throws Exception {
        // Use a classloader that cannot find the class, but the current (ObjectFactory's)
        // classloader can. With doFallback=true, it should fall back.
        final ClassLoader emptyCl = new URLClassLoader(new URL[0], null);
        final String className = "org.codelibs.nekohtml.ObjectFactory";

        // emptyCl has no parent (null), so it won't find ObjectFactory
        // With doFallback=true, should fall back to ObjectFactory.class.getClassLoader()
        final Class<?> clazz = ObjectFactory.findProviderClass(className, emptyCl, true);

        assertNotNull(clazz, "Should find class through fallback classloader");
        assertEquals(className, clazz.getName());
    }

    @Test
    public void testFindProviderClassNoFallbackThrows() {
        // Use a classloader that cannot find the class, with doFallback=false
        final ClassLoader emptyCl = new URLClassLoader(new URL[0], null);
        final String className = "org.codelibs.nekohtml.ObjectFactory";

        assertThrows(ClassNotFoundException.class, () -> {
            ObjectFactory.findProviderClass(className, emptyCl, false);
        }, "Should throw ClassNotFoundException when doFallback is false");
    }

    @Test
    public void testFindProviderClassWithNullClassLoaderUsesClassForName() throws Exception {
        // null classloader should use Class.forName()
        // java.lang.Thread is always available via bootstrap
        final Class<?> clazz = ObjectFactory.findProviderClass("java.lang.Thread", null, false);

        assertNotNull(clazz);
        assertEquals(Thread.class, clazz, "Should find Thread class via Class.forName");
    }

    @Test
    public void testFindProviderClassWithNullClassLoaderInvalidClass() {
        // null classloader + invalid class name
        assertThrows(ClassNotFoundException.class, () -> {
            ObjectFactory.findProviderClass("com.nonexistent.NoSuchClass", null, false);
        }, "Should throw ClassNotFoundException for invalid class with null classloader");
    }

    @Test
    public void testFindProviderClassFallbackWhenCurrentClassLoaderDiffers() throws Exception {
        // Create a classloader that wraps the system one but is a distinct instance
        final ClassLoader wrapperCl = new URLClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
        // wrapperCl delegates to system CL, so it can find java.lang.String,
        // but for classes only on the app classpath, let's test with a class
        // that a restricted classloader cannot find

        final ClassLoader restrictedCl = new ClassLoader(null) {
            @Override
            public Class<?> loadClass(final String name) throws ClassNotFoundException {
                // Only delegate java.* classes to bootstrap
                if (name.startsWith("java.")) {
                    return super.loadClass(name);
                }
                throw new ClassNotFoundException(name);
            }
        };

        // restrictedCl can't find ObjectFactory, but fallback (ObjectFactory's own CL) can
        final Class<?> clazz = ObjectFactory.findProviderClass("org.codelibs.nekohtml.ObjectFactory", restrictedCl, true);

        assertNotNull(clazz);
        assertEquals("org.codelibs.nekohtml.ObjectFactory", clazz.getName());
    }

    // -----------------------------------------------------------------------
    // Properties file caching tests
    // -----------------------------------------------------------------------

    @Test
    public void testCreateObjectWithExplicitPropertiesFile(@TempDir Path tempDir) throws IOException {
        // Create a temporary properties file with a factory mapping
        final Path propsFile = tempDir.resolve("test.properties");
        try (PrintWriter pw = new PrintWriter(Files.newOutputStream(propsFile))) {
            pw.println("test.coverage.factory=java.lang.StringBuilder");
        }

        // Use the three-arg version with explicit propertiesFilename
        final Object result = ObjectFactory.createObject("test.coverage.factory", propsFile.toString(), "java.lang.String");

        assertNotNull(result);
        assertTrue(result instanceof StringBuilder, "Should use class from properties file, not fallback");
    }

    @Test
    public void testCreateObjectWithExplicitPropertiesFileMissingKey(@TempDir Path tempDir) throws IOException {
        // Create a properties file without the requested key
        final Path propsFile = tempDir.resolve("test.properties");
        try (PrintWriter pw = new PrintWriter(Files.newOutputStream(propsFile))) {
            pw.println("other.key=java.lang.StringBuilder");
        }

        // The key is not in the properties file, should fall through to fallback
        final Object result = ObjectFactory.createObject("test.coverage.factory", propsFile.toString(), "java.lang.String");

        assertNotNull(result);
        assertTrue(result instanceof String, "Should fall back when key not found in properties file");
    }

    @Test
    public void testCreateObjectWithNonExistentPropertiesFile() {
        // Explicit properties file path that doesn't exist - should skip and use fallback
        final Object result =
                ObjectFactory.createObject("test.coverage.factory", "/nonexistent/path/to/properties.file", "java.lang.String");

        assertNotNull(result);
        assertTrue(result instanceof String, "Should fall back when properties file doesn't exist");
    }

    @Test
    public void testPropertiesCachingMultipleCalls() {
        // Call createObject multiple times with null propertiesFilename
        // to exercise the caching logic in the synchronized block
        final String factoryId = "test.coverage.factory";
        final String fallbackClassName = "java.lang.String";

        // First call initializes the cache state
        final Object result1 = ObjectFactory.createObject(factoryId, null, fallbackClassName);
        assertNotNull(result1);

        // Second call hits the cache path (fLastModified >= 0 or still -1)
        final Object result2 = ObjectFactory.createObject(factoryId, null, fallbackClassName);
        assertNotNull(result2);

        // Third call to further exercise the "file wasn't modified" branch
        final Object result3 = ObjectFactory.createObject(factoryId, null, fallbackClassName);
        assertNotNull(result3);

        // All should succeed with the same class
        assertTrue(result1 instanceof String);
        assertTrue(result2 instanceof String);
        assertTrue(result3 instanceof String);
    }

    // -----------------------------------------------------------------------
    // newInstance() edge cases
    // -----------------------------------------------------------------------

    @Test
    public void testNewInstanceWithClassLoaderAndValidClass() {
        // Use system classloader explicitly
        final ClassLoader cl = ClassLoader.getSystemClassLoader();
        final Object result = ObjectFactory.newInstance("java.util.ArrayList", cl, true);

        assertNotNull(result);
        assertTrue(result instanceof java.util.ArrayList, "Should create ArrayList instance");
    }

    @Test
    public void testNewInstanceFallbackWithDifferentClassLoader() {
        // Use an empty classloader that delegates to null (bootstrap only)
        final ClassLoader emptyCl = new URLClassLoader(new URL[0], null);

        // This class is on the app classpath but not bootstrap,
        // so emptyCl can't find it. With doFallback=true, it falls back
        // to ObjectFactory.class.getClassLoader()
        final Object result = ObjectFactory.newInstance("org.codelibs.nekohtml.testclasses.SimpleProvider", emptyCl, true);

        assertNotNull(result);
        assertEquals("org.codelibs.nekohtml.testclasses.SimpleProvider", result.getClass().getName());
    }

    @Test
    public void testNewInstanceNoFallbackWithUnloadableClass() {
        final ClassLoader emptyCl = new URLClassLoader(new URL[0], null);

        assertThrows(ObjectFactory.ConfigurationError.class, () -> {
            ObjectFactory.newInstance("org.codelibs.nekohtml.testclasses.SimpleProvider", emptyCl, false);
        }, "Should throw ConfigurationError when class not found and no fallback");
    }

    // -----------------------------------------------------------------------
    // ConfigurationError additional coverage
    // -----------------------------------------------------------------------

    @Test
    public void testConfigurationErrorExceptionRetrieval() {
        final RuntimeException cause = new RuntimeException("root cause");
        final ObjectFactory.ConfigurationError error = new ObjectFactory.ConfigurationError("msg", cause);

        assertSame(cause, error.getException());
        assertEquals("msg", error.getMessage());
        assertNotNull(error.toString());
    }

    @Test
    public void testConfigurationErrorIsThrowable() {
        final ObjectFactory.ConfigurationError error = new ObjectFactory.ConfigurationError("test", null);

        // ConfigurationError extends Error, verify inheritance
        assertTrue(error instanceof Error);
        assertTrue(error instanceof Throwable);
        assertNull(error.getException());
    }

    // -----------------------------------------------------------------------
    // createObject two-arg delegates to three-arg
    // -----------------------------------------------------------------------

    @Test
    public void testCreateObjectTwoArgDelegation() {
        // Verify the two-arg version delegates correctly by comparing behavior
        final Object fromTwoArg = ObjectFactory.createObject("test.coverage.factory", "java.lang.String");
        final Object fromThreeArg = ObjectFactory.createObject("test.coverage.factory", null, "java.lang.String");

        assertNotNull(fromTwoArg);
        assertNotNull(fromThreeArg);
        assertEquals(fromTwoArg.getClass(), fromThreeArg.getClass(), "Two-arg and three-arg should produce same class");
    }

    // -----------------------------------------------------------------------
    // Context classloader scenarios for findJarServiceProvider
    // -----------------------------------------------------------------------

    @Test
    public void testJarServiceProviderFallbackToCurrentClassLoader() {
        // Set context classloader to one that cannot find the service file
        // but ObjectFactory's classloader can
        final ClassLoader emptyCl = new URLClassLoader(new URL[0], null) {
            @Override
            public InputStream getResourceAsStream(final String name) {
                // Return null for everything - force fallback to current CL
                return null;
            }
        };

        Thread.currentThread().setContextClassLoader(emptyCl);

        // com.example.Factory service file is on the classpath accessible by
        // ObjectFactory's classloader. The fallback in findJarServiceProvider
        // should find it if cl != current.
        final Object result = ObjectFactory.createObject("com.example.Factory", null, "java.lang.String");

        assertNotNull(result);
        // Could be either SimpleProvider (from service file) or String (fallback)
        // depending on whether the fallback path in findJarServiceProvider kicks in
    }

    @Test
    public void testCreateObjectWithSystemPropertySecurityHandled() {
        // Set a system property pointing to a valid class
        final String factoryId = "test.coverage.factory";
        System.setProperty(factoryId, "java.util.HashMap");

        try {
            final Object result = ObjectFactory.createObject(factoryId, null, "java.lang.String");
            assertNotNull(result);
            assertTrue(result instanceof java.util.HashMap, "Should use class from system property");
        } finally {
            System.clearProperty(factoryId);
        }
    }

    @Test
    public void testCreateObjectWithExplicitPropertiesFileAndSystemProperty(@TempDir Path tempDir) throws IOException {
        // System property should take precedence over properties file
        final Path propsFile = tempDir.resolve("test.properties");
        try (PrintWriter pw = new PrintWriter(Files.newOutputStream(propsFile))) {
            pw.println("test.coverage.factory=java.util.ArrayList");
        }

        System.setProperty("test.coverage.factory", "java.util.LinkedList");
        try {
            final Object result = ObjectFactory.createObject("test.coverage.factory", propsFile.toString(), "java.lang.String");

            assertNotNull(result);
            assertTrue(result instanceof java.util.LinkedList, "System property should take precedence over properties file");
        } finally {
            System.clearProperty("test.coverage.factory");
        }
    }

}
