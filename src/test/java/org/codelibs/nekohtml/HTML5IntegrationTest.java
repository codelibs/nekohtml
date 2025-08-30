/*
 * Copyright Marc Guillemot
 * Copyright 2002-2009 Andy Clark, Marc Guillemot
 * Copyright 2017-2024 Shinsuke Sugaya
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

import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.codelibs.nekohtml.filters.Writer;
import org.codelibs.nekohtml.parsers.DOMParser;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Integration tests for HTML5 features.
 *
 * @author Shinsuke Sugaya
 */
public class HTML5IntegrationTest {

    private static final String TEST_DATA_DIR = "src/test/resources/data/";

    @Test
    @DisplayName("HTML5 complete document should parse correctly")
    void testHTML5CompleteDocument() throws IOException, SAXException {
        DOMParser parser = new DOMParser();
        parser.parse(TEST_DATA_DIR + "test-html5-complete.html");

        Document doc = parser.getDocument();
        assertNotNull(doc, "Document should be parsed successfully");

        Element root = doc.getDocumentElement();
        assertEquals("HTML", root.getNodeName());
        assertEquals("en", root.getAttribute("lang"));

        // Check HTML5 semantic elements
        NodeList headers = doc.getElementsByTagName("HEADER");
        assertTrue(headers.getLength() >= 2, "Should have multiple header elements");

        NodeList navs = doc.getElementsByTagName("NAV");
        assertEquals(1, navs.getLength(), "Should have one nav element");

        NodeList mains = doc.getElementsByTagName("MAIN");
        assertEquals(1, mains.getLength(), "Should have one main element");

        NodeList articles = doc.getElementsByTagName("ARTICLE");
        assertEquals(1, articles.getLength(), "Should have one article element");

        NodeList sections = doc.getElementsByTagName("SECTION");
        assertTrue(sections.getLength() >= 3, "Should have multiple section elements");

        NodeList asides = doc.getElementsByTagName("ASIDE");
        assertEquals(1, asides.getLength(), "Should have one aside element");

        NodeList footers = doc.getElementsByTagName("FOOTER");
        assertEquals(1, footers.getLength(), "Should have one footer element");
    }

    @Test
    @DisplayName("HTML5 media elements should contain fallback content")
    void testHTML5MediaElementsFallback() throws IOException, SAXException {
        DOMParser parser = new DOMParser();
        parser.parse(TEST_DATA_DIR + "test-html5-complete.html");

        Document doc = parser.getDocument();

        // Check video element with fallback
        NodeList videos = doc.getElementsByTagName("VIDEO");
        assertEquals(1, videos.getLength(), "Should have one video element");
        Element video = (Element) videos.item(0);

        NodeList videoSources = video.getElementsByTagName("SOURCE");
        assertEquals(2, videoSources.getLength(), "Video should have two source elements");

        NodeList videoParagraphs = video.getElementsByTagName("P");
        assertEquals(1, videoParagraphs.getLength(), "Video should have fallback paragraph");

        // Check audio element with fallback
        NodeList audios = doc.getElementsByTagName("AUDIO");
        assertEquals(1, audios.getLength(), "Should have one audio element");
        Element audio = (Element) audios.item(0);

        NodeList audioSources = audio.getElementsByTagName("SOURCE");
        assertEquals(2, audioSources.getLength(), "Audio should have two source elements");

        NodeList audioParagraphs = audio.getElementsByTagName("P");
        assertEquals(1, audioParagraphs.getLength(), "Audio should have fallback paragraph");

        // Check canvas element with fallback
        NodeList canvases = doc.getElementsByTagName("CANVAS");
        assertEquals(1, canvases.getLength(), "Should have one canvas element");
        Element canvas = (Element) canvases.item(0);

        NodeList canvasParagraphs = canvas.getElementsByTagName("P");
        assertEquals(1, canvasParagraphs.getLength(), "Canvas should have fallback paragraph");

        NodeList canvasImages = canvas.getElementsByTagName("IMG");
        assertEquals(1, canvasImages.getLength(), "Canvas should have fallback image");
    }

    @Test
    @DisplayName("HTML5 form elements should be parsed correctly")
    void testHTML5FormElements() throws IOException, SAXException {
        DOMParser parser = new DOMParser();
        parser.parse(TEST_DATA_DIR + "test-html5-complete.html");

        Document doc = parser.getDocument();

        // Check various input types
        NodeList inputs = doc.getElementsByTagName("INPUT");
        assertTrue(inputs.getLength() >= 7, "Should have multiple input elements");

        // Check specific HTML5 input types exist
        boolean hasEmail = false, hasNumber = false, hasUrl = false, hasColor = false, hasDate = false, hasRange = false;

        for (int i = 0; i < inputs.getLength(); i++) {
            Element input = (Element) inputs.item(i);
            String type = input.getAttribute("type");
            switch (type) {
            case "email":
                hasEmail = true;
                break;
            case "number":
                hasNumber = true;
                break;
            case "url":
                hasUrl = true;
                break;
            case "color":
                hasColor = true;
                break;
            case "date":
                hasDate = true;
                break;
            case "range":
                hasRange = true;
                break;
            }
        }

        assertTrue(hasEmail, "Should have email input type");
        assertTrue(hasNumber, "Should have number input type");
        assertTrue(hasUrl, "Should have url input type");
        assertTrue(hasColor, "Should have color input type");
        assertTrue(hasDate, "Should have date input type");
        assertTrue(hasRange, "Should have range input type");

        // Check HTML5 form elements
        NodeList outputs = doc.getElementsByTagName("OUTPUT");
        assertEquals(1, outputs.getLength(), "Should have one output element");

        NodeList meters = doc.getElementsByTagName("METER");
        assertEquals(1, meters.getLength(), "Should have one meter element");

        NodeList progresses = doc.getElementsByTagName("PROGRESS");
        assertEquals(1, progresses.getLength(), "Should have one progress element");
    }

