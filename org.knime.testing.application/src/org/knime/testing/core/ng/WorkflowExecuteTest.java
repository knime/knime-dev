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
import java.lang.management.MemoryUsage;
import java.util.Formatter;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.ThreadUtils;
import org.knime.testing.core.TestrunConfiguration;
import org.knime.testing.node.config.TestConfigNodeModel;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

/**
 * Executed a workflows and checks if all nodes are executed (except nodes that are supposed to fail). The workflow is
 * canceled if it still running after the configured timeout.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
class WorkflowExecuteTest extends WorkflowTest {

    private final class WatchDog extends TimerTask {
        private final TestflowConfiguration m_flowConfiguration;

        private final TestResult m_result;

        private final long m_timeout;

        private final long m_startTime = System.currentTimeMillis();

        /**
         * @param flowConfiguration
         * @param result
         */
        private WatchDog(final TestflowConfiguration flowConfiguration, final TestResult result) {
            m_flowConfiguration = flowConfiguration;
            m_result = result;
            m_timeout = 1000L * ((m_flowConfiguration.getTimeout() > 0) ? m_flowConfiguration.getTimeout()
                : m_runConfiguration.getTimeout());
        }

        @Override
        public void run() {
            try {
                internalRun();
            } catch (Exception ex) {
                m_result.addError(WorkflowExecuteTest.this, ex);
            }
        }

        private void internalRun() {
            if (m_progressMonitor.isCanceled()) {
                m_result.addError(WorkflowExecuteTest.this, new InterruptedException("Testflow canceled by user"));
                m_context.getWorkflowManager().getParent().cancelExecution(m_context.getWorkflowManager());
                this.cancel();
            } else if (System.currentTimeMillis() > m_startTime + m_timeout) {
                String status =
                    m_context.getWorkflowManager().printNodeSummary(m_context.getWorkflowManager().getID(), 0);
                String message =
                    "Worklow running longer than " + (m_timeout / 1000.0) + " seconds.\n" + "Node status:\n" + status;
                if (m_runConfiguration.isStacktraceOnTimeout()) {
                    MemoryUsage usage = getHeapUsage();

                    try (final var formatter = new Formatter()) {
                        formatter.format("Memory usage: %1$,.3f MB max, %2$,.3f MB used, %3$,.3f MB free",
                            usage.getMax() / 1024.0 / 1024.0, usage.getUsed() / 1024.0 / 1024.0,
                            (usage.getMax() - usage.getUsed()) / 1024.0 / 1024.0);
                        message += "\n" + formatter.out().toString();
                        message += "\nThread status:\n" + ThreadUtils.getJVMStacktraces();
                    }
                }
                NodeLogger.getLogger(WorkflowExecuteTest.class).info(message);
                NodeLogger.getLogger(WorkflowExecuteTest.class).infoWithFormat(
                    "KNIME Global ThreadPool Stats:" + "%d running tasks of %d maximum",
                    KNIMEConstants.GLOBAL_THREAD_POOL.getRunningThreads(),
                    KNIMEConstants.GLOBAL_THREAD_POOL.getMaxThreads());
                m_result.addFailure(WorkflowExecuteTest.this, new AssertionFailedError(message));
                m_context.getWorkflowManager().getParent().cancelExecution(m_context.getWorkflowManager());
                this.cancel();
            }
        }
    }

    private static final Timer TIMEOUT_TIMER = new Timer("Workflow watchdog", true);

    /**
     * The current testrun configuration.
     */
    protected final TestrunConfiguration m_runConfiguration;

    protected final File m_testcaseRoot;

    WorkflowExecuteTest(final File mountPointRoot, final String workflowName, final IProgressMonitor monitor,
        final TestrunConfiguration runConfiguration, final WorkflowTestContext context) {
        super(workflowName, monitor, context);
        m_testcaseRoot = mountPointRoot;
        m_runConfiguration = runConfiguration;
    }

    /**
     * Hook that is called before the workflow is executed where the workflow manager can be configured. The default
     * implementation does nothing.
     *
     * @param wfm the workflow manager, never <code>null</code>
     * @throws InvalidSettingsException if settings cannot be loaded into a node (e.g.)
     */
    protected void configureWorkflowManager(final WorkflowManager wfm) throws InvalidSettingsException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final TestResult result) {
        if (!m_context.getTestflowConfiguration().executeWithCurrentTableBackend()) {
            ignoreTest(result);
            return;
        }

        boolean resetToDefaultWorkspaceDirRequired = setCustomWorkspaceDirPath(m_testcaseRoot);

        result.startTest(this);

        TimerTask watchdog = null;
        try {

            resetTestflowConfigNode();
            //this method should be called _after_ resetting the testflow config node
            //this prevents that temporary changes to the node's settings (e.g. JobManager) are reverted
            configureWorkflowManager(m_context.getWorkflowManager());

            final var flowConfiguration = m_context.getTestflowConfiguration();

            watchdog = new WatchDog(flowConfiguration, result);

            TIMEOUT_TIMER.schedule(watchdog, 500, 500);
            m_context.getWorkflowManager().executeAllAndWaitUntilDone();
            if (!m_progressMonitor.isCanceled()) {
                checkExecutionStatus(result, m_context.getWorkflowManager(), flowConfiguration);
            }
        } catch (Throwable t) {
            result.addError(this, t);
        } finally {
            result.endTest(this);
            if (resetToDefaultWorkspaceDirRequired) {
                setDefaultWorkspaceDirPath();
            }
            if (watchdog != null) {
                watchdog.cancel();
            }
        }

    }

    private void resetTestflowConfigNode() {
        final var wfm = m_context.getWorkflowManager();

        for (NodeContainer cont : wfm.getNodeContainers()) {
            if (cont instanceof NativeNodeContainer nnc && nnc.getNodeModel() instanceof TestConfigNodeModel) {
                wfm.resetAndConfigureNode(cont.getID());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "execute workflow";
    }

    private void checkExecutionStatus(final TestResult result, final WorkflowManager wfm,
        final TestflowConfiguration flowConfiguration) {

        for (NodeContainer node : wfm.getNodeContainers()) {
            if (node instanceof SubNodeContainer snc) {
                if (flowConfiguration.testNodesInComponents()) {
                    checkExecutionStatus(result, snc.getWorkflowManager(), flowConfiguration);
                } else {
                    checkNodeExecutionStatus(node, result, flowConfiguration);
                }
            } else if (node instanceof WorkflowManager wfmNode) {
                checkExecutionStatus(result, wfmNode, flowConfiguration);
            } else if (node instanceof SingleNodeContainer) {
                checkNodeExecutionStatus(node, result, flowConfiguration);
            } else {
                throw new IllegalStateException("Unknown node container type: " + node.getClass());
            }
        }
    }

    private void checkNodeExecutionStatus(final NodeContainer node, final TestResult result,
        final TestflowConfiguration flowConfiguration) {
        final var status = node.getNodeContainerState();
        if (!status.isExecuted() && !flowConfiguration.nodeMustFail(node.getID())) {
            final var nodeMessage = node.getNodeMessage();
            result.addFailure(this, executionFailure(node, nodeMessage));

            final var pattern = Pattern.compile(Pattern.quote(nodeMessage.getMessage()));
            flowConfiguration.addNodeErrorMessage(node.getID(), pattern);
            flowConfiguration.addRequiredError(pattern);
        } else if (status.isExecuted() && flowConfiguration.nodeMustFail(node.getID())) {
            result.addFailure(this, expectedErrorNotThrown(node, flowConfiguration.getNodeErrorMessage(node.getID())));
        }
    }

    /**
     * @param node that did not fail
     * @param pattern of expected error messages
     * @return an error that explains that the node was executed
     */
    private static AssertionFailedError expectedErrorNotThrown(final NodeContainer node, final Pattern pattern) {
        final var message = String.format( //
            "Node \"%s\" is executed although it should be failed with node message %s", //
            node.getNameWithID(), //
            WorkflowNodeMessagesTest.expectedMessageString(NodeMessage.Type.ERROR, pattern));
        return new AssertionFailedError(message);
    }

    /**
     * @param node that failed
     * @param nodeMessage that states the failure
     * @return error that explains that the node should have been executed
     */
    private static AssertionFailedError executionFailure(final NodeContainer node, final NodeMessage nodeMessage) {
        final var explain = Map.of( //
            NodeMessage.Type.ERROR, "is failed", //
            NodeMessage.Type.RESET, "is reset", //
            NodeMessage.Type.WARNING, "has warning" //
        );

        final var message = "Node \"%s\" %s although it should be executed. Node message is: %s".formatted( //
            node.getNameWithID(), //
            explain.get(nodeMessage.getMessageType()), //
            WorkflowNodeMessagesTest.messageToDetailedString(nodeMessage));
        return new AssertionFailedError(message);
    }

}
