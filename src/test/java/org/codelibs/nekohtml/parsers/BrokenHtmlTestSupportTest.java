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
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.parse;
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.saxEvents;
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.saxStartElements;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Sanity tests for {@link BrokenHtmlTestSupport}.
 */
public class BrokenHtmlTestSupportTest {

    @Test
    public void parseReturnsDocument() throws Exception {
        final Document doc = parse("<html><body><p>Hello</p></body></html>");
        assertNotNull(doc);
        assertEquals(1, count(doc, "//P"));
        assertEquals("Hello", firstText(doc, "//P"));
        assertNotNull(first(doc, "//P"));
        assertNull(first(doc, "//NOSUCH"));
        assertEquals(0, count(doc, "//NOSUCH"));
    }

    @Test
    public void saxStartElementsCollectsNames() throws Exception {
        assertEquals(List.of("HTML", "BODY", "P"), saxStartElements("<html><body><p>x</p></body></html>"));
    }

    @Test
    public void saxEventsIncludesStartEndChars() throws Exception {
        final List<String> ev = saxEvents("<p>x</p>");
        assertTrue(ev.contains("start:P"));
        assertTrue(ev.contains("end:P"));
        assertTrue(ev.stream().anyMatch(e -> e.startsWith("chars:")));
    }
}
