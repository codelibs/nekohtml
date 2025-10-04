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

/**
 * Represents a qualified name in HTML/XML parsing.
 * This is a SAX-compatible replacement for Xerces XNI QName.
 *
 * @author CodeLibs Project
 */
public class HTMLQName {

    /** The namespace URI. */
    public String uri;

    /** The local part of the qualified name. */
    public String localpart;

    /** The raw qualified name (prefix:localpart or just localpart). */
    public String rawname;

    /**
     * Default constructor.
     */
    public HTMLQName() {
        clear();
    }

    /**
     * Constructs a qualified name with the specified raw name.
     *
     * @param rawname The raw qualified name
     */
    public HTMLQName(final String rawname) {
        this(null, rawname, rawname);
    }

    /**
     * Constructs a qualified name with the specified parts.
     *
     * @param uri The namespace URI
     * @param localpart The local part
     * @param rawname The raw qualified name
     */
    public HTMLQName(final String uri, final String localpart, final String rawname) {
        setValues(uri, localpart, rawname);
    }

    /**
     * Copy constructor.
     *
     * @param qname The qualified name to copy
     */
    public HTMLQName(final HTMLQName qname) {
        setValues(qname);
    }

    /**
     * Sets the values of this qualified name.
     *
     * @param uri The namespace URI
     * @param localpart The local part
     * @param rawname The raw qualified name
     */
    public void setValues(final String uri, final String localpart, final String rawname) {
        this.uri = uri;
        this.localpart = localpart;
        this.rawname = rawname;
    }

    /**
     * Sets the values from another qualified name.
     *
     * @param qname The qualified name to copy from
     */
    public void setValues(final HTMLQName qname) {
        this.uri = qname.uri;
        this.localpart = qname.localpart;
        this.rawname = qname.rawname;
    }

    /**
     * Clears all values.
     */
    public void clear() {
        this.uri = null;
        this.localpart = null;
        this.rawname = null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((rawname == null) ? 0 : rawname.hashCode());
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HTMLQName other = (HTMLQName) obj;
        if (rawname == null) {
            if (other.rawname != null) {
                return false;
            }
        } else if (!rawname.equals(other.rawname)) {
            return false;
        }
        if (uri == null) {
            if (other.uri != null) {
                return false;
            }
        } else if (!uri.equals(other.uri)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder str = new StringBuilder();
        str.append("QName{");
        if (uri != null) {
            str.append("uri=\"").append(uri).append("\",");
        }
        if (localpart != null) {
            str.append("localpart=\"").append(localpart).append("\",");
        }
        str.append("rawname=\"").append(rawname).append("\"");
        str.append('}');
        return str.toString();
    }

} // class HTMLQName
