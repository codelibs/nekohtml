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

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.codelibs.nekohtml.sax.HTMLSAXParser;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * DOM parser for HTML documents using SAX and standard JAXP.
 * This implementation uses the SAX-based HTML parser and converts the events to a DOM tree.
 *
 * @author CodeLibs Project
 */
public class DOMParser {

    /** The SAX parser. */
    protected final HTMLSAXParser saxParser;

    /** The DOM document builder. */
    protected final DocumentBuilder documentBuilder;

    /** The resulting DOM document. */
    protected Document document;

    /**
     * Default constructor.
     *
     * @throws ParserConfigurationException If a DocumentBuilder cannot be created
     */
    public DOMParser() throws ParserConfigurationException {
        saxParser = new HTMLSAXParser();

        // Create a DOM builder using standard JAXP
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        documentBuilder = factory.newDocumentBuilder();
    }

    /**
     * Parses an HTML document and builds a DOM tree.
     *
     * @param source The input source
     * @throws SAXException If a SAX error occurs
     * @throws IOException If an I/O error occurs
     */
    public void parse(final InputSource source) throws SAXException, IOException {
        // Create a SAX to DOM handler
        final SAXToDOMHandler handler = new SAXToDOMHandler(documentBuilder);

        saxParser.setContentHandler(handler);
        saxParser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
        saxParser.parse(source);

        document = handler.getDocument();
    }

    /**
     * Gets the parsed DOM document.
     *
     * @return The DOM document
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Sets the error handler.
     *
     * @param errorHandler The error handler
     */
    public void setErrorHandler(final ErrorHandler errorHandler) {
        saxParser.setErrorHandler(errorHandler);
        documentBuilder.setErrorHandler(errorHandler);
    }

    /**
     * Sets the entity resolver.
     *
     * @param entityResolver The entity resolver
     */
    public void setEntityResolver(final EntityResolver entityResolver) {
        saxParser.setEntityResolver(entityResolver);
        documentBuilder.setEntityResolver(entityResolver);
    }

    /**
     * Sets a feature on the underlying SAX parser.
     *
     * @param name The feature name
     * @param value The feature value
     * @throws SAXException If the feature cannot be set
     */
    public void setFeature(final String name, final boolean value) throws SAXException {
        try {
            saxParser.setFeature(name, value);
        } catch (final Exception e) {
            throw new SAXException("Feature not supported: " + name, e);
        }
    }

    /**
     * Gets a feature from the underlying SAX parser.
     *
     * @param name The feature name
     * @return The feature value
     * @throws SAXException If the feature cannot be retrieved
     */
    public boolean getFeature(final String name) throws SAXException {
        try {
            return saxParser.getFeature(name);
        } catch (final Exception e) {
            throw new SAXException("Feature not supported: " + name, e);
        }
    }

    /**
     * Sets a property on the underlying SAX parser.
     *
     * @param name The property name
     * @param value The property value
     * @throws SAXException If the property cannot be set
     */
    public void setProperty(final String name, final Object value) throws SAXException {
        try {
            saxParser.setProperty(name, value);
        } catch (final Exception e) {
            throw new SAXException("Property not supported: " + name, e);
        }
    }

    /**
     * Gets a property from the underlying SAX parser.
     *
     * @param name The property name
     * @return The property value
     * @throws SAXException If the property cannot be retrieved
     */
    public Object getProperty(final String name) throws SAXException {
        try {
            return saxParser.getProperty(name);
        } catch (final Exception e) {
            throw new SAXException("Property not supported: " + name, e);
        }
    }

} // class DOMParser
