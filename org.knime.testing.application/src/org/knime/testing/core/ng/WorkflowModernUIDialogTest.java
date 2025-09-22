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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.InvalidSettingsException;
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
import org.knime.core.webui.data.util.InputPortUtil;
import org.knime.core.webui.node.DataServiceManager;
import org.knime.core.webui.node.NodeWrapper;
import org.knime.core.webui.node.dialog.NodeDialogFactory;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.VariableSettingsRO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.UpdatesUtil;
import org.knime.core.webui.node.dialog.defaultdialog.jsonforms.JsonFormsSettingsImpl;
import org.knime.core.webui.node.dialog.defaultdialog.settingsconversion.VariableSettingsUtil;
import org.knime.core.webui.node.dialog.internal.VariableSettings;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

/**
 * Before execution, all nodes having a modern UI dialog backed by {@link NodeParameters} are loading & applying their
 * settings via the dialog's {@link NodeParameters} class. Checks that "modern ui" (MUI) migration does not break the
 * dialog's load/save settings functionality.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 */
class WorkflowModernUIDialogTest extends WorkflowTest {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowModernUIDialogTest.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
            final var dataServiceManager = NodeDialogManager.getInstance().getDataServiceManager();
            if (nodeFactory instanceof NodeDialogFactory ndf && ndf.hasNodeDialog()
                && ndf.createNodeDialog() instanceof DefaultNodeDialog nodeDialog) {
                long start = System.currentTimeMillis();

                LOGGER.debugWithFormat("Loading node settings into NodeParameters (%s)", nnc.getName());
                final var nodeSettings = new NodeSettings("original");
                wfm.saveNodeSettings(nnc.getID(), nodeSettings);
                final SingleNodeContainerSettings sncs = new SingleNodeContainerSettings(nodeSettings);
                final var settingsClasses = nodeDialog.getSettingsClasses();
                final Map<SettingsType, NodeParameters> loadedSettings = new HashMap<>();
                Map<SettingsType, VariableSettingsRO> variableSettings = new HashMap<>();
                if (settingsClasses.containsKey(SettingsType.MODEL)) {
                    loadedSettings.put(SettingsType.MODEL, NodeParametersUtil.loadSettings(sncs.getModelSettings(),
                        settingsClasses.get(SettingsType.MODEL)));
                    variableSettings.put(SettingsType.MODEL, new VariableSettings(nodeSettings, SettingsType.MODEL));
                }
                if (settingsClasses.containsKey(SettingsType.VIEW) && sncs.getViewSettings() != null) {
                    loadedSettings.put(SettingsType.VIEW, NodeParametersUtil.loadSettings(sncs.getViewSettings(),
                        settingsClasses.get(SettingsType.VIEW)));
                    variableSettings.put(SettingsType.VIEW, new VariableSettings(nodeSettings, SettingsType.VIEW));
                }

                final var input = NodeParametersUtil
                    .createDefaultNodeSettingsContext(InputPortUtil.getInputSpecsExcludingVariablePort(nnc));
                /**
                 * Check that (at least) the initially computed dialog states (e.g. choices of a dropdown) don't throw
                 * an error.
                 */
                UpdatesUtil.constructTreesAndAddUpdates(MAPPER.createObjectNode(), loadedSettings, input);
                final var data = new JsonFormsSettingsImpl(loadedSettings, input).getData();
                applyData(nnc, dataServiceManager, data, variableSettings, input);

                long delay = System.currentTimeMillis() - start;
                LOGGER.debugWithFormat("Applying NodeParamters took %d ms (%s)", delay, nnc.getName());
                return true;
            }
        } catch (final Exception e) {
            String msg = "Dialog of node '" + nnc.getNameWithID() + "' has thrown an " + e.getClass().getSimpleName()
                + ": " + e.getMessage();
            final var error = new AssertionFailedError(msg);
            error.initCause(e);
            result.addFailure(this, error);
        } finally {
            NodeContext.removeLastContext();
        }
        return false;
    }

    private static void applyData(final NativeNodeContainer nnc,
        final DataServiceManager<NodeWrapper> dataServiceManager, final JsonNode data,
        final Map<SettingsType, VariableSettingsRO> variableSettings, final NodeParametersInput input)
        throws JsonProcessingException, InvalidSettingsException {
        final var applyData = toApplyData(data, variableSettings, input);
        final var nodeWrapper = NodeWrapper.of(nnc);
        final var applyDataResult = dataServiceManager.callApplyDataService(nodeWrapper, applyData);
        // "isApplied" comes from {@link ApplyDataService#IS_APPLIED}
        final var isApplied = MAPPER.readTree(applyDataResult).path("isApplied").asBoolean(false);
        if (!isApplied) {
            // "error" comes from {@link ApplyDataService#ERROR}
            final var errorString = MAPPER.readTree(applyDataResult).path("error").asText();

            throw new InvalidSettingsException(
                String.format("Dialog of node '%s' could not apply settings via NodeParameters. Error message: %s",
                    nnc.getNameWithID(), errorString));
        }

    }

    private static String toApplyData(final JsonNode data, final Map<SettingsType, VariableSettingsRO> variableSettings,
        final NodeParametersInput input) throws JsonProcessingException {
        final var applyData = MAPPER.createObjectNode();
        applyData.set("data", data);
        VariableSettingsUtil.addVariableSettingsToRootJson(applyData, variableSettings, input);
        return MAPPER.writeValueAsString(applyData);
    }

}
