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
import org.w3c.dom.Element;

/**
 * Characterization tests for broken/malformed list structures (category G).
 * These tests lock in the current parsing behavior; some of it is
 * intentionally non-standard compared to a real HTML5 parser.
 */
public class BrokenListStructureTest {

    @Test
    public void textBeforeLiStaysUnderUl() throws Exception {
        // characterization: text preceding <li> inside <ul> stays as a UL child (not dropped)
        final Document doc = parse("<ul>text<li>a</li></ul>");
        assertEquals(1, count(doc, "//UL"));
        assertEquals(1, count(doc, "//UL/LI"));
        assertTrue(firstText(doc, "//UL").contains("text"));
        assertEquals("a", firstText(doc, "//LI"));
    }

    @Test
    public void nestedListsKeepBothULs() throws Exception {
        // characterization: <ul><ul>... does not merge/complete li; both ULs are preserved
        final Document doc = parse("<ul><ul><li>Item</li></ul></ul>");
        assertEquals(2, count(doc, "//UL"));
        assertEquals(1, count(doc, "//LI"));
        assertEquals(1, count(doc, "//UL/UL"));
        assertEquals("Item", firstText(doc, "//LI"));
    }

    @Test
    public void properlyClosedListItemsAreSiblings() throws Exception {
        // control/contrast case: with explicit </li>, items are true siblings under UL
        final Document doc = parse("<ul><li>a</li><li>b</li></ul>");
        assertEquals(1, count(doc, "//UL"));
        assertEquals(2, count(doc, "//UL/LI"));
        assertEquals(0, count(doc, "//LI/LI"));
    }

    @Test
    public void unclosedListItemsNestInsteadOfSibling() throws Exception {
        // characterization: without </li>, the second <li> nests inside the first (no sibling auto-close)
        final Document doc = parse("<ul><li>a<li>b</ul>");
        assertEquals(2, count(doc, "//LI"));
        assertEquals(1, count(doc, "//UL/LI"));
        assertEquals(1, count(doc, "//LI/LI"));
    }

    @Test
    public void threeConsecutiveUnclosedLiFormChain() throws Exception {
        // characterization: three consecutive unclosed <li> form a nesting chain, not three siblings
        final Document doc = parse("<ul><li>a<li>b<li>c</ul>");
        assertEquals(3, count(doc, "//LI"));
        assertEquals(1, count(doc, "//UL/LI"));
        assertEquals(2, count(doc, "//LI/LI"));
    }

    @Test
    public void olWithSingleUnclosedLi() throws Exception {
        // characterization: </ol> does not close the still-open <li> (mismatched end tag is ignored);
        // the tree built so far (OL > LI) is kept regardless
        final Document doc = parse("<ol><li>1</ol>");
        assertEquals(1, count(doc, "//OL"));
        assertEquals(1, count(doc, "//OL/LI"));
        assertEquals("1", firstText(doc, "//LI"));
    }

    @Test
    public void dlDtDdNestWithoutClosingTags() throws Exception {
        // characterization: <dd> is not auto-completed as a sibling of the open <dt>; it nests inside it
        final Document doc = parse("<dl><dt>t<dd>d</dl>");
        assertEquals(1, count(doc, "//DL"));
        assertEquals(1, count(doc, "//DL/DT"));
        assertEquals(1, count(doc, "//DT/DD"));
        assertTrue(firstText(doc, "//DT").contains("d"));
    }

    @Test
    public void properlyClosedDlHasSiblingDtDd() throws Exception {
        // control/contrast case: explicit closing tags produce true DT/DD siblings under DL
        final Document doc = parse("<dl><dt>t1</dt><dd>d1</dd><dt>t2</dt><dd>d2</dd></dl>");
        assertEquals(1, count(doc, "//DL"));
        assertEquals(2, count(doc, "//DL/DT"));
        assertEquals(2, count(doc, "//DL/DD"));
        assertEquals(0, count(doc, "//DT/DD"));
    }

    @Test
    public void orphanLiOutsideList() throws Exception {
        // characterization: <li> with no <ul>/<ol> ancestor is still created (only HTML root is implicit)
        final Document doc = parse("<li>orphan</li>");
        assertEquals(1, count(doc, "//LI"));
        assertEquals("orphan", firstText(doc, "//LI"));
        assertEquals(0, count(doc, "//UL"));
        assertEquals(0, count(doc, "//BODY"));
    }

    @Test
    public void emptyUlHasNoChildren() throws Exception {
        final Document doc = parse("<ul></ul>");
        assertEquals(1, count(doc, "//UL"));
        assertEquals(0, count(doc, "//UL/*"));
    }

    @Test
    public void properlyNestedSubListInsideLi() throws Exception {
        // control/contrast case: a properly nested sub-list (valid HTML) nests as expected
        final Document doc = parse("<ul><li>a<ul><li>b</li></ul></li></ul>");
        assertEquals(2, count(doc, "//UL"));
        assertEquals(2, count(doc, "//LI"));
        assertEquals(1, count(doc, "//UL/LI/UL/LI"));
    }

    @Test
    public void olClosedProperlyThreeSiblingLis() throws Exception {
        final Document doc = parse("<ol><li>1</li><li>2</li><li>3</li></ol>");
        assertEquals(1, count(doc, "//OL"));
        assertEquals(3, count(doc, "//OL/LI"));
        assertEquals(0, count(doc, "//LI/LI"));
    }

    @Test
    public void ulWithOnlyTextNoLi() throws Exception {
        final Document doc = parse("<ul>just text</ul>");
        assertEquals(1, count(doc, "//UL"));
        assertEquals(0, count(doc, "//LI"));
        assertTrue(firstText(doc, "//UL").contains("just text"));
    }

    @Test
    public void nestedUlThreeLevelsDeep() throws Exception {
        // characterization: repeated <ul> nesting (no li in between) stacks up three levels
        final Document doc = parse("<ul><ul><ul><li>x</li></ul></ul></ul>");
        assertEquals(3, count(doc, "//UL"));
        assertEquals(1, count(doc, "//UL/UL/UL/LI"));
        final Element outer = first(doc, "//UL");
        assertNotNull(outer);
    }
}
