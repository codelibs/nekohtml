/*
 * Copyright 2025 CodeLibs, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codelibs.nekohtml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for HTML5 element definitions.
 */
public class HTML5ElementsTest {

    @Test
    @DisplayName("HTML5 media elements should be defined as CONTAINER")
    void testHTML5MediaElements() {
        // Test AUDIO element
        HTMLElements.Element audio = HTMLElements.getElement(HTMLElements.AUDIO);
        assertNotNull(audio, "AUDIO element should exist");
        assertTrue(audio.isContainer(), "AUDIO should be a container element");
        assertFalse(audio.isEmpty(), "AUDIO should not be empty");

        // Test VIDEO element
        HTMLElements.Element video = HTMLElements.getElement(HTMLElements.VIDEO);
        assertNotNull(video, "VIDEO element should exist");
        assertTrue(video.isContainer(), "VIDEO should be a container element");
        assertFalse(video.isEmpty(), "VIDEO should not be empty");

        // Test CANVAS element
        HTMLElements.Element canvas = HTMLElements.getElement(HTMLElements.CANVAS);
        assertNotNull(canvas, "CANVAS element should exist");
        assertTrue(canvas.isContainer(), "CANVAS should be a container element");
        assertFalse(canvas.isEmpty(), "CANVAS should not be empty");

        // Test PICTURE element
        HTMLElements.Element picture = HTMLElements.getElement(HTMLElements.PICTURE);
        assertNotNull(picture, "PICTURE element should exist");
        assertTrue(picture.isContainer(), "PICTURE should be a container element");
        assertFalse(picture.isEmpty(), "PICTURE should not be empty");
    }

    @Test
    @DisplayName("HTML5 form elements should be properly defined")
    void testHTML5FormElements() {
        // Test PROGRESS element
        HTMLElements.Element progress = HTMLElements.getElement(HTMLElements.PROGRESS);
        assertNotNull(progress, "PROGRESS element should exist");
        assertTrue(progress.isContainer(), "PROGRESS should be a container element");
        assertFalse(progress.isEmpty(), "PROGRESS should not be empty");

        // Test METER element
        HTMLElements.Element meter = HTMLElements.getElement(HTMLElements.METER);
        assertNotNull(meter, "METER element should exist");
        assertTrue(meter.isContainer(), "METER should be a container element");
        assertFalse(meter.isEmpty(), "METER should not be empty");

        // Test OUTPUT element
        HTMLElements.Element output = HTMLElements.getElement(HTMLElements.OUTPUT);
        assertNotNull(output, "OUTPUT element should exist");
        assertTrue(output.isContainer(), "OUTPUT should be a container element");
        assertFalse(output.isEmpty(), "OUTPUT should not be empty");

        // Test DATALIST element
        HTMLElements.Element datalist = HTMLElements.getElement(HTMLElements.DATALIST);
        assertNotNull(datalist, "DATALIST element should exist");
        assertTrue(datalist.isContainer(), "DATALIST should be a container element");
        assertFalse(datalist.isEmpty(), "DATALIST should not be empty");
    }

    @Test
    @DisplayName("HTML5 void elements should be defined as EMPTY")
    void testHTML5VoidElements() {
        // Test SOURCE element (void element)
        HTMLElements.Element source = HTMLElements.getElement(HTMLElements.SOURCE);
        assertNotNull(source, "SOURCE element should exist");
        assertTrue(source.isEmpty(), "SOURCE should be an empty element");
        assertFalse(source.isContainer(), "SOURCE should not be a container");

        // Test TRACK element (void element)
        HTMLElements.Element track = HTMLElements.getElement(HTMLElements.TRACK);
        assertNotNull(track, "TRACK element should exist");
        assertTrue(track.isEmpty(), "TRACK should be an empty element");
        assertFalse(track.isContainer(), "TRACK should not be a container");

        // Test WBR element (void element)
        HTMLElements.Element wbr = HTMLElements.getElement(HTMLElements.WBR);
        assertNotNull(wbr, "WBR element should exist");
        assertTrue(wbr.isEmpty(), "WBR should be an empty element");
        assertFalse(wbr.isContainer(), "WBR should not be a container");

        // Test EMBED element (void element)
        HTMLElements.Element embed = HTMLElements.getElement(HTMLElements.EMBED);
        assertNotNull(embed, "EMBED element should exist");
        assertTrue(embed.isEmpty(), "EMBED should be an empty element");
        assertFalse(embed.isContainer(), "EMBED should not be a container");
    }

