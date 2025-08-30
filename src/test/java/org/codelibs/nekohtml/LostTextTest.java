package org.codelibs.nekohtml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for LostText.
 *
 * Focus:
 * - Leading whitespace-only text is ignored initially.
 * - Non-whitespace text is copied (including offset/length).
 * - Subsequent whitespace is preserved once non-whitespace has been added.
 * - Augmentations are copied into HTMLAugmentations (deep copy for LocationItem).
 * - Null augmentations are passed through as null.
 * - refeed clears internal state.
 */
@ExtendWith(MockitoExtension.class)
class LostTextTest {

    private static XMLString xmlStringFrom(String s) {
        char[] chars = s.toCharArray();
        return new XMLString(chars, 0, chars.length);
    }

    private static XMLString xmlStringFromWithOffset(String full, int offset, int length) {
        char[] chars = full.toCharArray();
        return new XMLString(chars, offset, length);
    }

    @Test
    void isEmpty_initiallyTrue_andIgnoresLeadingWhitespace() {
        LostText lost = new LostText();
        assertTrue(lost.isEmpty(), "New LostText should be empty");

        // Add leading whitespace-only -> ignored
        XMLString ws = xmlStringFrom(" \t \n  ");
        lost.add(ws, null);
        assertTrue(lost.isEmpty(), "Leading whitespace-only should be ignored");

        // Refeed should not call characters
        XMLDocumentHandler handler = mock(XMLDocumentHandler.class);
        lost.refeed(handler);
        verify(handler, never()).characters(any(XMLString.class), any());
        assertTrue(lost.isEmpty(), "Should still be empty after refeed");
    }

    @Test
    void add_nonWhitespace_thenWhitespace_preservedAndCopied() {
        LostText lost = new LostText();

        // Build augmentation with a string and a LocationItem to validate deep copy semantics
        HTMLAugmentations inputAugs = new HTMLAugmentations();
        inputAugs.putItem("k1", "v1");
        HTMLScanner.LocationItem loc = new HTMLScanner.LocationItem();
        loc.setValues(1, 2, 3, 4, 5, 6);
        inputAugs.putItem("loc", loc);

        // Add non-whitespace using offset/length to ensure LostText copies the exact slice
        XMLString textSlice = xmlStringFromWithOffset("xxHELLOyy", 2, 5); // "HELLO"
        lost.add(textSlice, inputAugs);

        // Subsequent whitespace-only should be preserved now
        XMLString trailingWs = xmlStringFrom("  ");
        lost.add(trailingWs, inputAugs);

        assertFalse(lost.isEmpty(), "LostText should contain entries after non-whitespace add");

        XMLDocumentHandler handler = mock(XMLDocumentHandler.class);

        // Refeed and capture arguments
        lost.refeed(handler);

        ArgumentCaptor<XMLString> textCaptor = ArgumentCaptor.forClass(XMLString.class);
        ArgumentCaptor<Augmentations> augsCaptor = ArgumentCaptor.forClass(Augmentations.class);
        verify(handler, times(2)).characters(textCaptor.capture(), augsCaptor.capture());

        List<XMLString> texts = textCaptor.getAllValues();
        List<Augmentations> augs = augsCaptor.getAllValues();

        // First call: "HELLO"
        XMLString first = texts.get(0);
        assertEquals("HELLO", new String(first.ch, first.offset, first.length), "Should refeed exact copied slice");

        // Second call: "  "
        XMLString second = texts.get(1);
        assertEquals("  ", new String(second.ch, second.offset, second.length), "Trailing whitespace should be preserved");

        // Augmentations should be HTMLAugmentations copies (not the same instance), with same keys/values
        Augmentations firstAugs = augs.get(0);
        assertNotNull(firstAugs, "Augmentations should not be null when input was provided");
        assertTrue(firstAugs instanceof HTMLAugmentations, "Augmentations should be copied to HTMLAugmentations");
        assertNotSame(inputAugs, firstAugs, "Augmentations instance should be a copy");

        // Validate string item copied
        assertEquals("v1", firstAugs.getItem("k1"), "String augmentation value should be copied");

        // Validate LocationItem was deep-copied
        Object copiedLocObj = firstAugs.getItem("loc");
        assertNotNull(copiedLocObj, "LocationItem should be present in copied augmentations");
        assertTrue(copiedLocObj instanceof HTMLScanner.LocationItem, "LocationItem type should be preserved");

        HTMLScanner.LocationItem copiedLoc = (HTMLScanner.LocationItem) copiedLocObj;
        // The instance should be different from original
        assertNotSame(loc, copiedLoc, "LocationItem should be deep-copied");
        // Values should match
        assertEquals(1, copiedLoc.getBeginLineNumber());
        assertEquals(2, copiedLoc.getBeginColumnNumber());
        assertEquals(3, copiedLoc.getBeginCharacterOffset());
        assertEquals(4, copiedLoc.getEndLineNumber());
        assertEquals(5, copiedLoc.getEndColumnNumber());
        assertEquals(6, copiedLoc.getEndCharacterOffset());

        // After refeed entries should be cleared
        reset(handler);
        lost.refeed(handler);
        verify(handler, never()).characters(any(XMLString.class), any());
        assertTrue(lost.isEmpty(), "Entries should be cleared after refeed");
    }

    @Test
    void add_withNullAugmentations_passesNullOnRefeed() {
        LostText lost = new LostText();
        XMLString text = xmlStringFrom("TEXT");
        lost.add(text, null);

        XMLDocumentHandler handler = mock(XMLDocumentHandler.class);
        ArgumentCaptor<XMLString> textCaptor = ArgumentCaptor.forClass(XMLString.class);
        ArgumentCaptor<Augmentations> augsCaptor = ArgumentCaptor.forClass(Augmentations.class);

        lost.refeed(handler);
        verify(handler, times(1)).characters(textCaptor.capture(), augsCaptor.capture());

        assertEquals("TEXT", new String(textCaptor.getValue().ch, textCaptor.getValue().offset, textCaptor.getValue().length));
        assertNull(augsCaptor.getValue(), "Augmentations should be null when null was provided");
    }

    @Test
    void add_zeroLength_thenNonEmpty_thenZeroLength_behavesPerRules() {
        LostText lost = new LostText();

        // First zero-length -> ignored
        XMLString empty = new XMLString(new char[0], 0, 0);
        lost.add(empty, null);
        assertTrue(lost.isEmpty(), "Zero-length first entry should be ignored");

        // Non-empty -> accepted
        XMLString nonEmpty = xmlStringFrom("A");
        lost.add(nonEmpty, null);

        // Zero-length after content exists -> accepted (preserved)
        XMLString empty2 = new XMLString(new char[0], 0, 0);
        lost.add(empty2, null);

        XMLDocumentHandler handler = mock(XMLDocumentHandler.class);
        ArgumentCaptor<XMLString> textCaptor = ArgumentCaptor.forClass(XMLString.class);
        lost.refeed(handler);

        verify(handler, times(2)).characters(textCaptor.capture(), any());
        List<XMLString> texts = textCaptor.getAllValues();
        assertEquals("A", new String(texts.get(0).ch, texts.get(0).offset, texts.get(0).length), "First should be the non-empty char");
        assertEquals("", new String(texts.get(1).ch, texts.get(1).offset, texts.get(1).length), "Second should preserve empty segment");
    }
}
