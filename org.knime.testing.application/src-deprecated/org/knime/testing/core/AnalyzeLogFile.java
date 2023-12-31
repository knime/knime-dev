/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
package org.knime.testing.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import javax.swing.JFileChooser;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.util.FileUtil;

/**
 * Analyzes a log file of a previous regression run. Log file should only
 * contain the log of only one run. It will create a subdir in the java temp dir
 * (with a timestamp from the log file) and stores a bunch of files in there
 * with the output of failing tests and a summary file and stuff. Each file
 * contains the email address of the failing regression test owner in the first
 * line.
 *
 * @author ohl, University of Konstanz
 * @deprecated the new testing framework (in <tt>org.knime.testing.core.ng</tt>) analyzes the log output on-the-fly
 */
@Deprecated
@SuppressWarnings({"squid:S106"})
public class AnalyzeLogFile {
    // this pattern in the log file indicates a starting test log
    private static final String TEST_START_CODE = "<Start> Test='";

    // this pattern in the log file indicates the end of a test log
    private static final String TEST_END_CODE = "<End> Test='";

    private static final String CRLF = "\r\n";

    private int m_numOfTestsRun = 0;

    private int m_numOfFailingTests = 0;

    // directory all temporary files go in.
    private File m_tmpDir;

    private final String m_startTime;

    private Writer m_summaryFailingTests;

    private Writer m_summarySucceedingTests;

    /**
     * Constructor.
     *
     * @param logFile the log file to analyze.
     * @param outputDir a directory in which a dir will be created containing
     *            the results. If null, the java default temp dir will be used.
     * @throws FileNotFoundException if it couldn't find the log file.
     * @throws IOException if something went wrong writing the files.
     */
    public AnalyzeLogFile(final File logFile, final File outputDir) throws IOException {
        if ((logFile == null) || (!logFile.exists())) {
            throw new IllegalArgumentException("You must specify an existing"
                    + " LogFile to analyze.");
        }

        // create the temp dir. Contains reg run start time in its name.
        m_startTime = extractTimestamp(logFile);
        if (outputDir == null) {
            m_tmpDir = File.createTempFile("foo", null).getParentFile();
        } else if (!outputDir.isDirectory()) {
            System.out.println("Specified output is not a directory!"
                    + " Using Java Temp dir instead!");
            m_tmpDir = File.createTempFile("foo", null).getParentFile();
        } else {
            m_tmpDir = outputDir;
        }
        m_tmpDir = new File(m_tmpDir, "RegRunAnalyze_" + m_startTime);
        if (m_tmpDir.exists()) {
            if (!m_tmpDir.isDirectory()) {
                throw new IOException("The output dir can't be created. "
                        + "A file with the same name exists (" + m_tmpDir
                        + ").");
            }
            System.out.println("Output dir exists. "
                    + "Overriding previous results");
        } else {
            if (!m_tmpDir.mkdirs()) {
                throw new IOException("Couldn't create dir for result files ("
                        + m_tmpDir + ").");
            }
        }

        // copy log file into result dir
        String copyName = logFile.getName();
        if (copyName.endsWith(".log") && (copyName.length() > 4)) {
            copyName =
                    "_" + copyName.substring(0, copyName.length() - 4)
                            + m_startTime + ".log";
        } else {
            copyName = "_" + copyName + m_startTime;
        }
        File logCopy = new File(m_tmpDir, copyName);
        FileUtil.copy(logFile, logCopy);

        // global file writer. All methods write in there.
        File failTests =
                new File(m_tmpDir, "SummaryFailingTests_" + m_startTime
                        + ".txt");
        m_summaryFailingTests = new OutputStreamWriter(new FileOutputStream(failTests), StandardCharsets.UTF_8);

        // global file writer. All methods write in there.
        File goodTests =
                new File(m_tmpDir, "SummarySucceedingTests_" + m_startTime
                        + ".txt");
        m_summarySucceedingTests = new OutputStreamWriter(new FileOutputStream(goodTests), StandardCharsets.UTF_8);

        checkWorkbenchInit(logCopy);

        extractFailingTests(logCopy);

        // close them for the createSummary method.
        m_summaryFailingTests.close();
        m_summarySucceedingTests.close();

        try (BufferedReader negReader =
                new BufferedReader(new InputStreamReader(new FileInputStream(failTests), StandardCharsets.UTF_8));
        BufferedReader posReader =
                new BufferedReader(new InputStreamReader(new FileInputStream(goodTests), StandardCharsets.UTF_8))) {
            // combines the two summary files
            createSummary(negReader, posReader);
        }

        System.out.println("Results are in '" + m_tmpDir.getAbsolutePath()
                + "'.");

        failTests.delete(); // should be contained in the overallsummary file
        goodTests.delete(); // should be contained in the overallsummary file
    }

