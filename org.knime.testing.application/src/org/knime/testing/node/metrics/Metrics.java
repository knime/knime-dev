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
 *   Jan 23, 2025 (wiswedel): created
 */
package org.knime.testing.node.metrics;

import static org.knime.core.monitor.ApplicationHealth.getGlobalThreadPoolLoadAverages;

import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.DoubleWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.IntWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.LongWriteValue;
import org.knime.core.monitor.ApplicationHealth;
import org.knime.core.node.KNIMEConstants;

/**
 * Gathers metrics that are reported by the node.
 * @author wiswedel
 */
final class Metrics {

    private Metrics() {
    }

    record Metric(String name, DataType type, Supplier<Number> supplier) {
        void writeTo(final WriteValue<? extends DataValue> writeValue) {
            if (type == DoubleCell.TYPE) {
                ((DoubleWriteValue)writeValue).setDoubleValue(supplier.get().doubleValue());
            } else if (type == IntCell.TYPE) {
                ((IntWriteValue)writeValue).setIntValue(supplier.get().intValue());
            } else if (type == LongCell.TYPE) {
                ((LongWriteValue)writeValue).setLongValue(supplier.get().longValue());
            } else {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    static Stream<Metric> availableMetrics() {
        return Stream.concat( //
            Stream.of( //
                new Metric("Thread Pool Size", IntCell.TYPE, KNIMEConstants.GLOBAL_THREAD_POOL::getMaxThreads), //
                new Metric("Thread Pool Load (1min)", DoubleCell.TYPE, getGlobalThreadPoolLoadAverages()::avg1Min), //
                new Metric("Thread Pool Load (5min)", DoubleCell.TYPE, getGlobalThreadPoolLoadAverages()::avg5Min), //
                new Metric("Thread Pool Load (15min)", DoubleCell.TYPE, getGlobalThreadPoolLoadAverages()::avg15Min), //
                new Metric("Nodes in state 'executed'", IntCell.TYPE, ApplicationHealth::getNodeStateExecutedCount), //
                new Metric("Nodes in state 'executing'", IntCell.TYPE, ApplicationHealth::getNodeStateExecutingCount), //
                new Metric("Nodes in state other", IntCell.TYPE, ApplicationHealth::getNodeStateOtherCount) //
            ), //
            ApplicationHealth.getInstanceCounters().stream() //
                .map(i -> new Metric("instance count - " + i.getName(), LongCell.TYPE, i::get) //
            ) //
        );
    }

}
