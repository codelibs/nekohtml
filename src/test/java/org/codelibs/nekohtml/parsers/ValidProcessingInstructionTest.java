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
 * HTML. The current {@code SimpleHTMLScanner} never recognizes {@code <?...?>} constructs: nothing
 * ever calls {@code ContentHandler.processingInstruction}. Instead, the leading {@code '<'} is
 * silently dropped (treated as an unrecognized tag) and the remainder of the construct leaks into
 * the surrounding text as ordinary characters.
 */
public class ValidProcessingInstructionTest {

    @Test
    public void xmlDeclarationEmitsNoPiEvent() throws Exception {
        final List<String> events = lexicalEvents("<?xml version=\"1.0\" encoding=\"UTF-8\"?><p>x</p>");
        assertTrue(events.stream().noneMatch(e -> e.startsWith("pi:")), events.toString());
    }

    @Test
    public void xmlDeclarationLeaksAsCharactersContainingDeclarationText() throws Exception {
        final List<String> events = lexicalEvents("<?xml version=\"1.0\" encoding=\"UTF-8\"?><p>x</p>");
        assertTrue(events.stream().anyMatch(e -> e.startsWith("chars:") && e.contains("xml version")), events.toString());
    }

    @Test
    public void xmlDeclarationLeakedTextDropsOnlyLeadingAngleBracket() throws Exception {
        // characterization: only the leading '<' is dropped; the rest, including the closing "?>",
        // survives verbatim in the leaked characters
        final List<String> events = lexicalEvents("<?xml version=\"1.0\" encoding=\"UTF-8\"?><p>x</p>");
        assertTrue(events.contains("chars:?xml version=\"1.0\" encoding=\"UTF-8\"?>"), events.toString());
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
        final Document doc = parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?><p>x</p>");
        final List<String> sig = childSignature(doc.getDocumentElement());
        assertTrue(sig.stream().anyMatch(s -> s.startsWith("text:") && s.contains("xml version")), sig.toString());
    }

    @Test
    public void phpProcessingInstructionEmitsNoPiEvent() throws Exception {
        final List<String> events = lexicalEvents("<?php echo 1; ?><p>x</p>");
        assertTrue(events.stream().noneMatch(e -> e.startsWith("pi:")), events.toString());
    }

    @Test
    public void phpProcessingInstructionLeaksAsCharactersText() throws Exception {
        final List<String> events = lexicalEvents("<?php echo 1; ?><p>x</p>");
        assertTrue(events.contains("chars:?php echo 1; ?>"), events.toString());
        assertTrue(events.contains("start:P"), events.toString());
    }

    @Test
    public void phpProcessingInstructionSaxStartElementsShowOnlyRealElements() throws Exception {
        // characterization: no pseudo-element is ever started for the "<?php ...?>" construct
        final List<String> names = saxStartElements("<?php echo 1; ?><p>x</p>");
        assertEquals(List.of("HTML", "P"), names);
    }

    @Test
    public void piInMiddleOfDivLeaksIntoTextMergedWithFollowingContent() throws Exception {
        // characterization: '<' is dropped, "?target data?>" leaks as text, and it merges with the
        // following "after" text into a single characters run (no tag boundary between them)
        final List<String> events = lexicalEvents("<div><?target data?>after</div>");
        assertTrue(events.contains("chars:?target data?>after"), events.toString());
    }

    @Test
    public void piInMiddleOfDivDomChildSignatureLocksExactText() throws Exception {
        final Document doc = parse("<div><?target data?>after</div>");
        final List<String> sig = childSignature(doc.getElementsByTagName("DIV").item(0));
        assertEquals(List.of("text:?target data?>after"), sig);
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
        // characterization: each "<?...?>" construct starts with its own dropped '<', which resets
        // the text-scanning boundary; two adjacent PI-like constructs therefore leak as TWO separate
        // "chars:" events rather than merging into one, unlike a single PI followed by plain text
        final List<String> events = lexicalEvents("<?a?><?b?><p>x</p>");
        assertTrue(events.contains("chars:?a?>"), events.toString());
        assertTrue(events.contains("chars:?b?>"), events.toString());
        assertTrue(events.indexOf("chars:?a?>") < events.indexOf("chars:?b?>"), events.toString());
        assertTrue(events.indexOf("chars:?b?>") < events.indexOf("start:P"), events.toString());
    }

    @Test
    public void piConstructDoesNotAffectVoidElementHandlingAfterward() throws Exception {
        final Document doc = parse("<?xml version=\"1.0\"?><img src=\"a.png\">");
        assertEquals(1, count(doc, "//IMG"));
    }
}
