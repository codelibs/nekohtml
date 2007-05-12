/* (C) Copyright 2002, Andy Clark. All rights reserved. */

package org.cyberneko.html;

import java.io.IOException;
import java.util.Locale;
                                                                               
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLDTDHandler;
import org.apache.xerces.xni.XMLDTDContentModelHandler;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLConfigurationException;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLDocumentScanner;
import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
                                      
/**
 * An XNI-based parser configuration that can be used to parse HTML documents.
 *
 * @author Andy Clark
 */
public class HTMLConfiguration 
    implements XMLParserConfiguration {

    //
    // Data
    //

    /** Document handler. */
    protected XMLDocumentHandler fDocumentHandler;

    /** DTD handler. */
    protected XMLDTDHandler fDTDHandler;

    /** DTD content model handler. */
    protected XMLDTDContentModelHandler fDTDContentModelHandler;

    /** Error handler. */
    protected XMLErrorHandler fErrorHandler;

    /** Entity resolver. */
    protected XMLEntityResolver fEntityResolver;

    /** Locale. */
    protected Locale fLocale;

    // pipeline

    /** Document scanner. */
    protected XMLDocumentScanner fDocumentScanner = new HTMLScanner();

    /** HTML tag balancer. */
    protected XMLDocumentFilter fTagBalancer = new HTMLTagBalancer();

    //
    // XMLParserConfiguration methods
    //

    /** Sets the document handler. */
    public void setDocumentHandler(XMLDocumentHandler handler) {
        fDocumentHandler = handler;
    } // setDocumentHandler(XMLDocumentHandler)

    /** Returns the document handler. */
    public XMLDocumentHandler getDocumentHandler() {
        return fDocumentHandler;
    } // getDocumentHandler():XMLDocumentHandler

    /** Sets the DTD handler. */
    public void setDTDHandler(XMLDTDHandler handler) {
        fDTDHandler = handler;
    } // setDTDHandler(XMLDTDHandler)

    /** Returns the DTD handler. */
    public XMLDTDHandler getDTDHandler() {
        return fDTDHandler;
    } // getDTDHandler():XMLDTDHandler

    /** Sets the DTD content model handler. */
    public void setDTDContentModelHandler(XMLDTDContentModelHandler handler) {
        fDTDContentModelHandler = handler;
    } // setDTDContentModelHandler(XMLDTDContentModelHandler)

    /** Returns the DTD content model handler. */
    public XMLDTDContentModelHandler getDTDContentModelHandler() {
        return fDTDContentModelHandler;
    } // getDTDContentModelHandler():XMLDTDContentModelHandler

    /** Sets the error handler. */
    public void setErrorHandler(XMLErrorHandler handler) {
        fErrorHandler = handler;
    } // setErrorHandler(XMLErrorHandler)

    /** Returns the error handler. */
    public XMLErrorHandler getErrorHandler() {
        return fErrorHandler;
    } // getErrorHandler():XMLErrorHandler

    /** Sets the entity resolver. */
    public void setEntityResolver(XMLEntityResolver resolver) {
        fEntityResolver = resolver;
    } // setEntityResolver(XMLEntityResolver)

    /** Returns the entity resolver. */
    public XMLEntityResolver getEntityResolver() {
        return fEntityResolver;
    } // getEntityResolver():XMLEntityResolver

    /** Sets the locale. */
    public void setLocale(Locale locale) {
        fLocale = locale;
    } // setLocale(Locale)

    /** Returns the locale. */
    public Locale getLocale() {
        return fLocale;
    } // getLocale():Locale

    /** Adds recognized features. */
    public void addRecognizedFeatures(String[] featureIds) {}

    /** Sets the state of a feature. */
    public void setFeature(String featureId, boolean state) throws XMLConfigurationException {}

    /** Returns a feature state. */
    public boolean getFeature(String featureId) throws XMLConfigurationException { return false; }

    /** Adds recognized properties. */
    public void addRecognizedProperties(String[] propertyIds) {}

    /** Sets the value of a property. */
    public void setProperty(String propertyId, Object value) throws XMLConfigurationException {}

    /** Returns a property value. */
    public Object getProperty(String propertyId) throws XMLConfigurationException { return null; }

    /** Parses a document. */
    public void parse(XMLInputSource source) throws XNIException, IOException {
        reset();
        fDocumentScanner.setInputSource(source);
        fDocumentScanner.scanDocument(true);
    } // parse(XMLInputSource)

    //
    // Protected methods
    //

    /** Resets the parser configuration. */
    protected void reset() throws XMLConfigurationException {
        fDocumentScanner.setDocumentHandler(fTagBalancer);
        fTagBalancer.setDocumentHandler(fDocumentHandler);
    } // reset()

} // class HTMLConfiguration
