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

import org.codelibs.nekohtml.HTMLEntities;
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

    /** Constructs a new SimpleHTMLScanner instance. */
    public SimpleHTMLScanner() {
    }

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
    private static final Pattern START_TAG = Pattern.compile("<([a-zA-Z][a-zA-Z0-9-:]*)([^>]*)>");
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
                } catch (final IOException ioe) {
                    // Wrap all IOExceptions (including FileNotFoundException from URL.openStream())
                    throw new SAXException("Cannot open SystemId: " + input.getSystemId(), ioe);
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
                final String rawText = html.substring(pos, endPos);

                // Always emit text content, including whitespace
                // This preserves spacing between elements for proper text extraction
                if (rawText.length() > 0) {
                    final String text = resolveEntities(rawText);
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

            attrs.addAttribute("", name, name, "CDATA", resolveEntities(value, true));
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

    // Pattern for HTML character references: &#decimal; or &#xhex; or &name;
    // Semicolon is optional to handle common malformed HTML
    private static final Pattern ENTITY_PATTERN = Pattern.compile("&(?:#([0-9]+)|#[xX]([0-9a-fA-F]+)|([a-zA-Z][a-zA-Z0-9]*));?");

    /**
     * Resolves HTML character entities in text content.
     * Semicolon-less named entities are decoded in text context.
     *
     * @param text The text containing entities
     * @return The text with entities resolved to their character equivalents
     */
    protected String resolveEntities(final String text) {
        return resolveEntities(text, false);
    }

    /**
     * Resolves HTML character entities in the given text.
     * Handles numeric decimal (&#214;), numeric hex (&#xD6;), and named (&Ouml;) entities.
     * In attribute context, semicolon-less named entities followed by [A-Za-z0-9=] are not decoded
     * per HTML5 attribute value state rules, preventing corruption of URLs like {@code &not=, &copy=}.
     *
     * @param text The text containing entities
     * @param inAttribute Whether this text is an attribute value
     * @return The text with entities resolved to their character equivalents
     */
    protected String resolveEntities(final String text, final boolean inAttribute) {
        if (text == null || text.indexOf('&') < 0) {
            return text;
        }

        final Matcher m = ENTITY_PATTERN.matcher(text);
        final StringBuilder sb = new StringBuilder(text.length());
        int lastEnd = 0;

        while (m.find()) {
            sb.append(text, lastEnd, m.start());

            if (m.group(1) != null) {
                // Numeric decimal: &#214;
                try {
                    final int codePoint = Integer.parseInt(m.group(1));
                    sb.append(resolveCodePoint(codePoint, m.group(0)));
                } catch (final NumberFormatException e) {
                    sb.append(m.group(0));
                }
            } else if (m.group(2) != null) {
                // Numeric hex: &#xD6;
                try {
                    final int codePoint = Integer.parseInt(m.group(2), 16);
                    sb.append(resolveCodePoint(codePoint, m.group(0)));
                } catch (final NumberFormatException e) {
                    sb.append(m.group(0));
                }
            } else if (m.group(3) != null) {
                // Named entity: &Ouml;
                final String matched = m.group(0);
                final boolean hasSemicolon = matched.endsWith(";");

                // HTML5 attribute value state: if no semicolon and next char is [A-Za-z0-9=],
                // do not decode (prevents corruption of URLs like &not=2, &copy=, &reg=)
                if (inAttribute && !hasSemicolon) {
                    final int afterEnd = m.end();
                    if (afterEnd < text.length()) {
                        final char nextChar = text.charAt(afterEnd);
                        if (Character.isLetterOrDigit(nextChar) || nextChar == '=') {
                            sb.append(matched);
                            lastEnd = m.end();
                            continue;
                        }
                    }
                }

                final int c = HTMLEntities.get(m.group(3));
                if (c != -1) {
                    sb.appendCodePoint(c);
                } else {
                    sb.append(matched);
                }
            }

            lastEnd = m.end();
        }

        sb.append(text, lastEnd, text.length());
        return sb.toString();
    }

    /**
     * Validates a numeric code point and returns the resolved character or replacement.
     * Invalid code points (null char, surrogates, out of range, XML-illegal) are replaced with U+FFFD.
     */
    private static String resolveCodePoint(final int codePoint, final String original) {
        if (codePoint == 0) {
            // Null character: replace with U+FFFD per HTML5 spec
            return "\uFFFD";
        }
        if (codePoint >= 0xD800 && codePoint <= 0xDFFF) {
            // Surrogate range: invalid Unicode scalar value
            return "\uFFFD";
        }
        if (codePoint > 0x10FFFF) {
            // Out of Unicode range
            return "\uFFFD";
        }
        // XML 1.0 illegal characters (except tab, newline, carriage return)
        if (codePoint < 0x20 && codePoint != 0x9 && codePoint != 0xA && codePoint != 0xD) {
            return "\uFFFD";
        }
        if (codePoint >= 0xFDD0 && codePoint <= 0xFDEF) {
            // Unicode noncharacters
            return "\uFFFD";
        }
        if ((codePoint & 0xFFFE) == 0xFFFE) {
            // U+xFFFE and U+xFFFF are noncharacters
            return "\uFFFD";
        }
        return new String(Character.toChars(codePoint));
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
