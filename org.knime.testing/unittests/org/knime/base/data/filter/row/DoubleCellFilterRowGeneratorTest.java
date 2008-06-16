/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 */
package org.knime.base.data.filter.row;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultCellIterator;
import org.knime.core.data.def.DoubleCell;

import junit.framework.TestCase;

/**
 * JUnit test class for {@link org.knime.core.data.def.DoubleCell} row
 * filter generator.
 * 
 * @see DoubleCellFilterRowGenerator
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 */
public final class DoubleCellFilterRowGeneratorTest extends TestCase {

    /**
     * Test row which keeps one <code>DoubleCell</code> value.
     */
    private final class MyTestRow implements DataRow {
        private final DataCell m_cell;

        /**
         * Creates a new test row.
         * 
         * @param value Using this double values as the only cell.
         */
        MyTestRow(final double value) {
            m_cell = new DoubleCell(value);
        }

        /**
         * Returns <code>1</code>, always.
         * 
         * @return The number of cells, <code>1</code>.
         */
        public int getNumCells() {
            return 1;
        }

        /**
         * @return Should not be called.
         */
        public RowKey getKey() {
            fail("Should not be called.");
            return null;
        }

        /**
         * The data cell holding the double value at position <code>0</code>.
         * 
         * @param index Has to be <code>0</code>.
         * @return A double cell.
         */
        public DataCell getCell(final int index) {
            assertTrue(index == 0);
            return m_cell;
        }

        /**
         * {@inheritDoc}
         */
        public Iterator<DataCell> iterator() {
            return new DefaultCellIterator(this);
        }

    } // MyTestRow

    /** Keep double value for left border. */
    private DoubleValue m_dblLeft;

    /** Keep double value for right border. */
    private DoubleValue m_dblRight;

    /**
     * Init internal members.
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        m_dblLeft = new DoubleCell(5.0);
        m_dblRight = new DoubleCell(8.0);
    }

    /**
     * Destroy internal members.
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() {
        m_dblLeft = null;
        m_dblRight = null;
    }

    /*
     * Static filter row index.
     */
    private static final int IDX = 0;

    /*
     * Create LEFT_IN, LEFT_OUT, RIGHT_IN, RIGHT_OUT for further use.
     */
    private static final int LEFT_IN = DoubleCellFilterRowGenerator.LEFT
            + DoubleCellFilterRowGenerator.IN;

    private static final int LEFT_OUT = DoubleCellFilterRowGenerator.LEFT
            + DoubleCellFilterRowGenerator.OUT;

    private static final int RIGHT_IN = DoubleCellFilterRowGenerator.RIGHT
            + DoubleCellFilterRowGenerator.IN;

    private static final int RIGHT_OUT = DoubleCellFilterRowGenerator.RIGHT
            + DoubleCellFilterRowGenerator.OUT;

