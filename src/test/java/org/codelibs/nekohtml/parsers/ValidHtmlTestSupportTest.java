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
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.firstText;
import static org.codelibs.nekohtml.parsers.ValidHtmlTestSupport.childSignature;
import static org.codelibs.nekohtml.parsers.ValidHtmlTestSupport.lexicalEvents;
import static org.codelibs.nekohtml.parsers.ValidHtmlTestSupport.parseBytes;
import static org.codelibs.nekohtml.parsers.ValidHtmlTestSupport.saxQNameUriLocal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Sanity tests for {@link ValidHtmlTestSupport}.
 */
public class ValidHtmlTestSupportTest {

    @Test
    public void lexicalEventsCapturesContentStream() throws Exception {
        final List<String> ev = lexicalEvents("<p>x</p>");
        assertTrue(ev.contains("startDocument"), ev.toString());
        assertTrue(ev.contains("start:P"), ev.toString());
        assertTrue(ev.contains("chars:x"), ev.toString());
        assertTrue(ev.contains("end:P"), ev.toString());
        assertTrue(ev.contains("endDocument"), ev.toString());
    }

    @Test
    public void parseBytesDecodesUtf8() throws Exception {
        final Document doc = parseBytes("<p>x</p>".getBytes(StandardCharsets.UTF_8), "UTF-8");
        assertNotNull(doc);
        assertEquals(1, count(doc, "//P"));
        assertEquals("x", firstText(doc, "//P"));
    }

    @Test
    public void saxQNameUriLocalReportsUri() throws Exception {
        final List<String> out = saxQNameUriLocal("<p>x</p>");
        assertTrue(out.stream().anyMatch(s -> s.startsWith("P|uri=")), out.toString());
    }

    @Test
    public void childSignatureListsChildrenInOrder() throws Exception {
        final Document doc = parseBytes("<div>a<b>c</b></div>".getBytes(StandardCharsets.UTF_8), "UTF-8");
        final List<String> sig = childSignature(first(doc, "//DIV"));
        assertTrue(sig.contains("text:a"), sig.toString());
        assertTrue(sig.contains("elem:B"), sig.toString());
    }
}
