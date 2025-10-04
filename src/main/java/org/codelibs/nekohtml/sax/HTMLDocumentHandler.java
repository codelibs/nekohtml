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
package org.codelibs.nekohtml.sax;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 * Extended document handler interface for HTML parsing.
 * Combines SAX ContentHandler and LexicalHandler with additional HTML-specific methods.
 *
 * @author CodeLibs Project
 */
public interface HTMLDocumentHandler extends ContentHandler, LexicalHandler {

    /**
     * Notifies of an empty element.
     * This is a convenience method equivalent to startElement() followed immediately by endElement().
     *
     * @param qname The element's qualified name
     * @param attributes The element's attributes
     * @param augs Additional information about the element
     * @throws SAXException If an error occurs
     */
    default void emptyElement(final HTMLQName qname, final HTMLAttributesImpl attributes, final HTMLAugmentations augs) throws SAXException {
        startElement(qname.uri != null ? qname.uri : "", qname.localpart != null ? qname.localpart : qname.rawname, qname.rawname,
                attributes);
        endElement(qname.uri != null ? qname.uri : "", qname.localpart != null ? qname.localpart : qname.rawname, qname.rawname);
    }

    /**
     * Notifies of character data.
     * This is a convenience wrapper around the SAX characters() method that accepts HTMLStringBuffer.
     *
     * @param text The character data
     * @param augs Additional information
     * @throws SAXException If an error occurs
     */
    default void characters(final HTMLStringBuffer text, final HTMLAugmentations augs) throws SAXException {
        characters(text.ch, text.offset, text.length);
    }

    /**
     * Notifies of a comment.
     * This is a convenience wrapper around the LexicalHandler comment() method that accepts HTMLStringBuffer.
     *
     * @param text The comment text
     * @param augs Additional information
     * @throws SAXException If an error occurs
     */
    default void comment(final HTMLStringBuffer text, final HTMLAugmentations augs) throws SAXException {
        comment(text.ch, text.offset, text.length);
    }

} // interface HTMLDocumentHandler
