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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codelibs.nekohtml.HTMLElements;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * SAX filter for HTML tag balancing.
 *
 * <p>
 * This filter guarantees a well-formed, balanced event stream regardless of how
 * malformed the incoming HTML is: for every {@code startElement} it emits,
 * exactly one matching {@code endElement} is emitted before its parent's
 * {@code endElement}, and it never emits an {@code endElement} without a prior
 * matching {@code startElement}. It also synthesizes the {@code HTML},
 * {@code HEAD} and {@code BODY} structure implied by the HTML5 tree
 * construction algorithm, applies implied end tags for sibling containers
 * (list items, table cells, paragraphs, ...), and ignores stray end tags.
 * </p>
 *
 * @author CodeLibs Project
 */
public class HTMLTagBalancerFilter extends XMLFilterImpl implements LexicalHandler {

    /** Logger for this class. */
    private static final Logger logger = Logger.getLogger(HTMLTagBalancerFilter.class.getName());

    /** Lexical handler for DTD and CDATA events. */
    protected LexicalHandler lexicalHandler;

    /**
     * An open element on the stack. Attributes are copied at push time so that
     * an element can be reopened (with its attributes preserved) when a
     * misnested formatting end tag forces its enclosing containers to be
     * reconstructed.
     */
    protected static final class ElementEntry {
        /** The namespace URI as received. */
        final String uri;
        /** The local name as received. */
        final String localName;
        /** The qualified name as received. */
        final String qName;
        /** The upper-cased element name used for structural matching. */
        final String tagName;
        /** A defensive copy of the element's attributes. */
        final AttributesImpl attrs;

        ElementEntry(final String uri, final String localName, final String qName, final String tagName, final Attributes atts) {
            this.uri = uri;
            this.localName = localName;
            this.qName = qName;
            this.tagName = tagName;
            this.attrs = new AttributesImpl(atts);
        }
    }

    /** Stack of open elements (top of stack is the head of the deque). */
    protected final Deque<ElementEntry> elementStack = new ArrayDeque<>();

    /**
     * Count of currently-open elements per upper-cased tag name. Kept in sync
     * with {@link #elementStack} by {@link #pushElement}/{@link #popElement} so
     * that {@link #isOnStack} is an O(1) lookup instead of an O(n) scan (which
     * made stray-end-tag handling quadratic on deeply nested input).
     */
    protected final Map<String, Integer> openTagCounts = new HashMap<>();

    /**
     * List of active formatting elements (a, b, i, strong, etc.) currently on
     * the stack. Maintained as elements are pushed and popped; consulted by the
     * balanced formatting-end reconstruction.
     */
    protected final LinkedList<String> activeFormattingElements = new LinkedList<>();

    /** Whether the HTML root element has been opened (or synthesized). */
    protected boolean htmlOpened = false;

    /** Whether the HEAD element has been closed (BODY/FRAMESET seen). */
    protected boolean headClosed = false;

    /** Whether the BODY (or FRAMESET) element has been opened. */
    protected boolean bodyOpened = false;

    /** Elements that close HEAD and open the body region when they appear. */
    protected static final Set<String> BODY_ELEMENTS = new HashSet<>();
    static {
        BODY_ELEMENTS.add("BODY");
        BODY_ELEMENTS.add("FRAMESET");
    }

