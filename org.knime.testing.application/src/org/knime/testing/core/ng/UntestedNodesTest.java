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
 *   27.08.2013 (thor): created
 */
package org.knime.testing.core.ng;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeSetFactory;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

/**
 * Testcase that records all nodes in the loaded test workflows and compares them with the list of all available nodes
 * (by querying the extension point).
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
class UntestedNodesTest implements TestWithName {

    private final Set<String> m_testedNodes = new HashSet<>();

    private Set<String> m_includedPlugins;

    private final Set<String> m_nodesToTest;

    /**
     * Testcase for a single missing node
     */
    private class UntestedNodeTest implements TestWithName {

        private final String m_nodefactoryID;

        UntestedNodeTest(final String nodefactoryID) {
            m_nodefactoryID = nodefactoryID;
        }

        @Override
        public int countTestCases() {
            return 1;
        }

        @Override
        public void run(final TestResult result) {
            result.startTest(this);
            if (!m_testedNodes.contains(m_nodefactoryID)) {
                result.addFailure(this, new AssertionFailedError("No testflow with " + m_nodefactoryID + " found"));
            }
            result.endTest(this);
        }

        @Override
        public String getName() {
            return m_nodefactoryID;
        }

        @Override
        public String getSuiteName() {
            return "";
        }

    }

    UntestedNodesTest(final Set<String> includedPlugins) {
        m_includedPlugins = includedPlugins;

        IExtensionRegistry registry = Platform.getExtensionRegistry();

        // get nodes
        IExtensionPoint nodeExtensionPoint = registry.getExtensionPoint("org.knime.workbench.repository.nodes");
        if (nodeExtensionPoint == null) {
            throw new IllegalStateException("Invalid extension point : org.knime.workbench.repository.nodes");
        }

        Set<String> allAvailableNodes = new HashSet<>();
        for (IExtension ext : nodeExtensionPoint.getExtensions()) {
            for (IConfigurationElement e : ext.getConfigurationElements()) {
                if (m_includedPlugins.contains(e.getContributor().getName())) {
                    allAvailableNodes.add(e.getAttribute("factory-class"));
                }
            }
        }

        // get nodes from NodeSetFactories
        IExtensionPoint nodeSetPoint = registry.getExtensionPoint("org.knime.workbench.repository.nodesets");
        if (nodeSetPoint == null) {
            throw new IllegalStateException("Invalid extension point : org.knime.workbench.repository.nodesets");
        }

        for (IExtension ext : nodeSetPoint.getExtensions()) {
            for (IConfigurationElement e : ext.getConfigurationElements()) {
                if (m_includedPlugins.contains(ext.getContributor().getName())) {
                    try {
                        NodeSetFactory factory = (NodeSetFactory)e.createExecutableExtension("factory-class");
                        allAvailableNodes.addAll(factory.getNodeFactoryIds());
                    } catch (CoreException ex) {
                        throw new IllegalStateException(
                            "Invalid extension point : org.knime.workbench.repository.nodesets", ex);
                    }
                }
            }
        }

        m_nodesToTest = allAvailableNodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countTestCases() {
        return m_nodesToTest.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final TestResult result) {
        result.startTest(this);
        try {
            m_nodesToTest.forEach(id -> new UntestedNodeTest(id).run(result));
        } catch (RuntimeException t) {
            result.addError(this, t);
        } finally {
            result.endTest(this);
        }
    }

    /**
     * Writes a CSV reports to the given directory, listing tested and untested nodes.
     *
     * @param outputDir directory to write the reports to
     * @throws IOException
     */
    public void createCSVReport(final Path outputDir) throws IOException {
        // create target directory if it does not exist yet
        Files.createDirectories(outputDir);

        Path tested = outputDir.resolve("nodes_tested.csv");
        Path untested = outputDir.resolve("nodes_untested.csv");

        // get untested nodes
        var untestedNodes = new HashSet<String>();
        var testedNodes = new HashSet<String>();

        for (String n : m_nodesToTest) {
            if (m_testedNodes.contains(n)) {
                // m_testedNodes contains all nodes, not just the ones we are looking for
                testedNodes.add(n);
            } else {
                untestedNodes.add(n);
            }
        }

        Files.write(tested, testedNodes);
        Files.write(untested, untestedNodes);
    }

    /**
     * Adds the given set of factory class names to the nodes under test.
     *
     * @param set a set with factory class names
     */
    public void addNodesUnderTest(final Set<String> set) {
        m_testedNodes.addAll(set);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "untested nodes";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSuiteName() {
        return getClass().getName() + ".assertions_" + (KNIMEConstants.ASSERTIONS_ENABLED ? "on" : "off");
    }
}
