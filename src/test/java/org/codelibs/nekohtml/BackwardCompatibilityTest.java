/*
 * Copyright 2002-2009 Andy Clark, Marc Guillemot
 * Copyright 2017-2024 CodeLibs Project and the Others.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;

import org.apache.xerces.xni.parser.XMLInputSource;
import org.codelibs.nekohtml.filters.Writer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test class to verify backward compatibility with HTML4 and XHTML parsing.
 * Ensures that existing HTML4/XHTML documents continue to be parsed correctly
 * even after HTML5 features are added.
 */
class BackwardCompatibilityTest {

    /**
     * Test that HTML 4.01 Strict DOCTYPE does not enable HTML5 mode
     * and parses correctly.
     */
    @Test
    @DisplayName("HTML 4.01 Strict compatibility")
    void testHTML401StrictCompatibility() throws Exception {
        final String html =
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n"
                        + "<html><head><title>HTML 4.01 Test</title></head>\n"
                        + "<body><p>This is HTML 4.01 Strict content.</p></body></html>";

        final HTMLConfiguration config = new HTMLConfiguration();
        final StringWriter output = new StringWriter();
        final Writer writer = new Writer(output, "UTF-8");
        config.setProperty("http://cyberneko.org/html/properties/filters", new Writer[] { writer });

        config.parse(new XMLInputSource(null, null, null, new StringReader(html), null));

        final String result = output.toString();
        assertTrue(result.contains("<!DOCTYPE"), "Should preserve HTML 4.01 DOCTYPE");
        assertTrue(result.contains("HTML 4.01 Test"), "Should parse title correctly");
        assertTrue(result.contains("HTML 4.01 Strict content"), "Should parse body content correctly");

        // Verify HTML5 mode was not enabled
        final HTMLScanner scanner = config.fDocumentScanner;
        assertFalse(scanner.isHTML5Mode(), "HTML5 mode should not be enabled for HTML 4.01");
    }

    /**
     * Test that XHTML 1.0 DOCTYPE does not enable HTML5 mode
     * and parses correctly with proper namespace handling.
     */
    @Test
    @DisplayName("XHTML 1.0 compatibility")
    void testXHTML10Compatibility() throws Exception {
        final String xhtml =
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
                        + "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" + "<head><title>XHTML 1.0 Test</title></head>\n"
                        + "<body><p>This is XHTML 1.0 content with <br/> self-closing tags.</p></body>\n" + "</html>";

        final HTMLConfiguration config = new HTMLConfiguration();
        final StringWriter output = new StringWriter();
        final Writer writer = new Writer(output, "UTF-8");
        config.setProperty("http://cyberneko.org/html/properties/filters", new Writer[] { writer });

        config.parse(new XMLInputSource(null, null, null, new StringReader(xhtml), null));

        final String result = output.toString();
        assertTrue(result.contains("<!DOCTYPE"), "Should preserve XHTML 1.0 DOCTYPE");
        assertTrue(result.contains("xmlns=\"http://www.w3.org/1999/xhtml\""), "Should preserve XHTML namespace");
        assertTrue(result.contains("XHTML 1.0 Test"), "Should parse title correctly");
        assertTrue(result.contains("XHTML 1.0 content"), "Should parse body content correctly");

        // Verify HTML5 mode was not enabled
        final HTMLScanner scanner = config.fDocumentScanner;
        assertFalse(scanner.isHTML5Mode(), "HTML5 mode should not be enabled for XHTML 1.0");
    }

    /**
     * Test that HTML5 DOCTYPE enables HTML5 mode
     * while maintaining compatibility with older parsing behavior.
     */
    @Test
    @DisplayName("HTML5 mode compatibility")
    void testHTML5ModeCompatibility() throws Exception {
        final String html5 =
                "<!DOCTYPE html>\n" + "<html><head><title>HTML5 Test</title></head>\n"
                        + "<body><p>This is HTML5 content with <section>semantic elements</section>.</p></body></html>";

        final HTMLConfiguration config = new HTMLConfiguration();
        final StringWriter output = new StringWriter();
        final Writer writer = new Writer(output, "UTF-8");
        config.setProperty("http://cyberneko.org/html/properties/filters", new Writer[] { writer });

        config.parse(new XMLInputSource(null, null, null, new StringReader(html5), null));

        final String result = output.toString();
        assertTrue(result.contains("<!DOCTYPE html>"), "Should preserve HTML5 DOCTYPE");
        assertTrue(result.contains("HTML5 Test"), "Should parse title correctly");
        assertTrue(result.contains("semantic elements"), "Should parse HTML5 semantic elements");

        // Verify HTML5 mode was enabled
        final HTMLScanner scanner = config.fDocumentScanner;
        assertTrue(scanner.isHTML5Mode(), "HTML5 mode should be enabled for HTML5 DOCTYPE");
    }

