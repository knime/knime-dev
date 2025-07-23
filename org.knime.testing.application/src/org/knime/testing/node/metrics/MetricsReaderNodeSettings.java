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
 *   Jan 26, 2023 (wiswedel): created
 */
package org.knime.testing.node.metrics;

import java.util.concurrent.TimeUnit;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.HorizontalLayout;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 *
 * @author wiswedel
 */
@SuppressWarnings("restriction")
public final class MetricsReaderNodeSettings implements NodeParameters {


    interface OutputRepresentationModeRef extends ParameterReference<OutputRepresentation> {
    }

    interface SeriesWatchModeRef extends ParameterReference<Series> {
    }

    static final class OutputRepresentationPredicateProvider implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(OutputRepresentationModeRef.class).isOneOf(OutputRepresentation.Rows);
        }
    }

    static final class EnableWatchPredicateProvider implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getPredicate(OutputRepresentationPredicateProvider.class) //
                    .and(i.getEnum(SeriesWatchModeRef.class).isOneOf(Series.TimeSeries));
        }
    }

    enum OutputRepresentation {
        @Label("Single Column")
        Column,
        @Label("Rows")
        Rows;
    }

    enum Series {
        @Label("Snapshot")
        Snapshot,
        @Label("Time Series")
        TimeSeries
    }

    enum Unit {
        @Label("Seconds")
        Seconds(TimeUnit.SECONDS),
        @Label("Minutes")
        Minutes(TimeUnit.MINUTES),
        @Label("Hours")
        Hours(TimeUnit.HOURS);

        private final TimeUnit m_timeUnit;

        Unit(final TimeUnit timeUnit) {
            m_timeUnit = timeUnit;
        }

        TimeUnit toTimeUnit() {
            return m_timeUnit;
        }
    }

    @Section(title = "Time Range", description = "The time range over which measurements are collected.")
    interface TimeRangeSection {
        @HorizontalLayout
        interface TimeRangeRowLayout { }
    }

    @Section(title = "Reporting Period", description = "The time period over which measurements are aggregated. "
        + "The period needs to be smaller than the time range.")
    @After(TimeRangeSection.class   )
    interface ReportingPeriodSection {
        @HorizontalLayout
        interface ReportingPeriodRowLayout { }
    }

    @Widget(title = "Output Representation", description = "How measures will be represented in the output.")
    @ValueSwitchWidget
    @ValueReference(OutputRepresentationModeRef.class)
    OutputRepresentation m_outputRepresentation = OutputRepresentation.Column;

    @Widget(title = "Data Collection Method", description = "Choose <tt>Snapshot</tt> to capture a single, current "
        + "measurement, or <tt>Time Series</tt> to continuously record measurements over a period.")
    @ValueSwitchWidget
    @Effect(predicate = OutputRepresentationPredicateProvider.class, type = Effect.EffectType.ENABLE)
    @ValueReference(SeriesWatchModeRef.class)
    Series m_series = Series.TimeSeries;

    @Layout(TimeRangeSection.class)
    @Effect(predicate = EnableWatchPredicateProvider.class, type = Effect.EffectType.ENABLE)
    TimeRange m_timeRange = new TimeRange();

    @Layout(ReportingPeriodSection.class)
    @Effect(predicate = EnableWatchPredicateProvider.class, type = Effect.EffectType.ENABLE)
    ReportingPeriod m_reportingPeriod = new ReportingPeriod();

    static final class TimeRange implements NodeParameters {

        @Widget(title = "Value", description = " ")
        @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
        @Layout(TimeRangeSection.TimeRangeRowLayout.class)
        int m_timeRangeValue = 1;

        @Widget(title = "Unit", description = " ")
        @Effect(predicate = EnableWatchPredicateProvider.class, type = Effect.EffectType.ENABLE)
        @Layout(TimeRangeSection.TimeRangeRowLayout.class)
        Unit m_timeRangeUnit = Unit.Minutes;

        long toSeconds() {
            return m_timeRangeUnit.toTimeUnit().toSeconds(m_timeRangeValue);
        }

    }

    static final class ReportingPeriod implements NodeParameters {

        @Widget(title = "Value", description = " ")
        @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
        @Layout(ReportingPeriodSection.ReportingPeriodRowLayout.class)
        int m_reportingPeriodValue = 5;

        @Widget(title = "Unit", description = " ")
        @Effect(predicate = EnableWatchPredicateProvider.class, type = Effect.EffectType.ENABLE)
        @Layout(ReportingPeriodSection.ReportingPeriodRowLayout.class)
        Unit m_reportingPeriodUnit = Unit.Seconds;

        long toSeconds() {
            return m_reportingPeriodUnit.toTimeUnit().toSeconds(m_reportingPeriodValue);
        }
    }

}
