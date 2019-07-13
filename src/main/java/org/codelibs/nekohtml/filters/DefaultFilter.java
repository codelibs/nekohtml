/*
 * Copyright 2002-2009 Andy Clark, Marc Guillemot
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codelibs.nekohtml.filters;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.parser.XMLComponentManager;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLDocumentSource;
import org.codelibs.nekohtml.HTMLComponent;
import org.codelibs.nekohtml.xercesbridge.XercesBridge;

/**
 * This class implements a filter that simply passes document
 * events to the next handler. It can be used as a base class to
 * simplify the development of new document filters.
 *
 * @author Andy Clark
 *
 * @version $Id: DefaultFilter.java,v 1.7 2005/02/14 03:56:54 andyc Exp $
 */
public class DefaultFilter implements XMLDocumentFilter, HTMLComponent {

    //
    // Data
    //

    /** Document handler. */
    protected XMLDocumentHandler fDocumentHandler;

    /** Document source. */
    protected XMLDocumentSource fDocumentSource;

    //
    // XMLDocumentSource methods
    //

    /** Sets the document handler. */
    @Override
    public void setDocumentHandler(final XMLDocumentHandler handler) {
        fDocumentHandler = handler;
    } // setDocumentHandler(XMLDocumentHandler)

    // @since Xerces 2.1.0

    /** Returns the document handler. */
    @Override
    public XMLDocumentHandler getDocumentHandler() {
        return fDocumentHandler;
    } // getDocumentHandler():XMLDocumentHandler

    /** Sets the document source. */
    @Override
    public void setDocumentSource(final XMLDocumentSource source) {
        fDocumentSource = source;
    } // setDocumentSource(XMLDocumentSource)

    /** Returns the document source. */
    @Override
    public XMLDocumentSource getDocumentSource() {
        return fDocumentSource;
    } // getDocumentSource():XMLDocumentSource

    //
    // XMLDocumentHandler methods
    //

    // since Xerces-J 2.2.0

    /** Start document. */
    @Override
    public void startDocument(final XMLLocator locator, final String encoding, final NamespaceContext nscontext, final Augmentations augs) {
        if (fDocumentHandler != null) {
            XercesBridge.getInstance().XMLDocumentHandler_startDocument(fDocumentHandler, locator, encoding, nscontext, augs);
        }
    } // startDocument(XMLLocator,String,Augmentations)

    // old methods

    /** XML declaration. */
    @Override
    public void xmlDecl(final String version, final String encoding, final String standalone, final Augmentations augs) {
        if (fDocumentHandler != null) {
            fDocumentHandler.xmlDecl(version, encoding, standalone, augs);
        }
    } // xmlDecl(String,String,String,Augmentations)

    /** Doctype declaration. */
    @Override
    public void doctypeDecl(final String root, final String publicId, final String systemId, final Augmentations augs) {
        if (fDocumentHandler != null) {
            fDocumentHandler.doctypeDecl(root, publicId, systemId, augs);
        }
    } // doctypeDecl(String,String,String,Augmentations)

    /** Comment. */
    @Override
    public void comment(final XMLString text, final Augmentations augs) {
        if (fDocumentHandler != null) {
            fDocumentHandler.comment(text, augs);
        }
    } // comment(XMLString,Augmentations)

    /** Processing instruction. */
    @Override
    public void processingInstruction(final String target, final XMLString data, final Augmentations augs) {
        if (fDocumentHandler != null) {
            fDocumentHandler.processingInstruction(target, data, augs);
        }
    } // processingInstruction(String,XMLString,Augmentations)

    /** Start element. */
    @Override
    public void startElement(final QName element, final XMLAttributes attributes, final Augmentations augs) {
        if (fDocumentHandler != null) {
            fDocumentHandler.startElement(element, attributes, augs);
        }
    } // startElement(QName,XMLAttributes,Augmentations)

    /** Empty element. */
    @Override
    public void emptyElement(final QName element, final XMLAttributes attributes, final Augmentations augs) {
        if (fDocumentHandler != null) {
            fDocumentHandler.emptyElement(element, attributes, augs);
        }
    } // emptyElement(QName,XMLAttributes,Augmentations)

    /** Characters. */
    @Override
    public void characters(final XMLString text, final Augmentations augs) {
        if (fDocumentHandler != null) {
            fDocumentHandler.characters(text, augs);
        }
    } // characters(XMLString,Augmentations)

    /** Ignorable whitespace. */
    @Override
    public void ignorableWhitespace(final XMLString text, final Augmentations augs) {
        if (fDocumentHandler != null) {
            fDocumentHandler.ignorableWhitespace(text, augs);
        }
    } // ignorableWhitespace(XMLString,Augmentations)

    /** Start general entity. */
    @Override
    public void startGeneralEntity(final String name, final XMLResourceIdentifier id, final String encoding, final Augmentations augs) {
        if (fDocumentHandler != null) {
            fDocumentHandler.startGeneralEntity(name, id, encoding, augs);
        }
    } // startGeneralEntity(String,XMLResourceIdentifier,String,Augmentations)

    /** Text declaration. */
    @Override
    public void textDecl(final String version, final String encoding, final Augmentations augs) {
        if (fDocumentHandler != null) {
            fDocumentHandler.textDecl(version, encoding, augs);
        }
    } // textDecl(String,String,Augmentations)

