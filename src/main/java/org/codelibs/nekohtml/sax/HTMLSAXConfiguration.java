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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.xml.sax.helpers.DefaultHandler;

/**
 * Pure SAX-based HTML parser configuration.
 * This class orchestrates a SAX filter pipeline for HTML parsing without
 * requiring XNI dependencies.
 *
 * @author CodeLibs Project
 */
public class HTMLSAXConfiguration implements XMLReader {

    /** Logger for this class. */
    private static final Logger logger = Logger.getLogger(HTMLSAXConfiguration.class.getName());

    // Feature identifiers

    /** Namespaces feature identifier. */
    public static final String NAMESPACES = "http://xml.org/sax/features/namespaces";

    /** Augmentations feature identifier. */
    public static final String AUGMENTATIONS = "http://cyberneko.org/html/features/augmentations";

    /** Report errors feature identifier. */
    public static final String REPORT_ERRORS = "http://cyberneko.org/html/features/report-errors";

    /** Simple error format feature identifier. */
    public static final String SIMPLE_ERROR_FORMAT = "http://cyberneko.org/html/features/report-errors/simple-format";

    /** Balance tags feature identifier. */
    public static final String BALANCE_TAGS = "http://cyberneko.org/html/features/balance-tags";

    /** Normalize element names feature identifier. */
    public static final String NAMES_ELEMS = "http://cyberneko.org/html/features/scanner/normalize-elements";

    /** Normalize attribute names feature identifier. */
    public static final String NAMES_ATTRS = "http://cyberneko.org/html/features/scanner/normalize-attrs";

    /** HTML5 mode feature identifier. */
    public static final String HTML5_MODE = "http://cyberneko.org/html/features/html5-mode";

    // Property identifiers

    /** Filters property identifier. */
    public static final String FILTERS = "http://cyberneko.org/html/properties/filters";

    /** Error reporter property identifier. */
    public static final String ERROR_REPORTER = "http://cyberneko.org/html/properties/error-reporter";

    // Protected fields

    /** Features map. */
    protected final Map<String, Boolean> fFeatures;

    /** Properties map. */
    protected final Map<String, Object> fProperties;

    /** Content handler. */
    protected ContentHandler fContentHandler;

    /** DTD handler. */
    protected DTDHandler fDTDHandler;

    /** Entity resolver. */
    protected EntityResolver fEntityResolver;

    /** Error handler. */
    protected ErrorHandler fErrorHandler;

    /** Lexical handler. */
    protected LexicalHandler fLexicalHandler;

    /** Locale. */
    protected Locale fLocale;

    /** SAX filter pipeline. */
    protected final List<XMLReader> fPipeline;

    /** Scanner (first filter in pipeline). */
    protected HTMLSAXScanner fScanner;

    /** Tag balancer filter. */
    protected HTMLTagBalancerFilter fTagBalancer;

    /** Namespace binder filter (optional). */
    protected XMLReader fNamespaceBinder;

    /**
     * Default constructor.
     */
    public HTMLSAXConfiguration() {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Initializing HTMLSAXConfiguration");
        }

        fFeatures = new HashMap<>();
        fProperties = new HashMap<>();
        fPipeline = new ArrayList<>();

        // Set default features
        fFeatures.put(NAMESPACES, false);
        fFeatures.put(AUGMENTATIONS, false);
        fFeatures.put(REPORT_ERRORS, false);
        fFeatures.put(SIMPLE_ERROR_FORMAT, false);
        fFeatures.put(BALANCE_TAGS, true);
        fFeatures.put(HTML5_MODE, false);

        // Set default properties (note: NAMES_ELEMS and NAMES_ATTRS are properties, not features)
        fProperties.put(NAMES_ELEMS, "upper");
        fProperties.put(NAMES_ATTRS, "lower");

