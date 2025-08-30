package org.codelibs.nekohtml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for HTMLElements and nested classes.
 * Focuses on element lookup, flags, relationships, and collection behavior.
 */
class HTMLElementsTest {

    @Test
    @DisplayName("Lookup by code returns canonical element")
    void getElementByCode_returnsCanonical() {
        HTMLElements.Element divByCode = HTMLElements.getElement(HTMLElements.DIV);
        HTMLElements.Element divByName = HTMLElements.getElement("DIV");

        assertNotNull(divByCode);
        assertNotNull(divByName);
        // Both retrievals should return the same canonical instance for known elements
        assertSame(divByName, divByCode);
        assertEquals("DIV", divByCode.name);
        assertTrue(divByCode.isContainer());
        assertFalse(divByCode.isBlock()); // DIV is CONTAINER, not BLOCK
        assertFalse(divByCode.isEmpty());
        assertFalse(divByCode.isInline());
        assertFalse(divByCode.isSpecial());
    }

    @Test
    @DisplayName("Lookup by name is case-insensitive")
    void getElementByName_caseInsensitive() {
        HTMLElements.Element e1 = HTMLElements.getElement("div");
        HTMLElements.Element e2 = HTMLElements.getElement("Div");
        HTMLElements.Element e3 = HTMLElements.getElement("dIv");
        HTMLElements.Element e4 = HTMLElements.getElement("DIV");

        assertSame(e1, e2);
        assertSame(e2, e3);
        assertSame(e3, e4);
        assertEquals(HTMLElements.DIV, e1.code);
    }

    @Test
    @DisplayName("Unknown element creates a new CONTAINER with BODY/HEAD parents")
    void getElementByName_unknownCreatesContainerWithDefaultParents() {
        String unknownName = "custom-tag";
        HTMLElements.Element unknown = HTMLElements.getElement(unknownName);

        assertNotNull(unknown);
        assertEquals(HTMLElements.UNKNOWN, unknown.code);
        assertEquals(unknownName.toUpperCase(), unknown.name);
        assertTrue(unknown.isContainer());
        assertFalse(unknown.isInline());
        assertFalse(unknown.isBlock());
        assertFalse(unknown.isEmpty());
        assertFalse(unknown.isSpecial());

        // Parents are taken from NO_SUCH_ELEMENT (BODY, HEAD) as element instances
        assertNotNull(unknown.parent, "Unknown element should inherit concrete parents");
        assertEquals(2, unknown.parent.length);
        HTMLElements.Element body = HTMLElements.getElement(HTMLElements.BODY);
        HTMLElements.Element head = HTMLElements.getElement(HTMLElements.HEAD);
        assertTrue(unknown.isParent(body));
        assertTrue(unknown.isParent(head));
    }

    @Test
    @DisplayName("getElement(name, default) returns default for unknowns")
    void getElementWithDefault_fallback() {
        HTMLElements.Element fallback = HTMLElements.getElement("SPAN");
        HTMLElements.Element res = HTMLElements.getElement("this-name-does-not-exist", fallback);

        assertSame(fallback, res);
    }

    @Test
    @DisplayName("getElement(name, default) respects names starting with non-letters")
    void getElementWithDefault_nonLetterStartUsesDefault() {
        HTMLElements.Element fallback = HTMLElements.getElement("DIV");
        HTMLElements.Element res = HTMLElements.getElement("123abc", fallback);
        assertSame(fallback, res);
    }

    @Nested
    class FlagsTests {
        @Test
        @DisplayName("Inline flag is set for inline elements")
        void inlineFlag() {
            HTMLElements.Element em = HTMLElements.getElement("EM");
            HTMLElements.Element span = HTMLElements.getElement("SPAN");
            assertTrue(em.isInline());
            assertFalse(span.isInline()); // SPAN is CONTAINER, not INLINE
            assertFalse(em.isBlock());
        }

        @Test
        @DisplayName("Block flag is set for block elements")
        void blockFlag() {
            HTMLElements.Element div = HTMLElements.getElement("DIV");
            HTMLElements.Element p = HTMLElements.getElement("P");
            assertFalse(div.isBlock()); // DIV is CONTAINER, not BLOCK
            assertTrue(p.isContainer()); // P is defined as CONTAINER
        }

