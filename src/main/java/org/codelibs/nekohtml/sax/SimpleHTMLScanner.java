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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Simple HTML scanner implementation that doesn't depend on Xerces.
 * This is a basic implementation that handles common HTML structures.
 *
 * @author CodeLibs Project
 */
public class SimpleHTMLScanner implements XMLReader {

    /** Logger for this class. */
    private static final Logger logger = Logger.getLogger(SimpleHTMLScanner.class.getName());

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

    /** Normalize element names. */
    protected boolean fNormalizeElements = true;

    /** Normalize attribute names. */
    protected boolean fNormalizeAttributes = true;

    /** Element name case. */
    protected String fElementCase = "upper";

    /** Attribute name case. */
    protected String fAttributeCase = "lower";

    // HTML element patterns
    private static final Pattern START_TAG = Pattern.compile("<([a-zA-Z][a-zA-Z0-9-:]*)([^>]*)>", Pattern.DOTALL);
    private static final Pattern END_TAG = Pattern.compile("</([a-zA-Z][a-zA-Z0-9-:]*)\\s*>");
    private static final Pattern COMMENT = Pattern.compile("<!--(.*?)-->", Pattern.DOTALL);
    private static final Pattern DOCTYPE = Pattern.compile("<!DOCTYPE\\s+([^>]+)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ATTRIBUTE = Pattern.compile("([a-zA-Z][a-zA-Z0-9:._-]*)(?:=(\"([^\"]*)\"|'([^']*)'|([^\\s>]+)))?");
    private static final Pattern CDATA = Pattern.compile("<!\\[CDATA\\[(.*?)\\]\\]>", Pattern.DOTALL);

    // Void elements (self-closing in HTML5)
    private static final java.util.Set<String> VOID_ELEMENTS = new java.util.HashSet<>();
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

    @Override
    public void setContentHandler(final ContentHandler handler) {
        fContentHandler = handler;
    }

    @Override
    public ContentHandler getContentHandler() {
        return fContentHandler;
    }

    @Override
    public void setDTDHandler(final DTDHandler handler) {
        fDTDHandler = handler;
    }

    @Override
    public DTDHandler getDTDHandler() {
        return fDTDHandler;
    }

    @Override
    public void setEntityResolver(final EntityResolver resolver) {
        fEntityResolver = resolver;
    }

    @Override
    public EntityResolver getEntityResolver() {
        return fEntityResolver;
    }

    @Override
    public void setErrorHandler(final ErrorHandler handler) {
        fErrorHandler = handler;
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
        if (input == null) {
            throw new SAXException("InputSource cannot be null");
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Starting HTML parsing from InputSource");
        }

        if (fContentHandler == null) {
            return;
        }

        // Get reader from input source
        Reader reader = input.getCharacterStream();
        if (reader == null) {
            InputStream stream = input.getByteStream();
            if (stream == null && input.getSystemId() != null) {
                // Open stream from systemId
                try {
                    final java.net.URI uri = new java.net.URI(input.getSystemId());
                    stream = uri.toURL().openStream();
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Opened input stream from SystemId: " + input.getSystemId());
                    }
                } catch (final java.net.URISyntaxException | java.net.MalformedURLException | IllegalArgumentException e) {
                    // Try as a file path
                    try {
                        stream = new java.io.FileInputStream(input.getSystemId());
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("Opened file input stream from SystemId: " + input.getSystemId());
                        }
                    } catch (final java.io.FileNotFoundException fnfe) {
                        throw new SAXException("Cannot open SystemId: " + input.getSystemId(), fnfe);
                    }
                }
            }
            if (stream != null) {
                String encoding = input.getEncoding();
                if (encoding == null) {
                    encoding = "UTF-8";
                }
                reader = new InputStreamReader(stream, encoding);
            }
        }

        if (reader == null) {
            throw new SAXException("No input source available");
        }

        // Read all content
        final StringBuilder content = new StringBuilder();
        final BufferedReader br = new BufferedReader(reader);
        String line;
        while ((line = br.readLine()) != null) {
            content.append(line).append('\n');
        }

        // Parse HTML
        final String htmlContent = content.toString();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Parsing HTML content (" + htmlContent.length() + " characters)");
        }
        parseHTML(htmlContent);
    }

    @Override
    public void parse(final String systemId) throws IOException, SAXException {
        parse(new InputSource(systemId));
    }

    /**
     * Parses HTML content.
     *
     * @param html The HTML content
     * @throws SAXException If a SAX error occurs
     */
    protected void parseHTML(final String html) throws SAXException {
        if (html == null) {
            throw new SAXException("HTML content cannot be null");
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Begin HTML parsing");
        }

        fContentHandler.startDocument();

        int pos = 0;
        final int length = html.length();

        while (pos < length) {
            final char ch = html.charAt(pos);

            if (ch == '<') {
                // Check for CDATA section
                if (html.startsWith("<![CDATA[", pos)) {
                    final Matcher m = CDATA.matcher(html.substring(pos));
                    if (m.find() && m.start() == 0) {
                        if (fLexicalHandler != null) {
                            fLexicalHandler.startCDATA();
                            final String cdataText = m.group(1);
                            if (cdataText.length() > 0) {
                                fContentHandler.characters(cdataText.toCharArray(), 0, cdataText.length());
                            }
                            fLexicalHandler.endCDATA();
                        } else {
                            // If no lexical handler, just emit the CDATA content as text
                            final String cdataText = m.group(1);
                            if (cdataText.length() > 0) {
                                fContentHandler.characters(cdataText.toCharArray(), 0, cdataText.length());
                            }
                        }
                        pos += m.end();
                        continue;
                    }
                }

                // Check for comment
                if (html.startsWith("<!--", pos)) {
                    final Matcher m = COMMENT.matcher(html.substring(pos));
                    if (m.find() && m.start() == 0) {
                        if (fLexicalHandler != null) {
                            final String commentText = m.group(1);
                            fLexicalHandler.comment(commentText.toCharArray(), 0, commentText.length());
                        }
                        pos += m.end();
                        continue;
                    }
                }

                // Check for DOCTYPE
                if (html.startsWith("<!DOCTYPE", pos) || html.startsWith("<!doctype", pos)) {
                    final Matcher m = DOCTYPE.matcher(html.substring(pos));
                    if (m.find() && m.start() == 0) {
                        if (fLexicalHandler != null) {
                            fLexicalHandler.startDTD("html", null, null);
                            fLexicalHandler.endDTD();
                        }
                        pos += m.end();
                        continue;
                    }
                }

                // Check for end tag
                final Matcher endMatcher = END_TAG.matcher(html.substring(pos));
                if (endMatcher.find() && endMatcher.start() == 0) {
                    final String tagName = normalizeElementName(endMatcher.group(1));
                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer("End element: " + tagName);
                    }
                    fContentHandler.endElement("", tagName, tagName);
                    pos += endMatcher.end();
                    continue;
                }

                // Check for start tag
                final Matcher startMatcher = START_TAG.matcher(html.substring(pos));
                if (startMatcher.find() && startMatcher.start() == 0) {
                    final String tagName = normalizeElementName(startMatcher.group(1));
                    final String attrString = startMatcher.group(2);

                    final AttributesImpl attrs = parseAttributes(attrString);
                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer("Start element: " + tagName + " (attributes: " + attrs.getLength() + ")");
                    }
                    fContentHandler.startElement("", tagName, tagName, attrs);

                    // Immediately close void elements
                    if (VOID_ELEMENTS.contains(tagName.toUpperCase())) {
                        if (logger.isLoggable(Level.FINER)) {
                            logger.finer("Auto-closing void element: " + tagName);
                        }
                        fContentHandler.endElement("", tagName, tagName);
                    }

                    pos += startMatcher.end();
                    continue;
                }

                // Unknown tag, skip character
                pos++;
            } else {
                // Text content
                final int nextTag = html.indexOf('<', pos);
                final int endPos = nextTag >= 0 ? nextTag : length;
                final String text = html.substring(pos, endPos);

                // Always emit text content, including whitespace
                // This preserves spacing between elements for proper text extraction
                if (text.length() > 0) {
                    fContentHandler.characters(text.toCharArray(), 0, text.length());
                }

                pos = endPos;
            }
        }

        fContentHandler.endDocument();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Completed HTML parsing");
        }
    }

    /**
     * Parses attributes from a string.
     *
     * @param attrString The attribute string
     * @return The parsed attributes
     */
    protected AttributesImpl parseAttributes(final String attrString) {
        final AttributesImpl attrs = new AttributesImpl();

        if (attrString == null || attrString.trim().isEmpty()) {
            return attrs;
        }

        final Matcher m = ATTRIBUTE.matcher(attrString);
        while (m.find()) {
            final String name = normalizeAttributeName(m.group(1));
            String value = m.group(3); // Double quoted
            if (value == null) {
                value = m.group(4); // Single quoted
            }
            if (value == null) {
                value = m.group(5); // Unquoted
            }
            if (value == null) {
                value = ""; // No value
            }

            attrs.addAttribute("", name, name, "CDATA", value);
        }

        return attrs;
    }

    /**
     * Normalizes an element name.
     *
     * @param name The element name
     * @return The normalized name
     */
    protected String normalizeElementName(final String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (!fNormalizeElements) {
            return name;
        }
        return "upper".equals(fElementCase) ? name.toUpperCase() : "lower".equals(fElementCase) ? name.toLowerCase() : name;
    }

    /**
     * Normalizes an attribute name.
     *
     * @param name The attribute name
     * @return The normalized name
     */
    protected String normalizeAttributeName(final String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (!fNormalizeAttributes) {
            return name;
        }
        return "upper".equals(fAttributeCase) ? name.toUpperCase() : "lower".equals(fAttributeCase) ? name.toLowerCase() : name;
    }

    @Override
    public boolean getFeature(final String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new SAXNotRecognizedException("Feature not recognized: " + name);
    }

    @Override
    public void setFeature(final String name, final boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        // Features not yet implemented
    }

    @Override
    public Object getProperty(final String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
            return fLexicalHandler;
        }
        throw new SAXNotRecognizedException("Property not recognized: " + name);
    }

    @Override
    public void setProperty(final String name, final Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
            setLexicalHandler((LexicalHandler) value);
            return;
        }
        // Properties not yet implemented
    }

} // class SimpleHTMLScanner
