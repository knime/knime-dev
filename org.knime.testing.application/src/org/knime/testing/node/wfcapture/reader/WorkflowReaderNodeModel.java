/*
 * ------------------------------------------------------------------------
 *
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
 *   Feb 10, 2020 (hornm): created
 */
package org.knime.testing.node.wfcapture.reader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.dialog.InputNode;
import org.knime.core.node.dialog.OutputNode;
import org.knime.core.node.exec.dataexchange.PortObjectIDSettings;
import org.knime.core.node.exec.dataexchange.in.PortObjectInNodeModel;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.node.workflow.capture.WorkflowPortObject;
import org.knime.core.node.workflow.capture.WorkflowPortObjectSpec;
import org.knime.core.node.workflow.capture.WorkflowSegment;
import org.knime.core.node.workflow.capture.WorkflowSegment.Input;
import org.knime.core.node.workflow.capture.WorkflowSegment.Output;
import org.knime.core.node.workflow.capture.WorkflowSegment.PortID;
import org.knime.core.util.LockFailedException;
import org.knime.filehandling.core.connections.knimerelativeto.RelativeToUtil;
import org.knime.filehandling.core.node.portobject.reader.PortObjectFromPathReaderNodeModel;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;

/**
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
final class WorkflowReaderNodeModel extends PortObjectFromPathReaderNodeModel<WorkflowReaderNodeConfig> {

    protected WorkflowReaderNodeModel(final NodeCreationConfiguration creationConfig) {
        super(creationConfig, new WorkflowReaderNodeConfig(creationConfig));
    }

    @Override
    protected PortObject[] readFromPath(final Path inputPath, final ExecutionContext exec) throws Exception {
        File wfFile = getAbsoluteWorkflowFile(inputPath, getConfig().getWorkflowName().getStringValue());
        WorkflowManager wfm = readWorkflow(wfFile, exec);
        List<Input> inputs;
        List<Output> outputs;
        if (getConfig().getRemoveIONodes().getBooleanValue()) {
            inputs = new ArrayList<>();
            outputs = new ArrayList<>();
            removeAndCollectContainerInputsAndOutputs(wfm, inputs, outputs);
        } else {
            inputs = Collections.emptyList();
            outputs = Collections.emptyList();
        }
        if (getConfig().getUpdatePortObjectReaderRefs().getBooleanValue()) {
            updatePortObjectReferenceReaderPaths(wfm, wfFile);
        }
        WorkflowSegment ws =
            new WorkflowSegment(wfm, inputs, outputs, Collections.emptySet());
        try {
            return new PortObject[]{new WorkflowPortObject(new WorkflowPortObjectSpec(ws, null,
                inputs.stream().map(i -> i.getConnectedPorts().toString()).collect(Collectors.toList()),
                outputs.stream().map(o -> o.getConnectedPort().toString()).collect(Collectors.toList())))};
        } finally {
            ws.serializeAndDisposeWorkflow();
        }
    }

    private static WorkflowManager readWorkflow(final File wfFile, final ExecutionContext exec)
        throws IOException, InvalidSettingsException, CanceledExecutionException, UnsupportedWorkflowVersionException,
        LockFailedException {

        final WorkflowLoadHelper loadHelper = new WorkflowLoadHelper(wfFile);
        final WorkflowLoadResult loadResult =
            WorkflowManager.EXTRACTED_WORKFLOW_ROOT.load(wfFile, exec, loadHelper, false);

        final WorkflowManager m = loadResult.getWorkflowManager();
        if (m == null) {
            throw new IOException(
                "Errors reading workflow: " + loadResult.getFilteredError("", LoadResultEntryType.Ok));
        } else {
            if (loadResult.getType() != LoadResultEntryType.Ok) {
                WorkflowManager.EXTRACTED_WORKFLOW_ROOT.removeProject(m.getID());
                throw new IOException("Errors reading workflow: " + wfFile.toString());
            }
        }
        return loadResult.getWorkflowManager();
    }

    private File getAbsoluteWorkflowFile(final Path inputPath, final String wfName)
        throws URISyntaxException, IOException {
        // this is fairly hacky and likely won't work universally (e.g., when node is executed on the server)
        // should we ever decide to release this node, we should instead add a method to the WorkflowAware interface
        switch (getConfig().getFileChooserModel().getLocation().getFSCategory()) {
            case LOCAL:
                return inputPath.resolve(wfName).toFile();
            case RELATIVE:
                // the way the destination path is determined here follows the same code path as
                // LocalRelativeToFileSystemProvider#deployWorkflow -> ExplorerMountPointFileSystemAccess#deployWorkflow
                final Path src = inputPath.resolve(wfName);
                final Path absoluteSrc = src.toAbsolutePath().normalize();
                final WorkflowContext context = RelativeToUtil.getWorkflowContext();
                CheckUtils.checkState(context.getMountpointURI().isPresent(),
                    "Cannot determine name of mountpoint to deploy workflow.");
                final String currentMountpoint = context.getMountpointURI().get().getAuthority();
                final URI uri = new URI("knime", currentMountpoint, absoluteSrc.toString(), null);
                final AbstractExplorerFileStore store = ExplorerMountTable.getFileSystem().getStore(uri);
                try {
                    return store.toLocalFile();
                } catch (CoreException e) {
                    throw new IOException(e);
                }
            case MOUNTPOINT:
            case CUSTOM_URL:
            case CONNECTED:
            default:
                throw new UnsupportedOperationException("File system not yet implemented for this node.");
        }
    }

    private static void removeAndCollectContainerInputsAndOutputs(final WorkflowManager wfm, final List<Input> inputs,
        final List<Output> outputs) {
        List<NodeID> nodesToRemove = new ArrayList<>();
        for (NodeContainer nc : wfm.getNodeContainers()) {
            if (nc instanceof NativeNodeContainer) {
                NativeNodeContainer nnc = (NativeNodeContainer)nc;
                if (collectInputs(wfm, inputs, nnc) || collectOutputs(wfm, outputs, nnc)) {
                    nodesToRemove.add(nnc.getID());
                }
            }
        }
        nodesToRemove.forEach(wfm::removeNode);
    }

    private static boolean collectOutputs(final WorkflowManager wfm, final List<Output> outputs,
        final NativeNodeContainer nnc) {
        if (nnc.getNodeModel() instanceof OutputNode) {
            for (ConnectionContainer cc : wfm.getIncomingConnectionsFor(nnc.getID())) {
                outputs.add(new Output(nnc.getInPort(cc.getDestPort()).getPortType(), null,
                    new PortID(NodeIDSuffix.create(wfm.getID(), cc.getSource()), cc.getSourcePort())));
            }
            return true;
        } else {
            return false;
        }
    }

    private static boolean collectInputs(final WorkflowManager wfm, final List<Input> inputs,
        final NativeNodeContainer nnc) {
        if (nnc.getNodeModel() instanceof InputNode) {
            for (int i = 0; i < nnc.getNrOutPorts(); i++) {
                Set<PortID> ports = wfm.getOutgoingConnectionsFor(nnc.getID(), i).stream()
                    .map(cc -> new PortID(NodeIDSuffix.create(wfm.getID(), cc.getDest()), cc.getDestPort()))
                    .collect(Collectors.toSet());
                if (!ports.isEmpty()) {
                    inputs.add(new Input(nnc.getOutputType(i), null, ports));
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /*
     * Ugly HACK: Changes the port reference reader such that they have an absolute reference to the data in located in
     * the directory of the workflow which is read in.
     *
     * A release-able version of the Workflow Reader needs to read the data in, store it as part of its execution
     * context and let the Reference Reader nodes reference it via the PortObjectRepository.
     */
    private static void updatePortObjectReferenceReaderPaths(final WorkflowManager wfm, final File wfFile)
        throws InvalidSettingsException {
        for (NodeContainer nc : wfm.getNodeContainers()) {
            if (nc instanceof NativeNodeContainer
                && ((NativeNodeContainer)nc).getNodeModel() instanceof PortObjectInNodeModel) {
                PortObjectInNodeModel portObjectReader =
                    (PortObjectInNodeModel)((NativeNodeContainer)nc).getNodeModel();
                final PortObjectIDSettings poSettings = portObjectReader.getInputNodeSettingsCopy();
                URI uri = poSettings.getUri();

                URI absoluteDataPath = new File(wfFile, uri.toString().replace("knime://knime.workflow", "")).toURI();
                poSettings.setFileReference(absoluteDataPath, poSettings.isTable());

                final NodeSettings settings = new NodeSettings("root");
                wfm.saveNodeSettings(nc.getID(), settings);
                final NodeSettingsWO modelSettings = settings.addNodeSettings("model");
                poSettings.saveSettings(modelSettings);
                wfm.loadNodeSettings(nc.getID(), settings);
            }
        }
    }

}
