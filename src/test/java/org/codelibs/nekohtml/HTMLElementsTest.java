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
package org.codelibs.nekohtml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.codelibs.nekohtml.HTMLElements.Element;
import org.codelibs.nekohtml.HTMLElements.ElementList;
import org.junit.jupiter.api.Test;

/**
 * Test cases for HTMLElements.
 * Tests HTML element metadata and lookup functionality.
 */
public class HTMLElementsTest {

    @Test
    public void testConstructor() {
        // When: HTMLElements is instantiated
        final HTMLElements elements = new HTMLElements();

        // Then: Instance should be created
        assertNotNull(elements, "HTMLElements should be instantiated");
    }

    @Test
    public void testGetElementByCode() {
        // Given: Element codes
        final short divCode = HTMLElements.DIV;
        final short pCode = HTMLElements.P;

        // When: Getting elements by code
        final Element div = HTMLElements.getElement(divCode);
        final Element p = HTMLElements.getElement(pCode);

        // Then: Elements should be retrieved correctly
        assertNotNull(div, "DIV element should be found");
        assertEquals("DIV", div.name, "Element name should be DIV");

        assertNotNull(p, "P element should be found");
        assertEquals("P", p.name, "Element name should be P");
    }

    @Test
    public void testGetElementByName() {
        // Given: Element names
        final String divName = "div";
        final String pName = "P";

        // When: Getting elements by name (case-insensitive)
        final Element div = HTMLElements.getElement(divName);
        final Element p = HTMLElements.getElement(pName);

        // Then: Elements should be retrieved correctly
        assertNotNull(div, "DIV element should be found");
        assertEquals("DIV", div.name, "Element name should be uppercase DIV");

        assertNotNull(p, "P element should be found");
        assertEquals("P", p.name, "Element name should be P");
    }

    @Test
    public void testGetElementCaseInsensitive() {
        // Given: Various case combinations
        final Element lower = HTMLElements.getElement("html");
        final Element upper = HTMLElements.getElement("HTML");
        final Element mixed = HTMLElements.getElement("HtMl");

        // Then: All should return the same element
        assertNotNull(lower, "Should find lowercase html");
        assertNotNull(upper, "Should find uppercase HTML");
        assertNotNull(mixed, "Should find mixed case HtMl");

        assertEquals("HTML", lower.name, "All should resolve to HTML");
        assertEquals("HTML", upper.name, "All should resolve to HTML");
        assertEquals("HTML", mixed.name, "All should resolve to HTML");
    }

    @Test
    public void testGetUnknownElement() {
        // Given: Unknown element name
        final String unknownName = "custom-element";

        // When: Getting unknown element
        final Element element = HTMLElements.getElement(unknownName);

        // Then: Should return element with UNKNOWN code
        assertNotNull(element, "Unknown element should return element");
        assertEquals(HTMLElements.UNKNOWN, element.code, "Should have UNKNOWN code");
        assertEquals("CUSTOM-ELEMENT", element.name, "Name should be uppercased");
    }

    @Test
    public void testGetElementWithEmptyName() {
        // Given: Empty element name
        final String emptyName = "";

        // When: Getting element with empty name
        final Element element = HTMLElements.getElement(emptyName, HTMLElements.NO_SUCH_ELEMENT);

        // Then: Should return default element
        assertSame(HTMLElements.NO_SUCH_ELEMENT, element, "Empty name should return default");
    }

    @Test
    public void testCommonHtmlElements() {
        // Test commonly used HTML elements
        assertElementExists("HTML", HTMLElements.HTML);
        assertElementExists("HEAD", HTMLElements.HEAD);
        assertElementExists("BODY", HTMLElements.BODY);
        assertElementExists("DIV", HTMLElements.DIV);
        assertElementExists("SPAN", HTMLElements.SPAN);
        assertElementExists("P", HTMLElements.P);
        assertElementExists("A", HTMLElements.A);
        assertElementExists("IMG", HTMLElements.IMG);
        assertElementExists("TABLE", HTMLElements.TABLE);
        assertElementExists("TR", HTMLElements.TR);
        assertElementExists("TD", HTMLElements.TD);
        assertElementExists("TH", HTMLElements.TH);
    }

