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
package org.codelibs.nekohtml.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Test cases for DOMParser.
 */
public class DOMParserTest {

    @Test
    public void testBasicParsing() throws Exception {
        final String html = "<html><head><title>Test</title></head><body><p>Hello World</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        final NodeList htmlElements = doc.getElementsByTagName("HTML");
        assertEquals(1, htmlElements.getLength(), "Should have one HTML element");

        final NodeList pElements = doc.getElementsByTagName("P");
        assertEquals(1, pElements.getLength(), "Should have one P element");

        final Element pElement = (Element) pElements.item(0);
        assertEquals("Hello World", pElement.getTextContent(), "P element should contain 'Hello World'");
    }

    @Test
    public void testAttributes() throws Exception {
        final String html = "<html><body><div id=\"test\" class=\"container\">Content</div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList divElements = doc.getElementsByTagName("DIV");
        assertEquals(1, divElements.getLength(), "Should have one DIV element");

        final Element divElement = (Element) divElements.item(0);
        assertEquals("test", divElement.getAttribute("id"), "DIV should have id='test'");
        assertEquals("container", divElement.getAttribute("class"), "DIV should have class='container'");
    }

    @Test
    public void testNestedElements() throws Exception {
        final String html = "<html><body><div><p>Paragraph 1</p><p>Paragraph 2</p></div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList pElements = doc.getElementsByTagName("P");
        assertEquals(2, pElements.getLength(), "Should have two P elements");

        assertEquals("Paragraph 1", ((Element) pElements.item(0)).getTextContent(), "First P should contain 'Paragraph 1'");
        assertEquals("Paragraph 2", ((Element) pElements.item(1)).getTextContent(), "Second P should contain 'Paragraph 2'");
    }

    @Test
    public void testEmptyElements() throws Exception {
        final String html = "<html><body><br><hr></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        final NodeList brElements = doc.getElementsByTagName("BR");
        assertTrue(brElements.getLength() > 0, "Should have BR element");

        final NodeList hrElements = doc.getElementsByTagName("HR");
        assertTrue(hrElements.getLength() > 0, "Should have HR element");
    }

    @Test
    public void testComments() throws Exception {
        final String html = "<html><body><!--This is a comment--><p>Text</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Comments should be present in the DOM
        final NodeList pElements = doc.getElementsByTagName("P");
        assertEquals(1, pElements.getLength(), "Should have one P element");
    }

