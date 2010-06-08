/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   20.05.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.logistic.predict;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knime.base.node.mine.regression.logistic.learner.LogRegLearner;
import org.knime.base.node.mine.regression.logistic.learner.LogRegLearnerNodeFactory;
import org.knime.base.node.mine.regression.logistic.learner.LogRegLearnerSettings;
import org.knime.base.node.mine.regression.logistic.learner.LogisticRegressionContent;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.workflow.SingleNodeContainer;

/**
 *
 * @author Heiko Hofer
 */
public class LogRegPredictorTest {
    private ExecutionContext m_exec;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        NodeFactory nodeFactory = new LogRegLearnerNodeFactory();
        Node node = new Node(nodeFactory);
        m_exec = new ExecutionContext(
                new DefaultNodeProgressMonitor(), node,
                    SingleNodeContainer.MemoryPolicy.CacheOnDisc,
                    new HashMap<Integer, ContainerTable>());
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link org.knime.base.node.mine.regression.logistic.predict.LogRegPredictor#getCells(org.knime.core.data.DataRow)}.
     * @throws Exception
     */
    @Test
    public final void testGetCells() throws Exception {
        double p0 = 0.10;
        double p1 = 0.95;
        double x1 = 10;
        double beta0 = -1 * Math.log(-1.0 + 1.0 / p0);
        double beta1 = (-1 * Math.log(-1.0 + 1.0 / p1) - beta0) / x1;
        p0 = 0.20;
        p1 = 0.85;
        x1 = 10;
        double beta2 = -1 * Math.log(-1.0 + 1.0 / p0);
        double beta3 = (-1 * Math.log(-1.0 + 1.0 / p1) - beta0) / x1;
        double[][] beta = new double[][]{
            new double[]{beta0, beta1},
            new double[]{beta2, beta3},
        };
        ArtificialData data = new ArtificialData(1000, 100, beta,
                new DataCell[] {new StringCell("1"),
                new StringCell("2"),
                new StringCell("3")});
        LogRegLearnerSettings settings = new LogRegLearnerSettings();
        settings.setIncludeAll(true);
        settings.setTargetColumn(data.getTargetCol().getName());
        // Create Learner
        LogRegLearner learner = new LogRegLearner(data.getDataTableSpec(),
                settings);

        BufferedDataTable trainingData =
            m_exec.createBufferedDataTable(data, m_exec);
        LogisticRegressionContent content = learner.execute(
                trainingData, m_exec);


        List<DataCell> logits = content.getLogits();
        double beta0Est = content.getIntercept(logits.get(0));
        double beta1Est = content.getCoefficients(
                logits.get(0)).get(data.getLearningCols().get(0).getName());
        double beta2Est = content.getIntercept(logits.get(1));
        double beta3Est = content.getCoefficients(
                logits.get(1)).get(data.getLearningCols().get(0).getName());
        // When this test fails there is a problem with the learner and
        // not with the predictor!
        double eps = 0.01;
        if (Math.abs(beta0 - beta0Est) > eps
                || Math.abs(beta1 - beta1Est) > eps
                || Math.abs(beta2 - beta2Est) > eps
                || Math.abs(beta3 - beta3Est) > eps) {
            Assert.fail("Logistic Regression Learner failed to estimate the "
                    + "regression coefficients. Fix learner and rerun test for"
                    + "predictor.");
        }
        // Create predictor
        LogRegPredictor predictor = new LogRegPredictor(
                content.createPortObject(), data.getDataTableSpec(),
                true);
        for (DataRow row : data) {
            DataCell[] predicted = predictor.getCells(row);
            double[] trueProb = data.getProbability(row);

            Assert.assertEquals(trueProb[0],
                    ((DoubleCell)predicted[1]).getDoubleValue(),
                     0.005);
            Assert.assertEquals(trueProb[1],
                    ((DoubleCell)predicted[2]).getDoubleValue(),
                     0.005);
        }
    }

    private static class ArtificialData implements DataTable {
        private final int m_rowCount;
        private final DataCell[] m_targetValues;
        private final int m_groupCount;
        private final double[][] m_beta;
        private Random m_random;
        private int m_parameterCount;
        private List<DataRow> m_data;
        private List<DataColumnSpec> m_learningCols;
        private DataColumnSpec m_targetCol;
        private int m_count;
        private Map<DataRow, double[]> m_probabilty;