    @Test
    public void testHtml5Elements() {
        // Test HTML5 elements
        assertElementExists("ARTICLE", HTMLElements.ARTICLE);
        assertElementExists("ASIDE", HTMLElements.ASIDE);
        assertElementExists("AUDIO", HTMLElements.AUDIO);
        assertElementExists("CANVAS", HTMLElements.CANVAS);
        assertElementExists("FOOTER", HTMLElements.FOOTER);
        assertElementExists("HEADER", HTMLElements.HEADER);
        assertElementExists("HGROUP", HTMLElements.HGROUP);
        assertElementExists("NAV", HTMLElements.NAV);
        assertElementExists("SEARCH", HTMLElements.SEARCH);
        assertElementExists("SECTION", HTMLElements.SECTION);
        assertElementExists("SLOT", HTMLElements.SLOT);
        assertElementExists("VIDEO", HTMLElements.VIDEO);
    }

    @Test
    public void testElementIsInline() {
        // Given: Inline elements
        final Element strong = HTMLElements.getElement(HTMLElements.STRONG);
        final Element em = HTMLElements.getElement(HTMLElements.EM);
        final Element code = HTMLElements.getElement(HTMLElements.CODE);

        // Then: Should be inline
        assertTrue(strong.isInline(), "STRONG should be inline");
        assertTrue(em.isInline(), "EM should be inline");
        assertTrue(code.isInline(), "CODE should be inline");
    }

    @Test
    public void testElementIsBlock() {
        // Given: Block elements
        final Element pre = HTMLElements.getElement(HTMLElements.PRE);
        final Element dl = HTMLElements.getElement(HTMLElements.DL);
        final Element h1 = HTMLElements.getElement(HTMLElements.H1);

        // Then: Should be block
        assertTrue(pre.isBlock(), "PRE should be block");
        assertTrue(dl.isBlock(), "DL should be block");
        assertTrue(h1.isBlock(), "H1 should be block");
    }

    @Test
    public void testElementIsEmpty() {
        // Given: Empty (void) elements
        final Element br = HTMLElements.getElement(HTMLElements.BR);
        final Element hr = HTMLElements.getElement(HTMLElements.HR);
        final Element img = HTMLElements.getElement(HTMLElements.IMG);
        final Element input = HTMLElements.getElement(HTMLElements.INPUT);

        // Then: Should be empty
        assertTrue(br.isEmpty(), "BR should be empty");
        assertTrue(hr.isEmpty(), "HR should be empty");
        assertTrue(img.isEmpty(), "IMG should be empty");
        assertTrue(input.isEmpty(), "INPUT should be empty");
    }

    @Test
    public void testElementIsContainer() {
        // Given: Container elements
        final Element div = HTMLElements.getElement(HTMLElements.DIV);
        final Element span = HTMLElements.getElement(HTMLElements.SPAN);
        final Element table = HTMLElements.getElement(HTMLElements.TABLE);

        // Then: Should be container
        assertTrue(div.isContainer(), "DIV should be container");
        assertTrue(span.isContainer(), "SPAN should be container");
        assertTrue(table.isContainer(), "TABLE should be container");
    }

    @Test
    public void testElementIsSpecial() {
        // Given: Special elements
        final Element script = HTMLElements.getElement(HTMLElements.SCRIPT);
        final Element style = HTMLElements.getElement(HTMLElements.STYLE);
        final Element title = HTMLElements.getElement(HTMLElements.TITLE);

        // Then: Should be special
        assertTrue(script.isSpecial(), "SCRIPT should be special");
        assertTrue(style.isSpecial(), "STYLE should be special");
        assertTrue(title.isSpecial(), "TITLE should be special");
    }

    @Test
    public void testElementCloses() {
        // Given: P element can close another P
        final Element p = HTMLElements.getElement(HTMLElements.P);

        // Then: P should close P
        assertTrue(p.closes(HTMLElements.P), "P should close P");
    }

