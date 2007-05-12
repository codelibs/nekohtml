/* 
 * (C) Copyright 2002, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

package org.cyberneko.html;

/**
 * Collection of HTML element information.
 *
 * @author Andy Clark
 *
 * @version $Id$
 */
public class HTMLElements {

    //
    // Constants
    //
    
    // element codes

    // NOTE: The element codes *must* start with 0 and increment in
    //       sequence. The parent and closes references depends on 
    //       this assumption. -Ac

    public static final short A = 0;
    public static final short ABBR = A+1;
    public static final short ACRONYM = ABBR+1;
    public static final short ADDRESS = ACRONYM+1;
    public static final short APPLET = ADDRESS+1;
    public static final short AREA = APPLET+1;
    public static final short B = AREA+1;
    public static final short BASE = B+1;
    public static final short BASEFONT = BASE+1;
    public static final short BDO = BASEFONT+1;
    public static final short BGSOUND = BDO+1;
    public static final short BIG = BGSOUND+1;
    public static final short BLINK = BIG+1;
    public static final short BLOCKQUOTE = BLINK+1;
    public static final short BODY = BLOCKQUOTE+1;
    public static final short BR = BODY+1;
    public static final short BUTTON = BR+1;
    public static final short CAPTION = BUTTON+1;
    public static final short CENTER = CAPTION+1;
    public static final short CITE = CENTER+1;
    public static final short CODE = CITE+1;
    public static final short COL = CODE+1;
    public static final short COLGROUP = COL+1;
    public static final short COMMENT = COLGROUP+1;
    public static final short DEL = COMMENT+1;
    public static final short DFN = DEL+1;
    public static final short DIR = DFN+1;
    public static final short DIV = DIR+1;
    public static final short DD = DIV+1;
    public static final short DL = DD+1;
    public static final short DT = DL+1;
    public static final short EM = DT+1;
    public static final short EMBED = EM+1;
    public static final short FIELDSET = EMBED+1;
    public static final short FONT = FIELDSET+1;
    public static final short FORM = FONT+1;
    public static final short FRAME = FORM+1;
    public static final short FRAMESET = FRAME+1;
    public static final short H1 = FRAMESET+1;
    public static final short H2 = H1+1;
    public static final short H3 = H2+1;
    public static final short H4 = H3+1;
    public static final short H5 = H4+1;
    public static final short H6 = H5+1;
    public static final short HEAD = H6+1;
    public static final short HR = HEAD+1;
    public static final short HTML = HR+1;
    public static final short I = HTML+1;
    public static final short IFRAME = I+1;
    public static final short ILAYER = IFRAME+1;
    public static final short IMG = ILAYER+1;
    public static final short INPUT = IMG+1;
    public static final short INS = INPUT+1;
    public static final short ISINDEX = INS+1;
    public static final short KBD = ISINDEX+1;
    public static final short KEYGEN = KBD+1;
    public static final short LABEL = KEYGEN+1;
    public static final short LAYER = LABEL+1;
    public static final short LEGEND = LAYER+1;
    public static final short LI = LEGEND+1;
    public static final short LINK = LI+1;
    public static final short LISTING = LINK+1;
    public static final short MAP = LISTING+1;
    public static final short MARQUEE = MAP+1;
    public static final short MENU = MARQUEE+1;
    public static final short META = MENU+1;
    public static final short MULTICOL = META+1;
    public static final short NEXTID = MULTICOL+1;
    public static final short NOBR = NEXTID+1;
    public static final short NOEMBED = NOBR+1;
    public static final short NOFRAMES = NOEMBED+1;
    public static final short NOLAYER = NOFRAMES+1;
    public static final short NOSCRIPT = NOLAYER+1;
    public static final short OBJECT = NOSCRIPT+1;
    public static final short OL = OBJECT+1;
    public static final short OPTION = OL+1;
    public static final short OPTGROUP = OPTION+1;
    public static final short P = OPTGROUP+1;
    public static final short PARAM = P+1;
    public static final short PLAINTEXT = PARAM+1;
    public static final short PRE = PLAINTEXT+1;
    public static final short Q = PRE+1;
    public static final short RB = Q+1;
    public static final short RBC = RB+1;
    public static final short RP = RBC+1;
    public static final short RT = RP+1;
    public static final short RTC = RT+1;
    public static final short RUBY = RTC+1;
    public static final short S = RUBY+1;
    public static final short SAMP = S+1;
    public static final short SCRIPT = SAMP+1;
    public static final short SELECT = SCRIPT+1;
    public static final short SMALL = SELECT+1;
    public static final short SOUND = SMALL+1;
    public static final short SPACER = SOUND+1;
    public static final short SPAN = SPACER+1;
    public static final short STRIKE = SPAN+1;
    public static final short STRONG = STRIKE+1;
    public static final short STYLE = STRONG+1;
    public static final short SUB = STYLE+1;
    public static final short SUP = SUB+1;
    public static final short TABLE = SUP+1;
    public static final short TBODY = TABLE+1;
    public static final short TD = TBODY+1;
    public static final short TEXTAREA = TD+1;
    public static final short TFOOT = TEXTAREA+1;
    public static final short TH = TFOOT+1;
    public static final short THEAD = TH+1;
    public static final short TITLE = THEAD+1;
    public static final short TR = TITLE+1;
    public static final short TT = TR+1;
    public static final short U = TT+1;
    public static final short UL = U+1;
    public static final short VAR = UL+1;
    public static final short WBR = VAR+1;
    public static final short XML = WBR+1;
    public static final short XMP = XML+1;

