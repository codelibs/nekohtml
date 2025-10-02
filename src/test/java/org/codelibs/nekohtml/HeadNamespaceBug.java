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

import org.codelibs.xerces.parsers.AbstractSAXParser;
import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Unit test for <a href="http://sourceforge.net/support/tracker.php?aid=2799585">Bug 2799585</a>.
 * @author Charles Yates
 * @author Marc Guillemot
 *
 */
public class HeadNamespaceBug {

    /**
     * Ensure that the inserted head element has the right namespace
     */
    @Test
    public void testHeadNamespace() throws Exception {
        final int[] nbTags = { 0 };
        final ContentHandler handler = new DefaultHandler() {
            public void startElement(final String ns, final String name, final String qName, final Attributes atts) {
                System.out.println("HEAD Namespace: " + ns + ":" + name);
                assertEquals("http://www.w3.org/1999/xhtml:" + name, ns + ":" + name);
                ++nbTags[0];
            }
        };
        InputSource source = new InputSource();
        source.setByteStream(new ByteArrayInputStream("<html xmlns='http://www.w3.org/1999/xhtml'><body/></html>".getBytes()));
        HTMLConfiguration conf = new HTMLConfiguration();
        conf.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
        conf.setFeature("http://cyberneko.org/html/features/insert-namespaces", true);
        AbstractSAXParser parser = new AbstractSAXParser(conf) {
        };
        parser.setContentHandler(handler);
        parser.parse(source);

        // to be sure that test doesn't pass just because handler has never been called
        assertEquals(5, nbTags[0]);
    }
}
