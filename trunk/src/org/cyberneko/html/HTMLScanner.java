/* (C) Copyright 2002, Andy Clark. All rights reserved. */

package org.cyberneko.html;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;

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

    /** Default buffer size. */
    private static final int DEFAULT_BUFFER_SIZE = 2048;

    // debugging

    /** Set to true to debug changes in the scanner. */
    private static final boolean DEBUG_SCANNER = false;

    /** Set to true to debug changes in the scanner state. */
    private static final boolean DEBUG_SCANNER_STATE = false;

    /** Set to true to debug the buffer. */
    private static final boolean DEBUG_BUFFER = false;

    //
    // Data
    //

    // state

    /** The character stream. */
    protected Reader fIn;

    /** The current scanner. */
    protected Scanner fScanner;

    /** The current scanner state. */
    protected short fScannerState;

    /** The document handler. */
    protected XMLDocumentHandler fDocumentHandler;

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
    protected final char[] fBuffer = new char[DEFAULT_BUFFER_SIZE];

    /** Offset into character buffer. */
    protected int fOffset = 0;

    /** Length of characters read into character buffer. */
    protected int fLength = 0;

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
        Reader reader = source.getCharacterStream();
        if (reader == null) {
            InputStream stream = source.getByteStream();
            if (stream == null) {
                String systemId = source.getSystemId();
                stream = new FileInputStream(systemId);
            }
            //reader = new InputStreamReader(stream, "UTF-8");
            //reader = new InputStreamReader(stream, "ISO-8859-1");
            //reader = new InputStreamReader(stream, "ISO8859_1");
            reader = new InputStreamReader(stream, "Cp1252");
        }
        fIn = reader;
        setScanner(fContentScanner);
        setScannerState(STATE_START_DOCUMENT);
        fOffset = 0;
        fLength = 0;
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
    // Protected methods
    //

    // i/o

    /** Reads a single character. */
    protected int read() throws IOException {
        if (fOffset == fLength) {
            if (load(0) == -1) {
                return -1;
            }
        }
        return fBuffer[fOffset++];
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
        int count = fIn.read(fBuffer, offset, fBuffer.length - offset);
        fLength = count != -1 ? count + offset : offset;
        fOffset = offset;
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
        if (fOffset == fLength) {
            if (load(0) == -1) {
                if (DEBUG_BUFFER) {
                    System.out.print(")scanName: ");
                    printBuffer();
                    System.out.println(" -> null");
                }
                return null;
            }
        }
        int offset = fOffset;
        while (true) {
            while (fOffset < fLength) {
                char c = fBuffer[fOffset];
                if (!Character.isLetterOrDigit(c) &&
                    !(c == '-' || c == '.' || c == ':')) {
                    break;
                }
                fOffset++;
            }
            if (fOffset == fLength) {
                int length = fLength - offset;
                System.arraycopy(fBuffer, offset, fBuffer, 0, length);
                load(length);
                offset = 0;
            }
            else {
                break;
            }
        }
        int length = fOffset - offset;
        String name = length > 0 ? new String(fBuffer, offset, length) : null;
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
            if (fOffset == fLength) {
                if (load(0) == -1) {
                    break OUTER;
                }
            }
            while (fOffset < fLength) {
                char c = fBuffer[fOffset++];
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
            System.out.print(fLength);
            System.out.print(' ');
            System.out.print(fOffset);
            if (fLength > 0) {
                System.out.print(" \"");
                for (int i = 0; i < fLength; i++) {
                    if (i == fOffset) {
                        System.out.print('^');
                    }
                    char c = fBuffer[i];
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
                if (fOffset == fLength) {
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
                                if (fDocumentHandler != null) {
                                    fStringBuffer.clear();
                                    fStringBuffer.append(ce);
                                    fDocumentHandler.characters(fStringBuffer, null);
                                }
                            }
                            else if (c == -1) {
                                setScannerState(STATE_END_DOCUMENT);
                                next = true;
                            }
                            else {
                                fOffset--;
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
                                fOffset--;
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
                            if (fDocumentHandler != null) {
                                fDocumentHandler.startDocument(null, null, null);
                            }
                            setScannerState(STATE_CONTENT);
                            break;
                        }
                        case STATE_END_DOCUMENT: {
                            if (fDocumentHandler != null) {
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
                    fOffset--;
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
            if (fOffset == fLength) {
                if (load(0) == -1) {
                    if (DEBUG_BUFFER) {
                        System.out.print(")scanCharacters: ");
                        printBuffer();
                        System.out.println();
                    }
                    return;
                }
            }
            char c = fBuffer[fOffset];
            int newlines = 0;
            int offset = fOffset;
            if (c == '\n' || c == '\r') {
                do {
                    c = fBuffer[fOffset++];
                    if (c == '\r') {
                        newlines++;
                        if (fOffset == fLength) {
                            offset = 0;
                            fOffset = newlines;
                            if (load(newlines) == -1) {
                                break;
                            }
                        }
                        if (fBuffer[fOffset] == '\n') {
                            fOffset++;
                            offset++;
                        }
                        else {
                            newlines++;
                        }
                    }
                    else if (c == '\n') {
                        newlines++;
                        if (fOffset == fLength) {
                            offset = 0;
                            fOffset = newlines;
                            if (load(newlines) == -1) {
                                break;
                            }
                        }
                    }
                    else {
                        fOffset--;
                        break;
                    }
                } while (fOffset < fLength - 1);
                for (int i = offset; i < fOffset; i++) {
                    fBuffer[i] = '\n';
                }
            }
            while (fOffset < fLength) {
                c = fBuffer[fOffset];
                if (c == '<' || c == '&' || c == '\n' || c == '\r') {
                    break;
                }
                fOffset++;
            }
            if (fOffset > offset && fDocumentHandler != null) {
                fString.setValues(fBuffer, offset, fOffset - offset);
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
                        fOffset--;
                        continue;
                    }
                    if (c != '>') {
                        for (int i = 0; i < count; i++) {
                            fStringBuffer.append('-');
                        }
                        fOffset--;
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
            if (fDocumentHandler != null) {
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
            /***
            // REVISIT: implement the ability to change encoding
            if (ename.equals("META")) {
                String httpEquiv = fAttributes.getValue("http-equiv");
                if (httpEquiv != null && httpEquiv.equalsIgnoreCase("content-type")) {
                    String content = fAttributes.getValue("content");
                    System.out.println(">>> http-equiv: '"+httpEquiv+"'");
                    System.out.println(">>> content: '"+content+"'");
                }
            }
            /***/
            if (fDocumentHandler != null) {
                fQName.setValues(null, null, ename, null);
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
            fOffset--;
            String aname = scanName();
            if (aname == null) {
                skip();
                return false;
            }
            aname = aname.toLowerCase();
            c = read();
            if (c == '/' || c == '>') {
                fQName.setValues(null, null, aname, null);
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
                fQName.setValues(null, null, aname, null);
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
                    fQName.setValues(null, null, aname, null);
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
                                fOffset--;
                                break;
                            }
                        }
                        if (c == -1) {
                            throw new EOFException();
                        }
                        fStringBuffer.append((char)c);
                    }
                    fQName.setValues(null, null, aname, null);
                    attributes.addAttribute(fQName, "CDATA", fStringBuffer.toString());
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
                fQName.setValues(null, null, aname, null);
                attributes.addAttribute(fQName, "CDATA", fStringBuffer.toString());
            }
            else {
                fQName.setValues(null, null, aname, null);
                attributes.addAttribute(fQName, "CDATA", "");
                fOffset--;
            }
            return true;
        } // scanAttribute(XMLAttributesImpl):boolean

        /** Scans an end element. */
        protected void scanEndElement() throws IOException {
            String ename = scanName();
            skip();
            if (ename != null) {
                ename = ename.toUpperCase();
                if (fDocumentHandler != null) {
                    fQName.setValues(null, null, ename, null);
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
                                            fQName.setValues(null, null, ename.toUpperCase(), null);
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
                        fOffset--;
                    }
                    break;
                }
                buffer.append((char)c);
            }
            if (buffer.length > 0) {
                fDocumentHandler.characters(buffer, null);
            }
        } // scanCharacters(StringBuffer)

    } // class SpecialScanner

} // class HTMLScanner
