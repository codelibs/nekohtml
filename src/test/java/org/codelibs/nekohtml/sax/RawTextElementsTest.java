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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.codelibs.nekohtml.parsers.DOMParser;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Tests for raw text element handling (script, style, textarea, title, etc.).
 * These elements contain raw text that should not be parsed as HTML.
 *
 * @author CodeLibs Project
 */
public class RawTextElementsTest {

    // =========================================================================
    // Script Element Tests
    // =========================================================================

    @Test
    public void testScriptWithHtmlLikeTags() throws Exception {
        final String html = "<html><head><script>var x = '<div>not a tag</div>';</script></head><body></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList scripts = doc.getElementsByTagName("SCRIPT");
        assertEquals(1, scripts.getLength(), "Should have one script element");

        // The script content should contain the HTML-like string
        final String scriptContent = scripts.item(0).getTextContent();
        assertTrue(scriptContent.contains("<div>") || scriptContent.contains("not a tag"),
                "Script content should preserve HTML-like content");
    }

    @Test
    public void testScriptWithComments() throws Exception {
        final String html = "<html><head><script>// This is a comment\n/* multi\nline */</script></head></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList scripts = doc.getElementsByTagName("SCRIPT");
        assertEquals(1, scripts.getLength(), "Should have one script element");
    }

    @Test
    public void testScriptWithTypeAttribute() throws Exception {
        final String html = "<html><head><script type=\"text/javascript\">alert('test');</script></head></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList scripts = doc.getElementsByTagName("SCRIPT");
        assertEquals(1, scripts.getLength(), "Should have one script element");

        final Element script = (Element) scripts.item(0);
        assertEquals("text/javascript", script.getAttribute("type"), "Should preserve type attribute");
    }

    @Test
    public void testMultipleScriptElements() throws Exception {
        final String html =
                "<html><head>" + "<script>var a = 1;</script>" + "<script>var b = 2;</script>" + "<script>var c = 3;</script>"
                        + "</head></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList scripts = doc.getElementsByTagName("SCRIPT");
        assertEquals(3, scripts.getLength(), "Should have three script elements");
    }

    @Test
    public void testEmptyScript() throws Exception {
        final String html = "<html><head><script></script></head></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList scripts = doc.getElementsByTagName("SCRIPT");
        assertEquals(1, scripts.getLength(), "Should have one empty script element");
    }

    @Test
    public void testScriptInBody() throws Exception {
        final String html = "<html><body><script>document.write('Hello');</script></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList scripts = doc.getElementsByTagName("SCRIPT");
        assertEquals(1, scripts.getLength(), "Should have one script element in body");
    }

    // =========================================================================
    // Style Element Tests
    // =========================================================================

    @Test
    public void testStyleWithSelectors() throws Exception {
        final String html = "<html><head><style>p { color: red; } div > span { font-size: 12px; }</style></head></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList styles = doc.getElementsByTagName("STYLE");
        assertEquals(1, styles.getLength(), "Should have one style element");

        final String styleContent = styles.item(0).getTextContent();
        assertTrue(styleContent.contains("color: red") || styleContent.contains("color:"), "Style content should contain CSS rules");
    }

    @Test
    public void testStyleWithMediaQuery() throws Exception {
        final String html = "<html><head><style>@media (max-width: 600px) { body { font-size: 14px; } }</style></head></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList styles = doc.getElementsByTagName("STYLE");
        assertEquals(1, styles.getLength(), "Should have one style element");
    }

    @Test
    public void testStyleWithTypeAttribute() throws Exception {
        final String html = "<html><head><style type=\"text/css\">body { margin: 0; }</style></head></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList styles = doc.getElementsByTagName("STYLE");
        assertEquals(1, styles.getLength(), "Should have one style element");

        final Element style = (Element) styles.item(0);
        assertEquals("text/css", style.getAttribute("type"), "Should preserve type attribute");
    }

    @Test
    public void testEmptyStyle() throws Exception {
        final String html = "<html><head><style></style></head></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList styles = doc.getElementsByTagName("STYLE");
        assertEquals(1, styles.getLength(), "Should have one empty style element");
    }

    // =========================================================================
    // Textarea Element Tests
    // =========================================================================

    @Test
    public void testTextareaWithHtmlContent() throws Exception {
        final String html = "<html><body><textarea><p>This is not a paragraph</p></textarea></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList textareas = doc.getElementsByTagName("TEXTAREA");
        assertEquals(1, textareas.getLength(), "Should have one textarea element");
    }

    @Test
    public void testTextareaWithAttributes() throws Exception {
        final String html = "<html><body><textarea name=\"content\" rows=\"10\" cols=\"50\">Initial text</textarea></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList textareas = doc.getElementsByTagName("TEXTAREA");
        assertEquals(1, textareas.getLength(), "Should have one textarea element");

        final Element textarea = (Element) textareas.item(0);
        assertEquals("content", textarea.getAttribute("name"), "Should preserve name attribute");
        assertEquals("10", textarea.getAttribute("rows"), "Should preserve rows attribute");
        assertEquals("50", textarea.getAttribute("cols"), "Should preserve cols attribute");
    }

    @Test
    public void testMultilineTextarea() throws Exception {
        final String html = "<html><body><textarea>Line 1\nLine 2\nLine 3</textarea></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList textareas = doc.getElementsByTagName("TEXTAREA");
        assertEquals(1, textareas.getLength(), "Should have one textarea element");
    }

