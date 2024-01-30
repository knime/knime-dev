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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.xml.XMLValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.rule.TrueCondition;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
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

//    @Widget(title = "Control document column", description = "Provides the expected values to compare against.")
//    @ChoicesWidget(choices = SecondTableXmlColumnChoicesProvider.class)
//    @Layout(Sections.Input.class)
//    String m_externalControlColumn;


    // CONTROLS

    @Widget(title = "Tolerate element reordering", description = """
            If checked, a reordering of elements is output as different of type SIMILAR instead of type DIFFERENT.
            Use
            """)
    @Layout(Sections.Controls.class)
    boolean m_ignoreElementOrder = false;

    @Widget(title = "Ignore comments",
        description = "Strip all comments from the test and control document before comparing.")
    @Layout(Sections.Controls.class)
    boolean m_ignoreComments = false;

    @Widget(title = "Ignore whitespace",
        description = """
                      Ignore whitespace by removing all empty text nodes and trimming the non-empty ones.
                      If you only want to remove text nodes consisting solely of whitespace
                      (AKA element content whitespace) but leave all other text nodes alone you should
                      use ignore element content whitespace instead.
                      """)
    @Layout(Sections.Controls.class)
    boolean m_ignoreWhiteSpace;

    @Widget(title = "Ignore element content whitespace",
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

    // OUTPUT

    interface IsFailInExecution {
    }

    @Widget(title = "Node fails if diff is non-empty",
        description = "Node throws an error as soon as the first difference is found.")
    @Layout(Sections.Output.class)
    @Signal(id = IsFailInExecution.class, condition = TrueCondition.class)
    boolean m_failExecution = false;

    @Widget(title = "Remove source columns", description = "Removes the test and control column from the output.")
    @Layout(Sections.Output.class)
    @Effect(signals = IsFailInExecution.class, type = EffectType.HIDE)
    boolean m_removeSourceColumns = false;

    // UTIL

    private static final class XmlColumnChoicesProvider extends CompatibleColumnChoicesProvider {
        XmlColumnChoicesProvider() {
            super(XMLValue.class);
        }
    }



    private static final class SecondTableXmlColumnChoicesProvider implements ColumnChoicesProvider {
        SecondTableXmlColumnChoicesProvider() {
        }

        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            final var spec = context.getDataTableSpec(1);
            if (spec.isEmpty()) {
                return new DataColumnSpec[0];
            } else {
                return spec.get().stream().map(DataColumnSpec::getType) //
                    .filter(t -> t.isCompatible(XMLValue.class)) //
                    .toArray(DataColumnSpec[]::new);
            }
        }
    }

}
