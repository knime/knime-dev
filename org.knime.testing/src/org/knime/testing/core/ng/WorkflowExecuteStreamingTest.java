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
 * ---------------------------------------------------------------------
 *
 * History
 *   02.08.2013 (thor): created
 */
package org.knime.testing.core.ng;

import java.io.File;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNode;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainer.NodeContainerSettings;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.GUIDeadlockDetector;
import org.knime.testing.core.TestrunConfiguration;
import org.knime.testing.node.config.TestConfigNodeModel;
import org.knime.testing.streaming.testexecutor.StreamingTestNodeExecutionJob;
import org.knime.testing.streaming.testexecutor.StreamingTestNodeExecutionJobManager;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

/**
 * Executes a workflows in streaming mode (i.e. sets for each single node the {@link StreamingTestNodeExecutionJob}) and checks if all nodes are executed (except nodes that are supposed to fail). The workflow is
 * canceled if it still running after the configured timeout.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @author Martin Horn, University of Konstanz
 */
class WorkflowExecuteStreamingTest extends WorkflowTest {

    private final static NodeLogger LOGGER = NodeLogger.getLogger(WorkflowExecuteStreamingTest.class);

    private static final Timer TIMEOUT_TIMER = new Timer("Workflow watchdog", true);

    private final TestrunConfiguration m_runConfiguration;

    private File m_workflowDir;

    private File m_testcaseRoot;

    WorkflowExecuteStreamingTest(final File workflowDir, final File testcaseRoot, final String workflowName, final IProgressMonitor monitor,
                        final TestrunConfiguration runConfiguration, final WorkflowTestContext context) {
        super(workflowName, monitor, context);
        m_workflowDir = workflowDir;
        m_runConfiguration = runConfiguration;
        m_testcaseRoot = testcaseRoot;
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final TestResult result) {
        if(!m_context.getTestflowConfiguration().runStreamingTest()) {
            result.endTest(this);
            return;
        }

        TimerTask watchdog = null;

        try {
            result.startTest(this);

            //load the workflow
            LOGGER.info("Loading workflow '" + m_workflowName + "' for streaming test");
            WorkflowManager wfm =
                WorkflowLoadTest.loadWorkflow(this, result, m_workflowDir, m_testcaseRoot, m_runConfiguration);
            m_context.setWorkflowManager(wfm);

            // recursively set the test streaming executor for all non-executed nodes
            setStreamingTestExecutor(wfm);

            resetTestflowConfigNode();

            final TestflowConfiguration flowConfiguration = m_context.getTestflowConfiguration();

            watchdog = new TimerTask() {
                private final long timeout = ((flowConfiguration.getTimeout() > 0) ? flowConfiguration.getTimeout()
                    : m_runConfiguration.getTimeout()) * 1000;

                private final long startTime = System.currentTimeMillis();

                @Override
                public void run() {
                    try {
                        internalRun();
                    } catch (Exception ex) {
                        result.addError(WorkflowExecuteStreamingTest.this, ex);
                    }
                }

                private void internalRun() {
                    if (m_progressMonitor.isCanceled()) {
                        result.addError(WorkflowExecuteStreamingTest.this,
                            new InterruptedException("Testflow canceled by user"));
                        m_context.getWorkflowManager().getParent().cancelExecution(m_context.getWorkflowManager());
                        this.cancel();
                    } else if (System.currentTimeMillis() > startTime + timeout) {
                        String status =
                            m_context.getWorkflowManager().printNodeSummary(m_context.getWorkflowManager().getID(), 0);
                        String message = "Worklow running longer than " + (timeout / 1000.0) + " seconds.\n"
                            + "Node status:\n" + status;
                        if (m_runConfiguration.isStacktraceOnTimeout()) {
                            MemoryUsage usage = getHeapUsage();

                            Formatter formatter = new Formatter();
                            formatter.format("Memory usage: %1$,.3f MB max, %2$,.3f MB used, %3$,.3f MB free",
                                usage.getMax() / 1024.0 / 1024.0, usage.getUsed() / 1024.0 / 1024.0,
                                (usage.getMax() - usage.getUsed()) / 1024.0 / 1024.0);
                            message += "\n" + formatter.out().toString();
                            message += "\nThread status:\n" + GUIDeadlockDetector.createStacktrace();
                        }
                        NodeLogger.getLogger(WorkflowExecuteStreamingTest.class).info(message);
                        result.addFailure(WorkflowExecuteStreamingTest.this, new AssertionFailedError(message));
                        m_context.getWorkflowManager().getParent().cancelExecution(m_context.getWorkflowManager());
                        this.cancel();
                    }
                }
            };

            TIMEOUT_TIMER.schedule(watchdog, 500, 500);
            m_context.getWorkflowManager().executeAllAndWaitUntilDone();
            if (!m_progressMonitor.isCanceled()) {
                checkExecutionStatus(result, m_context.getWorkflowManager(), flowConfiguration);
            }


            //close workflow
            wfm.shutdown();
            wfm.getParent().removeNode(wfm.getID());

            List<NodeContainer> openWorkflows = new ArrayList<NodeContainer>(WorkflowManager.ROOT.getNodeContainers());
            openWorkflows.removeAll(m_context.getAlreadyOpenWorkflows());
            if (openWorkflows.size() > 0) {
                result.addFailure(this, new AssertionFailedError(openWorkflows.size()
                        + " dangling workflows detected: " + openWorkflows));
            }

        } catch (Throwable t) {
            result.addError(this, t);
        } finally {
            result.endTest(this);
            if (watchdog != null) {
                watchdog.cancel();
            }
        }

    }

