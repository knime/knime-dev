/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 * Created on 22.08.2013 by thor
 */
package org.knime.testing.internal.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.knime.testing.core.ng.TestrunConfiguration;

/**
 * Dialog in which the user can select which tests should be performed (in addition to standard test such as execute and
 * check for exceptions).
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class TestrunConfigDialog extends Dialog {
    private final TestrunConfiguration m_runConfig;

    private Button m_checkLogMessages;

    private Button m_checkNodeMessages;

    private Button m_reportDeprecatedNodes;

    private Button m_testDialogs;

    private Button m_testViews;

    private Button m_loadSaveLoad;

    TestrunConfigDialog(final Shell shell, final TestrunConfiguration runConfig) {
        super(shell);
        m_runConfig = runConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        getShell().setText("Testrun Configuration");

        Composite container = (Composite)super.createDialogArea(parent);

        Label l = new Label(container, SWT.WRAP);
        l.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, true, false));
        l.setText("Select which tests should be run in addition to the standard tests.");

        m_loadSaveLoad = new Button(container, SWT.CHECK);
        m_loadSaveLoad.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        m_loadSaveLoad.setText("Load-&save-load instead of load only");
        m_loadSaveLoad.setSelection(m_runConfig.isLoadSaveLoad());

        m_reportDeprecatedNodes = new Button(container, SWT.CHECK);
        m_reportDeprecatedNodes.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        m_reportDeprecatedNodes.setText("Check for de&precated nodes");
        m_reportDeprecatedNodes.setSelection(m_runConfig.isReportDeprecatedNodes());

        m_testViews = new Button(container, SWT.CHECK);
        m_testViews.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        m_testViews.setText("Test &views (open && close)");
        m_testViews.setSelection(m_runConfig.isTestViews());

        m_testDialogs = new Button(container, SWT.CHECK);
        m_testDialogs.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        m_testDialogs.setText("Test &dialogs (load && save)");
        m_testDialogs.setSelection(m_runConfig.isTestDialogs());

        m_checkLogMessages = new Button(container, SWT.CHECK);
        m_checkLogMessages.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        m_checkLogMessages.setText("Check &log messages");
        m_checkLogMessages.setSelection(m_runConfig.isCheckLogMessages());

        m_checkNodeMessages = new Button(container, SWT.CHECK);
        m_checkNodeMessages.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        m_checkNodeMessages.setText("Check &node messages");
        m_checkNodeMessages.setSelection(m_runConfig.isCheckNodeMessages());


        return container;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Point getInitialSize() {
        return new Point(250, 300);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void okPressed() {
        m_runConfig.setLoadSaveLoad(m_loadSaveLoad.getSelection());
        m_runConfig.setCheckLogMessages(m_checkLogMessages.getSelection());
        m_runConfig.setCheckNodeMessages(m_checkNodeMessages.getSelection());
        m_runConfig.setReportDeprecatedNodes(m_reportDeprecatedNodes.getSelection());
        m_runConfig.setTestDialogs(m_testDialogs.getSelection());
        m_runConfig.setTestViews(m_testViews.getSelection());
        super.okPressed();
    }
}
