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
 *   Mar 13, 2015 (wiswedel): created
 */
package org.knime.testing.node.failing;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Config to node.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class FailingNodeConfiguration {

    private int m_failAtIndex = -1;

    private boolean m_failDuringConfiguration;

    /** @return the failAfterNumber */
    int getFailAtIndex() {
        return m_failAtIndex;
    }

    /** @param value the failAtIndex to set */
    void setFailAtIndex(final int value) {
        m_failAtIndex = value;
    }

    /** @return the failDuringConfiguration */
    boolean isFailDuringConfiguration() {
        return m_failDuringConfiguration;
    }

    /** @param value the failDuringConfiguration to set */
    void setFailDuringConfiguration(final boolean value) {
        m_failDuringConfiguration = value;
    }

    void saveSettings(final NodeSettingsWO s) {
        s.addInt("failAtIndex", m_failAtIndex);
        s.addBoolean("failDuringConfiguration", m_failDuringConfiguration);
    }

    FailingNodeConfiguration loadSettingsInModel(final NodeSettingsRO s) throws InvalidSettingsException {
        m_failAtIndex = s.getInt("failAtIndex", -1); // added in 2.12
        m_failDuringConfiguration = s.getBoolean("failDuringConfiguration", false); // added in 5.5
        return this;
    }

    void loadSettingsInDialog(final NodeSettingsRO s) {
        m_failAtIndex = s.getInt("failAtIndex", -1);
        m_failDuringConfiguration = s.getBoolean("failDuringConfiguration", false);
    }
}
