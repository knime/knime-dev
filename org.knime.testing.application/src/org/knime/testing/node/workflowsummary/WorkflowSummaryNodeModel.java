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
 *   Mar 4, 2020 (hornm): created
 */
package org.knime.testing.node.workflowsummary;

import static org.knime.core.util.workflowsummary.WorkflowSummaryGenerator.generate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.workflowsummary.WorkflowSummaryConfiguration;
import org.knime.core.util.workflowsummary.WorkflowSummaryConfiguration.SummaryFormat;

/**
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
class WorkflowSummaryNodeModel extends NodeModel {

    static SettingsModelStringArray createNodesToIgnoreModel() {
        return new SettingsModelStringArray("nodes_to_ignore", new String[0]);
    }

    static SettingsModelBoolean createIncludeExecutionInfoModel() {
        return new SettingsModelBoolean("include_execution_info", false);
    }

    private final SettingsModelStringArray m_nodesToIgnore = createNodesToIgnoreModel();

    private final SettingsModelBoolean m_includeExecutionInfo = createIncludeExecutionInfoModel();

    WorkflowSummaryNodeModel() {
        super(0, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {
        WorkflowManager wfm = NodeContext.getContext().getWorkflowManager();
        if (wfm != null) {
            List<NodeID> nodesToIgnore = Arrays.stream(m_nodesToIgnore.getStringArrayValue())
                .map(s -> NodeIDSuffix.fromString(s.substring(s.indexOf("(#") + 2, s.length() - 1)))
                .map(s -> s.prependParent(wfm.getID())).collect(Collectors.toList());
            WorkflowSummaryConfiguration xmlConfig = WorkflowSummaryConfiguration.builder(SummaryFormat.XML)
                .nodesToIgnore(nodesToIgnore).includeExecutionInfo(m_includeExecutionInfo.getBooleanValue()).build();
            WorkflowSummaryConfiguration jsonConfig = WorkflowSummaryConfiguration.builder(SummaryFormat.JSON)
                .nodesToIgnore(nodesToIgnore).includeExecutionInfo(m_includeExecutionInfo.getBooleanValue()).build();
            try (ByteArrayOutputStream outXML = new ByteArrayOutputStream();
                    ByteArrayOutputStream outJSON = new ByteArrayOutputStream()) {
                generate(wfm, outXML, xmlConfig);
                generate(wfm, outJSON, jsonConfig);
                BufferedDataContainer container = exec.createDataContainer(createSpec());
                container.addRowToTable(new DefaultRow("summary", XMLCellFactory.create(outXML.toString()),
                    JSONCellFactory.create(outJSON.toString(), false)));
                container.close();
                return new BufferedDataTable[]{container.getTable()};
            }
        } else {
            throw new IllegalStateException("No workflow context available");
        }
    }

    private static DataTableSpec createSpec() {
        return new DataTableSpec(new String[]{"XML workflow summary", "JSON workflow summary"},
            new DataType[]{XMLCell.TYPE, JSONCell.TYPE});
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
        m_nodesToIgnore.saveSettingsTo(settings);
        m_includeExecutionInfo.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_nodesToIgnore.validateSettings(settings);
        m_includeExecutionInfo.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_nodesToIgnore.loadSettingsFrom(settings);
        m_includeExecutionInfo.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //
    }

}
