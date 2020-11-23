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
 *   Jun 19, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.testing.node.io.filehandling.file;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * The file difference checker node dialog.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class FileDifferenceCheckerNodeDialog extends NodeDialogPane {

    private static final String HISTORY_ID = "file_difference_checker";

    private final DialogComponentReaderFileChooser m_referenceFileChooser;

    private final DialogComponentReaderFileChooser m_testFileChooser;

    FileDifferenceCheckerNodeDialog(final FileDifferenceCheckerConfiguration config) {
        m_referenceFileChooser = new DialogComponentReaderFileChooser(config.getReferenceFileChooser(), HISTORY_ID,
            createFlowVariableModel(config.getReferenceFileChooser().getKeysForFSLocation(),
                FSLocationVariableType.INSTANCE),
            FilterMode.FILE);

        m_testFileChooser = new DialogComponentReaderFileChooser(config.getFileChooser(), HISTORY_ID,
            createFlowVariableModel(config.getFileChooser().getKeysForFSLocation(), FSLocationVariableType.INSTANCE),
            FilterMode.FILE);

        addTab("Settings", layout());
    }

    private Component layout() {
        final JPanel panel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = new GBCBuilder();
        gbc.anchorFirstLineStart().resetX().resetY().fillHorizontal().setWeightX(1).setWeightY(0)
            .setInsets(new Insets(5, 0, 0, 0));
        panel.add(createFileChooserPanel(m_testFileChooser, "Test File"), gbc.build());
        gbc.incY();
        panel.add(createFileChooserPanel(m_referenceFileChooser, "Reference File"), gbc.build());
        gbc.incY().fillBoth().setWeightY(1);
        panel.add(Box.createVerticalBox(), gbc.build());
        return panel;
    }

    private static JPanel createFileChooserPanel(final DialogComponentReaderFileChooser chooser, final String title) {
        final JPanel panel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = new GBCBuilder();
        gbc.resetX().setWeightX(1).fillHorizontal().setInsets(new Insets(0, 5, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title));
        panel.add(chooser.getComponentPanel(), gbc.build());
        return panel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_testFileChooser.saveSettingsTo(settings);
        m_referenceFileChooser.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_testFileChooser.loadSettingsFrom(settings, specs);
        m_referenceFileChooser.loadSettingsFrom(settings, specs);
    }

    @Override
    public void onClose() {
        m_referenceFileChooser.onClose();
        m_testFileChooser.onClose();
    }

}
