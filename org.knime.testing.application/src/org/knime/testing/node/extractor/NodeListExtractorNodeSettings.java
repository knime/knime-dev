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
package org.knime.testing.node.extractor;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DefaultProvider;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 *
 * @author wiswedel
 */
@SuppressWarnings("restriction")
public final class NodeListExtractorNodeSettings implements DefaultNodeSettings {

    @Persist(customPersistor = IncludeNodeFactoryIDPersistor.class)
    @Widget(title = "NodeFactory ID",
    description = "The node factory ID")
    boolean m_includeNodeFactoryID;

    @Persist(configKey = "includeNodeDescription", defaultProvider = TrueProvider.class)
    @Widget(title = "Node Description",
    description = "If selected, includes a column containing the full node description")
    boolean m_includeNodeDescription;

    @Persist(configKey = "includeKeywords", optional = true)
    @Widget(title = "Keywords",
    description = "If selected, includes a column containing the keywords used during (fuzzy) node search")
    boolean m_includeKeywords;

    private static final class IncludeNodeFactoryIDPersistor implements FieldNodeSettingsPersistor<Boolean> {
        private static final String INCL_ID = "includeNodeFactoryID"; // >= 5.3
        private static final String INCL = "includeNodeFactory"; // 5.0 until 5.2

        @Override
        public Boolean load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(INCL)) {
                return settings.getBoolean(INCL);
            }
            return settings.getBoolean(INCL_ID, true);
        }

        @Override
        public void save(final Boolean obj, final NodeSettingsWO settings) {
            settings.addBoolean(INCL_ID, obj);
        }

        @Override
        public String[] getConfigKeys() {
            return new String[] {INCL_ID};
        }
    }

    private static final class TrueProvider implements DefaultProvider<Boolean> {
        @Override
        public Boolean getDefault() {
            return true;
        }

    }
}
