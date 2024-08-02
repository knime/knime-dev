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
 * ---------------------------------------------------------------------
 */
package org.knime.testing.node.xmldiffer;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.filter.TableFilter;
import org.knime.core.data.v2.RowCursor;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.w3c.dom.Document;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.ComparisonControllers;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

/**
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction") // webui
final class XmlDifferNodeModel extends WebUINodeModel<XmlDifferNodeSettings> {

    XmlDifferNodeModel(final WebUINodeConfiguration cfg) {
        super(cfg, XmlDifferNodeSettings.class);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final XmlDifferNodeSettings settings)
        throws InvalidSettingsException {
        validateSettings(settings);

        final var controlColumnIndex =
            inSpecs[XmlDifferNodeSettings.CONTROL_TABLE_PORT_INDEX].findColumnIndex(settings.m_controlColumn);
        CheckUtils.checkSetting(controlColumnIndex >= 0, "Unknown control column \"%s\"", settings.m_controlColumn);

        final var testSpec = inSpecs[XmlDifferNodeSettings.TEST_TABLE_PORT_INDEX];
        final var testColumnIndex = testSpec.findColumnIndex(settings.m_testColumn);
        CheckUtils.checkSetting(testColumnIndex >= 0, "Unknown test column \"%s\"", settings.m_testColumn);

        final var outputSpec =
            createColumnRearranger(testSpec, testColumnIndex, null, controlColumnIndex, settings).createSpec();
        return new DataTableSpec[]{outputSpec};
    }

    @Override
    protected void validateSettings(final XmlDifferNodeSettings settings) throws InvalidSettingsException {
        CheckUtils.checkSettingNotNull(settings.m_controlColumn, "Control XML document column must be set");
        CheckUtils.checkSettingNotNull(settings.m_testColumn, "Test XML document column must be set");
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final XmlDifferNodeSettings settings) throws Exception {
        final var testTable = inData[XmlDifferNodeSettings.TEST_TABLE_PORT_INDEX];
        final var controlTable = inData[XmlDifferNodeSettings.CONTROL_TABLE_PORT_INDEX];

        final var testColumnIndex = testTable.getSpec().findColumnIndex(settings.m_testColumn);
        final var controlColumnIndex = controlTable.getSpec().findColumnIndex(settings.m_controlColumn);
        try (final var cursor = controlTable.cursor(TableFilter.materializeCols(controlColumnIndex))) {
            final var columnRearranger =
                createColumnRearranger(testTable.getSpec(), testColumnIndex, cursor, controlColumnIndex, settings);
            final BufferedDataTable table = exec.createColumnRearrangeTable(testTable, columnRearranger, exec);
            return new BufferedDataTable[]{table};
        }
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec testTableSpec, final int testColumnIndex,
        final RowCursor controlData, final int controlColumnIndex, final XmlDifferNodeSettings settings)
                throws InvalidSettingsException {

        final var rearranger = new ColumnRearranger(testTableSpec);
        rearranger.remove(testColumnIndex);

        final var cellFactory = new XmlDifferCellFactory(settings, testColumnIndex, controlData, controlColumnIndex,
            createDiffBuilder(settings), createMessageBuilder());
        rearranger.append(cellFactory);
        return rearranger;
    }

    private static BiFunction<Document, Document, DiffBuilder> createDiffBuilder(final XmlDifferNodeSettings settings) {
        final BiFunction<Document, Document, DiffBuilder> diffBuilder = (control, test) -> {
            // if not swapping test and control documents, the diff is inverted, e.g,. expected and actual in summary
            final var b = DiffBuilder.compare(test).withTest(control);

            if (settings.m_ignoreElementOrder) {
                // an element is allowed to match another with the same name and text node (if any),
                // irrespective of order
                b.withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText));
            }

            if (settings.m_ignoreComments) {
                b.ignoreComments();
            }
            if (settings.m_normalizeWhitespace) {
                b.normalizeWhitespace();
            }
            if (settings.m_ignoreWhiteSpace) {
                b.ignoreWhitespace();
            }
            if (settings.m_ignoreElementContentWhitespace) {
                return b.ignoreElementContentWhitespace();
            }

            final var nodeNames = Arrays.stream(settings.m_filterNodeNames).collect(Collectors.toSet());
            if (!nodeNames.isEmpty()) {
                b.withNodeFilter(n -> !nodeNames.contains(n.getNodeName()));
            }

            // no need to compute all results if we are failing on the first difference
            if (settings.m_failExecution) {
                b.withComparisonController(ComparisonControllers.StopWhenDifferent);
            }

            return b;
        };
        return diffBuilder;
    }

}