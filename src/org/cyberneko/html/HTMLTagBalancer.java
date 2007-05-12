/* 
 * (C) Copyright 2002, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

package org.cyberneko.html;

import java.util.Hashtable;
import java.util.Stack;
                                          
import org.apache.xerces.util.AugmentationsImpl;
import org.apache.xerces.util.XMLAttributesImpl;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLComponentManager;
import org.apache.xerces.xni.parser.XMLConfigurationException;
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
    implements XMLDocumentFilter, HTMLComponent {

    //
    // Constants
    //

    // features

    /** Include infoset augmentations. */
    protected static final String AUGMENTATIONS = "http://cyberneko.org/html/features/augmentations";

    /** Report errors. */
    protected static final String REPORT_ERRORS = "http://cyberneko.org/html/features/report-errors";

    /** Recognized features. */
    private static final String[] RECOGNIZED_FEATURES = {
        AUGMENTATIONS,
        REPORT_ERRORS,
    };

    /** Recognized features defaults. */
    private static final Boolean[] RECOGNIZED_FEATURES_DEFAULTS = {
        Boolean.FALSE,
        null,
    };

    // properties

    /** Modify HTML element names: { "upper", "lower", "default" }. */
    protected static final String NAMES_ELEMS = "http://cyberneko.org/html/properties/names/elems";

    /** Modify HTML attribute names: { "upper", "lower", "default" }. */
    protected static final String NAMES_ATTRS = "http://cyberneko.org/html/properties/names/attrs";

    /** Error reporter. */
    protected static final String ERROR_REPORTER = "http://cyberneko.org/html/properties/error-reporter";

    /** Recognized properties. */
    private static final String[] RECOGNIZED_PROPERTIES = {
        NAMES_ELEMS,
        NAMES_ATTRS,
        ERROR_REPORTER,
    };

    /** Recognized properties defaults. */
    private static final Object[] RECOGNIZED_PROPERTIES_DEFAULTS = {
        "upper",
        "lower",
        null,
    };

    // modify HTML names

    /** Don't modify HTML names. */
    protected static final short NAMES_NO_CHANGE = 0;

    /** Match HTML element names. */
    protected static final short NAMES_MATCH = 0;

    /** Uppercase HTML names. */
    protected static final short NAMES_UPPERCASE = 1;

    /** Lowercase HTML names. */
    protected static final short NAMES_LOWERCASE = 2;

    // static vars

    /** Synthesized event info item. */
    protected static final HTMLEventInfo SYNTHESIZED_ITEM = new SynthesizedItem();

    //
    // Data
    //

    // features

    /** Include infoset augmentations. */
    protected boolean fAugmentations;
    
    /** Report errors. */
    protected boolean fReportErrors;

    // properties

    /** Modify HTML element names. */
    protected short fNamesElems;

    /** Modify HTML attribute names. */
    protected short fNamesAttrs;

    /** Error reporter. */
    protected HTMLErrorReporter fErrorReporter;

    // handlers

    /** The document handler. */
    protected XMLDocumentHandler fDocumentHandler;

    // state

    /** The element stack. */
    protected final Stack fElementStack = new Stack();

    /** The inline stack. */
    protected final Stack fInlineStack = new Stack();

    /** True if root element has been seen. */
    protected boolean fSeenRootElement;

    // temp vars

    /** A qualified name. */
    private final QName fQName = new QName();

    /** Empty attributes. */
    private final XMLAttributes fEmptyAttrs = new XMLAttributesImpl();

    /** Augmentations for synthesize values. */
    private final Augmentations fSynthesizedAugs = new AugmentationsImpl();

    //
    // HTMLComponent methods
    //

    /** Returns the default state for a feature. */
    public Boolean getFeatureDefault(String featureId) {
        int length = RECOGNIZED_FEATURES != null ? RECOGNIZED_FEATURES.length : 0;
        for (int i = 0; i < length; i++) {
            if (RECOGNIZED_FEATURES[i].equals(featureId)) {
                return RECOGNIZED_FEATURES_DEFAULTS[i];
            }
        }
        return null;
    } // getFeatureDefault(String):Boolean

    /** Returns the default state for a property. */
    public Object getPropertyDefault(String propertyId) {
        int length = RECOGNIZED_PROPERTIES != null ? RECOGNIZED_PROPERTIES.length : 0;
        for (int i = 0; i < length; i++) {
            if (RECOGNIZED_PROPERTIES[i].equals(propertyId)) {
                return RECOGNIZED_PROPERTIES_DEFAULTS[i];
            }
        }
        return null;
    } // getPropertyDefault(String):Object

    //
    // XMLComponent methods
    //

    /** Returns recognized features. */
    public String[] getRecognizedFeatures() {
        return RECOGNIZED_FEATURES;
    } // getRecognizedFeatures():String[]

    /** Returns recognized properties. */
    public String[] getRecognizedProperties() {
        return RECOGNIZED_PROPERTIES;
    } // getRecognizedProperties():String[]

    /** Resets the component. */
    public void reset(XMLComponentManager manager)
        throws XMLConfigurationException {

        // get features
        fAugmentations = manager.getFeature(AUGMENTATIONS);
        fReportErrors = manager.getFeature(REPORT_ERRORS);

        // get properties
        fNamesElems = getNamesValue(String.valueOf(manager.getProperty(NAMES_ELEMS)));
        fNamesAttrs = getNamesValue(String.valueOf(manager.getProperty(NAMES_ATTRS)));
        fErrorReporter = (HTMLErrorReporter)manager.getProperty(ERROR_REPORTER);

    } // reset(XMLComponentManager)

    /** Sets a feature. */
    public void setFeature(String featureId, boolean state)
        throws XMLConfigurationException {

        if (featureId.equals(AUGMENTATIONS)) {
            fAugmentations = state;
            return;
        }
        if (featureId.equals(REPORT_ERRORS)) {
            fReportErrors = state;
            return;
        }

    } // setFeature(String,boolean)

    /** Sets a property. */
    public void setProperty(String propertyId, Object value)
        throws XMLConfigurationException {
        
        if (propertyId.equals(NAMES_ELEMS)) {
            fNamesElems = getNamesValue(String.valueOf(value));
            return;
        }
        if (propertyId.equals(NAMES_ATTRS)) {
            fNamesAttrs = getNamesValue(String.valueOf(value));
            return;
        }

    } // setProperty(String,Object)

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
            if (fReportErrors) {
                fErrorReporter.reportError("HTML2000", null);
            }
            String ename = modifyName("html", fNamesElems);
            fQName.setValues(null, ename, ename, null);
            startElement(fQName, emptyAttributes(), synthesizedAugs());
            endElement(fQName, synthesizedAugs());
        }

        // pop all remaining elements
        else {
            int length = fElementStack.size();
            for (int i = 0; i < length; i++) {
                Info info = (Info)fElementStack.peek();
                if (fReportErrors) {
                    String ename = modifyName(info.element.rawname, fNamesElems);
                    fErrorReporter.reportWarning("HTML2001", new Object[]{ename});
                }
                endElement(info.element, synthesizedAugs());
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
        throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.startPrefixMapping(prefix, uri, augs);
        }
    } // startPrefixMapping(String,String,Augmentations)

    /** Start element. */
    public void startElement(QName elem, XMLAttributes attrs, Augmentations augs)
        throws XNIException {
        
        // get element information
        HTMLElements.Element element = HTMLElements.getElement(elem.rawname);
        String ename = modifyName(elem.rawname, fNamesElems);

        // check proper parent
        if (element.parent != null) {
            if (!fSeenRootElement) {
                String pname = element.parent instanceof String ? (String)element.parent : ((String[])element.parent)[0];
                pname = modifyName(pname, fNamesElems);
                if (fReportErrors) {
                    fErrorReporter.reportWarning("HTML2002", new Object[]{ename,pname});
                }
                QName qname = new QName(null, pname, pname, null);
                startElement(qname, emptyAttributes(), synthesizedAugs());
            }
            else {
                int depth = getDepth(element.parent, false);
                if (depth == -1) {
                    String pname = element.parent instanceof String
                                 ? (String)element.parent 
                                 : ((String[])element.parent)[0];
                    pname = modifyName(pname, fNamesElems);
                    HTMLElements.Element pelement = HTMLElements.getElement(pname);
                    if (pelement != null) {
                        int pdepth = getDepth(pelement.parent, false);
                        if (pdepth != -1) {
                            for (int i = 1; i < pdepth; i++) {
                                Info info = (Info)fElementStack.peek();
                                if (fReportErrors) {
                                    String iname = modifyName(info.element.rawname, fNamesElems);
                                    String ppname = pelement.parent instanceof String
                                                  ? (String)pelement.parent
                                                  : ((String[])pelement.parent)[0];
                                    ppname = modifyName(ppname, fNamesElems);
                                    fErrorReporter.reportWarning("HTML2003", new Object[]{iname,ename,pname,ppname});
                                }
                                endElement(info.element, synthesizedAugs());
                            }
                            QName qname = new QName(null, pname, pname, null);
                            if (fReportErrors) {
                                pname = modifyName(pname, fNamesElems);
                                fErrorReporter.reportWarning("HTML2004", new Object[]{ename,pname});
                            }
                            startElement(qname, emptyAttributes(), synthesizedAugs());
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
                if (fReportErrors) {
                    String iname = modifyName(info.element.rawname, fNamesElems);
                    fErrorReporter.reportWarning("HTML2005", new Object[]{ename,iname});
                }
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

        // modify element and attribute names
        if (HTMLElements.getElement(elem.rawname, null) != null) {
            QName qname = elem;
            qname.setValues(null, ename, ename, null);
            int attrCount = attrs != null ? attrs.getLength() : 0;
            qname = fQName;
            for (int i = 0; i < attrCount; i++) {
                attrs.getName(i, qname);
                String aname = modifyName(qname.rawname, fNamesAttrs);
                qname.setValues(null, aname, aname, null);
                attrs.setName(i, qname);
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
            String ename = modifyName("body", fNamesElems);
            fQName.setValues(null, ename, ename, null);
            if (fReportErrors) {
                fErrorReporter.reportWarning("HTML2006", new Object[]{ename});
            }
            startElement(fQName, emptyAttributes(), synthesizedAugs());
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
        
        // get element information
        HTMLElements.Element elem = null;
        if (fReportErrors) {
            elem = HTMLElements.getElement(element.rawname);
        }

        // find unbalanced inline elements
        int depth = getDepth(element.rawname, true);
        if (depth > 1 && HTMLElements.getElement(element.rawname).isInline()) {
            int size = fElementStack.size();
            fInlineStack.removeAllElements();
            for (int i = 0; i < depth - 1; i++) {
                Info info = (Info)fElementStack.elementAt(size - i - 1);
                HTMLElements.Element pelem = HTMLElements.getElement(info.element.rawname);
                if (pelem.isInline()) {
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
            if (fReportErrors && i < depth - 1) {
                String ename = modifyName(element.rawname, fNamesElems);
                String iname = modifyName(info.element.rawname, fNamesElems);
                fErrorReporter.reportWarning("HTML2007", new Object[]{ename,iname});
            }
            if (fDocumentHandler != null) {
                fDocumentHandler.endElement(info.element, augs);
            }
        }

        // re-open inline elements
        if (depth > 1) {
            int size = fInlineStack.size();
            for (int i = 0; i < size; i++) {
                Info info = (Info)fInlineStack.pop();
                XMLAttributes attributes = info.attributes;
                if (attributes == null) {
                    attributes = emptyAttributes();
                }
                if (fReportErrors) {
                    String iname = modifyName(info.element.rawname, fNamesElems);
                    fErrorReporter.reportWarning("HTML2008", new Object[]{iname});
                }
                startElement(info.element, attributes, synthesizedAugs());
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
    // Protected methods
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
                if (info.element.rawname.equalsIgnoreCase(string)) {
                    return length - i;
                }
                if (blocked && element != null && !element.isContainer()) {
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
                    if (info.element.rawname.equalsIgnoreCase(array[j])) {
                        return length - i;
                    }
                }
                if (blocked && element != null && !element.isContainer()) {
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

    /** Modifies the given name based on the specified mode. */
    protected static final String modifyName(String name, short mode) {
        switch (mode) {
            case NAMES_UPPERCASE: return name.toUpperCase();
            case NAMES_LOWERCASE: return name.toLowerCase();
        }
        return name;
    } // modifyName(String,short):String

    /**
     * Converts HTML names string value to constant value. 
     *
     * @see #NAMES_NO_CHANGE
     * @see #NAMES_LOWERCASE
     * @see #NAMES_UPPERCASE
     */
    protected static final short getNamesValue(String value) {
        if (value.equals("lower")) {
            return NAMES_LOWERCASE;
        }
        if (value.equals("upper")) {
            return NAMES_UPPERCASE;
        }
        return NAMES_NO_CHANGE;
    } // getNamesValue(String):short

    /** Returns a set of empty attributes. */
    protected final XMLAttributes emptyAttributes() {
        fEmptyAttrs.removeAllAttributes();
        return fEmptyAttrs;
    } // emptyAttributes():XMLAttributes

    /** Returns an augmentations object with a synthesized item added. */
    protected final Augmentations synthesizedAugs() {
        Augmentations augs = null;
        if (fAugmentations) {
            augs = fSynthesizedAugs;
            augs.clear();
            augs.putItem(AUGMENTATIONS, SYNTHESIZED_ITEM);
        }
        return augs;
    } // synthesizedAugs():Augmentations

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
            }
        } // <init>(QName,XMLAttributes)

    } // class Info

    /**
     * Synthesized infoset item.
     *
     * @author Andy Clark
     */
    protected static class SynthesizedItem
        implements HTMLEventInfo {

        //
        // HTMLEventInfo methods
        //

        /** Returns true if this corresponding event was synthesized. */
        public boolean isSynthesized() {
            return true;
        } // isSynthesized():boolean

        //
        // Object methods
        //

        /** Returns a string representation of this object. */
        public String toString() {
            return "synthesized";
        } // toString():String

    } // class SynthesizedItem

} // class HTMLTagBalancer
