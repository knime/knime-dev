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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jun 27, 2012 (wiswedel): created
 */
package org.knime.testing.node.filestore.create;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelLong;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.knime.testing.data.filestore.LargeFile;
import org.knime.testing.data.filestore.LargeFileStoreCell;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class FileStoreCreateNodeModel extends SimpleStreamableFunctionNodeModel {

    private final SettingsModelBoolean m_keepInMemorySettingsModel = createKeepInMemorySettingsModel();

    private final SettingsModelIntegerBounded m_numFileStoresSettingsModel = createNumFileStoresSettingsModel();

    /** {@inheritDoc} */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) {
        final ColumnRearranger r = new ColumnRearranger(spec);
        final String name = DataTableSpec.getUniqueColumnName(spec, "large-file-store");
        final DataColumnSpec s = new DataColumnSpecCreator(name, LargeFileStoreCell.TYPE).createSpec();
        final boolean keepInMemory = m_keepInMemorySettingsModel.getBooleanValue();
        r.append(new SingleCellFactory(s) {
            @Override
            public DataCell getCell(final DataRow row) {
                if (m_numFileStoresSettingsModel.getIntValue() == 0) {
                    return new LargeFileStoreCell();
                }

                LargeFile lf;
                final long seed = Double.doubleToLongBits(Math.random());
                try {
                    final FileStore fs = getFileStoreFactory().createFileStore(row.getKey().getString());
                    lf = LargeFile.create(fs, seed, keepInMemory);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
                if (m_numFileStoresSettingsModel.getIntValue() > 1) {
                    try {
                        final FileStore fs2 = getFileStoreFactory().createFileStore("other" + row.getKey().getString());
                        LargeFile lf2 = LargeFile.create(fs2, seed >> 1, keepInMemory);
                        return new LargeFileStoreCell(lf, lf2, seed);
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return new LargeFileStoreCell(lf, seed);
            }
        });
        return r;
    }

    /**
     * @return
     */
    static final SettingsModelLong createSeedModel() {
        return new SettingsModelLong("seed", 12345678L);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_keepInMemorySettingsModel.saveSettingsTo(settings);
        m_numFileStoresSettingsModel.saveSettingsTo(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // m_keepInMemorySettingsModel.validate -- don't do it, added later
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            m_keepInMemorySettingsModel.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            m_keepInMemorySettingsModel.setBooleanValue(false);
        }
        try {
            m_numFileStoresSettingsModel.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            m_numFileStoresSettingsModel.setIntValue(1);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // TODO Auto-generated method stub

    }

    static SettingsModelBoolean createKeepInMemorySettingsModel() {
        return new SettingsModelBoolean("keepInMemory", false);
    }


    static SettingsModelIntegerBounded createNumFileStoresSettingsModel() {
        return new SettingsModelIntegerBounded("numFileStores", 1, 0, 2);
    }
}
