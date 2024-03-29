/* 
 * Copyright 2002-2009 Andy Clark, Marc Guillemot
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codelibs.nekohtml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.codelibs.nekohtml.HTMLConfiguration;
import org.codelibs.nekohtml.HTMLScanner;
import org.codelibs.nekohtml.xercesbridge.XercesBridge;

/**
 * This test generates canonical result using the <code>Writer</code> class
 * and compares it against the expected canonical output. Simple as that.
 *
 * @author Andy Clark
 * @author Marc Guillemot
 * @author Ahmed Ashour
 */
public class CanonicalTest extends TestCase {

    private static final File canonicalDir = new File("src/test/resources/data/canonical");
    private static final File outputDir = new File("target/build/data/output/" + XercesBridge.getInstance().getVersion());
    private File dataFile;

    public static Test suite() throws Exception {
        outputDir.mkdirs();

        TestSuite suite = new TestSuite();
        final List/*File*/dataFiles = new ArrayList();
        File dataDir = new File("src/test/resources/data");
        dataDir.listFiles(new FileFilter() {
            public boolean accept(final File file) {
                String name = file.getName();
                if (file.isDirectory() && !"canonical".equals(name)) {
                    file.listFiles(this);
                } else if (name.startsWith("test") && name.endsWith(".html")) {
                    dataFiles.add(file);
                }
                return false; // we don't care to listFiles' result
            }
        });
        Collections.sort(dataFiles);

        for (Object file : dataFiles) {
            suite.addTest(new CanonicalTest((File) file));
        }
        return suite;
    }

    CanonicalTest(final File dataFile) throws Exception {
        super(dataFile.getName() + " [" + XercesBridge.getInstance().getVersion() + "]");
        this.dataFile = dataFile;
    }

    protected void runTest() throws Exception {
        final String dataLines = getResult(dataFile);
        try {
            // prepare for future changes where canonical files are next to test file
            File canonicalFile = new File(dataFile.getParentFile(), dataFile.getName() + ".canonical");
            if (!canonicalFile.exists()) {
                canonicalFile = new File(canonicalDir, dataFile.getName());
            }
            if (!canonicalFile.exists()) {
                fail("Canonical file not found: " + canonicalFile.getAbsolutePath() + ": " + dataLines);
            }
            final String canonicalLines = getCanonical(canonicalFile);

            assertEquals(canonicalLines, dataLines);
        } catch (final AssertionFailedError e) {
            final File output = new File(outputDir, dataFile.getName());
            final PrintWriter pw = new PrintWriter(new FileOutputStream(output));
            pw.print(dataLines);
            pw.close();

            throw e;
        }
    }

    private String getCanonical(final File infile) throws IOException {
        final BufferedReader reader =
                new BufferedReader(new InputStreamReader(new UTF8BOMSkipper(new FileInputStream(infile)), StandardCharsets.UTF_8));
        final StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    private String getResult(final File infile) throws IOException {

        final StringBuilder sb = new StringBuilder();
        try (StringWriter out = new StringWriter()) {
            // create filters
            XMLDocumentFilter[] filters = { new Writer(out) };

            // create parser
            XMLParserConfiguration parser = new HTMLConfiguration();

            // parser settings
            parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
            final String infilename = infile.toString();
            final File insettings = new File(infile.toString() + ".settings");
            if (insettings.exists()) {
                readSettings(insettings, parser, out);
            }

            // parse
            parser.parse(new XMLInputSource(null, infilename, null));

            // make nice output
            addNewLines(out, sb);
        }
        return sb.toString();
    }

    private static void addNewLines(StringWriter out, StringBuilder sb) throws IOException {
        try (final BufferedReader reader = new BufferedReader(new StringReader(out.toString()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
    }

    private static void readSettings(File insettings, XMLParserConfiguration parser, StringWriter out) throws IOException {
        try (BufferedReader settings = new BufferedReader(new FileReader(insettings))) {
            String settingline;
            while ((settingline = settings.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(settingline);
                String type = tokenizer.nextToken();
                String id = tokenizer.nextToken();
                String value = tokenizer.nextToken();
                if ("feature".equals(type)) {
                    parser.setFeature(id, "true".equals(value));
                    if (HTMLScanner.REPORT_ERRORS.equals(id)) {
                        parser.setErrorHandler(new HTMLErrorHandler(out));
                    }
                } else {
                    parser.setProperty(id, value);
                }
            }
        }
    }
}
