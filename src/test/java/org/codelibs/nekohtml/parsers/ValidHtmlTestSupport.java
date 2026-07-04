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

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Shared helper for VALID / happy-path HTML characterization tests.
 *
 * <p>
 * Adds lexical-event capture, byte-stream parsing, feature toggling and DOM
 * child-signature helpers on top of {@link BrokenHtmlTestSupport}. Uses only
 * the public {@code DOMParser}/{@code SAXParser} APIs.
 * </p>
 */
public final class ValidHtmlTestSupport {

    private ValidHtmlTestSupport() {
    }

    /** Parses HTML bytes with the given encoding label on the InputSource ({@code null} = default). */
    public static Document parseBytes(final byte[] bytes, final String encoding) throws Exception {
        final DOMParser parser = new DOMParser();
        final InputSource source = new InputSource(new ByteArrayInputStream(bytes));
        if (encoding != null) {
            source.setEncoding(encoding);
        }
        parser.parse(source);
        return parser.getDocument();
    }

    /** Captures the combined content + lexical event stream as strings. */
    public static List<String> lexicalEvents(final String html) throws Exception {
        final List<String> events = new ArrayList<>();
        final SAXParser parser = new SAXParser();
        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startDocument() {
                events.add("startDocument");
            }

            @Override
            public void endDocument() {
                events.add("endDocument");
            }

            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes atts) {
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

            @Override
            public void processingInstruction(final String target, final String data) {
                events.add("pi:" + target + "|" + data);
            }
        });
        parser.setLexicalHandler(new LexicalHandler() {
            @Override
            public void comment(final char[] ch, final int start, final int length) {
                events.add("comment:" + new String(ch, start, length));
            }

            @Override
            public void startDTD(final String name, final String publicId, final String systemId) {
                events.add("startDTD:" + name + "|" + publicId + "|" + systemId);
            }

            @Override
            public void endDTD() {
                events.add("endDTD");
            }

            @Override
            public void startCDATA() {
                events.add("startCDATA");
            }

            @Override
            public void endCDATA() {
                events.add("endCDATA");
            }

            @Override
            public void startEntity(final String name) {
            }

            @Override
            public void endEntity(final String name) {
            }
        });
        parser.parse(new InputSource(new StringReader(html)));
        return events;
    }

    /** Collects one {@code "QN|uri=U|local=L"} descriptor per startElement. */
    public static List<String> saxQNameUriLocal(final String html) throws Exception {
        final List<String> out = new ArrayList<>();
        final SAXParser parser = new SAXParser();
        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes atts) {
                out.add(qName + "|uri=" + uri + "|local=" + localName);
            }
        });
        parser.parse(new InputSource(new StringReader(html)));
        return out;
    }

    /** Collects start/end/chars events after setting one feature to a value. */
    public static List<String> saxEventsWithFeature(final String html, final String feature, final boolean value) throws Exception {
        final List<String> events = new ArrayList<>();
        final SAXParser parser = new SAXParser();
        parser.setFeature(feature, value);
        parser.setContentHandler(new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes atts) {
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

    /** Returns an ordered signature of a node's children. */
    public static List<String> childSignature(final Node parent) {
        final List<String> sig = new ArrayList<>();
        final NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node n = children.item(i);
            switch (n.getNodeType()) {
            case Node.ELEMENT_NODE -> sig.add("elem:" + n.getNodeName());
            case Node.TEXT_NODE -> sig.add("text:" + n.getNodeValue());
            case Node.COMMENT_NODE -> sig.add("comment:" + n.getNodeValue());
            case Node.CDATA_SECTION_NODE -> sig.add("cdata:" + n.getNodeValue());
            case Node.PROCESSING_INSTRUCTION_NODE -> sig.add("pi:" + n.getNodeName() + "|" + n.getNodeValue());
            default -> sig.add("other:" + n.getNodeType());
            }
        }
        return sig;
    }
}
