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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codelibs.nekohtml.parsers.DOMParser;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Structural tests for {@link HTMLTagBalancerFilter}: HEAD/BODY synthesis,
 * implied end tags, stray-end-tag suppression, and the balance invariant
 * (every startElement has exactly one matching endElement, and no endElement
 * is emitted without a prior matching startElement).
 *
 * @author CodeLibs Project
 */
public class TagBalancerStructureTest {

    private static Document parse(final String html) throws Exception {
        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        return parser.getDocument();
    }

    /**
     * ContentHandler that records the event stream and continuously validates
     * that end elements match the innermost open start element.
     */
    private static final class RecordingHandler extends DefaultHandler {
        final List<String> events = new ArrayList<>();
        final Deque<String> open = new ArrayDeque<>();
        final Map<String, Integer> starts = new HashMap<>();
        final Map<String, Integer> ends = new HashMap<>();
        boolean balanced = true;
        String firstImbalance = null;

        @Override
        public void startElement(final String uri, final String localName, final String qName, final org.xml.sax.Attributes atts) {
            events.add("START:" + qName);
            starts.merge(qName, 1, Integer::sum);
            open.push(qName);
        }

        @Override
        public void endElement(final String uri, final String localName, final String qName) {
            events.add("END:" + qName);
            ends.merge(qName, 1, Integer::sum);
            if (open.isEmpty()) {
                fail("END " + qName + " emitted with no open element");
                return;
            }
            final String top = open.pop();
            if (!top.equals(qName) && balanced) {
                balanced = false;
                firstImbalance = "END " + qName + " but innermost open was " + top;
            }
        }
    }

    private static RecordingHandler record(final String html) throws Exception {
        final HTMLSAXConfiguration config = new HTMLSAXConfiguration();
        final RecordingHandler handler = new RecordingHandler();
        config.setContentHandler(handler);
        config.parse(new InputSource(new StringReader(html)));
        return handler;
    }

    @Test
    public void testBodySynthesized() throws Exception {
        final Document doc = parse("<p>hello</p>");
        final NodeList body = doc.getElementsByTagName("BODY");
        assertEquals(1, body.getLength());
        assertEquals("HTML", ((Element) body.item(0)).getParentNode().getNodeName());
        assertEquals("BODY", doc.getElementsByTagName("P").item(0).getParentNode().getNodeName());
    }

    @Test
    public void testHeadSynthesizedForTitle() throws Exception {
        final Document doc = parse("<title>x</title><p>y</p>");
        assertEquals("HEAD", doc.getElementsByTagName("TITLE").item(0).getParentNode().getNodeName());
        assertEquals("BODY", doc.getElementsByTagName("P").item(0).getParentNode().getNodeName());
    }

    @Test
    public void testLiSiblings() throws Exception {
        final Document doc = parse("<html><body><ul><li>a<li>b</ul></body></html>");
        final NodeList lis = doc.getElementsByTagName("LI");
        assertEquals(2, lis.getLength());
        assertEquals("UL", lis.item(0).getParentNode().getNodeName());
        assertEquals("UL", lis.item(1).getParentNode().getNodeName());
    }

    @Test
    public void testNestedListStillNests() throws Exception {
        final Document doc = parse("<html><body><ul><li>a<ul><li>b</ul></li></ul></body></html>");
        final NodeList uls = doc.getElementsByTagName("UL");
        assertEquals("LI", uls.item(1).getParentNode().getNodeName());
    }

    @Test
    public void testPSiblings() throws Exception {
        final Document doc = parse("<html><body><p>one<p>two</body></html>");
        final NodeList ps = doc.getElementsByTagName("P");
        assertEquals(2, ps.getLength());
        assertEquals("BODY", ps.item(1).getParentNode().getNodeName());
    }

    @Test
    public void testTdSiblings() throws Exception {
        final Document doc = parse("<html><body><table><tr><td>x<td>y</table></body></html>");
        final NodeList tds = doc.getElementsByTagName("TD");
        assertEquals(2, tds.getLength());
        assertEquals("TR", tds.item(1).getParentNode().getNodeName());
    }

