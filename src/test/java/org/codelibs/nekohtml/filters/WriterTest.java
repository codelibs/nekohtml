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
package org.codelibs.nekohtml.filters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.codelibs.nekohtml.HTMLConfiguration;
import org.codelibs.nekohtml.filters.Writer;

/**
 * Unit tests for {@link Writer}.
 *
 * @author Marc Guillemot
 */
public class WriterTest {

    /**
     * Regression test for bug: writer changed attribute value causing NPE in 2nd writer.
     * http://sourceforge.net/support/tracker.php?aid=2815779
     */
    @Test
    public void testEmptyAttribute() throws Exception {

        final String content = "<html><head>" + "<meta name='COPYRIGHT' content='SOMEONE' />" + "</head><body></body></html>";
        final InputStream inputStream = new ByteArrayInputStream(content.getBytes());

        final XMLDocumentFilter[] filters =
                { new org.codelibs.nekohtml.filters.Writer(new ByteArrayOutputStream(), "UTF-8"),
                        new org.codelibs.nekohtml.filters.Writer(new ByteArrayOutputStream(), "UTF-8") };

        // create HTML parser
        final XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setProperty("http://cyberneko.org/html/properties/filters", filters);

        XMLInputSource source = new XMLInputSource(null, "currentUrl", null, inputStream, "UTF-8");

        parser.parse(source);
        inputStream.close();
    }
}
