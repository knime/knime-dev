/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * History
 *   21.08.2013 (thor): created
 */
package org.knime.testing.internal.ui;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.testing.core.ng.TestrunConfiguration;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.view.ContentObject;

/**
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public class RunTestflowAction implements IObjectActionDelegate {
    private LocalExplorerFileStore m_filestore;

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final IAction action) {
        TestrunConfiguration runConfig = getRunConfiguration();
        if (runConfig == null) {
            return;
        }

        IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        LocalExplorerFileStore workflowFile = m_filestore.getChild(WorkflowPersistor.WORKFLOW_FILE);
        IEditorInput editorInput = new FileStoreEditorInput(workflowFile);
        IEditorPart editor = activeWindow.getActivePage().findEditor(editorInput);
        if ((editor != null) && !editor.getEditorSite().getPage().closeEditor(editor, true)) {
            return;
        }

        IEditorDescriptor editorDescriptor;
        try {
            editorDescriptor = IDE.getEditorDescriptor(workflowFile.getName());
            editor = activeWindow.getActivePage().openEditor(editorInput, editorDescriptor.getId());
            Job job =
                new TestflowJob("Testflow " + m_filestore.getName(), m_filestore,
                    ((WorkflowEditor)editor).getWorkflowManager(), runConfig);
            job.setUser(true);
            job.schedule();
        } catch (PartInitException ex) {
            MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error while executing testflow",
                ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectionChanged(final IAction action, final ISelection selection) {
        m_filestore = null;
        if (selection instanceof IStructuredSelection) {
            Object selectedObject = ((IStructuredSelection)selection).getFirstElement();
            if (selectedObject instanceof ContentObject) {
                if (((ContentObject)selectedObject).getObject() instanceof LocalExplorerFileStore) {
                    LocalExplorerFileStore fs = (LocalExplorerFileStore)((ContentObject)selectedObject).getObject();
                    if (fs.fetchInfo().isWorkflow()) {
                        m_filestore = fs;
                    }
                }
            }
        }
        action.setEnabled(m_filestore != null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setActivePart(final IAction action, final IWorkbenchPart targetPart) {

    }

    private TestrunConfiguration getRunConfiguration() {
        TestrunConfiguration runConfig = new TestrunConfiguration();
        TestrunConfigDialog dialog = new TestrunConfigDialog(Display.getCurrent().getActiveShell(), runConfig);
        dialog.setBlockOnOpen(true);
        if (dialog.open() == Window.OK) {
            return runConfig;
        } else {
            return null;
        }
    }
}
