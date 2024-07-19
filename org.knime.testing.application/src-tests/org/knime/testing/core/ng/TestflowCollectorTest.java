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
 *   23 Jul 2024 (leonard.woerteler): created
 */
package org.knime.testing.core.ng;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.knime.testing.core.TestrunConfiguration;

import junit.framework.TestResult;

/**
 * Tests for the {@link TestflowCollector}.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
final class TestflowCollectorTest {

    /**
     * Tests that zipped workflows are collected correctly and are executed in the workflow context that represents
     * exactly the context of each zip file (see AP-22900).
     *
     * @throws IOException
     */
    @SuppressWarnings("static-method")
    @Test
    void testZippedWorkflows() throws IOException {
        final TestflowCollector collector = new TestflowCollector(null, null, new File("test-resources/zips/"));
        final TestrunConfiguration config = new TestrunConfiguration();
        config.setEnableStreamingMode(false);
        final var testCases = new ArrayList<>(collector.collectTestCases(config));
        assertThat(testCases).as("Test cases").hasSize(4);

        // actually run the testflows, which check the workflow context
        final var names = new ArrayList<String>();
        for (final var test : testCases) {
            names.add(test.getWorkflowName());
            final TestResult res = new TestResult();
            test.run(res);
            assertThat(toList(res.errors())).as("Test errors").isEmpty();
            assertThat(toList(res.failures())).as("Test failures").isEmpty();
        }

        // compare the names in an OS-independent way
        assertThat(names).as("Testflow names").containsExactlyInAnyOrder("single", "single",
            Path.of("sub/caller").toString(), Path.of("sub/callee").toString());
    }

    private static <T> List<T> toList(final Enumeration<T> enumeration) {
        final List<T> out = new ArrayList<>();
        enumeration.asIterator().forEachRemaining(out::add);
        return out;
    }
}
