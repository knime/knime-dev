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
package org.knime.testing.data.filestore;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;

/**
 * A cell implementation that mimics a file store cell - it has "large" binary content with some information.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public final class LargeFileStoreCell extends FileStoreCell implements LargeFileStoreValue {
    /**
     * Serializer for {@link LargeFileStoreCell}s.
     */
    public static final class Serializer implements DataCellSerializer<LargeFileStoreCell> {
        /** {@inheritDoc} */
        @Override
        public LargeFileStoreCell deserialize(final DataCellDataInput input) throws IOException {
            long seed = input.readLong();
            return new LargeFileStoreCell(seed);
        }

        /** {@inheritDoc} */
        @Override
        public void serialize(final LargeFileStoreCell cell, final DataCellDataOutput output) throws IOException {
            if (null != cell.m_largeFile) {
                cell.m_largeFile.flushToFileStore(); // does nothing if already written (handles "keepInMemory")
            }
            if (null != cell.m_otherLargeFile) {
                cell.m_otherLargeFile.flushToFileStore();
            }
            output.writeLong(cell.m_seed);
        }
    }

    public static final DataType TYPE = DataType.getType(LargeFileStoreCell.class);

    private final long m_seed;

    private LargeFile m_largeFile;

    private LargeFile m_otherLargeFile;

    /** {@inheritDoc} */
    @Override
    public LargeFile getLargeFile() {
        return m_largeFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LargeFile getOtherLargeFile() {
        return m_otherLargeFile;
    }

    /** {@inheritDoc} */
    @Override
    public long getSeed() {
        return m_seed;
    }

    /**
     * Deserialization constructor.
     *
     * @param input
     * @throws IOException
     */
    LargeFileStoreCell(final long seed) throws IOException {
        m_seed = seed;
    }

    /**
     * @param largeFile
     * @param seed the expected seed as hidden in largeFile.
     */
    public LargeFileStoreCell(final LargeFile largeFile, final long seed) {
        super(largeFile.getFileStore());
        m_largeFile = largeFile;
        m_seed = seed;
    }

    /**
     * @param largeFile
     * @param otherLargeFile a second file with the same seed
     * @param seed the expected seed as hidden in largeFile.
     */
    public LargeFileStoreCell(final LargeFile largeFile, final LargeFile otherLargeFile, final long seed) {
        super(new FileStore[]{largeFile.getFileStore(), otherLargeFile.getFileStore()});
        m_largeFile = largeFile;
        m_otherLargeFile = otherLargeFile;
        m_seed = seed;
    }

    /**
     * Constructor that creates a LageFileStoreCell with no FileStores to check that
     * (de)serialization also works in that case.
     */
    public LargeFileStoreCell() {
        super();
        m_largeFile = null;
        m_seed = 0;
    }

    @Override
    protected void postConstruct() throws IOException {
        final FileStore[] fileStores = getFileStores();
        if (fileStores.length < 1) {
            return;
        }

        m_largeFile = LargeFile.restore(fileStores[0]);
        if (fileStores.length == 2) {
            m_otherLargeFile = LargeFile.restore(fileStores[1]);
        }
    }

    @Override
    protected void flushToFileStore() throws IOException {
        if (null != m_largeFile) {
            m_largeFile.flushToFileStore();
        }
        if (null != m_otherLargeFile) {
            m_otherLargeFile.flushToFileStore();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        try {
            if (getOtherLargeFile() != null) {
                return "Content of LargeFile 1: " + getLargeFile().read() + ", LargeFile 2: "
                    + getOtherLargeFile().read();
            } else if (getLargeFile() != null) {
                return "Content of LargeFile: " + getLargeFile().read();
            } else {
                return "LargeFile without contents";
            }
        } catch (Exception e) {
            throw new IllegalStateException("Large file not accessible!", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        final LargeFileStoreCell odc = (LargeFileStoreCell)dc;
        return odc.m_seed == m_seed && super.equalsDataCell(dc);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Long.hashCode(m_seed);
    }
}
