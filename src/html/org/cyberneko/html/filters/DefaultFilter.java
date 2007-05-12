/* 
 * (C) Copyright 2002-2003, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

package org.cyberneko.html.filters;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLDocumentSource;

/**
 * This class implements a filter that simply passes document
 * events to the next handler. It can be used as a base class to
 * simplify the development of new document filters.
 *
 * @author Andy Clark
 *
 * @version $Id$
 */
public class DefaultFilter 
    implements XMLDocumentFilter {

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
    public void setDocumentHandler(XMLDocumentHandler handler) {
        fDocumentHandler = handler;
    } // setDocumentHandler(XMLDocumentHandler)

    // @since Xerces 2.1.0

    /** Returns the document handler. */
    public XMLDocumentHandler getDocumentHandler() {
        return fDocumentHandler;
    } // getDocumentHandler():XMLDocumentHandler

    /** Sets the document source. */
    public void setDocumentSource(XMLDocumentSource source) {
        fDocumentSource = source;
    } // setDocumentSource(XMLDocumentSource)

    /** Returns the document source. */
    public XMLDocumentSource getDocumentSource() {
        return fDocumentSource;
    } // getDocumentSource():XMLDocumentSource

    //
    // XMLDocumentHandler methods
    //

    // since Xerces-J 2.2.0

    /** Start document. */
    public void startDocument(XMLLocator locator, String encoding, 
                              NamespaceContext nscontext, Augmentations augs) 
        throws XNIException {
        if (fDocumentHandler != null) {
            try {
                // NOTE: Hack to allow the default filter to work with
                //       old and new versions of the XNI document handler
                //       interface. -Ac
                Class cls = fDocumentHandler.getClass();
                Class[] types = {
                    XMLLocator.class, String.class,
                    NamespaceContext.class, Augmentations.class
                };
                Method method = cls.getMethod("startDocument", types);
                Object[] params = {
                    locator, encoding, 
                    nscontext, augs
                };
                method.invoke(fDocumentHandler, params);
            } 
            catch (IllegalAccessException e) {
                throw new XNIException(e);
            } 
            catch (InvocationTargetException e) {
                throw new XNIException(e);                
            } 
            catch (NoSuchMethodException e) {
                try {
                    // NOTE: Hack to allow the default filter to work with
                    //       old and new versions of the XNI document handler
                    //       interface. -Ac
                    Class cls = fDocumentHandler.getClass();
                    Class[] types = {
                        XMLLocator.class, String.class, Augmentations.class
                    };
                    Method method = cls.getMethod("startDocument", types);
                    Object[] params = {
                        locator, encoding, augs
                    };
                    method.invoke(fDocumentHandler, params);
                } 
                catch (NoSuchMethodException ex) {
                    // NOTE: Should not happen!
                    throw new XNIException(ex);                
                } 
                catch (IllegalAccessException ex) {
                    // NOTE: Should not happen!
                    throw new XNIException(ex);                
                } 
                catch (InvocationTargetException ex) {
                    // NOTE: Should not happen!
                    throw new XNIException(ex);                
                }
            }
        }
    } // startDocument(XMLLocator,String,Augmentations)

    // old methods

    /** XML declaration. */
    public void xmlDecl(String version, String encoding, String standalone, Augmentations augs)
        throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.xmlDecl(version, encoding, standalone, augs);
        }
    } // xmlDecl(String,String,String,Augmentations)

    /** Doctype declaration. */
    public void doctypeDecl(String root, String publicId, String systemId, Augmentations augs)
        throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.doctypeDecl(root, publicId, systemId, augs);
        }
    } // doctypeDecl(String,String,String,Augmentations)

    /** Comment. */
    public void comment(XMLString text, Augmentations augs)
        throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.comment(text, augs);
        }
    } // comment(XMLString,Augmentations)

    /** Processing instruction. */
    public void processingInstruction(String target, XMLString data, Augmentations augs)
        throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.processingInstruction(target, data, augs);
        }
    } // processingInstruction(String,XMLString,Augmentations)

    /** Start element. */
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs)
        throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.startElement(element, attributes, augs);
        }
    } // startElement(QName,XMLAttributes,Augmentations)

    /** Empty element. */
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs)
        throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.emptyElement(element, attributes, augs);
        }
    } // emptyElement(QName,XMLAttributes,Augmentations)

    /** Characters. */
    public void characters(XMLString text, Augmentations augs) 
        throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.characters(text, augs);
        }
    } // characters(XMLString,Augmentations)

    /** Ignorable whitespace. */
    public void ignorableWhitespace(XMLString text, Augmentations augs) 
        throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.ignorableWhitespace(text, augs);
        }
    } // ignorableWhitespace(XMLString,Augmentations)

    /** Start general entity. */
    public void startGeneralEntity(String name, XMLResourceIdentifier id, String encoding, Augmentations augs)
        throws XNIException {
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

    /** End general entity. */
    public void endGeneralEntity(String name, Augmentations augs)
        throws XNIException {
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

    /** End element. */
    public void endElement(QName element, Augmentations augs)
        throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.endElement(element, augs);
        }
    } // endElement(QName,Augmentations)

    /** End document. */
    public void endDocument(Augmentations augs) throws XNIException {
        if (fDocumentHandler != null) {
            fDocumentHandler.endDocument(augs);
        }
    } // endDocument(Augmentations)

    // removed since Xerces-J 2.3.0

    /** Start document. */
    public void startDocument(XMLLocator locator, String encoding, Augmentations augs) 
        throws XNIException {
        startDocument(locator, encoding, null, augs);
    } // startDocument(XMLLocator,String,Augmentations)

    /** Start prefix mapping. */
    public void startPrefixMapping(String prefix, String uri, Augmentations augs)
        throws XNIException {
        if (fDocumentHandler != null) {
            Class cls = fDocumentHandler.getClass();
            Class[] types = { String.class, String.class, Augmentations.class };
            try {
                Method method = cls.getMethod("startPrefixMapping", types);
                Object[] args = { prefix, uri, augs };
                method.invoke(fDocumentHandler, args);
            }
            catch (NoSuchMethodException e) {
                // ignore
            }
            catch (IllegalAccessException e) {
                // ignore
            }
            catch (InvocationTargetException e) {
                // ignore
            }
        }
    } // startPrefixMapping(String,String,Augmentations)

    /** End prefix mapping. */
    public void endPrefixMapping(String prefix, Augmentations augs)
        throws XNIException {
        if (fDocumentHandler != null) {
            Class cls = fDocumentHandler.getClass();
            Class[] types = { String.class, Augmentations.class };
            try {
                Method method = cls.getMethod("endPrefixMapping", types);
                Object[] args = { prefix, augs };
                method.invoke(fDocumentHandler, args);
            }
            catch (NoSuchMethodException e) {
                // ignore
            }
            catch (IllegalAccessException e) {
                // ignore
            }
            catch (InvocationTargetException e) {
                // ignore
            }
        }
    } // endPrefixMapping(String,Augmentations)

} // class DefaultFilter
