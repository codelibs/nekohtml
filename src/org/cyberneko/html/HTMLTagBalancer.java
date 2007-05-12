/* (C) Copyright 2002, Andy Clark. All rights reserved. */

package org.cyberneko.html;

import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
                                          
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

    /** Element information. */
    protected static final Vector ELEMENTS = new Vector();

    /** Empty attributes. */
    private static final XMLAttributes EMPTY_ATTRS = new XMLAttributesImpl();

    //
    // Static initializer
    //

    // <!ENTITY % heading "H1|H2|H3|H4|H5|H6">
    // <!ENTITY % fontstyle "TT | I | B | BIG | SMALL">
    // <!ENTITY % phrase "EM | STRONG | DFN | CODE | SAMP | KBD | VAR | CITE | ABBR | ACRONYM" >
    // <!ENTITY % special "A | IMG | OBJECT | BR | SCRIPT | MAP | Q | SUB | SUP | SPAN | BDO">
    // <!ENTITY % formctrl "INPUT | SELECT | TEXTAREA | LABEL | BUTTON">
    // <!ENTITY % inline "#PCDATA | %fontstyle; | %phrase; | %special; | %formctrl;">
    // <!ENTITY % block "P | %heading; | %list; | %preformatted; | DL | DIV | NOSCRIPT | BLOCKQUOTE | FORM | HR | TABLE | FIELDSET | ADDRESS">
    // <!ENTITY % flow "%block; | %inline;">
    
    static {
        // A - - (%inline;)* -(A)
        ELEMENTS.addElement(new Element("A", Element.INLINE, "BODY", null));
        // ABBR - - (%inline;)*
        ELEMENTS.addElement(new Element("ABBR", Element.INLINE, "BODY", null));
        // ACRONYM - - (%inline;)*
        ELEMENTS.addElement(new Element("ACRONYM", Element.INLINE, "BODY", null));
        // ADDRESS - - (%inline;)*
        ELEMENTS.addElement(new Element("ADDRESS", Element.BLOCK, "BODY", null));
        // APPLET
        ELEMENTS.addElement(new Element("APPLET", 0, "BODY", null));
        // AREA - O EMPTY
        ELEMENTS.addElement(new Element("AREA", Element.EMPTY, "MAP", null));
        // B - - (%inline;)*
        ELEMENTS.addElement(new Element("B", Element.INLINE, "BODY", null));
        // BASE - O EMPTY
        ELEMENTS.addElement(new Element("BASE", Element.EMPTY, "HEAD", null));
        // BASEFONT
        ELEMENTS.addElement(new Element("BASEFONT", 0, "HEAD", null));
        // BDO - - (%inline;)*
        ELEMENTS.addElement(new Element("BDO", Element.INLINE, "BODY", null));
        // BGSOUND
        ELEMENTS.addElement(new Element("BGSOUND", Element.EMPTY, "HEAD", null));
        // BIG - - (%inline;)*
        ELEMENTS.addElement(new Element("BIG", Element.INLINE, "BODY", null));
        // BLINK
        ELEMENTS.addElement(new Element("BLINK", Element.INLINE, "BODY", null));
        // BLOCKQUOTE - - (%block;|SCRIPT)+
        ELEMENTS.addElement(new Element("BLOCKQUOTE", Element.BLOCK, "BODY", null));
        // BODY O O (%block;|SCRIPT)+ +(INS|DEL)
        ELEMENTS.addElement(new Element("BODY", 0, "HTML", new String[]{"HEAD"}));
        // BR - O EMPTY
        ELEMENTS.addElement(new Element("BR", Element.EMPTY, "BODY", null));
        // BUTTON - - (%flow;)* -(A|%formctrl;|FORM|FIELDSET)
        ELEMENTS.addElement(new Element("BUTTON", 0, "FORM", null));
        // CAPTION - - (%inline;)*
        ELEMENTS.addElement(new Element("CAPTION", Element.INLINE, "TABLE", null));
        // CENTER, 
        ELEMENTS.addElement(new Element("CENTER", Element.INLINE, "BODY", null));
        // CITE - - (%inline;)*
        ELEMENTS.addElement(new Element("CITE", Element.INLINE, "BODY", null));
        // CODE - - (%inline;)*
        ELEMENTS.addElement(new Element("CODE", Element.INLINE, "BODY", null));
        // COL - O EMPTY
        ELEMENTS.addElement(new Element("COL", 0, "COLGROUP", new String[] {"COL"}));
        // COLGROUP - O (COL)*
        ELEMENTS.addElement(new Element("COLGROUP", 0, "TABLE", new String[]{"COLGROUP"}));
        // COMMENT
        ELEMENTS.addElement(new Element("COMMENT", 0, "HTML", null));
        // DEL - - (%flow;)*
        ELEMENTS.addElement(new Element("DEL", 0, "BODY", null));
        // DFN - - (%inline;)*
        ELEMENTS.addElement(new Element("DFN", Element.INLINE, "BODY", null));
        // DIR
        ELEMENTS.addElement(new Element("DIR", 0, "BODY", null));
        // DIV - - (%flow;)*
        ELEMENTS.addElement(new Element("DIV", Element.BLOCK, "BODY", null));
        // DD - O (%flow;)*
        ELEMENTS.addElement(new Element("DD", 0, "DL", new String[]{"DT","DD"}));
        // DL - - (DT|DD)+
        ELEMENTS.addElement(new Element("DL", Element.BLOCK, "BODY", null));
        // DT - O (%inline;)*
        ELEMENTS.addElement(new Element("DT", 0, "DL", new String[]{"DT","DD"}));
        // EM - - (%inline;)*
        ELEMENTS.addElement(new Element("EM", Element.INLINE, "BODY", null));
        // EMBED
        ELEMENTS.addElement(new Element("EMBED", 0, "BODY", null));
        // FIELDSET - - (#PCDATA,LEGEND,(%flow;)*)
        ELEMENTS.addElement(new Element("FIELDSET", 0, "FORM", null));
        // FONT
        ELEMENTS.addElement(new Element("FONT", Element.INLINE, "BODY", null));
        // FORM - - (%block;|SCRIPT)+ -(FORM)
        ELEMENTS.addElement(new Element("FORM", 0, "BODY", null));
        // FRAME - O EMPTY
        ELEMENTS.addElement(new Element("FRAME", Element.EMPTY, "FRAMESET", null));
        // FRAMESET - - ((FRAMESET|FRAME)+ & NOFRAMES?)
        ELEMENTS.addElement(new Element("FRAMESET", 0, "HTML", null));
        // (H1|H2|H3|H4|H5|H6) - - (%inline;)*
        ELEMENTS.addElement(new Element("H1", Element.BLOCK, "BODY", new String[]{"H1","H2","H3","H4","H5","H6","P"}));
        ELEMENTS.addElement(new Element("H2", Element.BLOCK, "BODY", new String[]{"H1","H2","H3","H4","H5","H6","P"}));
        ELEMENTS.addElement(new Element("H3", Element.BLOCK, "BODY", new String[]{"H1","H2","H3","H4","H5","H6","P"}));
        ELEMENTS.addElement(new Element("H4", Element.BLOCK, "BODY", new String[]{"H1","H2","H3","H4","H5","H6","P"}));
        ELEMENTS.addElement(new Element("H5", Element.BLOCK, "BODY", new String[]{"H1","H2","H3","H4","H5","H6","P"}));
        ELEMENTS.addElement(new Element("H6", Element.BLOCK, "BODY", new String[]{"H1","H2","H3","H4","H5","H6","P"}));
        // HEAD O O (%head.content;) +(%head.misc;)
        ELEMENTS.addElement(new Element("HEAD", 0, "HTML", null));
        // HR - O EMPTY
        ELEMENTS.addElement(new Element("HR", Element.EMPTY, "BODY", new String[]{"P"}));
        // HTML O O (%html.content;)
        ELEMENTS.addElement(new Element("HTML", 0, null, null));
        // I - - (%inline;)*
        ELEMENTS.addElement(new Element("I", Element.INLINE, "BODY", null));
        // IFRAME
        ELEMENTS.addElement(new Element("IFRAME", Element.BLOCK, "BODY", null));
        // ILAYER
        ELEMENTS.addElement(new Element("ILAYER", Element.BLOCK, "BODY", null));
        // IMG - O EMPTY
        ELEMENTS.addElement(new Element("IMG", Element.EMPTY, "BODY", null));
        // INPUT - O EMPTY
        ELEMENTS.addElement(new Element("INPUT", Element.EMPTY, "FORM", null));
        // INS - - (%flow;)*
        ELEMENTS.addElement(new Element("INS", 0, "BODY", null));
        // ISINDEX
        ELEMENTS.addElement(new Element("ISINDEX", 0, "HEAD", null));
        // KBD - - (%inline;)*
        ELEMENTS.addElement(new Element("KBD", Element.INLINE, "BODY", null));
        // KEYGEN
        ELEMENTS.addElement(new Element("KEYGEN", 0, "FORM", null));
        // LABEL - - (%inline;)* -(LABEL)
        ELEMENTS.addElement(new Element("LABEL", 0, "FORM", null));
        // LAYER
        ELEMENTS.addElement(new Element("LAYER", Element.BLOCK, "BODY", null));
        // LEGEND - - (%inline;)*
        ELEMENTS.addElement(new Element("LEGEND", Element.INLINE, "FIELDSET", null));
        // LI - O (%flow;)*
        ELEMENTS.addElement(new Element("LI", 0, "BODY", new String[]{"LI"}));
        // LINK - O EMPTY
        ELEMENTS.addElement(new Element("LINK", Element.EMPTY, "HEAD", null));
        // LISTING
        ELEMENTS.addElement(new Element("LISTING", 0, "BODY", null));
        // MAP - - ((%block;) | AREA)+
        ELEMENTS.addElement(new Element("MAP", Element.INLINE, "BODY", null));
        // MARQUEE
        ELEMENTS.addElement(new Element("MARQUEE", 0, "BODY", null));
        // MENU
        ELEMENTS.addElement(new Element("MENU", 0, "BODY", null));
        // META - O EMPTY
        ELEMENTS.addElement(new Element("META", Element.EMPTY, "HEAD", new String[]{"STYLE","TITLE"}));
        // MULTICOL
        ELEMENTS.addElement(new Element("MULTICOL", 0, "BODY", null));
        // NEXTID
        ELEMENTS.addElement(new Element("NEXTID", Element.EMPTY, "BODY", null));
        // NOBR
        ELEMENTS.addElement(new Element("NOBR", Element.INLINE, "BODY", new String[]{}));
        // NOEMBED
        ELEMENTS.addElement(new Element("NOEMBED", 0, "EMBED", null));
        // NOFRAMES - - (BODY) -(NOFRAMES)
        ELEMENTS.addElement(new Element("NOFRAMES", 0, "FRAMESET", null));
        // NOLAYER
        ELEMENTS.addElement(new Element("NOLAYER", 0, "LAYER", null));
        // NOSCRIPT - - (%block;)+
        ELEMENTS.addElement(new Element("NOSCRIPT", 0, new String[]{"HEAD","BODY"}, null));
        // OBJECT - - (PARAM | %flow;)*
        ELEMENTS.addElement(new Element("OBJECT", 0, "BODY", null));
        // OL - - (LI)+
        ELEMENTS.addElement(new Element("OL", Element.BLOCK, "BODY", null));
        // OPTION - O (#PCDATA)
        ELEMENTS.addElement(new Element("OPTION", 0, "SELECT", new String[]{"OPTION"}));
        // OPTGROUP - - (OPTION)+
        ELEMENTS.addElement(new Element("OPTGROUP", 0, "SELECT", new String[]{"OPTION"}));
        // P - O (%inline;)*
        ELEMENTS.addElement(new Element("P", 0, "BODY", new String[]{"P"}));
        // PARAM - O EMPTY
        ELEMENTS.addElement(new Element("PARAM", Element.EMPTY, "OBJECT", null));
        // PLAINTEXT
        ELEMENTS.addElement(new Element("PLAINTEXT", 0, "BODY", null));
        // PRE - - (%inline;)* -(%pre.exclusion;)
        ELEMENTS.addElement(new Element("PRE", 0, "BODY", null));
        // Q - - (%inline;)*
        ELEMENTS.addElement(new Element("Q", Element.INLINE, "BODY", null));
        // RT
        ELEMENTS.addElement(new Element("RT", 0, "RUBY", null));
        // RUBY
        ELEMENTS.addElement(new Element("RUBY", 0, "BODY", null));
        // S
        ELEMENTS.addElement(new Element("S", 0, "BODY", null));
        // SAMP - - (%inline;)*
        ELEMENTS.addElement(new Element("SAMP", Element.INLINE, "BODY", null));
        // SCRIPT - - %Script;
        ELEMENTS.addElement(new Element("SCRIPT", 0, new String[]{"HEAD","BODY"}, null));
        // SELECT - - (OPTGROUP|OPTION)+
        ELEMENTS.addElement(new Element("SELECT", 0, "FORM", new String[]{"SELECT"}));
        // SMALL - - (%inline;)*
        ELEMENTS.addElement(new Element("SMALL", Element.INLINE, "BODY", null));
        // SOUND
        ELEMENTS.addElement(new Element("SOUND", Element.EMPTY, "HEAD", null));
        // SPACER
        ELEMENTS.addElement(new Element("SPACER", Element.EMPTY, "BODY", null));
        // SPAN - - (%inline;)*
        ELEMENTS.addElement(new Element("SPAN", Element.INLINE, "BODY", null));
        // STRIKE
        ELEMENTS.addElement(new Element("STRIKE", Element.INLINE, "BODY", null));
        // STRONG - - (%inline;)*
        ELEMENTS.addElement(new Element("STRONG", Element.INLINE, "BODY", null));
        // STYLE - - %StyleSheet;
        ELEMENTS.addElement(new Element("STYLE", 0, new String[]{"HEAD","BODY"}, new String[]{"STYLE","TITLE","META"}));
        // SUB - - (%inline;)*
        ELEMENTS.addElement(new Element("SUB", Element.INLINE, "BODY", null));
        // SUP - - (%inline;)*
        ELEMENTS.addElement(new Element("SUP", Element.INLINE, "BODY", null));
        // TABLE - - (CAPTION?, (COL*|COLGROUP*), THEAD?, TFOOT?, TBODY+)
        ELEMENTS.addElement(new Element("TABLE", Element.BLOCK, "BODY", null));
        // TBODY O O (TR)+
        ELEMENTS.addElement(new Element("TBODY", Element.BLOCK, "TABLE", new String[]{"TD","THEAD","TR"}));
        // TEXTAREA - - (#PCDATA)
        ELEMENTS.addElement(new Element("TEXTAREA", 0, "FORM", null));
        // TD - O (%flow;)*
        ELEMENTS.addElement(new Element("TD", Element.BLOCK, "TABLE", new String[]{"TD","TH"}));
        // TFOOT - O (TR)+
        ELEMENTS.addElement(new Element("TFOOT", Element.BLOCK, "TABLE", new String[]{"THEAD","TBODY","TD","TR"}));
        // TH - O (%flow;)*
        ELEMENTS.addElement(new Element("TH", Element.BLOCK, "TR", null));
        // THEAD - O (TR)+
        ELEMENTS.addElement(new Element("THEAD", Element.BLOCK, "TABLE", null));
        // TITLE - - (#PCDATA) -(%head.misc;)
        ELEMENTS.addElement(new Element("TITLE", 0, "HEAD", null));
        // TR - O (TH|TD)+
        ELEMENTS.addElement(new Element("TR", Element.BLOCK, "TABLE", new String[]{"TD","TR"}));
        // TT - - (%inline;)*
        ELEMENTS.addElement(new Element("TT", Element.INLINE, "BODY", null));
        // U, 
        ELEMENTS.addElement(new Element("U", Element.INLINE, "BODY", null));
        // UL - - (LI)+
        ELEMENTS.addElement(new Element("UL", Element.BLOCK, "BODY", null));
        // VAR - - (%inline;)*
        ELEMENTS.addElement(new Element("VAR", Element.INLINE, "BODY", null));
        // WBR
        ELEMENTS.addElement(new Element("WBR", Element.EMPTY, "BODY", null));
        // XML
        ELEMENTS.addElement(new Element("XML", 0, "BODY", null));
        // XMP
        ELEMENTS.addElement(new Element("XMP", 0, "BODY", null));
        
        // ...unknown element name...
        ELEMENTS.addElement(new Element("", 0, "BODY", null));
    } // <clinit>()

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
        Element element = getElement(elem.rawname);
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
                    Element pelement = getElement(element.parent);
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
        if (depth > 1 && getElement(element.rawname).isInline()) {
            int size = fElementStack.size();
            fInlineStack.removeAllElements();
            for (int i = 0; i < depth - 1; i++) {
                Info info = (Info)fElementStack.elementAt(size - i - 1);
                Element elem = getElement(info.element.rawname);
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
     * Returns the element information for the specified element name.
     *
     * @param ename The element name.
     */
    protected static Element getElement(Object ename) {

        int length = ELEMENTS.size();
        for (int i = 0; i < length; i++) {
            Element element = (Element)ELEMENTS.elementAt(i);
            if (element.name.equals(ename instanceof String ? (String)ename : ((String[])ename)[0])) {
                return element;
            }
        }
        return (Element)ELEMENTS.lastElement();

    } // getElement(String):Element

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
        Element element = getElement(ename);
        if (ename instanceof String) {
            String string = (String)ename;
            for (int i = length - 1; i >= 0; i--) {
                Info info = (Info)fElementStack.elementAt(i);
                if (info.element.rawname.equals(string)) {
                    return length - i;
                }
                if (blocked && element != null) {
                    Element tag = getElement(info.element.rawname);
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
                    Element tag = getElement(info.element.rawname);
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
     * Element information.
     *
     * @author Andy Clark
     */
    public static class Element {

        //
        // Constants
        //

        /** Inline element. */
        public static final int INLINE = 0x01;

        /** Block element. */
        public static final int BLOCK = 0x02;

        /** Empty element. */
        public static final int EMPTY = 0x04;

        /** Empty string array. */
        private static final String[] EMPTY_CLOSES = {};

        //
        // Data
        //

        /** The element name. */
        public String name;

        /** Informational flags. */
        public int flags;

        /** Natural closing element name. */
        public Object parent;

        /** List of elements this element can close. */
        public String[] closes;

        //
        // Constructors
        //

        /** 
         * Constructs an element object.
         *
         * @param name The element name.
         * @param flags Informational flags
         * @param parent Natural closing parent name.
         * @param closes List of elements this element can close.
         */
        public Element(String name, int flags, Object parent, String[] closes) {
            this.name = name;
            this.flags = flags;
            this.parent = parent;
            this.closes = closes != null ? closes : EMPTY_CLOSES;
        } // <init>(String,int,Object,String[])

        //
        // Public methods
        //

        /** Returns true if this element is an inline element. */
        public boolean isInline() {
            return (flags & INLINE) != 0;
        } // isInline():boolean

        /** Returns true if this element is a block element. */
        public boolean isBlock() {
            return (flags & BLOCK) != 0;
        } // isBlock():boolean

        /** Returns true if this element is an empty element. */
        public boolean isEmpty() {
            return (flags & EMPTY) != 0;
        } // isEmpty():boolean

        /**
         * Returns true if this element can close the specified Element.
         *
         * @param tag The element.
         */
        public boolean closes(String tag) {

            for (int i = 0; i < closes.length; i++) {
                if (closes[i].equals(tag)) {
                    return true;
                }
            }
            return false;

        } // closes(String):boolean

        //
        // Object methods
        //

        /** Returns a hash code for this object. */
        public int hashCode() {
            return name.hashCode();
        } // hashCode():int

        /** Returns true if the objects are equal. */
        public boolean equals(Object o) {
            return name.equals(o);
        } // equals(Object):boolean

    } // class Element

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
