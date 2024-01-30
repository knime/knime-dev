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
 *   1 Feb 2024 (carlwitt): created
 */
package org.knime.testing.core.compare;

/**
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @noreference internal use only
 */
public final class LineDiffer {

    private LineDiffer() {}

    /**
     * Expected
     *
     * <pre>
     * Contains one node with execution failure (Table Writer #727)
     * Table Writer #727: Output file 'expectedWorkflowSummary.table' exists and must not be overwritten
     * </pre>
     *
     * Actual
     *
     * <pre>
     * Contains 2 nodes with execution failure
     * Table Writer #727: Output file 'expectedWorkflowSummary.table' exists and must not be overwritten
     * </pre>
     *
     * Output
     *
     * <pre>
     * Expected line #1> Contains one node with execution failure (Table Writer #727)
     *   Actual line #1> Contains 2 nodes with execution failure
     * </pre>
     *
     * @param expected control string
     * @param actual test string
     *
     * @return a line-by-line summary of the differences
     */
    public static String summary(final String expected, final String actual) {
        final String[] expectedLines = expected.split("\n");
        final String[] actualLines = actual.split("\n");
        final var builder = new StringBuilder();
        for (var i = 0; i < Math.max(expectedLines.length, actualLines.length); i++) {
            if (i >= expectedLines.length) {
                builder.append("Unexpected extra actual line #").append(i + 1).append("> '").append(actualLines[i])
                    .append("'\n");
            } else if (i >= actualLines.length) {
                builder.append("Missing actual line for expected line #").append(i + 1).append("> '")
                    .append(expectedLines[i]).append("'\n");
            } else if (!expectedLines[i].equals(actualLines[i])) {
                builder.append("Expected line #").append(i + 1).append("> '").append(expectedLines[i]).append("'\n");
                builder.append("  Actual line #").append(i + 1).append("> '").append(actualLines[i]).append("'\n");
            }
        }
        return builder.toString().trim();
    }
}
