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
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
import org.knime.core.data.v2.RowCursor;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.data.xml.XMLValue;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.message.Message;
import org.knime.core.node.message.MessageBuilder;
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
     * The columns created from each found difference. Each column has
     * <ul>
     * <li>an extractor that gets the specified property from the diff object</li>
     * <li>a column name</li>
     * <li>a filter predicate that decides whether the column should be included in the output</li>
     * </ul>
     * Each diff property is treated as string and since there can be many diffs between two documents, is turned into a
     * string collection cell.
     */
    enum DiffColumn {
            /** e.g., TEXT_VALUE, ATTR_VALUE, etc. */
            COMPARISON_TYPE(c -> c.getComparison().getType(), "Comparison type", spec -> true),
            /** SIMILAR or DIFFERENT */
            SIMILARITY(c -> c.getResult(), "Similarity", XmlDifferNodeSettings::isSimilarityIncluded),
            /** actual value, e.g., lion */
            TEST_VALUE(c -> c.getComparison().getTestDetails().getValue(), "Test value", settings -> true),
            /** expected value, e.g., zebra */
            CONTROL_VALUE(c -> c.getComparison().getControlDetails().getValue(), "Control value", settings -> true),
            /** path to the node that is under test, e.g., /root[1]/element[1] */
            TEST_XPATH(c -> c.getComparison().getTestDetails().getXPath(), "XPath of test value", settings -> true),
            /** path to the parent node that is used as reference value, e.g., /root[1]/element[1] */
            CONTROL_XPATH(c -> c.getComparison().getControlDetails().getXPath(), "XPath of control value",
                settings -> true),
            /** path to the parent node that is used as reference value, e.g., /root[1] */
            TEST_PARENT_XPATH(c -> c.getComparison().getTestDetails().getParentXPath(), "Parent XPath of test value",
                XmlDifferNodeSettings::isParentXPathIncluded),
            /** path to the parent node that is used under test, e.g., /root[1] */
            CONTROL_PARENT_XPATH(c -> c.getComparison().getControlDetails().getParentXPath(),
                "Parent XPath of control value", XmlDifferNodeSettings::isParentXPathIncluded),
            /**
             * For instance
             * <code>{@literal Expected text value 'lion' but was 'zebra' - comparing <element ...>lion</element>
             * at /root[1]/element[1]/text()[1] to <element ...>zebra</element>
             * at /root[1]/element[1]/text()[1] (DIFFERENT)}</code>
             */
            SUMMARY(c -> c.toString(), "Summary", XmlDifferNodeSettings::isSummaryIncluded);

        private final Function<Difference, Object> m_extractor;

        private final DataColumnSpec m_spec;

        private final Predicate<XmlDifferNodeSettings> m_include;

        DiffColumn(final Function<Difference, Object> fun, final String columnName, final Predicate<XmlDifferNodeSettings> filter) {
            m_extractor = fun;
            m_include = filter;
            m_spec = new DataColumnSpecCreator(columnName, ListCell.getCollectionType(StringCell.TYPE)).createSpec();
        }

        String getName() {
            return m_spec.getName();
        }

        private static DiffColumn[] filteredWith(final XmlDifferNodeSettings settings) {
            return Arrays.stream(DiffColumn.values()) //
                .filter(column -> column.m_include.test(settings)) //
                .toArray(DiffColumn[]::new);
        }

        private static Stream<DataColumnSpec> newColumns(final XmlDifferNodeSettings settings) {
            return Arrays.stream(filteredWith(settings)) //
                .map(column -> column.m_spec);
        }
    }

    private final int m_testColumnIndex;

    private final int m_controlColumnIndex;

    private final BiFunction<Document, Document, DiffBuilder> m_diffBuilder;

    private final XmlDifferNodeSettings m_settings;

    /** Cursor over input table that contains the baseline XML documents to compare against. */
    private RowCursor m_controlData;

    /** The previous document used as a baseline or {@code null}. */
    private DataCell m_lastControlDocument;

    private MessageBuilder m_messageBuilder;

    private long m_rowIndex = 0L;

    XmlDifferCellFactory(final XmlDifferNodeSettings settings, final DataTableSpec[] tableSpecs,
        final RowCursor controlData, final BiFunction<Document, Document, DiffBuilder> diffBuilder,
        final MessageBuilder messageBuilder) throws IllegalArgumentException {
        super(newColumns(settings));

        m_controlData = controlData;
        m_testColumnIndex =
            tableSpecs[XmlDifferNodeSettings.TEST_TABLE_PORT_INDEX].findColumnIndex(settings.m_testColumn);
        m_controlColumnIndex =
            tableSpecs[XmlDifferNodeSettings.CONTROL_TABLE_PORT_INDEX].findColumnIndex(settings.m_controlColumn);
        m_diffBuilder = diffBuilder;
        m_settings = settings;
        m_messageBuilder = messageBuilder;
    }

    private static DataColumnSpec[] newColumns(final XmlDifferNodeSettings settings) {
        final var sourceColumns = settings.m_addSourceColumns ? Stream.of( //
            new DataColumnSpecCreator("Test document (%s)".formatted(settings.m_testColumn), XMLCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Control document (%s)".formatted(settings.m_controlColumn), XMLCell.TYPE)
                .createSpec())
            : Stream.empty();

        return Stream.concat(sourceColumns, DiffColumn.newColumns(settings)).toArray(DataColumnSpec[]::new);
    }

    /**
     * Compute a single diff between two documents.
     *
     * @param settings that specify what counts as difference
     * @param test document to compare
     * @param control baseline document
     * @return
     */
    private List<List<String>> diff(final XmlDifferNodeSettings settings, final Document test, final Document control) {
        final var selectedDiffColumns = DiffColumn.filteredWith(settings);
        final var diffColumnResults = IntStream.range(0, selectedDiffColumns.length) //
            .mapToObj(i -> (List<String>)new ArrayList<String>()) //
            .toList();

        Diff myDiff = m_diffBuilder.apply(test, control).build();
        Iterator<Difference> diffIter = myDiff.getDifferences().iterator();
        // each difference contributes to all selected output columns
        while (diffIter.hasNext()) {
            Difference difference = diffIter.next();

            if (settings.m_ignoreSmallDifferences && difference.getResult() == ComparisonResult.SIMILAR) {
                continue;
            }

            if (settings.m_failExecution) {
                final var message = m_messageBuilder//
                    .withSummary("Test document does not match control document.")
                    .addRowIssue(XmlDifferNodeSettings.TEST_TABLE_PORT_INDEX, m_testColumnIndex, m_rowIndex,
                        difference.toString())
                    .addResolutions("Change node settings to not fail on detected differences.").build().orElseThrow();
                throw KNIMEException.of(message).toUnchecked();
            }

            for (var i = 0; i < selectedDiffColumns.length; i++) {
                final var value = selectedDiffColumns[i].m_extractor.apply(difference);
                diffColumnResults.get(i).add(value != null ? value.toString() : null);
            }
        }
        return diffColumnResults;
    }

    private DataCell nextControlDocumentCell() {
        if (m_controlData.canForward()) {
            final var next = m_controlData.forward().getAsDataCell(m_controlColumnIndex);
            m_lastControlDocument = next;
            return next;
        } else {
            return switch (m_settings.m_columnPadding) {
                case FAIL -> failWithExhaustedControlColumn();
                case MISSING -> DataType.getMissingCell();
                case REPEAT_LAST -> m_lastControlDocument;
            };
        }
    }

    @Override
    public DataCell[] getCells(final DataRow row, final long rowIndex) {
        final var testCell = row.getCell(m_testColumnIndex);
        final var controlCell = nextControlDocumentCell();

        if (testCell == null || testCell.isMissing() || controlCell == null || controlCell.isMissing()) {
            return createEmptyCells(m_settings);
        } else if (testCell instanceof XMLValue test && controlCell instanceof XMLValue control) {
            try (var testDoc = test.getDocumentSupplier(); var controlDoc = control.getDocumentSupplier()) {
                final var differences = diff(m_settings, (Document)testDoc.get(), (Document)controlDoc.get());
                m_rowIndex++;
                final var sourceCells =
                    m_settings.m_addSourceColumns ? Stream.of(testCell, controlCell) : Stream.empty();
                final var diffCells = differences.stream() //
                    .map(XmlDifferCellFactory::stringsToStringCells) //
                    .map(CollectionCellFactory::createListCell);
                return Stream.concat(sourceCells, diffCells).toArray(DataCell[]::new);
            }
        }
        throw KNIMEException.of(Message.fromRowIssue("Found a non-xml cell, aborting.", 0, rowIndex, m_testColumnIndex,
            "This is most likely an implementation error.")).toUnchecked();
    }

    private static List<StringCell> stringsToStringCells(final List<String> values) {
        return values.stream().map(s -> s == null ? "<NULL>" : s).map(StringCell::new).toList();
    }

    private static DataCell[] createEmptyCells(final XmlDifferNodeSettings settings) {
        final var result = new DataCell[numCells(settings)];
        Arrays.fill(result, DataType.getMissingCell());
        return result;
    }

    /** @return number of cells produced by {@link #getCells(DataRow)} */
    private static int numCells(final XmlDifferNodeSettings settings) {
        final var numDiffCells = DiffColumn.filteredWith(settings).length;
        return settings.m_addSourceColumns ? numDiffCells + 2 : numDiffCells;
    }

    /** Use message builder to create a message and throw it as an unchecked exception. */
    private DataCell failWithExhaustedControlColumn() {
        final var message = m_messageBuilder//
            .withSummary("Not enough control documents.")
            .addRowIssue(XmlDifferNodeSettings.TEST_TABLE_PORT_INDEX, m_testColumnIndex, m_rowIndex,
                "Cannot compute diff, no more control documents available.")
            .addResolutions("Change the settings to use missing values or repeat the last value.")//
            .addResolutions("Make sure that the table with the control documents has at "
                + "least as many rows as the table with test documents.")
            .build().orElseThrow();
        throw KNIMEException.of(message).toUnchecked();
    }

}