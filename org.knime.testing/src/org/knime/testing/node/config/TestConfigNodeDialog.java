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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
package org.knime.testing.node.config;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.workflow.FileWorkflowPersistor.LoadVersion;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessage.Type;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.testing.core.TestrunJanitor;

/**
 * This is the dialog for the testflow configuration node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class TestConfigNodeDialog extends NodeDialogPane {
    private final TestConfigSettings m_settings = new TestConfigSettings();

    private final JTextField m_owner = new JTextField(15);

    private final DefaultListModel<NodeContainer> m_allNodesModel = new DefaultListModel<>();

    private final DefaultListModel<String> m_logErrorsModel = new DefaultListModel<>();

    private final DefaultListModel<String> m_logWarningsModel = new DefaultListModel<>();

    private final DefaultListModel<String> m_logInfosModel = new DefaultListModel<>();

    private final DefaultListModel<String> m_optLogMessagesModel = new DefaultListModel<>();

    private final JList<NodeContainer> m_allNodes = new JList<>(m_allNodesModel);

    private final JCheckBox m_mustFail = new JCheckBox();

    private final JTextArea m_requiredError = new JTextArea(3, 20);

    private final JTextArea m_requiredWarning = new JTextArea(3, 20);

    private final JSpinner m_timeout = new JSpinner(new SpinnerNumberModel(30, 0, 3600, 10));

    private final JSpinner m_maxHiliteRows = new JSpinner(new SpinnerNumberModel(2500, 0, Integer.MAX_VALUE, 10));

    private final JCheckBox m_streamingTest = new JCheckBox();

    private final JComboBox<String> m_requiredLoadVersion = new JComboBox<>(Arrays
        .<LoadVersion> asList(LoadVersion.values()).stream().filter((val) -> val != LoadVersion.UNKNOWN).map(val -> {
            if (val == LoadVersion.FUTURE) {
                return "None";
            } else {
                return val.getVersionString();
            }
        }).toArray((i) -> new String[i]));

    private int m_lastSelectedIndex = -1;

    private final Map<String, JCheckBox> m_usedJanitors = new HashMap<>();

    /**
     * Creates a new dialog.
     */
    @SuppressWarnings("serial")
    public TestConfigNodeDialog() {
        m_allNodes.setCellRenderer(new DefaultListCellRenderer() {
            /**
             * {@inheritDoc}
             */
            @Override
            public Component getListCellRendererComponent(final JList<?> list,
                    final Object value, final int index,
                    final boolean isSelected, final boolean cellHasFocus) {
                if (value != null) {
                    NodeContainer cont = (NodeContainer)value;
                    String text = cont.getNameWithID();
                    Component c = super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
                    if (cont.getNodeMessage().getMessageType().equals(NodeMessage.Type.ERROR)) {
                        setForeground(Color.RED.darker().darker());
                    } else {
                        setForeground(list.getForeground());
                    }
                    return c;
                } else {
                    return super.getListCellRendererComponent(list, value,
                            index, isSelected, cellHasFocus);
                }
            }
        });

        addTab("Workflow settings", createWorkflowSettingsPanel());
        addTab("Node settings", createNodeSettingsPanel());
        if (!TestrunJanitor.getJanitors().isEmpty()) {
            addTab("Testrun janitors", createJanitorsTab());
        }
    }

    private JPanel createWorkflowSettingsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.insets = new Insets(2, 0, 2, 0);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("Workflow owner's mail address:   "), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        p.add(m_owner, c);

        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        p.add(new JLabel("Execution timeout in seconds:   "), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        p.add(m_timeout, c);

        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        p.add(new JLabel("Maximum number of hilited rows:   "), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        p.add(m_maxHiliteRows, c);

        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        p.add(new JLabel("Test in streaming mode:   "), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        p.add(m_streamingTest, c);

        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        p.add(new JLabel("Require workflow version:   "), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        p.add(m_requiredLoadVersion, c);

        c.gridx = 0;
        c.gridy++;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 2;
        c.insets = new Insets(10, 0, 2, 0);
        JTabbedPane logLevels = new JTabbedPane();
        p.add(logLevels, c);

        logLevels.addTab("Log Errors", createLogLevelTab(m_logErrorsModel));
        logLevels.addTab("Log Warnings", createLogLevelTab(m_logWarningsModel));
        logLevels.addTab("Log Infos", createLogLevelTab(m_logInfosModel));
        logLevels.addTab("Log Optional", createLogLevelTab(m_optLogMessagesModel));
        return p;
    }

    private JPanel createLogLevelTab(final DefaultListModel<String> listModel) {
        JPanel p = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 0, 2, 0);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        final JTextField input = new JTextField();
        p.add(input, c);
        c.gridy++;

        c.gridx = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        JButton add = new JButton("Add");
        p.add(add, c);
        c.gridx = 1;
        JButton remove = new JButton("Remove");
        p.add(remove, c);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        final JList<String> list = new JList<>(listModel);
        p.add(new JScrollPane(list), c);

        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                String text = input.getText();
                if (text.trim().length() > 0) {
                    listModel.addElement(text.trim());
                    input.setText("");
                }
            }
        });

        remove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (list.getSelectedIndex() >= 0) {
                    listModel.remove(list.getSelectedIndex());
                }
            }
        });

        return p;
    }

    private JPanel createNodeSettingsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.insets = new Insets(2, 0, 10, 0);
        p.add(new JScrollPane(m_allNodes), c);

        JPanel p2 = new JPanel(new GridBagLayout());
        c.gridy++;
        c.insets = new Insets(2, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0.1;
        p.add(p2, c);

        p2.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        GridBagConstraints c2 = new GridBagConstraints();

        c2.anchor = GridBagConstraints.NORTHWEST;
        c2.insets = new Insets(2, 2, 2, 2);
        c2.gridx = 0;
        c2.gridy = 0;
        p2.add(new JLabel("Node is expected to fail   "), c2);
        c2.gridx = 1;
        p2.add(m_mustFail, c2);

        c2.gridx = 0;
        c2.gridy++;
        p2.add(new JLabel("Required error message   "), c2);
        c2.gridx = 1;
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.weightx = 1;
        p2.add(new JScrollPane(m_requiredError), c2);

        c2.gridx = 0;
        c2.gridy++;
        c2.weightx = 0;
        c2.fill = GridBagConstraints.NONE;
        p2.add(new JLabel("Required warning message   "), c2);
        c2.gridx = 1;
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.weightx = 1;
        JScrollPane sp = new JScrollPane(m_requiredWarning);
        sp.setMinimumSize(new Dimension(100, 100));
        p2.add(sp, c2);

        m_allNodes.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                int selected = m_allNodes.getSelectedIndex();
                if (!e.getValueIsAdjusting()) {
                    m_lastSelectedIndex = selected;
                    if (selected >= 0) {
                        updateNodeConfigurationFields(selected);
                    }
                }
            }
        });

        FocusListener focusListener = new FocusListener() {
            @Override
            public void focusLost(final FocusEvent e) {
                if (m_lastSelectedIndex >= 0) {
                    e.getComponent().setForeground(Color.BLACK);
                    storeNodeConfiguration(m_lastSelectedIndex);
                }
            }

            @Override
            public void focusGained(final FocusEvent e) {
            }
        };

        m_mustFail.addFocusListener(focusListener);
        m_requiredError.addFocusListener(focusListener);
        m_requiredWarning.addFocusListener(focusListener);

        return p;
    }


    private JPanel createJanitorsTab() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 2, 0, 2);

        for (TestrunJanitor j : TestrunJanitor.getJanitors()) {
            JCheckBox cb = new JCheckBox(j.getName());
            cb.setToolTipText(j.getDescription());
            p.add(cb, c);
            c.gridy++;

            m_usedJanitors.put(j.getID(), cb);
        }

        return p;
    }

    private void storeNodeConfiguration(final int index) {
        NodeContainer cont = m_allNodesModel.get(index);
        String nodeID =
                TestConfigSettings.getNodeIDWithoutRootPrefix(getNodeContext().getWorkflowManager(), cont);

        if (m_mustFail.isSelected()) {
            m_settings.addFailingNode(nodeID);
        } else {
            m_settings.removeFailingNode(nodeID);
        }

        if (m_requiredWarning.getText().trim().length() > 0) {
            m_settings.setRequiredNodeWarning(nodeID, m_requiredWarning
                    .getText().trim());
        } else {
            m_settings.setRequiredNodeWarning(nodeID, null);
        }

        if (m_requiredError.getText().trim().length() > 0) {
            m_settings.setRequiredNodeError(nodeID, m_requiredError.getText()
                    .trim());
        } else {
            m_settings.setRequiredNodeError(nodeID, null);
        }
    }

    private void updateNodeConfigurationFields(final int index) {
        NodeContainer cont = m_allNodesModel.get(index);
        String nodeID =
                TestConfigSettings.getNodeIDWithoutRootPrefix(getNodeContext().getWorkflowManager(), cont);

        m_mustFail.setSelected(m_settings.failingNodes().contains(nodeID));

        String reqError = m_settings.requiredNodeErrors().get(nodeID);
        if (reqError == null) {
            NodeMessage msg = cont.getNodeMessage();
            if (msg.getMessageType() == Type.ERROR) {
                reqError = msg.getMessage();
                m_requiredError.setForeground(Color.GRAY);
            }
        } else {
            m_requiredError.setForeground(Color.BLACK);
        }
        m_requiredError.setText(reqError != null ? reqError : "");

        String reqWarning = m_settings.requiredNodeWarnings().get(nodeID);
        if (reqWarning == null) {
            NodeMessage msg = cont.getNodeMessage();
            if (msg.getMessageType() == Type.WARNING) {
                reqWarning = msg.getMessage();
                m_requiredWarning.setForeground(Color.GRAY);
            }
        } else {
            m_requiredWarning.setForeground(Color.BLACK);
        }
        m_requiredWarning.setText(reqWarning != null ? reqWarning : "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.owner(m_owner.getText());
        m_settings.timeout((Integer) m_timeout.getValue());
        m_settings.maxHiliteRows((Integer) m_maxHiliteRows.getValue());
        m_settings.streamingTest(m_streamingTest.isSelected());

        final String requiredLoadVersionName = (String)m_requiredLoadVersion.getSelectedItem();
        m_settings.requiredLoadVersion(TestConfigSettings.parseLoadVersion(requiredLoadVersionName));

        List<String> temp = new ArrayList<String>();
        for (int i = 0; i < m_logErrorsModel.getSize(); i++) {
            temp.add(m_logErrorsModel.get(i));
        }
        m_settings.requiredLogErrors(temp);

        temp.clear();
        for (int i = 0; i < m_logWarningsModel.getSize(); i++) {
            temp.add(m_logWarningsModel.get(i));
        }
        m_settings.requiredLogWarnings(temp);

        temp.clear();
        for (int i = 0; i < m_logInfosModel.getSize(); i++) {
            temp.add(m_logInfosModel.get(i));
        }
        m_settings.requiredLogInfos(temp);

        temp.clear();
        for (int i = 0; i < m_optLogMessagesModel.getSize(); i++) {
            temp.add(m_optLogMessagesModel.get(i));
        }
        m_settings.optionalLogMessages(temp);

        List<String> janitorIds = new ArrayList<>(m_usedJanitors.size());
        for (Map.Entry<String, JCheckBox> e : m_usedJanitors.entrySet()) {
            if (e.getValue().isSelected()) {
                janitorIds.add(e.getKey());
            }
        }
        m_settings.usedJanitors(janitorIds);

        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);
        m_owner.setText(m_settings.owner());
        m_timeout.setValue(m_settings.timeout());
        m_maxHiliteRows.setValue(m_settings.maxHiliteRows());
        m_streamingTest.setSelected(m_settings.streamingTest());

        LoadVersion loadVersion = m_settings.requiredLoadVersion();
        m_requiredLoadVersion
            .setSelectedItem((loadVersion == LoadVersion.FUTURE) ? "None" : loadVersion.getVersionString());

        m_logErrorsModel.removeAllElements();
        for (String l : m_settings.requiredLogErrors()) {
            m_logErrorsModel.addElement(l);
        }

        m_logWarningsModel.removeAllElements();
        for (String l : m_settings.requiredLogWarnings()) {
            m_logWarningsModel.addElement(l);
        }

        m_logInfosModel.removeAllElements();
        for (String l : m_settings.requiredLogInfos()) {
            m_logInfosModel.addElement(l);
        }

        m_optLogMessagesModel.removeAllElements();
        for (String l : m_settings.optionalLogMessages()) {
            m_optLogMessagesModel.addElement(l);
        }

        fillNodeList();

        List<String> janitorIds = m_settings.usedJanitors();
        for (Map.Entry<String, JCheckBox> e : m_usedJanitors.entrySet()) {
            e.getValue().setSelected(janitorIds.contains(e.getKey()));
        }
    }


    private void fillNodeList(final WorkflowManager root, final Set<String> existingNodeIds) {
        for (NodeContainer cont : root.getNodeContainers()) {
            if (cont instanceof NativeNodeContainer) {
                if (!(((NativeNodeContainer)cont).getNode().getNodeModel() instanceof TestConfigNodeModel)) {
                    m_allNodesModel.addElement(cont);
                    existingNodeIds.add(TestConfigSettings.getNodeIDWithoutRootPrefix(getNodeContext()
                            .getWorkflowManager(), cont));
                }
            } else if (cont instanceof SubNodeContainer) {
//                m_allNodesModel.addElement(cont);
//                existingNodeIds.add(TestConfigSettings
//                        .getNodeIDWithoutRootPrefix(getNodeContext().getWorkflowManager(), cont));
                fillNodeList(((SubNodeContainer)cont).getWorkflowManager(), existingNodeIds);
            } else if (cont instanceof WorkflowManager) {
                m_allNodesModel.addElement(cont);
                existingNodeIds.add(TestConfigSettings
                        .getNodeIDWithoutRootPrefix(getNodeContext().getWorkflowManager(), cont));
                fillNodeList((WorkflowManager) cont, existingNodeIds);
            }
        }

    }

    private void fillNodeList() {
        m_allNodesModel.removeAllElements();

        Set<String> existingNodeIds = new HashSet<String>();
        fillNodeList(getNodeContext().getWorkflowManager(), existingNodeIds);

        // remove config of non-existing nodes
        Set<String> set = new HashSet<String>();
        for (String nodeId : m_settings.requiredNodeErrors().keySet()) {
            if (!existingNodeIds.contains(nodeId)) {
                set.add(nodeId);
            }
        }
        for (String s : set) {
            m_settings.setRequiredNodeError(s, null);
        }


        set.clear();
        for (String nodeId : m_settings.requiredNodeWarnings().keySet()) {
            if (!existingNodeIds.contains(nodeId)) {
                set.add(nodeId);
            }
        }
        for (String s : set) {
            m_settings.setRequiredNodeWarning(s, null);
        }
    }
}
