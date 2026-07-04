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

import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Category J: raw text elements (script/style/textarea/title/xmp/plaintext) characterization tests.
 *
 * <p>
 * characterization: the current implementation has NO raw-text tokenizer mode at all. SimpleHTMLScanner
 * scans the whole document with the same generic start-tag/end-tag/text regex loop regardless of the
 * enclosing element, so content inside script/style/textarea/title/xmp/plaintext is entity-decoded
 * (non-standard for script/style/xmp) and any '&lt;letter' sequence inside it is parsed as a real tag
 * (non-standard for all of these elements, which are raw text or RCDATA in the HTML5 spec).
 * </p>
 */
public class BrokenRawTextElementsTest {

    // ------------------------------------------------------------------
    // <script>
    // ------------------------------------------------------------------

    @Test
    public void scriptWithoutAngleBracketsIsPreservedVerbatim() throws Exception {
        final Document doc = parse("<script>alert('x')</script>");
        assertEquals(1, count(doc, "//SCRIPT"));
        assertTrue(firstText(doc, "//SCRIPT").contains("alert('x')"));
    }

    @Test
    public void scriptEntitiesAreDecodedNonStandard() throws Exception {
        // characterization: current implementation decodes entities inside <script> (non-standard;
        // real HTML5 script content is raw text and entities are never decoded).
        final Document doc = parse("<script>a &amp; b</script>");
        assertEquals(1, count(doc, "//SCRIPT"));
        final String text = firstText(doc, "//SCRIPT");
        assertNotNull(text);
        assertTrue(text.contains("a & b"));
    }

    @Test
    public void unterminatedScriptIsClosedAtEof() throws Exception {
        final Document doc = parse("<script>foo");
        assertEquals(1, count(doc, "//SCRIPT"));
        assertTrue(firstText(doc, "//SCRIPT").contains("foo"));
    }

    @Test
    public void scriptWithTagLikeContentCreatesRealChildElement() throws Exception {
        // characterization (non-standard): since there is no raw-text mode, a '<div>' inside <script>
        // is parsed as an actual child DIV element rather than remaining literal script text.
        final Document doc = parse("<script>var x = '<div>not a tag</div>';</script>");
        assertEquals(1, count(doc, "//SCRIPT"));
        assertEquals(1, count(doc, "//SCRIPT/DIV"));
        assertTrue(firstText(doc, "//SCRIPT/DIV").contains("not a tag"));
        // the text before/after the bogus <div> remains as text of SCRIPT itself
        final String scriptText = firstText(doc, "//SCRIPT");
        assertTrue(scriptText.contains("var x = '"));
    }

    @Test
    public void scriptWithNonTagLikeLessThanDropsTheAngleBracket() throws Exception {
        // characterization: '<' not followed by a letter (here a digit) matches neither START_TAG nor
        // END_TAG, so the scanner falls into the "unknown tag, skip character" branch and silently
        // drops the lone '<'; the surrounding text merges without it.
        final Document doc = parse("<script>if (x<5) foo();</script>");
        assertEquals(1, count(doc, "//SCRIPT"));
        assertEquals(0, count(doc, "//SCRIPT/*"));
        final String text = firstText(doc, "//SCRIPT");
        assertTrue(text.contains("if (x5) foo();"));
    }

    @Test
    public void multipleScriptElementsEachKeepOwnContent() throws Exception {
        final Document doc = parse("<script>a</script><script>b</script>");
        assertEquals(2, count(doc, "//SCRIPT"));
    }

    // ------------------------------------------------------------------
    // <style>
    // ------------------------------------------------------------------

    @Test
    public void styleWithoutAngleBracketsIsPreservedVerbatim() throws Exception {
        final Document doc = parse("<style>.a{color:red}</style>");
        assertEquals(1, count(doc, "//STYLE"));
        assertTrue(firstText(doc, "//STYLE").contains(".a{color:red}"));
    }

    @Test
    public void styleEntitiesAreDecodedNonStandard() throws Exception {
        // characterization: same non-standard entity decoding applies to <style> content.
        final Document doc = parse("<style>a &amp; b</style>");
        assertEquals(1, count(doc, "//STYLE"));
        assertTrue(firstText(doc, "//STYLE").contains("a & b"));
    }

    @Test
    public void unterminatedStyleIsClosedAtEof() throws Exception {
        final Document doc = parse("<style>foo");
        assertEquals(1, count(doc, "//STYLE"));
        assertTrue(firstText(doc, "//STYLE").contains("foo"));
    }

    // ------------------------------------------------------------------
    // <textarea>
    // ------------------------------------------------------------------

    @Test
    public void textareaEntitiesAreDecoded() throws Exception {
        final Document doc = parse("<textarea>a &amp; b</textarea>");
        assertEquals(1, count(doc, "//TEXTAREA"));
        assertTrue(firstText(doc, "//TEXTAREA").contains("a & b"));
    }

