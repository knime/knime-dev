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
 *
 * History
 *   07.09.2011 (meinl): created
 */
package org.knime.testing.node.workflowcontext;

import java.util.regex.Pattern;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;

/**
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class TestWorkflowContextSettings {

    /**
     * Used to verify the result of {@link WorkflowContextV2#toString()}. Built from {@link #m_patternStringModel} using
     * {@link #createPatternFromMessage(String)}.
     */
    private Pattern m_contextStringRepresentationPattern;

    /**
     * This describes all valid outputs of {@link WorkflowContextV2#toString()}. By enclosing regular expressions within
     * <code>_!_</code> markers, the user can for instance abstract from random numbers in temporary directories.
     */
    private SettingsModelString m_patternStringModel = new SettingsModelString("pattern", null);

    /**
     * Loads the settings from the given settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if an expected setting is missing
     */
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

        m_patternStringModel.loadSettingsFrom(settings);

        // if no pattern is present, initialize with the current state
        if (m_patternStringModel.getStringValue() == null) {
            var context = NodeContext.getContext().getWorkflowManager().getContextV2();
            getPatternStringModel().setStringValue(context.toString());
        }

        m_contextStringRepresentationPattern = createPatternFromMessage(m_patternStringModel.getStringValue());
    }

    /**
     * Saves the settings into the given settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        m_patternStringModel.saveSettingsTo(settings);
    }

    /**
     * @return the pattern to verify the result of {@link WorkflowContextV2#toString()}
     */
    public Pattern getContextStringRepresentationPattern() {
        return m_contextStringRepresentationPattern;
    }

    /**
     * Copied verbatim from TestflowConfiguration class. The Testflow Configuration allows to define a language that
     * describes the expected log messages by letting the user enclose regular expressions between "_!_" markers.
     *
     * @param message
     * @return
     */
    private static Pattern createPatternFromMessage(String message) {
        final var REGEX_PATTERN = "_!_";
        int index = message.indexOf(REGEX_PATTERN);

        if (index < 0) {
            return Pattern.compile(Pattern.quote(message));
        } else {
            var patternString = new StringBuilder();

            while (index >= 0) {
                // non-regex part
                patternString.append("\\Q").append(message, 0, index).append("\\E");

                // regex pattern starts
                message = message.substring(index + REGEX_PATTERN.length());
                index = message.indexOf(REGEX_PATTERN);
                if (index >= 0) {
                    patternString.append(message.substring(0, index));
                } else {
                    patternString.append(message);
                }

                // regex pattern ends
                message = message.substring(index + REGEX_PATTERN.length());
                index = message.indexOf(REGEX_PATTERN);
            }
            patternString.append("\\Q").append(message).append("\\E");

            return Pattern.compile(patternString.toString(), Pattern.DOTALL);
        }
    }

    SettingsModelString getPatternStringModel() {
        return m_patternStringModel;
    }

}