    // =========================================================================
    // Title Element Tests
    // =========================================================================

    @Test
    public void testTitleElement() throws Exception {
        final String html = "<html><head><title>Test Page Title</title></head></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList titles = doc.getElementsByTagName("TITLE");
        assertEquals(1, titles.getLength(), "Should have one title element");
        assertEquals("Test Page Title", titles.item(0).getTextContent(), "Should preserve title text");
    }

    @Test
    public void testTitleWithSpecialChars() throws Exception {
        final String html = "<html><head><title>Test < > & \" Page</title></head></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList titles = doc.getElementsByTagName("TITLE");
        assertEquals(1, titles.getLength(), "Should have one title element");
    }

    // =========================================================================
    // XMP and Listing Element Tests (deprecated but may appear in legacy HTML)
    // =========================================================================

    @Test
    public void testXmpElement() throws Exception {
        final String html = "<html><body><xmp><b>This is not bold</b></xmp></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList xmps = doc.getElementsByTagName("XMP");
        // XMP may or may not be supported, just verify parsing doesn't fail
        assertNotNull(doc, "Document should be parsed");
    }

    // =========================================================================
    // Entity Handling in Raw Text Elements
    // =========================================================================

    @Test
    public void testScriptWithEntityLikeContent() throws Exception {
        final String html = "<html><head><script>var x = '&amp; is an ampersand';</script></head></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList scripts = doc.getElementsByTagName("SCRIPT");
        assertEquals(1, scripts.getLength(), "Should have one script element");
    }

    @Test
    public void testStyleWithEntityLikeContent() throws Exception {
        final String html = "<html><head><style>/* &lt; &gt; */</style></head></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList styles = doc.getElementsByTagName("STYLE");
        assertEquals(1, styles.getLength(), "Should have one style element");
    }

    // =========================================================================
    // SAX-Level Tests for Raw Text Content
    // =========================================================================

    @Test
    public void testSAXScriptContent() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final List<String> elements = new ArrayList<>();
        final StringBuilder content = new StringBuilder();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes atts) {
                elements.add("START:" + qName);
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                elements.add("END:" + qName);
            }

            @Override
            public void characters(char[] ch, int start, int length) {
                content.append(new String(ch, start, length));
            }
        });

        final String html = "<html><script>var x = 1;</script></html>";
        scanner.parse(new InputSource(new StringReader(html)));

        assertTrue(elements.contains("START:SCRIPT"), "Should have script start element");
        assertTrue(elements.contains("END:SCRIPT"), "Should have script end element");
    }

    @Test
    public void testSAXStyleContent() throws Exception {
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final List<String> elements = new ArrayList<>();

        scanner.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes atts) {
                elements.add("START:" + qName);
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                elements.add("END:" + qName);
            }
        });

        final String html = "<html><style>p { color: red; }</style></html>";
        scanner.parse(new InputSource(new StringReader(html)));

        assertTrue(elements.contains("START:STYLE"), "Should have style start element");
        assertTrue(elements.contains("END:STYLE"), "Should have style end element");
    }

    // =========================================================================
    // Noscript Element Tests
    // =========================================================================

    @Test
    public void testNoscriptElement() throws Exception {
        final String html = "<html><body><noscript><p>JavaScript is disabled</p></noscript></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList noscripts = doc.getElementsByTagName("NOSCRIPT");
        assertEquals(1, noscripts.getLength(), "Should have one noscript element");
    }

    // =========================================================================
    // Inline Script/Style Tests
    // =========================================================================

    @Test
    public void testInlineEventHandler() throws Exception {
        final String html = "<html><body><button onclick=\"alert('clicked')\">Click</button></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList buttons = doc.getElementsByTagName("BUTTON");
        assertEquals(1, buttons.getLength(), "Should have one button element");

        final Element button = (Element) buttons.item(0);
        assertEquals("alert('clicked')", button.getAttribute("onclick"), "Should preserve onclick attribute");
    }

    @Test
    public void testInlineStyle() throws Exception {
        final String html = "<html><body><div style=\"color: red; font-size: 14px;\">Styled</div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList divs = doc.getElementsByTagName("DIV");
        assertEquals(1, divs.getLength(), "Should have one div element");

        final Element div = (Element) divs.item(0);
        assertTrue(div.getAttribute("style").contains("color"), "Should preserve style attribute");
    }

    // =========================================================================
    // Complex Script Content Tests
    // =========================================================================

    @Test
    public void testScriptWithRegex() throws Exception {
        final String html = "<html><head><script>var regex = /<\\/script>/;</script></head></html>";

        final DOMParser parser = new DOMParser();
        // This is a tricky case - script end tag detection
        assertDoesNotThrow(() -> parser.parse(new InputSource(new StringReader(html))));
    }

    @Test
    public void testScriptWithTemplateStrings() throws Exception {
        final String html = "<html><head><script>var tmpl = `<div>${name}</div>`;</script></head></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        assertNotNull(doc, "Document should be parsed");
    }

    @Test
    public void testScriptWithJsonData() throws Exception {
        final String html = "<html><head><script type=\"application/json\">{\"key\": \"<value>\"}</script></head></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        final Document doc = parser.getDocument();

        final NodeList scripts = doc.getElementsByTagName("SCRIPT");
        assertEquals(1, scripts.getLength(), "Should have one script element");

        final Element script = (Element) scripts.item(0);
        assertEquals("application/json", script.getAttribute("type"), "Should preserve type attribute");
    }

} // class RawTextElementsTest
