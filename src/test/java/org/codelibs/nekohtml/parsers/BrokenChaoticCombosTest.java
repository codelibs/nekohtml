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
 * Category N: chaotic / degenerate combination characterization tests. These lock in
 * conservative "does not crash" and best-effort structural assertions for inputs that
 * combine several kinds of breakage (unclosed + misnested + stray end tags, extreme
 * sizes, control characters, etc.).
 */
public class BrokenChaoticCombosTest {

    @Test
    public void emptyInputProducesDocument() throws Exception {
        // characterization: empty input never crashes and always yields a non-null Document
        final Document doc = parse("");
        assertNotNull(doc);
    }

    @Test
    public void whitespaceOnlyInputDoesNotCrash() throws Exception {
        final Document doc = parse("   ");
        assertNotNull(doc);
    }

    @Test
    public void newlineOnlyInputDoesNotCrash() throws Exception {
        final Document doc = parse("\n\n");
        assertNotNull(doc);
    }

    @Test
    public void textOnlyInputProducesImplicitRoot() throws Exception {
        // characterization: the scanner appends a trailing newline which bootstraps the implicit HTML root
        final Document doc = parse("just text");
        assertNotNull(doc);
        assertEquals(1, count(doc, "//HTML"));
        assertTrue(firstText(doc, "//HTML").contains("just text"));
    }

    @Test
    public void unclosedMisnestedAndStrayEndTagCombo() throws Exception {
        // characterization: unclosed + misnested + stray-end-tag combination does not crash;
        // key elements survive conservatively
        final Document doc = parse("<div><b>x</p></i></div>");
        assertNotNull(doc);
        assertTrue(count(doc, "//DIV") >= 1);
        assertTrue(count(doc, "//B") >= 1);
    }

    @Test
    public void multipleCrossingTagsDoNotCrash() throws Exception {
        // characterization: heavily crossing/interleaved tags parse without crashing
        final Document doc = parse("<a><b><c></a></b></c>");
        assertNotNull(doc);
        assertTrue(count(doc, "//A") >= 1);
    }

    @Test
    public void deeplyNestedDivsDoNotCrash() throws Exception {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("<div>");
        }
        sb.append("deep");
        final Document doc = parse(sb.toString());
        assertNotNull(doc);
        // characterization: current implementation nests every <div> instead of flattening
        assertEquals(100, count(doc, "//DIV"));
    }

    @Test
    public void hugeAttributeValueDoesNotCrash() throws Exception {
        final StringBuilder sb = new StringBuilder("<div title=\"");
        for (int i = 0; i < 10000; i++) {
            sb.append('a');
        }
        sb.append("\">x</div>");
        final Document doc = parse(sb.toString());
        assertNotNull(doc);
        assertEquals(1, count(doc, "//DIV"));
        assertEquals(10000, first(doc, "//DIV").getAttribute("title").length());
    }

    @Test
    public void controlCharactersMixedWithTextDoNotCrash() throws Exception {
        // characterization: control characters (NUL, BEL) embedded in text do not crash the parser
        final Document doc = parse("<p>a" + "\u0000" + "b" + "\u0007" + "c</p>");
        assertNotNull(doc);
        assertEquals(1, count(doc, "//P"));
        assertTrue(firstText(doc, "//P").contains("a"));
        assertTrue(firstText(doc, "//P").contains("c"));
    }

    @Test
    public void legacyCenterAndFontComboDoesNotCrash() throws Exception {
        final Document doc = parse("<center><font size=3>x</font></center>");
        assertNotNull(doc);
        assertEquals(1, count(doc, "//CENTER"));
        assertEquals(1, count(doc, "//CENTER/FONT"));
        assertTrue(firstText(doc, "//FONT").contains("x"));
    }

    @Test
    public void emptyTagSoupWithOnlyEndTagsDoesNotCrash() throws Exception {
        // characterization: a document consisting solely of stray end tags does not crash
        final Document doc = parse("</div></span></p>");
        assertNotNull(doc);
        assertEquals(0, count(doc, "//DIV"));
        assertEquals(0, count(doc, "//SPAN"));
        assertEquals(0, count(doc, "//P"));
    }

    @Test
    public void manyConsecutiveSelfClosingVoidElementsDoNotCrash() throws Exception {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            sb.append("<br/>");
        }
        final Document doc = parse(sb.toString());
        assertNotNull(doc);
        assertEquals(500, count(doc, "//BR"));
    }

    @Test
    public void deeplyNestedMisnestedFormattingDoesNotCrash() throws Exception {
        // characterization: combining deep nesting with misnested formatting tags is survivable
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("<b><i>");
        }
        sb.append("x");
        final Document doc = parse(sb.toString());
        assertNotNull(doc);
        assertTrue(count(doc, "//B") >= 1);
        assertTrue(count(doc, "//I") >= 1);
    }

    @Test
    public void mixedBrokenAttributesAndTagsDoNotCrash() throws Exception {
        // characterization: combined attribute-syntax breakage and tag breakage does not crash
        final Document doc = parse("<a b=c d><div title=\"x>y\"><123>text");
        assertNotNull(doc);
        assertTrue(count(doc, "//A") >= 1);
    }

    @Test
    public void nulCharacterOnlyInputDoesNotCrash() throws Exception {
        final Document doc = parse("\u0000");
        assertNotNull(doc);
    }

    @Test
    public void combinedCommentAndUnclosedTagDoesNotCrash() throws Exception {
        // characterization: an unterminated comment followed by an unclosed tag still parses conservatively
        final Document doc = parse("<!-- unclosed<div><b>x");
        assertNotNull(doc);
        assertTrue(count(doc, "//DIV") >= 0);
    }

    @Test
    public void repeatedNestingOfTableInsideInlineDoesNotCrash() throws Exception {
        // characterization: chaotic mixing of table structure inside formatting elements survives
        final Document doc = parse("<b><table><tr><i><td>x</i></td></tr></table></b>");
        assertNotNull(doc);
        assertTrue(count(doc, "//TABLE") >= 1);
    }
}
