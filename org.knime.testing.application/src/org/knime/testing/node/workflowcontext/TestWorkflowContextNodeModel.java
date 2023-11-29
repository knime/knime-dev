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
 *   07.09.2011 (meinl): created
 */
package org.knime.testing.node.workflowcontext;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.node.workflow.contextv2.ServerLocationInfo;
import org.knime.core.util.auth.Authenticator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class TestWorkflowContextNodeModel extends NodeModel {

    /**
     * The authenticator might leak sensitive information or throw an exception during serialization if not
     * authenticated.
     */
    private interface IgnoreAuthenticator {
        @JsonIgnore
        abstract Authenticator getAuthenticator();
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    static {
        MAPPER.addMixIn(HubSpaceLocationInfo.class, IgnoreAuthenticator.class);
        MAPPER.addMixIn(ServerLocationInfo.class, IgnoreAuthenticator.class);
        MAPPER.registerModule(new Jdk8Module()); // See AP-21578
    }

    private static final DataColumnSpec LOCATION_INFO =
        new DataColumnSpecCreator("Location Information", JSONCell.TYPE).createSpec();

    private static final DataColumnSpec EXECUTOR_INFO =
        new DataColumnSpecCreator("Executor Information", JSONCell.TYPE).createSpec();

    private static final DataColumnSpec WORKFLOW_CONTEXT_DUMP =
        new DataColumnSpecCreator("Raw Information", StringCell.TYPE).createSpec();

    private static final DataTableSpec OUTPUT_SPEC =
        new DataTableSpecCreator().addColumns(LOCATION_INFO, EXECUTOR_INFO, WORKFLOW_CONTEXT_DUMP).createSpec();

    /**
     * Creates a new node model.
     */
    public TestWorkflowContextNodeModel() {
        super(0, 1);
    }

    /**
     * The first output table contains location info. The second output table contains the executor info.
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{OUTPUT_SPEC};
    }

    /**
     * Fetch the current workflow context properties and fail if they do not match the expected properties.
     *
     * {@inheritDoc}
     *
     * @throws IOException
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws IOException {

        final var context = NodeContext.getContext().getWorkflowManager().getContextV2();

        NodeLogger.getLogger(getClass()).info(String.format("WorkflowContextV2%n%s", context));

        // create JSON representation of location and executor info
        var locationJson = MAPPER.writeValueAsString(context.getLocationInfo());
        var executorJson = MAPPER.writeValueAsString(context.getExecutorInfo());

        // create output table (currently there's no JSON flow variable type, so we go with a one row table)
        final var row = new DefaultRow("WorkflowContextProperties", JSONCellFactory.create(locationJson),
            JSONCellFactory.create(executorJson), StringCellFactory.create(context.toString()));
        var container = exec.createDataContainer(OUTPUT_SPEC);
        container.addRowToTable(row);
        container.close();

        return new BufferedDataTable[]{container.getTable()};
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // no configuration to validate
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // no configuration to load
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // no configuration to persist
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // Nothing to do
    }

    @Override
    protected void reset() {
        // Nothing to do
    }

}