    @Test
    @DisplayName("HTML5 interactive elements should be parsed correctly")
    void testHTML5InteractiveElements() throws IOException, SAXException {
        DOMParser parser = new DOMParser();
        parser.parse(TEST_DATA_DIR + "test-html5-complete.html");

        Document doc = parser.getDocument();

        // Check details/summary elements
        NodeList details = doc.getElementsByTagName("DETAILS");
        assertEquals(1, details.getLength(), "Should have one details element");

        Element detailsElement = (Element) details.item(0);
        NodeList summaries = detailsElement.getElementsByTagName("SUMMARY");
        assertEquals(1, summaries.getLength(), "Details should have one summary element");

        // Check dialog element
        NodeList dialogs = doc.getElementsByTagName("DIALOG");
        assertEquals(1, dialogs.getLength(), "Should have one dialog element");

        Element dialog = (Element) dialogs.item(0);
        assertEquals("sample-dialog", dialog.getAttribute("id"));
    }

    @Test
    @DisplayName("HTML5 text-level semantic elements should be parsed correctly")
    void testHTML5TextLevelElements() throws IOException, SAXException {
        DOMParser parser = new DOMParser();
        parser.parse(TEST_DATA_DIR + "test-html5-complete.html");

        Document doc = parser.getDocument();

        // Check mark element
        NodeList marks = doc.getElementsByTagName("MARK");
        assertEquals(1, marks.getLength(), "Should have one mark element");

        // Check time element
        NodeList times = doc.getElementsByTagName("TIME");
        assertEquals(1, times.getLength(), "Should have one time element");

        Element time = (Element) times.item(0);
        assertEquals("2025-08-30", time.getAttribute("datetime"));

        // Check data element
        NodeList dataElements = doc.getElementsByTagName("DATA");
        assertEquals(1, dataElements.getLength(), "Should have one data element");

        Element data = (Element) dataElements.item(0);
        assertEquals("123", data.getAttribute("value"));
    }

    @Test
    @DisplayName("HTML5 picture element should be parsed correctly")
    void testHTML5PictureElement() throws IOException, SAXException {
        DOMParser parser = new DOMParser();
        parser.parse(TEST_DATA_DIR + "test-html5-complete.html");

        Document doc = parser.getDocument();

        NodeList pictures = doc.getElementsByTagName("PICTURE");
        assertEquals(1, pictures.getLength(), "Should have one picture element");

        Element picture = (Element) pictures.item(0);
        NodeList sources = picture.getElementsByTagName("SOURCE");
        assertEquals(2, sources.getLength(), "Picture should have two source elements");

        NodeList imgs = picture.getElementsByTagName("IMG");
        assertEquals(1, imgs.getLength(), "Picture should have one img element");
    }

    @Test
    @DisplayName("HTML5 document should be processed with correct DOCTYPE")
    void testHTML5DOCTYPE() throws IOException {
        HTMLScanner scanner = new HTMLScanner();
        XMLInputSource source =
                new XMLInputSource(null, TEST_DATA_DIR + "test-html5-complete.html", null, new FileReader(TEST_DATA_DIR
                        + "test-html5-complete.html"), "UTF-8");
        scanner.setInputSource(source);

        while (scanner.scanDocument(false)) {
            // Continue parsing
        }

        assertTrue(scanner.isHTML5Mode(), "Should detect HTML5 mode for complete HTML5 document");
    }

    @Test
    @DisplayName("HTML5 canonical test should pass")
    void testHTML5Canonical() throws IOException {
        // This test ensures that the HTML5 complete test produces the expected canonical output
        HTMLConfiguration config = new HTMLConfiguration();
        StringWriter writer = new StringWriter();
        Writer filter = new Writer(writer, "UTF-8");
        config.setDocumentHandler(filter);

        XMLInputSource source =
                new XMLInputSource(null, TEST_DATA_DIR + "test-html5-complete.html", null, new FileReader(TEST_DATA_DIR
                        + "test-html5-complete.html"), "UTF-8");
        config.parse(source);

        String result = writer.toString();

        // Check key HTML5 elements are present in output
        assertTrue(result.contains("<HTML"), "Should contain HTML root element");
        assertTrue(result.contains("<HEADER"), "Should contain header elements");
        assertTrue(result.contains("<NAV"), "Should contain nav element");
        assertTrue(result.contains("<MAIN"), "Should contain main element");
        assertTrue(result.contains("<ARTICLE"), "Should contain article element");
        assertTrue(result.contains("<SECTION"), "Should contain section elements");
        assertTrue(result.contains("<ASIDE"), "Should contain aside element");
        assertTrue(result.contains("<FOOTER"), "Should contain footer element");
        assertTrue(result.contains("<VIDEO"), "Should contain video element");
        assertTrue(result.contains("<AUDIO"), "Should contain audio element");
        assertTrue(result.contains("<CANVAS"), "Should contain canvas element");
        assertTrue(result.contains("<PICTURE"), "Should contain picture element");
        assertTrue(result.contains("<DETAILS"), "Should contain details element");
        assertTrue(result.contains("<SUMMARY"), "Should contain summary element");
        assertTrue(result.contains("<OUTPUT"), "Should contain output element");
        assertTrue(result.contains("<METER"), "Should contain meter element");
        assertTrue(result.contains("<PROGRESS"), "Should contain progress element");
    }
}