    private void resetTestflowConfigNode() {
        WorkflowManager wfm = m_context.getWorkflowManager();

        for (NodeContainer cont : wfm.getNodeContainers()) {
            if ((cont instanceof NativeNodeContainer)
                    && (((NativeNodeContainer)cont).getNodeModel() instanceof TestConfigNodeModel)) {
                wfm.resetAndConfigureNode(cont.getID());
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "execute workflow in streaming mode";
    }

    private void checkExecutionStatus(final TestResult result, final WorkflowManager wfm,
                                      final TestflowConfiguration flowConfiguration) {
        for (NodeContainer node : wfm.getNodeContainers()) {
            NodeContainerState status = node.getNodeContainerState();

            if (node instanceof SubNodeContainer) {
                checkExecutionStatus(result, ((SubNodeContainer)node).getWorkflowManager(), flowConfiguration);
            } else if (node instanceof WorkflowManager) {
                checkExecutionStatus(result, (WorkflowManager)node, flowConfiguration);
            } else if (node instanceof SingleNodeContainer) {
                if (!status.isExecuted() && !flowConfiguration.nodeMustFail(node.getID())) {
                    NodeMessage nodeMessage = node.getNodeMessage();
                    String error =
                            "Node '" + node.getNameWithID() + "' is not executed. Error message is: "
                                    + nodeMessage.getMessage();
                    result.addFailure(this, new AssertionFailedError(error));

                    Pattern p = Pattern.compile(Pattern.quote(nodeMessage.getMessage()));
                    flowConfiguration.addNodeErrorMessage(node.getID(), p);
                    flowConfiguration.addRequiredError(p);
                } else if (status.isExecuted() && flowConfiguration.nodeMustFail(node.getID())) {
                    String error = "Node '" + node.getNameWithID() + "' is executed although it should have failed.";
                    result.addFailure(this, new AssertionFailedError(error));
                }
            } else {
                throw new IllegalStateException("Unknown node container type: " + node.getClass());
            }
        }
    }

    private void setStreamingTestExecutor(final WorkflowManager wfm) throws InvalidSettingsException {
        for (NodeContainer node : wfm.getNodeContainers()) {
            NodeContainerState status = node.getNodeContainerState();

            if (node instanceof SubNodeContainer) {
                setStreamingTestExecutor(((SubNodeContainer)node).getWorkflowManager());
            } else if (node instanceof WorkflowManager) {
                setStreamingTestExecutor((WorkflowManager)node);
            } else if (node instanceof SingleNodeContainer) {

                //only set the streaming executor if not loop start or end node and node is not executed
                if (!status.isExecuted() && !((SingleNodeContainer)node).isModelCompatibleTo(LoopStartNode.class)
                    && !((SingleNodeContainer)node).isModelCompatibleTo(LoopEndNode.class)) {
                    //set the job manager (mainly copied from AbstractClusterJob#setDefaultJobExecutor(...))
                    // TODO Can this be replaced by simply calling
                    // parent.setJobManager(nodeID, manager) ???
                    NodeSettings oldSettings = new NodeSettings("old");
                    NodeSettings newSettings = new NodeSettings("new");

                    wfm.saveNodeSettings(node.getID(), oldSettings);
                    wfm.saveNodeSettings(node.getID(), newSettings);

                    NodeContainerSettings ncSettings = new NodeContainerSettings();
                    ncSettings.load(oldSettings);

                    ncSettings.setJobManager(StreamingTestNodeExecutionJobManager.INSTANCE);

                    ncSettings.save(newSettings);
                    wfm.loadNodeSettings(node.getID(), newSettings);

                }
            }
        }
    }
}