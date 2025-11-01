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

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * SAX filter for HTML tag balancing.
 * Automatically closes tags and fixes HTML structure.
 *
 * @author CodeLibs Project
 */
public class HTMLTagBalancerFilter extends XMLFilterImpl implements LexicalHandler {

    /** Logger for this class. */
    private static final Logger logger = Logger.getLogger(HTMLTagBalancerFilter.class.getName());

    /** Lexical handler for DTD and CDATA events. */
    protected LexicalHandler lexicalHandler;

    /** Stack of open elements. */
    protected final Stack<String> elementStack = new Stack<>();

    /** Whether the document structure has been initialized (HTML element started). */
    protected boolean documentInitialized = false;

    /** Elements that should close HEAD when they appear. */
    protected static final Set<String> BODY_ELEMENTS = new HashSet<>();
    static {
        BODY_ELEMENTS.add("BODY");
        BODY_ELEMENTS.add("FRAMESET");
    }

    /** Elements that belong in HEAD. */
    protected static final Set<String> HEAD_ELEMENTS = new HashSet<>();
    static {
        HEAD_ELEMENTS.add("TITLE");
        HEAD_ELEMENTS.add("META");
        HEAD_ELEMENTS.add("LINK");
        HEAD_ELEMENTS.add("STYLE");
        HEAD_ELEMENTS.add("SCRIPT");
        HEAD_ELEMENTS.add("BASE");
    }

    /** Self-closing elements (void elements). */
    protected static final Set<String> VOID_ELEMENTS = new HashSet<>();
    static {
        VOID_ELEMENTS.add("AREA");
        VOID_ELEMENTS.add("BASE");
        VOID_ELEMENTS.add("BR");
        VOID_ELEMENTS.add("COL");
        VOID_ELEMENTS.add("EMBED");
        VOID_ELEMENTS.add("HR");
        VOID_ELEMENTS.add("IMG");
        VOID_ELEMENTS.add("INPUT");
        VOID_ELEMENTS.add("LINK");
        VOID_ELEMENTS.add("META");
        VOID_ELEMENTS.add("PARAM");
        VOID_ELEMENTS.add("SOURCE");
        VOID_ELEMENTS.add("TRACK");
        VOID_ELEMENTS.add("WBR");
    }

    /**
     * Default constructor.
     */
    public HTMLTagBalancerFilter() {
        this(null);
    }

    /**
     * Constructs a tag balancer filter with the specified parent reader.
     *
     * @param parent The parent XML reader
     */
    public HTMLTagBalancerFilter(final XMLReader parent) {
        super(parent);
    }

    @Override
    public void setContentHandler(final ContentHandler handler) {
        super.setContentHandler(handler);
    }

    /**
     * Sets the lexical handler.
     *
     * @param handler The lexical handler
     */
    public void setLexicalHandler(final LexicalHandler handler) {
        this.lexicalHandler = handler;
    }

    @Override
    public void setDocumentLocator(final Locator locator) {
        if (getContentHandler() != null) {
            getContentHandler().setDocumentLocator(locator);
        }
    }

