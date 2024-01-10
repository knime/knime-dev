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
 *   11 Jan 2024 (carlwitt): created
 */
package org.knime.testing.node.xmldiffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.xml.XMLValue;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.message.Message;
import org.w3c.dom.Document;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

/**
 * Provides collection cells that summarize the type and location of differences.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
final class XmlDifferCellFactory extends AbstractCellFactory {

    /**
     * The columns appended to each row. Each column is a string collection cell, containing properties of each found
     * difference.
     */
    enum Columns {
            // e.g., TEXT_VALUE
            COMPARISON_TYPE(c -> c.getComparison().getType(), "Comparison type"),
            // similar or diffent
            SIMILARITY(c -> c.getResult(), "Similarity"),
            // expected value, e.g., zebra
            CONTROL_VALUE(c -> c.getComparison().getControlDetails().getValue(), "Control value"),
            // e.g., /root[1]/element[1]
            // actual value, e.g., lion
            TEST_VALUE(c -> c.getComparison().getTestDetails().getValue(), "Test value"),
            // e.g., /root[1]/element[1]/text()[1]
            CONTROL_XPATH(c -> c.getComparison().getControlDetails().getXPath(), "XPath of control value"),
            TEST_XPATH(c -> c.getComparison().getTestDetails().getXPath(), "XPath of test value"),
            CONTROL_PARENT_XPATH(c -> c.getComparison().getControlDetails().getParentXPath(),
                    "Parent XPath of control value"),
            // e.g., /root[1]/element[1]
            TEST_PARENT_XPATH(c -> c.getComparison().getTestDetails().getParentXPath(), "Parent XPath of test value"),
            //e.g., /root[1]/element[1]/text()[1]
            /**
             * For instance
             * <code>{@literal Expected text value 'lion' but was 'zebra' - comparing <element ...>lion</element>
             * at /root[1]/element[1]/text()[1] to <element ...>zebra</element>
             * at /root[1]/element[1]/text()[1] (DIFFERENT)}</code>
             */
            SUMMARY(c -> c.toString(), "Summary");

        private final Function<Difference, Object> extractor;

        private final DataColumnSpec spec;

        Columns(final Function<Difference, Object> fun, final String columnName) {
            extractor = fun;
            spec = new DataColumnSpecCreator(columnName, ListCell.getCollectionType(StringCell.TYPE)).createSpec();
        }

        String getName() {
            return spec.getName();
        }

        private static DataColumnSpec[] newColumns() {
            return Arrays.stream(Columns.values()).map(e -> e.spec).toArray(DataColumnSpec[]::new);
        }
    }

    private final int m_testColumnIndex;

    private final int m_controlColumnIndex;

    private final BiFunction<Document, Document, DiffBuilder> m_diffBuilder;

    private final XmlDifferNodeSettings m_settings;

    XmlDifferCellFactory(final XmlDifferNodeSettings settings, final DataTableSpec tableSpec,
        final BiFunction<Document, Document, DiffBuilder> diffBuilder) throws IllegalArgumentException {
        super(Columns.newColumns());

        m_testColumnIndex = tableSpec.findColumnIndex(settings.m_testColumn);
        m_controlColumnIndex = tableSpec.findColumnIndex(settings.m_controlColumn);
        m_diffBuilder = diffBuilder;
        m_settings = settings;
    }

    /**
     * @param lockedSupplier the document to check
     * @param lockedSupplier2 the document that is the reference
     * @return a list of values for each of the selected output columns
     */
    private List<List<String>> diff(final XmlDifferNodeSettings settings, final Document test, final Document control) {
        Diff myDiff = m_diffBuilder.apply(test, control).build();

        Iterator<Difference> iter = myDiff.getDifferences().iterator();

        final var columnResults =
            IntStream.range(0, Columns.values().length).mapToObj(i -> (List<String>)new ArrayList<String>()).toList();

        // each difference contributes to all selected output columns
        while (iter.hasNext()) {
            Difference difference = iter.next();

            if (settings.m_ignoreSmallDifferences && difference.getResult() == ComparisonResult.SIMILAR) {
                continue;
            }

            for (var i = 0; i < Columns.values().length; i++) {
                final var value = Columns.values()[i].extractor.apply(difference);
                columnResults.get(i).add(value != null ? value.toString() : null);
            }
        }
        return columnResults;
    }

    @Override
    public DataCell[] getCells(final DataRow row, final long rowIndex) {
        final var testCell = row.getCell(m_testColumnIndex);
        final var controlCell = row.getCell(m_controlColumnIndex);
        if (testCell.isMissing() || controlCell.isMissing()) {
            return createEmptyCells();
        } else if (testCell instanceof XMLValue test && controlCell instanceof XMLValue control) {
            try (var testDoc = test.getDocumentSupplier(); var controlDoc = control.getDocumentSupplier()) {
                final var differences = diff(m_settings, (Document)testDoc.get(), (Document)controlDoc.get());
                return differences.stream() //
                    .map(XmlDifferCellFactory::toStringCells) //
                    .map(CollectionCellFactory::createListCell) //
                    .toArray(DataCell[]::new);
            }
        }
        throw KNIMEException.of(Message.fromRowIssue("Found a non-xml cell, aborting.", 0, rowIndex, m_testColumnIndex,
            "This is most likely an implementation error.")).toUnchecked();
    }

    private static List<StringCell> toStringCells(final List<String> values) {
        return values.stream().map(s -> s == null ? "<NULL>" : s).map(StringCell::new).toList();
    }

    private static DataCell[] createEmptyCells() {
        final var missingCell = DataType.getMissingCell();
        return new DataCell[]{missingCell, missingCell};
    }

}