    @Test
    @DisplayName("HTML5 semantic elements should be properly defined")
    void testHTML5SemanticElements() {
        // Test ARTICLE element
        HTMLElements.Element article = HTMLElements.getElement(HTMLElements.ARTICLE);
        assertNotNull(article, "ARTICLE element should exist");
        assertTrue(article.isContainer(), "ARTICLE should be a container element");

        // Test ASIDE element
        HTMLElements.Element aside = HTMLElements.getElement(HTMLElements.ASIDE);
        assertNotNull(aside, "ASIDE element should exist");
        assertTrue(aside.isBlock(), "ASIDE should be a block element");

        // Test FOOTER element
        HTMLElements.Element footer = HTMLElements.getElement(HTMLElements.FOOTER);
        assertNotNull(footer, "FOOTER element should exist");
        assertTrue(footer.isContainer(), "FOOTER should be a container element");

        // Test HEADER element
        HTMLElements.Element header = HTMLElements.getElement(HTMLElements.HEADER);
        assertNotNull(header, "HEADER element should exist");
        assertTrue(header.isContainer(), "HEADER should be a container element");

        // Test MAIN element
        HTMLElements.Element main = HTMLElements.getElement(HTMLElements.MAIN);
        assertNotNull(main, "MAIN element should exist");
        assertTrue(main.isContainer(), "MAIN should be a container element");

        // Test NAV element
        HTMLElements.Element nav = HTMLElements.getElement(HTMLElements.NAV);
        assertNotNull(nav, "NAV element should exist");
        assertTrue(nav.isContainer(), "NAV should be a container element");

        // Test SECTION element
        HTMLElements.Element section = HTMLElements.getElement(HTMLElements.SECTION);
        assertNotNull(section, "SECTION element should exist");
        assertTrue(section.isContainer(), "SECTION should be a container element");
    }

    @Test
    @DisplayName("HTML5 interactive elements should be properly defined")
    void testHTML5InteractiveElements() {
        // Test DETAILS element
        HTMLElements.Element details = HTMLElements.getElement(HTMLElements.DETAILS);
        assertNotNull(details, "DETAILS element should exist");
        assertTrue(details.isBlock(), "DETAILS should be a block element");

        // Test SUMMARY element
        HTMLElements.Element summary = HTMLElements.getElement(HTMLElements.SUMMARY);
        assertNotNull(summary, "SUMMARY element should exist");
        assertTrue(summary.isInline(), "SUMMARY should be an inline element");
        // SUMMARY element's parent is correctly set to DETAILS
        assertNotNull(summary.parent, "SUMMARY should have a parent element");
        assertEquals(1, summary.parent.length, "SUMMARY should have exactly one parent");
        assertEquals(HTMLElements.DETAILS, summary.parent[0].code, "SUMMARY parent should be DETAILS");

        // Test DIALOG element
        HTMLElements.Element dialog = HTMLElements.getElement(HTMLElements.DIALOG);
        assertNotNull(dialog, "DIALOG element should exist");
        assertTrue(dialog.isBlock(), "DIALOG should be a block element");
    }

    @Test
    @DisplayName("HTML5 text-level semantic elements should be properly defined")
    void testHTML5TextLevelElements() {
        // Test MARK element
        HTMLElements.Element mark = HTMLElements.getElement(HTMLElements.MARK);
        assertNotNull(mark, "MARK element should exist");
        assertTrue(mark.isInline(), "MARK should be an inline element");

        // Test TIME element
        HTMLElements.Element time = HTMLElements.getElement(HTMLElements.TIME);
        assertNotNull(time, "TIME element should exist");
        assertTrue(time.isInline(), "TIME should be an inline element");

        // Test BDI element
        HTMLElements.Element bdi = HTMLElements.getElement(HTMLElements.BDI);
        assertNotNull(bdi, "BDI element should exist");
        assertTrue(bdi.isInline(), "BDI should be an inline element");

        // Test DATA element
        HTMLElements.Element data = HTMLElements.getElement(HTMLElements.DATA);
        assertNotNull(data, "DATA element should exist");
        assertTrue(data.isInline(), "DATA should be an inline element");
    }

    @Test
    @DisplayName("HTML5 template element should be special")
    void testHTML5TemplateElement() {
        // Test TEMPLATE element
        HTMLElements.Element template = HTMLElements.getElement(HTMLElements.TEMPLATE);
        assertNotNull(template, "TEMPLATE element should exist");
        assertTrue(template.isSpecial(), "TEMPLATE should be a special element");
    }
}
