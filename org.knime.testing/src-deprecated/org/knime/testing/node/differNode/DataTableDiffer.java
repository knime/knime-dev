/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by 
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
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
 * --------------------------------------------------------------------- *
 *
 * History
 *   May 18, 2006 (ritmeier): created
 */
package org.knime.testing.node.differNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DefaultTable;
import org.knime.core.data.def.StringCell;
import org.knime.testing.internal.nodes.image.ImageDifferNodeFactory;

/**
 *
 * @author ritmeier, University of Konstanz
 * @deprecated use the new image comparator {@link ImageDifferNodeFactory} and the extension point for difference
 *             checker instead
 */
@Deprecated
public class DataTableDiffer implements TestEvaluator {

    private DataTable m_dataTable1 = null;

    private DataTable m_dataTable2 = null;

    private DataTable m_diffTable;

    private double m_epsilon = 0.0;

    /**
     * Sets the maximum allowed relative deviation of reference and actual value
     * for numbers.
     *
     * @param eps the relative deviation; 0 means two values must be identical,
     *            1 means the deviation can be 100% (twice or half as large)
     */
    public void setEpsilon(final double eps) {
        m_epsilon = eps;
    }

    /**
     *
     * @throws TestEvaluationException
     * @see org.knime.testing.node.differNode.TestEvaluator#compare(
     *      org.knime.core.data.DataTable, org.knime.core.data.DataTable)
     */
    @Override
    public void compare(final DataTable table1, final DataTable table2)
            throws TestEvaluationException {

        // Compare the table specs

        if (!table1.getDataTableSpec()
                .equalStructure(table2.getDataTableSpec())) {
            // generate a helpful error message
            DataTableSpec spec1 = table1.getDataTableSpec();
            DataTableSpec spec2 = table2.getDataTableSpec();
            if (spec1.getNumColumns() != spec2.getNumColumns()) {
                throw new TestEvaluationException("Tables have different number of columns: expected "
                        + spec2.getNumColumns() + ", got " + spec1.getNumColumns());
            }
            boolean colsDiff = false;
            String msg =
                    "The following columns differ in "
                            + "name, type, and/or domain:\n";
            for (int c = 0; c < spec1.getNumColumns(); c++) {
                if (!spec1.getColumnSpec(c).equalStructure(
                        spec2.getColumnSpec(c))) {
                    colsDiff = true;
                    msg += " Col#" + c;
                }
            }
            if (colsDiff) {
                throw new TestEvaluationException(msg);
            }

            throw new TestEvaluationException("DataTableSpecs are different"
                    + " (even though DataColumnSpecs are all equal).");
        }

        // Compare the table content

        Iterator<DataRow> rowIt1 = table1.iterator();
        Iterator<DataRow> rowIt2 = table2.iterator();
        long rowNum = -1;

        while (rowIt1.hasNext() && rowIt2.hasNext()) {
            DataRow row1 = rowIt1.next();
            DataRow row2 = rowIt2.next();
            rowNum++;
            // we've checked the table structure before
            assert row1.getNumCells() == row2.getNumCells();

            // check the row key
            if (!row1.getKey().equals(row2.getKey())) {
                throw new TestEvaluationException("Row keys in row #" + rowNum + " differ: expected '" + row2.getKey()
                        + "',  got '" + row1.getKey() + "'");
            }
            // and all data cells
            for (int c = 0; c < row1.getNumCells(); c++) {
                DataCell c1 = row1.getCell(c);
                DataCell c2 = row2.getCell(c);
                if (!c1.isMissing() && !c2.isMissing()
                        && c1.getType().isCompatible(DoubleValue.class)
                        && c2.getType().isCompatible(DoubleValue.class)) {
                    double diff =
                            ((DoubleValue)c1).getDoubleValue()
                                    / ((DoubleValue)c2).getDoubleValue();
                    if (diff < 0) {
                        diff = -diff;
                    }
                    if (diff < 1) {
                        diff = 1 / diff;
                    }
                    if ((diff - 1) > m_epsilon) {
                        throw new TestEvaluationException("Cell content differs more than " + m_epsilon + " in row #"
                                + rowNum + "('" + row1.getKey() + "')" + " column #" + c + "('"
                                + table1.getDataTableSpec().getColumnSpec(c).getName() + "'): expected '" + c2
                                + "', got '" + c1 + "'");
                    }
                } else if (!c1.equals(c2)) {
                    throw new TestEvaluationException("Cell content differs" + " in row #" + rowNum + "('"
                            + row1.getKey() + "')" + " column #" + c + "('"
                            + table1.getDataTableSpec().getColumnSpec(c).getName() + "'): expected '" + c2 + "' got '"
                            + c1 + "'");
                }
            }

        }
        if (rowIt1.hasNext() || rowIt2.hasNext()) {
            throw new TestEvaluationException(
                    "DataTables are different in length.");
        }

    }

    /**
     * Returns the difference table, with those cells marked, which are not
     * equal.
     *
     * @return - the difference table.
     */
    public DataTable getDiffTable() {
        if (m_diffTable == null) {
            List<DataRow> rows = new ArrayList<DataRow>();
            Iterator<DataRow> rowIt1 = m_dataTable1.iterator();
            Iterator<DataRow> rowIt2 = m_dataTable2.iterator();
            // this is bullshit! (po)
            if (rowIt1.hasNext() != rowIt2.hasNext()) {
                throw new IllegalStateException("The tables are not "
                        + "of the same size!");
            }

            while (rowIt1.hasNext() && rowIt2.hasNext()) {
                DataRow currentRow = rowIt1.next();

                Iterator<DataCell> cellIt1 = currentRow.iterator();
                Iterator<DataCell> cellIt2 = rowIt2.next().iterator();
                DataCell[] cells = new DataCell[currentRow.getNumCells()];
                DataCell currentCell1 = null;
                DataCell currentCell2 = null;
                int pos = 0;
                while (cellIt1.hasNext() && cellIt2.hasNext()) {
                    currentCell1 = cellIt1.next();
                    currentCell2 = cellIt2.next();
                    if (currentCell1.equals(currentCell2)) {
                        cells[pos] = currentCell1;
                    } else if (!currentCell1.isMissing()
                            && !currentCell2.isMissing()
                            && currentCell1.getType().isCompatible(
                                    DoubleValue.class)
                            && currentCell2.getType().isCompatible(
                                    DoubleValue.class)) {
                        double diff =
                                ((DoubleValue)currentCell1).getDoubleValue()
                                        / ((DoubleValue)currentCell2)
                                                .getDoubleValue();
                        if (diff < 1) {
                            diff = 1 / diff;
                        }
                        if ((diff - 1) > m_epsilon) {
                            cells[pos] =
                                new StringCell("CELL DIFFERS:"
                                        + currentCell1.toString() + " <> "
                                        + currentCell2.toString());
                        } else {
                            cells[pos] = currentCell1;
                        }
                    } else {
                        cells[pos] =
                                new StringCell("CELL DIFFERS:"
                                        + currentCell1.toString() + " <> "
                                        + currentCell2.toString());
                    }
                    pos++;
                }
                RowKey oldRowKey = currentRow.getKey();
                RowKey rowId = oldRowKey;
                rows.add(new DefaultRow(rowId, cells));
            }
            Object[] tmp = rows.toArray();
            DataRow[] dataRows = new DataRow[tmp.length];
            for (int i = 0; i < tmp.length; i++) {
                dataRows[i] = (DataRow)tmp[i];
            }

            m_diffTable =
                    new DefaultTable(dataRows, m_dataTable1.getDataTableSpec());
        }
        return m_diffTable;
    }

}
