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

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.message.Message;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.w3c.dom.Document;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.ComparisonControllers;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

import com.google.common.base.Objects;

/**
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
final class XmlDifferNodeModel extends WebUINodeModel<XmlDifferNodeSettings> {

    XmlDifferNodeModel(final WebUINodeConfiguration cfg) {
        super(cfg, XmlDifferNodeSettings.class);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final XmlDifferNodeSettings settings)
        throws InvalidSettingsException {
        final var outputSpec = createColumnRearranger(inSpecs[0], settings, this::setWarning).createSpec();
        return new DataTableSpec[]{outputSpec};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final XmlDifferNodeSettings settings) throws Exception {
        final BufferedDataTable table = exec.createColumnRearrangeTable(inData[0],
            createColumnRearranger(inData[0].getDataTableSpec(), settings, this::setWarning), exec);
        return new BufferedDataTable[]{table};
    }

    @Override
    protected void validateSettings(final XmlDifferNodeSettings settings) throws InvalidSettingsException {
        if (settings.m_controlColumn == null) {
            throw new InvalidSettingsException("Control column must be set");
        }
        if (settings.m_testColumn == null) {
            throw new InvalidSettingsException("Test column must be set");
        }

        if (Objects.equal(settings.m_controlColumn, settings.m_testColumn)) {
            this.setWarningMessage("Same columns used for control and test - will always be identical");
        }
    }

    private static ColumnRearranger createColumnRearranger(final DataTableSpec inputTableSpec,
        final XmlDifferNodeSettings settings, final Consumer<Message> warningConsumer) throws InvalidSettingsException {

        final BiFunction<Document, Document, DiffBuilder> builder = (control, test) -> {
            final var b = DiffBuilder.compare(control).withTest(test);

            if (settings.m_stopAfterFirstDifference) {
                b.withComparisonController(ComparisonControllers.StopWhenDifferent);
            }

            if (settings.m_ignoreElementOrder) {
                // an element is allowed to match another with the same name and text node (if any),
                // irrespective of order
                b.withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText));
            }

            if (settings.m_ignoreComments) {
                b.ignoreComments();
            }
            if (settings.m_ignoreWhiteSpace) {
                b.ignoreWhitespace();
            }
            if (settings.m_ignoreElementContentWhitespace) {
                return b.ignoreElementContentWhitespace();
            }
            return b;
        };

        final var cellFactory = new XmlDifferCellFactory(settings, inputTableSpec, builder);

        final var rearranger = new ColumnRearranger(inputTableSpec);
        if(settings.m_removeSourceColumns) {
            rearranger.remove(settings.m_testColumn, settings.m_controlColumn);
        }
        rearranger.append(cellFactory);
        return rearranger;
    }
}