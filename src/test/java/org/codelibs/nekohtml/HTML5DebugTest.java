/*
 * Copyright 2025 CodeLibs, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codelibs.nekohtml;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.codelibs.nekohtml.filters.Writer;
import org.apache.xerces.xni.parser.XMLInputSource;

/**
 * Debug test to see HTML5 optional tag output.
 */
public class HTML5DebugTest {

    private static final String TEST_DATA_DIR = "src/test/resources/data/";

    @Test
    void debugHTML5OptionalTags() throws IOException {
        HTMLConfiguration config = new HTMLConfiguration();
        StringWriter writer = new StringWriter();
        Writer filter = new Writer(writer, "UTF-8");
        config.setDocumentHandler(filter);

        XMLInputSource source =
                new XMLInputSource(null, TEST_DATA_DIR + "test-html5-optional-tags.html", null, new FileReader(TEST_DATA_DIR
                        + "test-html5-optional-tags.html"), "UTF-8");
        config.parse(source);

        String result = writer.toString();
        System.out.println("=== HTML5 Optional Tags Debug Output ===");
        System.out.println(result);
        System.out.println("=== End Output ===");
    }

    @Test
    void debugSimpleHTML5() throws IOException {
        String html = "<!DOCTYPE html><p>First<p>Second";

        HTMLConfiguration config = new HTMLConfiguration();
        StringWriter writer = new StringWriter();
        Writer filter = new Writer(writer, "UTF-8");
        config.setDocumentHandler(filter);

        XMLInputSource source = new XMLInputSource(null, null, null, new java.io.StringReader(html), null);
        config.parse(source);

        String result = writer.toString();
        System.out.println("=== Simple HTML5 Debug Output ===");
        System.out.println(result);
        System.out.println("=== End Output ===");
    }
}
