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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link ObjectFactory}.
 *
 * @author CodeLibs Project
 */
public class ObjectFactoryTest {

    private String originalSystemProperty;
    private static final String TEST_FACTORY_ID = "test.factory.id";

    @BeforeEach
    public void setUp() {
        // Save original system property if exists
        originalSystemProperty = System.getProperty(TEST_FACTORY_ID);
    }

    @AfterEach
    public void tearDown() {
        // Restore original system property
        if (originalSystemProperty != null) {
            System.setProperty(TEST_FACTORY_ID, originalSystemProperty);
        } else {
            System.clearProperty(TEST_FACTORY_ID);
        }
    }

    @Test
    public void testCreateObjectWithFallbackClass() {
        // Given: Factory ID with no system property set
        final String factoryId = TEST_FACTORY_ID;
        final String fallbackClassName = "java.lang.String";

        // When: Creating object with fallback
        final Object result = ObjectFactory.createObject(factoryId, fallbackClassName);

        // Then: Should return instance of fallback class
        assertNotNull(result, "Created object should not be null");
        assertTrue(result instanceof String, "Should be instance of fallback class");
    }

    @Test
    public void testCreateObjectWithSystemProperty() {
        // Given: System property set to a valid class
        final String factoryId = TEST_FACTORY_ID;
        final String className = "java.lang.StringBuilder";
        System.setProperty(factoryId, className);

        // When: Creating object
        final Object result = ObjectFactory.createObject(factoryId, "java.lang.String");

        // Then: Should use system property instead of fallback
        assertNotNull(result, "Created object should not be null");
        assertTrue(result instanceof StringBuilder, "Should be instance of system property class");
    }

    @Test
    public void testCreateObjectWithNullFallback() {
        // Given: Non-existent factory ID with null fallback
        final String factoryId = "non.existent.factory";

        // When/Then: Should throw ConfigurationError
        assertThrows(ObjectFactory.ConfigurationError.class, () -> {
            ObjectFactory.createObject(factoryId, (String) null);
        }, "Should throw ConfigurationError when no fallback provided");
    }

    @Test
    public void testCreateObjectWithInvalidFallbackClass() {
        // Given: Invalid class name as fallback
        final String factoryId = TEST_FACTORY_ID;
        final String fallbackClassName = "non.existent.Class";

        // When/Then: Should throw ConfigurationError
        assertThrows(ObjectFactory.ConfigurationError.class, () -> {
            ObjectFactory.createObject(factoryId, fallbackClassName);
        }, "Should throw ConfigurationError for invalid class");
    }

    @Test
    public void testCreateObjectWithThreeParameters() {
        // Given: Factory ID, properties filename (null), and fallback
        final String factoryId = TEST_FACTORY_ID;
        final String fallbackClassName = "java.lang.String";

        // When: Creating object with three parameters
        final Object result = ObjectFactory.createObject(factoryId, null, fallbackClassName);

        // Then: Should return instance of fallback class
        assertNotNull(result, "Created object should not be null");
        assertTrue(result instanceof String, "Should be instance of fallback class");
    }

    @Test
    public void testFindClassLoader() {
        // When: Finding class loader
        final ClassLoader cl = ObjectFactory.findClassLoader();

        // Then: Should return a valid class loader
        assertNotNull(cl, "Class loader should not be null");
    }

    @Test
    public void testNewInstanceValidClass() {
        // Given: Valid class name and class loader
        final String className = "java.lang.String";
        final ClassLoader cl = ObjectFactory.findClassLoader();

        // When: Creating new instance
        final Object result = ObjectFactory.newInstance(className, cl, true);

        // Then: Should create instance
        assertNotNull(result, "Instance should not be null");
        assertTrue(result instanceof String, "Should be String instance");
    }