    // information

    /** Element information. */
    protected static final ElementList ELEMENTS = new ElementList();

    /** No such element. */
    public static final Element NO_SUCH_ELEMENT = new Element((short)-1, "", 0, BODY, null);

    //
    // Static initializer
    //

    /**
     * Initializes the element information.
     * <p>
     * <strong>Note:</strong>
     * The <code>getElement</code> method requires that the HTML elements
     * are added to the list in alphabetical order. If new elements are
     * added, then they <em>must</em> be inserted in alphabetical order.
     */
    static {
        // <!ENTITY % heading "H1|H2|H3|H4|H5|H6">
        // <!ENTITY % fontstyle "TT | I | B | BIG | SMALL">
        // <!ENTITY % phrase "EM | STRONG | DFN | CODE | SAMP | KBD | VAR | CITE | ABBR | ACRONYM" >
        // <!ENTITY % special "A | IMG | OBJECT | BR | SCRIPT | MAP | Q | SUB | SUP | SPAN | BDO">
        // <!ENTITY % formctrl "INPUT | SELECT | TEXTAREA | LABEL | BUTTON">
        // <!ENTITY % inline "#PCDATA | %fontstyle; | %phrase; | %special; | %formctrl;">
        // <!ENTITY % block "P | %heading; | %list; | %preformatted; | DL | DIV | NOSCRIPT | BLOCKQUOTE | FORM | HR | TABLE | FIELDSET | ADDRESS">
        // <!ENTITY % flow "%block; | %inline;">

        // A - - (%inline;)* -(A)
        ELEMENTS.addElement(new Element(A, "A", Element.INLINE, BODY, null));
        // ABBR - - (%inline;)*
        ELEMENTS.addElement(new Element(ABBR, "ABBR", Element.INLINE, BODY, null));
        // ACRONYM - - (%inline;)*
        ELEMENTS.addElement(new Element(ACRONYM, "ACRONYM", Element.INLINE, BODY, null));
        // ADDRESS - - (%inline;)*
        ELEMENTS.addElement(new Element(ADDRESS, "ADDRESS", Element.BLOCK, BODY, null));
        // APPLET
        ELEMENTS.addElement(new Element(APPLET, "APPLET", 0, BODY, null));
        // AREA - O EMPTY
        ELEMENTS.addElement(new Element(AREA, "AREA", Element.EMPTY, MAP, null));
        // B - - (%inline;)*
        ELEMENTS.addElement(new Element(B, "B", Element.INLINE, BODY, null));
        // BASE - O EMPTY
        ELEMENTS.addElement(new Element(BASE, "BASE", Element.EMPTY, HEAD, null));
        // BASEFONT
        ELEMENTS.addElement(new Element(BASEFONT, "BASEFONT", 0, HEAD, null));
        // BDO - - (%inline;)*
        ELEMENTS.addElement(new Element(BDO, "BDO", Element.INLINE, BODY, null));
        // BGSOUND
        ELEMENTS.addElement(new Element(BGSOUND, "BGSOUND", Element.EMPTY, HEAD, null));
        // BIG - - (%inline;)*
        ELEMENTS.addElement(new Element(BIG, "BIG", Element.INLINE, BODY, null));
        // BLINK
        ELEMENTS.addElement(new Element(BLINK, "BLINK", Element.INLINE, BODY, null));
        // BLOCKQUOTE - - (%block;|SCRIPT)+
        ELEMENTS.addElement(new Element(BLOCKQUOTE, "BLOCKQUOTE", Element.BLOCK, BODY, null));
        // BODY O O (%block;|SCRIPT)+ +(INS|DEL)
        ELEMENTS.addElement(new Element(BODY, "BODY", 0, HTML, new short[]{HEAD}));
        // BR - O EMPTY
        ELEMENTS.addElement(new Element(BR, "BR", Element.EMPTY, BODY, null));
        // BUTTON - - (%flow;)* -(A|%formctrl;|FORM|FIELDSET)
        ELEMENTS.addElement(new Element(BUTTON, "BUTTON", 0, FORM, null));
        // CAPTION - - (%inline;)*
        ELEMENTS.addElement(new Element(CAPTION, "CAPTION", Element.INLINE, TABLE, null));
        // CENTER, 
        ELEMENTS.addElement(new Element(CENTER, "CENTER", Element.INLINE, BODY, null));
        // CITE - - (%inline;)*
        ELEMENTS.addElement(new Element(CITE, "CITE", Element.INLINE, BODY, null));
        // CODE - - (%inline;)*
        ELEMENTS.addElement(new Element(CODE, "CODE", Element.INLINE, BODY, null));
        // COL - O EMPTY
        ELEMENTS.addElement(new Element(COL, "COL", 0, COLGROUP, new short[]{COL}));
        // COLGROUP - O (COL)*
        ELEMENTS.addElement(new Element(COLGROUP, "COLGROUP", 0, TABLE, new short[]{COLGROUP}));
        // COMMENT
        ELEMENTS.addElement(new Element(COMMENT, "COMMENT", Element.SPECIAL, HTML, null));
        // DEL - - (%flow;)*
        ELEMENTS.addElement(new Element(DEL, "DEL", 0, BODY, null));
        // DFN - - (%inline;)*
        ELEMENTS.addElement(new Element(DFN, "DFN", Element.INLINE, BODY, null));
        // DIR
        ELEMENTS.addElement(new Element(DIR, "DIR", 0, BODY, null));
        // DIV - - (%flow;)*
        ELEMENTS.addElement(new Element(DIV, "DIV", Element.BLOCK, BODY, null));
        // DD - O (%flow;)*
        ELEMENTS.addElement(new Element(DD, "DD", 0, DL, new short[]{DT,DD}));
        // DL - - (DT|DD)+
        ELEMENTS.addElement(new Element(DL, "DL", Element.BLOCK, BODY, null));
        // DT - O (%inline;)*
        ELEMENTS.addElement(new Element(DT, "DT", 0, DL, new short[]{DT,DD}));
        // EM - - (%inline;)*
        ELEMENTS.addElement(new Element(EM, "EM", Element.INLINE, BODY, null));
        // EMBED
        ELEMENTS.addElement(new Element(EMBED, "EMBED", 0, BODY, null));
        // FIELDSET - - (#PCDATA,LEGEND,(%flow;)*)
        ELEMENTS.addElement(new Element(FIELDSET, "FIELDSET", 0, FORM, null));
        // FONT
        ELEMENTS.addElement(new Element(FONT, "FONT", Element.INLINE, BODY, null));
        // FORM - - (%block;|SCRIPT)+ -(FORM)
        ELEMENTS.addElement(new Element(FORM, "FORM", 0, new short[]{BODY,TD}, null));
        // FRAME - O EMPTY
        ELEMENTS.addElement(new Element(FRAME, "FRAME", Element.EMPTY, FRAMESET, null));
        // FRAMESET - - ((FRAMESET|FRAME)+ & NOFRAMES?)
        ELEMENTS.addElement(new Element(FRAMESET, "FRAMESET", 0, HTML, null));
        // (H1|H2|H3|H4|H5|H6) - - (%inline;)*
        ELEMENTS.addElement(new Element(H1, "H1", Element.BLOCK, BODY, new short[]{H1,H2,H3,H4,H5,H6,P}));
        ELEMENTS.addElement(new Element(H2, "H2", Element.BLOCK, BODY, new short[]{H1,H2,H3,H4,H5,H6,P}));
        ELEMENTS.addElement(new Element(H3, "H3", Element.BLOCK, BODY, new short[]{H1,H2,H3,H4,H5,H6,P}));
        ELEMENTS.addElement(new Element(H4, "H4", Element.BLOCK, BODY, new short[]{H1,H2,H3,H4,H5,H6,P}));
        ELEMENTS.addElement(new Element(H5, "H5", Element.BLOCK, BODY, new short[]{H1,H2,H3,H4,H5,H6,P}));
        ELEMENTS.addElement(new Element(H6, "H6", Element.BLOCK, BODY, new short[]{H1,H2,H3,H4,H5,H6,P}));
        // HEAD O O (%head.content;) +(%head.misc;)
        ELEMENTS.addElement(new Element(HEAD, "HEAD", 0, HTML, null));
        // HR - O EMPTY
        ELEMENTS.addElement(new Element(HR, "HR", Element.EMPTY, BODY, new short[]{P}));
        // HTML O O (%html.content;)
        ELEMENTS.addElement(new Element(HTML, "HTML", 0, null, null));
        // I - - (%inline;)*
        ELEMENTS.addElement(new Element(I, "I", Element.INLINE, BODY, null));
        // IFRAME
        ELEMENTS.addElement(new Element(IFRAME, "IFRAME", Element.BLOCK, BODY, null));
        // ILAYER
        ELEMENTS.addElement(new Element(ILAYER, "ILAYER", Element.BLOCK, BODY, null));
        // IMG - O EMPTY
        ELEMENTS.addElement(new Element(IMG, "IMG", Element.EMPTY, BODY, null));
        // INPUT - O EMPTY
        ELEMENTS.addElement(new Element(INPUT, "INPUT", Element.EMPTY, FORM, null));
        // INS - - (%flow;)*
        ELEMENTS.addElement(new Element(INS, "INS", 0, BODY, null));
        // ISINDEX
        ELEMENTS.addElement(new Element(ISINDEX, "ISINDEX", 0, HEAD, null));
        // KBD - - (%inline;)*
        ELEMENTS.addElement(new Element(KBD, "KBD", Element.INLINE, BODY, null));
        // KEYGEN
        ELEMENTS.addElement(new Element(KEYGEN, "KEYGEN", 0, FORM, null));
        // LABEL - - (%inline;)* -(LABEL)
        ELEMENTS.addElement(new Element(LABEL, "LABEL", 0, FORM, null));
        // LAYER
        ELEMENTS.addElement(new Element(LAYER, "LAYER", Element.BLOCK, BODY, null));
        // LEGEND - - (%inline;)*
        ELEMENTS.addElement(new Element(LEGEND, "LEGEND", Element.INLINE, FIELDSET, null));
        // LI - O (%flow;)*
        ELEMENTS.addElement(new Element(LI, "LI", 0, BODY, new short[]{LI}));
        // LINK - O EMPTY
        ELEMENTS.addElement(new Element(LINK, "LINK", Element.EMPTY, HEAD, null));
        // LISTING
        ELEMENTS.addElement(new Element(LISTING, "LISTING", 0, BODY, null));
        // MAP - - ((%block;) | AREA)+
        ELEMENTS.addElement(new Element(MAP, "MAP", Element.INLINE, BODY, null));
        // MARQUEE
        ELEMENTS.addElement(new Element(MARQUEE, "MARQUEE", 0, BODY, null));
        // MENU
        ELEMENTS.addElement(new Element(MENU, "MENU", 0, BODY, null));
        // META - O EMPTY
        ELEMENTS.addElement(new Element(META, "META", Element.EMPTY, HEAD, new short[]{STYLE,TITLE}));
        // MULTICOL
        ELEMENTS.addElement(new Element(MULTICOL, "MULTICOL", 0, BODY, null));
        // NEXTID
        ELEMENTS.addElement(new Element(NEXTID, "NEXTID", Element.EMPTY, BODY, null));
        // NOBR
        ELEMENTS.addElement(new Element(NOBR, "NOBR", Element.INLINE, BODY, null));
        // NOEMBED
        ELEMENTS.addElement(new Element(NOEMBED, "NOEMBED", 0, BODY, null));
        // NOFRAMES - - (BODY) -(NOFRAMES)
        ELEMENTS.addElement(new Element(NOFRAMES, "NOFRAMES", 0, FRAMESET, null));
        // NOLAYER
        ELEMENTS.addElement(new Element(NOLAYER, "NOLAYER", 0, BODY, null));
        // NOSCRIPT - - (%block;)+
        ELEMENTS.addElement(new Element(NOSCRIPT, "NOSCRIPT", 0, new short[]{HEAD,BODY}, null));
        // OBJECT - - (PARAM | %flow;)*
        ELEMENTS.addElement(new Element(OBJECT, "OBJECT", 0, BODY, null));
        // OL - - (LI)+
        ELEMENTS.addElement(new Element(OL, "OL", Element.BLOCK, BODY, null));
        // OPTGROUP - - (OPTION)+
        ELEMENTS.addElement(new Element(OPTGROUP, "OPTGROUP", 0, SELECT, new short[]{OPTION}));
        // OPTION - O (#PCDATA)
        ELEMENTS.addElement(new Element(OPTION, "OPTION", 0, SELECT, new short[]{OPTION}));
        // P - O (%inline;)*
        ELEMENTS.addElement(new Element(P, "P", 0, BODY, new short[]{P}));
        // PARAM - O EMPTY
        ELEMENTS.addElement(new Element(PARAM, "PARAM", Element.EMPTY, OBJECT, null));
        // PLAINTEXT
        ELEMENTS.addElement(new Element(PLAINTEXT, "PLAINTEXT", Element.SPECIAL, BODY, null));
        // PRE - - (%inline;)* -(%pre.exclusion;)
        ELEMENTS.addElement(new Element(PRE, "PRE", 0, BODY, null));
        // Q - - (%inline;)*
        ELEMENTS.addElement(new Element(Q, "Q", Element.INLINE, BODY, null));
        // RB
        ELEMENTS.addElement(new Element(RB, "RB", Element.INLINE, RUBY, new short[]{RB}));
        // RBC
        ELEMENTS.addElement(new Element(RBC, "RBC", 0, RUBY, null));
        // RP
        ELEMENTS.addElement(new Element(RP, "RP", Element.INLINE, RUBY, new short[]{RB}));
        // RT
        ELEMENTS.addElement(new Element(RT, "RT", Element.INLINE, RUBY, new short[]{RB,RP}));
        // RTC
        ELEMENTS.addElement(new Element(RTC, "RTC", 0, RUBY, new short[]{RBC}));
        // RUBY
        ELEMENTS.addElement(new Element(RUBY, "RUBY", 0, BODY, new short[]{RUBY}));
        // S
        ELEMENTS.addElement(new Element(S, "S", 0, BODY, null));
        // SAMP - - (%inline;)*
        ELEMENTS.addElement(new Element(SAMP, "SAMP", Element.INLINE, BODY, null));
        // SCRIPT - - %Script;
        ELEMENTS.addElement(new Element(SCRIPT, "SCRIPT", Element.SPECIAL, new short[]{HEAD,BODY}, null));
        // SELECT - - (OPTGROUP|OPTION)+
        ELEMENTS.addElement(new Element(SELECT, "SELECT", 0, FORM, new short[]{SELECT}));
        // SMALL - - (%inline;)*
        ELEMENTS.addElement(new Element(SMALL, "SMALL", Element.INLINE, BODY, null));
        // SOUND
        ELEMENTS.addElement(new Element(SOUND, "SOUND", Element.EMPTY, HEAD, null));
        // SPACER
        ELEMENTS.addElement(new Element(SPACER, "SPACER", Element.EMPTY, BODY, null));
        // SPAN - - (%inline;)*
        ELEMENTS.addElement(new Element(SPAN, "SPAN", Element.INLINE, BODY, null));
        // STRIKE
        ELEMENTS.addElement(new Element(STRIKE, "STRIKE", Element.INLINE, BODY, null));
        // STRONG - - (%inline;)*
        ELEMENTS.addElement(new Element(STRONG, "STRONG", Element.INLINE, BODY, null));
        // STYLE - - %StyleSheet;
        ELEMENTS.addElement(new Element(STYLE, "STYLE", 0, new short[]{HEAD,BODY}, new short[]{STYLE,TITLE,META}));
        // SUB - - (%inline;)*
        ELEMENTS.addElement(new Element(SUB, "SUB", Element.INLINE, BODY, null));
        // SUP - - (%inline;)*
        ELEMENTS.addElement(new Element(SUP, "SUP", Element.INLINE, BODY, null));
        // TABLE - - (CAPTION?, (COL*|COLGROUP*), THEAD?, TFOOT?, TBODY+)
        ELEMENTS.addElement(new Element(TABLE, "TABLE", Element.BLOCK|Element.CONTAINER, BODY, null));
        // TBODY O O (TR)+
        ELEMENTS.addElement(new Element(TBODY, "TBODY", 0, TABLE, new short[]{THEAD,TD,TH,TR}));
        // TD - O (%flow;)*
        ELEMENTS.addElement(new Element(TD, "TD", 0, TABLE, new short[]{TD,TH}));
        // TEXTAREA - - (#PCDATA)
        ELEMENTS.addElement(new Element(TEXTAREA, "TEXTAREA", Element.SPECIAL, FORM, null));
        // TFOOT - O (TR)+
        ELEMENTS.addElement(new Element(TFOOT, "TFOOT", 0, TABLE, new short[]{THEAD,TBODY,TD,TH,TR}));
        // TH - O (%flow;)*
        ELEMENTS.addElement(new Element(TH, "TH", 0, TR, new short[]{TD,TH}));
        // THEAD - O (TR)+
        ELEMENTS.addElement(new Element(THEAD, "THEAD", 0, TABLE, null));
        // TITLE - - (#PCDATA) -(%head.misc;)
        ELEMENTS.addElement(new Element(TITLE, "TITLE", 0, new short[]{HEAD,BODY}, null));
        // TR - O (TH|TD)+
        ELEMENTS.addElement(new Element(TR, "TR", Element.BLOCK, TABLE, new short[]{TD,TH,TR}));
        // TT - - (%inline;)*
        ELEMENTS.addElement(new Element(TT, "TT", Element.INLINE, BODY, null));
        // U, 
        ELEMENTS.addElement(new Element(U, "U", Element.INLINE, BODY, null));
        // UL - - (LI)+
        ELEMENTS.addElement(new Element(UL, "UL", Element.BLOCK, BODY, null));
        // VAR - - (%inline;)*
        ELEMENTS.addElement(new Element(VAR, "VAR", Element.INLINE, BODY, null));
        // WBR
        ELEMENTS.addElement(new Element(WBR, "WBR", Element.EMPTY, BODY, null));
        // XML
        ELEMENTS.addElement(new Element(XML, "XML", 0, BODY, null));
        // XMP
        ELEMENTS.addElement(new Element(XMP, "XMP", Element.SPECIAL, BODY, null));
        // no such element -- this should always be at the end
        ELEMENTS.addElement(NO_SUCH_ELEMENT);

        // initialize cross references to parent elements
        for (int i = 0; i < ELEMENTS.size; i++) {
            Element element = ELEMENTS.data[i];
            if (element.parentCodes != null) {
                element.parent = new Element[element.parentCodes.length];
                for (int j = 0; j < element.parentCodes.length; j++) {
                    element.parent[j] = ELEMENTS.data[element.parentCodes[j]];
                }
                element.parentCodes = null;
            }
        }

    } // <clinit>()

