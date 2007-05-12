/* 
 * (C) Copyright 2004, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

package org.cyberneko.html.filters;

import org.cyberneko.html.HTMLEventInfo;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.apache.xerces.util.AugmentationsImpl;
import org.apache.xerces.util.XMLChar;
import org.apache.xerces.util.XMLStringBuffer;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLComponentManager;
import org.apache.xerces.xni.parser.XMLConfigurationException;

/**
 * This filter purifies the HTML input to ensure XML well-formedness.
 * The purification process includes:
 * <ul>
 * <li>fixing illegal characters in the document, including
 *  <ul>
 *  <li>element and attribute names,
 *  <li>processing instruction target and data,
 *  <li>document text;
 *  </ul>
 * <li>ensuring the string "--" does not appear in the content of
 *     a comment; and
 * <li>ensuring the string "]]>" does not appear in the content of
 *     a CDATA section.
 * </ul>
 * <p>
 * Illegal characters in XML names are converted to the character 
 * sequence "_u####_" where "####" is the value of the Unicode 
 * character represented in hexadecimal. Whereas illegal characters
 * appearing in document content is converted to the character
 * sequence "\\u####".
 * <p>
 * In comments, the character '-' is replaced by the character
 * sequence "- " to prevent "--" from ever appearing in the comment
 * content. For CDATA sections, the character ']' is replaced by
 * the character sequence "] " to prevent "]]" from appearing.
 * 
 * @author Andy Clark
 * 
 * @version $Id$
 */