    @Test
    public void testNewInstanceInvalidClass() {
        // Given: Invalid class name
        final String className = "invalid.class.Name";
        final ClassLoader cl = ObjectFactory.findClassLoader();

        // When/Then: Should throw ConfigurationError
        assertThrows(ObjectFactory.ConfigurationError.class, () -> {
            ObjectFactory.newInstance(className, cl, true);
        }, "Should throw ConfigurationError for invalid class");
    }

    @Test
    public void testNewInstanceWithNullClassLoader() {
        // Given: Null class loader
        final String className = "java.lang.String";

        // When: Creating instance with null class loader
        final Object result = ObjectFactory.newInstance(className, null, true);

        // Then: Should still create instance using bootstrap class loader
        assertNotNull(result, "Instance should not be null");
        assertTrue(result instanceof String, "Should be String instance");
    }

    @Test
    public void testFindProviderClassValidClass() throws Exception {
        // Given: Valid class name and class loader
        final String className = "java.lang.String";
        final ClassLoader cl = ObjectFactory.findClassLoader();

        // When: Finding provider class
        final Class<?> providerClass = ObjectFactory.findProviderClass(className, cl, true);

        // Then: Should find the class
        assertNotNull(providerClass, "Provider class should not be null");
        assertEquals(String.class, providerClass, "Should be String class");
    }

    @Test
    public void testFindProviderClassInvalidClass() {
        // Given: Invalid class name
        final String className = "invalid.class.Name";
        final ClassLoader cl = ObjectFactory.findClassLoader();

        // When/Then: Should throw ClassNotFoundException
        assertThrows(ClassNotFoundException.class, () -> {
            ObjectFactory.findProviderClass(className, cl, false);
        }, "Should throw ClassNotFoundException for invalid class");
    }

    @Test
    public void testFindProviderClassWithFallback() throws Exception {
        // Given: Invalid class name but with fallback enabled
        final String className = "java.lang.String";
        final ClassLoader cl = ObjectFactory.findClassLoader();

        // When: Finding provider class with fallback
        final Class<?> providerClass = ObjectFactory.findProviderClass(className, cl, true);

        // Then: Should find the class
        assertNotNull(providerClass, "Provider class should not be null");
        assertEquals(String.class, providerClass, "Should be String class");
    }

    @Test
    public void testFindProviderClassWithNullClassLoader() throws Exception {
        // Given: Null class loader
        final String className = "java.lang.String";

        // When: Finding provider class with null class loader
        final Class<?> providerClass = ObjectFactory.findProviderClass(className, null, true);

        // Then: Should find the class using bootstrap class loader
        assertNotNull(providerClass, "Provider class should not be null");
        assertEquals(String.class, providerClass, "Should be String class");
    }

    @Test
    public void testConfigurationErrorCreation() {
        // Given: Error message and exception
        final String message = "Test error message";
        final Exception cause = new Exception("Cause exception");

        // When: Creating ConfigurationError
        final ObjectFactory.ConfigurationError error = new ObjectFactory.ConfigurationError(message, cause);

        // Then: Should contain message and exception
        assertEquals(message, error.getMessage(), "Should have correct message");
        assertSame(cause, error.getException(), "Should have correct exception");
    }

    @Test
    public void testConfigurationErrorWithNullException() {
        // Given: Error message with null exception
        final String message = "Test error message";

        // When: Creating ConfigurationError with null exception
        final ObjectFactory.ConfigurationError error = new ObjectFactory.ConfigurationError(message, null);

        // Then: Should contain message and null exception
        assertEquals(message, error.getMessage(), "Should have correct message");
        assertNull(error.getException(), "Exception should be null");
    }

    @Test
    public void testCreateObjectWithAbstractClass() {
        // Given: Abstract class as fallback
        final String factoryId = TEST_FACTORY_ID;
        final String fallbackClassName = "java.util.AbstractList";

        // When/Then: Should throw ConfigurationError (cannot instantiate abstract class)
        assertThrows(ObjectFactory.ConfigurationError.class, () -> {
            ObjectFactory.createObject(factoryId, fallbackClassName);
        }, "Should throw ConfigurationError for abstract class");
    }