    @Test
    public void testStrayEndTagIgnoredAndBalanced() throws Exception {
        final RecordingHandler h = record("<html><body><p>one</p></div><p>two</p></body></html>");
        assertTrue(h.balanced, h.firstImbalance);
        // No stray DIV should appear in the stream at all.
        assertFalse(h.ends.containsKey("DIV"), "stray </div> should be ignored");
        assertFalse(h.starts.containsKey("DIV"), "no DIV should be started");
        // Per-name start/end counts must match.
        for (final String name : h.starts.keySet()) {
            assertEquals(h.starts.get(name), h.ends.get(name), "start/end mismatch for " + name);
        }
        assertEquals(2, h.starts.getOrDefault("P", 0), "two P elements");
    }

    @Test
    public void testFormattingEndBalanced() throws Exception {
        final RecordingHandler h = record("<b>bold<div>block</b>after</div>");
        assertTrue(h.balanced, h.firstImbalance);
        for (final String name : h.starts.keySet()) {
            assertEquals(h.starts.get(name), h.ends.get(name), "start/end mismatch for " + name);
        }

        // 'after' must not be inside B (bold does not leak past </b>).
        final Document doc = parse("<b>bold<div>block</b>after</div>");
        final NodeList bs = doc.getElementsByTagName("B");
        assertEquals(1, bs.getLength());
        assertFalse(bs.item(0).getTextContent().contains("after"), "B must not contain post-</b> text");
    }

    @Test
    public void testAttributesPreservedOnReopen() throws Exception {
        final Document doc = parse("<html><body><b><div id=\"d1\">x</b>y</div></body></html>");
        // The DIV that contains "y" (reopened) must still carry id="d1".
        boolean found = false;
        final NodeList divs = doc.getElementsByTagName("DIV");
        for (int i = 0; i < divs.getLength(); i++) {
            if ("d1".equals(((Element) divs.item(i)).getAttribute("id")) && divs.item(i).getTextContent().contains("y")) {
                found = true;
            }
        }
        assertTrue(found, "reopened DIV should preserve id=d1 and contain 'y'");
    }

    @Test
    public void testLateContentStaysInBody() throws Exception {
        final Document doc = parse("<html><body>a</body>b</html>");
        assertTrue(doc.getElementsByTagName("BODY").item(0).getTextContent().contains("b"), "late content stays in BODY");
    }

    @Test
    public void testEmptyDocumentSynthesizesNothing() throws Exception {
        final RecordingHandler h = record("");
        assertTrue(h.events.isEmpty(), "empty document must not synthesize elements");
    }

    /**
     * Deeply nested elements plus a large run of stray end tags must be handled
     * without quadratic blow-up: the open-element lookup is O(1), so this stays
     * far below the (deliberately generous) time bound. Also asserts the stray
     * end tags are still correctly suppressed and the stream stays balanced.
     */
    @Test
    public void testStrayEndTagHandlingIsNotQuadratic() throws Exception {
        final int n = 10_000;
        final StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        for (int i = 0; i < n; i++) {
            sb.append("<div>");
        }
        for (int i = 0; i < n; i++) {
            sb.append("</span>"); // stray: never opened
        }
        for (int i = 0; i < n; i++) {
            sb.append("</div>");
        }
        sb.append("</body></html>");

        final long startNanos = System.nanoTime();
        final RecordingHandler h = record(sb.toString());
        final long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;

        assertTrue(h.balanced, h.firstImbalance);
        assertFalse(h.starts.containsKey("SPAN"), "stray </span> must never start a SPAN");
        assertFalse(h.ends.containsKey("SPAN"), "stray </span> must be ignored");
        assertEquals(Integer.valueOf(n), h.starts.get("DIV"), "all DIVs opened");
        assertEquals(Integer.valueOf(n), h.ends.get("DIV"), "all DIVs closed");
        assertTrue(elapsedMs < 4_000L, "stray-end handling must not be quadratic (took " + elapsedMs + "ms)");
    }

