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

    /**
     * List of active formatting elements for the Adoption Agency Algorithm.
     * This list tracks formatting elements (a, b, i, strong, etc.) that are currently "open"
     * and may need to be reconstructed when closing tags are encountered out of order.
     */
    protected final java.util.LinkedList<String> activeFormattingElements = new java.util.LinkedList<>();

    /**
     * Marker object used in the active formatting elements list.
     * Markers are used to separate different contexts (e.g., when entering tables or lists).
     */
    protected static final String MARKER = new String("MARKER");

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

    /** Generic container elements that can contain almost any child element. */
    protected static final Set<String> GENERIC_CONTAINERS = new HashSet<>();
    static {
        // Basic containers
        GENERIC_CONTAINERS.add("DIV");
        GENERIC_CONTAINERS.add("SPAN");
        GENERIC_CONTAINERS.add("P");
        GENERIC_CONTAINERS.add("BLOCKQUOTE");
        GENERIC_CONTAINERS.add("ADDRESS");
        GENERIC_CONTAINERS.add("PRE");

        // HTML5 semantic elements
        GENERIC_CONTAINERS.add("ARTICLE");
        GENERIC_CONTAINERS.add("SECTION");
        GENERIC_CONTAINERS.add("NAV");
        GENERIC_CONTAINERS.add("HEADER");
        GENERIC_CONTAINERS.add("FOOTER");
        GENERIC_CONTAINERS.add("ASIDE");
        GENERIC_CONTAINERS.add("MAIN");
        GENERIC_CONTAINERS.add("SEARCH");

        // List elements
        GENERIC_CONTAINERS.add("LI");
        GENERIC_CONTAINERS.add("DD");
        GENERIC_CONTAINERS.add("DT");
        GENERIC_CONTAINERS.add("UL");
        GENERIC_CONTAINERS.add("OL");
        GENERIC_CONTAINERS.add("DL");
        GENERIC_CONTAINERS.add("MENU");

        // Table elements
        GENERIC_CONTAINERS.add("TABLE");
        GENERIC_CONTAINERS.add("TBODY");
        GENERIC_CONTAINERS.add("THEAD");
        GENERIC_CONTAINERS.add("TFOOT");
        GENERIC_CONTAINERS.add("TR");
        GENERIC_CONTAINERS.add("TD");
        GENERIC_CONTAINERS.add("TH");
        GENERIC_CONTAINERS.add("CAPTION");
        GENERIC_CONTAINERS.add("COLGROUP");

        // Form elements
        GENERIC_CONTAINERS.add("FORM");
        GENERIC_CONTAINERS.add("FIELDSET");
        GENERIC_CONTAINERS.add("LABEL");
        GENERIC_CONTAINERS.add("BUTTON");
        GENERIC_CONTAINERS.add("LEGEND");

        // Other semantic containers
        GENERIC_CONTAINERS.add("FIGURE");
        GENERIC_CONTAINERS.add("FIGCAPTION");
        GENERIC_CONTAINERS.add("DETAILS");
        GENERIC_CONTAINERS.add("SUMMARY");
        GENERIC_CONTAINERS.add("DIALOG");
        GENERIC_CONTAINERS.add("HGROUP");

        // Web Components
        GENERIC_CONTAINERS.add("SLOT");

        // Legacy containers
        GENERIC_CONTAINERS.add("CENTER");
        GENERIC_CONTAINERS.add("MARQUEE");

        // Special containers
        GENERIC_CONTAINERS.add("NOSCRIPT");
        GENERIC_CONTAINERS.add("A"); // HTML5 allows block elements in <a>
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
        activeFormattingElements.clear();
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

        if (qName == null || qName.isEmpty()) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.warning("Ignoring element with null or empty qName");
            }
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

        // Start the element
        handler.startElement(uri, localName, qName, atts);

        // Track non-void elements
        if (!VOID_ELEMENTS.contains(tagName)) {
            elementStack.push(tagName);
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Pushed element onto stack: " + tagName + " (stack depth: " + elementStack.size() + ")");
            }

            // Track formatting elements for AAA
            if (org.codelibs.nekohtml.HTMLElements.isFormattingElement(tagName)) {
                addFormattingElement(tagName);
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Added formatting element: " + tagName);
                }
            }
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        final ContentHandler handler = getContentHandler();
        if (handler == null) {
            return;
        }

        if (qName == null || qName.isEmpty()) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.warning("Ignoring end element with null or empty qName");
            }
            return;
        }

        final String tagName = qName.toUpperCase();

        // Check if this is a formatting element - if so, run AAA
        final boolean isFormatting = org.codelibs.nekohtml.HTMLElements.isFormattingElement(tagName);
        if (isFormatting && findFormattingElement(tagName) >= 0) {
            // Run Adoption Agency Algorithm
            runAdoptionAgencyAlgorithm(tagName, uri, localName, qName);
            return;
        }

        // Find and close the element (standard logic)
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
                    // Remove from formatting elements if present
                    removeFormattingElement(elem);
                    handler.endElement("", elem, elem);
                }
                // Now close the target element
                elementStack.pop();
                removeFormattingElement(tagName);
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

    //
    // Active Formatting Elements Management (for Adoption Agency Algorithm)
    //

    /**
     * Adds a formatting element to the active formatting elements list.
     * This is part of the Adoption Agency Algorithm implementation.
     *
     * @param tagName The element tag name
     */
    protected void addFormattingElement(final String tagName) {
        // Remove any duplicate entry of the same element (only one instance allowed)
        activeFormattingElements.remove(tagName);
        // Add to the end of the list
        activeFormattingElements.add(tagName);
    }

    /**
     * Removes a formatting element from the active formatting elements list.
     *
     * @param tagName The element tag name
     * @return true if the element was found and removed
     */
    protected boolean removeFormattingElement(final String tagName) {
        return activeFormattingElements.remove(tagName);
    }

    /**
     * Finds the index of a formatting element in the active formatting elements list.
     * Searches from the end of the list (most recent elements first).
     *
     * @param tagName The element tag name
     * @return The index of the element, or -1 if not found
     */
    protected int findFormattingElement(final String tagName) {
        for (int i = activeFormattingElements.size() - 1; i >= 0; i--) {
            final String element = activeFormattingElements.get(i);
            if (tagName.equals(element)) {
                return i;
            }
            // Stop at marker
            if (element == MARKER) {
                break;
            }
        }
        return -1;
    }

    /**
     * Clears all active formatting elements up to and including the last marker.
     * This is called when exiting certain contexts (e.g., tables, select elements).
     */
    protected void clearFormattingElementsToLastMarker() {
        while (!activeFormattingElements.isEmpty()) {
            final String element = activeFormattingElements.removeLast();
            if (element == MARKER) {
                break;
            }
        }
    }

    /**
     * Pushes a marker onto the active formatting elements list.
     * Markers are used to separate different parsing contexts.
     */
    protected void pushFormattingMarker() {
        activeFormattingElements.add(MARKER);
    }

    //
    // Adoption Agency Algorithm (AAA)
    //

    /**
     * Checks if an element is a "special" category element (block-level elements).
     * These elements are used to find the "furthest block" in the AAA.
     *
     * @param tagName The element tag name
     * @return true if the element is special/block-level
     */
    protected boolean isSpecialElement(final String tagName) {
        // Block-level and special elements according to HTML Living Standard
        return "ADDRESS".equals(tagName) || "ARTICLE".equals(tagName) || "ASIDE".equals(tagName) || "BLOCKQUOTE".equals(tagName)
                || "DETAILS".equals(tagName) || "DIALOG".equals(tagName) || "DIV".equals(tagName) || "DL".equals(tagName)
                || "FIELDSET".equals(tagName) || "FIGCAPTION".equals(tagName) || "FIGURE".equals(tagName) || "FOOTER".equals(tagName)
                || "FORM".equals(tagName) || "H1".equals(tagName) || "H2".equals(tagName) || "H3".equals(tagName) || "H4".equals(tagName)
                || "H5".equals(tagName) || "H6".equals(tagName) || "HEADER".equals(tagName) || "HGROUP".equals(tagName)
                || "HR".equals(tagName) || "LI".equals(tagName) || "MAIN".equals(tagName) || "NAV".equals(tagName) || "OL".equals(tagName)
                || "P".equals(tagName) || "PRE".equals(tagName) || "SEARCH".equals(tagName) || "SECTION".equals(tagName)
                || "TABLE".equals(tagName) || "UL".equals(tagName);
    }

    /**
     * Finds the "furthest block" for the Adoption Agency Algorithm.
     * The furthest block is the first special/block element after the formatting element in the stack.
     *
     * @param formattingIndex The index of the formatting element in the element stack
     * @return The index of the furthest block, or -1 if none found
     */
    protected int findFurthestBlock(final int formattingIndex) {
        for (int i = formattingIndex + 1; i < elementStack.size(); i++) {
            if (isSpecialElement(elementStack.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Runs the Adoption Agency Algorithm for formatting elements.
     * This is a simplified implementation that handles the most common cases.
     *
     * @param tagName   The formatting element tag name
     * @param uri       The namespace URI
     * @param localName The local name
     * @param qName     The qualified name
     * @throws SAXException If an error occurs
     */
    protected void runAdoptionAgencyAlgorithm(final String tagName, final String uri, final String localName, final String qName)
            throws SAXException {
        final ContentHandler handler = getContentHandler();
        if (handler == null) {
            return;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Running Adoption Agency Algorithm for: " + tagName);
        }

        // Outer loop - run up to 8 times
        for (int outerLoop = 0; outerLoop < 8; outerLoop++) {
            // Step 1: Find the formatting element in active formatting elements
            final int formattingElemIndexInList = findFormattingElement(tagName);
            if (formattingElemIndexInList < 0) {
                // Not in active formatting elements - use standard close logic
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("AAA: Formatting element not in active list, using standard close");
                }
                break;
            }

            // Step 2: Find the formatting element in the element stack
            final int formattingElemIndexInStack = elementStack.lastIndexOf(tagName);
            if (formattingElemIndexInStack < 0) {
                // Not in stack - remove from active list and return
                activeFormattingElements.remove(formattingElemIndexInList);
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("AAA: Formatting element not in stack, removed from active list");
                }
                return;
            }

            // Step 3: Find the furthest block
            final int furthestBlockIndex = findFurthestBlock(formattingElemIndexInStack);

            if (furthestBlockIndex < 0) {
                // No furthest block - close elements and reopen formatting elements that were inside
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("AAA: No furthest block, closing and reopening formatting elements");
                }

                // Collect formatting elements that need to be reopened
                final java.util.List<String> elementsToReopen = new java.util.ArrayList<>();
                for (int i = formattingElemIndexInStack + 1; i < elementStack.size(); i++) {
                    final String elem = elementStack.get(i);
                    if (org.codelibs.nekohtml.HTMLElements.isFormattingElement(elem)) {
                        elementsToReopen.add(elem);
                    }
                }

                // Close all elements from top down to and including the formatting element
                while (elementStack.size() > formattingElemIndexInStack) {
                    final String elem = elementStack.pop();
                    removeFormattingElement(elem);
                    handler.endElement("", elem, elem);
                    if (elem.equals(tagName)) {
                        break;
                    }
                }

                // Reopen the formatting elements that were inside
                for (final String elem : elementsToReopen) {
                    elementStack.push(elem);
                    addFormattingElement(elem);
                    handler.startElement("", elem.toLowerCase(), elem, new org.xml.sax.helpers.AttributesImpl());
                }

                return;
            }

            // Step 4: Complex case with furthest block
            // This is a simplified version - full AAA is much more complex
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("AAA: Furthest block found at index " + furthestBlockIndex + ", reconstructing");
            }

            // For now, we'll do a simplified reconstruction:
            // Close elements between formatting element and furthest block,
            // then reopen the formatting element after the furthest block

            // Close elements from formatting element up to (but not including) furthest block
            java.util.List<String> elementsToReopen = new java.util.ArrayList<>();
            for (int i = formattingElemIndexInStack + 1; i < furthestBlockIndex; i++) {
                elementsToReopen.add(elementStack.get(i));
            }

            // Close formatting element - use pop/push operations instead of remove(index) for better performance
            // Save elements after the formatting element
            final java.util.List<String> elementsAfter = new java.util.ArrayList<>();
            for (int i = formattingElemIndexInStack + 1; i < elementStack.size(); i++) {
                elementsAfter.add(elementStack.get(i));
            }
            // Pop all elements down to and including the formatting element
            while (elementStack.size() > formattingElemIndexInStack) {
                elementStack.pop();
            }
            // Restore elements that were after
            for (final String elem : elementsAfter) {
                elementStack.push(elem);
            }
            removeFormattingElement(tagName);
            handler.endElement(uri, localName, qName);

            // Reopen formatting element after furthest block (simplified)
            // In full AAA, this would involve complex node manipulation
            // For our purposes, the formatting has been properly closed

            if (logger.isLoggable(Level.FINER)) {
                logger.finer("AAA: Completed reconstruction for " + tagName);
            }

            return;
        }

        // If we exit the loop without returning, fall back to standard close
        final int index = elementStack.lastIndexOf(tagName);
        if (index >= 0) {
            while (elementStack.size() > index) {
                final String elem = elementStack.pop();
                removeFormattingElement(elem);
                handler.endElement("", elem, elem);
                if (elem.equals(tagName)) {
                    break;
                }
            }
        }
    }

} // class HTMLTagBalancerFilter
