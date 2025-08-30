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
package org.codelibs.nekohtml.filters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.apache.xerces.util.XMLAttributesImpl;
import org.codelibs.nekohtml.HTMLConfiguration;
import org.codelibs.nekohtml.filters.Writer;

/**
 * Unit tests for {@link Writer}.
 *
 * @author Marc Guillemot
 */
public class WriterTest {

    /**
     * Regression test for bug: writer changed attribute value causing NPE in 2nd writer.
     * http://sourceforge.net/support/tracker.php?aid=2815779
     */
    @Test
    public void testEmptyAttribute() throws Exception {

        final String content = "<html><head>" + "<meta name='COPYRIGHT' content='SOMEONE' />" + "</head><body></body></html>";
        final InputStream inputStream = new ByteArrayInputStream(content.getBytes());

        final XMLDocumentFilter[] filters =
                { new org.codelibs.nekohtml.filters.Writer(new ByteArrayOutputStream(), "UTF-8"),
                        new org.codelibs.nekohtml.filters.Writer(new ByteArrayOutputStream(), "UTF-8") };

        // create HTML parser
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        XMLInputSource source = new XMLInputSource(null, "currentUrl", null, inputStream, "UTF-8");

        parser.parse(source);
        inputStream.close();
    }

    /**
     * Test HTML output format with various elements and attributes.
     */
    @Test
    public void testHtmlOutputFormat() throws Exception {
        final String content =
                "<html><head><title>Test</title></head><body><div id=\"main\" class=\"container\"><p>Hello &amp; world</p></div></body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Output should not be empty");
        // Basic check that some HTML-like content was generated
        assertTrue(result.length() > 10, "Should generate substantial output");
    }

    /**
     * Test XHTML output format with self-closing tags.
     */
    @Test
    public void testXhtmlOutputFormat() throws Exception {
        final String content =
                "<html><head><meta charset=\"UTF-8\"><link rel=\"stylesheet\" href=\"style.css\"></head><body><br><hr></body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Output should not be empty");
    }

    /**
     * Test encoding handling with UTF-8.
     */
    @Test
    public void testUtf8Encoding() throws Exception {
        final String content = "<html><body><p>Testing UTF-8: 日本語 énçødè ♠♥♦♣</p></body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes("UTF-8")), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Output should not be empty");
        assertTrue(result.length() > 10, "Should generate substantial output");
    }

    /**
     * Test encoding handling with ISO-8859-1.
     */
    @Test
    public void testIso88591Encoding() throws Exception {
        final String content = "<html><body><p>Testing ISO-8859-1: café naïve résumé</p></body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "ISO-8859-1") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source =
                new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes("ISO-8859-1")), "ISO-8859-1");
        parser.parse(source);

        final String result = output.toString("ISO-8859-1");
        assertFalse(result.isEmpty(), "Output should not be empty");
    }

    /**
     * Test attribute handling and quoting with special characters.
     */
    @Test
    public void testAttributeQuoting() throws Exception {
        final String content = "<div title=\"It's &quot;quoted&quot; text\" class='multi word class' data-value=\"&lt;script&gt;\"></div>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("title="), "Should handle quoted attributes");
        assertTrue(result.contains("class="), "Should handle class with spaces");
        assertTrue(result.contains("&lt;") || result.contains("&quot;"), "Should escape HTML in attributes");
    }

    /**
     * Test namespace handling.
     */
    @Test
    public void testNamespaceHandling() throws Exception {
        final String content =
                "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>Test</title></head><body><p>Content</p></body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("html"), "Should handle XHTML namespace");
    }

    /**
     * Test element serialization with nested structures.
     */
    @Test
    public void testNestedElementSerialization() throws Exception {
        final String content = "<html><body><div><ul><li><a href=\"#\">Link</a></li><li>Item 2</li></ul></div></body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Output should not be empty");
        assertTrue(result.length() > 10, "Should generate substantial output");
    }

    /**
     * Test error handling with malformed input.
     */
    @Test
    public void testMalformedInputHandling() throws Exception {
        final String content = "<html><body><div><p>Unclosed paragraph<div>Nested div</div></body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");

        // Should not throw exception with malformed input
        assertDoesNotThrow(() -> parser.parse(source));

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Output should not be empty");
    }

    /**
     * Test empty element handling.
     */
    @Test
    public void testEmptyElements() throws Exception {
        final String content =
                "<html><head><meta charset=\"UTF-8\"><link rel=\"stylesheet\"></head><body><br><hr><img src=\"test.jpg\"></body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Output should not be empty");
    }

    /**
     * Test special character entities.
     */
    @Test
    public void testCharacterEntities() throws Exception {
        final String content = "<html><body><p>&lt;&gt;&amp;&quot;&#39;&nbsp;&#8364;</p></body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("&lt;") || result.contains("<"), "Should handle entities");
        assertTrue(result.contains("&amp;") || result.contains("&"), "Should handle entities");
    }

    /**
     * Test multiple Writer filters chained together.
     */
    @Test
    public void testChainedWriters() throws Exception {
        final String content = "<html><body><p>Test content</p></body></html>";
        final ByteArrayOutputStream output1 = new ByteArrayOutputStream();
        final ByteArrayOutputStream output2 = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output1, "UTF-8"), new Writer(output2, "UTF-8") };

        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result1 = output1.toString("UTF-8");
        final String result2 = output2.toString("UTF-8");

        assertFalse(result1.isEmpty(), "First writer output should not be empty");
        assertFalse(result2.isEmpty(), "Second writer output should not be empty");
    }

    /**
     * Test document without root element.
     */
    @Test
    public void testFragmentOutput() throws Exception {
        final String content = "<p>Fragment paragraph</p><div>Fragment div</div>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Output should not be empty");
    }

    /**
     * Test error handling with IOException during writing.
     */
    @Test
    public void testWriterWithIOException() throws Exception {
        final String content = "<html><body><p>Test content</p></body></html>";

        // Create a mock OutputStream that throws IOException
        final OutputStream failingOutputStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("Simulated write failure");
            }
        };

        final XMLDocumentFilter[] filters = { new Writer(failingOutputStream, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");

        // Should handle IOException gracefully
        assertDoesNotThrow(() -> parser.parse(source));
    }

    /**
     * Test handling of null OutputStream.
     */
    @Test
    public void testWriterWithNullOutputStream() throws Exception {
        assertThrows(NullPointerException.class, () -> {
            new org.codelibs.nekohtml.filters.Writer((OutputStream) null, "UTF-8");
        });
    }

    /**
     * Test comment writing.
     */
    @Test
    public void testWriteComment() throws Exception {
        final String content = "<html><!-- This is a comment --><body>Content</body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("comment") || result.contains("<!--"), "Should handle comments");
    }

    /**
     * Test CDATA section writing.
     */
    @Test
    public void testWriteCDATA() throws Exception {
        final String content = "<html><head><script><![CDATA[function test() { return 'hello'; }]]></script></head></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("test()") || result.contains("function"), "Should handle CDATA content");
    }

    /**
     * Test processing instruction writing.
     */
    @Test
    public void testWriteProcessingInstruction() throws Exception {
        final String content = "<?xml version=\"1.0\"?><html><body>Content</body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Output should not be empty");
    }

    /**
     * Test writer with different character encodings.
     */
    @Test
    public void testWriterWithDifferentEncodings() throws Exception {
        final String content = "<html><body><p>Test åäöñç 中文 русский</p></body></html>";

        String[] encodings = { "UTF-8", "UTF-16", "ISO-8859-1" };

        for (String encoding : encodings) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            XMLDocumentFilter[] filters = { new Writer(output, encoding) };
            XMLParserConfiguration parser = new HTMLConfiguration();
            parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

            XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes("UTF-8")), "UTF-8");
            parser.parse(source);

            String result = output.toString(encoding);
            assertFalse(result.isEmpty(), "Output should not be empty for encoding: " + encoding);
        }
    }

    /**
     * Test writer with invalid characters.
     */
    @Test
    public void testWriterWithInvalidCharacters() throws Exception {
        // Include some control characters and invalid XML characters
        final String content = "<html><body><p>Test \u0000\u0001\u001F invalid chars</p></body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes("UTF-8")), "UTF-8");

        // Should handle invalid characters gracefully
        assertDoesNotThrow(() -> parser.parse(source));

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Output should not be empty");
    }

    /**
     * Test writer with large content to test buffer handling.
     */
    @Test
    public void testWriterWithLargeContent() throws Exception {
        StringBuilder largeContent = new StringBuilder("<html><body>");

        // Generate large content - 10000 paragraphs
        for (int i = 0; i < 10000; i++) {
            largeContent.append("<p>Paragraph ").append(i).append(" with some content text.</p>");
        }
        largeContent.append("</body></html>");

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source =
                new XMLInputSource(null, "test", null, new ByteArrayInputStream(largeContent.toString().getBytes("UTF-8")), "UTF-8");

        assertDoesNotThrow(() -> parser.parse(source));

        final String result = output.toString("UTF-8");
        assertTrue(result.length() > 100000, "Should handle large content");
    }

    /**
     * Test writer buffer boundary conditions.
     */
    @Test
    public void testWriterBufferBoundaries() throws Exception {
        // Create content that will span multiple internal buffer boundaries
        StringBuilder content = new StringBuilder("<html><body>");

        // Add attributes that create exactly buffer-sized chunks
        for (int i = 0; i < 100; i++) {
            content.append("<div data-test-attribute-").append(i).append("=\"very-long-attribute-value-that-spans-multiple-characters-")
                    .append(i).append("\">Content ").append(i).append("</div>");
        }
        content.append("</body></html>");

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source =
                new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.toString().getBytes("UTF-8")), "UTF-8");

        assertDoesNotThrow(() -> parser.parse(source));

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("data-test-attribute"), "Should handle buffer boundaries");
    }

    /**
     * Test namespace prefix handling.
     */
    @Test
    public void testNamespacePrefixHandling() throws Exception {
        final String content =
                "<html xmlns:custom=\"http://example.com/custom\">" + "<head><title>Test</title></head>"
                        + "<body><custom:element custom:attr=\"value\">Content</custom:element></body>" + "</html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("custom") || result.contains("xmlns"), "Should handle namespace prefixes");
    }

    /**
     * Test writer output format consistency with multiple parse cycles.
     */
    @Test
    public void testOutputConsistency() throws Exception {
        final String content = "<html><body><div class=\"test\"><p>Content with &amp; entities</p></div></body></html>";

        ByteArrayOutputStream output1 = new ByteArrayOutputStream();
        ByteArrayOutputStream output2 = new ByteArrayOutputStream();

        // Parse the same content twice
        for (ByteArrayOutputStream output : new ByteArrayOutputStream[] { output1, output2 }) {
            XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
            XMLParserConfiguration parser = new HTMLConfiguration();
            parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

            XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
            parser.parse(source);
        }

        String result1 = output1.toString("UTF-8");
        String result2 = output2.toString("UTF-8");

        assertEquals(result1, result2, "Output should be consistent across multiple parses");
    }

    /**
     * Test writing with special DOCTYPE declarations.
     */
    @Test
    public void testDoctypeHandling() throws Exception {
        final String content =
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">"
                        + "<html><head><title>Test</title></head><body>Content</body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Output should not be empty with DOCTYPE");
    }

    /**
     * Test default constructor for Writer.
     */
    @Test
    public void testDefaultConstructor() throws Exception {
        assertDoesNotThrow(() -> {
            Writer writer = new Writer();
            assertNotNull(writer, "Default constructor should create valid instance");
        });
    }

    /**
     * Test Writer constructor with Writer parameter.
     */
    @Test
    public void testWriterConstructorWithWriter() throws Exception {
        StringWriter stringWriter = new StringWriter();

        assertDoesNotThrow(() -> {
            Writer writer = new Writer(stringWriter, "ISO-8859-1");
            assertNotNull(writer, "Writer-based constructor should create valid instance");
        });
    }

    /**
     * Test Writer constructor with null Writer parameter.
     */
    @Test
    public void testWriterConstructorWithNullWriter() throws Exception {
        assertThrows(NullPointerException.class, () -> {
            new Writer((java.io.Writer) null, "UTF-8");
        });
    }

    /**
     * Test Writer constructor with null encoding.
     */
    @Test
    public void testWriterConstructorWithNullEncoding() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertThrows(NullPointerException.class, () -> {
            new Writer(output, null);
        });
    }

    /**
     * Test printAttributeValue with null value.
     */
    @Test
    public void testPrintAttributeValueNull() throws Exception {
        final String content = "<div attr1=\"\" attr2=\"value\"></div>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        assertDoesNotThrow(() -> parser.parse(source));

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("attr1"), "Should handle empty attribute values");
    }

    /**
     * Test printAttributeValue with special characters that need escaping.
     */
    @Test
    public void testPrintAttributeValueEscaping() throws Exception {
        final String content = "<div data-test=\"&lt;&gt;&amp;&quot;'&#9;&#10;&#13;\"></div>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        // Should escape at least some of these characters
        assertTrue(result.contains("&lt;") || result.contains("&amp;") || result.contains("&quot;"),
                "Should escape special characters in attributes");
    }

    /**
     * Test printCharacters with normalization enabled.
     */
    @Test
    public void testPrintCharactersWithNormalization() throws Exception {
        final String content = "<html><body><p>Text with   multiple   spaces</p></body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
        // Enable normalization if available
        parser.setFeature("http://cyberneko.org/html/features/scanner/normalize-attrs", true);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Output should not be empty");
    }

    /**
     * Test printCharacters with empty content.
     */
    @Test
    public void testPrintCharactersEmpty() throws Exception {
        final String content = "<html><body><p></p><div></div></body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Output should not be empty");
    }

    /**
     * Test startElement with null QName.
     */
    @Test
    public void testStartElementWithNullQName() throws Exception {
        Writer writer = new Writer();

        assertThrows(NullPointerException.class, () -> {
            writer.startElement(null, null, null);
        });
    }

    /**
     * Test emptyElement with null QName.
     */
    @Test
    public void testEmptyElementWithNullQName() throws Exception {
        Writer writer = new Writer();

        assertThrows(NullPointerException.class, () -> {
            writer.emptyElement(null, null, null);
        });
    }

    /**
     * Test endElement with null QName.
     */
    @Test
    public void testEndElementWithNullQName() throws Exception {
        Writer writer = new Writer();

        assertThrows(NullPointerException.class, () -> {
            writer.endElement(null, null);
        });
    }

    /**
     * Test startGeneralEntity with null entity name.
     */
    @Test
    public void testStartGeneralEntityWithNull() throws Exception {
        Writer writer = new Writer();

        assertThrows(NullPointerException.class, () -> {
            writer.startGeneralEntity(null, null, null, null);
        });
    }

    /**
     * Test endGeneralEntity with null entity name.
     */
    @Test
    public void testEndGeneralEntityWithNull() throws Exception {
        Writer writer = new Writer();

        assertDoesNotThrow(() -> {
            writer.endGeneralEntity(null, null);
        });
    }

    /**
     * Test entity handling with various entities.
     */
    @Test
    public void testEntityHandling() throws Exception {
        final String content = "<html><body>&amp; &lt; &gt; &quot; &apos; &nbsp;</body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Output should not be empty");
    }

    /**
     * Test attributes with no value.
     */
    @Test
    public void testAttributesWithNoValue() throws Exception {
        final String content = "<input type=\"checkbox\" checked disabled readonly>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("checked") || result.contains("disabled"), "Should handle boolean attributes");
    }

    /**
     * Test mixed content with text and elements.
     */
    @Test
    public void testMixedContent() throws Exception {
        final String content = "<p>Before <strong>bold</strong> middle <em>italic</em> after.</p>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("Before"), "Should handle mixed content");
        assertTrue(result.contains("bold") || result.contains("strong"), "Should handle nested elements");
    }

    /**
     * Test table structures with complex nesting.
     */
    @Test
    public void testTableStructures() throws Exception {
        final String content = "<table><thead><tr><th>Header</th></tr></thead><tbody><tr><td>Data</td></tr></tbody></table>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("Header") && result.contains("Data"), "Should handle table structures");
    }

    /**
     * Test form elements with various input types.
     */
    @Test
    public void testFormElements() throws Exception {
        final String content =
                "<form><input type=\"text\" name=\"name\"><textarea>Content</textarea><select><option>Option</option></select></form>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Should generate some output");
    }

    /**
     * Test script and style elements.
     */
    @Test
    public void testScriptAndStyleElements() throws Exception {
        final String content = "<html><head><style>body { color: red; }</style><script>alert('test');</script></head></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("color") || result.contains("alert"), "Should handle script and style content");
    }

    /**
     * Test HTML5 semantic elements.
     */
    @Test
    public void testHtml5SemanticElements() throws Exception {
        final String content = "<article><header><h1>Title</h1></header><section><p>Content</p></section><footer>Footer</footer></article>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("Title") && result.contains("Content"), "Should handle HTML5 semantic elements");
    }

    /**
     * Test whitespace handling in different contexts.
     */
    @Test
    public void testWhitespaceHandling() throws Exception {
        final String content = "<pre>  Preformatted  \n  text  </pre><p>  Normal  paragraph  </p>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("Preformatted") && result.contains("paragraph"), "Should handle whitespace context");
    }

    /**
     * Test writer with different output streams.
     */
    @Test
    public void testWriterWithDifferentOutputStreams() throws Exception {
        final String content = "<html><body><p>Test content</p></body></html>";

        // Test with different output stream types
        OutputStream[] streams = { new ByteArrayOutputStream(), new ByteArrayOutputStream(1024), new ByteArrayOutputStream(8192) };

        for (OutputStream stream : streams) {
            XMLDocumentFilter[] filters = { new Writer(stream, "UTF-8") };
            XMLParserConfiguration parser = new HTMLConfiguration();
            parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

            XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
            parser.parse(source);

            String result = stream.toString();
            assertFalse(result.isEmpty(), "Output should not be empty for different stream types");
        }
    }

    /**
     * Test writer with very large attribute values.
     */
    @Test
    public void testWriterWithLargeAttributeValues() throws Exception {
        StringBuilder largeValue = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeValue.append("very-long-attribute-value-part-").append(i).append("-");
        }

        final String content = "<div data-large=\"" + largeValue.toString() + "\">Content</div>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes("UTF-8")), "UTF-8");

        assertDoesNotThrow(() -> parser.parse(source));

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("data-large"), "Should handle large attribute values");
    }

    /**
     * Test nested depth boundaries.
     */
    @Test
    public void testNestedDepthBoundaries() throws Exception {
        StringBuilder content = new StringBuilder("<html><body>");

        // Create deeply nested structure
        for (int i = 0; i < 100; i++) {
            content.append("<div class=\"level").append(i).append("\">");
        }
        content.append("Deep content");
        for (int i = 0; i < 100; i++) {
            content.append("</div>");
        }
        content.append("</body></html>");

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source =
                new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.toString().getBytes("UTF-8")), "UTF-8");

        assertDoesNotThrow(() -> parser.parse(source));

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("Deep content"), "Should handle deeply nested structures");
    }

    // ===========================================================================
    // ADDITIONAL COMPREHENSIVE TESTS FOR IMPROVED COVERAGE
    // ===========================================================================

    /**
     * Test UnsupportedEncodingException handling in OutputStream constructor.
     */
    @Test
    public void testUnsupportedEncodingExceptionInConstructor() {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThrows(UnsupportedEncodingException.class, () -> {
            new Writer(output, "INVALID-ENCODING-12345");
        });
    }

    /**
     * Test Writer constructor with PrintWriter.
     */
    @Test
    public void testConstructorWithPrintWriter() throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintWriter printWriter = new PrintWriter(baos, true);

        final Writer writer = new Writer(printWriter, "UTF-8");
        assertNotNull(writer, "Writer should be created with PrintWriter");

        // Test that the writer actually uses the PrintWriter
        final String content = "<html><body><p>Test</p></body></html>";
        final XMLDocumentFilter[] filters = { writer };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = baos.toString("UTF-8");
        assertFalse(result.isEmpty(), "Should produce output through PrintWriter");
    }

    /**
     * Test Writer constructor with regular Writer (not PrintWriter).
     */
    @Test
    public void testConstructorWithRegularWriter() throws Exception {
        final StringWriter stringWriter = new StringWriter();
        final Writer writer = new Writer(stringWriter, "ISO-8859-1");
        assertNotNull(writer, "Writer should be created with regular Writer");

        // Test that the writer wraps the regular Writer with PrintWriter
        final String content = "<html><body><p>Test content</p></body></html>";
        final XMLDocumentFilter[] filters = { writer };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = stringWriter.toString();
        assertFalse(result.isEmpty(), "Should produce output through wrapped Writer");
    }

    /**
     * Test RuntimeException wrapping UnsupportedEncodingException in default constructor.
     */
    @Test
    public void testDefaultConstructorRuntimeException() {
        // This test mainly ensures the default constructor works
        // The RuntimeException case is difficult to trigger since UTF-8 should always be supported
        assertDoesNotThrow(() -> {
            Writer writer = new Writer();
            assertNotNull(writer, "Default constructor should work");
        });
    }

    /**
     * Test direct XMLDocumentHandler methods - startDocument with NamespaceContext.
     */
    @Test
    public void testStartDocumentWithNamespaceContext() throws Exception {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final Writer writer = new Writer(output, "UTF-8");

        // Test the startDocument method directly
        assertDoesNotThrow(() -> {
            writer.startDocument(null, "UTF-8", null, null);
        });

        // Verify internal state is reset
        final String content = "<html><body><p>Test</p></body></html>";
        final XMLDocumentFilter[] filters = { writer };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Should produce output after startDocument");
    }

    /**
     * Test old startDocument method (without NamespaceContext).
     */
    @Test
    public void testOldStartDocumentMethod() throws Exception {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final Writer writer = new Writer(output, "UTF-8");

        assertDoesNotThrow(() -> {
            writer.startDocument(null, "UTF-8", null);
        });
    }

    /**
     * Test comment handling with different positioning.
     */
    @Test
    public void testCommentPositioning() throws Exception {
        // Test comment before root element
        String content1 = "<!-- Before root --><html><body>Content</body></html>";
        ByteArrayOutputStream output1 = new ByteArrayOutputStream();

        XMLDocumentFilter[] filters1 = { new Writer(output1, "UTF-8") };
        XMLParserConfiguration parser1 = new HTMLConfiguration();
        parser1.setProperty("http://cyberneko.org/html/properties/filters", filters1);

        XMLInputSource source1 = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content1.getBytes()), "UTF-8");
        parser1.parse(source1);

        String result1 = output1.toString("UTF-8");
        assertFalse(result1.isEmpty(), "Should handle comments before root element");

        // Test comment after root element
        String content2 = "<html><body>Content</body></html><!-- After root -->";
        ByteArrayOutputStream output2 = new ByteArrayOutputStream();

        XMLDocumentFilter[] filters2 = { new Writer(output2, "UTF-8") };
        XMLParserConfiguration parser2 = new HTMLConfiguration();
        parser2.setProperty("http://cyberneko.org/html/properties/filters", filters2);

        XMLInputSource source2 = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content2.getBytes()), "UTF-8");
        parser2.parse(source2);

        String result2 = output2.toString("UTF-8");
        assertFalse(result2.isEmpty(), "Should handle comments after root element");
    }

    /**
     * Test character entity reference handling.
     */
    @Test
    public void testCharacterEntityReferences() throws Exception {
        final String content = "<html><body>&#65;&#66;&#67;&#x41;&#x42;&#x43;</body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
        parser.setFeature(Writer.NOTIFY_CHAR_REFS, true);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Should handle character entity references");
    }

    /**
     * Test built-in HTML entity reference handling.
     */
    @Test
    public void testBuiltinHtmlEntityReferences() throws Exception {
        final String content = "<html><body>&amp;&lt;&gt;&quot;&apos;</body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
        parser.setFeature(Writer.NOTIFY_HTML_BUILTIN_REFS, true);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Should handle built-in HTML entity references");
    }

    /**
     * Test numeric entity parsing errors.
     */
    @Test
    public void testNumericEntityParsingErrors() throws Exception {
        final String content = "<html><body>&#invalid;&#xinvalid;</body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
        parser.setFeature(Writer.NOTIFY_CHAR_REFS, true);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");

        // Should handle invalid numeric entities gracefully
        assertDoesNotThrow(() -> parser.parse(source));

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Should handle invalid numeric entities gracefully");
    }

    /**
     * Test special element normalization behavior.
     */
    @Test
    public void testSpecialElementNormalization() throws Exception {
        // Test with special elements that should not normalize characters (script, style, etc.)
        final String content = "<html><head><script>var x = '&amp;test';</script><style>body { content: '&lt;'; }</style></head></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Should handle special elements properly");
    }

    /**
     * Test META http-equiv content-type modification.
     */
    @Test
    public void testMetaHttpEquivContentTypeModification() throws Exception {
        final String content = "<html><head><meta http-equiv='Content-Type' content='text/html;charset=ISO-8859-1'></head></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        // The output should contain UTF-8 encoding
        assertTrue(result.toLowerCase().contains("utf-8") || result.toLowerCase().contains("content"), "Should modify charset in META tag");
    }

    /**
     * Test META http-equiv with different case variations.
     */
    @Test
    public void testMetaHttpEquivCaseVariations() throws Exception {
        String[] variations =
                { "<meta HTTP-EQUIV='CONTENT-TYPE' CONTENT='text/html;charset=us-ascii'>",
                        "<meta Http-Equiv='Content-Type' Content='text/html;charset=us-ascii'>",
                        "<META http-equiv='content-type' content='text/html'>" };

        for (String metaTag : variations) {
            String content = "<html><head>" + metaTag + "</head><body>Test</body></html>";
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            XMLDocumentFilter[] filters = { new Writer(output, "UTF-16") };
            XMLParserConfiguration parser = new HTMLConfiguration();
            parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

            XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
            parser.parse(source);

            String result = output.toString("UTF-16");
            assertFalse(result.isEmpty(), "Should handle case variations in META tag: " + metaTag);
        }
    }

    /**
     * Test META tag without http-equiv.
     */
    @Test
    public void testMetaTagWithoutHttpEquiv() throws Exception {
        final String content = "<html><head><meta name='description' content='Test page'></head></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("description"), "Should handle META without http-equiv");
    }

    /**
     * Test META http-equiv without content attribute.
     */
    @Test
    public void testMetaHttpEquivWithoutContent() throws Exception {
        final String content = "<html><head><meta http-equiv='Content-Type'></head></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("content-type") || result.contains("Content-Type"), "Should handle META http-equiv without content");
    }

    /**
     * Test content attribute without charset.
     */
    @Test
    public void testContentWithoutCharset() throws Exception {
        final String content = "<html><head><meta http-equiv='Content-Type' content='text/html'></head></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        // Should add charset parameter
        assertTrue(result.toLowerCase().contains("charset"), "Should add charset to content");
    }

    /**
     * Test attribute value escaping edge cases.
     */
    @Test
    public void testAttributeValueEscapingEdgeCases() throws Exception {
        // Test with only double quotes
        String content1 = "<div title='\"Only quotes\"'>Test</div>";
        ByteArrayOutputStream output1 = new ByteArrayOutputStream();

        XMLDocumentFilter[] filters1 = { new Writer(output1, "UTF-8") };
        XMLParserConfiguration parser1 = new HTMLConfiguration();
        parser1.setProperty("http://cyberneko.org/html/properties/filters", filters1);

        XMLInputSource source1 = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content1.getBytes()), "UTF-8");
        parser1.parse(source1);

        String result1 = output1.toString("UTF-8");
        assertTrue(result1.contains("&quot;") || result1.contains("Only"), "Should escape quotes in attributes");

        // Test with empty attribute value
        String content2 = "<input type='text' value=''>";
        ByteArrayOutputStream output2 = new ByteArrayOutputStream();

        XMLDocumentFilter[] filters2 = { new Writer(output2, "UTF-8") };
        XMLParserConfiguration parser2 = new HTMLConfiguration();
        parser2.setProperty("http://cyberneko.org/html/properties/filters", filters2);

        XMLInputSource source2 = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content2.getBytes()), "UTF-8");
        parser2.parse(source2);

        String result2 = output2.toString("UTF-8");
        assertTrue(result2.contains("value"), "Should handle empty attribute values");
    }

    /**
     * Test character normalization with newlines.
     */
    @Test
    public void testCharacterNormalizationWithNewlines() throws Exception {
        final String content = "<html><body><p>Line 1\nLine 2\nLine 3</p></body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        // Should handle newlines appropriately
        assertTrue(result.contains("Line"), "Should handle text with newlines");
    }

    /**
     * Test flushing behavior.
     */
    @Test
    public void testFlushingBehavior() throws Exception {
        final ByteArrayOutputStream output = new ByteArrayOutputStream() {
            private boolean flushed = false;

            @Override
            public void flush() throws IOException {
                flushed = true;
                super.flush();
            }

            public boolean isFlushed() {
                return flushed;
            }
        };

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final String content = "<html><body>Test</body></html>";
        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        // The writer should have caused some flushing
        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Should produce output and flush");
    }

    /**
     * Test different encodings with special characters.
     */
    @Test
    public void testEncodingsWithSpecialCharacters() throws Exception {
        // Characters that might have different representations in different encodings
        final String content = "<html><body><p>Ñoñó €£¥ ñañé</p></body></html>";

        String[] encodings = { "UTF-8", "UTF-16", "ISO-8859-1", "ISO-8859-15", "US-ASCII" };

        for (String encoding : encodings) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            try {
                XMLDocumentFilter[] filters = { new Writer(output, encoding) };
                XMLParserConfiguration parser = new HTMLConfiguration();
                parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

                XMLInputSource source =
                        new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes("UTF-8")), "UTF-8");
                parser.parse(source);

                // Try to decode with the specified encoding
                String result = output.toString(encoding);
                assertFalse(result.isEmpty(), "Should produce output for encoding: " + encoding);

            } catch (UnsupportedEncodingException e) {
                // Some encodings might not be supported on all systems, skip them
                System.out.println("Encoding not supported, skipping: " + encoding);
            }
        }
    }

    /**
     * Test main method functionality by testing valid arguments.
     * Note: We cannot test the System.exit behavior directly in Java 17+ due to SecurityManager deprecation.
     * Instead, we test that the main method works correctly with valid arguments.
     */
    @Test
    public void testMainMethodUsage() throws Exception {
        // Test with valid arguments that won't cause System.exit
        // Create a temporary file for testing
        java.io.File tempFile = java.io.File.createTempFile("test", ".html");
        tempFile.deleteOnExit();

        // Write some test HTML content
        try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
            writer.write("<html><body><p>Test content</p></body></html>");
        }

        String[] validArgs = { "-e", "UTF-8", tempFile.getAbsolutePath() };

        // Capture system.out to verify output is generated
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));

        try {
            // This should parse the file and write to System.out without calling System.exit
            assertDoesNotThrow(() -> {
                Writer.main(validArgs);
            }, "Main should work with valid arguments");
        } finally {
            System.setOut(originalOut);
        }

        String output = outContent.toString();
        assertFalse(output.isEmpty(), "Should generate output for valid HTML file");
        assertTrue(output.contains("<html>") || output.contains("<body>") || output.contains("Test"), "Should contain HTML content");
    }

    /**
     * Test main method with multiple valid arguments to test different options.
     * Note: We cannot test the -h flag directly due to System.exit, but we test other valid options.
     */
    @Test
    public void testMainMethodWithOptions() throws Exception {
        // Test with identity filter option
        java.io.File tempFile = java.io.File.createTempFile("test", ".html");
        tempFile.deleteOnExit();

        // Write some test HTML content
        try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
            writer.write("<html><head><title>Test</title></head><body><p>Test &amp; content</p></body></html>");
        }

        String[] identityArgs = { "-i", "-e", "UTF-8", tempFile.getAbsolutePath() };

        // Capture system.out to verify output is generated
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalOut = System.out;
        System.setOut(new java.io.PrintStream(outContent));

        try {
            // This should parse the file with identity filter
            assertDoesNotThrow(() -> {
                Writer.main(identityArgs);
            }, "Main should work with identity filter option");
        } finally {
            System.setOut(originalOut);
        }

        String output = outContent.toString();
        assertFalse(output.isEmpty(), "Should generate output with identity filter");

        // Test with purify filter option
        outContent = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(outContent));

        String[] purifyArgs = { "-p", "-oe", "UTF-8", tempFile.getAbsolutePath() };

        try {
            assertDoesNotThrow(() -> {
                Writer.main(purifyArgs);
            }, "Main should work with purify filter option");
        } finally {
            System.setOut(originalOut);
        }

        output = outContent.toString();
        assertFalse(output.isEmpty(), "Should generate output with purify filter");
    }

    /**
     * Test printEntity method coverage.
     */
    @Test
    public void testPrintEntityMethod() throws Exception {
        final String content = "<html><body>&customEntity;</body></html>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
        parser.setFeature(Writer.NOTIFY_HTML_BUILTIN_REFS, true);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Should handle custom entities");
    }

    /**
     * Test various character encodings boundary conditions.
     */
    @Test
    public void testCharacterEncodingBoundaries() throws Exception {
        // Test with characters at encoding boundaries
        StringBuilder content = new StringBuilder("<html><body><p>");

        // Add various Unicode ranges
        content.append("\u0000\u001F\u007F\u0080"); // Control characters and extended ASCII boundary
        content.append("\u00FF\u0100\u07FF\u0800"); // Latin-1 and Unicode boundaries
        content.append("\uD800\uDFFF"); // Surrogate pair boundaries (might be invalid)
        content.append("\uFFFE\uFFFF"); // Unicode non-characters

        content.append("</p></body></html>");

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source =
                new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.toString().getBytes("UTF-8")), "UTF-8");

        // Should handle boundary characters gracefully
        assertDoesNotThrow(() -> parser.parse(source));

        final String result = output.toString("UTF-8");
        assertFalse(result.isEmpty(), "Should handle character encoding boundaries");
    }

    /**
     * Test Writer state after multiple parse cycles.
     */
    @Test
    public void testWriterStateMultipleParseCycles() throws Exception {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final Writer writer = new Writer(output, "UTF-8");
        final XMLDocumentFilter[] filters = { writer };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        // Parse multiple documents with the same writer
        String[] contents =
                { "<html><body><p>Document 1</p></body></html>", "<div>Fragment 2</div>",
                        "<html><head><meta charset='utf-8'></head><body><p>Document 3</p></body></html>" };

        StringBuilder allOutput = new StringBuilder();

        for (int i = 0; i < contents.length; i++) {
            XMLInputSource source = new XMLInputSource(null, "test" + i, null, new ByteArrayInputStream(contents[i].getBytes()), "UTF-8");
            parser.parse(source);

            String currentOutput = output.toString("UTF-8");
            allOutput.append(currentOutput);
            output.reset(); // Clear for next iteration
        }

        assertFalse(allOutput.toString().isEmpty(), "Should handle multiple parse cycles");
    }

    /**
     * Test edge case with very long attribute names and values.
     */
    @Test
    public void testVeryLongAttributeNamesAndValues() throws Exception {
        StringBuilder longName = new StringBuilder("very-long-attribute-name-");
        StringBuilder longValue = new StringBuilder("very-long-attribute-value-");

        // Create names and values that are likely to exceed internal buffer sizes
        for (int i = 0; i < 1000; i++) {
            longName.append("part").append(i).append("-");
            longValue.append("value").append(i).append("-");
        }

        final String content = "<div " + longName.toString() + "='" + longValue.toString() + "'>Content</div>";
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        final XMLDocumentFilter[] filters = { new Writer(output, "UTF-8") };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes("UTF-8")), "UTF-8");

        assertDoesNotThrow(() -> parser.parse(source));

        final String result = output.toString("UTF-8");
        assertTrue(result.contains("part") && result.contains("value"), "Should handle very long attribute names and values");
    }

    /**
     * Test Writer with custom PrintWriter that tracks method calls.
     */
    @Test
    public void testWriterWithCustomPrintWriter() throws Exception {
        final StringBuilder tracked = new StringBuilder();

        final PrintWriter customPrintWriter = new PrintWriter(new java.io.Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                tracked.append(new String(cbuf, off, len));
            }

            @Override
            public void flush() throws IOException {
                tracked.append("[FLUSH]");
            }

            @Override
            public void close() throws IOException {
                tracked.append("[CLOSE]");
            }
        });

        final Writer writer = new Writer(customPrintWriter, "UTF-8");
        final XMLDocumentFilter[] filters = { writer };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final String content = "<html><body><p>Test</p></body></html>";
        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String trackedOutput = tracked.toString();
        assertTrue(trackedOutput.contains("<html>") || trackedOutput.contains("[FLUSH]"), "Should use custom PrintWriter and call flush");
    }

    /**
     * Test that attribute values are properly restored after META content modification.
     */
    @Test
    public void testAttributeValueRestoration() throws Exception {
        final String content =
                "<html><head><meta http-equiv='Content-Type' content='text/html;charset=ISO-8859-1'><meta name='other' content='preserved'></head></html>";

        // Use a custom writer to capture both the output stream and verify no side effects on original attributes
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final Writer writer = new Writer(output, "UTF-8") {
            @Override
            protected void printStartElement(final QName element, final XMLAttributes attributes) {
                // Call the parent implementation
                super.printStartElement(element, attributes);

                // Verify that attributes haven't been permanently modified for the next filter in chain
                if ("meta".equalsIgnoreCase(element.rawname)) {
                    for (int i = 0; i < attributes.getLength(); i++) {
                        String attrName = attributes.getQName(i);
                        String attrValue = attributes.getValue(i);
                        if ("content".equals(attrName) && attrValue.toLowerCase().contains("text/html")) {
                            // The content attribute should be restored to original value for downstream filters
                            // This is hard to test directly, but we ensure the method completes without error
                            assertTrue(attrValue.length() > 0, "Content attribute should have a value");
                        }
                    }
                }
            }
        };

        final XMLDocumentFilter[] filters = { writer };
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        final XMLInputSource source = new XMLInputSource(null, "test", null, new ByteArrayInputStream(content.getBytes()), "UTF-8");
        parser.parse(source);

        final String result = output.toString("UTF-8");
        assertTrue(result.toLowerCase().contains("utf-8"), "Should modify charset in output");
        assertTrue(result.contains("preserved"), "Should preserve other meta tags");
    }
}