    @Test
    public void testLegacyBlockClosesParagraph() throws Exception {
        // <center> (and dir/listing/plaintext/summary/xmp) are block-level and close an open <p>,
        // producing siblings rather than nesting the block inside the paragraph.
        final Document doc = parse("<html><body><p>a<center>b</center></body></html>");
        assertEquals("BODY", doc.getElementsByTagName("CENTER").item(0).getParentNode().getNodeName());
        assertEquals(0, ((Element) doc.getElementsByTagName("P").item(0)).getElementsByTagName("CENTER").getLength(),
                "CENTER must not be nested inside P");
    }

    @Test
    public void testCaptionClosedByRow() throws Exception {
        final Document doc = parse("<html><body><table><caption>cap<tr><td>x</table></body></html>");
        final NodeList caption = doc.getElementsByTagName("CAPTION");
        assertEquals(1, caption.getLength());
        assertEquals("cap", caption.item(0).getTextContent());
        assertEquals(0, ((Element) caption.item(0)).getElementsByTagName("TR").getLength(), "TR must not be nested inside CAPTION");
    }

    @Test
    public void testFormattingEndReopensContainersInOriginalOrder() throws Exception {
        // <b><div><span>text</b> closes B and reopens its enclosing containers; the reopened pair
        // must preserve the original DIV > SPAN (outer -> inner) nesting.
        final Document doc = parse("<html><body><b><div><span>text</b>more</body></html>");
        final NodeList divs = doc.getElementsByTagName("DIV");
        final NodeList spans = doc.getElementsByTagName("SPAN");
        assertEquals(2, divs.getLength());
        assertEquals(2, spans.getLength());
        assertEquals("DIV", spans.item(1).getParentNode().getNodeName(), "reopened SPAN must stay inside reopened DIV");
        assertEquals("BODY", divs.item(1).getParentNode().getNodeName());
        assertEquals("more", spans.item(1).getTextContent());
    }

    @Test
    public void testFormattingEndKeepsEventsBalanced() throws Exception {
        final RecordingHandler h = record("<html><body><b><div><span>text</b>more</body></html>");
        assertTrue(h.balanced, h.firstImbalance);
        assertEquals(h.starts.get("DIV"), h.ends.get("DIV"), "every DIV start has a matching end");
        assertEquals(h.starts.get("SPAN"), h.ends.get("SPAN"), "every SPAN start has a matching end");
        assertEquals(h.starts.get("B"), h.ends.get("B"), "every B start has a matching end");
    }

    /**
     * Misnested inline formatting reconstructs the active formatting elements
     * (Adoption-Agency-style): closing {@code </b>} in {@code <b><i>x</b>y} closes I and B and then
     * reopens I, so the trailing "y" stays italic. This matches the long-standing (pre-3.0.4)
     * behavior downstream consumers depend on; all text is preserved and the stream stays balanced.
     */
    @Test
    public void testMisnestedFormattingReopensInnerFormatting() throws Exception {
        final Document doc = parse("<html><body><b><i>x</b>y</body></html>");
        assertEquals("xy", doc.getElementsByTagName("BODY").item(0).getTextContent(), "misnested <b><i>x</b>y must preserve all text");
        final NodeList is = doc.getElementsByTagName("I");
        assertEquals(2, is.getLength(), "inner <i> must be reopened for the post-</b> text");
        assertEquals("x", is.item(0).getTextContent());
        assertEquals("y", is.item(1).getTextContent(), "trailing text stays wrapped in a reopened <i>");
    }

    /**
     * A stray end tag (no matching open element) is suppressed rather than passed through, but the
     * surrounding text must remain intact and contiguous.
     */
    @Test
    public void testStrayEndTagPreservesAllText() throws Exception {
        final Document doc = parse("<html><body><p>text</span>more</p></body></html>");
        assertEquals("textmore", doc.getElementsByTagName("BODY").item(0).getTextContent(),
                "a stray </span> must not drop surrounding text");
    }
}
