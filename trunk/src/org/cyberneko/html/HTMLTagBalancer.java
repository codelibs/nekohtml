/* (C) Copyright 2002, Andy Clark. All rights reserved. */

package org.cyberneko.html;

import java.util.Hashtable;
import java.util.Stack;
                                          
import org.apache.xerces.util.XMLAttributesImpl;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
                      
/**
 * Balances tags in an HTML document. This component receives document events
 * and tries to correct many common mistakes that human (and computer) HTML
 * document authors make. The tag balancer can:
 * <ul>
 * <li>add missing parent elements;
 * <li>automatically close elements with optional end tags; and
 * <li>handle mis-matched inline element tags.
 * </ul>
 *
 * @author Andy Clark
 *
 * @version $Id$
 */
public class HTMLTagBalancer
    implements XMLDocumentFilter {

    //
    // Constants
    //

    /** Empty attributes. */
    private static final XMLAttributes EMPTY_ATTRS = new XMLAttributesImpl();

    //
    // Data
    //

    /** The document handler. */
    protected XMLDocumentHandler fDocumentHandler;

    /** The element stack. */
    protected final Stack fElementStack = new Stack();

    /** The inline stack. */
    protected final Stack fInlineStack = new Stack();

    /** True if root element has been seen. */
    protected boolean fSeenRootElement;

    // temp vars

    /** A qualified name. */
    private final QName fQName = new QName();

    //
    // XMLDocumentSource methods
    //

    /** Sets the document handler. */
    public void setDocumentHandler(XMLDocumentHandler handler) {
        fDocumentHandler = handler;
    } // setDocumentHandler(XMLDocumentHandler)

    //
    // XMLDocumentHandler methods
    //

    /** Start document. */
    public void startDocument(XMLLocator locator, String encoding, Augmentations augs) 
        throws XNIException {
        
        fElementStack.removeAllElements();
        fSeenRootElement = false;
        if (fDocumentHandler != null) {
            fDocumentHandler.startDocument(locator, encoding, augs);
        }

    } // startDocument(Augmentations)

    /** XML declaration. */
    public void xmlDecl(String version, String encoding, String standalone,
                        Augmentations augs) throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.xmlDecl(version, encoding, standalone, augs);
        }
    } // xmlDecl(String,String,String,Augmentations)

    /** Doctype declaration. */
    public void doctypeDecl(String rootElementName, String publicId, String systemId,
                            Augmentations augs) throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.doctypeDecl(rootElementName, publicId, systemId, augs);
        }
    } // doctypeDecl(String,String,String,Augmentations)

    /** End document. */
    public void endDocument(Augmentations augs) throws XNIException {

        // handle empty document
        if (!fSeenRootElement) {
            fQName.setValues(null, null, "HTML", null);
            startElement(fQName, EMPTY_ATTRS, null);
            endElement(fQName, null);
        }

        // pop all remaining elements
        else {
            int length = fElementStack.size();
            for (int i = 0; i < length; i++) {
                Info info = (Info)fElementStack.peek();
                endElement(info.element, null);
            }
        }

        // call handler
        if (fDocumentHandler != null) {
            fDocumentHandler.endDocument(augs);
        }

    } // endDocument(Augmentations)

    /** Comment. */
    public void comment(XMLString text, Augmentations augs) throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.comment(text, augs);
        }
    } // comment(XMLString,Augmentations)

    /** Processing instruction. */
    public void processingInstruction(String target, XMLString data,
                                      Augmentations augs) throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.processingInstruction(target, data, augs);
        }
    } // processingInstruction(String,XMLString,Augmentations)

    /** Start prefix mapping. */
    public void startPrefixMapping(String prefix, String uri, Augmentations augs)
        throws XNIException {} // startPrefixMapping(String,String,Augmentations)

    /** Start element. */
    public void startElement(QName elem, XMLAttributes attrs, Augmentations augs)
        throws XNIException {
        
        // get element information
        HTMLElements.Element element = HTMLElements.getElement(elem.rawname);
        if (element == null) {
            System.out.println("!!! NOT IMPLEMENTED: <"+elem.rawname+"> !!!");
            System.exit(1);
        }

        // check proper parent
        if (element.parent != null) {
            if (!fSeenRootElement) {
                QName qname = new QName(null, null, element.parent instanceof String ? (String)element.parent : ((String[])element.parent)[0], null);
                startElement(qname, EMPTY_ATTRS, null);
            }
            else {
                int depth = getDepth(element.parent, false);
                if (depth == -1) {
                    String ename = element.parent instanceof String
                                 ? (String)element.parent 
                                 : ((String[])element.parent)[0];
                    HTMLElements.Element pelement = HTMLElements.getElement(ename);
                    if (pelement != null) {
                        int pdepth = getDepth(pelement.parent, false);
                        if (pdepth != -1) {
                            for (int i = 1; i < pdepth; i++) {
                                Info info = (Info)fElementStack.peek();
                                endElement(info.element, null);
                            }
                            QName qname = new QName(null, null, element.parent instanceof String ? (String)element.parent : ((String[])element.parent)[0], null);
                            startElement(qname, EMPTY_ATTRS, null);
                        }
                    }
                }
            }
        }

        // close previous elements
        int length = fElementStack.size();
        for (int i = length - 1; i >= 0; i--) {
            Info info = (Info)fElementStack.peek();
            if (element.closes(info.element.rawname)) {
                for (int j = length - 1; j >= i; j--) {
                    info = (Info)fElementStack.pop();
                    if (fDocumentHandler != null) {
                        fDocumentHandler.endElement(info.element, null);
                    }
                }
                length = i;
                continue;
            }
        }

        // call handler
        fSeenRootElement = true;
        if (element != null && element.isEmpty()) {
            if (fDocumentHandler != null) {
                fDocumentHandler.emptyElement(elem, attrs, augs);
            }
        }
        else {
            boolean inline = element != null && element.isInline();
            fElementStack.push(new Info(elem, inline ? attrs : null));
            if (fDocumentHandler != null) {
                fDocumentHandler.startElement(elem, attrs, augs);
            }
        }

    } // startElement(QName,XMLAttributes,Augmentations)

    /** Empty element. */
    public void emptyElement(QName elem, XMLAttributes attrs, Augmentations augs)
        throws XNIException {
        startElement(elem, attrs, augs);
        endElement(elem, augs);
    } // emptyElement(QName,XMLAttributes,Augmentations)

    /** Start entity. */
    public void startGeneralEntity(String name, 
                                   XMLResourceIdentifier id,
                                   String encoding,
                                   Augmentations augs) throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.startGeneralEntity(name, id, encoding, augs);
        }
    } // startGeneralEntity(String,XMLResourceIdentifier,String,Augmentations)

    /** Text declaration. */
    public void textDecl(String version, String encoding, Augmentations augs)
        throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.textDecl(version, encoding, augs);
        }
    } // textDecl(String,String,Augmentations)

    /** End entity. */
    public void endGeneralEntity(String name, Augmentations augs) throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.endGeneralEntity(name, augs);
        }
    } // endGeneralEntity(String,Augmentations)

    /** Start CDATA section. */
    public void startCDATA(Augmentations augs) throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.startCDATA(augs);
        }
    } // startCDATA(Augmentations)

    /** End CDATA section. */
    public void endCDATA(Augmentations augs) throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.endCDATA(augs);
        }
    } // endCDATA(Augmentations)

    /** Characters. */
    public void characters(XMLString text, Augmentations augs) throws XNIException {

        // handle bare characters
        if (!fSeenRootElement) {
            boolean whitespace = true;
            for (int i = 0; i < text.length; i++) {
                if (!Character.isWhitespace(text.ch[text.offset + i])) {
                    whitespace = false;
                    break;
                }
            }
            if (whitespace) {
                return;
            }
            fQName.setValues(null, null, "BODY", null);
            startElement(fQName, EMPTY_ATTRS, null);
        }

        // call handler
        if (fDocumentHandler != null) {
            fDocumentHandler.characters(text, augs);
        }

    } // characters(XMLString,Augmentations)

    /** Ignorable whitespace. */
    public void ignorableWhitespace(XMLString text, Augmentations augs)
        throws XNIException {
        characters(text, augs);
    } // ignorableWhitespace(XMLString,Augmentations)

    /** End element. */
    public void endElement(QName element, Augmentations augs) throws XNIException {
        
        // find unbalanced inline elements
        int depth = getDepth(element.rawname, true);
        if (depth > 1 && HTMLElements.getElement(element.rawname).isInline()) {
            int size = fElementStack.size();
            fInlineStack.removeAllElements();
            for (int i = 0; i < depth - 1; i++) {
                Info info = (Info)fElementStack.elementAt(size - i - 1);
                HTMLElements.Element elem = HTMLElements.getElement(info.element.rawname);
                if (elem.isInline()) {
                    // NOTE: I don't have to make a copy of the info because
                    //       it will just be popped off of the element stack
                    //       as soon as we close it, anyway.
                    fInlineStack.push(info);
                }
            }
        }

        // close children up to appropriate element
        for (int i = 0; i < depth; i++) {
            Info info = (Info)fElementStack.pop();
            if (fDocumentHandler != null) {
                fDocumentHandler.endElement(info.element, augs);
            }
        }

        // re-open inline elements
        if (depth > 1) {
            int size = fInlineStack.size();
            for (int i = 0; i < size; i++) {
                Info info = (Info)fInlineStack.pop();
                startElement(info.element, info.attributes, null);
            }
        }

    } // endElement(QName,Augmentations)

    /** End prefix mapping. */
    public void endPrefixMapping(String prefix, Augmentations augs)
        throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.endPrefixMapping(prefix, augs);
        }
    } // endPrefixMapping(String,Augmentations)

    //
    // Protected static methods
    //

    /**
     * Returns the depth of the open tag associated with the specified
     * element name or -1 if no matching element is found. The "blocked"
     * parameter can be used to specify whether the depth checking is
     * limited to the nearest block element.
     *
     * @param ename The element name.
     * @param blocked True if the depth checking should stop at nearest block.
     */
    protected int getDepth(Object ename, boolean blocked) {

        int length = fElementStack.size();
        String name = ename instanceof String
                    ? (String)ename : ((String[])ename)[0];
        HTMLElements.Element element = HTMLElements.getElement(name);
        if (ename instanceof String) {
            String string = (String)ename;
            for (int i = length - 1; i >= 0; i--) {
                Info info = (Info)fElementStack.elementAt(i);
                if (info.element.rawname.equals(string)) {
                    return length - i;
                }
                if (blocked && element != null) {
                    HTMLElements.Element tag = HTMLElements.getElement(info.element.rawname);
                    if (tag != null && tag.isBlock() && 
                        !element.closes(info.element.rawname)) {
                        break;
                    }
                }
            }
        }
        else {
            String[] array = (String[])ename;
            for (int i = length - 1; i >= 0; i--) {
                Info info = (Info)fElementStack.elementAt(i);
                for (int j = 0; j < array.length; j++) {
                    if (info.element.rawname.equals(array[j])) {
                        return length - i;
                    }
                }
                if (blocked && element != null) {
                    HTMLElements.Element tag = HTMLElements.getElement(info.element.rawname);
                    if (tag != null && tag.isBlock() && 
                        !element.closes(info.element.rawname)) {
                        break;
                    }
                }
            }
        }
        return -1;

    } // getDepth(String,boolean):int

    //
    // Classes
    //

    /**
     * Element info for each start element. This information is used when
     * closing unbalanced inline elements. For example:
     * <pre>
     * &lt;i>unbalanced &lt;b>HTML&lt;/i> content&lt;/b>
     * </pre>
     * <p>
     * It seems that it is a waste of processing and memory to copy the 
     * attributes for every start element even if there are no unbalanced 
     * inline elements in the document. However, if the attributes are
     * <em>not</em> saved, then important attributes such as style
     * information would be lost.
     *
     * @author Andy Clark
     */
    public static class Info {

        //
        // Data
        //

        /** The element qualified name. */
        public QName element;

        /** The element attributes. */
        public XMLAttributes attributes;

        //
        // Constructors
        //

        /**
         * Creates an element information object.
         * <p>
         * <strong>Note:</strong>
         * This constructor makes a copy of the element information.
         *
         * @param element The element qualified name.
         */
        public Info(QName element) {
            this(element, null);
        } // <init>(QName)

        /**
         * Creates an element information object.
         * <p>
         * <strong>Note:</strong>
         * This constructor makes a copy of the element information.
         *
         * @param element The element qualified name.
         * @param attributes The element attributes.
         */
        public Info(QName element, XMLAttributes attributes) {
            if (element != null) {
                this.element = new QName(element);
            }
            if (attributes != null) {
                int length = attributes.getLength();
                if (length > 0) {
                    QName qname = new QName();
                    XMLAttributes newattrs = new XMLAttributesImpl();
                    for (int i = 0; i < length; i++) {
                        attributes.getName(i, qname);
                        String type = attributes.getType(i);
                        String value = attributes.getValue(i);
                        String nonNormalizedValue = attributes.getNonNormalizedValue(i);
                        boolean specified = attributes.isSpecified(i);
                        newattrs.addAttribute(qname, type, value);
                        newattrs.setNonNormalizedValue(i, nonNormalizedValue);
                        newattrs.setSpecified(i, specified);
                    }
                    this.attributes = newattrs;
                }
                else {
                    this.attributes = EMPTY_ATTRS;
                }
            }
        } // <init>(QName,XMLAttributes)

    } // class Info

} // class HTMLTagBalancer
