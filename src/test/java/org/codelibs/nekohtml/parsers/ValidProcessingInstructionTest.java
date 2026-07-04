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
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.parse;
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.saxEvents;
import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.saxStartElements;
import static org.codelibs.nekohtml.parsers.ValidHtmlTestSupport.childSignature;
import static org.codelibs.nekohtml.parsers.ValidHtmlTestSupport.lexicalEvents;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Characterization tests for processing instructions and the XML declaration on VALID/well-formed
 * HTML. The current {@code SimpleHTMLScanner} treats {@code <?...?>} constructs as HTML5 "bogus
 * comments": nothing ever calls {@code ContentHandler.processingInstruction}; instead the payload is
 * reported via {@code LexicalHandler.comment} (a DOM Comment node whose content runs from the
 * {@code '?'} up to but excluding the closing {@code '>'}), so it no longer leaks into surrounding
 * text.
 */
public class ValidProcessingInstructionTest {

    @Test
    public void xmlDeclarationEmitsNoPiEvent() throws Exception {
        final List<String> events = lexicalEvents("<?xml version=\"1.0\" encoding=\"UTF-8\"?><p>x</p>");
        assertTrue(events.stream().noneMatch(e -> e.startsWith("pi:")), events.toString());
    }

    @Test
    public void xmlDeclarationLeaksAsCharactersContainingDeclarationText() throws Exception {
        // characterization: the XML declaration becomes a bogus comment (HTML5), reported via the
        // lexical handler, not leaked as characters
        final List<String> events = lexicalEvents("<?xml version=\"1.0\" encoding=\"UTF-8\"?><p>x</p>");
        assertTrue(events.stream().anyMatch(e -> e.startsWith("comment:") && e.contains("xml version")), events.toString());
    }

    @Test
    public void xmlDeclarationLeakedTextDropsOnlyLeadingAngleBracket() throws Exception {
        // characterization: the construct becomes a bogus comment whose content runs from the '?' up
        // to (but excluding) the closing '>', so it is reported as "comment:?xml ... ?"
        final List<String> events = lexicalEvents("<?xml version=\"1.0\" encoding=\"UTF-8\"?><p>x</p>");
        assertTrue(events.contains("comment:?xml version=\"1.0\" encoding=\"UTF-8\"?"), events.toString());
    }

    @Test
    public void xmlDeclarationStillAllowsFollowingElementToParse() throws Exception {
        final List<String> events = lexicalEvents("<?xml version=\"1.0\" encoding=\"UTF-8\"?><p>x</p>");
        assertTrue(events.contains("start:P"), events.toString());
        assertTrue(events.contains("chars:x"), events.toString());
        assertTrue(events.contains("end:P"), events.toString());
    }

    @Test
    public void domXmlDeclarationDoesNotPreventPElementParsing() throws Exception {
        final Document doc = parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?><p>x</p>");
        assertEquals(1, count(doc, "//P"));
    }

    @Test
    public void domXmlDeclarationLeaksAsStrayTextNodeAtRoot() throws Exception {
        // characterization: the XML declaration becomes a document-level Comment node (before the
        // first element), so the HTML root's only child is the synthesized BODY -- no stray text node
        final Document doc = parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?><p>x</p>");
        final List<String> sig = childSignature(doc.getDocumentElement());
        assertEquals(List.of("elem:BODY"), sig);
    }

    @Test
    public void phpProcessingInstructionEmitsNoPiEvent() throws Exception {
        final List<String> events = lexicalEvents("<?php echo 1; ?><p>x</p>");
        assertTrue(events.stream().noneMatch(e -> e.startsWith("pi:")), events.toString());
    }

    @Test
    public void phpProcessingInstructionLeaksAsCharactersText() throws Exception {
        // characterization: the PHP construct becomes a bogus comment (HTML5), reported via the
        // lexical handler; the following <p> still parses normally
        final List<String> events = lexicalEvents("<?php echo 1; ?><p>x</p>");
        assertTrue(events.contains("comment:?php echo 1; ?"), events.toString());
        assertTrue(events.contains("start:P"), events.toString());
    }

    @Test
    public void phpProcessingInstructionSaxStartElementsShowOnlyRealElements() throws Exception {
        // characterization: no pseudo-element is ever started for the "<?php ...?>" construct; only the
        // real elements appear (plus the HTML5 synthesized BODY)
        final List<String> names = saxStartElements("<?php echo 1; ?><p>x</p>");
        assertEquals(List.of("HTML", "BODY", "P"), names);
    }

    @Test
    public void piInMiddleOfDivLeaksIntoTextMergedWithFollowingContent() throws Exception {
        // characterization: the PI becomes a bogus comment (HTML5), so it is reported as a separate
        // comment event and the following "after" text is a separate characters run
        final List<String> events = lexicalEvents("<div><?target data?>after</div>");
        assertTrue(events.contains("comment:?target data?"), events.toString());
        assertTrue(events.contains("chars:after"), events.toString());
    }

    @Test
    public void piInMiddleOfDivDomChildSignatureLocksExactText() throws Exception {
        // characterization: the PI becomes a Comment node inside DIV, followed by the "after" text
        final Document doc = parse("<div><?target data?>after</div>");
        final List<String> sig = childSignature(doc.getElementsByTagName("DIV").item(0));
        assertEquals(List.of("comment:?target data?", "text:after"), sig);
    }

    @Test
    public void piInMiddleDoesNotEmitStartElementForTargetPseudoTag() throws Exception {
        final List<String> events = lexicalEvents("<div><?target data?>after</div>");
        assertTrue(events.stream().noneMatch(e -> e.equals("start:target") || e.equals("start:?target")), events.toString());
    }

    @Test
    public void saxEventsForXmlDeclarationContainNoQuestionMarkStartTag() throws Exception {
        final List<String> events = saxEvents("<?xml version=\"1.0\"?><p>x</p>");
        assertTrue(events.stream().noneMatch(e -> e.startsWith("start:?")), events.toString());
    }

    @Test
    public void multiplePiLikeConstructsInSequenceEachLeakAsSeparateTextRuns() throws Exception {
        // characterization: each "<?...?>" construct becomes its own bogus comment (HTML5), so two
        // adjacent PI-like constructs are reported as TWO separate "comment:" events
        final List<String> events = lexicalEvents("<?a?><?b?><p>x</p>");
        assertTrue(events.contains("comment:?a?"), events.toString());
        assertTrue(events.contains("comment:?b?"), events.toString());
        assertTrue(events.indexOf("comment:?a?") < events.indexOf("comment:?b?"), events.toString());
        assertTrue(events.indexOf("comment:?b?") < events.indexOf("start:P"), events.toString());
    }

    @Test
    public void piConstructDoesNotAffectVoidElementHandlingAfterward() throws Exception {
        final Document doc = parse("<?xml version=\"1.0\"?><img src=\"a.png\">");
        assertEquals(1, count(doc, "//IMG"));
    }
}