    //
    // Public static methods
    //

    /**
     * Returns the element information for the specified element code.
     *
     * @param code The element code.
     */
    public static final Element getElement(short code) {
        return ELEMENTS.data[code];
    } // getElement(short):Element

    /**
     * Returns the element information for the specified element name.
     *
     * @param ename The element name.
     */
    public static final Element getElement(String ename) {
        return getElement(ename, NO_SUCH_ELEMENT);
    } // getElement(String):Element

    /**
     * Returns the element information for the specified element name.
     *
     * @param ename The element name.
     * @param element The default element to return if not found.
     */
    public static final Element getElement(String ename, Element element) {

        int head = 0;
        int tail = ELEMENTS.size;
        while (head != tail) {
            int midpoint = head + (tail - head) / 2;
            Element elem = ELEMENTS.data[midpoint];
            int comparison = elem.name.compareToIgnoreCase(ename);
            if (comparison == 0) {
                return elem;
            }
            if (comparison < 0) {
                head = midpoint + 1;
            }
            else {
                tail = midpoint;
            }
        }
        return element;

    } // getElement(String):Element

    //
    // Classes
    //

    /**
     * Element information.
     *
     * @author Andy Clark
     */
    public static class Element {

        //
        // Constants
        //

        /** Inline element. */
        public static final int INLINE = 0x01;