        // Build the default pipeline: Scanner -> TagBalancer
        buildPipeline();

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("HTMLSAXConfiguration initialized with " + fPipeline.size() + " filters");
        }
    }

    /**
     * Builds the SAX filter pipeline.
     */
    protected void buildPipeline() {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Building SAX filter pipeline");
        }

        fPipeline.clear();

        // Create scanner (first in pipeline)
        if (fScanner == null) {
            fScanner = createScanner();
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Created HTML scanner");
            }
        }
        fPipeline.add(fScanner);

        // Add tag balancer if enabled
        if (Boolean.TRUE.equals(fFeatures.get(BALANCE_TAGS))) {
            if (fTagBalancer == null) {
                fTagBalancer = new HTMLTagBalancerFilter();
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Created tag balancer filter");
                }
            }
            fPipeline.add(fTagBalancer);

            // Connect scanner to tag balancer
            fScanner.setContentHandler(fTagBalancer);
        }

        // TODO: Add namespace binder if namespaces enabled
        // TODO: Add custom filters from FILTERS property

        // Reconnect content handler to the last filter in pipeline
        if (fContentHandler != null) {
            final XMLReader lastFilter = getLastFilter();
            if (lastFilter != null) {
                lastFilter.setContentHandler(fContentHandler);
            }
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Pipeline built with " + fPipeline.size() + " filters");
        }
    }

    /**
     * Creates the HTML scanner.
     *
     * @return The HTML scanner
     */
    protected HTMLSAXScanner createScanner() {
        return new HTMLSAXScanner();
    }

    /**
     * Gets the last filter in the pipeline.
     *
     * @return The last XMLReader in the pipeline
     */
    protected XMLReader getLastFilter() {
        return fPipeline.isEmpty() ? null : fPipeline.get(fPipeline.size() - 1);
    }

    // XMLReader implementation

    @Override
    public void setContentHandler(final ContentHandler handler) {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Setting content handler: " + (handler != null ? handler.getClass().getName() : "null"));
        }

        fContentHandler = handler;

        // Set the content handler on the last filter in the pipeline
        final XMLReader lastFilter = getLastFilter();
        if (lastFilter != null) {
            lastFilter.setContentHandler(handler);
        }

        // Ensure the scanner feeds into the tag balancer if present
        if (fScanner != null && fTagBalancer != null && Boolean.TRUE.equals(fFeatures.get(BALANCE_TAGS))) {
            fScanner.setContentHandler(fTagBalancer);
            fTagBalancer.setContentHandler(handler);
        } else if (fScanner != null) {
            fScanner.setContentHandler(handler);
        }
    }

    @Override
    public ContentHandler getContentHandler() {
        return fContentHandler;
    }

    @Override
    public void setDTDHandler(final DTDHandler handler) {
        fDTDHandler = handler;
        final XMLReader lastFilter = getLastFilter();
        if (lastFilter != null) {
            lastFilter.setDTDHandler(handler);
        }
    }

    @Override
    public DTDHandler getDTDHandler() {
        return fDTDHandler;
    }

    @Override
    public void setEntityResolver(final EntityResolver resolver) {
        fEntityResolver = resolver;
        for (final XMLReader filter : fPipeline) {
            filter.setEntityResolver(resolver);
        }
    }

    @Override
    public EntityResolver getEntityResolver() {
        return fEntityResolver;
    }

    @Override
    public void setErrorHandler(final ErrorHandler handler) {
        fErrorHandler = handler;
        for (final XMLReader filter : fPipeline) {
            filter.setErrorHandler(handler);
        }
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return fErrorHandler;
    }

    /**
     * Sets the lexical handler.
     *
     * @param handler The lexical handler
     */
    public void setLexicalHandler(final LexicalHandler handler) {
        fLexicalHandler = handler;
        // Set on filters that support LexicalHandler
        for (final XMLReader filter : fPipeline) {
            if (filter instanceof HTMLTagBalancerFilter) {
                ((HTMLTagBalancerFilter) filter).setLexicalHandler(handler);
            } else if (filter instanceof HTMLSAXScanner) {
                ((HTMLSAXScanner) filter).setLexicalHandler(handler);
            }
        }
    }

    /**
     * Gets the lexical handler.
     *
     * @return The lexical handler
     */
    public LexicalHandler getLexicalHandler() {
        return fLexicalHandler;
    }

    @Override
    public void parse(final InputSource input) throws IOException, SAXException {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Starting parse with " + fPipeline.size() + " filters in pipeline");
        }

        if (fScanner == null) {
            throw new SAXException("Parser not configured");
        }

        // Ensure handlers are set
        if (fContentHandler == null) {
            setContentHandler(new DefaultHandler());
        }

        // Parse through the scanner (first filter in pipeline)
        // The scanner will generate SAX events that flow through the filters to the content handler
        fScanner.parse(input);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Parse completed");
        }
    }

    @Override
    public void parse(final String systemId) throws IOException, SAXException {
        parse(new InputSource(systemId));
    }

    @Override
    public boolean getFeature(final String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (fFeatures.containsKey(name)) {
            final Object value = fFeatures.get(name);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        }
        throw new SAXNotRecognizedException("Feature not recognized: " + name);
    }

    @Override
    public void setFeature(final String name, final boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Setting feature: " + name + " = " + value);
        }

        fFeatures.put(name, value);

        // Handle feature changes that affect pipeline
        if (BALANCE_TAGS.equals(name)) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("BALANCE_TAGS feature changed, rebuilding pipeline");
            }
            buildPipeline();
            if (fContentHandler != null) {
                setContentHandler(fContentHandler);
            }
        }

        // Propagate to filters
        for (final XMLReader filter : fPipeline) {
            try {
                filter.setFeature(name, value);
            } catch (final SAXNotRecognizedException | SAXNotSupportedException e) {
                // Filter doesn't recognize this feature, ignore
            }
        }
    }

    @Override
    public Object getProperty(final String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        // Handle SAX2 properties
        if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
            return fLexicalHandler;
        }

        if (fProperties.containsKey(name)) {
            return fProperties.get(name);
        }
        throw new SAXNotRecognizedException("Property not recognized: " + name);
    }

    @Override
    public void setProperty(final String name, final Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        // Handle SAX2 properties
        if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
            setLexicalHandler((LexicalHandler) value);
            return;
        }

        fProperties.put(name, value);

        // Handle properties that affect pipeline
        if (FILTERS.equals(name)) {
            buildPipeline();
            if (fContentHandler != null) {
                setContentHandler(fContentHandler);
            }
        }

        // Propagate to filters
        for (final XMLReader filter : fPipeline) {
            try {
                filter.setProperty(name, value);
            } catch (final SAXNotRecognizedException | SAXNotSupportedException e) {
                // Filter doesn't recognize this property, ignore
            }
        }
    }

    /**
     * Sets the locale.
     *
     * @param locale The locale
     */
    public void setLocale(final Locale locale) {
        fLocale = locale;
    }

    /**
     * Gets the locale.
     *
     * @return The locale
     */
    public Locale getLocale() {
        return fLocale;
    }

} // class HTMLSAXConfiguration
