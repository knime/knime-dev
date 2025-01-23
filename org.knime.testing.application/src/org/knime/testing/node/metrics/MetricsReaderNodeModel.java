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
 *   Jan 26, 2023 (wiswedel): created
 */
package org.knime.testing.node.metrics;

import java.io.IOException;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.value.ValueInterfaces.DoubleWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.StringWriteValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * Model for "Metrics Reader" node.
 * @author wiswedel
 */
@SuppressWarnings("restriction")
final class MetricsReaderNodeModel extends WebUINodeModel<MetricsReaderNodeSettings> {

    MetricsReaderNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, MetricsReaderNodeSettings.class);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final MetricsReaderNodeSettings s)
        throws InvalidSettingsException {
        DataTableSpec outputSpec;
        if (s.m_outputRepresentation == MetricsReaderNodeSettings.OutputRepresentation.Column) {
            outputSpec = createSpecInSingleColumn();
        } else {
            outputSpec = createSpecInManyColumns();
        }
        return new DataTableSpec[] {outputSpec};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final MetricsReaderNodeSettings s) throws Exception {
        BufferedDataTable table;
        if (s.m_outputRepresentation == MetricsReaderNodeSettings.OutputRepresentation.Column) {
            table = createTableInSingleColumn(exec);
        } else {
            table = createTableInManyColumns(exec);
        }
        return new BufferedDataTable[] {table};
    }

    private static DataTableSpec createSpecInSingleColumn() {
        return new DataTableSpec(new String[] {"metric", "value"}, new DataType[] {StringCell.TYPE, DoubleCell.TYPE});
    }

    private static BufferedDataTable createTableInSingleColumn(final ExecutionContext context) throws IOException {
        try (final var container = context.createRowContainer(createSpecInSingleColumn());
                final var cursor = container.createCursor()) {
            final var rowBuffer = container.createRowBuffer();
            final var rowIDLong = new MutableLong();
            Metrics.availableMetrics() //
            .forEach(metric -> {
                rowBuffer.setRowKey(RowKey.createRowKey(rowIDLong.getAndIncrement()));
                rowBuffer.<StringWriteValue>getWriteValue(0).setStringValue(metric.name());
                rowBuffer.<DoubleWriteValue>getWriteValue(1).setDoubleValue(metric.supplier().get().doubleValue());
                cursor.commit(rowBuffer);
            });
            return container.finish();
        }
    }

    private static DataTableSpec createSpecInManyColumns() {
        DataColumnSpec[] columnSpecs = Metrics.availableMetrics() //
                .map(metric -> new DataColumnSpecCreator(metric.name(), metric.type())) //
                .map(DataColumnSpecCreator::createSpec) //
                .toArray(DataColumnSpec[]::new);
        return new DataTableSpec(columnSpecs);
    }

    private static BufferedDataTable createTableInManyColumns(final ExecutionContext context) throws IOException {
        try (var container = context.createRowContainer(createSpecInManyColumns());
                var cursor = container.createCursor()) {
            final var rowBuffer = container.createRowBuffer();
            final var colIndexInt = new MutableInt();
            rowBuffer.setRowKey(RowKey.createRowKey(0L));
            Metrics.availableMetrics()
                .forEach(metric -> metric.writeTo(rowBuffer.getWriteValue(colIndexInt.getAndIncrement())));
            cursor.commit(rowBuffer);
            return container.finish();
        }
    }

}
