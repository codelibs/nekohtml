/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.nekohtml.xercesbridge;

import java.lang.reflect.InvocationTargetException;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLDocumentSource;

/**
 * This class allows to transparently handle Xerces methods that have changed among versions.
 * @author Marc Guillemot
 */
public abstract class XercesBridge {

    private static final XercesBridge instance = makeInstance();

    /**
     * Default constructor for XercesBridge.
     * Creates a bridge for handling version-specific Xerces implementation differences.
     */
    protected XercesBridge() {
        // Protected constructor
    }

    /**
     * The access point for the bridge.
     * @return the instance corresponding to the Xerces version being currently used.
     */
    public static XercesBridge getInstance() {
        return instance;
    }

    private static XercesBridge makeInstance() {
        final String[] classNames =
                { "org.codelibs.nekohtml.xercesbridge.XercesBridge_2_3", "org.codelibs.nekohtml.xercesbridge.XercesBridge_2_2" };

        for (int i = 0; i != classNames.length; ++i) {
            final String className = classNames[i];
            final XercesBridge bridge = newInstanceOrNull(className);
            if (bridge != null) {
                return bridge;
            }
        }
        throw new IllegalStateException("Failed to create XercesBridge instance");
    }

    private static XercesBridge newInstanceOrNull(final String className) {
        try {
            return (XercesBridge) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (final ClassNotFoundException | SecurityException | LinkageError | IllegalArgumentException | IllegalAccessException
                | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            // nothing
        }

        return null;
    }

    /**
     * Default implementation does nothing
     * @param namespaceContext The namespace context to declare the prefix in
     * @param ns The namespace prefix
     * @param avalue The namespace URI value
     */
    public void NamespaceContext_declarePrefix(final NamespaceContext namespaceContext, final String ns, final String avalue) {
        // nothing
    }

    /**
     * Gets the Xerces version used
     * @return the version
     */
    public abstract String getVersion();

    /**
     * Calls startDocument on the {@link XMLDocumentHandler}.
     * @param documentHandler The document handler to call startDocument on
     * @param locator The document locator
     * @param encoding The character encoding
     * @param nscontext The namespace context
     * @param augs Additional information that may include infoset augmentations
     */
    public abstract void XMLDocumentHandler_startDocument(XMLDocumentHandler documentHandler, XMLLocator locator, String encoding,
            NamespaceContext nscontext, Augmentations augs);

    /**
     * Calls startPrefixMapping on the {@link XMLDocumentHandler}.
     * @param documentHandler The document handler to call startPrefixMapping on
     * @param prefix The namespace prefix
     * @param uri The namespace URI
     * @param augs Additional information that may include infoset augmentations
     */
    public void XMLDocumentHandler_startPrefixMapping(final XMLDocumentHandler documentHandler, final String prefix, final String uri,
            final Augmentations augs) {
        // default does nothing
    }

    /**
     * Calls endPrefixMapping on the {@link XMLDocumentHandler}.
     * @param documentHandler The document handler to call endPrefixMapping on
     * @param prefix The namespace prefix
     * @param augs Additional information that may include infoset augmentations
     */
    public void XMLDocumentHandler_endPrefixMapping(final XMLDocumentHandler documentHandler, final String prefix, final Augmentations augs) {
        // default does nothing
    }

    /**
     * Calls setDocumentSource (if available in the Xerces version used) on the {@link XMLDocumentFilter}.
     * This implementation does nothing.
     * @param filter The document filter to set the document source on
     * @param lastSource The document source to set
     */
    public void XMLDocumentFilter_setDocumentSource(final XMLDocumentFilter filter, final XMLDocumentSource lastSource) {
        // nothing, it didn't exist on old Xerces versions
    }
}
