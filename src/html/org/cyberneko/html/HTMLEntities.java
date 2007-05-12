/* 
 * (C) Copyright 2002-2004, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

package org.cyberneko.html;

import java.io.IOException;
import java.util.Properties;
                           
/**
 * Pre-defined HTML entities.
 *
 * @author Andy Clark
 *
 * @version $Id$
 */
public class HTMLEntities {

    //
    // Constants
    //

    /** Entities. */
    protected static final Properties ENTITIES = new Properties();

    //
    // Static initialization
    //

    static {
        load0("res/HTMLlat1.properties");
        load0("res/HTMLspecial.properties");
        load0("res/HTMLsymbol.properties");
        load0("res/XMLbuiltin.properties");
    }

    //
    // Public static methods
    //

    /** Returns the character associated to the given entity name. */
    public static int get(String name) {
        String value = (String)ENTITIES.get(name);
        return value != null ? value.charAt(0) : -1;
    } // get(String):char

    //
    // Private static methods
    //

    /** Loads the entity values in the specified resource. */
    private static void load0(String filename) {
        try {
            ENTITIES.load(HTMLEntities.class.getResourceAsStream(filename));
        }
        catch (IOException e) {
            System.err.println("error: unable to load resource \""+filename+"\"");
        }
    } // load0(String)

} // class HTMLEntities
