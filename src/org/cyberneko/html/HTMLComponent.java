/* 
 * (C) Copyright 2002-2005, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */

package org.cyberneko.html;

import org.apache.xerces.xni.parser.XMLComponent;

/**
 * This interface extends the XNI <code>XMLComponent</code> interface
 * to add methods that allow the preferred default values for features
 * and properties to be queried.
 *
 * @author Andy Clark
 *
 * @version $Id: HTMLComponent.java,v 1.4 2005/02/14 03:56:54 andyc Exp $
 */
public interface HTMLComponent 
    extends XMLComponent {

    //
    // HTMLComponent methods
    //

    /** 
     * Returns the default state for a feature, or null if this
     * component does not want to report a default value for this
     * feature.
     */
    public Boolean getFeatureDefault(String featureId);

    /** 
     * Returns the default state for a property, or null if this
     * component does not want to report a default value for this
     * property. 
     */
    public Object getPropertyDefault(String propertyId);

} // interface HTMLComponent
