/*
 * Copyright 2004-2008 Andy Clark
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

import java.util.Locale;

import org.apache.xerces.util.XMLChar;
import org.apache.xerces.util.XMLStringBuffer;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.parser.XMLComponentManager;
import org.codelibs.nekohtml.HTMLAugmentations;
import org.codelibs.nekohtml.HTMLEventInfo;
import org.codelibs.nekohtml.xercesbridge.XercesBridge;

/**
 * This filter purifies the HTML input to ensure XML well-formedness.
 * The purification process includes:
 * <ul>
 * <li>fixing illegal characters in the document, including
 *  <ul>
 *  <li>element and attribute names,
 *  <li>processing instruction target and data,
 *  <li>document text;
 *  </ul>
 * <li>ensuring the string "--" does not appear in the content of
 *     a comment;
 * <li>ensuring the string "]]&gt;" does not appear in the content of
 *     a CDATA section;
 * <li>ensuring that the XML declaration has required pseudo-attributes
 *     and that the values are correct;
 * and
 * <li>synthesized missing namespace bindings.
 * </ul>
 * <p>
 * Illegal characters in XML names are converted to the character
 * sequence "_u####_" where "####" is the value of the Unicode
 * character represented in hexadecimal. Whereas illegal characters
 * appearing in document content is converted to the character
 * sequence "\\u####".
 * <p>
 * In comments, the character '-' is replaced by the character
 * sequence "- " to prevent "--" from ever appearing in the comment
 * content. For CDATA sections, the character ']' is replaced by
 * the character sequence "] " to prevent "]]" from appearing.
 * <p>
 * The URI used for synthesized namespace bindings is
 * "http://cyberneko.org/html/ns/synthesized/<i>number</i>" where
 * <i>number</i> is generated to ensure uniqueness.
 *
 * @author Andy Clark
 *
 * @version $Id: Purifier.java,v 1.5 2005/02/14 03:56:54 andyc Exp $
 */
public class Purifier extends DefaultFilter {

    //
    // Constants
    //

    /** Synthesized namespace binding prefix. */
    public static final String SYNTHESIZED_NAMESPACE_PREFX = "http://cyberneko.org/html/ns/synthesized/";

    /** Namespaces. */
    protected static final String NAMESPACES = "http://xml.org/sax/features/namespaces";

    /** Include infoset augmentations. */
    protected static final String AUGMENTATIONS = "http://cyberneko.org/html/features/augmentations";

    // static vars

    /** Synthesized event info item. */
    protected static final HTMLEventInfo SYNTHESIZED_ITEM = new HTMLEventInfo.SynthesizedItem();

    //
    // Data
    //

    // features

    /** Namespaces. */
    protected boolean fNamespaces;

    /** Augmentations. */
    protected boolean fAugmentations;

    // state

    /** True if the doctype declaration was seen. */
    protected boolean fSeenDoctype;

    /** True if root element was seen. */
    protected boolean fSeenRootElement;

    /** True if inside a CDATA section. */
    protected boolean fInCDATASection;

    // doctype declaration info

    /** Public identifier of doctype declaration. */
    protected String fPublicId;

    /** System identifier of doctype declaration. */
    protected String fSystemId;

    // namespace info

    /** Namespace information. */
    protected NamespaceContext fNamespaceContext;

    /** Synthesized namespace binding count. */
    protected int fSynthesizedNamespaceCount;

    // temp vars

    /** Qualified name. */
    private final QName fQName = new QName();

    /** Augmentations. */
    private final HTMLAugmentations fInfosetAugs = new HTMLAugmentations();

    /** String buffer. */
    private final XMLStringBuffer fStringBuffer = new XMLStringBuffer();

    //
    // XMLComponent methods
    //

    @Override
    public void reset(final XMLComponentManager manager) {

        // state
        fInCDATASection = false;

        // features
        fNamespaces = manager.getFeature(NAMESPACES);
        fAugmentations = manager.getFeature(AUGMENTATIONS);

    } // reset(XMLComponentManager)

    //
    // XMLDocumentHandler methods
    //

    /** Start document. */
    @Override
    public void startDocument(final XMLLocator locator, final String encoding, final Augmentations augs) {
        fNamespaceContext = fNamespaces ? new NamespaceBinder.NamespaceSupport() : null;
        fSynthesizedNamespaceCount = 0;
        handleStartDocument();
        super.startDocument(locator, encoding, augs);
    } // startDocument(XMLLocator,String,Augmentations)

