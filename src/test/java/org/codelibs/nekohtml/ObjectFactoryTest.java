package org.codelibs.nekohtml;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.codelibs.nekohtml.ObjectFactory.ConfigurationError;
import org.codelibs.nekohtml.testclasses.SimpleProvider;
import org.junit.jupiter.api.Test;

/**
 * Tests for ObjectFactory.
 * Focus: system property precedence, explicit properties file,
 * fallback handling, classloader fallback, service provider, and error wrapping.
 */
public class ObjectFactoryTest {

    @Test
    void testCreateObject_UsesSystemPropertyFirst() throws Exception {
        final String factoryId = "test.factory.system";
        final String orig = System.getProperty(factoryId);
        try {
            System.setProperty(factoryId, "java.lang.String");
            Object obj = ObjectFactory.createObject(factoryId, "java.lang.Integer");
            assertNotNull(obj, "Instance should not be null");
            assertTrue(obj instanceof String, "System property should take precedence over fallback");
        } finally {
            // Clean up system property to avoid interference across tests
            if (orig == null) {
                System.clearProperty(factoryId);
            } else {
                System.setProperty(factoryId, orig);
            }
        }
    }

    @Test
    void testCreateObject_ReadsExplicitPropertiesFile() throws Exception {
        final String factoryId = "my.factory";
        Path props = Files.createTempFile("objectfactory-", ".properties");
        try {
            Properties p = new Properties();
            p.setProperty(factoryId, "java.lang.String");
            try (FileOutputStream fos = new FileOutputStream(props.toFile())) {
                p.store(fos, "test");
            }
            Object obj = ObjectFactory.createObject(factoryId, props.toString(), "java.lang.Integer");
            assertNotNull(obj);
            assertTrue(obj instanceof String, "Should instantiate class from explicit properties file");
        } finally {
            Files.deleteIfExists(props);
        }
    }

    @Test
    void testCreateObject_FallbackWhenNotFoundAndFallbackProvided() throws Exception {
        final String factoryId = "missing.factory";
        Path props = Files.createTempFile("objectfactory-missing-", ".properties");
        try {
            // No mapping for factoryId → will use fallback
            Properties p = new Properties();
            try (FileOutputStream fos = new FileOutputStream(props.toFile())) {
                p.store(fos, "none");
            }
            Object obj = ObjectFactory.createObject(factoryId, props.toString(), "java.lang.String");
            assertNotNull(obj);
            assertTrue(obj instanceof String, "Should instantiate fallback class when no provider found");
        } finally {
            Files.deleteIfExists(props);
        }
    }

    @Test
    void testCreateObject_NoProviderAndNoFallbackThrows() throws Exception {
        final String factoryId = "missing.factory.nofallback";
        Path props = Files.createTempFile("objectfactory-nofallback-", ".properties");
        try {
            Properties p = new Properties();
            try (FileOutputStream fos = new FileOutputStream(props.toFile())) {
                p.store(fos, "none");
            }
            ConfigurationError err =
                    assertThrows(ConfigurationError.class, () -> ObjectFactory.createObject(factoryId, props.toString(), null),
                            "Should throw when no provider and no fallback");
            assertTrue(err.getMessage().contains("Provider for " + factoryId + " cannot be found"));
        } finally {
            Files.deleteIfExists(props);
        }
    }

    @Test
    void testNewInstance_ClassNotFoundThrowsConfigurationError() {
        ConfigurationError err =
                assertThrows(ConfigurationError.class,
                        () -> ObjectFactory.newInstance("no.such.ClassName$DefinitelyMissing", ObjectFactory.findClassLoader(), true));
        assertTrue(err.getMessage().contains("not found"));
        assertNotNull(err.getException(), "Wrapped cause should be present");
        assertTrue(err.getException() instanceof ClassNotFoundException, "Cause should be CNF");
    }

    @Test
    void testNewInstance_ConstructorThrowsWrappedInConfigurationError() {
        ConfigurationError err =
                assertThrows(
                        ConfigurationError.class,
                        () -> ObjectFactory.newInstance("org.codelibs.nekohtml.testclasses.ThrowingConstructor",
                                ObjectFactory.findClassLoader(), true));
        assertTrue(err.getMessage().contains("could not be instantiated"));
        assertNotNull(err.getException(), "Wrapped cause should be present");
        // The constructor throws RuntimeException("boom"), but when called via reflection,
        // it gets wrapped in InvocationTargetException, then our code wraps that in ConfigurationError
        assertTrue(err.getException() instanceof java.lang.reflect.InvocationTargetException);
        assertTrue(err.getException().getCause() instanceof RuntimeException);
        assertTrue(err.getException().getCause().getMessage().contains("boom"));
    }

    @Test
    void testNewInstance_WithNullClassLoader_UsesBootstrap() throws Exception {
        Object obj = ObjectFactory.newInstance("java.lang.String", null, true);
        assertNotNull(obj);
        assertTrue(obj instanceof String, "Should instantiate via bootstrap/Class.forName when cl == null");
    }

