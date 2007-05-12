/* 
 * (C) Copyright 2002-2003, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

package org.cyberneko.html.filters;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.cyberneko.html.HTMLConfiguration;
import org.cyberneko.html.HTMLElements;
import org.cyberneko.html.filters.DefaultFilter;

import org.apache.xerces.util.XMLAttributesImpl;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;

/**
 * An HTML writer written as a filter. Besides serializing the HTML
 * event stream, the writer also passes the document events to the next
 * stage in the pipeline. This allows applications to insert writer
 * filters between other custom filters for debugging purposes.
 * <p>
 * Since an HTML document may have specified its encoding using the
 * &lt;META&gt; tag and http-equiv/content attributes, the writer will
 * automatically change any character set specified in this tag to
 * match the encoding of the output stream. Therefore, the character
 * encoding name used to construct the writer should be an official
 * <a href='http://www.iana.org/assignments/character-sets'>IANA</a>
 * encoding name and not a Java encoding name.
 * <p>
 * <strong>Note:</strong>
 * The modified character set in the &lt;META&gt; tag is <em>not</em>
 * propagated to the next stage in the pipeline. The changed value is
 * only output to the stream; the original value is sent to the next
 * stage in the pipeline.
 *
 * @author Andy Clark
 *
 * @version $Id$
 */
