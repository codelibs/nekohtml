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

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Category M: implicit document structure characterization tests. The balancer synthesizes
 * HTML5-style structure: a HEAD wrapping head metadata elements and a BODY wrapping body content.
 */
public class BrokenImplicitStructureTest {

    @Test
    public void onlyHtmlRootIsImplicitlyCreated() throws Exception {
        // characterization: leading text is body content, wrapped in a synthesized BODY (HTML5);
        // no HEAD is created for pure body content
        final Document doc = parse("Hello<b>x</b>");
        assertEquals(1, count(doc, "//HTML"));
        assertEquals(0, count(doc, "//HEAD"));
        assertEquals(1, count(doc, "//BODY"));
        assertEquals(1, count(doc, "//B"));
    }

    @Test
    public void leadingCommentCreatesHtmlRoot() throws Exception {
        // characterization: a leading comment also bootstraps the implicit HTML root
        final Document doc = parse("<!-- c --><p>x");
        assertEquals(1, count(doc, "//HTML"));
        assertEquals(1, count(doc, "//P"));
        assertNotNull(firstText(doc, "//P"));
        assertTrue(firstText(doc, "//P").contains("x"));
    }

    @Test
    public void simpleBodyOnlyContentUnderHtmlRoot() throws Exception {
        // characterization: bare body content is wrapped in a synthesized BODY under the implicit
        // HTML root (HTML5); no HEAD is created
        final Document doc = parse("<p>x");
        assertEquals(1, count(doc, "//HTML"));
        assertEquals(0, count(doc, "//HEAD"));
        assertEquals(1, count(doc, "//BODY"));
        assertEquals(1, count(doc, "//P"));
        assertTrue(firstText(doc, "//P").contains("x"));
    }

    @Test
    public void bodyClosesOpenHeadAndTitle() throws Exception {
        // characterization: an explicit BODY tag force-closes any still-open HEAD/TITLE
        final Document doc = parse("<head><title>t</title><body>x");
        assertEquals(1, count(doc, "//HEAD"));
        assertEquals(1, count(doc, "//BODY"));
        assertEquals(1, count(doc, "//HEAD/TITLE"));
        assertTrue(firstText(doc, "//TITLE").contains("t"));
        assertTrue(firstText(doc, "//BODY").contains("x"));
        // HEAD must not be an ancestor of BODY (it was closed before BODY opened)
        assertEquals(0, count(doc, "//HEAD//BODY"));
    }

    @Test
    public void framesetAlsoClosesOpenHeadAndTitle() throws Exception {
        // characterization: FRAMESET behaves like BODY and force-closes HEAD/TITLE (BODY_ELEMENTS set)
        final Document doc = parse("<head><title>t</title><frameset>x");
        assertEquals(1, count(doc, "//HEAD"));
        assertEquals(1, count(doc, "//FRAMESET"));
        assertEquals(1, count(doc, "//HEAD/TITLE"));
        assertEquals(0, count(doc, "//HEAD//FRAMESET"));
    }

    @Test
    public void titleThenTrailingTextGoesToHtmlDirectly() throws Exception {
        // characterization: TITLE is head metadata wrapped in a synthesized HEAD, and the trailing
        // text following </title> is body content wrapped in a synthesized BODY (HTML5)
        final Document doc = parse("<title>t</title>x");
        assertEquals(1, count(doc, "//HEAD"));
        assertEquals(1, count(doc, "//BODY"));
        assertEquals(1, count(doc, "//TITLE"));
        assertTrue(firstText(doc, "//TITLE").contains("t"));
        assertTrue(firstText(doc, "//HTML").contains("x"));
    }

    @Test
    public void headElementsNotRelocatedIntoBody() throws Exception {
        // characterization: META is head metadata wrapped in a synthesized HEAD (HTML5) and kept OUT
        // of BODY (not relocated into body); only the following P goes into the synthesized BODY
        final Document doc = parse("<meta charset=utf-8><p>x");
        assertEquals(1, count(doc, "//HTML"));
        assertEquals(1, count(doc, "//HEAD"));
        assertEquals(1, count(doc, "//META"));
        assertEquals(1, count(doc, "//P"));
        assertTrue(firstText(doc, "//P").contains("x"));
    }

    @Test
    public void multipleVoidHeadElementsStayUnwrapped() throws Exception {
        // characterization: several void head-ish elements in a row are wrapped in a single synthesized HEAD (HTML5)
        final Document doc = parse("<meta a=1><link rel=x><p>y");
        assertEquals(1, count(doc, "//HEAD"));
        assertEquals(1, count(doc, "//META"));
        assertEquals(1, count(doc, "//LINK"));
        assertEquals(1, count(doc, "//P"));
    }

    @Test
    public void leadingTextThenHeadElementStillNoHeadWrapper() throws Exception {
        // characterization: a head-ish element appearing after leading body text is not wrapped in HEAD either
        final Document doc = parse("x<title>t</title>");
        assertEquals(0, count(doc, "//HEAD"));
        assertEquals(1, count(doc, "//TITLE"));
        assertTrue(firstText(doc, "//TITLE").contains("t"));
    }

    @Test
    public void explicitHtmlDoesNotCreateSecondDocumentRoot() throws Exception {
        // characterization: the DOM only ever has one document element even with a duplicated <html> tag
        final Document doc = parse("<html><html>");
        assertNotNull(doc.getDocumentElement());
        assertEquals("HTML", doc.getDocumentElement().getNodeName());
        assertEquals(1, count(doc, "/*"));
    }

    @Test
    public void htmlRootAttributesArePreserved() throws Exception {
        // characterization: attributes on an explicit <html> are preserved; the P content is wrapped
        // in a synthesized BODY (HTML5), and no HEAD is created for pure body content
        final Document doc = parse("<html lang=en><p>x</p></html>");
        assertEquals(1, count(doc, "//HTML"));
        assertEquals(0, count(doc, "//HEAD"));
        assertEquals(1, count(doc, "//BODY"));
        assertEquals("en", first(doc, "//HTML").getAttribute("lang"));
    }

    @Test
    public void headWithoutBodyClosedAtEof() throws Exception {
        // characterization: an unclosed HEAD/TITLE with no BODY at all is still closed (LIFO) at end-of-document
        final Document doc = parse("<head><title>t</title>");
        assertEquals(1, count(doc, "//HEAD"));
        assertEquals(1, count(doc, "//HEAD/TITLE"));
        assertEquals(0, count(doc, "//BODY"));
    }

    @Test
    public void commentInsideExplicitHtmlDoesNotDisruptStructure() throws Exception {
        // characterization: a comment between HTML and BODY does not create a second root nor implicit HEAD
        final Document doc = parse("<html><!-- c --><body>x</body></html>");
        assertEquals(1, count(doc, "//HTML"));
        assertEquals(0, count(doc, "//HEAD"));
        assertEquals(1, count(doc, "//BODY"));
        assertTrue(firstText(doc, "//BODY").contains("x"));
    }

    @Test
    public void bodyAfterExplicitCloseIsCharacterized() throws Exception {
        // characterization: content appearing after </html> is recorded as-is (whatever the current parser does)
        final Document doc = parse("<body>x</body><html>y</html>");
        assertNotNull(doc);
        assertEquals(1, count(doc, "//BODY"));
        assertTrue(count(doc, "//HTML") >= 1);
    }
}
