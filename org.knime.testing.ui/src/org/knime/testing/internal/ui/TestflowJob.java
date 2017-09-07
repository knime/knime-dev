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
 * History
 *   21.08.2013 (thor): created
 */
package org.knime.testing.internal.ui;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.ui.node.workflow.UIWorkflowManager;
import org.knime.core.util.FileUtil;
import org.knime.testing.core.TestrunConfiguration;
import org.knime.testing.core.ng.WorkflowTestResult;
import org.knime.testing.core.ng.WorkflowTestSuite;
import org.knime.testing.core.ng.XMLResultFileWriter;
import org.knime.workbench.editor2.WorkflowManagerInput;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.osgi.framework.FrameworkUtil;

/**
 * Job for running a single testflow.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class TestflowJob extends Job {
    private final List<LocalExplorerFileStore> m_filestores;

    private final DateFormat m_dateFormatter = new SimpleDateFormat("yy-MM-dd_hhmmss", Locale.US);

    private final TestrunConfiguration m_runConfig;

    private final IWorkbenchWindow m_activeWindow;

    TestflowJob(final List<LocalExplorerFileStore> workflowFilestores, final TestrunConfiguration runConfig,
        final IWorkbenchWindow activeWindow) {
        super(workflowFilestores.size() + " testflows");
        m_filestores = workflowFilestores;
        m_runConfig = runConfig;
        m_activeWindow = activeWindow;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        try {
            monitor.beginTask("Executing testflows", m_filestores.size());

            File resultFile = FileUtil.createTempFile("Testflows_" + m_dateFormatter.format(new Date()), ".xml", true);
            XMLResultFileWriter resultWriter = new XMLResultFileWriter(resultFile);
            resultWriter.startSuites();

            for (LocalExplorerFileStore fs : m_filestores) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                monitor.subTask("Executing " + fs.getName());
                if (!closeEditor(fs, true)) {
                    return Status.CANCEL_STATUS;
                }
                executeTestflow(new SubProgressMonitor(monitor, 1, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK),
                    fs, resultWriter);
            }

            resultWriter.endSuites();
            displayResults(resultFile);
            return Status.OK_STATUS;
        } catch (Exception ex) {
            Status status =
                new Status(IStatus.ERROR, FrameworkUtil.getBundle(getClass()).getSymbolicName(),
                    "Error while executing the testflows", ex);
            return status;
        }
    }

    private void displayResults(final File resultFile) throws CoreException {
        JUnitCore.importTestRunSession(resultFile);
        final AtomicReference<PartInitException> exception = new AtomicReference<PartInitException>();
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                        .showView("org.eclipse.jdt.junit.ResultView");
                } catch (PartInitException ex) {
                    exception.set(ex);
                }
            }

        });
        if (exception.get() != null) {
            throw exception.get();
        }
    }

    private void executeTestflow(final IProgressMonitor monitor, final LocalExplorerFileStore fs,
        final XMLResultFileWriter resultWriter) throws CoreException, IOException, ParserConfigurationException,
        TransformerException {
        File workflowDir = fs.toLocalFile();
        File mountPointRoot = fs.getContentProvider().getFileStore("/").toLocalFile();

        m_runConfig.setCloseWorkflowAfterTest(false);

        WorkflowTestSuite suite = new GUIWorkflowTestSuite(workflowDir, mountPointRoot, m_runConfig, monitor);
        WorkflowTestResult result = WorkflowTestSuite.runTest(suite, resultWriter);
        resultWriter.addResult(result);
        if (result.wasSuccessful()) {
            closeEditor(fs, false);
        }
    }


    private boolean closeEditor(final LocalExplorerFileStore fs, final boolean saveChanges) throws CoreException {
        LocalExplorerFileStore workflowFile = fs.getChild(WorkflowPersistor.WORKFLOW_FILE);
        IEditorInput editorInput = new FileStoreEditorInput(workflowFile);
        IEditorPart editor = m_activeWindow.getActivePage().findEditor(editorInput);
        UIWorkflowManager wfm = null;
        if (editor == null) {
            for (IEditorReference editorRef : m_activeWindow.getActivePage().getEditorReferences()) {
                IEditorInput input = editorRef.getEditorInput();

                if ((input instanceof WorkflowManagerInput) &&
                        fs.toLocalFile().toURI().equals(((WorkflowManagerInput) input).getWorkflowLocation())) {
                    editor = editorRef.getEditor(false);
                    wfm = ((WorkflowManagerInput) input).getWorkflowManager();
                    break;
                }
            }
        }
        final AtomicBoolean ret = new AtomicBoolean(true);
        if (editor != null) {
            final IEditorPart finalEditor = editor;
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    ret.set(finalEditor.getEditorSite().getPage().closeEditor(finalEditor, saveChanges));
                }
            });
        }
        return ret.get();
    }
}
