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
 *   18.09.2013 (thor): created
 */
package org.knime.testing.core.ng;

import java.lang.management.MemoryUsage;

import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.testing.core.TestrunConfiguration;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

/**
 * Testcase that checks the used heap before and after a testflows is run. If the difference is greater than the
 * threshold (see {@link TestrunConfiguration#getAllowedMemoryIncrease()} the test fails.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class WorkflowMemLeakTest extends WorkflowTest {
    private final TestrunConfiguration m_runConfiguration;

    private final MemoryUsage m_initalUsage;

    WorkflowMemLeakTest(final String workflowName, final IProgressMonitor monitor,
                        final TestrunConfiguration runConfiguration, final WorkflowTestContext context) {
        super(workflowName, monitor, context);
        m_runConfiguration = runConfiguration;
        m_initalUsage = getHeapUsage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "memory leaks";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final TestResult result) {
        if (!m_context.getTestflowConfiguration().executeWithCurrentTableBackend()) {
            // if no nodes run, then there is also no point in checking for memory leaks
            ignoreTest(result);
            return;
        }

        result.startTest(this);

        try {
            MemoryUsage currentUsage = getHeapUsage();
            long diff = currentUsage.getUsed() - m_initalUsage.getUsed();
            if (diff > m_runConfiguration.getAllowedMemoryIncrease()) {
                result.addFailure(this, new AssertionFailedError("Heap usage increased by " + diff
                        + " bytes which is more than the allowed " + m_runConfiguration.getAllowedMemoryIncrease()
                        + "bytes (before test: " + m_initalUsage.getUsed() + ", after test: " + currentUsage.getUsed()));
            }
        } catch (Throwable t) {
            result.addError(this, t);
        } finally {
            result.endTest(this);
        }
    }
}
