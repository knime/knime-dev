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
 * ------------------------------------------------------------------------
 */
package org.knime.testing.core.ng;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.knime.core.node.Node;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSetFactory;
import org.knime.core.node.workflow.virtual.VirtualNodeSetFactory;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

/**
 * This application instantiate all nodes of a list of plug-ins in headless mode (<tt>java.awt.headless=true</tt>). The
 * result is written into an XML file identical to the one produced by ANT's &lt;junit> task. It can then be analyzed
 * further.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings({"squid:S106"})
public class HeadlessNodeInstantiationApplication implements IApplication {

    private static final String FACTORY = "factory-class";

    private static final Set<String> SKIPPED_NODESET_FACTORIES = Set.of(VirtualNodeSetFactory.class.getCanonicalName());

    private Set<String> m_includedPlugins;

    private String m_xmlResultFile;

    /**
     * Test that tries to instantiate a node via the NodeFactory supplied to it.
     */
    private static class NodeInstantiationTest implements TestWithName {

        private final String m_factoryID;

        private final Callable<NodeFactory<NodeModel>> m_nodeFactorySupplier;

        /**
         * @param factoryID the factory id (used to set the name of the test)
         * @param nodeFactorySupplier Callable that returns the nodefactoy that should be instantiated
         */
        NodeInstantiationTest(final String factoryID, final Callable<NodeFactory<NodeModel>> nodeFactorySupplier) {
            m_factoryID = factoryID;
            m_nodeFactorySupplier = nodeFactorySupplier;
        }

        @Override
        public int countTestCases() {
            return 1;
        }

        @Override
        public void run(final TestResult result) {
            result.startTest(this);
            try {
                // try to instantiate node
                new Node(m_nodeFactorySupplier.call()).getNodeModel();
            } catch (Exception ex) {
                var err = new AssertionFailedError("Could not instantiate " + m_factoryID + ": " + ex.getMessage());
                err.initCause(ex);
                result.addFailure(this, err);
            } catch (Throwable t) { // NOSONAR
                result.addError(this, t);
            } finally {
                result.endTest(this);
            }
        }

        @Override
        public String getName() {
            return m_factoryID;
        }

        @Override
        public String getSuiteName() {
            return "instantiate";
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    /**
     * Node instantiation test for a {@link NodeSetFactory}. It creates a {@link NodeInstantiationTest} for each node
     * factory covered by the {@link NodeSetFactory} under test.
     */
    private static class NodeSetInstantiationTest implements TestWithName {

        private Collection<String> m_nodeIds;

        private String m_id;

        private NodeSetFactory m_nodeSetFactory;

        /**
         * Initializer function that needs to be called after creating an instance of this Test.
         *
         * @param nodeSetFactoryExtension the configuration element representing the {@link NodeSetFactory}
         * @throws CoreException if the instantiation of the {@link NodeSetFactory} was not successful.
         */
        public void init(final IConfigurationElement nodeSetFactoryExtension) throws CoreException {
            m_nodeSetFactory = (NodeSetFactory)nodeSetFactoryExtension.createExecutableExtension(FACTORY);
            m_nodeIds = m_nodeSetFactory.getNodeFactoryIds();
            m_id = nodeSetFactoryExtension.getAttribute(FACTORY);
        }

        @Override
        public int countTestCases() {
            return m_nodeIds.size();
        }

        @SuppressWarnings({"unchecked", "deprecation"})
        @Override
        public void run(final TestResult result) {
            result.startTest(this);
            try {
                m_nodeIds.forEach(n -> new NodeInstantiationTest(n, () -> {
                    var instance = (NodeFactory<NodeModel>)m_nodeSetFactory.getNodeFactory(n).newInstance();
                    instance.loadAdditionalFactorySettings(m_nodeSetFactory.getAdditionalSettings(n));
                    return instance;
                }).run(result));
            } catch (Exception ex) {
                var err = new AssertionFailedError("Could not instantiate " + m_id + ": " + ex.getMessage());
                err.initCause(ex);
                result.addFailure(this, err);
            } catch (Throwable t) { // NOSONAR
                result.addError(this, t);
            } finally {
                result.endTest(this);
            }
        }

        @Override
        public String getName() {
            return " Headless Instantiation of: " + m_id;
        }

        @Override
        public String getSuiteName() {
            return m_id;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    /**
     * Headless instantiation test for a plug-in. Creates {@link NodeInstantiationTest} for each defined Node extension,
     * and {@link NodeSetInstantiationTest} for each defined Nodeset extension.
     */
    private static class PluginInstantiationTest implements TestWithName {
        /**
         * Output format: align left, width 7, newline at the end.
         */
        private static final String FORMAT = "%-7s%n";

        private final String m_name;

        private final List<IConfigurationElement> m_nodes;

        private final List<IConfigurationElement> m_nodeSetFactories;

        PluginInstantiationTest(final String name, final List<IConfigurationElement> nodes,
            final List<IConfigurationElement> nodeSetFactories) {
            m_name = name;
            m_nodes = nodes;
            m_nodeSetFactories = nodeSetFactories;
        }

        @Override
        public int countTestCases() {
            return m_nodes.size() + m_nodeSetFactories.size();
        }

        @Override
        public void run(final TestResult result) {
            result.startTest(this);

            System.out.printf("[%1$tH:%1$tM:%1$tS.%1$tL] => Instantiating nodes from %2$s%n", LocalDateTime.now(),
                getName());

            int maxNameLength = m_nodes.stream().mapToInt(e -> e.getAttribute(FACTORY).length()).max().orElse(0);

            for (IConfigurationElement e : m_nodes) {
                int ec = result.errorCount();
                int fc = result.failureCount();
                System.out.printf("[%1$tH:%1$tM:%1$tS.%1$tL] => Instantiating %2$-" + maxNameLength + "s...",
                    LocalDateTime.now(), e.getAttribute(FACTORY));

                @SuppressWarnings("unchecked")
                var test = new NodeInstantiationTest(e.getAttribute(FACTORY),
                    () -> (NodeFactory<NodeModel>)e.createExecutableExtension(FACTORY));
                test.run(result);

                if (result.errorCount() > ec) {
                    System.out.printf(FORMAT, "ERROR");
                } else if (result.failureCount() > fc) {
                    System.out.printf(FORMAT, "FAILURE");
                } else {
                    System.out.printf(FORMAT, "OK");
                }
            }

            // Test NodeSetFactories
            for (IConfigurationElement e : m_nodeSetFactories) {
                int ec = result.errorCount();
                int fc = result.failureCount();

                var test = new NodeSetInstantiationTest();
                try {
                    test.init(e);
                    test.run(result);
                } catch (CoreException e1) {
                    result.addError(test, e1);
                }

                if (result.errorCount() > ec) {
                    System.out.printf(FORMAT, "ERROR");
                } else if (result.failureCount() > fc) {
                    System.out.printf(FORMAT, "FAILURE");
                } else {
                    System.out.printf(FORMAT, "OK");
                }
            }
            result.endTest(this);
        }

        @Override
        public String getName() {
            return m_name;
        }

        @Override
        public String getSuiteName() {
            return "Headless instantiation of all nodes in " + m_name;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object start(final IApplicationContext context) throws Exception {
        System.setProperty("java.awt.headless", "true");

        Object args = context.getArguments().get(IApplicationContext.APPLICATION_ARGS);

        if (!extractCommandLineArgs(args) || (m_xmlResultFile == null)) {
            printUsage();
            return EXIT_OK;
        }

        var xmlResultFile = new File(m_xmlResultFile);
        if (!xmlResultFile.getParentFile().exists() && !xmlResultFile.getParentFile().mkdirs()) {
            throw new IOException("Can not create directory for results file " + m_xmlResultFile);
        }

        context.applicationRunning();

        IExtensionRegistry registry = Platform.getExtensionRegistry();

        // create a list of all nodesets
        IExtensionPoint nodesPoint = registry.getExtensionPoint("org.knime.workbench.repository.nodes");
        if (nodesPoint == null) {
            throw new IllegalStateException("Invalid extension point : org.knime.workbench.repository.nodes");
        }

        Map<String, List<IConfigurationElement>> allNodes = new HashMap<>();
        for (IExtension ext : nodesPoint.getExtensions()) {
            if (m_includedPlugins.contains(ext.getContributor().getName())) {
                allNodes.computeIfAbsent(ext.getContributor().getName(), k -> new ArrayList<>())
                    .addAll(Arrays.asList(ext.getConfigurationElements()));
            }
        }

        // create a list of all nodesets
        IExtensionPoint nodeSetsPoint = registry.getExtensionPoint("org.knime.workbench.repository.nodesets");
        if (nodeSetsPoint == null) {
            throw new IllegalStateException("Invalid extension point : org.knime.workbench.repository.nodesets");
        }
        Map<String, List<IConfigurationElement>> allNodeSets = new HashMap<>();
        for (IExtension ext : nodeSetsPoint.getExtensions()) {
            if (m_includedPlugins.contains(ext.getContributor().getName())) {
                allNodeSets.computeIfAbsent(ext.getContributor().getName(), k -> new ArrayList<>())
                    .addAll(Arrays.stream(ext.getConfigurationElements())
                        .filter(s -> !SKIPPED_NODESET_FACTORIES.contains(s.getAttribute(FACTORY))).toList());
            }
        }

        AbstractXMLResultWriter resultWriter = new XMLResultFileWriter(xmlResultFile);
        resultWriter.startSuites();
        for (var plugin : m_includedPlugins) {
            var test = new PluginInstantiationTest(plugin, allNodes.getOrDefault(plugin, Collections.emptyList()),
                allNodeSets.getOrDefault(plugin, Collections.emptyList()));
            var result = new WorkflowTestResult(test);
            result.addListener(resultWriter);
            test.run(result);
            resultWriter.addResult(result);
        }

        resultWriter.endSuites();

        return EXIT_OK;
    }

    /**
     * Extracts from the passed object the arguments. Returns <code>true</code> if everything went smooth,
     * <code>false</code> if the application must exit.
     *
     * @param args the object with the command line arguments.
     * @return <code>true</code> if the members were set according to the command line arguments, <code>false</code>, if
     *         an error message was printed and the application must exit.
     * @throws CoreException if preferences cannot be imported
     * @throws FileNotFoundException if the specified preferences file does not exist
     */
    private boolean extractCommandLineArgs(final Object args) {
        String[] stringArgs;
        if (args instanceof String[]) {
            stringArgs = (String[])args;
            if ((stringArgs.length > 0) && stringArgs[0].equals("-pdelaunch")) {
                var copy = new String[stringArgs.length - 1];
                System.arraycopy(stringArgs, 1, copy, 0, copy.length);
                stringArgs = copy;
            }
        } else if (args != null) {
            System.err.println("Unable to read application's arguments. Was expecting a String array, but got a "
                + args.getClass().getName() + ". toString() returns '" + args.toString() + "'");
            return false;
        } else {
            stringArgs = new String[0];
        }

        var i = 0;
        while (i < stringArgs.length) {
            if (stringArgs[i] == null) {
                i++;
            } else if (stringArgs[i].equals("-include")) {
                if (m_includedPlugins != null) {
                    System.err.println("Multiple -include arguments not allowed");
                    return false;
                }
                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing list for included plug-ins.");
                    return false;
                }
                m_includedPlugins = Stream.of(stringArgs[i++].split(",")).collect(Collectors.toSet());
            } else if (stringArgs[i].equals("-xmlResult")) {
                if (m_xmlResultFile != null) {
                    System.err.println("Multiple -xmlResult arguments not allowed");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <file_name> for option -xmlResult.");
                    return false;
                }
                m_xmlResultFile = stringArgs[i++];
            } else if (stringArgs[i].equals("-eclipse.password") || stringArgs[i].equals("-eclipse.keyring")) {
                // proxy arguments, nothing to do for us
                i += 2;
            } else {
                System.err.println("Invalid option: '" + stringArgs[i] + "'\n");
                return false;
            }
        }

        return true;
    }

    private static void printUsage() {
        System.err.println("Valid arguments:");

        System.err.println("    -include <list>: comma-separeted list of plug-in names; only nodes from these "
            + "plug-ins will be instantiated.");
        System.err
            .println("    -xmlResult <file_name>: specifies a single XML file where the test results are written to.");
    }

    @Override
    public void stop() {
        // nothing to do
    }
}
