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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.xerces.xni.parser.XMLComponentManager;
import org.apache.xerces.xni.parser.XMLConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HTMLComponentTest {

    private HTMLComponent htmlComponent;

    @Mock
    private XMLComponentManager componentManager;

    @BeforeEach
    void setUp() {
        // Create a test implementation of HTMLComponent
        htmlComponent = new TestHTMLComponent();
    }

    @Test
    @DisplayName("Should return feature default value when feature exists")
    void testGetFeatureDefault_ExistingFeature() {
        // When
        Boolean result = htmlComponent.getFeatureDefault("test.feature.enabled");

        // Then
        assertNotNull(result);
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return null when feature does not exist")
    void testGetFeatureDefault_NonExistingFeature() {
        // When
        Boolean result = htmlComponent.getFeatureDefault("non.existing.feature");

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should return null for empty feature ID")
    void testGetFeatureDefault_EmptyFeatureId() {
        // When
        Boolean result = htmlComponent.getFeatureDefault("");

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should return null for null feature ID")
    void testGetFeatureDefault_NullFeatureId() {
        // When
        Boolean result = htmlComponent.getFeatureDefault(null);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should return property default value when property exists")
    void testGetPropertyDefault_ExistingProperty() {
        // When
        Object result = htmlComponent.getPropertyDefault("test.property.value");

        // Then
        assertNotNull(result);
        assertEquals("default-value", result);
    }

    @Test
    @DisplayName("Should return null when property does not exist")
    void testGetPropertyDefault_NonExistingProperty() {
        // When
        Object result = htmlComponent.getPropertyDefault("non.existing.property");

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should return null for empty property ID")
    void testGetPropertyDefault_EmptyPropertyId() {
        // When
        Object result = htmlComponent.getPropertyDefault("");

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should return null for null property ID")
    void testGetPropertyDefault_NullPropertyId() {
        // When
        Object result = htmlComponent.getPropertyDefault(null);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should handle different property value types")
    void testGetPropertyDefault_DifferentValueTypes() {
        // Test Integer property
        Object intResult = htmlComponent.getPropertyDefault("test.property.integer");
        assertNotNull(intResult);
        assertEquals(42, intResult);

        // Test String array property
        Object arrayResult = htmlComponent.getPropertyDefault("test.property.array");
        assertNotNull(arrayResult);
        assertInstanceOf(String[].class, arrayResult);
        String[] array = (String[]) arrayResult;
        assertEquals(2, array.length);
        assertEquals("item1", array[0]);
        assertEquals("item2", array[1]);
    }

    @Test
    @DisplayName("Should properly implement XMLComponent interface methods")
    void testXMLComponentMethods() throws XMLConfigurationException {
        // Test reset method
        assertDoesNotThrow(() -> htmlComponent.reset(componentManager));

        // Test getRecognizedFeatures
        String[] features = htmlComponent.getRecognizedFeatures();
        assertNotNull(features);
        assertEquals(2, features.length);
        assertEquals("test.feature.enabled", features[0]);
        assertEquals("test.feature.disabled", features[1]);

        // Test getRecognizedProperties
        String[] properties = htmlComponent.getRecognizedProperties();
        assertNotNull(properties);
        assertEquals(3, properties.length);
        assertEquals("test.property.value", properties[0]);
        assertEquals("test.property.integer", properties[1]);
        assertEquals("test.property.array", properties[2]);

        // Test setFeature
        assertDoesNotThrow(() -> htmlComponent.setFeature("test.feature.enabled", false));

        // Test setProperty
        assertDoesNotThrow(() -> htmlComponent.setProperty("test.property.value", "new-value"));
    }

    @Test
    @DisplayName("Should verify interface contract with mock implementation")
    void testInterfaceContractWithMock() {
        // Create mock implementation
        HTMLComponent mockComponent = mock(HTMLComponent.class);

        // Setup behavior
        when(mockComponent.getFeatureDefault("mock.feature")).thenReturn(Boolean.TRUE);
        when(mockComponent.getPropertyDefault("mock.property")).thenReturn("mock-value");

        // Verify behavior
        assertEquals(Boolean.TRUE, mockComponent.getFeatureDefault("mock.feature"));
        assertEquals("mock-value", mockComponent.getPropertyDefault("mock.property"));

        // Verify interactions
        verify(mockComponent, times(1)).getFeatureDefault("mock.feature");
        verify(mockComponent, times(1)).getPropertyDefault("mock.property");
    }

    /**
     * Test implementation of HTMLComponent interface for testing purposes
     */
    private static class TestHTMLComponent implements HTMLComponent {

        @Override
        public Boolean getFeatureDefault(String featureId) {
            if ("test.feature.enabled".equals(featureId)) {
                return Boolean.TRUE;
            }
            if ("test.feature.disabled".equals(featureId)) {
                return Boolean.FALSE;
            }
            return null;
        }

        @Override
        public Object getPropertyDefault(String propertyId) {
            if ("test.property.value".equals(propertyId)) {
                return "default-value";
            }
            if ("test.property.integer".equals(propertyId)) {
                return 42;
            }
            if ("test.property.array".equals(propertyId)) {
                return new String[] { "item1", "item2" };
            }
            return null;
        }

        @Override
        public void reset(XMLComponentManager componentManager) throws XMLConfigurationException {
            // Test implementation - no-op
        }

        @Override
        public String[] getRecognizedFeatures() {
            return new String[] { "test.feature.enabled", "test.feature.disabled" };
        }

        @Override
        public void setFeature(String featureId, boolean state) throws XMLConfigurationException {
            // Test implementation - no-op
        }

        @Override
        public String[] getRecognizedProperties() {
            return new String[] { "test.property.value", "test.property.integer", "test.property.array" };
        }

        @Override
        public void setProperty(String propertyId, Object value) throws XMLConfigurationException {
            // Test implementation - no-op
        }
    }
}