    /** Elements that belong in HEAD (SCRIPT only while the body is not open). */
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
     * Implied-close table. Maps a start tag to the set of open elements it
     * implicitly closes when found at the top of the stack. Applied
     * repeatedly against the top of the stack before the start tag is pushed.
     */
    protected static final Map<String, Set<String>> IMPLIED_CLOSE = new HashMap<>();
    static {
        // Block-level containers that close an open paragraph. Includes the legacy blocks
        // (CENTER/DIR/LISTING/PLAINTEXT/SUMMARY/XMP) that also close <p> per HTML5.
        final Set<String> closesP = Set.of("P");
        for (final String t : new String[] { "P", "H1", "H2", "H3", "H4", "H5", "H6", "UL", "OL", "DL", "DIV", "BLOCKQUOTE", "PRE",
                "TABLE", "ADDRESS", "ARTICLE", "ASIDE", "DETAILS", "DIALOG", "FIELDSET", "FIGCAPTION", "FIGURE", "FOOTER", "FORM",
                "HEADER", "HGROUP", "HR", "MAIN", "MENU", "NAV", "SEARCH", "SECTION", "CENTER", "DIR", "LISTING", "PLAINTEXT", "SUMMARY",
                "XMP" }) {
            IMPLIED_CLOSE.put(t, closesP);
        }
        IMPLIED_CLOSE.put("LI", Set.of("LI", "P"));
        IMPLIED_CLOSE.put("DT", Set.of("DT", "DD", "P"));
        IMPLIED_CLOSE.put("DD", Set.of("DT", "DD", "P"));
        IMPLIED_CLOSE.put("OPTION", Set.of("OPTION"));
        IMPLIED_CLOSE.put("OPTGROUP", Set.of("OPTION", "OPTGROUP"));
        // Ruby annotation base/text markers behave like list items (siblings, not nesting).
        IMPLIED_CLOSE.put("RP", Set.of("RP", "RT"));
        IMPLIED_CLOSE.put("RT", Set.of("RP", "RT"));
        // Table content: a new row/section/colgroup also closes an open CAPTION.
        IMPLIED_CLOSE.put("TR", Set.of("TR", "TD", "TH", "CAPTION"));
        IMPLIED_CLOSE.put("TD", Set.of("TD", "TH"));
        IMPLIED_CLOSE.put("TH", Set.of("TD", "TH"));
        final Set<String> closesSection = Set.of("TR", "TD", "TH", "THEAD", "TBODY", "TFOOT", "CAPTION");
        IMPLIED_CLOSE.put("THEAD", closesSection);
        IMPLIED_CLOSE.put("TBODY", closesSection);
        IMPLIED_CLOSE.put("TFOOT", closesSection);
        IMPLIED_CLOSE.put("COLGROUP", Set.of("COLGROUP", "CAPTION"));
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
        openTagCounts.clear();
        activeFormattingElements.clear();
        htmlOpened = false;
        headClosed = false;
        bodyOpened = false;
        if (getContentHandler() != null) {
            getContentHandler().startDocument();
        }
    }

