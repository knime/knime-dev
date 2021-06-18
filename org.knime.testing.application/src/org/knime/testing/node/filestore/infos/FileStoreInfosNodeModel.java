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
 *   Jun 18, 2021 (hornm): created
 */
package org.knime.testing.node.filestore.infos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.internal.IFileStoreHandler;
import org.knime.core.data.filestore.internal.IWriteFileStoreHandler;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowDataRepository;
import org.knime.core.node.workflow.WorkflowManager;

/**
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class FileStoreInfosNodeModel extends NodeModel {

    /**
     */
    protected FileStoreInfosNodeModel() {
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
        WorkflowManager wfm = NodeContext.getContext().getNodeContainer().getParent();
        pushFlowVariableInt("file store handler count", getWriteFileStoreHandlers(wfm).size());
        BufferedDataContainer container = exec.createDataContainer(createSpec());
        for (NodeContainer nc : wfm.getNodeContainers()) {
            IWriteFileStoreHandler fsh = getFileStoreHandler(nc);
            if (fsh != null && fsh.getBaseDir() != null) {
                File dir = fsh.getBaseDir();
                List<File> files = collectFilesInDirectory(dir, null);
                DataRow row = new DefaultRow(nc.getID().toString(), //
                    new StringCell(nc.getName()), //
                    new StringCell(dir.getAbsolutePath()), //
                    CollectionCellFactory.createSetCell(
                        files.stream().map(File::getAbsolutePath).map(StringCell::new).collect(Collectors.toList())), //
                    new IntCell(files.size()), //
                    new StringCell(fsh.getClass().getSimpleName()));
                container.addRowToTable(row);
            }
        }
        container.close();
        return new BufferedDataTable[]{container.getTable()};
    }

    private static DataTableSpec createSpec() {
        return new DataTableSpec(//
            ar("node name", "file store base dir", "files", "file count", "file store handler class"), //
            ar(StringCell.TYPE, StringCell.TYPE, SetCell.getCollectionType(StringCell.TYPE), IntCell.TYPE,
                StringCell.TYPE));
    }

    private static <T> T[] ar(final T... a) {
        return a;
    }

    private static IWriteFileStoreHandler getFileStoreHandler(final NodeContainer nc) {
        if (nc instanceof NativeNodeContainer) {
            IFileStoreHandler fsh = ((NativeNodeContainer)nc).getNode().getFileStoreHandler();
            if (fsh instanceof IWriteFileStoreHandler) {
                return (IWriteFileStoreHandler)fsh;
            }
        }
        return null;
    }

    private static List<File> collectFilesInDirectory(final File directory, List<File> files) {
        if (files == null) {
            files = new ArrayList<>();
        }
        for (File child : directory.listFiles()) {
            if (child.isDirectory()) {
                collectFilesInDirectory(child, files);
            } else {
                files.add(child);
            }
        }
        return files;
    }

    private static Collection<IWriteFileStoreHandler> getWriteFileStoreHandlers(final WorkflowManager wfm) {
        WorkflowDataRepository dataRepository = wfm.getWorkflowDataRepository();
        return dataRepository.getWriteFileStoreHandlers();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO Auto-generated method stub

    }

}