    @Test
    public void testElementDoesNotClose() {
        // Given: DIV element
        final Element div = HTMLElements.getElement(HTMLElements.DIV);

        // Then: DIV should not close SPAN (no closes defined)
        assertFalse(div.closes(HTMLElements.SPAN), "DIV should not close SPAN");
    }

    @Test
    public void testHeadingElements() {
        // Test all heading elements
        final Element h1 = HTMLElements.getElement(HTMLElements.H1);
        final Element h2 = HTMLElements.getElement(HTMLElements.H2);
        final Element h3 = HTMLElements.getElement(HTMLElements.H3);
        final Element h4 = HTMLElements.getElement(HTMLElements.H4);
        final Element h5 = HTMLElements.getElement(HTMLElements.H5);
        final Element h6 = HTMLElements.getElement(HTMLElements.H6);

        // All should be block elements
        assertTrue(h1.isBlock(), "H1 should be block");
        assertTrue(h2.isBlock(), "H2 should be block");
        assertTrue(h3.isBlock(), "H3 should be block");
        assertTrue(h4.isBlock(), "H4 should be block");
        assertTrue(h5.isBlock(), "H5 should be block");
        assertTrue(h6.isBlock(), "H6 should be block");

        // Headings should close other headings and P
        assertTrue(h1.closes(HTMLElements.P), "H1 should close P");
        assertTrue(h1.closes(HTMLElements.H2), "H1 should close H2");
    }

    @Test
    public void testFormElements() {
        // Test form-related elements
        assertElementExists("FORM", HTMLElements.FORM);
        assertElementExists("INPUT", HTMLElements.INPUT);
        assertElementExists("TEXTAREA", HTMLElements.TEXTAREA);
        assertElementExists("SELECT", HTMLElements.SELECT);
        assertElementExists("BUTTON", HTMLElements.BUTTON);
        assertElementExists("LABEL", HTMLElements.LABEL);
    }

    @Test
    public void testTableElements() {
        // Test table structure elements
        assertElementExists("TABLE", HTMLElements.TABLE);
        assertElementExists("THEAD", HTMLElements.THEAD);
        assertElementExists("TBODY", HTMLElements.TBODY);
        assertElementExists("TFOOT", HTMLElements.TFOOT);
        assertElementExists("TR", HTMLElements.TR);
        assertElementExists("TD", HTMLElements.TD);
        assertElementExists("TH", HTMLElements.TH);
        assertElementExists("CAPTION", HTMLElements.CAPTION);
        assertElementExists("COLGROUP", HTMLElements.COLGROUP);
        assertElementExists("COL", HTMLElements.COL);
    }

    @Test
    public void testElementParent() {
        // Given: Elements with parent relationships
        final Element td = HTMLElements.getElement(HTMLElements.TD);
        final Element tr = HTMLElements.getElement(HTMLElements.TR);

        // Then: TD should have TR as parent
        assertTrue(td.isParent(tr), "TD should have TR as parent");
    }

    @Test
    public void testElementWithoutParent() {
        // Given: HTML element (root)
        final Element html = HTMLElements.getElement(HTMLElements.HTML);
        final Element div = HTMLElements.getElement(HTMLElements.DIV);

        // Then: HTML should not have parent
        assertFalse(html.isParent(div), "HTML should not have DIV as parent");
    }

    @Test
    public void testElementHashCode() {
        // Given: Two instances of same element
        final Element div1 = HTMLElements.getElement("DIV");
        final Element div2 = HTMLElements.getElement("div");

        // Then: Should have same hash code
        assertEquals(div1.hashCode(), div2.hashCode(), "Same elements should have same hash code");
    }

    @Test
    public void testElementEquals() {
        // Given: Elements
        final Element div = HTMLElements.getElement(HTMLElements.DIV);

        // Then: Element equals should work with name
        assertTrue(div.equals("DIV"), "Element should equal its name");
        assertFalse(div.equals("SPAN"), "Element should not equal different name");
    }

