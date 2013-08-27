/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
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
 *   23.05.2006 (Pluto): created // aka Fabian Dill!
 *   02.08.2007 (ohl): not a JUnit test anymore
 */
package org.knime.testing.core;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import junit.framework.TestSuite;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.FileUtil;

/**
 *
 * @author Fabian Dill, University of Konstanz
 * @deprecated use the new testing framework in <tt>org.knime.testing.core.ng</tt> instead
 */
@Deprecated
public class KnimeTestRegistry {
    private static NodeLogger logger = NodeLogger
            .getLogger(KnimeTestRegistry.class);

    /**
     * pattern (regular expression) used to select test cases to run.
     */
    private final String m_pattern;

    private final Collection<File> m_testRootDirs = new ArrayList<File>();

    private final File m_saveRootDir;

    private final boolean m_testDialogs;

    private final boolean m_testViews;

    private final int m_timeout;

    /**
     * Registry for all testing workflows.
     *
     * @param testNamePattern the pattern test names are matched against
     *            (regular expression). If <code>null</code> (or empty) all
     *            tests are run.
     * @param testRootDir a directory that is recursively searched for workflows
     * @param saveRoot directory where the executed workflows should be saved
     *            to. If <code>null</code> workflows are not saved.
     * @param testDialogs <code>true</code> if dialogs should be tested, too,
     *            <code>false</code> otherwise
     * @param testViews <code>true</code> if all views should be opended prior
     *            to running the workflow, <code>false</code> otherwise
     * @param timeout the timeout for each individual workflow
     */
    public KnimeTestRegistry(final String testNamePattern,
            final File testRootDir, final File saveRoot, final boolean testDialogs, final boolean testViews,
            final int timeout) {
        this(testNamePattern, Collections.singleton(testRootDir), saveRoot, testDialogs, testViews, timeout);
    }

    /**
     * Registry for all testing workflows.
     *
     * @param testNamePattern the pattern test names are matched against
     *            (regular expression). If <code>null</code> (or empty) all
     *            tests are run.
     * @param testRootDirs a collection of directories that are recursively
     *            searched for workflows
     * @param saveRoot directory where the executed workflows should be saved
     *            to. If <code>null</code> workflows are not saved.
     * @param testDialogs <code>true</code> if dialogs should be tested, too,
     *            <code>false</code> otherwise
     * @param testViews <code>true</code> if all views should be opended prior
     *            to running the workflow, <code>false</code> otherwise
     * @param timeout the timeout for each individual workflow
     */
    public KnimeTestRegistry(final String testNamePattern,
            final Collection<File> testRootDirs, final File saveRoot, final boolean testDialogs,
            final boolean testViews, final int timeout) {
        if (testRootDirs == null) {
            throw new IllegalArgumentException("Root dir for tests must not be null");
        }
        for (File dir : testRootDirs) {
            if (!dir.isDirectory()) {
                throw new IllegalArgumentException("Root dir '" + dir + "' for tests must be an existing directory");
            }
        }

        if ((testNamePattern != null) && (testNamePattern.length() == 0)) {
            m_pattern = ".*";
        } else {
            m_pattern = testNamePattern;
        }

        m_testRootDirs.addAll(testRootDirs);
        String saveSubDir = "savedTestFlows_";
        Calendar now = Calendar.getInstance();

        saveSubDir +=
                now.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US);
        if (now.get(Calendar.DATE) < 10) {
            saveSubDir += "0";
        }
        saveSubDir += now.get(Calendar.DATE);
        saveSubDir += "_";
        if (now.get(Calendar.HOUR_OF_DAY) < 10) {
            saveSubDir += "0";
        }
        saveSubDir += now.get(Calendar.HOUR_OF_DAY);
        saveSubDir += "_";
        if (now.get(Calendar.MINUTE) < 10) {
            saveSubDir += "0";
        }
        saveSubDir += now.get(Calendar.MINUTE);
        saveSubDir += "_";
        if (now.get(Calendar.SECOND) < 10) {
            saveSubDir += "0";
        }
        saveSubDir += now.get(Calendar.SECOND);

        if (saveRoot != null) {
            // if specified is must be a existing dir
            if (!saveRoot.exists() || !saveRoot.isDirectory()) {
                throw new IllegalArgumentException("Location to save workflows"
                        + " must be an existing directory");
            }
            m_saveRootDir = new File(saveRoot, saveSubDir);
        } else {
            m_saveRootDir = null;
        }