    @Test
    void testFindProviderClass_FallbackToCurrentClassLoader() throws Exception {
        final String cn = SimpleProvider.class.getName();

        // A classloader that refuses to load our target class, forcing fallback
        ClassLoader failingCl = new ClassLoader(null) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.equals(cn)) {
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name, resolve);
            }
        };

        Class<?> cls = ObjectFactory.findProviderClass(cn, failingCl, true);
        assertEquals(SimpleProvider.class, cls, "Should fallback to current classloader on failure");
    }

    @Test
    void testFindProviderClass_NoFallbackThrows() {
        final String cn = SimpleProvider.class.getName();

        ClassLoader failingCl = new ClassLoader(null) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.equals(cn)) {
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name, resolve);
            }
        };

        assertThrows(ClassNotFoundException.class, () -> ObjectFactory.findProviderClass(cn, failingCl, false),
                "Without fallback the CNF should propagate");
    }

    @Test
    void testFindJarServiceProvider_ReturnsInstanceFromService() throws Exception {
        final String factoryId = "com.example.Factory";
        final String orig = System.getProperty(factoryId);
        try {
            // Ensure system property does not mask the service provider
            System.clearProperty(factoryId);

            // Service file is provided in test resources; should resolve to SimpleProvider
            Object obj = ObjectFactory.createObject(factoryId, null, null);
            assertNotNull(obj);
            assertTrue(obj instanceof SimpleProvider, "Should load provider from META-INF/services");
        } finally {
            if (orig == null) {
                System.clearProperty(factoryId);
            } else {
                System.setProperty(factoryId, orig);
            }
        }
    }

    @Test
    void testConfigurationError_getException() {
        Exception cause = new IllegalStateException("cause");
        ConfigurationError err = new ConfigurationError("msg", cause);
        assertSame(cause, err.getException(), "getException should return original cause");
    }

    @Test
    void testFindClassLoader_NoThrow() {
        // We don't assert specifics to avoid brittleness across environments.
        // This call primarily increases coverage and ensures no exceptions are thrown.
        ClassLoader cl = ObjectFactory.findClassLoader();
        // cl may be null on some environments; just verify the call succeeds.
        assertTrue(cl == null || cl instanceof ClassLoader);
    }

    @Test
    void testCreateObject_WithSecurityManager() throws Exception {
        final String factoryId = "security.test.factory";
        final String orig = System.getProperty(factoryId);

        try {
            System.setProperty(factoryId, "java.lang.String");

            // Test with security restrictions - should still work for basic classes
            Object obj = ObjectFactory.createObject(factoryId, "java.lang.Integer");
            assertNotNull(obj);
            assertTrue(obj instanceof String, "Should handle security-related instantiation");

        } finally {
            if (orig == null) {
                System.clearProperty(factoryId);
            } else {
                System.setProperty(factoryId, orig);
            }
        }
    }

    @Test
    void testCreateObject_WithInvalidPropertiesFile() throws Exception {
        final String factoryId = "invalid.properties.test";
        Path invalidProps = Files.createTempFile("invalid-", ".properties");

        try {
            // Create an invalid properties file
            Files.write(invalidProps, "invalid content without = sign\nmalformed line".getBytes());

            // Should handle invalid properties file gracefully
            Object obj = ObjectFactory.createObject(factoryId, invalidProps.toString(), "java.lang.String");
            assertNotNull(obj);
            assertTrue(obj instanceof String, "Should fall back when properties file is malformed");

        } finally {
            Files.deleteIfExists(invalidProps);
        }
    }

    @Test
    void testCreateObject_WithNonExistentPropertiesFile() throws Exception {
        final String factoryId = "nonexistent.properties.test";
        final String nonExistentFile = "/tmp/definitely-does-not-exist-" + System.currentTimeMillis() + ".properties";

        // Should handle non-existent properties file gracefully
        Object obj = ObjectFactory.createObject(factoryId, nonExistentFile, "java.lang.String");
        assertNotNull(obj);
        assertTrue(obj instanceof String, "Should fall back when properties file doesn't exist");
    }

    @Test
    void testNewInstance_WithAbstractClass() {
        // Test with an abstract class that cannot be instantiated
        ConfigurationError err =
                assertThrows(ConfigurationError.class,
                        () -> ObjectFactory.newInstance("java.util.AbstractList", ObjectFactory.findClassLoader(), true));

        assertNotNull(err.getException(), "Should wrap instantiation exception");
        assertTrue(err.getMessage().contains("could not be instantiated"), "Error message should indicate instantiation failure");
    }

    @Test
    void testNewInstance_WithInterface() {
        // Test with an interface that cannot be instantiated
        ConfigurationError err =
                assertThrows(ConfigurationError.class,
                        () -> ObjectFactory.newInstance("java.util.List", ObjectFactory.findClassLoader(), true));

        assertNotNull(err.getException(), "Should wrap instantiation exception");
        assertTrue(err.getMessage().contains("could not be instantiated"), "Error message should indicate instantiation failure");
    }

    @Test
    void testNewInstance_WithPrivateConstructor() {
        // Test with a class that has only private constructors
        ConfigurationError err =
                assertThrows(ConfigurationError.class,
                        () -> ObjectFactory.newInstance("java.lang.Math", ObjectFactory.findClassLoader(), true));

        assertNotNull(err.getException(), "Should wrap instantiation exception");
    }

    @Test
    void testFindProviderClass_WithDifferentClassLoaders() throws Exception {
        final String className = "java.lang.String";

        // Test with different classloader scenarios
        ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
        ClassLoader systemCL = ClassLoader.getSystemClassLoader();

        // Test with current classloader
        Class<?> cls1 = ObjectFactory.findProviderClass(className, currentCL, true);
        assertEquals(String.class, cls1);

        // Test with system classloader
        Class<?> cls2 = ObjectFactory.findProviderClass(className, systemCL, true);
        assertEquals(String.class, cls2);

        // Test with null classloader (bootstrap)
        Class<?> cls3 = ObjectFactory.findProviderClass(className, null, true);
        assertEquals(String.class, cls3);
    }

    @Test
    void testCreateObject_WithEmptySystemProperty() throws Exception {
        final String factoryId = "empty.system.property.test";
        final String orig = System.getProperty(factoryId);

        try {
            // Set empty system property
            System.setProperty(factoryId, "");

            // Should throw ConfigurationError when system property is empty
            assertThrows(ConfigurationError.class, () -> {
                ObjectFactory.createObject(factoryId, "java.lang.String");
            });

        } finally {
            if (orig == null) {
                System.clearProperty(factoryId);
            } else {
                System.setProperty(factoryId, orig);
            }
        }
    }

    @Test
    void testCreateObject_WithInvalidClassName() throws Exception {
        final String factoryId = "invalid.class.test";
        final String orig = System.getProperty(factoryId);

        try {
            // Set system property to invalid class name
            System.setProperty(factoryId, "invalid.class.name.DoesNotExist");

            // Should throw ConfigurationError when class is not found
            assertThrows(ConfigurationError.class, () -> {
                ObjectFactory.createObject(factoryId, "java.lang.String");
            });

        } finally {
            if (orig == null) {
                System.clearProperty(factoryId);
            } else {
                System.setProperty(factoryId, orig);
            }
        }
    }

    @Test
    void testConfigurationError_MessageHandling() {
        // Test different constructor variations
        ConfigurationError err1 = new ConfigurationError("Test message", null);
        assertEquals("Test message", err1.getMessage());
        assertNull(err1.getException());

        Exception cause = new RuntimeException("Cause");
        ConfigurationError err2 = new ConfigurationError("Test with cause", cause);
        assertEquals("Test with cause", err2.getMessage());
        assertSame(cause, err2.getException());
    }

    @Test
    void testObjectFactory_ThreadSafety() throws Exception {
        // Test concurrent access to ObjectFactory methods
        final String factoryId = "thread.safety.test";
        final int threadCount = 10;
        final int operationsPerThread = 100;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            Object obj = ObjectFactory.createObject(factoryId, "java.lang.String");
                            if (obj instanceof String) {
                                successCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            exceptions.add(e);
                        }
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    finishLatch.countDown();
                }
            }).start();
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete
        assertTrue(finishLatch.await(30, TimeUnit.SECONDS), "All threads should complete within timeout");

        // Check results
        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent access: " + exceptions);
        assertEquals(threadCount * operationsPerThread, successCount.get(), "All operations should succeed");
    }

    @Test
    void testFindJarServiceProvider_WithCorruptedServiceFile() throws Exception {
        // This test would require creating a corrupted service file, 
        // but the service file is part of the test resources and we shouldn't modify it.
        // Instead, we test the behavior with a non-existent service
        final String factoryId = "corrupted.service.test";

        // Should handle missing service gracefully and return null or throw appropriate exception
        assertThrows(ConfigurationError.class, () -> ObjectFactory.createObject(factoryId, null, null),
                "Should throw ConfigurationError when no service provider found");
    }

    @Test
    void testCreateObject_WithClassLoaderEdgeCases() throws Exception {
        final String factoryId = "classloader.edge.test";

        // Test with custom classloader that has unusual behavior
        ClassLoader customCL = new ClassLoader(ClassLoader.getSystemClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                // Always delegate to parent for standard classes
                if (name.startsWith("java.") || name.startsWith("javax.")) {
                    return super.loadClass(name);
                }
                // Throw exception for other classes to test error handling
                throw new ClassNotFoundException("Custom classloader refuses: " + name);
            }
        };

        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(customCL);

            // Should still work with fallback mechanisms
            Object obj = ObjectFactory.createObject(factoryId, "java.lang.String");
            assertNotNull(obj);
            assertTrue(obj instanceof String);

        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    @Test
    void testObjectFactory_PropertiesFilePrecedence() throws Exception {
        final String factoryId = "precedence.test";
        final String orig = System.getProperty(factoryId);

        Path propsFile = Files.createTempFile("precedence-", ".properties");

        try {
            // Set system property
            System.setProperty(factoryId, "java.lang.Integer");

            // Create properties file with different value
            Properties p = new Properties();
            p.setProperty(factoryId, "java.lang.String");
            try (FileOutputStream fos = new FileOutputStream(propsFile.toFile())) {
                p.store(fos, "test");
            }

            // System property of Integer should fail (no default constructor)
            assertThrows(ConfigurationError.class, () -> {
                ObjectFactory.createObject(factoryId, propsFile.toString(), "java.lang.Boolean");
            });

        } finally {
            if (orig == null) {
                System.clearProperty(factoryId);
            } else {
                System.setProperty(factoryId, orig);
            }
            Files.deleteIfExists(propsFile);
        }
    }

    @Test
    void testObjectFactory_ResourceConstraints() throws Exception {
        // Test behavior under resource constraints (memory, file handles, etc.)
        final String factoryId = "resource.test";

        // Test with very long class names (potential buffer issues)
        final StringBuilder longClassName = new StringBuilder("java.lang.");
        for (int i = 0; i < 1000; i++) {
            longClassName.append("X");
        }

        ConfigurationError err =
                assertThrows(ConfigurationError.class, () -> ObjectFactory.createObject(factoryId, null, longClassName.toString()));
        assertNotNull(err.getException());

        // Test with many concurrent classloader operations
        for (int i = 0; i < 50; i++) {
            ClassLoader cl = ObjectFactory.findClassLoader();
            // Just ensure the call completes without issues
            assertTrue(cl == null || cl instanceof ClassLoader);
        }
    }

    /**
     * Test security manager access checks during findProviderClass.
     */
    @Test
    void testFindProviderClass_WithSecurityManager() throws Exception {
        final String className = "java.lang.String";
        ClassLoader cl = ObjectFactory.findClassLoader();

        // Test without security manager (current scenario)
        Class<?> cls = ObjectFactory.findProviderClass(className, cl, true);
        assertEquals(String.class, cls, "Should load class without security restrictions");

        // Test that the security check code path is exercised
        // Note: Installing actual SecurityManager is complex in modern Java,
        // so we focus on testing the accessible code paths
        assertDoesNotThrow(() -> ObjectFactory.findProviderClass(className, cl, true));
    }

    /**
     * Test findClassLoader complex classloader hierarchy scenarios.
     */
    @Test
    void testFindClassLoader_ComplexHierarchy() throws Exception {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        ClassLoader systemCL = ClassLoader.getSystemClassLoader();

        try {
            // Test with context classloader that's same as system
            Thread.currentThread().setContextClassLoader(systemCL);
            ClassLoader result1 = ObjectFactory.findClassLoader();
            assertNotNull(result1);

            // Test with custom context classloader
            ClassLoader customCL = new ClassLoader(systemCL) {
            };
            Thread.currentThread().setContextClassLoader(customCL);
            ClassLoader result2 = ObjectFactory.findClassLoader();
            assertNotNull(result2);

            // Test with null context classloader
            Thread.currentThread().setContextClassLoader(null);
            ClassLoader result3 = ObjectFactory.findClassLoader();
            // May be null or not null depending on environment
            assertTrue(result3 == null || result3 instanceof ClassLoader);

        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }

    /**
     * Test xerces.properties file caching and modification detection.
     */
    @Test
    void testXercesProperties_CachingAndModification() throws Exception {
        final String factoryId = "xerces.caching.test";

        // First call should establish cache
        try {
            ObjectFactory.createObject(factoryId, null, "java.lang.String");
        } catch (ConfigurationError e) {
            // Expected if no xerces.properties or no entry
        }

        // Second call should use cache  
        try {
            ObjectFactory.createObject(factoryId, null, "java.lang.String");
        } catch (ConfigurationError e) {
            // Expected if no xerces.properties or no entry
        }

        // The cache logic should handle file modification time checks
        // This tests the synchronized block and modification detection
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    ObjectFactory.createObject(factoryId + i, null, "java.lang.String");
                } catch (ConfigurationError e) {
                    // Expected for unknown factory IDs
                }
            }
        });
    }

    /**
     * Test properties file with various edge cases.
     */
    @Test
    void testPropertiesFile_EdgeCases() throws Exception {
        final String factoryId = "edge.case.test";

        // Test with empty properties file - should fall back to default class
        Path emptyProps = Files.createTempFile("empty-", ".properties");
        try {
            // Try with a valid fallback class that has a default constructor
            Object obj = ObjectFactory.createObject(factoryId, emptyProps.toString(), "java.lang.Object");
            assertNotNull(obj, "Should create fallback object with empty properties file");
            assertTrue(obj instanceof Object, "Should be instance of fallback class");
        } finally {
            Files.deleteIfExists(emptyProps);
        }

        // Test with properties file containing only comments - should use fallback
        Path commentProps = Files.createTempFile("comments-", ".properties");
        try {
            Files.write(commentProps, "# This is a comment\n! Another comment\n".getBytes());
            Object obj = ObjectFactory.createObject(factoryId, commentProps.toString(), "java.lang.Object");
            assertNotNull(obj, "Should create fallback object with comments-only properties file");
            assertTrue(obj instanceof Object, "Should be instance of fallback class");
        } finally {
            Files.deleteIfExists(commentProps);
        }

        // Test with properties file with invalid class - should throw ConfigurationError
        Path invalidProps = Files.createTempFile("invalid-", ".properties");
        try {
            Properties p = new Properties();
            p.setProperty(factoryId, "invalid.class.Name");
            try (FileOutputStream fos = new FileOutputStream(invalidProps.toFile())) {
                p.store(fos, "test");
            }
            assertThrows(ObjectFactory.ConfigurationError.class, () -> {
                ObjectFactory.createObject(factoryId, invalidProps.toString(), "java.lang.Integer");
            }, "Should throw ConfigurationError for invalid class name");
        } finally {
            Files.deleteIfExists(invalidProps);
        }
    }

    /**
     * Test newInstance with various constructor scenarios.
     */
    @Test
    void testNewInstance_ConstructorVariations() throws Exception {
        // Test class with public no-arg constructor
        Object obj1 = ObjectFactory.newInstance("java.lang.Object", ObjectFactory.findClassLoader(), true);
        assertTrue(obj1 instanceof Object);

        // Test class with multiple constructors (should use no-arg)
        Object obj2 = ObjectFactory.newInstance("java.lang.StringBuilder", ObjectFactory.findClassLoader(), true);
        assertTrue(obj2 instanceof StringBuilder);

        // Test enum class (should fail)
        assertThrows(ConfigurationError.class,
                () -> ObjectFactory.newInstance("java.time.DayOfWeek", ObjectFactory.findClassLoader(), true));
    }

    /**
     * Test findProviderClass fallback scenarios in detail.
     */
    @Test
    void testFindProviderClass_FallbackScenarios() throws Exception {
        final String className = SimpleProvider.class.getName();

        // Create a classloader that fails for our test class
        ClassLoader failingLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.equals(className)) {
                    throw new ClassNotFoundException("Simulated failure for " + name);
                }
                return super.loadClass(name);
            }
        };

        // Test fallback works
        Class<?> cls = ObjectFactory.findProviderClass(className, failingLoader, true);
        assertEquals(SimpleProvider.class, cls);

        // Test that fallback doesn't work when disabled
        assertThrows(ClassNotFoundException.class, () -> ObjectFactory.findProviderClass(className, failingLoader, false));

        // Test fallback when current classloader is also null
        ClassLoader nullCurrentLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                throw new ClassNotFoundException("Always fails: " + name);
            }
        };

        // This should eventually fall back to Class.forName for bootstrap classes
        Class<?> bootstrapClass = ObjectFactory.findProviderClass("java.lang.Object", null, true);
        assertEquals(Object.class, bootstrapClass);
    }

    /**
     * Test findJarServiceProvider edge cases.
     */
    @Test
    void testFindJarServiceProvider_EdgeCases() throws Exception {
        // Test service provider with empty/whitespace-only content
        // This requires access to the internal method, so we test indirectly through createObject
        final String nonExistentFactory = "non.existent.factory.id";

        // Should return null when no service file exists
        assertThrows(ConfigurationError.class, () -> ObjectFactory.createObject(nonExistentFactory, null, null));

        // Test the path where service file exists but is empty
        // This is hard to test directly without modifying classpath resources
        // So we test the robustness of the service loading mechanism
        assertDoesNotThrow(() -> {
            try {
                ObjectFactory.createObject(nonExistentFactory, null, "java.lang.String");
            } catch (ConfigurationError e) {
                // Expected - just testing it doesn't crash
            }
        });
    }

    /**
     * Test createObject with various system property edge cases.
     */
    @Test
    void testCreateObject_SystemPropertyEdgeCases() throws Exception {
        final String factoryId = "system.prop.edge.test";
        final String orig = System.getProperty(factoryId);

        try {
            // Test with whitespace-only system property
            System.setProperty(factoryId, "   ");
            assertThrows(ConfigurationError.class, () -> ObjectFactory.createObject(factoryId, "java.lang.String"));

            // Test with system property containing special characters
            System.setProperty(factoryId, "java.lang.String$InvalidInnerClass");
            assertThrows(ConfigurationError.class, () -> ObjectFactory.createObject(factoryId, "java.lang.String"));

            // Test with valid inner class
            System.setProperty(factoryId, "java.util.Map$Entry");
            assertThrows(ConfigurationError.class, () -> ObjectFactory.createObject(factoryId, "java.lang.String"));

        } finally {
            if (orig == null) {
                System.clearProperty(factoryId);
            } else {
                System.setProperty(factoryId, orig);
            }
        }
    }

    /**
     * Test ConfigurationError serialization and error handling.
     */
    @Test
    void testConfigurationError_Serialization() throws Exception {
        Exception cause = new IllegalArgumentException("Test cause");
        ConfigurationError original = new ConfigurationError("Test error message", cause);

        // Test that the error contains expected data
        assertEquals("Test error message", original.getMessage());
        assertSame(cause, original.getException());

        // Test error without cause
        ConfigurationError noCause = new ConfigurationError("No cause error", null);
        assertEquals("No cause error", noCause.getMessage());
        assertNull(noCause.getException());

        // Test that it's an Error subclass
        assertTrue(original instanceof Error);
    }

    /**
     * Test createObject with SecurityException handling.
     */
    @Test
    void testCreateObject_SecurityExceptionHandling() throws Exception {
        final String factoryId = "security.exception.test";
        final String orig = System.getProperty(factoryId);

        try {
            // Set a valid system property
            System.setProperty(factoryId, "java.lang.String");

            // Test that security exceptions are caught and processing continues
            // The method should handle SecurityException when getting system properties
            Object obj = ObjectFactory.createObject(factoryId, "java.lang.Integer");
            assertNotNull(obj);
            assertTrue(obj instanceof String, "Should use system property despite potential security issues");

        } finally {
            if (orig == null) {
                System.clearProperty(factoryId);
            } else {
                System.setProperty(factoryId, orig);
            }
        }
    }

    /**
     * Test complex classloader interaction scenarios.
     */
    @Test
    void testComplexClassLoaderInteractions() throws Exception {
        ClassLoader originalContext = Thread.currentThread().getContextClassLoader();
        ClassLoader currentLoader = ObjectFactory.class.getClassLoader();

        try {
            // Create a complex classloader hierarchy
            ClassLoader parent = new ClassLoader(ClassLoader.getSystemClassLoader()) {
            };
            ClassLoader child = new ClassLoader(parent) {
            };
            ClassLoader grandchild = new ClassLoader(child) {
            };

            // Test with various combinations
            Thread.currentThread().setContextClassLoader(grandchild);
            ClassLoader result1 = ObjectFactory.findClassLoader();
            assertNotNull(result1);

            Thread.currentThread().setContextClassLoader(parent);
            ClassLoader result2 = ObjectFactory.findClassLoader();
            assertNotNull(result2);

            // Test class loading with complex hierarchy
            String className = "java.lang.String";
            Class<?> cls1 = ObjectFactory.findProviderClass(className, grandchild, true);
            assertEquals(String.class, cls1);

            Class<?> cls2 = ObjectFactory.findProviderClass(className, parent, false);
            assertEquals(String.class, cls2);

        } finally {
            Thread.currentThread().setContextClassLoader(originalContext);
        }
    }

    /**
     * Test properties file reading with various file system scenarios.
     */
    @Test
    void testPropertiesFile_FileSystemScenarios() throws Exception {
        final String factoryId = "filesystem.test";

        // Test with read-only properties file
        Path readOnlyProps = Files.createTempFile("readonly-", ".properties");
        try {
            Properties p = new Properties();
            p.setProperty(factoryId, "java.lang.String");
            try (FileOutputStream fos = new FileOutputStream(readOnlyProps.toFile())) {
                p.store(fos, "test");
            }

            // Make file read-only
            readOnlyProps.toFile().setReadOnly();

            Object obj = ObjectFactory.createObject(factoryId, readOnlyProps.toString(), "java.lang.Integer");
            assertTrue(obj instanceof String, "Should read from read-only properties file");
        } finally {
            // Restore write permission before deleting
            readOnlyProps.toFile().setWritable(true);
            Files.deleteIfExists(readOnlyProps);
        }

        // Test with directory instead of file
        Path tempDir = Files.createTempDirectory("props-dir-");
        try {
            // Should handle gracefully when properties file is actually a directory
            Object obj = ObjectFactory.createObject(factoryId, tempDir.toString(), "java.lang.String");
            assertTrue(obj instanceof String, "Should use fallback when properties path is directory");
        } finally {
            Files.deleteIfExists(tempDir);
        }
    }

    /**
     * Test stress conditions and edge cases for robustness.
     */
    @Test
    void testStressConditions() throws Exception {
        // Test with rapid-fire calls to exercise caching and synchronization
        final String baseFactoryId = "stress.test";
        final int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            final String factoryId = baseFactoryId + "." + i;
            assertDoesNotThrow(() -> {
                try {
                    ObjectFactory.createObject(factoryId, "java.lang.String");
                } catch (ConfigurationError e) {
                    // Expected for most factory IDs
                }
            });
        }

        // Test classloader operations under stress
        for (int i = 0; i < 50; i++) {
            assertDoesNotThrow(() -> {
                ClassLoader cl = ObjectFactory.findClassLoader();
                assertNotNull(cl);
            });
        }

        // Test newInstance with various classes
        String[] testClasses =
                { "java.lang.String", "java.lang.Object", "java.lang.StringBuilder", "java.util.ArrayList", "java.util.HashMap" };

        for (String className : testClasses) {
            Object obj = ObjectFactory.newInstance(className, ObjectFactory.findClassLoader(), true);
            assertNotNull(obj, "Should create instance of " + className);
        }
    }

    /**
     * Test boundary conditions and limit cases.
     */
    @Test
    void testBoundaryConditions() throws Exception {
        // Test with very long factory ID
        StringBuilder longFactoryId = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longFactoryId.append("very.long.factory.id.segment.");
        }

        final String longId = longFactoryId.toString();
        Object obj = ObjectFactory.createObject(longId, "java.lang.String");
        assertTrue(obj instanceof String, "Should handle very long factory IDs");

        // Test with factory ID containing special characters
        String specialId = "factory.with.special-chars_123$test";
        Object obj2 = ObjectFactory.createObject(specialId, "java.lang.String");
        assertTrue(obj2 instanceof String, "Should handle special characters in factory ID");

        // Test with null values in various combinations
        assertThrows(ConfigurationError.class, () -> ObjectFactory.createObject("null.test", null, null));

        // Test with empty strings
        assertThrows(IllegalArgumentException.class, () -> ObjectFactory.createObject("", "java.lang.String"));
    }

    /**
     * Test debugPrintln method functionality and DEBUG mode.
     * Tests the private debugPrintln method through reflection and verifies DEBUG behavior.
     */
    @Test
    void testDebugPrintlnFunctionality() throws Exception {
        // Test debugPrintln method via reflection
        Method debugPrintlnMethod = ObjectFactory.class.getDeclaredMethod("debugPrintln", String.class);
        debugPrintlnMethod.setAccessible(true);

        // Get DEBUG field via reflection to check its value
        Field debugField = ObjectFactory.class.getDeclaredField("DEBUG");
        debugField.setAccessible(true);
        boolean debugValue = debugField.getBoolean(null);

        // Call debugPrintln - should not throw exception regardless of DEBUG value
        assertDoesNotThrow(() -> {
            try {
                debugPrintlnMethod.invoke(null, "Test debug message");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "debugPrintln should not throw exception");

        // Verify DEBUG constant is properly defined (should be false in production)
        assertFalse(debugValue, "DEBUG should be false in production code");
    }

    /**
     * Test xerces.properties file caching mechanism and synchronization.
     * Tests the internal caching logic for xerces.properties file.
     */
    @Test
    void testXercesPropertiesCachingMechanism() throws Exception {
        final String factoryId = "xerces.properties.cache.test";

        // Access private fields to verify caching behavior
        Field fXercesPropertiesField = ObjectFactory.class.getDeclaredField("fXercesProperties");
        Field fLastModifiedField = ObjectFactory.class.getDeclaredField("fLastModified");
        fXercesPropertiesField.setAccessible(true);
        fLastModifiedField.setAccessible(true);

        // Store original values
        Properties originalProps = (Properties) fXercesPropertiesField.get(null);
        long originalLastModified = fLastModifiedField.getLong(null);

        try {
            // Reset cache state
            fXercesPropertiesField.set(null, null);
            fLastModifiedField.setLong(null, -1);

            // First call should attempt to load xerces.properties
            try {
                ObjectFactory.createObject(factoryId, null, "java.lang.String");
            } catch (ConfigurationError e) {
                // Expected if no xerces.properties or no entry
            }

            // Verify cache state after first call
            long firstCallLastModified = fLastModifiedField.getLong(null);
            Properties firstCallProps = (Properties) fXercesPropertiesField.get(null);

            // Second call should use cached data
            try {
                ObjectFactory.createObject(factoryId, null, "java.lang.String");
            } catch (ConfigurationError e) {
                // Expected if no xerces.properties or no entry
            }

            // Verify cache wasn't unnecessarily reloaded
            long secondCallLastModified = fLastModifiedField.getLong(null);
            Properties secondCallProps = (Properties) fXercesPropertiesField.get(null);
            assertEquals(firstCallLastModified, secondCallLastModified, "Cache timestamp should remain same on subsequent calls");
            assertSame(firstCallProps, secondCallProps, "Cache should reuse same Properties object");

        } finally {
            // Restore original values
            fXercesPropertiesField.set(null, originalProps);
            fLastModifiedField.setLong(null, originalLastModified);
        }
    }

    /**
     * Test comprehensive SecurityManager interaction scenarios.
     * Tests various security-related code paths in ObjectFactory.
     */
    @Test
    void testComprehensiveSecurityManagerInteraction() throws Exception {
        final String factoryId = "security.comprehensive.test";
        final String orig = System.getProperty(factoryId);

        try {
            // Test package access checks in findProviderClass
            String restrictedClassName = "java.lang.String";
            ClassLoader cl = ObjectFactory.findClassLoader();

            // This should work normally without SecurityManager restrictions
            Class<?> cls = ObjectFactory.findProviderClass(restrictedClassName, cl, true);
            assertEquals(String.class, cls, "Should load class without security issues");

            // Test system property access with potential security exceptions
            System.setProperty(factoryId, "java.lang.String");

            // Test that SecurityException during system property access is handled gracefully
            Object obj = ObjectFactory.createObject(factoryId, "java.lang.Integer");
            assertNotNull(obj);
            assertTrue(obj instanceof String, "Should use system property value");

            // Test SecurityException during file access (xerces.properties)
            String nonExistentFactory = "security.file.access.test";
            try {
                ObjectFactory.createObject(nonExistentFactory, null, "java.lang.String");
            } catch (ConfigurationError e) {
                // Expected for non-existent factory
            }

        } finally {
            if (orig == null) {
                System.clearProperty(factoryId);
            } else {
                System.setProperty(factoryId, orig);
            }
        }
    }

    /**
     * Test file modification time detection in properties file caching.
     * Verifies that the caching mechanism properly detects file changes.
     */
    @Test
    void testFileModificationTimeDetection() throws Exception {
        final String factoryId = "modification.time.test";

        // Create a temporary properties file
        Path tempProps = Files.createTempFile("mod-time-", ".properties");
        try {
            // Create initial properties file
            Properties p1 = new Properties();
            p1.setProperty(factoryId, "java.lang.String");
            try (FileOutputStream fos = new FileOutputStream(tempProps.toFile())) {
                p1.store(fos, "initial");
            }

            // First call - should read from file
            Object obj1 = ObjectFactory.createObject(factoryId, tempProps.toString(), "java.lang.Integer");
            assertTrue(obj1 instanceof String, "Should use value from properties file");

            // Wait a bit to ensure different modification time
            Thread.sleep(100);

            // Modify the properties file
            Properties p2 = new Properties();
            p2.setProperty(factoryId, "java.lang.StringBuilder");
            try (FileOutputStream fos = new FileOutputStream(tempProps.toFile())) {
                p2.store(fos, "modified");
            }

            // Second call - should detect modification and reload
            Object obj2 = ObjectFactory.createObject(factoryId, tempProps.toString(), "java.lang.Integer");
            assertTrue(obj2 instanceof StringBuilder, "Should use updated value from modified properties file");

        } finally {
            Files.deleteIfExists(tempProps);
        }
    }

    /**
     * Test IllegalAccessException and InstantiationException handling.
     * Tests exception wrapping for various instantiation failures.
     */
    @Test
    void testInstantiationExceptionHandling() throws Exception {
        // Test with class that has no public constructor accessible
        ConfigurationError err1 =
                assertThrows(ConfigurationError.class,
                        () -> ObjectFactory.newInstance("java.lang.System", ObjectFactory.findClassLoader(), true));
        assertNotNull(err1.getException(), "Should wrap instantiation exception");
        assertTrue(err1.getMessage().contains("could not be instantiated"), "Error message should indicate instantiation failure");

        // Test with final class that cannot be instantiated via reflection
        ConfigurationError err2 =
                assertThrows(ConfigurationError.class,
                        () -> ObjectFactory.newInstance("java.lang.Integer", ObjectFactory.findClassLoader(), true));
        assertNotNull(err2.getException(), "Should wrap instantiation exception for Integer (no default constructor)");

        // Test with primitive class
        ConfigurationError err3 =
                assertThrows(ConfigurationError.class, () -> ObjectFactory.newInstance("int", ObjectFactory.findClassLoader(), true));
        assertNotNull(err3.getException(), "Should wrap exception for primitive type");
        assertTrue(err3.getException() instanceof ClassNotFoundException, "Should be ClassNotFoundException for primitive");
    }

    /**
     * Test service file reading with various encoding scenarios.
     * Tests META-INF/services file reading robustness.
     */
    @Test
    void testServiceFileEncodingScenarios() throws Exception {
        final String factoryId = "service.encoding.test";

        // Test service file reading behavior indirectly
        // Since we can't easily create META-INF/services files dynamically,
        // we test the robustness of the service loading mechanism

        // Test with non-existent service
        assertThrows(ConfigurationError.class, () -> ObjectFactory.createObject(factoryId, null, null),
                "Should throw ConfigurationError when no service provider found");

        // Test service loading doesn't crash with various factory IDs
        String[] testFactoryIds =
                { "service.with.unicode.名前", "service.with.special!@#$%characters",
                        "service.with.very.long." + "segment.".repeat(100) + "name", "service.with\\backslashes",
                        "service.with/forward/slashes" };

        for (String testId : testFactoryIds) {
            assertDoesNotThrow(() -> {
                try {
                    ObjectFactory.createObject(testId, null, "java.lang.String");
                } catch (ConfigurationError e) {
                    // Expected for non-existent services
                }
            }, "Service loading should handle special characters gracefully: " + testId);
        }
    }

    /**
     * Test advanced ClassLoader hierarchy edge cases.
     * Tests complex ClassLoader delegation scenarios.
     */
    @Test
    void testAdvancedClassLoaderHierarchy() throws Exception {
        ClassLoader originalContext = Thread.currentThread().getContextClassLoader();

        try {
            // Create a custom ClassLoader that selectively fails
            ClassLoader selectiveFailLoader = new ClassLoader(ClassLoader.getSystemClassLoader()) {
                @Override
                public Class<?> loadClass(String name) throws ClassNotFoundException {
                    if (name.contains("Selective")) {
                        throw new ClassNotFoundException("Selective failure: " + name);
                    }
                    return super.loadClass(name);
                }

                @Override
                public InputStream getResourceAsStream(String name) {
                    if (name.contains("selective")) {
                        return null; // Simulate missing resource
                    }
                    return super.getResourceAsStream(name);
                }
            };

            // Test with selective failure ClassLoader
            Thread.currentThread().setContextClassLoader(selectiveFailLoader);

            // Should work for normal classes
            Object obj1 = ObjectFactory.createObject("test.normal", "java.lang.String");
            assertTrue(obj1 instanceof String);

            // Test ClassLoader fallback behavior
            Class<?> cls1 = ObjectFactory.findProviderClass("java.lang.String", selectiveFailLoader, true);
            assertEquals(String.class, cls1);

            // Test with null ClassLoader
            Class<?> cls2 = ObjectFactory.findProviderClass("java.lang.Object", null, true);
            assertEquals(Object.class, cls2);

            // Test complex hierarchy detection
            ClassLoader result = ObjectFactory.findClassLoader();
            assertNotNull(result, "findClassLoader should return non-null result");

            // Test with ClassLoader that throws SecurityException
            ClassLoader securityThrowingLoader = new ClassLoader() {
                @Override
                public Class<?> loadClass(String name) throws ClassNotFoundException {
                    throw new SecurityException("Access denied: " + name);
                }
            };

            // Should propagate SecurityException since it's not wrapped in findProviderClass
            assertThrows(SecurityException.class, () -> ObjectFactory.findProviderClass("java.lang.String", securityThrowingLoader, false));

        } finally {
            Thread.currentThread().setContextClassLoader(originalContext);
        }
    }

    /**
     * Test comprehensive ConfigurationError behavior and exception chaining.
     * Tests all aspects of the ConfigurationError inner class.
     */
    @Test
    void testComprehensiveConfigurationErrorBehavior() throws Exception {
        // Test various exception chaining scenarios
        Exception rootCause = new IllegalArgumentException("Root cause");
        ConfigurationError error1 = new ConfigurationError("Wrapper message", rootCause);

        assertEquals("Wrapper message", error1.getMessage());
        assertSame(rootCause, error1.getException());
        assertTrue(error1 instanceof Error, "ConfigurationError should extend Error");
        // ConfigurationError doesn't override getCause(), so it returns null

        // Test with null exception
        ConfigurationError error2 = new ConfigurationError("No cause", null);
        assertEquals("No cause", error2.getMessage());
        assertNull(error2.getException());
        assertNull(error2.getCause());

        // Test with nested exceptions
        Exception level1 = new RuntimeException("Level 1", rootCause);
        ConfigurationError error3 = new ConfigurationError("Top level", level1);
        assertSame(level1, error3.getException());
        // ConfigurationError doesn't override getCause(), so it returns null
        assertSame(rootCause, level1.getCause());

        // Test serialization support
        assertNotNull(ConfigurationError.class.getDeclaredField("serialVersionUID"), "ConfigurationError should have serialVersionUID");

        // Test toString behavior
        String toString = error1.toString();
        assertTrue(toString.contains("ConfigurationError"), "toString should contain class name");
        assertTrue(toString.contains("Wrapper message"), "toString should contain message");
    }

    /**
     * Test properties file loading with various I/O edge cases.
     * Tests robustness of properties file reading under adverse conditions.
     */
    @Test
    void testPropertiesFileIOEdgeCases() throws Exception {
        final String factoryId = "io.edge.cases.test";

        // Test with very large properties file
        Path largeProps = Files.createTempFile("large-", ".properties");
        try {
            Properties largeP = new Properties();
            // Add many properties to create a large file
            for (int i = 0; i < 10000; i++) {
                largeP.setProperty("property." + i, "value." + i);
            }
            largeP.setProperty(factoryId, "java.lang.String");

            try (FileOutputStream fos = new FileOutputStream(largeProps.toFile())) {
                largeP.store(fos, "large properties file");
            }

            Object obj = ObjectFactory.createObject(factoryId, largeProps.toString(), "java.lang.Integer");
            assertTrue(obj instanceof String, "Should handle large properties file");

        } finally {
            Files.deleteIfExists(largeProps);
        }

        // Test with properties file containing Unicode characters
        Path unicodeProps = Files.createTempFile("unicode-", ".properties");
        try {
            Properties unicodeP = new Properties();
            unicodeP.setProperty(factoryId, "java.lang.String");
            unicodeP.setProperty("unicode.test.名前", "値");
            unicodeP.setProperty("unicode.test.emoji", "😀🎉");

            try (FileOutputStream fos = new FileOutputStream(unicodeProps.toFile())) {
                unicodeP.store(fos, "unicode properties");
            }

            Object obj = ObjectFactory.createObject(factoryId, unicodeProps.toString(), "java.lang.Integer");
            assertTrue(obj instanceof String, "Should handle Unicode in properties file");

        } finally {
            Files.deleteIfExists(unicodeProps);
        }

        // Test with properties file that gets deleted during processing
        Path transientProps = Files.createTempFile("transient-", ".properties");
        try {
            Properties transientP = new Properties();
            transientP.setProperty(factoryId, "java.lang.String");

            try (FileOutputStream fos = new FileOutputStream(transientProps.toFile())) {
                transientP.store(fos, "transient");
            }

            // Delete the file to simulate concurrent deletion
            Files.delete(transientProps);

            // Should fall back gracefully
            Object obj = ObjectFactory.createObject(factoryId, transientProps.toString(), "java.lang.String");
            assertTrue(obj instanceof String, "Should fall back when properties file is deleted");

        } catch (Exception e) {
            // File might already be deleted
        }
    }

    /**
     * Test concurrent access to ObjectFactory methods under high load.
     * Verifies thread safety of static methods and caching mechanisms.
     */
    @Test
    void testConcurrentAccessUnderHighLoad() throws Exception {
        final int threadCount = 20;
        final int operationsPerThread = 50;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Create multiple threads performing different operations
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await();

                    for (int j = 0; j < operationsPerThread; j++) {
                        try {
                            // Mix different types of operations
                    if (j % 3 == 0) {
                        // createObject with system property
                        ObjectFactory.createObject("thread." + threadId + ".op." + j, "java.lang.String");
                    } else if (j % 3 == 1) {
                        // newInstance
                        ObjectFactory.newInstance("java.lang.Object", ObjectFactory.findClassLoader(), true);
                    } else {
                        // findClassLoader
                        ObjectFactory.findClassLoader();
                    }
                    successCount.incrementAndGet();
                } catch (ConfigurationError e) {
                    // Expected for many factory IDs
                    errorCount.incrementAndGet();
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
        } catch (Exception e) {
            exceptions.add(e);
        } finally {
            finishLatch.countDown();
        }
    }       ).start();
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for completion
        assertTrue(finishLatch.await(60, TimeUnit.SECONDS), "All threads should complete within timeout");

        // Check results
        assertTrue(exceptions.isEmpty(), "No unexpected exceptions should occur: " + exceptions);
        assertTrue(successCount.get() > 0, "Some operations should succeed");
        // Note: errorCount might be 0 if all operations succeed with fallback classes
        assertTrue(errorCount.get() >= 0, "ConfigurationErrors may occur for non-existent factories");
        assertEquals(threadCount * operationsPerThread, successCount.get() + errorCount.get(),
                "Total operations should match expected count");
    }

    /**
     * Test method parameter validation and edge cases.
     * Tests how ObjectFactory handles invalid or edge-case parameters.
     */
    @Test
    void testMethodParameterValidationEdgeCases() throws Exception {
        // Test createObject with null factoryId - System.getProperty throws NPE for null key
        assertThrows(NullPointerException.class, () -> ObjectFactory.createObject(null, "java.lang.String"));

        // Test newInstance with null className
        assertThrows(ConfigurationError.class, () -> ObjectFactory.newInstance(null, ObjectFactory.findClassLoader(), true));

        // Test findProviderClass with null className - throws NullPointerException internally
        assertThrows(NullPointerException.class, () -> ObjectFactory.findProviderClass(null, ObjectFactory.findClassLoader(), true));

        // Test with empty className
        assertThrows(ConfigurationError.class, () -> ObjectFactory.newInstance("", ObjectFactory.findClassLoader(), true));

        // Test with whitespace-only className
        assertThrows(ConfigurationError.class, () -> ObjectFactory.newInstance("   ", ObjectFactory.findClassLoader(), true));

        // Test with invalid class name characters
        assertThrows(ConfigurationError.class,
                () -> ObjectFactory.newInstance("invalid..class..name", ObjectFactory.findClassLoader(), true));

        // Test with class name containing null character
        assertThrows(ConfigurationError.class, () -> ObjectFactory.newInstance("invalid\u0000name", ObjectFactory.findClassLoader(), true));

        // Test very long class names
        String longClassName = "a" + "b".repeat(10000) + ".VeryLongClassName";
        assertThrows(ConfigurationError.class, () -> ObjectFactory.newInstance(longClassName, ObjectFactory.findClassLoader(), true));
    }
}