    @Test
    public void testXPathQueries() throws Exception {
        final String html =
                "<html>\n" + "<head>\n" + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n"
                        + "<title>タイトル</title>\n" + "</head>\n" + "<body>\n" + "<div>テスト</div>\n" + "</body>\n" + "</html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Create XPath
        final javax.xml.xpath.XPathFactory factory = javax.xml.xpath.XPathFactory.newInstance();
        final javax.xml.xpath.XPath xpath = factory.newXPath();

        // Test XPath with uppercase element names
        final String body = (String) xpath.evaluate("//HTML/BODY", doc, javax.xml.xpath.XPathConstants.STRING);
        assertTrue(body.contains("テスト"), "XPath //HTML/BODY should return text containing 'テスト'");

        final String div = (String) xpath.evaluate("//HTML/BODY/DIV", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("テスト", div.trim(), "XPath //HTML/BODY/DIV should return 'テスト'");

        final String title = (String) xpath.evaluate("//HTML/HEAD/TITLE", doc, javax.xml.xpath.XPathConstants.STRING);
        assertEquals("タイトル", title.trim(), "XPath //HTML/HEAD/TITLE should return 'タイトル'");
    }

    @Test
    public void testCommentNodes() throws Exception {
        final String html = "foo1<!--googleoff: index--><a href=\"index.html\">foo3</a><!--googleon: index-->foo5";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Get root element
        final Element root = doc.getDocumentElement();
        assertNotNull(root, "Root element should not be null");
        assertEquals("HTML", root.getNodeName(), "Root element should be HTML");

        // Body-level content (text/comments/anchor) lives inside the synthesized
        // BODY element per the HTML5 tree construction algorithm.
        final Element body = (Element) doc.getElementsByTagName("BODY").item(0);
        assertNotNull(body, "BODY should be synthesized for body content");
        assertEquals("HTML", body.getParentNode().getNodeName(), "BODY should be a child of HTML");

        // Check children
        final NodeList children = body.getChildNodes();
        int commentCount = 0;
        int elementCount = 0;
        int textCount = 0;

        for (int i = 0; i < children.getLength(); i++) {
            final org.w3c.dom.Node node = children.item(i);
            switch (node.getNodeType()) {
            case org.w3c.dom.Node.COMMENT_NODE:
                commentCount++;
                break;
            case org.w3c.dom.Node.ELEMENT_NODE:
                elementCount++;
                break;
            case org.w3c.dom.Node.TEXT_NODE:
                if (!node.getNodeValue().trim().isEmpty()) {
                    textCount++;
                }
                break;
            }
        }

        assertEquals(2, commentCount, "Should have 2 comment nodes");
        assertEquals(1, elementCount, "Should have 1 element node");
        assertEquals(2, textCount, "Should have 2 text nodes (foo1 and foo5)");

        // Verify comment content
        boolean foundGoogleOff = false;
        boolean foundGoogleOn = false;
        for (int i = 0; i < children.getLength(); i++) {
            final org.w3c.dom.Node node = children.item(i);
            if (node.getNodeType() == org.w3c.dom.Node.COMMENT_NODE) {
                final String comment = node.getNodeValue();
                if ("googleoff: index".equals(comment)) {
                    foundGoogleOff = true;
                } else if ("googleon: index".equals(comment)) {
                    foundGoogleOn = true;
                }
            }
        }

        assertTrue(foundGoogleOff, "Should have googleoff comment");
        assertTrue(foundGoogleOn, "Should have googleon comment");
    }

    @Test
    public void testAttributesWithSpecialCharacters() throws Exception {
        final String html = "<html><body><div x-y=\"a-.:_0\">test</div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        final NodeList divElements = doc.getElementsByTagName("DIV");
        assertEquals(1, divElements.getLength(), "Should have one DIV element");

        final Element divElement = (Element) divElements.item(0);

        // Test attribute with hyphen in name
        final org.w3c.dom.Node xyAttr = divElement.getAttributes().getNamedItem("x-y");
        assertNotNull(xyAttr, "Should have x-y attribute");
        assertEquals("a-.:_0", xyAttr.getNodeValue(), "x-y attribute value should be 'a-.:_0'");
    }

    @Test
    public void testDataAttributes() throws Exception {
        final String html = "<html><body><div data-test=\"value\" data-foo_bar=\"test\" data-x.y=\"dot\">content</div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList divElements = doc.getElementsByTagName("DIV");
        assertEquals(1, divElements.getLength(), "Should have one DIV element");

        final Element divElement = (Element) divElements.item(0);

        // Test data-* attributes with various special characters
        assertEquals("value", divElement.getAttribute("data-test"), "data-test should be 'value'");
        assertEquals("test", divElement.getAttribute("data-foo_bar"), "data-foo_bar should be 'test'");
        assertEquals("dot", divElement.getAttribute("data-x.y"), "data-x.y should be 'dot'");
    }

    @Test
    public void testCustomElements() throws Exception {
        final String html = "<html><body><my-custom-element attr-name=\"value\">content</my-custom-element></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList customElements = doc.getElementsByTagName("MY-CUSTOM-ELEMENT");
        assertEquals(1, customElements.getLength(), "Should have one MY-CUSTOM-ELEMENT element");

        final Element customElement = (Element) customElements.item(0);
        assertEquals("value", customElement.getAttribute("attr-name"), "attr-name should be 'value'");
        assertEquals("content", customElement.getTextContent().trim(), "Element should contain 'content'");
    }

    // ========== HTML5 Semantic Elements ==========

    @Test
    public void testHTML5SemanticElements() throws Exception {
        final String html =
                "<html><body><header>Header</header><nav>Nav</nav><main><article>Article</article>"
                        + "<section>Section</section><aside>Aside</aside></main><footer>Footer</footer></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();

        assertEquals(1, doc.getElementsByTagName("HEADER").getLength(), "Should have one HEADER element");
        assertEquals(1, doc.getElementsByTagName("NAV").getLength(), "Should have one NAV element");
        assertEquals(1, doc.getElementsByTagName("MAIN").getLength(), "Should have one MAIN element");
        assertEquals(1, doc.getElementsByTagName("ARTICLE").getLength(), "Should have one ARTICLE element");
        assertEquals(1, doc.getElementsByTagName("SECTION").getLength(), "Should have one SECTION element");
        assertEquals(1, doc.getElementsByTagName("ASIDE").getLength(), "Should have one ASIDE element");
        assertEquals(1, doc.getElementsByTagName("FOOTER").getLength(), "Should have one FOOTER element");
    }

    @Test
    public void testHTML5ArticleStructure() throws Exception {
        final String html = "<html><body><article><h1>Title</h1><p>Content</p></article></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList articles = doc.getElementsByTagName("ARTICLE");
        assertEquals(1, articles.getLength(), "Should have one ARTICLE element");

        final Element article = (Element) articles.item(0);
        assertEquals(1, article.getElementsByTagName("H1").getLength(), "Article should contain one H1");
        assertEquals(1, article.getElementsByTagName("P").getLength(), "Article should contain one P");
    }

    @Test
    public void testHTML5SectionWithHeader() throws Exception {
        final String html = "<html><body><section><header><h2>Section Title</h2></header><p>Section content</p></section></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList sections = doc.getElementsByTagName("SECTION");
        assertEquals(1, sections.getLength(), "Should have one SECTION element");

        final Element section = (Element) sections.item(0);
        assertEquals(1, section.getElementsByTagName("HEADER").getLength(), "Section should contain one HEADER");
        assertEquals(1, section.getElementsByTagName("H2").getLength(), "Section should contain one H2");
    }

    @Test
    public void testHTML5FigureAndFigcaption() throws Exception {
        final String html = "<html><body><figure><img src=\"image.jpg\"><figcaption>Caption</figcaption></figure></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("FIGURE").getLength(), "Should have one FIGURE element");
        assertEquals(1, doc.getElementsByTagName("FIGCAPTION").getLength(), "Should have one FIGCAPTION element");
        assertEquals(1, doc.getElementsByTagName("IMG").getLength(), "Should have one IMG element");
    }

    @Test
    public void testHTML5TimeElement() throws Exception {
        final String html = "<html><body><time datetime=\"2025-10-04\">October 4, 2025</time></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList timeElements = doc.getElementsByTagName("TIME");
        assertEquals(1, timeElements.getLength(), "Should have one TIME element");

        final Element timeElement = (Element) timeElements.item(0);
        assertEquals("2025-10-04", timeElement.getAttribute("datetime"), "TIME should have datetime attribute");
        assertEquals("October 4, 2025", timeElement.getTextContent(), "TIME should contain text");
    }

    @Test
    public void testHTML5MarkElement() throws Exception {
        final String html = "<html><body><p>This is <mark>highlighted</mark> text</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("MARK").getLength(), "Should have one MARK element");

        final Element mark = (Element) doc.getElementsByTagName("MARK").item(0);
        assertEquals("highlighted", mark.getTextContent(), "MARK should contain 'highlighted'");
    }

    // ========== HTML5 Void Elements ==========

    @Test
    public void testHTML5VoidElements() throws Exception {
        final String html =
                "<html><head><meta charset=\"UTF-8\"><link rel=\"stylesheet\" href=\"style.css\">"
                        + "<base href=\"https://example.com/\"></head><body><img src=\"image.jpg\" alt=\"Image\">"
                        + "<input type=\"text\" name=\"field\"><br><hr><area shape=\"rect\" coords=\"0,0,100,100\">"
                        + "<embed src=\"file.swf\"><col><source src=\"audio.mp3\"><track src=\"captions.vtt\">"
                        + "<wbr><param name=\"autoplay\" value=\"true\"></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();

        // Verify all void elements are present
        assertTrue(doc.getElementsByTagName("META").getLength() > 0, "Should have META element");
        assertTrue(doc.getElementsByTagName("LINK").getLength() > 0, "Should have LINK element");
        assertTrue(doc.getElementsByTagName("BASE").getLength() > 0, "Should have BASE element");
        assertTrue(doc.getElementsByTagName("IMG").getLength() > 0, "Should have IMG element");
        assertTrue(doc.getElementsByTagName("INPUT").getLength() > 0, "Should have INPUT element");
        assertTrue(doc.getElementsByTagName("BR").getLength() > 0, "Should have BR element");
        assertTrue(doc.getElementsByTagName("HR").getLength() > 0, "Should have HR element");
        assertTrue(doc.getElementsByTagName("AREA").getLength() > 0, "Should have AREA element");
        assertTrue(doc.getElementsByTagName("EMBED").getLength() > 0, "Should have EMBED element");
        assertTrue(doc.getElementsByTagName("COL").getLength() > 0, "Should have COL element");
        assertTrue(doc.getElementsByTagName("SOURCE").getLength() > 0, "Should have SOURCE element");
        assertTrue(doc.getElementsByTagName("TRACK").getLength() > 0, "Should have TRACK element");
        assertTrue(doc.getElementsByTagName("WBR").getLength() > 0, "Should have WBR element");
        assertTrue(doc.getElementsByTagName("PARAM").getLength() > 0, "Should have PARAM element");
    }

    @Test
    public void testImageWithAttributes() throws Exception {
        final String html = "<html><body><img src=\"test.jpg\" alt=\"Test Image\" width=\"100\" height=\"200\"></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList images = doc.getElementsByTagName("IMG");
        assertEquals(1, images.getLength(), "Should have one IMG element");

        final Element img = (Element) images.item(0);
        assertEquals("test.jpg", img.getAttribute("src"), "IMG should have src attribute");
        assertEquals("Test Image", img.getAttribute("alt"), "IMG should have alt attribute");
        assertEquals("100", img.getAttribute("width"), "IMG should have width attribute");
        assertEquals("200", img.getAttribute("height"), "IMG should have height attribute");
    }

    @Test
    public void testInputWithVariousTypes() throws Exception {
        final String html =
                "<html><body><form><input type=\"text\" name=\"text1\">"
                        + "<input type=\"password\" name=\"pass1\"><input type=\"checkbox\" name=\"check1\">"
                        + "<input type=\"radio\" name=\"radio1\"><input type=\"submit\" value=\"Submit\">"
                        + "<input type=\"email\" name=\"email1\"><input type=\"number\" name=\"num1\">"
                        + "<input type=\"date\" name=\"date1\"><input type=\"file\" name=\"file1\"></form></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList inputs = doc.getElementsByTagName("INPUT");
        assertEquals(9, inputs.getLength(), "Should have 9 INPUT elements");

        // Verify types
        final Element textInput = (Element) inputs.item(0);
        assertEquals("text", textInput.getAttribute("type"), "First input should be type='text'");

        final Element emailInput = (Element) inputs.item(5);
        assertEquals("email", emailInput.getAttribute("type"), "Email input should be type='email'");
    }

    @Test
    public void testMetaWithCharset() throws Exception {
        final String html =
                "<html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width\">"
                        + "</head><body></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList metas = doc.getElementsByTagName("META");
        assertTrue(metas.getLength() >= 2, "Should have at least 2 META elements");

        // Find charset meta
        boolean foundCharset = false;
        boolean foundViewport = false;
        for (int i = 0; i < metas.getLength(); i++) {
            final Element meta = (Element) metas.item(i);
            if ("UTF-8".equals(meta.getAttribute("charset"))) {
                foundCharset = true;
            }
            if ("viewport".equals(meta.getAttribute("name"))) {
                foundViewport = true;
                assertEquals("width=device-width", meta.getAttribute("content"), "Viewport meta should have content");
            }
        }

        assertTrue(foundCharset, "Should have charset meta");
        assertTrue(foundViewport, "Should have viewport meta");
    }

    // ========== Table Structure Auto-completion ==========

    @Test
    public void testTableAutoCompletion() throws Exception {
        // Test that table structure is preserved
        final String html = "<html><body><table><tr><td>Cell</td></tr></table></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList tables = doc.getElementsByTagName("TABLE");
        assertEquals(1, tables.getLength(), "Should have one TABLE element");

        final NodeList trs = doc.getElementsByTagName("TR");
        assertEquals(1, trs.getLength(), "Should have one TR element");

        final NodeList tds = doc.getElementsByTagName("TD");
        assertEquals(1, tds.getLength(), "Should have one TD element");
    }

    @Test
    public void testComplexTableStructure() throws Exception {
        final String html =
                "<html><body><table><caption>Table Caption</caption><thead><tr><th>Header</th></tr></thead>"
                        + "<tbody><tr><td>Data</td></tr></tbody><tfoot><tr><td>Footer</td></tr></tfoot></table></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("TABLE").getLength(), "Should have one TABLE element");
        assertEquals(1, doc.getElementsByTagName("CAPTION").getLength(), "Should have one CAPTION element");
        assertEquals(1, doc.getElementsByTagName("THEAD").getLength(), "Should have one THEAD element");
        assertEquals(1, doc.getElementsByTagName("TBODY").getLength(), "Should have one TBODY element");
        assertEquals(1, doc.getElementsByTagName("TFOOT").getLength(), "Should have one TFOOT element");
        assertEquals(3, doc.getElementsByTagName("TR").getLength(), "Should have three TR elements");
    }

    @Test
    public void testTableWithColgroup() throws Exception {
        final String html =
                "<html><body><table><colgroup><col span=\"2\" style=\"background:red\"></colgroup>"
                        + "<tr><td>Cell 1</td><td>Cell 2</td></tr></table></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("COLGROUP").getLength(), "Should have one COLGROUP element");
        assertEquals(1, doc.getElementsByTagName("COL").getLength(), "Should have one COL element");

        final Element col = (Element) doc.getElementsByTagName("COL").item(0);
        assertEquals("2", col.getAttribute("span"), "COL should have span='2'");
    }

    @Test
    public void testTableWithColspanRowspan() throws Exception {
        final String html =
                "<html><body><table><tr><td colspan=\"2\">Wide Cell</td></tr>"
                        + "<tr><td rowspan=\"2\">Tall Cell</td><td>Normal</td></tr><tr><td>Normal</td></tr></table></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList tds = doc.getElementsByTagName("TD");
        assertTrue(tds.getLength() >= 4, "Should have at least 4 TD elements");

        // Check colspan
        final Element wideCell = (Element) tds.item(0);
        assertEquals("2", wideCell.getAttribute("colspan"), "First cell should have colspan='2'");

        // Check rowspan
        final Element tallCell = (Element) tds.item(1);
        assertEquals("2", tallCell.getAttribute("rowspan"), "Second cell should have rowspan='2'");
    }

    // ========== Entity Processing ==========

    @Test
    public void testHtmlEntities() throws Exception {
        final String html = "<html><body><p>Text with &amp; and &lt; and &gt;</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList pElements = doc.getElementsByTagName("P");
        assertEquals(1, pElements.getLength(), "Should have one P element");

        final String content = pElements.item(0).getTextContent();
        assertNotNull(content, "Content should not be null");
        assertTrue(content.length() > 0, "Content should have text");
    }

    @Test
    public void testNumericEntities() throws Exception {
        final String html = "<html><body><p>&#65; &#x41;</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final String content = doc.getElementsByTagName("P").item(0).getTextContent();
        assertNotNull(content, "Content should not be null");
        assertTrue(content.trim().length() > 0, "Content should have text");
    }

    @Test
    public void testEntityInAttribute() throws Exception {
        final String html = "<html><body><a href=\"page.html?foo=1&amp;bar=2\" title=\"Link\">Link</a></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final Element link = (Element) doc.getElementsByTagName("A").item(0);
        final String href = link.getAttribute("href");
        assertNotNull(href, "Href should not be null");
        assertTrue(href.contains("page.html"), "Href should contain page.html");

        final String title = link.getAttribute("title");
        assertEquals("Link", title, "Title should be 'Link'");
    }

    @Test
    public void testHtml5NamedEntities() throws Exception {
        final String html = "<html><body>&lpar;&excl;&hearts;&NotEqualTilde;</body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList bodyElements = doc.getElementsByTagName("BODY");
        assertEquals(1, bodyElements.getLength(), "Should have one BODY element");

        final String content = bodyElements.item(0).getTextContent();
        assertEquals("(!♥≂̸", content, "HTML5 named entities should resolve to their WHATWG values");
    }

    @Test
    public void testHtml5EntityInAttribute() throws Exception {
        final String html = "<html><body><a href=\"?a=1&lpar;2\">Link</a></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final Element link = (Element) doc.getElementsByTagName("A").item(0);
        final String href = link.getAttribute("href");
        assertEquals("?a=1(2", href, "Semicolon-terminated HTML5 named entity should resolve in an attribute value");
    }

    // ========== Malformed HTML Auto-correction ==========

    @Test
    public void testUnclosedTags() throws Exception {
        final String html = "<html><body><p>Unclosed paragraph<div>Div content</div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should be parsed despite unclosed tags");

        assertEquals(1, doc.getElementsByTagName("P").getLength(), "Should have one P element");
        assertEquals(1, doc.getElementsByTagName("DIV").getLength(), "Should have one DIV element");
    }

