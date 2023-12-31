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
 *   19.08.2013 (thor): created
 */
package org.knime.testing.core.ng;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.taskdefs.optional.junit.IgnoredTestResult;

import junit.framework.Test;
import junit.framework.TestResult;

/**
 * Extension of {@link TestResult} which collects some additional information.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class WorkflowTestResult extends IgnoredTestResult {
    private final TestWithName m_suite;

    private final StringBuilder m_sysout = new StringBuilder();

    private final StringBuilder m_syserr = new StringBuilder();

    private final List<Test> m_allTests = new ArrayList<Test>();

    private final Set<Test> m_skipped = new HashSet<>();

    /**
     * Creates a new test result for the given test suite.
     *
     * @param suite a test suite
     */
    public WorkflowTestResult(final TestWithName suite) {
        m_suite = suite;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void testIgnored(final Test test) throws Exception {
        super.testIgnored(test);
        m_skipped.add(test);
    }

    /**
     * Handles output to <tt>System.out</tt>.
     *
     * @param cbuf array of characters
     * @param off offset from which to start writing characters
     * @param len number of characters to write
     */
    public void handleSystemOut(final char[] cbuf, final int off, final int len) {
        m_sysout.append(cbuf, off, len);
    }

    /**
     * Handles output to <tt>System.err</tt>.
     *
     * @param cbuf array of characters
     * @param off offset from which to start writing characters
     * @param len number of characters to write
     */
    public void handleSystemErr(final char[] cbuf, final int off, final int len) {
        m_syserr.append(cbuf, off, len);
    }

    /**
     * Returns the collected output to <tt>System.out</tt>.
     *
     * @return the output
     */
    public String getSystemOut() {
        return m_sysout.toString();
    }

    /**
     * Returns the collected output to <tt>System.err</tt>.
     *
     * @return the output
     */
    public String getSystemErr() {
        return m_syserr.toString();
    }

    /**
     * Returns the suite to which this result belongs.
     *
     * @return a test suite
     */
    public TestWithName getSuite() {
        return m_suite;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startTest(final Test test) {
        super.startTest(test);
        m_allTests.add(test);
    }

    /**
     * Returns a list with all tests that have been run as part of this workflow test. The order in the list is the
     * order in which they have been started.
     *
     * @return a list with tests
     */
    public List<Test> getAllTests() {
        return Collections.unmodifiableList(m_allTests);
    }

    /**
     * Returns a (possibly empty) list of skipped tests.
     *
     * @return a list with skipped tests, never <code>null</code>
     */
    public Collection<Test> getSkippedTests() {
        return Collections.unmodifiableSet(m_skipped);
    }
}
