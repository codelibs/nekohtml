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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 * Coverage tests for {@link SimpleHTMLScanner} targeting uncovered code paths.
 *
 * @author CodeLibs Project
 */
public class SimpleHTMLScannerCoverageTest {

    /**
     * Testable subclass that exposes protected fields for direct manipulation.
     */
    private static class TestableScanner extends SimpleHTMLScanner {

        public void setElementCase(final String elementCase) {
            fElementCase = elementCase;
        }

        public void setAttributeCase(final String attributeCase) {
            fAttributeCase = attributeCase;
        }

        public void setNormalizeElements(final boolean normalize) {
            fNormalizeElements = normalize;
        }

        public void setNormalizeAttributes(final boolean normalize) {
            fNormalizeAttributes = normalize;
        }

        public void clearLexicalHandler() {
            fLexicalHandler = null;
        }

        /** Expose resolveEntities for direct testing */
        public String testResolveEntities(final String text) {
            return resolveEntities(text);
        }

        public String testResolveEntitiesInAttr(final String text) {
            return resolveEntities(text, true);
        }
    }

    private TestableScanner scanner;
    private ContentHandler contentHandler;
    private LexicalHandler lexicalHandler;

    @BeforeEach
    public void setUp() {
        scanner = new TestableScanner();
        contentHandler = mock(ContentHandler.class);
        lexicalHandler = mock(LexicalHandler.class);
        scanner.setContentHandler(contentHandler);
    }

    // -----------------------------------------------------------------------
    // Helper to capture all text emitted via characters()
    // -----------------------------------------------------------------------

