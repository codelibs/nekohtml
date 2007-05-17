/* 
 * (C) Copyright 2004-2005, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

package org.cyberneko.html;

import org.apache.xerces.xni.Augmentations;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * This class is here to overcome the XNI changes to the 
 * <code>Augmentations</code> interface. In early versions of XNI, the
 * augmentations interface contained a <code>clear()</code> method to
 * remove all of the items from the augmentations instance. A later
 * version of XNI changed this method to <code>removeAllItems()</code>.
 * Therefore, this class extends the augmentations interface and
 * explicitly implements both of these methods.
 * <p>
 * <strong>Note:</strong>
 * This code is inspired by performance enhancements submitted by
 * Marc-André Morissette.
 * 
 * @author Andy Clark
 */
public class HTMLAugmentations
    implements Augmentations {

    //
    // Data
    //

    /** Augmentation items. */
    protected Hashtable fItems = new Hashtable();

    //
    // Public methods
    //

    // since Xerces 2.3.0

    /** Removes all of the elements in this augmentations object. */
    public void removeAllItems() {
        fItems.clear();
    } // removeAllItems()

    // from Xerces 2.0.0 (beta4) until 2.3.0

    /** Removes all of the elements in this augmentations object. */
    public void clear() {
        fItems.clear();
    } // clear()

    //
    // Augmentations methods
    //

    /**
     * Add additional information identified by a key to the Augmentations 
     * structure.
     * 
     * @param key    Identifier, can't be <code>null</code>
     * @param item   Additional information
     *
     * @return The previous value of the specified key in the Augmentations 
     *         structure, or <code>null</code> if it did not have one.
     */
    public Object putItem(String key, Object item) {
        return fItems.put(key, item);
    } // putItem(String, Object):Object


    /**
     * Get information identified by a key from the Augmentations structure.
     * 
     * @param key    Identifier, can't be <code>null</code>
     *
     * @return The value to which the key is mapped in the Augmentations 
     *         structure; <code>null</code> if the key is not mapped to any 
     *         value.
     */
    public Object getItem(String key) {
        return fItems.get(key);
    } // getItem(String):Object
    
    /**
     * Remove additional info from the Augmentations structure
     * 
     * @param key    Identifier, can't be <code>null</code>
     * @return The previous value of the specified key in the Augmentations 
     *         structure, or <code>null</code> if it did not have one.
     */
    public Object removeItem(String key) {
        return fItems.remove(key);
    } // removeItem(String):Object

    /**
     * Returns an enumeration of the keys in the Augmentations structure.
     */
    public Enumeration keys() {
        return fItems.keys();
    } // keys():Enumeration

} // class HTMLAugmentations
