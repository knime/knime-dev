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

import org.knime.core.data.xml.XMLValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProviderUtil.CompatibleColumnChoicesProvider;

/**
 * Settings class for the XmlDiffer node.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
final class XmlDifferNodeSettings implements DefaultNodeSettings {

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

    // INPUT

    @Widget(title = "Test document column",
        description = "Provides the actual XML document to compare to the values provided by the control column.")
    @ChoicesWidget(choices = XmlColumnChoicesProvider.class)
    @Layout(Sections.Input.class)
    String m_testColumn;

    @Widget(title = "Control document column", description = "Provides the expected values to compare against.")
    @ChoicesWidget(choices = XmlColumnChoicesProvider.class)
    @Layout(Sections.Input.class)
    String m_controlColumn;

    // CONTROLS

    @Widget(title = "Ignore element order", description = "If checked, the order of elements is ignored.")
    @Layout(Sections.Controls.class)
    boolean m_ignoreElementOrder = false;

    @Widget(title = "Ignore comments",
        description = "Strip all comments from the test and control document before comparing.")
    @Layout(Sections.Controls.class)
    boolean m_ignoreComments = false;

    @Widget(title = "Ignore whitespace",
        description = "Remove all empty text nodes and trim the non-empty ones from the test and control document before comparing.")
    @Layout(Sections.Controls.class)
    boolean m_ignoreWhiteSpace;

    @Widget(title = "Ignore element content whitespace",
        description = "Remove all text nodes that consist of only whitespace from the test and control document before comparing.")
    @Layout(Sections.Controls.class)
    boolean m_ignoreElementContentWhitespace;

    @Widget(title = "Ignore small differences", description = """
            Omits the following differences from the output:
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

    // OUTPUT

    @Widget(title = "Remove source columns", description = "Removes the test and control column from the output.")
    @Layout(Sections.Output.class)
    boolean m_removeSourceColumns = false;

    @Widget(title = "Stop after first difference",
        description = "If checked, at most one difference will be reported per row, i.e., pair of XML documents.")
    @Layout(Sections.Output.class)
    boolean m_stopAfterFirstDifference = false;

    private static final class XmlColumnChoicesProvider extends CompatibleColumnChoicesProvider {
        XmlColumnChoicesProvider() {
            super(XMLValue.class);
        }
    }

}
