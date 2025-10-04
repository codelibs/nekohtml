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
package org.codelibs.nekohtml.sax;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * SAX-compatible attributes implementation for HTML parsing.
 * This is a replacement for Xerces XNI XMLAttributesImpl.
 *
 * @author CodeLibs Project
 */
public class HTMLAttributesImpl extends AttributesImpl {

    /**
     * Default constructor.
     */
    public HTMLAttributesImpl() {
        super();
    }

    /**
     * Copy constructor.
     *
     * @param attributes The attributes to copy
     */
    public HTMLAttributesImpl(final Attributes attributes) {
        super(attributes);
    }

    /**
     * Adds an attribute and returns the index.
     *
     * @param uri The namespace URI
     * @param localName The local name
     * @param qName The qualified name
     * @param type The attribute type
     * @param value The attribute value
     * @return The index of the added attribute
     */
    public int addAttributeWithIndex(final String uri, final String localName, final String qName, final String type, final String value) {
        super.addAttribute(uri, localName, qName, type, value);
        return getLength() - 1;
    }

    /**
     * Adds an attribute with QName.
     *
     * @param qname The qualified name
     * @param type The attribute type
     * @param value The attribute value
     * @return The index of the added attribute
     */
    public int addAttribute(final HTMLQName qname, final String type, final String value) {
        return addAttributeWithIndex(qname.uri, qname.localpart, qname.rawname, type, value);
    }

    /**
     * Removes the attribute at the specified index.
     *
     * @param index The attribute index
     */
    public void removeAttributeAt(final int index) {
        if (index >= 0 && index < getLength()) {
            super.removeAttribute(index);
        }
    }

    /**
     * Removes all attributes.
     */
    public void removeAllAttributes() {
        super.clear();
    }

    /**
     * Sets the attribute name at the specified index.
     *
     * @param index The attribute index
     * @param uri The namespace URI
     * @param localName The local name
     * @param qName The qualified name
     */
    public void setName(final int index, final String uri, final String localName, final String qName) {
        if (index >= 0 && index < getLength()) {
            super.setURI(index, uri);
            super.setLocalName(index, localName);
            super.setQName(index, qName);
        }
    }

    /**
     * Sets the attribute name at the specified index.
     *
     * @param index The attribute index
     * @param qname The qualified name
     */
    public void setName(final int index, final HTMLQName qname) {
        setName(index, qname.uri, qname.localpart, qname.rawname);
    }

    /**
     * Gets the non-normalized value of an attribute.
     * In this implementation, returns the same as getValue since SAX doesn't distinguish.
     *
     * @param index The attribute index
     * @return The attribute value
     */
    public String getNonNormalizedValue(final int index) {
        return getValue(index);
    }

    /**
     * Sets the non-normalized value of an attribute.
     * In this implementation, sets the same as setValue since SAX doesn't distinguish.
     *
     * @param index The attribute index
     * @param value The attribute value
     */
    public void setNonNormalizedValue(final int index, final String value) {
        setValue(index, value);
    }

    /**
     * Sets whether an attribute was specified in the document.
     * Note: SAX doesn't have this concept, so this is a no-op for compatibility.
     *
     * @param index The attribute index
     * @param specified Whether the attribute was specified
     */
    public void setSpecified(final int index, final boolean specified) {
        // SAX doesn't distinguish between specified and default attributes
        // This method is provided for API compatibility but does nothing
    }

    /**
     * Checks whether an attribute was specified in the document.
     * Note: In SAX, all attributes in the Attributes object are considered specified.
     *
     * @param index The attribute index
     * @return Always true in this implementation
     */
    public boolean isSpecified(final int index) {
        return index >= 0 && index < getLength();
    }

    /**
     * Gets an attribute's QName.
     *
     * @param index The attribute index
     * @param qname The QName object to populate
     */
    public void getName(final int index, final HTMLQName qname) {
        if (index >= 0 && index < getLength()) {
            qname.setValues(getURI(index), getLocalName(index), getQName(index));
        } else {
            qname.clear();
        }
    }

} // class HTMLAttributesImpl
