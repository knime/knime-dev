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
package org.knime.testing.node.wfcapture.workflow2table;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.base.JSONConfig;
import org.knime.core.node.config.base.JSONConfig.WriterConfig;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.capture.WorkflowFragment;
import org.knime.core.node.workflow.capture.WorkflowPortObject;

/**
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
class Workflow2TableNodeModel extends NodeModel {

    Workflow2TableNodeModel() {
        super(new PortType[]{WorkflowPortObject.TYPE}, new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE});
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
        WorkflowFragment wf = ((WorkflowPortObject)inObjects[0]).getSpec().getWorkflowFragment();
        WorkflowManager wfm = wf.loadWorkflow();
        try {
            BufferedDataContainer nodeDC = exec.createDataContainer(createNodeTableSpec());
            //TODO support nested workflows
            for (NodeContainer nc : wfm.getNodeContainers()) {
                nodeDC.addRowToTable(createRowFromNode(nc));
            }
            nodeDC.close();

            BufferedDataContainer connectionDC = exec.createDataContainer(createConnectionTableSpec());
            //TODO support nested workflows
            int i = 0;
            for (ConnectionContainer cc : wfm.getConnectionContainers()) {
                connectionDC.addRowToTable(createRowFromConnection(cc, i, wfm.getID()));
                i++;
            }
            connectionDC.close();
            return new PortObject[]{nodeDC.getTable(), connectionDC.getTable()};
        } finally {
            wf.disposeWorkflow();
        }
    }

    private static DataRow createRowFromNode(final NodeContainer nc) throws InvalidSettingsException, IOException {
        int[] bounds = nc.getUIInformation().getBounds();
        NodeSettings settings = new NodeSettings("settings");
        nc.getParent().saveNodeSettings(nc.getID(), settings);
        StringWriter settingsString = new StringWriter();
        JSONConfig.writeJSON(settings, settingsString, WriterConfig.PRETTY);
        return new DefaultRow(NodeIDSuffix.create(nc.getParent().getID(), nc.getID()).toString(),
            new StringCell(nc.getName()), new StringCell(nc.getNodeAnnotation().getText()),
            new StringCell(settingsString.toString()), new IntCell(bounds[0]), new IntCell(bounds[1]),
            new IntCell(bounds[2]), new IntCell(bounds[3]));
    }

    private static DataRow createRowFromConnection(final ConnectionContainer cc, final int idx, final NodeID parent) {
        return new DefaultRow("connection " + idx,
            new StringCell(NodeIDSuffix.create(parent, cc.getSource()).toString()), new IntCell(cc.getSourcePort()),
            new StringCell(NodeIDSuffix.create(parent, cc.getDest()).toString()), new IntCell(cc.getDestPort()));
    }

    private static DataTableSpec createNodeTableSpec() {
        DataColumnSpec[] colSpecs =
            new DataColumnSpec[]{new DataColumnSpecCreator("node name", StringCell.TYPE).createSpec(),
                new DataColumnSpecCreator("annotation", StringCell.TYPE).createSpec(),
                new DataColumnSpecCreator("settings", StringCell.TYPE).createSpec(),
                new DataColumnSpecCreator("x", IntCell.TYPE).createSpec(),
                new DataColumnSpecCreator("y", IntCell.TYPE).createSpec(),
                new DataColumnSpecCreator("w", IntCell.TYPE).createSpec(),
                new DataColumnSpecCreator("h", IntCell.TYPE).createSpec()};
        return new DataTableSpec(colSpecs);
    }

    private static DataTableSpec createConnectionTableSpec() {
        DataColumnSpec[] colSpecs =
            new DataColumnSpec[]{new DataColumnSpecCreator("source node id suffix", StringCell.TYPE).createSpec(),
                new DataColumnSpecCreator("source port index", IntCell.TYPE).createSpec(),
                new DataColumnSpecCreator("destination node id suffix", StringCell.TYPE).createSpec(),
                new DataColumnSpecCreator("destination port index", IntCell.TYPE).createSpec()};
        return new DataTableSpec(colSpecs);
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
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //
    }

}
