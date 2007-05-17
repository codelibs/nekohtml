/* 
 * (C) Copyright 2002-2005, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

package org.cyberneko.html;

/**
 * This interface is used to pass augmentated information to the
 * application through the XNI pipeline.
 *
 * @author Andy Clark
 *
 * @version $Id: HTMLEventInfo.java,v 1.4 2005/02/14 03:56:54 andyc Exp $
 */
public interface HTMLEventInfo {

    //
    // HTMLEventInfo methods
    //

    // location information

    /** Returns the line number of the beginning of this event.*/
    public int getBeginLineNumber();

    /** Returns the column number of the beginning of this event.*/
    public int getBeginColumnNumber();

    /** Returns the line number of the end of this event.*/
    public int getEndLineNumber();

    /** Returns the column number of the end of this event.*/
    public int getEndColumnNumber();

    // other information

    /** Returns true if this corresponding event was synthesized. */
    public boolean isSynthesized();

    /**
     * Synthesized infoset item.
     *
     * @author Andy Clark
     */
    public static class SynthesizedItem
        implements HTMLEventInfo {

        //
        // HTMLEventInfo methods
        //

        // location information

        /** Returns the line number of the beginning of this event.*/
        public int getBeginLineNumber() {
            return -1;
        } // getBeginLineNumber():int

        /** Returns the column number of the beginning of this event.*/
        public int getBeginColumnNumber() { 
            return -1;
        } // getBeginColumnNumber():int

        /** Returns the line number of the end of this event.*/
        public int getEndLineNumber() {
            return -1;
        } // getEndLineNumber():int

        /** Returns the column number of the end of this event.*/
        public int getEndColumnNumber() {
            return -1;
        } // getEndColumnNumber():int

        // other information

        /** Returns true if this corresponding event was synthesized. */
        public boolean isSynthesized() {
            return true;
        } // isSynthesized():boolean

        //
        // Object methods
        //

        /** Returns a string representation of this object. */
        public String toString() {
            return "synthesized";
        } // toString():String

    } // class SynthesizedItem

} // interface HTMLEventInfo