public class Writer 
    extends DefaultFilter {

    //
    // Constants
    //

    /** Notify character entity references. */
    public static final String NOTIFY_CHAR_REFS = "http://apache.org/xml/features/scanner/notify-char-refs";

    /** Notify built-in entity references. */
    public static final String NOTIFY_HTML_BUILTIN_REFS = "http://cyberneko.org/html/features/scanner/notify-builtin-refs";

    /** Augmentations feature identifier. */
    protected static final String AUGMENTATIONS = "http://cyberneko.org/html/features/augmentations";

    /** Filters property identifier. */
    protected static final String FILTERS = "http://cyberneko.org/html/properties/filters";

    //
    // Data
    //

    /** The encoding. */
    protected String fEncoding;

    /** 
     * The print writer used for serializing the document with the
     * appropriate character encoding. 
     */
    protected PrintWriter fPrinter;

    // state

    /** Seen root element. */
    protected boolean fSeenRootElement;

    /** Seen http-equiv directive. */
    protected boolean fSeenHttpEquiv;

    /** Element depth. */
    protected int fElementDepth;

    /** Normalize character content. */
    protected boolean fNormalize;

    /** Print characters. */
    protected boolean fPrintChars;

    //
    // Constructors
    //

    /** Constructs a writer filter that prints to standard out. */
    public Writer() {
        // Note: UTF-8 should *always* be a supported encoding. Although,
        //       I've heard of the old M$ JVM not supporting it! Amazing. -Ac
        try {
            fEncoding = "UTF-8";
            fPrinter = new PrintWriter(new OutputStreamWriter(System.out, fEncoding));
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage());
        }
    } // <init>()

    /**
     * Constructs a writer filter using the specified output stream and
     * encoding.
     *
     * @param outputStream The output stream to write to.
     * @param encoding The encoding to be used for the output. The encoding name
     *                 should be an official IANA encoding name.
     */
    public Writer(OutputStream outputStream, String encoding) 
        throws UnsupportedEncodingException {
        this(new OutputStreamWriter(outputStream, encoding), encoding);
    } // <init>(OutputStream,String)

    /**
     * Constructs a writer filter using the specified Java writer and
     * encoding.
     *
     * @param writer The Java writer to write to.
     * @param encoding The encoding to be used for the output. The encoding name
     *                 should be an official IANA encoding name.
     */
    public Writer(java.io.Writer writer, String encoding) {
        fEncoding = encoding;
        if (writer instanceof PrintWriter) {
            fPrinter = (PrintWriter)writer;
        }
        else {
            fPrinter = new PrintWriter(writer);
        }
    } // <init>(java.io.Writer,String)

    //
    // XMLDocumentHandler methods
    //

    // since Xerces-J 2.2.0

    /** Start document. */
    public void startDocument(XMLLocator locator, String encoding, 
                              NamespaceContext nscontext, Augmentations augs)
        throws XNIException {
        fSeenRootElement = false;
        fSeenHttpEquiv = false;
        fElementDepth = 0;
        fNormalize = true;
        fPrintChars = true;
        super.startDocument(locator, encoding, nscontext, augs);
    } // startDocument(XMLLocator,String,NamespaceContext,Augmentations)

    // old methods

    /** Start document. */
    public void startDocument(XMLLocator locator, String encoding, Augmentations augs)
        throws XNIException {
        startDocument(locator, encoding, null, augs);
    } // startDocument(XMLLocator,String,Augmentations)

    /** Comment. */
    public void comment(XMLString text, Augmentations augs) 
        throws XNIException {
        if (fSeenRootElement && fElementDepth <= 0) {
            fPrinter.println();
        }
        fPrinter.print("<!--");
        printCharacters(text, false);
        fPrinter.print("-->");
        if (!fSeenRootElement) {
            fPrinter.println();
        }
        fPrinter.flush();
    } // comment(XMLString,Augmentations)

    /** Start element. */
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs)
        throws XNIException {
        fSeenRootElement = true;
        fElementDepth++;
        fNormalize = !HTMLElements.getElement(element.rawname).isSpecial();
        printStartElement(element, attributes);
        super.startElement(element, attributes, augs);
    } // startElement(QName,XMLAttributes,Augmentations)

    /** Empty element. */
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs)
        throws XNIException {
        fSeenRootElement = true;
        printStartElement(element, attributes);
        super.emptyElement(element, attributes, augs);
    } // emptyElement(QName,XMLAttributes,Augmentations)

    /** Characters. */
    public void characters(XMLString text, Augmentations augs) 
        throws XNIException {
        if (fPrintChars) {
            printCharacters(text, fNormalize);
        }
        super.characters(text, augs);
    } // characters(XMLString,Augmentations)

    /** End element. */
    public void endElement(QName element, Augmentations augs)
        throws XNIException {
        fElementDepth--;
        fNormalize = true;
        /***
        // NOTE: Not sure if this is waht should be done in the case where
        //       the encoding is not explitly declared within the HEAD. So
        //       I'm leaving it commented out for now. -Ac
        if (element.rawname.equalsIgnoreCase("head") && !fSeenHttpEquiv) {
            boolean capitalize = Character.isUpperCase(element.rawname.charAt(0));
            String ename = capitalize ? "META" : "meta";
            QName qname = new QName(null, ename, ename, null);
            XMLAttributes attrs = new XMLAttributesImpl();
            QName aname = new QName(null, "http-equiv", "http-equiv", null);
            attrs.addAttribute(aname, "CDATA", "Content-Type");
            aname.setValues(null, "content", "content", null);
            attrs.addAttribute(aname, "CDATA", "text/html; charset="+fEncoding);
            super.emptyElement(qname, attrs, null);
        }
        /***/
        printEndElement(element);
        super.endElement(element, augs);
    } // endElement(QName,Augmentations)

    /** Start general entity. */
    public void startGeneralEntity(String name, XMLResourceIdentifier id, String encoding, Augmentations augs)
        throws XNIException {
        fPrintChars = false;
        printEntity(name);
        super.startGeneralEntity(name, id, encoding, augs);
    } // startGeneralEntity(String,XMLResourceIdentifier,String,Augmentations)

    /** End general entity. */
    public void endGeneralEntity(String name, Augmentations augs)
        throws XNIException {
        fPrintChars = true;
        super.endGeneralEntity(name, augs);
    } // endGeneralEntity(String,Augmentations)

    //
    // Protected methods
    //

    /** Print attribute value. */
    protected void printAttributeValue(String text) {
        int length = text.length();
        for (int j = 0; j < length; j++) {
            char c = text.charAt(j);
            if (c == '"') {
                fPrinter.print("&quot;");
            }
            else {
                fPrinter.print(c);
            }
        }
        fPrinter.flush();
    } // printAttributeValue(String)

    /** Print characters. */
    protected void printCharacters(XMLString text, boolean normalize) {
        if (normalize) {
            for (int i = 0; i < text.length; i++) {
                char c = text.ch[text.offset + i];
                if (c != '\n') {
                    String entity = entity(c);
                    if (entity != null) {
                        printEntity(entity);
                    }
                    else {
                        fPrinter.print(c);
                    }
                }
                else {
                    fPrinter.println();
                }
            }
        }
        else {
            for (int i = 0; i < text.length; i++) {
                char c = text.ch[text.offset + i];
                fPrinter.print(c);
            }
        }
        fPrinter.flush();
    } // printCharacters(XMLString,boolean)

    /** Print start element. */
    protected void printStartElement(QName element, XMLAttributes attributes) {

        // modify META[@http-equiv='content-type']/@content value
        int contentIndex = -1;
        String originalContent = null;
        if (element.rawname.toLowerCase().equals("meta")) {
            String httpEquiv = null;
            int length = attributes.getLength();
            for (int i = 0; i < length; i++) {
                String aname = attributes.getQName(i).toLowerCase();
                if (aname.equals("http-equiv")) {
                    httpEquiv = attributes.getValue(i);
                }
                else if (aname.equals("content")) {
                    contentIndex = i;
                }
            }
            if (httpEquiv != null && httpEquiv.toLowerCase().equals("content-type")) {
                fSeenHttpEquiv = true;
                String content = null;
                if (contentIndex != -1) {
                    originalContent = attributes.getValue(contentIndex);
                    content = originalContent.toLowerCase();
                }
                if (content != null) {
                    int charsetIndex = content.indexOf("charset=");
                    if (charsetIndex != -1) {
                        content = content.substring(0, charsetIndex + 8);
                    }
                    else {
                        content += ";charset=";
                    }
                    content += fEncoding;
                    attributes.setValue(contentIndex, content);
                }
            }
        }

        // print element
        fPrinter.print('<');
        fPrinter.print(element.rawname);
        int attrCount = attributes != null ? attributes.getLength() : 0;
        for (int i = 0; i < attrCount; i++) {
            String aname = attributes.getQName(i);
            String avalue = attributes.getValue(i);
            fPrinter.print(' ');
            fPrinter.print(aname);
            fPrinter.print("=\"");
            printAttributeValue(avalue);
            fPrinter.print('"');
        }
        fPrinter.print('>');
        fPrinter.flush();

        // return original META[@http-equiv]/@content value
        if (contentIndex != -1) {
            attributes.setValue(contentIndex, originalContent);
        }

    } // printStartElement(QName,XMLAttributes)

    /** Print end element. */
    protected void printEndElement(QName element) {
        fPrinter.print("</");
        fPrinter.print(element.rawname);
        fPrinter.print('>');
        fPrinter.flush();
    } // printEndElement(QName)

    /** Print entity. */
    protected void printEntity(String name) {
        fPrinter.print('&');
        fPrinter.print(name);
        fPrinter.print(';');
        fPrinter.flush();
    } // printEntity(String)

    //
    // Private static methods
    //

    // NOTE: These methods are private because I have every intention
    //       of removing them later to be replaced with something that
    //       is designed better. -Ac

    /** Returns the name of the entity for the specified character. */
    private static String entity(char c) {
        switch (c) {
            case 0x0026: return "amp";
            case 0x003c: return "lt";
            case 0x00a0: return "nbsp";
            case 0x00a9: return "copy";
            case 0x00ae: return "reg";
            case 0x2014: return "mdash";
            case 0x00a7: return "sect";
            case 0x00b7: return "middot";
            case 0x00e9: return "eacute";
            case 0x003e: return "gt";
        }
        return null;
    } // entity(char):String
    
    //
    // MAIN
    //

    /** Main. */
    public static void main(String[] argv) throws Exception {
        if (argv.length == 0) {
            printUsage();
            System.exit(1);
        }
        XMLParserConfiguration parser = new HTMLConfiguration();
        parser.setFeature(NOTIFY_CHAR_REFS, true);
        parser.setFeature(NOTIFY_HTML_BUILTIN_REFS, true);
        String encoding = "Windows-1252";
        boolean identity = false;
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            if (arg.equals("-e")) {
                encoding = argv[++i];
                continue;
            }
            if (arg.equals("-i")) {
                identity = true;
                continue;
            }
            if (arg.equals("-h")) {
                printUsage();
                System.exit(1);
            }
            XMLDocumentFilter[] filters;
            if (identity) {
                parser.setFeature(AUGMENTATIONS, true);
                filters = new XMLDocumentFilter[] { 
                    new Identity(),
                    new Writer(System.out, encoding) 
                };
            }
            else {
                filters = new XMLDocumentFilter[] { new Writer(System.out, encoding) };
            }
            parser.setProperty(FILTERS, filters);
            parser.parse(new XMLInputSource(null, arg, null));
        }
    } // main(String[])

    /** Print usage. */
    private static void printUsage() {
        System.err.println("usage: java "+Writer.class.getName()+" (options) file ...");
        System.err.println();
        System.err.println("options:");
        System.err.println("  -e name  Specify IANA name of output encoding.");
        System.err.println("  -i       Perform identity transform.");
        System.err.println("  -h       Display help screen.");
    } // printUsage()

} // class Writer
