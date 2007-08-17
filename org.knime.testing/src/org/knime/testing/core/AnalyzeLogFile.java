/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   16.01.2007 (ohl): created
 */
package org.knime.testing.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JFileChooser;

import org.knime.core.node.KNIMEConstants;

/**
 * Analyzes a log file of a previous regression run. Log file should only
 * contain the log of only one run. It will create a subdir in the java temp dir
 * (with a timestamp from the log file) and stores a bunch of files in there
 * with the output of failing tests and a summary file and stuff. Each file
 * contains the email address of the failing regression test owner in the first
 * line.
 * 
 * @author ohl, University of Konstanz
 */
public class AnalyzeLogFile {

    /**
     * privately used enum
     */
    enum ErrorCode {
        /** good test */
        OK,
        /** test produced error(s) */
        ERROR,
        /** test fails due to exception */
        EXCEPTION,
        /** test has errors and exceptions */
        ERREXCEPT
    };

    /**
     * This guy(s) get a summary email and some more general regression
     * notifications.
     */
    private final static String REGRESSIONS_OWNER = "peter.ohl@uni-konstanz.de";

    private final static String CRLF = "\r\n";

    private int m_numOfTestsRun = 0;

    private int m_numOfFailingTests = 0;

    private int m_numOfOwnerlessTests = 0;

    // directory all temporary files go in.
    private File m_tmpDir;

    private final String m_startTime;

    private FileWriter m_summaryFailingTests;

    private FileWriter m_summarySucceedingTests;

    /**
     * Constructor.
     * 
     * @param logFile the log file to analyze.
     * @param outputDir a directory in which a dir will be created containing
     *            the results. If null, the java default temp dir will be used.
     * @throws FileNotFoundException if it couldn't find the log file.
     * @throws IOException if something went wrong writing the files.
     */
    AnalyzeLogFile(final File logFile, final File outputDir)
            throws FileNotFoundException, IOException {
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
        BufferedReader logReader = new BufferedReader(new FileReader(logFile));
        String copyName = logFile.getName();
        if (copyName.endsWith(".log") && (copyName.length() > 4)) {
            copyName =
                    "_" + copyName.substring(0, copyName.length() - 4)
                            + m_startTime + ".log";
        } else {
            copyName = "_" + copyName + m_startTime;
        }
        File logCopy = new File(m_tmpDir, copyName);
        FileWriter logWriter = new FileWriter(logCopy);
        String line = null;
        while ((line = logReader.readLine()) != null) {
            logWriter.write(line);
            logWriter.write(CRLF);
        }
        logReader.close();
        logWriter.close();

        // global file writer. All methods write in there.
        File failTests =
                new File(m_tmpDir, "SummaryFailingTests_" + m_startTime
                        + ".txt");
        m_summaryFailingTests = new FileWriter(failTests);

        // global file writer. All methods write in there.
        File goodTests =
                new File(m_tmpDir, "SummarySucceedingTests_" + m_startTime
                        + ".txt");
        m_summarySucceedingTests = new FileWriter(goodTests);

        extractOwnerlessTests(logCopy);

        extractFailingTests(logCopy);

        // close them for the createSummary method.
        m_summaryFailingTests.close();
        m_summarySucceedingTests.close();

        BufferedReader negReader =
                new BufferedReader(new FileReader(failTests));
        BufferedReader posReader =
                new BufferedReader(new FileReader(goodTests));
        // combines the two summary files
        createSummary(negReader, posReader);
        negReader.close();
        posReader.close();

        System.out.println("Results are in '" + m_tmpDir.getAbsolutePath()
                + "'.");

        failTests.delete(); // should be contained in the overallsummary file
        goodTests.delete(); // should be contained in the overallsummary file
    }