        /**
         *
         */
        public ArtificialData(final int rowCount, final int groupCount,
                final double[][] beta,
                final DataCell[] targetValues) {
            m_rowCount = rowCount;
            m_groupCount = groupCount;
            m_beta = beta;
            m_parameterCount = beta[0].length - 1;
            m_random = new Random(1);
            m_targetValues = targetValues;
            initData();

            m_learningCols =
                new ArrayList<DataColumnSpec>();
            for (int i = 0; i < m_parameterCount; i++) {
                m_learningCols.add((new DataColumnSpecCreator(
                        "c" + i, DoubleCell.TYPE)).createSpec());
            }

            // target
            DataColumnSpecCreator colCreator = new DataColumnSpecCreator(
                    "target", StringCell.TYPE);
            DataColumnDomainCreator domainCreator =
                new DataColumnDomainCreator(m_targetValues);
            colCreator.setDomain(domainCreator.createDomain());
            m_targetCol = colCreator.createSpec();
        }


        /**
         * @return the learningCols
         */
        public List<DataColumnSpec> getLearningCols() {
            return m_learningCols;
        }


        /**
         * @return the targetCol
         */
        public DataColumnSpec getTargetCol() {
            return m_targetCol;
        }

        public double[] getProbability(final DataRow row) {
            return m_probabilty.get(row);
        }

        /**
         * {@inheritDoc}
         */
        public DataTableSpec getDataTableSpec() {
            List<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>();
            colSpecs.addAll(m_learningCols);
            colSpecs.add(m_targetCol);

            return new DataTableSpec(colSpecs.toArray(new DataColumnSpec[0]));
        }

        /**
         * {@inheritDoc}
         */
        public RowIterator iterator() {
            return new TestDataIterator(m_data.iterator());
        }

        private void initData() {
            m_data = new ArrayList<DataRow>(m_rowCount * m_groupCount);
            m_count = 0;
            m_probabilty = new HashMap<DataRow, double[]>();

            for (int i = 0; i < m_rowCount; i++) {
                // parameter-vector
                double[] x = new double[m_parameterCount + 1];
                x[0] = 1;
                for (int k = 1; k < x.length; k++) {
                    x[k] = 10 * m_random.nextDouble();

                }

                // distribution function over the target categories
                double[] probabilty = new double[m_beta.length];

                double sumEBetaTx = 0;
                for (int k = 0; k < probabilty.length; k++) {
                    sumEBetaTx += Math.exp(inner(x, m_beta[k]));
                }
                probabilty[0] = Math.exp(inner(x, m_beta[0]))
                    / (1 + sumEBetaTx);
                for (int k = 1; k < probabilty.length; k++) {
                    probabilty[k] = Math.exp(inner(x, m_beta[k]))
                            / (1.0 + sumEBetaTx);
                }
                // cumulative distribution function over the target categories
                double[] cdf = new double[m_beta.length];
                cdf[0] = probabilty[0];
                for (int k = 1; k < cdf.length; k++) {
                    cdf[k] = cdf[k - 1] + probabilty[k];
                }

                DoubleCell[] parameterData = new DoubleCell[m_parameterCount];
                for (int k = 0; k < m_parameterCount; k++) {
                    parameterData[k] = new DoubleCell(x[k + 1]);
                }
                for (int j = 0; j < m_groupCount; j++) {
                    double p = m_random.nextDouble();
                    int g = -1;
                    if (p >= cdf[cdf.length - 1]) {
                        g = cdf.length;
                    } else {
                        for (int k = 0; k < cdf.length; k++) {
                            if (p < cdf[k]) {
                                g = k;
                                break;
                            }
                        }
                    }
                    List<DataCell> cells = new ArrayList<DataCell>();
                    cells.addAll(Arrays.asList(parameterData));
                    cells.add(m_targetValues[g]);
                    DataRow row = new DefaultRow(
                            Integer.toString(m_count), cells);
                    m_data.add(row);
                    m_count++;
                    m_probabilty.put(row, probabilty);
                }
            }
            Collections.shuffle(m_data, m_random);
        }

        private double inner(final double[] x, final double[] y) {
            double inner = 0;
            for (int i = 0; i < x.length; i++) {
                inner += x[i] * y[i];
            }
            return inner;
        }

        private static class TestDataIterator extends RowIterator {

            private Iterator<DataRow> m_iter;

            public TestDataIterator(final Iterator<DataRow> iter) {
                m_iter = iter;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean hasNext() {
                return m_iter.hasNext();
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public DataRow next() {
                return m_iter.next();
            }
        }
    }

}
