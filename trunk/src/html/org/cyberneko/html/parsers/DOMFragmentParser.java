/* 
 * (C) Copyright 2002-2003, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

package org.cyberneko.html.parsers;

import org.cyberneko.html.HTMLConfiguration;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;

import org.apache.xerces.xni.parser.XMLDocumentSource;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParseException;
import org.apache.xerces.xni.parser.XMLParserConfiguration;

import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

import org.w3c.dom.html.HTMLDocument;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A DOM parser for HTML fragments.
 *
 * @author Andy Clark
 *
 * @version $Id$
 */
public class DOMFragmentParser
    implements XMLDocumentHandler {

    //
    // Constants
    //

    // features

    /** Document fragment balancing only. */
    protected static final String DOCUMENT_FRAGMENT = "http://cyberneko.org/html/features/document-fragment";

    //
    // Data
    //

    /** Parser configuration. */
    protected XMLParserConfiguration fParserConfiguration;

    /** Document source. */
    protected XMLDocumentSource fDocumentSource;

    /** DOM document fragment. */
    protected DocumentFragment fDocumentFragment;

    /** HTML document. */
    protected HTMLDocument fHTMLDocument;

    /** Current node. */
    protected Node fCurrentNode;

    /** True if within a CDATA section. */
    protected boolean fInCDATASection;

    //
    // Constructors
    //

    /** Default constructor. */
    public DOMFragmentParser() {
        fParserConfiguration = new HTMLConfiguration();
        fParserConfiguration.setFeature(DOCUMENT_FRAGMENT, true);
        fParserConfiguration.setDocumentHandler(this);
    } // <init>()

    //
    // Public methods
    //

    /** Parses a document fragment. */
    public void parse(String systemId, DocumentFragment fragment) 
        throws SAXException, IOException {
        parse(new InputSource(systemId), fragment);
    } // parse(String,DocumentFragment)

    /** Parses a document fragment. */
    public void parse(InputSource source, DocumentFragment fragment) 
        throws SAXException, IOException {

        fDocumentFragment = fragment;
        fHTMLDocument = (HTMLDocument)fDocumentFragment.getOwnerDocument();
        fCurrentNode = fDocumentFragment;

        try {
            String pubid = source.getPublicId();
            String sysid = source.getSystemId();
            String encoding = source.getEncoding();
            InputStream stream = source.getByteStream();
            Reader reader = source.getCharacterStream();
            
            XMLInputSource inputSource = 
                new XMLInputSource(pubid, sysid, sysid);
            inputSource.setEncoding(encoding);
            inputSource.setByteStream(stream);
            inputSource.setCharacterStream(reader);
            
            fParserConfiguration.parse(inputSource);
        }
        catch (XMLParseException e) {
            Exception ex = e.getException();
            if (ex != null) {
                throw new SAXParseException(e.getMessage(), null, ex);
            }
            throw new SAXParseException(e.getMessage(), null);
        }

    } // parse(InputSource,DocumentFragment)

    //
    // XMLDocumentHandler methods
    //

    /** Sets the document source. */
    public void setDocumentSource(XMLDocumentSource source) {
        fDocumentSource = source;
    } // setDocumentSource(XMLDocumentSource)

    /** Returns the document source. */
    public XMLDocumentSource getDocumentSource() {
        return fDocumentSource;
    } // getDocumentSource():XMLDocumentSource

    /** Start document. */
    public void startDocument(XMLLocator locator, String encoding,
                              Augmentations augs) throws XNIException {
        startDocument(locator, encoding, null, augs);
    } // startDocument(XMLLocator,String,Augmentations)

    // since Xerces 2.2.0

    /** Start document. */
    public void startDocument(XMLLocator locator, String encoding,
                              NamespaceContext nscontext,
                              Augmentations augs) throws XNIException {
        fInCDATASection = false;
    } // startDocument(XMLLocator,String,NamespaceContext,Augmentations)

    /** XML declaration. */
    public void xmlDecl(String version, String encoding,
                        String standalone, Augmentations augs)
        throws XNIException {
    } // xmlDecl(String,String,String,Augmentations)

    /** Document type declaration. */
    public void doctypeDecl(String root, String pubid, String sysid,
                            Augmentations augs) throws XNIException {
    } // doctypeDecl(String,String,String,Augmentations)

    /** Processing instruction. */
    public void processingInstruction(String target, XMLString data,
                                      Augmentations augs)
        throws XNIException {
        ProcessingInstruction pi = 
            fHTMLDocument.createProcessingInstruction(target, data.toString());
        fCurrentNode.appendChild(pi);
    } // processingInstruction(String,XMLString,Augmentations)

    /** Comment. */
    public void comment(XMLString text, Augmentations augs)
        throws XNIException {
        Comment comment = fHTMLDocument.createComment(text.toString());
        fCurrentNode.appendChild(comment);
    } // comment(XMLString,Augmentations)

    /** Start prefix mapping. @deprecated Since Xerces 2.2.0. */
    public void startPrefixMapping(String prefix, String uri,
                                   Augmentations augs) throws XNIException {
    } // startPrefixMapping(String,String,Augmentations)

    /** End prefix mapping. @deprecated Since Xerces 2.2.0. */
    public void endPrefixMapping(String prefix, Augmentations augs)
        throws XNIException {
    } // endPrefixMapping(String,Augmentations)

    /** Start element. */
    public void startElement(QName element, XMLAttributes attrs,
                             Augmentations augs) throws XNIException {
        Element elementNode = fHTMLDocument.createElement(element.rawname);
        int count = attrs != null ? attrs.getLength() : 0;
        for (int i = 0; i < count; i++) {
            String aname = attrs.getQName(i);
            String avalue = attrs.getValue(i);
            elementNode.setAttribute(aname, avalue);
        }
        fCurrentNode.appendChild(elementNode);
        fCurrentNode = elementNode;
    } // startElement(QName,XMLAttributes,Augmentations)

    /** Empty element. */
    public void emptyElement(QName element, XMLAttributes attrs,
                             Augmentations augs) throws XNIException {
        startElement(element, attrs, augs);
        endElement(element, augs);
    } // emptyElement(QName,XMLAttributes,Augmentations)

    /** Characters. */
    public void characters(XMLString text, Augmentations augs)
        throws XNIException {

        if (fInCDATASection) {
            Node node = fCurrentNode.getLastChild();
            if (node != null && node.getNodeType() == Node.CDATA_SECTION_NODE) {
                CDATASection cdata = (CDATASection)node;
                cdata.appendData(text.toString());
            }
            else {
                CDATASection cdata = fHTMLDocument.createCDATASection(text.toString());
                fCurrentNode.appendChild(cdata);
            }
        }
        else {
            Node node = fCurrentNode.getLastChild();
            if (node != null && node.getNodeType() == Node.TEXT_NODE) {
                Text textNode = (Text)node;
                textNode.appendData(text.toString());
            }
            else {
                Text textNode = fHTMLDocument.createTextNode(text.toString());
                fCurrentNode.appendChild(textNode);
            }
        }

    } // characters(XMLString,Augmentations)

    /** Ignorable whitespace. */
    public void ignorableWhitespace(XMLString text, Augmentations augs)
        throws XNIException {
        characters(text, augs);
    } // ignorableWhitespace(XMLString,Augmentations)

    /** Start general entity. */
    public void startGeneralEntity(String name, XMLResourceIdentifier id,
                                   String encoding, Augmentations augs)
        throws XNIException {
        EntityReference entityRef = fHTMLDocument.createEntityReference(name);
        fCurrentNode.appendChild(entityRef);
        fCurrentNode = entityRef;
    } // startGeneralEntity(String,XMLResourceIdentifier,String,Augmentations)

    /** Text declaration. */
    public void textDecl(String version, String encoding,
                         Augmentations augs) throws XNIException {
    } // textDecl(String,String,Augmentations)

    /** End general entity. */
    public void endGeneralEntity(String name, Augmentations augs)
        throws XNIException {
        fCurrentNode = fCurrentNode.getParentNode();
    } // endGeneralEntity(String,Augmentations)

    /** Start CDATA section. */
    public void startCDATA(Augmentations augs) throws XNIException {
        fInCDATASection = true;
    } // startCDATA(Augmentations)

    /** End CDATA section. */
    public void endCDATA(Augmentations augs) throws XNIException {
        fInCDATASection = false;
    } // endCDATA(Augmentations)

    /** End element. */
    public void endElement(QName element, Augmentations augs)
        throws XNIException {
        fCurrentNode = fCurrentNode.getParentNode();
    } // endElement(QName,Augmentations)

    /** End document. */
    public void endDocument(Augmentations augs) throws XNIException {
    } // endDocument(Augmentations)

    //
    // DEBUG
    //

    /***
    public static void print(Node node) {
        short type = node.getNodeType();
        switch (type) {
            case Node.ELEMENT_NODE: {
                System.out.print('<');
                System.out.print(node.getNodeName());
                org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
                int attrCount = attrs != null ? attrs.getLength() : 0;
                for (int i = 0; i < attrCount; i++) {
                    Node attr = attrs.item(i);
                    System.out.print(' ');
                    System.out.print(attr.getNodeName());
                    System.out.print("='");
                    System.out.print(attr.getNodeValue());
                    System.out.print('\'');
                }
                System.out.print('>');
                break;
            }
            case Node.TEXT_NODE: {
                System.out.print(node.getNodeValue());
                break;
            }
        }
        Node child = node.getFirstChild();
        while (child != null) {
            print(child);
            child = child.getNextSibling();
        }
        if (type == Node.ELEMENT_NODE) {
            System.out.print("</");
            System.out.print(node.getNodeName());
            System.out.print('>');
        }
        else if (type == Node.DOCUMENT_NODE || type == Node.DOCUMENT_FRAGMENT_NODE) {
            System.out.println();
        }
        System.out.flush();
    }

    public static void main(String[] argv) throws Exception {
        DOMFragmentParser parser = new DOMFragmentParser();
        HTMLDocument document = new org.apache.html.dom.HTMLDocumentImpl();
        for (int i = 0; i < argv.length; i++) {
            String sysid = argv[i];
            System.err.println("# "+sysid);
            DocumentFragment fragment = document.createDocumentFragment();
            parser.parse(sysid, fragment);
            print(fragment);
        }
    }
    /***/

} // class DOMFragmentParser
