/* 
 * Copyright 2002-2008 Andy Clark
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

package org.cyberneko.html;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.StringTokenizer;

import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;

/**
 * A simple regression tester.<br/>
 * This isn't written as Ant task anymore to avoid collision between the Xerces version used by Ant
 * and the one we want to test NekoHTML with.<br/>
 * It generates canonical output using the <code>Writer</code> class
 * and compares it against the expected canonical output. Simple
 * as that.
 *
 * @author Andy Clark
 * @author Marc Guillemot
 */
public class Tester
{
    private File fCanonicalDir; // Canonical test directory.
    private File fOutputDir; // Output directory for generated files. 
    private File[] testFiles; // the files to run the tests on
    private boolean fDebug = false;

    //
    // Public methods
    //

    /** Sets the canonical test directory. */
    public void setCanonDir(final File canondir) {
        fCanonicalDir = canondir;
    } // setCanonDir(String)

    /** Sets the output directory for generated files. */
    public void setOutputDir(final File outdir) {
        fOutputDir = outdir;
    } // setOutputDir(String)

    //
    // Task methods
    //
    
    public static void main(String[] args) throws Exception
    {
		if (args.length < 3) {
			throw new RuntimeException("Bad number of parameters!\n"
					+ "Expected: input_dir outputDir canonicalDir\n" 
					+ "Example:\n"
					+ "data build/data/output data/canonical");
		}
		
		final File inputDir = new File(args[0]);
		final FilenameFilter filter = new FilenameFilter() {
			public boolean accept(final File dir, final String name) {
				// JDK1.3 doesn't know regular expressions!
				return name.startsWith("test") && name.endsWith(".html");
			}
		};
		
		final Tester tester = new Tester();
		tester.setInputFiles(inputDir.listFiles(filter));
		tester.setOutputDir(new File(args[1]));
		tester.setCanonDir(new File(args[2]));
		if (args.length >= 4 && "true".equals(args[3]))
			tester.setDebug(true);
		
		tester.execute();
	}

    private void setDebug(boolean b) {
		fDebug = b;
	}

	private void setInputFiles(File[] listFiles) {
		testFiles = listFiles;
	}

	/** Performs the test. */
    public void execute() throws Exception {

        // parse input files and produce output files
        int size = testFiles.length;
        info("Parsing " + size + " test files and generating output...");
        parseFiles(testFiles);

        // compare against canonical output
        log("Comparing parsed output against canonical output...");
        int errors = 0;
        for (int i = 0; i < size; i++) 
        {
        	final File file = testFiles[i];
        	final String fileName = file.getName();
            final File canonfile = new File(fCanonicalDir, fileName);
            if (!canonfile.exists()) {
                errors++;
                log("  canonical file missing, " + canonfile);
                continue;
            }
            final File outfile = new File(fOutputDir, fileName);
            if (!outfile.exists()) {
                errors++;
                log("  output file missing, " + outfile);
                continue;
            }
            debug("  comparing "+canonfile+" and "+outfile);
            try {
                if (compare(canonfile, outfile)) {
                    errors++;
                }
            }
            catch (IOException e) {
                errors++;
                log("i/o error");  
            }
        }

        // finished
        if (errors > 0) {
        	final String msg = "Finished with " + errors + " errors.";
            log(msg);
            throw new Exception(msg);
        }
        info("Done ");

    } // execute()

	private void info(String string) {
		System.out.println(string);
	}

	private void debug(String string) {
		if (fDebug)
			log(string);
	}

	private void log(String string) {
		if (fDebug)
			System.out.println(string);
	}

	private void parseFiles(final File[] files) 
	{
		for (int i = 0; i < files.length; i++) 
		{
		    final File infile = files[i];
		    final File outfile = new File(fOutputDir, infile.getName());
		    parseFile(infile, outfile);
		}
	}

	private void parseFile(final File infile, final File outfile) {
		log("Parsing " + infile + " and generating " + outfile);
		debug("  " + outfile);
		OutputStream out = null;
		try {
		    // create filters
		    out = new FileOutputStream(outfile);
		    XMLDocumentFilter[] filters = { new Writer(out) };

		    // create parser
		    XMLParserConfiguration parser = new HTMLConfiguration();

		    // parser settings
		    parser.setProperty("http://cyberneko.org/html/properties/filters", filters);
		    String infilename = infile.toString();
		    File insettings = new File(infilename+".settings");
		    if (insettings.exists()) {
		        BufferedReader settings = new BufferedReader(new FileReader(insettings));
		        String settingline;
		        while ((settingline = settings.readLine()) != null) {
		            StringTokenizer tokenizer = new StringTokenizer(settingline);
		            String type = tokenizer.nextToken();
		            String id = tokenizer.nextToken();
		            String value = tokenizer.nextToken();
		            if (type.equals("feature")) {
		                parser.setFeature(id, value.equals("true"));
		            }
		            else {
		                parser.setProperty(id, value);
		            }
		        }
		        settings.close();
		    }

		    // parse
		    parser.parse(new XMLInputSource(null, infilename, null));
		}
		catch (Exception e) {
		    log("  error parsing input file, "+infile);
		    throw new RuntimeException(e.getMessage());
		}
		finally {
		    try {
		        out.close();
		    }
		    catch (Exception e) {
		        log("  error closing output file, "+outfile);
		        throw new RuntimeException(e.getMessage());
		    }
		}
	}

	//
    // Protected methods
    //

    /** Compares two files. */
    protected boolean compare(File f1, File f2) throws IOException {
        BufferedReader i1 = new BufferedReader(new InputStreamReader(new UTF8BOMSkipper(new FileInputStream(f1)), "UTF8"));
        BufferedReader i2 = new BufferedReader(new InputStreamReader(new FileInputStream(f2), "UTF8"));
        String l1;
        String l2;
        int errors = 0;
        long n = 0;
        while ((l1 = i1.readLine()) != null) {
            n++;
            if ((l2 = i2.readLine()) == null) {
                errors++;
                log("  file lengths don't match ("+f1+")");
                break;
            }
            if (compare(f1.getName(), n, l1, l2)) {
                errors++;
                break;
            }
        }
        if (errors == 0 && (l2 = i2.readLine()) != null) {
            errors++;
            log("  file lengths don't match ("+f1+")");
        }
        i1.close();
        i2.close();
        return errors > 0;
    } // compare(File,File):boolean

    /** Compares two strings. */
    protected boolean compare(String f, long n, String s1, String s2) {
        int l1 = s1.length();
        int l2 = s2.length();
        boolean error = false;
        if (l1 < l2) {
            error = true;
            log("  "+f+':'+n+" output string too long");
        }
        else if (l1 > l2) {
            error = true;
            log("  "+f+':'+n+" output string too short");
        }
        else if (!s1.equals(s2)) {
            error = true;
            log("  "+f+':'+n+" strings don't match");
        }
        if (error) {
            log("    [in:  "+s1+']');
            log("    [out: "+s2+']');
        }
        return error;
    } // compare(String,long,String,String):boolean

} // class Tester
