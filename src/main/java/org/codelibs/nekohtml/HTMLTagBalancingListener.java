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
package org.codelibs.nekohtml;

import org.codelibs.xerces.xni.Augmentations;
import org.codelibs.xerces.xni.QName;
import org.codelibs.xerces.xni.XMLAttributes;

/**
 * XMLDocumentHandler implementing this interface will get notified of elements discarded
 * by the tag balancer when they:
 * <ul>
 * <li>are configured using {@link HTMLConfiguration}</li>
 * <li>activate the tag balancing feature</li>
 * </ul>
 * @author Marc Guillemot
 * @version $Id$
 */
public interface HTMLTagBalancingListener {
    /**
     * Notifies that the start element has been ignored.
     * @param elem The element name that was ignored
     * @param attrs The element attributes
     * @param augs Additional information that may include infoset augmentations
     */
    void ignoredStartElement(QName elem, XMLAttributes attrs, Augmentations augs);

    /**
     * Notifies that the end element has been ignored.
     * @param element The element name that was ignored
     * @param augs Additional information that may include infoset augmentations
     */
    void ignoredEndElement(QName element, Augmentations augs);

}
