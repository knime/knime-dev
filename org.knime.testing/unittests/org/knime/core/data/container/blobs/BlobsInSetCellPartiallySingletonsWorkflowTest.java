/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Nov 27, 2008 (wiswedel): created
 */
package org.knime.core.data.container.blobs;

import java.util.ArrayList;
import java.util.Collection;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.testing.data.blob.LargeBlobCell;

/**
 * Creates table with set cell, whereby some elements are singletons and others
 * are new blob cells.
 * @author wiswedel, University of Konstanz
 */
public class BlobsInSetCellPartiallySingletonsWorkflowTest extends AbstractBlobsInWorkflowTest {

    private final int ROW_COUNT = 20;
    
    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable createBDT(final ExecutionContext exec) {
        DataType t = ListCell.getCollectionType(
                DataType.getType(DataCell.class));
        BufferedDataContainer c = exec.createDataContainer(
                new DataTableSpec(new DataColumnSpecCreator(
                        "Sequence", t).createSpec()));
        LargeBlobCell singleton = new LargeBlobCell("singleton");
        for (int i = 0; i < ROW_COUNT; i++) {
            String s = "someName_" + i;
            // every other a ordinary string cell
            Collection<DataCell> cells = new ArrayList<DataCell>();
            for (int j = 0; j < 4; j++) {
                String val = "Row_" + i + "; Cell index " + j;
                switch (j) {
                case 0:
                    cells.add(singleton);
                    break;
                case 1:
                case 3:
                    cells.add(new StringCell(val));
                    break;
                case 2:
                    cells.add(new LargeBlobCell(val));
                    break;
                default:
                    fail("invalid index");
                }
            }
            ListCell cell = CollectionCellFactory.createListCell(cells);
            c.addRowToTable(new DefaultRow(s, cell));
        }
        c.close();
        return c.getTable();
    }

    /** {@inheritDoc} */
    @Override
    protected long getApproximateSize() {
        return LargeBlobCell.SIZE_OF_CELL * (1 + ROW_COUNT);
    }

}