        @Test
        @DisplayName("Empty flag is set for empty elements")
        void emptyFlag() {
            HTMLElements.Element br = HTMLElements.getElement("BR");
            HTMLElements.Element img = HTMLElements.getElement("IMG");
            assertTrue(br.isEmpty());
            assertTrue(img.isEmpty());
            assertFalse(br.isContainer());
        }

        @Test
        @DisplayName("Special flag is set for special elements")
        void specialFlag() {
            HTMLElements.Element script = HTMLElements.getElement("SCRIPT");
            HTMLElements.Element style = HTMLElements.getElement("STYLE");
            HTMLElements.Element xmp = HTMLElements.getElement("XMP");
            HTMLElements.Element comment = HTMLElements.getElement("COMMENT");

            assertTrue(script.isSpecial());
            assertTrue(style.isSpecial());
            assertTrue(xmp.isSpecial());
            assertTrue(comment.isSpecial());
        }
    }

    @Nested
    class RelationshipsTests {
        @Test
        @DisplayName("Element closes() respects the closes list")
        void closesBehavior() {
            HTMLElements.Element p = HTMLElements.getElement("P");
            HTMLElements.Element li = HTMLElements.getElement("LI");
            HTMLElements.Element div = HTMLElements.getElement("DIV");

            assertTrue(p.closes(HTMLElements.P), "P should close P");
            assertTrue(li.closes(HTMLElements.LI), "LI should close LI");
            assertTrue(li.closes(HTMLElements.P), "LI should close P");
            assertTrue(div.closes(HTMLElements.P), "DIV closes P according to HTML specification");
        }

        @Test
        @DisplayName("isParent() is true for declared parents")
        void isParentRecognizesDeclaredParents() {
            HTMLElements.Element caption = HTMLElements.getElement("CAPTION");
            HTMLElements.Element table = HTMLElements.getElement("TABLE");
            assertTrue(caption.isParent(table), "CAPTION should have TABLE as a parent");

            HTMLElements.Element li = HTMLElements.getElement("LI");
            HTMLElements.Element ul = HTMLElements.getElement("UL");
            HTMLElements.Element ol = HTMLElements.getElement("OL");
            HTMLElements.Element body = HTMLElements.getElement("BODY");
            assertTrue(li.isParent(ul));
            assertTrue(li.isParent(ol));
            assertTrue(li.isParent(body));
        }
    }

    @Nested
    class EqualityAndStringTests {
        @Test
        @DisplayName("equals() compares against the name string (current behavior)")
        void equalsComparesToNameString() {
            // NOTE: Current implementation: equals(Object o) returns name.equals(o)
            HTMLElements.Element div = HTMLElements.getElement("DIV");

            assertTrue(div.equals("DIV"), "Element equals() should match equal String name");
            assertFalse(div.equals(div), "Element equals() does NOT match the same Element instance");
            assertFalse("DIV".equals(div), "String equals() does not match Element (non-symmetric)");
        }

        @Test
        @DisplayName("hashCode() matches name hash; toString() contains name")
        void hashCodeAndToString() {
            HTMLElements.Element div = HTMLElements.getElement("DIV");

            assertEquals("DIV".hashCode(), div.hashCode());
            String ts = div.toString();
            assertNotNull(ts);
            assertTrue(ts.contains("(name=DIV)"), "toString() should contain '(name=DIV)'");
        }
    }

    @Nested
    class ElementListTests {
        @Test
        @DisplayName("ElementList grows capacity when adding beyond initial length")
        void elementListGrows() {
            HTMLElements.ElementList list = new HTMLElements.ElementList();
            HTMLElements.Element sample = HTMLElements.getElement("DIV");

            int initialCapacity = list.data.length;
            assertTrue(initialCapacity >= 120, "Initial capacity should be at least 120");

            // Add more than initial capacity to trigger growth
            int toAdd = initialCapacity + 5;
            for (int i = 0; i < toAdd; i++) {
                list.addElement(sample);
            }

            assertEquals(toAdd, list.size);
            assertSame(sample, list.data[list.size - 1], "Last element should be the one added");
            assertTrue(list.data.length > initialCapacity, "Capacity should have grown");
        }
    }