    @Test
    public void textareaWithTagLikeContentCreatesRealChildElement() throws Exception {
        // characterization (non-standard): <p> inside <textarea> becomes a real child P element
        // instead of literal RCDATA text.
        final Document doc = parse("<textarea><p>This is not a paragraph</p></textarea>");
        assertEquals(1, count(doc, "//TEXTAREA"));
        assertEquals(1, count(doc, "//TEXTAREA/P"));
        assertTrue(firstText(doc, "//TEXTAREA/P").contains("This is not a paragraph"));
    }

    // ------------------------------------------------------------------
    // <title>
    // ------------------------------------------------------------------

    @Test
    public void titleEntitiesAreDecoded() throws Exception {
        final Document doc = parse("<title>a &amp; b</title>");
        assertEquals(1, count(doc, "//TITLE"));
        assertTrue(firstText(doc, "//TITLE").contains("a & b"));
    }

    @Test
    public void unterminatedTitleIsClosedAtEof() throws Exception {
        final Document doc = parse("<title>abc");
        assertEquals(1, count(doc, "//TITLE"));
        assertTrue(firstText(doc, "//TITLE").contains("abc"));
    }

    // ------------------------------------------------------------------
    // <xmp>
    // ------------------------------------------------------------------

    @Test
    public void xmpEntitiesAreDecodedNonStandard() throws Exception {
        // characterization: real <xmp> is raw text (no entity decoding); current implementation
        // decodes entities like everywhere else.
        final Document doc = parse("<xmp>a &amp; b</xmp>");
        assertEquals(1, count(doc, "//XMP"));
        assertTrue(firstText(doc, "//XMP").contains("a & b"));
    }

    @Test
    public void xmpWithTagLikeContentCreatesRealChildElement() throws Exception {
        // characterization (non-standard): <b> inside <xmp> becomes a real child B element instead of
        // literal text ("This is not bold" should never actually become bold per the HTML5 spec).
        final Document doc = parse("<xmp><b>This is not bold</b></xmp>");
        assertEquals(1, count(doc, "//XMP"));
        assertEquals(1, count(doc, "//XMP/B"));
        assertTrue(firstText(doc, "//XMP/B").contains("This is not bold"));
    }

    // ------------------------------------------------------------------
    // <plaintext>
    // ------------------------------------------------------------------

    @Test
    public void unterminatedPlaintextConsumesRestOfDocumentAsText() throws Exception {
        // characterization: <plaintext> has no explicit end tag in HTML5 (rest of document is raw
        // text); here it is just a normal element left open on the stack and closed at EOF like any
        // other unclosed element.
        final Document doc = parse("<plaintext>rest");
        assertEquals(1, count(doc, "//PLAINTEXT"));
        assertTrue(firstText(doc, "//PLAINTEXT").contains("rest"));
    }

    @Test
    public void plaintextWithTagLikeContentCreatesRealChildElement() throws Exception {
        // characterization (non-standard): a real <p> tag appearing after <plaintext> is parsed as an
        // actual child element rather than being treated as literal text (real plaintext turns
        // everything after it, including "<p>", into inert text for the rest of the document).
        final Document doc = parse("<plaintext>rest<p>x</p>");
        assertEquals(1, count(doc, "//PLAINTEXT"));
        assertEquals(1, count(doc, "//PLAINTEXT/P"));
        assertTrue(firstText(doc, "//PLAINTEXT/P").contains("x"));
    }

    // ------------------------------------------------------------------
    // SAX event-level checks
    // ------------------------------------------------------------------

    @Test
    public void saxEventsShowScriptEntityDecodedAsCharacters() throws Exception {
        final List<String> events = saxEvents("<script>a &amp; b</script>");
        assertTrue(events.contains("start:SCRIPT"));
        assertTrue(events.contains("end:SCRIPT"));
        assertTrue(events.stream().anyMatch(e -> e.startsWith("chars:") && e.contains("a & b")));
    }

    @Test
    public void saxEventsShowTagLikeScriptContentAsNestedElement() throws Exception {
        // characterization: the SAX stream for a bogus tag inside <script> shows a genuine
        // start/end pair for the nested element rather than a single chars event for the whole script.
        // Also locks in that an implicit HTML root wraps the fragment, auto-closed at EOF.
        final List<String> events = saxEvents("<script>x<div>y</div>z</script>");
        assertEquals("start:HTML", events.get(0));
        assertEquals(List.of("start:SCRIPT", "chars:x", "start:DIV", "chars:y", "end:DIV", "chars:z", "end:SCRIPT"), events.subList(1, 8));
        assertEquals("end:HTML", events.get(events.size() - 1));
    }

}