    private void createSummary(final BufferedReader failTests,
            final BufferedReader goodTests) throws IOException {
        File summaryFile = new File(m_tmpDir, "_Summary_" + m_startTime + ".txt");
        try (Writer summary = new OutputStreamWriter(new FileOutputStream(summaryFile), StandardCharsets.UTF_8)) {
            int posTests = m_numOfTestsRun - m_numOfFailingTests;
            int posRate = (int)Math.floor((posTests * 100.0) / m_numOfTestsRun);

            summary.write("Regression run on " + m_startTime + CRLF);
            summary.write("Tests run: " + m_numOfTestsRun + ", failing: "
                    + m_numOfFailingTests + ", succeeding: " + posTests + CRLF);
            summary.write("Success rate: " + posRate + "%" + CRLF);
            summary.write(CRLF);
            summary.write("--------------------------------------");
            summary.write("--------------------------------------" + CRLF);
            summary.write("--------------------------------------");
            summary.write("--------------------------------------" + CRLF);
            summary
                    .write("Failing tests: (see individual logs for details)"
                            + CRLF);
            summary.write("--------------------------------------");
            summary.write("--------------------------------------" + CRLF);
            summary.write("--------------------------------------");
            summary.write("--------------------------------------" + CRLF);

            // copy the failing tests summary
            String line;
            int cnt = 0;
            while ((line = failTests.readLine()) != null) {
                summary.write(line + CRLF);
                cnt++;
            }
            if (cnt == 0) {
                summary.write("Hooray, no failures today!");
            }

            summary.write(CRLF);
            summary.write("--------------------------------------");
            summary.write("--------------------------------------" + CRLF);
            summary.write("--------------------------------------");
            summary.write("--------------------------------------" + CRLF);
            summary.write("Succeeding tests: (no individual log exists)" + CRLF);
            summary.write("--------------------------------------");
            summary.write("--------------------------------------" + CRLF);
            summary.write("--------------------------------------");
            summary.write("--------------------------------------" + CRLF);

            // copy the good tests summary
            cnt = 0;
            while ((line = goodTests.readLine()) != null) {
                summary.write(line + CRLF);
                cnt++;
            }
            if (cnt == 0) {
                summary.write("Ooops. Who broke the system?!?");
            }
        }
    }

