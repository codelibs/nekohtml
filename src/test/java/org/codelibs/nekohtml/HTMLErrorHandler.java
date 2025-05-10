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
package org.codelibs.nekohtml;

import java.io.IOException;

import org.apache.xerces.util.DefaultErrorHandler;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLParseException;

/**
 * Error handler for test purposes: just logs the errors to the provided PrintWriter.
 * @author Marc Guillemot
 */
class HTMLErrorHandler extends DefaultErrorHandler {
    private final java.io.Writer out_;

    public HTMLErrorHandler(final java.io.Writer out) {
        out_ = out;
    }

    /** @see DefaultErrorHandler#error(String,String,XMLParseException) */
    public void error(final String domain, final String key, final XMLParseException exception) throws XNIException {
        println("Err", key, exception);
    }

    private void println(final String type, String key, XMLParseException exception) throws XNIException {
        try {
            out_.append("[").append(type).append("] ").append(key).append(" ").append(exception.getMessage()).append("\n");
        } catch (final IOException e) {
            throw new XNIException(e);
        }
    }

    /** @see DefaultErrorHandler#warning(String,String,XMLParseException) */
    public void warning(final String domain, final String key, final XMLParseException exception) throws XNIException {
        println("Warn", key, exception);
    }
}
