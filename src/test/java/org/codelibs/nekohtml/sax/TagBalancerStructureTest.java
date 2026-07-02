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
}