    @Test
    public void testElementToString() {
        // Given: Element
        final Element div = HTMLElements.getElement(HTMLElements.DIV);

        // Then: toString should contain name
        final String str = div.toString();
        assertNotNull(str, "toString should not be null");
        assertTrue(str.contains("DIV"), "toString should contain element name");
    }

    @Test
    public void testNoSuchElement() {
        // Then: NO_SUCH_ELEMENT should exist
        assertNotNull(HTMLElements.NO_SUCH_ELEMENT, "NO_SUCH_ELEMENT should exist");
        assertEquals(HTMLElements.UNKNOWN, HTMLElements.NO_SUCH_ELEMENT.code, "Should have UNKNOWN code");
    }

    @Test
    public void testElementList() {
        // Given: ElementList
        final ElementList list = new ElementList();

        // When: Adding elements
        final Element elem1 = new Element((short) 0, "TEST1", Element.BLOCK, HTMLElements.BODY, null);
        final Element elem2 = new Element((short) 1, "TEST2", Element.INLINE, HTMLElements.BODY, null);

        list.addElement(elem1);
        list.addElement(elem2);

        // Then: Elements should be in list
        assertEquals(2, list.size, "List should have 2 elements");
        assertSame(elem1, list.data[0], "First element should match");
        assertSame(elem2, list.data[1], "Second element should match");
    }

    @Test
    public void testElementListGrowth() {
        // Given: ElementList
        final ElementList list = new ElementList();

        // When: Adding more elements than initial capacity
        for (int i = 0; i < 150; i++) {
            list.addElement(new Element((short) i, "TEST" + i, Element.BLOCK, HTMLElements.BODY, null));
        }

        // Then: List should grow
        assertEquals(150, list.size, "List should have 150 elements");
        assertTrue(list.data.length >= 150, "Array should have grown");
    }

    @Test
    public void testElementConstructorWithSingleParent() {
        // Given/When: Element with single parent
        final Element elem = new Element((short) 999, "TEST", Element.BLOCK, HTMLElements.BODY, new short[] { HTMLElements.P });

        // Then: Element should be created correctly
        assertEquals("TEST", elem.name, "Name should match");
        assertEquals(Element.BLOCK, elem.flags, "Flags should match");
        assertNotNull(elem.parentCodes, "Parent codes should be set");
        assertEquals(1, elem.parentCodes.length, "Should have 1 parent");
    }

    @Test
    public void testElementConstructorWithMultipleParents() {
        // Given/When: Element with multiple parents
        final short[] parents = new short[] { HTMLElements.BODY, HTMLElements.DIV };
        final Element elem = new Element((short) 999, "TEST", Element.BLOCK, parents, new short[] { HTMLElements.P });

        // Then: Element should be created correctly
        assertNotNull(elem.parentCodes, "Parent codes should be set");
        assertEquals(2, elem.parentCodes.length, "Should have 2 parents");
    }

    @Test
    public void testElementWithBounds() {
        // Given/When: Element with bounds
        final Element elem = new Element((short) 999, "TEST", Element.BLOCK, HTMLElements.BODY, HTMLElements.TABLE, null);

        // Then: Bounds should be set
        assertEquals(HTMLElements.TABLE, elem.bounds, "Bounds should be TABLE");
    }

    @Test
    public void testDeprecatedElements() {
        // Test deprecated but still supported elements
        assertElementExists("FONT", HTMLElements.FONT);
        assertElementExists("CENTER", HTMLElements.CENTER);
        assertElementExists("STRIKE", HTMLElements.STRIKE);
        assertElementExists("BASEFONT", HTMLElements.BASEFONT);
    }

    @Test
    public void testRubyElements() {
        // Test Ruby annotation elements
        assertElementExists("RUBY", HTMLElements.RUBY);
        assertElementExists("RB", HTMLElements.RB);
        assertElementExists("RT", HTMLElements.RT);
        assertElementExists("RTC", HTMLElements.RTC);
        assertElementExists("RP", HTMLElements.RP);
    }

