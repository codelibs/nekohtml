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

/**
 * Pure SAX-based HTML parser without Xerces dependencies.
 *
 * @author CodeLibs Project
 */
public class HTMLSAXParser implements XMLReader {

    /** The SAX configuration. */
    protected final HTMLSAXConfiguration fConfiguration;

    /**
     * Default constructor.
     */
    public HTMLSAXParser() {
        fConfiguration = new HTMLSAXConfiguration();
    }

    @Override
    public void setContentHandler(final ContentHandler handler) {
        fConfiguration.setContentHandler(handler);
    }

    @Override
    public ContentHandler getContentHandler() {
        return fConfiguration.getContentHandler();
    }

    @Override
    public void setDTDHandler(final DTDHandler handler) {
        fConfiguration.setDTDHandler(handler);
    }

    @Override
    public DTDHandler getDTDHandler() {
        return fConfiguration.getDTDHandler();
    }

    @Override
    public void setEntityResolver(final EntityResolver resolver) {
        fConfiguration.setEntityResolver(resolver);
    }

    @Override
    public EntityResolver getEntityResolver() {
        return fConfiguration.getEntityResolver();
    }

    @Override
    public void setErrorHandler(final ErrorHandler handler) {
        fConfiguration.setErrorHandler(handler);
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return fConfiguration.getErrorHandler();
    }

    /**
     * Sets the lexical handler.
     *
     * @param handler The lexical handler
     */
    public void setLexicalHandler(final LexicalHandler handler) {
        fConfiguration.setLexicalHandler(handler);
    }

    /**
     * Gets the lexical handler.
     *
     * @return The lexical handler
     */
    public LexicalHandler getLexicalHandler() {
        return fConfiguration.getLexicalHandler();
    }

    @Override
    public void parse(final InputSource input) throws IOException, SAXException {
        fConfiguration.parse(input);
    }

    @Override
    public void parse(final String systemId) throws IOException, SAXException {
        fConfiguration.parse(systemId);
    }

    @Override
    public boolean getFeature(final String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return fConfiguration.getFeature(name);
    }

    @Override
    public void setFeature(final String name, final boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        fConfiguration.setFeature(name, value);
    }

    @Override
    public Object getProperty(final String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return fConfiguration.getProperty(name);
    }

    @Override
    public void setProperty(final String name, final Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        fConfiguration.setProperty(name, value);
    }

} // class HTMLSAXParser
