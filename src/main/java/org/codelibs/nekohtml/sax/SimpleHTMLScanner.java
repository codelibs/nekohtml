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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;
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

    /**
     * DOCTYPE declaration parser applied to the bounded declaration string only.
     * Captures the root name, and either a PUBLIC (publicId [systemId]) or SYSTEM (systemId) identifier.
     */
    private static final Pattern DOCTYPE_DECL = Pattern.compile("<!DOCTYPE\\s+([^\\s>]+)"
            + "(?:\\s+PUBLIC\\s+(\"[^\"]*\"|'[^']*')(?:\\s+(\"[^\"]*\"|'[^']*'))?" + "|\\s+SYSTEM\\s+(\"[^\"]*\"|'[^']*'))?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

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

    // Raw-text elements: content is taken verbatim (no markup, no entity resolution) until the matching end tag.
    private static final java.util.Set<String> RAWTEXT_ELEMENTS = new java.util.HashSet<>();
    static {
        RAWTEXT_ELEMENTS.add("SCRIPT");
        RAWTEXT_ELEMENTS.add("STYLE");
        RAWTEXT_ELEMENTS.add("XMP");
        RAWTEXT_ELEMENTS.add("IFRAME");
        RAWTEXT_ELEMENTS.add("NOEMBED");
        RAWTEXT_ELEMENTS.add("NOFRAMES");
    }

    // RCDATA elements: content is taken as text (no markup) until the matching end tag, but entities are resolved.
    private static final java.util.Set<String> RCDATA_ELEMENTS = new java.util.HashSet<>();
    static {
        RCDATA_ELEMENTS.add("TEXTAREA");
        RCDATA_ELEMENTS.add("TITLE");
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

        // Get reader from input source. Track whether this method opened the
        // underlying resource so that it can be closed afterwards; caller-provided
        // streams/readers are left open per the SAX convention.
        Reader reader = input.getCharacterStream();
        Reader readerToClose = null;
        if (reader == null) {
            InputStream stream = input.getByteStream();
            boolean opened = false;
            if (stream == null && input.getSystemId() != null) {
                // Open stream from systemId
                try {
                    final java.net.URI uri = new java.net.URI(input.getSystemId());
                    stream = uri.toURL().openStream();
                    opened = true;
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Opened input stream from SystemId: " + input.getSystemId());
                    }
                } catch (final java.net.URISyntaxException | java.net.MalformedURLException | IllegalArgumentException e) {
                    // Try as a file path
                    try {
                        stream = new java.io.FileInputStream(input.getSystemId());
                        opened = true;
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
                if (opened) {
                    // We opened the underlying stream, so we are responsible for closing it.
                    readerToClose = reader;
                }
            }
        }

        if (reader == null) {
            throw new SAXException("No input source available");
        }

        try {
            // Read all content via a fixed-size buffer (no trailing newline appended).
            final StringBuilder content = new StringBuilder();
            final char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                content.append(buffer, 0, read);
            }

            // Parse HTML
            final String htmlContent = content.toString();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Parsing HTML content (" + htmlContent.length() + " characters)");
            }
            parseHTML(htmlContent);
        } finally {
            if (readerToClose != null) {
                try {
                    readerToClose.close();
                } catch (final IOException e) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Failed to close input stream: " + e.getMessage());
                    }
                }
            }
        }
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

        // Normalize line endings once: CRLF and lone CR both become LF.
        final String source = normalizeLineEndings(html);

        fContentHandler.startDocument();

        final int length = source.length();
        final StringBuilder text = new StringBuilder();
        int pos = 0;

        while (pos < length) {
            final char ch = source.charAt(pos);
            if (ch != '<') {
                // Accumulate a text run up to the next '<'.
                final int lt = source.indexOf('<', pos);
                final int end = lt < 0 ? length : lt;
                text.append(source, pos, end);
                pos = end;
                continue;
            }

            // ch == '<': dispatch on the following character.
            final char next = pos + 1 < length ? source.charAt(pos + 1) : '\0';
            if (isAsciiLetter(next)) {
                flushText(text);
                pos = scanStartTag(source, pos, length);
            } else if (next == '/') {
                final char afterSlash = pos + 2 < length ? source.charAt(pos + 2) : '\0';
                if (isAsciiLetter(afterSlash)) {
                    flushText(text);
                    pos = scanEndTag(source, pos, length);
                } else if (afterSlash == '>') {
                    // "</>" is ignored entirely (surrounding text stays contiguous).
                    pos += 3;
                } else {
                    // Bogus comment starting after "</".
                    flushText(text);
                    pos = scanBogusComment(source, pos + 2, length);
                }
            } else if (next == '!') {
                flushText(text);
                pos = scanMarkupDeclaration(source, pos, length);
            } else if (next == '?') {
                // Processing instruction -> bogus comment (content from '?').
                flushText(text);
                pos = scanBogusComment(source, pos + 1, length);
            } else {
                // Not a tag start: treat '<' as literal text.
                text.append('<');
                pos++;
            }
        }

        flushText(text);

        fContentHandler.endDocument();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Completed HTML parsing");
        }
    }

    /**
     * Normalizes CRLF and lone CR line endings to LF. Returns the input unchanged when no CR is present.
     *
     * @param html The raw HTML content
     * @return The content with normalized line endings
     */
    private static String normalizeLineEndings(final String html) {
        if (html.indexOf('\r') < 0) {
            return html;
        }
        final int length = html.length();
        final StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            final char c = html.charAt(i);
            if (c == '\r') {
                sb.append('\n');
                if (i + 1 < length && html.charAt(i + 1) == '\n') {
                    i++;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Flushes the accumulated text run, resolving entities, as a single {@code characters()} event.
     *
     * @param text The accumulated text buffer (cleared on return)
     * @throws SAXException If a SAX error occurs
     */
    private void flushText(final StringBuilder text) throws SAXException {
        if (text.length() == 0) {
            return;
        }
        final String resolved = resolveEntities(text.toString());
        text.setLength(0);
        if (!resolved.isEmpty()) {
            fContentHandler.characters(resolved.toCharArray(), 0, resolved.length());
        }
    }

    /**
     * Scans a start tag beginning at {@code startPos} (the '&lt;'), emitting the corresponding
     * {@code startElement} (and, for void/self-closing/raw-text elements, additional events).
     *
     * @param html The source content
     * @param startPos The index of the opening '&lt;'
     * @param length The source length
     * @return The index immediately after the consumed markup
     * @throws SAXException If a SAX error occurs
     */
    private int scanStartTag(final String html, final int startPos, final int length) throws SAXException {
        int pos = startPos + 1;
        final int nameStart = pos;
        pos++; // first character is a guaranteed ASCII letter
        while (pos < length && isNameChar(html.charAt(pos))) {
            pos++;
        }
        final String rawName = html.substring(nameStart, pos);

        final AttributesImpl attrs = new AttributesImpl();
        boolean terminated = false;
        boolean selfClosing = false;

        while (pos < length) {
            // Skip whitespace before the next attribute.
            while (pos < length && isSpace(html.charAt(pos))) {
                pos++;
            }
            if (pos >= length) {
                break; // EOF in tag
            }
            final char c = html.charAt(pos);
            if (c == '>') {
                pos++;
                terminated = true;
                break;
            }
            if (c == '/') {
                if (pos + 1 < length && html.charAt(pos + 1) == '>') {
                    pos += 2;
                    terminated = true;
                    selfClosing = true;
                    break;
                }
                pos++; // stray slash
                continue;
            }

            // Attribute name (HTML5 attribute-name state).
            final int attrNameStart = pos;
            while (pos < length) {
                final char nc = html.charAt(pos);
                if (isSpace(nc) || nc == '=' || nc == '/' || nc == '>') {
                    break;
                }
                pos++;
            }
            final String attrName = html.substring(attrNameStart, pos);

            // Skip whitespace between name and '='.
            while (pos < length && isSpace(html.charAt(pos))) {
                pos++;
            }

            String value = "";
            if (pos < length && html.charAt(pos) == '=') {
                pos++; // consume '='
                while (pos < length && isSpace(html.charAt(pos))) {
                    pos++;
                }
                if (pos >= length) {
                    break; // EOF in tag
                }
                final char q = html.charAt(pos);
                if (q == '"' || q == '\'') {
                    pos++; // opening quote
                    final int valueStart = pos;
                    while (pos < length && html.charAt(pos) != q) {
                        pos++;
                    }
                    if (pos >= length) {
                        // Unterminated quoted value == EOF in tag: drop the partial tag.
                        return length;
                    }
                    value = html.substring(valueStart, pos);
                    pos++; // closing quote
                } else if (q == '>') {
                    // Missing attribute value; leave the '>' for the loop to handle.
                    value = "";
                } else {
                    final int valueStart = pos;
                    while (pos < length) {
                        final char vc = html.charAt(pos);
                        if (isSpace(vc) || vc == '>') {
                            break;
                        }
                        pos++;
                    }
                    value = html.substring(valueStart, pos);
                }
            }

            addAttribute(attrs, attrName, value);
        }

        if (!terminated) {
            // HTML5 eof-in-tag: emit nothing for the partial tag.
            return length;
        }

        final String qName = normalizeElementName(rawName);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Start element: " + qName + " (attributes: " + attrs.getLength() + ")");
        }
        fContentHandler.startElement("", qName, qName, attrs);

        final String upperName = rawName.toUpperCase(Locale.ROOT);
        if (VOID_ELEMENTS.contains(upperName)) {
            fContentHandler.endElement("", qName, qName);
            return pos;
        }
        if (selfClosing) {
            // HTML5 ignores the slash on non-void HTML elements; do not enter raw-text mode.
            return pos;
        }
        if (RAWTEXT_ELEMENTS.contains(upperName)) {
            return scanRawText(html, pos, length, rawName, qName, false);
        }
        if (RCDATA_ELEMENTS.contains(upperName)) {
            return scanRawText(html, pos, length, rawName, qName, true);
        }
        return pos;
    }

    /**
     * Scans raw-text (or RCDATA) element content up to the matching end tag.
     *
     * @param html The source content
     * @param contentStart The index of the first content character
     * @param length The source length
     * @param rawName The element name as written
     * @param qName The normalized element name to report
     * @param resolveEntities Whether to resolve entities in the content (RCDATA) or not (RAWTEXT)
     * @return The index immediately after the consumed content and end tag
     * @throws SAXException If a SAX error occurs
     */
    private int scanRawText(final String html, final int contentStart, final int length, final String rawName, final String qName,
            final boolean resolveEntities) throws SAXException {
        final int closeLt = findRawTextClose(html, contentStart, length, rawName);
        final int contentEnd = closeLt < 0 ? length : closeLt;
        if (contentEnd > contentStart) {
            final String content = html.substring(contentStart, contentEnd);
            final String out = resolveEntities ? resolveEntities(content) : content;
            if (!out.isEmpty()) {
                fContentHandler.characters(out.toCharArray(), 0, out.length());
            }
        }
        if (closeLt < 0) {
            // No matching end tag; the balancer closes the element at end of document.
            return length;
        }
        // Consume the end tag up to and including its '>'.
        final int gt = html.indexOf('>', closeLt);
        fContentHandler.endElement("", qName, qName);
        return gt < 0 ? length : gt + 1;
    }

    /**
     * Finds the index of the '&lt;' that begins the matching {@code &lt;/name} end tag for a
     * raw-text/RCDATA element, or -1 if none exists.
     *
     * @param html The source content
     * @param from The index to start searching from
     * @param length The source length
     * @param name The element name to match (case-insensitive)
     * @return The index of the closing tag's '&lt;', or -1
     */
    private static int findRawTextClose(final String html, final int from, final int length, final String name) {
        final int nameLen = name.length();
        int searchPos = from;
        while (true) {
            final int lt = html.indexOf('<', searchPos);
            if (lt < 0) {
                return -1;
            }
            if (lt + 1 < length && html.charAt(lt + 1) == '/' && html.regionMatches(true, lt + 2, name, 0, nameLen)) {
                final int after = lt + 2 + nameLen;
                final char term = after < length ? html.charAt(after) : '\0';
                if (term == '\0' || isSpace(term) || term == '/' || term == '>') {
                    return lt;
                }
            }
            searchPos = lt + 1;
        }
    }

    /**
     * Scans an end tag beginning at {@code startPos} (the '&lt;'), emitting {@code endElement}.
     * The scan is quote-aware: a '&gt;' inside a quoted value does not close the tag.
     *
     * @param html The source content
     * @param startPos The index of the opening '&lt;'
     * @param length The source length
     * @return The index immediately after the consumed markup
     * @throws SAXException If a SAX error occurs
     */
    private int scanEndTag(final String html, final int startPos, final int length) throws SAXException {
        int pos = startPos + 2; // skip "</"
        final int nameStart = pos;
        pos++; // first character is a guaranteed ASCII letter
        while (pos < length && isNameChar(html.charAt(pos))) {
            pos++;
        }
        final String rawName = html.substring(nameStart, pos);

        // Skip the remainder of the tag (including attributes) up to '>', quote-aware.
        while (pos < length) {
            final char c = html.charAt(pos);
            if (c == '"' || c == '\'') {
                pos++;
                while (pos < length && html.charAt(pos) != c) {
                    pos++;
                }
                if (pos < length) {
                    pos++; // closing quote
                }
            } else if (c == '>') {
                pos++;
                break;
            } else {
                pos++;
            }
        }

        final String qName = normalizeElementName(rawName);
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("End element: " + qName);
        }
        fContentHandler.endElement("", qName, qName);
        return pos;
    }

    /**
     * Handles a markup declaration ({@code &lt;!...}): comment, CDATA section, DOCTYPE or bogus comment.
     *
     * @param html The source content
     * @param startPos The index of the opening '&lt;'
     * @param length The source length
     * @return The index immediately after the consumed markup
     * @throws SAXException If a SAX error occurs
     */
    private int scanMarkupDeclaration(final String html, final int startPos, final int length) throws SAXException {
        if (html.regionMatches(startPos, "<!--", 0, 4)) {
            // Comment: runs to "-->", or to EOF if unterminated (HTML5 eof-in-comment).
            final int idx = html.indexOf("-->", startPos + 4);
            final int contentEnd = idx < 0 ? length : idx;
            if (fLexicalHandler != null) {
                final String content = html.substring(startPos + 4, contentEnd);
                fLexicalHandler.comment(content.toCharArray(), 0, content.length());
            }
            return idx < 0 ? length : idx + 3;
        }
        if (html.regionMatches(startPos, "<![CDATA[", 0, 9)) {
            final int idx = html.indexOf("]]>", startPos + 9);
            final int contentEnd = idx < 0 ? length : idx;
            final String content = html.substring(startPos + 9, contentEnd);
            if (fLexicalHandler != null) {
                fLexicalHandler.startCDATA();
                if (!content.isEmpty()) {
                    fContentHandler.characters(content.toCharArray(), 0, content.length());
                }
                fLexicalHandler.endCDATA();
            } else if (!content.isEmpty()) {
                fContentHandler.characters(content.toCharArray(), 0, content.length());
            }
            return idx < 0 ? length : idx + 3;
        }
        if (html.regionMatches(true, startPos, "<!DOCTYPE", 0, 9)) {
            final int gt = html.indexOf('>', startPos);
            final int declEnd = gt < 0 ? length : gt;
            parseDoctype(html.substring(startPos, declEnd));
            return gt < 0 ? length : gt + 1;
        }
        // Any other "<!..." is a bogus comment; content is everything between "<!" and '>'.
        return scanBogusComment(html, startPos + 2, length);
    }

    /**
     * Consumes a bogus comment: content from {@code contentStart} up to the next '&gt;' (or EOF),
     * reporting it via the lexical handler if one is set.
     *
     * @param html The source content
     * @param contentStart The index of the first content character
     * @param length The source length
     * @return The index immediately after the consumed markup
     * @throws SAXException If a SAX error occurs
     */
    private int scanBogusComment(final String html, final int contentStart, final int length) throws SAXException {
        final int gt = html.indexOf('>', contentStart);
        final int contentEnd = gt < 0 ? length : gt;
        if (fLexicalHandler != null) {
            final String content = html.substring(contentStart, contentEnd);
            fLexicalHandler.comment(content.toCharArray(), 0, content.length());
        }
        return gt < 0 ? length : gt + 1;
    }

    /**
     * Parses a bounded DOCTYPE declaration string and reports it via {@code startDTD}/{@code endDTD}.
     *
     * @param decl The bounded declaration, from '&lt;!DOCTYPE' up to (not including) '&gt;'
     * @throws SAXException If a SAX error occurs
     */
    private void parseDoctype(final String decl) throws SAXException {
        if (fLexicalHandler == null) {
            return;
        }
        String name = null;
        String publicId = null;
        String systemId = null;
        final Matcher m = DOCTYPE_DECL.matcher(decl);
        if (m.lookingAt()) {
            name = m.group(1);
            if (m.group(2) != null) {
                publicId = stripQuotes(m.group(2));
                if (m.group(3) != null) {
                    systemId = stripQuotes(m.group(3));
                }
            } else if (m.group(4) != null) {
                systemId = stripQuotes(m.group(4));
            }
        }
        fLexicalHandler.startDTD(name, publicId, systemId);
        fLexicalHandler.endDTD();
    }

    /**
     * Adds an attribute to the collection, normalizing the name, resolving entities in the value,
     * and dropping duplicates (first occurrence wins) and names that are not valid XML names.
     *
     * @param attrs The attribute collection
     * @param rawName The attribute name as written
     * @param value The raw attribute value
     */
    private void addAttribute(final AttributesImpl attrs, final String rawName, final String value) {
        if (rawName.isEmpty()) {
            return;
        }
        final String name = normalizeAttributeName(rawName);
        if (!XMLChar.isValidName(name)) {
            // Cannot be represented as a DOM/XML attribute name (HTML5 parse error); skip it.
            return;
        }
        if (attrs.getIndex(name) >= 0) {
            return; // duplicate: first occurrence wins
        }
        attrs.addAttribute("", name, name, "CDATA", resolveEntities(value, true));
    }

    /** Returns true for ASCII letters (tag-name start characters). */
    private static boolean isAsciiLetter(final char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /** Returns true for characters allowed after the first character of an element name. */
    private static boolean isNameChar(final char c) {
        return isAsciiLetter(c) || (c >= '0' && c <= '9') || c == ':' || c == '_' || c == '.' || c == '-';
    }

    /** Returns true for HTML whitespace characters. */
    private static boolean isSpace(final char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r';
    }

    /** Removes the surrounding single or double quote characters from a quoted token. */
    private static String stripQuotes(final String quoted) {
        return quoted.substring(1, quoted.length() - 1);
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
     * Sets the element name case normalization mode.
     *
     * @param c {@code "upper"}, {@code "lower"}, or {@code "match"}/{@code "default"}/{@code "no-change"}
     *          (all of the latter meaning keep the name as written)
     */
    public void setElementCase(final String c) {
        fElementCase = c;
    }

    /**
     * Sets the attribute name case normalization mode.
     *
     * @param c {@code "upper"}, {@code "lower"}, or {@code "match"}/{@code "default"}/{@code "no-change"}
     *          (all of the latter meaning keep the name as written)
     */
    public void setAttributeCase(final String c) {
        fAttributeCase = c;
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

    /** SAX2 standard "namespaces" feature name. */
    private static final String FEATURE_NAMESPACES = "http://xml.org/sax/features/namespaces";

    /** SAX2 standard "namespace-prefixes" feature name. */
    private static final String FEATURE_NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";

    @Override
    public boolean getFeature(final String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (FEATURE_NAMESPACES.equals(name) || FEATURE_NAMESPACE_PREFIXES.equals(name)) {
            // Neither feature is actually implemented by this scanner; report false.
            return false;
        }
        throw new SAXNotRecognizedException("Feature not recognized: " + name);
    }

    @Override
    public void setFeature(final String name, final boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (FEATURE_NAMESPACES.equals(name) || FEATURE_NAMESPACE_PREFIXES.equals(name)) {
            if (value) {
                // Enabling either feature is not supported; disabling (the default) is a no-op.
                throw new SAXNotSupportedException("Feature not supported: " + name);
            }
            return;
        }
        throw new SAXNotRecognizedException("Feature not recognized: " + name);
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
