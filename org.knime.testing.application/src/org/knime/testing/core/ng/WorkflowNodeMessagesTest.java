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

import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessage.Type;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

/**
 * Testcase that check all node messages after execution. Unexpected or wrong messages are reported as failures.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
class WorkflowNodeMessagesTest extends WorkflowTest {
    WorkflowNodeMessagesTest(final String workflowName, final IProgressMonitor monitor,
        final WorkflowTestContext context) {
        super(workflowName, monitor, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final TestResult result) {
        if (!m_context.getTestflowConfiguration().executeWithCurrentTableBackend()) {
            // this test also checks messages that are written by execute, therefore it can't be run if
            // nothing is executed
            ignoreTest(result);
            return;
        }

        result.startTest(this);

        try {
            checkNodeMessages(result, m_context.getWorkflowManager(), m_context.getTestflowConfiguration());
        } catch (Throwable t) {
            result.addError(this, t);
        } finally {
            result.endTest(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "node messages";
    }

    private void checkNodeMessages(final TestResult result, final WorkflowManager wfm,
        final TestflowConfiguration flowConfiguration) {
        for (NodeContainer node : wfm.getNodeContainers()) {
            if (!m_context.isPreExecutedNode(node)) {
                if (node instanceof SubNodeContainer) {
                    if (flowConfiguration.testNodesInComponents()) {
                        checkNodeMessages(result, ((SubNodeContainer)node).getWorkflowManager(), flowConfiguration);
                    } else {
                        checkSingleNode(result, node, flowConfiguration);
                    }
                } else if (node instanceof WorkflowManager) {
                    checkNodeMessages(result, (WorkflowManager)node, flowConfiguration);
                    checkSingleNode(result, node, flowConfiguration);
                } else {
                    checkSingleNode(result, node, flowConfiguration);
                }
            }
        }
    }

    private void checkSingleNode(final TestResult result, final NodeContainer node,
        final TestflowConfiguration flowConfiguration) {
        NodeMessage nodeMessage = node.getNodeMessage();

        Pattern expectedErrorMessagePattern = flowConfiguration.getNodeErrorMessage(node.getID());
        final var actualMessageBuilder = new StringBuilder(nodeMessage.getMessage());
        nodeMessage.getIssue().ifPresent(issue -> actualMessageBuilder.append("\n").append(issue));
        final var actualMessage = actualMessageBuilder.toString().trim();
        if (expectedErrorMessagePattern != null) {
            if (!expectedErrorMessagePattern.matcher(actualMessage).matches()) {
                String error =
                    "Node '" + node.getNameWithID() + "' has unexpected error message: expected\n"
                        + TestflowConfiguration.patternToString(expectedErrorMessagePattern) + "\n... but got...\n"
                        + actualMessage;
                result.addFailure(this, new AssertionFailedError(error));
            }
        } else if (Type.ERROR.equals(nodeMessage.getMessageType())) {
            String error =
                "Node '" + node.getNameWithID() + "' has unexpected error message: " + nodeMessage.getMessage();
            result.addFailure(this, new AssertionFailedError(error));
        }

        Pattern expectedWarningMessagePattern = flowConfiguration.getNodeWarningMessage(node.getID());
        if (expectedWarningMessagePattern != null) {
            if (!expectedWarningMessagePattern.matcher(actualMessage).matches()) {
                String error =
                    "Node '" + node.getNameWithID() + "' has unexpected warning message: expected\n"
                        + TestflowConfiguration.patternToString(expectedWarningMessagePattern) + "\n... but got...\n"
                        + actualMessage;
                result.addFailure(this, new AssertionFailedError(error));
            }
        } else if (Type.WARNING.equals(nodeMessage.getMessageType())) {
            String error =
                "Node '" + node.getNameWithID() + "' has unexpected warning message: " + nodeMessage.getMessage();
            result.addFailure(this, new AssertionFailedError(error));
        }
    }
}