public class Purifier
    extends DefaultFilter {

    //
    // Constants
    //

    /** Namespaces. */
    protected static final String NAMESPACES = "http://xml.org/sax/features/namespaces";

    /** Include infoset augmentations. */
    protected static final String AUGMENTATIONS = "http://cyberneko.org/html/features/augmentations";

    /** Recognized features. */
    private static final String[] RECOGNIZED_FEATURES = {
        NAMESPACES,
        AUGMENTATIONS,
    };

    /** Recognized features defaults. */
    private static final Boolean[] RECOGNIZED_FEATURES_DEFAULTS = {
        null,
        null,
    };

    // static vars

    /** Synthesized event info item. */
    protected static final HTMLEventInfo SYNTHESIZED_ITEM = 
        new HTMLEventInfo.SynthesizedItem();

    //
    // Data
    //

    // features

    /** Namespaces. */
    protected boolean fNamespaces;

    /** Augmentations. */
    protected boolean fAugmentations;

    // state

    /** True if the doctype declaration was seen. */
    protected boolean fSeenDoctype;

    /** True if root element was seen. */
    protected boolean fSeenRootElement;

    /** True if inside a CDATA section. */
    protected boolean fInCDATASection;

    // doctype declaration info

    /** Public identifier of doctype declaration. */
    protected String fPublicId;

    /** System identifier of doctype declaration. */
    protected String fSystemId;

    // temp vars

    /** Qualified name. */
    private QName fQName = new QName();

    /** Augmentations. */
    private final Augmentations fInfosetAugs = new AugmentationsImpl();

    /** String buffer. */
    private final XMLStringBuffer fStringBuffer = new XMLStringBuffer();

    //
    // XMLComponent methods
    //

    public void reset(XMLComponentManager manager) 
        throws XMLConfigurationException {

        // state
        fInCDATASection = false;

        // features
        fNamespaces = manager.getFeature(NAMESPACES);
        fAugmentations = manager.getFeature(AUGMENTATIONS);

    } // reset(XMLComponentManager)

    //
    // XMLDocumentHandler methods
    //

    /** Start document. */
    public void startDocument(XMLLocator locator, String encoding,
                              Augmentations augs) throws XNIException {
        handleStartDocument();
        super.startDocument(locator, encoding, augs);
    } // startDocument(XMLLocator,String,Augmentations)

    /** Start document. */
    public void startDocument(XMLLocator locator, String encoding,
                              NamespaceContext nscontext, Augmentations augs)
        throws XNIException {
        handleStartDocument();
        super.startDocument(locator, encoding, nscontext, augs);
    } // startDocument(XMLLocator,NamespaceContext,String,Augmentations)

    /** Comment. */
    public void comment(XMLString text, Augmentations augs)
        throws XNIException {
        StringBuffer str = new StringBuffer(purifyText(text).toString());
        int length = str.length();
        for (int i = length-1; i >= 0; i--) {
            char c = str.charAt(i);
            if (c == '-') {
                str.insert(i + 1, ' ');
            }
        }
        fStringBuffer.length = 0;
        fStringBuffer.append(str.toString());
        text = fStringBuffer;
        super.comment(text, augs);
    } // comment(XMLString,Augmentations)

    /** Processing instruction. */
    public void processingInstruction(String target, XMLString data,
                                      Augmentations augs)
        throws XNIException {
        target = purifyName(target, true);
        data = purifyText(data);
        super.processingInstruction(target, data, augs);
    } // processingInstruction(String,XMLString,Augmentations)

    /** Doctype declaration. */
    public void doctypeDecl(String root, String pubid, String sysid,
                            Augmentations augs) throws XNIException {
        fSeenDoctype = true;
        // NOTE: It doesn't matter what the root element name is because
        //       it must match the root element. -Ac
        fPublicId = pubid;
        fSystemId = sysid;
        // NOTE: If the public identifier is specified, then a system
        //       identifier must also be specified. -Ac
        if (fPublicId != null && fSystemId == null) {
            fSystemId = "";
        }
        // NOTE: Can't save the augmentations because the object state
        //       is transient. -Ac
    } // doctypeDecl(String,String,String,Augmentations)

    /** Start element. */
    public void startElement(QName element, XMLAttributes attrs,
                             Augmentations augs) throws XNIException {
        handleStartElement(element, attrs);
        super.startElement(element, attrs, augs);
    } // startElement(QName,XMLAttributes,Augmentations)

    /** Empty element. */
    public void emptyElement(QName element, XMLAttributes attrs,
                             Augmentations augs) throws XNIException {
        handleStartElement(element, attrs);
        super.emptyElement(element, attrs, augs);
    } // emptyElement(QName,XMLAttributes,Augmentations)

    /** Start CDATA section. */
    public void startCDATA(Augmentations augs) throws XNIException {
        fInCDATASection = true;
        super.startCDATA(augs);
    } // startCDATA(Augmentations)

    /** End CDATA section. */
    public void endCDATA(Augmentations augs) throws XNIException {
        fInCDATASection = false;
        super.endCDATA(augs);
    } // endCDATA(Augmentations)

    /** Characters. */
    public void characters(XMLString text, Augmentations augs)
        throws XNIException {
        text = purifyText(text);
        if (fInCDATASection) {
            StringBuffer str = new StringBuffer(text.toString());
            int length = str.length();
            for (int i = length-1; i >= 0; i--) {
                char c = str.charAt(i);
                if (c == ']') {
                    str.insert(i + 1, ' ');
                }
            }
            fStringBuffer.length = 0;
            fStringBuffer.append(str.toString());
            text = fStringBuffer;
        }
        super.characters(text,augs);
    } // characters(XMLString,Augmentations)

    /** End element. */
    public void endElement(QName element, Augmentations augs)
        throws XNIException {
        element = purifyQName(element);
        super.endElement(element, augs);
    } // endElement(QName,Augmentations)

    //
    // Protected methods
    //

    /** Handle start document. */
    protected void handleStartDocument() {
        fSeenDoctype = false;
        fSeenRootElement = false;
    } // handleStartDocument()

    /** Handle start element. */
    protected void handleStartElement(QName element, XMLAttributes attrs) {

        // handle element and attributes
        element = purifyQName(element);
        int attrCount = attrs != null ? attrs.getLength() : 0;
        for (int i = 0; i < attrCount; i++) {
            attrs.getName(i, fQName);
            attrs.setName(i, purifyQName(fQName));
        }

        // synthesize doctype declaration
        if (!fSeenRootElement && fSeenDoctype) {
            Augmentations augs = synthesizedAugs();
            super.doctypeDecl(element.rawname, fPublicId, fSystemId, augs);
        }

        // mark start element as seen
        fSeenRootElement = true;

    } // handleStartElement(QName,XMLAttributes)

    /** Returns an augmentations object with a synthesized item added. */
    protected final Augmentations synthesizedAugs() {
        Augmentations augs = null;
        if (fAugmentations) {
            augs = fInfosetAugs;
            Class cls = augs.getClass();
            Method method = null;
            try {
                method = cls.getMethod("clear", null);
            }
            catch (NoSuchMethodException e) {
                try {
                    method = cls.getMethod("removeAllItems", null);
                }
                catch (NoSuchMethodException e2) {
                    // NOTE: This should not happen! -Ac
                    augs = new AugmentationsImpl();
                }
            }
            if (method != null) {
                try {
                    method.invoke(augs, null);
                }
                catch (IllegalAccessException e) {
                    // NOTE: This should not happen! -Ac
                    augs = new AugmentationsImpl();
                } 
                catch (InvocationTargetException e) {
                    // NOTE: This should not happen! -Ac
                    augs = new AugmentationsImpl();
                } 
            }
            augs.putItem(AUGMENTATIONS, SYNTHESIZED_ITEM);
        }
        return augs;
    } // synthesizedAugs():Augmentations

    //
    // Protected methods
    //

    /** Purify qualified name. */
    protected QName purifyQName(QName qname) {
        qname.prefix = purifyName(qname.prefix, true);
        qname.localpart = purifyName(qname.localpart, true);
        qname.rawname = purifyName(qname.rawname, false);
        return qname;
    } // purifyQName(QName):QName

    /** Purify name. */
    protected String purifyName(String name, boolean localpart) {
        if (name == null) {
            return name;
        }
        StringBuffer str = new StringBuffer();
        int length = name.length();
        boolean seenColon = localpart;
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);
            if (i == 0) {
                if (!XMLChar.isNameStart(c)) {
                    str.append("_u"+toHexString(c,4)+"_");
                }
                else {
                    str.append(c);
                }
            }
            else {
                if ((fNamespaces && c == ':' && seenColon) || !XMLChar.isName(c)) {
                    str.append("_u"+toHexString(c,4)+"_");
                }
                else {
                    str.append(c);
                }
                seenColon = seenColon || c == ':';
            }
        }
        return str.toString();
    } // purifyName(String):String

    /** Purify content. */
    protected XMLString purifyText(XMLString text) {
        fStringBuffer.length = 0;
        for (int i = 0; i < text.length; i++) {
            char c = text.ch[text.offset+i];
            if (XMLChar.isInvalid(c)) {
                fStringBuffer.append("\\u"+toHexString(c,4));
            }
            else {
                fStringBuffer.append(c);
            }
        }
        return fStringBuffer;
    } // purifyText(XMLString):XMLString

    //
    // Protected static methods
    //

    /** Returns a padded hexadecimal string for the given value. */
    protected static String toHexString(int c, int padlen) {
        StringBuffer str = new StringBuffer(padlen);
        str.append(Integer.toHexString(c));
        int len = padlen - str.length();
        for (int i = 0; i < len; i++) {
            str.insert(0, '0');
        }
        return str.toString().toUpperCase();
    } // toHexString(int,int):String

} // class Purifier