        /** Block element. */
        public static final int BLOCK = 0x02;

        /** Empty element. */
        public static final int EMPTY = 0x04;

        /** Container element. */
        public static final int CONTAINER = 0x08;

        /** Special element. */
        public static final int SPECIAL = 0x10;

        /** Empty array. */
        private static final short[] EMPTY_ARRAY = new short[0];

        //
        // Data
        //

        /** The element code. */
        public short code;

        /** The element name. */
        public String name;

        /** Informational flags. */
        public int flags;

        /** Parent elements. */
        public short[] parentCodes;

        /** Parent elements. */
        public Element[] parent;

        /** List of elements this element can close. */
        public short[] closes;

        //
        // Constructors
        //

        /** 
         * Constructs an element object.
         *
         * @param code The element code.
         * @param name The element name.
         * @param flags Informational flags
         * @param parent Natural closing parent name.
         * @param closes List of elements this element can close.
         */
        public Element(short code, String name, int flags, 
                       short parent, short[] closes) {
            this(code, name, flags, new short[]{parent}, closes);
        } // <init>(short,String,int,short,short[])

        /** 
         * Constructs an element object.
         *
         * @param code The element code.
         * @param name The element name.
         * @param flags Informational flags
         * @param parents Natural closing parent names.
         * @param closes List of elements this element can close.
         */
        public Element(short code, String name, int flags, 
                       short[] parent, short[] closes) {
            this.code = code;
            this.name = name;
            this.flags = flags;
            this.parentCodes = parent;
            this.parent = null;
            this.closes = closes;
        } // <init>(short,String,int,short[],short[])

