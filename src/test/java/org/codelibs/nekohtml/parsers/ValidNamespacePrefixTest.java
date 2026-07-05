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

import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.count;
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.first;
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.parse;
import static org.codelibs.nekohtml.parsers.ValidHtmlTestSupport.saxQNameUriLocal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Characterization tests locking in NekoHTML's handling of colon-prefixed tag/attribute names (e.g.
 * {@code <o:p>}, {@code xlink:href}) and of element/attribute name casing. NekoHTML does not perform
 * XML namespace processing: prefixes are kept verbatim (uppercased for elements) in the SAX qName /
 * DOM node name, {@code uri} is always empty, and {@code localName == qName}.
 */
public class ValidNamespacePrefixTest {

    // -----------------------------------------------------------------
    // Prefixed elements: SAX qName / uri / localName
    // -----------------------------------------------------------------

    @Test
    public void prefixedElementSaxQNameIsUppercasedWithColonKept() throws Exception {
        final List<String> out = saxQNameUriLocal("<o:p>x</o:p>");
        assertTrue(out.contains("O:P|uri=|local=O:P"), out.toString());
    }

    @Test
    public void prefixedElementSaxReportsEmptyUriAndLocalNameEqualsQName() throws Exception {
        final List<String> out = saxQNameUriLocal("<o:p>x</o:p>");
        final String entry = out.stream().filter(s -> s.startsWith("O:P|")).findFirst().orElseThrow();
        assertEquals("O:P|uri=|local=O:P", entry);
    }

    @Test
    public void multiplePrefixedElementsAreEachUppercasedIndependently() throws Exception {
        final List<String> out = saxQNameUriLocal("<fb:like></fb:like><og:image></og:image>");
        assertTrue(out.contains("FB:LIKE|uri=|local=FB:LIKE"), out.toString());
        assertTrue(out.contains("OG:IMAGE|uri=|local=OG:IMAGE"), out.toString());
    }

    @Test
    public void rootHtmlIsStillAutoInsertedAroundPrefixedContent() throws Exception {
        final List<String> out = saxQNameUriLocal("<o:p>x</o:p>");
        assertTrue(out.contains("HTML|uri=|local=HTML"), out.toString());
    }

    // -----------------------------------------------------------------
    // Prefixed elements: DOM node identity (namespaces not processed)
    // -----------------------------------------------------------------

    @Test
    public void prefixedElementDomNodeNameIsUppercasedWithColonKept() throws Exception {
        final Document doc = parse("<o:p>x</o:p>");
        final Element el = (Element) doc.getElementsByTagName("O:P").item(0);
        assertEquals("O:P", el.getNodeName());
        assertEquals("O:P", el.getTagName());
    }

    @Test
    public void prefixedElementHasNoNamespaceUri() throws Exception {
        final Document doc = parse("<o:p>x</o:p>");
        final Element el = (Element) doc.getElementsByTagName("O:P").item(0);
        // characterization: NekoHTML never runs namespace processing, so a colon-containing tag name
        // is just an (uppercased) local name, never split into prefix + namespace URI.
        assertNull(el.getNamespaceURI());
    }

    @Test
    public void prefixedElementHasNoPrefix() throws Exception {
        final Document doc = parse("<o:p>x</o:p>");
        final Element el = (Element) doc.getElementsByTagName("O:P").item(0);
        assertNull(el.getPrefix());
    }

    @Test
    public void prefixedElementHasNoLocalName() throws Exception {
        final Document doc = parse("<o:p>x</o:p>");
        final Element el = (Element) doc.getElementsByTagName("O:P").item(0);
        // characterization: getLocalName() is null (not namespace-aware), unlike the SAX localName
        // attribute value, which mirrors the qName instead.
        assertNull(el.getLocalName());
    }

    // -----------------------------------------------------------------
    // Namespaced/prefixed attribute names
    // -----------------------------------------------------------------

    @Test
    public void namespacedAttributeValueIsPreservedVerbatim() throws Exception {
        final Document doc = parse("<svg xmlns:xlink=\"http://x\"><use xlink:href=\"#a\"></use></svg>");
        final Element use = first(doc, "//*[name()='USE']");
        assertEquals("#a", use.getAttribute("xlink:href"));
    }

