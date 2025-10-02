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
package org.codelibs.nekohtml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codelibs.xerces.parsers.AbstractSAXParser;
import org.codelibs.xerces.xni.Augmentations;
import org.codelibs.xerces.xni.QName;
import org.codelibs.xerces.xni.XMLAttributes;
import org.codelibs.xerces.xni.XNIException;
import org.codelibs.xerces.xni.parser.XMLInputSource;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HTMLTagBalancingListener}.
 * @author Marc Guillemot
 * @version $Id$
 */
public class HTMLTagBalancingListenerTest {

    @Test
    public void testIgnoredTags() throws Exception {
        String string =
                "<html><head><title>foo</title></head>" + "<body>" + "<body onload='alert(123)'>" + "<div>" + "<form action='foo'>"
                        + "  <input name='text1'/>" + "</div>" + "</form>" + "</body></html>";

        final TestParser parser = new TestParser();
        final StringReader sr = new StringReader(string);
        final XMLInputSource in = new XMLInputSource(null, "foo", null, sr, null);

        parser.parse(in);

        final String[] expectedMessages =
                { "start HTML", "start HEAD", "start TITLE", "end TITLE", "end HEAD", "start BODY", "ignored start BODY", "start DIV",
                        "start FORM", "start INPUT", "end INPUT", "end FORM", "end DIV", "ignored end FORM", "end BODY", "end HTML" };

        assertEquals(Arrays.asList(expectedMessages).toString(), parser.messages.toString());
    }

    /**
     * HTMLTagBalancer field fSeenFramesetElement was not correctly reset as of 1.19.17  
     * @throws Exception
     */
    @Test
    public void testReuse() throws Exception {
        String string = "<head><title>title</title></head><body><div>hello</div></body>";

        final TestParser parser = new TestParser();
        final StringReader sr = new StringReader(string);
        final XMLInputSource in = new XMLInputSource(null, "foo", null, sr, null);

        parser.parse(in);

        final String[] expectedMessages =
                { "start HTML", "start HEAD", "start TITLE", "end TITLE", "end HEAD", "start BODY", "start DIV", "end DIV", "end BODY",
                        "end HTML" };

        assertEquals(Arrays.asList(expectedMessages).toString(), parser.messages.toString());

        parser.messages.clear();
        parser.parse(new XMLInputSource(null, "foo", null, new StringReader(string), null));
        assertEquals(Arrays.asList(expectedMessages).toString(), parser.messages.toString());
    }
}

class TestParser extends AbstractSAXParser implements HTMLTagBalancingListener {
    final List messages = new ArrayList();

    TestParser() throws Exception {
        super(new HTMLConfiguration());
        setFeature("http://cyberneko.org/html/features/balance-tags/ignore-outside-content", true);
    }

    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {

        messages.add("start " + element.rawname);
        super.startElement(element, attributes, augs);
    }

    public void ignoredEndElement(QName element, Augmentations augs) {
        messages.add("ignored end " + element.rawname);
    }

    public void ignoredStartElement(QName element, XMLAttributes attrs, Augmentations augs) {
        messages.add("ignored start " + element.rawname);
    }

    public void endElement(QName element, Augmentations augs) throws XNIException {
        messages.add("end " + element.rawname);
        super.endElement(element, augs);
    }
}