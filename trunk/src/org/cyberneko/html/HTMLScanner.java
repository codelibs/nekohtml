/* 
 * Copyright 2002-2007 Andy Clark
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

package org.cyberneko.html;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Stack;

import org.apache.xerces.util.EncodingMap;
import org.apache.xerces.util.NamespaceSupport;
import org.apache.xerces.util.URI;
import org.apache.xerces.util.XMLAttributesImpl;
import org.apache.xerces.util.XMLResourceIdentifierImpl;
import org.apache.xerces.util.XMLStringBuffer;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XMLDocumentHandler;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLComponentManager;
import org.apache.xerces.xni.parser.XMLConfigurationException;
import org.apache.xerces.xni.parser.XMLDocumentScanner;
import org.apache.xerces.xni.parser.XMLInputSource;

/**
 * A simple HTML scanner. This scanner makes no attempt to balance tags
 * or fix other problems in the source document &mdash; it just scans what 
 * it can and generates XNI document "events", ignoring errors of all 
 * kinds.
 * <p>
 * This component recognizes the following features:
 * <ul>
 * <li>http://cyberneko.org/html/features/augmentations
 * <li>http://cyberneko.org/html/features/report-errors
 * <li>http://apache.org/xml/features/scanner/notify-char-refs
 * <li>http://apache.org/xml/features/scanner/notify-builtin-refs
 * <li>http://cyberneko.org/html/features/scanner/notify-builtin-refs
 * <li>http://cyberneko.org/html/features/scanner/fix-mswindows-refs
 * <li>http://cyberneko.org/html/features/scanner/script/strip-cdata-delims
 * <li>http://cyberneko.org/html/features/scanner/script/strip-comment-delims
 * <li>http://cyberneko.org/html/features/scanner/style/strip-cdata-delims
 * <li>http://cyberneko.org/html/features/scanner/style/strip-comment-delims
 * <li>http://cyberneko.org/html/features/scanner/ignore-specified-charset
 * <li>http://cyberneko.org/html/features/scanner/cdata-sections
 * <li>http://cyberneko.org/html/features/override-doctype
 * <li>http://cyberneko.org/html/features/insert-doctype
 * </ul>
 * <p>
 * This component recognizes the following properties:
 * <ul>
 * <li>http://cyberneko.org/html/properties/names/elems
 * <li>http://cyberneko.org/html/properties/names/attrs
 * <li>http://cyberneko.org/html/properties/default-encoding
 * <li>http://cyberneko.org/html/properties/error-reporter
 * <li>http://cyberneko.org/html/properties/doctype/pubid
 * <li>http://cyberneko.org/html/properties/doctype/sysid
 * </ul>
 *
 * @see HTMLElements
 * @see HTMLEntities
 *
 * @author Andy Clark
 *
 * @version $Id: HTMLScanner.java,v 1.19 2005/06/14 05:52:37 andyc Exp $
 */
