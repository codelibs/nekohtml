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
package org.codelibs.nekohtml.parsers;

import org.codelibs.nekohtml.sax.HTMLSAXParser;

/**
 * A SAX parser for HTML documents.
 * This is a simple wrapper around HTMLSAXParser for backward compatibility.
 *
 * @author CodeLibs Project
 */
public class SAXParser extends HTMLSAXParser {

    /**
     * Default constructor.
     */
    public SAXParser() {
        super();
    }

} // class SAXParser