    /** Start document. */
    @Override
    public void startDocument(final XMLLocator locator, final String encoding, final NamespaceContext nscontext, final Augmentations augs) {
        fNamespaceContext = nscontext;
        fSynthesizedNamespaceCount = 0;
        handleStartDocument();
        super.startDocument(locator, encoding, nscontext, augs);
    } // startDocument(XMLLocator,NamespaceContext,String,Augmentations)

    /** XML declaration. */
    @Override
    public void xmlDecl(String version, String encoding, String standalone, final Augmentations augs) {
        if (version == null || !version.equals("1.0")) {
            version = "1.0";
        }
        if (encoding != null && encoding.length() == 0) {
            encoding = null;
        }
        if (standalone != null) {
            if (!standalone.equalsIgnoreCase("true") && !standalone.equalsIgnoreCase("false")) {
                standalone = null;
            } else {
                standalone = standalone.toLowerCase();
            }
        }
        super.xmlDecl(version, encoding, standalone, augs);
    } // xmlDecl(String,String,String,Augmentations)

    /** Comment. */
    @Override
    public void comment(XMLString text, final Augmentations augs) {
        final StringBuilder str = new StringBuilder(purifyText(text).toString());
        final int length = str.length();
        for (int i = length - 1; i >= 0; i--) {
            final char c = str.charAt(i);
            if (c == '-') {
                str.insert(i + 1, ' ');
            }
        }
        fStringBuffer.length = 0;
        fStringBuffer.append(str.toString());
        text = fStringBuffer;
        super.comment(text, augs);
    } // comment(XMLString,Augmentations)

    /** Processing instruction. */
    @Override
    public void processingInstruction(String target, XMLString data, final Augmentations augs) {
        target = purifyName(target, true);
        data = purifyText(data);
        super.processingInstruction(target, data, augs);
    } // processingInstruction(String,XMLString,Augmentations)

    /** Doctype declaration. */
    @Override
    public void doctypeDecl(final String root, final String pubid, final String sysid, final Augmentations augs) {
        fSeenDoctype = true;
        // NOTE: It doesn't matter what the root element name is because
        //       it must match the root element. -Ac
        fPublicId = pubid;
        fSystemId = sysid;
        // NOTE: If the public identifier is specified, then a system
        //       identifier must also be specified. -Ac
        if (fPublicId != null && fSystemId == null) {
            fSystemId = "";
        }
        // NOTE: Can't save the augmentations because the object state
        //       is transient. -Ac
    } // doctypeDecl(String,String,String,Augmentations)

    /** Start element. */
    @Override
    public void startElement(final QName element, final XMLAttributes attrs, final Augmentations augs) {
        handleStartElement(element, attrs);
        super.startElement(element, attrs, augs);
    } // startElement(QName,XMLAttributes,Augmentations)

    /** Empty element. */
    @Override
    public void emptyElement(final QName element, final XMLAttributes attrs, final Augmentations augs) {
        handleStartElement(element, attrs);
        super.emptyElement(element, attrs, augs);
    } // emptyElement(QName,XMLAttributes,Augmentations)

    /** Start CDATA section. */
    @Override
    public void startCDATA(final Augmentations augs) {
        fInCDATASection = true;
        super.startCDATA(augs);
    } // startCDATA(Augmentations)

    /** End CDATA section. */
    @Override
    public void endCDATA(final Augmentations augs) {
        fInCDATASection = false;
        super.endCDATA(augs);
    } // endCDATA(Augmentations)

    /** Characters. */
    @Override
    public void characters(XMLString text, final Augmentations augs) {
        text = purifyText(text);
        if (fInCDATASection) {
            final StringBuilder str = new StringBuilder(text.toString());
            final int length = str.length();
            for (int i = length - 1; i >= 0; i--) {
                final char c = str.charAt(i);
                if (c == ']') {
                    str.insert(i + 1, ' ');
                }
            }
            fStringBuffer.length = 0;
            fStringBuffer.append(str.toString());
            text = fStringBuffer;
        }
        super.characters(text, augs);
    } // characters(XMLString,Augmentations)

    /** End element. */
    @Override
    public void endElement(QName element, final Augmentations augs) {
        element = purifyQName(element);
        if (fNamespaces) {
            if (element.prefix != null && element.uri == null) {
                element.uri = fNamespaceContext.getURI(element.prefix);
            }
        }
        super.endElement(element, augs);
    } // endElement(QName,Augmentations)

    //
    // Protected methods
    //

    /** Handle start document. */
    protected void handleStartDocument() {
        fSeenDoctype = false;
        fSeenRootElement = false;
    } // handleStartDocument()

