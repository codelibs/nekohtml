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

import static org.codelibs.nekohtml.parsers.BrokenHtmlTestSupport.saxEvents;
import static org.codelibs.nekohtml.parsers.ValidHtmlTestSupport.saxEventsWithFeature;
import static org.codelibs.nekohtml.parsers.ValidHtmlTestSupport.saxQNameUriLocal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Characterization tests locking in which SAX features are actual NO-OPs (stored in the feature
 * map but never consulted by {@code SimpleHTMLScanner}/the parsing pipeline for observable output)
 * versus {@code balance-tags}, which is the only feature that demonstrably changes the SAX event
 * stream.
 *
 * <p>
 * This is a regression guard: if a future change wires one of the currently-inert features into
 * the scanner, the corresponding "no-op" test below will start failing, which is the intended
 * signal that the characterization needs to be revisited.
 * </p>
 */
public class ValidConfigNoOpGuardsTest {

    private static final String NAMESPACES = "http://xml.org/sax/features/namespaces";

    private static final String AUGMENTATIONS = "http://cyberneko.org/html/features/augmentations";

    private static final String REPORT_ERRORS = "http://cyberneko.org/html/features/report-errors";

    private static final String SIMPLE_ERROR_FORMAT = "http://cyberneko.org/html/features/report-errors/simple-format";

    private static final String BALANCE_TAGS = "http://cyberneko.org/html/features/balance-tags";

    private static final String HTML5_MODE = "http://cyberneko.org/html/features/html5-mode";

    private static final String WELL_FORMED_HTML = "<html><body><p>hi</p></body></html>";

    private static final String HEADLESS_HTML = "<p>hi</p>";

    private static final String ATTR_HTML = "<div id=\"a\">x</div>";

    // =========================================================================
    // NAMESPACES: no-op
    // =========================================================================

    @Test
    public void namespacesTrueIsNoOpOnWellFormedHtml() throws Exception {
        assertEquals(saxEvents(WELL_FORMED_HTML), saxEventsWithFeature(WELL_FORMED_HTML, NAMESPACES, true));
    }

    @Test
    public void namespacesFalseIsNoOpOnWellFormedHtml() throws Exception {
        assertEquals(saxEvents(WELL_FORMED_HTML), saxEventsWithFeature(WELL_FORMED_HTML, NAMESPACES, false));
    }

    @Test
    public void namespacesTrueIsNoOpOnHeadlessHtml() throws Exception {
        assertEquals(saxEvents(HEADLESS_HTML), saxEventsWithFeature(HEADLESS_HTML, NAMESPACES, true));
    }

    @Test
    public void namespacesTrueIsNoOpOnHtmlWithAttributes() throws Exception {
        assertEquals(saxEvents(ATTR_HTML), saxEventsWithFeature(ATTR_HTML, NAMESPACES, true));
    }

    @Test
    public void namespacesTrueDoesNotActuallyEnableNamespaceProcessing() throws Exception {
        // characterization: setting NAMESPACES=true does not change the qName/uri/localName
        // reporting at all; uri is always empty because namespace processing is never performed.
        final List<String> out = saxQNameUriLocal(WELL_FORMED_HTML);
        assertFalse(out.isEmpty(), out.toString());
        for (final String entry : out) {
            assertTrue(entry.contains("|uri=|"), entry);
        }
    }

    // =========================================================================
    // AUGMENTATIONS: no-op
    // =========================================================================

    @Test
    public void augmentationsTrueIsNoOpOnWellFormedHtml() throws Exception {
        assertEquals(saxEvents(WELL_FORMED_HTML), saxEventsWithFeature(WELL_FORMED_HTML, AUGMENTATIONS, true));
    }

    @Test
    public void augmentationsFalseIsNoOpOnWellFormedHtml() throws Exception {
        assertEquals(saxEvents(WELL_FORMED_HTML), saxEventsWithFeature(WELL_FORMED_HTML, AUGMENTATIONS, false));
    }

    @Test
    public void augmentationsTrueIsNoOpOnHeadlessHtml() throws Exception {
        assertEquals(saxEvents(HEADLESS_HTML), saxEventsWithFeature(HEADLESS_HTML, AUGMENTATIONS, true));
    }

    @Test
    public void augmentationsTrueIsNoOpOnHtmlWithAttributes() throws Exception {
        assertEquals(saxEvents(ATTR_HTML), saxEventsWithFeature(ATTR_HTML, AUGMENTATIONS, true));
    }

    // =========================================================================
    // REPORT_ERRORS: no-op (well-formed input has nothing to report anyway, but this locks
    // that turning it on doesn't alter the observable SAX event stream via the content handler)
    // =========================================================================

    @Test
    public void reportErrorsTrueIsNoOpOnWellFormedHtml() throws Exception {
        assertEquals(saxEvents(WELL_FORMED_HTML), saxEventsWithFeature(WELL_FORMED_HTML, REPORT_ERRORS, true));
    }

    @Test
    public void reportErrorsFalseIsNoOpOnWellFormedHtml() throws Exception {
        assertEquals(saxEvents(WELL_FORMED_HTML), saxEventsWithFeature(WELL_FORMED_HTML, REPORT_ERRORS, false));
    }

    @Test
    public void reportErrorsTrueIsNoOpOnHeadlessHtml() throws Exception {
        assertEquals(saxEvents(HEADLESS_HTML), saxEventsWithFeature(HEADLESS_HTML, REPORT_ERRORS, true));
    }