    @Test
    public void namespacedElementCountIsOne() throws Exception {
        final Document doc = parse("<svg xmlns:xlink=\"http://x\"><use xlink:href=\"#a\"></use></svg>");
        assertEquals(1, count(doc, "//*[name()='USE']"));
    }

    @Test
    public void namespacedAttributeNameIsLowercasedWithColonKept() throws Exception {
        final Document doc = parse("<svg xmlns:xlink=\"http://x\"><use XLink:HREF=\"#b\"></use></svg>");
        final Element use = first(doc, "//*[name()='USE']");
        // characterization: the attribute name is stored lowercased ("xlink:href"), so a
        // case-sensitive lookup using the original mixed-case source spelling misses entirely.
        assertEquals("#b", use.getAttribute("xlink:href"));
        assertEquals("", use.getAttribute("XLink:HREF"));
    }

    // -----------------------------------------------------------------
    // Element/attribute casing (independent of any prefix)
    // -----------------------------------------------------------------

    @Test
    public void elementCasingIsNormalizedToUppercaseRegardlessOfSourceCase() throws Exception {
        final Document doc = parse("<Div><SPAN>x</SPAN></div>");
        assertEquals(1, doc.getElementsByTagName("DIV").getLength());
        assertEquals(1, doc.getElementsByTagName("SPAN").getLength());
    }

    @Test
    public void lowercaseClosingTagStillClosesAMixedCaseOpeningTag() throws Exception {
        final Document doc = parse("<Div><SPAN>x</SPAN></div>");
        final Element div = first(doc, "//DIV");
        assertEquals("x", first(doc, "//SPAN").getTextContent());
        assertEquals(1, div.getElementsByTagName("SPAN").getLength());
    }

    @Test
    public void attributeNamesAreLowercasedRegardlessOfSourceCase() throws Exception {
        final Document doc = parse("<div ID=\"a\" Class=\"c\" DATA-X=\"1\">t</div>");
        final Element div = first(doc, "//DIV");
        assertEquals("a", div.getAttribute("id"));
        assertEquals("c", div.getAttribute("class"));
        assertEquals("1", div.getAttribute("data-x"));
    }

    @Test
    public void getAttributeIsCaseSensitiveOnTheStoredLowercaseName() throws Exception {
        final Document doc = parse("<div ID=\"a\" Class=\"c\" DATA-X=\"1\">t</div>");
        final Element div = first(doc, "//DIV");
        // characterization: DOM's getAttribute() does a case-sensitive lookup against the stored
        // (lowercased) attribute name, so querying with the original mixed-case spelling returns "".
        assertEquals("", div.getAttribute("ID"));
    }

    // -----------------------------------------------------------------
    // Custom/unknown elements
    // -----------------------------------------------------------------

    @Test
    public void customElementIsAcceptedAsAnOrdinaryContainer() throws Exception {
        final Document doc = parse("<my-widget><p>x</p></my-widget>");
        assertEquals(1, doc.getElementsByTagName("MY-WIDGET").getLength());
        final Element widget = first(doc, "//*[name()='MY-WIDGET']");
        assertEquals(1, widget.getElementsByTagName("P").getLength());
        assertEquals("x", widget.getElementsByTagName("P").item(0).getTextContent());
    }

    // -----------------------------------------------------------------
    // SAX uri is always empty (no namespace processing at all)
    // -----------------------------------------------------------------

    @Test
    public void saxUriIsEmptyForEveryOrdinaryElement() throws Exception {
        // characterization: HTML5 body synthesis inserts a BODY, so there are four start elements
        // (HTML, BODY, DIV, P); every one still reports an empty namespace uri
        final List<String> out = saxQNameUriLocal("<div><p>x</p></div>");
        assertEquals(4, out.size(), out.toString());
        for (final String entry : out) {
            assertTrue(entry.contains("|uri=|"), entry);
        }
    }

    @Test
    public void saxLocalNameEqualsQNameForEveryOrdinaryElement() throws Exception {
        final List<String> out = saxQNameUriLocal("<div><p>x</p></div>");
        assertTrue(out.contains("HTML|uri=|local=HTML"), out.toString());
        assertTrue(out.contains("DIV|uri=|local=DIV"), out.toString());
        assertTrue(out.contains("P|uri=|local=P"), out.toString());
    }
}
