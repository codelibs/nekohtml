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

import java.util.HashMap;
import java.util.Map;

/**
 * Container for additional information about parsing events.
 * This is a SAX-compatible replacement for Xerces XNI Augmentations.
 *
 * @author CodeLibs Project
 */
public class HTMLAugmentations {

    /** The augmentation data. */
    private Map<String, Object> data;

    /**
     * Default constructor.
     */
    public HTMLAugmentations() {
        // Lazy initialization of map
    }

    /**
     * Adds an augmentation item.
     *
     * @param key The augmentation key
     * @param item The augmentation value
     * @return The previous value associated with the key, or null
     */
    public Object putItem(final String key, final Object item) {
        if (data == null) {
            data = new HashMap<>();
        }
        return data.put(key, item);
    }

    /**
     * Gets an augmentation item.
     *
     * @param key The augmentation key
     * @return The augmentation value, or null if not found
     */
    public Object getItem(final String key) {
        return data != null ? data.get(key) : null;
    }

    /**
     * Removes an augmentation item.
     *
     * @param key The augmentation key
     * @return The removed value, or null if not found
     */
    public Object removeItem(final String key) {
        return data != null ? data.remove(key) : null;
    }

    /**
     * Removes all augmentation items.
     */
    public void removeAllItems() {
        if (data != null) {
            data.clear();
        }
    }

    /**
     * Checks if an augmentation item exists.
     *
     * @param key The augmentation key
     * @return true if the item exists, false otherwise
     */
    public boolean containsItem(final String key) {
        return data != null && data.containsKey(key);
    }

    @Override
    public String toString() {
        return data != null ? data.toString() : "{}";
    }

} // class HTMLAugmentations
