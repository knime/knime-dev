/* Created on 08.01.2007 13:42:40 by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ------------------------------------------------------------------- * 
 */
package org.knime.testing.node.disturber;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class DisturberNodeModel extends NodeModel {
    /**
     * Creates a model for the disturber node.
     */
    public DisturberNodeModel() {
        super(1, 3);
    }

    /**
     * @see org.knime.core.node.NodeModel#configure(org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{inSpecs[0], inSpecs[0], inSpecs[0]};
    }

    /**
     * @see org.knime.core.node.NodeModel#execute(org.knime.core.node.BufferedDataTable[],
     *      org.knime.core.node.ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(BufferedDataTable[] inData,
            ExecutionContext exec) throws Exception {
        BufferedDataTable origTable = inData[0];
        BufferedDataContainer emptyTable = exec.createDataContainer(
                inData[0].getDataTableSpec());
        emptyTable.close();
        BufferedDataContainer missingValueTable = exec
                .createDataContainer(inData[0].getDataTableSpec());

        int count = 0;
        for (DataRow row : inData[0]) {
            exec.setProgress(count++ / (double) inData[0].getRowCount());
            DataCell[] cells = new DataCell[row.getNumCells()];
            for (int i = 0; i < cells.length; i++) {
                if (Math.random() < 0.1) {
                    cells[i] = DataType.getMissingCell();
                } else {
                    cells[i] = row.getCell(i);
                }
            }
            missingValueTable
                    .addRowToTable(new DefaultRow(row.getKey(), cells));
        }
        missingValueTable.close();
        return new BufferedDataTable[] {
                origTable, emptyTable.getTable(), missingValueTable.getTable()
        };
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * @see org.knime.core.node.NodeModel#loadValidatedSettingsFrom(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
            throws InvalidSettingsException {
        // nothing to do
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * @see org.knime.core.node.NodeModel#saveSettingsTo(org.knime.core.node.NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(NodeSettingsWO settings) {
        // nothing to do
    }

    /**
     * @see org.knime.core.node.NodeModel#validateSettings(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void validateSettings(NodeSettingsRO settings)
            throws InvalidSettingsException {
        // nothing to do
    }
}
