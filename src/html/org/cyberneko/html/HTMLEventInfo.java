/* 
 * (C) Copyright 2002, Andy Clark.  All rights reserved.
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
 * @version $Id$
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

} // interface HTMLEventInfo
