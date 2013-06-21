/* Created on 08.01.2007 13:42:40 by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
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
import java.util.Random;

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
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{inSpecs[0], inSpecs[0], inSpecs[0]};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataTable origTable = inData[0];
        BufferedDataContainer emptyTable = exec.createDataContainer(
                inData[0].getDataTableSpec());
        emptyTable.close();
        BufferedDataContainer missingValueTable = exec
                .createDataContainer(inData[0].getDataTableSpec());


        // check if at least one random missing value will be inserted into the output table
        // if not, enforce a missing value in the first cell
        Random r = new Random(12345678);
        boolean missingValueCreated = false;
        for (int i = 0; i < inData[0].getRowCount(); i++) {
            if (r.nextDouble() < 0.1) {
                missingValueCreated = true;
                break;
            }
        }

        int count = 0;
        r = new Random(12345678);
        for (DataRow row : inData[0]) {
            exec.setProgress(count++ / (double) inData[0].getRowCount());
            DataCell[] cells = new DataCell[row.getNumCells()];
            for (int i = 0; i < cells.length; i++) {
                if ((r.nextDouble() < 0.1) || !missingValueCreated) {
                    cells[i] = DataType.getMissingCell();
                    missingValueCreated = true;
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
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // nothing to do
    }
}