    private void createSummary(final BufferedReader failTests,
            final BufferedReader goodTests) throws IOException {
        FileWriter summary =
                new FileWriter(new File(m_tmpDir, "_Summary_" + m_startTime
                        + ".txt"));

        int posTests = m_numOfTestsRun - m_numOfFailingTests;
        int posRate =
                (int)Math.round((posTests * 100.0) / (double)m_numOfTestsRun);

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
        while ((line = failTests.readLine()) != null) {
            summary.write(line + CRLF);
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
        while ((line = goodTests.readLine()) != null) {
            summary.write(line + CRLF);
        }

        summary.close();
    }

    private void extractFailingTests(final File logFile) throws IOException {

        BufferedReader logReader = new BufferedReader(new FileReader(logFile));

        String startKey = "<Start> Test='";
        String startLine;

        while ((startLine = getLineContaining(startKey, logReader)) != null) {

            ErrorCode testResult = ErrorCode.OK;

            String testName =
                    startLine.substring(startLine.indexOf('\'') + 1, startLine
                            .lastIndexOf('\''));
            File testFile =
                    new File(m_tmpDir, testName + "_" + m_startTime + ".txt");
            FileWriter testFileWriter = new FileWriter(testFile);

            String ownerKey = "TestOwners=";
            String ownerLine = getLineContaining(ownerKey, logReader);
            String ownerAddress = null;
            if (ownerLine != null) {
                ownerAddress =
                        ownerLine.substring(ownerLine.indexOf(ownerKey)
                                + ownerKey.length());
                testFileWriter.write(ownerAddress + CRLF);
            }

            // also add the first line
            testFileWriter.write(startLine + CRLF);

            // copy all lines into the testfile and analyze them
            testResult = writeOneTest(testFileWriter, logReader);
            testFileWriter.close();

            m_numOfTestsRun++;
            switch (testResult) {
            case OK:
                m_summarySucceedingTests.write("Test '" + testName
                        + "' succeeded ");
                m_summarySucceedingTests.write("(Owner: " + ownerAddress + ")."
                        + CRLF);
                // delete the file of succeeeding tests
                testFile.delete();
                break;
            case ERROR:
                m_numOfFailingTests++;
                m_summaryFailingTests.write("Test '" + testName
                        + "' failed with ERRORs.");
                m_summaryFailingTests.write("(Owner: " + ownerAddress + ")."
                        + CRLF);
                break;
            case EXCEPTION:
                m_numOfFailingTests++;
                m_summaryFailingTests.write("Test '" + testName
                        + "' failed with Exceptions.");
                m_summaryFailingTests.write("(Owner: " + ownerAddress + ")."
                        + CRLF);
                break;
            case ERREXCEPT:
                m_numOfFailingTests++;
                m_summaryFailingTests.write("Test '" + testName
                        + "' failed with ERRORs and Exceptions.");
                m_summaryFailingTests.write("(Owner: " + ownerAddress + ")."
                        + CRLF);
                break;
            }
        }

        logReader.close();

    }

    /*
     * copies all lines from the buffer into the outfile until it sees the end
     * test line. Analyzes each file and returns a code indicating whether it
     * saw an error or exception in the logfile.
     */
    private ErrorCode writeOneTest(final FileWriter testFile,
            final BufferedReader logReader) throws IOException {

        // a test fails if an error or exception in the log file occurs.s
        ErrorCode result = ErrorCode.OK;

        String endKey = "<End> Test='";
        String nextLine;

        while ((nextLine = logReader.readLine()) != null) {

            testFile.write(nextLine + CRLF);

            if (nextLine.indexOf(endKey) >= 0) {
                // we're done.
                break;
            }

            /*
             * An ERROR line starts like this: 2007-01-16 15:55:38,546 ERROR
             * That is 23 characters before the " ERROR ".
             */
            if (nextLine.indexOf(" ERROR ") == 23) {
                // if there is an exception in the test, the TestCase appends
                // a certain message at the end of the test to the logfile
                if (nextLine.contains(KnimeTestCase.EXCEPT_FAIL_MSG)) {
                    if (result == ErrorCode.ERROR) {
                        result = ErrorCode.ERREXCEPT;
                    } else {
                        result = ErrorCode.EXCEPTION;
                    }
                } else {
                    // it's a "simple" error
                    if (result == ErrorCode.EXCEPTION) {
                        result = ErrorCode.ERREXCEPT;
                    } else {
                        result = ErrorCode.ERROR;
                    }
                }
            }

        }

        return result;

    }

    /*
     * call only once with the logReader at the right position (somewhere before
     * the date in the log file).
     */
    private String extractTimestamp(final File logFile) throws IOException {

        BufferedReader logReader = new BufferedReader(new FileReader(logFile));

        String result = "unknown";
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

        logReader.close();

        return result;
    }

    private void extractOwnerlessTests(final File logFile) throws IOException {

        BufferedReader logReader = new BufferedReader(new FileReader(logFile));

        File ownerlessFile =
                new File(m_tmpDir, "_OwnerlessTests" + m_startTime + ".txt");

        FileWriter olfWriter = new FileWriter(ownerlessFile);

        String key = "ERROR main KnimeTestRegistry : Skipping test '";

        String line = null;

        while ((line = getLineContaining(key, logReader)) != null) {
            if (m_numOfOwnerlessTests == 0) {
                // first line in the file should be email of receiver
                olfWriter.write(REGRESSIONS_OWNER + CRLF);
            }
            olfWriter.write(line + CRLF);
            m_numOfOwnerlessTests++;
        }
        olfWriter.close();
        logReader.close();

        if (m_numOfOwnerlessTests == 0) {
            ownerlessFile.delete();
        }

    }

    private String getLineContaining(final String key,
            final BufferedReader logReader) throws IOException {

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
    public static void main(String[] args) {

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