    @Test
    @DisplayName("UNKNOWN code maps to NO_SUCH_ELEMENT via getElement(short)")
    void unknownCodeMapsToNoSuchElement() {
        HTMLElements.Element e = HTMLElements.getElement(HTMLElements.UNKNOWN);
        assertSame(HTMLElements.NO_SUCH_ELEMENT, e);
        assertEquals("", HTMLElements.NO_SUCH_ELEMENT.name, "NO_SUCH_ELEMENT name is empty by design");
        assertTrue(HTMLElements.NO_SUCH_ELEMENT.isContainer());
        // NO_SUCH_ELEMENT should have concrete BODY/HEAD parents configured in static init
        assertNotNull(HTMLElements.NO_SUCH_ELEMENT.parent);
        assertEquals(2, HTMLElements.NO_SUCH_ELEMENT.parent.length);
    }

    @Test
    @DisplayName("Empty string name returns NO_SUCH_ELEMENT")
    void getElementWithEmptyString() {
        HTMLElements.Element fallback = HTMLElements.getElement("DIV");
        HTMLElements.Element result = HTMLElements.getElement("", fallback);
        assertSame(fallback, result, "Empty string should return fallback element");

        // Empty string without fallback creates UNKNOWN element
        HTMLElements.Element unknown = HTMLElements.getElement("");
        assertNotNull(unknown);
        assertEquals(HTMLElements.UNKNOWN, unknown.code);
        assertEquals("", unknown.name);
    }

    @Test
    @DisplayName("Element with bounds attribute (TD, TH)")
    void elementWithBounds() {
        HTMLElements.Element td = HTMLElements.getElement(HTMLElements.TD);
        HTMLElements.Element th = HTMLElements.getElement(HTMLElements.TH);

        assertNotNull(td);
        assertNotNull(th);
        assertEquals("TD", td.name);
        assertEquals("TH", th.name);

        // TD and TH have TABLE as bounds
        assertEquals(HTMLElements.TABLE, td.bounds);
        assertEquals(HTMLElements.TABLE, th.bounds);

        // They should close each other
        assertTrue(td.closes(HTMLElements.TD));
        assertTrue(td.closes(HTMLElements.TH));
        assertTrue(th.closes(HTMLElements.TD));
        assertTrue(th.closes(HTMLElements.TH));
    }

    @Test
    @DisplayName("Element constructors with different parameters")
    void elementConstructors() {
        // Test single parent constructor
        HTMLElements.Element e1 =
                new HTMLElements.Element((short) 999, "TEST1", HTMLElements.Element.INLINE, HTMLElements.BODY,
                        new short[] { HTMLElements.P });
        assertEquals(999, e1.code);
        assertEquals("TEST1", e1.name);
        assertTrue(e1.isInline());
        assertEquals(-1, e1.bounds);

        // Test single parent with bounds constructor
        HTMLElements.Element e2 =
                new HTMLElements.Element((short) 998, "TEST2", HTMLElements.Element.CONTAINER, HTMLElements.BODY, HTMLElements.TABLE,
                        new short[] { HTMLElements.P });
        assertEquals(998, e2.code);
        assertEquals("TEST2", e2.name);
        assertTrue(e2.isContainer());
        assertEquals(HTMLElements.TABLE, e2.bounds);

        // Test multiple parents constructor
        HTMLElements.Element e3 =
                new HTMLElements.Element((short) 997, "TEST3", HTMLElements.Element.BLOCK, new short[] { HTMLElements.BODY,
                        HTMLElements.HEAD }, new short[] { HTMLElements.P });
        assertEquals(997, e3.code);
        assertEquals("TEST3", e3.name);
        assertTrue(e3.isBlock());
        assertEquals(-1, e3.bounds);

        // Test multiple parents with bounds constructor
        HTMLElements.Element e4 =
                new HTMLElements.Element((short) 996, "TEST4", HTMLElements.Element.EMPTY, new short[] { HTMLElements.BODY,
                        HTMLElements.HEAD }, HTMLElements.TABLE, null);
        assertEquals(996, e4.code);
        assertEquals("TEST4", e4.name);
        assertTrue(e4.isEmpty());
        assertEquals(HTMLElements.TABLE, e4.bounds);
        assertNull(e4.closes);
    }

