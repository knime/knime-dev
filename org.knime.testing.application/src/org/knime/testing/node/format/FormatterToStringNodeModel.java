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
 *   Feb 28, 2025 (gerlingr): created
 */
package org.knime.testing.node.format;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;

/**
 * @author Robin Gerling
 */
public class FormatterToStringNodeModel extends NodeModel {

    @SuppressWarnings("javadoc")
    protected FormatterToStringNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{getColumnSpecsWithAttachedFormatters(inSpecs[0])};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inObjects, final ExecutionContext exec)
        throws Exception {
        final var inDTSpec = inObjects[0].getDataTableSpec();
        final var outDTSpec = getColumnSpecsWithAttachedFormatters(inObjects[0].getSpec());
        final var container = exec.createDataContainer(outDTSpec);

        try (final var iterator = inObjects[0].iterator()) {
            while (iterator.hasNext()) {
                final var row = iterator.next();
                container.addRowToTable(new DefaultRow(row.getKey(), getRowContent(row, inDTSpec)));
            }
        }

        container.close();
        return new BufferedDataTable[]{container.getTable()};
    }

    private static List<DataCell> getRowContent(final DataRow row, final DataTableSpec inDTSpec) {
        final var rowDataCells = new ArrayList<DataCell>(inDTSpec.getNumColumns());
        for (var ind = 0; ind < inDTSpec.getNumColumns(); ind++) {
            final var currColSpec = inDTSpec.getColumnSpec(ind);
            final var colValue = row.getCell(ind);
            rowDataCells.add(colValue);
            if (currColSpec.getValueFormatHandler() != null) {
                rowDataCells.add(new StringCell(currColSpec.getValueFormatHandler().get(colValue)));
            }
        }
        return rowDataCells;
    }

    private static DataTableSpec getColumnSpecsWithAttachedFormatters(final DataTableSpec dts) {
        final List<DataColumnSpec> dataColumnSpecs = new ArrayList<>();
        for (var ind = 0; ind < dts.getNumColumns(); ind++) {
            final var currColSpec = dts.getColumnSpec(ind);
            dataColumnSpecs.add(currColSpec);
            if (currColSpec.getValueFormatHandler() != null) {
                dataColumnSpecs.add(
                    new DataColumnSpecCreator(currColSpec.getName() + " (formatted)", StringCell.TYPE).createSpec());
            }
        }
        return new DataTableSpec(dataColumnSpecs.toArray(DataColumnSpec[]::new));
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