    @Test
    public void testMismatchedTags() throws Exception {
        final String html = "<html><body><b><i>Bold and italic</b></i></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should be parsed despite mismatched tags");

        assertTrue(doc.getElementsByTagName("B").getLength() > 0, "Should have B element");
        assertTrue(doc.getElementsByTagName("I").getLength() > 0, "Should have I element");
    }

    @Test
    public void testMissingHtmlStructure() throws Exception {
        final String html = "<p>Just a paragraph</p>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // HTML structure should be auto-added
        final Element root = doc.getDocumentElement();
        assertEquals("HTML", root.getNodeName(), "Root should be HTML element");

        assertEquals(1, doc.getElementsByTagName("P").getLength(), "Should have one P element");
    }

    @Test
    public void testNestedInlineElements() throws Exception {
        final String html = "<html><body><b><i><u>Nested inline</u></i></b></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("B").getLength(), "Should have one B element");
        assertEquals(1, doc.getElementsByTagName("I").getLength(), "Should have one I element");
        assertEquals(1, doc.getElementsByTagName("U").getLength(), "Should have one U element");
    }

    @Test
    public void testMissingClosingTagsForOptionalEndTags() throws Exception {
        final String html = "<html><body><ul><li>Item 1<li>Item 2<li>Item 3</ul></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("UL").getLength(), "Should have one UL element");
        assertEquals(3, doc.getElementsByTagName("LI").getLength(), "Should have three LI elements");
    }

    @Test
    public void testParagraphAutoClosing() throws Exception {
        final String html = "<html><body><p>First<p>Second<p>Third</body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList pElements = doc.getElementsByTagName("P");
        assertEquals(3, pElements.getLength(), "Should have three P elements");
    }

    // ========== Script and Style Elements ==========

    @Test
    public void testScriptElement() throws Exception {
        final String html = "<html><head><script type=\"text/javascript\">var x = 1;</script></head><body></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList scripts = doc.getElementsByTagName("SCRIPT");
        assertEquals(1, scripts.getLength(), "Should have one SCRIPT element");

        final Element script = (Element) scripts.item(0);
        assertEquals("text/javascript", script.getAttribute("type"), "Script should have type attribute");

        final String scriptContent = script.getTextContent();
        assertNotNull(scriptContent, "Script content should not be null");
    }

    @Test
    public void testStyleElement() throws Exception {
        final String html =
                "<html><head><style type=\"text/css\">body { margin: 0; } p > span { color: red; }</style></head><body></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList styles = doc.getElementsByTagName("STYLE");
        assertEquals(1, styles.getLength(), "Should have one STYLE element");

        final Element style = (Element) styles.item(0);
        assertEquals("text/css", style.getAttribute("type"), "Style should have type attribute");

        final String styleContent = style.getTextContent();
        assertTrue(styleContent.contains(">"), "Style content should preserve >");
    }

    @Test
    public void testExternalScriptAndStyleLinks() throws Exception {
        final String html =
                "<html><head><script src=\"app.js\"></script><link rel=\"stylesheet\" href=\"style.css\"></head><body></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();

        final NodeList scripts = doc.getElementsByTagName("SCRIPT");
        assertEquals(1, scripts.getLength(), "Should have one SCRIPT element");
        final Element script = (Element) scripts.item(0);
        assertEquals("app.js", script.getAttribute("src"), "Script should have src attribute");

        final NodeList links = doc.getElementsByTagName("LINK");
        assertEquals(1, links.getLength(), "Should have one LINK element");
        final Element link = (Element) links.item(0);
        assertEquals("stylesheet", link.getAttribute("rel"), "Link should have rel='stylesheet'");
        assertEquals("style.css", link.getAttribute("href"), "Link should have href attribute");
    }

    // ========== Form and List Elements ==========

    @Test
    public void testFormWithVariousInputs() throws Exception {
        final String html =
                "<html><body><form action=\"/submit\" method=\"post\">" + "<input type=\"text\" name=\"username\" required>"
                        + "<input type=\"password\" name=\"password\" placeholder=\"Password\">"
                        + "<input type=\"email\" name=\"email\" value=\"test@example.com\">"
                        + "<input type=\"checkbox\" name=\"remember\" checked>" + "<input type=\"radio\" name=\"gender\" value=\"male\">"
                        + "<input type=\"radio\" name=\"gender\" value=\"female\">" + "<button type=\"submit\">Submit</button>"
                        + "</form></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList forms = doc.getElementsByTagName("FORM");
        assertEquals(1, forms.getLength(), "Should have one FORM element");

        final Element form = (Element) forms.item(0);
        assertEquals("/submit", form.getAttribute("action"), "Form should have action attribute");
        assertEquals("post", form.getAttribute("method"), "Form should have method attribute");

        assertEquals(6, doc.getElementsByTagName("INPUT").getLength(), "Should have 6 INPUT elements");
        assertEquals(1, doc.getElementsByTagName("BUTTON").getLength(), "Should have one BUTTON element");
    }

    @Test
    public void testSelectElement() throws Exception {
        final String html =
                "<html><body><form><select name=\"country\">" + "<option value=\"us\">United States</option>"
                        + "<option value=\"jp\" selected>Japan</option>" + "<option value=\"uk\">United Kingdom</option>"
                        + "</select></form></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("SELECT").getLength(), "Should have one SELECT element");
        assertEquals(3, doc.getElementsByTagName("OPTION").getLength(), "Should have three OPTION elements");

        final NodeList options = doc.getElementsByTagName("OPTION");
        final Element selectedOption = (Element) options.item(1);
        assertTrue(selectedOption.hasAttribute("selected"), "Second option should have selected attribute");
    }

    @Test
    public void testTextareaElement() throws Exception {
        final String html =
                "<html><body><form><textarea name=\"message\" rows=\"5\" cols=\"40\">Default text</textarea></form></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList textareas = doc.getElementsByTagName("TEXTAREA");
        assertEquals(1, textareas.getLength(), "Should have one TEXTAREA element");

        final Element textarea = (Element) textareas.item(0);
        assertEquals("message", textarea.getAttribute("name"), "Textarea should have name attribute");
        assertEquals("5", textarea.getAttribute("rows"), "Textarea should have rows attribute");
        assertEquals("40", textarea.getAttribute("cols"), "Textarea should have cols attribute");
        assertTrue(textarea.getTextContent().contains("Default text"), "Textarea should contain default text");
    }

    @Test
    public void testLabelElement() throws Exception {
        final String html =
                "<html><body><form><label for=\"username\">Username:</label>"
                        + "<input type=\"text\" id=\"username\" name=\"username\"></form></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList labels = doc.getElementsByTagName("LABEL");
        assertEquals(1, labels.getLength(), "Should have one LABEL element");

        final Element label = (Element) labels.item(0);
        assertEquals("username", label.getAttribute("for"), "Label should have for attribute");
    }

    @Test
    public void testFieldsetAndLegend() throws Exception {
        final String html =
                "<html><body><form><fieldset><legend>Personal Info</legend>"
                        + "<input type=\"text\" name=\"name\"></fieldset></form></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("FIELDSET").getLength(), "Should have one FIELDSET element");
        assertEquals(1, doc.getElementsByTagName("LEGEND").getLength(), "Should have one LEGEND element");
    }

    @Test
    public void testUnorderedList() throws Exception {
        final String html = "<html><body><ul><li>Item 1</li><li>Item 2</li><li>Item 3</li></ul></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("UL").getLength(), "Should have one UL element");
        assertEquals(3, doc.getElementsByTagName("LI").getLength(), "Should have three LI elements");
    }

    @Test
    public void testOrderedList() throws Exception {
        final String html = "<html><body><ol type=\"1\" start=\"5\"><li>Item 5</li><li>Item 6</li></ol></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList ols = doc.getElementsByTagName("OL");
        assertEquals(1, ols.getLength(), "Should have one OL element");

        final Element ol = (Element) ols.item(0);
        assertEquals("1", ol.getAttribute("type"), "OL should have type='1'");
        assertEquals("5", ol.getAttribute("start"), "OL should have start='5'");

        assertEquals(2, doc.getElementsByTagName("LI").getLength(), "Should have two LI elements");
    }

    @Test
    public void testDescriptionList() throws Exception {
        final String html =
                "<html><body><dl><dt>Term 1</dt><dd>Definition 1</dd>" + "<dt>Term 2</dt><dd>Definition 2</dd></dl></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("DL").getLength(), "Should have one DL element");
        assertEquals(2, doc.getElementsByTagName("DT").getLength(), "Should have two DT elements");
        assertEquals(2, doc.getElementsByTagName("DD").getLength(), "Should have two DD elements");
    }

    @Test
    public void testNestedLists() throws Exception {
        final String html = "<html><body><ul><li>Item 1<ul><li>Nested 1</li><li>Nested 2</li></ul></li><li>Item 2</li></ul></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(2, doc.getElementsByTagName("UL").getLength(), "Should have two UL elements");
        assertEquals(4, doc.getElementsByTagName("LI").getLength(), "Should have four LI elements");
    }

    // ========== Edge Cases and Special Scenarios ==========

    @Test
    public void testDoctypeHandling() throws Exception {
        final String html = "<!DOCTYPE html><html><head><title>Test</title></head><body><p>Content</p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");

        // Verify the document has the expected structure
        assertEquals(1, doc.getElementsByTagName("HTML").getLength(), "Should have HTML element");
        assertEquals(1, doc.getElementsByTagName("HEAD").getLength(), "Should have HEAD element");
        assertEquals(1, doc.getElementsByTagName("BODY").getLength(), "Should have BODY element");
        assertEquals(1, doc.getElementsByTagName("P").getLength(), "Should have P element");
    }

    @Test
    public void testCDATASection() throws Exception {
        final String html = "<html><body><script><![CDATA[function test() { return 1 < 2; }]]></script></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");
        assertEquals(1, doc.getElementsByTagName("SCRIPT").getLength(), "Should have one SCRIPT element");
    }

    @Test
    public void testAttributesWithoutQuotes() throws Exception {
        final String html = "<html><body><div id=test class=container data-value=123>Content</div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertEquals("test", div.getAttribute("id"), "Should parse unquoted id attribute");
        assertEquals("container", div.getAttribute("class"), "Should parse unquoted class attribute");
        assertEquals("123", div.getAttribute("data-value"), "Should parse unquoted data-value attribute");
    }

    @Test
    public void testAttributesWithSingleQuotes() throws Exception {
        final String html = "<html><body><div id='test' class='container'>Content</div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertEquals("test", div.getAttribute("id"), "Should parse single-quoted id attribute");
        assertEquals("container", div.getAttribute("class"), "Should parse single-quoted class attribute");
    }

    @Test
    public void testBooleanAttributes() throws Exception {
        final String html = "<html><body><input type=\"checkbox\" checked disabled readonly required></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final Element input = (Element) doc.getElementsByTagName("INPUT").item(0);
        assertTrue(input.hasAttribute("checked"), "Should have checked attribute");
        assertTrue(input.hasAttribute("disabled"), "Should have disabled attribute");
        assertTrue(input.hasAttribute("readonly"), "Should have readonly attribute");
        assertTrue(input.hasAttribute("required"), "Should have required attribute");
    }

    @Test
    public void testWhitespaceHandling() throws Exception {
        final String html = "<html>  <body>  \n  <p>  Text  with  spaces  </p>  \n  </body>  </html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertNotNull(doc, "Document should not be null");
        assertEquals(1, doc.getElementsByTagName("P").getLength(), "Should have one P element");
    }

    @Test
    public void testMultipleClasses() throws Exception {
        final String html = "<html><body><div class=\"class1 class2 class3\">Content</div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertEquals("class1 class2 class3", div.getAttribute("class"), "Should preserve multiple classes");
    }

    @Test
    public void testEmptyAttributes() throws Exception {
        final String html = "<html><body><div id=\"\" class=\"\" title=\"\">Content</div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);
        assertEquals("", div.getAttribute("id"), "Should handle empty id attribute");
        assertEquals("", div.getAttribute("class"), "Should handle empty class attribute");
        assertEquals("", div.getAttribute("title"), "Should handle empty title attribute");
    }

    @Test
    public void testHeadingElements() throws Exception {
        final String html = "<html><body><h1>H1</h1><h2>H2</h2><h3>H3</h3><h4>H4</h4><h5>H5</h5><h6>H6</h6></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("H1").getLength(), "Should have one H1 element");
        assertEquals(1, doc.getElementsByTagName("H2").getLength(), "Should have one H2 element");
        assertEquals(1, doc.getElementsByTagName("H3").getLength(), "Should have one H3 element");
        assertEquals(1, doc.getElementsByTagName("H4").getLength(), "Should have one H4 element");
        assertEquals(1, doc.getElementsByTagName("H5").getLength(), "Should have one H5 element");
        assertEquals(1, doc.getElementsByTagName("H6").getLength(), "Should have one H6 element");
    }

    @Test
    public void testAnchorElement() throws Exception {
        final String html =
                "<html><body><a href=\"https://example.com\" target=\"_blank\" rel=\"noopener noreferrer\">Link</a></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList anchors = doc.getElementsByTagName("A");
        assertEquals(1, anchors.getLength(), "Should have one A element");

        final Element anchor = (Element) anchors.item(0);
        assertEquals("https://example.com", anchor.getAttribute("href"), "A should have href attribute");
        assertEquals("_blank", anchor.getAttribute("target"), "A should have target attribute");
        assertEquals("noopener noreferrer", anchor.getAttribute("rel"), "A should have rel attribute");
    }

    @Test
    public void testPreformattedText() throws Exception {
        final String html = "<html><body><pre>  Line 1\n  Line 2\n  Line 3  </pre></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList preElements = doc.getElementsByTagName("PRE");
        assertEquals(1, preElements.getLength(), "Should have one PRE element");

        final String content = preElements.item(0).getTextContent();
        assertTrue(content.contains("\n"), "PRE content should preserve newlines");
    }

    @Test
    public void testBlockquoteAndCite() throws Exception {
        final String html = "<html><body><blockquote cite=\"https://example.com\"><p>Quote</p></blockquote></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList blockquotes = doc.getElementsByTagName("BLOCKQUOTE");
        assertEquals(1, blockquotes.getLength(), "Should have one BLOCKQUOTE element");

        final Element blockquote = (Element) blockquotes.item(0);
        assertEquals("https://example.com", blockquote.getAttribute("cite"), "BLOCKQUOTE should have cite attribute");
    }

    @Test
    public void testCodeAndKbd() throws Exception {
        final String html = "<html><body><p>Press <kbd>Ctrl</kbd>+<kbd>C</kbd> to <code>copy()</code></p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(2, doc.getElementsByTagName("KBD").getLength(), "Should have two KBD elements");
        assertEquals(1, doc.getElementsByTagName("CODE").getLength(), "Should have one CODE element");
    }

    @Test
    public void testAbbrAndAcronym() throws Exception {
        final String html = "<html><body><abbr title=\"HyperText Markup Language\">HTML</abbr></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList abbrs = doc.getElementsByTagName("ABBR");
        assertEquals(1, abbrs.getLength(), "Should have one ABBR element");

        final Element abbr = (Element) abbrs.item(0);
        assertEquals("HyperText Markup Language", abbr.getAttribute("title"), "ABBR should have title attribute");
    }

    @Test
    public void testSpanWithStyle() throws Exception {
        final String html = "<html><body><p>Text with <span style=\"color: red; font-weight: bold;\">styled span</span></p></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList spans = doc.getElementsByTagName("SPAN");
        assertEquals(1, spans.getLength(), "Should have one SPAN element");

        final Element span = (Element) spans.item(0);
        assertTrue(span.getAttribute("style").contains("color"), "SPAN should have style attribute with color");
    }

    @Test
    public void testDivWithMultipleAttributes() throws Exception {
        final String html =
                "<html><body><div id=\"main\" class=\"container wrapper\" style=\"display: block;\" "
                        + "data-id=\"123\" aria-label=\"Main content\" role=\"main\">Content</div></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final Element div = (Element) doc.getElementsByTagName("DIV").item(0);

        assertEquals("main", div.getAttribute("id"), "DIV should have id");
        assertEquals("container wrapper", div.getAttribute("class"), "DIV should have class");
        assertTrue(div.getAttribute("style").contains("display"), "DIV should have style");
        assertEquals("123", div.getAttribute("data-id"), "DIV should have data-id");
        assertEquals("Main content", div.getAttribute("aria-label"), "DIV should have aria-label");
        assertEquals("main", div.getAttribute("role"), "DIV should have role");
    }

    @Test
    public void testIframeElement() throws Exception {
        final String html = "<html><body><iframe src=\"page.html\" width=\"800\" height=\"600\" frameborder=\"0\"></iframe></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList iframes = doc.getElementsByTagName("IFRAME");
        assertEquals(1, iframes.getLength(), "Should have one IFRAME element");

        final Element iframe = (Element) iframes.item(0);
        assertEquals("page.html", iframe.getAttribute("src"), "IFRAME should have src attribute");
        assertEquals("800", iframe.getAttribute("width"), "IFRAME should have width attribute");
    }

    @Test
    public void testVideoElement() throws Exception {
        final String html =
                "<html><body><video width=\"640\" height=\"480\" controls>" + "<source src=\"video.mp4\" type=\"video/mp4\">"
                        + "<source src=\"video.webm\" type=\"video/webm\">" + "</video></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("VIDEO").getLength(), "Should have one VIDEO element");
        assertEquals(2, doc.getElementsByTagName("SOURCE").getLength(), "Should have two SOURCE elements");

        final Element video = (Element) doc.getElementsByTagName("VIDEO").item(0);
        assertTrue(video.hasAttribute("controls"), "VIDEO should have controls attribute");
    }

    @Test
    public void testAudioElement() throws Exception {
        final String html =
                "<html><body><audio controls autoplay loop>" + "<source src=\"audio.mp3\" type=\"audio/mpeg\">" + "</audio></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("AUDIO").getLength(), "Should have one AUDIO element");
        assertEquals(1, doc.getElementsByTagName("SOURCE").getLength(), "Should have one SOURCE element");

        final Element audio = (Element) doc.getElementsByTagName("AUDIO").item(0);
        assertTrue(audio.hasAttribute("controls"), "AUDIO should have controls attribute");
        assertTrue(audio.hasAttribute("autoplay"), "AUDIO should have autoplay attribute");
        assertTrue(audio.hasAttribute("loop"), "AUDIO should have loop attribute");
    }

    @Test
    public void testCanvasElement() throws Exception {
        final String html = "<html><body><canvas id=\"myCanvas\" width=\"200\" height=\"100\">Fallback text</canvas></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList canvases = doc.getElementsByTagName("CANVAS");
        assertEquals(1, canvases.getLength(), "Should have one CANVAS element");

        final Element canvas = (Element) canvases.item(0);
        assertEquals("myCanvas", canvas.getAttribute("id"), "CANVAS should have id attribute");
        assertEquals("200", canvas.getAttribute("width"), "CANVAS should have width attribute");
    }

    @Test
    public void testProgressAndMeter() throws Exception {
        final String html =
                "<html><body><progress value=\"70\" max=\"100\">70%</progress>"
                        + "<meter value=\"0.6\" min=\"0\" max=\"1\">60%</meter></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("PROGRESS").getLength(), "Should have one PROGRESS element");
        assertEquals(1, doc.getElementsByTagName("METER").getLength(), "Should have one METER element");

        final Element progress = (Element) doc.getElementsByTagName("PROGRESS").item(0);
        assertEquals("70", progress.getAttribute("value"), "PROGRESS should have value attribute");
        assertEquals("100", progress.getAttribute("max"), "PROGRESS should have max attribute");
    }

    @Test
    public void testDetailsAndSummary() throws Exception {
        final String html = "<html><body><details open><summary>Summary</summary><p>Details content</p></details></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("DETAILS").getLength(), "Should have one DETAILS element");
        assertEquals(1, doc.getElementsByTagName("SUMMARY").getLength(), "Should have one SUMMARY element");

        final Element details = (Element) doc.getElementsByTagName("DETAILS").item(0);
        assertTrue(details.hasAttribute("open"), "DETAILS should have open attribute");
    }

    @Test
    public void testDialogElement() throws Exception {
        final String html = "<html><body><dialog open><p>Dialog content</p></dialog></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("DIALOG").getLength(), "Should have one DIALOG element");

        final Element dialog = (Element) doc.getElementsByTagName("DIALOG").item(0);
        assertTrue(dialog.hasAttribute("open"), "DIALOG should have open attribute");
    }

    @Test
    public void testOutputElement() throws Exception {
        final String html = "<html><body><form><output name=\"result\" for=\"a b\">0</output></form></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        final NodeList outputs = doc.getElementsByTagName("OUTPUT");
        assertEquals(1, outputs.getLength(), "Should have one OUTPUT element");

        final Element output = (Element) outputs.item(0);
        assertEquals("result", output.getAttribute("name"), "OUTPUT should have name attribute");
        assertEquals("a b", output.getAttribute("for"), "OUTPUT should have for attribute");
    }

    @Test
    public void testDatalistElement() throws Exception {
        final String html =
                "<html><body><input list=\"browsers\"><datalist id=\"browsers\">"
                        + "<option value=\"Chrome\"><option value=\"Firefox\"><option value=\"Safari\">" + "</datalist></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("DATALIST").getLength(), "Should have one DATALIST element");
        assertEquals(3, doc.getElementsByTagName("OPTION").getLength(), "Should have three OPTION elements");
    }

    @Test
    public void testMapAndArea() throws Exception {
        final String html =
                "<html><body><img src=\"map.jpg\" usemap=\"#mymap\">"
                        + "<map name=\"mymap\"><area shape=\"rect\" coords=\"0,0,100,100\" href=\"page1.html\">"
                        + "<area shape=\"circle\" coords=\"200,200,50\" href=\"page2.html\"></map></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("MAP").getLength(), "Should have one MAP element");
        assertEquals(2, doc.getElementsByTagName("AREA").getLength(), "Should have two AREA elements");

        final Element map = (Element) doc.getElementsByTagName("MAP").item(0);
        assertEquals("mymap", map.getAttribute("name"), "MAP should have name attribute");
    }

    @Test
    public void testObjectAndParam() throws Exception {
        final String html =
                "<html><body><object data=\"file.swf\" type=\"application/x-shockwave-flash\">"
                        + "<param name=\"quality\" value=\"high\"><param name=\"bgcolor\" value=\"#ffffff\">" + "</object></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("OBJECT").getLength(), "Should have one OBJECT element");
        assertEquals(2, doc.getElementsByTagName("PARAM").getLength(), "Should have two PARAM elements");
    }

    @Test
    public void testPictureElement() throws Exception {
        final String html =
                "<html><body><picture>" + "<source srcset=\"image-large.jpg\" media=\"(min-width: 800px)\">"
                        + "<source srcset=\"image-small.jpg\" media=\"(max-width: 799px)\">"
                        + "<img src=\"image-fallback.jpg\" alt=\"Image\">" + "</picture></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("PICTURE").getLength(), "Should have one PICTURE element");
        assertEquals(2, doc.getElementsByTagName("SOURCE").getLength(), "Should have two SOURCE elements");
        assertEquals(1, doc.getElementsByTagName("IMG").getLength(), "Should have one IMG element");
    }

    @Test
    public void testTemplateElement() throws Exception {
        final String html = "<html><body><template id=\"mytemplate\"><p>Template content</p></template></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("TEMPLATE").getLength(), "Should have one TEMPLATE element");

        final Element template = (Element) doc.getElementsByTagName("TEMPLATE").item(0);
        assertEquals("mytemplate", template.getAttribute("id"), "TEMPLATE should have id attribute");
    }

    @Test
    public void testSlotElement() throws Exception {
        final String html = "<html><body><template><slot name=\"header\">Default header</slot></template></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("SLOT").getLength(), "Should have one SLOT element");

        final Element slot = (Element) doc.getElementsByTagName("SLOT").item(0);
        assertEquals("header", slot.getAttribute("name"), "SLOT should have name attribute");
    }

    @Test
    public void testRubyAnnotation() throws Exception {
        final String html = "<html><body><ruby>漢<rp>(</rp><rt>かん</rt><rp>)</rp>字<rp>(</rp><rt>じ</rt><rp>)</rp></ruby></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("RUBY").getLength(), "Should have one RUBY element");
        assertEquals(2, doc.getElementsByTagName("RT").getLength(), "Should have two RT elements");
        assertEquals(4, doc.getElementsByTagName("RP").getLength(), "Should have four RP elements");
    }

    @Test
    public void testBdiAndBdo() throws Exception {
        final String html = "<html><body><bdi>مرحبا</bdi><bdo dir=\"rtl\">Hello</bdo></body></html>";

        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));

        final Document doc = parser.getDocument();
        assertEquals(1, doc.getElementsByTagName("BDI").getLength(), "Should have one BDI element");
        assertEquals(1, doc.getElementsByTagName("BDO").getLength(), "Should have one BDO element");

        final Element bdo = (Element) doc.getElementsByTagName("BDO").item(0);
        assertEquals("rtl", bdo.getAttribute("dir"), "BDO should have dir attribute");
    }

} // class DOMParserTest