    @Test
    public void testCreateObjectWithInterface() {
        // Given: Interface as fallback
        final String factoryId = TEST_FACTORY_ID;
        final String fallbackClassName = "java.util.List";

        // When/Then: Should throw ConfigurationError (cannot instantiate interface)
        assertThrows(ObjectFactory.ConfigurationError.class, () -> {
            ObjectFactory.createObject(factoryId, fallbackClassName);
        }, "Should throw ConfigurationError for interface");
    }

    @Test
    public void testCreateObjectWithNoDefaultConstructor() {
        // Given: Class without default constructor
        final String factoryId = TEST_FACTORY_ID;
        final String fallbackClassName = "java.lang.Integer";

        // When/Then: Should throw ConfigurationError (Integer has no no-arg constructor)
        assertThrows(ObjectFactory.ConfigurationError.class, () -> {
            ObjectFactory.createObject(factoryId, fallbackClassName);
        }, "Should throw ConfigurationError for class without default constructor");
    }

    @Test
    public void testCreateObjectMultipleTimes() {
        // Given: Same factory ID and fallback
        final String factoryId = TEST_FACTORY_ID;
        final String fallbackClassName = "java.lang.String";

        // When: Creating object multiple times
        final Object result1 = ObjectFactory.createObject(factoryId, fallbackClassName);
        final Object result2 = ObjectFactory.createObject(factoryId, fallbackClassName);

        // Then: Should create different instances each time
        assertNotNull(result1, "First instance should not be null");
        assertNotNull(result2, "Second instance should not be null");
        assertNotSame(result1, result2, "Should create different instances");
        assertEquals(result1.getClass(), result2.getClass(), "Should be same class");
    }

    @Test
    public void testSystemPropertyOverridesEverything() {
        // Given: System property set
        final String factoryId = TEST_FACTORY_ID;
        System.setProperty(factoryId, "java.lang.StringBuilder");
        final String fallbackClassName = "java.lang.String";

        // When: Creating object
        final Object result = ObjectFactory.createObject(factoryId, fallbackClassName);

        // Then: System property should take precedence over fallback
        assertTrue(result instanceof StringBuilder, "System property should override fallback");
    }

    @Test
    public void testCreateObjectWithEmptySystemProperty() {
        // Given: Empty system property
        final String factoryId = TEST_FACTORY_ID;
        System.setProperty(factoryId, "");
        final String fallbackClassName = "java.lang.String";

        // When/Then: Creating object with empty system property should throw ConfigurationError
        assertThrows(ObjectFactory.ConfigurationError.class, () -> {
            ObjectFactory.createObject(factoryId, fallbackClassName);
        }, "Should throw ConfigurationError for empty class name");
    }

    @Test
    public void testFindClassLoaderConsistency() {
        // When: Finding class loader multiple times
        final ClassLoader cl1 = ObjectFactory.findClassLoader();
        final ClassLoader cl2 = ObjectFactory.findClassLoader();

        // Then: Should return consistent results
        assertNotNull(cl1, "First class loader should not be null");
        assertNotNull(cl2, "Second class loader should not be null");
        // Note: May or may not be the same instance depending on context
    }

    @Test
    public void testNewInstanceWithDoFallbackFalse() {
        // Given: Invalid class with doFallback false
        final String className = "invalid.class.Name";
        final ClassLoader cl = ObjectFactory.findClassLoader();

        // When/Then: Should throw ConfigurationError
        assertThrows(ObjectFactory.ConfigurationError.class, () -> {
            ObjectFactory.newInstance(className, cl, false);
        }, "Should throw ConfigurationError when doFallback is false");
    }

    @Test
    public void testConfigurationErrorIsError() {
        // Given: ConfigurationError
        final ObjectFactory.ConfigurationError error = new ObjectFactory.ConfigurationError("Test", null);

        // Then: Should be instance of Error
        assertTrue(error instanceof Error, "ConfigurationError should be an Error");
    }

} // class ObjectFactoryTest