public class HTMLScanner 
    implements XMLDocumentScanner, XMLLocator, HTMLComponent {

    //
    // Constants
    //

    // doctype info: HTML 4.01 strict

    /** HTML 4.01 strict public identifier ("-//W3C//DTD HTML 4.01//EN"). */
    public static final String HTML_4_01_STRICT_PUBID = "-//W3C//DTD HTML 4.01//EN";

    /** HTML 4.01 strict system identifier ("http://www.w3.org/TR/html4/strict.dtd"). */
    public static final String HTML_4_01_STRICT_SYSID = "http://www.w3.org/TR/html4/strict.dtd";

    // doctype info: HTML 4.01 loose

    /** HTML 4.01 transitional public identifier ("-//W3C//DTD HTML 4.01 Transitional//EN"). */
    public static final String HTML_4_01_TRANSITIONAL_PUBID = "-//W3C//DTD HTML 4.01 Transitional//EN";

    /** HTML 4.01 transitional system identifier ("http://www.w3.org/TR/html4/loose.dtd"). */
    public static final String HTML_4_01_TRANSITIONAL_SYSID = "http://www.w3.org/TR/html4/loose.dtd";

    // doctype info: HTML 4.01 frameset

    /** HTML 4.01 frameset public identifier ("-//W3C//DTD HTML 4.01 Frameset//EN"). */
    public static final String HTML_4_01_FRAMESET_PUBID = "-//W3C//DTD HTML 4.01 Frameset//EN";

    /** HTML 4.01 frameset system identifier ("http://www.w3.org/TR/html4/frameset.dtd"). */
    public static final String HTML_4_01_FRAMESET_SYSID = "http://www.w3.org/TR/html4/frameset.dtd";

    // features

    /** Include infoset augmentations. */
    protected static final String AUGMENTATIONS = "http://cyberneko.org/html/features/augmentations";

    /** Report errors. */
    protected static final String REPORT_ERRORS = "http://cyberneko.org/html/features/report-errors";

    /** Notify character entity references (e.g. &amp;#32;, &amp;#x20;, etc). */
    public static final String NOTIFY_CHAR_REFS = "http://apache.org/xml/features/scanner/notify-char-refs";

    /** 
     * Notify handler of built-in entity references (e.g. &amp;amp;, 
     * &amp;lt;, etc).
     * <p>
     * <strong>Note:</strong>
     * This only applies to the five pre-defined XML general entities.
     * Specifically, "amp", "lt", "gt", "quot", and "apos". This is done 
     * for compatibility with the Xerces feature.
     * <p>
     * To be notified of the built-in entity references in HTML, set the 
     * <code>http://cyberneko.org/html/features/scanner/notify-builtin-refs</code> 
     * feature to <code>true</code>.
     */
    public static final String NOTIFY_XML_BUILTIN_REFS = "http://apache.org/xml/features/scanner/notify-builtin-refs";

    /** 
     * Notify handler of built-in entity references (e.g. &amp;nobr;, 
     * &amp;copy;, etc).
     * <p>
     * <strong>Note:</strong>
     * This <em>includes</em> the five pre-defined XML general entities.
     */
    public static final String NOTIFY_HTML_BUILTIN_REFS = "http://cyberneko.org/html/features/scanner/notify-builtin-refs";

    /** Fix Microsoft Windows&reg; character entity references. */
    public static final String FIX_MSWINDOWS_REFS = "http://cyberneko.org/html/features/scanner/fix-mswindows-refs";

    /** 
     * Strip HTML comment delimiters ("&lt;!&minus;&minus;" and 
     * "&minus;&minus;&gt;") from SCRIPT tag contents.
     */
    public static final String SCRIPT_STRIP_COMMENT_DELIMS = "http://cyberneko.org/html/features/scanner/script/strip-comment-delims";

    /** 
     * Strip XHTML CDATA delimiters ("&lt;![CDATA[" and "]]&gt;") from 
     * SCRIPT tag contents.
     */
    public static final String SCRIPT_STRIP_CDATA_DELIMS = "http://cyberneko.org/html/features/scanner/script/strip-cdata-delims";

    /** 
     * Strip HTML comment delimiters ("&lt;!&minus;&minus;" and 
     * "&minus;&minus;&gt;") from STYLE tag contents.
     */
    public static final String STYLE_STRIP_COMMENT_DELIMS = "http://cyberneko.org/html/features/scanner/style/strip-comment-delims";

    /** 
     * Strip XHTML CDATA delimiters ("&lt;![CDATA[" and "]]&gt;") from 
     * STYLE tag contents.
     */
    public static final String STYLE_STRIP_CDATA_DELIMS = "http://cyberneko.org/html/features/scanner/style/strip-cdata-delims";

    /**
     * Ignore specified charset found in the &lt;meta equiv='Content-Type'
     * content='text/html;charset=&hellip;'&gt; tag.
     */
    public static final String IGNORE_SPECIFIED_CHARSET = "http://cyberneko.org/html/features/scanner/ignore-specified-charset";

    /** Scan CDATA sections. */
    public static final String CDATA_SECTIONS = "http://cyberneko.org/html/features/scanner/cdata-sections";

    /** Override doctype declaration public and system identifiers. */
    public static final String OVERRIDE_DOCTYPE = "http://cyberneko.org/html/features/override-doctype";

    /** Insert document type declaration. */
    public static final String INSERT_DOCTYPE = "http://cyberneko.org/html/features/insert-doctype";

    /** Normalize attribute values. */
    protected static final String NORMALIZE_ATTRIBUTES = "http://cyberneko.org/html/features/normalize-attributes";

    /** Recognized features. */
    private static final String[] RECOGNIZED_FEATURES = {
        AUGMENTATIONS,
        REPORT_ERRORS,
        NOTIFY_CHAR_REFS,
        NOTIFY_XML_BUILTIN_REFS,
        NOTIFY_HTML_BUILTIN_REFS,
        FIX_MSWINDOWS_REFS,
        SCRIPT_STRIP_CDATA_DELIMS,
        SCRIPT_STRIP_COMMENT_DELIMS,
        STYLE_STRIP_CDATA_DELIMS,
        STYLE_STRIP_COMMENT_DELIMS,
        IGNORE_SPECIFIED_CHARSET,
        CDATA_SECTIONS,
        OVERRIDE_DOCTYPE,
        INSERT_DOCTYPE,
        NORMALIZE_ATTRIBUTES,
    };

    /** Recognized features defaults. */
    private static final Boolean[] RECOGNIZED_FEATURES_DEFAULTS = {
        null,
        null,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.FALSE,
        Boolean.TRUE,
    };

    // properties

    /** Modify HTML element names: { "upper", "lower", "default" }. */
    protected static final String NAMES_ELEMS = "http://cyberneko.org/html/properties/names/elems";

    /** Modify HTML attribute names: { "upper", "lower", "default" }. */
    protected static final String NAMES_ATTRS = "http://cyberneko.org/html/properties/names/attrs";
    
    /** Default encoding. */
    protected static final String DEFAULT_ENCODING = "http://cyberneko.org/html/properties/default-encoding";
    
    /** Error reporter. */
    protected static final String ERROR_REPORTER = "http://cyberneko.org/html/properties/error-reporter";

    /** Doctype declaration public identifier. */
    protected static final String DOCTYPE_PUBID = "http://cyberneko.org/html/properties/doctype/pubid";

    /** Doctype declaration system identifier. */
    protected static final String DOCTYPE_SYSID = "http://cyberneko.org/html/properties/doctype/sysid";

    /** Recognized properties. */
    private static final String[] RECOGNIZED_PROPERTIES = {
        NAMES_ELEMS,
        NAMES_ATTRS,
        DEFAULT_ENCODING,
        ERROR_REPORTER,
        DOCTYPE_PUBID,
        DOCTYPE_SYSID,
    };

    /** Recognized properties defaults. */
    private static final Object[] RECOGNIZED_PROPERTIES_DEFAULTS = {
        null,
        null,
        "Windows-1252",
        null,
        HTML_4_01_TRANSITIONAL_PUBID,
        HTML_4_01_TRANSITIONAL_SYSID,
    };

    // states

    /** State: content. */
    protected static final short STATE_CONTENT = 0;

    /** State: markup bracket. */
    protected static final short STATE_MARKUP_BRACKET = 1;

    /** State: start document. */
    protected static final short STATE_START_DOCUMENT = 10;

    /** State: end document. */
    protected static final short STATE_END_DOCUMENT = 11;

    // modify HTML names

    /** Don't modify HTML names. */
    protected static final short NAMES_NO_CHANGE = 0;

    /** Uppercase HTML names. */
    protected static final short NAMES_UPPERCASE = 1;

    /** Lowercase HTML names. */
    protected static final short NAMES_LOWERCASE = 2;

    // defaults

    /** Default buffer size. */
    protected static final int DEFAULT_BUFFER_SIZE = 2048;

    // debugging

    /** Set to true to debug changes in the scanner. */
    private static final boolean DEBUG_SCANNER = false;

    /** Set to true to debug changes in the scanner state. */
    private static final boolean DEBUG_SCANNER_STATE = false;

    /** Set to true to debug the buffer. */
    private static final boolean DEBUG_BUFFER = false;

    /** Set to true to debug character encoding handling. */
    private static final boolean DEBUG_CHARSET = false;

    /** Set to true to debug callbacks. */
    protected static final boolean DEBUG_CALLBACKS = false;
    
    // other
    
    /** Charset#forName method, if available. */
    protected static Method CHARSET_forName;
    
    /** CharsetDecoder#averageCharsPerByte method, if available. */
    protected static Method DECODER_averageCharsPerByte;
    
    // static vars

    /** Synthesized event info item. */
    protected static final HTMLEventInfo SYNTHESIZED_ITEM = 
        new HTMLEventInfo.SynthesizedItem();
        
    //
    // Static initializer
    //
    
    static {
    	try {
    		Class charsetClass = Class.forName("java.nio.charset.Charset");
			CHARSET_forName = charsetClass.getMethod("forName", new Class[]{String.class});

    		Class decoderClass = Class.forName("java.nio.charset.CharsetDecoder");
			DECODER_averageCharsPerByte = decoderClass.getMethod("averageCharsPerByte", (Class[])null);
    	}
    	catch (Exception e) {
    		// ignore
    	}
    }

    //
    // Data
    //

    // features

    /** Augmentations. */
    protected boolean fAugmentations;

    /** Report errors. */
    protected boolean fReportErrors;

    /** Notify character entity references. */
    protected boolean fNotifyCharRefs;

    /** Notify XML built-in general entity references. */
    protected boolean fNotifyXmlBuiltinRefs;

    /** Notify HTML built-in general entity references. */
    protected boolean fNotifyHtmlBuiltinRefs;

    /** Fix Microsoft Windows&reg; character entity references. */
    protected boolean fFixWindowsCharRefs;

    /** Strip CDATA delimiters from SCRIPT tags. */
    protected boolean fScriptStripCDATADelims;

    /** Strip comment delimiters from SCRIPT tags. */
    protected boolean fScriptStripCommentDelims;

    /** Strip CDATA delimiters from STYLE tags. */
    protected boolean fStyleStripCDATADelims;

    /** Strip comment delimiters from STYLE tags. */
    protected boolean fStyleStripCommentDelims;

    /** Ignore specified character set. */
    protected boolean fIgnoreSpecifiedCharset;

    /** CDATA sections. */
    protected boolean fCDATASections;

    /** Override doctype declaration public and system identifiers. */
    protected boolean fOverrideDoctype;

    /** Insert document type declaration. */
    protected boolean fInsertDoctype;

    /** Normalize attribute values. */
    protected boolean fNormalizeAttributes;

    // properties

    /** Modify HTML element names. */
    protected short fNamesElems;

    /** Modify HTML attribute names. */
    protected short fNamesAttrs;

    /** Default encoding. */
    protected String fDefaultIANAEncoding;

    /** Error reporter. */
    protected HTMLErrorReporter fErrorReporter;

    /** Doctype declaration public identifier. */
    protected String fDoctypePubid;

    /** Doctype declaration system identifier. */
    protected String fDoctypeSysid;

    // boundary locator information

    /** Beginning line number. */
    protected int fBeginLineNumber;

    /** Beginning column number. */
    protected int fBeginColumnNumber;

    /** Ending line number. */
    protected int fEndLineNumber;

    /** Ending column number. */
    protected int fEndColumnNumber;

    // state

    /** The playback byte stream. */
    protected PlaybackInputStream fByteStream;

    /** Current entity. */
    protected CurrentEntity fCurrentEntity;
    
    /** The current entity stack. */
    protected final Stack fCurrentEntityStack = new Stack();

    /** The current scanner. */
    protected Scanner fScanner;

    /** The current scanner state. */
    protected short fScannerState;

    /** The document handler. */
    protected XMLDocumentHandler fDocumentHandler;

    /** Auto-detected IANA encoding. */
    protected String fIANAEncoding;

    /** Auto-detected Java encoding. */
    protected String fJavaEncoding;

    /** True if the encoding matches "ISO-8859-*". */
    protected boolean fIso8859Encoding;

    /** Element count. */
    protected int fElementCount;

    /** Element depth. */
    protected int fElementDepth;

    // scanners

    /** Content scanner. */
    protected Scanner fContentScanner = new ContentScanner();

    /** 
     * Special scanner used for elements whose content needs to be scanned 
     * as plain text, ignoring markup such as elements and entity references.
     * For example: &lt;SCRIPT&gt; and &lt;COMMENT&gt;.
     */
    protected SpecialScanner fSpecialScanner = new SpecialScanner();

    // temp vars

    /** String. */
    protected final XMLString fString = new XMLString();

    /** String buffer. */
    protected final XMLStringBuffer fStringBuffer = new XMLStringBuffer(1024);

    /** String buffer. */
    private final XMLStringBuffer fStringBuffer2 = new XMLStringBuffer(1024);

    /** Non-normalized attribute string buffer. */
    private final XMLStringBuffer fNonNormAttr = new XMLStringBuffer(128);

    /** Augmentations. */
    private final HTMLAugmentations fInfosetAugs = new HTMLAugmentations();

    /** Location infoset item. */
    private final LocationItem fLocationItem = new LocationItem();

    /** Single boolean array. */
    private final boolean[] fSingleBoolean = { false };

    /** Resource identifier. */
    private final XMLResourceIdentifierImpl fResourceId = new XMLResourceIdentifierImpl();

    //
    // Public methods
    //

    /** 
     * Pushes an input source onto the current entity stack. This 
     * enables the scanner to transparently scan new content (e.g. 
     * the output written by an embedded script). At the end of the
     * current entity, the scanner returns where it left off at the
     * time this entity source was pushed.
     * <p>
     * <strong>Note:</strong>
     * This functionality is experimental at this time and is
     * subject to change in future releases of NekoHTML.
     *
     * @param inputSource The new input source to start scanning.
     */
    public void pushInputSource(XMLInputSource inputSource) {
        Reader reader = inputSource.getCharacterStream();
        if (reader == null) {
            throw new IllegalArgumentException("pushed input source has no reader");
        }
        fCurrentEntityStack.push(fCurrentEntity);
        String encoding = inputSource.getEncoding();
        String publicId = inputSource.getPublicId();
        String baseSystemId = inputSource.getBaseSystemId();
        String literalSystemId = inputSource.getSystemId();
        String expandedSystemId = expandSystemId(literalSystemId, baseSystemId);
        fCurrentEntity = new CurrentEntity(reader, encoding, 
                                           publicId, baseSystemId,
                                           literalSystemId, expandedSystemId);
    } // pushInputSource(XMLInputSource)

    /**
     * Cleans up used resources. For example, if scanning is terminated
     * early, then this method ensures all remaining open streams are
     * closed.
     *
     * @param closeall Close all streams, including the original.
     *                 This is used in cases when the application has
     *                 opened the original document stream and should
     *                 be responsible for closing it.
     */
    public void cleanup(boolean closeall) {
        int size = fCurrentEntityStack.size();
        if (size > 0) {
            // current entity is not the original, so close it
            if (fCurrentEntity != null) {
                try {
                    fCurrentEntity.stream.close();
                }
                catch (IOException e) {
                    // ignore
                }
            }
            // close remaining streams
            for (int i = closeall ? 0 : 1; i < size; i++) {
                fCurrentEntity = (CurrentEntity)fCurrentEntityStack.pop();
                try {
                    fCurrentEntity.stream.close();
                }
                catch (IOException e) {
                    // ignore
                }
            }
        }
        else if (closeall && fCurrentEntity != null) {
            try {
                fCurrentEntity.stream.close();
            }
            catch (IOException e) {
                // ignore
            }
        }
    } // cleanup(boolean)

    //
    // XMLLocator methods
    //

    /** Returns the encoding. */
    public String getEncoding() {
        return fCurrentEntity != null ? fCurrentEntity.encoding : null;
    } // getEncoding():String

    /** Returns the public identifier. */
    public String getPublicId() { 
        return fCurrentEntity != null ? fCurrentEntity.publicId : null; 
    } // getPublicId():String

    /** Returns the base system identifier. */
    public String getBaseSystemId() { 
        return fCurrentEntity != null ? fCurrentEntity.baseSystemId : null; 
    } // getBaseSystemId():String

    /** Returns the literal system identifier. */
    public String getLiteralSystemId() { 
        return fCurrentEntity != null ? fCurrentEntity.literalSystemId : null; 
    } // getLiteralSystemId():String

    /** Returns the expanded system identifier. */
    public String getExpandedSystemId() { 
        return fCurrentEntity != null ? fCurrentEntity.expandedSystemId : null; 
    } // getExpandedSystemId():String

    /** Returns the current line number. */
    public int getLineNumber() { 
        return fCurrentEntity != null ? fCurrentEntity.lineNumber : -1; 
    } // getLineNumber():int

    /** Returns the current column number. */
    public int getColumnNumber() { 
        return fCurrentEntity != null ? fCurrentEntity.columnNumber : -1; 
    } // getColumnNumber():int
    
    /** Returns the XML version. */
    public String getXMLVersion() {
		return fCurrentEntity != null ? fCurrentEntity.version : null; 
    } // getXMLVersion():String
    
    /** Returns the character offset. */
    public int getCharacterOffset() {
		return fCurrentEntity != null ? fCurrentEntity.charOffset : -1; 
    } // getCharacterOffset():int

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
        fNotifyCharRefs = manager.getFeature(NOTIFY_CHAR_REFS);
        fNotifyXmlBuiltinRefs = manager.getFeature(NOTIFY_XML_BUILTIN_REFS);
        fNotifyHtmlBuiltinRefs = manager.getFeature(NOTIFY_HTML_BUILTIN_REFS);
        fFixWindowsCharRefs = manager.getFeature(FIX_MSWINDOWS_REFS);
        fScriptStripCDATADelims = manager.getFeature(SCRIPT_STRIP_CDATA_DELIMS);
        fScriptStripCommentDelims = manager.getFeature(SCRIPT_STRIP_COMMENT_DELIMS);
        fStyleStripCDATADelims = manager.getFeature(STYLE_STRIP_CDATA_DELIMS);
        fStyleStripCommentDelims = manager.getFeature(STYLE_STRIP_COMMENT_DELIMS);
        fIgnoreSpecifiedCharset = manager.getFeature(IGNORE_SPECIFIED_CHARSET);
        fCDATASections = manager.getFeature(CDATA_SECTIONS);
        fOverrideDoctype = manager.getFeature(OVERRIDE_DOCTYPE);
        fInsertDoctype = manager.getFeature(INSERT_DOCTYPE);
        fNormalizeAttributes = manager.getFeature(NORMALIZE_ATTRIBUTES);

        // get properties
        fNamesElems = getNamesValue(String.valueOf(manager.getProperty(NAMES_ELEMS)));
        fNamesAttrs = getNamesValue(String.valueOf(manager.getProperty(NAMES_ATTRS)));
        fDefaultIANAEncoding = String.valueOf(manager.getProperty(DEFAULT_ENCODING));
        fErrorReporter = (HTMLErrorReporter)manager.getProperty(ERROR_REPORTER);
        fDoctypePubid = String.valueOf(manager.getProperty(DOCTYPE_PUBID));
        fDoctypeSysid = String.valueOf(manager.getProperty(DOCTYPE_SYSID));
    
    } // reset(XMLComponentManager)

    /** Sets a feature. */
    public void setFeature(String featureId, boolean state)
        throws XMLConfigurationException {

        if (featureId.equals(AUGMENTATIONS)) { 
            fAugmentations = state; 
        }
        else if (featureId.equals(IGNORE_SPECIFIED_CHARSET)) { 
            fIgnoreSpecifiedCharset = state; 
        }
        else if (featureId.equals(NOTIFY_CHAR_REFS)) { 
            fNotifyCharRefs = state; 
        }
        else if (featureId.equals(NOTIFY_XML_BUILTIN_REFS)) { 
            fNotifyXmlBuiltinRefs = state; 
        }
        else if (featureId.equals(NOTIFY_HTML_BUILTIN_REFS)) { 
            fNotifyHtmlBuiltinRefs = state; 
        }
        else if (featureId.equals(FIX_MSWINDOWS_REFS)) { 
            fFixWindowsCharRefs = state; 
        }
        else if (featureId.equals(SCRIPT_STRIP_CDATA_DELIMS)) { 
            fScriptStripCDATADelims = state; 
        }
        else if (featureId.equals(SCRIPT_STRIP_COMMENT_DELIMS)) { 
            fScriptStripCommentDelims = state; 
        }
        else if (featureId.equals(STYLE_STRIP_CDATA_DELIMS)) { 
            fStyleStripCDATADelims = state; 
        }
        else if (featureId.equals(STYLE_STRIP_COMMENT_DELIMS)) { 
            fStyleStripCommentDelims = state; 
        }
        else if (featureId.equals(IGNORE_SPECIFIED_CHARSET)) { 
            fIgnoreSpecifiedCharset = state; 
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

        if (propertyId.equals(DEFAULT_ENCODING)) {
            fDefaultIANAEncoding = String.valueOf(value);
            return;
        }

    } // setProperty(String,Object)

    //
    // XMLDocumentScanner methods
    //

    /** Sets the input source. */
    public void setInputSource(XMLInputSource source) throws IOException {

        // reset state
        fElementCount = 0;
        fElementDepth = -1;
        fByteStream = null;
        fCurrentEntityStack.removeAllElements();

        fBeginLineNumber = 1;
        fBeginColumnNumber = 1;
        fEndLineNumber = fBeginLineNumber;
        fEndColumnNumber = fBeginColumnNumber;

        // reset encoding information
        fIANAEncoding = fDefaultIANAEncoding;
        fJavaEncoding = fIANAEncoding;

        // get location information
        String encoding = source.getEncoding();
        String publicId = source.getPublicId();
        String baseSystemId = source.getBaseSystemId();
        String literalSystemId = source.getSystemId();
        String expandedSystemId = expandSystemId(literalSystemId, baseSystemId);

        // open stream
        Reader reader = source.getCharacterStream();
        if (reader == null) {
            InputStream inputStream = source.getByteStream();
            if (inputStream == null) {
                URL url = new URL(expandedSystemId);
                inputStream = url.openStream();
            }
            fByteStream = new PlaybackInputStream(inputStream);
            String[] encodings = new String[2];
            if (encoding == null) {
                fByteStream.detectEncoding(encodings);
            }
            else {
                encodings[0] = encoding;
            }
            if (encodings[0] == null) {
                encodings[0] = fDefaultIANAEncoding;
                if (fReportErrors) {
                    fErrorReporter.reportWarning("HTML1000", null);
                }
            }
            if (encodings[1] == null) {
                encodings[1] = EncodingMap.getIANA2JavaMapping(encodings[0].toUpperCase());
                if (encodings[1] == null) {
                    encodings[1] = encodings[0];
                    if (fReportErrors) {
                        fErrorReporter.reportWarning("HTML1001", new Object[]{encodings[0]});
                    }
                }
            }
            fIANAEncoding = encodings[0];
            fJavaEncoding = encodings[1];
            /* PATCH: Asgeir Asgeirsson */
            fIso8859Encoding = fIANAEncoding == null 
                            || fIANAEncoding.toUpperCase().startsWith("ISO-8859")
                            || fIANAEncoding.equalsIgnoreCase(fDefaultIANAEncoding);
            encoding = fIANAEncoding;
            reader = new InputStreamReader(fByteStream, fJavaEncoding);
        }
        fCurrentEntity = new CurrentEntity(reader, encoding,
                                           publicId, baseSystemId,
                                           literalSystemId, expandedSystemId);

        // set scanner and state
        setScanner(fContentScanner);
        setScannerState(STATE_START_DOCUMENT);

    } // setInputSource(XMLInputSource)

    /** Scans the document. */
    public boolean scanDocument(boolean complete) throws XNIException, IOException {
        do {
            if (!fScanner.scan(complete)) {
                return false;
            }
        } while (complete);
        return true;
    } // scanDocument(boolean):boolean

    /** Sets the document handler. */
    public void setDocumentHandler(XMLDocumentHandler handler) {
        fDocumentHandler = handler;
    } // setDocumentHandler(XMLDocumentHandler)

    // @since Xerces 2.1.0

    /** Returns the document handler. */
    public XMLDocumentHandler getDocumentHandler() {
        return fDocumentHandler;
    } // getDocumentHandler():XMLDocumentHandler

    //
    // Protected static methods
    //

    /** Returns the value of the specified attribute, ignoring case. */
    protected static String getValue(XMLAttributes attrs, String aname) {
        int length = attrs != null ? attrs.getLength() : 0;
        for (int i = 0; i < length; i++) {
            if (attrs.getQName(i).equalsIgnoreCase(aname)) {
                return attrs.getValue(i);
            }
        }
        return null;
    } // getValue(XMLAttributes,String):String

    /**
     * Expands a system id and returns the system id as a URI, if
     * it can be expanded. A return value of null means that the
     * identifier is already expanded. An exception thrown
     * indicates a failure to expand the id.
     *
     * @param systemId The systemId to be expanded.
     *
     * @return Returns the URI string representing the expanded system
     *         identifier. A null value indicates that the given
     *         system identifier is already expanded.
     *
     */
    public static String expandSystemId(String systemId, String baseSystemId) {

        // check for bad parameters id
        if (systemId == null || systemId.length() == 0) {
            return systemId;
        }
        // if id already expanded, return
        try {
            URI uri = new URI(systemId);
            if (uri != null) {
                return systemId;
            }
        }
        catch (URI.MalformedURIException e) {
            // continue on...
        }
        // normalize id
        String id = fixURI(systemId);

        // normalize base
        URI base = null;
        URI uri = null;
        try {
            if (baseSystemId == null || baseSystemId.length() == 0 ||
                baseSystemId.equals(systemId)) {
                String dir;
                try {
                    dir = fixURI(System.getProperty("user.dir"));
                }
                catch (SecurityException se) {
                    dir = "";
                }
                if (!dir.endsWith("/")) {
                    dir = dir + "/";
                }
                base = new URI("file", "", dir, null, null);
            }
            else {
                try {
                    base = new URI(fixURI(baseSystemId));
                }
                catch (URI.MalformedURIException e) {
                    String dir;
                    try {
                        dir = fixURI(System.getProperty("user.dir"));
                    }
                    catch (SecurityException se) {
                        dir = "";
                    }
                    if (baseSystemId.indexOf(':') != -1) {
                        // for xml schemas we might have baseURI with
                        // a specified drive
                        base = new URI("file", "", fixURI(baseSystemId), null, null);
                    }
                    else {
                        if (!dir.endsWith("/")) {
                            dir = dir + "/";
                        }
                        dir = dir + fixURI(baseSystemId);
                        base = new URI("file", "", dir, null, null);
                    }
                }
             }
             // expand id
             uri = new URI(base, id);
        }
        catch (URI.MalformedURIException e) {
            // let it go through
        }

        if (uri == null) {
            return systemId;
        }
        return uri.toString();

    } // expandSystemId(String,String):String

    /**
     * Fixes a platform dependent filename to standard URI form.
     *
     * @param str The string to fix.
     *
     * @return Returns the fixed URI string.
     */
    protected static String fixURI(String str) {

        // handle platform dependent strings
        str = str.replace(java.io.File.separatorChar, '/');

        // Windows fix
        if (str.length() >= 2) {
            char ch1 = str.charAt(1);
            // change "C:blah" to "/C:blah"
            if (ch1 == ':') {
                char ch0 = Character.toUpperCase(str.charAt(0));
                if (ch0 >= 'A' && ch0 <= 'Z') {
                    str = "/" + str;
                }
            }
            // change "//blah" to "file://blah"
            else if (ch1 == '/' && str.charAt(0) == '/') {
                str = "file:" + str;
            }
        }

        // done
        return str;

    } // fixURI(String):String

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

    /**
     * Fixes Microsoft Windows&reg; specific characters.
     * <p>
     * Details about this common problem can be found at 
     * <a href='http://www.cs.tut.fi/~jkorpela/www/windows-chars.html'>http://www.cs.tut.fi/~jkorpela/www/windows-chars.html</a>
     */
    protected int fixWindowsCharacter(int origChar) {
        /* PATCH: Asgeir Asgeirsson */
        switch(origChar) {
            case 130: return 8218;
            case 131: return 402;
            case 132: return 8222;
            case 133: return 8230;
            case 134: return 8224;
            case 135: return 8225;
            case 136: return 710;
            case 137: return 8240;
            case 138: return 352;
            case 139: return 8249;
            case 140: return 338;
            case 145: return 8216;
            case 146: return 8217;
            case 147: return 8220;
            case 148: return 8221;
            case 149: return 8226;
            case 150: return 8211;
            case 151: return 8212;
            case 152: return 732;
            case 153: return 8482;
            case 154: return 353;
            case 155: return 8250;
            case 156: return 339;
            case 159: return 376;
        }
        return origChar;
    } // fixWindowsCharacter(int):int

    //
    // Protected methods
    //

    // i/o

    /** Reads a single character. */
    protected int read() throws IOException {
        if (DEBUG_BUFFER) { 
            System.out.print("(read: ");
            printBuffer();
            System.out.println();
        }
        if (fCurrentEntity.offset == fCurrentEntity.length) {
            if (load(0) == -1) {
                if (DEBUG_BUFFER) { 
                    System.out.println(")read: -> -1");
                }
                return -1;
            }
        }
        int c = fCurrentEntity.buffer[fCurrentEntity.offset++];
        fCurrentEntity.columnNumber++;
        if (DEBUG_BUFFER) { 
            System.out.print(")read: ");
            printBuffer();
            System.out.print(" -> ");
            System.out.print(c);
            System.out.println();
        }
        return c;
    } // read():int

    /** 
     * Loads a new chunk of data into the buffer and returns the number of
     * characters loaded or -1 if no additional characters were loaded.
     *
     * @param offset The offset at which new characters should be loaded.
     */
    protected int load(int offset) throws IOException {
        if (DEBUG_BUFFER) { 
            System.out.print("(load: ");
            printBuffer();
            System.out.println();
        }
        // resize buffer, if needed
        if (offset == fCurrentEntity.buffer.length) {
            int adjust = fCurrentEntity.buffer.length / 4;
            char[] array = new char[fCurrentEntity.buffer.length + adjust];
            System.arraycopy(fCurrentEntity.buffer, 0, array, 0, fCurrentEntity.length);
            fCurrentEntity.buffer = array;
        }
        // read a block of characters
        int count = fCurrentEntity.stream.read(fCurrentEntity.buffer, offset, fCurrentEntity.buffer.length - offset);
        fCurrentEntity.length = count != -1 ? count + offset : offset;
        fCurrentEntity.offset = offset;
        if (DEBUG_BUFFER) { 
            System.out.print(")load: ");
            printBuffer();
            System.out.print(" -> ");
            System.out.print(count);
            System.out.println();
        }
        return count;
    } // load():int

    // debugging

    /** Sets the scanner. */
    protected void setScanner(Scanner scanner) {
        fScanner = scanner;
        if (DEBUG_SCANNER) {
            System.out.print("$$$ setScanner(");
            System.out.print(scanner!=null?scanner.getClass().getName():"null");
            System.out.println(");");
        }
    } // setScanner(Scanner)
    
    /** Sets the scanner state. */
    protected void setScannerState(short state) {
        fScannerState = state;
        if (DEBUG_SCANNER_STATE) {
            System.out.print("$$$ setScannerState(");
            switch (fScannerState) {
                case STATE_CONTENT: { System.out.print("STATE_CONTENT"); break; }
                case STATE_MARKUP_BRACKET: { System.out.print("STATE_MARKUP_BRACKET"); break; }
                case STATE_START_DOCUMENT: { System.out.print("STATE_START_DOCUMENT"); break; }
                case STATE_END_DOCUMENT: { System.out.print("STATE_END_DOCUMENT"); break; }
            }
            System.out.println(");");
        }
    } // setScannerState(short)

    // scanning

    /** Scans a DOCTYPE line. */
    protected void scanDoctype() throws IOException {
        String root = null;
        String pubid = null;
        String sysid = null;

        if (skipSpaces()) {
            root = scanName();
            if (root == null) {
                if (fReportErrors) {
                    fErrorReporter.reportError("HTML1014", null);
                }
            }
            else {
                root = modifyName(root, fNamesElems);
            }
            if (skipSpaces()) {
                if (skip("PUBLIC", false)) {
                    skipSpaces();
                    pubid = scanLiteral();
                    if (skipSpaces()) {
                        sysid = scanLiteral();
                    }
                }
                else if (skip("SYSTEM", false)) {
                    skipSpaces();
                    sysid = scanLiteral();
                }
            }
        }
        int c;
        while ((c = read()) != -1) {
            if (c == '<') {
                fCurrentEntity.offset--;
                fCurrentEntity.columnNumber--;
                break;
            }
            if (c == '>') {
                break;
            }
            if (c == '[') {
                skipMarkup(true);
                break;
            }
        }

        if (fDocumentHandler != null) {
            if (fOverrideDoctype) {
                pubid = fDoctypePubid;
                sysid = fDoctypeSysid;
            }
            fEndLineNumber = fCurrentEntity.lineNumber;
            fEndColumnNumber = fCurrentEntity.columnNumber;
            fDocumentHandler.doctypeDecl(root, pubid, sysid, locationAugs());
        }

    } // scanDoctype()

    /** Scans a quoted literal. */
    protected String scanLiteral() throws IOException {
        int quote = read();
        if (quote == '\'' || quote == '"') {
            StringBuffer str = new StringBuffer();
            int c;
            while ((c = read()) != -1) {
                if (c == quote) {
                    break;
                }
                if (c == '\r' || c == '\n') {
                    fCurrentEntity.offset--;
                    fCurrentEntity.columnNumber--;
                    // NOTE: This collapses newlines to a single space.
                    //       [Q] Is this the right thing to do here? -Ac
                    skipNewlines();
                    str.append(' ');
                }
                else if (c == '<') {
                    fCurrentEntity.offset--;
                    fCurrentEntity.columnNumber--;
                    break;
                }
                else {
                    str.append((char)c);
                }
            }
            if (c == -1) {
                if (fReportErrors) {
                    fErrorReporter.reportError("HTML1007", null);
                }
                throw new EOFException();
            }
            return str.toString();
        }
        else {
            fCurrentEntity.offset--;
            fCurrentEntity.columnNumber--;
        }
        return null;
    } // scanLiteral():String

    /** Scans a name. */
    protected String scanName() throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(scanName: ");
            printBuffer();
            System.out.println();
        }
        if (fCurrentEntity.offset == fCurrentEntity.length) {
            if (load(0) == -1) {
                if (DEBUG_BUFFER) {
                    System.out.print(")scanName: ");
                    printBuffer();
                    System.out.println(" -> null");
                }
                return null;
            }
        }
        int offset = fCurrentEntity.offset;
        while (true) {
            while (fCurrentEntity.offset < fCurrentEntity.length) {
                char c = fCurrentEntity.buffer[fCurrentEntity.offset];
                if (!Character.isLetterOrDigit(c) &&
                    !(c == '-' || c == '.' || c == ':' || c == '_')) {
                    break;
                }
                fCurrentEntity.offset++;
                fCurrentEntity.columnNumber++;
            }
            if (fCurrentEntity.offset == fCurrentEntity.length) {
                int length = fCurrentEntity.length - offset;
                System.arraycopy(fCurrentEntity.buffer, offset, fCurrentEntity.buffer, 0, length);
                int count = load(length);
                offset = 0;
                if (count == -1) {
                    break;
                }
            }
            else {
                break;
            }
        }
        int length = fCurrentEntity.offset - offset;
        String name = length > 0 ? new String(fCurrentEntity.buffer, offset, length) : null;
        if (DEBUG_BUFFER) {
            System.out.print(")scanName: ");
            printBuffer();
            System.out.print(" -> \"");
            System.out.print(name);
            System.out.println('"');
        }
        return name;
    } // scanName():String

    /** Scans an entity reference. */
    protected int scanEntityRef(XMLStringBuffer str, boolean content) 
        throws IOException {
        str.clear();
        str.append('&');
        while (true) {
            int c = read();
            if (c == ';') {
                str.append(';');
                break;
            }
            if (c == -1) {
                if (fReportErrors) {
                    fErrorReporter.reportWarning("HTML1004", null);
                }
                if (content && fDocumentHandler != null && fElementCount >= fElementDepth) {
                    fEndLineNumber = fCurrentEntity.lineNumber;
                    fEndColumnNumber = fCurrentEntity.columnNumber;
                    fDocumentHandler.characters(str, locationAugs());
                }
                return -1;
            }
            if (!Character.isLetterOrDigit((char)c) && c != '#') {
                if (fReportErrors) {
                    fErrorReporter.reportWarning("HTML1004", null);
                }
                fCurrentEntity.offset--;
                fCurrentEntity.columnNumber--;
                if (content && fDocumentHandler != null && fElementCount >= fElementDepth) {
                    fEndLineNumber = fCurrentEntity.lineNumber;
                    fEndColumnNumber = fCurrentEntity.columnNumber;
                    fDocumentHandler.characters(str, locationAugs());
                }
                return -1;
            }
            str.append((char)c);
        }
        if (str.length == 1) {
            if (content && fDocumentHandler != null && fElementCount >= fElementDepth) {
                fEndLineNumber = fCurrentEntity.lineNumber;
                fEndColumnNumber = fCurrentEntity.columnNumber;
                fDocumentHandler.characters(str, locationAugs());
            }
            return -1;
        }

        String name = str.toString().substring(1, str.length-1);
        if (name.startsWith("#")) {
            int value = -1;
            try {
                if (name.startsWith("#x")) {
                    value = Integer.parseInt(name.substring(2), 16);
                }
                else {
                    value = Integer.parseInt(name.substring(1));
                }
                /* PATCH: Asgeir Asgeirsson */
                if (fFixWindowsCharRefs && fIso8859Encoding) {
                    value = fixWindowsCharacter(value);
                }
                if (content && fDocumentHandler != null && fElementCount >= fElementDepth) {
                    fEndLineNumber = fCurrentEntity.lineNumber;
                    fEndColumnNumber = fCurrentEntity.columnNumber;
                    if (fNotifyCharRefs) {
                        XMLResourceIdentifier id = resourceId();
                        String encoding = null;
                        fDocumentHandler.startGeneralEntity(name, id, encoding, locationAugs());
                    }
                    str.clear();
                    str.append((char)value);
                    fDocumentHandler.characters(str, locationAugs());
                    if (fNotifyCharRefs) {
                        fDocumentHandler.endGeneralEntity(name, locationAugs());
                    }
                }
            }
            catch (NumberFormatException e) {
                if (fReportErrors) {
                    fErrorReporter.reportError("HTML1005", new Object[]{name});
                }
                if (content && fDocumentHandler != null && fElementCount >= fElementDepth) {
                    fEndLineNumber = fCurrentEntity.lineNumber;
                    fEndColumnNumber = fCurrentEntity.columnNumber;
                    fDocumentHandler.characters(str, locationAugs());
                }
            }
            return value;
        }

        int c = HTMLEntities.get(name);
        if (c == -1) {
            if (fReportErrors) {
                fErrorReporter.reportWarning("HTML1006", new Object[]{name});
            }
            if (content && fDocumentHandler != null && fElementCount >= fElementDepth) {
                fEndLineNumber = fCurrentEntity.lineNumber;
                fEndColumnNumber = fCurrentEntity.columnNumber;
                fDocumentHandler.characters(str, locationAugs());
            }
            return -1;
        }
        if (content && fDocumentHandler != null && fElementCount >= fElementDepth) {
            fEndLineNumber = fCurrentEntity.lineNumber;
            fEndColumnNumber = fCurrentEntity.columnNumber;
            boolean notify = fNotifyHtmlBuiltinRefs || (fNotifyXmlBuiltinRefs && builtinXmlRef(name));
            if (notify) {
                XMLResourceIdentifier id = resourceId();
                String encoding = null;
                fDocumentHandler.startGeneralEntity(name, id, encoding, locationAugs());
            }
            str.clear();
            str.append((char)c);
            fDocumentHandler.characters(str, locationAugs());
            if (notify) {
                fDocumentHandler.endGeneralEntity(name, locationAugs());
            }
        }
        return c;

    } // scanEntityRef(XMLStringBuffer,boolean):int

    /** Returns true if the specified text is present and is skipped. */
    protected boolean skip(String s, boolean caseSensitive) throws IOException {
        int length = s != null ? s.length() : 0;
        for (int i = 0; i < length; i++) {
            if (fCurrentEntity.offset == fCurrentEntity.length) {
                System.arraycopy(fCurrentEntity.buffer, fCurrentEntity.offset - i, fCurrentEntity.buffer, 0, i);
                if (load(i) == -1) {
                    fCurrentEntity.offset = 0;
                    return false;
                }
            }
            char c0 = s.charAt(i);
            char c1 = fCurrentEntity.buffer[fCurrentEntity.offset++];
            fCurrentEntity.columnNumber++;
            if (!caseSensitive) {
                c0 = Character.toUpperCase(c0);
                c1 = Character.toUpperCase(c1);
            }
            if (c0 != c1) {
                fCurrentEntity.offset -= i + 1;
                return false;
            }
        }
        return true;
    } // skip(String):boolean

    /** Skips markup. */
    protected boolean skipMarkup(boolean balance) throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(skipMarkup: ");
            printBuffer();
            System.out.println();
        }
        int depth = 1;
        boolean slashgt = false;
        OUTER: while (true) {
            if (fCurrentEntity.offset == fCurrentEntity.length) {
                if (load(0) == -1) {
                    break OUTER;
                }
            }
            while (fCurrentEntity.offset < fCurrentEntity.length) {
                char c = fCurrentEntity.buffer[fCurrentEntity.offset++];
                fCurrentEntity.columnNumber++;
                if (balance && c == '<') {
                    depth++;
                }
                else if (c == '>') {
                    depth--;
                    if (depth == 0) {
                        break OUTER;
                    }
                }
                else if (c == '/') {
                    if (fCurrentEntity.offset == fCurrentEntity.length) {
                        if (load(0) == -1) {
                            break OUTER;
                        }
                    }
                    c = fCurrentEntity.buffer[fCurrentEntity.offset++];
                    fCurrentEntity.columnNumber++;
                    if (c == '>') {
                        slashgt = true;
                        depth--;
                        if (depth == 0) {
                            break OUTER;
                        }
                    }
                    else {
                        fCurrentEntity.offset--;
                        fCurrentEntity.columnNumber--;
                    }
                }
                else if (c == '\r' || c == '\n') {
                    skipNewlines();
                }
            }
        }
        if (DEBUG_BUFFER) {
            System.out.print(")skipMarkup: ");
            printBuffer();
            System.out.print(" -> "+slashgt);
            System.out.println();
        }
        return slashgt;
    } // skipMarkup():boolean

    /** Skips whitespace. */
    protected boolean skipSpaces() throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(skipSpaces: ");
            printBuffer();
            System.out.println();
        }
        boolean spaces = false;
        while (true) {
            if (fCurrentEntity.offset == fCurrentEntity.length) {
                if (load(0) == -1) {
                    break;
                }
            }
            char c = fCurrentEntity.buffer[fCurrentEntity.offset];
            if (!Character.isSpace(c)) {
                break;
            }
            spaces = true;
            if (c == '\r' || c == '\n') {
                skipNewlines();
                continue;
            }
            fCurrentEntity.offset++;
            fCurrentEntity.columnNumber++;
        }
        if (DEBUG_BUFFER) {
            System.out.print(")skipSpaces: ");
            printBuffer();
            System.out.print(" -> ");
            System.out.print(spaces);
            System.out.println();
        }
        return spaces;
    } // skipSpaces()

    /** Skips newlines and returns the number of newlines skipped. */
    protected int skipNewlines() throws IOException {
        return skipNewlines(Integer.MAX_VALUE);
    } // skipNewlines():int

    /** Skips newlines and returns the number of newlines skipped. */
    protected int skipNewlines(int maxlines) throws IOException {
        if (DEBUG_BUFFER) {
            System.out.print("(skipNewlines: ");
            printBuffer();
            System.out.println();
        }
        if (fCurrentEntity.offset == fCurrentEntity.length) {
            if (load(0) == -1) {
                if (DEBUG_BUFFER) {
                    System.out.print(")skipNewlines: ");
                    printBuffer();
                    System.out.println();
                }
                return 0;
            }
        }
        char c = fCurrentEntity.buffer[fCurrentEntity.offset];
        int newlines = 0;
        int offset = fCurrentEntity.offset;
        if (c == '\n' || c == '\r') {
            do {
                c = fCurrentEntity.buffer[fCurrentEntity.offset++];
                if (c == '\r') {
                    newlines++;
                    if (fCurrentEntity.offset == fCurrentEntity.length) {
                        offset = 0;
                        fCurrentEntity.offset = newlines;
                        if (load(newlines) == -1) {
                            break;
                        }
                    }
                    if (fCurrentEntity.buffer[fCurrentEntity.offset] == '\n') {
                        fCurrentEntity.offset++;
                        offset++;
                    }
                }
                else if (c == '\n') {
                    newlines++;
                    if (fCurrentEntity.offset == fCurrentEntity.length) {
                        offset = 0;
                        fCurrentEntity.offset = newlines;
                        if (load(newlines) == -1) {
                            break;
                        }
                    }
                }
                else {
                    fCurrentEntity.offset--;
                    break;
                }
            } while (newlines < maxlines &&
                     fCurrentEntity.offset < fCurrentEntity.length - 1);
            fCurrentEntity.lineNumber += newlines;
            fCurrentEntity.columnNumber = 1;
        }
        if (DEBUG_BUFFER) {
            System.out.print(")skipNewlines: ");
            printBuffer();
            System.out.print(" -> ");
            System.out.print(newlines);
            System.out.println();
        }
        return newlines;
    } // skipNewlines(int):int

    // infoset utility methods

    /** Returns an augmentations object with a location item added. */
    protected final Augmentations locationAugs() {
        HTMLAugmentations augs = null;
        if (fAugmentations) {
            fLocationItem.setValues(fBeginLineNumber, fBeginColumnNumber, 
                                    fEndLineNumber, fEndColumnNumber);
            augs = fInfosetAugs;
            augs.removeAllItems();
            augs.putItem(AUGMENTATIONS, fLocationItem);
        }
        return augs;
    } // locationAugs():Augmentations

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

    /** Returns an empty resource identifier. */
    protected final XMLResourceIdentifier resourceId() {
        /***/
        fResourceId.clear();
        return fResourceId;
        /***
        // NOTE: Unfortunately, the Xerces DOM parser classes expect a
        //       non-null resource identifier object to be passed to
        //       startGeneralEntity. -Ac
        return null;
        /***/
    } // resourceId():XMLResourceIdentifier

    //
    // Protected static methods
    //

    /** Returns true if the name is a built-in XML general entity reference. */
    protected static boolean builtinXmlRef(String name) {
        return name.equals("amp") || name.equals("lt") || name.equals("gt") ||
               name.equals("quot") || name.equals("apos");
    } // builtinXmlRef(String):boolean

    //
    // Private methods
    //

    /** Prints the contents of the character buffer to standard out. */
    private void printBuffer() {
        if (DEBUG_BUFFER) {
            System.out.print('[');
            System.out.print(fCurrentEntity.length);
            System.out.print(' ');
            System.out.print(fCurrentEntity.offset);
            if (fCurrentEntity.length > 0) {
                System.out.print(" \"");
                for (int i = 0; i < fCurrentEntity.length; i++) {
                    if (i == fCurrentEntity.offset) {
                        System.out.print('^');
                    }
                    char c = fCurrentEntity.buffer[i];
                    switch (c) {
                        case '\r': {
                            System.out.print("\\r");
                            break;
                        }
                        case '\n': {
                            System.out.print("\\n");
                            break;
                        }
                        case '\t': {
                            System.out.print("\\t");
                            break;
                        }
                        case '"': {
                            System.out.print("\\\"");
                            break;
                        }
                        default: {
                            System.out.print(c);
                        }
                    }
                }
                if (fCurrentEntity.offset == fCurrentEntity.length) {
                    System.out.print('^');
                }
                System.out.print('"');
            }
            System.out.print(']');
        }
    } // printBuffer()

    //
    // Interfaces
    //

    /**
     * Basic scanner interface.
     *
     * @author Andy Clark
     */
    public interface Scanner {

        //
        // Scanner methods
        //

        /** 
         * Scans part of the document. This interface allows scanning to
         * be performed in a pulling manner.
         *
         * @param complete True if the scanner should not return until
         *                 scanning is complete.
         *
         * @return True if additional scanning is required.
         *
         * @throws IOException Thrown if I/O error occurs.
         */
        public boolean scan(boolean complete) throws IOException;

    } // interface Scanner

    //
    // Classes
    //

    /**
     * Current entity.
     *
     * @author Andy Clark
     */
    public static class CurrentEntity {

        //
        // Data
        //

        /** Character stream. */
        public Reader stream;

        /** Encoding. */
        public String encoding;

        /** Public identifier. */
        public String publicId;

        /** Base system identifier. */
        public String baseSystemId;

        /** Literal system identifier. */
        public String literalSystemId;

        /** Expanded system identifier. */
        public String expandedSystemId;

		/** XML version. */
		public String version = "1.0";

        /** Line number. */
        public int lineNumber = 1;

        /** Column number. */
        public int columnNumber = 1;
        
        /** Character offset. */
        public int charOffset = -1;

        // buffer

        /** Character buffer. */
        public char[] buffer = new char[DEFAULT_BUFFER_SIZE];

        /** Offset into character buffer. */
        public int offset = 0;

        /** Length of characters read into character buffer. */
        public int length = 0;

        //
        // Constructors
        //

        /** Constructs an entity from the specified stream. */
        public CurrentEntity(Reader stream, String encoding, 
                             String publicId, String baseSystemId,
                             String literalSystemId, String expandedSystemId) {
            this.stream = stream;
            this.encoding = encoding;
            this.publicId = publicId;
            this.baseSystemId = baseSystemId;
            this.literalSystemId = literalSystemId;
            this.expandedSystemId = expandedSystemId;
        } // <init>(Reader,String,String,String,String)

    } // class CurrentEntity

    /**
     * The primary HTML document scanner.
     *
     * @author Andy Clark
     */
    public class ContentScanner 
        implements Scanner {

        //
        // Data
        //

        // temp vars

        /** A qualified name. */
        private final QName fQName = new QName();

        /** Attributes. */
        private final XMLAttributesImpl fAttributes = new XMLAttributesImpl();

        //
        // Scanner methods
        //

        /** Scan. */
        public boolean scan(boolean complete) throws IOException {
            boolean next;
            do {
                try {
                    next = false;
                    switch (fScannerState) {
                        case STATE_CONTENT: {
                            fBeginLineNumber = fCurrentEntity.lineNumber;
                            fBeginColumnNumber = fCurrentEntity.columnNumber;
                            int c = read();
                            if (c == '<') {
                                setScannerState(STATE_MARKUP_BRACKET);
                                next = true;
                            }
                            else if (c == '&') {
                                scanEntityRef(fStringBuffer, true);
                            }
                            else if (c == -1) {
                                throw new EOFException();
                            }
                            else {
                                fCurrentEntity.offset--;
                                fCurrentEntity.columnNumber--;
                                scanCharacters();
                            }
                            break;
                        }
                        case STATE_MARKUP_BRACKET: {
                            int c = read();
                            if (c == '!') {
                                if (skip("--", false)) {
                                    scanComment();
                                }
                                else if (skip("[CDATA[", false)) {
                                    scanCDATA();
                                }
                                else if (skip("DOCTYPE", false)) {
                                    scanDoctype();
                                }
                                else {
                                    if (fReportErrors) {
                                        fErrorReporter.reportError("HTML1002", null);
                                    }
                                    skipMarkup(true);
                                }
                            }
                            else if (c == '?') {
                                scanPI();
                            }
                            else if (c == '/') {
                                scanEndElement();
                            }
                            else if (c == -1) {
                                if (fReportErrors) {
                                    fErrorReporter.reportError("HTML1003", null);
                                }
                                if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                                    fStringBuffer.clear();
                                    fStringBuffer.append('<');
                                    fDocumentHandler.characters(fStringBuffer, null);
                                }
                                throw new EOFException();
                            }
                            else {
                                fCurrentEntity.offset--;
                                fCurrentEntity.columnNumber--;
                                fElementCount++;
                                fSingleBoolean[0] = false;
                                String ename = scanStartElement(fSingleBoolean);
                                if (ename != null && !fSingleBoolean[0] &&
                                    HTMLElements.getElement(ename).isSpecial()) {
                                    setScanner(fSpecialScanner.setElementName(ename));
                                    setScannerState(STATE_CONTENT);
                                    return true;
                                }
                            }
                            setScannerState(STATE_CONTENT);
                            break;
                        }
                        case STATE_START_DOCUMENT: {
                            if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                                if (DEBUG_CALLBACKS) {
                                    System.out.println("startDocument()");
                                }
                                XMLLocator locator = HTMLScanner.this;
                                String encoding = fIANAEncoding;
                                Augmentations augs = locationAugs();
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
                                    NamespaceContext nscontext = new NamespaceSupport();
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
                                    catch (IllegalAccessException ex) {
                                        // NOTE: Should never reach here!
                                        throw new XNIException(ex);
                                    } 
                                    catch (InvocationTargetException ex) {
                                        // NOTE: Should never reach here!
                                        throw new XNIException(ex);
                                    } 
                                    catch (NoSuchMethodException ex) {
                                        // NOTE: Should never reach here!
                                        throw new XNIException(ex);
                                    }
                                }
                            }
                            if (fInsertDoctype && fDocumentHandler != null) {
                                String root = HTMLElements.getElement(HTMLElements.HTML).name;
                                root = modifyName(root, fNamesElems);
                                String pubid = fDoctypePubid;
                                String sysid = fDoctypeSysid;
                                fDocumentHandler.doctypeDecl(root, pubid, sysid,
                                                             synthesizedAugs());
                            }
                            setScannerState(STATE_CONTENT);
                            break;
                        }
                        case STATE_END_DOCUMENT: {
                            if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                                if (DEBUG_CALLBACKS) {
                                    System.out.println("endDocument()");
                                }
                                fEndLineNumber = fCurrentEntity.lineNumber;
                                fEndColumnNumber = fCurrentEntity.columnNumber;
                                fDocumentHandler.endDocument(locationAugs());
                            }
                            return false;
                        }
                        default: {
                            throw new RuntimeException("unknown scanner state: "+fScannerState);
                        }
                    }
                }
                catch (EOFException e) {
                    if (fCurrentEntityStack.empty()) {
                        setScannerState(STATE_END_DOCUMENT);
                    }
                    else {
                        fCurrentEntity = (CurrentEntity)fCurrentEntityStack.pop();
                    }
                    next = true;
                }
            } while (next || complete);
            return true;
        } // scan(boolean):boolean

        //
        // Protected methods
        //

        /** Scans characters. */
        protected void scanCharacters() throws IOException {
            if (DEBUG_BUFFER) {
                System.out.print("(scanCharacters: ");
                printBuffer();
                System.out.println();
            }
            int newlines = skipNewlines();
            if (newlines == 0 && fCurrentEntity.offset == fCurrentEntity.length) {
                if (DEBUG_BUFFER) {
                    System.out.print(")scanCharacters: ");
                    printBuffer();
                    System.out.println();
                }
                return;
            }
            char c;
            int offset = fCurrentEntity.offset - newlines;
            for (int i = offset; i < fCurrentEntity.offset; i++) {
                fCurrentEntity.buffer[i] = '\n';
            }
            while (fCurrentEntity.offset < fCurrentEntity.length) {
                c = fCurrentEntity.buffer[fCurrentEntity.offset];
                if (c == '<' || c == '&' || c == '\n' || c == '\r') {
                    break;
                }
                fCurrentEntity.offset++;
                fCurrentEntity.columnNumber++;
            }
            if (fCurrentEntity.offset > offset && 
                fDocumentHandler != null && fElementCount >= fElementDepth) {
                fString.setValues(fCurrentEntity.buffer, offset, fCurrentEntity.offset - offset);
                if (DEBUG_CALLBACKS) {
                    System.out.println("characters("+fString+")");
                }
                fEndLineNumber = fCurrentEntity.lineNumber;
                fEndColumnNumber = fCurrentEntity.columnNumber;
                fDocumentHandler.characters(fString, locationAugs());
            }
            if (DEBUG_BUFFER) {
                System.out.print(")scanCharacters: ");
                printBuffer();
                System.out.println();
            }
        } // scanCharacters()

        /** Scans a CDATA section. */
        protected void scanCDATA() throws IOException {
            if (DEBUG_BUFFER) {
                System.out.print("(scanCDATA: ");
                printBuffer();
                System.out.println();
            }
            fStringBuffer.clear();
            if (fCDATASections) {
                if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                    fEndLineNumber = fCurrentEntity.lineNumber;
                    fEndColumnNumber = fCurrentEntity.columnNumber;
                    if (DEBUG_CALLBACKS) {
                        System.out.println("startCDATA()");
                    }
                    fDocumentHandler.startCDATA(locationAugs());
                }
            }
            else {
                fStringBuffer.append("[CDATA[");
            }
            boolean eof = scanMarkupContent(fStringBuffer, ']');
            if (!fCDATASections) {
                fStringBuffer.append("]]");
            }
            if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                fEndLineNumber = fCurrentEntity.lineNumber;
                fEndColumnNumber = fCurrentEntity.columnNumber;
                if (fCDATASections) {
                    if (DEBUG_CALLBACKS) {
                        System.out.println("characters("+fStringBuffer+")");
                    }
                    fDocumentHandler.characters(fStringBuffer, locationAugs());
                    if (DEBUG_CALLBACKS) {
                        System.out.println("endCDATA()");
                    }
                    fDocumentHandler.endCDATA(locationAugs());
                }
                else {
                    if (DEBUG_CALLBACKS) {
                        System.out.println("comment("+fStringBuffer+")");
                    }
                    fDocumentHandler.comment(fStringBuffer, locationAugs());
                }
            }
            if (DEBUG_BUFFER) {
                System.out.print(")scanCDATA: ");
                printBuffer();
                System.out.println();
            }
            if (eof) {
                throw new EOFException();
            }
        } // scanCDATA()
        
        /** Scans a comment. */
        protected void scanComment() throws IOException {
            if (DEBUG_BUFFER) {
                System.out.print("(scanComment: ");
                printBuffer();
                System.out.println();
            }
            fStringBuffer.clear();
            boolean eof = scanMarkupContent(fStringBuffer, '-');
            if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                if (DEBUG_CALLBACKS) {
                    System.out.println("comment("+fStringBuffer+")");
                }
                fEndLineNumber = fCurrentEntity.lineNumber;
                fEndColumnNumber = fCurrentEntity.columnNumber;
                fDocumentHandler.comment(fStringBuffer, locationAugs());
            }
            if (DEBUG_BUFFER) {
                System.out.print(")scanComment: ");
                printBuffer();
                System.out.println();
            }
            if (eof) {
                throw new EOFException();
            }
        } // scanComment()

        /** Scans markup content. */
        protected boolean scanMarkupContent(XMLStringBuffer buffer, 
                                            char cend) throws IOException {
            int c = -1;
            OUTER: while (true) {
                c = read();
                if (c == cend) {
                    int count = 1;
                    while (true) {
                        c = read();
                        if (c == cend) {
                            count++;
                            continue;
                        }
                        break;
                    }
                    if (c == -1) {
                        if (fReportErrors) {
                            fErrorReporter.reportError("HTML1007", null);
                        }
                        break OUTER;
                    }
                    if (count < 2) {
                        buffer.append(cend);
                        //if (c != -1) {
                        fCurrentEntity.offset--;
                        fCurrentEntity.columnNumber--;
                        //}
                        continue;
                    }
                    if (c != '>') {
                        for (int i = 0; i < count; i++) {
                            buffer.append(cend);
                        }
                        fCurrentEntity.offset--;
                        fCurrentEntity.columnNumber--;
                        continue;
                    }
                    for (int i = 0; i < count - 2; i++) {
                        buffer.append(cend);
                    }
                    break;
                }
                else if (c == '\n' || c == '\r') {
                    fCurrentEntity.offset--;
                    fCurrentEntity.columnNumber--;
                    int newlines = skipNewlines();
                    for (int i = 0; i < newlines; i++) {
                        buffer.append('\n');
                    }
                    continue;
                }
                else if (c == -1) {
                    if (fReportErrors) {
                        fErrorReporter.reportError("HTML1007", null);
                    }
                    break;
                }
                buffer.append((char)c);
            }
            return c == -1;
        } // scanMarkupContent(XMLStringBuffer,char):boolean

        /** Scans a processing instruction. */
        protected void scanPI() throws IOException {
            if (DEBUG_BUFFER) {
                System.out.print("(scanPI: ");
                printBuffer();
                System.out.println();
            }
            if (fReportErrors) {
                fErrorReporter.reportWarning("HTML1008", null);
            }

            // scan processing instruction
            String target = scanName();
            if (target != null && !target.equalsIgnoreCase("xml")) {
                while (true) {
                    int c = read();
                    if (c == '\r' || c == '\n') {
                        fCurrentEntity.lineNumber++;
                        fCurrentEntity.columnNumber = 1;
                        if (c == '\r') {
                            c = read();
                            if (c != '\n') {
                                fCurrentEntity.offset--;
                            }
                        }
                        continue;
                    }
                    if (c == -1) {
                        break;
                    }
                    if (c != ' ' && c != '\t') {
                        fCurrentEntity.offset--;
                        fCurrentEntity.columnNumber--;
                        break;
                    }
                }
                fStringBuffer.clear();
                while (true) {
                    int c = read();
                    if (c == '?' || c == '/') {
                        char c0 = (char)c;
                        c = read();
                        if (c == '>') {
                            break;
                        }
                        else {
                            fStringBuffer.append(c0);
                            fCurrentEntity.offset--;
                            fCurrentEntity.columnNumber--;
                            continue;
                        }
                    }
                    else if (c == '\r' || c == '\n') {
                        fStringBuffer.append('\n');
                        fCurrentEntity.lineNumber++;
                        fCurrentEntity.columnNumber = 1;
                        if (c == '\r') {
                            c = read();
                            if (c != '\n') {
                                fCurrentEntity.offset--;
                            }
                        }
                        continue;
                    }
                    else if (c == -1) {
                        break;
                    }
                    else {
                        fStringBuffer.append((char)c);
                    }
                }
                XMLString data = fStringBuffer;
                if (fDocumentHandler != null) {
                    fEndLineNumber = fCurrentEntity.lineNumber;
                    fEndColumnNumber = fCurrentEntity.columnNumber;
                    fDocumentHandler.processingInstruction(target, data, locationAugs());
                }
            }

            // scan xml/text declaration
            else {
                int beginLineNumber = fBeginLineNumber;
                int beginColumnNumber = fBeginColumnNumber;
                fAttributes.removeAllAttributes();
                int aindex = 0;
                while (scanPseudoAttribute(fAttributes)) {
                    fAttributes.getName(aindex,fQName);
                    fQName.rawname = fQName.rawname.toLowerCase();
                    fAttributes.setName(aindex,fQName);
                    aindex++;
                }
                if (fDocumentHandler != null) {
                    String version = fAttributes.getValue("version");
                    String encoding = fAttributes.getValue("encoding");
                    String standalone = fAttributes.getValue("standalone");

                    fBeginLineNumber = beginLineNumber;
                    fBeginColumnNumber = beginColumnNumber;
                    fEndLineNumber = fCurrentEntity.lineNumber;
                    fEndColumnNumber = fCurrentEntity.columnNumber;
                    fDocumentHandler.xmlDecl(version, encoding, standalone,
                                             locationAugs());
                }
            }

            if (DEBUG_BUFFER) {
                System.out.print(")scanPI: ");
                printBuffer();
                System.out.println();
            }
        } // scanPI()

        /** 
         * Scans a start element. 
         *
         * @param empty Is used for a second return value to indicate whether
         *              the start element tag is empty (e.g. "/&gt;").
         */
        protected String scanStartElement(boolean[] empty) throws IOException {
            String ename = scanName();
            int length = ename != null ? ename.length() : 0;
            int c = length > 0 ? ename.charAt(0) : -1;
            if (length == 0 || !((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
                if (fReportErrors) {
                    fErrorReporter.reportError("HTML1009", null);
                }
                if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                    fStringBuffer.clear();
                    fStringBuffer.append('<');
                    if (length > 0) {
                        fStringBuffer.append(ename);
                    }
                    fDocumentHandler.characters(fStringBuffer, null);
                }
                return null;
            }
            ename = modifyName(ename, fNamesElems);
            fAttributes.removeAllAttributes();
            boolean print = false;
            int beginLineNumber = fBeginLineNumber;
            int beginColumnNumber = fBeginColumnNumber;
            while (scanAttribute(fAttributes, empty)) {
                // do nothing
            }
            fBeginLineNumber = beginLineNumber;
            fBeginColumnNumber = beginColumnNumber;
            if (fByteStream != null && fElementDepth == -1) {
                if (ename.equalsIgnoreCase("META")) {
                    if (DEBUG_CHARSET) {
                        System.out.println("+++ <META>");
                    }
                    String httpEquiv = getValue(fAttributes, "http-equiv");
                    if (httpEquiv != null && httpEquiv.equalsIgnoreCase("content-type")) {
                        if (DEBUG_CHARSET) {
                            System.out.println("+++ @content-type: \""+httpEquiv+'"');
                        }
                        String content = getValue(fAttributes, "content");
                        int index1 = content != null ? content.toLowerCase().indexOf("charset=") : -1;
                        if (index1 != -1 && !fIgnoreSpecifiedCharset) {
                            int index2 = content.indexOf(';', index1);
                            String charset = index2 != -1 ? content.substring(index1+8, index2) : content.substring(index1+8);
                            try {
                                String ianaEncoding = charset;
                                String javaEncoding = EncodingMap.getIANA2JavaMapping(ianaEncoding.toUpperCase());
                                if (DEBUG_CHARSET) {
                                    System.out.println("+++ ianaEncoding: "+ianaEncoding);
                                    System.out.println("+++ javaEncoding: "+javaEncoding);
                                }
                                if (javaEncoding == null) {
                                    javaEncoding = ianaEncoding;
                                    if (fReportErrors) {
                                        fErrorReporter.reportError("HTML1001", new Object[]{ianaEncoding});
                                    }
                                }
                                // patch: Marc Guillemot
                                if (!javaEncoding.equals(fJavaEncoding)) { 
                                  	if (!isCompatible(javaEncoding, fJavaEncoding)) {
                                        if (fReportErrors) {
                                            fErrorReporter.reportError("HTML1015", new Object[]{javaEncoding,fJavaEncoding});
                                        }
                                 	}
                              		// change the charset
                                 	else {
                                         fIso8859Encoding = ianaEncoding == null 
 		                                        || ianaEncoding.toUpperCase().startsWith("ISO-8859")
 		                                        || ianaEncoding.equalsIgnoreCase(fDefaultIANAEncoding);
 				                        fCurrentEntity.stream = new InputStreamReader(fByteStream, javaEncoding);
 				                        fByteStream.playback();
 				                        fElementDepth = fElementCount;
 				                        fElementCount = 0;
 				                        fCurrentEntity.offset = fCurrentEntity.length = 0;
 				                        fCurrentEntity.lineNumber = 1;
 				                        fCurrentEntity.columnNumber = 1;
                                 	}
                                 }
                            }
                            catch (UnsupportedEncodingException e) {
                                if (fReportErrors) {
                                    fErrorReporter.reportError("HTML1010", new Object[]{charset});
                                }
                                // NOTE: If the encoding change doesn't work, 
                                //       then there's no point in continuing to 
                                //       buffer the input stream.
                                fByteStream.clear();
                                fByteStream = null;
                            }
                        }
                    }
                }
                else if (ename.equalsIgnoreCase("BODY")) {
                    fByteStream.clear();
                    fByteStream = null;
                }
                else {
                     HTMLElements.Element element = HTMLElements.getElement(ename);
                     if (element.parent != null && element.parent.length > 0) {
                         if (element.parent[0].code == HTMLElements.BODY) {
                             fByteStream.clear();
                             fByteStream = null;
                         }
                     }
                }
            }
            if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                fQName.setValues(null, ename, ename, null);
                if (DEBUG_CALLBACKS) {
                    System.out.println("startElement("+fQName+','+fAttributes+")");
                }
                fEndLineNumber = fCurrentEntity.lineNumber;
                fEndColumnNumber = fCurrentEntity.columnNumber;
                if (empty[0]) {
                    fDocumentHandler.emptyElement(fQName, fAttributes, locationAugs());
                }
                else {
                    fDocumentHandler.startElement(fQName, fAttributes, locationAugs());
                }
            }
            return ename;
        } // scanStartElement():ename

        /** 
         * Scans a real attribute. 
         *
         * @param attributes The list of attributes.
         * @param empty      Is used for a second return value to indicate 
         *                   whether the start element tag is empty 
         *                   (e.g. "/&gt;").
         */
        protected boolean scanAttribute(XMLAttributesImpl attributes,
                                        boolean[] empty)
            throws IOException {
            return scanAttribute(attributes,empty,'/');
        } // scanAttribute(XMLAttributesImpl,boolean[]):boolean

        /** 
         * Scans a pseudo attribute. 
         *
         * @param attributes The list of attributes.
         */
        protected boolean scanPseudoAttribute(XMLAttributesImpl attributes)
            throws IOException {
            return scanAttribute(attributes,fSingleBoolean,'?');
        } // scanPseudoAttribute(XMLAttributesImpl):boolean

        /** 
         * Scans an attribute, pseudo or real. 
         *
         * @param attributes The list of attributes.
         * @param empty      Is used for a second return value to indicate 
         *                   whether the start element tag is empty 
         *                   (e.g. "/&gt;").
         * @param endc       The end character that appears before the
         *                   closing angle bracket ('>').
         */
        protected boolean scanAttribute(XMLAttributesImpl attributes,
                                        boolean[] empty, char endc)
            throws IOException {
            boolean skippedSpaces = skipSpaces();
            fBeginLineNumber = fCurrentEntity.lineNumber;
            fBeginColumnNumber = fCurrentEntity.columnNumber;
            int c = read();
            if (c == -1) {
                if (fReportErrors) {
                    fErrorReporter.reportError("HTML1007", null);
                }
                throw new EOFException();
            }
            if (c == '>') {
                return false;
            }
            fCurrentEntity.offset--;
            fCurrentEntity.columnNumber--;
            String aname = scanName();
            if (aname == null) {
                if (fReportErrors) {
                    fErrorReporter.reportError("HTML1011", null);
                }
                empty[0] = skipMarkup(false);
                return false;
            }
            if (!skippedSpaces && fReportErrors) {
                fErrorReporter.reportError("HTML1013", new Object[] { aname });
            }
            aname = modifyName(aname, fNamesAttrs);
            skipSpaces();
            c = read();
            if (c == -1) {
                if (fReportErrors) {
                    fErrorReporter.reportError("HTML1007", null);
                }
                throw new EOFException();
            }
            if (c == '/' || c == '>') {
                fQName.setValues(null, aname, aname, null);
                attributes.addAttribute(fQName, "CDATA", "");
                attributes.setSpecified(attributes.getLength()-1, true);
                if (fAugmentations) {
                    addLocationItem(attributes, attributes.getLength() - 1);
                }
                if (c == '/') {
                    fCurrentEntity.offset--;
                    fCurrentEntity.columnNumber--;
                    empty[0] = skipMarkup(false);
                }
                return false;
            }
            /***
            // REVISIT: [Q] Why is this still here? -Ac
            if (c == '/' || c == '>') {
                if (c == '/') {
                    fCurrentEntity.offset--;
                    fCurrentEntity.columnNumber--;
                    empty[0] = skipMarkup(false);
                }
                fQName.setValues(null, aname, aname, null);
                attributes.addAttribute(fQName, "CDATA", "");
                attributes.setSpecified(attributes.getLength()-1, true);
                if (fAugmentations) {
                    addLocationItem(attributes, attributes.getLength() - 1);
                }
                return false;
            }
            /***/
            if (c == '=') {
                skipSpaces();
                c = read();
                if (c == -1) {
                    if (fReportErrors) {
                        fErrorReporter.reportError("HTML1007", null);
                    }
                    throw new EOFException();
                }
                // Xiaowei/Ac: Fix for <a href=/cgi-bin/myscript>...</a>
                if (c == '>') {
                    fQName.setValues(null, aname, aname, null);
                    attributes.addAttribute(fQName, "CDATA", "");
                    attributes.setSpecified(attributes.getLength()-1, true);
                    if (fAugmentations) {
                        addLocationItem(attributes, attributes.getLength() - 1);
                    }
                    return false;
                }
                fStringBuffer.clear();
                fNonNormAttr.clear();
                if (c != '\'' && c != '"') {
                    fCurrentEntity.offset--;
                    fCurrentEntity.columnNumber--;
                    while (true) {
                        c = read();
                        // Xiaowei/Ac: Fix for <a href=/broken/>...</a>
                        if (Character.isSpace((char)c) || c == '>') {
                            //fCharOffset--;
                            fCurrentEntity.offset--;
                            fCurrentEntity.columnNumber--;
                            break;
                        }
                        if (c == -1) {
                            if (fReportErrors) {
                                fErrorReporter.reportError("HTML1007", null);
                            }
                            throw new EOFException();
                        }
                        if (c == '&') {
                            int ce = scanEntityRef(fStringBuffer2, false);
                            if (ce != -1) {
                                fStringBuffer.append((char)ce);
                            }
                            else {
                                fStringBuffer.append(fStringBuffer2);
                            }
                            fNonNormAttr.append(fStringBuffer2);
                        }
                        else {
                            fStringBuffer.append((char)c);
                            fNonNormAttr.append((char)c);
                        }
                    }
                    fQName.setValues(null, aname, aname, null);
                    String avalue = fStringBuffer.toString();
                    attributes.addAttribute(fQName, "CDATA", avalue);

                    int lastattr = attributes.getLength()-1;
                    attributes.setSpecified(lastattr, true);
                    attributes.setNonNormalizedValue(lastattr, fNonNormAttr.toString());
                    if (fAugmentations) {
                        addLocationItem(attributes, attributes.getLength() - 1);
                    }
                    return true;
                }
                char quote = (char)c;
                do {
                    c = read();
                    if (c == -1) {
                        if (fReportErrors) {
                            fErrorReporter.reportError("HTML1007", null);
                        }
                        throw new EOFException();
                    }
                    if (c == '&') {
                        int ce = scanEntityRef(fStringBuffer2, false);
                        if (ce != -1) {
                            fStringBuffer.append((char)ce);
                        }
                        else {
                            fStringBuffer.append(fStringBuffer2);
                        }
                        fNonNormAttr.append(fStringBuffer2);
                    }
                    else if (c == '\t') {
                        fStringBuffer.append(' ');
                        fNonNormAttr.append('\t');
                    }
                    else if (c == '\r' || c == '\n') {
                        fCurrentEntity.lineNumber++;
                        fCurrentEntity.columnNumber = 0;
                        if (c == '\r') {
                            int c2 = read();
                            if (c2 != '\n') {
                                fCurrentEntity.offset--;
                                fCurrentEntity.columnNumber--;
                            }
                            else {
                                fNonNormAttr.append('\r');
                                c = c2;
                            }
                        }
                        fStringBuffer.append(' ');
                        fNonNormAttr.append((char)c);
                    }
                    else if (c != quote) {
                        fStringBuffer.append((char)c);
                        fNonNormAttr.append((char)c);
                    }
                } while (c != quote);

                fQName.setValues(null, aname, aname, null);
                final String attrNormalizedValue = fStringBuffer.toString();
                final String attrNonNormalizedValue = fNonNormAttr.toString();
                final String avalue = fNormalizeAttributes ? attrNormalizedValue : attrNonNormalizedValue;
                attributes.addAttribute(fQName, "CDATA", avalue);

                int lastattr = attributes.getLength()-1;
                attributes.setSpecified(lastattr, true);
                attributes.setNonNormalizedValue(lastattr, attrNonNormalizedValue);
                if (fAugmentations) {
                    addLocationItem(attributes, attributes.getLength() - 1);
                }
            }
            else {
                fQName.setValues(null, aname, aname, null);
                attributes.addAttribute(fQName, "CDATA", "");
                attributes.setSpecified(attributes.getLength()-1, true);
                fCurrentEntity.offset--;
                fCurrentEntity.columnNumber--;
                if (fAugmentations) {
                    addLocationItem(attributes, attributes.getLength() - 1);
                }
            }
            return true;
        } // scanAttribute(XMLAttributesImpl):boolean

        /** Adds location augmentations to the specified attribute. */
        protected void addLocationItem(XMLAttributes attributes, int index) {
            fEndLineNumber = fCurrentEntity.lineNumber;
            fEndColumnNumber = fCurrentEntity.columnNumber;
            LocationItem locationItem = new LocationItem();
            locationItem.setValues(fBeginLineNumber, fBeginColumnNumber,
                                   fEndLineNumber, fEndColumnNumber);
            Augmentations augs = attributes.getAugmentations(index);
            augs.putItem(AUGMENTATIONS, locationItem);
        } // addLocationItem(XMLAttributes,int)

        /** Scans an end element. */
        protected void scanEndElement() throws IOException {
            String ename = scanName();
            if (fReportErrors && ename == null) {
                fErrorReporter.reportError("HTML1012", null);
            }
            skipMarkup(false);
            if (ename != null) {
                ename = modifyName(ename, fNamesElems);
                if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                    fQName.setValues(null, ename, ename, null);
                    if (DEBUG_CALLBACKS) {
                        System.out.println("endElement("+fQName+")");
                    }
                    fEndLineNumber = fCurrentEntity.lineNumber;
                    fEndColumnNumber = fCurrentEntity.columnNumber;
                    fDocumentHandler.endElement(fQName, locationAugs());
                }
            }
        } // scanEndElement()
        
        //
        // Private methods
        //
        
        private boolean isCompatible(String encoding1, String encoding2) {
        	if (CHARSET_forName == null || DECODER_averageCharsPerByte == null) {
        		return false;
        	}
        
        	try {
        		Object charset1 = CHARSET_forName.invoke(null, new Object[]{encoding1});
        		Object charset2 = CHARSET_forName.invoke(null, new Object[]{encoding2});
        		
        		Float average1 = (Float)DECODER_averageCharsPerByte.invoke(charset1, (Object[])null);
        		Float average2 = (Float)DECODER_averageCharsPerByte.invoke(charset2, (Object[])null);
        		
           		// If the average number of bytes for the old and new
           		// charset don't match, there's very little chance that
           		// we'll be able to properly parse the document with the
           		// new encoding. So assume the specified charset is wrong
           		// and do nothing.
                return Math.abs(average1.floatValue() - average2.floatValue()) < 0.1;
        	}
        	catch (Exception e) {
        		return false;
        	}
        }

    } // class ContentScanner

    /**
     * Special scanner used for elements whose content needs to be scanned 
     * as plain text, ignoring markup such as elements and entity references.
     * For example: &lt;SCRIPT&gt; and &lt;COMMENT&gt;.
     *
     * @author Andy Clark
     */
    public class SpecialScanner
        implements Scanner {

        //
        // Data
        //

        /** Name of element whose content needs to be scanned as text. */
        protected String fElementName;

        /** True if &lt;script&gt; element. */
        protected boolean fScript;

        /** True if &lt;style&gt; element. */
        protected boolean fStyle;

        /** True if &lt;textarea&gt; element. */
        protected boolean fTextarea;

        // temp vars

        /** A qualified name. */
        private final QName fQName = new QName();

        /** A string buffer. */
        private final XMLStringBuffer fStringBuffer = new XMLStringBuffer();

        //
        // Public methods
        //

        /** Sets the element name. */
        public Scanner setElementName(String ename) {
            fElementName = ename;
            fScript = fElementName.equalsIgnoreCase("SCRIPT");
            fStyle = fElementName.equalsIgnoreCase("STYLE");
            fTextarea = fElementName.equalsIgnoreCase("TEXTAREA");
            return this;
        } // setElementName(String):Scanner

        //
        // Scanner methods
        //

        /** Scan. */
        public boolean scan(boolean complete) throws IOException {
            boolean next;
            do {
                try {
                    next = false;
                    int delimiter = -1;
                    switch (fScannerState) {
                        case STATE_CONTENT: {
                            fBeginLineNumber = fCurrentEntity.lineNumber;
                            fBeginColumnNumber = fCurrentEntity.columnNumber;
                            int c = read();
                            if (c == '<') {
                                setScannerState(STATE_MARKUP_BRACKET);
                                continue;
                            }
                            if (c == '&') {
                                if (fTextarea) {
                                    scanEntityRef(fStringBuffer, true);
                                    continue;
                                }
                                fStringBuffer.clear();
                                fStringBuffer.append('&');
                            }
                            else if (c == -1) {
                                if (fReportErrors) {
                                    fErrorReporter.reportError("HTML1007", null);
                                }
                                throw new EOFException();
                            }
                            else {
                                fCurrentEntity.offset--;
                                fCurrentEntity.columnNumber--;
                                fStringBuffer.clear();
                            }
                            scanCharacters(fStringBuffer, -1);
                            break;
                        } // case STATE_CONTENT
                        case STATE_MARKUP_BRACKET: {
                            int c = read();
                            if (c == '!') {
                                if (skip("--", false)) {
                                    fStringBuffer.clear();
                                    boolean strip = (fScript && fScriptStripCommentDelims) ||
                                                    (fStyle && fStyleStripCommentDelims);
                                    if (strip) {
                                        do {
                                            c = read();
                                            if (c == '\r' || c == '\n') {
                                                fCurrentEntity.columnNumber--;
                                                fCurrentEntity.offset--;
                                                break;
                                            }
                                        } while (c != -1);
                                        skipNewlines(1);
                                        delimiter = '-';
                                    }
                                    else {
                                        fStringBuffer.append("<!--");
                                    }
                                }
                                else if (skip("[CDATA[", false)) {
                                    fStringBuffer.clear();
                                    boolean strip = (fScript && fScriptStripCDATADelims) ||
                                                    (fStyle  && fStyleStripCDATADelims);
                                    if (strip) {
                                        do {
                                            c = read();
                                            if (c == '\r' || c == '\n') {
                                                fCurrentEntity.columnNumber--;
                                                fCurrentEntity.offset--;
                                                break;
                                            }
                                        } while (c != -1);
                                        skipNewlines(1);
                                        delimiter = ']';
                                    }
                                    else {
                                        fStringBuffer.append("<![CDATA[");
                                    }
                                }
                            }
                            else if (c == '/') {
                                String ename = scanName();
                                if (ename != null) {
                                    if (ename.equalsIgnoreCase(fElementName)) {
                                        if (read() == '>') {
                                            ename = modifyName(ename, fNamesElems);
                                            if (fDocumentHandler != null && fElementCount >= fElementDepth) {
                                                fQName.setValues(null, ename, ename, null);
                                                if (DEBUG_CALLBACKS) {
                                                    System.out.println("endElement("+fQName+")");
                                                }
                                                fEndLineNumber = fCurrentEntity.lineNumber;
                                                fEndColumnNumber = fCurrentEntity.columnNumber;
                                                fDocumentHandler.endElement(fQName, locationAugs());
                                            }
                                            setScanner(fContentScanner);
                                            setScannerState(STATE_CONTENT);
                                            return true;
                                        }
                                        else {
                                            fCurrentEntity.offset--;
                                            fCurrentEntity.columnNumber--;
                                        }
                                    }
                                    fStringBuffer.clear();
                                    fStringBuffer.append("</");
                                    fStringBuffer.append(ename);
                                }
                                else {
                                    fStringBuffer.clear();
                                    fStringBuffer.append("</");
                                }
                            }
                            else {
                                fStringBuffer.clear();
                                fStringBuffer.append('<');
                                fStringBuffer.append((char)c);
                            }
                            scanCharacters(fStringBuffer, delimiter);
                            setScannerState(STATE_CONTENT);
                            break;
                        } // case STATE_MARKUP_BRACKET
                    } // switch
                } // try
                catch (EOFException e) {
                    setScanner(fContentScanner);
                    if (fCurrentEntityStack.empty()) {
                        setScannerState(STATE_END_DOCUMENT);
                    }
                    else {
                        fCurrentEntity = (CurrentEntity)fCurrentEntityStack.pop();
                        setScannerState(STATE_CONTENT);
                    }
                    return true;
                }
            } // do
            while (next || complete);
            return true;
        } // scan(boolean):boolean

        //
        // Protected methods
        //

        /** Scan characters. */
        protected void scanCharacters(XMLStringBuffer buffer,
                                      int delimiter) throws IOException {
            if (DEBUG_BUFFER) {
                System.out.print("(scanCharacters, delimiter="+delimiter+": ");
                printBuffer();
                System.out.println();
            }
            boolean strip = (fScript && fScriptStripCommentDelims) ||
                            (fScript && fScriptStripCDATADelims) ||
                            (fStyle  && fStyleStripCommentDelims) ||
                            (fStyle  && fStyleStripCDATADelims);
            while (true) {
                int c = read();
                if (c == -1 || (delimiter == -1 && (c == '<' || c == '&'))) {
                    if (c != -1) {
                        fCurrentEntity.offset--;
                        fCurrentEntity.columnNumber--;
                    }
                    break;
                }
                // Patch supplied by Jonathan Baxter
                else if (c == '\r' || c == '\n') {
                    fCurrentEntity.offset--;
                    fCurrentEntity.columnNumber--;
                    int newlines = skipNewlines();
                    for (int i = 0; i < newlines; i++) {
                        buffer.append('\n');
                    }
                }
                else if (delimiter != -1 && c == (char)delimiter) {
                    int count = 0;
                    do {
                        count++;
                        c = read();
                    } while (c == (char)delimiter);
                    for (int i = strip && c == '>' ? 2 : 0; i < count; i++) {
                        buffer.append((char)delimiter);
                    }
                    if (c == -1 || (count >= 2 && c == '>')) {
                        if (!strip) {
                            buffer.append((char)c);
                        }
                        break;
                    }
                    fCurrentEntity.offset--;
                    fCurrentEntity.columnNumber--;
                }
                else {
                    buffer.append((char)c);
                    if (c == '\n') {
                        fCurrentEntity.columnNumber = 1;
                        fCurrentEntity.lineNumber++;
                    }
                }
            }
            if (buffer.length > 0 && fDocumentHandler != null && fElementCount >= fElementDepth) {
                if (DEBUG_CALLBACKS) {
                    System.out.println("characters("+buffer+")");
                }
                fEndLineNumber = fCurrentEntity.lineNumber;
                fEndColumnNumber = fCurrentEntity.columnNumber;
                fDocumentHandler.characters(buffer, locationAugs());
            }
            if (DEBUG_BUFFER) {
                System.out.print(")scanCharacters: ");
                printBuffer();
                System.out.println();
            }
        } // scanCharacters(StringBuffer)

    } // class SpecialScanner

    /**
     * A playback input stream. This class has the ability to save the bytes
     * read from the underlying input stream and play the bytes back later.
     * This class is used by the HTML scanner to switch encodings when a 
     * &lt;meta&gt; tag is detected that specifies a different encoding. 
     * <p>
     * If the encoding is changed, then the scanner calls the 
     * <code>playback</code> method and re-scans the beginning of the HTML
     * document again. This should not be too much of a performance problem
     * because the &lt;meta&gt; tag appears at the beginning of the document.
     * <p>
     * If the &lt;body&gt; tag is reached without playing back the bytes,
     * then the buffer can be cleared by calling the <code>clear</code>
     * method. This stops the buffering of bytes and allows the memory used
     * by the buffer to be reclaimed. 
     * <p>
     * <strong>Note:</strong> 
     * If the buffer is never played back or cleared, this input stream
     * will continue to buffer the entire stream. Therefore, it is very
     * important to use this stream correctly.
     *
     * @author Andy Clark
     */
    public static class PlaybackInputStream
        extends FilterInputStream {

        //
        // Constants
        //

        /** Set to true to debug playback. */
        private static final boolean DEBUG_PLAYBACK = false;

        //
        // Data
        //

        // state

        /** Playback mode. */
        protected boolean fPlayback = false;

        /** Buffer cleared. */
        protected boolean fCleared = false;

        /** Encoding detected. */
        protected boolean fDetected = false;

        // buffer info

        /** Byte buffer. */
        protected byte[] fByteBuffer = new byte[1024];

        /** Offset into byte buffer during playback. */
        protected int fByteOffset = 0;

        /** Length of bytes read into byte buffer. */
        protected int fByteLength = 0;

        /** Pushback offset. */
        public int fPushbackOffset = 0;

        /** Pushback length. */
        public int fPushbackLength = 0;

        //
        // Constructors
        //

        /** Constructor. */
        public PlaybackInputStream(InputStream in) {
            super(in);
        } // <init>(InputStream)

        //
        // Public methods
        //

        /** Detect encoding. */
        public void detectEncoding(String[] encodings) throws IOException {
            if (fDetected) {
                throw new IOException("Should not detect encoding twice.");
            }
            fDetected = true;
            int b1 = read();
            if (b1 == -1) {
                return;
            }
            int b2 = read();
            if (b2 == -1) {
                fPushbackLength = 1;
                return;
            }
            // UTF-8 BOM: 0xEFBBBF
            if (b1 == 0xEF && b2 == 0xBB) {
                int b3 = read();
                if (b3 == 0xBF) {
                    fPushbackOffset = 3;
                    encodings[0] = "UTF-8";
                    encodings[1] = "UTF8";
                    return;
                }
                fPushbackLength = 3;
            }
            // UTF-16 LE BOM: 0xFFFE
            if (b1 == 0xFF && b2 == 0xFE) {
                encodings[0] = "UTF-16";
                encodings[1] = "UnicodeLittleUnmarked";
                return;
            }
            // UTF-16 BE BOM: 0xFEFF
            else if (b1 == 0xFE && b2 == 0xFF) {
                encodings[0] = "UTF-16";
                encodings[1] = "UnicodeBigUnmarked";
                return;
            }
            // unknown
            fPushbackLength = 2;
        } // detectEncoding()

        /** Playback buffer contents. */
        public void playback() {
            fPlayback = true;
        } // playback()

        /** 
         * Clears the buffer.
         * <p>
         * <strong>Note:</strong>
         * The buffer cannot be cleared during playback. Therefore, calling
         * this method during playback will not do anything. However, the
         * buffer will be cleared automatically at the end of playback.
         */
        public void clear() {
            if (!fPlayback) {
                fCleared = true;
                fByteBuffer = null;
            }
        } // clear()

        //
        // InputStream methods
        //

        /** Read a byte. */
        public int read() throws IOException {
            if (DEBUG_PLAYBACK) {
                System.out.println("(read");
            }
            if (fPushbackOffset < fPushbackLength) {
                return fByteBuffer[fPushbackOffset++];
            }
            if (fCleared) {
                return in.read();
            }
            if (fPlayback) {
                int c = fByteBuffer[fByteOffset++];
                if (fByteOffset == fByteLength) {
                    fCleared = true;
                    fByteBuffer = null;
                }
                if (DEBUG_PLAYBACK) {
                    System.out.println(")read -> "+(char)c);
                }
                return c;
            }
            int c = in.read();
            if (c != -1) {
                if (fByteLength == fByteBuffer.length) {
                    byte[] newarray = new byte[fByteLength + 1024];
                    System.arraycopy(fByteBuffer, 0, newarray, 0, fByteLength);
                    fByteBuffer = newarray;
                }
                fByteBuffer[fByteLength++] = (byte)c;
            }
            if (DEBUG_PLAYBACK) {
                System.out.println(")read -> "+(char)c);
            }
            return c;
        } // read():int

        /** Read an array of bytes. */
        public int read(byte[] array) throws IOException {
            return read(array, 0, array.length);
        } // read(byte[]):int

        /** Read an array of bytes. */
        public int read(byte[] array, int offset, int length) throws IOException {
            if (DEBUG_PLAYBACK) {
                System.out.println(")read("+offset+','+length+')');
            }
            if (fPushbackOffset < fPushbackLength) {
                int count = fPushbackLength - fPushbackOffset;
                if (count > length) {
                    count = length;
                }
                System.arraycopy(fByteBuffer, fPushbackOffset, array, offset, count);
                fPushbackOffset += count;
                return count;
            }
            if (fCleared) {
                return in.read(array, offset, length);
            }
            if (fPlayback) {
                if (fByteOffset + length > fByteLength) {
                    length = fByteLength - fByteOffset;
                }
                System.arraycopy(fByteBuffer, fByteOffset, array, offset, length);
                fByteOffset += length;
                if (fByteOffset == fByteLength) {
                    fCleared = true;
                    fByteBuffer = null;
                }
                return length;
            }
            int count = in.read(array, offset, length);
            if (count != -1) {
                if (fByteLength + count > fByteBuffer.length) {
                    byte[] newarray = new byte[fByteLength + count + 512];
                    System.arraycopy(fByteBuffer, 0, newarray, 0, fByteLength);
                    fByteBuffer = newarray;
                }
                System.arraycopy(array, offset, fByteBuffer, fByteLength, count);
                fByteLength += count;
            }
            if (DEBUG_PLAYBACK) {
                System.out.println(")read("+offset+','+length+") -> "+count);
            }
            return count;
        } // read(byte[]):int

    } // class PlaybackInputStream

    /**
     * Location infoset item. 
     *
     * @author Andy Clark
     */
    protected static class LocationItem 
        implements HTMLEventInfo {

        //
        // Data
        //

        /** Beginning line number. */
        protected int fBeginLineNumber;

        /** Beginning column number. */
        protected int fBeginColumnNumber;

        /** Ending line number. */
        protected int fEndLineNumber;

        /** Ending column number. */
        protected int fEndColumnNumber;

        //
        // Public methods
        //

        /** Sets the values of this item. */
        public void setValues(int beginLine, int beginColumn,
                              int endLine, int endColumn) {
            fBeginLineNumber = beginLine;
            fBeginColumnNumber = beginColumn;
            fEndLineNumber = endLine;
            fEndColumnNumber = endColumn;
        } // setValues(int,int,int,int)

        //
        // HTMLEventInfo methods
        //

        // location information

        /** Returns the line number of the beginning of this event.*/
        public int getBeginLineNumber() {
            return fBeginLineNumber;
        } // getBeginLineNumber():int

        /** Returns the column number of the beginning of this event.*/
        public int getBeginColumnNumber() { 
            return fBeginColumnNumber;
        } // getBeginColumnNumber():int

        /** Returns the line number of the end of this event.*/
        public int getEndLineNumber() {
            return fEndLineNumber;
        } // getEndLineNumber():int

        /** Returns the column number of the end of this event.*/
        public int getEndColumnNumber() {
            return fEndColumnNumber;
        } // getEndColumnNumber():int

        // other information

        /** Returns true if this corresponding event was synthesized. */
        public boolean isSynthesized() {
            return false;
        } // isSynthesize():boolean

        //
        // Object methods
        //

        /** Returns a string representation of this object. */
        public String toString() {
            StringBuffer str = new StringBuffer();
            str.append(fBeginLineNumber);
            str.append(':');
            str.append(fBeginColumnNumber);
            str.append(':');
            str.append(fEndLineNumber);
            str.append(':');
            str.append(fEndColumnNumber);
            return str.toString();
        } // toString():String

    } // class LocationItem

} // class HTMLScanner
