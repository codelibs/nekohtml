/* (C) Copyright 2002, Andy Clark. All rights reserved. */

package org.cyberneko.html;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.apache.xerces.util.URI;
import org.apache.xerces.util.XMLAttributesImpl;
import org.apache.xerces.util.XMLStringBuffer;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLDocumentScanner;
import org.apache.xerces.xni.parser.XMLInputSource;

/**
 * A simple HTML scanner. This scanner makes no attempt to balance tags
 * or fix other problems in the source document -- it just scans what it
 * can and generates XNI document "events", ignoring errors of all kinds.
 *
 * @author Andy Clark
 */
public class HTMLScanner 
    implements XMLDocumentScanner {

    //
    // Constants
    //

    // states

    /** State: content. */
    protected static final short STATE_CONTENT = 0;

    /** State: markup bracket. */
    protected static final short STATE_MARKUP_BRACKET = 1;

    /** State: start document. */
    protected static final short STATE_START_DOCUMENT = 10;

    /** State: end document. */
    protected static final short STATE_END_DOCUMENT = 11;

    // defaults

    /** Default buffer size. */
    protected static final int DEFAULT_BUFFER_SIZE = 2048;

    /** Default IANA encoding. */
    protected static final String DEFAULT_IANA_ENCODING = "WINDOWS-1252";

    /** Default Java encoding. */
    protected static final String DEFAULT_JAVA_ENCODING = "Cp1252";

    // debugging

    /** Set to true to debug changes in the scanner. */
    private static final boolean DEBUG_SCANNER = false;

    /** Set to true to debug changes in the scanner state. */
    private static final boolean DEBUG_SCANNER_STATE = false;

    /** Set to true to debug the buffer. */
    private static final boolean DEBUG_BUFFER = false;

    /** Set to true to debug callbacks. */
    protected static final boolean DEBUG_CALLBACKS = false;

    //
    // Data
    //

    // state

    /** The playback byte stream. */
    protected PlaybackInputStream fByteStream;

    /** The character stream. */
    protected Reader fCharStream;

    /** The current scanner. */
    protected Scanner fScanner;

    /** The current scanner state. */
    protected short fScannerState;

    /** The document handler. */
    protected XMLDocumentHandler fDocumentHandler;

    /** Auto-detected IANA encoding. */
    protected String fIANAEncoding;

    /** Element count. */
    protected int fElementCount;

    /** Element depth. */
    protected int fElementDepth;

    // scanners

    /** Content scanner. */
    protected Scanner fContentScanner = new ContentScanner();

    /** 
     * Special scanner used for elements whose content needs to be scanned 
     * as plain text, ignoring markup such as elements and entity references.
     * For example: &lt;SCRIPT&gt; and &lt;COMMENT&gt;.
     */
    protected SpecialScanner fSpecialScanner = new SpecialScanner();

    // buffering

    /** Character buffer. */
    protected final char[] fCharBuffer = new char[DEFAULT_BUFFER_SIZE];

    /** Offset into character buffer. */
    protected int fCharOffset = 0;

    /** Length of characters read into character buffer. */
    protected int fCharLength = 0;

    // temp vars

    /** String. */
    protected final XMLString fString = new XMLString();

    /** String buffer. */
    protected final XMLStringBuffer fStringBuffer = new XMLStringBuffer(1024);

    //
    // XMLDocumentScanner methods
    //

    /** Sets the input source. */
    public void setInputSource(XMLInputSource source) throws IOException {
        fElementCount = 0;
        fElementDepth = -1;
        fByteStream = null;
        fIANAEncoding = DEFAULT_IANA_ENCODING;
        Reader reader = source.getCharacterStream();
        if (reader == null) {
            InputStream inputStream = source.getByteStream();
            if (inputStream == null) {
                String systemId = source.getSystemId();
                String baseSystemId = source.getBaseSystemId();
                String expandedSystemId = expandSystemId(systemId, baseSystemId);
                URL url = new URL(expandedSystemId);
                inputStream = url.openStream();
            }
            fByteStream = new PlaybackInputStream(inputStream);
            String[] encodings = new String[2];
            fByteStream.detectEncoding(encodings);
            fIANAEncoding = encodings[0] != null ? encodings[0] : DEFAULT_IANA_ENCODING;
            if (encodings[1] == null) {
                encodings[1] = DEFAULT_JAVA_ENCODING;
            }
            reader = new InputStreamReader(fByteStream, encodings[1]);
        }
        fCharStream = reader;
        setScanner(fContentScanner);
        setScannerState(STATE_START_DOCUMENT);
        fCharOffset = 0;
        fCharLength = 0;
    } // setInputSource(XMLInputSource)

    /** Scans the document. */
    public boolean scanDocument(boolean complete) throws XNIException, IOException {
        do {
            if (!fScanner.scan(complete)) {
                return false;
            }
        } while (complete);
        return true;
    } // scanDocument(boolean):boolean

    public void setDocumentHandler(XMLDocumentHandler handler) {
        fDocumentHandler = handler;
    } // setDocumentHandler(XMLDocumentHandler)

    //
    // Protected static methods
    //

    /**
     * Expands a system id and returns the system id as a URI, if
     * it can be expanded. A return value of null means that the
     * identifier is already expanded. An exception thrown
     * indicates a failure to expand the id.
     *
     * @param systemId The systemId to be expanded.
     *
     * @return Returns the URI string representing the expanded system
     *         identifier. A null value indicates that the given
     *         system identifier is already expanded.
     *
     */
    public static String expandSystemId(String systemId, String baseSystemId) {

        // check for bad parameters id
        if (systemId == null || systemId.length() == 0) {
            return systemId;
        }
        // if id already expanded, return
        try {
            URI uri = new URI(systemId);
            if (uri != null) {
                return systemId;
            }
        }
        catch (URI.MalformedURIException e) {
            // continue on...
        }
        // normalize id
        String id = fixURI(systemId);

        // normalize base
        URI base = null;
        URI uri = null;
        try {
            if (baseSystemId == null || baseSystemId.length() == 0 ||
                baseSystemId.equals(systemId)) {
                String dir;
                try {
                    dir = fixURI(System.getProperty("user.dir"));
                }
                catch (SecurityException se) {
                    dir = "";
                }
                if (!dir.endsWith("/")) {
                    dir = dir + "/";
                }
                base = new URI("file", "", dir, null, null);
            }
            else {
                try {
                    base = new URI(fixURI(baseSystemId));
                }
                catch (URI.MalformedURIException e) {
                    String dir;
                    try {
                        dir = fixURI(System.getProperty("user.dir"));
                    }
                    catch (SecurityException se) {
                        dir = "";
                    }
                    if (baseSystemId.indexOf(':') != -1) {
                        // for xml schemas we might have baseURI with
                        // a specified drive
                        base = new URI("file", "", fixURI(baseSystemId), null, null);
                    }
                    else {
                        if (!dir.endsWith("/")) {
                            dir = dir + "/";
                        }
                        dir = dir + fixURI(baseSystemId);
                        base = new URI("file", "", dir, null, null);
                    }
                }
             }
             // expand id
             uri = new URI(base, id);
        }
        catch (Exception e) {
            // let it go through

        }

        if (uri == null) {
            return systemId;
        }
        return uri.toString();

    } // expandSystemId(String,String):String

    /**
     * Fixes a platform dependent filename to standard URI form.
     *
     * @param str The string to fix.
     *
     * @return Returns the fixed URI string.
     */
    protected static String fixURI(String str) {

        // handle platform dependent strings
        str = str.replace(java.io.File.separatorChar, '/');

        // Windows fix
        if (str.length() >= 2) {
            char ch1 = str.charAt(1);
            // change "C:blah" to "/C:blah"
            if (ch1 == ':') {
                char ch0 = Character.toUpperCase(str.charAt(0));
                if (ch0 >= 'A' && ch0 <= 'Z') {
                    str = "/" + str;
                }
            }
            // change "//blah" to "file://blah"
            else if (ch1 == '/' && str.charAt(0) == '/') {
                str = "file:" + str;
            }
        }

        // done
        return str;

    } // fixURI(String):String

    //
    // Protected methods
    //

    // i/o

    /** Reads a single character. */
    protected int read() throws IOException {
        if (fCharOffset == fCharLength) {
            if (load(0) == -1) {
                return -1;
            }
        }
        return fCharBuffer[fCharOffset++];
    } // read():int

    /** 
     * Loads a new chunk of data into the buffer and returns the number of
     * characters loaded or -1 if no additional characters were loaded.
     *
     * @param offset The offset at which new characters should be loaded.
     */
    protected int load(int offset) throws IOException {
        if (DEBUG_BUFFER) { 
            System.out.print("(load: ");
            printBuffer();
            System.out.println();
        }
        int count = fCharStream.read(fCharBuffer, offset, fCharBuffer.length - offset);
        fCharLength = count != -1 ? count + offset : offset;
        fCharOffset = offset;
        if (DEBUG_BUFFER) { 
            System.out.print(")load: ");
            printBuffer();
            System.out.println();
        }
        return count;
    } // load():int

    // debugging

    /** Sets the scanner. */
    protected void setScanner(Scanner scanner) {
        fScanner = scanner;
        if (DEBUG_SCANNER) {
            System.out.print("$$$ setScanner(");
            System.out.print(scanner!=null?scanner.getClass().getName():"null");
            System.out.println(");");
        }
    } // setScanner(Scanner)
    
    /** Sets the scanner state. */
    protected void setScannerState(short state) {
        fScannerState = state;
        if (DEBUG_SCANNER_STATE) {
            System.out.print("$$$ setScannerState(");
            switch (fScannerState) {
                case STATE_CONTENT: { System.out.print("STATE_CONTENT"); break; }
                case STATE_MARKUP_BRACKET: { System.out.print("STATE_MARKUP_BRACKET"); break; }
                case STATE_START_DOCUMENT: { System.out.print("STATE_START_DOCUMENT"); break; }
                case STATE_END_DOCUMENT: { System.out.print("STATE_END_DOCUMENT"); break; }
            }
            System.out.println(");");
        }
    } // setScannerState(short)

    // scanning

    /** Scans a name. */
    protected String scanName() throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(scanName: ");
            printBuffer();
            System.out.println();
        }
        if (fCharOffset == fCharLength) {
            if (load(0) == -1) {
                if (DEBUG_BUFFER) {
                    System.out.print(")scanName: ");
                    printBuffer();
                    System.out.println(" -> null");
                }
                return null;
            }
        }
        int offset = fCharOffset;
        while (true) {
            while (fCharOffset < fCharLength) {
                char c = fCharBuffer[fCharOffset];
                if (!Character.isLetterOrDigit(c) &&
                    !(c == '-' || c == '.' || c == ':')) {
                    break;
                }
                fCharOffset++;
            }
            if (fCharOffset == fCharLength) {
                int length = fCharLength - offset;
                System.arraycopy(fCharBuffer, offset, fCharBuffer, 0, length);
                load(length);
                offset = 0;
            }
            else {
                break;
            }
        }
        int length = fCharOffset - offset;
        String name = length > 0 ? new String(fCharBuffer, offset, length) : null;
        if (DEBUG_BUFFER) {
            System.out.print(")scanName: ");
            printBuffer();
            System.out.print(" -> \"");
            System.out.print(name);
            System.out.println('"');
        }
        return name;
    } // scanName():String

    /** Skips markup. */
    protected void skip() throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(skip: ");
            printBuffer();
            System.out.println();
        }
        int depth = 1;
        OUTER: while (true) {
            if (fCharOffset == fCharLength) {
                if (load(0) == -1) {
                    break OUTER;
                }
            }
            while (fCharOffset < fCharLength) {
                char c = fCharBuffer[fCharOffset++];
                if (c == '<') {
                    depth++;
                }
                else if (c == '>') {
                    depth--;
                    if (depth == 0) {
                        break OUTER;
                    }
                }
            }
        }
        if (DEBUG_BUFFER) {
            System.out.print(")skip: ");
            printBuffer();
            System.out.println();
        }
    } // skip()

    //
    // Private methods
    //

    /** Prints the contents of the character buffer to standard out. */
    private void printBuffer() {
        if (DEBUG_BUFFER) {
            System.out.print('[');
            System.out.print(fCharLength);
            System.out.print(' ');
            System.out.print(fCharOffset);
            if (fCharLength > 0) {
                System.out.print(" \"");
                for (int i = 0; i < fCharLength; i++) {
                    if (i == fCharOffset) {
                        System.out.print('^');
                    }
                    char c = fCharBuffer[i];
                    switch (c) {
                        case '\r': {
                            System.out.print("\\r");
                            break;
                        }
                        case '\n': {
                            System.out.print("\\n");
                            break;
                        }
                        case '\t': {
                            System.out.print("\\t");
                            break;
                        }
                        case '"': {
                            System.out.print("\\\"");
                            break;
                        }
                        default: {
                            System.out.print(c);
                        }
                    }
                }
                if (fCharOffset == fCharLength) {
                    System.out.print('^');
                }
                System.out.print('"');
            }
            System.out.print(']');
        }
    } // printBuffer()

    //
    // Interfaces
    //

    /**
     * Basic scanner interface.
     *
     * @author Andy Clark
     */
    public interface Scanner {

        //
        // Scanner methods
        //

        /** 
         * Scans part of the document. This interface allows scanning to
         * be performed in a pulling manner.
         *
         * @param complete True if the scanner should not return until
         *                 scanning is complete.
         *
         * @returns True if additional scanning is required.
         *
         * @throws IOException Thrown if I/O error occurs.
         */
        public boolean scan(boolean complete) throws IOException;

    } // interface Scanner

    //
    // Classes
    //

    /**
     * The primary HTML document scanner.
     *
     * @author Andy Clark
     */
    public class ContentScanner 
        implements Scanner {

        //
        // Data
        //

        // temp vars

        /** A qualified name. */
        private final QName fQName = new QName();

        /** Attributes. */
        private final XMLAttributesImpl fAttributes = new XMLAttributesImpl();

        //
        // Scanner methods
        //

        /** Scan. */
        public boolean scan(boolean complete) throws IOException {
            boolean next;
            do {
                try {
                    next = false;
                    switch (fScannerState) {
                        case STATE_CONTENT: {
                            int c = read();
                            if (c == '<') {
                                setScannerState(STATE_MARKUP_BRACKET);
                                next = true;
                            }
                            else if (c == '&') {
                                char ce = scanEntityRef();
                                if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                                    fStringBuffer.clear();
                                    fStringBuffer.append(ce);
                                    if (DEBUG_CALLBACKS) {
                                        System.out.println("characters("+fStringBuffer+')');
                                    }
                                    fDocumentHandler.characters(fStringBuffer, null);
                                }
                            }
                            else if (c == -1) {
                                setScannerState(STATE_END_DOCUMENT);
                                next = true;
                            }
                            else {
                                fCharOffset--;
                                scanCharacters();
                            }
                            break;
                        }
                        case STATE_MARKUP_BRACKET: {
                            int c = read();
                            if (c == '!') {
                                if (read() == '-' && read() == '-') {
                                    scanComment();
                                }
                                else {
                                    skip();
                                }
                            }
                            else if (c == '?') {
                                scanPI();
                            }
                            else if (c == '/') {
                                scanEndElement();
                            }
                            else if (c == -1) {
                                setScannerState(STATE_END_DOCUMENT);
                                continue;
                            }
                            else {
                                fCharOffset--;
                                fElementCount++;
                                String ename = scanStartElement();
                                if (ename != null && (ename.equals("SCRIPT") || ename.equals("COMMENT"))) {
                                    setScanner(fSpecialScanner.setElementName(ename));
                                    setScannerState(STATE_CONTENT);
                                    return true;
                                }
                            }
                            setScannerState(STATE_CONTENT);
                            break;
                        }
                        case STATE_START_DOCUMENT: {
                            if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                                if (DEBUG_CALLBACKS) {
                                    System.out.println("startDocument()");
                                }
                                fDocumentHandler.startDocument(null, fIANAEncoding, null);
                            }
                            setScannerState(STATE_CONTENT);
                            break;
                        }
                        case STATE_END_DOCUMENT: {
                            if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                                if (DEBUG_CALLBACKS) {
                                    System.out.println("endDocument()");
                                }
                                fDocumentHandler.endDocument(null);
                            }
                            return false;
                        }
                        default: {
                            throw new RuntimeException("unknown scanner state: "+fScannerState);
                        }
                    }
                }
                catch (EOFException e) {
                    setScannerState(STATE_END_DOCUMENT);
                    next = true;
                }
            } while (next || complete);
            return true;
        } // scan(boolean):boolean

        //
        // Protected methods
        //

        /** Scans an entity reference. */
        protected char scanEntityRef() throws IOException {
            fStringBuffer.clear();
            while (true) {
                int c = read();
                if (c == '<' || c == '&' || c == '>' || Character.isSpace((char)c)) {
                    fCharOffset--;
                    break;
                }
                if (c == ';') {
                    break;
                }
                if (c == -1) {
                    throw new EOFException();
                }
                fStringBuffer.append((char)c);
            }
            if (fStringBuffer.length == 0) {
                return '&';
            }

            String name = fStringBuffer.toString();
            if (name.startsWith("#")) {
                int value = -1;
                try {
                    if (name.startsWith("#x")) {
                        value = Integer.parseInt(name.substring(2), 16);
                    }
                    else {
                        value = Integer.parseInt(name.substring(1));
                    }
                }
                catch (NumberFormatException e) {
                    // REVISIT: What should be done with this?
                }
                return (char)value;
            }

            int c = HTMLEntities.get(name);
            return (char)c;
        
        } // scanEntityRef():char

        /** Scans characters. */
        protected void scanCharacters() throws IOException {
            if (DEBUG_BUFFER) {
                System.out.print("(scanCharacters: ");
                printBuffer();
                System.out.println();
            }
            if (fCharOffset == fCharLength) {
                if (load(0) == -1) {
                    if (DEBUG_BUFFER) {
                        System.out.print(")scanCharacters: ");
                        printBuffer();
                        System.out.println();
                    }
                    return;
                }
            }
            char c = fCharBuffer[fCharOffset];
            int newlines = 0;
            int offset = fCharOffset;
            if (c == '\n' || c == '\r') {
                do {
                    c = fCharBuffer[fCharOffset++];
                    if (c == '\r') {
                        newlines++;
                        if (fCharOffset == fCharLength) {
                            offset = 0;
                            fCharOffset = newlines;
                            if (load(newlines) == -1) {
                                break;
                            }
                        }
                        if (fCharBuffer[fCharOffset] == '\n') {
                            fCharOffset++;
                            offset++;
                        }
                        else {
                            newlines++;
                        }
                    }
                    else if (c == '\n') {
                        newlines++;
                        if (fCharOffset == fCharLength) {
                            offset = 0;
                            fCharOffset = newlines;
                            if (load(newlines) == -1) {
                                break;
                            }
                        }
                    }
                    else {
                        fCharOffset--;
                        break;
                    }
                } while (fCharOffset < fCharLength - 1);
                for (int i = offset; i < fCharOffset; i++) {
                    fCharBuffer[i] = '\n';
                }
            }
            while (fCharOffset < fCharLength) {
                c = fCharBuffer[fCharOffset];
                if (c == '<' || c == '&' || c == '\n' || c == '\r') {
                    break;
                }
                fCharOffset++;
            }
            if (fCharOffset > offset && 
                fDocumentHandler != null && fElementCount >= fElementDepth) {
                fString.setValues(fCharBuffer, offset, fCharOffset - offset);
                if (DEBUG_CALLBACKS) {
                    System.out.println("characters("+fString+")");
                }
                fDocumentHandler.characters(fString, null);
            }
            if (DEBUG_BUFFER) {
                System.out.print(")scanCharacters: ");
                printBuffer();
                System.out.println();
            }
        } // scanCharacters(int)

        /** Scans a comment. */
        protected void scanComment() throws IOException {
            if (DEBUG_BUFFER) {
                System.out.print("(scanComment: ");
                printBuffer();
                System.out.println();
            }
            fStringBuffer.clear();
            while (true) {
                int c = read();
                if (c == '-') {
                    int count = 1;
                    while (true) {
                        c = read();
                        if (c == '-') {
                            count++;
                            continue;
                        }
                        break;
                    }
                    if (count < 2) {
                        fStringBuffer.append('-');
                        fCharOffset--;
                        continue;
                    }
                    if (c != '>') {
                        for (int i = 0; i < count; i++) {
                            fStringBuffer.append('-');
                        }
                        fCharOffset--;
                        continue;
                    }
                    for (int i = 0; i < count - 2; i++) {
                        fStringBuffer.append('-');
                    }
                    break;
                }
                else if (c == -1) {
                    throw new EOFException();
                }
                fStringBuffer.append((char)c);
            }
            if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                if (DEBUG_CALLBACKS) {
                    System.out.println("comment("+fStringBuffer+")");
                }
                fDocumentHandler.comment(fStringBuffer, null);
            }
            if (DEBUG_BUFFER) {
                System.out.print(")scanComment: ");
                printBuffer();
                System.out.println();
            }
        } // scanComment()

        /** Scans a processing instruction. */
        protected void scanPI() throws IOException {
            if (DEBUG_BUFFER) {
                System.out.print("(scanPI: ");
                printBuffer();
                System.out.println();
            }
            skip();
            if (DEBUG_BUFFER) {
                System.out.print(")scanPI: ");
                printBuffer();
                System.out.println();
            }
        } // scanPI()

        /** Scans a start element. */
        protected String scanStartElement() throws IOException {
            String ename = scanName();
            if (ename == null) {
                skip();
                return null;
            }
            ename = ename.toUpperCase();
            fAttributes.removeAllAttributes();
            boolean print = false;
            while (scanAttribute(fAttributes)) {
                // do nothing
            }
            if (fByteStream != null && fElementDepth == -1) {
                if (ename.equals("META")) {
                    String httpEquiv = fAttributes.getValue("http-equiv");
                    if (httpEquiv != null && httpEquiv.equalsIgnoreCase("content-type")) {
                        String content = fAttributes.getValue("content");
                        int index1 = content.indexOf("charset=");
                        if (index1 != -1) {
                            int index2 = content.indexOf(';', index1);
                            String charset = index2 != -1 ? content.substring(index1+8, index2) : content.substring(index1+8);
                            try {
                                fCharStream = new InputStreamReader(fByteStream, charset);
                                fByteStream.playback();
                                fElementDepth = fElementCount;
                                fElementCount = 0;
                                fCharOffset = fCharLength = 0;
                            }
                            catch (UnsupportedEncodingException e) {
                                // REVISIT: report error
                                System.err.println("!!! UNSUPPORTED ENCODING !!!");
                                // NOTE: If the encoding change doesn't work, 
                                //       then there's no point in continuing to 
                                //       buffer the input stream.
                                fByteStream.clear();
                            }
                        }
                    }
                }
                else if (ename.equals("BODY")) {
                    fByteStream.clear();
                }
                else {
                     HTMLElements.Element element = HTMLElements.getElement(ename);
                     if (element.parent != null) {
                         String name = element.parent instanceof String
                                     ? (String)element.parent
                                     : ((String[])element.parent)[0];
                         if (name.equals("BODY")) {
                             fByteStream.clear();
                         }
                     }
                }
            }
            if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                fQName.setValues(null, ename, ename, null);
                if (DEBUG_CALLBACKS) {
                    System.out.println("startElement("+fQName+','+fAttributes+")");
                }
                fDocumentHandler.startElement(fQName, fAttributes, null);
            }
            return ename;
        } // scanStartElement():ename

        /** Scans an attribute. */
        protected boolean scanAttribute(XMLAttributesImpl attributes) throws IOException {
            int c = read();
            while (Character.isSpace((char)c)) {
                c = read();
            }
            if (c == -1) {
                throw new EOFException();
            }
            if (c == '>') {
                return false;
            }
            fCharOffset--;
            String aname = scanName();
            if (aname == null) {
                skip();
                return false;
            }
            aname = aname.toLowerCase();
            c = read();
            if (c == '/' || c == '>') {
                fQName.setValues(null, aname, aname, null);
                attributes.addAttribute(fQName, "CDATA", "");
                if (c == '/') {
                    skip();
                }
                return false;
            }
            if (Character.isSpace((char)c)) {
                do {
                    c = read();
                    if (c == -1) {
                        throw new EOFException();
                    }
                }
                while (Character.isSpace((char)c));
            }
            if (c == '/' || c == '>') {
                if (c == '/') {
                    skip();
                }
                fQName.setValues(null, aname, aname, null);
                attributes.addAttribute(fQName, "CDATA", "");
                return false;
            }
            if (c == '=') {
                c = read();
                if (Character.isSpace((char)c)) {
                    do {
                        c = read();
                        if (c == -1) {
                            throw new EOFException();
                        }
                    }
                    while (Character.isSpace((char)c));
                }
                if (c == '/' || c == '>') {
                    fQName.setValues(null, aname, aname, null);
                    attributes.addAttribute(fQName, "CDATA", "");
                    if (c == '/') {
                        skip();
                    }
                    return false;
                }
                if (c != '\'' && c != '"') {
                    fStringBuffer.clear();
                    fStringBuffer.append((char)c);
                    while (true) {
                        c = read();
                        if (Character.isSpace((char)c) || c == '/' || c == '>') {
                            if (c == '/') {
                                c = read();
                                if (c != '>') {
                                    fStringBuffer.append('/');
                                }
                            }
                            else {
                                fCharOffset--;
                                break;
                            }
                        }
                        if (c == -1) {
                            throw new EOFException();
                        }
                        fStringBuffer.append((char)c);
                    }
                    fQName.setValues(null, aname, aname, null);
                    String avalue = fStringBuffer.toString();
                    attributes.addAttribute(fQName, "CDATA", avalue);
                    return true;
                }
                char quote = (char)c;
                fStringBuffer.clear();
                do {
                    c = read();
                    if (c == -1) {
                        throw new EOFException();
                    }
                    if (c == '&') {
                        char ce = scanEntityRef();
                        fStringBuffer.append(ce);
                    }
                    else if (c != quote) {
                        fStringBuffer.append((char)c);
                    }
                } while (c != quote);
                fQName.setValues(null, aname, aname, null);
                String avalue = fStringBuffer.toString();
                attributes.addAttribute(fQName, "CDATA", avalue);
            }
            else {
                fQName.setValues(null, aname, aname, null);
                attributes.addAttribute(fQName, "CDATA", "");
                fCharOffset--;
            }
            return true;
        } // scanAttribute(XMLAttributesImpl):boolean

        /** Scans an end element. */
        protected void scanEndElement() throws IOException {
            String ename = scanName();
            skip();
            if (ename != null) {
                ename = ename.toUpperCase();
                if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                    fQName.setValues(null, ename, ename, null);
                    if (DEBUG_CALLBACKS) {
                        System.out.println("endElement("+fQName+")");
                    }
                    fDocumentHandler.endElement(fQName, null);
                }
            }
        } // scanEndElement()

    } // class ContentScanner

    /**
     * Special scanner used for elements whose content needs to be scanned 
     * as plain text, ignoring markup such as elements and entity references.
     * For example: &lt;SCRIPT&gt; and &lt;COMMENT&gt;.
     *
     * @author Andy Clark
     */
    public class SpecialScanner
        implements Scanner {

        //
        // Data
        //

        /** Name of element whose content needs to be scanned as text. */
        protected String fElementName;

        // temp vars

        /** A qualified name. */
        private final QName fQName = new QName();

        /** A string buffer. */
        private final XMLStringBuffer fStringBuffer = new XMLStringBuffer();

        //
        // Public methods
        //

        /** Sets the element name. */
        public Scanner setElementName(String ename) {
            fElementName = ename;
            return this;
        } // setElementName(String):Scanner

        //
        // Scanner methods
        //

        /** Scan. */
        public boolean scan(boolean complete) throws IOException {
            boolean next;
            do {
                try {
                    next = false;
                    switch (fScannerState) {
                        case STATE_CONTENT: {
                            int c = read();
                            if (c == '<') {
                                c = read();
                                if (c == '/') {
                                    String ename = scanName();
                                    if (ename != null) {
                                        if (ename.equalsIgnoreCase(fElementName)) {
                                            skip();
                                            String ENAME = ename.toUpperCase();
                                            fQName.setValues(null, ENAME, ENAME, null);
                                            if (DEBUG_CALLBACKS) {
                                                System.out.println("endElement("+fQName+")");
                                            }
                                            fDocumentHandler.endElement(fQName, null);
                                            setScanner(fContentScanner);
                                            setScannerState(STATE_CONTENT);
                                            return true;
                                        }
                                        fStringBuffer.clear();
                                        fStringBuffer.append("</");
                                        fStringBuffer.append(ename);
                                    }
                                    else {
                                        fStringBuffer.clear();
                                        fStringBuffer.append("</");
                                    }
                                }
                                else {
                                    fStringBuffer.clear();
                                    fStringBuffer.append('<');
                                    fStringBuffer.append((char)c);
                                }
                            }
                            else if (c == -1) {
                                throw new EOFException();
                            }
                            else {
                                fStringBuffer.clear();
                                fStringBuffer.append((char)c);
                            }
                            scanCharacters(fStringBuffer);
                            break;
                        }
                    } // switch
                } // try
                catch (EOFException e) {
                    setScanner(fContentScanner);
                    setScannerState(STATE_END_DOCUMENT);
                    return true;
                }
            } // do
            while (next || complete);
            return true;
        } // scan(boolean):boolean

        //
        // Protected methods
        //

        /** Scan characters. */
        protected void scanCharacters(XMLStringBuffer buffer) throws IOException {
            while (true) {
                int c = read();
                if (c == -1 || c == '<') {
                    if (c == '<') {
                        fCharOffset--;
                    }
                    break;
                }
                if (c == '\r') {
                    buffer.append('\n');
                    c = read();
                    if (c != '\n') {
                        if (c == -1 || c == '<') {
                            fCharOffset--;
                            break;
                        }
                        buffer.append((char)c);
                    }
                }
                else {
                    buffer.append((char)c);
                }
            }
            if (buffer.length > 0) {
                if (DEBUG_CALLBACKS) {
                    System.out.println("characters("+buffer+")");
                }
                fDocumentHandler.characters(buffer, null);
            }
        } // scanCharacters(StringBuffer)

    } // class SpecialScanner

    /**
     * A playback input stream. This class has the ability to save the bytes
     * read from the underlying input stream and play the bytes back later.
     * This class is used by the HTML scanner to switch encodings when a 
     * &lt;meta&gt; tag is detected that specifies a different encoding. 
     * <p>
     * If the encoding is changed, then the scanner calls the 
     * <code>playback</code> method and re-scans the beginning of the HTML
     * document again. This should not be too much of a performance problem
     * because the &lt;meta&gt; tag appears at the beginning of the document.
     * <p>
     * If the &lt;body&gt; tag is reached without playing back the bytes,
     * then the buffer can be cleared by calling the <code>clear</code>
     * method. This stops the buffering of bytes and allows the memory used
     * by the buffer to be reclaimed. 
     * <p>
     * <strong>Note:</strong> 
     * If the buffer is never played back or cleared, this input stream
     * will continue to buffer the entire stream. Therefore, it is very
     * important to use this stream correctly.
     *
     * @author Andy Clark
     */
    public static class PlaybackInputStream
        extends FilterInputStream {

        //
        // Constants
        //

        /** Set to true to debug playback. */
        private static final boolean DEBUG_PLAYBACK = false;

        //
        // Data
        //

        // state

        /** Playback mode. */
        protected boolean fPlayback = false;

        /** Buffer cleared. */
        protected boolean fCleared = false;

        /** Encoding detected. */
        protected boolean fDetected = false;

        // buffer info

        /** Byte buffer. */
        protected byte[] fByteBuffer = new byte[1024];

        /** Offset into byte buffer during playback. */
        protected int fByteOffset = 0;

        /** Length of bytes read into byte buffer. */
        protected int fByteLength = 0;

        /** Pushback offset. */
        public int fPushbackOffset = 0;

        /** Pushback length. */
        public int fPushbackLength = 0;

        //
        // Constructors
        //

        /** Constructor. */
        public PlaybackInputStream(InputStream in) {
            super(in);
        } // <init>(InputStream)

        //
        // Public methods
        //

        /** Detect encoding. */
        public void detectEncoding(String[] encodings) throws IOException {
            if (fDetected) {
                throw new IOException("should not detect encoding twice");
            }
            fDetected = true;
            int b1 = read();
            int b2 = read();
            // UTF-8 BOM: 0xEFBBBF
            if (b1 == 0xEF && b2 == 0xBB) {
                int b3 = read();
                if (b3 == 0xBF) {
                    fPushbackOffset = 3;
                    encodings[0] = "UTF-8";
                    encodings[1] = "UTF8";
                    return;
                }
                fPushbackLength = 3;
            }
            // UTF-16 LE BOM: 0xFFFE
            if (b1 == 0xFF && b2 == 0xFE) {
                encodings[0] = "UTF-16";
                encodings[1] = "UnicodeLittleUnmarked";
                return;
            }
            // UTF-16 BE BOM: 0xFEFF
            if (b1 == 0xFE && b2 == 0xFF) {
                encodings[0] = "UTF-16";
                encodings[1] = "UnicodeBigUnmarked";
                return;
            }
            // unknown
            fPushbackLength = 2;
        } // detectEncoding()

        /** Playback buffer contents. */
        public void playback() {
            fPlayback = true;
        } // playback()

        /** 
         * Clears the buffer.
         * <p>
         * <strong>Note:</strong>
         * The buffer cannot be cleared during playback. Therefore, calling
         * this method during playback will not do anything. However, the
         * buffer will be cleared automatically at the end of playback.
         */
        public void clear() {
            if (!fPlayback) {
                fCleared = true;
                fByteBuffer = null;
            }
        } // clear()

        //
        // InputStream methods
        //

        /** Read a byte. */
        public int read() throws IOException {
            if (DEBUG_PLAYBACK) {
                System.out.println("(read");
            }
            if (fPushbackOffset < fPushbackLength) {
                return fByteBuffer[fPushbackOffset++];
            }
            if (fCleared) {
                return in.read();
            }
            if (fPlayback) {
                int c = fByteBuffer[fByteOffset++];
                if (fByteOffset == fByteLength) {
                    fCleared = true;
                    fByteBuffer = null;
                }
                if (DEBUG_PLAYBACK) {
                    System.out.println(")read -> "+(char)c);
                }
                return c;
            }
            int c = in.read();
            if (c != -1) {
                if (fByteLength == fByteBuffer.length) {
                    byte[] newarray = new byte[fByteLength + 1024];
                    System.arraycopy(fByteBuffer, 0, newarray, 0, fByteLength);
                    fByteBuffer = newarray;
                }
                fByteBuffer[fByteLength++] = (byte)c;
            }
            if (DEBUG_PLAYBACK) {
                System.out.println(")read -> "+(char)c);
            }
            return c;
        } // read():int

        /** Read an array of bytes. */
        public int read(byte[] array) throws IOException {
            return read(array, 0, array.length);
        } // read(byte[]):int

        /** Read an array of bytes. */
        public int read(byte[] array, int offset, int length) throws IOException {
            if (DEBUG_PLAYBACK) {
                System.out.println(")read("+offset+','+length+')');
            }
            if (fPushbackOffset < fPushbackLength) {
                int count = fPushbackLength - fPushbackOffset;
                if (count > length) {
                    count = length;
                }
                System.arraycopy(fByteBuffer, fPushbackOffset, array, offset, count);
                fPushbackOffset += count;
                return count;
            }
            if (fCleared) {
                return in.read(array, offset, length);
            }
            if (fPlayback) {
                if (fByteOffset + length > fByteLength) {
                    length = fByteLength - fByteOffset;
                }
                System.arraycopy(fByteBuffer, fByteOffset, array, offset, length);
                fByteOffset += length;
                if (fByteOffset == fByteLength) {
                    fCleared = true;
                    fByteBuffer = null;
                }
                return length;
            }
            int count = in.read(array, offset, length);
            if (count != -1) {
                if (fByteLength + count > fByteBuffer.length) {
                    byte[] newarray = new byte[fByteLength + count + 512];
                    System.arraycopy(fByteBuffer, 0, newarray, 0, fByteLength);
                    fByteBuffer = newarray;
                }
                System.arraycopy(array, offset, fByteBuffer, fByteLength, count);
                fByteLength += count;
            }
            if (DEBUG_PLAYBACK) {
                System.out.println(")read("+offset+','+length+") -> "+count);
            }
            return count;
        } // read(byte[]):int

    } // class PlaybackInputStream

} // class HTMLScanner
