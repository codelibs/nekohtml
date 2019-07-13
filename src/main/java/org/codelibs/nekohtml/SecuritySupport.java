/*
 * Copyright 2002-2008 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codelibs.nekohtml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * This class is duplicated for each JAXP subpackage so keep it in sync.
 * It is package private and therefore is not exposed as part of the JAXP
 * API.
 *
 */
class SecuritySupport {

    /*
     * Make this of type Object so that the verifier won't try to
     * prove its type, thus possibly trying to load the SecuritySupport12
     * class.
     */
    private static final SecuritySupport securitySupport = new SecuritySupport();

    static SecuritySupport getInstance() {
        return securitySupport;
    }

    ClassLoader getContextClassLoader() {
        return (ClassLoader) AccessController.doPrivileged((PrivilegedAction) () -> {
            ClassLoader cl = null;
            try {
                cl = Thread.currentThread().getContextClassLoader();
            } catch (final SecurityException ex) {}
            return cl;
        });
    }

    ClassLoader getSystemClassLoader() {
        return (ClassLoader) AccessController.doPrivileged((PrivilegedAction) () -> {
            ClassLoader cl = null;
            try {
                cl = ClassLoader.getSystemClassLoader();
            } catch (final SecurityException ex) {}
            return cl;
        });
    }

    ClassLoader getParentClassLoader(final ClassLoader cl) {
        return (ClassLoader) AccessController.doPrivileged((PrivilegedAction) () -> {
            ClassLoader parent = null;
            try {
                parent = cl.getParent();
            } catch (final SecurityException ex) {}

            // eliminate loops in case of the boot
            // ClassLoader returning itself as a parent
                return (parent == cl) ? null : parent;
            });
    }

    String getSystemProperty(final String propName) {
        return (String) AccessController.doPrivileged((PrivilegedAction) () -> System.getProperty(propName));
    }

    FileInputStream getFileInputStream(final File file) throws FileNotFoundException {
        try {
            return (FileInputStream) AccessController.doPrivileged((PrivilegedExceptionAction) () -> new FileInputStream(file));
        } catch (final PrivilegedActionException e) {
            throw (FileNotFoundException) e.getException();
        }
    }

    InputStream getResourceAsStream(final ClassLoader cl, final String name) {
        return (InputStream) AccessController.doPrivileged((PrivilegedAction) () -> {
            InputStream ris;
            if (cl == null) {
                ris = ClassLoader.getSystemResourceAsStream(name);
            } else {
                ris = cl.getResourceAsStream(name);
            }
            return ris;
        });
    }

    boolean getFileExists(final File f) {
        return ((Boolean) AccessController.doPrivileged((PrivilegedAction) () -> Boolean.valueOf(f.exists()))).booleanValue();
    }

    long getLastModified(final File f) {
        return ((Long) AccessController.doPrivileged((PrivilegedAction) () -> Long.valueOf(f.lastModified()))).longValue();
    }

}