    @Test
    public void testListElements() {
        // Test list elements
        final Element ul = HTMLElements.getElement(HTMLElements.UL);
        final Element ol = HTMLElements.getElement(HTMLElements.OL);
        final Element li = HTMLElements.getElement(HTMLElements.LI);
        final Element dl = HTMLElements.getElement(HTMLElements.DL);
        final Element dt = HTMLElements.getElement(HTMLElements.DT);
        final Element dd = HTMLElements.getElement(HTMLElements.DD);

        assertNotNull(ul, "UL should exist");
        assertNotNull(ol, "OL should exist");
        assertNotNull(li, "LI should exist");
        assertNotNull(dl, "DL should exist");
        assertNotNull(dt, "DT should exist");
        assertNotNull(dd, "DD should exist");

        // LI should close LI and P
        assertTrue(li.closes(HTMLElements.LI), "LI should close LI");
        assertTrue(li.closes(HTMLElements.P), "LI should close P");
    }

    @Test
    public void testMetadataElements() {
        // Test metadata elements
        assertElementExists("TITLE", HTMLElements.TITLE);
        assertElementExists("META", HTMLElements.META);
        assertElementExists("LINK", HTMLElements.LINK);
        assertElementExists("BASE", HTMLElements.BASE);
        assertElementExists("STYLE", HTMLElements.STYLE);
        assertElementExists("SCRIPT", HTMLElements.SCRIPT);
    }

    @Test
    public void testMultimediaElements() {
        // Test multimedia elements
        assertElementExists("AUDIO", HTMLElements.AUDIO);
        assertElementExists("VIDEO", HTMLElements.VIDEO);
        assertElementExists("SOURCE", HTMLElements.SOURCE);
        assertElementExists("TRACK", HTMLElements.TRACK);
        assertElementExists("CANVAS", HTMLElements.CANVAS);
    }

    @Test
    public void testGetElementWithDefaultElement() {
        // Given: Custom default element
        final Element defaultElem = new Element((short) 9999, "DEFAULT", Element.BLOCK, HTMLElements.BODY, null);

        // When: Searching for non-existent element
        final Element result = HTMLElements.getElement("non-existent-xyz", defaultElem);

        // Then: Should return default element
        assertSame(defaultElem, result, "Should return provided default element");
    }

    @Test
    public void testNewHtml5Elements() {
        // Test newly added HTML Living Standard elements
        // SEARCH element
        final Element search = HTMLElements.getElement(HTMLElements.SEARCH);
        assertNotNull(search, "SEARCH element should exist");
        assertEquals("SEARCH", search.name, "SEARCH name should match");
        assertTrue(search.isContainer(), "SEARCH should be a container");
        assertFalse(search.isEmpty(), "SEARCH should not be empty");

        // SLOT element
        final Element slot = HTMLElements.getElement(HTMLElements.SLOT);
        assertNotNull(slot, "SLOT element should exist");
        assertEquals("SLOT", slot.name, "SLOT name should match");
        assertTrue(slot.isContainer(), "SLOT should be a container");
        assertFalse(slot.isEmpty(), "SLOT should not be empty");

        // HGROUP element
        final Element hgroup = HTMLElements.getElement(HTMLElements.HGROUP);
        assertNotNull(hgroup, "HGROUP element should exist");
        assertEquals("HGROUP", hgroup.name, "HGROUP name should match");
        assertTrue(hgroup.isBlock(), "HGROUP should be a block element");
        assertFalse(hgroup.isEmpty(), "HGROUP should not be empty");
    }

    @Test
    public void testElementNotEquals() {
        // Given: Different elements
        final Element div = HTMLElements.getElement(HTMLElements.DIV);
        final Element span = HTMLElements.getElement(HTMLElements.SPAN);

        // Then: Should not be equal
        assertNotEquals(div.hashCode(), span.hashCode(), "Different elements should have different hash codes");
        assertFalse(div.equals("SPAN"), "DIV should not equal SPAN");
    }

    // Helper method
    private void assertElementExists(final String name, final short code) {
        final Element element = HTMLElements.getElement(name);
        assertNotNull(element, name + " element should exist");
        assertEquals(name, element.name, "Element name should match");
        assertEquals(code, element.code, "Element code should match");
    }

} // class HTMLElementsTest
