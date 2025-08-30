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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.apache.xerces.xni.parser.XMLInputSource;

/**
 * Unit tests for HTML5 DOCTYPE handling.
 */
public class HTML5DoctypeTest {

    @Test
    @DisplayName("HTML5 DOCTYPE should be recognized")
    void testHTML5DoctypeRecognition() throws IOException {
        String html = "<!DOCTYPE html><html><head><title>Test</title></head><body><p>Hello</p></body></html>";

        HTMLScanner scanner = new HTMLScanner();
        XMLInputSource source = new XMLInputSource(null, "test", null, new StringReader(html), "UTF-8");
        scanner.setInputSource(source);

        // Parse to trigger DOCTYPE processing
        while (scanner.scanDocument(false)) {
            // Continue parsing
        }

        assertTrue(scanner.isHTML5Mode(), "HTML5 mode should be detected");
    }

    @Test
    @DisplayName("HTML5 DOCTYPE variations should be recognized")
    void testHTML5DoctypeVariations() throws IOException {
        String[] doctypes =
                { "<!DOCTYPE html>", "<!doctype html>", "<!DOCTYPE HTML>", "<!doctype HTML>", "<!DOCTYPE   html   >",
                        "<!doctype   HTML   >" };

        for (String doctype : doctypes) {
            String html = doctype + "<html><head><title>Test</title></head><body></body></html>";

            HTMLScanner scanner = new HTMLScanner();
            XMLInputSource source = new XMLInputSource(null, "test", null, new StringReader(html), "UTF-8");
            scanner.setInputSource(source);

            while (scanner.scanDocument(false)) {
                // Continue parsing
            }

            assertTrue(scanner.isHTML5Mode(), "HTML5 mode should be detected for: " + doctype);
        }
    }

    @Test
    @DisplayName("HTML4 DOCTYPE should not trigger HTML5 mode")
    void testHTML4DoctypeNotHTML5() throws IOException {
        String[] html4Doctypes =
                { "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">",
                        "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">",
                        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Frameset//EN\" \"http://www.w3.org/TR/html4/frameset.dtd\">" };

        for (String doctype : html4Doctypes) {
            String html = doctype + "<html><head><title>Test</title></head><body></body></html>";

            HTMLScanner scanner = new HTMLScanner();
            XMLInputSource source = new XMLInputSource(null, "test", null, new StringReader(html), "UTF-8");
            scanner.setInputSource(source);

            while (scanner.scanDocument(false)) {
                // Continue parsing
            }

            assertFalse(scanner.isHTML5Mode(), "HTML5 mode should NOT be detected for HTML4 DOCTYPE: " + doctype);
        }
    }

    @Test
    @DisplayName("XHTML DOCTYPE should not trigger HTML5 mode")
    void testXHTMLDoctypeNotHTML5() throws IOException {
        String[] xhtmlDoctypes =
                {
                        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">",
                        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">",
                        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">" };

        for (String doctype : xhtmlDoctypes) {
            String html = doctype + "<html><head><title>Test</title></head><body></body></html>";

            HTMLScanner scanner = new HTMLScanner();
            XMLInputSource source = new XMLInputSource(null, "test", null, new StringReader(html), "UTF-8");
            scanner.setInputSource(source);

            while (scanner.scanDocument(false)) {
                // Continue parsing
            }

            assertFalse(scanner.isHTML5Mode(), "HTML5 mode should NOT be detected for XHTML DOCTYPE: " + doctype);
        }
    }

    @Test
    @DisplayName("No DOCTYPE should not trigger HTML5 mode")
    void testNoDoctypeNotHTML5() throws IOException {
        String html = "<html><head><title>Test</title></head><body><p>Hello</p></body></html>";

        HTMLScanner scanner = new HTMLScanner();
        XMLInputSource source = new XMLInputSource(null, "test", null, new StringReader(html), "UTF-8");
        scanner.setInputSource(source);

        while (scanner.scanDocument(false)) {
            // Continue parsing
        }

        assertFalse(scanner.isHTML5Mode(), "HTML5 mode should NOT be detected when no DOCTYPE is present");
    }
}
