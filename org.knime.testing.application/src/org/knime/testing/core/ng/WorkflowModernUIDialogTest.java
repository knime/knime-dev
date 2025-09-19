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

import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.SingleNodeContainer.SingleNodeContainerSettings;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.node.parameters.NodeParameters;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

/**
 * Before execution, all nodes having a modern UI dialog backed by {@link NodeParameters} are loading & applying
 * their settings via the dialog's {@link NodeParameters} class. Checks that "modern ui" (MUI) migration does
 * not break the dialog's load/save settings functionality.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
class WorkflowModernUIDialogTest extends WorkflowTest {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowModernUIDialogTest.class);

    WorkflowModernUIDialogTest(final String workflowName, final IProgressMonitor monitor,
        final WorkflowTestContext context) {
        super(workflowName, monitor, context);
    }

    @Override
    public void run(final TestResult result) {
        result.startTest(this);

        try {
            boolean hasAppliedToAtLeastOneNode = checkMUIDialogsRecursively(result, m_context.getWorkflowManager());
            // TODO report as skipped when no dialog was found -- requires update to junit 4 or 5
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
        return "check modern ui dialogs";
    }

    private boolean checkMUIDialogsRecursively(final TestResult result, final WorkflowManager wfm) {
        var hasAppliedToAtLeastOneNode = false;
        for (final NodeContainer node : wfm.getNodeContainers()) {
            if (m_context.isPreExecutedNode(node)) {
                continue;
            }
            if (node instanceof NativeNodeContainer nnc) {
                hasAppliedToAtLeastOneNode |= reloadSettingsViaNodeParameters(nnc, result);
            } else if (node instanceof WorkflowManager subWfm) {
                hasAppliedToAtLeastOneNode |= checkMUIDialogsRecursively(result, subWfm);
            } else if (node instanceof SubNodeContainer snc) {
                hasAppliedToAtLeastOneNode |= checkMUIDialogsRecursively(result, snc.getWorkflowManager());
            }
        }
        return hasAppliedToAtLeastOneNode;
    }

    @SuppressWarnings("restriction")
    private boolean reloadSettingsViaNodeParameters(final NativeNodeContainer nnc, final TestResult result) {
        NodeContext.pushContext(nnc);
        try {
            WorkflowManager wfm = nnc.getParent();
            NodeFactory<NodeModel> nodeFactory = nnc.getNode().getFactory();
            if (nodeFactory instanceof NodeDialogFactory ndf && ndf.hasNodeDialog()
                && ndf.createNodeDialog() instanceof DefaultNodeDialog nodeDialog) {
                Class<? extends NodeParameters> settingsClass = nodeDialog.getSettingsClass(SettingsType.MODEL);
                if (settingsClass == null) {
                    LOGGER.debugWithFormat(
                        "Skipping dialog of node %s because node does not provide a settings class", nnc.getName());
                    return false;
                }
                LOGGER.debugWithFormat("Loading node settings into NodeParameters (%s)", nnc.getName());
                long start = System.currentTimeMillis();
                final var nodeSettings = new NodeSettings("original");
                wfm.saveNodeSettings(nnc.getID(), nodeSettings);
                final SingleNodeContainerSettings sncs = new SingleNodeContainerSettings(nodeSettings);
                final var nodeParameters = NodeParametersUtil.loadSettings(sncs.getModelSettings(), settingsClass);
                final var nodeSettingsCopy = new NodeSettings("washed");
                NodeParametersUtil.saveSettings(settingsClass, nodeParameters, nodeSettingsCopy);
                sncs.setModelSettings(nodeSettingsCopy);
                NodeSettings temp = new NodeSettings("temp");
                sncs.save(temp);
                wfm.loadNodeSettings(nnc.getID(), temp);
                long delay = System.currentTimeMillis() - start;
                LOGGER.debugWithFormat("Applying NodeParamters took %d ms (%s)", delay, nnc.getName());
                return true;
            }
        } catch (final Exception e) {
            String msg = "Dialog of node '" + nnc.getNameWithID() + "' has thrown an "
                    + e.getClass().getSimpleName() + ": " + e.getMessage();
            final var error = new AssertionFailedError(msg);
            error.initCause(e);
            result.addFailure(this, error);
        } finally {
            NodeContext.removeLastContext();
        }
        return false;
    }
}
