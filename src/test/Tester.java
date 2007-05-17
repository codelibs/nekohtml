/* 
 * (C) Copyright 2002-2005, Andy Clark.  All rights reserved.
 *
 * This file is distributed under an Apache style license. Please
 * refer to the LICENSE file for specific details.
 */
 
package test;

import org.cyberneko.html.HTMLConfiguration;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;

/**
 * A simple regression tester written as an Ant task. This task
 * generates canonical output using the <code>Writer</code> class
 * and compares it against the expected canonical output. Simple
 * as that.
 *
 * @author Andy Clark
 */
public class Tester
    extends Task {

    //
    // Data
    //

    /** Canonical test directory. */
    protected String fCanonicalDir;

    /** Output directory for generated files. */
    protected String fOutputDir;

    /** List of test filesets. */
    protected Vector fFileSets = new Vector();

    //
    // Public methods
    //

    /** Sets the canonical test directory. */
    public void setCanonDir(String canondir) {
        fCanonicalDir = canondir;
    } // setCanonDir(String)

    /** Sets the output directory for generated files. */
    public void setOutputDir(String outdir) {
        fOutputDir = outdir;
    } // setOutputDir(String)

    /** Adds a fileset to the list of test filesets. */
    public void addFileSet(FileSet fileset) {
        fFileSets.addElement(fileset);
    } // addFileSet(FileSet)

    //
    // Task methods
    //

    /** Performs the test. */
    public void execute() throws BuildException {

        // check params
        String canonicaldir = fCanonicalDir;
        if (canonicaldir == null) {
            canonicaldir = ".";
            log("Canonical directory not specified. Assuming current directory.",
                Project.MSG_WARN);
        }
        String outputdir = fOutputDir;
        if (outputdir == null) {
            outputdir = ".";
            log("Output directory not specified. Assuming current directory.",
                Project.MSG_WARN);
        }
        if (fFileSets.size() == 0) {
            throw new BuildException("must specify at least one fileset");
        }

        // parse input files and produce output files
        log("Parsing test files and generating output...");
        File outdir = new File(outputdir);
        int size = fFileSets.size();
        for (int i = 0; i < size; i++) {
            FileSet fileset = (FileSet)fFileSets.elementAt(i);
            DirectoryScanner dirscanner = fileset.getDirectoryScanner(project);
            File indir = dirscanner.getBasedir();
            String[] files = dirscanner.getIncludedFiles();
            for (int j = 0; j < files.length; j++) {
                File infile = new File(indir, files[j]);
                File outfile = new File(outdir, files[j]);
                log("  "+outfile, Project.MSG_VERBOSE);
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
                    throw new BuildException(e);
                }
                finally {
                    try {
                        out.close();
                    }
                    catch (Exception e) {
                        log("  error closing output file, "+outfile);
                        throw new BuildException(e);
                    }
                }
            }
        }

        // compare against canonical output
        log("Comparing parsed output against canonical output...");
        File canondir = new File(canonicaldir);
        int errors = 0;
        for (int i = 0; i < size; i++) {
            FileSet fileset = (FileSet)fFileSets.elementAt(i);
            DirectoryScanner dirscanner = fileset.getDirectoryScanner(project);
            File indir = dirscanner.getBasedir();
            String[] files = dirscanner.getIncludedFiles();
            for (int j = 0; j < files.length; j++) {
                File canonfile = new File(canondir, files[j]);
                if (!canonfile.exists()) {
                    errors++;
                    log("  canonical file missing, "+canonfile);
                    continue;
                }
                File outfile = new File(outdir, files[j]);
                if (!outfile.exists()) {
                    errors++;
                    log("  output file missing, "+outfile);
                    continue;
                }
                log("  comparing "+canonfile+" and "+outfile, Project.MSG_VERBOSE);
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
        }

        // finished
        if (errors > 0) {
            log("Finished with errors.");
            throw new BuildException();
        }
        log("Done.");

    } // execute()

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