    // =========================================================================
    // SIMPLE_ERROR_FORMAT: no-op
    // =========================================================================

    @Test
    public void simpleErrorFormatTrueIsNoOpOnWellFormedHtml() throws Exception {
        assertEquals(saxEvents(WELL_FORMED_HTML), saxEventsWithFeature(WELL_FORMED_HTML, SIMPLE_ERROR_FORMAT, true));
    }

    @Test
    public void simpleErrorFormatFalseIsNoOpOnWellFormedHtml() throws Exception {
        assertEquals(saxEvents(WELL_FORMED_HTML), saxEventsWithFeature(WELL_FORMED_HTML, SIMPLE_ERROR_FORMAT, false));
    }

    @Test
    public void simpleErrorFormatTrueIsNoOpOnHtmlWithAttributes() throws Exception {
        assertEquals(saxEvents(ATTR_HTML), saxEventsWithFeature(ATTR_HTML, SIMPLE_ERROR_FORMAT, true));
    }

    // =========================================================================
    // HTML5_MODE: no-op
    // =========================================================================

    @Test
    public void html5ModeTrueIsNoOpOnWellFormedHtml() throws Exception {
        assertEquals(saxEvents(WELL_FORMED_HTML), saxEventsWithFeature(WELL_FORMED_HTML, HTML5_MODE, true));
    }

    @Test
    public void html5ModeFalseIsNoOpOnWellFormedHtml() throws Exception {
        assertEquals(saxEvents(WELL_FORMED_HTML), saxEventsWithFeature(WELL_FORMED_HTML, HTML5_MODE, false));
    }

    @Test
    public void html5ModeTrueIsNoOpOnHeadlessHtml() throws Exception {
        assertEquals(saxEvents(HEADLESS_HTML), saxEventsWithFeature(HEADLESS_HTML, HTML5_MODE, true));
    }

    @Test
    public void html5ModeTrueIsNoOpOnHtmlWithAttributes() throws Exception {
        assertEquals(saxEvents(ATTR_HTML), saxEventsWithFeature(ATTR_HTML, HTML5_MODE, true));
    }

    // =========================================================================
    // BALANCE_TAGS: the one feature that DOES change output
    // =========================================================================

    @Test
    public void balanceTagsTrueEqualsBaselineDefault() throws Exception {
        // characterization: balance-tags defaults to true, so explicitly setting it true reproduces
        // the same event stream as the baseline helper (which never touches this feature).
        assertEquals(saxEvents(WELL_FORMED_HTML), saxEventsWithFeature(WELL_FORMED_HTML, BALANCE_TAGS, true));
    }

    @Test
    public void balanceTagsFalseChangesUnclosedParagraphEventStream() throws Exception {
        final List<String> balanced = saxEvents("<p>text");
        final List<String> unbalanced = saxEventsWithFeature("<p>text", BALANCE_TAGS, false);

        // characterization: turning balance-tags off produces an observably different event stream
        assertNotEquals(balanced, unbalanced);
        assertTrue(balanced.contains("end:P"), balanced.toString());
        assertFalse(unbalanced.contains("end:P"), unbalanced.toString());
    }

    @Test
    public void balanceTagsFalseSkipsImplicitHtmlRootInsertion() throws Exception {
        final List<String> unbalanced = saxEventsWithFeature("<p>text", BALANCE_TAGS, false);
        // characterization: with the tag balancer filter removed from the pipeline, the implicit
        // HTML root insertion (which lives inside HTMLTagBalancerFilter) never happens either.
        assertFalse(unbalanced.contains("start:HTML"), unbalanced.toString());
        assertTrue(unbalanced.contains("start:P"), unbalanced.toString());
    }

    @Test
    public void balanceTagsFalseStillAutoClosesVoidElements() throws Exception {
        // characterization: void-element auto-closing happens in the scanner itself (not the
        // balancer filter), so it still fires even with balance-tags off.
        final List<String> events = saxEventsWithFeature("<br>", BALANCE_TAGS, false);
        assertTrue(events.contains("start:BR"), events.toString());
        assertTrue(events.contains("end:BR"), events.toString());
    }

    @Test
    public void balanceTagsFalseOnAlreadyWellFormedHtmlLocksActualEventStream() throws Exception {
        final List<String> baseline = saxEvents(WELL_FORMED_HTML);
        final List<String> unbalanced = saxEventsWithFeature(WELL_FORMED_HTML, BALANCE_TAGS, false);
        // characterization: with input that is already fully well-formed, disabling the tag
        // balancer filter produces the identical event stream to the balanced baseline, since there
        // was nothing left to balance/auto-insert.
        assertEquals(baseline, unbalanced);
    }

    // =========================================================================
    // Round-trip getFeature for BALANCE_TAGS on the public SAXParser
    // =========================================================================

    @Test
    public void balanceTagsFeatureRoundTripsThroughSetAndGet() throws Exception {
        final SAXParser parser = new SAXParser();
        assertTrue(parser.getFeature(BALANCE_TAGS));
        parser.setFeature(BALANCE_TAGS, false);
        assertFalse(parser.getFeature(BALANCE_TAGS));
        parser.setFeature(BALANCE_TAGS, true);
        assertTrue(parser.getFeature(BALANCE_TAGS));
    }
}