    /**
     * Test mixed compatibility - ensure HTML4 elements work in HTML5 mode
     * and HTML5 elements are handled gracefully in HTML4 mode.
     */
    @Test
    @DisplayName("Mixed element compatibility")
    void testMixedElementCompatibility() throws Exception {
        // Test HTML4 elements in HTML5 mode
        final String html5WithOldElements =
                "<!DOCTYPE html>\n" + "<html><head><title>Mixed Test</title></head>\n"
                        + "<body><center>Centered content</center><font color='red'>Red text</font></body></html>";

        final HTMLConfiguration config1 = new HTMLConfiguration();
        final StringWriter output1 = new StringWriter();
        final Writer writer1 = new Writer(output1, "UTF-8");
        config1.setProperty("http://cyberneko.org/html/properties/filters", new Writer[] { writer1 });

        config1.parse(new XMLInputSource(null, null, null, new StringReader(html5WithOldElements), null));

        final String result1 = output1.toString();
        assertTrue(result1.toUpperCase().contains("CENTER"), "Should handle old HTML elements in HTML5 mode");
        assertTrue(result1.toUpperCase().contains("FONT"), "Should handle deprecated elements in HTML5 mode");

        // Test HTML5 elements in HTML4 mode
        final String html4WithNewElements =
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n"
                        + "<html><head><title>Mixed Test</title></head>\n"
                        + "<body><article>Article content</article><nav>Navigation</nav></body></html>";

        final HTMLConfiguration config2 = new HTMLConfiguration();
        final StringWriter output2 = new StringWriter();
        final Writer writer2 = new Writer(output2, "UTF-8");
        config2.setProperty("http://cyberneko.org/html/properties/filters", new Writer[] { writer2 });

        config2.parse(new XMLInputSource(null, null, null, new StringReader(html4WithNewElements), null));

        final String result2 = output2.toString();
        assertTrue(result2.toUpperCase().contains("ARTICLE"), "Should handle new HTML5 elements in HTML4 mode");
        assertTrue(result2.toUpperCase().contains("NAV"), "Should handle HTML5 semantic elements in HTML4 mode");
    }

    /**
     * Test that error reporting continues to work correctly
     * in both HTML4 and HTML5 modes.
     */
    @Test
    @DisplayName("Error reporting compatibility")
    void testErrorReportingCompatibility() throws Exception {
        // Test with HTML4 DOCTYPE
        final String malformedHTML4 =
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n"
                        + "<html><head><title>Test</title></head>\n"
                        + "<body><p>Unclosed paragraph<div>Block inside paragraph</div></body></html>";

        final HTMLConfiguration config1 = new HTMLConfiguration();
        config1.setFeature("http://cyberneko.org/html/features/report-errors", true);
        final HTMLErrorHandler errorHandler1 = new HTMLErrorHandler(new StringWriter());
        config1.setErrorHandler(errorHandler1);

        final StringWriter output1 = new StringWriter();
        final Writer writer1 = new Writer(output1, "UTF-8");
        config1.setProperty("http://cyberneko.org/html/properties/filters", new Writer[] { writer1 });

        config1.parse(new XMLInputSource(null, null, null, new StringReader(malformedHTML4), null));

        // Test with HTML5 DOCTYPE
        final String malformedHTML5 =
                "<!DOCTYPE html>\n" + "<html><head><title>Test</title></head>\n"
                        + "<body><p>Unclosed paragraph<div>Block inside paragraph</div></body></html>";

        final HTMLConfiguration config2 = new HTMLConfiguration();
        config2.setFeature("http://cyberneko.org/html/features/report-errors", true);
        final HTMLErrorHandler errorHandler2 = new HTMLErrorHandler(new StringWriter());
        config2.setErrorHandler(errorHandler2);

        final StringWriter output2 = new StringWriter();
        final Writer writer2 = new Writer(output2, "UTF-8");
        config2.setProperty("http://cyberneko.org/html/properties/filters", new Writer[] { writer2 });

        config2.parse(new XMLInputSource(null, null, null, new StringReader(malformedHTML5), null));

        // Both should produce similar corrected output
        final String result1 = output1.toString();
        final String result2 = output2.toString();
        assertTrue(result1.toUpperCase().contains("</P>"), "Should auto-close paragraph in HTML4 mode");
        assertTrue(result2.toUpperCase().contains("</P>"), "Should auto-close paragraph in HTML5 mode");
    }
}