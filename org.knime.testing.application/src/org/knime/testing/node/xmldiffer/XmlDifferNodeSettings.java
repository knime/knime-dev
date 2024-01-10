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

import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.xml.XMLValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProviderUtil.CompatibleColumnChoicesProvider;

/**
 * Settings class for the XmlDiffer node.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
final class XmlDifferNodeSettings implements DefaultNodeSettings {

    final static int TEST_TABLE_PORT_INDEX = 0;

    final static int CONTROL_TABLE_PORT_INDEX = 1;

    interface Sections {
        @Section(title = "Input")
        interface Input {
        }

        @Section(title = "Difference definitions")
        @After(Input.class)
        interface Controls {
        }

        @Section(title = "Output")
        @After(Controls.class)
        interface Output {
        }
    }

    // Input section
    @Widget(title = "Test document column",
        description = "Provides the actual XML document to compare to the values provided by the control column.")
    @ChoicesWidget(choices = TestTableXmlColumnChoicesProvider.class)
    @Layout(Sections.Input.class)
    String m_testColumn;

    @Widget(title = "Control document column", description = "Provides the expected values to compare against.")
    @ChoicesWidget(choices = ControlTableXmlColumnChoicesProvider.class)
    @Layout(Sections.Input.class)
    String m_controlColumn;

    enum ColumnPadding {
            @Label(value = "Use missing value",
                description = "Output missing values if no control document is available.")
            MISSING, //
            @Label(value = "Fail", description = "The node fails if there are too few control documents.")
            FAIL,
            @Label(value = "Repeat last control document",
                description = "Use that last control document for all remaining test documents. Useful for "
                    + "comparing a single control document to multiple test documents.")
            REPEAT_LAST
    }

    @Widget(title = "Handle different column lengths",
        description = "Choose how to handle the case when there are fewer control documents than test documents.")
    @Layout(Sections.Input.class)
    @ValueSwitchWidget
    ColumnPadding m_columnPadding = ColumnPadding.MISSING;

    @Widget(title = "Filter subtrees by node name",
        description = "Exclude a node and its descendants if its node name is in this list.")
    @ArrayWidget(elementTitle = "Node name", addButtonText = "Add node name")
    @Layout(Sections.Input.class)
    String[] m_filterNodeNames = new String[0];

    // Controls section
    @Widget(title = "Tolerate element reordering", description = """
            If checked, a reordering of elements is output as difference of type SIMILAR instead of type DIFFERENT.
            However, this only applies for instance if the XML nodes have different names, e.g.,
            <code>&lt;xml&gt;&lt;node id=&quot;1&quot;/&gt;&lt;node id=&quot;2&quot;/&gt;&lt;/xml&gt;</code> and
             <code>&lt;xml&gt;&lt;node id=&quot;2&quot;/&gt;&lt;node id=&quot;1&quot;/&gt;&lt;/xml&gt;</code>
            will <b>not</b> be considered similar, whereas
            <code>&lt;xml&gt;&lt;book id=&quot;1&quot;/&gt;&lt;animal id=&quot;2&quot;/&gt;&lt;/xml&gt;</code> and
             <code>&lt;xml&gt;&lt;animal id=&quot;2&quot;/&gt;&lt;book id=&quot;1&quot;/&gt;&lt;/xml&gt;</code>
             <b>will</b> be considered similar.
            """)
    @Layout(Sections.Controls.class)
    boolean m_ignoreElementOrder = false;

    @Widget(title = "Ignore comments",
        description = "Strip all comments from the test and control document before comparing.")
    @Layout(Sections.Controls.class)
    boolean m_ignoreComments = false;

    @Widget(title = "Ignore whitespace",
        description = "Ignore whitespace by removing all empty text nodes and trimming the non-empty ones.")
    @Layout(Sections.Controls.class)
    boolean m_ignoreWhiteSpace;

    @Widget(title = "Remove whitespace-only text nodes",
        description = "Ignore element content whitespace by removing all text nodes solely consisting of whitespace.")
    @Layout(Sections.Controls.class)
    boolean m_ignoreElementContentWhitespace = false;

    @Widget(title = "Normalize whitespace", description = """
            Remove all empty text nodes and normalize the non-empty ones.
            Normalization replaces all whitespace characters by space characters
            and collapses consecutive whitespace characters.
            """)
    @Persist(optional = true)
    @Layout(Sections.Controls.class)
    boolean m_normalizeWhitespace = false;

    @Widget(title = "Ignore differences of type SIMILAR", description = """
            Filters out differences of type SIMILAR. Includes element reordering if selected above.
            The following differences are by default considered as differences of type SIMILAR:
                <ul>
                <li>CDATA and Text nodes with the same content</li>
                <li>DOCTYPE differences</li>
                <li>different xsi:schemaLocation and xsi:noNamspaceSchemaLocation</li>
                <li>different XML namespaces prefixes</li>
                <li>explicit/implicit status of attributes</li>
                <li>a different order of child nodes</li>
                <li>XML encoding</li>
                </ul>
            """)
    @Layout(Sections.Controls.class)
    boolean m_ignoreSmallDifferences = false;

    // Output section
    interface IsFailInExecution {
    }

    @Widget(title = "Node fails if diff is non-empty",
        description = "Node throws an error as soon as the first difference is found.")
    @Layout(Sections.Output.class)
    boolean m_failExecution = false;

    @Widget(title = "Source documents",
        description = "Output the compared documents as test and control document columns.")
    @Layout(Sections.Output.class)
    boolean m_addSourceColumns = true;

    @Widget(title = "Output parent XPaths columns", description = "Include the XPath of control and test element.")
    @Layout(Sections.Output.class)
    boolean m_outputParentXPaths = true;

    @Widget(title = "Output similarity column",
        description = """
                Output category column. Differences are categorized into regular <code>DIFFERENT</code> and
                small differences <code>SIMILAR</code>.""")
    @Layout(Sections.Output.class)
    boolean m_outputSimilarity = true;

    @Widget(title = "Output summary column", description = "Output a summary of the differences.")
    @Layout(Sections.Output.class)
    boolean m_outputSummary = true;

    boolean isParentXPathIncluded() {
        return m_outputParentXPaths;
    }

    boolean isSimilarityIncluded() {
        return m_outputSimilarity;
    }

    boolean isSummaryIncluded() {
        return m_outputSummary;
    }

    // UTIL
    private static class XmlColumnChoicesProvider extends CompatibleColumnChoicesProvider {
        XmlColumnChoicesProvider() {
            super(XMLValue.class);
        }

        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context, final int portIndex) {
            return context.getDataTableSpec(portIndex)//
                .map(DataTableSpec::stream).orElseGet(Stream::empty) //
                .filter(dcs -> dcs.getType().isCompatible(XMLValue.class))//
                .toArray(DataColumnSpec[]::new);
        }
    }

    private static final class TestTableXmlColumnChoicesProvider extends XmlColumnChoicesProvider {
        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return columnChoices(context, TEST_TABLE_PORT_INDEX);
        }
    }

    private static final class ControlTableXmlColumnChoicesProvider extends XmlColumnChoicesProvider {
        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return columnChoices(context, CONTROL_TABLE_PORT_INDEX);
        }
    }

}
