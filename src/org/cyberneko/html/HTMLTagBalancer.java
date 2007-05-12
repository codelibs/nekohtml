/* 
 * (C) Copyright 2002, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

package org.cyberneko.html;

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
 * document authors make. This tag balancer can:
 * <ul>
 * <li>add missing parent elements;
 * <li>automatically close elements with optional end tags; and
 * <li>handle mis-matched inline element tags.
 * </ul>
 * <p>
 * This component recognizes the following features:
 * <ul>
 * <li>http://cyberneko.org/html/features/augmentations
 * <li>http://cyberneko.org/html/features/report-errors
 * </ul>
 * <p>
 * This component recognizes the following properties:
 * <ul>
 * <li>http://cyberneko.org/html/properties/names/elems
 * <li>http://cyberneko.org/html/properties/names/attrs
 * <li>http://cyberneko.org/html/properties/error-reporter
 * </ul>
 *
 * @see HTMLElements
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
        null,
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
        null,
        null,
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
    protected final InfoStack fElementStack = new InfoStack();

    /** The inline stack. */
    protected final InfoStack fInlineStack = new InfoStack();

    /** True if root element has been seen. */
    protected boolean fSeenRootElement;

    /** 
     * True if seen the end of the document element. In other words, 
     * this variable is set to false <em>until</em> the end &lt;/HTML&gt; 
     * tag is seen (or synthesized). This is used to ensure that 
     * extraneous events after the end of the document element do not 
     * make the document stream ill-formed.
     */
    protected boolean fSeenRootElementEnd;

    // temp vars

    /** A qualified name. */
    private final QName fQName = new QName();

    /** Empty attributes. */
    private final XMLAttributes fEmptyAttrs = new XMLAttributesImpl();

    /** Augmentations. */
    private final Augmentations fInfosetAugs = new AugmentationsImpl();

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
        
        fElementStack.top = 0;
        fSeenRootElement = false;
        fSeenRootElementEnd = false;
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
            int length = fElementStack.top;
            for (int i = 0; i < length; i++) {
                Info info = fElementStack.peek();
                if (fReportErrors) {
                    String ename = info.qname.rawname;
                    fErrorReporter.reportWarning("HTML2001", new Object[]{ename});
                }
                endElement(info.qname, synthesizedAugs());
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
        
        // check for end of document
        if (fSeenRootElementEnd) {
            return;
        }

        // call handler
        if (fDocumentHandler != null) {
            fDocumentHandler.startPrefixMapping(prefix, uri, augs);
        }

    } // startPrefixMapping(String,String,Augmentations)

    /** Start element. */
    public void startElement(QName elem, XMLAttributes attrs, Augmentations augs)
        throws XNIException {
        
        // check for end of document
        if (fSeenRootElementEnd) {
            return;
        }

        // get element information
        HTMLElements.Element element = HTMLElements.getElement(elem.rawname);

        // check proper parent
        if (element.parent != null) {
            if (!fSeenRootElement) {
                String pname = element.parent[0].name;
                pname = modifyName(pname, fNamesElems);
                if (fReportErrors) {
                    String ename = elem.rawname;
                    fErrorReporter.reportWarning("HTML2002", new Object[]{ename,pname});
                }
                QName qname = new QName(null, pname, pname, null);
                startElement(qname, emptyAttributes(), synthesizedAugs());
            }
            else {
                HTMLElements.Element pelement = element.parent[0];
                int depth = getParentDepth(element.parent);
                if (depth == -1) {
                    String pname = pelement.name;
                    pname = modifyName(pname, fNamesElems);
                    int pdepth = getParentDepth(pelement.parent);
                    if (pdepth != -1) {
                        for (int i = 1; i < pdepth; i++) {
                            Info info = fElementStack.peek();
                            if (fReportErrors) {
                                String iname = modifyName(info.qname.rawname, fNamesElems);
                                String ename = elem.rawname;
                                String ppname = pelement.parent[0].name;
                                ppname = modifyName(ppname, fNamesElems);
                                fErrorReporter.reportWarning("HTML2003", new Object[]{iname,ename,pname,ppname});
                            }
                            endElement(info.qname, synthesizedAugs());
                        }
                        QName qname = new QName(null, pname, pname, null);
                        if (fReportErrors) {
                            String ename = elem.rawname;
                            fErrorReporter.reportWarning("HTML2004", new Object[]{ename,pname});
                        }
                        startElement(qname, emptyAttributes(), synthesizedAugs());
                    }
                }
            }
        }

        // close previous elements
        if (element.closes != null) {
            int length = fElementStack.top;
            for (int i = length - 1; i >= 0; i--) {
                Info info = fElementStack.peek();
                if (element.closes(info.element.code)) {
                    if (fReportErrors) {
                        String ename = elem.rawname;
                        String iname = info.qname.rawname;
                        fErrorReporter.reportWarning("HTML2005", new Object[]{ename,iname});
                    }
                    for (int j = length - 1; j >= i; j--) {
                        info = fElementStack.pop();
                        if (fDocumentHandler != null) {
                            fDocumentHandler.endElement(info.qname, null);
                        }
                    }
                    length = i;
                    continue;
                }
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
            fElementStack.push(new Info(element, elem, inline ? attrs : null));
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

        // check for end of document
        if (fSeenRootElementEnd) {
            return;
        }

        // call handler
        if (fDocumentHandler != null) {
            fDocumentHandler.startGeneralEntity(name, id, encoding, augs);
        }

    } // startGeneralEntity(String,XMLResourceIdentifier,String,Augmentations)

    /** Text declaration. */
    public void textDecl(String version, String encoding, Augmentations augs)
        throws XNIException {
        
        // check for end of document
        if (fSeenRootElementEnd) {
            return;
        }

        // call handler
        if (fDocumentHandler != null) {
            fDocumentHandler.textDecl(version, encoding, augs);
        }

    } // textDecl(String,String,Augmentations)

    /** End entity. */
    public void endGeneralEntity(String name, Augmentations augs) throws XNIException {
        
        // check for end of document
        if (fSeenRootElementEnd) {
            return;
        }

        // call handler
        if (fDocumentHandler != null) {
            fDocumentHandler.endGeneralEntity(name, augs);
        }

    } // endGeneralEntity(String,Augmentations)

    /** Start CDATA section. */
    public void startCDATA(Augmentations augs) throws XNIException {
        
        // check for end of document
        if (fSeenRootElementEnd) {
            return;
        }

        // call handler
        if (fDocumentHandler != null) {
            fDocumentHandler.startCDATA(augs);
        }

    } // startCDATA(Augmentations)

    /** End CDATA section. */
    public void endCDATA(Augmentations augs) throws XNIException {

        // check for end of document
        if (fSeenRootElementEnd) {
            return;
        }

        // call handler
        if (fDocumentHandler != null) {
            fDocumentHandler.endCDATA(augs);
        }

    } // endCDATA(Augmentations)

    /** Characters. */
    public void characters(XMLString text, Augmentations augs) throws XNIException {

        // check for end of document
        if (fSeenRootElementEnd) {
            return;
        }

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

        // handle character content in head
        // NOTE: This fequently happens when the document looks like:
        //       <title>Title</title>
        //       And here's some text.
        else {
            Info info = fElementStack.peek();
            if (info.element.code == HTMLElements.HEAD) {
                String hname = modifyName("head", fNamesElems);
                String bname = modifyName("body", fNamesElems);
                if (fReportErrors) {
                    fErrorReporter.reportWarning("HTML2009", new Object[]{hname,bname});
                }
                fQName.setValues(null, hname, hname, null);
                endElement(fQName, synthesizedAugs());
                fQName.setValues(null, bname, bname, null);
                startElement(fQName, emptyAttributes(), synthesizedAugs());
            }
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
        
        // is there anything to do?
        if (fSeenRootElementEnd) {
            return;
        }
        
        // get element information
        HTMLElements.Element elem = HTMLElements.getElement(element.rawname);

        // check for end of document
        if (elem.code == HTMLElements.HTML) {
            fSeenRootElementEnd = true;
        }

        // find unbalanced inline elements
        int depth = getElementDepth(elem);
        if (depth > 1 && elem.isInline()) {
            int size = fElementStack.top;
            fInlineStack.top = 0;
            for (int i = 0; i < depth - 1; i++) {
                Info info = fElementStack.data[size - i - 1];
                HTMLElements.Element pelem = info.element;
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
            Info info = fElementStack.pop();
            if (fReportErrors && i < depth - 1) {
                String ename = modifyName(element.rawname, fNamesElems);
                String iname = info.qname.rawname;
                fErrorReporter.reportWarning("HTML2007", new Object[]{ename,iname});
            }
            if (fDocumentHandler != null) {
                fDocumentHandler.endElement(info.qname, augs);
            }
        }

        // re-open inline elements
        if (depth > 1) {
            int size = fInlineStack.top;
            for (int i = 0; i < size; i++) {
                Info info = (Info)fInlineStack.pop();
                XMLAttributes attributes = info.attributes;
                if (attributes == null) {
                    attributes = emptyAttributes();
                }
                if (fReportErrors) {
                    String iname = info.qname.rawname;
                    fErrorReporter.reportWarning("HTML2008", new Object[]{iname});
                }
                startElement(info.qname, attributes, synthesizedAugs());
            }
        }

    } // endElement(QName,Augmentations)

    /** End prefix mapping. */
    public void endPrefixMapping(String prefix, Augmentations augs)
        throws XNIException {
        
        // check for end of document
        if (fSeenRootElementEnd) {
            return;
        }

        // call handler
        if (fDocumentHandler != null) {
            fDocumentHandler.endPrefixMapping(prefix, augs);
        }

    } // endPrefixMapping(String,Augmentations)

    //
    // Protected methods
    //

    /**
     * Returns the depth of the open tag associated with the specified
     * element name or -1 if no matching element is found.
     *
     * @param element The element.
     */
    protected final int getElementDepth(HTMLElements.Element element) {
        for (int i = fElementStack.top - 1; i >= 0; i--) {
            Info info = fElementStack.data[i];
            if (info.element.code == element.code) {
                return fElementStack.top - i;
            }
            if (info.element.isBlock()) {
                break;
            }
        }
        return -1;
    } // getElementDepth(HTMLElements.Element)

    /**
     * Returns the depth of the open tag associated with the specified
     * element parent names or -1 if no matching element is found.
     *
     * @param parents The parent elements.
     */
    protected int getParentDepth(HTMLElements.Element[] parents) {
        for (int i = fElementStack.top - 1; i >= 0; i--) {
            Info info = fElementStack.data[i];
            for (int j = 0; j < parents.length; j++) {
                if (info.element.code == parents[j].code) {
                    return fElementStack.top - i;
                }
            }
        }
        return -1;
    } // getParentDepth(HTMLElements.Element[]):int

    /** Returns a set of empty attributes. */
    protected final XMLAttributes emptyAttributes() {
        fEmptyAttrs.removeAllAttributes();
        return fEmptyAttrs;
    } // emptyAttributes():XMLAttributes

    /** Returns an augmentations object with a synthesized item added. */
    protected final Augmentations synthesizedAugs() {
        Augmentations augs = null;
        if (fAugmentations) {
            augs = fInfosetAugs;
            augs.clear();
            augs.putItem(AUGMENTATIONS, SYNTHESIZED_ITEM);
        }
        return augs;
    } // synthesizedAugs():Augmentations

    //
    // Protected static methods
    //

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

        /** The element. */
        public HTMLElements.Element element;

        /** The element qualified name. */
        public QName qname;

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
        public Info(HTMLElements.Element element, QName qname) {
            this(element, qname, null);
        } // <init>(HTMLElements.Element,QName)

        /**
         * Creates an element information object.
         * <p>
         * <strong>Note:</strong>
         * This constructor makes a copy of the element information.
         *
         * @param element The element qualified name.
         * @param attributes The element attributes.
         */
        public Info(HTMLElements.Element element,
                    QName qname, XMLAttributes attributes) {
            this.element = element;
            this.qname = new QName(qname);
            if (attributes != null) {
                int length = attributes.getLength();
                if (length > 0) {
                    QName aqname = new QName();
                    XMLAttributes newattrs = new XMLAttributesImpl();
                    for (int i = 0; i < length; i++) {
                        attributes.getName(i, aqname);
                        String type = attributes.getType(i);
                        String value = attributes.getValue(i);
                        String nonNormalizedValue = attributes.getNonNormalizedValue(i);
                        boolean specified = attributes.isSpecified(i);
                        newattrs.addAttribute(aqname, type, value);
                        newattrs.setNonNormalizedValue(i, nonNormalizedValue);
                        newattrs.setSpecified(i, specified);
                    }
                    this.attributes = newattrs;
                }
            }
        } // <init>(HTMLElements.Element,QName,XMLAttributes)

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

        // location information

        /** Returns the line number of the beginning of this event.*/
        public int getBeginLineNumber() {
            return -1;
        } // getBeginLineNumber():int

        /** Returns the column number of the beginning of this event.*/
        public int getBeginColumnNumber() { 
            return -1;
        } // getBeginColumnNumber():int

        /** Returns the line number of the end of this event.*/
        public int getEndLineNumber() {
            return -1;
        } // getEndLineNumber():int

        /** Returns the column number of the end of this event.*/
        public int getEndColumnNumber() {
            return -1;
        } // getEndColumnNumber():int

        // other information

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

    /** Unsynchronized stack of element information. */
    public static class InfoStack {

        //
        // Data
        //

        /** The top of the stack. */
        public int top;

        /** The stack data. */
        public Info[] data = new Info[10];

        //
        // Public methods
        //

        /** Pushes element information onto the stack. */
        public void push(Info info) {
            if (top == data.length) {
                Info[] newarray = new Info[top + 10];
                System.arraycopy(data, 0, newarray, 0, top);
                data = newarray;
            }
            data[top++] = info;
        } // push(Info)

        /** Peeks at the top of the stack. */
        public Info peek() {
            return data[top-1];
        } // peek():Info

        /** Pops the top item off of the stack. */
        public Info pop() {
            return data[--top];
        } // pop():Info

    } // class InfoStack

} // class HTMLTagBalancer
