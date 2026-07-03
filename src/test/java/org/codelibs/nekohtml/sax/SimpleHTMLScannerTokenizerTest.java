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
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.codelibs.nekohtml.parsers.DOMParser;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

/**
 * Tests for the single-pass, quote-aware {@link SimpleHTMLScanner} tokenizer,
 * including raw-text/RCDATA element support and correct handling of comments,
 * DOCTYPE declarations, bogus comments and unterminated constructs.
 *
 * @author CodeLibs Project
 */
public class SimpleHTMLScannerTokenizerTest {

    private Document parse(final String html) throws Exception {
        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        return parser.getDocument();
    }

    @Test
    public void testScriptContentIsRawText() throws Exception {
        final Document doc = parse("<html><head><script>var x='<div>a</div>'; if(a<b){}</script></head><body>ok</body></html>");
        final NodeList scripts = doc.getElementsByTagName("SCRIPT");
        assertEquals(1, scripts.getLength());
        assertEquals(0, ((Element) scripts.item(0)).getElementsByTagName("*").getLength(), "script must have no element children");
        assertEquals("var x='<div>a</div>'; if(a<b){}", scripts.item(0).getTextContent());
        assertEquals("ok", doc.getElementsByTagName("BODY").item(0).getTextContent());
    }

    @Test
    public void testLtInTextPreserved() throws Exception {
        final Document doc = parse("<html><body>1 < 2 and a<b< c</body></html>");
        assertEquals("1 < 2 and a", doc.getElementsByTagName("BODY").item(0).getFirstChild().getTextContent());
        // "<b<" IS a tag start (letter follows '<') so a B element is correct; "< c" inside it is text
    }

    @Test
    public void testGtInsideQuotedAttribute() throws Exception {
        final Document doc = parse("<html><body><a title=\"a>b\" href=\"x\">link</a></body></html>");
        final Element a = (Element) doc.getElementsByTagName("A").item(0);
        assertEquals("a>b", a.getAttribute("title"));
        assertEquals("x", a.getAttribute("href"));
        assertEquals("link", a.getTextContent());
    }

    @Test
    public void testWhitespaceAroundEquals() throws Exception {
        final Document doc = parse("<html><body><a href = \"x\">link</a></body></html>");
        final Element a = (Element) doc.getElementsByTagName("A").item(0);
        assertEquals("x", a.getAttribute("href"));
        assertFalse(a.hasAttribute("x"));
    }

    @Test
    public void testUnterminatedCommentDoesNotLeak() throws Exception {
        final Document doc = parse("<html><body>before<!-- oops</body></html>");
        assertEquals("before", doc.getElementsByTagName("BODY").item(0).getTextContent());
    }

    @Test
    public void testProcessingInstructionDoesNotLeak() throws Exception {
        final Document doc = parse("<html><body><?php echo 1; ?>text</body></html>");
        assertEquals("text", doc.getElementsByTagName("BODY").item(0).getTextContent());
    }

    @Test
    public void testBogusCommentDoesNotLeak() throws Exception {
        final Document doc = parse("<html><body><!foo>text</body></html>");
        assertEquals("text", doc.getElementsByTagName("BODY").item(0).getTextContent());
    }

    @Test
    public void testTextareaIsRcdata() throws Exception {
        final Document doc = parse("<html><body><textarea><b>x</b> &amp; y</textarea></body></html>");
        final Node ta = doc.getElementsByTagName("TEXTAREA").item(0);
        assertEquals(0, ((Element) ta).getElementsByTagName("*").getLength());
        assertEquals("<b>x</b> & y", ta.getTextContent());
    }

    @Test
    public void testDoctypeIdsReported() throws Exception {
        // Drive the scanner directly with a recording LexicalHandler.
        final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
        final String[] captured = new String[3];
        final boolean[] endCalled = { false };
        scanner.setContentHandler(new DefaultHandler());
        scanner.setLexicalHandler(new LexicalHandler() {
            @Override
            public void startDTD(final String name, final String publicId, final String systemId) {
                captured[0] = name;
                captured[1] = publicId;
                captured[2] = systemId;
            }

            @Override
            public void endDTD() {
                endCalled[0] = true;
            }

            @Override
            public void startEntity(final String name) {
            }

            @Override
            public void endEntity(final String name) {
            }

            @Override
            public void startCDATA() {
            }

            @Override
            public void endCDATA() {
            }

            @Override
            public void comment(final char[] ch, final int start, final int length) {
            }
        });
        scanner.parse(new InputSource(new StringReader(
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\"><html></html>")));
        assertEquals("html", captured[0]);
        assertEquals("-//W3C//DTD HTML 4.01//EN", captured[1]);
        assertEquals("http://www.w3.org/TR/html4/strict.dtd", captured[2]);
        assertTrue(endCalled[0], "endDTD should be called");
    }