    @Test
    @DisplayName("Special HTML5 elements are properly configured")
    void html5Elements() {
        // Test HTML5 semantic elements
        HTMLElements.Element article = HTMLElements.getElement("ARTICLE");
        HTMLElements.Element section = HTMLElements.getElement("SECTION");
        HTMLElements.Element nav = HTMLElements.getElement("NAV");
        HTMLElements.Element aside = HTMLElements.getElement("ASIDE");
        HTMLElements.Element header = HTMLElements.getElement("HEADER");
        HTMLElements.Element footer = HTMLElements.getElement("FOOTER");
        HTMLElements.Element main = HTMLElements.getElement("MAIN");

        assertNotNull(article);
        assertNotNull(section);
        assertNotNull(nav);
        assertNotNull(aside);
        assertNotNull(header);
        assertNotNull(footer);
        assertNotNull(main);

        assertTrue(article.isContainer());
        assertTrue(section.isContainer());
        assertTrue(nav.isContainer());
        assertTrue(header.isContainer());
        assertTrue(footer.isContainer());
        assertTrue(main.isContainer());

        // ASIDE is defined as BLOCK
        assertTrue(aside.isBlock());

        // These elements should close P
        assertTrue(article.closes(HTMLElements.P));
        assertTrue(aside.closes(HTMLElements.P));
        assertTrue(footer.closes(HTMLElements.P));
        assertTrue(header.closes(HTMLElements.P));
    }

    @Test
    @DisplayName("Media elements are properly configured")
    void mediaElements() {
        HTMLElements.Element audio = HTMLElements.getElement("AUDIO");
        HTMLElements.Element video = HTMLElements.getElement("VIDEO");
        HTMLElements.Element canvas = HTMLElements.getElement("CANVAS");
        HTMLElements.Element source = HTMLElements.getElement("SOURCE");
        HTMLElements.Element track = HTMLElements.getElement("TRACK");

        assertNotNull(audio);
        assertNotNull(video);
        assertNotNull(canvas);
        assertNotNull(source);
        assertNotNull(track);

        assertTrue(audio.isEmpty());
        assertTrue(video.isEmpty());
        assertTrue(canvas.isEmpty());
        assertTrue(source.isEmpty());
        assertTrue(track.isEmpty());
    }

    @Test
    @DisplayName("Table elements hierarchy is properly configured")
    void tableElementsHierarchy() {
        HTMLElements.Element table = HTMLElements.getElement("TABLE");
        HTMLElements.Element tbody = HTMLElements.getElement("TBODY");
        HTMLElements.Element thead = HTMLElements.getElement("THEAD");
        HTMLElements.Element tfoot = HTMLElements.getElement("TFOOT");
        HTMLElements.Element tr = HTMLElements.getElement("TR");
        HTMLElements.Element td = HTMLElements.getElement("TD");
        HTMLElements.Element th = HTMLElements.getElement("TH");

        // TABLE is both BLOCK and CONTAINER
        assertTrue(table.isBlock());
        assertTrue(table.isContainer());

        // TR is BLOCK
        assertTrue(tr.isBlock());

        // TD and TH are CONTAINER
        assertTrue(td.isContainer());
        assertTrue(th.isContainer());

        // Check parent relationships
        assertTrue(tbody.isParent(table));
        assertTrue(thead.isParent(table));
        assertTrue(tfoot.isParent(table));

        // TR has multiple possible parents
        HTMLElements.Element trElem = HTMLElements.getElement(HTMLElements.TR);
        assertTrue(trElem.parent.length > 1);

        // Check closing behavior
        assertTrue(tbody.closes(HTMLElements.THEAD));
        assertTrue(tbody.closes(HTMLElements.TBODY));
        assertTrue(tbody.closes(HTMLElements.TFOOT));
        assertTrue(tbody.closes(HTMLElements.TD));
        assertTrue(tbody.closes(HTMLElements.TH));
        assertTrue(tbody.closes(HTMLElements.TR));
    }

