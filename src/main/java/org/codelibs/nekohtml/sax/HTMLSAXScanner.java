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

import java.io.IOException;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * SAX-based HTML scanner without Xerces dependencies.
 *
 * @author CodeLibs Project
 */
public class HTMLSAXScanner extends XMLFilterImpl {

    /** The underlying simple scanner. */
    protected final SimpleHTMLScanner fScanner;

    /** Lexical handler. */
    protected LexicalHandler fLexicalHandler;

    /**
     * Default constructor.
     */
    public HTMLSAXScanner() {
        this(null);
    }

    /**
     * Constructs a scanner with the specified parent reader.
     *
     * @param parent The parent XML reader
     */
    public HTMLSAXScanner(final XMLReader parent) {
        super(parent);

        // Create simple scanner
        fScanner = new SimpleHTMLScanner();
    }

    @Override
    public void setContentHandler(final ContentHandler handler) {
        super.setContentHandler(handler);
        fScanner.setContentHandler(handler);
    }

    /**
     * Sets the lexical handler.
     *
     * @param handler The lexical handler
     */
    public void setLexicalHandler(final LexicalHandler handler) {
        fLexicalHandler = handler;
        fScanner.setLexicalHandler(handler);
    }

    /**
     * Gets the lexical handler.
     *
     * @return The lexical handler
     */
    public LexicalHandler getLexicalHandler() {
        return fLexicalHandler;
    }

    /**
     * Sets the element name case normalization mode on the underlying scanner.
     *
     * @param c {@code "upper"}, {@code "lower"}, or {@code "match"}/{@code "default"}/{@code "no-change"}
     *          (all of the latter meaning keep the name as written)
     */
    public void setElementCase(final String c) {
        fScanner.setElementCase(c);
    }

    /**
     * Sets the attribute name case normalization mode on the underlying scanner.
     *
     * @param c {@code "upper"}, {@code "lower"}, or {@code "match"}/{@code "default"}/{@code "no-change"}
     *          (all of the latter meaning keep the name as written)
     */
    public void setAttributeCase(final String c) {
        fScanner.setAttributeCase(c);
    }

    @Override
    public void parse(final InputSource input) throws IOException, SAXException {
        fScanner.parse(input);
    }

    @Override
    public void parse(final String systemId) throws IOException, SAXException {
        parse(new InputSource(systemId));
    }

    @Override
    public boolean getFeature(final String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return fScanner.getFeature(name);
    }

    @Override
    public void setFeature(final String name, final boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        fScanner.setFeature(name, value);
    }

    @Override
    public Object getProperty(final String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        // Handle SAX2 properties
        if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
            return fLexicalHandler;
        }

        return fScanner.getProperty(name);
    }

    @Override
    public void setProperty(final String name, final Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        // Handle SAX2 properties
        if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
            setLexicalHandler((LexicalHandler) value);
            return;
        }

        fScanner.setProperty(name, value);
    }

} // class HTMLSAXScanner