    @Override
    public void startDocument() throws SAXException {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Starting document - initializing tag balancer");
        }
        elementStack.clear();
        documentInitialized = false;
        if (getContentHandler() != null) {
            getContentHandler().startDocument();
        }
    }

    @Override
    public void endDocument() throws SAXException {
        // Close any remaining open elements
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Ending document - closing " + elementStack.size() + " remaining open elements");
        }
        while (!elementStack.isEmpty()) {
            final String element = elementStack.pop();
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Auto-closing unclosed element at document end: " + element);
            }
            if (getContentHandler() != null) {
                getContentHandler().endElement("", element, element);
            }
        }
        if (getContentHandler() != null) {
            getContentHandler().endDocument();
        }
    }

    @Override
    public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
        if (getContentHandler() != null) {
            getContentHandler().startPrefixMapping(prefix, uri);
        }
    }

    @Override
    public void endPrefixMapping(final String prefix) throws SAXException {
        if (getContentHandler() != null) {
            getContentHandler().endPrefixMapping(prefix);
        }
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes atts) throws SAXException {
        final ContentHandler handler = getContentHandler();
        if (handler == null) {
            return;
        }

        final String tagName = qName.toUpperCase();

        // If this is an HTML element, mark document as initialized but don't auto-add
        if ("HTML".equals(tagName)) {
            documentInitialized = true;
        } else {
            ensureDocumentInitialized();
        }

        // If starting BODY or FRAMESET, close HEAD if it's open
        if (BODY_ELEMENTS.contains(tagName)) {
            closeElement("HEAD");
            closeElement("TITLE"); // Close any unclosed title
        }

        // If starting a HEAD element and we're in BODY, don't close HEAD
        // (this is malformed HTML, but we handle it gracefully)

        // Start the element
        handler.startElement(uri, localName, qName, atts);

        // Track non-void elements
        if (!VOID_ELEMENTS.contains(tagName)) {
            elementStack.push(tagName);
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Pushed element onto stack: " + tagName + " (stack depth: " + elementStack.size() + ")");
            }
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        final ContentHandler handler = getContentHandler();
        if (handler == null) {
            return;
        }

        final String tagName = qName.toUpperCase();

        // Find and close the element
        if (!elementStack.isEmpty()) {
            // If the element is on the stack, close it and everything above it
            final int index = elementStack.lastIndexOf(tagName);
            if (index >= 0) {
                // Close all elements above this one first (auto-close)
                final int elementsToClose = elementStack.size() - index - 1;
                if (elementsToClose > 0 && logger.isLoggable(Level.FINER)) {
                    logger.finer("Auto-closing " + elementsToClose + " elements above " + tagName);
                }
                while (elementStack.size() > index + 1) {
                    final String elem = elementStack.pop();
                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer("Auto-closing element: " + elem);
                    }
                    handler.endElement("", elem, elem);
                }
                // Now close the target element
                elementStack.pop();
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Popped element from stack: " + tagName + " (stack depth: " + elementStack.size() + ")");
                }
                handler.endElement(uri, localName, qName);
            } else {
                // Element not on stack - might be a void element or already closed
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("End element not on stack (void or already closed): " + tagName);
                }
                // Just pass through the end tag
                handler.endElement(uri, localName, qName);
            }
        } else {
            // Stack is empty, just pass through
            handler.endElement(uri, localName, qName);
        }
    }

    /**
     * Closes an element if it's currently open.
     *
     * @param tagName The tag name to close (uppercase)
     * @throws SAXException If an error occurs
     */
    protected void closeElement(final String tagName) throws SAXException {
        final ContentHandler handler = getContentHandler();
        if (handler == null || elementStack.isEmpty()) {
            return;
        }

        final int index = elementStack.lastIndexOf(tagName);
        if (index >= 0) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Closing element and " + (elementStack.size() - index - 1) + " elements above it: " + tagName);
            }
            // Close all elements from the top down to and including the target
            while (elementStack.size() > index) {
                final String elem = elementStack.pop();
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Auto-closing element: " + elem);
                }
                handler.endElement("", elem, elem);
            }
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        ensureDocumentInitialized();
        if (getContentHandler() != null) {
            getContentHandler().characters(ch, start, length);
        }
    }

    @Override
    public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException {
        if (getContentHandler() != null) {
            getContentHandler().ignorableWhitespace(ch, start, length);
        }
    }

    @Override
    public void processingInstruction(final String target, final String data) throws SAXException {
        if (getContentHandler() != null) {
            getContentHandler().processingInstruction(target, data);
        }
    }

    @Override
    public void skippedEntity(final String name) throws SAXException {
        if (getContentHandler() != null) {
            getContentHandler().skippedEntity(name);
        }
    }

    // LexicalHandler methods

    @Override
    public void startDTD(final String name, final String publicId, final String systemId) throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.startDTD(name, publicId, systemId);
        }
    }

    @Override
    public void endDTD() throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.endDTD();
        }
    }

    @Override
    public void startEntity(final String name) throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.startEntity(name);
        }
    }

    @Override
    public void endEntity(final String name) throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.endEntity(name);
        }
    }

    @Override
    public void startCDATA() throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.startCDATA();
        }
    }

    @Override
    public void endCDATA() throws SAXException {
        if (lexicalHandler != null) {
            lexicalHandler.endCDATA();
        }
    }

    @Override
    public void comment(final char[] ch, final int start, final int length) throws SAXException {
        ensureDocumentInitialized();
        if (lexicalHandler != null) {
            lexicalHandler.comment(ch, start, length);
        }
    }

    /**
     * Ensures the HTML document structure is initialized.
     * Automatically adds an HTML root element if none has been started yet.
     *
     * @throws SAXException If an error occurs
     */
    protected void ensureDocumentInitialized() throws SAXException {
        if (!documentInitialized) {
            documentInitialized = true;
            final ContentHandler handler = getContentHandler();
            if (handler != null) {
                // Auto-add HTML root element
                handler.startElement("", "HTML", "HTML", new org.xml.sax.helpers.AttributesImpl());
                elementStack.push("HTML");
            }
        }
    }

} // class HTMLTagBalancerFilter
