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
 *   02.08.2013 (thor): created
 */
package org.knime.testing.core.ng;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.LoadVersion;
import org.knime.core.util.LockFailedException;
import org.knime.core.webui.node.view.NodeViewManager;
import org.knime.testing.core.TestrunConfiguration;
import org.knime.testing.util.TryAlwaysWorkflowLoadHelper;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

/**
 * Testcase that monitors loading a workflow. Errors and if desired also warnings during load are reported as failures.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class WorkflowLoadTest extends WorkflowTest {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowLoadTest.class);

    private final File m_workflowDir;

    private final File m_testcaseRoot;

    private final TestrunConfiguration m_runConfiguration;

    /**
     * Creates a new test for loading a workflow.
     *
     * @param workflowDir the workflow dir
     * @param testcaseRoot root directory of all test workflows; this is used as a replacement for the mount point root
     * @param workflowName a unique name for the workflow
     * @param monitor a progress monitor, may be <code>null</code>
     * @param runConfiguration the run configuration
     * @param context the test context, must not be <code>null</code>
     */
    WorkflowLoadTest(final File workflowDir, final File testcaseRoot, final String workflowName,
        final IProgressMonitor monitor, final TestrunConfiguration runConfiguration,
        final WorkflowTestContext context) {
        super(workflowName, monitor, context);
        m_workflowDir = workflowDir;
        m_testcaseRoot = testcaseRoot;
        m_runConfiguration = runConfiguration;
    }

    @Override
    public void run(final TestResult result) {
        boolean resetToDefaultWorkspaceDirRequired = setCustomWorkspaceDirPath(m_testcaseRoot);
        result.startTest(this);
        try {
            m_context.setWorkflowManager(loadWorkflow(this, result, m_workflowDir, m_testcaseRoot, m_runConfiguration));
            checkLoadVersion(this, result);
        } catch (Throwable t) { // NOSONAR
            result.addError(this, t);
        } finally {
            result.endTest(this);
            if (resetToDefaultWorkspaceDirRequired) {
                setDefaultWorkspaceDirPath();
            }
        }
    }

    @Override
    public String getName() {
        return "load workflow";
    }

    static WorkflowManager loadWorkflow(final WorkflowTest test, final TestResult result, final File workflowDir,
            final File testcaseRoot, final TestrunConfiguration runConfig) throws IOException, InvalidSettingsException,
            CanceledExecutionException, UnsupportedWorkflowVersionException, LockFailedException {

        final var ctx = createWorkflowContext(testcaseRoot.getAbsoluteFile().toPath(),
            workflowDir.getAbsoluteFile().toPath(), test.getWorkflowName());

        WorkflowLoadHelper loadHelper = new TryAlwaysWorkflowLoadHelper(ctx);

        WorkflowLoadResult loadRes = WorkflowManager.loadProject(workflowDir, new ExecutionMonitor(), loadHelper);
        if ((loadRes.getType() == LoadResultEntryType.Error)
            || ((loadRes.getType() == LoadResultEntryType.DataLoadError) && loadRes.getGUIMustReportDataLoadErrors())) {
            result.addFailure(test, new AssertionFailedError(loadRes.getFilteredError("", LoadResultEntryType.Error)));
        }
        if (runConfig.isCheckForLoadWarnings() && loadRes.hasWarningEntries()) {
            result
                .addFailure(test, new AssertionFailedError(loadRes.getFilteredError("", LoadResultEntryType.Warning)));
        }

        WorkflowManager wfm = loadRes.getWorkflowManager();

        // Makes sure that the node view settings are loaded without problems, if available
        for (var nc : wfm.getNodeContainers()) {
            var nodeViewManager = NodeViewManager.getInstance();
            if (nc instanceof NativeNodeContainer nnc && NodeViewManager.hasNodeView(nnc)) {
                nodeViewManager.updateNodeViewSettings(nnc);
            }
        }

        wfm.addWorkflowVariables(true, runConfig.getFlowVariables());
        return wfm;
    }

    private static WorkflowContextV2 createWorkflowContext(final Path rootPath, final Path wfPath,
            final String workflowName) {
        final var execBuilder = AnalyticsPlatformExecutorInfo.builder() //
                .withCurrentUserAsUserId() //
                .withLocalWorkflowPath(wfPath);
        try {
            if (wfPath.startsWith(rootPath)) {
                // regular workspace
                execBuilder.withMountpoint("LOCAL", rootPath);
            } else {
                // When running testflows from ZIPs, the workflow is extracted into a temporary
                // directory different from `testcaseRoot`, each ZIP defines a workspace
                final var pathInRoot = Path.of(workflowName);
                if (wfPath.endsWith(pathInRoot)) {
                    // `Path#subpath(...)` removes the drive letter on Windows, no idea why
                    final var zipRootPath = wfPath.resolve(IntStream.range(0, pathInRoot.getNameCount()) //
                        .mapToObj(i -> "..") //
                        .collect(Collectors.joining("/"))).normalize();
                    execBuilder.withMountpoint("LOCAL", zipRootPath);
                }
            }
        } catch (IllegalArgumentException e) { // NOSONAR
            LOGGER.warn(String.format("Could not set mountpoint '%s' for workflow located at '%s'.",
                rootPath, wfPath), e);
        }

        return WorkflowContextV2.builder().withExecutor(execBuilder.build()).withLocalLocation().build();
    }

    /**
     * Checks if the current workflow version is the same or older then the required load version. In case this is not
     * the case a test error is logged. Callers have to ensure that the workflow has already been loaded and the
     * workflow manager has been set (via {@link WorkflowTestContext#setWorkflowManager(WorkflowManager)}, otherwise
     * the settings are not available.
     *
     * @param test the current test
     * @param result the test result to which the error will be added
     */
    public static void checkLoadVersion(final WorkflowTest test, final TestResult result) {
        LoadVersion requiredLoadVersion = test.m_context.getTestflowConfiguration().requiredLoadVersion();
        LoadVersion currentLoadVersion = test.m_context.getWorkflowManager().getLoadVersion();
        if (requiredLoadVersion.isOlderThan(currentLoadVersion)) {
            result.addFailure(test,
                new AssertionFailedError(String.format(
                    "Workflow was required to stay in an older version than it is now (required: %s, actual: %s). "
                        + "It may have been accidentally saved with a newer version.",
                    requiredLoadVersion, currentLoadVersion)));
        }
    }
}