        //
        // Public methods
        //

        /** Returns true if this element is an inline element. */
        public final boolean isInline() {
            return (flags & INLINE) != 0;
        } // isInline():boolean

        /** Returns true if this element is a block element. */
        public final boolean isBlock() {
            return (flags & BLOCK) != 0;
        } // isBlock():boolean

        /** Returns true if this element is an empty element. */
        public final boolean isEmpty() {
            return (flags & EMPTY) != 0;
        } // isEmpty():boolean

        /** Returns true if this element is a container element. */
        public final boolean isContainer() {
            return (flags & CONTAINER) != 0;
        } // isContainer():boolean

        /** 
         * Returns true if this element is special -- if its content
         * should be parsed ignoring markup.
         */
        public final boolean isSpecial() {
            return (flags & SPECIAL) != 0;
        } // isSpecial():boolean

        /**
         * Returns true if this element can close the specified Element.
         *
         * @param tag The element.
         */
        public boolean closes(short tag) {

            if (closes != null) {
                for (int i = 0; i < closes.length; i++) {
                    if (closes[i] == tag) {
                        return true;
                    }
                }
            }
            return false;

        } // closes(short):boolean

        //
        // Object methods
        //

        /** Returns a hash code for this object. */
        public int hashCode() {
            return name.hashCode();
        } // hashCode():int

        /** Returns true if the objects are equal. */
        public boolean equals(Object o) {
            return name.equals(o);
        } // equals(Object):boolean

    } // class Element

    /** Unsynchronized list of elements. */
    public static class ElementList {

        //
        // Data
        //

        /** The size of the list. */
        public int size;

        /** The data in the list. */
        public Element[] data = new Element[120];

        //
        // Public methods
        //

        /** Adds an element to list, resizing if necessary. */
        public void addElement(Element element) {
            if (size == data.length) {
                Element[] newarray = new Element[size + 20];
                System.arraycopy(data, 0, newarray, 0, size);
                data = newarray;
            }
            data[size++] = element;
        } // addElement(Element)

    } // class Element

} // class HTMLElements
