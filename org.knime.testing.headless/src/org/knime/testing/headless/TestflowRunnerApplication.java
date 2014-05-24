/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */
package org.knime.testing.headless;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitTaskMirror.JUnitTestRunnerMirror;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.util.FileUtil;
import org.knime.testing.core.AnalyzeLogFile;
import org.knime.testing.core.FullWorkflowTest;
import org.knime.testing.core.KnimeTestRegistry;
import org.knime.testing.core.SimpleWorkflowTest;
import org.knime.testing.core.WorkflowTest;
import org.knime.workbench.repository.RepositoryManager;

import com.knime.enterprise.client.filesystem.util.WorkflowDownloadApplication;

/**
 * This application executes the testflows and writes the results into an XML
 * file identical to the one produced by ANT's &lt;junit> task. This can then be
 * analyzed further.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class TestflowRunnerApplication implements IApplication {
    private boolean m_analyzeLogFile = false;

    private String m_testNamePattern;

    private Collection<File> m_rootDirs = new ArrayList<File>();

    private String m_serverUri;

    private String m_xmlResultFile;

    private String m_xmlResultDir;


    private File m_analyzeOutputDir;

    private boolean m_simpleTests;

    private boolean m_testDialogs;

    private boolean m_testViews;

    private int m_timeout = FullWorkflowTest.TIMEOUT;

    private static Test staticTestSuite;

    /**
     * This method is called by {@link JUnitTestRunner} to retrieve the tests to
     * run.
     *
     * @return a test suite
     */
    public static Test suite() {
        return staticTestSuite;
    }

    @Override
    public Object start(final IApplicationContext context) throws Exception {
        // we need a display, initialized as early as possible, otherwise closing JFrames may result
        // in X errors (BadWindow) under Linux
        PlatformUI.createDisplay();

        // make sure the logfile doesn't get split.
        System.setProperty(KNIMEConstants.PROPERTY_MAX_LOGFILESIZE, "-1");

        // this is to load the repository plug-in
        RepositoryManager.INSTANCE.toString();

        context.applicationRunning();

        Object args =
                context.getArguments()
                        .get(IApplicationContext.APPLICATION_ARGS);

        System.err.println("========================================================================================");
        System.err.println("This application is outdated, consider using org.knime.testing.NGTestflowRunner instead.");
        System.err.println("========================================================================================");

        if (!extractCommandLineArgs(args) || (m_rootDirs.isEmpty() && (m_serverUri == null))
            || ((m_xmlResultFile == null) && (m_xmlResultDir == null))) {
            printUsage();
            return EXIT_OK;
        }
        if (m_xmlResultDir != null) {
            File xmlResultDir = new File(m_xmlResultDir);
            if (!xmlResultDir.exists() && ! xmlResultDir.mkdirs()) {
                throw new IOException("Can not create directory for result files " + m_xmlResultDir);
            }
        } else {
            File xmlResultFile = new File(m_xmlResultFile);
            if (!xmlResultFile.getParentFile().exists() && ! xmlResultFile.getParentFile().mkdirs()) {
                throw new IOException("Can not create directory for results file " + m_xmlResultFile);
            }
        }


        if (m_serverUri != null) {
            m_rootDirs.add(downloadWorkflows());
        }

        KnimeTestRegistry registry = new KnimeTestRegistry(m_testNamePattern, m_rootDirs, null, m_testDialogs,
                m_testViews, m_timeout);
        TestSuite testSuite = registry.collectTestCases(m_simpleTests
                ? SimpleWorkflowTest.factory
                : FullWorkflowTest.factory);

        if (m_xmlResultDir != null) {
            Enumeration<Test> testEnum = testSuite.tests();

            int maxNameLength = 0;
            while (testEnum.hasMoreElements()) {
                WorkflowTest test = (WorkflowTest)testEnum.nextElement();
                maxNameLength = Math.max(maxNameLength, test.getName().length());
            }

            testEnum = testSuite.tests();
            while (testEnum.hasMoreElements()) {
                WorkflowTest test = (WorkflowTest)testEnum.nextElement();
                System.out.printf("=> Running %-" + maxNameLength + "s...", test.getName());
                int ret = runTest(test, new File(m_xmlResultDir, test.getName() + ".xml"));
                switch (ret) {
                    case JUnitTestRunnerMirror.SUCCESS:
                        System.out.println("OK");
                        break;
                    case JUnitTestRunnerMirror.FAILURES:
                        System.out.println("FAILURE");
                        break;
                    case JUnitTestRunnerMirror.ERRORS:
                        System.out.println("ERROR");
                        break;
                    default:
                        System.out.println("UNKNOWN");
                        break;
                }

            }
        } else {
            runTest(testSuite, new File(m_xmlResultFile));
        }

        // TODO add analysis of log file via junit tests
        if (m_analyzeLogFile) {
            analyzeLogFile();
        }
        return EXIT_OK;
    }

    private boolean analyzeLogFile() {

        if (m_analyzeOutputDir != null) {
            // create the output folder, if it doesn't exist
            if (!m_analyzeOutputDir.exists()) {
                boolean success = m_analyzeOutputDir.mkdirs();
                if (!success) {
                    System.err.println("Couldn't create output dir "
                            + "for analysis results. "
                            + "Using Java temp dir instead!");
                    m_analyzeOutputDir = null;
                }
            } else {
                // output dir exists - make sure its a dir and not a file
                if (!m_analyzeOutputDir.isDirectory()) {
                    System.err.println("Specified output location is not"
                            + "a directory. Please specify a directory!");
                    return false; // exit!
                }
            }
        }

        File logfile =
                new File(KNIMEConstants.getKNIMEHomeDir(), NodeLogger.LOG_FILE);

        try {
            // extract the tail of the logfile that contains the last run:
            logfile = extractLastTestRun(logfile);
            new AnalyzeLogFile(logfile, m_analyzeOutputDir);
            return true;

        } catch (IOException ioe) {
            System.err.println("Couldn't access logfile! (in "
                    + logfile.getAbsolutePath() + ")");
            ioe.printStackTrace();
            return false;
        }
    }

    /**
     * Copies the part of the specified log file that contains the log from the
     * last (possibly still running) KNIME run.
     *
     * @param logFile the log file to analyze and copy the last run from
     * @return a file in the same dir as the specified log file containing the
     *         last run of the specified file.
     */
    private File extractLastTestRun(final File logFile) throws IOException {

        final String startLine = "# Welcome to KNIME";

        File copyFile =
                new File(logFile.getParent(), "KNIMELastRunLogCopy.log");

        BufferedReader reader =
            new BufferedReader(new InputStreamReader(new FileInputStream(logFile), Charset.forName("UTF-8")));
        String line;
        BufferedWriter writer =
            new BufferedWriter(new OutputStreamWriter(new FileOutputStream(copyFile), Charset.forName("UTF-8")));

        while ((line = reader.readLine()) != null) {
            if (line.contains(startLine) && line.endsWith("#")) {
                // (re-) open the output file, overriding any previous content
                writer =
                    new BufferedWriter(new OutputStreamWriter(new FileOutputStream(copyFile), Charset.forName("UTF-8")));
            }
            writer.write(line + "\n");
        }
        reader.close();
        writer.close();

        return copyFile;
    }

    /**
     * Extracts from the passed object the arguments. Returns true if everything
     * went smooth, false if the application must exit.
     *
     * @param args the object with the command line arguments.
     * @return true if the members were set according to the command line
     *         arguments, false, if an error message was printed and the
     *         application must exit.
     */
    private boolean extractCommandLineArgs(final Object args) {
        String[] stringArgs;
        if (args instanceof String[]) {
            stringArgs = (String[])args;
            if (stringArgs.length > 0 && stringArgs[0].equals("-pdelaunch")) {
                String[] copy = new String[stringArgs.length - 1];
                System.arraycopy(stringArgs, 1, copy, 0, copy.length);
                stringArgs = copy;
            }
        } else if (args != null) {
            System.err.println("Unable to read application's arguments."
                    + " (was expecting a String array, but got a "
                    + args.getClass().getName() + ". toString() returns '"
                    + args.toString() + "')");
            return false;
        } else {
            stringArgs = new String[0];
        }

        int i = 0;
        while (i < stringArgs.length) {
            // the "-pattern" argument sets the test name pattern (reg exp)
            if ((stringArgs[i] != null) && stringArgs[i].equals("-pattern")) {
                if (m_testNamePattern != null) {
                    System.err.println("You can't specify multiple patterns"
                            + " at the command line");
                    return false;
                }
                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null)
                        || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing pattern for tests to run.");
                    printUsage();
                    return false;
                }
                m_testNamePattern = stringArgs[i++];
                continue;
            }

            // "-analyze" triggers analysis of log file after running the test
            if ((stringArgs[i] != null) && stringArgs[i].equals("-analyze")) {
                if (m_analyzeLogFile) {
                    System.err.println("You can't specify multiple -analyze "
                            + "options at the command line");
                    return false;
                }
                if (m_simpleTests) {
                    System.err.println("-analyze and -simple cannot be used "
                            + "together");
                    return false;
                }

                i++;
                m_analyzeLogFile = true;

                if ((i < stringArgs.length) && (stringArgs[i] != null)
                        && (stringArgs[i].length() > 0)
                        && (stringArgs[i].charAt(0) != '-')) {
                    // if the next argument is not an option, use it as path
                    m_analyzeOutputDir = new File(stringArgs[i++]);
                }

                continue;
            }

            // "-root" specifies the root dir of all testcases
            if ((stringArgs[i] != null) && stringArgs[i].equals("-root")) {
                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null)
                        || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <dir_name> for option -root.");
                    printUsage();
                    return false;
                }
                m_rootDirs.add(new File(stringArgs[i++]));
                continue;
            }

            // "-server" specifies a workflow group on a server
            if ((stringArgs[i] != null) && stringArgs[i].equals("-server")) {
                if (m_serverUri != null) {
                    System.err.println("You can't specify multiple -server "
                            + "options at the command line");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null)
                        || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <url> for option -server.");
                    printUsage();
                    return false;
                }
                m_serverUri = stringArgs[i++];
                continue;
            }

            // "-xmlResult" specifies the result file
            if ((stringArgs[i] != null) && stringArgs[i].equals("-xmlResult")) {
                if (m_xmlResultFile != null) {
                    System.err.println("You can't specify multiple -xmlResult "
                            + "options at the command line");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null)
                        || (stringArgs[i].length() == 0)) {
                    System.err
                            .println("Missing <file_name> for option -xmlResult.");
                    printUsage();
                    return false;
                }
                m_xmlResultFile = stringArgs[i++];
                continue;
            }

            // "-xmlResultDir" specifies the result directory
            if ((stringArgs[i] != null) && stringArgs[i].equals("-xmlResultDir")) {
                if (m_xmlResultDir != null) {
                    System.err.println("You can't specify multiple -xmlResultDir options at the command line");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <directory_name> for option -xmlResultDir.");
                    printUsage();
                    return false;
                }
                m_xmlResultDir = stringArgs[i++];
                continue;
            }

            if ((stringArgs[i] != null) && stringArgs[i].equals("-timeout")) {
                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null)
                        || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <seconds> for option -timeout.");
                    printUsage();
                    return false;
                }
                m_timeout = Integer.parseInt(stringArgs[i++]);
                continue;
            }

            if ((stringArgs[i] != null) && stringArgs[i].equals("-simple")) {
                if (m_analyzeLogFile) {
                    System.err.println("-analyze and -simple cannot be used "
                            + "together");
                    return false;
                }

                m_simpleTests = true;
                i++;
                continue;
            }

            if ((stringArgs[i] != null) && stringArgs[i].equals("-dialogs")) {
                m_testDialogs = true;
                i++;
                continue;
            }

            if ((stringArgs[i] != null) && stringArgs[i].equals("-views")) {
                m_testViews = true;
                i++;
                continue;
            }

            System.err.println("Invalid option: '" + stringArgs[i] + "'\n");
            printUsage();
            return false;
        }

        if (m_testNamePattern == null) {
            m_testNamePattern = ".+";
        }

        return true;
    }

    private void printUsage() {
        System.err.println("Valid arguments:");

        System.err.println("    -pattern <reg_exp>: "
                + "only test matching <reg_exp> will be run.");
        System.err.println("    -root <dir_name>: optional, specifies the"
                + " root dir where all testcases are located in. Multiple "
                + " root arguments may be present.");
        System.err.println("    -server <uri>: optional, a KNIME server "
                + "from which workflows should be downloaded first.");
        System.err.println("                   Example: "
                + "knimefs://<user>:<password>@host[:port]/workflowGroup1");
        System.err.println("    -analyze <dir_name>: optional, "
                + "analyzes the log file after the run.");
        System.err.println("                         The result files will "
                + "be placed in a directory in the " + "specified dir.");
        System.err.println("                         If "
                + "<dir_name> is omitted the Java temp dir is used.");
        System.err.println("    -xmlResult <file_name>: specifies a single XML "
                + " file where the test results are written to.");
        System.err.println("    -xmlResultDir <directory_name>: specifies the directory "
                + " into which each test result is written to as an XML files.");
        System.err.println("    -dialogs: additional tests all node dialogs.");
        System.err.println("    -views: opens all views during a workflow test.");
        System.err.println("    -timeout <seconds>: optional, specifies the timeout for each individual workflow.");
        System.err.println("    -simple: only checks if all nodes are "
                + " executed in the end.");
    }

    @Override
    public void stop() {
    }


    private int runTest(final Test test, final File resultFile) throws IOException {
        staticTestSuite = test;
        JUnitTest junitTest = new JUnitTest(TestflowRunnerApplication.class.getName());

        final JUnitTestRunner runner =
            new JUnitTestRunner(junitTest, false, false, false, this.getClass().getClassLoader());
        XMLJUnitResultFormatter formatter = new XMLJUnitResultFormatter();
        OutputStream out = new FileOutputStream(resultFile);
        formatter.setOutput(out);
        runner.addFormatter(formatter);

        Writer stdout = new Writer() {
            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                runner.handleOutput(new String(cbuf, off, len));
            }

            @Override
            public void flush() throws IOException {
            }

            @Override
            public void close() throws IOException {
            }
        };
        Writer stderr = new Writer() {

            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                runner.handleErrorOutput(new String(cbuf, off, len));
            }

            @Override
            public void flush() throws IOException {
            }

            @Override
            public void close() throws IOException {
            }
        };

        NodeLogger.addWriter(stdout, LEVEL.DEBUG, LEVEL.FATAL);
        NodeLogger.addWriter(stderr, LEVEL.ERROR, LEVEL.FATAL);
        runner.run();
        NodeLogger.removeWriter(stderr);
        NodeLogger.removeWriter(stdout);

        out.close();
        return runner.getRetCode();
    }


    private File downloadWorkflows() throws IOException, CoreException, URISyntaxException {
        File tempDir = FileUtil.createTempDir("KNIME Testflow");
        WorkflowDownloadApplication.downloadWorkflows(m_serverUri, tempDir);
        return tempDir;
    }
}
