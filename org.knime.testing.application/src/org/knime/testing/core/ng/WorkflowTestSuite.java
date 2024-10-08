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
 * ---------------------------------------------------------------------
 *
 * History
 *   02.08.2013 (thor): created
 */
package org.knime.testing.core.ng;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.data.container.Buffer;
import org.knime.core.data.util.memory.MemoryAlert;
import org.knime.core.data.util.memory.MemoryAlertListener;
import org.knime.core.data.util.memory.MemoryAlertSystem;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.node.util.ViewUtils;
import org.knime.testing.core.TestrunConfiguration;

import junit.framework.TestListener;
import junit.framework.TestResult;

/**
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class WorkflowTestSuite extends WorkflowTest {
    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());

    private final List<WorkflowTest> m_allTests = new ArrayList<WorkflowTest>(8);
    private Set<String> m_nodesUnderTest;

    /**
     * Creates a new suite of workflow tests. Which tests are actually executed is determined by the given run
     * configuration.
     *
     * @param workflowDir the workflow file (<tt>workflow.knime</tt>)
     * @param testcaseRoot root directory of all test workflows; this is used as a replacement for the mount point root
     * @param runConfig run configuration for all test flows
     * @param monitor progress monitor, may be <code>null</code>
     * @throws IOException if an I/O error occurs while setting up the test suite
     */
    public WorkflowTestSuite(final File workflowDir, final File testcaseRoot, final TestrunConfiguration runConfig,
                             final IProgressMonitor monitor) throws IOException {
        this(workflowDir, testcaseRoot, runConfig, monitor, new WorkflowTestContext(runConfig));
    }

    /**
     * Creates a new suite of workflow tests. Which tests are actually executed is determined by the given run
     * configuration.
     *
     * @param workflowName the name of the workflow/testsuite.
     * @param workflowDir the workflow file (<tt>workflow.knime</tt>)
     * @param testcaseRoot root directory of all test workflows; this is used as a replacement for the mount point root
     * @param runConfig run configuration for all test flows
     * @param monitor progress monitor, may be <code>null</code>
     * @throws IOException if an I/O error occurs while setting up the test suite
     */
    public WorkflowTestSuite(final String workflowName, final File workflowDir, final File testcaseRoot,
        final TestrunConfiguration runConfig, final IProgressMonitor monitor) throws IOException {
        this(workflowName, workflowDir, testcaseRoot, runConfig, monitor, new WorkflowTestContext(runConfig));
    }

    /**
     * Creates a new suite of workflow tests. Which tests are actually executed is determined by the given run
     * configuration.
     *
     * @param workflowDir the workflow file (<tt>workflow.knime</tt>)
     * @param testcaseRoot root directory of all test workflows; this is used as a replacement for the mount point root
     * @param runConfig run configuration for all test flows
     * @param monitor progress monitor, may be <code>null</code>
     * @param testContext the test context that should be used
     * @throws IOException if an I/O error occurs while setting up the test suite
     */
    protected WorkflowTestSuite(final File workflowDir, final File testcaseRoot, final TestrunConfiguration runConfig,
        final IProgressMonitor monitor, final WorkflowTestContext testContext) throws IOException {
        this(getWorkflowName(workflowDir, testcaseRoot), workflowDir, testcaseRoot, runConfig, monitor, testContext);
    }

    /**
     * Creates a new suite of workflow tests. Which tests are actually executed is determined by the given run
     * configuration.
     *
     * @param workflowName the name of the workflow/testsuite
     * @param workflowDir the workflow file (<tt>workflow.knime</tt>)
     * @param testcaseRoot root directory of all test workflows; this is used as a replacement for the mount point root
     * @param runConfig run configuration for all test flows
     * @param monitor progress monitor, may be <code>null</code>
     * @param testContext the test context that should be used
     * @throws IOException if an I/O error occurs while setting up the test suite
     */
    protected WorkflowTestSuite(final String workflowName, final File workflowDir, final File testcaseRoot,
        final TestrunConfiguration runConfig, final IProgressMonitor monitor, final WorkflowTestContext testContext)
            throws IOException {
        super(workflowName, monitor, testContext);

        initTestsuite(workflowDir, testcaseRoot, runConfig);
    }

    static String getWorkflowName(final File workflowDir, final File testcaseRoot) {
        if (workflowDir.equals(testcaseRoot)) {
            return workflowDir.getName();
        } else {
            return workflowDir.getAbsolutePath().substring(testcaseRoot.getAbsolutePath().length() + 1);
        }
    }

    private void initTestsuite(final File workflowDir, final File testcaseRoot, final TestrunConfiguration runConfig) {
        if (runConfig.isLoadSaveLoad()) {
            m_allTests.add(createLoadSaveLoadTest(workflowDir, testcaseRoot, runConfig));
        } else {
            m_allTests.add(createLoadTest(workflowDir, testcaseRoot, runConfig));
        }
        if (runConfig.isReportDeprecatedNodes()) {
            m_allTests.add(new WorkflowDeprecationTest(m_workflowName, m_progressMonitor, m_context));
        }

        if (runConfig.isTestViews()) {
            m_allTests.add(new WorkflowOpenViewsTest(m_workflowName, m_progressMonitor, m_context));
        }

        m_allTests.add(new WorkflowExecuteTest(testcaseRoot, m_workflowName, m_progressMonitor, runConfig, m_context));

        if (runConfig.isCheckNodeMessages()) {
            m_allTests.add(new WorkflowNodeMessagesTest(m_workflowName, m_progressMonitor, m_context));
        }

        if (runConfig.isTestDialogs()) {
            m_allTests.add(new WorkflowDialogsTest(m_workflowName, m_progressMonitor, m_context));
        }

        m_allTests.add(new WorkflowHiliteTest(m_workflowName, m_progressMonitor, m_context));

        if (runConfig.isTestViews()) {
            m_allTests.add(new WorkflowCloseViewsTest(m_workflowName, m_progressMonitor, m_context));
        }

        if (runConfig.getSaveLocation() != null) {
            m_allTests.add(new WorkflowSaveTest(m_workflowName, m_progressMonitor, runConfig.getSaveLocation(),
                    m_context));
        }

        if (runConfig.isCloseWorkflowAfterTest()) {
            m_allTests.add(new WorkflowCloseTest(m_workflowName, m_progressMonitor, m_context));
        }

        if (runConfig.isCheckLogMessages()) {
            m_allTests.add(new WorkflowLogMessagesTest(m_workflowName, m_progressMonitor, m_context));
        }

        if (runConfig.isEnableStreamingTests()) {
            m_allTests.add(new WorkflowExecuteStreamingTest(workflowDir, testcaseRoot, m_workflowName,
                m_progressMonitor, runConfig, m_context));
        }

        m_allTests.add(new WorkflowUncaughtExceptionsTest(m_workflowName, m_progressMonitor, m_context));

        if (runConfig.isCheckMemoryLeaks()) {
            m_allTests.add(new WorkflowMemLeakTest(m_workflowName, m_progressMonitor, runConfig, m_context));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countTestCases() {
        return m_allTests.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final TestResult result) {
        m_progressMonitor.beginTask(getName(), countTestCases());
        m_logger.info("================= Starting testflow " + getName() + " =================");

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        try (Formatter format = new Formatter()) {
            format.format("================= Average load: %02.2f =================", osBean.getSystemLoadAverage());
            m_logger.info(format.out());
        }

        result.startTest(this);
        try {
            for (WorkflowTest test : m_allTests) {
                test.aboutToStart();
            }

            for (WorkflowTest test : m_allTests) {
                m_progressMonitor.subTask(test.getName());
                m_logger.info("----------------- Starting sub-test " + test.getName() + " -----------------");
                test.run(result);
                m_progressMonitor.worked(1);
                m_logger.info("----------------- Finished sub-test " + test.getName() + " -----------------");
                if (m_progressMonitor.isCanceled()) {
                    break;
                }
            }
            Buffer.waitForAsyncWrites();
            waitForUIEvents();
            m_logger.info("Programmatically sending memory alert after test cases finished...");
            waitForMemoryAlertToPropagate();
        } catch (Throwable ex) {
            result.addError(this, ex);
        } finally {
            m_nodesUnderTest = new HashSet<>(m_context.getNodesUnderTest()); // store a copy before clearing
            m_context.clear();
            Thread.setDefaultUncaughtExceptionHandler(null);
            result.endTest(this);
            logMemoryStatus();
            m_logger.info("================= Finished testflow " + getName() + " =================");

        }
    }

    /**
     * Calls {@link MemoryAlertSystem#sendMemoryAlert()} and waits for alert to propagate. Will be called after each
     * test workflow to allow for temporary DataContainer to be collected. Added to address the issue where "new
     * DataContainer(..." is used but not cleared within the test case. The Container/Buffer is then added to the mem
     * alert system and swapped after the test case is run -- its workflow was already removed and that caused ERROR to
     * be logged to the knime.log (= test case failure).
     */
    private static void waitForMemoryAlertToPropagate() throws InterruptedException {
        for (MemoryAlertSystem mas : new LinkedHashSet<>(Arrays.asList(
            // might be the same, depending on selected GC
            MemoryAlertSystem.getInstance(), MemoryAlertSystem.getInstanceUncollected()))) {
            Lock lock = new ReentrantLock();
            Condition condition = lock.newCondition();
            MutableBoolean hasNotSignalled = new MutableBoolean(true);
            MemoryAlertListener listener = new MemoryAlertListener() {
                @Override
                protected boolean memoryAlert(final MemoryAlert alert) {
                    lock.lock();
                    try {
                        hasNotSignalled.setTrue();
                        condition.signalAll();
                        return true;
                    } finally {
                        lock.unlock();
                    }
                }
            };
            lock.lockInterruptibly();
            try {
                mas.addListener(listener);
                mas.sendMemoryAlert();
                while (!hasNotSignalled.booleanValue()) {
                    condition.await();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private void waitForUIEvents() throws InterruptedException {
        m_logger.info("----------------- Waiting for UI events to finish -----------------");
        CountDownLatch wait = new CountDownLatch(1);
        ViewUtils.invokeLaterInEDT(() -> wait.countDown());
        wait.await(2, TimeUnit.MINUTES);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return getWorkflowName();
    }

    /**
     * Runs a single workflow test suite.
     *
     * @param suite the test suite
     * @param listener a listener for test results
     * @return the result of the test
     */
    public static WorkflowTestResult runTest(final WorkflowTestSuite suite, final TestListener listener) {
        final WorkflowTestResult result = new WorkflowTestResult(suite);
        result.addListener(listener);
        Writer stdout = new Writer() {
            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                result.handleSystemOut(cbuf, off, len);
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
                result.handleSystemErr(cbuf, off, len);
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
        suite.run(result);
        NodeLogger.removeWriter(stderr);
        NodeLogger.removeWriter(stdout);
        return result;
    }

    private void logMemoryStatus() {
        MemoryUsage usage = getHeapUsage();

        try (Formatter formatter = new Formatter()) {
            formatter.format("===== Memory statistics: %1$,.3f MB max, %2$,.3f MB used, %3$,.3f MB free ====",
                usage.getMax() / 1024.0 / 1024.0, usage.getUsed() / 1024.0 / 1024.0,
                (usage.getMax() - usage.getUsed()) / 1024.0 / 1024.0);
            m_logger.info(formatter.out().toString());
        }
    }

    /**
     * Creates a new test for loading a workflow. The test must put the workflow manager into the test context. Note
     * that this method is called from the constructor, therefore instance variables from subclasses are not yet
     * available!
     *
     * @param workflowDir the workflow's directory
     * @param testcaseRoot the testcase root directory (used as the mountpoint root)
     * @param runConfig the run configuration
     *
     * @return a new workflow test
     */
    protected WorkflowTest createLoadTest(final File workflowDir, final File testcaseRoot,
                                          final TestrunConfiguration runConfig) {
        return new WorkflowLoadTest(workflowDir, testcaseRoot, m_workflowName, m_progressMonitor, runConfig, m_context);
    }

    /**
     * Creates a new load-save-load test. The test must put the workflow manager of the second load into the test
     * context. Note that this method is called from the constructor, therefore instance variables from subclasses are
     * not yet available!
     *
     * @param workflowDir the workflow's directory
     * @param testcaseRoot the testcase root directory (used a the mountpoint root)
     * @param runConfig the run configuration
     *
     * @return a new workflow test
     */
    protected WorkflowTest createLoadSaveLoadTest(final File workflowDir, final File testcaseRoot,
                                                  final TestrunConfiguration runConfig) {
        return new WorkflowLoadSaveLoadTest(workflowDir, testcaseRoot, m_workflowName, m_progressMonitor, runConfig,
                m_context);
    }

    /**
     * Returns a set of nodes that have been executed in this testflow run. Nodes that are present in the workflow but
     * are not executed are not tracked.
     *
     * @return nodes that where executed in this test
     */
    protected Set<String> getNodesUnderTest() {
        return m_nodesUnderTest;
    }
}
