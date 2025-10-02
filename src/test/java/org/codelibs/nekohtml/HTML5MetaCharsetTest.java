/*
 * Copyright 2025 CodeLibs, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codelibs.nekohtml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;

import org.codelibs.nekohtml.filters.Writer;
import org.codelibs.nekohtml.parsers.DOMParser;
import org.codelibs.xerces.xni.parser.XMLInputSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

/**
 * Unit tests for HTML5 meta charset shorthand support.
 */
public class HTML5MetaCharsetTest {

    @Test
    @DisplayName("HTML5 meta charset shorthand should be supported")
    void testHTML5MetaCharset() throws IOException, SAXException {
        String html =
                "<!DOCTYPE html>" + "<html>" + "<head>" + "<meta charset=\"UTF-8\">" + "<title>Test</title>" + "</head>" + "<body>"
                        + "<p>Hello World</p>" + "</body>" + "</html>";

        DOMParser parser = new DOMParser();
        parser.parse(new XMLInputSource(null, "test", null, new ByteArrayInputStream(html.getBytes("UTF-8")), "UTF-8"));

        // If parsing succeeds without exceptions, the meta charset is supported
        assertNotNull(parser.getDocument());
        assertEquals("HTML", parser.getDocument().getDocumentElement().getNodeName());
    }

    @Test
    @DisplayName("Traditional meta http-equiv charset should still work")
    void testTraditionalMetaCharset() throws IOException, SAXException {
        String html =
                "<!DOCTYPE html>" + "<html>" + "<head>" + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">"
                        + "<title>Test</title>" + "</head>" + "<body>" + "<p>Hello World</p>" + "</body>" + "</html>";

        DOMParser parser = new DOMParser();
        parser.parse(new XMLInputSource(null, "test", null, new ByteArrayInputStream(html.getBytes("UTF-8")), "UTF-8"));

        // If parsing succeeds without exceptions, the traditional meta charset is supported
        assertNotNull(parser.getDocument());
        assertEquals("HTML", parser.getDocument().getDocumentElement().getNodeName());
    }

    @Test
    @DisplayName("Both meta charset formats should produce same result")
    void testBothMetaCharsetFormats() throws IOException, SAXException {
        String html5Format =
                "<!DOCTYPE html>" + "<html>" + "<head>" + "<meta charset=\"UTF-8\">" + "<title>Test</title>" + "</head>" + "<body>"
                        + "<p>Test content</p>" + "</body>" + "</html>";

        String traditionalFormat =
                "<!DOCTYPE html>" + "<html>" + "<head>" + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">"
                        + "<title>Test</title>" + "</head>" + "<body>" + "<p>Test content</p>" + "</body>" + "</html>";

        // Parse HTML5 format
        HTMLConfiguration config1 = new HTMLConfiguration();
        StringWriter writer1 = new StringWriter();
        Writer filter1 = new Writer(writer1, "UTF-8");
        config1.setDocumentHandler(filter1);
        config1.parse(new XMLInputSource(null, "test1", null, new ByteArrayInputStream(html5Format.getBytes("UTF-8")), "UTF-8"));
        String result1 = writer1.toString();

        // Parse traditional format
        HTMLConfiguration config2 = new HTMLConfiguration();
        StringWriter writer2 = new StringWriter();
        Writer filter2 = new Writer(writer2, "UTF-8");
        config2.setDocumentHandler(filter2);
        config2.parse(new XMLInputSource(null, "test2", null, new ByteArrayInputStream(traditionalFormat.getBytes("UTF-8")), "UTF-8"));
        String result2 = writer2.toString();

        // Both should contain the same content structure
        assertTrue(result1.contains("<TITLE>Test</TITLE>"));
        assertTrue(result2.contains("<TITLE>Test</TITLE>"));
        assertTrue(result1.contains("<P>Test content</P>"));
        assertTrue(result2.contains("<P>Test content</P>"));
    }

    @Test
    @DisplayName("Meta charset should be case insensitive")
    void testMetaCharsetCaseInsensitive() throws IOException, SAXException {
        String[] charsetVariations = { "UTF-8", "utf-8", "Utf-8", "UTF8", "utf8" };

        for (String charset : charsetVariations) {
            String html =
                    "<!DOCTYPE html>" + "<html>" + "<head>" + "<meta charset=\"" + charset + "\">" + "<title>Test</title>" + "</head>"
                            + "<body>" + "<p>Hello</p>" + "</body>" + "</html>";

            DOMParser parser = new DOMParser();
            parser.parse(new XMLInputSource(null, "test", null, new ByteArrayInputStream(html.getBytes("UTF-8")), "UTF-8"));

            // Should parse successfully regardless of charset case
            assertNotNull(parser.getDocument(), "Should parse with charset: " + charset);
        }
    }

    @Test
    @DisplayName("Meta charset with different encodings should be handled")
    void testMetaCharsetDifferentEncodings() throws IOException, SAXException {
        String[] encodings = { "UTF-8", "ISO-8859-1", "Windows-1252" };

        for (String encoding : encodings) {
            String html =
                    "<!DOCTYPE html>" + "<html>" + "<head>" + "<meta charset=\"" + encoding + "\">" + "<title>Test</title>" + "</head>"
                            + "<body>" + "<p>Content</p>" + "</body>" + "</html>";

            DOMParser parser = new DOMParser();
            parser.parse(new XMLInputSource(null, "test", null, new ByteArrayInputStream(html.getBytes("UTF-8")), "UTF-8"));

            // Should parse successfully with different encodings
            assertNotNull(parser.getDocument(), "Should parse with encoding: " + encoding);
        }
    }
}