    @Test
    public void testEndTagWithAttributesAndQuotedGt() throws Exception {
        final Document doc = parse("<html><body><div>x</div foo=\">\"></body></html>");
        assertEquals("x", doc.getElementsByTagName("DIV").item(0).getTextContent());
        assertEquals(1, doc.getElementsByTagName("BODY").item(0).getChildNodes().getLength());
    }

    @Test
    public void testLargeDocumentLinearTime() throws Exception {
        final StringBuilder sb = new StringBuilder("<html><body>");
        for (int i = 0; i < 200_000; i++) {
            sb.append("<p class=\"c").append(i).append("\">x</p>");
        }
        sb.append("</body></html>");
        final long t0 = System.nanoTime();
        parse(sb.toString());
        assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - t0) < 20, "200k tags should parse in seconds, not minutes");
    }

    @Test
    public void testSelfClosingScriptStillEntersRawText() throws Exception {
        // HTML5 ignores the '/' on <script/>: it still enters script-data state, so the body
        // is raw text and must NOT be parsed as markup (no DIV/B element fabricated).
        final Document doc = parse("<html><body><script src=x />var a='<div>hi</div>';</script>after</body></html>");
        assertEquals(0, doc.getElementsByTagName("DIV").getLength(), "script body must not create a DIV");
        assertEquals(1, doc.getElementsByTagName("SCRIPT").getLength());
        assertEquals("var a='<div>hi</div>';", doc.getElementsByTagName("SCRIPT").item(0).getTextContent());
        assertEquals("after", doc.getElementsByTagName("BODY").item(0).getLastChild().getTextContent());
    }

    @Test
    public void testSelfClosingStyleStillEntersRawText() throws Exception {
        final Document doc = parse("<html><body><style/>.x{color:red}<b>bold</b></style>tail</body></html>");
        assertEquals(0, doc.getElementsByTagName("B").getLength(), "style body must not create a B");
        assertEquals(".x{color:red}<b>bold</b>", doc.getElementsByTagName("STYLE").item(0).getTextContent());
    }

    @Test
    public void testSelfClosingTextareaStillRcdata() throws Exception {
        final Document doc = parse("<html><body><textarea/>a<b>c</textarea>d</body></html>");
        assertEquals(0, doc.getElementsByTagName("B").getLength(), "textarea body must not create a B");
        assertEquals("a<b>c", doc.getElementsByTagName("TEXTAREA").item(0).getTextContent());
    }

    @Test
    public void testCommentEndBangTerminatesComment() throws Exception {
        // "--!>" is a valid comment terminator (HTML5 comment-end-bang state); the tail after it
        // must survive rather than being swallowed to EOF.
        final Document doc = parse("<html><body>before<!-- x --!>after</body></html>");
        assertEquals("beforeafter", doc.getElementsByTagName("BODY").item(0).getTextContent());
    }

    @Test
    public void testCommentEndBangPrefersEarliestTerminator() throws Exception {
        // A normal "-->" earlier than a later "--!>" still closes first.
        final Document doc = parse("<html><body>a<!-- c -->b --!> d</body></html>");
        assertEquals("ab --!> d", doc.getElementsByTagName("BODY").item(0).getTextContent());
    }

    @Test
    public void testAbruptEmptyComments() throws Exception {
        // "<!-->" and "<!--->" are abrupt-closing empty comments; content after them is text.
        assertEquals("ab", parse("<html><body>a<!-->b</body></html>").getElementsByTagName("BODY").item(0).getTextContent());
        assertEquals("ab", parse("<html><body>a<!--->b</body></html>").getElementsByTagName("BODY").item(0).getTextContent());
        // "<!---->" (already handled before the fix) remains an empty comment.
        assertEquals("ab", parse("<html><body>a<!---->b</body></html>").getElementsByTagName("BODY").item(0).getTextContent());
    }

    @Test
    public void testElementAndAttributeNameNormalizationIsLocaleIndependent() {
        // Under the Turkish locale, "title".toUpperCase() yields "TİTLE" (dotted capital I) and
        // "ID".toLowerCase() yields "ıd" (dotless i). Name normalization must use Locale.ROOT so
        // element/attribute names stay ASCII and structural matching keeps working everywhere.
        final Locale original = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            final SimpleHTMLScanner scanner = new SimpleHTMLScanner();
            scanner.setElementCase("upper");
            assertEquals("TITLE", scanner.normalizeElementName("title"));
            assertEquals("DIV", scanner.normalizeElementName("div"));
            scanner.setAttributeCase("lower");
            assertEquals("id", scanner.normalizeAttributeName("ID"));
        } finally {
            Locale.setDefault(original);
        }
    }

} // class SimpleHTMLScannerTokenizerTest