        m_testDialogs = testDialogs;
        m_testViews = testViews;
        m_timeout = timeout;
    }

    /**
     * @param factory a factory for creating workflow tests
     * @return all registered test cases.
     * @throws IOException if an I/O error occurs
     */
    public TestSuite collectTestCases(final WorkflowTestFactory factory) throws IOException {
        Collection<WorkflowTest> workflowTests = new ArrayList<WorkflowTest>();
        Collection<File> rootDirSnapshot = new ArrayList<File>(m_testRootDirs);
        // m_testRootDirs may be changed during search for zipped workflows
        for (File dir : rootDirSnapshot) {
            searchDirectory(dir, dir, workflowTests, factory);
        }

        if ((m_pattern != null) && (workflowTests.size() == 0)) {
            System.out.println("Found no matching tests. "
                    + "Thank you very much.");
        }

        TestSuite suite = new TestSuite();
        for (WorkflowTest test : workflowTests) {
            logger.debug("adding: " + test.getName());
            suite.addTest(test);
        }
        return suite;
    }

    /**
     * Searches in the directory and subdirectories for workflow.knime files and
     * adds them to m_registry.
     *
     * @param currentDir the current directory for the search
     * @param rootDir the root directory of all workflows
     * @param saveRoot the baseDir executed flows will be saved to (or null).
     * @throws IOException if an I/O error occurs
     */
    private void searchDirectory(final File currentDir, final File rootDir, final Collection<WorkflowTest> tests,
                                 final WorkflowTestFactory factory) throws IOException {
        if (m_pattern == null) {
            // null pattern matches nothing!
            return;
        }

        File workflowFile = new File(currentDir, WorkflowPersistor.WORKFLOW_FILE);

        if (workflowFile.exists()) {
            String name = currentDir.getName();
            if (name.matches(m_pattern)) {
                File saveLoc = createSaveLocation(workflowFile);
                WorkflowTest testCase = factory.createTestcase(workflowFile, rootDir, saveLoc);
                testCase.setTestDialogs(m_testDialogs);
                testCase.setTestViews(m_testViews);
                testCase.setTimeout(m_timeout);
                tests.add(testCase);
            } else {
                logger.info("Skipping testcase '" + name + "' (doesn't match"
                        + "pattern '" + m_pattern + "').");
            }
        } else {
            // recursivly search directories
            File[] dirList = currentDir.listFiles(directoryFilter);
            if (dirList == null) {
                logger.error("I/O error accessing '" + currentDir
                        + "'. Does it exist?!?");
                return;
            }
            for (int i = 0; i < dirList.length; i++) {
                searchDirectory(dirList[i], rootDir, tests, factory);
            }

            // check for zipped workflow(group)s
            File[] zipList = currentDir.listFiles(zipFilter);
            if (zipList == null) {
                logger.error("I/O error accessing '" + currentDir
                        + "'. Does it exist?!?");
                return;
            }
            for (int i = 0; i < zipList.length; i++) {
                String name = zipList[i].getName();
                if (name.matches(m_pattern)) {
                    File tempDir = FileUtil.createTempDir("UnzippedTestflows");
                    m_testRootDirs.add(tempDir);
                    FileUtil.unzip(zipList[i], tempDir);
                    searchDirectory(tempDir, rootDir, tests, factory);
                }
            }
        }
    }

    private File createSaveLocation(final File workflowDir) {
        if (m_saveRootDir == null) {
            // null indicates "do not save"
            return null;
        }

        File testName = workflowDir.getParentFile();
        // path from root to current flow dir
        String postfix = "";
        for (File dir : m_testRootDirs) {
            if (testName.getAbsolutePath().contains(dir.getAbsolutePath())) {
                postfix = testName.getAbsolutePath().substring(dir.getAbsolutePath().length());
            }
        }
        if (postfix.isEmpty()) {
            // seems the test was in the testRoot dir
            postfix = testName.getName();
        }
        return new File(m_saveRootDir, postfix);

    }

    private static final FileFilter directoryFilter =  new FileFilter() {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accept(final File pathname) {
            return pathname.isDirectory();
        }
    };

    private static final FileFilter zipFilter =  new FileFilter() {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accept(final File pathname) {
            return pathname.getName().endsWith(".zip");
        }
    };
}