    /**
     * Test on left border using <b>3.0</b>.
     */
    public void testOneBorder1() {
        DataRow row = new MyTestRow(3.0);
        FilterRowGenerator gen = new DoubleCellFilterRowGenerator(IDX,
                m_dblLeft, LEFT_IN);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_OUT);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_IN);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_OUT);
        assertFalse(gen.isIn(row));
    }

    /**
     * Test on left border using <b>5.0</b>.
     */
    public void testOneBorder2() {
        DataRow row = new MyTestRow(5.0);
        FilterRowGenerator gen = new DoubleCellFilterRowGenerator(IDX,
                m_dblLeft, LEFT_IN);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_OUT);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_IN);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_OUT);
        assertFalse(gen.isIn(row));
    }

    /**
     * Test on left border using <b>7.0</b>.
     */
    public void testOneBorder3() {
        DataRow row = new MyTestRow(7.0);
        FilterRowGenerator gen = new DoubleCellFilterRowGenerator(IDX,
                m_dblLeft, LEFT_IN);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_OUT);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_IN);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_OUT);
        assertTrue(gen.isIn(row));
    }

    /**
     * Test on left border using <b>8.0</b>.
     */
    public void testOneBorder4() {
        DataRow row = new MyTestRow(8.0);
        FilterRowGenerator gen = new DoubleCellFilterRowGenerator(IDX,
                m_dblLeft, 5);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, 6);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, 9);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, 10);
        assertTrue(gen.isIn(row));
    }

    /**
     * Test on left border using <b>9.0</b>.
     */
    public void testOneBorder5() {
        DataRow row = new MyTestRow(9.0);
        FilterRowGenerator gen = new DoubleCellFilterRowGenerator(IDX,
                m_dblLeft, LEFT_IN);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_OUT);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_IN);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_OUT);
        assertTrue(gen.isIn(row));
    }

    /**
     * Test on left and right border using <b>3.0</b>.
     */
    public void testTwoBorder1() {
        DataRow row = new MyTestRow(3.0);
        FilterRowGenerator gen = new DoubleCellFilterRowGenerator(IDX,
                m_dblLeft, LEFT_IN, m_dblRight, RIGHT_IN);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_OUT,
                m_dblRight, RIGHT_IN);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_IN,
                m_dblRight, RIGHT_OUT);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_OUT,
                m_dblRight, RIGHT_OUT);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_IN,
                m_dblRight, LEFT_IN);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_OUT,
                m_dblRight, LEFT_OUT);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_OUT,
                m_dblRight, LEFT_IN);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_IN,
                m_dblRight, LEFT_OUT);
        assertFalse(gen.isIn(row));
    }

    /**
     * Test on left and right border using <b>5.0</b>.
     */
    public void testTwoBorder2() {
        DataRow row = new MyTestRow(5.0);
        FilterRowGenerator gen = new DoubleCellFilterRowGenerator(IDX,
                m_dblLeft, LEFT_IN, m_dblRight, RIGHT_IN);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_OUT,
                m_dblRight, RIGHT_IN);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_IN,
                m_dblRight, RIGHT_OUT);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_OUT,
                m_dblRight, RIGHT_OUT);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_IN,
                m_dblRight, LEFT_IN);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_OUT,
                m_dblRight, LEFT_OUT);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_OUT,
                m_dblRight, LEFT_IN);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_IN,
                m_dblRight, LEFT_OUT);
        assertTrue(gen.isIn(row));
    }

    /**
     * Test on left and right border using <b>7.0</b>.
     */
    public void testTwoBorder3() {
        DataRow row = new MyTestRow(7.0);
        FilterRowGenerator gen = new DoubleCellFilterRowGenerator(IDX,
                m_dblLeft, LEFT_IN, m_dblRight, RIGHT_IN);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_OUT,
                m_dblRight, RIGHT_IN);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_IN,
                m_dblRight, RIGHT_OUT);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_OUT,
                m_dblRight, RIGHT_OUT);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_IN,
                m_dblRight, LEFT_IN);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_OUT,
                m_dblRight, LEFT_OUT);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_OUT,
                m_dblRight, LEFT_IN);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_IN,
                m_dblRight, LEFT_OUT);
        assertTrue(gen.isIn(row));
    }

    /**
     * Test on left and right border using <b>8.0</b>.
     */
    public void testTwoBorder4() {
        DataRow row = new MyTestRow(8.0);
        FilterRowGenerator gen = new DoubleCellFilterRowGenerator(IDX,
                m_dblLeft, LEFT_IN, m_dblRight, RIGHT_IN);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_OUT,
                m_dblRight, RIGHT_IN);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_IN,
                m_dblRight, RIGHT_OUT);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_OUT,
                m_dblRight, RIGHT_OUT);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_IN,
                m_dblRight, LEFT_IN);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_OUT,
                m_dblRight, LEFT_OUT);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_OUT,
                m_dblRight, LEFT_IN);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_IN,
                m_dblRight, LEFT_OUT);
        assertFalse(gen.isIn(row));
    }

    /**
     * Test on left and right border using <b>9.0</b>.
     */
    public void testTwoBorder5() {
        DataRow row = new MyTestRow(9.0);
        FilterRowGenerator gen = new DoubleCellFilterRowGenerator(IDX,
                m_dblLeft, LEFT_IN, m_dblRight, RIGHT_IN);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_OUT,
                m_dblRight, RIGHT_IN);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_IN,
                m_dblRight, RIGHT_OUT);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_OUT,
                m_dblRight, RIGHT_OUT);
        assertTrue(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_IN,
                m_dblRight, LEFT_IN);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_OUT,
                m_dblRight, LEFT_OUT);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_OUT,
                m_dblRight, LEFT_IN);
        assertFalse(gen.isIn(row));
        gen = new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_IN,
                m_dblRight, LEFT_OUT);
        assertFalse(gen.isIn(row));
    }

    /**
     * Negative column index.
     */
    public void testException1() {
        try {
            new DoubleCellFilterRowGenerator(-1, m_dblLeft, LEFT_IN);
            fail("Exception expected: Negative column index.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * <code>DoubleCell</code> border is <code>null</code>.
     */
    public void testException2() {
        try {
            new DoubleCellFilterRowGenerator(IDX, null, LEFT_IN);
            fail("Exception expected: DoubleCell is null.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Invalid mask.
     */
    public void testException3() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblLeft, -1);
            fail("Exception expected: Mask is invalid -1.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Invalid mask.
     */
    public void testException4() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblLeft, 4);
            fail("Exception expected: Mask is invalid 4.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Negative column index.
     */
    public void testException5() {
        try {
            new DoubleCellFilterRowGenerator(-1, m_dblLeft.getDoubleValue(),
                    LEFT_IN);
            fail("Exception expected: Negative column index.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Invalid mask.
     */
    public void testException6() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblLeft.getDoubleValue(),
                    -1);
            fail("Exception expected: Mask is invalid -1.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Invalid mask.
     */
    public void testException7() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblLeft.getDoubleValue(), 4);
            fail("Exception expected: Mask is invalid 4.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Negative column index.
     */
    public void testException8() {
        try {
            new DoubleCellFilterRowGenerator(-1, m_dblLeft, LEFT_IN,
                    m_dblRight, RIGHT_IN);
            fail("Exception expected: Mask is invalid -1.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Left <code>DoubleCell</code> border is <code>null</code>.
     */
    public void testException9() {
        try {
            new DoubleCellFilterRowGenerator(IDX, null, LEFT_IN, m_dblRight,
                    RIGHT_IN);
            fail("Exception expected: Left DoubleCell is null.");
        } catch (Exception e) {
            assertTrue(true);
        }

    }

    /**
     * Right <code>DoubleCell</code> border is <code>null</code>.
     */
    public void testException10() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_IN, null,
                    RIGHT_IN);
            fail("Exception expected: Right DoubleCell is null.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Left and right <code>DoubleCell</code> border are <code>null</code>.
     */
    public void testException11() {
        try {
            new DoubleCellFilterRowGenerator(IDX, null, LEFT_IN, null, RIGHT_IN);
            fail("Exception expected: Left and right DoubleCell are null.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Right border before left border.
     */
    public void testException12() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblRight, LEFT_IN,
                    m_dblLeft, RIGHT_IN);
            fail("Exception expected: Right border smaller than left border.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Borders with same values.
     */
    public void testException13() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_IN,
                    m_dblLeft, RIGHT_IN);
            fail("Exception expected: Left and right border are equal.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Overlapping border values.
     */
    public void testException14() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblRight.getDoubleValue(),
                    LEFT_IN, m_dblLeft.getDoubleValue(), RIGHT_IN);
            fail("Exception expected: Right border smaller than left border.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Same border double values.
     */
    public void testException15() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblLeft.getDoubleValue(),
                    LEFT_IN, m_dblLeft.getDoubleValue(), RIGHT_IN);
            fail("Exception expected: Left and right border are equal.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Left mask invalid.
     */
    public void testException16() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblLeft, -1, m_dblRight,
                    RIGHT_IN);
            fail("Exception expected: Left mask is invalid.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Right mask invalid.
     */
    public void testException17() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_IN,
                    m_dblRight, -1);
            fail("Exception expected: Right mask is invalid.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Left and right mask are invalid.
     * 
     */
    public void testException18() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblLeft, -1, m_dblRight, -1);
            fail("Exception expected: Left and right mask are invalid.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Overlapping intervals.
     */
    public void testException19() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_IN,
                    m_dblRight, LEFT_IN);
            fail("Exception expected: Mask intervals are overlapping.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Overlapping intervals.
     */
    public void testException20() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_OUT,
                    m_dblRight, LEFT_IN);
            fail("Exception expected: Mask intervals are overlapping.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Overlapping intervals.
     */
    public void testException21() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_IN,
                    m_dblRight, LEFT_OUT);
            fail("Exception expected: Mask intervals are overlapping.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Overlapping intervals.
     */
    public void testException22() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_OUT,
                    m_dblRight, LEFT_OUT);
            fail("Exception expected: Mask intervals are overlapping.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Overlapping intervals.
     */
    public void testException23() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_IN,
                    m_dblRight, LEFT_IN);
            fail("Exception expected: Mask intervals are overlapping.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Overlapping intervals.
     */
    public void testException24() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblLeft, LEFT_OUT,
                    m_dblRight, LEFT_IN);
            fail("Exception expected: Mask intervals are overlapping.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Overlapping intervals.
     */
    public void testException25() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_IN,
                    m_dblRight, RIGHT_OUT);
            fail("Exception expected: Mask intervals are overlapping.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * Overlapping intervals.
     */
    public void testException26() {
        try {
            new DoubleCellFilterRowGenerator(IDX, m_dblLeft, RIGHT_OUT,
                    m_dblRight, RIGHT_OUT);
            fail("Exception expected: Mask intervals are overlapping.");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    /**
     * System entry point.
     * 
     * @param args The command line parameters: ignored.
     */
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(DoubleCellFilterRowGeneratorTest.class);
    }

} // DoubleCellFilterRowGeneratorTest