    /** Handle start element. */
    protected void handleStartElement(QName element, final XMLAttributes attrs) {

        // handle element and attributes
        element = purifyQName(element);
        final int attrCount = attrs != null ? attrs.getLength() : 0;
        for (int i = attrCount - 1; i >= 0; i--) {
            // purify attribute name
            attrs.getName(i, fQName);
            attrs.setName(i, purifyQName(fQName));

            // synthesize namespace bindings
            if (fNamespaces) {
                if (!fQName.rawname.equals("xmlns") && !fQName.rawname.startsWith("xmlns:")) {
                    // NOTE: Must get attribute name again because the
                    //       purifyQName method does not guarantee that
                    //       the same QName object is returned. -Ac
                    attrs.getName(i, fQName);
                    if (fQName.prefix != null && fQName.uri == null) {
                        synthesizeBinding(attrs, fQName.prefix);
                    }
                }
            }
        }

        // synthesize namespace bindings
        if (fNamespaces) {
            if (element.prefix != null && element.uri == null) {
                synthesizeBinding(attrs, element.prefix);
            }
        }

        // synthesize doctype declaration
        if (!fSeenRootElement && fSeenDoctype) {
            final Augmentations augs = synthesizedAugs();
            super.doctypeDecl(element.rawname, fPublicId, fSystemId, augs);
        }

        // mark start element as seen
        fSeenRootElement = true;

    } // handleStartElement(QName,XMLAttributes)

    /** Synthesize namespace binding. */
    protected void synthesizeBinding(final XMLAttributes attrs, final String ns) {
        final String prefix = "xmlns";
        final String localpart = ns;
        final String qname = prefix + ':' + localpart;
        final String uri = NamespaceBinder.NAMESPACES_URI;
        final String atype = "CDATA";
        final String avalue = SYNTHESIZED_NAMESPACE_PREFX + fSynthesizedNamespaceCount++;

        // add attribute
        fQName.setValues(prefix, localpart, qname, uri);
        attrs.addAttribute(fQName, atype, avalue);

        // bind namespace
        XercesBridge.getInstance().NamespaceContext_declarePrefix(fNamespaceContext, ns, avalue);

    } // synthesizeBinding(XMLAttributes,String)

    /** Returns an augmentations object with a synthesized item added. */
    protected final Augmentations synthesizedAugs() {
        HTMLAugmentations augs = null;
        if (fAugmentations) {
            augs = fInfosetAugs;
            augs.removeAllItems();
            augs.putItem(AUGMENTATIONS, SYNTHESIZED_ITEM);
        }
        return augs;
    } // synthesizedAugs():Augmentations

    //
    // Protected methods
    //

    /** Purify qualified name. */
    protected QName purifyQName(final QName qname) {
        qname.prefix = purifyName(qname.prefix, true);
        qname.localpart = purifyName(qname.localpart, true);
        qname.rawname = purifyName(qname.rawname, false);
        return qname;
    } // purifyQName(QName):QName

    /** Purify name. */
    protected String purifyName(final String name, final boolean localpart) {
        if (name == null) {
            return name;
        }
        final StringBuilder str = new StringBuilder();
        final int length = name.length();
        boolean seenColon = localpart;
        for (int i = 0; i < length; i++) {
            final char c = name.charAt(i);
            if (i == 0) {
                if (!XMLChar.isNameStart(c)) {
                    str.append("_u").append(toHexString(c, 4)).append("_");
                } else {
                    str.append(c);
                }
            } else {
                if ((fNamespaces && c == ':' && seenColon) || !XMLChar.isName(c)) {
                    str.append("_u").append(toHexString(c, 4)).append("_");
                } else {
                    str.append(c);
                }
                seenColon = seenColon || c == ':';
            }
        }
        return str.toString();
    } // purifyName(String):String

    /** Purify content. */
    protected XMLString purifyText(final XMLString text) {
        fStringBuffer.length = 0;
        for (int i = 0; i < text.length; i++) {
            final char c = text.ch[text.offset + i];
            if (XMLChar.isInvalid(c)) {
                fStringBuffer.append("\\u" + toHexString(c, 4));
            } else {
                fStringBuffer.append(c);
            }
        }
        return fStringBuffer;
    } // purifyText(XMLString):XMLString

    //
    // Protected static methods
    //

    /** Returns a padded hexadecimal string for the given value. */
    protected static String toHexString(final int c, final int padlen) {
        final StringBuilder str = new StringBuilder(padlen);
        str.append(Integer.toHexString(c));
        final int len = padlen - str.length();
        for (int i = 0; i < len; i++) {
            str.insert(0, '0');
        }
        return str.toString().toUpperCase(Locale.ENGLISH);
    } // toHexString(int,int):String

} // class Purifier