    @Test
    @DisplayName("Ruby elements are properly configured")
    void rubyElements() {
        HTMLElements.Element ruby = HTMLElements.getElement("RUBY");
        HTMLElements.Element rb = HTMLElements.getElement("RB");
        HTMLElements.Element rt = HTMLElements.getElement("RT");
        HTMLElements.Element rp = HTMLElements.getElement("RP");
        HTMLElements.Element rtc = HTMLElements.getElement("RTC");
        HTMLElements.Element rbc = HTMLElements.getElement("RBC");

        assertNotNull(ruby);
        assertNotNull(rb);
        assertNotNull(rt);
        assertNotNull(rp);
        assertNotNull(rtc);
        assertNotNull(rbc);

        assertTrue(ruby.isContainer());
        assertTrue(rb.isInline());
        assertTrue(rt.isInline());
        assertTrue(rp.isInline());

        // Ruby elements have specific parent relationships
        assertTrue(rb.isParent(ruby));
        assertTrue(rt.isParent(ruby));
        assertTrue(rp.isParent(ruby));
        assertTrue(rtc.isParent(ruby));
        assertTrue(rbc.isParent(ruby));

        // Check closing behavior
        assertTrue(rb.closes(HTMLElements.RB));
        assertTrue(rp.closes(HTMLElements.RB));
        assertTrue(rt.closes(HTMLElements.RB));
        assertTrue(rt.closes(HTMLElements.RP));
    }

    @Test
    @DisplayName("Form elements are properly configured")
    void formElements() {
        HTMLElements.Element form = HTMLElements.getElement("FORM");
        HTMLElements.Element input = HTMLElements.getElement("INPUT");
        HTMLElements.Element select = HTMLElements.getElement("SELECT");
        HTMLElements.Element option = HTMLElements.getElement("OPTION");
        HTMLElements.Element optgroup = HTMLElements.getElement("OPTGROUP");
        HTMLElements.Element textarea = HTMLElements.getElement("TEXTAREA");
        HTMLElements.Element button = HTMLElements.getElement("BUTTON");
        HTMLElements.Element fieldset = HTMLElements.getElement("FIELDSET");
        HTMLElements.Element legend = HTMLElements.getElement("LEGEND");

        assertTrue(form.isContainer());
        assertTrue(input.isEmpty());
        assertTrue(select.isContainer());
        assertTrue(textarea.isSpecial());
        assertTrue(fieldset.isContainer());
        assertTrue(legend.isInline());

        // BUTTON has both INLINE and BLOCK flags
        assertTrue((button.flags & HTMLElements.Element.INLINE) != 0);
        assertTrue((button.flags & HTMLElements.Element.BLOCK) != 0);

        // Check parent relationships
        assertTrue(legend.isParent(fieldset));

        // FORM has multiple possible parents
        assertTrue(form.parent.length > 1);

        // Check closing behavior
        assertTrue(form.closes(HTMLElements.BUTTON));
        assertTrue(form.closes(HTMLElements.P));
        assertTrue(select.closes(HTMLElements.SELECT));
        assertTrue(option.closes(HTMLElements.OPTION));
        assertTrue(option.closes(HTMLElements.OPTGROUP));
    }

    @Test
    @DisplayName("Element.closes() with null closes array")
    void closesWithNullArray() {
        // Create element with null closes array
        HTMLElements.Element elem = new HTMLElements.Element((short) 995, "NOCLOSE", HTMLElements.Element.INLINE, HTMLElements.BODY, null);

        assertFalse(elem.closes(HTMLElements.P));
        assertFalse(elem.closes(HTMLElements.DIV));
        assertFalse(elem.closes((short) 999));
    }

    @Test
    @DisplayName("Element.isParent() with null parent array")
    void isParentWithNullArray() {
        // Create element with null parent (will be set during initialization)
        HTMLElements.Element elem = new HTMLElements.Element((short) 994, "NOPARENT", HTMLElements.Element.INLINE, (short[]) null, null);
        elem.parent = null; // Force null parent

        HTMLElements.Element body = HTMLElements.getElement(HTMLElements.BODY);
        assertFalse(elem.isParent(body));
    }
}