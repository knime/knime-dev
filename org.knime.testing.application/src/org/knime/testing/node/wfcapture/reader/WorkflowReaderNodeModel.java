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

import static java.util.Collections.emptyList;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.node.workflow.capture.WorkflowFragment;
import org.knime.core.node.workflow.capture.WorkflowPortObject;
import org.knime.core.node.workflow.capture.WorkflowPortObjectSpec;
import org.knime.core.util.FileUtil;
import org.knime.filehandling.core.defaultnodesettings.SettingsModelFileChooser2;

/**
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
class WorkflowReaderNodeModel extends NodeModel {

    static final SettingsModelFileChooser2 createSelectedWorkflowModel() {
        return new SettingsModelFileChooser2("selected_workflow");
    }

    private final SettingsModelFileChooser2 m_selectedWorkflow = createSelectedWorkflowModel();

    protected WorkflowReaderNodeModel() {
        super(new PortType[0], new PortType[]{WorkflowPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        WorkflowFragment wf = new WorkflowFragment(readWorkflow(exec), Collections.emptyList(), Collections.emptyList(),
            Collections.emptySet());
        try {
            return new PortObject[]{
                new WorkflowPortObject(new WorkflowPortObjectSpec(wf, null, emptyList(), emptyList()))};
        } finally {
            wf.serializeAndDisposeWorkflow();
        }
    }

    private WorkflowManager readWorkflow(final ExecutionContext exec) throws Exception {
        File wfFile = FileUtil.resolveToPath(new URL(m_selectedWorkflow.getPathOrURL().replace(" ", "%20"))).toFile();
        WorkflowLoadHelper loadHelper = new WorkflowLoadHelper(wfFile);
        WorkflowLoadResult loadResult = WorkflowManager.EXTRACTED_WORKFLOW_ROOT.load(wfFile, exec, loadHelper, false);
        WorkflowManager m = loadResult.getWorkflowManager();
        if (m == null) {
            throw new Exception("Errors reading workflow: " + loadResult.getFilteredError("", LoadResultEntryType.Ok));
        } else {
            switch (loadResult.getType()) {
                case Ok:
                    break;
                default:
                    WorkflowManager.EXTRACTED_WORKFLOW_ROOT.removeProject(m.getID());
                    throw new Exception("Errors reading workflow: " + m_selectedWorkflow);
            }
        }
        return loadResult.getWorkflowManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_selectedWorkflow.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_selectedWorkflow.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_selectedWorkflow.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //
    }

}