    @Override
    public void endDocument() throws SAXException {
        final ContentHandler handler = getContentHandler();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Ending document - closing " + elementStack.size() + " remaining open elements");
        }
        while (!elementStack.isEmpty()) {
            final ElementEntry entry = popElement();
            removeFormattingElement(entry.tagName);
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Auto-closing unclosed element at document end: " + entry.tagName);
            }
            if (handler != null) {
                handler.endElement(entry.uri, entry.localName, entry.qName);
            }
        }
        if (handler != null) {
            handler.endDocument();
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

        final String tagName = qName.toUpperCase(Locale.ROOT);

        // HTML: only one root; ignore any duplicate.
        if ("HTML".equals(tagName)) {
            if (htmlOpened) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Ignoring duplicate HTML start tag");
                }
                return;
            }
            htmlOpened = true;
            emitStart(uri, localName, qName, tagName, atts);
            return;
        }

        ensureDocumentInitialized();

        // HEAD: only meaningful before the body region.
        if ("HEAD".equals(tagName)) {
            if (bodyOpened || headClosed) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Ignoring HEAD start tag after body/head");
                }
                return;
            }
            emitStart(uri, localName, qName, tagName, atts);
            return;
        }

        // BODY / FRAMESET: close HEAD and enter the body region.
        if (BODY_ELEMENTS.contains(tagName)) {
            if (bodyOpened) {
                // Duplicate body region: HTML5 merges into the existing body
                // rather than nesting a second one; ignore the start tag.
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Ignoring duplicate " + tagName + " start tag");
                }
                return;
            }
            closeHead();
            bodyOpened = true;
            emitStart(uri, localName, qName, tagName, atts);
            return;
        }

        if (HEAD_ELEMENTS.contains(tagName) && !bodyOpened) {
            // Head content (SCRIPT counts as head content only before the body).
            if (!headClosed && !isOnStack("HEAD")) {
                final AttributesImpl empty = new AttributesImpl();
                handler.startElement("", "HEAD", "HEAD", empty);
                pushElement(new ElementEntry("", "HEAD", "HEAD", "HEAD", empty));
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Synthesized HEAD for head-content element " + tagName);
                }
            }
        } else {
            // Any other content forces the body region to open.
            ensureBodyOpen();
        }

        applyImpliedEndTags(tagName);
        emitStart(uri, localName, qName, tagName, atts);
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

        final String tagName = qName.toUpperCase(Locale.ROOT);

        // Stray end tag (never opened, void element, or already closed): ignore.
        if (!isOnStack(tagName)) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Ignoring stray end tag: " + tagName);
            }
            return;
        }

        // BODY / HTML close at end of document; only close what is above them so
        // that late content stays inside the body.
        if ("BODY".equals(tagName) || "HTML".equals(tagName)) {
            closeAbove(tagName);
            return;
        }

        // Formatting elements: balanced reconstruction of enclosing containers.
        if (HTMLElements.isFormattingElement(tagName)) {
            reconstructFormattingEnd(tagName);
            return;
        }

        // Standard element: close everything above, then close it.
        closeAbove(tagName);
        final ElementEntry target = popElement();
        removeFormattingElement(target.tagName);
        if ("HEAD".equals(tagName)) {
            // An explicit </head> ends the head region; later head-only content
            // is treated as body content instead of reopening a second HEAD.
            headClosed = true;
        }
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Popped element from stack: " + tagName + " (stack depth: " + elementStack.size() + ")");
        }
        handler.endElement(uri, localName, qName);
    }

    /**
     * Emits a start element and, unless it is a void element, pushes it onto the
     * stack (recording a copy of its attributes for possible reopening).
     */
    private void emitStart(final String uri, final String localName, final String qName, final String tagName, final Attributes atts)
            throws SAXException {
        final ContentHandler handler = getContentHandler();
        handler.startElement(uri, localName, qName, atts);
        if (VOID_ELEMENTS.contains(tagName)) {
            // Void elements are always empty; emit their end immediately so the
            // stream stays balanced. The upstream scanner's own (redundant) void
            // end tag, and any stray void end tag, are ignored in endElement().
            handler.endElement(uri, localName, qName);
            return;
        }
        pushElement(new ElementEntry(uri, localName, qName, tagName, atts));
        if (HTMLElements.isFormattingElement(tagName)) {
            addFormattingElement(tagName);
        }
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Pushed element onto stack: " + tagName + " (stack depth: " + elementStack.size() + ")");
        }
    }

    /**
     * Applies implied end tags for the given start tag: while the top of the
     * stack is an element that {@code tagName} implicitly closes, pop and close
     * it.
     *
     * @param tagName The incoming start tag (upper-cased)
     * @throws SAXException If an error occurs
     */
    protected void applyImpliedEndTags(final String tagName) throws SAXException {
        final Set<String> closes = IMPLIED_CLOSE.get(tagName);
        if (closes == null) {
            return;
        }
        final ContentHandler handler = getContentHandler();
        while (!elementStack.isEmpty() && closes.contains(elementStack.peek().tagName)) {
            final ElementEntry entry = popElement();
            removeFormattingElement(entry.tagName);
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Implied close of " + entry.tagName + " before " + tagName);
            }
            handler.endElement(entry.uri, entry.localName, entry.qName);
        }
    }

    /**
     * Closes (emits end for and pops) every element above the innermost
     * occurrence of {@code tagName}, leaving {@code tagName} itself on the stack.
     *
     * @param tagName The target tag name (upper-cased)
     * @throws SAXException If an error occurs
     */
    protected void closeAbove(final String tagName) throws SAXException {
        final ContentHandler handler = getContentHandler();
        while (!elementStack.isEmpty() && !elementStack.peek().tagName.equals(tagName)) {
            final ElementEntry entry = popElement();
            removeFormattingElement(entry.tagName);
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Auto-closing element: " + entry.tagName);
            }
            handler.endElement(entry.uri, entry.localName, entry.qName);
        }
    }

    /**
     * Balanced reconstruction for a misnested formatting end tag.
     *
     * <p>
     * Everything above the formatting element {@code F} is closed and, for the
     * non-formatting containers among them, reopened after {@code F} is closed
     * (preserving their attributes). Formatting elements stay closed
     * ("one-shot" formatting), so formatting does not leak past its end tag.
     * This keeps the event stream balanced.
     * </p>
     *
     * @param tagName The formatting element being closed (upper-cased)
     * @throws SAXException If an error occurs
     */
    protected void reconstructFormattingEnd(final String tagName) throws SAXException {
        final ContentHandler handler = getContentHandler();
        // Pop entries above F, collecting non-formatting containers in pop order (top->bottom).
        // Appending is O(1); the reopen loop below walks the list in reverse to restore the
        // original outer->inner nesting, avoiding the O(n^2) cost of repeated head insertion.
        final List<ElementEntry> reopen = new ArrayList<>();
        while (!elementStack.peek().tagName.equals(tagName)) {
            final ElementEntry entry = popElement();
            removeFormattingElement(entry.tagName);
            handler.endElement(entry.uri, entry.localName, entry.qName);
            if (!HTMLElements.isFormattingElement(entry.tagName)) {
                reopen.add(entry);
            }
        }
        // Close the formatting element itself.
        final ElementEntry formatting = popElement();
        removeFormattingElement(formatting.tagName);
        handler.endElement(formatting.uri, formatting.localName, formatting.qName);
        // Reopen the non-formatting containers (outermost first) with their original attributes.
        for (int k = reopen.size() - 1; k >= 0; k--) {
            final ElementEntry entry = reopen.get(k);
            handler.startElement(entry.uri, entry.localName, entry.qName, entry.attrs);
            pushElement(entry);
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Reopened container after formatting end: " + entry.tagName);
            }
        }
    }

    /**
     * Closes an element (and everything above it) if it is currently open.
     *
     * @param tagName The tag name to close (upper-cased)
     * @throws SAXException If an error occurs
     */
    protected void closeElement(final String tagName) throws SAXException {
        final ContentHandler handler = getContentHandler();
        if (handler == null || !isOnStack(tagName)) {
            return;
        }
        while (!elementStack.isEmpty()) {
            final ElementEntry entry = popElement();
            removeFormattingElement(entry.tagName);
            handler.endElement(entry.uri, entry.localName, entry.qName);
            if (entry.tagName.equals(tagName)) {
                break;
            }
        }
    }

    /**
     * Closes HEAD (and any open head content) if present, and marks the head
     * region as closed.
     *
     * @throws SAXException If an error occurs
     */
    protected void closeHead() throws SAXException {
        if (isOnStack("HEAD")) {
            closeElement("HEAD");
        }
        headClosed = true;
    }

    /**
     * Synthesizes and opens a BODY element if the body region is not yet open.
     *
     * @throws SAXException If an error occurs
     */
    protected void ensureBodyOpen() throws SAXException {
        if (bodyOpened) {
            return;
        }
        closeHead();
        final ContentHandler handler = getContentHandler();
        final AttributesImpl empty = new AttributesImpl();
        handler.startElement("", "BODY", "BODY", empty);
        pushElement(new ElementEntry("", "BODY", "BODY", "BODY", empty));
        bodyOpened = true;
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Synthesized BODY");
        }
    }

    /**
     * Returns whether an element with the given (upper-cased) name is currently
     * on the stack.
     *
     * @param tagName The tag name to look for
     * @return true if it is open
     */
    protected boolean isOnStack(final String tagName) {
        return openTagCounts.getOrDefault(tagName, 0) > 0;
    }

    /**
     * Pushes an entry onto the element stack, keeping {@link #openTagCounts} in
     * sync. All stack pushes must go through this helper so that
     * {@link #isOnStack} stays correct.
     *
     * @param entry The entry to push
     */
    private void pushElement(final ElementEntry entry) {
        elementStack.push(entry);
        openTagCounts.merge(entry.tagName, 1, Integer::sum);
    }

    /**
     * Pops the top entry off the element stack, keeping {@link #openTagCounts}
     * in sync. All stack pops must go through this helper so that
     * {@link #isOnStack} stays correct.
     *
     * @return The popped entry
     */
    private ElementEntry popElement() {
        final ElementEntry entry = elementStack.pop();
        final Integer count = openTagCounts.get(entry.tagName);
        if (count != null) {
            if (count <= 1) {
                openTagCounts.remove(entry.tagName);
            } else {
                openTagCounts.put(entry.tagName, count - 1);
            }
        }
        return entry;
    }

    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        final ContentHandler handler = getContentHandler();
        if (handler == null) {
            return;
        }
        if (!bodyOpened && containsNonWhitespace(ch, start, length)) {
            ensureDocumentInitialized();
            // Text that is the content of a head element (e.g. TITLE, STYLE, SCRIPT)
            // belongs to that element and must not force the body open.
            final ElementEntry top = elementStack.peek();
            if (top == null || !HEAD_ELEMENTS.contains(top.tagName)) {
                ensureBodyOpen();
            }
        }
        handler.characters(ch, start, length);
    }

    /**
     * Returns whether the given character run contains any non-whitespace
     * character. A byte-order mark (U+FEFF) is treated as insignificant so that
     * a leading BOM does not, by itself, force a body to be synthesized.
     */
    private static boolean containsNonWhitespace(final char[] ch, final int start, final int length) {
        for (int i = start; i < start + length; i++) {
            final char c = ch[i];
            if (c != '\uFEFF' && !Character.isWhitespace(c)) {
                return true;
            }
        }
        return false;
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
        // A comment does not force the document structure to be initialized: a
        // comment before <html> belongs at the document level.
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
        if (!htmlOpened) {
            htmlOpened = true;
            final ContentHandler handler = getContentHandler();
            if (handler != null) {
                final AttributesImpl empty = new AttributesImpl();
                handler.startElement("", "HTML", "HTML", empty);
                pushElement(new ElementEntry("", "HTML", "HTML", "HTML", empty));
            }
        }
    }

    //
    // Active formatting element bookkeeping
    //

    /**
     * Adds a formatting element to the active formatting elements list.
     *
     * @param tagName The element tag name (upper-cased)
     */
    protected void addFormattingElement(final String tagName) {
        activeFormattingElements.remove(tagName);
        activeFormattingElements.add(tagName);
    }

    /**
     * Removes a formatting element from the active formatting elements list.
     *
     * @param tagName The element tag name (upper-cased)
     * @return true if the element was found and removed
     */
    protected boolean removeFormattingElement(final String tagName) {
        return activeFormattingElements.remove(tagName);
    }

} // class HTMLTagBalancerFilter
