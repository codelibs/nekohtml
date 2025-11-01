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

import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX ContentHandler that builds a DOM tree.
 *
 * @author CodeLibs Project
 */
class SAXToDOMHandler extends DefaultHandler implements LexicalHandler {

    /** Logger for this class. */
    private static final Logger logger = Logger.getLogger(SAXToDOMHandler.class.getName());

    /** System property name for strict DOM mode. */
    private static final String PROPERTY_DOM_STRICT = "nekohtml.dom.strict";

    /** The document builder. */
    private final DocumentBuilder documentBuilder;

    /** The DOM document being built. */
    private Document document;

    /** Stack of elements being built. */
    private final Stack<Node> elementStack;

    /** Current text buffer. */
    private StringBuilder textBuffer;

    /** Skip depth for elements that couldn't be added to DOM. */
    private int skipDepth;

    /**
     * Constructor.
     *
     * @param documentBuilder The document builder
     */
    public SAXToDOMHandler(final DocumentBuilder documentBuilder) {
        this.documentBuilder = documentBuilder;
        this.elementStack = new Stack<>();
    }

    /**
     * Gets the built DOM document.
     *
     * @return The DOM document
     */
    public Document getDocument() {
        return document;
    }

    @Override
    public void startDocument() throws SAXException {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Starting DOM document building");
        }
        document = documentBuilder.newDocument();
        elementStack.clear();
        elementStack.push(document);
        textBuffer = new StringBuilder();
        skipDepth = 0;
    }

    @Override
    public void endDocument() throws SAXException {
        flushText();
        elementStack.clear();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Completed DOM document building");
        }
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
        flushText();

        // If we're already skipping elements, increment skip depth and return
        if (skipDepth > 0) {
            skipDepth++;
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Skipping element (skip depth " + skipDepth + "): " + qName);
            }
            return;
        }

        // Create element
        final Element element = document.createElement(qName);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Creating DOM element: " + qName);
        }

        // Add attributes
        for (int i = 0; i < attributes.getLength(); i++) {
            element.setAttribute(attributes.getQName(i), attributes.getValue(i));
        }

        // Add to parent
        final Node parent = elementStack.peek();
        try {
            parent.appendChild(element);
        } catch (final org.w3c.dom.DOMException e) {
            // Handle HIERARCHY_REQUEST_ERR and other DOM exceptions gracefully
            // This can occur when HTML structure violates DOM hierarchy rules
            // (e.g., block elements inside inline elements)
            if (System.getProperty(PROPERTY_DOM_STRICT) == null) {
                // In lenient mode (default), skip this element and all its children
                logger.warning("Could not append element <" + qName + "> to parent <" + parent.getNodeName() + ">: " + e.getMessage()
                        + " (Use -D" + PROPERTY_DOM_STRICT + "=true to fail on such errors)");
                // Start skipping this element and its children
                skipDepth = 1;
                return;
            } else {
                // In strict mode, propagate the exception
                throw new SAXException("DOM hierarchy violation: " + e.getMessage(), e);
            }
        }

        // Push onto stack
        elementStack.push(element);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        flushText();

        // If we're skipping elements, decrement skip depth and return
        if (skipDepth > 0) {
            skipDepth--;
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Unskipping element (skip depth " + skipDepth + "): " + qName);
            }
            return;
        }

        // Defensive check: ensure stack is not empty
        if (!elementStack.isEmpty()) {
            elementStack.pop();
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Popped element from DOM stack: " + qName + " (stack depth: " + elementStack.size() + ")");
            }
        } else {
            // Log warning when attempting to pop from empty stack
            // This indicates a potential parsing inconsistency
            logger.warning("Attempted to pop element <" + qName + "> from empty element stack. "
                    + "This may indicate mismatched start/end tags in the HTML document.");
        }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        textBuffer.append(ch, start, length);
    }

    /**
     * Flushes accumulated text to the current element.
     */
    private void flushText() {
        if (textBuffer.length() > 0) {
            String text = textBuffer.toString();
            textBuffer.setLength(0);

            // If we're skipping elements, don't add text
            if (skipDepth > 0) {
                return;
            }

            // Check if element stack is empty
            if (elementStack.isEmpty()) {
                return;
            }

            final Node parent = elementStack.peek();

            // Only add text nodes to elements, not to the document root
            if (parent.getNodeType() == Node.ELEMENT_NODE) {
                // Normalize whitespace: replace sequences of whitespace characters with a single space
                // This matches HTML rendering behavior where newlines and tabs are treated as spaces
                // TODO: text = text.replaceAll("\\s+", " ");

                // Add all text nodes, including whitespace-only ones
                // This preserves spacing between elements for text extraction
                if (!text.isEmpty()) {
                    final Text textNode = document.createTextNode(text);
                    parent.appendChild(textNode);
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.finest("Added text node (" + text.length() + " chars) to element: " + parent.getNodeName());
                    }
                }
            }
        }
    }

    // LexicalHandler implementation

    @Override
    public void startDTD(final String name, final String publicId, final String systemId) throws SAXException {
        // DTD not included in DOM tree
    }

    @Override
    public void endDTD() throws SAXException {
        // DTD not included in DOM tree
    }

    @Override
    public void startEntity(final String name) throws SAXException {
        // Entity references not included in DOM tree
    }

    @Override
    public void endEntity(final String name) throws SAXException {
        // Entity references not included in DOM tree
    }

    @Override
    public void startCDATA() throws SAXException {
        // CDATA sections handled as text
    }

    @Override
    public void endCDATA() throws SAXException {
        // CDATA sections handled as text
    }

    @Override
    public void comment(final char[] ch, final int start, final int length) throws SAXException {
        flushText();

        // If we're skipping elements, don't add comments
        if (skipDepth > 0) {
            return;
        }

        final String commentText = new String(ch, start, length);
        final Comment commentNode = document.createComment(commentText);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Creating comment node (" + commentText.length() + " chars)");
        }

        final Node parent = elementStack.peek();
        try {
            parent.appendChild(commentNode);
        } catch (final org.w3c.dom.DOMException e) {
            // Handle DOM exceptions gracefully when appending comments
            if (System.getProperty(PROPERTY_DOM_STRICT) == null) {
                // In lenient mode, skip the problematic comment
                logger.warning("Could not append comment to parent <" + parent.getNodeName() + ">: " + e.getMessage());
            } else {
                throw new SAXException("DOM hierarchy violation: " + e.getMessage(), e);
            }
        }
    }

} // class SAXToDOMHandler
