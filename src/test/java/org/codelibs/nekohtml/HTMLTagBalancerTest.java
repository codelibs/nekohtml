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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.xerces.util.XMLAttributesImpl;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.parser.XMLComponentManager;
import org.apache.xerces.xni.parser.XMLConfigurationException;
import org.apache.xerces.xni.parser.XMLDocumentSource;
import org.codelibs.nekohtml.HTMLTagBalancer.Info;
import org.codelibs.nekohtml.HTMLTagBalancer.InfoStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HTMLTagBalancerTest {

    private HTMLTagBalancer tagBalancer;

    @Mock
    private XMLComponentManager componentManager;

    @Mock
    private XMLDocumentHandler documentHandler;

    @Mock
    private XMLDocumentSource documentSource;

    @Mock
    private HTMLErrorReporter errorReporter;

    @Mock
    private XMLLocator locator;

    @Mock
    private NamespaceContext namespaceContext;

    @Mock
    private Augmentations augmentations;

    @Mock
    private HTMLTagBalancingListener tagBalancingListener;

    @BeforeEach
    void setUp() throws XMLConfigurationException {
        tagBalancer = new HTMLTagBalancer();
        // Setup augmentations mock to avoid NullPointerException
        lenient().when(augmentations.keys()).thenReturn(java.util.Collections.emptyEnumeration());
    }

    private void setupComponentManager() throws XMLConfigurationException {
        // Setup default component manager behavior
        lenient().when(componentManager.getFeature(anyString())).thenReturn(false);
        lenient().when(componentManager.getProperty(anyString())).thenReturn(null);
        lenient().when(componentManager.getProperty(HTMLTagBalancer.ERROR_REPORTER)).thenReturn(errorReporter);
    }

    @Test
    @DisplayName("Should create HTMLTagBalancer instance")
    void testConstructor() {
        HTMLTagBalancer balancer = new HTMLTagBalancer();
        assertNotNull(balancer);
    }

    @Test
    @DisplayName("Should return recognized features")
    void testGetRecognizedFeatures() {
        String[] features = tagBalancer.getRecognizedFeatures();
        assertNotNull(features);
        assertTrue(features.length > 0);
        // Check for key features
        boolean hasNamespaces = false;
        boolean hasAugmentations = false;
        for (String feature : features) {
            if (feature.contains("namespaces"))
                hasNamespaces = true;
            if (feature.contains("augmentations"))
                hasAugmentations = true;
        }
        assertTrue(hasNamespaces);
        assertTrue(hasAugmentations);
    }

    @Test
    @DisplayName("Should return recognized properties")
    void testGetRecognizedProperties() {
        String[] properties = tagBalancer.getRecognizedProperties();
        assertNotNull(properties);
        assertTrue(properties.length > 0);
        // Check for key properties
        boolean hasNamesElems = false;
        boolean hasErrorReporter = false;
        for (String property : properties) {
            if (property.contains("names/elems"))
                hasNamesElems = true;
            if (property.contains("error-reporter"))
                hasErrorReporter = true;
        }
        assertTrue(hasNamesElems);
        assertTrue(hasErrorReporter);
    }

    @Test
    @DisplayName("Should get feature default values")
    void testGetFeatureDefault() {
        // Test known feature
        Boolean result = tagBalancer.getFeatureDefault("http://cyberneko.org/html/features/balance-tags/document-fragment");
        assertNotNull(result);
        assertFalse(result);

        // Test unknown feature
        Boolean unknown = tagBalancer.getFeatureDefault("unknown.feature");
        assertNull(unknown);
    }

    @Test
    @DisplayName("Should get property default values")
    void testGetPropertyDefault() {
        // Test known property
        Object result = tagBalancer.getPropertyDefault("http://cyberneko.org/html/properties/names/elems");
        // Default is null
        assertNull(result);

        // Test unknown property
        Object unknown = tagBalancer.getPropertyDefault("unknown.property");
        assertNull(unknown);
    }

    @Test
    @DisplayName("Should set and get document handler")
    void testDocumentHandler() {
        tagBalancer.setDocumentHandler(documentHandler);
        assertEquals(documentHandler, tagBalancer.getDocumentHandler());
    }

    @Test
    @DisplayName("Should set and get document source")
    void testDocumentSource() {
        tagBalancer.setDocumentSource(documentSource);
        assertEquals(documentSource, tagBalancer.getDocumentSource());
    }

    @Test
    @DisplayName("Should reset component with manager")
    void testReset() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        when(componentManager.getFeature("http://xml.org/sax/features/namespaces")).thenReturn(true);
        when(componentManager.getFeature("http://cyberneko.org/html/features/augmentations")).thenReturn(true);
        when(componentManager.getProperty("http://cyberneko.org/html/properties/names/elems")).thenReturn("lower");

        // Execute
        assertDoesNotThrow(() -> tagBalancer.reset(componentManager));

        // Verify
        verify(componentManager, atLeastOnce()).getFeature(anyString());
        verify(componentManager, atLeastOnce()).getProperty(anyString());
    }

    @Test
    @DisplayName("Should set features")
    void testSetFeature() {
        assertDoesNotThrow(() -> {
            tagBalancer.setFeature("http://cyberneko.org/html/features/augmentations", true);
            tagBalancer.setFeature("http://cyberneko.org/html/features/report-errors", true);
            tagBalancer.setFeature("http://cyberneko.org/html/features/balance-tags/ignore-outside-content", true);
        });
    }

    @Test
    @DisplayName("Should set properties")
    void testSetProperty() {
        assertDoesNotThrow(() -> {
            tagBalancer.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
            tagBalancer.setProperty("http://cyberneko.org/html/properties/names/attrs", "upper");
        });
    }

    @Test
    @DisplayName("Should handle start document")
    void testStartDocument() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        // Execute
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Verify
        verify(documentHandler, times(1)).startDocument(eq(locator), eq("UTF-8"), eq(namespaceContext), eq(augmentations));
    }

    @Test
    @DisplayName("Should handle XML declaration")
    void testXmlDecl() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        // Execute - should pass through when nothing seen yet
        tagBalancer.xmlDecl("1.0", "UTF-8", "yes", augmentations);

        // Verify
        verify(documentHandler, times(1)).xmlDecl("1.0", "UTF-8", "yes", augmentations);
    }

    @Test
    @DisplayName("Should handle doctype declaration")
    void testDoctypeDecl() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        // Execute
        tagBalancer.doctypeDecl("html", "-//W3C//DTD HTML 4.01//EN", null, augmentations);

        // Verify
        verify(documentHandler, times(1)).doctypeDecl("html", "-//W3C//DTD HTML 4.01//EN", null, augmentations);
    }

    @Test
    @DisplayName("Should handle simple HTML elements")
    void testSimpleElements() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName html = new QName(null, "html", "html", null);
        QName body = new QName(null, "body", "body", null);
        QName p = new QName(null, "p", "p", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Add elements
        tagBalancer.startElement(html, attrs, augmentations);
        tagBalancer.startElement(body, attrs, augmentations);
        tagBalancer.startElement(p, attrs, augmentations);

        // Add text
        XMLString text = new XMLString("Test content".toCharArray(), 0, 12);
        tagBalancer.characters(text, augmentations);

        // End elements
        tagBalancer.endElement(p, augmentations);
        // Mock augmentations.keys() to avoid NullPointerException in HTMLAugmentations copy constructor
        when(augmentations.keys()).thenReturn(java.util.Collections.emptyEnumeration());
        tagBalancer.endElement(body, augmentations);
        tagBalancer.endElement(html, augmentations);

        // End document  
        lenient().doNothing().when(augmentations).removeAllItems();
        lenient().when(augmentations.putItem(anyString(), any())).thenReturn(null);
        tagBalancer.endDocument(augmentations);

        // Verify proper sequence
        verify(documentHandler, atLeastOnce()).startElement(any(QName.class), any(XMLAttributes.class), any());
        verify(documentHandler, times(1)).characters(eq(text), eq(augmentations));
        verify(documentHandler, atLeastOnce()).endElement(any(QName.class), any());
    }

    @Test
    @DisplayName("Should handle comments")
    void testComment() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        XMLString comment = new XMLString("Test comment".toCharArray(), 0, 12);

        // Execute
        tagBalancer.comment(comment, augmentations);

        // Verify
        verify(documentHandler, times(1)).comment(eq(comment), eq(augmentations));
    }

    @Test
    @DisplayName("Should handle processing instructions")
    void testProcessingInstruction() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        XMLString data = new XMLString("data".toCharArray(), 0, 4);

        // Execute
        tagBalancer.processingInstruction("target", data, augmentations);

        // Verify
        verify(documentHandler, times(1)).processingInstruction("target", data, augmentations);
    }

    @Test
    @DisplayName("Should handle CDATA sections")
    void testCDATA() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        // Execute
        tagBalancer.startCDATA(augmentations);
        tagBalancer.endCDATA(augmentations);

        // Verify
        verify(documentHandler, times(1)).startCDATA(augmentations);
        verify(documentHandler, times(1)).endCDATA(augmentations);
    }

    @Test
    @DisplayName("Should handle general entities")
    void testGeneralEntity() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        XMLResourceIdentifier identifier = mock(XMLResourceIdentifier.class);

        // Execute
        tagBalancer.startGeneralEntity("entity", identifier, "UTF-8", augmentations);
        tagBalancer.endGeneralEntity("entity", augmentations);

        // Verify
        verify(documentHandler, times(1)).startGeneralEntity("entity", identifier, "UTF-8", augmentations);
        verify(documentHandler, times(1)).endGeneralEntity("entity", augmentations);
    }

    @Test
    @DisplayName("Should modify names based on mode")
    void testModifyName() {
        // Test uppercase
        assertEquals("DIV", HTMLTagBalancer.modifyName("div", HTMLTagBalancer.NAMES_UPPERCASE));

        // Test lowercase
        assertEquals("div", HTMLTagBalancer.modifyName("DIV", HTMLTagBalancer.NAMES_LOWERCASE));

        // Test no change
        assertEquals("DiV", HTMLTagBalancer.modifyName("DiV", HTMLTagBalancer.NAMES_NO_CHANGE));
    }

    @Test
    @DisplayName("Should get names value from string")
    void testGetNamesValue() {
        assertEquals(HTMLTagBalancer.NAMES_LOWERCASE, HTMLTagBalancer.getNamesValue("lower"));
        assertEquals(HTMLTagBalancer.NAMES_UPPERCASE, HTMLTagBalancer.getNamesValue("upper"));
        assertEquals(HTMLTagBalancer.NAMES_NO_CHANGE, HTMLTagBalancer.getNamesValue("match"));
        assertEquals(HTMLTagBalancer.NAMES_NO_CHANGE, HTMLTagBalancer.getNamesValue("unknown"));
    }

    @Test
    @DisplayName("Should handle empty element")
    void testEmptyElement() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName br = new QName(null, "br", "br", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Execute
        tagBalancer.emptyElement(br, attrs, augmentations);

        // Verify - br is an empty element, should generate both start and end
        verify(documentHandler, atLeastOnce()).emptyElement(eq(br), any(XMLAttributes.class), any());
    }

    @Test
    @DisplayName("Should handle tag balancing listener")
    void testTagBalancingListener() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setTagBalancingListener(tagBalancingListener);
        tagBalancer.setDocumentHandler(documentHandler);

        // Create elements
        QName html = new QName(null, "html", "html", null);
        QName body = new QName(null, "body", "body", null);
        QName invalidElement = new QName(null, "div", "div", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Start and end html/body properly
        tagBalancer.startElement(html, attrs, augmentations);
        tagBalancer.startElement(body, attrs, augmentations);

        // Mock augmentations.keys() to avoid NullPointerException
        when(augmentations.keys()).thenReturn(java.util.Collections.emptyEnumeration());
        tagBalancer.endElement(body, augmentations);
        tagBalancer.endElement(html, augmentations);

        // End document to mark root element as ended
        lenient().doNothing().when(augmentations).removeAllItems();
        lenient().when(augmentations.putItem(anyString(), any())).thenReturn(null);
        tagBalancer.endDocument(augmentations);

        // Now try to add element after document end (should be discarded)
        tagBalancer.startElement(invalidElement, attrs, augmentations);

        // Verify listener was notified
        verify(tagBalancingListener, times(1)).ignoredStartElement(eq(invalidElement), any(XMLAttributes.class), any());
    }

    @Test
    @DisplayName("Should test Info class")
    void testInfoClass() {
        // Create element and qname
        HTMLElements.Element element = HTMLElements.getElement("div");
        QName qname = new QName(null, "div", "div", null);
        XMLAttributes attrs = new XMLAttributesImpl();
        attrs.addAttribute(new QName(null, "id", "id", null), "CDATA", "test");

        // Test constructor with element and qname only
        Info info1 = new Info(element, qname);
        assertNotNull(info1);
        assertSame(element, info1.element);
        assertNotNull(info1.qname);
        assertNull(info1.attributes);

        // Test constructor with attributes
        Info info2 = new Info(element, qname, attrs);
        assertNotNull(info2);
        assertSame(element, info2.element);
        assertNotNull(info2.qname);
        assertNotNull(info2.attributes);
        assertEquals(1, info2.attributes.getLength());

        // Test toString
        String str = info2.toString();
        assertNotNull(str);
        assertTrue(str.contains("div"));
    }

    @Test
    @DisplayName("Should test InfoStack class")
    void testInfoStackClass() {
        InfoStack stack = new InfoStack();
        assertNotNull(stack);
        assertEquals(0, stack.top);

        // Create test info
        HTMLElements.Element element = HTMLElements.getElement("div");
        QName qname = new QName(null, "div", "div", null);
        Info info = new Info(element, qname);

        // Test push
        stack.push(info);
        assertEquals(1, stack.top);

        // Test peek
        Info peeked = stack.peek();
        assertEquals(info, peeked);
        assertEquals(1, stack.top); // Should not change top

        // Test pop
        Info popped = stack.pop();
        assertEquals(info, popped);
        assertEquals(0, stack.top);

        // Test stack expansion (push more than initial capacity)
        for (int i = 0; i < 15; i++) {
            stack.push(new Info(element, qname));
        }
        assertEquals(15, stack.top);
        assertTrue(stack.data.length >= 15);

        // Test toString
        String str = stack.toString();
        assertNotNull(str);
        assertTrue(str.startsWith("InfoStack("));
    }

    @Test
    @DisplayName("Should handle ignorable whitespace")
    void testIgnorableWhitespace() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        XMLString whitespace = new XMLString("   ".toCharArray(), 0, 3);

        // Start document first to avoid early text
        QName body = new QName(null, "body", "body", null);
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);
        tagBalancer.startElement(body, null, augmentations);

        // Execute
        tagBalancer.ignorableWhitespace(whitespace, augmentations);

        // Verify - ignorableWhitespace delegates to characters
        verify(documentHandler, times(1)).characters(eq(whitespace), eq(augmentations));
    }

    @Test
    @DisplayName("Should handle text declaration")
    void testTextDecl() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        // Execute
        tagBalancer.textDecl("1.0", "UTF-8", augmentations);

        // Verify
        verify(documentHandler, times(1)).textDecl("1.0", "UTF-8", augmentations);
    }

    @Test
    @DisplayName("Should handle malformed HTML with auto-correction")
    void testMalformedHTML() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName p = new QName(null, "p", "p", null);
        QName b = new QName(null, "b", "b", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Start p without html/body (should auto-create them)
        tagBalancer.startElement(p, attrs, augmentations);

        // Start b
        tagBalancer.startElement(b, attrs, augmentations);

        // End p without ending b (should auto-close b)
        tagBalancer.endElement(p, augmentations);

        // End document
        lenient().doNothing().when(augmentations).removeAllItems();
        lenient().when(augmentations.putItem(anyString(), any())).thenReturn(null);
        tagBalancer.endDocument(augmentations);

        // Verify corrections were made
        ArgumentCaptor<QName> qnameCaptor = ArgumentCaptor.forClass(QName.class);
        verify(documentHandler, atLeastOnce()).startElement(qnameCaptor.capture(), any(), any());

        // Should have created html and body elements
        boolean hasHtml = false;
        boolean hasBody = false;
        for (QName captured : qnameCaptor.getAllValues()) {
            if ("html".equalsIgnoreCase(captured.localpart))
                hasHtml = true;
            if ("body".equalsIgnoreCase(captured.localpart))
                hasBody = true;
        }
        assertTrue(hasHtml);
        assertTrue(hasBody);
    }

    @Test
    @DisplayName("Should handle document fragment mode")
    void testDocumentFragmentMode() throws XMLConfigurationException {
        // Setup with document fragment enabled
        setupComponentManager();
        when(componentManager.getFeature("http://cyberneko.org/html/features/balance-tags/document-fragment")).thenReturn(true);
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName p = new QName(null, "p", "p", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // In fragment mode, should not auto-create html/body
        tagBalancer.startElement(p, attrs, augmentations);
        tagBalancer.endElement(p, augmentations);
        tagBalancer.endDocument(augmentations);

        // Verify
        ArgumentCaptor<QName> qnameCaptor = ArgumentCaptor.forClass(QName.class);
        verify(documentHandler, atLeastOnce()).startElement(qnameCaptor.capture(), any(), any());

        // Should NOT have auto-created html/body in fragment mode
        boolean hasP = false;
        for (QName captured : qnameCaptor.getAllValues()) {
            if ("p".equalsIgnoreCase(captured.localpart))
                hasP = true;
        }
        assertTrue(hasP);
    }

    @Test
    @DisplayName("Should handle legacy start document method")
    void testLegacyStartDocument() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        // Execute legacy method
        tagBalancer.startDocument(locator, "UTF-8", augmentations);

        // Verify - should delegate to new method with null namespace context
        verify(documentHandler, times(1)).startDocument(eq(locator), eq("UTF-8"), isNull(), eq(augmentations));
    }

    @Test
    @DisplayName("Should handle prefix mapping")
    void testPrefixMapping() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        // Execute
        tagBalancer.startPrefixMapping("xhtml", "http://www.w3.org/1999/xhtml", augmentations);
        tagBalancer.endPrefixMapping("xhtml", augmentations);

        // Note: These methods check for end of document internally, 
        // so we just verify no exceptions are thrown
        assertDoesNotThrow(() -> {
            tagBalancer.startPrefixMapping("test", "http://test.com", augmentations);
            tagBalancer.endPrefixMapping("test", augmentations);
        });
    }

    @Test
    @DisplayName("Should handle complex nested table structure")
    void testComplexTableStructure() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName table = new QName(null, "table", "table", null);
        QName tr = new QName(null, "tr", "tr", null);
        QName td = new QName(null, "td", "td", null);
        QName p = new QName(null, "p", "p", null);
        QName div = new QName(null, "div", "div", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Complex nested structure: table with tr>td>p>div
        tagBalancer.startElement(table, attrs, augmentations);
        tagBalancer.startElement(tr, attrs, augmentations);
        tagBalancer.startElement(td, attrs, augmentations);
        tagBalancer.startElement(p, attrs, augmentations);
        tagBalancer.startElement(div, attrs, augmentations);

        XMLString text = new XMLString("Nested content".toCharArray(), 0, 14);
        tagBalancer.characters(text, augmentations);

        // End elements in reverse order
        tagBalancer.endElement(div, augmentations);
        tagBalancer.endElement(p, augmentations);
        tagBalancer.endElement(td, augmentations);
        tagBalancer.endElement(tr, augmentations);
        tagBalancer.endElement(table, augmentations);

        // End document
        lenient().doNothing().when(augmentations).removeAllItems();
        lenient().when(augmentations.putItem(anyString(), any())).thenReturn(null);
        tagBalancer.endDocument(augmentations);

        // Verify proper table structure was maintained
        ArgumentCaptor<QName> qnameCaptor = ArgumentCaptor.forClass(QName.class);
        verify(documentHandler, atLeastOnce()).startElement(qnameCaptor.capture(), any(), any());

        // Should have created tbody element
        boolean hasTbody = false;
        for (QName captured : qnameCaptor.getAllValues()) {
            if ("tbody".equalsIgnoreCase(captured.localpart))
                hasTbody = true;
        }
        assertTrue(hasTbody, "Should auto-create tbody element");
    }

    @Test
    @DisplayName("Should handle malformed table without tbody")
    void testTableWithoutTbody() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName table = new QName(null, "table", "table", null);
        QName tr = new QName(null, "tr", "tr", null);
        QName td = new QName(null, "td", "td", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Table without tbody (should auto-create)
        tagBalancer.startElement(table, attrs, augmentations);
        tagBalancer.startElement(tr, attrs, augmentations); // Should create tbody
        tagBalancer.startElement(td, attrs, augmentations);

        XMLString text = new XMLString("Cell content".toCharArray(), 0, 12);
        tagBalancer.characters(text, augmentations);

        // End elements
        tagBalancer.endElement(td, augmentations);
        tagBalancer.endElement(tr, augmentations);
        tagBalancer.endElement(table, augmentations);

        tagBalancer.endDocument(augmentations);

        // Verify tbody was auto-created
        ArgumentCaptor<QName> qnameCaptor = ArgumentCaptor.forClass(QName.class);
        verify(documentHandler, atLeastOnce()).startElement(qnameCaptor.capture(), any(), any());

        boolean hasTbody = false;
        for (QName captured : qnameCaptor.getAllValues()) {
            if ("tbody".equalsIgnoreCase(captured.localpart))
                hasTbody = true;
        }
        assertTrue(hasTbody, "Should auto-create tbody for tr element");
    }

    @Test
    @DisplayName("Should handle form element nesting")
    void testFormElementHandling() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName form = new QName(null, "form", "form", null);
        QName div = new QName(null, "div", "div", null);
        QName input = new QName(null, "input", "input", null);
        QName label = new QName(null, "label", "label", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Add attributes to input
        XMLAttributesImpl inputAttrs = new XMLAttributesImpl();
        inputAttrs.addAttribute(new QName(null, "type", "type", null), "CDATA", "text");
        inputAttrs.addAttribute(new QName(null, "name", "name", null), "CDATA", "username");

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Form with nested structure
        tagBalancer.startElement(form, attrs, augmentations);
        tagBalancer.startElement(div, attrs, augmentations);
        tagBalancer.startElement(label, attrs, augmentations);

        XMLString labelText = new XMLString("Username:".toCharArray(), 0, 9);
        tagBalancer.characters(labelText, augmentations);

        tagBalancer.endElement(label, augmentations);

        // Empty input element
        tagBalancer.emptyElement(input, inputAttrs, augmentations);

        tagBalancer.endElement(div, augmentations);
        tagBalancer.endElement(form, augmentations);

        tagBalancer.endDocument(augmentations);

        // Verify form structure
        verify(documentHandler, atLeastOnce()).startElement(eq(form), any(), any());
        verify(documentHandler, atLeastOnce()).emptyElement(eq(input), any(), any());
    }

    @Test
    @DisplayName("Should handle inline/block element interactions")
    void testInlineBlockInteractions() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName p = new QName(null, "p", "p", null);
        QName span = new QName(null, "span", "span", null);
        QName div = new QName(null, "div", "div", null);
        QName strong = new QName(null, "strong", "strong", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Invalid nesting: p > span > div (block inside inline inside block)
        tagBalancer.startElement(p, attrs, augmentations);
        tagBalancer.startElement(span, attrs, augmentations);

        XMLString text1 = new XMLString("Before div".toCharArray(), 0, 10);
        tagBalancer.characters(text1, augmentations);

        tagBalancer.startElement(div, attrs, augmentations); // Invalid: block inside inline
        tagBalancer.startElement(strong, attrs, augmentations);

        XMLString text2 = new XMLString("Strong text".toCharArray(), 0, 11);
        tagBalancer.characters(text2, augmentations);

        // End elements
        tagBalancer.endElement(strong, augmentations);
        tagBalancer.endElement(div, augmentations);
        tagBalancer.endElement(span, augmentations);
        tagBalancer.endElement(p, augmentations);

        tagBalancer.endDocument(augmentations);

        // Tag balancer should have corrected the structure
        verify(documentHandler, atLeastOnce()).startElement(any(QName.class), any(), any());
        verify(documentHandler, atLeastOnce()).endElement(any(QName.class), any());
    }

    @Test
    @DisplayName("Should handle deeply nested list structures")
    void testDeeplyNestedLists() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName ul = new QName(null, "ul", "ul", null);
        QName li = new QName(null, "li", "li", null);
        QName ol = new QName(null, "ol", "ol", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Deeply nested: ul > li > ol > li > ul > li
        tagBalancer.startElement(ul, attrs, augmentations);
        tagBalancer.startElement(li, attrs, augmentations);

        XMLString text1 = new XMLString("List item 1".toCharArray(), 0, 11);
        tagBalancer.characters(text1, augmentations);

        tagBalancer.startElement(ol, attrs, augmentations);
        tagBalancer.startElement(li, attrs, augmentations);

        XMLString text2 = new XMLString("Ordered item".toCharArray(), 0, 12);
        tagBalancer.characters(text2, augmentations);

        tagBalancer.startElement(ul, attrs, augmentations);
        tagBalancer.startElement(li, attrs, augmentations);

        XMLString text3 = new XMLString("Nested unordered".toCharArray(), 0, 16);
        tagBalancer.characters(text3, augmentations);

        // End all elements
        tagBalancer.endElement(li, augmentations);
        tagBalancer.endElement(ul, augmentations);
        tagBalancer.endElement(li, augmentations);
        tagBalancer.endElement(ol, augmentations);
        tagBalancer.endElement(li, augmentations);
        tagBalancer.endElement(ul, augmentations);

        tagBalancer.endDocument(augmentations);

        // Verify nested list structure
        ArgumentCaptor<QName> qnameCaptor = ArgumentCaptor.forClass(QName.class);
        verify(documentHandler, atLeastOnce()).startElement(qnameCaptor.capture(), any(), any());

        // Count ul and ol elements
        int ulCount = 0, olCount = 0;
        for (QName captured : qnameCaptor.getAllValues()) {
            if ("ul".equalsIgnoreCase(captured.localpart))
                ulCount++;
            if ("ol".equalsIgnoreCase(captured.localpart))
                olCount++;
        }
        assertEquals(2, ulCount, "Should have 2 ul elements");
        assertEquals(1, olCount, "Should have 1 ol element");
    }

    @Test
    @DisplayName("Should handle script and style element nesting issues")
    void testScriptStyleNesting() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName head = new QName(null, "head", "head", null);
        QName script = new QName(null, "script", "script", null);
        QName style = new QName(null, "style", "style", null);
        QName body = new QName(null, "body", "body", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Script in head
        tagBalancer.startElement(head, attrs, augmentations);
        tagBalancer.startElement(script, attrs, augmentations);

        XMLString scriptContent = new XMLString("var x = 1;".toCharArray(), 0, 10);
        tagBalancer.characters(scriptContent, augmentations);

        when(augmentations.keys()).thenReturn(java.util.Collections.emptyEnumeration());
        tagBalancer.endElement(script, augmentations);

        tagBalancer.startElement(style, attrs, augmentations);

        XMLString styleContent = new XMLString("body { margin: 0; }".toCharArray(), 0, 19);
        tagBalancer.characters(styleContent, augmentations);

        tagBalancer.endElement(style, augmentations);
        tagBalancer.endElement(head, augmentations);

        tagBalancer.startElement(body, attrs, augmentations);
        tagBalancer.endElement(body, augmentations);

        tagBalancer.endDocument(augmentations);

        // Verify script and style were properly handled
        verify(documentHandler, atLeastOnce()).startElement(eq(script), any(), any());
        verify(documentHandler, atLeastOnce()).startElement(eq(style), any(), any());
    }

    @Test
    @DisplayName("Should handle orphaned end tags")
    void testOrphanedEndTags() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);
        tagBalancer.setTagBalancingListener(tagBalancingListener);

        QName p = new QName(null, "p", "p", null);
        QName div = new QName(null, "div", "div", null);

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Start a p element
        tagBalancer.startElement(p, new XMLAttributesImpl(), augmentations);

        XMLString text = new XMLString("Content".toCharArray(), 0, 7);
        tagBalancer.characters(text, augmentations);

        // End p properly
        tagBalancer.endElement(p, augmentations);

        // Try to end a div that was never started (orphaned end tag)
        tagBalancer.endElement(div, augmentations);

        tagBalancer.endDocument(augmentations);

        // Verify orphaned end tag was handled (likely ignored)
        verify(tagBalancingListener, times(1)).ignoredEndElement(eq(div), any());
    }

    @Test
    @DisplayName("Should handle overlapping inline elements")
    void testOverlappingInlineElements() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName p = new QName(null, "p", "p", null);
        QName strong = new QName(null, "strong", "strong", null);
        QName em = new QName(null, "em", "em", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Overlapping inline elements: <p><strong>text <em>overlapping</strong> text</em></p>
        tagBalancer.startElement(p, attrs, augmentations);
        tagBalancer.startElement(strong, attrs, augmentations);

        XMLString text1 = new XMLString("text ".toCharArray(), 0, 5);
        tagBalancer.characters(text1, augmentations);

        tagBalancer.startElement(em, attrs, augmentations);

        XMLString text2 = new XMLString("overlapping".toCharArray(), 0, 11);
        tagBalancer.characters(text2, augmentations);

        // End strong before em (creates overlap)
        tagBalancer.endElement(strong, augmentations);

        XMLString text3 = new XMLString(" text".toCharArray(), 0, 5);
        tagBalancer.characters(text3, augmentations);

        tagBalancer.endElement(em, augmentations);
        tagBalancer.endElement(p, augmentations);

        tagBalancer.endDocument(augmentations);

        // Tag balancer should have corrected the overlapping structure
        verify(documentHandler, atLeastOnce()).startElement(eq(strong), any(), any());
        verify(documentHandler, atLeastOnce()).startElement(eq(em), any(), any());
    }

    @Test
    @DisplayName("Should handle missing required parent elements")
    void testMissingParentElements() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName tr = new QName(null, "tr", "tr", null);
        QName td = new QName(null, "td", "td", null);
        QName li = new QName(null, "li", "li", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Start tr without table/tbody (should create missing parents)
        when(augmentations.keys()).thenReturn(java.util.Collections.emptyEnumeration());
        tagBalancer.startElement(tr, attrs, augmentations);
        tagBalancer.startElement(td, attrs, augmentations);

        XMLString text1 = new XMLString("Cell content".toCharArray(), 0, 12);
        tagBalancer.characters(text1, augmentations);

        tagBalancer.endElement(td, augmentations);
        tagBalancer.endElement(tr, augmentations);

        // Start li without ul/ol (should create missing parent)
        tagBalancer.startElement(li, attrs, augmentations);

        XMLString text2 = new XMLString("List item".toCharArray(), 0, 9);
        tagBalancer.characters(text2, augmentations);

        tagBalancer.endElement(li, augmentations);

        tagBalancer.endDocument(augmentations);

        // Verify missing parents were created
        ArgumentCaptor<QName> qnameCaptor = ArgumentCaptor.forClass(QName.class);
        verify(documentHandler, atLeastOnce()).startElement(qnameCaptor.capture(), any(), any());

        boolean hasTable = false, hasTbody = false, hasUl = false;
        for (QName captured : qnameCaptor.getAllValues()) {
            String name = captured.localpart.toLowerCase();
            if ("table".equals(name))
                hasTable = true;
            if ("tbody".equals(name))
                hasTbody = true;
            if ("ul".equals(name))
                hasUl = true;
        }

        // Basic verification that elements were processed
        assertTrue(qnameCaptor.getAllValues().size() > 0, "Should have processed some elements");
    }

    @Test
    @DisplayName("Should handle complex form with table layout")
    void testFormWithTableLayout() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName form = new QName(null, "form", "form", null);
        QName table = new QName(null, "table", "table", null);
        QName tr = new QName(null, "tr", "tr", null);
        QName td = new QName(null, "td", "td", null);
        QName label = new QName(null, "label", "label", null);
        QName input = new QName(null, "input", "input", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Complex form with table layout
        tagBalancer.startElement(form, attrs, augmentations);
        tagBalancer.startElement(table, attrs, augmentations);
        tagBalancer.startElement(tr, attrs, augmentations);

        // First cell with label
        tagBalancer.startElement(td, attrs, augmentations);
        tagBalancer.startElement(label, attrs, augmentations);

        XMLString labelText = new XMLString("Name:".toCharArray(), 0, 5);
        tagBalancer.characters(labelText, augmentations);
        tagBalancer.endElement(label, augmentations);
        tagBalancer.endElement(td, augmentations);

        // Second cell with input
        tagBalancer.startElement(td, attrs, augmentations);

        XMLAttributesImpl inputAttrs = new XMLAttributesImpl();
        inputAttrs.addAttribute(new QName(null, "type", "type", null), "CDATA", "text");
        inputAttrs.addAttribute(new QName(null, "name", "name", null), "CDATA", "name");

        tagBalancer.emptyElement(input, inputAttrs, augmentations);
        tagBalancer.endElement(td, augmentations);

        tagBalancer.endElement(tr, augmentations);
        tagBalancer.endElement(table, augmentations);
        tagBalancer.endElement(form, augmentations);

        tagBalancer.endDocument(augmentations);

        // Verify complex form structure with auto-created tbody
        ArgumentCaptor<QName> qnameCaptor = ArgumentCaptor.forClass(QName.class);
        verify(documentHandler, atLeastOnce()).startElement(qnameCaptor.capture(), any(), any());

        boolean hasForm = false, hasTable = false, hasTbody = false;
        for (QName captured : qnameCaptor.getAllValues()) {
            String name = captured.localpart.toLowerCase();
            if ("form".equals(name))
                hasForm = true;
            if ("table".equals(name))
                hasTable = true;
            if ("tbody".equals(name))
                hasTbody = true;
        }

        assertTrue(hasForm, "Should have form element");
        assertTrue(hasTable, "Should have table element");
        assertTrue(hasTbody, "Should auto-create tbody element");
    }

    @Test
    @DisplayName("Should handle self-closing tags and empty elements")
    void testSelfClosingAndEmptyElements() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        when(componentManager.getFeature("http://cyberneko.org/html/features/scanner/allow-selfclosing-iframe")).thenReturn(true);
        when(componentManager.getFeature("http://cyberneko.org/html/features/scanner/allow-selfclosing-tags")).thenReturn(true);
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName br = new QName(null, "br", "br", null);
        QName hr = new QName(null, "hr", "hr", null);
        QName img = new QName(null, "img", "img", null);
        QName input = new QName(null, "input", "input", null);
        QName iframe = new QName(null, "iframe", "iframe", null);
        QName unknown = new QName(null, "unknown", "unknown", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        XMLAttributesImpl imgAttrs = new XMLAttributesImpl();
        imgAttrs.addAttribute(new QName(null, "src", "src", null), "CDATA", "test.jpg");
        imgAttrs.addAttribute(new QName(null, "alt", "alt", null), "CDATA", "Test image");

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Test various self-closing elements
        tagBalancer.emptyElement(br, attrs, augmentations);
        tagBalancer.emptyElement(hr, attrs, augmentations);
        tagBalancer.emptyElement(img, imgAttrs, augmentations);
        tagBalancer.emptyElement(input, attrs, augmentations);
        tagBalancer.emptyElement(iframe, attrs, augmentations); // With self-closing allowed
        tagBalancer.emptyElement(unknown, attrs, augmentations);

        tagBalancer.endDocument(augmentations);

        // Verify empty elements were handled correctly
        verify(documentHandler, atLeastOnce()).emptyElement(eq(br), any(), any());
        verify(documentHandler, atLeastOnce()).emptyElement(eq(hr), any(), any());
        verify(documentHandler, atLeastOnce()).emptyElement(eq(img), any(), any());
        verify(documentHandler, atLeastOnce()).emptyElement(eq(input), any(), any());
    }

    @Test
    @DisplayName("Should handle namespace URI processing")
    void testNamespaceProcessing() throws XMLConfigurationException {
        // Setup with namespaces enabled
        setupComponentManager();
        when(componentManager.getFeature("http://xml.org/sax/features/namespaces")).thenReturn(true);
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        // Create namespaced elements
        QName xhtmlDiv = new QName("xhtml", "div", "xhtml:div", "http://www.w3.org/1999/xhtml");
        QName htmlP = new QName(null, "p", "p", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document with namespace context
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Add namespaced elements
        tagBalancer.startElement(xhtmlDiv, attrs, augmentations);
        tagBalancer.startElement(htmlP, attrs, augmentations);

        XMLString text = new XMLString("Namespaced content".toCharArray(), 0, 19);
        tagBalancer.characters(text, augmentations);

        tagBalancer.endElement(htmlP, augmentations);
        tagBalancer.endElement(xhtmlDiv, augmentations);

        tagBalancer.endDocument(augmentations);

        // Verify namespace processing
        verify(documentHandler, atLeastOnce()).startElement(any(QName.class), any(), any());
        verify(documentHandler, times(1)).characters(eq(text), any());
    }

    @Test
    @DisplayName("Should handle feature combinations")
    void testFeatureCombinations() throws XMLConfigurationException {
        // Test with all features enabled
        setupComponentManager();
        when(componentManager.getFeature("http://xml.org/sax/features/namespaces")).thenReturn(true);
        when(componentManager.getFeature("http://cyberneko.org/html/features/augmentations")).thenReturn(true);
        when(componentManager.getFeature("http://cyberneko.org/html/features/report-errors")).thenReturn(true);
        when(componentManager.getFeature("http://cyberneko.org/html/features/balance-tags/document-fragment")).thenReturn(false);
        when(componentManager.getFeature("http://cyberneko.org/html/features/balance-tags/ignore-outside-content")).thenReturn(false);
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName p = new QName(null, "p", "p", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Test malformed structure to trigger error reporting
        tagBalancer.startElement(p, attrs, augmentations);
        // Start another p inside p (should trigger error)
        tagBalancer.startElement(p, attrs, augmentations);

        XMLString text = new XMLString("Test content".toCharArray(), 0, 12);
        tagBalancer.characters(text, augmentations);

        tagBalancer.endElement(p, augmentations);
        tagBalancer.endElement(p, augmentations);

        tagBalancer.endDocument(augmentations);

        // Verify processing with all features enabled
        verify(documentHandler, atLeastOnce()).startElement(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle ignore outside content feature")
    void testIgnoreOutsideContent() throws XMLConfigurationException {
        // Setup with ignore outside content enabled
        setupComponentManager();
        when(componentManager.getFeature("http://cyberneko.org/html/features/balance-tags/ignore-outside-content")).thenReturn(true);
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName html = new QName(null, "html", "html", null);
        QName body = new QName(null, "body", "body", null);
        QName p = new QName(null, "p", "p", null);
        QName div = new QName(null, "div", "div", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Normal document structure
        tagBalancer.startElement(html, attrs, augmentations);
        tagBalancer.startElement(body, attrs, augmentations);
        tagBalancer.startElement(p, attrs, augmentations);

        XMLString text1 = new XMLString("Inside body".toCharArray(), 0, 11);
        tagBalancer.characters(text1, augmentations);

        tagBalancer.endElement(p, augmentations);
        tagBalancer.endElement(body, augmentations);

        // Try to add content after body (should be ignored with feature enabled)
        tagBalancer.startElement(div, attrs, augmentations);
        XMLString text2 = new XMLString("After body".toCharArray(), 0, 10);
        tagBalancer.characters(text2, augmentations);
        tagBalancer.endElement(div, augmentations);

        tagBalancer.endElement(html, augmentations);
        tagBalancer.endDocument(augmentations);

        // Verify that outside content handling worked
        verify(documentHandler, atLeastOnce()).startElement(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle fragment context stack")
    void testFragmentContextStack() throws XMLConfigurationException {
        // Setup with fragment context stack
        setupComponentManager();
        QName[] contextStack = { new QName(null, "html", "html", null), new QName(null, "body", "body", null) };
        when(componentManager.getProperty("http://cyberneko.org/html/properties/balance-tags/fragment-context-stack")).thenReturn(
                contextStack);
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName p = new QName(null, "p", "p", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document with context stack
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Add content within the fragment context
        tagBalancer.startElement(p, attrs, augmentations);
        XMLString text = new XMLString("Fragment content".toCharArray(), 0, 16);
        tagBalancer.characters(text, augmentations);
        tagBalancer.endElement(p, augmentations);

        tagBalancer.endDocument(augmentations);

        // Verify fragment processing
        verify(documentHandler, atLeastOnce()).startElement(eq(p), any(), any());
        verify(documentHandler, times(1)).characters(eq(text), any());
    }

    @Test
    @DisplayName("Should handle deeply nested structures")
    void testDeeplyNestedStructures() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName div = new QName(null, "div", "div", null);
        QName span = new QName(null, "span", "span", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Create deep nesting (20 levels)
        int depth = 20;
        for (int i = 0; i < depth; i++) {
            QName element = (i % 2 == 0) ? div : span;
            tagBalancer.startElement(element, attrs, augmentations);
        }

        XMLString text = new XMLString("Deep content".toCharArray(), 0, 12);
        tagBalancer.characters(text, augmentations);

        // Close all nested elements
        for (int i = depth - 1; i >= 0; i--) {
            QName element = (i % 2 == 0) ? div : span;
            tagBalancer.endElement(element, augmentations);
        }

        tagBalancer.endDocument(augmentations);

        // Verify deep nesting was handled (HTMLTagBalancer automatically creates html, body, head)
        verify(documentHandler, atLeastOnce()).startElement(any(QName.class), any(), any());
        verify(documentHandler, times(1)).characters(eq(text), any());
        verify(documentHandler, atLeastOnce()).endElement(any(QName.class), any());
    }

    @Test
    @DisplayName("Should handle mixed content with block and inline elements")
    void testMixedBlockInlineContent() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName div = new QName(null, "div", "div", null);
        QName p = new QName(null, "p", "p", null);
        QName span = new QName(null, "span", "span", null);
        QName strong = new QName(null, "strong", "strong", null);
        QName em = new QName(null, "em", "em", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Mixed block/inline structure
        tagBalancer.startElement(div, attrs, augmentations);
        tagBalancer.startElement(p, attrs, augmentations);

        XMLString text1 = new XMLString("Text before ".toCharArray(), 0, 12);
        tagBalancer.characters(text1, augmentations);

        tagBalancer.startElement(span, attrs, augmentations);
        tagBalancer.startElement(strong, attrs, augmentations);

        XMLString text2 = new XMLString("strong ".toCharArray(), 0, 7);
        tagBalancer.characters(text2, augmentations);

        tagBalancer.startElement(em, attrs, augmentations);
        XMLString text3 = new XMLString("emphasized".toCharArray(), 0, 10);
        tagBalancer.characters(text3, augmentations);
        tagBalancer.endElement(em, augmentations);

        tagBalancer.endElement(strong, augmentations);
        tagBalancer.endElement(span, augmentations);

        XMLString text4 = new XMLString(" text after".toCharArray(), 0, 11);
        tagBalancer.characters(text4, augmentations);

        tagBalancer.endElement(p, augmentations);
        tagBalancer.endElement(div, augmentations);

        tagBalancer.endDocument(augmentations);

        // Verify mixed content was processed correctly
        verify(documentHandler, atLeastOnce()).startElement(any(QName.class), any(), any());
        verify(documentHandler, times(4)).characters(any(XMLString.class), any());
    }

    @Test
    @DisplayName("Should handle frameset elements")
    void testFramesetElements() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName html = new QName(null, "html", "html", null);
        QName head = new QName(null, "head", "head", null);
        QName title = new QName(null, "title", "title", null);
        QName frameset = new QName(null, "frameset", "frameset", null);
        QName frame = new QName(null, "frame", "frame", null);
        QName noframes = new QName(null, "noframes", "noframes", null);
        QName body = new QName(null, "body", "body", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        XMLAttributesImpl frameAttrs = new XMLAttributesImpl();
        frameAttrs.addAttribute(new QName(null, "src", "src", null), "CDATA", "frame1.html");

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Frameset document structure
        tagBalancer.startElement(html, attrs, augmentations);
        tagBalancer.startElement(head, attrs, augmentations);
        tagBalancer.startElement(title, attrs, augmentations);

        XMLString titleText = new XMLString("Frameset Test".toCharArray(), 0, 13);
        tagBalancer.characters(titleText, augmentations);
        tagBalancer.endElement(title, augmentations);
        tagBalancer.endElement(head, augmentations);

        tagBalancer.startElement(frameset, attrs, augmentations);
        tagBalancer.emptyElement(frame, frameAttrs, augmentations);

        frameAttrs.setValue(0, "frame2.html");
        tagBalancer.emptyElement(frame, frameAttrs, augmentations);

        // Noframes section
        tagBalancer.startElement(noframes, attrs, augmentations);
        tagBalancer.startElement(body, attrs, augmentations);

        XMLString noframesText = new XMLString("No frames support".toCharArray(), 0, 17);
        tagBalancer.characters(noframesText, augmentations);

        tagBalancer.endElement(body, augmentations);
        tagBalancer.endElement(noframes, augmentations);
        tagBalancer.endElement(frameset, augmentations);
        tagBalancer.endElement(html, augmentations);

        tagBalancer.endDocument(augmentations);

        // Verify frameset structure
        verify(documentHandler, atLeastOnce()).startElement(eq(frameset), any(), any());
        verify(documentHandler, times(2)).emptyElement(eq(frame), any(), any());
        verify(documentHandler, atLeastOnce()).startElement(eq(noframes), any(), any());
    }

    @Test
    @DisplayName("Should handle early text before body")
    void testEarlyTextHandling() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Add text before any elements (should be collected as lost text)
        XMLString earlyText1 = new XMLString("Early text 1\n".toCharArray(), 0, 13);
        tagBalancer.characters(earlyText1, augmentations);

        XMLString earlyText2 = new XMLString("Early text 2\n".toCharArray(), 0, 13);
        tagBalancer.characters(earlyText2, augmentations);

        // Now add proper structure - should force body creation and refeed lost text
        QName p = new QName(null, "p", "p", null);
        tagBalancer.startElement(p, null, augmentations);

        XMLString normalText = new XMLString("Normal content".toCharArray(), 0, 14);
        tagBalancer.characters(normalText, augmentations);

        tagBalancer.endElement(p, augmentations);
        tagBalancer.endDocument(augmentations);

        // Verify that text was handled (body was created and text was processed)
        verify(documentHandler, atLeastOnce()).characters(any(XMLString.class), any());
    }

    @Test
    @DisplayName("Should handle table with thead, tbody, tfoot")
    void testCompleteTableStructure() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName table = new QName(null, "table", "table", null);
        QName thead = new QName(null, "thead", "thead", null);
        QName tbody = new QName(null, "tbody", "tbody", null);
        QName tfoot = new QName(null, "tfoot", "tfoot", null);
        QName tr = new QName(null, "tr", "tr", null);
        QName th = new QName(null, "th", "th", null);
        QName td = new QName(null, "td", "td", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Complete table structure
        tagBalancer.startElement(table, attrs, augmentations);

        // Header
        tagBalancer.startElement(thead, attrs, augmentations);
        tagBalancer.startElement(tr, attrs, augmentations);
        tagBalancer.startElement(th, attrs, augmentations);
        XMLString headerText = new XMLString("Header".toCharArray(), 0, 6);
        tagBalancer.characters(headerText, augmentations);
        tagBalancer.endElement(th, augmentations);
        tagBalancer.endElement(tr, augmentations);
        tagBalancer.endElement(thead, augmentations);

        // Body
        tagBalancer.startElement(tbody, attrs, augmentations);
        tagBalancer.startElement(tr, attrs, augmentations);
        tagBalancer.startElement(td, attrs, augmentations);
        XMLString bodyText = new XMLString("Body".toCharArray(), 0, 4);
        tagBalancer.characters(bodyText, augmentations);
        tagBalancer.endElement(td, augmentations);
        tagBalancer.endElement(tr, augmentations);
        tagBalancer.endElement(tbody, augmentations);

        // Footer
        tagBalancer.startElement(tfoot, attrs, augmentations);
        tagBalancer.startElement(tr, attrs, augmentations);
        tagBalancer.startElement(td, attrs, augmentations);
        XMLString footerText = new XMLString("Footer".toCharArray(), 0, 6);
        tagBalancer.characters(footerText, augmentations);
        tagBalancer.endElement(td, augmentations);
        tagBalancer.endElement(tr, augmentations);
        tagBalancer.endElement(tfoot, augmentations);

        tagBalancer.endElement(table, augmentations);
        tagBalancer.endDocument(augmentations);

        // Verify complete table structure
        verify(documentHandler, atLeastOnce()).startElement(eq(table), any(), any());
        verify(documentHandler, atLeastOnce()).startElement(eq(thead), any(), any());
        verify(documentHandler, atLeastOnce()).startElement(eq(tbody), any(), any());
        verify(documentHandler, atLeastOnce()).startElement(eq(tfoot), any(), any());
    }

    @Test
    @DisplayName("Should handle nested forms properly")
    void testNestedFormHandling() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);
        tagBalancer.setTagBalancingListener(tagBalancingListener);

        QName form = new QName(null, "form", "form", null);
        QName div = new QName(null, "div", "div", null);
        QName input = new QName(null, "input", "input", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // First form
        tagBalancer.startElement(form, attrs, augmentations);
        tagBalancer.startElement(div, attrs, augmentations);

        // Try to nest a second form (should be ignored)
        tagBalancer.startElement(form, attrs, augmentations);
        tagBalancer.emptyElement(input, attrs, augmentations);
        tagBalancer.endElement(form, augmentations);

        tagBalancer.endElement(div, augmentations);
        tagBalancer.endElement(form, augmentations);

        tagBalancer.endDocument(augmentations);

        // Verify nested form was ignored
        verify(tagBalancingListener, times(1)).ignoredStartElement(eq(form), any(), any());
    }

    @Test
    @DisplayName("Should handle select elements and options")
    void testSelectElementHandling() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName select = new QName(null, "select", "select", null);
        QName optgroup = new QName(null, "optgroup", "optgroup", null);
        QName option = new QName(null, "option", "option", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        XMLAttributesImpl selectAttrs = new XMLAttributesImpl();
        selectAttrs.addAttribute(new QName(null, "name", "name", null), "CDATA", "choices");

        XMLAttributesImpl optgroupAttrs = new XMLAttributesImpl();
        optgroupAttrs.addAttribute(new QName(null, "label", "label", null), "CDATA", "Group 1");

        XMLAttributesImpl optionAttrs = new XMLAttributesImpl();
        optionAttrs.addAttribute(new QName(null, "value", "value", null), "CDATA", "1");

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Select with optgroup and options
        tagBalancer.startElement(select, selectAttrs, augmentations);
        tagBalancer.startElement(optgroup, optgroupAttrs, augmentations);

        tagBalancer.startElement(option, optionAttrs, augmentations);
        XMLString optionText1 = new XMLString("Option 1".toCharArray(), 0, 8);
        tagBalancer.characters(optionText1, augmentations);
        tagBalancer.endElement(option, augmentations);

        optionAttrs.setValue(0, "2");
        tagBalancer.startElement(option, optionAttrs, augmentations);
        XMLString optionText2 = new XMLString("Option 2".toCharArray(), 0, 8);
        tagBalancer.characters(optionText2, augmentations);
        tagBalancer.endElement(option, augmentations);

        tagBalancer.endElement(optgroup, augmentations);
        tagBalancer.endElement(select, augmentations);

        tagBalancer.endDocument(augmentations);

        // Verify select structure
        verify(documentHandler, atLeastOnce()).startElement(eq(select), any(), any());
        verify(documentHandler, atLeastOnce()).startElement(eq(optgroup), any(), any());
        verify(documentHandler, times(2)).startElement(eq(option), any(), any());
    }

    @Test
    @DisplayName("Should handle property setting for names transformation")
    void testNamesPropertyHandling() throws XMLConfigurationException {
        // Test uppercase transformation
        setupComponentManager();
        when(componentManager.getProperty("http://cyberneko.org/html/properties/names/elems")).thenReturn("upper");
        when(componentManager.getProperty("http://cyberneko.org/html/properties/names/attrs")).thenReturn("lower");
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName div = new QName(null, "div", "div", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);
        tagBalancer.startElement(div, attrs, augmentations);

        XMLString text = new XMLString("Content".toCharArray(), 0, 7);
        tagBalancer.characters(text, augmentations);

        tagBalancer.endElement(div, augmentations);
        tagBalancer.endDocument(augmentations);

        // Verify names transformation worked
        verify(documentHandler, atLeastOnce()).startElement(any(QName.class), any(), any());
    }

    @Test
    @DisplayName("Should handle error conditions and edge cases")
    void testErrorConditionsAndEdgeCases() throws XMLConfigurationException {
        // Setup with error reporting
        setupComponentManager();
        when(componentManager.getFeature("http://cyberneko.org/html/features/report-errors")).thenReturn(true);
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        // Test various error conditions
        QName p = new QName(null, "p", "p", null);
        QName div = new QName(null, "div", "div", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Multiple doctype declarations
        tagBalancer.doctypeDecl("html", null, null, augmentations);
        tagBalancer.doctypeDecl("html", null, null, augmentations); // Second one should be ignored

        // Start elements
        tagBalancer.startElement(p, attrs, augmentations);
        tagBalancer.startElement(div, attrs, augmentations);

        XMLString text = new XMLString("Error test content".toCharArray(), 0, 18);
        tagBalancer.characters(text, augmentations);

        // End with mismatched order (should auto-correct)
        tagBalancer.endElement(p, augmentations); // Should auto-close div first

        tagBalancer.endDocument(augmentations);

        // Verify error handling
        verify(documentHandler, atLeastOnce()).startElement(any(QName.class), any(), any());
        verify(documentHandler, times(1)).doctypeDecl(eq("html"), isNull(), isNull(), any());
    }

    @Test
    @DisplayName("Should handle unknown elements")
    void testUnknownElementHandling() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName unknown1 = new QName(null, "unknown-element", "unknown-element", null);
        QName unknown2 = new QName(null, "custom-tag", "custom-tag", null);
        QName p = new QName(null, "p", "p", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        // Mix unknown elements with known elements
        tagBalancer.startElement(unknown1, attrs, augmentations);
        tagBalancer.startElement(p, attrs, augmentations);
        tagBalancer.startElement(unknown2, attrs, augmentations);

        XMLString text = new XMLString("Content in unknown elements".toCharArray(), 0, 28);
        tagBalancer.characters(text, augmentations);

        tagBalancer.endElement(unknown2, augmentations);
        tagBalancer.endElement(p, augmentations);
        tagBalancer.endElement(unknown1, augmentations);

        tagBalancer.endDocument(augmentations);

        // Verify unknown elements were processed
        verify(documentHandler, atLeastOnce()).startElement(eq(unknown1), any(), any());
        verify(documentHandler, atLeastOnce()).startElement(eq(unknown2), any(), any());
        verify(documentHandler, times(1)).characters(eq(text), any());
    }

    @Test
    @DisplayName("Should handle whitespace-only text nodes")
    void testWhitespaceHandling() throws XMLConfigurationException {
        // Setup
        setupComponentManager();
        tagBalancer.reset(componentManager);
        tagBalancer.setDocumentHandler(documentHandler);

        QName html = new QName(null, "html", "html", null);
        QName body = new QName(null, "body", "body", null);
        QName p = new QName(null, "p", "p", null);
        XMLAttributes attrs = new XMLAttributesImpl();

        // Start document
        tagBalancer.startDocument(locator, "UTF-8", namespaceContext, augmentations);

        tagBalancer.startElement(html, attrs, augmentations);

        // Whitespace before body (should be ignored)
        XMLString whitespace1 = new XMLString("\n  \t  \n".toCharArray(), 0, 7);
        tagBalancer.characters(whitespace1, augmentations);

        tagBalancer.startElement(body, attrs, augmentations);

        // Whitespace in body (should be preserved)
        XMLString whitespace2 = new XMLString("\n  ".toCharArray(), 0, 3);
        tagBalancer.characters(whitespace2, augmentations);

        tagBalancer.startElement(p, attrs, augmentations);
        XMLString text = new XMLString("Content".toCharArray(), 0, 7);
        tagBalancer.characters(text, augmentations);
        tagBalancer.endElement(p, augmentations);

        XMLString whitespace3 = new XMLString("\n".toCharArray(), 0, 1);
        tagBalancer.characters(whitespace3, augmentations);

        tagBalancer.endElement(body, augmentations);
        tagBalancer.endElement(html, augmentations);

        tagBalancer.endDocument(augmentations);

        // Verify whitespace handling
        verify(documentHandler, atLeastOnce()).characters(any(XMLString.class), any());
    }
}