    /**
     * Analyzes the part of the log file that belongs to no test (but the
     * workbench initialization). It adds a pseudo failing test to the summary
     * file if it detects an exception in the log file, and keeps the header of
     * the log file as partial log.
     *
     * @param logFile the log file to analyze.
     * @throws IOException if the log file is not accessible
     */
    private void checkWorkbenchInit(final File logFile) throws IOException {
        boolean workbenchInitFailed = false;
        String testName = "_workbenchInitializing";
        String ownerAddress = FullWorkflowTest.REGRESSIONS_OWNER;
        File testFile = new File(m_tmpDir, testName + "_" + m_startTime + ".txt");

        try (BufferedReader logReader =
                new BufferedReader(new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {

            try (Writer testFileWriter =
                new OutputStreamWriter(new FileOutputStream(testFile), StandardCharsets.UTF_8)) {
                testFileWriter.write(ownerAddress + CRLF);

                String line = null;

                boolean exceptionStartLine = false;
                while ((line = logReader.readLine()) != null) {

                    if (line.contains(TEST_START_CODE)) {
                        // regression tests start here. Workbench initialization done.
                        break;
                    }

                    testFileWriter.write(line + CRLF);

                    if (line.indexOf("Exception: ") >= 0) {
                        exceptionStartLine = true;
                    } else if ((line.indexOf(" ERROR ") == 23)
                            || (line.indexOf(" FATAL ") == 23)) {
                        // an error during workbench init is not good.
                        workbenchInitFailed = true;
                    } else {
                        if (exceptionStartLine) {
                            // if the previous line started an exception dump
                            // this should be the first line of the stacktrace
                            if (line.matches("^\\tat .*\\(.*\\)$")) {
                                workbenchInitFailed = true;
                                // continue here, we want to copy the entire
                                // log file header in the testlog file
                            }
                            exceptionStartLine = false;
                        }
                    }
                }
            }
        }


        if (workbenchInitFailed) {
            m_summaryFailingTests.write("Test '" + testName
                    + "' failed with ERRORs.");
            m_summaryFailingTests
                    .write("(Owner: " + ownerAddress + ")." + CRLF);
        } else {
            // delete the file of the succeeding init part.
            testFile.delete();
        }

    }

    private void extractFailingTests(final File logFile) throws IOException {
        try (BufferedReader logReader =
                new BufferedReader(new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {

            String startKey = TEST_START_CODE;
            String startLine;

            while ((startLine = getLineContaining(startKey, logReader)) != null) {

                String testName =
                        startLine.substring(startLine.indexOf('\'') + 1, startLine
                                .lastIndexOf('\''));
                File testFile =
                        new File(m_tmpDir, testName + "_" + m_startTime + ".txt");
                boolean testSucceeded;
                String ownerAddress = null;
                try (Writer testFileWriter = new OutputStreamWriter(new FileOutputStream(testFile), StandardCharsets.UTF_8)) {
                    String ownerKey = "TestOwners=";
                    String ownerLine = getLineContaining(ownerKey, logReader);
                    if (ownerLine != null) {
                        // All tests MUST have an owner line (even ownerless ones - they
                        // must be owned by the regression master)
                        ownerAddress =
                                ownerLine.substring(ownerLine.indexOf(ownerKey)
                                        + ownerKey.length());
                        testFileWriter.write(ownerAddress + CRLF);
                    }

                    // also add the first line
                    testFileWriter.write(startLine + CRLF);

                    // copy all lines into the testfile and analyze them
                    testSucceeded = writeOneTest(testFileWriter, logReader, testName);
                }

                m_numOfTestsRun++;
                if (testSucceeded) {
                    m_summarySucceedingTests.write("Test '" + testName
                            + "' succeeded ");
                    m_summarySucceedingTests.write("(Owner: " + ownerAddress + ")."
                            + CRLF);
                    // delete the file of succeeding tests
                    testFile.delete();
                } else {
                    // this text is parsed by the nightly test scripts!
                    m_numOfFailingTests++;
                    m_summaryFailingTests.write("Test '" + testName + "' failed.");
                    m_summaryFailingTests.write("(Owner: " + ownerAddress + ")."
                            + CRLF);
                }
            }
        }
    }

    /*
     * copies all lines from the buffer into the outfile until it sees the end
     * test line. Searches for the FAIL or SUCCESS pattern in each line and
     * returns true, if it sound the success pattern, or false, if it found the
     * fail pattern.
     */
    private static boolean writeOneTest(final Writer testFile,
            final BufferedReader logReader, final String testName)
            throws IOException {

        boolean succeeded = false;
        boolean failed = false;

        String endKey = TEST_END_CODE;
        String nextLine;

        while ((nextLine = logReader.readLine()) != null) {

            testFile.write(nextLine + CRLF);

            if (nextLine.indexOf(endKey) >= 0) {
                // we're done.
                break;
            }

            if (nextLine.endsWith(FullWorkflowTest.FAIL_MSG)) {
                failed = true;
            }
            if (nextLine.endsWith(FullWorkflowTest.SUCCESS_MSG)) {
                succeeded = true;
            }

        }

        if (!(failed ^ succeeded)) {
            System.err.println("Log file isn't clear about result of test '"
                    + testName + "'!! Failing it!");
            return false;
        } else {
            return succeeded;
        }

    }

    /*
     * call only once with the logReader at the right position (somewhere before
     * the date in the log file).
     */
    private String extractTimestamp(final File logFile) throws IOException {
        String result = "unknown";
        try (BufferedReader logReader =
                new BufferedReader(new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
            String key = "logging date=";

            String line = getLineContaining(key, logReader);

            if ((line != null)
                    && (line.indexOf(key) + key.length() + 1 < line.length())) {
                String dateStr = line.substring(line.indexOf(key) + key.length());
                // dateStr should look like "Tue Jan 16 15:54:47 CET 2007"
                String[] segm = dateStr.split(" ");
                if (segm.length > 3) {
                    // we want to create something like "Jan16_15_54_47"
                    String[] timeSegm = segm[3].split(":");
                    if (timeSegm.length == 3) {
                        result =
                                segm[1] + segm[2] + "_" + timeSegm[0] + "_"
                                        + timeSegm[1] + "_" + timeSegm[2];
                    }
                }
            }

        }

        return result;
    }

    private static String getLineContaining(final String key, final BufferedReader logReader) throws IOException {
        String result;
        while ((result = logReader.readLine()) != null) {

            if (result.indexOf(key) >= 0) {
                break;
            }
        }
        return result;
    }

    /**
     * @param args
     */
    public static void main(final String[] args) {

        try {
            File logFileName =
                    new File(KNIMEConstants.getKNIMEHomeDir() + File.separator
                    // + NodeLogger.LOG_FILE);
                            // do not start the logger! That would append
                            // stuff to the log file.
                            + "knime.log");
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Choose the log file to analyze");
            fc.setDialogType(JFileChooser.OPEN_DIALOG);
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setSelectedFile(logFileName);

            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                new AnalyzeLogFile(fc.getSelectedFile(), null);
            } else {
                System.out.println("Canceled.");
            }

        } catch (IOException ioe) {
            System.err.println("IO Exception during log file analyze:");
            if (ioe.getMessage() != null) {
                System.err.println(ioe.getMessage());
            } else {
                System.err.println("  no message.");
            }
        }
    }
}
