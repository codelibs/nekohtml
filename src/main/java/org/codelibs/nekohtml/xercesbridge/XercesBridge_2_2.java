/*
 * Copyright Marc Guillemot
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
package org.codelibs.nekohtml.xercesbridge;

import org.apache.xerces.impl.Version;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLDocumentSource;

/**
 * Xerces bridge for use with Xerces 2.2 and higher
 * @author Marc Guillemot
 */
public class XercesBridge_2_2 extends XercesBridge {
    /**
     * Should fail for Xerces version less than 2.2
     * @throws InstantiationException if instantiation failed
     */
    protected XercesBridge_2_2() throws InstantiationException {
        try {
            getVersion();
        } catch (final Throwable e) {
            throw new InstantiationException(e.getMessage());
        }
    }

    @Override
    public String getVersion() {
        return Version.getVersion();
    }

    @Override
    public void XMLDocumentHandler_startPrefixMapping(final XMLDocumentHandler documentHandler, final String prefix, final String uri,
            final Augmentations augs) {
        // does nothing, not needed
    }

    @Override
    public void XMLDocumentHandler_startDocument(final XMLDocumentHandler documentHandler, final XMLLocator locator, final String encoding,
            final NamespaceContext nscontext, final Augmentations augs) {
        documentHandler.startDocument(locator, encoding, nscontext, augs);
    }

    @Override
    public void XMLDocumentFilter_setDocumentSource(final XMLDocumentFilter filter, final XMLDocumentSource lastSource) {
        filter.setDocumentSource(lastSource);
    }
}
