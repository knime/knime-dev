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
 *   19.08.2013 (thor): created
 */
package org.knime.testing.core.ng;

import javax.swing.SwingUtilities;

import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.util.Pair;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

/**
 * Testcase that reports any uncaught exceptions. An exception handler is installed during creation of the test. It
 * records all uncaught exception and reports them as errors when the test is run. The exception handler is removed
 * after the test has run.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
class WorkflowUncaughtExceptionsTest extends WorkflowTest {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowUncaughtExceptionsTest.class);

    WorkflowUncaughtExceptionsTest(final String workflowName, final IProgressMonitor monitor,
                                   final WorkflowTestContext context) {
        super(workflowName, monitor, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void aboutToStart() {
        super.aboutToStart();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final Thread t, final Throwable e) {
                synchronized (m_context.getUncaughtExceptions()) {
                    m_context.getUncaughtExceptions().add(new Pair<Thread, Throwable>(t, e));

                    String msg = "Uncaught " + e.getClass().getName() + " in thread " + t.getName();
                    if (NodeContext.getContext() != null) {
                        msg += " with node context '" + NodeContext.getContext() + "'";
                    }
                    msg += ": " + e.getMessage();
                    LOGGER.debug(msg, e);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final TestResult result) {
        result.startTest(this);
        try {
            assert !SwingUtilities.isEventDispatchThread() : "This part must not be executed in the AWT thread";
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    // do nothing, just wait for all previous events to be processed (e.g. close dialog and views)
                }
            });
            synchronized (m_context.getUncaughtExceptions()) {
                for (Pair<Thread, Throwable> p : m_context.getUncaughtExceptions()) {
                    final var t = p.getFirst();
                    final var threadName = t.getName();
                    final var exc = p.getSecond();
                    final var className = exc.getClass().getSimpleName();
                    final var msg = exc.getMessage();
                    if (ignoreException(exc)) {
                        LOGGER.debug(String.format("Ignored uncaught %s of Thread %s: %s", className, threadName, msg));
                        continue;
                    }

                    AssertionFailedError error =
                            new AssertionFailedError("Thread " + threadName + " has thrown an uncaught "
                                    + className + ": " + msg);
                    error.initCause(exc);
                    result.addError(this, error);
                }
            }
        } catch (Throwable t) {
            result.addError(this, t);
        } finally {
            result.endTest(this);
            Thread.setDefaultUncaughtExceptionHandler(null);
        }
    }

    /**
     * Check if we should ignore <i>harmless</i> uncaught exceptions, see AP-19057.
     */
    private static boolean ignoreException(final Throwable exc) {
        // AP-19057: sporadic exception after Apache SSHD's I/O executor service is closed
        final boolean isSSHDExecutorShutDownException = IllegalStateException.class.equals(exc.getClass())
                && "Executor has been shut down".equals(exc.getMessage());
        return isSSHDExecutorShutDownException;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "uncaught exceptions";
    }
}
