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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.varia.LevelRangeFilter;
import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger.KNIMELogMessage;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.EclipseUtil;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

/**
 * Testcase that checks for expected log messages and reported unexpected ERRORs and FATALs. An appender to the root
 * logger is installed when an instance of this class is created. All subsequent log messages are recorded and analyzed
 * while the test is {@link #run(TestResult)}. The appender is unregistered after the test has run once. Therefore it
 * cannot be re-used.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
class WorkflowLogMessagesTest extends WorkflowTest {
    private final List<LoggingEvent> m_logEvents = new ArrayList<LoggingEvent>();

    private static final Pattern X_RANDR_PATTERN = Pattern.compile("^Xlib:\\s+extension \"RANDR\" missing on display.+");

    private final AppenderSkeleton m_logAppender = new AppenderSkeleton() {
        {
            LevelRangeFilter filter = new LevelRangeFilter();
            filter.setLevelMin(Level.DEBUG);
            filter.setLevelMax(Level.FATAL);
            addFilter(filter);
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }

        @Override
        public void close() {
            m_logEvents.clear();
        }

        @Override
        protected void append(final LoggingEvent event) {
            if (!Level.ERROR.equals(event.getLevel())
                || !X_RANDR_PATTERN.matcher(event.getRenderedMessage().trim()).matches()) {
                m_logEvents.add(event);
            }
        }
    };

    WorkflowLogMessagesTest(final String workflowName, final IProgressMonitor monitor, final WorkflowTestContext context) {
        super(workflowName, monitor, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void aboutToStart() {
        super.aboutToStart();
        Logger.getRootLogger().addAppender(m_logAppender);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final TestResult result) {
        if (!m_context.getTestflowConfiguration().executeWithCurrentTableBackend()) {
            // the test is supposed to test log messages of executed nodes as well, therefore it shouldn't be run if
            // no nodes are executed
            ignoreTest(result);
            return;
        }

        result.startTest(this);

        try {
            Logger.getRootLogger().removeAppender(m_logAppender);
            checkLogMessages(result, m_context.getTestflowConfiguration());
        } catch (Throwable t) {
            result.addError(this, t);
        } finally {
            result.endTest(this);
            Logger.getRootLogger().removeAppender(m_logAppender);
            m_logEvents.clear();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "log messages";
    }

    private static void findSubNodes(final WorkflowManager root, final boolean inComponent, final boolean testSubnodes,
        final Set<NodeID> ignoredIDs) {
        for (NodeContainer node : root.getNodeContainers()) {
            if (node instanceof SubNodeContainer) {
                findSubNodes(((SubNodeContainer)node).getWorkflowManager(), true, testSubnodes, ignoredIDs);
            } else {
                if (node instanceof WorkflowManager) {
                    findSubNodes((WorkflowManager)node, inComponent, testSubnodes, ignoredIDs);
                }
                if (inComponent && !testSubnodes) {
                    // subnodes log error messages, which, depending in the testSubnodes setting, we should ignore
                    ignoredIDs.add(node.getID());
                }
            }
        }
    }

    private void checkLogMessages(final TestResult result, final TestflowConfiguration flowConfiguration) {
        final var occurrenceMap = new HashMap<Level, List<Pattern>>();
        occurrenceMap.put(Level.ERROR, new ArrayList<>(flowConfiguration.getRequiredErrors()));
        occurrenceMap.put(Level.WARN, new ArrayList<>(flowConfiguration.getRequiredWarnings()));
        occurrenceMap.put(Level.INFO, new ArrayList<>(flowConfiguration.getRequiredInfos()));
        occurrenceMap.put(Level.DEBUG, new ArrayList<>(flowConfiguration.getRequiredDebugs()));

        final Set<NodeID> ignoredIDs = new HashSet<>();
        if (!flowConfiguration.testNodesInComponents()) {
            findSubNodes(m_context.getWorkflowManager(), false, flowConfiguration.testNodesInComponents(), ignoredIDs);
        }

        final var leftOverMap = createLeftOverMap(flowConfiguration);
        for (LoggingEvent logEvent : m_logEvents) {
            final Object o = logEvent.getMessage();
            final String nodeNameWithID;
            if (o instanceof KNIMELogMessage message) {
                if (ignoredIDs.contains(message.getNodeID())) {
                    continue;
                }
                nodeNameWithID = String.format("%s (%s)", message.getNodeName(), message.getNodeID());
            } else {
                nodeNameWithID = null;
            }

            final String message = logEvent.getRenderedMessage().trim();
            var expected = false;
            List<Pattern> currentList = occurrenceMap.get(logEvent.getLevel());
            if (currentList != null) {
                currentList.addAll(flowConfiguration.getOptionalLogMessages());
                Iterator<Pattern> it = currentList.iterator();
                while (it.hasNext()) {
                    final var p = it.next();
                    if (p.matcher(message).matches()) {
                        leftOverMap.get(logEvent.getLevel()).get(p.toString()).markAsOccurred();
                        expected = true;
                    }
                }
            }

            if (!expected && logEvent.getLevel().isGreaterOrEqual(Level.ERROR)) {
                result.addFailure(this,
                    new AssertionFailedError(String.format("Unexpected %s logged%s: %s", //
                        logEvent.getLevel(), //
                        nodeNameWithID == null ? "" : String.format(" by node \"%s\"", nodeNameWithID), //
                        message)));
            }
        }

        for (Map.Entry<Level, Map<String, PatternOccurrence>> e : leftOverMap.entrySet()) {
            for (Map.Entry<String, PatternOccurrence> entry : e.getValue().entrySet()) {
                if (!entry.getValue().hasOccurred()) {
                    result.addFailure(this, new AssertionFailedError("Expected " + e.getKey() + " log message '"
                            + TestflowConfiguration.patternToString(entry.getValue().getPattern()) + "' not found"));
                }
            }
        }
    }

    private static Map<Level, Map<String, PatternOccurrence>> createLeftOverMap(final TestflowConfiguration flowConfiguration) {
        final var leftOverMap = new HashMap<Level, Map<String, PatternOccurrence>>();
        var m = new HashMap<String, PatternOccurrence>();
        for (Pattern p : flowConfiguration.getRequiredErrors()) {
            if (!p.pattern().startsWith("\\QCODING PROBLEM") || KNIMEConstants.ASSERTIONS_ENABLED
                || EclipseUtil.isRunFromSDK()) {
                // don't add expected CODING PROBLEMs if they are not reported
                m.put(p.toString(), new PatternOccurrence(p));
            }
        }
        leftOverMap.put(Level.ERROR, m);

        m = new HashMap<>();
        for (Pattern p : flowConfiguration.getRequiredWarnings()) {
            m.put(p.toString(), new PatternOccurrence(p));
        }
        leftOverMap.put(Level.WARN, m);

        m = new HashMap<>();
        for (Pattern p : flowConfiguration.getRequiredInfos()) {
            m.put(p.toString(), new PatternOccurrence(p));
        }
        leftOverMap.put(Level.INFO, m);


        m = new HashMap<>();
        for (Pattern p : flowConfiguration.getRequiredDebugs()) {
            m.put(p.toString(), new PatternOccurrence(p));
        }
        leftOverMap.put(Level.DEBUG, m);

        return leftOverMap;
    }

    /**
     * Registers the occurrence of a string message, matching a {@link Pattern}.
     * Necessary because log messages can steal occurred patters in the following scenario:
     * <p>
     * Pattern p1 matches messages A and B.
     * Pattern p2 only matches message A.
     * <p>
     * If message A now comes in, it matches against p1 and the pattern would be marked
     * as seen and be removed. If message B comes in, only p2 is left and does not match.
     * This leads to an {@link AssertionFailedError} because the testflow thinks it that
     * this log message was not found.
     *
     * Using an occurrence flag for each {@link Pattern} solves this issue.
     *
     * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
     */
    private static final class PatternOccurrence {

        private final Pattern m_pattern;

        private boolean m_hasOccurred;

        private PatternOccurrence(final Pattern p) {
            m_pattern = p;
            m_hasOccurred = false;
        }

        private Pattern getPattern() {
            return m_pattern;
        }

        /**
         * Whether a log message matching the pattern has occurred before.
         *
         * @return has occurred?
         */
        private boolean hasOccurred() {
            return m_hasOccurred;
        }

        /**
         * Registers the occurrence of a message matching this pattern.
         */
        private void markAsOccurred() {
            m_hasOccurred = true;
        }
    }
}
