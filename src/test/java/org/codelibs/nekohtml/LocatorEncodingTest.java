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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.xerces.impl.Version;
import org.codelibs.nekohtml.parsers.SAXParser;
import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Locator2;

/**
 * Regression test for <a href="http://sourceforge.net/tracker/?func=detail&atid=952178&aid=3381270&group_id=195122">Bug 3381270</a>.
 * @author Marc Guillemot
 * @version $Revision$
 */
public class LocatorEncodingTest {

    @Test
    public void test() throws SAXException, IOException {
        if (Version.getVersion().startsWith("Xerces-J 2.2") || Version.getVersion().startsWith("Xerces-J 2.3")) {
            return; // this test makes sense only for more recent Xerces versions
        }

        final String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<html></html>";
        ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        SAXParser parser = new SAXParser();

        final Locator[] locators = { null };

        final ContentHandler contentHandler = new ContentHandler() {
            public void startPrefixMapping(String prefix, String uri) throws SAXException {
            }

            public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            }

            public void startDocument() throws SAXException {
            }

            public void skippedEntity(String name) throws SAXException {
            }

            public void setDocumentLocator(Locator locator) {
                locators[0] = locator;
            }

            public void processingInstruction(String target, String data) throws SAXException {
            }

            public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            }

            public void endPrefixMapping(String prefix) throws SAXException {
            }

            public void endElement(String uri, String localName, String qName) throws SAXException {
            }

            public void endDocument() throws SAXException {
            }

            public void characters(char[] ch, int start, int length) throws SAXException {
            }
        };
        parser.setContentHandler(contentHandler);
        parser.parse(new InputSource(input));
        assertEquals("UTF8", ((Locator2) locators[0]).getEncoding());
    }
}