    private String captureAllText(final String html) throws IOException, SAXException {
        final ArgumentCaptor<char[]> chCaptor = ArgumentCaptor.forClass(char[].class);
        final ArgumentCaptor<Integer> startCaptor = ArgumentCaptor.forClass(Integer.class);
        final ArgumentCaptor<Integer> lengthCaptor = ArgumentCaptor.forClass(Integer.class);

        parseHtml(html);

        final StringBuilder sb = new StringBuilder();
        try {
            verify(contentHandler, atLeastOnce()).characters(chCaptor.capture(), startCaptor.capture(), lengthCaptor.capture());
            final List<char[]> allCh = chCaptor.getAllValues();
            final List<Integer> allStart = startCaptor.getAllValues();
            final List<Integer> allLength = lengthCaptor.getAllValues();
            for (int i = 0; i < allCh.size(); i++) {
                sb.append(allCh.get(i), allStart.get(i), allLength.get(i));
            }
        } catch (final org.mockito.exceptions.verification.WantedButNotInvoked e) {
            // no characters called
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // 1. resolveCodePoint boundary values - tested via resolveEntities directly
    // -----------------------------------------------------------------------

    @Test
    public void testCodePointAboveUnicodeRange() {
        // &#x110000; is above 0x10FFFF -> should produce U+FFFD
        final String result = scanner.testResolveEntities("&#x110000;");
        assertTrue(result.contains("\uFFFD"), "Code point > 0x10FFFF should be replaced with U+FFFD");
    }

    @Test
    public void testNoncharacterFDD0() {
        final String result = scanner.testResolveEntities("&#xFDD0;");
        assertEquals("\uFFFD", result, "Noncharacter 0xFDD0 should be replaced with U+FFFD");
    }

    @Test
    public void testNoncharacterFDEF() {
        final String result = scanner.testResolveEntities("&#xFDEF;");
        assertEquals("\uFFFD", result, "Noncharacter 0xFDEF should be replaced with U+FFFD");
    }

    @Test
    public void testNoncharacterFFFE() {
        final String result = scanner.testResolveEntities("&#xFFFE;");
        assertEquals("\uFFFD", result, "Noncharacter 0xFFFE should be replaced with U+FFFD");
    }

    @Test
    public void testNoncharacterFFFF() {
        final String result = scanner.testResolveEntities("&#xFFFF;");
        assertEquals("\uFFFD", result, "Noncharacter 0xFFFF should be replaced with U+FFFD");
    }

    @Test
    public void testNoncharacter1FFFE() {
        final String result = scanner.testResolveEntities("&#x1FFFE;");
        assertEquals("\uFFFD", result, "Noncharacter 0x1FFFE should be replaced with U+FFFD");
    }

    @Test
    public void testXmlIllegalChar0x01() {
        final String result = scanner.testResolveEntities("&#x1;");
        assertEquals("\uFFFD", result);
    }

    @Test
    public void testXmlIllegalChar0x08() {
        final String result = scanner.testResolveEntities("&#x8;");
        assertEquals("\uFFFD", result);
    }

    @Test
    public void testXmlIllegalChar0x0B() {
        final String result = scanner.testResolveEntities("&#xB;");
        assertEquals("\uFFFD", result);
    }

    @Test
    public void testXmlIllegalChar0x0C() {
        final String result = scanner.testResolveEntities("&#xC;");
        assertEquals("\uFFFD", result);
    }

    @Test
    public void testXmlIllegalChar0x0E() {
        final String result = scanner.testResolveEntities("&#xE;");
        assertEquals("\uFFFD", result);
    }

    @Test
    public void testXmlIllegalChar0x1F() {
        final String result = scanner.testResolveEntities("&#x1F;");
        assertEquals("\uFFFD", result);
    }

    @Test
    public void testValidTab0x09() {
        final String result = scanner.testResolveEntities("&#x9;");
        assertEquals("\t", result);
    }

    @Test
    public void testValidNewline0x0A() {
        final String result = scanner.testResolveEntities("&#xA;");
        assertEquals("\n", result);
    }

    @Test
    public void testValidCarriageReturn0x0D() {
        final String result = scanner.testResolveEntities("&#xD;");
        assertEquals("\r", result);
    }

    @Test
    public void testNullCodePoint() {
        final String result = scanner.testResolveEntities("&#0;");
        assertEquals("\uFFFD", result);
    }

    @Test
    public void testSurrogateCodePoint() {
        final String result = scanner.testResolveEntities("&#xD800;");
        assertEquals("\uFFFD", result);
    }

    @Test
    public void testSurrogateCodePointHighEnd() {
        final String result = scanner.testResolveEntities("&#xDFFF;");
        assertEquals("\uFFFD", result);
    }

    @Test
    public void testValidHighCodePoint() {
        final String result = scanner.testResolveEntities("&#x1F600;");
        final String expected = new String(Character.toChars(0x1F600));
        assertEquals(expected, result);
    }

    // -----------------------------------------------------------------------
    // 2. Comment without LexicalHandler
    // -----------------------------------------------------------------------

    @Test
    public void testCommentWithoutLexicalHandler() throws IOException, SAXException {
        scanner.clearLexicalHandler();
        // Should not throw; comment is silently skipped
        final String text = captureAllText("<!-- this is a comment -->");
        // The comment itself should NOT appear in text output
        assertFalse(text.contains("this is a comment"));
    }

    // -----------------------------------------------------------------------
    // 3. CDATA without LexicalHandler
    // -----------------------------------------------------------------------

    @Test
    public void testCdataWithoutLexicalHandler() throws IOException, SAXException {
        scanner.clearLexicalHandler();
        final String text = captureAllText("<![CDATA[hello world]]>");
        assertTrue(text.contains("hello world"), "CDATA content should be emitted as characters");
    }

    @Test
    public void testCdataWithLexicalHandler() throws IOException, SAXException {
        scanner.setLexicalHandler(lexicalHandler);
        parseHtml("<![CDATA[some text]]>");
        verify(lexicalHandler).startCDATA();
        verify(lexicalHandler).endCDATA();
    }

    // -----------------------------------------------------------------------
    // 4. normalizeElementName/normalizeAttributeName with non-standard case
    // -----------------------------------------------------------------------

    @Test
    public void testNormalizeElementNamePreserveCase() throws IOException, SAXException {
        scanner.setElementCase("preserve");
        parseHtml("<DiV></DiV>");
        verify(contentHandler).startElement(eq(""), eq("DiV"), eq("DiV"), any(Attributes.class));
        verify(contentHandler).endElement(eq(""), eq("DiV"), eq("DiV"));
    }

    @Test
    public void testNormalizeAttributeNamePreserveCase() throws IOException, SAXException {
        scanner.setAttributeCase("preserve");
        parseHtml("<div MyAttr=\"value\"></div>");
        verify(contentHandler).startElement(eq(""), anyString(), anyString(), argThat(attrs -> {
            return attrs.getLength() == 1 && "MyAttr".equals(attrs.getLocalName(0));
        }));
    }

    // -----------------------------------------------------------------------
    // 5. Normalization disabled
    // -----------------------------------------------------------------------

    @Test
    public void testNormalizeElementsDisabled() throws IOException, SAXException {
        scanner.setNormalizeElements(false);
        parseHtml("<DiV></DiV>");
        verify(contentHandler).startElement(eq(""), eq("DiV"), eq("DiV"), any(Attributes.class));
        verify(contentHandler).endElement(eq(""), eq("DiV"), eq("DiV"));
    }

    @Test
    public void testNormalizeAttributesDisabled() throws IOException, SAXException {
        scanner.setNormalizeAttributes(false);
        parseHtml("<div MyAttr=\"val\"></div>");
        verify(contentHandler).startElement(eq(""), anyString(), anyString(), argThat(attrs -> {
            return attrs.getLength() == 1 && "MyAttr".equals(attrs.getLocalName(0));
        }));
    }

    // -----------------------------------------------------------------------
    // 6. Entity at end of attribute value (no semicolon)
    // -----------------------------------------------------------------------

    @Test
    public void testEntityWithoutSemicolonAtEndOfAttribute() throws IOException, SAXException {
        parseHtml("<a href=\"test&amp\"></a>");
        verify(contentHandler).startElement(eq(""), anyString(), anyString(), argThat(attrs -> {
            String val = attrs.getValue(0);
            return val != null && val.equals("test&");
        }));
    }

    // -----------------------------------------------------------------------
    // 7. NumberFormatException for very large numeric entities
    // -----------------------------------------------------------------------

    @Test
    public void testVeryLargeDecimalNumericEntity() {
        final String result = scanner.testResolveEntities("&#99999999999999999;");
        assertEquals("&#99999999999999999;", result, "Overflowing decimal should emit raw text");
    }

    @Test
    public void testVeryLargeHexNumericEntity() {
        final String result = scanner.testResolveEntities("&#xFFFFFFFFFF;");
        assertEquals("&#xFFFFFFFFFF;", result, "Overflowing hex should emit raw text");
    }

    // -----------------------------------------------------------------------
    // 8. Entity resolution in attribute context (HTML5 rules)
    // -----------------------------------------------------------------------

    @Test
    public void testEntityNotDecodedInAttributeWhenFollowedByAlphanumeric() {
        final String result = scanner.testResolveEntitiesInAttr("test&not=2");
        assertEquals("test&not=2", result, "&not=2 should NOT be decoded in attribute context");
    }

    @Test
    public void testEntityNotDecodedInAttributeFollowedByEquals() {
        final String result = scanner.testResolveEntitiesInAttr("&copy=");
        assertEquals("&copy=", result, "&copy= should NOT be decoded in attribute context");
    }

    @Test
    public void testEntityDecodedInTextContext() {
        // &not; with semicolon should always be decoded
        final String result = scanner.testResolveEntities("&not;");
        assertEquals("\u00AC", result, "&not; should resolve to NOT SIGN");
    }

    // -----------------------------------------------------------------------
    // 9. Lowercase DOCTYPE
    // -----------------------------------------------------------------------

    @Test
    public void testLowercaseDoctype() throws IOException, SAXException {
        scanner.setLexicalHandler(lexicalHandler);
        parseHtml("<!doctype html><html></html>");
        verify(lexicalHandler).startDTD(eq("html"), isNull(), isNull());
        verify(lexicalHandler).endDTD();
    }

    @Test
    public void testDoctypeWithoutLexicalHandler() throws IOException, SAXException {
        scanner.clearLexicalHandler();
        parseHtml("<!doctype html><html></html>");
        verify(contentHandler).startDocument();
        verify(contentHandler).endDocument();
    }

    // -----------------------------------------------------------------------
    // 10. Parse with null InputSource
    // -----------------------------------------------------------------------

    @Test
    public void testParseWithNullInputSource() {
        assertThrows(SAXException.class, () -> scanner.parse((InputSource) null));
    }

    // -----------------------------------------------------------------------
    // Additional: parse with no ContentHandler set
    // -----------------------------------------------------------------------

    @Test
    public void testParseWithoutContentHandler() throws IOException, SAXException {
        scanner.setContentHandler(null);
        // Should return silently without throwing
        parseHtml("<html></html>");
    }

    // -----------------------------------------------------------------------
    // Additional: normalizeElementName/normalizeAttributeName with null/empty
    // -----------------------------------------------------------------------

    @Test
    public void testNormalizeElementNameNull() throws IOException, SAXException {
        // Access via protected method through subclass
        final String result = scanner.normalizeElementName(null);
        assertNull(result);
    }

    @Test
    public void testNormalizeElementNameEmpty() throws IOException, SAXException {
        final String result = scanner.normalizeElementName("");
        assertEquals("", result);
    }

    @Test
    public void testNormalizeAttributeNameNull() {
        assertNull(scanner.normalizeAttributeName(null));
    }

    @Test
    public void testNormalizeAttributeNameEmpty() {
        assertEquals("", scanner.normalizeAttributeName(""));
    }

    // -----------------------------------------------------------------------
    // resolveEntities edge cases
    // -----------------------------------------------------------------------

    @Test
    public void testResolveEntitiesNull() {
        assertNull(scanner.testResolveEntities(null));
    }

    @Test
    public void testResolveEntitiesNoAmpersand() {
        assertEquals("hello world", scanner.testResolveEntities("hello world"));
    }

    @Test
    public void testUnknownNamedEntity() {
        // &nonexistent; should be kept as-is
        final String result = scanner.testResolveEntities("&nonexistent;");
        assertEquals("&nonexistent;", result);
    }

    /**
     * Helper method to parse an HTML string through the scanner.
     */
    private void parseHtml(final String html) throws IOException, SAXException {
        final InputSource inputSource = new InputSource(new StringReader(html));
        scanner.parse(inputSource);
    }
}
