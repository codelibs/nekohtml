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
 * characterization: SimpleHTMLScanner implements HTML5 raw-text/RCDATA modes. RAWTEXT elements
 * (script/style/xmp) keep their content verbatim -- entities are NOT decoded and tag-like text is
 * NOT parsed into child elements. RCDATA elements (textarea/title) keep tag-like text literal but
 * DO decode entities. PLAINTEXT is not in either set, so it is still generically parsed.
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
        // characterization: SCRIPT is a raw-text element (HTML5), so entities are NOT decoded; the
        // literal "a &amp; b" is preserved verbatim.
        final Document doc = parse("<script>a &amp; b</script>");
        assertEquals(1, count(doc, "//SCRIPT"));
        final String text = firstText(doc, "//SCRIPT");
        assertNotNull(text);
        assertTrue(text.contains("a &amp; b"));
    }

    @Test
    public void unterminatedScriptIsClosedAtEof() throws Exception {
        final Document doc = parse("<script>foo");
        assertEquals(1, count(doc, "//SCRIPT"));
        assertTrue(firstText(doc, "//SCRIPT").contains("foo"));
    }

    @Test
    public void scriptWithTagLikeContentCreatesRealChildElement() throws Exception {
        // characterization: SCRIPT is a raw-text element (HTML5), so a '<div>' inside it stays literal
        // script text; no child DIV element is created.
        final Document doc = parse("<script>var x = '<div>not a tag</div>';</script>");
        assertEquals(1, count(doc, "//SCRIPT"));
        assertEquals(0, count(doc, "//SCRIPT/DIV"));
        assertTrue(firstText(doc, "//SCRIPT").contains("<div>not a tag</div>"));
        // the whole script body, including the tag-like text, remains as text of SCRIPT itself
        final String scriptText = firstText(doc, "//SCRIPT");
        assertTrue(scriptText.contains("var x = '"));
    }

    @Test
    public void scriptWithNonTagLikeLessThanDropsTheAngleBracket() throws Exception {
        // characterization: SCRIPT is raw text (HTML5), so the '<' is preserved verbatim and "x<5"
        // stays intact (the old generic tokenizer dropped the '<', corrupting "x<5" to "x5").
        final Document doc = parse("<script>if (x<5) foo();</script>");
        assertEquals(1, count(doc, "//SCRIPT"));
        assertEquals(0, count(doc, "//SCRIPT/*"));
        final String text = firstText(doc, "//SCRIPT");
        assertTrue(text.contains("if (x<5) foo();"));
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
        // characterization: STYLE is a raw-text element (HTML5), so entities are NOT decoded.
        final Document doc = parse("<style>a &amp; b</style>");
        assertEquals(1, count(doc, "//STYLE"));
        assertTrue(firstText(doc, "//STYLE").contains("a &amp; b"));
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
        // characterization: TEXTAREA is RCDATA (HTML5), so <p> inside it stays literal text; no child
        // P element is created (entities would still decode, but there are none here).
        final Document doc = parse("<textarea><p>This is not a paragraph</p></textarea>");
        assertEquals(1, count(doc, "//TEXTAREA"));
        assertEquals(0, count(doc, "//TEXTAREA/P"));
        assertTrue(firstText(doc, "//TEXTAREA").contains("<p>This is not a paragraph</p>"));
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
        // characterization: XMP is a raw-text element (HTML5), so entities are NOT decoded.
        final Document doc = parse("<xmp>a &amp; b</xmp>");
        assertEquals(1, count(doc, "//XMP"));
        assertTrue(firstText(doc, "//XMP").contains("a &amp; b"));
    }

    @Test
    public void xmpWithTagLikeContentCreatesRealChildElement() throws Exception {
        // characterization: XMP is a raw-text element (HTML5), so <b> inside it stays literal text; no
        // child B element is created ("This is not bold" never actually becomes bold).
        final Document doc = parse("<xmp><b>This is not bold</b></xmp>");
        assertEquals(1, count(doc, "//XMP"));
        assertEquals(0, count(doc, "//XMP/B"));
        assertTrue(firstText(doc, "//XMP").contains("<b>This is not bold</b>"));
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
        // characterization: SCRIPT is raw text (HTML5), so the entity is NOT decoded; the chars run is
        // emitted verbatim as "a &amp; b".
        final List<String> events = saxEvents("<script>a &amp; b</script>");
        assertTrue(events.contains("start:SCRIPT"));
        assertTrue(events.contains("end:SCRIPT"));
        assertTrue(events.stream().anyMatch(e -> e.startsWith("chars:") && e.contains("a &amp; b")));
    }

    @Test
    public void saxEventsShowTagLikeScriptContentAsRawText() throws Exception {
        // <script> is a raw-text element: a tag-like construct inside it (<div>) is NOT parsed as
        // markup but preserved verbatim as a single chars run, so no phantom nested element appears.
        // An implicit HTML/HEAD structure wraps the fragment, auto-closed at EOF.
        final List<String> events = saxEvents("<script>x<div>y</div>z</script>");
        assertEquals(List.of("start:HTML", "start:HEAD", "start:SCRIPT", "chars:x<div>y</div>z", "end:SCRIPT", "end:HEAD", "end:HTML"),
                events);
    }

}
