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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Shared helper for broken-HTML characterization tests.
 *
 * <p>
 * This helper depends only on the public {@code DOMParser}/{@code SAXParser}
 * APIs and never touches the internal implementation. It exists purely to
 * remove the boilerplate of building an {@code XPath} for every assertion.
 * </p>
 */
public final class BrokenHtmlTestSupport {

    private BrokenHtmlTestSupport() {
    }

    /** Parses the given HTML string with {@link DOMParser} and returns the Document. */
    public static Document parse(final String html) throws Exception {
        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new StringReader(html)));
        return parser.getDocument();
    }

    /** Evaluates the XPath expression against the document and returns the node set. */
    public static NodeList nodes(final Document doc, final String xpath) throws Exception {
        final XPath xp = XPathFactory.newInstance().newXPath();
        return (NodeList) xp.evaluate(xpath, doc, XPathConstants.NODESET);
    }

    /** Returns the number of nodes matching the XPath expression. */
    public static int count(final Document doc, final String xpath) throws Exception {
        return nodes(doc, xpath).getLength();
    }

    /** Returns the first element matching the XPath expression, or {@code null} if none. */
    public static Element first(final Document doc, final String xpath) throws Exception {
        final NodeList nl = nodes(doc, xpath);
        return nl.getLength() == 0 ? null : (Element) nl.item(0);
    }

    /** Returns the text content of the first node matching the XPath, or {@code null} if none. */
    public static String firstText(final Document doc, final String xpath) throws Exception {
        final NodeList nl = nodes(doc, xpath);
        return nl.getLength() == 0 ? null : nl.item(0).getTextContent();
    }

    /** Returns the SAX {@code startElement} qNames in document order. */
    public static List<String> saxStartElements(final String html) throws Exception {
        final List<String> names = new ArrayList<>();
        final SAXParser parser = new SAXParser();
        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
                names.add(qName);
            }
        });
        parser.parse(new InputSource(new StringReader(html)));
        return names;
    }

    /**
     * Returns the SAX event stream as strings: {@code "start:X"}, {@code "end:X"} and
     * {@code "chars:..."} in document order.
     */
    public static List<String> saxEvents(final String html) throws Exception {
        final List<String> events = new ArrayList<>();
        final SAXParser parser = new SAXParser();
        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
                events.add("start:" + qName);
            }

            @Override
            public void endElement(final String uri, final String localName, final String qName) {
                events.add("end:" + qName);
            }

            @Override
            public void characters(final char[] ch, final int start, final int length) {
                events.add("chars:" + new String(ch, start, length));
            }
        });
        parser.parse(new InputSource(new StringReader(html)));
        return events;
    }
}
