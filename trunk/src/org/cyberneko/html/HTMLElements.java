/* 
 * (C) Copyright 2002, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

package org.cyberneko.html;

import java.util.Vector;

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
    
    /** Element information. */
    protected static final Vector ELEMENTS = new Vector();

    /** No such element. */
    public static final Element NO_SUCH_ELEMENT = new Element("", 0, "BODY", null);

    //
    // Static initializer
    //

    // <!ENTITY % heading "H1|H2|H3|H4|H5|H6">
    // <!ENTITY % fontstyle "TT | I | B | BIG | SMALL">
    // <!ENTITY % phrase "EM | STRONG | DFN | CODE | SAMP | KBD | VAR | CITE | ABBR | ACRONYM" >
    // <!ENTITY % special "A | IMG | OBJECT | BR | SCRIPT | MAP | Q | SUB | SUP | SPAN | BDO">
    // <!ENTITY % formctrl "INPUT | SELECT | TEXTAREA | LABEL | BUTTON">
    // <!ENTITY % inline "#PCDATA | %fontstyle; | %phrase; | %special; | %formctrl;">
    // <!ENTITY % block "P | %heading; | %list; | %preformatted; | DL | DIV | NOSCRIPT | BLOCKQUOTE | FORM | HR | TABLE | FIELDSET | ADDRESS">
    // <!ENTITY % flow "%block; | %inline;">
    
    static {
        // A - - (%inline;)* -(A)
        ELEMENTS.addElement(new Element("A", Element.INLINE, "BODY", null));
        // ABBR - - (%inline;)*
        ELEMENTS.addElement(new Element("ABBR", Element.INLINE, "BODY", null));
        // ACRONYM - - (%inline;)*
        ELEMENTS.addElement(new Element("ACRONYM", Element.INLINE, "BODY", null));
        // ADDRESS - - (%inline;)*
        ELEMENTS.addElement(new Element("ADDRESS", Element.BLOCK, "BODY", null));
        // APPLET
        ELEMENTS.addElement(new Element("APPLET", 0, "BODY", null));
        // AREA - O EMPTY
        ELEMENTS.addElement(new Element("AREA", Element.EMPTY, "MAP", null));
        // B - - (%inline;)*
        ELEMENTS.addElement(new Element("B", Element.INLINE, "BODY", null));
        // BASE - O EMPTY
        ELEMENTS.addElement(new Element("BASE", Element.EMPTY, "HEAD", null));
        // BASEFONT
        ELEMENTS.addElement(new Element("BASEFONT", 0, "HEAD", null));
        // BDO - - (%inline;)*
        ELEMENTS.addElement(new Element("BDO", Element.INLINE, "BODY", null));
        // BGSOUND
        ELEMENTS.addElement(new Element("BGSOUND", Element.EMPTY, "HEAD", null));
        // BIG - - (%inline;)*
        ELEMENTS.addElement(new Element("BIG", Element.INLINE, "BODY", null));
        // BLINK
        ELEMENTS.addElement(new Element("BLINK", Element.INLINE, "BODY", null));
        // BLOCKQUOTE - - (%block;|SCRIPT)+
        ELEMENTS.addElement(new Element("BLOCKQUOTE", Element.BLOCK, "BODY", null));
        // BODY O O (%block;|SCRIPT)+ +(INS|DEL)
        ELEMENTS.addElement(new Element("BODY", 0, "HTML", new String[]{"HEAD"}));
        // BR - O EMPTY
        ELEMENTS.addElement(new Element("BR", Element.EMPTY, "BODY", null));
        // BUTTON - - (%flow;)* -(A|%formctrl;|FORM|FIELDSET)
        ELEMENTS.addElement(new Element("BUTTON", 0, "FORM", null));
        // CAPTION - - (%inline;)*
        ELEMENTS.addElement(new Element("CAPTION", Element.INLINE, "TABLE", null));
        // CENTER, 
        ELEMENTS.addElement(new Element("CENTER", Element.INLINE, "BODY", null));
        // CITE - - (%inline;)*
        ELEMENTS.addElement(new Element("CITE", Element.INLINE, "BODY", null));
        // CODE - - (%inline;)*
        ELEMENTS.addElement(new Element("CODE", Element.INLINE, "BODY", null));
        // COL - O EMPTY
        ELEMENTS.addElement(new Element("COL", 0, "COLGROUP", new String[] {"COL"}));
        // COLGROUP - O (COL)*
        ELEMENTS.addElement(new Element("COLGROUP", 0, "TABLE", new String[]{"COLGROUP"}));
        // COMMENT
        ELEMENTS.addElement(new Element("COMMENT", Element.SPECIAL, "HTML", null));
        // DEL - - (%flow;)*
        ELEMENTS.addElement(new Element("DEL", 0, "BODY", null));
        // DFN - - (%inline;)*
        ELEMENTS.addElement(new Element("DFN", Element.INLINE, "BODY", null));
        // DIR
        ELEMENTS.addElement(new Element("DIR", 0, "BODY", null));
        // DIV - - (%flow;)*
        ELEMENTS.addElement(new Element("DIV", Element.BLOCK, "BODY", null));
        // DD - O (%flow;)*
        ELEMENTS.addElement(new Element("DD", 0, "DL", new String[]{"DT","DD"}));
        // DL - - (DT|DD)+
        ELEMENTS.addElement(new Element("DL", Element.BLOCK, "BODY", null));
        // DT - O (%inline;)*
        ELEMENTS.addElement(new Element("DT", 0, "DL", new String[]{"DT","DD"}));
        // EM - - (%inline;)*
        ELEMENTS.addElement(new Element("EM", Element.INLINE, "BODY", null));
        // EMBED
        ELEMENTS.addElement(new Element("EMBED", 0, "BODY", null));
        // FIELDSET - - (#PCDATA,LEGEND,(%flow;)*)
        ELEMENTS.addElement(new Element("FIELDSET", 0, "FORM", null));
        // FONT
        ELEMENTS.addElement(new Element("FONT", Element.INLINE, "BODY", null));
        // FORM - - (%block;|SCRIPT)+ -(FORM)
        ELEMENTS.addElement(new Element("FORM", 0, new String[]{"BODY","TD"}, null));
        // FRAME - O EMPTY
        ELEMENTS.addElement(new Element("FRAME", Element.EMPTY, "FRAMESET", null));
        // FRAMESET - - ((FRAMESET|FRAME)+ & NOFRAMES?)
        ELEMENTS.addElement(new Element("FRAMESET", 0, "HTML", null));
        // (H1|H2|H3|H4|H5|H6) - - (%inline;)*
        ELEMENTS.addElement(new Element("H1", Element.BLOCK, "BODY", new String[]{"H1","H2","H3","H4","H5","H6","P"}));
        ELEMENTS.addElement(new Element("H2", Element.BLOCK, "BODY", new String[]{"H1","H2","H3","H4","H5","H6","P"}));
        ELEMENTS.addElement(new Element("H3", Element.BLOCK, "BODY", new String[]{"H1","H2","H3","H4","H5","H6","P"}));
        ELEMENTS.addElement(new Element("H4", Element.BLOCK, "BODY", new String[]{"H1","H2","H3","H4","H5","H6","P"}));
        ELEMENTS.addElement(new Element("H5", Element.BLOCK, "BODY", new String[]{"H1","H2","H3","H4","H5","H6","P"}));
        ELEMENTS.addElement(new Element("H6", Element.BLOCK, "BODY", new String[]{"H1","H2","H3","H4","H5","H6","P"}));
        // HEAD O O (%head.content;) +(%head.misc;)
        ELEMENTS.addElement(new Element("HEAD", 0, "HTML", null));
        // HR - O EMPTY
        ELEMENTS.addElement(new Element("HR", Element.EMPTY, "BODY", new String[]{"P"}));
        // HTML O O (%html.content;)
        ELEMENTS.addElement(new Element("HTML", 0, null, null));
        // I - - (%inline;)*
        ELEMENTS.addElement(new Element("I", Element.INLINE, "BODY", null));
        // IFRAME
        ELEMENTS.addElement(new Element("IFRAME", Element.BLOCK, "BODY", null));
        // ILAYER
        ELEMENTS.addElement(new Element("ILAYER", Element.BLOCK, "BODY", null));
        // IMG - O EMPTY
        ELEMENTS.addElement(new Element("IMG", Element.EMPTY, "BODY", null));
        // INPUT - O EMPTY
        ELEMENTS.addElement(new Element("INPUT", Element.EMPTY, "FORM", null));
        // INS - - (%flow;)*
        ELEMENTS.addElement(new Element("INS", 0, "BODY", null));
        // ISINDEX
        ELEMENTS.addElement(new Element("ISINDEX", 0, "HEAD", null));
        // KBD - - (%inline;)*
        ELEMENTS.addElement(new Element("KBD", Element.INLINE, "BODY", null));
        // KEYGEN
        ELEMENTS.addElement(new Element("KEYGEN", 0, "FORM", null));
        // LABEL - - (%inline;)* -(LABEL)
        ELEMENTS.addElement(new Element("LABEL", 0, "FORM", null));
        // LAYER
        ELEMENTS.addElement(new Element("LAYER", Element.BLOCK, "BODY", null));
        // LEGEND - - (%inline;)*
        ELEMENTS.addElement(new Element("LEGEND", Element.INLINE, "FIELDSET", null));
        // LI - O (%flow;)*
        ELEMENTS.addElement(new Element("LI", 0, "BODY", new String[]{"LI"}));
        // LINK - O EMPTY
        ELEMENTS.addElement(new Element("LINK", Element.EMPTY, "HEAD", null));
        // LISTING
        ELEMENTS.addElement(new Element("LISTING", 0, "BODY", null));
        // MAP - - ((%block;) | AREA)+
        ELEMENTS.addElement(new Element("MAP", Element.INLINE, "BODY", null));
        // MARQUEE
        ELEMENTS.addElement(new Element("MARQUEE", 0, "BODY", null));
        // MENU
        ELEMENTS.addElement(new Element("MENU", 0, "BODY", null));
        // META - O EMPTY
        ELEMENTS.addElement(new Element("META", Element.EMPTY, "HEAD", new String[]{"STYLE","TITLE"}));
        // MULTICOL
        ELEMENTS.addElement(new Element("MULTICOL", 0, "BODY", null));
        // NEXTID
        ELEMENTS.addElement(new Element("NEXTID", Element.EMPTY, "BODY", null));
        // NOBR
        ELEMENTS.addElement(new Element("NOBR", Element.INLINE, "BODY", new String[]{}));
        // NOEMBED
        ELEMENTS.addElement(new Element("NOEMBED", 0, "BODY", null));
        // NOFRAMES - - (BODY) -(NOFRAMES)
        ELEMENTS.addElement(new Element("NOFRAMES", 0, "FRAMESET", null));
        // NOLAYER
        ELEMENTS.addElement(new Element("NOLAYER", 0, "BODY", null));
        // NOSCRIPT - - (%block;)+
        ELEMENTS.addElement(new Element("NOSCRIPT", 0, new String[]{"HEAD","BODY"}, null));
        // OBJECT - - (PARAM | %flow;)*
        ELEMENTS.addElement(new Element("OBJECT", 0, "BODY", null));
        // OL - - (LI)+
        ELEMENTS.addElement(new Element("OL", Element.BLOCK, "BODY", null));
        // OPTION - O (#PCDATA)
        ELEMENTS.addElement(new Element("OPTION", 0, "SELECT", new String[]{"OPTION"}));
        // OPTGROUP - - (OPTION)+
        ELEMENTS.addElement(new Element("OPTGROUP", 0, "SELECT", new String[]{"OPTION"}));
        // P - O (%inline;)*
        ELEMENTS.addElement(new Element("P", 0, "BODY", new String[]{"P"}));
        // PARAM - O EMPTY
        ELEMENTS.addElement(new Element("PARAM", Element.EMPTY, "OBJECT", null));
        // PLAINTEXT
        ELEMENTS.addElement(new Element("PLAINTEXT", Element.SPECIAL, "BODY", null));
        // PRE - - (%inline;)* -(%pre.exclusion;)
        ELEMENTS.addElement(new Element("PRE", 0, "BODY", null));
        // Q - - (%inline;)*
        ELEMENTS.addElement(new Element("Q", Element.INLINE, "BODY", null));
        // RT
        ELEMENTS.addElement(new Element("RT", 0, "RUBY", null));
        // RUBY
        ELEMENTS.addElement(new Element("RUBY", 0, "BODY", null));
        // S
        ELEMENTS.addElement(new Element("S", 0, "BODY", null));
        // SAMP - - (%inline;)*
        ELEMENTS.addElement(new Element("SAMP", Element.INLINE, "BODY", null));
        // SCRIPT - - %Script;
        ELEMENTS.addElement(new Element("SCRIPT", Element.SPECIAL, new String[]{"HEAD","BODY"}, null));
        // SELECT - - (OPTGROUP|OPTION)+
        ELEMENTS.addElement(new Element("SELECT", 0, "FORM", new String[]{"SELECT"}));
        // SMALL - - (%inline;)*
        ELEMENTS.addElement(new Element("SMALL", Element.INLINE, "BODY", null));
        // SOUND
        ELEMENTS.addElement(new Element("SOUND", Element.EMPTY, "HEAD", null));
        // SPACER
        ELEMENTS.addElement(new Element("SPACER", Element.EMPTY, "BODY", null));
        // SPAN - - (%inline;)*
        ELEMENTS.addElement(new Element("SPAN", Element.INLINE, "BODY", null));
        // STRIKE
        ELEMENTS.addElement(new Element("STRIKE", Element.INLINE, "BODY", null));
        // STRONG - - (%inline;)*
        ELEMENTS.addElement(new Element("STRONG", Element.INLINE, "BODY", null));
        // STYLE - - %StyleSheet;
        ELEMENTS.addElement(new Element("STYLE", 0, new String[]{"HEAD","BODY"}, new String[]{"STYLE","TITLE","META"}));
        // SUB - - (%inline;)*
        ELEMENTS.addElement(new Element("SUB", Element.INLINE, "BODY", null));
        // SUP - - (%inline;)*
        ELEMENTS.addElement(new Element("SUP", Element.INLINE, "BODY", null));
        // TABLE - - (CAPTION?, (COL*|COLGROUP*), THEAD?, TFOOT?, TBODY+)
        ELEMENTS.addElement(new Element("TABLE", Element.BLOCK|Element.CONTAINER, "BODY", null));
        // TBODY O O (TR)+
        ELEMENTS.addElement(new Element("TBODY", 0, "TABLE", new String[]{"TD","THEAD","TR"}));
        // TEXTAREA - - (#PCDATA)
        ELEMENTS.addElement(new Element("TEXTAREA", Element.SPECIAL, "FORM", null));
        // TD - O (%flow;)*
        ELEMENTS.addElement(new Element("TD", 0, "TABLE", new String[]{"TD","TH"}));
        // TFOOT - O (TR)+
        ELEMENTS.addElement(new Element("TFOOT", 0, "TABLE", new String[]{"THEAD","TBODY","TD","TR"}));
        // TH - O (%flow;)*
        ELEMENTS.addElement(new Element("TH", 0, "TR", null));
        // THEAD - O (TR)+
        ELEMENTS.addElement(new Element("THEAD", 0, "TABLE", null));
        // TITLE - - (#PCDATA) -(%head.misc;)
        ELEMENTS.addElement(new Element("TITLE", 0, "HEAD", null));
        // TR - O (TH|TD)+
        ELEMENTS.addElement(new Element("TR", Element.BLOCK, "TABLE", new String[]{"TD","TR"}));
        // TT - - (%inline;)*
        ELEMENTS.addElement(new Element("TT", Element.INLINE, "BODY", null));
        // U, 
        ELEMENTS.addElement(new Element("U", Element.INLINE, "BODY", null));
        // UL - - (LI)+
        ELEMENTS.addElement(new Element("UL", Element.BLOCK, "BODY", null));
        // VAR - - (%inline;)*
        ELEMENTS.addElement(new Element("VAR", Element.INLINE, "BODY", null));
        // WBR
        ELEMENTS.addElement(new Element("WBR", Element.EMPTY, "BODY", null));
        // XML
        ELEMENTS.addElement(new Element("XML", 0, "BODY", null));
        // XMP
        ELEMENTS.addElement(new Element("XMP", Element.SPECIAL, "BODY", null));
    } // <clinit>()

    //
    // Public static methods
    //

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

        int length = ELEMENTS.size();
        for (int i = 0; i < length; i++) {
            Element elem = (Element)ELEMENTS.elementAt(i);
            if (elem.name.equalsIgnoreCase(ename)) {
                return elem;
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

        /** Empty string array. */
        private static final String[] EMPTY_CLOSES = {};

        //
        // Data
        //

        /** The element name. */
        public String name;

        /** Informational flags. */
        public int flags;

        /** Natural closing element name. */
        public Object parent;

        /** List of elements this element can close. */
        public String[] closes;

        //
        // Constructors
        //

        /** 
         * Constructs an element object.
         *
         * @param name The element name.
         * @param flags Informational flags
         * @param parent Natural closing parent name.
         * @param closes List of elements this element can close.
         */
        public Element(String name, int flags, Object parent, String[] closes) {
            this.name = name;
            this.flags = flags;
            this.parent = parent;
            this.closes = closes != null ? closes : EMPTY_CLOSES;
        } // <init>(String,int,Object,String[])

        //
        // Public methods
        //

        /** Returns true if this element is an inline element. */
        public boolean isInline() {
            return (flags & INLINE) != 0;
        } // isInline():boolean

        /** Returns true if this element is a block element. */
        public boolean isBlock() {
            return (flags & BLOCK) != 0;
        } // isBlock():boolean

        /** Returns true if this element is an empty element. */
        public boolean isEmpty() {
            return (flags & EMPTY) != 0;
        } // isEmpty():boolean

        /** Returns true if this element is a container element. */
        public boolean isContainer() {
            return (flags & CONTAINER) != 0;
        } // isContainer():boolean

        /** 
         * Returns true if this element is special -- if its content
         * should be parsed ignoring markup.
         */
        public boolean isSpecial() {
            return (flags & SPECIAL) != 0;
        } // isSpecial():boolean

        /**
         * Returns true if this element can close the specified Element.
         *
         * @param tag The element.
         */
        public boolean closes(String tag) {

            for (int i = 0; i < closes.length; i++) {
                if (closes[i].equalsIgnoreCase(tag)) {
                    return true;
                }
            }
            return false;

        } // closes(String):boolean

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

} // class HTMLElements