    /** End general entity. */
    @Override
    public void endGeneralEntity(final String name, final Augmentations augs) {
        if (fDocumentHandler != null) {
            fDocumentHandler.endGeneralEntity(name, augs);
        }
    } // endGeneralEntity(String,Augmentations)

    /** Start CDATA section. */
    @Override
    public void startCDATA(final Augmentations augs) {
        if (fDocumentHandler != null) {
            fDocumentHandler.startCDATA(augs);
        }
    } // startCDATA(Augmentations)

    /** End CDATA section. */
    @Override
    public void endCDATA(final Augmentations augs) {
        if (fDocumentHandler != null) {
            fDocumentHandler.endCDATA(augs);
        }
    } // endCDATA(Augmentations)

    /** End element. */
    @Override
    public void endElement(final QName element, final Augmentations augs) {
        if (fDocumentHandler != null) {
            fDocumentHandler.endElement(element, augs);
        }
    } // endElement(QName,Augmentations)

    /** End document. */
    @Override
    public void endDocument(final Augmentations augs) {
        if (fDocumentHandler != null) {
            fDocumentHandler.endDocument(augs);
        }
    } // endDocument(Augmentations)

    // removed since Xerces-J 2.3.0

    /** Start document. */
    public void startDocument(final XMLLocator locator, final String encoding, final Augmentations augs) {
        startDocument(locator, encoding, null, augs);
    } // startDocument(XMLLocator,String,Augmentations)

    /** Start prefix mapping. */
    public void startPrefixMapping(final String prefix, final String uri, final Augmentations augs) {
        if (fDocumentHandler != null) {
            XercesBridge.getInstance().XMLDocumentHandler_startPrefixMapping(fDocumentHandler, prefix, uri, augs);
        }
    } // startPrefixMapping(String,String,Augmentations)

    /** End prefix mapping. */
    public void endPrefixMapping(final String prefix, final Augmentations augs) {
        if (fDocumentHandler != null) {
            XercesBridge.getInstance().XMLDocumentHandler_endPrefixMapping(fDocumentHandler, prefix, augs);
        }
    } // endPrefixMapping(String,Augmentations)

    //
    // HTMLComponent methods
    //

    /**
     * Returns a list of feature identifiers that are recognized by
     * this component. This method may return null if no features
     * are recognized by this component.
     */
    @Override
    public String[] getRecognizedFeatures() {
        return null;
    } // getRecognizedFeatures():String[]

    /**
     * Returns the default state for a feature, or null if this
     * component does not want to report a default value for this
     * feature.
     */
    @Override
    public Boolean getFeatureDefault(final String featureId) {
        return null;
    } // getFeatureDefault(String):Boolean

    /**
     * Returns a list of property identifiers that are recognized by
     * this component. This method may return null if no properties
     * are recognized by this component.
     */
    @Override
    public String[] getRecognizedProperties() {
        return null;
    } // getRecognizedProperties():String[]

    /**
     * Returns the default state for a property, or null if this
     * component does not want to report a default value for this
     * property.
     */
    @Override
    public Object getPropertyDefault(final String propertyId) {
        return null;
    } // getPropertyDefault(String):Object

    /**
     * Resets the component. The component can query the component manager
     * about any features and properties that affect the operation of the
     * component.
     *
     * @param componentManager The component manager.
     *
     * @Thrown by component on initialization error.
     */
    @Override
    public void reset(final XMLComponentManager componentManager) {
    } // reset(XMLComponentManager)

    /**
     * Sets the state of a feature. This method is called by the component
     * manager any time after reset when a feature changes state.
     * <p>
     * <strong>Note:</strong> Components should silently ignore features
     * that do not affect the operation of the component.
     *
     * @param featureId The feature identifier.
     * @param state     The state of the feature.
     *
     * @Thrown for configuration error.
     *                                   In general, components should
     *                                   only throw this exception if
     *                                   it is <strong>really</strong>
     *                                   a critical error.
     */
    @Override
    public void setFeature(final String featureId, final boolean state) {
    } // setFeature(String,boolean)

    /**
     * Sets the value of a property. This method is called by the component
     * manager any time after reset when a property changes value.
     * <p>
     * <strong>Note:</strong> Components should silently ignore properties
     * that do not affect the operation of the component.
     *
     * @param propertyId The property identifier.
     * @param value      The value of the property.
     *
     * @Thrown for configuration error.
     *                                   In general, components should
     *                                   only throw this exception if
     *                                   it is <strong>really</strong>
     *                                   a critical error.
     */
    @Override
    public void setProperty(final String propertyId, final Object value) {
    } // setProperty(String,Object)

    //
    // Protected static methods
    //

    /**
     * Utility method for merging string arrays for recognized features
     * and recognized properties.
     */
    protected static String[] merge(final String[] array1, final String[] array2) {

        // shortcut merge
        if (array1 == array2) {
            return array1;
        }
        if (array1 == null) {
            return array2;
        }
        if (array2 == null) {
            return array1;
        }

        // full merge
        final String[] array3 = new String[array1.length + array2.length];
        System.arraycopy(array1, 0, array3, 0, array1.length);
        System.arraycopy(array2, 0, array3, array1.length, array2.length);

        return array3;

    } // merge(String[],String[]):String[]

} // class DefaultFilter
