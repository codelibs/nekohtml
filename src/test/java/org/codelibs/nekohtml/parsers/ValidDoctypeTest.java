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
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.firstText;
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.parse;
import static org.codelibs.nekohtml.parsers.ValidHtmlTestSupport.lexicalEvents;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Characterization tests for {@code DOCTYPE} handling on VALID/well-formed HTML, through both the
 * {@code DOMParser} path (DOCTYPE never becomes part of the DOM) and the plain {@code SAXParser} path
 * with a {@link org.xml.sax.ext.LexicalHandler} registered (DOCTYPE reaches the handler as a
 * startDTD/endDTD pair, but always with a hardcoded name and null ids).
 */
public class ValidDoctypeTest {

    @Test
    public void doctypeIsDroppedFromDomButRestOfDocumentParses() throws Exception {
        final Document doc = parse("<!DOCTYPE html><html><body><p>x</p></body></html>");
        // characterization: DOCTYPE never becomes a DOM DocumentType node
        assertNull(doc.getDoctype());
        assertEquals(1, count(doc, "//P"));
        assertEquals("x", firstText(doc, "//P"));
    }

    @Test
    public void doctypeDoesNotLeakAsTextIntoDom() throws Exception {
        final Document doc = parse("<!DOCTYPE html><html><body><p>x</p></body></html>");
        assertEquals(0, count(doc, "//text()[contains(.,'DOCTYPE')]"));
    }

    @Test
    public void saxEmitsStartDtdThenEndDtdForBasicDoctype() throws Exception {
        final List<String> events = lexicalEvents("<!DOCTYPE html><p>x</p>");
        // characterization: name is always the literal "html"; publicId/systemId are always null
        assertTrue(events.contains("startDTD:html|null|null"), events.toString());
        assertTrue(events.contains("endDTD"), events.toString());
        assertTrue(events.indexOf("startDTD:html|null|null") < events.indexOf("endDTD"), events.toString());
    }

    @Test
    public void legacyPublicDoctypeStillReportsNullPublicAndSystemIds() throws Exception {
        final List<String> events =
                lexicalEvents("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\"><p>x</p>");
        // characterization: publicId/systemId are never captured, even for a legacy doctype that has them
        assertTrue(events.contains("startDTD:html|null|null"), events.toString());
        assertTrue(events.contains("endDTD"), events.toString());
    }

    @Test
    public void lowercaseDoctypeKeywordEmitsSameStartDtd() throws Exception {
        final List<String> events = lexicalEvents("<!doctype html><p>x</p>");
        assertTrue(events.contains("startDTD:html|null|null"), events.toString());
        assertTrue(events.contains("endDTD"), events.toString());
    }

    @Test
    public void xhtmlDoctypeEmitsSameStartDtd() throws Exception {
        final List<String> events =
                lexicalEvents("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
                        + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\"><p>x</p>");
        assertTrue(events.contains("startDTD:html|null|null"), events.toString());
        assertTrue(events.contains("endDTD"), events.toString());
    }

    @Test
    public void doctypeNameIsAlwaysHardcodedHtmlRegardlessOfActualContent() throws Exception {
        // characterization: the DTD name reported to the lexical handler is a hardcoded literal
        // "html", not derived from the actual DOCTYPE content
        final List<String> events = lexicalEvents("<!DOCTYPE foo><p>x</p>");
        assertTrue(events.contains("startDTD:html|null|null"), events.toString());
    }

    @Test
    public void startDocumentPrecedesStartDtd() throws Exception {
        final List<String> events = lexicalEvents("<!DOCTYPE html><p>x</p>");
        assertTrue(events.indexOf("startDocument") < events.indexOf("startDTD:html|null|null"), events.toString());
    }

    @Test
    public void startDtdAndEndDtdPrecedeFirstStartElement() throws Exception {
        final List<String> events = lexicalEvents("<!DOCTYPE html><p>x</p>");
        final int startDtd = events.indexOf("startDTD:html|null|null");
        final int endDtd = events.indexOf("endDTD");
        final int startP = events.indexOf("start:P");
        assertTrue(startDtd < endDtd, events.toString());
        assertTrue(endDtd < startP, events.toString());
    }

    @Test
    public void endDtdImmediatelyFollowsStartDtdWithNoInterveningEvents() throws Exception {
        final List<String> events = lexicalEvents("<!DOCTYPE html><p>x</p>");
        final int startDtd = events.indexOf("startDTD:html|null|null");
        final int endDtd = events.indexOf("endDTD");
        assertEquals(startDtd + 1, endDtd, events.toString());
    }

    @Test
    public void noDoctypeMeansNoDtdEvents() throws Exception {
        final List<String> events = lexicalEvents("<p>x</p>");
        assertFalse(events.stream().anyMatch(e -> e.startsWith("startDTD")), events.toString());
        assertFalse(events.contains("endDTD"), events.toString());
    }

    @Test
    public void lowercaseDoctypeDroppedFromDomToo() throws Exception {
        final Document doc = parse("<!doctype html><p>x</p>");
        assertNull(doc.getDoctype());
        assertEquals(1, count(doc, "//P"));
    }

    @Test
    public void doctypeFollowedByBodylessContentOnlyAutoInsertsHtmlRoot() throws Exception {
        // characterization: HEAD/BODY are never auto-created, even after a DOCTYPE
        final Document doc = parse("<!DOCTYPE html><p>x</p>");
        assertEquals(1, count(doc, "//HTML"));
        assertEquals(0, count(doc, "//BODY"));
        assertEquals(1, count(doc, "//HTML/P"));
    }

    @Test
    public void doctypeWithExtraWhitespaceBeforeNameStillRecognized() throws Exception {
        final List<String> events = lexicalEvents("<!DOCTYPE    html><p>x</p>");
        assertTrue(events.contains("startDTD:html|null|null"), events.toString());
    }

    @Test
    public void multilineLegacyDoctypeStillRecognized() throws Exception {
        final List<String> events =
                lexicalEvents("<!DOCTYPE html PUBLIC\n\"-//W3C//DTD HTML 4.01//EN\"\n"
                        + "\"http://www.w3.org/TR/html4/strict.dtd\"><p>x</p>");
        assertTrue(events.contains("startDTD:html|null|null"), events.toString());
        assertTrue(events.contains("endDTD"), events.toString());
    }
}
